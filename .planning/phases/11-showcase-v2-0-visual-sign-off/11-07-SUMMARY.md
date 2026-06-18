---
phase: 11-showcase-v2-0-visual-sign-off
plan: 07
subsystem: ui
tags: [datatable, treeview, compose, kotlin, cell-padding, sort, resize]

# Dependency graph
requires:
  - phase: 09-data
    provides: AeroDataTable + AeroTreeView implementation
provides:
  - F2: 8dp horizontal padding in each AeroTableRow cell (no adjacent-value merge)
  - F4: full-cell clickable sort target on AeroTableHeader column cells
  - F5: whole-row expand toggle in AeroTreeNode for expandable nodes
  - F-RESIZE: upper-bound clamp on column widths (availableDp - othersMinDp) so no column can leave the screen
affects: [11-10-showcase-config, sign-off checklist items 1-3]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Column resize bounds: coerceIn(minDp, maxDp) where maxDp = availableDp - sum(other columns' minWidth)"
    - "Full-cell sort target: clickable on the outer cell Box, not the inner label Row"
    - "Row-level tree expand: call onExpandClick() from row clickable when flatNode.isExpandable"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableRow.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTreeNode.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroDataTable.kt

key-decisions:
  - "F4: clickable moved from inner label Row to the full outer cell Box — resize splitter at CenterEnd is its own Box and consumes pointer events independently, so sorting and resize coexist without conflict"
  - "F5: calling onExpandClick() from the row-level click is safe because onExpandClick routes through handleExpand() -> toggleNode() (the PITFALL-05 once-only guard in AeroTreeView), never directly to the public onExpand callback"
  - "F-RESIZE: left over-shrink readability (Name column too narrow) is a showcase config concern addressed in 11-10 by passing a larger minWidth; the library-side fix here guarantees the minWidth floor is always honored as both lower bound and contribution to other-columns' floor in the max calc"

patterns-established:
  - "Column resize both-bounds: coerceIn(min, max) with max = available - others-min"

requirements-completed: [SHW-07, SHW-10]

# Metrics
duration: 3min
completed: 2026-06-18
---

# Phase 11 Plan 07: DataTable/TreeView Bug-Fix Summary

**Four library-level defects fixed: 8dp cell padding eliminates adjacent-value merge (F2), full-cell header click enables sort anywhere (F4), row-level click toggles expand on tree nodes (F5), and per-column coerceIn bounds prevent off-screen column drag (F-RESIZE)**

## Performance

- **Duration:** 3 min
- **Started:** 2026-06-18T17:32:20Z
- **Completed:** 2026-06-18T17:35:29Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- F2: `AeroTableRow.kt` cells now have `padding(horizontal = 8.dp)`, matching the header label's existing 8dp inset — adjacent values like `40084` and `2026-03-26` are no longer merged into `400842026-03-26`
- F4: `AeroTableHeader.kt` moves `.clickable { onSortClick(i) }` from the inner label `Row` to the full-cell `Box`, so clicking anywhere in a header cell (not only the text) triggers sort; resize splitter remains on its own inner `Box` at `CenterEnd` and consumes pointer events independently
- F5: `AeroTreeNode.kt` row-level `clickable` now calls `onExpandClick()` when `flatNode.isExpandable`, giving the whole node row the expand/collapse toggle; chevron's own `clickable` retained (harmless duplicate — routed through PITFALL-05 `toggleNode` guard)
- F-RESIZE: `AeroDataTable.kt` width resolution now computes `maxDp = (availableDp - othersMinDp).coerceAtLeast(minDp)` and uses `coerceIn(minDp, maxDp)` instead of `coerceAtLeast(minDp)`, bounding resize in both directions; sign-off item 3 now verifiable

## Task Commits

Each task was committed atomically:

1. **Task 1: F2 cell separation + F4 full-cell header sort** - `5b59481` (fix)
2. **Task 2: F5 whole-row TreeView expand toggle** - `8531690` (fix)
3. **Task 3: F-RESIZE column width upper bound + table width clamp** - `cee9016` (fix)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableRow.kt` — added `import androidx.compose.foundation.layout.padding`, `import androidx.compose.ui.unit.dp`; added `.padding(horizontal = 8.dp)` to each cell `Box` modifier (F2)
- `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt` — moved `.then(if (col.sortKey != null) Modifier.clickable { onSortClick(i) } else Modifier)` from inner label `Row` to the outer cell `Box`; inner `Row` now carries only `padding` and alignment (F4)
- `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTreeNode.kt` — row-level `clickable` block extended to call `onExpandClick()` when `flatNode.isExpandable` (F5)
- `library/src/main/kotlin/com/mordred/aero/components/datatable/AeroDataTable.kt` — added `val availableDp`, `val othersMinDp`, `val maxDp` variables; changed `coerceAtLeast(minDp)` to `coerceIn(minDp, maxDp)` in the `widthsDp` `remember` block (F-RESIZE)

## Decisions Made

- F4: Clickable placed on the outer cell `Box` rather than the inner label `Row`. The resize splitter is its own `Box` at `CenterEnd` inside the cell and owns its pointer input via `aeroDragSplitter`, so it intercepts drag events before they propagate to the cell click — sort and resize targets do not interfere.
- F5: `onExpandClick` is the `handleExpand()` lambda from `AeroTreeView`, which calls `toggleNode()` (the PITFALL-05 once-only guard). Calling it twice per click (row + chevron) is safe — `toggleNode` transitions the state machine correctly. The public `onExpand` callback fires at most once per node per the guard logic.
- F-RESIZE left over-shrink: deferred to 11-10 (showcase config — pass larger `minWidth` on the Name column). The library now correctly honors whatever `minWidth` the caller sets as both the lower bound and each column's contribution to the others-floor in the upper-bound calculation.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

- The `./gradlew :library:compileKotlin -x generateIcons` command in the plan fails because `generateIcons` is not a task in this Gradle project. Ran `./gradlew :library:compileKotlin` without the exclusion flag — compilation succeeded. Documented as non-issue (plan's task exclusion flag was incorrect but outcome is identical).

## Next Phase Readiness

- F2/F4/F5/F-RESIZE are resolved at the library level — sign-off items 1 (cell readability), 2 (interaction targets), and 3 (column resize bounds) are now verifiable
- PITFALL-05 once-only `onExpand` and PITFALL-10 four-state selection remain intact (no token changes)
- Next: 11-08 (F6 date formatter), 11-09 (accordion/color), 11-10 (showcase minWidth config) continue gap closure

---
*Phase: 11-showcase-v2-0-visual-sign-off*
*Completed: 2026-06-18*
