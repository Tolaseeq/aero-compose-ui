# Stack Research

**Domain:** Native Windows window management (Aero Snap / Win11 Snap Layouts / hit-testing) for undecorated Compose Desktop windows
**Researched:** 2026-09-25
**Confidence:** MEDIUM-HIGH (core library facts HIGH/verified against Maven Central + JNA/GitHub source; the exact Win32-message recipe is MEDIUM — well-attested in prior art on Swing/AWT and in a native C reference, but not yet proven against `ComposeWindow`/Skiko specifically)

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| `net.java.dev.jna:jna` | **5.19.1** | Core JNI runtime — `Pointer`, `Native`, `Callback`, `Structure`, `Native.load()` | Latest release on Maven Central (confirmed via `maven-metadata.xml`, `lastUpdated 20260612`). Required transitively by `jna-platform`; must be pinned explicitly to the same version to avoid a stray transitive mismatch. Exactly matches the version the consumer app already uses — zero version-skew risk. |
| `net.java.dev.jna:jna-platform` | **5.19.1** | Pre-built Win32 bindings: `User32`, `WinDef`, `WinNT`, `WinUser`, `WinGDI` | Ships `HWND`/`RECT`/`POINT`/`MONITORINFO` types and most of `User32` (`SetWindowLongPtr`, `GetWindowLongPtr`, `CallWindowProc`, `SetWindowPos`, `MonitorFromWindow`, `GetMonitorInfo`, `GetSystemMetrics`) so you don't hand-roll the common 80%. Confirmed present via the live JNA GitHub source (`contrib/platform/src/com/sun/jna/platform/win32/User32.java`, `master` branch, 2026-09-25). |

**Not adding a version newer than 5.19.1** — it is the `<release>`/`<latest>` in Maven Central's own metadata for `net.java.dev.jna:jna-platform` as of 2026-06-12; no 6.x exists yet. — Confidence: **HIGH**. Source: `https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/maven-metadata.xml` (fetched live).

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| *(none — no additional Maven artifact)* | — | Custom `Dwmapi` JNA interface, hand-declared | `jna-platform` 5.19.1 ships **no `Dwmapi.java`** in `com.sun.jna.platform.win32` at all (confirmed: the package directory listing on the JNA `master` branch has no `Dwm*` file). If you want DWM to draw the drop-shadow/rounded-corner on the borderless window (`DwmExtendFrameIntoClientArea` with a 1px `MARGINS`, or `DwmSetWindowAttribute(DWMWA_WINDOW_CORNER_PREFERENCE)`), declare a small interface yourself (see snippet below). **Not required** to satisfy the milestone's acceptance criteria (snap, hit-test, Snap Layouts) — treat as optional polish, not core scope. |
| *(none — no additional Maven artifact)* | — | Custom `User32` extension interface | `GetDpiForWindow`, `GetSystemMetricsForDpi`, `TrackMouseEvent`, `ClientToScreen`, `AdjustWindowRectExForDpi` are **confirmed absent** from `jna-platform` 5.19.1's `User32.java` (checked against live GitHub source). This is normal for JNA — its own docs call the shipped `User32` "an incomplete implementation to support demos." The standard JNA pattern is to declare your own interface extending `com.sun.jna.platform.win32.User32` (or a bare `StdCallLibrary`) with just the extra methods, then `Native.load("user32", MyUser32.class, W32APIOptions.UNICODE_OPTIONS)` — JNA resolves the symbols from the already-loaded system DLL at runtime; no native compilation needed. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| None new | — | No native toolchain (no MSVC/CMake) needed. JNA/jna-platform are pure-JVM libraries that dlopen the *existing* `user32.dll`/`dwmapi.dll` shipped with Windows — unlike FlatLaf's approach (below), there is no custom `.dll` to compile, sign, or bundle. This is a meaningful advantage for a JitPack-published library. |

## Installation

```kotlin
// gradle/libs.versions.toml
[versions]
jna = "5.19.1"

[libraries]
jna = { module = "net.java.dev.jna:jna", version.ref = "jna" }
jna-platform = { module = "net.java.dev.jna:jna-platform", version.ref = "jna" }
```

