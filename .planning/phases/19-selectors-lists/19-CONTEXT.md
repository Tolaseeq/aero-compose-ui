# Phase 19: Selectors + Lists - Context

**Gathered:** 2026-07-27
**Status:** Ready for planning

<domain>
## Phase Boundary

`AeroSwitch`, `AeroSegmentedControl`, and `AeroListItem` are restyled from flat Material-looking controls into genuine Aero surfaces by consuming the Phase 16 primitives and the Phase 17/18 style resolvers. The two selectors gain hover / press / focus **for the first time** (they currently have none at all), and `AeroListItem`'s selection highlight is clipped into a proper Aero pill that no longer suppresses hover.

**In scope:**
- `AeroSwitch` — recessed track groove + raised glossy thumb with shadow, replacing the two flat `Box`es (VSEL-01); hover / press / focus wired from scratch (VSEL-02).
- `AeroSegmentedControl` — selected segment **recessed** (inverted gradient + inner shadow) by reusing Phase 17's `AeroSurfaceStyle.pressedRecess(...)` (VSEL-03); hover and focus for the first time (VSEL-04); real selection semantics replacing the bare `.clickable`.
- `AeroListItem` — selection highlight clipped to a rounded pill with gradient + rim light (VLST-01); hover remains visible on an already-selected row (VLST-02); a focus visual (VLST-03).
- All newly-hover-wired components reuse the `Modifier.hoverable` + `collectIsHoveredAsState` pattern already correct in `AeroListItem` — never pointer-position tracking (VLST-04).

**Explicitly NOT in this phase:**
- Any change to public API **signatures beyond additive trailing params**, default sizes, or behavior beyond the new visual (milestone constraint — API/behavior stay 1:1). The `interactionSource` additions in D-04 are additive and source-compatible; nothing else changes.
- Restyling the other five target components (Phase 17 buttons, Phase 18 range) or building new primitives (Phase 16).
- The showcase's formal three-theme sign-off and grep-gates — that is Phase 20. Per-component demos still land here.
- `AeroListItem`'s bottom mirror reflection — already ruled out of scope in `.planning/STATE.md`.
- Arrow-key selection inside `AeroSegmentedControl` (see D-06 — Tab-per-segment was chosen instead, and arrow-key roving focus would be a behavior change, not a visual one).

</domain>

<decisions>
## Implementation Decisions

### AeroSwitch identity
- **D-01:** The **accent color lives in the track groove**; the thumb stays a **neutral raised glass nub**. Directly continues Phase 18 D-01's slider identity (accent = "how much / whether it is on", neutral thumb = "the thing you move"), so switch, slider, progress fill, and the filled button all read as one accent family. — **Reversibility:** costly — the accent-vs-neutral split determines which resolver each sub-surface reads and would need re-reviewing on three themes to swap.
- **D-02:** The **off state is the same neutral recessed groove** — identical groove geometry in both states, only the fill color changes. Keeps the existing single `animateColorAsState` transition and gives the animation exactly one moving part.
- **D-03:** The raised thumb is drawn **on top of, and outside, the track's clip** — its drop shadow and glow ring are not clipped by the 18dp-tall track. The visual bounding box may extend slightly beyond 36×18dp; the **layout size stays exactly 36×18dp** (milestone constraint). This is the same clip-ordering problem Phase 18 D-03 solved for the slider thumb — respect the `aeroGlowRing`-before-`aeroSurface` rule documented in `AeroSurfacePrimitives.kt`. — **Reversibility:** reversible — a layering change local to the switch.

### Public API
- **D-04:** `AeroSwitch` and `AeroSegmentedControl` each gain a **trailing `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` parameter**, matching the shape `AeroListItem` already exposes. Additive and source-compatible — existing call sites are untouched — and it makes the interaction-source convention uniform across the library instead of leaving `AeroListItem` as the lone exception. — **Reversibility:** one-way — once published, removing a public parameter is a breaking change to consumers (`aska`, `satellite-control`) and needs a deprecation cycle.

