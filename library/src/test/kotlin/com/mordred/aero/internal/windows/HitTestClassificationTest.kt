package com.mordred.aero.internal.windows

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SNAP-01 / SNAP-02 / BTN-02 / WIN-02 / WIN-06 / API-04: headless locks for [classifyHitTest],
 * the pure point-in-region classifier the Windows frame WndProc consults per WM_NCHITTEST.
 *
 * Every snapshot mirrors the live registry shape — a single Maximize role rect, an id-keyed
 * interactive map (minimize/close buttons, the `leading` slot, elements marked via API-02) and
 * an id-keyed captions map (draggable caption areas) — built from AeroTitleBar's real geometry
 * (32 dp row, 8 dp horizontal padding, 46 x 32 dp buttons) at the scale under test, so the
 * locked codes are the ones the live window answered in the VER-11 probes.
 */
class HitTestClassificationTest {

    /** AeroTitleBar's layout geometry (32 dp row, 8 dp padding, 46x32 dp buttons) at [scale]. */
    private class TitleBarGeometry(clientWidthPx: Int, scale: Float) {
        val rowPx = (32 * scale).toInt()
        private val padPx = (8 * scale).toInt()
        private val buttonW = (46 * scale).toInt()
        private val buttonsLeft = clientWidthPx - padPx - 3 * buttonW

        val snapshot = HitTestSnapshot(
            roles = mapOf(
                TitleBarRole.Maximize to PxRect(buttonsLeft + buttonW, 0, buttonsLeft + 2 * buttonW, rowPx),
            ),
            interactive = mapOf(
                101L to PxRect(padPx, 0, padPx + (32 * scale).toInt(), rowPx),                 // leading slot
                102L to PxRect(buttonsLeft, 0, buttonsLeft + buttonW, rowPx),                   // minimize
                103L to PxRect(buttonsLeft + 2 * buttonW, 0, buttonsLeft + 3 * buttonW, rowPx), // close
            ),
            captions = mapOf(
                1L to PxRect(padPx, 0, clientWidthPx - padPx, rowPx),
            ),
        )
    }

    /** The SHW-17 narrow window: ~300 px wide, leading slot + one API-02 marked element. */
    private fun narrowSnapshot(): HitTestSnapshot = HitTestSnapshot(
        roles = mapOf(TitleBarRole.Maximize to PxRect(200, 0, 246, 32)),
        interactive = mapOf(
            201L to PxRect(8, 0, 40, 32),     // leading slot
            202L to PxRect(120, 0, 160, 32),  // marked element (API-02)
            203L to PxRect(154, 0, 200, 32),  // minimize
            204L to PxRect(246, 0, 292, 32),  // close
        ),
        captions = mapOf(1L to PxRect(8, 0, 292, 32)),
    )

    /**
     * API-04 / D-05: a custom header that is NOT AeroTitleBar — a taller 48 dp row with two
     * caption areas split by a clickable search field and the maximize rect on the LEFT.
     */
    private fun customHeaderSnapshot(): HitTestSnapshot = HitTestSnapshot(
        roles = mapOf(TitleBarRole.Maximize to PxRect(0, 0, 40, 40)),
        interactive = mapOf(301L to PxRect(540, 0, 700, 48)), // clickable search field
        captions = mapOf(
            1L to PxRect(40, 0, 540, 48),
            2L to PxRect(700, 0, 1200, 48),
        ),
    )

    private fun code(
        snapshot: HitTestSnapshot,
        x: Int,
        y: Int,
        clientWidth: Int,
        clientHeight: Int,
        maximized: Boolean = false,
        resizeBandPx: Int = 8,
    ): Int = classifyHitTest(snapshot, x, y, clientWidth, clientHeight, maximized, resizeBandPx)

