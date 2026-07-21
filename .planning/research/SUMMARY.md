# Project Research Summary

**Project:** aero-compose-ui — v3.0 Glass Refinement
**Domain:** Compose Desktop (Kotlin) UI component library — Windows 7 Aero visual ornamentation over an existing Material3-based component set
**Researched:** 2026-07-21
**Confidence:** HIGH for the mandatory toolchain facts (version numbers, dates, compatibility deltas — GitHub Releases API / official CHANGELOG, machine-verified) and for bugs grounded directly in this repo's code (cited file:line). MEDIUM for exact behavior of brand-new CMP 1.9.0+ shadow APIs and the `AeroOrnamentTokens` derivation design (original synthesis, spike-verified rather than sourced). LOW explicitly flagged inline wherever only a single WebSearch source supports a claim.

**Five research documents inform this summary, not four:** STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md, and UPGRADE.md (toolchain migration). Two of the five contain conclusions that were subsequently **overturned by later research or by the user** — see "Superseded Findings" below. This summary represents the current, corrected state, not a flat concatenation of all five documents.

## Executive Summary

v3.0 is a two-part milestone with a hard ordering constraint: a mandatory toolchain upgrade (Kotlin 2.1.21→2.4.10, Compose Multiplatform 1.7.3→1.11.1, explicit stable Material3 pin) must land as its own phase, fully verified, before any visual work begins — because mixing dependency-upgrade risk with rendering-code risk makes it impossible to attribute a regression to either cause. On top of that upgraded toolchain, eight components (`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`) get restyled from flat/Material3-looking surfaces into genuine Win7 Aero glass — two-tone gradient fill, proportional top gloss, inner bevel/rim light, outer glow, and recessed track grooves — built on a repaired and extended shared "Aero primitive" layer (`GlassModifiers.kt` plus new `AeroSurfaceStyle`/`AeroSurfacePrimitives`/`AeroOrnamentTokens` types). Public API and component behavior do not change.

The recommended approach is Foundation-first, exactly mirroring the project's own successful v2.0 Phase 7 enabling-phase pattern: build one shared `Modifier.aeroSurface()`/`drawAeroSurfaceCore()` primitive (not per-component bespoke gradient code), one algorithmically-derived `AeroOrnamentTokens.derive(base)` color layer (not ~10 hand-tuned literals × 3 themes), and a shared "raised thumb" + "track groove" primitive pair that gates three-to-four components each — then let Buttons → Range → Selectors/Lists consume that layer in dependency order. The single most important architectural correction this research makes to the milestone's original framing is that `AeroSlider` does **not** need a full Material3-removal-plus-custom-drag rewrite: Material3's `Slider` exposes `thumb`/`track` composable slots, so keeping M3's drag/keyboard/step-snap/semantics machinery and only reskinning the slot paint is possible — this downgrades what was flagged as the milestone's one HIGH-complexity outlier to MEDIUM and removes an entire class of behavioral-regression risk (Pitfall 8).

Key risks, in order of leverage: (1) the shared glass layer's blast radius is wider than the eight target components — fixing `GlassModifiers.kt`'s three confirmed bugs re-renders ~40 out-of-scope components that already consume `glassSurface`/`glassPanel`/`glassEffect`, so a full-library visual smoke pass belongs at the Foundation-phase exit, not final sign-off; (2) the Kotlin 2.4.10 / CMP 1.11.1 pairing was never shipped by JetBrains as a matched pair (six weeks apart, with 1.12.x being the actual Kotlin-2.4-aligned line) and must be settled empirically by a bare `./gradlew build` before any other upgrade work, with three named, user-escalated fallbacks if it fails; (3) `Classic` theme's fully-opaque glass tokens vs. `AeroBlue`/`AeroDark`'s alpha-composited ones mean every new gradient must be designed to fade toward `baseColor.copy(alpha=0f)`, never a hardcoded `Color.Transparent`, or `Classic` renders a flat color block instead of a glass sheen; and (4) this project has direct institutional memory of a regression guard that passed two human sign-offs while shipping a wrong-cause fix (v2.0.3) — the same discipline (a guard must provably fail on unfixed code) applies to every new grep-gate and UI test this milestone adds, and doubly to porting `AeroPanelGroupRecomposeUiTest` across the CMP 1.11.0 test-dispatcher default change.

