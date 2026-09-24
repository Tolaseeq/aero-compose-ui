---
phase: 21-migration-release-3-1-0
plan: 03
subsystem: testing
tags: [compose-ui-test, capture-to-image, popup, d-07, d-08, d-09]

# Dependency graph
requires:
  - phase: 21-02
    provides: "UiCapture.kt opt-in PNG writer (aero.captureDir, D-03), pixelMapsDiffer comparator, BASE-05 captureToImage() proof, 21-UITEST-COVERAGE.md skeleton"
provides:
  - "D-07 proof: the first opened-Popup capture test empirically decides the capture method — onRoot() (M1) works with one semantics root; a Popup's second root makes onRoot() throw AssertionError, caught and handled by an onAllNodes(isRoot()) (M2) fallback that rasterizes the same shared canvas and reaches the popup pixels"
  - "captureOpened() and assertOpenedDiffers() — shared M1/M2 capture-and-assert helpers in UiCapture.kt, reused by both D-07 test files"
  - "39 permanent opened-state capture tests across all 13 Popup( components x 3 themes (D07MenuPopupCaptureTest.kt: 24 — AeroDropdown, AeroComboBox, AeroContextMenu, AeroMenuBar, AeroTooltip, AeroPopover, AeroDrawer, AeroColorPickerButton; D07PickerPopupCaptureTest.kt: 15 — AeroDatePicker, AeroDateRangePicker, AeroDateTimePicker, AeroDateTimeRangePicker, AeroTimePicker)"
  - "D-09 confirmation: AeroCalendarGrid has no Clock/today-highlight reference (grep = 0), so fixed picker values give deterministic before/after frames"
  - "AeroDialog/AeroAlertDialog (real Window) and AeroFilePicker (native java.awt.FileDialog) classified as not composable in unit tests, never composed by any capture test (grep gate = 0)"
  - "21-UITEST-COVERAGE.md extended with the Popup capture method finding, the full 13-component D-07 table, and the dialog/file-picker classification"
