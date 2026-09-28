---
phase: 22-native-window-behavior-release-3-2-0
reviewed: 2026-09-28T19:32:36Z
depth: standard
files_reviewed: 49
files_reviewed_list:
  - .captures/22-dontbreak/Invoke-DontBreakCheck.ps1
  - .captures/22-dontbreak/Invoke-HotReloadCheck.ps1
  - .captures/22-gappress/client-press-probe.ps1
  - .captures/22-gappress/diagnosis.txt
  - .captures/22-gappress/press-parity-probe.ps1
  - .captures/22-gappress/proof-pack-probe.ps1
  - .captures/22-gappress/proof-pack.log
  - .captures/22-maxbutton/Invoke-MaxButtonCheck.ps1
  - .captures/22-regions/Invoke-RegionsCheck.ps1
  - gradle/libs.versions.toml
  - library/build.gradle.kts
  - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
  - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt
  - library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroMaxButtonInteraction.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestClassification.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistry.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/MinimumSize.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeChromeStatus.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Chrome.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Dwm.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Geometry.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
  - library/src/main/kotlin/com/mordred/aero/internal/windows/WndProcSupport.kt
  - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroWindowChromeStateTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/navigation/ResizeHandlesGateTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/navigation/TitleBarButtonParityTest.kt
  - library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestClassificationTest.kt
  - library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistryTest.kt
  - library/src/test/kotlin/com/mordred/aero/internal/windows/MinimumSizeTest.kt
  - library/src/test/kotlin/com/mordred/aero/internal/windows/Win32GeometryTest.kt
  - library/src/test/kotlin/com/mordred/aero/internal/windows/WndProcSupportTest.kt
  - library/src/test/kotlin/com/mordred/aero/verification/PublicApiNoJnaTest.kt
  - showcase/build.gradle.kts
  - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
  - showcase/src/main/kotlin/com/mordred/showcase/NarrowQueueWindow.kt
  - showcase/src/main/kotlin/com/mordred/showcase/WindowStateReporter.kt
  - tools/winprobe/Invoke-EarlyGate.ps1
  - tools/winprobe/Invoke-FullSession.ps1
  - tools/winprobe/Invoke-WinProbe.ps1
  - tools/winprobe/PositiveControlHost.ps1
  - tools/winprobe/PositiveControlWindow.ps1
  - tools/winprobe/RealInput.ps1
  - tools/winprobe/SessionEnv.ps1
  - tools/winprobe/Watch-SnapFlyout.ps1
  - tools/winprobe/WinProbe.ps1
findings:
  critical: 0
  warning: 5
  info: 15
  total: 20
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-09-28T19:32:36Z
**Depth:** standard
**Files Reviewed:** 49
**Status:** issues_found

## Summary

Standard-depth review of the Phase 22 native-window-behavior release: the Win32 interop layer
(`internal/windows`), the public chrome API (`AeroWindowChrome` / `AeroTitleBar` /
`AeroResizeHandles`), unit tests, showcase wiring, and the `tools/winprobe` + `.captures`
PowerShell verification harness.

Overall this is unusually disciplined code: message ownership is declared and audited
(`OWNED_FRAME_MESSAGES`), every native call site is wrapped in `dispatchSafely`, the
EDT-crossing discipline is consistent (`SwingUtilities.invokeLater` only), the
copy-on-write snapshot bridge is lock-free and tested for tearing, the JNA
implementation-scoping gate (`PublicApiNoJnaTest`) is real, and the real-input harness is
authorization-gated with dry-run proof of no-input. No security vulnerability, crash-level
bug, or data-loss risk was found in the shipped library code: there is no injection surface
(OS messages and pinned-hash installs only), no secrets (the SHA-256 values in
`SessionEnv.ps1` are integrity pins for vetted binaries), no eval/deserialization.

What the adversarial pass did surface: one genuine unit-conversion logic error on the
Compose-side resize path (px treated as dp, so an app-set AWT minimum is inflated by the
DPI scale), one robustness gap in the deferred-install path that can leave a window with
no drag path at all, one harness function that detects a wrong-monitor DPI change but
leaves the user's primary display mis-scaled, silent `SendInput` failures, and a
state-sticking edge in the NC press bridge. The fixed `B01-PRESS-FRAME` check
(reset-after-toggle, fresh geometry, size guard) was re-traced and is correct.

Severity mix: 0 critical, 5 warnings, 15 info. Convention checks (gsd-tools, scoped to the
changed files) returned zero findings.

## Warnings