## Key Findings

### Recommended Stack

No new Gradle dependencies are required at the target toolchain. The visual work is achievable entirely with APIs already on the classpath once the upgrade lands: `Brush.verticalGradient`/`radialGradient` (already in use), the new unified `Shadow` class backing `Modifier.dropShadow`/`Modifier.innerShadow` (landed CMP 1.9.0, confirmed cross-platform including Desktop, package `androidx.compose.ui.graphics.shadow`), and `org.jetbrains.skia.*` as a bounded, isolated escape hatch only if a true Gaussian blur glow is ever pursued (not needed for this milestone's device catalog). AGSL/`RuntimeShader` is confirmed Android-only and must never appear in this codebase; the desktop equivalent (SkSL via `RuntimeEffect`) exists but is unnecessary complexity for gradient-achievable effects. No screenshot-regression testing framework (Roborazzi/Paparazzi) works on Compose Desktop — the project's proven three-theme human sign-off checklist, paired with mechanical grep-gates and `runComposeUiTest`/`captureToImage()` for structural (not pixel) assertions, remains the right verification strategy.

**Core technologies:**
- Kotlin 2.4.10 (from 2.1.21) — mandatory, user-locked, required by CMP 1.11.x's Kotlin-2.2+ language-version floor
- Compose Multiplatform (Desktop) 1.11.1 (from 1.7.3) — mandatory, user-locked latest-stable, chosen over the research-recommended conservative fallback of 1.9.3
- `org.jetbrains.compose.material3:material3` — must be **explicitly pinned** to a stable coordinate; the bare `compose.material3` alias resolves to an unacceptable 1.5.0-alpha17 at CMP 1.11.x
- `androidx.compose.ui.graphics.shadow.Shadow` (`Modifier.dropShadow`/`Modifier.innerShadow`) — the one genuinely new capability the upgrade unlocks; signature is MEDIUM-HIGH confidence (doc-mirror verified, not yet compiled against the real jar) and must be re-verified against the actual 1.11.1 artifact before `GlassModifiers.kt` production code depends on it
- `Brush.verticalGradient`/`radialGradient` with proportional (`size.*`-derived) stops — already available, zero new capability, but the existing `glassSurface` hardcodes a pixel literal (`endY = 100f`) that must be fixed as part of this milestone

### Expected Features

The milestone target is a precise, sourced "Aero device catalog" (two-tone fill, seam, top specular gloss, inner rim/bevel, outer contour, hover glow, pressed inversion, focus glow, drop-shadow-for-controls-only, track groove, raised thumb) applied consistently across all eight components, cross-checked against community Win7 recreation projects (7.css, PresentationTheme.Aero) for MEDIUM confidence and against official MS Learn documentation for HIGH-confidence part/state naming (exact pixel values were never published by Microsoft and are treated as original design proposals, not literal reproductions).

**Must have (table stakes):**
- Two-tone gradient fill + inner bevel + outer contour on every raised surface (all 8 components)
- Track "groove" recessed-channel treatment for `AeroSwitch` off-track, `AeroSlider`/`AeroRangeSlider` unfilled track, `AeroProgressBar` track
- Hover/press/focus glow wiring for the five components that currently have zero: `AeroSwitch`, `AeroSegmentedControl`, both slider thumbs, `AeroListItem`'s combined selected+hover case
- `AeroListItem` selection pill actually clipped to a rounded shape (currently unclipped — the confirmed baseline bug)

**Should have (differentiators):**
- The "seam" line and specular gloss oval devices (cheap, high visual signal)
- `AeroProgressBar` periodic sheen sweep — **locked decision: optional, default OFF** (`showSheen`-style param); always-on shimmer reads as a loading skeleton
- `AeroSegmentedControl` selected-segment recessed/pressed metaphor — **locked decision**, reusing pressed-button code, explicitly not raised

**Defer (out of scope for v3.0):**
- `AeroListItem` bottom-edge mirror reflection — **locked decision: deferred**, out of scope; the clipped pill + rim highlight is what matters
- Noise/grain texture, real DWM backdrop blur, AeroProgressBar ping-pong bounce (keep existing 1500ms restart timing, restyle appearance only — **locked decision**)
- A sweep of the remaining ~40 non-target components' own visual language (though their shared glass-layer code IS touched incidentally — see Architecture)

### Architecture Approach

One core `DrawScope` primitive (`internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float)`) exposed three ways: as `Modifier.aeroSurface(style, shape)` for Box-owning components, as a direct function call for Canvas-owning components (`AeroRangeSlider`), and as a circle-specialized `Modifier.aeroThumbSurface`/`drawAeroThumb` for anything grabbable. This is a single implementation exposed three ways, not three independent ones — directly avoiding the "shared visual coincidence, not shared code" drift this project's own memory already flags (PanelGroup DSL lesson). `AeroOrnamentTokens.derive(base: AeroColorScheme)` replaces ~10 hand-tuned literals per theme with one RGB lighten/darken algorithm plus a single trailing `ornamentOverride: AeroOrnamentTokens? = null` field on `AeroColorScheme` (source-compatible, since Kotlin only requires defaulted params to be trailing).

**Major components (new/changed types):**
1. `ColorMath.kt` (NEW) — `Color.lighten()`/`darken()` RGB-mix helpers; the load-bearing primitive every other device depends on, because `.copy(alpha=)` alone cannot brighten a color, only fade it
2. `AeroSurfaceStyle.kt` + `AeroSurfacePrimitives.kt` (NEW) — the single shared drawing core plus `Modifier.aeroSurface`/`aeroGlowRing`/`aeroThumbSurface` wrappers; `dropShadow`/`innerShadow` are real Modifier-chain nodes (not foldable into the cached draw block) so their ordering (`dropShadow` before `.clip()`, `innerShadow` after fill) is centralized here once
3. `AeroOrnamentTokens.kt` (NEW) + `AeroColorScheme.kt`/`AeroTheme.kt` (MODIFIED, additive) — the theme-token layer, algorithmically derived, three themes validated as a Foundation-phase spike deliverable, not deferred
4. `GlassModifiers.kt` (MODIFIED, bug fixes only) — `glassPanel`/`glassSurface`/`glassEffect` signatures stay source-compatible; this is where the three confirmed defects get fixed
5. `InteractionStates.kt` (MOVED from `components/buttons/` to `components/common/`, EXTENDED) — the existing `rememberHoverState`/`rememberPressedState`/`rememberFocusState` helpers are already correct and already usable module-wide (Kotlin `internal` is module-scoped, established precedent from Phase 7); a new bundling `rememberAeroInteractionState()` reduces five components' worth of fresh wiring to one call site each

**Locked build order (Phase 15 upgrade → 16 Foundation → 17 Buttons → 18 Range → 19 Selectors+Lists → 20 Verification):** dependency-justified, not arbitrary — Buttons must precede Selectors because `AeroSegmentedControl` reuses Button's pressed-fill code; Range must precede Selectors because `AeroSwitch`'s thumb/groove primitives don't exist until Range builds and proves them.

### Critical Pitfalls

1. **Draw-before-clip ordering (Pitfall 6)** — confirmed, live, three instances today: `glassSurface`'s bounds-centred 1dp stroke loses its outer half to a late `.clip()`; `AeroButton`'s hover overlay paints outside M3's internal clip (square corners on a rounded button); `AeroListItem` has no `.clip()` at all. Fix: `.clip(shape)` must be the outermost modifier affecting a component's own paint — centralized once inside `aeroSurface()` rather than re-derived per call site.
2. **Draw-time allocation inside `drawBehind` instead of `drawWithCache` (Pitfall 1)** — `glassPanel`/`glassSurface` already rebuild `Brush.verticalGradient` on every frame; multiplied by 4–5 new layers per component and by list-shaped consumers (`AeroListItem` in a `LazyColumn`), this is a real perf trap. Fix: `drawWithCache { onDrawBehind {...} }` for geometry, `State<Color>` reads only (not `Brush` rebuilds) for animated color.
3. **Theme-dependent ornamentation breaking, not just looking different (Pitfall 11)** — `Classic`'s glass tokens are fully opaque; `AeroBlue`/`AeroDark`'s are alpha-composited. A gradient tuned only against `AeroBlue` renders as a flat opaque color block on `Classic`, not merely "less pretty." Fix: every new gradient fades toward `baseColor.copy(alpha=0f)`, never a hardcoded `Color.Transparent`; verify all three themes at first Foundation-phase iteration, not deferred to sign-off.
4. **Silently regressing M3-provided keyboard/semantics/step-snap behavior (Pitfall 8)** — `AeroRangeSlider` is the cautionary precedent: zero `semantics`/`focusable`/keyboard handling anywhere in its from-scratch Canvas implementation. The corrected `AeroSlider` architecture (keep M3, reskin slots) sidesteps this almost entirely; `AeroButton`/`AeroOutlinedButton` still drop M3's container and must explicitly reimplement `Modifier.clickable(role = Role.Button, indication = null, ...)` rather than a raw `pointerInput` hand-roll.
5. **Human three-theme sign-off producing a false-positive pass, same failure shape as v2.0.3 (Pitfall 14)** — several of this milestone's defect classes (clipped hover corners, 0.5dp-vs-1dp border width, non-100%-DPI rendering) are only visible at specific interaction states or pixel positions a glance-based review can miss. Fix: pair the human checklist (keep it, it's valuable) with mechanical grep-gates for Pitfalls 4 and 6 specifically, and name exact conditions ("hover at a corner, not just center") in the checklist itself.

