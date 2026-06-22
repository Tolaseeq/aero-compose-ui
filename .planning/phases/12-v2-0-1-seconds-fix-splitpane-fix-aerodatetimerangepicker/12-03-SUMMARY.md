---
phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
plan: 03
subsystem: ui
tags: [compose-desktop, kotlinx-datetime, date-picker, range-picker, apply-gate, tdd]

requires:
  - phase: 12-01
    provides: formatAeroDateTime helper (shared datetime formatter, PITFALL-H prevention)
  - phase: 12-02
    provides: Fix B (SplitPane) — no direct code dependency, wave ordering only

provides:
  - AeroDateTimeRangePicker public composable (DTR-01..08)
  - orderDateTimeRange pure internal helper (same-day reversed-time swap, DTR-04)
  - AeroDateTimeRangePickerTest (6 tests covering DTR-03/04 logic)

affects:
  - 12-04-showcase (SHW-11 PickersSection demo consumes AeroDateTimeRangePicker)

tech-stack:
  added: []
  patterns:
    - "Apply-gate composable: day-click discards commit pair; sole emit+close site is Apply onClick"
    - "Four remember(expanded) pending-state blocks: no cross-open leaks (PITFALL-G)"
    - "Unconditional time rows + enabled gate: stable popup height before date selected (PITFALL-I)"
    - "orderDateTimeRange at sole emit site: LocalDateTime Comparable swap, no Instant conversion"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePickerTest.kt
  modified: []

key-decisions:
  - "Apply gate (PITFALL-E prevention): nextRangeState commit pair discarded in onDayClick; onRangeSelect fires ONLY at Apply onClick guarded by rangeState is Selected"
  - "orderDateTimeRange applied at sole emit site: a<=b Comparable check swaps same-day reversed times silently (DTR-04); no error UI"
  - "Both TimeFields rows rendered unconditionally; enabled=false until Selected — stable popup height for AeroCalendarPositionProvider flip logic (PITFALL-I)"
  - "Four remember(expanded) blocks (rangeState, leftMonth, pendingStartTime, pendingEndTime) — cancelled sessions leave no trace on reopen (DTR-07, PITFALL-G)"
  - "Default trigger formatter delegates to formatAeroDateTime from Fix A; formatter param overrides entirely (DTR-06, PITFALL-H prevention)"

patterns-established:
  - "Dual Apply-gate picker pattern: two endpoints, same pending-state discipline as AeroDateTimePicker but extended to rangeState machine"

requirements-completed: [DTR-01, DTR-02, DTR-03, DTR-04, DTR-05, DTR-06, DTR-07, DTR-08]

duration: 4min
completed: 2026-06-22
---

# Phase 12 Plan 03: AeroDateTimeRangePicker Summary

**AeroDateTimeRangePicker composable (DTR-01..08): Apply-gate dual-calendar datetime range picker with same-day reversed-time swap via pure orderDateTimeRange helper**

## Performance

- **Duration:** 4 min
- **Started:** 2026-06-22T12:52:06Z
- **Completed:** 2026-06-22T12:56:07Z
- **Tasks:** 2
- **Files modified:** 2 created

## Accomplishments

- `orderDateTimeRange` pure internal helper: same-day reversed times swapped via LocalDateTime Comparable (`a <= b`), cross-day date-dominates, equal datetimes unchanged — all four DTR-04 cases unit-tested and GREEN
- Apply-gate architecture locked: `onDayClick` discards the commit pair from `nextRangeState`; the single `onRangeSelect` call site is inside the Apply `onClick`, guarded by `rangeState is AeroDateRangeState.Selected` (PITFALL-E fully prevented)
- Two unconditional `TimeFields` rows rendered from popup frame 1, `enabled = false` until `Selected` — stable height for `AeroCalendarPositionProvider` flip logic (PITFALL-I); `showSeconds` and `minuteStep` apply equally to both rows (DTR-05)
- Four `remember(expanded)` blocks ensure cancelled/click-outside sessions never leak pending state into the next open (DTR-07, PITFALL-G)
- Default trigger formatter delegates to `formatAeroDateTime` from Fix A; nullable `formatter` param overrides (DTR-06, PITFALL-H prevention)

## Task Commits

Each task was committed atomically:

1. **Task 1: orderDateTimeRange helper + DTR-04/DTR-03 logic tests** - `09eabba` (feat)
2. **Task 2: AeroDateTimeRangePicker composable with Apply gate** - `d425d7e` (feat)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt` — pure `orderDateTimeRange` helper + full public `AeroDateTimeRangePicker` composable (same package as siblings, all internals accessible)
- `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePickerTest.kt` — 6 tests: 4 `orderDateTimeRange` cases (DTR-04) + 2 Apply-gate state-machine proofs via `nextRangeState` (DTR-03)

## Decisions Made

- Apply gate (PITFALL-E prevention): `nextRangeState` commit pair intentionally discarded with `_` in `onDayClick`; `onRangeSelect` is the sole emit site, gated by `rangeState is AeroDateRangeState.Selected`
- `orderDateTimeRange` uses `LocalDateTime <= LocalDateTime` directly (Comparable in kotlinx-datetime 0.6.2) — no Instant/timezone conversion needed
- Both `TimeFields` rows rendered unconditionally with `enabled` gate (not conditionally composed) — stable popup height prevents position-provider layout thrash (PITFALL-I confirmed from CONTEXT.md)
- `formatter: ((LocalDateTime) -> String)? = null` nullable pattern consistent with `AeroDateTimePicker` Fix A convention

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — compilation and all 6 tests GREEN on first attempt.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- `AeroDateTimeRangePicker` ready to consume in Plan 04 (showcase SHW-11 demo in `PickersSection`)
- All DTR-01..08 requirements implemented and verified
- No blockers

---
*Phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker*
*Completed: 2026-06-22*
