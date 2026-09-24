---
phase: 21-migration-release-3-1-0
plan: 10
subsystem: testing
tags: [visual-regression, compose-multiplatform, powershell, capture-tooling]

# Dependency graph
requires:
  - phase: 21-migration-release-3-1-0 (Plan 09)
    provides: Final toolchain (Kotlin 2.4.20 / Compose Multiplatform 1.12.0 / kotlinx-coroutines 1.11.0 / kotlinx-datetime 0.8.0 / JUnit 6.1.3 / Gradle 9.7.1 / JDK 21) in place, 541/0 test gate held
provides:
  - Post-upgrade showcase comparison (75/75 keys) against the pre-upgrade baseline, every outside-noise key localised, D-05 run-to-run tested and categorised
  - Post-upgrade UI-test comparison (200/200 keys) and 70 contact sheets, all viewed and reviewed
  - Fix for two PowerShell 5.1 pipeline-unrolling bugs in tools/capture/Compare-AeroCaptures.ps1 -Mode Compare/-Mode ContactSheet, never exercised before this plan
  - .planning/phases/21-migration-release-3-1-0/21-COMPARE.md — full method, totals, per-key tables, categorised observations for Plan 11
affects: [21-11]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PowerShell 5.1 'return ,$list' + downstream '| Where-Object { $_.Prop -eq X }' silently collapses the whole list into one bogus item via collection member-enumeration — use foreach over the raw call instead of piping through Where-Object"
    - "PowerShell 5.1 '$x = if (...) { $arrayVar } else { @() }' collapses a genuine one-element array to a bare scalar on assignment — wrap the whole if/else in @(...) to keep it an array"

key-files:
  created:
    - .planning/phases/21-migration-release-3-1-0/21-COMPARE.md
    - .captures/new-kt2.4.20-cmp1.12.0/showcase/runB-1 (15 keys, D-05 batch)
    - .captures/new-kt2.4.20-cmp1.12.0/showcase/runB-2 (18 keys, D-05 batch)
    - .captures/new-kt2.4.20-cmp1.12.0/showcase/runB-3 (18 keys, D-05 batch)
    - .captures/new-kt2.4.20-cmp1.12.0/showcase/runB-4 (24 keys, D-05 batch)
    - .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA (200 PNGs)
    - .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runB (200 PNGs, D-05)
    - .captures/diff/compare-showcase.json
    - .captures/diff/compare-uitest.json
    - .captures/diff/noise-new.json
    - .captures/diff/noise-new-uitest.json
    - .captures/diff/showcase/ (75 diff PNGs)
    - .captures/diff/ui-tests/ (27 diff PNGs)
    - .captures/sheets/ui-tests/ (70 contact sheets)
  modified:
    - tools/capture/Compare-AeroCaptures.ps1

key-decisions:
  - "Compare-AeroCaptures.ps1's -Mode Compare/-Mode ContactSheet had never been run before this plan (Plans 01-04 only used -Mode Noise/-Mode SelfTest); two real pipeline-unrolling bugs were found and fixed inline (Rule 1/3 — blocking, tooling not library/showcase code) before any comparison could produce correct data"
  - "All 75 showcase keys carried an outside-noise diff, so the full D-05 re-sweep (75 keys) was run rather than a filtered subset, split into 4 foreground batches (runB-1..4) to respect the 10-minute command limit"
  - "The one large anomaly (Classic/Layout/p3, 41.5% of frame) was investigated to a specific root cause (a scroll-settle timing flake, not a toolchain regression) via manifest scrollPx/contentPx comparison and an independent re-capture (runB-4) that reproduced the correct content — not just labelled 'varies-run-to-run' and left unexplained"

requirements-completed: [VER-07, VER-08]

# Metrics
duration: ~110min
completed: 2026-09-24
---

# Phase 21 Plan 10: Post-Upgrade Visual & UI-Test Comparison Summary

**Full post-upgrade comparison of all 75 showcase frames and 200 UI-test states against the pre-upgrade baseline: 173/200 UI-test states and the bulk of showcase frames are pixel-identical or differ only by a sub-pixel anti-aliasing shift traced to the Compose Multiplatform 1.11.1→1.12.0 rendering pipeline; the one large anomaly (Classic/Layout/p3) was run down to a one-off scroll-settle capture flake, disproved as a stable regression by an independent re-capture.**

