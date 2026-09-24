---
phase: 21-migration-release-3-1-0
plan: 04
subsystem: testing
tags: [powershell, image-diff, lockbits, baseline, noise, printwindow, compose-ui-test]

# Dependency graph
requires:
  - phase: 21-01
    provides: "tools/capture/Invoke-ShowcaseSweep.ps1 (BASE-01/BASE-02 sweep driver) and AeroCapture.ps1's AeroPixels/AeroCaptureNative"
  - phase: 21-02
    provides: "UiCapture.kt opt-in PNG writer (aero.captureDir, D-03), Base05GlassStateCaptureTest.kt / Base05DragCaptureTest.kt (BASE-05)"
  - phase: 21-03
    provides: "D07MenuPopupCaptureTest.kt / D07PickerPopupCaptureTest.kt (D-07 opened-popup captures), captureOpened()/assertOpenedDiffers()"
provides:
  - "tools/capture/Compare-AeroCaptures.ps1 — SelfTest/Noise/Compare/ContactSheet modes, self-tested comparison/noise-extraction/diff-highlight/contact-sheet tool for VER-07/VER-08"
  - "21-BASELINE.md — pre-upgrade toolchain, DPI, git sha, capture method, 51-row showcase frame index, UI-test reference index (BASE-04, BASE-05)"
  - "21-NOISE.md — every showcase and UI-test noise region named by source (BASE-03)"
  - "21-noise-regions.json — machine-readable showcase (75 keys) + uitest (200 keys) noise regions, consumed by later plans' -NoiseJson"
  - ".captures/old-kt2.4.10-cmp1.11.1/showcase/runA|runB — 75 frames x 2 independent runs, 96 DPI, Kotlin 2.4.10 / CMP 1.11.1"
  - ".captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA|runB — 200 PNGs x 2 runs (23 components + Base05Proof)"
