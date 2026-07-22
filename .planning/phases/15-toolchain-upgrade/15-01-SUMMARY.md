---
phase: 15-toolchain-upgrade
plan: 01
subsystem: testing
tags: [compose-multiplatform, showcase, visual-regression, screenshots, skia]

requires:
  - phase: 14
    provides: showcase app on Kotlin 2.1.21 + CMP 1.7.3 (pre-migration toolchain)
provides:
  - Pre-migration visual baseline (7 PNG screenshots, 3 themes) for the TOOL-06 before/after diff
affects: [15-04, toolchain-upgrade, visual-diff]

tech-stack:
  added: []
  patterns:
    - "Baseline screenshots captured at fixed 1920x1080 @100% DPI, 1:1 with screen pixels, for reproducible before/after Skia-drift diffs"

key-files:
  created:
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-AeroBlue-01-top.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-AeroBlue-02.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-AeroBlue-03.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-AeroDark-01-top.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-AeroDark-02-controls.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-Classic-01-top.png
    - .planning/phases/15-toolchain-upgrade/baseline/baseline-Classic-02-controls.png
  modified:
    - .planning/phases/15-toolchain-upgrade/baseline/README.md

key-decisions:
  - "Captured at a fixed 1920x1080 maximized framing (not free-form manual shots) so plan 04's after-pass can reproduce identical framing for a clean pixel diff"
  - "Whole-page section captures (2-3 per theme) rather than per-component crops — every one of the 8 targets appears in >=1 shot per theme while keeping framing reproducible"

patterns-established:
  - "windows-mcp desktop automation for showcase visual capture: gradlew :showcase:run, maximize to left monitor, in-app ThemeSwitcher tab-click per theme, scroll through sections, CopyFromScreen 1920x1080 PNG"

requirements-completed: [TOOL-06]

coverage:
  - id: D1
    description: "Pre-migration reference screenshots of the showcase on all three themes (AeroBlue, AeroDark, Classic) covering the eight target components, on Kotlin 2.1.21 + CMP 1.7.3"
    requirement: "TOOL-06"
    verification:
      - kind: automated_ui
        ref: "baseline/baseline-AeroBlue-*.png, baseline-AeroDark-*.png, baseline-Classic-*.png (7 files, 1920x1080)"
        status: pass
    human_judgment: true
    rationale: "This is only the 'before' half of TOOL-06. The actual pass/fail is the before/after diff performed in plan 04 on the migrated toolchain; a human must judge whether any post-migration Skia rendering drift is acceptable. The baseline itself is verified present and correct, but TOOL-06 is not satisfied until plan 04's diff."

duration: ~15min
completed: 2026-07-22
status: complete
---

# Phase 15 / Plan 01: Pre-Migration Visual Baseline Summary

**Seven 1920x1080 showcase reference screenshots across AeroBlue/AeroDark/Classic on the un-bumped Kotlin 2.1.21 + CMP 1.7.3 toolchain, covering all eight components restyled later this milestone — the "before" half of the TOOL-06 diff.**

## Performance

- **Duration:** ~15 min (capture session)
- **Completed:** 2026-07-22
- **Tasks:** 2 (Task 1 auto, Task 2 human-verify checkpoint)
- **Files modified:** 8 (7 PNGs created + README updated)

## Accomplishments
- Confirmed the showcase compiles green on the current pre-migration toolchain and the version bump had NOT yet happened (`gradle/libs.versions.toml` still `kotlin = "2.1.21"`, `composeMultiplatform = "1.7.3"`) at capture time.
- Captured 7 reference screenshots (1920x1080 @100% DPI) covering all three themes; every one of the 8 target components (AeroButton, AeroOutlinedButton, AeroSwitch, AeroSegmentedControl, AeroSlider, AeroRangeSlider, AeroProgressBar, AeroListItem) appears in at least one shot per theme.
- Recorded the file->component coverage map and the exact framing plan 04 must reproduce, in `baseline/README.md`.

## Task Commits

1. **Task 1: Confirm toolchain + scaffold baseline dir** - `27276a9` (feat)
2. **Task 2: Capture before-migration screenshots (3 themes)** - `681141a` (feat)

## Files Created/Modified
- `baseline/baseline-{AeroBlue,AeroDark,Classic}-*.png` - 7 reference screenshots (see README table for per-file component coverage)
- `baseline/README.md` - capture record: method, resolution, file->component map, note for plan 04

## Decisions Made
- **Capture method:** Performed via windows-mcp desktop automation (CursorTouch/windows-mcp) rather than the manual human capture the plan anticipated — the app was launched with `gradlew :showcase:run`, maximized to the left monitor, themed via the in-app `ThemeSwitcher` tabs, and each section frame saved with `Graphics.CopyFromScreen` at 1920x1080. Equivalent deliverable, but reproducible framing for the plan 04 diff.
- **Fixed framing over free-form shots:** all captures are 1:1 with screen pixels at a fixed maximized size so the after-pass can align pixel-for-pixel.

## Deviations from Plan
Task 2 was specified as a manual human-verify checkpoint ("Claude cannot capture desktop-window screenshots"). It was instead completed by Claude via the windows-mcp desktop-automation MCP server (connected mid-session at the user's request). The deliverable and acceptance criteria are unchanged and met; only the capture actor changed. User will independently re-verify the screenshots.

## Issues Encountered
- windows-mcp was initially not attached to the session (registered under a wrong-cased project key, then a mid-session add did not hot-load). Resolved by registering the server at user (global) scope and restarting the session, after which its tools loaded.

## Next Phase Readiness
- The "before" baseline is committed and ready for plan 04's after-migration diff.
- **Nothing has been bumped yet** — the toolchain is still Kotlin 2.1.21 + CMP 1.7.3. The next plan (15-02) is the load-bearing BUILD GATE (TOOL-01) and is a HARD STOP: if the Kotlin 2.4.10 + CMP 1.11.1 pairing will not compile after the three known-safe fixes, escalate to the user with the three named fallbacks — never pick one autonomously.

---
*Phase: 15-toolchain-upgrade*
*Completed: 2026-07-22*
