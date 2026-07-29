---
phase: 19-selectors-lists
plan: 11
subsystem: ui
tags: [compose-desktop, kotlin, hover, interaction-source, focus-visibility, code-review-gap-closure]

requires:
  - phase: 19-selectors-lists
    provides: plans 19-09 (FocusVisibility reducer one-arg fix) and 19-10 (raised-segment fill contrast) — WR-04's reducer desync risk widens exactly because 19-09 made the reducer's hover field survive longer
provides:
  - AeroSegmentedControl per-segment composition keyed on index-and-value identity (WR-03 closed)
  - Exactly one hover emitter per interaction source across AeroSwitch, AeroSegmentedControl and AeroListItem (WR-04 closed)
  - A behavioural HoverEmissionTest suite proving hover still works against the library's own Compose build, not a decompiled artifact
  - Three source guards rewritten from bare-token presence to an exactly-one-emitter contract
affects: [19-12 (three-theme re-sign-off), 20-verification]

tech-stack:
  added: []
  patterns:
    - "key(index, opt) around a forEachIndexed loop body to key per-item remembered state to item identity rather than slot position"
    - "Component-treatment-by-modifier-shape: whether an explicit hover emitter must be removed, kept, or made conditional depends on whether the component's OTHER interaction modifier on that source already emits hover"
    - "Premise-then-removal: prove a claim about platform modifier behaviour with a real composed test BEFORE removing code based on that claim"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/common/HoverEmissionTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt

key-decisions:
  - "Segment key uses index-and-value, not value alone, because duplicate options are still unguarded (WR-07 deferred) and a value-only key can collide"
  - "The switch's explicit hover emitter is deleted outright; the segmented control's per-segment emitter is deleted while its outer-strip emitter stays; the list row's emitter is moved into the else-branch of its existing click-handler condition — three different treatments because the three components' OTHER interaction modifiers relate to the shared source differently"
  - "The premise that toggleable/selectable/clickable emit hover on their own was proven with a new composed test suite run against the library's own Compose build BEFORE any removal, per the plan's explicit fallback-clause requirement"

requirements-completed: [VSEL-02, VSEL-04, VLST-03, VLST-04]

coverage:
  - id: D1
    description: "Each segment's remembered interaction source, focus-visibility state and in-flight selection tween are keyed to that segment's own identity via key(index, opt), so reordering/inserting/removing options cannot transfer one option's state to another"
    requirement: "VSEL-04"
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest#sourceKeysEachSegmentOnItsOwnIdentity"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSemanticsTest (all three tests)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every interaction source in AeroSwitch, AeroSegmentedControl and AeroListItem has exactly one hover emitter (was two), removing the WR-04 desync risk against the boolean focus-visibility reducer"
    requirement: "VLST-04"
    verification:
      - kind: unit
        ref: "HoverEmissionTest (all 5 tests)"
        status: pass
      - kind: unit
        ref: "AeroSwitchSourceTest#aeroSwitchCollectsHoverThroughOneSharedInteractionSourceNotRawPointerTracking"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSourceTest#sourceHasExactlyOneHoverEmitterAndNoRawPointerTracking"
        status: pass
      - kind: unit
        ref: "AeroListItemSourceTest#aeroListItemHasExactlyOneHoverEmitterOnEachPathAndNoRawPointerTracking"
        status: pass
    human_judgment: false
  - id: D3
    description: "AeroSegmentedControl's per-segment hover after emitter removal — no direct behavioural test exists because segment sources are created inside the component and are not reachable from a test"
    requirement: "VSEL-02"
    verification: []
    human_judgment: true
    rationale: "Segment-level interaction sources are private to the component's composition and cannot be hoisted into a test. Coverage is indirect (the generic selectable premise test in HoverEmissionTest plus the source guard proving exactly one emitter remains); direct visual confirmation is deferred to the human three-theme sign-off in plan 19-12, as this plan's task instructions explicitly require documenting rather than fabricating."

duration: 20min
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 11: Segment Identity Keying and Single Hover Emitter Summary

**Segment state now keyed to option identity via `key(index, opt)`, and every one of the three restyled components (switch, segmented control, list row) carries exactly one hover emitter instead of two — closing WR-03 and WR-04, both proven with new behavioural tests run against the library's own Compose build rather than a decompiled artifact.**

## Performance

- **Duration:** ~20 min
- **Completed:** 2026-07-28
- **Tasks:** 3
- **Files modified:** 7 (1 new test file, 3 production files, 3 test files)

## Accomplishments

- **WR-03 closed:** `AeroSegmentedControl`'s per-segment `forEachIndexed` loop body is wrapped in `key(index, opt)`, so a segment's remembered interaction source, focus-visibility state and in-flight selection tween can no longer be inherited by a different option after an insert/remove/reorder. Keyed on index-and-value (not value alone) because duplicate options remain unguarded (WR-07 deferred) — a value-only key would collide on that input.
- **WR-04 prerequisite proven, not assumed:** a new `HoverEmissionTest` suite proves — against the library's own Compose 1.11.1 build, not a decompiled artifact — that `toggleable`, `selectable` and `clickable` each emit hover on the `MutableInteractionSource` they are handed, with zero explicit hover modifier on the test node. All three passed on the FIRST run, so no removal in Task 3 was blocked by a false premise.
- **WR-04 closed on all three components**, each with the treatment its own modifier shape requires:
  - `AeroSwitch`: explicit hover emitter deleted outright — the `toggleable` modifier (always applied, WR-06 deferred) is now the switch's single emitter.
  - `AeroSegmentedControl`: the per-segment duplicate emitter deleted; the outer strip's emitter kept, because the control-level source has no other interaction modifier feeding it.
  - `AeroListItem`: the previously-unconditional emitter moved into the else-branch of the existing `onClick != null` condition, so the clickable path's sole emitter is the click modifier and the display-only path's sole emitter is the (now enabled-aware) explicit hover modifier.
