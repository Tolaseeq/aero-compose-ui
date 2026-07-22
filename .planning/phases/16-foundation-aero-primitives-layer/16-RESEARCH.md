# Phase 16: Foundation — Aero Primitives Layer - Research

**Researched:** 2026-07-22
**Domain:** Compose Multiplatform (Desktop/JVM) custom-drawing primitives layer — Kotlin 2.4.10 / CMP 1.11.1 / Material3 1.9.0
**Confidence:** HIGH for facts verified against this session's own codebase reads and `ScratchAeroShadowProof.kt`'s compile-proven signatures; MEDIUM for the exact `AeroOrnamentTokens` field set and lighten/darken magnitudes (original synthesis in `.planning/research/ARCHITECTURE.md`/`FEATURES.md`, explicitly flagged there as calibration starting points, not measured facts); LOW/ASSUMED flagged inline wherever a specific numeric literal (percentage, alpha, dp) has no verified source.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01 (Aero Fidelity Calibration):** The shared surface primitive is calibrated to **moderate "spirit of Aero"**, NOT literal Win7: top gloss occupying ~30–35% of component height, a **soft** two-tone seam (not a hard mid-surface break), and a subtle inner bevel/rim. This is still genuinely glass (gloss + gradient + bevel + depth), not generic modern-flat — it must not read as Feather-style outline or Material3-flat. Reversibility: costly — baked into `drawAeroSurfaceCore`/`AeroSurfaceStyle` defaults all eight Phase 17–19 components inherit.

**D-02 (Native Shadows vs Gradient Depth):** Whether native `Modifier.dropShadow`/`innerShadow` (confirmed available at CMP 1.11.1) or manual gradients render each depth cue is **Claude's discretion, decided per primitive on the three-theme review** — use whichever reads better: native for drop/recess where it looks cleaner, gradient where color control of the rim/highlight matters. Signatures + import packages already proven in `ScratchAeroShadowProof.kt`. Ordering rule: `dropShadow` before `.background()`, `innerShadow` after.

**D-03 (glassEffect elevation):** The dead `glassEffect(elevation)` parameter (PRIM-11) is resolved at **Claude's discretion on the review**: either revive it to draw a real `dropShadow` (source-compatible) or remove it (clean API, minor-breaking, acceptable in a major version). Decision folds into the D-05 blast-radius review: revive if the shadow improves cards/panels across three themes, remove if it adds noise.

**D-04 (Foundation Sign-off Vehicle):** A **dedicated Primitives showcase gallery** is built in `:showcase` — each primitive (surface, glow ring, thumb, groove, gloss, shadow) rendered in isolation × three themes × states — and it **stays in the project as a living reference** for Phases 17–19, not thrown away. Satisfies PRIM-16. Distinct from the PRIM-17 full-library smoke pass. Reversibility: reversible — additive showcase code.

**D-05 (GlassModifiers Fix Blast-Radius Posture):** Apply the correct `GlassModifiers.kt` fixes **as-is** (proportional gloss replacing `endY = 100f`, full-thickness border with corrected clip order, and the `elevation` resolution). The ~40 out-of-scope components sharing those modifiers are **allowed to become slightly "more glass"** as a result. PRIM-17's smoke pass regression bar = **broken or ugly rendering only**, not "looks slightly different." Do NOT hand-tune the fixes to keep the 40 pixel-identical. Reversibility: costly.

### Claude's Discretion

- Per-primitive native-shadow vs gradient choice (D-02) and the `glassEffect(elevation)` revive-or-remove call (D-03), both settled on the three-theme review.
- Exact `AeroSurfaceStyle` token shape/naming, `AeroOrnamentTokens` field set, `derive(base)` lighten/darken magnitudes, glow-ring geometry, and thumb/groove geometry — all technical, calibrated to hit the D-01 "moderate" target.
- Structure/placement of the Primitives gallery within `:showcase`.
- Exact gloss fraction within the ~30–35% band (D-01 sets the target, not a hard literal).

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope. Component restyles are Phases 17–19 by explicit milestone design; the `AeroSlider` M3-slot spike outcome and the `glassEffect(elevation)` fate are in-scope Phase 16 items, resolved on the review, not deferred.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PRIM-01 | `Color.lighten()`/`Color.darken()` RGB-mix helpers | Code Examples §ColorMath; Architecture Q3; Pitfall 11 (must be RGB, not alpha-only, for Classic) |
| PRIM-02 | `AeroOrnamentTokens` + `derive(base)` algorithmic derivation, all three themes | Architecture Q3; Code Examples §AeroOrnamentTokens; AeroColorScheme.kt read (Classic opaque tokens confirmed) |
| PRIM-03 | `AeroColorScheme` extended source-compatibly with trailing `ornamentOverride` | Architecture Q3 "Source-compatibility, addressed explicitly"; AeroColorScheme.kt (23-field data class confirmed) |
| PRIM-04 | `AeroSurfaceStyle` declarative surface description | Architecture Q1 "The core primitive" |
| PRIM-05 | Single `drawAeroSurfaceCore(style, cornerPx)` exposed as `Modifier.aeroSurface()` + direct Canvas call | Architecture Q1 full recommendation + code skeleton |
| PRIM-06 | `Modifier.aeroGlowRing` shared hover/focus primitive | Architecture Q1 "two small sibling primitives"; FEATURES.md A6/A8 |
| PRIM-07 | Raised-thumb primitive (`aeroThumbSurface`/`drawAeroThumb`) | Architecture Q1; FEATURES.md A12 |
| PRIM-08 | Recessed track-groove primitive | FEATURES.md A10 |
| PRIM-09 | `glassSurface` gloss proportional to height, `endY = 100f` removed | GlassModifiers.kt:95 confirmed; Pitfall 4; `glassPanel`'s already-correct `size.height * 0.55f` sibling pattern |
| PRIM-10 | `glassSurface` border full-thickness, clip order fixed | GlassModifiers.kt:87-105 confirmed draw-before-clip bug; Pitfall 6 item 1 |
| PRIM-11 | `glassEffect(elevation)` revived or removed, no dead params | GlassModifiers.kt:26-45 confirmed dead param; Pitfall 7; D-03 |
| PRIM-12 | Clip centralized inside `aeroSurface()` | Architecture Q1/Q2 "the confirmed-bug convention"; Pitfall 6 |
| PRIM-13 | Geometry/brushes in `drawWithCache`, no per-frame `Brush` rebuild | Architecture Q2; Pitfall 1 |
| PRIM-14 | New gradients fade to `baseColor.copy(alpha = 0f)`, never `Color.Transparent` | Pitfall 11; Architecture Q3 "opaque branch" note |
| PRIM-15 | `InteractionStates.kt` → `components/common/` + `rememberAeroInteractionState()` | Architecture Q4 full code skeleton; InteractionStates.kt read (confirmed `internal`, module-scoped) |
| PRIM-16 | Every new primitive proven on three themes at first iteration | Pitfall 11/14; D-04 gallery is the mechanism |
| PRIM-17 | Full-library (~50 component) smoke pass | Architecture Q7; D-05 blast-radius posture |
| PRIM-18 | M3 `Slider` thumb/track slot-sizing spike | Architecture Q5 (Context7-verified slot API); STATE.md locked architecture position |
</phase_requirements>

## Summary

