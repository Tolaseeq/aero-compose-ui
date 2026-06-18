---
phase: 09-data
plan: "02"
subsystem: datatable-composable
tags: [compose-desktop, datatable, virtualization, selection, column-resize, glass-header, aero]

requires:
  - phase: 09-data-01
    provides: [AeroScrollBar(LazyListState), AeroTableColumn, SortState, SelectionLogic, resolveColumnWidths]
  - phase: 07-shared-internal-primitives
    provides: [aeroDragSplitter(Orientation.Horizontal), glassPanel]

provides:
  - "public fun <T> AeroDataTable — virtualized table with owned LazyColumn+AeroScrollBar"
  - "internal AeroTableRow — four-state selectable row (borderSelected, buttonHover, Ctrl/Shift click)"
  - "internal AeroTableHeader — glass header with animateFloatAsState caret + aeroDragSplitter resize"

affects: [09-03-treeview, 11-showcase-DataSection]

tech-stack:
  added: []
  patterns:
    - "BoxWithConstraints + resolveColumnWidths(columns, availableWidthPx) for single-pass width compute per layout change"
    - "mutableStateMapOf resize overrides: accumulated px deltas applied over base widths, clamped to minWidth"
    - "Hybrid sort: uncontrolled (self-sort via sortKey) vs controlled (onSortChange) — both branches in KDoc"
    - "Private shiftAnchorKey via remember — not in public API; updates on single/Ctrl clicks only"
    - "Shared horizontalScrollState: data rows enabled=true, header enabled=false (header follows body)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableRow.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroDataTable.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/datatable/AeroTreeView.kt (KDoc fix — grep gate)

key-decisions:
  - "KDoc strings must not mention forbidden token names (AeroScrollArea, detectDragGestures, stickyHeader, primary.copy(0.2f)) to pass package-level grep gates — use paraphrase or context-free descriptions"
  - "icons are extension properties in package com.mordred.aero.icons.`internal` — require explicit import (e.g. import com.mordred.aero.icons.`internal`.CaretUp)"
  - "AeroTableHeader: horizontal scroll header=enabled=false; body rows=enabled=true — header tracks body without eating gestures"
  - "AeroDataTable resize state uses mutableStateMapOf<Int, Float> for per-column px override accumulated from drag deltas"

requirements-completed: [DATA-01, DATA-02, DATA-03, DATA-04]

duration: ~10min
completed: "2026-06-18"
---

# Phase 9 Plan 02: AeroDataTable Composable Summary

**Public AeroDataTable<T> composable with virtualized LazyColumn+AeroScrollBar, Win7 Aero glass header, three-position sort with animated caret, Ctrl/Shift multi-selection by Set<Any> key, and drag-resizable columns via aeroDragSplitter**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-06-18T12:48:40Z
- **Completed:** 2026-06-18T12:58:40Z
- **Tasks:** 3
- **Files created:** 3 (AeroTableRow, AeroTableHeader, AeroDataTable)
- **Files modified:** 1 (AeroTreeView.kt KDoc fix)

## Accomplishments

- `AeroTableRow` renders four distinct visual states (normal/hover/selected/selected+hover) with locked `borderSelected@0.15f` + `buttonHover` composite (PITFALL-10). Ctrl/Shift click via `Modifier.onClick(keyboardModifiers)` with `@OptIn(ExperimentalFoundationApi::class)`.
- `AeroTableHeader` provides a full-width glass gradient strip (`glassPanel(0.dp)`) with per-column sort carets (animateFloatAsState rotationZ, active-only alpha) and `aeroDragSplitter(Orientation.Horizontal)` resize zones. Header scroll is `enabled=false` so it tracks the body without competing for gestures.
- `AeroDataTable` owns its own `LazyColumn(state=lazyListState)` + `AeroScrollBar(lazyListState)` — no AeroScrollArea wrapping (PITFALL-01). Hybrid sort documented in KDoc with both branches preserved. `BoxWithConstraints` drives single-pass column width resolution; `mutableStateMapOf` accumulates per-column drag resize overrides clamped to `minWidth`.

## Task Commits

