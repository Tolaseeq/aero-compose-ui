---
phase: 19-selectors-lists
plan: 02
subsystem: ui
tags: [compose-desktop, kotlin, aero-primitives, selection, switch, hover, focus, keyboard]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "Modifier.aeroGroove (PRIM-08), Modifier.aeroThumbSurface (PRIM-07), Modifier.aeroGlowRing (PRIM-06), AeroSurfaceStyle rest()/neutralRest(), hoverLighten()/flattenDisabled() transforms"
  - phase: 18-range
    provides: "resolveSliderThumbStyle precedence-chain model this plan's resolveSwitchThumbStyle mirrors"
  - phase: 19-01
    provides: "Fail-then-pass source-guard discipline (VER-06) and rememberAeroInteractionState wiring pattern, proven on AeroListItem"
provides:
  - "resolveSwitchGrooveStyle / resolveSwitchThumbStyle — pure resolvers for AeroSwitch's recessed groove and raised neutral thumb"
  - "AeroSwitch's first-ever hover / press / focus / disabled interaction states"
  - "Proven D-05 divergence: pressed thumb brightens gloss instead of recessing (unlike the button precedent)"
affects: [19-03-selectors-lists, 19-04-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sibling-layering: thumb Box declared after and outside the groove Box's clip, so its drop shadow/glow escape the 18dp track (D-03)"
    - "Single animateFloatAsState value drives both thumb offset and groove fill lerp — exactly one moving part (D-02/D-07/P-03)"
    - "Both aeroGlowRing calls chained before any clip-applying primitive (aeroGroove/aeroThumbSurface) on the same modifier chain"
    - "Fail-then-pass source-scan guard discipline (VER-06), copied from AeroSliderSourceTest/AeroListItemSourceTest"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt

key-decisions:
  - "D-01/D-02 implemented exactly as specified: resolveSwitchGrooveStyle builds neutral+accent rest styles at the same 9.dp corner radius and lerps only fillTop/fillBottom by checkedProgress — gloss/bevel/rim/cornerRadius come from the neutral style in both states, verified byte-for-byte in AeroSwitchStylesTest"
  - "D-05 divergence implemented and asserted explicitly: pressed thumb keeps fillTop/fillBottom UNCHANGED from rest (not swapped, as pressedRecess would) and glossAlpha strictly greater — proven in the same test method so a future recess-transform regression fails loudly"
  - "D-03 sibling layering: thumb Box is declared after the groove Box, same parent, no shared clip — its THUMB_DROP_SHADOW and hover glow can visually exceed the 36x18dp track"
  - "D-04 additive interactionSource: MutableInteractionSource = remember { MutableInteractionSource() } trailing parameter — source-compatible, existing call sites compile unchanged"
  - "P-01 indication = null on toggleable, matching AeroButtonSurface's precedent — the custom groove/thumb hover/press cues replace platform indication"

requirements-completed: [VSEL-01, VSEL-02, VLST-04]

coverage:
  - id: D1
    description: "AeroSwitch's track renders through Modifier.aeroGroove and its thumb through Modifier.aeroThumbSurface, replacing the two flat Boxes; off-state groove uses identical geometry to the checked groove, only fill stops lerp by checkedProgress"
    requirement: "VSEL-01"
    verification:
      - kind: unit
        ref: "AeroSwitchSourceTest#switchUsesAeroGrooveAndAeroThumbSurface (and companion source guards)"
        status: pass
      - kind: unit
        ref: "AeroSwitchStylesTest#grooveUncheckedEnabledEqualsNeutralRest / grooveCheckedEnabledMovesOnlyTheFillStops / grooveMidpointLerpsEachFillStopExactly"
        status: pass
    human_judgment: true
    rationale: "Visual gloss/bevel/recess rendering quality is a three-theme sign-off concern deferred to Plan 04, not verifiable from unit tests alone"
  - id: D2
    description: "Hover puts an aeroGlowRing on the whole track plus hoverLighten() on the thumb; focus puts a persistent aeroGlowRing on the same track using the distinct colors.borderSelected token; press keeps the thumb raised with a brighter gloss rather than the button's inverted recess"
    requirement: "VSEL-02"
    verification:
      - kind: unit
        ref: "AeroSwitchStylesTest#thumbPressedKeepsUnchangedFillWithStrictlyBrighterGloss / thumbHoveredEqualsRestWithHoverLightenApplied"
        status: pass
      - kind: unit
        ref: "AeroSwitchSourceTest (aeroGlowRing-before-clip ordering guard)"
        status: pass
    human_judgment: true
    rationale: "Hover-vs-focus visual distinguishability across three themes is tracked as a prohibition plus a Plan 04 sign-off item (19-UI-SPEC.md backstop discipline)"
  - id: D3
    description: "AeroSwitch collects hover through Modifier.hoverable(interactionSource) chained alongside toggleable, never through pointer-position tracking"
    requirement: "VLST-04"
    verification:
      - kind: unit
        ref: "AeroSwitchSourceTest (hoverable-present / no-pointerInput guard)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Role.Switch semantics and Space-key keyboard activation survive the restyle; a disabled switch never forwards a real pointer click to onCheckedChange"
    verification:
      - kind: unit
        ref: "AeroSwitchSemanticsTest#aeroSwitchHasRoleSwitchAndToggleAction / aeroSwitchSpaceKeyFlipsCheckedExactlyOnceAfterFocus / aeroSwitchDisabledNeverInvokesOnCheckedChangeOnRealPointerClick"
        status: pass
    human_judgment: false

duration: 42min (a9a966f 15:22 -> d47ef97 16:04, UTC+3, per commit timestamps; this session's own active work was the Task 2 completion only — see Issues Encountered)
completed: 2026-07-27
status: complete
---

# Phase 19 Plan 02: AeroSwitch Aero Restyle Summary

**AeroSwitch's two flat Boxes are now a recessed accent-lerped groove (`Modifier.aeroGroove`) carrying a raised, glossy, shadowed neutral thumb (`Modifier.aeroThumbSurface`), with its first-ever hover/press/focus/disabled states wired via `resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle`, mirroring `resolveSliderThumbStyle`'s proven precedence chain and asserting D-05's "raised, not recessed" press divergence at value level.**

## Performance

- **Duration:** 42 min total plan span (Task 1 commit `a9a966f` at 15:22 -> Task 2 commit `d47ef97` at 16:04, UTC+3). This session's own active work was completing and committing Task 2 only (~10 min) — Task 1 was already committed on `master` when this session began.
- **Tasks:** 2/2 completed
- **Files modified:** 4 (1 main source, 3 test files)

## Accomplishments

- Replaced `AeroSwitch`'s two flat `Box`es (`.background(trackColor, shape)` track, `.background(colors.surface, RoundedCornerShape(50))` thumb) with `resolveSwitchGrooveStyle` painted via `Modifier.aeroGroove` and `resolveSwitchThumbStyle` painted via `Modifier.aeroThumbSurface` (VSEL-01).
- `resolveSwitchGrooveStyle`: builds `neutralRest`/`rest` at the same 9.dp corner radius, lerps only `fillTop`/`fillBottom` by `checkedProgress` — gloss/bevel/rim/cornerRadius come from the neutral style in both checked and unchecked states (D-02, "one moving part").
- `resolveSwitchThumbStyle`: mirrors `resolveSliderThumbStyle`'s precedence (disabled > pressed > hovered > rest) minus the `isDragging` axis. Pressed brightens `glossAlpha` by `PRESSED_GLOSS_BOOST` (0.10f) on the unmodified fill — never the button's `pressedRecess` transform (D-05). Disabled short-circuits before `THUMB_DROP_SHADOW` is attached, so a dead thumb never floats.
- Wired `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` as an additive trailing parameter (D-04); collected via `rememberAeroInteractionState` + `Modifier.hoverable(interactionSource)` (VLST-04, no raw pointer tracking).
- Two `aeroGlowRing` calls on the outer track `Box`, both chained before the groove/thumb's clip-applying primitives: focus uses `colors.borderSelected`, hover uses `AeroOrnamentTokens.derive(colors).hoverGlow` — distinct tokens so hover and focus stay tellable apart when co-active (D-06).
- Thumb `Box` kept as a sibling of the groove `Box` (same parent, declared after it), never nested inside the groove's clip, so `THUMB_DROP_SHADOW` and the hover-brightened gloss can visually exceed the 36x18dp track (D-03).
- Deleted the shipped `trackColor` `animateColorAsState`; the single `thumbProgress` `animateFloatAsState` (150ms `LinearEasing` tween, unchanged) now drives both the thumb offset and the groove's fill lerp (D-07/P-03).
- Layout geometry byte-identical to shipped values: `36.dp` x `18.dp` track, `14.dp` thumb, offset `(2.dp + 18.dp * thumbProgress)`, `RoundedCornerShape(50)` (VER-03).
- `AeroSwitchSourceTest` — 7 source-scan guards (aeroGroove/aeroThumbSurface calls, aeroGlowRing call, resolver delegation, no button pressed-recess transform, hoverable-not-pointerInput, `indication = null`, no `Color.Transparent`), proven RED against the shipped file, then GREEN after the restyle (VER-06).
- `AeroSwitchStylesTest` — full value-level matrix for both resolvers (unchecked/checked/midpoint-lerp/disabled groove; rest/pressed/hovered/disabled-wins thumb), swept across `AeroBlue` and `Classic`, expectations reconstructed independently from `AeroSurfaceStyle.neutralRest`/`rest` rather than the resolvers under test.
- `AeroSwitchSemanticsTest` (this session) — proves `Role.Switch` + toggle action survive the restyle, Space-after-focus flips `checked` exactly once through `onCheckedChange`, and a disabled switch never forwards a real pointer click (`moveTo`/`press`/`release` via `performMouseInput`) to `onCheckedChange`.

## Task Commits

1. **Task 1: AeroSwitchSourceTest proven RED first, then the groove + raised-thumb restyle with hover / press / focus / disabled, proven GREEN** - `a9a966f` (feat) — already present on `master` when this session began; verified against the plan's full spec (resolvers, modifier chain ordering, layout constants, KDoc hygiene) with no gaps found.
2. **Task 2: AeroSwitchStylesTest (resolver value matrix) and AeroSwitchSemanticsTest (Role.Switch + keyboard survives the restyle)** - `d47ef97` (test, tdd) — `AeroSwitchStylesTest.kt` found already written and correct on disk but never committed/run; `AeroSwitchSemanticsTest.kt` did not exist and was written this session. Both compiled, ran green, and were committed together.

**Plan metadata:** commit to follow this SUMMARY (docs: complete plan)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` - Restyled: `resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle` resolvers, groove+thumb sibling Boxes via `aeroGroove`/`aeroThumbSurface`, two `aeroGlowRing` calls (focus/hover), `hoverable`/`indication = null`, additive `interactionSource` parameter, `trackColor` animation removed
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` - 7 source-scan regression guards, fail-then-pass proven (Task 1, pre-existing this session)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt` - Pure JVM value-level tests of both resolvers' full state matrix across AeroBlue/Classic (found pre-written, uncommitted; committed this session)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt` - Role.Switch + Space-key keyboard activation + disabled-click-suppression proof (written and committed this session)

## Guard Fail-Then-Pass Proof (VER-06)

Per the commit message on `a9a966f` (the only record available — the shipped pre-restyle `AeroSwitch.kt` no longer exists on disk to re-run against in this session):

Against the shipped, un-restyled `AeroSwitch.kt`, 5 of 7 `AeroSwitchSourceTest` guards failed (RED):
- `aeroGroove(`/`aeroThumbSurface(` call guard — RED (shipped file used two flat `.background()` Boxes)
- `aeroGlowRing(` call guard — RED (no glow ring existed pre-restyle)
- `resolveSwitchThumbStyle(`/`resolveSwitchGrooveStyle(` delegation guard — RED (no resolvers existed)
- `.hoverable(` present guard — RED (shipped file had no hover collection at all)
- `indication = null` guard — RED (shipped `toggleable` call had no `indication` argument)

2 of 7 guards passed even against the shipped file (not regressions, already-correct baseline):
- No-button-pressed-recess-transform guard — GREEN (shipped file never called the button's recess transform)
- No `Color.Transparent` guard — GREEN (shipped file never used the fully-transparent color constant)

After the restyle (re-verified this session): all 7 guards GREEN — `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSwitchSourceTest"` exits 0.

This session additionally ran, all green:
- `./gradlew :library:compileKotlin` — exit 0
- `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSwitchStylesTest" --tests "com.mordred.aero.components.selection.AeroSwitchSemanticsTest"` — exit 0
- `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSwitch*"` (all three files together) — exit 0
- `./gradlew :library:test` (full suite, no-regression check) — exit 0

## Decisions Made

- D-01/D-02 groove resolver implemented exactly as specified — no deviation.
- D-05's press-brightens-gloss divergence implemented and explicitly asserted (fill unchanged + strict `glossAlpha` inequality in one test method), so a future accidental recess-transform substitution fails loudly.
- D-03 sibling layering (thumb Box declared after, not nested in, the groove Box) implemented exactly as specified.
- D-04 additive `interactionSource` parameter — source-compatible, no existing call sites broken.
- P-01 `indication = null` applied on `toggleable`, matching `AeroButtonSurface`'s precedent.
- Disabled switch's real-pointer-click suppression tested via an actual `performMouseInput` gesture (`moveTo`/`press`/`release`) rather than a semantics-action assertion, avoiding ambiguity around whether Compose's `toggleable` merges an `OnClick` semantics action when `enabled = false`.

## Deviations from Plan

None from the plan's task specifications. This session's own contribution was scoped exactly to Task 2's stated files (`AeroSwitchStylesTest.kt`, `AeroSwitchSemanticsTest.kt`) — no changes to `AeroSwitch.kt` or `AeroSwitchSourceTest.kt` (Task 1) were needed; both were verified correct against the plan as written.

## Issues Encountered

This execution session found the plan partially executed on disk at start:

1. **Task 1** (`AeroSwitch.kt` restyle + `AeroSwitchSourceTest.kt`) was already committed on `master` as `a9a966f`. This session read and cross-checked it against the plan's full `<action>`/`<acceptance_criteria>` block (resolver signatures, modifier-chain ordering, layout constants, KDoc hygiene, guard coverage) — no gaps found; not redone.
2. **`AeroSwitchStylesTest.kt`** (part of Task 2) existed on disk but was untracked and had never been run. Read and verified against the plan's `<behavior>`/`<action>` spec (independent reconstruction from `AeroSurfaceStyle.neutralRest`/`rest`, no resolver calls in expectations, both-scheme sweep, D-05 divergence assertion) — matched exactly; not rewritten.
3. **`AeroSwitchSemanticsTest.kt`** (part of Task 2) did not exist. Written this session mirroring `AeroButtonSemanticsTest.kt`'s `runComposeUiTest` shape, using `Modifier.testTag` (not `onNodeWithText`, since `AeroSwitch` renders no text) and a real `performMouseInput` gesture for the disabled-click-suppression assertion.
4. Compiled and ran the new/pending test files (initial `performMouseInput { click(center) }` attempt failed to compile — `click` is not a member of this project's `MouseInjectionScope` surface; switched to the `moveTo`/`press`/`release` sequence already used elsewhere in the codebase, e.g. `InteractionStatesTest.kt`), reached green, then ran the full `AeroSwitch*` suite and the full library test suite for the no-regression check — both green.
5. Committed Task 2's two test files atomically, then produced this SUMMARY and completed state/roadmap/requirements bookkeeping.

No blockers.

## Known Stubs

None found. `AeroSwitch.kt` is a complete render-path restyle with both resolvers fully wired into the composable — no hardcoded empty values, placeholder text, or unwired data sources.

## Next Phase Readiness

- `resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle` and the sibling-layering pattern are proven end-to-end and ready as the second reference point (alongside Plan 01's `AeroListItem`) for Plan 03 (`AeroSegmentedControl`).
- Visual/legibility verification of the groove gloss magnitude, thumb shadow, and hover-vs-focus glow distinguishability across all three themes is deferred to Plan 04's formal sign-off, per the plan's flagged assumptions for VSEL-01/VSEL-02 (unclassified edge-probe rows).
- No blockers.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27*

## Self-Check: PASSED

All created/modified files found on disk; both task commits (`a9a966f`, `d47ef97`) found in git history.
