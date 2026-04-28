---
phase: 03-composite-navigation
plan: "03"
subsystem: ui
tags: [compose-multiplatform, kotlin, window-chrome, undecorated-window, framewindowscope, window-draggable-area, resize-handles, win11]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: AeroTheme, AeroColorScheme tokens (titleBarGradientStart/End, titleBarText, buttonHover, closeButtonHover)
  - phase: 03-composite-navigation
    provides: Win11 transparent=false rule (battle-tested in Plan 03-04 dialogs); FrameWindowScope idiom for window-scoped chrome
provides:
  - public FrameWindowScope.AeroTitleBar composable (NAV-01) — 32dp gradient row, 46x32 control buttons (Min/Max-Restore/Close), WindowDraggableArea, theme-driven colors
  - public FrameWindowScope.AeroResizeHandles composable — 8-zone resize layout (4 edges + 4 corners) with native java.awt.Cursor *_RESIZE_CURSOR icons via Modifier.pointerHoverIcon
  - showcase Main.kt switched to Window(undecorated = true, transparent = false) — first non-decorated window in the project
  - AeroTitleBarTest — compile-only reachability test (FrameWindowScope cannot be invoked headlessly)
affects: [03-05-popover-tooltip-context, 03-07-menubar-statusbar-breadcrumb-tabs, 03-08-showcase]

# Tech tracking
tech-stack:
  added:
    - androidx.compose.foundation.window.WindowDraggableArea (CMP 1.7.3)
    - androidx.compose.ui.window.FrameWindowScope receiver type
    - androidx.compose.ui.input.pointer.PointerIcon + Modifier.pointerHoverIcon
    - java.awt.Cursor (8 native *_RESIZE_CURSOR constants)
    - androidx.compose.foundation.gestures.detectDragGestures
  patterns:
    - "Window chrome split: AeroTitleBar.kt for visual chrome, ResizeHandles.kt for resize behavior — single-responsibility per file"
    - "8-zone resize overlay sits AS A SIBLING of the chrome+content Column inside a fillMaxSize Box, so resize zones overlap content edges without disrupting layout"
    - "Top/Left edges and TL/TR/BL corners must mutate BOTH windowState.size AND windowState.position to keep the anchored opposite edge fixed; Bottom/Right edges and BR corner only mutate size"
    - "windowState.position mutation guarded by `is WindowPosition.Absolute` cast — PlatformDefault positions are not mutable in v1"
    - "WindowPlacement.Floating gate: AeroResizeHandles early-returns when placement is Maximized/Fullscreen so resize zones do not interfere with maximized window"
    - "Visibility: AeroResizeHandles is `public` (not `internal`) because :showcase and :library are separate Gradle modules; Kotlin `internal` would block cross-module access"
    - "Win11 rule (third source-of-truth): showcase Main.kt enforces undecorated = true / transparent = false explicitly; combined with AeroDialog (Plan 03-04) the rule is now applied at top-level Window AND OS-level DialogWindow"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/Main.kt

key-decisions:
  - "AeroTitleBar declared as FrameWindowScope.AeroTitleBar(...) extension — required so the composable can call WindowDraggableArea (which itself is a FrameWindowScope extension)."
  - "ResizeHandles.kt is a separate file from AeroTitleBar.kt — chrome and resize are orthogonal concerns; keeps each file focused and testable."
  - "AeroResizeHandles is public, not internal — the showcase module needs to call it across the Gradle module boundary; revisit visibility tightening when/if a navigation-internal-only helper is added later."
  - "Top/Left edge handlers + TL/TR/BL corner handlers compute deltaApplied = oldSize - newSize, then shift windowState.position by deltaApplied so the opposite (anchored) edge stays fixed. Bottom/Right handlers do not need this because growing down/right does not require moving the window origin."
  - "Min size is hardcoded to 320 x 240 dp inside AeroResizeHandles. Matches the showcase's rememberWindowState minimums; not yet a parameter, can be lifted in v2 if a consumer needs different minimums."
  - "Theme propagation gap: AeroTheme wraps the window content but theme state lives inside ShowcaseApp — meaning the titlebar always renders AeroBlue colors regardless of the active showcase theme. This is a known v1 limitation; Plan 03-08 will lift the theme state to Main.kt so the titlebar follows the active theme."
  - "Aero Snap is explicitly out of scope: WindowDraggableArea does NOT pass HTCAPTION to the OS, so dragging to a screen edge does not trigger Win11 native snap. Documented in AeroTitleBar KDoc and PROJECT.md Out-of-Scope."
  - "AeroTitleBarTest uses assertTrue(true) sentinel — FrameWindowScope cannot be instantiated headlessly. The test exists purely so `:library:test` touches the test source file, transitively forcing AeroTitleBar.kt to compile."