### AeroSwitch states
- **D-05:** **Press keeps the thumb raised** with a brighter gloss (Phase 18 D-04's `PRESSED_GLOSS_BOOST` idiom), **not** the button's `pressedRecess` (Phase 17 D-02). A thumb that rides in a groove should read as "picked up", and the groove is already recessed — recessing it further on an 18dp control would not read at all. `resolveSliderThumbStyle` in `AeroSlider.kt` already implements exactly this precedence chain and is the model to follow. — **Reversibility:** reversible — a per-state style swap.
- **D-06:** **Hover puts the `aeroGlowRing` on the whole track** (the hot zone, where `toggleable` lives) **plus `hoverLighten` on the thumb** — the whole control lights up, per Phase 17 D-03, and the lit area matches where the cursor actually is. Focus stays the persistent `aeroGlowRing` from Phase 17 D-04 (already locked; the existing `Modifier.toggleable(role = Role.Switch)` supplies focusability and Space/Enter for free).

### Animation policy (all three components)
- **D-07:** **Semantic transitions keep their 150ms tween; interaction states switch instantly.** `checked` / `selected` continue to animate at `tween(150, LinearEasing)` — that is current observable behavior and the milestone forbids changing it. `hover` / `press` / `focus` resolve to a style with no animation, exactly as `resolveButtonStyle` and `resolveSliderThumbStyle` already do. This closes the gap between "phase 19 components animate their background" and "phases 17–18 components snap" without regressing either. Do **not** try to animate a whole `AeroSurfaceStyle` — that would fight the `drawWithCache` perf baseline (PRIM-13). — **Reversibility:** costly — the animate-vs-snap split is applied consistently across all three components and matches two shipped phases; changing it re-touches everything.

### AeroSegmentedControl
- **D-08:** **Unselected segments are raised glass** — the strip reads as a row of buttons with one pushed in (the literal Win7 toolbar idiom). Unselected segments resolve the same rest style as `AeroButton`; the selected one is `pressedRecess` applied on top of that same base, so the raised→recessed contrast is maximal and the Phase 17 code is reused verbatim rather than re-authored. — **Reversibility:** costly — this is the cross-phase reuse promise recorded in `AeroButtonSurface.kt`'s KDoc; switching unselected segments to flat would fork the two paths.
- **D-09:** **Each segment is individually focusable via `Modifier.selectable(selected, role = Role.RadioButton, ...)`** — Tab reaches every segment, Space/Enter activates, and correct semantics come for free from the platform. This replaces today's bare `.clickable` with no role and no semantics. Deliberate: the milestone already recorded "the `AeroRangeSlider` zero-semantics precedent must not repeat" (VBTN-04), and the alternative (one focus stop + hand-rolled arrow-key roving) would be a behavior change rather than a visual one. Accepted trade-off: N Tab stops for an N-segment control.
- **D-10:** **Segment hover and focus render entirely inside the segment's own bounds** — hover is `hoverLighten` on the fill, focus is an inner rim / inner glow. No `aeroGlowRing` on individual segments: the segments sit flush inside a `Row` with `.clip(shape)`, so an outer bloom would be sliced by the frame *and* bleed onto its neighbours. This is the segmented-control-specific exception to the library's outer-glow language, and the reason must be recorded in the code so a later reviewer does not "fix" it back. — **Reversibility:** reversible — but re-check the clip interaction before changing it.

### AeroListItem
- **D-11:** **Hover and selection compose as transforms, never as `when` branches.** The base style resolves from `selected`, then `hoverLighten()` is applied on top of whichever base was chosen — the same composition shape `resolveButtonStyle` uses. VLST-02's "selection suppresses hover" bug is then structurally impossible rather than fixed by color-picking: there is no code path where selection can swallow hover. The current `when { selected -> …; hovered -> …; else -> … }` in `AeroListItem.kt:60-64` is exactly the anti-pattern being removed. — **Reversibility:** costly — the composition shape is the VLST-02 guarantee; reverting to branch selection reintroduces the bug class.
- **D-12:** **The hover highlight uses the same pill geometry as the selection highlight** — one shape, two style states, so hovering an unselected row and hovering a selected row are visually coherent. The full-bleed unclipped `.background(animatedBg)` is gone (VLST-01).
- **D-13:** **`AeroListItem`'s focus visual renders within the row's own bounds** (inner rim / brightened pill contour), not an outer `aeroGlowRing` bloom — list rows live inside clipping, scrolling containers where an outer bloom would be sliced exactly as it would be in the segmented control (D-10). The row is focusable only when `onClick != null` (that is what `Modifier.clickable` already provides); a display-only row gains no focus stop.

### Claude's Discretion

The user's explicit instruction during discussion: *"Я абсолютно не понимаю большинства подобных вопросов без визуальных примеров. Либо как-то приводи визуальный пример, либо делай как знаешь и будем править на фазе выполнения."* Everything below is therefore **settled by Claude on the three-theme review and corrected during execution / Phase 20 sign-off** — do not block on asking the user to imagine a gradient.

- **Selection-pill geometry** — horizontal/vertical inset from the 36dp row and the corner radius. Both the four-side-inset "floating pill" (Win7 Explorer idiom) and the vertical-inset full-width pill are acceptable; pick what reads correctly in the showcase on all three themes.
- **The recessed segment's color** — accent-recessed glass (literally a pressed `AeroButton`, double signal of depth + color, needs light text) vs. neutral-recessed with the accent left in the text. Same `pressedRecess` transform either way; the choice is which base it composes onto. Verify text legibility on all three themes whichever way it goes.
- Native `Modifier.dropShadow` / `innerShadow` vs. manual gradient for each depth cue — per Phase 16 D-02, decided on the review; signatures in `ScratchAeroShadowProof.kt`.
- Exact groove depth, thumb gloss magnitude, and how much the thumb's shadow is allowed to exceed the 36×18dp box (D-03).
- Exact hover-vs-focus differentiation on every component so both are distinguishable when co-active (mirrors Phase 17 D-03/D-04 discretion). For the segmented control and list item this must work with in-bounds cues only (D-10, D-13).
- Whether the raised/recessed segment styles are new resolver functions or reuse `resolveButtonStyle` with a parameter — the constraint is one shared painter, not two forks.
- Disabled for all three components follows the already-locked "flatten to dead" rule (Phase 17 D-05 / Phase 18 D-07) via `flattenDisabled`, replacing today's `.alpha(0.4f)`; the exact flattened values are discretion.
- The segment separators (currently a 1dp `borderDefault@0.5f` `Box`) — keep, convert to a bevel seam, or drop once segments have their own raised contour.

**Before planning, consider `/gsd-sketch`** to mock up the pill geometry and the recessed-segment color as throwaway HTML — that would convert the first two discretion items into real user decisions instead of review-time corrections.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Requirements & success criteria (primary)
- `.planning/REQUIREMENTS.md` lines 73–80 — **VSEL-01..04** and **VLST-01..04**, the eight requirements this phase closes. Written in Russian.
- `.planning/ROADMAP.md` §"Phase 19: Selectors + Lists" (lines 222–238) — the four explicit Success Criteria.

### Foundation this phase consumes (load-bearing — read before implementing)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — `drawAeroSurfaceCore`, `Modifier.aeroSurface`, `Modifier.aeroGlowRing` + `drawAeroGlowRing`, and the **raised-thumb / recessed-groove pair** (`drawAeroThumb`/`Modifier.aeroThumbSurface` PRIM-07, `drawAeroGroove`/`Modifier.aeroGroove` PRIM-08 — the direct consumers for `AeroSwitch`). **Ordering rule (in-file KDoc):** `aeroGlowRing` BEFORE `aeroSurface` — `aeroSurface`'s outer `.clip(shape)` erases any bloom drawn after it. This rule is the crux of D-03, D-10, and D-13.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` — the `@Immutable AeroSurfaceStyle` plus `rest(base, cornerRadius)` (accent-capable) and `neutralRest(base, cornerRadius)` factories, and the `pressedRecess(innerShadow)` / `hoverLighten()` / `flattenDisabled(base)` transforms this phase composes.
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` — **the Phase 17 → 19 handoff.** Its KDoc explicitly names `AeroSegmentedControl` as the cross-phase reuser of `pressedRecess`. `resolveButtonStyle` (line 244) is the precedence-chain model D-05/D-11 mirror; `PRESSED_INNER_SHADOW` (line 122) is the native-`Shadow` recess value; `outlinedStyle()` (line 195) is the "fixed delta on top of an already-resolved style" pattern D-08 should follow rather than authoring a second painter.
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` lines 228–275 — `resolveSliderThumbStyle` (the exact "press keeps it raised, gloss boost instead of recess" implementation D-05 adopts) and `resolveSliderTrackStyle`.
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — `rememberAeroInteractionState(source)` (raw hovered/pressed/focused booleans) and `ANIMATION_DURATION_MS = 150`.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` + `AeroColorScheme.kt` — ornament tokens and the `ornamentOverride` hatch; Classic tokens are **opaque**, so derived stops use RGB lighten/darken (`ColorMath.kt`), never alpha.

### Sibling phases whose decisions this phase mirrors (load-bearing for consistency)
- `.planning/phases/17-buttons/17-CONTEXT.md` — D-01 (accent filled identity), D-02 (`pressedRecess` — reused by D-08, deliberately NOT applied to the switch per D-05), D-03/D-04 (hover glow + brighten / persistent focus glow → D-06), D-05 (disabled flatten-to-dead), D-06 (shared painter, param-differentiated).
- `.planning/phases/18-range/18-CONTEXT.md` — D-01 (accent-in-track / neutral-thumb identity → D-01 here), D-03 (glow bloom vs. slot clipping → D-03 here), D-04 (dragged thumb stays raised → D-05 here), D-07 (disabled flatten).
- `.planning/phases/17-buttons/17-UI-SPEC.md` and `.planning/phases/18-range/18-UI-SPEC.md` — the per-state contract tables those phases were built against; this phase's UI-SPEC should follow the same shape.
- `.planning/phases/16-foundation-aero-primitives-layer/16-CONTEXT.md` — D-01 (fidelity band: gloss ~30–35% of height, soft seam, subtle bevel) and D-02 (native shadow vs. gradient, decided on review).
- `.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md` — the analog map, including the `AeroListItem` hover precedent VLST-04 points at.

### Institutional memory / precedent (load-bearing)
- `.planning/STATE.md` §"Baseline Findings" lines 56–76 — the per-component audit of exactly why these three read Material (switch = two plain `Box`es, no states at all; segmented = `Row` + border, flat `primary@0.3f` selected, no hover/focus; list item = unclipped `.background`, hard-edged full-bleed highlight, no focus visual) — and §"Architecture positions locked for planning" (line 92: segmented selected is recessed and never raised; list-item mirror reflection out of scope).
- `.planning/PROJECT.md` §"Key Decisions" — single-`drawBehind`/`drawWithCache` glass perf baseline (PRIM-13, load-bearing for D-07); `Color.Transparent` is the anti-pattern (PRIM-14 — fade to `baseColor.copy(alpha = 0f)`, critical for Classic's opaque tokens and for the pill's gradient edges); three-theme review mandatory and repro-must-exercise-the-path (the v2.0.3/v2.0.4 false-positive-sign-off lesson).

### Current files this phase modifies
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` (77 lines) — two flat `Box`es, `animateColorAsState` track + `animateFloatAsState` thumb at 150ms, `.alpha(0.4f)` disabled, `toggleable(role = Role.Switch)`. Gains groove + raised thumb, all three states, `interactionSource` param. Keep the 150ms `checked` animation (D-07) and the 36×18 / 14dp sizes (D-03).
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` (96 lines) — `Row` + `border` + `.clip`, per-segment `animateColorAsState`, bare `.clickable(enabled)` with no role, 1dp separator `Box`es. Gains raised/recessed segments, `selectable(role = Role.RadioButton)`, in-bounds hover/focus, `interactionSource` param.
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` (105 lines) — the `when { selected -> … }` at lines 60–64 is the VLST-02 bug; `.background(animatedBg)` with no clip is the VLST-01 bug. Already has the correct `hoverable` + `collectIsHoveredAsState` wiring and a public `interactionSource` — that part is the pattern the other two copy (VLST-04).
- `showcase/src/main/kotlin/com/mordred/showcase/sections/…` — the sections demoing selectors and lists, updated to exercise every state × three themes. Phase 20 consolidates the formal sign-off.

### No external specs/ADRs
- No external ADRs or standalone spec docs exist. No `*-SPEC.md` for this phase — requirements live in REQUIREMENTS.md (VSEL/VLST), ROADMAP.md Success Criteria, the mirrored Phase 16/17/18 CONTEXT decisions, and the decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AeroSurfaceStyle.pressedRecess(PRESSED_INNER_SHADOW)`** — already written and shipped for the pressed button, and its KDoc already names this phase as the consumer. `AeroSegmentedControl`'s selected segment must call it, not re-derive an inverted gradient.
- **`resolveSliderThumbStyle` (`AeroSlider.kt:244`)** — the "disabled → pressed-stays-raised → hover → rest" chain D-05 wants for the switch thumb, already proven on the sliders. Reuse or mirror; do not author a third precedence chain.
- **`drawAeroGroove` / `Modifier.aeroGroove` (PRIM-08)** and **`drawAeroThumb` / `Modifier.aeroThumbSurface` (PRIM-07)** — the switch's track and thumb come straight from here.
- **`hoverLighten()` / `flattenDisabled(colors)`** — the shared hover and disabled transforms; all three components compose them rather than picking colors by hand.
- **`AeroListItem`'s existing `hoverable` + `collectIsHoveredAsState` + public `interactionSource`** — the reference wiring VLST-04 mandates, and the shape D-04 copies onto the two selectors.
- **`resolveButtonStyle`'s structure** (theme-aware rim alpha via `minOf(nativeRimAlpha, cap)`, terminal `flattenDisabled`, transform composition) — the model for every resolver added in this phase.

### Established Patterns
- **One shared painter, param-differentiated** — `AeroButtonSurface(outlined = …)` and `AeroPanelGroupImpl(orientation)`. The segmented control's raised/recessed segments are two states of one painter, never two painters.
- **Transform composition over `when`-branch selection** — the structural answer to VLST-02 (D-11).
- **`Color.Transparent` is the anti-pattern (PRIM-14)** — every gradient fade (pill edges, groove, rim) goes to `baseColor.copy(alpha = 0f)`; on Classic's opaque tokens `Color.Transparent` renders a flat block. Note `AeroSegmentedControl.kt:59` currently uses `Color.Transparent` directly and `AeroListItem.kt:63` does too — both are removed by this phase.
- **`drawWithCache` for brushes/geometry (PRIM-13)** — no per-frame `Brush` rebuild; the reason D-07 refuses to animate whole styles.
- **Clip order is load-bearing** — `aeroGlowRing` before `aeroSurface`; and any outer bloom inside a clipped parent (segment row, scrolling list) gets sliced. D-03, D-10, D-13 are three consequences of the same rule.

### Integration Points
- `LocalAeroColors` inside `AeroTheme {}` supplies `AeroColorScheme` + `AeroOrnamentTokens`; each component resolves its `AeroSurfaceStyle` from it — accent for the switch groove (D-01), button-rest for segments (D-08).
- `Modifier.toggleable(role = Role.Switch)` on `AeroSwitch` already carries focusability and Space/Enter — the focus visual is the only missing half. `Modifier.selectable(role = Role.RadioButton)` gives the segments the same for free (D-09).
- `AeroListItem`'s `interactionSource` is public and pass-through; parents already share hover state with it. Adding pill rendering must not change that contract.

</code_context>

<specifics>
## Specific Ideas

- The switch is the slider's sibling: colored groove, neutral raised knob riding in it (D-01) — one accent family across switch, slider, progress, and filled button.
- Pressing the switch feels like grabbing the knob, not pushing a button — it stays raised and its gloss brightens (D-05).
- The segmented control is a Win7 toolbar strip: raised glass buttons with exactly one visibly pushed in (D-08).
- Because the segment strip is clipped, its hover and focus glow *inward* rather than blooming outward (D-10) — a deliberate, documented exception to the library's glow language, not an oversight.
- A selected list row that you hover gets brighter, never flatter — the two states add up (D-11), which is what the current code gets wrong.
- Semantic changes glide at 150ms; interaction states are instant (D-07) — the same feel as the already-shipped buttons and sliders.

</specifics>

<deferred>
## Deferred Ideas

- **Arrow-key roving focus for `AeroSegmentedControl`** (one Tab stop, ←/→ change selection — the standard radiogroup/toolbar pattern). Considered and rejected for this phase in D-09: it is a behavior change requiring hand-rolled `onKeyEvent` plus a custom semantics group, and this milestone is visual-only. A genuine keyboard-ergonomics improvement worth its own future phase alongside the deferred `AeroRangeSlider` semantics work.
- **`AeroRangeSlider` accessibility semantics + keyboard support** — carried forward unresolved from Phase 18's deferred list. Still open, still needs its own phase.
- **`AeroListItem` bottom mirror reflection** — ruled out of scope at milestone level (`.planning/STATE.md` line 92). Recorded here so it is not rediscovered as a gap.
- **`AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton`** — outside the eight-target set, still untouched. Whether they eventually adopt the shared Aero surface is a future-milestone question (carried from Phase 17's deferred list).

</deferred>

---

*Phase: 19-selectors-lists*
*Context gathered: 2026-07-27*