Phase 16 builds a shared drawing/token layer that every Phase 17–19 component will consume, following this project's own proven "enabling-phase-then-consume" pattern (v2.0 Phase 7). Nothing here is genuinely novel Compose API surface — every capability needed (gradients, strokes, `drawWithCache`, and now native `dropShadow`/`innerShadow`) already exists at the Phase-15-locked toolchain (Kotlin 2.4.10 / CMP 1.11.1 / Material3 1.9.0, confirmed pinned in `library/build.gradle.kts:22` and `gradle/libs.versions.toml:3`). The work is architectural discipline: one `drawAeroSurfaceCore` function exposed through four call conventions instead of duplicated per-component gradient code, one algorithmic color-derivation function instead of ~30 hand-authored per-theme literals, and three concrete bug fixes to `GlassModifiers.kt` that are already fully diagnosed with exact line numbers.

The single highest-risk item is **not** a drawing question — it's PRIM-18's M3 `Slider` thumb/track slot-sizing spike, which gates whether Phase 18's `AeroSlider` stays MEDIUM complexity (keep M3, custom-sized slots) or reverts to HIGH (full M3 removal, reuse `AeroRangeSlider`'s drag pattern). This spike must run and produce a clear pass/fail artifact before Phase 16 closes.

`.planning/research/STACK.md` is stale on one point: it recommends *against* upgrading Compose to reach `dropShadow`/`innerShadow` (written against the pre-Phase-15 1.7.3 baseline). Phase 15 already completed that upgrade to CMP 1.11.1, and `ScratchAeroShadowProof.kt` has compile-proven the corrected signatures. Treat `ScratchAeroShadowProof.kt` as the current source of truth for shadow APIs, not `STACK.md`'s Q1/Q5 recommendation.

**Primary recommendation:** Build `ColorMath.kt` → `AeroOrnamentTokens.kt` → `AeroSurfaceStyle.kt` → `AeroSurfacePrimitives.kt` in that dependency order (new files, `theme` package), fix the three confirmed `GlassModifiers.kt` bugs in place (same file, same public signatures), relocate `InteractionStates.kt` to `components/common/`, run the M3 Slider slot spike as a standalone scratch composable before committing to Phase 18's architecture, and build the Primitives gallery as a new `FoundationSection`-adjacent showcase section (that section already exists and already demonstrates `glassEffect`/`glassPanel`/`glassSurface` in isolation — it is the direct precedent to extend, not a new pattern to invent).

## Architectural Responsibility Map

This project has no browser/server/API tiers — it is a single-process desktop Compose Multiplatform UI library with a layered internal architecture. Tiers below are this project's own layers, used the same way the standard template's tiers are used (to sanity-check that each capability lands in the right layer).

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Color math (lighten/darken) | Theme/Token layer (`theme/ColorMath.kt`) | — | Pure `Color` → `Color` functions, no Compose runtime dependency; belongs beside `AeroColorScheme` |
| Ornament token derivation (`AeroOrnamentTokens.derive`) | Theme/Token layer | — | Reads `AeroColorScheme`, produces a new immutable token set; must live where `AeroColorScheme`/`LocalAeroColors` already live |
| `AeroSurfaceStyle` (declarative surface spec) | Drawing Primitives layer (`theme/AeroSurfaceStyle.kt`) | Theme/Token layer | Consumes ornament tokens to build a style object; sits between tokens and the draw function |
| `drawAeroSurfaceCore` / `Modifier.aeroSurface` / `aeroGlowRing` / thumb+groove | Drawing Primitives layer (`theme/AeroSurfacePrimitives.kt`) | — | The actual `DrawScope` painting logic; must be the ONLY place gradient/bevel/rim geometry is authored |
| `GlassModifiers.kt` fixes (`glassSurface`/`glassPanel`/`glassEffect`) | Legacy Glass Modifier layer (existing file, bug-fixed, signatures unchanged) | Drawing Primitives layer | These are pre-existing public API surfaces used by ~40 out-of-scope components; fixed in place, not folded into the new primitives (see D-05) |
| `rememberAeroInteractionState()` | Interaction State layer (`components/common/InteractionStates.kt`) | — | State-collection only (booleans), no drawing; deliberately does not resolve colors/styles (Architecture Q4) |
| Primitives showcase gallery | Showcase/Verification layer (`:showcase` module) | — | Consumer-facing proof surface, not library code; extends the existing `FoundationSection` precedent |
| Full-library smoke pass | Showcase/Verification layer + manual sign-off | — | Exercises the ~40 out-of-scope components indirectly through the `GlassModifiers.kt` fix, not new code of its own |
| M3 Slider slot-sizing spike | Showcase/Verification layer (scratch composable) | Drawing Primitives layer (consumes `aeroThumbSurface`) | A throwaway compile+visual proof, same pattern as `ScratchAeroShadowProof.kt` — not wired into any real component this phase |

**Downstream consumers (Phase 17–19) live entirely in the Component layer** (`components/buttons`, `components/range`, `components/selection`, `components/list`) and are explicitly OUT of this phase's scope — they consume what this phase produces, they do not get modified here.

## Standard Stack

### Core

No new external dependencies. Everything below is already vendored via the Phase-15-locked coordinates.

| Library / API | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.ui.draw.dropShadow` / `.innerShadow` | Ships with CMP 1.11.1 (`ui-desktop-1.11.1.jar`) | Native drop/inner shadow modifiers | `[VERIFIED: codebase]` — compile-proven in `ScratchAeroShadowProof.kt` against the real 1.11.1 jar via `javap` bytecode inspection, 2026-07-22. Package is `androidx.compose.ui.draw`, NOT `androidx.compose.ui.graphics.shadow` (that package holds only the `Shadow` value class itself) |
| `androidx.compose.ui.graphics.shadow.Shadow` | Ships with CMP 1.11.1 (`ui-graphics-desktop-1.11.1.jar`) | Shadow parameter value class (`radius`, `color`/`brush`, `spread`, `offset`, `alpha`, `blendMode`) | `[VERIFIED: codebase]` — same scratch-proof session |
| `androidx.compose.ui.unit.DpOffset` | Ships with `compose.ui` | Offset param for `Shadow` | `[VERIFIED: codebase]` — corrected from an earlier wrong import path (`ui.geometry`) in the same scratch session |
| `androidx.compose.ui.draw.drawWithCache` | Ships with `compose.ui`, stable since Compose 1.0 | Cached geometry/brush construction, PRIM-13's mandated pattern | `[CITED: developer.android.com/develop/ui/compose/graphics/draw/modifiers]` per `.planning/research/PITFALLS.md` Pitfall 1 |
| `androidx.compose.ui.graphics.Brush` (`verticalGradient`, `radialGradient`) | Ships with `compose.ui`, stable since Compose 1.0 | Two-tone fills, gloss ovals, groove shading | `[VERIFIED: codebase]` — already in use in `GlassModifiers.kt` |
| `androidx.compose.foundation.interaction.*` (`collectIsHoveredAsState`, etc.) | Ships with `compose.foundation` | `rememberAeroInteractionState()` backing | `[VERIFIED: codebase]` — `InteractionStates.kt` and `AeroListItem.kt:57,74` already use this exact API |
| `androidx.compose.material3:material3:1.9.0` | Pinned explicit stable coordinate | M3 `Slider` (PRIM-18 spike target) | `[VERIFIED: codebase]` — `library/build.gradle.kts:22`, confirmed pinned (not the `compose.material3` alias, which resolves to an alpha per TOOL-02) |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.ui.graphics.drawscope.Stroke` | Ships with `compose.ui` | Hand-drawn rim/bevel strokes | Where `.border()` can't express a gradient/multi-tone rim (Pitfall 6's guidance: prefer `.border()` for plain single-color rims, reserve `Stroke` for gradient rims) |
| `androidx.compose.ui.geometry.CornerRadius` | Ships with `compose.ui` | `drawRoundRect` corner math | Must be derived from the SAME `cornerPx` value used for the component's `RoundedCornerShape` (Pitfall 5) — never two independent `.toPx()` calls |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Native `dropShadow`/`innerShadow` (D-02, per-primitive discretion) | Manual layered `drawRoundRect` glow / double-stroke bevel (STACK.md's pre-upgrade recommendation) | Manual approach is zero-Modifier-chain-node, fits entirely inside one cached `drawWithCache` block; native approach is two unavoidable additional Modifier-chain nodes (Architecture Q1) but is declarative and matches the ordering rule already proven. Decide per-primitive on the three-theme review, not universally |
| `AeroOrnamentTokens.derive()` (algorithmic) | Hand-authored ~10 literals × 3 themes on `AeroColorScheme` directly | Hand-authoring repeats the exact risk Pitfall 11 names: tuned against `AeroBlue`, silently wrong on `Classic`'s opaque tokens. Rejected per Architecture Q3 |
| Keep M3 `Slider` + custom `thumb=`/`track=` slots (PRIM-18) | Full M3 removal, `AeroRangeSlider`-style Canvas+`awaitPointerEventScope` rewrite | Full removal is the STATE.md-flagged fallback ONLY if the slot-sizing spike fails (clipping/misalignment) — keeping M3 preserves keyboard nudge, `steps` snap, `onValueChangeFinished`, and semantics for free (Architecture Q5, Pitfall 8) |

