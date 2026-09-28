package com.mordred.aero.internal.windows

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PITFALLS 7 / T-22-03: headless locks for the pure helpers used inside the native callback
 * body. A Throwable must never escape a JNA callback — it does not unwind normally through the
 * native stack and can crash the JVM — so [dispatchSafely] must fall back exactly once, and
 * WM_NCHITTEST's packed lParam must decode negative screen coordinates (multi-monitor windows
 * live at negative origins).
 */
class WndProcSupportTest {

    /** T-22-03: the happy path returns the handled value. */
    @Test
    fun dispatchSafelyReturnsTheHandledValue() {
        assertEquals(42L, dispatchSafely(handle = { 42L }, fallback = { -1L }))
    }

    /** PITFALLS 7: a RuntimeException yields the fallback, which runs exactly once. */
    @Test
    fun runtimeExceptionYieldsTheFallbackExactlyOnce() {
        var fallbackRuns = 0
        val result = dispatchSafely(
            handle = { throw IllegalStateException("wndproc handler boom") },
            fallback = { fallbackRuns++; 7L },
        )
        assertEquals(7L, result)
        assertEquals(1, fallbackRuns)
    }

    /**
     * PITFALLS 7: a StackOverflowError is also a Throwable and must degrade to the fallback
     * rather than escaping the callback boundary. The process-wide one-shot first-failure
     * stderr print is consumed by a cheap RuntimeException first, so the catch block does as
     * little work as possible while the stack is still deep.
     */
    @Test
    fun stackOverflowErrorYieldsTheFallback() {
        dispatchSafely(
            handle = { throw IllegalStateException("consume the one-shot error print") },
            fallback = { 0L },
        )
        assertEquals(9L, dispatchSafely(handle = { infiniteRecursion() }, fallback = { 9L }))
    }

    @Suppress("InfiniteRecursion")
    private fun infiniteRecursion(): Long = infiniteRecursion() + 1L

    /** SNAP-01 / WIN-04: signed 16-bit halves decode, including negative multi-monitor origins. */
    @Test
    fun decodeScreenPointHandlesNegativeSixteenBitCoordinates() {
        assertEquals(-1 to -1, decodeScreenPoint(encodeScreenPoint(-1, -1)))
        assertEquals(-1920 to 100, decodeScreenPoint(encodeScreenPoint(-1920, 100)))
        assertEquals(100 to -1920, decodeScreenPoint(encodeScreenPoint(100, -1920)))
        assertEquals(1171 to 64, decodeScreenPoint(encodeScreenPoint(1171, 64)))
    }

    private fun encodeScreenPoint(x: Int, y: Int): Long =
        ((y.toLong() and 0xFFFF) shl 16) or (x.toLong() and 0xFFFF)

    /**
     * SNAP-05: MENU only for the SC_KEYMENU trigger; the commands that must keep forwarding
     * verbatim (Alt+F4's SC_CLOSE, the state commands, anything unrelated) all discriminate to
     * FORWARD.
     */
    @Test
    fun systemMenuDispositionAnswersMenuOnlyForKeymenu() {
        assertEquals(SysCommandDisposition.MENU, systemMenuDisposition(SC_KEYMENU))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(SC_CLOSE))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(SC_MAXIMIZE))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(SC_MINIMIZE))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(SC_RESTORE))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(0x1234))
    }

    /** SNAP-05: wParam's low four bits are internal flags — 0xF102 is still SC_KEYMENU. */
    @Test
    fun systemMenuDispositionMasksLowFlagBitsBeforeDiscriminating() {
        assertEquals(SysCommandDisposition.MENU, systemMenuDisposition(0xF102))
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(0xF061))
    }

    /** SNAP-05: zero is not a menu trigger — it forwards like every unrelated value. */
    @Test
    fun systemMenuDispositionForwardsZero() {
        assertEquals(SysCommandDisposition.FORWARD, systemMenuDisposition(0))
    }

    /**
     * SNAP-04: the caption double-click routes to DefWindowProc, the maximize button keeps its
     * swallow, everything else forwards to AWT's proc as before.
     */
    @Test
    fun ncDoubleClickDispositionRoutesByHitCode() {
        assertEquals(NcDoubleClickDisposition.DEF_WINDOW_PROC, ncDoubleClickDisposition(HTCAPTION))
        assertEquals(NcDoubleClickDisposition.SWALLOW, ncDoubleClickDisposition(HTMAXBUTTON))
        assertEquals(NcDoubleClickDisposition.FORWARD_AWT, ncDoubleClickDisposition(HTCLIENT))
        assertEquals(NcDoubleClickDisposition.FORWARD_AWT, ncDoubleClickDisposition(HTLEFT))
    }
}