## Performance

- **Duration:** ~110 min
- **Started:** 2026-09-24T11:40:00+03:00 (approx, first file read after resume)
- **Completed:** 2026-09-24T12:50:28+03:00
- **Tasks:** 2
- **Files modified:** 1 code file (`tools/capture/Compare-AeroCaptures.ps1`), 1 doc file (`21-COMPARE.md`), plus ~1,376 new capture/diff/sheet images under `.captures/` (gitignored)

## Accomplishments
- Re-swept and compared all 75 showcase frames on the final toolchain against the pre-upgrade baseline; classified every outside-noise diff into 8 patterns (A–H) with a D-05 run-to-run verdict for each of the 75 keys
- Re-ran and compared all 200 UI-test states on the final toolchain; 173/200 identical, 27/200 with a sub-pixel `stable` diff, all traced to the same rendering-pipeline shift
- Generated and personally viewed all 70 UI-test contact sheets (10 BASE-05 + 13 D-07 components x 3 themes + the mechanism-proof folder) — every hover/press/focus/drag/opened-popup state confirmed correct in both before and after columns
- Found and fixed two real, previously-unexercised PowerShell 5.1 pipeline-unrolling bugs in `Compare-AeroCaptures.ps1` that silently corrupted every `-Mode Compare`/`-Mode ContactSheet` run
- Root-caused the single large anomaly (`Classic/Layout/p3`, 41.5% of frame) to a one-off scroll-settle capture flake via manifest `scrollPx` inspection and an independent re-capture, rather than leaving it as an unexplained "varies-run-to-run" entry

## Task Commits

Each task was committed atomically:

1. **Task 1: Showcase after-sweep, comparison, D-05 check, agent review (VER-07)** - `c3ba358` (docs, includes the tooling fix)
2. **Task 2: UI-test images, comparison, contact sheets, agent review (VER-08)** - `e7ca1b2` (docs)

## Files Created/Modified
- `tools/capture/Compare-AeroCaptures.ps1` - Fixed `-Mode Compare` (lines 438-439) and `-Mode ContactSheet` (line 549) `Get-AeroFrameEntries | Where-Object { $_.Capture -eq 1 }` collection-member-enumeration bug (foreach over the raw call instead); fixed `-Mode Compare`'s `$keyNoise = if (...) {...} else { @() }` single-element-array-collapse bug (wrapped in `@(...)`)
- `.planning/phases/21-migration-release-3-1-0/21-COMPARE.md` - Full VER-07/VER-08 method, totals, per-key/per-component tables, categorised observations, D-05 verdicts, frames/sheets viewed

## Decisions Made
- Fixed the two `Compare-AeroCaptures.ps1` bugs inline (Rule 1/3: blocking issue, tooling code not library/showcase drawing code, so D-04 does not apply) rather than working around them or stopping — the plan could not produce any comparison data otherwise
- Ran the full D-05 re-sweep (all 75 showcase keys, not a filtered subset) since every key carried an outside-noise diff, split into 4 foreground-safe batches
- Investigated the `Classic/Layout/p3` anomaly to a specific, evidenced root cause (scroll-settle timing) rather than accepting "varies-run-to-run" as a sufficient classification on its own

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1/3 - Blocking bug] Fixed PowerShell 5.1 pipeline-unrolling bug in `-Mode Compare`/`-Mode ContactSheet`**
- **Found during:** Task 1, first `-Mode Compare` invocation
- **Issue:** `Get-AeroFrameEntries -RunDir ... -Kind ... | Where-Object { $_.Capture -eq 1 }` collapsed 225 real per-file entries into a single malformed entry. `Get-AeroFrameEntries` intentionally returns its `List<object>` as one non-unrolled pipeline object (`return , $entries`, correct for the working `-Mode Noise` `foreach` usage) — but piping that single object through `Where-Object { $_.Capture -eq 1 }` triggers PowerShell's collection member-enumeration on `.Capture` (returns an array of all Capture values), and the array `-eq 1` comparison yields a non-empty (truthy) result, so the *entire list* passes through as one bogus item instead of being filtered element-by-element.
- **Fix:** Replaced `@(Get-AeroFrameEntries ... | Where-Object { $_.Capture -eq 1 })` with a `foreach` loop over the raw function call, appending matching items to a fresh `List<object>` — matches the pattern already used correctly by `-Mode Noise`.
- **Files modified:** `tools/capture/Compare-AeroCaptures.ps1` (lines 438-439, and the identical pattern at line 549 in `-Mode ContactSheet`)
- **Verification:** `-Mode SelfTest` re-run and still passes; `-Mode Compare` then correctly reported `keys=75` (showcase) and `keys=200` (uitest) instead of `keys=1`
- **Committed in:** `c3ba358` (Task 1 commit)

