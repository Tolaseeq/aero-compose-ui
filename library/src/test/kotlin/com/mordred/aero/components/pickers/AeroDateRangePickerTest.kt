package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * PICK-02: locks the sealed-state transition behind AeroDateRangePicker via the pure
 * [nextRangeState] helper. The crux (PITFALL-06): `onRangeSelect` must fire EXACTLY once per
 * completed range and NEVER after only a start click. Because the composable's single
 * `onRangeSelect(` call site is guarded by the non-null commit pair returned here, proving the
 * commit-emission contract on [nextRangeState] proves the callback contract.
 */
class AeroDateRangePickerTest {

    private val d1 = LocalDate(2026, 6, 1)
    private val d5 = LocalDate(2026, 6, 5)
    private val d10 = LocalDate(2026, 6, 10)

    @Test
    fun firstClickFromIdleBeginsSelectingEndAndEmitsNothing() {
        val (next, commit) = nextRangeState(AeroDateRangeState.Idle, d5)
        assertEquals(AeroDateRangeState.SelectingEnd(d5), next, "first click must enter SelectingEnd")
        assertNull(commit, "a start-only click must NOT commit a range (PITFALL-06)")
    }

    @Test
    fun secondClickAfterStartCommitsOrderedRangeExactlyOnce() {
        val (next, commit) = nextRangeState(AeroDateRangeState.SelectingEnd(d5), d10)
        assertEquals(AeroDateRangeState.Selected(d5, d10), next)
        assertEquals(d5 to d10, commit, "completing a range must emit the ordered (start, end)")
    }

    @Test
    fun secondClickBeforeStartOrdersTheRange() {
        // Clicking an earlier date as the "end" still yields an ordered start <= end range.
        val (next, commit) = nextRangeState(AeroDateRangeState.SelectingEnd(d5), d1)
        assertEquals(AeroDateRangeState.Selected(d1, d5), next, "range must be ordered start <= end")
        assertEquals(d1 to d5, commit)
    }

    @Test
    fun clickFromSelectedBeginsNewRangeWithoutEmitting() {
        val (next, commit) = nextRangeState(AeroDateRangeState.Selected(d1, d10), d5)
        assertEquals(AeroDateRangeState.SelectingEnd(d5), next, "a new click restarts selection")
        assertNull(commit, "starting a fresh range must NOT re-emit (PITFALL-06)")
    }

    @Test
    fun twoClickSequenceEmitsExactlyOnce() {
        // Drive a full first-click then second-click sequence; count the non-null commit pairs.
        val emitted = mutableListOf<Pair<LocalDate, LocalDate>>()
        var state: AeroDateRangeState = AeroDateRangeState.Idle

        // First click (start).
        val (s1, c1) = nextRangeState(state, d1)
        state = s1
        c1?.let { emitted += it }
        assertTrue(emitted.isEmpty(), "no emission after only the first click")

        // Second click (end).
        val (s2, c2) = nextRangeState(state, d10)
        state = s2
        c2?.let { emitted += it }

        assertEquals(1, emitted.size, "onRangeSelect must fire exactly once per completed range")
        assertEquals(d1 to d10, emitted.single())
    }

    // F14: same-month range selection — both endpoints in the SAME calendar month must commit.

    private val d7june = LocalDate(2025, 6, 7)
    private val d17june = LocalDate(2025, 6, 17)

    @Test
    fun sameMonthForwardOrderCommitsOnce() {
        // (Idle -> SelectingEnd(07.06)) + (SelectingEnd(07.06) -> 17.06) must yield Selected(07,17).
        val (nextStart, commitStart) = nextRangeState(AeroDateRangeState.Idle, d7june)
        assertNull(commitStart, "start click in same month must NOT emit (PITFALL-06)")
        assertEquals(AeroDateRangeState.SelectingEnd(d7june), nextStart)

        val (nextEnd, commitEnd) = nextRangeState(nextStart, d17june)
        assertNotNull(commitEnd, "end click in same month must commit a range (F14)")
        assertEquals(d7june to d17june, commitEnd, "same-month range must be ordered 07.06->17.06")
        assertEquals(AeroDateRangeState.Selected(d7june, d17june), nextEnd)
    }

    @Test
    fun sameMonthReverseOrderCommitsOrderedRange() {
        // Clicking 17.06 first then 07.06 must still yield an ordered start<=end range (F14).
        val (nextStart, _) = nextRangeState(AeroDateRangeState.Idle, d17june)
        val (nextEnd, commitEnd) = nextRangeState(nextStart, d7june)

        assertNotNull(commitEnd, "reverse same-month end click must commit a range (F14)")
        assertEquals(d7june to d17june, commitEnd, "reverse-order same-month range must reorder to start<=end")
        assertEquals(AeroDateRangeState.Selected(d7june, d17june), nextEnd)
    }
}
