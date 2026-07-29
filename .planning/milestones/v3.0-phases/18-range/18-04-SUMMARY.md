---
phase: 18-range
plan: 04
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, showcase, three-theme-signoff, glow-ring, drawscope]

# Dependency graph
requires:
  - phase: 18-range
    plan: 01
    provides: "AeroSurfaceStyle.neutralRest, resolveSliderThumbStyle/resolveSliderTrackStyle, AeroSlider's human-approved (\"идеально\") focus/hover/press glow-ring + 1.05x lift calibration"
  - phase: 18-range
    plan: 02
    provides: "AeroRangeSlider restyled onto the Aero primitives (groove/thumbs/active fill), per-thumb hover MutableInteractionSource wiring, DrawScope.inset sub-region-draw idiom"
  - phase: 18-range
    plan: 03
    provides: "AeroProgressBar determinate/indeterminate restyle, showRunningSheen opt-in overlay"
provides:
  - "RangeSection showcase demos exercising every reviewable state (enabled/disabled x hover/focus/press-drag) of AeroSlider/AeroRangeSlider, plus AeroProgressBar's determinate/sheen/indeterminate variants, under the existing three-theme switcher"
  - "DrawScope.drawAeroGlowRing(active, glowColor, cornerPx) — direct-Canvas exposure of aeroGlowRing's bloom-ring draw logic (AeroSurfacePrimitives.kt), matching drawAeroSurfaceCore/drawAeroThumb/drawAeroGroove's existing dual Modifier/direct-Canvas convention; Modifier.aeroGlowRing now delegates to it"
  - "AeroRangeSlider per-thumb hover/press-drag glow-ring + 1.05x radius-lift parity with AeroSlider's human-approved thumb treatment (PRESSED_RADIUS_SCALE), each thumb independent"
  - "Human three-theme (AeroBlue/AeroDark/Classic) visual sign-off PASSED for all three restyled range components, closing Phase 18's VRNG-03/05/06/08 manual verification gate"
