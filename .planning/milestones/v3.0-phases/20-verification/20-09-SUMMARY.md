---
phase: 20-verification
plan: 09
subsystem: ui
tags: [compose-desktop, theme, wcag-contrast, aero-color-scheme]

# Dependency graph
requires:
  - phase: 20-verification
    plan: 04
    provides: "resolveLabelColor (retired by this plan) and the AeroDark recessed-segment 4.079 exception it authorized (also retired by this plan — see 'Voided Exception')"
  - phase: 20-verification
    plan: 08
    provides: "the maintainer's checkpoint rule this plan implements: 'per theme, ONE text colour, not per element, not per state'; ordering only, no artifact dependency"
provides:
  - "AeroColorScheme.labelOnFilledSurface / labelOnOutlinedSurface — two scheme-level, trailing-and-defaulted label tokens replacing per-call-site resolveLabelColor"
  - "VER10OneLabelColorPerThemeTest — the pre-change measurement: worst-case WCAG ratio for pure black/white across every fill in each scheme, proving no single combined colour serves both surface polarities"
  - "AeroButtonSurface and AeroSegmentedControl both reduced to reading the scheme token; the old per-fill computation is gone from both call sites"
  - "VER08SegmentLabelFlipTest strengthened from 'changes at most once' to true invariance across selection animation, hover, press, focus and disabled"
  - "AeroButtonContrastRegressionTest re-derived from scratch against the new mechanism, encoding exactly three surviving below-floor exceptions"