## Superseded Findings (represent current state only, do not silently repeat)

1. **STACK.md recommended NOT upgrading Compose Multiplatform**, and therefore recommended hand-rolled double-stroke bevel approximations instead of first-party shadow modifiers. **This is overridden by explicit user decision.** The upgrade is mandatory; `Modifier.dropShadow`/`Modifier.innerShadow` (the unified `Shadow` class, landed CMP 1.9.0) are available at the 1.11.1 target and are the architecturally-preferred primitive for the inner-bevel/outer-glow devices. STACK.md's Brush/Skia/AGSL-is-Android-only/testing-tooling findings remain valid and are carried forward above.
2. **UPGRADE.md recommended targeting CMP 1.9.3** (the latest stable 1.9.x line) rather than the newest overall stable, specifically to avoid the Kotlin 2.2+ floor, the alpha-Material3-alias trap, and the CMP 1.11.0 test-dispatcher default change. **The user chose 1.11.1 anyway**, knowingly accepting all three consequences as mandatory work items (Kotlin 2.4.10, explicit stable-M3 pin, `AeroPanelGroupRecomposeUiTest` port + re-proof). The 1.9.3 analysis in UPGRADE.md survives only as a documented, named fallback if the Kotlin 2.4.10 + CMP 1.11.1 pairing gate fails.
3. **FEATURES.md rated `AeroSlider` the milestone's one HIGH-complexity outlier**, assuming full M3 removal plus a custom `awaitPointerEventScope` drag rewrite (reusing `AeroRangeSlider`'s pattern). **ARCHITECTURE.md overturned this** via Context7-verified confirmation that Material3's `Slider` exposes `thumb: @Composable (SliderState) -> Unit` and `track: @Composable (SliderState) -> Unit` slots, both long-stable (not new-in-1.4/1.5). Custom-drawn slot composables keep drag, keyboard arrow-nudge, `steps` snap, `onValueChangeFinished`, and semantics for free — complexity revises to MEDIUM, and Pitfall 8's entire M3-behavior-loss checklist becomes a non-issue for this component specifically. Residual verification: confirm the pinned M3 coordinate really exposes these slots, and spike whether a custom-sized slot composable fits M3 `Slider`'s internal layout math without clipping/misalignment (Phase 16 exit item).

