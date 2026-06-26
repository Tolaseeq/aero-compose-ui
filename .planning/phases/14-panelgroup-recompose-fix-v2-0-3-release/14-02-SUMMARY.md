---
phase: 14-panelgroup-recompose-fix-v2-0-3-release
plan: 02
subsystem: ui
tags: [compose, panelgroup, recompose, regression-demo, showcase, kotlin]

# Dependency graph
requires:
  - phase: 14-panelgroup-recompose-fix-v2-0-3-release plan 01
    provides: SideEffect-deferred expandedState sync + isExpanded()-derived expandedArr fix (RCMP-02/03)
provides:
  - Permanent labeled horizontal CONTROLLED AeroPanelGroup repro in LayoutSection.kt
  - RCMP-04 recompose-during-drag regression guard demo (LaunchedEffect-ticked live counter)
  - Manual visual verification surface for RCMP-01 (N-not-N*N) with the plan-01 fix landed
affects: [14-panelgroup-recompose-fix-v2-0-3-release plan 03, future AeroPanelGroup changes]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Permanent showcase regression demo: LaunchedEffect-ticked mutableStateOf in one section's content drives recompose independently during divider drag"
    - "Controlled AeroPanelGroup repro pattern: expandedKeys + onExpandedChange with dedicated rcmpExpandedKeys state"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt

key-decisions:
  - "Used rcmpExpandedKeys (not expandedKeys) as the controlled state variable name to avoid shadowing"
  - "Counter cadence: 32ms (~30fps) — fast enough to drive visible recompose during manual divider drag (Claude's Discretion per CONTEXT.md)"
  - "Section titles: Live / Static A / Static B (per plan action block, not the research example's Left/Center/Right)"
  - "bodySmall typography token confirmed present in AeroTypography — used for caption as specified"

patterns-established:
  - "RCMP-04 regression demo: horizontal controlled AeroPanelGroup + LaunchedEffect counter in one section as permanent showcase guard"

requirements-completed: [RCMP-04, RCMP-01, REG-01]

# Metrics
duration: 6min
completed: 2026-06-26
---

# Phase 14 Plan 02: Showcase RCMP-04 Repro Demo Summary

**Permanent horizontal controlled AeroPanelGroup regression demo in LayoutSection.kt: 3 sections with one reading a LaunchedEffect-ticked counter, guarding the recompose-during-drag fix (RCMP-04/RCMP-01)**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-06-26T08:29:24Z
- **Completed:** 2026-06-26T08:35:00Z
- **Tasks:** 2 of 3 (Task 3 is human-verify checkpoint — awaiting sign-off)
- **Files modified:** 1

## Accomplishments

- Added permanent labeled RCMP-04 repro block as last child of LayoutSection's outer Column
- Block uses horizontal controlled AeroPanelGroup (expandedKeys + onExpandedChange), 3 sections (Live / Static A / Static B)
- "Live" section reads a `LaunchedEffect`-ticked `mutableStateOf` counter (~32ms cadence) so it recomposes independently during a divider drag
- Captioned to explain it guards the recompose-during-drag fix introduced in v2.0.3
- Added `import androidx.compose.runtime.LaunchedEffect` and `import kotlinx.coroutines.delay`
- Showcase module compiles clean (`BUILD SUCCESSFUL`); 12 `PanelGroupLogicTest` JVM tests remain GREEN

## Task Commits

1. **Task 1: Add permanent RCMP-04 recompose-during-drag repro demo to LayoutSection.kt** - `77701d9` (feat)
2. **Task 2: Build the showcase to confirm the repro compiles** - (verification only, no file changes — compile passed BUILD SUCCESSFUL, 12 tests GREEN)
3. **Task 3: Human verify** - CHECKPOINT — awaiting human sign-off

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — Added RCMP-04 repro block (52 lines: 2 imports + demo block) appended inside outer Column before its closing brace

## Decisions Made

- Used `rcmpExpandedKeys` as the state variable name (not `expandedKeys`) to avoid potential shadowing with the existing uncontrolled demo patterns
- Counter cadence 32ms: matches the plan action block and falls within Claude's Discretion per CONTEXT.md
- Section titles Live/Static A/Static B: matches the plan's `<action>` block (plan takes precedence over research example's Left/Center/Right titles)
- `typography.bodySmall` confirmed present in AeroTypography.kt (not a fallback needed)

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Task 3 checkpoint requires human visual sign-off: launch showcase, navigate to LayoutSection, observe the RCMP-04 demo (counter ticking, divider drag shows exactly 3 sections, never 9)
- After sign-off: Plan 03 (version bump 2.0.2 → 2.0.3 + JitPack tag release) is unblocked

---
*Phase: 14-panelgroup-recompose-fix-v2-0-3-release*
*Completed: 2026-06-26 (partial — checkpoint at Task 3)*
