---
phase: 14-panelgroup-recompose-fix-v2-0-3-release
plan: 01
subsystem: ui
tags: [compose, panelgroup, sideeffect, snapshot-state, recompose-fix, layout]

# Dependency graph
requires:
  - phase: 13-aeropanelgroup
    provides: AeroPanelGroupImpl horizontal + vertical core; expandedState, sizePx, isExpanded() helper
provides:
  - SideEffect-deferred expandedState sync eliminating write-during-composition violation
  - Size-math expandedArr derived from isExpanded() each composition (not stale mirror)
affects:
  - 14-02 (showcase repro checkpoint depends on this fix being in place)
  - 14-03 (version bump plan)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "SideEffect for post-commit state mirror sync (expandedState <- isExpanded() after commit)"
    - "Read authoritative expansion source (isExpanded()) for structural decisions; read mirror (expandedState) only for animation targets"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt

key-decisions:
  - "RCMP-02+03: SideEffect-deferred expandedState sync + isExpanded()-derived expandedArr are both required together; neither alone is sufficient (RESEARCH Pitfall 1)"
  - "expandedState.toBooleanArray() at the BoxWithConstraints size-math site (was line 346) removed; remaining .toBooleanArray() calls in onToggle/onDragBetween event-handler lambdas are correct (they run at event time, not during composition)"
  - "Seed block (lines 297-320) left exactly as-is per RESEARCH Pitfall 4 — fires only on structure change, safe as-is"
  - "SideEffect body contains only expandedState sync; onLayoutChange NOT added to SideEffect (REG-01)"

patterns-established:
  - "SideEffect pattern for state mirror: identical to AeroSidebar.kt:79 precedent — defer write-back of a composition-derived value into a SnapshotStateList to post-commit via SideEffect"
  - "isExpanded() is the authoritative expansion source for structural decisions (child count, size math); expandedState is the mirror for animation targets only"

requirements-completed: [RCMP-01, RCMP-02, RCMP-03, REG-01, REG-02]

# Metrics
duration: 3min
completed: 2026-06-26
---

# Phase 14 Plan 01: PanelGroup Recompose Fix Summary

**Write-during-composition eliminated in AeroPanelGroupImpl: expandedState sync moved to SideEffect, size-math expandedArr derived from isExpanded() — 12 PanelGroupLogicTest JVM tests GREEN**

## Performance

- **Duration:** 3 min
- **Started:** 2026-06-26T08:18:25Z
- **Completed:** 2026-06-26T08:21:25Z
- **Tasks:** 3 (Task 3 human-verify checkpoint — APPROVED)
- **Files modified:** 1

## Accomplishments

- EDIT A: Added `import androidx.compose.runtime.SideEffect` to `AeroPanelGroup.kt` (Pitfall 6 guard)
- EDIT B: Wrapped the in-composition expandedState sync loop (was lines 331-335) in `SideEffect { ... }` so it runs post-commit, never mid-composition; eliminates the write-during-composition loop that caused horizontal-controlled N→N×N header-strip duplication (RCMP-03)
- EDIT C: Replaced `expandedState.toBooleanArray()` at the BoxWithConstraints size-math site with `BooleanArray(sections.size) { i -> isExpanded(sections[i]) }` — size-math now reads the authoritative expansion source each composition (RCMP-02)
- 12 PanelGroupLogicTest JVM tests GREEN (BUILD SUCCESSFUL, 0 failures); Compose 1.7.3, zero new deps (REG-02)
- No build files, dependency files, or other source files touched

## Task Commits

1. **Task 1: Move expandedState sync into SideEffect and derive expandedArr from isExpanded()** - `fbba375` (fix)
2. **Task 2: Compile and run PanelGroupLogicTest JVM gate** - no new commit (verification only; code compiled as part of test run confirming Task 1 commit)
3. **Task 3: Human visual sign-off (checkpoint:human-verify)** - APPROVED. Vertical + horizontal drag, collapse/expand, and proportions unchanged vs v2.0.2; no duplication during drag (REG-01 + RCMP-01 first signal confirmed).

**Plan metadata:** `a55d904` (docs: complete plan)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` - Three surgical edits: SideEffect import, SideEffect-wrapped sync loop, isExpanded()-derived expandedArr for size-math

## Decisions Made

- Both EDIT B and EDIT C are required together; neither alone is sufficient (RESEARCH Pitfall 1). Moving sync to SideEffect without fixing expandedArr leaves size-math reading a stale mirror; fixing expandedArr without moving sync still has the composition-phase write.
- The remaining `expandedState.toBooleanArray()` calls in `onToggle` and `onDragBetween` event-handler lambdas are correct — they run at event time (not during composition), reading the current snapshot of expandedState. The plan's acceptance criterion of "0 occurrences" was written assuming line 346 was the only occurrence; only the size-math occurrence was removed per the plan's surgical-change constraint ("exactly three edits").
- `onLayoutChange` not added to SideEffect body — stays drag-end + toggle only (REG-01).

## Deviations from Plan

### Acceptance Criteria Interpretation

**1. [Observation] expandedState.toBooleanArray() count is 3, not 0**
- **Found during:** Task 2 verification
- **Issue:** The plan's acceptance criterion says `grep -c "expandedState.toBooleanArray"` returns `0`. After the three surgical edits, 3 occurrences remain — in `onToggle` (lines 366, 387) and `onDragBetween` (line 405). These are event-handler lambdas that execute at user-interaction time (not during composition).
- **Resolution:** The critical size-math occurrence (was line 346) is removed. The remaining occurrences are correct event-time reads and were not part of the "exactly three edits" the plan specified. The plan's must-have truths and RESEARCH goals are all satisfied. This is an oversight in the acceptance criterion (it assumed line 346 was the only occurrence).
- **Verdict:** No fix needed; the three specified edits are complete and correct.

---

**Total deviations:** 1 clarification (no code changes needed)
**Impact on plan:** Zero scope creep. The three surgical edits are in place exactly as specified.

## Issues Encountered

None — all three edits applied cleanly; compilation succeeded on first run; all 12 tests GREEN immediately.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Fix is in place and human-verified (Task 3 checkpoint APPROVED): vertical + horizontal drag/collapse/expand and proportions unchanged vs v2.0.2; no duplication during drag.
- Plan 14-02 adds the horizontal controlled showcase repro (RCMP-04) with the live-counter recompose-during-drag guard.
- Plan 14-03 bumps version to 2.0.3 and releases on JitPack.

---
*Phase: 14-panelgroup-recompose-fix-v2-0-3-release*
*Completed: 2026-06-26*

## Self-Check: PASSED

- FOUND: `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt`
- FOUND: `.planning/phases/14-panelgroup-recompose-fix-v2-0-3-release/14-01-SUMMARY.md`
- FOUND: commit fbba375
