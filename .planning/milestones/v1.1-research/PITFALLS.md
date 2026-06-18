# Pitfalls Research

**Domain:** Custom vector icon set introduction + dependency removal in Compose Desktop library (aero-compose-ui v1.1)
**Researched:** 2026-04-28
**Confidence:** HIGH (sourced from direct codebase inspection + Compose/Kotlin source knowledge)

---

## ⚠ SOURCE REVISION (2026-04-28, supersedes Feather references below)

**Icon source changed from Feather to Phosphor Regular.** Most pitfalls in this document (lazy init, materialIconsExtended dep removal, AeroBreadcrumb separator API, tint discipline, test-file imports, JAR size verification, AeroSearchField "x" missed by glyph greps) are **source-agnostic and apply unchanged**.

**Pitfalls that need re-calibration for Phosphor:**

1. **Pitfall 6 / 11 — Stroke contrast at small sizes:** Phosphor Regular stroke is 16 in a 256-unit viewBox = `16/256 ≈ 6.25%` of dimension. At 24dp render the stroke is `24 × 0.0625 = 1.5dp`; at 16dp render it's `16 × 0.0625 = 1.0dp`; at 10dp (AeroNumberSpinner button context) it's `10 × 0.0625 ≈ 0.63dp` — sub-pixel at 96 DPI, will antialias to a faint line. **Same risk as Feather**, though slightly thicker stroke than originally assumed. Mitigation unchanged: Canvas-draw the spinner chevrons explicitly at the small size, OR raise spinner button to 14dp+, OR use Phosphor `Bold` weight icons just for spinner (rejected per "one weight only" decision).

2. **Pitfall 12 — SVG conversion artifacts:** All conversion-bug categories still apply, but the specific values to verify in generated `.kt` change:
   - `viewportWidth`/`viewportHeight` must be `256f` (NOT `24f`)
   - `strokeLineWidth` must be `16f` (NOT `2f`)
   - `defaultWidth`/`defaultHeight` stays at `24.dp`
   - `strokeLineCap = StrokeCap.Round`, `strokeLineJoin = StrokeJoin.Round` (unchanged — Phosphor also uses round)
   - `fill = Color.Transparent` for stroke-only icons (Phosphor Regular is pure stroke; some icons also use small filled shapes — verify per-icon when conversion completes)
   - **Spot-check 5 representative Phosphor icons** before batch-converting all 150 (per Stack research recommendation).

3. **Phosphor-specific hygiene:**
   - SVG source committed to `tools/phosphor-svgs/regular/` (NOT `tools/feather-svgs/`)
   - Pinned to a specific commit hash from `phosphor-icons/core` repo (not "latest" — Phosphor has multiple weight branches; only `raw/regular/` is in scope)
   - Naming gotcha: Phosphor names differ from Material/Feather conventions. Migration touchpoints in code use Phosphor names (`AeroIcons.X` for close, `AeroIcons.CaretDown` for chevron-down, `AeroIcons.MagnifyingGlass` for search, `AeroIcons.Gear` for settings, `AeroIcons.House` for home). See FEATURES.md for full mapping. Pitfall: developers used to Material may type `AeroIcons.Close` and not find it.

All other pitfalls (lazy init pattern, JAR size verification, materialIconsExtended removal order, AeroBreadcrumb API, test imports, tint discipline, cross-OS glyph removal benefits, KDoc requirements, build-time vs commit-checked-in) apply verbatim with "Feather" mentally substituted by "Phosphor Regular".

---

## Critical Pitfalls

### Pitfall 1: ImageVector Eager Initialization Explodes Startup (120+ objects)

**What goes wrong:**
Declaring 120-150 `val Close: ImageVector = ...` (non-lazy) at the top level of an `AeroIcons` object forces every single ImageVector to be fully constructed at class-load time. Each ImageVector involves allocating a `VectorGroup` tree, path data parsing, and `PathBuilder` calls. At 120+ icons that is a measurable startup spike — benchmark reports from community ports (e.g. Feather-Compose) show 80-200ms added to first-frame time when all icons load eagerly on a mid-range desktop JVM.

