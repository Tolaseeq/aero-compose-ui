package com.mordred.aero.components.datatable.internal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.datatable.AeroTableColumn
import com.mordred.aero.components.datatable.SortDirection
import com.mordred.aero.components.internal.drag.aeroDragSplitter
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretUp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

/**
 * Internal glass header row for [AeroDataTable].
 *
 * Renders the column header labels with a Win7 Aero glass gradient ([glassPanel]).
 * Each column with a non-null [AeroTableColumn.sortKey] shows a sort caret ([AeroIcons.CaretUp])
 * that rotates 180° for Desc and fades out when no sort is active on that column.
 * A resize splitter ([aeroDragSplitter]) is placed at the right edge of each column cell.
 *
 * The header's horizontal scroll is driven by the body — [horizontalScrollState] with
 * `enabled = false` so the header tracks the body but does NOT eat scroll gestures.
 *
 * Uses external header Row (not LazyColumn's sticky-header API — JetBrains #3016/#2940).
 * Column resize uses [aeroDragSplitter], not raw pointer gestures (PITFALL-03).
 *
 * @param columns column specs in order.
 * @param columnWidthsDp resolved widths per column.
 * @param sortState current [SortState] (which column + direction).
 * @param horizontalScrollState shared [ScrollState]; header sets enabled=false.
 * @param onSortClick called when a sortable column header is clicked; receives column index.
 * @param onColumnResize called for every drag delta (px); receives column index + delta.
 * @param onColumnResizeEnd called when drag ends.
 * @param rowHeight header height (matches data row height).
 */
@Composable
internal fun <T> AeroTableHeader(
    columns: List<AeroTableColumn<T>>,
    columnWidthsDp: List<Dp>,
    sortState: SortState,
    horizontalScrollState: ScrollState,
    onSortClick: (columnIndex: Int) -> Unit,
    onColumnResize: (columnIndex: Int, deltaPx: Float) -> Unit,
    onColumnResizeEnd: () -> Unit,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val dividerColor = colors.borderDefault.copy(alpha = 0.4f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .glassPanel(cornerRadius = 0.dp)
            .horizontalScroll(horizontalScrollState, enabled = false),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { i, col ->
            // Cell box: contains label + caret, then resize splitter on right edge
            Box(
                modifier = Modifier
                    .width(columnWidthsDp[i])
                    .fillMaxHeight(),
            ) {
                // Label + optional sort caret
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                        .then(
                            if (col.sortKey != null)
                                Modifier.clickable { onSortClick(i) }
                            else
                                Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = col.header,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (col.sortKey != null) {
                        val rotation by animateFloatAsState(
                            targetValue = if (sortState.columnKey == i && sortState.direction == SortDirection.Desc) 180f else 0f,
                            animationSpec = tween(durationMillis = 100),
                            label = "sortCaret",
                        )
                        val caretAlpha = if (sortState.columnKey == i && sortState.direction != SortDirection.None) 1f else 0f

                        Icon(
                            imageVector = AeroIcons.CaretUp,
                            contentDescription = null,
                            tint = colors.onSurface,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .graphicsLayer { rotationZ = rotation }
                                .alpha(caretAlpha),
                        )
                    }
                }

                // Resize splitter on the right edge of this column (drawn as thin vertical line + drag area)
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .drawBehind {
                            // Thin vertical divider — Win7 column separator look
                            drawLine(
                                color = dividerColor,
                                start = Offset(size.width / 2f, 4f),
                                end = Offset(size.width / 2f, size.height - 4f),
                                strokeWidth = 1f,
                            )
                        }
                        .aeroDragSplitter(
                            orientation = Orientation.Horizontal,
                            onDrag = { delta -> onColumnResize(i, delta) },
                            onDragEnd = onColumnResizeEnd,
                        ),
                )
            }
        }
    }
}
