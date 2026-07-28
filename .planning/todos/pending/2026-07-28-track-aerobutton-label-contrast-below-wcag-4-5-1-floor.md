---
created: 2026-07-28T12:46:45Z
type: finding
title: AeroButton label-to-fill contrast is below the WCAG 4.5:1 floor
area: ui
severity: minor
files:
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:71-125
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:177-188
---

## Problem

Filed while closing gap G5 (`.planning/phases/19-selectors-lists/19-UAT.md`) — the maintainer
explicitly deferred this rather than fixing it inline, since fixing it would be a visual change to
a Phase-17-signed-off component requiring fresh sign-off.

`AeroButtonSurface.kt`'s `Text` sets no `color =` parameter. `AeroTheme.typography.bodyLarge`
leaves the color at `Color.Unspecified`, so the label resolves to whatever `LocalContentColor` is
ambient at the call site — in the showcase, the root `Surface(color = colors.background)` makes
that `onBackground`, which is defined byte-identical to `onSurface` in all three shipped schemes.
**The button's label has never actually been white**, despite `FILLED_FILL_TOP_DARKEN`'s KDoc
historically claiming the darken exists "for legible white button text" (that KDoc wording was
corrected to state the true ambient-resolution mechanism as part of closing G5 — a comment-only
fix, no behavior change).

Measured label-to-fill contrast ratios (WCAG 2.x formula, `(lighter+0.05)/(darker+0.05)` of
relative luminance), against the button's own rest fill
(`colors.primary.darken(FILLED_FILL_TOP_DARKEN)` / `.darken(FILLED_FILL_BOTTOM_DARKEN)`):

| Theme    | fillTop ratio | fillBottom ratio |
|----------|---------------|-------------------|
| AeroBlue | 2.35          | 3.49              |
| AeroDark | 1.70          | 2.56              |
| Classic  | 3.98          | 5.54              |

All three themes fall short of the WCAG 1.4.3 normal-text floor (4.5:1) on at least one fill stop;
AeroBlue and AeroDark fall short on both. No test in the codebase currently measures this — it was
never covered before this todo was filed.

## Solution

TBD — not to be fixed as part of gap G5. Two directions were identified during investigation, and
either requires fresh visual sign-off since `AeroButton` was accepted in Phase 17:

1. Darken the button's fill further (magnitude TBD) so the ambient `onBackground`/`onSurface`
   label clears 4.5:1 against both fill stops in every theme.
2. Introduce an explicit on-fill content token (e.g. a near-white `onPrimary`-style token) and set
   it explicitly on the button's `Text`, decoupling the button's label from the ambient ceremony
   entirely — this is the "white button text" the old KDoc assumed was already happening.

Whichever direction is chosen, add a value-level contrast regression test (mirroring the guard
retired from `AeroSegmentedControlStylesTest.kt` when gap G5 closed) so this does not silently
regress again.