**Why it happens:**
Developers see the Material Icons pattern (`Icons.Outlined.Info`) and assume they just need a similar singleton object. They do not realize Material Icons uses a nullable-cached private field pattern internally:
```kotlin
private var _info: ImageVector? = null
val Icons.Outlined.info: ImageVector
    get() {
        if (_info != null) return _info!!
        _info = materialIcon(name = "Outlined.Info") { ... }
        return _info!!
    }
```
This is a manually inlined lazy — the field is null until first access, then cached forever. Using `by lazy { }` is the Kotlin idiomatic equivalent but the Material source uses the nullable-field pattern to avoid the `Lazy<T>` wrapper allocation (micro-optimization, not required for 120 icons).

**How to avoid:**
Use `by lazy { }` on every icon constant in `AeroIcons`:
```kotlin
public object AeroIcons {
    public val Close: ImageVector by lazy { buildCloseVector() }
    public val ChevronDown: ImageVector by lazy { buildChevronDownVector() }
    // ...
}
```
`by lazy` is thread-safe (SYNCHRONIZED mode by default), causes zero allocation until first access, and does not interfere with recomposition — `ImageVector` is consumed by `Icon()` as a stable value and does not participate in Compose state. There is no recomposition issue from using `by lazy` because `ImageVector` is not a `State<T>`, `MutableState<T>`, or `@Stable`-tracked value.

Do NOT use `val foo: ImageVector = ...` (eager). Do NOT reach for `remember { }` at call sites — that puts allocation inside composition which is wrong; the singleton lazy is the correct level.

**Warning signs:**
- If `AeroIcons` object initialization appears in profiler traces at startup
- If the Showcase's `IconsSection` (which will reference all ~150 icons at once) causes a noticeable first-frame hitch

**Phase to address:** icons-foundation phase (when `AeroIcons.kt` is first written — bake in the pattern from the start)

---

### Pitfall 2: Removing `materialIconsExtended` Does Not Compile Until ALL Import Sites Are Purged

**What goes wrong:**
`build.gradle.kts` has `implementation(compose.materialIconsExtended)`. Removing that line causes an immediate compile failure at every file that still imports from `androidx.compose.material.icons.*`. The exact files confirmed by codebase inspection:

- `AeroAlertKind.kt` — 4 Material icon imports, used in `.icon` property
- `AeroBannerKind.kt` — 4 Material icon imports, used in `.icon` property
- `AeroAlertKindTest.kt` — imports + asserts against `Icons.Outlined.*`
- `AeroBannerKindTest.kt` — imports + asserts against `Icons.Outlined.*`

The test files are particularly treacherous: removing the dep and migrating the source files will still produce a compile failure if the test files are missed, because `assertEquals(Icons.Outlined.Info, AeroAlertKind.Info.icon)` imports from the now-removed dep.

**Why it happens:**
Developers remove the Gradle dep, migrate the production source, run the build, and get a cryptic Unresolved reference error from the test directory — a directory they may not have checked since the tests were "just validating the old behavior."

**How to avoid:**
1. Before removing the dep line, run: `grep -r "material.icons" library/src/ --include="*.kt" -l`
   This surfaces all 4 files (2 source + 2 test).
2. Migrate production source files to `AeroIcons.*` first.
3. Rewrite test assertions — `AeroAlertKindTest` and `AeroBannerKindTest` must be rewritten to assert against `AeroIcons.*` references instead of `Icons.Outlined.*`.
4. Only then remove `compose.materialIconsExtended` from `build.gradle.kts`.
5. Verify with `./gradlew :library:dependencies --configuration compileClasspath | grep materialIcons` — must return nothing.

**Warning signs:**
- Build fails with `Unresolved reference: Icons` after removing the dep
- `./gradlew :library:dependencies` still shows `materialIconsExtended`

**Phase to address:** migration phase (icons-foundation creates `AeroIcons`; migration phase removes the dep after all consumers are ported)

---

### Pitfall 3: AeroBreadcrumb `separator: String` — Breaking API Change on a v1.1 Release

**What goes wrong:**
`AeroBreadcrumb` has `separator: String = ">"` as a public parameter (confirmed at `AeroBreadcrumb.kt` line 43). If v1.1 changes this to `separator: ImageVector = AeroIcons.ChevronRight`, any caller who explicitly passed a custom `String` separator (e.g., `separator = "/"` or `separator = ">"`) gets a compile error. This is a source-incompatible (breaking) change.

Even callers who never touched the parameter are safe (default value substitution), but callers with explicit string arguments break. For a library that is distributed as a JAR, this is a binary-breaking change regardless.

**Why it happens:**
Developers think "it is just a default change, no one will notice." In reality, any consumer who wrote `AeroBreadcrumb(items = ..., onItemClick = ..., separator = ">")` (explicit string) will get a type mismatch compile error after upgrading.

