package com.mordred.aero.components.datatable.internal

/** One row in the flattened tree, depth-aware for indentation. */
internal data class FlatNode<T>(
    val item: T,
    val depth: Int,
    val key: Any,
    val isExpandable: Boolean,
    val isExpanded: Boolean,
)

/** Pure pre-order flatten. Expanded nodes include their children at depth+1. */
internal fun <T> flattenTree(
    nodes: List<T>,
    expandedKeys: Set<Any>,
    childrenFn: (T) -> List<T>,
    isExpandableFn: (T) -> Boolean,
    keyFn: (T) -> Any,
    depth: Int = 0,
): List<FlatNode<T>> = buildList {
    for (node in nodes) {
        val k = keyFn(node)
        val expanded = k in expandedKeys
        add(FlatNode(node, depth, k, isExpandableFn(node), expanded))
        if (expanded) {
            addAll(flattenTree(childrenFn(node), expandedKeys, childrenFn, isExpandableFn, keyFn, depth + 1))
        }
    }
}
