@file:OptIn(ExperimentalFoundationApi::class)

package com.mordred.aero.components.datatable.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.unit.Dp
import com.mordred.aero.components.datatable.AeroTableColumn
import com.mordred.aero.components.datatable.SelectionMode
import com.mordred.aero.theme.AeroTheme

/**
 * Internal selectable data row for [AeroDataTable].
 *
 * Renders four distinct visual states via locked tokens (PITFALL-10):
 * - normal: transparent
 * - hover: [colors.buttonHover]
 * - selected: [colors.borderSelected] @ 0.15f
 * - selected + hover: [colors.borderSelected] @ 0.15f composited over [colors.buttonHover]
 *
 * Ctrl/Shift click handlers are wired when [selectionMode] != None.
 * All three handlers (single, ctrl, shift) are always passed — the parent [AeroDataTable]
 * decides the semantics per mode (e.g. in Single mode ctrl/shift map to single-replace).
 *
 * @param item the data item for this row.
 * @param columns column specs (used for cell rendering + alignment).
 * @param columnWidthsDp resolved widths per column in Dp.
 * @param rowHeight fixed row height.
 * @param isSelected whether this row is currently in the selection set.
 * @param selectionMode none/single/multi; when None no click handlers are attached.
 * @param horizontalScrollState shared [ScrollState] for synchronized horizontal scrolling.
 * @param onSingleClick fired on a plain click (no modifiers).
 * @param onCtrlClick fired on a Ctrl+click.
 * @param onShiftClick fired on a Shift+click.
 */
@Composable
internal fun <T> AeroTableRow(
    item: T,
    columns: List<AeroTableColumn<T>>,
    columnWidthsDp: List<Dp>,
    rowHeight: Dp,
    isSelected: Boolean,
    selectionMode: SelectionMode,
    horizontalScrollState: ScrollState,
    onSingleClick: () -> Unit,
    onCtrlClick: () -> Unit,
    onShiftClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Four-state background — PITFALL-10: borderSelected@0.15f (NOT the AeroListItem primary token)
    val bg = when {
        isSelected && isHovered -> colors.borderSelected.copy(alpha = 0.15f)
            .compositeOver(colors.buttonHover)
        isSelected -> colors.borderSelected.copy(alpha = 0.15f)
        isHovered  -> colors.buttonHover
        else       -> Color.Transparent
    }

    val borderDefaultColor = colors.borderDefault.copy(alpha = 0.4f)

    val clickModifier = if (selectionMode == SelectionMode.None) {
        Modifier
    } else {
        Modifier
            .onClick(keyboardModifiers = { isCtrlPressed }, interactionSource = interactionSource) { onCtrlClick() }
            .onClick(keyboardModifiers = { isShiftPressed }, interactionSource = interactionSource) { onShiftClick() }
            .onClick(interactionSource = interactionSource) { onSingleClick() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(bg)
            .hoverable(interactionSource)
            .then(clickModifier)
            .drawBehind {
                // Thin bottom divider — Win7 list aesthetic
                drawLine(
                    color = borderDefaultColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .horizontalScroll(horizontalScrollState),
    ) {
        columns.forEachIndexed { i, col ->
            Box(
                modifier = Modifier
                    .width(columnWidthsDp[i])
                    .height(rowHeight),
                contentAlignment = alignmentFrom(col.alignment),
            ) {
                col.cell(item)
            }
        }
    }
}

/** Map [Alignment.Horizontal] to a Box [Alignment] (centered vertically). */
private fun alignmentFrom(horizontal: Alignment.Horizontal): Alignment =
    when (horizontal) {
        Alignment.Start              -> Alignment.CenterStart
        Alignment.End                -> Alignment.CenterEnd
        Alignment.CenterHorizontally -> Alignment.Center
        else                         -> Alignment.CenterStart
    }
