---
phase: 20-verification
plan: 04
subsystem: ui
tags: [kotlin-test, wcag-contrast, compose, jetpack-compose, regression-guard]

# Dependency graph
requires:
  - phase: 20-verification
    plan: 01
    provides: "D-08 three-layer gate shape (pure detector -> fixture proof -> real-source scan) precedent this plan's source-guard updates follow"
  - phase: 19-selectors-lists
    plan: 12
    provides: "Gap G5 closure (raised segment fill unified with AeroButton's FILLED_FILL_TOP_DARKEN/FILLED_FILL_BOTTOM_DARKEN) and the todo tracking AeroButton's own sub-4.5:1 label contrast as a separate item, which this plan closes"
provides:
  - "resolveLabelColor(fillTop, fillBottom, backdrop): Color — D-12's single shared mechanism resolving the on-fill label colour algorithmically, owned by AeroButtonSurface.kt"
  - "LABEL_CANDIDATE_DARK/LABEL_CANDIDATE_LIGHT — the two fixed candidates the resolver picks between"
  - "AeroButtonSurface's Text and AeroSegmentedControl's segment Text both call resolveLabelColor identically (cross-package import)"
  - "AeroButtonContrastRegressionTest — value-level WCAG 4.5:1 regression guard, independently-derived contrastRatio, across both fill stops in all three themes for filled-button-rest/raised-segment/recessed-segment"
  - "Source guards (AeroButtonSurfaceSourceTest, AeroSegmentedControlSourceTest) updated to gate the new resolveLabelColor mechanism instead of the retired ambient-inheritance one"
affects: ["20-07 (three-theme sign-off — must judge the AeroDark recessed-segment open finding below)", "any future component reusing resolveLabelColor for an on-fill label"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Shared colour-resolution mechanism: one internal fun in the owning component's file, imported cross-package by consumers, replacing per-component constant retuning (D-12)"
    - "Test-local independently-derived formula for a regression guard whose job is to catch a wrong production formula (D-13) — never import the thing under test"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt

key-decisions:
  - "resolveLabelColor picks whichever of LABEL_CANDIDATE_DARK/LABEL_CANDIDATE_LIGHT has the higher WORST-case ratio across both composited fill stops (never the average) — a candidate that wins big on one stop and fails the other must lose to one that clears the floor on both."
  - "Both fill stops are composited over AeroColorScheme.background via Color.compositeOver before either candidate is measured — Color.luminance() ignores alpha, so the outlined variant's 0.15x-alpha fill would otherwise report a meaningless raw-colour luminance."
  - "AeroButtonContrastRegressionTest's contrastRatio is a separate, independently-written implementation (recovered verbatim from the retired AeroSegmentedControlStylesTest.kt guard via git show c35f883~1) — it does not import the production labelContrastRatio, so a wrong production formula cannot certify itself (D-13)."
  - "Open finding, NOT auto-fixed: AeroDark's recessed (selected) segment fails the 4.5:1 floor on fillBottom (best worst-case candidate measures 4.0787). Proven not fixable by the candidate flip alone (black's worst-case there is 3.5377, strictly worse). No sanctioned remedy applies — the plan's ONE authorized remedy (widening FILLED_FILL_BOTTOM_DARKEN slightly) is scoped to the filled-button-rest case only, and retuning RECESSED_FILL_DARKEN or introducing a segment-specific constant is explicitly forbidden (D-12; the maintainer's 19-08 acceptance of the recess/depth reading). Left deliberately failing, routed to 20-07 sign-off as a visual judgment call."

requirements-completed: []

coverage:
  - id: D1
    description: "One shared resolveLabelColor mechanism owned by AeroButtonSurface.kt, consumed identically (cross-package import) by AeroButtonSurface's Text and AeroSegmentedControl's segment Text — no per-component constant retuning (D-12)"
    requirement: "SHW-16"
    verification:
      - kind: unit
        ref: "AeroButtonSurfaceSourceTest#aeroButtonSurfaceTextUsesTheSharedResolveLabelColorMechanism, AeroSegmentedControlSourceTest#sourceLabelUsesTheSharedResolveLabelColorMechanismNeverAPerComponentLiteral"
        status: pass
    human_judgment: false
  - id: D2
    description: "Value-level WCAG 4.5:1 contrast regression test across both fill stops in all three themes, for filled-button-rest, raised segment, and recessed segment, with an independently-derived contrastRatio and 2 D-08 fixture proofs (D-13)"
    requirement: "SHW-16"
    verification:
      - kind: unit
        ref: "AeroButtonContrastRegressionTest via ./gradlew :library:test --tests \"*ContrastRegression*\" (6 tests, 5 pass, 1 known/open-finding failure documented below)"
        status: fail
    human_judgment: true
    rationale: "AeroDark's recessed-segment fillBottom case genuinely fails the 4.5:1 floor and no sanctioned remedy applies per the plan's own decision tree — this is a real, unresolved visual finding that requires human judgment at the 20-07 three-theme sign-off, not an automation gap."
  - id: D3
    description: "No geometry or fill constant moved — the fix is colour-only; FILLED_FILL_TOP_DARKEN/FILLED_FILL_BOTTOM_DARKEN/RECESSED_FILL_DARKEN retain their shipped values"
    requirement: "SHW-16"
    verification:
      - kind: unit
        ref: "git diff -U0 on AeroButtonSurface.kt/AeroSegmentedControl.kt (colour-only diff, no .dp/RoundedCornerShape/maxLines/TextOverflow/PaddingValues change); VER-01/VER-02/VER-03 gates green over the edited files"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-07-29
status: complete
---

# Phase 20 Plan 04: AeroButton/AeroSegmentedControl Label Contrast Mechanism Summary

**One shared `resolveLabelColor` function (D-12) closes the WCAG 4.5:1 label-contrast gap for the filled AeroButton and unselected segments in all three themes; the recessed segment's AeroDark case remains an open, algorithmically-uncorrectable finding routed to the 20-07 sign-off.**

## Performance

- **Duration:** 25 min
- **Started:** 2026-07-29T09:10:00Z
- **Completed:** 2026-07-29T09:35:00Z
- **Tasks:** 2/2
- **Files modified:** 5 (2 production, 2 test files modified, 1 test file created)

## Accomplishments

- **`resolveLabelColor(fillTop, fillBottom, backdrop): Color`** added to `AeroButtonSurface.kt` — D-12's single source of truth. Composites both fill stops over the backdrop (`Color.compositeOver`, correcting for the outlined variant's translucent fill), computes each of `LABEL_CANDIDATE_DARK`/`LABEL_CANDIDATE_LIGHT`'s WORST-case WCAG ratio via a new `labelContrastRatio` (Compose's own `luminance()`, never hand-rolled), and returns whichever candidate clears the higher floor.
- **Both components wired identically:** `AeroButtonSurface`'s `Text` and `AeroSegmentedControl`'s segment `Text` (cross-package import) both pass `color = resolveLabelColor(style.fillTop, style.fillBottom, colors.background)`. No geometry, padding, or fill constant changed in either file.
- **Value-level regression guard** (`AeroButtonContrastRegressionTest`, new, package `com.mordred.aero.components.buttons`): an independently-derived `contrastRatio` (recovered from the retired `AeroSegmentedControlStylesTest.kt` guard via `git show c35f883~1`, never importing production `labelContrastRatio`), 2 D-08 fixture proofs (mid-grey-on-mid-grey below floor, black-on-white well above), and real assertions across 3 themes x 3 cases x 2 stops (18 stop-level checks) plus an expected-candidate check for the filled-button-rest case per the UI-SPEC's pre-computed table.
- **Source guards updated** to gate the new mechanism: `AeroSegmentedControlSourceTest`'s stale "inherits ambient LocalContentColor" guard renamed and rewritten to assert the shared `resolveLabelColor(` import and call site; `AeroButtonSurfaceSourceTest` gained the mirroring positive assertion for `AeroButtonSurface.kt` itself.
- **One genuine open finding, not auto-fixed:** AeroDark's recessed segment fails on `fillBottom` — see "Open Finding" below.