1. **Task 1: AeroTableRow** - `633ddbd` (feat)
2. **Task 2: AeroTableHeader** - `ef10f65` (feat) — file introduced in `89515b6`, comment fix in `ef10f65`
3. **Task 3: AeroDataTable** - `b2a9cf9` (feat)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableRow.kt` — Four-state selectable row, Ctrl/Shift click, shared horizontal scroll, drawBehind bottom divider
- `library/src/main/kotlin/com/mordred/aero/components/datatable/internal/AeroTableHeader.kt` — Glass header, animateFloatAsState caret, aeroDragSplitter per column
- `library/src/main/kotlin/com/mordred/aero/components/datatable/AeroDataTable.kt` — Public composable, 249 lines, hybrid sort, selection, resize wiring
- `library/src/main/kotlin/com/mordred/aero/components/datatable/AeroTreeView.kt` — KDoc deviation fix (2 lines)

## Decisions Made

1. **Icon import pattern:** Extension properties on `AeroIcons` live in `com.mordred.aero.icons.\`internal\`` and need explicit named imports (e.g. `import com.mordred.aero.icons.\`internal\`.CaretUp`). The object `AeroIcons` alone does not expose them.
2. **KDoc grep safety:** Acceptance criteria use `grep -c` across the whole package. Forbidden tokens in KDoc comments (like `AeroScrollArea`, `stickyHeader`) trigger false matches. KDoc must use paraphrases instead.
3. **Resize via mutableStateMapOf:** Per-column drag override stored as accumulated px Float in a `SnapshotStateMap<Int, Float>`. Applied additively over base `resolveColumnWidths` result, clamped to `minWidth`. Recompose key includes `resizeOverridesPx.toMap()` to react to map changes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Icon import from backtick-escaped `internal` package**
- **Found during:** Task 2 (AeroTableHeader compilation)
- **Issue:** `AeroIcons.CaretUp` compiled to `Unresolved reference` — the extension property lives in `com.mordred.aero.icons.\`internal\`.CaretUp` and requires an explicit star or named import
- **Fix:** Added `import com.mordred.aero.icons.\`internal\`.CaretUp` (same pattern used in existing `AeroTreeNode.kt` and `AeroNumberSpinner.kt`)
- **Files modified:** AeroTableHeader.kt
- **Verification:** `./gradlew :library:compileKotlin` exits 0
- **Committed in:** ef10f65

**2. [Rule 3 - Blocking] Removed `import androidx.compose.foundation.layout.weight` (internal API)**
- **Found during:** Task 3 (AeroDataTable compilation)
- **Issue:** `Cannot access val RowColumnParentData?.weight: it is internal in file` — the `weight` extension is accessed via ColumnScope, not by direct import
- **Fix:** Removed the erroneous import; `Modifier.weight(1f)` accessed within `Column` scope naturally
- **Files modified:** AeroDataTable.kt
- **Verification:** `./gradlew :library:compileKotlin` exits 0
- **Committed in:** b2a9cf9

**3. [Rule 3 - Blocking] KDoc grep gate fixes (comments mentioning forbidden tokens)**
- **Found during:** Task 2 + Task 3 acceptance criteria checks
- **Issue:** KDoc comments in AeroTableHeader (stickyHeader, detectDragGestures), AeroDataTable (AeroScrollArea, stickyHeader), and AeroTreeView (AeroScrollArea) triggered package-level grep gates
- **Fix:** Rephrased all KDoc references to forbidden tokens using paraphrases ("LazyColumn header slot", "scroll-area container", "raw pointer gestures")
- **Files modified:** AeroTableHeader.kt, AeroDataTable.kt, AeroTreeView.kt
- **Verification:** All `grep -rc` acceptance criteria return 0
- **Committed in:** ef10f65, b2a9cf9

---

**Total deviations:** 3 auto-fixed (3 blocking)
**Impact on plan:** All three deviations were mechanical compile/test fixes. No scope creep, no architectural changes.

## Issues Encountered

None beyond the auto-fixed deviations above.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- `AeroDataTable<T>` is ready for use; all DATA-01..04 requirements met
- Plan-03 (AeroTreeView) was partially pre-executed — `AeroTreeView.kt`, `AeroTreeNode.kt`, `flattenTree`, `NodeState`, plan-03 test files already committed before this plan-02 execution completed. The plan-03 SUMMARY.md still needs to be created.
- Phase 11 showcase wiring (`DataSection`) can consume `AeroDataTable` directly

---
*Phase: 09-data*
*Completed: 2026-06-18*
