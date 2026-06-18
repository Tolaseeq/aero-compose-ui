---
phase: 09-data
plan: 03
subsystem: datatable/treeview
tags: [treeview, lazy-expand, virtualization, tdd, data-05, data-06]
dependency_graph:
  requires: [09-01]
  provides: [AeroTreeView, FlatNode, flattenTree, NodeState, toggleNode, AeroTreeNode]
  affects: []
tech_stack:
  added: []
  patterns: [flatten-and-replace, SnapshotStateMap-above-LazyColumn, derivedStateOf-expand-guard]
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/TreeFlatten.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/NodeState.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTreeNode.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroTreeView.kt
    - library/src/test/kotlin/com/mordred/aero/components/datatable/TreeFlattenTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/datatable/NodeStateTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt
decisions:
  - "flatten-and-replace pattern used: children are separate LazyColumn items, no AnimatedVisibility, fixed rowHeight=36.dp"
  - "SnapshotStateMap above LazyColumn guards childrenLoaded — PITFALL-05 locked"
  - "derivedStateOf used for expandedKeys to minimize recomputation on non-expand state changes"
  - "AeroScrollBar(lazyListState) overload from plan-01 used directly"
metrics:
  duration: "~5 min"
  completed_date: "2026-06-18"
  tasks: 4
  files: 7
---

# Phase 09 Plan 03: AeroTreeView Summary

AeroTreeView<T> lazy tree with function-model API — pure flattenTree + SnapshotStateMap-guarded once-only onExpand using toggleNode, virtualized via flatten-and-replace into an owned LazyColumn + AeroScrollBar.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Pure flattenTree + 6 unit tests (DATA-05) | a66cbcf | TreeFlatten.kt, TreeFlattenTest.kt |
| 2 | NodeState + toggleNode guard + 5 unit tests (DATA-06) | a23df22 | NodeState.kt, NodeStateTest.kt |
| 3 | AeroTreeNode internal composable | 89515b6 | AeroTreeNode.kt, AeroTableHeader.kt (fix) |
| 4 | AeroTreeView public composable | 5408c8a | AeroTreeView.kt |

## What Was Built

**TreeFlatten.kt** — `FlatNode<T>` data class (item, depth, key, isExpandable, isExpanded) + `flattenTree()` pure pre-order flatten producing depth-aware flat list for LazyColumn rendering. Collapsed nodes contribute only themselves; expanded nodes include children recursively at depth+1.

**NodeState.kt** — `NodeState(isExpanded, childrenLoaded)` + `ToggleResult(state, shouldFireExpand)` + `toggleNode(current: NodeState?)` pure transition function. First expand sets `childrenLoaded=true` and returns `shouldFireExpand=true`. All subsequent re-expands (after collapse or scroll-back) return `shouldFireExpand=false`. The DATA-06/PITFALL-05 guard is locked in pure logic before touching Compose.

**AeroTreeNode.kt** — Internal composable for a single row: depth indent Spacer, caret Box (animated via `animateFloatAsState` + `graphicsLayer { rotationZ }` 0→90°, 150ms, expandable nodes only), `nodeContent` slot. Row clickable fires `onNodeClick`; caret clickable fires `onExpandClick` — two independent gestures.

**AeroTreeView.kt** — Public `AeroTreeView<T>` composable. `mutableStateMapOf<Any, NodeState>()` lives above the LazyColumn (PITFALL-05). `derivedStateOf` computes `expandedKeys` from the map. `flattenTree` recomputes in `remember(expandedKeys, rootNodes)`. `handleExpand` calls `toggleNode` and conditionally fires `onExpand` on `shouldFireExpand` only. `LazyColumn` with `items(key = { it.key })` + `AeroScrollBar(lazyListState)`. Empty state shows `emptyContent` slot or default "Нет элементов" text.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing CaretUp import in AeroTableHeader.kt**
- **Found during:** Task 3 compileKotlin
- **Issue:** `AeroTableHeader.kt` (from plan-02, previously uncommitted) had `AeroIcons.CaretUp` usage without the required extension property import `com.mordred.aero.icons.internal.CaretUp`. This caused a compile error blocking Task 3.
- **Fix:** Added `import com.mordred.aero.icons.`internal`.CaretUp` to AeroTableHeader.kt imports.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt`
- **Commit:** 89515b6 (included in Task 3 commit)

**2. [Rule 3 - Blocking] AeroTreeNode required explicit CaretRight import**
- **Found during:** Task 3 first compile attempt
- **Issue:** Extension property `AeroIcons.CaretRight` requires explicit import of `com.mordred.aero.icons.internal.CaretRight` — confirmed from existing usage in `AeroContextMenu.kt`.
- **Fix:** Added explicit import to AeroTreeNode.kt.
- **Commit:** 89515b6

## Test Results

| Test class | Tests | Result |
|------------|-------|--------|
| TreeFlattenTest | 6 | GREEN |
| NodeStateTest | 5 | GREEN |
| com.mordred.aero.components.datatable.* (full suite) | all | GREEN |

## Verification Gates

- `./gradlew :library:compileKotlin` — PASSED
- `./gradlew :library:test --tests "com.mordred.aero.components.datatable.TreeFlattenTest"` — PASSED
- `./gradlew :library:test --tests "com.mordred.aero.components.datatable.NodeStateTest"` — PASSED
- `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*"` — PASSED
- `grep AeroScrollArea AeroTreeView.kt` — only in KDoc comment (no code usage), PASSED
- `grep AeroScrollArea AeroTreeNode.kt` — 0 hits, PASSED
- `grep detectDragGestures datatable/` — 0 hits across all files, PASSED

## Success Criteria

- DATA-05: AeroTreeView renders hierarchy with expand/collapse, CaretRight rotation (0→90°), depth indentation, expand-only indicator — DONE
- DATA-06: onExpand fires exactly once per node on first expand; no re-fire on collapse/re-expand/scroll-back (SnapshotStateMap above LazyColumn + toggleNode guard) — DONE
- Tree owns its LazyColumn + AeroScrollBar(lazyListState); no AeroScrollArea — DONE
- Tree logic (flattenTree, toggleNode) is pure + unit-tested before composable — DONE (11 tests total)