**How to avoid:**
Choose one of three strategies:

Option A — Keep `separator: String`, do NOT migrate AeroBreadcrumb to ImageVector. Render separator as `Text(separator, ...)` as today. Safe but inconsistent with the icon migration goal.

Option B — Add an overload, deprecate the String variant:
```kotlin
@Deprecated("Use ImageVector separator", ReplaceWith("..."))
public fun AeroBreadcrumb(items, onItemClick, modifier, separator: String = ">") { ... }

public fun AeroBreadcrumb(items, onItemClick, modifier, separator: ImageVector = AeroIcons.ChevronRight) { ... }
```
This is source-compatible for existing callers (they compile with deprecation warning, not error) and gives a migration path.

Option C — Change to ImageVector, bump to v2.0. Cleanest API but justifies a semver major bump since the public `separator: String` parameter change is binary-breaking.

**Recommendation:** Option B for v1.1. The `separator` parameter is unusual enough (most callers use the default `>`) that a deprecation warning is the right trade-off. Option C is correct semver but disproportionate for a single parameter type change on a library not yet published to Maven Central. Document the deprecation.

**Warning signs:**
- Trying to compile a downstream consumer project after the change will immediately surface the break

**Phase to address:** migration phase (decision must be made before touching the AeroBreadcrumb signature)

---

### Pitfall 4: Test Files Hardcode Material Icons Identity — Compile Failure if Not Rewritten

**What goes wrong:**
`AeroAlertKindTest.kt` asserts: `assertEquals(Icons.Outlined.Info, AeroAlertKind.Info.icon)` and imports `androidx.compose.material.icons.outlined.Info`. After migration, `AeroAlertKind.Info.icon` returns `AeroIcons.Info` (or whatever name is chosen). The import will cause a compile failure once `materialIconsExtended` is removed from `build.gradle.kts`.

`AeroBannerKindTest.kt` has the same pattern for all 4 banner kinds — confirmed by codebase inspection.

**Why it happens:**
The tests were written to verify the specific Material Icons instance returned. After migration, the identity check (`assertEquals` on `ImageVector` references) must point to the new `AeroIcons.*` constants.

**How to avoid:**
As part of the migration phase, rewrite both test files:
- `AeroAlertKindTest.kt`: change imports and assertions to reference `AeroIcons.*` constants
- `AeroBannerKindTest.kt`: same

Since `ImageVector` does not override `equals()` meaningfully (it is compared by reference in practice), use `assertSame` (reference equality) or compare `.name` property: `assertEquals("AeroIcons.InfoCircle", AeroAlertKind.Info.icon.name)`.

**Warning signs:**
- After migration: `./gradlew :library:test` produces compile errors citing `Icons.Outlined.*` — test files not updated
- `grep -r "material.icons" library/src/test/` returns any results after migration is declared complete

**Phase to address:** migration phase (test rewrite is part of the same plan that migrates the enum source files)

---

### Pitfall 5: Tint Disappears on Dark Themes — `Icon()` Default Tint is `LocalContentColor`

**What goes wrong:**
`androidx.compose.material3.Icon()` applies `tint = LocalContentColor.current` when no explicit `tint` is passed. In AeroTheme, `LocalContentColor` is NOT explicitly set via `CompositionLocalProvider` — it inherits the Material3 default. If the Material3 default `LocalContentColor` happens to be dark (e.g., black or near-black on a surface that Material3 considers "light"), icons on AeroDark's near-black surfaces will be invisible.

Current library code is mostly correct: `AeroNotificationBanner.kt` line 55 uses `tint = accent` (explicit). `AeroContextMenu.kt` lines 139 and 175 use `tint = colors.onSurface` (explicit). But future AeroIcons usage in migrated components must maintain this discipline, and it is not enforced by the compiler.

**Why it happens:**
Developers see icons "look fine" in AeroBlue theme (moderately dark, M3 tokens resolve acceptably) and ship. The issue surfaces on AeroDark (background `#0A0A1A`) or Classic theme where M3's `LocalContentColor` may resolve to an unexpected color. They do not test all three themes.

**How to avoid:**
1. In all library components: always pass explicit `tint = colors.onSurface` (or the appropriate semantic color) — never rely on `LocalContentColor`.
2. Document in KDoc of `AeroIcons`: "Always pass an explicit `tint` to `Icon()`. `LocalContentColor` is not set by `AeroTheme` and may produce invisible icons on dark surfaces."
3. In the showcase `IconsSection`, render all icons with `tint = AeroTheme.colors.onSurface` explicitly, and test in all three themes before sign-off.

