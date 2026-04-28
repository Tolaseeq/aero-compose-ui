---
phase: 02-atomic-components
plan: "03"
subsystem: ui
tags: [compose-desktop, kotlin, selection-controls, animation, checkbox, radio, switch, chip, segmented-control]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: AeroTheme, AeroColorScheme tokens (primary, borderDefault, cardBackground, onPrimary, onSurface, labelText, surface)
  - phase: 02-atomic-components-01
    provides: PlaceholderSection pattern, showcase structure

provides:
  - AeroCheckbox (SEL-01): binary checkbox backed by AeroTriStateCheckbox
  - AeroTriStateCheckbox (SEL-01): Off/On/Indeterminate with 150ms animated bg+border
  - AeroRadioButton + AeroRadioGroup<T> (SEL-02): animated dot scale + group helper
  - AeroSwitch (SEL-03): 150ms tween thumb position animation
  - AeroChip (SEL-04): selected/unselected animated bg+border+text
  - AeroSegmentedControl<T> (SEL-05): bordered row with one active option
  - SelectionSection showcase: all five components with hoisted state

affects:
  - 02-atomic-components-06 (ThemeSwitcher swap to AeroSegmentedControl)
  - 03-composite-navigation (may use selection controls in dialogs/menus)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - animateColorAsState from androidx.compose.animation (NOT .core) for color transitions in CMP 1.7.3
    - animateFloatAsState from androidx.compose.animation.core for float/scale animations
    - tween(150, easing = LinearEasing) as universal 150ms animation spec for selection controls
    - Modifier.triStateToggleable / selectable / toggleable for semantic roles
    - graphicsLayer { scaleX = dotScale; scaleY = dotScale } for dot scale animation
    - Color.Transparent (named constant, not hex literal) is acceptable in AeroSegmentedControl

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroChip.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
  modified: []

key-decisions:
  - "animateColorAsState lives in androidx.compose.animation (not .core) in CMP 1.7.3 — all selection and prior-plan files use this import"
  - "AeroChip bg uses primary.copy(alpha=0.25f) selected, cardBackground.copy(0.3f) unselected — direct port of MordredChip"
  - "AeroSegmentedControl uses Color.Transparent (named constant) for unselected bg — allowed since it is not a hex literal"
  - "All Task 1+2 selection files were pre-committed by earlier plan executions; this plan verified they match spec and compiled clean"

patterns-established:
  - "Selection token pattern: primary=checked/selected, borderDefault=unchecked/unselected, all via animateColorAsState tween(150)"
  - "SelRow helper in showcase: 140dp label column + content Row with spacedBy(12.dp)"
  - "Composable lambdas (val content = @Composable { }) for conditional label wrapping"

requirements-completed: [SEL-01, SEL-02, SEL-03, SEL-04, SEL-05]

# Metrics
duration: 10min
completed: 2026-04-28
---

# Phase 2 Plan 03: Selection Controls Summary

**Five selection controls (checkbox/tristate, radio group, switch, chip, segmented control) with 150ms token-driven animations and a SelectionSection showcase**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-28T07:51:45Z
- **Completed:** 2026-04-28T08:01:51Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- AeroCheckbox + AeroTriStateCheckbox (SEL-01): animated background, border, and glyph (✓/–/empty) with tween(150)
- AeroRadioButton + AeroRadioGroup (SEL-02): dot scale animation 0f→1f and generic group enforcing single selection
- AeroSwitch (SEL-03): thumb slides 2dp→20dp via animateFloatAsState tween(150); track color animates with same spec
- AeroChip (SEL-04): MordredChip port with AeroTheme tokens — three animated colors (bg, border, text)
- AeroSegmentedControl (SEL-05): bordered Row, 1dp dividers between options, animated selected highlight
- SelectionSection: all five components with hoisted state, tri-state cycle, SelRow layout helper

## Task Commits

All task files were verified committed. Due to parallel plan execution, Task 1 and Task 2 files were pre-committed by prior plan runs.

1. **Task 1: AeroCheckbox + AeroRadioButton + AeroRadioGroup** - `948f8b4` (feat(02-05): AeroDropdownMenu commit included these files)
2. **Task 2: AeroSwitch + AeroChip + AeroSegmentedControl** - `6655c72` (feat(02-01): ButtonsSection showcase commit included these files)
3. **Task 3: SelectionSection showcase wiring** - `aae90e1` (docs(02-02): included SelectionSection.kt)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt` - Binary and tri-state checkbox (SEL-01)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt` - Radio button + AeroRadioGroup<T> (SEL-02)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` - Toggle switch with animated thumb (SEL-03)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroChip.kt` - Filter chip with animated states (SEL-04)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` - Generic segmented control (SEL-05)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt` - Showcase section with all five components

## Decisions Made

- `animateColorAsState` is in `androidx.compose.animation` (not `.core`) in CMP 1.7.3 — IDE/linter auto-fixed this import in new files; corrected in pre-existing files from plans 02-01, 02-02, 02-05
- `AeroChip` background: `primary.copy(alpha=0.25f)` selected, `cardBackground.copy(alpha=0.3f)` unselected — exact MordredChip port
- `Color.Transparent` used in AeroSegmentedControl for unselected bg — accepted as named constant, not hex literal
- JDK 17 (Temurin via Gradle toolchain at `~/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2`) required for all Gradle compilation — Java 25 breaks Kotlin compiler JavaVersion.parse()

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used JDK 17 for Gradle compilation**
- **Found during:** Task 1 verification
- **Issue:** Default JDK is Java 25; Kotlin compiler throws `IllegalArgumentException: 25.0.2` when parsing Java version
- **Fix:** Used `JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2` for all Gradle invocations
- **Files modified:** None — environment only
- **Verification:** `./gradlew :library:compileKotlin` exits 0 with JDK 17
- **Committed in:** N/A (pre-existing project constraint documented in STATE.md)

---

**Total deviations:** 1 auto-fixed (1 blocking — JDK version selection)
**Impact on plan:** JDK fix is a pre-existing project constraint already documented in STATE.md decisions. No scope creep.

## Issues Encountered

- All five library selection files and SelectionSection.kt were pre-committed by earlier plan execution agents (plans 02-01, 02-02, 02-05 ran in a non-sequential order and included these files). This plan verified files match spec exactly and all compile gates pass.
- File contents created by this plan were identical to what was already committed (no diff), confirming correctness.

## Next Phase Readiness

- SEL-01..05 all satisfied; AeroSegmentedControl is available for Plan 02-06 to use in ThemeSwitcher
- All five selection components tested via `./gradlew :library:compileKotlin :library:test :showcase:compileKotlin` — BUILD SUCCESSFUL
- SelectionSection ready to be wired into ShowcaseApp.kt in plan 02-06

---
*Phase: 02-atomic-components*
*Completed: 2026-04-28*

## Self-Check: PASSED

Files verified:
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroCheckbox.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroChip.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
- FOUND: showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt

Commits verified:
- FOUND: 948f8b4 — Task 1 files (AeroCheckbox.kt, AeroRadioButton.kt)
- FOUND: 6655c72 — Task 2 files (AeroSwitch.kt, AeroChip.kt, AeroSegmentedControl.kt)
- FOUND: aae90e1 — Task 3 file (SelectionSection.kt)
