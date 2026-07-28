---
phase: 19-selectors-lists
reviewed: 2026-07-28T00:00:00Z
depth: standard
files_reviewed: 17
files_reviewed_list:
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
  - library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt
  - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt
  - library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
findings:
  critical: 2
  warning: 11
  info: 5
  total: 18
status: issues_found
---

# Phase 19: Code Review Report

**Reviewed:** 2026-07-28
**Depth:** standard
**Files Reviewed:** 17
**Status:** issues_found

## Summary

Phase 19 restyled `AeroSwitch`, `AeroSegmentedControl` and `AeroListItem`, then closed three
human-gate defects (G1 row growth, G2 focus-visible, G3 selected-segment label colour).

Three of the four items flagged for scrutiny in the phase brief check out cleanly and are
**not** findings:

- `RECESSED_FILL_DARKEN` really is applied **after** `pressedRecess` (`AeroSegmentedControl.kt:231-235`),
  `Color.darken` (`ColorMath.kt:22-27`) multiplies RGB only and copies `alpha` verbatim, and with
  `amount = 0.20f` no channel can go negative. The order and the alpha-safety claims hold.
- `PRESSED_INNER_SHADOW` has exactly one declaration (`AeroButtonSurface.kt:126`); the only other
  references are the segmented control's import/use and the styles test. Nothing redeclares or
  mutates it.
- The `heightIn` + `matchParentSize()` combination is measured correctly: `BoxMeasurePolicy` sizes
  the Box from the non-`matchParentSize` child (the content `Row`) plus the incoming `minHeight`
  of 36.dp, then measures the pill/focus boxes at that resolved size. The trailing-content slot and
  the `onClick == null` display-only path both behave as documented.

The fourth item — the new `FocusVisibility` reducer — **does** mishandle a reachable sequence and
resurfaces the exact G2 symptom it was written to kill (CR-02).

The larger problem is elsewhere. The G3 fix reasoned about the *selected* label's colour but never
re-checked the *unselected* labels against the opaque `primary`-derived fill the 19-03 restyle
gave every segment. Measured contrast is 1.04:1 on AeroDark — the labels are effectively invisible
(CR-01), and `AeroButtonSurface` had already diagnosed and fixed this exact failure mode two
phases earlier.

Test coverage is where the review is most adversarial: the G2 regression test asserts nothing
about the gate it names, one layout assertion is a tautology, and `FocusVisibilityTest` covers
only happy paths the implementation already satisfies — which is precisely why CR-02 shipped.

## Structural Findings (fallow)

_No `<structural_findings>` block was supplied for this review._

## Narrative Findings (AI reviewer)

## Critical Issues

### CR-01: Segment labels are illegible on the new raised fill — 1.04:1 contrast on AeroDark

**Severity:** BLOCKER
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:135,156-160` (with `AeroSurfaceStyle.kt:49-60`, `AeroOrnamentTokens.kt:47-48`)

**Issue:**
G3 put every label on `colors.onSurface` at full alpha. Correct in isolation — but the 19-03
restyle in the same phase simultaneously gave **every** segment (selected *and* unselected) an
opaque fill derived from `colors.primary`:

```
AeroSurfaceStyle.rest(colors, 4.dp).fillTop    = ornaments.fillSplitTop    = primary.lighten(0.08f)
AeroSurfaceStyle.rest(colors, 4.dp).fillBottom = ornaments.fillSplitBottom = primary.darken(0.12f)
```

`primary` is fully opaque (`0xFF...`) on all three schemes, so this is a solid light-blue plate.
Before the restyle the unselected segment background was `Color.Transparent`
(`git show 669980a~1`), so `onSurface` sat on the dark panel and read fine. It no longer does.

Measured WCAG 2.x contrast of `onSurface` against `fillTop` (the upper half of the segment, which
`glossAlpha = 0.22f` lightens further):

| Scheme | `onSurface` | `fillTop` | Contrast |
|---|---|---|---|
| AeroDark | `#CCCCCC` | `#99CEF9` | **1.04 : 1** |
| AeroBlue | `#E0E0E0` | `#5DC8F8` | **1.44 : 1** |
| Classic  | `#E0E0E0` | `#6993C4` | **2.42 : 1** |