**Warning signs:**
- Icons render as black blocks or are invisible on AeroDark/Classic showcase visual checkpoint
- Any `Icon()` call in library source without a `tint` parameter

**Phase to address:** cross-cutting (set in icons-foundation for all AeroIcons usage patterns; verified in showcase phase against all three themes)

---

### Pitfall 6: Feather Stroke Icons on Dark Surfaces — Disabled State Contrast

**What goes wrong:**
Feather icons are pure stroke (no fill). On AeroDark theme (background `#0A0A1A`, surface near-black), a disabled icon rendered at 40% opacity will have a tint of approximately `colors.onSurface.copy(alpha = 0.4f)` — which on a very dark background may have a contrast ratio below 3:1 (WCAG AA minimum for non-text). Material Icons (mostly Filled variants) sidestep this because the filled shape provides more pixel mass at the same opacity.

Specifically: `AeroNumberSpinner` currently uses `Text("▲", fontSize = 8.sp)` inside a `16.dp x 12.dp` button at `0.4f` alpha for disabled state. After migration to a Feather `ChevronUp` icon at that size, the stroke-based rendering at 40% opacity on a dark glass surface could be nearly invisible.

**Why it happens:**
Stroke-only icons rely entirely on their tint color for visibility. Reducing opacity of a stroke icon on a matching-darkness surface reduces contrast faster than a filled icon would.

**How to avoid:**
1. For disabled states: use `alpha = 0.5f` as the minimum floor rather than `0.4f` for stroke icons.
2. In AeroNumberSpinner's up/down buttons specifically: after migration, visually verify the disabled state in AeroDark theme against the `glassSurface` background. The small button area (16x12dp) compounds the issue.
3. Hover state: icons inside `AeroIconButton` should use `colors.onSurface` tint at full opacity regardless of hover — the hover overlay provides the visual feedback, not icon dimming.

**Warning signs:**
- Disabled up/down arrows in AeroNumberSpinner are nearly invisible in AeroDark showcase visual checkpoint
- Any disabled icon with `alpha < 0.5f` on a dark surface

**Phase to address:** migration phase (visual checkpoint for disabled icon states across all three themes required before sign-off on each migrated component)

---

## Moderate Pitfalls

### Pitfall 7: `Icon()` Composable Is NOT Removed by Removing `materialIconsExtended`

**What goes wrong:**
Developers conflate the icon SET dependency (`compose.materialIconsExtended` — the ~7-9MB JAR of `ImageVector` data) with the `Icon()` composable function (which lives in `compose.material3`). Removing `materialIconsExtended` does NOT remove `androidx.compose.material3.Icon`. All existing `Icon()` call sites in `AeroNotificationBanner.kt` and `AeroContextMenu.kt` continue to compile and work normally.

**Why it happens:**
The naming is confusing: "Material Icons" in the Gradle dependency refers to the icon data set. The `Icon()` composable is a separate artifact in `material3`.

**How to avoid:**
State explicitly in the migration plan success criteria: "We remove `compose.materialIconsExtended` (the data). We keep `androidx.compose.material3.Icon` (the composable). All `Icon(imageVector = ...)` call sites continue to compile." Verify: `./gradlew :library:dependencies | grep materialIcons` returns nothing.

**Phase to address:** migration phase (clarify in plan success criteria)

---

### Pitfall 8: `AeroNumberSpinner` Glyph Size vs. Icon Stroke at Small Sizes

**What goes wrong:**
`AeroNumberSpinner` uses `Text("▲", fontSize = 8.sp)` inside a `16.dp x 12.dp` button. If replaced with a 24dp-viewBox Feather `ChevronUp` icon at `Modifier.size(10.dp)`, the stroke width (2px in a 24x24 viewBox) at 10dp renders as `(2/24)*10 = 0.83dp` — less than 1 physical pixel at 96dpi, which antialiases to near-invisible.

**How to avoid:**
For AeroNumberSpinner specifically, use one of:
1. Keep Unicode `▲▼` text glyphs — they render reliably via system font at any size and the spinner buttons are too small for the icon system anyway.
2. Use the Canvas approach already established in `AeroSearchField.kt` (magnifier drawn via `drawCircle`/`drawLine` with explicit `strokeWidth`) — draw chevron-up/down at native pixel density.
3. If using `Icon()`, bump the button height to 16dp minimum and use `Modifier.size(12.dp)` on the icon — larger target gives more viable stroke.

