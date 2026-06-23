package com.mordred.aero.components.layout.internal.panelgroup

/**
 * Pure-JVM distribution and clamp logic for AeroPanelGroup.
 *
 * No Compose imports — fully unit-testable without a Compose runtime.
 *
 * PNL-PITFALL-04 (N-section cascading clamp crash): [clampPanelDividerPx] carries the
 * same PITFALL-B `coerceAtLeast` guard as SplitClamp.kt, generalized to Σ-minSizes.
 * PNL-PITFALL-05 (share-transfer rounding gap): [shareTransferOnCollapse] assigns the
 * rounding remainder to the last expanded neighbor.
 * PNL-PITFALL-06 (lastExpandedPx overflow after window shrink): [lastExpandedFraction]
 * stores a fraction; [restoreFromFraction] multiplies by the CURRENT available so the
 * result is always bounded by `currentAvailableForExpanded`.
 * PNL-PITFALL-07 (wrong active-divider count): [activeDividerCount] uses zipWithNext so
 * non-adjacent expanded pairs are not counted.
 * PNL-PITFALL-02 (float drift): [distributePx] assigns the last expanded section with
 * `weight(1f)` semantics via remainder arithmetic so Σ expanded heights == availableForExpanded.
 */

/**
 * Clamps the above-section size after applying [deltaPx] during a drag gesture.
 *
 * @param aboveSizePx current size (in sizePx proportion units) of the section above the divider
 * @param deltaPx scaled drag delta (already in sizePx units — caller applies scale factor)
 * @param minAbovePx minimum allowed size for the above section (in sizePx units)
 * @param minBelowPx minimum allowed aggregate size for sections below (in sizePx units)
 * @param totalBudgetPx combined budget: aboveSizePx + belowSizePx (invariant during drag)
 * @return new clamped size for the above section
 *
 * REQ: PNL-10 / PNL-PITFALL-04. The `coerceAtLeast(minAbovePx)` on `maxAbovePx` is the
 * PITFALL-B guard: prevents `coerceIn(min, max)` from throwing [IllegalArgumentException]
 * when the window is squeezed below Σ minSizes (same guard as SplitClamp.kt line 22).
 */
internal fun clampPanelDividerPx(
    aboveSizePx: Float,
    deltaPx: Float,
    minAbovePx: Float,
    minBelowPx: Float,
    totalBudgetPx: Float,
): Float {
    val maxAbovePx = (totalBudgetPx - minBelowPx).coerceAtLeast(minAbovePx)  // PNL-PITFALL-04 / PITFALL-B guard
    return (aboveSizePx + deltaPx).coerceIn(minAbovePx, maxAbovePx)
}

/**
 * Computes the pixel budget available for expanded sections after reserving:
 * - one header strip per section (all sections always render a header — REQ PNL-02 / spike finding 1)
 * - one divider thickness per adjacent pair of expanded sections (zipWithNext — PNL-PITFALL-07)
 *
 * @param totalPx total container height in px
 * @param expanded boolean flags per section (true = expanded)
 * @param headerPx header strip height in px (reserved for every section regardless of state)
 * @param dividerPx divider thickness in px
 * @return pixel budget for expanded content; guarded to 0f when all sections are collapsed
 *
 * REQ: PNL-15 / spike finding 3. `.coerceAtLeast(0f)` prevents negative heights when
 * all sections are collapsed.
 */
internal fun computeAvailablePx(
    totalPx: Float,
    expanded: BooleanArray,
    headerPx: Float,
    dividerPx: Float,
): Float {
    // When no section is expanded there is no budget for expanded content (PNL-15).
    if (expanded.none { it }) return 0f
    val activeDividers = expanded.toList().zipWithNext().count { (a, b) -> a && b }
    // Every section reserves one headerPx (spike finding 1: reserve for ALL sections, not just collapsed).
    return (totalPx - expanded.size * headerPx - activeDividers * dividerPx).coerceAtLeast(0f)
}

/**
 * Returns the count of active (draggable) dividers between adjacent pairs of expanded sections.
 *
 * Uses zipWithNext so non-adjacent expanded sections — e.g. E/E/C/E — do NOT produce a
 * phantom divider between the two non-adjacent expanded sections. Pattern E/E/C/E → 1.
 *
 * REQ: PNL-06 / PNL-PITFALL-07.
 */
internal fun activeDividerCount(expanded: BooleanArray): Int =
    expanded.toList().zipWithNext().count { (a, b) -> a && b }

/**
 * Computes per-section render heights from the current [sizePx] weights and [totalPx].
 *
 * - Collapsed sections → [headerPx].
 * - Expanded sections → proportional share of [computeAvailablePx].
 * - Single expanded section → full `availableForExpanded`.
 * - All collapsed → [headerPx] for every section (availableForExpanded == 0).
 * - Last expanded section absorbs float rounding so Σ expanded heights == availableForExpanded
 *   exactly (PNL-PITFALL-02 / PNL-PITFALL-11 last-section remainder rule).
 *
 * REQ: PNL-04 / PNL-15 / PNL-PITFALL-02.
 */
