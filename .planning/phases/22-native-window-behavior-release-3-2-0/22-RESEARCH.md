# Phase 22: Native Window Behavior + Release 3.2.0 - Research

**Researched:** 2026-09-25
**Domain:** Win32 native window-management integration (JNA-based `WM_NCHITTEST`/`WM_NCCALCSIZE` subclassing) grafted onto this repository's existing `AeroTitleBar`/`AeroResizeHandles`, plus the release plumbing to ship `v3.2.0`
**Confidence:** MEDIUM-HIGH (milestone-level Win32/JNA facts) / HIGH (everything below that is first-hand-read from this repo or measured on this machine today)

## Summary

The milestone-level desk research (`.planning/research/{SUMMARY,STACK,ARCHITECTURE,PITFALLS,FEATURES}.md`) already answers "what is the Win32 recipe" in detail, cross-corroborated across four independent passes, and explicitly defers seven implementation specifics to a first-hand spike during execution rather than more research. This document does **not** repeat that recipe — it exists to close the gap between that recipe and an executable plan: exact current file paths/signatures in this codebase, the exact build/test/release mechanics the plan must hook into, what verification tooling exists today versus what must be built, and what is actually installed on this machine right now (checked, not assumed).

Three load-bearing facts, all read directly from the current source, not inferred: (1) `AeroTitleBar.kt` wraps its content in `WindowDraggableArea` (line 83) and its KDoc already documents the "Aero Snap limitation" this phase removes (lines 63-66) — this is the literal string REL-07 requires deleted. (2) `AeroResizeHandles.kt` is a flat `Box(Modifier.fillMaxSize())` with 8 `pointerInput`+`detectDragGestures` zones, gated only on `windowState.placement != Floating` (line 56) — WIN-02 requires this become a no-op on Windows once native hit-testing owns the same pixels, with the non-Windows path (Linux/macOS) untouched. (3) The showcase (`Main.kt`) already opens exactly one `undecorated=true, transparent=false` window and already threads `-Daero.*` launch parameters through both `:showcase:run` and Compose Hot Reload's `ComposeHotRun` task type — the natural place to add a second, narrow (~300px) window for SHW-17/WIN-06 is inside this same `application { }` block, following the existing parameter-forwarding pattern in `showcase/build.gradle.kts`.

On tooling: this machine has exactly one monitor (1920×1080 @ 100% DPI, confirmed via `System.Windows.Forms.Screen.AllScreens`), no PowerToys installed (confirmed via registry uninstall-key query), taskbar auto-hide is genuinely ON (confirmed via `StuckRects3\Settings` byte 8 = `0x03`), Windows 11 24H2 build 26100 is confirmed, and JNA 5.19.1 (both `jna` and `jna-platform`) is already present in this machine's Gradle cache — meaning a prior build on this machine (almost certainly Pinya's) already resolved it successfully, which is strong practical corroboration on top of the milestone research's live Maven Central fetch. Neither `com.jetbrains.JBR` nor `CustomWindowDecoration` appears anywhere in this repo (`grep` returned zero matches) — conflict #7's cheapest check is already answered: JBR's own decoration machinery is not engaged today, so there is nothing to disable, only the Hot Reload double-install idempotency guard to build.

**Primary recommendation:** Plan the phase exactly along ROADMAP.md's already-agreed 10-step execution order (it is the correct, cross-corroborated ordering — do not re-derive it), but ground every step's task list in the file paths, build hooks, and tooling facts below rather than re-deriving Win32 mechanics, which the four milestone research files already cover in depth.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Window style bits (`WS_CAPTION`/`WS_THICKFRAME`/etc.) | OS / Win32 (native) | Client (JNA call from JVM) | The style bits live in the HWND's kernel-object state; the library only issues the `SetWindowLongPtr` call that sets them |
| Hit-testing (`WM_NCHITTEST` → `HTCAPTION`/`HTMAXBUTTON`/resize codes/`HTCLIENT`) | Client (JNA WndProc subclass, `internal/windows/`) | OS (DWM consumes the answer) | The library owns the *answer*; Windows owns everything that happens once it has that answer (drag loop, Snap Layouts flyout, preview) |
| Region geometry publishing (title bar, buttons, `leading`/interactive regions) | Client (Compose UI layer, `AeroTitleBar`) | Client (cross-thread bridge, `HitTestRegionRegistry`) | Compose owns layout; the bridge only republishes an immutable snapshot for the native thread to read |
| Frame removal / maximize geometry (`WM_NCCALCSIZE`, `WM_GETMINMAXINFO`) | Client (JNA subclass) | OS (DWM/Shell supplies monitor work-area data) | The library computes the inset; the monitor work-area numbers themselves come from `GetMonitorInfo`, owned by the OS |
| Maximize-button hover/press visuals | Client (Compose, `TitleBarButton`) | Client (native → EDT bridge, `AeroMaxButtonInteraction`) | Rendering stays 100% Compose; only the *trigger* for hover/press state crosses from the native thread |
| Snap Layouts flyout, Snap Assist, Snap Groups, FancyZones, Aero Shake, Win+Arrow | OS / Shell (DWM) | — | Entirely OS-owned once hit-testing is correct; the library has zero code responsibility here, only a verification responsibility |
| `WindowState.placement`/`size`/`position` sync | Client (Compose Multiplatform's own `WindowStateListener`, unmodified) | Client (explicit push from `AeroWndProc` only for OS-native transitions, if conflict #4 proves it's needed) | Existing CMP pipeline is authoritative for CMP-originated transitions; the open question is only whether OS-originated transitions (native Snap/maximize) also reach it "for free" via `CallWindowProc` forwarding |
| Test-count guard, release tagging, JitPack build | Build / Release tooling (`library/build.gradle.kts`, git tags) | — | Pure project-tooling concern, no Win32 involvement |
| Showcase second window, capture/proof tooling | Client (showcase module `:showcase`) + Dev tooling (`tools/capture/`) | — | Verification surface, not shipped in the published artifact |

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SNAP-01 | Edge/corner drag-to-snap + drag-away restore | `ARCHITECTURE.md` Pattern 1/2 (hit-test bridge); milestone `FEATURES.md` row #1. Codebase: replaces `WindowDraggableArea` in `AeroTitleBar.kt:83` |
| SNAP-02 | Snap Layouts flyout on maximize-button hover (Win11) | `STACK.md`/`ARCHITECTURE.md` Pattern 3 (`HTMAXBUTTON`); conflict #5 (interaction-bridge design choice) must resolve first |
| SNAP-03 | Win+Arrow / Win+Up / Win+Down | `FEATURES.md` row #3 — automatic once styles+hit-test are correct, not gated on `HTMAXBUTTON` at all |
| SNAP-04 | Double-click title bar maximize/restore | `FEATURES.md` row #4 — automatic once `WM_NCLBUTTONDBLCLK` reaches `DefWindowProc`; must not be swallowed by any Compose double-tap gesture (none currently registered on the title Row) |
| SNAP-05 | Alt+Space system menu, "close" = `onCloseRequest` path | Conflict #2 (automatic vs. explicit `GetSystemMenu`/`TrackPopupMenu`); `PITFALLS.md` Pitfall 20 (Move/Size verification) |
| SNAP-06 | Shared-border resize of a snapped pair | `FEATURES.md` row #6 — Win11-only, automatic once SNAP-01/03 work on both windows; relevant once SHW-17's second window exists |
| SNAP-07 | FancyZones pickup | `FEATURES.md` row #11 — automatic once SNAP-01 works; **cannot be verified on this machine today** (PowerToys not installed, confirmed below) |
| WIN-01 | Maximized window = monitor work area, auto-hide taskbar still reveals | `PITFALLS.md` Pitfalls 8/9; this machine's auto-hide is genuinely ON (confirmed below) — the dev-machine repro condition PITFALLS.md assumed is real |
| WIN-02 | Native resize via OS; `AeroResizeHandles` Windows no-op; non-Windows unchanged | Codebase: `ResizeHandles.kt` full source read below — exact gating point identified |
| WIN-03 | No frame artifacts; corner/shadow choice is maintainer's, shown via preview | `PITFALLS.md` Pitfalls 17/23/24; `FEATURES.md` row #19; Visual Companion required per user's global CLAUDE.md for the corner/shadow choice |
| WIN-04 | 100%↔150% monitor moves, no size jumps | `PITFALLS.md` Pitfall 13/14 — **cannot be exercised on this machine** (single monitor, confirmed below); needs the temporary virtual display from VER-12 |
| WIN-05 | Maximize icon + `WindowState.placement` match real OS state | Conflict #4; `PITFALLS.md` Pitfall 15; codebase: `AeroTitleBar.kt:119-136` reads `windowState.placement` directly for icon choice — already correct for half-snap (never sets `Maximized`), per `ARCHITECTURE.md`'s explicit non-finding |
| WIN-06 | Multiple native-chrome windows, incl. narrow ~300px, independent | `PITFALLS.md` Pitfall 5 (per-HWND state); codebase: showcase currently opens exactly one window — SHW-17 requires adding a second |
| BTN-01 | Maximize button hover/press/click parity with min/close | Conflict #5; `PITFALLS.md` Pitfalls 11/12; codebase: `TitleBarButton` (`AeroTitleBar.kt:148-173`) uses `collectIsHoveredAsState()` today — maximize button's source of truth must switch to `AeroMaxButtonInteraction` |
| BTN-02 | Minimize/close/interactive `leading` content unaffected | `PITFALLS.md` Pitfall 19; codebase: `leading` slot already exists (`AeroTitleBar.kt:79,99-102`) — needs bounds published as `HTCLIENT`, not a new slot |
| API-01 | Source-compatible `AeroTitleBar`/`AeroResizeHandles`; no JNA types public; non-Windows unchanged | `ARCHITECTURE.md` "Public API Shape"; codebase: current signature `AeroTitleBar(title, windowState, onCloseRequest, leading, modifier)` — additive-only new params |
| API-02 | Mark an element as clickable/non-draggable inside the title bar | `ARCHITECTURE.md`'s `markAeroTitleBarInteractive()` design; ties to BTN-02's `leading`-slot boundary problem |
| API-03 | Opt out of native behavior per window | `ARCHITECTURE.md`'s `nativeWindowManagement: Boolean = true` design |
| DEP-01 | JNA + jna-platform 5.19.1, `implementation` scope, matches Pinya | `STACK.md` §1-2; **confirmed locally**: both jars already in this machine's Gradle cache at exactly 5.19.1 (see Environment Availability) |
| SHW-17 | Showcase opens a second narrow (~300px) window with interactive title-bar element | Codebase: `Main.kt` `application { }` block — second `Window(...)` composable is the natural addition point, following the existing `-Daero.*` parameter-forwarding pattern in `showcase/build.gradle.kts` |
| VER-11 | Headless live-window checks, each proven failing on old `WindowDraggableArea` code | `PITFALLS.md` "Verification" section's provable-without-cursor list; see dedicated section below for the fail-on-old-code mechanism |
| VER-12 | Two real-input sessions (early + full), warn-and-confirm protocol | `feedback_never_ask_user_to_stop_using_pc.md` (verbatim exception already recorded for this milestone); tooling gaps confirmed below |
| VER-13 | Agent self-review via `PrintWindow`; explicit "unconfirmed" list | `reference_compose_hot_reload_mcp.md`; `21-UNCONFIRMED.md` precedent read and summarized below |
| VER-14 | 541→N tests, guard bumped with a naming commit | Codebase: `library/build.gradle.kts` guard mechanics + the one prior bump commit (`ffece58`) — see Build & Test Guard section |
| REL-06 | README Windows-behavior section, JNA dependency named | Codebase: **no such section exists in README.md today** (grep confirmed) — this is new content, not an edit |
| REL-07 | KDoc updated, "Aero Snap limitation" removed | Codebase: exact string located at `AeroTitleBar.kt:63-66` |
| REL-08 | `3.2.0` tagged, JitPack green, coordinate resolves | Codebase: `jitpack.yml` (JDK 21, confirmed below) + prior release-commit precedent from v3.1 |

