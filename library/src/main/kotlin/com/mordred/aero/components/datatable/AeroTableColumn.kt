package com.mordred.aero.components.datatable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/** Type-safe column width. Either a fixed [Dp] or a [Weight] share of remaining space. */
public sealed interface AeroColumnWidth {
    public data class Fixed(public val dp: Dp) : AeroColumnWidth
    public data class Weight(public val value: Float) : AeroColumnWidth
}

/**
 * One column spec for [AeroDataTable]. Plain data class (no DSL).
 *
 * @param header column title; the library draws the sort caret next to it.
 * @param width [AeroColumnWidth.Fixed] or [AeroColumnWidth.Weight].
 * @param alignment horizontal alignment of cell content (numeric/date columns use [Alignment.End]).
 * @param sortKey OPTIONAL comparable extractor. If set AND `onSortChange` is NOT supplied to the
 *   table, the table self-sorts on header click (asc -> desc -> none). If `onSortChange` IS
 *   supplied the table is controlled and this branch is bypassed. Both modes are intentional —
 *   see AeroDataTable KDoc; do not collapse to one branch.
 * @param minWidth lower clamp for drag-resize; default 40.dp prevents collapse to 0dp (DATA-04).
 * @param cell composable slot rendering one cell for a row of type [T].
 */
public data class AeroTableColumn<T>(
    public val header: String,
    public val width: AeroColumnWidth,
    public val alignment: Alignment.Horizontal = Alignment.Start,
    public val sortKey: ((T) -> Comparable<*>)? = null,
    public val minWidth: Dp = 40.dp,
    public val cell: @Composable (T) -> Unit,
)

/**
 * Convenience builder for the common text-cell case.
 * Renders [text] for the row via a plain Text using AeroTheme.colors.onSurface in the cell.
 */
public fun <T> textColumn(
    header: String,
    width: AeroColumnWidth,
    alignment: Alignment.Horizontal = Alignment.Start,
    sortKey: ((T) -> Comparable<*>)? = null,
    minWidth: Dp = 40.dp,
    text: (T) -> String,
): AeroTableColumn<T> = AeroTableColumn(
    header = header,
    width = width,
    alignment = alignment,
    sortKey = sortKey,
    minWidth = minWidth,
    cell = { row -> Text(text(row)) },
)
