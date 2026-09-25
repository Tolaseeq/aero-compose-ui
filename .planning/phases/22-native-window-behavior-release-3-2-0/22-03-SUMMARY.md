---
phase: 22-native-window-behavior-release-3-2-0
plan: 03
subsystem: testing
tags: [powershell, win32, sendinput, uia, win-automation, real-input, virtual-display, powertoys]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "Plan 01's live-window probe harness (tools/winprobe/WinProbe.ps1, Invoke-WinProbe.ps1) and Plan 02's native WndProc subclass (HTMAXBUTTON now answered through the real hit-test chain)"
provides:
  - "tools/winprobe/RealInput.ps1 -- SendInput-based cursor move/click/drag/key-chord, every function gated by an AeroInputSession that only a non-empty -AuthorizedBy can create; -DryRun sends nothing"
  - "tools/winprobe/Watch-SnapFlyout.ps1 -- EnumWindows + UI Automation baseline/diff watcher for the Snap Layouts flyout, never reads window titles, Name kept only for shell-process elements"
  - "tools/winprobe/Invoke-EarlyGate.ps1 -- scripted 1-2 minute VER-12 early session: Readiness, PositiveControl (notepad), ComposeFlyout, DragSnap, DragAway, Cleanup"
  - ".planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md -- vetted virtual-display-driver and PowerToys candidates, Authenticode-checked, install/uninstall/restore commands, nothing installed"
affects: [22-04, 22-15]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AeroInputSession gate: every SendInput-capable function requires a session object only New-AeroInputSession (non-empty -AuthorizedBy) can create; -DryRun sessions log the action and never call SendInput"
    - "Shell-process allow-list for UIA Name capture (explorer/ShellExperienceHost/ShellHost/StartMenuExperienceHost/SearchHost) -- Name is blanked for every other process's element, by construction"
    - "Write-Host, not Write-Output, for a function's diagnostic lines when the function also returns a value -- Write-Output flows into the caller's captured pipeline instead of the console (same gotcha WinProbe.ps1's Invoke-WinProbeV11 already documented; rediscovered live in RealInput.ps1's -SelfTest and fixed the same way)"
    - "Positive-control button-span discovery via Invoke-WinProbeHitTest row-scan (language-independent, no window-text read) instead of matching a locale-specific title string"

key-files:
  created:
    - tools/winprobe/RealInput.ps1
    - tools/winprobe/Watch-SnapFlyout.ps1
    - tools/winprobe/Invoke-EarlyGate.ps1
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md
  modified: []

key-decisions:
  - "Virtual display driver candidate: VirtualDrivers/Virtual-Display-Driver 25.7.23 (MttVDD) -- installer, driver DLL and catalog all independently downloaded to session scratch and Authenticode-checked Valid (SignPath Foundation via GlobalSign GCC R45 CodeSigning CA 2020), no test-signing needed; devcon.exe (bundled, Microsoft-signed) does the install/remove; DisplayConfig module's Set-DisplayScale targets only the virtual display's own DisplayId for the 150% scale"
  - "PowerToys candidate: microsoft/PowerToys v0.101.2362.0 PowerToysUserSetup (per-user, no UAC), Authenticode Valid Microsoft Corporation -- winget is absent on this machine, confirming 22-RESEARCH.md's own fallback path"
  - "Invoke-EarlyGate.ps1's -DryRun exit code reports whether the dry run itself completed with the cursor unchanged, not the (deliberately inapplicable) FLYOUT/SNAP/RESTORE verdict -- a dry run can never legitimately produce those OK verdicts, so gating exit code on them would make -DryRun always fail"
  - "virtual-driver-manager.ps1's own Get-PnpDevice -HardwareID example does not run on this machine's PowerShell 5.1 PnpDevice module (confirmed via ParameterSets); 22-SESSION-ENV.md records the working -InstanceId form instead of copying the unverified upstream snippet"

patterns-established:
  - "Real-input scripts in this repo take a session object gated by a non-empty -AuthorizedBy string, never an implicit always-on switch"
  - "Environment vetting for a later plan's real install downloads candidates only into the session's own scratch directory outside the repo, hashes and Authenticode-checks them in place, and never runs the installer"

requirements-completed: [VER-12]

# Metrics
duration: 9min
completed: 2026-09-25
---

# Phase 22 Plan 03: VER-12 Real-Input Tooling + Session Environment Vetting Summary

**Built the guarded SendInput driver, UI Automation Snap Layouts watcher, and a scripted 5-step early real-input gate for VER-12, then independently downloaded, hashed and Authenticode-verified the virtual-display-driver and PowerToys candidates for the full session (VirtualDrivers/Virtual-Display-Driver 25.7.23 and PowerToys v0.101.2362.0, both Valid, nothing installed).**

## Performance

- **Duration:** 9 min (commit span 15:29-15:38; excludes file-reading/context-loading and the
  environment-vetting downloads/verification before the first commit)
