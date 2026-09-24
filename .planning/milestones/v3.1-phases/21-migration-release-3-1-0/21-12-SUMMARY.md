---
phase: 21-migration-release-3-1-0
plan: 12
subsystem: verification
tags: [d-04, noise-regions, handoff, ver-10]

# Dependency graph
requires:
  - phase: 21-11
    provides: "21-DRIFT.md (102 items / 8 groups), the maintainer's ruling «всё принимаем», the hand-off page generator"
provides:
  - "21-DRIFT.md with the verbatim ruling on every item and a Rulings section"
  - "21-noise-regions.json widened for the 9 run-to-run-varying keys of groups 2, 5, 6 (stable:false, D-05 proof cited)"
  - "Re-comparison with the widened noise list: showcase withOutside 75 -> 66 (the 9 group 2/5/6 keys now inside-noise only); UI tests unchanged (27, groups 3/4 kept as accepted rendering)"
  - "VER-10: the maintainer inspected the page and the live showcase after the agent's complete sweep and approved"
affects: [21-13, 21-14]

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/phases/21-migration-release-3-1-0/21-DRIFT.md
    - .planning/phases/21-migration-release-3-1-0/21-COMPARE.md
    - .planning/phases/21-migration-release-3-1-0/21-noise-regions.json

key-decisions:
  - "No drawing, showcase or tooling code changed: every group was ruled accept; groups 7/8 (capture flakes) are recorded as known capture limitations, not noise"
  - "VER-10 reply «+» recorded verbatim and read as approval (the resume signal asked for «approved» or a list of issues; «+» names no issue)"

requirements-completed: [VER-10]

# Metrics
duration: about 20 min (plus the maintainer's review)
completed: 2026-09-24
---

# Phase 21 Plan 12: Rulings applied, VER-10 hand-off Summary

**The maintainer's «всё принимаем» is on every drift item. The 9 animation and unstable keys moved into the named noise regions and now compare clean. The maintainer looked at the page and the live showcase after the full sweep and approved («+»).**

## Performance

- **Duration:** about 20 min of agent work
- **Completed:** 2026-09-24
- **Tasks:** 2/2 (Task 1 by an executor, Task 2 the maintainer's hand-off)
- **Files modified:** 3

## Accomplishments

- **Task 1:** ruling transcribed into all 102 rows, plus a Rulings section. Noise list widened for `AeroDark/List/p1`, `{AeroBlue,AeroDark,Classic}/Range/p0` and `AeroBlue/Layout/p3`, `AeroBlue/Layout/p4`, `AeroDark/Layout/p3`, `AeroDark/Layout/p4`, `Classic/Layout/p4`. The re-compare went to a new path, `.captures/diff-r12/compare-showcase.json`: `keys=75 identical=0 insideOnly=9 withOutside=66 missing=0`. Gate `AERO_TEST_COUNT total=541 skipped=0`. The page reports `AERO_HANDOFF OK 301 images`.
- **Task 2:** the page (`.captures/handoff/index.html`) was opened and the showcase launched normally for the maintainer. The checkpoint listed the 96 DPI (100 %) scale, the rulings, the unconfirmed list and the non-interference fact, including the two Classic dialog focus takes. The agent closed the showcase after the reply, since it would otherwise lock the library jar for the next plan.

## Task Commits

1. **Task 1: record D-04 rulings and re-comparison** - `721692f` (docs)
2. **Task 2: VER-10 hand-off**: reply recorded below

## VER-10 reply (verbatim)

> +

## Deviations from Plan

- Task 1 run by an executor, Task 2 presented by the orchestrator (MCP and user interaction stay in the main session).
- `New-HandoffPage.ps1` reads the fixed `.captures/diff/compare-*.json` paths, so the refreshed page shows the Plan 10 comparison plus the updated DRIFT text; the post-ruling numbers are in `21-DRIFT.md` / `21-COMPARE.md` and `.captures/diff-r12/`.

**Total deviations:** 2, no scope change.

## Issues Encountered

None.

## Next Phase Readiness

- Plan 21-13: throwaway JitPack verify tag, published bytecode check, README consumer floor.

## Self-Check: PASSED

- `21-DRIFT.md` has the Ruling column filled and a Rulings section (`721692f`)
- `git status --porcelain -- library/src showcase/src tools` is empty
- `.captures/old-kt2.4.10-cmp1.11.1` has 1152 files
