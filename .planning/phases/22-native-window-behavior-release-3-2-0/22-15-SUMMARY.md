---
phase: 22-native-window-behavior-release-3-2-0
plan: 15
subsystem: verification
tags: [ver-12, real-input, snap, win-behavior, both-jvms]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "All mechanics (22-05..22-12, 22-19); the dry-run-proven session suite (22-14); vetted environment (22-03)"
provides:
  "Full real-input VER-12 session on both JVMs with agent-recorded evidence; C2 and C3 settled; honest PASS/FAIL/UNCONFIRMED per check on both passes; the gap list that drives gap closure"
affects: [22-16, gap-closure planning, 22-17, 22-18]

tech-stack:
  added: []
  patterns:
    - "Session evidence = scripted real input with per-check JSON records, UIA flyout signatures against a positive control, PrintWindow frames — never impressions"
    - "Environment commands from a vetted doc must be live-tested before the maintainer's hands-off window; three of SessionEnv's commands needed fixing on first live run (d30e605, 0dd682e)"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION.md
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-15-SUMMARY.md
  modified:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md
    - tools/winprobe/SessionEnv.ps1
    - tools/winprobe/Invoke-FullSession.ps1

key-decisions:
  - "C2 SETTLED: Alt+Space is NOT automatic on the native path; the known fix is hand-declared GetSystemMenu/TrackPopupMenu — gap closure material"
  - "C3 fully SETTLED: the auto-hide taskbar reveals over the maximized window in ~230 ms with the 2 px inset, on both JVMs"
  - "Maintainer chose (2026-09-28): close the real gaps BEFORE the release — Alt+Space system menu, caption double-click, shared-border resize, max-button press fill, the JBR-only drag/Win+arrow anomaly, plus the 8 check-formula fixes; then a short re-verification session; only then 22-18"
  - "VER-12's early gate + full session both ran → VER-12 Complete; SNAP/BTN/WIN requirement completion is split: proven clauses stay proven, failed clauses go to gap closure (requirements stay Pending until their clauses fully pass)"
  - "W04 (150% virtual display) stays UNCONFIRMED on this machine: the driver installs and Windows reports it running, but no monitor appears (two attempts, including with its control app); S02-LAYOUT-PICK (flyout zones not UIA-clickable) and S06-SNAP-GROUP (no taskbar preview in 2 s) also UNCONFIRMED"

requirements-completed: []  # VER-12 completes in 22-16 (driver removal + PowerToys-per-word clauses)

duration: ~2h wall (8.8 min of actual input checks; the rest was first-live-run environment repair)
completed: 2026-09-28
---

# Phase 22 Plan 15: Full real-input session (JDK 21 + JBR 21) — Summary

**The session ran end-to-end with agent-recorded evidence on both JVMs. The core native behaviors are proven; 4 real library gaps and 1 JBR-only anomaly were found and go to gap closure; 8 FAILs are check-formula artifacts with the behavior actually proven; 3 checks stay UNCONFIRMED for environmental reasons.**

## Performance

- Pass 1 (standard JDK 21, ms-21.0.9): 31 PASS / 18 FAIL / 5 UNCONFIRMED of 54
- Pass 2 (JBR 21.0.9): 24 PASS / 25 FAIL / 5 UNCONFIRMED of 54
- Real input: 272.8 s + 255.2 s; cursor restored; auto-hide restored (byte 8 = 0x03); primary monitor back at 100% after the scale-guard incident (caught and reverted by the session's own safety net)
- Evidence: `.captures/22-session/{jdk,jbr}/results.json`, summary.json, console logs; 22-SESSION.md per-pass tables

## What is proven (highlights)

Edge/corner/top snapping + restore and Win+arrows on JDK 21; the Snap Layouts flyout via UIA signature match on BOTH JVMs; auto-hide taskbar reveals over the maximized window (~230 ms, C3 settled); frame removal/corners visually maintainer-consistent; window independence; open/close during drag; live flicker-free resize; all four caption buttons by real clicks; D-01 minimum floors clamping at exactly 320/260 (behavior proven, formula was wrong).

## Gaps found (gap closure, maintainer-approved before release)

1. S05 Alt+Space system menu does not open on the native path (works on opt-out) → hand-declared GetSystemMenu/TrackPopupMenu (C2's recorded fix path); Move/Size/Minimize/Maximize/Close-via-menu ride on it. Alt+F4 works.
2. S04 caption double-click does not maximize.
3. S06 shared-border resize of two snapped windows does nothing.
4. B01 max-button PRESS fill differs from minimize (hover identical — the pre-agreed C5 fallback condition).
5. JBR-only: caption drags and Win+arrows were inert in this run while clicks/edge-resize/Shift-drag worked; cause unknown from the evidence — investigation item.

## Check-formula artifacts (behavior proven, FAIL wrong)

W02 corners (4) — diagonal 60×60 growth with correct cursor is correct corner behavior; W02 floors (2) — width clamped exactly at the floor, height cannot change in a left-edge drag; S07-FANCYZONES — Shift-drag landed exactly in a real priority-grid zone, heuristic assumed 2×2; W06-CLOSE-DURING-DRAG — close reached, window alive, only the ncdestroy trace timing missed; S03-WIN-DOWN — ordering artifact (standalone S03-WIN-UP passed seconds earlier).

## UNCONFIRMED

W04 trio (virtual 150% display never materialized — driver installs, no monitor, two attempts); S02-LAYOUT-PICK (flyout zones not clickable via UIA); S06-SNAP-GROUP (no taskbar preview within 2 s).

## Commits

- e1ce688 fix(22-15): correct VDD Control.exe SHA-256 constant in SessionEnv (VER-12)
- d30e605 fix(22-15): session env hardening from the first live driver run (VER-12)
- 0dd682e fix(22-15): correct PowerToys paths, scale guard, zone parser for live env (VER-12)
- 0c8a6da docs(22-15): full real-input session results, JDK 21 and JBR 21 (VER-12)
- final docs commit — this SUMMARY

## Deviations from Plan

- First session attempt aborted at ~2 s on the corrupted hash constant with zero side effects; retried cleanly.
- The environment steps from the vetted doc were executed live for the first time inside the maintainer's hands-off window; three needed fixes (committed). The session took ~2 h instead of the estimated 19 min — the estimate formula will be revised for any future session (measure live-tested env commands, not vetted-doc assumptions).

## Issues Encountered

- Scale-miss incident on pass 2 setup: 150% briefly applied to the PRIMARY monitor; the session's own guard caught it, it was reverted immediately and verified (96 DPI, 1920×1080), and a dedicated guard was added. Maintainer informed in the return message.

## Next Phase Readiness

- Maintainer decision recorded: gap closure before release. Next: 22-16 (teardown + unconfirmed list + build), 22-17 (docs), then `/bm:plan-phase 22 --gaps`, gap execution + short re-verification session, then 22-18 (release).

## Self-Check: PASSED

- 22-SESSION.md has both passes with evidence tables; C2/C3 SETTLED in 22-NOTES.md; auto-hide equals the recorded original; no leftover showcase JVM; VER-12 marked complete (both named sessions ran and passed their session-level criteria); the return message to the maintainer started with «Можно возвращаться».
