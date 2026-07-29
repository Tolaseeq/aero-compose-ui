---
phase: 16-foundation-aero-primitives-layer
plan: 05
type: execute
wave: 3
depends_on: [16-01, 16-02, 16-03]
files_modified:
  - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
autonomous: false
requirements: [PRIM-16, PRIM-17, PRIM-18]
must_haves:
  truths:
    - "A ScratchSliderSlotSpike.kt composable feeds a custom-sized thumb=/track= slot (consuming aeroThumbSurface + the groove primitive) into a Material3 Slider and its KDoc records the verbatim pass/fail verdict on whether M3's internal layout math clips or misaligns the custom-sized slots (PRIM-18)"
    - "The spike result is a hard gate for Phase 18: PASS keeps AeroSlider on M3 + custom slots (MEDIUM); FAIL documents the fallback to full M3 removal reusing AeroRangeSlider's drag pattern (HIGH)"
    - "A full-library smoke pass visits all ~50 existing showcase sections under AeroBlue, AeroDark, and Classic and shows no broken/ugly rendering from the GlassModifiers.kt fixes (PRIM-17)"
    - "The dedicated Primitives gallery (surface, glow ring, thumb, groove, gloss, shadow) is signed off on all three themes as genuinely glass per D-01 (moderate spirit-of-Aero: ~30-35% gloss, soft seam, subtle bevel), not modern-flat/outline (PRIM-16)"
    - "The glassEffect(elevation) revive-vs-remove call (D-03) is confirmed on this three-theme review — revive kept if the shadow improves cards/panels, else removed"
    - "populated: the gallery and the full showcase render legibly across all three themes at the sign-off (UI-SPEC E1/E2 covered)"
    - { statement: "The full showcase renders in its vertically scrollable container across all three themes with no clipped/overflowing section from the GlassModifiers re-render (blast-radius review)", verification: backstop }
    - { statement: "The M3 AeroSlider spike slot and every gallery demo stay within their layout-card bounds across states on all three themes (ornamentation insets or draws outside bounds, never expands the card — Pitfall 7)", verification: backstop }
    - { statement: "caption row width fits the demo-card column at all three theme densities (AeroBlue/AeroDark/Classic) without pushing layout", verification: backstop }
    - { statement: "At least one smoke pass is run at a non-100% DPI scale to catch scale-dependent gloss/border artifacts (carries SHW-16 discipline forward)", verification: backstop }
  artifacts:
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
  key_links:
    - "aeroThumbSurface + groove primitive (16-02) -> M3 Slider thumb=/track= slots (the spike's subject; gates Phase 18 architecture)"
    - "GlassModifiers.kt fixes (16-03) -> ~40 out-of-scope components (the smoke pass surface)"
  prohibitions:
    - { statement: "MUST NOT wire ScratchSliderSlotSpike into any real showcase screen or component — it is a throwaway-but-evidenced compile+visual proof like ScratchAeroShadowProof.kt; the SCOPE GUARD KDoc comment must say so", flagged: true }
    - { statement: "MUST NOT record a spike PASS without exercising the actual custom-sized slot inside a real M3 Slider — a repro that does not drive the slot path gives a false sign-off (the v2.0.3 false-positive lesson)", flagged: true }
    - { statement: "MUST NOT treat 'looks slightly more glass than before' as a smoke-pass regression — only broken or ugly rendering is a regression (D-05); do not hand-tune GlassModifiers to suppress expected change", flagged: true }
    - { statement: "MUST NOT sign off the gallery as glass if it reads as Feather-style outline or Material3-flat — the D-01 moderate-Aero bar (gloss + gradient + soft seam + bevel + depth) is the acceptance floor", flagged: true }
  assumptions:
    - "PRIM-18 spike outcome is UNKNOWN by design (RESEARCH A7 / Open Question 1) — it is the one genuine open technical question; this plan runs it, it does not assume the result."
    - "PRIM-16/17 are manual three-theme sign-offs with no viable automated pixel-diff on Compose Desktop (RESEARCH Validation Architecture; STACK Q6) — human checkpoints, justified."
    - "PRIM-16/17/18 edges derived from 16-RESEARCH.md + STATE.md exit-item posture, not the edge-probe (all 18 PRIM rows unclassified) — flagged."
