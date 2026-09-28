---
phase: 22-native-window-behavior-release-3-2-0
verified: 2026-09-28T19:41:47Z
status: human_needed
score: 16/23 must-haves verified
has_blocking_gaps: false
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 15/23
  gaps_closed:
    - "BTN-01 press-fill parity — 22-23 diagnosed the 22-15 FAIL as a check-formula artifact (the check maximized its own window before the min step); parity proven exact on JDK 22-25 (B01-PRESS-FRAME PASS, diff=0); requirement flipped Complete"
    - "SNAP-05 menu-opens clause — 22-20 hand-declared GetSystemMenu/TrackPopupMenu landed (code verified this run); menu opens on JBR (S05-ALTSPACE-MENU PASS 22-25)"
    - "SNAP-04 mechanism — 22-20 WM_NCLBUTTONDBLCLK-at-HTCAPTION -> DefWindowProc (code verified this run); headless posted-message proof green on JDK; real MAX+RESTORE pair PASS on JBR 22-25"
    - "JBR drift, chords + W05 halves — chords PASS foreground-ours on JBR (S03-WIN-RIGHT/UP/DOWN 22-25); W05 float-instability did not reproduce (16/16 samples, float1-vs-float2=0); SNAP-03 and WIN-05 flipped Complete"
    - "All five minor check-formula gaps (M1-M5) — 22-22/22-23/22-24 fixes verified in tools/winprobe this run; re-run green wherever the gesture was judgeable (floors + corners TR/BL/BR both JVMs, S03-WIN-DOWN on JBR, W06 ncdestroy on JBR)"
    - "REL-08 verify-tag gate — version 3.2.0 committed (ff19cb7), v3.2.0-verify01 green on JitPack at exactly that SHA (re-confirmed live this verification), DEP-01 proven on the published POM/.module; release explicitly held by the maintainer per the documented hold branch"
  gaps_remaining:
    - "SNAP-04 JDK differential — real-input FAIL on JDK in the same 22-25 run that PASSED on JBR; unexplained by the record; needs the maintainer's manual check"
    - "SNAP-05 command navigation — observed inert once on JBR (menu open, window foreground) under the elevated-VS-Code foreground race; needs manual confirmation"
    - "SNAP-06 shared-border resize — never judgeable live (pre-snap foreground-blocked twice); library exonerated headlessly; needs manual drag test"
    - "SNAP-01 on JBR — caption-drag drift reproduced x4, narrowed to the drag modal loop; open"
  regressions: []
human_verification:
  - test: "Maintainer's manual round over the unpassed/unconfirmed items (the recorded basis of the release hold)"
    expected: "Caption double-click maximizes/restores on the JDK build; Alt+Space menu commands act (Move/Size/Min/Max/Close); two half-snapped windows resize together when their shared border is dragged; the pair shows a Snap Groups taskbar thumbnail; a Snap Layouts flyout zone click places the window; caption drag snaps on the JBR build"
    why_human: "Every scripted route to these rows is exhausted: UIA is blind inside the flyout, the 22-25 session was distorted by an elevated VS Code foreground race (WR-04: blocked SendInput is indistinguishable from a product failure), and two pre-snap attempts were environment-blocked. The maintainer explicitly claimed this verification («давай я попробую проверить непрошедшее и неподтверждённое руками…»)"
  - test: "WIN-04: move a window between a 100% and a real 150% monitor (drag, Win+Shift+arrow, maximize on 150%)"
    expected: "No size jumps; caption/buttons at correct scale; hit zones match what is drawn"
    why_human: "The virtual 150% display never attached on this machine; needs real hardware"
  - test: "Release cut decision after the manual round"
    expected: "Outcome A (items work): tag v3.2.0 on ff19cb7, push, JitPack ok, artifact resolves (22-18 Task 3 release branch). Outcome B (items fail): fix round, then a fresh release commit + verify02"
    why_human: "D-06: the real tag and any master push wait for the maintainer's explicit word; the hold is the maintainer's recorded decision"
---

# Phase 22: Native Window Behavior + Release 3.2.0 — Re-Verification Report

