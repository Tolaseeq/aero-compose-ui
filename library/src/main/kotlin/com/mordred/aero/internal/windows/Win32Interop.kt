package com.mordred.aero.internal.windows

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

// SNAP-01 / SNAP-02 / WIN-03: WM_* message identifiers this WindowProc implementations own.
internal const val WM_STYLECHANGING = 0x007C
internal const val WM_NCDESTROY = 0x0082
internal const val WM_NCCALCSIZE = 0x0083
internal const val WM_NCHITTEST = 0x0084
internal const val WM_PARENTNOTIFY = 0x0210

// WM_NCHITTEST return codes.
internal const val HTTRANSPARENT = -1
internal const val HTCLIENT = 1
internal const val HTCAPTION = 2
internal const val HTMAXBUTTON = 9

// GWL_STYLE / GWLP_WNDPROC indices — SetWindowLongPtr/GetWindowLongPtr only (T-22-04): the
// legacy 32-bit SetWindowLong/GetWindowLong truncate a 64-bit WNDPROC pointer on x64 Windows.
internal const val GWL_STYLE = -16
internal const val GWLP_WNDPROC = -4

// Window style bits Win32Chrome.ensureNativeFrameStyles adds back: undecorated=true alone
// yields WS_POPUP with neither bit set, so DWM/Shell never see the window as
// resizable-with-a-frame.
internal const val WS_MAXIMIZEBOX = 0x00010000
internal const val WS_MINIMIZEBOX = 0x00020000
internal const val WS_THICKFRAME = 0x00040000
internal const val WS_SYSMENU = 0x00080000
internal const val WS_CAPTION = 0x00C00000

// SetWindowPos flags used once after a GWL_STYLE change, to invalidate Windows' cached frame
// geometry so the new style bits actually take effect.
internal const val SWP_NOSIZE = 0x0001
internal const val SWP_NOMOVE = 0x0002
internal const val SWP_NOZORDER = 0x0004
internal const val SWP_NOACTIVATE = 0x0010
internal const val SWP_FRAMECHANGED = 0x0020
internal const val SWP_NOOWNERZORDER = 0x0200

// WIN-01: SHAppBarMessage flag/message constants jna-platform's ShellAPI does not declare
// (ABM_GETSTATE and the four ABE_* edges are already present there and are used from that
// interface directly).
internal const val ABM_GETAUTOHIDEBAREX = 0xB
internal const val ABS_AUTOHIDE = 0x0001

/**
 * SNAP-01: guards every native call site. Touches no JNA class, so evaluating it never
 * triggers a native load on a non-Windows platform.
 */
internal val isWindowsOs: Boolean =
    System.getProperty("os.name").orEmpty().startsWith("Windows")

/**
 * DEP-01: hand-declared extension interface for the user32.dll entry points jna-platform
 * 5.19.1 does not ship. Everything already present on jna-platform's own
 * `com.sun.jna.platform.win32.User32` (GetWindowLongPtr, SetWindowLongPtr, CallWindowProc,
 * SetWindowPos, GetClientRect, EnumChildWindows, ...) is used from that interface's own
 * `INSTANCE` instead of being re-declared here.
 */
internal interface AeroUser32 : StdCallLibrary {
    public fun IsZoomed(hWnd: HWND): Boolean
    public fun ScreenToClient(hWnd: HWND, lpPoint: POINT): Boolean
    public fun ClientToScreen(hWnd: HWND, lpPoint: POINT): Boolean
    public fun GetDpiForWindow(hWnd: HWND): Int
    public fun GetSystemMetricsForDpi(nIndex: Int, dpi: Int): Int
}

/**
 * Loaded lazily so nothing native happens on a non-Windows JVM, or before the first
 * Windows-only call site actually needs it (SNAP-01).
 */
internal val aeroUser32: AeroUser32 by lazy {
    Native.load("user32", AeroUser32::class.java, W32APIOptions.DEFAULT_OPTIONS)
}

/**
 * VER-11 diagnostic hook: prints `AERO_CHROME event=<event> hwnd=0x<hex> <detail>` and
 * flushes, only when `-Daero.chromeTrace=true` is set — mirrors `WindowStateReporter`'s
 * `-Daero.windowState` gate, so an ordinary launch is unchanged.
 */
internal fun chromeTrace(event: String, hwnd: Long, detail: String) {
    if (System.getProperty("aero.chromeTrace") != "true") return
    println("AERO_CHROME event=$event hwnd=0x${hwnd.toString(16)} $detail")
    System.out.flush()
}
