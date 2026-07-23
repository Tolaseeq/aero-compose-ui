# Phase 18: Range - Context

**Gathered:** 2026-07-23
**Status:** Ready for planning

<domain>
## Phase Boundary

`AeroSlider`, `AeroRangeSlider`, and `AeroProgressBar` are restyled from flat Material3-looking controls into genuine Aero-glass surfaces — **recessed track grooves + raised, glossy thumbs/fills** — by consuming the Phase 16 primitives (`aeroThumbSurface`/`drawAeroThumb` raised thumb PRIM-07, recessed track-groove PRIM-08, `drawAeroSurfaceCore`, `aeroGlowRing`, `AeroSurfaceStyle`, `rememberAeroInteractionState()`). Public API and behavior stay 1:1.

**In scope:**
- `AeroSlider` **keeps** Material3's `Slider` and supplies custom `thumb =` / `track =` slots — repaired architecture, not full M3 removal (VRNG-01, gated by the PRIM-18 spike which PASSED in Phase 16). Recessed groove, raised glossy thumb, hover/focus on the thumb (VRNG-03).
- `AeroSlider` behavior parity: drag, keyboard-arrow nudge, `steps` snapping (not a dead param), `onValueChangeFinished`, semantics all identical to before (VRNG-02).
- `AeroRangeSlider` (stays custom `Canvas` — M3 banned per PITFALL-03): same groove + raised-thumb treatment via the `drawAeroSurfaceCore` direct-call path; **only rendering changes, drag logic and structure untouched** (VRNG-04); hover/press independent per thumb (VRNG-05).
- `AeroProgressBar`: recessed track bed, accent gradient fill with gloss (VRNG-06); periodic running sheen present but **OFF by default** (VRNG-07); indeterminate mode restyled keeping the existing **1500ms restart timing, no ping-pong** (VRNG-08).
- Pattern 3 reused anywhere animation + drag write the same value: animation reads a target-only value, drag writes directly, `isDragging` switches to `snap()` (VRNG-09).

**Explicitly NOT in this phase:**
- Any change to public API signatures, default sizes, or behavior beyond the new visual (milestone constraint — API/behavior stay 1:1).
- Restyling the other five target components (`AeroButton`/`AeroOutlinedButton` were Phase 17; `AeroSwitch`/`AeroSegmentedControl`/`AeroListItem` are Phase 19).
- Building new primitives (that was Phase 16 — this phase consumes them).
- **Adding accessibility semantics / keyboard support to `AeroRangeSlider`** — it currently has zero semantics (custom Canvas). Closing that gap is deferred to its own future phase (see Deferred Ideas); Phase 18 stays strictly render-only for the range slider per VRNG-04.

</domain>

<decisions>
## Implementation Decisions

### Track fill & thumb color identity
- **D-01:** The **active** portion of the track (left of the thumb on `AeroSlider`) and the `AeroProgressBar` fill are painted as **accent/primary two-tone glass** — the same visual family as Phase 17's filled `AeroButton` (Phase 17 D-01), so slider-fill, progress-fill, and the filled button read as one system. The **thumb is a neutral raised glass nub** (not accent-colored). The **inactive** portion of the track is a **recessed neutral groove**. This is the classic Win7 slider identity (blue active fill, neutral movable thumb). — **Reversibility:** costly — the accent-vs-neutral split is baked into how the slider track slots and the range-slider Canvas resolve their `AeroSurfaceStyle`; switching later means re-reviewing all three components on three themes.
- **D-02:** The accent fill uses the **same two-tone-glass treatment as the filled button** (top-down accent gradient + top gloss + soft seam/bevel), but the **gloss is proportional to the thin track height** (~30–35% per Phase 16 D-01), never a pixel literal. Exact seam/bevel magnitude on a thin track is Claude's discretion within the "spirit of Aero" band (a thin 4dp track cannot show the full bevel a 30dp button does — err toward clean over ornate). — **Reversibility:** reversible — a tuning of stop/gloss values, local to the fill painter.

### Thumb states (hover / focus / press)
- **D-03:** Thumb **hover** and **focus** reuse the Phase 17 button glow idiom: `aeroGlowRing` on hover plus a light brighten (Phase 17 D-03), and a **constant `aeroGlowRing`** on keyboard focus (Phase 17 D-04) — one shared glow language across the library. **Load-bearing check:** the outer glow bloom must NOT be clipped by M3 `Slider`'s internal `thumb =` slot bounds; verify at first implementation (respect the `aeroGlowRing`-before-`aeroSurface` ordering rule, and confirm the slot gives the bloom room or size the thumb slot to include it). — **Reversibility:** reversible — swap the state visual per thumb.
- **D-04:** Thumb **press/drag** does **NOT** adopt the button's recessed/inverted press (Phase 17 D-02). A dragged slider thumb **stays raised** (you are moving it — it should read as picked-up), with the glow **intensified/darkened** and a slightly brighter gloss as the active-drag cue. Applied **independently per thumb** on `AeroRangeSlider` (VRNG-05). — **Reversibility:** reversible.

