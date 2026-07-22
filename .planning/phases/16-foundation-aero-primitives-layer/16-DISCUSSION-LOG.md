# Phase 16: Foundation — Aero Primitives Layer - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 16-foundation-aero-primitives-layer
**Areas discussed:** Aero fidelity calibration, Native shadows vs gradient depth, Foundation sign-off vehicle, GlassModifiers fix blast radius
**Language:** Discussion conducted in Russian at user request (technical terms/paths/code kept English).

---

## Aero Fidelity Calibration

| Option | Description | Selected |
|--------|-------------|----------|
| Заметно стеклянный (lean Win7) | Bright top gloss ~45–50% height, sharp two-tone seam mid-surface, pronounced bevel-highlight + rim, glassy dome. Matches `aero-aesthetic` memory. | |
| Умеренный (spirit of Aero) | Gloss present but restrained (~30–35% height), soft seam, thin bevel; depth reads but doesn't dominate. Closer to PROJECT.md wording. | ✓ |
| Гибрид: сильный глянец, мягкий шов | Bright top gloss + bevel (Win7-recognizable) but no hard mid-seam, to avoid looking skeuomorphic-dated on large surfaces. | |

**User's choice:** Умеренный (spirit of Aero)
**Notes:** Moderate point on the Win7↔modern spectrum. Still glass (gloss/gradient/bevel/depth), not generic-flat — no conflict with the `aero-aesthetic` memory, which forbids generic-flat/Feather-outline defaults, not moderate glass. Baked into `drawAeroSurfaceCore`/`AeroSurfaceStyle` defaults that all eight Phase 17–19 components inherit.

---

## Native Shadows vs Gradient Depth

| Option | Description | Selected |
|--------|-------------|----------|
| Нативные = единый механизм глубины | dropShadow on raised thumbs/buttons, innerShadow for grooves/pressed segments + rim; gradients only for fill/gloss. | |
| Нативные для теней, градиент для rim | dropShadow/innerShadow only for drop/recess; bevel-highlight + rim stay gradient inside drawAeroSurfaceCore for color control. | |
| Ты решаешь (по-примитивно) | Calibrate on 3-theme review per primitive; native where it looks better, gradient where more precise. | ✓ |

**User's choice:** Ты решаешь (по-примитивно) — Claude discretion, per primitive, on the 3-theme review.
**Notes:** Signatures already proven in `ScratchAeroShadowProof.kt`. Follow-up sub-question on the dead `glassEffect(elevation)` param (revive via dropShadow / remove / you decide) → **Ты решаешь**: revive if it improves cards/panels on 3 themes, remove if it adds noise; folds into the blast-radius review.

---

## Foundation Sign-off Vehicle

| Option | Description | Selected |
|--------|-------------|----------|
| Выделенная Primitives-галерея | Dedicated showcase section: each primitive in isolation ×3 themes ×states; stays as living reference for Phases 17–19. Max catches silent defects. | ✓ |
| Throwaway scratch-экран | Like ScratchAeroShadowProof.kt — temporary eyes-on screen, then deleted. Lighter but no lasting reference. | |
| Только через Phase 17 кнопки | No separate stand; judge primitives via first consumer (AeroButton). Less work but violates PRIM-16 and repeats false-positive risk. | |

**User's choice:** Выделенная Primitives-галерея
**Notes:** Directly addresses the v2.0.3 false-positive-sign-off lesson (`feedback_repro_must_exercise_path`). Distinct from the PRIM-17 full-library smoke pass (existing ~50 showcase components). Permanent living reference, additive/reversible.

---

## GlassModifiers Fix Blast Radius

| Option | Description | Selected |
|--------|-------------|----------|
| Консервативно: 40 должны выглядеть как раньше | Only the 8 targets get the new Aero look; glassSurface/glassEffect fixes tuned so the other ~40 stay maximally unchanged. Regression = any visible shift. | |
| «Более правильно»: принять сдвиг | Apply correct fixes as-is (proportional gloss, full border); the ~40 also become slightly "more glass". Regression = only broken/ugly, not "slightly different". | ✓ |
| Ты решаешь по факту на smoke pass | Run full-library smoke pass, show before/after where shift is visible, decide spot-by-spot. | |

**User's choice:** «Более правильно»: принять сдвиг
**Notes:** PRIM-17 smoke pass gate = no broken/ugly rendering, NOT pixel-identical. Do not hand-tune fixes to preserve the 40 unchanged.

---

## Claude's Discretion

- Per-primitive native-shadow vs gradient choice, settled on the 3-theme review.
- `glassEffect(elevation)` revive-via-dropShadow vs remove, settled on the review (folds into blast-radius).
- `AeroSurfaceStyle`/`AeroOrnamentTokens` field shapes, `derive(base)` lighten/darken magnitudes, glow-ring + thumb/groove geometry, exact gloss fraction within ~30–35%, Primitives-gallery structure.

## Deferred Ideas

None — discussion stayed within phase scope.
