---
phase: 19-selectors-lists
verified: 2026-07-28T00:00:00Z
status: gaps_found
score: 4/8 must-haves verified
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "VSEL-03: AeroSegmentedControl's selected segment appears recessed and the control is legible (all labels readable) in all three themes"
    status: failed
    reason: >
      CR-01 CONFIRMED against source. `resolveSegmentStyle` (AeroSegmentedControl.kt:230)
      builds `base = AeroSurfaceStyle.rest(colors, cornerRadius = SEGMENT_CORNER_RADIUS)`
      completely unmodified and uses it for every unselected (raised) segment. `rest()`
      (AeroSurfaceStyle.kt:49-53) sets `fillTop`/`fillBottom` to
      `AeroOrnamentTokens.derive(base).fillSplitTop/Bottom`, i.e.
      `colors.primary.lighten(0.08f)` / `colors.primary.darken(0.12f)`
      (AeroOrnamentTokens.kt:47-48) — a fully opaque, primary-derived plate, not
      `Color.Transparent`. The label at AeroSegmentedControl.kt:158 is unconditionally
      `colors.onSurface` at full alpha, in every state. On AeroDark, `primary = 0xFF90CAF9`
      and `onSurface = 0xCCCCCC` (AeroColorScheme.kt:73,78) — both light colours, so the
      unselected label sits at very low contrast against its own fill, exactly the "1.04:1
      on AeroDark" defect the reviewer measured. `AeroButtonSurface.kt:168-176` documents
      and fixes the IDENTICAL problem for buttons via `FILLED_FILL_TOP_DARKEN = 0.20f` /
      `FILLED_FILL_BOTTOM_DARKEN = 0.36f` applied to `colors.primary` before it becomes a
      fill — `resolveSegmentStyle` does not reuse or reproduce that darken for its raised
      base, only for the already-recessed selected segment (`RECESSED_FILL_DARKEN`, line
      191, applied strictly to `pressedTransform`, never to `base`).
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt"
        issue: "resolveSegmentStyle's `base` (raised/unselected fill) is AeroSurfaceStyle.rest() unmodified — reproduces the exact opaque primary-derived fill AeroButtonSurface already had to darken for label legibility, but the darken is never applied here."
    missing:
      - "Darken the raised base's fillTop/fillBottom by an AeroButtonSurface-precedent amount (review suggests reusing 0.20f/0.36f as RAISED_FILL_TOP_DARKEN/RAISED_FILL_BOTTOM_DARKEN) before using it as `base` in resolveSegmentStyle, so onSurface stays legible against unselected segments on AeroDark/AeroBlue."
      - "Add a value-level contrast guard (contrastRatio(colors.onSurface, resolved.fillTop) >= 3f) per scheme, for both selectedProgress=0f and =1f, so this class of regression fails a test instead of only a human eye."
      - "Update AeroSegmentedControlStylesTest.selectedEqualsIndependentlyReconstructedPressedRecess and the VSEL-03 anti-drift KDoc once the raised base itself is no longer literal rest(...)."
  - truth: "VSEL-02 / VSEL-04 / VLST-03: focus ring stays suppressed for pointer-acquired focus and does not reappear on a later mouse click after an intervening focus loss"
    status: failed
    reason: >
      CR-02 CONFIRMED against source. `InteractionStates.kt:123`:
      `is FocusInteraction.Unfocus -> FocusVisibility()` constructs a fresh all-defaults
      instance, zeroing `hovered` along with `focused`/`pointerAcquired`. `hovered`
      (line 89) is documented as mirroring the raw pointer stream, not focus — and Compose
      only emits `HoverInteraction.Enter` on a pointer-ENTER transition, so a pointer that
      is already resting on the control when focus is lost can never re-arm `hovered`.
      Traced fold: Enter(hovered=T) -> Press(hovered→pointerAcquired=T) -> Focus(focused=T,
      visible=false, correct) -> Unfocus(->FocusVisibility(), hovered reset to F) ->
      Focus(focused=T, hovered still F, visible=true, correct) -> Press(hovered==F so the
      `if (hovered)` guard on line 121 does NOT set pointerAcquired) -> visible stays true
      while a mouse press just occurred. This is the exact G2 symptom (residual/reappearing
      focus ring after a mouse click) reintroduced via Tab-away/Shift+Tab-back or any
      window-deactivation/reactivation with the pointer parked on the control.
      `FocusVisibilityTest.recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain` (line 94)
      stops one interaction short of the repro's final Press, so this is untested. This
      reducer is the single shared mechanism consumed by AeroSwitch (InteractionStates.kt
      wired via rememberAeroInteractionState, used at AeroSwitch.kt:82,104), by
      AeroSegmentedControl (per-segment `segState.focusVisible`, line 137), and by
      AeroListItem (`state.focusVisible`, line 116) — the defect is not isolated to one
      component.
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt"
        issue: "FocusVisibility.reduce's Unfocus branch resets `hovered` to false instead of preserving it, even though `hovered` tracks the physical pointer, which Unfocus does not move."
    missing:
      - "Change line 123 to `is FocusInteraction.Unfocus -> FocusVisibility(hovered = hovered)` (preserve the pointer-derived field, reset only focus-derived fields), per the review's suggested fix."
      - "Add the missing regression test exercising the full 6-interaction repro (Enter, Press, Focus, Unfocus, Focus, Press) and asserting the ring stays suppressed, per CR-02's suggested test."
