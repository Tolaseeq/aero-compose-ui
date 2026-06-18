package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * F6 (plan 11-08): verifies [formatAeroDate] produces the DD.MM.YYYY canonical date display.
 * Pure-function test — no composable driven.
 */
class AeroDateFormatTest {

    @Test
    fun singleDigitDayAndMonthArePaddedWithLeadingZero() {
        val date = LocalDate(2025, 6, 7)
        assertEquals(
            "07.06.2025",
            formatAeroDate(date),
            "formatAeroDate(2025-06-07) must return \"07.06.2025\"",
        )
    }

    @Test
    fun lastDayOfYearFormatsCorrectly() {
        val date = LocalDate(2026, 12, 31)
        assertEquals(
            "31.12.2026",
            formatAeroDate(date),
            "formatAeroDate(2026-12-31) must return \"31.12.2026\"",
        )
    }
}
