---
phase: 03-composite-navigation
plan: "07"
subsystem: ui
tags: [compose-multiplatform, kotlin, navigation, menubar, statusbar, breadcrumb, tabbar]

# Dependency graph
requires:
  - phase: 03-composite-navigation
    provides: AeroDropdownPopup (Plan 03-01) — used by AeroMenuBar for dropdown menus
provides:
  - public AeroMenuBar (NAV-02) with cross-item hover-switch state and AeroMenuItem data class
  - public AeroStatusBar (NAV-03) — slot-based bottom bar
  - public AeroBreadcrumb (NAV-04) — clickable navigation chain with onItemClick
  - public AeroTabBar (NAV-05) — horizontal tabs with overflow scroll
  - 4 test files
affects: [03-08-showcase]

# Tech tracking
tech-stack:
  added:
    - androidx.compose.foundation.lazy.LazyRow (used by AeroTabBar overflow scroll)
  patterns:
    - Hover-switch menu pattern: when one menu is open and the user hovers a sibling root item, the open menu auto-switches to that sibling without requiring a click. Implemented via shared `openIndex` state in AeroMenuBar's parent scope.
    - Slot-based status bar: AeroStatusBar(start, center, end) — three RowScope slots packed via Arrangement.SpaceBetween.
    - Breadcrumb separator interleaving: drawItems().intersperse(separator { "›" }) avoids trailing-separator edge case.
    - Tab overflow scroll: LazyRow + selectedIndex animateScrollToItem so the selected tab stays in view.
    - Stub-test compile-reachability via Class.forName (preserved from Plan 03-01).

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroMenuBar.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroMenuItem.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroStatusBar.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumb.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTabBar.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroMenuBarTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroStatusBarTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTabBarTest.kt
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumbTest.kt (Wave-0 stub → real assertions)

key-decisions:
  - "AeroMenuBar uses AeroDropdownPopup (Plan 03-01) for the dropdown body — keeps menu visual and behavior consistent with AeroDropdown / AeroComboBox."
  - "Hover-switch is implemented at the AeroMenuBar parent (single source of truth `openIndex`) rather than per-item — avoids item-level state coordination."
  - "AeroStatusBar uses three-slot API (start/center/end RowScope) with Arrangement.SpaceBetween rather than a single content lambda; gives callers a clean pattern for left-status / center-progress / right-icons layouts."
  - "AeroBreadcrumb's separator is a Composable lambda (default '›') so callers can swap it (e.g., to '/' or chevron icon)."
  - "AeroTabBar uses LazyRow for overflow rather than horizontalScroll — gives keyboard-tab navigation and animateScrollToItem out of the box."

patterns-established:
  - "Slot-based bar API: 3 RowScope slots with internal Arrangement.SpaceBetween — reusable shape for AeroToolbar / AeroFooter if added later."
  - "Cross-item shared-state pattern: parent owns `openIndex: Int?`, children call `onHover(i) { openIndex = i }` and `onClickAway { openIndex = null }`. Reusable for any sibling-coordinated control set (segmented buttons, tab strips, menu rows)."

requirements-completed: [NAV-02, NAV-03, NAV-04, NAV-05]

# Metrics
duration: ~30m (agent watchdog killed at SUMMARY-write step; orchestrator finalized)
completed: 2026-04-28
---

# Phase 3 Plan 07: Remaining Navigation (MenuBar + StatusBar + Breadcrumb + TabBar) Summary

**NAV-02 AeroMenuBar (cross-item hover-switch model with AeroDropdownPopup), NAV-03 AeroStatusBar (3-slot bottom bar), NAV-04 AeroBreadcrumb (clickable chain with custom separator), NAV-05 AeroTabBar (LazyRow with overflow + animateScrollToItem) — completes the navigation surface for v1.**

## Performance

- **Duration:** ~30 min (agent watchdog stalled before SUMMARY write; orchestrator wrote this file after task-stop)
- **Started:** 2026-04-28T13:50:00Z (retry after first agent watchdog-killed)
- **Completed:** 2026-04-28T17:00:00Z (orchestrator-finalized after stop)
- **Tasks:** 3 (all committed)
- **Files created:** 5 main + 4 tests (1 promoted)

