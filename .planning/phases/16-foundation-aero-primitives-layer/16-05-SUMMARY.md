---
phase: 16-foundation-aero-primitives-layer
plan: 05
subsystem: ui
tags: [compose, kotlin, material3, slider, glow, verification, sign-off]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer (Plan 01)
    provides: drawAeroSurfaceCore / AeroSurfaceStyle primitives consumed by the spike thumb+groove
  - phase: 16-foundation-aero-primitives-layer (Plan 02)
    provides: aeroThumbSurface, aeroGroove, aeroGlowRing primitives (the gallery + spike subjects)
  - phase: 16-foundation-aero-primitives-layer (Plan 03)
    provides: GlassModifiers.kt fixes (endY proportional gloss, clip-order fix, glassEffect elevation revive) — the PRIM-17 smoke-pass surface
provides:
  - "PRIM-18 verdict PASS — AeroSlider stays on Material3 + custom-sized thumb=/track= slots at MEDIUM complexity for Phase 18 (not full M3 removal)"
  - "aeroGlowRing fixed to a genuinely visible, moderate Aero glow (bloom-clip and same-hue-family defects closed) — the shared hover/focus primitive every Phase 17-19 interactive component inherits"
  - "D-03 locked: glassEffect(elevation) REVIVE confirmed as final (dropShadow wired in Plan 03, kept as-is)"
  - "PRIM-16/PRIM-17 human sign-off records — Phase 16's exit gate"