affects: [19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dual Modifier/direct-Canvas exposure extended to aeroGlowRing (DrawScope.drawAeroGlowRing) — the fourth Phase 16 primitive (after drawAeroSurfaceCore/drawAeroThumb/drawAeroGroove) to offer both a Modifier wrapper and a directly-callable DrawScope function for Canvas-owning consumers"
    - "Canvas-drawn 'lift' cue: scale the drawn radius passed into inset()/drawAeroThumb (not a graphicsLayer, unavailable inside a raw DrawScope draw call) to approximate a Modifier-drawn sibling's graphicsLayer(scaleX/scaleY) press cue"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt (net unchanged — fix-then-revert round)
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt

key-decisions:
  - "AeroSlider is the cross-component reference for focus/press-drag glow treatment, not AeroRangeSlider — human sign-off initially reported the divergence with the direction reversed (b9cbc08 stripped AeroSlider to match AeroRangeSlider), corrected via an explicit revert (ecb544b) once the human clarified AeroSlider's calibrated ('идеально', Plan 01/a8ce1fd) look was the target"
  - "AeroRangeSlider does NOT get a keyboard-focus ring — its Canvas has no keyboard-focus tracking at all (VRNG-04's pre-existing, unchanged accessibility deferral); adding one would require new per-thumb keyboard-navigation logic, out of this render-only restyle's scope and the drag-loop guardrail. Human-accepted as an explicit, deferred-to-a-later-task scope decision, not a gap in this phase's sign-off"
  - "AeroProgressBar keeps its showRunningSheen=true instance instead of a disabled variant in the showcase — it has no `enabled` parameter (Plan 03's own decision, UI-SPEC's Disabled row scopes to AeroSlider/AeroRangeSlider only) and this plan's prohibitions forbid new public API beyond showRunningSheen; human-accepted as-is"
  - "aeroGlowRing's bloom-ring draw logic extracted into DrawScope.drawAeroGlowRing rather than duplicated inline in AeroRangeSlider — keeps 'one implementation, dual exposure' (Phase 16 architecture boundary) instead of authoring a second gradient/bloom routine for the Canvas path"

requirements-completed: [VRNG-03, VRNG-05, VRNG-06, VRNG-08]

coverage:
  - id: D1
    description: "RangeSection demos exercise all three components across every reviewable state (rest/hover/focus/press-drag/disabled) and both indeterminate + determinate progress, on all three themes (AeroBlue/AeroDark/Classic)"
    requirement: "VRNG-03"
    verification:
      - kind: manual_procedural
        ref: "Live showcase three-theme review (human, this plan's Task 2 checkpoint) — APPROVED"
        status: pass
    human_judgment: true
    rationale: "Visual fidelity across three themes is not unit-testable — this project's mandated gate against the v2.0.3/v2.0.4 false-positive-sign-off lesson. Human confirmed all seven review criteria (groove/raised-gloss/glow/D-04 raised-drag/indeterminate-sweep/disabled-flatten/no-new-overflow) directly."
  - id: D2
    description: "A dragged thumb reads as picked-up (stays raised, glow intensifies) — confirmed independently per thumb, and AeroSlider and AeroRangeSlider now show IDENTICAL hover/press-drag glow-ring + lift treatment (cross-component consistency the human sign-off specifically required)"
    requirement: "VRNG-05"
    verification:
      - kind: unit
        ref: "./gradlew :library:test --tests \"com.mordred.aero.components.range.*\" (AeroRangeSliderDragLogicUntouchedTest confirms drag/keyboard/step logic untouched by the glow/lift additions)"
        status: pass
      - kind: manual_procedural
        ref: "Live showcase three-theme review, round 3 (human, this plan's Task 2 checkpoint, re-presented twice after two fix rounds) — APPROVED"
        status: pass
    human_judgment: true
    rationale: "The 'picked up, not pushed in' read and cross-component visual identity are judgment calls a unit test cannot make — two calibration rounds were needed before the human confirmed parity (see Deviations). Human explicitly accepted AeroRangeSlider's keyboard-focus ring omission as a separate, deferred scope decision, not a defect in this sign-off."
  - id: D3
    description: "The indeterminate bar shows a single left-to-right accent-glass sweep at 1500ms restart with no ping-pong and soft alpha=0f edges (not a hard block on Classic)"
    requirement: "VRNG-08"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt#indeterminateKeepsThe1500msRestartTimingWithNoPingPong (pre-existing, Plan 03, unaffected by this plan)"
        status: pass
      - kind: manual_procedural
        ref: "Live showcase three-theme review (human, this plan's Task 2 checkpoint) — APPROVED"
        status: pass
    human_judgment: true
    rationale: "Sweep motion, restart cadence, and soft-edge fade across three themes (especially the Classic opaque-token edge case) are visual reads a timing-only unit test cannot confirm."
  - id: D4
    description: "AeroProgressBar's determinate default gloss vs. showRunningSheen=true traveling highlight, and both sliders' D-07 disabled flatten, read correctly across all three themes; restyle introduces no NEW overflow beyond the pre-existing tooltip pills (backstop)"
    requirement: "VRNG-06"
    verification:
      - kind: manual_procedural
        ref: "Live showcase three-theme review (human, this plan's Task 2 checkpoint) — APPROVED, including the backstop overflow check"
        status: pass
    human_judgment: true
    rationale: "Gloss/sheen distinctness, disabled-flatten depth removal, and overflow-regression confirmation are all visual judgments — the backstop item is explicitly a human-confirmed-at-review statement per 18-UI-SPEC.md's UI Considerations table."

# Metrics
duration: 19min
completed: 2026-07-24
status: complete
---

# Phase 18 Plan 04: RangeSection Showcase Expansion + Three-Theme Sign-Off Summary

**RangeSection expanded to exercise every reviewable state of AeroSlider/AeroRangeSlider/AeroProgressBar across three themes, and the human three-theme visual sign-off PASSED after two calibration rounds unified AeroSlider's and AeroRangeSlider's focus/press-drag glow-ring + lift reaction (AeroSlider as the reference, AeroRangeSlider's keyboard-focus ring explicitly deferred out of scope).**

## Performance

- **Duration:** ~19 min (12:52:07 → 13:11:16 +03:00, task commits only; checkpoint round-trips with the human took longer wall-clock)
- **Started:** 2026-07-24T12:52:07+03:00 (Task 1 commit)
- **Completed:** 2026-07-24T13:11:16+03:00 (final fix-round commit, prior to sign-off approval)
- **Tasks:** 2 (Task 1 showcase expansion + Task 2 checkpoint, resolved after 2 coordinator-directed fix rounds)
- **Files modified:** 5 (1 showcase file, 3 library files touched net, 1 test file; `AeroSlider.kt` net-unchanged after a fix-then-revert round)

## Accomplishments
- `RangeSection.kt` now instantiates `AeroSlider`/`AeroRangeSlider` with both `enabled = true` and `enabled = false` (D-07 flatten-to-dead reviewable), and `AeroProgressBar` with its default determinate gloss, a `showRunningSheen = true` determinate instance, and the indeterminate overload — all under the existing three-theme switcher, no forced-state static styling.
- Human three-theme (AeroBlue/AeroDark/Classic) visual sign-off **PASSED** for all three restyled range components against 18-UI-SPEC.md's per-state contract, after resolving a cross-component consistency defect the review itself caught.
- Cross-component consistency fix (2 rounds): the human flagged `AeroSlider` and `AeroRangeSlider` reacting differently to focus/press-drag. Round 1 mistakenly stripped `AeroSlider`'s calibrated treatment to match `AeroRangeSlider` (`b9cbc08`); the human corrected the direction — `AeroSlider`'s Plan 01 human-approved ("идеально") focus glow ring, press/drag glow ring, and 1.05x lift are the reference — so `b9cbc08` was reverted (`ecb544b`) and `AeroRangeSlider` was instead given the same per-thumb treatment (`4c8f0b5`).
- `aeroGlowRing`'s bloom-ring draw logic extracted into a new `DrawScope.drawAeroGlowRing(active, glowColor, cornerPx)` in `AeroSurfacePrimitives.kt` — the fourth Phase 16 primitive to gain a direct-Canvas exposure alongside its Modifier wrapper (matching `drawAeroSurfaceCore`/`drawAeroThumb`/`drawAeroGroove`'s existing convention) — so `AeroRangeSlider`'s per-thumb hover/press rings reuse the identical bloom-ring implementation rather than a second one.
- `AeroRangeSlider`'s per-thumb draw now shows: a hover-only ring, a press/drag ring (`onSurface`-derived neutral hue, suppressed while hover-only), and a 1.05x radius lift while pressed/dragging (`PRESSED_RADIUS_SCALE`) — each thumb independently, keyed off its own `MutableInteractionSource`/`activeThumb` state. The gloss boost was already present via the shared `resolveSliderThumbStyle` resolver (Plan 02).
- **Deliberately NOT added:** a keyboard-focus ring on `AeroRangeSlider` — its Canvas has no keyboard-focus tracking (pre-existing VRNG-04 accessibility deferral); adding one would be new keyboard-navigation logic between two thumbs, out of this render-only restyle's scope. Human explicitly accepted this as a deferred, separate-task scope decision during final sign-off — not a gap in this phase.
- Fixed 3 `AeroSurfacePrimitivesTest` regression guards (bloom-edge-anchor, no-`rememberInfiniteTransition`, no-single-`radialGradient`) that the `drawAeroGlowRing` extraction would otherwise have made vacuously-passing — rescoped to scan the new function's body, where the real draw calls now live.
- Full `:library:test` suite (320 tests) green throughout, including `AeroRangeSliderDragLogicUntouchedTest` (drag/keyboard/step logic confirmed byte-for-byte untouched by all glow/lift additions — every change lives in the Canvas draw lambda, never inside the `pointerInput`/`awaitPointerEventScope` blocks).

## Task Commits

Each task (and each human-sign-off-driven fix round) was committed atomically:

1. **Task 1: Expand RangeSection to exercise all states x three themes** - `4292da3` (feat)
2. **Fix round 1 (superseded): align AeroSlider to AeroRangeSlider** - `b9cbc08` (fix) — reversed direction, corrected next
3. **Fix round 1 revert: restore AeroSlider's human-approved reaction** - `ecb544b` (revert)
4. **Fix round 2: give AeroRangeSlider the AeroSlider focus/press glow + lift reaction** - `4c8f0b5` (feat)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt` - Added enabled/disabled instances for both sliders and a `showRunningSheen=true` `AeroProgressBar` instance
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` - Touched twice (fix then revert); net byte-identical to its pre-plan state
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` - Per-thumb hover/press-drag glow ring + 1.05x radius lift added to the Canvas draw block, matching `AeroSlider`'s thumb slot
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` - New `DrawScope.drawAeroGlowRing`; `Modifier.aeroGlowRing` now delegates to it
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt` - 3 regression guards rescoped to also cover `drawAeroGlowRing`'s body

## Decisions Made
- AeroSlider is the cross-component visual reference for focus/press-drag glow treatment (not AeroRangeSlider) — corrected mid-checkpoint after an initial reversed defect report.
- AeroRangeSlider's keyboard-focus ring is explicitly out of scope for this phase (human-accepted, deferred to a later separate task) — its Canvas has no keyboard-focus tracking at all, and adding one would be new logic beyond a render-only restyle.
- AeroProgressBar's showcase gets a `showRunningSheen=true` instance instead of a disabled variant — it has no `enabled` parameter (Plan 03's decision, human-accepted as-is here too).
- `aeroGlowRing`'s bloom-ring logic was extracted into a direct-`DrawScope` function rather than duplicated in `AeroRangeSlider.kt`, keeping "one implementation, dual exposure" consistent with the other three Phase 16 primitives.

## Deviations from Plan

### Auto-fixed / Human-Sign-Off-Driven Issues

**1. [Human visual sign-off] AeroSlider and AeroRangeSlider reacted differently to focus and press/drag**
- **Found during:** Task 2 checkpoint, first human three-theme review
- **Issue:** `AeroSlider`'s thumb slot layered a stacked focus glow ring, a press/drag glow ring, and a 1.05x scale-up on top of the shared resolver's fill/gloss output; `AeroRangeSlider`'s Canvas-drawn thumbs had none of it.
- **Fix (round 1, superseded):** Stripped `AeroSlider` down to match `AeroRangeSlider` (removed its glow rings + scale) — `b9cbc08`.
- **Correction:** The human clarified the reference direction was reversed — `AeroSlider`'s Plan 01 human-approved ("идеально") treatment is the target. `b9cbc08` reverted via `ecb544b`; `AeroRangeSlider` instead given the same per-thumb glow-ring + lift treatment (`4c8f0b5`), extracting a new `DrawScope.drawAeroGlowRing` so both sliders share one bloom-ring implementation.
- **Files modified:** `AeroSlider.kt` (net unchanged), `AeroRangeSlider.kt`, `AeroSurfacePrimitives.kt`, `AeroSurfacePrimitivesTest.kt`
- **Verification:** `./gradlew :library:compileKotlin`/`:showcase:compileKotlin` green; `./gradlew :library:test` (320 tests) green including `AeroRangeSliderDragLogicUntouchedTest`; human re-reviewed and APPROVED
- **Committed in:** `b9cbc08`, `ecb544b`, `4c8f0b5`

**2. [Rule 1 - Bug, caused by own refactor] Three `AeroSurfacePrimitivesTest` guards became vacuously-passing**
- **Found during:** Full `:library:test` run after extracting `DrawScope.drawAeroGlowRing`
- **Issue:** `aeroGlowRingFirstBloomRingTouchesTheSurfaceEdgeWithNoDeadGap` failed outright (the bloom-anchor code moved out of the `aeroGlowRing`-named function the test scanned); two sibling guards (`noRememberInfiniteTransitionInAeroGlowRingBody`, `aeroGlowRingOuterBloomIsNotASingleGradientCenteredOnTheBox`) would have kept passing but no longer against the real draw code.
- **Fix:** Rescoped all three (plus `noColorTransparentInNewPrimitiveFunctionBodies`) to also scan `drawAeroGlowRing`'s body, where the bloom-ring draw calls now live.
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt`
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.theme.AeroSurfacePrimitivesTest"` green; full suite (320 tests) green
- **Committed in:** `4c8f0b5`

---

**Total deviations:** 1 human-sign-off-driven cross-component consistency fix (2 rounds, one superseded and reverted) + 1 auto-fixed Rule 1 test-regression caused by the fix's own refactor.
**Impact on plan:** No architectural change — same shared resolver, same primitives (with one new direct-Canvas exposure of an existing bloom-ring implementation), same render-only boundary throughout. `AeroRangeSliderDragLogicUntouchedTest` confirms the drag/keyboard/step logic guard held through all three fix commits.

## Issues Encountered
- Compose Desktop doesn't hot-reload — the showcase process was killed (`taskkill //F //IM java.exe //T`) and relaunched (`./gradlew :showcase:run`) after each fix round before re-presenting the checkpoint, mirroring Plan 01's own precedent.
- The initial human defect report named the wrong component as the reference (see Deviation #1) — resolved by an explicit `git revert` rather than trying to hand-reconstruct `AeroSlider`'s prior state, keeping the human-approved Plan 01 calibration byte-identical.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 18 (Range) is now complete: all three components (`AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`) restyled onto the shared Aero primitives layer, cross-component focus/press-drag visual consistency confirmed, and the human three-theme sign-off passed.
- `DrawScope.drawAeroGlowRing` is available as a reusable direct-Canvas primitive for any future Canvas-owning Aero component needing a glow ring (e.g. Phase 19's `AeroSwitch`/`AeroSegmentedControl` if either turns out to be Canvas-drawn).
- Open, human-accepted scope item for a later task: `AeroRangeSlider` keyboard-focus ring parity with `AeroSlider` (requires new per-thumb keyboard-navigation logic, deliberately deferred).
- No blockers. Full `:library:test` suite (320 tests) green after this plan's changes.

---
*Phase: 18-range*
*Completed: 2026-07-24*

## Self-Check: PASSED

All modified files (`RangeSection.kt`, `AeroSlider.kt`, `AeroRangeSlider.kt`, `AeroSurfacePrimitives.kt`, `AeroSurfacePrimitivesTest.kt`) and all four commits (`4292da3`, `b9cbc08`, `ecb544b`, `4c8f0b5`) verified present on disk / in git history (see Self-Check step below).
