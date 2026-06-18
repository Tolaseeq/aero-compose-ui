package com.mordred.aero.components.datatable.internal

import com.mordred.aero.components.datatable.SortDirection

/** Active sort: which column key (null = none) and its direction. */
internal data class SortState(
    val columnKey: Int? = null,      // index into columns list; null = no active sort
    val direction: SortDirection = SortDirection.None,
)

/**
 * Pure transition for a header click on column [clickedKey].
 * - different column -> Asc on the new column
 * - same column -> Asc -> Desc -> None -> Asc
 * - None resets columnKey to null so no indicator shows.
 */
internal fun nextSortState(current: SortState, clickedKey: Int): SortState =
    if (current.columnKey != clickedKey) {
        SortState(columnKey = clickedKey, direction = SortDirection.Asc)
    } else when (current.direction) {
        SortDirection.Asc -> SortState(clickedKey, SortDirection.Desc)
        SortDirection.Desc -> SortState(columnKey = null, direction = SortDirection.None)
        SortDirection.None -> SortState(clickedKey, SortDirection.Asc)
    }