## Codebase Grounding

### `AeroTitleBar.kt` (`library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`)

- Public signature today (line 75-81): `FrameWindowScope.AeroTitleBar(title: String, windowState: WindowState, onCloseRequest: () -> Unit, leading: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier)`. API-01/API-02/API-03 need this extended with trailing default parameters only (`nativeWindowManagement: Boolean = true` at minimum) — every existing call site (this repo's own `showcase/Main.kt:98-102`, and any external consumer) must keep compiling unmodified.
- Line 83: `WindowDraggableArea(modifier = modifier) { Row(...) }` — the entire title Row, including all three `TitleBarButton`s, is currently inside one `WindowDraggableArea`. Per `PITFALLS.md` Pitfall 15, this must be removed once native `HTCAPTION` hit-testing covers the same region, not left "as a fallback" — leaving it active is a second, competing drag-handling path.
- Lines 63-66 (KDoc): the literal "**Aero Snap limitation:** `WindowDraggableArea` does NOT pass HTCAPTION to the OS..." paragraph is the exact text REL-07 requires replaced with the new behavior's description.
- Lines 119-136: the maximize/restore icon and its `onClick` read/write `windowState.placement` directly, comparing only against `WindowPlacement.Maximized`. Per `ARCHITECTURE.md`'s verified non-finding, a half-snap does **not** set `Maximized`, so this logic needs no change for SNAP-01 — but WIN-05 requires verifying it stays correct once maximize can also be triggered by `WM_SYSCOMMAND SC_MAXIMIZE` outside this `onClick`.
- `TitleBarButton` (lines 148-173): a private composable, one instance per button (minimize/maximize/close), driven by `MutableInteractionSource` + `collectIsHoveredAsState()` + `Modifier.hoverable(...).clickable(...)`. BTN-01 requires the maximize-button instance specifically to read hover/pressed state from a different source (`AeroMaxButtonInteraction`, per `ARCHITECTURE.md`) while minimize/close keep this exact code unchanged — the parameterization needs to happen at the call site (lines 113-142), not inside `TitleBarButton` itself, unless a new optional parameter is added there.
- `leading` slot (line 79, rendered lines 99-102): already exists, already documented as holding arbitrary composables. API-02/BTN-02 can extend this existing slot's bounds-tracking rather than invent a new one — `ARCHITECTURE.md`'s "Public API Additions" section independently reached the same conclusion (option: "automatically treat the existing `leading` slot's measured bounds as `HTCLIENT`").
- No `onGloballyPositioned` calls anywhere in this file today — Step 4's region registry wiring is entirely new code, not a modification of existing layout callbacks.

### `ResizeHandles.kt` (`library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt`)

- Public signature: `FrameWindowScope.AeroResizeHandles(windowState: WindowState)` — no parameters to add for API-01 (this composable takes no `nativeWindowManagement` flag in `ARCHITECTURE.md`'s design; the Windows/non-Windows branch is fully internal).
- Line 56: `if (windowState.placement != WindowPlacement.Floating) return` is the **only** existing early-exit gate. WIN-02 requires extending this to also early-return on Windows once native chrome is active — i.e., the guard becomes `if (windowState.placement != Floating || nativeChromeActive) return`, where `nativeChromeActive` is a Windows-only, per-window read (e.g. `NativeWindowChromeRegistry.isInstalled(window)` per `ARCHITECTURE.md`'s design) that is always `false` on Linux/macOS, preserving today's behavior there with zero platform-specific branching inside this file beyond the one added condition.
- Eight `Box` + `pointerInput` + `detectDragGestures` zones (lines 76-229), all mutating `windowState.size`/`windowState.position` directly via `DpSize`/`WindowPosition.Absolute`. None of this code needs to change on non-Windows; on Windows it becomes dead code once the gate fires, not code that needs its internal logic rewritten.
- `minW = 320f, minH = 240f` — the existing minimum-size floor. Per `ARCHITECTURE.md`'s DPI table, at 150% scale against a 300px-wide second window the resize-border bands (~36px combined) still leave comfortable width, but this project's stated 320×240 minimum is **wider** than the milestone's "~300px narrow window" scenario — WIN-06/SHW-17 planning must reconcile whether the narrow showcase window uses a smaller explicit minimum than this constant, or whether this constant needs a per-window override. Flagged as an open question below, not assumed either way.

### Showcase (`showcase/src/main/kotlin/com/mordred/showcase/Main.kt`, `showcase/build.gradle.kts`)

- `Main.kt` opens exactly **one** `Window(...)` inside `application { }` (lines 78-113), `undecorated = true, transparent = false` (matching the project's locked CMP-3757 rule), with `AeroTitleBar` + `AeroResizeHandles` composed inside a `Box(Modifier.fillMaxSize())`. SHW-17 needs a **second** `Window(...)` call inside the same `application { }` block — CMP's `application {}` DSL supports multiple `Window` calls natively, so this is additive, not a restructure.
- Existing launch-parameter plumbing (`initialScheme()`, `initialSection()`, `initialPage()`, `captureMode()`, all reading `System.getProperty("aero.*")`, lines 32-63) and `showcase/build.gradle.kts`'s forwarding of `-Paero.*` Gradle properties into both the plain `run` task and `ComposeHotRun` (the Hot Reload task type) is the established, already-working pattern for opening the showcase into a specific, reproducible state without synthetic input. A new `-Paero.secondWindow=true`-style flag (or similar) is the natural way to make the second narrow window's presence deterministic for capture/testing, following this exact precedent — do not invent a different mechanism.
- The second window needs its own `WindowState` (via `rememberWindowState(width = ~300.dp, ...)`), its own `AeroTitleBar`/`AeroResizeHandles` pair, and — per the milestone's Pinya-mirroring scenario — one interactive element inside its title bar (a counter or a button) marked via API-02, to give WIN-06/BTN-02/SHW-17 something concrete to click-test.
- **No existing multi-window precedent in this codebase** to copy from — this is genuinely new showcase code, not a refactor of something already working.

### Build, Dependencies, Test-Count Guard

- `library/build.gradle.kts` dependency block (top of file) already documents the `api`-vs-`implementation` convention this phase must follow: `kotlinx-coroutines-core` is the existing precedent for "internal only — not exposed in any public signature", declared `implementation`. JNA/`jna-platform` must be added the same way — confirmed by `STACK.md`'s independent analysis and directly visible in this file's own comment.
- `gradle/libs.versions.toml` has no JNA entries today (confirmed by reading the file in full — reproduced below). New entries needed:
  ```toml
  [versions]
  jna = "5.19.1"
  [libraries]
  jna = { module = "net.java.dev.jna:jna", version.ref = "jna" }
  jna-platform = { module = "net.java.dev.jna:jna-platform", version.ref = "jna" }
  ```
- **Test-count guard mechanics** (`library/build.gradle.kts`, top of file + `tasks.test { }` block): `val lockedTestTotal = 541` / `val lockedTestSkipped = 0` are plain Kotlin `val`s read by a `TestListener` that fails the build via `GradleException` if the actual executed count (unfiltered runs only — `--tests` filtering is detected and exempted) doesn't match exactly. **There is exactly one prior bump commit to model the new one on**: `ffece58` ("build(21-05): lock executed test count at 541 with a count guard (TOOL-16)") — this is the commit that *introduced* the guard itself (previous state was unguarded), not a bump of an existing guard, so this phase's bump will be the **first real bump** of a previously-locked number. The commit message convention to follow: `build(22-NN): raise locked test count to <N> — <reason> (VER-14)`.
- `jitpack.yml` (repo root): `jdk: [openjdk21]`, `before_install: sdk install/use java 21.0.12+1.1-tem`. No JNA-specific concern found here — JNA ships pure-JVM jars with the native dispatch stub bundled inside (per `STACK.md`), so no JitPack build-step changes are anticipated, but this has not been empirically re-verified against an actual JitPack build with JNA on the classpath (flagged in Open Questions).
- `showcase/build.gradle.kts` has no JNA reference either; JNA stays `:library`-only per API-01/DEP-01 — the showcase gets it transitively through `implementation(project(":library"))`.

### README / KDoc / Release

- `README.md` contains **zero** existing "window behavior", "Aero Snap", or "undecorated" content (grep confirmed) beyond the generic `Window(...)` usage snippet at line ~94-101 showing basic setup. REL-06 is **net-new README content**, not an edit of stale text — a new section (e.g. "## Windows window behavior") needs to be authored from scratch, covering: what works, Win10-vs-Win11 differences, how to mark an interactive title-bar element (API-02), how to opt out (API-03), and the new JNA 5.19.1 dependency.
- REL-07's target string is confirmed at `AeroTitleBar.kt:63-66` (quoted above) — this KDoc paragraph needs rewriting to describe the new native behavior, not just deletion.
- Release precedent from `v3.1.0` (`.planning/milestones/v3.1-phases/21-migration-release-3-1-0/`): plans 13-14 handled "verify tag green on JitPack" (push a `v3.1.0-verifyNN` disposable tag first, confirm JitPack build status `ok` via its build-status API, only then push the real `v3.1.0` tag) and README-floor updates as two separate, maintainer-confirmed steps. REL-08 should follow the identical two-step verify-then-real-tag pattern, not skip straight to the real tag.

## Test Infrastructure

- Framework: JUnit 6.1.3 (via `kotlin.test`) for pure-JVM logic tests, `runComposeUiTest` (from `compose.uiTest`, already a `testImplementation` dependency) for Compose-level UI tests, both already wired in `library/build.gradle.kts`.
- Existing test package layout under `library/src/test/kotlin/com/mordred/aero/`: one directory per component family (`components/buttons`, `components/navigation`, `components/range`, etc.), plus `capture/`, `theme/`, `verification/`. **There is no `internal/windows` or equivalent test package today** — new pure-JVM tests for hit-test classification math, DPI-thickness formulas, and maximized-client-rect math (per `ARCHITECTURE.md`'s "Testing Architecture" section) are new test files, most naturally under a new `library/src/test/kotlin/com/mordred/aero/internal/windows/` package mirroring the proposed `internal/windows/` main-source package.
- **No live-window / opt-in test harness exists in this codebase today.** `tools/capture/` (PowerShell, `AeroCapture.ps1`, `Invoke-ShowcaseSweep.ps1`, `Compare-AeroCaptures.ps1`, `New-HandoffPage.ps1`, `Test-NonInterference.ps1`) is the closest analog — a maintainer-invoked, non-CI, opt-in PowerShell toolkit, not a JUnit test `./gradlew test` runs by default. `ARCHITECTURE.md`'s own recommendation (option (a): "a small Kotlin/JVM `main()` under `tools/`") matches this project's existing precedent better than inventing a new Gradle source set — this phase's plan should follow that precedent explicitly rather than introduce a `winIntegrationTest` source set with no local prior art.
- `AeroTitleBarTest.kt` exists at `library/src/test/kotlin/com/mordred/aero/components/navigation/` (confirmed via `find`, referenced in `ARCHITECTURE.md`'s sources list) — this phase's pure-JVM/Compose-UI-test additions for `AeroTitleBar`'s new region-reporting behavior belong alongside it, in the same package, not a new one.
- No system-property-gated opt-in pattern currently exists for "live window, real HWND" tests specifically (the existing `aero.captureDir` system property from BASE-05/D-03 gates *image-writing*, not live-window execution) — a new gating convention (e.g. a Gradle task explicitly excluded from `check`, per `ARCHITECTURE.md`'s option (b), or a `main()` tool per option (a)) is a decision this phase's first plan must make explicitly, not leave implicit.

## Showcase / Hot Reload / JBR

- `showcase/build.gradle.kts` applies `alias(libs.plugins.compose.hot.reload)` (`org.jetbrains.compose.hot-reload` 1.2.0, from `libs.versions.toml`) and forwards `-Paero.*` properties to both `tasks.withType<JavaExec>` (the plain `run` task) and `tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>` (the `hotRun`/`hotRunAsync`/`hotDev`/`hotDevAsync` family) via two near-identical blocks — a known, already-recorded code-review debt item (IN-01 in `21-REVIEW.md`, "duplicated `aero.*` forwarding block"), not something to silently "fix" as part of this phase unless the phase's own new launch parameter needs the same treatment (in which case, add the third property to both existing blocks, matching the existing pattern, rather than refactoring the duplication away as unplanned scope).
- `grep -rn "JBR\|CustomWindowDecoration\|jetbrains.runtime\|com.jetbrains"` across all `.kt`/`.kts` files in the repo returned **zero matches**. This directly answers conflict #7's cheapest check (`SUMMARY.md` §7): JBR's own custom-window-decoration machinery is not engaged anywhere in this codebase today, so there is nothing to disable — the remaining risk from Pitfall 16 is purely the Hot Reload re-run/double-subclass-install risk, which needs an idempotent-install guard (keyed by HWND) regardless of the JBR finding, not a JBR-specific mitigation.
- `.mcp.json` (repo root) already correctly targets `:showcase:hotMcpServer` (module-qualified, per the v3.1 locked lesson) via `cmd /c .\gradlew.bat --no-daemon --quiet --console=plain :showcase:hotMcpServer` — no changes needed here for this phase; the existing Hot Reload MCP setup is reusable as-is for driving/observing the Compose-side hover/press animation of the new maximize-button state (per `ARCHITECTURE.md`'s testing-architecture note that MCP tools "cannot themselves send `WM_NCHITTEST`/`WM_SYSCOMMAND`" but "complement" the live-window harness).
- JetBrains Runtime 21.0.9 is installed at `C:\Users\1\.jdks\jbr-21.0.9` (per `MCP-HOWTO.md`, maintainer-verified 2026-09-23) and is what `hotRun` uses; the published library itself still targets a standard JDK 21 toolchain (`kotlin { jvmToolchain(21) }` in `library/build.gradle.kts`, no JBR requirement) — Pitfall 16's "verify cold `./gradlew run` matches Hot Reload behavior" check is meaningful specifically because these are two different JVMs (standard JDK 21 vs. JBR 21.0.9) on this machine, not a hypothetical.

## Release Plumbing

- `jitpack.yml`: `jdk: [openjdk21]`, confirms JDK 21 (not JBR) is what JitPack itself builds with — consistent with the milestone's "must work on a standard JDK 21" constraint; JitPack building successfully with JNA on the classpath has not been empirically tested yet for this project (Open Question below).
- No JNA-related JitPack build customization exists or appears necessary from the `STACK.md` analysis (no native compilation step, pure-JVM jars) — but this is a MEDIUM-confidence carry-over from milestone research, not independently re-verified against a real JitPack run in this session.
- Prior release pattern (v3.1.0, `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/`): version bump in `build.gradle.kts` lands in its own commit before tagging; a disposable `-verifyNN` tag confirms JitPack green before the real tag is pushed; `README.md`'s stated consumer floor is updated in the same release-adjacent commit group. REL-08 should mirror this exactly.

## Verification Tooling on This Machine

Direct constraints from the maintainer's own memory notes, folded into the Validation Architecture below (all read in full this session, not summarized from an index):

- **`PrintWindow(hwnd, hdc, 2)` is the only sanctioned capture method, project-wide** (`reference_compose_hot_reload_mcp.md`, `reference_windows_mcp_showcase_capture.md` — the latter is 64 days stale per its own staleness banner and is explicitly superseded by the former for capture purposes). The MCP server's own `take_screenshot` tool is banned (real screen-region grab, already caused one real incident).
- **All GUI debugging runs through Compose Hot Reload MCP before the maintainer ever sees it** (`feedback_gui_self_review_before_user.md`) — this phase's Step 10 hand-off (VER-13) must follow the same "agent looks first" discipline already established in v3.1, using `PrintWindow` frames + the MCP semantic tree, not raw hand-off.
- **Real mouse/keyboard input is banned by default; the exception is explicitly pre-recorded for this exact milestone** (`feedback_never_ask_user_to_stop_using_pc.md`, modified 2026-09-24 with a verbatim v3.2.0-specific carve-out): warn the maintainer, wait for their "ok", drive input, announce when done — batched into exactly the two sessions ROADMAP.md/PROJECT.md already specify (early 1-2 min risk-gate session, full end-of-phase session). The non-interference guard may only fail on what the agent's own tooling causes (its own cursor moves, its own app taking foreground) — the maintainer's own concurrent activity must never abort a run (fixed in `9834e8c`, per this memory).
- **A repro/guard must provably fail on the unfixed code, or a passing result is a false positive** (`feedback_repro_must_exercise_path.md`) — this is the exact discipline VER-11 requires, and `PITFALLS.md`'s closing section already names the concrete mechanism (see next section).
- **One goal = one phase; tools are steps, not phases; proofs are self-measured, not human ceremonies** (`feedback_no_process_inflation.md`) — directly reinforces ROADMAP.md's "one phase, ten ordered steps" structure; the plan must not split VER-11/VER-12/VER-13/VER-14 or the release into separate phases.
- **The VS Code Kotlin language server can hold `library-*.jar` open**, causing `Unable to delete file` on any build that rewrites the jar (a version bump, `--refresh-dependencies`) (`reference_vscode_kotlin_ls_jar_lock.md`) — worth knowing before this phase's version-bump commit (Step 10) if the build fails with that exact error; the fix is asking the maintainer before ending the process, never suggesting "Reload Window" (that would kill the running Claude Code session, since it runs inside VS Code here).

## VER-11: Proving Checks Fail on the Old `WindowDraggableArea` Code

`PITFALLS.md`'s closing section ("Building a guard that provably FAILS on the old `WindowDraggableArea`-only implementation") already specifies the exact mechanism, directly applicable to this codebase:

1. **Before writing any new hit-testing code**, write the `SendMessage(hwnd, WM_NCHITTEST, ...)` probe against the **current, unmodified** showcase window (today's `AeroTitleBar`/`AeroResizeHandles`, still wrapped in `WindowDraggableArea`) and confirm it returns whatever AWT's own default answers (`HTCLIENT` is the expected baseline for an unmodified undecorated `WS_POPUP` frame) across the title-bar region and the resize-zone regions — **not** `HTCAPTION`/`HTTOP`/etc.
2. This is the RED half of a RED→GREEN pair: once Step 4 (region registry) and Step 2 (styles) land, the same probe against the same screen points must now return `HTCAPTION`/`HTMAXBUTTON`/resize codes — proving the guard actually distinguishes "native hit-testing present" from "native hit-testing absent," not just "always passes."
3. **API-03's opt-out flag (`nativeWindowManagement = false`) gives this a mechanical, permanent form**, beyond the one-time RED capture at the start of the phase: any VER-11 check can be run twice against two windows in the same process — one with `nativeWindowManagement = true`, one with `false` — and the `false` instance must reproduce the pre-phase `HTCLIENT`-everywhere behavior at any point in the future, not just during initial development. This turns VER-11's "provably fails on old code" requirement into a regression guard that survives past the phase itself, not a one-shot proof.
4. The other checks in `PITFALLS.md`'s "provable without synthetic input" list (style-bit read-back via `GetWindowLongPtr(GWL_STYLE)`, maximize geometry via `GetWindowRect` vs. `MonitorInfo.rcWork`, auto-hide detection via `SHAppBarMessage`) follow the same RED-first discipline: capture the unmodified-code answer first, then prove the new code changes it in the expected direction.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Windows 11, build 26100 (24H2) | Entire phase | Yes | 10.0.26100 | — |
| Second monitor at a different DPI scale | WIN-04, VER-12 full session | **No** — confirmed via `[System.Windows.Forms.Screen]::AllScreens`: exactly one screen, `\\.\DISPLAY1`, 1920×1080, Primary=True | — | Temporary virtual display driver, per ROADMAP.md Step 9 / PROJECT.md's pre-authorized plan (admin-rights install, maintainer confirms, removed after) |
| PowerToys (FancyZones) | SNAP-07, VER-12 full session | **No** — confirmed via `HKLM:\...\Uninstall\*` registry scan (no `PowerToys` entry) and `Test-Path 'C:\Program Files\PowerToys\PowerToys.exe'` = `False` | — | Install for the duration of the full VER-12 session only, per PROJECT.md's pre-authorized plan; uninstall afterward "by the maintainer's word" |
| Taskbar auto-hide state | WIN-01 (auto-hide-specific inset behavior) | **Yes, currently ON** — confirmed via `HKCU:\...\StuckRects3` `Settings` byte offset 8 = `0x03` (autohide + always-on-top bits both set) | — | None needed — this is the real repro condition `PITFALLS.md` Pitfall 9 assumed, not hypothetical |
| JNA 5.19.1 / jna-platform 5.19.1 | DEP-01, entire implementation | **Yes** — both jars already present in this machine's Gradle module cache (`~/.gradle/caches/modules-2/files-2.1/net.java.dev.jna/{jna,jna-platform}/5.19.1`), meaning a prior build on this machine already resolved them successfully | 5.19.1 | — |
| JetBrains Runtime 21 (for `hotRun`) | Steps 1/7/10 (JBR-vs-standard-JDK comparison) | Yes — `C:\Users\1\.jdks\jbr-21.0.9`, confirmed by `MCP-HOWTO.md` (maintainer-verified 2026-09-23) | 21.0.9 | — |
| UI Automation tooling for detecting the Snap Layouts flyout without a screenshot | VER-12 ("fixed without a screenshot, e.g. via UI Automation") | **Not independently verified this session** — no UIA-scripting tool (e.g. a PowerShell `System.Windows.Automation` snippet, or an existing project script) was found in `tools/` | Needs a small ad-hoc script (PowerShell `UIAutomationClient`/`FlaUI`-equivalent) built as part of Step 10, or the flyout's appearance is confirmed by the maintainer's own eyes during the real-input session and named as maintainer-confirmed rather than agent-confirmed — this distinction should be explicit in the phase's plan, not glossed over |
| Windows 10 machine | VER-F04 (future requirement, explicitly out of this milestone) | No | — | N/A — explicitly out of scope; goes on the unconfirmed list per VER-13 |

**Missing dependencies with no fallback:** none — every gap above has an authorized, named fallback (virtual display, temporary PowerToys install, or explicit "maintainer-confirmed not agent-confirmed" framing for the UIA gap).

**Missing dependencies with fallback:** second monitor (virtual display driver, pre-authorized), PowerToys (temporary install, pre-authorized), UI-Automation-based flyout detection (small new script, or maintainer-eyes framing).

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `net.java.dev.jna:jna` `[ASSUMED]` | 5.19.1 | JNI runtime (`Pointer`, `Native`, `Callback`) | Confirmed live against Maven Central's `maven-metadata.xml` by the milestone's `STACK.md` research (fetched 2026-09-25); already resolved in this machine's own Gradle cache (see Environment Availability), matching the version Pinya (this library's own first consumer) already depends on |
| `net.java.dev.jna:jna-platform` `[ASSUMED]` | 5.19.1 | Pre-built `User32`/`WinDef`/`WinUser`/`WinNT` Win32 bindings | Same version-pin rationale; ships most needed types (`HWND`, `RECT`, `MONITORINFO`, `WinUser.WindowProc`) — `STACK.md` confirmed by reading the live JNA GitHub source which calls are present vs. absent |

**Version verification:** `npm view`-equivalent for Maven is `mvn dependency` resolution or a direct `search.maven.org` query; this session's own `slopcheck scan --pkg maven net.java.dev.jna:jna` attempt returned a registry-timeout error (`search.maven.org` unreachable from this sandbox), so slopcheck itself did **not** independently confirm the package this session — see Package Legitimacy Audit below for how this is handled. The version number and its currency were independently confirmed by the milestone's own `STACK.md` research (`https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/maven-metadata.xml`, fetched live 2026-09-25, `lastUpdated 20260612`, no 6.x exists) and by this session's own finding that 5.19.1 is already the version resolved in this machine's local Gradle cache.

**Installation:**
```kotlin
// gradle/libs.versions.toml — new entries, no existing jna.* keys found in the file today
[versions]
jna = "5.19.1"

[libraries]
jna = { module = "net.java.dev.jna:jna", version.ref = "jna" }
jna-platform = { module = "net.java.dev.jna:jna-platform", version.ref = "jna" }
```
```kotlin
// library/build.gradle.kts — implementation scope, matching the existing
// kotlinx-coroutines-core "internal only" precedent already documented in this file
dependencies {
    implementation(libs.jna)
    implementation(libs.jna.platform)
}
```

### Supporting

No additional Maven artifacts. `Dwmapi` (for optional shadow/corner-preference polish) and DPI-related `User32` extension methods (`GetDpiForWindow`, `GetSystemMetricsForDpi`, `TrackMouseEvent`, `GetSystemMenu`, `TrackPopupMenu`) are confirmed **absent** from `jna-platform` 5.19.1's shipped bindings (per `STACK.md`, verified against live JNA source) and must be hand-declared as small internal extension interfaces — standard JNA pattern, no native compilation involved.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JNA | A custom-compiled native `.dll` (FlatLaf's own approach) | Requires a C/C++ toolchain, per-arch builds, code signing, and JitPack build-step changes — no precedent anywhere in this repo's build; rejected by `STACK.md` for exactly this reason |
| JNA | `java.lang.foreign` (Panama/FFM API, JDK 22+) | Project targets JDK 21; FFM's restricted-method warning regime doesn't apply until JDK 24 (JEP 472) anyway — no reason to take on FFM's steeper API surface for a JDK-21-targeted library |

## Package Legitimacy Audit

Both packages are long-established, widely-used Java ecosystem libraries (Java Native Access, active since 2007, thousands of downstream consumers) confirmed via the milestone's own direct fetch of Maven Central's authoritative metadata and the JNA project's own GitHub source (`STACK.md`, HIGH confidence on presence/version/license). This session additionally confirmed both jars are already present in this machine's own Gradle module cache at exactly 5.19.1 — meaning a real build on this machine has already successfully resolved and used them (almost certainly via Pinya, the stated first consumer of this milestone's own work).

`slopcheck scan --pkg maven net.java.dev.jna:jna` / `jna-platform` were attempted this session but both returned `REGISTRY_ERROR` (`search.maven.org` connection timeout from this sandboxed environment) — slopcheck itself could not independently confirm either package this session. Per the Package Legitimacy Gate protocol's graceful-degradation rule, both packages are therefore tagged `[ASSUMED]` below (not `[VERIFIED]`), despite the strong corroborating evidence above, and the planner should gate the dependency-addition task behind a `checkpoint:human-verify` before the first `./gradlew build` that resolves them for real.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `net.java.dev.jna:jna` | Maven Central | ~19 years (JNA project founded 2007) | Extremely high (foundational native-interop library; exact figures not queried this session) | `github.com/java-native-access/jna` | `ERROR` (registry unreachable this session) | Approved with `[ASSUMED]` tag — pre-existing local Gradle cache + milestone-research live Maven Central fetch are strong corroboration; planner adds `checkpoint:human-verify` before first real resolution |
| `net.java.dev.jna:jna-platform` | Maven Central | Same project, same age | Same | Same repo (`contrib/platform` module) | `ERROR` (registry unreachable this session) | Approved with `[ASSUMED]` tag — same rationale |

**Packages removed due to slopcheck `[SLOP]` verdict:** none.
**Packages flagged as suspicious `[SUS]`:** none — the `ERROR` status is a network-reachability failure in this sandbox, not a suspicion signal, but per protocol it does not upgrade either package to `[VERIFIED]`.

## Architecture Patterns

The full architecture (system diagram, component responsibilities, the three core patterns — `CallWindowProc` passthrough, immutable-snapshot cross-thread bridge, non-client-driven button state via EDT hop — data flow, DPI/multi-monitor considerations, anti-patterns, and public API shape) is already fully specified in `.planning/research/ARCHITECTURE.md` at MEDIUM-HIGH confidence and should be treated as the authoritative architecture for this phase's plan. Recommended project structure from that file:

```
library/src/main/kotlin/com/mordred/aero/
├── components/navigation/
│   ├── AeroTitleBar.kt              # existing — modified: region reporting, opt-out flag, native max-button state
│   └── ResizeHandles.kt             # existing — modified: Windows no-op guard
├── internal/windows/                # new package — Windows-only, `internal`, JNA-backed
│   ├── NativeWindowChromeRegistry.kt
│   ├── AeroWndProc.kt
│   ├── HitTestRegionRegistry.kt
│   ├── AeroMaxButtonInteraction.kt
│   ├── Win32Chrome.kt
│   ├── Win32Dpi.kt
│   └── Win32Interop.kt
└── components/navigation/AeroWindowChrome.kt   # new — public, additive lower-level API surface
```

This matches the codebase's existing convention of one file per responsibility (mirrors `GlassModifiers.kt`'s split from v3.0) and keeps `internal` Kotlin visibility (module-scoped, same as the rest of `:library`) enforceable by the same kind of greppable convention `tools/verify/check-material3.sh` already uses for a different concern — a natural follow-up gate (not required by this phase, but worth naming) would grep for `com.sun.jna` outside `internal/windows/`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Edge/corner snap detection, preview overlay | Custom Kotlin proximity math + a translucent preview Composable | Correct `WM_NCHITTEST` hit-testing only — Windows draws its own preview and does the math | `FEATURES.md`'s anti-features table; the OS version handles multi-monitor geometry, DPI, and every future Windows UI refresh for free |
| Snap Layouts flyout UI | A custom Compose popup mimicking the Win11 flyout | `HTMAXBUTTON` from `WM_NCHITTEST` | No message-based way exists to render a look-alike that stays visually correct across Windows updates; Microsoft's own guidance is explicitly "return `HTMAXBUTTON`," not "draw your own" |
| Alt+Space system menu items | Custom Compose context menu for Move/Size/Minimize/Maximize/Close | `WS_SYSMENU`+`WS_CAPTION` styles, `DefWindowProc`'s own `WM_SYSCOMMAND` handling | Already-native OS behavior; forwarding unhandled `WM_SYSCOMMAND` to `CallWindowProc` gives this for free |
| DPI-scaled resize-border thickness | Hardcoding the existing 4.dp Compose constant for the native path | `GetSystemMetricsForDpi(SM_CXSIZEFRAME, ...)`, re-queried per-window at its current DPI | The existing `ResizeHandles.kt` constant (`edge = 4.dp`) is a Compose-space value tuned for the old drag-based implementation; it has no defined relationship to the physical-pixel resize band Windows expects once native hit-testing owns the border |
| A JNA callback-lifetime tracker | Ad hoc `remember{}`/lambda holding the `WindowProc` `Callback` | A dedicated `NativeWindowChromeRegistry` singleton keyed by HWND, holding a strong `val` reference | `PITFALLS.md` Pitfall 1 — this is a documented, well-known JNA crash class (native SIGSEGV, no Kotlin stack trace), not a style preference |

**Key insight:** every OS-owned behavior in this milestone (Snap, Snap Layouts, Snap Assist, Snap Groups, Aero Shake, Win+Arrow, FancyZones pickup, Alt-Tab thumbnails, minimize animation) is unlocked entirely by getting `WM_NCHITTEST`/`WM_NCCALCSIZE`/window styles right — there is no feature on the milestone's list that requires writing OS-behavior-replicating code in Kotlin. The one exception (maximize-button hover/press *visuals*, which stay Compose-rendered) is explicitly the one place custom code is correct, per `ARCHITECTURE.md` Pattern 3.

## Common Pitfalls

The full 24-pitfall catalogue in `.planning/research/PITFALLS.md` is already phase-step-mapped and should be treated as the authoritative pitfall list for this plan (each pitfall names its target `Phase step`, its verification method, and its recovery cost). The five flagged **Critical** in the milestone `SUMMARY.md` are, in order of how early they must be structurally addressed:

1. **JNA callback GC'd mid-session → native crash** (Pitfall 1) — structural fix (HWND-keyed strong-reference registry) must exist before any hit-test logic is written, not retrofitted.
2. **`undecorated=true` does not yield `WS_THICKFRAME`; Snap silently does nothing without it** (Pitfall 10) — confirmed by reading `awt_Frame.cpp` directly in the milestone research; the style-bit fixup is a load-bearing first sub-step of Step 2, verified via `GetWindowLongPtr` read-back before any hit-test debugging.
3. **Naive `WM_NCCALCSIZE` "return 0" causes maximized-window taskbar overhang**, and the fix for that can in turn swallow auto-hide taskbar hover-reveal on **this exact dev machine's real configuration** (auto-hide confirmed ON above) (Pitfalls 8/9) — needs the DPI-scaled frame-thickness inset plus the `SHAppBarMessage`/`ABM_GETAUTOHIDEBAREX` 2px-inset approach.
4. **Not chaining unhandled messages to AWT's original WndProc via `CallWindowProc`** (Pitfall 3) — silently breaks IME, focus, DnD, accessibility; "forward by default, intercept only the named few" is a structural invariant from the first commit, not a later cleanup.
5. **`WindowState.placement` can desync from real OS state**, and `AeroTitleBar`'s existing `WindowDraggableArea` + `AeroResizeHandles`' existing `pointerInput` handlers become **second, competing input paths** once native `HTCAPTION`/resize codes take over the same pixels (Pitfall 15) — `WindowDraggableArea` must be removed from `AeroTitleBar.kt:83`, not left "just in case."

Two additional pitfalls worth surfacing here because they interact directly with facts confirmed on this machine this session:
- **Pitfall 13/14 (DPI/`WM_DPICHANGED` during cross-monitor drag)** — this machine genuinely cannot exercise this at all (single monitor, confirmed above); the phase plan must explicitly decide whether the temporary virtual-display setup (pre-authorized in PROJECT.md) covers this in Step 9, or whether it becomes a named gap in the final `21-UNCONFIRMED.md`-style report, the same way v3.0/v3.1 named their own DPI gaps rather than hiding them.
- **Pitfall 16 (JBR double-subclass under Hot Reload)** — this session's own grep confirms JBR's decoration machinery is not engaged in this codebase today, narrowing this pitfall to purely the Hot-Reload-re-run idempotency risk (no JBR-specific disable code needed), which simplifies Step 1's scope slightly versus what `PITFALLS.md` alone would suggest.

## Code Examples

See `.planning/research/ARCHITECTURE.md` §"Architectural Patterns" for verified-shape code examples of the three core patterns (`CallWindowProc` passthrough subclass, `onGloballyPositioned` → `AtomicReference<HitTestSnapshot>` publish, EDT-hop button-state bridge) and `.planning/research/STACK.md` §2 for the exact JNA extension-interface declarations needed for methods absent from `jna-platform` (`GetDpiForWindow`, `GetSystemMetricsForDpi`, `TrackMouseEvent`, `Dwmapi`). These are not reproduced here to avoid drift between two copies of the same code — the plan should cite `ARCHITECTURE.md` directly.

One codebase-specific example worth capturing here — the exact gating change `WIN-02` needs in `ResizeHandles.kt`, shown against the current source (line 56):

```kotlin
// Current (ResizeHandles.kt:56):
if (windowState.placement != WindowPlacement.Floating) return

// Target shape (illustrative — exact read mechanism per ARCHITECTURE.md's
// NativeWindowChromeRegistry design, not yet implemented):
if (windowState.placement != WindowPlacement.Floating) return
if (isWindows && nativeChromeActive) return  // OS now owns HT*-code resize on this HWND
```

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `net.java.dev.jna:jna`/`jna-platform` 5.19.1 are legitimate, current, non-hallucinated packages | Standard Stack, Package Legitimacy Audit | If wrong (e.g. slopsquatted lookalike), the phase would pull a malicious dependency into `:library`, shipped to every consumer including Pinya — mitigated by the strong independent corroboration (already in this machine's own resolved Gradle cache, live Maven Central fetch in milestone research, official `java-native-access` GitHub org) but not by a slopcheck registry pass this session, since the registry was unreachable |
| A2 | `WS_SYSMENU` is already present on the current unmodified undecorated frame's native style (so Alt+Space already opens *a* system menu today, pre-phase) | Phase Requirements SNAP-05, Common Pitfalls | If wrong, Step 6's "unchanged from before this phase" baseline check would compare against a false assumption — mitigated by conflict #1's own cheap `GetWindowLongPtr` read-back being the first sub-step of Step 2, which settles this empirically before anything depends on it |
| A3 | JitPack will build this library successfully once JNA is added to `:library`'s classpath, with no `jitpack.yml` changes needed | Release Plumbing | If wrong, REL-08's verify-tag step would fail and need a `jitpack.yml` fix (e.g. explicit memory/timeout settings) discovered only at release time, not during implementation — low risk given JNA's pure-JVM packaging, but genuinely unverified against this project's actual JitPack pipeline |
| A4 | Windows Terminal's 2px auto-hide-taskbar inset (not 1px) is the correct value to copy for this project's own maximize-taskbar interaction | Common Pitfalls (Pitfall 9, inherited from milestone research) | If wrong, maximize could either clip a sliver of content (inset too large) or fail to let the auto-hide taskbar reveal on hover (inset too small) — mitigated by the milestone research's own explicit flag that no source proposes 1px specifically for this concern, only Windows Terminal's real production 2px |

**If this table is empty:** N/A — see entries above.

## Open Questions (RESOLVED)

1. **How should the ~300px narrow showcase window's minimum size interact with `AeroResizeHandles`' existing hardcoded `minW = 320f` constant?**
   - What we know: `ResizeHandles.kt` enforces `minW = 320f, minH = 240f` unconditionally, for every window using this composable; the milestone explicitly asks for a "~300px" narrow window, which is narrower than that floor.
   - What's unclear: whether the 300px figure is the window's *initial* size (which could still grow past 320px on first resize) or a true minimum the narrow window must be allowed to reach; and whether `minW`/`minH` should become per-window parameters or stay a shared constant now that WIN-02 makes this composable a no-op on Windows anyway (making the constant only relevant on non-Windows platforms going forward).
   - Recommendation: ask the maintainer directly during phase planning rather than guessing a number — this is a concrete product decision (Russian: "узкое окно ~300px — это начальный размер или минимум, до которого его можно ужать?"), not a research gap.
   - RESOLVED: 22-CONTEXT.md D-01 — default floor 320×240 dp; an app-set `window.minimumSize` wins; the SHW-17 narrow window sets its own minimum below 300 px.

2. **Does `jna-platform` 5.19.1 actually ship `GetSystemMenu`/`TrackPopupMenu`?**
   - What we know: `STACK.md` flags this as "not exhaustively checked... LOW-MEDIUM confidence," needed only if conflict #2 resolves toward "Alt+Space needs explicit handling" rather than "fully automatic."
   - What's unclear: the answer either way — this session did not independently re-verify it (would require fetching the live JNA source, which the milestone research already did once at LOW-MEDIUM confidence and flagged for re-check).
   - Recommendation: settle via conflict #2's own headless check (style-bit read-back) early in Step 1/2, per `SUMMARY.md`'s own plan — if Alt+Space turns out automatic, this becomes moot.
   - RESOLVED: planner checked the 5.19.1 jar with javap — `GetSystemMenu`/`TrackPopupMenu` are absent and are hand-declared in 22-02 `<interfaces>`; whether they are needed is settled by conflict C2 (22-10, 22-15).

3. **Will JitPack's build succeed unmodified with JNA on `:library`'s classpath, or does `jitpack.yml` need adjustment (memory limits, native-library extraction path)?**
   - What we know: JNA ships as pure-JVM jars with the native dispatch stub bundled inside; `STACK.md` found no reason this should need special JitPack handling.
   - What's unclear: this has never been tried against this project's actual JitPack pipeline.
   - Recommendation: the REL-08 "disposable verify tag" step is exactly the mechanism to answer this cheaply and safely, before the real `v3.2.0` tag — no separate research needed, just don't skip that verification step.
   - RESOLVED: settled empirically by the verify tag in 22-18 (pushed without asking per 22-CONTEXT.md D-06).

4. **What is the sanctioned way to detect the Snap Layouts flyout's appearance without a screenshot (VER-12's own requirement), given no existing UI-Automation tooling was found in this repo?**
   - What we know: the milestone brief itself says "fixed without a snapshot (e.g., via UI Automation)"; no existing script in `tools/` does this today.
   - What's unclear: whether a small new PowerShell UIA script should be built as project tooling (reusable, like `tools/capture/`), or whether this one check is simply confirmed by the maintainer's own eyes during the real-input session and recorded as maintainer-confirmed rather than agent-confirmed.
   - Recommendation: ask the maintainer which they'd prefer before Step 10 — building UIA tooling is a nontrivial new tool investment for a single check; maintainer-eyes-only confirmation is cheaper but shifts VER-12's "agent measures this" framing toward "maintainer attests to this," which is a real difference in what the phase can claim as proven.
   - RESOLVED: 22-CONTEXT.md D-04 — a UI Automation detector under `tools/` (22-03); if UIA cannot observe the flyout, stop and ask.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 6.1.3 (`kotlin.test`) for pure-JVM; `runComposeUiTest` (`compose.uiTest`) for Compose-level UI tests — both already configured in `library/build.gradle.kts` |
| Config file | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }` + the `lockedTestTotal`/`lockedTestSkipped` guard) |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.internal.windows.*"` (once the new package exists) |
| Full suite command | `./gradlew test` (only `:library` has a test source set, per the v3.1 `21-VALIDATION.md` precedent) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-01 | Existing `AeroTitleBar`/`AeroResizeHandles` call sites compile unchanged | build | `./gradlew :library:compileKotlin :showcase:compileKotlin` | ✅ existing call sites (`Main.kt`) |
| DEP-01 | JNA/`jna-platform` pinned 5.19.1, `implementation` scope | build verification | `grep -A2 "libs.jna" library/build.gradle.kts` shows `implementation` not `api` | ❌ Wave 0 |
| Hit-test classification math | Pure function correctness (caption/button/client/edge/corner points) | unit | `./gradlew :library:test --tests "*HitTestClassificationTest*"` | ❌ Wave 0 |
| DPI-thickness formula | Pure arithmetic at 100/125/150/200% | unit | `./gradlew :library:test --tests "*Win32DpiTest*"` | ❌ Wave 0 |
| Maximized-client-rect math | Synthetic monitor geometries incl. taskbar on each edge | unit | `./gradlew :library:test --tests "*MaximizedRectTest*"` | ❌ Wave 0 |
| Region-registry concurrency | No torn reads under concurrent writer/reader | unit (stress) | `./gradlew :library:test --tests "*HitTestRegionRegistryTest*"` | ❌ Wave 0 |
| VER-11 (RED→GREEN, style bits + hit-test answers) | Live-window `SendMessage`/`GetWindowLongPtr` probes | live-window opt-in tool | new `tools/`-based Kotlin `main()` (no existing harness — see Test Infrastructure) | ❌ Wave 0 |
| VER-14 | Locked test count raised with a named-reason commit | build verification | `./gradlew :library:test --rerun` → `AERO_TEST_COUNT total=<new locked>` | ✅ mechanism exists (`ffece58` precedent) |
| REL-06/07 | README section + KDoc updated | grep | `grep -c "Aero Snap limitation" library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` = 0; `grep -c "Windows window behavior" README.md` ≥ 1 | ✅ existing files to edit |
| REL-08 | `3.2.0` tagged, JitPack green | remote | JitPack build-status API, per v3.1's disposable-verify-tag precedent | — |

### Sampling Rate

- **Per task commit:** full `./gradlew test` — this project's own locked convention since v3.1 ("every bump gated by a full test run"), not quick-run sampling; the Nyquist default of quick-run-per-commit is explicitly overridden by this project's own stricter standing rule.
- **Per wave merge:** full `./gradlew test` at the (possibly newly bumped) locked count.
- **Phase gate:** full suite green at exactly the locked count, guard proven red earlier (per VER-14's own requirement), before `/bm:verify-work`.

### Wave 0 Gaps

- [ ] `gradle/libs.versions.toml` + `library/build.gradle.kts` — add `jna`/`jna-platform` 5.19.1 as `implementation` (legitimacy verified by checksum against Maven Central, 22-CONTEXT.md D-03 — no human-verify checkpoint)
- [ ] `library/src/test/kotlin/com/mordred/aero/internal/windows/` — new test package, no existing files
- [ ] A live-window opt-in verification tool under `tools/` (Kotlin `main()`, following the `tools/capture/` precedent, not a new Gradle source set) — decide the exact shape explicitly in the first plan, per `ARCHITECTURE.md`'s own flagged decision point
- [ ] The RED-state capture described in the VER-11 section above (probe today's unmodified `WindowDraggableArea`-based window before writing any new hit-test code)
- [ ] A UI-Automation-based Snap-Layouts-flyout detector, or an explicit maintainer decision to skip it (Open Question 4)

## Security Domain

`security_enforcement` is absent from `.planning/config.json`, treated as enabled per the default rule.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Not applicable — this is a desktop UI library with no authentication surface |
| V3 Session Management | No | Not applicable |
| V4 Access Control | No | Not applicable |
| V5 Input Validation | Marginal | The only "input" crossing a trust boundary is Win32 message parameters (`wParam`/`lParam`) arriving at the JNA `WindowProc` callback from the OS itself — not attacker-controlled in any normal desktop scenario, but per `PITFALLS.md` Pitfall 7, the callback must defensively catch all exceptions and never assume well-formed values, since an uncaught exception inside a native callback boundary destabilizes the JVM rather than failing safely |
| V6 Cryptography | No | Not applicable — no cryptographic operations in this phase |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Native memory corruption via a dangling JNA callback pointer (GC'd `Callback` object, HWND destroyed mid-callback) | Denial of Service (process-wide crash, not exploitable remotely — this is a desktop app with no network-facing attack surface) | `PITFALLS.md` Pitfall 1's HWND-keyed strong-reference registry; per-HWND install/uninstall ordering (Pitfall 5) |
| Untrusted native library resolution (a malicious/typosquatted JNA fork on the classpath) | Tampering / Elevation of Privilege | Package Legitimacy Audit above — pin exact coordinates and versions, verify via slopcheck where the registry is reachable, gate the dependency-add task behind `checkpoint:human-verify` since this session's slopcheck run could not reach the registry |
| Malformed native callback input causing an uncaught exception to escape the JNA callback boundary | Denial of Service | `PITFALLS.md` Pitfall 7 — top-level `try/catch(Throwable)` in the WndProc callback, fail-safe to `CallWindowProc`/`DefWindowProc` rather than an undefined return value |

This phase's threat surface is narrow and desktop-local (no network, no auth, no user data) — the dominant risk category is availability (native crash), not confidentiality/integrity, and is already the dominant concern driving `PITFALLS.md`'s entire "Critical Pitfalls" section.

## Sources

### Primary (HIGH confidence)
- This repository, read directly this session: `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt`, `ResizeHandles.kt`; `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`; `showcase/build.gradle.kts`; `library/build.gradle.kts`; `gradle/libs.versions.toml`; `jitpack.yml`; `.mcp.json`; `README.md` (grepped); `.planning/REQUIREMENTS.md`; `.planning/STATE.md`; `.planning/ROADMAP.md` Phase 22 section; `.planning/PROJECT.md`; `.planning/config.json`; `.planning/milestones/v3.1-phases/21-migration-release-3-1-0/{21-UNCONFIRMED.md,21-VALIDATION.md}`
- This machine, measured directly this session: `[System.Windows.Forms.Screen]::AllScreens` (one monitor, 1920×1080, 100%), `HKLM:\...\Uninstall\*` registry scan (no PowerToys), `HKCU:\...\StuckRects3` (`Settings` byte 8 = `0x03`, auto-hide ON), `[System.Environment]::OSVersion` + `CurrentBuild` (Windows 11, build 26100), `~/.gradle/caches/modules-2/files-2.1/net.java.dev.jna/` (jna/jna-platform 5.19.1 both present), `grep -rn "JBR|CustomWindowDecoration|jetbrains.runtime|com.jetbrains"` across the repo (zero matches)
- Maintainer memory, read in full this session: `reference_compose_hot_reload_mcp.md`, `reference_windows_mcp_showcase_capture.md`, `feedback_never_ask_user_to_stop_using_pc.md`, `feedback_gui_self_review_before_user.md`, `feedback_repro_must_exercise_path.md`, `feedback_no_process_inflation.md`, `reference_vscode_kotlin_ls_jar_lock.md`
- `.planning/research/{SUMMARY,ARCHITECTURE,STACK,PITFALLS,FEATURES}.md` — milestone-level desk research, already MEDIUM-HIGH confidence, cross-corroborated across four independent passes; treated as authoritative for Win32/JNA mechanics per the phase's own explicit instruction not to re-derive it
- `git log` on `library/build.gradle.kts` — confirmed `ffece58` as the sole prior test-count-guard-introduction commit (no prior bump commits exist to pattern-match against)

### Secondary (MEDIUM confidence)
- `slopcheck scan --pkg maven net.java.dev.jna:jna`/`jna-platform` — attempted this session, returned `REGISTRY_ERROR` (network-unreachable to `search.maven.org` from this sandbox); does not independently confirm the packages this session, see Package Legitimacy Audit

### Tertiary (LOW confidence)
- None new this session beyond what the milestone research already flagged as LOW confidence (`GetSystemMenu`/`TrackPopupMenu` presence in `jna-platform`, JitPack-with-JNA build success) — carried forward as open questions, not re-asserted as fact

## Metadata

**Confidence breakdown:**
- Codebase grounding (file paths, signatures, build/test mechanics): HIGH — everything read directly this session
- Environment availability (monitor count, PowerToys, auto-hide state, JNA cache, JBR grep): HIGH — everything measured directly this session
- Standard stack / Win32 mechanics / architecture / pitfalls: MEDIUM-HIGH — inherited from the milestone's own four cross-corroborated research passes, explicitly not re-derived per this phase's scope
- Package legitimacy: MEDIUM — strong corroborating evidence (local cache, live Maven Central fetch in milestone research, official GitHub org) but slopcheck itself could not reach the registry this session; tagged `[ASSUMED]` per protocol

**Research date:** 2026-09-25
**Valid until:** ~7 days for the environment-availability facts (monitor/PowerToys/auto-hide state could change if the maintainer alters this machine); ~30 days for the codebase-grounding facts (stable until the phase itself starts editing these files); the inherited milestone Win32/JNA research is separately dated 2026-09-25 in its own files with the same validity window
