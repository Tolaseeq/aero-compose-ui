---
phase: 22-native-window-behavior-release-3-2-0
plan: 23
subsystem: testing
tags: [win32, wndproc, compose-indication, session-tooling, press-parity]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-13's headless parity baseline and the 22-15 session frames the diagnosis measured against; 22-22's -Checks subset used to dry-run the fixed formula"
provides:
  - "Evidence-backed exoneration of the maximize-button bridge for the B01-PRESS-FRAME FAIL (the diff was a check-formula artifact, not a bridged-interaction delta)"
  - "Corrected B01-PRESS-FRAME formula: floating reset after each toggle-causing up, fresh min geometry, both press frames size-guarded vs the rest frame"
  - "Headless reproduction recipe (posted NC messages) for the max-button press visual, with the TME_LEAVE hover limitation recorded"
affects: [22-25, 22-26]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "State-drift guard: any check whose own steps toggle window state must reset + recompute geometry + size-guard frames against a reference frame"

key-files:
  created:
    - .captures/22-gappress/diagnosis.txt
    - .captures/22-gappress/proof-pack.log
    - .captures/22-gappress/press-parity-probe.ps1
    - .captures/22-gappress/client-press-probe.ps1
    - .captures/22-gappress/proof-pack-probe.ps1
  modified:
    - tools/winprobe/Invoke-FullSession.ps1

key-decisions:
  - "No library change: the bridge's press emission is the settled, correct press visual (hover fill + material-ripple at pressedAlpha, FadeIn=75ms/Radius=225ms, captured at +400ms); Route A had nothing to fix and Route B's re-injection would chase a reference artifact while risking the proven flyout/click paths"
  - "The 144/176 FAIL's true mechanism: the max press's up-click toggles placement, so the check's own sequence maximized the window before the min step; the min press then used floating geometry against the maximized window (press-min frames are 1936x1048 on BOTH JVMs, every other B01 frame 1200x800)"
  - "Formulas guard against their own side effects - the 22-22 lesson applied here"

patterns-established:
  - "Frame-size guard: parity strips are only meaningful when both frames come from the same window state; guard by dimensions, fail honestly on drift"

requirements-completed: []  # BTN-01's press clause and VER-14's count are NOT flipped here:
  # the real-press verdict belongs to 22-25's re-run of the FIXED formula on both JVMs;
  # 22-26 flips only the rows 22-25 actually proves. No library test was added (no library
  # change), so the locked count stays 596.

# Metrics
duration: ~2h5m wall (executor quota-failure ~1h; orchestrator inline completion ~65m active)
completed: 2026-09-28
---

# Phase 22 Plan 23: BTN-01 press parity — diagnosis and formula correction

**The B01-PRESS-FRAME FAIL was a check-formula artifact: the check's own max-press click maximized the window before the min step, so the "parity" strip compared a floating-window button against maximized-window content — the bridge's press visual was the settled, correct one all along; the formula now resets, recomputes, and size-guards.**

## Performance

- **Duration:** ~2h5m wall (2026-09-28, 12:50-14:05Z executor attempt terminated by API quota; 14:52-15:57Z orchestrator inline completion)
- **Started:** 2026-09-28T12:50Z
- **Completed:** 2026-09-28T15:57Z
- **Tasks:** 3/3
- **Files modified:** 1 (tools/winprobe/Invoke-FullSession.ps1)

## Accomplishments
- Task 1 (diagnosis): all three candidate deltas measured/refuted with numbers — press origin is the same centre by construction (code read of `buttonCenter()` half-extents); emission timing produces the settled press visual (probe + material-ripple source constants FadeIn=75ms/Radius=225ms vs the +400ms capture); hover provably holds across the swallow (`ncHovered` untouched by `swallowMaxButtonPress`). The actual mechanism found while cross-checking references: **press-min frames are 1936×1048 on both JVMs** — the check maximized its own window before the min step.
- Task 2 (fix, deviated route): tools-only formula fix in `Invoke-FullSession.ps1` — `Reset-SessionWindow` to floating after each toggle-causing up (max up → maximize; min up → minimize), min point recomputed from fresh geometry, and both press frames size-guarded against the rest frame (state drift now FAILs honestly instead of producing a bogus parity number).
- Task 3 (proof pack): V11 sweep on a fresh capture-mode launch **pass=18 fail=0**, zero leftover MainKt JVMs, click-toggle round trip observed (posted down/up → `AERO_CHROME up click=true`, IsZoomed→True, SC_RESTORE→Floating 360,140,1560,940). Suite green at locked 596 (no library change; count intentionally unmoved).

