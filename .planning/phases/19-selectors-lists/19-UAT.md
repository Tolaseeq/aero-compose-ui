---
status: diagnosed
phase: 19-selectors-lists
source: [19-04-PLAN.md task 2 human-verify checkpoint]
started: 2026-07-27T14:31:42Z
updated: 2026-07-27T14:31:42Z
---

## Current Test

number: 4
name: Remaining state matrix — AeroSwitch / AeroSegmentedControl / AeroListItem states not exercised in the 2026-07-27 pass
expected: |
  Every state listed in 19-04-PLAN.md task 2 walked in all three themes
  (AeroBlue, AeroDark, Classic) and explicitly reported pass or fail.
awaiting: user response

## Tests

Verification was performed by the user on 2026-07-27 against the running showcase
(`./gradlew :showcase:run`) at commit `de44669`, in AeroBlue and AeroDark. Findings
below are recorded only for states the user actually reported on, plus states the
assistant could assess directly from the two supplied screenshots.

**Unreported states are recorded as `pending`, never as `passed`.** The project's
v2.0.3 / v2.0.4 false-positive sign-off lesson is cited in 19-04-PLAN.md as the reason
this gate cannot be automated or inferred; marking an unexercised state as passing
would repeat exactly that failure.

### 1. AeroListItem — long-label selected row, pill bounded edge
expected: Long primary label plus secondary line stays within the pill's bounded edge.
result: FAILED — text overflows the pill's bottom edge. Visible in the supplied screenshot on both the "Sent" row (its `secondary line` spills past the pill) and the deliberately-long-label row (text cut at the pill boundary).
reported_by: user
gap: G1

### 2. AeroSegmentedControl — long-label segment overflow behaviour
expected: Scope decision — segment widens cleanly, or truncation should be added.
result: PASSED — user's decision: leave the widening behaviour as is. No truncation to be added. No work follows from this item.
reported_by: user

### 3. AeroListItem — pill geometry (2.dp vertical inset / 6.dp corner radius)
expected: Scope decision — keep 2/6 or move to 4/8 on the 4dp grid.
result: PASSED — assistant took discretion on 2/6 (per the standing preference that pixel deltas are not judgeable from prose); the user's screenshot confirms the two adjacent pinned-selected pills read as two separate pills with a clear gap, which is the case the tighter inset was chosen to protect. Keep 2.dp / 6.dp.
reported_by: assistant, confirmed against user screenshot

### 4. AeroSwitch — focus cue after mouse click
expected: Hover glow appears on mouse-over and clears when the pointer leaves.
result: FAILED — after clicking and moving the pointer away, a highlight remains until focus moves elsewhere. User: "неясно, зачем нужна подсветка, остающаяся после нажатия, выглядит лишней". Hover glow itself is correct and is not in scope to change.
reported_by: user
gap: G2

### 5. AeroSwitch — overall value/brightness on AeroBlue and AeroDark
expected: Control sits within the theme surface's value range; recessed groove and raised thumb legible.
result: FAILED — reads as an eye-searingly light element against the dark background. User: "слишком вырвиглазно-светлым получается из-за светло-голубого фона, белого свечения и белого же контура. Возможно, проблема не только переключателя" — the suspicion that this is not switch-specific is confirmed (see G4).
reported_by: user
gap: G4

### 6. AeroSegmentedControl — overall value/brightness on AeroBlue and AeroDark
expected: Raised glass segments legible against theme surface; exactly one visibly pushed in (VSEL-03).
result: FAILED — same eye-searing lightness (light blue fill, white glow, white text, white contour). Additionally, from the screenshot the assistant judges VSEL-03 unmet on dark themes: the strip reads as one uniformly bright block and the recessed segment is not distinguishable by eye. The AeroChip row directly beneath is the in-showcase reference for correctly restrained value.
reported_by: user (brightness), assistant (VSEL-03 from screenshot)
gap: G3, G4

### 7. AeroSegmentedControl — selected-segment label colour
expected: Label legible against the recessed fill in all three themes.
result: FAILED — selected label turns near-black. User: "странная идея менять цвет текста на чёрный у выбранного элемента, зачем?". Root cause is worse than the reported symptom: the token used carries an alpha, so the selected label is also rendered semi-transparent (see G3).
reported_by: user
gap: G3

### 8. AeroSwitch — rest / toggle / hover / press / disabled matrix, all themes
expected: Per 19-04-PLAN.md task 2 AeroSwitch checklist.
result: [pending]

### 9. AeroSegmentedControl — hover / Tab / Space / N=1 / disabled matrix, all themes
expected: Per 19-04-PLAN.md task 2 AeroSegmentedControl checklist.
result: [pending]

### 10. AeroListItem — VLST-02 selected+hover, focus stroke, display-only non-focusability, disabled
expected: Per 19-04-PLAN.md task 2 AeroListItem checklist. VLST-02 (hover brightens an already-selected row rather than replacing its treatment) is the phase's headline fix and was not reported on.
result: [pending]

### 11. Classic theme — full matrix for all three components
expected: Classic is called out in 19-04-PLAN.md as the theme that matters most, because its tokens are fully opaque and several defects are invisible on AeroBlue/AeroDark.
result: [pending] — the reported pass covered AeroBlue and AeroDark only.

## Summary

