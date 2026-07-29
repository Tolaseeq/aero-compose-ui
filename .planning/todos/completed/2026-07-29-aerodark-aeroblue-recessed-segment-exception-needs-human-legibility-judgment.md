---
created: 2026-07-29T00:00:00Z
type: finding
title: AeroBlue/AeroDark recessed-segment label contrast (black, 3.218-4.228) needs a human legibility judgment
area: ui
severity: minor
files:
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
---

## Closed — superseded by the same-day 20-09 revision (2026-07-29)

This finding's premise (`AeroColorScheme.labelOnFilledSurface = Color.Black` for AeroBlue/AeroDark,
under the surface-polarity rule) no longer describes the code. The maintainer reviewed that rule
running live, across all three themes, and rejected it on appearance in favor of white on both
polarities in both schemes, matching Classic. `AeroColorScheme.AeroBlue`/`AeroDark` now set
`labelOnFilledSurface = Color.White`.

**The `3.218`-`4.228`/black recessed-segment-only exception this todo tracked is VOID** — it is not
a variant or ancestor of the current deviation, it depended on the black-label polarity rule that
has been reverted. Under white, AeroBlue's recessed segment at rest actually CLEARS the floor
(`6.525`/`4.602`) rather than needing an exception at all; AeroDark's recessed segment and both
schemes' recessed-hover case, along with most of their filled/raised-segment rest/hover/press cases,
now sit below the floor instead — a larger, differently-shaped deviation than the one this todo
described.

**Replacement todo (still open, tracks the current, larger deviation and its known-but-not-taken
remedy):**
`.planning/todos/pending/2026-07-29-aeroblue-aerodark-opaque-fill-label-below-wcag-floor-white-decision.md`

Original finding preserved below for history — do not treat its numbers or mechanism as current.

---

## Problem

Filed while closing plan 20-09 (label-colour-per-theme rule), replacing
`.planning/todos/completed/2026-07-29-aerodark-recessed-segment-label-contrast-below-wcag-floor.md`
which described a now-void number derived under a retired mechanism.

Plan 20-09 replaced the per-call-site `resolveLabelColor` mechanism with two scheme-level tokens,
`AeroColorScheme.labelOnFilledSurface` / `labelOnOutlinedSurface`, resolved once per scheme rather
than recomputed per call site from an animating fill. Under this new mechanism, AeroBlue and
AeroDark both set `labelOnFilledSurface = Color.Black` (their `primary` is a light blue, so their
other opaque fills — filled button, raised segment — read comfortably with black).

The recessed (selected) `AeroSegmentedControl` segment's fill is that same base darkened further by
`RECESSED_FILL_DARKEN` (0.20) on top of the pressed-recess transform, which pushes `fillTop` past
the point where black still clears the WCAG 1.4.3 normal-text floor (4.5:1):

| Scheme   | fillTop rest/press | fillTop hover | fillBottom (both states) |
|----------|---------------------|----------------|----------------------------|
| AeroBlue | 3.218               | 3.853          | 4.563 / 5.248 (clears)     |
| AeroDark | 3.538               | 4.228          | 5.149 / 5.910 (clears)     |

This is a **different, freshly-derived exception**, not the `4.0787`/white value 20-04 originally
authorized and 20-08's own SUMMARY still records — that number depended on a per-fill rescue
algorithm now retired, and is void (see the closed todo linked above).

`AeroButtonContrastRegressionTest.recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException` gates
this as a regression bound (each measured ratio must stay `>=` the pinned value, never silently get
worse) — it does not judge whether `3.218`-`4.228` reads legibly to a human eye in practice.
`MIN_LABEL_CONTRAST` (4.5f) is unchanged and never lowered (D-13).

## Why not fixed inline

Flipping the recessed segment's label to white would make this one fill legible again at the cost
of reintroducing exactly the defect plan 20-09 exists to remove — a label that changes colour
depending on selection state, since every OTHER opaque fill in these two schemes (filled button,
raised segment) stays black. Retuning `RECESSED_FILL_DARKEN` to rescue this instead is forbidden by
that constant's own KDoc (the maintainer's 19-08 acceptance of the recess/depth reading that produces
this fill, unchanged since).

## Solution

TBD — this is a perceptual judgment, not a code fix. **20-07's three-theme human sign-off must judge
this visually** as part of SHW-16: does AeroBlue/AeroDark's recessed segment label (black, measured
3.218-4.228 depending on state) read legibly in practice? If judged unacceptable, any fix must:

1. Come with fresh contrast math re-confirming both `AeroButton` rest and both segment cases (raised
   and recessed) still clear 4.5:1 in all three themes — not just this one case in isolation.
2. Retire this exception rather than extend it — a fix does not widen the authorized carve-out, it
   closes it.
3. Update or remove `AeroButtonContrastRegressionTest`'s pinned regression-bound constants
   (`AEROBLUE_RECESSED_TOP_REST_AUTHORIZED_RATIO`, `AEROBLUE_RECESSED_TOP_HOVER_AUTHORIZED_RATIO`,
   `AERODARK_RECESSED_TOP_REST_AUTHORIZED_RATIO`, `AERODARK_RECESSED_TOP_HOVER_AUTHORIZED_RATIO`)
   once the measured ratio changes.

Until then, the guard keeps the finding from getting worse unnoticed and asserts the flip to white
is not silently taken as a better choice — but does not judge whether the black recessed label is
visually acceptable.
