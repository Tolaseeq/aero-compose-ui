package com.mordred.aero.components.selection

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.PRESSED_INNER_SHADOW
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.darken
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import com.mordred.aero.theme.pressedRecess

/**
 * SEL-05 / VSEL-03 / VSEL-04: Segmented control enforcing exactly one selected option, restyled
 * as a Win7 toolbar strip — raised glass segments with exactly one visibly pushed in.
 *
 * The selected segment's recess is the Phase 17 pressed-button code path reached by IMPORT
 * ([pressedRecess] plus [PRESSED_INNER_SHADOW], both consumed from the buttons/theme packages),
 * never re-derived locally (VSEL-03) — see [resolveSegmentStyle]'s KDoc for the exact composition.
 *
 * This component deliberately does NOT use the library's outer glow-ring bloom for hover or focus
 * (D-10): the segments sit flush inside the outer [Row]'s clipped shape, so an outer bloom would
 * be sliced by that frame AND bleed onto neighbouring segments. Hover and focus instead render
 * entirely inside each segment's own bounds — hover via [hoverLighten] composed onto the resolved
 * fill, focus via an inset rounded-rect stroke drawn as part of the segment's own paint region. A
 * future reviewer should not "fix" this back to the outer bloom for consistency with the rest of
 * the library — the in-bounds treatment is required by this component's own clipped geometry, not
 * an oversight.
 *
 * The in-bounds focus stroke draws only for keyboard-acquired focus (`segState.focusVisible`, gap
 * G2/VSEL-04) — a mouse click on a segment leaves no stroke once the pointer moves away, while Tab
 * still walks every segment and draws it inside that segment's own bounds. Per-segment focusability
 * and the Space/Enter activation binding are unaffected by this gate: only whether the stroke is
 * DRAWN is suppressed, never whether the segment can be reached or activated by keyboard.
 *
 * The 1.dp inter-segment divider shipped previously is dropped (D-08/D-10): once every segment
 * carries its own raised or recessed bevel/rim contour via [aeroSurface], that divider is
 * redundant — each segment's own contour now supplies the visual break, the literal Win7 toolbar
 * idiom (adjacent raised buttons need no line drawn between them).
 *
 * Every segment label — selected or not — resolves from `colors.onSurface` at that token's own
 * alpha (gap G3, VSEL-03). The label previously animated to `colors.surface` when selected, which
 * was wrong twice over: (a) `surface` is a background/panel token, not a content token, being
 * used for text; and (b) on AeroBlue/AeroDark that token carries an `0xCC` alpha, so the selected
 * label was also rendered roughly 80% opaque — the "near-black, semi-transparent" defect the user
 * reported. Neither fault is fixed by substituting a different special-case colour for the
 * selected state — the fix is that there is no special case; contrast against the recessed fill
 * is restored instead by darkening that fill (see [resolveSegmentStyle]'s `RECESSED_FILL_DARKEN`
 * KDoc). A future reviewer must not reintroduce a per-state label colour to "fix" contrast.
 *
 * @param interactionSource Additive trailing parameter (D-04) observing the control as a whole;
 * each segment additionally holds its own remembered interaction source for per-segment
 * hover/press/focus, matching [com.mordred.aero.components.list.AeroListItem]'s shape.
 */
@Composable
public fun <T> AeroSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(SEGMENT_CORNER_RADIUS)

    Row(
        modifier = modifier
            .height(28.dp)
            .border(1.dp, colors.borderDefault, shape)
            .clip(shape)
            .hoverable(interactionSource)
    ) {
        options.forEachIndexed { index, opt ->
            val isSelected = (opt == selected)
            val segSource = remember { MutableInteractionSource() }
            val segState = rememberAeroInteractionState(segSource)

            val selectedProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(150, easing = LinearEasing),
                label = "segSelected_$index"
            )

            val style = resolveSegmentStyle(
                colors = colors,
                selectedProgress = selectedProgress,
                hovered = segState.hovered,
                pressed = segState.pressed,
                enabled = enabled,
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = segSource,
                        indication = null,
                        onClick = { onSelect(opt) },
                    )
                    .hoverable(segSource)
                    .aeroSurface(style, RoundedCornerShape(SEGMENT_CORNER_RADIUS))
                    .then(
                        if (segState.focusVisible && enabled) {
                            Modifier.drawBehind {
                                val strokePx = FOCUS_STROKE_WIDTH.toPx()
                                val cornerPx = SEGMENT_CORNER_RADIUS.toPx()
                                drawRoundRect(
                                    color = colors.borderSelected.copy(alpha = FOCUS_STROKE_ALPHA),
                                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                                    size = Size(size.width - strokePx, size.height - strokePx),
                                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                                    style = Stroke(width = strokePx),
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel(opt),
                    color = colors.onSurface,
                    style = AeroTheme.typography.bodyLarge
                )
            }
        }
    }
}

