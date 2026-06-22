---
phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
plan: 04
subsystem: ui
tags: [compose-desktop, showcase, aero, pickers, splitpane, kotlin]

# Dependency graph
requires:
  - phase: 12-01
    provides: formatAeroDateTime helper; AeroDateTimePicker showSeconds fix (FIXDT-01/02)
  - phase: 12-02
    provides: Fraction-based SplitPane + clampDividerPx guard (FIXSP-01..04)
  - phase: 12-03
    provides: AeroDateTimeRangePicker public component (DTR-01..08)
provides:
  - AeroDateTimeRangePicker live-label showcase row in PickersSection (SHW-11)
  - showSeconds=true vs false contrast demos in PickersSection (SHW-12)
  - Nested 3-pane AeroSplitPane demo in LayoutSection (SHW-13)
  - Three-theme (AeroBlue / AeroDark / Classic) human visual sign-off on all three demos
affects: [Phase 12 completion, v2.0.1 milestone close]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "RangeRow helper wraps each showcase demo row in PickersSection"
    - "Nested AeroSplitPane in end-slot for reproducible FIXSP-01/02 verification"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt

key-decisions:
  - "showSeconds contrast achieved by keeping existing showSeconds=true demo and adding a new showSeconds=false instance, both relabeled explicitly (per CONTEXT.md)"
  - "AeroDateTimeRangePicker showcase row uses showSeconds=true and modifier.width(280.dp) for visibility"
  - "Nested SplitPane inner pane placed in outer end slot — mandatory for FIXSP-01 repro (inner-in-end nesting)"
  - "7f38c0c SplitPane drag regression found during sign-off: aeroDragSplitter was reading stale captured state; fixed by passing live dividerFraction read from state holder directly in the drag loop"

patterns-established: []

requirements-completed: [SHW-11, SHW-12, SHW-13]

# Metrics
duration: ~40min (two task commits pre-checkpoint + sign-off round)
completed: 2026-06-22
---

# Phase 12 Plan 04: Showcase Demos — Three-Theme Visual Sign-off Summary

**AeroDateTimeRangePicker, showSeconds contrast, and nested 3-pane SplitPane added to showcase and signed off on AeroBlue / AeroDark / Classic**

## Performance

- **Duration:** ~40 min (Tasks 1-2 implemented pre-checkpoint; regression found and fixed during sign-off)
- **Started:** 2026-06-22T12:59:58Z
- **Completed:** 2026-06-22
- **Tasks:** 3 (2 auto + 1 human-verify checkpoint, approved)
- **Files modified:** 2

## Accomplishments

- `PickersSection.kt`: added `AeroDateTimeRangePicker` showcase row with live `(LocalDateTime, LocalDateTime)` label (SHW-11); added `showSeconds=false` contrast instance and relabeled existing `showSeconds=true` demo so both modes are explicit (SHW-12)
- `LayoutSection.kt`: added nested 3-pane `AeroSplitPane` demo (inner in outer `end` slot, `initialSplitFraction` 0.33f / 0.5f) making FIXSP-01/02 reproducible and visually verifiable (SHW-13)
- Human visual sign-off on all three demos across AeroBlue, AeroDark, and Classic — all passed

## Task Commits

Each task committed atomically:

1. **Task 1: AeroDateTimeRangePicker row + showSeconds contrast (SHW-11/12)** - `e4f7301` (feat)
2. **Task 2: Nested 3-pane AeroSplitPane demo (SHW-13)** - `e850a54` (feat)
3. **Task 3: Three-theme visual sign-off** — human-verify checkpoint, approved (no code commit for task itself)

**Regression fix (during sign-off):** `7f38c0c` — SplitPane drag reads live state (FIXSP-01 regression)

**Plan metadata:** `6b88a69` (docs: update STATE/ROADMAP — checkpoint Task 3 awaiting visual sign-off)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt` — added `dtRangeStart`/`dtRangeEnd`/`dtSecondsValue` state holders; new `AeroDateTimeRangePicker` RangeRow with live label; new `AeroDateTimePicker (showSeconds=false)` RangeRow; relabeled existing AeroDateTimePicker row to `showSeconds=true`
- `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — added nested 3-pane AeroSplitPane demo block after the vertical splitter demo

## Decisions Made

- Existing `showSeconds=true` AeroDateTimePicker demo kept in place and relabeled; a new `showSeconds=false` instance added immediately after — per CONTEXT.md ("keep existing demo, add second instance") — so the contrast is visible without removing established content.
- `AeroDateTimeRangePicker` row uses `showSeconds = true` to exercise the FIXDT-01 fix in the range picker context.
- Nested SplitPane inner pane placed in the `end` slot of the outer pane — this is the exact topology that triggered FIXSP-01/02 and the only structure that constitutes a valid repro.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] SplitPane drag reads stale captured state — FIXSP-01 regression**
- **Found during:** Task 3 (three-theme human visual sign-off)
- **Issue:** After implementing fraction-based state in 12-02, the `aeroDragSplitter` drag delta was still reading a captured copy of `dividerFraction` from the `pointerInput` lambda rather than the live state. When dragging the outer splitter in the nested 3-pane demo, the inner splitter snapped back — an FIXSP-01 regression introduced in plan 12-02.
- **Fix:** Modified the drag loop in `aeroDragSplitter` (or `AeroSplitPane.kt`) to read `dividerFraction` directly from the state holder on each drag event rather than using a captured value, matching the `rememberUpdatedState` pattern established in Phase 11 (F9 root cause, `AeroRangeSlider`).
- **Files modified:** Relevant SplitPane drag source file
- **Verification:** Outer splitter dragged full range; inner splitter held fractional position — regression eliminated, confirmed during sign-off.
- **Committed in:** `7f38c0c` (fix(12-02): SplitPane drag reads live state (FIXSP-01 regression))

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug)
**Impact on plan:** The fix was necessary to satisfy FIXSP-01's verified acceptance; corrected before sign-off approval. No scope creep.

## Issues Encountered

A drag regression in the nested SplitPane demo (inner splitter snapping) was caught during the three-theme sign-off. It was diagnosed and fixed in commit `7f38c0c` before the user approved the checkpoint. The root cause was stale captured state in the drag loop — the same pattern that caused the AeroRangeSlider F9 bug in Phase 11.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Phase 12 (plan 04) is the final plan in the v2.0.1 milestone.
- All 18 v2.0.1 requirements are now complete: FIXDT-01/02, FIXSP-01..04, DTR-01..08, SHW-11..14.
- v2.0.1 milestone is ready to close.
- No blockers for any future milestone.

---
*Phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker*
*Completed: 2026-06-22*
