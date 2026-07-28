---
status: gap-open
phase: 19-selectors-lists
source: [19-04-PLAN.md task 2 human-verify checkpoint, 19-08-PLAN.md task 2 three-theme re-sign-off, 19-12-PLAN.md task 2 gap-round re-sign-off]
started: 2026-07-27T14:31:42Z
updated: 2026-07-28T12:50:00Z
---

## Current Test

number: 11
name: Classic theme — full matrix, all three components (closes the 2026-07-27 pending row)
expected: |
  Every state listed in 19-04-PLAN.md task 2 walked in all three themes
  (AeroBlue, AeroDark, Classic) and explicitly reported pass or fail.
awaiting: none — resolved 2026-07-28. Gate APPROVED. G1/G2/G3 confirmed closed by eye on all
  three themes; G4 remains deferred by explicit reviewer decision.

## Tests

Verification was performed by the user on 2026-07-27 against the running showcase
(`./gradlew :showcase:run`) at commit `de44669`, in AeroBlue and AeroDark. Findings
below are recorded only for states the user actually reported on, plus states the
assistant could assess directly from the two supplied screenshots.

**Unreported states are recorded as `pending`, never as `passed`.** The project's
v2.0.3 / v2.0.4 false-positive sign-off lesson is cited in 19-04-PLAN.md as the reason
this gate cannot be automated or inferred; marking an unexercised state as passing
would repeat exactly that failure.

A second sign-off pass ran on 2026-07-28 (19-08 Task 2), after gap plans 19-05/19-06/19-07
closed G1, G2 and G3, against the running showcase at commit `4d92c13` in all three themes
(AeroBlue, AeroDark, Classic). Its findings are recorded inline against the tests they
resolve, and separately for the four rows that stood at `pending`.