AA normal text requires 4.5:1; even non-text UI needs 3:1. On AeroDark the label is essentially
invisible against the unselected segments.

This is not a novel discovery — `AeroButtonSurface.kt:168-176` already documents it:

> "the ornament-derived `fillSplitTop` (`primary.lighten(0.08f)`) read too light for legible white
> button text on the light-blue Aero primaries, so the BUTTON ... overrides to a darker two-tone."

The button fixed it with `primary.darken(0.20f)/darken(0.36f)`. The segmented control consumes
`AeroSurfaceStyle.rest` **unmodified** for its raised base and therefore reproduces the bug the
button already solved. Ironically the *selected* (recessed) segment — the one G3 was about — is the
only one that reads acceptably (~2.94:1 on AeroBlue), because `RECESSED_FILL_DARKEN` darkens it.

Note also that `AeroSegmentedControlStylesTest.recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged`
(line 247) is the only value-level check touching this area, and it only compares selected vs
unselected luminance. Nothing anywhere asserts label-vs-fill contrast, so the defect is invisible
to the suite.

**Fix:** darken the raised base the same way `resolveButtonStyle` does, so raised and recessed both
land in a legible neighbourhood. In `resolveSegmentStyle`:

```kotlin
/** Darken applied to the RAISED segment fill so onSurface labels stay legible against it —
 *  the same correction AeroButtonSurface.FILLED_FILL_TOP_DARKEN/BOTTOM_DARKEN already make
 *  for the identical primary-derived fill (see that file's FIX B KDoc). */
private const val RAISED_FILL_TOP_DARKEN: Float = 0.20f
private const val RAISED_FILL_BOTTOM_DARKEN: Float = 0.36f

internal fun resolveSegmentStyle(...): AeroSurfaceStyle {
    val restStyle = AeroSurfaceStyle.rest(colors, cornerRadius = SEGMENT_CORNER_RADIUS)
    val base = restStyle.copy(
        fillTop = colors.primary.darken(RAISED_FILL_TOP_DARKEN),
        fillBottom = colors.primary.darken(RAISED_FILL_BOTTOM_DARKEN),
    )
    val pressedTransform = base.pressedRecess(PRESSED_INNER_SHADOW)
    // ... unchanged from here
}
```

Then add a value-level guard that fails on regression, e.g. assert
`contrastRatio(colors.onSurface, resolved.fillTop) >= 3f` for every scheme in both the
`selectedProgress = 0f` and `= 1f` cases. Note this changes the recessed style away from a literal
`rest(...).pressedRecess(...)`, so `AeroSegmentedControlStylesTest.selectedEqualsIndependentlyReconstructedPressedRecess`
and the VSEL-03 anti-drift KDoc must be updated to describe the darken as applied to the raised
base rather than only to the recessed fill.

Separately, `AeroSegmentedControl.kt:78` instructs future reviewers that "A future reviewer must
not reintroduce a per-state label colour to 'fix' contrast." That ban is currently absolute and
locks in the defect. Narrow it to "must not reintroduce a *state-dependent* label colour; fix
contrast in the fill instead."

---

### CR-02: `FocusVisibility.reduce` drops `hovered` on `Unfocus`, resurfacing the G2 symptom

**Severity:** BLOCKER
**File:** `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt:118-125`

**Issue:**

```kotlin
is FocusInteraction.Unfocus -> FocusVisibility()   // line 123 — resets hovered too
```

`FocusVisibility()` zeroes all three fields, including `hovered`. But `hovered` does not track
focus — it tracks the physical pointer. Compose emits `HoverInteraction.Enter` **only** on a
pointer-enter transition; if the pointer is already resting inside the control when focus is lost,
no further `Enter` will ever arrive. The reducer's `hovered` is therefore stuck at `false` while
the pointer is still physically over the control, and the `is PressInteraction.Press -> if (hovered)`
guard on line 121 stops firing.

Reachable repro (pointer never moves):