```kotlin
// library/build.gradle.kts
dependencies {
    // Windows-only native window management (WM_NCHITTEST subclassing for Snap
    // Layouts / Aero Snap). Not part of any public function signature — kept as
    // `implementation` so consumers don't need JNA on their compile classpath,
    // matching the existing kotlinx-coroutines-core precedent in this file.
    implementation(libs.jna)
    implementation(libs.jna.platform)
}
```

No separate native-library extraction step is needed for JitPack: both jars bundle the native dispatch stub (`jnidispatch`) for all supported OS/arch pairs *inside the jar itself* (classic JNA packaging), and `jna-platform` contains only Java bindings, no natives of its own beyond what `jna` already carries. Total added classpath weight, measured directly against the published artifacts: **`jna-5.19.1.jar` = 1.86 MB, `jna-platform-5.19.1.jar` = 1.38 MB (≈3.2 MB combined)** — small relative to Compose Multiplatform's own footprint, and it does not inflate `aero-compose-ui`'s own jar (JNA stays a separate transitive dependency, nothing is shaded). — Confidence: **HIGH** (jar sizes measured directly via `HEAD` against `repo1.maven.org`).

## Requirement-by-Requirement Findings

### 1. JNA / jna-platform — license, scope, JPMS/Java 21 concerns

