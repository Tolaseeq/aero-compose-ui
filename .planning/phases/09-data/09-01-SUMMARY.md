---
phase: 09-data
plan: "01"
subsystem: datatable-api
tags: [tdd, pure-logic, api-surface, data-table, scrollbar]
dependency_graph:
  requires: []
  provides: [AeroScrollBar(LazyListState), AeroTableColumn, AeroColumnWidth, SortDirection, SelectionMode, SortState, SelectionLogic, resolveColumnWidths]
  affects: [09-02-datatable-composable, 09-03-treeview]
tech_stack:
  added: []
  patterns: [pure-function-tdd, sealed-interface-width, set-any-selection-key]
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroDataTableTypes.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroTableColumn.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/SortState.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/SelectionLogic.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/ColumnWidth.kt
    - library/src/test/kotlin/com/mordred/aero/components/datatable/SortStateTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/datatable/SelectionLogicTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/datatable/ColumnWidthTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt
decisions:
  - "AeroScrollBar(LazyListState) added as Option A additive overload — keeps existing ScrollState callers unaffected; DataTable/TreeView use this to avoid AeroScrollArea (PITFALL-01)"
  - "Selection uses Set<Any> + caller key fn — not Set<Int> indices (PITFALL-04 locked at type level)"
  - "resolveColumnWidths uses pxPerDp: Float not Compose Density — pure JVM testable without Compose runtime"
  - "SortState uses Int? columnKey (column index) nullable to null when direction=None so no indicator shows"
metrics:
  duration: "~4 min"
  completed_date: "2026-06-18"
  tasks_completed: 5
  files_created: 8
  files_modified: 1
---

# Phase 9 Plan 01: DataTable API Surface + Pure Logic Summary

**One-liner:** Locked AeroDataTable/TreeView public API surface and all pure state machines (sort, selection, column-width) with 17 JUnit tests before any composable is written; added AeroScrollBar(LazyListState) additive overload (PITFALL-NEW-02).

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | AeroScrollBar(LazyListState) overload | 5ec76c7 | AeroScrollBar.kt (+22 lines) |
| 2 | Public column + table types | 7140ff2 | AeroDataTableTypes.kt, AeroTableColumn.kt |
| 3 | Pure SortState machine + 5 tests | ed225a1 | SortState.kt, SortStateTest.kt |
| 4 | Pure SelectionLogic (Ctrl/Shift) + 6 tests | 7f97d5e | SelectionLogic.kt, SelectionLogicTest.kt |
| 5 | Pure resolveColumnWidths + 6 tests | 3edac24 | ColumnWidth.kt, ColumnWidthTest.kt |

## Verification Gates Passed

- `./gradlew :library:compileKotlin` exits 0
- `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*"` all 17 tests green (5+6+6)
- `grep -rc "Set<Int>" library/src/main/kotlin/com/mordred/aero/components/datatable/` == 0 (PITFALL-04)
- `grep -c "fun AeroScrollBar(" AeroScrollBar.kt` == 2 (both overloads present)

## Decisions Made

1. **AeroScrollBar Option A (additive overload):** Added `AeroScrollBar(lazyListState: LazyListState)` alongside existing `AeroScrollBar(scrollState: ScrollState)`. Additive, no callers broken, consistent with v2.0 additive-only rule. Resolves PITFALL-NEW-02.

2. **Selection by Set<Any> locked at type level:** `computeShiftRange(displayedKeys: List<Any>, ...)` and all SelectionLogic fns use `Any` not `Int`. The plan-02 composable will receive `selectedKeys: Set<Any>` + `key: (T) -> Any`. PITFALL-04 resolved before any composable is written.

3. **pxPerDp: Float instead of Density:** `resolveColumnWidths` takes a plain Float density factor so it is pure-JVM testable with `pxPerDp = 1f` in tests (no Compose runtime dependency in test classpath). The composable in plan-02 will pass `LocalDensity.current.density`.

4. **SortState.columnKey: Int? (nullable):** When direction=None after cycling Desc->None, columnKey resets to null. This ensures no column shows a sort indicator when unsorted — matches "start=none, no indicator" requirement from CONTEXT.md.

## Deviations from Plan

None — plan executed exactly as written. All 5 tasks completed in order with code copied verbatim from plan where provided.

## Self-Check

All created files verified on disk. All 5 commits verified in git log.
