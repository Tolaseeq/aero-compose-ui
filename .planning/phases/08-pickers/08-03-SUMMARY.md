---
phase: 08-pickers
plan: 03
subsystem: ui
tags: [compose-desktop, popup, timepicker, kotlinx-datetime, aero, number-spinner]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives
    provides: AeroCalendarPositionProvider
  - phase: 08-pickers
    plan: 02
    provides: PickerPopupContainer (shared W11-02 popup surface)
  - phase: 01-foundation
    provides: AeroTextField, AeroIconButton, AeroNumberSpinner, AeroColorScheme tokens
provides:
  - "AeroTimePicker public composable (PICK-03): read-only trigger + popup hosting hour/minute(/second) spinners, emits kotlinx.datetime.LocalTime (24-hour)"
  - "TimeFields internal composable: reusable hour/minute(/second) AeroNumberSpinner row consumed by AeroDateTimePicker (plan 04)"
  - "assembleTime internal pure helper: clamps h 0..23, m/s 0..59 and drops seconds when showSeconds=false"
affects: [08-04-datetimepicker]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TimeFields extracted as an internal reusable spinner row so AeroDateTimePicker reuses the time entry without duplication"
    - "assembleTime is a pure internal helper so the LocalTime emission/clamp contract is unit-testable without driving the composable"
    - "AeroNumberSpinner callbacks passed straight through (no debounce/transform) to avoid remember(value) text desync (NEW-PICK-04)"
    - "Popup anchored inside the trigger Box via AeroCalendarPositionProvider; no Apply gate — emits on every spinner change"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroTimePicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/TimeFields.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroTimePickerTest.kt
  modified: []

key-decisions:
  - "AeroTimePicker is 24-hour only — no use12Hour / AM-PM parameter (descoped per user decision); enforced by zero-match grep gate"
  - "TimePicker emits LocalTime on every spinner change (no Apply gate); the Apply gate belongs only to AeroDateTimePicker (plan 04)"
  - "assembleTime clamps each component (hour 0..23, minute/second 0..59) and zeroes seconds when showSeconds=false"

patterns-established:
  - "Internal time-fields primitive (TimeFields) + pure assembler (assembleTime) is the reuse seam for the DateTime picker"

metrics:
  duration: "resumed after reboot; ~6 min active work"
  tasks: 3
  files: 3
  completed: "2026-06-18"
---

# Phase 8 Plan 03: AeroTimePicker Summary

Public 24-hour `AeroTimePicker` (PICK-03) emitting `kotlinx.datetime.LocalTime` from hour/minute(/optional second) `AeroNumberSpinner` fields, with the spinner row extracted as an internal reusable `TimeFields` composable for `AeroDateTimePicker` to consume.

## What Was Built

- **`TimeFields.kt`** (internal, committed `18be38f` before reboot): a `Row` of `AeroNumberSpinner`s — hour (0..23), minute (0..59, step=`minuteStep`), and an optional second (0..59) field gated by `showSeconds`, separated by `":"` labels. Each spinner's `onValueChange` reassembles a `LocalTime` via the pure internal `assembleTime(hour, minute, second, showSeconds)` helper that clamps every component and drops seconds when `showSeconds=false`. Callbacks pass straight through with no debounce (NEW-PICK-04).
- **`AeroTimePicker.kt`** (public, committed `411cca6`): a read-only `AeroTextField` trigger with a `Clock` `AeroIconButton` (and optional clearable `X`) that toggles an anchored `Popup` (positioned by `AeroCalendarPositionProvider`, surfaced by the shared `PickerPopupContainer`) hosting `TimeFields`. Emits `onValueChange(LocalTime)` on every spinner change — no Apply gate. Uses `Popup` (never `Dialog`), never sets the transparency flag (W11-01).
- **`AeroTimePickerTest.kt`** (committed `22bfd5e`): 4 unit tests over `assembleTime` — seconds dropped/kept by `showSeconds`, above-range clamp to 23:59, below-range clamp to 00:00:00.

## Task Completion

| Task | Name | Commit | Status |
| ---- | ---- | ------ | ------ |
| 1 | TimeFields internal spinner row + assembleTime helper | `18be38f` | Pre-existing (committed before reboot) |
| 2 | AeroTimePicker public composable | `411cca6` | Completed this session (file was uncommitted in working tree) |
| 3 | AeroTimePicker / TimeFields LocalTime tests | `22bfd5e` | Completed this session |

## Verification

- `./gradlew :library:compileKotlin` — BUILD SUCCESSFUL.
- `./gradlew :library:test --tests "com.mordred.aero.components.pickers.AeroTimePickerTest"` — BUILD SUCCESSFUL (4 tests green).
- `grep use12Hour|AM_PM|AmPm|amPm` across both main files — 0 functional matches (the single hit is a KDoc descope note, not a parameter).
- `grep "transparent = true" | "androidx.compose.ui.window.Dialog"` in AeroTimePicker.kt — 0 matches.

## Deviations from Plan

None — plan executed as written. Tasks 1 and 2 were already on disk at resume (Task 1 committed at `18be38f`, Task 2 uncommitted in the working tree); both were verified correct against the plan before Task 2 was committed and Task 3 added.

## Resume Note

This plan was interrupted by a machine reboot mid-execution. On resume: Task 1's commit was confirmed (not redone); the uncommitted Task 2 file was read, compiled clean, and committed; Task 3 (tests) was written and committed. No work was duplicated.

## Self-Check: PASSED
