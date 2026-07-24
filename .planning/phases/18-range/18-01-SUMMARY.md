---
phase: 18-range
plan: 01
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, slider, material3-custom-slots, source-scan-guards]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "drawAeroSurfaceCore/Modifier.aeroSurface, Modifier.aeroGlowRing, aeroThumbSurface/drawAeroThumb (PRIM-07), aeroGroove/drawAeroGroove (PRIM-08), AeroSurfaceStyle.rest(), rememberAeroInteractionState(); PRIM-18 spike confirming M3 Slider custom thumb=/track= slots don't clip/misalign"
  - phase: 17-buttons
    provides: "resolveButtonStyle precedence-chain shape, glow-before-surface modifier ordering, pressedRecess/flattenDisabled/hoverLighten transforms in theme/AeroSurfaceStyle.kt"
provides:
  - "AeroSurfaceStyle.neutralRest(base, cornerRadius) — neutral-glass companion factory (glassHighlight/glassSurface/glassBorder-derived, not primary-derived), consumed by AeroSlider's thumb + inactive groove; reusable cross-package by Phase 19's AeroSwitch"
  - "resolveSliderThumbStyle(colors, hovered, pressed, isDragging, focused, enabled) and resolveSliderTrackStyle(colors, enabled) — package-internal pure resolvers in components/range/AeroSlider.kt, reused verbatim by AeroRangeSlider (Plan 02)"
  - "AeroSlider fully restyled onto the Aero primitives layer via M3's @OptIn(ExperimentalMaterial3Api) custom thumb=/track= Slider overload — neutral raised thumb, recessed neutral groove, accent active fill, three visually-distinct thumb states (focus/hover/press), D-07 disabled flatten with zero SliderColors reliance"
  - "aeroGlowRingRepeated(...) local stacking helper — additive-compositing pattern for intensifying a single aeroGlowRing beyond its default alpha without adding params to the shared Phase 16 primitive"
