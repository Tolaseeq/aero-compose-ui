---
phase: 19-selectors-lists
plan: 03
subsystem: ui
tags: [compose-desktop, kotlin, aero-primitives, selection, segmented-control, hover, focus, keyboard]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "Modifier.aeroSurface (PRIM-05), AeroSurfaceStyle rest()/hoverLighten()/flattenDisabled()"
  - phase: 17-buttons
    provides: "AeroSurfaceStyle.pressedRecess(innerShadow) transform and PRESSED_INNER_SHADOW value, resolveButtonStyle's disabled-wins precedence-chain shape"
  - phase: 19-01
    provides: "Fail-then-pass source-guard discipline (VER-06), rememberAeroInteractionState wiring pattern"
  - phase: 19-02
    provides: "AeroSwitch's disabled-real-pointer-click test pattern (performMouseInput moveTo/press/release), reused here for the disabled segmented control"
provides:
  - "resolveSegmentStyle — pure per-segment resolver composing the imported pressedRecess/PRESSED_INNER_SHADOW for the recessed selected segment"
  - "AeroSegmentedControl's first-ever hover/press/focus interaction states and real Role.RadioButton semantics"
  - "PRESSED_INNER_SHADOW widened internal — cross-package reuse contract now enforced by a source guard"
affects: [19-04-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "effectiveProgress = if (pressed) 1f else selectedProgress inside resolveSegmentStyle — press snaps depth to full recess instantly (D-07) without waiting on the 150ms selection tween, while the fill-stop lerp still rides selectedProgress for the animated selection transition"
    - "In-bounds focus stroke (drawBehind, inset by half stroke width) chained AFTER aeroSurface so it paints over the fill but stays inside aeroSurface's own clip — no aeroGlowRing on segments (D-10)"
    - "Disabled selectable exposes no RequestFocus semantics action — proven empirically; disabled-path semantics tests must drive a real pointer gesture (performMouseInput moveTo/press/release), not requestFocus+Space"
    - "Fail-then-pass source-scan guard discipline (VER-06), copied from AeroButtonSurfaceSourceTest"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt

key-decisions:
  - "PRESSED_INNER_SHADOW widened private -> internal in AeroButtonSurface.kt, value unchanged, one KDoc line recording the cross-package reuse contract (VSEL-03)"
  - "resolveSegmentStyle folds `pressed` into an effectiveProgress (1f when pressed, else selectedProgress) rather than lerping the raw animated selectedProgress while pressed — required so 'pressed while unselected equals the fully recessed style' holds exactly, matching Task 2's <behavior> spec verbatim; the fill-stop lerp still targets selectedProgress alone when not pressed, preserving D-07's 150ms-animated-selection / instant-interaction split"
  - "Selected segment's label switches to colors.surface (not colors.primary) — colors.primary text over a primary-derived recessed fill would be illegible; unselected labels stay colors.onSurface, unchanged"
  - "Outer Row keeps its .border(1.dp, colors.borderDefault, shape)/.clip(shape) frame unchanged (P-08); only the 1.dp inter-segment separator Box is dropped"
  - "Disabled segmented-control semantics test drives a real pointer gesture (performMouseInput) instead of requestFocus()+Space — empirically a disabled selectable node has no RequestFocus action, mirroring AeroSwitchSemanticsTest's 19-02 precedent"

requirements-completed: [VSEL-03, VSEL-04, VLST-04]

coverage:
  - id: D1
    description: "The selected segment is recessed via the imported pressedRecess transform + imported PRESSED_INNER_SHADOW constant (never a local Shadow literal); unselected segments resolve the same rest(colors, 4.dp) base resolveButtonStyle starts from"
    requirement: "VSEL-03"
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest#sourceImportsAndCallsPressedRecessCrossPackage / sourceImportsPressedInnerShadowAndDoesNotRedeclareAShadowLiteral"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlStylesTest#selectedEqualsIndependentlyReconstructedPressedRecess (exact value equality against a from-scratch reconstruction)"
        status: pass
    human_judgment: true
    rationale: "Whether the recess reads as depth on all three themes is a visual judgement routed to the three-theme sign-off in Plan 04, per the plan's flagged VSEL-03 assumption"
  - id: D2
    description: "Every segment is individually focusable and activatable via Modifier.selectable(role = Role.RadioButton, interactionSource, indication = null), replacing the bare clickable with no role/semantics; Tab reaches each segment and Space/Enter selects it; hover and focus render entirely in-bounds (hoverLighten + inset drawRoundRect stroke), no aeroGlowRing"
    requirement: "VSEL-04"
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest#sourceUsesSelectableRoleRadioButtonNotBareClickable / sourceDoesNotUseAeroGlowRingOnSegments / sourcePassesIndicationNull"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSemanticsTest#everySegmentHasRoleRadioButtonAndExactlyOneReportsSelected / everySegmentIsIndependentlyFocusableAndActivatesViaSpace / ...ViaEnter"
        status: pass
    human_judgment: true
    rationale: "Whether the in-bounds hover and focus cues stay distinguishable from each other when co-active is a visual judgement, recorded as a prohibition and routed to the Plan 04 sign-off"
  - id: D3
    description: "Each segment collects hover through Modifier.hoverable(its own interactionSource) chained alongside its selectable, never through pointer-position tracking"
    requirement: "VLST-04"
    verification:
      - kind: unit
        ref: "AeroSegmentedControlSourceTest#sourceCollectsHoverViaHoverableNotPointerTracking"
        status: pass
    human_judgment: false
  - id: D4
    description: "resolveSegmentStyle's disabled-wins precedence: enabled = false with any combination of selectedProgress/hovered/pressed equals the corresponding depth-resolved style with flattenDisabled(colors) applied, and no hover brightening is present"
    verification:
      - kind: unit
        ref: "AeroSegmentedControlStylesTest#disabledWinsRegardlessOfSelectedProgressOrHoveredOrPressed"
        status: pass
      - kind: unit
        ref: "AeroSegmentedControlSemanticsTest#disabledControlNeverInvokesOnSelect"
        status: pass
    human_judgment: false

duration: 21min (669980a 16:24 -> 47b826c 16:36, UTC+3, per commit timestamps)
completed: 2026-07-27
status: complete
---

# Phase 19 Plan 03: AeroSegmentedControl Aero Restyle Summary

**AeroSegmentedControl's flat `primary@0.3f`-tinted selected segment is now recessed by the literal Phase 17 pressed-button code path (imported `pressedRecess` + imported `PRESSED_INNER_SHADOW`, both widened/reused cross-package), unselected segments are raised glass, every segment is a real `Role.RadioButton` focus stop with in-bounds hover/focus, and the 1.dp separator is gone — all proven fail-then-pass and value-exact against an independently reconstructed pressed-button style.**

## Performance

- **Duration:** 21 min (commit `669980a` at 16:24 -> commit `47b826c` at 16:36, UTC+3).
- **Tasks:** 2/2 completed.
- **Files modified:** 5 (2 main source, 3 test files).

## Accomplishments

- Widened `PRESSED_INNER_SHADOW` from `private` to `internal` in `AeroButtonSurface.kt` — value (`radius = 2.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp)`) unchanged, one KDoc line added recording the cross-package reuse contract (VSEL-03).
- `resolveSegmentStyle(colors, selectedProgress, hovered, pressed, enabled)`: builds `base = AeroSurfaceStyle.rest(colors, 4.dp)` and `recessed = base.pressedRecess(PRESSED_INNER_SHADOW)` (the imported transform and imported constant, never a local copy); folds `pressed` into `effectiveProgress` (1f when pressed, else `selectedProgress`) so an unselected-but-pressed segment reaches full recess immediately rather than riding the selection animation; lerps `fillTop`/`fillBottom` across `effectiveProgress` while gloss/bevel/rim/innerShadow switch instantly with whichever base `effectiveProgress` currently favors (P-05); disabled short-circuits to `.flattenDisabled(colors)` before hover is considered.
- Restyled `AeroSegmentedControl.kt`: per-segment `Modifier.selectable(selected, enabled, role = Role.RadioButton, interactionSource = segSource, indication = null, onClick = { onSelect(opt) })` replaces the bare `.clickable(enabled) { onSelect(opt) }` with no role/semantics; each segment holds its own remembered `MutableInteractionSource` chained with `.hoverable(segSource)` alongside `.selectable(...)` (VLST-04, Pitfall 6 — selectable does not report hover for free).
- In-bounds focus cue: an inset `drawBehind { drawRoundRect(...) }` stroke at `colors.borderSelected.copy(alpha = 0.8f)`, chained AFTER `.aeroSurface(...)` so it paints over the fill but stays inside the segment's own clip — no `aeroGlowRing` on individual segments (D-10: the segments sit flush inside the outer `Row`'s clip, so an outer bloom would be sliced by the frame and bleed onto neighbours).
- Selected-segment label color switches to `colors.surface` (was `colors.primary`, now illegible over a primary-derived recessed fill); unselected labels stay `colors.onSurface`.
- Dropped the shipped 1.dp `borderDefault@0.5f` separator `Box` between segments (D-08/D-10) — each segment's own raised or recessed bevel/rim contour now supplies the visual break.
- Outer `Row` keeps its `.height(28.dp)`, `.border(1.dp, colors.borderDefault, shape)`, `.clip(shape)` unchanged (P-08, VER-03 no size creep); gained `.hoverable(interactionSource)` for the additive trailing `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` parameter (D-04); lost the shipped `.alpha(if (enabled) 1f else 0.4f)`.
- Removed the `Color.Transparent` anti-pattern that was in the shipped `bgColor` animation target (PRIM-14).
- `AeroSegmentedControlSourceTest` — 9 source-scan guards (pressedRecess import/call, PRESSED_INNER_SHADOW reference + no local `Shadow(` literal, resolveSegmentStyle/aeroSurface delegation, selectable+Role.RadioButton present/bare-clickable absent, no aeroGlowRing, hoverable present/no pointer-tracking, `indication = null` present, no `Color.Transparent`, dropped-separator absent), proven RED against the shipped file (8/9 failed), then GREEN after the restyle (VER-06).
- `AeroSegmentedControlStylesTest` — full raised/recessed/hover/press/disabled matrix across `AeroBlue`/`Classic`, including the VSEL-03 anti-drift assertion (selected == independently reconstructed `rest(colors, 4.dp).pressedRecess(PRESSED_INNER_SHADOW)`, exact value equality), the exact-midpoint fill-stop lerp, press-reaches-full-depth-immediately, press-on-selected-is-a-no-op, hover-composes-not-replaces, and disabled-wins-over-everything.
- `AeroSegmentedControlSemanticsTest` — `Role.RadioButton` + exactly-one-selected across all three segments (not sampled), Space and Enter activation proven per segment in two dedicated tests, and a disabled-control-never-invokes-onSelect test using a real pointer gesture (a disabled selectable node exposes no `RequestFocus` semantics action, discovered empirically — mirrors `AeroSwitchSemanticsTest`'s 19-02 precedent).

## Task Commits

1. **Task 1: widen PRESSED_INNER_SHADOW to internal, prove AeroSegmentedControlSourceTest RED, then the raised/recessed segment restyle with selectable semantics and in-bounds hover/focus, proven GREEN** - `669980a` (feat)
2. **Task 2: AeroSegmentedControlStylesTest (VSEL-03 value-equality) and AeroSegmentedControlSemanticsTest (per-segment Role.RadioButton + Tab + Space/Enter)** - `47b826c` (test, tdd)

**Plan metadata:** commit to follow this SUMMARY (docs: complete plan)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - `PRESSED_INNER_SHADOW` visibility `private` -> `internal`, value unchanged, KDoc line added
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` - Restyled: `resolveSegmentStyle` resolver, raised/recessed per-segment `aeroSurface` painting, `selectable(role = Role.RadioButton)` + `hoverable` per segment, in-bounds focus stroke, `interactionSource` parameter, separator dropped, `Color.Transparent` removed
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` - 9 source-scan regression guards, fail-then-pass proven (Task 1)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` - Full value-level state matrix for `resolveSegmentStyle` across `AeroBlue`/`Classic` (Task 2)
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt` - `Role.RadioButton` + keyboard activation proven per segment, disabled-suppression via real pointer gesture (Task 2)

## Guard Fail-Then-Pass Proof (VER-06)

Run against the shipped, un-restyled `AeroSegmentedControl.kt` (before Task 1's restyle edit), 8 of 9 `AeroSegmentedControlSourceTest` guards failed (RED):

- `sourceImportsAndCallsPressedRecessCrossPackage` — RED (shipped file used a flat `primary.copy(alpha = 0.3f)` fill, no `pressedRecess` import or call)
- `sourceImportsPressedInnerShadowAndDoesNotRedeclareAShadowLiteral` — RED (no `PRESSED_INNER_SHADOW` reference at all)
- `sourceDelegatesToResolveSegmentStyleAndAeroSurface` — RED (no resolver, no `aeroSurface(` call)
- `sourceUsesSelectableRoleRadioButtonNotBareClickable` — RED (bare `.clickable(enabled) { onSelect(opt) }`, no role)
- `sourceCollectsHoverViaHoverableNotPointerTracking` — RED (no `.hoverable(` at all — shipped file had zero hover/focus states)
- `sourcePassesIndicationNull` — RED (no `selectable(`/`indication` argument existed yet)
- `sourceDoesNotUseFullyTransparentColorConstant` — RED (shipped `bgColor` animation targeted `Color.Transparent` directly)
- `sourceDoesNotDrawTheDroppedSeparatorBox` — RED (shipped file drew the 1.dp `borderDefault.copy(alpha = 0.5f)` separator `Box`)

1 of 9 guards passed even against the shipped file (not a regression, already-correct baseline):

- `sourceDoesNotUseAeroGlowRingOnSegments` — GREEN (shipped file never called `aeroGlowRing(` — it had no hover/focus states at all, so the "no outer bloom" guard was vacuously satisfied)

After the restyle: all 9 guards GREEN — `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSegmentedControlSourceTest"` exits 0.

This session additionally ran, all green:

- `./gradlew :library:compileKotlin` — exit 0 (after Step 1 and again after Step 3)
- `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSegmentedControlSourceTest" --tests "com.mordred.aero.components.buttons.*"` — exit 0 (visibility widening did not disturb the shipped button suite)
- `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSegmentedControlStylesTest" --tests "com.mordred.aero.components.selection.AeroSegmentedControlSemanticsTest"` — exit 0
- `./gradlew :library:test --tests "com.mordred.aero.components.selection.AeroSegmentedControl*" --tests "com.mordred.aero.components.buttons.*"` — exit 0
- `./gradlew :library:test` (full suite, no-regression check) — exit 0

## Decisions Made

- `resolveSegmentStyle` folds `pressed` into an `effectiveProgress` (1f when pressed, else the raw `selectedProgress`) rather than lerping `selectedProgress` unconditionally while pressed — this is a small, deliberate refinement of the plan's literal prose, required to satisfy Task 2's explicit `<behavior>` line "Pressed while unselected: equals the recessed style" exactly. Without folding, a segment pressed at `selectedProgress = 0f` would resolve a hybrid style (base fill stops with recessed bevel/gloss), not the fully recessed style the acceptance test requires. Verified this holds for all six styles-test scenarios (unselected/selected/midpoint/pressed-unselected/pressed-selected/hover-compose) plus disabled-wins.
- Selected segment's label color set to `colors.surface` per the UI-SPEC's resolved legibility decision (not `colors.primary`, which would be illegible over the recessed accent fill).
- Disabled-control semantics test uses a real pointer gesture (`performMouseInput { moveTo(center); press(); release() }`) rather than `requestFocus()` + Space — a disabled `selectable` node was empirically found to expose no `RequestFocus` semantics action (see "Issues Encountered" below), mirroring the `AeroSwitchSemanticsTest` 19-02 precedent for the same reason.
- Outer `Row`'s `.border(1.dp, colors.borderDefault, shape)`/`.clip(shape)` frame kept unchanged (P-08) — only the inter-segment separator was removed.

## Deviations from Plan

**1. [Rule 1 - Bug, caught during test-writing, not a runtime bug] `resolveSegmentStyle` refined to fold `pressed` into an effective progress value**
- **Found during:** Task 2, while writing the `pressedWhileUnselectedEqualsRecessedStyle` styles test against Task 1's implementation.
- **Issue:** The plan's literal `<action>` prose specifies `lerp(base.fillTop, recessed.fillTop, selectedProgress)` using the raw `selectedProgress` parameter. Task 1's initial implementation (written directly from that prose) would resolve a segment that is `pressed = true` but `selectedProgress = 0f` (unselected) to a hybrid style — recessed bevel/gloss/innerShadow (since `target` already accounted for `pressed` in the `if (effectiveProgress >= 0.5f...)`-style condition) but base (unrecessed) fill stops, because the fill lerp itself still used the raw `selectedProgress = 0f`. Task 2's `<behavior>` spec explicitly requires this case to equal the FULLY recessed style, which the hybrid result would not satisfy.
- **Fix:** Introduced `effectiveProgress = if (pressed) 1f else selectedProgress` and used it consistently for both the `target` selection and the fill-stop lerp, so a pressed-unselected segment resolves to the fully recessed style in one step, matching Task 2's acceptance criteria exactly while still preserving the 150ms-animated selection transition (D-07) when not pressed.
- **Files modified:** `AeroSegmentedControl.kt` (implemented directly during Task 1, before commit — no separate fix-up commit needed since Task 1 was written with this design from the start after working through the styles-test behavior spec first).
- **Commit:** `669980a` (part of the original Task 1 commit, not a follow-up).

### Auth Gates

None encountered — this plan involves no network/auth-gated tooling.

## Issues Encountered

- A disabled `Modifier.selectable(...)` node's semantics tree does not expose a `RequestFocus` action (confirmed by an actual test failure showing `Actions = [ClearTextSubstitution, GetTextLayoutResult, OnClick, SetTextSubstitution, ShowTextSubstitution]` with no `RequestFocus`, and `[Disabled]` on the node). The originally-written `disabledControlNeverInvokesOnSelect` test called `requestFocus()` + `performKeyInput { pressKey(Key.Spacebar) }`, which threw before the assertion could even run. Fixed by switching to a real pointer gesture (`performMouseInput { moveTo(center); press(); release() }`) on the target node, mirroring `AeroSwitchSemanticsTest`'s 19-02 precedent for the identical underlying constraint (a disabled interactive node cannot be driven via keyboard-focus-dependent test APIs). No blocker — resolved within the same session before commit.

No other blockers.

## Known Stubs

None found. `AeroSegmentedControl.kt` is a complete render-path restyle — the resolver is fully wired into the composable, every state (rest/hover/press/focus/selected/disabled) resolves through real code, no hardcoded empty values or placeholder text.

## Next Phase Readiness

- `resolveSegmentStyle` and the in-bounds focus-stroke pattern are proven end-to-end and ready as the third reference point (alongside Plan 01's `AeroListItem` and Plan 02's `AeroSwitch`) for Plan 04's formal three-theme sign-off.
- Visual/legibility verification of the recessed-segment color, hover-vs-focus glow distinguishability, and the two flagged edge-probe assumptions (VSEL-03's "does the recess read as depth" and VSEL-04's "do hover/focus stay distinguishable") are deferred to Plan 04's sign-off, per the plan's flagged assumptions.
- The two E2 overflow/long-text backstops (deliberately long segment label vs. the now-separator-less segment break) are also deferred to Plan 04's sign-off per the plan's `must_haves.truths` backstop entries.
- No blockers.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27*

## Self-Check: PASSED

All created/modified files found on disk; both task commits (`669980a`, `47b826c`) found in git history.
