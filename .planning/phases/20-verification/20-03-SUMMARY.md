---
phase: 20-verification
plan: 03
subsystem: testing
tags: [kotlin-test, source-scan-gate, compose-ui-test, regression-guard, retroactive-summary]

# Dependency graph
requires:
  - phase: 20-verification
    plan: 01
    provides: "D-08 three-layer gate shape (pure detector -> in-file fixtures -> real-source scan) and the sourceFile()/stripComments cwd-independent idiom this plan's VER-03 gate reuses verbatim"
  - phase: 19-selectors-lists
    plan: 06
    provides: "AeroListItem's approved G1 fixed-to-min row-height change, cited here as VER-03's one authorized exception"
  - phase: 18-range
    plan: 01
    provides: "AeroRangeSlider's locked thumb/track dimensions, which AeroSlider's Phase-18-approved substitutions match (cited as VER-03's other documented finding)"
provides:
  - "VER-03 gate: VER03BaselineSizeSnapshotTest — measures 7 component sizes from composed nodes at pinned density (1f) plus 6 value-level radius/size constants from comment-stripped source, both compared by exact Dp equality against an identity-keyed 14-entry v2.0.4 baseline map (D-10), with its own 5-fixture D-08 red/green proof (VER-06)"
  - "VER-04 closure: AeroButtonSemanticsTest strengthened in place (D-16, no new class) — boolean clicked flag replaced by an Int invocation counter asserted == 2 after two key presses, assertExists() added before every requestFocus(), and the missing AeroOutlinedButton Space case added"
