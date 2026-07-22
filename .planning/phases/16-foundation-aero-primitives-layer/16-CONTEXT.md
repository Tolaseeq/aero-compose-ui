# Phase 16: Foundation — Aero Primitives Layer - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

A single, shared, three-theme-proven Aero drawing/token layer exists so every visual component phase (17–19) consumes it rather than re-deriving gradients, gloss, bevel, and grooves independently.

**In scope:** `Color.lighten()`/`darken()`; `AeroOrnamentTokens` + `derive(base)`; source-compatible `AeroColorScheme` extension via trailing `ornamentOverride`; `AeroSurfaceStyle`; one `drawAeroSurfaceCore(style, cornerPx)` exposed as `Modifier.aeroSurface()` + direct Canvas calls; `Modifier.aeroGlowRing`; raised-thumb + recessed-track-groove primitives; the three `GlassModifiers.kt` defect fixes; centralized clip; `drawWithCache` geometry/brushes; `baseColor.copy(alpha = 0f)` gradient fades; `InteractionStates.kt` → `components/common/` + `rememberAeroInteractionState()`; a dedicated Primitives showcase gallery; full-library (~50-component) smoke pass; the M3 Slider thumb/track slot-sizing spike.

**Explicitly NOT in this phase:** restyling any of the eight target components (that is Phases 17–19), any behavior/signature/default-size change, any new component capability. This layer draws primitives and proves them; it does not wire them into the eight yet.

</domain>

<decisions>
## Implementation Decisions

### Aero Fidelity Calibration
- **D-01:** The shared surface primitive is calibrated to **moderate "spirit of Aero"**, NOT literal Win7: top gloss occupying ~30–35% of component height, a **soft** two-tone seam (not a hard mid-surface break), and a subtle inner bevel/rim. This is still genuinely glass (gloss + gradient + bevel + depth), not generic modern-flat — it must not read as Feather-style outline or Material3-flat. Sits between PROJECT.md's "spirit of Aero, modern execution" and the `aero-aesthetic` memory's "lean Win7"; the user consciously chose the moderate point on that spectrum. — **Reversibility:** costly — this is baked into `drawAeroSurfaceCore`/`AeroSurfaceStyle` defaults that all eight Phase 17–19 components inherit; re-tuning after those consume it means re-reviewing every component on three themes again.

### Native Shadows vs Gradient Depth
- **D-02:** Whether native `Modifier.dropShadow`/`innerShadow` (confirmed available at CMP 1.11.1) or manual gradients render each depth cue is **Claude's discretion, decided per primitive on the three-theme review** — use whichever reads better: native for drop/recess where it looks cleaner, gradient where color control of the rim/highlight matters. Signatures + import packages are already proven in `ScratchAeroShadowProof.kt` (`dropShadow`/`innerShadow` from `androidx.compose.ui.draw`; `Shadow` from `androidx.compose.ui.graphics.shadow`; `DpOffset` from `androidx.compose.ui.unit`). Ordering rule from the scratch: `dropShadow` before `.background()`, `innerShadow` after.
- **D-03:** The dead `glassEffect(elevation)` parameter (PRIM-11) is resolved at **Claude's discretion on the review**: either revive it to draw a real `dropShadow` (source-compatible — existing `elevation = 2.dp` call sites start working, nothing breaks) or remove it (clean API, minor-breaking, acceptable in a major version). Decision folds into the blast-radius review (D-05): revive if the shadow improves cards/panels across three themes, remove if it adds noise.

### Foundation Sign-off Vehicle
- **D-04:** A **dedicated Primitives showcase gallery** is built in `:showcase` — each primitive (surface, glow ring, thumb, groove, gloss, shadow) rendered in isolation × three themes × states — and it **stays in the project as a living reference** for Phases 17–19, not thrown away. This is the mechanism satisfying PRIM-16 (every primitive proven on three themes at first iteration) and directly answers the project's false-positive-sign-off history (`feedback_repro_must_exercise_path`, the v2.0.3 lesson). It is distinct from the PRIM-17 full-library smoke pass (which exercises the existing ~50 showcase components). — **Reversibility:** reversible — additive showcase code; can be deleted or restructured without touching library API.