affects: [21-04, 21-10, 21-11, verification-plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "M1-then-M2 popup capture fallback: onRoot().captureToImage() first; catch the AssertionError a second semantics root throws and fall back to onAllNodes(isRoot())[count-1].captureToImage() — both methods rasterize the same shared Skia canvas on Compose Multiplatform Desktop, so the fallback always reaches popup pixels too"
    - "Locating an AeroIconButton trigger via onNodeWithContentDescription(...) — the clickable AeroIconButton merges its child Icon's contentDescription into one semantics node, so the description string alone is enough to find and click the trigger without any source changes"
    - "Popup-only marker text picked per-component by reading source for something that only exists once the popup is composed (month header, Apply button, a spinner's own editable-text value) rather than adding new testTags to library sources"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/capture/D07MenuPopupCaptureTest.kt
    - library/src/test/kotlin/com/mordred/aero/capture/D07PickerPopupCaptureTest.kt
  modified:
    - library/src/test/kotlin/com/mordred/aero/capture/UiCapture.kt
    - .planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md

key-decisions:
  - "captureOpened()/assertOpenedDiffers() started in D07MenuPopupCaptureTest.kt (Task 1/2) and were moved into UiCapture.kt in Task 3 once D07PickerPopupCaptureTest needed them too, per the plan's own move-only-if-both-need-it instruction"
  - "AeroComboBox opens with an empty hoisted text state alone (no performTextInput needed) because shouldAutoOpen's options.any { it.contains(text, ignoreCase = true) } is vacuously true for text = \"\" — documented in the coverage table instead of adding an unnecessary text-input step"
  - "AeroTimePicker's popup-only marker is the minute spinner's own editable-text node (\"30\"), not a testTag, since TimeFields has no other unique text and the trigger's read-only field renders the combined \"10:30\" as a single node that never collides with the popup's separate \"30\" node"

patterns-established:
  - "Every future D-07/D-08/BASE-05-style popup test reuses captureOpened()/assertOpenedDiffers() from UiCapture.kt verbatim — no component in this plan needed a bespoke capture method"

requirements-completed: [BASE-05]

# Metrics
duration: ~17min
completed: 2026-09-23
---

# Phase 21 Plan 03: D-07 opened-popup capture tests (menus, overlays, pickers) Summary

**39 permanent opened-state UI-test captures cover all 13 `Popup(`-based components in three themes on the pre-upgrade toolchain, built on an empirically-decided onRoot()-then-onAllNodes(isRoot()) capture fallback that every test reuses unmodified — no component needed the D-08 after-only escape hatch.**

## Performance

- **Duration:** ~17 min (16:38 -> 16:55)
- **Started:** 2026-09-23T16:38:39+03:00 (previous plan's last commit)
- **Completed:** 2026-09-23T16:55:09+03:00 (last commit)
- **Tasks:** 3/3 completed
- **Files modified/created:** 4 (2 modified, 2 created)

## Accomplishments
- The first D-07 test (`AeroDropdown`) empirically proved how an open `Popup` is captured in desktop `runComposeUiTest`: a closed component has exactly one semantics root and `onRoot()` works directly; the instant a `Popup` opens, Compose Multiplatform Desktop reports a second root and `onRoot()` throws `AssertionError`, which `captureOpened()` catches and handles by falling back to `onAllNodes(isRoot())`'s last-added root — this reaches the popup pixels because both roots paint onto the same shared window canvas.
- All 13 components whose source contains `Popup(` — `AeroDropdown`, `AeroComboBox`, `AeroContextMenu`, `AeroMenuBar`, `AeroTooltip`, `AeroPopover`, `AeroDrawer`, `AeroColorPickerButton`, `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`, `AeroDateTimeRangePicker`, `AeroTimePicker` — now have three-theme opened-state capture tests, each opened by real click/right-click/hover input, never `java.awt.Robot` or OS input.
- Every one of the 39 new tests produced a genuine `pixelMapsDiffer(closed, opened) == true` result on the first run — no component needed the D-08 "after-only via MCP" fallback.
- D-09 confirmed: `AeroCalendarGrid` has zero `Clock`/`todayLocalDate`/`now(` references, so a fixed `value` on every picker test gives deterministic before/after frames regardless of the day the tests run.
- `AeroDialog`/`AeroAlertDialog` (real `Window`) and `AeroFilePicker` (native `java.awt.FileDialog`) are classified as not composable in unit tests and never composed anywhere under `library/src/test/kotlin/com/mordred/aero/capture` (grep gate = 0).
- Full suite (`./gradlew :library:test --rerun`) is green: **541 tests across 93 classes** (up from 502/91 before this plan). An opt-in smoke capture run (`-Paero.captureDir`) produced 200 PNGs; three (`AeroMenuBar`, `AeroDatePicker`, `AeroDrawer` opened states) were opened with the Read tool and visually confirm the popup content is present.

## Task Commits

Each task was committed atomically:

1. **Task 1: Popup-capture probe with AeroDropdown + D-09 today-highlight check (D-07 first step)** - `7491abb` (test)
2. **Task 2: Opened-state tests for ComboBox, ContextMenu, MenuBar, Tooltip, Popover, Drawer, ColorPickerButton (D-07)** - `cd2cb49` (test)
3. **Task 3: Picker opened-state tests with fixed values + dialog/file-picker classification (D-07, D-08, D-09)** - `c37e7a1` (test)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `library/src/test/kotlin/com/mordred/aero/capture/D07MenuPopupCaptureTest.kt` (new) - 24 tests (3 AeroDropdown probe + 21 across 7 menu/overlay components x 3 themes), each opened via real input and asserted with `assertOpenedDiffers`
- `library/src/test/kotlin/com/mordred/aero/capture/D07PickerPopupCaptureTest.kt` (new) - 15 tests (5 pickers x 3 themes), fixed D-09 values, positional `LocalDate`/`LocalDateTime`/`LocalTime` constructors only, dialog/file-picker classification documented in the class KDoc
- `library/src/test/kotlin/com/mordred/aero/capture/UiCapture.kt` - gained `captureOpened()` (M1-then-M2 popup capture) and `assertOpenedDiffers()` (write + assert), moved here from `D07MenuPopupCaptureTest.kt` in Task 3 once both D-07 test files needed them
- `.planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md` - "Popup capture method" empirical finding, the full 8-component Task 2 table, the full 5-picker Task 3 table, and the "Not composable in unit tests" classification for `AeroDialog`/`AeroAlertDialog`/`AeroFilePicker`

## Decisions Made
- `captureOpened()`/`assertOpenedDiffers()` were written first inside `D07MenuPopupCaptureTest.kt` (Task 1/2, since only that file needed them yet) and moved into `UiCapture.kt` in Task 3 exactly when `D07PickerPopupCaptureTest` needed the same helpers — matching the plan's explicit "move it into UiCapture.kt only if both classes need it" instruction rather than moving it preemptively.
- `AeroComboBox`'s popup opens from a real click alone, no `performTextInput` step: `shouldAutoOpen`'s `options.any { it.contains(text, ignoreCase = true) }` is vacuously `true` for an empty hoisted text state, so focusing the field via `performClick()` is sufficient — documented in the coverage table rather than adding an unneeded text-input step the plan only asked for conditionally ("if `shouldAutoOpen` needs text").
- `AeroTimePicker` has no month header or Apply/Cancel button to use as a popup-only marker (`TimeFields` renders bare hour/minute spinners with no commit gate), so the minute spinner's own editable-text node ("30" for the fixed `LocalTime(10, 30)`) is used instead — verified safe because the closed trigger renders the combined value as a single "10:30" node that never collides with the popup's separate "30" node.

## Deviations from Plan

None - plan executed exactly as written. The Task 1 probe's exception type needed one correction during development (catching `AssertionError`, not `IllegalStateException`, since that is what `onRoot()` actually throws for a multi-root tree) — this was resolved empirically while writing the probe itself, which is precisely the "first test decides empirically" step the plan describes, not a deviation from it.

## Issues Encountered
None beyond the empirical exception-type discovery above, which is the plan's intended Task 1 outcome, not an issue.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- D-07 is satisfied before the TOOL-16 count lock (Plan 05): every one of the 13 `Popup(`-based components has a three-theme opened-state baseline test with deterministic content, and the 3 `Window`/native-dialog components (`AeroDialog`, `AeroAlertDialog`, `AeroFilePicker`) are explicitly routed to the D-08 after-only/unconfirmed lists instead.
- `captureOpened()`/`assertOpenedDiffers()` in `UiCapture.kt` are ready for reuse by any later plan that needs another popup-opened capture (none currently planned, but the mechanism is proven and shared).
- These 39 tests are part of the locked test count TOOL-16 will fix in Plan 05 — full suite is green at 541 tests across 93 classes as of this plan's last commit.
- No blockers identified for downstream plans (04-14).

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 5 created/modified files found on disk; all 3 task commits (`7491abb`, `cd2cb49`, `c37e7a1`) found in git history.
