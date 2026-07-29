# Phase 17: Buttons - Research

**Researched:** 2026-07-23
**Domain:** Compose Multiplatform (desktop) component restyling — consuming an already-shipped internal "Aero glass" primitives layer (Phase 16) to repaint two existing M3-backed buttons, with zero new external dependencies.
**Confidence:** HIGH

## Summary

This phase does not introduce new technology — it is a pure consumption exercise against Phase 16's shipped primitives layer (`drawAeroSurfaceCore` / `Modifier.aeroSurface` / `Modifier.aeroGlowRing` / `AeroSurfaceStyle` / `rememberAeroInteractionState`), all of which were read directly from source for this research (not inferred from docs). The two files being replaced (`AeroButton.kt`, `AeroOutlinedButton.kt`) are small (~113/~112 lines), M3-`Button`/`OutlinedButton`-backed, and every current defect (square-corner hover bleed, non-proportional-nothing since there's no gloss at all today, flat overlay, scale-shrink press, hard border focus, uniform-alpha disabled) is already independently fixed by primitives that exist and are proven working today (three-theme reviewed at Phase 16 sign-off).

The one genuinely new design decision this phase must resolve in code (not already fully pre-computed by 17-UI-SPEC.md, which is unusually complete) is **how to structure the shared internal surface composable and its state-resolution logic** so that (a) filled vs. outlined and all five states are expressed as parameters/transforms of one function, not two painters, and (b) the pressed-fill transform is cleanly reusable by Phase 19's `AeroSegmentedControl` without either phase reaching into the other's package. This research recommends extracting **pure, Compose-free style-resolution functions** (mirroring the project's own `PanelDistribution.kt`/`SplitClampTest.kt` precedent) so the state → `AeroSurfaceStyle` mapping is unit-testable without `runComposeUiTest`, and placing the **pressed/recessed transform as an extension function in `theme/AeroSurfaceStyle.kt`** (not inside `components/buttons/`) so Phase 19 can import it without a cross-package reach-around.

A second finding worth flagging explicitly: CONTEXT.md's canonical refs point to `AeroListItem` as "the" hover-wiring precedent, but the **more precise in-package analog is `AeroIconButton.kt`**, which already demonstrates the exact pattern VBTN-04 needs — `Modifier.clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)` alone, with `rememberHoverState`/`rememberPressedState`/`rememberFocusState` (soon `rememberAeroInteractionState`) collecting from that *same* interaction source. `AeroListItem` additionally calls `.hoverable(interactionSource)` only because its `onClick` is nullable (a row can be non-clickable but still hover-highlighted); `AeroButton`'s `onClick` is a required, non-nullable parameter, so that extra call is unnecessary here and should not be blindly copied.

**Primary recommendation:** Build one `internal` composable (e.g. `AeroButtonSurface`) that takes an already-resolved `AeroSurfaceStyle` (or a small state-bundle it resolves itself from pure functions) plus `shape`/`interactionSource`/`enabled`/`onClick`/`content`, chains `Modifier.clickable(role = Role.Button, indication = null, interactionSource = interactionSource, enabled = enabled, onClick = onClick).aeroGlowRing(focus...).aeroGlowRing(hover...).aeroSurface(currentStyle, shape)`, and let `AeroButton`/`AeroOutlinedButton` differ only in the style-transform they pass in (filled vs. outlined per D-06's fixed delta) and their own height/padding defaults (unchanged, locked).

## Architectural Responsibility Map

This is a component-library restyle, not a multi-tier app — "tiers" here are re-scoped to the project's own layering (Theme primitives → shared component surface → public component). Reinterpreted accordingly, per the same reinterpretation convention 17-UI-SPEC.md already applied to Color/Copywriting.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Gradient/gloss/bevel/rim pixel drawing | Theme primitives (`theme/AeroSurfacePrimitives.kt`) | — | `drawAeroSurfaceCore` is the *only* place surface geometry is authored (PRIM-05); the button never draws its own gradients |
| Per-state style resolution (rest/hover/press/focus/disabled → `AeroSurfaceStyle`) | Component (`components/buttons/`), as pure functions | Theme (`AeroOrnamentTokens`) supplies the raw accent-derived tokens | The button owns "which style for which state"; the ornament layer owns "what accent-derived colors look like" — kept separate so the resolver is unit-testable without Compose |
| Cross-phase reusable pressed/recessed transform | Theme (`theme/AeroSurfaceStyle.kt`, extension fn) | Component (`components/buttons/`, `components/selection/` in Phase 19) | Must live somewhere both packages can import within the same Gradle module — `theme/` is the natural shared home, matching `AeroSurfaceStyle.rest()`'s existing companion-object location |
| Interaction state collection (hover/press/focus booleans) | `components/common/InteractionStates.kt` (`rememberAeroInteractionState`) | Component (buttons/) | Single collector shared by all Phase 17-19 components (PRIM-15); button consumes booleans only, resolves its own colors |
| Click + keyboard semantics (`Role.Button`, Space/Enter) | Component (`components/buttons/`), via `Modifier.clickable` | — | No M3 container tier exists anymore after this phase — semantics live directly on the button's own root modifier chain |
| Theme/color tokens | Theme (`AeroColorScheme` / `AeroOrnamentTokens`) | — | Never hardcoded at the component level (locked project convention) |

## Package Legitimacy Audit

**Not applicable — this phase introduces zero new external dependencies.** It consumes only:
- Compose Foundation/UI APIs already declared in `library/build.gradle.kts` and already used elsewhere in this codebase (`Modifier.clickable`, `androidx.compose.ui.semantics.Role`, `androidx.compose.ui.test.*`).
- The Phase 16 in-house primitives (`drawAeroSurfaceCore`, `Modifier.aeroSurface`, `Modifier.aeroGlowRing`, `AeroSurfaceStyle`, `AeroOrnamentTokens`, `rememberAeroInteractionState`) — already shipped and merged, not external packages.

No `npm install` / `pip install` / `cargo add` equivalent applies to this phase. The Package Legitimacy Gate protocol is skipped per its own scope rule (no packages to check).

## Standard Stack

### Core (already in place — no version changes this phase)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.4.10 | Language | Locked toolchain target, Phase 15 `[VERIFIED: STATE.md — Phase 15 gate passed]` |
| Compose Multiplatform (desktop) | 1.11.1 | UI runtime, `Modifier.clickable`/`dropShadow`/`innerShadow` | Locked toolchain target, Phase 15 `[VERIFIED: STATE.md]` |
| Material3 | pinned explicit stable coordinate (not the `compose.material3` alpha-resolving alias) | Only for `Text()`/typography access in showcase; buttons themselves drop M3 `Button`/`OutlinedButton` containers this phase | `[VERIFIED: STATE.md — TOOL-02]` |
| `androidx.compose.ui.semantics.Role` | bundled with Compose UI | `Role.Button` semantics for `Modifier.clickable` | `[VERIFIED: in-repo precedent — AeroCheckbox.kt/AeroSwitch.kt/AeroRadioButton.kt already import and use `Role`]` |

### Supporting (Phase 16 in-house primitives, already shipped)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `theme/AeroSurfacePrimitives.kt` | shipped Phase 16 | `drawAeroSurfaceCore`, `Modifier.aeroSurface`, `Modifier.aeroGlowRing` | Every surface paint / hover-focus glow in this phase |
| `theme/AeroSurfaceStyle.kt` | shipped Phase 16 | Declarative style data class + `rest()` factory | Base for every state's style derivation |
| `theme/AeroOrnamentTokens.kt` | shipped Phase 16 | `derive(base)` — accent-derived fill/bevel/glow tokens | Confirms D-01's "accent identity" requirement is already met by `rest()` (see Finding below) |
| `components/common/InteractionStates.kt` | shipped Phase 16 | `rememberAeroInteractionState(source)` | Single hover/press/focus collector for both buttons |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `Modifier.clickable(interactionSource=...)` alone for hover+press+focus | `Modifier.clickable(...) + Modifier.hoverable(...)` (AeroListItem's pattern) | Redundant here — `AeroListItem` needs the split only because its `onClick` is nullable; `AeroButton`'s `onClick` is required, so the split adds a second hover-emission path for no benefit `[CITED: developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions]` |
| One `internal` shared surface composable (VBTN-06) | Two independent painter functions with duplicated gradient code | Reintroduces the exact drift risk this phase exists to remove; explicitly rejected by D-06 |
| Native `Modifier.innerShadow` for the press recess | Manual gradient rim (as `drawAeroGroove` does today for the track recess) | Native chosen per Phase 16 D-02 precedent and compile-proven in `ScratchAeroShadowProof.kt`; gradient rim is the named fallback if the three-theme review flags artifacts |

**Installation:** none — no new Gradle dependency lines this phase.

## Architecture Patterns

### System Architecture Diagram

```
Caller (showcase / consumer app)
        │
        │  AeroButton(text, onClick, enabled, height, interactionSource)
        │  AeroOutlinedButton(text, onClick, enabled, height, interactionSource)
        ▼
┌─────────────────────────────────────────────────────────────┐
│  Shared internal surface composable (new — e.g.              │
│  "AeroButtonSurface")                                        │
│                                                                │
│  1. rememberAeroInteractionState(interactionSource)           │
│         → { hovered, pressed, focused }                       │
│  2. Pure state resolver (new, Compose-free):                  │
│     baseStyle = filledStyle(colors) | outlinedStyle(colors)   │
│     currentStyle = baseStyle                                  │
│         .let { if (hovered) it.lighten() else it }             │
│         .let { if (pressed) it.pressedRecess() else it }       │  ◄── theme/AeroSurfaceStyle.kt
│         .let { if (!enabled) it.flattenDisabled() else it }     │      extension fns — shared
│  3. Modifier chain:                                            │      with Phase 19 VSEL
│     Modifier.height(h)                                         │
│       .clickable(role = Role.Button, indication = null,        │
│                  interactionSource = interactionSource,         │
│                  enabled = enabled, onClick = onClick)          │
│       .aeroGlowRing(focused && enabled, focusGlowColor, 4.dp)   │
│       .aeroGlowRing(hovered && enabled, hoverGlowColor, 4.dp)   │
│       .aeroSurface(currentStyle, RoundedCornerShape(4.dp))      │
│  4. Text(text, maxLines = 1, overflow = Ellipsis)               │
└─────────────────────────────────────────────────────────────┘
        │                                   │
        ▼                                   ▼
  drawAeroSurfaceCore                  Phase 19 AeroSegmentedControl
  (theme/AeroSurfacePrimitives.kt —    (imports the SAME
   fill/gloss/bevel/rim, unchanged)     theme/AeroSurfaceStyle.kt
                                        pressedRecess() extension —
                                        does NOT import from
                                        components/buttons/)
```

A reader can trace: caller → shared surface composable → interaction-state collection → pure style resolution (state in, `AeroSurfaceStyle` out) → modifier chain (clickable outermost for semantics, glow rings, then `aeroSurface`'s own internal `.clip()` + draw) → the actual pixels via the untouched Phase 16 primitive. The dotted-line branch shows the one piece (`pressedRecess()`) that must be reachable from a sibling package in Phase 19 without importing `components/buttons/*`.

### Recommended Project Structure
```
library/src/main/kotlin/com/mordred/aero/
├── theme/
│   ├── AeroSurfaceStyle.kt        # ADD: pressedRecess()/flattenDisabled() extension fns here (shared, cross-phase)
│   ├── AeroSurfacePrimitives.kt   # UNCHANGED this phase
│   └── AeroOrnamentTokens.kt      # UNCHANGED this phase
├── components/
│   ├── buttons/
│   │   ├── AeroButtonSurface.kt   # NEW: shared internal composable (VBTN-06 vehicle) + pure filled/outlined style resolvers
│   │   ├── AeroButton.kt          # MODIFIED: thin public wrapper, filled style + 30.dp/12.dp
│   │   └── AeroOutlinedButton.kt  # MODIFIED: thin public wrapper, outlined style + 28.dp/10.dp
│   └── common/
│       └── InteractionStates.kt   # UNCHANGED (already shipped rememberAeroInteractionState)
└── (Phase 19, later) components/selection/AeroSegmentedControl.kt  # imports theme/AeroSurfaceStyle.kt's pressedRecess()
```

### Pattern 1: Shared Internal Surface, Param-Differentiated (VBTN-06)

**What:** One `internal` composable backs both `AeroButton` and `AeroOutlinedButton`; filled vs. outlined is expressed as a parameter/style-transform, never a second painter.
**When to use:** Any time two public variants must be structurally guaranteed not to drift.
**Precedent in this codebase:** `AeroPanelGroupImpl(orientation)` (v2.0.2, Phase 13.1) — one internal core, one additive parameter, zero drift, confirmed by 12 unchanged logic tests plus zero-regression sign-off.
**Example (skeleton, not exact final signature — Claude's discretion per CONTEXT.md):**
```kotlin
// package com.mordred.aero.components.buttons  (internal, not part of public API)
@Composable
internal fun AeroButtonSurface(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp,
    contentPadding: PaddingValues,
    outlined: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val colors = AeroTheme.colors
    val state = rememberAeroInteractionState(interactionSource)
    val shape = RoundedCornerShape(4.dp)
    val style = resolveButtonStyle(colors, outlined, state, enabled) // pure fn, see Pattern 7

    Box(
        modifier
            .height(height)
            .clickable(
                role = Role.Button,
                indication = null,
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = onClick,
            )
            .aeroGlowRing(state.focused && enabled, colors.borderSelected, cornerRadius = 4.dp)
            .aeroGlowRing(state.hovered && enabled, AeroOrnamentTokens.derive(colors).hoverGlow, cornerRadius = 4.dp)
            .aeroSurface(style, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, /* ... */)
    }
}
```

### Pattern 2: Accent-Driven Filled Style — Already Satisfied by `rest()` (D-01 finding)

**What:** 17-UI-SPEC.md documents a "finding to confirm during planning": reading the actual shipped `AeroOrnamentTokens.derive()`, `fillSplitTop`/`fillSplitBottom`/`bevelLight`/`bevelShadow`/`hoverGlow` are **already** derived from `base.primary` (`derive()` lines confirmed by direct read this session), not from a neutral token. Only `glossHighlight`/`rimLight` are neutral-derived — correctly, per D-01's own instruction that gloss/rim should read as "light hitting glass" regardless of hue.
**Confirmed by direct source read `[VERIFIED: AeroOrnamentTokens.kt, read this session]`:**
```kotlin
public fun derive(base: AeroColorScheme): AeroOrnamentTokens = AeroOrnamentTokens(
    glossHighlight = base.glassHighlight.lighten(0.15f),   // neutral-derived — correct
    bevelLight = base.primary.lighten(0.25f),               // accent-derived
    bevelShadow = base.primary.darken(0.20f),                // accent-derived
    rimLight = base.glassBorder.lighten(0.10f),               // neutral-derived — correct
    hoverGlow = base.primary.lighten(0.30f),                   // accent-derived
    fillSplitTop = base.primary.lighten(0.18f),                 // accent-derived
    fillSplitBottom = base.primary.darken(0.12f),                // accent-derived
)
```
**Implication for the planner:** `AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)` used verbatim already satisfies D-01's "not the neutral default" wording, because the fields that matter (fill/bevel/glow) are not neutral — only gloss/rim are, by design. **No bespoke accent-override style is needed for the rest state.** This must still be flagged to the human three-theme reviewer as a literal-wording confirmation (D-01 said "not... the neutral `AeroSurfaceStyle.rest(base)` default" when in fact `rest()`'s fields already resolve to accent colors) — treat as resolved-by-construction, not silently reinterpreted.

### Pattern 3: Pressed/Recessed Style Transform — Cross-Phase Reusable Asset (D-02 → Phase 19)

**What:** Press swaps fill/bevel direction (exact idiom already proven by `drawAeroGroove` for track recesses) and adds an inner-shadow rim, with gloss disabled.
**Precedent — the exact field-swap idiom to reuse, not reinvent** `[VERIFIED: AeroSurfacePrimitives.kt, read this session]`:
```kotlin
internal fun DrawScope.drawAeroGroove(style: AeroSurfaceStyle, cornerPx: Float) {
    drawAeroSurfaceCore(
        style = style.copy(
            fillTop = style.fillBottom,
            fillBottom = style.fillTop,
            bevelLight = style.bevelShadow,
            bevelShadow = style.bevelLight,
            glossAlpha = 0f,
        ),
        cornerPx = cornerPx,
    )
    // + a bevelShadow-derived inner-shadow cue hugging the top edge
}
```
**Recommendation:** extract the *style transform* (not the drawing) as a small extension function living in `theme/AeroSurfaceStyle.kt` — e.g. `internal fun AeroSurfaceStyle.pressedRecess(innerShadow: Shadow): AeroSurfaceStyle = copy(fillTop = fillBottom, fillBottom = fillTop, bevelLight = bevelShadow, bevelShadow = bevelLight, glossAlpha = 0f, innerShadow = innerShadow)`. Because Kotlin's `internal` visibility is module-wide (not package-wide), this is directly importable from `components/selection/AeroSegmentedControl.kt` in Phase 19 without any public API surface change — matching the project's own confirmation in Phase 16 notes that `internal` relocation required zero visibility change. **Do not place this transform inside `components/buttons/`** — that would force Phase 19 to import across a component-package boundary, which is exactly the kind of coupling the "one shared internal surface" convention exists to avoid.
**Inner shadow value (from 17-UI-SPEC.md, already decided):** `Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp))`, applied via `AeroSurfaceStyle.innerShadow` — `Modifier.aeroSurface()` already applies `style.innerShadow` automatically (chained after `.clip()`/`drawWithCache`), so no extra draw call is needed in the button itself.

### Pattern 4: Dual Independent Glow Rings for Hover + Focus (D-03/D-04)

**What:** Hover and focus are each their own `aeroGlowRing(...)` call with a *different* token (`hoverGlow` vs. `borderSelected`), chained independently so both bloom together when co-active.
**Example (from 17-UI-SPEC.md, confirmed against the primitive's actual signature):**
```kotlin
Modifier
    .aeroGlowRing(active = focused && enabled, glowColor = colors.borderSelected, cornerRadius = 4.dp)
    .aeroGlowRing(active = hovered && enabled, glowColor = ornaments.hoverGlow, cornerRadius = 4.dp)
    .aeroSurface(style, shape)
```
**Critical ordering rule** `[VERIFIED: AeroSurfacePrimitives.kt KDoc, read this session]`: both `aeroGlowRing` calls MUST precede `aeroSurface` in the chain — `aeroSurface`'s `.clip(shape)` is its outermost paint-affecting modifier, and a `.clip()` earlier in the chain erases the drawing of everything nested after it. This was the exact defect found and fixed at the Phase 16 sign-off (glow was invisible until reordered + made multi-ring).
**Pitfall:** `cornerRadius` on both `aeroGlowRing` calls and on `aeroSurface`'s shape/`AeroSurfaceStyle.cornerRadius` MUST all be the same explicit `4.dp` — never left at any function's own default (`aeroGlowRing`'s default is `8.dp`, `AeroSurfaceStyle.rest()`'s default is `8.dp`), per 16-RESEARCH.md Pitfall 5 and 17-UI-SPEC.md's explicit Spacing Scale callout.

### Pattern 5: Filled → Outlined as a Fixed Delta, Never a Second Painter (D-06)

**What:** every field in the filled per-state table transformed by one fixed ratio set (fill alpha ×~0.15, gloss 0.32→~0.15, rim 0.6→~0.85), applied identically across all 5 states.
**Recommendation:** express this as a pure function `outlinedStyle(filled: AeroSurfaceStyle): AeroSurfaceStyle = filled.copy(fillTop = filled.fillTop.copy(alpha = filled.fillTop.alpha * 0.15f), fillBottom = ..., glossAlpha = filled.glossAlpha * (0.15f/0.32f), rimAlpha = 0.85f)` — computed once from the already-resolved filled style for the same state, so the outlined variant is provably "the filled style, transformed," not an independently-authored set of literals (this is what makes VBTN-06's "cannot visually drift" true by construction per 17-UI-SPEC.md).

### Pattern 6: `clickable(interactionSource=...)` Alone Is Sufficient for Hover+Press+Focus (corrects a naive precedent copy)

**What:** `Modifier.clickable(interactionSource = source, ...)` emits hover, focus, AND press interactions into the passed `InteractionSource` — a separate `.hoverable(source)` call is redundant when `onClick` is a required (non-nullable) parameter.
**Verified precedent in this exact codebase** `[VERIFIED: AeroIconButton.kt, read this session]`:
```kotlin
Box(
    modifier = modifier
        .size(size)
        .clip(shape)
        // ...
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
)
// hovered/pressed/focused all collected from the SAME interactionSource via
// rememberHoverState/rememberPressedState/rememberFocusState — no .hoverable() call anywhere in this file
```
**Cross-checked against official docs** `[CITED: developer.android.com/develop/ui/compose/touch-input/user-interactions/handling-interactions]`: "Use Modifier.clickable as a high-level abstraction to handle hover, focus, and press interactions simultaneously... This InteractionSource will emit hover, focus, and press interactions." `AeroListItem`'s separate `.hoverable(interactionSource)` call exists only because its `onClick` is nullable (`(() -> Unit)? = null`) — a row must still show hover feedback even when non-clickable, which requires hover tracking independent of whether `.clickable(...)` is conditionally attached at all. `AeroButton`/`AeroOutlinedButton`'s `onClick` is a required parameter, so this split does not apply — `Modifier.clickable(interactionSource = interactionSource, ...)` is the complete, non-redundant pattern.
**Add `role = Role.Button`** to this same call (VBTN-04's explicit requirement) — `AeroIconButton` does not currently pass `role`, so it is not a 1:1 template for the semantics requirement, only for the interaction-wiring shape.

### Pattern 7: Pure Style-Resolution Functions for Testability

**What:** extract the state → `AeroSurfaceStyle` mapping (hover-lighten, press-recess, disabled-flatten, filled↔outlined delta) as plain Kotlin functions taking `AeroColorScheme`/`AeroOrnamentTokens`/booleans and returning `AeroSurfaceStyle` — zero Compose imports, callable from `kotlin.test` without `runComposeUiTest`.
**Precedent in this codebase** `[VERIFIED: STATE.md — v2.0.2 entry]`: `PanelDistribution.kt` (8 functions, zero Compose imports) + 12 GREEN JVM tests written before any Compose code, cited explicitly as the project's TDD model for exactly this kind of "logic separable from rendering" case.
**Why this matters here:** `AeroSurfaceStyle` fields are plain `Color`/`Float`/`Dp` values — every state transform (D-01..D-06) is describable as pure data transformation, which is both faster to test (no Compose test harness startup) and gives the Validation Architecture section below concrete, automatable per-requirement test targets instead of relying solely on manual three-theme review.

### Anti-Patterns to Avoid
- **Two independent painter functions for filled/outlined:** defeats VBTN-06's entire purpose — reintroduces the drift risk the phase exists to remove.
- **Adding `.hoverable(interactionSource)` alongside `.clickable(interactionSource=...)` on these buttons:** redundant (Pattern 6) — copy `AeroIconButton`'s shape, not `AeroListItem`'s, for these two components specifically.
- **Falling through to `AeroSurfaceStyle.rest()`'s or `aeroGlowRing()`'s own default `cornerRadius = 8.dp`:** must be explicit `4.dp` at every call site (17-UI-SPEC.md Spacing Scale, locked).
- **Leaving `LocalMinimumInteractiveComponentSize` / the `graphicsLayer { scaleX/scaleY }` press-shrink in place:** both are dead once the M3 container and the D-02 scale-shrink are removed — leftover scaffolding around a component that no longer needs it is a silent-regression risk (nothing enforces their removal automatically).
- **Placing the pressed-recess style transform inside `components/buttons/`:** blocks clean Phase 19 reuse; put it in `theme/AeroSurfaceStyle.kt` (Pattern 3).
- **Omitting `role = Role.Button`:** click still works without it, but the button's accessibility semantics degrade silently (no test failure unless a semantics-role assertion is specifically written) — this is precisely the kind of "looks fine, isn't" regression VER-04 was created to catch.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Two-tone fill / gloss / bevel / rim drawing | A bespoke `drawWithContent`/`Canvas` block per button | `Modifier.aeroSurface(style, shape)` → `drawAeroSurfaceCore` | Single source of truth (PRIM-05); any bespoke draw call duplicates already-solved gradient math and reintroduces the exact clip-ordering bug VBTN-03 fixes |
| Hover/focus glow | A custom radial-gradient halo | `Modifier.aeroGlowRing(active, glowColor, cornerRadius)` | A naive single `Brush.radialGradient` centered on the box was tried and rejected at Phase 16 sign-off — it fades to near-zero exactly at the perimeter for non-square shapes; the shipped primitive's concentric-ring approach is the proven fix, with a regression test (`AeroSurfacePrimitivesTest.aeroGlowRingOuterBloomIsNotASingleGradientCenteredOnTheBox`) already guarding against reverting to it |
| Pressed recessed look | A new inner-shadow gradient calculation | The `drawAeroGroove` field-swap idiom, extracted as a style-transform (Pattern 3) | Zero new gradient math — same fill/bevel channels, direction reversed |
| Hover/press/focus collection | A fresh `InteractionSource` collector per button | `rememberAeroInteractionState(source)` (PRIM-15) | Single collection point for every Phase 17-19 component; deliberately returns raw booleans so this phase still picks its own style variant |
| Keyboard-activatable click semantics | A custom `pointerInput`/`onKeyEvent` handler | `Modifier.clickable(role = Role.Button, ...)` | Already handles Space/Enter activation and the `Role.Button` semantics node natively `[CITED: developer.android.com/develop/ui/compose/touch-input/keyboard-input/commands]`; the `AeroRangeSlider` zero-semantics precedent this project explicitly wants not to repeat is the cautionary tale |
| Color lightening/darkening for state deltas | Ad hoc `.copy(alpha = ...)` tricks | `Color.lighten()`/`Color.darken()` (PRIM-01, `ColorMath.kt`) | Alpha tricks are silently invisible on `AeroColorScheme.Classic`'s fully-opaque tokens — RGB-mix is the only theme-correct approach, already proven project-wide |

**Key insight:** every drawing/interaction primitive this phase needs already exists and was three-theme-reviewed in Phase 16. The only genuinely new code this phase writes is *which style, for which state* — pure data transformation, not new rendering or gesture-handling logic. Treat any temptation to add a `Canvas`, `drawWithContent`, or custom pointer-input block as a signal something is being re-derived that already exists.

## Common Pitfalls

### Pitfall 1: `cornerRadius` default mismatch (8.dp vs. locked 4.dp)
**What goes wrong:** `AeroSurfaceStyle.rest()` and `Modifier.aeroGlowRing()` both default `cornerRadius` to `8.dp`; this component's locked corner radius is `4.dp`.
**Why it happens:** easy to call `AeroSurfaceStyle.rest(colors)` without the second argument, or to chain `aeroGlowRing(active, color)` without its third.
**How to avoid:** pass `cornerRadius = 4.dp` explicitly at every one of the (at least) three call sites (both `aeroGlowRing` calls + `aeroSurface`'s shape/style).
**Warning signs:** visually-mismatched corner rounding between the glow ring and the surface fill in the showcase.

### Pitfall 2: Glow-ring / surface modifier ordering reversed
**What goes wrong:** if `aeroSurface(...)` appears before `aeroGlowRing(...)` in the chain, the glow's outer bloom is invisible — `aeroSurface`'s `.clip(shape)` erases the drawing of everything chained after it.
**Why it happens:** the natural reading order ("draw the surface, then the glow on top") is backwards relative to how Compose modifier chaining actually composites.
**How to avoid:** always write `Modifier.aeroGlowRing(...).aeroGlowRing(...).aeroSurface(...)` — glow calls first (outer), surface last (inner).
**Warning signs:** this exact defect was found and fixed at the Phase 16 sign-off (`aeroGlowRing was invisible in the gallery` — STATE.md, Phase 16 Plan 05) — treat any "glow doesn't show up" report as this bug first.

### Pitfall 3: Leftover M3-container scaffolding after the swap
**What goes wrong:** `LocalMinimumInteractiveComponentSize` override and the `graphicsLayer { scaleX = scale; scaleY = scale }` press-shrink are both artifacts of the M3 `Button`/`OutlinedButton` container this phase removes; neither has any effect once that container is gone, but nothing forces their deletion.
**Why it happens:** easy to leave dead imports/composition-locals in place when refactoring around them rather than through them.
**How to avoid:** grep both files for `LocalMinimumInteractiveComponentSize` and `graphicsLayer` after the rewrite — both should be gone entirely (D-02 explicitly requires the scale-shrink removed; the min-interactive-size override has nothing left to constrain).
**Warning signs:** a stray, unused `CompositionLocalProvider` or `animateFloatAsState` for "pressedScale" surviving into the final diff.

### Pitfall 4: Redundant `.hoverable()` call
**What goes wrong:** copying `AeroListItem`'s exact wiring (`.hoverable(interactionSource)` + `.clickable(...)`) onto a button whose `onClick` is required adds a second, unnecessary hover-emission path.
**Why it happens:** CONTEXT.md's canonical refs name `AeroListItem` as *the* hover-wiring precedent; the more precise analog (`AeroIconButton`, same package, no `.hoverable()` needed) isn't named there.
**How to avoid:** use `Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, enabled = enabled, onClick = onClick)` alone — see Pattern 6.
**Warning signs:** two interaction-emitting modifiers in the chain when only one is needed; no functional bug expected, but it's unnecessary code and confuses the next reader about which modifier is "the" hover source.

### Pitfall 5: Missing `role = Role.Button`
**What goes wrong:** `Modifier.clickable(...)` without an explicit `role` still handles clicks and keyboard activation, but the semantics tree won't report the node as a `Button` — a silent accessibility/semantics regression that no visual review would catch.
**Why it happens:** `AeroIconButton`'s existing `.clickable(...)` call (the nearest in-package precedent) does *not* pass `role` — copying it verbatim misses VBTN-04's specific requirement.
**How to avoid:** always include `role = Role.Button` explicitly; write an automated semantics-assertion test (see Validation Architecture) rather than relying on visual review, since this defect produces no visible symptom.
**Warning signs:** a `SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)` assertion failing while `assertHasClickAction()` passes.

### Pitfall 6: Compose Desktop keyboard-input test API not yet compile-proven on this toolchain
**What goes wrong:** `performKeyInput { pressKey(Key.Enter) }`-style test code may not compile identically against the CMP 1.11.1 desktop test artifact as against the Android artifact the training-data examples assume.
**Why it happens:** this exact class of surprise already happened once this milestone — TOOL-07 found `dropShadow`/`innerShadow` living in a different package than 16-RESEARCH.md's (Android-oriented) research had assumed, corrected only by direct bytecode inspection (`javap`) against the real desktop jar.
**How to avoid:** before writing the real VBTN-04 keyboard-activation regression test, do a small compile-proof spike (mirroring `ScratchAeroShadowProof.kt`'s precedent) confirming the exact desktop-target import paths and signature for `performKeyInput`/`pressKey`/`Key.Enter`/`Key.Spacebar` and focus-requesting (`requestFocus()`) on a `SemanticsNodeInteraction`.
**Warning signs:** a compile error citing a missing symbol in `androidx.compose.ui.test.*` for the desktop source set specifically.

### Pitfall 7: Forgetting `indication = null`
**What goes wrong:** without it, `Modifier.clickable(...)` uses `LocalIndication.current` (typically a Material ripple), which visually doubles up with the button's own custom hover-glow/fill-brighten cue.
**Why it happens:** `indication` is easy to omit since it's not required.
**How to avoid:** always pass `indication = null` — precedent explicitly documented in `AeroIconButton.kt`'s own KDoc: "`indication = null` is intentional — hover/pressed states are drawn manually; the M3 ripple is suppressed to avoid a double-effect."
**Warning signs:** a visible ripple/flash on click in addition to the intended press-recess.

## Code Examples

### Verified `clickable` signature and keyboard-activation behavior
```kotlin
// Source: developer.android.com/develop/ui/compose/modifiers-list (CITED)
Modifier.clickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit
)
// "The Spacebar and Enter keys can trigger click events. The clickable modifier
//  intercepts these key events and invokes the onClick() callback."
```

### Verified in-repo precedent for the button's own interaction wiring
```kotlin
// Source: AeroIconButton.kt (read this session) — the pattern to follow for AeroButton/AeroOutlinedButton,
// PLUS role = Role.Button added (AeroIconButton itself omits role — VBTN-04 is a button-specific requirement)
Modifier.clickable(
    enabled = enabled,
    interactionSource = interactionSource,
    indication = null,
    role = Role.Button,   // ADD — not present in AeroIconButton, required by VBTN-04
    onClick = onClick
)
```

### Verified glow-ring / surface ordering
```kotlin
// Source: AeroSurfacePrimitives.kt KDoc (read this session) — USAGE CONTRACT, verbatim
// "apply this modifier OUTSIDE (i.e. before, to the left of) any clipping modifier in the
//  chain — including aeroSurface... Correct ordering is
//  Modifier.aeroGlowRing(...).aeroSurface(...)."
Modifier
    .aeroGlowRing(active = focused && enabled, glowColor = colors.borderSelected, cornerRadius = 4.dp)
    .aeroGlowRing(active = hovered && enabled, glowColor = ornaments.hoverGlow, cornerRadius = 4.dp)
    .aeroSurface(style = currentStyle, shape = RoundedCornerShape(4.dp))
```

### Verified pressed-fill field-swap idiom to extract as a reusable transform
```kotlin
// Source: AeroSurfacePrimitives.kt drawAeroGroove (read this session) — the exact idiom,
// to be lifted into a theme/AeroSurfaceStyle.kt extension fn (Pattern 3), not called directly
// (drawAeroGroove itself also disables gloss and adds a groove-specific shadow cue not needed here)
style.copy(
    fillTop = style.fillBottom,
    fillBottom = style.fillTop,
    bevelLight = style.bevelShadow,
    bevelShadow = style.bevelLight,
    glossAlpha = 0f,
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| M3 `Button`/`OutlinedButton` container + `drawWithContent { drawRect(hoverColor) }` full-bleed overlay | `Modifier.aeroSurface`/`aeroGlowRing` chain, all drawing inside `aeroSurface`'s own `.clip(shape)` | This phase | Fixes VBTN-03's square-corner bleed by construction — the old overlay was drawn *outside* the M3 container's clip |
| `graphicsLayer { scaleX = scale; scaleY = scale }` press feedback (0.97f shrink) | Recessed gradient inversion + inner-shadow rim | This phase (D-02) | Removes a scale-transform animation entirely; replaced by a static style swap, no new animation machinery |
| 2.dp solid `border(colors.borderSelected)` for focus | Persistent `aeroGlowRing` | This phase (D-04) | Focus and hover now share the same visual primitive family (glow), differentiated by token/intensity rather than by a completely different visual language (border vs. glow) |
| Uniform `alpha = 0.4f` for disabled | Geometry-level flatten (gloss/bevel collapse) + muted color | This phase (D-05) | Disabled buttons now read as "dead glass," not merely faded — a qualitatively different visual signal, not just an opacity reduction |

**Deprecated/outdated:**
- `Modifier.border(...)`-based focus indicator for these two components — superseded by `aeroGlowRing`, per D-04.
- `graphicsLayer` scale-shrink press feedback for these two components — superseded by the recessed-gradient idiom, per D-02. (Not deprecated project-wide — `AeroIconButton` still uses it and is out of scope this phase.)

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `performKeyInput { pressKey(Key.Enter) }` / `Key.Spacebar` and `requestFocus()` compile identically on the CMP 1.11.1 desktop test artifact as in Android-oriented training examples | Common Pitfalls #6, Validation Architecture | A VER-04-style keyboard test could fail to compile; mitigated by recommending a compile-proof spike before writing the real test, mirroring the project's own TOOL-07 precedent |
| A2 | The exact shared-surface composable name/param shape (`AeroButtonSurface`, its arguments) is a workable design, not a prescribed one — CONTEXT.md leaves this to Claude's discretion | Architecture Patterns 1 | Low risk — any equivalent one-composable structure satisfies VBTN-06; the name/shape is explicitly non-load-bearing per CONTEXT.md |
| A3 | Placing the pressed-recess transform in `theme/AeroSurfaceStyle.kt` (rather than e.g. a new `theme/AeroButtonBehaviors.kt` file) is the best location for Phase 19 reuse | Architecture Patterns 3 | Low risk — `internal` visibility is module-wide either way; this is a file-organization preference, not a correctness concern |
| A4 | The outlined-transform ratios given in 17-UI-SPEC.md (fill α×0.15, gloss 0.32→0.15, rim 0.6→0.85) are the final calibration, not subject to further tuning at the three-theme review | Architecture Patterns 5 | Low risk — 17-UI-SPEC.md itself frames these as "Claude's discretion... resolved here to a concrete, implementable default," i.e., already reviewed as reasonable defaults, but the actual review is a Phase 17 execution step, not something this research can pre-confirm visually |

**If this table is empty:** N/A — see above; all four items are LOW risk and none blocks planning.

## Open Questions

1. **Does the showcase app's theme-switcher make it straightforward to render all 5 states × 2 variants × 3 themes for the mandatory human sign-off, or does `ButtonsSection.kt` need new interactive demo affordances (e.g., a way to force a "hover"/"focus" preview state without a live mouse) to make press/focus states reviewable in a static screenshot?**
   - What we know: 16-PATTERNS.md flagged this exact question for `PrimitivesSection.kt` and left it "not confirmed in this pass, worth a quick showcase-app-root check during planning."
   - What's unclear: whether Phase 16's showcase work already answered it (Phase 16 shipped after this research's source phase but before Phase 17 planning) — worth a quick check of the current showcase app root during planning, before assuming a new state-preview affordance is needed.
   - Recommendation: planner should do a 5-minute check of the showcase app's current theme-switching mechanism before deciding whether `ButtonsSection.kt` needs new interactive/forced-state demo rows.

2. **Should the pure style-resolver functions (Pattern 7) be `internal` in `components/buttons/` or promoted to `theme/` alongside the pressed-recess transform?**
   - What we know: the pressed-recess transform itself must live in `theme/` for Phase 19 reuse (Pattern 3); the *filled-vs-outlined* and *hover/disabled* transforms are button-specific and don't need to be reachable from Phase 19.
   - What's unclear: whether keeping filled/outlined/hover/disabled resolvers in `components/buttons/` while only the pressed-recess transform lives in `theme/` creates an awkward split, or is the cleanest boundary.
   - Recommendation: keep button-specific-only transforms (filled/outlined/hover/disabled) in `components/buttons/`, and put only the genuinely-cross-phase piece (pressed-recess) in `theme/` — matches the "reusable" scope exactly, doesn't over-promote button-only logic into the shared theme layer.

## Environment Availability

Skipped — this phase depends only on the already-verified Kotlin 2.4.10 / Compose Multiplatform 1.11.1 / Gradle toolchain established and confirmed working in Phase 15 (`./gradlew build` gate passed, STATE.md). No new external tool, service, or runtime is introduced.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | `kotlin.test` (JVM) + `androidx.compose.ui.test` (`ExperimentalTestApi`, v1 `runComposeUiTest` — deliberately kept over the v2 API per TOOL-03/TOOL-04, confirmed still working at CMP 1.11.1) |
| Config file | none dedicated — default Gradle `tasks.test {}` block, `library/build.gradle.kts:44` |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.buttons.*"` |
| Full suite command | `./gradlew :library:test` (232+ tests baseline as of Phase 15/16; must stay green) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| VBTN-01 | Rest-state style resolves to accent-derived fill/bevel + neutral gloss/rim, non-zero gloss/bevel/rim fields | unit (pure fn) | `./gradlew :library:test --tests "*AeroButtonStylesTest*"` | ❌ Wave 0 |
| VBTN-01 | Button renders without exception on all 3 theme presets | integration (`runComposeUiTest`) | `./gradlew :library:test --tests "*AeroButtonTest.rendersWithoutException*"` | ❌ Wave 0 |
| VBTN-02 | Hover/press/disabled style transforms produce the documented deltas (lighten/swap/flatten) at the value level | unit (pure fn) | `./gradlew :library:test --tests "*AeroButtonStylesTest*"` | ❌ Wave 0 |
| VBTN-02 | Three-theme × 5-state visual review reads as genuine Aero (glow/recess/flatten, not just alpha changes) | manual (showcase) | n/a — human sign-off | ❌ Wave 0 (ButtonsSection.kt needs new demo rows) |
| VBTN-03 | Hover-brighten draw call happens inside `aeroSurface`'s own clip; no bespoke unclipped `drawWithContent` overlay remains | unit (source-scan regression guard, mirrors `AeroSurfacePrimitivesTest`'s `functionBody()` pattern) | `./gradlew :library:test --tests "*AeroButtonTest*NoUnclippedOverlay*"` | ❌ Wave 0 |
| VBTN-04 | `Role.Button` semantics present; Space/Enter both invoke `onClick` | integration (`runComposeUiTest` + `performKeyInput`) | `./gradlew :library:test --tests "*AeroButtonTest*KeyboardActivation*"` | ❌ Wave 0 — spike compile-proof first (Pitfall 6) |
| VBTN-05 | Outlined style = filled style transformed by the fixed delta (fill α×0.15, gloss ratio, rim 0.85), for every state | unit (pure fn, parametrized over states) | `./gradlew :library:test --tests "*AeroOutlinedButtonStylesTest*"` | ❌ Wave 0 |
| VBTN-06 | Both `AeroButton.kt` and `AeroOutlinedButton.kt` call the same shared internal surface composable/function name — no duplicated drawing code | unit (source-scan regression guard, mirrors `aeroThumbSurfaceReusesSharedDrawCoreNotAFreshGradientConstructor`) | `./gradlew :library:test --tests "*AeroButtonSurfaceTest*"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests "com.mordred.aero.components.buttons.*"`
- **Per wave merge:** `./gradlew :library:test` (full suite)
- **Phase gate:** full suite green AND three-theme × 5-state human sign-off in the showcase (mandatory — see below) before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` — new, pure-function value-level tests for VBTN-01/02 style resolution (no Compose runtime needed)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt` — new, VBTN-05 delta-transform tests
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt` — new, `runComposeUiTest`-based render/semantics/keyboard tests for VBTN-01/03/04
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceTest.kt` — new, VBTN-06 source-scan regression guard (both public buttons call the one shared internal composable)
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — extend existing rows (currently only enabled/disabled shown, per current read) to exercise all 5 states × both variants for the mandatory three-theme sign-off
- [ ] Compile-proof spike for `performKeyInput`/`pressKey`/`Key.Enter`/`Key.Spacebar` against the CMP 1.11.1 desktop test artifact (mirrors `ScratchAeroShadowProof.kt`'s precedent) — do this BEFORE writing the real VBTN-04 keyboard test, per Pitfall 6

**Repro-must-exercise-the-path requirement (project lesson, v2.0.3/v2.0.4):** the VBTN-03 (no-unclipped-overlay) and VBTN-06 (shared-surface) regression guards above must each be proven to actually fail against the *current* (pre-fix) `AeroButton.kt`/`AeroOutlinedButton.kt` source (or a deliberately-reintroduced duplicate-painter/unclipped-overlay) before being counted as real gates — a guard that would pass against both the broken and fixed code is not a guard, per this project's own documented v2.0.3 false-positive-sign-off lesson (TOOL-04/VER-06 precedent).

## Security Domain

**Mostly not applicable** — `AeroButton`/`AeroOutlinedButton` are stateless, non-authenticating, non-data-persisting UI controls; there is no network call, credential handling, or stored-data surface in this phase's scope.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-------------------|
| V2 Authentication | No | N/A — no auth surface |
| V3 Session Management | No | N/A |
| V4 Access Control | No | N/A |
| V5 Input Validation | No | The only "input" is a caller-supplied `text: String` label rendered via Compose `Text()` — Compose text rendering is not an injection vector (no HTML/markup interpretation); `maxLines`/`overflow` govern layout, not validation |
| V6 Cryptography | No | N/A |

### Known Threat Patterns for this stack
None applicable to this phase's scope. The one accessibility-adjacent concern (missing `Role.Button` degrading screen-reader semantics, Pitfall 5) is tracked as a functional/testing concern in Common Pitfalls and Validation Architecture rather than a security threat.

## Sources

### Primary (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — read directly this session (drawAeroSurfaceCore, aeroSurface, aeroGlowRing, aeroThumbSurface, aeroGroove, all ordering/pitfall KDoc)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` — read directly this session
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` — read directly this session (confirms Pattern 2's finding)
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — read directly this session
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt`, `AeroOutlinedButton.kt`, `AeroIconButton.kt` — read directly this session (current implementation + the more-precise interaction-wiring precedent)
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — read directly this session
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt` — read directly this session (test-framework/regression-guard conventions)
- `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt` — read directly this session (runComposeUiTest v1 precedent, TOOL-04 re-proof convention)
- `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` — read directly this session
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — read directly this session
- Context7 `/websites/developer_android_develop_ui_compose` — `Modifier.clickable` signature, Space/Enter keyboard-activation behavior, hover/focus/press interaction-source semantics — fetched this session

### Secondary (MEDIUM confidence)
- `.planning/phases/17-buttons/17-CONTEXT.md`, `17-UI-SPEC.md` — locked decisions and pre-computed per-state values, treated as authoritative for *values*, cross-checked against actual source for *mechanism*
- `.planning/STATE.md`, `.planning/PROJECT.md` — institutional-memory precedents (v2.0.2 `AeroPanelGroupImpl`, v2.0.2 `PanelDistribution.kt`, v2.0.3/v2.0.4 false-positive lesson, Phase 16 sign-off findings)

### Tertiary (LOW confidence)
- None — every claim in this document is either read directly from source this session, cited from official docs, or explicitly logged in the Assumptions table above.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; all consumed primitives read directly from shipped source.
- Architecture: HIGH — patterns derived from direct source reads of the exact files this phase touches/consumes, plus one officially-cited correction (Pattern 6) to a stale precedent pointer in CONTEXT.md.
- Pitfalls: HIGH — all 7 pitfalls are either directly observed in source (defaults mismatch, ordering rule, dead scaffolding) or drawn from this project's own documented incident history (Phase 16 glow-ring bug, TOOL-07 shadow-package surprise).
- Validation architecture: MEDIUM — test framework/commands verified against actual `build.gradle.kts`/existing test files; the exact keyboard-input test API surface is flagged LOW/ASSUMED pending a compile-proof spike (A1).

**Research date:** 2026-07-23
**Valid until:** 30 days (stable — this phase consumes an already-shipped, three-theme-reviewed primitives layer; the only stale-risk item is the CMP 1.11.1 desktop test-API surface, mitigated by the recommended spike)