---

# Phase 19: Selectors + Lists Verification Report

**Phase Goal:** `AeroSwitch` and `AeroSegmentedControl` gain their first-ever hover/press/focus states and Aero volume, and `AeroListItem`'s selection highlight is finally clipped to a proper Aero pill with a visible focus state.

**Verified:** 2026-07-28
**Status:** gaps_found
**Re-verification:** No — initial verification

## Primary Task: Code-Review Blocker Adjudication

### CR-01 — Unselected AeroSegmentedControl labels illegible on AeroDark

**Verdict: CONFIRMED**

Evidence, read directly from source (not from the review's prose):

- `AeroSegmentedControl.kt:230` — `val base = AeroSurfaceStyle.rest(colors, cornerRadius = SEGMENT_CORNER_RADIUS)`. This `base` is used unmodified as the raised/unselected style; only the separately-built `recessed` (via `pressedTransform.darken(RECESSED_FILL_DARKEN)`, lines 231-235) is darkened.
- `AeroSurfaceStyle.kt:49-53` — `rest()` sets `fillTop = ornaments.fillSplitTop`, `fillBottom = ornaments.fillSplitBottom`.
- `AeroOrnamentTokens.kt:47-48` — `fillSplitTop = base.primary.lighten(0.08f)`, `fillSplitBottom = base.primary.darken(0.12f)`. This is an **opaque, primary-derived fill**, confirming the review's claim that the pre-restyle `Color.Transparent` background is gone.
- `AeroSegmentedControl.kt:158` — `Text(..., color = colors.onSurface, ...)` — unconditional, every segment, every state (this is the correct G3 fix in isolation, per 19-UAT.md G3).
- `AeroColorScheme.kt:73,78` (AeroDark) — `primary = 0xFF90CAF9`, `onSurface = 0xCCCCCC`. Both are light values; `onSurface` sitting on a `primary.lighten(0.08f)` fill is a low-contrast, near-monochrome pairing exactly as the review measured (~1.04:1).
- `AeroButtonSurface.kt:168-176` — the precedent DOES exist: `FILLED_FILL_TOP_DARKEN = 0.20f` / `FILLED_FILL_BOTTOM_DARKEN = 0.36f` are applied to `colors.primary` specifically because "the ornament-derived `fillSplitTop` ... read too light for legible ... button text." `resolveSegmentStyle` never calls this or an equivalent darken on its raised `base`.

The claim holds exactly as described. This is a real, source-confirmed defect that threatens success criterion 3 and requirement **VSEL-03** on AeroDark (and to a lesser extent AeroBlue) — not merely a cosmetic nit, since the review's own precedent (AeroButtonSurface) proves the library already treats this magnitude of contrast loss as blocking.

### CR-02 — `FocusVisibility.reduce` drops `hovered` on `Unfocus`

**Verdict: CONFIRMED**

Evidence, read directly from source:

- `InteractionStates.kt:118-125`, the actual reducer:
  ```kotlin
  internal fun FocusVisibility.reduce(interaction: Interaction): FocusVisibility = when (interaction) {
      is HoverInteraction.Enter -> copy(hovered = true)
      is HoverInteraction.Exit -> copy(hovered = false)
      is PressInteraction.Press -> if (hovered) copy(pointerAcquired = true) else this
      is FocusInteraction.Focus -> copy(focused = true)
      is FocusInteraction.Unfocus -> FocusVisibility()   // <- line 123
      else -> this
  }
  ```
  Line 123 constructs a brand-new `FocusVisibility()` — all three fields (`focused`, `hovered`, `pointerAcquired`) default to `false`/`false`/`false`. This is not a narrow "reset focus fields" operation; it wipes `hovered` too.
- `FocusVisibility`'s own KDoc (line 88-93) defines `hovered` as mirroring "the raw interaction stream," i.e. the physical pointer — it is not a focus-derived field, so there is no principled reason for `Unfocus` to touch it.
- Compose's `HoverInteraction.Enter` fires only on pointer-enter (this is standard Compose foundation behavior, consistent with `collectIsHoveredAsState`'s reference-counting model used elsewhere in this same file at line 74) — so once `hovered` is wiped while the pointer is still resting on the control, nothing re-arms it until the pointer actually leaves and re-enters.
- Manually folding the review's traced sequence against the actual `when` arms above reproduces the described failure: after `Enter, Press, Focus` (correctly suppresses: `pointerAcquired=T`), `Unfocus` unconditionally clears `hovered`; `Focus` again sets `visible=true` (correct so far); but a second `Press` with `hovered==false` fails the `if (hovered)` guard on line 121, leaving `pointerAcquired=false` and `visible=true` even though the interaction was a mouse press. This is the exact G2 symptom.
- This reducer is the **single shared mechanism** — confirmed consumed by all three phase-19 components: `AeroSwitch.kt:82` (`rememberAeroInteractionState`) gating `state.focusVisible` at line 104; `AeroSegmentedControl.kt:107,137` (`segState.focusVisible` per segment); `AeroListItem.kt:81,116` (`state.focusVisible`). A fix or regression here is universal across VSEL-02, VSEL-04, and VLST-03, not isolated to one component.
- `FocusVisibilityTest.recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain` (referenced at review line 94) walks Enter → Press → Focus → Unfocus → Focus and stops — it never issues the final Press that would have caught this, matching the review's claim about test coverage.