patterns-established:
  - "FrameWindowScope-extension chrome pattern: top-level window-chrome composables (titlebar, status bar, menu bar) should be declared as `fun FrameWindowScope.X(...)` so they have direct access to WindowDraggableArea + window-level effects. Future NAV-02 (AeroMenuBar) and NAV-03 (AeroStatusBar) may follow this pattern."
  - "8-zone resize overlay pattern: `Box(Modifier.fillMaxSize()) { Column { chrome; content }; AeroResizeHandles(state) }` — the resize Box sits AFTER the column so its zones paint on top of content edges. Reusable verbatim by any future undecorated-window app."
  - "Per-zone PointerIcon caching: the 8 native cursors are built once via `remember { PointerIcon(Cursor(Cursor.X_RESIZE_CURSOR)) }` and referenced by each Box. Cheaper than per-recomposition allocation; idiomatic for any pointer-icon overlay."
  - "Compile-only smoke test for FrameWindowScope-bound composables: assertTrue(true) is acceptable because the test class's mere presence forces the source file to compile. Manual VALIDATION.md is the primary verification."

requirements-completed: [NAV-01]

# Metrics
duration: ~10m (Tasks 1+2 implementation, Task 3 manual smoke verification, finalization)
completed: 2026-04-28
---

# Phase 3 Plan 03: AeroTitleBar + Window Chrome Summary

**NAV-01 AeroTitleBar (FrameWindowScope-extension chrome with 32dp gradient row, draggable area, and Min/Max-Restore/Close glyph buttons) + AeroResizeHandles (8-zone resize overlay with native N/S/E/W/NE/NW/SE/SW cursors) + showcase switched to undecorated Window(undecorated = true, transparent = false). First non-decorated window in the project, manual smoke confirmed: drag/min/max/restore/close + all 8 resize zones with correct cursor changes, no Win11 EXCEPTION_ACCESS_VIOLATION crash.**

## Performance

- **Duration:** ~10 min (Task 1 + Task 2 + manual smoke at Task 3 + finalization)
- **Started:** 2026-04-28T16:16:41Z (Task 1 commit)
- **Completed:** 2026-04-28T16:22:01Z (Task 2 commit; Task 3 manual smoke approved later)
- **Tasks:** 3 (Tasks 1+2 auto, Task 3 manual checkpoint approved by user)
- **Files created:** 3 (AeroTitleBar.kt, ResizeHandles.kt, AeroTitleBarTest.kt)
- **Files modified:** 1 (showcase/Main.kt)

## Accomplishments

