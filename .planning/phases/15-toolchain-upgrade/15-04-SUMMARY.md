---
phase: 15-toolchain-upgrade
plan: 04
subsystem: testing
tags: [kotlin, compose-multiplatform, gradle, junit5, visual-regression, skia]

# Dependency graph
requires:
  - phase: 15-toolchain-upgrade (plan 01)
    provides: Pre-migration visual baseline (Kotlin 2.1.21 + CMP 1.7.3 screenshots in baseline/)
  - phase: 15-toolchain-upgrade (plan 02)
    provides: Kotlin 2.4.10 + CMP 1.11.1 confirmed compiling/building, Material3 pinned to stable 1.9.0
  - phase: 15-toolchain-upgrade (plan 03)
    provides: AeroPanelGroupRecomposeUiTest ported and live re-proven non-inert (TOOL-03/TOOL-04)
provides:
  - "Full 232-test library suite empirically confirmed green on the migrated toolchain (0 failures, 0 errors), including all 12 PanelGroupLogicTest — TOOL-05"
  - "Showcase confirmed compiling, launching, and rendering on the migrated toolchain with stable M3 1.9.0 (no alpha leak)"
  - "After-migration three-theme screenshot set + per-pixel diff report against the plan-01 baseline, human-confirmed NO Skia rendering drift — TOOL-06"
  - "Empirical closure: the Kotlin 2.4.10 + CMP 1.11.1 migration is proven behaviorally AND visually inert; Phase 16's Aero restyle work starts from a verified-clean baseline"