### WR-01: `resolveComposeMinimumDp` treats the AWT minimum (physical px) as dp — app minimum inflated at any scale != 1.0

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/MinimumSize.kt:46-55` (called from `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt:104-106,128-130,146-148,164-166,188-190,217-219,241-243,265-267`)
**Issue:** Every drag handler in `AeroResizeHandles` passes `window.minimumSize.width/height`
(physical pixels, per AWT semantics) into `resolveComposeMinimumDp`, whose result is compared
against and assigned to `windowState.size` (dp). At 150% scale, an app that sets
`window.minimumSize = Dimension(390, 300)` (260x200 dp) gets a Compose-side clamp of 390 *dp*
(= 585 px): the user cannot resize below 585 px even though the app allows 390 px. The native
path does the conversion correctly (`resolveMinimumTrackSizePx` works in physical px at the
window DPI), so the two paths disagree at HiDPI — a direct violation of D-01 ("the same
resolution rule applies to the Compose-side handles") on every non-100% display. All recorded
verification ran at scale 1.0, so the lock tests (`MinimumSizeTest.composePathUsesTheAppMinimumWhenSet`,
which passes `260, 200` and expects `260f to 200f`) bake in the px-as-dp assumption and cannot
catch it.
**Fix:** Convert the AWT px minimum back to dp at the window's current density before clamping:

```kotlin
// ResizeHandles.kt — inside each detectDragGestures block
val density = LocalDensity.current // capture once per composition outside pointerInput
val scale = density.density
val (minW, minH) = resolveComposeMinimumDp(
    window.isMinimumSizeSet,
    window.minimumSize.width / scale,
    window.minimumSize.height / scale,
)
```

(or change `resolveComposeMinimumDp` to take px + scale and divide internally, updating
`MinimumSizeTest` with a 1.5x case that locks the conversion).

### WR-02: Deferred install path has no failure handling — a throwing install leaves the window undraggable while `isNative` reports true

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt:124-139` (consumer: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt:116-121`)
**Issue:** `acquire` wraps `installOrReuse` in try/catch (traces `install-failed`, sets
`NativeChromeStatus` false, returns null so `installFailed` flips and `AeroTitleBar` falls
back to `WindowDraggableArea`). `acquireDeferred` — taken whenever the peer is not yet
displayable, the normal first-frame case for a Compose window — has no such wrapping: if
`installOrReuse` throws inside the `HierarchyListener`, the throwable escapes into AWT's
listener dispatch, `deferredHandle` stays null (nothing to release), no `install-failed`
trace is written, and `installFailed` in `rememberAeroWindowChrome` never becomes true. The
state object then reports `isNative = true`, so `TitleBarRow` composes the native path with
no WndProc installed: `HTCAPTION` is never answered (no native drag) *and*
`WindowDraggableArea` is not composed (no Compose drag) — the window has no drag path at all.
**Fix:** Mirror `acquire`'s guard in the listener and surface the failure to the compositor:

```kotlin
private fun acquireDeferred(window: Window): ChromeHandle {
    var deferredHandle: ChromeHandle? = null
    val listener = object : HierarchyListener {
        override fun hierarchyChanged(event: HierarchyEvent) {
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && window.isShowing) {
                window.removeHierarchyListener(this)
                deferredHandle = try {
                    installOrReuse(window)
                } catch (t: Throwable) {
                    NativeChromeStatus.set(window, false)
                    chromeTrace("install-failed", 0L, "${t::class.java.name}: ${t.message}")
                    DeferredInstallFailed.set(window)   // or another channel rememberAeroWindowChrome polls
                    null
                }
            }
        }
    }
    ...
}
```

At minimum, catch-and-trace so the failure is visible; ideally flip a per-window failure
flag `rememberAeroWindowChrome` reads so the legacy drag path is restored.

### WR-03: `Set-AeroDisplayScale` detects a wrong-monitor DPI application but exits by throwing, leaving the primary display mis-scaled

**File:** `tools/winprobe/SessionEnv.ps1:818-835`
**Issue:** After `Set-DisplayScale`, the function verifies the target monitor's effective DPI
and that the primary's DPI is untouched — but on detecting that the primary was changed it
only `throw`s. The maintainer's primary display is then left at the wrong scale with no
automated restore; the code's own comment records that exactly this happened live on
2026-09-28 ("applied the 150% to the PRIMARY panel instead … caught by the safety check below
and manually restored"). The pre-change primary DPI is already in hand
(`$beforePrimary.EffectiveDpi`), so rollback is cheap and deterministic.
**Fix:** Before throwing, attempt to restore the primary:

```powershell
if ($beforePrimary -and $afterPrimary -and ($afterPrimary.EffectiveDpi -ne $beforePrimary.EffectiveDpi)) {
    # Best-effort rollback: re-target the scale change at the primary's own display id.
    $primaryDisplay = Get-DisplayInfo | Where-Object { $_.DisplayName -eq ((Get-AeroEnvMonitors | Where-Object IsPrimary | Select-Object -First 1).Name) } | Select-Object -First 1
    if ($primaryDisplay) {
        $rollbackPercent = [int](96 * $beforePrimary.EffectiveDpi / 96 / 96 * 100) # = beforePercent if recorded
        Set-DisplayScale -DisplayId $primaryDisplay.DisplayId -Scale $RecordedBeforePercent
    }
    throw "Set-AeroDisplayScale: primary monitor DPI changed $($beforePrimary.EffectiveDpi) -> $($afterPrimary.EffectiveDpi); rollback attempted"
}
```

(Record the primary's pre-run scale percent alongside `$beforePrimary` so the rollback uses
the exact prior value rather than deriving it.)

### WR-04: `SendInput` return values are discarded — a blocked injection is indistinguishable from a product failure in the recorded verdicts

**File:** `tools/winprobe/RealInput.ps1:230,257`
**Issue:** `Send-AeroRawMouseInput` and `Send-AeroRawKeyEvent` pipe `SendInput(...)` to
`Out-Null`. `SendInput` returns the number of events successfully injected; 0 means the
input was blocked (classic case: an elevated window in the foreground and a non-elevated
harness — UIPI). Every downstream check that depends on that gesture then records
FAIL/UNCONFIRMED with evidence that looks exactly like "the feature is broken", corrupting
the verification record the release relies on. Nothing in the action log or summary can
distinguish "input never delivered" from "input delivered and ignored".
**Fix:**

```powershell
$.sent = [AeroRealInput.Native]::SendInput(1, $arr, $size)
if ($sent -ne 1) {
    throw "Send-AeroRawMouseInput: SendInput injected $sent of 1 event (blocked by UIPI? elevated foreground window?)"
}
```

Throwing is correct here: the session scripts already wrap each check in try/catch, so a
blocked injection becomes an explicit ERROR on that check instead of a misleading FAIL.

### WR-05: NC press state sticks when `WM_NCLBUTTONUP` arrives with a non-HTMAXBUTTON hit while pressed

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt:260-267` (consequence at `181-197`)
**Issue:** `handleNcButtonUp` only touches `ncPressed` on the HTMAXBUTTON branch; a release
with any other hit code is forwarded to AWT while `ncPressed` (and possibly `ncHovered`)
remain true on the toolkit thread. The Compose-side `PressInteraction.Press` emitted earlier
is then never matched by a `Release`/`Cancel` until some later `WM_NCMOUSEMOVE` fires the
else-branch in `handleNcMouseMove` — and the next `WM_NCMOUSEMOVE` at HTMAXBUTTON is also
affected: `if (!ncHovered)` is false, so no fresh `hoverEnter` is emitted. Net effect: the
maximize button's pressed (and hover) visual sticks. Real-input reach is narrow but real:
press the max button, then have the window move/resize under the stationary cursor (e.g. a
Snap animation or a probe-driven `SC_MAXIMIZE`) so the release is re-classified as
`HTCAPTION`. The probes' posted-message sequences (`Invoke-MaxButtonCheck.ps1`) exercise
exactly this shape.
**Fix:** Clear the mirror whenever an up arrives while pressed, regardless of hit code:

```kotlin
private fun handleNcButtonUp(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): Long {
    if (wParam.toInt() != HTMAXBUTTON) {
        if (ncPressed) {
            ncPressed = false
            hop { maxButton.cancelPress() }
        }
        return callPrevious(hwnd, uMsg, wParam, lParam).toLong()
    }
    ...
}
```

## Info

### IN-01: `syncChildren` accumulates duplicate `InstalledChild` entries for one live child HWND

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt:254-277,324-339`
**Issue:** After an eviction, re-subclassing adds a *second* `InstalledChild` with the same
`childHwnd`; stale-removal only drops entries whose HWND no longer exists, so duplicates
persist until release. Release still restores correctly (the last matching entry wins, the
evicted one is dead), but `event=verify ... children=N` counts inflate per eviction cycle
and the list grows unboundedly under repeated churn.
**Fix:** Before `subclassChild`, remove any existing entries with the same `childHwndLong`
whose `procPtr` no longer matches the current proc (they are unreachable by definition).

### IN-02: Two concurrent `rememberAeroWindowChrome` compositions on one window break each other's maximize click

**File:** `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt:122-131`
**Issue:** `maxInteraction.onClick` is a single per-window slot (`MaxButtonDirectory`
singleton). A second chrome on the same window overwrites it; the *first* one's
`onDispose { maxInteraction.onClick = null }` then disarms the survivor, so a native max
click stops toggling placement. Today only one chrome per window is expected, but nothing
documents or guards the constraint.
**Fix:** Either document "one chrome per window" in `rememberAeroWindowChrome`'s KDoc, or
ref-count the click handler (e.g. a small listener list on `AeroMaxButtonInteraction`).

### IN-03: `handleSysCommand` swallows SC_KEYMENU when `GetSystemMenu` returns null

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt:292-309`
**Issue:** If `GetSystemMenu(hwnd, false)` returns null, the MENU branch returns 0 having
shown nothing and forwarded nothing — Alt+Space becomes silently dead instead of degrading.
Essentially unreachable for a live `WS_SYSMENU` window, but the branch is trivially
fail-safe-able.
**Fix:** `if (menu == null) return callPrevious(hwnd, uMsg, wParam, lParam).toLong()`.

