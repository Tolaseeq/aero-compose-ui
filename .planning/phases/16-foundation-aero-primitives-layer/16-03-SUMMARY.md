---
phase: 16-foundation-aero-primitives-layer
plan: 03
subsystem: ui
tags: [compose-desktop, glass-modifiers, dropshadow, drawWithCache, gradient, kotlin]

# Dependency graph
requires:
  - phase: 16-01
    provides: aeroSurface spine, AeroOrnamentTokens, Color.lighten/darken, ScratchAeroShadowProof-proven dropShadow signature
provides:
  - "GlassModifiers.kt with all three confirmed defects fixed in place, signatures preserved"
  - "glassSurface's gloss gradient stop proportional to size.height (PRIM-09)"
  - "glassSurface's 1.dp border rendering at full thickness via outermost clip + half-width stroke inset (PRIM-10/PRIM-12)"
  - "glassEffect's elevation parameter wired to a real dropShadow, no dead API surface (PRIM-11)"
  - "glassSurface + glassPanel gloss/border geometry cached via drawWithCache instead of reallocated per-frame in drawBehind (PRIM-13 discipline, T-16-04 mitigation)"
  - "GlassModifiersTest.kt: render-without-exception + source-level regression guards"
affects: [17-buttons, 18-range, 19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "glassSurface/glassPanel now build Brush + geometry once in drawWithCache { onDrawBehind { ... } }, not per-frame in drawBehind {}"
    - ".clip(shape) placed first (outermost) in a Modifier chain whose subsequent draw block paints a border stroke, so the clip cannot remove the stroke's outer half"
    - "Stroke width inset by half itself (strokeInset = strokeWidthPx / 2) as a belt-and-suspenders guard independent of clip ordering"
    - "glassEffect's dropShadow(shape, Shadow(radius = elevation, ...)) precedes .background(), matching ScratchAeroShadowProof's proven ordering"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt

key-decisions:
  - "glassEffect(elevation) revived (source-compatible dropShadow wiring), not removed — the two elevation = 2.dp call sites (AeroSlider.kt:65, AeroRangeSlider.kt:286) keep compiling unchanged; per D-03, 16-05's three-theme review confirms revive or requests removal"
  - "Gloss fraction for glassSurface set to 0.32 (D-01's ~30-35% band), deliberately distinct from glassPanel's heavier 0.55 fraction — copied the size.height * fraction idiom only, not the value"
  - "Migrated glassPanel to drawWithCache alongside glassSurface (not strictly named in the plan's action list, but explicitly named by the threat model's T-16-04 mitigation covering both functions) with zero value/visual changes — Brush construction only, same 0.55f fraction and Color.Transparent fade target preserved verbatim"

requirements-completed: [PRIM-09, PRIM-10, PRIM-11]

coverage:
  - id: D1
    description: "glassSurface's gloss gradient endY is size.height * 0.32 (proportional), not a fixed pixel literal — completes on short controls instead of banding (PRIM-09)"
    requirement: "PRIM-09"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassSurfaceGlossStopIsSizeProportionalNotAFixedPixelLiteral"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassSurfaceRendersWithoutExceptionOnAllThreePresets"
        status: pass
    human_judgment: false
  - id: D2
    description: "glassSurface's declared 1.dp border renders at full thickness — .clip(shape) is outermost and the stroke is inset by half its width (PRIM-10/PRIM-12)"
    requirement: "PRIM-10"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassSurfaceClipIsOutermostPaintAffectingModifier"
        status: pass
    human_judgment: true
    rationale: "The source-level regression guard proves the code structure (clip-before-draw, size-proportional stop) is correct, but whether the border visually reads as full 1.dp thickness and the gloss reads as a proper fade (vs. a subjective judgment of 'looks right') across all three themes is deferred to 16-05's three-theme review per the plan's own design (D-04/D-05)."
  - id: D3
    description: "glassEffect's elevation parameter draws a real dropShadow — no dead/no-op parameter remains (PRIM-11)"
    requirement: "PRIM-11"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassEffectElevationIsWiredToARealDropShadow"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassEffectRendersWithoutExceptionOnAllThreePresets"
        status: pass
    human_judgment: true
    rationale: "Whether the revived dropShadow visually improves cards/panels enough to justify keeping the parameter (vs. reading as noise) is explicitly deferred to 16-05's three-theme review per D-03 — a judgment call, not a compile/render assertion."
  - id: D4
    description: "glassSurface, glassPanel, glassEffect render without exception under AeroBlue, AeroDark, and Classic after the fixes"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassEffectRendersWithoutExceptionOnAllThreePresets"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassPanelRendersWithoutExceptionOnAllThreePresets"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt#glassSurfaceRendersWithoutExceptionOnAllThreePresets"
        status: pass
      - kind: integration
        ref: "./gradlew :library:test (full suite, 253 tests)"
        status: pass
    human_judgment: false

duration: 10min
completed: 2026-07-22
status: complete
---

# Phase 16 Plan 03: GlassModifiers Fixes Summary

**Fixed all three confirmed GlassModifiers.kt defects in place — proportional gloss stop, full-thickness border via outermost clip, and a revived (not dead) glassEffect elevation dropShadow — plus migrated glassSurface/glassPanel to drawWithCache and added a dedicated regression-guard test file.**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-07-22T14:24:00Z
- **Completed:** 2026-07-22T14:32:04Z
- **Tasks:** 1
- **Files modified:** 2 (1 modified, 1 created)

## Accomplishments
- `glassSurface`'s gloss gradient now fades over `size.height * 0.32f` instead of a hardcoded `endY = 100f` pixel literal (PRIM-09) — the gloss completes on short controls (e.g. an 18.dp `AeroSwitch` track) instead of banding.
- `glassSurface`'s modifier chain reorders `.clip(shape)` to be first (outermost), and the 1.dp border stroke is additionally inset by half its width — the declared 1.dp border now renders at full thickness (PRIM-10/PRIM-12).
- `glassEffect`'s previously dead `elevation` parameter now drives a real `dropShadow(shape, Shadow(radius = elevation, ...))` applied before the background fill, matching the proven `ScratchAeroShadowProof.kt` ordering — the existing `elevation = 2.dp` call sites in `AeroSlider.kt` and `AeroRangeSlider.kt` keep compiling unchanged and now render an actual shadow (PRIM-11, revive path).
- `glassSurface` and `glassPanel` both moved their brush/geometry construction from per-frame `drawBehind {}` into `drawWithCache { onDrawBehind { ... } }` — addresses the threat model's T-16-04 per-frame-allocation mitigation with zero visual change to `glassPanel`.
- New `GlassModifiersTest.kt`: 6 tests — three render-without-exception smoke tests (one per modifier, across all three `AeroColorScheme` presets) and three source-level regression guards (proportional gloss stop, clip-before-draw ordering, dropShadow wiring) that read `GlassModifiers.kt`'s own source text so the exact PRIM-09/10 defects cannot silently reappear.
- Full `:library:test` suite stays green — 253 tests total (up from the 232 baseline before Phase 16, plus this plan's 6 new tests and Plan 01's additions).
- `:showcase:compileKotlin` verified green — confirms the ~40 out-of-scope consumers of these modifiers (via `FoundationSection.kt`, `AeroCard`, `AeroToastHost`, etc.) still compile against the corrected, signature-preserved API.

## Task Commits

Each task was committed atomically:

1. **Task 1: Repair glassSurface gloss + border clip order; resolve glassEffect elevation** - `51c932f` (fix)

**Plan metadata:** committed separately by `state.record-session` / `roadmap.update-plan-progress` flow below.

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` - `glassSurface` gloss/border/clip fixes, `glassEffect` elevation revive, `glassPanel`+`glassSurface` migrated to `drawWithCache`
- `library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt` - render smoke tests (3 modifiers × 3 themes) + 3 source-level regression guards

## Decisions Made
- **glassEffect(elevation) revived, not removed** — source-compatible; the two existing `elevation = 2.dp` call sites keep compiling. Per D-03, 16-05's three-theme review has the final call on keep-vs-remove after seeing it rendered.
- **Gloss fraction = 0.32** for `glassSurface`, inside D-01's ~30-35% band, deliberately not copying `glassPanel`'s heavier 0.55 fraction verbatim (per the plan's explicit instruction — only the `size.height * fraction` idiom was copied, not the value).
- **Migrated `glassPanel` to `drawWithCache` too**, even though the plan's task action text only named `glassSurface` — the threat model's T-16-04 register explicitly names "Per-frame Brush allocation in `glassSurface`/`glassPanel`" with disposition `mitigate`, so this is Rule 2 (auto-add missing critical functionality per the threat register) rather than scope creep. Zero visual/value change: same 0.55f fraction, same `Color.Transparent` fade target, only the allocation strategy changed.

## Deviations from Plan

None beyond the threat-model-driven `glassPanel` `drawWithCache` migration documented above under Decisions Made (which is itself an explicit Rule 2 application, not an undocumented deviation).

## Issues Encountered
- First draft of `GlassModifiersTest.kt`'s `glassSurfaceClipIsOutermostPaintAffectingModifier` test searched for the literal substring `.drawWithCache(` (with parentheses), but the actual code uses Kotlin's trailing-lambda call syntax `.drawWithCache { ... }` (no parentheses) — the search never matched, both `.drawWithCache(` and `.drawBehind(` returned -1, and the test failed on the "must have a draw block" assertion. Fixed by searching for `.drawWithCache`/`.drawBehind` without the trailing paren before committing; verified failing-then-passing during development (this is exactly the kind of regression guard the acceptance criteria requires — confirmed it can actually fail).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- The three PRIM-09/10/11 defects are fixed with signatures preserved; `glassSurface`, `glassPanel`, `glassEffect` are ready for the ~40 out-of-scope consumers to re-render against on the next full build, and for Phases 17-19's new components to build on a corrected foundation.
- 16-05 (verification/spike/smoke phase) still owns the final revive-vs-remove call for `glassEffect(elevation)` (D-03) and the visual sign-off on whether the border/gloss changes read correctly across AeroBlue/AeroDark/Classic (D-04/D-05) — this plan's job was the mechanical/structural fix + regression guard, not the aesthetic verdict.
- No blockers for the remaining Phase 16 plans.

## Known Stubs

None.

---
*Phase: 16-foundation-aero-primitives-layer*
*Completed: 2026-07-22*