    /** SNAP-01: a point on the caption band answers HTCAPTION — Windows owns the drag. */
    @Test
    fun captionAreaAnswersHtcaption() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCAPTION, code(g.snapshot, 600, 16, 1200, 800))
        val g150 = TitleBarGeometry(1800, 1.5f)
        assertEquals(HTCAPTION, code(g150.snapshot, 900, 24, 1800, 1200, resizeBandPx = 12))
    }

    /** SNAP-02: the maximize rect answers HTMAXBUTTON — the OS offers Snap Layouts there. */
    @Test
    fun maximizeRectAnswersHtmaxbutton() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTMAXBUTTON, code(g.snapshot, 1123, 16, 1200, 800))
        val g150 = TitleBarGeometry(1800, 1.5f)
        assertEquals(HTMAXBUTTON, code(g150.snapshot, 1684, 24, 1800, 1200, resizeBandPx = 12))
    }

    /** BTN-02: minimize and close stay HTCLIENT — their ordinary Compose clicks survive. */
    @Test
    fun minimizeAndCloseStayHtclient() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCLIENT, code(g.snapshot, 1077, 16, 1200, 800))
        assertEquals(HTCLIENT, code(g.snapshot, 1169, 16, 1200, 800))
        val g150 = TitleBarGeometry(1800, 1.5f)
        assertEquals(HTCLIENT, code(g150.snapshot, 1615, 24, 1800, 1200, resizeBandPx = 12))
        assertEquals(HTCLIENT, code(g150.snapshot, 1753, 24, 1800, 1200, resizeBandPx = 12))
    }

    /** BTN-02 / API-02: an interactive rect inside the caption answers HTCLIENT, not HTCAPTION. */
    @Test
    fun leadingInteractiveInsideTheCaptionAnswersHtclient() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCLIENT, code(g.snapshot, 24, 16, 1200, 800))
    }

    /** SNAP-01: everywhere unmarked is plain client area. */
    @Test
    fun plainClientPointAnswersHtclient() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCLIENT, code(g.snapshot, 600, 400, 1200, 800))
        val g150 = TitleBarGeometry(1800, 1.5f)
        assertEquals(HTCLIENT, code(g150.snapshot, 900, 600, 1800, 1200, resizeBandPx = 12))
    }

    /** WIN-02: 2 px inside the left/right/bottom border answers the single-edge resize codes. */
    @Test
    fun edgesAnswerResizeCodesTwoPixelsFromTheBorder() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTLEFT, code(g.snapshot, 2, 400, 1200, 800))
        assertEquals(HTRIGHT, code(g.snapshot, 1198, 400, 1200, 800))
        assertEquals(HTBOTTOM, code(g.snapshot, 600, 798, 1200, 800))
    }

    /** WIN-02: 2 px inside each corner answers the two-edge corner codes. */
    @Test
    fun cornersAnswerCornerResizeCodes() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTTOPLEFT, code(g.snapshot, 2, 2, 1200, 800))
        assertEquals(HTTOPRIGHT, code(g.snapshot, 1198, 2, 1200, 800))
        assertEquals(HTBOTTOMLEFT, code(g.snapshot, 2, 798, 1200, 800))
        assertEquals(HTBOTTOMRIGHT, code(g.snapshot, 1198, 798, 1200, 800))
    }

    /**
     * WIN-02 / PITFALLS 19: a caption point inside the top resize band answers HTTOP, and the
     * same column just below the band answers HTCAPTION — band precedence over caption, at both
     * scales with their respective band thickness.
     */
    @Test
    fun topResizeBandBeatsTheCaption() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTTOP, code(g.snapshot, 600, 2, 1200, 800))
        assertEquals(HTCAPTION, code(g.snapshot, 600, 9, 1200, 800))
        val g150 = TitleBarGeometry(1800, 1.5f)
        assertEquals(HTTOP, code(g150.snapshot, 900, 4, 1800, 1200, resizeBandPx = 12))
        assertEquals(HTCAPTION, code(g150.snapshot, 900, 13, 1800, 1200, resizeBandPx = 12))
    }

    /** WIN-01: a maximized window has no resize codes — the top point falls through to caption. */
    @Test
    fun maximizedWindowHasNoResizeCodes() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCAPTION, code(g.snapshot, 600, 2, 1200, 800, maximized = true))
        assertEquals(HTCLIENT, code(g.snapshot, 2, 400, 1200, 800, maximized = true))
        assertEquals(HTCLIENT, code(g.snapshot, 2, 2, 1200, 800, maximized = true))
    }

    /** WIN-02: a zero band disables band classification entirely. */
    @Test
    fun zeroBandDisablesBandClassification() {
        val g = TitleBarGeometry(1200, 1.0f)
        assertEquals(HTCLIENT, code(g.snapshot, 2, 400, 1200, 800, resizeBandPx = 0))
        assertEquals(HTCLIENT, code(g.snapshot, 2, 2, 1200, 800, resizeBandPx = 0))
        assertEquals(HTCAPTION, code(g.snapshot, 600, 2, 1200, 800, resizeBandPx = 0))
    }

    /** SNAP-01: an EMPTY snapshot answers HTCLIENT everywhere except inside the bands. */
    @Test
    fun emptySnapshotAnswersHtclientExceptInsideBands() {
        assertEquals(HTLEFT, code(HitTestSnapshot.EMPTY, 2, 400, 1200, 800))
        assertEquals(HTTOP, code(HitTestSnapshot.EMPTY, 600, 2, 1200, 800))
        assertEquals(HTCLIENT, code(HitTestSnapshot.EMPTY, 600, 400, 1200, 800))
        assertEquals(HTCLIENT, code(HitTestSnapshot.EMPTY, 2, 400, 1200, 800, resizeBandPx = 0))
        assertEquals(HTCLIENT, code(HitTestSnapshot.EMPTY, 600, 2, 1200, 800, resizeBandPx = 0))
    }

    /**
     * WIN-06 / API-02: on the narrow window the leading slot and the marked element answer
     * HTCLIENT while the caption between them answers HTCAPTION, and the left edge still
     * answers HTLEFT — the narrow window drags AND resizes natively.
     */
    @Test
    fun narrowWindowInteractiveElementsStayHtclientWithCaptionBetween() {
        val snap = narrowSnapshot()
        assertEquals(HTCLIENT, code(snap, 24, 16, 300, 480), "leading slot")
        assertEquals(HTCLIENT, code(snap, 140, 16, 300, 480), "marked element (API-02)")
        assertEquals(HTCAPTION, code(snap, 100, 16, 300, 480), "caption left of the marked element")
        assertEquals(HTCLIENT, code(snap, 269, 16, 300, 480), "close button")
        assertEquals(HTMAXBUTTON, code(snap, 223, 16, 300, 480), "maximize button")
        assertEquals(HTLEFT, code(snap, 2, 240, 300, 480), "left edge band")
    }

    /**
     * API-04 / D-05: a custom non-AeroTitleBar header — maximize on the left, two caption areas
     * split by an interactive search field — answers each area's own code.
     */
    @Test
    fun customHeaderAreasAnswerTheirOwnCodes() {
        val snap = customHeaderSnapshot()
        assertEquals(HTMAXBUTTON, code(snap, 20, 24, 1200, 800, resizeBandPx = 0), "left-hand maximize box")
        assertEquals(HTCAPTION, code(snap, 290, 24, 1200, 800, resizeBandPx = 0), "caption left of the search field")
        assertEquals(HTCLIENT, code(snap, 620, 24, 1200, 800, resizeBandPx = 0), "clickable search field")
        assertEquals(HTCAPTION, code(snap, 950, 24, 1200, 800, resizeBandPx = 0), "caption right of the search field")
        assertEquals(HTTOP, code(snap, 600, 2, 1200, 800), "top band still wins on a custom header")
    }
}
