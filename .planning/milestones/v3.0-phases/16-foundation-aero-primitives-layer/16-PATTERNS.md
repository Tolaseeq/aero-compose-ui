# Phase 16: Foundation — Aero Primitives Layer - Pattern Map

**Mapped:** 2026-07-22
**Files analyzed:** 9 (5 new theme files, 1 modified theme file, 2 modified files (move+extend / append field), 2 new showcase files)
**Analogs found:** 9 / 9 (all have a strong same-codebase analog — this phase's whole job is generalizing existing in-repo patterns, not inventing new ones)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `library/.../theme/ColorMath.kt` (NEW) | utility | transform | `library/.../theme/GlassModifiers.kt` (Brush/Color construction style) | role-match (no existing pure-Color-math file; closest sibling by package + Color-manipulation intent) |
| `library/.../theme/AeroOrnamentTokens.kt` (NEW) | model/config | transform | `library/.../theme/AeroColorScheme.kt` (data class + companion-object factory) | exact |
| `library/.../theme/AeroColorScheme.kt` (MODIFIED — append `ornamentOverride`) | model/config | CRUD (source-compatible field add) | itself (in-place) | exact |
| `library/.../theme/AeroSurfaceStyle.kt` (NEW) | model/config | transform | `library/.../theme/AeroColorScheme.kt` (immutable data class + preset pattern) | role-match |
| `library/.../theme/AeroSurfacePrimitives.kt` (NEW) | utility (DrawScope extension + Modifier factory) | transform/streaming (per-frame draw) | `library/.../theme/GlassModifiers.kt` (`glassSurface`/`glassPanel`/`glassEffect` — Modifier-extension + `drawBehind`/`Canvas` DrawScope pattern) | exact |
| `library/.../theme/GlassModifiers.kt` (MODIFIED — 3 bug fixes in place) | utility | transform | itself (in-place; `glassPanel`'s already-correct proportional-gloss branch is the internal analog for fixing `glassSurface`) | exact |
| `library/.../components/common/InteractionStates.kt` (MOVED from `components/buttons/` + extended) | hook/state | event-driven | itself (relocated) + `library/.../components/list/AeroListItem.kt` (real hover-wiring call site) | exact |
| `showcase/.../sections/PrimitivesSection.kt` (NEW) | component (showcase gallery) | request-response (static render) | `showcase/.../sections/FoundationSection.kt` | exact |
| `showcase/.../scratch/ScratchSliderSlotSpike.kt` (NEW) | component (scratch/spike) | request-response (static render) | `showcase/.../scratch/ScratchAeroShadowProof.kt` | exact |

## Pattern Assignments

### `library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt` (utility, transform)

**Analog:** `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` (package placement + Color-typed helper convention; no direct RGB-math sibling exists, so this is a role-match, not exact)

**Package/imports convention** (from `GlassModifiers.kt` lines 1-16):
```kotlin
package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color
// no Compose-runtime import needed for a pure Color -> Color function
```

**Signature shape to follow** (RESEARCH.md Code Examples, consistent with this file's `internal` visibility convention seen on `InteractionStates.kt`'s helpers):
```kotlin
internal fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)
internal fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha,
)
```
Note: must be RGB-mix, NOT `.copy(alpha = ...)` — `AeroColorScheme.Classic` (see below) uses fully opaque literals, so alpha tricks are silently invisible on that theme.

---

### `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` (model/config, transform)

**Analog:** `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` (exact — same file will literally sit beside it)

**Data class + companion-object preset pattern** (`AeroColorScheme.kt` lines 12-37):
```kotlin
@Immutable
public data class AeroColorScheme(
    public val primary: Color,
    // ...23 fields total, verbatim order, no defaults on the existing 23...
    public val panelBackground: Color
) {
    public companion object {
        public val AeroBlue: AeroColorScheme = AeroColorScheme(/* ... */)
        public val AeroDark: AeroColorScheme = AeroColorScheme(/* ... */)
        public val Classic: AeroColorScheme = AeroColorScheme(/* ... */)
    }
}
```
`AeroOrnamentTokens` should mirror this shape: `@Immutable data class` with a `companion object { fun derive(base: AeroColorScheme): AeroOrnamentTokens }` factory instead of hardcoded per-theme presets (derive is algorithmic, per RESEARCH.md Pattern 2).

**Critical fact driving `derive()`'s implementation** — three presets' actual token opacity, confirmed by direct read:
- `AeroBlue.glassSurface = Color(0x30FFFFFF)` (translucent, line 54)
- `AeroDark.glassSurface = Color(0x20FFFFFF)` (translucent, line 80)
- `Classic.glassSurface = Color(0xFF333333)` (**fully opaque**, line 106)

This is why `derive(base)` must call `ColorMath.lighten/darken` (RGB-mix) on `base.*` fields — an alpha-based derivation would produce zero visible change on `Classic`.

---

### `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` (MODIFIED — append `ornamentOverride`)

**Analog:** itself, in place. Append-only, trailing, defaulted field per PRIM-03/D-01 costly-reversibility note:
```kotlin
public data class AeroColorScheme(
    public val primary: Color,
    // ...all 23 existing fields, UNCHANGED, same order...
    public val panelBackground: Color,
    public val ornamentOverride: AeroOrnamentTokens? = null,  // NEW — trailing, defaulted
)
```
All three companion presets (`AeroBlue`/`AeroDark`/`Classic`, lines 39-116) use fully-named constructor calls with no trailing comma issue — confirmed safe to append after `panelBackground = ...` in each.

---

### `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` (model/config, transform)

**Analog:** `AeroColorScheme.kt` (immutable data-class-with-presets convention) — role-match, not exact, since this is a new architectural layer (declarative style, not raw color tokens).

**Pattern to follow:** `@Immutable` (or `@Stable`) data class with named fields for fill stops / gloss / bevel / rim / optional shadows, consumed by `drawAeroSurfaceCore`. See RESEARCH.md Architecture Pattern 1's skeleton — field names/shape are Claude's discretion (CONTEXT.md).

---

### `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` (utility, transform/per-frame draw)

**Analog:** `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` (exact — this is the direct generalization target)

**Imports pattern** (`GlassModifiers.kt` lines 1-16):
```kotlin
package com.mordred.aero.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind   // NEW code should use drawWithCache instead (PRIM-13)
import androidx.compose.ui.draw.shadow        // NEW code uses dropShadow/innerShadow (see ScratchAeroShadowProof)
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
```

**Core Modifier-extension pattern to generalize** (`glassSurface`, lines 79-106 — this is the negative example whose bugs PRIM-09/10/12 fix, but also the closest structural analog for `Modifier.aeroSurface()`):
```kotlin
@Composable
public fun Modifier.glassSurface(
    cornerRadius: Dp = 8.dp
): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val glassHighlight = colors.glassHighlight
    val glassBorder = colors.glassBorder
    return this
        .drawBehind {
            val cornerPx = cornerRadius.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glassHighlight, Color.Transparent),
                    startY = 0f,
                    endY = 100f          // BUG (PRIM-09): literal px, not size.height * fraction
                ),
                cornerRadius = cr
            )
            drawRoundRect(
                color = glassBorder,
                cornerRadius = cr,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)   // BUG (PRIM-10/12): clip AFTER drawBehind — stroke's outer half is clipped away
}
```

**Already-correct proportional-gloss sibling to copy the fraction pattern from** (`glassPanel`, lines 60-73, `endY = size.height * 0.55f` at line 68):
```kotlin
return this.drawBehind {
    val cornerPx = cornerRadius.toPx()
    val cr = CornerRadius(cornerPx, cornerPx)
    drawRoundRect(color = panelBackground, cornerRadius = cr)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(glassHighlight, Color.Transparent),
            startY = 0f,
            endY = size.height * 0.55f   // CORRECT pattern — proportional, not literal
        ),
        cornerRadius = cr
    )
}
```
Note per D-01: 0.55f is heavier than the ~30-35% target for the new primitive — do not copy the fraction, only the `size.height * fraction` idiom.

**Native shadow ordering to copy verbatim** (`showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` lines 67-91 — compile-proven, corrected imports):
```kotlin
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset

Modifier
    .size(120.dp, 40.dp)
    .dropShadow(
        shape = shape,
        shadow = Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 2.dp)),
    )
    .background(color = Color(0xFF3A6EA5), shape = shape)
    .innerShadow(
        shape = shape,
        shadow = Shadow(radius = 2.dp, color = Color.White.copy(alpha = 0.45f), offset = DpOffset(0.dp, 1.dp)),
    )
```
Rule: `dropShadow` precedes `.background()`; `innerShadow` follows it. Do NOT import from `androidx.compose.ui.graphics.shadow` for the two modifier functions — only `Shadow` itself lives there; `dropShadow`/`innerShadow` are in `androidx.compose.ui.draw`. `DpOffset` is `androidx.compose.ui.unit.DpOffset`, not `ui.geometry`.

**Canvas-owning direct-call exposure path analog** (`library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` lines 1-27 — imports `Canvas`, uses raw `DrawScope`/`Stroke` inside a `Canvas {}` block; the file this phase's `drawAeroSurfaceCore` direct-Canvas-call convention must match for `AeroRangeSlider`-style consumers):
```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
// ... AeroRangeSlider calls Canvas { drawRoundRect(...) / drawCircle(...) } directly —
// drawAeroSurfaceCore(style, cornerPx) must be callable identically inside such a block,
// not only via a Modifier extension.
```

---

### `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` (MODIFIED — 3 bug fixes in place)

**Analog:** itself. Exact line-numbered defects (confirmed by direct read this session):
- **PRIM-09** — `endY = 100f` literal at line 95 (inside `glassSurface`) → replace with `size.height * fraction` per the `glassPanel` sibling's correct `size.height * 0.55f` pattern (line 68), using a fraction within D-01's ~30-35% band, not 0.55f verbatim.
- **PRIM-10/12** — `.clip(shape)` at line 105 comes AFTER `.drawBehind {...}` (lines 88-104): stroke drawn before clip loses its outer half. Fix: reorder so `.clip(shape)` is outermost affecting paint, or inset the stroke by half width as belt-and-suspenders.
- **PRIM-11** — `glassEffect(cornerRadius, elevation)` at lines 26-45: `elevation: Dp = 4.dp` parameter is read into local scope implicitly via the function signature but never passed to any shadow-drawing call in the body (no `.shadow()`/`dropShadow()` call exists in the function). Revive (wire to `dropShadow`, see fix skeleton in RESEARCH.md Code Examples) or remove — decided on the three-theme review per D-03.

---

### `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` (MOVED + extended, hook/state, event-driven)

**Analog:** itself (relocated verbatim) — current content at `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt`, full file (34 lines):
```kotlin
package com.mordred.aero.components.buttons   // -> becomes com.mordred.aero.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

internal const val ANIMATION_DURATION_MS: Int = 150

@Composable
internal fun rememberHoverState(source: InteractionSource): State<Boolean> =
    source.collectIsHoveredAsState()

@Composable
internal fun rememberPressedState(source: InteractionSource): State<Boolean> =
    source.collectIsPressedAsState()

@Composable
internal fun rememberFocusState(source: InteractionSource): State<Boolean> =
    source.collectIsFocusedAsState()

@Composable
internal fun animatedAlpha(target: Float): State<Float> =
    animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
        label = "alpha"
    )
```
All functions already `internal` (module-scoped, not package-scoped) — relocating package requires zero visibility change (confirmed: `internal` in Kotlin is module-wide, not file/package-wide).

**Real hover-wiring call site to copy verbatim for `rememberAeroInteractionState()`'s own usage pattern** (`library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` lines 6-10, 54, 57, 74):
```kotlin
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState

interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
// ...
val hovered by interactionSource.collectIsHoveredAsState()
// ...
Row(
    modifier = modifier
        .background(animatedBg)
        .hoverable(interactionSource)   // <-- pairing to copy verbatim: hoverable() + collectIsHoveredAsState()
        ...
)
```

**New composite function shape** (RESEARCH.md Pattern 4, consistent with the existing file's `@Composable internal fun` convention):
```kotlin
internal data class AeroInteractionState(val hovered: Boolean, val pressed: Boolean, val focused: Boolean)

@Composable
internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return AeroInteractionState(hovered, pressed, focused)
}
```

---

### `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt` (component/showcase gallery, request-response)

**Analog:** `showcase/src/main/kotlin/com/mordred/showcase/sections/FoundationSection.kt` (exact — explicit precedent per CONTEXT.md D-04 and RESEARCH.md)

**Full pattern to extend** (FoundationSection.kt, 84 lines):
```kotlin
package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassEffect
import com.mordred.aero.theme.glassPanel
import com.mordred.aero.theme.glassSurface

@Composable
fun FoundationSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(cornerRadius = 8.dp)
            .padding(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            DemoBox(label = "glassEffect") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .glassEffect(cornerRadius = 8.dp, elevation = 4.dp)
                )
            }
            // ...glassPanel, glassSurface boxes, same shape...
        }
    }
}

@Composable
private fun DemoBox(label: String, content: @Composable () -> Unit) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
        Text(text = label, color = colors.labelText, style = typography.label)
    }
}
```
`PrimitivesSection.kt` should follow this exact `DemoBox`-per-variant + `Row`/`Column` grid shape, extended to cover: surface, glow ring, thumb, groove, gloss, shadow — each × three themes × states (D-04). Three-theme coverage means the gallery composable will need to be invoked (or wrapped) once per `AeroColorScheme` preset (`AeroBlue`/`AeroDark`/`Classic`) — check how the existing showcase app switches themes (likely a top-level theme-picker state) before assuming `AeroTheme.colors` alone suffices; not confirmed in this pass, worth a quick showcase-app-root check during planning.

---

### `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt` (component/scratch spike, request-response)

**Analog:** `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` (exact — same throwaway-but-compile-proven pattern, same package)

**Structural pattern to copy** (full file shape, 92 lines — package, `internal @Composable fun Scratch...()`, heavy KDoc documenting what was proven/discovered, a single self-contained `Box`/demo composable, explicit "SCOPE GUARD: do NOT wire into any real showcase screen" comment):
```kotlin
package com.mordred.showcase.scratch

import androidx.compose.runtime.Composable
// ...

/**
 * PHASE 16 SPIKE ARTIFACT (PRIM-18).
 * ... document what was proven/discovered about M3 Slider thumb/track slot sizing ...
 * SCOPE GUARD: this is a compile+visual proof, not a production primitive. Do NOT wire
 * this into any real showcase screen or component.
 */
@Composable
internal fun ScratchSliderSlotSpike() {
    // M3 Slider with custom-sized thumb=/track= slots, consuming aeroThumbSurface
    // from AeroSurfacePrimitives.kt once that exists
}
```

---

## Shared Patterns

### Theme-token access — `LocalAeroColors` / `AeroTheme.colors`
**Source:** `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` lines 23-24, 96-100
```kotlin
public val LocalAeroColors: ProvidableCompositionLocal<AeroColorScheme> =
    staticCompositionLocalOf { AeroColorScheme.AeroBlue }

public object AeroTheme {
    public val colors: AeroColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalAeroColors.current
}
```
**Apply to:** every new `theme/` file that reads color tokens (`AeroSurfacePrimitives.kt`'s Modifier extensions must read `LocalAeroColors.current` exactly like `GlassModifiers.kt` does); every showcase composable uses `AeroTheme.colors`/`AeroTheme.typography` (both `FoundationSection.kt` and the new `PrimitivesSection.kt`).

### Modifier-extension-as-`@Composable`-function convention
**Source:** `GlassModifiers.kt` (`glassEffect`, `glassPanel`, `glassSurface` — all `@Composable public fun Modifier.xxx(...): Modifier`)
**Apply to:** `Modifier.aeroSurface(style, shape)`, `Modifier.aeroGlowRing(...)`, `Modifier.aeroThumbSurface(...)` — same `@Composable public fun Modifier.xxx(...): Modifier` shape, reading `LocalAeroColors.current` internally so call sites don't have to pass colors explicitly.

### Hover/press/focus wiring — `hoverable()` + `collectIsXAsState()`
**Source:** `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` lines 6-10, 54-57, 74
**Apply to:** any Phase 17-19 component consuming `rememberAeroInteractionState()` — must pair `Modifier.hoverable(interactionSource)` (or `.clickable(interactionSource = ...)`) with the state collector, exactly as `AeroListItem` does. This phase's `InteractionStates.kt` extension does not itself need this wiring (it only collects state), but the KDoc/example for `rememberAeroInteractionState()` should reference this exact call site per RESEARCH.md Pattern 4.

### Gradient fade target — `baseColor.copy(alpha = 0f)`, never `Color.Transparent`
**Source:** negative examples at `GlassModifiers.kt` lines 66, 93 (`Color.Transparent` — the anti-pattern to avoid in new code, confirmed present in both `glassPanel` and `glassSurface`)
**Apply to:** every new gradient in `AeroSurfacePrimitives.kt` (PRIM-14) — fades must target `baseColor.copy(alpha = 0f)` so `AeroColorScheme.Classic`'s opaque tokens (e.g. `glassSurface = Color(0xFF333333)`, `AeroColorScheme.kt:106`) degrade to "less of themselves," not a flat opaque block.

### `drawWithCache` vs `drawBehind` for geometry/brush construction
**Source:** negative example — `GlassModifiers.kt` lines 60-73 and 88-104, both allocate `Brush.verticalGradient(...)` fresh inside `drawBehind {}` on every draw pass (PRIM-13's confirmed bug, not to be repeated)
**Apply to:** `AeroSurfacePrimitives.kt`'s `Modifier.aeroSurface()` — geometry/brush built once in `drawWithCache { onDrawBehind { ... } }` per RESEARCH.md Architecture Pattern 1's skeleton; only cheap draw calls / animated-`State` reads happen inside the per-frame block.

## No Analog Found

None. Every file in this phase's scope has at least a role-match analog already in the codebase — consistent with this being a "generalize existing proven patterns" phase rather than a greenfield one (per RESEARCH.md's framing: "Nothing here is genuinely novel Compose API surface").

## Metadata

**Analog search scope:** `library/src/main/kotlin/com/mordred/aero/theme/`, `library/src/main/kotlin/com/mordred/aero/components/{buttons,list,range}/`, `showcase/src/main/kotlin/com/mordred/showcase/{sections,scratch}/`
**Files scanned:** 8 (GlassModifiers.kt, AeroColorScheme.kt, AeroTheme.kt, InteractionStates.kt, AeroListItem.kt, AeroRangeSlider.kt (partial, lines 1-120), ScratchAeroShadowProof.kt, FoundationSection.kt)
**Pattern extraction date:** 2026-07-22
