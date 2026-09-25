# Phase 22: Native Window Behavior + Release 3.2.0 - Pattern Map

**Mapped:** 2026-09-25
**Files analyzed:** 24 (10 new `internal/windows`+public source files, 5 new test files, 2 new tool scripts, 7 modified config/doc/existing-component files)
**Analogs found:** 20 / 24 (4 explicitly "no analog" — genuinely new categories of code in this codebase)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` | component | event-driven + request-response | itself (existing file, modified in place) | exact (self) |
| `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` | component | event-driven | itself (existing file, modified in place) | exact (self) |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt` | service (registry/lifecycle) | event-driven | `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt` (package shape) — no lifecycle-registry analog exists | role-match (package convention only) |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt` | controller (native message dispatch) | event-driven | none — no existing native-callback / WndProc code in this codebase | no analog |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistry.kt` | model/store (immutable-snapshot publisher) | pub-sub | `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt` (pure-JVM, `internal`, no Compose imports) | role-match |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroMaxButtonInteraction.kt` | provider (state bridge) | event-driven | `AeroTitleBar.kt`'s `TitleBarButton` (`collectIsHoveredAsState()` pattern it must mimic the shape of) | role-match |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Chrome.kt` | utility (Win32 style/geometry math) | transform | `PanelDistribution.kt` (pure-function, `internal`, unit-testable style) | exact (style) |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Dpi.kt` | utility (DPI math) | transform | `PanelDistribution.kt` | exact (style) |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt` | utility (JNA interface declarations) | request-response | none — no existing JNA/native-interop code in this codebase | no analog |
| `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt` | component (public additive API) | request-response | `AeroTitleBar.kt` (public composable signature conventions, `FrameWindowScope` receiver) | exact |
| `gradle/libs.versions.toml` | config | batch | itself (existing file, additive entries) | exact (self) |
| `library/build.gradle.kts` | config | batch | itself (existing file — dependency block + `lockedTestTotal` guard) | exact (self) |
| `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` | component (entry point) | event-driven | itself (existing file — single-`Window` `application {}` block to extend to two) | exact (self) |
| `showcase/build.gradle.kts` | config | batch | itself (existing file — `-Paero.*` forwarding blocks to extend) | exact (self) |
| `library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestClassificationTest.kt` | test (pure-JVM unit) | transform | `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt` | exact |
| `library/src/test/kotlin/com/mordred/aero/internal/windows/Win32DpiTest.kt` | test (pure-JVM unit) | transform | `PanelGroupLogicTest.kt` | exact |
| `library/src/test/kotlin/com/mordred/aero/internal/windows/MaximizedRectTest.kt` | test (pure-JVM unit) | transform | `PanelGroupLogicTest.kt` | exact |
| `library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistryTest.kt` | test (concurrency stress) | event-driven | `PanelGroupLogicTest.kt` (closest structure; no concurrency-stress test exists yet) | role-match |
| `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt` | test (Compose UI, compile-gate) | request-response | itself (existing file, extend with region-reporting assertions) | exact (self) |
| `tools/<live-window harness>/*.kt` (new opt-in `main()`, exact path is Claude's Discretion per CONTEXT.md) | tool (live-window probe) | event-driven | `tools/capture/AeroCapture.ps1` (HWND discovery + non-interfering probe conventions) | role-match (cross-language) |
| `tools/<flyout detector>` (UI Automation, PowerShell or Kotlin) | tool (native state probe) | event-driven | `tools/capture/AeroCapture.ps1` + `tools/verify/check-material3.sh` (grep-gate exit-code convention) | role-match |
| `README.md` | docs | batch | itself (existing file — new "Windows window behavior" section, net-new content) | exact (self) |
| `build.gradle.kts` (root) | config | batch | `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/21-14-PLAN.md` Task 2 (version bump + tag) | exact |
| `library/src/main/kotlin/com/mordred/aero/AeroTitleBar.kt` KDoc / REL-07 string removal | docs (in-source) | transform | itself, lines 63-66 (exact string to replace) | exact (self) |

## Pattern Assignments

### `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` (component, modified in place)

**Analog:** itself — current source already read in full (174 lines).

**Current public signature** (lines 74-81):
```kotlin
@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
```
API-01/API-02/API-03 require only **trailing additive default parameters** here (e.g. `nativeWindowManagement: Boolean = true`) — every existing call site (`showcase/Main.kt:98-102`) must keep compiling unmodified. This is the exact "source-compatible extension" pattern `ARCHITECTURE.md`'s Public API Shape section already specifies verbatim — copy that shape, do not re-derive it.

**Draggable-area wrapper to remove** (line 83, KDoc at lines 63-66):
```kotlin
WindowDraggableArea(modifier = modifier) {
    Row( /* whole title Row incl. all 3 TitleBarButtons */ ) { ... }
}
```
Per `PITFALLS.md` Pitfall 15 (cited in RESEARCH.md), this wrapper is a **second, competing input path** once native `HTCAPTION` hit-testing covers the same pixels and must be deleted outright, not left as a fallback. The KDoc paragraph at lines 63-66 (`"**Aero Snap limitation:** WindowDraggableArea does NOT pass HTCAPTION..."`) is the literal string REL-07 requires replaced.

**Maximize icon / placement read** (lines 119-136) — unchanged logic, already correct per `ARCHITECTURE.md`'s verified non-finding (half-snap never sets `Maximized`):
```kotlin
icon = if (windowState.placement == WindowPlacement.Maximized)
           AeroIcons.FrameCorners
       else
           AeroIcons.Square,
...
onClick = {
    windowState.placement =
        if (windowState.placement == WindowPlacement.Maximized)
            WindowPlacement.Floating
        else
            WindowPlacement.Maximized
}
```
WIN-05 only requires *verifying* this stays correct once maximize can also be triggered by `WM_SYSCOMMAND SC_MAXIMIZE` outside this `onClick` — no code change assumed here without empirical proof.

**`TitleBarButton` hover/press source** (lines 148-173) — the pattern the maximize button's new state source (`AeroMaxButtonInteraction`) must visually match:
```kotlin
@Composable
private fun TitleBarButton(
    icon: ImageVector,
    hoverColor: Color,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 32.dp)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(if (hovered) hoverColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) { Icon(...) }
}
```
Per `ARCHITECTURE.md` Pattern 3, the maximize button instance needs its `hovered`/`pressed` booleans sourced from `AeroMaxButtonInteraction.hovered`/`.pressed` (same `State<Boolean>` shape `collectIsHoveredAsState()` already returns) instead of its own `interactionSource` — parameterize at the call site (lines 113-142), not inside `TitleBarButton` itself, so minimize/close keep this exact code path unchanged.

**`leading` slot** (declared line 79, rendered lines 99-102) — reuse point for API-02/BTN-02, not a new slot:
```kotlin
if (leading != null) {
    leading()
    Spacer(Modifier.width(8.dp))
}
```

**No `onGloballyPositioned` anywhere in this file today** — Step 4's region-registry wiring (`Modifier.onGloballyPositioned { coords -> regionRegistry.publish(...) }`, per `ARCHITECTURE.md` Pattern 2) is genuinely new code added to this `Row`/`TitleBarButton` layout, not a modification of an existing callback.

---

### `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` (component, modified in place)

**Analog:** itself — current source already read in full (232 lines).

**Signature** (line 55) — no new parameter needed (per `ARCHITECTURE.md`, the Windows/non-Windows branch is fully internal):
```kotlin
@Composable
public fun FrameWindowScope.AeroResizeHandles(windowState: WindowState) {
```

**The one existing gate to extend** (line 56):
```kotlin
if (windowState.placement != WindowPlacement.Floating) return
```
WIN-02's target shape (from RESEARCH.md's own "Code Examples" section, illustrative not literal):
```kotlin
if (windowState.placement != WindowPlacement.Floating) return
if (isWindows && nativeChromeActive) return  // OS now owns HT*-code resize on this HWND
```
`nativeChromeActive` is a Windows-only per-window read (e.g. `NativeWindowChromeRegistry.isInstalled(window)`) that is always `false` on Linux/macOS — preserving today's behavior there with zero other changes to this file. The 8 `Box`+`pointerInput`+`detectDragGestures` zones below (lines 76-229) and the `minW = 320f, minH = 240f` floor (lines 60-61) need **no internal-logic changes** — they become dead code on Windows once the gate fires, live code unchanged elsewhere.

