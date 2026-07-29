---
phase: 19-selectors-lists
plan: 09
subsystem: ui
tags: [compose-desktop, kotlin, focus-visible, interaction-source, aero-switch, aero-button]

# Dependency graph
requires:
  - phase: 19-selectors-lists (plan 05)
    provides: FocusVisibility reducer / rememberFocusVisible mechanism (gap G2)
  - phase: 19-selectors-lists (plan 08)
    provides: Phase 19 human sign-off + verification report identifying CR-02/WR-01
provides:
  - "FocusVisibility.reduce's Unfocus branch preserves the pointer-derived hovered field, closing CR-02"
  - "Composed end-to-end reachability proof against a real AeroSwitch (AeroSwitchFocusVisibleWiringTest)"
  - "AeroButtonSurface's focus glow gated on focusVisible, matching AeroSwitch/AeroSegmentedControl/AeroListItem (WR-01)"
affects: [19-10, 19-11, 19-12]

tech-stack:
  added: []
  patterns:
    - "Fold reducer contract proven both by a hand-folded Interaction list (fast, deterministic) and a composed runComposeUiTest against a real component (reachability proof)"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchFocusVisibleWiringTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
    - library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt

key-decisions:
  - "Unfocus branch changed to FocusVisibility(hovered = hovered) — one-argument fix, no field-shape change (WR-04's counted-hover alternative is deliberately not taken here; see 19-11)"
  - "AeroButtonSurface's focus glow gate moved to state.focusVisible; hover glow and resolveButtonStyle's dead focused parameter left untouched, per scope fence"

patterns-established:
  - "One shared FocusVisibility reducer now correctly serves four consumers (AeroSwitch, AeroSegmentedControl, AeroListItem, AeroButtonSurface) with one gesture producing one cue class library-wide"

requirements-completed: [VSEL-02, VSEL-04, VLST-03]

coverage:
  - id: D1
    description: "FocusVisibility.reduce's Unfocus branch preserves hovered instead of resetting it, closing the six-interaction CR-02 repro (Enter, Press, Focus, Unfocus, Focus, Press ends suppressed)"
    requirement: "VSEL-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt#mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt (9 pre-existing cases, incl. recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The CR-02 defect is reachable end-to-end against a real AeroSwitch driving the real modifier chain and a real MutableInteractionSource"
    requirement: "VSEL-04"
    verification:
      - kind: integration
        ref: "library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchFocusVisibleWiringTest.kt#mouseClickAfterTabAwayAndBackWithAStationaryPointerDrawsNoFocusCue"
        status: pass
    human_judgment: false
  - id: D3
    description: "AeroButtonSurface's focus glow now gates on focusVisible (WR-01), matching the other three components; hover glow untouched"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt#aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag"
        status: pass
      - kind: integration
        ref: "./gradlew :library:test --tests \"*AeroButtonSemanticsTest*\" (click + Space/Enter activation unaffected)"
        status: pass
    human_judgment: false
  - id: D4
    description: "VLST-03 (AeroListItem focus visual) unblocked at the reducer level — AeroListItem consumes the same fixed FocusVisibility.reduce"
    requirement: "VLST-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt (reducer-level fix, shared by all consumers)"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 09: CR-02 focus/hover reducer fix + WR-01 button focus-gate alignment Summary

**Fixed the one reducer branch that dropped the pointer-derived `hovered` field on focus loss (CR-02), proved the repro end-to-end against a real `AeroSwitch`, and moved `AeroButtonSurface`'s focus glow onto the same `focusVisible` gate the other three Phase 19 components already use (WR-01).**

## Performance

- **Duration:** ~20 min
- **Completed:** 2026-07-28
- **Tasks:** 2
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments

- `FocusVisibility.reduce`'s `Unfocus` branch now constructs `FocusVisibility(hovered = hovered)` instead of a bare default instance, so losing focus no longer wipes the pointer-derived `hovered` field it has no business touching
- Added the missing six-interaction regression case (`mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed`) to `FocusVisibilityTest.kt` — the fold test the shipped suite stopped one interaction short of
- Added `AeroSwitchFocusVisibleWiringTest.kt`, a composed `runComposeUiTest` proof that drives a real `AeroSwitch`, a real `MutableInteractionSource` and a real focus move between two nodes, closing the reachability gap the fold test alone cannot close
- Moved `AeroButtonSurface`'s focus `aeroGlowRing` call onto `state.focusVisible && enabled` (was `state.focused && enabled`), so `AeroButton`/`AeroOutlinedButton` now draw one cue class for one gesture, consistent with `AeroSwitch`/`AeroSegmentedControl`/`AeroListItem`
- Added `aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag` source guard to `AeroButtonSurfaceSourceTest.kt`, mirroring `AeroSwitchSourceTest`'s guard shape
- VSEL-02, VSEL-04 and VLST-03 are unblocked at the shared-reducer level for all three consuming components simultaneously

## Task Commits

Each task was committed atomically:

1. **Task 1: preserve the pointer-derived field across a focus loss, and prove the repro end-to-end (CR-02)** - `1ad5414` (fix)
2. **Task 2: put the button's focus glow on the same gate as everything else (WR-01)** - `0c4cad9` (fix)

