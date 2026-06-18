package com.mordred.aero.components.range

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassEffect
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Which thumb of the dual-thumb [AeroRangeSlider] a drag/move refers to.
 */
internal enum class RangeThumb { Start, End }

/**
 * Snap a raw value to the discrete grid when [steps] > 0; identity when [steps] == 0.
 *
 * With `steps` discrete intervals there are `steps + 1` snap positions evenly spaced
 * across [range]. The result is always coerced back into [range].
 */
internal fun snapToStep(
    raw: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): Float {
    if (steps <= 0) return raw.coerceIn(range.start, range.endInclusive)
    val span = range.endInclusive - range.start
    if (span <= 0f) return range.start
    val stepSize = span / steps
    val index = ((raw - range.start) / stepSize).roundToInt().coerceIn(0, steps)
    return (range.start + index * stepSize).coerceIn(range.start, range.endInclusive)
}

/**
 * Minimum separation between the two thumbs.
 *
 * - `steps > 0` → exactly one step `(span / steps)` so thumbs cannot share a snap position.
 * - `steps == 0` → `span * 0.001f` so two thumbs cannot occupy the exact same value.
 */
private fun minSeparation(range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    val span = range.endInclusive - range.start
    return if (steps > 0) span / steps else span * 0.001f
}

/**
 * Apply a moved [thumb] to [current], enforcing `start <= end` and minimum separation.
 *
 * - [RangeThumb.Start]: new start is snapped then coerced into
 *   `range.start .. (current.endInclusive - minSep)`; end is unchanged.
 * - [RangeThumb.End]: new end is snapped then coerced into
 *   `(current.start + minSep) .. range.endInclusive`; start is unchanged.
 */
internal fun applyThumbMove(
    current: ClosedFloatingPointRange<Float>,
    thumb: RangeThumb,
    rawValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): ClosedFloatingPointRange<Float> {
    val minSep = minSeparation(range, steps)
    return when (thumb) {
        RangeThumb.Start -> {
            val upper = (current.endInclusive - minSep).coerceAtLeast(range.start)
            val newStart = snapToStep(rawValue, range, steps).coerceIn(range.start, upper)
            newStart..current.endInclusive
        }
        RangeThumb.End -> {
            val lower = (current.start + minSep).coerceAtMost(range.endInclusive)
            val newEnd = snapToStep(rawValue, range, steps).coerceIn(lower, range.endInclusive)
            current.start..newEnd
        }
    }
}

/**
 * Map an x pixel offset across [trackWidth] to a value within [range].
 */
internal fun xToValue(
    x: Float,
    range: ClosedFloatingPointRange<Float>,
    trackWidth: Float,
): Float {
    if (trackWidth <= 0f) return range.start
    val fraction = (x / trackWidth).coerceIn(0f, 1f)
    return range.start + fraction * (range.endInclusive - range.start)
}

/**
 * Map a value [v] within [range] to an x pixel offset across [trackWidth].
 */
internal fun valueToX(
    v: Float,
    range: ClosedFloatingPointRange<Float>,
    trackWidth: Float,
): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0f
    val fraction = ((v - range.start) / span).coerceIn(0f, 1f)
    return fraction * trackWidth
}

/**
 * Z-order helper: given the [lastMovedThumb], returns the OTHER thumb — the one that should
 * be drawn FIRST so that [lastMovedThumb] draws last (on top) when both thumbs overlap.
 *
 * [lastMovedThumb] defaults to [RangeThumb.End].
 */
internal fun thumbToDrawFirst(lastMovedThumb: RangeThumb = RangeThumb.End): RangeThumb =
    when (lastMovedThumb) {
        RangeThumb.End -> RangeThumb.Start
        RangeThumb.Start -> RangeThumb.End
    }