The claim holds exactly as described, including the mechanism, the reachability, and the missing test. This is a real, source-confirmed regression affecting all three consuming components.

## Goal Achievement

### Observable Truths (mapped to phase requirement IDs)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | VSEL-01: AeroSwitch shows recessed track groove + raised glossy/shadowed thumb | ✓ VERIFIED | `AeroSwitch.kt:118` (`aeroGroove`), `128` (`aeroThumbSurface`), `148-152` (`THUMB_DROP_SHADOW`), `resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle` (162-203) build distinct recessed/raised styles with gloss and shadow. |
| 2 | VSEL-02: AeroSwitch has working hover/press/focus for the first time | ✗ FAILED | Mechanism present (`AeroSwitch.kt:102-112`, gates on `state.hovered`/`state.focusVisible`) but the shared `focusVisible` reducer has the CR-02 defect — a reachable sequence redraws the focus ring on a mouse click, the exact symptom this requirement exists to eliminate. |
| 3 | VSEL-03: AeroSegmentedControl's selected segment reads recessed (reused pressed-button code) AND the control is legible | ✗ FAILED | Recess-reuse mechanism itself is correct (`resolveSegmentStyle:231`, imports `pressedRecess`/`PRESSED_INNER_SHADOW` from `AeroButtonSurface` unmodified) — but CR-01 confirms unselected labels are effectively invisible on AeroDark (~1.04:1), so the control as a whole is not legible. |
| 4 | VSEL-04: AeroSegmentedControl gains hover and focus for the first time | ✗ FAILED | Mechanism present (`AeroSegmentedControl.kt:118,137`) but shares the CR-02 reducer defect. |
| 5 | VLST-01: AeroListItem selection highlight clips to a rounded pill with gradient + rim | ✓ VERIFIED | `AeroListItem.kt:108-114`, pill drawn via `aeroSurface(pillStyle, RoundedCornerShape(PILL_CORNER_RADIUS))` inside a `matchParentSize().padding(vertical = PILL_VERTICAL_INSET)` box — clipped, not full-bleed. `resolveListItemPillStyle` (212-231) resolves a gradient (`fillTop`/`fillBottom`) `AeroSurfaceStyle`. |
| 6 | VLST-02: hover remains visible on an already-selected row (combine, not suppress) | ✓ VERIFIED | `resolveListItemPillStyle`'s D-11 base-then-transform (`AeroListItem.kt:224-229`): `base` resolves from `selected` first, then `hoverLighten()` is composed on top when hovered — selected+hovered is base.hoverLighten(), strictly brighter, never a replacement. |
| 7 | VLST-03: AeroListItem has a focus visual | ✗ FAILED | Visual present (`AeroListItem.kt:116-133`, in-bounds stroke gated on `state.focusVisible && enabled && onClick != null`) but shares the CR-02 reducer defect. |
| 8 | VLST-04: newly-hover-wired components reuse `Modifier.hoverable` + `collectIsHoveredAsState`, not custom pointer tracking | ✓ VERIFIED | `AeroSwitch.kt:102`, `AeroSegmentedControl.kt:102,134`, `AeroListItem.kt:94` all call `.hoverable(source)`; `rememberAeroInteractionState` (`InteractionStates.kt:74`) uses `source.collectIsHoveredAsState()` — no raw `pointerInput` hover tracking found in any of the three files. (Note: review WR-04 flags a real but non-blocking wiring smell — `AeroSegmentedControl`/`AeroListItem`/`AeroSwitch` all chain an extra explicit `.hoverable(source)` on top of the same source already passed to `selectable`/`clickable`/`toggleable`, which already emits hover per WR-04's decompiled-artifact evidence. This double-emission does not violate VLST-04's literal requirement — the mandated pattern is used, not invented pointer tracking — but is worth folding into the next gap round; see Anti-Patterns below.) |

