# Phase 1: Foundation - Research

**Researched:** 2026-04-27
**Domain:** Compose Desktop — theme system (CompositionLocal, color tokens, typography), glass modifiers (drawBehind rendering), Gradle multi-module library structure, explicitApi enforcement
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**AeroColorScheme token set**
- Clean general-purpose set — strip all mordred-specific fields (connectionActive, connectionInactive, executionHighlight, logBackground, timeStampBackground)
- Keep roughly 20-22 general tokens: primary/onPrimary, surface/onSurface, background/onBackground, error/onError, glassSurface, glassBorder, glassHighlight, panelBackground, borderDefault, borderSelected, labelText, buttonHover, closeButtonHover, cardBackground (+ a few more as needed)
- Declared as `@Immutable data class AeroColorScheme(...)`
- Three built-in presets live as companion object properties: `AeroColorScheme.AeroBlue`, `AeroColorScheme.AeroDark`, `AeroColorScheme.Classic` — with exact hex values from mordred
- Custom themes via standard Kotlin `copy()`: `AeroTheme(colorScheme = AeroColorScheme.AeroBlue.copy(primary = myColor))`
- No DSL or builder — copy() is idiomatic and sufficient

**AeroTypography**
- Separate `@Immutable data class AeroTypography(...)` with TextStyle instances: title 18sp bold, bodyLarge 14sp, bodyMedium 13sp, bodySmall 12sp, label 11sp bold
- Accessed via `LocalAeroTypography` CompositionLocal — mirrors how `LocalAeroColors` works
- `AeroTheme {}` sets both `LocalAeroColors` and `LocalAeroTypography`
- Material3 Typography is also configured inside `AeroTheme` (as in mordred) for Material3 component compatibility

**Glass modifier API**
- All three modifiers auto-read from `LocalAeroColors.current` internally — no explicit colors parameter required at the call site
- Must be called within `AeroTheme {}` (crashes if not — acceptable constraint, documented in library KDoc)
- Rendering approach: keep `.shadow()` for elevation (unavoidable on Compose Desktop), put gradient fill + border stroke inside a single `drawBehind {}` block — satisfies FOUND-07 (no overdraw from background layering)
- Same rendering pattern across all three, different tokens:
  - `glassEffect` → shadow + gradient(glassSurface) + border(glassBorder) — card/element level
  - `glassPanel` → gradient(panelBackground), no border, no shadow — large section backgrounds
  - `glassSurface` → gradient(glassHighlight → transparent) + border(glassBorder) — mid-level surfaces
- Signature keeps optional `cornerRadius: Dp` and `elevation: Dp` parameters (as in mordred)

**Project / module structure**
- Two modules only: `:library` + `:showcase`
- `:library` — `compose.desktop.common` plugin (platform-neutral JAR, not `currentOs`)
- `:showcase` — `compose.desktop.currentOs` plugin (runnable desktop app)
- `:showcase` depends on `:library` via `implementation(project(":library"))` — no local Maven publish needed during development
- All dependency versions in `gradle/libs.versions.toml` (version catalog)
- `explicitApi()` declared in `:library` build script

**Showcase Phase 1 scope**
- Standard decorated window (OS chrome) for now — AeroTitleBar arrives in Phase 3 with `FrameWindowScope`
- Top section: theme switcher using plain Material3 SegmentedButton (replaced with AeroSegmentedControl in Phase 2 — one-line swap)
- Foundation section: three demo boxes showing glassEffect, glassPanel, glassSurface with labeled captions — proves theme system works visually
- Below Foundation: empty placeholder sections for future phases (Buttons, Input, etc.) with "coming Phase 2..." labels
- Showcase grows in each phase — Phase 1 establishes the structure, each subsequent phase adds its section