total: 11
passed: 2
issues: 5
pending: 4
skipped: 0
blocked: 0

## Gaps

### G1 — AeroListItem row cannot grow, content overflows the pill
status: failed
routes_to: 19-01 (AeroListItem)
requirement: VLST-01, VLST-03
root_cause: |
  `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt:84` sets a fixed
  `.height(36.dp)`. A row carrying a primary plus a secondary line needs more than that, but
  cannot grow, so content spills past both the row and the pill. The pill is the row's bounds
  minus `PILL_VERTICAL_INSET` (2.dp), so it inherits the same ceiling.
fix_direction: |
  Replace the fixed height with `.heightIn(min = 36.dp)` so the row grows with its content and
  the pill grows with the row. Truncation was explicitly NOT chosen — it would mask a layout
  defect rather than fix it, and the user's complaint is that text escapes the pill, not that
  it is long.

### G2 — Focus ring shown for pointer-acquired focus (no focus-visible semantics)
status: failed
routes_to: 19-01, 19-02, 19-03 (shared concern across all three components)
requirement: VSEL-02, VSEL-04, VLST-03
root_cause: |
  `AeroSwitch.kt:97` gates the focus glow on `state.focused && enabled`, and
  `AeroSegmentedControl.kt` gates its focus stroke on `segState.focused && enabled`.
  In Compose a mouse click also grants focus, so the focus indication switches on at click
  and persists after the pointer leaves, until focus moves elsewhere.
fix_direction: |
  Gate focus indication on focus-visible semantics: draw it only when the current input mode is
  keyboard (`LocalInputModeManager.current.inputMode == InputMode.Keyboard`), not merely when the
  element holds focus. The element must remain focused and keyboard-operable either way — only
  the drawing is suppressed. The separate hover glow ring is correct and must NOT be changed;
  the user explicitly wants hover indication retained.

### G3 — Selected segment: wrong label token and wrong fill direction
status: failed
routes_to: 19-03 (AeroSegmentedControl)
requirement: VSEL-03, VSEL-04
root_cause: |
  `AeroSegmentedControl.kt:100` resolves the label as
  `if (isSelected) colors.surface else colors.onSurface`. Two faults:
  (a) `surface` is a background token, and in AeroBlue (`0xCC1A3A5C`) and AeroDark
      (`0xCC1A1A2E`) it carries alpha `0xCC` — so the selected label is drawn ~80% opaque.
      A text colour must never inherit a panel-background alpha.
  (b) Inverting the label colour compensates for a recessed fill that reads lighter than its
      raised neighbours. A pushed-in surface catches less light and should read darker; if the
      fill moved in the correct direction the label could stay `onSurface` across every segment,
      one colour for the whole strip.
  Consequence: VSEL-03 ("exactly one visibly pushed in") is not achieved on dark themes.
fix_direction: |
  Keep the label on `onSurface` in all states. Correct the recessed fill so selection is carried
  by a downward value change plus the existing bevel/inner-shadow geometry, not by a colour
  inversion. Re-check against a pressed `AeroButton` in the Buttons section, which is the
  reference depth the plan reuses verbatim.

### G4 — Ornament token derivation blows out on light-primary themes
status: deferred
routes_to: separate foundation session (Phase 16 territory) — user decision 2026-07-27
requirement: cross-cutting
root_cause: |
  `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt:43` derives
  `hoverGlow = base.primary.lighten(0.30f)`. The formula ignores how light `primary` already is:
    AeroBlue  primary 0xFF4FC3F7  -> +30% lands near white
    AeroDark  primary 0xFF90CAF9  -> +30% lands near white
    Classic   primary 0xFF5C8ABF  -> +30% stays restrained
  `AeroColorScheme.kt` additionally sets `borderSelected = primary` in all three schemes, so the
  contour is near-white on the same two themes. Together these produce the reported
  "light blue fill + white glow + white contour" glare, and explain why Classic is unaffected.
fix_direction: |
  Make the derivation relative to the base colour's existing luminance rather than a flat +30%,
  and reconsider `borderSelected = primary` for light-primary schemes.
scope_note: |
  DEFERRED OUT OF PHASE 19 by explicit user decision. These tokens are Phase 16 foundation and
  affect every component in the library, including work already accepted in phases 16-18.
  Changing them inside the phase 19 gap round would alter the appearance of accepted work
  without re-verifying it. To be handled in a separate foundation session.

## Notes

- Phase 19 is NOT complete. Plan 19-04 stands at its open human-verify checkpoint: task 1
  (`de44669`, showcase state matrix) is committed, task 2 is unapproved, no 19-04-SUMMARY.md
  exists, and STATE.md has not been advanced. This is the correct state for a failed gate.
- Plans 19-01, 19-02 and 19-03 are complete and their test suites are green; the defects above
  are visual/behavioural and were not detectable by their unit tests.
- Next step: `/gsd-plan-phase 19 --gaps` — it should produce gap-closure plans for G1, G2 and G3
  only. G4 is deferred and must not be pulled into that round.
- Execution environment note: subagent dispatch for this phase must use `run_in_background: true`.
  Synchronous dispatch was aborted twice mid-plan at 22 and 39 minutes; background dispatch
  completed a 26-minute run. See the project memory entry `subagent-runtime-ceiling`.