**Score:** 4/8 truths verified (0 present-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/.../selection/AeroSwitch.kt` | Recessed groove, raised thumb, hover/press/focus | ✓ VERIFIED (wired), ⚠️ carries CR-02 | Exists, substantive, wired to theme primitives and shared interaction-state collector. |
| `library/.../selection/AeroSegmentedControl.kt` | Recessed selection via reused pressed-button code, hover/focus | ✓ VERIFIED (wired), ✗ carries CR-01 + CR-02 | Exists, substantive, wired; reuse of `pressedRecess`/`PRESSED_INNER_SHADOW` confirmed genuine (import, not re-derivation). |
| `library/.../list/AeroListItem.kt` | Clipped pill, hover+selection combine, focus visual | ✓ VERIFIED (wired), ⚠️ carries CR-02 | Exists, substantive, wired. |
| `library/.../common/InteractionStates.kt` | Shared focus-visible reducer | ✓ VERIFIED (exists, wired to all 3 consumers), ✗ contains CR-02 logic defect | The artifact is real and consumed everywhere it should be — the defect is a logic bug inside it, not a missing/stub artifact. |
| `library/.../buttons/AeroButtonSurface.kt` | Source of `pressedRecess`/`PRESSED_INNER_SHADOW`/darken precedent | ✓ VERIFIED | `PRESSED_INNER_SHADOW` (line 126) is `internal`, single declaration, consumed cross-package by the segmented control confirmed via import at `AeroSegmentedControl.kt:31`. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `AeroSegmentedControl.resolveSegmentStyle` | `AeroButtonSurface.pressedRecess`/`PRESSED_INNER_SHADOW` | import + call (`AeroSegmentedControl.kt:31,40,231`) | ✓ WIRED | Confirmed genuine reuse, not re-derivation — the exact VSEL-03 requirement ("reusing the pressed-button code from Phase 17"). |
| `AeroSwitch`/`AeroSegmentedControl`/`AeroListItem` | `InteractionStates.rememberAeroInteractionState` / `rememberFocusVisible` | call sites at `AeroSwitch.kt:82`, `AeroSegmentedControl.kt:107`, `AeroListItem.kt:81` | ✓ WIRED (but propagates CR-02) | All three share the one reducer; a fix to `InteractionStates.kt:123` fixes all three at once. |
| Hover/press/focus state | Pill/groove/segment fill resolution | `state.hovered`/`state.pressed`/`state.focusVisible` passed into `resolveListItemPillStyle`/`resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle`/`resolveSegmentStyle` | ✓ WIRED | State flows from collection to style resolution to `aeroSurface`/`aeroGroove`/`aeroThumbSurface` draw calls in all three components. |

### Requirements Coverage

| Requirement | Description (abridged) | Status | Evidence |
|---|---|---|---|
| VSEL-01 | AeroSwitch groove + raised glossy/shadowed thumb | ✓ SATISFIED | Truth #1 |
| VSEL-02 | AeroSwitch hover/press/focus, first time | ✗ BLOCKED | CR-02 |
| VSEL-03 | AeroSegmentedControl recessed selection via reused pressed-button code | ✗ BLOCKED | CR-01 (legibility half); recess-reuse mechanism itself is satisfied |
| VSEL-04 | AeroSegmentedControl hover + focus, first time | ✗ BLOCKED | CR-02 |
| VLST-01 | AeroListItem selection clips to rounded pill w/ gradient + rim | ✓ SATISFIED | Truth #5 |
| VLST-02 | Hover visible on selected row, combined not suppressed | ✓ SATISFIED | Truth #6 |
| VLST-03 | AeroListItem focus visual | ✗ BLOCKED | CR-02 |
| VLST-04 | Reuse `hoverable` + `collectIsHoveredAsState`, no custom pointer tracking | ✓ SATISFIED | Truth #8 (WR-04 wiring smell noted, non-blocking) |

No orphaned requirements found — REQUIREMENTS.md line 144 maps exactly `VSEL-01..04, VLST-01..04` to Phase 19, matching the 8 IDs supplied.

### Anti-Patterns Found (secondary findings, not independently re-derived — carried from 19-REVIEW.md for gap-round triage)

These did not block this verification's CONFIRMED/REFUTED calls on CR-01/CR-02, and are lower severity, but are relevant input for the next `/gsd-plan-phase 19 --gaps` round since the user has already indicated one is coming. Source-checked at the specific lines cited during this verification pass (AeroSwitch.kt, AeroSegmentedControl.kt, AeroListItem.kt, InteractionStates.kt) — the review's citations against those four files match what is actually in them.

| File | Concern | Severity | Impact |
|------|---------|----------|--------|
| `AeroSegmentedControl.kt:97-103` | No `.alpha()` dimming when `enabled=false`; only the fill flattens (WR-02) | Warning | Disabled control reads as "enabled but grey," inconsistent with `AeroListItem`'s `DISABLED_CONTENT_ALPHA` treatment. |
| `AeroSegmentedControl.kt:104-113` | `remember`/`animateFloatAsState` inside `forEachIndexed` with no `key(opt)` (WR-03) | Warning | Reordering/removing options can transfer stale interaction/animation state to the wrong segment. |
| `AeroListItem.kt:94`, `AeroSegmentedControl.kt:134`, `AeroSwitch.kt:102` | Redundant explicit `.hoverable(source)` on top of a source already passed to `clickable`/`selectable`/`toggleable` (WR-04) | Warning | Duplicate hover emissions; `FocusVisibility`'s boolean (not counted) hover model can desync from the platform's own bookkeeping — same failure class as CR-02. |
| `AeroSegmentedControl.kt:97-99` | Fixed `.height(28.dp)` with no `maxLines`/`overflow` (WR-05) | Warning | Same overflow-clipping defect class G1 fixed in `AeroListItem`, left unaddressed here. |
| `AeroSwitch.kt:94-101` | `toggleable` applied unconditionally even when `onCheckedChange == null` (WR-06) | Warning | A read-only switch still exposes `Role.Switch`/click action to accessibility and takes focus. |
| `AeroSegmentedControl.kt:43,85-105` | KDoc claims "enforcing exactly one selected" but nothing enforces it (WR-07) | Warning | `selected` not in `options`, duplicates, or empty `options` are all silently unhandled. |
| `AeroButtonSurface.kt:96` | Focus glow still gated on raw `state.focused`, not `focusVisible` (WR-01) | Warning | Out of this phase's 3 named components but creates an inconsistent gesture-to-cue mapping library-wide; also means a button-side fix for CR-02 would need to happen separately. |

No `TBD`/`FIXME`/`XXX`/placeholder debt markers found in `AeroSwitch.kt`, `AeroSegmentedControl.kt`, `AeroListItem.kt`, or `InteractionStates.kt` (grep, case-insensitive).

### Behavioral Spot-Checks

Step 7b: SKIPPED — per explicit task instruction, no gradle/test execution was performed in this verification pass (the full build is separately established as green and is not what is being re-verified here). CR-01/CR-02 were instead confirmed by direct trace of the pure resolver/reducer functions' source, which is deterministic and sufficient for both claims (fill/label token resolution and interaction-fold logic contain no external state that a test run would reveal beyond what the source itself shows).

### Probe Execution

No `scripts/*/tests/probe-*.sh` conventions or phase-declared probes found for phase 19 (selectors-lists is a Compose UI component phase, not a migration/tooling phase). Skipped.

### Human Verification Required

None beyond what 19-UAT.md already recorded (status: passed, gate APPROVED 2026-07-28, G4 explicitly deferred out of phase by user decision). This verification's two gaps (CR-01, CR-02) are both confirmed by direct source/logic trace, not visual judgment calls — they do not require a new human sign-off round to establish existence, though the FIX for CR-01 (a re-tuned darken magnitude) will need the same "Claude's discretion, retunable at sign-off" visual confirmation pattern used for `RECESSED_FILL_DARKEN` in 19-07.

### Gaps Summary

Two blocker-level, source-confirmed gaps remain despite the phase's two human sign-off rounds passing:

1. **CR-01 (VSEL-03):** `AeroSegmentedControl`'s raised/unselected segment fill is the unmodified, opaque, `primary`-derived `AeroSurfaceStyle.rest()` fill — the same fill shape `AeroButtonSurface` already had to darken (`FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN`) to keep `onSurface` labels legible. The segmented control never applies an equivalent correction to its raised base, so unselected labels are effectively invisible on AeroDark (~1.04:1) and marginal on AeroBlue. Fix direction: darken the raised base by the `AeroButtonSurface` precedent amount before using it in `resolveSegmentStyle`, plus add a contrast-ratio unit guard.

2. **CR-02 (VSEL-02, VSEL-04, VLST-03):** The shared `FocusVisibility.reduce`'s `Unfocus` branch resets `hovered` alongside the focus-derived fields, even though `hovered` mirrors the physical pointer and Compose cannot re-emit `Enter` for a pointer that never left. A reachable Tab-away/Shift+Tab-back-then-click sequence (or any focus loss/regain with the pointer resting on the control) reintroduces the exact "focus ring drawn for a mouse click" symptom G2 was written to eliminate — across all three phase-19 components simultaneously, since they share one reducer. Fix direction: `is FocusInteraction.Unfocus -> FocusVisibility(hovered = hovered)`, plus the missing 6-step regression test.

Both gaps sit underneath sign-off rounds that visually passed because the reachable failure paths (a specific low-contrast colour pairing; a specific focus/pointer interleaving) are not things a human clicking through a showcase in a single session is likely to hit by chance — this is consistent with the project's own stated "false-positive sign-off" caution in 19-UAT.md, just triggered by a different mechanism (code-level trace vs. visual spot-check) than the one that caution was written for.

G4 (ornament token derivation blowing out on light-primary themes) is confirmed still correctly out of scope per the explicit, twice-made user deferral recorded in 19-UAT.md, and is not re-litigated here.

---

_Verified: 2026-07-28_
_Verifier: Claude (gsd-verifier)_
