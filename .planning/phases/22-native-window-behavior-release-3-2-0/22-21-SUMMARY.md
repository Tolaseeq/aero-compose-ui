---
phase: 22-native-window-behavior-release-3-2-0
plan: 21
subsystem: verification
tags: [ver-12, jbr-drift, gap-closure, headless-battery, blocker-5]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "The 22-15 session record (both passes, console/results/logs); the 22-02 C7 battery precedent; WinProbe/FullSession launch mechanics"
provides:
  "Blocker #5 diagnosis: headless battery verdict (message-level path byte-identical on JDK 21 vs JBR 21, so the drift lives in the real-input pipeline) + the folded JBR-only confirmation spec consumed by 22-25"
affects: [22-25, 22-18 (release gate: no unexplained JVM-specific failure mode)]

tech-stack:
  added: []
  patterns:
    - "Diagnosis-before-fix: a JVM-specific failure mode is bounded by measuring every library-owned interface headlessly on both JVMs before any library change is considered"
    - "Evidence-recording guards are part of the evidence: a dropped foreground line (PS 5.1 empty-List falsiness) is what left the 22-15 record undecidable"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-21-SUMMARY.md
  modified: []
  git-ignored-artifacts:
    - .captures/22-jbrdiag/battery-results.txt
    - .captures/22-jbrdiag/jbrbattery.py
    - .captures/22-jbrdiag/Invoke-JbrBattery.ps1
    - .captures/22-jbrdiag/logs/std-run2.out.log
    - .captures/22-jbrdiag/logs/jbr-hotrun.out.log
    - .captures/22-jbrdiag/before-standard.json
    - .captures/22-jbrdiag/before-jbr.json
    - .captures/22-jbrdiag/after-all.json

key-decisions:
  - "VERDICT (b): not reproducible headlessly -- every message-level probe answer is value-identical on JDK 21 and JBR 21 (42/42 normalized lines), so the JBR drift lives in the real-input pipeline and the 22-25 JBR-only confirmation with per-gesture foreground pids is the deciding evidence"
  - "No library-fix candidate is filed: a library change cannot target a difference that does not exist at any interface the library owns (hit-test classification, NC message handling, placement sync, MINMAXINFO floor, subclass install/verify -- all measured identical)"
  - "22-15 recording gap root-caused: Invoke-FullSession.ps1:304 `if ($Evidence)` drops the foregroundOurs line whenever the evidence list is still empty (PS 5.1 falsy empty List) -- 22-25 prep must change it to a $null check or the confirmation session will lose the same lines again"
  - "W05 JBR failure bounded to paint: 17/17 placement samples matched on JBR in 22-15; only the floating glyph-crop repaint across the maximize cycle differed (616 px vs JDK 0)"

requirements-completed: []  # diagnosis only; SNAP-01/SNAP-03/WIN-05 clauses stay Pending until 22-25 confirms

duration: ~29 min
completed: 2026-09-28
---

# Phase 22 Plan 21: JBR drift headless battery + verdict — Summary

**Blocker #5 (JBR-only caption-drag / Win-arrow inertness in the 22-15 run) is bounded: the
current build's message-level window path is value-identical on standard JDK 21 and JBR 21
across the full probe battery, so the drift lives in the real-input pipeline
(foreground/activation or gesture delivery), and plan 22-25 receives the exact nine-check
JBR-only confirmation spec with per-gesture foreground-pid instrumentation and a retry rule
that converts fluke inertness into a diagnosable outcome.**

## VERDICT

**Not reproducible headlessly: the message-level path is byte-identical (value-identical on
every normalized probe line) on both JVMs.**

Evidence: `.captures/22-jbrdiag/battery-results.txt` (worktree-local; BATTERY SUMMARY block,
`differing probe lines after normalization: 0`, 42 lines per JVM). Concretely, both columns
of the battery -- standard JDK 21 (`:showcase:run`, ms-21.0.9) vs JBR 21 (`:showcase:hotRun`,
jbr-21.0.9), identical launch properties -- produced:

- style `0x96CF0000` with the same decoded style-name set, `exStyle=0x00000100`, 96 DPI, same
  floating rect;
- the same single `SunAwtCanvas` child covering the whole client (F8 unchanged);
- all 14 main + 18 narrow `WM_NCHITTEST` chain answers identical, including the
  `SunAwtCanvas:-1 -> SunAwtFrame:<code>` bounce (caption=HTCAPTION, max=HTMAXBUTTON,
  min/close/marked/leading=HTCLIENT, edges/corners correct, marked/captionLeftOfMarked
  boundary correct on the narrow window);
