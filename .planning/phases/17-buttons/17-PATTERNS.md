# Phase 17: Buttons - Pattern Map

**Mapped:** 2026-07-23
**Files analyzed:** 5 (2 modified components, 1 new shared surface, 1 modified showcase section, 1 consumed theme extension target)
**Analogs found:** 5 / 5

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` | component (public wrapper) | request-response (click/keyboard event) | itself (current M3 version, being gutted) + `AeroIconButton.kt` (interaction wiring target) | exact (self) / role-match (interaction pattern) |
| `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt` | component (public wrapper) | request-response | itself (current M3 version) + `AeroIconButton.kt` | exact (self) / role-match |
| `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` (NEW, name Claude's discretion) | component (internal shared surface) | request-response + transform (style resolution) | `AeroPanelGroupImpl(orientation)` (one-core/param-differentiated precedent, cited in RESEARCH — not re-read this pass, already excerpted in RESEARCH.md) — structural precedent only; **pixel-level analog is `AeroSurfacePrimitives.kt`'s `aeroSurface`/`aeroGlowRing` consumption contract** | role-match (structural) / exact (primitive consumption contract) |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` (ADD extension fns: `pressedRecess()`, `flattenDisabled()`, `hoverLighten()` etc. — CONSUME/EXTEND, not new file) | utility (pure style transform) | transform | `drawAeroGroove`'s field-swap idiom (`AeroSurfacePrimitives.kt` lines 259-269) | exact (idiom to lift into an extension fn) |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` | component (showcase demo) | request-response (static render) | itself (current file, extend rows) | exact |

## Pattern Assignments

### `AeroButton.kt` / `AeroOutlinedButton.kt` (component, request-response)

**Primary analog for interaction wiring: `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt`**
(Not `AeroListItem.kt` — RESEARCH.md's correction confirmed by direct read: `AeroListItem`'s extra `.hoverable()` call only exists because its `onClick` is nullable; both button types here have required `onClick`, so `AeroIconButton`'s simpler single-`.clickable()` wiring is the precise in-package template.)

**Current imports being removed** (`AeroButton.kt` lines 1-30, `AeroOutlinedButton.kt` lines 1-30) — both files currently import M3 `Button`/`OutlinedButton`, `ButtonDefaults`, `LocalMinimumInteractiveComponentSize`, `BorderStroke`, `animateFloatAsState`/`tween`/`LinearEasing` (for the 0.97f scale-shrink), `drawWithContent`, `graphicsLayer`. **All of this is dead once the M3 container + scale-shrink are dropped** (RESEARCH.md Pitfall 3) — grep both files for `LocalMinimumInteractiveComponentSize` and `graphicsLayer` post-rewrite to confirm removal.

**Interaction wiring pattern to copy** (`AeroIconButton.kt` lines 79-92):
```kotlin
Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .size(size)
        .clip(shape)
        .alpha(if (enabled) 1f else 0.4f)
        .graphicsLayer { scaleX = scale; scaleY = scale }   // ← DO NOT copy this line into buttons (D-02 removes scale-shrink)
        .then(focusBorderModifier)                           // ← DO NOT copy this line either (D-04 replaces border-focus with aeroGlowRing)
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
)
```
**Required delta from this analog (VBTN-04):** add `role = Role.Button` — `AeroIconButton` omits it (Pitfall 5); it is button-specific and must be added explicitly:
```kotlin
.clickable(
    enabled = enabled,
    interactionSource = interactionSource,
    indication = null,
    role = Role.Button,   // ADD — not in AeroIconButton, required by VBTN-04
    onClick = onClick
)
```

**Interaction-state collection pattern** — switch from the three separate `rememberHoverState`/`rememberPressedState`/`rememberFocusState` calls (current `AeroButton.kt` lines 59-61, `AeroOutlinedButton.kt` lines 59-61, `AeroIconButton.kt` lines 61-63) to the single bundled collector added in Phase 16 (`InteractionStates.kt` lines 63-69):
```kotlin
internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return AeroInteractionState(hovered, pressed, focused)
}
```

**Current dead-code shape to delete entirely** (`AeroButton.kt` lines 63-73, mirrored in `AeroOutlinedButton.kt` lines 63-75) — the scale animation and border-focus modifier:
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed && enabled) 0.97f else 1f,
    animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
    label = "pressedScale"
)
val focusModifier = if (focused && enabled) {
    Modifier.border(2.dp, colors.borderSelected, RoundedCornerShape(4.dp))
} else { Modifier }
```
Both are replaced wholesale by D-02 (recessed press) and D-04 (persistent `aeroGlowRing` focus) — do not adapt this code, delete it.

**Locked defaults to preserve verbatim** (no behavior change per milestone constraint):
- `AeroButton`: `height: Dp = 30.dp`, `contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)`, corner `4.dp`.
- `AeroOutlinedButton`: `height: Dp = 28.dp`, `contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)`, corner `4.dp`.
- Both: `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` param signature, `Text(text, style = AeroTheme.typography.bodyLarge, fontSize = 14.sp)` content — public API/behavior stays 1:1 (explicit phase boundary).

---

### `AeroButtonSurface.kt` (NEW — internal shared surface, VBTN-06 vehicle)

**Analog for primitive consumption: `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt`**

**Ordering contract to follow exactly** (KDoc lines 100-106, function signatures lines 108-116 and 176):
```kotlin
public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier = this
    .let { m -> style.dropShadow?.let { m.dropShadow(shape = shape, shadow = it) } ?: m }
    .clip(shape)
    .drawWithCache {
        val cornerPx = style.cornerRadius.toPx()
        onDrawBehind { drawAeroSurfaceCore(style, cornerPx) }
    }
    .let { m -> style.innerShadow?.let { m.innerShadow(shape = shape, shadow = it) } ?: m }