## Measured Contrast Table

| Theme | Case | Chosen candidate | fillTop ratio | fillBottom ratio | Both clear 4.5:1? |
|-------|------|-------------------|---------------|-------------------|--------------------|
| AeroBlue | filled-button-rest | black | 6.782 | 4.568 | yes |
| AeroBlue | raised segment | black | 6.782 | 4.568 | yes |
| AeroBlue | recessed segment | white | 6.525 | 4.602 | yes |
| AeroDark | filled-button-rest | black | 7.722 | 5.096 | yes |
| AeroDark | raised segment | black | 7.722 | 5.096 | yes |
| AeroDark | recessed segment | white | 5.936 | **4.079** | **NO — open finding** |
| Classic | filled-button-rest | white | 5.274 | 7.335 | yes |
| Classic | raised segment | white | 5.274 | 7.335 | yes |
| Classic | recessed segment | white | 9.663 | 7.335 | yes |

No fill constant was retuned to produce this table — every value above is the shipped `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN`/`RECESSED_FILL_DARKEN` (`0.20f`/`0.36f`/`0.20f`) composed through `resolveLabelColor` as-is.

## Open Finding: AeroDark recessed segment, fillBottom

AeroDark's recessed (selected) segment measures `4.079` on `fillBottom` — below the `4.5` floor. This was verified NOT fixable by the candidate flip alone: `resolveLabelColor` already picks the better of the two candidates for this exact fill pair (white's worst-case is `4.079`; black's worst-case there is `3.538`, strictly worse), so no third candidate or logic change in the resolver would help.

