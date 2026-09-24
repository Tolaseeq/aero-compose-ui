---
phase: 21-migration-release-3-1-0
reviewed: 2026-09-24T00:00:00Z
depth: standard
files_reviewed: 22
files_reviewed_list:
  - .gitignore
  - .mcp.json
  - build.gradle.kts
  - settings.gradle.kts
  - jitpack.yml
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.properties
  - library/build.gradle.kts
  - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt
  - library/src/test/kotlin/com/mordred/aero/capture/Base05CaptureProofTest.kt
  - library/src/test/kotlin/com/mordred/aero/capture/Base05DragCaptureTest.kt
  - library/src/test/kotlin/com/mordred/aero/capture/Base05GlassStateCaptureTest.kt
  - library/src/test/kotlin/com/mordred/aero/capture/D07MenuPopupCaptureTest.kt
  - library/src/test/kotlin/com/mordred/aero/capture/D07PickerPopupCaptureTest.kt
  - library/src/test/kotlin/com/mordred/aero/capture/UiCapture.kt
  - showcase/build.gradle.kts
  - showcase/src/main/kotlin/com/mordred/showcase/Main.kt
  - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
  - tools/capture/AeroCapture.ps1
  - tools/capture/Compare-AeroCaptures.ps1
  - tools/capture/Invoke-ShowcaseSweep.ps1
  - tools/capture/New-HandoffPage.ps1
  - tools/capture/Test-NonInterference.ps1
  - tools/verify/check-material3.sh
findings:
  critical: 0
  warning: 2
  info: 2
  total: 4
status: issues_found
---

# Phase 21: Code Review Report

**Reviewed:** 2026-09-24T00:00:00Z
**Depth:** standard
**Files Reviewed:** 22
**Status:** issues_found

## Summary

Reviewed the phase-21 toolchain migration (Gradle 9.7.1 / JDK 21 / Kotlin 2.4.20 / Compose
Multiplatform 1.12.0 / kotlinx-datetime 0.8.0 / JUnit 6.1.3), the new Compose Hot Reload wiring in
`:showcase`, and the new PrintWindow-based capture/compare PowerShell toolchain, against
`git diff 051a64f^..HEAD` for each listed file — not the pre-existing code around those diffs.

The four picker files (`AeroDatePicker.kt`, `AeroDateRangePicker.kt`, `AeroDateTimePicker.kt`,
`AeroDateTimeRangePicker.kt`) each changed by exactly one line — `kotlinx.datetime.Clock` ->
`kotlin.time.Clock` — consistent with the kotlinx-datetime 0.8.0 migration; no issues found there.
The Gradle/version-catalog/wrapper/jitpack changes are mechanical version bumps with no logic to
review. The two maintainer-confirmed intentional behaviors (capture-guard fails only on the
captured app's own process taking foreground; `.mcp.json` running `:showcase:hotMcpServer` via
`cmd /c .\gradlew.bat`) were verified present and are not reported as defects.

Two WARNING-level defects were found in the newly added tooling: a scoping bug in
`Invoke-ShowcaseSweep.ps1`'s page-discovery loop that silently records an unrequested page-0 frame
in the manifest when `-Pages` excludes page 0, and an overly broad `catch (e: Throwable)` in a new
capture test that can mask real crashes (`Error` subtypes) as a benign "fallback unavailable"
outcome. Two INFO-level maintainability notes are also included. No Critical/security issues were
found in the reviewed diff.

## Warnings

### WR-01: `-Pages` filter does not prevent an unrequested page-0 frame from being recorded

**File:** `tools/capture/Invoke-ShowcaseSweep.ps1:370-417`

**Issue:** The per-`(theme, section)` loop always starts with `$pagesToCapture = @(0)` and launches
page 0 first to discover `$totalPages` from the `AERO_READY` line. Once the real page list is known
(`$pagesToCapture`, built from `-Pages` when supplied), the loop correctly resets `$i = -1` to visit
every requested page — but the discovery iteration's own `[void]$frames.Add(...)` call at the
bottom of the loop body (lines 398-414) executes unconditionally, using the still-live `$page = 0`
from the top of that iteration. When the caller passes `-Pages` and 0 is *not* one of the requested
pages (e.g. `-Pages 2,3`), the manifest still gains a `.../p0` entry — and its capture files stay on
disk — that was never requested and is outside the filtered scope the caller asked for. This
silently pollutes `manifest.json` and the capture directory tree with an extra, unscoped frame,
which will also throw off any downstream tool (e.g. `New-HandoffPage.ps1`'s "complete run"
detection) that assumes the manifest matches the requested `-Pages` set exactly.

