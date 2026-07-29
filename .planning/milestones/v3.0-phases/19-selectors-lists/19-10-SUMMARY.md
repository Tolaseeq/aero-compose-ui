---
phase: 19-selectors-lists
plan: 10
subsystem: ui
tags: [compose-desktop, aero-theme, segmented-control, contrast, wcag, kotlin-test]

# Dependency graph
requires:
  - phase: 19-selectors-lists
    provides: "AeroSegmentedControl's resolveSegmentStyle (19-07), RECESSED_FILL_DARKEN (19-07/19-08 sign-off), AeroButtonSurface's FILLED_FILL_TOP_DARKEN/FILLED_FILL_BOTTOM_DARKEN precedent (Phase 17)"
provides:
  - "RAISED_FILL_TOP_DARKEN/RAISED_FILL_BOTTOM_DARKEN (0.45f/0.61f) applied to resolveSegmentStyle's raised base, fixing CR-01 label-on-fill illegibility on AeroDark/AeroBlue"
  - "everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio — 12 value-level WCAG contrast assertions (3 schemes x 2 selection-axis endpoints x 2 fill stops) gating this regression class permanently"
  - "Styles suite widened to all three shipped schemes (AeroDark added), reconstructions rebuilt from scratch"
affects: [19-11, 19-12]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "WCAG 2.x contrast ratio computed from androidx.compose.ui.graphics.luminance() (already imported for the pre-existing luminance-direction test), no new luminance implementation"
    - "Raised-base darken applied BEFORE the imported pressedRecess transform so the recessed style inherits the correction through the fill-stop exchange"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt

key-decisions:
  - "RAISED_FILL_TOP_DARKEN/RAISED_FILL_BOTTOM_DARKEN landed at 0.45f/0.61f, materially darker than the filled AeroButton's 0.20f/0.36f precedent — required because this component's label is locked to the on-surface content token (not near-white button text), per the planner's pre-computed arithmetic"
  - "Task 1 shipped with the button's precedent values (0.20f/0.36f) and its own tests green; Task 2 then raised both constants once the contrast guard proved the precedent insufficient on AeroDark/AeroBlue, per the plan's explicit sequencing note"
  - "Darken applied to colors.primary directly (mirroring resolveButtonStyle's own construction) rather than to the ornament-derived fillTop/fillBottom, so the raised base's copy(...) shape matches the filled button's precedent exactly"

requirements-completed: [VSEL-03]

coverage:
  - id: D1
    description: "Raised (unselected) segment fill darkened via two named constants applied before the imported pressedRecess transform, so the recessed style inherits the correction and the strictly-darker invariant survives"
    requirement: "VSEL-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt#unselectedEnabledNoHoverNoPressEqualsTheDarkenedRaisedBase"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt#selectedEqualsIndependentlyReconstructedPressedRecess"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt#recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged"
        status: pass
    human_judgment: false
  - id: D2
    description: "Value-level contrast guard (12 assertions: 3 schemes x 2 selection-axis endpoints x 2 fill stops) fails a test rather than only a human eye when label-to-fill contrast regresses, per-scheme minimum ratio 3.0"
    requirement: "VSEL-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt#everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio"
        status: pass
    human_judgment: false
  - id: D3
    description: "The composited pixel (after gloss/bevel/rim painting) reads legibly on all three schemes — a separate claim from the value-level fill-stop guard, requiring the human eye"
    verification: []
    human_judgment: true
    rationale: "The contrast test guards resolved fill stops only; the gloss band, bevel and rim are painted afterward by drawAeroSurfaceCore and are not modeled by a value-level assertion. Deferred to plan 19-12's three-theme human checkpoint, as this plan's must_haves explicitly state."

duration: 18min
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 10: Segmented control raised-fill contrast fix (CR-01) Summary

**Darkened AeroSegmentedControl's raised (unselected) segment fill by 0.45f/0.61f (colors.primary darken) so the on-surface label survives on AeroDark/AeroBlue, backed by a new 12-assertion WCAG contrast test that would have caught the original 1.04:1 defect.**