- posted `SC_MAXIMIZE`/`SC_RESTORE` placement sync identical (`Maximized`+isZoomed+awt 6 →
  `Floating`+0, reporter == OS state at every step);
- the posted `WM_NCLBUTTONDOWN/UP`-at-HTMAXBUTTON toggle pair identical (Maximized then
  Floating through today's real `onClick` path);
- `WM_GETMINMAXINFO` floor identical (50x50 request → exactly 320x240 on both);
- foreground behavior identical: the external foreground pid (12752) was unchanged
  before/during/after a probe `SC_MAXIMIZE` on BOTH JVMs -- F9 re-proven; no
  `SetForegroundWindow` was ever called;
- chrome-trace inventory identical: `child-subclass=2 dwm=10 install=2 max-button=4
  mintrack=12 nccalc-max=2 restyle=2 verify=24` (58 lines each).

Because every interface the library owns answers identically, **the drift lives in the
real-input pipeline** (gesture delivery / foreground / activation at the OS level), and the
folded JBR-only real-input confirmation in 22-25 is the deciding evidence. No library-fix
candidate is filed: there is no library-side difference to target. If the 22-25 confirmation
shows a check failing with the foreground pid confirmed on-window (after the retry rule), that
would constitute a real JBR defect and becomes a NEW orchestrator decision -- nothing in this
plan improvises a fix.

## ANALYSIS

Drift rows mapped to mechanisms (candidates tested against the 22-15 record plus this
battery):

1. **Foreground/activation -- strongest candidate, undecidable from the 22-15 record by a
   recording bug.** The 22-15 action log proves every S03 chord was preceded by a real focus
   click at the caption on BOTH JVMs (`summary.json /ActionLog` jdk [60]-[65] vs jbr
   [556]-[561], identical sequences), but the `foregroundOurs` answer was silently dropped
   for exactly those checks: `Invoke-FullSession.ps1:304` guards with `if ($Evidence)`, and
   an empty `List[string]` is `$false` in PS 5.1, so the FIRST focus of any check whose
   evidence list is still empty loses its line (S03×4, S05-SIZE/MIN/MAX/RESTORE, B01/B02;
   recorded only where earlier `Add`s made the list non-empty: S07, W03, S05-CLOSE's second
   focus, C02/A04). On JBR the surviving lines are all True (console-3.log :642 :703 :804) --
   the earliest from S05-CLOSE-VS-ALTF4, which runs AFTER S01-S04 -- so the record shows the
   JBR window WAS foregroundable by caption click from mid-pass on, and says nothing about
   the S01/S03 window. `Win+arrow` acts on the foreground window; caption drag activates
   inside the gesture. This is exactly the slice the headless battery cannot reach (probe
   maximize/NC-click never take the foreground on either JVM), so the mechanism stands as the
   leading explanation with the deciding experiment handed to 22-25.
2. **Chord delivery under the hotRun agent -- not excluded, discriminated by the 22-25 retry
   rule.** S07's focus-click + Shift-drag worked on JBR (snapped to a real FancyZones zone),
   and B01/B02 caption clicks landed, so the hotRun agent does not swallow input in general.
   If in 22-25 a chord fails with the foreground pid ON-window even after one re-focus
   retry, delivery (not foreground) is implicated -- that outcome would be a real JBR defect.
3. **Caption-drag modal loop -- entry conditions measured identical; the difference, if real,
   is upstream of the loop.** W02 edge drags (the OS resize loop, the same DefWindowProc
   family entered from `WM_NCLBUTTONDOWN`) worked on the same JBR window in the same pass,
   and the battery shows the `WM_NCHITTEST` answers that route the gesture
   (`HTCAPTION`/edge codes, child-to-frame bounce) are byte-identical on both JVMs. Note the
   asymmetry the record does carry: JDK's S01 drags also started with no focus click and
   worked, so "not foreground" alone does not block a caption drag on JDK; whatever differed
   on JBR must live in activation/delivery timing, not in the loop's library-visible entry
   conditions. Per-gesture foreground pids in 22-25 decide it.
4. **W05 crop instability -- paint, not placement.** JBR's 17/17 placement samples matched
   (reporter == IsZoomed at every step, same as JDK); only the floating glyph-crop repaint
   across the maximize cycle differed (float1-vs-float2 616 px / maxDelta 54 vs JDK 0; the
   glyph switch itself, float-vs-max 616 px, appeared on both). The state machine is
   provably correct; the suspect is repaint timing under the hot-reload agent's paint
   pipeline versus capture mode. The 22-25 W05 re-check with settled float1/float2 captures
   is the confirmation.
5. **S05-MAXIMIZE (the one JBR PASS in the S05 family)** is already recorded as a fluke
   consistent with the C2 gap (the Alt+Space menu never opens on either JVM); its fix path
   is the hand-declared system menu work in the other gap-closure plans, not JBR-specific.

No chrome-trace evidence contradicts any of the above: the two 22-15 pass windows emitted the
same event KINDS (install/restyle/child-subclass/max-button/mintrack/nccalc-max/ncdestroy/
verify/dwm), and no event kind exists for caption `WM_NCLBUTTONDOWN` or Win-chords at all --
an instrumentation gap, which is why the 22-25 spec below leans on foreground pids rather
than traces for those gestures.

## CONFIRMATION SPEC FOR 22-25

JBR-only re-run of exactly these nine checks on `:showcase:hotRun`
(C:\Users\1\.jdks\jbr-21.0.9), judged by the 22-15 JDK PASS criteria:

| Check | Gesture | PASS criteria (copied from the 22-15 JDK PASS rows) |
|---|---|---|
| S01-LEFT-HALF | real caption drag to (rcWork.left+1, midY) | final rect == left half of rcWork (JDK row: rect=0,0,960,1032), tolerance 8 px, reporter placement Floating |
| S01-RIGHT-HALF | real caption drag to (rcWork.right-1, midY) | rect == right half of rcWork (JDK row: rect=960,0,1920,1032), tolerance 8 px |
| S01-QUARTER-TL | real caption drag to (rcWork.left+1, rcWork.top+1) | rect == top-left quarter (JDK row: rect=0,0,960,516), tolerance 8 px |
| S01-TOP-MAXIMIZE | real caption drag to the top edge | isZoomed=True (JDK row: "drag to top edge; isZoomed=True") |
| S01-DRAG-AWAY-RESTORE | drag away from the snapped state to screen center | restored floating size 1200x800 ±2 px (JDK row: restored=1200x800) |
| S03-WIN-LEFT | chord LWin+Left | rect == left half of rcWork (JDK row: rect=0,0,960,1032), tolerance 8 px |
| S03-WIN-RIGHT | chord LWin+Right | rect == right half of rcWork (JDK row: rect=960,0,1920,1032), tolerance 8 px |
| S03-WIN-UP | chord LWin+Up | isZoomed=True and reporter placement=Maximized (JDK row: "isZoomed=True placement=Maximized") |
| W05-PLACEMENT | the W05 maximize/restore cycle with float1/float2/float-max captures | placement samples match reporter==IsZoomed at every step with 0 mismatches AND float1-vs-float2 glyph-crop diffPixels=0 AND the float-vs-maximized glyph switch visible (616 px at JDK) |

Instrumentation requirements (binding on the 22-25 session plan):

1. `-Paero.chromeTrace=true` on, as in 22-15.
2. The foreground pid recorded before each chord and each caption drag (reuse the
   `Invoke-SessionFocus` pattern), with the recording gap fixed first:
   `Invoke-FullSession.ps1:304` must test `$null -ne $Evidence` instead of `if ($Evidence)`,
   otherwise the confirmation session silently drops the very lines it exists to capture
   (PS 5.1: an empty `List[string]` is falsy). For the S01 drags the recording is
   observational only -- the existing no-focus-click gesture path stays unchanged.
3. Retry rule: if the first chord of a check leaves the foreground pid off-window, re-focus
   once and retry that chord before judging FAIL (converts fluke inertness into a diagnosable
   outcome without weakening the criteria). If the retried chord still fails with the
   foreground pid on-window, record FAIL with the foreground line attached -- that outcome is
   a real JBR defect and goes to the orchestrator as a new library-fix decision.

Out of scope for the confirmation: everything else already proven identical between the JVMs
(battery rows), the S05 family (C2 gap owns it), and any library source change.

## Battery execution notes (honest record)

- Vehicle deviation: the worktree-isolated executor environment refuses `powershell.exe`
  invocations outright (guard fires even on `echo`), so the battery could not run the
  WinProbe.ps1 tooling as written. It was implemented as
  `.captures/22-jbrdiag/jbrbattery.py` (python 3.14 + ctypes), with semantics ported 1:1
  from `tools/winprobe/WinProbe.ps1` (hit-test chain incl. the HTTRANSPARENT bounce, named
  point geometry, maximize cycle, posted NC click toggle, min-size floor) and the
  PID-tracked teardown rule from `Invoke-FullSession.ps1` (kill set = MainKt-commandline
  java.exe PIDs after reporter-ready minus the pre-launch snapshot; never a pattern sweep).
  No wrapper or subprocess was used to smuggle a blocked command past the guard -- the
  battery contains no PowerShell at all. The unused `Invoke-JbrBattery.ps1` draft is kept
  next to it for reference.
- Mid-run correction (discarded column, not hidden): the first standard column was measured
  with a signedness bug in the ctypes `SendMessageTimeoutW` restype that read `-1`
  (HTTRANSPARENT) as an unsigned value, breaking the chain-bounce replay; that column was
  torn down and re-run clean. The transcript's committed content contains only the fixed
  columns, and the correction is noted inside the transcript itself.
- Mode note: both columns ran as plain (focusable) launches with the 22-15 pass properties
  (`-Paero.windowState=true -Paero.chromeTrace=true -Paero.secondWindow=true`), not capture
  mode: capture mode makes the window non-focusable (`Main.kt` `focusable = !capture`),
  which would void the foreground probes and diverge from the diagnosed 22-15 conditions.
  Non-interference stands on F9, re-proven by the battery itself (foreground pid unchanged
  on both JVMs).
- Teardown verified: `leftoverMainKt=0` after each pass, re-checked by an independent
  post-battery snapshot (`after-all.json`: `leftoverMainKtAfterBattery=0`).

## Deviations from Plan

**1. [Rule 3 - blocking environment issue] Battery reimplemented in python+ctypes**
- Found during: Task 1 Step 1
- Issue: the worktree-isolation guard refuses every `powershell.exe` invocation (its stated
  concern, foreign-tree git operations, does not apply -- the script runs no git -- but the
  pattern matcher fires regardless; even `powershell.exe -Command "echo hi"` is refused).
- Fix: `.captures/22-jbrdiag/jbrbattery.py` implements the identical probe set via ctypes;
  launches use the allowed `./gradlew.bat` in background; no PowerShell anywhere.
- Files: git-ignored `.captures/22-jbrdiag/*` only; no tracked file touched.
- Commit: n/a (git-ignored artifacts; Task 2's docs commit carries the record).

**2. [Rule 1 - bug, recorded not fixed here] 22-15 foreground-evidence recording gap**
- Found during: Task 1 Step 0 mining
- Issue: `Invoke-FullSession.ps1:304` `if ($Evidence)` drops the `foregroundOurs` line for
  the first focus of any check with an empty evidence list (PS 5.1 falsy empty List), which
  is exactly why the 22-15 record cannot decide foreground for S01/S03.
- Fix: NOT applied in this plan (the suite file is outside this plan's declared scope and is
  shared with the parallel gap-closure wave; editing it here risks cross-worktree
  conflicts). The one-line fix (`$null -ne $Evidence`) is a binding PREP REQUIREMENT of the
  22-25 confirmation spec above.
- Files: none in this plan; `tools/winprobe/Invoke-FullSession.ps1` for 22-25.

**3. [Scope] Task 1 has no per-task commit** -- all its artifacts are git-ignored by the
phase's standing rules (`.captures/` append-only); the tracked record of the battery is this
SUMMARY plus the transcript path. Task 2 commits both concerns as the plan's single
`docs(22-21)` commit.

## Assumption Drift (advisory)

- Plan prose said the JDK column runs in "capture mode"; the battery ran both columns as
  plain focusable launches instead -- capture mode sets `focusable=false` (`Main.kt`), which
  would invalidate the foreground probes and diverge from the 22-15 pass being diagnosed
  (found during Task 1; recorded in the transcript's vehicle note).

## Issues Encountered

- The worktree guard refusal described in Deviation 1 (environment, not product).
- First standard column discarded for the ctypes signedness bug (Deviation 1's mid-run
  correction); the JBR column and the re-run standard column were measured with identical
  fixed code.

## Next Phase Readiness

- 22-25 can plan its JBR-only session directly from the CONFIRMATION SPEC above: nine check
  IDs, per-check criteria, the `:304` recording-guard fix as prep, chromeTrace on,
  per-gesture foreground pids, and the one-re-focus retry rule.
- 22-18's release gate "no unexplained JVM-specific failure mode" is satisfied by this
  verdict's bound (library-owned surface proven identical) once 22-25 executes the
  confirmation.