Option 2 is most robust. AeroSearchField demonstrates the pattern; it should be replicated.

**Warning signs:**
- Up/down arrows in AeroNumberSpinner look faint or disappear at standard DPI in showcase visual checkpoint after migration

**Phase to address:** migration phase (flag AeroNumberSpinner as needing the Canvas approach rather than `Icon()`)

---

### Pitfall 9: SVG-to-ImageVector Conversion Artifacts

**What goes wrong:**
Common conversion bugs when porting Feather SVGs to `ImageVector` Kotlin code:

1. **Wrong `defaultWidth`/`defaultHeight`**: Feather uses 24x24 viewBox. A generator that outputs `defaultWidth = 512.dp` causes the icon to render at 512dp by default.
2. **Fill vs stroke confusion**: Feather is stroke-based (`stroke="currentColor"`, `fill="none"`). A generator defaulting to fill mode produces solid filled shapes — visually wrong and inconsistent with the Aero outline aesthetic.
3. **Transform groups not flattened**: SVGs with `<g transform="...">` may produce a `VectorGroup` with a rotation/scale. At 24dp this may be invisible; at 48dp it shows as off-center icons.
4. **Missing or wrong viewport**: `viewportWidth = 24f` and `viewportHeight = 24f` are required. A generator defaulting to 1f or 100f scales path data incorrectly.
5. **Stroke linecap/linejoin not set**: Feather uses `stroke-linecap="round"` and `stroke-linejoin="round"`. Generated code must set `strokeLineCap = StrokeCap.Round` and `strokeLineJoin = StrokeJoin.Round` on each `path { }` node — otherwise corners appear sharp and angular, breaking the Feather visual identity.

**How to avoid:**
After generating `AeroIcons.kt`, perform a manual spot-check on 5 icons of different shapes (X, chevron, circle, complex path, multi-path):
- Render each at 24dp in AeroBlue showcase — verify visual match to Feather reference SVG
- Render each at 16dp and 32dp — verify stroke does not become filled or disappear
- Grep the generated file: `grep "fill = SolidColor" AeroIcons.kt` — all fills for Feather icons should be `Color.Transparent`; any non-transparent fill is suspicious
- Verify `defaultWidth = 24.dp` and `defaultHeight = 24.dp`
- Verify `viewportWidth = 24f` and `viewportHeight = 24f`

**Warning signs:**
- Any icon that appears "filled" (solid blob) instead of outline strokes
- Icons that appear off-center or clipped at certain sizes

**Phase to address:** icons-foundation phase (verification before any icon is used in a component)

---

### Pitfall 10: AeroTitleBar Glyphs Render Differently Cross-OS

**What goes wrong:**
`AeroTitleBar.kt` uses Unicode glyphs in `TitleBarButton`:
- `"─"` (U+2500 Box Drawings Light Horizontal) for Minimize
- `"□"` / `"❒"` (U+25A1 / U+2752) for Maximize/Restore
- `"✕"` (U+2715) for Close

These are rendered via `Text()`, which falls back to the system font. On Windows 11, Segoe UI covers all these glyphs. On macOS, `□` maps to a different glyph in San Francisco. On Linux without good fallback fonts, `"❒"` may render as a tofu replacement box. The project targets Windows primarily but says Linux/macOS are secondary — inconsistent rendering is a real risk.

Migrating to `AeroIcons.Minus`, `AeroIcons.Square`, `AeroIcons.X` as `ImageVector` eliminates font-fallback dependency entirely and renders identically on all platforms.

**How to avoid:**
Migrate all `TitleBarButton` glyphs to `Icon(imageVector = ...)` in the AeroTitleBar migration plan. Call out cross-OS consistency as the primary motivation in the plan rationale. Include a visual checkpoint note: "verify AeroTitleBar on Windows primary; flag for macOS/Linux if test matrix available."

**Warning signs:**
- Title bar buttons show replacement boxes or wrong shapes on macOS/Linux during manual testing

**Phase to address:** migration phase (AeroTitleBar is one of the named components to migrate)

---

### Pitfall 11: `explicitApi()` + `by lazy` Delegated Properties

**What goes wrong:**
`explicitApi()` is active in `:library/build.gradle.kts` (confirmed at line 9). It requires explicit visibility modifiers on all public declarations. A `val` declared without `public` in `object AeroIcons` produces:
```
Visibility must be specified in explicit API mode
```