**Minimum-size constant, load-bearing for D-01:**
```kotlin
val minW = 320f
val minH = 240f
```
D-01 requires this floor honored only when the app hasn't set its own `window.minimumSize` (`Component.isMinimumSizeSet()`); the SHW-17 narrow showcase window sets its own AWT minimum below 300px specifically to exercise the override path.

---

### `library/src/main/kotlin/com/mordred/aero/internal/windows/*.kt` (new package — `NativeWindowChromeRegistry.kt`, `AeroWndProc.kt`, `HitTestRegionRegistry.kt`, `AeroMaxButtonInteraction.kt`, `Win32Chrome.kt`, `Win32Dpi.kt`, `Win32Interop.kt`)

**Package-shape analog:** `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt`

**Imports pattern** (line 1) — pure-JVM, no Compose imports, `internal` visibility:
```kotlin
package com.mordred.aero.components.layout.internal.panelgroup

/**
 * Pure-JVM distribution and clamp logic for AeroPanelGroup.
 *
 * No Compose imports — fully unit-testable without a Compose runtime.
 */
```
Copy this exact discipline for `Win32Chrome.kt`/`Win32Dpi.kt`: every pure-math function (hit-test classification, DPI-thickness formulas, maximized-client-rect math) stays free of Compose/JNA imports so it can be tested headlessly, matching `ARCHITECTURE.md`'s own Testing Architecture split ("headless, part of `:library:test`" vs "needs a live window").