## Performance

- **Duration:** 18 min
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- `resolveSegmentStyle`'s raised base now darkens `colors.primary` by two named constants (`RAISED_FILL_TOP_DARKEN`, `RAISED_FILL_BOTTOM_DARKEN`) before the imported `pressedRecess` transform runs, so the recessed (selected) style inherits the correction through the fill-stop exchange — the strictly-darker invariant survives unchanged.
- A new value-level contrast test (`everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio`) asserts `contrastRatio(onSurface, fillStop) >= 3.0` for every shipped scheme, at both ends of the selection axis, for both fill stops — 12 assertions total, closing the exact gap that let CR-01 ship with a fully green suite.
- The styles suite now runs every existing value-level assertion against `AeroDark` as well as `AeroBlue`/`Classic` — the scheme where the label was measurably invisible had never been exercised before this plan.
- The recessed segment still equals the imported pressed-button transform applied to the darkened raised base in every field except its own documented `RECESSED_FILL_DARKEN` shift — VSEL-03's cross-package reuse guarantee is intact and independently reconstructed/asserted in the test file.

## Task Commits

Each task was committed atomically:

1. **Task 1: darken the raised segment base so the label survives on it (CR-01)** - `3436f23` (fix)
2. **Task 2: make label-to-fill contrast a test, not an eye (CR-01 guard)** - `248814a` (test)