affects: [17-buttons, 18-range, 19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "M3 Slider's internal MeasurePolicy (SliderKt$SliderImpl$2$1.measure-3p2s80s) measures thumb/track slots from their own real Placeable dimensions and derives all placement math from those measured values, never a hardcoded SliderTokens literal — custom-sized thumb=/track= slots are first-class supported, confirmed via javap bytecode inspection of material3-desktop-1.9.0.jar"
    - "aeroGlowRing USAGE CONTRACT: must be applied OUTSIDE (before, to the left of) any clipping modifier in the chain — a surface's own .clip(shape) silently erases the glow's out-of-bounds bloom if aeroGlowRing is applied after aeroSurface/clip"
    - "Multi-ring concentric bloom (GLOW_RING_BLOOM_LAYERS solid-color rings stepping outward, each fading by GLOW_RING_BLOOM_FALLOFF) replaces a single center-anchored Brush.radialGradient for aeroGlowRing's outer glow — a radial gradient centered on a non-square box puts its bright stop in the interior, not at the perimeter, which is why it read as invisible against fillSplitTop of the same hue family"

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt

key-decisions:
  - "PRIM-18 spike verdict: PASS — keep Material3 Slider + custom thumb=/track= slots at MEDIUM complexity for Phase 18 (STATE.md's locked architecture position), confirmed by both javap bytecode inspection of the shipped measure/placement algorithm and the human three-theme review finding nothing live that contradicts it"
  - "D-03 CONFIRMED: glassEffect(elevation) stays REVIVED (dropShadow wired in Plan 03) — the reviewer judged the shadow adds legible depth to cards/panels across all three themes without reading as noise"
  - "aeroGlowRing hoverGlow final calibration: primary.lighten(0.30f) (AeroOrnamentTokens), GLOW_RING_BLOOM_BASE_ALPHA = 0.33f, GLOW_RING_INNER_STROKE_ALPHA = 0.45f (AeroSurfacePrimitives) — arrived at via two soften passes after an initial 0.75f/0.9f/0.65f overcorrection read as a blown-out near-white rim instead of a soft focus glow"
  - "PrimitivesSection.kt gallery demo modifier order fixed to aeroGlowRing().aeroSurface() (glow outside, surface's .clip(shape) innermost) — the actual root cause of the glow being invisible in the gallery, distinct from the intensity/hue defect"
  - "Non-100% DPI smoke pass explicitly waived by the reviewer (would require altering the whole desktop's display scale) — recorded as a deliberate scope reduction of that backstop must_have, not a silent skip"

patterns-established:
  - "Bytecode-inspection verdict methodology (javap -c on the shipped dependency jar) as a legitimate compile+static-analysis proof for library-internals questions that can't be answered from source-level API docs alone — same rigor tier as ScratchAeroShadowProof.kt's TOOL-07 precedent"
  - "aeroGlowRing must be the outermost-applied-before-clip modifier for any glass primitive it wraps — carried forward as a hard rule for every Phase 17-19 component that pairs hover/focus glow with a clipped surface"

requirements-completed: [PRIM-16, PRIM-17, PRIM-18]

coverage:
  - id: D1
    description: "ScratchSliderSlotSpike.kt feeds a custom-sized thumb=/track= slot (aeroThumbSurface + aeroGroove) into a real Material3 Slider; KDoc records verbatim PASS verdict with Phase 18 consequence"
    requirement: "PRIM-18"
    verification:
      - kind: automated
        ref: "./gradlew :showcase:compileKotlin — BUILD SUCCESSFUL against the real Material3 1.9.0 artifact"
        status: pass
      - kind: manual
        ref: "javap -c bytecode inspection of material3-desktop-1.9.0.jar's SliderKt$SliderImpl$2$1.measure-3p2s80s + SliderImpl's requiredSizeIn floor-only sizing; human checkpoint confirmed nothing observed live contradicts the verdict"
        status: pass
    human_judgment: true
  - id: D2
    description: "Full-library smoke pass (~50 sections) across AeroBlue/AeroDark/Classic shows no broken/ugly rendering from the GlassModifiers.kt fixes"
    requirement: "PRIM-17"
    verification:
      - kind: manual
        ref: "Three-theme human sign-off at the blocking checkpoint; screenshot evidence captured in .planning/phases/16-foundation-aero-primitives-layer/signoff-capture/ (aeroblue-scroll1/2.png, aerodark-top.png, classic-top.png, etc.)"
        status: pass
    human_judgment: true
  - id: D3
    description: "Dedicated Primitives gallery (surface, glow ring, thumb, groove, gloss, shadow) signed off as genuinely glass per D-01 on all three themes; Classic's opaque tokens confirmed not flat"
    requirement: "PRIM-16"
    verification:
      - kind: manual
        ref: "Three-theme gallery sign-off; aeroGlowRing defect found and fixed mid-review (see Deviations), then re-confirmed PASS via pixel measurement of the outer bloom + zoomed visual review (zoom-glowfix-*.png, zoom-soft*.png)"
        status: pass
    human_judgment: true
  - id: D4
    description: "D-03 glassEffect(elevation) revive-vs-remove call confirmed on this review"
    requirement: "D-03"
    verification:
      - kind: manual
        ref: "Reviewer confirmed REVIVE (dropShadow, wired in Plan 03) stays — improves cards/panels across all three themes, no noise"
        status: pass
    human_judgment: true

duration: ~2h10min
completed: 2026-07-22
status: complete
---

# Phase 16 Plan 05: Verification Spike + Smoke Summary

**PRIM-18's M3 Slider custom-slot spike passes (bytecode-proven, keeping AeroSlider at MEDIUM complexity for Phase 18), and the three-theme gallery/smoke sign-off closes Phase 16 after fixing a real aeroGlowRing visibility defect found during review — glow now genuinely visible and moderate on all three themes, D-03 confirmed REVIVE.**

## Performance

- **Duration:** ~2h10min (Task 1 spike + bytecode analysis: ~35min; Task 2 human three-theme review + glow gap-fix cycle: ~1h35min)
- **Tasks:** 2 (Task 1 auto; Task 2 checkpoint:human-verify, gate="blocking")
- **Files modified:** 6 (1 created scratch spike, 2 library source, 1 showcase source, 2 test files)
- **Commits:** 5 (1 spike + 4 glow-fix)

## Accomplishments

### Task 1 — PRIM-18 M3 Slider slot-sizing spike (PASS)

- `ScratchSliderSlotSpike.kt` feeds an 18.dp raised circular `aeroThumbSurface` thumb and a 10.dp recessed `aeroGroove` track into a real `androidx.compose.material3.Slider` via its `thumb=`/`track=` slot parameters, compiling cleanly against the pinned Material3 1.9.0 artifact.
- Verdict **PASS**, recorded verbatim in the file's KDoc: keep M3 `Slider` + custom-sized slots at MEDIUM complexity for Phase 18 (STATE.md's locked architecture position) — do NOT fall back to full M3 removal / `AeroRangeSlider`-style Canvas rewrite.
- Evidence basis: `javap -c` bytecode inspection of the real `material3-desktop-1.9.0.jar`. `SliderKt$SliderImpl$2$1.measure-3p2s80s` (the `MeasurePolicy` backing `SliderImpl`'s internal `Layout`) measures the `THUMB`-tagged `Measurable` first, then derives the `TRACK` measurable's own constraints from that placeable's real `getWidth()`/`getHeight()` via `Constraints.offset(...)` — never a hardcoded `SliderTokens` literal. All placement math (overall bounding box, track vertical centering, drag-fraction thumb offset) is likewise computed from the two placeables' actual measured dimensions. `SliderImpl`'s own `requiredSizeIn(minWidth, minHeight)` uses `SliderTokens` values as a floor only (no max bound), so an oversized custom thumb/track is never clipped.
- Discrepancy noted (not a verdict change): the `thumb=`/`track=` overload is `@ExperimentalMaterial3Api` at the 1.9.0 pin (confirmed by a compile error before `@OptIn` was added) — Phase 18's real `AeroSlider` restyle will need the same opt-in.
- No live screenshot capture was available in the spike's own execution context; the KDoc explicitly instructs that any live-observed clipping/misalignment at the Task 2 checkpoint is authoritative over the bytecode-derived verdict. The checkpoint confirmed nothing observed contradicts PASS.

### Task 2 — Three-theme gallery sign-off, full-library smoke pass, D-03 decision (blocking checkpoint, PASS)

- **PRIM-16 (gallery, 3 themes): PASS.** Each primitive (surface, glow ring, thumb, groove, gloss, shadow) reads as genuinely glass per D-01 on AeroBlue, AeroDark, and Classic; Classic's opaque tokens confirmed not rendering as a flat block.
- **PRIM-17 (full-library smoke pass, ~50 sections, 3 themes): PASS.** No broken/ugly rendering from the `GlassModifiers.kt` fixes (Plan 03); "slightly more glass" changes accepted per D-05, not treated as regressions.
- **PRIM-18 (slider spike): PASS**, confirmed matching the Task 1 bytecode verdict — nothing observed live contradicts it.
- **D-03 (glassEffect elevation): REVIVE confirmed final.** The dropShadow wired in Plan 03 adds legible depth to cards/panels across all three themes without reading as noise.
- **Non-100% DPI pass: waived by the reviewer** (would require altering the whole desktop's display scale in this environment) — a deliberate, recorded scope reduction of that backstop `must_have`, not a silent skip.
- Screenshot evidence for the review is captured under `.planning/phases/16-foundation-aero-primitives-layer/signoff-capture/` (per-theme scroll/top captures, glow-fix zoom comparisons, soft-calibration zoom comparisons).

## Deviations from Plan

### Auto-fixed Issues (Rule 1 — bug found during human sign-off)

**1. [Rule 1 - Bug] `aeroGlowRing` produced no visible glow — indistinguishable from a plain `aeroSurface`**

- **Found during:** Task 2's PRIM-16 gallery three-theme sign-off.
- **Root causes (two independent defects):**
  1. The glow's outer bloom used a single `Brush.radialGradient` centered on the box — for any non-square box its bright stop landed in the card interior rather than at the perimeter, and `hoverGlow`'s original derivation (`primary.lighten(0.30f)` at the time) was too close in hue/luminance to the surrounding surface fill to read as distinct even where it did show.
  2. The gallery demo applied `.aeroSurface(...).aeroGlowRing(...)` — `aeroSurface`'s own outermost `.clip(shape)` silently erased the entire out-of-bounds bloom before it could render, since `aeroGlowRing`'s bloom is deliberately drawn outside layout bounds via an unclipped `drawBehind`.
- **Fix (4 atomic commits):**
  - `81253d3` — Replaced the single center-anchored radial gradient with a multi-ring concentric bloom (`GLOW_RING_BLOOM_LAYERS` solid-color rings stepping outward by `GLOW_RING_BLOOM_STEP`, each fading by `GLOW_RING_BLOOM_FALLOFF`), anchored directly to the surface's own edge with no dead-zone gap; lightened `hoverGlow`'s derivation from `0.30f` to `0.75f` for initial contrast; added regression guards (luminance-contrast test for `hoverGlow` vs `fillSplitTop` across all three presets).
  - `b0c511c` — Swapped `PrimitivesSection.kt`'s gallery demo modifier order to `aeroGlowRing().aeroSurface()` (glow outside the clip); added a USAGE CONTRACT line to `aeroGlowRing`'s KDoc documenting that it must be applied outside any clipping modifier — binding guidance for Phase 17-19 component authors.
  - `d815f7a` — First soften pass: `0.75f` read as a blown-out near-white hard rim, not a soft focus glow. Reduced `hoverGlow` to `primary.lighten(0.45f)`, bloom base alpha `0.9f → 0.5f`, softened the inner crisp stroke's alpha; recalibrated the luminance-gap regression guard threshold `0.08f → 0.04f`.
  - `3feae28` — Second, final soften pass: `hoverGlow` `0.45f → 0.30f` (back to the original hue target, now correctly visible thanks to the bloom-geometry and clip-order fixes), `GLOW_RING_BLOOM_BASE_ALPHA` `0.5f → 0.33f`, `GLOW_RING_INNER_STROKE_ALPHA` `0.65f → 0.45f`; recalibrated the luminance-gap guard threshold `0.04f → 0.02f` (AeroDark worst-case margin verified ~0.007f above threshold).
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt`, `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt`, `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt`, `library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt`, `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt`.
- **Commits:** `81253d3`, `b0c511c`, `d815f7a`, `3feae28`.
- **Final result:** the reviewer confirmed the final glow level as a moderate, tasteful Aero focus glow, visibly distinct from a plain `aeroSurface` on AeroBlue, AeroDark, and Classic — verified via pixel measurement of the outer bloom plus zoomed visual review (`zoom-glowfix-*.png`, `zoom-soft-blue.png`, `zoom-soft2-blue.png`).
- **Regression status:** `./gradlew :library:compileKotlin :showcase:compileKotlin` both green; `./gradlew :library:test` full suite green — 270 tests, 0 failures, 0 errors, including the recalibrated `AeroOrnamentTokensTest`/`AeroSurfacePrimitivesTest` luminance guards.

No other deviations — the rest of Task 1 and Task 2 executed as planned.

## Task Commits

Each task/fix was committed atomically:

1. **Task 1: PRIM-18 M3 Slider thumb/track slot-sizing spike** — `de23c46` (feat)
2. **[Rule 1 fix] aeroGlowRing bloom geometry + hoverGlow contrast** — `81253d3` (fix)
3. **[Rule 1 fix] aeroGlowRing clip-order fix in gallery demo + USAGE CONTRACT** — `b0c511c` (fix)
4. **[Rule 1 fix] aeroGlowRing intensity soften pass 1** — `d815f7a` (fix)
5. **[Rule 1 fix] aeroGlowRing intensity soften pass 2 (final calibration)** — `3feae28` (fix)

**Plan metadata:** (this commit, following SUMMARY.md creation)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt` — created; PRIM-18 throwaway-but-evidenced spike, not wired into any real showcase screen
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — `aeroGlowRing` bloom geometry replaced (multi-ring concentric bloom), intensity constants recalibrated twice, USAGE CONTRACT KDoc added
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` — `hoverGlow` derivation recalibrated (`0.30f → 0.75f → 0.45f → 0.30f`, landing back at the original value once the real bugs were fixed)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt` — gallery demo modifier order fixed (`aeroGlowRing` outside `aeroSurface`'s clip)
- `library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt` — `hoverGlow`-vs-`fillSplitTop` luminance-contrast regression guard added and recalibrated twice alongside the intensity passes
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt` — bloom-implementation regression guards added

## Decisions Made

- PRIM-18: keep Material3 `Slider` + custom `thumb=`/`track=` slots at MEDIUM complexity for Phase 18 — bytecode-proven and human-confirmed PASS, closing the phase's one genuinely open technical question (16-RESEARCH.md Assumption A7 / Open Question 1).
- D-03: `glassEffect(elevation)`'s revived `dropShadow` (wired in Plan 03) stays as final — improves cards/panels across all three themes.
- `aeroGlowRing` must be applied outside (before) any clipping modifier in the chain — now a documented USAGE CONTRACT, binding for every Phase 17-19 component that pairs a hover/focus glow with a clipped surface.
- Non-100% DPI smoke pass explicitly waived by the reviewer for this environment; not performed, recorded as a scope reduction rather than silently skipped.

## Issues Encountered

- The `aeroGlowRing` visibility defect (see Deviations) required two rounds of intensity recalibration after the initial fix overcorrected to a blown-out near-white rim — resolved within the same checkpoint session, no re-spawn needed.

## Next Phase Readiness

- Phase 16 (Foundation — Aero Primitives Layer) is now complete: all five plans (16-01 through 16-05) executed, PRIM-01 through PRIM-18 requirements covered.
- Phase 18 (Range) can proceed with `AeroSlider`'s restyle using M3 `Slider` + custom `thumb=`/`track=` slots at MEDIUM complexity (not HIGH), reusing `aeroThumbSurface`/`aeroGroove`/`aeroGlowRing` from this phase; must add `@OptIn(ExperimentalMaterial3Api::class)` per the spike's discrepancy note.
- `aeroGlowRing`'s USAGE CONTRACT (apply outside any clip) is load-bearing guidance for every interactive Phase 17-19 component (buttons, switches, segmented controls, sliders, list items) that pairs hover/focus glow with a clipped own-surface.
- Full `./gradlew :library:test` suite green: 270 tests, 0 failures, 0 errors. `./gradlew :showcase:compileKotlin` green.

---
*Phase: 16-foundation-aero-primitives-layer*
*Completed: 2026-07-22*

## Self-Check: PASSED

- FOUND: showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
- FOUND: showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
- FOUND commit: de23c46
- FOUND commit: 81253d3
- FOUND commit: b0c511c
- FOUND commit: d815f7a
- FOUND commit: 3feae28
