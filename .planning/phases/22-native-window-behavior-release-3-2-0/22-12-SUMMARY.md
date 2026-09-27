---
phase: 22-native-window-behavior-release-3-2-0
plan: 12
subsystem: windows-native
tags: [api-01, api-02, api-03, win-06, shw-17, btn-02, ver-11, opt-out, interactive-marker, multi-window]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-07 live-region pipeline (publishInteractive, LEADING_INTERACTIVE_ID); 22-08 narrow fixture + V11-N checks with the standing MARKED-BOUNDARY RED; 22-11 full native V11 GREEN on both windows"
provides:
  - "AeroTitleBar trailing defaulted nativeWindowManagement: Boolean = true opt-out (false = exact legacy branch, no install, NativeChromeStatus stays false) — API-03"
  - "public Modifier.markAeroTitleBarInteractive() in AeroWindowChrome.kt: publishes live boundsInWindow into the window's interactive-region registry (HTCLIENT), removed on dispose, inert off Windows / without a subclass — API-02"
  - "Showcase: -Paero.nativeChrome=false permanent RED control (forwarded in run + hotRun), narrow 'Вернуть' overlay marked interactive"
  - "Registry: event=ncdestroy trace on the WM_NCDESTROY entry drop (window teardown observability)"
  - "Live proof: API-02 RED->GREEN (V11-N-HT-MARKED-BOUNDARY PASS), RED control 0/18 + 0/8 with pre-phase GWL_STYLE, WIN-06 close-independence and delayed-second-window install"
affects: [22-13, 22-15, 22-16, 22-17, 22-19]

tech-stack:
  added: []
  patterns:
    - "Per-window opt-out via a trailing defaulted Boolean parameter; the flag composes the exact legacy branch, so the opt-out IS the pre-phase behavior and doubles as the VER-11 RED control"
    - "Consumer-facing marker modifier backed by composed{} + DisposableEffect over the window-keyed interactive-region registry (one id per usage, null-rect publish on dispose)"
    - "Probe invocation from bash: never comma-separated -GradleProps through powershell.exe -File (collapses to one string); a wrapper .ps1 binding the array literally is the reliable shape"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-12-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
    - showcase/src/main/kotlin/com/mordred/showcase/NarrowQueueWindow.kt
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
    - showcase/build.gradle.kts
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "markAeroTitleBarInteractive() implemented with composed{} (plan-sanctioned) matching AeroTitleBar's own onGloballyPositioned + DisposableEffect publishing pattern; one registry id per usage, removed on dispose"
  - "WM_NCDESTROY teardown path now traces event=ncdestroy: a posted WM_CLOSE destroys the HWND before onDispose runs release(), and the close path was otherwise invisible"
  - "API-01/API-02/API-03/WIN-06/SHW-17 marked complete; BTN-02 stays Pending — its ordinary-click delivery clause is Plan 15's real-input session (the hit-test half is GREEN)"

requirements-completed: [API-01, API-02, API-03, WIN-06, SHW-17]

duration: ~40min
completed: 2026-09-27
---

# Phase 22 Plan 12: Public API surface — opt-out, interactive marker, multi-window — Summary

**The public surface grew exactly additively — a per-window `nativeWindowManagement = false` opt-out and a `Modifier.markAeroTitleBarInteractive()` marker — proven source-compatible (showcase call sites byte-unchanged at compile), with the narrow window's marked overlay turning its standing RED check GREEN and the opt-out launch reproducing the pre-phase style + all-FAIL V11 as the permanent RED control.**

## Performance

- **Duration:** ~40min (started 2026-09-27T16:36:07Z)
- **Tasks:** 3/3 + 1 Rule-2 fix commit
- **Files:** 7 tracked (1 library file created, 2 library modified, 3 showcase modified, 1 NOTES) + git-ignored probe scripts and evidence under `.captures/22-publicapi/`
- `./gradlew :library:test --rerun` — AERO_TEST_COUNT total=541 green after Task 1 and after the fix commit; `:library:compileKotlin :showcase:compileKotlin` green
- Evidence: `.captures/22-publicapi/green.json`, `multiwindow.json`, `red-control.json` (+ console logs), 22-NOTES.md "Public API and multi-window (22-12)"

