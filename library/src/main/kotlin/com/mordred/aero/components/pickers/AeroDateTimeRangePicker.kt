package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Orders a (start, end) datetime range so the emitted pair satisfies start <= end by full
 * [LocalDateTime] (DTR-04). [nextRangeState] orders DATES but not times; when start date == end
 * date and startTime > endTime the pair would be chronologically inverted. Applied ONLY at the
 * Apply onClick (the sole emit site). LocalDateTime is Comparable in kotlinx-datetime 0.6.2 —
 * `<=` works directly, no Instant/timezone conversion. Pure + internal for unit testing.
 */
internal fun orderDateTimeRange(
    startDate: LocalDate,
    startTime: LocalTime,
    endDate: LocalDate,
    endTime: LocalTime,
): Pair<LocalDateTime, LocalDateTime> {
    val a = combineDateTime(startDate, startTime)
    val b = combineDateTime(endDate, endTime)
    return if (a <= b) a to b else b to a
}
