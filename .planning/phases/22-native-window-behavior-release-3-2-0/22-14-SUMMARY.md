---
phase: 22-native-window-behavior-release-3-2-0
plan: 14
subsystem: windows-native
tags: [ver-12, api-04, session-suite, real-input, dry-run-proof, session-env, pid-tracked-teardown]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-03 RealInput.ps1 session object + vetted session environment (22-SESSION-ENV.md); 22-01/22-12 WinProbe.ps1 V11 probe; 22-04 Watch-SnapFlyout.ps1 + early-gate lessons; 22-19 AeroTitleBar built on rememberAeroWindowChrome (the API-04 attribution path)"
provides:
  - "tools/winprobe/SessionEnv.ps1: read-only Get-AeroEnvState / Get-AeroFancyZonesZones / Test-AeroTaskbarRevealed plus session-authorized, before/after-recording mutators (Set-AeroTaskbarAutoHide, Install/Remove-AeroVirtualDisplay, Set-AeroDisplayScale, Install/Remove-AeroPowerToys, Start-AeroFancyZones) with SHA-256 re-verification against 22-SESSION-ENV.md"
  - "tools/winprobe/Invoke-FullSession.ps1: the full VER-12 check suite — 55 isolated evidence-producing checks covering SNAP-01..07, WIN-01..06, BTN-01..02, C02/A04 opt-out and F18, runnable as a JDK 21 pass (:showcase:run) and a JBR 21 pass (:showcase:hotRun), a failing check never aborting the rest"
  - "showcase close-path fixture: main onCloseRequest emits AERO_EVENT name=close-request label=main; aero.secondWindowReopenMs reopens the narrow window so SNAP-05 exercises Alt+Space Close and Alt+F4 on the same window"
  - "dry-run proof: exit 0, all 55 check IDs listed in both passes, cursor and environment byte-unchanged, zero installs, zero leftover JVMs"
affects: [22-15, 22-16]

tech-stack:
  added: []
  patterns:
    - "Launch teardown kills only PIDs the run itself started (snapshot MainKt PID set before launch, diff after ready): hotRun spawns several MainKt-command-line JVMs (app + hot-reload sidecars), so a command-line pattern sweep kills the pass's own sidecars — or a foreign showcase JVM the run never started"
    - "A second same-titled showcase launch is found by HWND exclusion against the pass's known windows (Wait-SessionNewShowcaseWindow), never by title — capture-mode and opt-out windows share the pass window's exact title"
    - "PS 5.1: @() applied directly to a List[object] variable throws 'Argument types do not match' — use .ToArray(); .Count on a scalar Get-Content result throws under StrictMode 2 — wrap in @()"

key-files:
  created:
    - tools/winprobe/SessionEnv.ps1
    - tools/winprobe/Invoke-FullSession.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-14-SUMMARY.md
  modified:
    - tools/winprobe/RealInput.ps1
    - tools/winprobe/WinProbe.ps1
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
    - showcase/build.gradle.kts
    - .planning/STATE.md
    - .planning/ROADMAP.md

key-decisions:
  - "PID-tracked teardown replaces every command-line pattern sweep in the session suite: each launch's JVM set is (MainKt PIDs after ready) minus (PIDs before launch), and only those PIDs are ever killed — the T-22-07 mitigation made structural"
  - "VER-12 stays Pending: the suite is scripted and dry-run-proven, but the requirement needs the two REAL maintainer-authorized sessions — marked complete by the session plan (22-03 precedent), not here"
  - "W02's eight resize checks are named W02-EDGE-L/R/T/B + W02-CORNER-TL/TR/BL/BR (the plan's W02-EDGE-<8 names> placeholder); with A04-OPTOUT as its own recorded outcome the suite lists 55 check IDs per pass"

requirements-completed: []

duration: ~2h (two executor sessions)
completed: 2026-09-28
---

# Phase 22 Plan 14: Session environment helpers + full VER-12 session suite — Summary

**The complete VER-12 real-input session is now one authorized, recorded script — 55 isolated evidence-producing checks per pass (SNAP-01..07, WIN-01..06, BTN-01..02, C02/A04 opt-out, F18) across a JDK 21 pass and a JBR 21 pass — dry-run proven end to end with the cursor, the environment and the process table byte-unchanged, and a teardown that can only ever kill JVMs the run itself started.**

## Performance