_Note: Task 1 intentionally shipped with the filled button's own precedent darken values (0.20f/0.36f) and its own (pre-existing) test suite green, per the plan's explicit sequencing note — the contrast guard added in Task 2 is what forced those values up to their final magnitude._

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — `RAISED_FILL_TOP_DARKEN`/`RAISED_FILL_BOTTOM_DARKEN` (0.45f/0.61f) declared and applied to the raised base inside `resolveSegmentStyle`; anti-drift KDoc rewritten to describe the new darkened-base/darkened-recess composition; component KDoc's label-colour ban narrowed to STATE-DEPENDENT colours only.
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` — `schemes` widened to all three shipped schemes; `expectedRaised`/`expectedRecessed` reconstructed from scratch with literal darken values (never imported from the component); renamed `unselectedEnabledNoHoverNoPressEqualsRaisedRest` → `unselectedEnabledNoHoverNoPressEqualsTheDarkenedRaisedBase`; added `MIN_LABEL_CONTRAST`, `contrastRatio(...)`, and the new 12-assertion contrast test.

## Guard Fail-Then-Pass Proof

**Renamed raised-base test (Task 1):** `unselectedEnabledNoHoverNoPressEqualsTheDarkenedRaisedBase` was run against the pre-edit resolver (bare `AeroSurfaceStyle.rest(...)` raised base, no darken at all) — result: **RED**, 7 of 9 tests in the class failed (the reconstructions already expected the darkened base). Fix applied, full class re-run: **GREEN**, all 9 tests passed (with the button's precedent values 0.20f/0.36f).

**Contrast guard test (Task 2):** `everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio` was run twice against pre-fix states before being trusted:
- Against the literal pre-Task-1 resolver (bare, unmodified rest preset raised base) — **RED** (1 test failed, as expected — this is the original 1.04:1-class defect).
- Against Task 1's own precedent-value resolver (`RAISED_FILL_TOP_DARKEN = 0.20f`, `RAISED_FILL_BOTTOM_DARKEN = 0.36f`) — **RED**, failing at `label-to-fillTop contrast ratio 2.34557 for AeroBlue at selectedProgress=0.0` (must be >= 3.0) — matching the planner's pre-computed table exactly (AeroBlue d=0.20 → 2.35).
- After raising the constants to `0.45f`/`0.61f` and re-running: **GREEN**, all 12 assertions pass. Full `./gradlew :library:test --tests "*AeroSegmentedControl*"` and `./gradlew build` both exit 0.

## Measured contrast

Final constants: **`RAISED_FILL_TOP_DARKEN = 0.45f`**, **`RAISED_FILL_BOTTOM_DARKEN = 0.61f`** (unchanged from the planner's pre-computed estimate of ~0.45f/0.61f — the test confirmed the estimate directly, no further raising was needed).

| Scheme | selectedProgress | fillTop ratio | fillBottom ratio |
|---|---|---|---|
| AeroBlue | 0f (unselected) | 4.467 | 7.036 |
| AeroBlue | 1f (selected) | 8.760 | 6.072 |
| AeroDark | 0f (unselected) | **3.296** ← tightest margin | 5.317 |
| AeroDark | 1f (selected) | 6.766 | 4.564 |
| Classic | 0f (unselected) | 6.688 | 9.349 |
| Classic | 1f (selected) | 10.889 | 8.418 |

All 12 measured ratios clear the 3.0 floor; the tightest margin is AeroDark's unselected `fillTop` at 3.296, consistent with the planner's `d=0.45` estimate of 3.29 for that exact cell.

**Cost relative to a filled `AeroButton`:** the filled button darkens `colors.primary` by 0.20f/0.36f for its own rest fill. This component's raised base now darkens the same token by 0.45f/0.61f — roughly **2.25x the top-stop darken and 1.7x the bottom-stop darken** of the button's precedent. The segmented control's raised strip is therefore visibly darker than a filled `AeroButton` at rest; this is the real, visible consequence the plan's `<planner_finding>` flagged in advance, and it is exactly why plan 19-12's human checkpoint exists — the magnitude is retunable there, the direction (strictly darker than the ornament-derived plate) is not.

## Decisions Made

- Started Task 1 at the filled button's own precedent (0.20f/0.36f) per the plan's explicit sequencing note, landing its own reconstruction-based tests green before Task 2 introduced the contrast gate that forced the final magnitude.
- Landed the darken via `colors.primary.darken(...)` applied in a `copy(...)` on the rest preset — mirroring `resolveButtonStyle`'s own construction shape exactly, rather than darkening the already-ornament-derived `fillTop`/`fillBottom` fields a second time.
- Kept `MIN_LABEL_CONTRAST` at the developer-specified WCAG non-text floor of 3.0 (not the 4.5 normal-text target) — this is a value-level guard on resolved fill stops, not a claim about the gloss/bevel/rim-composited pixel.

## Deviations from Plan

None - plan executed exactly as written. Task 1 landed the precedent values and its own tests green; Task 2 added the contrast guard, observed it RED against both the pre-Task-1 resolver and Task 1's precedent-value resolver, then raised the constants to 0.45f/0.61f (matching the planner's own pre-computed estimate) until all 12 assertions passed. No architectural changes, no edits to `theme/` or `components/buttons/`, no re-tinted or state-dependent label colour introduced.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CR-01's three `missing:` items in `19-VERIFICATION.md` are all satisfied: the raised base carries a documented darken, a value-level contrast guard exists per scheme at both ends of the selection axis, and both the anti-drift reconstruction and the anti-drift KDoc describe what the resolver actually returns.
- VSEL-03 is unblocked on the legibility half; its recess-reuse half was already satisfied and is provably still intact (`recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` and `selectedEqualsIndependentlyReconstructedPressedRecess` both pass unchanged).
- The chosen darken magnitude (0.45f/0.61f), its measured effect (12 ratios above), and its cost relative to a filled button (~2.25x/1.7x the button's own precedent darken) are all written down above for the developer to judge at plan 19-12's three-theme checkpoint — the composited-pixel legibility claim (this SUMMARY's coverage item D3) still needs that human sign-off.
- No blockers for 19-11/19-12.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*

## Self-Check: PASSED

- FOUND: `.planning/phases/19-selectors-lists/19-10-SUMMARY.md`
- FOUND: commit `3436f23` (Task 1)
- FOUND: commit `248814a` (Task 2)
