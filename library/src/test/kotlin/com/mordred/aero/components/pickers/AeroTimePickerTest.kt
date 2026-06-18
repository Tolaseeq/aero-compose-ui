package com.mordred.aero.components.pickers

import com.mordred.aero.components.pickers.internal.assembleTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PICK-03: verifies [assembleTime] — the pure helper behind AeroTimePicker / TimeFields emission.
 * Covers seconds drop when showSeconds=false, seconds kept when true, and out-of-range clamping
 * for hour (0..23) and minute/second (0..59). 24-hour only — no 12h / AM-PM behavior to test.
 */
class AeroTimePickerTest {

    @Test
    fun secondsDroppedWhenShowSecondsFalse() {
        assertEquals(
            LocalTime(14, 30, 0),
            assembleTime(14, 30, 45, showSeconds = false),
            "with showSeconds=false the emitted LocalTime must have second=0",
        )
    }

    @Test
    fun secondsKeptWhenShowSecondsTrue() {
        assertEquals(
            LocalTime(14, 30, 45),
            assembleTime(14, 30, 45, showSeconds = true),
            "with showSeconds=true the emitted LocalTime must keep the second component",
        )
    }

    @Test
    fun aboveRangeValuesClampToUpperBounds() {
        assertEquals(
            LocalTime(23, 59, 0),
            assembleTime(25, 70, 0, showSeconds = false),
            "hour clamps to 23 and minute clamps to 59",
        )
    }

    @Test
    fun belowRangeValuesClampToZero() {
        assertEquals(
            LocalTime(0, 0, 0),
            assembleTime(-1, -5, -5, showSeconds = true),
            "negative hour/minute/second clamp to 0",
        )
    }
}
