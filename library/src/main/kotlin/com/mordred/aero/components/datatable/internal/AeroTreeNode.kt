package com.mordred.aero.components.datatable.internal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.theme.AeroTheme

/** Fixed hit-area width for the caret icon + surrounding clickable zone. */
private val CaretAreaWidth = 24.dp

/**
 * Internal composable for a single tree row.
 *
 * Renders depth-based indentation, an animated caret for expandable nodes
 * (AeroIcons.CaretRight rotated 0→90 deg via graphicsLayer), and the caller's
 * [nodeContent] slot. Caret click and node click are independent — tapping the caret
 * toggles expand/collapse; tapping the row body fires [onNodeClick].
 *
 * No AnimatedVisibility wrapping children — children are separate LazyColumn items
 * via the flatten-and-replace pattern (PITFALL-NEW-01). Row height is fixed = [rowHeight].
 */
@Composable
internal fun <T> AeroTreeNode(
    flatNode: FlatNode<T>,
    rowHeight: Dp,
    indentPerLevel: Dp,
    nodeContent: @Composable (T) -> Unit,
    onExpandClick: () -> Unit,
    onNodeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val rot by animateFloatAsState(
        targetValue = if (flatNode.isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "treeCaret",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable { onNodeClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Depth indentation
        Spacer(Modifier.width(indentPerLevel * flatNode.depth))

        // Caret area — fixed width; clickable only when expandable
        Box(
            modifier = Modifier
                .width(CaretAreaWidth)
                .height(rowHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (flatNode.isExpandable) {
                Icon(
                    imageVector = AeroIcons.CaretRight,
                    contentDescription = if (flatNode.isExpanded) "Collapse" else "Expand",
                    tint = colors.onSurface,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rot }
                        .clickable { onExpandClick() },
                )
            } else {
                Spacer(Modifier.width(CaretAreaWidth))
            }
        }

        // Node content slot
        nodeContent(flatNode.item)
    }
}
