# Architecture Research — v3.0 Glass Refinement

**Domain:** Compose Desktop UI component library — Aero visual layer architecture at Kotlin 2.4.10 / Compose Multiplatform 1.11.1
**Researched:** 2026-07-21
**Confidence:** HIGH for the M3 `Slider` thumb/track slot finding (Context7, official androidx reference, cross-checked against two independent doc entries — this is a long-stable M3 API shape, not new). HIGH for modifier-ordering/clip conventions (derived directly from this repo's own confirmed bugs in `GlassModifiers.kt` + PITFALLS.md's Pitfall 6 analysis). MEDIUM for exact `dropShadow`/`innerShadow` behavior under real load (UPGRADE.md already flags MEDIUM-HIGH, unresolved until a Foundation-phase jar-level spike). MEDIUM for the `AeroOrnamentTokens` derivation design (original synthesis, not sourced from a precedent) — flagged as a locked-at-Foundation-phase decision to validate against all three themes immediately, not deferred.

This document builds directly on `.planning/research/FEATURES.md` (device catalog, per-component treatment, dependency graph) and `.planning/research/PITFALLS.md` (14 named pitfalls). It does not re-derive those — it answers the specific structural question the milestone needs before Phase 16 (Foundation) can be planned: **what Kotlin types and modifier chains implement the device catalog, and in what order do the 8 components consume them.**

---

## Q1 — Shape of the Primitive API

**Recommendation: (d) a mix, but a *narrow*, deliberate one — one core `DrawScope` function + one data-driven style object + one `Modifier` wrapper that is nothing but that function called from `drawWithCache`. Not three independent implementations; one implementation exposed three ways.**

### The core primitive

```kotlin
// library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt  (NEW)
@Immutable
public data class AeroSurfaceStyle(
    val cornerRadius: Dp,
    val fillTop: Color,           // two-tone gradient, A1
    val fillBottom: Color,
    val seam: Boolean = true,     // A2
    val glossAlpha: Float = 0f,   // A3, 0 = no gloss (grooves get none, per A10)
    val bevelLight: Color,        // A4
    val bevelShadow: Color,
    val rimColor: Color,          // A5
    val rimAlpha: Float,
    val dropShadow: Shadow? = null,   // A9 — androidx.compose.ui.graphics.shadow.Shadow, CMP 1.9.0+
    val innerShadow: Shadow? = null,  // A10 groove wall / A7 pressed inset
)
```

```kotlin
// library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt  (NEW — separate file
// from GlassModifiers.kt so the legacy-modifier bug-fix diff stays reviewable on its own)

internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float) {
    val cr = CornerRadius(cornerPx, cornerPx)
    // 1. two-tone fill (A1) — Brush built here, NOT allocated per-frame (see Q2)
    // 2. seam (A2)
    // 3. gloss oval, clipped to own shape (A3) — only if style.glossAlpha > 0
    // 4. bevel rim, single inset stroke w/ vertical gradient (A4, LOW-complexity fallback per FEATURES.md)
    // 5. outer contour stroke, INSET by half stroke width so it survives clip (A5 — the confirmed bug fix)
}

public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier = this
    .let { if (style.dropShadow != null) it.dropShadow(shape, style.dropShadow) else it }
    .clip(shape)
    .drawWithCache {
        val cornerPx = style.cornerRadius.toPx()
        onDrawBehind { drawAeroSurfaceCore(style, cornerPx) }
    }
    .let { if (style.innerShadow != null) it.innerShadow(shape, style.innerShadow) else it }
```

