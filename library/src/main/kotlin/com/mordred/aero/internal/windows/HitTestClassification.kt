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
 * BTN-02 / SNAP-01: classifies the client-space point ([x], [y]) against the published
 * [snapshot]. Precedence (PITFALLS 19, Anti-Pattern 3):
 *
 *  1. maximize rect → [HTMAXBUTTON] (OS-owned, unlocks the Snap Layouts flyout, SNAP-04)
 *  2. minimize rect, close rect, any interactive rect (e.g. the `leading` slot) → [HTCLIENT],
 *     so Compose keeps receiving ordinary clicks there and never starts a window drag (BTN-02)
 *  3. caption rect → [HTCAPTION] (SNAP-01: the native drag path)
 *  4. everywhere else → [HTCLIENT]
 *
 * [clientWidth] / [clientHeight] are the client-area size in physical px. [resizeBandPx] is
 * accepted now so the WndProc call sites do not change shape later, and must be 0-safe: 0 —
 * the only value passed until Plan 11 — disables resize-band classification entirely
 * (edge/corner bands HTLEFT…HTBOTTOMRIGHT are Plan 11's scope, as is honoring [maximized],
 * which suppresses bands because a maximized window has no resize edges).
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
    snapshot.roles[TitleBarRole.Maximize]?.let { if (it.containsPoint(x, y)) return HTMAXBUTTON }
    snapshot.roles[TitleBarRole.Minimize]?.let { if (it.containsPoint(x, y)) return HTCLIENT }
    snapshot.roles[TitleBarRole.Close]?.let { if (it.containsPoint(x, y)) return HTCLIENT }
    for (rect in snapshot.interactive.values) {
        if (rect.containsPoint(x, y)) return HTCLIENT
    }
    snapshot.roles[TitleBarRole.Caption]?.let { if (it.containsPoint(x, y)) return HTCAPTION }
    return HTCLIENT
}
