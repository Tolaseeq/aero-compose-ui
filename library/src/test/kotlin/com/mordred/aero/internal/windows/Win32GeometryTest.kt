package com.mordred.aero.internal.windows

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WIN-01 / WIN-04: headless locks for the pure maximized-client-rect math and frame-thickness
 * helpers — the geometry that keeps a maximized window inside its monitor's work area, leaves
 * auto-hide taskbar edges revealable, and sizes the native resize band at the window's DPI.
 */
class Win32GeometryTest {

    /** WIN-01 / PITFALLS 8: the sizing-border overhang is clamped to the visible taskbar work area. */
    @Test
    fun overhangingProposedRectClampsToTheVisibleTaskbarWorkArea() {
        val r = maximizedClientRect(
            proposed = PxRect(-8, -8, 1928, 1088),
            workArea = PxRect(0, 0, 1920, 1040),
            autoHideEdges = emptySet(),
        )
        assertEquals(PxRect(0, 0, 1920, 1040), r)
    }

    /**
     * WIN-01 / PITFALLS 9: with an auto-hide bottom taskbar `rcWork == rcMonitor`, so the work
     * area alone cannot reserve the edge — the explicit 2 px inset leaves it revealable.
     */
    @Test
    fun autoHideBottomInsetsTheBottomEdgeWhenWorkAreaEqualsTheMonitor() {
        val r = maximizedClientRect(
            proposed = PxRect(-8, -8, 1928, 1088),
            workArea = PxRect(0, 0, 1920, 1080),
            autoHideEdges = setOf(ScreenEdge.Bottom),
        )
        assertEquals(PxRect(0, 0, 1920, 1078), r)
    }

    /** WIN-01: a visible taskbar on any edge reserves it through its own work-area rect. */
    @Test
    fun visibleTaskbarReservesItsOwnEdge() {
        val proposed = PxRect(-8, -8, 1928, 1088)
        assertEquals(
            PxRect(48, 0, 1920, 1080),
            maximizedClientRect(proposed, PxRect(48, 0, 1920, 1080), emptySet()),
            "taskbar docked left",
        )
        assertEquals(
            PxRect(0, 48, 1920, 1080),
            maximizedClientRect(proposed, PxRect(0, 48, 1920, 1080), emptySet()),
            "taskbar docked top",
        )
        assertEquals(
            PxRect(0, 0, 1872, 1080),
            maximizedClientRect(proposed, PxRect(0, 0, 1872, 1080), emptySet()),
            "taskbar docked right",
        )
    }

    /** WIN-04: monitor arithmetic stays correct at a negative secondary-monitor origin. */
    @Test
    fun secondaryMonitorAtANegativeOrigin() {
        val monitor = PxRect(-1920, 0, 0, 1080)
        assertEquals(
            PxRect(-1920, 0, 0, 1080),
            maximizedClientRect(PxRect(-1928, -8, 8, 1088), monitor, emptySet()),
        )
        assertEquals(
            PxRect(-1920, 0, 0, 1078),
            maximizedClientRect(PxRect(-1928, -8, 8, 1088), monitor, setOf(ScreenEdge.Bottom)),
            "auto-hide bottom on the secondary monitor",
        )
    }

    /** WIN-01: the inset moves only the detected auto-hide edge, never the others. */
    @Test
    fun autoHideInsetsApplyOnlyOnDetectedEdges() {
        val monitor = PxRect(0, 0, 1920, 1080)
        val proposed = PxRect(-8, -8, 1928, 1088)
        assertEquals(2, maximizedClientRect(proposed, monitor, setOf(ScreenEdge.Left)).left)
        assertEquals(0, maximizedClientRect(proposed, monitor, setOf(ScreenEdge.Left)).top)
        assertEquals(2, maximizedClientRect(proposed, monitor, setOf(ScreenEdge.Top)).top)
        assertEquals(1918, maximizedClientRect(proposed, monitor, setOf(ScreenEdge.Right)).right)
    }

    /** WIN-01: the sizing frame is the sum of the SM_CXSIZEFRAME and SM_CXPADDEDBORDER metrics. */
    @Test
    fun resizeFramePxSumsTheSystemMetrics() {
        assertEquals(8, resizeFramePx(sizeFramePx = 4, paddedBorderPx = 4))
        assertEquals(12, resizeFramePx(sizeFramePx = 4, paddedBorderPx = 8))
    }

    /**
     * WIN-02 / T-22-25: the native band is the larger of the DPI-scaled system frame and CMP's
     * own 8 dp undecorated resizer — whichever grows first, the band fully shadows the
     * Compose-side resizer so no press can land on a second resize path.
     */
    @Test
    fun resizeBandPxShadowsBothTheSystemFrameAndCmpsResizer() {
        assertEquals(8, CMP_UNDECORATED_RESIZER_DP, "the mirrored CMP resizer thickness")
        assertEquals(8, resizeBandPx(4, 4, 1.0f), "100%: system 8 == CMP 8")
        assertEquals(12, resizeBandPx(6, 6, 1.0f), "100%: a thicker system frame wins")
        assertEquals(10, resizeBandPx(4, 4, 1.25f), "125%: CMP round(8*1.25)=10 wins over system 8")
        assertEquals(12, resizeBandPx(6, 6, 1.5f), "150%: system 12 == CMP 12")
        assertEquals(16, resizeBandPx(8, 8, 2.0f), "200%: system 16 == CMP 16")
        assertEquals(16, resizeBandPx(4, 4, 2.0f), "200%: CMP 16 wins over system 8")
    }
}
