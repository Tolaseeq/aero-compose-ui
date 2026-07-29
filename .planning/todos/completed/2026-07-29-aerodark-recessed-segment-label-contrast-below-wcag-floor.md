---
created: 2026-07-29T00:00:00Z
type: finding
title: AeroDark recessed segment label-to-fillBottom contrast is below the WCAG 4.5:1 floor
area: ui
severity: minor
files:
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:195-206
---

## Closed — superseded by 20-09 (2026-07-29)

This finding's premise (label fixed to `Color.White` via the retired per-call-site
`resolveLabelColor(fillTop, fillBottom, backdrop)` algorithm, `4.0787` measured against AeroDark's
recessed `fillBottom`) no longer describes the code. Plan 20-09 retired `resolveLabelColor` entirely
in favor of `AeroColorScheme.labelOnFilledSurface`/`labelOnOutlinedSurface`, each resolved once per
scheme rather than recomputed per call site from an animating fill (the maintainer's 20-08 checkpoint
rule: "per theme, ONE text colour, not per element, not per state").

Under the new mechanism AeroBlue/AeroDark's `labelOnFilledSurface` is `Color.Black`, not white, and
the recessed segment's contrast was re-measured fresh rather than inherited: AeroBlue rest/press
`fillTop` = `3.218`, hover = `3.853`; AeroDark rest/press `fillTop` = `3.538`, hover = `4.228`
(`fillBottom` clears comfortably in every case). **The `4.0787`/white exception this todo tracked is
VOID** — it is not a variant or ancestor of the current number, it depended on a per-fill rescue
mechanism that has been deleted.

The underlying finding — this fill cannot clear 4.5:1 without either retuning `RECESSED_FILL_DARKEN`
(forbidden by that constant's own KDoc) or reintroducing per-state label colour (forbidden by the
20-08 checkpoint rule) — still holds, now under new numbers and a new named, regression-bounded
exception: `AeroButtonContrastRegressionTest.recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException`.

**Replacement todo (still open, awaiting 20-07's human legibility judgment):**
`.planning/todos/pending/2026-07-29-aerodark-aeroblue-recessed-segment-exception-needs-human-legibility-judgment.md`

Original finding preserved below for history — do not treat its numbers or mechanism as current.

---

## Problem

Filed while closing 20-04's own open finding (`20-04-SUMMARY.md`, "Open Finding: AeroDark recessed
segment, fillBottom") and converting its deliberately-red test into a tracked, guarded exception
(`AeroButtonContrastRegressionTest.aeroDarkRecessedSegmentFillBottomIsTheOneAuthorizedContrastException`),
per the user's decision at the 20-04 orchestrator checkpoint on 2026-07-29.

AeroDark's recessed (selected) `AeroSegmentedControl` segment measures a label-to-`fillBottom`
contrast ratio of `4.0787` — below the WCAG 1.4.3 normal-text floor of `4.5:1`
(`MIN_LABEL_CONTRAST` in `AeroButtonContrastRegressionTest.kt`).

This was verified NOT fixable by `resolveLabelColor`'s own candidate flip: it already picks
whichever of `LABEL_CANDIDATE_DARK`/`LABEL_CANDIDATE_LIGHT` wins on the WORST case across both
composited fill stops. For this exact fill pair, white's worst case (the `fillBottom` stop,
`4.0787`) is still the better choice — black's worst case (its `fillTop` stop, `3.5377`) is
strictly worse. No third candidate or resolver logic change would help.

No sanctioned remedy currently applies:
- Widening `FILLED_FILL_BOTTOM_DARKEN` (20-04's one authorized remedy) is scoped to the filled
  AeroButton rest case only, and was not needed there (AeroBlue's `fillBottom` measured `4.568`,
  comfortably clearing the floor without any widening).
- Retuning `RECESSED_FILL_DARKEN`, or introducing a segment-specific darken constant to chase this
  floor, is explicitly FORBIDDEN — D-12's one-source-of-truth rule for fill darkening, and the
  maintainer's 19-08 explicit acceptance of the recess/depth reading that produces this fill.

## Solution

TBD — closing this requires revisiting the recess depth itself (the maintainer's 19-08 accepted
value), not a colour-only patch. Any fix must:
1. Come with fresh contrast math confirming both `AeroButton` rest and both segment cases
   (raised and recessed) still clear `4.5:1` in all three themes — not just AeroDark's recessed
   case in isolation.
2. Be its own decision record — `AeroButtonContrastRegressionTest`'s guarded exception
   (`aeroDarkRecessedSegmentFillBottomIsTheOneAuthorizedContrastException`) is explicitly the ONLY
   authorized below-floor carve-out; a fix does not extend it, it retires it.
3. Update or remove the exception's hard regression bound
   (`AERODARK_RECESSED_FILLBOTTOM_AUTHORIZED_RATIO = 4.0787f`) once the measured ratio changes.

Until then, `AeroButtonContrastRegressionTest`'s guarded exception keeps the finding from getting
worse unnoticed, and asserts the flip is still not a better choice — but does not judge whether
`4.0787` is visually acceptable. **20-07's three-theme human sign-off still judges this visually.**