### GlassModifiers Fix Blast-Radius Posture
- **D-05:** Apply the correct `GlassModifiers.kt` fixes **as-is** (proportional gloss replacing `endY = 100f`, full-thickness border with corrected clip order, and the `elevation` resolution). The ~40 out-of-scope components that share `glassSurface`/`glassPanel`/`glassEffect` are **allowed to become slightly "more glass"** as a result. For PRIM-17's smoke pass, **regression means broken or ugly rendering only** — not "looks slightly different from before." Do NOT hand-tune the fixes to keep the 40 pixel-identical. — **Reversibility:** costly — the corrected modifiers are the shared foundation; walking this back to a conservative "keep 40 identical" posture would mean forking the fix or re-introducing the defects for non-target components.

### Claude's Discretion
- Per-primitive native-shadow vs gradient choice (D-02) and the `glassEffect(elevation)` revive-or-remove call (D-03), both settled on the three-theme review.
- Exact `AeroSurfaceStyle` token shape/naming, `AeroOrnamentTokens` field set, `derive(base)` lighten/darken magnitudes, glow-ring geometry, and thumb/groove geometry — all technical, calibrated to hit the D-01 "moderate" target.
- Structure/placement of the Primitives gallery within `:showcase`.
- Exact gloss fraction within the ~30–35% band (D-01 sets the target, not a hard literal).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Requirements & success criteria (primary)
- `.planning/REQUIREMENTS.md` — **PRIM-01..PRIM-18** (the 18 requirements this phase closes), lines ~27–48. Also VBTN/VRNG/VSEL/VLST sections for how downstream phases will consume the primitives.
- `.planning/ROADMAP.md` §"Phase 16: Foundation — Aero Primitives Layer" — the five explicit Success Criteria.

### Locked architecture & institutional memory (load-bearing)
- `.planning/STATE.md` §"Architecture positions locked for planning" — `drawAeroSurfaceCore` single-implementation, `AeroSlider` keeps M3 with custom slots (spike gate), `AeroOrnamentTokens.derive(base)` RGB lighten/darken + `ornamentOverride` escape hatch, PRIM-17 full-library smoke pass is a Phase 16 EXIT item, the locked design decisions.
- `.planning/STATE.md` §"Baseline Findings — why these eight look Material" and §"Glass layer defects to fix" — the confirmed three `GlassModifiers.kt` defects and the per-component current-surface audit.
- `.planning/PROJECT.md` §"Key Decisions" — `undecorated` without `transparent` rule (extends to all Popup/Dialog), single-`drawBehind` glass performance baseline, `Icon()`-direct pattern, "spirit of Aero, modern execution" fidelity note.

