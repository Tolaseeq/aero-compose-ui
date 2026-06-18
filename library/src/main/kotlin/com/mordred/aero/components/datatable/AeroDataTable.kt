package com.mordred.aero.components.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.containers.AeroScrollBar
import com.mordred.aero.components.datatable.internal.AeroTableHeader
import com.mordred.aero.components.datatable.internal.AeroTableRow
import com.mordred.aero.components.datatable.internal.ResolvedWidth
import com.mordred.aero.components.datatable.internal.SortState
import com.mordred.aero.components.datatable.internal.applyCtrlClick
import com.mordred.aero.components.datatable.internal.applySingleClick
import com.mordred.aero.components.datatable.internal.computeShiftRange
import com.mordred.aero.components.datatable.internal.nextSortState
import com.mordred.aero.components.datatable.internal.resolveColumnWidths
import com.mordred.aero.theme.AeroTheme

/**
 * DATA-01..04: Public virtualized data table with sortable headers, row selection,
 * and drag-resizable columns.
 *
 * ## Virtualization (DATA-01)
 * AeroDataTable **owns its own [LazyColumn] + [LazyListState]** paired with [AeroScrollBar].
 * Do NOT place this inside a vertically-scrollable container wrapper — that provides unbounded
 * height to the inner [LazyColumn], causing all rows to materialize and destroying
 * virtualization (PITFALL-01).
 *
 * ## Sort — hybrid dual-mode (DATA-02)
 * Sort operates in two distinct modes that must NOT be collapsed:
 *
 * **Uncontrolled (default):** When a column has a non-null [AeroTableColumn.sortKey] and
 * [onSortChange] is `null`, the table self-sorts the displayed rows internally on each
 * header click (asc → desc → none cycle). The caller supplies unsorted [data] and the
 * table handles ordering. Suitable for in-memory datasets where the caller does not need
 * to know the sort state.
 *
 * **Controlled:** When [onSortChange] is non-null, the table is in controlled mode — on each
 * header click it reports the column index and new direction to [onSortChange] and does NOT
 * reorder [data] itself. The caller is responsible for supplying pre-sorted data (e.g. for
 * server-side sorting or custom comparators). Both branches are intentional and must be kept.
 *
 * ## Selection (DATA-03)
 * Selection is controlled: pass [selectedKeys] and receive updates via [onSelectionChange].
 * Keys are stable across sort because they come from the caller's [key] function, not row indices.
 * Ctrl-click toggles, Shift-click selects a contiguous range in displayed order. The Shift
 * anchor is a private internal `remember` — it is not part of the public API.
 *
 * ## Column resize (DATA-04)
 * Column drag-resize via [aeroDragSplitter] clamped to each column's [AeroTableColumn.minWidth].
 * No column collapses to 0dp.
 *
 * @param data the data list to display. In uncontrolled sort mode the table sorts this internally.
 *   In controlled sort mode pass pre-sorted data.
 * @param columns column specs (header, width, cell slot, optional sortKey).
 * @param key stable key extractor per row item — used for both [LazyColumn] item keys and
 *   selection identity. Must be unique and stable across recompositions and sort reorderings.
 * @param modifier optional layout modifier applied to the root [BoxWithConstraints].
 * @param selectionMode none / single / multi; default none (no row click handling).
 * @param selectedKeys set of currently selected row keys. Controlled by the caller.
 * @param rowHeight fixed row height; also used for header height. Default 36.dp.
 * @param emptyContent optional composable shown when [data] is empty. If null, a default
 *   localized "Нет данных" text is rendered.
 * @param onSelectionChange called when the selection set changes. Receives the new [Set<Any>].
 * @param onSortChange when non-null, puts the table in controlled sort mode. Called on each
 *   header click with the column index and new [SortDirection]. Null = uncontrolled sort mode.
 */