1. Mouse-click the switch. → `Enter`, `Press`, `Focus` → `hovered=T, pointerAcquired=T, focused=T`,
   ring correctly suppressed.
2. Press **Tab** to move focus to the next control, leaving the pointer where it is.
   → `Unfocus` → state reset to all-`false`. No `HoverInteraction.Exit` is emitted (the pointer
   did not leave).
3. Shift+Tab back. → `Focus` → `visible = true`. Still correct.
4. Click the control again with the same, still-resting pointer. → `Press` arrives with the
   reducer's `hovered == false` → `pointerAcquired` stays `false` → **the focus ring is drawn for
   a mouse click**, and stays drawn after the pointer leaves.

That is verbatim the reported G2 symptom the reducer exists to eliminate. The same sequence is
reachable via window deactivation/reactivation (alt-tab away and back with the pointer parked on
the control) and via any programmatic focus move.

`FocusVisibilityTest.recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain` (line 94) walks the
first three steps and stops — it never issues the fourth interaction, so the defect is uncovered.

**Fix:** preserve the pointer-derived field across the focus reset:

```kotlin
internal fun FocusVisibility.reduce(interaction: Interaction): FocusVisibility = when (interaction) {
    is HoverInteraction.Enter -> copy(hovered = true)
    is HoverInteraction.Exit -> copy(hovered = false)
    is PressInteraction.Press -> if (hovered) copy(pointerAcquired = true) else this
    is FocusInteraction.Focus -> copy(focused = true)
    // Unfocus clears the focus-derived fields ONLY. `hovered` mirrors the physical pointer,
    // which losing focus does not move — and Compose emits HoverInteraction.Enter only on a
    // pointer-ENTER transition, so a hovered flag cleared here can never be restored while the
    // pointer sits still (the reducer would then treat the next mouse press as keyboard focus).
    is FocusInteraction.Unfocus -> FocusVisibility(hovered = hovered)
    else -> this
}
```

Add the missing regression case:

```kotlin
@Test
fun mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed() {
    val enter = HoverInteraction.Enter()
    val focus = FocusInteraction.Focus()
    val result = fold(
        enter,                              // pointer arrives and never leaves
        PressInteraction.Press(Offset.Zero),
        focus,
        FocusInteraction.Unfocus(focus),    // Tab away — no HoverInteraction.Exit is emitted
        FocusInteraction.Focus(),           // Shift+Tab back
        PressInteraction.Press(Offset.Zero) // click again with the SAME resting pointer
    )
    assertFalse(result.visible, "a mouse press must stay suppressed even though the hover Enter " +
        "that preceded it arrived before an intervening Unfocus")
}
```

---

## Warnings

### WR-01: `AeroButtonSurface` still gates its focus glow on the raw `focused` flag

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:96`

**Issue:** The phase added `focusVisible` to `AeroInteractionState` and wired it into `AeroSwitch`,
`AeroSegmentedControl` and `AeroListItem` — but not into `AeroButtonSurface`, which sits in the
review scope, calls the same `rememberAeroInteractionState`, and is the file that exports
`PRESSED_INNER_SHADOW` to the segmented control:

```kotlin
.aeroGlowRing(
    active = state.focused && enabled,   // line 96 — raw flag
    ...
)
```

`AeroButton` and `AeroOutlinedButton` therefore keep the exact G2 defect the phase fixed
everywhere else: a mouse click leaves a residual focus glow ring after the pointer moves away.
The library is now internally inconsistent — the same gesture on a button and on a switch produces
different cues.

Relatedly, `resolveButtonStyle(focused = ...)` (line 253) is a parameter that never branches
anything. Its KDoc acknowledges this, but it is still a dead argument that has to be kept in sync
by every caller.

**Fix:** `active = state.focusVisible && enabled`, and add the corresponding guard to
`AeroButtonSurfaceSourceTest` mirroring `AeroSwitchSourceTest.aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag`.
If buttons are intentionally excluded, say so explicitly in `AeroButtonSurface`'s KDoc — right now
the omission is indistinguishable from an oversight.

---

### WR-02: Disabled `AeroSegmentedControl` no longer dims its labels or its frame (regression)

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:97-103,156-160`

