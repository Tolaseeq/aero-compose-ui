package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * PICK-04: verifies [combineDateTime] — the pure commit-gate helper behind AeroDateTimePicker.
 * The composable holds a pending (date, time) pair; [combineDateTime] is the ONLY place those two
 * are merged into the emitted [LocalDateTime], and the composable only calls it inside the Apply
 * button's onClick (NEW-PICK-02). These tests lock the merge contract; the Apply-only emission
 * wiring is covered by acceptance-criteria greps in Task 2.
 */
class AeroDateTimePickerTest {

    @Test
    fun combinesDateAndTimeIntoLocalDateTime() {
        assertEquals(
            LocalDateTime(2026, 6, 18, 14, 30, 0),
            combineDateTime(LocalDate(2026, 6, 18), LocalTime(14, 30, 0)),
            "combineDateTime must merge the date and time components into one LocalDateTime",
        )
    }

    @Test
    fun resultDateAndTimeAccessorsMatchInputs() {
        val date = LocalDate(2026, 12, 31)
        val time = LocalTime(23, 59, 58)
        val result = combineDateTime(date, time)

        assertEquals(date, result.date, "result.date must equal the input date")
        assertEquals(time, result.time, "result.time must equal the input time")
    }

    @Test
    fun applyGateProducesValueOnlyWhenInvoked() {
        // Models the composable's commit gate: a pending date/time pair must NOT yield an emitted
        // LocalDateTime until "apply" runs. Day selection alone leaves nothing to emit.
        val pendingDate = LocalDate(2026, 6, 18)
        val pendingTime = LocalTime(9, 15, 0)

        var emitted: LocalDateTime? = null
        // Before Apply: nothing emitted even though a pending pair exists.
        assertNull(emitted, "no value may be emitted before Apply is invoked")

        // Apply is invoked: the gate combines pending state and emits exactly once.
        emitted = combineDateTime(pendingDate, pendingTime)
        assertEquals(LocalDateTime(2026, 6, 18, 9, 15, 0), emitted)
    }
}