- Two more `HoverEmissionTest` tests prove hover still works on the real, modified components: `AeroSwitch` reports hover with no explicit hover modifier, and `AeroListItem` reports hover on both the clickable and the display-only path.
- All three source-scan suites rewritten from "hoverable( is present" to "exactly one hoverable( occurrence" (or, for the switch, "zero"), closing the gap where a bare-presence guard could not detect a duplicate emitter.
- Segmented control also gained a pinning assertion (`interactionSource = segSource`) and the switch guard's KDoc was corrected to no longer claim the toggle modifier "does not report hover for free" — that claim is refuted and now covered behaviourally instead.

## Task Commits

Each task was committed atomically:

1. **Task 1: key each segment on its own identity (WR-03)** - `77bfa59` (feat)
2. **Task 2: prove the premise — interaction modifiers emit hover on their source (WR-04 prerequisite)** - `ddd7bf4` (test)
3. **Task 3: one hover emitter per interaction source, across all three components (WR-04)** - `4048c57` (fix)

_Note: this plan's tasks are not TDD-shaped (task 2 is a standalone premise-proof, not a RED phase for task 3's production change), so the `test`→`fix` commit shape here documents the premise-then-removal discipline the plan mandated, not a strict TDD RED/GREEN pair._

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` - per-segment loop body wrapped in `key(index, opt)`; per-segment duplicate hover emitter removed; outer-strip emitter kept
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` - explicit hover emitter and its now-unused import removed; toggle modifier is the sole emitter
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` - explicit hover emitter moved into the else-branch of the click-handler condition, now passing `enabled`
- `library/src/test/kotlin/com/mordred/aero/components/common/HoverEmissionTest.kt` - new: 3 premise tests (toggleable/selectable/clickable emit hover) + 2 component-level tests (switch, list row both paths)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` - new `sourceKeysEachSegmentOnItsOwnIdentity` test; hover guard rewritten to exactly-one-emitter contract with `interactionSource = segSource` pin
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` - hover guard renamed/rewritten to exactly-one-emitter contract; new `nonCommentSource` accessor; class KDoc's VLST-04 bullet corrected
- `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt` - hover guard renamed/rewritten to exactly-one-emitter contract with enabled-flag pin

## Decisions Made

- **Index-and-value key, not value-only:** the review's primary suggestion was value-only, but WR-07 (forbidding duplicate options) is deferred this round, so a value-only key can collide on input the component does not reject. Index-and-value fully closes WR-03 at the cost of not preserving a moved option's state across a reorder — the strictly safer of the two behaviours while duplicates remain unguarded.
- **Three different removal treatments, not one:** confirmed via `HoverEmissionTest`'s premise tests that the correct fix is per-component, not uniform — the switch's toggle modifier is unconditional (safe to delete the emitter outright), the segmented control's outer strip has no other emitter (must keep it), and the list row's display-only path has no interaction modifier at all (the emitter must move to that branch, not disappear).
- **Removal-blocking fallback clause was defined but never triggered:** all 5 `HoverEmissionTest` tests passed on first run for both the premise (Task 2) and the post-removal component-level checks (Task 3), so no component's removal was reverted.

## Deviations from Plan

None - plan executed exactly as written. All acceptance criteria in Tasks 1-3 were verified via the exact `awk`/`grep` commands specified in the plan, and all matched the expected counts on first check.

## Issues Encountered

None.

## Known Stubs

None. No hardcoded empty values, placeholder text, or unwired data sources were introduced by this plan's changes — production edits were all modifier-chain rewiring and composition keying, no new UI surface.

## Premise Test Results (Task 2, recorded before Task 3's edits per plan requirement)

Run against the unmodified components, before any removal:

- `toggleableEmitsHoverOnItsSuppliedInteractionSource` — **PASS**
- `selectableEmitsHoverOnItsSuppliedInteractionSource` — **PASS**
- `clickableEmitsHoverOnItsSuppliedInteractionSource` — **PASS**

All three passed on the first run. No component's Task 3 removal was blocked or reverted.

## Component-Level Post-Removal Results (Task 3)

- `aeroSwitchStillReportsHoverWithNoExplicitHoverModifier` — **PASS**
- `aeroListItemReportsHoverOnBothTheClickableAndTheDisplayOnlyPath` — **PASS**

`AeroSegmentedControl`'s per-segment hover has **no direct behavioural test**: its segment sources are created inside the component (`remember { MutableInteractionSource() }` per segment) and are not reachable from an external test. Its coverage is the generic `selectableEmitsHoverOnItsSuppliedInteractionSource` premise test (proves the underlying modifier behaviour) plus the source guard `sourceHasExactlyOneHoverEmitterAndNoRawPointerTracking` (proves the wiring shape) plus the human confirmation deferred to plan 19-12's three-theme sign-off. No assertion was fabricated to claim otherwise.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- WR-03 and WR-04 are closed; the reducer's one-shared-source-per-component invariant (CR-02's fix, from plan 19-09) is now strengthened rather than weakened, since every component has exactly one hover emitter feeding it.
- Plan 19-12 (three-theme re-sign-off) should visually confirm the segmented control's per-segment hover still renders correctly on all three themes — this is the human-judgment coverage item (D3 above) this plan could not close automatically.
- No blockers.

## Self-Check: PASSED

All 7 created/modified files verified present on disk; all 3 task commits (`77bfa59`, `ddd7bf4`, `4048c57`) and the docs commit (`299ff51`) verified present in git log.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*