**Issue:** The pre-restyle implementation dimmed the whole control when disabled:

```kotlin
.alpha(if (enabled) 1f else 0.4f)   // 669980a~1, dropped by the restyle
```

The new code flattens only the per-segment **fill** (`resolveSegmentStyle` → `flattenDisabled`).
The labels still render at `colors.onSurface` full alpha and the outer
`.border(1.dp, colors.borderDefault, shape)` still draws at full strength. A disabled strip
therefore reads as "enabled but oddly grey", not disabled — and this is directly visible in the
showcase (`SelectionSection.kt:87-89`).

Compare `AeroListItem.kt:138`, which correctly keeps
`Modifier.alpha(DISABLED_CONTENT_ALPHA)` on its content `Row`. The two components in the same
phase now disagree about what "disabled" looks like.

**Fix:** apply the same content-alpha treatment used by `AeroListItem`, and dim the frame:

```kotlin
/** Alpha applied to segment labels and the outer frame when disabled — the per-segment fill
 *  flattens separately via flattenDisabled, matching AeroListItem.DISABLED_CONTENT_ALPHA. */
private const val DISABLED_CONTENT_ALPHA: Float = 0.4f

Row(
    modifier = modifier
        .height(28.dp)
        .then(if (!enabled) Modifier.alpha(DISABLED_CONTENT_ALPHA) else Modifier)
        .border(1.dp, colors.borderDefault, shape)
        .clip(shape)
        .hoverable(interactionSource)
)
```

Note the `.alpha()` must sit before `.border(...)` to dim the frame too, and this composes with
`flattenDisabled` rather than replacing it.

---

