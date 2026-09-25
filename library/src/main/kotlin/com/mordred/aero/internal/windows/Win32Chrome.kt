package com.mordred.aero.internal.windows

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinDef.UINT

/**
 * WIN-03: the five style bits a native-chrome window on `AeroTitleBar` must always carry.
 * `undecorated = true` alone yields
 * `WS_POPUP | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_CLIPCHILDREN` — neither
 * `WS_CAPTION` nor `WS_THICKFRAME` is present, so DWM/Shell never treat the window as
 * resizable-with-a-frame.
 */
internal const val REQUIRED_FRAME_STYLES: Int =
    WS_CAPTION or WS_SYSMENU or WS_THICKFRAME or WS_MINIMIZEBOX or WS_MAXIMIZEBOX

/**
 * WIN-03: a pure OR — every bit already present in [style] is kept, nothing is ever cleared.
 * Used both at install time ([Win32Chrome.ensureNativeFrameStyles]) and from
 * `AeroFrameWndProc`'s `WM_STYLECHANGING` handler, so a later AWT-driven style rewrite cannot
 * drop these bits without them being added straight back before the new style is applied.
 */
internal fun withRequiredFrameStyles(style: Int): Int = style or REQUIRED_FRAME_STYLES

/**
 * SNAP-01: guard-rail (CMP-3757 / PITFALLS 17), restated for every file in this package —
 * never enable window transparency on the owning `Window`, and never add the layered
 * extended window style to this HWND. That combination crashes with
 * EXCEPTION_ACCESS_VIOLATION on Windows 11; see `AeroTitleBar.kt`'s own KDoc. The Aero glass
 * effect is provided by `Modifier.glassEffect`, never by window transparency.
 */
internal object Win32Chrome {

    internal data class StyleResult(val before: Int, val after: Int)

    internal fun ensureNativeFrameStyles(hwnd: HWND): StyleResult {
        val before = readStyle(hwnd)
        val target = withRequiredFrameStyles(before)
        if (target != before) {
            writeStyle(hwnd, target)
            User32.INSTANCE.SetWindowPos(
                hwnd,
                null,
                0,
                0,
                0,
                0,
                SWP_FRAMECHANGED or SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_NOACTIVATE or SWP_NOOWNERZORDER,
            )
        }
        val after = readStyle(hwnd)
        chromeTrace(
            "restyle",
            Pointer.nativeValue(hwnd.pointer),
            "before=0x${before.toString(16)} after=0x${after.toString(16)}",
        )
        return StyleResult(before, after)
    }

    private fun readStyle(hwnd: HWND): Int =
        User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_STYLE).toInt()

    private fun writeStyle(hwnd: HWND, style: Int) {
        User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_STYLE, Pointer(style.toLong()))
    }
}

private val AUTO_HIDE_EDGE_CODES: List<Pair<ScreenEdge, Int>> = listOf(
    ScreenEdge.Left to ShellAPI.ABE_LEFT,
    ScreenEdge.Top to ShellAPI.ABE_TOP,
    ScreenEdge.Right to ShellAPI.ABE_RIGHT,
    ScreenEdge.Bottom to ShellAPI.ABE_BOTTOM,
)

/**
 * WIN-01 / PITFALLS 9: per-edge auto-hide taskbar detection on [monitorRect] (the maximizing
 * window's own monitor — from `MonitorFromWindow` + `GetMonitorInfo`, never a hardcoded
 * primary monitor). `ABM_GETSTATE` short-circuits to no edges when the shell reports no
 * auto-hide bar exists anywhere; otherwise each `ABE_*` edge is queried individually via
 * `ABM_GETAUTOHIDEBAREX`, and a non-null bar HWND means that edge is docked on this monitor.
 */
internal fun autoHideEdges(monitorRect: PxRect): Set<ScreenEdge> {
    val stateQuery = newAppBarData()
    val state = Shell32.INSTANCE.SHAppBarMessage(DWORD(ShellAPI.ABM_GETSTATE.toLong()), stateQuery)
    if (state.toInt() and ABS_AUTOHIDE == 0) return emptySet()

    val edges = mutableSetOf<ScreenEdge>()
    for ((edge, edgeCode) in AUTO_HIDE_EDGE_CODES) {
        val query = newAppBarData()
        query.uEdge = UINT(edgeCode.toLong())
        query.rc = monitorRect.toWinRect()
        val bar = Shell32.INSTANCE.SHAppBarMessage(DWORD(ABM_GETAUTOHIDEBAREX.toLong()), query)
        if (bar.toLong() != 0L) edges += edge
    }
    return edges
}

private fun newAppBarData(): ShellAPI.APPBARDATA {
    val data = ShellAPI.APPBARDATA()
    data.cbSize = DWORD(data.size().toLong())
    return data
}

/** WIN-01: `MONITORINFO.rcWork` / `rcMonitor` (screen pixels) as a pure [PxRect]. */
internal fun RECT.toPxRect(): PxRect = PxRect(left, top, right, bottom)

private fun PxRect.toWinRect(): RECT {
    val rect = RECT()
    rect.left = left
    rect.top = top
    rect.right = right
    rect.bottom = bottom
    return rect
}
