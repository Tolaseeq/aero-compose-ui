---
phase: 22-native-window-behavior-release-3-2-0
plan: 02
subsystem: ui
tags: [jna, win32, wndproc, aero-titlebar, compose-desktop, hot-reload]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "Plan 01's live-window probe harness (tools/winprobe/), RED baseline, F8 finding (child HWND is the hit-test subclass target)"
provides:
  - "JNA 5.19.1 (jna, jna-platform) as an internal-only dependency of :library"
  - "internal/windows package: Win32Interop.kt (constants + hand-declared AeroUser32 extension interface + isWindowsOs + chromeTrace), WndProcSupport.kt (dispatchSafely, decodeScreenPoint, classifySpikeTitleRow), Win32Chrome.kt (ensureNativeFrameStyles), AeroWndProc.kt (AeroFrameWndProc, AeroChildWndProc), NativeWindowChromeRegistry.kt (HWND-keyed install/acquire/release registry)"
  - "AeroTitleBar.kt wired to install/release native chrome via a Windows-only DisposableEffect(window)"
  - "22-NOTES.md '## Spike (22-02)' section: C1 (after), C4 and C7 (runtime half) settled against a live window on both standard JDK 21 and JBR 21"
affects: [22-03, 22-04, 22-05, 22-07, 22-09, 22-10]

# Tech tracking
tech-stack:
  added: ["net.java.dev.jna:jna:5.19.1 (implementation)", "net.java.dev.jna:jna-platform:5.19.1 (implementation)"]
  patterns:
    - "HWND-keyed ConcurrentHashMap registry holding strong Callback references, recorded before SetWindowLongPtr takes effect, owner-counted for idempotent Hot-Reload-safe acquire/release"
    - "Every WindowProc callback wrapped in dispatchSafely (Throwable -> CallWindowProc fallback); every unhandled message forwarded to CallWindowProc(previous, ...) verbatim"
    - "Child HWND (Skiko's SunAwtCanvas) subclassed alongside the frame, answering HTTRANSPARENT wherever the frame would answer non-client, so the real OS hit-test chain reaches the frame"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/WndProcSupport.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Chrome.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt
    - library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt
  modified:
    - gradle/libs.versions.toml
    - library/build.gradle.kts
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md

key-decisions:
  - "JNA/jna-platform 5.19.1 checksums re-confirmed against the resolved local Gradle cache jars, matching D-03 exactly; implementation scope only, zero leakage onto :showcase's compile classpath"
  - "C1 (after): all five style bits (WS_CAPTION, WS_SYSMENU, WS_THICKFRAME, WS_MINIMIZEBOX, WS_MAXIMIZEBOX) present after install; V11-HT-CAPTION and V11-HT-MAX now PASS through the real child-to-frame hit-test chain"
  - "C4: WindowState.placement still syncs Maximized/Floating with zero explicit push code even with the subclass live and answering WM_NCHITTEST/WM_NCCALCSIZE -- CallWindowProc passthrough for WM_SIZE/WM_SYSCOMMAND does not disturb AWT's own sync pipeline"
  - "C7 runtime half: cold standard-JDK 21 and hot JBR 21 runs produce byte-identical style/hit-test/maximize answers; three Hot Reload cycles held exactly one live install with unchanged hit-test answers throughout, no WNDPROC stacking"
  - "New finding (not previously predicted): the spike's unconditional WM_NCCALCSIZE -> 0 handler produces an 8px maximized overhang past the monitor on every edge -- expected, deferred to a later plan, not a regression"

patterns-established:
  - "internal/windows/ package: every declaration internal, no JNA/com.sun.jna type reaches a public signature or AeroTitleBar.kt"
  - "aero.chromeTrace diagnostic hook (AERO_CHROME event=... lines), gated the same way as WindowStateReporter's aero.windowState from Plan 01"

requirements-completed: [DEP-01, SNAP-01, SNAP-02, VER-11]

# Metrics
duration: 35min
completed: 2026-09-25
---

# Phase 22 Plan 02: JNA Dependency + Native WndProc Subclass Spike Summary

**Added JNA 5.19.1 as an internal-only dependency and built the minimal native WndProc subclass (frame + child HWND) that answers WM_NCHITTEST with HTCAPTION/HTMAXBUTTON through Windows' real hit-test chain on a standard JDK 21, proven live on both cold and JBR Hot Reload runs.**

## Performance

- **Duration:** 35 min (14:37 start of file reading through 15:12 final commit)
- **Started:** 2026-09-25T14:37:00+03:00
- **Completed:** 2026-09-25T15:11:49+03:00
- **Tasks:** 3
- **Files modified:** 9 (5 created, 4 modified)

## Accomplishments

