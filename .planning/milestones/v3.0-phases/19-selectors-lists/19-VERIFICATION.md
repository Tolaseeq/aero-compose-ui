---
phase: 19-selectors-lists
verified: 2026-07-28T15:58:00Z
status: passed
score: 8/8 must-haves verified
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 4/8
  gaps_closed:
    - "VSEL-03: AeroSegmentedControl's selected segment appears recessed and the control is legible (all labels readable) in all three themes (CR-01)"
    - "VSEL-02 / VSEL-04 / VLST-03: focus ring stays suppressed for pointer-acquired focus and does not reappear on a later mouse click after an intervening focus loss (CR-02)"
  gaps_remaining: []
  regressions: []
---

# Phase 19: Selectors + Lists Verification Report

**Phase Goal:** `AeroSwitch` and `AeroSegmentedControl` gain their first-ever hover/press/focus states and Aero volume, and `AeroListItem`'s selection highlight is finally clipped to a proper Aero pill with a visible focus state.

**Verified:** 2026-07-28
**Status:** passed
**Re-verification:** Yes — after gap closure (this file supersedes the prior `gaps_found` report; both of its listed gaps, CR-01 and CR-02, are re-verified below as source-confirmed closed, with no regressions found in the previously-passed truths)

## Re-Verification Context

The prior verification pass found two blocker gaps, both confirmed by direct source trace:

- **CR-01** — `AeroSegmentedControl`'s raised/unselected fill was the literal, undarkened `AeroSurfaceStyle.rest()` primary-derived fill, making the `onSurface` label ~1.04:1 (effectively invisible) on AeroDark.
- **CR-02** — `FocusVisibility.reduce`'s `Unfocus` branch reset `hovered` to `false` alongside the focus-derived fields, letting a mouse-click focus ring reappear after an intervening Tab-away/Tab-back or window-deactivation cycle, across all three components sharing the reducer.

Since that verification, two further executed rounds ran: 19-09/19-10/19-11 (CR-01, CR-02, WR-01, WR-03, WR-04 gap closure, re-reviewed clean in `19-REVIEW.md`), then a WR-12 contrast retune, then a maintainer-directed gap G5 (`c35f883`) that unified `AeroSegmentedControl`'s raised fill and label mechanism with `AeroButton`'s own `resolveButtonStyle`/`AeroButtonSurface` source of truth, deleting the WR-12 4.5:1 contrast guard by explicit decision ("match the button, retire the guard"). This report re-verifies both original gaps directly against the current source (not from SUMMARY.md or UAT narrative) and checks that the G5 rewrite did not reopen either one or regress the four previously-passed truths.

## Goal Achievement

