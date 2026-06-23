---
phase: 13-aeropanelgroup
plan: 02
subsystem: ui
tags: [compose-desktop, panel-group, tdd, pure-logic, unit-tests]

# Dependency graph
requires:
  - phase: 13-aeropanelgroup
    plan: 01
    provides: spike findings (header reservation, drag delta scaling, availableForExpanded guard)
provides:
  - PanelDistribution.kt pure JVM functions: clampPanelDividerPx, computeAvailablePx,
    activeDividerCount, distributePx, shareTransferOnCollapse, shareTransferOnExpand,
    lastExpandedFraction, restoreFromFraction
  - PanelGroupLogicTest.kt: 12 GREEN tests covering all six 13-VALIDATION.md named tests
    plus six supporting tests
affects: [13-03-plan, 13-04-plan, 13-05-plan]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure-JVM function file with zero Compose imports, unit-testable on JVM without Compose runtime"
    - "PITFALL-B coerceAtLeast guard ported from SplitClamp.kt into N-section clampPanelDividerPx"
    - "Last-section remainder in distributePx absorbs float drift (PNL-PITFALL-02/11)"
    - "Last-neighbor remainder in shareTransferOnCollapse conserves total px (PNL-PITFALL-05)"
    - "Fraction-based collapse restore: lastExpandedFraction / restoreFromFraction (PNL-PITFALL-06)"
    - "zipWithNext active-divider count guards against phantom dividers in E/C/E patterns (PNL-PITFALL-07)"
    - "all-collapsed early return in computeAvailablePx gates the PNL-15 invariant"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt
    - library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt
  modified: []

key-decisions:
  - "computeAvailablePx reserves headerPx for ALL sections (not just collapsed) per spike finding 1; plus explicit all-collapsed early-return for PNL-15 invariant"
  - "distributePx uses last-expanded-index remainder, not weight(1f) in Compose, to absorb float drift at the pure-logic layer"
  - "shareTransferOnCollapse/Expand use proportional redistribution with last-neighbor remainder to satisfy PNL-PITFALL-05 conservation"

# Metrics
duration: ~4 minutes
completed: 2026-06-23
---

# Phase 13 Plan 02: Pure-Logic TDD Foundation Summary

**Pure-JVM `PanelDistribution.kt` (8 functions, zero Compose imports) with 12 GREEN `kotlin.test` unit tests — all six 13-VALIDATION.md named tests pass, PNL-PITFALL-04/05/06/07 and PITFALL-A/B classes defused at the pure-logic layer; plans 13-03/04/05 unblocked**

## Performance

- **Duration:** ~4 minutes
- **Completed:** 2026-06-23
- **Tasks:** 2 (TDD RED → GREEN, both auto)
- **Files created:** 2

## Accomplishments

- TDD RED: `PanelGroupLogicTest.kt` written with 12 tests calling non-existent functions — compile failure confirmed.
- TDD GREEN: `PanelDistribution.kt` implemented with 8 pure-JVM internal functions — full suite passes, full library test suite still passes.
- All six 13-VALIDATION.md named tests GREEN individually and as a suite.
- Zero Compose imports in `PanelDistribution.kt` confirmed by grep count 0.

## Task Commits

1. **Task 1: RED — PanelGroupLogicTest.kt** - `460c1ee` (test)
2. **Task 2: GREEN — PanelDistribution.kt** - `7b2f84d` (feat)

## Pure Function Signatures (for plans 13-03/04/05 to call verbatim)

```kotlin
// clampPanelDividerPx — N-section drag clamp with PITFALL-B guard
internal fun clampPanelDividerPx(
    aboveSizePx: Float, deltaPx: Float,
    minAbovePx: Float, minBelowPx: Float,
    totalBudgetPx: Float,
): Float

// computeAvailablePx — header reserved for ALL sections; dividers via zipWithNext
internal fun computeAvailablePx(
    totalPx: Float, expanded: BooleanArray,
    headerPx: Float, dividerPx: Float,
): Float

// activeDividerCount — zipWithNext, not expandedCount-1
internal fun activeDividerCount(expanded: BooleanArray): Int

// distributePx — collapsed→headerPx; expanded→proportional; last-section remainder
internal fun distributePx(
    sizePx: FloatArray, expanded: BooleanArray,
    totalPx: Float, headerPx: Float, dividerPx: Float,
): FloatArray

// shareTransferOnCollapse — proportional redistribution; last-neighbor absorbs remainder
internal fun shareTransferOnCollapse(
    sizePx: FloatArray, expanded: BooleanArray, index: Int,
): FloatArray

// shareTransferOnExpand — proportional take from donors, floor-clamped
internal fun shareTransferOnExpand(
    sizePx: FloatArray, expanded: BooleanArray, index: Int, restorePx: Float,
): FloatArray

// lastExpandedFraction — store fraction, not absolute px (PNL-PITFALL-06)
internal fun lastExpandedFraction(sizePx_i: Float, availableForExpanded: Float): Float

// restoreFromFraction — result always <= currentAvailableForExpanded for fraction<=1
internal fun restoreFromFraction(fraction: Float, currentAvailableForExpanded: Float): Float
```

