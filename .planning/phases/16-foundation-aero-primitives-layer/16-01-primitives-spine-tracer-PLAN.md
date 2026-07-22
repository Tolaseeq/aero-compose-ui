---
phase: 16-foundation-aero-primitives-layer
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
  - library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt
  - library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt
  - library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
  - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
autonomous: true
requirements: [PRIM-01, PRIM-02, PRIM-03, PRIM-04, PRIM-05, PRIM-12, PRIM-13, PRIM-14, PRIM-16]
must_haves:
  truths:
    - "Color.lighten(amount)/darken(amount) RGB-mix a Color toward White/Black (alpha preserved), producing a brighter/darker Color even on Classic's fully-opaque tokens (per D-01, PRIM-01)"
    - "AeroOrnamentTokens.derive(base) returns a non-null token set with distinct values for AeroBlue, AeroDark, and Classic — no hand-authored per-theme literals (PRIM-02)"
    - "Every existing AeroColorScheme constructor call (positional and named) still compiles unchanged; ornamentOverride is a trailing, defaulted (null) field (PRIM-03)"
    - "AeroTheme.ornaments resolves base.ornamentOverride when non-null, else AeroOrnamentTokens.derive(base) — one accessor, escape-hatch honored (PRIM-03)"
    - "A single internal DrawScope.drawAeroSurfaceCore(style, cornerPx) authors the surface fill+gloss+bevel+rim; Modifier.aeroSurface(style, shape) is the only Box-owning exposure and drawAeroSurfaceCore is callable directly inside a Canvas DrawScope (PRIM-05)"
    - ".clip(shape) is the outermost paint-affecting modifier in Modifier.aeroSurface's chain — no per-call-site clip re-derivation (PRIM-12)"
    - "The PrimitivesSection surface demo renders the aeroSurface primitive and is registered in ShowcaseApp so it is reachable in the running showcase (PRIM-16 vehicle, focal point is the rendered surface per UI-SPEC)"
    - "populated: the gallery surface DemoBox shows the aeroSurface primitive legibly under each of the three themes via the existing ThemeSwitcher (UI-SPEC E1/E2 covered)"
    - { statement: "The Primitives gallery renders inside the showcase's existing vertically scrollable Column so the growing demo grid never clips", verification: backstop }
    - { statement: "aeroSurface ornamentation renders inset within the demo-card bounds (or outside layout bounds via unclipped draw), never expanding the card, across all three themes", verification: backstop }
  artifacts:
    - library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
  key_links:
    - "AeroTheme.ornaments -> AeroOrnamentTokens.derive -> Color.lighten/darken (token derivation chain, breaks silently on Classic if alpha-based)"
    - "AeroSurfaceStyle -> drawAeroSurfaceCore -> Modifier.aeroSurface -> PrimitivesSection demo (the surface render path proven end-to-end here)"
    - "ShowcaseApp registration of PrimitivesSection (without it the D-04 gallery is unreachable)"
  prohibitions:
    - { statement: "MUST NOT fade any new gradient to a hardcoded Color.Transparent — every fade targets baseColor.copy(alpha = 0f) so Classic's opaque tokens degrade to less-of-themselves, not a flat colored block (PRIM-14)", flagged: true }
    - { statement: "MUST NOT express any gradient stop as a bare Float pixel literal for startY/endY/startX/endX — proportional size.height*fraction / size.width*fraction only (PRIM-09 anti-pattern; VER-01 discipline starts here)", flagged: true }
    - { statement: "MUST NOT rebuild Brush/Path geometry inside the per-frame onDrawBehind — size/cornerRadius/style-dependent geometry is built once in drawWithCache (PRIM-13)", flagged: true }
    - { statement: "MUST NOT add a new raw Color field to AeroColorScheme's 23-field constructor — new visual tokens come from AeroOrnamentTokens.derive(base), only the trailing ornamentOverride escape-hatch is added (PRIM-03 source-compatibility)", flagged: true }
    - { statement: "MUST NOT restyle or modify any of the eight target components (AeroButton/AeroOutlinedButton/AeroSwitch/AeroSegmentedControl/AeroSlider/AeroRangeSlider/AeroProgressBar/AeroListItem) in this phase — that is Phases 17-19", flagged: true }
    - { statement: "MUST NOT change any existing component's default size or corner radius (VER-03 / Pitfall 7 out-of-scope guard)", flagged: true }
  assumptions:
    - "Edge-probe classified all 18 PRIM rows as unclassified (Russian requirement text unparsed); PRIM-01/02/03/04/05/12/13/14/16 edges here are derived from 16-RESEARCH.md + 16-UI-SPEC.md, not machine-classified — flagged, not silently backstopped."
    - "AeroSurfaceStyle field names/shape, AeroOrnamentTokens field set, and derive() lighten/darken magnitudes are Claude's-discretion starting points (RESEARCH A1/A2/A4); calibrated to D-01's moderate ~30-35% gloss target and re-reviewed at 16-05 three-theme sign-off."
