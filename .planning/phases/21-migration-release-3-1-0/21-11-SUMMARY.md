---
phase: 21-migration-release-3-1-0
plan: 11
subsystem: verification
tags: [compose-hot-reload, mcp, printwindow, drift, handoff, d-04, d-08, ver-09]

# Dependency graph
requires:
  - phase: 21-10
    provides: "21-COMPARE.md, compare-showcase.json / compare-uitest.json, highlights, contact sheets, D-05 run-to-run verdicts"
provides:
  - "After-only MCP captures of AeroDialog and AeroAlertDialog in all three themes (D-08)"
  - "21-DRIFT.md: 102 outside-noise differences in 8 cause groups, each with paths, cause and proposed action"
  - "21-UNCONFIRMED.md: VER-09 list (incl. 125% / 200% scales, 96 DPI actually used, AeroFilePicker, after-only dialogs)"
  - "tools/capture/New-HandoffPage.ps1 and the offline page .captures/handoff/index.html (AERO_HANDOFF OK 301 images)"
  - "The maintainer's D-04 ruling on the complete list, recorded verbatim"
affects: [21-12, 21-13, 21-14]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dialog watcher: poll every 50 ms for a new top-level window of the app PID, send it to HWND_BOTTOM without activation, capture with PrintWindow, record the foreground owner before / at appearance / after; if the app took the foreground, stop the app right after the capture"
    - "D-08 launches use hotRun in capture mode (-Paero.capture=true) so the showcase's own start-up can never take focus; MCP semantics clicks still work on the non-focusable window"

key-files:
  created:
    - .planning/phases/21-migration-release-3-1-0/21-DRIFT.md
    - .planning/phases/21-migration-release-3-1-0/21-UNCONFIRMED.md
    - tools/capture/New-HandoffPage.ps1
  modified:
    - .planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md

key-decisions:
  - "D-04 stop triggered (102 items). Maintainer reply, verbatim: «всё принимаем». Read as `accept` for all 8 groups: groups 1, 3, 4 keep the new rendering; groups 2, 5, 6 extend the named noise regions; groups 7, 8 (capture-tooling flakes) get no tooling change and are recorded as known capture limitations"
  - "An AeroDialog window opened through MCP takes the foreground when the maintainer has been idle past the Windows foreground-lock timeout (2 of 6 openings, both Classic). This is correct dialog behaviour, not a library defect; for MCP-driven checks it is interference, so the watcher now stops the app immediately after the capture"

requirements-completed: [VER-09]