### IN-04: `Invoke-RegionsCheck.ps1` re-declares `ShowWindow` that `WinProbe.ps1` already ships

**File:** `.captures/22-regions/Invoke-RegionsCheck.ps1:28-33` (vs `tools/winprobe/WinProbe.ps1:203-206`)
**Issue:** A local `AeroRegionsProbe.Native` Add-Type declares `ShowWindow`, while the
dot-sourced `AeroWinProbe.Native` already declares `ShowWindow` and `SW_SHOWNOACTIVATE = 4`.
Duplication of the exact P/Invoke the shared library owns.
**Fix:** Delete the local Add-Type block and call
`[AeroWinProbe.Native]::ShowWindow($hwnd, [AeroWinProbe.Native]::SW_SHOWNOACTIVATE)`.

### IN-05: `Invoke-SessionCheck` normalization throws on a `$null` array element — would abort the entire pass outside per-check isolation

**File:** `tools/winprobe/Invoke-FullSession.ps1:475-479`
**Issue:** `$o.PSObject.Properties['Id']` on a `$null` element throws under
`Set-StrictMode -Version 2` (verified live: `PropertyNotFoundException`), and this loop runs
*after* the body's try/catch — the throw escapes `Invoke-SessionCheck` into the per-pass
handler, killing every remaining check of that pass. Today's only array-returning body
(`C02-OPTOUT-ALTSPACE`) initializes both elements to `$null` and assigns both on every
non-throwing path, so the trap is not currently sprung — but any future compound check that
can return a null element turns a single-check bug into a whole-pass loss.
**Fix:** `foreach ($o in @($outcomes)) { if ($null -eq $o) { continue } ... }` or coerce
nulls to `UNCONFIRMED` entries the way the `$null -eq $outcomes` branch already does.

### IN-06: Dead conditional in the session summary

**File:** `tools/winprobe/Invoke-FullSession.ps1:2471`
**Issue:** `AuthorizedBy = $(if ($DryRun) { $session.AuthorizedBy } else { $session.AuthorizedBy })`
— both branches identical.
**Fix:** `AuthorizedBy = $session.AuthorizedBy`.

### IN-07: Hot-reload probe's source restore can change bytes (BOM/encoding) and a hard kill leaves the tree edited

**File:** `.captures/22-dontbreak/Invoke-HotReloadCheck.ps1:30,98,123,137`
**Issue:** `[System.IO.File]::ReadAllText` + `WriteAllText` round-trips
`AeroTitleBar.kt` as UTF-8 without BOM; if the source ever carries a BOM (or a different
encoding), the "restored" file differs byte-wise and the caller's `git diff --quiet --
library/` gate fails spuriously. Also, a hard process kill between edit and `finally` leaves
the modified source on disk.
**Fix:** Capture and re-apply the raw bytes (`[System.IO.File]::ReadAllBytes` /
`WriteAllBytes`) instead of text round-trip; that removes both the encoding risk and any
newline-normalization risk.

### IN-08: `proof-pack-probe.ps1` never exits non-zero on FAIL counts

**File:** `.captures/22-gappress/proof-pack-probe.ps1:35-38`
**Issue:** Unlike every sibling (`Invoke-EarlyGate`, `Invoke-FullSession`, `Invoke-WinProbe
-AssertV11`), the proof pack prints `failCount` to the log but always exits 0 — a CI/agent
invocation must scrape `proof-pack.log` to learn the verdict.
**Fix:** `if ($failCount -gt 0) { exit 1 } else { exit 0 }` after writing the summary lines.

### IN-09: Dead locals in `Invoke-WinProbeMaximize`

**File:** `tools/winprobe/WinProbe.ps1:748,785`
**Issue:** `$result` and `$result2` capture the `SendMessageTimeoutW` return values and are
never read.
**Fix:** Pipe to `Out-Null` (matching the file's own style) or check the returns.

### IN-10: `AeroChildWndProc.childHwnd` is never read

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt:459-464`
**Issue:** The constructor-valued `childHwnd` is unused inside the class
(`InstalledChild` carries its own copy for the registry side).
**Fix:** Drop the property (keep the constructor parameter out entirely) or use it in the
subclass trace for symmetry.