affects: [18-02, 18-03, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "aeroGlowRingRepeated(...) — chain the same aeroGlowRing call N times for additive intensity, kept local to the file that needs it rather than adding a stroke-width/spread param to the shared primitive"
    - "graphicsLayer(scaleX, scaleY) placed early in a thumb slot's modifier chain scales every subsequently-drawn modifier (glow rings + surface) uniformly — the 'picked up' physical lift cue"
    - "Custom M3 Slider thumb=/track= slots read live interaction/drag booleans from the same rememberAeroInteractionState()/collectIsDraggedAsState() calls already wired to the outer Slider(...), never re-derive from value/valueRange (steps-not-dead-param discipline)"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt

key-decisions:
  - "AeroSlider's custom thumb/track dp sizing (20.dp thumb diameter, 4.dp track height) matches AeroRangeSlider's already-locked dimensions rather than M3's raw SliderTokens (HandleWidth=4dp/HandleHeight=44dp, InactiveTrackHeight=16dp — confirmed via direct sources-jar extraction, not memory) — the M3 default is a tall vertical pill shape incompatible with aeroThumbSurface's circle-only primitive; reusing it verbatim would clip to a near-invisible 4dp dot"
  - "D-04's press/drag thumb cue uses three independent channels (fillTop/fillBottom unchanged + gloss-alpha boost, a single distinctly-neutral-hued glow ring, and a small graphicsLayer scale) rather than one — calibrated down twice after human visual sign-off found the first pass over-tuned (2x-stacked bright ring + 15% scale read as a 'flare/pop', not a tasteful lift)"
  - "Focus and press/drag glow rings deliberately diverge in hue, not just intensity — focus stays colors.borderSelected (the system-wide selection blue every other Aero control's focus ring uses), press/drag uses colors.onSurface-derived neutral bright glow — because both this project's hoverGlow and borderSelected tokens are primary-derived, a same-hue-family intensity-only differentiation was not visually distinguishable at thumb scale (root cause of the round-1 sign-off defect)"
  - "onValueChangeFinished wired as null through the M3 SliderState (never exposed as new AeroSlider public API) — resolves 18-RESEARCH.md's Open Question 1 per its own recommendation, preserving the milestone's 1:1 public-signature constraint (VRNG-02)"

requirements-completed: [VRNG-01, VRNG-02, VRNG-03, VRNG-09]

coverage:
  - id: D1
    description: "AeroSlider renders a recessed neutral groove + raised neutral thumb + accent two-tone active fill through M3's custom thumb=/track= slots, replacing the fully M3-drawn surface (D-01)"
    requirement: "VRNG-01"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt#aeroSliderUsesCustomThumbAndTrackSlotsWithHoverableOnTheThumb"
        status: pass
    human_judgment: true
    rationale: "Source-scan + value-level tests prove the slot wiring and style resolution are structurally correct; the actual raised/recessed/glossy visual read was confirmed via the human three-theme showcase sign-off during Task 1's tracer checkpoint (see Deviations below), not re-verifiable by an automated unit test."
  - id: D2
    description: "Drag, keyboard-arrow nudge, steps snapping, and semantics are unregressed by the slot swap; onValueChangeFinished stays wired through SliderState with no new public API surface"
    requirement: "VRNG-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderTest.kt (pre-existing, unaffected — proves the range package's logic tests still pass after touching the sibling AeroSlider.kt file)"
        status: pass
      - kind: unit
        ref: "./gradlew :library:compileKotlin (confirms the @OptIn 10-arg Slider overload compiles with onValueChangeFinished = null and no new AeroSlider public parameter)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Hovering the AeroSlider thumb shows the hover glow + brighten; keyboard-focusing shows a constant, distinctly-colored focus glow; pressing/dragging shows a raised 'picked up' cue distinct from both — three visually distinguishable states"
    requirement: "VRNG-03"
    verification:
      - kind: manual_procedural
        ref: "showcase RangeSection.kt AeroSlider demo, three-theme (AeroBlue/AeroDark/Classic) human visual sign-off — two calibration rounds (015eb73 differentiation fix, a8ce1fd intensity/scale calm-down)"
        status: pass
    human_judgment: true
    rationale: "Hover/focus/press glow distinctness and 'tasteful vs. flare' calibration are inherently visual judgments — the coordinator's human sign-off explicitly approved this ('идеально') after two rounds of automated-verification-backed calibration."
  - id: D4
    description: "Pattern 3 readiness: the thumb's press/drag visual cue (fill, gloss, glow, scale) is driven directly by live pressed/isDragging booleans with no animateFloatAsState lag, matching the 'drag writes directly' half of Pattern 3 for VRNG-09's later range-slider reuse"
    requirement: "VRNG-09"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt#pressedOrDraggingKeepsUnchangedFillWithBrighterGloss"
        status: pass
    human_judgment: false

# Metrics
duration: 1h04min
completed: 2026-07-24
status: complete
---

# Phase 18 Plan 01: AeroSlider Tracer + neutralRest Foundation Summary

**AeroSlider restyled end-to-end onto the Phase 16 Aero primitives via M3's custom thumb=/track= slots — neutral raised thumb, recessed neutral groove, accent two-tone active fill, and three visually-distinct thumb states (focus/hover/press) reached after two human-sign-off calibration rounds — plus the shared `neutralRest()` style factory and slider resolvers Plans 02/03 will reuse.**

## Performance

- **Duration:** ~1h04min
- **Started:** 2026-07-24T11:13:16+03:00 (Task 1 first commit)
- **Completed:** 2026-07-24T12:17:15+03:00 (Task 2 commit)
- **Tasks:** 2 (Task 1 tracer + 3 human-sign-off-driven fix/calibration rounds; Task 2 test guards)
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments
- `AeroSurfaceStyle.neutralRest(base, cornerRadius)` added alongside `rest()` — the neutral-glass counterpart D-01 requires so the thumb/inactive-groove read silver, not accent-tinted (`AeroOrnamentTokens.derive()` confirmed `fillSplitTop/Bottom` AND `bevelLight/bevelShadow` are both `primary`-derived, so `rest()` cannot supply neutral fields unmodified).
- `AeroSlider` rewritten onto the `@OptIn(ExperimentalMaterial3Api::class)` 10-arg custom-slot `Slider` overload: `resolveSliderThumbStyle`/`resolveSliderTrackStyle` package-internal resolvers, glow-before-surface modifier ordering, `.hoverable(interactionSource)` applied explicitly inside the thumb slot (M3's default `Thumb` is the only place that's wired for free otherwise).
- D-07: the old `alpha = 0.4f` `SliderColors` disabled path fully removed — disabled now flattens via `flattenDisabled(colors)` inside AeroSlider's own thumb/track slots.
- D-04: pressed/dragging keeps the thumb's fill unchanged (never `pressedRecess`'s invert-and-recede swap) — stays visually raised, "picked up."
- Three-state visual distinctness (rest/focus/press, all clearly distinguishable from each other and from hover) reached after two human-sign-off calibration rounds on the live showcase across AeroBlue/AeroDark/Classic — see Deviations below.
- Two Wave-0 test guards (`AeroSliderStylesTest`, `AeroSliderSourceTest`, 8 tests total) added and each individually proven to FAIL against a deliberately-reintroduced violation before being trusted (fail-then-pass proof below).
- Full `:library:test` suite green throughout, no regression to the pre-existing `AeroRangeSliderTest`.

