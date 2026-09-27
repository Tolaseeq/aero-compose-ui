package com.mordred.aero.internal.windows

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser

/**
 * SNAP-01 / SNAP-02: the frame's `WindowProc`. Owns exactly WM_NCCALCSIZE, WM_NCHITTEST and
 * WM_NCDESTROY; every other message is `CallWindowProc(previous, ...)` verbatim — AWT's own
 * WndProc still needs to run for focus, IME, painting and `WindowState` sync.
 */
internal class AeroFrameWndProc(
    val hwnd: Long,
    val previous: Pointer,
    val regions: HitTestRegionRegistry,
) : WinUser.WindowProc {

    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        LRESULT(
            dispatchSafely(
                handle = { handle(hwnd, uMsg, wParam, lParam) },
                fallback = { callPrevious(hwnd, uMsg, wParam, lParam).toLong() },
            ),
        )

    private fun handle(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long = when (uMsg) {
        WM_NCCALCSIZE -> handleNcCalcSize(hwnd, wParam, lParam)

        WM_NCHITTEST -> handleNcHitTest(hwnd, wParam, lParam)

        WM_STYLECHANGING -> handleStyleChanging(hwnd, uMsg, wParam, lParam)

        WM_SIZE -> handleSize(hwnd, uMsg, wParam, lParam)

        WM_NCDESTROY -> {
            val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
            NativeWindowChromeRegistry.onDestroyed(this.hwnd)
            result
        }

        else -> callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    /**
     * WIN-03 / PITFALLS 24: corner rounding follows maximize/restore. The message is
     * forwarded to `CallWindowProc(previous)` FIRST — AWT owns the `WindowState` sync
     * pipeline (C4) and nothing here may replace it — then the corner policy is re-applied
     * for the new state (square while maximized, the maintainer's chosen look when floating).
     */
    private fun handleSize(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
        when (wParam.toInt()) {
            SIZE_MAXIMIZED -> applyCornerPolicy(hwnd, maximized = true)
            SIZE_RESTORED -> applyCornerPolicy(hwnd, maximized = false)
        }
        return result
    }

    /**
     * WIN-03: keeps [REQUIRED_FRAME_STYLES] set even if AWT rewrites `GWL_STYLE` later
     * (activation changes, DnD registration, always-on-top toggles — Pitfall 4). `STYLESTRUCT`
     * is `{ DWORD styleOld; DWORD styleNew }`; writing `styleNew` in place before forwarding
     * changes the style Windows actually applies. Only `GWL_STYLE` is touched —
     * `GWL_EXSTYLE` changes pass through unread.
     */
    private fun handleStyleChanging(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() == GWL_STYLE) {
            val styleStruct = Pointer(lParam.toLong())
            val styleNew = styleStruct.getInt(4)
            val kept = withRequiredFrameStyles(styleNew)
            if (kept != styleNew) {
                styleStruct.setInt(4, kept)
                chromeTrace(
                    "style-kept",
                    this.hwnd,
                    "styleNew=0x${styleNew.toString(16)} kept=0x${kept.toString(16)}",
                )
            }
        }
        return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    /**
     * SNAP-01 / BTN-02 / PITFALLS 6: converts the screen point to client coordinates INSIDE
     * the handler (window position never enters the math), then classifies it against the
     * live region snapshot `AeroTitleBar` publishes — a plain volatile read of an immutable
     * [HitTestSnapshot], never Compose state from this native thread.
     */
    private fun handleNcHitTest(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        val (screenX, screenY) = decodeScreenPoint(lParam.toLong())
        val point = POINT(screenX, screenY)
        aeroUser32.ScreenToClient(hwnd, point)
        val clientRect = RECT()
        User32.INSTANCE.GetClientRect(hwnd, clientRect)
        val code = classifyHitTest(
            snapshot = regions.snapshot(),
            x = point.x,
            y = point.y,
            clientWidth = clientRect.right - clientRect.left,
            clientHeight = clientRect.bottom - clientRect.top,
            maximized = aeroUser32.IsZoomed(hwnd),
            resizeBandPx = 0,
        )
        // HTCLIENT: let AWT's own answer stand (keeps its default outside published regions).
        return if (code == HTCLIENT) callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong() else code.toLong()
    }

    /**
     * WIN-01 / PITFALLS 8+9: `wParam == 0` is forwarded untouched (AWT owns that shape).
     * `wParam != 0` (proposed-rect form): floating state returns 0 with `rgrc[0]` unchanged
     * (client = the full proposed window rect, hiding the native caption paint); maximized
     * state rewrites `rgrc[0]` in place to [maximizedClientRect] — the intersection with the
     * window's own monitor work area, minus a taskbar-aware auto-hide inset — before also
     * returning 0. Never touches `WM_GETMINMAXINFO` or `WM_DPICHANGED` (out of this handler's
     * scope, forward-only per PITFALLS 14).
     */
    private fun handleNcCalcSize(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        if (wParam.toInt() == 0) return callPrevious(hwnd, WM_NCCALCSIZE, wParam, lParam).toLong()

        if (aeroUser32.IsZoomed(hwnd)) {
            val params = Pointer(lParam.toLong())
            val proposed = PxRect(params.getInt(0), params.getInt(4), params.getInt(8), params.getInt(12))

            val monitor = User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
            val monitorInfo = WinUser.MONITORINFO()
            User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo)
            val workArea = monitorInfo.rcWork.toPxRect()
            val edges = autoHideEdges(monitorInfo.rcMonitor.toPxRect())

            val client = maximizedClientRect(proposed, workArea, edges)
            params.setInt(0, client.left)
            params.setInt(4, client.top)
            params.setInt(8, client.right)
            params.setInt(12, client.bottom)
            chromeTrace("nccalc-max", this.hwnd, "rect=$client edges=$edges")
        }
        return 0L
    }

    private fun callPrevious(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        User32.INSTANCE.CallWindowProc(previous, hwnd, uMsg, wParam, lParam)
}

/**
 * SNAP-01: the child `WindowProc` — Skiko's `HardwareLayer` (`SunAwtCanvas`) covers the whole
 * client area and answers WM_NCHITTEST before the frame ever sees it, so the frame's
 * classification must be reached through this proc, not by subclassing the frame alone. Owns
 * exactly WM_NCHITTEST: bounces HTTRANSPARENT back up to the frame wherever the frame would
 * answer non-client, `CallWindowProc(previous)` for its own HTCLIENT answer and for every
 * other message.
 */
internal class AeroChildWndProc(
    val frameHwnd: Long,
    val childHwnd: Long,
    val previous: Pointer,
    val regions: HitTestRegionRegistry,
) : WinUser.WindowProc {

    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        LRESULT(
            dispatchSafely(
                handle = { handle(hwnd, uMsg, wParam, lParam) },
                fallback = { callPrevious(hwnd, uMsg, wParam, lParam).toLong() },
            ),
        )

    private fun handle(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long = when (uMsg) {
        WM_NCHITTEST -> handleNcHitTest(hwnd, wParam, lParam)
        else -> callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    private fun handleNcHitTest(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        val (screenX, screenY) = decodeScreenPoint(lParam.toLong())
        val frame = HWND(Pointer(frameHwnd))
        val point = POINT(screenX, screenY)
        aeroUser32.ScreenToClient(frame, point)
        val clientRect = RECT()
        User32.INSTANCE.GetClientRect(frame, clientRect)
        val code = classifyHitTest(
            snapshot = regions.snapshot(),
            x = point.x,
            y = point.y,
            clientWidth = clientRect.right - clientRect.left,
            clientHeight = clientRect.bottom - clientRect.top,
            maximized = aeroUser32.IsZoomed(frame),
            resizeBandPx = 0,
        )
        return if (code == HTCLIENT) {
            callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong()
        } else {
            HTTRANSPARENT.toLong()
        }
    }

    private fun callPrevious(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        User32.INSTANCE.CallWindowProc(previous, hwnd, uMsg, wParam, lParam)
}