**How to avoid:**
Declare all icon constants as `public val Name: ImageVector by lazy { ... }` member properties inside `public object AeroIcons`. The `public` applies to the property declaration; `by lazy` applies to the initializer. These are orthogonal — no interaction issue. Run `./gradlew :library:compileKotlin` after writing the first 5 icons to confirm `explicitApi()` is satisfied before generating the remaining 145.

**Warning signs:**
- Compiler error: "Visibility must be specified in explicit API mode" on any line in `AeroIcons.kt`

**Phase to address:** icons-foundation phase (first-compile check after writing initial icons)

---

### Pitfall 12: Build Hygiene — Generated File Location and Regeneration Traceability

**What goes wrong:**
If `AeroIcons.kt` is placed in `build/generated/` (as a Gradle-generated source), it disappears on `./gradlew clean` and the build is not reproducible without the generator tool present. If it is placed in `src/main/kotlin/` without the SVG source also committed, there is no traceability to what was converted or how — making future icon additions or corrections opaque.

**How to avoid:**
Commit `AeroIcons.kt` directly to `src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` as ordinary source (not generated). Also commit the Feather SVG source subset (~150 selected icons, not all 280) to `tools/feather-svgs/` — this is the audit trail for what was converted. Do NOT add the SVG directory to `.gitignore`. The generator script goes in `tools/` and is run manually only when adding new icons. The Gradle build does not invoke the generator.

**Warning signs:**
- `./gradlew clean && ./gradlew :library:build` fails because `AeroIcons.kt` was only in `build/`
- No SVG source committed — no way to audit what was converted or regenerate faithfully

**Phase to address:** icons-foundation phase (repository structure decision before generating any icons)

---

### Pitfall 13: JAR Size Verification — Transitive Pull-in Risk

**What goes wrong:**
`compose.materialIconsExtended` is approximately 7-9 MB. Adding 120-150 `ImageVector` constants adds approximately 200-400 KB of compiled bytecode. Net impact: the library JAR shrinks by roughly 6-8 MB. However, if a downstream consumer of `:library` already has `compose.materialIconsExtended` in their own Gradle dependencies, it will appear in the consumer's classpath — but NOT in `:library`'s own compiled JAR. The library size reduction is real and permanent.

The risk: if `compose.material3` ever begins transitively including `materialIconsExtended` (it does not in CMP 1.7.x), the dep would return silently. This must be actively verified, not assumed.

**How to avoid:**
Run as a mandatory step in migration phase success criteria:
```
./gradlew :library:dependencies --configuration compileClasspath | grep -i materialIcons
```
Must return nothing. Also check the output JAR size:
```
ls -lh library/build/libs/*.jar
```
Document before/after sizes in the migration phase summary.

**Warning signs:**
- `./gradlew :library:dependencies` shows `materialIconsExtended` in compile classpath after removal attempt
- Library JAR size does not decrease after removal

**Phase to address:** migration phase (verification at completion)

---

### Pitfall 14: Showcase `ButtonsSection` Glyph Demos and `AeroContextMenu` Submenu Glyph

**What goes wrong:**
`ButtonsSection.kt` lines 50-52 use glyphs as demo content for `AeroIconButton`:
```kotlin
AeroIconButton(onClick = {}) { Text("▲", color = colors.onSurface) }
AeroIconButton(onClick = {}) { Text("▼", color = colors.onSurface) }
AeroIconButton(onClick = {}, enabled = false) { Text("×", color = colors.onSurface) }
```
These are showcase-only demos. After migration, leaving them as text glyphs contradicts the `IconsSection` demonstration of the icon system.

More critically, `AeroContextMenu.kt` line 183 uses `Text("▶", ...)` for the submenu indicator — this IS library internals. A library that claims to have removed all text glyphs but still renders `▶` via `Text()` in context menus is incomplete.

**How to avoid:**
After the migration phase, run a comprehensive glyph grep:
```
grep -rn 'Text("▲\|▼\|▶\|›\|✕\|✓\|─\|□\|❒' library/src/ showcase/src/
```
All hits must be resolved before the migration is declared complete. Showcase `ButtonsSection` glyph demos switch to `Icon(AeroIcons.ChevronUp/Down/X, ...)`. `AeroContextMenu` submenu indicator switches to `Icon(AeroIcons.ChevronRight, Modifier.size(12.dp), ...)`.