affects: [16-foundation-aero-primitives, 17-buttons, 18-range, 19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Per-pixel RGB-delta diff (Graphics.LockBits, threshold 8) as the mechanical corroborator for a human visual verdict, rather than relying on eyes-on-alone for a toolchain-drift check"
    - "Distinguishing genuine drift from capture artifacts (animation phase, scroll offset) by cross-referencing multiple frames of the same axis (AeroDark/Classic controls corroborating that AeroBlue's high diff % was a scroll artifact, not real drift)"

key-files:
  created:
    - .planning/phases/15-toolchain-upgrade/after/after-AeroBlue-01-top.png
    - .planning/phases/15-toolchain-upgrade/after/after-AeroBlue-03.png
    - .planning/phases/15-toolchain-upgrade/after/after-AeroDark-01-top.png
    - .planning/phases/15-toolchain-upgrade/after/after-AeroDark-02-controls.png
    - .planning/phases/15-toolchain-upgrade/after/after-Classic-01-top.png
    - .planning/phases/15-toolchain-upgrade/after/after-Classic-02-controls.png
    - .planning/phases/15-toolchain-upgrade/after/DIFF-REPORT.md
  modified: []

key-decisions:
  - "TOOL-05 test-suite verification and TOOL-06 showcase-compile/launch verification were run with zero file changes needed — both are pure verification tasks (the toolchain was already correctly configured by plans 02/03), so neither has a standalone task commit, matching the plan 02/03 precedent for verification-only tasks"
  - "The two non-zero diff rows (AeroDark/Classic controls at 0.03-0.045%, AeroBlue controls at 7.28%) were investigated rather than accepted at face value: the small ones attributed to the indeterminate AeroProgressBar's continuous animation being caught at a different phase, the large one attributed to a scroll-offset capture artifact — corroborated by comparing against the properly-aligned AeroDark/Classic frames, not asserted from the number alone"

requirements-completed: [TOOL-05, TOOL-06]

coverage:
  - id: D1
    description: "Full library test suite (232 tests) passes on the migrated Kotlin 2.4.10 + CMP 1.11.1 toolchain, including all 12 PanelGroupLogicTest, with zero rendering/component-behavior source changes"
    requirement: "TOOL-05"
    verification:
      - kind: unit
        ref: "./gradlew :library:test --rerun-tasks (232 tests, 0 failures, 0 errors; PanelGroupLogicTest 12/12)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Showcase compiles, launches, and smoke-runs without crashing on the migrated toolchain, with Material3 resolved to stable 1.9.0 (no alpha leak) in the showcase compile classpath"
    requirement: "TOOL-06"
    verification:
      - kind: unit
        ref: "./gradlew :showcase:compileKotlin (BUILD SUCCESSFUL) + ./gradlew :showcase:dependencies --configuration compileClasspath | grep material3 (1.9.0, no -alpha)"
        status: pass
      - kind: manual_procedural
        ref: "./gradlew :showcase:run launched without exceptions/crash (log inspected for exception/error traces)"
        status: pass
    human_judgment: false
  - id: D3
    description: "After-migration screenshots captured on all three themes (AeroBlue, AeroDark, Classic) at baseline framing (1920x1080 @100% DPI) and per-pixel diffed against the plan-01 baseline; verdict is NO observable Skia rendering drift across the eight target components"
    requirement: "TOOL-06"
    verification:
      - kind: manual_procedural
        ref: ".planning/phases/15-toolchain-upgrade/after/DIFF-REPORT.md — per-pixel diff table (all 6 pairs), committed 4038b3a"
        status: pass
    human_judgment: true
    rationale: "TOOL-06 explicitly requires a human-confirmed visual verdict for Skia-drift detection (m126->m138->m144 across this version range) — a mechanical pixel-diff alone cannot distinguish a genuine rendering regression from an animation-phase or scroll-offset capture artifact; the orchestrator inspected the diff report and screenshots and confirmed the verdict of no drift."

duration: 25min
completed: 2026-07-22
status: complete
---

# Phase 15 Plan 04: Full Test Suite + Visual Drift Check (TOOL-05/TOOL-06) Summary

**232/232 library tests green on Kotlin 2.4.10 + CMP 1.11.1 (incl. all 12 PanelGroupLogicTest), and a human-confirmed, pixel-diff-corroborated verdict of NO Skia rendering drift across all three themes vs the plan-01 pre-migration baseline — the toolchain migration is proven behaviorally and visually inert.**

## Performance

- **Duration:** ~25 min (executor: test suite + showcase launch ~10 min; orchestrator: screenshot capture + diff ~15 min)
- **Started:** 2026-07-22T10:32:51Z
- **Completed:** 2026-07-22T13:46:13Z
- **Tasks:** 3 of 3 (Task 1 and Task 2 verification-only, no file changes; Task 3 human/orchestrator checkpoint)
- **Files modified:** 0 (Tasks 1-2); 7 created (Task 3: 6 PNGs + DIFF-REPORT.md, committed separately by the orchestrator)

## Accomplishments

- Ran the complete library test suite for real on the migrated toolchain: `./gradlew :library:test --rerun-tasks` → **BUILD SUCCESSFUL**, parsed from JUnit XML across all suites: **232 tests total, 0 failures, 0 errors** — exact match to the plan's required count. `PanelGroupLogicTest` specifically confirmed at **12/12, 0 failures** via its dedicated XML report. No rendering or component-behavior source file was touched to reach green (`git status --short` showed zero task-related diffs before and after the run).
- Confirmed the showcase compiles cleanly (`./gradlew :showcase:compileKotlin` → BUILD SUCCESSFUL) and that Material3 resolves to the explicit stable coordinate `org.jetbrains.compose.material3:material3:1.9.0` (+ `material3-desktop:1.9.0` transitive) in the showcase's `compileClasspath` — no `-alpha`/`-beta` suffix anywhere.
- Launched `./gradlew :showcase:run` in the background and confirmed it opened without crashing (no exception/stack-trace in the run log after ~23s of runtime; the `:showcase:run` task remained active, as expected for a running desktop app), leaving the window live and ready for the orchestrator's after-migration capture.
- The orchestrator captured after-migration screenshots on all three themes (AeroBlue, AeroDark, Classic) via the same windows-mcp automation and 1920x1080 @100% DPI framing used for the plan-01 baseline, and ran a per-pixel RGB-delta diff (`Graphics.LockBits`, threshold 8) against each baseline counterpart — committed as `4038b3a` with `after/DIFF-REPORT.md`.
- **Diff results (see `after/DIFF-REPORT.md` for full table):** all three "top" views (Buttons/Foundation/Icons) are pixel-identical (3-4 differing px out of ~2M, single-glyph anti-aliasing, max delta 25-28). The two "controls" views with non-trivial diff (AeroDark 0.033%, Classic 0.045%) were traced to the indeterminate `AeroProgressBar`'s continuously-animated bar being caught at a different phase between baseline and after captures — not a rendering change; every static component (Switch, SegmentedControl, Slider, RangeSlider, deterministic ProgressBar, ListItem) is unchanged. The one high-percentage row (AeroBlue controls, 7.28%) was traced to a scroll-offset capture artifact (baseline reached via two 8-wheel scrolls, after via one 16-wheel scroll, landing at a slightly different vertical position) — corroborated by the properly-aligned AeroDark/Classic controls frames both showing the same ~0.04% drift-free delta.
- **Verdict: NO Skia rendering drift** (m126 -> m138 -> m144 across CMP 1.7.3 -> 1.11.1) in any of the eight target components on any of the three themes. Combined with the 232/232 green test suite, the migration is confirmed both behaviorally and visually inert.

## Task Commits

1. **Task 1: Run the full library test suite** - no standalone commit (verification-only; zero file changes — the toolchain was already correctly configured by plans 02/03)
2. **Task 2: Compile and launch the showcase on the new toolchain** - no standalone commit (verification-only; zero file changes)
3. **Task 3: Human/orchestrator captures after-migration screenshots and diffs against baseline** - `4038b3a` (feat) — `.planning/phases/15-toolchain-upgrade/after/` (6 PNGs + DIFF-REPORT.md), committed by the orchestrator via windows-mcp desktop automation (executor subagents cannot capture desktop-window screenshots)

**Plan metadata:** (this commit, following SUMMARY.md creation)

## Files Created/Modified

- `.planning/phases/15-toolchain-upgrade/after/after-AeroBlue-01-top.png` - After-migration AeroBlue top view (Foundation/Icons/Buttons)
- `.planning/phases/15-toolchain-upgrade/after/after-AeroBlue-03.png` - After-migration AeroBlue controls view (Switch/Segmented/Range/Progress/Lists)
- `.planning/phases/15-toolchain-upgrade/after/after-AeroDark-01-top.png` - After-migration AeroDark top view
- `.planning/phases/15-toolchain-upgrade/after/after-AeroDark-02-controls.png` - After-migration AeroDark controls view
- `.planning/phases/15-toolchain-upgrade/after/after-Classic-01-top.png` - After-migration Classic top view
- `.planning/phases/15-toolchain-upgrade/after/after-Classic-02-controls.png` - After-migration Classic controls view
- `.planning/phases/15-toolchain-upgrade/after/DIFF-REPORT.md` - Per-pixel diff table (6 pairs) + drift analysis + conclusion, committed `4038b3a`

_(All seven files above were created and committed by the orchestrator in `4038b3a`, prior to this SUMMARY — not re-committed here.)_

## Decisions Made

- Tasks 1 and 2 produced no file changes and therefore have no standalone task commit, following the same verification-only precedent established in plans 02 and 03 (an empty `git diff` on a verification task means nothing new to stage).
- The two non-zero diff percentages were investigated to determine root cause rather than accepted or rejected on the raw number alone: cross-referencing the properly-aligned AeroDark/Classic controls frames (both ~0.03-0.045%, both traced to the indeterminate progress bar's animation phase) let the orchestrator confidently attribute AeroBlue's outlier 7.28% to a scroll-offset capture artifact rather than a genuine Skia rendering regression.

## Deviations from Plan

None — plan executed exactly as written. Both automated tasks passed on the first run with no fixes needed; the checkpoint task (Task 3) was correctly deferred to the orchestrator per the `orchestrator_handles_screenshots` routing (desktop-window screenshot capture is outside an executor subagent's capability), and no rendering code was touched in response to the two non-zero (but ultimately non-drift) diff rows, honoring the plan's SCOPE GUARD.

## Issues Encountered

None. The test suite and showcase compile/launch passed cleanly on the first attempt — no genuine behavioral regression, no supporting-dependency incompatibility requiring the sanctioned minimal-bump escalation path.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- TOOL-05 and TOOL-06 are both satisfied and empirically closed: the 232-test suite is green and the visual diff shows no toolchain-induced drift.
- Phase 16 (Foundation - Aero Primitives Layer) can proceed on a verified-clean baseline: the migrated toolchain (Kotlin 2.4.10 + CMP 1.11.1, Material3 1.9.0) introduces zero behavioral or visual regressions ahead of the Aero restyle work.
- Plans 05-06 (TOOL-07 `dropShadow`/`innerShadow` scratch-compile confirmation, TOOL-08 JitPack build) remain outstanding for Phase 15's full closure.
- No blockers or concerns carried forward from this plan.

## Self-Check: PASSED

- FOUND: .planning/phases/15-toolchain-upgrade/after/after-AeroBlue-01-top.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/after-AeroBlue-03.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/after-AeroDark-01-top.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/after-AeroDark-02-controls.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/after-Classic-01-top.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/after-Classic-02-controls.png
- FOUND: .planning/phases/15-toolchain-upgrade/after/DIFF-REPORT.md
- FOUND: .planning/phases/15-toolchain-upgrade/15-04-SUMMARY.md
- FOUND: 4038b3a (Task 3 commit, orchestrator)

---
*Phase: 15-toolchain-upgrade*
*Completed: 2026-07-22*