internal fun distributePx(
    sizePx: FloatArray,
    expanded: BooleanArray,
    totalPx: Float,
    headerPx: Float,
    dividerPx: Float,
): FloatArray {
    val availableForExpanded = computeAvailablePx(totalPx, expanded, headerPx, dividerPx)
    val expandedIndices = sizePx.indices.filter { expanded[it] }
    val expandedSizeSum = expandedIndices.sumOf { sizePx[it].toDouble() }.toFloat()

    val result = FloatArray(sizePx.size) { headerPx }

    if (expandedIndices.isEmpty() || availableForExpanded <= 0f) {
        // All collapsed or no budget → every section gets headerPx (already set)
        return result
    }

    if (expandedSizeSum <= 0f) {
        // All expanded sizes are zero — distribute equally
        val share = availableForExpanded / expandedIndices.size
        var assigned = 0f
        expandedIndices.forEachIndexed { listIdx, sectionIdx ->
            val h = if (listIdx == expandedIndices.lastIndex) availableForExpanded - assigned else share
            result[sectionIdx] = h
            assigned += h
        }
        return result
    }

    // Proportional distribution with last-section remainder (PNL-PITFALL-02)
    var assigned = 0f
    expandedIndices.forEachIndexed { listIdx, sectionIdx ->
        val h = if (listIdx == expandedIndices.lastIndex) {
            availableForExpanded - assigned
        } else {
            (sizePx[sectionIdx] / expandedSizeSum) * availableForExpanded
        }
        result[sectionIdx] = h
        assigned += h
    }
    return result
}

/**
 * Distributes the collapsing section's [sizePx]\[[index]] proportionally to the remaining
 * expanded sections. The last remaining expanded neighbor absorbs the rounding remainder
 * so the total is conserved exactly (PNL-PITFALL-05).
 *
 * @param sizePx current size weights (will not be mutated)
 * @param expanded current expanded flags (caller updates these after the call)
 * @param index index of the section being collapsed
 * @return new [sizePx] array with redistribution applied; collapsed section keeps its old value
 *
 * REQ: PNL-02 / PNL-03 / PNL-PITFALL-05.
 */
internal fun shareTransferOnCollapse(
    sizePx: FloatArray,
    expanded: BooleanArray,
    index: Int,
): FloatArray {
    val newSizes = sizePx.copyOf()
    val releasedShare = sizePx[index]

    // Donors are all currently-expanded sections except the collapsing one
    val recipients = sizePx.indices.filter { it != index && expanded[it] }
    if (recipients.isEmpty()) return newSizes  // No expanded neighbors; nothing to transfer

    val recipientSum = recipients.sumOf { sizePx[it].toDouble() }.toFloat()

    var transferred = 0f
    recipients.forEachIndexed { listIdx, i ->
        val gain = if (listIdx == recipients.lastIndex) {
            releasedShare - transferred  // last absorbs rounding remainder (PNL-PITFALL-05)
        } else {
            if (recipientSum > 0f) (sizePx[i] / recipientSum) * releasedShare else releasedShare / recipients.size
        }
        newSizes[i] = sizePx[i] + gain
        transferred += gain
    }
    return newSizes
}

/**
 * Takes [restorePx] from other expanded sections proportionally (by current size weight),
 * clamped so no donor drops below zero, and assigns the restored share to [index].
 *
 * @param sizePx current size weights (will not be mutated)
 * @param expanded current expanded flags (caller has already set [index] to true before calling)
 * @param index index of the section being expanded/restored
 * @param restorePx amount to restore to [index] (e.g. `lastExpandedFraction * currentAvailableForExpanded`)
 * @return new [sizePx] array
 *
 * REQ: PNL-03 / PNL-PITFALL-06.
 */
internal fun shareTransferOnExpand(
    sizePx: FloatArray,
    expanded: BooleanArray,
    index: Int,
    restorePx: Float,
): FloatArray {
    val newSizes = sizePx.copyOf()
    // Donors are all currently-expanded sections except the one being restored
    val donors = sizePx.indices.filter { it != index && expanded[it] }
    if (donors.isEmpty()) {
        newSizes[index] = restorePx
        return newSizes
    }

    val donorSum = donors.sumOf { newSizes[it].toDouble() }.toFloat()
    // Cap restorePx to available donor budget (clamped so no donor goes below 0)
    val actualRestore = restorePx.coerceAtMost(donorSum)

    var taken = 0f
    donors.forEachIndexed { listIdx, i ->
        val give = if (listIdx == donors.lastIndex) {
            actualRestore - taken
        } else {
            if (donorSum > 0f) (sizePx[i] / donorSum) * actualRestore else actualRestore / donors.size
        }
        newSizes[i] = (sizePx[i] - give).coerceAtLeast(0f)
        taken += give
    }
    newSizes[index] = actualRestore
    return newSizes
}

/**
 * Captures the fraction of [availableForExpanded] that section [sizePx_i] occupies at
 * collapse time. Returns 0f if [availableForExpanded] is zero or negative.
 *
 * Store this fraction instead of an absolute px value to survive window shrinks
 * without overflow (PNL-PITFALL-06).
 *
 * REQ: PNL-03 / PNL-PITFALL-06.
 */
internal fun lastExpandedFraction(sizePx_i: Float, availableForExpanded: Float): Float =
    if (availableForExpanded <= 0f) 0f else sizePx_i / availableForExpanded

/**
 * Restores a section's sizePx from its captured fraction and the CURRENT available budget.
 *
 * Because fraction was computed at collapse time from the original [availableForExpanded],
 * and is <= 1f for a valid section, the result is always <= [currentAvailableForExpanded].
 * No additional clamp is needed for the common case (fraction <= 1f).
 *
 * REQ: PNL-03 / PNL-PITFALL-06.
 */
internal fun restoreFromFraction(fraction: Float, currentAvailableForExpanded: Float): Float =
    fraction * currentAvailableForExpanded
