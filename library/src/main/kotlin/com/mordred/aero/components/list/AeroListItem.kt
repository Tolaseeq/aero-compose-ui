package com.mordred.aero.components.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten

/**
 * VLST-01: Aero-styled list row with hover and selection state.
 *
 * [ROW_MIN_HEIGHT] (36.dp) is a minimum, not a fixed height (G1): a single-line row still
 * resolves to exactly 36.dp, but a row carrying a primary label plus [secondaryText], or a
 * primary label wrapped onto multiple lines, grows past it instead of clipping its content.
 * Selection and hover render as a single clipped Aero pill (gradient fill + gloss + rim,
 * [PILL_VERTICAL_INSET] vertical inset, [PILL_CORNER_RADIUS] corner radius) resolved by
 * [resolveListItemPillStyle] — hover composes on top of the selected base rather than
 * replacing it (D-11), so an already-selected row that is also hovered reads strictly
 * brighter, never flatter. The pill and the focus-stroke [Box] are both measured against the
 * row's own resolved size (`Modifier.matchParentSize`, not a fill-the-parent size modifier —
 * see this plan's `<planner_finding>`), so both grow with the row instead of collapsing to zero height
 * once the row's height stops being a fixed value. A clickable row draws an in-bounds focus
 * stroke inside that same pill geometry only for keyboard-acquired focus (G2, VLST-03) — the
 * shared [com.mordred.aero.components.common.rememberFocusVisible] mechanism suppresses the
 * stroke for a mouse click while leaving the row focused and clickable either way, so Tab still
 * draws the stroke and a display-only row (`onClick == null`) never becomes a focus stop. The
 * stroke also stays inside the pill's own bounds instead of the library's outer glow-ring bloom:
 * list rows live inside scrolling/clipping containers (e.g. a lazy list) that would slice an
 * outer bloom against neighboring rows, so this component's focus cue deliberately stays inside
 * its own bounds (D-13) — do not "fix" it back to the outer bloom used elsewhere in the library.
 *
 * @param text Primary label text.
 * @param onClick Click handler. If null, the row is non-clickable and gains no focus stop.
 * @param modifier Layout modifier.
 * @param enabled Whether the item responds to interaction.
 * @param selected Whether the row displays a selection highlight.
 * @param leadingContent Optional composable rendered before the text column.
 * @param trailingContent Optional composable rendered after the text column.
 * @param secondaryText Optional secondary label rendered below [text].
 * @param interactionSource Interaction source — pass to share state with parent.
 */
@Composable
public fun AeroListItem(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val state = rememberAeroInteractionState(interactionSource)

    val pillStyle = resolveListItemPillStyle(
        colors = colors,
        selected = selected,
        hovered = state.hovered,
        enabled = enabled,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .hoverable(interactionSource)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (pillStyle != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = PILL_VERTICAL_INSET)
                    .aeroSurface(pillStyle, RoundedCornerShape(PILL_CORNER_RADIUS))
            )
        }
        if (state.focusVisible && enabled && onClick != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(vertical = PILL_VERTICAL_INSET)
                    .drawBehind {
                        val strokePx = FOCUS_STROKE_WIDTH.toPx()
                        val cornerPx = PILL_CORNER_RADIUS.toPx()
                        drawRoundRect(
                            color = colors.borderSelected.copy(alpha = FOCUS_STROKE_ALPHA),
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            style = Stroke(width = strokePx),
                        )
                    }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = ROW_VERTICAL_PADDING)
                .then(if (!enabled) Modifier.alpha(DISABLED_CONTENT_ALPHA) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingContent != null) {
                leadingContent()
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = text,
                    color = colors.onSurface,
                    style = AeroTheme.typography.bodyLarge
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        color = colors.labelText,
                        style = AeroTheme.typography.bodySmall
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/** Corner radius of the selection/hover pill and of the in-bounds focus stroke (VLST-01). */
private val PILL_CORNER_RADIUS: Dp = 6.dp

/** Vertical inset of the pill from the row's own resolved bounds; horizontal inset is 0.dp (full-bleed width). */
private val PILL_VERTICAL_INSET: Dp = 2.dp

/**
 * Minimum row height (G1, VLST-01). This is now a FLOOR, not a ceiling: a single-line row still
 * resolves to exactly this height (VER-03's locked single-line geometry is unchanged), but a row
 * whose content needs more — a secondary line, or a wrapped long primary label — grows past it
 * instead of clipping. The pill and focus [Box]es measure against the row's resolved size via
 * `Modifier.matchParentSize`, so they inherit whatever height the row actually grows to.
 */
private val ROW_MIN_HEIGHT: Dp = 36.dp

/**
 * Vertical padding on the content [Row] (G1). This is what keeps a grown row's text off the
 * pill's rounded edge — without it, text in a two-line row sits flush against the pill's bevel.
 */
private val ROW_VERTICAL_PADDING: Dp = 4.dp

/** Fraction the selected pill's rest() fill alphas are scaled by, so the accent reads as a highlight, not a solid fill. */
private const val SELECTED_FILL_ALPHA_SCALE: Float = 0.5f

/** Stroke width of the in-bounds focus cue (D-13). */
private val FOCUS_STROKE_WIDTH: Dp = 1.dp

/** Alpha of the in-bounds focus cue's stroke color. */
private const val FOCUS_STROKE_ALPHA: Float = 0.8f

/** Alpha applied to the content Row only when disabled — the pill itself flattens via [flattenDisabled]. */
private const val DISABLED_CONTENT_ALPHA: Float = 0.4f

/**
 * D-11's base-then-transform composition — the structural fix for VLST-02. The base resolves
 * from [selected] FIRST; [hovered] transforms whatever base was chosen SECOND, so a hovered
 * selected row always equals the resting-selected style with [hoverLighten] composed on top,
 * never a `when`-branch color pick where hover and selection compete for the same slot. Returns
 * `null` for the unselected + unhovered row — the call site then draws no pill at all, matching
 * the shipped "no background at rest" look exactly.
 *
 * Disabled is the terminal transform: when [enabled] is `false`, hover is not even considered
 * (`hovered && enabled` short-circuits to `false`), so a disabled row's pill is whichever
 * selected-or-null base would otherwise resolve, run through [flattenDisabled] — never a
 * hover-lightened variant.
 */
internal fun resolveListItemPillStyle(
    colors: AeroColorScheme,
    selected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle? {
    val selectedStyle = AeroSurfaceStyle.rest(colors, cornerRadius = PILL_CORNER_RADIUS).let { s ->
        s.copy(
            fillTop = s.fillTop.copy(alpha = s.fillTop.alpha * SELECTED_FILL_ALPHA_SCALE),
            fillBottom = s.fillBottom.copy(alpha = s.fillBottom.alpha * SELECTED_FILL_ALPHA_SCALE),
        )
    }
    val base: AeroSurfaceStyle? = if (selected) selectedStyle else null
    val withHover = if (hovered && enabled) {
        (base ?: AeroSurfaceStyle.neutralRest(colors, cornerRadius = PILL_CORNER_RADIUS)).hoverLighten()
    } else {
        base
    }
    return if (!enabled) withHover?.flattenDisabled(colors) else withHover
}
