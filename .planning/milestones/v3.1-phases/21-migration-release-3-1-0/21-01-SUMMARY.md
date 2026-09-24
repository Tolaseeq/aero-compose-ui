---
phase: 21-migration-release-3-1-0
plan: 01
subsystem: testing
tags: [compose-desktop, powershell, winapi, printwindow, showcase, capture-tooling]

# Dependency graph
requires: []
provides:
  - "BASE-01: showcase `-Paero.section=<Name> -Paero.page=<N> -Paero.capture=true` launch parameters — opens one section, scrolled to a whole-viewport page, with no mouse input, non-focusable, starts behind other windows"
  - "BASE-02: committed PrintWindow(hwnd, hdc, 2) capture helper (tools/capture/AeroCapture.ps1) with pixel primitives, an input-state probe, and a covered/minimized non-interference self-test"
  - "tools/capture/Invoke-ShowcaseSweep.ps1 — sweep driver (theme x section x page), manifest.json, orphan-process sweep"
affects: [21-02, 21-03, 21-04, 21-10, 21-11, hot-reload-mcp-plans, verification-plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Gradle property -> JVM system property forwarding (aero.section/aero.page/aero.capture), extending the existing aero.scheme idiom in showcase/build.gradle.kts"
    - "AERO_READY single-line stdout contract (scheme/section/page/pages/viewportPx/contentPx/scrollPx/background/jvm), polled by an external driver instead of a GUI automation tool"
    - "PowerShell 5.1 + C# 5 Add-Type P/Invoke library, double-load guarded, for WinAPI capture (PrintWindow/ShowWindow/SetWindowPos/GetDpiForWindow) with LockBits pixel primitives"
    - "Non-interference proof: measure cursor+foreground before/after every capture, retry the whole capture up to 3x, else throw NON-INTERFERENCE VIOLATION"

key-files:
  created:
    - tools/capture/AeroCapture.ps1
    - tools/capture/Test-NonInterference.ps1
    - tools/capture/Invoke-ShowcaseSweep.ps1
  modified:
    - .gitignore
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
    - showcase/build.gradle.kts

key-decisions:
  - "SHOWCASE_SECTIONS covers all 17 top-level blocks including Foundation/Primitives, per 21-CONTEXT.md's resolution of RESEARCH Open Question 2"
  - "AERO_READY reporter reads colors.background from composable scope before entering LaunchedEffect (AeroTheme.colors is a @Composable getter, cannot be called from the coroutine body)"
  - "Invoke-AeroWindowCapture retries the whole capture (not just the input-state measurement) up to 3 attempts when cursor/foreground state changed, matching the plan's 'repeat the whole capture' wording"
  - "Invoke-ShowcaseSweep.ps1 normalizes -Themes/-Sections by re-splitting each element on comma, because powershell.exe -File binds a comma-joined CLI value as one string element instead of an array in this host"

patterns-established:
  - "Non-interfering capture pipeline (launch parameter + PrintWindow) is the single before/after mechanism for the rest of Phase 21 — no MCP needed until CMP 1.12.0 (Plan 07+)"

requirements-completed: [BASE-01, BASE-02]

# Metrics
duration: ~25min
completed: 2026-09-23
---

# Phase 21 Plan 01: Non-interfering showcase launch + PrintWindow capture pipeline Summary

**Showcase gained `-Paero.section/-Paero.page/-Paero.capture` launch parameters that render one section scrolled to a whole-viewport page with an `AERO_READY` stdout contract, and a committed PowerShell/WinAPI `PrintWindow` capture library + sweep driver proved (self-test PASS at 96 DPI) to capture that window covered and minimized without moving the real cursor or stealing focus.**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-09-23T15:50Z (approx., context load)
- **Completed:** 2026-09-23T16:15:23+03:00 (last commit)
- **Tasks:** 3/3 completed
- **Files modified/created:** 7 (4 modified, 3 created)

## Accomplishments
- Showcase now opens a single named section, scrolled to a deterministic page, with zero synthetic input, non-focusable and starting behind other windows in capture mode — while the default (no `aero.section`) launch path is byte-for-byte the same 17-block page as before.
- A `PrintWindow(hwnd, hdc, 2)`-based capture library (`AeroCapture.ps1`) is committed, self-proven against its own pixel-comparison fixtures, and contains none of the banned input/activation/screen-scrape WinAPI calls (grep gate = 0 matches).
- The sweep driver (`Invoke-ShowcaseSweep.ps1`) launches, captures, and terminates one Gradle showcase process at a time, and its `-SelfTest` mode proved on this machine, on the OLD toolchain (Kotlin 2.4.10 / CMP 1.11.1), that capture works when the window is covered by an opaque magenta occluder and when it is minimized, with the cursor position and foreground window handle unchanged around every step.

## Task Commits

Each task was committed atomically:

1. **Task 1: Ignore .captures/ and add the section/page/capture launch parameters (BASE-01, D-01)** - `00e8e65` (feat)
2. **Task 2: PrintWindow capture helper, pixel primitives and input-state probe (BASE-02)** - `8229805` (feat)
3. **Task 3: Sweep driver with self-test — covered, minimized, no cursor/focus change (BASE-02 proof)** - `45026c0` (feat)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `.gitignore` - adds `.captures/` (D-01: no image artifact of this phase is ever committed)
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` - block-bodied `main()` reads `aero.section`/`aero.page`/`aero.capture` once; capture window is centered, non-focusable, starts behind other windows via `window.toBack()`; `undecorated=true`/`transparent=false` unchanged
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` - `SHOWCASE_SECTIONS` (17 names), `section`/`page` params wrap all 17 top-level blocks in `shows(name)`, `AERO_READY`/`AERO_SECTION_UNKNOWN` reporter via `LaunchedEffect` + `snapshotFlow`/`withFrameNanos`
- `showcase/build.gradle.kts` - forwards `aero.section`/`aero.page`/`aero.capture` to the `run` task's system properties, alongside the existing `aero.scheme` forwarding
- `tools/capture/AeroCapture.ps1` (new) - PS 5.1 library: `AeroCaptureNative` (P/Invoke: EnumWindows/PrintWindow/ShowWindow/SetWindowPos/GetWindowRect/GetCursorPos/GetForegroundWindow/GetDpiForWindow/WindowFromPoint/GetAncestor/SetThreadDpiAwarenessContext), `AeroPixels` (LockBits `CountDiff`/`DiffCells`/`GetArgb`/`HasColorBlock`/`MaxChannelDelta`), `NoActivateForm`; PowerShell functions `Initialize-AeroDpiAwareness`, `Get-AeroInputState`, `Find-AeroShowcaseWindow`, `Invoke-AeroWindowCapture`, `Show-AeroOccluder`, `Test-AeroOccluded`, `Stop-AeroProcessOfWindow`, `Test-AeroPixelsSelf`
- `tools/capture/Test-NonInterference.ps1` (new) - HRM-03 probe: prints `Get-AeroInputState` as compact JSON
- `tools/capture/Invoke-ShowcaseSweep.ps1` (new) - drives theme x section x page launches, captures N frames per combination, writes `manifest.json`, kills orphan showcase JVMs by command line; `-SelfTest` proves the covered/minimized/non-interference case

## Decisions Made
- `SHOWCASE_SECTIONS` addresses all 17 top-level blocks (including `Foundation`/`Primitives`, wrapped as whole `Column`s) — resolves 21-RESEARCH.md Open Question 2 per 21-CONTEXT.md's note that this is Claude's discretion.
- The `AERO_READY` reporter's background-ARGB read had to use the `colors` value already captured in composable scope (`val colors = AeroTheme.colors` at the top of `ShowcaseApp`), not a fresh `AeroTheme.colors.background` call inside the `LaunchedEffect` coroutine body — `AeroTheme.colors` is a `@Composable` getter and cannot be invoked from non-composable code (compile error, caught immediately by `:showcase:compileKotlin`).
- `Invoke-AeroWindowCapture`'s non-interference handling retries the *entire* capture cycle (SetWindowPos + PrintWindow + sanity check + before/after measurement) up to 3 total attempts when the cursor/foreground state changed, rather than only re-measuring input state — this matches the plan's literal instruction to "repeat the whole capture."
- `Invoke-ShowcaseSweep.ps1` re-splits `-Themes`/`-Sections` array parameters on comma internally: when invoked as `powershell.exe -File ... -Sections Buttons,Range` from outside a PowerShell session (this repo's Bash tool spawns `powershell.exe` directly), the host bound the comma-joined value as a single one-element array entry instead of splitting it, which surfaced as a real `AERO_SECTION_UNKNOWN` failure during the mandated mini-sweep proof run — fixed with a normalization step so the script is robust to either binding behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `AeroTheme.colors` read from a non-composable coroutine context**
- **Found during:** Task 1 (`:showcase:compileKotlin` verification)
- **Issue:** The `AERO_READY` reporter's `LaunchedEffect` body called `AeroTheme.colors.background.toArgb()` directly; `AeroTheme.colors` is a `@Composable` property getter, so this failed with `@Composable invocations can only happen from the context of a @Composable function`.
- **Fix:** Reused the `colors` value already captured at the top of `ShowcaseApp` (composable scope) instead of re-reading `AeroTheme.colors` inside the effect.
- **Files modified:** `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt`
- **Verification:** `./gradlew :showcase:compileKotlin` → BUILD SUCCESSFUL
- **Committed in:** `00e8e65` (Task 1 commit)

**2. [Rule 1 - Bug] `-SelfTest` terminated the showcase launch before the covered/minimized cases ran**
- **Found during:** Task 3 (writing `Invoke-ShowcaseSweep.ps1`, caught before the mandated proof run by re-reading the drafted script)
- **Issue:** The self-test block initially called the shared `Invoke-AeroCaptureLaunchCycle` helper for the c1 capture; that helper's `finally` always terminates its own launch on return (by design, for the sweep loop), which would have killed the window and invalidated `hwnd` before the covered (`Show-AeroOccluder`) and minimized (`ShowWindow`) cases could run against the same window.
- **Fix:** Rewrote the self-test block to own its launch lifecycle directly (`Start-ShowcaseCaptureLaunch` + `Wait-AeroReadyLine` + `Find-AeroShowcaseWindow`, kept alive through c1/c2/c3, terminated once in the outer `finally`).
- **Files modified:** `tools/capture/Invoke-ShowcaseSweep.ps1`
- **Verification:** `-SelfTest` run produced `AERO_SELFTEST PASS` with all three PNGs (c1/c2/c3) present and non-empty.
- **Committed in:** `45026c0` (Task 3 commit)

**3. [Rule 1 - Bug] `$selftest` local variable collided with the `-SelfTest` switch parameter**
- **Found during:** Task 3 (running the mandated self-test proof)
- **Issue:** PowerShell variables are case-insensitive; a local `$selftest = [ordered]@{ ... }` hashtable assignment inside the `if ($SelfTest)` block was actually reassigning the script's own `[switch]$SelfTest` parameter variable, which threw `Cannot convert value "System.Collections.Specialized.OrderedDictionary" to type "System.Management.Automation.SwitchParameter"`.
- **Fix:** Renamed the local variable to `$selftestResult` throughout.
- **Files modified:** `tools/capture/Invoke-ShowcaseSweep.ps1`
- **Verification:** Re-ran `-SelfTest`; script completed and printed `AERO_SELFTEST PASS`.
- **Committed in:** `45026c0` (Task 3 commit)

**4. [Rule 3 - Blocking] `param()` block was not the first statement in `Invoke-ShowcaseSweep.ps1`**
- **Found during:** Task 3 (parse-checking the script before running it)
- **Issue:** `Set-StrictMode -Version 2` was placed between `#Requires -Version 5.1` and `[CmdletBinding()] param(...)`; PowerShell requires the `param()` block to be the first statement after only comments/`#Requires`, so the parser rejected every parameter default-value expression.
- **Fix:** Moved `Set-StrictMode -Version 2` to immediately after the `param()` block closes.
- **Files modified:** `tools/capture/Invoke-ShowcaseSweep.ps1`
- **Verification:** `[System.Management.Automation.Language.Parser]::ParseFile(...)` reported zero parse errors.
- **Committed in:** `45026c0` (Task 3 commit)

**5. [Rule 1 - Bug] `-Themes`/`-Sections` comma-separated CLI values bound as one string element**
- **Found during:** Task 3 (running the mandated mini-sweep proof: `-Sections Buttons,Range`)
- **Issue:** `powershell.exe -File ... -Sections Buttons,Range`, invoked from outside a PowerShell session, bound the entire `"Buttons,Range"` string as a single array element instead of splitting it on the comma, so the launch requested a section literally named `Buttons,Range`, which the showcase correctly rejected with `AERO_SECTION_UNKNOWN`.
- **Fix:** Added a normalization step at script start that re-splits every `-Themes`/`-Sections` element on comma (a no-op for values that arrive already split).
- **Files modified:** `tools/capture/Invoke-ShowcaseSweep.ps1`
- **Verification:** Re-ran the mini sweep (`-Themes AeroDark -Sections Buttons,Range -Captures 2`); `AERO_SWEEP_DONE frames=2`, `manifest.json` lists both `AeroDark/Buttons/p0` and `AeroDark/Range/p0`.
- **Committed in:** `45026c0` (Task 3 commit)

**6. [Rule 1 - Bug] `orphansKilled` serialized as `{}` instead of `[]` when empty**
- **Found during:** Task 3 (inspecting `manifest.json` from the mini sweep)
- **Issue:** `Remove-AeroOrphanShowcaseProcesses` returns `$killed` (an array) via `return $killed`; PowerShell's pipeline unrolls an empty array to `$null` on return, so `ConvertTo-Json` rendered the manifest field as an empty object rather than an empty array.
- **Fix:** Wrapped the call site in `@( ... )` (`$orphansKilled = @(Remove-AeroOrphanShowcaseProcesses)`) to force array context regardless of pipeline unrolling.
- **Files modified:** `tools/capture/Invoke-ShowcaseSweep.ps1`
- **Verification:** Re-ran the mini sweep; `manifest.json` now shows `"orphansKilled": []`.
- **Committed in:** `45026c0` (Task 3 commit)

---

**Total deviations:** 6 auto-fixed (5 Rule 1 bugs, 1 Rule 3 blocking issue)
**Impact on plan:** All fixes were required to make the plan's own mandated proof runs (compile, self-test, mini sweep) actually pass; none changed the plan's design or scope. No scope creep.

## Issues Encountered
None beyond the auto-fixed items above — no unresolved issues.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness

- BASE-01 and BASE-02 are both proven on the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1): `AERO_SELFTEST PASS` with `occludedVerified=true`, `magentaAbsent=true`, `minimizedRestored=true`, `inputUnchanged=true`, `externalActivityEvents=0`, captured at **96 DPI** (100% scaling — 125%/200% remain out of scope per VER-F02/D-01 discretion, unchanged from the phase's known gap).
- The mini sweep (`AeroDark`, `Buttons`+`Range`, 2 captures each) produced a complete `manifest.json` with `toolchain.kotlin=2.4.10` and `toolchain.composeMultiplatform=1.11.1`, confirming the manifest-writing path end-to-end before it is relied on by later plans (04, 10, 11) for the real before/after sweeps.
- No orphan `java.exe` process with `com.mordred.showcase.MainKt` in its command line remained after either proof run.
- `git status --porcelain -- .captures` is empty after both proof runs — the gitignore rule is confirmed working, so later plans' large image sweeps will not risk being staged accidentally.
- Ready for Plan 02 (same wave, runs after this plan on the same tree) and for the later plans that consume this pipeline (BASE-04 baseline capture, BASE-05 UI-test proof, the D-04 drift sweep, and Hot Reload MCP setup once CMP reaches 1.12.0).
- No blockers identified for downstream plans.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 7 created/modified files found on disk; all 3 task commits (`00e8e65`, `8229805`, `45026c0`) found in git history.
