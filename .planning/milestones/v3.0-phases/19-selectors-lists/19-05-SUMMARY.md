---
phase: 19-selectors-lists
plan: 05
subsystem: ui
tags: [compose-desktop, kotlin, focus-visible, accessibility, gap-closure]

# Dependency graph
requires:
  - phase: 19-02
    provides: "Restyled AeroSwitch with hover/press/focus states and rememberAeroInteractionState wiring"
provides:
  - "Internal, Compose-free FocusVisibility/reduce/rememberFocusVisible mechanism in InteractionStates.kt"
  - "AeroInteractionState.focusVisible field, populated by rememberAeroInteractionState for every consumer"
  - "AeroSwitch's focus glow ring gated on keyboard-acquired focus only, closing gap G2 (VSEL-02)"
affects: [19-06, 19-07, 19-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Interaction-derived focus-visible reducer (pure state machine folding Interaction over FocusVisibility) as the library's answer to focus-visible semantics, in place of LocalInputModeManager — the single shared mechanism 19-06/19-07 will consume in one line each"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt

key-decisions:
  - "Deviated from 19-UAT.md's G2 fix_direction wording (LocalInputModeManager.current.inputMode == InputMode.Keyboard) in favor of a pure interaction-derived reducer — see 'Deviations from Plan' below for the full reasoning from this plan's planner_finding."
  - "AeroInteractionState gained a 4th positional field (focusVisible) rather than a second parallel collector type — every other consumer (AeroButton, AeroSlider, AeroRangeSlider, AeroListItem, AeroSegmentedControl) keeps reading hovered/pressed/focused unchanged and renders byte-identically."

requirements-completed: [VSEL-02]

coverage:
  - id: D1
    description: "AeroSwitch's focus glow ring draws only for keyboard-acquired focus (Tab); a mouse click leaves no residual ring, including after the pointer leaves — the reported G2 symptom"
    requirement: VSEL-02
    verification:
      - kind: unit
        ref: "FocusVisibilityTest.kt#mouseClickPressBeforeFocusIsNotVisible, #mouseClickFocusBeforePressIsNotVisible, #reportedSymptomResidualRingAfterPointerExitIsSuppressed, #tabFocusAloneIsVisible"
        status: pass
      - kind: unit
        ref: "AeroSwitchSourceTest.kt#aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag"
        status: pass
    human_judgment: true
    rationale: "The observable G2 contract on the live AeroSwitch (mouse click leaves no ring, Tab draws one, hover unchanged) is confirmed by a human in 19-08 across three themes, per this plan's own <verification> section — unit tests prove the reducer and the source wiring, not the rendered pixels."
  - id: D2
    description: "The reducer's idempotency and concurrency probes (VSEL-02): clicking twice in a row stays suppressed both times; hover and keyboard focus co-active both draw and stay independently readable"
    requirement: VSEL-02
    verification:
      - kind: unit
        ref: "FocusVisibilityTest.kt#idempotencyASecondPressAfterMouseClickStaysNotVisible, #concurrencyHoverAfterKeyboardFocusStaysVisibleAndHoveredIsTrue"
        status: pass
    human_judgment: false
  - id: D3
    description: "Keyboard operability (focus stop + Space toggle) is unaffected by the focus-visible gate"
    requirement: VSEL-02
    verification:
      - kind: unit
        ref: "AeroSwitchSemanticsTest.kt#aeroSwitchStaysFocusedAndSpaceStillTogglesAfterTheFocusVisibleGate"
        status: pass
    human_judgment: false

# Metrics
duration: 15min
completed: 2026-07-27
status: complete
---

# Phase 19 Plan 05: Focus-visible reducer, AeroSwitch focus ring (gap G2) Summary

**Interaction-derived `FocusVisibility` reducer in the shared collector suppresses AeroSwitch's focus glow ring for pointer-acquired focus while keeping it for Tab, with hover untouched — closing gap G2 (VSEL-02)**

## Performance

- **Duration:** 15 min
- **Started:** 2026-07-27T15:45:39Z (approx, after 19-04 close-out commit)
- **Completed:** 2026-07-27T15:49:59Z
- **Tasks:** 2
- **Files modified:** 4 (1 new test file, 1 new resolver in an existing file, 1 component behaviour change, 2 existing test files extended)

## Accomplishments

- Added `FocusVisibility` (internal data class), `FocusVisibility.reduce(Interaction)` (internal extension), and `rememberFocusVisible(source)` (Composable) to `InteractionStates.kt` — a pure, Compose-free state machine deciding whether a focus cue should be DRAWN, independent of whether the element IS focused.
- Added a 4th field `focusVisible: Boolean` to `AeroInteractionState`, populated by `rememberAeroInteractionState` — the single shared collection point every Aero component already uses, so 19-06/19-07 can consume the mechanism in one line each.
- `AeroSwitch`'s FOCUS `aeroGlowRing` call now reads `state.focusVisible && enabled` instead of `state.focused && enabled`; the HOVER `aeroGlowRing` call is byte-identical, still reading `state.hovered && enabled`.
- 9 pure JVM unit tests in `FocusVisibilityTest.kt` cover Tab-focus, mouse-click ordering (both press-before-focus and focus-before-press), the reported residual-ring symptom, VSEL-02 idempotency and concurrency probes, unfocus/refocus recovery, and keyboard (Space) activation while already focused.
- New source guard in `AeroSwitchSourceTest.kt` asserts the FOCUS ring reads `focusVisible` and the HOVER ring still reads the raw `hovered` flag; new semantics test in `AeroSwitchSemanticsTest.kt` proves focus stop and Space-toggle both survive the gate.

## Task Commits

Each task was committed atomically:

1. **Task 1: focus-visible reducer in the shared collector, wired end-to-end through AeroSwitch's focus glow ring** - `36b291a` (feat)
2. **Task 2: source guard and keyboard-operability regression test for the switch's focus gate** - `79b11a5` (test)

_TDD note: Task 1 is `tdd="true"`. The test file (`FocusVisibilityTest.kt`) was written first and confirmed to fail at compile time (`Unresolved reference 'FocusVisibility'`) before `InteractionStates.kt`/`AeroSwitch.kt` were implemented; RED and GREEN landed in a single commit per this plan's task granularity (the plan did not require separate `test(...)`/`feat(...)` commits for this task), matching this phase's established single-commit-per-task convention (19-01..19-04)._

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` - `FocusVisibility`, `FocusVisibility.reduce`, `rememberFocusVisible`, `AeroInteractionState.focusVisible`
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` - FOCUS `aeroGlowRing` gated on `state.focusVisible`; KDoc focus paragraph updated to document the gap G2 gate
- `library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt` - new, 9 pure JVM tests, no Compose runtime
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` - new guard for the FOCUS/HOVER ring split
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt` - new keyboard-operability regression case

## Guard Fail-Then-Pass Proof

Per this project's fail-then-pass convention (VER-06), the new source guard
(`AeroSwitchSourceTest.kt#aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag`)
was proven to FAIL against the pre-fix argument before being trusted:

1. Temporarily reverted `AeroSwitch.kt`'s FOCUS `aeroGlowRing` call's `active` argument from
   `state.focusVisible && enabled` back to `state.focused && enabled` (the raw flag, matching the
   shipped 19-02 behaviour that produces gap G2's symptom).
2. Ran `./gradlew :library:test --tests "*AeroSwitchSourceTest*"`:
   ```
   > Task :library:test FAILED

   AeroSwitchSourceTest > aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag() FAILED
       org.opentest4j.AssertionFailedError at AeroSwitchSourceTest.kt:112

   8 tests completed, 1 failed
   ```
   Confirmed FAIL — the guard correctly rejects the pre-fix source.
3. Restored `AeroSwitch.kt`'s FOCUS `aeroGlowRing` call's `active` argument to
   `state.focusVisible && enabled`.
4. Re-ran `./gradlew :library:test --tests "*AeroSwitchSourceTest*"` — exited 0, all 9 tests pass
   (the guard is silent evidence, not printed per-test names, but the task returned success with
   no failures reported).
5. Ran the full `./gradlew :library:test` suite after restoring — exited 0, no regression in
   `AeroButton`/`AeroSlider`/`AeroRangeSlider`/`AeroListItem`/`AeroSegmentedControl`, all of which
   share `AeroInteractionState`.

`git diff --stat` for `AeroSwitch.kt` after the revert-and-restore cycle showed zero net changes —
the file returned to its Task 1 committed state exactly, so no extra commit was needed for the
proof itself.

## Decisions Made

- **Interaction-derived gate instead of the `LocalInputModeManager` gate named in 19-UAT.md's G2
  `fix_direction`** (recorded per this plan's `<planner_finding>` and the plan's `output` spec).
  19-UAT.md's `fix_direction` proposed gating on
  `LocalInputModeManager.current.inputMode == InputMode.Keyboard`. Planning-time evidence from
  inspecting the resolved Compose Multiplatform 1.11.1 desktop artifacts in the Gradle cache showed:
  - The only non-manager class calling `requestInputMode` in `ui-desktop-1.11.1.jar` is
    `androidx.compose.ui.node.RootNodeOwner`, referencing both `InputMode.Touch` and
    `InputMode.Keyboard` — which pointer types map to which mode is not determinable from the
    bytecode alone.
  - `foundation-desktop-1.11.1.jar` ships `InputModeFilterIndication`/`InputModeFilterInteractionSource`,
    which does implement an input-mode-based focus-visible filter on desktop — but it applies to
    the platform `Indication`, which this library deliberately disables everywhere
    (`indication = null`, P-01). The platform's own mechanism is therefore inert for any Aero
    component.
  - Net: if a desktop mouse press requests `InputMode.Touch`, the input-mode gate would work; if it
    requests `InputMode.Keyboard` (Compose's general "non-touch input" classification), the gate
    would be a silent no-op and gap G2 would survive unfixed. This risk was judged unacceptable for
    a fix whose entire purpose is closing G2.
  - The `fix_direction` mechanism was a diagnosing agent's suggestion during UAT triage, not a
    locked user decision — 19-CONTEXT.md has no D-XX entry on focus-visible semantics — so
    deviating from its literal wording while preserving its **intent** (the observable contract:
    mouse click → no ring, Tab → ring, hover unchanged, element stays focusable/operable) was
    within the plan's own explicit design, not an unplanned deviation requiring a checkpoint.
  - The interaction-derived reducer is correct under both possible `InputMode` behaviours, needs no
    platform assumption, and is unit-testable without a Compose runtime — all properties the
    input-mode gate lacks.
- **`AeroInteractionState` widened by one field rather than introducing a second collector type** —
  every other component reading `hovered`/`pressed`/`focused` (AeroButton, AeroOutlinedButton,
  AeroSlider, AeroRangeSlider, AeroListItem, AeroSegmentedControl) needed zero code changes and
  renders byte-identically; only `AeroSwitch` in this plan opted into the new `focusVisible` field.
  Confirmed via the full `./gradlew :library:test` suite passing with no regressions.

## Deviations from Plan

**1. [Explicitly planned deviation, not a Rule 1-4 auto-fix] Interaction-derived gate instead of
`LocalInputModeManager`** — see "Decisions Made" above and this plan's own `<planner_finding>`
section, which pre-authorized and required this deviation to be recorded in the SUMMARY. This is
not a Rule 1-4 auto-fix; it was the plan's designed approach from the start, chosen during planning
specifically because the UAT-recorded `fix_direction` carried unverifiable platform-behaviour risk.

No Rule 1-4 auto-fixes were needed during execution — the plan's tasks, `<behavior>` spec, and
`<action>` instructions were followed exactly, and both tasks' acceptance criteria (grep counts,
test class contents, whole-suite green) were met without additional code changes beyond what the
plan specified.

## Issues Encountered

None. Both tasks executed cleanly: the RED compile failure in `FocusVisibilityTest.kt` was the
expected TDD signal (not an issue), and the whole-library test suite (`./gradlew :library:test`)
was green after each task and after the fail-then-pass proof's revert-and-restore cycle.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `FocusVisibility`/`rememberFocusVisible`/`AeroInteractionState.focusVisible` are in place and
  proven end-to-end on `AeroSwitch` — 19-06 (`AeroListItem`) and 19-07 (`AeroSegmentedControl`) can
  consume `state.focusVisible` in their own focus-stroke gates with no further mechanism work.
- `AeroButton` and `AeroSlider` (Phase 17/18 accepted work) were deliberately left reading the raw
  `focused` flag per this plan's scope fence — they render byte-identically after this plan, verified
  by the full test suite. Adopting `focusVisible` library-wide remains a follow-on decision.
- The observable G2 contract on the live `AeroSwitch` (mouse click leaves no ring, Tab draws one,
  hover unchanged, in all three themes) is NOT yet human-confirmed — that happens in 19-08's
  re-sign-off, per this plan's own `<verification>` section.
- G4 (ornament token brightness) remains untouched — no file under
  `library/src/main/kotlin/com/mordred/aero/theme/` was modified in this plan, per the scope fence.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27*

## Self-Check: PASSED

All created/modified files verified present on disk; both task commits (`36b291a`, `79b11a5`) verified present in git log.