**Core function shape** (lines 35-44), the pattern to copy for e.g. `classifyHitTest(...)` / `computeMaximizedClientRect(...)`:
```kotlin
internal fun clampPanelDividerPx(
    aboveSizePx: Float,
    deltaPx: Float,
    minAbovePx: Float,
    minBelowPx: Float,
    totalBudgetPx: Float,
): Float {
    val maxAbovePx = (totalBudgetPx - minBelowPx).coerceAtLeast(minAbovePx)  // guard comment names the pitfall
    return (aboveSizePx + deltaPx).coerceIn(minAbovePx, maxAbovePx)
}
```
Doc-comment convention: each function's KDoc names the requirement ID it satisfies and the pitfall it guards against (e.g. `PNL-10 / PNL-PITFALL-04`) — for this phase, cite the matching `WIN-*`/`SNAP-*` requirement ID and the `PITFALLS.md` pitfall number the same way.

**No analog exists** for `AeroWndProc.kt` (native `WindowProc` callback/dispatch) or `Win32Interop.kt` (hand-declared JNA extension interfaces for `GetDpiForWindow`/`GetSystemMetricsForDpi`/`TrackMouseEvent`/`Dwmapi`) — this codebase has never called into native code via JNA before. Use `ARCHITECTURE.md`'s own "Architectural Patterns" §1 (`CallWindowProc` passthrough) and `STACK.md` §2 (extension-interface declarations) as the authoritative shape; there is nothing closer in this repo to copy from. Same for `NativeWindowChromeRegistry.kt` (HWND-keyed strong-`Callback` registry, lifecycle singleton) — no existing registry/singleton-with-lifecycle pattern exists in `:library`; follow `ARCHITECTURE.md`'s Component Responsibilities table verbatim.

**`HitTestRegionRegistry.kt` / `AeroMaxButtonInteraction.kt`** have a closer conceptual analog in `AeroTitleBar.kt`'s `TitleBarButton` `collectIsHoveredAsState()`/`MutableInteractionSource` shape (the `State<Boolean>` read contract `AeroMaxButtonInteraction` must expose so `TitleBarButton` barely changes) — see the excerpt already given above.

---

### `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt` (new, public, additive)

**Analog:** `AeroTitleBar.kt` public signature + `FrameWindowScope` receiver conventions (lines 74-81).