`Modifier.aeroSurface(style, shape)` is the primary API for every component that **owns a `Box`**: `AeroSwitch` track, `AeroSwitch`/`AeroSlider` thumb wrapper, `AeroListItem` selection pill, `AeroSegmentedControl` per-segment background, `AeroProgressBar` groove + fill, `AeroButton`/`AeroOutlinedButton` container (once M3's `Button`/`OutlinedButton` container is dropped — see Q5).

`internal fun DrawScope.drawAeroSurfaceCore(...)` is the **exception seam** named in the question: components that already own a `Canvas` (`AeroRangeSlider`, and `AeroSlider`'s custom `track =` slot if it draws via `Canvas`) call this function **directly inside their existing `DrawScope`**, bypassing the `Modifier` wrapper entirely — no extra `Box`/layer, no extra `clip()`, same exact rendering code as every `Modifier.aeroSurface()` call site. This is the answer to "some own a Canvas, some are Boxes": **one function, two call conventions, zero duplicated drawing logic.**

Two small sibling primitives, each with a *different* composition rule than `aeroSurface`, get their own thin wrapper rather than being folded into the general style object:

- **`Modifier.aeroGlowRing(color: Color, shape: Shape, intensity: Float): Modifier`** (A6/A8) — must draw **outside** the shape's own clip (FEATURES.md A6/A8: "drawn just outside contour, not clipped"). It is therefore applied **before** `.aeroSurface()`'s own `.clip(shape)` in a component's chain, never merged into `AeroSurfaceStyle` (which is deliberately clip-scoped).
- **`internal fun DrawScope.drawAeroThumb(style: AeroSurfaceStyle, radiusPx: Float)`** (A12) — a circle-shape special case of the same core (`CornerRadius` becomes `radiusPx` both axes); exposed as `Modifier.aeroThumbSurface(style)` for `Box`-based thumbs (`AeroSwitch`) and as the raw function for Canvas-based thumbs (`AeroRangeSlider`, `AeroSlider`'s `thumb =` slot if Canvas-drawn).

### Why not (a) or (b) alone

- **Pure Modifier-extension family (a)** — the *current* `GlassModifiers.kt` shape — breaks down the moment a component already owns a `Canvas` (`AeroRangeSlider`): wrapping Canvas content in an extra `Modifier`-decorated `Box` just to reuse a gradient would add a layer and contradict the single-drawBehind performance baseline for no benefit.
- **Pure data object + one mega-modifier (b) with no `DrawScope` escape hatch** — forces every Canvas-owning consumer to either duplicate the drawing math or restructure around an extra Box, which is exactly the "shared visual coincidence, not shared code" drift this project's own memory (`project_panelgroup_composable_dsl_pitfall`) warns about.
- **Pure per-component `DrawScope` helpers (c) with no shared style type** — reintroduces the parameter-explosion problem PITFALLS.md's Pitfall 7 already diagnoses (bare positional Dp/Color/Boolean arguments are how a dead `elevation` parameter went unnoticed for two milestones). A named `AeroSurfaceStyle` with presets (`AeroSurfaceStyle.raised(...)`, `.pressed(...)`, `.groove(...)`) makes every call site self-documenting and every parameter's consumer traceable by construction.

### How `dropShadow`/`innerShadow` force the layering decision

They are `Modifier`s, not `DrawScope` calls (UPGRADE.md Part 4, confirmed signature: `fun Modifier.dropShadow(shape, shadow)`, `fun Modifier.innerShadow(shape, shadow)`). This **does** force a layering decision, and it is the one embedded in `aeroSurface()` above: `dropShadow` must sit **outside** `.clip(shape)` (a shadow clipped to its own shape would just disappear), and `innerShadow` must sit **inside**/**after** the fill (it draws recessed, on top). Because both are real `Modifier` chain nodes (not draw calls inside `drawWithCache`), they **cannot** be folded into the single cached `onDrawBehind` block — they are two additional, unavoidable modifier-chain nodes per surface that has them. `aeroSurface()` centralizes this ordering in one place so no per-component call site has to re-derive it (directly forecloses PITFALLS.md Pitfall 6's failure mode by construction, not by convention alone).

**Exceptions to `aeroSurface()`:** `AeroRangeSlider`'s track segments (draws two `drawLine`-style groove/fill strips directly in its existing `Canvas`, calling `drawAeroSurfaceCore`-adjacent line-drawing helpers, not the rounded-rect version), and any M3-slot-based component (see Q5) where M3 itself is drawing the outer container and only the slot's own content needs `aeroSurface`/`aeroThumbSurface` at a smaller scope.

---

## Q2 — Draw-Pass Economy

**Convention locked: exactly one `drawWithCache { onDrawBehind { ... } }` block per surface carries fill + two-tone gradient + seam + gloss + bevel + rim. `dropShadow`/`innerShadow`, when used, are two *additional*, unavoidable Modifier-chain nodes — not part of the cached block, and not optional overhead to try to fold in.**

### `drawBehind` vs `drawWithCache` vs `drawWithContent`

- **`drawWithCache` is the correct default for the fill/gradient/gloss/bevel/rim block**, replacing the current `drawBehind`-only pattern in `glassPanel`/`glassSurface` (PITFALLS.md Pitfall 1 — confirmed existing bug: both currently allocate `Brush.verticalGradient(...)` fresh on every draw). `drawWithCache`'s split is exactly what this milestone needs: everything that depends only on `size`/`cornerRadius`/`style` identity (the `Brush` objects, the `CornerRadius`, any `Path`) is built once outside `onDrawBehind {}` and re-built only when those inputs change; only cheap draw calls run inside `onDrawBehind {}` every frame. **Colors that animate (hover brighten, pressed invert) must still be read inside `onDrawBehind {}`** — that's correct and required, not a cache-defeating mistake, as long as it's a `State<Color>` *read*, not a `Brush` *rebuild*. Concretely: build a gradient shaped by fixed stop *positions* once; if the *colors* at those stops change with state, accept a small `Brush.verticalGradient(listOf(currentFillTop, currentFillBottom))` rebuild inside `onDrawBehind` (cheap — two `Color` values, no `Path`) rather than trying to force color-only updates through the cache (Compose's `drawWithCache` invalidates on *any* captured value used inside the cache-block change, so if `style` itself changes because a `Color` changed, the whole cache block still needs to be sensitive to that — the practical rule is: **never rebuild a `Path` or a >2-stop `Brush` with size-computed positions inside `onDrawBehind`; a 2-stop color-only `Brush` rebuild there is acceptable**).
- **`drawBehind` remains correct only for genuinely static, size-independent draws** — none of the new devices qualify; `drawBehind` should be treated as deprecated-in-spirit for anything new in this milestone, not just for the two existing buggy call sites.
- **`drawWithContent`** is reserved for the one case it's already used for correctly and dangerously — `AeroButton.kt:85-90`'s hover overlay — which is also the confirmed corner-square bug (Pitfall 6). The fix is not "stop using `drawWithContent`," it's "move the overlay draw *inside* the same `aeroSurface()`-owned, already-clipped chain" (i.e., bake the hover-brighten into `AeroSurfaceStyle`'s fill colors, selected by the caller *before* calling `aeroSurface()`, rather than drawing a second unclipped overlay on top of M3's `Button` content).

### Where clipping sits — the confirmed-bug convention

`glassSurface` (`GlassModifiers.kt:87-105`) currently does `.drawBehind { ...stroke... }.clip(shape)` — draw-then-clip, so the bounds-centred 1.dp stroke's outer half is clipped away (confirmed, PITFALLS.md Pitfall 6 item 1). **The locked convention going forward, enforced by `aeroSurface()`'s own chain order in Q1:**

```
[optional dropShadow]  →  .clip(shape)  →  [cached fill+gloss+bevel+rim block]  →  [optional innerShadow]
```

`.clip(shape)` is the **outermost modifier that affects the surface's own paint** — everything meant to respect rounding is written *after* it in the chain (Pitfall 6's exact rule, now embedded in one shared function instead of repeated per component). The rim/contour stroke itself is additionally **inset by half its stroke width** inside `drawAeroSurfaceCore` (belt-and-suspenders — correct clip order alone is sufficient, but an inset stroke is also correct regardless of clip order and costs nothing extra).

### Pass count per component instance

| State | Passes | Notes |
|---|---|---|
| Rest, no shadow style | 1 (`onDrawBehind` block) | e.g. `AeroListItem` at rest — fully transparent, cheapest path is `Modifier` unchanged (no `aeroSurface` call at all when `!selected && !hovered`) |
| Rest, with drop+inner shadow | 3 (dropShadow node + fill block + innerShadow node) | e.g. `AeroButton` rest state |
| Hover/focus active | +1 (`aeroGlowRing`, gated) | Only composed into the chain while `hovered \|\| focused` is true — `Modifier.then(if (glow) Modifier.aeroGlowRing(...) else Modifier)` costs literally zero when inactive, directly satisfying PITFALLS.md Pitfall 3's "shadow/glow scoped to interactive-state-only" guidance |

`AeroListItem` specifically must **never** carry `dropShadow` (PITFALLS.md Pitfall 3 names it explicitly as the one component that can appear in numbers in a `LazyColumn`) — its `AeroSurfaceStyle` preset for the selection pill sets `dropShadow = null` unconditionally; only the rim + low-alpha fill + optional gloss line render.

---

## Q3 — Theme Tokens

**Recommendation: do NOT add ~10 new flat literal fields × 3 themes to `AeroColorScheme`. Add ONE new nested, algorithmically-derived type, plus ONE new trailing nullable override field on `AeroColorScheme` for the additive, source-compatible escape hatch.**

### Why not flat properties, why not hand-authored-per-theme

`AeroColorScheme` (`library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt:13-37`) is a `public data class` with **23 mandatory, no-default `Color` parameters**, constructed directly (not just via `.copy()`) by any consumer building a custom theme. Hand-authoring ~10 new ornament tokens (gloss, bevel light/dark, rim, shadow, hover glow, groove fill, gradient split stops) × 3 built-in themes repeats the exact risk FEATURES.md's Cross-Theme Finding and PITFALLS.md's Pitfall 11 both flag: a human tunes new literals against `AeroBlue` (the "most Aero" theme, most likely to be open during development) and `Classic`'s fully-opaque tokens silently render wrong — not merely less pretty, categorically different (a translucent sheen renders as a flat opaque patch). Hand-authoring also multiplies the "which of these ~30 new literals did we forget to update for `Classic`" surface to audit.

### The design

```kotlin
// library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt  (NEW)
internal fun Color.lighten(amount: Float): Color = /* RGB mix toward Color.White, NOT alpha */
internal fun Color.darken(amount: Float): Color = /* RGB mix toward Color.Black */
```

```kotlin
// library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt  (NEW)
@Immutable
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
        public fun derive(base: AeroColorScheme): AeroOrnamentTokens {
            val opaque = base.glassSurface.alpha >= 0.99f  // Classic vs AeroBlue/AeroDark branch
            return AeroOrnamentTokens(
                glossHighlight = base.glassHighlight.lighten(0.15f),
                bevelLight = base.primary.lighten(0.25f),
                bevelShadow = base.primary.darken(0.20f),
                rimLight = base.glassBorder.lighten(0.10f),
                hoverGlow = base.primary.lighten(0.30f),
                grooveShadow = base.surface.darken(0.15f),
                fillSplitTop = base.primary.lighten(0.18f),
                fillSplitBottom = base.primary.darken(0.12f),
                // `opaque` branch: gradients fade toward each color's own alpha=0 copy,
                // never a hardcoded Color.Transparent, so Classic's opaque tokens degrade to
                // "less of itself" rather than assuming translucency existed to begin with
                // (Pitfall 11's exact prescribed fix — implemented once, here, not per-primitive).
            )
        }
    }
}
```

```kotlin
// AeroColorScheme.kt — MODIFIED, one new trailing field, default null:
@Immutable
public data class AeroColorScheme(
    public val primary: Color,
    // ...all 23 existing fields, UNCHANGED, in the SAME order...
    public val panelBackground: Color,
    public val ornamentOverride: AeroOrnamentTokens? = null,  // NEW — trailing, defaulted
)
```

```kotlin
// AeroTheme.kt — MODIFIED, new computed accessor alongside the existing `colors`/`typography`:
public object AeroTheme {
    public val colors: AeroColorScheme
        @Composable @ReadOnlyComposable get() = LocalAeroColors.current
    public val ornaments: AeroOrnamentTokens
        @Composable @ReadOnlyComposable get() =
            LocalAeroColors.current.ornamentOverride ?: AeroOrnamentTokens.derive(LocalAeroColors.current)
    // ...
}
```

### Source-compatibility, addressed explicitly

`AeroColorScheme` is public and constructed **positionally and by name** at existing call sites (`AeroColorScheme.AeroBlue`, `.AeroDark`, `.Classic` inside the same file; any consumer's custom theme via the full 23-arg constructor). Kotlin requires defaulted parameters to be trailing (or for every subsequent parameter to also have a default) for the no-named-args call form to keep compiling. **The new field is appended last, with a default of `null`.** This means:
- Existing **positional** constructor calls (all 23 args, in order) compile unchanged — a 24th trailing defaulted parameter is invisible to them.
- Existing **named** constructor calls compile unchanged for the same reason.
- Existing **`.copy()`** calls are unaffected regardless of field position — `.copy()` always was source-compatible for additive fields.
- **Not source-compatible in exactly one narrow case:** a consumer using purely-positional `AeroColorScheme(...)` construction while *also* supplying a 24th trailing positional argument that happens to be a `Color` (impossible, since the new field's type is `AeroOrnamentTokens?`, not `Color` — a `Color` argument in that position would be a compile error before and after this change). No real-world call site is broken by this addition.

Built-in `AeroBlue`/`AeroDark`/`Classic` presets leave `ornamentOverride` at its default (`null`), so all three get the derived tokens — validated against all three themes as a **Foundation-phase spike deliverable** (per PITFALLS.md Pitfall 11: "verify against all three themes independently... not deferred to the final sign-off"), not assumed correct from the formula alone. If the derived Classic result reads wrong once actually rendered, the fix is to adjust `derive()`'s `opaque` branch (one function, one place), not to hand-patch three sets of literals.

---

## Q4 — State-Driven Visuals

**Recommendation: `MutableInteractionSource` + `collectIsHoveredAsState`/`collectIsPressedAsState`/`collectIsFocusedAsState`, wired through `Modifier.hoverable`/`focusable`/`clickable` (Foundation-level, not raw `pointerInput`) — the exact pattern already proven correct in `AeroListItem.kt:57,74` and already implemented (but under-reused) in `components/buttons/InteractionStates.kt`. A shared helper is warranted; relocate the existing helper file rather than duplicate it.**

### The existing helper is already right, just mis-scoped

`library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt` already provides `rememberHoverState`/`rememberPressedState`/`rememberFocusState` (thin wrappers over `collectIsHoveredAsState`/etc.) plus `ANIMATION_DURATION_MS = 150`. All are `internal` — and **Kotlin's `internal` is module-scoped, not package-scoped** (already established precedent in this codebase: STATE.md's Phase 7 note "Kotlin internal is module-scoped, so the showcase imports a thin public wrapper"). This means `AeroSwitch.kt`, `AeroSegmentedControl.kt`, `AeroSlider.kt`, `AeroRangeSlider.kt`, and `AeroListItem.kt` **can already call these functions today** without any visibility change — the gap is that nobody outside `components/buttons/` does, which reads as accidental rather than intentional scoping.

**Action for Phase 16 (Foundation):**
1. **Move** `InteractionStates.kt` from `components/buttons/` to a new `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — purely a relocation (no visibility change needed, since `internal` already works module-wide), done to remove the "why is a switch importing something that lives in the buttons package" friction for the next five components that adopt it.
2. **Add one bundling helper** next to the three existing ones:

```kotlin
internal data class AeroInteractionState(
    val hovered: Boolean,
    val pressed: Boolean,
    val focused: Boolean,
)

@Composable
internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return AeroInteractionState(hovered, pressed, focused)
}
```

This is warranted, not optional: FEATURES.md's Gap Summary confirms 5 of 8 components (`AeroSwitch`, `AeroSegmentedControl`, both slider thumbs, `AeroListItem`'s combined selected+hover case) are wiring hover/press/focus **fresh** in this milestone. One bundling call site per component, instead of three separate `by remember { ... }` lines repeated five times, is the direct application of this project's own "shared code, not shared coincidence" lesson (the same class of lesson the PanelGroup DSL-lambda incident already taught it). `rememberAeroInteractionState` deliberately returns **only booleans**, not resolved colors/styles — each component's own `AeroSurfaceStyle` selection (`if (state.pressed) style.pressed() else if (state.hovered) style.hovered() else style.rest()`) stays component-specific, since fill/rim/gloss targets differ too much per component to share a second-level abstraction.

