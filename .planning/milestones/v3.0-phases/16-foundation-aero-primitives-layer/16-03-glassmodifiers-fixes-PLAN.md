---
phase: 16-foundation-aero-primitives-layer
plan: 03
type: execute
wave: 1
depends_on: []
files_modified:
  - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt
  - library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt
autonomous: true
requirements: [PRIM-09, PRIM-10, PRIM-11]
must_haves:
  truths:
    - "glassSurface's gloss gradient endY is proportional to component height (size.height * fraction), so the gloss band completes on short controls instead of banding at a fixed pixel distance (PRIM-09)"
    - "glassSurface's border renders at its full declared 1.dp thickness — the stroke is no longer clipped to an effective half-width because .clip(shape) is now outermost or the stroke is inset by half its width (PRIM-10)"
    - "glassEffect's elevation parameter either draws a real dropShadow or is removed — no dead/no-op parameter remains in the published GlassModifiers API (PRIM-11)"
    - "glassSurface, glassPanel, and glassEffect keep their existing public signatures where the elevation resolution is 'revive' (source-compatible); if 'remove', the two elevation=2.dp call sites are updated in-repo"
    - "glassSurface/glassPanel/glassEffect render without exception under AeroBlue, AeroDark, and Classic after the fixes"
  artifacts:
    - library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt
  key_links:
    - "GlassModifiers.kt is consumed by ~40 out-of-scope components + the 8 targets — the fixes re-render all of them (blast radius accepted per D-05; regression = broken/ugly only)"
  prohibitions:
    - { statement: "MUST NOT express the gloss gradient's endY (or any gradient stop) as a bare Float pixel literal — proportional size.height*fraction only (the exact PRIM-09 bug must not survive)", flagged: true }
    - { statement: "MUST NOT leave glassEffect's elevation parameter accepted-but-never-applied — a value that produces zero rendered difference is a dead parameter and is forbidden here (PRIM-11)", flagged: true }
    - { statement: "MUST NOT hand-tune the three fixes to keep the ~40 out-of-scope components pixel-identical — apply the correct fixes as-is; 'slightly more glass' is expected and accepted, only broken/ugly is a regression (D-05)", flagged: true }
    - { statement: "MUST NOT alter the glassSurface/glassPanel default corner radii or introduce new size defaults (VER-03 guard)", flagged: true }
  assumptions:
    - "PRIM-09/10/11 edges derived from 16-RESEARCH.md (Pitfalls 1-4, State-of-the-Art table) + STATE.md's confirmed three-defect audit with exact line numbers, not the edge-probe (all 18 PRIM rows unclassified) — flagged."
    - "The glassEffect(elevation) revive-vs-remove call (D-03) is Claude's discretion on the 16-05 three-theme review; this plan implements the source-compatible REVIVE (wire elevation to a real dropShadow) as the safe default, and 16-05 confirms revive or requests removal."
---

<objective>
Fix the three confirmed GlassModifiers.kt defects in place, with signatures preserved where possible: proportional gloss (no fixed-pixel gradient stop), full-thickness border (corrected clip order), and a live-or-removed glassEffect elevation (no dead parameter).

Purpose: These modifiers back ~40 out-of-scope components plus the eight targets; the defects make every glass surface subtly wrong. Fixing them is a Phase 16 exit requirement and the reason PRIM-17's full-library smoke pass exists.
Output: A corrected GlassModifiers.kt and a new GlassModifiersTest.kt render/regression guard.
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
</context>

