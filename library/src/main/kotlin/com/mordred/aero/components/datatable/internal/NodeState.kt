package com.mordred.aero.components.datatable.internal

/** Per-node expand state. Lives in a tree-level SnapshotStateMap ABOVE the LazyColumn. */
internal data class NodeState(
    val isExpanded: Boolean = false,
    val childrenLoaded: Boolean = false,
)

/** Result of a toggle: the next state + whether onExpand should fire this time. */
internal data class ToggleResult(val state: NodeState, val shouldFireExpand: Boolean)

/**
 * Pure expand/collapse transition with once-only load guard (DATA-06 / PITFALL-05).
 * First expand -> childrenLoaded becomes true and shouldFireExpand = true.
 * Collapse -> keeps childrenLoaded; shouldFireExpand = false.
 * Re-expand after load -> shouldFireExpand = false (never re-fires).
 */
internal fun toggleNode(current: NodeState?): ToggleResult {
    val state = current ?: NodeState()
    val nowExpanded = !state.isExpanded
    val fire = nowExpanded && !state.childrenLoaded
    val next = state.copy(
        isExpanded = nowExpanded,
        childrenLoaded = state.childrenLoaded || fire,
    )
    return ToggleResult(next, fire)
}