- **Started:** 2026-09-25T15:29:00+03:00
- **Completed:** 2026-09-25T15:37:43+03:00
- **Tasks:** 3
- **Files modified:** 4 created (RealInput.ps1, Watch-SnapFlyout.ps1, Invoke-EarlyGate.ps1, 22-SESSION-ENV.md)

## Accomplishments

- `RealInput.ps1`: SendInput-based `Move-AeroCursor`/`Invoke-AeroMouseButton`/`Invoke-AeroDrag`/
  `Invoke-AeroClick`/`Invoke-AeroDoubleClick`/`Send-AeroKeyChord`, `Get-AeroCursorShape` (GetCursorInfo
  vs. LoadCursor handles), `Save-AeroCursor`/`Restore-AeroCursor` -- every input-sending function
  requires an `AeroInputSession` only `New-AeroInputSession -AuthorizedBy <non-empty>` can create;
  `-SelfTest` proves a planned drag + Win+Left chord send nothing and the cursor stays put
- `Watch-SnapFlyout.ps1`: `Get-AeroShellSnapshot` (EnumWindows class/process/rect + UIA root-children
  class/AutomationId/ControlType/process/rect, Name kept only for
  explorer/ShellExperienceHost/ShellHost/StartMenuExperienceHost/SearchHost), `Wait-AeroSnapFlyout`
  (poll-diff against baseline, collects up to 40 descendants per new element),
  `Test-AeroFlyoutSignatureMatch`, `Get-AeroSnapSettings` (read-only); `-SelfTest` proves no non-shell
  element ever carries a `Name`
- `Invoke-EarlyGate.ps1`: Readiness (reads Snap settings, never writes) -> Launch (blocks on JBR) ->
  PositiveControl (notepad, language-independent HTMAXBUTTON-span scan) -> ComposeFlyout (hover +
  signature match) -> DragSnap (drag to left edge, rect-vs-work-area tolerance) -> DragAway (drag to
  center, size-vs-pre-snap tolerance) -> Cleanup (cursor restore, process teardown, JSON report);
  refuses to run without `-AuthorizedBy` unless `-DryRun`, verified live (exits 1, sends nothing);
  `-DryRun` run verified live: cursor unchanged (890,986 before and after), notepad never started,
  full action plan printed and written to `.captures/22-gate/dryrun.json`, exit 0
