---
phase: 19-selectors-lists
plan: 07
subsystem: ui
tags: [compose-desktop, kotlin, aero-primitives, selection, segmented-control, focus-visible, accessibility, gap-closure]

# Dependency graph
requires:
  - phase: 19-03
    provides: "AeroSegmentedControl's resolveSegmentStyle resolver, imported pressedRecess/PRESSED_INNER_SHADOW cross-package reuse, per-segment selectable(Role.RadioButton)/hoverable wiring"
  - phase: 19-05
    provides: "Internal, Compose-free FocusVisibility/reduce/rememberFocusVisible mechanism and AeroInteractionState.focusVisible field"
provides:
  - "Every segment label — selected or not — resolved from colors.onSurface at full alpha; the per-segment animateColorAsState label-colour animation is gone (gap G3a, VSEL-03)"
  - "private const val RECESSED_FILL_DARKEN = 0.20f applied to the recessed style's fillTop/fillBottom AFTER the imported pressedRecess transform, so the recessed segment's total fill luminance is strictly below the raised segment's on every scheme (gap G3b, VSEL-03)"
  - "AeroSegmentedControl's per-segment in-bounds focus stroke gated on state.focusVisible, closing gap G2 for the last of three components sharing that gap"
affects: [19-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Darken-after-transform: apply a component-local Color.darken(amount) to a shared transform's OUTPUT fields via copy(...), rather than retuning the shared transform itself — keeps a cross-package-reused pressed/recessed style (AeroButtonSurface's pressedRecess) provably unchanged for its original consumer while correcting a value defect specific to the new consumer"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt

key-decisions:
  - "RECESSED_FILL_DARKEN = 0.20f matches AeroButtonSurface's own FILLED_FILL_TOP_DARKEN so the recessed segment lands in the same value neighbourhood as the pressed AeroButton it is compared against at sign-off; magnitude is Claude's discretion (retunable at 19-08), direction is not."
  - "darken(RECESSED_FILL_DARKEN) applied via copy(...) to the recessed style's fillTop/fillBottom strictly AFTER calling the imported pressedRecess(PRESSED_INNER_SHADOW) — never by retuning pressedRecess or PRESSED_INNER_SHADOW themselves, which would silently alter the already-accepted pressed AeroButton (Phase 17). Verified via git status --porcelain on theme/ and buttons/ after every task."
  - "AeroSegmentedControlStylesTest's expectedRecessed reconstructs the darken from scratch with the literal 0.20f, never importing the component's own RECESSED_FILL_DARKEN constant — matches the file's existing 'never import the component's own constant' anti-drift convention for the corner radius."
  - "A nonCommentSource helper (added to AeroSegmentedControlSourceTest.kt, strips lines whose trimmed form starts with *, //, or /*) backs the new negative guards so KDoc prose explaining the rejected colors.surface/animateColorAsState behaviour cannot itself satisfy or break the guard."

requirements-completed: [VSEL-03, VSEL-04]