affects: [21-05, 21-10, 21-11, verification-plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "8px-cell DiffCells union -> 8-connected BFS merge -> padded/clamped rects -> insideNoise/outsideNoise classification, shared between Noise (union across all non-reference captures) and Compare (before vs. after) modes"
    - "C#/LockBits-only per-pixel work (AeroCompareNative.MakeDiffImage: 50% dim + magenta diff paint), PowerShell only touches cell-level (8px grid) or file-level data, never a per-pixel PowerShell loop"
    - "Comma-joined -RunDirs/-Dirs CLI value normalization, mirroring Invoke-ShowcaseSweep.ps1's -Themes/-Sections fix for this host's powershell.exe -File array binding"

key-files:
  created:
    - tools/capture/Compare-AeroCaptures.ps1
    - .planning/phases/21-migration-release-3-1-0/21-BASELINE.md
    - .planning/phases/21-migration-release-3-1-0/21-NOISE.md
    - .planning/phases/21-migration-release-3-1-0/21-noise-regions.json
  modified: []

key-decisions:
  - "Noise mode's reference is the group's earliest (RunIndex, Capture) entry after sorting all captures across all RunDirs together — for showcase this is runA's c1 (6 captures/key = 3 per launch x 2 launches); for uitest (1 capture/key/run, no c-suffix) this is runA's single file compared only against runB's single file, exactly matching the plan's 'reference = first run's c1 (or the file)' wording"
  - "Region px value in Noise/Compare output is the SUM of per-cell diffPx across every comparison that touched a cell in that region (not a single comparison's count) — an indicative magnitude, not a strict per-pixel count, since a region can be built from up to 5 separate comparisons in Noise mode"
  - "ContactSheet mode groups showcase captures by Theme/Section and uitest captures by Component/Theme (first two segments of the frame key) — neither mode is exercised by this plan (no Compare/ContactSheet caller yet), only proven structurally present and self-tested"

patterns-established:
  - "Every future VER-07/VER-08 before/after comparison and the D-06 hand-off page consume Compare-AeroCaptures.ps1's Compare/ContactSheet modes against this plan's 21-noise-regions.json unmodified"

requirements-completed: [BASE-03, BASE-04, BASE-05]

# Metrics
duration: ~26min (task commits 17:04-17:27; full plan incl. context load ~40min)
completed: 2026-09-23
---

# Phase 21 Plan 04: Pre-upgrade baseline, noise list, and comparison tool Summary

**Two independent full showcase sweeps (75 frames each, 96 DPI, zero interference violations) and two opt-in UI-test capture runs (200 PNGs each, 541/93 tests green) on the pre-upgrade toolchain (Kotlin 2.4.10 / CMP 1.11.1) produced a named, machine-readable noise list (9/75 showcase keys unstable — all `AeroProgressBar` shimmer or a live recompose-drive counter, none over 0.5% of frame area; 0/200 UI-test keys unstable) and a self-tested `Compare-AeroCaptures.ps1` tool ready for the post-upgrade VER-07/VER-08 comparison.**

## Performance

- **Duration:** ~26 min for the 3 task commits (17:04 -> 17:27 UTC+3); longer wall-clock including context load and the two ~8.5min showcase sweeps
- **Started:** 2026-09-23 (previous plan's last commit `45975be`)
- **Completed:** 2026-09-23T17:27:10+03:00 (last task commit)
- **Tasks:** 3/3 completed
- **Files created/modified:** 4 (all new: 1 tool, 3 reports/data files)

## Accomplishments
- `tools/capture/Compare-AeroCaptures.ps1` (PowerShell 5.1, dot-sources `AeroCapture.ps1`) implements `SelfTest`/`Noise`/`Compare`/`ContactSheet` modes; its fixture self-test (`AERO_COMPARE_SELFTEST PASS`) proves the shared diff-region engine detects zero difference on identical bitmaps, localises a synthetic 10x10 changed block to exactly one rect containing it, and classifies that rect both `insideNoise` (a covering noise region) and `outsideNoise` (an unrelated one) correctly.
- Two independent full showcase sweeps (`runA`, `runB`) captured all 17 sections x 3 themes x every page (75 frame keys each, 51 `(Theme,Section)` combinations with multi-page sections captured on every page) on Kotlin 2.4.10 / Compose Multiplatform 1.11.1, 96 DPI (100% scaling), with identical toolchain blocks, identical frame-key sets, zero orphaned JVMs, and zero `NON-INTERFERENCE VIOLATION`/`LAUNCH ACTIVATED WINDOW` events in either run.
- Run-to-run noise (`Compare-AeroCaptures.ps1 -Mode Noise`, 6 captures per key = 3 per launch x 2 launches, 8px cells, 2px padding) found 66/75 showcase keys pixel-stable and named the other 9: `AeroProgressBar (ind)` indeterminate shimmer (Range section, all 3 themes) and a live 30fps recompose-drive counter text on the Layout section's RCMP-04 demo (2 pages x 3 themes) — both confirmed by reading the source (`AeroProgressBar.kt:181`, `LayoutSection.kt:403-420`) and visually confirmed against the actual `runA` frames. No region covers more than 0.5% of its 960,000px frame.
- Two opt-in UI-test capture runs (`./gradlew :library:test --rerun --tests "com.mordred.aero.capture.*" -Paero.captureDir=...`) were both `BUILD SUCCESSFUL` (541 tests / 93 classes), producing 200 PNGs each across 23 library components (10 BASE-05 state-capture + 13 D-07 popup-opened) plus the `Base05Proof` mechanism-proof folder; the same Noise mode (merged into the same JSON with `-Merge`) found 0/200 UI-test keys unstable, confirming UI tests are already clock-controlled with zero run-to-run noise, unlike the showcase's live wall-clock animations.
- D-03 re-proven after this plan's own UI-test runs: `.captures` file count was 1152 both before and after a plain `./gradlew :library:test --rerun` (no `-Paero.captureDir`), and `git status --porcelain` shows no changes attributable to that run.
- `21-BASELINE.md` and `21-NOISE.md` are committed text reports (images stay in `.captures/`, D-01/D-02); `21-noise-regions.json` has both a `showcase` (75 keys) and `uitest` (200 keys) object, ready for the post-upgrade `-NoiseJson` comparison.

## Task Commits

Each task was committed atomically:

1. **Task 1: Comparison / noise tool with fixture self-test** - `096155e` (feat)
2. **Task 2: Showcase baseline — two full runs on the old toolchain, noise list, baseline record (BASE-03, BASE-04)** - `06c8eeb` (docs)
3. **Task 3: UI-test reference images — two opt-in runs, noise, D-03 recheck (BASE-05, D-07)** - `56252ae` (docs)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `tools/capture/Compare-AeroCaptures.ps1` (new) - `AeroCompareNative.MakeDiffImage` (C#/LockBits: 50% dim + magenta diff paint + outside-noise outline), `Merge-AeroDiffCellsToRegions` (8-connected cell BFS -> padded/clamped rects), `Test-AeroRegionFullyInside`, `Get-AeroFrameEntries` (showcase/uitest key parsing), four `-Mode` branches
- `.planning/phases/21-migration-release-3-1-0/21-BASELINE.md` (new) - toolchain/DPI/git-sha/capture-method table, two-runs summary table, 51-row showcase frame index, "UI-test reference images" section (test classes, component index, popup capture method, D-08 status, not-composable classification)
- `.planning/phases/21-migration-release-3-1-0/21-NOISE.md` (new) - showcase noise method + 9-region named table + UI-test noise summary (0/200 unstable)
- `.planning/phases/21-migration-release-3-1-0/21-noise-regions.json` (new) - `{ "showcase": {75 keys}, "uitest": {200 keys} }`, each `{ captures, stable, dimensionMismatch, regions: [{x,y,w,h,px}] }`

Images (not committed, D-01): `.captures/old-kt2.4.10-cmp1.11.1/showcase/{runA,runB}/` (75 frames x 3 captures x 2 runs = 450 PNGs + 2 manifests), `.captures/old-kt2.4.10-cmp1.11.1/ui-tests/{runA,runB}/` (200 PNGs x 2 runs = 400 PNGs).

## Decisions Made
- Noise mode's reference capture is the earliest `(RunIndex, Capture)` pair after sorting all captures of a key across every `-RunDirs` entry together — for showcase (3 captures/launch x 2 launches) this resolves to `runA`'s `c1`; for UI-test captures (exactly 1 file per key per run, no `c`-suffix) it resolves to `runA`'s single file, matching the plan's "reference = first run's c1 (or the file)" instruction for both kinds with one code path.
- A merged region's `px` field sums the per-cell `diffPx` across every comparison that touched a cell inside it (up to 5 comparisons in Noise mode: `runA` c2/c3 + `runB` c1/c2/c3), not a single comparison's count — documented as an indicative magnitude rather than a strict single-pair pixel count, since the SelfTest fixture (which only exercises one before/after pair) does not need to distinguish this.
- `Compare` and `ContactSheet` modes are implemented and structurally verified (present mode names, required parameters, no PowerShell function literally named `Diff`) but not exercised end-to-end by this plan — no `-BeforeDir`/`-AfterDir` pair exists yet (that is a post-upgrade plan's job); their correctness rests on the shared `Merge-AeroDiffCellsToRegions`/`Test-AeroRegionFullyInside` engine, which IS exercised by the mandated `SelfTest` fixture.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `Merge-AeroDiffCellsToRegions -Cells` rejected an empty `List[int[]]`**
- **Found during:** Task 1 (`-Mode SelfTest` verification run)
- **Issue:** PowerShell's mandatory-parameter binding rejected an empty `System.Collections.Generic.List[int[]]` argument with `ParameterArgumentValidationErrorEmptyCollectionNotAllowed`, which the identical-bitmaps fixture (0 diff cells) legitimately produces; the error was non-terminating (default `$ErrorActionPreference = 'Continue'`), so the script printed `AERO_COMPARE_SELFTEST PASS` despite two thrown errors above it — a false pass that would have hidden a real defect.
- **Fix:** Added `[AllowEmptyCollection()]` to the `Cells` parameter, and set `$ErrorActionPreference = 'Stop'` at script scope so any future non-terminating error stops the script instead of allowing a misleading pass/fail message to print regardless.
- **Files modified:** `tools/capture/Compare-AeroCaptures.ps1`
- **Verification:** Re-ran `-Mode SelfTest` — clean output, single line `AERO_COMPARE_SELFTEST PASS`, no errors.
- **Committed in:** `096155e` (Task 1 commit)

**2. [Rule 1 - Bug] `-RunDirs`/`-Dirs` comma-separated CLI values bound as one string element**
- **Found during:** Task 2 (`-Mode Noise -RunDirs runA,runB` run)
- **Issue:** Same root cause already fixed once in `Invoke-ShowcaseSweep.ps1` (Plan 01, deviation 5): `powershell.exe -File ... -RunDirs pathA,pathB`, invoked from outside a PowerShell session (this repo's Bash tool spawns `powershell.exe` directly), bound the whole comma-joined string as a single one-element array entry instead of splitting it, so `Get-AeroFrameEntries` was called once with a single non-existent combined path and threw `Noise mode requires -RunDirs with at least 2 run directories`.
- **Fix:** Added the same comma-resplit normalization Plan 01 used for `-Themes`/`-Sections`, applied here to `-RunDirs` and `-Dirs` (both accept multiple path/mode arguments over the same binding path).
- **Files modified:** `tools/capture/Compare-AeroCaptures.ps1`
- **Verification:** Re-ran `-Mode Noise -RunDirs runA,runB -Kind showcase ...` — `AERO_COMPARE_NOISE_DONE kind=showcase keys=75`; re-ran `-Mode SelfTest` immediately after to confirm the normalization did not affect SelfTest's own (RunDirs-less) path — still `AERO_COMPARE_SELFTEST PASS`.
- **Committed in:** `06c8eeb` (Task 2 commit, alongside the baseline/noise reports it was needed to produce)

---

**Total deviations:** 2 auto-fixed (1 Rule 3 blocking, 1 Rule 1 bug — both discovered and fixed before any report was written from their output, so no downstream data was affected).

## Issues Encountered
None beyond the two auto-fixed items above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- BASE-03 and BASE-04 are fully satisfied: two independent full showcase sweeps on the old toolchain, every frame's run-to-run noise measured and named, committed as text (`21-BASELINE.md`, `21-NOISE.md`, `21-noise-regions.json`); images retained under `.captures/old-kt2.4.10-cmp1.11.1/showcase/{runA,runB}/`.
- BASE-05's reference-image half is complete: two opt-in UI-test runs on the old toolchain, 200 PNGs each, zero run-to-run noise, `D-08` confirmed not needed by any of the 13 `Popup`-based components (matching Plan 03's finding).
- `Compare-AeroCaptures.ps1`'s `Compare`/`ContactSheet` modes are ready for the post-upgrade plan(s) that will pass `-BeforeDir .captures/old-.../showcase/runA -AfterDir .captures/<new-toolchain>/showcase/<run> -NoiseJson 21-noise-regions.json` (VER-07/VER-08) and build the D-06 hand-off page from the same `-DiffDir` output.
- No blockers identified for downstream plans (05-14). The precondition check in Task 2 (toolchain/wrapper versions, clean git status) passed before capture began, confirming this baseline genuinely predates any Phase 21 dependency bump.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 4 created files found on disk; all 3 task commits (`096155e`, `06c8eeb`, `56252ae`) found in git history.
