---
phase: 22-native-window-behavior-release-3-2-0
plan: 06
subsystem: windows-native
tags: [win32, dwm, corners, shadow, win-03, visual-companion]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-05 production frame removal (baseline-identical title band); 22-01 probe + baseline frames; maintainer's Visual Companion rule"
provides:
  - "Win32Dwm.kt — hand-declared dwmapi (DwmSetWindowAttribute / DwmGetWindowAttribute / DwmExtendFrameIntoClientArea, MARGINS), WindowCornerLook enum, CHOSEN_CORNER_LOOK = variant A (DWMWCP_DONOTROUND, 0 margins)"
  - "applyCornerPolicy at install and on WM_SIZE SIZE_MAXIMIZED / SIZE_RESTORED after CallWindowProc — square while maximized (PITFALLS 24)"
  - "Conflict #6 settled: Windows 11 did not round corners by default on this machine (maintainer's observation); the look is explicit and read-back-verified"
affects: [22-10, 22-13, 22-15, 22-16, 22-17]

tech-stack:
  added: []
  patterns:
    - "DWM corner policy: apply explicitly, never rely on the OS default; read back via DwmGetWindowAttribute and trace HRESULTs"
    - "WM_SIZE handling runs AFTER CallWindowProc(previous) — AWT owns the WindowState sync pipeline (C4)"
    - "PrintWindow cannot observe compositor effects (corners/shadow) — visual observations of window chrome outside the content are maintainer-only by construction"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Dwm.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-corners-preview.html
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-06-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - .gitignore
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "Maintainer chose variant A (square corners, no shadow) from the Visual Companion preview with the real window open; observation: corners not rounded by default, shadow unobserved"
  - "The look is applied explicitly (DWMWCP_DONOTROUND + 0 margins) so it is deterministic across Windows builds"
  - "WIN-03 stays Pending in REQUIREMENTS.md: the remaining WIN-03 verification is part of the phase-level VER-11 GREEN and the Plan 15 session (22-05 precedent)"

requirements-completed: []

duration: ~30min across two executor runs (interrupted by a usage limit) plus orchestrator checkpoint handling
completed: 2026-09-27
---

# Phase 22 Plan 06: Window corners and shadow — Summary

**The window's corner/shadow look is the maintainer's explicit choice — variant A (square, no shadow) — applied through DWM attributes and verified by read-back; the title band is still pixel-identical to the pre-phase baseline.**

## Performance

- Tasks: 3/3 (agent self-review + preview; maintainer checkpoint; DWM application + read-back)
- `AERO_TEST_COUNT total=541 skipped=0` green after the library change
- Evidence: `.captures/22-corners/` (three-scheme sweep, dwm-check.json, AeroBlue-rest-dwm.png), 22-NOTES.md § Corners and shadow (22-06) + C6 SETTLED

## Accomplishments

- Self-review before the maintainer saw anything: title bands identical to the baseline in all three schemes (0 differing pixels), recorded fact that PrintWindow cannot answer conflict #6.
- Visual Companion preview (ВАРИАНТ A/B/C) built from the real AeroBlue tokens; maintainer observed the live window (corners not rounded by default) and chose A.
- DWM attributes applied explicitly and read back as S_OK + DONOTROUND at install, after SC_MAXIMIZE and after SC_RESTORE; maximize geometry unchanged.

## Commits

- `99872ec` docs(22-06): window corner/shadow preview and self-review (WIN-03)
- `50317d3` docs(22-06): record the maintainer's corner/shadow choice (WIN-03)
- `2a5fedf` feat(22-06): window corners and shadow per maintainer's choice (WIN-03)
- `4b7dc2d` docs(22-06): C6 settled — observation, choice, DWM read-back (WIN-03)
- final docs commit — this SUMMARY

## Deviations from Plan

None in content. Two execution interruptions: the Task 3 executor died on a usage limit with the library edits complete but unverified/uncommitted; the orchestrator verified (compile, 541 tests, read-back probe run, title-band comparison), recorded C6 and committed inline.

## Issues Encountered

- None technical; two scratch-script misuses by the orchestrator (capture return value used as a path; `MaxDelta` vs `MaxChannelDelta`) were corrected before the final measurements.

## Next Phase Readiness

- Wave 6: 22-07 (live hit-test regions from AeroTitleBar's layout, replacing the spike's hardcoded rectangles).

## Self-Check: PASSED

- `CHOSEN_CORNER_LOOK = SQUARE_NO_SHADOW`; read-back trace shows DONOTROUND floating and maximized; title band 0-diff vs baseline; C6 SETTLED in 22-NOTES.md; 541 green; no showcase JVM left running.