coverage:
  - id: D1
    description: "Every segment label, in every state, resolves from colors.onSurface at that token's own alpha — no per-state label colour, no inherited background-token alpha"
    requirement: VSEL-03
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest.kt#sourceUsesOneContentTokenForEveryLabelState"
        status: pass
    human_judgment: true
    rationale: "Whether the single-token label is legible against the recessed fill on all three themes is a visual judgement, confirmed by a human in 19-08 (UAT test 9) per this plan's own <verification> section — the source guard proves the wiring, not the rendered pixels."
  - id: D2
    description: "The recessed selected segment's fill luminance is strictly below the raised unselected segment's on every scheme, and glossAlpha stays 0f — selection is carried by a downward value change plus the existing bevel swap and inner shadow, never a colour inversion"
    requirement: VSEL-03
    verification:
      - kind: unit
        ref: "AeroSegmentedControlStylesTest.kt#recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlStylesTest.kt#selectedEqualsIndependentlyReconstructedPressedRecess (updated expectedRecessed, all 8 pre-existing assertions still pass against the darkened reconstruction)"
        status: pass
    human_judgment: true
    rationale: "Whether the recessed segment now reads as depth comparable to a pressed AeroButton, on all three themes, is confirmed by a human in 19-08 (UAT test 9) per this plan's own <verification> section — the luminance-direction test proves the arithmetic, not the eye."
  - id: D3
    description: "The recessed fill correction lives entirely inside resolveSegmentStyle's own construction of the recessed style; pressedRecess and PRESSED_INNER_SHADOW are imported and consumed unchanged, and neither theme/ nor buttons/ was touched"
    requirement: VSEL-03
    verification:
      - kind: unit
        ref: "git status --porcelain library/src/main/kotlin/com/mordred/aero/theme library/src/main/kotlin/com/mordred/aero/components/buttons (empty after every task)"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSourceTest.kt#sourceImportsAndCallsPressedRecessCrossPackage, #sourceImportsPressedInnerShadowAndDoesNotRedeclareAShadowLiteral (unchanged, still green)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Clicking a segment with the mouse and moving the pointer away leaves no focus stroke; Tab still draws the stroke inside that segment's own bounds; Space still selects"
    requirement: VSEL-04
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest.kt#sourceGatesTheFocusStrokeOnFocusVisible"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSemanticsTest.kt (unchanged, all Role.RadioButton/Tab-stop/Space/Enter cases still pass)"
        status: pass
    human_judgment: true
    rationale: "The observable G2 contract on the live AeroSegmentedControl (mouse click leaves no stroke, Tab draws one, hover unchanged) is confirmed by a human in 19-08 across three themes, per this plan's own <verification> section — the source guard proves the wiring, not the rendered pixels. 19-05's FocusVisibilityTest.kt already unit-tests the shared reducer's behaviour."
  - id: D5
    description: "The per-segment hover cue is unchanged in token, magnitude and composition"
    requirement: VSEL-04
    verification:
      - kind: unit
        ref: "AeroSegmentedControlStylesTest.kt#hoveredUnselectedEqualsRaisedRestHoverLighten, #hoveredSelectedEqualsRecessedHoverLightenNeverUnselectedHoverStyle (both unchanged, still pass against the darkened recessed fixture)"
        status: pass
    human_judgment: false

# Metrics
duration: 22min
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 07: AeroSegmentedControl label token + recessed-fill darken + focus-visible gate (gaps G3, G2) Summary

**Every segment label now resolves to one on-surface content token at full alpha, the recessed selected segment is darkened by a local 0.20f constant applied after the imported pressed-button transform so its total fill luminance is provably below its raised neighbours', and the per-segment focus stroke gates on the shared focus-visible mechanism — closing gaps G3 and G2 for `AeroSegmentedControl` (VSEL-03, VSEL-04)**

## Performance

- **Duration:** 22 min (approx, first task commit `5533bcc` at 10:55:35+03:00 to last task commit `cb64605` at 11:01:49+03:00, plus preceding read/analysis time)
- **Started:** 2026-07-28T07:40:00Z (approx)
- **Completed:** 2026-07-28T08:02:00Z (approx)
- **Tasks:** 3
- **Files modified:** 3 (1 main source, 2 test files)

## Accomplishments