**Target shape** (from `ARCHITECTURE.md`'s "Public API Shape", authoritative — cite directly, do not re-derive):
```kotlin
public fun Modifier.markAeroTitleBarInteractive(): Modifier

@Composable
public fun FrameWindowScope.rememberAeroWindowChrome(
    windowState: WindowState,
): AeroWindowChromeState

public interface AeroWindowChromeState {
    public fun Modifier.captionArea(): Modifier
    public fun Modifier.captionExclude(): Modifier
    public fun Modifier.maximizeButtonArea(): Modifier
    public val maximizeHovered: State<Boolean>
    public val maximizePressed: State<Boolean>
}
```
Same `public`-because-showcase-is-a-separate-module rationale as `AeroResizeHandles`'s own KDoc (`ResizeHandles.kt` lines 51-52): `internal` is per-module in Kotlin, so anything the showcase (a separate Gradle module) must call has to be `public`.

---

### `gradle/libs.versions.toml` / `library/build.gradle.kts` (config, dependency + test-count guard)

**Analog:** itself — both files already read in full.

**`libs.versions.toml` — no existing `jna` entries; add alongside `kotlinx`/`junit` entries** (existing shape, lines 1-14):
```toml
[versions]
kotlinxCoroutines = "1.11.0"
...
[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
```
New entries (from RESEARCH.md's own Standard Stack section):
```toml
[versions]
jna = "5.19.1"
[libraries]
jna = { module = "net.java.dev.jna:jna", version.ref = "jna" }
jna-platform = { module = "net.java.dev.jna:jna-platform", version.ref = "jna" }
```

**`library/build.gradle.kts` dependency block** (lines 27-38) — the `api`-vs-`implementation` convention this phase must follow, already documented in-file:
```kotlin
dependencies {
    api(compose.desktop.common)
    ...
    // Internal only — not exposed in any public signature.
    implementation(libs.kotlinx.coroutines.core)
    ...
}
```
JNA must go in the `implementation` block with the same one-line comment convention, per DEP-01/API-01 (no `com.sun.jna.*` type in any public signature).

**Test-count guard** (lines 1-8, 53-94) — the exact mechanism VER-14 bumps:
```kotlin
// TOOL-16: tests executed on the pre-upgrade toolchain (Kotlin 2.4.10 / CMP 1.11.1), measured by
// an actual `./gradlew :library:test --rerun` run immediately before the first version bump. The
// guard in tasks.test below fails the build on any other number; change only with a commit that
// states why.
val lockedTestTotal = 541
val lockedTestSkipped = 0
```
The `TestListener` + `doLast { ... throw GradleException(...) }` block (lines 60-93) is unchanged mechanics — only `lockedTestTotal` moves. **Commit precedent:** `ffece58` ("build(21-05): lock executed test count at 541 with a count guard (TOOL-16)") introduced the guard; this phase's bump is the **first real bump**, message convention `build(22-NN): raise locked test count to <N> — <reason> (VER-14)`.

---

### `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` (entry point, modified — second window)

**Analog:** itself — full 115-line file already read.

**Existing single-`Window` shape inside `application {}`** (lines 70-114) — the second `Window(...)` call is additive inside this same block (CMP's `application {}` DSL supports multiple `Window` calls natively):
```kotlin
application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp, ...)
    Window(
        onCloseRequest = ::exitApplication,
        title = ...,
        state = windowState,
        undecorated = true,
        transparent = false,
        focusable = !capture
    ) {
        ...
        AeroTheme(colorScheme = currentScheme) {
            Box(Modifier.fillMaxSize()...) {
                Column(Modifier.fillMaxSize()) {
                    AeroTitleBar(title = ..., windowState = windowState, onCloseRequest = ::exitApplication)
                    ShowcaseApp(...)
                }
                AeroResizeHandles(windowState)
            }
        }
    }
}
```
SHW-17's second, narrow (~300px) window needs its own `rememberWindowState(width = ~300.dp, ...)`, its own `AeroTitleBar`/`AeroResizeHandles` pair, and one interactive header element marked via API-02 (mirroring Pinya's detached-queue-window scenario named in CONTEXT.md's Specific Ideas) — **no existing multi-window precedent in this codebase**, this is genuinely new showcase code following the single-window shape above, not a refactor.

**Existing launch-property plumbing** (lines 32-63), the pattern a new `-Daero.secondWindow`-style flag must follow:
```kotlin
private fun captureMode(): Boolean = System.getProperty("aero.capture") == "true"
```

---

### `showcase/build.gradle.kts` (config, `-Paero.*` forwarding)

**Analog:** itself — full 60-line file already read.

**Both forwarding blocks to extend with the third property** (lines 30-48) — a known, already-recorded duplication (`21-REVIEW.md` IN-01); do not silently refactor it away, add the new property to both blocks matching the existing pattern:
```kotlin
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
        ...
    }
}
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    ...
}
```

---

### `library/src/test/kotlin/com/mordred/aero/internal/windows/*Test.kt` (new pure-JVM unit tests)

**Analog:** `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt`

**Imports + class shape** (lines 1-14):
```kotlin
package com.mordred.aero.components.layout

import com.mordred.aero.components.layout.internal.panelgroup.activeDividerCount
import com.mordred.aero.components.layout.internal.panelgroup.clampPanelDividerPx
...
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PanelGroupLogicTest {
```

**Named-test-wired-to-requirement-doc convention** (lines 16-50):
```kotlin
/**
 * PNL-04: 3 equal expanded sections; distribute at totalPx=900 then 600;
 * each section's share ratio is preserved (~1/3 of availableForExpanded) within 0.5f.
 */
@Test
fun distributePxWindowResizePreservesRatios() {
    ...
    assertEquals(expected900, heights900[0], 0.5f)
    ...
}
```
Copy this exact discipline for `HitTestClassificationTest.kt` (table-driven tests across caption/button/client/edge/corner points), `Win32DpiTest.kt` (arithmetic at 100/125/150/200% scale), and `MaximizedRectTest.kt` (synthetic monitor geometries incl. taskbar on each of 4 edges) — each `@Test` name and KDoc cites the `SNAP-*`/`WIN-*` requirement ID it proves, same as `PNL-04` above. New test package: `library/src/test/kotlin/com/mordred/aero/internal/windows/` (does not exist today — mirrors the new main-source package 1:1, same as every other component-family test directory in this repo).

---

### `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt` (existing, extend)

**Analog:** itself — full 22-line file already read.

```kotlin
/**
 * Compile-only test — AeroTitleBar requires FrameWindowScope receiver and cannot
 * be invoked headlessly. Manual smoke is the primary verification (see VALIDATION.md).
 */
class AeroTitleBarTest {
    @Test
    fun aeroTitleBarFileCompiles() {
        assertTrue(true)
    }
}
```
For the new region-reporting behavior, either extend this compile-gate style or add `runComposeUiTest`-based assertions (already a `testImplementation` dependency in `library/build.gradle.kts` line 49) in the **same package**, not a new one — RESEARCH.md's Test Infrastructure section explicitly names this file as the attachment point.

---

### `tools/<live-window harness>` and `tools/<flyout detector>` (new, opt-in, VER-11/VER-12)

**Analog:** `tools/capture/AeroCapture.ps1` (HWND discovery, `PrintWindow`-based capture) and `tools/verify/check-material3.sh` (grep-gate exit-code convention).

**HWND discovery + non-interfering probe conventions** (`AeroCapture.ps1` lines 17-118) — the shape a Kotlin/JVM `main()` under `tools/` should mirror conceptually (find the showcase window by title, never move the real cursor, never call `take_screenshot`):
```csharp
public static List<IntPtr> FindTopLevelWindows(string exactTitle)
{
    List<IntPtr> found = new List<IntPtr>();
    EnumWindowsProc callback = delegate(IntPtr hWnd, IntPtr lParam)
    {
        if (!IsWindowVisible(hWnd)) return true;
        ...
        if (string.Equals(sb.ToString(), exactTitle, StringComparison.Ordinal))
        {
            found.Add(hWnd);
        }
        return true;
    };
    EnumWindows(callback, IntPtr.Zero);
    return found;
}
...
[DllImport("user32.dll")]
public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, uint nFlags);
```
`PrintWindow(hwnd, hdc, 2)` (`PW_RENDERFULLCONTENT = 2`, defined line 49) is the **only sanctioned capture method project-wide** per the maintainer's own memory (`reference_compose_hot_reload_mcp.md`) — reuse this constant's value, never the MCP `take_screenshot` tool.

**Grep-gate / exit-code convention** (`check-material3.sh`, full 49-line file) — the shape for any pass/fail tool output the new harness or flyout detector should follow:
```bash
if grep -qi "alpha" "$LOG_FILE"; then
    echo "MATERIAL3 FAIL: alpha found on a classpath — see $LOG_FILE" >&2
    exit 1
fi
...
echo "MATERIAL3 OK"
```
**No existing UI-Automation tooling or live-HWND-message-probe tool exists in this repo** (confirmed by RESEARCH.md's own Environment Availability table) — this is genuinely new tooling; `ARCHITECTURE.md`'s Testing Architecture §"Needs a live window" section and CONTEXT.md's Claude's Discretion note (Kotlin `main()` under `tools/`, following the `tools/capture/` precedent over a new Gradle source set) are the authoritative shape to follow, not a closer in-repo analog.

---

### `README.md` (docs, net-new "Windows window behavior" section)

**Analog:** `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/21-13-PLAN.md` Task 2 (README consumer-floor edit precedent) + `README.md`'s own existing structure.

**Existing structure to extend** (confirmed lines):
```
line 6:   > Package: `com.mordred.aero` · Kotlin `2.4.20` · Compose Multiplatform `1.12.0` · JVM 21
line 73:  implementation("com.github.Tolaseeq:aero-compose-ui:v3.1.0")
line 80:  **Toolchain requirement.** `v3.1.0` requires **Java 21**, ...
line 89:  ### Usage
line 111: ### Theming
line 127: ## Building & testing
line 134: ## Tech stack
```
REL-06 is **net-new content** (grep for "Aero Snap"/"undecorated"/"window behavior" returns zero matches today) — a new `## Windows window behavior` section, placed the same way `21-13-PLAN.md`'s Task 2 placed the consumer-floor paragraph "directly after the dependency snippet" (within a bounded number of lines of the version string), not appended at the end. Content per RESEARCH.md REL-06: what works, Win10-vs-Win11 differences, how to mark an interactive title-bar element (API-02's `markAeroTitleBarInteractive()`), how to opt out (API-03's `nativeWindowManagement = false`), and the new JNA 5.19.1 `implementation`-scope dependency. Version bump to `v3.2.0` follows the identical `21-13`/`21-14` two-step pattern: `implementation("com.github.Tolaseeq:aero-compose-ui:v3.1.0")` → `:v3.2.0`, `> Package: ...` line's version references, `## Tech stack` untouched unless a listed library version actually changes.

---

### `build.gradle.kts` (root, version bump + tag)

**Analog:** `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/21-13-PLAN.md` (disposable verify tag) + `21-14-PLAN.md` (real tag, `checkpoint:human-action`).

**Task shape to copy verbatim** (from `21-14-PLAN.md` Task 2's `<action>`):
```
Change `version = "3.0.0"` to `version = "3.1.0"` in `build.gradle.kts` (only that line) and
commit `chore(release): 3.1.0`. `git tag -a v3.1.0 -m "aero-compose-ui 3.1.0"` on that commit.
`git push origin v3.1.0`.
```
For v3.2.0: `chore(release): 3.2.0`, tag `v3.2.0`, same disposable-verify-tag-first sequence (`21-13-PLAN.md` Task 1: `v3.2.0-verifyNN`, poll `https://jitpack.io/api/builds/com.github.Tolaseeq/aero-compose-ui/v3.2.0-verifyNN` until `status: ok`, `commit` SHA-matched, before the real tag). The real-tag step is a `checkpoint:human-action` gate exactly like `21-14-PLAN.md` Task 1 — only the maintainer authorizes moving remote master / pushing the real tag (D-10-equivalent for this phase, carried forward per CONTEXT.md's "Release follows the v3.1.0 pattern").

---

## Shared Patterns

### Public API additive-parameter discipline
**Source:** `AeroTitleBar.kt` lines 74-81, `AeroResizeHandles.kt` line 55, `ARCHITECTURE.md` §"Public API Shape"
**Apply to:** `AeroTitleBar.kt`, `AeroWindowChrome.kt` (new)
```kotlin
@Composable
public fun FrameWindowScope.AeroTitleBar(
    title: String,
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    nativeWindowManagement: Boolean = true,  // new — always last, always defaulted
): Unit
```
Every existing call site must keep compiling unmodified — this is the same discipline `ARCHITECTURE.md`'s own "Public API Shape" section states explicitly.

### `internal`, pure-JVM, one-file-per-responsibility package convention
**Source:** `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt`
**Apply to:** every file under `library/src/main/kotlin/com/mordred/aero/internal/windows/`
```kotlin
package com.mordred.aero.components.layout.internal.panelgroup
/** Pure-JVM distribution and clamp logic for AeroPanelGroup. No Compose imports... */
internal fun clampPanelDividerPx(...): Float { ... }
```
`internal` (module-scoped) visibility, no leakage into any public signature, KDoc citing the requirement ID and pitfall guarded against — mirrors this project's existing v3.0 "GlassModifiers.kt split" convention named in ARCHITECTURE.md.

### `implementation`-not-`api` scope for non-public dependencies
**Source:** `library/build.gradle.kts` line 38 (`// Internal only — not exposed in any public signature.`)
**Apply to:** `jna`/`jna-platform` entries in `library/build.gradle.kts`
```kotlin
// Internal only — not exposed in any public signature.
implementation(libs.kotlinx.coroutines.core)
```

### Locked test-count guard bump
**Source:** `library/build.gradle.kts` lines 1-8, commit `ffece58`
**Apply to:** the VER-14 commit that raises `lockedTestTotal`
```kotlin
val lockedTestTotal = 541
val lockedTestSkipped = 0
```
Commit message convention: `build(22-NN): raise locked test count to <N> — <reason> (VER-14)`.

### `-Paero.*` launch-property forwarding (dual JavaExec + ComposeHotRun blocks)
**Source:** `showcase/build.gradle.kts` lines 30-48
**Apply to:** any new `-Paero.secondWindow`-style flag for SHW-17
```kotlin
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
}
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
}
```
Add the new property to **both** blocks — known duplication debt (`21-REVIEW.md` IN-01), not this phase's to fix unless the new parameter needs it anyway.

### PrintWindow-only capture, no synthetic cursor input
**Source:** `tools/capture/AeroCapture.ps1` lines 17-100 (`PW_RENDERFULLCONTENT = 2`, `EnumWindows`/`FindTopLevelWindows`)
**Apply to:** any new `tools/`-based live-window harness or flyout detector
- `PrintWindow(hwnd, hdc, 2)` is the only sanctioned capture call project-wide.
- Enumerate by exact window title via `EnumWindows`, never assume a fixed HWND.

### Named-test-wired-to-requirement-ID KDoc convention
**Source:** `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt` lines 20-23
**Apply to:** all new `library/src/test/kotlin/com/mordred/aero/internal/windows/*Test.kt` files
```kotlin
/**
 * PNL-04: 3 equal expanded sections; distribute at totalPx=900 then 600;
 * each section's share ratio is preserved (~1/3 of availableForExpanded) within 0.5f.
 */
@Test
fun distributePxWindowResizePreservesRatios() { ... }
```
Substitute the matching `SNAP-*`/`WIN-*`/`VER-*` requirement ID.

### Verify-tag-before-real-tag release sequence
**Source:** `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/21-13-PLAN.md` + `21-14-PLAN.md`
**Apply to:** REL-08 (v3.2.0 release)
- Push a disposable `v3.2.0-verifyNN` tag first (never reuse a name), poll JitPack's build-status API until `status: ok` with a SHA-matched `commit`.
- Only then, under a `checkpoint:human-action` gate the executor cannot answer on the maintainer's behalf, bump `build.gradle.kts`'s `version`, tag the real `v3.2.0`, and push — per the maintainer's explicit choice (`hold` / `release-tag-only` / `release-full`).
- Never force-push, delete, or re-point any tag.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt` | controller (native message dispatch) | event-driven | No native `WindowProc`/Win32-callback code exists anywhere in this codebase today; follow `ARCHITECTURE.md` §"Architectural Patterns" Pattern 1 (`CallWindowProc` passthrough) verbatim, it is the authoritative shape |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt` | utility (hand-declared JNA extension interfaces) | request-response | No JNA usage exists anywhere in this codebase today (`grep` for `com.sun.jna` across the repo returns zero); follow `STACK.md` §2's exact interface declarations |
| `library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt` | service (HWND-keyed lifecycle registry) | event-driven | No singleton/lifecycle-registry pattern (install/uninstall keyed by an external handle, GC-safety concern) exists in `:library` today; follow `ARCHITECTURE.md`'s Component Responsibilities table and Anti-Pattern/pitfall list (JNA `Callback` GC pitfall) |
| `tools/<live-window harness>/*.kt` | tool (opt-in live-HWND probe, JVM `main()`) | event-driven | `tools/capture/` is PowerShell, not Kotlin/JVM, and probes pixels not Win32 messages; no existing Kotlin `main()` tool exists under `tools/` at all — follow `ARCHITECTURE.md`'s Testing Architecture "Needs a live window" section and CONTEXT.md's Claude's Discretion note (option (a): small Kotlin/JVM `main()`, not a new Gradle source set) |

## Conventions

Convention derivation was attempted via the shared deterministic module (`gsd-tools.cjs verify conventions --derive`), scoped first to `library/src/main/kotlin/com/mordred/aero` and then repo-wide with no scope. Both runs returned:
```json
{"mode":"derive","skipped":true,"reason":"no-readable-files","axes":[]}
```
Convention derivation skipped (the shared module's file-name/identifier/export/import axis rules apply only to JS/TS source files — `bin/lib/conventions.cjs` line 46 restricts the idiom rule packs to "The file extensions the JS/TS idiom rule packs apply to"; this repository is 100% Kotlin/Gradle-DSL under `library/`/`showcase/`, so no file in scope was readable by that module). No 4-axis table is produced.

In its place, the conventions actually observed first-hand while reading this codebase this session (informal, not derived by the shared tool):
- **Kotlin `internal` visibility, one-file-per-responsibility packages** — `components/layout/internal/panelgroup/PanelDistribution.kt` is the load-bearing precedent named directly in `ARCHITECTURE.md` ("mirrors `GlassModifiers.kt`'s split from v3.0"); the new `internal/windows/` package must follow the same shape.
- **`api` vs `implementation` dependency scoping** — anything appearing in a public Compose signature is `api`; anything internal-only (currently only `kotlinx-coroutines-core`) is `implementation` with an explicit `// Internal only — not exposed in any public signature.` comment. JNA/`jna-platform` must follow the `implementation` branch.
- **KDoc cites the requirement ID it satisfies and the pitfall it guards** — every `PanelDistribution.kt` function and every `PanelGroupLogicTest.kt` test names a `PNL-*` ID inline; this phase's `internal/windows/` files and tests should cite `SNAP-*`/`WIN-*`/`BTN-*`/`VER-*` IDs the same way.
- **Contested hotspot (author's choice), not derivable by axis vote:** this repo has no CJS/ESM split (it is a single-language Kotlin/Gradle-DSL codebase), so the dual-resolver contested-hotspot pattern from JS/TS-oriented repos does not apply here. The nearest analogous "two internally-consistent-but-different local styles" split in this codebase is `library/` (`explicitApi()`, `public`/`internal` modifiers mandatory) vs `showcase/` (no `explicitApi()`, default visibility) — each module is internally consistent per its own `build.gradle.kts`, and planners/reviewers should match whichever module a new file lands in rather than importing the other module's visibility discipline.

## Metadata

**Analog search scope:** `library/src/main/kotlin/com/mordred/aero/components/navigation/`, `library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/`, `library/src/main/kotlin/com/mordred/aero/theme/`, `library/src/test/kotlin/com/mordred/aero/components/{navigation,layout}/`, `showcase/src/main/kotlin/com/mordred/showcase/`, `showcase/build.gradle.kts`, `library/build.gradle.kts`, `gradle/libs.versions.toml`, `README.md`, `tools/capture/`, `tools/verify/`, `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/`
**Files scanned:** 15 read in full or targeted (`AeroTitleBar.kt`, `ResizeHandles.kt`, `Main.kt`, `showcase/build.gradle.kts`, `library/build.gradle.kts`, `gradle/libs.versions.toml`, `GlassModifiers.kt`, `PanelDistribution.kt`, `AeroTitleBarTest.kt`, `PanelGroupLogicTest.kt`, `AeroCapture.ps1`, `check-material3.sh`, `README.md`, `21-13-PLAN.md`, `21-14-PLAN.md`)
**Pattern extraction date:** 2026-09-25