## Green Test Evidence

```
./gradlew :library:test --tests "*.PanelGroupLogicTest" → BUILD SUCCESSFUL
./gradlew :library:test → BUILD SUCCESSFUL (full suite)

Individual named tests (all BUILD SUCCESSFUL):
  *.PanelGroupLogicTest.clampPanelDividerPxInvertedRangeNoThrow
  *.PanelGroupLogicTest.shareTransferOnCollapseConservesPx
  *.PanelGroupLogicTest.distributePxWindowResizePreservesRatios
  *.PanelGroupLogicTest.activeDividerCountEECEGivesOne
  *.PanelGroupLogicTest.restoreAfterShrinkDoesNotExceedAvailable
  *.PanelGroupLogicTest.allCollapsedAvailableIsZero
```

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt` — 245 lines, 8 pure-JVM internal functions, zero Compose imports
- `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt` — 12 unit tests: 6 named (13-VALIDATION.md), 6 supporting

## Decisions Made

**Header reservation formula:** `computeAvailablePx` subtracts `expanded.size * headerPx` (all sections, not just collapsed — spike finding 1). Adds explicit `if (expanded.none { it }) return 0f` guard so the PNL-15 invariant ("all-collapsed → available == 0") holds without contradicting the all-sections-header formula.

**Float drift:** `distributePx` assigns the last expanded section the remainder (`availableForExpanded - assigned`) rather than a proportional share, so Σ expanded heights == `availableForExpanded` exactly at the pure-logic layer (PNL-PITFALL-02 / PNL-PITFALL-11).

**Share conservation:** `shareTransferOnCollapse` applies the same last-index remainder trick to guarantee `Σ transferred == releasedShare` exactly (PNL-PITFALL-05).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] PLAN formula for `computeAvailablePx` contradicts PNL-15 named test**
- **Found during:** Task 2 (first test run)
- **Issue:** The PLAN Task 2 action gave `collapsed * headerPx` in the formula (only collapsed sections). The critical inputs and spike finding 1 require `sectionCount * headerPx` (all sections). Using all-sections-header reservation, `computeAvailablePx` returned 792 when all-collapsed (900 - 3*36 = 792), but `allCollapsedAvailableIsZero` asserts available <= 0f.
- **Fix:** Used the spike-finding formula (`sectionCount * headerPx`), and added `if (expanded.none { it }) return 0f` early-return to gate PNL-15 cleanly. The early return is semantically correct: when no sections are expanded, the budget for expanded content is zero by definition.
- **Files modified:** `PanelDistribution.kt`
- **Commit:** `7b2f84d`

## Issues Encountered

One test failure on first GREEN run (`allCollapsedAvailableIsZero`) — resolved with a single early-return guard. All other tests passed on first run.

## User Setup Required

None.

## Next Phase Readiness

- Plan 13-03 (layout skeleton) may begin: `BoxWithConstraints`, `mutableStateListOf` state, `key(section.id)` render loop. Pure-logic functions are the contract; call them verbatim.
- Plan 13-04 (drag resize) must apply drag-animation-disable finding from 13-01 (`isDragging` + `snap()`) and call `clampPanelDividerPx` from `PanelDistribution.kt`.
- Plan 13-05 (cleanup) must delete `PanelGroupSpikeSection.kt` and its `ShowcaseApp.kt` wiring.

## Self-Check

### File existence

- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt` — FOUND
- `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt` — FOUND

### Commit existence

- `460c1ee` (RED) — FOUND
- `7b2f84d` (GREEN) — FOUND

## Self-Check: PASSED

---
*Phase: 13-aeropanelgroup*
*Completed: 2026-06-23*
