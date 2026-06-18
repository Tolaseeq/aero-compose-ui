package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Merges a [LocalDate] and a [LocalTime] into a single [LocalDateTime]. This is the commit-gate
 * seam for `AeroDateTimePicker` (PICK-04): the pending date and pending time live in separate
 * state holders and are only combined here when the user clicks **Apply**. Extracted as a pure,
 * `internal` function so the combine contract is unit-testable without driving the composable.
 */
internal fun combineDateTime(date: LocalDate, time: LocalTime): LocalDateTime =
    LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute, time.second)