### Two-writer / hover-not-clearing risk (Pitfall 9) — apply, don't re-derive

Any component where a **drag** gesture and a **hover/press animation** touch the same visual value (most relevantly `AeroSlider`'s and `AeroRangeSlider`'s thumbs, if a thumb both animates a "pressed depth" *and* tracks drag position) must apply this project's already-locked Pattern 3 (`AeroPanelGroup`'s animation-target-only vs. drag-writes-directly split, `isDragging` flips the spec to `snap()`) — this is not new architecture, just a note that Q1–Q3's new primitives do not remove the need for it.

### Indication / ripple story at CMP 1.11.1 / Material3 1.4+

**No change relevant to this milestone.** UPGRADE.md Part 3 §3 (grounded in a full-text search of the official CMP CHANGELOG.md across the entire 1.7.3→1.11.1 window) found **zero matches** for `rememberRipple()`→`ripple()`, `Indication`/`IndicationNodeFactory`, or `LocalIndication` migration entries, and a repo grep confirms `rememberRipple` is not used anywhere in `library/src/main` today (this project draws its own Aero press-state visuals rather than relying on M3 ripple, consistent with the milestone's whole premise). **Practical consequence for Q5:** wherever M3's `Button`/`Slider` container is fully dropped (`AeroButton`/`AeroOutlinedButton`, per Q5), the replacement `Modifier.clickable(...)` call must pass **`indication = null`** explicitly — Aero's own press-state (scale + gradient invert, A7) is the intended feedback; a default M3/Foundation ripple underneath it would visually double up. Where M3's `Slider` container is *kept* (Q5), its own internal indication for the default thumb is irrelevant, since the custom `thumb =`/`track =` slots replace the visual entirely and M3 does not force a ripple onto caller-supplied slot content.

---

## Q5 — Rewriting the M3 Wrappers (the key architectural question)

### What M3 provides today, enumerated

| Behavior | `AeroButton`/`AeroOutlinedButton` (M3 `Button`/`OutlinedButton`) | `AeroSlider` (M3 `Slider`) |
|---|---|---|
| `Role.Button` / progress semantics | Automatic via M3's internal `Modifier.semantics` | `ProgressBarRangeInfo` etc., automatic |
| Keyboard activation (Enter/Space) | Automatic (M3 `Button` is built on `Modifier.clickable`) | Arrow-key nudge, automatic |
| Focus traversal | Automatic | Automatic |
| `LocalMinimumInteractiveComponentSize` | Present, **already overridden to `Dp.Unspecified`** at `AeroButton.kt:79`/`AeroOutlinedButton.kt:79` — pre-existing opt-out, not new scope | N/A (Slider has no equivalent floor) |
| Indication/ripple | Default M3 ripple (not currently overridden) | N/A (thumb/track visuals) |
| Drag handling | N/A | Full gesture state machine inside `SliderState` |
| Step-snap (`steps`) | N/A | Automatic, currently forwarded live at `AeroSlider.kt:81` |
| `onValueChangeFinished` contract | N/A | Automatic (fires once per completed drag/step-change gesture) |
| Disabled pointer gating | Automatic (`enabled = false` blocks all M3-owned pointer input) | Automatic |

### VERIFIED via Context7 (androidx.compose.material3 official reference): `Slider` exposes `thumb`/`track` slot composables

```kotlin
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    steps: Int = 0,
    thumb: @Composable (SliderState) -> Unit = { SliderDefaults.Thumb(interactionSource, colors) },
    track: @Composable (SliderState) -> Unit = { SliderDefaults.Track(sliderState = it, colors = colors) },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
): Unit
```

Both the `value`/`onValueChange` overload (this project's current call shape) and the newer `SliderState`-based overload expose `thumb: @Composable (SliderState) -> Unit` and `track: @Composable (SliderState) -> Unit`, each independently overridable, each defaulting to `SliderDefaults.Thumb(...)`/`SliderDefaults.Track(...)`. **This is not a new-in-1.4/1.5 API** — the shape is consistent across the current androidx reference and has been part of Material3's public surface since well before the CMP 1.9.x-era M3 1.4.0 pin UPGRADE.md documents; HIGH confidence this slot API exists at whatever specific stable M3 version Phase 15 ultimately pins (M3 1.4.x at minimum, per UPGRADE.md's own finding that CMP 1.9.1+ already resolves `compose.material3` to a stable M3 1.4.0 artifact — and the milestone's locked CMP 1.11.1 target requires an *explicit* pin to a stable M3 regardless, since the bare alias resolves to an unacceptable 1.5.0-alpha17 there). **Residual verification step, not a re-investigation:** once Phase 15 lands on its final pinned M3 coordinate, a 5-minute IDE-autocomplete check that `thumb`/`track` are present on that exact resolved artifact — cheap, and consistent with this project's "verify before asserting" discipline, but this is now a confirmation step, not an open question.

### Per-component recommendation

| Component | Recommendation | Rationale |
|---|---|---|
| **`AeroButton`** | **Hand-roll the container, but on top of Foundation's `Modifier.clickable`, not M3's `Button`/`Surface` and not a raw `pointerInput`.** `Box(Modifier.aeroSurface(style, shape).clickable(interactionSource = is, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)) { Text(...) }`. | M3 `Button`'s only container-customization surface is `colors: ButtonColors` (flat container/content colors, no `Brush`, no gradient) plus `shape`/`elevation`/`border`/`contentPadding` — there is no slot equivalent to `Slider`'s `thumb`/`track` for a Button's own container paint, so M3's *container* genuinely blocks the Aero geometry (matches the milestone's original premise for these two). But `Modifier.clickable(role = Role.Button, ...)` — a **Foundation**-level primitive, one layer below M3, not a from-scratch `pointerInput`/`Canvas` — already supplies `Role.Button` semantics, Enter/Space keyboard activation, Tab focus order, and automatic disabled-gating. This is meaningfully lower-risk than the "hand-roll like `AeroRangeSlider`" precedent PITFALLS.md's Pitfall 8 worries about, because `AeroRangeSlider` genuinely built on bare `Canvas` + `pointerInput` with **zero** `semantics`/`focusable` (confirmed, no such calls anywhere in `AeroRangeSlider.kt`) — `Modifier.clickable` does not have that gap. |
| **`AeroOutlinedButton`** | Same pattern as `AeroButton`, sharing one internal container composable/config (`AeroButtonSurface(filled: Boolean, ...)` per FEATURES.md B1/B2) so the two buttons cannot visually drift apart. | Identical M3-limitation analysis; `OutlinedButton`'s only differentiator from `Button` is `border`/transparent-fill, both fully expressible via `AeroSurfaceStyle`'s existing fields. |
| **`AeroSlider`** | **Keep M3 `Slider`. Supply custom `thumb =` and `track =` slots built from the shared `aeroSurface`/`aeroThumbSurface`/groove primitives.** | This is the most important correction this research makes to the milestone's working assumption. FEATURES.md classified `AeroSlider` as the single HIGH-complexity, drag-rewrite-required outlier (its B5 section explicitly recommended reusing `AeroRangeSlider`'s `awaitPointerEventScope` pattern). The verified slot API removes that requirement: M3 retains ALL of drag handling, keyboard arrow-nudge, `steps` snap, the `onValueChangeFinished` contract, and semantics — Pitfall 8's entire checklist (items 1–5) is satisfied *by keeping M3*, not by re-implementing it. Only the thumb's and track's **paint** changes. Complexity revises from HIGH to MEDIUM; the dependency on porting `AeroRangeSlider`'s drag loop to `AeroSlider` is removed (that pattern stays scoped to `AeroRangeSlider`, which has no M3 dual-thumb equivalent this project uses, per the existing locked PITFALL-03 decision — untouched). |
| **`AeroRangeSlider`** | No change to this question — already Canvas-based, no M3 to keep or drop; pure visual upgrade consuming `drawAeroSurfaceCore`/`drawAeroThumb` directly (per Q1). | Confirms FEATURES.md's existing B6 analysis; nothing here revises it. |

### Foundation-phase spike required before Phase 18 commits

The `thumb =`/`track =` slot recommendation needs one empirical check the milestone brief didn't originally scope: confirm that a custom-sized `AeroSurfaceStyle`-drawn thumb (target ~16–18.dp per FEATURES.md A12) and a custom groove/fill track render correctly inside M3 `Slider`'s own layout box — i.e., that supplying non-default-sized slot composables doesn't get clipped or misaligned by `Slider`'s own internal height/track-position math. This is a cheap scratch-composable spike (mirrors the already-planned `dropShadow`/`innerShadow` signature spike in UPGRADE.md Part 5) and should be added to Phase 16's exit checklist, not discovered mid-Phase-18.

---

## Q6 — Build Order

```
Phase 15  Toolchain upgrade (Kotlin 2.4.10 / CMP 1.11.1)              [already locked, separate]
    │
    ▼
Phase 16  Foundation — Aero primitives layer
    - ColorMath.kt (lighten/darken)
    - AeroOrnamentTokens.kt (+ derive())
    - AeroColorScheme.kt: + ornamentOverride trailing field
    - AeroTheme.kt: + AeroTheme.ornaments accessor
    - AeroSurfaceStyle.kt (+ raised/pressed/groove/pill presets)
    - AeroSurfacePrimitives.kt: drawAeroSurfaceCore, Modifier.aeroSurface,
      Modifier.aeroGlowRing, drawAeroThumb / Modifier.aeroThumbSurface
    - GlassModifiers.kt: BUG FIXES ONLY (endY proportional, clip-order, dead elevation
      wired or removed) — legacy 3 modifiers stay source-compatible
    - components/common/InteractionStates.kt (relocated) + rememberAeroInteractionState()
    - SPIKE: dropShadow/innerShadow real signature vs. actual 1.11.1 jar
    - SPIKE: M3 Slider thumb/track slot sizing feasibility (Q5)
    - Three-theme spot check of every new primitive — NOT deferred (Pitfall 11)
    - Full-library visual smoke pass (glassSurface/glassPanel/glassEffect touch ~40
      out-of-scope components too — see Q7)
    │
    ▼
Phase 17  Buttons — AeroButton, AeroOutlinedButton
    - First real consumer of aeroSurface + aeroGlowRing + Modifier.clickable pattern
    - Shared AeroButtonSurface(filled: Boolean) internal composable
    - Expect this phase to surface any remaining Foundation-layer bugs (FEATURES.md's
      own dependency note: "first real consumer, surfaces any remaining bugs")
    │
    ▼
Phase 18  Range — AeroSlider (M3-slot restyle), AeroRangeSlider, AeroProgressBar
    - Validates aeroThumbSurface/drawAeroThumb + groove/fill primitives together
    - AeroSlider first (proves the M3-slot approach against the Phase 16 spike findings);
      AeroRangeSlider second (reuses the now-proven thumb primitive, per FEATURES.md's
      own "second consumer, bugs fixed once for both" recommendation);
      AeroProgressBar third (shares the fill/groove renderer with AeroSlider's track)
    │
    ▼
Phase 19  Selectors + Lists — AeroSwitch, AeroSegmentedControl, AeroListItem
    - AeroSwitch: needs thumb + groove (Phase 18 dependency) — cannot move earlier
    - AeroSegmentedControl: reuses AeroButton's A7 pressed-fill code as its "active"
      segment look (Phase 17 dependency) + aeroGlowRing — no new primitives
    - AeroListItem: reuses fixed glassSurface-descended pill + aeroGlowRing (focus) —
      technically has NO hard dependency on Phase 18 and could move directly after
      Phase 17 if phase-sizing favors it; grouped here for lowest-risk/last-wave buffer
      and because Selectors+Lists together is a natural "polish wave" showcase unit
    │
    ▼
Phase 20  Verification — showcase wiring, grep-gates, three-theme sign-off
    - Distributed per-phase mini-checks (17/18/19 above) ALREADY happened —
      this phase is the consolidated pass + structural gates (Q7), not the first look
```

This order is dependency-justified, not merely FEATURES.md's wave labels restated: Buttons before Range is required because `AeroSegmentedControl` (Phase 19) reuses Button's pressed-fill code; Range before Selectors is required because `AeroSwitch`'s thumb/groove genuinely don't exist until Phase 18 builds and proves them via `AeroSlider`/`AeroRangeSlider`. `AeroListItem`'s placement in Phase 19 rather than earlier is the one non-strict-dependency choice, flagged as such for the roadmapper to move if phase-sizing argues otherwise.

---

## Q7 — Regression Surface

### What could silently break

1. **Default sizes / corner radii.** Every one of the 8 components has hardcoded or default-parameter intrinsic dimensions consumer layouts are already built against: `AeroButton(height: Dp = 30.dp)`, `AeroOutlinedButton(height: Dp = 28.dp)`, `AeroSwitch` (hardcoded `36×18.dp` track / `14.dp` thumb, not parameterized), `AeroListItem` (hardcoded `36.dp` row height), `AeroSegmentedControl` (hardcoded `28.dp` height). New ornamentation (shadow, glow ring) wanting "breathing room" is the most likely accidental cause of a silent bump (PITFALLS.md Pitfall 13).
2. **Removed M3 semantics/keyboard.** `AeroButton`/`AeroOutlinedButton` dropping M3's `Button`/`OutlinedButton` (Q5) removes the automatic `Role.Button`/keyboard/focus M3 provided — the replacement (`Modifier.clickable(role = Role.Button, ...)`) is lower-risk than a from-scratch reimplementation, but it is still a **change of implementation**, not a no-op, and must be verified, not assumed correct by design alone.
3. **`AeroSlider.steps` becoming a dead parameter** — the exact Pitfall 7 forward-looking flag — is a **non-issue under the Q5 recommendation** specifically because `steps` continues to flow into M3's own `Slider(steps = steps, ...)` call; this is a direct, positive consequence of the slot-based (not full-rewrite) decision, worth stating explicitly since it removes a previously-flagged risk rather than merely mitigating it.
4. **Existing unit tests** referencing these 8 components — grep `library/src/test` for the 8 component names before Phase 17 starts, to enumerate the exact at-risk test set rather than discovering breakage after the fact.
5. **The shared glass layer's blast radius is wider than "8 components."** `glassEffect`/`glassPanel`/`glassSurface` (`GlassModifiers.kt`) are used across the **~40 out-of-scope components** too (pickers' popup containers, `AeroPanelGroup` headers, `AeroCard`, etc. — per PROJECT.md's "explicitly NOT in scope: a sweep of the remaining ~40 components," which describes feature scope, not blast-radius scope). Fixing `glassSurface`'s `endY`/clip-order bugs in Phase 16 **necessarily re-renders every existing consumer of those three modifiers**, not just the 8 target components. This is a real regression-surface expansion the milestone's framing understates — addressed by Phase 16's own "full-library visual smoke pass" exit item (Q6), catching it at the cheapest possible point (immediately after the fix, before 4 more phases build on top of it) rather than at final sign-off.
6. **No real external consumer app available this milestone.** `aska` and `satellite-control` are explicitly pinned to the old toolchain / `2.0.4` and are NOT expected to track this milestone (PROJECT.md, STATE.md). The project's strongest historical regression-catcher (a real consumer app — it caught the actual RCMP-04 root cause and the SplitPane stale-state bug that showcase sign-off alone missed) is unavailable this time. This raises the relative importance of the structural gates below and of a minimal scratch-consumer smoke step.

### Regression-guard strategy — named, and built to be provably-failing-first per `feedback_repro_must_exercise_path`

| Guard | What it catches | Provably-fails-on-unfixed-code how |
|---|---|---|
| **Defaults Snapshot Test** — a plain JVM test asserting every public default-parameter numeric literal across the 8 components' signatures (height, corner radius, track/thumb sizes) against a recorded pre-milestone baseline | Pitfall 13 (silent size creep) | Written from the *current* values before any component-phase work starts; any unreviewed literal change flips it immediately — deterministic, not a screenshot judgment call |
| **Keyboard-activation UI test** (`runComposeUiTest`, same style as `AeroPanelGroupRecomposeUiTest`) for `AeroButton`/`AeroOutlinedButton`: programmatic Tab-focus + `Enter`/`Space` triggers `onClick` | Pitfall 8 (lost M3 semantics/keyboard) | Written **before** the M3→`clickable` conversion (fails against a hypothetically-broken hand-roll that forgot `role`/keyboard wiring), passes after — TDD-red-then-green, the exact bar this project's own memory (`feedback_repro_must_exercise_path`) requires |
| **Clip-before-paint centralization** — not a grep-gate first, a **structural** guarantee: because `aeroSurface()` (Q1) is the single place the clip/dropShadow/innerShadow ordering is written, Pitfall 6 cannot recur at a NEW call site without someone bypassing the shared function. The grep-gate (`.drawBehind`/`.drawWithContent` preceded by `.clip(`) becomes a cheap **"did anyone bypass `aeroSurface()`"** check, not the primary defense | Pitfall 6 (draw-before-clip) | Grep is zero-cost and mechanical; the real defense is architectural (one function, not per-call-site discipline) |
| **Gradient-literal grep-gate** — regex for bare numeric `Float` literals in `startY`/`endY`/`startX`/`endX` gradient arguments across `GlassModifiers.kt`, `AeroSurfacePrimitives.kt`, and the 8 component files (only `size.*`/`.toPx()`-derived values allowed) | Pitfall 4 (non-proportional hardcoded gradients — the exact `endY = 100f` bug already confirmed in `glassSurface`) | Mechanical, zero-match required, checkable without running the app |
| **Full-library visual smoke pass**, end of Phase 16, all ~50 components not just the 8 targets | Item 5 above (shared glass-layer blast radius) | Distributed early rather than deferred — matches this project's own explicit v2.0-retrospective lesson (Pitfall 14) about aggregate-at-the-end verification being its "biggest inefficiency" |
| **Distributed per-phase three-theme sign-off** (Phases 17/18/19 each get their own mini pass) + one consolidated Phase 20 pass | Pitfall 14 (human sign-off false-positive) | Matches v2.0's own precedent; Phase 20's consolidated pass is a re-check, not the first look |
| **Minimal scratch-consumer smoke step** — a throwaway `publishToMavenLocal` + tiny non-showcase-styled screen exercising `AeroButton`/`AeroSlider`/`AeroListItem` outside the `:showcase` module's own styling conventions | Item 6 above (no real consumer app this milestone) | Cheap insurance against showcase-only blind spots; explicitly named because the project has direct history of this exact gap mattering (RCMP-04's real root cause, the SplitPane stale-state regression — both caught outside showcase, not by it) |

---

## Integration Points — Concrete File/Type Map

| File | Status | New/Changed types |
|---|---|---|
| `library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt` | NEW | `internal fun Color.lighten(Float): Color`, `internal fun Color.darken(Float): Color` |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` | NEW | `public data class AeroOrnamentTokens(...)`, `AeroOrnamentTokens.Companion.derive(base: AeroColorScheme)` |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` | MODIFIED | + trailing `val ornamentOverride: AeroOrnamentTokens? = null` (source-compatible additive field) |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` | MODIFIED | + `AeroTheme.ornaments: AeroOrnamentTokens` computed accessor |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` | NEW | `public data class AeroSurfaceStyle(...)` + `raised()`/`pressed()`/`groove()`/`pill()` presets |
| `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` | NEW | `internal fun DrawScope.drawAeroSurfaceCore(...)`, `public fun Modifier.aeroSurface(...)`, `public fun Modifier.aeroGlowRing(...)`, `internal fun DrawScope.drawAeroThumb(...)`, `public fun Modifier.aeroThumbSurface(...)` |
| `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` | MODIFIED (bug fixes only) | `glassPanel`/`glassSurface`/`glassEffect` signatures unchanged; `endY` proportional fix, clip-order fix, `elevation` wired or removed |
| `library/src/main/kotlin/com/mordred/aero/components/buttons/InteractionStates.kt` → `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` | MOVED + EXTENDED | + `internal data class AeroInteractionState`, `internal fun rememberAeroInteractionState(InteractionSource): AeroInteractionState` |
| `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` | REWRITTEN (Phase 17) | Drops M3 `Button`; `Box(Modifier.aeroSurface(...).clickable(role = Role.Button, indication = null, ...))` |
| `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt` | REWRITTEN (Phase 17) | Same pattern, shares internal `AeroButtonSurface(filled: Boolean, ...)` with `AeroButton` |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` | MODIFIED (Phase 18) | KEEPS `Slider(...)`; adds custom `thumb = {...}`/`track = {...}` slot lambdas using `aeroThumbSurface`/groove primitives |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` | MODIFIED (Phase 18) | Visual-only; `drawAeroSurfaceCore`/`drawAeroThumb` called directly inside existing `Canvas` |
| `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt` | MODIFIED (Phase 18) | Groove + fill via `aeroSurface`, shares fill renderer with `AeroSlider`'s track slot |
| `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` | MODIFIED (Phase 19) | Track via `aeroSurface` (groove/raised presets), thumb via `aeroThumbSurface`, hover/press/focus via `rememberAeroInteractionState` (currently has NONE of this wiring) |
| `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` | MODIFIED (Phase 19) | Per-segment `aeroSurface` + `aeroGlowRing`, selected-segment reuses `AeroButton`'s A7 pressed-fill code path |
| `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` | MODIFIED (Phase 19) | Adds missing `.clip()`, selection pill via `aeroSurface` (no drop shadow, per Q2), combined selected+hover branch fixed |

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Per-component bespoke gradient/bevel code

**What people would do:** write `AeroSwitch`'s track gradient inline, then `AeroSlider`'s groove inline, slightly differently, because "it's just a few lines each."
**Why it's wrong:** this is precisely the `PanelGroup`-DSL-lambda class of risk already in this project's memory — two independently-maintained copies of "the same visual idea" silently drift, and a bug fixed in one is not fixed in the other.
**Do this instead:** every one of the 8 components calls `Modifier.aeroSurface`/`drawAeroSurfaceCore`/`aeroThumbSurface` — zero component owns its own gradient-construction code.

### Anti-Pattern 2: Reaching for `pointerInput`/`Canvas` reflexively when "hand-rolling"

**What people would do:** treat "M3 geometry blocks the look, so custom-draw it" as license to drop all the way to `AeroRangeSlider`'s bare-Canvas-plus-manual-loop pattern for `AeroButton`/`AeroOutlinedButton` too.
**Why it's wrong:** `AeroRangeSlider` had no M3 equivalent to fall back on for its precision-drag requirement (PITFALL-03) — that's why it goes all the way to the bottom. `AeroButton`/`AeroOutlinedButton` and `AeroSlider` are not in that position: Foundation's `Modifier.clickable` (Buttons) and M3's `thumb =`/`track =` slots (Slider) both exist specifically to avoid re-deriving semantics/keyboard/drag from scratch.
**Do this instead:** drop only the layer that actually blocks the look (M3's `Surface`/`ButtonColors` for Buttons; M3's default thumb/track *painting* for Slider) and keep every layer below it that isn't blocking anything (Foundation's `clickable`; M3's `SliderState` gesture/keyboard/step-snap machinery).

### Anti-Pattern 3: Treating `AeroColorScheme` as a place to keep adding raw color fields

**What people would do:** add `glossHighlight2`, `bevelLight`, `bevelShadow`, `rimAero`, etc. directly as new `AeroColorScheme` constructor parameters, one per device.
**Why it's wrong:** compounds the already-23-field data class, multiplies the "did we tune this for all three themes" audit surface per Pitfall 11, and every future ornament device would demand another round of the same three-theme hand-tuning.
**Do this instead:** `AeroOrnamentTokens.derive(base)` — one algorithm, three themes for free, one trailing override field for the rare case a custom theme needs to hand-author its own.

---

## Sources

- `.planning/research/FEATURES.md` — device catalog (A1–A13), per-component treatment (B1–B8), dependency graph, interaction-state matrix. Builds on this directly; not restated except where a finding is revised (Q5).
- `.planning/research/PITFALLS.md` — 14 named pitfalls, especially Pitfall 1 (`drawWithCache`), Pitfall 6 (clip-order, confirmed bug), Pitfall 7 (dead parameters), Pitfall 8 (M3 semantics/keyboard loss), Pitfall 11 (theme-dependent breakage), Pitfall 13 (size creep), Pitfall 14 (sign-off false positives).
- `.planning/research/UPGRADE.md` — CMP 1.11.1/Kotlin 2.4.10 compatibility findings, `dropShadow`/`innerShadow` verified signature (Part 4), Material3-alias-must-be-pinned finding (Part 2), test-infrastructure dispatcher-default risk (Part 3 §6).
- `.planning/STATE.md`, `.planning/PROJECT.md` — v3.0 scoping decisions, baseline defect table, locked cross-milestone conventions (single-`drawBehind` rule, `internal` module-scoping precedent from Phase 7).
- Context7, `/websites/developer_android_reference_kotlin_androidx_compose_material3` — **direct verification** of `Slider(..., thumb: @Composable (SliderState) -> Unit, track: @Composable (SliderState) -> Unit, ...)` and `SliderDefaults.Thumb`/`SliderDefaults.Track`, both the `value`/`onValueChange` overload and the `SliderState` overload. This is the load-bearing finding for Q5. HIGH confidence — official androidx reference, two independently-returned doc entries agreeing on the same shape.
- Direct code inspection (this repo, read in full for this research): `library/build.gradle.kts`, `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt`, `AeroColorScheme.kt`, `AeroTheme.kt`; `components/buttons/AeroButton.kt`, `AeroOutlinedButton.kt`, `InteractionStates.kt`; `components/range/AeroSlider.kt`, `AeroRangeSlider.kt`, `AeroProgressBar.kt`; `components/selection/AeroSwitch.kt`, `AeroSegmentedControl.kt`; `components/list/AeroListItem.kt`.

---
*Architecture research for: aero-compose-ui v3.0 Glass Refinement*
*Researched: 2026-07-21*
