# Phase 17: Buttons - Context

**Gathered:** 2026-07-23
**Status:** Ready for planning

<domain>
## Phase Boundary

`AeroButton` and `AeroOutlinedButton` are restyled from flat Material3-container buttons into genuine Aero-glass controls, painted by **one shared internal surface composable** so the two variants cannot visually drift. Both consume the Phase 16 primitives (`Modifier.aeroSurface`, `Modifier.aeroGlowRing`, `AeroSurfaceStyle`, `rememberAeroInteractionState()`). Full hover / press / focus / disabled states in Aero logic.

**In scope:** two-tone gradient fill with a soft seam, proportional top gloss, inner bevel, outer contour (VBTN-01); Aero-idiom hover/press/focus/disabled (VBTN-02); hover overlay clipped to the rounded shape — square-corner bleed eliminated (VBTN-03); `Role.Button` semantics + Space/Enter keyboard activation preserved via `Modifier.clickable(role = Role.Button, ...)` after the M3 container is dropped (VBTN-04); equivalent outlined-variant treatment (VBTN-05); one shared internal surface composable backing both buttons (VBTN-06); the shared pressed-fill code is written so Phase 19's `AeroSegmentedControl` selected segment can reuse it.

**Explicitly NOT in this phase:** any change to public API signatures, default sizes, or behavior beyond the new visual (milestone constraint — public API/behavior stay 1:1); restyling any other of the eight target components (Phases 18–19); `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` (not in the eight-target set — untouched); building new primitives (that was Phase 16 — this phase consumes them).

</domain>

<decisions>
## Implementation Decisions

### Filled-button identity
- **D-01:** `AeroButton`'s filled variant keeps its **accent/primary color identity** (the current blue Aero action button), but rendered as two-tone glass — gloss + soft seam + inner bevel — instead of the flat `primary.copy(alpha = 0.8f)`. It must read as a classic Win7 blue default-action button, visibly distinct from neutral panels/cards. This means the button's `AeroSurfaceStyle` is **not** the neutral `AeroSurfaceStyle.rest(base)` default (which derives from neutral glass ornament tokens); the fill stops are driven from the accent/`primary` color while still passing through the shared surface primitive. — **Reversibility:** costly — the accent-vs-neutral choice is baked into the shared internal surface composable both buttons and (downstream) the segmented control inherit; switching to neutral later means re-reviewing both buttons on three themes.

### Pressed state
- **D-02:** Press renders as **fully recessed** — the gradient inverts (dark-top / light-bottom) **plus an inner shadow rim**, so the button looks physically pushed in. The current `scale = 0.97f` graphicsLayer shrink is **removed** (superseded by the recess). This is the strongest Aero idiom and is deliberately chosen because Phase 19's `AeroSegmentedControl` reuses this exact pressed-fill code for its recessed "selected segment" look. — **Reversibility:** costly — this pressed-fill code is a cross-phase shared asset (VBTN + Phase 19 VSEL); re-tuning it later re-touches the segmented control too. Whether the inner shadow uses native `Modifier.innerShadow` or a gradient rim is per-D-02-of-Phase-16 (Claude's discretion on the review).

### Hover state
- **D-03:** Hover = **glow ring + fill brightening** — the outer `aeroGlowRing` bloom PLUS the surface gloss/fill lightens, so the whole button "lights up" like Win7 Aero on mouseover (not just an outer ring). Replaces the current flat `drawRect(buttonHover)` overlay.

### Focus state
- **D-04:** Keyboard focus = a **constant `aeroGlowRing`** (the same glow primitive as hover, but persistent and tuned to a slightly different intensity/tint so focus is distinguishable from hover when both are active). Replaces the current solid 2.dp `borderSelected` rectangle. Focus must remain clearly visible for keyboard navigation on all three themes.

### Disabled state
- **D-05:** Disabled **flattens the glass** — gloss and bevel are removed and the gradient collapses toward near-flat, plus the color is muted — so a disabled button reads as genuinely "dead" (loses depth), not merely a translucent version of the live button. Replaces the current uniform `alpha = 0.4f`.

### Outlined variant
- **D-06:** `AeroOutlinedButton` = **faint glass fill + Aero contour** — a near-transparent glass surface (a whisper of gloss/gradient) inside a bright Aero bevel-contour, clearly a lighter sibling of the filled button. Painted by the **same shared internal surface composable** as `AeroButton` (VBTN-06), differing only in fill opacity / contour emphasis — not a separate code path. Its hover/press/focus/disabled follow D-02..D-05 in the lighter register. — **Reversibility:** costly — the "same shared surface, param-differentiated" structure is the VBTN-06 guarantee; splitting outlined into its own painter later reintroduces the drift risk this phase exists to remove.