- **License:** JNA and jna-platform are dual-licensed **LGPL 2.1 (or later) / Apache License 2.0** — the consumer picks either. Apache-2.0 is fully compatible with this library's own Apache-2.0 `pom.xml` license declaration. — Confidence: **HIGH**. Source: `https://github.com/java-native-access/jna/blob/master/LICENSE`.
- **`api` vs `implementation`:** Use `implementation`. `AeroTitleBar`/`AeroResizeHandles`'s public Kotlin signatures should never expose `com.sun.jna.*` or `com.sun.jna.platform.win32.*` types (`HWND`, `Pointer`, etc.) — the native subclassing is an internal implementation detail triggered from inside the composables (e.g. a `DisposableEffect` that installs/uninstalls a `WindowProc` on `window`). This mirrors how `kotlinx-coroutines-core` is already `implementation`-scoped in `library/build.gradle.kts` for the same reason (internal-only, not in any public signature). Gradle's `implementation` still publishes as a runtime-scope dependency in the POM (and in Gradle Module Metadata), so JitPack/Gradle consumers still resolve it transitively at runtime automatically — they simply don't get it polluting their own compile classpath.
- **JPMS / `--enable-native-access` on Java 21:** JNA has used classic **JNI** (not the newer `java.lang.foreign` Panama/FFM API) through at least the 5.19.x line. The `--enable-native-access=ALL-UNNAMED` restricted-method warning mechanism (JEP 454, java.lang.foreign) applies to FFM-API callers, and the broader "warn/restrict on any native access including JNI" behavior is **JEP 472, targeted at JDK 24**, not JDK 21 — a live JNA GitHub issue is literally titled *"Warning about use of JNI in JDK 24"* (`java-native-access/jna#1665`), confirming the warning is a JDK-24-era concern. **On JDK 21 (this project's pinned toolchain), no `--enable-native-access` warning is expected.** Flag this as a watch-item only: since this project has a track record of aggressively chasing the latest stable JDK (17→21 in v3.1), a future JDK bump past 24 would need `--enable-native-access=ALL-UNNAMED` on the `:showcase` run task (and documented for consumers running JDK 24+) or a JNA major-version bump that migrates to FFM. — Confidence: **MEDIUM** (verified the JEP timeline and the JNA issue exists confirming the JDK-24 framing; did not find an explicit "JNA is JNI-only through 5.19.1, no FFM opt-in" statement in an official JNA release note for this exact version).
- **Native-library extraction impact on consumers:** none beyond the ~3.2 MB combined jar weight noted above — no unpacking to a temp directory is required at build time (JNA extracts its native stub lazily at first `Native.load()` call, into `java.io.tmpdir`, same as it always has on any JVM app using JNA; this is unchanged runtime behavior, not a JitPack-specific concern).

### 2. Exact jna-platform types needed, and what must be hand-declared

**Present in `jna-platform` 5.19.1** (verified against the live `User32.java` source on JNA's `master` branch):

```java
// com.sun.jna.platform.win32.User32 (interface User32 extends StdCallLibrary, WinUser, WinNT)
Pointer SetWindowLongPtr(HWND hWnd, int nIndex, Pointer dwNewLongPtr);
LONG_PTR GetWindowLongPtr(HWND hWnd, int nIndex);
LRESULT CallWindowProc(Pointer lpPrevWndFunc, HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);
boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
boolean GetWindowRect(HWND hWnd, RECT rect);
boolean GetClientRect(HWND hWnd, RECT rect);
HMONITOR MonitorFromWindow(HWND hwnd, int dwFlags);
boolean GetMonitorInfo(HMONITOR hMonitor, MONITORINFO lpmi);      // + MONITORINFOEX overload
int GetSystemMetrics(int nIndex);
```

Plus the callback type, `WinUser.WindowProc` (an `interface WindowProc extends StdCallCallback { LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam); }`), and the `WM_*`/`HT*`/`GWL_WNDPROC`/`SW_*` integer constants declared on `WinUser`. `MONITORINFO` (struct with `rcMonitor`/`rcWork`) is what you need for "maximize fills the work area, never covers the taskbar" — it's present and ready to use.

**Confirmed absent from `jna-platform` 5.19.1** — must be hand-declared by this project (standard JNA extension pattern, no native compilation involved):

```kotlin
// Not in jna-platform's User32 — declare your own extension interface.
internal interface AeroUser32Ext : User32 {
    companion object {
        val INSTANCE: AeroUser32Ext = Native.load("user32", AeroUser32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
    fun GetDpiForWindow(hWnd: HWND): Int                                    // Win10 1607+
    fun GetSystemMetricsForDpi(nIndex: Int, dpi: Int): Int                  // Win10 1607+
    fun AdjustWindowRectExForDpi(rect: RECT, style: Int, menu: Boolean, exStyle: Int, dpi: Int): Boolean
    fun TrackMouseEvent(lpEventTrack: TRACKMOUSEEVENT): Boolean
    fun ClientToScreen(hWnd: HWND, lpPoint: POINT): Boolean
}
```

`GetDpiForWindow`/`GetSystemMetricsForDpi` are needed so the `WM_NCCALCSIZE` frame-margin math (resize-border thickness, caption height) is computed per-monitor-DPI — this is exactly what the acceptance criterion "moving between monitors with different scaling (100%/150%) keeps sane sizes" requires; hard-coded 96-DPI metrics would visibly misbehave at 150%. `TRACKMOUSEEVENT`/`POINT`/`RECT` structs themselves **are** already defined in `jna-platform`'s `WinUser`/`WinDef` — only the `User32` method entry points listed above are missing.

`Dwmapi` (optional, for shadow/rounded corners only) — also hand-declared, since `jna-platform` ships no `Dwmapi.java` at all:

```kotlin
internal interface Dwmapi : StdCallLibrary {
    companion object {
        val INSTANCE: Dwmapi = Native.load("dwmapi", Dwmapi::class.java)
    }
    fun DwmExtendFrameIntoClientArea(hWnd: HWND, pMarInset: MARGINS): Int   // HRESULT
    fun DwmSetWindowAttribute(hWnd: HWND, dwAttribute: Int, pvAttribute: Pointer, cbAttribute: Int): Int
}
// MARGINS is a plain 4-int Structure (cxLeftWidth, cxRightWidth, cyTopHeight, cyBottomHeight) — not in jna-platform, declare it too.
```

— Confidence: **HIGH** for what's present/absent (verified against live JNA source on 2026-09-25); **MEDIUM** for the exact DPI-handling recipe in `WM_NCCALCSIZE` (pattern is well established in FlatLaf's native code, see §4, but not yet exercised against this codebase).

### 3. Obtaining the HWND from `FrameWindowScope.window` on CMP 1.12

`ComposeWindow` (the type of `FrameWindowScope.window`) is a `javax.swing.JFrame` subclass (confirmed: `androidx/compose/desktop/ComposeWindow.desktop.kt` in JetBrains' `androidx` fork declares `class ComposeWindow : JFrame`). Since `JFrame extends Frame extends Window extends Container extends Component`, both of JNA's AWT/Swing bridge utilities apply:

```java
// com.sun.jna.Native (verified against JNA 5.19.1 javadoc)
public static Pointer getWindowPointer(java.awt.Window w) throws HeadlessException
public static Pointer getComponentPointer(java.awt.Component c) throws HeadlessException
```

Either works for a `ComposeWindow` (it is simultaneously a `Window` and a `Component`); wrap the result: `val hwnd = WinDef.HWND(Native.getComponentPointer(window))`.

**When it becomes valid:** both methods throw `HeadlessException` if the JVM is headless, and — per Swing/AWT semantics generally, not something JNA documents — the underlying native peer (and therefore a non-null pointer) only exists once the `Window` is **displayable** (`Component.isDisplayable()` true, i.e. `addNotify()`/peer creation has run, which normally happens as part of `setVisible(true)`/`pack()`). Compose Desktop's `Window(...)` entry point creates and shows the AWT peer as part of bringing the window up before your `@Composable` content body starts recomposing, so in practice `window.isDisplayable` is already `true` by the time `FrameWindowScope.AeroTitleBar`/`AeroResizeHandles` run. **Guard defensively anyway**: install the `WindowProc` subclass inside a `DisposableEffect(Unit)` that checks `window.isDisplayable`, and — since `Window(visible = false, ...)` is a documented, valid Compose Desktop call pattern the consumer app might use — also register via `window.addComponentListener`/`addHierarchyListener` (`HierarchyEvent.SHOWING_CHANGED`) as a fallback so installation still happens if content composes before the peer exists. Uninstall the subclass (restore the original `WNDPROC` via `SetWindowLongPtr` with the saved pointer) in the effect's `onDispose`, mirroring `WM_DESTROY` cleanup ordering FlatLaf uses (see §4) — skipping this leaves a dangling native callback pointer if the JVM GCs the `Callback` object while Windows can still call it. — Confidence: **MEDIUM** (the `ComposeWindow extends JFrame` fact and the JNA methods are HIGH-confidence/verified; the exact "is it displayable by the time FrameWindowScope content runs" claim is inferred from Compose Desktop's documented `Window()` lifecycle, not verified by reading the `Window.desktop.kt` source directly in this pass — validate empirically in the first implementation spike).

### 4. Prior art on standard JDKs (non-JBR)

| Project | Approach | Standard-JDK? | License | What to learn / copy (with attribution) |
|---|---|---|---|---|
| **FlatLaf** — `flatlaf-natives-windows` (`FlatWindowsNativeWindowBorder.java` + `FlatWndProc.cpp`) | **JNI + a custom compiled `.dll`** (not JNA) subclassing the Swing/AWT `JFrame`'s `WNDPROC` via `SetWindowLongPtr`. This is the most complete, most battle-tested (millions of installs) standard-JDK reference. | **Yes** — pure Swing/AWT, runs on any OpenJDK/Oracle JDK, no JBR dependency (confirmed by reading the actual `.cpp` source). | **Apache-2.0** (verified via GitHub API `license` field) — attribution-friendly to study and structurally imitate. | Concrete, verified-by-reading-the-source message list to replicate with our own `WindowProc`: <br>• **`WM_NCCALCSIZE`** — call the default proc first, then trim frame margins (resize-border thickness only, via DPI-aware metrics) so the OS title bar disappears but the resize border and Windows 11 auto-hide-taskbar edge reduction still work. <br>• **`WM_NCHITTEST`** — call default proc first (frame edges "for free"), then delegate the caption/button region to app logic; return `HTMAXBUTTON` over the maximize button (**this is what makes the Snap Layouts flyout appear on hover** — confirmed as the documented Windows 11 contract, not just this project's assumption). <br>• **`WM_NCMOUSEMOVE` / `WM_NCLBUTTONDOWN` / `WM_NCLBUTTONUP`** — forward these into the client area so the Swing (here: Compose) button underneath the `HTMAXBUTTON`/`HTCAPTION` region still receives its own hover/press/click. **This is the concrete answer to the "known gotcha" flagged in the milestone brief**: once a region reports `HTMAXBUTTON`/`HTCAPTION`, Windows stops delivering ordinary client (`WM_MOUSEMOVE`/`WM_LBUTTONDOWN`) messages there, and FlatLaf's fix is exactly to re-inject those non-client messages back into the client message stream rather than trying to read hover state from Compose's own pointer-input system in that region. <br>• **`WM_NCRBUTTONUP`** over the caption → opens the system menu (answers the milestone's "Alt+Space system menu" bullet's mouse-driven cousin; Alt+Space itself is `WM_SYSCOMMAND`/`SC_KEYMENU`, handled by leaving `WS_SYSMENU` semantics to the default proc). <br>• **`WM_DPICHANGED`** — call default proc, then manually re-fire a resize/relayout for maximized windows because Windows sometimes omits the automatic one. <br>• Preserves `WS_THICKFRAME`/`WS_MAXIMIZEBOX`/`WS_MINIMIZEBOX` — queries them, never strips them (matches the milestone's explicit instruction). <br>• Uses `GetDpiForWindow`/`GetSystemMetricsForDpi`/`MonitorFromWindow(MONITOR_DEFAULTTONEAREST)` for the multi-monitor/DPI acceptance criteria. — Confidence: **HIGH**, read directly from the live `FlatWndProc.cpp` source. |
| **grassator/win32-window-custom-titlebar** | Minimal, from-scratch **C** reference (not a library, not JNA) — the canonical "here is the whole recipe in ~300 lines" writeup that most blog posts (Tauri, Electron, Rust `winit`) cite when explaining `HTMAXBUTTON`. | N/A (not JVM at all) | **MIT** (verified via GitHub API) | Confirms the same `WM_NCCALCSIZE` (trim to 0 client-area outset while manually reapplying resize-border padding, plus one extra pixel of top padding on maximize) + `WM_NCHITTEST` (default-proc-first for edges, then app-owned `HTCAPTION`/`HTMAXBUTTON` regions) shape as FlatLaf, from an independent, much smaller codebase — good for cross-checking FlatLaf's approach or for writing test fixtures/spikes in raw C before porting to Kotlin+JNA. MIT license permits copying with attribution; do so explicitly in code comments if any snippet is lifted near-verbatim. |
| **Konyaco/compose-fluent-ui** (`compose-fluent-ui`, org now `compose-fluent`) | Fluent Design **Compose Multiplatform** library; its README credits list **JNA** as the mechanism used "to interact with Win32 APIs, enabling title bar customization," and separately credits the MIT-licensed `win32-window-custom-titlebar` repo above as an inspiration for that work. | Likely yes (it's a Compose Desktop library targeting normal JVMs, same as this project) — **not independently verified by reading its Kotlin source in this pass**. | **Apache-2.0** (verified via GitHub API) | This is the closest known prior art to "JNA + Compose Desktop + Windows titlebar," and is Apache-2.0 (freely attributable). **Flag as an open follow-up**: before/during the implementation phase, clone the repo and read the actual Kotlin+JNA title-bar code (likely under a `fluent-desktop` or `gallery` module) to see their concrete `WindowProc` Kotlin idiom — this pass could not pin the exact file path or confirm `HTMAXBUTTON`/Snap-Layouts support specifically. — Confidence: **LOW-MEDIUM** (existence and Apache-2.0 license are HIGH-confidence/API-verified; the depth/correctness of their Snap-Layouts handling is unverified). |
| **MayakaApps/ComposeWindowStyler** | Compose Desktop library for DWM **backdrop effects only** (Mica/Acrylic/Aero blur via `DwmSetWindowAttribute`/similar), Windows 11 21H2+. | Yes — plain Compose Desktop, standard JDK. | **MIT** (verified via GitHub API) | **Not relevant to hit-testing or Snap Layouts** — it does not implement `WM_NCHITTEST`/`HTMAXBUTTON` at all; it is cosmetic backdrop styling only. Worth a quick read purely for its "how do I get an `HWND` out of a `ComposeWindow` in idiomatic Kotlin+JNA" utility code (same sub-problem as §3), but do not look to it for the snap/hit-test logic itself. — Confidence: **MEDIUM** (scope-negative claim based on a single fetched summary of the README, not a full source read). |

### 5. JBR's custom title bar / Jewel `DecoratedWindow` — lessons only (not usable on standard JDK)

- **JBR mechanism:** `com.jetbrains.JBR.getWindowDecorations().createCustomTitleBar()` returns a `com.jetbrains.WindowDecorations.CustomTitleBar`. Jewel's `decorated-window` module (`TitleBar.Windows.kt`) wraps this: it exposes `leftInset`/`rightInset` (regions Compose should treat as "occupied by native controls") and a `forceHitTest()`-style call that pushes hit-test-relevant geometry down into JBR's native layer. — Confidence: **HIGH** for "this is JBR-only" (the class is literally `com.jetbrains.JBR`, unavailable on any non-JetBrains-Runtime OpenJDK build — will not resolve/`ClassNotFoundException` on a standard JDK 21) — this directly confirms the milestone brief's constraint that the library must work "on a standard JDK 21 WITHOUT JBR," i.e. **Jewel/JBR's actual classes cannot be a dependency here at all**, only a design reference.
- **Lesson worth copying conceptually:** JBR's model separates "which screen-space regions are non-client (title bar / controls)" from "how those regions render" — Compose owns rendering (the button's pixels), the native layer owns only the hit-test *geometry* for those same pixel regions, kept in sync every time layout changes (e.g. via `onGloballyPositioned` reporting bounds up to the native side). This is the same shape as FlatLaf's approach in §4 and is the right mental model for `AeroTitleBar`'s three buttons and the drag region: track each button's `Rect` in window-local coordinates via `Modifier.onGloballyPositioned`, hand those bounds to the `WindowProc`'s `WM_NCHITTEST` handler (a simple mutable holder object read from the callback thread), and return `HTMAXBUTTON`/`HTCAPTION`/`HTCLIENT` accordingly.
- **Lesson on the "loses client mouse events" gotcha:** neither the JBR/Jewel summary nor further digging surfaced JBR's *specific* mitigation for hover state on the `HTMAXBUTTON`-covered maximize button (Jewel's public API abstracts this away entirely). Treat FlatLaf's `WM_NCMOUSEMOVE`/`WM_NCLBUTTONDOWN`/`UP`-forwarding recipe (§4) as the concrete, verified answer to copy — it is the same underlying Win32 problem, solved on a standard JDK, with source available. — Confidence: **MEDIUM** (JBR class names and non-portability are HIGH; the internal JBR native-side hit-test-loss mitigation is unverified/opaque by design — it's closed-source at the JBR native layer).

## Constraint Check: does `undecorated = true` (required, per CMP-3757) conflict with this approach?

**No conflict found — report per the milestone brief's instruction, stated explicitly since this was a named risk.** Every standard-JDK prior-art example in §4 (FlatLaf, the C reference) operates on exactly this shape of window: **undecorated, non-transparent, resizable**, with the native title bar/border removed via `WM_NCCALCSIZE` trimming rather than via window transparency or layering. `transparent = true` (which crashes on Win11 per CMP-3757) is an unrelated, orthogonal Compose/Skiko concept (alpha-blended surface compositing) from `undecorated = true` (an AWT/Win32 style-bit concept, `WS_CAPTION`/`WS_SYSMENU`/`WS_THICKFRAME`). None of the WM_NCHITTEST/HTMAXBUTTON/Snap-Layouts machinery in §4 requires `WS_EX_LAYERED` or per-pixel window transparency — DWM will still draw a drop shadow and rounded corners around a plain opaque undecorated resizable window on Windows 11 as long as it keeps `WS_THICKFRAME` and (optionally) uses `DwmExtendFrameIntoClientArea` with a 1px margin (the `Dwmapi` addition in §2, optional polish). — Confidence: **MEDIUM-HIGH** (the architectural non-conflict is well-supported by reading FlatLaf's actual native code against an undecorated-equivalent AWT `Frame`; not yet empirically confirmed against `ComposeWindow`/Skiko's specific AWT peer implementation, which is the first thing to spike).

**One real gap to flag for phase planning, not a stack gap:** `Alt+Space` opening the system menu is **not** something `WM_NCHITTEST`/`HTMAXBUTTON` handling gives you "for free." It requires its own explicit handling — typically intercepting `WM_SYSCOMMAND`/`SC_KEYMENU` (or `WM_NCRBUTTONUP` over the caption, which FlatLaf does handle) and calling `GetSystemMenu(hwnd, FALSE)` + `TrackPopupMenu`. `GetSystemMenu`/`TrackPopupMenu` are **also absent from `jna-platform`'s `User32`** (not checked exhaustively in this pass — assume hand-declaration needed, same pattern as §2) — Confidence: **LOW-MEDIUM** on the exact `jna-platform` coverage for these two specific calls; re-verify in the implementation phase before writing the phase plan's task list.

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `androidx.compose.foundation.window.WindowDraggableArea`-only approach (current code) | Never reports `HTCAPTION` to the OS — confirmed root cause of the "Aero Snap limitation" already documented in this codebase's own `AeroTitleBar.kt` KDoc; Compose just repositions the window on drag, bypassing the OS move-loop entirely, which is also why FancyZones/PowerToys and Snap Groups don't see the drag. | The JNA `WM_NCHITTEST` subclass in this document — return `HTCAPTION` from the real Win32 hit-test so Windows' own move-loop drives the drag. `WindowDraggableArea` can stay as a Linux/macOS fallback path (see below). |
| Writing a custom native `.dll` (FlatLaf's own approach) | Requires a C/C++ toolchain, per-arch compilation (x86/x64/arm64), code signing considerations, and a JitPack build step to produce/bundle native binaries — significant build-system burden for a JAR-only, JitPack-published library that has never needed native compilation before. | JNA — same Win32 surface, zero native compilation, ships as ordinary jars JitPack already knows how to build. |
| JBR-specific `com.jetbrains.JBR`/Jewel `DecoratedWindow` | Explicitly ruled out by the milestone brief ("must run on a standard JDK 21 WITHOUT JBR") and confirmed here to be JBR-only at the class-loading level — would `ClassNotFoundException` for every consumer not running the showcase's Hot Reload JBR. | Read for design lessons only (§5); implement with JNA. |
| `transparent = true` on `Window(...)` | Causes `EXCEPTION_ACCESS_VIOLATION` on Windows 11 per the project's own tracked issue (CMP-3757/GH#3171) — already a hard constraint, restated here because it's tempting to reach for when trying to get DWM shadows/rounded corners; don't. | `DwmExtendFrameIntoClientArea` with a 1px `MARGINS` on an **opaque** (`transparent = false`) window achieves the shadow without triggering the crash. |

## Stack Patterns by Variant

**If the target platform is not Windows (Linux/macOS, per PROJECT.md "secondary" compatibility):**
- Skip installing the `WindowProc` subclass entirely — guard all new code behind `System.getProperty("os.name").startsWith("Windows")` (or `com.sun.jna.Platform.isWindows()`, already available once `jna` is a dependency).
- Fall back to today's `WindowDraggableArea` + `AeroResizeHandles` behavior unchanged — this satisfies the milestone constraint "non-Windows falls back to today's behavior" with a single top-level `if`, no separate code path needed elsewhere.

**If a consumer creates several `AeroTitleBar`/`AeroResizeHandles` windows in one process (the stated "main window + narrow ~300px second window" case):**
- The `WindowProc` subclass must be installed **per-HWND**, not once globally — each `ComposeWindow` gets its own `SetWindowLongPtr`/original-proc-pointer pair, stored keyed by `HWND` (or simply captured in the per-window `DisposableEffect`'s closure — no global registry needed, since each window's effect owns its own install/uninstall lifecycle independently).

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `net.java.dev.jna:jna:5.19.1` | `net.java.dev.jna:jna-platform:5.19.1` | Always pin both to the identical version — `jna-platform` depends on the exact matching `jna` core version; mismatches are a common source of `NoSuchMethodError`/`UnsatisfiedLinkError` in JNA-using projects generally. |
| `jna-platform:5.19.1` | Java 21 (`jvmToolchain(21)`) | JNI-based, no FFM/Panama dependency at this version — no `--enable-native-access` warning expected on JDK 21 (see §1). Re-verify if/when the toolchain moves past JDK 24. |
| `jna-platform:5.19.1` | JetBrains Runtime 21 (used by `:showcase`'s `hotRun`) | JBR is a standard OpenJDK-ABI-compatible build for JNI purposes — JNA's native dispatch loading is unaffected by JBR vs. stock OpenJDK; no special-casing expected, but confirm empirically in the first Hot Reload spike since JBR is also where the milestone's dual "JBR for showcase / standard JDK for consumers" requirement is most likely to surface a difference if one exists. |
| Kotlin 2.4.20 / Compose Multiplatform 1.12.0 / `explicitApi()` | JNA types kept fully `internal` | `explicitApi()` forces every public declaration to have an explicit visibility+type; since no public `AeroTitleBar`/`AeroResizeHandles` signature should reference `com.sun.jna.*` types (see §1's `api`-vs-`implementation` reasoning), this is a non-issue as long as all JNA-touching code (the `WindowProc` implementation, the extension `User32`/`Dwmapi` interfaces) is declared `internal`. |

## Sources

- `https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/maven-metadata.xml` — authoritative latest-version confirmation (5.19.1), fetched live 2026-09-25. **HIGH**.
- `https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/User32.java` (master branch) — live source read to confirm exact method presence/absence and signatures. **HIGH**.
- `https://github.com/java-native-access/jna/blob/master/LICENSE` — dual LGPL-2.1/Apache-2.0 confirmation. **HIGH**.
- `https://java-native-access.github.io/jna/5.19.1/javadoc/com/sun/jna/Native.html` — `getWindowPointer`/`getComponentPointer`/`getWindowID`/`getComponentID` signatures, version-pinned javadoc. **HIGH**.
- `https://github.com/JFormDesigner/FlatLaf` (Apache-2.0, confirmed via GitHub API `license` field) + live-read `flatlaf-natives/flatlaf-natives-windows/src/main/cpp/FlatWndProc.cpp` — the concrete, verified WM_NCCALCSIZE/WM_NCHITTEST/HTMAXBUTTON/NC-mouse-forwarding recipe on a standard JDK. **HIGH** for what's read; this is the strongest single source in this research.
- `https://github.com/grassator/win32-window-custom-titlebar` (MIT, confirmed via GitHub API) — independent cross-check of the same WM_NCCALCSIZE/WM_NCHITTEST shape in minimal C. **MEDIUM-HIGH**.
- `https://github.com/Konyaco/compose-fluent-ui` (Apache-2.0, confirmed via GitHub API) — JNA-based Win32 title-bar customization exists per README credits; exact implementation unread in this pass. **LOW-MEDIUM**.
- `https://github.com/MayakaApps/ComposeWindowStyler` (MIT, confirmed via GitHub API) — DWM backdrop styling only, not hit-testing; scope-negative finding from a single README summary. **MEDIUM**.
- `https://github.com/JetBrains/jewel/blob/main/decorated-window/src/main/kotlin/org/jetbrains/jewel/window/TitleBar.Windows.kt` — JBR-only `CustomTitleBar` dependency confirmed; internal hit-test-loss mitigation opaque/unread (closed at the JBR native layer). **MEDIUM**.
- `https://github.com/JetBrains/compose-multiplatform/issues/1248` + `https://api.github.com/repos/JetBrains/compose-multiplatform/issues/1248/comments` — confirms this is a known, still-open community request (tracked onward at YouTrack `CMP-6039`), with no built-in Compose Multiplatform solution shipped as of this research pass. **HIGH** (primary GitHub/API source, directly read).
- `https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-snap-layout-menu` — Microsoft's own documented contract that `WM_NCHITTEST` must answer `HTMAXBUTTON` for Snap Layouts to appear. **HIGH** (official Microsoft Learn source, cited by multiple independent search results).
- `https://openjdk.org/jeps/472` / `https://github.com/java-native-access/jna/issues/1665` — JDK 24 (not 21) as the point where JNI-native-access warnings begin. **MEDIUM** (timeline corroborated by two independent sources, not read as raw JEP text in full in this pass).

---
*Stack research for: native Windows window management (Aero Snap / Snap Layouts / hit-testing) on undecorated Compose Desktop windows*
*Researched: 2026-09-25*
