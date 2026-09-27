package com.mordred.aero.internal.windows

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

// WIN-03: DWM window-attribute identifiers and corner-preference values (dwmapi.dll,
// hand-declared — jna-platform 5.19.1's Dwmapi does not ship them).
internal const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
internal const val DWMWCP_DEFAULT = 0
internal const val DWMWCP_DONOTROUND = 1
internal const val DWMWCP_ROUND = 2
internal const val DWMWCP_ROUNDSMALL = 3

/**
 * WIN-03 / DEP-01: hand-declared extension interface for the dwmapi.dll entry points
 * jna-platform does not ship, mirroring [AeroUser32]. Loaded lazily so nothing native
 * happens on a non-Windows JVM or before the first corner-policy application.
 */
internal interface Dwmapi : StdCallLibrary {
    public fun DwmSetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: Pointer, cbAttribute: Int): Int
    public fun DwmGetWindowAttribute(hwnd: HWND, dwAttribute: Int, pvAttribute: Pointer, cbAttribute: Int): Int
    public fun DwmExtendFrameIntoClientArea(hwnd: HWND, pMarInset: MARGINS): Int
}

internal val dwmapi: Dwmapi by lazy {
    Native.load("dwmapi", Dwmapi::class.java, W32APIOptions.DEFAULT_OPTIONS)
}

/** WIN-03: `MARGINS` struct for `DwmExtendFrameIntoClientArea` (four ints, by reference). */
internal class MARGINS : Structure() {
    @JvmField var cxLeftWidth: Int = 0
    @JvmField var cxRightWidth: Int = 0
    @JvmField var cyTopHeight: Int = 0
    @JvmField var cyBottomHeight: Int = 0

    override fun getFieldOrder(): List<String> =
        listOf("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight")
}

/**
 * WIN-03: the corner/shadow looks offered to the maintainer in 22-06's preview. A 1px
 * frame-extension margin on all four sides is the documented way to get the DWM drop shadow
 * on an otherwise borderless window (melak47/BorderlessWindow); 0 margins = no extension.
 */
internal enum class WindowCornerLook(val cornerPref: Int, val shadowMarginPx: Int) {
    /** Variant A: square corners, no system shadow — the pre-phase look. */
    SQUARE_NO_SHADOW(DWMWCP_DONOTROUND, 0),

    /** Variant B: rounded corners plus the soft Windows 11 system shadow. */
    ROUND_WITH_SHADOW(DWMWCP_ROUND, 1),

    /** Variant C: rounded corners without a shadow. */
    ROUND_NO_SHADOW(DWMWCP_ROUND, 0),
}

/**
 * WIN-03: the look the maintainer picked from the 22-06 Visual Companion preview — variant A
 * (square corners, no system shadow), recorded verbatim in 22-NOTES.md § "Corners and shadow
 * (22-06)". Applied explicitly so the result is deterministic across Windows builds instead
 * of depending on the OS default (conflict #6: the default on this machine was observed to
 * leave the corners square, but PrintWindow cannot observe compositor effects, so nothing
 * is left to a default).
 */
internal val CHOSEN_CORNER_LOOK: WindowCornerLook = WindowCornerLook.SQUARE_NO_SHADOW

/**
 * WIN-03 / PITFALLS 24: applies the corner/shadow policy to [hwnd]. Corner rounding follows
 * maximize/restore — [DWMWCP_DONOTROUND] while maximized, [CHOSEN_CORNER_LOOK]'s preference
 * otherwise — and the frame extension uses the chosen shadow margin on all four sides. Every
 * HRESULT is traced (`event=dwm`) and the corner preference is read back through
 * `DwmGetWindowAttribute` so the applied value is verified, not assumed. A failure is traced
 * and swallowed — it must never throw out of a WndProc (T-22-03); [dispatchSafely] is the
 * second layer for the WndProc call sites, this catch covers the registry install call.
 */
internal fun applyCornerPolicy(hwnd: HWND, maximized: Boolean) {
    val hwndLong = Pointer.nativeValue(hwnd.pointer)
    try {
        val cornerPref = if (maximized) DWMWCP_DONOTROUND else CHOSEN_CORNER_LOOK.cornerPref
        val prefMem = Memory(4)
        prefMem.setInt(0, cornerPref)
        val setHr = dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, prefMem, 4)

        val marginPx = CHOSEN_CORNER_LOOK.shadowMarginPx
        val margins = MARGINS()
        margins.cxLeftWidth = marginPx
        margins.cxRightWidth = marginPx
        margins.cyTopHeight = marginPx
        margins.cyBottomHeight = marginPx
        margins.write()
        val marginsHr = dwmapi.DwmExtendFrameIntoClientArea(hwnd, margins)

        val readBackMem = Memory(4)
        val getHr = dwmapi.DwmGetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, readBackMem, 4)
        val readBack = if (getHr == 0) readBackMem.getInt(0).toString() else "n/a"

        chromeTrace(
            "dwm",
            hwndLong,
            "maximized=$maximized cornerPref=$cornerPref setHr=0x${setHr.toString(16)}" +
                " getHr=0x${getHr.toString(16)} readBack=$readBack" +
                " marginsPx=$marginPx marginsHr=0x${marginsHr.toString(16)}",
        )
    } catch (t: Throwable) {
        chromeTrace("dwm-error", hwndLong, "${t::class.java.name}: ${t.message}")
    }
}
