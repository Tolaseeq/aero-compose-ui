package com.mordred.aero.internal.windows

import java.util.concurrent.atomic.AtomicBoolean

/**
 * SNAP-01 / SNAP-02: pure-JVM helpers used from inside a native callback body. No JNA, no
 * Compose imports — unit-testable headlessly, same discipline as `PanelDistribution.kt`.
 */

private val chromeErrorPrinted = AtomicBoolean(false)

/**
 * T-22-03: a Throwable must never escape a JNA callback (it does not unwind normally through
 * the native call stack and can crash or destabilize the JVM). Any failure inside [handle]
 * falls back to [fallback] — in every call site here, that is `CallWindowProc(previous, ...)`,
 * so a bug in this feature degrades to "acts like a normal AWT window" instead of corrupting
 * the message loop. The first failure per process is printed to stderr (prefix
 * `AERO_CHROME_ERROR`) so it stays visible without crashing.
 */
internal inline fun dispatchSafely(handle: () -> Long, fallback: () -> Long): Long =
    try {
        handle()
    } catch (t: Throwable) {
        if (chromeErrorPrinted.compareAndSet(false, true)) {
            System.err.println("AERO_CHROME_ERROR ${t::class.java.name}: ${t.message}")
        }
        fallback()
    }

/** WM_NCHITTEST's `lParam` packs a screen point as signed 16-bit halves. */
internal fun decodeScreenPoint(lParam: Long): Pair<Int, Int> {
    val x = (lParam and 0xFFFF).toShort().toInt()
    val y = ((lParam shr 16) and 0xFFFF).toShort().toInt()
    return x to y
}

/**
 * SNAP-05: what the frame proc does with a WM_SYSCOMMAND it receives. MENU — the SC_KEYMENU
 * trigger, owned: display the system menu and swallow (return 0). FORWARD — every other
 * syscommand (SC_CLOSE, SC_MAXIMIZE, SC_MINIMIZE, SC_RESTORE, snap commands, ...): forwarded
 * verbatim to AWT's proc, byte-identical to the pre-ownership behavior.
 */
internal enum class SysCommandDisposition { MENU, FORWARD }

/**
 * SNAP-05: discrimination for WM_SYSCOMMAND. The low four bits of wParam are internal flags,
 * so the command is masked with 0xFFF0 first — `wParam = 0xF102` is still SC_KEYMENU.
 */
internal fun systemMenuDisposition(cmd: Int): SysCommandDisposition =
    if ((cmd and 0xFFF0) == SC_KEYMENU) SysCommandDisposition.MENU else SysCommandDisposition.FORWARD
