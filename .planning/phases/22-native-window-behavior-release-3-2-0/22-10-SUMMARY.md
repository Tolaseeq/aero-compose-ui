---
phase: 22-native-window-behavior-release-3-2-0
plan: 10
subsystem: windows-native
tags: [win-05, snap-05, ver-11, wndproc, churn, gc-safety, hot-reload]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-02 subclass + C4/C7 settlements; 22-05 frame removal + taskbar-aware maximize; 22-06 corner policy; 22-07 live regions; 22-09 max-button bridge"
provides:
  - "OWNED_FRAME_MESSAGES / OWNED_CHILD_MESSAGES: the single declared audit of what each proc owns, enforced at dispatch — every other message forwards through CallWindowProc verbatim (PITFALLS 3)"
  - "NativeWindowChromeRegistry.verify(window) + churn guard: re-installs a lost frame proc on top of the CURRENT proc, subclasses new children, prunes dead ones; traces event=verify frame=ok / reinstall / child-subclass"
  - "WM_PARENTNOTIFY(LOWORD=WM_CREATE) observed-then-forwarded with an EDT hop into verify (new child canvases subclassed automatically)"
  - "Headless don't-break evidence: state sync 6/6, GC stress 500 hit-tests stable, UIA equal to baseline, 3 Hot Reload cycles with one install, V11 sweep unchanged vs 22-07, C2 headless half recorded"
affects: [22-11, 22-12, 22-13, 22-15, 22-16]

tech-stack:
  added: []
  patterns:
    - "Ownership-by-declaration: the message set IS the dispatcher's guard — a message cannot gain handling without being declared in the owned set (auditable by grep + code read)"
    - "Churn repair re-subclasses on top of the CURRENT proc and retires replaced procs into the registry entry (strongly referenced until WM_NCDESTROY) — never restores a stale saved pointer, never lets an in-chain Callback become GC-eligible (PITFALLS 1+4)"
    - "WNDPROC-equality evidence is in-process (registry compares its pointers to GWLP_WNDPROC and traces the outcome) because cross-process WNDPROC reads are meaningless (F8)"

key-files:
  created:
    - .captures/22-dontbreak/Invoke-DontBreakCheck.ps1 (git-ignored probe script)
    - .captures/22-dontbreak/Invoke-HotReloadCheck.ps1 (git-ignored probe script)
  modified:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - tools/winprobe/WinProbe.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "C4 = 'syncs automatically' (settled 22-02): NO WM_SIZE placement push added; the branch KDoc records why (a push could only risk a feedback loop, T-22-23) and the 6/6 state-sync table re-proves it on the hardened subclass"
  - "verify's intact outcome is traced (event=verify frame=ok) so probes have an in-process eviction signal — the check the plan words as 'GWLP_WNDPROC equal the registry's pointers'"
  - "WIN-05 / SNAP-05 / VER-11 stay Pending in REQUIREMENTS.md: snapping, Alt+Space and edge/corner/min-size clauses are Plan 11/15 scope (22-05/22-07/22-09 precedent)"

requirements-completed: []

duration: ~1h25m
completed: 2026-09-27
---

# Phase 22 Plan 10: Don't-break-what-works checkpoint — Summary

**The native subclass now declares its owned-message set once and enforces it at dispatch, repairs itself when AWT/Skiko evict or re-create HWNDs (verify + churn guard + WM_PARENTNOTIFY hop, re-installing on top of the current proc), adds no placement push (C4 re-proven 6/6), and headless evidence shows WindowState sync, GC safety, accessibility, Hot Reload and the whole passing V11 set intact before resize handling starts.**

## Performance

