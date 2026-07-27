package com.mordred.aero.components.selection

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroOrnamentTokens
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGlowRing
import com.mordred.aero.theme.aeroGroove
import com.mordred.aero.theme.aeroThumbSurface
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten

/**
 * SEL-03 / 19-02 restyle: Aero toggle switch — a recessed accent-lerped groove
 * ([Modifier.aeroGroove], PRIM-08) carrying a raised, glossy, shadowed neutral thumb
 * ([Modifier.aeroThumbSurface], PRIM-07), plus its first-ever hover / press / focus / disabled
 * states.
 *
 * Identity (D-01): the accent lives in the groove; the thumb stays a NEUTRAL raised glass nub in
 * every state, checked or not — [AeroSlider]'s sibling. The off-state groove uses the identical
 * geometry as the checked groove (D-02); only the two fill stops move, lerped by the checked
 * progress, so the animation has exactly one moving part.
 *
 * Layering (D-03): the thumb `Box` is a deliberate SIBLING of the groove `Box`, drawn after it so
 * it paints on top, and is not nested inside whichever `Box` carries the groove's clip — its drop
 * shadow and glow may therefore visually exceed the 36x18dp track, while the declared layout size
 * stays exactly 36x18dp and the thumb stays 14dp travelling x = 2.dp to 20.dp. A later reader
 * should not "tidy" the thumb back into a nested child of the groove; doing so would clip its
 * shadow against the 18dp-tall recess and flatten it to a neutral circle.
 *
 * Press keeps the thumb raised with a brighter gloss (D-05) — never the button's inverted-recess
 * transform; a thumb riding in an already-recessed groove reads as picked up, not pushed in.
 * Hover puts an outer glow ring on the whole track plus a brightened thumb gloss (D-06); focus
 * puts a persistent glow ring on the same track using a distinct token so hover and focus stay
 * tellable apart when co-active. Both glow calls are chained before any clip-applying primitive
 * on this modifier chain, per `AeroSurfacePrimitives.kt`'s ordering rule.
 *
 * Track: 36x18dp rounded pill. Thumb: 14x14dp circle. Thumb animates from x=2dp (off) to x=20dp
 * (on) via a single `animateFloatAsState` tween(150) that drives both the thumb offset and the
 * groove's fill lerp (D-07/P-03 — exactly one animated value).
 */
@Composable
public fun AeroSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AeroTheme.colors
    val state = rememberAeroInteractionState(interactionSource)

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "switchThumb"
    )

    Box(
        modifier = modifier
            .width(36.dp)
            .height(18.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) }
            )
            .hoverable(interactionSource)
            .aeroGlowRing(
                active = state.focused && enabled,
                glowColor = colors.borderSelected,
                cornerRadius = TRACK_CORNER_RADIUS,
            )
            .aeroGlowRing(
                active = state.hovered && enabled,
                glowColor = AeroOrnamentTokens.derive(colors).hoverGlow,
                cornerRadius = TRACK_CORNER_RADIUS,
            )
    ) {
        // Groove — a child Box whose own .clip lives here, local to this Box (D-03).
        Box(
            modifier = Modifier
                .matchParentSize()
                .aeroGroove(resolveSwitchGrooveStyle(colors, thumbProgress, enabled))
        )

        // Thumb — a SIBLING of the groove Box, declared after it so it paints on top; NOT nested
        // inside the groove's clip, so its drop shadow/glow escape the 18dp-tall recess (D-03).
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((2.dp + 18.dp * thumbProgress).roundToPx(), 0) }
                .size(14.dp)
                .aeroThumbSurface(resolveSwitchThumbStyle(colors, state.hovered, state.pressed, enabled))
        )
    }
}

/** Half of the 18.dp track height — the groove's corner radius. */
private val TRACK_CORNER_RADIUS = 9.dp

/** Half of the 14.dp thumb diameter — the thumb's corner radius. */
private val THUMB_CORNER_RADIUS = 7.dp

/** Gloss-alpha delta applied on top of [AeroSurfaceStyle.neutralRest] while the thumb is pressed
 * (D-05) — a slightly brighter gloss as the "picked up" cue, never a recess/invert transform.
 * Declared privately here rather than widening [AeroSlider]'s own `PRESSED_GLOSS_BOOST` (which is
 * scoped to the `range` package) — the thumb gloss magnitude is an explicit Claude's-discretion,
 * per-component value (19-CONTEXT.md), unlike the shared inner-shadow value VSEL-03 requires. */
private const val PRESSED_GLOSS_BOOST: Float = 0.10f

/** Native drop shadow attached to the raised thumb (P-06) — a small radius, low-alpha black,
 * downward offset, matching Phase 16 D-02's native-shadow-first precedent. */
private val THUMB_DROP_SHADOW: Shadow = Shadow(
    radius = 2.dp,
    color = Color.Black.copy(alpha = 0.30f),
    offset = DpOffset(0.dp, 1.dp),
)

/**
 * Pure, Compose-free resolver for [AeroSwitch]'s recessed track groove (19-UI-SPEC.md `AeroSwitch`
 * per-state contract). Builds the neutral (off) and accent (on) rest styles at the SAME corner
 * radius and lerps only the two fill stops by [checkedProgress] — gloss, bevel, rim and corner
 * radius all come from the neutral style in BOTH states (D-02: identical groove geometry, only
 * the fill color moves). [enabled] is the terminal transform via [flattenDisabled], mirroring
 * [com.mordred.aero.components.buttons.resolveButtonStyle]'s disabled-is-terminal shape.
 */
internal fun resolveSwitchGrooveStyle(
    colors: AeroColorScheme,
    checkedProgress: Float,
    enabled: Boolean,
): AeroSurfaceStyle {
    val neutral = AeroSurfaceStyle.neutralRest(colors, cornerRadius = TRACK_CORNER_RADIUS)
    val accent = AeroSurfaceStyle.rest(colors, cornerRadius = TRACK_CORNER_RADIUS)
    val resolved = neutral.copy(
        fillTop = lerp(neutral.fillTop, accent.fillTop, checkedProgress),
        fillBottom = lerp(neutral.fillBottom, accent.fillBottom, checkedProgress),
    )
    return if (enabled) resolved else resolved.flattenDisabled(colors)
}

/**
 * Pure, Compose-free resolver for [AeroSwitch]'s raised neutral thumb (19-UI-SPEC.md `AeroSwitch`
 * per-state contract), mirroring [com.mordred.aero.components.range.resolveSliderThumbStyle]'s
 * precedence chain (disabled wins over everything, then pressed, then hovered, else rest) minus
 * the slider's `isDragging` axis — the switch thumb has no drag state of its own.
 *
 * D-05's deliberate divergence from the button precedent: pressed does NOT call the button's
 * pressed-recess transform (a thumb riding in an already-recessed groove must stay RAISED,
 * "picked up") — instead the gloss is brightened by [PRESSED_GLOSS_BOOST] on top of the
 * unmodified neutral fill. Disabled short-circuits ahead of the drop shadow being attached — a
 * dead thumb must not float, so the disabled branch returns the shadowless neutral rest flattened,
 * never [THUMB_DROP_SHADOW] plus a flattened fill.
 */
internal fun resolveSwitchThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = THUMB_CORNER_RADIUS)
    if (!enabled) return rest.flattenDisabled(colors)
    val raised = rest.copy(dropShadow = THUMB_DROP_SHADOW)
    return when {
        pressed -> raised.copy(glossAlpha = raised.glossAlpha + PRESSED_GLOSS_BOOST)
        hovered -> raised.hoverLighten()
        else -> raised
    }
}
