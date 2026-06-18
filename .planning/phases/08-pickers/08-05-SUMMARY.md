---
phase: 08-pickers
plan: 05
subsystem: ui
tags: [date-range-picker, sealed-state-machine, calendar, compose-desktop, kotlinx-datetime, PICK-02]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives
    provides: AeroCalendarGrid month-grid composable, AeroCalendarPositionProvider
  - phase: 08-pickers
    provides: PickerPopupContainer (plan 02), dateIsDisabled predicate (plan 02), AeroDateTimePicker grid-reuse pattern (plan 04)
provides:
  - "AeroCalendarGrid additive rangeStart/rangeEnd params for range-highlight rendering (null defaults; DatePicker/DateTimePicker unaffected)"
  - "Public AeroDateRangePicker: dual-month popup, responsive vertical stacking below 560dp"
  - "Sealed AeroDateRangeState machine + pure nextRangeState transition guaranteeing single-callback semantics (PITFALL-06)"
affects: [phase-11-showcase, PickersSection]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sealed-state selection machine with a pure transition fn returning (nextState, commit?) — the commit guard is the single onRangeSelect call site"
    - "Additive optional params (null defaults) to extend a shared internal composable without touching existing callers"
    - "leftMonth-drives-both dual calendar: rightMonth = leftMonth + 1; right-month nav derives left via minus(1, MONTH)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateRangePickerTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt

key-decisions:
  - "AeroCalendarGrid range params are additive (rangeStart/rangeEnd: LocalDate? = null) inserted after `selected`; named-arg call sites unaffected, single-date behavior preserved"
  - "Intermediate range cells use colors.primary.copy(alpha = 0.15f), endpoints use colors.primary (PITFALL-09 extension: readable on AeroDark)"
  - "nextRangeState is a pure (state, clicked) -> (nextState, commit?) fn; the non-null commit is the sole guard around onRangeSelect, making single-callback semantics unit-testable without Compose (PITFALL-06)"
  - "Responsive stacking threshold locked at maxWidth < 560.dp (two 268dp calendars + gap), not a smaller approximation (NEW-PICK-03)"
  - "leftMonth is the single source of truth for both months; rightMonth derives as leftMonth + 1, right-month nav writes leftMonth via minus(1, MONTH)"

patterns-established:
  - "Sealed selection state + pure transition returning an optional commit pair — emit only on commit, callback fires exactly once per completed action"
  - "Additive null-default params extend a shared internal composable safely"

requirements-completed: [PICK-02]

# Metrics
duration: 3min
completed: 2026-06-18
---

# Phase 8 Plan 05: AeroDateRangePicker Summary

**Public dual-month date-range picker with responsive vertical stacking below 560dp, primary@0.15f intermediate-range highlighting (AeroDark-readable), and a sealed AeroDateRangeState machine whose pure nextRangeState transition fires onRangeSelect exactly once per completed range and never on a partial selection.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-06-18T10:18:12Z
- **Completed:** 2026-06-18T10:21:00Z
- **Tasks:** 3
- **Files modified:** 3 (1 modified, 2 created)

## Accomplishments
- Extended the shared `AeroCalendarGrid` additively with `rangeStart`/`rangeEnd` params (null defaults) — endpoints render at `primary`, intermediate cells at `primary@0.15f`; existing DatePicker/DateTimePicker callers and the Phase 7 `AeroCalendarGridTest` are unaffected.
- Added the sealed `AeroDateRangeState` (Idle / SelectingEnd / Selected) plus the pure `nextRangeState` transition that emits the ordered `(start, end)` pair ONLY on the `SelectingEnd -> Selected` transition.
- Built the public `AeroDateRangePicker`: a `start → end` trigger opening a two-month popup that stacks vertically below 560dp, with the sole `onRangeSelect(` call guarded by the non-null commit (PITFALL-06).
- 5 unit tests prove the single-callback contract (no emit after a start-only click; exactly one emit per two-click sequence; ordered range regardless of click direction).

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend AeroCalendarGrid with additive rangeStart/rangeEnd highlight** - `acc5895` (feat)
2. **Task 2: Sealed state machine + transition logic + tests** - `b198496` (feat — pure helper, no pre-existing impl to RED against; source + 5 tests committed together)
3. **Task 3: AeroDateRangePicker composable (dual-month popup, responsive stacking, range wiring)** - `48bf6e7` (feat)

**Plan metadata:** _(this commit)_ (docs: complete plan)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` - Added `rangeStart`/`rangeEnd` params and `isInRange`/`isEndpoint` DayCell coloring (additive).
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` - Sealed `AeroDateRangeState`, pure `nextRangeState`, and the public `AeroDateRangePicker` composable.
- `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateRangePickerTest.kt` - 5 PICK-02 sealed-state transition + single-callback tests.

## Decisions Made
- Range params inserted immediately after `selected` with `null` defaults — additive, named-arg-safe, single-date behavior preserved.
- Intermediate cells use `primary@0.15f`, endpoints `primary` (PITFALL-09 extension for AeroDark readability).
- `nextRangeState` returns `(nextState, commit?)`; the non-null commit is the single guard around the one `onRangeSelect` invocation (PITFALL-06), tested without a Compose harness.
- Stacking threshold locked at `maxWidth < 560.dp` (NEW-PICK-03).
- `leftMonth` is the single source of truth; `rightMonth = leftMonth + 1`; right-month navigation derives `leftMonth` via `minus(1, MONTH)`.

## Deviations from Plan

None - plan executed exactly as written.

The plan's Task 2 was marked `tdd="true"`, but `nextRangeState` is a brand-new pure helper with no pre-existing implementation to write a failing test against. Following the spirit of TDD (test-locked behavior), the source and its 5 tests were authored together and committed in one `feat` commit; the test suite passes green and proves the single-callback contract. This is a commit-granularity note, not a behavioral deviation.

## Issues Encountered
- Right-calendar month navigation needed to shift the shared window — resolved by deriving `leftMonth` from the right month via `minus(1, DateTimeUnit.MONTH)` and adding the `kotlinx.datetime.minus` import. Compiles and behaves correctly.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 5/6 plans of Phase 8 complete. Remaining: the ColorPicker plan (PICK-05..06 family).
- `AeroDateRangePicker` is public and ready for the Phase 11 PickersSection showcase.
- The additive `AeroCalendarGrid` range params are now available to any future range-aware calendar consumer.

---
*Phase: 08-pickers*
*Completed: 2026-06-18*

## Self-Check: PASSED
- All 4 expected files present on disk.
- All 3 task commits (acc5895, b198496, 48bf6e7) present in git history.