### AeroProgressBar look
- **D-05:** The determinate fill **is glass with a static top gloss ON by default** (part of the base glass look, per D-01/D-02). The **animated running sheen** (the highlight that periodically travels across the fill) is a **separate optional parameter, default `false`** — this is the exact boundary VRNG-07 draws ("periodic sheen OFF by default"): static gloss is always on, the traveling highlight is opt-in. — **Reversibility:** reversible — a default flag flip.
- **D-06:** The restyled **indeterminate** indicator is a **single accent glass segment** (same fill as the determinate fill) sweeping left→right across the recessed bed, with **soft edges that fade to `baseColor.copy(alpha = 0f)`** (never `Color.Transparent`, per PRIM-14). Keeps the existing **1500ms restart timing, no ping-pong** (VRNG-08). Exact segment width/gradient shape is Claude's discretion on the three-theme review. — **Reversibility:** reversible.

### Disabled state (all three components)
- **D-07:** Disabled **mirrors Phase 17 D-05 "flatten to dead"** — gloss/bevel removed, gradient collapses toward near-flat, color muted, so the control reads as genuinely dead (loses depth), not a translucent version of the live control. Replaces `AeroSlider`'s current `alpha = 0.4f` M3 `SliderColors` disabled and `AeroRangeSlider`'s flat disabled. **Because `AeroSlider` now draws its own `thumb =`/`track =` slots, the disabled rendering lives in our slots — do NOT rely on M3 `SliderColors` for disabled appearance** (M3 disabled colors are superseded by the flattened Aero surface). — **Reversibility:** costly — the disabled transform is shared across all three range components (and echoes the button rule); re-tuning re-touches everything on three themes.

### Claude's Discretion
- Native `Modifier.dropShadow`/`innerShadow` vs manual gradient for each depth cue (raised thumb, recessed groove/bed, inner-shadow rim) — decided on the three-theme review per Phase 16 D-02. Signatures are in `ScratchAeroShadowProof.kt`.
- Exact groove depth / track thickness / thumb size within the existing default sizes (public sizes must not change — VRNG scope), and the exact seam/bevel magnitude on thin tracks (D-02).
- Exact indeterminate segment width and gradient falloff shape (D-06).
- Exact hover-vs-focus glow differentiation on the thumb (intensity/tint), mirroring the Phase 17 D-03/D-04 discretion, so both are distinguishable when co-active.
- Whether the disabled flattened surface is a distinct `AeroSurfaceStyle` variant or a transform of the rest style (mirror whatever Phase 17 chose for buttons for consistency).
- The existing drag-tooltip glass pill on the two sliders — keep, restyle to match, or leave as-is (it is the only pre-existing glass in these files); low priority, on-review.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Requirements & success criteria (primary)
- `.planning/REQUIREMENTS.md` §"Range (VRNG)" — **VRNG-01..VRNG-09** (the nine requirements this phase closes), lines ~59–69. Note VRNG-01..09 are written in Russian; also VRNG-F01 (Win7-authentic ping-pong for indeterminate) is a **future/deferred** flag, explicitly NOT this phase (VRNG-08 forbids ping-pong).
- `.planning/ROADMAP.md` §"Phase 18: Range" — the five explicit Success Criteria (M3-kept slider with custom slots + groove/raised thumb/hover-focus; behavior parity; range-slider groove+raised-thumb w/ per-thumb hover-press & untouched drag logic; progress recessed bed + gloss fill, sheen OFF by default, indeterminate restyled at 1500ms no ping-pong; Pattern 3 reuse).