### Claude's Discretion
- Native `Modifier.dropShadow`/`innerShadow` vs manual gradient for each depth cue (drop, inner-shadow recess, rim) — settled on the three-theme review per Phase 16 D-02. Signatures proven in `ScratchAeroShadowProof.kt`.
- Exact focus-vs-hover glow differentiation (intensity/tint deltas) so both are distinguishable when co-active (D-03/D-04).
- Exact gloss fraction, seam softness, and bevel magnitude within the D-01 (Phase 16) "moderate spirit of Aero" band; exact accent-fill stop derivation for the filled button.
- The shape/parameters of the shared internal surface composable (name, param set, how filled-vs-outlined and rest/hover/press/focus/disabled are expressed) — the VBTN-06 vehicle; must be structured so the pressed-fill is cleanly reusable by Phase 19.
- Whether disabled's flattened surface is a distinct `AeroSurfaceStyle` variant or a transform of the rest style.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Requirements & success criteria (primary)
- `.planning/REQUIREMENTS.md` §"Buttons (VBTN)" — **VBTN-01..VBTN-06** (the six requirements this phase closes), lines ~50–57.
- `.planning/ROADMAP.md` §"Phase 17: Buttons" — the four explicit Success Criteria (two-tone fill/gloss/bevel/contour; hover-glow/press-invert/focus/disabled + clipped hover overlay; `Role.Button`+keyboard via `Modifier.clickable`; shared internal surface so the outlined variant can't drift).

### Foundation this phase consumes (load-bearing — read before implementing)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — the shipped primitives: `DrawScope.drawAeroSurfaceCore(style, cornerPx)`, `Modifier.aeroSurface(style, shape)`, `Modifier.aeroGlowRing(active, glowColor, cornerRadius)`, thumb/groove pair. **Ordering rule (in-file KDoc):** `Modifier.aeroGlowRing(...).aeroSurface(...)` — glow BEFORE surface, because `aeroSurface`'s outermost `.clip(shape)` would otherwise erase the outer bloom.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` — the `@Immutable AeroSurfaceStyle` data class (fillTop/fillBottom/glossColor/bevelLight/bevelShadow/rimColor/seamFraction/glossAlpha/glossHeightFraction/rimAlpha/cornerRadius/dropShadow/innerShadow) and `AeroSurfaceStyle.rest(base, cornerRadius)` factory. Note: `rest()` is the **neutral** default — D-01 requires the filled button drive its stops from the accent/`primary` color, not `rest()` verbatim.
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — `rememberAeroInteractionState(source): AeroInteractionState` (hovered/pressed/focused booleans; deliberately raw booleans, callers pick their own style variant) plus the existing `rememberHoverState`/`rememberPressedState`/`rememberFocusState` and `ANIMATION_DURATION_MS = 150`.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` + `AeroColorScheme.kt` — ornament token source and the `ornamentOverride` escape hatch; Classic tokens are **opaque** (RGB math, not alpha, for any derived stops).
- `.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md` — Phase 16 locked decisions, especially **D-01** (moderate "spirit of Aero" fidelity target: gloss ~30–35% height, soft seam, subtle bevel — buttons inherit it) and **D-02** (native-shadow-vs-gradient per cue, decided on the review).
- `.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md` — the analog map (GlassModifiers style/imports, InteractionStates wiring, `AeroListItem` hover precedent).

### Institutional memory / precedent (load-bearing)
- `.planning/STATE.md` §"Architecture positions locked for planning" and §"Baseline Findings — why these eight look Material" — the per-component current-surface audit (why the buttons read Material3) and locked positions.
- `.planning/PROJECT.md` §"Key Decisions" — `undecorated`-without-`transparent` rule, single-`drawBehind` glass perf baseline, `Icon()`-direct pattern, "spirit of Aero, modern execution" fidelity note, and the v2.0.3/v2.0.4 false-positive-sign-off lesson (three-theme review is mandatory, repro must exercise the path).

### Downstream consumer to keep in mind (do not break)
- Phase 19 `AeroSegmentedControl` (VSEL) reuses this phase's **pressed-button fill code** for its recessed selected segment (`.planning/ROADMAP.md` §"Phase 19", success criterion 3). Write the pressed-fill so it is cleanly extractable, not buried inside the button-only path.

### Current files this phase modifies
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` — currently M3 `Button` + `LocalMinimumInteractiveComponentSize` override + `drawWithContent` hover overlay (the square-corner bleed, VBTN-03) + `graphicsLayer` scale + `border` focus. All of that container/overlay machinery is replaced by the shared Aero surface.
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt` — currently M3 `OutlinedButton` + `BorderStroke` + same `drawWithContent` overlay. Replaced by the shared surface in the lighter (outlined) register.
- New: the shared internal surface composable (package/name Claude's discretion; `components/buttons/` or `components/common/`).
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — showcase demos updated to exercise all states × three themes for sign-off.

### No external specs/ADRs
- No external ADRs or standalone spec docs — requirements are fully captured in REQUIREMENTS.md (VBTN), ROADMAP.md Success Criteria, and the decisions above. No `*-SPEC.md` exists for this phase.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Phase 16 primitives** (`AeroSurfacePrimitives.kt`, `AeroSurfaceStyle.kt`) — the whole drawing layer this phase consumes; do not re-derive gradients/gloss/bevel/glow. Respect the `aeroGlowRing`-before-`aeroSurface` ordering rule.
- **`rememberAeroInteractionState(source)`** — the single collector for hovered/pressed/focused; both buttons switch to it (it deliberately returns raw booleans so each variant picks its own surface style — filled vs outlined vs the five states).
- **`AeroListItem`** — the reference hover wiring (`Modifier.hoverable(interactionSource)` + `collectIsHoveredAsState`, VLST-04); copy this interaction pattern, don't reinvent.
- **`glassPanel`'s proportional gloss** and the fixed `glassSurface` (Phase 16) — the correct proportional-gloss pattern; the buttons' gloss must be proportional (~30–35% per D-01), never a pixel literal.

### Established Patterns
- **Single shared internal surface, param-differentiated** — VBTN-06's core structure: one composable, filled vs outlined and the five states expressed by parameters/style variants, never two independent painters. Precedent: `AeroPanelGroupImpl(orientation)` (v2.0.2 Phase 13.1) — one internal core, additive param, zero drift.
- **`Color.Transparent` is the anti-pattern (PRIM-14)** — any gradient fade must go to `baseColor.copy(alpha = 0f)`, so Classic (opaque tokens) doesn't render a flat colored block.
- **Single `drawBehind` / `drawWithCache` glass** — locked perf baseline; build brushes/geometry in `drawWithCache`, no per-frame `Brush` rebuild (PRIM-13).
- **`Modifier.clickable(role = Role.Button, ...)`** replaces the M3 container while preserving semantics + keyboard (VBTN-04) — the explicit non-negotiable; do NOT hand-roll a zero-semantics clickable (the `AeroRangeSlider` zero-semantics precedent must not repeat).

### Integration Points
- `LocalAeroColors` inside `AeroTheme {}` supplies `AeroColorScheme` + `AeroOrnamentTokens`; the buttons resolve their `AeroSurfaceStyle` from it (filled = accent-driven per D-01; outlined = fainter).
- `LocalMinimumInteractiveComponentSize` override is currently used to let the 28/30.dp heights survive M3's 48.dp min-touch-target — once the M3 container is dropped this constraint disappears with it (verify the small default heights still render at 28/30.dp).
- **Phase 17 → 19 handoff:** the pressed-fill code is a shared asset for `AeroSegmentedControl`; structure accordingly.

</code_context>

<specifics>
## Specific Ideas

- Filled button = "classic Win7 blue default-action button, but real glass" — accent color survives, rendered two-tone with gloss/seam/bevel (D-01).
- Pressed = physically pushed in: inverted gradient + inner shadow, scale-shrink dropped (D-02) — chosen specifically so the recessed look transfers to Phase 19's selected segment.
- Hover = the whole button "lights up" (glow ring + brighter fill), Win7-style, not just an outer ring (D-03).
- Disabled = the glass goes "dead" (flattened gloss/bevel + muted color), not just faded (D-05).
- Outlined = a lighter sibling from the same surface — faint glass inside an Aero contour (D-06), never a separate painter.
- Fidelity target inherited from Phase 16 D-01: gloss ~30–35% height, soft (not hard) seam, subtle bevel — recognizably glass, never generic-flat/outline, never Material3-flat.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. `AeroIconButton`, `AeroColorPickerButton`, and `AeroRadioButton` are outside the eight-target milestone set and are intentionally untouched (any future Aero pass on them would be its own phase). Whether they should eventually adopt the shared surface is a possible future-milestone candidate, not a Phase 17 concern.

</deferred>

---

*Phase: 17-buttons*
*Context gathered: 2026-07-23*
