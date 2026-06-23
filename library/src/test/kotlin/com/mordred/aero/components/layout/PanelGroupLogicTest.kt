package com.mordred.aero.components.layout

import com.mordred.aero.components.layout.internal.panelgroup.activeDividerCount
import com.mordred.aero.components.layout.internal.panelgroup.clampPanelDividerPx
import com.mordred.aero.components.layout.internal.panelgroup.computeAvailablePx
import com.mordred.aero.components.layout.internal.panelgroup.distributePx
import com.mordred.aero.components.layout.internal.panelgroup.lastExpandedFraction
import com.mordred.aero.components.layout.internal.panelgroup.restoreFromFraction
import com.mordred.aero.components.layout.internal.panelgroup.shareTransferOnCollapse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelGroupLogicTest {

    // ──────────────────────────────────────────────────────────────────────
    // Named tests wired to 13-VALIDATION.md (must match exactly)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * PNL-04: 3 equal expanded sections; distribute at totalPx=900 then 600;
     * each section's share ratio is preserved (~1/3 of availableForExpanded) within 0.5f.
     */
    @Test
    fun distributePxWindowResizePreservesRatios() {
        val headerPx = 36f
        val dividerPx = 8f
        // All 3 expanded → activeDividers = 2
        val expanded = booleanArrayOf(true, true, true)
        // Equal initial sizes
        val sizePx = floatArrayOf(300f, 300f, 300f)

        val available900 = computeAvailablePx(900f, expanded, headerPx, dividerPx)
        val heights900 = distributePx(sizePx, expanded, 900f, headerPx, dividerPx)

        val available600 = computeAvailablePx(600f, expanded, headerPx, dividerPx)
        val heights600 = distributePx(sizePx, expanded, 600f, headerPx, dividerPx)

        // Each section should be ~1/3 of available at each total
        val expected900 = available900 / 3f
        val expected600 = available600 / 3f

        assertEquals(expected900, heights900[0], 0.5f)
        assertEquals(expected900, heights900[1], 0.5f)
        assertEquals(expected900, heights900[2], 0.5f)

        assertEquals(expected600, heights600[0], 0.5f)
        assertEquals(expected600, heights600[1], 0.5f)
        assertEquals(expected600, heights600[2], 0.5f)
    }

    /**
     * PNL-02/03: Middle section of 3 collapses; Σ of new expanded sizePx equals
     * sum of the two survivors' original sizePx + collapsed section's released share (conservation).
     * Last neighbor absorbs rounding remainder (PNL-PITFALL-05).
     */
    @Test
    fun shareTransferOnCollapseConservesPx() {
        // 3 sections all expanded; middle (index=1) collapses
        val sizePx = floatArrayOf(200f, 150f, 250f)
        val expanded = booleanArrayOf(true, true, true)
        val collapsingIndex = 1

        val originalExpandedSum = sizePx[0] + sizePx[2]  // survivors' original share
        val releasedShare = sizePx[collapsingIndex]

        val newSizes = shareTransferOnCollapse(sizePx, expanded, collapsingIndex)

        // After collapse: the two expanded survivors (index 0 and 2) must together hold
        // originalExpandedSum + releasedShare
        val newExpandedSum = newSizes[0] + newSizes[2]
        assertEquals(originalExpandedSum + releasedShare, newExpandedSum, 0.5f)
    }

    /**
     * PNL-10 / PNL-PITFALL-04: Three expanded sections each minSizePx=60, totalBudget=100
     * (below Σmin). Call must NOT throw IllegalArgumentException; returns a finite value.
     */
    @Test
    fun clampPanelDividerPxInvertedRangeNoThrow() {
        val minAbovePx = 60f
        val minBelowPx = 60f
        val totalBudgetPx = 100f  // 100 < 60 + 60 = 120 → inverted range

        val result = clampPanelDividerPx(
            aboveSizePx = 50f,
            deltaPx = 10f,
            minAbovePx = minAbovePx,
            minBelowPx = minBelowPx,
            totalBudgetPx = totalBudgetPx,
        )
        assertTrue(result.isFinite(), "result must be finite, was $result")
    }

    /**
     * PNL-06 / PNL-PITFALL-07: expanded=[true,true,false,true] (E/E/C/E) →
     * activeDividerCount == 1 (zipWithNext gives only pair 0-1), NOT 2.
     */
    @Test
    fun activeDividerCountEECEGivesOne() {
        val expanded = booleanArrayOf(true, true, false, true)
        assertEquals(1, activeDividerCount(expanded))
    }

    /**
     * PNL-03 / PNL-PITFALL-06: Collapse captures lastExpandedFraction;
     * window shrinks; expand restores fraction * currentAvailableForExpanded.
     * Assert restored px <= currentAvailableForExpanded (no overflow).
     */
    @Test
    fun restoreAfterShrinkDoesNotExceedAvailable() {
        val headerPx = 36f
        val dividerPx = 8f

        // Section 0 of 2 expanded; compute available at original window size
        val expanded = booleanArrayOf(true, true)
        val originalAvailable = computeAvailablePx(600f, expanded, headerPx, dividerPx)

        // Section 0 has sizePx=200 → fraction = 200 / originalAvailable
        val fraction = lastExpandedFraction(200f, originalAvailable)

        // Window shrinks to 400
        val currentAvailable = computeAvailablePx(400f, expanded, headerPx, dividerPx)

        // Restore as fraction of CURRENT available
        val restoredPx = restoreFromFraction(fraction, currentAvailable)

        // Must not exceed currentAvailable (fraction <= 1 guard)
        assertTrue(
            restoredPx <= currentAvailable + 0.5f,
            "restoredPx ($restoredPx) must not exceed currentAvailable ($currentAvailable)"
        )
    }

    /**
     * PNL-15: All sections collapsed → availableForExpanded == 0 (or <= 0)
     * and every expanded-section share is 0.
     */
    @Test
    fun allCollapsedAvailableIsZero() {
        val headerPx = 36f
        val dividerPx = 8f
        val expanded = booleanArrayOf(false, false, false)
        val sizePx = floatArrayOf(200f, 200f, 200f)

        val available = computeAvailablePx(900f, expanded, headerPx, dividerPx)
        assertTrue(available <= 0f, "available should be <= 0 when all collapsed, was $available")

        val heights = distributePx(sizePx, expanded, 900f, headerPx, dividerPx)
        // All sections collapsed → all render as header strip
        for (h in heights) {
            assertEquals(headerPx, h, 0.5f)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Supporting tests
    // ──────────────────────────────────────────────────────────────────────

    /** First section collapses → right neighbor gains all. */
    @Test
    fun firstCollapseRightNeighborGainsAll() {
        val sizePx = floatArrayOf(200f, 300f)
        val expanded = booleanArrayOf(true, true)
        val totalReleased = sizePx[0]
        val originalRight = sizePx[1]

        val newSizes = shareTransferOnCollapse(sizePx, expanded, 0)

        assertEquals(originalRight + totalReleased, newSizes[1], 0.5f)
    }

    /** Last section collapses → left neighbor gains all. */
    @Test
    fun lastCollapseLeftNeighborGainsAll() {
        val sizePx = floatArrayOf(300f, 200f)
        val expanded = booleanArrayOf(true, true)
        val totalReleased = sizePx[1]
        val originalLeft = sizePx[0]

        val newSizes = shareTransferOnCollapse(sizePx, expanded, 1)

        assertEquals(originalLeft + totalReleased, newSizes[0], 0.5f)
    }

    /** Single expanded section fills availableForExpanded. */
    @Test
    fun singleExpandedFillsAvailable() {
        val headerPx = 36f
        val dividerPx = 8f
        // Section 0 expanded, sections 1 and 2 collapsed
        val expanded = booleanArrayOf(true, false, false)
        val sizePx = floatArrayOf(200f, 100f, 100f)

        val available = computeAvailablePx(900f, expanded, headerPx, dividerPx)
        val heights = distributePx(sizePx, expanded, 900f, headerPx, dividerPx)

        assertEquals(available, heights[0], 0.5f)
    }

    /** computeAvailablePx: all expanded, 2 active dividers, correct result. */
    @Test
    fun computeAvailablePxAllExpanded() {
        // 3 expanded → activeDividers = zipWithNext count of (true,true) pairs = 2
        val expanded = booleanArrayOf(true, true, true)
        val available = computeAvailablePx(
            totalPx = 900f,
            expanded = expanded,
            headerPx = 36f,
            dividerPx = 8f,
        )
        // 900 - 3*36 - 2*8 = 900 - 108 - 16 = 776
        assertEquals(776f, available, 0.5f)
    }

    /** lastExpandedFraction: zero available guard returns 0f. */
    @Test
    fun lastExpandedFractionZeroAvailableReturnsZero() {
        assertEquals(0f, lastExpandedFraction(100f, 0f), 0.01f)
    }

    /** clampPanelDividerPx: normal in-range drag is applied exactly. */
    @Test
    fun clampPanelDividerPxFreeMoveInRange() {
        val result = clampPanelDividerPx(
            aboveSizePx = 200f,
            deltaPx = 10f,
            minAbovePx = 60f,
            minBelowPx = 60f,
            totalBudgetPx = 400f,
        )
        // maxAbovePx = 400 - 60 = 340; 200 + 10 = 210 in [60, 340]
        assertEquals(210f, result, 0.01f)
    }
}
