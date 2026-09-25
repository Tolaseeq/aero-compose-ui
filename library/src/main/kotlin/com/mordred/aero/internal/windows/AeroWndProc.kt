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
) : WinUser.WindowProc {

    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        LRESULT(
            dispatchSafely(
                handle = { handle(hwnd, uMsg, wParam, lParam) },
                fallback = { callPrevious(hwnd, uMsg, wParam, lParam).toLong() },
            ),
        )

    private fun handle(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long = when (uMsg) {
        WM_NCCALCSIZE ->
            // Floating state only: a maximized window's overhang fix is a separate, later
            // concern (returning 0 unconditionally would push maximized content off-screen).
            if (wParam.toInt() != 0) 0L else callPrevious(hwnd, uMsg, wParam, lParam).toLong()

        WM_NCHITTEST -> handleNcHitTest(hwnd, wParam, lParam)

        WM_NCDESTROY -> {
            val result = callPrevious(hwnd, uMsg, wParam, lParam).toLong()
            NativeWindowChromeRegistry.onDestroyed(this.hwnd)
            result
        }

        else -> callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }

    private fun handleNcHitTest(hwnd: HWND, wParam: WPARAM, lParam: LPARAM): Long {
        val (screenX, screenY) = decodeScreenPoint(lParam.toLong())
        val point = POINT(screenX, screenY)
        aeroUser32.ScreenToClient(hwnd, point)
        val clientRect = RECT()
        User32.INSTANCE.GetClientRect(hwnd, clientRect)
        val clientWidthPx = clientRect.right - clientRect.left
        val scale = aeroUser32.GetDpiForWindow(hwnd) / 96f
        val code = classifySpikeTitleRow(point.x, point.y, clientWidthPx, scale)
        // HTCLIENT: let AWT's own answer stand (keeps its default outside the title row).
        return if (code == HTCLIENT) callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong() else code.toLong()
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
        val clientWidthPx = clientRect.right - clientRect.left
        val scale = aeroUser32.GetDpiForWindow(frame) / 96f
        val code = classifySpikeTitleRow(point.x, point.y, clientWidthPx, scale)
        return if (code == HTCLIENT) {
            callPrevious(hwnd, WM_NCHITTEST, wParam, lParam).toLong()
        } else {
            HTTRANSPARENT.toLong()
        }
    }

    private fun callPrevious(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
        User32.INSTANCE.CallWindowProc(previous, hwnd, uMsg, wParam, lParam)
}
