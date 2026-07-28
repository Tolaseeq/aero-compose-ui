---
phase: 19-selectors-lists
reviewed: 2026-07-28T00:00:00Z
depth: standard
files_reviewed: 19
files_reviewed_list:
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
  - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
  - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/common/HoverEmissionTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchFocusVisibleWiringTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
findings:
  critical: 0
  warning: 10
  info: 5
  total: 15
status: issues_found
---

# Phase 19: Code Review Report (re-review after gap-closure round 19-09/19-10/19-11)

**Reviewed:** 2026-07-28
**Depth:** standard
**Files Reviewed:** 19
**Status:** issues_found

## Summary

This is a re-review of the prior `19-REVIEW.md` after plans 19-09, 19-10 and 19-11 claimed to
close CR-01, CR-02, WR-01, WR-03 and WR-04. All five are verified genuinely closed in the current
code and covered by regression tests that were confirmed RED-before/GREEN-after against the
un-fixed source (per each plan's commit message). Details in the "Gap-Closure Verification"
section below.

No new BLOCKER-class defect was introduced by the three gap-closure plans. One new WARNING is
raised: the CR-01 fix clears its own value-level contrast guard by only the barest of margins on
AeroDark (3.296:1, against a **3.0** floor the test itself chose), while the guard's own KDoc calls
this "the 3.0 floor rather than the 4.5 normal-text target" — i.e. the segment label is proven to
clear the *non-text UI component* WCAG floor, not the *normal-text* floor that actually applies to
a text label. This is exactly the class of low-contrast defect the task brief warns a human visual
sign-off tends to miss, so it is called out explicitly even though the value-level test the
developer added is internally consistent and passes.

The remaining ~10 previously-reported WARNING/INFO items (WR-02, WR-05 through WR-11, IN-01 through
IN-05) were **not** in scope for 19-09/19-10/19-11 and are re-verified below as still open against
the current source, so they are not silently dropped by this file being overwritten. None of them
are re-litigated in depth here beyond confirming they still reproduce; see the prior review's
original write-ups (now superseded by this file, but the facts are re-confirmed) for full
detail — they are restated concisely below.

## Structural Findings (fallow)

_No `<structural_findings>` block was supplied for this review._

## Narrative Findings (AI reviewer)

## Gap-Closure Verification (CR-01, CR-02, WR-01, WR-03, WR-04)

### CR-01 — CLOSED
**Claim:** `RAISED_FILL_TOP_DARKEN`/`RAISED_FILL_BOTTOM_DARKEN` applied to `colors.primary` before
`pressedRecess`, plus a WCAG contrast test.

**Verified:** `AeroSegmentedControl.kt:238-241` declares `RAISED_FILL_TOP_DARKEN = 0.45f` /
`RAISED_FILL_BOTTOM_DARKEN = 0.61f` (raised via commit `248814a` from an initial `0.20f/0.36f` that
still failed AeroDark/AeroBlue below the 3.0 floor). `resolveSegmentStyle` (lines 283-286) applies
both to `colors.primary` before `base.pressedRecess(...)` (line 287), so the recessed style inherits
the correction through the fill-stop exchange — both raised and recessed move down together, and
`recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` still passes. A new
`everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio` test (`AeroSegmentedControlStylesTest.kt:297-348`)
value-checks `onSurface` against both `fillTop` and `fillBottom` at both ends of the selection axis,
across AeroBlue/AeroDark/Classic (12 assertions), against a `MIN_LABEL_CONTRAST = 3f` floor. I
independently recomputed the tightest case (AeroDark, unselected, fillTop) by hand from the shipped
RGB literals and the shipped `Color.darken`/relative-luminance formulas and got the same ~3.3:1 the
commit message for `248814a` claims ("tightest margin: AeroDark unselected top = 3.296"). The
label-vanishing defect (1.04:1 pre-fix) is gone. See the new WARNING below re: the chosen threshold.

### CR-02 — CLOSED
**Claim:** `FocusVisibility(hovered = hovered)` on `Unfocus`.

**Verified:** `InteractionStates.kt:129` reads exactly `is FocusInteraction.Unfocus -> FocusVisibility(hovered = hovered)`,
resetting only `focused`/`pointerAcquired` and preserving the pointer-derived `hovered` field.
`FocusVisibilityTest.mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed` (added in
`1ad5414`) folds the exact six-interaction repro from the original CR-02 write-up and asserts
`result.visible == false`; I traced the fold by hand and confirmed it only passes with the fix
applied (with the old `FocusVisibility()` reset it would zero `hovered`, the guard on
`PressInteraction.Press` would not re-arm `pointerAcquired`, and `visible` would wrongly be `true`).
A second, composed end-to-end proof (`AeroSwitchFocusVisibleWiringTest.kt`) drives a real
`AeroSwitch` + real focus move between two nodes with a stationary pointer and asserts
`state.focusVisible == false` after the repro sequence — this is a genuine reachability proof, not
just a reducer-level fold.

### WR-01 — CLOSED
**Claim:** switched to `state.focusVisible && enabled`.

**Verified:** `AeroButtonSurface.kt:104-108`'s FOCUS `aeroGlowRing` call reads
`active = state.focusVisible && enabled`; the HOVER call two lines below (line 109-113) is
untouched and still reads `state.hovered && enabled`, matching the documented intent (hover cue
stays live, focus cue gates on keyboard-acquired focus only). `AeroButtonSurfaceSourceTest.aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag`
asserts both substrings are present. `resolveButtonStyle`'s `focused` parameter is still unused
(acknowledged dead-by-design in its own KDoc) — not a regression, just an existing wart; see IN
items below for the general "keep it tidy" concern, not repeated as a fresh finding here.

### WR-03 — CLOSED
**Claim:** per-segment interaction state keyed on option identity, not slot position.

**Verified:** `AeroSegmentedControl.kt:118-181` wraps the entire per-segment body (interaction
source, focus-visibility state, the selection tween, and the `Box`) in `key(index, opt) { ... }`.
Index-and-value (not value-only) is a deliberate, documented tradeoff given duplicate options remain
unguarded (deferred as WR-07, still open — see below). `sourceKeysEachSegmentOnItsOwnIdentity`
guards the `key(index, opt)` token's presence directly in source.

### WR-04 — CLOSED
**Claim:** removed a redundant second hover emitter on each of the three components.

**Verified:**
- `AeroSwitch.kt`: the explicit `.hoverable(interactionSource)` after `.toggleable(...)` was
  deleted; the `hoverable` import itself was removed (no dead import left behind).
- `AeroSegmentedControl.kt:148-153`: the per-segment `.hoverable(segSource)` was deleted, the outer
  strip's `.hoverable(interactionSource)` (line 105) — the control-level source's only emitter —
  was correctly left in place.
- `AeroListItem.kt:91-112`: the unconditional `.hoverable(interactionSource)` was replaced by a
  `.then(...)` that puts `Modifier.clickable(...)` on the `onClick != null` path (which already
  emits hover) and `Modifier.hoverable(interactionSource, enabled = enabled)` on the
  `onClick == null` display-only path (which has no other interaction modifier and would otherwise
  silently lose hover).

New `HoverEmissionTest` premise tests prove `toggleable`/`selectable`/`clickable` each emit hover on
their own supplied source without an explicit `.hoverable`, plus component-level proofs
(`aeroSwitchStillReportsHoverWithNoExplicitHoverModifier`,
`aeroListItemReportsHoverOnBothTheClickableAndTheDisplayOnlyPath`) drive real pointer input against
the real, post-fix components and confirm hover still works on every path. Source-scan guards in all
three `*SourceTest.kt` files now assert an exact count of one `.hoverable(` call per file (list
item, switch) or two total with one attributable to the outer strip (segmented control), rather than
the old "at least one" check that could not have caught the duplicate. This also closes the
"disabled control still reports itself hovered" half of the original WR-04 — `AeroListItem`'s
retained `.hoverable(...)` now passes `enabled = enabled`, and `AeroSwitch`/`AeroSegmentedControl`'s
sole hover emitters are the enabled-aware `toggleable`/`selectable` modifiers themselves.

## Warnings

### WR-12 (new): CR-01's contrast fix clears a 3.0 floor, not the 4.5 normal-text floor the label actually needs

**File:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt:351-357`,
`library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:238-241`

**Issue:** The new `everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio` guard is
correctly written and does gate what it claims to gate — but what it claims to gate is deliberately
the *non-text UI component* WCAG floor (3.0:1), not the *normal-text* floor (4.5:1) that actually
applies to a rendered label. The guard's own KDoc is explicit about this:

> "the 3.0 floor rather than the 4.5 normal-text target ... because the developer named 3.0
> explicitly for this guard (CR-01)."

On AeroDark, the unselected segment's `fillTop` (the worst case) sits at ~3.3:1 against
`colors.onSurface` — comfortably above 3.0, but still below 4.5. That 14sp label text is neither
large-text-exempt (WCAG 1.4.3's 3:1 large-text carve-out needs ~18pt regular or ~14pt bold) nor a
"UI component" in the graphical-object sense the 3:1 floor is meant for (borders, icons, focus
indicators) — it is a text label, which is exactly the case the task brief flags as easy for a human
visual sign-off to wave through as "looks fine" while still reading as noticeably low-contrast next
to the button/list/switch text elsewhere in the library (all of which clear 4.5:1+ via their own
darken constants).

This is not a functional regression — CR-01's original 1.04:1 defect (label effectively invisible)
is fixed — but the margin chosen leaves AeroDark's raised segment label in a "technically passes,
visually marginal" zone that this exact review round was asked to scrutinize.

**Fix:** Either (a) raise `MIN_LABEL_CONTRAST` to `4.5f` and retune
`RAISED_FILL_TOP_DARKEN`/`RAISED_FILL_BOTTOM_DARKEN` (and correspondingly `RECESSED_FILL_DARKEN`)
until all three schemes clear it at both selection-axis endpoints, or (b) keep the 3.0 floor but
say so explicitly in the human sign-off checklist for 19-12 ("AeroDark unselected segment label is
~3.3:1 — expected to read slightly dim, this is accepted, not a defect") so the visual reviewer is
not left to independently discover and second-guess a margin the code already knows is tight.

---

### WR-02 (carried forward, still open): Disabled `AeroSegmentedControl` still doesn't dim its labels or its frame

**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:100-105`

Re-verified against current source: the outer `Row` still has no `.alpha(...)` treatment when
`enabled == false` — `.height(28.dp).border(1.dp, colors.borderDefault, shape).clip(shape).hoverable(interactionSource)`
is unconditional, and the per-segment `Text` (line 175-179) still always renders
`color = colors.onSurface` at full alpha regardless of `enabled`. Only the per-segment fill
flattens via `resolveSegmentStyle(...).flattenDisabled(colors)`. `AeroListItem` correctly applies
`Modifier.alpha(DISABLED_CONTENT_ALPHA)` to its content row (`AeroListItem.kt:144`); the two
components shipped in the same phase still disagree about what "disabled" looks like. Not addressed
by 19-09/10/11 (out of their scope); still reproducible.

---

### WR-05 (carried forward, still open): `AeroSegmentedControl` keeps a fixed `.height(28.dp)` while `AeroListItem` grows

**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:102`

Re-verified: `.height(28.dp)` (a hard ceiling) is unchanged, and the segment `Text` still sets
neither `maxLines` nor `overflow`. The showcase's `LONG_SEGMENT_LABEL` row
(`SelectionSection.kt:93-99`) still exercises a path with no measured-height regression test, unlike
`AeroListItemLayoutTest`. Segments still carry no `Modifier.weight(1f)`, so N wide segments overflow
the `Row`'s width rather than sharing it. Not addressed by this round.

---

### WR-06 (carried forward, still open): `AeroSwitch` with `onCheckedChange = null` is still a focus stop with a no-op click action

**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt:93-100`

Re-verified: `.toggleable(...)` is still applied unconditionally, with
`onValueChange = { onCheckedChange?.invoke(it) }` (line 99) swallowing the null case rather than
omitting the modifier. A read-only switch still exposes `Role.Switch` + a click action to
accessibility, takes Tab focus, and draws press/focus cues, all for a control that silently no-ops
every activation. Not addressed by this round.

---

### WR-07 (carried forward, still open): `AeroSegmentedControl` still does not enforce "exactly one selected"

**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:88-96,119`

Re-verified: no `require(...)` guards exist; `selected` not present in `options`, `options`
containing duplicates, and empty `options` are all still silently accepted, contradicting the KDoc's
"enforcing exactly one selected option" claim (line 44). This is also the precondition 19-11's WR-03
fix explicitly built its `key(index, opt)` tradeoff around ("duplicate options remain unguarded per
deferred WR-07") — so it is directly load-bearing for whether that tradeoff is still the right one.
Not addressed by this round.

---

### WR-08 (carried forward, still open): Tautological adjacency assertion in `AeroListItemLayoutTest`

**File:** `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt:118-124`

Re-verified: `adjacentSelectedSingleLineRowsEachMeasure36DpAndDoNotOverlap`'s
`assertTrue(bounds2.top >= bounds1.bottom, ...)` is unchanged and is still guaranteed true by
`Column`'s measure policy alone for any `AeroListItem` implementation, including a broken one. Not
addressed by this round.

---

### WR-09 (carried forward, partially mitigated): `AeroSwitchSemanticsTest`'s named G2 regression test still doesn't test the focus-visible gate

**File:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt:91-120`

Re-verified: `aeroSwitchStaysFocusedAndSpaceStillTogglesAfterTheFocusVisibleGate` is unchanged and
still would pass unmodified if `AeroSwitch.kt`'s focus gate were reverted to the raw `focused` flag
— it asserts only that Space still toggles and the node stays focused, never anything about whether
a ring is drawn. This is now **partially mitigated** rather than fully open: 19-09 separately added
`AeroSwitchFocusVisibleWiringTest.kt`, which is a genuine composed proof that does assert on
`state.focusVisible` through a real focus-loss/re-focus sequence with a real `AeroSwitch`. The
originally-named test's misleading KDoc/coverage claim itself is still not corrected, so the
underlying naming/documentation issue stands, but the "no composed test proves the gate" premise the
original finding rested on no longer holds library-wide.

---

### WR-10 (carried forward, still open): Bare `kotlin.assert` in `AeroSegmentedControlStylesTest`

**File:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt:200-203`

Re-verified: `hoveredSelectedEqualsRecessedHoverLightenNeverUnselectedHoverStyle` still uses bare
`assert(resolved != unselectedHoverStyle) { ... }`, gated on the JVM `-ea` flag rather than
`kotlin.test.assertNotEquals` (used for the equivalent invariant elsewhere in this same file and in
`AeroListItemStylesTest`). Not addressed by this round.

---

### WR-11 (carried forward, partially mitigated): `FocusVisibilityTest` still doesn't cover duplicate-emission or Release/Cancel paths

**File:** `library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt`

19-09 added the missing CR-02 regression case
(`mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed`), closing the specific gap
CR-01/CR-02 review round called out. Still not covered, and now slightly more relevant given WR-04's
fix relies on each interaction modifier being the *sole* hover emitter on its source: a fold proving
`Enter, Enter, Exit` still resolves `hovered` sensibly (duplicate emission would no longer occur in
production per WR-04's fix, but nothing pins that assumption at the reducer level should a future
edit reintroduce a second emitter), and nothing pins `PressInteraction.Release`/`Cancel` falling
through to `else -> this` rather than clearing `pointerAcquired`. Not addressed by this round.

---

## Info

### IN-01 (carried forward, still open): `animatedAlpha`/`ANIMATION_DURATION_MS` remain dead code

**File:** `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt:22,36-42`

Re-verified: still no call site in `library/` or `showcase/`. Not addressed by this round.

---

### IN-02 (carried forward, still open): Source-scan guards ban identifiers rather than behaviour

**Files:** `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt:140-162`,
`library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt:65-81`

Re-verified: `aeroListItemRowGrowthContractSurvives` still permanently bans the bare tokens
`maxLines`/`TextOverflow` anywhere in non-comment source (rather than relying on the stronger
measured-height tests already present), and `sourceUsesSelectableRoleRadioButtonNotBareClickable`
still scans the raw (not comment-stripped) source for its `clickable(` ban, unlike sibling guards in
the same file that deliberately strip comments first. Not addressed by this round.

---

### IN-03 (carried forward, still open): `AeroListItem` hardcodes content-padding magic numbers

**File:** `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt:143,146`

Re-verified: `padding(horizontal = 12.dp, ...)` and `Arrangement.spacedBy(8.dp)` are still inline
literals, unlike every other geometry constant in this file. Not addressed by this round.

---

### IN-04 (carried forward, still open): `SelectionSection.kt` still uses wildcard imports

**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt:3,5,11`

Re-verified: `androidx.compose.foundation.layout.*`, `androidx.compose.runtime.*` and
`com.mordred.aero.components.selection.*` are all still wildcards, unlike the sibling
`ListSection.kt`. Not addressed by this round.

---

### IN-05 (carried forward, still open): Showcase "disabled" segmented strip still shares live state

**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt:88`

Re-verified: `AeroSegmentedControl(options = listOf("Day","Week","Month"), selected = segValue, onSelect = {}, enabled = false)`
still binds `selected = segValue` — the live strip's own state — so the disabled row's highlighted
segment still silently follows whatever the reviewer clicks in the live strip above it. Not
addressed by this round.

---

_Reviewed: 2026-07-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
