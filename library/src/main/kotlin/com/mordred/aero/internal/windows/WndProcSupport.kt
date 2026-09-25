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
 * SNAP-01 hardcoded title-row classifier (a later, live region-registry read replaces this
 * once real layout rects are published from Compose): the whole 32dp title row answers
 * HTCAPTION except the three 46dp-wide, right-aligned button rects (8dp right padding, order
 * Minimize/Maximize/Close, matching `AeroTitleBar`'s own layout) — the maximize rect answers
 * HTMAXBUTTON, minimize/close answer HTCLIENT so Compose keeps handling their clicks
 * directly; everything outside the row answers HTCLIENT.
 */
internal fun classifySpikeTitleRow(clientX: Int, clientY: Int, clientWidthPx: Int, scale: Float): Int {
    val titleRowBottomPx = (32f * scale).toInt()
    if (clientY < 0 || clientY >= titleRowBottomPx) return HTCLIENT

    val buttonWidthPx = (46f * scale).toInt()
    val rightPaddingPx = (8f * scale).toInt()

    val closeLeft = clientWidthPx - rightPaddingPx - buttonWidthPx
    val closeRight = clientWidthPx - rightPaddingPx
    val maxLeft = closeLeft - buttonWidthPx
    val maxRight = closeLeft
    val minLeft = maxLeft - buttonWidthPx
    val minRight = maxLeft

    return when (clientX) {
        in maxLeft until maxRight -> HTMAXBUTTON
        in minLeft until minRight -> HTCLIENT
        in closeLeft until closeRight -> HTCLIENT
        else -> HTCAPTION
    }
}