- NAV-01 AeroTitleBar ships as a `FrameWindowScope` extension. The composable builds a 32.dp tall vertical-gradient row using `colors.titleBarGradientStart`/`titleBarGradientEnd`, wraps the row in `WindowDraggableArea`, and renders three 46×32 dp control buttons (Minimize ─, Maximize □ / Restore ❒, Close ✕) with hover backgrounds (`colors.buttonHover` for Min/Max, `colors.closeButtonHover` for Close). Each button uses `hoverable(interactionSource) + clickable(onClick)` so click reliably wins over the surrounding drag area.
- AeroResizeHandles ships in a separate `ResizeHandles.kt` file. It exposes a single `public fun FrameWindowScope.AeroResizeHandles(windowState: WindowState)` that overlays 8 invisible Boxes inside a `Box(Modifier.fillMaxSize())`: 4 edges (Top, Bottom, Right, Left at 4.dp thickness) and 4 corners (TL, TR, BL, BR at 8.dp size). Each box sets a `Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.X_RESIZE_CURSOR)))` for the matching native cursor, and a `Modifier.pointerInput { detectDragGestures }` that mutates `windowState.size` (and `windowState.position` for top/left edges and TL/TR/BL corners so the anchored opposite edge stays fixed).
- Showcase `Main.kt` switched from a default decorated `Window(...)` to `Window(..., undecorated = true, transparent = false)`. The mandatory `transparent = false` is in source code (grep-checkable) per the Win11 EXCEPTION_ACCESS_VIOLATION rule (CMP-3757 / GH#3171). AeroTitleBar + ShowcaseApp render in a Column; AeroResizeHandles overlays the whole window inside the same fillMaxSize Box.
- Manual smoke approved by user: app launches without Win11 native crash; titlebar drag, minimize, maximize, restore, and close all work; all 8 resize zones (N/S/E/W/NE/NW/SE/SW) show correct native cursors on hover and resize correctly; resize handles disable when window is maximized.
- AeroTitleBarTest is a compile-reachability sentinel — FrameWindowScope cannot be invoked headlessly, so `assertTrue(true)` documents the intent that the test exists to force AeroTitleBar.kt into the test compile graph.

## Task Commits

Each task was committed atomically:

1. **Task 1: AeroTitleBar composable + compile-reachability test** - `76fc662` (feat)
   - FrameWindowScope.AeroTitleBar — 32dp gradient row, 46x32 control buttons, mordred port (Min ─, Max □, Restore ❒, Close ✕)
   - WindowDraggableArea wraps Row; per-button clickable wins over drag
   - AeroTitleBarTest is compile-only (FrameWindowScope receiver cannot be invoked headlessly)
2. **Task 2: 8-zone AeroResizeHandles + switch showcase to undecorated window** - `8f63ca4` (feat)
   - ResizeHandles.kt with 4 edges + 4 corners, native java.awt.Cursor *_RESIZE_CURSOR per zone via pointerHoverIcon
   - detectDragGestures mutates windowState.size; top/left/top-corner zones also shift windowState.position
   - public visibility (showcase calls across Gradle module boundary)
   - showcase/Main.kt: Window(undecorated = true, transparent = false) — Win11 rule enforced explicitly
3. **Task 3: Manual smoke checkpoint** - no code commit (human-verify gate; user approved drag/min/max/restore/close + all 8 resize zones with correct native cursors and no Win11 crash)

**Plan metadata:** _will land with the docs commit_ (this SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md + 03-03-PLAN.md update)

## Files Created/Modified

**Main sources (created):**
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` — public FrameWindowScope.AeroTitleBar with 32dp gradient row, draggable area, three control buttons reading theme tokens (titleBarGradientStart/End, titleBarText, buttonHover, closeButtonHover)
- `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` — public FrameWindowScope.AeroResizeHandles overlay: 4 edges (4dp thick) + 4 corners (8dp), each with pointerHoverIcon(native cursor) + detectDragGestures mutating windowState.size and (for anchored edges) windowState.position

**Test sources (created):**
- `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt` — compile-only smoke (assertTrue(true) sentinel; FrameWindowScope is not headlessly instantiable)

**Modified:**
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` — Window(undecorated = true, transparent = false), AeroTheme wrap, AeroTitleBar mounted at top of FrameWindowScope, AeroResizeHandles overlay sibling, ShowcaseApp content beneath the titlebar

## Decisions Made

- **AeroTitleBar is a FrameWindowScope extension.** Required so the composable can call `WindowDraggableArea` (itself a FrameWindowScope extension). This locks the API to "must be invoked inside a Window content lambda" — which is fine because AeroTitleBar has no meaning outside a window.
- **Resize logic lives in its own file (ResizeHandles.kt), not inside AeroTitleBar.kt.** Chrome and resize are orthogonal concerns. AeroTitleBar.kt stays focused on visual chrome (gradient + buttons + drag); ResizeHandles.kt owns the 8-zone overlay and pointerInput plumbing. Future window-chrome plans (NAV-03 status bar, NAV-02 menu bar) will live in their own files following the same convention.
- **AeroResizeHandles is `public`, not `internal`.** `:showcase` and `:library` are separate Gradle modules — Kotlin `internal` would block cross-module access. The plan originally suggested `internal` but flagged the rebasing during action; final choice was public because there is no usable internal-only seam yet.
- **Top/Left edges + TL/TR/BL corners mutate BOTH size AND position.** The math: `deltaApplied = oldSize - newSize`, then `position += deltaApplied`. This anchors the opposite edge so dragging the top edge down does not also move the bottom edge down. Bottom/Right edges and BR corner only mutate size (no position shift needed when growing down/right).
- **`windowState.position` mutation is guarded by `is WindowPosition.Absolute` cast.** `WindowPosition.PlatformDefault` (the initial state from `rememberWindowState` before user interaction) cannot be mutated; the first drag at top/left edges silently won't move the window position. Acceptable v1 behavior; subsequent drags after the OS has assigned an absolute position will work correctly.
- **Min size 320 × 240 dp is hardcoded inside AeroResizeHandles.** Matches the showcase's rememberWindowState minimums. Not currently a parameter — can be lifted in v2 if a consumer needs custom minimums; keeping it hardcoded simplifies the v1 API.
- **Theme propagation limitation accepted.** AeroTheme wraps the window body but the showcase's theme state lives inside ShowcaseApp, so the titlebar always renders AeroBlue colors regardless of which theme the user picks in ThemeSwitcher. This is documented in the Plan and explicitly deferred to Plan 03-08 (which will lift theme state to Main.kt).
- **Aero Snap is out of scope.** `WindowDraggableArea` does not pass HTCAPTION to the OS, so Windows native snap-to-half / snap-to-quadrant won't trigger when the user drags the titlebar to a screen edge. Aero Snap requires a JNI/WinAPI shim and is explicitly v2+. Documented in AeroTitleBar KDoc.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] AeroResizeHandles visibility lifted from `internal` to `public`**
- **Found during:** Task 2 (showcase Main.kt cannot import AeroResizeHandles)
- **Issue:** The plan's `<action>` block initially declared `internal fun FrameWindowScope.AeroResizeHandles(...)`. Because `:library` and `:showcase` are separate Gradle modules in this project, Kotlin's per-module `internal` modifier blocks cross-module access — `:showcase` could not call AeroResizeHandles. The plan itself flagged this in the corrective note: "if `internal` causes a 'cannot access AeroResizeHandles' error from showcase, change to `public`."
- **Fix:** Declared `public fun FrameWindowScope.AeroResizeHandles(windowState: WindowState)` in ResizeHandles.kt (per the plan's own corrective note).
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt`
- **Verification:** `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0; manual smoke confirms resize works from showcase.
- **Committed in:** `8f63ca4` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 Rule 3 blocking — pre-flagged by plan)
**Impact on plan:** The plan anticipated this exact deviation and pre-described the fix. No scope change.

### User-Accepted Visual Issue (out of scope, documented for follow-up)

**White border flashes during window resize on Win11.**
- During the manual smoke checkpoint the user noticed a brief white border flash around the window during active resize drags. They explicitly classified this as minor visual polish and out of scope for this plan ("skip if difficult"). No code change was attempted in this plan.
- **Likely cause (hypothesis, not investigated):** the flash may be the OS's default window background painting briefly during a resize cycle, before AeroResizeHandles + the inner content can re-paint. A future polish plan could investigate setting the AWT root window background to match the AeroTheme background, or wrapping the content in a synchronously-painted opaque Box.
- **Status:** accepted by user; revisit only if it bothers users in production.

## Issues Encountered

- **AeroDropdown popup offset + trailing empty space (reported by user during this checkpoint).** The user noticed during manual smoke that the AeroDropdown popup is offset to the right of its anchor and shows extra empty space below the last item. **This is NOT a regression in Plan 03-03 — Plan 03-03 did not touch AeroDropdown.** It is most likely a side-effect of Plan 03-01's retrofit of `AeroDropdownPopup` to wrap its inner Column in `AeroScrollArea` (a known patterns-established decision: "AeroDropdownPopup uses AeroScrollArea wrapping inner Column (preserves vertical padding)"). The wrapping introduces extra padding that shows up as empty space below the last item, and the popup's positioning may need re-checking against the new content size. **Defer to a gap-closure plan or follow-up task.** Not investigated or fixed here.

## Next Phase Readiness

- **Plans 03-05 (anchored popups), 03-07 (remaining nav), 03-08 (showcase wiring)** all unblocked by Plan 03-03. None of them depend on the resize logic or AeroTitleBar internals.
- **Plan 03-08 must lift theme state from ShowcaseApp to Main.kt** so the AeroTitleBar follows the active theme. This is recorded as a known v1 limitation and is the explicit gate Plan 03-08 will close.
- **The Win11 transparent=false rule is now applied at three sources** (showcase Main.kt top-level Window, AeroDialog from Plan 03-04, and any future DialogWindow). Future window-creating composables should follow the same grep-checkable pattern.
- **Aero Snap remains v2+ (HTCAPTION via JNI/WinAPI).** Recorded in AeroTitleBar KDoc and STATE.md Blockers.

## Self-Check: PASSED

**Files verified on disk (in HEAD):**
- library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt — FOUND
- library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt — FOUND
- library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt — FOUND
- showcase/src/main/kotlin/com/mordred/showcase/Main.kt — FOUND (modified to undecorated = true, transparent = false)

**Commits verified in git log:**
- 76fc662 (Task 1: AeroTitleBar composable + compile-reachability test) — FOUND
- 8f63ca4 (Task 2: 8-zone AeroResizeHandles + switch showcase to undecorated window) — FOUND

**Manual smoke verification:**
- Drag titlebar — PASSED (user-confirmed)
- Minimize / Restore from taskbar — PASSED
- Maximize / Restore — PASSED
- Close button — PASSED (clean exit)
- 4 edges (N/S/E/W) cursor + resize — PASSED
- 4 corners (NE/NW/SE/SW) cursor + resize — PASSED
- No Win11 EXCEPTION_ACCESS_VIOLATION crash — PASSED

**Acceptable visual issues noted (not regressions):**
- White border flash during resize — user-accepted, deferred
- AeroDropdown popup offset + trailing empty space — pre-existing 03-01 side effect, deferred to follow-up

---
*Phase: 03-composite-navigation*
*Completed: 2026-04-28*
