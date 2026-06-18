package com.mordred.aero.components.range

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