affects: ["20-04 (SHW-16 label-contrast fix), 20-05 (code review), any future gate needing the sourceFile()/stripComments/dpLiteralAfter idioms"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Component-size baseline snapshot: pinned-density composed-node measurement + comment-stripped value-level source assertion, unified under one identity-keyed Map<String, Dp> comparison function (dpLiteralAfter added as a new small idiom extracting a Dp literal following a named marker in stripped source)"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt

key-decisions:
  - "VER-03's BASELINE map holds exactly 14 identity-keyed entries (Component.property), read directly from git tag v2.0.4 per D-10, with three deliberate same-value coincidences (4.dp corner radii on AeroButton/AeroOutlinedButton/AeroSegmentedControl; 28.dp height on AeroOutlinedButton/AeroSegmentedControl; 36.dp on AeroSwitch.trackWidth/AeroListItem.rowMinHeight) each proven independently caught by the twoCoincidentBaselineKeysDriftingOppositeDirectionsAreBothCaught fixture."
  - "Exactly two authorized VER-03 exceptions, no more: AeroListItem's fixed-to-min row-height change (G1, 19-06) — asserted as still exactly 36.dp but now a heightIn(min = ...) floor, not a fixed height — and AeroSlider's complete absence from BASELINE because it declared no numeric size default at v2.0.4 (confirmed by direct git show v2.0.4 read), so its current THUMB_DIAMETER/TRACK_HEIGHT constants (Phase-18-approved substitutions matching AeroRangeSlider) have no pre-migration number to compare against."
  - "VER-04 closed by strengthening the EXISTING AeroButtonSemanticsTest, not by adding a new keyboard-activation test class (D-16) — audited against all four of VER-04's explicit criteria; only the invocation-count criterion required a code change."

requirements-completed: [VER-03, VER-04, VER-06]

coverage:
  - id: D1
    description: "VER-03 gate: 7 composed-node size measurements (pinned density 1f) + 6 value-level radius/size assertions from comment-stripped source, compared by exact Dp equality against a 14-entry identity-keyed v2.0.4 baseline map; carries its own 5-fixture D-08 red/green proof plus the real assertion, and documents exactly two authorized exceptions (AeroListItem G1/19-06, AeroSlider's absent baseline)"
    requirement: "VER-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt (6 tests, 0 failures) via ./gradlew :library:test --tests \"*VER03*\""
        status: pass
    human_judgment: false
  - id: D2
    description: "VER-04 closure: AeroButtonSemanticsTest audited criterion-by-criterion against VER-04's four explicit criteria (see 'VER-04 Audit' section below) and strengthened where the invocation-count criterion was not yet met — boolean flag replaced with an Int counter asserted == 2, assertExists() added to every activation test, missing AeroOutlinedButton Space case added"
    requirement: "VER-04"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt (6 tests, 0 failures, up from 5) via ./gradlew :library:test --tests \"*AeroButtonSemanticsTest*\""
        status: pass
    human_judgment: false
  - id: D3
    description: "VER-06: VER-03's gate carries its own in-file, re-executed red-on-broken/green-on-clean fixture proof — 3 RED fixtures (one-dp-above, one-dp-below, empty map) + 1 GREEN fixture (clean map) + 1 identity-coincidence adjacency fixture, all against the same baselineDeviations() pure comparison function the real assertion also exercises (D-08)"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "VER03BaselineSizeSnapshotTest: cleanFixtureMatchingBaselineIsEmpty, oneKeyOneDpAboveBaselineIsFlagged, oneKeyOneDpBelowBaselineIsFlagged, emptyMeasuredMapIsFlagged, twoCoincidentBaselineKeysDriftingOppositeDirectionsAreBothCaught — all pass. Full suite: ./gradlew :library:test --rerun-tasks"
        status: pass
    human_judgment: false

duration: unknown (interrupted executor session — see Deviations)
completed: 2026-07-28
status: complete
---

# Phase 20 Plan 03: VER-03/VER-04 Gates Summary

**A new identity-keyed VER-03 snapshot gate comparing 7 measured component sizes and 6 value-level radii against a 14-entry v2.0.4 baseline (D-10) with its own D-08 fixture proof, plus VER-04 closed by strengthening the existing `AeroButtonSemanticsTest` with invocation-count assertions instead of a new test class (D-16).**

**Note on this SUMMARY:** this document was written retroactively. The executor that authored and committed both tasks (`68dbc60`, `477567d`) was interrupted before reaching its closeout step — no `.planning/async-jobs/` manifest existed for this plan, so this was an abort, not a legal deferral. Per the user's explicit approval to close out manually rather than re-execute, this SUMMARY was reconstructed from the two committed diffs, the plan's own text, and a fresh full-suite verification run (`./gradlew :library:test --rerun-tasks`), not written by the executor that did the work. See "Deviations from Plan" below.

## Performance

- **Duration:** unknown — the executor session that produced the two task commits was interrupted before it could record its own timing; not fabricated here.
- **Completed:** 2026-07-28 (commit timestamps: `68dbc60` 19:59:40, `477567d` 20:01:21, both +0300)
- **Tasks:** 2/2
- **Files modified:** 2 (1 new test file, 1 modified test file; zero production source files touched)

## Accomplishments

- **VER-03** (`VER03BaselineSizeSnapshotTest.kt`, new, 395 lines): a private `BASELINE: Map<String, Dp>` holds 14 identity-keyed entries (`"<Component>.<property>"`) — every value read directly from git tag `v2.0.4` per D-10, not a frozen-current snapshot. Inside `runComposeUiTest`, `setContent` pins `LocalDensity` to `Density(1f)` and composes `AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroProgressBar`, and `AeroRangeSlider`, measuring 7 sizes via `onNodeWithTag(...).getUnclippedBoundsInRoot()`. Because corner radii don't participate in layout and their backing constants are `private`, 6 more values (`AeroButton.cornerRadius`, `AeroOutlinedButton.cornerRadius`, `AeroSwitch.thumbSize`, `AeroSwitch.trackCornerRadius`, `AeroSwitch.thumbCornerRadius`, `AeroSegmentedControl.cornerRadius`, `AeroListItem.rowMinHeight`) are extracted from comment-stripped source text via a new `dpLiteralAfter(source, marker)` helper built on the same `stripComments`/`sourceFile` idiom Plan 20-01 established. The pure `baselineDeviations(measured, expected): List<String>` compares by exact `Dp` equality (no float tolerance) and is proven by 5 in-file fixtures: a clean match (empty result), one key shifted +1.dp (flagged), one key shifted -1.dp (flagged), an empty measured map (flagged, never a vacuous pass), and a fixture swapping two keys that coincidentally share the same baseline value in opposite directions (both flagged independently, proving identity-keying rather than an aggregate/positional comparison). The real assertion composes all measurements, asserts `measured.keys == BASELINE.keys` and non-emptiness, then asserts zero deviations.
- **Two documented findings, recorded in the class KDoc, not hidden:** (1) `AeroListItem`'s row height moved from a fixed `.height(36.dp)` at v2.0.4 to today's `.heightIn(min = ROW_MIN_HEIGHT)` — the one approved VER-03 exception, citing the G1 closure at `19-06`; the gate asserts the constant is still exactly `36.dp` AND that the declaration is a `heightIn(min = ...)` floor, never a fixed height again. (2) `AeroSlider` declared no numeric size default of its own at v2.0.4 (confirmed by a direct `git show v2.0.4:...AeroSlider.kt` read — it wrapped Material3's `Slider` with `SliderDefaults.colors(...)` only), so its current `THUMB_DIAMETER = 20.dp` / `TRACK_HEIGHT = 4.dp` (Phase-18-approved substitutions matching `AeroRangeSlider`'s locked dimensions, `18-01`) have no pre-migration number to compare against and `AeroSlider` is deliberately absent from `BASELINE`. The KDoc states plainly that no other exception is authorized.
- **VER-04** (`AeroButtonSemanticsTest.kt`, modified, +58/-10 lines): audited against VER-04's four explicit criteria (see the audit table below). The invocation-count criterion was the only one not already met; the boolean `clicked` flag in both existing key-activation tests (`aeroButtonSpaceKeyInvokesOnClickAfterFocus`, `aeroButtonEnterKeyInvokesOnClickAfterFocus`, `aeroOutlinedButtonEnterKeyInvokesOnClickAfterFocus`) was replaced with an `Int` counter incremented by `onClick`, each test now presses its activation key TWICE and asserts the counter equals exactly `2`. `assertExists()` was added before every `requestFocus()` call across all activation tests, so a missing node fails with an explicit message instead of surfacing as a focus error. The missing `AeroOutlinedButton` Space case (`aeroOutlinedButtonSpaceKeyInvokesOnClickAfterFocus`) was added, mirroring the existing Enter case exactly — so both converted buttons now carry both activation keys. The class KDoc gained a "VER-04 closure" block recording the per-criterion audit and the one deliberately-not-asserted limitation (key-event delivery interleaved with a same-frame recomposition — `runComposeUiTest`'s single-threaded test clock has no equivalent for a real-device input/recomposition race). No new test class was created (D-16); the file went from 5 `@Test` methods to 6.
- Full library suite verified green during this closeout (`./gradlew :library:test --rerun-tasks`, non-cached, 24s): 8 tasks executed, 425 tests across 81 classes, 0 failures, 0 errors, 0 skipped. `VER03BaselineSizeSnapshotTest`: 6/6 pass. `AeroButtonSemanticsTest`: 6/6 pass. `git status --porcelain library showcase` was clean before this closeout's own commit — no production source was touched by either task.

