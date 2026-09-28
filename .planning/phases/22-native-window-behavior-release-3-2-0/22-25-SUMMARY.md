---
phase: 22-native-window-behavior-release-3-2-0
plan: 25
subsystem: verification
tags: [ver-12, gap-closure, re-verification, real-input, jdk21, jbr21]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "The 22-20..22-24 gap-closure fixes on the build; the 22-22 -Checks subset contract; the 22-21 JBR confirmation spec; Task 1's readiness record (6e7a3d2)"
provides:
  - "The real-input verdict rows 22-26 flips requirement checkboxes from: JDK 10/12/1 (23 checks) + JBR 13/18/1 (32 checks) under recorded consent, with every non-PASS row explained (environment-blocked / honest FAIL / harness error)"
  - "Proof that landed on the gap-closure build: the system menu OPENS on JBR (S05-ALTSPACE PASS), the caption double-click works on JBR (S04 MAX+RESTORE real pair), JBR chords work with the window foreground (S03 RIGHT/UP/DOWN PASS), press parity is exact on JDK (B01-PRESS PASS), the W02 corner/floor formulas are correct (TR/BL/BR + both floors PASS on both JVMs), and W06 emits ncdestroy on JBR (PASS)"
  - "The JBR S01 caption-drag inertness REPRODUCED with stronger discrimination (same-point double-click works, chords work foreground-ours, edge drags work) — real drift, not hit-test/foreground/click-delivery"
  - "The environment anomaly record: VS Code (pid 12752, elevated) held/reclaimed foreground across focus clicks on both JVMs, blocking every foreground-gated gesture in those windows"
affects: [22-26 (the flip plan), 22-18 (release gate)]

tech-stack:
  added: []
  patterns:
    - "Verdict honesty under a hostile foreground environment: every non-PASS row carries its foreground line, and blocked rows are never read as product verdicts"
    - "Foreign-foreground attribution: probe the actual foreground holder read-only (pid + window title) instead of guessing from booleans"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-25-SUMMARY.md
  modified:
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION.md (appended "Gap-closure re-verification (22-25)")
  git-ignored-artifacts:
    - .captures/22-reverify/jdk/ (23-check pass evidence, results.json under jdk/jdk/)
    - .captures/22-reverify/jbr/ (32-check pass evidence, results.json under jbr/jbr/)
    - .captures/22-reverify/jdk-console.log
    - .captures/22-reverify/jbr-console.log
    - .captures/22-reverify/session-start.txt (consent + pass timestamps)
    - .captures/22-reverify/post-env-check.ps1 (independent teardown verification)

key-decisions:
  - "No FAIL row was retried beyond the script's own single S07 retry: the session discipline (no improvised retries of real input) outweighed recovering rows; the un-retried JBR S03-WIN-LEFT FAIL is recorded with its fluke evidence (S03-WIN-RIGHT passed foreground-ours one check later)"
  - "The foreground-blocked rows (JDK S03-DOWN/S05 family/S07, both S06 pre-snaps, JBR S07/B01-CLICK) are recorded as environment-blocked, NOT as product regressions — VS Code pid 12752 held foreground at those moments, so the gestures never reached the showcase window"
  - "requirements-completed stays empty on purpose: 22-26 flips only rows actually proved, and several proof targets were blocked by the environment anomaly this run"

requirements-completed: []  # 22-26 flips rows from this record; nothing is marked here

duration: ~23 min
completed: 2026-09-28
---

# Phase 22 Plan 25: Gap-closure re-verification session — Summary

**Under recorded consent, the 23-check JDK and 32-check JBR re-verification passes ran to completion and split the gap-closure story in three: the fixes that landed (menu opens, double-click, press parity, formulas, JBR ncdestroy), the JBR S01 drag drift reproduced with new discrimination, and an external foreground race (VS Code, elevated, pid 12752) that blocked every foreground-gated row it touched — all recorded honestly for 22-26.**

## Performance

- **Duration:** Task 2 ~23 min (consent 2026-09-28, JDK pass 17:33:21Z start, JBR pass 17:48:31–17:50:37Z, teardown verified by ~17:57Z)
- **Tasks:** 2/2 (Task 1 readiness merged earlier as 6e7a3d2; Task 2 this session)
- **Files modified:** 2 tracked (22-SESSION.md, this SUMMARY); evidence git-ignored under `.captures/22-reverify/`

## Task Commits

1. **Task 1: Readiness — headless V11 sweep, dry-run subset proof, honest time estimate** — `6e7a3d2` (merged to master in b4ec94b)
2. **Task 2: The supervised re-verification session** — `62d4b9e` (docs: session record appended to 22-SESSION.md)

**Plan metadata:** this commit (docs(22-25): complete).