- `22-SESSION-ENV.md`: downloaded `VirtualDisplayDriver-x86.Driver.Only.zip`, `VDD.Control.25.7.23.zip`
  and `PowerToysUserSetup-0.101.2362.0-x64.exe` into session scratch (never the repo), ran
  `Get-AuthenticodeSignature` on every installer/driver/catalog file -- all `Status=Valid`
  (VDD: SignPath Foundation via GlobalSign; PowerToys: Microsoft Corporation) -- and recorded SHA-256,
  install/uninstall/verify/restore commands, and the 150% single-display scale method
  (`DisplayConfig` module's `Set-DisplayScale -DisplayId <virtual-display-id>`)

## Task Commits

1. **Task 1: RealInput.ps1 + Watch-SnapFlyout.ps1** - `e757691` (feat)
2. **Task 2: Invoke-EarlyGate.ps1** - `47b48a5` (feat)
3. **Task 3: 22-SESSION-ENV.md** - `62db7cb` (docs)

**Plan metadata:** (this commit)

## Files Created/Modified

- `tools/winprobe/RealInput.ps1` - guarded SendInput driver, `namespace AeroRealInput`, session-gated
- `tools/winprobe/Watch-SnapFlyout.ps1` - EnumWindows + UIA baseline/diff watcher, `namespace AeroSnapWatch`
- `tools/winprobe/Invoke-EarlyGate.ps1` - scripted early VER-12 session runner
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md` - vetted temporary
  environment (virtual display driver, PowerToys, taskbar auto-hide toggle, teardown checklist)

## Decisions Made

- Virtual display driver: VirtualDrivers/Virtual-Display-Driver 25.7.23 (MttVDD), Authenticode Valid
  on all three checked files, no test-signing needed; `devcon.exe install/remove "Root\MttVDD"`;
  150% scale via the `DisplayConfig` module targeting only the virtual display's `DisplayId`
- PowerToys: v0.101.2362.0 `PowerToysUserSetup` (per-user, no UAC), Authenticode Valid Microsoft
  Corporation; winget confirmed absent on this machine (matches 22-RESEARCH.md's own finding)
- `Invoke-EarlyGate.ps1 -DryRun`'s exit code reflects "the dry run completed with the cursor
  unchanged", not the FLYOUT/SNAP/RESTORE verdict, which a dry run can never legitimately satisfy
- 22-SESSION-ENV.md's `Get-PnpDevice` verification command uses `-InstanceId`, not the upstream
  community script's `-HardwareID` (which does not exist on this machine's PnpDevice module,
  confirmed via `(Get-Command Get-PnpDevice).ParameterSets` before writing it into the doc)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `RealInput.ps1 -SelfTest` and `Watch-SnapFlyout.ps1 -SelfTest` printed nothing**
- **Found during:** Task 1, first live run of the self-tests
- **Issue:** `Test-AeroRealInputSelf`/`Test-AeroWatchSnapFlyoutSelf` used `Write-Output` for their
  diagnostic lines while also `return`-ing a boolean; the top-level `$ok = Test-...` assignment
  captured every `Write-Output` line into `$ok` instead of letting it reach the console (an array,
  not a boolean, which also silently defeated the intended pass/fail branch) -- the exact gotcha
  `tools/winprobe/WinProbe.ps1`'s own `Invoke-WinProbeV11` already documents and works around.
- **Fix:** Switched both functions' diagnostic lines to `Write-Host`, matching the existing
  documented pattern; kept `return $true`/`return $false` as the only pipeline output.
- **Files modified:** tools/winprobe/RealInput.ps1, tools/winprobe/Watch-SnapFlyout.ps1
- **Verification:** Both `-SelfTest` invocations now print the full plan/counts and exit 0.
- **Committed in:** `e757691` (Task 1 commit -- found and fixed before the task's own commit)

**2. [Rule 1 - Bug] `Invoke-EarlyGate.ps1 -DryRun` exited 1**
- **Found during:** Task 2, first live `-DryRun` run against the plan's own `<verify>` command
- **Issue:** The exit-code logic gated on `$verdict -eq 'GATE PASS'` unconditionally; a dry run
  never sends real input, so `FLYOUT OK`/`SNAP OK`/`RESTORE OK` can never be true, making
  `-DryRun` always exit non-zero regardless of correctness -- contradicting the plan's own
  acceptance criterion ("the dry run exits 0").
- **Fix:** For `-DryRun`, the exit code now reflects whether the dry run itself completed with
  the cursor unchanged (`$savedCursor` vs. the post-run `Save-AeroCursor` read), not the
  (deliberately inapplicable) pass/fail verdict.
- **Files modified:** tools/winprobe/Invoke-EarlyGate.ps1
- **Verification:** `-DryRun -Json .captures/22-gate/dryrun.json` now exits 0; JSON records
  `CursorBefore` == `CursorAfter` (890,986 both).
- **Committed in:** `47b48a5` (Task 2 commit -- found and fixed before the task's own commit)

**3. [Rule 1 - Bug] `Get-PnpDevice -HardwareID` in the draft SESSION-ENV.md doesn't run here**
- **Found during:** Task 3, live-testing the "nothing installed" verification commands before
  writing them into the doc
- **Issue:** The upstream project's own `virtual-driver-manager.ps1` uses
  `Get-PnpDevice -HardwareID "Root\MttVDD"`; this machine's PowerShell 5.1 `PnpDevice` module has
  no `-HardwareID` parameter set (`(Get-Command Get-PnpDevice).ParameterSets` lists only
  `ByInstanceId`/`ByFriendlyName`/`ByPresence`/`ByClass`/`ByStatus`), so copying that command
  verbatim would have shipped a verification step that fails to even parse.
- **Fix:** Verified `-InstanceId 'ROOT\MttVDD\*'` works instead and recorded that form.
- **Files modified:** .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md
- **Verification:** Confirmed `Get-Command Get-PnpDevice`'s parameter sets live before writing
  the corrected command into the doc.
- **Committed in:** `62db7cb` (Task 3 commit -- found and fixed before the task's own commit)

---

**Total deviations:** 3 auto-fixed (all Rule 1 -- bugs found and fixed by actually running each
tool live before committing, per this project's own "a guard/command must provably work, not be
assumed" discipline).
**Impact on plan:** All three fixes were necessary for the delivered tools to actually work as
specified; no scope creep beyond what Tasks 1-3 already called for.

## Issues Encountered

None beyond the deviations above.

## User Setup Required

None - no external service configuration required. Nothing was installed this plan; the full
VER-12 session (a later plan) will consume `22-SESSION-ENV.md`'s already-vetted commands under the
maintainer's own admin-rights confirmation, per PROJECT.md's pre-authorized plan.

## Next Phase Readiness

- `RealInput.ps1`, `Watch-SnapFlyout.ps1` and `Invoke-EarlyGate.ps1` are ready to run the moment
  the maintainer says "ok" for the early VER-12 session (a later plan/step) -- every real-input
  path is proven reachable only through an authorized, non-DryRun session
- `22-SESSION-ENV.md` gives the full-session plan (virtual display + PowerToys + auto-hide) a
  vetted, reversible starting point instead of a live decision
- No blockers. All showcase/gradle processes launched during this plan's dry run were stopped by
  PID (confirmed via process listing after cleanup); `git status --porcelain` stays clean of
  anything beyond this plan's four new files; all environment-vetting downloads lived only in the
  session scratch directory and were deleted after verification, never committed

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-25*

## Self-Check: PASSED

- FOUND: tools/winprobe/RealInput.ps1
- FOUND: tools/winprobe/Watch-SnapFlyout.ps1
- FOUND: tools/winprobe/Invoke-EarlyGate.ps1
- FOUND: .planning/phases/22-native-window-behavior-release-3-2-0/22-SESSION-ENV.md
- FOUND commits: e757691, 47b48a5, 62db7cb