## Implications for Roadmap

Based on combined research, the phase structure below is dependency-justified (Q6 of ARCHITECTURE.md), building directly on the locked v3.0 scoping in STATE.md. Phase numbering continues from 15 per project convention.

### Phase 15: Toolchain Upgrade (Kotlin 2.4.10 / CMP 1.11.1)

**Rationale:** Categorically different risk class from visual/drawing work (dependency/build-graph vs. rendering code) — must land and be fully verified in isolation so any later regression can be unambiguously attributed. Has independently verifiable, mechanical exit criteria that don't depend on any visual judgment call.
**Delivers:** Green build on the new toolchain; explicitly pinned stable Material3; ported and re-proven `AeroPanelGroupRecomposeUiTest`; full 232-test suite green; three-theme showcase smoke launch unchanged; `dropShadow`/`innerShadow` scratch-composable proof against the real jar; JitPack build verification.
**Addresses:** The mandatory upgrade half of the milestone (STATE.md v3.0 Scoping Decisions).
**Avoids:** Pitfall-adjacent risk of conflating upgrade breakage with visual-phase breakage; directly protects against a repeat of the v2.0.3 false-positive-guard failure mode by requiring the recompose-drag guard be re-proven to FAIL on unfixed code post-port, not just re-run.
**Gate before proceeding:** bare `./gradlew build` on Kotlin 2.4.10 + CMP 1.11.1 is the very first task — the pairing was never shipped by JetBrains as matched (CMP 1.11.1 published six weeks before Kotlin 2.4.10; the Kotlin-2.4-aligned CMP line is 1.12.x, prerelease). Three named fallbacks exist if this gate fails, in preference order: (a) Kotlin 2.4.10 + CMP 1.12.0-beta02 (matched but prerelease), (b) CMP 1.11.1 + newest Kotlin it actually accepts, (c) CMP 1.9.3 + Kotlin unchanged. Any fallback choice must be escalated to the user, not picked silently, since it walks back an explicit locked decision.

