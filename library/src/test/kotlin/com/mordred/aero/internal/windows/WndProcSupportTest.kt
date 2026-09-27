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
}
