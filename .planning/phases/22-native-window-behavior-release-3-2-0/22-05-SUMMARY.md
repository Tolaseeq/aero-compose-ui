---
phase: 22-native-window-behavior-release-3-2-0
plan: 05
subsystem: windows-native
tags: [win32, wndproc, nccalcsize, stylechanging, win-01, win-03]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-02 native WndProc spike (subclass + registry); 22-01 probe harness and RED baseline frames; 22-04 passed early gate"
provides:
  - "Sticky frame styles: REQUIRED_FRAME_STYLES kept through AWT rewrites via WM_STYLECHANGING (WIN-03)"
  - "Production frame removal: WM_NCCALCSIZE wParam=1 — floating client = window, maximized client = window's own monitor work area minus a 2px inset on each auto-hide edge (WIN-01)"
  - "Pure Win32Geometry.kt (maximizedClientRect, resizeFramePx, PxRect, ScreenEdge, AUTO_HIDE_INSET_PX=2) with no JNA/Compose imports"
  - "Probe-side maximized-client measurement (ClientToScreen) replacing the misleading outer-rect comparison"
affects: [22-06, 22-07, 22-09, 22-10, 22-11, 22-13, 22-15]

tech-stack:
  added: []
  patterns:
    - "WM_STYLECHANGING (wParam=GWL_STYLE): rewrite STYLESTRUCT.styleNew with style or REQUIRED_FRAME_STYLES, then forward to CallWindowProc(previous)"
    - "Maximize geometry belongs to WM_NCCALCSIZE (client rect), not WM_GETMINMAXINFO (outer rect) — the outer rect keeps its sizing-border overhang by design until Plan 11"
    - "Auto-hide detection: ABM_GETSTATE + ABM_GETAUTOHIDEBAREX per edge against MonitorFromWindow(hwnd, NEAREST)'s own monitor — never a hardcoded primary monitor"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Geometry.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-05-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Chrome.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - tools/winprobe/WinProbe.ps1
    - tools/winprobe/Invoke-WinProbe.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "AUTO_HIDE_INSET_PX = 2 (Windows Terminal's production value); measured maximized client (0,0)-(1920,1078) vs rcWork (0,0)-(1920,1080) — bottom edge uncovered by exactly 2px"
  - "WM_NCACTIVATE lParam=-1 mitigation NOT needed: zero differing pixels in the title band across activation toggles"
  - "The ~8px border in the raw PrintWindow maximized frame is a capture-method artifact (PrintWindow renders the off-screen outer rect); on the physical display the visible overlap equals the client rect exactly"
  - "WIN-01 and WIN-03 stay Pending in REQUIREMENTS.md: WIN-01's reveal-on-hover half is Plan 15 (real input), WIN-03's corner/shadow clause is the maintainer's choice in Plan 06"

requirements-completed: []

duration: ~40min (interrupted once by an account hold; finished inline by the orchestrator)
completed: 2026-09-26
---

# Phase 22 Plan 05: Production window styles + frame removal — Summary

**The window keeps its native frame style bits for its whole lifetime, and the frame is removed correctly in both states: floating is pixel-identical to the pre-phase baseline, maximized fills exactly the monitor work area and leaves the auto-hide taskbar edge uncovered.**

## Performance

- Tasks: 3/3 (sticky styles; frame removal; live checks + conflict #3 detection half)
- `AERO_TEST_COUNT total=541 skipped=0` green after each library commit
- Evidence: `.captures/22-frame/` (rest, maximized, deactivated/reactivated frames, v11.json), 22-NOTES.md § Frame removal (22-05)

## Accomplishments

- V11-STYLE, V11-HT-MAXIMIZED-TOP, V11-MAX-WORKAREA, V11-AUTOHIDE-EDGE all PASS on the production code (RED → 8 pass / 10 fail; the remaining FAILs are edge/corner resize and min-size, owned by Plans 07/11).
- Title band (rows 0–31) and top-3-rows white-strip checks: 0 differing pixels vs the pre-phase baseline, before and after a WM_NCACTIVATE toggle — no ghost caption.
- Conflict #3 detection half settled: Bottom auto-hide edge detected via ABM on the window's own monitor, 2px inset, measured client rect recorded.

## Commits

- `aabea71` feat(22-05): keep native frame styles on AeroTitleBar windows (WIN-03)
- `1cefbab` feat(22-05): frame removal with taskbar-aware maximize (WIN-01, WIN-03)
- `852c7f6` fix(22-05): measure maximized client rect, not the outer window rect (WIN-01)
- `48b4b8c` docs(22-05): frame removal live checks (WIN-01, WIN-03)
- final docs commit — this SUMMARY

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] V11-MAX-WORKAREA measured the wrong rectangle**
- It compared `GetWindowRect` (outer rect, which keeps its sizing-border overhang by design until Plan 11's WM_GETMINMAXINFO work). The check now measures the client rect via ClientToScreen — what WM_NCCALCSIZE actually controls and what the user sees.

**2. [Rule 1 - Bug] V11-AUTOHIDE-EDGE was a placeholder that always FAILed**
- Replaced with the real per-edge 1–4px assertion reusing V11-MAX-WORKAREA's deltas.

**Total deviations:** 2 auto-fixed tooling bugs. **Impact:** none on the library.

## Issues Encountered

- The executor was interrupted mid-Task-3 by an Anthropic account hold after writing the notes and probe fixes but before the final verify/commit. The orchestrator verified the recorded evidence (all acceptance criteria already met) and finished the commit, SUMMARY and tracking inline.

## Next Phase Readiness

- Wave 4 continues: 22-08 (narrow showcase window). Plan 06 next wave: corner rounding / shadow decision — the maintainer chooses via preview if Windows starts rounding corners.

## Self-Check: PASSED

- `## Frame removal (22-05)` in 22-NOTES.md with V11-STYLE PASS; `.captures/22-frame/AeroBlue-rest.png` exists; library untouched since the two task commits; no showcase JVM left running.