# Metrics
duration: about 110 min (12:55–14:45 local, incl. the focus stop and the maintainer's ruling)
completed: 2026-09-24
---

# Phase 21 Plan 11: After-only inspection, drift list, hand-off page Summary

**Both Window-based dialogs were opened through MCP in all three themes and render correctly on the new toolchain. The 102 outside-noise differences from Plan 10 are on one list in 8 cause groups, with an offline before/after page. The maintainer ruled on the whole list at once: accept everything.**

## Performance

- **Duration:** about 110 min
- **Started:** 2026-09-24T12:55 local (after `72960d4`)
- **Completed:** 2026-09-24 (after the ruling)
- **Tasks:** 3/3 (Task 1 inline by the orchestrator (MCP), Task 2 by an executor, Task 3 the maintainer's ruling)
- **Files modified:** 4

## Accomplishments

- **Task 1 (D-08):** 6/6 captures (`mcp-after-only/{AeroDialog,AeroAlertDialog}/{AeroBlue,AeroDark,Classic}/opened.png`), each viewed and described in `21-UITEST-COVERAGE.md` as "inspected without baseline". AeroFilePicker was never clicked and `take_screenshot` was never called. No orphan processes were left.
- **Global stop rule fired once:** AeroDialog/Classic took the foreground from the idle maintainer for about 8 s. The agent closed the app and asked. The maintainer chose to capture the last dialog anyway ("снимай щас"). That capture also took the foreground, and the watcher stopped the app about 1 s later.
- **Task 2:** `21-DRIFT.md` has 102 items (75 showcase + 27 UI-test keys, cross-checked against both JSONs) in 8 groups. `21-UNCONFIRMED.md` covers the VER-09 minimum. `New-HandoffPage.ps1` reports `AERO_HANDOFF OK 301 images` with no absolute `src` paths.
- **Task 3 (D-04):** the page was opened in the maintainer's browser, and the ruling on all 8 groups was received and is recorded below.

## Task Commits

1. **Task 1: after-only MCP inspection (D-08)** - `2619cc1` (docs)
2. **Task 2: drift list, unconfirmed list, hand-off page generator** - `712845e` (docs)
3. **Task 3: maintainer ruling** - recorded in this SUMMARY (transcribed into `21-DRIFT.md`'s Ruling column by Plan 12 Task 1)

## D-04 ruling (verbatim)

> всё принимаем

Interpretation used by Plan 12 (stated back to the maintainer when recorded): `accept` for every group.

| Group | Cause | Keys | Ruling | Effect |
|---|---|---|---|---|
| 1 | glyph anti-aliasing shift (CMP 1.12.0) | 82 | accept | keep new rendering |
| 2 | same shift, not byte-stable on one key | 1 | accept | extend noise regions |
| 3 | popup border/shadow-edge AA shift | 6 | accept | keep new rendering |
| 4 | caret colour 1 px shift in AeroComboBox | 3 | accept | keep new rendering |
| 5 | AeroProgressBar shimmer, wider footprint | 3 | accept | extend noise regions |
| 6 | recompose counter + nearby glyphs, wider footprint | 5 | accept | extend noise regions |
| 7 | scroll-settle capture flake (Classic/Layout/p3) | 1 | accept | no tooling change; known capture limitation |
| 8 | real-cursor hover during capture (Classic/Data/p1) | 1 | accept | no tooling change; known capture limitation |

## Deviations from Plan

- **Task 1 ran with hotRun in capture mode.** The plan's launch line has no capture flag. The capture flag keeps the showcase window non-focusable and behind other windows, so the launch itself cannot take focus from the maintainer (21-HRM.md recorded 1 of 2 normal launches doing so). MCP clicks work the same on the non-focusable window, and the dialogs are separate library `Window`s that the flag does not affect.
- **Fresh state per dialog via MCP `restart`** within a theme instead of a new Gradle launch; a new launch per theme.
- **The foreground check counts only the app's own process.** Following the maintainer's instruction that their own activity must never stop the agent (and `9834e8c`), a foreground change to another program is not a violation.
- **"Repeat once to confirm" was not done** after the first focus take: repeating would take focus from the maintainer again. The maintainer decided on the last dialog instead.
- **Task 2 split:** executed by an executor, with the orchestrator presenting Task 3.

**Total deviations:** 5 execution adjustments, no scope change. **Impact:** all acceptance criteria met; the one extra interruption (the focus stop) was required by the global stop rule.

## Issues Encountered

- The first stop-by-PID check in the orchestrator's helper did not recognise the app relaunched by MCP `restart` (argfile command line). It was widened to match the showcase path. No process was left behind.

## Next Phase Readiness

- Plan 21-12: transcribe the ruling into `21-DRIFT.md`, extend the noise regions for groups 2/5/6, re-compare the affected frames, then the GUI hand-off to the maintainer (VER-10).

## Self-Check: PASSED

- `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/` has 6 `opened.png`; `grep -c "inspected without baseline" 21-UITEST-COVERAGE.md` = 6
- `21-DRIFT.md` (102 items), `21-UNCONFIRMED.md` ("125%" present), `New-HandoffPage.ps1` (`AERO_HANDOFF OK 301 images`) exist and are committed
- `git status --porcelain -- library/src showcase/src` is empty; `.captures/old-kt2.4.10-cmp1.11.1` has 1152 files