/**
 * Aero-styled dual-thumb range slider (PICK-08).
 *
 * Two independently draggable thumbs share a single horizontal track; the segment between
 * them is filled with `colors.primary`. The thumbs cannot cross (`start <= end` always holds),
 * and the most-recently-moved thumb is drawn on top when the two overlap.
 *
 * **PITFALL-03 mitigation:** drag uses a custom [Canvas] + `awaitPointerEventScope` manual loop.
 * Material3 `RangeSlider` and the gesture-detector drag helpers are BANNED — their 18dp Desktop
 * touchSlop silently swallows the first pixels of pixel-precise drags. Here drag responds on the
 * first mouse-down (click-to-set) and every subsequent move.
 *
 * @param value Current selected range within [valueRange].
 * @param onValueChange Emits a new range while dragging; `start <= endInclusive` is guaranteed.
 * @param modifier Layout modifier (the slider fills available width, 48dp tall).
 * @param enabled When false, pointer input is ignored and the active track renders with reduced emphasis.
 * @param valueRange Permitted range for both thumbs. Default `0f..1f`.
 * @param steps Number of discrete steps (0 = continuous). Also sets minimum thumb separation.
 * @param showTooltip Show a glass value pill above the active thumb while dragging.
 */
@Composable
public fun AeroRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true,
) {
    val colors = AeroTheme.colors
    val density = LocalDensity.current

    // Live-value read inside the drag loop: rememberUpdatedState ensures the lambda captures
    // the latest committed value on every recomposition, so applyThumbMove always carries the
    // correct non-dragged endpoint over (fixes F9 other-thumb reset).
    val currentValue by rememberUpdatedState(value)

    var lastMovedThumb by remember { mutableStateOf(RangeThumb.End) }
    var trackWidthPx by remember { mutableStateOf(0f) }
    var activeThumb by remember { mutableStateOf<RangeThumb?>(null) }

    Box(modifier = modifier.fillMaxWidth().height(48.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(enabled, valueRange, steps) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        val width = size.width.toFloat()
                        trackWidthPx = width
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Pick the nearest thumb to the press point using the LIVE range.
                            val startX = valueToX(currentValue.start, valueRange, width)
                            val endX = valueToX(currentValue.endInclusive, valueRange, width)
                            val thumb = if (
                                abs(down.position.x - startX) <= abs(down.position.x - endX)
                            ) {
                                RangeThumb.Start
                            } else {
                                RangeThumb.End
                            }
                            lastMovedThumb = thumb
                            activeThumb = thumb
                            onValueChange(
                                applyThumbMove(
                                    currentValue,
                                    thumb,
                                    xToValue(down.position.x, valueRange, width),
                                    valueRange,
                                    steps,
                                ),
                            )
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                onValueChange(
                                    applyThumbMove(
                                        currentValue,
                                        thumb,
                                        xToValue(change.position.x, valueRange, width),
                                        valueRange,
                                        steps,
                                    ),
                                )
                                change.consume()
                            }
                            activeThumb = null
                        }
                    }
                },
        ) {
            val width = size.width
            trackWidthPx = width
            val centerY = size.height / 2f
            val trackThickness = 4.dp.toPx()
            val thumbRadius = 10.dp.toPx()

            val startX = valueToX(value.start, valueRange, width)
            val endX = valueToX(value.endInclusive, valueRange, width)

            // 1. Inactive track (full width).
            drawLine(
                color = colors.borderDefault,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = trackThickness,
            )

            // 2. Active track between the two thumbs.
            drawLine(
                color = if (enabled) colors.primary else colors.borderDefault,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = trackThickness,
            )

            // 3. Thumbs — draw the non-last-moved first so lastMoved lands on top.
            fun drawThumb(x: Float) {
                drawCircle(
                    color = if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f),
                    radius = thumbRadius,
                    center = Offset(x, centerY),
                )
                drawCircle(
                    color = colors.onPrimary,
                    radius = thumbRadius,
                    center = Offset(x, centerY),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            val firstThumb = thumbToDrawFirst(lastMovedThumb)
            val firstX = if (firstThumb == RangeThumb.Start) startX else endX
            val secondX = if (firstThumb == RangeThumb.Start) endX else startX
            drawThumb(firstX)
            drawThumb(secondX)
        }

        // Glass value tooltip above the active thumb (mirrors AeroSlider's glass pill).
        val current = activeThumb
        if (showTooltip && current != null && trackWidthPx > 0f) {
            val thumbValue = if (current == RangeThumb.Start) value.start else value.endInclusive
            val thumbXPx = valueToX(thumbValue, valueRange, trackWidthPx)
            val thumbXDp = with(density) { thumbXPx.toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = thumbXDp - 16.dp)
                    .glassEffect(cornerRadius = 4.dp, elevation = 2.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "%.2f".format(thumbValue),
                    color = colors.onSurface,
                    style = AeroTheme.typography.label,
                )
            }
        }
    }
}