**Fix:** Track whether page 0 is actually present in the resolved `$pagesToCapture` before adding
the discovery frame, e.g.:
```powershell
$discoveryFrame = [pscustomobject]@{ key = "$theme/$section/p$page"; ... }

if (-not $discoveredPages) {
    ... # existing discovery logic that resolves $pagesToCapture
    $discoveredPages = $true
    if ($pagesToCapture[0] -ne 0) {
        $i = -1
        if (0 -notin $pagesToCapture) {
            # discovery page wasn't actually requested — skip recording it
            continue
        }
    }
}

[void]$frames.Add($discoveryFrame)
```
(or equivalently, skip the `Add` call outright when `$page -eq 0` and `0 -notin $pagesToCapture`).

### WR-02: Overly broad `catch (Throwable)` in the focus-fallback helper can mask real test crashes

**File:** `library/src/test/kotlin/com/mordred/aero/capture/Base05GlassStateCaptureTest.kt:565-572`

**Issue:** `focusViaTabThenFallback`'s fallback path is:
```kotlin
try {
    explicitFocus()
    waitForIdle()
    img = capture(component, theme, "focus")
    method = "requestFocus (Tab traversal produced no visible change)"
} catch (e: Throwable) {
    method = "tab-traversal (explicit requestFocus fallback unavailable on this node)"
}
```
Catching `Throwable` (rather than a narrower type such as `IllegalStateException` or
`AssertionError`) swallows every kind of failure from `explicitFocus()`/`waitForIdle()`/`capture()`,
including `Error` subtypes (`OutOfMemoryError`, `StackOverflowError`, Compose internal assertion
errors). A genuine crash inside the fallback path is silently reinterpreted as "fallback unavailable
on this node" and the test proceeds to assert on `img`/`defaultImg`, which can produce a misleading
pass/fail result instead of surfacing the real failure. This directly affects the reliability of a
permanent regression-guard test (BASE-05), which is in scope for review despite being a test file.

**Fix:** Narrow the catch to the actual exception types `requestFocus()`/related Compose test APIs
are documented to throw (e.g. `AssertionError`, `IllegalStateException`), and let anything else
propagate:
```kotlin
} catch (e: AssertionError) {
    method = "tab-traversal (explicit requestFocus fallback unavailable on this node)"
}
```

## Info

### IN-01: Duplicated `aero.*` system-property forwarding block in `showcase/build.gradle.kts`

**File:** `showcase/build.gradle.kts:24-48`

**Issue:** The four `(project.findProperty("aero.*") as String?)?.let { systemProperty(...) }`
lines are duplicated verbatim between the `tasks.withType<JavaExec>().configureEach { if (name ==
"run") ... }` block and the new `tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>()
.configureEach { ... }` block added for Compose Hot Reload. The accompanying comment explains *why*
two blocks are needed (different task types), but the forwarding logic itself could still be
factored into one shared local function to avoid the two copies drifting out of sync the next time
an `aero.*` launch parameter is added.

**Fix:**
```kotlin
fun JavaExec.forwardAeroLaunchProps() {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
    (project.findProperty("aero.page") as String?)?.let { systemProperty("aero.page", it) }
    (project.findProperty("aero.capture") as String?)?.let { systemProperty("aero.capture", it) }
}

tasks.withType<JavaExec>().configureEach { if (name == "run") forwardAeroLaunchProps() }
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach { forwardAeroLaunchProps() }
```

### IN-02: `tasks.test` guard depends on an internal Gradle API class

**File:** `library/build.gradle.kts:73-80`

**Issue:** The `TOOL-16` test-count guard casts `filter` to
`org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter` (an `org.gradle.api.internal.*`
type, not part of Gradle's public API) to detect `--tests` filtering. The comment explains why the
public `TestFilter` interface is insufficient on this Gradle version, but referencing an internal
class is a compile-time dependency: a future Gradle upgrade that renames/removes/relocates
`DefaultTestFilter` will break the build script's compilation, not just its runtime behavior, with
no warning before that upgrade. Worth a follow-up note (e.g. in `21-TOOLCHAIN-LOG.md`) so the next
Gradle bump knows to re-verify this cast still resolves.

**Fix:** No change required now; flagging for awareness on the next Gradle version bump. If it
becomes a recurring pain point, consider dropping the filtered-run distinction and instead gating
the guard on an explicit opt-out Gradle property (e.g. `-Paero.skipTestCountGuard=true`) supplied by
whoever intentionally runs a filtered subset.

---

_Reviewed: 2026-09-24T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
