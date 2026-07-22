---
phase: 16-foundation-aero-primitives-layer
plan: 02
type: execute
wave: 2
depends_on: [16-01]
files_modified:
  - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
  - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
autonomous: true
requirements: [PRIM-06, PRIM-07, PRIM-08, PRIM-13, PRIM-14, PRIM-16]
must_haves:
  truths:
    - "Modifier.aeroGlowRing draws a hover/focus glow as a double-stroke approximation (inner crisp + outer wider/fainter), gated to active interaction state, never a persistent rememberInfiniteTransition (PRIM-06)"
    - "A raised-thumb primitive exists as drawAeroThumb(style, radiusPx) (DrawScope) and Modifier.aeroThumbSurface — the circle-shape special case reusing drawAeroSurfaceCore's fill+gloss+bevel, not a second gradient implementation (PRIM-07)"
    - "A recessed track-groove primitive draws an inset bed with an inner shadow/darkened groove, exposed for both Modifier and direct-Canvas call sites (PRIM-08)"
    - "aeroGlowRing, aeroThumbSurface, and the groove primitive each build geometry/brushes in drawWithCache and fade to baseColor.copy(alpha = 0f), never Color.Transparent (PRIM-13, PRIM-14)"
    - "PrimitivesSection shows aeroGlowRing, aeroThumbSurface, and groove DemoBoxes alongside the surface demo, each renderable under all three themes (PRIM-16 vehicle)"
    - "populated: glow/thumb/groove demo groups render legibly in the gallery grid under AeroBlue/AeroDark/Classic (UI-SPEC E1/E2 covered)"
    - { statement: "The glow ring renders outside the thumb/component layout bounds via unclipped draw (or inset within), never by expanding the demo card or the eventual component's declared size (Pitfall 7)", verification: backstop }
  artifacts:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt
  key_links:
    - "drawAeroThumb reuses drawAeroSurfaceCore's fill/gloss/bevel geometry (shared implementation — must not fork into a second gradient copy)"
    - "aeroGlowRing gating -> AeroInteractionState booleans (consumed by Phase 17-19; here proven with static demo states)"
  prohibitions:
    - { statement: "MUST NOT implement the thumb or groove with bespoke inline gradient/bevel code — both are special-case call paths of drawAeroSurfaceCore; a second independently-maintained gradient copy is the exact drift anti-pattern this phase exists to prevent", flagged: true }
    - { statement: "MUST NOT drive aeroGlowRing from a rememberInfiniteTransition or any always-on animation — it costs zero when the interaction state is inactive (local-DoS avoidance)", flagged: true }
    - { statement: "MUST NOT fade the glow/thumb/groove gradients to Color.Transparent — baseColor.copy(alpha = 0f) only (PRIM-14)", flagged: true }
    - { statement: "MUST NOT wire any of these primitives into a real target component in this phase — the gallery demos are isolated proof surfaces (Phases 17-19 consume them)", flagged: true }
  assumptions:
    - "PRIM-06/07/08 edges derived from 16-RESEARCH.md (FEATURES A6/A8/A10/A12, Don't-Hand-Roll glow row) + 16-UI-SPEC.md, not from the edge-probe (all 18 PRIM rows returned unclassified) — flagged."
    - "Glow-ring geometry and thumb/groove geometry are Claude's-discretion (RESEARCH A4), calibrated to D-01 and re-reviewed at 16-05."
---

<objective>
Expand the proven surface slice into the remaining shared draw primitives: the hover/focus glow ring, the raised-thumb primitive, and the recessed track-groove — all as call paths of the single drawAeroSurfaceCore, added to the Primitives gallery for three-theme proof.

Purpose: Complete the drawing-primitives layer that Phases 18-19 (AeroSwitch, both sliders, AeroProgressBar) are gated on. Builds horizontally out from the 16-01 tracer without touching the token layer again.
Output: aeroGlowRing, drawAeroThumb/aeroThumbSurface, and the groove primitive in AeroSurfacePrimitives.kt, plus their gallery demos and a render smoke test.
</objective>