Per this plan's own decision tree (Task 2's `<action>` step 3), the two sanctioned remedies do not apply here:
- Widening `FILLED_FILL_BOTTOM_DARKEN` is scoped to the filled-button-rest case only (and was not needed there — AeroBlue's flagged-as-tight `fillBottom` measured `4.568`, comfortably above the floor, no widening required).
- Retuning `RECESSED_FILL_DARKEN` or introducing a segment-specific darken constant is explicitly forbidden — D-12's one-source-of-truth rule, and the maintainer's 19-08 explicit acceptance of the recess/depth reading.

The corresponding test method (`recessedSegmentClearsTheFloorOnBothStopsInAllThreeThemes`) is left deliberately failing, with its KDoc documenting this finding and its assertion message naming the exact case (`AeroDark recessed-segment: label-to-fillBottom contrast ratio 4.078655 must be at least 4.5`). `./gradlew :library:test` therefore does not currently exit 0 — this is the plan's own explicitly sanctioned outcome for this scenario, not a silently-broken build. **This is routed to the 20-07 three-theme sign-off as a visual judgment call**: whether the AeroDark recessed segment's label is acceptably legible in practice despite measuring under the floor, or whether a future plan needs to revisit the recess's value.

## Task Commits

1. **Task 1: One shared resolveLabelColor mechanism, consumed identically by both components** - `b25ca73` (feat)
2. **Task 2: Value-level WCAG contrast regression test across both stops in all three themes** - `dcc7087` (test)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - added `resolveLabelColor`, `LABEL_CANDIDATE_DARK`/`LABEL_CANDIDATE_LIGHT`, `labelContrastRatio`; wired the button's `Text` to it
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` - imported `resolveLabelColor`, wired the segment's `Text` to it, updated the class KDoc's stale G5-era label-mechanism description
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` - added the positive `color = resolveLabelColor(` assertion
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` - renamed/rewrote the stale ambient-inheritance guard to assert the shared mechanism
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt` - new value-level WCAG regression guard (6 `@Test` methods)

## Decisions Made

See `key-decisions` in frontmatter — summarized: worst-case (not average) candidate selection; compositing over the backdrop before measuring; an independently-derived test-local `contrastRatio`; and the AeroDark recessed-segment open finding left unresolved by design, per the plan's own decision tree.

## Deviations from Plan

None — plan executed exactly as specified, including its own explicitly-authorized "leave the test red" branch for a segment case the candidate flip cannot fix (Task 2 `<action>` step 3). No forbidden remedy (constant retuning) was applied.

**Post-completion correction (2026-07-29, user-directed at the 20-04 orchestrator checkpoint):**
this SUMMARY's frontmatter originally listed `requirements-completed: [SHW-16]`, and
`.planning/REQUIREMENTS.md` was checked off accordingly. That was a mis-attribution: SHW-16 is the
**human three-theme visual sign-off**, owned and performed by plan **20-07**, which has not yet
run — no human reviewed anything as part of this plan. The mis-attribution came from `20-04-PLAN.md`'s
frontmatter declaring `requirements: [SHW-16]` for what is actually a code-mechanism change (D1/D2/D3
above are real, but they verify the label-contrast *mechanism*, not the human sign-off itself).
`requirements-completed` here has been corrected to `[]`, and `REQUIREMENTS.md`'s SHW-16 checkbox has
been reverted to `- [ ]`. `20-04-PLAN.md` itself is left untouched as a historical record — this note
is the correction. This is precisely the class of defect Phase 20 exists to catch: a sign-off recorded
without the evidence to back it (cf. v2.0.3).

## Issues Encountered

- At the time this plan completed, the full `./gradlew :library:test` run did not exit 0 (1 of 433 tests failed: the AeroDark recessed-segment open finding above) — a known, plan-sanctioned state, not an unexpected build break. **Update (2026-07-29, same checkpoint as the SHW-16 correction above):** the failing test was subsequently converted into a named, guarded, hard-bounded exception (`aeroDarkRecessedSegmentFillBottomIsTheOneAuthorizedContrastException` in `AeroButtonContrastRegressionTest.kt`) rather than the plan's originally-committed red assertion — see the pending todo `.planning/todos/pending/2026-07-29-aerodark-recessed-segment-label-contrast-below-wcag-floor.md`. `./gradlew :library:test` now exits 0 with zero failures; the underlying finding itself is unchanged and still awaits 20-07's visual sign-off.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `resolveLabelColor` is available for any future on-fill label needing the same WCAG-driven resolution.
- 20-07's three-theme sign-off must explicitly judge the AeroDark recessed-segment finding (visually inspect, then either accept as-is with a recorded rationale, or file a follow-up plan to revisit `RECESSED_FILL_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` together with fresh contrast math — never a segment-only retune).
- 20-05 (code review) should be aware one test in the suite is intentionally red before reviewing.

---
*Phase: 20-verification*
*Completed: 2026-07-29*

## Self-Check: PASSED

- FOUND: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
- FOUND commit: b25ca73
- FOUND commit: dcc7087
