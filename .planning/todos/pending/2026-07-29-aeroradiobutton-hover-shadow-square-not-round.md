---
created: 2026-07-29T00:00:00Z
type: finding
title: AeroRadioButton hover shadow renders SQUARE, should be ROUND
area: ui
severity: minor
files:
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt
---

## Problem

Filed from the SHW-16 three-theme sign-off (20-07). Maintainer's verdict, verbatim:

> У RadioButton при наведении тень квадратная, а должна быть круглая. Но это неблокирующий баг
> конкретно его, просто фиксируем на будущее

The maintainer classified this explicitly as **non-blocking and specific to `AeroRadioButton`** —
not a phase blocker, not a defect in the shared theme/primitives layer. Recorded here for later,
not fixed now.

## Context for whoever picks this up

`AeroRadioButton` was **not restyled** in the v3.0 Glass Refinement milestone (it stayed out of the
eight-component scope: `AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`,
`AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`). But the shared theme layer moved
underneath it twice during Phase 20:

- `AeroTheme` began painting its own background (`1d139a7`, plan 20-06).
- The label-colour tokens changed (`24e6705`, plan 20-09) — though `AeroRadioButton`'s own label uses
  `colors.onSurface` directly, not `labelOnFilledSurface`/`labelOnOutlinedSurface`, so this second
  move is less likely to be the cause.

A square shadow on a round control usually means a shadow/elevation or hover-indication modifier is
being applied without the component's shape, or applied before the shape is clipped.

**One candidate worth checking first** (observed while filing this todo, not confirmed as root
cause): the radio dot's outer `Box` in `AeroRadioButton.kt` applies `.border(1.dp, borderColor, shape)`
then `.selectable(...)` with no `.clip(shape)` anywhere in the chain, and no explicit `indication =`
argument. Compose's default hover/press indication for `selectable` is not shape-aware unless the
modifier chain clips to the target shape first — an unclipped default indication would render its
highlight/shadow against the `Box`'s rectangular layout bounds rather than the circular border,
which would look exactly like "square shadow on a round control."

## Why not fixed inline

The maintainer explicitly deferred this: non-blocking, component-specific, "просто фиксируем на
будущее" (just record it for later). SHW-16's sign-off does not gate on it.

## Solution

Not scoped here — left for whoever picks up this todo to confirm the root cause (indication clipping
vs. a shadow modifier vs. something else) and apply the minimal fix, then re-verify the hover state
on `AeroRadioButton` across all three themes.