**Warning signs:**
- Glyph grep returns any hits in library source after migration
- Showcase visual reveals mismatched icon styles (glyphs alongside AeroIcons)

**Phase to address:** migration phase for library internals; showcase phase for ButtonsSection demos

---

### Pitfall 15: Recommended Icon Size Range — Stroke Readability Bounds

**What goes wrong:**
Feather is designed for 24dp at stroke-width 2. Below 16dp, the stroke-to-bound ratio produces strokes thinner than 1.33dp — at standard 96dpi, that is 1.28 physical pixels, which antialiases to a faint line. Above 40dp, the 2-unit stroke looks thin relative to icon bounds (ratio 2/24 = 8.3%), giving a wireframe appearance on Aero's dark surfaces. Neither extreme is Feather's intended use case.

**How to avoid:**
Document in `AeroIcons` KDoc and in a usage guide:
- Recommended range: 16dp-32dp
- Default render size: 24dp
- For very small contexts (12dp or below, e.g. AeroNumberSpinner buttons at 16x12dp): use Canvas-drawn custom shapes, not scaled-down `AeroIcons.*`
- For large display contexts (48dp+): acceptable but the stroke appears visually light; consumer may need a custom heavier-weight icon

**Warning signs:**
- Icons rendered at 12dp or smaller via `Icon()` look faint in visual checkpoint
- Icons at 48dp+ look like thin wireframes against AeroDark background

**Phase to address:** icons-foundation phase (document in AeroIcons KDoc; flag AeroNumberSpinner as the concrete exception)

---

### Pitfall 16: AeroSearchField Clear Button — Lowercase `"x"` is Not an Icon

**What goes wrong:**
`AeroSearchField.kt` line 121 uses `Text("x", ...)` — a lowercase Latin "x", not a Unicode multiplication sign or purpose-drawn X icon. This renders as a typographic character in the body font, visually inconsistent with any future `AeroIcons.X` icon in weight and stroke. This is exactly the class of glyph-as-icon usage the migration is meant to eliminate, but its lowercase form makes it easy to miss in grep patterns that search for uppercase or Unicode glyphs.

**How to avoid:**
The migration plan for AeroSearchField must explicitly include the clear button: replace `Text("x")` with `Icon(AeroIcons.X, Modifier.size(14.dp), tint = colors.labelText)`. Add lowercase `"x"` to the glyph grep pattern:
```
grep -rn 'Text("x"' library/src/
```

**Warning signs:**
- AeroSearchField's clear button looks like typed text rather than an icon in the showcase after migration is declared complete
- Glyph grep misses it because the search pattern targets Unicode glyphs only

**Phase to address:** migration phase (AeroSearchField migration plan explicitly includes the clear button)

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Keep `separator: String` in AeroBreadcrumb permanently | No API break | Separator is text-rendered; inconsistent with icon-based separators everywhere else | Acceptable for v1.1 Option A only if explicitly documented as intentional |
| Deprecated String overload (Option B) | Source-compatible migration | Two overloads to maintain; deprecation warning noise | Acceptable for v1.1; clean up in v2.0 |
| Use `Text("▲▼")` in AeroNumberSpinner permanently | Avoids Canvas complexity | Text glyphs remain in library after migration is "complete" | Never acceptable as permanent state |
| Skip SVG source commit | Saves repo space | No traceability to what was converted | Never acceptable |
| Eager `val` for all AeroIcons (non-lazy) | Simpler code | Startup spike; penalizes all consumers even those using 2 icons | Never acceptable for 120+ icons |
| Rely on LocalContentColor for tint | Less code in Icon call sites | Invisible icons on dark themes | Never acceptable in library code |

---

## "Looks Done But Isn't" Checklist

