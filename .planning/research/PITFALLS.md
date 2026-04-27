# Pitfalls Research

**Domain:** Compose Desktop UI component library (glass morphism, custom window chrome, ~50 components, 3 themes)
**Researched:** 2026-04-27
**Confidence:** HIGH (critical pitfalls verified via official JetBrains issue tracker and Android official docs; MEDIUM for performance traps where Compose Desktop specifics were extrapolated from Compose general guidance)

---

## Critical Pitfalls

### Pitfall 1: Undecorated + Transparent Window Crashes on Windows 11

**What goes wrong:**
Setting `undecorated = true` combined with `transparent = true` on a `ComposeWindow` triggers a fatal JVM crash (`EXCEPTION_ACCESS_VIOLATION`) on Windows 11 in certain Skiko versions. The crash is in native Skiko code — not catchable. Same code may work fine on Windows 10.

**Why it happens:**
Skiko's DirectX 12 renderer and Windows 11 DWM composition interact differently than on Windows 10. The combination of undecorated + transparent forces a code path in Skiko that is unstable on some Windows 11 driver configurations.

**How to avoid:**
- Test the undecorated+transparent combination on Windows 11 before committing to this approach for `AeroTitleBar`.
- Prefer `undecorated = true` WITHOUT `transparent = true`: simulate transparency with a solid dark background rather than a truly transparent window.
- If glass-through-to-desktop transparency is not required (Aero style does not require it — it uses simulated glass via gradients and alpha), avoid `transparent = true` entirely.
- Pin the Skiko/Compose Desktop version and verify the fix status in the JetBrains issue tracker before upgrading.

**Warning signs:**
- JVM crash on startup on Windows 11 during window creation.
- Works on dev machine (Windows 10) but crashes on Windows 11 CI/test environment.
- Stack trace mentions `skiko` native libraries and EXCEPTION_ACCESS_VIOLATION.

**Phase to address:** Foundation phase (theme system + window chrome setup). Must be validated before any component work begins.

---

### Pitfall 2: WindowDraggableArea Does Not Integrate with OS Window Management

**What goes wrong:**
`WindowDraggableArea` moves the window by manually updating its position in Compose state — it does NOT send `HTCAPTION` to the OS. Consequences:
- Windows Aero Snap (drag-to-edge for snap, drag-to-top for maximize) does not trigger.
- Window dragging feels slightly laggy compared to native title bars because it goes through the JVM event loop.
- The window cannot be dragged outside screen bounds in directions that native windows can.
- Missing OS-level window animations (minimize/maximize/restore transitions).

**Why it happens:**
Compose Desktop's `WindowDraggableArea` is a pure-Compose implementation that listens to mouse events and calls `window.location = ...`. This bypasses the OS's `WM_NCHITTEST` / `HTCAPTION` mechanism that normally enables Aero Snap and native drag behavior.

**How to avoid:**
- Accept the limitation for v1: document that the custom title bar does not support native Aero Snap and add a maximize button instead.
- For native snap support, intercept `WM_NCHITTEST` via JNA/AWT event filter to return `HTCAPTION` for the title bar region. This is significantly more complex and platform-specific.
- Do NOT try to reimplement drag behavior with `MouseEvent` offsets — this causes jitter. Use `WindowDraggableArea` as-is or do it properly with OS hit testing.

**Warning signs:**
- Users report that dragging to screen edges does not snap the window.
- Double-clicking the title bar does not maximize.
- Dragging feels laggy or stutters at high monitor refresh rates.

**Phase to address:** `AeroTitleBar` component phase. Decision must be documented: accept limitation or implement JNA-based hit testing.

---

### Pitfall 3: Mixing `staticCompositionLocalOf` vs `compositionLocalOf` for Theme Colors

**What goes wrong:**
Using `compositionLocalOf` (dynamic) instead of `staticCompositionLocalOf` for a color scheme that never changes mid-session causes unnecessary recompositions across the entire subtree whenever ANY theme value changes. Conversely, using `staticCompositionLocalOf` for something that DOES change (e.g., a per-component override) causes the entire composition under the provider to be invalidated when it changes.

**Why it happens:**
Developers default to `compositionLocalOf` because it is more familiar or copy-pasted from examples. The distinction matters significantly for a library: the theme color scheme `AeroColorScheme` is switched rarely (user switches theme), making it a candidate for `staticCompositionLocalOf`. But if it is `static`, switching theme triggers full recomposition of the entire app — which is acceptable since theme switches are explicit user actions, not per-frame events.