---

<objective>
Establish the Aero primitives spine end-to-end: the color-math + ornament-token layer, a source-compatible AeroColorScheme escape hatch, the declarative AeroSurfaceStyle, the single drawAeroSurfaceCore draw function exposed as Modifier.aeroSurface, and a Primitives showcase gallery wired into the running showcase — proven across all three themes.

Purpose: Prove the whole four-layer architecture (tokens -> style -> single draw core -> showcase) on one vertical slice before expanding to glow/thumb/groove. Catches an architectural dead-end after one plan instead of after the whole phase. This is the tracer for Phase 16.
Output: ColorMath.kt, AeroOrnamentTokens.kt, AeroSurfaceStyle.kt, AeroSurfacePrimitives.kt (surface path only), AeroColorScheme + AeroTheme extensions, PrimitivesSection.kt gallery, ShowcaseApp registration, and unit tests for the token layer.
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
</context>

<artifacts_this_phase_produces>
This plan (16-01) produces:
- `internal fun Color.lighten(amount: Float): Color` and `internal fun Color.darken(amount: Float): Color` — theme/ColorMath.kt
- `public data class AeroOrnamentTokens(...)` + `companion object { public fun derive(base: AeroColorScheme): AeroOrnamentTokens }` — theme/AeroOrnamentTokens.kt
- New trailing field `public val ornamentOverride: AeroOrnamentTokens? = null` on `AeroColorScheme`
- New accessor `AeroTheme.ornaments: AeroOrnamentTokens` (@Composable @ReadOnlyComposable)
- `public data class AeroSurfaceStyle(...)` (+ presets) — theme/AeroSurfaceStyle.kt
- `internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float)` and `public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier` — theme/AeroSurfacePrimitives.kt
- `@Composable fun PrimitivesSection()` — showcase gallery (surface group)
(Later plans add aeroGlowRing/aeroThumbSurface/drawAeroThumb/groove, the GlassModifiers fixes, rememberAeroInteractionState, and the spike.)
</artifacts_this_phase_produces>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: End-to-end aeroSurface spine — tokens through showcase, three themes</name>
  <files>
    library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt,
    library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt,
    library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt,
    library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt,
    library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt,
    library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt,
    library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt,
    library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt,
    library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt,
    showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt,
    showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
  </files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt (analog for package/imports, Modifier-extension shape, and the drawWithCache-vs-drawBehind lesson; glassPanel's proportional size.height*fraction gloss idiom to mirror, NOT its 0.55 fraction)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt (23-field data class, named-arg presets ending panelBackground; Classic tokens are opaque 0xFF..., AeroBlue/AeroDark translucent — the fact that forces RGB-mix in derive())
    - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt (LocalAeroColors + AeroTheme object accessor pattern to mirror for AeroTheme.ornaments)
    - library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt (existing test to extend for ornamentOverride)
    - showcase/src/main/kotlin/com/mordred/showcase/sections/FoundationSection.kt (exact DemoBox-per-variant Row/Column gallery precedent to extend)
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt (section-invocation list around line 82-100 + the ThemeSwitcher wiring that toggles AeroColorScheme)
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt (compile-proven dropShadow/innerShadow/Shadow/DpOffset import paths — the corrected source of truth if a native shadow is used in the surface rim)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (Architecture Pattern 1/2, Code Examples for lighten/darken and derive; magnitudes are ASSUMED)
    - .planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md (per-file analog assignments and line-numbered excerpts)
  </read_first>
  <behavior>
    - ColorMath: lighten(0f)/darken(0f) return the input color unchanged; lighten(1f) -> White (alpha preserved), darken(1f) -> Black (alpha preserved); a mid amount on an opaque Classic token produces a channel value strictly between input and White/Black.
    - AeroOrnamentTokens.derive(AeroBlue), derive(AeroDark), derive(Classic) each return non-null tokens; at least one field differs between any two presets (proves the derivation is theme-sensitive, not constant).
    - AeroColorScheme: the three existing presets and any existing positional/named constructor call compile with zero edits; ornamentOverride defaults to null.
    - aeroSurface: composing a Box with Modifier.aeroSurface(style, shape) renders without exception under AeroBlue, AeroDark, and Classic.
  </behavior>
  <action>
    Build the vertical slice in dependency order.
    (1) theme/ColorMath.kt: add internal Color.lighten/darken as RGB-mix-toward-White/Black extension functions preserving alpha (RGB mix, never .copy(alpha=) — Classic's opaque tokens make alpha tricks invisible). Package com.mordred.aero.theme.
    (2) theme/AeroOrnamentTokens.kt: public @Immutable data class with the ornament field set (glossHighlight, bevelLight, bevelShadow, rimLight, hoverGlow, grooveShadow, fillSplitTop, fillSplitBottom — field set is Claude's discretion per CONTEXT), plus companion derive(base: AeroColorScheme) computing every field via Color.lighten/darken on base tokens. Magnitudes are ASSUMED starting points calibrated to D-01's moderate target.
    (3) theme/AeroColorScheme.kt: append a single trailing defaulted field ornamentOverride: AeroOrnamentTokens? = null after panelBackground; leave all 23 existing fields and all three presets byte-identical except the appended field.
    (4) theme/AeroTheme.kt: add an ornaments accessor on the AeroTheme object mirroring the existing colors/typography accessors — returns LocalAeroColors.current.ornamentOverride ?: AeroOrnamentTokens.derive(LocalAeroColors.current).
    (5) theme/AeroSurfaceStyle.kt: public @Immutable data class describing the surface declaratively (fill top/bottom stops, soft seam position, gloss alpha + gloss height fraction within D-01's ~30-35% band, bevel light/shadow, rim color/alpha, optional dropShadow/innerShadow Shadow params) with a rest() preset built from AeroOrnamentTokens. Field naming Claude's discretion.
    (6) theme/AeroSurfacePrimitives.kt: internal DrawScope.drawAeroSurfaceCore(style, cornerPx) authoring two-tone fill, a SOFT seam (not a hard mid-surface break, per D-01), a gloss oval clipped to the shape at ~30-35% height, an inner bevel rim, and an outer contour stroke inset by half its width so it survives the clip. Expose public Modifier.aeroSurface(style, shape) whose chain applies any dropShadow, then .clip(shape) as the outermost paint-affecting modifier, then drawWithCache { val cornerPx = ...; onDrawBehind { drawAeroSurfaceCore(style, cornerPx) } }, then any innerShadow. All fades target baseColor.copy(alpha = 0f). Build all Brush/geometry inside drawWithCache, not per-frame.
    (7) showcase PrimitivesSection.kt: extend the FoundationSection DemoBox pattern with a "Primitives" title (typography.title) and an "aeroSurface" DemoBox (typography.label caption) rendering a Box sized within the locked component-geometry band and styled with Modifier.aeroSurface.
    (8) ShowcaseApp.kt: register PrimitivesSection() in the section list near FoundationSection() so it renders under the active ThemeSwitcher theme.
    (9) Tests: ColorMathTest.kt (boundary + mid-amount assertions), AeroOrnamentTokensTest.kt (non-null + at-least-one-field-differs across the three presets), and extend AeroColorSchemeTest.kt to assert ornamentOverride defaults null and an existing-shape constructor call still compiles/returns.
    Do not reference AeroSurfaceStyle default calibration as "v1"/"placeholder" — it is the production default all Phase 17-19 components inherit (D-01 costly-reversibility).
  </action>
  <verify>
    <automated>./gradlew :library:test --tests "com.mordred.aero.theme.ColorMathTest" --tests "com.mordred.aero.theme.AeroOrnamentTokensTest" --tests "com.mordred.aero.theme.AeroColorSchemeTest" && ./gradlew :library:compileKotlin :showcase:compileKotlin</automated>
    <human-check>Launch the showcase (./gradlew :showcase:run), open the Primitives section, and confirm the aeroSurface demo reads as glass (gloss + gradient + soft seam + bevel) under AeroBlue, AeroDark, and Classic via the ThemeSwitcher — deferred to the 16-05 three-theme sign-off checkpoint.</human-check>
  </verify>
  <acceptance_criteria>
    - ColorMathTest and AeroOrnamentTokensTest exist and pass; AeroOrnamentTokensTest asserts derive() returns distinct tokens for AeroBlue vs AeroDark vs Classic.
    - AeroColorScheme.kt contains `ornamentOverride: AeroOrnamentTokens? = null` as the final constructor parameter; the pre-existing AeroColorSchemeTest assertions still pass with no edits to their construction calls.
    - AeroTheme.kt object exposes an `ornaments` accessor returning the override-or-derive result.
    - AeroSurfacePrimitives.kt declares `internal fun DrawScope.drawAeroSurfaceCore(` and `public fun Modifier.aeroSurface(`; the aeroSurface chain places `.clip(shape)` before its drawWithCache block (grep confirms `.clip(` precedes `drawWithCache` in the function body).
    - Every Brush/gradient in AeroSurfacePrimitives.kt is constructed inside a `drawWithCache` block; grep for gradient stop expressions shows `size.height` / `size.width` factors, and no fade uses `Color.Transparent`.
    - PrimitivesSection.kt declares `fun PrimitivesSection(` with an "aeroSurface" captioned DemoBox; ShowcaseApp.kt invokes `PrimitivesSection()`.
    - `./gradlew :library:compileKotlin :showcase:compileKotlin` succeeds.
  </acceptance_criteria>
  <reversibility rating="costly">AeroSurfaceStyle/drawAeroSurfaceCore defaults encode the D-01 fidelity calibration inherited by all eight Phase 17-19 components; re-tuning after consumption means re-reviewing every component on three themes (locked in CONTEXT, not re-decided here).</reversibility>
  <done>Token math, ornament derivation, source-compatible AeroColorScheme escape hatch, AeroSurfaceStyle, the single drawAeroSurfaceCore + Modifier.aeroSurface, and a registered Primitives gallery surface demo all exist and compile; the token layer is unit-tested green; the surface path is renderable under all three themes and committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none) | Single-process desktop Compose UI library — no network, no persistence, no untrusted external input crosses into this phase's code. AeroSurfaceStyle/AeroOrnamentTokens are internal Kotlin data classes constructed by library authors, not parsed from external data. |

## STRIDE Threat Register (ASVS L1)

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-16-01 | Tampering | New library API surface (transitive dependency floor on JitPack consumers) | low | mitigate | Zero new external dependencies added; only APIs in the already-pinned CMP 1.11.1 / Material3 1.9.0 BOM are used (RESEARCH Package Legitimacy Audit: not applicable). |
| T-16-02 | Denial of Service (local) | Draw-time allocation / animation | low | mitigate | Geometry/brushes built in drawWithCache (PRIM-13), no rememberInfiniteTransition introduced in this plan. |

No high/critical threats apply to a pure drawing/token layer; no blocking security gate required (ASVS L1, block-on-high).
</threat_model>

<verification>
- `./gradlew :library:test --tests "com.mordred.aero.theme.*"` green (fast scoped run).
- `./gradlew :library:compileKotlin :showcase:compileKotlin` succeeds.
- Existing full suite regression is checked at wave merge (16-03/16-05 run `./gradlew :library:test`).
</verification>

<success_criteria>
Color.lighten/darken + AeroOrnamentTokens.derive produce three-theme-distinct tokens (tested); AeroColorScheme carries a source-compatible trailing ornamentOverride; a single drawAeroSurfaceCore backs Modifier.aeroSurface with a centralized clip and cached geometry; the Primitives gallery surface demo is registered and renders on all three themes.
</success_criteria>

<output>
Create `.planning/phases/16-foundation-aero-primitives-layer/16-01-SUMMARY.md` when done.
</output>
