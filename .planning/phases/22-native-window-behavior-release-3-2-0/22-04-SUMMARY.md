---
phase: 22-native-window-behavior-release-3-2-0
plan: 04
subsystem: testing
tags: [real-input, snap-layouts, uia, winevent, ver-12, early-gate]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-02 native WndProc spike (HTMAXBUTTON / HTCAPTION through the child-to-frame chain); 22-03 real-input driver, flyout watcher and early-gate script"
provides:
  - "Agent-recorded proof that Windows 11 shows the Snap Layouts flyout over the spike Compose window's maximize button on a standard JDK 21, matched against a DefWindowProc positive control"
  - "Agent-recorded proof of native half-snap to the left edge and restore-on-drag for the spike window"
  - "Working early-gate tooling: own WinForms positive control, WinEvent-hook cloak-aware flyout watcher, per-hover event mark, evidence persisted in the gate JSON"
affects: [22-05, 22-07, 22-09, 22-14, 22-15]

tech-stack:
  added: []
  patterns:
    - "Flyout evidence = WinEvent SHOW/UNCLOAKED from explorer after the hover's own baseline mark, matched by class|process signature against a positive control hovered in the same run"
    - "Positive control for non-client behavior: an own WinForms Form in a child powershell -STA process (DefWindowProc frame), found by exact GUID title, max button found by a hit-test scan of the non-client caption row"
    - "A PowerShell function returning a collection that may hold one element returns it with the unary comma (return , $set)"

key-files:
  created:
    - tools/winprobe/PositiveControlHost.ps1
    - tools/winprobe/PositiveControlWindow.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-04-SUMMARY.md
  modified:
    - tools/winprobe/Watch-SnapFlyout.ps1
    - tools/winprobe/Invoke-EarlyGate.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "Early gate PASSED on the third real run: FLYOUT OK (804 ms, explorer XamlExplorerHostIslandWindow / DesktopWindowContentBridge under the showcase max button), SNAP OK (left half 0,0,960,1080), RESTORE OK (1200x800), standard JDK 21"
  - "Runs 1 and 2 failed on tooling only (Notepad positive control unusable on Win11; watcher blind to pre-created cloaked hosts; corner-zone drag target; cumulative event log; one-element set unrolled); the library build was identical across all three runs"
  - "No requirement marked complete here: SNAP-01 (quarter/top/maximize variants), SNAP-02 (layout selection) and VER-12 (full session) are proven by later plans"

requirements-completed: []

duration: ~75min (three maintainer-authorized real-input runs and two tooling fixes)
completed: 2026-09-25
---

# Phase 22 Plan 04: Early real-input gate — Summary

**The existential gate passed: Windows 11 shows the Snap Layouts flyout over the Compose window's maximize button and snaps/restores the window natively, on a standard JDK 21.**

## Performance

- Tasks: 3/3 (readiness, maintainer "ok" checkpoint, gate run); the gate ran three times, each under its own explicit "ok"
- Real input per run: 29.0 s, 34.7 s, 36.1 s; cursor restored every time
- Evidence: `.captures/22-gate/early.json`, `early2.json`, `early3.json` (+ console logs); 22-NOTES.md § Early gate (22-04)

## Accomplishments

- FLYOUT OK: explorer's `XamlExplorerHostIslandWindow` uncloaked with a `DesktopWindowContentBridge` flyout (344×244) centred under the showcase's maximize button 804 ms after the hover; same signature as the DefWindowProc positive control (666 ms).
- SNAP OK: drag to the middle of the left edge gives exactly the left half of the work area; RESTORE OK: dragging away restores 1200×800.
- The early-gate tooling is now trustworthy for the full session (22-14 / 22-15 reuse it).

## Commits

- `6614376` docs(22-04): early gate readiness (VER-12)
- `cbc4825` docs(22-04): early real-input gate result (VER-12) — run 1
- `c97570f`, `4ed4386`, `91b6a1b`, `aeebfc8`, `13660a4` — tooling fix after run 1 (positive control, WinEvent watcher, batched lookups, half-edge drag, host survey)
- `1357137` fix(22-04): judge each hover only on its own events, fix one-element signature match — after run 2
- final docs commit — run 3 GATE PASS + this SUMMARY

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Positive control could not find Notepad's maximize button**
- Win11 Notepad is WinUI with no HTMAXBUTTON span, and the scan looked below the client origin. Replaced by an own WinForms window and a non-client row scan.

**2. [Rule 1 - Bug] Watcher blind to the real flyout host**
- The flyout host is pre-created and cloaked; only brand-new HWNDs were counted. Added a WinEvent hook (SHOW/UNCLOAKED) and cloak-aware snapshots.

**3. [Rule 1 - Bug] Drag target in the corner zone**
- Released at (1,64), so Windows correctly quarter-snapped. Now released at the vertical middle of the edge.

**4. [Rule 1 - Bug] Cumulative event log and unrolled one-element set**
- The Compose hover counted the positive control's events, and the signature comparison crashed. Fixed with a per-hover event mark and `return , $set`; covered by an offline logic test before run 3.

**Total deviations:** 4 auto-fixed tooling bugs. **Impact:** two extra maintainer sessions (about 30 s of input each); no library change.

## Issues Encountered

- Two tool approvals for the real run were rejected once each before starting; no input was sent on a rejected call, and the run started only after the maintainer's follow-up.

## Next Phase Readiness

- Global stop rule not triggered; the phase continues. Wave 4: 22-05 (production window styles + frame removal) and 22-08 (narrow showcase window).

## Self-Check: PASSED

- 22-NOTES.md contains `GATE PASS`, both flyout signatures and the snap/restore rects; `.captures/22-gate/early3.json` exists; no showcase JVM or positive-control process remains.