## Accomplishments

- **API-01:** `nativeWindowManagement: Boolean = true` appended as the last `AeroTitleBar` parameter; `:showcase:compileKotlin` passed with showcase call sites byte-unchanged at `d85bc86` (`git diff --quiet HEAD -- showcase/`); `com.sun.jna` appears only under `internal/windows/` with no top-level `public` there; explicitApi/Dokka kept clean with a one-line `@param` stub.
- **API-02:** new `library/.../navigation/AeroWindowChrome.kt` with `public fun Modifier.markAeroTitleBarInteractive(): Modifier` — publishes the element's live `boundsInWindow(clipBounds = true)` into `WindowRegionsDirectory.forWindow(window)` via `onGloballyPositioned`, removes it on dispose, returns the receiver unchanged off Windows / outside a window composition. The narrow window's "Вернуть" overlay is marked; `V11-N-HT-MARKED-BOUNDARY` went FAIL (`marked=2,captionLeftOfMarked=2`, 22-08/22-11) → PASS (`marked=1,captionLeftOfMarked=2`).
- **API-03:** `nativeRequested = isWindowsOs && nativeWindowManagement`; false composes exactly the legacy `WindowDraggableArea` branch and installs nothing. Showcase control: `-Paero.nativeChrome=false` (forwarded in both JavaExec and ComposeHotRun blocks) → `RED OK`, main 0/18 and narrow 0/8, GWL_STYLE = `0x960B0000` (the pre-phase C1 value), zero `AERO_CHROME` lines.
- **WIN-06 / SHW-17:** posted `WM_CLOSE` to the narrow window → `AERO_EVENT name=close-request label=narrow`, trace `event=ncdestroy hwnd=0x50482 entry dropped` for the narrow HWND only, main install count stayed 1, main title-bar V11 checks 5/5 PASS after the close; `-Paero.secondWindow=3000` delayed window appeared ~3.2 s after the main one with its own install + child-subclass.
- **BTN-02 proxy:** posted `WM_LBUTTONDOWN/UP` at the narrow window's `marked` and `leading` points did not reach Compose within 3 s (identical to 22-07's `min`-point outcome) — recorded, not faked; the real-click proof stays with Plan 15, the HTCLIENT half is GREEN.

## Task Commits

1. **Task 1: nativeWindowManagement opt-out and markAeroTitleBarInteractive (API-01..03)** - `6fb9921` (feat)
2. **Task 2: showcase opt-out launch property and marked title-bar element** - `0a1e64a` (feat)
3. **Fix: trace registry entry drop on WM_NCDESTROY (WIN-06 observability)** - `44b91ae` (fix)
4. **Task 3: live checks — API-02 red-green, opt-out RED control, multi-window** - `ecab3fa` (docs)

## Decisions Made

- `markAeroTitleBarInteractive()` uses `composed {}` (explicitly acceptable per plan) with `remember(window)` + `DisposableEffect(regions, id)` — the same publishing pattern `AeroTitleBar` itself uses for its regions; `Modifier.Node` offered no functional gain here.
- The opt-out is per-call-site (each `AeroTitleBar` takes its own flag); the showcase drives both windows from one launch property so the RED control covers both.
- Requirements: API-01 (compile-unchanged + additive-only + JNA confinement + legacy-branch identity), API-02 (RED→GREEN per the plan's designated proof), API-03 (opt-out reproduces pre-phase behavior, RED control), WIN-06 (independent installs, close-independence, delayed open), SHW-17 (fixture + marked element live) marked complete. BTN-02 stays Pending: the "ordinary clicks" delivery clause belongs to Plan 15's real-input session (22-07 precedent).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical observability] WM_NCDESTROY entry drop left no trace**
- **Found during:** Task 3 (multi-window close check)
- **Issue:** the plan's close check expects a trace line (`ncdestroy`/`uninstall`) for the closing window; a posted WM_CLOSE destroys the HWND before the composable's `onDispose` runs `release()`, so the registry dropped the entry silently and the first run showed `narrowUninstall=0` despite correct teardown
- **Fix:** `NativeWindowChromeRegistry.onDestroyed` now traces `event=ncdestroy ... entry dropped` when it drops a live entry
- **Files modified:** library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
- **Verification:** 541 suite green; re-run shows `event=ncdestroy hwnd=0x50482` for the narrow window only, main untouched
- **Committed in:** `44b91ae`