**Phase Goal (ROADMAP.md):** Windows treats windows built on `AeroTitleBar`/`AeroResizeHandles` as native windows — snapping, Snap Layouts, hotkeys, shared-border resize, taskbar-aware maximize, multi-monitor DPI, FancyZones — on a standard JDK 21 without JBR; released as `v3.2.0`.
**Verified:** 2026-09-28T19:41:47Z
**Status:** human_needed
**Re-verification:** Yes — after the gap-closure round (plans 22-20..22-26) and release plan 22-18 (held branch)

## What changed since the previous verification (2026-09-28T10:57:01Z)

The gap-closure wave (22-20..22-26) and release plan 22-18 executed. All five blocking gaps
were converted into mechanisms, evidence-backed diagnoses, or proven behaviors; all five
minor check-formula gaps were fixed and re-run. What remains open is a set of runtime
verdicts that only a human or a clean-foreground session can decide — exactly the set the
maintainer claimed in the recorded release hold. Commit existence, code wiring, evidence
files, requirement flips, and the remote/JitPack release state were all re-verified
first-hand in this run; no SUMMARY claim was taken on trust where code or records could be
checked.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Early real-input gate passed on the first draft (SC1) | VERIFIED | 22-04 `GATE PASS` (both control and Compose window); unchanged by gap closure |
| 2 | Edge/corner snap + drag-away restore incl. narrow window (SNAP-01) | UNCERTAIN (unverifiable_runtime) | JDK: S01 x5 PASS 22-15 + 22-21 battery proves the message-level path identical on the current build — evidence stands but predates the WndProc edits (22-26 calls it stale for flipping). JBR: S01 x4 FAIL reproduced 22-25, narrowed to the drag modal loop (22-UNCONFIRMED § 17). Both-JVM clause not establishable without a live run |
| 3 | Snap Layouts flyout on maximize-button hover (SNAP-02 flyout clause) | VERIFIED | S02-FLYOUT PASS both JVMs 22-15 (UIA/event signature, ~610-720 ms); gap closure left the flyout's non-KEYMENU syscommand forwarding verbatim; 22-25 re-run threw a harness error (`Events.EventName`) before verdict — tooling gap (§ 17), not an observed regression. Layout pick → human item |
| 4 | Win+arrow hotkeys behave natively (SNAP-03) | VERIFIED | JDK 22-15 S03-LEFT/RIGHT/UP PASS; JBR 22-25 S03-RIGHT/UP/DOWN PASS with foregroundOurs=True (the 22-15 JBR inertness was foreground-shaped, 22-21 confirmed); requirement flipped Complete by 22-26 |
| 5 | Caption double-click maximizes/restores (SNAP-04) | UNCERTAIN (unverifiable_runtime) | Mechanism landed and wired (this run: `AeroWndProc.kt:136,224-256` dispatches `WM_NCLBUTTONDBLCLK` via `ncDoubleClickDisposition`, HTCAPTION → `DefWindowProc`; `WndProcSupport.kt:60-66` + 8 guard tests; locked 596). Headless posted-message proof green on JDK (22-20); real MAX+RESTORE pair PASS on JBR 22-25; JDK real-input FAIL in the same run — an unexplained differential the record itself refuses to adjudicate («not flippable as a requirement») |
| 6 | Alt+Space menu opens; commands work; Close == Alt+F4 (SNAP-05) | UNCERTAIN (unverifiable_runtime) | Menu-opens clause FIXED and PROVEN: 22-20's hand-declared `GetSystemMenu`/`TrackPopupMenu` (verified in `Win32Interop.kt:162-172`, `AeroWndProc.kt:287-306`) — `S05-ALTSPACE-MENU` PASS on JBR 22-25. Commands clause: observed inert once on JBR (menu open, window foreground) under the session's elevated-foreground race — implementation exists and routes through the proven FORWARD path (headless-routed), no clean verdict obtainable. Alt+F4/Close equality PROVEN (both JVMs) |
| 7 | Shared-border resize of a snapped pair (SNAP-06 border clause) | UNCERTAIN (unverifiable_runtime) | Never judgeable live: 22-15 dragged un-snapped windows (check artifact), 22-25 pre-snap foreground-blocked both passes (hardened check honestly FAILed at PRE-SNAP — `.captures/22-reverify/` read this run). Library exonerated headlessly (22-24: border hit codes byte-match the OS seam convention in every reachable state, zero nccalc-max for snapped windows). Manual drag test is trivial for the maintainer |
| 8 | Snapped pair visible as a Snap Group thumbnail (SNAP-06 group clause) | UNCERTAIN (unverifiable_runtime) | Never observed within scripted windows (shell feature); human item (unchanged) |
| 9 | FancyZones snaps by zone (SNAP-07) | VERIFIED | 22-15 JBR Shift-drag landed at 16,16,1432,1016 — a real priority-grid zone; 22-22's `Get-AeroPriorityGridZones` (verified in `SessionEnv.ps1:263,458-469`) derives that rect exactly from the PowerToys model; zones re-found 22-25. 22-26 keeps the requirement Pending only because no green verdict row exists on the current build (22-25 drag env-blocked) — the observed behavior stands, nothing in the snap path changed |
| 10 | Taskbar-aware maximize incl. auto-hide edge (WIN-01) | VERIFIED | W01 both JVMs (22-15) + headless V11 re-proven post-gap-closure (22-20/22-23/22-24 proof packs, 18/18); flipped Complete |
| 11 | Native resize on every edge/corner + floors (WIN-02) | VERIFIED | Formula fixes verified in code (`Invoke-FullSession.ps1` corner two-axis, floor width-only); re-run 22-25: floors 320/260 exact and corners TR/BL/BR PASS both JVMs; edges PASS 22-15 both. Open nit: TL corner read inert on both JVMs in 22-25 (§ 17 anomaly; 22-15 observed values were correct) — warning, not a blocker |
| 12 | No system-frame traces; corner/shadow per maintainer choice (WIN-03) | VERIFIED | W03 both JVMs; 22-16 FULL-FRAME 0 px; gap closure touched no paint path; V11 packs green |
| 13 | 100% <-> 150% DPI moves keep sane size (WIN-04) | UNCERTAIN (unverifiable_runtime) | 150% virtual display never attached on this machine; needs real hardware (human item, unchanged) |
| 14 | Maximize/restore glyph + placement match real state (WIN-05) | VERIFIED | 17/17 samples JDK (22-15) + 16/16 JBR (22-25, zero mismatches); glyph switch proven 22-15 both JVMs (588/616 px); the 22-15 JBR float-instability did NOT reproduce (diff=0); flipped Complete. Nit: the 22-25 glyph capture read flat (§ 17) |
| 15 | Multiple windows independent, close-safe (WIN-06) | VERIFIED | W06-INDEPENDENT + OPEN-WHILE-DRAGGING PASS both (22-15); W06-CLOSE-DURING-DRAG full PASS on JBR 22-25 incl. ncdestroy within budget (the M4 question decided for JBR); JDK close path green, JDK ncdestroy lag open (§ 17 nit) |
| 16 | Maximize button hover + press + click parity (BTN-01) | VERIFIED | Gap B4 CLOSED: 22-23 proved the 22-15 press FAIL was the check maximizing its own window before the min step (no library defect; C5's fallback condition never met); parity proven exact on JDK 22-25 (`B01-PRESS-FRAME` PASS, press-max-vs-min diff=0, press-vs-rest 144); hover 0 px both JVMs; click toggle both; flipped Complete. JBR 22-25 triple FAIL = end-of-pass zero-diff cluster (environment-attributed, § 17) |
| 17 | Full real-input session green on BOTH JDK 21 and JBR 21 (SC4) | UNCERTAIN (unverifiable_runtime) | Gap B5 SPLIT-RESOLVED for chords and W05 (now green foreground-ours on JBR). The both-green aggregate still not achieved: 22-25 subsets read JDK 10 PASS/12 FAIL/1 UNCONFIRMED, JBR 13/18/1 (results.json read this run — matches 22-25-SUMMARY exactly); residual FAILs are the § 17 open items plus rows the elevated VS Code (pid 12752) foreground race blocked (WR-04 explains how blocked SendInput masquerades as product FAIL). Deciding evidence is the maintainer's manual round / a clean-foreground session |
| 18 | Min/close/leading/marked clicks are ordinary clicks (BTN-02) | VERIFIED | B02 x4 PASS both JVMs; client-click routing untouched by gap closure; flipped Complete |
| 19 | Public API additive + opt-out + custom title bar, no JNA in signatures (API-01..04) | VERIFIED | Regression: no public API change in gap closure (22-26's Kotlin diff comment-lines-only); `AeroTitleBar.kt:104` still built on `rememberAeroWindowChrome`; `PublicApiNoJnaTest` present; opt-out RED control re-proven (22-25 Task 1, 6e7a3d2) |
| 20 | Non-Windows behaves as before | VERIFIED | Legacy branch untouched by gap-closure commits (code-read + commit diffs are tools/docs-only) |
| 21 | Verification infrastructure honest and reproducible (VER-11..14) | VERIFIED | Locked count raised 592 → 596 by named commit `2336728` with the guard's own RED record (VER-14 discipline held); V11 main 18/18 + narrow 8/8 re-proven on the gap-closure build (6e7a3d2 + three proof packs); 22-25 session under recorded consent with verified teardown (VER-12); consolidated HANDOFF/UNCONFIRMED (VER-13) |
| 22 | README + KDoc updated, "Aero Snap limitation" removed (REL-06/07) | VERIFIED | This run: `## Windows window behavior` section present; "Aero Snap limitation" zero matches; "Known gaps" section reworded from the 22-25 verdict table (names every clause without a green row); AeroTitleBar KDoc carries the double-click/Alt+Space statements with honest runtime qualifiers (JDK verdict open / commands not yet acting) |
| 23 | v3.2.0 release per the documented criterion (REL-08, SC6) | VERIFIED (held branch) | 22-18's success criterion: "v3.2.0 published and resolving on JitPack **(or explicitly held by the maintainer)**, with DEP-01 confirmed on the published artifact." Live first-hand this run: `build.gradle.kts:4 version = "3.2.0"`; tag `v3.2.0-verify01` exists locally and on origin and dereferences to `ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae` (= RELEASE_SHA, `chore(release): 3.2.0`); JitPack API returns `status: ok, commit: ff19cb7…, isTag: true`; no `v3.2.0` tag local or remote; remote master unmoved (`bbe3658`). DEP-01 confirmed on the published POM/.module (jna + jna-platform 5.19.1 runtime-only) + single-JNA scratch consumer. The hold is the maintainer's recorded decision (verbatim reply in 22-18-SUMMARY Task 2), so the literal "tag pushed" bar is intentionally unmet — the cut is a human action, not missing work |

**Score:** 16/23 truths verified (16 VERIFIED, 7 UNCERTAIN-runtime — 0 FAILED)

No truth is FAILED-by-absence: every previously missing mechanism now exists in code,
is wired, and is guarded (SNAP-04/05 mechanisms, formula fixes, test raise). The seven
UNCERTAIN truths are runtime verdicts the recorded evidence cannot decide — contradictory
cross-JVM rows, environment-blocked gestures, UIA-blind shell surfaces, and absent hardware
— precisely the items the maintainer claimed for manual verification when holding the
release.

### Required Artifacts (re-checked this run)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/.../internal/windows/AeroWndProc.kt` | WndProc subclass + SNAP-04/05 ownership | VERIFIED | 511 lines (was 426); `WM_NCLBUTTONDBLCLK`/`WM_SYSCOMMAND` in OWNED_FRAME_MESSAGES; dispatch lines 136/140; `handleNcDoubleClick` (224-256), `handleSysCommand` (287-306) with TrackPopupMenu re-dispatch; CallWindowProc fallback intact |
| `library/.../internal/windows/Win32Interop.kt` | Hand-declared GetSystemMenu/TrackPopupMenu | VERIFIED | Lines 162-172, `WinDef.HMENU`, SC_KEYMENU/TPM_* constants present |
| `library/.../internal/windows/WndProcSupport.kt` | Pure routing seams | VERIFIED | `systemMenuDisposition` (0xFFF0 mask), `ncDoubleClickDisposition` (3-way); 8 @Test guards in WndProcSupportTest |
| `library/build.gradle.kts` | lockedTestTotal 596 + guard | VERIFIED | Line 11 `val lockedTestTotal = 596`; guard raises with named-commit discipline (raise commit 2336728 verified) |
| `tools/winprobe/Invoke-FullSession.ps1` | Formula fixes + -Checks | VERIFIED | -Checks validation/abort, `Wait-SessionChromeEventAfter`, `Reset-SessionWindow(s)`, corner two-axis verdicts, floor width-only, S06 pre-snap verification (lines 1420-1451), B01 reset + size guard |
| `tools/winprobe/SessionEnv.ps1` | Priority-grid zone model | VERIFIED | `Get-AeroPriorityGridZones` (263) + priority-grid branch (458-469) reproducing the PowerToys geometry |
| `build.gradle.kts` (root) | `version = "3.2.0"` | VERIFIED | Line 4, in commit ff19cb7 (the tagged verify commit) |
| `.captures/22-reverify/{jdk,jbr}/` | 22-25 session evidence | VERIFIED | Both results.json present; verdict counts read directly: JDK 10/12/1, JBR 13/18/1 (match 22-25-SUMMARY) |
| `.captures/22-gapmenu/` | 22-20 headless proof | GONE (documented) | Worktree-local by the phase's git-ignore rule; destroyed at worktree removal — evidence quoted verbatim in the committed 22-20-SUMMARY (recorded as Assumption Drift there, not hidden) |
| All previously verified artifacts (registry, DWM policy, chrome API, tests, probes, fixture) | Regression | VERIFIED | All present; internal/windows 12 files 1854 lines; AeroTitleBar wiring line 104; PublicApiNoJnaTest at verification/; showcase fixture unchanged |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| WM_NCLBUTTONDBLCLK at HTCAPTION | DefWindowProc SC_MAXIMIZE/SC_RESTORE | `ncDoubleClickDisposition` → `User32.INSTANCE.DefWindowProc` | WIRED (code lines 238-256; headless-proven; JBR real-input proven) |
| WM_SYSCOMMAND(SC_KEYMENU) | GetSystemMenu → TrackPopupMenu(TPM_RETURNCMD) → SendMessage re-dispatch | `handleSysCommand` | WIRED (code lines 287-306; menu-open proven on JBR) |
| Non-KEYMENU syscommands (Alt+F4, flyout) | FORWARD path unchanged | `systemMenuDisposition` masks to FORWARD | WIRED (tests: SC_CLOSE/MAXIMIZE/MINIMIZE/RESTORE/0x1234 all FORWARD) |
| Session driver | subset re-runs | `-Checks` parameter | WIRED (validation + dispatch filter; dry-run proven; used by 22-25) |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| AeroWndProc hit-test/syscommand answers | region snapshot / wParam | Compose onGloballyPositioned → registry → WndProc | Yes — V11 18/18 on the current build; 22-21 battery value-identical across JVMs | FLOWING |
| 22-25 session evidence | per-check Result/Evidence | real input under consent | Yes — results.json verdicts read this run, counts match all records | FLOWING |
| Release state | tag → JitPack build | git push of verify tag | Yes — live API check this run: ok @ ff19cb7 | FLOWING |

### Behavioral Spot-Checks (this verification)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| JitPack verify build green at release SHA | `curl https://jitpack.io/api/builds/.../v3.2.0-verify01` | `status: ok`, `commit: ff19cb7…`, `isTag: true` | PASS |
| Verify tag points at release commit | `git rev-parse v3.2.0-verify01^{commit}` | `ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae` | PASS |
| Hold invariant: no real tag, master unmoved | `git ls-remote --tags origin v3.2.0*`; `git ls-remote origin refs/heads/master` | only verify01; master `bbe3658` | PASS |
| Release version committed | `grep version build.gradle.kts` | line 4 `version = "3.2.0"` | PASS |
| Locked test count raised | `grep lockedTestTotal library/build.gradle.kts` | 596 with guard; raise commit `2336728` present | PASS |
| 22-25 evidence matches records | python read of `.captures/22-reverify/*/results.json` | JDK 10 PASS/12 FAIL/1 UNCONF; JBR 13/18/1 | PASS |
| Gap-closure commits exist | `git log -1` x14 | all 14 present (9fc02d7…9058325) | PASS |
| README caveat removed / Known gaps present | grep | "Aero Snap limitation" 0; "Known gaps" 1 | PASS |
| Debt markers in phase code | grep TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER | 0 matches in internal/windows + navigation chrome | PASS |
| Full `:library:test` run | not run by verifier | — | SKIP — Gradle exceeds spot-check budget; recorded evidence: 22-18 Task 1 `AERO_TEST_COUNT total=596 skipped=0 expected=596`, BUILD SUCCESSFUL on the release commit itself |

### Probe Execution

| Probe | Command | Result | Status |
|-------|---------|--------|--------|
| tools/winprobe live probes | not run (need live GUI + real input; side-effect contract) | — | SKIP — recorded evidence re-read instead: `.captures/22-reverify/{jdk,jbr}/results.json` verdicts parsed directly this run; `.captures/22-final/v11-green-console.log` present |

### Requirements Coverage (28 IDs)

Union of `requirements:` across all 26 plans = the 28 phase IDs; no orphans, none unclaimed
(re-confirmed; gap-closure plans 22-20..22-26 claim exactly the reopened IDs).

| Requirement | Source Plan(s) | Status | Evidence |
|-------------|----------------|--------|----------|
| SNAP-01 | 22-15/20-26 | PARTIAL — JDK proven (22-15 + battery-identical), JBR drag open; checkbox Pending (honest) | § 17; `.captures/22-reverify/jbr/` |
| SNAP-02 | 22-03/04/15 | Flyout SATISFIED both JVMs; layout pick NEEDS HUMAN; checkbox Pending (honest) | S02-FLYOUT PASS 22-15; § 17 harness error |
| SNAP-03 | 22-15/26 | SATISFIED — flipped Complete | JDK 22-15 + JBR 22-25 foreground-ours |
| SNAP-04 | 22-15/20 | UNCERTAIN — mechanism proven, JDK differential open; Pending | This run: code + tests verified; 22-25 rows |
| SNAP-05 | 22-15/20 | PARTIAL — menu opens (proven); commands need human; Pending | S05-ALTSPACE PASS JBR; § 17 |
| SNAP-06 | 22-15/24 | UNPROVEN live (library exonerated); Pending; group + border → human | § 12, § 17 |
| SNAP-07 | 22-15/22 | Behavior observed (22-15 real zone, model derives it); 22-26 keeps Pending for no current-build green row — honest, not contradicted | 22-22 arithmetic proof re-verified in code |
| WIN-01..03, 05, 06 | (flipped 22-26) | SATISFIED — checkboxes [x] | 22-25 rows + proof packs |
| WIN-04 | 22-15 | NEEDS HUMAN (hardware) — Pending | § 2-3 |
| BTN-01, BTN-02 | 22-23/26 | SATISFIED — checkboxes [x] | B01-PRESS PASS JDK; B02 x4 both |
| API-01..04 | 22-07/12/19 | SATISFIED | unchanged by gap closure (regression-checked) |
| DEP-01 | 22-02/18 | SATISFIED — now also on the published artifact | live JitPack + POM/.module runtime scope + consumer |
| SHW-17 | 22-08 | SATISFIED | fixture present, exercised every pass |
| VER-11..14 | 22-01/12/13/16/20 | SATISFIED | 596 locked + guard; V11 18/18 current build |
| REL-06/07 | 22-17/26 | SATISFIED | README/KDoc re-verified this run |
| REL-08 | 22-18 | HELD-BRANCH SATISFIED; literal tag bar awaits the maintainer — checkbox Pending (honest) | live checks this run (see truth 23) |

REQUIREMENTS.md checkbox states match the 22-26 flip audit exactly (7 flipped, 8 Pending —
SNAP-01/02/04/05/06/07, WIN-04, REL-08); nothing is silently passed or failed.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (phase code) | — | No debt markers in internal/windows or chrome/navigation files | — | — |
| 22-REVIEW.md WR-01 | MinimumSize.kt:46-55 | AWT minimum (px) treated as dp in Compose-side clamp — app floor inflated at scale != 1.0 | Warning (advisory) | Latent DPI-path defect; interacts with the unconfirmed WIN-04 territory; backlog candidate |
| 22-REVIEW.md WR-02 | NativeWindowChromeRegistry.kt:124-139 | Deferred install path has no try/catch — a throw leaves window undraggable while isNative=true | Warning (advisory) | Robustness; backlog candidate |
| 22-REVIEW.md WR-05 | AeroWndProc.kt:260-267 | NC press state can stick on non-HTMAXBUTTON release | Warning (advisory) | Latent interaction-state bug; backlog candidate |
| 22-REVIEW.md WR-03/WR-04 | SessionEnv.ps1 / RealInput.ps1 | Session-tooling: wrong-monitor DPI throw without restore; SendInput return discarded (blocked injection looks like product FAIL) | Warning (advisory) | Tooling only — but WR-04 materially explains part of the 22-25 FAIL texture |
| ROADMAP.md line 129 | — | Milestone line says "`v3.2.0` released on JitPack (completed 2026-09-28)" while the release is held (no v3.2.0 tag) — the "explicitly held" branch lives in 22-18-PLAN's success_criteria and ROADMAP's wave-16 annotation/last-updated note, not in SC6's literal text | Warning (docs) | Bookkeeping overstatement; fix when the release outcome lands |
| deferred-items.md | — | README dependency snippet omits `google()` (pre-existing since v3.1.0) | Info | Already parked in-phase; one-line README follow-up |
| repo root | — | Untracked `hs_err_pid*.log` / `replay_pid*.log` crash residue | Info | Add to .gitignore or delete |

### Human Verification Required

1. **Maintainer's manual round over the unpassed/unconfirmed items** (the recorded basis of
   the release hold). Test: on the 3.2.0 build — double-click the caption (JDK app and JBR
   app), open Alt+Space and run Move/Size/Minimize/Maximize/Close, half-snap two windows and
   drag their shared border, hover the taskbar button for the Snap Groups thumbnail, hover
   the maximize button and click a flyout zone, drag the JBR window by the caption to an
   edge. Expected: native behavior for each. Why human: UIA is blind inside the flyout; the
   scripted 22-25 session was distorted by an elevated VS Code foreground race (WR-04:
   blocked input injection is indistinguishable from a product failure); the maintainer
   explicitly claimed this verification verbatim in 22-18 Task 2.
2. **WIN-04 on real hardware** — drag / Win+Shift+arrow / maximize between a 100% and a real
   150% monitor; no size jumps, correct scale, hit zones match the drawing. Why human: the
   virtual 150% display never attached on this machine.
3. **Release cut decision** — after the manual round: Outcome A → tag `v3.2.0` on ff19cb7,
   push, confirm JitPack ok + artifact resolves (22-18 Task 3 release branch); Outcome B →
   fix round first (the § 17 items), then a fresh release commit + verify02. Why human: D-06
   reserves the real tag and any master push for the maintainer's explicit word.

### Re-Verification Summary

The gap-closure round did what it claimed: every one of the five blocking gaps from the
previous verification was either fixed and proven (BTN-01 press parity — the FAIL was the
check's own state pollution; JBR chords + W05 — foreground-shaped, proven green
foreground-ours), or landed as a real mechanism with headless proof and partial live proof
(SNAP-04 double-click, SNAP-05 menu-opens), or was exonerated headlessly with the live
verdict blocked by a hostile environment (SNAP-06 border). All five minor formula gaps were
fixed in the tooling and re-run green where judgeable. The release reached its documented
held branch: the exact release commit is proven green on JitPack (re-confirmed live), DEP-01
is proven on the published artifact, and the real tag waits — by the maintainer's recorded
decision — on a manual round over exactly the items automation cannot decide. Nothing
actionable remains for a code plan ahead of that human input; hence `human_needed`, not
`gaps_found`.

---

_Verified: 2026-09-28T19:41:47Z_
_Verifier: Claude (gsd-verifier)_