**How to avoid:**
- Use `staticCompositionLocalOf` for `LocalAeroColors` (the full `AeroColorScheme` data class). Theme switches are intentional, full-tree recompose is acceptable.
- Use `compositionLocalOf` only for values that change frequently during a session (e.g., hover state would never be a CompositionLocal — use `remember` + state instead).
- Provide sensible default values in the `staticCompositionLocalOf` factory lambda instead of `error("not provided")` — this prevents crashes in Previews and test environments.

**Warning signs:**
- Profiler shows full app recomposition on any minor theme-adjacent state change.
- Previews crash with "CompositionLocal not provided" errors.
- Different components use inconsistent CompositionLocal types for the same concern.

**Phase to address:** Phase 1 (theme system foundation). Wrong choice here forces a library-wide refactor.

---

### Pitfall 4: Glass Effect Implemented as Overdraw-Heavy Layer Stack

**What goes wrong:**
Naive glass morphism in Compose stacks multiple `Box` layers: dark background, gradient overlay, semi-transparent tint, border. Each layer paints the entire component area. With 20+ glass components visible simultaneously (e.g., AeroDataTable rows + AeroSidebar + AeroPanel), this becomes severe GPU overdraw and drops frame rate below 30fps on integrated graphics.

**Why it happens:**
The Aero glass look requires gradient + transparency + border effects. Developers implement each as a separate Modifier chain call or nested composable, not realizing each adds a draw pass. Skiko's DirectX 12 renderer is fast, but the software fallback (4x slower) and integrated GPU targets are unforgiving of overdraw.

**How to avoid:**
- Consolidate all glass drawing into a single `drawBehind {}` block that paints gradient + border in one Canvas draw sequence.
- Avoid `Modifier.alpha()` on containers — it forces the entire subtree into an offscreen layer. Use `Color.copy(alpha = ...)` at paint time instead.
- The existing `GlassModifiers.kt` from mordred should be reviewed: ensure it uses `drawBehind` or `drawWithContent` rather than wrapping in semi-transparent `Box` layers.
- Mark glass modifiers as `@Stable` so Compose can skip recomposition when inputs haven't changed.
- Add a software-rendering fallback mode: when glass effects are too expensive, fall back to solid panel with subtle border.

**Warning signs:**
- `Layout Inspector` > `Show Overdraw` shows red/dark-red on glass components.
- FPS drops to 20-40 on machines without discrete GPU.
- Animations stutter when multiple glass panels are on screen simultaneously.

**Phase to address:** Phase 1 (glass modifier foundation) — get the drawing model right before 50 components use it.

---

### Pitfall 5: Unstable Component Parameters Causing Excessive Recomposition

**What goes wrong:**
A library component that accepts `List<T>`, `Map<K,V>`, or non-`@Stable` lambda parameters will be recomposed on every parent recomposition, even when nothing visually changed. For a 50-component library, this silently kills performance in consuming applications.

**Why it happens:**
Compose's compiler infers stability from type declarations. `List<T>` is not stable (it is a mutable interface under the hood). Lambdas captured in composable scopes are also not stable by default. Library authors often don't run the Compose compiler metrics tool and only discover this when users report sluggishness.

**How to avoid:**
- Use `ImmutableList` from `kotlinx.collections.immutable` for any `List` parameters in public component APIs (e.g., `AeroDataTable` columns, `AeroTreeView` nodes).
- Apply `@Immutable` to all `data class` token/color types (`AeroColorScheme`, typography specs). This tells the Compose compiler they are stable.
- Apply `@Stable` to component state holders (if any are exposed publicly).
- Enable Compose compiler metrics in the library module and review the generated `*-composables.txt` report before each release: `freeCompilerArgs += ["-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=..."]`
- Never expose raw Kotlin `List` or `Map` in public component APIs — wrap them or use `ImmutableList`.

**Warning signs:**
- Compose compiler metrics show library composables as "unstable" or "restartable but not skippable".
- Consuming app shows full-screen recompositions on unrelated state changes via Layout Inspector.
- `AeroDataTable` or `AeroTreeView` cause visible frame drops when parent state updates.

**Phase to address:** Phase 1 (API design decisions) and verified per component as each is built.

---

### Pitfall 6: Material3 Defaults Bleeding Through Custom Components

**What goes wrong:**
When wrapping Material3 components (e.g., using `Button` as the base for `AeroButton`), unreplaced Material parameters render with Material3 defaults: M3 purple ripple, M3 rounded shapes, M3 elevation. The result is visual inconsistency — Aero-styled surface with a purple ripple on hover.