**2. [Rule 3 - Blocking tooling] `-GradleProps` array collapsed through powershell.exe -File**
- **Found during:** Task 3 (RED control)
- **Issue:** two RED-control attempts measured an all-PASS native window with zero chrome-trace lines: bash strips the inner quotes of `"-Pa","-Pb"`, PowerShell argument-mode binding then sees one string, and gradlew received `-Paero.nativeChrome=false,-Paero.chromeTrace=true` — the opt-out never engaged (property value ≠ "false")
- **Fix:** wrapper `.captures/22-publicapi/Invoke-RedControl.ps1` binds the array literally; pitfall recorded in 22-NOTES.md
- **Verification:** opt-out isolated first via `Test-OptOut.ps1` (manual launch: JVM received all four `-D` args, legacy style `0x960B0000`, HTCLIENT caption), then the wrapper run produced `RED OK`
- **Committed in:** `ecab3fa` (probe scripts are git-ignored capture artifacts)

**Total deviations:** 2 auto-fixed (1 Rule 2, 1 Rule 3)
**Impact on plan:** both necessary for the plan's own acceptance criteria (trace evidence; RED control). No scope creep.

## Assumption Drift (advisory)

- The plan's RED-control expectation implicitly assumed the narrow window's `V11-N-MINSIZE` would fail on the legacy path; it does (`observed=50x50`) — Windows does NOT clamp `SetWindowPos` to the app's AWT minimum on the legacy style, so the honored-app-minimum behavior (22-08/22-11) is itself a trait of the native-chrome path. This strengthens the RED control rather than weakening it.
- `marked=1,captionLeftOfMarked=1` on the opt-out run: without a subclass the overlay point is plain client area, so the boundary check's caption half also fails — the RED control fails the check for the opposite reason than the GREEN run passes it, which is exactly what a control should do.

## Issues Encountered

- Two invalid RED-control runs before the quoting root cause was found (see Deviation 2); diagnosed by a fully instrumented manual launch (`Test-OptOut.ps1`) proving the library code correct and isolating the invocation shape as the failure. No library change resulted.

## Threat Model Coverage

- **T-22-26 (API leakage):** mitigated — grep gate green (`com.sun.jna` only under `internal/windows/`, no top-level `public` there); explicitApi() enforced; the new public declarations use Compose/AWT types only.
- **T-22-27 (stale registry):** mitigated — marker ids are allocated per usage and removed on dispose (`publishInteractive(id, null)`); the registry is window-keyed in a WeakHashMap; inert when no subclass is installed (RED control shows the marker has no effect there).
- **T-22-05b (opt-out regressions):** mitigated — the opt-out composes the exact legacy branch; the RED control proves pre-phase answers (0/18 + 0/8) and the pre-phase GWL_STYLE `0x960B0000`.

## Known Stubs

None — the marker, opt-out and trace are all wired end to end and proven live.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 13 (pixel parity) / Plan 15 (real input): the marked/leading elements' real-click delivery (BTN-02) and the max-button hover/press frames are the remaining proof obligations; the hit-test halves are all GREEN.
- Plan 19 (per D-05): `rememberAeroWindowChrome()` builds on this plan's opt-out + interactive-region path; `AeroWindowChrome.kt` is its home file.
- Every future probe invocation from bash should use a wrapper `.ps1` for multi-value `-GradleProps` (22-NOTES pitfall).

---

*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED

- Created files exist (AeroWindowChrome.kt, SUMMARY, green/multiwindow/red-control JSON); task commits 6fb9921 / 0a1e64a / 44b91ae / ecab3fa present in git log; no showcase JVM left running.