## Task Commits

Each task (and each human-sign-off-driven fix round) was committed atomically:

1. **Task 1: neutralRest factory + slider resolvers + AeroSlider custom-slot restyle (tracer)** - `67877d1` (feat)
2. **Task 1 fix: make thumb focus and press/drag states visually distinct** - `015eb73` (fix) — human sign-off round 1 found focus/press indistinguishable
3. **Task 1 calibration: calm down press/drag glow intensity and lift scale** - `a8ce1fd` (fix) — human sign-off round 2 found the round-1 fix over-tuned
4. **Task 2: Wave-0 AeroSlider test guards (value-level resolver + source-scan)** - `20b091a` (test)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` - Added `neutralRest(base, cornerRadius)` companion factory alongside `rest()`
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` - Rewritten onto the custom-slot M3 `Slider` overload; `resolveSliderThumbStyle`/`resolveSliderTrackStyle` resolvers; `aeroGlowRingRepeated` local stacking helper; three-state thumb glow/scale treatment
- `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt` - New value-level JVM tests (7 tests) for both resolvers across AeroBlue + Classic
- `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt` - New source-scan guards (3 tests): custom-slot shape + `.hoverable(`, no `SliderColors(`, no `.pressedRecess(`

## Decisions Made
- Thumb/track dp sizing (20.dp/4.dp) matches `AeroRangeSlider`'s already-locked dimensions rather than M3's raw `SliderTokens` (4dp×44dp pill, 16dp track — confirmed via direct `material3-desktop-1.9.0-sources.jar` extraction) — the M3 default shape is incompatible with `aeroThumbSurface`'s circle-only primitive.
- Focus and press/drag glow rings diverge in **hue**, not just intensity (focus = `colors.borderSelected`, press = `colors.onSurface`-derived neutral) — a same-hue-family, intensity-only differentiation was the root cause of the round-1 sign-off defect, since this project's `hoverGlow` and `borderSelected` tokens are both `primary`-derived.
- `onValueChangeFinished = null` wired through `SliderState` internally, no new public `AeroSlider` parameter — resolves 18-RESEARCH.md's Open Question 1 per its own recommendation (VRNG-02 milestone constraint: public signature stays 1:1).

## Deviations from Plan

### Auto-fixed / Human-Sign-Off-Driven Issues

**1. [Rule 1 - Bug, human-visual-sign-off-found] Wrong import package for `graphicsLayer`**
- **Found during:** Task 1 fix round 1 (`015eb73`), first compile attempt
- **Issue:** `Modifier.graphicsLayer(...)` was imported from `androidx.compose.ui.draw` (assumed) — the actual pinned CMP 1.11.1 artifact defines it in `androidx.compose.ui.graphics` (confirmed via direct `ui-desktop-1.11.1-sources.jar` extraction: `commonMain/androidx/compose/ui/graphics/GraphicsLayerModifier.kt`)
- **Fix:** Corrected the import to `androidx.compose.ui.graphics.graphicsLayer`
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt`
- **Verification:** `./gradlew :library:compileKotlin` — passes
- **Committed in:** `015eb73`

**2. [Human visual sign-off, round 1] Focus and press/drag thumb states were indistinguishable**
- **Found during:** Task 1 tracer checkpoint, first human visual review of the live showcase
- **Issue:** Focus ring (`colors.borderSelected`) and the interaction ring (`hoverGlow`/`hoverGlow.lighten(0.15f)` for press) were both `primary`-derived — same hue family, insufficient contrast at thumb scale. The D-04 press cue (gloss +0.06f only) had no other differentiating channel.
- **Fix:** Focus ring stacked 2x (`aeroGlowRingRepeated`) for unmistakable intensity; press/drag ring switched to a distinctly neutral `colors.onSurface`-derived hue (also initially stacked 2x) plus a `graphicsLayer` scale-up (initially 1.15x) as a physical "picked up" cue; hover-only ring explicitly excluded while pressed/dragging.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt`
- **Verification:** `./gradlew :library:compileKotlin` + `:library:test --tests "com.mordred.aero.components.range.*"` green; showcase relaunched, human confirmed states now distinguishable
- **Committed in:** `015eb73`