### Observable Truths (mapped to phase requirement IDs)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | VSEL-01: AeroSwitch shows recessed track groove + raised glossy/shadowed thumb | ✓ VERIFIED | `AeroSwitch.kt:122` (`.aeroGroove(resolveSwitchGrooveStyle(...))`), `132` (`.aeroThumbSurface(resolveSwitchThumbStyle(...))`), `152-156` (`THUMB_DROP_SHADOW`). `resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle` (166-207) build distinct recessed/raised styles with gloss and shadow. Regression check: unchanged since prior pass, `AeroSwitchStylesTest` green. |
| 2 | VSEL-02: AeroSwitch has working hover/press/focus for the first time, with no residual focus cue after a mouse click | ✓ VERIFIED | Mechanism at `AeroSwitch.kt:107-116` gates the focus glow on `state.focusVisible && enabled` (separate from the hover glow on `state.hovered && enabled`). CR-02 fix confirmed at `InteractionStates.kt:129`: `is FocusInteraction.Unfocus -> FocusVisibility(hovered = hovered)` — preserves the pointer-derived field, resets only focus-derived fields, closing the exact defect the prior verification traced. `FocusVisibilityTest.mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed` (10 tests, all green) and the composed `AeroSwitchFocusVisibleWiringTest` (1 test, green) both exercise the six-interaction repro against real state/a real component. |
| 3 | VSEL-03: AeroSegmentedControl's selected segment reads recessed (reused pressed-button code) AND the control is legible in all three themes | ✓ VERIFIED | `resolveSegmentStyle` (`AeroSegmentedControl.kt:271-296`): raised base now darkens `colors.primary` by imported `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` (`0.20f`/`0.36f`, same constants and values `AeroButtonSurface` applies to a filled button's rest fill) — the prior verification's "base is literal, undarkened `rest()`" defect no longer holds. Recessed selection is still `base.pressedRecess(PRESSED_INNER_SHADOW)` (both imported from `AeroButtonSurface`, unmodified) then `.darken(RECESSED_FILL_DARKEN = 0.20f)`. `recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged` (all 3 schemes, green) proves the recessed segment stays strictly darker than the raised one after the base moved — the depth relationship survives. Human sign-off (19-UAT.md, verbatim "одобряю, да, всё хорошо") confirms legibility and that it now reads as the same "glass button" object rather than a bespoke plate. |
| 4 | VSEL-04: AeroSegmentedControl gains hover and focus for the first time, with no residual focus cue after a mouse click | ✓ VERIFIED | Per-segment `selectable(...)` (`AeroSegmentedControl.kt:158-165`) plus `segState.focusVisible`-gated in-bounds stroke (173-189), sharing the now-fixed `InteractionStates` reducer. `AeroSegmentedControlSemanticsTest`/`AeroSegmentedControlSourceTest` green. |
| 5 | VLST-01: AeroListItem selection highlight clips to a rounded pill with gradient + rim, and content no longer overflows it | ✓ VERIFIED | `AeroListItem.kt:113-121`, pill via `aeroSurface(pillStyle, RoundedCornerShape(PILL_CORNER_RADIUS))` on a `matchParentSize().padding(vertical = PILL_VERTICAL_INSET)` Box. G1 fix confirmed: `.heightIn(min = ROW_MIN_HEIGHT)` (line 93, a floor not a ceiling) replaces the old fixed `.height(36.dp)`, so pill and row both grow with content. `AeroListItemLayoutTest` (5 tests, green) and `AeroListItemStylesTest` (9 tests, green). |
| 6 | VLST-02: hover remains visible on an already-selected row (combine, not suppress) | ✓ VERIFIED | `resolveListItemPillStyle`'s base-then-transform (`AeroListItem.kt:218-237`): `base` resolves from `selected` first, `hoverLighten()` composes on top when hovered — unchanged since prior pass. |
| 7 | VLST-03: AeroListItem has a focus visual, with no residual focus cue after a mouse click | ✓ VERIFIED | In-bounds stroke at `AeroListItem.kt:122-139`, gated on `state.focusVisible && enabled && onClick != null`, now backed by the fixed reducer (see truth 2). |
| 8 | VLST-04: newly-hover-wired components reuse `Modifier.hoverable` + `collectIsHoveredAsState`, not custom pointer tracking | ✓ VERIFIED | `AeroSwitch.kt` (hover emitted by `toggleable`, no explicit `.hoverable` left per WR-04), `AeroSegmentedControl.kt:123` (outer strip `.hoverable`), `AeroListItem.kt:110` (display-only path `.hoverable`); `InteractionStates.kt:74` (`collectIsHoveredAsState`). No `pointerInput`-based hover tracking found in any of the four files (grep confirms zero matches). |

**Score:** 8/8 truths verified (0 present-behavior-unverified)

### Gap Closure Verification (from prior `gaps_found` report)

| Gap | Prior Status | Current Status | Evidence |
|-----|-------------|-----------------|----------|
| CR-01 (VSEL-03 label legibility) | FAILED | ✓ RESOLVED | Raised base now darkened via imported `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN`; matches `AeroButton`'s own accepted (Phase 17) contrast precedent exactly, byte-identical fill values across all three schemes (verified in 19-UAT.md G5 resolution: AeroBlue `0xFF3F9CC6`/`0xFF337D9E`, AeroDark `0xFF73A2C7`/`0xFF5C819F`, Classic `0xFF4A6E99`/`0xFF3B587A`). Human-confirmed legible by eye after being warned contrast is lower than the interim dark-plate fix. |
| CR-02 (VSEL-02/04/VLST-03 focus-ring reappearance) | FAILED | ✓ RESOLVED | `InteractionStates.kt:129` preserves `hovered` on `Unfocus`; `FocusVisibilityTest`'s named regression case and the composed `AeroSwitchFocusVisibleWiringTest` both pass against the exact six-interaction repro the prior verification traced by hand. |

### Regression Check on Previously-Passed Truths