### IN-11: Declared-but-mainline-unused Win32 constants

**File:** `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt:43-46,52-55`
**Issue:** `SC_CLOSE`, `SC_MAXIMIZE`, `SC_MINIMIZE`, `SC_RESTORE` are referenced only from
`WndProcSupportTest`; `TPM_LEFTALIGN`/`TPM_TOPALIGN` are `0x0000` no-ops ORed into the
`TrackPopupMenu` flags (documented as deliberate readability idiom). Not harmful, but the
SC_* quartet reads as production API when it is test-only surface.
**Fix:** Either note "test-use anchors" in the comment block or move the quartet's usage
assertions to reference the constants already needed.

### IN-12: `ForegroundTaken` actually means "foreground owned at any sampled moment"

**File:** `.captures/22-dontbreak/Invoke-DontBreakCheck.ps1:152` and `.captures/22-maxbutton/Invoke-MaxButtonCheck.ps1:132-134`
**Issue:** The flag is computed as `(before -eq pid) -or (after -eq pid)` — i.e. true also
when the showcase *already* owned the foreground and never took it. Conservative for an
interference detector (fails loudly), but the JSON key name invites misreading as
"activation happened here".
**Fix:** Rename to `ForegroundOwned` or record both `ownedBefore`/`ownedAfter` and compute
`taken = after -eq pid -and before -ne pid` alongside.

### IN-13: Inconsistent showcase-process matching scopes across the harness

**File:** `tools/winprobe/Invoke-FullSession.ps1:390-393` vs `.captures/22-gappress/press-parity-probe.ps1:35-41`
**Issue:** `Get-SessionMainKtPids` and `Remove-AeroOrphanShowcaseProcesses` filter
`Name='java.exe'`, while the gappress probes use `Name like 'java%'` (catching `javaw.exe`).
A `javaw.exe`-launched showcase would be invisible to PID tracking (leaking a tracked-JVM
kill) yet also skipped by the orphan sweep, depending on which matcher ran.
**Fix:** Pick one matcher (`Name like 'java%'` is the safer superset for tracking; the
targeted sweep in `Invoke-FullSession` is already PID-exact) and use it in both places.

### IN-14: Silent catch blocks in session teardown swallow diagnostics

**File:** `tools/winprobe/Invoke-FullSession.ps1:285,291,2351,2442`
**Issue:** `catch { }` around `Reset-SessionWindows` per-window resets,
`Stop-AeroShellEventWatch`, and `Restore-AeroCursor` discard the exception text entirely.
Best-effort cleanup is the right semantics, but a failed cursor restore or a watcher thread
that never joins leaves no trace anywhere (the cursor after-position is recorded, the reason
is not).
**Fix:** `catch { Write-Host "SESSION CLEANUP WARN: $($_.Exception.Message)" }` — keeps the
best-effort flow, keeps the evidence.

### IN-15: Test-count guard depends on a Gradle internal API via an unchecked cast

**File:** `library/build.gradle.kts:85-88`
**Issue:** `(filter as? org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter)?.commandLineIncludePatterns`
silently returns null when the internal class moves/renames on a Gradle upgrade; `filtered`
then reads false for a `--tests` run, and the count guard throws a false
"executed total != 596" failure for a legitimately filtered run. Fails loudly rather than
silently, and the workaround is well commented, but the failure mode is a confusing
false positive exactly when tooling changes underneath.
**Fix:** Also treat a small executed count with `filter.hasTests() == true` (public API on
this Gradle line) as filtered, or catch the guard's `GradleException` and re-check
`filter.hasTests()` before failing.

---

_Reviewed: 2026-09-28T19:32:36Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