public fun Modifier.aeroGlowRing(active: Boolean, glowColor: Color, cornerRadius: Dp = 8.dp): Modifier = ...
```
**Critical ordering rule (glow BEFORE surface, verbatim from KDoc):**
```kotlin
Modifier
    .aeroGlowRing(active = focused && enabled, glowColor = colors.borderSelected, cornerRadius = 4.dp)
    .aeroGlowRing(active = hovered && enabled, glowColor = ornaments.hoverGlow, cornerRadius = 4.dp)
    .aeroSurface(style = currentStyle, shape = RoundedCornerShape(4.dp))
```
Note: `aeroSurface`'s own `cornerRadius` default is `8.dp` and `aeroGlowRing`'s is `8.dp` — this component's locked corner is `4.dp`; pass it explicitly at all three call sites (Pitfall 1).

**Style-source pattern (`AeroSurfaceStyle.rest()`, `AeroSurfaceStyle.kt` lines 43-54):**
```kotlin
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
**D-01 finding confirmed by direct read of `AeroOrnamentTokens.derive()` (lines 29-43):** `fillSplitTop`/`fillSplitBottom`/`bevelLight`/`bevelShadow`/`hoverGlow` are already `base.primary`-derived (accent), only `glossHighlight`/`rimLight` are neutral (`base.glassHighlight`/`base.glassBorder`-derived) — this is by design (gloss/rim should read as "light on glass" regardless of hue). **`AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)` used verbatim already satisfies D-01's accent-identity requirement — no bespoke accent-override style needed for rest state.** Flag this literal-wording resolution to the three-theme reviewer per RESEARCH.md Pattern 2.

**Pressed/recessed transform to extract as an extension fn in `theme/AeroSurfaceStyle.kt` (not `components/buttons/`, so Phase 19 can import cross-package) — exact field-swap idiom, copy from `drawAeroGroove` (`AeroSurfacePrimitives.kt` lines 259-269):**
```kotlin
internal fun DrawScope.drawAeroGroove(style: AeroSurfaceStyle, cornerPx: Float) {
    drawAeroSurfaceCore(
        style = style.copy(
            fillTop = style.fillBottom,
            fillBottom = style.fillTop,
            bevelLight = style.bevelShadow,
            bevelShadow = style.bevelLight,
            glossAlpha = 0f,
        ),
        cornerPx = cornerPx,
    )
    // + bevelShadow-derived inner-shadow cue (not needed for buttons; groove-specific)
}
```
Lift only the `style.copy(...)` field-swap (not the groove-specific inner-shadow-cue draw call) into a pure extension fn, e.g.:
```kotlin
internal fun AeroSurfaceStyle.pressedRecess(innerShadow: Shadow): AeroSurfaceStyle = copy(
    fillTop = fillBottom,
    fillBottom = fillTop,
    bevelLight = bevelShadow,
    bevelShadow = bevelLight,
    glossAlpha = 0f,
    innerShadow = innerShadow,
)
```
Inner-shadow value (from UI-SPEC, per RESEARCH.md): `Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp))`. `Modifier.aeroSurface()` already applies `style.innerShadow` automatically (line 115) — no extra draw call needed in the button.

**Structural precedent for "one shared internal composable, param-differentiated" (VBTN-06):** `AeroPanelGroupImpl(orientation)` (v2.0.2, Phase 13.1) — cited in RESEARCH.md as the codebase's own precedent for exactly this "one core + additive param, zero drift" shape. Not re-excerpted here (not directly read this pass — RESEARCH.md already validated it); the shared button surface should follow the same shape: one `internal` composable, `outlined: Boolean` (or equivalent style-transform param) differentiates filled vs. outlined, never two painters.

---

### `ButtonsSection.kt` (showcase demo)