- Removed the per-segment `animateColorAsState` label-colour animation (the shipped `colors.surface`/`colors.onSurface` inversion) and its import; every segment `Text` now reads `color = colors.onSurface` in every state. Component KDoc records both root-cause faults from gap G3: `surface` is a background/panel token being used for text, and on AeroBlue/AeroDark it carries an `0xCC` alpha, so the selected label was also rendered ~80% opaque.
- Declared `private const val RECESSED_FILL_DARKEN: Float = 0.20f` and applied `darken(RECESSED_FILL_DARKEN)` to the recessed style's `fillTop`/`fillBottom` via `copy(...)`, strictly after computing `base.pressedRecess(PRESSED_INNER_SHADOW)` — the imported cross-package transform and constant are consumed completely unchanged. `0.20f` matches `AeroButtonSurface`'s own `FILLED_FILL_TOP_DARKEN`.
- Changed the per-segment in-bounds focus-stroke condition from `segState.focused && enabled` to `segState.focusVisible && enabled`, consuming 19-05's shared mechanism with no changes to the mechanism itself; `selectable(interactionSource = segSource)` and `.hoverable(segSource)` continue sharing the one source the reducer requires.
- `AeroSegmentedControlStylesTest.kt`: `expectedRecessed(colors)` rebuilt from scratch with the same literal `0.20f` darken (never importing the component's own constant, matching the file's existing anti-drift convention); all 8 pre-existing assertions still pass against the darkened reconstruction unchanged. Added `recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` proving the luminance-direction invariant (measured below) and that `glossAlpha` stays `0f` on the recessed style, on both `AeroBlue` and `Classic`.
- `AeroSegmentedControlSourceTest.kt`: added a `nonCommentSource` property (strips `*`/`//`/`/*`-prefixed lines) plus two new guards — one for the single-token label (gap G3), one for the focus-visible gate (gap G2) — bringing the file to 11 `@Test` functions (9 pre-existing + 2 new).
- No file under `library/src/main/kotlin/com/mordred/aero/theme/` or `library/src/main/kotlin/com/mordred/aero/components/buttons/` was modified — verified via `git status --porcelain` after every task, proving the shared pressed-`AeroButton` code path is untouched (T-19-07-02 mitigation).

## Task Commits

Each task was committed atomically:

1. **Task 1: one content token for every segment label, at full alpha (G3a)** — `5533bcc` (feat)
2. **Task 2: move the recessed fill downward so exactly one segment reads pushed in (G3b)** — `93da660` (feat)
3. **Task 3: gate each segment's focus stroke on focus-visible (G2)** — `cb64605` (test)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — single-token label (`color = colors.onSurface`), `animateColorAsState` label animation removed, `RECESSED_FILL_DARKEN` constant + darken-after-transform in `resolveSegmentStyle`, focus stroke gated on `segState.focusVisible`, KDoc updated for all three changes
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` — `expectedRecessed` rebuilt with the literal `0.20f` darken, new `recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` luminance-direction test (9 `@Test` total)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` — `nonCommentSource` property, `sourceUsesOneContentTokenForEveryLabelState` guard (Task 1), `sourceGatesTheFocusStrokeOnFocusVisible` guard (Task 3) (11 `@Test` total)

## Measured Recessed-vs-Raised Fill Luminance (AeroSegmentedControlStylesTest, observed via a temporary diagnostic run, not committed)

Captured by temporarily adding a `println` to the already-committed
`recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` test, running
`./gradlew :library:test --tests "*AeroSegmentedControlStylesTest*recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged*" --info`,
then restoring the test file to its exact committed content (`git diff --stat` empty
afterward, no extra commit needed):

| Scheme | Raised (unselected) fill luminance sum | Recessed (selected) fill luminance sum |
|---|---|---|
| AeroBlue | 0.8622992 | 0.5274838 |
| Classic | 0.46107447 | 0.28670442 |

On both schemes the recessed segment's total fill luminance is well below the raised
segment's — the darken constant produces a clearly readable value gap, not a marginal
one, confirming the arithmetic in this plan's `<planner_finding>`.

## Guard Fail-Then-Pass Proof

Per this project's fail-then-pass convention (VER-06), both new guards were proven to
FAIL against the pre-fix source before being trusted, run together as instructed by
Task 3:

1. Temporarily reverted `AeroSegmentedControl.kt`'s label `Text` call's `color` argument
   from `colors.onSurface` back to `if (isSelected) colors.surface else colors.onSurface`
   (the pre-fix inversion) and its focus-stroke condition from
   `segState.focusVisible && enabled` back to `segState.focused && enabled`.
2. Ran `./gradlew :library:test --tests "*AeroSegmentedControlSourceTest*"`:
   ```
   AeroSegmentedControlSourceTest > sourceGatesTheFocusStrokeOnFocusVisible() FAILED
       org.opentest4j.AssertionFailedError at AeroSegmentedControlSourceTest.kt:177

   AeroSegmentedControlSourceTest > sourceUsesOneContentTokenForEveryLabelState() FAILED
       org.opentest4j.AssertionFailedError at AeroSegmentedControlSourceTest.kt:149

   11 tests completed, 2 failed
   ```
   Confirmed FAIL — both new guards correctly reject the pre-fix source; the 9
   pre-existing guards stayed green throughout.