### Claude's Discretion
- Exact set of the ~20-22 token names in AeroColorScheme (stay close to mordred names where general enough)
- Whether to use `.composed {}` or `Modifier.Node` API for the CompositionLocal-reading modifiers
- Exact layout of the showcase Foundation section
- Whether to add a `secondary`/`onSecondary` token or defer to Phase 2 when buttons need it

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| FOUND-01 | AeroTheme {} wraps content; children receive active theme's colors and typography automatically | CompositionLocalProvider + staticCompositionLocalOf pattern; confirmed in mordred MordredTheme.kt |
| FOUND-02 | AeroColorScheme — @Immutable data class with full set of color tokens | Direct port from MordredColorScheme (29 tokens → ~22 after stripping domain-specific ones); @Immutable documented in Android Compose stability docs |
| FOUND-03 | AeroBlue theme included by default (bg #0D1B2A, accent #4FC3F7) | Hex values confirmed in mordred ColorScheme.kt — MordredThemePresets.AeroBlue |
| FOUND-04 | AeroDark theme included by default (bg #0A0A1A, accent #90CAF9) | Hex values confirmed in mordred ColorScheme.kt — MordredThemePresets.AeroDark |
| FOUND-05 | Classic theme included by default (bg #1E1E1E, accent #5C8ABF) | Hex values confirmed in mordred ColorScheme.kt — MordredThemePresets.Classic |
| FOUND-06 | Developer can supply custom AeroColorScheme to AeroTheme | Kotlin data class copy() pattern; no special framework needed |
| FOUND-07 | Modifier.glassEffect() renders gradient + border + shadow in a single drawBehind block, no overdraw | mordred GlassModifiers.kt shows current multi-modifier chain (overdraw); must be consolidated into drawBehind — architectural change identified |
| FOUND-08 | Modifier.glassPanel() and Modifier.glassSurface() available | Both in mordred GlassModifiers.kt; same port + drawBehind consolidation as FOUND-07 |
| FOUND-09 | AeroTypography data class with TextStyle set (title 18sp, body 14sp/13sp/12sp, label 11sp) | mordred MordredTheme.kt has matching Typography instance; port to standalone data class |
| FOUND-10 | explicitApi() declared in :library | Gradle Kotlin DSL feature; one-line addition to library build.gradle.kts |
| SHW-01 | :showcase Compose Desktop app launches and shows components grouped by category | Greenfield — new Compose Desktop application with compose.desktop.currentOs plugin |
| SHW-02 | Showcase has theme switcher that instantly toggles AeroBlue / AeroDark / Classic | mutableStateOf(colorScheme) at ShowcaseApp root; pass to AeroTheme; staticCompositionLocalOf ensures full recompose |
| SHW-03 | Showcase updated in parallel with each phase | Structure: each phase adds its own section; Phase 1 adds Foundation section only |
</phase_requirements>

---

## Summary

Phase 1 is an extraction and refactoring exercise, not a design-from-scratch exercise. All core concepts — color tokens, CompositionLocals, glass modifiers, theme switching — are already proven in the `mordred` reference application. The primary work is: (1) creating the two-module Gradle project structure, (2) porting `MordredColorScheme` → `AeroColorScheme` with domain tokens stripped, (3) porting `MordredTheme.kt` → `AeroTheme.kt`, (4) refactoring `GlassModifiers.kt` to eliminate the multi-modifier overdraw chain and consolidate into `drawBehind {}`, and (5) building the `:showcase` skeleton with a decorated window and theme switcher.

The single most important architectural decision for this phase is how the glass modifiers read colors from CompositionLocal. The CONTEXT.md decision ("auto-read from `LocalAeroColors.current`") requires the modifiers to be `@Composable` (cannot call `LocalAeroColors.current` in a regular Modifier extension). The clean solution is a `@Composable` Modifier extension or `Modifier.composed {}` factory — both patterns are well-understood and have a clear answer (see Architecture Patterns section). The `Modifier.Node` API is the modern approach but adds complexity for this phase; `@Composable Modifier` extension or `composed {}` is simpler and fully supported.

The overdraw issue in mordred's current implementation (stacked `.background()` + `.border()` calls = multiple draw passes) must be fixed before it becomes the pattern for 50+ components. Consolidating to `drawBehind {}` in Phase 1 prevents a library-wide refactor in Phase 3.

**Primary recommendation:** Port mordred sources directly, strip domain tokens, consolidate glass modifier drawing into `drawBehind {}`, use `@Composable` Modifier extension for CompositionLocal access, enforce `explicitApi()` from the first commit.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.3.21 | Language | Current stable; required K2 compiler; CMP 1.10.3 minimum is 2.1.0 |
| Compose Multiplatform | 1.10.3 | UI engine (Skiko/Skia) | Latest stable; bundles Material3, foundation, ui; Desktop target is production-ready |
| Gradle Kotlin DSL | 9.4.1 | Build system | Type-safe build scripts; mandatory for new KMP projects |
| JDK | 17 | JVM runtime | Floor set in PROJECT.md; required for jpackage distributions |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `compose.material3` | bundled with CMP 1.10.3 | Material3 primitives; darkColorScheme, Typography | Always — AeroTheme wraps MaterialTheme, not replaces it |
| `compose.foundation` | bundled | BorderStroke, Brush, RoundedCornerShape, drawBehind | Always — glass modifiers use DrawScope APIs from here |
| `compose.ui` | bundled | Modifier, CompositionLocal, TextStyle, Color, Canvas | Always — fundamental Compose APIs |
| `kotlinx-coroutines-core` | 1.10.2 | Async state, animation ticks | Available from day 1; needed by animation in Phase 2+ |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `Modifier.composed {}` for CompositionLocal access | `Modifier.Node` API | Modifier.Node is the modern approach (better performance, deferred setup), but adds complexity; `composed` or `@Composable` extension is simpler for Phase 1 and fully functional |
| `staticCompositionLocalOf` for theme locals | `compositionLocalOf` | Dynamic tracks reads (finer recompose), static invalidates all children on change; for infrequent theme switches, static is correct and faster (no tracking overhead on every frame) |
| Gradient-only glass simulation | Haze library (chrisbanes/haze) | Haze provides real GPU-backed blur using Skiko; adds a dependency and changes modifier contract (requires hazeSource + hazeChild cooperating composables); gradient simulation is visually sufficient for Aero aesthetic |

**Installation:**
```bash
# No npm — pure Gradle. After creating build files:
./gradlew build
./gradlew :showcase:run
```

---

## Architecture Patterns

### Recommended Project Structure

```
aero-compose-ui/                      # root Gradle project
├── gradle/
│   └── libs.versions.toml            # version catalog (ALL versions here)
├── settings.gradle.kts               # includes :library and :showcase
├── build.gradle.kts                  # root build (repos, allprojects config)
│
├── library/                          # :library module — the published artifact
│   ├── build.gradle.kts              # kotlin.multiplatform + compose + explicitApi()
│   └── src/main/kotlin/
│       └── com/mordred/aero/
│           └── theme/
│               ├── AeroColorScheme.kt    # @Immutable data class + 3 companion presets
│               ├── AeroTypography.kt     # @Immutable data class + TextStyle set
│               ├── AeroTheme.kt          # CompositionLocals + AeroTheme{} + accessor object
│               └── GlassModifiers.kt     # glassEffect / glassPanel / glassSurface
│
└── showcase/                          # :showcase module — NOT published
    ├── build.gradle.kts               # compose.desktop.currentOs app
    └── src/main/kotlin/
        └── com/mordred/showcase/
            ├── Main.kt
            └── ShowcaseApp.kt
```

**Phase 1 only creates the `theme/` package inside `:library`.** The `components/` tree is Phase 2+.

### Pattern 1: staticCompositionLocalOf for Theme Tokens

**What:** `LocalAeroColors` and `LocalAeroTypography` are declared with `staticCompositionLocalOf`, not `compositionLocalOf`.

**When to use:** When the value changes only on explicit user action (theme switch), not on every frame. Theme tokens are read on every composable frame but changed rarely.

**Trade-off:** `staticCompositionLocalOf` invalidates the entire `content` subtree when the value changes (full recompose of all `AeroTheme {}` children). This is acceptable for deliberate theme switches; the benefit is zero tracking overhead on every frame read, which matters when 50+ components all read `LocalAeroColors.current` on every frame.

**Example:**
```kotlin
// Source: mordred/MordredTheme.kt (adapted)
// theme/AeroTheme.kt

val LocalAeroColors = staticCompositionLocalOf<AeroColorScheme> {
    AeroColorScheme.AeroBlue  // sensible default — not error() — prevents Preview crashes
}

val LocalAeroTypography = staticCompositionLocalOf<AeroTypography> {
    AeroTypography()
}

@Composable
public fun AeroTheme(
    colorScheme: AeroColorScheme = AeroColorScheme.AeroBlue,
    content: @Composable () -> Unit
) {
    val materialColors = darkColorScheme(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        error = colorScheme.error,
        onError = colorScheme.onError
    )
    CompositionLocalProvider(
        LocalAeroColors provides colorScheme,
        LocalAeroTypography provides AeroTypography(colorScheme)
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}

// Clean accessor — no need to import LocalAeroColors at call site
public object AeroTheme {
    public val colors: AeroColorScheme
        @Composable get() = LocalAeroColors.current
    public val typography: AeroTypography
        @Composable get() = LocalAeroTypography.current
}
```

### Pattern 2: Glass Modifiers with drawBehind (No Overdraw)

**What:** Glass surface treatment uses a single `drawBehind {}` block that paints gradient fill + border stroke in one Canvas draw pass. Shadow is applied via `Modifier.shadow()` before `drawBehind` (unavoidable separate pass on Compose Desktop — shadow needs its own layer). Three public extension functions: `glassEffect`, `glassPanel`, `glassSurface`.

**Critical difference from mordred:** Mordred's `GlassModifiers.kt` stacks `.background()` + `.border()` as separate Modifier chain entries = 2–3 separate draw passes (overdraw). The Phase 1 implementation consolidates gradient + border into one `drawBehind` block.

**CompositionLocal access:** The CONTEXT.md decision requires the modifiers to auto-read from `LocalAeroColors.current` — this means they must be `@Composable`. Use `@Composable` Modifier extension function:

```kotlin
// Source: pattern derived from mordred GlassModifiers.kt + drawBehind consolidation
// theme/GlassModifiers.kt

@Composable
public fun Modifier.glassEffect(
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp
): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation, shape)
        .drawBehind {
            val cornerPx = cornerRadius.toPx()
            // Gradient fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.glassSurface,
                        colors.glassSurface.copy(alpha = colors.glassSurface.alpha * 0.5f)
                    )
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
            )
            // Border stroke (single draw call — no overdraw)
            drawRoundRect(
                color = colors.glassBorder,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)
}

@Composable
public fun Modifier.glassPanel(
    cornerRadius: Dp = 0.dp
): Modifier {
    val colors = LocalAeroColors.current
    return this.drawBehind {
        val cornerPx = cornerRadius.toPx()
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colors.panelBackground,
                    colors.panelBackground.copy(alpha = colors.panelBackground.alpha * 0.8f)
                )
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
        )
    }
}

@Composable
public fun Modifier.glassSurface(
    cornerRadius: Dp = 8.dp
): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    return this.drawBehind {
        val cornerPx = cornerRadius.toPx()
        // Highlight gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.glassHighlight, Color.Transparent),
                startY = 0f,
                endY = 100f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
        )
        // Border
        drawRoundRect(
            color = colors.glassBorder,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx),
            style = Stroke(width = 1.dp.toPx())
        )
    }.clip(shape)
}
```

**Note on `@Composable Modifier` vs `composed {}`:** `Modifier.composed {}` is an older factory that achieves the same CompositionLocal access; it is still supported but `@Composable Modifier` extension is the modern pattern. Both are acceptable. `Modifier.Node` (the newest API) does not support CompositionLocal reads in its `draw()` phase without additional complexity — defer to Phase 2 if needed.

### Pattern 3: AeroColorScheme with Companion Object Presets

**What:** `AeroColorScheme` is declared as an `@Immutable data class` with all tokens as constructor parameters. Three presets are companion object properties (not a separate object like mordred's `MordredThemePresets`).

**Why companion object:** Consumer call site is `AeroColorScheme.AeroBlue` instead of `AeroThemePresets.AeroBlue`. More discoverable and idiomatic for a data class.

```kotlin
// theme/AeroColorScheme.kt
// Exact hex values from mordred/ColorScheme.kt — MordredThemePresets

@Immutable
public data class AeroColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val error: Color,
    val onError: Color,
    val cardBackground: Color,
    val borderDefault: Color,
    val borderSelected: Color,
    val labelText: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val titleBarGradientStart: Color,
    val titleBarGradientEnd: Color,
    val titleBarText: Color,
    val buttonHover: Color,
    val closeButtonHover: Color,
    val panelBackground: Color
) {
    public companion object {
        public val AeroBlue: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003B5C),
            secondary = Color(0xFF81D4FA),
            onSecondary = Color(0xFF003B5C),
            surface = Color(0xCC1A3A5C),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF0D1B2A),
            onBackground = Color(0xFFE0E0E0),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0x40FFFFFF),
            borderDefault = Color(0x60FFFFFF),
            borderSelected = Color(0xFF4FC3F7),
            labelText = Color(0xFFBDBDBD),
            glassSurface = Color(0x30FFFFFF),
            glassBorder = Color(0x50FFFFFF),
            glassHighlight = Color(0x20FFFFFF),
            titleBarGradientStart = Color(0xDD1A3A6C),
            titleBarGradientEnd = Color(0xDD0D1F3C),
            titleBarText = Color(0xFFE0E8F0),
            buttonHover = Color(0x40FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xCC152A42)
        )
        // AeroDark and Classic follow the same pattern with mordred hex values
    }
}
```

**Discretion note:** `secondary`/`onSecondary` are included above (present in mordred, needed by Material3 bridge) even though no Aero component uses them in Phase 1. This avoids a breaking API change when Phase 2 buttons need them. This is the recommended approach.

### Pattern 4: Theme Switch in Showcase

**What:** Theme state is hoisted to the root of `ShowcaseApp`. `AeroTheme` receives the current scheme. The theme switcher updates state.

```kotlin
// showcase/ShowcaseApp.kt
@Composable
fun ShowcaseApp() {
    var currentScheme by remember { mutableStateOf(AeroColorScheme.AeroBlue) }

    AeroTheme(colorScheme = currentScheme) {
        Column {
            // Theme switcher — Material3 SegmentedButton (replaced with AeroSegmentedControl in Phase 2)
            ThemeSwitcher(
                current = currentScheme,
                onSelect = { currentScheme = it }
            )
            // Foundation demo section
            FoundationSection()
            // Placeholders for Phase 2+
            PlaceholderSection("Buttons", "coming Phase 2...")
            PlaceholderSection("Input", "coming Phase 2...")
        }
    }
}
```

### Anti-Patterns to Avoid

- **Stacked .background() + .border() for glass:** Each is a separate draw pass. At 20+ glass components on screen, this collapses frame rate on integrated graphics. Always consolidate into `drawBehind {}`.
- **Passing AeroColorScheme as a parameter to every component:** Prop drilling. Use `AeroTheme.colors` (CompositionLocal) inside each component. The mordred GlassModifiers.kt already uses explicit params — the Phase 1 refactor removes them.
- **Using `compositionLocalOf` instead of `staticCompositionLocalOf` for theme tokens:** Adds per-frame tracking overhead for values that almost never change. Use `staticCompositionLocalOf` for the color scheme and typography.
- **`error("not provided")` as default in `staticCompositionLocalOf`:** Crashes Compose Previews. Use the default preset as the default value.
- **Calling `LocalAeroColors.current` inside a non-`@Composable` Modifier extension:** Compile error. Must use `@Composable` Modifier extension, `composed {}`, or pass colors as a parameter.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Multi-theme color propagation | Custom event bus / singleton for colors | `staticCompositionLocalOf` + `CompositionLocalProvider` | CompositionLocal is the Compose contract for implicit data propagation; custom solutions break scoping and testability |
| Material3 dark color palette bridge | Manually set MaterialTheme.colorScheme fields | `darkColorScheme(primary = colorScheme.primary, ...)` | One function call creates the full Material3 dark palette correctly; missing fields default correctly |
| Typography TextStyle instances | Custom font loading and TextStyle factory | Declare `TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)` directly | System font rendering on Desktop; no custom TTF loading needed in Phase 1 |
| Gradle multi-module structure | Monorepo scripts | `settings.gradle.kts include(":library", ":showcase")` + project dependency | Native Gradle feature; no extra tooling needed |
| explicitApi enforcement | Code review checklists | `explicitApi()` in `kotlin {}` block of library build.gradle.kts | Compiler enforces at build time; zero runtime cost |

**Key insight:** The Compose CompositionLocal system handles all the "pass colors to every component without prop drilling" complexity that custom solutions attempt to solve manually. Trust the framework.

---

## Common Pitfalls

### Pitfall 1: Glass Modifiers Readable from Non-Composable Scope

**What goes wrong:** Attempting to call `LocalAeroColors.current` inside a regular `fun Modifier.glassEffect()` extension — compile error because Modifier extensions are not `@Composable`.

**Why it happens:** Developers familiar with `Modifier.padding()` etc. try to write modifiers the same way and forget CompositionLocal reads require `@Composable` context.

**How to avoid:** Declare the modifier as `@Composable fun Modifier.glassEffect(...)` — this is valid Kotlin and the correct pattern for modifiers that need CompositionLocal access. Alternatively use `Modifier.composed { val colors = LocalAeroColors.current; glassEffect(colors) }` (older but still supported pattern).

**Warning signs:** `@Composable invocations can only happen from the context of a @Composable function` compile error when trying to call the modifier.

### Pitfall 2: mordred GlassModifiers Overdraw Not Fixed

**What goes wrong:** Porting mordred's `GlassModifiers.kt` verbatim — it stacks `.background(brush)` + `.background(cardBackground)` + `.border()` as separate Modifier chain entries = 3 draw passes = overdraw. FOUND-07 explicitly requires a single `drawBehind` block.

**Why it happens:** The mordred implementation is correct for an application (acceptable overdraw for a few components) but wrong as a library pattern scaled to 50+ components.

**How to avoid:** Rewrite the drawing logic inside `drawBehind {}` using `DrawScope.drawRoundRect()` for both the gradient fill and the border stroke. Do not use `.background()` or `.border()` in the glass modifier chain.

**Warning signs:** Layout Inspector's "Show Overdraw" shows red/dark-red on glass demo boxes in the Phase 1 showcase.

### Pitfall 3: AeroColorScheme Missing @Immutable

**What goes wrong:** Without `@Immutable`, the Compose compiler treats `AeroColorScheme` as potentially mutable. Every composable that reads it is marked as "unstable" and cannot be skipped during recomposition. Silent performance regression in consuming apps.

**Why it happens:** Developers forget that `data class` alone does not guarantee Compose-stability — the `@Immutable` annotation is required as an explicit contract.

**How to avoid:** Apply `@Immutable` from the `androidx.compose.runtime` package to `AeroColorScheme` and `AeroTypography` from the first commit. Verify with Compose compiler metrics output.

**Warning signs:** Compose compiler metrics report `AeroColorScheme` as "unstable"; composables using it are "not skippable".

### Pitfall 4: explicitApi() Applied to Wrong Module

**What goes wrong:** Applying `explicitApi()` to the `:showcase` module instead of (or in addition to) `:library`. Showcase internal composables require `public` annotations on every function — significant boilerplate.

**Why it happens:** `explicitApi()` is added at the root build.gradle.kts level instead of scoped to the library kotlin block.

**How to avoid:** Apply `explicitApi()` only inside the `:library` module's `kotlin {}` block:
```kotlin
// library/build.gradle.kts
kotlin {
    explicitApi()
    // ...
}
```
Showcase module has no `explicitApi()`.

**Warning signs:** Build error "Visibility must be specified explicitly" on showcase-internal composables that have no business being public.

### Pitfall 5: Default Value of staticCompositionLocalOf Uses error()

**What goes wrong:** `val LocalAeroColors = staticCompositionLocalOf<AeroColorScheme> { error("AeroTheme not found") }` crashes Compose Previews and any test that renders a library composable without wrapping in `AeroTheme {}`.

**Why it happens:** This pattern is used for required providers (e.g., NavController) where crashing is intentional. For a theme library, sensible defaults are always better.

**How to avoid:** Use the default preset: `staticCompositionLocalOf { AeroColorScheme.AeroBlue }`. Components outside `AeroTheme {}` render in AeroBlue rather than crashing — still documented as requiring `AeroTheme` for correct behavior, but Previews work.

**Warning signs:** Previews crash with "AeroTheme not found" error even for trivially-wrapped composables.

### Pitfall 6: Win11 undecorated+transparent Crash (Phase 3 blocker, Phase 1 investigation)

**What goes wrong:** `Window(undecorated = true, transparent = true)` triggers `EXCEPTION_ACCESS_VIOLATION` in Skiko on Windows 11. The crash is in native code and is not catchable.

**Why it happens:** Skiko's DirectX 12 renderer + Windows 11 DWM composition code path is unstable with `transparent = true` in some driver configurations.

**How to avoid for Phase 1:** The showcase uses a standard decorated window — no undecorated window in Phase 1. AeroTitleBar is Phase 3. However: STATE.md identifies this as a Phase 1 validation item — test `undecorated=true` WITHOUT `transparent=true` in the Phase 1 showcase to confirm no crash before Phase 3 commits to this approach.

**Warning signs:** JVM crash on window creation on Windows 11; stack trace mentions `skiko` native libs and `EXCEPTION_ACCESS_VIOLATION`.

---

## Code Examples

Verified patterns from mordred source (read directly):

### AeroColorScheme — Token Count Decision

The mordred `MordredColorScheme` has 29 fields. Strip these 5 domain-specific ones for Phase 1:
- `connectionActive` (satellite connection state)
- `connectionInactive` (satellite connection state)
- `executionHighlight` (execution log highlight)
- `logBackground` (log panel background)
- `timeStampBackground` (log timestamp background)

Keep all 24 remaining fields. The CONTEXT.md says 20-22 but `secondary`/`onSecondary` should be included to maintain Material3 bridge compatibility, and `cardBackground` is useful even in Phase 1 for the demo boxes. The recommended count is **23 tokens** (all mordred fields minus the 5 stripped domain-specific ones plus keeping secondary/onSecondary).

### AeroTypography — Port from mordred MordredTheme.kt

```kotlin
// Source: mordred/MordredTheme.kt Typography instance — extracted to standalone data class

@Immutable
public data class AeroTypography(
    val title: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    val bodyLarge: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val bodyMedium: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    val bodySmall: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
)
```

Color applied at call site via `LocalContentColor` or explicit `color` param — not baked into TextStyle in the data class (allows reuse across themes).

### Material3 Typography bridge in AeroTheme

```kotlin
// Source: mordred/MordredTheme.kt — typography bridging pattern

val material3Typography = Typography(
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
)
// Pass to MaterialTheme(typography = material3Typography)
```

### Showcase Window (Phase 1)

```kotlin
// showcase/Main.kt — standard decorated window; no undecorated/transparent

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "aero-compose-ui Showcase",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        ShowcaseApp()
    }
}
```

### theme switcher in ShowcaseApp

```kotlin
// showcase/ShowcaseApp.kt
@Composable
fun ShowcaseApp() {
    var currentScheme by remember { mutableStateOf(AeroColorScheme.AeroBlue) }
    AeroTheme(colorScheme = currentScheme) {
        val colors = AeroTheme.colors
        Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Theme switcher row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "AeroBlue" to AeroColorScheme.AeroBlue,
                        "AeroDark" to AeroColorScheme.AeroDark,
                        "Classic" to AeroColorScheme.Classic
                    ).forEach { (name, scheme) ->
                        Button(
                            onClick = { currentScheme = scheme },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentScheme == scheme) colors.primary else colors.surface
                            )
                        ) { Text(name) }
                    }
                }
                Spacer(Modifier.height(24.dp))
                // Foundation demo
                FoundationSection()
                Spacer(Modifier.height(16.dp))
                // Future phase placeholders
                Text("Buttons — coming Phase 2...", color = colors.labelText)
                Text("Input — coming Phase 2...", color = colors.labelText)
            }
        }
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Modifier.composed {}` for CompositionLocal in modifiers | `@Composable fun Modifier.extension()` | CMP 1.6+ / stable in 1.8+ | Cleaner syntax; `composed {}` still works but is less idiomatic |
| Separate `MordredThemePresets` object | Companion object properties on `AeroColorScheme` | Design decision for this project | More discoverable — `AeroColorScheme.AeroBlue` vs `MordredThemePresets.AeroBlue` |
| Separate `.background()` + `.border()` in Modifier chain | Single `drawBehind {}` with multiple draw calls | Skiko performance awareness in CMP | Eliminates overdraw; critical for iGPU performance |
| `kotlin("jvm")` for library module | `kotlin("multiplatform")` with `jvm("desktop")` target | Gradle KMP convention | Enables future multiplatform expansion; correct publication structure |

**Deprecated/outdated (avoid):**
- `Modifier.composed {}`: Still works, but `@Composable Modifier` extension is preferred
- `currentOs` in library build: Wrong — forces OS-native Skiko binary into the JAR; use `compose.desktop.common`
- Groovy `build.gradle` files: No type safety; use Kotlin DSL `.kts` throughout

---

## Open Questions

1. **`@Composable Modifier` extension vs `Modifier.composed {}`**
   - What we know: Both work for CompositionLocal access in modifiers; `@Composable Modifier` extension is the modern pattern; `composed {}` is deprecated-in-spirit but not removed
   - What's unclear: Whether `@Composable Modifier` extensions behave identically to non-composable ones for inline optimization
   - Recommendation: Use `@Composable Modifier` extension for Phase 1; if performance profiling shows a regression vs explicit parameter passing, evaluate `Modifier.Node` at that time

2. **Whether to include `secondary`/`onSecondary` in Phase 1 AeroColorScheme**
   - What we know: Material3 `darkColorScheme()` expects `secondary`/`onSecondary`; mordred has them; Phase 2 buttons may use them
   - What's unclear: Whether any Phase 1 component actually uses them (showcase only shows glass demo boxes)
   - Recommendation: Include — removing them later is a breaking API change; adding now costs nothing and prevents a breaking change in Phase 2

3. **Win11 undecorated+transparent crash status in CMP 1.10.3**
   - What we know: Issue #3757 was filed against an older CMP version; STATE.md says "may have been patched — test in Phase 1"
   - What's unclear: Whether CMP 1.10.3 / Skiko current version has fixed this
   - Recommendation: Add a validation task in Phase 1 — create a minimal `Window(undecorated = true)` in showcase and verify no crash on Windows 11. This is a Phase 3 prerequisite.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | None detected — greenfield project; no test infrastructure exists |
| Config file | None — Wave 0 must create test setup |
| Quick run command | `./gradlew :library:test` (after Wave 0 setup) |
| Full suite command | `./gradlew test` |

**Note:** Phase 1 is almost entirely visual/structural — theme tokens, glass rendering, Gradle structure. The most meaningful validation is visual (run the showcase, see correct colors) and structural (compile-time enforcement via `explicitApi()`). Traditional unit tests cover only the deterministic parts.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FOUND-01 | AeroTheme {} provides colors to child composables | unit — CompositionLocal read test | `./gradlew :library:test --tests "*.AeroThemeTest.providesColorsToChildren"` | Wave 0 |
| FOUND-02 | AeroColorScheme is @Immutable data class with expected fields | unit — reflection check | `./gradlew :library:test --tests "*.AeroColorSchemeTest.*"` | Wave 0 |
| FOUND-03 | AeroBlue preset has correct background and accent hex values | unit — value assertion | `./gradlew :library:test --tests "*.AeroColorSchemeTest.aeroBluePrimaryIsCorrect"` | Wave 0 |
| FOUND-04 | AeroDark preset has correct background and accent hex values | unit — value assertion | same test class | Wave 0 |
| FOUND-05 | Classic preset has correct background and accent hex values | unit — value assertion | same test class | Wave 0 |
| FOUND-06 | copy() on AeroColorScheme produces new scheme with changed token | unit — copy() assertion | `./gradlew :library:test --tests "*.AeroColorSchemeTest.copyChangesToken"` | Wave 0 |
| FOUND-07 | glassEffect modifier does not use multiple background layers | manual-only | n/a — verified via Layout Inspector overdraw view in showcase | manual |
| FOUND-08 | glassPanel and glassSurface modifiers exist and compile | unit — compile-time smoke | `./gradlew :library:compileKotlin` (succeeds = passes) | implicit |
| FOUND-09 | AeroTypography has fields at correct sp sizes | unit — value assertion | `./gradlew :library:test --tests "*.AeroTypographyTest.*"` | Wave 0 |
| FOUND-10 | explicitApi() enforced — internal type causes compile error | build-time enforcement | `./gradlew :library:compileKotlin` fails if non-public type missing visibility | implicit |
| SHW-01 | :showcase launches without crash | smoke — manual or launch test | `./gradlew :showcase:run` (exits 0 = pass) | implicit |
| SHW-02 | Theme switcher changes colors — verified visually | manual-only | n/a — screenshot comparison requires visual tooling not in scope | manual |
| SHW-03 | Showcase structure exists with placeholders | build test | `./gradlew :showcase:compileKotlin` | implicit |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test` (unit tests, ~15 seconds)
- **Per wave merge:** `./gradlew test` (full suite)
- **Phase gate:** Full suite green + manual showcase visual check before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt` — covers FOUND-02..06
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeTest.kt` — covers FOUND-01
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroTypographyTest.kt` — covers FOUND-09
- [ ] Test framework: Kotlin JUnit5 test dependency in library build.gradle.kts:
  ```kotlin
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  ```

---

## Sources

### Primary (HIGH confidence)

- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/ColorScheme.kt` — all 29 token names and hex values for three presets; verified directly
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/GlassModifiers.kt` — current modifier implementation (overdraw pattern identified); verified directly
- `C:/1A_WORK/lastver_131/mordred/src/main/kotlin/presentation/ui/theme/MordredTheme.kt` — CompositionLocalProvider pattern, Material3 bridge, Typography instance; verified directly
- `.planning/research/STACK.md` — Kotlin 2.3.21, CMP 1.10.3, Gradle 9.4.1 versions; HIGH confidence (JetBrains official sources cited therein)
- `.planning/research/ARCHITECTURE.md` — staticCompositionLocalOf pattern, Modifier approaches, anti-patterns; HIGH confidence (Android official docs + mordred source)
- `.planning/research/PITFALLS.md` — overdraw pitfall, Win11 crash, explicitApi, @Immutable; HIGH confidence (JetBrains issue tracker + Android official docs)

### Secondary (MEDIUM confidence)

- `.planning/research/FEATURES.md` — feature landscape, MVP scope, competitor analysis (Jewel); MEDIUM (Jewel/ecosystem HIGH, Aero-specific patterns MEDIUM)
- `.planning/STATE.md` — Win11 undecorated+transparent validation task noted; MEDIUM (project decision record)

### Tertiary (LOW confidence)

- None — all material for Phase 1 is either from official sources or directly-read source code.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions from `.planning/research/STACK.md` which cites JetBrains official releases
- Architecture: HIGH — patterns directly from mordred source code + Android CompositionLocal official docs
- Pitfalls: HIGH (critical) / MEDIUM (performance numbers) — Win11 crash from JetBrains issue tracker; overdraw from official Compose performance docs

**Research date:** 2026-04-27
**Valid until:** 2026-05-27 (stable domain; CMP releases are roughly quarterly)