- JNA 5.19.1 (`jna`, `jna-platform`) added as `implementation`-scope to `:library`, SHA-1 re-confirmed against the resolved jars (matches D-03 exactly); `:showcase` compile classpath carries zero `net.java.dev.jna` entries
- Built the `com.mordred.aero.internal.windows` package: Win32 constants, a hand-declared `AeroUser32` extension interface for the entry points `jna-platform` 5.19.1 doesn't ship, `AeroFrameWndProc`/`AeroChildWndProc` (own exactly WM_NCCALCSIZE/WM_NCHITTEST/WM_NCDESTROY, everything else `CallWindowProc(previous, ...)`), and `NativeWindowChromeRegistry` (HWND-keyed, strong-ref-holding, idempotent install/acquire/release)
- Wired `AeroTitleBar.kt` to install/release the native chrome via a Windows-only `DisposableEffect(window)`, with `WindowDraggableArea` and every button left unchanged (per this spike's scope)
- Proved live against the real showcase window on both a standard JDK 21 (`gradlew run`) and JBR 21 (`hotRun`): `WS_CAPTION`/`WS_THICKFRAME` present after install, `caption`→2 and `max`→9 through the real child→frame `HTTRANSPARENT` bounce (previously 1/1 on the RED baseline), `WindowState.placement` still syncs automatically, and three Hot Reload cycles held exactly one live install with unchanged hit-test answers throughout

## Task Commits

1. **Task 1: JNA 5.19.1 as an internal dependency of :library (DEP-01, D-03)** - `5e91d95` (build)
2. **Task 2: Spike — native WndProc subclass with the structural invariants** - `3cf67f4` (feat)
3. **Task 3: Headless spike checks — conflicts #1 (after), #4, #7 (runtime)** - `0a22b5b` (docs)

**Plan metadata:** (this commit)

## Files Created/Modified

- `gradle/libs.versions.toml` - `jna`/`jna-platform` 5.19.1 catalog entries
- `library/build.gradle.kts` - `implementation(libs.jna)` / `implementation(libs.jna.platform)`, internal-only comment
- `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Interop.kt` - Win32 constants, `AeroUser32` extension interface, `isWindowsOs`, `chromeTrace`
- `library/src/main/kotlin/com/mordred/aero/internal/windows/WndProcSupport.kt` - `dispatchSafely`, `decodeScreenPoint`, `classifySpikeTitleRow` (pure-JVM, unit-testable)
- `library/src/main/kotlin/com/mordred/aero/internal/windows/Win32Chrome.kt` - `ensureNativeFrameStyles` (adds `WS_CAPTION`/`WS_THICKFRAME`, invalidates cached frame geometry)
- `library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt` - `AeroFrameWndProc`, `AeroChildWndProc`
- `library/src/main/kotlin/com/mordred/aero/internal/windows/NativeWindowChromeRegistry.kt` - HWND-keyed install/acquire/release registry
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` - Windows-only `DisposableEffect(window)` installing/releasing native chrome
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-NOTES.md` - `## Spike (22-02)` section: C1 (after), C4, C7 (runtime) settled with observed values

## Decisions Made

- JNA/jna-platform 5.19.1 checksums re-confirmed against the resolved local Gradle cache jars (matches D-03); `implementation` scope only
- C1 (after): all five style bits present post-install; `V11-HT-CAPTION`/`V11-HT-MAX` now PASS through the real chain (spike state, not full GREEN — edges/corners/maximize-geometry/min-size are later steps)
- C4: `WindowState.placement` syncs automatically with zero explicit push code, confirmed with the subclass live (not just the RED baseline)
- C7 runtime half: cold JDK 21 and hot JBR 21 produce byte-identical answers; three Hot Reload cycles proved no WNDPROC stacking and no behavior drift
- New finding: the spike's maximized state shows an 8px overhang past the monitor (naive `WM_NCCALCSIZE` → 0 regardless of maximized state) — expected, tracked as a later-plan fix, not addressed here

## Deviations from Plan

None — plan executed exactly as written. Task 2's file contents follow the plan's `<action>` specification closely; the only judgment calls were implementation-detail sequencing already covered by the plan's own "before returning" language (the registry entry is recorded even earlier than the literal wording requires — before `SetWindowLongPtr` rather than merely before `acquire()` returns — which is strictly safer and still satisfies the stated acceptance criterion).

## Issues Encountered

- The IDE's Kotlin language server repeatedly reported stale diagnostics (unresolved references, JVM target mismatches, incompatible metadata version) on every edit in this session — a known, previously-documented issue on this machine (mismatched cached Kotlin compiler/stdlib versions in the language server, unrelated to the real Gradle build). Verified all real compilation/test results via `./gradlew` directly instead; every `:library:compileKotlin`/`:showcase:compileKotlin`/`:library:test` run was green.
- `Invoke-WinProbe.ps1 -Launch hotRun -KeepRunning`'s own console output did not surface through this session's backgrounded Bash tool (the underlying PowerShell process completed and the window came up successfully, confirmed via direct process/window inspection) — worked around by re-invoking the probe with `-Launch none` against the already-running window by title, which is a supported mode of the existing tool.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The native chrome spike is proven end-to-end on a live window on both the standard JDK the published library targets and the JBR the showcase's Hot Reload path runs on; `NativeWindowChromeRegistry`'s idempotent install/reuse path is confirmed safe across Hot Reload
- Plan 04 (the real-input gate, per this plan's objective) can proceed: the `HTMAXBUTTON` hit-test answer is in place for the Snap Layouts flyout to be tested against
- Known, deliberately out-of-scope gap for a later plan: the maximized-state overhang (`WM_NCCALCSIZE` returning 0 unconditionally) — recorded in `22-NOTES.md`, not a regression, not silently dropped
- No showcase/gradle process launched during this plan was left running; confirmed via process/window inspection after cleanup. `git diff --quiet HEAD -- library/` held after the temporary Hot Reload test edit was reverted

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-25*