**Why it happens:**
Not all Material3 styling is exposed as direct function parameters. Some values are read via `CompositionLocal` internally (e.g., `LocalRippleConfiguration`, `LocalTextStyle`, `LocalContentColor`). Wrapping a Material composable without overriding these locals leaves M3 defaults active.

**How to avoid:**
- For each wrapped Material component, enumerate ALL CompositionLocals it reads internally and provide overrides.
- For `AeroButton`: override `LocalRippleConfiguration` to use a white/light ripple instead of M3 purple. Provide custom `ButtonColors` via `ButtonDefaults.buttonColors(...)` sourced from `LocalAeroColors`.
- For text inside components: wrap content in `ProvideTextStyle(LocalAeroTypography.current.bodyMedium)`.
- Consider using `CompositionLocalProvider` blocks rather than hoping parameter defaults propagate correctly.
- Write a visual regression test in the showcase: render each component, screenshot it, and confirm no M3 purple/M3-blue tones appear.

**Warning signs:**
- Purple ripple on click for AeroButton or AeroCheckbox.
- Default M3 blue focus indicator color on AeroTextField.
- Components look correct in isolation but show M3 defaults when the user's own MaterialTheme is also applied.

**Phase to address:** Every component implementation phase — needs a checklist item "verify no M3 defaults visible."

---

### Pitfall 7: Public API Surface Designed for Internal Use