### Foundation this phase consumes (load-bearing — read before implementing)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — shipped primitives: `DrawScope.drawAeroSurfaceCore(style, cornerPx)`, `Modifier.aeroSurface(style, shape)`, `Modifier.aeroGlowRing(active, glowColor, cornerRadius)`, **and the raised-thumb + recessed-track-groove pair** (`aeroThumbSurface`/`drawAeroThumb` PRIM-07, groove PRIM-08 — the direct consumers of this phase). **Ordering rule (in-file KDoc):** `Modifier.aeroGlowRing(...).aeroSurface(...)` — glow BEFORE surface (the surface's outer `.clip(shape)` would erase the bloom); this is the crux of the D-03 slot-clipping check.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` — the `@Immutable AeroSurfaceStyle` data class and `AeroSurfaceStyle.rest(base, cornerRadius)` neutral factory. D-01 requires the active track / progress fill drive stops from the accent/`primary` color, not `rest()` verbatim (mirror how the Phase 17 filled button did it); the thumb and inactive groove use the neutral path.
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — `rememberAeroInteractionState(source): AeroInteractionState` (hovered/pressed/focused raw booleans) + `ANIMATION_DURATION_MS = 150`. The single collector both sliders switch to (per-thumb for the range slider).
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` + `AeroColorScheme.kt` — ornament token source + `ornamentOverride` escape hatch; Classic tokens are **opaque** (RGB lighten/darken math, not alpha, for derived stops).

### Sibling phase whose decisions this phase mirrors (load-bearing for consistency)
- `.planning/phases/17-buttons/17-CONTEXT.md` — Phase 17's D-01 (accent two-tone filled identity → this phase's D-01/D-02), D-02 (pressed recess — deliberately NOT carried to the thumb, see D-04), D-03/D-04 (hover glow + brighten / constant focus glow → this phase's D-03), D-05 (disabled "flatten to dead" → this phase's D-07). The point is one shared visual language, not divergence.
- `.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md` — Phase 16 D-01 (moderate "spirit of Aero" fidelity: gloss ~30–35% height, soft seam, subtle bevel — the fills inherit it) and D-02 (native-shadow-vs-gradient per cue, decided on the review). Also the PRIM-18 slider slot-sizing spike outcome (custom slots fit — this phase depends on that verdict).
- `.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md` — the analog map (primitive wiring, `AeroListItem` hover precedent, thumb/groove usage).

### Institutional memory / precedent (load-bearing)
- `.planning/STATE.md` §"Baseline Findings" (per-component current-surface audit: why these three read Material — `AeroSlider` is entirely M3-drawn; `AeroRangeSlider` is flat `drawLine`/`drawCircle` on Canvas; `AeroProgressBar` is flat nested Boxes) and §"Architecture positions locked for planning" (`AeroSlider` keeps M3 with custom slots; `AeroRangeSlider` stays Canvas; one `drawAeroSurfaceCore` exposed multiple ways; ProgressBar sheen default-OFF + indeterminate 1500ms no-ping-pong LOCKED; Pattern 3 is the locked answer for animation-vs-drag).
- `.planning/PROJECT.md` §"Key Decisions" — `detectDragGestures` banned for Canvas drag (use `awaitPointerEventScope` manual loop, PITFALL-03 — do NOT touch this on the range slider); single-`drawBehind`/`drawWithCache` glass perf baseline (build brushes/geometry in cache, no per-frame `Brush` rebuild, PRIM-13); `Color.Transparent` is the anti-pattern (PRIM-14 — fade to `baseColor.copy(alpha = 0f)`); three-theme review mandatory + repro-must-exercise-the-path (v2.0.3/v2.0.4 false-positive-sign-off lesson).

### Current files this phase modifies
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` — currently M3 `Slider` + `SliderDefaults`/`SliderColors`, glass tooltip pill, `glassEffect`. Gains custom `thumb =`/`track =` slots (recessed groove + raised glossy thumb, accent active fill, hover/focus glow, D-07 disabled) — M3 container kept.
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` — custom `Canvas`, manual `awaitPointerEventScope` drag (297 lines). **Rendering only** replaced (groove + raised thumbs + accent active span + per-thumb hover/press glow + D-07 disabled) via `drawAeroSurfaceCore` direct calls; drag logic, `snapToStep`, `RangeThumb`, structure untouched (VRNG-04). No semantics added (deferred).
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt` — currently flat nested Boxes + `infiniteRepeatable` indeterminate (114 lines). Gains recessed bed, accent glass fill with static gloss, optional running-sheen param (default false), restyled sweeping-segment indeterminate at 1500ms.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/...` — the section(s) demoing sliders/range/progress, updated to exercise all states × three themes for sign-off (Phase 20 consolidates the formal sign-off, but per-component demos land here).

### No external specs/ADRs
- No external ADRs or standalone spec docs — requirements are fully captured in REQUIREMENTS.md (VRNG), ROADMAP.md Success Criteria, the mirrored Phase 16/17 CONTEXT decisions, and the decisions above. No `*-SPEC.md` exists for this phase.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Phase 16 thumb/groove primitives** (`aeroThumbSurface`/`drawAeroThumb` PRIM-07, recessed groove PRIM-08) — the exact drawing this phase was blocked on; do not re-derive. Both sliders' thumbs and all three components' recessed track beds come from here.
- **`drawAeroSurfaceCore` direct-call path** — the Canvas-owning route (`AeroRangeSlider`) uses direct calls, not the `Modifier.aeroSurface` Box route. Same core implementation, no third copy.
- **`rememberAeroInteractionState(source)`** — hovered/pressed/focused collector; `AeroSlider` wires one, `AeroRangeSlider` wires one **per thumb** (VRNG-05).
- **Phase 17's accent-fill / disabled-flatten / hover-glow style resolvers** — whatever Phase 17 named its accent-`AeroSurfaceStyle` derivation, `flattenDisabled`, and `hoverLighten` transforms, reuse the same helpers so the slider fill and disabled read identically to the button (D-01/D-07). Do not fork parallel copies.

### Established Patterns
- **Pattern 3** (`AeroPanelGroup` precedent, locked) — the answer for "animation vs. drag write the same value": animation reads a target-only value, drag writes directly, `isDragging` toggles to `snap()`. VRNG-09 reuses it explicitly on the slider/range-slider thumbs.
- **`Color.Transparent` is the anti-pattern (PRIM-14)** — every gradient fade (fill edges, indeterminate segment falloff, groove) fades to `baseColor.copy(alpha = 0f)` so Classic (opaque tokens) never renders a flat block.
- **Single `drawBehind`/`drawWithCache` glass (PRIM-13)** — build brushes/geometry in cache, no per-frame `Brush` rebuild; critical on the animated indeterminate/sheen paths.
- **`detectDragGestures` banned on Desktop Canvas (PITFALL-03)** — `AeroRangeSlider`'s manual `awaitPointerEventScope` loop is load-bearing; VRNG-04 forbids touching it.
- **`steps` must not become a dead param (VRNG-02)** — the M3 `Slider` snapping and `AeroRangeSlider`'s `snapToStep` both stay live after the restyle.

### Integration Points
- `LocalAeroColors` inside `AeroTheme {}` supplies `AeroColorScheme` + `AeroOrnamentTokens`; the components resolve their `AeroSurfaceStyle` from it (active fill = accent-driven per D-01; thumb + inactive groove = neutral).
- `AeroSlider`'s custom `thumb =`/`track =` slots plug into M3 `Slider`'s internal layout — the PRIM-18 spike confirmed custom-sized slots fit without clipping/misalignment; the one open runtime check is the D-03 glow-ring bloom fitting the thumb slot bounds.
- The drag-tooltip glass pill already present in both sliders is the only pre-existing glass in these files — keep it consistent with the new surfaces.

</code_context>

<specifics>
## Specific Ideas

- Slider = "classic Win7 slider": blue accent glass on the filled/active portion, a neutral raised glass thumb, an inactive neutral recessed groove (D-01).
- The accent fill on tracks/progress reads as the same material as the Phase 17 filled button, just proportioned for a thin track (D-02) — one Aero family, not three lookalikes.
- A dragged thumb feels "picked up" (stays raised + glow intensifies), never pushed-in — the deliberate divergence from the button's press-recess (D-04).
- Progress fill is glass-with-gloss out of the box; the traveling sheen is a deliberate opt-in for the Win7-progress look, default off (D-05).
- Indeterminate = one accent glass segment sweeping across the recessed bed with soft fading edges (D-06), 1500ms restart, no ping-pong.
- Disabled controls go "dead" (flattened, muted), matching the buttons — not a faded live control (D-07).

</specifics>

<deferred>
## Deferred Ideas

- **`AeroRangeSlider` accessibility semantics + keyboard support** — the component currently exposes zero semantics (custom Canvas; flagged in Phase 17 VBTN-04 as "the zero-semantics precedent must not repeat"). The user's explicit call: Phase 18 stays **strictly render-only** (VRNG-04), and adding `Modifier.semantics` (`progressBarRangeInfo`/role) + keyboard-arrow support to the range slider is its own future phase / backlog item — not folded into Range, to protect the phase's zero-regression goal. **Do not lose this** — it is a real accessibility gap awaiting its own phase.
- **`VRNG-F01` Win7-authentic ping-pong indeterminate** (bar decelerating at the edges) — already a future flag in REQUIREMENTS.md, explicitly excluded by VRNG-08 (no ping-pong this milestone). Candidate for a later polish phase.

</deferred>

---

*Phase: 18-range*
*Context gathered: 2026-07-23*
