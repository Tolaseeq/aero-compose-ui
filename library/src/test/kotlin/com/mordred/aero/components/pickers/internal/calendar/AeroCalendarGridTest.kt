package com.mordred.aero.components.pickers.internal.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Phase 7 ROADMAP success criterion #1 — 3-scenario calendar test.
 *
 * Note: AeroCalendarGrid itself is a `@Composable` and rendering it requires the Compose UI
 * test harness (out of v2.0 scope). We test the LOGIC layer (`daysInMonth`, month arithmetic)
 * which is what actually drives the grid's cell count. A compile-smoke test confirms the
 * composable file is on the classpath.
 */
class AeroCalendarGridTest {

    // === Scenario 1: Current month (April 2026 — 30 days) ===

    @Test
    fun aprilHas30Days() {
        val april = LocalDate(2026, 4, 1)
        assertEquals(30, daysInMonth(april))
        assertEquals(Month.APRIL, april.month)
    }

    // === Scenario 2: Month boundary (Dec -> Jan crosses year) ===

    @Test
    fun decemberPlusOneMonthBecomesJanuaryNextYear() {
        val dec2026 = LocalDate(2026, 12, 15)
        val nextMonth = dec2026.plus(1, DateTimeUnit.MONTH)
        assertEquals(2027, nextMonth.year, "year must increment when crossing Dec->Jan")
        assertEquals(Month.JANUARY, nextMonth.month, "month must roll to January")
        assertEquals(15, nextMonth.dayOfMonth, "day-of-month preserved by kotlinx-datetime month-add")
    }

    // === Scenario 3: Leap year — Feb 2024 has 29 days; Feb 2023 has 28 ===

    @Test
    fun leapYearFeb2024Has29Days() {
        val feb2024 = LocalDate(2024, 2, 1)
        assertEquals(29, daysInMonth(feb2024), "2024 is a leap year — Feb has 29 days")
    }

    @Test
    fun nonLeapYearFeb2023Has28Days() {
        val feb2023 = LocalDate(2023, 2, 1)
        assertEquals(28, daysInMonth(feb2023), "2023 is NOT a leap year — Feb has 28 days")
    }

    @Test
    fun centuryNonLeapYearFeb1900Has28Days() {
        // 1900 is divisible by 100 but not by 400 -> NOT a leap year (Gregorian rule).
        val feb1900 = LocalDate(1900, 2, 1)
        assertEquals(28, daysInMonth(feb1900))
    }

    @Test
    fun centuryLeapYearFeb2000Has29Days() {
        // 2000 is divisible by 400 -> leap year.
        val feb2000 = LocalDate(2000, 2, 1)
        assertEquals(29, daysInMonth(feb2000))
    }

    // === Compile-smoke: composable file is on the classpath ===

    @Test
    fun aeroCalendarGridCompileSmoke() {
        val cls = Class.forName(
            "com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGridKt"
        )
        assertNotNull(cls)
    }
}