**What goes wrong:**
Internal helper composables, utility functions, and intermediate state holders are accidentally published as `public` (Kotlin's default visibility). Consumers depend on them. When the library author tries to refactor, it is a breaking change. The library becomes frozen around internal implementation details.

**Why it happens:**
When migrating code from a working application (mordred) to a library, everything starts as `internal` or `private` within a module, but the module boundary changes during extraction. Developers forget to audit visibility after extraction.

**How to avoid:**
- Enable `explicitApi()` in the library module's `build.gradle.kts`. This makes all declarations require explicit `public` / `internal` / `private` — no defaults. Any missed annotation is a compile error.
- Rule: nothing is `public` by default. Only add `public` after conscious decision.
- Separate the library module from the showcase module from day one. The library module should have no knowledge of showcase-internal utilities.
- Keep `internal` for: drawing helpers, glass modifier internals, color calculation utilities, state machine implementations.
- Keep `public` for: composable component functions, token data classes, theme provider, modifier extensions intended for consumer use.

**Warning signs:**
- Showcase and library are in the same Gradle module.
- Helper functions like `lerp`, `glassGradient`, or `colorWithAlpha` are `public` without conscious intent.
- Library JAR contains classes not intended for consumer use (check with `jar tf` or dependency analysis).

**Phase to address:** Project structure setup (before any code is written).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hardcode Aero colors as literals (`Color(0xFF4FC3F7)`) in component files | Faster to write | Theme switching breaks, impossible to reskin | Never — always use `LocalAeroColors.current` |
| Single Gradle module for library + showcase | Simpler setup | Showcase dependencies leak into published JAR, `explicitApi()` hard to enforce | Never — separate from day one |
| Use `compositionLocalOf` for everything | Simpler mental model | Full-tree recomposition on any theme change | Acceptable only for truly dynamic per-frame values |
| Skip `@Immutable` / `@Stable` annotations on token classes | Faster to write | Compose compiler cannot skip recomposition, silent perf regression | Never on published API types |
| `Modifier.alpha()` on glass container instead of painting alpha | Simpler code | Forces offscreen layer allocation per component, kills performance with many glass components | Only for single-instance full-screen overlays (e.g., AeroDialog backdrop) |
| Copy-paste color values between themes | Faster initial setup | Themes diverge, one-off fixes missed in other themes | MVP only, immediately extract to shared constants |
| Use raw `List<T>` in component public API | Familiar Kotlin | Compose marks composable as unstable, consuming apps recompose unnecessarily | Never for collections — use `ImmutableList` or `@Immutable` wrapper |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Material3 ripple system | Leaving M3's default purple ripple | Override `LocalRippleConfiguration` with `RippleConfiguration(color = LocalAeroColors.current.accent)` inside each component or at theme level |
| AWT/Swing interop (for JNA window effects) | Calling AWT methods from the Compose main thread | AWT and Compose share the EDT on Desktop; still use `SwingUtilities.invokeLater` for AWT-specific calls to avoid ordering issues |
| Skiko DirectX/OpenGL fallback | Assuming hardware acceleration is always available | Detect `SKIKO_RENDER_API` env variable in showcase; document that glass effects may be degraded on software renderer (4x slower) |
| `ComposeWindowStyler` (MayakaApps library) | Not pinning version when using for Mica/Acrylic effects | Library is community-maintained; pin exact version, wrap in feature flag so library works without it |
| Popup / ContextMenu z-ordering with SwingPanel | Compose popups clip behind Swing heavyweight components | Never mix SwingPanel with Compose popups in the same layout region; if JxBrowser or similar needed, keep it in a separate undecorated sub-window |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Glass modifier as nested `Box` layers | FPS drop with multiple glass panels, high GPU overdraw | Consolidate to single `drawBehind` / `Canvas` block | 5+ glass components on screen simultaneously |
| `Modifier.alpha()` on glass containers | Allocates offscreen framebuffer per component | Use `Color.copy(alpha=...)` at draw time | Every glass component on screen |
| Animating glass panel background on every frame | Constant recomposition of the entire component tree | Use `graphicsLayer { alpha = ... }` for animations — bypasses recomposition | Any animation involving glass panels |
| `animateFloatAsState` for alpha with lambda NOT used | Recomposes the component body on every animation frame | Use `graphicsLayer { alpha = animatedAlpha }` (lambda form defers read to draw phase) | Any per-frame alpha animation |
| Unstable `List<T>` in AeroDataTable / AeroTreeView | Full recompose of entire table on any parent state change | Wrap data in `@Immutable` data class or use `ImmutableList` | Any table with > 20 rows |
| Creating new lambda instances in component body | Composable marked non-skippable by compiler | Extract lambdas to `remember { }` or stable references | Recompositions triggered by parent |
| Software renderer fallback active silently | Glass animations at 5-10 FPS on integrated/virtual GPU | Log `SKIKO_RENDER_API` at startup in showcase; provide degraded-glass fallback mode | Systems without DirectX12/OpenGL support |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Custom title bar without keyboard window management | Alt+F4 doesn't work, window not accessible via keyboard | AeroTitleBar must wire keyboard shortcuts (Alt+F4 close, Win+Up maximize) — test explicitly |
| No disabled state for glass components | Components look "ghosted" due to transparency; disabled unclear | Define explicit `disabledAlpha` in `AeroColorScheme`; apply to all interactive components consistently |
| AeroToast / AeroSnackbar spawning new windows for overlay | Multiple JVM windows appear in taskbar, flicker on display | Use a `Popup` anchored to the main window rather than a new `ComposeWindow` |
| Glass blur radius too high on low-DPI displays | Blurry, illegible text over glass surfaces | Test at 100% DPI (1920x1080) not just high-DPI; blur should be decorative, not obscuring |
| Inconsistent hover states across components | Some components light up, others don't respond | Define hover elevation/tint rules in `AeroColorScheme` and enforce for all interactive components |
| Missing focus ring for keyboard navigation | Keyboard-only users cannot see focused component | Define `focusBorderColor` in `AeroColorScheme`, apply via `Modifier.onFocusChanged` in every interactive component |

---

## "Looks Done But Isn't" Checklist

- [ ] **AeroTitleBar:** Often missing double-click to maximize — verify `onDoubleClick` on `WindowDraggableArea` calls `window.extendedState = Frame.MAXIMIZED_BOTH`
- [ ] **Glass modifier:** Visually correct but missing `@Stable` annotation — verify Compose compiler metrics show it as stable
- [ ] **AeroDropdown / AeroComboBox:** Dropdown renders within Compose layout bounds — verify it uses `Popup` (not a nested `Box`) so it can overlap other components
- [ ] **Theme switching:** Colors switch but existing `remember`-ed values do not update — verify no color values are cached in `remember {}` without theme key
- [ ] **AeroDialog:** Appears as child of main window layout — verify it uses a `Dialog` window composable so it truly floats above all content
- [ ] **AeroTooltip:** Does not appear on keyboard focus, only on hover — verify `Modifier.onFocusChanged` triggers tooltip in addition to `onHover`
- [ ] **Published JAR:** Contains showcase classes — verify with `jar tf aero-compose-ui-*.jar | grep -v "com/mordred"` finds nothing unexpected
- [ ] **AeroColorScheme:** `@Immutable` annotation applied — verify with Compose compiler metrics output
- [ ] **Font bundled in library:** Custom font included in JAR but not declared in library manifest — verify consuming project can load font without additional setup
- [ ] **Software renderer:** Glass effects silently degraded — verify behavior under `SKIKO_RENDER_API=SOFTWARE` before release

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Wrong CompositionLocal type (dynamic vs static) for theme | MEDIUM | Change `compositionLocalOf` to `staticCompositionLocalOf` in theme file; retest all components for recomposition behavior; not a breaking API change if the public accessor (`AeroTheme.colors`) is unchanged |
| `List<T>` in public API (unstable params) | MEDIUM | Add `ImmutableList` wrapper types; add overloads accepting `ImmutableList`; deprecate `List` overloads; binary-compatible if done with overloads |
| Internal types leaked to public API | HIGH | Requires major version bump; rename/move internal types to new package; provide migration guide; very painful if consumers have already taken dependency |
| Glass modifier as Box layers (overdraw) | LOW-MEDIUM | Refactor modifier internals only; public API surface unchanged; run visual comparison screenshots to confirm no regression |
| Undecorated+transparent crash on Windows 11 | HIGH if discovered late | Drop `transparent = true`; adopt simulated glass (gradient on solid background); requires re-testing all window chrome components |
| M3 defaults bleeding through (purple ripple) | LOW per component | Per-component fix: wrap with `CompositionLocalProvider(LocalRippleConfiguration provides ...)` |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Undecorated+transparent crash (Windows 11) | Phase 1: Window setup | Create bare undecorated window on Windows 11 and confirm no crash before writing any title bar code |
| WindowDraggableArea OS integration limits | Phase 1: AeroTitleBar | Document limitation explicitly in component KDoc; add maximize on double-click |
| Wrong CompositionLocal type for theme | Phase 1: Theme foundation | Compose compiler metrics show `LocalAeroColors` as static; no recompose on theme switch except intentional |
| Glass overdraw | Phase 1: Glass modifiers | Layout Inspector overdraw view shows at most 2x overdraw on glass component |
| Unstable component parameters | Every component phase | Compiler metrics report each published composable as "skippable" |
| M3 defaults bleeding | Every component phase | Visual check in showcase: no purple, no M3-blue on any Aero component |
| Public API leaking internals | Project setup phase | `explicitApi()` enforced in library module `build.gradle.kts` from day one |
| Popup z-ordering issues | AeroDropdown / AeroContextMenu / AeroPopover phase | Test: open dropdown while AeroPanel is behind it; dropdown must appear above |
| Font rendering degradation on Windows | Phase 1 typography setup | Test showcase on Windows at 100% DPI with ClearType; text must be legible at body sizes |
| Software renderer degradation | All glass-heavy components | Test showcase with `SKIKO_RENDER_API=SOFTWARE`; document minimum frame rate expectations |

---

## Sources

- JetBrains compose-multiplatform GitHub issue #3757: Fatal error undecorated+transparent on Windows 11 — https://github.com/JetBrains/compose-multiplatform/issues/3757
- JetBrains compose-multiplatform GitHub issue #1248: Aero Snap not triggered with WindowDraggableArea — https://github.com/JetBrains/compose-jb/issues/1248
- JetBrains compose-multiplatform GitHub issue #3772: WindowDraggableArea cannot drag outside screen bounds — https://github.com/JetBrains/compose-multiplatform/issues/3772
- JetBrains compose-multiplatform GitHub issue #520: Resize not supported for undecorated windows on Windows — https://github.com/JetBrains/compose-jb/issues/520
- JetBrains compose-multiplatform GitHub issue #2926: Z-ordering with Swing interop — https://github.com/JetBrains/compose-multiplatform/issues/2926
- Android Official Docs — Custom design systems in Compose: https://developer.android.com/develop/ui/compose/designsystems/custom
- Android Official Docs — CompositionLocal: https://developer.android.com/develop/ui/compose/compositionlocal
- Android Official Docs — Compose stability: https://developer.android.com/develop/ui/compose/performance/stability
- Android Official Docs — Fix stability issues: https://developer.android.com/develop/ui/compose/performance/stability/fix
- Compose API Guidelines (official): https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md
- Skiko render API discussion (JetBrains): https://github.com/JetBrains/compose-jb/issues/382
- DirectX 12 renderer announcement: https://blog.jetbrains.com/kotlin/2021/02/jetpack-compose-for-desktop-milestone-3-released/
- ComposeWindowStyler (community library for native window effects): https://github.com/MayakaApps/ComposeWindowStyler
- Shader-based render effects in Compose Desktop: https://www.pushing-pixels.org/2022/04/09/shader-based-render-effects-in-compose-desktop-with-skia.html
- Explicit API mode for Kotlin: https://chao2zhang.medium.com/explicit-api-mode-for-kotlin-on-android-b8264fdd76d1
- Sasikanth: WindowDraggableArea exploration: https://www.sasikanth.dev/dragging-undecorated-windows-in-compose-desktop/

---
*Pitfalls research for: Compose Desktop UI component library (aero-compose-ui)*
*Researched: 2026-04-27*