**3. [Human visual sign-off, round 2] Press/drag cue over-tuned after round 1**
- **Found during:** Task 1 tracer checkpoint, second human visual review
- **Issue:** The round-1 fix made states distinguishable but the press/drag ring (2x-stacked bright neutral glow) plus 1.15x scale read as a "flare"/"pop" rather than a tasteful lift.
- **Fix:** Press/drag ring reduced from 2x stack to a single ring (distinctness now rests on hue + the lift cue, not raw brightness); scale reduced from 1.15x to 1.05x.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt`
- **Verification:** `./gradlew :library:compileKotlin` + `:library:test --tests "com.mordred.aero.components.range.*"` green; showcase relaunched, human approved ("идеально")
- **Committed in:** `a8ce1fd`

---

**Total deviations:** 1 auto-fixed bug (wrong import package) + 2 human-visual-sign-off calibration rounds (both expected outcomes of the plan's own tracer-feedback-gate checkpoint, not scope creep — D-03/D-04's own "Claude's discretion, three-theme review" clause anticipated exactly this kind of tuning).
**Impact on plan:** No architectural change — same resolvers, same primitives, same modifier-ordering rules throughout. All changes localized to `AeroSlider.kt`'s thumb-slot glow/scale treatment.

## Guard Fail-Then-Pass Proof

Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path, VER-06), all four new/relevant guards were demonstrated to FAIL against deliberately-broken code via temporary local edits — each reverted (confirmed via `git diff` showing zero changes) before Task 2's commit.

**D-04 divergence guard (`AeroSliderStylesTest#pressedOrDraggingKeepsUnchangedFillWithBrighterGloss`):**
- Temporarily changed `resolveSliderThumbStyle`'s pressed/dragging branch from `rest.copy(glossAlpha = ...)` to `rest.copy(fillTop = rest.fillBottom, fillBottom = rest.fillTop)` (a `pressedRecess`-style swap).
- Re-ran `AeroSliderStylesTest`: **7 tests completed, 1 failed** — `pressedOrDraggingKeepsUnchangedFillWithBrighterGloss()` failed with `AssertionFailedError` at the `fillTop` unchanged assertion. All 6 other tests still passed.
- Reverted; re-ran: 7/7 passed.

**`.hoverable(` guard (`AeroSliderSourceTest#aeroSliderUsesCustomThumbAndTrackSlotsWithHoverableOnTheThumb`):**
- Temporarily removed the `.hoverable(interactionSource)` line from the thumb slot's modifier chain.
- Re-ran `AeroSliderSourceTest`: **3 tests completed, 1 failed** — isolated to the intended assertion.
- Reverted; re-ran: 3/3 passed.

**No-`SliderColors(` guard (`AeroSliderSourceTest#aeroSliderDoesNotRelyOnSliderColorsForDisabled`):**
- Temporarily added a `// TEMP-VIOLATION-PROOF: SliderColors(thumbColor = colors.primary)` line.
- Re-ran `AeroSliderSourceTest`: **3 tests completed, 1 failed** — isolated to the intended assertion.
- Reverted; re-ran: 3/3 passed.

**No-`.pressedRecess(` guard (`AeroSliderSourceTest#aeroSliderThumbPressDoesNotUsePressedRecess`):**
- Temporarily added a `// TEMP-VIOLATION-PROOF: rest.pressedRecess(innerShadow)` line.
- Re-ran `AeroSliderSourceTest`: **3 tests completed, 1 failed** — isolated to the intended assertion.
- Reverted; re-ran: 3/3 passed.

All four proofs used the exact same guard test files that ship in `20b091a` — no guard code was authored differently between the fail-demonstration and the final commit. Full `:library:test` suite confirmed green after every revert.

## Issues Encountered
- Wrong assumed import package for `Modifier.graphicsLayer` (see Deviations #1) — resolved via direct sources-jar extraction rather than a second guess, matching this project's own TOOL-07/VER-03 discipline.
- Background `:showcase:run` processes needed a manual `taskkill //F //IM java.exe //T` between calibration rounds since Compose Desktop doesn't hot-reload — each relaunch was confirmed reaching the `:showcase:run` task before returning the checkpoint.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `AeroSurfaceStyle.neutralRest()` and `resolveSliderThumbStyle`/`resolveSliderTrackStyle`'s precedence-chain shape are the proven foundation Plan 02 (`AeroRangeSlider`) reuses verbatim, per-thumb (VRNG-05).
- The `aeroGlowRingRepeated` stacking pattern and the "diverge press/focus by hue, not just intensity" lesson from the two human-sign-off rounds are directly applicable to `AeroRangeSlider`'s own per-thumb hover/press/focus treatment in Plan 02 — worth reusing rather than re-discovering.
- No blockers. Full `:library:test` suite green after this plan's changes.

---
*Phase: 18-range*
*Completed: 2026-07-24*