### Phase 16: Foundation — Aero Primitives Layer

**Rationale:** Mirrors the successful v2.0 Phase 7 enabling-phase pattern; every device catalog item depends on `Color.lighten/darken`, the fixed `glassSurface`, and/or the shared `aeroGlowRing`/thumb/groove primitives — building any component before these exist means rework. This is the single highest-leverage phase in the milestone.
**Delivers:** `ColorMath.kt`, `AeroOrnamentTokens.kt` + `derive()`, `AeroSurfaceStyle.kt`, `AeroSurfacePrimitives.kt` (core draw function + `Modifier.aeroSurface`/`aeroGlowRing`/`aeroThumbSurface`), `GlassModifiers.kt` bug fixes (proportional `endY`, clip-order, dead `elevation` wired-or-removed), relocated + extended `InteractionStates.kt`.
**Uses:** `Brush.verticalGradient`/`radialGradient` (STACK.md Q3), the new `Shadow`/`dropShadow`/`innerShadow` APIs (STACK.md Q1, corrected per Superseded Findings), `drawWithCache` (PITFALLS.md Pitfall 1).
**Implements:** The `AeroSurfaceStyle`/`AeroSurfacePrimitives` and `AeroOrnamentTokens` architecture components above.
**Must also include (non-negotiable exit items, not optional polish):** a Foundation-phase spike verifying the real `dropShadow`/`innerShadow` signature against the actual 1.11.1 jar; a spike verifying custom-sized M3 `Slider` thumb/track slots don't clip/misalign; three-theme spot-check of every new primitive at first iteration (not deferred); and — critically — a **full-library visual smoke pass across all ~50 components**, not just the eight targets, because fixing `GlassModifiers.kt`'s shared bugs re-renders every existing consumer of `glassSurface`/`glassPanel`/`glassEffect`.
**Avoids:** Pitfalls 1, 4, 6, 7, 11, 12 (all foundation-scoped per PITFALLS.md's own Pitfall-to-Phase Mapping).

### Phase 17: Buttons — AeroButton, AeroOutlinedButton

**Rationale:** First real consumer of `aeroSurface`+`aeroGlowRing`; expected to surface any remaining Foundation-layer bugs cheaply, before three more phases build on top of it.
**Delivers:** Both buttons drop M3's `Button`/`Surface` container (M3's `ButtonColors` genuinely cannot express the gradient/bevel geometry) but keep Foundation's `Modifier.clickable(role = Role.Button, indication = null, ...)` — lower-risk than the `AeroRangeSlider` zero-semantics precedent. Shared internal `AeroButtonSurface(filled: Boolean, ...)` composable so the two buttons cannot visually drift apart.
**Addresses:** FEATURES.md B1/B2 device sets; fixes the confirmed hover-corner-square bug and the hardcoded-focus-radius bug as side effects of correct clip ordering.
**Avoids:** Pitfall 8 (M3 semantics/keyboard loss) via the `Modifier.clickable` pattern rather than a from-scratch hand-roll; Pitfall 6 via the centralized `aeroSurface()` clip ordering.

