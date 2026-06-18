---
phase: 10-layout
plan: 02
subsystem: ui
tags: [compose-multiplatform, kotlin, accordion, animation, state-management, tdd]

# Dependency graph
requires:
  - phase: 10-01
    provides: internal/accordion directory + layout package bootstrap
  - phase: 07-shared-internal-primitives
    provides: AeroIcons.CaretRight, GlassModifiers.glassPanel
provides:
  - AeroAccordion public composable (LAYO-01, LAYO-02)
  - AeroAccordionSection data class
  - AeroAccordionMode enum (Single, Multi)
  - AccordionToggle.kt pure internal helpers (accordionToggleSingle, accordionToggleMulti)
  - AccordionToggleTest.kt unit tests (6 cases, all green)
affects: [10-03, 10-04, 10-05, 11-showcase]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Hybrid uncontrolled/controlled accordion: uncontrolled default, controlled when onExpandedChange != null"
    - "State lifted to parent (PITFALL-13): sections receive expanded + onToggle, hold no state"
    - "Pure toggle helpers in internal/accordion: accordionToggleSingle, accordionToggleMulti"
    - "animateContentSize + animateFloatAsState for expand/collapse (NOT animateIntAsState)"
    - "mutableStateOf(Set<Int>) for internal multi-mode expansion state"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/layout/internal/accordion/AccordionToggle.kt
    - library/src/test/kotlin/com/mordred/aero/components/layout/AccordionToggleTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroSidebarState.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt

key-decisions:
  - "AeroAccordion hybrid ownership: onExpandedChange null = uncontrolled (internal Set<Int> state); non-null = controlled (pure renderer)"
  - "mutableStateOf(Set<Int>) used instead of mutableStateSetOf for multi-mode uncontrolled state (simpler, triggers recomposition on assignment)"
  - "AeroSplitPane switched from onSizeChanged to BoxWithConstraints per RESEARCH.md recommendation (linter improvement)"
  - "AeroSidebarMode enum + targetWidthForMode restored to AeroSidebarState.kt (were removed by linter; also fixed labelSmall->label + added setValue import)"

patterns-established:
  - "Pattern: pure toggle functions in internal/accordion package, no Compose imports, JVM-testable"
  - "Pattern: data-list sections (List<AeroAccordionSection>) matching Phase 9 columns pattern"
  - "Pattern: AccordionSectionRow private composable receives expanded+onToggle only (PITFALL-13)"

requirements-completed: [LAYO-01, LAYO-02]

# Metrics
duration: 11min
completed: 2026-06-18
---

# Phase 10 Plan 02: AeroAccordion Summary

**AeroAccordion with single/multi mode, animateContentSize expand/collapse, caret rotation, and hybrid state ownership — backed by 6 unit-tested pure toggle helpers (LAYO-01, LAYO-02)**

## Performance

- **Duration:** 11 min
- **Started:** 2026-06-18T14:14:04Z
- **Completed:** 2026-06-18T14:25:24Z
- **Tasks:** 2 (Task 1 TDD + Task 2 composable)
- **Files modified:** 5

## Accomplishments

- `AccordionToggle.kt` — two pure internal functions (`accordionToggleSingle`, `accordionToggleMulti`) with no Compose imports, fully JVM-testable
- `AccordionToggleTest.kt` — 6 unit test cases covering all single/multi toggle behaviors (green)
- `AeroAccordion.kt` — public composable with `AeroAccordionMode`, `AeroAccordionSection`, hybrid ownership KDoc, `animateContentSize` + `animateFloatAsState` caret, state lifted to parent (PITFALL-13), `glassPanel` header
- Fixed 3 pre-existing bugs in untracked layout files blocking `compileKotlin`

## Task Commits

1. **Task 1: Pure accordion toggle helpers + unit tests (TDD)** - `8de8c4d` (test)
2. **Task 2: Public AeroAccordion composable** - `d62dadd` (feat)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/accordion/AccordionToggle.kt` - Pure single/multi toggle functions
- `library/src/test/kotlin/com/mordred/aero/components/layout/AccordionToggleTest.kt` - 6 unit test cases
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt` - Public AeroAccordion + AeroAccordionSection + AeroAccordionMode (pre-committed; verified all acceptance criteria)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSidebarState.kt` - Restored AeroSidebarMode enum + targetWidthForMode, fixed labelSmall->label, added setValue import
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` - Removed invalid weight Modifier import; switched to BoxWithConstraints per RESEARCH.md

## Decisions Made

- Used `mutableStateOf(Set<Int>)` instead of `mutableStateSetOf` for multi-mode uncontrolled state — simpler approach, triggers recomposition via assignment rather than mutation
- Restored `AeroSidebarMode` enum + `targetWidthForMode` to `AeroSidebarState.kt` (linter had removed them; pre-existing Wave 0 test `SidebarStateTest.kt` requires them)
- `AeroSplitPane.kt` switched to `BoxWithConstraints` approach (linter improvement aligning with RESEARCH.md Pattern 3)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing Wave 0 test files referenced unresolved symbols**
- **Found during:** Task 1 (AccordionToggleTest compilation)
- **Issue:** `SidebarStateTest.kt`, `SplitClampTest.kt`, `WizardStepTest.kt` already existed in the test directory (committed by previous plan runs) and referenced `AeroSidebarMode`, `targetWidthForMode`, `clampDividerPx`, `fractionToPx`, `pxToFraction`, `nextStepIndex`, `prevStepIndex`, `isLastStep`. Their implementations existed in separate source files but `AeroSidebarMode` + `targetWidthForMode` had been removed from `AeroSidebarState.kt` by linter.
- **Fix:** Restored `AeroSidebarMode` enum + `targetWidthForMode` to `AeroSidebarState.kt`; also fixed `labelSmall` -> `label` typography token and added missing `setValue` import
- **Files modified:** `AeroSidebarState.kt`
- **Verification:** `compileKotlin` exits 0; AccordionToggleTest suite green
- **Committed in:** `8de8c4d` (Task 1 commit)

**2. [Rule 1 - Bug] AeroSplitPane.kt had invalid `weight` Modifier import**
- **Found during:** Task 2 (compileKotlin verification)
- **Issue:** `import androidx.compose.foundation.layout.weight` on line 15 — this is an internal Compose API that cannot be directly imported; `weight` is only accessible via `RowScope`/`ColumnScope`
- **Fix:** Removed the invalid import line
- **Files modified:** `AeroSplitPane.kt`
- **Verification:** `compileKotlin` exits 0
- **Committed in:** `d62dadd` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes necessary for `compileKotlin` to succeed. No scope creep.

## Issues Encountered

- The `AeroAccordion.kt` file had been pre-committed in a previous agent run (`d93df5c`), so Task 2 verification confirmed the existing file rather than creating a new one. All acceptance criteria confirmed present.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- LAYO-01 + LAYO-02 requirements complete
- AccordionToggle helpers are pure and tested — single-mode coordination locked
- AeroSplitPane.kt and AeroSidebarState.kt compile errors fixed, unblocking plans 10-03 and 10-04
- Ready for Plan 10-03 (AeroSplitPane completion) or showcase wiring in Phase 11

---
*Phase: 10-layout*
*Completed: 2026-06-18*
