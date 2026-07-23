# Phase 17: Buttons - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-23
**Phase:** 17-buttons
**Areas discussed:** Filled-button identity, Pressed state, Hover state, Outlined variant, Focus state, Disabled state

---

## Filled-button identity

| Option | Description | Selected |
|--------|-------------|----------|
| Keep accent-tinted (blue) | Filled button stays accent/primary color, rendered as two-tone glass (gloss+seam+bevel) instead of flat; classic Win7 blue action button, distinct from panels/cards | ✓ |
| Neutral glass surface | Filled button becomes neutral light-glass, matching `AeroSurfaceStyle.rest()`; risk of reading like a panel | |
| Neutral at rest, accent on hover/press | Rests neutral, accent surfaces on hover/press; subtler, more "modern Aero" | |

**User's choice:** Keep accent-tinted (blue)
**Notes:** The filled button must retain its blue action-button identity — the glass treatment is additive, not a shift to neutral. Consequence: filled button drives its surface stops from `primary`/accent, not the neutral `rest()` default.

---

## Pressed state

| Option | Description | Selected |
|--------|-------------|----------|
| Fully recessed (inversion + inner shadow) | Gradient flips (dark-top/light-bottom) + inner shadow rim; looks physically pushed in; drops current 0.97 scale; best fit for Phase 19 selected-segment reuse | ✓ |
| Inverted gradient + keep 0.97 scale | Flip plus the existing scale-shrink | |
| Inverted gradient only | Just the two-tone flip; cleanest but may read too subtly for the recessed segment reuse | |

**User's choice:** Fully recessed (inversion + inner shadow)
**Notes:** Chosen deliberately because Phase 19's `AeroSegmentedControl` selected segment reuses this pressed-fill code — the strongest recess reads best there. Current `graphicsLayer` scale 0.97 is removed.

---

## Hover state

| Option | Description | Selected |
|--------|-------------|----------|
| Glow ring + brighten fill | Outer `aeroGlowRing` bloom PLUS the surface gloss/fill lightens; whole button "lights up" like Win7 | ✓ |
| Glow ring only | Just the outer bloom, fill stays at rest; cleaner/subtler | |
| You decide on the three-theme review | Calibrate hover intensity empirically per theme | |

**User's choice:** Glow ring + brighten fill
**Notes:** Replaces the current flat `drawRect(buttonHover)` overlay.

---

## Outlined variant

| Option | Description | Selected |
|--------|-------------|----------|
| Faint glass fill + Aero contour | Near-transparent glass (whisper of gloss/gradient) inside a bright Aero bevel-contour; lighter sibling of the filled button; strong shared-surface story for VBTN-06 | ✓ |
| Truly transparent, contour + hover glow only | No fill at rest; only Aero bevel/rim border + hover glow; fill appears only on hover/press | |
| Transparent at rest, faint glass on hover | Empty at rest, faint glass fades in on hover before the press inversion | |

**User's choice:** Faint glass fill + Aero contour
**Notes:** Painted by the SAME shared internal surface composable as the filled button (VBTN-06), differing only by fill opacity / contour emphasis.

---

## Focus state

| Option | Description | Selected |
|--------|-------------|----------|
| Glow ring (like hover, but constant) | Focus = the same `aeroGlowRing`, persistent and tuned to a slightly different intensity/tint than hover | ✓ |
| Glow ring + crisp inner outline | Glow plus a thin crisp edge line; more accessible but slightly less "clean" | |
| You decide | Tune on the three-theme review so focus is distinguishable from hover and clearly visible | |

**User's choice:** Glow ring (like hover, but constant)
**Notes:** Replaces the current solid 2.dp `borderSelected` border. Focus/hover differentiation (intensity/tint) left to Claude on the review.

---

## Disabled state

| Option | Description | Selected |
|--------|-------------|----------|
| Flatten glass (remove gloss/bevel) + mute | Gradient collapses to near-flat, gloss/bevel removed, color muted; button reads genuinely "dead", loses depth | ✓ |
| Just alpha (as now) | Keep uniform 0.4 alpha; glass stays "alive", just translucent | |
| You decide | Tune on the review so disabled reads distinctly on all three themes | |

**User's choice:** Flatten glass (remove gloss/bevel) + mute
**Notes:** Replaces the current uniform `alpha = 0.4f`.

---

## Claude's Discretion

- Native `Modifier.dropShadow`/`innerShadow` vs manual gradient per depth cue (per Phase 16 D-02), settled on the three-theme review.
- Focus-vs-hover glow differentiation (intensity/tint deltas) so both are distinguishable when co-active.
- Exact gloss fraction / seam softness / bevel magnitude within the Phase 16 D-01 "moderate" band; accent-fill stop derivation for the filled button.
- Shape/parameters of the shared internal surface composable (name, param set, filled-vs-outlined and state expression) — must keep the pressed-fill cleanly reusable by Phase 19.
- Whether disabled's flattened surface is a distinct `AeroSurfaceStyle` variant or a transform of the rest style.

## Deferred Ideas

None — discussion stayed within phase scope. `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` are outside the eight-target milestone set and intentionally untouched (a future Aero pass on them would be its own phase).
