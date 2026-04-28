---
phase: 03-composite-navigation
plan: "01"
subsystem: ui
tags: [compose-multiplatform, kotlin, popup, scrollbar, theme, foundation]

# Dependency graph
requires:
  - phase: 02-atomic-components
    provides: AeroDropdown, AeroComboBox, AeroTextArea (now retrofitted with public popup + scrollbar)
provides:
  - public components/popup/ package (AeroPopupSide, AeroPopupPositionProvider, AeroCursorPositionProvider, AeroDropdownPopup, AeroDropdownItem)
  - CNT-05 AeroScrollArea + CNT-06 AeroScrollBar
  - LocalScrollbarStyle wired into AeroTheme so any Foundation VerticalScrollbar inside the theme picks up Aero tokens automatically
  - AeroTextArea visibly shows AeroScrollBar overlay
  - AeroDropdownPopup wraps scrollable content in AeroScrollArea
  - 9 Wave-0 stub-test files covering all phase-3 future components
affects: [03-02-containers, 03-03-titlebar, 03-04-dialog-drawer, 03-05-popover-tooltip-context, 03-06-toast-banner, 03-07-menubar-breadcrumb-tabs, 03-08-showcase]

# Tech tracking
tech-stack:
  added:
    - androidx.compose.foundation.LocalScrollbarStyle
    - androidx.compose.foundation.ScrollbarStyle
    - androidx.compose.foundation.VerticalScrollbar / rememberScrollbarAdapter
  patterns:
    - public popup wrapper exposed via components/popup/ for reuse across composite components
    - scrollbar styling via CompositionLocal (single source of truth in AeroTheme)
    - smart popup positioning with side parameter + auto-flip on overflow + window-bounds clamp

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/popup/PopupSide.kt
    - library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt
    - library/src/main/kotlin/com/mordred/aero/components/popup/AeroCursorPositionProvider.kt
    - library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt
    - library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt
    - library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollArea.kt
    - library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/popup/AeroCursorPositionProviderTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/containers/AeroScrollAreaTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/containers/AeroScrollBarTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroToastHostStateTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroContextMenuItemTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumbTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt (imports from popup/)
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt (imports from popup/)
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroTextArea.kt (AeroScrollBar overlay)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt (LocalScrollbarStyle)
  deleted:
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt (split into popup/AeroDropdownPopup.kt)

key-decisions:
  - "AeroPopupSide enum (Top/Bottom/Start/End) with auto-flip + window-bounds clamp covers Bottom→Top, Top→Bottom, Start→End, End→Start cases for all anchored popups (DRP, OVL-03/04/07)."
  - "AeroDropdownPopup retrofit uses AeroScrollArea wrapping inner Column(padding(vertical=4.dp)) — preserves existing vertical padding while adding visible Aero scrollbar."
  - "AeroTextArea retrofit uses AeroScrollBar overlay (not AeroScrollArea wrapper) because BasicTextField needs to manage its own verticalScroll modifier order; both share the same ScrollState."
  - "LocalScrollbarStyle in AeroTheme: 12.dp thickness, 6.dp rounded shape, cardBackground unhover, borderSelected hover, 150ms transition — single source of truth for any VerticalScrollbar inside the theme."
  - "Stub-test compile-reachability uses Class.forName(\"...Kt\") instead of `::AeroComposable` function reference, because Kotlin Compose plugin disallows function refs to @Composable functions (Rule 1 auto-fix to plan-supplied stub code)."
  - "AeroCursorPositionProvider takes IntOffset cursor (window-local) and clamps to window bounds; ready for AeroContextMenu in Plan 03-05."

patterns-established:
  - "Public popup primitives: any new anchored popup composable in this library should use AeroPopupPositionProvider(side=...) + AeroCursorPositionProvider — no per-component re-implementation."
  - "Scrollable container pattern: AeroScrollArea wraps content + permanent AeroScrollBar; alternative is AeroScrollBar overlay sharing a ScrollState with a sibling verticalScroll target (used by AeroTextArea)."
  - "Theme tokens via CompositionLocal: AeroTheme installs LocalScrollbarStyle so future overlays / drawers / lists pick up styling for free."

requirements-completed: [CNT-05, CNT-06]

# Metrics
duration: 7m
completed: 2026-04-28
---

# Phase 3 Plan 01: Foundation (Popup + Scrollbar) Summary

**Public components/popup/ package with auto-flip AeroPopupPositionProvider, AeroCursorPositionProvider for right-click menus, CNT-05/CNT-06 scrollbar primitives, AeroTheme-wide LocalScrollbarStyle, and Phase-2 AeroDropdownPopup + AeroTextArea retrofits — load-bearing foundation for Phase 3 Plans 02-08.**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-04-28T12:53:06Z
- **Completed:** 2026-04-28T12:59:39Z
- **Tasks:** 4 of 4
- **Files modified/created:** 16 created, 4 modified, 1 deleted

