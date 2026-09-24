# Phase 21: Migration + Release 3.1.0 - Pattern Map

**Mapped:** 2026-09-23
**Files analyzed:** 28 (10 version/build config, 2 showcase app-entry, 10 kotlinx-datetime mechanical-rename, ~6 new-test/tooling groups)
**Analogs found:** 22 / 28 (6 groups have no in-repo analog — external WinAPI script, `.mcp.json`, TOOL-16 Gradle listener, D-06 HTML page, D-07 popup-capture-inside-`runComposeUiTest` — flagged below, use RESEARCH.md's sketches/MCP-HOWTO.md instead)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `gradle/libs.versions.toml` | config | batch/transform | itself (current state, §below) | exact (edit-in-place) |
| `gradle/wrapper/gradle-wrapper.properties` | config | batch | itself | exact |
| `settings.gradle.kts` | config | batch | itself | exact |
| `build.gradle.kts` (root) | config | batch | itself | exact |
| `library/build.gradle.kts` | config | batch + event-driven (TOOL-16 test listener, D-03 opt-in property) | itself + Code Examples sketch in 21-RESEARCH.md | role-match (guard task is new) |
| `showcase/build.gradle.kts` | config | request-response (Gradle property → JVM system property) | itself, lines 23-31 (`-Paero.scheme` block) | exact (extend with sibling block) |
| `jitpack.yml` | config | batch | itself | exact |
| `README.md` | config/docs | transform | itself, line 6 | exact |
| `.gitignore` | config | batch | itself | exact |
| `.mcp.json` (new) | config | request-response (stdio JSON-RPC spawn) | none in-repo | no analog — use MCP-HOWTO.md's verbatim JSON block |
| `showcase/.../Main.kt` | provider/entry-point | request-response (system property → initial state) | itself, `initialScheme()` lines 29-33 | exact |
| `showcase/.../ShowcaseApp.kt` | component | transform (render tree) | itself, lines 50-129 | exact |
| `library/src/test/.../Base05ProofOfWorkTest.kt` (new) | test | streaming (UI capture) | `VER07ButtonStateMatrixTest.kt` | exact |
| `library/src/test/.../popup/*PopupOpenedStateTest.kt` (new, ~13 components × 3 themes, D-07) | test | streaming (UI capture, before/after) | `AeroThemeBackgroundEstablishmentTest.kt` (onRoot capture) + `VER07ButtonStateMatrixTest.kt` (pixelMapsDiffer) | role-match (Popup-root capture is untested territory, Pitfall 5) |
| Drag-based UI tests (AeroSplitPane/AeroDataTable column resize, if in BASE-05 scope) | test | event-driven (programmatic drag) | `AeroPanelGroupRecomposeUiTest.kt` | exact |
| Animation-stepping UI tests (BASE-03 noise/animation states, if in BASE-05 scope) | test | streaming (frame-by-frame) | `VER08SegmentLabelFlipTest.kt` | exact |
| TOOL-16 count-guard task (in `library/build.gradle.kts`) | build-config task | event-driven (`Test.afterSuite` listener) | none in-repo | no analog — use RESEARCH.md Code Examples sketch |
| BASE-02 `PrintWindow` capture helper (PowerShell + C# `Add-Type`, outside Gradle) | utility (external tooling) | file-I/O | v3.0's `CopyFromScreen` PowerShell recipe (superseded method, same shape) | partial — method changes (`PrintWindow` not `CopyFromScreen`), script structure/idiom carries over |
| D-06 hand-off HTML page (generator + template, `.captures/handoff/index.html`) | utility | file-I/O | v3.0 `baseline/README.md` + `after/DIFF-REPORT.md` (text-report shape, not HTML) | partial — report *content* shape reusable, output format (HTML vs Markdown) is new |
| D-04/D-05 drift stop-list report (Markdown, phase directory) | report/doc | transform | `after/DIFF-REPORT.md` (v3.0) | exact (format precedent) |
| BASE-04 baseline capture record (Markdown, phase directory) | report/doc | transform | `baseline/README.md` (v3.0) | exact (format precedent) |
| 4× `AeroDate*Picker.kt` `Clock` import rename | component | transform (mechanical rename) | itself | exact (edit-in-place) |
| `AeroCalendarGrid.kt` (`.monthNumber` ×2) | component | transform | itself | exact |
| `AeroDatePicker.kt` / `AeroDateTimePicker.kt` (`.dayOfMonth`/`.monthNumber`) | component | transform | itself | exact |
| `AeroCalendarGridTest.kt` (`.dayOfMonth`) | test | transform | itself | exact |
| `showcase/.../sections/DataSection.kt`, `PickersSection.kt` (×6 call sites) | component | transform | itself | exact |

## Pattern Assignments

### BASE-05 hover/press/focus/drag UI capture test (new file, test, streaming)

**Analog:** `library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt`

**Imports pattern** (lines 1-28):
```kotlin
package com.mordred.aero.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
```

**Theme-wrapping pattern** (lines 168-195) — every state-matrix test wraps content in `AeroTheme(colorScheme = scheme)`, one test method per scheme (all 3: AeroBlue/AeroDark/Classic), with a `testTag("capture")` container sized generously around the target component (glow/bloom paints beyond layout bounds) and a `testTag("btn")` on the component itself:
```kotlin
) = runComposeUiTest {
    val enabledState = mutableStateOf(true)
    setContent {
        AeroTheme(colorScheme = scheme) {
            Column(modifier = Modifier.testTag("capture").size(260.dp, 80.dp)) {
                AeroButton(text = "Label", onClick = {}, modifier = Modifier.testTag("btn"), enabled = enabledState.value)
                Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
            }
        }
    }
    waitForIdle()
```

**Core capture pattern** (lines 198-256) — real input only, never a hand-built `InteractionSource`: `requestFocus()` first (before hover/press so `pointerAcquired` stays false), move focus to a sibling `other` node, then `performMouseInput { moveTo(center) }` (hover), `press()`/`release()`/`exit()` (press), disabled last via a `mutableStateOf` flip. Each state captured via `onNodeWithTag("capture").captureToImage().toPixelMap()` and compared to the default capture with `pixelMapsDiffer`.

**Comparator to reuse verbatim** (lines 265-273):
```kotlin
internal fun pixelMapsDiffer(a: PixelMap, b: PixelMap): Boolean {
    if (a.width != b.width || a.height != b.height) return true
    for (y in 0 until a.height) {
        for (x in 0 until a.width) {
            if (a[x, y] != b[x, y]) return true
        }
    }
    return false
}
```

**D-08 falsifiability pattern** (lines 87-124) — every gate in this file proves its own comparator can both detect a real difference (two different-color captures) and correctly report "no difference" (two identical captures) BEFORE using it for real assertions. New BASE-05/D-07 tests must include the same pair of fixture proofs.

---

### D-07 popup-opened-state UI tests (new files, test, streaming, before/after two-toolchain)

**Primary analog for the capture mechanism:** `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt`

**Key pattern** (lines 41-72) — `onRoot().captureToImage()` (not a tagged node) is the correct call when the defect/content being verified is about what paints OUTSIDE a specific node's own bounds — exactly the D-07 situation (an open `Popup` is a second semantics "owner" outside the trigger component's bounds):
```kotlin
@Test
fun defaultEstablishBackgroundPaintsThemeBackgroundBehindBareContent() = runComposeUiTest {
    setContent {
        AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
            BareUnfillingContent()
        }
    }
    waitForIdle()
    val pixels = onRoot().captureToImage().toPixelMap()
    val sampled = pixels[pixels.width - 1, pixels.height - 1]
    assertEquals(AeroColorScheme.AeroBlue.background, sampled, "...")
}
```
**Caveat (Pitfall 5 / Open Question 1 in RESEARCH.md):** no existing test in this codebase opens a `Popup` and then calls `captureToImage()`. The first D-07 test written must empirically check whether `onRoot()` sees the popup's pixels or whether `onAllNodes(isRoot(), useUnmergedTree = true)` (unverified API) is needed to enumerate/capture all roots. If `onRoot()` doesn't reach the popup layer, fall back to D-08 (after-only MCP + `PrintWindow` inspection, marked "inspected without baseline") for that component only — this is NOT a stop per D-08.

**Secondary analog for real-input driving + fixed sampling points:** `library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderHoverCancellationTest.kt` — shows the idiom for opening something via `performMouseInput`, waiting `waitForIdle()`, then sampling a fixed `Offset` computed from `fetchSemanticsNode().boundsInRoot` (useful for locating an opened popup's known screen position deterministically, lines 70-88).

**D-09 determinism requirement:** every picker test (`AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`, `AeroDateTimeRangePicker`, `AeroTimePicker`) must pass a fixed non-null `value` so the calendar does not open on `todayLocalDate()` (`Clock.System.now()`), per D-09. The 4 `Clock.System.now()` call sites to be aware of when picking a fixed value: `AeroDatePicker.kt:167-168`, `AeroDateTimePicker.kt:196-197`, `AeroDateRangePicker.kt:263-264`, `AeroDateTimeRangePicker.kt:333-334`.

**Popup-source files to cover (14 files, `Popup(` grep-confirmed):**
```
library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt
library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt
library/src/main/kotlin/com/mordred/aero/components/navigation/AeroMenuBar.kt
library/src/main/kotlin/com/mordred/aero/components/overlay/AeroContextMenu.kt
library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt
library/src/main/kotlin/com/mordred/aero/components/overlay/AeroPopover.kt
library/src/main/kotlin/com/mordred/aero/components/overlay/AeroTooltip.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPickerButton.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroTimePicker.kt
library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt
```
(`AeroDialog.kt`, `AeroAlertDialog.kt` also in scope per D-07, built on `Dialog` not `Popup`.)

---

### Drag-based UI tests, if BASE-05 covers AeroSplitPane/AeroPanelGroup/AeroDataTable resize (test, event-driven)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt`

**Deterministic programmatic drag pattern** (lines 94-109):
```kotlin
val root = onRoot()
root.performMouseInput {
    moveTo(Offset(dividerX, y))
    press()
}
repeat(10) { step ->
    root.performMouseInput {
        moveTo(Offset(dividerX + step * 5f, y))
    }
    waitForIdle()
}
root.performMouseInput { release() }
waitForIdle()
```
Locate the drag target via `onNodeWithText(...).fetchSemanticsNode().boundsInRoot` (line 90), never a hardcoded pixel coordinate.

---

### Animation-stepping / noise-freezing pattern (BASE-03 discretion, test, streaming)

**Analog:** `library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt`, lines 126-158

```kotlin
mainClock.autoAdvance = false
// ... trigger the animation (click, state change) ...
waitForIdle()
val samples = mutableListOf<Color>()
repeat(FRAME_COUNT) {
    mainClock.advanceTimeByFrame()
    waitForIdle()
    samples.add(sampleLabelColor(target))
}
```
Directly reusable for `AeroProgressBar`'s two `rememberInfiniteTransition`s (`runningSheen`, `indeterminate`) and the text caret if BASE-03 chooses "freeze via manual clock" over "measure and list" for UI-test captures (BASE-04/showcase-level noise, driven via a real window, needs a different freezing mechanism — MCP has no clock control, so BASE-03's showcase-level noise handling is measure-and-list only; this pattern applies to BASE-05/D-07 UI-test captures specifically).

---

### TOOL-16 test-count guard task (new, `library/build.gradle.kts`, build-config, event-driven)

**No in-repo analog** — this is the first `Test` task listener in the project. Use the RESEARCH.md Code Examples sketch directly (`21-RESEARCH.md` "Test-count guard sketch", not yet validated against Gradle 9.7.1):
```kotlin
tasks.test {
    useJUnitPlatform()
    var executedCount = 0
    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        if (desc.parent == null) { // root suite only
            executedCount = result.testCount.toInt()
            val expected = (project.findProperty("aero.expectedTestCount") as String?)?.toIntOrNull()
            if (expected != null && executedCount != expected) {
                throw GradleException("Test count guard: expected $expected, got $executedCount")
            }
        }
    }))
}
```
**Prove red first** (locked rule): temporarily `tasks.test { exclude("**/SomeKnownTestClass.class") }`, confirm the guard throws, then remove the exclusion. This sits in the SAME `tasks.test { }` block as `useJUnitPlatform()` (current `library/build.gradle.kts:44-46`), so the D-03 opt-in image-writing property (below) and this guard are natural neighbors in the same edit.

---

### D-03 opt-in image-writing property forwarding (new, `library/build.gradle.kts` `tasks.test`, request-response)

**No exact in-repo analog for `tasks.test`** — but the general "Gradle property → JVM-visible flag, read only when present" idiom already exists at the `JavaExec`/run-task level in `showcase/build.gradle.kts:27-31` and should be mirrored at the `Test` task level:
```kotlin
// showcase/build.gradle.kts:27-31 — the idiom to mirror on tasks.test
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
}
```
Applied to `library/build.gradle.kts`, the equivalent shape is `tasks.test { (project.findProperty("aero.writeCaptures") as String?)?.let { systemProperty("aero.writeCaptures", it) } }`, read inside the test file via `System.getProperty("aero.writeCaptures") != null` before writing into `.captures/` — so an ordinary `./gradlew test` (property absent) never dirties the working tree (D-03).

---

### BASE-01 section launch parameter (extend existing, `showcase/build.gradle.kts` + `Main.kt`, request-response)

**Analog:** `showcase/build.gradle.kts` lines 23-31 (existing, unchanged) — the exact pattern to extend:
```kotlin
// Forward -Paero.scheme=<name> to the run task as a system property, so a review pass can open a
// specific theme directly (see initialScheme() in Main.kt) instead of clicking the theme switcher.
// withType(...).configureEach is lazy: the Compose Desktop plugin registers `run` after this
// script is evaluated, so tasks.named("run") would fail with "Task with name 'run' not found".
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
}
```
Add `aero.section` to this SAME block, plus a sibling block for Hot Reload's `ComposeHotRun` task type (not a `JavaExec`):
```kotlin
tasks.withType<org.jetbrains.compose.reload.ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
}
```
(FQN `org.jetbrains.compose.reload.ComposeHotRun` is RESEARCH.md Assumption A1 — confirm via `gradlew :showcase:help --task hotRun` before writing.)

**`Main.kt` analog** (lines 21-33, `initialScheme()` — pattern to mirror for `initialSection()`):
```kotlin
private fun initialScheme(): AeroColorScheme = when (System.getProperty("aero.scheme")) {
    "AeroDark" -> AeroColorScheme.AeroDark
    "Classic" -> AeroColorScheme.Classic
    else -> AeroColorScheme.AeroBlue
}
```

**`ShowcaseApp.kt` current section list** (lines 79-119, order): `ThemeSwitcher`, `VerificationSection`, inline `Foundation` heading + `FoundationSection`, inline `Primitives` heading + `PrimitivesSection`, `IconsSection`, `ButtonsSection`, `InputSection`, `SelectionSection`, `DropdownSection`, `RangeSection`, `ListSection`, `ContainersSection`, `OverlaysSection`, `NavigationSection`, `DataSection`, `PickersSection`, `LayoutSection`. `Foundation`/`Primitives` are the two NOT wrapped in a standalone `*Section()` composable of their own (Pitfall 3) — RESEARCH.md recommends excluding them from the addressable list by default.

---

### BASE-02 `PrintWindow` capture helper (new, external PowerShell/WinAPI script, file-I/O)

**No in-repo `PrintWindow` analog** — the v3.0 precedent (`reference_windows_mcp_showcase_capture.md`) used `Graphics.CopyFromScreen`, explicitly superseded this phase (D-01/MCP-HOWTO.md: `CopyFromScreen`/`take_screenshot` both scrape the screen region and fail when occluded). Reuse the SCRIPT STRUCTURE (PowerShell + `Add-Type` C# P/Invoke, save PNG, sanity-check pixels) but swap the capture call:
- Old (v3.0, superseded): `Graphics.CopyFromScreen(0,0,0,0,1920x1080)` → PNG.
- New (this phase, per MCP-HOWTO.md): `PrintWindow(hwnd, hdc, 2)` (the `2` flag = `PW_RENDERFULLCONTENT`, captures occluded content) → sanity-check 2px against app background → save PNG to `.captures/`.
- Window handle discovery: `FindWindow`/`EnumWindows` by title (same as v3.0's approach, per RESEARCH.md architecture diagram).
- Minimized-window handling (new, no v3.0 precedent): `ShowWindow(h, 4)` (`SW_SHOWNOACTIVATE`, no focus steal) before capture; keep behind other windows via `SetWindowPos(h, HWND_BOTTOM, ..., 0x13)`.
- PowerShell 5.1 only (no PS7, no `Diff` as a function name — it's a built-in alias for `Compare-Object`, per the v3.0 memory's caveat at `reference_windows_mcp_showcase_capture.md:17`).

---

### Report formats (BASE-04 baseline record, D-04/D-05 drift stop-list) — reuse v3.0 shape

**Analog:** `.planning/milestones/v3.0-phases/15-toolchain-upgrade/baseline/README.md` and `.../after/DIFF-REPORT.md`

**Baseline record shape** (`baseline/README.md`): toolchain-at-capture-time table, themes list, target-components checklist, capture method + framing description, a table mapping `Theme | File | Sections shown | Target components`.

**Diff-report shape** (`after/DIFF-REPORT.md`): a **Verdict** line first, a **Method** paragraph, a results table (`Pair | diff px | % | maxΔ | meanΔ | Interpretation`), a **Notes on non-zero rows** section explaining each non-trivial diff by named cause, and a **Conclusion** tying back to the requirement ID. This exact shape maps directly onto D-04/D-05's "before/after pair, named cause, proposed action" requirement — reuse the table columns, add a "Proposed action" column, and change "Verdict" to reflect D-04's "one stop with the complete list" (i.e. no verdict of "no drift" is assumed; the list itself IS the deliverable, even for the zero-diff sections/states).

**Delta from v3.0:** this phase's report text is committed but its IMAGES are NOT (`.captures/`, gitignored, D-01) — v3.0's images were committed directly beside the `.md` files (`baseline/*.png`, `after/*.png`). New reports must reference images by `.captures/`-relative path instead of a sibling file (D-02).

---

## Shared Patterns

### `runComposeUiTest` + `AeroTheme` wrapping, all 3 schemes
**Source:** `VER07ButtonStateMatrixTest.kt`, `VER08SegmentLabelFlipTest.kt`, `AeroThemeBackgroundEstablishmentTest.kt`
**Apply to:** every new BASE-05/D-07 test file
```kotlin
@OptIn(ExperimentalTestApi::class)
class SomeStateMatrixTest {
    @Test fun aeroBlue() = run("AeroBlue", AeroColorScheme.AeroBlue)
    @Test fun aeroDark() = run("AeroDark", AeroColorScheme.AeroDark)
    @Test fun classic() = run("Classic", AeroColorScheme.Classic)

    private fun run(label: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent { AeroTheme(colorScheme = scheme) { /* ... */ } }
        waitForIdle()
        // ...
    }
}
```

### Pixel-comparison via `pixelMapsDiffer`
**Source:** `VER07ButtonStateMatrixTest.kt:265-273`
**Apply to:** every BASE-05/D-07/VER-07/VER-08 test that needs "did this state's capture change" — reuse this exact function (make it a shared `internal` utility rather than re-copying per file, since multiple new test files need it — consider extracting to a shared test-fixtures file under `library/src/test/kotlin/com/mordred/aero/verification/` if 3+ new files need it).

### D-08 falsifiability fixture proof, in the SAME file as the real gate
**Source:** `VER07ButtonStateMatrixTest.kt:87-124`, `VER08SegmentLabelFlipTest.kt:70-107`, `AeroThemeBackgroundEstablishmentTest.kt:74-94`
**Apply to:** every new mechanical-comparison test file (BASE-05 proof-of-work, D-07 popup tests, TOOL-16 count guard) — a synthetic case proving the gate CAN fail (detects a genuine difference) and a synthetic case proving it doesn't vacuously always-fail (accepts an indistinguishable pair), both exercising the exact same comparator function the real assertions use.

### `testTag` + `onNodeWithTag` targeting (not text/index-based where avoidable)
**Source:** `VER07ButtonStateMatrixTest.kt` (`testTag("btn")`, `testTag("capture")`, `testTag("other")`), `AeroRangeSliderHoverCancellationTest.kt` (`testTag("slider")`)
**Apply to:** any showcase-level `testTag`s added for HRM-02 (currently zero exist in `showcase/src`, Pitfall 2) — mirror the library-test convention of one tag per addressable element, named for its role not its content.

### `-P<name>` Gradle property → `System.getProperty` JVM flag
**Source:** `showcase/build.gradle.kts:23-31` + `Main.kt:29-33`
**Apply to:** BASE-01 (`aero.section`), D-03 (`aero.writeCaptures`), TOOL-16 (`aero.expectedTestCount` — already in the RESEARCH.md sketch)

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `.mcp.json` | config | request-response (stdio spawn) | First MCP server config in this project; use MCP-HOWTO.md's verbatim JSON block, not a codebase pattern |
| TOOL-16 count-guard `Test.afterSuite` listener | build-config task | event-driven | No existing Gradle `Test` task listener in either module; use RESEARCH.md's Code Examples sketch, prove red first per the locked rule |
| BASE-02 `PrintWindow` capture script | utility (external) | file-I/O | No `PrintWindow` usage anywhere in-repo (v3.0 used the now-superseded `CopyFromScreen`); build from MCP-HOWTO.md's WinAPI spec, reusing only the PowerShell/`Add-Type` script SHAPE from the v3.0 memory |
| D-06 hand-off HTML page | utility (generator) | file-I/O | No HTML-report generator exists; v3.0's `DIFF-REPORT.md`/`README.md` are Markdown, not HTML — reuse their CONTENT structure (before/after pairs, cause, lists) but the output format itself is new |
| D-07 popup capture inside `runComposeUiTest` | test | streaming | Genuinely untested territory in this codebase (RESEARCH.md Pitfall 5 / Open Question 1) — `onRoot()` capturing an open `Popup`'s content has no prior test; the first D-07 test IS the proof-of-work |
| `AeroDropdownPopup.kt` / dialog-popup source composables themselves | component | request-response | Not modified by this phase (no visual changes outside D-04 drift fixes) — listed only because D-07 tests target them; source files stay as-is |

## Metadata

**Analog search scope:** `library/src/test/kotlin/com/mordred/aero/**`, `library/src/main/kotlin/com/mordred/aero/components/pickers/**`, `library/src/main/kotlin/com/mordred/aero/components/layout/**`, `showcase/src/main/kotlin/com/mordred/showcase/**`, `gradle/`, root build files, `.planning/milestones/v3.0-phases/15-toolchain-upgrade/`, `.planning/research/MCP-HOWTO.md`, user memory (`reference_windows_mcp_showcase_capture.md`, `reference_compose_hot_reload_mcp.md`)
**Files scanned:** ~20 read directly (test files, build files, Main.kt, ShowcaseApp.kt, v3.0 baseline/diff reports, MCP-HOWTO.md) + grep sweeps (`Popup(`, `dayOfMonth`/`monthNumber`, `@Test`, `testTag`, `systemProperty`)
**Pattern extraction date:** 2026-09-23

## Conventions

Convention derivation (`gsd-tools.cjs verify conventions --derive`) was run against this repo but **skipped**: the deriver's idiom rule packs only cover JS/TS file extensions (`bin/lib/conventions.cjs:46`), and this project is 100% Kotlin (`library/src/**/*.kt`, `showcase/src/**/*.kt`, `.gradle.kts` build scripts) — `reason: "no-readable-files"` on every scope tried (repo-wide and `library/src/test` alone). No 4-axis table is produced; this section is a documented skip, not a finding.

**Kotlin-native conventions observed directly during this mapping pass (not tool-derived, informal):**
- File naming: PascalCase matching the primary class/composable (`AeroButton.kt`, `VER07ButtonStateMatrixTest.kt`) — consistent across every file read.
- Identifier casing: `camelCase` for functions/properties, `PascalCase` for classes/composables/types — no exceptions observed.
- Import style: fully-qualified, one-per-line, no wildcard imports, alphabetically groupable by package prefix — consistent in every file read (`VER07ButtonStateMatrixTest.kt:1-28`, `Main.kt:1-19`).
- Package-per-directory: `package com.mordred.aero.<area>` matches the directory path exactly in every file read — no deviations found.

**Contested hotspots (author's choice):** none surfaced in this Kotlin codebase during this pass — unlike a JS/TS repo with a CJS↔ESM dual-resolver split (the documented prototype contested-hotspot case), this project has a single, uniform Kotlin/Gradle-KTS toolchain throughout; `library/build.gradle.kts` and `showcase/build.gradle.kts` share the identical plugin-alias and `jvmToolchain(...)` idiom. If a genuine split ever appears here, it would most likely be at the `library` (published, `explicitApi()`-enforced) vs. `showcase` (internal, unrestricted) API-visibility boundary — that boundary is intentional and directory-scoped (per-module), not a contested style question, so planners/reviewers should treat `library`'s `explicitApi()` discipline as non-negotiable and `showcase`'s looser visibility as its own consistent local convention, each internally uniform per module.