3. Restored `AeroSegmentedControl.kt`'s label colour and focus-stroke condition to their
   Task 1/Task 3 committed values.
4. Re-ran `./gradlew :library:test --tests "*AeroSegmentedControl*"` — exited 0, all
   tests (styles, semantics, source) pass.
5. `git diff --stat library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt`
   against the last commit showed only the intended Task 3 change (the new KDoc
   paragraph plus the focus-stroke condition) — zero unintended drift from the
   revert-and-restore cycle.
6. Ran the full `./gradlew :library:test` suite after restoring — exited 0, no
   regression anywhere in the library.

## Decisions Made

- **`RECESSED_FILL_DARKEN = 0.20f`, matching `AeroButtonSurface`'s `FILLED_FILL_TOP_DARKEN`** — chosen per this plan's `<planner_finding>` so the recessed segment lands in the same value neighbourhood as the pressed `AeroButton` the reviewer compares it against at 19-08. Magnitude is explicitly Claude's discretion and retunable at sign-off; direction (strictly darker) is not.
- **Darken applied via `copy(...)` on the recessed style's own two fields, never by retuning `pressedRecess`/`PRESSED_INNER_SHADOW`** — preserves VSEL-03's cross-package reuse guarantee and keeps the already-accepted pressed `AeroButton` (Phase 17) byte-for-byte unchanged, verified via `git status --porcelain` on `theme/` and `buttons/` after every task (T-19-07-02 mitigation, `security_block_on: high`).
- **`expectedRecessed` in the styles test reconstructs the darken from scratch with a literal `0.20f`, mirroring the existing corner-radius convention** — a drift in either the component's `RECESSED_FILL_DARKEN` constant or the test's literal is caught rather than silently agreeing with itself.
- **`nonCommentSource` helper added to the source test** — the same KDoc-vs-guard collision problem 19-06 solved for `AeroListItemSourceTest.kt` recurred here (the new KDoc paragraph explaining the rejected `colors.surface` label needed to reference that identifier in prose without satisfying or breaking the negative guard); solved identically by stripping comment lines before the `contains` checks.

## Deviations from Plan

None — plan executed exactly as written. All three tasks' `<action>` instructions and acceptance criteria were followed and met on the first attempt; no Rule 1-4 auto-fixes were required.

## Issues Encountered

None. All three tasks compiled and passed their scoped test filters on the first run; the full `./gradlew :library:test` suite and `./gradlew :showcase:compileKotlin` were green after every task.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Gaps G2 and G3 are both closed for `AeroSegmentedControl`. Combined with 19-05 (`AeroSwitch`, G2) and 19-06 (`AeroListItem`, G1 + G2), all three components sharing gap G2 have now adopted the same `state.focusVisible` gate, and both G3 faults (background token used for text; recessed fill not moving downward) are corrected in the one file that owned them.
- No file under `library/src/main/kotlin/com/mordred/aero/theme/` was modified — gap G4 (ornament token brightness) remains untouched and deferred, per the scope fence. `AeroButtonSurface.kt`/`pressedRecess`/`PRESSED_INNER_SHADOW` are provably unchanged.
- `AeroListItem.kt` was not touched, per the scope fence — 19-06's row-growth and focus-visible fixes for that component remain exactly as signed off for re-verification.
- The visual result (single-token label legible on all three themes, recessed segment reading depth comparable to a pressed `AeroButton`, no residual focus stroke after a mouse click) is NOT yet human-confirmed — that happens in 19-08's re-sign-off (UAT test 9), per this plan's own `<verification>` section.
- This is the last gap-closure plan (19-05/06/07) before 19-08's formal three-theme re-verification of gaps G1, G2 and G3, with G4 explicitly deferred out of Phase 19.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*

## Self-Check: PASSED

All modified files verified present on disk with the expected content; all three task commits (`5533bcc`, `93da660`, `cb64605`) verified present in git log.