<artifacts_this_phase_produces>
This plan (16-03) produces:
- Corrected `Modifier.glassSurface` (proportional gloss + full-thickness border + centralized clip)
- Corrected `Modifier.glassEffect` (elevation revived to a real dropShadow, or removed)
- `GlassModifiersTest.kt` — render-without-exception + proportional-stop regression guard
</artifacts_this_phase_produces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Repair glassSurface gloss + border clip order; resolve glassEffect elevation</name>
  <files>
    library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt,
    library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt
  </files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt (all three functions; glassPanel's correct proportional gloss at line 68 is the sibling pattern; glassSurface's fixed-pixel gloss stop at line 95 and its clip-after-drawBehind at line 105 are the bugs; glassEffect's unused elevation parameter at lines 26-45)
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt (compile-proven dropShadow(shape, Shadow(radius=..., color=..., offset=DpOffset(...))) signature + import paths for the elevation-revive path)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (Pitfalls 1-4; Code Examples "Dead-parameter fix pattern for glassEffect"; D-03/D-05)
    - .planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md (GlassModifiers.kt section with the exact defect line numbers)
  </read_first>
  <behavior>
    - glassSurface's gloss gradient stop scales with size.height (a shorter surface still shows a completed gloss fade, not a hard band).
    - glassSurface's declared 1.dp border renders at full thickness (the outer half is no longer clipped away).
    - glassEffect with a larger elevation produces a visibly deeper shadow than a smaller elevation (the parameter now affects the render) — or the parameter no longer exists.
    - All three modifiers render without exception under AeroBlue, AeroDark, and Classic.
  </behavior>
  <action>
    In GlassModifiers.kt:
    (1) PRIM-09 — replace the fixed-pixel gloss gradient stop inside glassSurface (the numeric literal on the gloss endY at line 95) with a proportional size.height * fraction expression, using a fraction within D-01's ~30-35% band (do NOT copy glassPanel's heavier 0.55 fraction).
    (2) PRIM-10/PRIM-12 — reorder glassSurface so .clip(shape) is the outermost paint-affecting modifier (move it before the drawBehind/drawWithCache block), and additionally inset the border stroke by half its width so the full declared thickness survives; prefer migrating the gloss/border geometry into drawWithCache while here.
    (3) PRIM-11 — resolve the dead glassEffect elevation parameter by REVIVING it (source-compatible): wire elevation into a real dropShadow(shape, Shadow(radius = elevation, color = Color.Black.copy(alpha = ...))) applied before the background, per the ScratchAeroShadowProof ordering, keeping the existing signature so the two elevation=2.dp call sites keep compiling. (The 16-05 three-theme review confirms revive or requests removal per D-03.)
    Add GlassModifiersTest.kt: runComposeUiTest cases composing each of glassEffect/glassPanel/glassSurface under all three presets asserting no exception; plus a source-level regression assertion that glassSurface's gloss stop is a size-proportional expression (see acceptance).
  </action>
  <verify>
    <automated>./gradlew :library:test --tests "com.mordred.aero.theme.GlassModifiersTest" && ./gradlew :library:test</automated>
  </verify>
  <acceptance_criteria>
    - Grepping glassSurface's gloss gradient shows the endY stop expressed with `size.height *` (proportional); no bare Float pixel literal remains on a gradient stop in glassSurface.
    - In glassSurface's modifier chain, `.clip(` appears before the `drawBehind`/`drawWithCache` block (grep line-order check).
    - glassEffect's body contains a `dropShadow(` call that consumes the `elevation` parameter (revive path), OR the `elevation` parameter is absent from the signature and both former call sites are updated.
    - GlassModifiersTest.kt exists and passes across all three presets.
    - The full `./gradlew :library:test` suite (232+ baseline) stays green — no regression in the ~40 consumers' compile/behavior.
  </acceptance_criteria>
  <reversibility rating="costly">The corrected modifiers are the shared foundation for ~40 out-of-scope components; walking back to a 'keep 40 pixel-identical' posture would mean forking the fix or reintroducing the defects (D-05, locked). The revive-vs-remove sub-decision is confirmed at 16-05.</reversibility>
  <done>glassSurface has proportional gloss and a full-thickness border with centralized clip, glassEffect's elevation is live (or removed), all three render green on three themes, and the full suite stays green; committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none) | In-place bug fixes to a pure drawing modifier file; no external input, network, or persistence. |

## STRIDE Threat Register (ASVS L1)

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-16-04 | Denial of Service (local) | Per-frame Brush allocation in glassSurface/glassPanel | low | mitigate | Migrate gloss/border geometry into drawWithCache while fixing (PRIM-13 discipline), reducing per-frame allocation. |

No high/critical threats apply; no blocking security gate required (ASVS L1, block-on-high). The revive path adds no new dependency (dropShadow ships in the pinned CMP 1.11.1 BOM).
</threat_model>

<verification>
- `./gradlew :library:test --tests "com.mordred.aero.theme.GlassModifiersTest"` green.
- Full `./gradlew :library:test` green (regression guard for the ~40 out-of-scope consumers).
</verification>

<success_criteria>
The three confirmed GlassModifiers.kt defects are fixed in place with signatures preserved on the revive path, the fixes render green on three themes, and the full library suite stays green.
</success_criteria>

<output>
Create `.planning/phases/16-foundation-aero-primitives-layer/16-03-SUMMARY.md` when done.
</output>