@Composable
public fun <T> AeroDataTable(
    data: List<T>,
    columns: List<AeroTableColumn<T>>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    selectionMode: SelectionMode = SelectionMode.None,
    selectedKeys: Set<Any> = emptySet(),
    rowHeight: Dp = 36.dp,
    emptyContent: (@Composable () -> Unit)? = null,
    onSelectionChange: (Set<Any>) -> Unit = {},
    onSortChange: ((columnIndex: Int, direction: SortDirection) -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    var sortState by remember { mutableStateOf(SortState()) }

    // Private shift anchor — not exposed in public API (see KDoc above)
    var shiftAnchorKey by remember { mutableStateOf<Any?>(null) }

    // Per-column resize overrides in px (accumulated drag deltas)
    val resizeOverridesPx = remember { mutableStateMapOf<Int, Float>() }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val pxPerDp = density.density
        val availableWidthPx = constraints.maxWidth.toFloat()

        // Resolved base widths from column specs + available width, plus per-column resize override
        val widthsDp: List<Dp> = remember(columns, availableWidthPx, resizeOverridesPx.toMap()) {
            val baseWidths = resolveColumnWidths(
                widths = columns.map { it.width },
                minWidthsDp = columns.map { it.minWidth.value },
                totalWidthPx = availableWidthPx,
                pxPerDp = pxPerDp,
            )
            baseWidths.mapIndexed { i, resolved ->
                val overrideDp = (resizeOverridesPx[i] ?: 0f) / pxPerDp
                val adjustedDp = resolved.dpValue + overrideDp
                val minDp = columns[i].minWidth.value
                ResolvedWidth(adjustedDp.coerceAtLeast(minDp)).dpValue.dp
            }
        }

        // Hybrid sort: self-sort in uncontrolled mode; pass through in controlled mode
        @Suppress("UNCHECKED_CAST")
        val displayedData: List<T> = remember(data, sortState, onSortChange) {
            if (onSortChange == null
                && sortState.columnKey != null
                && sortState.direction != SortDirection.None
            ) {
                val col = columns[sortState.columnKey!!]
                val cmp = compareBy<T> { (col.sortKey!!.invoke(it)) as Comparable<Any?> }
                val sorted = data.sortedWith(cmp)
                if (sortState.direction == SortDirection.Desc) sorted.reversed() else sorted
            } else {
                data
            }
        }

        val displayedKeys: List<Any> = remember(displayedData) { displayedData.map(key) }

        // Sort click handler — cycles asc/desc/none; notifies controlled caller if present
        val onSortClick: (Int) -> Unit = { columnIndex ->
            val next = nextSortState(sortState, columnIndex)
            sortState = next
            onSortChange?.invoke(columnIndex, next.direction)
        }

        // Column resize: accumulate delta into overrides map, clamp applied during width resolve
        val onColumnResize: (Int, Float) -> Unit = { columnIndex, deltaPx ->
            val current = resizeOverridesPx[columnIndex] ?: 0f
            resizeOverridesPx[columnIndex] = current + deltaPx
        }
        val onColumnResizeEnd: () -> Unit = {}

        Column(modifier = Modifier.fillMaxSize()) {
            // Glass header row — external Row, not LazyColumn header slot (JetBrains #3016/#2940)
            AeroTableHeader(
                columns = columns,
                columnWidthsDp = widthsDp,
                sortState = sortState,
                horizontalScrollState = horizontalScrollState,
                onSortClick = onSortClick,
                onColumnResize = onColumnResize,
                onColumnResizeEnd = onColumnResizeEnd,
                rowHeight = rowHeight,
            )

            if (displayedData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    emptyContent?.invoke() ?: Text(
                        text = "Нет данных",
                        color = colors.labelText,
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp),
                    ) {
                        items(items = displayedData, key = { key(it) }) { item ->
                            val rowKey = key(item)
                            AeroTableRow(
                                item = item,
                                columns = columns,
                                columnWidthsDp = widthsDp,
                                rowHeight = rowHeight,
                                isSelected = rowKey in selectedKeys,
                                selectionMode = selectionMode,
                                horizontalScrollState = horizontalScrollState,
                                onSingleClick = {
                                    onSelectionChange(applySingleClick(rowKey))
                                    shiftAnchorKey = rowKey
                                },
                                onCtrlClick = {
                                    if (selectionMode == SelectionMode.Multi) {
                                        val ns = applyCtrlClick(selectedKeys, rowKey)
                                        onSelectionChange(ns)
                                        shiftAnchorKey = rowKey
                                    } else {
                                        // Single mode: Ctrl maps to plain single-replace
                                        onSelectionChange(applySingleClick(rowKey))
                                        shiftAnchorKey = rowKey
                                    }
                                },
                                onShiftClick = {
                                    if (selectionMode == SelectionMode.Multi) {
                                        // Shift anchor does NOT update on shift-click
                                        onSelectionChange(computeShiftRange(displayedKeys, shiftAnchorKey, rowKey))
                                    } else {
                                        // Single mode: Shift maps to plain single-replace
                                        onSelectionChange(applySingleClick(rowKey))
                                        shiftAnchorKey = rowKey
                                    }
                                },
                            )
                        }
                    }

                    // Own scrollbar — paired with owned LazyListState (PITFALL-01 compliant)
                    AeroScrollBar(lazyListState = lazyListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}