### Phase 18: Range — AeroSlider, AeroRangeSlider, AeroProgressBar

**Rationale:** Validates the shared thumb/groove primitives together; `AeroSlider` first (proves the corrected M3-slot approach against Phase 16's spike findings), `AeroRangeSlider` second (reuses the now-proven thumb primitive so any bugs get fixed once for both sliders), `AeroProgressBar` third (shares the fill/groove renderer with `AeroSlider`'s track).
**Delivers:** `AeroSlider` KEEPS M3's `Slider(...)` and supplies custom `thumb =`/`track =` slot lambdas — the corrected (not original) architecture position; `AeroRangeSlider` gets a pure visual upgrade (already Canvas-based, no M3 to drop, drag logic untouched); `AeroProgressBar` gets groove+fill+optional sheen with the two locked design decisions applied (sheen default OFF; indeterminate keeps 1500ms restart timing, restyle only).
**Addresses:** FEATURES.md B5/B6/B7; the corrected AeroSlider position from Superseded Findings item 3.
**Avoids:** Pitfall 8 is largely defused for AeroSlider by construction (keeping M3); Pitfall 9 (two-writer animation conflicts) applies directly to both sliders' thumb hover+drag interaction and must reuse the locked Pattern 3 (`AeroPanelGroup` precedent: animation reads target-only, drag writes directly, `isDragging` flips to `snap()`).

### Phase 19: Selectors + Lists — AeroSwitch, AeroSegmentedControl, AeroListItem

**Rationale:** `AeroSwitch` cannot move earlier — its thumb+groove primitives don't exist until Phase 18 builds and proves them. `AeroSegmentedControl` reuses Phase 17's Button pressed-fill code as its "active" segment look. `AeroListItem` has no hard dependency on Phase 18 and could technically move right after Phase 17, but is grouped here as the lowest-risk "polish wave" buffer — flagged as movable if phase-sizing argues otherwise.
**Delivers:** `AeroSwitch`'s first-ever hover/press/focus wiring (currently 100% absent — the single largest gap of the eight components); `AeroSegmentedControl`'s recessed/pressed selected-segment metaphor (locked design decision) plus first-ever hover/focus; `AeroListItem`'s missing `.clip()`, selection pill, and the combined selected+hover state (currently: selected short-circuits hover entirely — a confirmed baseline bug).
**Addresses:** FEATURES.md B3/B4/B8; five of the milestone's most consequential "GAP today" cells in the interaction-state matrix.
**Avoids:** Pitfall 9 (hover-not-clearing) — must copy `AeroListItem`'s already-correct `Modifier.hoverable`+`collectIsHoveredAsState` pairing verbatim for the two components authoring hover fresh, rather than inventing raw pointer-position tracking.

### Phase 20: Verification — Showcase Wiring, Grep-Gates, Three-Theme Sign-Off

**Rationale:** Distributed per-phase mini-checks (17/18/19) already happened; this is the consolidated pass plus the structural, mechanical gates — a re-check, not the first look, directly applying PITFALLS.md's Pitfall 14 lesson and the project's own v2.0-retrospective "aggregate-at-the-end is the biggest inefficiency" finding.
**Delivers:** Grep-gate for gradient literals (Pitfall 4); grep-gate confirming no bypass of the centralized `aeroSurface()` clip ordering (Pitfall 6); Defaults Snapshot Test against a pre-milestone baseline (Pitfall 13, size creep); keyboard-activation UI test for the two converted buttons (Pitfall 8); consolidated three-theme sign-off, explicitly run at least once at non-100% DPI scaling; a minimal scratch-consumer smoke step outside the showcase's own styling conventions (mitigating the fact that no real external consumer app — `aska`/`satellite-control` — will track this milestone).
**Avoids:** Pitfall 14 directly — pairs the human checklist with provably-failing-first mechanical guards, per this project's own `feedback_repro_must_exercise_path` memory.

