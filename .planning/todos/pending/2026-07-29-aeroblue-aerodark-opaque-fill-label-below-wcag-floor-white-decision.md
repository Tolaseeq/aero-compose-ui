---
created: 2026-07-29T00:00:00Z
type: finding
title: AeroBlue/AeroDark opaque-fill label sits below WCAG 4.5:1 floor (white, maintainer-approved) — darkening the fills is the known remedy, deliberately not taken
area: ui
severity: minor
files:
  - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
---

## Problem

Filed while revising plan 20-09 (label-colour-per-theme rule) a second time, same day. Supersedes
`.planning/todos/pending/2026-07-29-aerodark-aeroblue-recessed-segment-exception-needs-human-legibility-judgment.md`'s
underlying decision context — that todo described the black-token surface-polarity rule's one
recessed-segment exception; this todo describes the current, larger deviation after the maintainer
rejected the polarity rule and reverted to white on both polarities.

`AeroColorScheme.AeroBlue`/`AeroDark` now set `labelOnFilledSurface = Color.White`, matching Classic,
per the maintainer's explicit, informed decision (shown the cost table below in writing twice,
including a live cross-theme run, before approving — "одобряю"). This puts most of the two schemes'
opaque-fill (filled button, segment-raised, segment-recessed-hover) rest/hover/press cases below the
WCAG 1.4.3 normal-text floor (4.5:1):

| Case                          | AeroBlue top / bottom | AeroDark top / bottom |
|-------------------------------|-------------------------|--------------------------|
| filled-rest / segment-raised  | 3.096 / 4.597           | 2.720 / 4.121            |
| filled-hover / raised-hover    | 2.801 / 3.996           | 2.493 / 3.589            |
| filled-press                  | 4.597 / 3.096           | 4.121 / 2.720            |
| segment-recessed (rest)        | 6.525 / 4.602 (clears)  | 5.936 / 4.079            |
| segment-recessed-hover         | 5.450 / 4.002           | 4.966 / 3.553            |

`AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation` gates every one of
these as a regression bound — each measured ratio must stay `>=` its pinned value, never silently
get worse — but does not, and cannot, judge whether the current shipped result is visually
acceptable in practice. `MIN_LABEL_CONTRAST` (4.5f) is unchanged and never lowered (D-13).

## Why not fixed inline

This is an accepted aesthetic-over-measurement trade-off, not a bug: the maintainer explicitly chose
cross-theme visual coherence (AeroBlue/AeroDark reading the same "white label everywhere" way as
Classic and the rest of the library) over clearing the contrast floor on every opaque fill. It was
not fixed inline because the maintainer's decision already settled the visual question that a code
fix would otherwise be second-guessing.

## Solution

**The known remedy: darken AeroBlue/AeroDark's opaque fills (as Classic's dark `primary` already
does), so white clears the floor without a state-dependent label colour.** Classic's `primary` is
dark, which is exactly why its opaque fills already clear 4.5:1 with white in every state (`filled-hover`'s `4.455` near-miss is Classic's own separate, much smaller finding — see
`AeroButtonContrastRegressionTest.classicFilledHoverIsTheOneAuthorizedLightTokenException`).
Bringing AeroBlue/AeroDark's `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN`/`RECESSED_FILL_DARKEN`-derived
fills down toward Classic's darkness would let white clear the floor there too, closing this
deviation entirely rather than just bounding it.

**This was deliberately NOT done now**, because darkening the opaque fills changes how AeroBlue and
AeroDark's buttons and segments actually look — their fill colour, not just the label — and that
button appearance was signed off in Phase 17. A fill-darkening fix belongs in its own plan, with:

1. Fresh contrast math re-confirming `AeroButton` rest/hover/press and both segment cases (raised
   and recessed) clear 4.5:1 in all three themes with the new, darker fill — not just AeroBlue/AeroDark
   in isolation, and not just the cases currently below the floor.
2. A fresh 20-07-style three-theme visual sign-off, since darkening the fill changes what every
   button/segment looks like at rest, not only the label's legibility — a strictly bigger visual
   change than either of the two label-only decisions this todo and its predecessor describe.
3. Retirement of `aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`'s regression-bound constants
   (or a subset, if only some fills are darkened enough to clear) once the new measured values are in.

Until a future plan takes this up, `aeroBlueAeroDarkAcceptedSubFloorLabelDeviation` keeps the
current, maintainer-approved cost from silently getting worse — it does not, and is not meant to,
judge whether that cost is acceptable forever.