### Phase 15 → 16 handoff artifact
- `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` — **compile-proven** `dropShadow`/`innerShadow`/`Shadow` signatures and import packages against the real 1.11.1 jar, plus the shadow-ordering rule. This is the corrected source of truth (RESEARCH.md's package was wrong); build primitives on it, do NOT re-derive from docs.

### Research (facts, not superseded recommendations)
- `.planning/research/ARCHITECTURE.md` — the intended primitives-layer architecture (`AeroSurfaceStyle`, `drawAeroSurfaceCore`, exposure paths).
- `.planning/research/FEATURES.md` — the Aero visual-technique catalogue (two-tone fill, top gloss, bevel/rim, groove, glow).
- `.planning/research/PITFALLS.md` — pitfall-to-phase mapping (e.g. `detectDragGestures` ban PITFALL-03, `AeroScrollArea` virtualization ban, per-frame brush rebuild).
- `.planning/research/UPGRADE.md` §Part 4 — the `dropShadow`/`innerShadow`/`Shadow` signatures (MEDIUM-HIGH confidence; **already re-verified** by ScratchAeroShadowProof.kt — prefer the scratch).
- `.planning/research/STACK.md` — zero-`org.jetbrains.skia.*`-references audit of `GlassModifiers.kt`.

### Files this phase modifies/creates (verified this session)
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — the three defect fixes (PRIM-09/10/11); `glassSurface` `endY = 100f` literal (line 96), border clip order (lines 99–105), dead `elevation` param (lines 26–45).
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — 23-field `data class` + three presets (AeroBlue/AeroDark/Classic); extended source-compatibly with trailing `ornamentOverride` (PRIM-03). Note Classic tokens are **opaque** (e.g. `glassSurface = 0xFF333333`), which is why `derive` must RGB-lighten/darken, not alpha-manipulate.
- `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt` — moves to `components/common/` and gains `rememberAeroInteractionState()` (PRIM-15); currently `internal` helpers `rememberHoverState`/`rememberPressedState`/`rememberFocusState`/`animatedAlpha`, `ANIMATION_DURATION_MS = 150`.
- New: `AeroOrnamentTokens` + `AeroSurfaceStyle` + `drawAeroSurfaceCore`/`Modifier.aeroSurface`/`aeroGlowRing`/thumb/groove primitives (package placement Claude's discretion; `components/common/` exists after PRIM-15 move — no `components/common/` dir exists yet).
- New: dedicated Primitives gallery in `showcase/` (D-04).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ScratchAeroShadowProof.kt` — the proven shadow basis (see canonical refs). Consume its confirmed signature; it is a seed, not throwaway.
- `InteractionStates.kt` — existing hover/press/focus + `animatedAlpha` helpers; the base to relocate and extend into `rememberAeroInteractionState()` (PRIM-15). `AeroListItem`'s `Modifier.hoverable` + `collectIsHoveredAsState` is the reference wiring downstream components must copy (VLST-04), not reinvent.
- `glassPanel` already uses a **proportional** gloss (`endY = size.height * 0.55f`, GlassModifiers.kt:68) — the correct pattern that `glassSurface`'s `endY = 100f` literal (line 96) should mirror; note 0.55 is heavier than the D-01 ~30–35% target, so glassSurface should not just copy glassPanel's fraction.

### Established Patterns
- **Enabling-phase-then-consume** (v2.0 Phase 7): build a proven shared artifact in one phase for later phases to consume — this whole phase mirrors it.
- **Single `drawBehind` glass** — locked performance baseline; extends to `drawWithCache` for cached geometry/brushes (PRIM-13, no per-frame `Brush` rebuild).
- **`Color.Transparent` is the anti-pattern** — existing `glassPanel`/`glassSurface`/`glassEffect` all fade to `Color.Transparent`; PRIM-14 requires new gradients fade to `baseColor.copy(alpha = 0f)` so Classic (opaque tokens) doesn't render a flat colored block. The existing code is the negative example to fix, not copy.
- **`api()`-scoped public deps** — any new public modifier/token becomes a transitive floor on JitPack consumers; keep `AeroColorScheme` constructor source-compatible (PRIM-03).

### Integration Points
- `LocalAeroColors` — all glass modifiers read tokens from it inside `AeroTheme {}`; new `AeroOrnamentTokens` plug in via `AeroColorScheme` + `LocalAeroColors`.
- **Blast radius (D-05):** ~40 out-of-scope components consume `glassSurface`/`glassPanel`/`glassEffect` — fixing those modifiers re-renders all of them; PRIM-17 full-library smoke pass is the Phase 16 EXIT gate.
- **Phase 16 → 17/18/19 handoff:** the eight target components consume `drawAeroSurfaceCore`/`aeroSurface`/`aeroGlowRing`/thumb/groove + `rememberAeroInteractionState()`; the M3 Slider slot-sizing spike (PRIM-18) gates Phase 18's `AeroSlider` architecture (fallback = full M3 removal if custom slots clip/misalign).

</code_context>

<specifics>
## Specific Ideas

- Fidelity target phrased concretely: gloss ~30–35% of height, soft (not hard) seam, subtle bevel — recognizably glass, never generic-flat/outline.
- The Primitives gallery is a **permanent living reference**, not a throwaway scratch — Phases 17–19 look at it to stay visually consistent.
- Blast-radius rule of thumb for the smoke pass: "broken or ugly" is a regression; "slightly more glass than before" is expected and accepted for the 40 non-target components.
- Native shadow vs gradient is settled empirically on the three-theme review, not decided up front — so is the `glassEffect(elevation)` revive-or-remove call.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. (Component restyles are Phases 17–19 by explicit milestone design; the `AeroSlider` M3-slot spike outcome and the `glassEffect(elevation)` fate are in-scope Phase 16 items, resolved on the review, not deferred.)

</deferred>

---

*Phase: 16-foundation-aero-primitives-layer*
*Context gathered: 2026-07-22*
