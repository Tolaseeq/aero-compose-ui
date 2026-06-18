package com.mordred.aero.components.layout.internal.splitpane

/**
 * Pure clamp + fraction<->px helpers for [com.mordred.aero.components.layout.AeroSplitPane].
 *
 * No Compose imports — these are plain JVM functions, fully unit-testable without a Compose runtime.
 *
 * PITFALL-14 clamp formula: `(dividerPx + delta).coerceIn(minFirst.toPx(), total - minSecond.toPx())`
 * The caller computes `maxPx = totalPx - minSecondPaneSize.toPx()` and passes it in.
 */

/**
 * Clamps the divider position after applying [deltaPx].
 *
 * @param currentPx current divider position in px
 * @param deltaPx drag delta to apply
 * @param minFirstPx minimum allowed position (from start edge); prevents first pane from collapsing
 * @param maxPx maximum allowed position; caller computes as `totalPx - minSecondPaneSize.toPx()`
 * @return new clamped divider position
 */
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float =
    (currentPx + deltaPx).coerceIn(minFirstPx, maxPx)

/**
 * Converts a split fraction [0f..1f] to an absolute pixel position.
 */
internal fun fractionToPx(fraction: Float, totalPx: Float): Float = fraction * totalPx

/**
 * Converts an absolute pixel position back to a split fraction [0f..1f].
 * Returns 0f if [totalPx] is zero or negative (divide-by-zero guard).
 */
internal fun pxToFraction(px: Float, totalPx: Float): Float = if (totalPx <= 0f) 0f else px / totalPx
