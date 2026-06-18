package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PICK-01: verifies [dateIsDisabled] composes the minDate / maxDate / selectableDates
 * constraints correctly. Pure-function test — no composable driven.
 */
class AeroDatePickerTest {

    private val min = LocalDate(2026, 6, 10)
    private val max = LocalDate(2026, 6, 20)
    private val allowAll: (LocalDate) -> Boolean = { true }

    @Test
    fun dateBeforeMinDateIsDisabled() {
        val before = LocalDate(2026, 6, 9)
        assertTrue(
            dateIsDisabled(before, minDate = min, maxDate = max, selectableDates = allowAll),
            "date before minDate must be disabled",
        )
    }

    @Test
    fun dateAfterMaxDateIsDisabled() {
        val after = LocalDate(2026, 6, 21)
        assertTrue(
            dateIsDisabled(after, minDate = min, maxDate = max, selectableDates = allowAll),
            "date after maxDate must be disabled",
        )
    }

    @Test
    fun dateInRangeRejectedBySelectableDatesIsDisabled() {
        val target = LocalDate(2026, 6, 15)
        val rejectTarget: (LocalDate) -> Boolean = { it != target }
        assertTrue(
            dateIsDisabled(target, minDate = min, maxDate = max, selectableDates = rejectTarget),
            "in-range date rejected by selectableDates must be disabled",
        )
    }

    @Test
    fun dateInRangeAcceptedBySelectableDatesIsNotDisabled() {
        val target = LocalDate(2026, 6, 15)
        assertFalse(
            dateIsDisabled(target, minDate = min, maxDate = max, selectableDates = allowAll),
            "in-range, accepted date must NOT be disabled",
        )
    }

    @Test
    fun noConstraintsLeavesEveryDateEnabled() {
        val anyDate = LocalDate(1999, 1, 1)
        assertFalse(
            dateIsDisabled(anyDate, minDate = null, maxDate = null, selectableDates = allowAll),
            "with no constraints, no date is disabled",
        )
    }

    @Test
    fun boundaryDatesAreInclusive() {
        // minDate and maxDate themselves are selectable (inclusive bounds).
        assertFalse(
            dateIsDisabled(min, minDate = min, maxDate = max, selectableDates = allowAll),
            "minDate boundary must be enabled (inclusive)",
        )
        assertFalse(
            dateIsDisabled(max, minDate = min, maxDate = max, selectableDates = allowAll),
            "maxDate boundary must be enabled (inclusive)",
        )
    }
}