VSEL-01, VLST-01, VLST-02 and VLST-04 (all ✓ SATISFIED in the prior pass) were re-checked at file level for continued existence, substance and wiring — no regression found. `AeroSwitch.kt`'s groove/thumb resolvers, `AeroListItem.kt`'s pill/base-then-transform mechanism, and the `hoverable`/`collectIsHoveredAsState` pattern are all unchanged in mechanism from the prior pass; the full relevant test suites (`AeroSwitchStylesTest`, `AeroListItemLayoutTest`, `AeroListItemStylesTest`, `AeroSegmentedControlStylesTest`, `AeroSegmentedControlSemanticsTest`, `AeroSegmentedControlSourceTest`, `AeroButtonSurfaceSourceTest`, `FocusVisibilityTest`, `AeroSwitchFocusVisibleWiringTest`) were executed fresh in this pass and are all green (0 failures, 0 errors).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/.../selection/AeroSwitch.kt` | Recessed groove, raised thumb, hover/press/focus, no residual focus cue | ✓ VERIFIED | Exists, substantive, wired; CR-02 no longer reachable. |
| `library/.../selection/AeroSegmentedControl.kt` | Recessed selection via reused pressed-button code, legible raised segments, hover/focus | ✓ VERIFIED | Exists, substantive, wired; CR-01 and CR-02 both closed; unified with `AeroButton`'s own fill/label mechanism (gap G5). |
| `library/.../list/AeroListItem.kt` | Clipped pill, hover+selection combine, focus visual, content no longer overflows | ✓ VERIFIED | Exists, substantive, wired; row height is a floor (`heightIn(min=...)`), pill/focus boxes use `matchParentSize()`. |
| `library/.../common/InteractionStates.kt` | Shared focus-visible reducer | ✓ VERIFIED | `Unfocus` branch now preserves `hovered`; consumed correctly by all 3 components. |
| `library/.../buttons/AeroButtonSurface.kt` | Source of `pressedRecess`/`PRESSED_INNER_SHADOW`/darken constants, now also `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` promoted `internal` for cross-package reuse | ✓ VERIFIED | Confirmed `internal` (not `private`), consumed by `AeroSegmentedControl.kt` import; `AeroButton`'s own appearance/behaviour unchanged (only a KDoc correction). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `AeroSegmentedControl.resolveSegmentStyle` | `AeroButtonSurface.pressedRecess`/`PRESSED_INNER_SHADOW`/`FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` | import + call (`AeroSegmentedControl.kt:32-34,283-285`) | ✓ WIRED | Genuine cross-package reuse for both the recess transform and (new, gap G5) the raised fill darken — same constants, same values as the button's own rest fill. |
| `AeroSwitch`/`AeroSegmentedControl`/`AeroListItem` | `InteractionStates.rememberAeroInteractionState`/`rememberFocusVisible` | call sites at `AeroSwitch.kt:81`, `AeroSegmentedControl.kt:139`, `AeroListItem.kt:81` | ✓ WIRED | All three share the one (now-fixed) reducer. |
| Hover/press/focus state | Pill/groove/segment fill resolution | `state.hovered`/`state.pressed`/`state.focusVisible` into `resolveListItemPillStyle`/`resolveSwitchGrooveStyle`/`resolveSwitchThumbStyle`/`resolveSegmentStyle` | ✓ WIRED | Unchanged from prior pass, still flows to `aeroSurface`/`aeroGroove`/`aeroThumbSurface` draw calls. |

### Requirements Coverage

| Requirement | Description (abridged) | Status | Evidence |
|---|---|---|---|
| VSEL-01 | AeroSwitch groove + raised glossy/shadowed thumb | ✓ SATISFIED | Truth #1 |
| VSEL-02 | AeroSwitch hover/press/focus, first time | ✓ SATISFIED | Truth #2 |
| VSEL-03 | AeroSegmentedControl recessed selection via reused pressed-button code, legible | ✓ SATISFIED | Truth #3 |
| VSEL-04 | AeroSegmentedControl hover + focus, first time | ✓ SATISFIED | Truth #4 |
| VLST-01 | AeroListItem selection clips to rounded pill w/ gradient + rim | ✓ SATISFIED | Truth #5 |
| VLST-02 | Hover visible on selected row, combined not suppressed | ✓ SATISFIED | Truth #6 |
| VLST-03 | AeroListItem focus visual | ✓ SATISFIED | Truth #7 |
| VLST-04 | Reuse `hoverable` + `collectIsHoveredAsState`, no custom pointer tracking | ✓ SATISFIED | Truth #8 |

All 8 requirement IDs (VSEL-01..04, VLST-01..04) are marked `[x]` in `.planning/REQUIREMENTS.md` and map exactly to this phase's `Requirements:` line in ROADMAP.md. No orphaned requirements found. `VLST-F01` (mirrored-reflection layer removal) is an explicitly deferred **Future Requirement**, not part of this phase's scope, and is correctly excluded.

### Behavioral Spot-Checks

The full relevant test suites were executed fresh for this verification (not merely cited from a prior run):

```
./gradlew :library:test --tests "com.mordred.aero.components.selection.*" \
  --tests "com.mordred.aero.components.buttons.*" \
  --tests "com.mordred.aero.components.list.*" \
  --tests "com.mordred.aero.components.common.*"