/** Corner radius of each segment's own fill and of the in-bounds focus stroke (VER-03: matches the outer 4.dp shape). */
private val SEGMENT_CORNER_RADIUS = 4.dp

/** Stroke width of the in-bounds focus cue (D-10). */
private val FOCUS_STROKE_WIDTH = 1.dp

/** Alpha of the in-bounds focus cue's stroke color. */
private const val FOCUS_STROKE_ALPHA: Float = 0.8f

/**
 * Downward value shift applied to the recessed selected segment's two fill stops, on top of the
 * imported [pressedRecess] geometry (gap G3, VSEL-03).
 *
 * A pushed-in surface catches less light and must read darker than its raised neighbours.
 * [pressedRecess] exchanges [AeroSurfaceStyle.fillTop]/[AeroSurfaceStyle.fillBottom] (plus the two
 * bevel tones), which preserves their mean exactly — the recessed style therefore has the same
 * average value as the raised style, differing only by gradient direction, missing gloss and the
 * inner shadow. On dark themes that is not a readable difference, so the recess needs a downward
 * value change on top of that geometry, not instead of it.
 *
 * `0.20f` matches [com.mordred.aero.components.buttons] `AeroButtonSurface`'s own filled-fill top
 * darken, so the recessed segment lands in the same value neighbourhood as the pressed `AeroButton`
 * it is compared against at sign-off. This magnitude is Claude's discretion (retunable at sign-off
 * per 19-CONTEXT.md); the DIRECTION — strictly darker, never inverted or re-tinted — is not.
 */
private const val RECESSED_FILL_DARKEN: Float = 0.20f

/**
 * Pure, Compose-free per-segment resolution point (19-UI-SPEC.md "AeroSegmentedControl —
 * per-state contract"), consumed by [AeroSegmentedControl].
 *
 * [selectedProgress] is the 150ms-animated 0f..1f selection axis (D-07 — the one semantic
 * transition this component keeps animating). [pressed] and [hovered] resolve instantly, no
 * animation (D-07). An unselected-but-pressed segment reaches full recess immediately rather than
 * riding the selection tween — [pressed] is folded into an `effectiveProgress` that snaps to 1f
 * the instant a segment is pressed, so the Press row of the UI-SPEC's per-state table (a segment
 * recesses momentarily while pressed, and press on an already-selected/recessed segment is a
 * visual no-op) holds exactly regardless of where the selection animation currently sits.
 *
 * `target` picks whichever of the raised base or the recessed style [effectiveProgress] currently
 * favors — gloss/bevel/rim/innerShadow switch instantly with `target` (never mid-lerp), while only
 * `fillTop`/`fillBottom` are interpolated across [effectiveProgress] (P-05) — the shipped
 * observable animation was a background-color fade, so a fill-stop lerp is its faithful
 * translation. Precedence is disabled-wins, then depth (selected-or-pressed), then hover —
 * mirroring [com.mordred.aero.components.buttons.resolveButtonStyle].
 *
 * Invariant asserted at value level by `AeroSegmentedControlStylesTest` (the VSEL-03 anti-drift
 * guarantee): at `selectedProgress = 1f, pressed = false, hovered = false, enabled = true` this
 * returns a style identical to an independently reconstructed
 * `AeroSurfaceStyle.rest(colors, 4.dp).pressedRecess(PRESSED_INNER_SHADOW)` in every field EXCEPT
 * `fillTop`/`fillBottom`, which additionally carry this component's own documented downward value
 * shift ([RECESSED_FILL_DARKEN], gap G3) — the recessed style is the imported pressed-button
 * transform, cross-package reuse unchanged, with only its two fill stops darkened afterward; at
 * `selectedProgress = 0f` (same other args) it is byte-identical to
 * `AeroSurfaceStyle.rest(colors, 4.dp)` unmodified. The anti-drift guarantee now lives in this
 * phrasing and in the test's from-scratch reconstruction (which applies the same literal darken).
 */
internal fun resolveSegmentStyle(
    colors: AeroColorScheme,
    selectedProgress: Float,
    hovered: Boolean,
    pressed: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val base = AeroSurfaceStyle.rest(colors, cornerRadius = SEGMENT_CORNER_RADIUS)
    val pressedTransform = base.pressedRecess(PRESSED_INNER_SHADOW)
    val recessed = pressedTransform.copy(
        fillTop = pressedTransform.fillTop.darken(RECESSED_FILL_DARKEN),
        fillBottom = pressedTransform.fillBottom.darken(RECESSED_FILL_DARKEN),
    )
    val effectiveProgress = if (pressed) 1f else selectedProgress
    val target = if (effectiveProgress >= 0.5f) recessed else base
    val depthResolved = target.copy(
        fillTop = lerp(base.fillTop, recessed.fillTop, effectiveProgress),
        fillBottom = lerp(base.fillBottom, recessed.fillBottom, effectiveProgress),
    )
    if (!enabled) return depthResolved.flattenDisabled(colors)
    return if (hovered) depthResolved.hoverLighten() else depthResolved
}
