# Phase 18: Range - Pattern Map

**Mapped:** 2026-07-23
**Files analyzed:** 7 (3 restyled components, 1 new theme factory, 3 new Wave-0 test files)
**Analogs found:** 7 / 7

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|---------------|
| `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` (+ `neutralRest()`) | utility (style factory) | transform | `theme/AeroSurfaceStyle.kt`'s own `rest()`/`pressedRecess()`/`flattenDisabled()`/`hoverLighten()` (same file, additive companion factory + extension fns) | exact |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` | component (M3-hosted, custom slots) | request-response (drag/keyboard → value emit) | `components/buttons/AeroButtonSurface.kt` (style resolver + modifier-ordering + interaction-state wiring) | role-match (button = Box-owning; AeroSlider = M3-slot-owning, same resolver/glow pattern) |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` | component (Canvas, direct-call draw) | event-driven (pointer drag loop → value emit) + transform (draw-only edit) | `components/buttons/AeroButtonSurface.kt` (resolver shape) + `theme/AeroSurfacePrimitives.kt`'s `drawAeroGroove`/`drawAeroSurfaceCore`/`drawAeroThumb` direct-call precedent + `AeroPanelGroup.kt:443-451` (Pattern 3) | role-match |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt` | component (Box-owning, animated) | transform (Box.background → aeroGroove/aeroSurface swap) + event-driven (infiniteRepeatable sweep) | `theme/AeroSurfacePrimitives.kt`'s `Modifier.aeroGroove`/`Modifier.aeroSurface` exposures (same Box-owning Modifier-path as `AeroButtonSurface.kt` uses, not the Canvas direct-call path) | role-match |
| `library/src/test/kotlin/.../range/AeroSliderStylesTest.kt` (Wave 0, new) | test (value-level, no Compose runtime) | request-response (pure fn assertions) | `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` | exact |
| `library/src/test/kotlin/.../range/AeroSliderSourceTest.kt` (Wave 0, new) | test (source-scan guard) | transform (string-contains assertions) | `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` | exact |
| `library/src/test/kotlin/.../range/AeroProgressBarSourceTest.kt` (Wave 0, new) | test (source-scan guard) | transform (string-contains assertions) | `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` | exact |
| `library/src/test/kotlin/.../range/AeroRangeSliderDragLogicUntouchedTest.kt` (Wave 0, new) | test (source-scan / logic-snapshot guard) | transform | `AeroButtonSurfaceSourceTest.kt`'s file-content-assertion pattern (same `sourceFile()` cwd-independent resolver idiom) | role-match |

## Pattern Assignments

### `theme/AeroSurfaceStyle.kt` — add `neutralRest()` (utility, transform)

**Analog:** the file's own `rest()` companion factory (lines 49-60) and the three extension-fn
transforms below it (`pressedRecess` lines 92-99, `flattenDisabled` lines 122-135, `hoverLighten`
lines 143-146).

**Placement pattern** — new factory lives in the SAME file, next to `rest()`, not in
`components/range/`, so Phase 19's `AeroSwitch` can reuse it cross-package without a reach-around
(RESEARCH.md Pattern 1 explicit instruction):
```kotlin
// theme/AeroSurfaceStyle.kt — companion factory pattern to copy (existing rest(), lines 49-60)
public fun rest(base: AeroColorScheme, cornerRadius: Dp = 8.dp): AeroSurfaceStyle {
    val ornaments = base.ornamentOverride ?: AeroOrnamentTokens.derive(base)
    return AeroSurfaceStyle(
        fillTop = ornaments.fillSplitTop,
        fillBottom = ornaments.fillSplitBottom,
        glossColor = ornaments.glossHighlight,
        bevelLight = ornaments.bevelLight,
        bevelShadow = ornaments.bevelShadow,
        rimColor = ornaments.rimLight,
        cornerRadius = cornerRadius,
    )
}
```
`neutralRest()` must be the SAME shape, sourcing `fillTop`/`fillBottom`/`bevelLight`/`bevelShadow`
from `base.glassHighlight`/`base.glassSurface`/`base.glassBorder` (via the already-shipped
`Color.lighten()`/`Color.darken()` helpers, same ones `hoverLighten`/`flattenDisabled` already
use) instead of `ornaments.fillSplitTop`/`bevelLight` (which RESEARCH.md confirmed are
`primary`-derived, not neutral). `glossColor`/`rimColor` reuse `ornaments.glossHighlight`/
`ornaments.rimLight` as-is (already neutral tokens).

**Transform-fn signature pattern to mirror** (internal extension fn, `copy()`-based, RGB-mix never
`.copy(alpha=)`):
```kotlin
// theme/AeroSurfaceStyle.kt:143-146 — hoverLighten(), the exact idiom neutralRest's callers chain
internal fun AeroSurfaceStyle.hoverLighten(): AeroSurfaceStyle = copy(
    fillTop = fillTop.lighten(HOVER_LIGHTEN_AMOUNT),
    fillBottom = fillBottom.lighten(HOVER_LIGHTEN_AMOUNT),
)
```

---

### `components/range/AeroSlider.kt` (component, request-response, M3-slot-hosted)

**Analog:** `components/buttons/AeroButtonSurface.kt` (resolver + modifier-ordering + interaction
wiring) — NOT a byte-for-byte structural copy (AeroSlider stays inside M3 `Slider`'s `thumb=`/
`track=` slots, AeroButtonSurface is a bare `Box`), but every state-resolution and glow-ordering
rule transfers directly.

**Interaction-state collection pattern** (`AeroButtonSurface.kt:73`):
```kotlin
val state = rememberAeroInteractionState(interactionSource)
```
Reuse verbatim — one `MutableInteractionSource` (`AeroSlider.kt` already has this at line 48,
currently only used for `collectIsDraggedAsState()`) feeds one `rememberAeroInteractionState()`
call, same as the button.

**Resolver-shape pattern to mirror** (`AeroButtonSurface.kt:244-279`, `resolveButtonStyle`) —
copy the precedence chain shape (disabled wins > pressed/dragging > hovered > rest), NOT the
button-specific constants:
```kotlin
internal fun resolveButtonStyle(
    colors: AeroColorScheme, outlined: Boolean,
    hovered: Boolean, pressed: Boolean, focused: Boolean, enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).copy(/* button-specific override */)
    if (!enabled) return rest.flattenDisabled(colors)   // terminal transform, no hover/press applied
    return when {
        pressed -> rest.pressedRecess(PRESSED_INNER_SHADOW)
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
```
For the slider thumb, D-04 diverges at exactly the `pressed` branch: replace
`rest.pressedRecess(...)` with `rest.copy(glossAlpha = rest.glossAlpha + 0.06f)` (stays raised,
brighter gloss, not inverted) — this divergence point is the single most load-bearing thing to
get right; everything else in the chain shape (disabled-wins, hover-vs-rest) copies unchanged.

**Modifier-ordering pattern — LOAD-BEARING, copy verbatim** (`AeroButtonSurface.kt:95-105`):
```kotlin
.aeroGlowRing(
    active = state.focused && enabled,
    glowColor = AeroTheme.colors.borderSelected,
    cornerRadius = 4.dp,
)
.aeroGlowRing(
    active = state.hovered && enabled,
    glowColor = AeroOrnamentTokens.derive(AeroTheme.colors).hoverGlow,
    cornerRadius = 4.dp,
)
.aeroSurface(style, RoundedCornerShape(4.dp))
```
For the thumb slot, swap `.aeroSurface(...)` → `.aeroThumbSurface(thumbStyle)` and cornerRadius →
`thumbRadius`, but the glow-before-surface ORDER and the focused-then-hovered stacking order must
be copied exactly (D-03's explicit load-bearing check). Also add
`.hoverable(interactionSource)` on the thumb slot's own `Box` — this is the ONE piece with no
direct Phase 17 precedent (buttons get hover for free via `Modifier.clickable`; M3's custom
`thumb=` slot does not — RESEARCH.md Pitfall 1).

**Current-file baseline** (`AeroSlider.kt`, full file, 87 lines) — today: 6-arg `Slider(...)` call
+ `SliderDefaults.colors(...)` (lines 51-58) + `alpha = 0.4f` disabled (lines 55-57) to be REMOVED
per D-07; drag-tooltip glass pill (lines 60-74, `glassEffect`) kept as-is (CONTEXT.md discretion
item, low priority).

---

### `components/range/AeroRangeSlider.kt` (component, event-driven drag + transform draw-only edit)

**Analog 1 — resolver shape:** same `resolveButtonStyle`-shape mirror as `AeroSlider.kt` above,
but instantiated TWICE (once per thumb, per VRNG-05), each fed its own
`MutableInteractionSource`/`rememberAeroInteractionState()` pair.

**Analog 2 — direct-call Canvas draw, not the Modifier-path:** `theme/AeroSurfacePrimitives.kt`'s
internal direct-call functions (`drawAeroGroove` lines 259-284, `drawAeroSurfaceCore` lines 39-98,
`drawAeroThumb` lines 222-224) — called INSIDE the existing `Canvas(...) { ... }` draw lambda,
never through `Modifier.aeroGroove`/`Modifier.aeroSurface`/`Modifier.aeroThumbSurface` (those are
Box-owning exposures that apply their own `.clip()`; inside an existing `Canvas` `DrawScope` you
are already past that). Both are the SAME underlying implementation — Phase 16 explicitly ships
both call surfaces from one file for exactly this dual consumer split.

**Draw calls being replaced** (`AeroRangeSlider.kt`, current lines 239-266, full file already
read — 297 lines):
```kotlin
// 1. Inactive track (full width) — CURRENT (line 239-244):
drawLine(color = colors.borderDefault, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = trackThickness)
// → replace with: drawAeroGroove(neutralRest(colors, cornerRadius=...), cornerPx = trackThickness/2f)

// 2. Active track between thumbs — CURRENT (line 247-252):
drawLine(color = if (enabled) colors.primary else colors.borderDefault, start = Offset(startX, centerY), end = Offset(endX, centerY), strokeWidth = trackThickness)
// → replace with: drawAeroSurfaceCore(accentTrackStyle, cornerPx) inside a translate/clipped sub-region (see Pitfall 3 below)

// 3. Thumbs — CURRENT (line 255-267), fun drawThumb(x: Float):
drawCircle(color = ..., radius = thumbRadius, center = Offset(x, centerY))
drawCircle(color = colors.onPrimary, radius = thumbRadius, center = Offset(x, centerY), style = Stroke(width = 2.dp.toPx()))
// → replace with: translate(left = x - thumbRadius, top = centerY - thumbRadius) { drawAeroThumb(thumbStyle, radiusPx = thumbRadius) }
```

**Pitfall 3 (sub-region draw) — no byte-identical precedent exists.** `drawAeroSurfaceCore` draws
across the FULL receiver `size`, unlike `drawLine`'s inherent `[start,end]` sub-region awareness.
Verify at implementation via `translate(left = startX) { ... }` + a clipped/inset sub-scope sized
to `(endX - startX)`; RESEARCH.md flags this as the one part with no existing codebase idiom to
copy — treat as first-implementation spot-check, not a blocked pattern gap.

**Untouched-by-construction boundary (VRNG-04):** the `.pointerInput(enabled, valueRange, steps) {
awaitPointerEventScope { ... } }` block, lines 180-227, and every logic fn above it
(`snapToStep`, `applyThumbMove`, `xToValue`, `valueToX`, `thumbToDrawFirst`, lines 40-131) are the
"analog" for what must NOT change — these are the load-bearing PITFALL-03 drag-precision
functions; the new per-thumb hover/press interaction sources are ADDED alongside this block
(new state variables), never a refactor of its structure.

**Analog 3 — Pattern 3 (VRNG-09), copy verbatim shape:**
```kotlin
// Source: AeroPanelGroup.kt:443-451 (direct project read)
val animated by animateFloatAsState(
    targetValue = targetPx,
    animationSpec = if (isDragging) snap() else tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "panelHeight_${sections[i].key}",
)
```
Apply this shape (renamed target/label) to any animated thumb glow/gloss transition value that
must not lag during that thumb's own active drag — `isDragging` here is per-thumb
(`activeThumb == RangeThumb.Start/End`, already tracked at line 173), matching VRNG-05's
independence requirement.

---

### `components/range/AeroProgressBar.kt` (component, transform + event-driven sweep)

**Analog:** `theme/AeroSurfacePrimitives.kt`'s Box-owning Modifier exposures (`Modifier.aeroGroove`
line 292-300, `Modifier.aeroSurface` line 108-115) — this file is NOT a Canvas (unlike
`AeroRangeSlider`), so it uses the SAME Modifier-path `AeroButtonSurface.kt` uses, not the direct-
call path.

**Determinate swap** (current `AeroProgressBar.kt:47-60`):
```kotlin
// CURRENT:
Box(Modifier.fillMaxWidth().height(height).background(colors.surface, RoundedCornerShape(50))) {
    Box(Modifier.fillMaxWidth(clamped).height(height).background(colors.primary, RoundedCornerShape(50)))
}
// → TARGET (RESEARCH.md Pattern 4):
val cornerPx = height / 2  // pill — MUST feed both clip Shape and drawAeroSurfaceCore's cornerPx (Pitfall 5)
Box(Modifier.fillMaxWidth().height(height).aeroGroove(neutralRest(colors, cornerPx))) {
    Box(Modifier.fillMaxWidth(clamped).height(height).aeroSurface(AeroSurfaceStyle.rest(colors, cornerPx), RoundedCornerShape(cornerPx)))
    if (showRunningSheen) { /* new optional overlay, default false — D-05/VRNG-07 */ }
}
```
Disabled state for the fill uses the SAME `rest(colors,...).flattenDisabled(colors)` terminal
transform as the button's disabled branch (`AeroButtonSurface.kt:267`), not a `.copy(alpha=)`
fade.

**Indeterminate swap** (current `AeroProgressBar.kt:98-113`) — same `Modifier.aeroGroove`/
`Modifier.aeroSurface` pair for the bed/segment, but the segment ADDITIONALLY needs a horizontal
edge-fade (D-06) with no existing primitive for it — RESEARCH.md Assumption A4 flags this as
needing a second `Brush.horizontalGradient` alpha-mask overlay, fading to
`baseColor.copy(alpha = 0f)` (PRIM-14, never `Color.Transparent` — this is the exact anti-pattern
`drawAeroSurfaceCore`'s own gloss/fill gradients already avoid, see
`AeroSurfacePrimitives.kt:30-34`'s KDoc). Timing (`infiniteRepeatable(tween(1500, LinearEasing),
RepeatMode.Restart)`, current lines 91-94) is UNCHANGED — `RepeatMode.Reverse` is the one banned
value (VRNG-08).

---

### Wave 0 test files (value-level + source-scan guards)

**`AeroSliderStylesTest.kt`** — analog `AeroButtonStylesTest.kt` (full file, 204 lines). Copy the
exact shape: `schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)`, a private
`expectedXxxRest(colors)` helper reconstructing the resolver's rest style independently (so the
test doesn't just re-call the function under test), then one `@Test` per state
(rest/disabled-wins/pressed/hovered/focused) asserting on the PURE resolver fn — no
`runComposeUiTest`, no Compose runtime:
```kotlin
// AeroButtonStylesTest.kt:37-57 — the exact test shape to mirror per state
@Test
fun restStateEqualsThemeAwareButtonRest() {
    schemes.forEach { colors ->
        val resolved = resolveButtonStyle(colors = colors, outlined = false, hovered = false, pressed = false, focused = false, enabled = true)
        val expected = expectedButtonRest(colors)
        assertEquals(expected, resolved, "...")
    }
}
```
For the slider, replace `pressedReturnsPressedRecessTransformedStyle` (button analog,
`AeroButtonStylesTest.kt:94-109`) with a slider-specific assertion that press/drag does NOT swap
fillTop/fillBottom (D-04 divergence) — assert `fillTop == rest.fillTop` (unchanged, not swapped)
and `glossAlpha > rest.glossAlpha` (brighter) instead of asserting `pressedRecess`'s swap +
zeroed-gloss + non-null `innerShadow`.

**`AeroSliderSourceTest.kt` / `AeroProgressBarSourceTest.kt`** — analog
`AeroButtonSurfaceSourceTest.kt` (full file, 99 lines). Copy the `sourceFile()` cwd-independent
resolver helper verbatim (lines 89-97) and the `assertTrue`/`assertFalse`-on-`.readText()`
pattern. Concrete assertions to adapt:
- `AeroSliderSourceTest.kt`: assert `AeroSlider.kt`'s source does NOT contain `SliderColors(` with
  disabled-alpha fields (D-07 — "do not rely on M3 SliderColors for disabled"), analog to the
  button test's `assertFalse(...contains("aeroSurface(")...)` "no duplicated painter" shape
  (`AeroButtonSurfaceSourceTest.kt:49-62`).
- `AeroProgressBarSourceTest.kt`: assert source contains `RepeatMode.Restart`, does NOT contain
  `RepeatMode.Reverse`, contains `"1500"`, and contains `showRunningSheen: Boolean = false` (or
  equivalent default-false param declaration) — same string-contains assertion style as
  `AeroButtonSurfaceSourceTest.kt:30-46`.

**`AeroRangeSliderDragLogicUntouchedTest.kt`** — analog same `AeroButtonSurfaceSourceTest.kt`
file-content-assertion pattern, but framed as a diff/snapshot guard: capture the
`.pointerInput { awaitPointerEventScope { ... } }` block's source text (or rely on the EXISTING
`AeroRangeSliderTest.kt`'s coverage of `snapToStep`/`applyThumbMove`/`xToValue`/`valueToX`, already
present per RESEARCH.md's Validation Architecture table, ✅ exists) and assert it is unchanged
line-for-line, or assert those pre-existing tests still pass post-restyle as the proof VRNG-04
wasn't violated.

**Repro-must-exercise-the-path discipline (Pitfall 8, institutional memory — mirrors
`feedback_repro_must_exercise_path.md`):** every new source-scan test above must be temporarily
broken (revert the fix locally), confirmed to FAIL, then restored and confirmed to PASS — document
the fail-then-pass proof in the plan's SUMMARY exactly as `17-03-SUMMARY.md`'s "Guard Fail-Then-
Pass Proof" did for VBTN-03/06.

## Shared Patterns

### Style-resolver precedence chain (disabled > pressed/dragging > hovered > rest)
**Source:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:244-279` (`resolveButtonStyle`)
**Apply to:** `resolveSliderThumbStyle`-equivalent fn in `AeroSlider.kt`/`AeroRangeSlider.kt` — same
`when { pressed/isDragging -> ...; hovered -> hoverLighten(); else -> rest }` shape, disabled always
short-circuits first via a terminal `flattenDisabled(colors)` call (never composed with hover/press).

### Modifier-ordering — glow BEFORE surface/thumb clip
**Source:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:95-105`
**Apply to:** the thumb slot's own Box modifier chain in both sliders —
`.aeroGlowRing(focused).aeroGlowRing(hovered).aeroThumbSurface(style)`, never reversed (D-03's
explicit load-bearing check, PRIM-06 usage contract in `AeroSurfacePrimitives.kt:162-166`).

### Disabled — flatten to dead, never alpha-fade
**Source:** `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt:122-135` (`flattenDisabled`)
**Apply to:** all three range components' disabled branch — replaces `AeroSlider`'s
`SliderColors(disabledThumbColor = ...copy(alpha=0.4f)...)` and `AeroRangeSlider`'s
`.copy(alpha = 0.4f)` inline fades entirely (D-07).

### Interaction-state collection
**Source:** `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt:63-69` (`rememberAeroInteractionState`)
**Apply to:** one call per interactive region — `AeroSlider` (1x, existing `interactionSource` at
line 48 already present, currently unused for hover/press/focus), `AeroRangeSlider` (2x, one per
thumb, both new).

### Pattern 3 — animation-vs-drag value conflict
**Source:** `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt:443-451`
**Apply to:** any animated thumb glow/gloss transition on `AeroSlider`/`AeroRangeSlider` that must
not lag during that thumb's own active drag (VRNG-09) — `animateFloatAsState(targetValue = ...,
animationSpec = if (isDragging) snap() else tween(...))`.

### `Color.Transparent` anti-pattern — always fade to `baseColor.copy(alpha = 0f)`
**Source:** `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt:30-34` (KDoc) and `:271-277` (`drawAeroGroove`'s inner-shadow cue gradient, concrete instance)
**Apply to:** every new gradient this phase adds — most notably the indeterminate segment's
horizontal edge-fade (D-06) on `AeroProgressBar`, which has no existing primitive and must be
hand-built as a new `Brush.horizontalGradient` following this exact fade-target rule.

### Test structure — value-level resolver tests + source-scan guards
**Source:** `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` (full file) and `AeroButtonSurfaceSourceTest.kt` (full file)
**Apply to:** all four Wave-0 test files — `schemes = listOf(AeroColorScheme.AeroBlue,
AeroColorScheme.Classic)` sweep for value-level tests, cwd-independent `sourceFile()` helper +
`.readText()` string-contains assertions for source-scan guards.

## No Analog Found

None — every file in scope has a direct or role-match analog from Phase 16/17. The one piece with
no BYTE-IDENTICAL precedent in the codebase (not "no analog," but "no prior example of this exact
technique") is the sub-region `drawAeroSurfaceCore` call for `AeroRangeSlider`'s active-fill
segment (Pitfall 3, flagged above) — RESEARCH.md's own Open Question #2 already flags this as a
first-implementation spot-check, not a blocked gap.

## Metadata

**Analog search scope:** `library/src/main/kotlin/com/mordred/aero/components/buttons/`,
`library/src/main/kotlin/com/mordred/aero/theme/`, `library/src/main/kotlin/com/mordred/aero/components/range/`,
`library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt`,
`library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt`,
`library/src/test/kotlin/com/mordred/aero/components/buttons/`
**Files scanned:** 10 (full reads) — `AeroButtonSurface.kt`, `AeroSurfaceStyle.kt`,
`AeroSurfacePrimitives.kt`, `InteractionStates.kt`, `AeroButtonStylesTest.kt`,
`AeroButtonSurfaceSourceTest.kt`, `AeroSlider.kt`, `AeroRangeSlider.kt`, `AeroProgressBar.kt`,
`AeroPanelGroup.kt` (targeted 30-line read, lines 430-459)
**Pattern extraction date:** 2026-07-23