_Note: no plan-metadata commit was made separately per this project's `commit_docs` convention — STATE.md/ROADMAP.md updates are captured in the final metadata commit below._

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` - `Unfocus` branch preserves `hovered`; reason recorded inline in the reducer's `when` arm
- `library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt` - added the missing six-interaction regression case (10th `@Test`, 9 pre-existing unchanged)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchFocusVisibleWiringTest.kt` - new file, composed end-to-end reachability proof against a real `AeroSwitch`
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - focus glow gate moved to `state.focusVisible`; KDoc focus paragraph rewritten to state the new gate, the library-wide consistency it buys, and that focusability/activation are unaffected
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` - added `aeroButtonSurfaceSource` accessor and one new guard test (6th `@Test`, 5 pre-existing unchanged)

## Decisions Made

- The reducer fix is the minimal one-argument change (`FocusVisibility(hovered = hovered)`), not a restructuring of `FocusVisibility`'s field shape — WR-04's counted-hover alternative is deliberately deferred to plan 19-11, per the plan's explicit prohibition
- `resolveButtonStyle`'s dead `focused` parameter was left untouched — removing it is out of this round's scope and it is already documented as intentionally unused
- The composed wiring test never calls `exit()` on the switch node (verified via a literal grep for `HoverInteraction.Exit`/`exit()` returning 0) — the whole repro depends on the pointer never leaving the control

## Deviations from Plan

**1. [Rule 1 - Bug in test authoring, self-corrected before commit] KDoc wording in `AeroSwitchFocusVisibleWiringTest.kt` accidentally matched the acceptance-criteria's forbidden literal strings**
- **Found during:** Task 1, before committing — running the acceptance-criteria grep for `HoverInteraction.Exit|exit()` returned 2 instead of the required 0
- **Issue:** The test's class KDoc explained the "never exits" invariant using the literal substrings `HoverInteraction.Exit` and `exit()`, which the plan's acceptance criteria specifically greps for as a proxy that no hover-exit is emitted anywhere in the file, including comments
- **Fix:** Reworded the KDoc to describe the invariant without using either literal substring ("no hover-exit interaction is ever emitted")
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchFocusVisibleWiringTest.kt`
- **Verification:** Re-ran the grep (returns 0) and re-ran the test suite (still green) before committing
- **Committed in:** `1ad5414` (part of Task 1 commit — caught before any commit was made)

---

**Total deviations:** 1 auto-fixed (Rule 1, caught pre-commit during self-verification)
**Impact on plan:** No scope creep — a self-correction during acceptance-criteria verification, not a change to behavior or test intent.

## Guard Fail-Then-Pass Proof

**Task 1 — reducer fix + regression tests:**

1. Wrote `mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed` (fold test) and `AeroSwitchFocusVisibleWiringTest.mouseClickAfterTabAwayAndBackWithAStationaryPointerDrawsNoFocusCue` (composed test) against the **unfixed** reducer (`Unfocus -> FocusVisibility()`).
2. Ran `./gradlew :library:test --tests "*FocusVisibilityTest*" --tests "*AeroSwitchFocusVisibleWiringTest*"`:
   ```
   AeroSwitchFocusVisibleWiringTest > mouseClickAfterTabAwayAndBackWithAStationaryPointerDrawsNoFocusCue() FAILED
   FocusVisibilityTest > mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed() FAILED
   11 tests completed, 2 failed
   ```
   Both new tests RED; observed the 9 pre-existing `FocusVisibilityTest` cases still passing.
3. Applied the fix (`Unfocus -> FocusVisibility(hovered = hovered)`) plus the inline KDoc comment.
4. Re-ran the same command:
   ```
   TEST-...FocusVisibilityTest.xml: tests="10" failures="0"
   TEST-...AeroSwitchFocusVisibleWiringTest.xml: tests="1" failures="0"
   ```
   Both new tests GREEN, all 10 `FocusVisibilityTest` cases (9 pre-existing + 1 new) pass, `AeroSwitchFocusVisibleWiringTest` passes.
5. Ran the full `./gradlew :library:test` and `./gradlew build` — both green.

**Task 2 — button source guard:**

1. Added `aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag` (and its `aeroButtonSurfaceSource` accessor) to `AeroButtonSurfaceSourceTest.kt`, against the **unfixed** `AeroButtonSurface.kt` (focus glow still reading `state.focused`).
2. Ran `./gradlew :library:test --tests "*AeroButtonSurfaceSourceTest*" --rerun`:
   ```
   AeroButtonSurfaceSourceTest > aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag() FAILED
   org.opentest4j.AssertionFailedError at AeroButtonSurfaceSourceTest.kt:96
   6 tests completed, 1 failed
   ```
   New guard RED; the other 5 pre-existing guards passed.
3. Applied the fix (`active = state.focusVisible && enabled` on the focus glow call) plus the KDoc rewrite.
4. Re-ran `./gradlew :library:test --tests "*AeroButton*"` — all green (0 failures), including `AeroButtonSurfaceSourceTest` (6/6) and `AeroButtonSemanticsTest` (click/Space/Enter activation unaffected).
5. Ran `./gradlew build` — green.

## Issues Encountered

None beyond the self-corrected literal-substring KDoc issue documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CR-02 is closed at the shared-reducer level for all four consumers (`AeroSwitch`, `AeroSegmentedControl`, `AeroListItem`, `AeroButtonSurface`); VSEL-02, VSEL-04, VLST-03 are unblocked
- WR-01 is closed; the button family now agrees with the rest of the library on what a mouse click looks like
- Plan 19-10 (CR-01 raised-base darken) and 19-11 (WR-03/WR-04/WR-07 hover-emission cleanup) can proceed independently — neither touches `InteractionStates.kt`'s field shape or `AeroButtonSurface.kt`'s glow gating, per this plan's prohibitions
- No blockers identified

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*

## Self-Check: PASSED

All 5 modified/created source files confirmed present on disk; both task commits (`1ad5414`, `0c4cad9`) confirmed in `git log`.