## Task Commits

1. **Task 1: Headless diagnosis** — evidence under git-ignored `.captures/22-gappress/` (diagnosis.txt, two probe transcripts); no repo commit by standing rules (same as 22-21 Task 1)
2. **Task 2: B01-PRESS-FRAME formula fix** - `ecd5e39` (fix)
3. **Task 3: Guards/proof pack** — no library tests added (no library change), locked count stays 596; evidence in proof-pack.log

## Files Created/Modified
- `tools/winprobe/Invoke-FullSession.ps1` — B01-PRESS-FRAME rebuilt (reset + fresh geometry + size guard)
- `.captures/22-gappress/*` — diagnosis, probes, proof pack (git-ignored evidence)

## Decisions Made
- **No library change (Route A/B both declined, evidence-backed).** The bridge emits the settled correct press visual; the recorded FAIL compared against a polluted reference. C5's fallback selection condition ("the bridged emission cannot reach parity") is factually unmet — parity was already there.
- **Formula follows 22-22's artifact class:** a check whose own steps change window state must restore that state and derive points from live geometry; frame dimensions are part of the check's contract.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Wrong route] Diagnosis refuted the plan's premise; fix retargeted to the check formula**
- **Found during:** Task 1 (reference cross-comparison)
- **Issue:** The plan assumed the 144/176 FAIL measured a bridge-vs-real press difference and mandated Route A (bridge correction) or Route B (C5 re-injection). The evidence shows the diff was between a floating-window button strip and a maximized-window content strip — no bridged-interaction delta exists to fix.
- **Fix:** Implemented the evidence-selected route: B01-PRESS-FRAME formula correction in the session tooling; no change to `AeroMaxButtonInteraction.kt` / `AeroWndProc.kt`.
- **Files modified:** tools/winprobe/Invoke-FullSession.ps1
- **Verification:** parser PARSE OK; `-DryRun -Checks B01-PRESS-FRAME -Pass Both` exit 0 on both JVMs with frames produced; the real verdict is 22-25's re-run.
- **Committed in:** ecd5e39

**2. [Rule 3 - Environment] Executor terminated by API quota (429, 5-hour limit) mid-plan**
- **Found during:** Task 1 (executor's own probe crashed after SC_MAXIMIZE before restore)
- **Issue:** The worktree executor died from a provider quota limit, leaving a maximized probe window and two JVMs on the maintainer's desktop, an untracked scratch file, and no commits.
- **Fix:** Orchestrator tore down the leftover processes (PID-verified from the dead worktree's paths), salvaged the scratch diagnosis to `.captures/22-23-salvage/`, removed the dead worktree/branch, and completed the plan inline on the main tree per the maintainer's explicit "продолжаем".
- **Files modified:** none beyond the plan's own scope
- **Verification:** zero leftover MainKt JVMs recorded in every probe's teardown lines.
- **Committed in:** n/a (environment recovery)

---

**Total deviations:** 2 auto-fixed (1 wrong-route, 1 environment)
**Impact on plan:** The route deviation is the honest outcome of the diagnosis the plan itself mandated; the environment deviation was forced by provider quota. No scope creep — the library was intentionally left untouched.

## Issues Encountered
- The probe's chrome-trace line poll recorded `TRACE down=<>` (empty) although the message demonstrably arrived (chrome log contains the line; the placement toggle fired): stdout redirect buffering delayed the line past the 10s poll. Delivery is proven by `AERO_CHROME event=max-button down`, the nccalc-max trace, and the observed IsZoomed toggle; noted as a probe-tooling caveat, not re-run.
- Posted `WM_MOUSEMOVE`/`WM_LBUTTONDOWN` to the child canvas paint nothing (probe 2): AWT's hover/press pipeline does not synthesize Compose input from posted client messages — recorded as the reason the press-min artifact is not headless-reproducible, and consistent with 22-NOTES' prior finding that a posted NC move cannot hold a hover while the real cursor is elsewhere.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- 22-25 must re-run **B01-PRESS-FRAME** (now fixed) plus B01-HOVER/B01-CLICK guards on both JVMs via `-Checks`; the real press verdicts land there.
- 22-26 flips BTN-01's press clause only from 22-25's rows; VER-14 count remains 596 unless 22-24 adds tests.

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-28*