## Accomplishments

- Promoted internal popup primitives to public `components/popup/` package, enabling reuse across Plans 03-04, 03-05, 03-06, 03-07.
- Extended `AeroPopupPositionProvider` with `side: AeroPopupSide` parameter and full four-direction auto-flip + window-bounds clamp.
- Built `AeroCursorPositionProvider` for right-click context menus (clamps cursor to window).
- Shipped CNT-05 `AeroScrollArea` and CNT-06 `AeroScrollBar` wrapping Foundation `VerticalScrollbar` + `rememberScrollbarAdapter`.
- Installed `LocalScrollbarStyle` inside `AeroTheme` (12.dp thickness, RoundedCornerShape(6.dp), cardBackground/borderSelected tokens) so any `VerticalScrollbar` in the library now picks up Aero styling automatically.
- Retrofitted `AeroTextArea` to use `AeroScrollBar` overlay (sibling Box) and `AeroDropdownPopup` to wrap scrollable content in `AeroScrollArea` — no bare `verticalScroll` remains in either component.
- Authored 9 Wave-0 stub test files; popup-position-provider tests run 7 green assertions (Bottom/Top/Start/End primary placement, four auto-flip cases, both-sides-overflow clamp).

## Task Commits

1. **Task 1: Create Wave-0 stub tests** — `c853c2e` (test)
2. **Task 3 partial — AeroScrollBar + AeroScrollArea foundation** — `6d858a1` (feat)
3. **Task 2: Promote popup infrastructure to public components/popup/** — `76ec272` (refactor)
4. **Task 3 remainder: AeroTheme LocalScrollbarStyle + AeroTextArea retrofit** — `ce47fd6` (feat)
5. **Task 4 fix: stub-test Class.forName probe (deviation Rule 1)** — `b5e3a88` (fix)

_Note on commit ordering:_ Plan Task 3 documents that Steps A+B of Task 3 had to run before Task 2 Step D so the AeroDropdownPopup's `import AeroScrollArea` resolved at compile time. Commits follow that physical dependency rather than the plan's logical task numbering — same outcome, every commit compiles standalone.

**Plan metadata commit:** to follow this commit (this SUMMARY + STATE.md + ROADMAP.md).

## Files Created/Modified

**Created (main):**
- `library/src/main/kotlin/com/mordred/aero/components/popup/PopupSide.kt` — `enum class AeroPopupSide { Top, Bottom, Start, End }`.
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt` — public class with side + gap, auto-flip + clamp.
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroCursorPositionProvider.kt` — public cursor-anchored provider.
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt` — public popup wrapper + AeroDropdownItem; wraps content in AeroScrollArea.
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt` — CNT-06 standalone scrollbar.
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollArea.kt` — CNT-05 scrollable container.

**Created (tests):** 9 stub-test files under `popup/`, `containers/`, `overlay/`, `navigation/` — full list in frontmatter.

**Modified:**
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` — imports `AeroDropdownPopup` and `AeroDropdownItem` from `components.popup`.
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt` — imports `AeroDropdownPopup`, `AeroDropdownItem`, and `AeroPopupPositionProvider` from `components.popup`.
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroTextArea.kt` — wraps `BasicTextField` + `AeroScrollBar` overlay in a Box; right-side padding reserves space behind the scrollbar.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — installs `LocalScrollbarStyle` with Aero tokens; adds three imports.

**Deleted:**
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt` — content split into `popup/AeroDropdownPopup.kt` (Git tracks this as a rename).

## Decisions Made

See key-decisions in frontmatter. Most consequential:

- **Auto-flip semantics:** `overflows()` checks all four window edges before flipping; `opposite()` reflects across both axes (Bottom↔Top, Start↔End); final `clamp()` guarantees non-negative offsets even when popup exceeds window in both directions. This satisfies OVL-03 / OVL-04 / OVL-07 simultaneously.
- **AeroDropdownPopup retrofit nests `AeroScrollArea { Column(padding(vertical=4.dp)) }`:** preserves the prior visual padding behavior while adding a visible scrollbar. The nested Column is a slight structural cost, but eliminates a subtle visual regression where the first/last items would touch the popup border.
- **AeroTextArea uses overlay (not wrapper):** `BasicTextField` needs `verticalScroll(state)` directly on its modifier to keep cursor-into-view behavior working; wrapping it in `AeroScrollArea` would require a separate state and break input ergonomics. The Box overlay shares the same `ScrollState` so visual + behavioral coherence is preserved.
- **Class.forName probe instead of `::AeroComposable`:** Compose plugin restriction on @Composable function refs forced the substitution; functionally equivalent (still fails at runtime if the file is renamed/removed).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Stub-test `::Composable` references fail to compile**
- **Found during:** Task 4 (cumulative compile gate)
- **Issue:** `AeroScrollAreaTest.kt` and `AeroScrollBarTest.kt` used `::AeroScrollArea` / `::AeroScrollBar` as compile-reachability probes. Kotlin Compose plugin disallows function references to `@Composable` functions, so `compileTestKotlin` failed with "Function References of @Composable functions are not currently supported".
- **Fix:** Replaced the function-reference probe with `Class.forName("com.mordred.aero.components.containers.AeroScrollAreaKt")` — Kotlin top-level functions live in a synthetic `<File>Kt` class, so this still proves the file compiled and is loadable from the classpath without crossing into the Compose-restricted zone.
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/containers/AeroScrollAreaTest.kt`, `library/src/test/kotlin/com/mordred/aero/components/containers/AeroScrollBarTest.kt`
- **Verification:** `./gradlew :library:compileKotlin :library:compileTestKotlin :library:test :showcase:compileKotlin` exits 0 after the fix. `:library:test` reports 7 green tests in `AeroPopupPositionProviderTest`, 2 in `AeroCursorPositionProviderTest`, 1 each in the two containers tests, plus the 5 forward-referencing overlay/navigation stubs — all green.
- **Committed in:** `b5e3a88`

**2. [Rule 3 - Blocking] JAVA_HOME unset / Java 25 on PATH**
- **Found during:** First `./gradlew :library:compileKotlin` invocation
- **Issue:** Initial Gradle invocation failed with "What went wrong: 25.0.2" — matches the documented Phase 1 decision that `JAVA_HOME must be JDK 17 (Gradle toolchain) for all Gradle invocations — Java 25 version string breaks Kotlin compiler JavaVersion.parse()`.
- **Fix:** Exported `JAVA_HOME=C:/Users/1/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2` and prepended `$JAVA_HOME/bin` to PATH for every Gradle invocation in this plan.
- **Files modified:** none (environmental fix only)
- **Verification:** `./gradlew :library:compileKotlin` succeeds with JDK 17 on PATH.
- **Committed in:** N/A (environmental, no source change). Documented here so future agents know the workaround.

---

**Total deviations:** 2 auto-fixed (1 Rule 1 bug in plan-supplied stub code, 1 Rule 3 environmental gate per existing project decision).
**Impact on plan:** Both deviations are mechanical; no behavioral or scope drift. The popup auto-flip contract, AeroScrollArea/AeroScrollBar API, AeroTheme scrollbar wiring, and both retrofits ship exactly as the plan specified.

## Issues Encountered

- None beyond the two deviations above. The plan's strategic note in Task 3 (execute Steps A+B before Task 2 Step D) matched the actual physical compile-order constraint precisely; commits were sequenced accordingly.

## User Setup Required

None — all changes are library-internal Kotlin source. No environment variables or external services involved.

## Next Phase Readiness

- **Plans 03-04, 03-05, 03-06, 03-07** can import `AeroDropdownPopup`, `AeroPopupPositionProvider(side=...)`, `AeroCursorPositionProvider(cursor)` directly from `com.mordred.aero.components.popup` for AeroDialog content-list, AeroPopover, AeroTooltip, AeroContextMenu, AeroMenuBar, and AeroBreadcrumb auto-flip overflow.
- **Plan 03-04** (AeroDialog/AeroDrawer) can wrap dialog content in `AeroScrollArea` for tall dialogs.
- **Wave-0 stubs** for OVL-02, OVL-04, OVL-05, OVL-06, NAV-04 are in place — Plans 03-04 through 03-07 fill in real assertions when their components ship.
- No blockers introduced.

## Self-Check: PASSED

Verified files on disk:
- FOUND: library/src/main/kotlin/com/mordred/aero/components/popup/PopupSide.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/popup/AeroCursorPositionProvider.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollArea.kt
- FOUND: 9 stub-test files under popup/, containers/, overlay/, navigation/
- MISSING: library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt (intentional — deleted per plan)

Verified commits:
- FOUND: c853c2e (Wave-0 stubs)
- FOUND: 6d858a1 (AeroScrollBar + AeroScrollArea)
- FOUND: 76ec272 (popup package promotion)
- FOUND: ce47fd6 (AeroTheme + AeroTextArea retrofit)
- FOUND: b5e3a88 (stub-test compile fix)

Verified compile + test gate:
- `./gradlew :library:compileKotlin :library:compileTestKotlin :library:test :showcase:compileKotlin` exits 0.

---
*Phase: 03-composite-navigation*
*Plan: 01*
*Completed: 2026-04-28*
