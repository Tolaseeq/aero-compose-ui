package com.mordred.aero.components.pickers

import kotlinx.datetime.LocalDate

/**
 * Selection state machine for [AeroDateRangePicker] (PICK-02). A sealed type makes the three
 * legal states explicit and lets [nextRangeState] decide — in one place — when a completed range
 * may be emitted. This is the crux of PITFALL-06: `onRangeSelect` must fire EXACTLY once per
 * completed range and NEVER after only a start click.
 *
 *  - [Idle]:         nothing selected yet.
 *  - [SelectingEnd]: a start date is chosen; awaiting the end click.
 *  - [Selected]:     an ordered (start, end) range is committed.
 */
internal sealed interface AeroDateRangeState {
    data object Idle : AeroDateRangeState
    data class SelectingEnd(val start: LocalDate) : AeroDateRangeState
    data class Selected(val start: LocalDate, val end: LocalDate) : AeroDateRangeState
}

/**
 * Pure transition for the range selection state machine (testable without Compose).
 *
 * Returns the next [AeroDateRangeState] and, ONLY when a range is committed (the
 * [AeroDateRangeState.SelectingEnd] -> [AeroDateRangeState.Selected] transition), the ordered
 * `(start, end)` pair to emit. For every other transition the second element is `null`, which the
 * composable uses as the guard around the single `onRangeSelect(` call site (PITFALL-06):
 *
 *  - From [AeroDateRangeState.Idle] or [AeroDateRangeState.Selected]: a click begins a new range
 *    -> `SelectingEnd(clicked)`, emit `null` (NOT a completed range).
 *  - From [AeroDateRangeState.SelectingEnd]: the click completes the range. The two dates are
 *    ordered so `start <= end` regardless of click order -> `Selected(s, e)`, emit `(s, e)`.
 */
internal fun nextRangeState(
    current: AeroDateRangeState,
    clicked: LocalDate,
): Pair<AeroDateRangeState, Pair<LocalDate, LocalDate>?> = when (current) {
    is AeroDateRangeState.Idle,
    is AeroDateRangeState.Selected -> AeroDateRangeState.SelectingEnd(clicked) to null
    is AeroDateRangeState.SelectingEnd -> {
        val (s, e) = if (clicked >= current.start) current.start to clicked else clicked to current.start
        AeroDateRangeState.Selected(s, e) to (s to e)
    }
}