**Installation:** No new Gradle dependencies. All work is new/modified `.kt` files under existing modules.

**Version verification:** `library/build.gradle.kts:22` confirms `org.jetbrains.compose.material3:material3:1.9.0` (explicit stable pin, not the `compose.material3` alias). `gradle/libs.versions.toml:3` confirms `composeMultiplatform = "1.11.1"`. Both verified by direct file read this session — `[VERIFIED: codebase]`.

## Package Legitimacy Audit

**Not applicable this phase.** PRIM-01..18 introduce zero new external packages/dependencies — every API used (`dropShadow`, `innerShadow`, `Shadow`, `drawWithCache`, `Brush`, `collectIsHoveredAsState`) ships inside the CMP 1.11.1 / Material3 1.9.0 coordinates already pinned and verified during Phase 15. No `npm view`/`pip index`/`cargo search`-equivalent registry check applies to a Gradle dependency-free phase. If a future review discovers a need for a third-party blur/shadow library, treat that as a new locked decision requiring its own legitimacy check before adoption — `.planning/research/PITFALLS.md`'s Integration Gotchas table already flags this exact risk ("assuming a purely-visual milestone can't introduce a transitive dependency leak").

**Packages removed due to [SLOP] verdict:** none — no packages considered.
**Packages flagged as suspicious [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
Component call site (Phase 17-19, OUT OF SCOPE this phase)
        │
        │  consumes
        ▼
┌───────────────────────────────────────────────────────────┐
│  DRAWING PRIMITIVES LAYER  (theme/AeroSurfacePrimitives.kt)│
│                                                             │
│  drawAeroSurfaceCore(style, cornerPx)  ◄── single impl     │
│      │              │              │                       │
│      ▼              ▼              ▼                       │
│  Modifier.      direct call    Modifier.aeroGlowRing        │
│  aeroSurface()  (Canvas-owning   (hover/focus, drawn         │
│  (Box-owning     components,     OUTSIDE clip, applied      │
│   components)    e.g. AeroRange  BEFORE .clip() in chain)   │
│                   Slider)                                   │
│                                                              │
│  drawAeroThumb(style, radiusPx) ──► Modifier.aeroThumbSurface│
│  (circle-shape special case of the same core)                │
└───────────────────────────────────────────────────────────┘
        │  reads style built from
        ▼
┌───────────────────────────────────────────────────────────┐
│  THEME / TOKEN LAYER  (theme/ package)                     │
│                                                              │
│  AeroColorScheme (23 fields, UNCHANGED)                    │
│      │  + trailing ornamentOverride: AeroOrnamentTokens?    │
│      ▼                                                       │
│  AeroOrnamentTokens.derive(base) ◄── Color.lighten/darken   │
│      (RGB-mix, not alpha — works on Classic's opaque tokens)│
│      │                                                       │
│      ▼                                                       │
│  AeroSurfaceStyle (declarative: fill stops, gloss, bevel,   │
│  rim, dropShadow?, innerShadow?)                            │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│  LEGACY GLASS MODIFIER LAYER (theme/GlassModifiers.kt)      │
│  glassSurface / glassPanel / glassEffect — SAME public      │
│  signatures, THREE bug fixes applied in place:               │
│    1. endY = 100f → size.height * fraction (PRIM-09)         │
│    2. drawBehind-then-clip → clip-then-draw, or inset stroke │
│       (PRIM-10)                                              │
│    3. dead elevation param → wired to real dropShadow, or    │
│       removed (PRIM-11, D-03)                                │
│  Consumed by ~8 target components (Phase 17-19) AND ~40      │
│  out-of-scope components (D-05: blast radius accepted)       │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│  INTERACTION STATE LAYER (components/common/InteractionStates.kt, MOVED)│
│  rememberHoverState / rememberPressedState / rememberFocusState│
│  + NEW rememberAeroInteractionState() bundling all three      │
│  → returns booleans ONLY, never resolved colors/styles        │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│  SHOWCASE / VERIFICATION LAYER (:showcase module)            │
│  Primitives gallery (D-04, NEW, permanent) — each primitive  │
│  × 3 themes × states, extends existing FoundationSection      │
│  pattern (already demos glassEffect/glassPanel/glassSurface)  │
│                                                                │
│  M3 Slider slot-sizing spike (PRIM-18, scratch composable,    │
│  same throwaway-but-proven pattern as ScratchAeroShadowProof) │
│                                                                │
│  Full-library smoke pass (PRIM-17) — visits ~50 existing       │
│  showcase sections, verifies GlassModifiers fix caused no      │
│  broken/ugly rendering (NOT "looks identical")                 │
└───────────────────────────────────────────────────────────┘
```

### Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
├── theme/
│   ├── ColorMath.kt                 # NEW — Color.lighten()/darken(), internal
│   ├── AeroOrnamentTokens.kt        # NEW — data class + derive(base)
│   ├── AeroColorScheme.kt           # MODIFIED — + trailing ornamentOverride field
│   ├── AeroTheme.kt                 # MODIFIED — + AeroTheme.ornaments accessor
│   ├── AeroSurfaceStyle.kt          # NEW — declarative style data class + presets
│   ├── AeroSurfacePrimitives.kt     # NEW — drawAeroSurfaceCore, aeroSurface,
│   │                                #        aeroGlowRing, drawAeroThumb/aeroThumbSurface,
│   │                                #        groove primitive
│   └── GlassModifiers.kt            # MODIFIED — 3 bug fixes only, signatures unchanged
└── components/
    └── common/                      # NEW package (does not exist yet)
        └── InteractionStates.kt     # MOVED from components/buttons/
                                     #   + rememberAeroInteractionState()

showcase/src/main/kotlin/com/mordred/showcase/
├── sections/
│   ├── FoundationSection.kt         # EXISTING — precedent pattern (glassEffect/Panel/Surface demo)
│   └── PrimitivesSection.kt         # NEW (D-04) — permanent gallery, naming Claude's discretion
└── scratch/
    ├── ScratchAeroShadowProof.kt    # EXISTING — Phase 15→16 handoff, DO NOT modify/wire in
    └── ScratchSliderSlotSpike.kt    # NEW — PRIM-18 spike, throwaway-but-evidenced
```

### Pattern 1: One draw function, four exposure paths (PRIM-05)

**What:** `internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float)` is the single implementation. It is called from: (a) `Modifier.aeroSurface(style, shape)` for Box-owning components via `drawWithCache { onDrawBehind { ... } }`, (b) directly inside an existing `Canvas`'s `DrawScope` for Canvas-owning components (`AeroRangeSlider`), (c) `drawAeroThumb` — the circle-shape special case reusing the same core with `CornerRadius` collapsed to a single radius, exposed as `Modifier.aeroThumbSurface`.

**When to use:** Every one of the eight Phase 17–19 target components' own-surface paint. Never write component-specific gradient/bevel code (Anti-Pattern 1 below).

**Example:**
```kotlin
// Source: .planning/research/ARCHITECTURE.md Q1 (design skeleton, not yet implemented)
internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float) {
    val cr = CornerRadius(cornerPx, cornerPx)
    // 1. two-tone fill — Brush built in drawWithCache, NOT per-frame (PRIM-13)
    // 2. soft seam (D-01: soft, not a hard mid-surface break)
    // 3. gloss oval, clipped to own shape — only if style.glossAlpha > 0, ~30-35% height (D-01)
    // 4. bevel rim, single inset stroke w/ vertical gradient
    // 5. outer contour stroke, INSET by half stroke width so it survives clip (PRIM-10 fix pattern)
}

public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier = this
    .let { if (style.dropShadow != null) it.dropShadow(shape, style.dropShadow) else it }
    .clip(shape)                                          // PRIM-12: outermost paint-affecting modifier
    .drawWithCache {
        val cornerPx = style.cornerRadius.toPx()
        onDrawBehind { drawAeroSurfaceCore(style, cornerPx) }
    }
    .let { if (style.innerShadow != null) it.innerShadow(shape, style.innerShadow) else it }
```

### Pattern 2: Algorithmic ornament derivation, not hand-authored literals (PRIM-01/02)

**What:** `Color.lighten(amount)`/`Color.darken(amount)` mix RGB channels toward White/Black (never alpha-only — `.copy(alpha=)` cannot brighten, and Classic's tokens are opaque so alpha tricks produce zero visible change). `AeroOrnamentTokens.derive(base: AeroColorScheme)` runs this once, producing all ornament tokens for whichever theme is active.

**When to use:** Any new visual token needed by `AeroSurfaceStyle` presets. Never add a new raw `Color` field directly to `AeroColorScheme` (Anti-Pattern 3).

**Example:**
```kotlin
// Source: .planning/research/ARCHITECTURE.md Q3 (design skeleton)
internal fun Color.lighten(amount: Float): Color = /* RGB mix toward Color.White */
internal fun Color.darken(amount: Float): Color = /* RGB mix toward Color.Black */

public data class AeroOrnamentTokens(
    val glossHighlight: Color,
    val bevelLight: Color,
    val bevelShadow: Color,
    val rimLight: Color,
    val hoverGlow: Color,
    val grooveShadow: Color,
    val fillSplitTop: Color,
    val fillSplitBottom: Color,
) {
    public companion object {
        public fun derive(base: AeroColorScheme): AeroOrnamentTokens = AeroOrnamentTokens(
            glossHighlight = base.glassHighlight.lighten(0.15f),
            bevelLight = base.primary.lighten(0.25f),
            bevelShadow = base.primary.darken(0.20f),
            rimLight = base.glassBorder.lighten(0.10f),
            hoverGlow = base.primary.lighten(0.30f),
            grooveShadow = base.surface.darken(0.15f),
            fillSplitTop = base.primary.lighten(0.18f),
            fillSplitBottom = base.primary.darken(0.12f),
        )
    }
}
```
`[ASSUMED]` — all numeric magnitudes above (0.15f, 0.25f, 0.20f, etc.) are original design proposals from `.planning/research/ARCHITECTURE.md`, not measured or verified values. Treat as a starting point to refine during the three-theme spike (PRIM-16), not a locked spec. See Assumptions Log.

### Pattern 3: Native shadow ordering (D-02, confirmed by compile)

**What:** `dropShadow` precedes `.background()`/fill (drawn behind); `innerShadow` follows it (drawn on top, recessed).

**Example:**
```kotlin
// Source: showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt
// (compile-proven against the real CMP 1.11.1 jar, 2026-07-22 — the corrected source of truth)
Modifier
    .size(120.dp, 40.dp)
    .dropShadow(
        shape = shape,
        shadow = Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 2.dp)),
    )
    .background(color = Color(0xFF3A6EA5), shape = shape)
    .innerShadow(
        shape = shape,
        shadow = Shadow(radius = 2.dp, color = Color.White.copy(alpha = 0.45f), offset = DpOffset(0.dp, 1.dp)),
    )
```
Imports: `androidx.compose.ui.draw.dropShadow`, `androidx.compose.ui.draw.innerShadow`, `androidx.compose.ui.graphics.shadow.Shadow`, `androidx.compose.ui.unit.DpOffset` — `[VERIFIED: codebase]`, do not re-derive from `.planning/research/UPGRADE.md`'s Part 4 (its package path for the two modifier functions was wrong; the scratch file's KDoc documents the correction verbatim).

### Pattern 4: `rememberAeroInteractionState()` — booleans only, no resolved styling

**What:** Bundles `collectIsHoveredAsState`/`collectIsPressedAsState`/`collectIsFocusedAsState` into one call. Deliberately returns only booleans — each component picks its own `AeroSurfaceStyle` variant (`rest()`/`hovered()`/`pressed()`) based on those booleans; the shared helper does not resolve colors itself, since fill/rim/gloss targets differ too much per component to share a second abstraction level.

**Example:**
```kotlin
// Source: .planning/research/ARCHITECTURE.md Q4 (design skeleton); pattern already proven at
// AeroListItem.kt:57,74 (collectIsHoveredAsState + Modifier.hoverable pairing) — copy that
// pairing verbatim for any newly-hover-enabled component, do not invent pointer-position tracking.
internal data class AeroInteractionState(val hovered: Boolean, val pressed: Boolean, val focused: Boolean)

@Composable
internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return AeroInteractionState(hovered, pressed, focused)
}
```
`InteractionStates.kt`'s existing `internal` functions are already module-scoped (not package-scoped — established precedent from v2.0 Phase 7), so relocating the file to `components/common/` requires no visibility change; every Phase 17–19 component file can already call these functions today.

### Anti-Patterns to Avoid

- **Per-component bespoke gradient/bevel code:** Writing `AeroSwitch`'s track gradient inline, then `AeroSlider`'s groove inline, "because it's just a few lines each" — this is the exact `project_panelgroup_composable_dsl_pitfall`-class risk (two independently-maintained copies of the same visual idea silently drift). Every surface calls `drawAeroSurfaceCore`/`Modifier.aeroSurface`/`aeroThumbSurface` — zero component owns its own gradient-construction code.
- **Reaching for bare `Canvas`+`pointerInput` reflexively:** `AeroRangeSlider`'s bare-Canvas pattern exists because it had no M3 equivalent for its precision-drag requirement (locked PITFALL-03). `AeroSlider` is NOT in that position — M3's `thumb=`/`track=` slots exist specifically to avoid re-deriving semantics/keyboard/drag from scratch (Architecture Q5). Only drop the layer that actually blocks the look.
- **Treating `AeroColorScheme` as a growing bag of raw color fields:** Adding `glossHighlight2`, `bevelLight`, etc. directly as new 24th/25th/26th constructor parameters compounds the already-23-field data class and multiplies the three-theme audit surface. Use `AeroOrnamentTokens.derive(base)` — one algorithm, three themes for free, one trailing override for the rare hand-authored-custom-theme case.
- **Hardcoded pixel gradient stops (the exact PRIM-09 bug, do not reintroduce elsewhere):** Any `Brush.verticalGradient`/`horizontalGradient` call with a bare numeric literal for `startY`/`endY`/`startX`/`endX` instead of `size.height * fraction`/`size.width * fraction`. This is VER-01's grep-gate target (Phase 20) but the discipline starts here.
- **Draw-before-clip ordering (the exact PRIM-10 bug):** `.clip(shape)` must be the outermost modifier affecting a component's own paint. Everything meant to respect rounding — background, border, `drawBehind`, hover/press overlays — goes after (inside) `.clip()` in the chain.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Per-theme ornament colors | ~10 hand-tuned literals × 3 themes on `AeroColorScheme` | `AeroOrnamentTokens.derive(base)` | Hand-authoring silently breaks on `Classic`'s opaque tokens (Pitfall 11) — tuned-against-`AeroBlue`-only is the exact failure mode this project's own retrospective already names as its "biggest inefficiency" |
| Gradient rebuild cost | Rebuilding `Brush`/`Path` inside `drawBehind` per frame | `drawWithCache { onDrawBehind { ... } }` | Already a confirmed existing bug in `glassPanel`/`glassSurface` (Pitfall 1); scales badly the moment `AeroListItem` appears in a `LazyColumn` with dozens of rows |
| Soft glow/blur | Real Gaussian blur (`nativeCanvas` + `MaskFilter.makeBlur`, or `Modifier.blur`) for hover/focus rings | Double-stroke approximation (`aeroGlowRing`: inner crisp stroke + outer wider/fainter stroke) | Zero extra composited layer, zero Skia-type coupling risk (STACK.md Q1/Q4 — Skia API shape has already broken third-party libraries across CMP version bumps); cheap enough for components appearing in numbers |
| Keyboard/semantics for M3-dropping components | Reimplementing `Role.Button`, focus traversal, Enter/Space activation from scratch | `Modifier.clickable(role = Role.Button, indication = null, ...)` (Foundation-level, one layer below M3) | `AeroRangeSlider` is the negative precedent: zero `semantics`/`focusable`/keyboard handling because it went all the way to bare `Canvas`+`pointerInput` (Pitfall 8) — `Modifier.clickable` already supplies this for free |
| M3 Slider drag/keyboard/step-snap | Full custom `Canvas`+drag rewrite for `AeroSlider` | Keep M3 `Slider`, supply custom `thumb=`/`track=` slots (contingent on PRIM-18 spike passing) | Context7-verified: `thumb`/`track` slot API is long-stable, not new-in-1.4/1.5 — keeping M3 preserves `steps` snap, arrow-key nudge, `onValueChangeFinished`, and semantics for free (Architecture Q5) |

**Key insight:** Every "don't hand-roll" item above maps to a pitfall this project has either already hit once (`GlassModifiers.kt`'s three bugs, `AeroRangeSlider`'s zero-semantics precedent) or is structurally certain to hit again if the same shortcut is taken a second time. The Foundation phase's entire value is converting each of these from "per-component judgment call" into "call the shared primitive."

## Common Pitfalls

### Pitfall 1: Draw-time `Brush`/`Path` allocation inside `drawBehind` instead of `drawWithCache` (PRIM-13)
**What goes wrong:** `glassPanel` and `glassSurface` both currently allocate `Brush.verticalGradient(...)` fresh inside their `drawBehind {}` block on every single draw pass — confirmed at `GlassModifiers.kt:64-69` and `:91-97`.
**Why it happens:** `drawBehind` is the simplest API and "already works" for a 1-2 layer version; nobody notices allocation cost until layer count and instance count both rise (exactly what Phases 17-19 do).
**How to avoid:** Anything depending only on `size`/`cornerRadius`/`style` identity goes inside `drawWithCache { ... }`, built once and reused; only cheap draw calls and animated-color `State` reads run inside `onDrawBehind {}` every frame. Never rebuild a `Path` or a >2-stop `Brush` inside `onDrawBehind`; a 2-stop color-only `Brush` rebuild there is acceptable.
**Warning signs:** Frame drops specifically scaling with visible instance count (e.g., a dense `AeroListItem` list), not reproducing with 1-2 items.

### Pitfall 2: Draw-before-clip ordering — bounds-centred strokes losing their outer half (PRIM-10/12)
**What goes wrong:** `glassSurface` does `.drawBehind { ...Stroke(1dp)... }.clip(shape)` — the stroke draws before the clip, and `Stroke`-style strokes are bounds-centred (half outside, half inside), so the clip removes the outer 0.5dp, leaving an effective 0.5dp border (confirmed `GlassModifiers.kt:87-105`).
**Why it happens:** Compose's `.clip()` only affects draw calls nested *inside* it in the modifier chain; a `drawBehind` positioned before (outer to) `.clip()` is genuinely unaffected by it.
**How to avoid:** Lock the rule: `.clip(shape)` is the outermost modifier that can affect a component's own paint. `aeroSurface()`'s own chain (`[dropShadow] → .clip(shape) → [cached fill block] → [innerShadow]`) encodes this once so no per-component call site re-derives it. For the stroke itself, additionally inset by half its width (belt-and-suspenders).
**Warning signs:** A hand-drawn stroke that looks thinner than its declared width; hover/press overlays correctly rounded at rest but square at the corners when a state triggers.

### Pitfall 3: Dead/no-op API parameters surviving review (PRIM-11)
**What goes wrong:** `glassEffect(elevation: Dp = 4.dp)` imports `androidx.compose.ui.draw.shadow` but never calls it — confirmed at `GlassModifiers.kt:26-45`. Every `elevation = 2.dp` call site (`AeroSlider.kt:65`, `AeroRangeSlider.kt:286`) is a silent no-op.
**Why it happens:** An unused function parameter whose value is read into scope but never reaches a draw call triggers no compiler/IDE warning — unlike an unused local variable.
**How to avoid:** Either wire `elevation` to a real `dropShadow`/`Modifier.shadow` call (source-compatible, non-breaking) or remove the parameter entirely (minor-breaking, acceptable per D-03). Do not leave it dead while adding new real shadow-consuming primitives alongside it in the same file — actively confusing.
**Warning signs:** Changing a visual parameter's value between two extremes produces zero rendered difference.

### Pitfall 4: Hardcoded pixel gradient stops that don't scale with component size (PRIM-09)
**What goes wrong:** `glassSurface`'s gloss gradient uses `endY = 100f` as a **literal pixel** value (`GlassModifiers.kt:95`), not `size.height * fraction` like the already-correct sibling `glassPanel` three functions above it (`:68`, `size.height * 0.55f`). On any component shorter than ~100px-worth of actual pixels (essentially every one of the eight target components), the gloss band never completes — reads as a hard color band, not a fade.
**Why it happens:** Very likely written by copy-adjusting `glassPanel`'s pattern and losing the `size.height *` multiplier, or eyeballing one specific component's height and hardcoding it.
**How to avoid:** Every gradient stop expressed as a distance along the drawn shape must be `size.height * fraction`/`size.width * fraction`, never a bare Float literal. Keep dp-derived px values (e.g., a bevel that's always "2dp thick") explicitly separate from proportional fraction-of-size values in the same expression.
**Warning signs:** Gloss/gradient band looks proportionally different (thicker relative to shape) on `AeroSwitch` (18.dp) vs `AeroButton` (30.dp) vs `AeroListItem`'s 36.dp selection pill — a fast three-way comparison to run at the Foundation-phase spike.

### Pitfall 5: Ornamentation that reads correctly on one theme and categorically wrong on another (PRIM-02/14, PRIM-16)
**What goes wrong:** `AeroColorScheme.Classic` defines `glassSurface`/`glassBorder`/`glassHighlight` as fully **opaque** literals (e.g. `Color(0xFF333333)`), while `AeroBlue`/`AeroDark` define the same tokens as alpha-composited (e.g. `Color(0x30FFFFFF)`) — confirmed by direct read of `AeroColorScheme.kt:91-115` vs `:39-89`. A gloss/bevel effect tuned against `AeroBlue`'s translucent tokens renders on `Classic` as a flat opaque dark-grey patch, not merely "less pretty" — categorically different.
**Why it happens:** `AeroColorScheme` is a flat `data class` with no type-level distinction between "this token is meant to be translucent" and "this token is meant to be opaque."
**How to avoid:** Every new gradient fades toward `baseColor.copy(alpha = 0f)`, never a hardcoded `Color.Transparent` (PRIM-14) — this makes both opaque and translucent base tokens degrade sensibly ("less of itself" rather than assuming translucency existed). Check every new primitive against all three themes at first iteration (PRIM-16), not deferred to sign-off.
**Warning signs:** A new primitive's gloss looks like a translucent sheen on `AeroBlue`/`AeroDark` screenshots and a flat color block on `Classic` in the same comparison.

### Pitfall 6: Silently regressing M3-provided behavior when converting `AeroSlider` (relevant to PRIM-18's spike scope)
**What goes wrong:** M3 `Slider` currently provides keyboard arrow-nudge, `steps` snap, `onValueChangeFinished`, semantics/`Role`, and disabled-pointer-gating for free. `AeroRangeSlider` (already Canvas-based, no M3 to fall back to) has **zero** of this — confirmed no `semantics`/`focusable`/`onKeyEvent` anywhere in that file. If `AeroSlider`'s PRIM-18 spike fails and forces the fallback (full M3 removal), all of this must be explicitly re-derived, not assumed automatic.
**Why it happens:** The visual gap (M3's `SliderColors` can't express a two-tone gradient thumb) draws attention away from the invisible behavior M3 was quietly also providing.
**How to avoid:** The spike's job is precisely to avoid this path — confirm custom-sized slots fit inside M3's own layout math so ALL of the above stays automatic. Only if the spike fails does Pitfall 8's full M3-behavior checklist become load-bearing for Phase 18.
**Warning signs:** (For Phase 18, not this phase) Tabbing to a converted slider with no mouse and finding arrow keys do nothing.

### Pitfall 7: Ornamentation "breathing room" silently bumping default sizes
**What goes wrong:** The natural instinct when adding a rim/bevel/glow is to want extra dp around existing content so ornamentation doesn't feel clipped — but every component has hardcoded intrinsic dimensions consumers already build against (`AeroButton` `height: Dp = 30.dp`, `AeroSwitch`'s hardcoded `36×18.dp` track, `AeroListItem`'s hardcoded `36.dp` row height).
**Why it happens:** "Just add a couple dp" is the path of least resistance versus insetting the core shape or drawing ornamentation outside layout bounds without changing measured size.
**How to avoid:** Design new glow/shadow/rim to render either (a) inset within existing declared size, or (b) drawn outside layout bounds via unclipped `drawBehind` (Compose's draw phase isn't clipped to layout bounds by default). This phase doesn't touch any of the eight components' actual sizes, but the primitives it builds (`AeroSurfaceStyle` defaults, glow-ring geometry) determine whether Phase 17-19 can honor this without a fight.
**Warning signs:** Not directly checkable this phase (no component sizes change here) — but `AeroSurfaceStyle`'s default corner/gloss/glow dimensions should be designed with this constraint in mind since they're inherited wholesale by Phase 17-19.

## Code Examples

### `Color.lighten`/`darken` — RGB mix, not alpha (PRIM-01)
```kotlin
// Source: .planning/research/ARCHITECTURE.md Q3 — design skeleton, magnitudes are ASSUMED
internal fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)
internal fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha,
)
```
`[ASSUMED]` implementation shape — the RGB-mix-toward-White/Black requirement itself is `[VERIFIED: codebase]` (Classic's opaque tokens demonstrably need it, confirmed by direct read of `AeroColorScheme.Classic`), but the exact channel-mix formula is an implementation choice, not sourced from official docs.

### Source-compatible `AeroColorScheme` extension (PRIM-03)
```kotlin
// AeroColorScheme.kt — append-only change, all 23 existing fields UNCHANGED, same order
@Immutable
public data class AeroColorScheme(
    public val primary: Color,
    // ...all 23 existing fields, verbatim, unchanged order (confirmed via direct file read)...
    public val panelBackground: Color,
    public val ornamentOverride: AeroOrnamentTokens? = null,  // NEW — trailing, defaulted, source-compatible
)
```
Verified source-compatible for both positional and named existing constructor calls (Kotlin requires only trailing params to carry defaults for no-named-args call forms to keep compiling) — `[VERIFIED: codebase]`, confirmed by direct read of the current 23-field, no-default constructor.

### Dead-parameter fix pattern for `glassEffect` (PRIM-11, D-03)
```kotlin
// EITHER revive (source-compatible, existing elevation = 2.dp call sites start working):
@Composable
public fun Modifier.glassEffect(cornerRadius: Dp = 8.dp, elevation: Dp = 4.dp): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .dropShadow(shape, Shadow(radius = elevation, color = Color.Black.copy(alpha = 0.25f)))
        .background(brush = /* existing gradient */ TODO(), shape = shape)
        .border(width = 1.dp, color = colors.glassBorder, shape = shape)
}
// OR remove the parameter entirely (minor-breaking, acceptable in a major version) —
// decide on the three-theme review per D-03: revive if it improves cards/panels, remove if noise.
```

## State of the Art

| Old Approach (current `GlassModifiers.kt`) | New Approach (this phase) | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Brush.verticalGradient(...)` allocated fresh inside `drawBehind {}` | Geometry/brush built once in `drawWithCache { onDrawBehind { ... } }` | Phase 16 (PRIM-13) | Removes per-frame allocation cost that would otherwise scale with instance count in Phase 17-19's `AeroListItem`/`AeroButton` consumers |
| `endY = 100f` hardcoded pixel literal | `endY = size.height * fraction` | Phase 16 (PRIM-09) | Gloss band renders proportionally correct on components from 18.dp (`AeroSwitch`) to 36.dp (`AeroListItem`) |
| Stroke drawn before `.clip(shape)` | `.clip(shape)` outermost, stroke inset by half width | Phase 16 (PRIM-10) | Declared 1.dp border actually renders at 1.dp, not an effective 0.5dp |
| `elevation` parameter accepted, never applied | Either wired to real `dropShadow` or removed | Phase 16 (PRIM-11, D-03) | No dead API surface remains in a published library |
| Manual layered `drawRoundRect` glow as the ONLY option (STACK.md's pre-Phase-15 recommendation) | Native `dropShadow`/`innerShadow` available as an additional per-primitive option | Phase 15 (toolchain upgrade to CMP 1.11.1) | `.planning/research/STACK.md`'s Q1/Q5 "do not upgrade" recommendation is now moot — the capability exists; D-02 makes the choice per-primitive, not universal |
| `InteractionStates.kt` lives in `components/buttons/`, unused by 5 of 8 target components despite being importable | Relocated to `components/common/`, gains `rememberAeroInteractionState()` | Phase 16 (PRIM-15) | Removes the "why is a switch importing something from buttons" friction ahead of Phase 19's `AeroSwitch`/`AeroSegmentedControl` needing it fresh |

**Deprecated/outdated:** `.planning/research/STACK.md`'s recommendation to avoid `dropShadow`/`innerShadow` and stay on CMP 1.7.3 is superseded by the Phase 15 toolchain upgrade (now on 1.11.1) — do not follow that document's Q1/Q5 conclusion; follow `ScratchAeroShadowProof.kt` instead, which is the corrected, compile-proven Phase 15→16 handoff artifact.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `AeroOrnamentTokens`'s exact 8-field shape (`glossHighlight`, `bevelLight`, `bevelShadow`, `rimLight`, `hoverGlow`, `grooveShadow`, `fillSplitTop`, `fillSplitBottom`) | Architecture Pattern 2, Code Examples | Low — additive `data class`, easy to rename/add/remove fields before any component consumes it; explicitly flagged as Claude's discretion in CONTEXT.md |
| A2 | Lighten/darken magnitudes (0.15f, 0.18f, 0.20f, 0.25f, 0.30f, etc.) used in `derive()` | Architecture Pattern 2 | Medium — wrong values read as "too subtle" or "too harsh" per theme; mitigated by D-04's mandatory three-theme gallery review before these ship to Phase 17-19 |
| A3 | Exact gloss fraction within the D-01-mandated ~30-35% band | Architecture Pattern 1, D-01 | Low — D-01 sets a target range explicitly, not a hard literal; any value in-band satisfies the locked decision |
| A4 | `AeroSurfaceStyle`'s exact field list/naming (`fillTop`, `fillBottom`, `seam`, `glossAlpha`, `bevelLight`, `bevelShadow`, `rimColor`, `rimAlpha`, `dropShadow`, `innerShadow`) | Architecture Pattern 1 | Low — internal API shape, explicitly Claude's discretion per CONTEXT.md |
| A5 | `glassEffect(elevation)` revive-vs-remove outcome | Code Examples, D-03 | Medium — affects whether existing `elevation = 2.dp` call sites at `AeroSlider.kt:65`/`AeroRangeSlider.kt:286` keep compiling unchanged (revive) or need updating (remove); explicitly deferred to the three-theme review per D-03, not resolved by this research |
| A6 | Native `dropShadow`/`innerShadow` vs manual-gradient choice per primitive | Architecture Pattern 3, D-02 | Low — both options are proven-available; the choice is a visual-quality judgment call explicitly deferred to the three-theme review |
| A7 | M3 Slider slot-sizing spike outcome (pass = keep M3+slots at MEDIUM complexity for Phase 18; fail = fallback to full M3 removal at HIGH complexity) | Don't Hand-Roll, Pitfall 6 | High if assumed rather than run — Architecture Q5's Context7-verified API *shape* exists, but whether custom-*sized* slots specifically clip/misalign inside M3's internal layout math is unverified; this is the one item in this research that is a genuine open technical question, not a design-discretion item |

## Open Questions

1. **Does the M3 `Slider`'s internal layout math accept a custom-sized `thumb=`/`track=` slot without clipping or misaligning it?**
   - What we know: The `thumb: @Composable (SliderState) -> Unit` / `track: @Composable (SliderState) -> Unit` slot API itself is Context7-verified as long-stable on `androidx.compose.material3` (HIGH confidence, official reference, cross-checked across two doc entries per `.planning/research/ARCHITECTURE.md` Q5). Material3 is pinned at the explicit stable `1.9.0` coordinate in this project (`library/build.gradle.kts:22`).
   - What's unclear: Whether `Slider`'s own internal height/track-position math tolerates a thumb/track sized meaningfully differently from `SliderDefaults.Thumb`/`SliderDefaults.Track` (target ~16-18.dp thumb per FEATURES.md A12) without clipping or offset artifacts.
   - Recommendation: Run this as a standalone scratch composable spike (mirroring `ScratchAeroShadowProof.kt`'s already-proven pattern: compile + visually inspect, document the verbatim result in KDoc, do NOT wire into any real showcase screen) before Phase 16 closes. Treat the result as a hard gate for Phase 18's architecture, per STATE.md's already-locked position.

2. **Should `glassEffect(elevation)` be revived or removed?**
   - What we know: Reviving is source-compatible and low-risk; removing is a minor breaking API change acceptable in a major version (this is v3.0).
   - What's unclear: Whether a real shadow actually improves the two current call sites (`AeroSlider`'s and `AeroRangeSlider`'s drag-tooltip pills) enough to justify keeping the parameter, or whether it reads as noise.
   - Recommendation: Decide during the D-04 gallery review, after seeing both options rendered on all three themes — this is explicitly deferred to Claude's discretion on the review per D-03, not something this research should pre-decide.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Kotlin | Entire phase | ✓ | 2.4.10 (Phase 15 locked) | — |
| Compose Multiplatform (Desktop) | `dropShadow`/`innerShadow`, `drawWithCache` | ✓ | 1.11.1 (Phase 15 locked, `gradle/libs.versions.toml:3`) | — |
| Material3 | PRIM-18 spike (`Slider` slot API) | ✓ | 1.9.0, explicit stable pin (`library/build.gradle.kts:22`) | — |
| `ScratchAeroShadowProof.kt` compile proof | D-02 native shadow decision | ✓ | Confirmed compiling at Phase 15 close | — |
| JitPack / consumer-app verification | N/A this phase (no release cut) | — | — | — |

No missing dependencies — this phase requires zero tools beyond what Phase 15 already locked and verified.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Kotlin Test + JUnit Jupiter (`libs.kotlin.test`, `libs.junit.jupiter`), plus `compose.uiTest`/`compose.desktop.currentOs` for `runComposeUiTest`-based UI tests (established Phase 14) |
| Config file | `library/build.gradle.kts` (`tasks.test { ... }`, confirmed present) |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.theme.*"` |
| Full suite command | `./gradlew :library:test` (232 tests baseline per STATE.md, pre-Phase-16) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PRIM-01 | `Color.lighten()`/`darken()` produce correct RGB-mixed output at boundary values (0f, 1f, mid) | unit | `./gradlew :library:test --tests "com.mordred.aero.theme.ColorMathTest"` | ❌ Wave 0 |
| PRIM-02 | `AeroOrnamentTokens.derive(base)` produces non-null, distinct tokens for all three built-in presets | unit | `./gradlew :library:test --tests "com.mordred.aero.theme.AeroOrnamentTokensTest"` | ❌ Wave 0 |
| PRIM-03 | Existing `AeroColorScheme` constructor calls (positional + named) still compile; `ornamentOverride` defaults to `null` | unit (compile-time assertion + runtime default check) | `./gradlew :library:test --tests "com.mordred.aero.theme.AeroColorSchemeTest"` | ✅ (extend existing `AeroColorSchemeTest.kt`) |
| PRIM-09/10/11 | `glassSurface`/`glassPanel`/`glassEffect` render without exception; gradient stop values are proportional (not literal) | unit (pure logic where extractable) + manual three-theme visual check | `./gradlew :library:test --tests "com.mordred.aero.theme.GlassModifiersTest"` | ❌ Wave 0 — no `GlassModifiersTest.kt` currently exists |
| PRIM-13 | New `drawWithCache`-based primitives don't rebuild `Brush`/`Path` per frame | manual (profiling) | Dense synthetic list smoke check per Pitfall 1's guidance — not a deterministic automated assertion | manual-only, justified: draw-time allocation isn't observable via `runComposeUiTest` semantics assertions |
| PRIM-15 | `rememberAeroInteractionState()` returns correct booleans as hover/press/focus toggle | UI test (`runComposeUiTest`) | `./gradlew :library:test --tests "com.mordred.aero.components.common.InteractionStatesTest"` | ❌ Wave 0 |
| PRIM-16 | Every new primitive renders (no exception) under `AeroBlue`/`AeroDark`/`Classic` | manual (three-theme showcase gallery review, D-04) | N/A — human sign-off, per this project's own established pattern (STACK.md Q6: no viable automated pixel-diff for Compose Desktop) | manual-only, justified |
| PRIM-17 | Full-library smoke pass shows no broken/ugly rendering across ~50 components | manual (showcase full run-through, all 3 themes) | N/A — human sign-off | manual-only, justified per D-05's "broken/ugly, not pixel-identical" bar |
| PRIM-18 | M3 Slider accepts custom-sized `thumb=`/`track=` slots without clip/misalign | manual (scratch composable spike, visual inspection) | N/A — this is inherently a visual-layout question, no assertion framework exists for "does this look clipped" | manual-only, justified; document result in the spike file's KDoc per the `ScratchAeroShadowProof.kt` precedent |

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests "com.mordred.aero.theme.*"` (fast, scoped to the theme package this phase touches)
- **Per wave merge:** `./gradlew :library:test` (full 232+ suite, confirms no regression in the ~40 out-of-scope components' consumers)
- **Phase gate:** Full suite green + D-04 gallery three-theme sign-off + PRIM-17 full-library smoke pass, all before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt` — covers PRIM-01
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt` — covers PRIM-02
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/GlassModifiersTest.kt` — covers PRIM-09/10/11 (no such file currently exists, confirmed by directory listing — every other `theme/` file has a sibling test except `GlassModifiers.kt`)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/common/InteractionStatesTest.kt` — covers PRIM-15 (new package, new test directory)
- Framework install: none — `compose.uiTest`/JUnit Jupiter/kotlin-test already present in `library/build.gradle.kts`

## Security Domain

**Applicability note:** This is a desktop, single-process, no-network, no-persistence UI drawing library. No authentication, session, network input, or stored-credential surface exists in this phase's scope. Most ASVS categories are structurally not applicable — documented explicitly below rather than silently omitted, per this project's own "state explicitly, don't leave blank" discipline.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | No auth surface in a UI drawing library |
| V3 Session Management | No | No session concept |
| V4 Access Control | No | No access-controlled resources |
| V5 Input Validation | Partial | `AeroSurfaceStyle`/`AeroOrnamentTokens` are internal Kotlin data classes constructed by library authors (Phase 17-19), not parsed from untrusted external input (JSON/network/file) — standard Kotlin type-safety (non-null `Color`, `Dp` types) is the applicable control, not a runtime validation library |
| V6 Cryptography | No | No cryptographic operation in this phase |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Resource exhaustion via unconditional infinite animation (Pitfall 10, not newly introduced this phase but adjacent to `aeroGlowRing`'s hover-gated design) | Denial of Service (mild, local-only) | `aeroGlowRing` and all new primitives are gated to interactive states (`Modifier.then(if (active) Modifier.aeroGlowRing(...) else Modifier)`), never a `rememberInfiniteTransition` — costs zero when inactive, per Architecture Q2's pass-count table |
| Third-party dependency supply-chain risk (Pitfall 12/STACK.md Q4 — Skia API instability precedent, Haze library breakage) | Tampering (transitive dependency) | This phase adds **zero** new external dependencies (Package Legitimacy Audit above) — the one identified risk vector (adopting a third-party blur/glow library) is explicitly avoided by using only APIs already in the CMP 1.11.1 BOM |

## Sources

### Primary (HIGH confidence)
- Direct codebase reads this session: `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt`, `AeroColorScheme.kt`, `AeroTheme.kt`; `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt`; `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt`; `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt`; `showcase/src/main/kotlin/com/mordred/showcase/sections/FoundationSection.kt`; `library/build.gradle.kts`; `gradle/libs.versions.toml`
- `ScratchAeroShadowProof.kt` — compile-proven `dropShadow`/`innerShadow`/`Shadow`/`DpOffset` signatures against the real CMP 1.11.1 jar via `javap`, 2026-07-22
- `.planning/research/ARCHITECTURE.md` — Context7-verified M3 `Slider` `thumb=`/`track=` slot API (`/websites/developer_android_reference_kotlin_androidx_compose_material3`), cross-checked across two independent doc entries

### Secondary (MEDIUM confidence)
- `.planning/research/FEATURES.md` — Aero visual device catalog (A1-A13), per-component treatment (B1-B8); numeric alphas/proportions explicitly flagged there as original design proposals, MEDIUM confidence as "faithful in spirit," cross-checked against community recreation projects (7.css, PresentationTheme.Aero)
- `.planning/research/PITFALLS.md` — 14 named pitfalls, HIGH confidence where grounded in this repo's own confirmed code (cited file:line), MEDIUM for general Compose/Skia platform behavior verified against official docs

### Tertiary (LOW confidence / superseded)
- `.planning/research/STACK.md` — its Q1/Q5 "do not upgrade Compose" recommendation is **superseded** by the completed Phase 15 toolchain upgrade; retained here only as historical context for why `dropShadow`/`innerShadow` weren't available pre-Phase-15
- `.planning/research/UPGRADE.md` Part 4 — its exact package path for `dropShadow`/`innerShadow` (`androidx.compose.ui.graphics.shadow`) was **wrong**; corrected by `ScratchAeroShadowProof.kt`'s `javap` inspection to `androidx.compose.ui.draw`. Use the scratch file, not this document, for the exact import paths

## Metadata

**Confidence breakdown:**
- Standard stack (no new deps, existing CMP/M3 APIs): HIGH — every API cited is either directly read from this codebase or compile-proven in `ScratchAeroShadowProof.kt`
- Architecture (single `drawAeroSurfaceCore`, token derivation design): MEDIUM-HIGH — the structural design (one function, multiple exposure paths; algorithmic derivation over hand-authored literals) is well-justified against this project's own confirmed bugs and precedents, but exact field names/magnitudes are original synthesis, explicitly Claude's discretion per CONTEXT.md
- Pitfalls: HIGH — 6 of 7 cited pitfalls are grounded in this repo's own confirmed code with exact file:line references, not speculative
- PRIM-18 spike outcome: UNKNOWN (by design) — this is the one genuine open technical question this research could not resolve without running the spike itself; flagged as A7 in the Assumptions Log and as Open Question 1

**Research date:** 2026-07-22
**Valid until:** Effectively pinned to the Phase 15 toolchain (Kotlin 2.4.10 / CMP 1.11.1 / Material3 1.9.0) — re-verify signatures if any future phase bumps these versions again. No natural 30-day decay for the architectural recommendations themselves (they don't depend on external service availability).