---

<objective>
Close the phase's verification gates: run the M3 Slider thumb/track slot-sizing spike (the one open technical question), then a full-library smoke pass and the dedicated Primitives gallery three-theme sign-off — and confirm the glassEffect revive-vs-remove call on that review.

Purpose: The spike gates Phase 18's AeroSlider architecture; the smoke pass proves the GlassModifiers blast radius caused no breakage; the gallery sign-off is the mechanism that answers this project's false-positive-sign-off history (v2.0.3). These are Phase 16 exit items.
Output: ScratchSliderSlotSpike.kt with a documented verdict, plus human sign-off on the smoke pass and gallery across three themes.
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
@.planning/phases/16-foundation-aero-primitives-layer/16-02-SUMMARY.md
@.planning/phases/16-foundation-aero-primitives-layer/16-03-SUMMARY.md
</context>

<artifacts_this_phase_produces>
This plan (16-05) produces:
- `showcase/.../scratch/ScratchSliderSlotSpike.kt` — throwaway-but-evidenced PRIM-18 spike with the verbatim pass/fail verdict in its KDoc
- Human three-theme sign-off records for PRIM-16 (gallery) and PRIM-17 (full-library smoke), and the confirmed D-03 glassEffect disposition
</artifacts_this_phase_produces>

<tasks>

