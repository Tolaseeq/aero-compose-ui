package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class AeroDateTimeRangePickerTest {
    private val day = LocalDate(2026, 6, 18)
    private val day20 = LocalDate(2026, 6, 20)

    @Test
    fun sameDayReversedTimesSwapToOrdered() {
        val (s, e) = orderDateTimeRange(day, LocalTime(15, 0, 0), day, LocalTime(8, 0, 0))
        assertEquals(LocalDateTime(2026, 6, 18, 8, 0, 0), s)
        assertEquals(LocalDateTime(2026, 6, 18, 15, 0, 0), e)
    }

    @Test
    fun sameDayOrderedTimesUnchanged() {
        val (s, e) = orderDateTimeRange(day, LocalTime(8, 0, 0), day, LocalTime(15, 0, 0))
        assertEquals(LocalDateTime(2026, 6, 18, 8, 0, 0), s)
        assertEquals(LocalDateTime(2026, 6, 18, 15, 0, 0), e)
    }

    @Test
    fun crossDayDateDominatesOverTimeOfDay() {
        // start later time-of-day but earlier DATE → must stay ordered (date dominates).
        val (s, e) = orderDateTimeRange(day, LocalTime(23, 0, 0), day20, LocalTime(1, 0, 0))
        assertEquals(LocalDateTime(2026, 6, 18, 23, 0, 0), s)
        assertEquals(LocalDateTime(2026, 6, 20, 1, 0, 0), e)
    }

    @Test
    fun equalDateTimesUnchanged() {
        val (s, e) = orderDateTimeRange(day, LocalTime(10, 0, 0), day, LocalTime(10, 0, 0))
        assertEquals(s, e)
        assertEquals(LocalDateTime(2026, 6, 18, 10, 0, 0), s)
    }

    // DTR-03 Apply-gate: nextRangeState reaching Selected is the SOLE Apply-enable condition.
    @Test
    fun applyDisabledAfterOnlyStartClick() {
        val (next, commit) = nextRangeState(AeroDateRangeState.Idle, day)
        assertEquals(AeroDateRangeState.SelectingEnd(day), next)
        assertNull(commit, "single start click must not produce a committable range → Apply disabled")
    }

    @Test
    fun applyEnabledForSingleDayRange() {
        // SelectingEnd(day) + same-date click → Selected(day, day) (clicked >= start at equality).
        val (next, commit) = nextRangeState(AeroDateRangeState.SelectingEnd(day), day)
        assertEquals(AeroDateRangeState.Selected(day, day), next)
        assertNotNull(commit, "single-day range is a valid Selected → Apply enabled")
    }
}
