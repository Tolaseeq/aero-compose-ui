---
phase: 07-shared-internal-primitives
plan: 03
subsystem: ui
tags: [compose-desktop, kotlin, aero, calendar, step-indicator, glass, gap-closure]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives-plan-01
    provides: AeroCalendarGrid, AeroCalendarPositionProvider, AeroStepIndicator primitives (all internal)
  - phase: 07-shared-internal-primitives-plan-02
    provides: AeroPhase7Scratch aggregator + Phase7ScratchSection showcase wrapper

provides:
  - AeroCalendarGrid with symmetric layout (header width == 7x36dp day-grid width)
  - Scratch demo with glass-wrapped StepIndicator block (AeroCard) representing Phase 10 hosting context
  - Scratch demo with glass-wrapped calendar popup body (AeroCard) representing Phase 8 hosting context
  - Scratch demo using AeroButton/AeroOutlinedButton at intrinsic width (no raw Material buttons)

affects: [Phase 8 AeroDatePicker, Phase 10 AeroStepperWizard, 07-VERIFICATION.md re-check]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Gap-closure plan: fix only the gaps recorded in VERIFICATION.md, exactly 2 files modified, 0 new files"
    - "Architecture B locked: glass surfaces belong to consumers (AeroCard in scratch/Phase8/Phase10), never inside primitives"
    - "header Row pinned to intrinsic grid width via Modifier.width(252.dp) — outer Column wrapContentWidth prevents wide-parent inflation"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
    - library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt

key-decisions:
  - "AeroCalendarGrid header Row: Modifier.width(252.dp) (not fillMaxWidth) — outer Column wrapContentWidth; 7x36dp=252dp is the invariant day-grid width"
  - "Architecture B confirmed: AeroCard glass wrapper applied in scratch demo only; AeroCalendarGrid and AeroStepIndicator stay surface-less"
  - "AeroButton/AeroOutlinedButton replace raw Material Button calls — text param (not content slot); intrinsic width by default"

patterns-established:
  - "Gap-closure discipline: modify only the 2 target files, close only the 4 recorded gaps, no scope creep"
  - "Scratch demo glass containers represent the real Phase 8/10 hosting context — AeroCard wraps the whole demo block, not individual cells"

requirements-completed: []

# Metrics
duration: 3min
completed: 2026-06-17
---

# Phase 7 Plan 03: Gap Closure Summary

**AeroCalendarGrid header pinned to 252dp symmetric width + scratch demos glass-wrapped on AeroCard + Material buttons replaced with AeroButton/AeroOutlinedButton at intrinsic width**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-06-17T14:13:34Z
- **Completed:** 2026-06-17T14:16:05Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- gap-01 (BLOCKING) closed: AeroCalendarGrid header Row no longer fillMaxWidth; outer Column wrapContentWidth() + header Modifier.width(252.dp) makes prev/label/next align precisely above the 7-column day grid — visual asymmetry eliminated
- gap-02 closed: StepIndicator demo wrapped in AeroCard (glass surface); AeroStepIndicator itself stays surface-less per architecture B
- gap-03 closed: CalendarPopupDemo popup body wrapped in AeroCard (glass); AeroCalendarGrid itself stays surface-less per architecture B
- gap-04 closed: all raw Material Button/OutlinedButton replaced with AeroButton/AeroOutlinedButton at intrinsic width; trigger button no longer stretches across the 1024dp frame
- 27 existing unit tests remain green (AeroCalendarGridTest 7 + AeroColorMathTest 16 + AeroCalendarPositionProviderTest 4)
- Both :library and :showcase compile clean

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix AeroCalendarGrid header stretching past the day grid (gap-01)** - `3ce9ea0` (fix)
2. **Task 2: Glass-wrap StepIndicator + calendar popup demos and swap raw Buttons to Aero buttons (gaps 02/03/04)** - `06c460f` (feat)

**Plan metadata:** (docs commit — created after this section)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` — outer Column modifier.wrapContentWidth(); header Row Modifier.width(252.dp) replaces fillMaxWidth(); imports width + wrapContentWidth added
- `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` — StepIndicatorDemo wrapped in AeroCard with AeroOutlinedButton/AeroButton; CalendarPopupDemo trigger AeroButton; popup body AeroCard; Material Button/OutlinedButton imports removed; AeroButton/AeroOutlinedButton/AeroCard/wrapContentWidth imports added

## Decisions Made

- `Modifier.width(252.dp)` on the header Row (not `Modifier.fillMaxWidth(0.wrapContentWidth)`) — explicit pixel constant matches 7 × 36dp cell invariant; this is more readable and self-documenting than deriving the width at runtime
- `AeroCard(modifier = Modifier.wrapContentWidth())` on StepIndicatorDemo wrap — AeroCard sizes to content; wrapContentWidth prevents the card from inflating to the full column width
- `Modifier.width(320.dp)` on AeroStepIndicator inside the card — gives the 4-step dots+connectors a comfortable reading width without stretching the card to full column width
- Architecture B reconfirmed: glass lives only in the scratch wrapper composables; no glass modifier passed into AeroCalendarGrid or AeroStepIndicator params

## Deviations from Plan

None — plan executed exactly as written. Both edits matched the concrete change specifications in the plan. No unexpected compilation errors; no missing imports; no edge cases.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- The 2 UAT truths previously `blocked_by_gaps` are now re-checkable:
  - **AeroStepIndicator three-theme contrast** (gap-02 + gap-04 closed): human can now run `./gradlew :showcase:run`, scroll to Phase 7 Scratch, and verify Current/Completed/Upcoming dots across AeroBlue / AeroDark / Classic
  - **AeroCalendarPositionProvider wide-popup visual** (gap-03 + gap-04 closed): trigger button sits at intrinsic width at right edge of 1024dp frame; popup body on glass panel — clean visual assessment now possible
- Phase 7 gap closure is complete; `/gsd:verify-work` can re-check the two blocked_by_gaps truths and promote Phase 7 to fully verified
- Phase 8 (Pickers) is unblocked: AeroCalendarGrid layout is symmetric; AeroCalendarPositionProvider + AeroCalendarGrid together represent the correct hosting pattern for AeroDatePicker

---
*Phase: 07-shared-internal-primitives*
*Completed: 2026-06-17*
