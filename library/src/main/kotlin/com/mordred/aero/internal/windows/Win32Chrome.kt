package com.mordred.aero.internal.windows

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND

/**
 * SNAP-01: `undecorated = true` alone yields
 * `WS_POPUP | WS_SYSMENU | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_CLIPCHILDREN` — neither
 * `WS_CAPTION` nor `WS_THICKFRAME` is present, so DWM/Shell never treat the window as
 * resizable-with-a-frame. This adds both back (keeping every bit already present) and
 * invalidates Windows' cached frame geometry so the new style actually takes effect.
 */
internal object Win32Chrome {

    internal data class StyleResult(val before: Int, val after: Int)

    internal fun ensureNativeFrameStyles(hwnd: HWND): StyleResult {
        val before = readStyle(hwnd)
        val target = before or WS_CAPTION or WS_SYSMENU or WS_THICKFRAME or WS_MINIMIZEBOX or WS_MAXIMIZEBOX
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