### 1. AeroListItem — long-label selected row, pill bounded edge
expected: Long primary label plus secondary line stays within the pill's bounded edge.
result: FAILED (2026-07-27) — text overflows the pill's bottom edge. Visible in the supplied screenshot on both the "Sent" row (its `secondary line` spills past the pill) and the deliberately-long-label row (text cut at the pill boundary).
result_2026-07-28: PASSED — G1 closed by 19-06 (`AeroListItem.kt`'s row height changed from a fixed `.height(36.dp)` to `.heightIn(min = 36.dp)`, commit `08ed471`). Exercised across all three themes as part of the 2026-07-28 reviewer's blanket "остальное passed" ("everything else passed") report covering Blocks A, B, D, E, F, G; no defect was raised against this row.
reported_by: user (2026-07-27 failure), user (2026-07-28 re-verification)
gap: G1 — resolved

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
result: FAILED (2026-07-27) — after clicking and moving the pointer away, a highlight remains until focus moves elsewhere. User: "неясно, зачем нужна подсветка, остающаяся после нажатия, выглядит лишней". Hover glow itself is correct and is not in scope to change.
result_2026-07-28: PASSED — G2 closed by 19-05's shared `FocusVisibility` reducer (`InteractionStates.kt`, commits `36b291a`/`79b11a5`), consumed by `AeroSwitch`. Exercised across all three themes under the 2026-07-28 reviewer's Block B pass and covered by the same blanket "остальное passed" report; no residual focus cue after a mouse click was reported, hover unaffected.
reported_by: user (2026-07-27 failure), user (2026-07-28 re-verification)
gap: G2 — resolved

### 5. AeroSwitch — overall value/brightness on AeroBlue and AeroDark
expected: Control sits within the theme surface's value range; recessed groove and raised thumb legible.
result: FAILED — reads as an eye-searingly light element against the dark background. User: "слишком вырвиглазно-светлым получается из-за светло-голубого фона, белого свечения и белого же контура. Возможно, проблема не только переключателя" — the suspicion that this is not switch-specific is confirmed (see G4).
reported_by: user
gap: G4 — deferred (see G4 re-report under 2026-07-28 below; not re-recorded as a separate numbered row per 19-08's resume scope, which named tests 1, 4, 6 and 7 for re-recording)

### 6. AeroSegmentedControl — overall value/brightness on AeroBlue and AeroDark
expected: Raised glass segments legible against theme surface; exactly one visibly pushed in (VSEL-03).
result: FAILED (2026-07-27) — same eye-searing lightness (light blue fill, white glow, white text, white contour). Additionally, from the screenshot the assistant judges VSEL-03 unmet on dark themes: the strip reads as one uniformly bright block and the recessed segment is not distinguishable by eye. The AeroChip row directly beneath is the in-showcase reference for correctly restrained value.
reported_by: user (brightness), assistant (VSEL-03 from screenshot)
gap: G3, G4

**This is a SPLIT row — the 2026-07-28 re-verification resolves only its G3 half. Both halves are recorded explicitly below; the row as a whole is not silently passed.**

- **6a. G3 half (VSEL-03, "exactly one segment visibly pushed in")** — result: PASSED (2026-07-28). Closed by 19-07 (`AeroSegmentedControl.kt`: single-token `colors.onSurface` label at full alpha, replacing the alpha-carrying `colors.surface` inversion; `RECESSED_FILL_DARKEN = 0.20f` applied after the imported `pressedRecess` transform — commits `5533bcc`, `93da660`). Reviewer's Block C report: every label is the same colour and legible; the recessed segment reads pushed in, comparable to a pressed `AeroButton`; the strip reads darker than the `AeroChip` reference row. Reviewer's explicit judgement-call answer on the `0.20f` darken magnitude: "в самый раз" (just right) — no follow-up tuning. Exercised on all three themes.
- **6b. G4 half (overall brightness on AeroBlue/AeroDark)** — result: FAILED, still deferred (2026-07-28 re-report). Reviewer confirmed the same known brightness condition persists on the segmented control, not worse than commit `de44669`. See the G4 gap entry below for the verbatim re-report and the new "try darker in these two themes" direction.

reported_by (2026-07-28): user

### 7. AeroSegmentedControl — selected-segment label colour
expected: Label legible against the recessed fill in all three themes.
result: FAILED (2026-07-27) — selected label turns near-black. User: "странная идея менять цвет текста на чёрный у выбранного элемента, зачем?". Root cause is worse than the reported symptom: the token used carries an alpha, so the selected label is also rendered semi-transparent (see G3).
result_2026-07-28: PASSED — G3's label fault closed by 19-07 (every segment label now resolves from `colors.onSurface` at full alpha in every state, commit `5533bcc`). Reviewer's Block C report: every label is the same colour, selected or not, and legible on all three themes — not near-black, not washed out, not semi-transparent. Exercised on all three themes.
reported_by: user (2026-07-27 failure), user (2026-07-28 re-verification)
gap: G3 — resolved

### 8. AeroSwitch — rest / toggle / hover / press / disabled matrix, all themes
expected: Per 19-04-PLAN.md task 2 AeroSwitch checklist.
result: PASSED (2026-07-28 reviewer pass, Block D) — exercised in all three themes (AeroBlue, AeroDark, Classic). Reviewer's report: "остальное passed" ("everything else passed"), which the reviewer confirmed covers Blocks A, B, D, E, F and G. The structural rest/toggle/hover/press/disabled states are confirmed independently of the standing G4 brightness caveat on AeroBlue/AeroDark (test 5), which is a separate, already-tracked, deferred condition and is not reopened by this result.
reported_by: user (2026-07-28 reviewer pass)

### 9. AeroSegmentedControl — hover / Tab / Space / N=1 / disabled matrix, all themes
expected: Per 19-04-PLAN.md task 2 AeroSegmentedControl checklist.
result: PASSED (2026-07-28 reviewer pass, Block E) — exercised in all three themes. Covered by the same blanket "остальное passed" report. The standing G4 brightness caveat on AeroBlue/AeroDark (test 6b) is a separate, already-tracked, deferred condition and is not reopened by this result.
reported_by: user (2026-07-28 reviewer pass)

### 10. AeroListItem — VLST-02 selected+hover, focus stroke, display-only non-focusability, disabled
expected: Per 19-04-PLAN.md task 2 AeroListItem checklist. VLST-02 (hover brightens an already-selected row rather than replacing its treatment) is the phase's headline fix and was not reported on.
result: PASSED (2026-07-28 reviewer pass, Block F) — exercised in all three themes, including VLST-02 (selecting then hovering the same row brightens it rather than flattening or replacing the treatment). Covered by the same blanket "остальное passed" report; no defect raised against `AeroListItem` at all in this pass.
reported_by: user (2026-07-28 reviewer pass)

### 11. Classic theme — full matrix for all three components
expected: Classic is called out in 19-04-PLAN.md as the theme that matters most, because its tokens are fully opaque and several defects are invisible on AeroBlue/AeroDark.
result: PASSED (2026-07-28 reviewer pass, Block G) — Classic exercised for the first time in this phase. Reviewer's verbatim report: "у classic всё выглядит нормально, проблемы только у первых двух тем" ("on Classic everything looks fine; the problems are only on the first two themes"). This closes the row that stood pending since 2026-07-27 and independently corroborates G4's root-cause analysis: Classic's darker, more restrained `primary` (`0xFF5C8ABF`) is why it is unaffected by the AeroBlue/AeroDark brightness defect.
reported_by: user (2026-07-28 reviewer pass)

## Round 3 — 2026-07-28 (gap-closure re-sign-off: CR-01, CR-02, WR-01, WR-03, WR-04 — 19-12 Task 2)

Verification performed by the user against the running showcase (`./gradlew :showcase:run`),
built on top of plan 19-12 Task 1's reachability audit (commit `fc04d17`). The developer answered
per block (A through G), not per theme. Two screenshots were attached, both taken on **AeroDark**,
comparing the Selection section's segmented strips against the Buttons section's `AeroButton`
("Save Changes"). No per-theme breakdown was given for AeroBlue or Classic, and none is recorded
here as exercised — per this project's standing rule, an unreported theme is not inferred as
passed just because the block overall reads PASS.

**Developer's response, verbatim:**

```
A - PASS
B - PASS
C - PASS
D - PASS
E - PASS
F - PASS
G - Получилось плохо. Теперь у нас появился новый цвет только для segmented control. SegmentedControl должен быть сделан ПО ОБРАЗУ И ПОДОБИЮ ОБЫЧНОЙ КНОПКИ

по глубине всё норм вроде, всё понятно, что вдавлено и что нет, но надо ЕДИНЫЙ СТИЛЬ
```

**Translation of block G:** "This turned out badly. We've now ended up with a new colour that
belongs only to the segmented control. `AeroSegmentedControl` must be built in the image and
likeness of an ordinary button. Depth-wise it seems fine — it's clear what's pushed in and what
isn't — but we need a UNIFIED STYLE."

### 12. Block A — unselected segment labels (CR-01)
expected: Every unselected segment's label plainly readable against its own fill, on all three themes.
result: PASSED — developer's per-block verdict. Evidence attached is AeroDark only; AeroBlue and
  Classic are not separately confirmed and are not claimed as individually exercised.
reported_by: user

### 13. Block B — depth and darkness judgement (CR-01, the open judgement call)
expected: Three explicit questions — (1) does the strip still read as raised-glass buttons with
  one pushed in; (2) should an unselected segment match a filled `AeroButton` more closely even at
  some cost to label contrast; (3) does the recess still read right against the new, darker base.
result: PASSED for the depth/recess question — developer's own words: "по глубине всё норм вроде,
  всё понятно, что вдавлено и что нет" ("depth-wise it seems fine, it's clear what's pushed in and
  what isn't"). **The recess/pushed-in judgement is explicitly ACCEPTED and is out of scope for
  any follow-up gap.** Question 2 (match the button more closely, even at some contrast cost) is
  answered **YES — match the button** — this is the substance of the Block G failure below, not a
  separate open item. Question 1 (does it still read as raised glass) is subsumed by the same
  answer: it does not currently read as the same "glass button" language the filled `AeroButton`
  uses, which is exactly what Block G calls out.
reported_by: user
gap: G5 (question 2's answer is the fix direction for G5; recess/depth explicitly NOT part of G5)

### 14. Block C — focus-visible on switch, segment, list row (CR-02)
expected: No residual focus cue after a mouse click, on all three components.
result: PASSED — developer's per-block verdict. Per-theme breakdown not given; AeroDark evidenced
  by screenshot, AeroBlue/Classic not separately confirmed.
reported_by: user

### 15. Block D — focus-visible on both button variants (WR-01)
expected: Filled and outlined `AeroButton` no longer keep a residual glow after a click.
result: PASSED — developer's per-block verdict. Per-theme breakdown not given.
reported_by: user

### 16. Block E — hover after the emitter change, including disabled (WR-04)
expected: Hover still lights up switch/segment/row; disabled instances do not.
result: PASSED — developer's per-block verdict. Per-theme breakdown not given.
reported_by: user

### 17. Block F — segment identity under an option-list change (WR-03)
expected: Removing/restoring a segment option never hands one segment's hover/press/focus/animation
  to another (using the "Remove first" / "Restore full list" affordance added by 19-12 Task 1).
result: PASSED — developer's per-block verdict. Per-theme breakdown not given.
reported_by: user

### 18. Block G — anything else / overall impression
expected: Anything wrong not covered by A-F, distinguished from the twice-deferred G4 condition.
result: FAILED (2026-07-28) — NOT the deferred G4 ornament-brightness condition; this is a new,
  distinct defect. `AeroSegmentedControl` has acquired a bespoke colour treatment (the CR-01/WR-12
  raised-fill darken) that exists nowhere else in the library, so it now reads as a different
  visual language from a filled `AeroButton` — a dark navy plate next to the button's light blue.
  Developer's explicit instruction: build the segmented control in the image and likeness of an
  ordinary button, one unified style. Depth/recess is explicitly excluded from this finding (see
  test 13/Block B).
reported_by: user
gap: G5 — new, open

## Summary

total: 18
passed: 15
issues: 3
pending: 0
skipped: 0
blocked: 0

Note: tests 5 and 6 are 2 of the 3 counted "issues" — both are the deferred G4 brightness condition
(6's G3 half is separately resolved and recorded inline as 6a). Test 18 (Block G) is the third —
the new G5 gap from the 2026-07-28 gap-closure round. No row is counted as passed that the reviewer
did not actually report on. Tests 12-17's evidence is per-block (A-F), not per-theme; only AeroDark
is directly evidenced by the attached screenshots, and no other theme is claimed as separately
exercised for those six blocks.

## Gaps

### G1 — AeroListItem row cannot grow, content overflows the pill
status: resolved
routes_to: 19-06 (AeroListItem)
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
resolution: |
  Closed 2026-07-28. 19-06 (commit `08ed471`) replaced the fixed height with
  `.heightIn(min = ROW_MIN_HEIGHT)` and switched the pill/focus Boxes to `matchParentSize()` so
  they grow with the row instead of collapsing to zero height. Confirmed by the 19-08 three-theme
  reviewer pass (UAT test 1, Block A): the long-label-plus-secondary row and the "Sent" row both
  keep their text inside the pill's bounded edge on all three themes; the two adjacent
  pinned-selected pills still read as two separate pills with a gap; a plain single-line row is
  unchanged at its original height.

### G2 — Focus ring shown for pointer-acquired focus (no focus-visible semantics)
status: resolved
routes_to: 19-05 (mechanism), 19-06 (AeroListItem), 19-07 (AeroSegmentedControl)
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
resolution: |
  Closed 2026-07-28 for all three components. 19-05 built a pure, Compose-free
  `FocusVisibility`/`reduce`/`rememberFocusVisible` reducer (deviating from the
  `LocalInputModeManager` wording above for verified platform-behaviour reasons — see
  19-05-SUMMARY.md's Decisions Made) and wired it through `AeroSwitch` (commits `36b291a`,
  `79b11a5`). 19-06 applied the same `state.focusVisible` gate to `AeroListItem`'s in-bounds
  focus stroke (commit `3127f3a`). 19-07 applied it to `AeroSegmentedControl`'s per-segment focus
  stroke (commit `cb64605`). Confirmed by the 19-08 three-theme reviewer pass (Block B, and the
  blanket "остальное passed" report covering tests 8-10): on all three components, a mouse click
  followed by moving the pointer away leaves no focus cue; Tab still draws one; Space/Enter still
  operates the control; hover indication is unchanged.

### G3 — Selected segment: wrong label token and wrong fill direction
status: resolved
routes_to: 19-07 (AeroSegmentedControl)
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
resolution: |
  Closed 2026-07-28. 19-07 (commits `5533bcc`, `93da660`) collapsed every segment label to the
  single `colors.onSurface` content token at full alpha, and declared
  `RECESSED_FILL_DARKEN = 0.20f` applied via `copy(...)` to the recessed style's `fillTop`/
  `fillBottom` strictly after the imported `pressedRecess` transform — matching
  `AeroButtonSurface`'s own `FILLED_FILL_TOP_DARKEN` so the recessed segment lands in the same
  value neighbourhood as a pressed `AeroButton`. Confirmed by the 19-08 three-theme reviewer pass
  (UAT tests 6a and 7, Block C): every label reads the same colour and is legible; exactly one
  segment reads visibly pushed in, comparable to a pressed `AeroButton`; the strip reads darker
  than the `AeroChip` reference row. Reviewer's explicit judgement call on the `0.20f` darken
  magnitude: "в самый раз" (just right) — no follow-up tuning needed.

### G4 — Ornament token derivation blows out on light-primary themes
status: deferred
routes_to: separate foundation session (Phase 16 territory) — user decision 2026-07-27, reaffirmed 2026-07-28
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
re-report_2026-07-28: |
  Reviewer re-exercised AeroDark and AeroBlue during the 19-08 three-theme sign-off and confirmed
  the same condition persists on both `AeroSwitch` and `AeroSegmentedControl`. Verbatim:
  "Светло-голубой цвет в AeroDark и AeroBlue всё ещё слишком яркий и светлый, при добавлении
  белого контура или свечений выглядит вообще вырвиглазно, его надо в этих темах пробовать
  делать темнее" (the light-blue is still too bright and light on AeroDark and AeroBlue; with the
  white contour and glows added it looks outright eye-searing; it should be tried darker in
  these themes). The reviewer did NOT report this as worse than commit `de44669` — it is the
  same known condition, now confirmed by eye a second time, which strengthens the evidence rather
  than reopening scope.

  Newly confirmed by this pass: Classic is unaffected (UAT test 11, "у classic всё выглядит
  нормально, проблемы только у первых двух тем") — this independently corroborates G4's own
  root-cause analysis above, since Classic's `primary` (`0xFF5C8ABF`) stays restrained under the
  same `+30%` lighten formula that blows out AeroBlue's and AeroDark's lighter primaries.

  New direction for the eventual foundation-session fix, additive to the existing `fix_direction`:
  the reviewer's explicit suggestion is to try darker values specifically for AeroDark and
  AeroBlue (not a global change to the derivation formula's magnitude), alongside the existing
  relative-luminance-derivation and `borderSelected = primary` reconsideration.

  Decision: when asked directly whether to reopen G4 or close Phase 19 with it deferred, the
  reviewer chose to keep G4 deferred and close Phase 19. G4 remains routed to the separate
  Phase 16 foundation session; no work on it is authorized inside Phase 19.

### G5 — AeroSegmentedControl's raised fill has drifted into a bespoke colour language, unlike AeroButton
status: resolved
routes_to: AeroSegmentedControl.kt (owned by 19-03/19-07, retuned by 19-10 and WR-12's `5cca3a1`)
  — no library source may change inside 19-12 per its scope fence; needs a new gap-closure plan.
requirement: VSEL-03, VSEL-04
found_2026-07-28: |
  19-12 Task 2 three-theme re-sign-off, Block G. Developer's verbatim report:
  "Получилось плохо. Теперь у нас появился новый цвет только для segmented control.
  SegmentedControl должен быть сделан ПО ОБРАЗУ И ПОДОБИЮ ОБЫЧНОЙ КНОПКИ" ("This turned out badly.
  We've now ended up with a new colour that belongs only to the segmented control.
  `AeroSegmentedControl` must be built in the image and likeness of an ordinary button"). Two
  screenshots attached, both on AeroDark, comparing the Selection section's segmented strips
  (reading as dark navy plates) directly against the Buttons section's `AeroButton` "Save Changes"
  (reading as light blue).
root_cause: |
  Plan 19-10 (CR-01) introduced `RAISED_FILL_TOP_DARKEN`/`RAISED_FILL_BOTTOM_DARKEN` applied to
  `colors.primary` for the segment's raised (unselected) fill, at `0.45f`/`0.61f`. The WR-12 retune
  (commit `5cca3a1`) pushed `RAISED_FILL_TOP_DARKEN` further, to `0.58f`, specifically to clear the
  4.5:1 normal-text contrast floor against `colors.onSurface`. The result reads as a dark navy
  plate, while a filled `AeroButton` at rest reads light blue — two different visual languages for
  what should be the same raised-glass affordance.

  The darkening was chasing contrast against the wrong content token. The segment label is locked
  to `colors.onSurface` (deliberately, per gap G3's resolution above — a state-dependent label
  colour must not be reintroduced). The button's label, by contrast, is not locked to `onSurface`:
  19-10-SUMMARY.md's own key-decisions record that the button's own darken precedent (`0.20f`/
  `0.36f`) was insufficient for the segment specifically "because this component's label is locked
  to the on-surface content token (not near-white button text)" — i.e. the button's label resolves
  to something near-white, its own on-fill content token, which already clears contrast against a
  much lighter fill without any bespoke darkening. Because the segment kept `onSurface` instead of
  adopting the button's content token, the only way left to win contrast was to drive the plate
  dark.
fix_direction: |
  Per the developer's explicit instruction: `AeroSegmentedControl` must be built in the image and
  likeness of an ordinary button — one unified style. The raised segment's fill AND its label
  content token should derive from the same code path `AeroButton`/`AeroButtonSurface` already
  uses, rather than from segmented-control-specific darken constants layered on top of a label
  token (`onSurface`) that was never designed to sit on a bespoke-darkened fill. The recessed/
  selected treatment (which reuses the Phase 17 pressed-button code, `RECESSED_FILL_DARKEN`) is
  unaffected by this gap and must be preserved as-is.
scope_note: |
  Depth/recess magnitude is explicitly OUT of scope for this gap — the developer accepted it in
  the same response ("по глубине всё норм вроде, всё понятно, что вдавлено и что нет" / "depth-wise
  it seems fine, it's clear what's pushed in and what isn't"; recorded as UAT test 13/Block B).
  This gap is about the raised (unselected) fill's colour identity and its label token only.
  Not fixed in 19-12: this plan may touch showcase and UAT record only, per its scope fence.
resolution: |
  Closed 2026-07-28 by a scoped gap-closure fix (commit `c35f883`, todo commit `615f477`), authorized
  by an explicit maintainer decision made when shown this gap's own contrast numbers: **"match the
  button, retire the guard."** The 4.5:1 label-contrast floor introduced by WR-12
  (`AeroSegmentedControlStylesTest.kt`'s `everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio`
  / `MIN_LABEL_CONTRAST`) was itself the reason the raised fill had been driven dark past the
  button's own value — that guard is deleted, not retuned.

  `AeroSegmentedControl.kt`'s raised (unselected) segment fill now derives `colors.primary.darken(...)`
  from the imported `com.mordred.aero.components.buttons.FILLED_FILL_TOP_DARKEN`/
  `FILLED_FILL_BOTTOM_DARKEN` (promoted `private` → `internal` for this cross-package reuse) — the
  exact same constants `resolveButtonStyle` applies to a filled `AeroButton`'s rest fill — instead of
  the segment-only `RAISED_FILL_TOP_DARKEN`/`RAISED_FILL_BOTTOM_DARKEN` (`0.58f`/`0.61f`), both
  deleted along with their KDoc. The raised segment's fill is therefore now byte-identical to
  `AeroButton`'s own rest fill in every scheme (verified: AeroBlue `0xFF3F9CC6`/`0xFF337D9E`,
  AeroDark `0xFF73A2C7`/`0xFF5C819F`, Classic `0xFF4A6E99`/`0xFF3B587A`, top/bottom respectively).

  The segment's label no longer sets an explicit `color` parameter on its `Text` and instead
  inherits ambient `LocalContentColor`, exactly matching `AeroButtonSurface`'s own label mechanism
  (gap G3's fix — an explicit `color = colors.onSurface` — was a correct value on the wrong
  mechanism; G5 removes the re-specified literal). The recessed/selected path
  (`pressedRecess(PRESSED_INNER_SHADOW)` then `RECESSED_FILL_DARKEN`) is structurally untouched —
  only the raised base it composes on top of moved, so its rendered value shifted as an accepted
  side effect; the recess/depth magnitude itself was NOT retuned, per the maintainer's explicit
  sign-off on that reading (UAT test 13/Block B). `recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged`
  (value-level, all three schemes) confirms exactly one segment still reads recessed after the
  raised base moved.

  `AeroButton`'s own appearance is unchanged — only `FILLED_FILL_TOP_DARKEN`'s KDoc was corrected
  (it incorrectly claimed the darken existed for "legible white button text"; the label actually
  resolves to ambient `onBackground`/`onSurface`, never white). That component's own sub-4.5:1
  label contrast (1.70–3.98:1 measured across the three themes) is tracked separately as its own
  todo (`.planning/todos/pending/2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor.md`),
  not fixed here — fixing it would be a visual change to a Phase-17 signed-off component requiring
  fresh sign-off of its own.

  `./gradlew build` and the full `*AeroSegmentedControl*`/`*AeroButton*` test suites are green.
  This closure is authorized by the maintainer's explicit decision recorded above, not by a fresh
  human visual re-verification pass against the running showcase — no new screenshot-based
  sign-off was performed for this specific fix.

## Notes

- **Reviewer question, not a gap (2026-07-28):** the reviewer asked whether it is expected that
  clicking an "adjacent selected" `AeroListItem` row (the two pinned-selected demo rows added in
  19-04 Task 1 for the pill-adjacency backstop) produces no visual change. Resolved: YES, expected
  — verified against source. Those two rows at `ListSection.kt:68-69` are
  `AeroListItem(text = "Pinned selected — first"/"second", onClick = {}, selected = true)`:
  `selected` is a hardcoded literal, not state-bound, and `onClick` is an empty lambda. They exist
  solely so two selected pills read as two, with a visible gap between them (UAT test 3). Live
  selection switching is demonstrated separately by the "Inbox" / "Sent" / "Drafts" group at
  `ListSection.kt:49-55`, which is bound to `selectedIndex` and does change on click. This is a
  clarifying question with a resolution, not a defect — it routes nowhere and required no code
  change.
- Phase 19's sign-off gate is **APPROVED** as of 2026-07-28 (19-08 Task 2). Gaps G1, G2 and G3 are
  confirmed closed by eye across all three themes. G4 remains deferred by explicit, twice-made
  user decision and is out of Phase 19's scope; it routes to a separate Phase 16 foundation
  session. No defect from this pass routes back to 19-05, 19-06 or 19-07.
- Plans 19-01, 19-02 and 19-03 are complete and their test suites are green; plans 19-05, 19-06
  and 19-07 closed G1/G2/G3 with unit-tested mechanisms and are now human-confirmed end to end.
- Execution environment note: subagent dispatch for this phase must use `run_in_background: true`.
  Synchronous dispatch was aborted twice mid-plan at 22 and 39 minutes; background dispatch
  completed a 26-minute run. See the project memory entry `subagent-runtime-ceiling`.
- **19-12 gap-closure round (2026-07-28):** CR-01, CR-02, WR-01, WR-03 and WR-04 were re-verified
  against the live showcase. Blocks A-F all PASSED per the developer's explicit per-block verdict
  (evidenced on AeroDark; AeroBlue and Classic were not separately broken out and are not claimed
  as individually exercised for these six blocks). Block B's depth/recess judgement is explicitly
  ACCEPTED. Block G surfaced a new, distinct defect — G5 — routed above; it is NOT the deferred G4
  ornament-brightness condition. Phase 19's gate is therefore **NOT closed** as of this round: G5
  is open and un-fixed (19-12's scope fence forbids touching library source). No approval was
  recorded or inferred beyond what the developer explicitly stated.
- **G5 gap-closure fix (2026-07-28, commit `c35f883`):** the raised (unselected) segment fill and
  label mechanism were unified with `AeroButton`'s own `resolveButtonStyle`/`AeroButtonSurface`
  source of truth, per the maintainer's explicit "match the button, retire the guard" decision made
  after being shown this gap's own contrast measurements. The 4.5:1 label-contrast guard that had
  forced the segment's raised fill dark is deleted, not retuned; the recessed/selected depth
  treatment is unchanged. `AeroButton`'s own sub-4.5:1 label contrast is tracked separately (todo
  committed `615f477`) and was NOT fixed as part of this closure — only its KDoc's inaccurate
  "white button text" claim was corrected. This closure rests on the maintainer's explicit decision
  plus green `./gradlew build` and the full `*AeroSegmentedControl*`/`*AeroButton*` suites; no fresh
  human visual re-verification pass (screenshot-based sign-off) was performed against the running
  showcase for this specific fix. G4 remains separately deferred and out of Phase 19's scope, as
  before. See the G5 gap entry above for the full resolution.
</content>
