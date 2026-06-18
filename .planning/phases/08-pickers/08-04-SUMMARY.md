---
phase: 08-pickers
plan: 04
subsystem: ui
tags: [compose-desktop, kotlinx-datetime, datetime-picker, popup, commit-gate]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives
    provides: AeroCalendarGrid, AeroCalendarPositionProvider
  - phase: 08-pickers (plan 02)
    provides: PickerPopupContainer, dateIsDisabled, AeroDatePicker trigger pattern
  - phase: 08-pickers (plan 03)
    provides: TimeFields internal spinner row, assembleTime helper
provides:
  - public AeroDateTimePicker composable (PICK-04) with an Apply/Cancel commit gate
  - internal combineDateTime(date, time) -> LocalDateTime merge helper
affects: [11-showcase, pickers]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Commit-gate: day click + time edits mutate pending state only; onValueChange fires ONLY on Apply (NEW-PICK-02)"
    - "Pending state keyed on `expanded` so a cancelled session never leaks edits into the next open"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt
  modified: []

key-decisions:
  - "AeroDateTimePicker composes AeroCalendarGrid + TimeFields directly; it does NOT embed AeroDatePicker/AeroTimePicker (avoids double trigger/popup and per-day-click emission)"
  - "combineDateTime is the single merge seam and the only onValueChange( call site lives in the Apply onClick — proven by grep (1 total hit)"
  - "Pending date/time/displayMonth are remember(expanded)-keyed so Cancel discards edits with no callback"

patterns-established:
  - "Commit-gate popup: pending state + Apply (enabled = pendingDate != null) / Cancel (silent dismiss)"

requirements-completed: [PICK-04]

# Metrics
duration: 2min
completed: 2026-06-18
---

# Phase 8 Plan 04: AeroDateTimePicker Summary

**Public AeroDateTimePicker (PICK-04): one popup with AeroCalendarGrid above a TimeFields spinner row and a Cancel/Apply commit gate — day clicks and time edits only mutate pending state; onValueChange(LocalDateTime) fires solely on Apply (NEW-PICK-02).**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-06-18T10:13:26Z
- **Completed:** 2026-06-18T10:15:08Z
- **Tasks:** 2
- **Files modified:** 2 (both created)

## Accomplishments
- `combineDateTime(LocalDate, LocalTime) -> LocalDateTime` pure merge helper + 3 unit tests (merge, .date/.time accessors, Apply-gate emission model)
- Public `AeroDateTimePicker` composable: read-only trigger field + calendar icon button opening a single popup hosting the calendar grid, time spinner row, and Cancel/Apply buttons
- Commit gate enforced and grep-proven: the only `onValueChange(` in the file is `onValueChange(combineDateTime(...))` inside the Apply onClick; `onDateSelected` only mutates `pendingDate`
- Reuses `dateIsDisabled` (plan 02) for min/max/selectableDates; `Popup` not `Dialog`, no transparency flag (W11-01)

## Task Commits

Each task was committed atomically:

1. **Task 1: combineDateTime helper + tests scaffold** - `92581fa` (test)
2. **Task 2: AeroDateTimePicker composable with pending state + Apply/Cancel gate** - `40c2f59` (feat)

**Plan metadata:** see final docs commit.

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` - public AeroDateTimePicker + internal combineDateTime + private todayLocalDate helper
- `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt` - PICK-04 commit-gate / merge tests

## Decisions Made
- Composes the Phase 7 grid + plan-03 TimeFields directly rather than embedding the standalone pickers, so there is exactly one trigger, one popup, and one emission path.
- Apply button is gated on `pendingDate != null` (cannot emit a time-only value); Cancel and click-outside dismiss silently.

## Deviations from Plan
None - plan executed exactly as written.

The plan's pseudo-code showed `AeroButton(onClick = ...) { Text("Apply") }`, but the actual `AeroButton` / `AeroOutlinedButton` signatures take `text: String` (no content lambda). Used the real signature `AeroButton(text = "Apply", onClick = ...)`. This is signature-matching to the read_first sources, not a behavioral deviation.

## Issues Encountered
None. compileKotlin and the AeroDateTimePickerTest suite were green on first run for both tasks.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- PICK-04 complete. Phase 8 remaining: plan 05 (AeroDateRangePicker) which reuses the same grid + PickerPopupContainer + dateIsDisabled seam.
- The commit-gate pattern (pending state + Apply/Cancel) is now established for plan 05's range selection.

## Self-Check: PASSED

- FOUND: library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt
- FOUND: 92581fa (Task 1)
- FOUND: 40c2f59 (Task 2)

---
*Phase: 08-pickers*
*Completed: 2026-06-18*
