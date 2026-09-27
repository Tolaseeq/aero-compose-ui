package com.mordred.aero.internal.windows

/**
 * SNAP-01 / SNAP-02 / BTN-02 / PITFALLS 6+19: pure point-in-region classification for
 * `WM_NCHITTEST`. No JNA, no Compose imports — the same discipline as `PanelDistribution.kt`
 * / `WndProcSupport.kt`, so it is unit-testable headlessly.
 */

/** SNAP-01: half-open containment, matching the Win32 rect convention (right/bottom exclusive). */
private fun PxRect.containsPoint(x: Int, y: Int): Boolean =
    x >= left && x < right && y >= top && y < bottom

/**
 * BTN-02 / SNAP-01 / WIN-02: classifies the client-space point ([x], [y]) against the
 * published [snapshot] and the resize bands. Precedence (PITFALLS 19, Anti-Pattern 3):
 *
 *  1. not maximized and [resizeBandPx] > 0: corner band (within the band of two adjacent
 *     edges) → HTTOPLEFT/HTTOPRIGHT/HTBOTTOMLEFT/HTBOTTOMRIGHT, then single-edge band →
 *     HTLEFT/HTRIGHT/HTTOP/HTBOTTOM — Windows owns resizing from every edge and corner
 *     (WIN-02); a maximized window has no resize bands
 *  2. maximize rect → [HTMAXBUTTON] (OS-owned, unlocks the Snap Layouts flyout, SNAP-04)
 *  3. minimize rect, close rect, any interactive rect (e.g. the `leading` slot) → [HTCLIENT],
 *     so Compose keeps receiving ordinary clicks there and never starts a window drag (BTN-02)
 *  4. caption rect, or any API-04 `captions` entry a custom title bar marked through
 *     `captionArea()` → [HTCAPTION] (SNAP-01: the native drag path)
 *  5. everywhere else → [HTCLIENT]
 *
 * [clientWidth] / [clientHeight] are the client-area size in physical px. [resizeBandPx] is
 * 0-safe: 0 disables resize-band classification entirely.
 */
internal fun classifyHitTest(
    snapshot: HitTestSnapshot,
    x: Int,
    y: Int,
    clientWidth: Int,
    clientHeight: Int,
    maximized: Boolean,
    resizeBandPx: Int,
): Int {
    if (!maximized && resizeBandPx > 0) {
        val nearLeft = x < resizeBandPx
        val nearRight = x >= clientWidth - resizeBandPx
        val nearTop = y < resizeBandPx
        val nearBottom = y >= clientHeight - resizeBandPx
        if (nearTop && nearLeft) return HTTOPLEFT
        if (nearTop && nearRight) return HTTOPRIGHT
        if (nearBottom && nearLeft) return HTBOTTOMLEFT
        if (nearBottom && nearRight) return HTBOTTOMRIGHT
        if (nearLeft) return HTLEFT
        if (nearRight) return HTRIGHT
        if (nearTop) return HTTOP
        if (nearBottom) return HTBOTTOM
    }
    snapshot.roles[TitleBarRole.Maximize]?.let { if (it.containsPoint(x, y)) return HTMAXBUTTON }
    snapshot.roles[TitleBarRole.Minimize]?.let { if (it.containsPoint(x, y)) return HTCLIENT }
    snapshot.roles[TitleBarRole.Close]?.let { if (it.containsPoint(x, y)) return HTCLIENT }
    for (rect in snapshot.interactive.values) {
        if (rect.containsPoint(x, y)) return HTCLIENT
    }
    snapshot.roles[TitleBarRole.Caption]?.let { if (it.containsPoint(x, y)) return HTCAPTION }
    for (rect in snapshot.captions.values) {
        if (rect.containsPoint(x, y)) return HTCAPTION
    }
    return HTCLIENT
}