**2. [Rule 1/3 - Blocking bug] Fixed PowerShell 5.1 single-element-array collapse in `-Mode Compare`**
- **Found during:** Task 1, after fixing bug 1, second `ConvertTo-Json` failure
- **Issue:** `$keyNoise = if ($noiseData.ContainsKey($key)) { $noiseData[$key] } else { @() }` collapses a genuine one-element array (e.g. a key with exactly one baseline noise region, like `AeroBlue/Layout/p3`) to a bare scalar `PSCustomObject` on assignment, because PowerShell's if/else-as-expression output stream re-collapses single-item results. `$keyNoise.Count` then threw `PropertyNotFoundStrict` under `Set-StrictMode -Version 2`.
- **Fix:** Wrapped the whole if/else in `@(...)` to force the result to stay an array regardless of element count.
- **Files modified:** `tools/capture/Compare-AeroCaptures.ps1` (line 488)
- **Verification:** `-Mode Compare` completed without error and produced correct per-key `classification` fields (`insideNoise`/`outsideNoise`) for keys with 1, 4, 5, and 9-region noise entries
- **Committed in:** `c3ba358` (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (both Rule 1/3 — blocking bugs in never-before-exercised capture tooling)
**Impact on plan:** Both fixes were prerequisites for the plan's own verification steps to produce any usable data at all; neither touches library or showcase drawing code (D-04 respected — `git status --porcelain -- library/src/main showcase/src/main` printed nothing throughout both tasks).

## Issues Encountered

- **`Classic/Layout/p3` showed a 41.5%-of-frame diff** on the first showcase comparison — investigated rather than dismissed: the "after" frame showed content from roughly page-2's scroll position despite the manifest reporting the identical `scrollPx=2304`/`contentPx=3316` target as the correct "before" frame, indicating the scroll had not visually settled by capture time. The D-05 re-sweep's independent re-capture of the same key (`runB-4`) landed correctly, matching the baseline — confirmed as a one-off capture-timing flake, not a reproducible toolchain regression. Named to Plan 11 as a capture-pipeline robustness note (`SettleMs=1500` occasionally insufficient for a 5-page, 3316px section).
- **`Classic/Data/p1` showed a hover-highlighted row** not present in the baseline, despite not being flagged by the `externalActivity` foreground-takeover guard — traced to real mouse-cursor movement over the (non-focused, non-activated) capture window's screen region, which is enough for Compose to register genuine pointer-hover without the window ever taking the OS foreground. Named to Plan 11 as a capture-guard gap (hover-only interference is not currently detected).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Both VER-07 and VER-08's capture-and-compare half is complete. `21-COMPARE.md` gives Plan 11 a single, categorised drift list to work from: 3 named categories are the already-known baseline noise sources re-observed; 3 categories are a small, consistent, sub-pixel anti-aliasing delta (text/icon/outline glyphs, popup shadow edges) traced to the Compose Multiplatform 1.11.1→1.12.0 bump; 2 are one-off capture artifacts (a scroll-settle flake and a real-cursor hover interference) independently disproved as stable regressions. No blockers for Plan 11's single D-04 stop.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-24*

## Self-Check: PASSED

Verified present on disk: `.planning/phases/21-migration-release-3-1-0/21-COMPARE.md`,
`.captures/new-kt2.4.20-cmp1.12.0/showcase/runB-1..4/manifest.json`,
`.captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/manifest.json` (implicit via 200 PNGs),
`.captures/diff/compare-showcase.json`, `.captures/diff/compare-uitest.json`,
`.captures/diff/noise-new.json`, `.captures/diff/noise-new-uitest.json`,
`.captures/sheets/ui-tests/` (70 files). Verified present in `git log`: `c3ba358`, `e7ca1b2`.
`.captures/old-kt2.4.10-cmp1.11.1` file count reconfirmed at 1152 (unchanged).
