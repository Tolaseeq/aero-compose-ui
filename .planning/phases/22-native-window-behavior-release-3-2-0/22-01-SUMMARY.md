---
phase: 22-native-window-behavior-release-3-2-0
plan: 01
subsystem: testing
tags: [powershell, win32, jna-precursor, verification, compose-desktop, vernier-harness]

# Dependency graph
requires: []
provides:
  - "showcase/src/main/kotlin/com/mordred/showcase/WindowStateReporter.kt — AERO_WINDOW_STATE / AERO_EVENT stdout reporter, gated by -Daero.windowState=true"
  - "tools/winprobe/WinProbe.ps1 + Invoke-WinProbe.ps1 — live-window Win32 probe library and runner (namespace AeroWinProbe), evaluates the 18 VER-11 check IDs"
  - "22-NOTES.md — RED baseline of the unmodified window, conflicts C1/C7 settled, C4 baseline recorded, findings F8-F11"
  - ".captures/22-baseline/ — red-main.json + AeroBlue/AeroDark/Classic-rest.png + AeroBlue-maximized.png (gitignored, not committed)"
affects: [22-02, 22-05, 22-06, 22-09, 22-15]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AeroWinProbe namespace for all new Win32 P/Invoke types (avoids clashing with tools/capture/AeroCapture.ps1's global RECT/POINT)"
    - "Real child-to-parent WM_NCHITTEST chain reproduction (ChildWindowFromPointEx descent, GetParent resend on HTTRANSPARENT) instead of a single frame-only hit-test"
    - "Guard-must-fail-on-old-code discipline applied to a live-window Win32 check (V11-MAX-WORKAREA redesigned per-edge after it false-positive-passed on unmodified code)"

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/WindowStateReporter.kt
    - tools/winprobe/WinProbe.ps1
    - tools/winprobe/Invoke-WinProbe.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
    - showcase/build.gradle.kts

key-decisions:
  - "V11-MAX-WORKAREA's tolerance must be per-edge (auto-hide edge: 1-4px uncovered; other edges: <=1px), not one blanket <=4px-on-every-edge check — the blanket form passed on the unmodified window because CMP's own maximize already fills the monitor exactly when auto-hide is on"
  - "F9 = safe: a probe-driven SC_MAXIMIZE never takes the foreground on this machine (measured 3 times), so maximize-dependent VER-11 checks run headless without -SkipMaximize"
  - "F8: the hit-test subclass must attach to the child HWND (SunAwtCanvas, Skiko's HardwareLayer), not only the frame — it covers the whole client area and answers WM_NCHITTEST first"

patterns-established:
  - "Pattern: RED-baseline-first for live Win32 guards — capture every check's answer on the genuinely unmodified window before any native code exists, and treat any check that PASSES on old code as a bug in the check itself (feedback_repro_must_exercise_path), not a shortcut to skip"

requirements-completed: [VER-11]

# Metrics
duration: 19min
completed: 2026-09-25
---

# Phase 22 Plan 01: Live-Window Probe Harness + RED Baseline Summary

**Built a PowerShell/Win32 live-window probe (`tools/winprobe/`) that reproduces Windows' real WM_NCHITTEST child-to-parent chain against the running showcase, added a Compose-side AERO_WINDOW_STATE stdout reporter, and captured the RED baseline proving all 18 VER-11 checks fail on the unmodified `WindowDraggableArea` window before any native hit-test code exists.**

## Performance

- **Duration:** 19 min (commit span 14:13-14:32; excludes file-reading/context-loading time before the first commit)
- **Started:** 2026-09-25T14:13:21+03:00
- **Completed:** 2026-09-25T14:32:38+03:00
- **Tasks:** 3
- **Files modified:** 6 (3 created + 1 new doc, 2 modified) across the 3 task commits, plus 1 fix commit

## Accomplishments

- `WindowStateReporter.kt` prints machine-readable `AERO_WINDOW_STATE`/`AERO_EVENT` lines from both Compose's `snapshotFlow` and raw AWT `ComponentListener`/`WindowStateListener` callbacks, gated by `-Daero.windowState=true`; a normal launch is unchanged
- `tools/winprobe/WinProbe.ps1` + `Invoke-WinProbe.ps1`: a genuine, working live-window probe that sends real `WM_NCHITTEST`/`WM_SYSCOMMAND` messages into the running showcase HWND via `SendMessageTimeout`, reproduces the OS's own child-to-parent hit-test descent, reads style bits, maximize geometry vs. monitor work area, `SHAppBarMessage` auto-hide state, min-size clamping, and a UI Automation summary
- Ran the probe against the genuinely unmodified showcase window on this machine and recorded a full RED baseline: all 18 `VER-11` checks FAIL (`V11 SUMMARY pass=0 fail=18 skip=0`, `RED OK`)
- Settled conflicts C1 (style bits) and the grep half of C7 (no JBR machinery in this repo) first-hand; recorded C4's baseline measurement for 22-02 to compare against
- Recorded findings F8 (hit-test subclass target must be the child HWND, not the frame), F9 (probe maximize is safe, doesn't steal foreground), F10 (CMP's own `UndecoratedWindowResizer` is already active today), F11 (no min-size floor exists today, confirming D-01's precondition)

## Task Commits

1. **Task 1: Showcase window-state reporter launch parameter** - `a033c79` (feat)
2. **Task 2: Live-window probe harness (tools/winprobe)** - `bc8241d` (feat)
3. **Task 3: RED baseline of the unmodified window + conflicts #1/#7a + findings F8-F11** - `423fc9d` (docs)

**Fix commit (found running Task 2's tool live for the first time in Task 3):** `0ac7b50` (fix)

**Plan metadata:** (this commit)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/WindowStateReporter.kt` - `AERO_WINDOW_STATE`/`AERO_EVENT` stdout reporter, no-op unless `-Daero.windowState=true`
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` - calls `WindowStateReporter(windowState, label = "main")` as the first statement in `Window { }`
- `showcase/build.gradle.kts` - forwards `aero.windowState`/`aero.chromeTrace` to both the `run` task and the Compose Hot Reload `ComposeHotRun` task family
- `tools/winprobe/WinProbe.ps1` - dot-sourceable probe library: Win32 P/Invokes under `namespace AeroWinProbe`, the named-point layout table, hit-test/maximize/taskbar/minsize/UIA/region-compare functions, `Invoke-WinProbeV11`
- `tools/winprobe/Invoke-WinProbe.ps1` - runner (`-Launch`, `-Report`, `-SkipMaximize`, `-Json`, `-AssertV11`, `-ExpectRed`, `-KeepRunning`, `-SelfTest`)
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` - conflicts C1-C7, findings F8-F11, full RED baseline table

## Decisions Made

- `V11-MAX-WORKAREA`'s tolerance is per-edge (auto-hide edge: delta must be 1-4px; other edges: delta must be <=1px) rather than one blanket "<=4px everywhere" tolerance — the blanket form is not a guard on this machine, since CMP's own maximize already fills the monitor exactly when the taskbar auto-hides (delta=0 on all four edges, which would pass a blanket check on the unmodified window)
- F9 measured as safe (foreground never taken by a probe-driven maximize) on this machine — maximize-dependent checks default to running headless; `-SkipMaximize` stays available for hosts where this doesn't hold
- F8 identifies the hit-test subclass target as the child HWND (`SunAwtCanvas`, Skiko's `HardwareLayer`), not the frame alone, for Plan 02+

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `V11-MAX-WORKAREA` false-positive-passed on the unmodified window**
- **Found during:** Task 3 (running the probe live for the first time)
- **Issue:** The check used one blanket "delta <=4px on every edge" tolerance comparing the maximized window rect to `rcWork`. On this machine's auto-hide-bottom monitor, `rcWork` already equals the full monitor (auto-hide means no space is reserved), and CMP's own placement-driven maximize already fills the monitor exactly — so the check PASSED on code with zero native hit-test logic, violating this project's "a guard must provably fail on unfixed code" rule (`feedback_repro_must_exercise_path`).
- **Fix:** Redesigned the check per-edge: an edge in `taskbar.AutoHideEdges` must show a delta of 1-4px (left uncovered for the auto-hide reveal strip); every other edge must show a delta of <=1px (flush). Re-run gives `RED OK` with all 18 checks failing.
- **Files modified:** tools/winprobe/WinProbe.ps1
- **Verification:** `Invoke-WinProbe.ps1 -Launch run -Report ... -ExpectRed` now prints `RED OK` (previously `RED BROKEN V11-MAX-WORKAREA`)
- **Committed in:** `0ac7b50`

**2. [Rule 3 - Blocking] Five runtime bugs found running the probe against a live window for the first time**
- **Found during:** Task 3
- **Issue:** (a) `$pid` collided with PowerShell's automatic `$PID` variable, throwing on every `Get-WinProbeWindowInfo` call; (b) `-Report`'s `[ValidateSet]` rejected a single comma-joined string before the sweep-precedent normalization could split it; (c) `(if (...) {...} else {...})` used as a bare function argument (parens denote an expression, not a statement) threw `CommandNotFoundException: if` at runtime in six `V11-*` check sites; (d) `Invoke-WinProbeV11`'s internal `Write-Output` calls flattened into its own `$results` return value, corrupting the caller's pass/fail counting; (e) `Where-Object | .Count` on a possible single-match result threw under `Set-StrictMode -Version 2` (PowerShell's single-item pipeline unwrap).
- **Fix:** (a) renamed to `$ownerPid`; (b) removed `[ValidateSet]`, added manual split + validate; (c) wrapped all six sites in `$(...)`; (d) switched to `Write-Host`; (e) wrapped in `@(...)`.
- **Files modified:** tools/winprobe/WinProbe.ps1, tools/winprobe/Invoke-WinProbe.ps1
- **Verification:** `-SelfTest` and a full live `-Launch run` pass end-to-end with no PowerShell errors
- **Committed in:** `0ac7b50`

---

**Total deviations:** 2 auto-fixed (1 Rule 1 bug in a check's own correctness, 1 Rule 3 batch of blocking runtime bugs)
**Impact on plan:** Both fixes were necessary for the probe tool (built in Task 2) to actually work against a live window in Task 3 and for VER-11's guard to be a real guard, not a false pass. No scope creep — no functionality was added beyond what Task 2/3 already specified.

## Issues Encountered

None beyond the deviations above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The probe harness (`tools/winprobe/`) and RED baseline (`22-NOTES.md`, `.captures/22-baseline/`) are ready for Plan 02 (styles + subclass spike), which must compare its own hit-test/maximize/style results against this exact baseline to prove GREEN
- F8 tells Plan 02 exactly where to attach the subclass (the child HWND, not just the frame)
- C4's baseline (SC_MAXIMIZE already syncs `WindowState.placement` with zero native code) is the comparison point for Plan 02's own C4 settlement
- No blockers. All showcase/gradle processes launched during this plan were stopped by PID; `git status --porcelain library/` remains empty and no JNA reference exists yet

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-25*

## Self-Check: PASSED

- FOUND: showcase/src/main/kotlin/com/mordred/showcase/WindowStateReporter.kt
- FOUND: tools/winprobe/WinProbe.ps1
- FOUND: tools/winprobe/Invoke-WinProbe.ps1
- FOUND: .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md
- FOUND: a033c79, bc8241d, 0ac7b50, 423fc9d