affects: ["20-07 (human three-theme sign-off — SHW-16 still open; the AeroDark/AeroBlue recessed-segment legibility judgment it must make now concerns a DIFFERENT authorized value (3.218-4.228, black) than the one 20-08's own SUMMARY records (4.079, white) — that old number is void, see below)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Surface-polarity-scoped scheme tokens (labelOnFilledSurface / labelOnOutlinedSurface) rather than one combined per-scheme colour — measured proof that a single winner exists only by accepting AeroBlue/AeroDark's opaque fills at 1.24-1.42:1 with black, which VER10 shows is worse than splitting by polarity"
    - "Luminance-derived fallback (defaultLabelColorForSurface) for both new tokens, following ornamentOverride's trailing-and-defaulted source-compatibility convention exactly"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt
    - library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt

key-decisions:
  - "Label colour is chosen by SURFACE POLARITY (opaque filled vs. ~15%-alpha outlined), not by scheme alone — AeroBlue/AeroDark contain both polarities and no single colour serves both (opaque wants dark, outlined wants light; Classic's primary is dark so both polarities coincide on white)."
  - "The old resolveLabelColor / labelContrastRatio / LABEL_CANDIDATE_DARK|LIGHT mechanism was RETIRED, not narrowed — leaving it live alongside the new scheme tokens would have left two competing mechanisms in the codebase."
  - "20-04's authorized 4.079/white recessed-segment exception is VOID. It was derived under the retired per-fill algorithm; re-derived from scratch under the new mechanism, AeroBlue/AeroDark's recessed segment now uses labelOnFilledSurface=Black and measures a DIFFERENT worst case (AeroBlue 3.218/3.853, AeroDark 3.538/4.228, fillTop only), not a variant of the old number."
  - "Three exceptions survive re-derivation, none inherited: (1) recessed segment fillTop, AeroBlue/AeroDark, black token, 3.218-4.228; (2) filled/raised-segment fillTop on hover, Classic, white token, 4.455 (misses 4.5 by 0.045); (3) disabled surfaces, all schemes, WCAG-1.4.3-exempt (AeroBlue 2.87, AeroDark 2.37, Classic 9.49)."
  - "MIN_LABEL_CONTRAST stays 4.5f in both test files, unchanged (D-13)."

patterns-established:
  - "Every enumerated exception carries its own named test, its own measured regression-bound constant, and a KDoc justification citing why the alternative (flip the colour) would reintroduce state-dependence — the pattern this plan's must_haves required and future contrast exceptions should follow."

requirements-completed: [VER-06]
# SHW-16 is deliberately NOT marked complete — see 'Requirements Note' below, same convention as 20-08.

coverage:
  - id: D1
    description: "Pre-change measurement: worst-case WCAG ratio for pure black and pure white across every fill an on-fill label lands on (filled/outlined button rest/hover/press/disabled, segment raised/recessed/hover/press), all three schemes — proving no single combined colour clears 4.5:1 everywhere and that outlined's near-black composited fill is what makes black catastrophic (1.24-1.42:1) wherever it appears."
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt#measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInAeroBlue, #measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInAeroDark, #measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInClassic, #printsFullPerSchemePerFillContrastTable, #fixtureBlackOnWhiteMeasuresWellAboveTheFloor, #fixtureMidGreyOnMidGreyMeasuresBelowTheFloor"
        status: pass
    human_judgment: false
  - id: D2
    description: "AeroColorScheme gains two trailing, defaulted, luminance-fallback label tokens (labelOnFilledSurface, labelOnOutlinedSurface); AeroButtonSurface's Text and AeroSegmentedControl's segment Text both read the token instead of computing a colour from their own animating fill; source guards confirm no call site outside the theme layer still computes one."
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt, library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt, library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt"
        status: pass
    human_judgment: false
  - id: D3
    description: "VER08 strengthened from 'the label changes at most once' to true invariance — never changes across the selection animation, hover, press, focus or disabled; its own D-08 fail-then-pass proof updated to match the stronger claim."
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt"
        status: pass
    human_judgment: false
  - id: D4
    description: "AeroButtonContrastRegressionTest re-derived from scratch against the new mechanism: filled/outlined button and raised/recessed segment measured on both fill stops in all three schemes, with exactly three named below-floor exceptions (recessed segment fillTop AeroBlue/AeroDark, Classic filled-hover fillTop, disabled surfaces), each carrying its own measured regression-bound constant."
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt#filledButtonRestHoverPressClearTheFloorOnBothStopsInAllThreeThemes, #outlinedButtonRestHoverPressClearTheFloorOnBothStopsInAllThreeThemes, #raisedSegmentRestAndHoverClearTheFloorOnBothStopsInAllThreeThemes, #recessedSegmentClearsTheFloorInClassic, #recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException, #classicFilledHoverIsTheOneAuthorizedLightTokenException, #disabledSurfacesAreExemptFromTheContrastFloorPerWcag143"
        status: pass
    human_judgment: false
  - id: D5
    description: "Whether AeroBlue/AeroDark's re-derived recessed-segment exception (black label, 3.218-4.228, replacing the void 4.079/white number) reads legibly by eye is a perceptual judgment this plan does not attempt."
    verification: []
    human_judgment: true
    rationale: "The measurement is now gated fresh by AeroButtonContrastRegressionTest under the new mechanism; whether a number below 4.5:1 is visually acceptable is a different question from whether it is correctly measured, and stays with 20-07's three-theme sign-off exactly as 20-04's original (now-void) exception did."

duration: n/a (closeout for a completed autonomous plan; no live task execution in this session)
completed: 2026-07-29
status: complete
---

# Phase 20 Plan 09: One Label Colour Per Theme, Resolved by Surface Polarity — Summary

> **⚠ REVISED SAME DAY (2026-07-29) — read the "Amendment" section near the end before trusting
> the black/white polarity table below as current.** The surface-polarity split described in this
> document (dark label on AeroBlue/AeroDark's opaque fills, light on outlined) was shipped, then the
> maintainer reviewed it running live across all three themes and rejected it on appearance. Their
> final decision reverts `labelOnFilledSurface` to White for AeroBlue/AeroDark too — matching
> Classic on both tokens. Consequently the "Three Surviving Exceptions" section's exception 1 (black
> token, recessed segment, 3.218-4.228) below is **itself now void** — see the Amendment for its
> replacement. Everything else in this document (the two-token scheme-level mechanism, the retained
> `VER10OneLabelColorPerThemeTest` measurement, `MIN_LABEL_CONTRAST` staying 4.5f) still stands.

**`AeroColorScheme` gains `labelOnFilledSurface`/`labelOnOutlinedSurface`, resolved once per scheme by surface polarity rather than recomputed per call site from an animating fill; both `AeroButton` and `AeroSegmentedControl` labels now read the fixed token in every state; 452 -> 467 tests, 0 failures, and 20-04's 4.079/white recessed-segment exception is VOID — re-derived from scratch as a different value (3.218-4.228, black).**

## Performance

- **Tasks:** 3/3 (all three previously executed and committed; this session is a closeout writing SUMMARY/STATE/ROADMAP only, no re-execution)
- **Files created:** 1 (`VER10OneLabelColorPerThemeTest.kt`)
- **Files modified:** 8 — 3 under `library/src/main` (`AeroColorScheme.kt`, `AeroButtonSurface.kt`, `AeroSegmentedControl.kt`), 5 under `library/src/test`
- **Suite:** 452 -> 467 tests, 0 failures (+15 tests: VER10's 6, plus growth in `AeroButtonContrastRegressionTest`, `AeroButtonSurfaceSourceTest`, `AeroSegmentedControlSourceTest`, `AeroColorSchemeTest` (24 -> 26 fields), `VER08SegmentLabelFlipTest`)

## Origin — why this plan exists

During the 20-08 checkpoint the maintainer observed that in AeroBlue and AeroDark the label colour jumped between black and white depending both on which element drew it and on that element's interaction state, while Classic was stable (white everywhere). Their directive: per theme ONE text colour, not per element and not per state, with rare justified exceptions where it would otherwise be illegible or where interactive text colour is genuinely useful.

**Root cause.** 20-04's `resolveLabelColor(fillTop, fillBottom, backdrop)` was called with `style.fillTop`/`style.fillBottom`, and `resolveSegmentStyle` folds hover (`hoverLighten()`), press (`effectiveProgress = 1f`) and the selection animation into that fill. The label was therefore recomputed from a fill that moved with interaction and flipped whenever it crossed the contrast midpoint. 20-04 correctly fixed the at-rest contrast defect; the state-dependence was an unnoticed consequence, not a design decision.

## Accomplishments

1. **Task 1 — Measure first (`3b55237`).** `VER10OneLabelColorPerThemeTest` enumerates every fill an on-fill label actually lands on (filled/outlined button rest/hover/press/disabled, segment raised/recessed/hover/press) across all three schemes, from the real `resolveButtonStyle`/`resolveSegmentStyle` outputs — no production change. The measurement established that **AeroBlue and AeroDark contain two opposite surface polarities and no single colour serves both**:
   - Opaque filled surfaces — `primary` is a light blue (`0xFF4FC3F7` AeroBlue, `0xFF90CAF9` AeroDark), so the fill is light and wants dark text.
   - Outlined surfaces — the fill is ~0.15 alpha of `primary` composited down onto the dark backdrop, so the surface is dark and wants light text. Black there measures 1.24-1.42:1: illegible.
   Classic is stable today precisely because its `primary` is dark, so both polarities want white. The combined single-winner-per-scheme candidate this file computes (White for all three, since outlined's near-black stop makes Black catastrophic wherever it appears) is deliberately **not** what shipped — see Task 2.

2. **Task 2 — Add the scheme-level tokens (`15cf312`).** `AeroColorScheme` gains `labelOnFilledSurface` and `labelOnOutlinedSurface`, both trailing and defaulted exactly like `ornamentOverride`, with a luminance-derived fallback (`defaultLabelColorForSurface`) so a custom scheme built via `copy()` without them still resolves sensibly instead of throwing or rendering invisible text. The maintainer's decision (2026-07-29) rejected the single combined winner from Task 1 in favor of splitting by polarity:

   | Scheme   | `labelOnFilledSurface` | `labelOnOutlinedSurface` |
   |----------|------------------------|---------------------------|
   | AeroBlue | Black                  | White                     |
   | AeroDark | Black                  | White                     |
   | Classic  | White                  | White                     |

   `AeroButtonSurface`'s `Text` and `AeroSegmentedControl`'s segment `Text` are both reduced to reading the appropriate token; the per-fill computation stops being consulted at either call site. The old `resolveLabelColor` / `labelContrastRatio` / `LABEL_CANDIDATE_DARK|LIGHT` were **retired**, not narrowed — leaving them live would have left two competing mechanisms. Source guards (`AeroButtonSurfaceSourceTest`, `AeroSegmentedControlSourceTest`) and the field-count structural test (`AeroColorSchemeTest`, 24 -> 26 fields) gate the new mechanism.

3. **Task 3 — Gate the invariance, re-derive the contrast table (`34ff18f`).** `VER08SegmentLabelFlipTest` strengthened from "the label changes at most once across the selection animation" to true invariance — never changes, across the animation and across hover/press/focus/disabled — with its D-08 fail-then-pass proof updated to match. `AeroButtonContrastRegressionTest` was re-derived from scratch (not narrowed from the 20-04 version) against every fill the new fixed-per-scheme label lands on. `MIN_LABEL_CONTRAST` stays `4.5f` in both test files, never lowered (D-13).

## The Three Surviving Exceptions — re-derived from scratch, NOT inherited from 20-04

> **⚠ Exception 1 below is ITSELF now void** — see the "Amendment" section near the end of this
> document. It described the black-token recessed-segment exception under the surface-polarity
> rule; that rule was reverted the same day. Left here as history rather than deleted.

1. **Recessed (selected) segment `fillTop`, dark token, AeroBlue and AeroDark.** AeroBlue rest/press `3.218`, hover `3.853`; AeroDark rest/press `3.538`, hover `4.228` (`fillBottom` always clears comfortably — 4.563-5.910). Flipping to a light label here would reintroduce state-dependent colour — the exact defect this plan removes, since every other opaque fill in these two schemes (filled button, raised segment) stays black. Retuning `RECESSED_FILL_DARKEN` to rescue this is forbidden by that constant's own KDoc (the maintainer's 19-08 acceptance of the recess/depth reading).

   **This VOIDS 20-04's authorized `4.079`/white exception.** That number was derived under the retired per-fill algorithm and depended on a rescue mechanism that no longer exists — it is not a variant or an ancestor of the current exception, it describes code that has been deleted. Anyone consulting 20-04's or 20-08's SUMMARY for "the" AeroDark recessed-segment number must use the numbers above instead.

2. **Filled button / raised segment `fillTop` on hover, light token, Classic.** Measures `4.455385` against the `4.5` floor — misses by `0.045`. Classic is this plan's own reference case ("white label everywhere, independent of element and state"), so the exception is taken rather than flipping Classic's label to black for the sake of one hover fill. The raised segment shares this exact fill formula with the filled button (the earlier gap-G5 unification), so one exception covers both consumers.

3. **Disabled surfaces, all schemes.** Exempt from WCAG 1.4.3 by the standard itself, not a defect. AeroBlue `2.87`, AeroDark `2.37` carry regression bounds (not floor requirements) so this exemption cannot silently mask a future fill change making disabled text meaningfully worse; Classic `9.49` clears the floor anyway.

No sub-floor case beyond these three exists — verified by exhaustive sweep over filled/outlined rest-hover-press plus segment raised/recessed rest-hover across all three schemes (`AeroButtonContrastRegressionTest`'s full assertion set).

## Task Commits

Each task was committed atomically (all three previously executed and committed prior to this closeout):

1. **Task 1: Measure first — what a single label colour costs in each scheme** — `3b55237` (test)
2. **Task 2: Add the scheme-level label token and reduce both call sites to reading it** — `15cf312` (feat)
3. **Task 3: Gate the invariance, and re-derive the contrast table** — `34ff18f` (test)

**Plan metadata:** `1389153` (docs: plan one-label-colour-per-theme rule), committed before task execution began.

**This closeout:** committed immediately after this SUMMARY — `docs(20-09): complete label-colour polarity plan`.

## Files Created/Modified

- `library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt` — new, 271 lines, 6 tests: pre-change per-scheme/per-fill worst-case measurement for both pure candidates
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — `labelOnFilledSurface`/`labelOnOutlinedSurface` tokens added (trailing, defaulted, luminance-fallback), set explicitly on all three built-in schemes
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` — `Text` reduced to reading the scheme token; old per-fill computation removed
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — segment `Text` reduced to reading the scheme token; old per-fill computation removed
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt` — re-derived from scratch (464-line diff) against the new mechanism; encodes the three named exceptions
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` — updated to gate the new call-site reduction
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` — updated to gate the new call-site reduction
- `library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt` — field-count structural test updated 24 -> 26
- `library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt` — strengthened from at-most-once to true invariance, D-08 proof updated

No file under `showcase/` was touched.

## Decisions Made

See `key-decisions` in frontmatter. Summarized: label colour is chosen by surface polarity (opaque vs. outlined), not by scheme alone, because AeroBlue/AeroDark contain both polarities and no single colour serves both; the old per-call-site mechanism was retired outright rather than narrowed; 20-04's 4.079/white exception is void and superseded by a differently-valued, differently-coloured exception derived fresh under the new mechanism; `MIN_LABEL_CONTRAST` stays 4.5f.

## Deviations from Plan

None — plan executed exactly as written across all three tasks. This closeout session performed no new implementation work; it wrote the SUMMARY/STATE/ROADMAP artifacts, and updated the stale 20-04-derived todo, for work already committed.

### Todo Maintenance (closeout action, not a plan deviation)

`.planning/todos/pending/2026-07-29-aerodark-recessed-segment-label-contrast-below-wcag-floor.md` described the now-void 20-04 exception (white label, `4.0787`, per-fill algorithm). It was moved to `.planning/todos/completed/` with a closure note explaining the supersession, and a replacement todo was filed at `.planning/todos/pending/2026-07-29-aerodark-aeroblue-recessed-segment-exception-needs-human-legibility-judgment.md` describing the current exception (black label, `3.218`-`4.228`, `AeroButtonContrastRegressionTest.recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException`), still awaiting 20-07's human legibility judgment.

## Requirements Note (deliberate deviation from default requirement-marking)

This plan's own frontmatter lists `requirements: [SHW-16, VER-06]`, matching 20-08's precedent:

- **SHW-16 is NOT marked complete.** It remains `- [ ]` in `REQUIREMENTS.md`. Plan 20-07's human verdict owns SHW-16's closure and has not yet been given.
- **VER-06 is already `- [x]` in `REQUIREMENTS.md`** (closed by Plan 20-01/20-03, reinforced by this plan's D-08-style fail-then-pass discipline on every gate it touched). Left untouched per instruction.

`gsd-tools query requirements.mark-complete` was deliberately NOT run for this plan, for the same reason as 20-08: it would mark both frontmatter IDs and there is no partial-mark mode.

## Issues Encountered

None. All three tasks' verify steps had already passed prior to this closeout session; this session did not re-run the build.

## User Setup Required

None — no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. All changes are theme-token resolution and test-file changes to existing shipped composables' label rendering — no new network, auth, or file-access surface introduced.

## Next Phase Readiness

- The label-colour-per-theme rule from the 20-08 checkpoint is now structurally enforced: `AeroColorScheme` owns the value, `AeroButtonSurface`/`AeroSegmentedControl` only read it, and `VER08SegmentLabelFlipTest` gates true invariance across every interaction state.
- **SHW-16 remains open.** 20-07 must still run its cross-component coherence pass, its two live non-100%-DPI passes, and — specific to this plan — a FRESH legibility judgment on the recessed-segment exception's NEW numbers (black, 3.218-4.228 on AeroBlue/AeroDark), not the old 4.079/white number that 20-08's own SUMMARY still records for historical reasons. A reader consulting only 20-08's SUMMARY for "the" AeroDark recessed-segment contrast value would carry forward a void number; this SUMMARY is the correction.
- Phase 20 now has **9** plans (20-01 through 20-09) — `ROADMAP.md` and `STATE.md` updated accordingly in this closeout.

## Amendment (2026-07-29, same-day revision): white label reinstated on both polarities

**This section appends to the document above; nothing above was rewritten, only marked where it is
now superseded (see the warning blockquotes near the top and above "The Three Surviving
Exceptions").**

**What happened.** The surface-polarity rule this plan shipped — `labelOnFilledSurface = Black` for
AeroBlue/AeroDark, `White` for Classic — was measurement-driven and correctly cleared the 4.5:1
floor on the opaque-fill polarity (with the one recessed-segment exception documented above). The
maintainer then reviewed it running live, across all three themes, at the 20-09 checkpoint that
followed this plan's original completion. They rejected the polarity rule on appearance, not on
correctness: with a dark label on filled surfaces, AeroBlue and AeroDark read as a visually distinct
approach from Classic and from the rest of the library, which is light-on-dark throughout.

**Why rejected, in the maintainer's own framing.** The polarity rule was measured correctly and did
exactly what it was asked to do — clear the contrast floor by choosing the better candidate per
surface. What it did not account for is that AeroBlue/AeroDark's dark-on-fill / light-on-outline
split makes those two themes look like a different design language from Classic's uniform white,
breaking the cross-theme visual coherence the library otherwise maintains. The maintainer was shown
this trade-off in writing twice — including a live run of the white variant across all three
themes captured to `.planning/phases/20-verification/signoff-capture/trial-white-{aeroblue,aerodark,classic}.png`
— before approving white on both polarities in both dark schemes ("одобряю").

**The decision.** `AeroColorScheme.AeroBlue` and `AeroColorScheme.AeroDark` now set BOTH
`labelOnFilledSurface` and `labelOnOutlinedSurface` to `Color.White`, matching Classic exactly.

**Accepted cost — full measured table** (white label, both fill stops, re-measured against the real
`resolveButtonStyle`/`resolveSegmentStyle` outputs, not hand-computed):

| Case                          | AeroBlue top / bottom | AeroDark top / bottom |
|-------------------------------|-------------------------|--------------------------|
| filled-rest / segment-raised  | 3.096 / 4.597           | 2.720 / 4.121            |
| filled-hover / raised-hover    | 2.801 / 3.996           | 2.493 / 3.589            |
| filled-press                  | 4.597 / 3.096           | 4.121 / 2.720            |
| filled-disabled (exempt)       | 7.304 (clears)          | 8.845 (clears)           |
| outlined (all states)          | 13.8 – 14.8 (clears)    | 15.8 – 16.9 (clears)     |
| segment-recessed (rest)        | 6.525 / 4.602 (clears)  | 5.936 / 4.079            |
| segment-recessed-hover         | 5.450 / 4.002           | 4.966 / 3.553            |

Most of AeroBlue/AeroDark's opaque-fill (filled button, segment-raised, segment-recessed-hover)
rest/hover/press cases now sit below the WCAG 4.5:1 normal-text floor. What the white choice also
BUYS, which the black polarity rule could not: AeroBlue's recessed (selected) segment at rest fully
clears the floor (6.525/4.602) without reintroducing state-dependent label colour.

**How it is encoded — one named, scheme-level deviation, not fifteen scattered exceptions.**
`MIN_LABEL_CONTRAST` stays `4.5f`, never lowered (D-13). Every case above that misses the floor is
pinned as a regression bound (must stay `>=` the measured value, never silently regress further
below it) in ONE test method,
`AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`, replacing the
now-void black-token recessed-segment-only exception this document's "Three Surviving Exceptions"
section described. Every case that still clears the floor (outlined, disabled, AeroBlue's
recessed-rest) keeps a plain `>= MIN_LABEL_CONTRAST` assertion elsewhere in the same file, unchanged.
`contrastRatio` in both `AeroButtonContrastRegressionTest` and `VER10OneLabelColorPerThemeTest`
remains an independently-written implementation that does not import production code (D-13).

**What was deliberately NOT done.** No fill constant changed (`FILLED_FILL_TOP_DARKEN`,
`FILLED_FILL_BOTTOM_DARKEN`, `RECESSED_FILL_DARKEN` are untouched) and
`VER10OneLabelColorPerThemeTest`'s measured table is intact — the polarity measurement that
surfaced this trade-off is retained as history, not deleted, per the maintainer's own framing that
the measurement was correct and only the choice of which side to take changed. Darkening the
opaque fills (as Classic's dark `primary` already does) is the known remedy that would remove this
deviation entirely, and was deliberately not taken now because it would change button appearance
signed off in Phase 17 — tracked as a pending todo for a future plan, not silently deferred (see
`.planning/todos/pending/2026-07-29-aeroblue-aerodark-opaque-fill-label-below-wcag-floor-white-decision.md`).

**Commits (revision, this session):**

1. `feat(20-09): white label on both polarities in AeroBlue and AeroDark` — KDoc rewrite in
   `AeroColorScheme.kt`, no behavior change (the working-tree trial value was already White).
2. `test(20-09): encode the accepted sub-floor label contrast in AeroBlue/AeroDark` — the new
   `aeroBlueAeroDarkAcceptedSubFloorLabelDeviation` test, `AeroColorSchemeTest` expectation update,
   `VER10OneLabelColorPerThemeTest` addendum update. `./gradlew :library:test`: 467 tests, 0 failures.
3. `feat(showcase): launch on a chosen theme via -Paero.scheme` — showcase-only, unrelated to the
   label-colour decision itself; exists so a review pass can open a specific theme directly for
   screenshot capture.

**SHW-16 remains open**, unaffected by this revision — 20-07's human three-theme sign-off still owns
its closure and has not been given.

---
*Phase: 20-verification*
*Completed: 2026-07-29*
*Revised: 2026-07-29*

## Self-Check: PASSED

- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt (labelOnFilledSurface/labelOnOutlinedSurface confirmed present)
- FOUND: library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
- FOUND commit: 3b55237 (test(20-09): measure single-label-colour cost per scheme)
- FOUND commit: 15cf312 (feat(20-09): resolve label colour from scheme polarity, not the fill)
- FOUND commit: 34ff18f (test(20-09): gate label-colour invariance across state)
- FOUND commit: 1389153 (docs(20-09): plan one-label-colour-per-theme rule)
