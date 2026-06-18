package com.mordred.aero.components.layout

import com.mordred.aero.components.layout.internal.splitpane.clampDividerPx
import com.mordred.aero.components.layout.internal.splitpane.fractionToPx
import com.mordred.aero.components.layout.internal.splitpane.pxToFraction
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitClampTest {

    // clampDividerPx tests

    @Test
    fun clampAtMinLeftEdge() {
        // 50 + (-100) = -50, clamped to minFirstPx=48
        assertEquals(48f, clampDividerPx(50f, -100f, 48f, 500f), 0.01f)
    }

    @Test
    fun clampAtMaxRightEdge() {
        // 490 + 100 = 590, clamped to maxPx=500
        assertEquals(500f, clampDividerPx(490f, 100f, 48f, 500f), 0.01f)
    }

    @Test
    fun freeMoveInRange() {
        // 250 + 10 = 260, within [48, 500]
        assertEquals(260f, clampDividerPx(250f, 10f, 48f, 500f), 0.01f)
    }

    // fractionToPx tests

    @Test
    fun fractionToPxHalf() {
        assertEquals(200f, fractionToPx(0.5f, 400f), 0.01f)
    }

    // pxToFraction tests

    @Test
    fun pxToFractionHalf() {
        assertEquals(0.5f, pxToFraction(200f, 400f), 0.01f)
    }

    @Test
    fun pxToFractionZeroTotalGuard() {
        // Guard against divide-by-zero when totalPx == 0
        assertEquals(0f, pxToFraction(150f, 0f), 0.01f)
    }
}
