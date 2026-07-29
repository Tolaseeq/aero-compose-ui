---
phase: 16-foundation-aero-primitives-layer
plan: 04
subsystem: ui
tags: [compose, kotlin, interaction-state, hover, focus, press]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer (Plan 01)
    provides: AeroSurfaceStyle / drawAeroSurfaceCore primitives that later components pair with interaction state
provides:
  - components/common/InteractionStates.kt as the single relocated home for hover/press/focus helpers
  - rememberAeroInteractionState(source) — one hover/press/focus connection point (booleans only) for any future component
affects: [17-buttons, 18-range, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "rememberAeroInteractionState(source) bundles hovered/pressed/focused booleans via collectIsHoveredAsState/collectIsPressedAsState/collectIsFocusedAsState; callers pick their own AeroSurfaceStyle variant from the booleans, never colors resolved inside the helper"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
    - library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt

key-decisions:
  - "Kept InteractionStates.kt's existing internal helpers (rememberHoverState/rememberPressedState/rememberFocusState/animatedAlpha, ANIMATION_DURATION_MS) verbatim during the relocation — zero visibility change needed since internal is module-scoped in Kotlin"
  - "rememberAeroInteractionState returns booleans only (AeroInteractionState data class); explicitly does not resolve AeroSurfaceStyle/colors, matching the PRIM-15 prohibition against a second shared color-picking abstraction"

patterns-established:
  - "Pattern 4 (RESEARCH.md): rememberAeroInteractionState(source) — the connection point every Phase 17-19 component wires via Modifier.hoverable/clickable/focusable(interactionSource) + this collector, mirroring AeroListItem's Modifier.hoverable + collectIsHoveredAsState pairing"

requirements-completed: [PRIM-15]

coverage:
  - id: D1
    description: "InteractionStates.kt relocated to components/common/ (single source of truth, old file removed)"
    requirement: "PRIM-15"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt — file existence/grep confirmed via done criteria; full suite green"
        status: pass
    human_judgment: false
  - id: D2
    description: "rememberAeroInteractionState(source) added, returning AeroInteractionState(hovered, pressed, focused) booleans only"
    requirement: "PRIM-15"
    verification:
      - kind: unit
        ref: "InteractionStatesTest.kt#rememberAeroInteractionStateTracksHover, #rememberAeroInteractionStateTracksPress, #rememberAeroInteractionStateTracksFocus"
        status: pass
    human_judgment: false
  - id: D3
    description: "AeroButton/AeroOutlinedButton/AeroIconButton updated to import helpers from components.common; compile and behave identically"
    requirement: "PRIM-15"
    verification:
      - kind: unit
        ref: "./gradlew :library:test (full suite, 256 tests, 0 failures)"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-07-22
status: complete
---

# Phase 16 Plan 04: Interaction States Common Summary

**Relocated InteractionStates.kt to components/common/ and added rememberAeroInteractionState(source), a single boolean hover/press/focus connection point for future switch/segmented-control/slider/range components (PRIM-15).**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-22T14:34:09Z (STATE.md session start)
- **Completed:** 2026-07-22T14:41:40Z
- **Tasks:** 1
- **Files modified:** 6 (1 created source, 1 created test, 1 deleted, 3 import updates)

## Accomplishments
- `InteractionStates.kt` now lives in `com.mordred.aero.components.common`, with the old `components/buttons/InteractionStates.kt` removed — single source of truth
- Added `AeroInteractionState(hovered, pressed, focused)` + `rememberAeroInteractionState(source)`, collecting via `collectIsHoveredAsState`/`collectIsPressedAsState`/`collectIsFocusedAsState`, booleans only (no color resolution)
- `AeroButton`, `AeroOutlinedButton`, `AeroIconButton` updated to import the relocated helpers from `com.mordred.aero.components.common`; behavior unchanged
- `InteractionStatesTest.kt` (3 tests, `runComposeUiTest`) proves hover/press/focus toggle correctly through the new bundled collector

## Task Commits

Each task was committed atomically:

1. **Task 1: Move InteractionStates to components/common, add rememberAeroInteractionState, fix imports** - `575101d` (feat)

**Plan metadata:** (this commit, following SUMMARY.md creation)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` - relocated helpers + new `AeroInteractionState`/`rememberAeroInteractionState`
- `library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt` - hover/press/focus coverage via `runComposeUiTest`
- `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt` - deleted (moved, not copied)
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` - imports updated
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt` - imports updated
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt` - imports updated

## Decisions Made
- Kept all existing helpers (`rememberHoverState`, `rememberPressedState`, `rememberFocusState`, `animatedAlpha`, `ANIMATION_DURATION_MS`) verbatim during the move — `internal` visibility in Kotlin is module-scoped, so the package relocation required no visibility changes.
- `rememberAeroInteractionState` deliberately returns only booleans (`AeroInteractionState`), never resolved `AeroSurfaceStyle`/colors — matches the plan's explicit prohibition against a second shared color-picking abstraction (RESEARCH.md Pattern 4 drift risk).
- Test press simulation used `performMouseInput { moveTo(center); press() }` / `release()` (the actual `MouseInjectionScope` API) rather than the plan's illustrative `down()`/`up()` naming, which doesn't exist on that scope — a straightforward API-name correction, not a behavior change.

## Deviations from Plan

None — plan executed exactly as written. One implementation-detail correction (test API method names `press()`/`release()` instead of the plan's illustrative `down()`/`up()`) was made while writing the test; not a deviation from the plan's intent, just the actual Compose test API surface.

## Issues Encountered
- Local machine hit transient JVM memory allocation failures during the first two `./gradlew :library:test` invocations (`Native memory allocation (malloc) failed`, `Failed to reserve memory for increasing the overflow mark stack capacity`) — resolved by stopping stale Gradle daemons (`./gradlew --stop`) and retrying with `-Dorg.gradle.jvmargs=-Xmx1g`. Not a code issue; environment-only.

## Next Phase Readiness
- `rememberAeroInteractionState` is available at `com.mordred.aero.components.common` for Phase 17 (Buttons), Phase 18 (Range/sliders), and Phase 19 (Selectors/Lists — `AeroSwitch`, `AeroSegmentedControl`) to consume without importing from the buttons package.
- Full `./gradlew :library:test` suite green: 256 tests, 0 failures, 0 errors — no regression on the three button components.
- Remaining Phase 16 plans (16-02 draw-primitives-expansion, 16-05 verification-spike-smoke) are still incomplete per init context and unaffected by this plan.

---
*Phase: 16-foundation-aero-primitives-layer*
*Completed: 2026-07-22*

## Self-Check: PASSED

- FOUND: library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt
- CONFIRMED REMOVED: library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt
- FOUND commit: 575101d