### Phase Ordering Rationale

- Toolchain-first is non-negotiable per explicit user decision and per this project's own established discipline of isolating one class of change per phase (v2.0.x patch-release history, the 13.1 orientation-param precedent).
- Foundation-before-components is confirmed correct by both FEATURES.md's dependency graph (5+ of 8 components consume each Foundation primitive) and ARCHITECTURE.md's Q6 build-order analysis — this is not a restatement of FEATURES.md's wave labels, it is independently dependency-derived.
- Buttons-before-Selectors and Range-before-Selectors are hard dependencies (shared code reuse), not scheduling preferences — `AeroSwitch` and `AeroSegmentedControl` literally cannot be correctly implemented before their prerequisite primitives exist.
- Verification-as-its-own-phase, rather than folded into the last component phase, directly reflects the project's own retrospective lesson about deferred-to-the-end aggregate verification being costly, while still doing a final consolidated pass (not skipping it) given the reduced-blast-radius per-phase mini-checks already catch most defects earlier.

### Research Flags

Phases likely needing deeper research during planning (`/gsd:research-phase`):
- **Phase 15 (Toolchain):** The Kotlin 2.4.10/CMP 1.11.1 pairing gate outcome is genuinely unknown until run — if it fails, the fallback decision (three named options) needs real investigation, not just picking the first one.
- **Phase 16 (Foundation):** The `dropShadow`/`innerShadow` exact signature spike and the M3 `Slider` slot-sizing feasibility spike are both explicitly named as requiring empirical verification against the real jar/API, not assumed from documentation alone.
- **Phase 18 (Range):** Even with the corrected (lower-risk) AeroSlider architecture, this remains the phase combining the most moving parts (M3 slot integration + two-writer animation/drag interaction on both sliders + the opt-in sheen decision) — worth a lighter research pass even though the core approach is now well-specified.