- [ ] **materialIconsExtended removal verified**: `./gradlew :library:dependencies --configuration compileClasspath | grep materialIcons` returns nothing
- [ ] **All glyph sites migrated**: `grep -rn 'Text("▲\|▼\|▶\|›\|✕\|✓\|─\|□\|❒\|"x"' library/src/` returns no icon-glyph hits
- [ ] **AeroAlertKindTest + AeroBannerKindTest rewritten**: `grep -r "material.icons" library/src/test/` returns nothing
- [ ] **AeroIcons lazy pattern**: every constant in `AeroIcons` uses `by lazy` — `grep -c 'by lazy' AeroIcons.kt` equals the total icon count
- [ ] **Three-theme visual checkpoint**: IconsSection renders with correct tint on AeroBlue, AeroDark, and Classic — no black or invisible icons
- [ ] **AeroNumberSpinner disabled state in AeroDark**: up/down controls visible at disabled alpha — visual checkpoint passes
- [ ] **AeroBreadcrumb API decision documented**: String kept with rationale, deprecated overload added, or v2.0 bump — decision recorded in CHANGELOG or plan summary
- [ ] **Feather SVG source committed**: `tools/feather-svgs/` contains the ~150 source SVG files
- [ ] **Icon KDoc written**: AeroIcons object documents recommended size range (16dp-32dp) and tint requirement
- [ ] **Spot-check 5 icons**: fill=transparent, viewportWidth=24f, strokeLineCap=Round verified in generated AeroIcons.kt
- [ ] **AeroContextMenu submenu indicator migrated**: `Text("▶")` replaced with `Icon(AeroIcons.ChevronRight, Modifier.size(12.dp))`
- [ ] **AeroSearchField clear button migrated**: `Text("x")` replaced with `Icon(AeroIcons.X, Modifier.size(14.dp))`

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Eager ImageVector init | icons-foundation | `by lazy` enforced; first-compile check after 5 icons |
| materialIconsExtended not fully removed | migration | `./gradlew :library:dependencies` compileClasspath grep |
| AeroBreadcrumb breaking API | migration | API decision documented before touching the signature |
| Test files still import Material Icons | migration | `grep -r "material.icons" library/src/test/` returns nothing |
| Tint missing on dark themes | icons-foundation (doc) + migration (enforcement) | Three-theme showcase visual checkpoint |
| Feather stroke at small sizes | icons-foundation (doc) + migration (AeroNumberSpinner) | AeroNumberSpinner visual checkpoint at 16x12dp in all themes |
| Icon composable not removed (misunderstanding) | migration | Stated explicitly in plan success criteria |
| AeroNumberSpinner tiny button vs icon stroke | migration | Visual checkpoint; Canvas approach if Icon stroke too thin |
| SVG conversion artifacts | icons-foundation | Spot-check 5 icons; fill/viewport/strokeLineCap grep |
| AeroTitleBar cross-OS glyphs | migration | Icon replacement; visual checkpoint |
| explicitApi() + lazy | icons-foundation | Compile after first 5 icons written |
| Generated file hygiene | icons-foundation | AeroIcons.kt in src/main; SVGs in tools/ |
| JAR size verification | migration | ls -lh on library JAR before/after; dep grep |
| Showcase ButtonsSection glyph demos | showcase | Glyph grep across library/src/ and showcase/src/ |
| AeroContextMenu submenu glyph | migration | Glyph grep includes `Text("▶")` |
| Icon size range documentation | icons-foundation | KDoc on AeroIcons object reviewed before merge |
| AeroSearchField "x" clear button | migration | Glyph grep includes `Text("x"` in library/src/ |

---

## Sources

- Direct inspection: `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroAlertKind.kt`, `AeroBannerKind.kt`, `AeroNotificationBanner.kt`, `AeroContextMenu.kt`, `AeroContextMenuItem.kt`, `AeroToastHost.kt`
- Direct inspection: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumb.kt` (line 43: `separator: String = ">"`)
- Direct inspection: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` (glyphs: `"─"`, `"□"`, `"❒"`, `"✕"`)
- Direct inspection: `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` (lines 129, 141: `Text("▲"/"▼", fontSize = 8.sp)`)
- Direct inspection: `library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt` (line 121: `Text("x")`)
- Direct inspection: `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt`, `AeroBannerKindTest.kt` (Material Icons imports in test assertions)
- Direct inspection: `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` (lines 50-52: AeroIconButton glyph demos)
- Direct inspection: `library/build.gradle.kts` (line 15: `implementation(compose.materialIconsExtended)`)
- Compose source (HIGH confidence): Material Icons nullable-field lazy pattern; `Icon()` tint default = `LocalContentColor.current` in `androidx.compose.material3`
- Kotlin language spec (HIGH confidence): `by lazy` + `explicitApi()` — visibility modifier on property declaration, delegate is orthogonal
- Feather Icons specification (HIGH confidence): 24x24 viewBox, stroke-width 2, stroke-linecap round, stroke-linejoin round, fill none

---
*Pitfalls research for: aero-compose-ui v1.1 — icon set introduction and materialIconsExtended removal*
*Researched: 2026-04-28*