- **Duration:** ~1h25m (incl. one stuck probe run investigated and re-run)
- **Tasks:** 2/2 (passthrough audit + churn hardening; headless don't-break checks)
- **Files modified:** 5 (3 library, 1 tool, 1 NOTES) + 2 git-ignored probe scripts
- `./gradlew :library:test --rerun` — AERO_TEST_COUNT total=541 green after Task 1 (twice: before and after the componentMoved amendment) and after Task 2; `:showcase:compileKotlin` green
- Evidence: `.captures/22-dontbreak/run3-console.log` + `live.json`, `hotreload-console.log`, `v11.json` + `v11-console.log`, logs under `.captures/22-dontbreak/logs/`, 22-NOTES.md "Don't break what works (22-10)"

## Accomplishments

- `AeroWndProc.kt`: `OWNED_FRAME_MESSAGES` (11 messages) and `OWNED_CHILD_MESSAGES` (WM_NCHITTEST) declared with a per-message ownership-mode KDoc; both `handle` dispatchers now guard on their set, so nothing outside it can ever be touched and every owned branch returns `CallWindowProc`'s result except the three sanctioned cases (WM_NCCALCSIZE wParam 1, WM_NCHITTEST, NC buttons at HTMAXBUTTON). New `WM_PARENTNOTIFY` branch: forwarded untouched, and on LOWORD(wParam) == WM_CREATE hops to the EDT into registry verify. WM_SIZE KDoc records C4 = syncs automatically (no push exists, by design).
- `NativeWindowChromeRegistry.kt`: `verify(window)` (EDT) + `verifyByHwnd`; `reinstallFrameProc` re-subclasses on top of the CURRENT proc, retiring the replaced proc/pointer into the entry (strongly referenced until WM_NCDESTROY); `syncChildren` subclasses children not currently ours and prunes entries whose HWNDs no longer exist; intact checks trace `event=verify frame=ok`. `ChurnGuard` (ComponentListener shown/moved/resized + WindowStateListener) registered at install, removed at uninstall. `installOrReuse` now REUSES the entry after an eviction instead of replacing it — fixing a latent Pitfall 1 exposure where the replaced entry's still-in-chain procs could become GC-eligible.
- `Win32Interop.kt`: `WM_CREATE` constant for the WM_PARENTNOTIFY LOWORD test.
- Live evidence (22-NOTES.md): state sync 6/6 with a fresh intact verify trace after every step; GC stress 20 × (GC.run + 25 hit-tests) = 500 with byte-identical answers, no new hs_err; UIA `{Pane: 1}` equal to the 22-01 baseline; Hot Reload on JBR 3 reloads with install count 1 and identical answers; V11 sweep `pass=8 fail=10` identical to 22-07 (same expected Plan 11/12 FAILs); WS_SYSMENU present and GetSystemMenu/TrackPopupMenu absent from jna-platform 5.19.1 (C2 headless half).

## Task Commits

1. **Task 1: Passthrough audit, C4 action, HWND/proc churn hardening** - `9a0adac` (fix)
2. **Task 2: Headless don't-break checks + NOTES section** - `d424a21` (docs; preceded by tool commit `2e19b0f` adding WinProbe ShowWindow/SW_SHOWNOACTIVATE)

## Files Created/Modified

- `library/.../internal/windows/AeroWndProc.kt` — owned sets, dispatch guards, WM_PARENTNOTIFY hop, C4 KDoc
- `library/.../internal/windows/NativeWindowChromeRegistry.kt` — verify/reinstall/syncChildren, churn guard, entry reuse after eviction, listener removal at uninstall
- `library/.../internal/windows/Win32Interop.kt` — WM_CREATE
- `tools/winprobe/WinProbe.ps1` — ShowWindow + SW_SHOWNOACTIVATE
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` — "Don't break what works (22-10)"

## Decisions Made

- The WNDPROC-equality check is implemented as the registry's own in-process comparison (traced), because a cross-process GWLP_WNDPROC read is meaningless (F8) — the plan's wording is satisfied by the only mechanism that can actually observe it.
- verify is called from componentShown / componentMoved / componentResized / WindowStateListener (the plan named shown/resized; moved was added because the plan's own per-step check requires a post-move signal — a pure SetWindowPos move fires only componentMoved).
- Eviction repair never restores the originally-saved "previous" pointer: it re-subclasses on top of whatever is current, matching PITFALLS 4's "verify, don't blindly reinstall".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pure moves produced no verify trace**
- **Found during:** Task 2, first check run (move step FAIL with empty verify line)
- **Issue:** The plan wires verify into componentShown / componentResized / WindowStateListener, but a SetWindowPos move without resize fires only componentMoved — the plan's own "after EACH step: no silent eviction" check then has no observable signal for the move step.
- **Fix:** `ChurnGuard` also listens to componentMoved (one line; verify is idempotent and cheap). Amended into the Task 1 commit.
- **Files modified:** library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
- **Verification:** re-run — move step PASS with `event=verify frame=ok` trace; 541 green
- **Commit:** `9a0adac`

**2. [Rule 3 - Blocking] WinProbe lacked ShowWindow**
- **Found during:** Task 2 script preparation
- **Issue:** The restore-from-minimize step names ShowWindow(SW_SHOWNOACTIVATE); the probe's C# Native class had no such import.
- **Fix:** Added the DllImport + SW_SHOWNOACTIVATE constant to tools/winprobe/WinProbe.ps1 (own chore commit).
- **Files modified:** tools/winprobe/WinProbe.ps1
- **Commit:** `2e19b0f`

**3. [Rule 1 - Bug] acquire-after-eviction could orphan in-chain procs (pre-existing)**
- **Found during:** Task 1 implementation
- **Issue:** `installOrReuse`'s full-install path replaced the registry entry when our proc had been evicted; the old entry (holding the only strong reference to a proc that may still sit in the evictor's chain) became unreachable — a Pitfall 1 JVM-crash exposure the churn work exists to close.
- **Fix:** The eviction path now reuses the entry through `reinstallFrameProc` (old procs retire into `retiredProcs`, referenced until WM_NCDESTROY); children re-sync via `syncChildren` instead of unconditionally re-subclassing on top of themselves.
- **Files modified:** library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
- **Commit:** `9a0adac`

**4. [Mechanics] reload task name**
- **Found during:** Task 2, part B
- **Issue:** The plan says `./gradlew :showcase:reload`; the actual task is the top-level `reload` (22-02's own finding).
- **Fix:** Used `gradlew reload`; recorded in NOTES.
- **Commit:** `d424a21` (recorded)

---

**Total deviations:** 4 (2 blocking, 1 bug, 1 mechanics)
**Impact on plan:** All required to make the plan's own checks observable or to close a crash exposure in scope; no scope creep — resize bands, min-size floor and marked boundaries remain Plans 11–12.

## Assumption Drift (advisory)

- The plan's step text reads like the probe will compare GWLP_WNDPROC pointers directly; cross-process WNDPROC reads return 0 (finding F8, already recorded in 22-01), so the equality evidence is the registry's in-process verify trace instead (`event=verify frame=ok`) — same assertion, different observer.

## Issues Encountered

- **First probe run spun in `ConvertTo-Json` (tooling only):** all measurements had completed (5/6 state steps — the move step failed only for the missing verify signal, GC stress and UIA all PASS) but the final JSON dump of nested pscustomobjects burned 100% CPU for minutes; the run was stopped by hand (no product code involved; all processes the run started were killed). Flattening the stored entries fixed it; run 3 is the clean complete pass recorded in NOTES. PS 5.1 quirk documented in NOTES for future probe scripts.

## User Setup Required

None — no external service configuration required.

## Threat Model Coverage

- **T-22-01 (callback lifetime under churn):** mitigated — replaced procs retire into the registry entry and stay strongly referenced until WM_NCDESTROY; GC stress (20 forced GCs around 500 hit-tests) stable with no crash.
- **T-22-02 (swallowed messages):** mitigated — owned sets declared and enforced; UIA summary equal to the pre-phase baseline.
- **T-22-09 (Hot Reload stacking):** mitigated — three reload cycles held exactly one install with unchanged answers.
- **T-22-23 (C4 feedback loop):** structurally impossible — no placement push exists; assignments happen only in Compose's own pipeline.

## Known Stubs

None — verify is wired end to end (listeners + WM_PARENTNOTIFY → registry repair → traced outcome) and proven live.

## Next Phase Readiness

- Plan 11 (resize bands + min-size floor): the classifier's `resizeBandPx` seam and the ten standing V11 FAILs are its GREEN targets; the churn-hardened install it builds on self-repairs and is proven regression-free at this checkpoint.
- Plan 13 (pixel parity) / Plan 15 (real input): the WM_NCMOUSEMOVE forwarding path and the owned-message audit are the baseline they diff against; C2's hand-declare-only-if-needed condition is recorded.
- Plan 16 (unconfirmed list): no new unconfirmed items from this plan (the DWM visual remains maintainer-observed as before).

---

*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED
