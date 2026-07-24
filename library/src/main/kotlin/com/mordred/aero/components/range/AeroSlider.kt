package com.mordred.aero.components.range

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroOrnamentTokens
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGlowRing
import com.mordred.aero.theme.aeroGroove
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.aeroThumbSurface
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.glassEffect
import com.mordred.aero.theme.hoverLighten
import com.mordred.aero.theme.lighten

/** Custom thumb slot diameter/radius — matches [AeroRangeSlider]'s own locked thumb size
 * (18-UI-SPEC.md Spacing Scale) so the two sibling sliders read as one visual family. M3's own
 * default thumb (`SliderTokens.HandleWidth`/`HandleHeight`, confirmed 4.dp/44.dp by direct
 * bytecode+source read of the pinned `material3-desktop-1.9.0` artifact per VER-03/TOOL-07
 * discipline) is a tall vertical "expressive" pill, not a circle — 18-RESEARCH.md's own Code
 * Examples note explicitly warns AeroSlider's actual thumb/track dp values are NOT M3's raw
 * token literals; [aeroThumbSurface] is a circle-only primitive (PRIM-07), so reusing the raw
 * M3 pill dimensions would clip to a near-invisible dot rather than a readable Aero thumb nub. */
private val THUMB_DIAMETER = 20.dp
private val THUMB_RADIUS = 10.dp

/** Custom track slot thickness — matches [AeroRangeSlider]'s own locked 4.dp track thickness
 * (18-UI-SPEC.md Spacing Scale), same "thin track" family the accent-fill gloss (D-02) is
 * proportioned against. */
private val TRACK_HEIGHT = 4.dp
private val TRACK_CORNER_RADIUS = 2.dp

/**
 * Aero-styled slider wrapping [Slider] from Material3 (VRNG-01) — M3's layout/drag/keyboard/
 * `steps`-snap/semantics machinery is kept entirely; only the `thumb =`/`track =` custom slots
 * are supplied, painting through the shared Aero primitives (Phase 16) instead of
 * [androidx.compose.material3.SliderDefaults]. Classic Win7 slider identity (D-01): the thumb
 * and inactive track are neutral raised/recessed glass ([AeroSurfaceStyle.neutralRest]), the
 * active (filled) track segment is accent two-tone glass ([AeroSurfaceStyle.rest]) matching
 * Phase 17's filled `AeroButton` family.
 *
 * When the user drags the thumb and [showTooltip] is true, a glass tooltip appears
 * above the slider centre showing the current [value] formatted to 2 decimals.
 *
 * TODO(Phase 3): track tooltip x to thumb position — currently centred above the track.
 *
 * @param value Current value within [valueRange].
 * @param onValueChange Emits new Float values while dragging.
 * @param modifier Layout modifier.
 * @param enabled Whether interactions are enabled (disabled state flattens to dead per D-07 —
 * no reliance on M3 [androidx.compose.material3.SliderColors] disabled fields).
 * @param valueRange The permitted range of [value]. Default 0f..1f.
 * @param steps Number of discrete steps (0 = continuous).
 * @param showTooltip Show a glass pill above the slider while dragging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AeroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    val state = rememberAeroInteractionState(interactionSource)

    val thumbStyle = resolveSliderThumbStyle(
        colors = colors,
        hovered = state.hovered,
        pressed = state.pressed,
        isDragging = isDragging,
        focused = state.focused,
        enabled = enabled,
    )
    val trackStyle = resolveSliderTrackStyle(colors, enabled)
    val grooveStyle = AeroSurfaceStyle.neutralRest(colors, cornerRadius = TRACK_CORNER_RADIUS).let {
        if (enabled) it else it.flattenDisabled(colors)
    }
    val hoverGlow = AeroOrnamentTokens.derive(colors).hoverGlow
    val interacting = state.hovered || state.pressed || isDragging
    val interactionGlowColor = if (state.pressed || isDragging) hoverGlow.lighten(0.15f) else hoverGlow

    Box(modifier = modifier.fillMaxWidth()) {
        if (isDragging && showTooltip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .glassEffect(cornerRadius = 4.dp, elevation = 2.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "%.2f".format(value),
                    color = colors.onSurface,
                    style = AeroTheme.typography.label
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            onValueChangeFinished = null,
            interactionSource = interactionSource,
            steps = steps,
            thumb = {
                Box(
                    Modifier
                        .size(THUMB_DIAMETER)
                        .hoverable(interactionSource)
                        .aeroGlowRing(
                            active = state.focused && enabled,
                            glowColor = colors.borderSelected,
                            cornerRadius = THUMB_RADIUS,
                        )
                        .aeroGlowRing(
                            active = interacting && enabled,
                            glowColor = interactionGlowColor,
                            cornerRadius = THUMB_RADIUS,
                        )
                        .aeroThumbSurface(thumbStyle)
                )
            },
            track = { sliderState ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TRACK_HEIGHT)
                        .aeroGroove(grooveStyle)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(sliderState.coercedValueAsFraction)
                            .height(TRACK_HEIGHT)
                            .aeroSurface(trackStyle, RoundedCornerShape(TRACK_CORNER_RADIUS))
                    )
                }
            },
            valueRange = valueRange,
        )
    }
}

/**
 * Pure, Compose-free per-state resolution point for [AeroSlider]'s custom thumb slot
 * (18-UI-SPEC.md "Neutral thumb" per-state contract), mirroring
 * [com.mordred.aero.components.buttons.resolveButtonStyle]'s precedence chain shape (disabled
 * wins over everything, then pressed/dragging, then hovered, else rest) — reused verbatim by
 * [AeroRangeSlider]'s per-thumb resolution (Plan 02, VRNG-05).
 *
 * D-04's deliberate divergence from the button precedent: press/drag does NOT call
 * [com.mordred.aero.theme.pressedRecess] (a dragged thumb must NOT invert/recede — it stays
 * RAISED, "picked up"). Instead the gloss is brightened by a small fixed delta
 * ([PRESSED_GLOSS_BOOST]) on top of the unmodified neutral fill.
 *
 * [focused] is accepted for API symmetry with [com.mordred.aero.components.buttons.resolveButtonStyle]
 * but does not branch here — focus is expressed purely by [AeroSlider]'s persistent
 * [aeroGlowRing] focus call, never a fill delta (matching Phase 17 D-04).
 */
internal fun resolveSliderThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    isDragging: Boolean,
    focused: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = THUMB_RADIUS)
    if (!enabled) return rest.flattenDisabled(colors)
    return when {
        pressed || isDragging -> rest.copy(glossAlpha = rest.glossAlpha + PRESSED_GLOSS_BOOST)
        hovered -> rest.hoverLighten()
        else -> rest
    }
}

/** Gloss-alpha delta applied on top of [AeroSurfaceStyle.neutralRest] while the thumb is
 * pressed/dragged (D-04) — a slightly brighter gloss as the active-drag cue, never
 * [com.mordred.aero.theme.pressedRecess]'s invert-and-recede transform. */
private const val PRESSED_GLOSS_BOOST: Float = 0.06f

/**
 * Pure, Compose-free resolver for [AeroSlider]'s custom track slot's ACCENT active-fill segment
 * (18-UI-SPEC.md "Active accent fill" per-state contract) — no hover/press states of its own
 * (VRNG-03 scopes interactive states to the thumb, not the fill). Reused verbatim by
 * [AeroRangeSlider]'s between-thumbs active segment (Plan 02).
 */
internal fun resolveSliderTrackStyle(colors: AeroColorScheme, enabled: Boolean): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.rest(colors, cornerRadius = TRACK_CORNER_RADIUS)
    return if (enabled) rest else rest.flattenDisabled(colors)
}