### WR-03: `remember` and `animateFloatAsState` inside `forEachIndexed` with no `key()`

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:104-113`

**Issue:**

```kotlin
options.forEachIndexed { index, opt ->
    val isSelected = (opt == selected)
    val segSource = remember { MutableInteractionSource() }      // line 106 — positional only
    val segState = rememberAeroInteractionState(segSource)
    val selectedProgress by animateFloatAsState(..., label = "segSelected_$index")   // line 109
```

All iterations share one call site, so Compose memoizes them purely by position in the slot table.
When `options` changes (an item inserted, removed, or reordered), segment *i* inherits the
`MutableInteractionSource`, the `FocusVisibility` state, and the in-flight selection tween of
whichever option previously occupied slot *i*. Concretely: removing `"Day"` from
`["Day","Week","Month"]` leaves `"Week"` rendering with `"Day"`'s `selectedProgress = 1f`
animation and `"Day"`'s stale hover/press/focus state, so it briefly draws recessed and possibly
hover-lit while the tween unwinds. `rememberAeroInteractionState` also starts a `LaunchedEffect`
per segment keyed on the source, which is now the wrong source for that option.

`options` is a public `List<T>` parameter with no stability contract, so this is caller-reachable,
not theoretical.

**Fix:** wrap the loop body so identity follows the option, not the index:

```kotlin
options.forEachIndexed { index, opt ->
    key(opt) {
        val isSelected = (opt == selected)
        val segSource = remember { MutableInteractionSource() }
        ...
    }
}
```

(Use `key(index, opt)` if duplicate options must be tolerated — see WR-07.)

---

### WR-04: Redundant `.hoverable(...)` double-emits hover, and stays enabled when the control is disabled

**Severity:** WARNING
**Files:** `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt:94`,
`library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:134`,
`library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt:102`

**Issue:** All three components chain an explicit `.hoverable(source)` onto the **same**
`InteractionSource` they already pass to `clickable` / `selectable` / `toggleable`. Verified
against the resolved artifact (`foundation-desktop-1.11.1.jar`): `AbstractClickableNode` — the
shared base of all three interaction modifiers — carries its own
`HoverInteraction$Enter hoverInteraction` field plus `emitHoverEnter`/`emitHoverExit`/`onPointerEvent`
members. It already emits hover on the supplied source.

Two consequences:

1. **Duplicate emissions.** Each pointer entry/exit produces two `Enter`/`Exit` pairs on one
   source. `collectIsHoveredAsState` survives this because it is reference-counted (it keeps a list
   of `Enter`s and removes on the matching `Exit`). `FocusVisibility.reduce` is **not** — it models
   `hovered` as a plain `Boolean`, so the *first* `Exit` clears it while a second `Enter` is still
   outstanding. The reducer's hover model now disagrees with the platform's own bookkeeping about
   the same stream. Today the emissions arrive batched (`E,E … X,X`) so the press always lands
   inside the true-window, but the model is wrong and this is exactly the kind of fragility that
   produced CR-02.

2. **Disabled controls still report hovered.** `Modifier.hoverable` defaults `enabled = true`, and
   none of the three call sites pass `enabled = enabled`. A disabled row/segment/switch sets
   `state.hovered = true` on pointer-over; only the downstream `&& enabled` guards in
   `resolveListItemPillStyle`, `resolveSegmentStyle` and the `aeroGlowRing` calls stop it from
   rendering. Any future consumer of `state.hovered` that forgets that guard gets a hover cue on a
   dead control.

`VLST-04`'s source guards (`AeroListItemSourceTest.kt:85`, `AeroSwitchSourceTest.kt:77`,
`AeroSegmentedControlSourceTest.kt:94`) mandate `.hoverable(` be present, so this cannot be fixed
without touching those guards — worth doing, since VLST-04's real intent ("never raw
`pointerInput` hover tracking") is already satisfied by the interaction modifiers alone.

**Fix (minimum, no guard churn):** pass the enabled flag through —

```kotlin
.hoverable(interactionSource, enabled = enabled)
```

**and** make the reducer count `Enter`s so duplicate emissions cannot desync it:

```kotlin
internal data class FocusVisibility(
    val focused: Boolean = false,
    val hoverCount: Int = 0,
    val pointerAcquired: Boolean = false
) {
    val hovered: Boolean get() = hoverCount > 0
    val visible: Boolean get() = focused && !pointerAcquired
}

is HoverInteraction.Enter -> copy(hoverCount = hoverCount + 1)
is HoverInteraction.Exit  -> copy(hoverCount = (hoverCount - 1).coerceAtLeast(0))
```

(`FocusVisibilityTest.concurrencyHoverAfterKeyboardFocusStaysVisibleAndHoveredIsTrue` asserts on
`result.hovered`, which the derived property keeps satisfying.)

---

### WR-05: `AeroSegmentedControl` keeps the fixed height that G1 just removed from `AeroListItem`

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:97-99,156-160`

**Issue:** `.height(28.dp)` is a hard ceiling, and the segment `Text` sets neither `maxLines` nor
`overflow`. A label wider than its segment wraps to a second line, which is then clipped by the
fixed 28.dp *and* by the outer `.clip(shape)` — silently, with no ellipsis. This is the identical
defect class G1 fixed in `AeroListItem` in the same phase, left in place one file over.

The showcase deliberately renders a 65-character segment label
(`SelectionSection.kt:104` `LONG_SEGMENT_LABEL`, used at line 95) "to exercise the E2 overflow
backstop" — so the phase knows this path exists but has no measured-height test for it, unlike
`AeroListItemLayoutTest`.

Second, related problem: segments carry no `weight`, so N segments with wide labels overflow the
`Row`'s incoming width constraint and the trailing segments are clipped out of existence rather
than shrinking.

**Fix:** decide the contract explicitly and test it. Either
(a) mirror G1 — `.heightIn(min = SEGMENT_MIN_HEIGHT)` and let the strip grow, adding an
`AeroSegmentedControlLayoutTest` measuring the long-label case the way `AeroListItemLayoutTest`
does; or
(b) keep the fixed height and truncate deliberately —
`Text(..., maxLines = 1, overflow = TextOverflow.Ellipsis)` plus `Modifier.weight(1f)` on each
segment `Box` so they share the available width instead of overflowing.
Either is defensible; silently clipping is not.

---

### WR-06: `AeroSwitch` with `onCheckedChange = null` is a focus stop with a no-op click action

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt:94-101`

**Issue:**

```kotlin
.toggleable(
    value = checked,
    enabled = enabled,
    role = Role.Switch,
    interactionSource = interactionSource,
    indication = null,
    onValueChange = { onCheckedChange?.invoke(it) }   // line 100
)
```

`onCheckedChange` is declared nullable, but `toggleable` is applied unconditionally. A switch
constructed with `onCheckedChange = null` (the documented way to express a read-only/parent-driven
switch) still:

- exposes `Role.Switch` **and** a click action to accessibility, so a screen reader announces it as
  actionable;
- takes Tab focus and draws the focus glow ring;
- shows press cues via `state.pressed`;
- silently swallows every activation.

Material3's own `Switch` omits the toggleable modifier entirely when `onCheckedChange == null`,
precisely so the parent row can own the semantics. `AeroSwitchSemanticsTest` never covers the null
case.

**Fix:**

```kotlin
.then(
    if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            indication = null,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }
)
```

Add a semantics test asserting a null-handler switch exposes no click action and is not a focus
stop, mirroring `AeroListItem`'s display-only (`onClick == null`) contract.

---

### WR-07: `AeroSegmentedControl` does not enforce its documented "exactly one selected" invariant

**Severity:** WARNING
**File:** `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt:43,85-105`

**Issue:** The KDoc opens with "Segmented control **enforcing** exactly one selected option", but
nothing enforces anything. Selection is a plain `opt == selected` on line 105:

- `selected` not present in `options` → **zero** segments selected; every segment renders raised and
  every `SemanticsProperties.Selected` is `false`. No error, no fallback. This is easy to hit
  transiently when `options` changes and the caller's `selected` state has not caught up.
- `options` containing duplicates (or `T` with a coarse `equals`, e.g. a data class where only some
  fields matter) → **several** segments render selected and report `Selected = true` simultaneously,
  which is invalid for `Role.RadioButton`.
- `options` empty → a bare 28.dp bordered strip with no content.

`AeroSegmentedControlSemanticsTest.everySegmentHasRoleRadioButtonAndExactlyOneReportsSelected`
only ever tests the well-formed case.

**Fix:** either enforce or downgrade the claim. Cheapest honest option:

```kotlin
require(options.isNotEmpty()) { "AeroSegmentedControl requires at least one option" }
require(options.count { it == selected } <= 1) {
    "AeroSegmentedControl: `selected` matches ${options.count { it == selected }} options — " +
        "`options` must not contain duplicates under T.equals()"
}
```

and change the KDoc from "enforcing" to a stated precondition: "`selected` must be an element of
`options`; if it is not, no segment renders selected."

---

### WR-08: `adjacentSelectedSingleLineRowsEachMeasure36DpAndDoNotOverlap` asserts a tautology

**Severity:** WARNING
**File:** `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt:118-124`

**Issue:**

```kotlin
val bounds1 = onNodeWithTag("row1").fetchSemanticsNode().boundsInRoot
val bounds2 = onNodeWithTag("row2").fetchSemanticsNode().boundsInRoot
assertTrue(bounds2.top >= bounds1.bottom, "adjacent selected rows must not overlap: ...")
```

The two rows are children of a `Column`, which stacks children without overlap by definition. The
assertion is satisfied by `Column`'s measure policy alone and cannot fail for **any**
`AeroListItem` implementation — including the pre-G1 fixed-height one, or one that draws its pill
2000dp tall. It reports coverage of the "adjacent selected pills must not merge into one bar"
concern (the showcase calls this the "pill adjacency backstop", `ListSection.kt:60-71`) while
providing none.

**Fix:** measure the thing that can actually regress — the painted gap between the two pills,
which is `2 * PILL_VERTICAL_INSET = 4.dp`. Since the pill is not a semantics node, assert the
invariant that produces it instead: that each row's height is exactly `ROW_MIN_HEIGHT` and that
`bounds2.top - bounds1.bottom == 0` while the inset is non-zero — or, more robustly, capture the
rendered bitmap and assert the row-boundary scanline is background-coloured. At minimum, delete the
vacuous assertion so the test's name stops overstating what it proves.

---

### WR-09: The named G2 regression test does not test the focus-visible gate

**Severity:** WARNING
**File:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt:86-120`

**Issue:** `aeroSwitchStaysFocusedAndSpaceStillTogglesAfterTheFocusVisibleGate` carries a KDoc
declaring it the "19-05/gap G2 regression" proof. What it actually does:

```kotlin
node.requestFocus()
node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
node.performKeyInput { pressKey(Key.Spacebar) }
assertEquals(1, invocationCount, ...)
assertEquals(true, checked, ...)
node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
```

That is a near-verbatim duplicate of `aeroSwitchSpaceKeyFlipsCheckedExactlyOnceAfterFocus`
(line 59) plus one extra `Focused` assertion. It never issues a pointer gesture, never touches
`focusVisible`, and would pass unchanged if `AeroSwitch.kt:104` were reverted to
`active = state.focused && enabled`. It proves only "the gate did not break Space", never "the gate
works".

Across the whole suite there is **no** composed test that asserts anything about whether a focus
cue is drawn. The only real G2 coverage is the pure `FocusVisibilityTest` fold — which is why
CR-02 shipped undetected.

**Fix:** either rename the test to what it is
(`aeroSwitchFocusAndSpaceBindingSurviveTheFocusVisibleGate`) and drop the G2-proof claim from its
KDoc, or make it real: drive `performMouseInput { moveTo(center); press(); release() }` against the
switch and assert via a captured `AeroInteractionState` (or a test-only `onSemantics`/testTag on the
glow-ring node) that the ring is not active, then `requestFocus()` and assert it is.

---

### WR-10: Bare `kotlin.assert` in an otherwise `kotlin.test`-based styles suite

**Severity:** WARNING
**File:** `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt:190-193`

**Issue:**

```kotlin
val unselectedHoverStyle = expectedRaised(colors).hoverLighten()
assert(resolved != unselectedHoverStyle) {
    "hovered + selected must NEVER equal the unselected-hover style for $colors"
}
```

`kotlin.assert` compiles to `if (_Assertions.ENABLED) { ... }`, gated on the JVM `-ea` flag. Every
other assertion in this file (and in the sibling `AeroListItemStylesTest`, which uses
`assertNotEquals` for the exact same invariant at line 118) uses `kotlin.test`, which always runs.
Gradle's `Test` task defaults `enableAssertions = true`, so it executes today — but any change to
`jvmArgs`/`enableAssertions` silently deletes this check with no test failure to signal it.

**Fix:**

```kotlin
assertNotEquals(
    expectedRaised(colors).hoverLighten(),
    resolved,
    "hovered + selected must NEVER equal the unselected-hover style for $colors",
)
```

---

### WR-11: `FocusVisibilityTest` covers only paths the implementation already satisfies

**Severity:** WARNING
**File:** `library/src/test/kotlin/com/mordred/aero/components/common/FocusVisibilityTest.kt`

**Issue:** All nine cases are happy paths. The reducer's actual failure surface is untested:

- **`Unfocus` while hovered, then re-`Focus`, then `Press`** — the CR-02 defect.
  `recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain` (line 94) stops one interaction short.
- **Duplicate `Enter`/`Exit` pairs.** The shared `InteractionSource` really does emit two of each
  (see WR-04), and the reducer's boolean model handles them differently from
  `collectIsHoveredAsState`. Nothing folds `Enter, Enter, Exit` and asserts `hovered`.
- **`PressInteraction.Release` / `PressInteraction.Cancel`.** Both fall into `else -> this` on
  line 124. Falling through is the intended design, but nothing pins it, so a future edit that
  clears `pointerAcquired` on `Release` would reintroduce the residual-ring symptom with a green
  suite.
- **Repeated `Focus` without an intervening `Unfocus`**, which Compose can emit on window
  re-activation.

`defaultStateIsNotVisible` (line 143) asserts only that a freshly-constructed data class holds its
own default values — it exercises no reducer logic at all.

**Fix:** add the four cases above. The CR-02 case is written out in that finding.

---

## Info

### IN-01: `animatedAlpha` and `ANIMATION_DURATION_MS` are dead code

**Severity:** INFO
**File:** `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt:22,36-42`

`animatedAlpha` has no call site in `library/` or `showcase/` (grep confirms declaration only), and
`ANIMATION_DURATION_MS` exists solely to feed it. Both are `internal`, so nothing outside the module
can be relying on them. `rememberHoverState`/`rememberPressedState`/`rememberFocusState` above them
are still live (`AeroIconButton.kt:61-63`) but are now a second, parallel way to read the same three
booleans that `rememberAeroInteractionState` bundles — worth migrating `AeroIconButton` and deleting
them so there is one collection point, matching the file's own stated "single connection point"
rationale (line 45-52).

**Fix:** delete `animatedAlpha` and `ANIMATION_DURATION_MS`; file the `AeroIconButton` migration as
follow-up.

---

### IN-02: Source-scan guards ban identifiers rather than behaviour

**Severity:** INFO
**Files:** `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt:133-142`,
`library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt:76-80`

Two issues:

1. `aeroListItemRowGrowthContractSurvives` forbids the bare tokens `maxLines` and `TextOverflow`
   anywhere in non-comment source, permanently. That also bans legitimate future uses — a
   `maxLines = Int.MAX_VALUE` written for clarity, a parameter named `maxLines`, or an ellipsis on
   an unrelated slot such as a trailing badge. `AeroListItemLayoutTest` already proves row growth by
   **measurement**, which is the stronger and more durable guarantee; the token ban adds no signal
   the measurement lacks and will eventually produce a confusing failure whose message
   ("truncation was explicitly rejected") does not match what the developer did.
2. `sourceUsesSelectableRoleRadioButtonNotBareClickable` scans `aeroSegmentedControlSource` (raw)
   rather than `nonCommentSource` for its `assertFalse(... "clickable(")` guard, unlike the sibling
   guards at lines 154 and 161 which deliberately strip comments. A KDoc sentence explaining
   "replaces the bare `clickable(...)`" — exactly the kind of prose this file is full of — would
   fail the test.

**Fix:** drop the `maxLines`/`TextOverflow` bans in favour of the measured-height tests; switch the
`clickable(` guard to `nonCommentSource` for consistency with the file's own convention.

---

### IN-03: `AeroListItem` hardcodes content-padding magic numbers

**Severity:** INFO
**File:** `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt:137,140`

`padding(horizontal = 12.dp, ...)` and `Arrangement.spacedBy(8.dp)` are inline literals in a file
where every other geometry value (`PILL_CORNER_RADIUS`, `PILL_VERTICAL_INSET`, `ROW_MIN_HEIGHT`,
`ROW_VERTICAL_PADDING`, `FOCUS_STROKE_WIDTH`) is a named `private val` carrying a KDoc rationale.
The horizontal padding in particular defines where content sits relative to the pill's rounded
edge, which is the same concern `ROW_VERTICAL_PADDING` was extracted for in G1.

**Fix:** extract `ROW_HORIZONTAL_PADDING = 12.dp` and `ROW_CONTENT_SPACING = 8.dp` alongside them.

---

### IN-04: `SelectionSection.kt` uses wildcard imports

**Severity:** INFO
**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt:3,5,11`

`androidx.compose.foundation.layout.*`, `androidx.compose.runtime.*` and
`com.mordred.aero.components.selection.*` — while the sibling `ListSection.kt` (edited in the same
plan, 19-04) uses explicit imports throughout. The library wildcard in particular hides which
public components the showcase actually exercises, which matters for a file whose job is to be the
reviewable state matrix.

**Fix:** expand to explicit imports, matching `ListSection.kt`.

---

### IN-05: Showcase "disabled" segmented strip shares live state

**Severity:** INFO
**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt:87-89`

```kotlin
AeroSegmentedControl(options = listOf("Day","Week","Month"), selected = segValue, onSelect = {}, enabled = false)
```

The disabled strip binds `selected = segValue` — the *live* strip's state — so its highlighted
segment silently moves whenever the reviewer clicks the live strip above it. For a row whose stated
purpose is pinning a state for visual sign-off, that is a moving target.

**Fix:** pin it — `selected = "Day"`.

---

_Reviewed: 2026-07-28_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