Phases with standard, well-documented patterns (skip research-phase, plan directly):
- **Phase 17 (Buttons):** Pattern (drop M3 container, keep `Modifier.clickable`, shared internal surface composable) is fully specified by ARCHITECTURE.md Q5 with a concrete code sketch.
- **Phase 19 (Selectors+Lists):** Reuses Phase 17/18 primitives with no new architectural questions; FEATURES.md's per-component device tables are directly implementable.
- **Phase 20 (Verification):** Directly mirrors the already-proven v2.0.2/v2.0.4 three-theme sign-off + grep-gate pattern; no new verification mechanism is being invented.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Toolchain (STACK.md's superseded parts + UPGRADE.md) | HIGH for version/date/compatibility facts (GitHub Releases API, Maven Central, official CHANGELOG.md — machine-verified); MEDIUM for exact `dropShadow`/`innerShadow` constructor shape (doc-mirror cross-checked, not yet compiled against the real jar — explicit residual verification item) |
| Features | MEDIUM-HIGH for the device catalog (cross-checked against community Win7 recreation projects) / MEDIUM for per-component numeric proposals (explicitly original synthesis, not sourced pixel values — Microsoft never published them) |
| Architecture | HIGH for the M3 `Slider` slot finding (Context7, official androidx reference, two independently-returned doc entries agreeing) and for modifier-ordering conventions (derived directly from this repo's own confirmed bugs); MEDIUM for the `AeroOrnamentTokens` derivation design (original synthesis, flagged as a Foundation-phase spike-and-validate item, not assumed correct) |
| Pitfalls | HIGH for pitfalls grounded in this repo's own code (cited file:line); MEDIUM for general Compose/Skia platform behavior (verified against official docs via Context7); LOW explicitly flagged inline for single-WebSearch-source claims (e.g., cross-platform blur API divergence specifics) |

**Overall confidence:** HIGH for what to build and in what order; MEDIUM for a handful of named, explicitly-scoped empirical unknowns that are already built into the roadmap as spike/verification items rather than left implicit.

### Gaps to Address

- **Kotlin 2.4.10 + CMP 1.11.1 pairing outcome** — genuinely unverified until the Phase 15 `./gradlew build` gate runs; three fallbacks are named but untested. Handle by running the gate as the literal first task of Phase 15 and escalating to the user immediately if it fails, rather than silently substituting a fallback.
- **Exact `dropShadow`/`innerShadow` constructor signature at the real 1.11.1 artifact** — currently MEDIUM-HIGH confidence from a version-pinned third-party doc mirror, not a compiled check. Handle via the named Phase 16 scratch-composable spike before any production `GlassModifiers.kt` code depends on it.
- **Whether custom-sized M3 `Slider` thumb/track slots actually fit M3's internal layout math without clipping/misalignment** — the load-bearing assumption behind the corrected (lower-complexity) AeroSlider architecture. Handle via the named Phase 16 spike; if it fails, the fallback is FEATURES.md's original full-M3-removal plan for AeroSlider specifically (re-promote Phase 18 to HIGH complexity, reuse `AeroRangeSlider`'s drag pattern as originally proposed).
- **Whether the `AeroOrnamentTokens.derive()` algorithm actually produces a correct-looking result on `Classic`'s opaque tokens** — the formula's opaque/translucent branch is a design proposal, not yet rendered. Handle via the named Phase 16 three-theme spot-check, with the explicit fallback of adjusting `derive()`'s branch logic (one function, one place) rather than hand-patching per-theme literals if it reads wrong.
- **No real external consumer app tracks this milestone** (`aska`/`satellite-control` are pinned to the old toolchain) — the project's historically strongest regression-catcher is unavailable this time. Handle via Phase 20's named minimal scratch-consumer smoke step, raising the relative importance of the mechanical grep-gates.

## Sources

### Primary (HIGH confidence)
- GitHub Releases API (`api.github.com/repos/JetBrains/compose-multiplatform/releases`) — version/date/prerelease-flag ground truth for the entire 1.7.3→1.11.1 path
- Official `JetBrains/compose-multiplatform` `CHANGELOG.md` (6,875 lines, read in full across the relevant version range) — Material3-alias resolution table, Popup deprecation timeline, test-infrastructure dispatcher-default change
- Context7 `/websites/developer_android_reference_kotlin_androidx_compose_material3` — direct verification of `Slider(thumb =, track =)` slot signatures
- Context7 `/websites/developer_android_develop_ui_compose` — `drawWithCache` caching semantics, `CompositingStrategy.Offscreen` behavior
- GitHub REST API + YouTrack API direct queries — issue #3757 (closed, moot for this project) and SKIKO-1072 (still open, also moot given `transparent=false` is locked)
- Direct code inspection of this repository — `GlassModifiers.kt`, `AeroColorScheme.kt`, `AeroButton.kt`, `AeroOutlinedButton.kt`, `AeroSwitch.kt`, `AeroSegmentedControl.kt`, `AeroSlider.kt`, `AeroRangeSlider.kt`, `AeroProgressBar.kt`, `AeroListItem.kt`, `InteractionStates.kt` — every confirmed baseline bug cited above
- `.planning/PROJECT.md`, `.planning/STATE.md` — locked v3.0 scoping decisions, mandatory-upgrade decision record, resolved design questions

### Secondary (MEDIUM confidence)
- `composables.com` version-pinned (1.9.0-rc01) third-party API mirror — `dropShadow`/`innerShadow`/`Shadow` signature, internally consistent but not first-party
- 7.css / PresentationTheme.Aero community Win7 recreation projects — corroborate the two-tone gradient/bevel/groove device shapes independently
- WebSearch (Haze/Cloudy library docs) — Skia-backed desktop blur mechanism, AGSL-is-Android-only, `.asComposeShader()` landing point

### Tertiary (LOW confidence, flagged for validation)
- WebSearch-only claims about cross-platform Skia blur API divergence specifics (PITFALLS.md Pitfall 12) — recommend a Foundation-phase spike against the real target before relying on any specific blur API claim if true blur is ever adopted
- The August-2025 preview-build `DropShadow(...)` factory-style shape (superseded by the RC-pinned unified `Shadow` class, but not independently compiled-confirmed this session)

---
*Research completed: 2026-07-21*
*Ready for roadmap: yes*
