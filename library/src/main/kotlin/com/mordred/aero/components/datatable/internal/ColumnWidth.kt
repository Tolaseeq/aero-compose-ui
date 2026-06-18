package com.mordred.aero.components.datatable.internal

import com.mordred.aero.components.datatable.AeroColumnWidth

/** One column's resolved layout width, in dp units (Float). */
internal data class ResolvedWidth(val dpValue: Float)

/**
 * Resolve Fixed/Weight column widths against available width (px).
 * Fixed -> max(fixedDp, minWidthDp). Weight -> proportional share of remaining px,
 * clamped to minWidthDp. No column resolves below its minWidth (DATA-04: no 0dp collapse).
 *
 * @param widths column width specs in order.
 * @param minWidthsDp per-column minWidth in dp (same order/size as [widths]).
 * @param totalWidthPx available width in px.
 * @param pxPerDp density factor (px per dp); pass 1f in pure tests.
 */
internal fun resolveColumnWidths(
    widths: List<AeroColumnWidth>,
    minWidthsDp: List<Float>,
    totalWidthPx: Float,
    pxPerDp: Float,
): List<ResolvedWidth> {
    val fixedPx = widths.sumOf { w ->
        when (w) {
            is AeroColumnWidth.Fixed -> (w.dp.value * pxPerDp).toDouble()
            is AeroColumnWidth.Weight -> 0.0
        }
    }.toFloat()
    val totalWeight = widths.sumOf { w ->
        when (w) { is AeroColumnWidth.Weight -> w.value.toDouble(); else -> 0.0 }
    }.toFloat()
    val remainingPx = maxOf(0f, totalWidthPx - fixedPx)
    return widths.mapIndexed { i, w ->
        val minDp = minWidthsDp[i]
        when (w) {
            is AeroColumnWidth.Fixed -> ResolvedWidth(maxOf(w.dp.value, minDp))
            is AeroColumnWidth.Weight -> {
                val sharePx = if (totalWeight > 0f) remainingPx * w.value / totalWeight else 0f
                val shareDp = sharePx / pxPerDp
                ResolvedWidth(maxOf(shareDp, minDp))
            }
        }
    }
}