<task type="auto">
  <name>Task 1: PRIM-18 M3 Slider thumb/track slot-sizing spike</name>
  <files>
    showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
  </files>
  <read_first>
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt (exact throwaway-but-compile-proven pattern: package, internal @Composable fun, heavy KDoc recording the verdict, SCOPE GUARD comment)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt (aeroThumbSurface + groove primitive from 16-02 that the custom slots consume)
    - .planning/phases/16-foundation-aero-primitives-layer/16-RESEARCH.md (Open Question 1, Architecture Q5 Context7-verified thumb=/track= slot API; target ~16-18.dp thumb per FEATURES A12; Pitfall 6 M3-behavior list)
    - library/build.gradle.kts (line 22: Material3 1.9.0 explicit stable pin — the Slider source)
  </read_first>
  <behavior>
    - The spike composes a Material3 Slider with a custom thumb slot sized ~16-18.dp (raised aeroThumbSurface) and a custom track slot (recessed groove), and is visually inspected for clipping/misalignment inside M3's internal layout math.
    - The spike file compiles against the real Material3 1.9.0 artifact and its KDoc records the verbatim PASS (slots fit cleanly) or FAIL (clip/misalign, with the AeroSlider full-M3-removal fallback noted) verdict.
  </behavior>
  <action>
    Create ScratchSliderSlotSpike.kt in the showcase scratch package mirroring ScratchAeroShadowProof.kt: an internal @Composable fun ScratchSliderSlotSpike() that builds a Material3 Slider supplying a custom thumb = { ... } slot (raised aeroThumbSurface at ~16-18.dp) and a custom track = { ... } slot (recessed groove primitive), sized meaningfully differently from SliderDefaults.Thumb/Track. Add a heavy KDoc block documenting the PRIM-18 verdict verbatim (pass = keep M3 + custom slots at MEDIUM for Phase 18; fail = fallback to full M3 removal reusing AeroRangeSlider's awaitPointerEventScope drag pattern at HIGH) and a SCOPE GUARD comment forbidding wiring it into any real showcase screen. Compile and visually inspect; write the actual observed result into the KDoc — do not assume the outcome.
  </action>
  <verify>
    <automated>./gradlew :showcase:compileKotlin</automated>
    <human-check>Launch the spike composable, confirm whether the custom-sized thumb/track slots render inside M3's Slider without clipping or misalignment, and confirm the KDoc records the actual observed verdict.</human-check>
  </verify>
  <acceptance_criteria>
    - ScratchSliderSlotSpike.kt exists under showcase/.../scratch/, compiles against Material3 1.9.0, and contains a Material3 `Slider(` call with custom `thumb =` and `track =` slots consuming `aeroThumbSurface`/the groove primitive.
    - The file KDoc contains an explicit PRIM-18 PASS/FAIL verdict sentence and a SCOPE GUARD line; grep confirms the spike is not invoked from ShowcaseApp.kt or any section file.
  </acceptance_criteria>
  <reversibility rating="costly">Running the spike is reversible (throwaway evidence-gathering); its verdict informs the Phase 18 AeroSlider architecture decision (keep-M3 vs full-M3-removal), but that commitment is made in Phase 18, not walked through here. The following human-verify checkpoint reviews the recorded verdict before the phase closes.</reversibility>
  <done>The PRIM-18 spike compiles, exercises real custom-sized M3 slots, and its KDoc records the actual observed pass/fail verdict with the Phase 18 consequence; committed.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Three-theme gallery sign-off + full-library smoke pass + D-03 decision</name>
  <what-built>
    The full Aero primitives layer: token math + ornament derivation, AeroSurfaceStyle, drawAeroSurfaceCore/aeroSurface/aeroGlowRing/thumb/groove, the three GlassModifiers.kt fixes, rememberAeroInteractionState, the dedicated Primitives gallery, and the PRIM-18 slot spike.
  </what-built>
  <how-to-verify>
    1. Run the showcase: `./gradlew :showcase:run`.
    2. PRIM-16 gallery sign-off: open the Primitives section and, using the ThemeSwitcher, confirm on AeroBlue, AeroDark, AND Classic that each primitive (surface, glow ring, thumb, groove, gloss, shadow) reads as genuinely glass per D-01 — visible gloss ~30-35% of height, a soft (not hard) two-tone seam, a subtle bevel/rim, and depth — and never as Feather-style outline or Material3-flat. Confirm Classic (opaque tokens) does NOT render a flat colored block.
    3. PRIM-17 full-library smoke pass: scroll through all ~50 existing showcase sections on each of the three themes and confirm the GlassModifiers.kt fixes caused no broken or ugly rendering (slightly-more-glass is expected and accepted, not a regression).
    4. Run at least one pass at a non-100% DPI scale (e.g. 125%/150%) to catch scale-dependent gloss/border artifacts.
    4b. Caption fit (UI-SPEC E3 backstop): on each of the three themes confirm every per-primitive/per-state caption row width fits within its demo-card column without pushing or wrapping the layout.
    5. D-03 decision: on this review, decide whether glassEffect(elevation)'s revived dropShadow improves cards/panels across the three themes (keep) or reads as noise (request removal); record the verdict.
    6. Review the ScratchSliderSlotSpike KDoc verdict (Task 1) and confirm it matches what you observe running the spike.
  </how-to-verify>
  <resume-signal>Type "approved" with the D-03 glassEffect verdict (revive/remove) and the PRIM-18 verdict (pass/fail), or describe the broken/ugly items to fix.</resume-signal>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none) | A scratch spike composable + manual visual sign-off; no external input, network, or persistence. |

## STRIDE Threat Register (ASVS L1)

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-16-06 | Tampering | Material3 Slider slot spike (dependency surface) | low | mitigate | Spike consumes only the already-pinned Material3 1.9.0 Slider slot API; zero new dependencies (RESEARCH Package Legitimacy Audit: not applicable). |

No high/critical threats apply to a spike + visual sign-off; no blocking security gate required (ASVS L1, block-on-high). The blocking checkpoint here is a design/quality gate, not a security gate.
</threat_model>

<verification>
- `./gradlew :showcase:compileKotlin` succeeds (spike compiles).
- Human three-theme sign-off on the gallery (PRIM-16) and full-library smoke pass (PRIM-17), including a non-100% DPI pass.
- PRIM-18 spike verdict recorded in KDoc and confirmed at the checkpoint.
</verification>

<success_criteria>
The PRIM-18 spike runs and records a real verdict gating Phase 18; the full-library smoke pass shows no broken/ugly rendering from the GlassModifiers fixes; the Primitives gallery is signed off as genuinely glass on all three themes (incl. a non-100% DPI pass); the glassEffect revive-vs-remove call is confirmed.
</success_criteria>

<output>
Create `.planning/phases/16-foundation-aero-primitives-layer/16-05-SUMMARY.md` when done.
</output>