## VER-04 Audit (per-criterion, as recorded in the class KDoc)

| Criterion | Status before this plan | Status after |
|-----------|--------------------------|--------------|
| Both buttons asserted separately | Already met — `AeroButton`/`AeroOutlinedButton` each have their own test methods | Unchanged |
| No vacuous pass on a missing node | Implicit (`onNodeWithText` fails if absent) but not explicit | `assertExists()` now called explicitly before `requestFocus()` in every activation test |
| Order independence | Already met — each test method is its own `runComposeUiTest` composition | Unchanged |
| Invocation count, not "at least once" | NOT met — boolean `clicked` flag only proved the handler fired at least once | Closed — `Int` counter asserted `== 2` after two key presses, in all 4 activation tests including the new Space case |

## Task Commits

Each task was committed atomically (both authored and committed by a prior, interrupted executor session; see Deviations):

1. **Task 1: VER-03 baseline size and corner-radius snapshot gate** - `68dbc60` (feat)
2. **Task 2: Audit AeroButtonSemanticsTest against VER-04's criteria and strengthen the invocation-count assertion** - `477567d` (test)

**Plan metadata:** this closeout commit created immediately after this SUMMARY (see repository history).

## Files Created/Modified

- `library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt` - new VER-03 gate: `BASELINE`, `baselineDeviations`, `baselineShiftedBy`, `dpLiteralAfter`, `stripComments`, `sourceFile`, 6 `@Test` methods
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` - VER-04 closure: boolean flags replaced with `Int` invocation counters, `assertExists()` added to all activation tests, `aeroOutlinedButtonSpaceKeyInvokesOnClickAfterFocus` added (6 `@Test` methods, up from 5)

## Decisions Made

- **BASELINE's 14 entries and their three deliberate value-coincidences** (see key-decisions in frontmatter) — asserted independently via the `twoCoincidentBaselineKeysDriftingOppositeDirectionsAreBothCaught` fixture, so a comparator that summed or averaged deviations instead of comparing per-key could not silently pass a real regression.
- **Exactly two authorized VER-03 exceptions** (AeroListItem G1/19-06 min-height change; AeroSlider's absent v2.0.4 baseline) — both recorded in the class KDoc with no further exception authorized; any other divergence turns the gate red and must be fixed in the drifted component, not absorbed by a new table entry.
- **VER-04 closed by strengthening the existing test class, not a new one (D-16)** — the audit found three of four criteria already satisfied; only the invocation-count gap required a code change, made inside the pre-existing `AeroButtonSemanticsTest`.

## Deviations from Plan

### Process deviation (not a code deviation): retroactive SUMMARY after an interrupted executor

The executor session that authored and committed both tasks (`68dbc60`, `477567d`) did not reach its own closeout step (SUMMARY.md, STATE.md/ROADMAP.md/REQUIREMENTS.md updates, final metadata commit) before being interrupted. No `.planning/async-jobs/` manifest existed for this plan at the time of interruption, meaning this was an unplanned abort rather than a deliberate, resumable deferral. Per the user's explicit instruction, this SUMMARY was written retroactively by a separate closeout-only pass: reading the plan text, both committed diffs (`git show 68dbc60`, `git show 477567d`), and the final committed file contents, then re-running the full test suite (`./gradlew :library:test --rerun-tasks`) to confirm the committed work is green before documenting it as complete. No task was re-executed and no file under `library/` or `showcase/` was modified by this closeout.

### Auto-fixed Issues

None recorded — no deviation from the plan's substance was found in either committed diff. Both files match the plan's `<action>` blocks:
- VER-03's `BASELINE` has 14 entries (plan specified 14), the 5 fixture tests plus 1 real-measurement test are all present, density is pinned via `LocalDensity provides Density(1f)`, no float-tolerance construct appears in the comparison, and the class KDoc names both `AeroListItem` and `AeroSlider` findings, cites `19-06`, and states no further exception is authorized — matching all of the plan's acceptance criteria for Task 1.
- VER-04's audit found 3 of 4 criteria already met and closed the 4th exactly as specified (Int counter, `assertEquals(2, ...)`, `assertExists()` before `requestFocus()`, added `AeroOutlinedButton` Space case) without creating a new test class — matching Task 2's acceptance criteria, including the file going from 5 to 6 `@Test` methods.

## Issues Encountered

- The executor producing the committed work did not complete its own closeout (see Deviations above). This closeout pass re-verified the full suite from a clean re-run rather than trusting the interrupted session's own (unrecorded) verification claim.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- VER-03 and VER-04 both close cleanly: VER-03 ships as a normal Gradle test with its D-08 fixture proof living inside the class (D-09), VER-04 is closed on the pre-existing `AeroButtonSemanticsTest` per D-16 with no new class.
- `dpLiteralAfter`/`stripComments`/`sourceFile` are available for reuse by 20-04's SHW-16 label-contrast regression guard, the next plan in this phase.
- No blockers carried forward from this plan. 20-04 (label-contrast fix) and 20-05 (code review) remain for the phase's other plans per ROADMAP.md.

---
*Phase: 20-verification*
*Completed: 2026-07-28*

## Self-Check: PASSED

- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt
- FOUND commit: 68dbc60
- FOUND commit: 477567d
