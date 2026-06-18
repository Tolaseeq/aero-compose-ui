package com.mordred.aero.components.datatable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.containers.AeroScrollBar
import com.mordred.aero.components.datatable.internal.AeroTreeNode
import com.mordred.aero.components.datatable.internal.NodeState
import com.mordred.aero.components.datatable.internal.flattenTree
import com.mordred.aero.components.datatable.internal.toggleNode
import com.mordred.aero.theme.AeroTheme

/**
 * Lazy virtualized tree view with expand/collapse and animated caret indicators.
 *
 * ## Usage
 *
 * The tree is described by three pure functions over your data type [T]:
 * - [children] returns the direct children of a node (empty list for leaves).
 * - [isExpandable] returns `true` if the node should show a caret — can be `true`
 *   even before children are loaded (indicates "has children" before lazy fetch).
 * - [key] returns a stable, unique identity for each node (survives recomposition and scroll).
 *
 * The [nodeContent] slot renders the row body; the library owns the caret indicator,
 * depth indentation, and click handling.
 *
 * ## Expand behaviour (DATA-06 / PITFALL-05)
 *
 * [onExpand] fires **EXACTLY ONCE** per node on first expand. The expand state and
 * `childrenLoaded` guard live in a tree-level `SnapshotStateMap` **above** the `LazyColumn`
 * — scrolling a node off-screen and back does **NOT** re-fire [onExpand]. Collapse and
 * re-expand after the initial load also do not re-fire.
 *
 * ## Scroll (PITFALL-01)
 *
 * `AeroTreeView` owns its own `LazyColumn` + `AeroScrollBar(lazyListState)`. Do **NOT**
 * wrap it in the scroll-area container — that would give the inner `LazyColumn` an unbounded
 * height constraint and destroy virtualization.
 *
 * ## Selection
 *
 * The tree is expand-only — there is no built-in selection state. To highlight an active
 * node, render the highlight inside [nodeContent] and track the selection in your own state,
 * reacting to [onNodeClick].
 *
 * @param rootNodes Top-level nodes. Changing this list triggers a recomposition.
 * @param children Returns the direct children of a node on demand.
 * @param isExpandable Returns `true` if the node should show a caret indicator.
 * @param key Returns a stable, unique key for each node (must be consistent across recompositions).
 * @param nodeContent Composable slot rendered as the row body (right of caret + indent).
 * @param modifier Layout modifier applied to the outer [Box] container.
 * @param rowHeight Fixed height of every row. Defaults to 36.dp.
 * @param indentPerLevel Horizontal indent added per depth level. Defaults to 16.dp.
 * @param emptyContent Composable shown centered when [rootNodes] is empty.
 *   Pass `null` (default) to use the built-in "Нет элементов" label.
 * @param onExpand Called exactly once per node when it is first expanded.
 *   Use to load lazy children into your data model.
 * @param onNodeClick Called when the user taps the row body (not the caret).
 */
@Composable
public fun <T> AeroTreeView(
    rootNodes: List<T>,
    children: (T) -> List<T>,
    isExpandable: (T) -> Boolean,
    key: (T) -> Any,
    nodeContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 36.dp,
    indentPerLevel: Dp = 16.dp,
    emptyContent: (@Composable () -> Unit)? = null,
    onExpand: (T) -> Unit = {},
    onNodeClick: (T) -> Unit = {},
) {
    // CRITICAL: SnapshotStateMap lives ABOVE LazyColumn — survives item disposal on scroll-out (PITFALL-05)
    val expandStateMap = remember { mutableStateMapOf<Any, NodeState>() }
    val lazyListState = rememberLazyListState()

    // Derived set of expanded keys — only recomputes when expansion map changes (Open Question 2)
    val expandedKeys by remember {
        derivedStateOf {
            expandStateMap.entries
                .filter { it.value.isExpanded }
                .map { it.key }
                .toSet()
        }
    }

    // Flatten the tree to a linear list whenever expanded keys or root nodes change
    val flatNodes = remember(expandedKeys, rootNodes) {
        flattenTree(rootNodes, expandedKeys, children, isExpandable, key)
    }

    // Expand handler: pure toggleNode + once-only onExpand guard
    fun handleExpand(item: T) {
        val k = key(item)
        val result = toggleNode(expandStateMap[k])
        expandStateMap[k] = result.state
        if (result.shouldFireExpand) onExpand(item)   // EXACTLY ONCE per node
    }

    val colors = AeroTheme.colors

    Box(modifier) {
        if (flatNodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                emptyContent?.invoke()
                    ?: Text("Нет элементов", color = colors.labelText)
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp),
            ) {
                items(items = flatNodes, key = { it.key }) { flatNode ->
                    AeroTreeNode(
                        flatNode = flatNode,
                        rowHeight = rowHeight,
                        indentPerLevel = indentPerLevel,
                        nodeContent = nodeContent,
                        onExpandClick = { handleExpand(flatNode.item) },
                        onNodeClick = { onNodeClick(flatNode.item) },
                    )
                }
            }
            AeroScrollBar(
                lazyListState = lazyListState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}