**Current structure to extend** (full file read, 98 lines) — a `SectionRow(label) { content }` table pattern; currently only shows enabled/disabled pairs:
```kotlin
SectionRow(label = "AeroButton") {
    AeroButton(text = "Save", onClick = {})
    AeroButton(text = "Disabled", onClick = {}, enabled = false)
}
SectionRow(label = "AeroOutlinedButton") {
    AeroOutlinedButton(text = "Cancel", onClick = {})
    AeroOutlinedButton(text = "Disabled", onClick = {}, enabled = false)
}
```
**Gap flagged by RESEARCH.md Open Question 1 / Wave 0 Gap:** these two rows only exercise enabled/disabled — the phase gate requires **5-state × 2-variant × 3-theme** human sign-off (rest/hover/press/focus/disabled), and hover/press/focus are transient states with no static-screenshot affordance in the current `SectionRow` pattern. Planner must decide whether `ButtonsSection.kt` needs a forced-state preview mechanism (e.g., extra demo rows wired to a debug/preview flag) or whether the existing showcase theme-switcher plus live mouse interaction during manual review is sufficient — do a quick check of the current showcase app's theme-switching/interaction mechanism before assuming new affordances are needed (RESEARCH.md explicit recommendation).

## Shared Patterns

### Single shared internal surface, param-differentiated (VBTN-06)
**Source:** `AeroPanelGroupImpl(orientation)` precedent (structural) + `AeroSurfacePrimitives.kt`'s `aeroSurface`/`aeroGlowRing` (pixel-level, exact contract above)
**Apply to:** the new `AeroButtonSurface` (or equivalent name) backing both `AeroButton` and `AeroOutlinedButton`; outlined is a fixed-delta transform of the filled style (RESEARCH.md Pattern 5: fill α×0.15, gloss ratio 0.32→0.15, rim 0.6→0.85), never an independently-authored style.

### `clickable(interactionSource=...)` alone is sufficient (no `.hoverable()`)
**Source:** `AeroIconButton.kt` lines 87-92 (excerpted above)
**Apply to:** both `AeroButton.kt` and `AeroOutlinedButton.kt` — do NOT copy `AeroListItem`'s extra `.hoverable(interactionSource)` call (that split exists only for nullable `onClick`, not applicable here).

### `rememberAeroInteractionState` single collector
**Source:** `InteractionStates.kt` lines 63-69 (excerpted above)
**Apply to:** both button files, replacing the three separate `rememberHoverState`/`rememberPressedState`/`rememberFocusState` calls each currently makes.

### Glow-before-surface modifier ordering
**Source:** `AeroSurfacePrimitives.kt` lines 100-106 KDoc + verified usage lines 360-364 of RESEARCH.md
**Apply to:** every modifier chain in the new shared surface — both `aeroGlowRing` calls (focus, hover) must precede `aeroSurface` or the outer bloom is invisible (this exact bug was found/fixed at Phase 16 sign-off).

### Corner radius must be explicit `4.dp` everywhere
**Source:** `AeroSurfaceStyle.rest()` default `8.dp` (line 43) / `aeroGlowRing` default `8.dp` (line 176) vs. this component's locked `4.dp`
**Apply to:** all `aeroGlowRing(...)` calls, `aeroSurface(style, shape)`'s `RoundedCornerShape(4.dp)`, and `AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)` — never fall through to either function's own default.

### `Color.Transparent` anti-pattern (PRIM-14)
**Source:** `AeroSurfacePrimitives.kt` lines 30-34, 58-66 (gloss fade), 271-277 (groove shadow-cue fade)
**Apply to:** any new gradient fade written in the button's own style-resolution code (if any) must fade to `baseColor.copy(alpha = 0f)`, never `Color.Transparent` — irrelevant if the button only ever consumes `drawAeroSurfaceCore` (which already does this correctly) and never draws its own gradient.

## No Analog Found

None — every file this phase touches has a direct, already-shipped analog (Phase 16 primitives are the drawing layer; `AeroIconButton.kt` is the interaction-wiring analog; `ButtonsSection.kt` and the two button files are self-analogs being rewritten in place).

## Metadata

**Analog search scope:** `library/src/main/kotlin/com/mordred/aero/components/buttons/`, `library/src/main/kotlin/com/mordred/aero/theme/`, `library/src/main/kotlin/com/mordred/aero/components/common/`, `showcase/src/main/kotlin/com/mordred/showcase/sections/`
**Files read directly this pass:** `AeroButton.kt`, `AeroOutlinedButton.kt`, `AeroIconButton.kt`, `AeroSurfaceStyle.kt`, `AeroOrnamentTokens.kt`, `InteractionStates.kt`, `AeroSurfacePrimitives.kt` (lines 1-135, 170-300), `ButtonsSection.kt` (full)
**Pattern extraction date:** 2026-07-23