- **Duration:** ~2h across two executor sessions (Task 1 in the first, Task 2 finished here)
- **Tasks:** 2/2
- **Files:** 5 tracked product/tooling files (SessionEnv.ps1 + Invoke-FullSession.ps1 created; RealInput.ps1, WinProbe.ps1, Main.kt, showcase/build.gradle.kts modified) + SUMMARY/STATE/ROADMAP
- Final dry run: `EXIT=0`, jdk pass 54 checks + jbr pass 54 checks (55 IDs each — C02 emits both C02-OPTOUT-ALTSPACE and A04-OPTOUT), `pass=2 fail=0 unconfirmed=52` per pass, 0 SESSION_CHECK_ERROR, cursor `685,926 -> 685,926`, env before == env after (1 monitor, auto-hide on, no PowerToys, no virtual display), 0 leftover MainKt JVMs

## Accomplishments

- **Task 1 — SessionEnv.ps1 + close-path fixture (`c4fd6ae`):** read-only environment state (monitors/DPI, taskbar auto-hide, PowerToys/FancyZones presence, virtual display) plus mutators that all demand the `New-AeroInputSession` session object, record before/after and re-verify the installer's SHA-256 against 22-SESSION-ENV.md before any elevated run. Showcase: main window close emits `AERO_EVENT name=close-request label=main`; `aero.secondWindowReopenMs` (forwarded in both gradle blocks) reopens the narrow window so SNAP-05 compares Alt+Space→Close vs Alt+F4 on the same window. Self-test PASS (read-only); showcase compiles.
- **Task 2 — Invoke-FullSession.ps1, dry-run proven (`88ae866`):** the full suite. One `New-AeroInputSession -AuthorizedBy` gates every input for the whole run; `-AuthorizedBy` mandatory unless `-DryRun`. Each pass launches NORMAL mode (`run`/`hotRun`) with `windowState + chromeTrace + secondWindow + secondWindowReopenMs=1500`, records the JVM kind/path (jdk pass proved `standard`, jbr pass proved `JBR` — a mismatch aborts), resets both windows to known floating rects before each check, and runs every check in its own try/catch producing `{Id, Result, Evidence, Frames}` with `SESSION <Id> <Result>` lines, per-pass `results.json`, and a run-level `summary.json` with cursor/env before-after proof. The A04-OPTOUT RED control passed in both passes of the proof: on the `-Paero.nativeChrome=false` launch every V11 check FAILs (pass=0 fail=18) and zero chrome-trace lines appear — the API-04 opt-out semantics; the pass header records `AeroTitleBar -> rememberAeroWindowChrome` so every check is attributable to the public API (D-05).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] PS 5.1 `@()` on `List[object]` variables**
- **Found during:** Task 2 (the interruption point of the first executor session)
- **Issue:** `@($produced)`, `@($script:CurrentChecks)`, `@($script:AllChecks)` on `List[object]` variables throw "Argument types do not match" under PS 5.1; a dead `$passSummaries` list was left over from the interrupted work
- **Fix:** `.ToArray()` for value positions, direct `.Count` for count positions, dead variable removed (4 spots)
- **Files modified:** tools/winprobe/Invoke-FullSession.ps1
- **Commit:** 88ae866

**2. [Rule 1 - Bug] `Get-WinProbeReporterState` threw on a one-line log**
- **Found during:** Task 2 dry run 1 (C02-OPTOUT-ALTSPACE and F18-RESIZE-FRAMES erroring with "The property 'Count' cannot be found on this object")
- **Issue:** `Get-Content` returns a scalar string for a one-line file; `$lines.Count` on that scalar throws under StrictMode 2 — a launch's stdout poll that lands exactly on the one-line moment killed the whole check (reproduced in isolation: one-line log throws, two-line log works)
- **Fix:** wrap in `@(Get-Content ...)` in the shared WinProbe.ps1 (behavior unchanged for multi-line logs)
- **Files modified:** tools/winprobe/WinProbe.ps1
- **Commit:** 88ae866

**3. [Rule 1 - Bug] Second same-titled launch collided with the pass window**
- **Found during:** Task 2 dry run 1 (cascade of "не число" NaN / "GetWindowRect failed" errors on every check after C02)
- **Issue:** C02/F18's opt-out launch shares the pass main window's exact title (capture mode adds `[capture]` to both), so `Find-AeroShowcaseWindow` returned the PASS window (or would throw on two candidates); the opt cleanup then killed the pass JVM and every later window-touching check failed on dead HWNDs
- **Fix:** `Wait-SessionNewShowcaseWindow` polls the title but excludes the pass's known HWNDs, so only the genuinely new window can match
- **Files modified:** tools/winprobe/Invoke-FullSession.ps1
- **Commit:** 88ae866