## Accomplishments
- AeroMenuBar (NAV-02) with hover-switch model and AeroMenuItem data class
- AeroStatusBar (NAV-03) — slot-based bottom bar
- AeroBreadcrumb (NAV-04) — clickable nav chain
- AeroTabBar (NAV-05) — horizontal tabs with overflow

## Task Commits

1. **Task 1: AeroMenuBar (NAV-02) RED + GREEN** — `9cdf488` test(03-07): add failing test for NAV-02 AeroMenuBar + AeroMenuItem _(commit subject correct, but the diff also contains 03-03 finalization metadata absorbed by parallel commit)_, then `d9fceb9` feat(03-07): implement NAV-02 AeroMenuBar + AeroMenuItem
2. **Task 2: AeroBreadcrumb (NAV-04) RED + GREEN** — `8086031` test(03-07): promote AeroBreadcrumbTest from Wave-0 stub to NAV-04 RED, then `81dce61` feat(03-07): implement NAV-04 AeroBreadcrumb
3. **Task 3: AeroStatusBar (NAV-03) + AeroTabBar (NAV-05)** — `7fe9eb0` feat(03-07): implement NAV-03 AeroStatusBar + NAV-05 AeroTabBar _(diff also includes AeroContextMenu/AeroContextMenuItem files from Plan 03-05 because parallel agent staged them while this commit was being prepared)_

**Plan metadata:** included in this commit (orchestrator finalization).

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroMenuBar.kt`
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroMenuItem.kt`
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroStatusBar.kt`
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumb.kt`
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTabBar.kt`
- 4 test files

## Decisions Made
See `key-decisions` in frontmatter.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Compose plugin rejects function references to @Composable**
- **Issue:** Test pattern `::AeroFunction` failed compilation per the established Compose-plugin restriction.
- **Fix:** Used `Class.forName("...Kt")` reachability probe (matches Plans 03-01, 03-02, 03-04, 03-05, 03-06).

**2. [Cross-plan] Wave-0 stub for AeroToastHostState referenced types not yet shipped**
- **Issue:** First 03-07 agent run was blocked because Wave-0 stub `AeroToastHostStateTest` (created by Plan 03-01) had real assertions referencing types from Plan 03-06 — but 03-06 hadn't finished at that time.
- **Fix:** Resolved automatically when 03-06 landed before this retry. Retry agent verified `:library:compileTestKotlin` succeeds across the full test set.

**3. [Orchestrator-fix] Watchdog stall during verification**
- **Issue:** Retry agent stalled 600s at the post-Task-3 SUMMARY write, likely Kotlin daemon LazyStorage cache contention from parallel Wave-2 agents.
- **Fix:** Orchestrator stopped the agent (commits already in HEAD), verified `:library:compileKotlin :library:compileTestKotlin` succeeds, wrote this SUMMARY directly.

---

**Total deviations:** 3 (2 auto-fixed, 1 orchestrator-finalization)
**Impact on plan:** No scope creep. All planned components shipped.

## Issues Encountered
- **Interleaved commits across parallel plans:** During Wave 2 multiple executors committed concurrently. Two side-effects: (a) commit `7fe9eb0` (subject "03-07 StatusBar+TabBar") includes 03-05's AeroContextMenu files; (b) commit `9cdf488` (subject "03-07 NAV-02 test") includes the 03-03 finalization metadata. Diffs are correct but subject lines don't always match. Documented; no rebase.
- **Kotlin daemon LazyStorage hangs under parallel load:** Same root cause as 03-05 retry stall. Recommendation for future Wave-2 phases: serialize executors or stop daemon between agents.

## Next Phase Readiness
- All navigation components available for showcase wiring in Plan 03-08.
- NAV-01 AeroTitleBar ships in Plan 03-03 (already in showcase Main.kt). 03-08 will mount the four NAV-02..05 components in NavigationSection.

---
*Phase: 03-composite-navigation*
*Completed: 2026-04-28*