## Verdict rows for 22-26 (the honest per-requirement list)

| Requirement (clause) | Verdict 22-26 should record | Basis |
|---|---|---|
| SNAP-03 (Win-arrow chords, JBR drift) | CONFIRMED WORKING on JBR when foreground | JBR S03-WIN-RIGHT/UP/DOWN PASS with foregroundOurs=True; LEFT FAIL was an un-retried foreground fluke (no retry wired); JDK S03-WIN-DOWN FAIL was foreground-blocked (chords went to VS Code). 22-21's leading explanation confirmed. |
| SNAP-04 (caption double-click) | PROVEN on JBR; JDK FAILED this run | JBR real MAX+RESTORE pair PASS from a verified floating reset; JDK S04-DBLCLICK-MAX FAIL (isZoomed=False) on the same reset discipline — a JVM-differential the record cannot explain; not flippable as a requirement. |
| SNAP-05 (system menu) | HALF-PROVEN: menu opens; command navigation inert | JBR S05-ALTSPACE-MENU PASS (#32768 of our pid, foreground=True) — the 22-23 fix opens. JBR MOVE/SIZE/MINIMIZE/MAXIMIZE FAIL with the menu open and window foreground (dx=0, 0x0, no state change; S05-SIZE left the window 160x28). JDK family untestable (foreground-blocked). Alt+F4 half PROVEN on JBR (narrow destroyed). |
| SNAP-01 (drag snap, JBR drift) | STILL FAILING on JBR — real drift, narrowed | JBR S01 x4 FAIL (rect unchanged) on a fresh launch; same-point double-click works, chords work foreground-ours, W02 edge drags work — not hit-test, not foreground-per-se, not click delivery. JDK S01 not re-run today (22-15 JDK PASS predates the WndProc changes — stale for flipping). |
| WIN-02 (floors) | PROVEN both JVMs | W02-MAIN-FLOOR 320 exact + W02-NARROW-FLOOR 260 exact (D-01), height unchanged, PASS on both. |
| WIN-02 (corners) | PROVEN 3/4 both JVMs | TR/BL/BR PASS with correct shapes + 60x60 on both; CORNER-TL FAIL on both with ZERO deltas (shape Arrow/Other) — the first corner runs immediately after S07; new anomaly, not the old formula bug. |
| BTN-01 (press parity clause) | PROVEN on JDK | B01-PRESS-FRAME PASS: press-max-vs-press-min diff=0 (parity exact), press-vs-rest=144 (press paints). JBR FAIL is the zero-diff cluster (visual never observed — not a parity difference). |
| BTN-01 (hover/click guards) | GREEN on JDK, blocked on JBR | JDK B01-HOVER PASS + B01-CLICK PASS (foreground=True); JBR triple FAIL clustered at pass end with foregroundOurs=False — occlusion/activation-shaped, not product-attributable from this record. |
| SNAP-06 (shared border clause) | UNPROVEN this session | Both passes failed at PRE-SNAP (narrow chord foreground-blocked, narrowSnapped=False) — per the 22-24 semantics this is chord-failure evidence, not border evidence; the border drag never ran against a landed pair. |
| SNAP-07 (FancyZones) | UNPROVEN this session | S07 FAIL on both: drag never started foreground-ours even after the script's one retry; zones were found (priority-grid, 3 zones, spacing 16 — the 22-22 model works). |
| WIN-05 (placement, JBR drift) | PLACEMENT + FLOAT-STABILITY PROVEN; glyph switch unobserved | JBR W05: 16/16 samples match, float1-vs-float2 diff=0 (the 22-15 616-px instability did NOT reproduce); float-vs-maximized diff=0 — the glyph-switch capture showed no difference this run (FAIL by that clause). |
| WIN-06 (close during drag) | PROVEN on JBR (incl. ncdestroy); JDK trace lag recorded | JBR W06 PASS (closeEvent=True ncdestroy=True, main alive, v11=0, hs_err=0). JDK: close path green but ncdestroy absent within the 10 s budget — the 22-22 open question decided for JBR (not pipe-lag), JDK-side lag unexplained. |
| SNAP-02 (flyout gate guard) | UNPROVEN — harness error | S02-FLYOUT UNCONFIRMED on both: the reference control matched (signal=Event, 708/652 ms) but the check threw formatting evidence (`Events.EventName` property error) before the signature verdict. |
| VER-11 | Holding | Task 1 sweep: 596 tests, V11 18/18 + 8/8 + clean RED control (6e7a3d2). |
| VER-12 (protocol) | SATISFIED | Consent «ок» recorded with timestamp before any input; input only inside the window; teardown verified (0x03 untouched, cursor restored, zero JVMs); return announced. |

## Accomplishments

- Both consented passes ran to completion with full evidence capture (55 check rows total across JDK+JBR, every row with its evidence list; frames and winprobe logs preserved under `.captures/22-reverify/`).
- Five gap-closure claims landed green where the gesture could be judged: JBR system-menu open, JBR double-click pair, JBR chords-with-foreground, JDK press parity, W02 formulas (both JVMs), JBR W06 ncdestroy.
- The JBR S01 caption-drag drift was reproduced and materially narrowed (not hit-test, not foreground, not click delivery — the drag modal loop itself), which is exactly the decision-grade evidence 22-21 wanted even though the verdict is negative.
- The machine was verified restored to its pre-session state (auto-hide 0x03 never toggled, cursor restored, zero leftover MainKt JVMs, screens/DPI unchanged, PowerToys+FancyZones running, no driver step, no UAC).

## Deviations from Plan

None fixed in-session (the plan's own rule: no improvisation inside the session). Recorded
observations that differ from the plan's expectations:

1. **[Recorded] Results path nesting** — the plan's verify expects
   `.captures/22-reverify/{jdk,jbr}/results.json`; the script writes one pass subdirectory
   deeper (`jdk/jdk/results.json`, `jbr/jbr/results.json`). Both files exist and are
   complete; the layout difference is documented here and in the session section.
2. **[Recorded] JDK pass exit code unrecorded** — the wrapper's exit-code echo was lost when
   the 600 s shell timeout moved the still-running pass to background; completion is fully
   evidenced by SESSION TOTALS, ENV-AFTER and summary.json. The JBR pass exited 1, the
   script's convention for "completed with FAIL rows" (not an abort).
3. **[Recorded] Maintainer observations not offered** — the plan's optional observation step
   (hover/flyout/zone click/taskbar group) would have required prompting inside the
   hands-off window; recorded as declined-by-protocol in the session section.
4. **[Recorded] Stray input to the foreground holder** — during foreground-blocked rows the
   scripted keys (Alt+Space, arrows, Alt+F4) and caption-point clicks were delivered to VS
   Code (pid 12752). It remained running and foreground after both passes; the maintainer
   was told to check the editor/chat for stray characters.

**Total deviations:** 0 auto-fixed (session discipline forbids in-session fixes; 4 honest
records above).
**Impact on plan:** the session's verdict value is intact; the environment anomaly reduced
coverage on the foreground-gated rows, which 22-26 must treat as unproven (not failed).

## Issues Encountered

- The external foreground race (VS Code, elevated) — dominant issue of the session; see the
  session section's "environment anomaly" for the full record and attribution rules used.
- Harness gaps found and left for follow-up (not fixed in-session): the S02-FLYOUT
  `Events.EventName` formatting throw; the missing 22-21 retry rule in
  `Invoke-SessionHotkeyCheck` (only S07 retries).
- The readiness estimate's mechanics term was measured on a warm Gradle daemon; the real
  JDK pass paid a cold launch (~9 min) before its 124 s check window. Total session wall
  ~17 min hands-off versus the announced ~7 min — the overage was launch time, not input
  time. (Assumption drift: dry-run launch warmth -> cold-launch wall-clock.)

## Known Stubs

None — no product code was written or modified in this plan.

## Threat Flags

None new. Real input stayed inside the consented window (T-22-25-01); no environment state
was changed beyond per-user app state that was already running (T-22-25-02); teardown was
PID-tracked and verified (T-22-25-03); every non-PASS row carries its evidence (T-22-25-04).

## User Setup Required

None.

## Next Phase Readiness

- 22-26 receives the verdict table above: flip only the proven rows (SNAP-03 JBR drift
  annotation, BTN-01 press clause on JDK evidence, WIN-02 floor clauses, the W06 JBR row)
  and record the rest as blocked/unproven with this session's evidence paths.
- Follow-up planning material (new findings, not 22-26 scope): the JDK S04 double-click
  differential; the system-menu command-navigation inertness on JBR (menu opens, commands
  don't act — including the 160x28 state drift); the JBR S01 drag-modal-loop drift; the
  first-corner-after-S07 anomaly; the JDK ncdestroy lag; the two harness gaps (S02
  EventName throw, missing S03 retry rule); and the foreground-race mitigation for any
  future session (close/elevate-down the foreign window, or wire the retry rule first).
- Release gate 22-18: no UNEXPLAINED JVM-specific failure mode remains unbounded — the JBR
  drift rows now carry foreground/discrimination evidence, though two of them (S01 drag,
  JDK-vs-JBR double-click) remain OPEN product questions for the next plan.

## Return announcement

The maintainer has been told they can return to the machine: **«Можно возвращаться»**
(announced in the completion report immediately after the teardown verification; the
environment was already restored and verified at that point).

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-28*