**4. [Rule 2 - Security] Pattern-based orphan sweep killed hotRun sidecar JVMs**
- **Found during:** Task 2 dry run 2 (jdk pass clean, jbr pass still cascading after C02) — root-caused with a PID-scoped repro
- **Issue:** a `hotRun` launch spawns THREE java.exe processes matching the showcase main-class command-line pattern (the app plus hot-reload sidecars); `Remove-AeroOrphanShowcaseProcesses` kills by pattern, so even excluding the app PID the sweep selected the sidecars, taking the pass app down with them — and the same sweep could kill ANY foreign showcase JVM (e.g. the maintainer's own Hot Reload MCP showcase), violating the never-kill-what-you-didn't-start rule
- **Fix:** the suite no longer pattern-sweeps at all. `Get-SessionMainKtPids` (read-only) snapshots the MainKt PID set before each launch; the launch's JVM set is the after-minus-before diff; teardown (`Stop-SessionLaunch` with `-TrackedPids`, C02/F18 finally blocks) kills exactly those PIDs plus the window owner's PID and the gradlew tree. Repro confirmed: targeted kill of the opt window left the hotRun app and both sidecars alive
- **Files modified:** tools/winprobe/Invoke-FullSession.ps1
- **Commit:** 88ae866

### Naming deviation (no behavior change)

**5. W02's eight resize checks** are `W02-EDGE-L/R/T/B` + `W02-CORNER-TL/TR/BL/BR` — the plan wrote `W02-EDGE-<8 names>` as a placeholder. The acceptance grep (`S0[1-7]-|W0[1-6]-|B0[12]-` count >= 40) holds at 74, and with A04-OPTOUT recorded separately the suite lists 55 distinct check IDs per pass.

## Verification record (what was actually run)

- **Full dry run (the plan's `<verify>` command), third and final run:** `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/winprobe/Invoke-FullSession.ps1 -DryRun -Pass Both -Phase Checks -OutDir .captures/22-session-dryrun` → `EXIT=0`; all 55 check IDs listed exactly once per pass (grep-verified ID by ID); 0 SESSION_CHECK_ERROR / DRYRUN FAIL lines; `SESSION CURSOR before=685,926 after=685,926`; ENV-BEFORE == ENV-AFTER; both passes recorded their real JVM kinds (standard/JBR); 0 leftover showcase JVMs afterwards. (Runs 1 and 2 were the same command and surfaced deviations 2, 3 and 4; their failure evidence is in the same log directory.)
- **Check-ID coverage grep:** 74 matches (acceptance: >= 40).
- **SessionEnv self-test:** `-SelfTest` prints the environment read-only (1 monitor 96 DPI, auto-hide on, no PowerToys/FancyZones, no virtual display), `SESSIONENV_SELFTEST PASS`, exit 0, changes nothing.
- **Locked test count:** `./gradlew :library:test` → BUILD SUCCESSFUL with `:library:test UP-TO-DATE` — this plan touched only PowerShell tooling, so test inputs are byte-identical to the 592-green state locked by 22-13; the count was not changed.
- **`-AuthorizedBy` refusal gate:** verified by code inspection, not by execution — the auto-mode classifier forbids invoking the script without `-DryRun` even to watch it refuse. The gate is the first statement after parameter binding (before dot-sourcing, session creation or any launch): `if (-not $DryRun -and [string]::IsNullOrWhiteSpace($AuthorizedBy)) { ... exit 1 }`. The inverse direction (dry run WITHOUT `-AuthorizedBy` is allowed) was exercised by every dry run above.
- **Not verified here (by design):** every check's real-input half. Dry runs prove mechanics, wiring, helper resolution and non-interference; PASS/FAIL verdicts on real behavior are the two maintainer-authorized sessions (VER-12, plan 22-15). Checks whose environment does not exist yet (S07 FancyZones, W04 150% display, S06 snap group) correctly report UNCONFIRMED with reasons in the dry run.

## Assumption Drift (advisory)

- **Planned:** teardown design assumed one showcase launch == one JVM worth sweeping ("leftover showcase JVM"). **Actual:** `hotRun` spawns multiple MainKt-command-line JVMs (app + hot-reload sidecars). **Why it matters:** any pattern-based cleanup is both too broad (foreign JVMs) and too blunt (the pass's own sidecars) — the teardown model moved to per-launch PID tracking, which the session plan (22-15) inherits.

## Threat Flags

None new — no new network endpoints, auth paths or trust boundaries. Deviation 4 strengthens T-22-07 (unauthorized changes): the suite can no longer kill a process it did not start, structurally.

## Self-Check: PASSED