```

Result: exit code 0. Per-class result counts (`tests`/`failures`/`errors`), sampled from the generated XML reports:

| Test class | tests | failures | errors |
|---|---|---|---|
| `AeroSegmentedControlStylesTest` | 9 | 0 | 0 |
| `AeroButtonSurfaceSourceTest` | 4 | 0 | 0 |
| `AeroListItemLayoutTest` | 5 | 0 | 0 |
| `AeroSwitchFocusVisibleWiringTest` | 1 | 0 | 0 |
| `FocusVisibilityTest` | 10 | 0 | 0 |

All other classes in the four packages (`AeroSwitchStylesTest`, `AeroSwitchSemanticsTest`, `AeroSwitchSourceTest`, `AeroSegmentedControlSemanticsTest`, `AeroSegmentedControlSourceTest`, `AeroListItemSourceTest`, `AeroListItemStylesTest`, `AeroButtonSemanticsTest`, `AeroButtonStylesTest`, `AeroButtonTest`, `AeroOutlinedButtonStylesTest`) generated a report with no failure/error markers; no `failures="[1-9]"` grep hit across any of the produced XML reports.

### Probe Execution

No `scripts/*/tests/probe-*.sh` conventions or phase-declared probes found for phase 19 (a Compose UI component phase, not a migration/tooling phase). Skipped.

### Anti-Patterns Found

No `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` debt markers found in `AeroSwitch.kt`, `AeroSegmentedControl.kt`, `AeroListItem.kt`, `InteractionStates.kt`, or `AeroButtonSurface.kt` (grep, case-insensitive).

One design note, not a blocker: closing gap G5 deleted the value-level `everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio` contrast-ratio guard entirely (both its original 3.0-floor and its WR-12 4.5-floor incarnations) rather than retuning it, per an explicit maintainer decision recorded in 19-UAT.md ("match the button, retire the guard"). No replacement contrast regression test exists for `AeroSegmentedControl`'s label — but this is consistent with the rest of the library: `AeroButton` (the component now being matched byte-for-byte) has never had one either, and its own sub-4.5:1 contrast is tracked in a separate, filed todo (`.planning/todos/pending/2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor.md`). This does not block the phase goal — VSEL-03's requirement is recessed-segment reuse and overall legibility (human-confirmed), not WCAG conformance, and no ROADMAP success criterion for phase 19 references a contrast floor. Recorded here for traceability, not as a gap.

The ~10 lower-severity WARNING/INFO items carried forward unchanged in `19-REVIEW.md` (WR-02, WR-05 through WR-11, IN-01 through IN-05 — e.g. disabled `AeroSegmentedControl` not dimming, no `require()` guard for "exactly one selected", dead code, wildcard imports) were reviewed against the phase goal and do not block it: none of them concern the four Success Criteria (groove/thumb rendering, hover/press/focus, recessed-segment legibility, pill clipping/hover-combine/focus visual), all were already known before the human sign-off gate passed, and none were re-flagged as blocking by the maintainer's final "одобряю" approval.

### Human Verification Required

None. 19-UAT.md's gate is APPROVED as of 2026-07-28, including the final G5 fix ("одобряю, да, всё хорошо"). G4 (ornament-token brightness on light-primary themes) remains explicitly, twice-deferred out of this phase's scope by maintainer decision, routed to a separate Phase 16 foundation session — not re-litigated here.

### Gaps Summary

None remaining. Both blocker gaps from the prior `gaps_found` verification (CR-01, CR-02) are confirmed closed directly against current source, with passing regression tests exercising the exact repro sequences the prior verification traced by hand. The subsequent G5 gap (segmented control drifting into a bespoke colour language, found by the human sign-off after CR-01's WR-12 retune) is also confirmed closed: the raised fill and label mechanism are now unified with `AeroButton`'s own source of truth, the depth/recess invariant survives (verified by a passing luminance-comparison test), and the human gave final approval. No regression was found in the four truths that already passed the prior verification round.

---

_Verified: 2026-07-28_
_Verifier: Claude (gsd-verifier)_
