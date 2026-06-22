---
phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
plan: "01"
subsystem: ui
tags: [kotlin, compose-desktop, datetime-picker, kotlinx-datetime, tdd]

requires:
  - phase: 11-showcase-v2-0-visual-sign-off
    provides: AeroDateTimePicker with combineDateTime helper, formatAeroDate in AeroDatePicker.kt

provides:
  - "internal fun formatAeroDateTime(ldt, showSeconds) — shared datetime trigger format helper"
  - "AeroDateTimePicker with nullable formatter param + showSeconds-aware default displayText"
  - "Three new unit tests for formatAeroDateTime (seconds off, seconds on, custom formatter priority)"
  - "PROJECT.md corrected kotlinx-datetime decision row (api, not implementation)"

affects:
  - "12-03 (AeroDateTimeRangePicker) — inherits formatAeroDateTime helper"

tech-stack:
  added: []
  patterns:
    - "formatAeroDateTime internal helper pattern: single source of truth for datetime trigger strings"
    - "nullable formatter: ((T) -> String)? = null with body-level dispatch pattern"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt
    - .planning/PROJECT.md

key-decisions:
  - "formatter param changed to nullable (((LocalDateTime) -> String)? = null); displayText dispatches via formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)"
  - "formatAeroDateTime is internal and lives in AeroDateTimePicker.kt (same package as combineDateTime) — AeroDateTimeRangePicker can call it without visibility changes"
  - "TDD RED confirmed: compileTestKotlin failed with Unresolved reference formatAeroDateTime before helper was added"

patterns-established:
  - "nullable-formatter-dispatch: callers pass null to get showSeconds-aware default; non-null lambda used verbatim — establishes the pattern for AeroDateTimeRangePicker"

requirements-completed: [FIXDT-01, FIXDT-02, SHW-14]

duration: 4min
completed: 2026-06-22
---

# Phase 12 Plan 01: Fix A — AeroDateTimePicker Seconds Fix Summary

**`internal fun formatAeroDateTime(ldt, showSeconds)` helper introduced with nullable-formatter dispatch in AeroDateTimePicker; showSeconds now reflected in trigger text; custom formatters preserved verbatim; kotlinx-datetime doc corrected to api(...)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-06-22T12:44:10Z
- **Completed:** 2026-06-22T12:48:38Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Added `internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String` to AeroDateTimePicker.kt — shared helper preventing future re-introduction of the hardcoded-HH:MM pattern (PITFALL-H)
- Wired `AeroDateTimePicker` displayText computation through the new helper via `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)` — fixes FIXDT-01 (seconds now visible) and preserves FIXDT-02 (custom formatter priority)
- Extended test suite with three new `@Test` methods covering seconds-off, seconds-on, and custom-formatter-priority; all pass alongside existing combineDateTime tests
- Corrected the stale `implementation` / "Revisit at publish" row in PROJECT.md to the factual `api(libs.kotlinx.datetime)` record (SHW-14)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add formatAeroDateTime helper + seconds/custom-formatter tests (TDD)** - `37c05b6` (feat)
2. **Task 2: Wire AeroDateTimePicker trigger to formatAeroDateTime** - `cf74ae7` (fix)
3. **Task 3: SHW-14 doc hygiene — correct kotlinx-datetime decision row** - `497a1f7` (docs)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` - Added `formatAeroDateTime` helper; changed `formatter` param to nullable; updated `displayText` computation and KDoc
- `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt` - Added three new test methods (formatAeroDateTimeOmitsSecondsWhenDisabled, formatAeroDateTimeIncludesSecondsWhenEnabled, customFormatterTakesPriorityOverDefault)
- `.planning/PROJECT.md` - Replaced stale decision row with factual api(libs.kotlinx.datetime) record

## Decisions Made
- `formatter` param changed from non-nullable-with-default lambda to `((LocalDateTime) -> String)? = null` — avoids PITFALL-H (default lambda can't close over `showSeconds` since it's declared after `formatter`); body-level dispatch resolves the capture issue cleanly
- `formatAeroDateTime` placed at file scope in `AeroDateTimePicker.kt` (alongside `combineDateTime`) — same package as future `AeroDateTimeRangePicker.kt`; no visibility changes needed for plan 03
- TDD protocol applied: RED phase confirmed with `compileTestKotlin` reporting `Unresolved reference 'formatAeroDateTime'`; GREEN passed after helper addition

## Deviations from Plan

None — plan executed exactly as written.

One minor IO error occurred on second test run (file lock on test results binary from prior run); resolved by retrying immediately. Not a code deviation.

## Issues Encountered
- Gradle `Unable to delete directory .../test-results/test/binary` on second `./gradlew test` invocation — Windows file lock from the previous test process. Resolved by re-running the command; no code change required.

## User Setup Required
None — no external service configuration required.

## Next Phase Readiness
- `formatAeroDateTime` is `internal` in `com.mordred.aero.components.pickers` — plan 03 (`AeroDateTimeRangePicker`) can call it directly without extra visibility changes
- `AeroDateTimePicker` compiles clean; all 6 unit tests green
- Fix B (plan 02, SplitPane freeze) is independent and can proceed in parallel or sequentially

---
*Phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker*
*Completed: 2026-06-22*

## Self-Check: PASSED

- FOUND: AeroDateTimePicker.kt
- FOUND: AeroDateTimePickerTest.kt
- FOUND: PROJECT.md
- FOUND: 12-01-SUMMARY.md
- FOUND: commit 37c05b6 (Task 1)
- FOUND: commit cf74ae7 (Task 2)
- FOUND: commit 497a1f7 (Task 3)
- PASS: `internal fun formatAeroDateTime(` in AeroDateTimePicker.kt
- PASS: nullable formatter param present
- PASS: dispatch expression `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)` present
- PASS: `api(libs.kotlinx.datetime)` in PROJECT.md
- PASS: stale "Revisit на publish" note absent