<execution_context>
@$HOME/.claude/gsd-core/workflows/execute-plan.md
@$HOME/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md
@.planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md
@.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md
@.planning/phases/16-foundation-aero-primitives-layer/16-UI-SPEC.md
@.planning/phases/16-foundation-aero-primitives-layer/16-01-SUMMARY.md
</context>

<artifacts_this_phase_produces>
This plan (16-02) produces:
- `public fun Modifier.aeroGlowRing(...): Modifier` (hover/focus glow, gated)
- `internal fun DrawScope.drawAeroThumb(style: AeroSurfaceStyle, radiusPx: Float)` and `public fun Modifier.aeroThumbSurface(...): Modifier`
- The recessed track-groove primitive (DrawScope draw fn + Modifier exposure)
- Gallery DemoBoxes for glow ring, thumb, and groove in PrimitivesSection.kt
</artifacts_this_phase_produces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: aeroGlowRing + raised-thumb primitive</name>
  <files>
    library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt,
    library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt
  </files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt (the 16-01 drawAeroSurfaceCore + Modifier.aeroSurface to extend and reuse)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt (style fields the thumb/glow read)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt (hoverGlow, glossHighlight, bevelLight/Shadow tokens)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (Don't-Hand-Roll "Soft glow/blur" double-stroke row; FEATURES A6/A8/A12; Pitfall 7 breathing-room guard)
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt (native shadow ordering, if a thumb dropShadow is chosen per D-02)
  </read_first>
  <behavior>
    - aeroGlowRing composed on a Box renders without exception when active=true and is a no-op (adds no visible ring) when active=false.
    - drawAeroThumb renders a circular raised surface (fill + gloss highlight top + bevel) without exception at a given radius, under all three themes.
    - A thumb built via aeroThumbSurface reuses the same fill/gloss code path as drawAeroSurfaceCore (no second gradient constructor).
  </behavior>
  <action>
    In AeroSurfacePrimitives.kt add: (1) public Modifier.aeroGlowRing(active: Boolean, ...) reading AeroTheme ornament hoverGlow, drawn as a double-stroke (inner crisp stroke + outer wider, fainter stroke fading to hoverGlow.copy(alpha = 0f)), gated so when active is false the modifier contributes no draw; geometry built in drawWithCache. (2) internal DrawScope.drawAeroThumb(style, radiusPx) that reuses drawAeroSurfaceCore's fill+gloss+bevel with CornerRadius collapsed to a single radius (circle), plus public Modifier.aeroThumbSurface(style) exposing it for Box-owning call sites. Draw glow/thumb ornamentation inset or outside layout bounds (unclipped drawBehind), never by growing declared size (Pitfall 7). Add AeroSurfacePrimitivesTest.kt using runComposeUiTest to compose each primitive under AeroBlue/AeroDark/Classic and assert no exception (renders).
  </action>
  <verify>
    <automated>./gradlew :library:test --tests "com.mordred.aero.theme.AeroSurfacePrimitivesTest" && ./gradlew :library:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - AeroSurfacePrimitives.kt declares `fun Modifier.aeroGlowRing(` and `fun DrawScope.drawAeroThumb(` and `fun Modifier.aeroThumbSurface(`.
    - drawAeroThumb calls into the shared drawAeroSurfaceCore fill/gloss path (grep shows a reference to drawAeroSurfaceCore or a shared private fill helper, not a fresh Brush.verticalGradient authored independently for the thumb).
    - AeroSurfacePrimitivesTest exercises all three themes and passes; grep confirms no `rememberInfiniteTransition` in AeroSurfacePrimitives.kt.
    - No gradient in the new code fades to `Color.Transparent`; glow/thumb geometry is built inside `drawWithCache`.
  </acceptance_criteria>
  <done>aeroGlowRing and the raised-thumb primitive exist, reuse the shared draw core, render green on three themes, and are committed.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Recessed track-groove primitive + gallery demos for glow/thumb/groove</name>
  <files>
    library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt,
    library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt,
    showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
  </files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt (drawAeroSurfaceCore, aeroGlowRing, drawAeroThumb from Task 1)
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt (lines 1-27: the Canvas + direct-DrawScope call convention the groove's direct-call path must match)
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt (the 16-01 surface DemoBox to extend)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (FEATURES A10 groove; Pitfall 5 opaque-Classic fade rule)
    - .planning/phases/16-foundation-aero-primitives-layer/16-UI-SPEC.md (## UI Considerations: gallery scroll/overflow backstops, focal-point note)
  </read_first>
  <behavior>
    - The groove primitive renders a recessed inset bed (darkened groove + inner shadow toward the top) without exception under all three themes, callable both as a Modifier and directly inside a Canvas DrawScope.
    - PrimitivesSection renders four demo groups (surface, aeroGlowRing, aeroThumbSurface, groove) in the existing DemoBox grid, each captioned with its primitive name.
  </behavior>
  <action>
    Add the recessed track-groove primitive to AeroSurfacePrimitives.kt: a DrawScope draw function (e.g. drawAeroGroove(style, cornerPx)) rendering an inset bed with a darkened groove fill and an inner-shadow cue at the top edge (grooveShadow token; native innerShadow or gradient per D-02), plus a Modifier exposure — matching the same dual Modifier/direct-Canvas convention as drawAeroSurfaceCore so AeroRangeSlider-style Canvas owners can call it directly. All fades to baseColor.copy(alpha = 0f), geometry in drawWithCache. Extend AeroSurfacePrimitivesTest with a groove render case across three themes. Extend PrimitivesSection.kt: add "aeroGlowRing" (default + active states), "aeroThumbSurface", and "groove" captioned DemoBoxes following the FoundationSection DemoBox pattern; keep the six-group grid within the existing showcase scroll container (UI-SPEC overflow backstop).
  </action>
  <verify>
    <automated>./gradlew :library:test --tests "com.mordred.aero.theme.AeroSurfacePrimitivesTest" && ./gradlew :library:compileKotlin :showcase:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - AeroSurfacePrimitives.kt declares a groove draw function and a Modifier groove exposure; the direct-call draw function has a `DrawScope` receiver (callable inside Canvas).
    - AeroSurfacePrimitivesTest covers the groove under AeroBlue/AeroDark/Classic and passes.
    - PrimitivesSection.kt contains captioned DemoBoxes for `aeroGlowRing`, `aeroThumbSurface`, and `groove` (grep for each caption string).
    - `./gradlew :showcase:compileKotlin` succeeds; no new gradient fades to `Color.Transparent`.
  </acceptance_criteria>
  <done>The recessed groove primitive exists on the shared draw core and the gallery shows all four primitive groups renderable on three themes; committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none) | Pure drawing primitives; no external input, network, or persistence. |

## STRIDE Threat Register (ASVS L1)

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-16-03 | Denial of Service (local) | aeroGlowRing animation/allocation | low | mitigate | Glow gated to active interaction state, no rememberInfiniteTransition; geometry cached in drawWithCache (costs zero when inactive). |

No high/critical threats apply; no blocking security gate required (ASVS L1, block-on-high).
</threat_model>

<verification>
- `./gradlew :library:test --tests "com.mordred.aero.theme.*"` green.
- `./gradlew :library:compileKotlin :showcase:compileKotlin` succeeds.
</verification>

<success_criteria>
aeroGlowRing, the raised-thumb primitive, and the recessed groove all exist as call paths of the shared draw core, render green on all three themes in the gallery, and consume zero new dependencies.
</success_criteria>

<output>
Create `.planning/phases/16-foundation-aero-primitives-layer/16-02-SUMMARY.md` when done.
</output>
