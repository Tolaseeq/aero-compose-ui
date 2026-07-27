# Phase 19: Selectors + Lists - Research

**Researched:** 2026-07-27
**Domain:** Internal Compose Desktop UI restyle — consuming Phase 16/17/18 Aero primitives/resolvers to restyle `AeroSwitch`, `AeroSegmentedControl`, `AeroListItem`. Zero new external dependencies; zero new frameworks. This is a codebase-continuity phase, not a greenfield one.
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Accent color lives in the switch's track groove; the thumb stays a neutral raised glass nub (continues Phase 18 D-01's slider identity).
- **D-02:** The off-state groove uses identical geometry to the checked groove (`drawAeroGroove`/`Modifier.aeroGroove`) — only fill color animates at 150ms; one moving part.
- **D-03:** The raised thumb is drawn on top of, and outside, the track's own `.clip(shape)` — its drop shadow/glow ring are not clipped by the 18dp-tall track. Layout size stays exactly 36×18dp.
- **D-04:** `AeroSwitch` and `AeroSegmentedControl` each gain a trailing `interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }` parameter — additive, source-compatible, matching `AeroListItem`'s existing convention.
- **D-05:** Press keeps the switch thumb raised with a brighter gloss (`PRESSED_GLOSS_BOOST` idiom from `resolveSliderThumbStyle`), NOT `pressedRecess`.
- **D-06:** Hover puts `aeroGlowRing` on the whole switch track (the `toggleable` hot zone) plus `hoverLighten()` on the thumb. Focus stays the persistent `aeroGlowRing` from Phase 17 D-04, using a distinct token (`colors.borderSelected`) from hover's `hoverGlow`.
- **D-07:** Semantic transitions (`checked`/`selected`) keep their 150ms `tween`; `hover`/`press`/`focus` resolve to a style with NO animation, exactly as `resolveButtonStyle`/`resolveSliderThumbStyle` already do. Never animate a whole `AeroSurfaceStyle`.
- **D-08:** Unselected `AeroSegmentedControl` segments are raised glass (`AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)`, same base `resolveButtonStyle` starts from); the selected segment is that same base transformed by `.pressedRecess(PRESSED_INNER_SHADOW)`, imported verbatim from `AeroButtonSurface.kt` cross-package.
- **D-09:** Each segment is individually focusable via `Modifier.selectable(selected, role = Role.RadioButton, ...)` — Tab reaches every segment; N Tab stops accepted as trade-off. Replaces the bare `.clickable` with no role.
- **D-10:** Segment hover/focus render entirely in-bounds (hover = `hoverLighten()` on fill; focus = inner rim/inner glow) — NO `aeroGlowRing` on individual segments (outer bloom would be sliced by the outer `Row.clip(shape)` and bleed onto neighbours).
- **D-11:** `AeroListItem` hover and selection compose as TRANSFORMS, never `when`-branches — base resolves from `selected` first, then `hoverLighten()` applies on top of whichever base was chosen. Structurally makes VLST-02 impossible rather than color-picking around it. The existing `when { selected -> …; hovered -> …; else -> … }` at `AeroListItem.kt:60-64` is the anti-pattern being removed.
- **D-12:** Hover highlight uses the SAME pill geometry as the selection highlight — one shape, two style states.
- **D-13:** `AeroListItem`'s focus visual renders within the row's own bounds (inner rim/brightened pill contour), not an outer `aeroGlowRing` — list rows live inside scrolling/clipping containers. Row is focusable only when `onClick != null`.

### Claude's Discretion

- Selection-pill geometry (inset/corner radius) — UI-SPEC resolved default: 2.dp vertical / 0.dp horizontal inset, 6.dp corner radius (full-width pill); four-side inset is the documented fallback if the review finds full-width reads flat.
- The recessed segment's color — UI-SPEC resolved default: accent-recessed glass (`rest(colors,...).pressedRecess(...)`), text switches to a light/neutral tone on the selected segment.
- Native `dropShadow`/`innerShadow` vs. manual gradient per depth cue — decided on the three-theme review, per Phase 16 D-02 precedent (`ScratchAeroShadowProof.kt` has the proven signatures).
- Exact groove depth / thumb gloss magnitude / how far the thumb's shadow may exceed 36×18dp.
- Exact hover-vs-focus differentiation per component so both read distinguishable when co-active (mirrors 17 D-03/D-04).
- Whether raised/recessed segment styles are new resolver functions or `resolveButtonStyle` with a parameter — constraint: one shared painter, not two forks.
- Exact `flattenDisabled` values for all three components (mechanism is locked: `flattenDisabled`).
- Fate of the 1dp segment separators — UI-SPEC resolved default: dropped, each segment's own contour supplies the break.

### Deferred Ideas (OUT OF SCOPE)

- Arrow-key roving focus for `AeroSegmentedControl` (one Tab stop, ←/→ change selection) — rejected for this phase (D-09); behavior change, not visual; candidate future phase.
- `AeroRangeSlider` accessibility semantics + keyboard support — still open from Phase 18, unrelated to this phase.
- `AeroListItem` bottom mirror reflection — ruled out of scope at milestone level (`STATE.md` line 92 / `VLST-F01`).
- `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` Aero pass — outside the eight-target set.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| VSEL-01 | `AeroSwitch` — track groove + raised glossy/shadowed thumb | `Modifier.aeroGroove`/`drawAeroGroove` (PRIM-08) and `Modifier.aeroThumbSurface`/`drawAeroThumb` (PRIM-07) in `AeroSurfacePrimitives.kt` are the direct primitives; `AeroSlider.kt`'s thumb/track slot wiring is the layering precedent (see Code Examples). |
| VSEL-02 | `AeroSwitch` — hover/press/focus for the first time | `resolveSliderThumbStyle` (`AeroSlider.kt:244-259`) is the exact precedence-chain model (D-05); `rememberAeroInteractionState` + `aeroGlowRing` supply hover/focus wiring (see Code Examples, Common Pitfalls #1/#6). |
| VSEL-03 | `AeroSegmentedControl` — selected segment recessed via reused pressed-button code | `AeroSurfaceStyle.pressedRecess(PRESSED_INNER_SHADOW)` (`AeroSurfaceStyle.kt:126-133`) plus `PRESSED_INNER_SHADOW` (`AeroButtonSurface.kt:122-126`) — imported cross-package verbatim, not re-derived. |
| VSEL-04 | `AeroSegmentedControl` — hover and focus for the first time | `Modifier.selectable(role = Role.RadioButton, ...)` already proven in-repo (`AeroRadioButton.kt:59-64`); in-bounds hover/focus per D-10 (Common Pitfalls #4). |
| VLST-01 | `AeroListItem` — selection clipped to rounded pill w/ gradient+rim | `Modifier.aeroSurface(style, RoundedCornerShape(6.dp))` on an inset `Box`, replacing the current unclipped `.background(animatedBg)` (`AeroListItem.kt:73`). |
| VLST-02 | Hover visible on selected row — combine, don't suppress | D-11's transform-composition shape (see Code Examples "Composition order"); this is the one requirement with a genuinely pure-function unit test (see Validation Architecture). |
| VLST-03 | `AeroListItem` — focus visual | In-bounds inset `drawRoundRect` stroke at `colors.borderSelected`, gated on `onClick != null` (row is only focusable when clickable). |
| VLST-04 | New hover-wired components copy `AeroListItem`'s `hoverable`+`collectIsHoveredAsState` pattern | `AeroListItem.kt:74` (`Modifier.hoverable(interactionSource)`) + `rememberAeroInteractionState` (`InteractionStates.kt:63-69`, which wraps `collectIsHoveredAsState` internally) — mandatory pattern, never pointer-position tracking (contrast with `AeroRangeSlider`'s Canvas-forced `pointerInput` hack, which does NOT apply here since `AeroSwitch`/`AeroSegmentedControl` are Box/Row-based, not Canvas-based). |
</phase_requirements>

## Summary

This phase has almost no external unknowns — every primitive, transform, and precedence-chain pattern it needs already exists, shipped, and is proven on three themes by Phases 16-18. The work is disciplined reuse, not invention: `AeroSwitch`'s groove/thumb come straight from `PRIM-07`/`PRIM-08`; its press behavior is a literal copy of `resolveSliderThumbStyle`'s "stays raised, brighter gloss" idiom; `AeroSegmentedControl`'s recessed selection is `AeroButtonSurface.kt`'s `pressedRecess` transform imported cross-package (the KDoc there already names this phase as the consumer); `AeroListItem`'s hover/selection composition is the same base-then-transform shape `resolveButtonStyle` already uses for hover-on-top-of-pressed.

The one genuinely load-bearing piece of NEW logic is D-11's structural fix for `AeroListItem`: the current `when { selected -> …; hovered -> …; else -> … }` branch (lines 60-64) must become `base = if (selected) selectedStyle else null; resolved = if (hovered) (base ?: neutralHoverStyle).hoverLighten() else base`. This is a pure, unit-testable function — the single highest-value automated check this phase can add (VLST-02).

Three clip-ordering/in-bounds rules recur across all three components and are the most common way this phase could regress: (1) `aeroGlowRing` MUST precede any `.clip()`-applying primitive in a modifier chain (`AeroSurfacePrimitives.kt` KDoc, violated = the bloom is silently erased); (2) the switch thumb must be a Box SIBLING drawn after the groove Box, not nested inside it, so its shadow/glow aren't clipped by the 18dp track (D-03); (3) segmented-control and list-item focus/hover must stay in-bounds (inner rim, not `aeroGlowRing`) because both live inside an outer `.clip()` that would slice an outer bloom (D-10/D-13) — this is a deliberate, documented EXCEPTION to the library's glow language, not an oversight, and the planner should make sure the code comments explaining the exception land, so a future reviewer doesn't "fix" it back to `aeroGlowRing`.

**Primary recommendation:** Build each component's own pure `resolveXStyle(...)` function mirroring `resolveButtonStyle`/`resolveSliderThumbStyle`'s shape (Compose-free, disabled-wins precedence chain), wire it through the existing Phase 16 primitives with the exact modifier-chain ordering documented below, and add the `indication = null` parameter to every new `.toggleable`/`.selectable`/`.clickable` call so the platform's default ripple indication does not visually double up with the new custom-painted hover/press cues (see Common Pitfalls #7 — a real gap found during this research, not covered by any locked decision).

## Architectural Responsibility Map

This is a single-module Compose Desktop UI library, not a multi-tier web app — "tiers" here map to the codebase's own layering (public component API → pure style resolvers → shared draw primitives → showcase demo), not Browser/Server/CDN.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| `AeroSwitch` groove/thumb geometry (fill, gloss, bevel, rim) | Primitives/Draw layer (`theme/AeroSurfacePrimitives.kt`) | Public Component API (`AeroSwitch.kt` wires the modifiers) | `aeroGroove`/`aeroThumbSurface` already own all gradient/bevel/rim authoring (PRIM-05/07/08 prohibition on bespoke gradients) |
| `AeroSwitch` hover/press/focus style resolution | Style Resolver layer (new `resolveSwitchThumbStyle`/`resolveSwitchGrooveStyle`, likely in `AeroSwitch.kt` itself, mirroring `AeroSlider.kt`'s in-file resolver placement) | — | Pure, Compose-free function — same architectural slot as `resolveSliderThumbStyle`/`resolveButtonStyle` |
| `AeroSegmentedControl` recessed-selection fill | Style Resolver layer (new segment resolver, reusing `pressedRecess` from `theme/AeroSurfaceStyle.kt`) | Public Component API | `pressedRecess` is cross-package by design (placed in `theme/`, not `components/buttons/`, specifically for this reuse) |
| `AeroSegmentedControl` per-segment focus/hover semantics | Public Component API (`Modifier.selectable(role = Role.RadioButton)`) | — | Semantics/interaction wiring belongs at the component-API layer, not the draw layer |
| `AeroListItem` pill clip + hover/selection composition | Style Resolver layer (new composition function implementing D-11) | Primitives/Draw layer (`aeroSurface` for the pill) | The composition LOGIC (base-then-transform) is resolver-layer; the actual pill paint is primitives-layer |
| Interaction-state collection (hover/press/focus booleans) | Shared common layer (`components/common/InteractionStates.kt`) | — | Already exists (`rememberAeroInteractionState`), consumed identically by all three components — no new abstraction needed |
| Showcase demo wiring (state-matrix rows, three-theme review surface) | Showcase/Demo layer (`showcase/.../sections/SelectionSection.kt`, `ListSection.kt`) | — | Presentation-only; must exercise every state × theme combination per Phase 17/18 precedent (17-04/18-04) |

## Standard Stack

No new external dependencies. This phase consumes only APIs already declared and pinned by Phase 15/16/17/18.

### Core (already present, reused verbatim)
| Library / API | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose Multiplatform Foundation | 1.11.1 (locked Phase 15) [VERIFIED: codebase — `AeroSlider.kt`/`AeroRangeSlider.kt` already compile against this toolchain] | `Modifier.hoverable`, `Modifier.toggleable`, `Modifier.selectable`, `MutableInteractionSource`, `collectIsHoveredAsState`/`collectIsPressedAsState`/`collectIsFocusedAsState` | Already the project's pinned toolchain; no alternative under consideration |
| Compose Material3 | pinned stable (`material3-desktop-1.9.0` per `AeroSlider.kt:41` comment) [CITED: in-repo code comment, TOOL-02] | Not consumed by any of the three Phase 19 components directly (unlike `AeroSlider`, none of `AeroSwitch`/`AeroSegmentedControl`/`AeroListItem` wrap an M3 container) | N/A for this phase — confirms these three components have NO M3 dependency to preserve or remove |
| Kotlin | 2.4.10 (locked Phase 15) [VERIFIED: codebase] | Language/stdlib | Project-wide pin |
| `androidx.compose.ui.draw.dropShadow`/`innerShadow` + `androidx.compose.ui.graphics.shadow.Shadow` | 1.11.1 API surface, package corrected in Phase 15 TOOL-07 [VERIFIED: codebase — `AeroButtonSurface.kt` imports `androidx.compose.ui.graphics.shadow.Shadow`, `AeroSurfaceStyle.kt` same] | `PRESSED_INNER_SHADOW`'s native `Shadow` type, reused verbatim for the segmented control's recess | Confirmed compiling in this exact codebase already — no new spike needed this phase |

### Supporting (internal, this-repo-only "libraries")
| Function/Module | Location | Purpose | When to Use |
|---------|---------|---------|-------------|
| `AeroSurfaceStyle.rest(colors, cornerRadius)` | `theme/AeroSurfaceStyle.kt:49` | Accent-capable raised-glass rest style | `AeroSwitch` checked groove target color, `AeroSegmentedControl` unselected/selected-pre-recess base |
| `AeroSurfaceStyle.neutralRest(colors, cornerRadius)` | `theme/AeroSurfaceStyle.kt:83` | Neutral-glass rest style (Phase 18 factory) | `AeroSwitch` thumb (always), `AeroSwitch` off-groove |
| `pressedRecess(innerShadow)` | `theme/AeroSurfaceStyle.kt:126` | Inverts fill/bevel, disables gloss, attaches inner shadow | `AeroSegmentedControl` selected segment (VSEL-03) — the exact reuse target named in this function's own KDoc |
| `hoverLighten()` | `theme/AeroSurfaceStyle.kt:177` | Brightens both fill stops by 0.08 via RGB-mix | All three components' hover state |
| `flattenDisabled(colors)` | `theme/AeroSurfaceStyle.kt:156` | Flattens fill/bevel/gloss/rim to "dead" | All three components' disabled state, replacing each's current uniform `alpha = 0.4f` |
| `Modifier.aeroGroove(style)` / `drawAeroGroove` | `theme/AeroSurfacePrimitives.kt:271-312` (PRIM-08) | Recessed track-groove primitive | `AeroSwitch` track |
| `Modifier.aeroThumbSurface(style)` / `drawAeroThumb` | `theme/AeroSurfacePrimitives.kt:228-251` (PRIM-07) | Raised-thumb primitive (circle special-case) | `AeroSwitch` thumb |
| `Modifier.aeroSurface(style, shape)` | `theme/AeroSurfacePrimitives.kt:108-115` | General raised/recessed surface for Box-owning components | `AeroSegmentedControl` per-segment fill, `AeroListItem` selection/hover pill |
| `Modifier.aeroGlowRing(active, glowColor, cornerRadius)` | `theme/AeroSurfacePrimitives.kt:176-180` (PRIM-06) | Hover/focus outer bloom | `AeroSwitch` track hover+focus ONLY (never the segmented control or list item, per D-10/D-13) |
| `rememberAeroInteractionState(source)` | `components/common/InteractionStates.kt:63-69` | Bundles hovered/pressed/focused booleans | All three components, replacing any bespoke collection |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `Modifier.selectable(role = Role.RadioButton)` per segment (D-09, locked) | One focus stop + hand-rolled `onKeyEvent` arrow-key roving | Rejected — behavior change beyond visual-only milestone scope; deferred idea for a future phase |
| `pressedRecess` reused cross-package (D-08/VSEL-03, locked) | A second, independently-authored recessed-segment gradient | Rejected — would fork the two paths the `AeroButtonSurface.kt` KDoc explicitly promises stay unified |
| In-bounds inner-rim focus for segmented control/list item (D-10/D-13, locked) | `aeroGlowRing` outer bloom everywhere for consistency | Rejected — outer bloom would be sliced by the parent's own `.clip()` and bleed onto neighbouring segments/rows |

**Installation:** None — no `build.gradle.kts` changes required. All consumed symbols already resolve within `library`'s existing module graph.

**Version verification:** N/A — no new package versions to verify. Confirmed via direct source read (not `npm view`/`pip` equivalents, since this is a JVM/Gradle single-module project with no new coordinates) that every consumed function/type already exists in the current source tree at the paths cited above.

## Package Legitimacy Audit

**Not applicable.** This phase introduces zero new external packages, Gradle coordinates, or npm-equivalent dependencies. Every API surface consumed (Compose Foundation `hoverable`/`toggleable`/`selectable`, Compose UI `dropShadow`/`innerShadow`/`Shadow`) is already declared, pinned, and compiling in this codebase as of Phase 15/16/17/18. The Package Legitimacy Gate protocol (registry check, postinstall-script scan) is skipped — there is nothing to check against a registry.

## Architecture Patterns

### System Architecture Diagram

```
Caller (showcase / consumer app)
        │
        │  checked / selected / onCheckedChange / onSelect / interactionSource
        ▼
┌─────────────────────────────────────────────────────────────┐
│ Public Component API layer                                  │
│  AeroSwitch(...)  AeroSegmentedControl(...)  AeroListItem(...)│
│                                                               │
│  - Modifier.toggleable(role=Switch, interactionSource, ...)  │
│  - Modifier.selectable(role=RadioButton, interactionSource)  │
│  - Modifier.clickable(onClick, ...) [AeroListItem, if set]   │
│  - Modifier.hoverable(interactionSource) [wraps toggleable/  │
│    selectable's own hover reporting is NOT automatic — see   │
│    Common Pitfall #6]                                        │
└───────────────┬───────────────────────────────────────────┘
                │  interactionSource
                ▼
┌─────────────────────────────────────────────────────────────┐
│ Shared common layer                                          │
│  rememberAeroInteractionState(source)                        │
│    -> AeroInteractionState(hovered, pressed, focused)         │
│  (components/common/InteractionStates.kt — PRIM-15)          │
└───────────────┬───────────────────────────────────────────┘
                │  hovered/pressed/focused booleans + checked/selected + enabled
                ▼
┌─────────────────────────────────────────────────────────────┐
│ Style Resolver layer (NEW this phase, pure functions)         │
│  resolveSwitchThumbStyle / resolveSwitchGrooveStyle           │
│  resolveSegmentStyle (unselected/selected × hover)             │
│  resolveListItemStyle (D-11 base-then-transform composition)  │
│                                                                │
│  precedence: disabled wins > pressed > hovered > rest          │
│  (mirrors resolveButtonStyle / resolveSliderThumbStyle)        │
└───────────────┬───────────────────────────────────────────┘
                │  AeroSurfaceStyle (fillTop/Bottom, gloss, bevel, rim, shadows)
                ▼
┌─────────────────────────────────────────────────────────────┐
│ Primitives/Draw layer (Phase 16, UNCHANGED this phase)        │
│  drawAeroSurfaceCore / Modifier.aeroSurface                    │
│  drawAeroThumb / Modifier.aeroThumbSurface (PRIM-07)           │
│  drawAeroGroove / Modifier.aeroGroove (PRIM-08)                │
│  drawAeroGlowRing / Modifier.aeroGlowRing (PRIM-06)            │
└───────────────┬───────────────────────────────────────────┘
                │  painted pixels
                ▼
        Showcase (SelectionSection.kt / ListSection.kt)
        — state-matrix demo rows × 3 themes, human sign-off
```

**Reading the primary use case (AeroSwitch hover→press→check):** caller taps the switch → `toggleable`'s `interactionSource` emits Press/Release → `rememberAeroInteractionState` surfaces `pressed=true` → `resolveSwitchThumbStyle` returns the gloss-boosted (not recessed) style per D-05 → `Modifier.aeroThumbSurface` paints it → on release, `onCheckedChange` fires → `checked` flips → the (separately-owned) 150ms `animateColorAsState`/equivalent progress animates the groove's `AeroSurfaceStyle.rest`-derived accent fill in per D-02/D-07.

### Recommended Project Structure

No new files/folders — this phase edits three existing component files in place, adds resolver functions likely co-located in each component's own file (mirroring `AeroSlider.kt`'s in-file `resolveSliderThumbStyle` placement, NOT a separate file per component, unless the planner decides a shared file is cleaner given three resolvers land in the same wave):

```
library/src/main/kotlin/com/mordred/aero/
├── components/selection/
│   ├── AeroSwitch.kt              # MODIFIED — groove+thumb, 3 states, resolver added in-file
│   └── AeroSegmentedControl.kt    # MODIFIED — raised/recessed segments, selectable, resolver added in-file
├── components/list/
│   └── AeroListItem.kt            # MODIFIED — pill clip, D-11 composition, resolver added in-file
└── theme/
    ├── AeroSurfaceStyle.kt        # UNCHANGED — pressedRecess/hoverLighten/flattenDisabled already exist
    └── AeroSurfacePrimitives.kt   # UNCHANGED — all primitives already exist

showcase/src/main/kotlin/com/mordred/showcase/sections/
├── SelectionSection.kt            # MODIFIED — state-matrix rows for AeroSwitch/AeroSegmentedControl
└── ListSection.kt                 # MODIFIED — state-matrix rows for AeroListItem
```

### Pattern 1: Disabled-wins precedence-chain resolver (mirrors `resolveButtonStyle`/`resolveSliderThumbStyle`)

**What:** A pure, Compose-free function taking `colors`, interaction booleans, and `enabled`, returning a resolved `AeroSurfaceStyle`.
**When to use:** Every new per-state resolution point this phase adds (switch thumb, switch groove, segment fill, list-item pill).
**Example:**
```kotlin
// Source: library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt:244-259 (existing, proven)
internal fun resolveSliderThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    isDragging: Boolean,
    focused: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = THUMB_RADIUS)
    if (!enabled) return rest.flattenDisabled(colors)
    return when {
        pressed || isDragging -> rest.copy(glossAlpha = rest.glossAlpha + PRESSED_GLOSS_BOOST)
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
```
The switch thumb resolver (VSEL-02/D-05) should be near-identical to this, swapping `isDragging` for nothing (switches don't drag) and using `neutralRest(colors, cornerRadius = 7.dp)` (14dp thumb → 7dp radius per UI-SPEC).

### Pattern 2: D-11 composition (base-then-transform, never `when`-branch)

**What:** Resolve a nullable base style from ONE state axis (`selected`) first, then unconditionally apply a transform (`hoverLighten()`) from the SECOND axis (`hovered`) on top of whatever base resulted — including the "no pill" case.
**When to use:** `AeroListItem`'s pill resolution (VLST-02) — this is the structural fix for the bug at `AeroListItem.kt:60-64`.
**Example:**
```kotlin
// Source: 19-UI-SPEC.md "Composition order, structurally (D-11)" — pseudocode to implement verbatim
val base: AeroSurfaceStyle? = if (selected) {
    AeroSurfaceStyle.rest(colors, cornerRadius = 6.dp).let { s ->
        s.copy(fillTop = s.fillTop.copy(alpha = s.fillTop.alpha * 0.5f), fillBottom = s.fillBottom.copy(alpha = s.fillBottom.alpha * 0.5f))
    }
} else null

val resolved: AeroSurfaceStyle? = if (hovered && enabled) {
    (base ?: AeroSurfaceStyle.neutralRest(colors, cornerRadius = 6.dp)).hoverLighten()
} else base
// resolved == null -> no pill Box/aeroSurface call at all (Rest-unselected row)
// resolved != null -> Modifier.aeroSurface(resolved, RoundedCornerShape(6.dp)) on the inset pill Box
```
Note the anti-pattern this replaces (`AeroListItem.kt:59-64`, current):
```kotlin
// CURRENT — the exact anti-pattern D-11 removes. selected suppresses hover entirely (VLST-02 bug).
val animatedBg by animateColorAsState(
    targetValue = when {
        selected -> colors.primary.copy(alpha = 0.2f)
        hovered && enabled -> colors.buttonHover
        else -> Color.Transparent   // PRIM-14 anti-pattern too — also being removed
    },
    ...
)
```

### Pattern 3: Thumb-as-sibling-outside-clip layering (D-03)

**What:** The switch's raised thumb `Box` (its own `aeroGlowRing` + `aeroThumbSurface` chain) is a SIBLING of the groove `Box`, positioned via `.offset`, NOT a child nested inside the groove's own clipped bounds.
**When to use:** `AeroSwitch`'s thumb, so its shadow/glow are not sliced by the 18dp-tall track's `.clip(shape)`.
**Example:**
```kotlin
// Source: AeroSlider.kt's M3 thumb=/track= slot separation is the closest existing precedent —
// M3's Slider already keeps thumb and track as independent composables, never one nested in the
// other's clip. AeroSwitch must replicate this at the Box level since it owns its own layout
// (no M3 container to supply the separation for free):
Box(modifier = /* toggleable, sized 36x18, NO groove clip leaking into thumb draw */) {
    Box(modifier = Modifier.aeroGroove(grooveStyle) /* clips itself internally via aeroGroove's own .clip */)
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset { /* existing x animation */ }
            .size(14.dp)
            .aeroGlowRing(active = hovered && enabled, glowColor = hoverGlow, cornerRadius = 7.dp)
            .aeroThumbSurface(thumbStyle)   // this Box's OWN .clip(CircleShape) is local to it —
                                             // it does NOT inherit the groove Box's clip because
                                             // it's a sibling, not a child of that Box.
    )
}
```
Critical correction to the current code: `AeroSwitch.kt`'s existing structure ALREADY has the thumb as an inner `Box` of the outer `Box` (not nested inside a groove-owning child) — so the sibling relationship largely already exists structurally (outer `Box` = track + toggleable hot zone, inner `Box` = thumb). The planner's job is to make sure the NEW groove-drawing call attaches to the OUTER box (or a groove-only inner box that itself does NOT wrap the thumb), and that the thumb's own `aeroGlowRing`+`aeroThumbSurface` chain is unclipped by whichever box carries the groove's `.clip(shape)`.

### Pattern 4: Cross-package pure-function reuse (`pressedRecess`)

**What:** Import an `internal` function from `theme/` into `components/selection/` — already proven safe because Kotlin `internal` is module-wide (confirmed by Phase 16's `InteractionStates.kt` relocation, which needed zero visibility changes for the same reason).
**When to use:** `AeroSegmentedControl`'s recessed selected segment (VSEL-03).
**Example:**
```kotlin
// Source: library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt:126-133
internal fun AeroSurfaceStyle.pressedRecess(innerShadow: Shadow): AeroSurfaceStyle = copy(
    fillTop = fillBottom, fillBottom = fillTop,
    bevelLight = bevelShadow, bevelShadow = bevelLight,
    glossAlpha = 0f, innerShadow = innerShadow,
)
// AeroSegmentedControl.kt import:
import com.mordred.aero.theme.pressedRecess
// AeroButtonSurface.kt's PRESSED_INNER_SHADOW is currently `private` — the planner must either
// (a) make it internal/public and import it, or (b) redeclare an identical constant in
// AeroSegmentedControl.kt with the EXACT same Shadow(radius=2.dp, color=Black@0.35f, offset=(0,1.dp))
// values, per UI-SPEC's explicit "reused verbatim, not a new one" requirement. Option (a) is
// cleaner and avoids value drift; flag this as a concrete Wave-0 task.
```

### Anti-Patterns to Avoid

- **`when`-branch selection for combined interaction states (D-11's target bug):** Any `when { selected -> ...; hovered -> ...; else -> ... }` shape where one state can fully suppress another. Replace with base-then-transform composition (Pattern 2).
- **`aeroSurface`/`aeroGroove`/`aeroThumbSurface` before `aeroGlowRing` in a modifier chain:** Silently erases the outer bloom — `aeroSurface`'s internal `.clip(shape)` clips everything drawn after it, and `.aeroGlowRing()` chained AFTER `.aeroSurface()` draws AFTER the clip already applied to prior modifiers, but Compose modifier chains apply draw effects in the order they're listed for that specific node — the KDoc in `AeroSurfacePrimitives.kt` states this explicitly: `Modifier.aeroSurface(...).aeroGlowRing(...)` is WRONG; `Modifier.aeroGlowRing(...).aeroSurface(...)` is correct.
- **`aeroGlowRing` on segmented-control segments or list-item rows:** Both live inside a parent `.clip()` (outer `Row.clip(shape)` for segments, potential `LazyColumn` clipping for rows) — an outer bloom gets sliced by the frame and/or bleeds onto neighbours (D-10/D-13's documented exception).
- **Re-deriving a second recessed-gradient implementation for the segmented control:** Must reuse `pressedRecess` verbatim (D-08/VSEL-03) — a parallel implementation is exactly the drift `AeroButtonSurface.kt`'s KDoc warns against.
- **Independently `.toPx()`-deriving a corner radius that doesn't match the component's own clip shape (16-RESEARCH.md Pitfall 5, still live):** Every `cornerPx` passed into a `draw*` function or `cornerRadius` passed into an `aero*` modifier must come from the SAME source as the component's own clip/shape corner value.
- **Animating a whole `AeroSurfaceStyle` (D-07's explicit prohibition):** Hover/press/focus resolve instantly; only `checked`/`selected` (the semantic transitions) keep their existing 150ms `tween`. Animating a whole style object would fight the `drawWithCache` perf baseline (PRIM-13) since brushes would need rebuilding every animation frame.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Recessed/inverted-gradient surface | A new manual gradient-inversion function for the segmented control | `AeroSurfaceStyle.pressedRecess(innerShadow)` (`theme/AeroSurfaceStyle.kt:126`) | Already shipped, three-theme-proven on `AeroButton`'s press state; VSEL-03 explicitly requires reuse, not re-derivation |
| Hover/press/focus boolean collection | Custom `mutableStateOf` + manual `PointerEvent` inspection | `rememberAeroInteractionState(interactionSource)` (`components/common/InteractionStates.kt:63`) | Already wraps `collectIsHoveredAsState`/`collectIsPressedAsState`/`collectIsFocusedAsState` correctly; VLST-04 explicitly forbids inventing pointer-position tracking |
| Glow/bloom halo for hover or focus | A `Canvas` + blur/`RenderEffect` | `Modifier.aeroGlowRing(active, glowColor, cornerRadius)` (PRIM-06) | Deliberately a zero-extra-composited-layer, zero-Skia-coupling approximation; a real blur was rejected in Phase 16's own research |
| Disabled-state dimming | `.alpha(0.4f)` (the CURRENT code in all three target files) | `flattenDisabled(colors)` | Alpha-only fading is invisible on `AeroColorScheme.Classic`'s fully-opaque tokens in some contexts and doesn't "flatten the geometry" the way the rest of the milestone's disabled treatment does (17-05/18-07 precedent) |
| Radio-group/single-select semantics for the segmented control | Bare `.clickable` (current code) with manually-tracked "which one is selected" | `Modifier.selectable(selected, role = Role.RadioButton, onClick = ...)` — proven working in-repo already at `AeroRadioButton.kt:59-64` | Semantics (Tab focus, Space/Enter activation, `Role.RadioButton` accessibility) come from the platform for free; VBTN-04's "no zero-semantics repeat" lesson applies equally here (D-09) |
| Selection-highlight clipping | A second, independent rounded-rect draw call inside `AeroListItem` | `Modifier.aeroSurface(style, RoundedCornerShape(6.dp))` on an inset pill `Box` | Reuses the one shared painter (`drawAeroSurfaceCore`) rather than a bespoke gradient+clip combo |

**Key insight:** Every "Don't Hand-Roll" row in this phase points back to code that ALREADY EXISTS in this repository and is already three-theme-proven. The risk this phase carries is not "will the pattern work" (it's proven) but "will the plan correctly wire the existing pattern into three NEW call sites without re-deriving a parallel copy." The planner's highest-value verification step is therefore a source-scan guard (mirroring `AeroButtonSurfaceSourceTest.kt`) asserting each of these reuse points is actually imported/called, not reimplemented.

## Common Pitfalls

### Pitfall 1: `aeroGlowRing` chained after a clip-applying primitive
**What goes wrong:** The hover/focus glow bloom renders invisibly (or doesn't render at all).
**Why it happens:** `Modifier.aeroSurface`/`Modifier.aeroGroove`/`Modifier.aeroThumbSurface` all apply `.clip(shape)` internally. A `.clip()` earlier in a modifier chain clips the drawing of every modifier chained after it. `aeroGlowRing`'s bloom rings are drawn OUTSIDE the component's own layout bounds — if a clip from an earlier-chained primitive is already in effect, that bloom gets sliced away entirely (this exact bug was found and fixed during Phase 16's own sign-off, per `STATE.md`'s decision log: "aeroGlowRing was invisible in the gallery ... fixed via ... USAGE CONTRACT (apply outside any clip)").
**How to avoid:** `aeroGlowRing` calls MUST be chained BEFORE (to the left of, earlier in the `Modifier` chain than) any `aeroSurface`/`aeroGroove`/`aeroThumbSurface` call on the same modifier chain, exactly as `AeroButtonSurface.kt:95-105` and `AeroSlider.kt`'s thumb slot both do.
**Warning signs:** Hover/focus state is correctly `true` (verifiable in a debugger/log) but nothing visibly changes on screen.

### Pitfall 2: Corner radius mismatch between clip shape and glow-ring/surface `cornerRadius`
**What goes wrong:** The glow ring or surface bevel doesn't align with the component's own rounded corners — visible seams or a glow that "steps" instead of following the shape.
**Why it happens:** `cornerPx`/`cornerRadius` parameters are independently `.toPx()`'d or hardcoded at each call site instead of being derived from one shared source (16-RESEARCH.md Pitfall 5, still live in Phase 19's three new components).
**How to avoid:** Derive every corner value passed to `aeroGlowRing`/`aeroSurface`/`aeroGroove`/`aeroThumbSurface` for a given component region from ONE named constant (e.g., a private `SEGMENT_CORNER_RADIUS = 4.dp` val), never a second `.toPx()` call on a re-typed literal.
**Warning signs:** Visual seam at the corners; glow ring corner radius that "doesn't quite match" the surface underneath it, most visible on non-100% DPI scale (per SHW-16's explicit non-100%-DPI review pass requirement).

### Pitfall 3: Thumb shadow/glow clipped by the switch track's own `.clip(shape)`
**What goes wrong:** The switch thumb reads flat — no visible drop shadow or glow bloom around the raised nub, even though the styling code is otherwise correct.
**Why it happens:** If the thumb `Box` is nested as a CHILD inside a parent that has the groove's `.clip(RoundedCornerShape(50))` already applied (rather than being a true sibling), Compose clips the thumb's own draw output to the parent's clip bounds — the 18dp-tall track leaves almost no headroom for a shadow/glow that extends beyond it.
**How to avoid:** Structure exactly as Pattern 3 describes — the thumb's own `aeroGlowRing`+`aeroThumbSurface` chain must be on a `Box` that is a sibling of (not descendant of) whichever `Box` carries the groove's own `.clip()`. The current `AeroSwitch.kt` structure (outer `Box` = hot zone + `.clip(shape).background(...)`, inner `Box` = thumb) is CLOSE to correct already, but the planner must verify that moving the groove fill from `.background(trackColor, shape)` to `Modifier.aeroGroove(grooveStyle)` doesn't accidentally move the `.clip()` onto a `Box` that also wraps the thumb.
**Warning signs:** Thumb renders as a flat neutral circle with no gloss/shadow visible, particularly near the track edges where the clip would bite hardest.

### Pitfall 4: Outer `aeroGlowRing` bloom sliced by a parent `.clip()` (segmented control / list item)
**What goes wrong:** Applying `AeroButtonSurface`'s exact hover/focus pattern (`aeroGlowRing` before `aeroSurface`) to an individual segment or list row produces a glow that's abruptly cut off at the segment/row boundary, or bleeds into the neighbouring segment/row.
**Why it happens:** Individual segments sit INSIDE the segmented control's own outer `Row.clip(shape)` (`AeroSegmentedControl.kt:53`); list rows sit inside potentially-clipping scroll containers. `aeroGlowRing`'s bloom deliberately draws outside the modifier's own layout bounds — but "outside the modifier's bounds" is still "inside the outer Row's clip bounds," so the bloom gets sliced by the OUTER clip even though it correctly escapes the segment's own (nonexistent) inner clip.
**How to avoid:** D-10/D-13 already mandate the fix — segmented-control and list-item focus/hover render EXCLUSIVELY in-bounds (fill lightening for hover, an inset `drawRoundRect` stroke for focus), never `aeroGlowRing`. This is the one place in the whole Phase 16-19 arc where the library's usual glow language does NOT apply — document this exception inline in the code (a comment referencing D-10/D-13) so a future maintainer doesn't "fix" it back to `aeroGlowRing` for consistency.
**Warning signs:** Glow visibly clipped at a hard edge that doesn't match the glow's own soft falloff; glow bleeding onto an adjacent segment/row.

### Pitfall 5: `Color.Transparent` reintroduced instead of `baseColor.copy(alpha = 0f)`
**What goes wrong:** On `AeroColorScheme.Classic` (fully-opaque tokens), a gradient meant to "fade to nothing" instead renders a flat, wrong-colored block.
**Why it happens:** `Color.Transparent` is always fully transparent black regardless of the surrounding gradient's hue — a gradient from `someColor` to `Color.Transparent` looks fine on translucent-token themes (AeroBlue/AeroDark) but produces a visible color-shift artifact on Classic's opaque tokens. Both current bugs this phase must fix already do this: `AeroSegmentedControl.kt:59` and `AeroListItem.kt:63`.
**How to avoid:** Every new gradient stop must be `baseColor.copy(alpha = 0f)`, per PRIM-14 (`AeroSurfacePrimitives.kt` and `AeroSurfaceStyle.kt`'s own KDoc reiterate this rule verbatim for exactly this reason).
**Warning signs:** A visible flat-colored patch (rather than a smooth fade) specifically on the Classic theme during three-theme review — this defect is INVISIBLE on AeroBlue/AeroDark, which is exactly why PRIM-14 exists as a named rule rather than something caught by casual single-theme testing.

### Pitfall 6: `Modifier.hoverable` is NOT automatically supplied by `toggleable`/`selectable`
**What goes wrong:** `rememberAeroInteractionState(source).hovered` never becomes `true` even though `interactionSource` is correctly wired into `toggleable`/`selectable`.
**Why it happens:** `Modifier.toggleable`/`Modifier.selectable` report press/focus/click interactions into the given `interactionSource`, but hover reporting is a SEPARATE concern in Compose Foundation — `AeroListItem`'s existing correct pattern explicitly chains BOTH `.hoverable(interactionSource)` AND the click/toggle modifier on the same `interactionSource` (`AeroListItem.kt:74-78`: `.hoverable(interactionSource).then(if (onClick != null) Modifier.clickable(...) else Modifier)`). Simply passing `interactionSource` to `.toggleable(...)` alone (as `AeroSwitch.kt` will need to for D-04) does not, by itself, guarantee hover events are collected unless `.hoverable(interactionSource)` is ALSO explicitly chained.
**How to avoid:** Every new hover-wired component in this phase (VLST-04's explicit mandate) must chain `.hoverable(interactionSource)` alongside `.toggleable(...)`/`.selectable(...)`, exactly as `AeroListItem` already does — never assume the toggle/select modifier supplies hover for free.
**Warning signs:** `state.hovered` reads `false` in all manual testing despite the mouse visibly hovering the control; unit tests on the resolver function pass (since they test the pure function, not the wiring) while the actual rendered component never shows a hover state.

### Pitfall 7: Default platform indication (ripple) doubling with the new custom-painted hover/press cues
**What goes wrong:** Once `AeroSwitch`/`AeroSegmentedControl` gain real hover/press-responsive `AeroSurfaceStyle` painting, the platform's default `LocalIndication` (a ripple/overlay effect) may ALSO render on top, since neither `AeroSwitch.kt`'s current `.toggleable(...)` call nor `AeroSegmentedControl.kt`'s current `.clickable(...)` call passes `indication = null`. `AeroButtonSurface.kt` already solved this for buttons (`Modifier.clickable(enabled = enabled, interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)` at `AeroButtonSurface.kt:88-94`), but `AeroListItem.kt`'s current `.clickable(enabled = enabled, onClick = onClick)` also has NO `indication = null` — meaning this may already be a live minor visual doubling on `AeroListItem` today, worth confirming during the three-theme review even though it's not one of the eight named requirements.
**Why it happens:** Compose Foundation's `clickable`/`toggleable`/`selectable` all draw `LocalIndication.current` by default unless explicitly suppressed. When a component ALSO paints its own custom hover/press `AeroSurfaceStyle`, the default ripple becomes a redundant (and potentially clashing) second visual layer.
**How to avoid:** Pass `indication = null` on every new `.toggleable`/`.selectable` call this phase adds (`AeroSwitch`, `AeroSegmentedControl`), matching `AeroButtonSurface`'s established precedent. This is a concrete, specific recommendation from this research — it is NOT locked by any CONTEXT.md decision, so flag it for the planner as an actionable addition rather than treating it as already-settled. `[ASSUMED]` — no CONTEXT.md/UI-SPEC line explicitly calls this out; verify at implementation/review time whether the ripple is visually objectionable enough to warrant the change, given the milestone's "behavior 1:1" constraint could be read as also covering "don't remove the existing ripple, if any provides accessibility value." The Phase 17 precedent (buttons already suppress it) argues for consistency.
**Warning signs:** A visible secondary flash/ripple overlay distinguishable from the custom Aero hover-lighten/press-gloss cue during manual interaction testing, most visible on Classic's higher-contrast tokens.

## Code Examples

### Switch thumb resolver (VSEL-02/D-05), adapted from the proven slider precedent

```kotlin
// Source: mirrors library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt:244-259
// (resolveSliderThumbStyle) verbatim in shape; switches have no isDragging axis.
internal fun resolveSwitchThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = 7.dp) // 14dp thumb -> 7dp radius
    if (!enabled) return rest.flattenDisabled(colors)
    return when {
        pressed -> rest.copy(glossAlpha = rest.glossAlpha + PRESSED_GLOSS_BOOST) // D-05: NOT pressedRecess
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
```

### Switch groove resolver (VSEL-01/D-02), the lerp-driven checked-progress approach the UI-SPEC recommends

```kotlin
// Source: 19-UI-SPEC.md Color § "Finding to confirm during planning — AeroSwitch's off-groove"
internal fun resolveSwitchGrooveStyle(
    colors: AeroColorScheme,
    checkedProgress: Float,   // 0f..1f, the existing animateFloatAsState-equivalent
    enabled: Boolean,
): AeroSurfaceStyle {
    val neutral = AeroSurfaceStyle.neutralRest(colors, cornerRadius = 9.dp) // 18dp track -> 9dp radius
    val accent = AeroSurfaceStyle.rest(colors, cornerRadius = 9.dp)
    val resolved = neutral.copy(
        fillTop = androidx.compose.ui.graphics.lerp(neutral.fillTop, accent.fillTop, checkedProgress),
        fillBottom = androidx.compose.ui.graphics.lerp(neutral.fillBottom, accent.fillBottom, checkedProgress),
    )
    return if (enabled) resolved else resolved.flattenDisabled(colors)
}
```

### Segmented control per-segment resolver (VSEL-03/VSEL-04/D-08)

```kotlin
// Source: composes AeroSurfaceStyle.rest + pressedRecess (theme/AeroSurfaceStyle.kt) + hoverLighten,
// mirroring resolveButtonStyle's precedence shape (AeroButtonSurface.kt:244-279).
internal fun resolveSegmentStyle(
    colors: AeroColorScheme,
    isSelected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val base = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)
    val selectedOrNot = if (isSelected) base.pressedRecess(PRESSED_INNER_SHADOW) else base
    if (!enabled) return selectedOrNot.flattenDisabled(colors)
    return if (hovered) selectedOrNot.hoverLighten() else selectedOrNot
}
```

### `AeroListItem` D-11 composition (VLST-01/VLST-02/VLST-04) — the load-bearing unit-testable piece

```kotlin
// Source: 19-UI-SPEC.md's literal pseudocode, made concrete. Returns null when no pill should draw.
internal fun resolveListItemPillStyle(
    colors: AeroColorScheme,
    selected: Boolean,
    hovered: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle? {
    val selectedStyle = AeroSurfaceStyle.rest(colors, cornerRadius = 6.dp).let { s ->
        s.copy(
            fillTop = s.fillTop.copy(alpha = s.fillTop.alpha * 0.5f),
            fillBottom = s.fillBottom.copy(alpha = s.fillBottom.alpha * 0.5f),
        )
    }
    val base: AeroSurfaceStyle? = if (selected) selectedStyle else null
    val withHover = if (hovered && enabled) {
        (base ?: AeroSurfaceStyle.neutralRest(colors, cornerRadius = 6.dp)).hoverLighten()
    } else base
    return withHover?.let { if (enabled) it else it.flattenDisabled(colors) }
}
```

## State of the Art

Not applicable in the usual "framework changed" sense — there is no external framework/library version drift relevant to this phase (Compose Multiplatform/Kotlin are already pinned by Phase 15 and unchanged since). The relevant "state of the art" evolution is entirely internal, across this milestone's own phases:

| Old Approach (Phase 15 baseline, this repo) | Current Approach (Phase 17-18, this repo) | When Changed | Impact |
|--------------------------------------------|---------------------------------------------|---------------|--------|
| `when { selected -> ...; hovered -> ...; else -> ... }` branch selection for combined states | Base-then-transform composition (`resolveButtonStyle`'s hover/press chain) | Phase 17 | Phase 19 is the first place this pattern is applied RETROACTIVELY to fix an existing bug (D-11/VLST-02), not just used for a new component |
| Flat `.alpha(0.4f)` disabled dimming | `flattenDisabled(colors)` geometry-flattening transform | Phase 17 (17-05 sign-off gap-fix) | All three Phase 19 components currently use the OLD `.alpha(0.4f)` approach and must migrate |
| `Color.Transparent` gradient fade target | `baseColor.copy(alpha = 0f)` | Phase 16 (PRIM-14) | `AeroSegmentedControl.kt:59` and `AeroListItem.kt:63` are the two remaining pre-Phase-16-fix call sites in the whole eight-component target set |

**Deprecated/outdated (within this repo, being removed this phase):**
- `AeroSwitch.kt`'s two-plain-`Box` rendering with no gloss/bevel/rim — replaced by `aeroGroove`/`aeroThumbSurface`.
- `AeroSegmentedControl.kt`'s bare `.clickable` with no `Role` — replaced by `Modifier.selectable(role = Role.RadioButton)`.
- `AeroListItem.kt`'s unclipped `.background(animatedBg)` — replaced by a clipped `Modifier.aeroSurface(...)` pill.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | New `.toggleable`/`.selectable` calls on `AeroSwitch`/`AeroSegmentedControl` should pass `indication = null` to avoid doubling with the new custom-painted hover/press visuals (Common Pitfall #7). | Common Pitfalls #7 | Low-to-medium — if wrong, a visible ripple-doubling artifact ships; easily caught and fixed at the three-theme human sign-off (Phase 17/18 precedent already includes this checkpoint), not a structural risk |
| A2 | The segmented-control resolver and switch resolvers should live IN-FILE (`AeroSwitch.kt`/`AeroSegmentedControl.kt`) rather than a new shared file, mirroring `AeroSlider.kt`'s placement of `resolveSliderThumbStyle` in the component's own file rather than a separate resolvers module. | Architecture Patterns / Recommended Project Structure | Low — purely an organizational choice; either placement compiles and tests identically. Planner's discretion, following the closest existing precedent |
| A3 | `AeroButtonSurface.kt`'s `PRESSED_INNER_SHADOW` constant is currently `private` and will need visibility widened (or duplicated verbatim) for `AeroSegmentedControl.kt` to import it cross-package. | Code Examples "Pattern 4" | Low — a five-minute compile-error-driven fix either way; flagged so the planner allocates a task for it rather than discovering it mid-implementation |

**If this table is empty:** N/A — see rows above. All three are LOW risk; nothing in this research reaches HIGH-risk unverified territory because the phase reuses code that is already shipped and three-theme-proven.

## Open Questions

1. **Should the segmented control's per-segment focus inner-rim reuse a shared helper, or be a one-off `drawRoundRect` inline in `AeroSegmentedControl.kt`?**
   - What we know: D-10/D-13 both want an "inset `drawRoundRect` stroke at `colors.borderSelected`" for segmented-control focus AND list-item focus — the same visual idiom, twice.
   - What's unclear: Whether this warrants a small new shared primitive (e.g., `Modifier.aeroInnerFocusRing`) in `theme/AeroSurfacePrimitives.kt`, or two independent inline implementations, since only two of eight total library components need it and adding new PUBLIC API surface to the Phase 16 primitives layer outside its own phase is technically out-of-scope-creep.
   - Recommendation: Planner's discretion — if the two inline implementations end up byte-identical, extracting a shared internal (not public) helper avoids drift risk cheaply; if they diverge even slightly (e.g., different alpha), keep them separate rather than forcing a shared abstraction prematurely.

2. **Does the switch's `checkedProgress` float (needed for the groove's lerp-based fill in the resolver example above) already exist as a reusable value, or does `AeroSwitch.kt`'s current `animateFloatAsState(thumbProgress)` need to be reused for BOTH thumb-position AND groove-fill-lerp?**
   - What we know: `AeroSwitch.kt`'s current code already computes `thumbProgress` via `animateFloatAsState` for the thumb's x-offset (`AeroSwitch.kt:48-52`).
   - What's unclear: Whether reusing that exact same progress float to also drive the groove's fill-color lerp is correct, or whether keeping the existing separate `trackColor` `animateColorAsState` (recolored to read through the accent/neutral `AeroSurfaceStyle` fields) is simpler and equally correct. Both approaches honor D-02's "one moving part" if done carefully — but they are not code-identical, and the planner should pick one explicitly rather than leaving it ambiguous across tasks.
   - Recommendation: Reuse the existing `thumbProgress` value for the groove lerp too (fewer animated values total, satisfies D-02's spirit maximally) unless a concrete rendering issue (e.g., needing different easing) surfaces during implementation.

## Environment Availability

Skipped — this phase has no external dependencies beyond the project's own already-building Gradle/Kotlin/Compose toolchain (fully verified and pinned by the completed Phase 15). No new tools, services, runtimes, or CLIs are introduced.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | `kotlin.test` (JUnit-backed) for pure-function/value tests; `androidx.compose.ui.test.runComposeUiTest` (`@OptIn(ExperimentalTestApi::class)`, CMP 1.11 API) for semantics/keyboard UI tests — both already in use project-wide (see `AeroSliderStylesTest.kt`, `AeroButtonSemanticsTest.kt`) |
| Config file | none dedicated — standard Gradle `test` source set (`library/src/test/kotlin/...`), no separate test-runner config |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.selection.*"` / `--tests "com.mordred.aero.components.list.*"` (module-scoped, seconds) |
| Full suite command | `./gradlew build` (per TOOL-05/VER precedent — 232+ tests baseline, growing each phase) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| VSEL-01 | Switch groove/thumb geometry (visual gloss/bevel/shadow) | visual/UAT (backstop) | three-theme human sign-off (Phase 17-04/18-04 precedent) | N/A — not automatable pixel-diff (Roborazzi/Paparazzi Android-only, ruled out at milestone level) |
| VSEL-01 (partial) | Switch thumb/groove resolver returns theme-aware `neutralRest`/`rest`-derived styles | unit | `./gradlew :library:test --tests "*.AeroSwitchStylesTest"` | ❌ Wave 0 — new file needed |
| VSEL-02 | Press keeps thumb raised w/ brighter gloss (not `pressedRecess`), hover = `hoverLighten`, focus = unchanged fill + persistent glow ring | unit | `./gradlew :library:test --tests "*.AeroSwitchStylesTest"` (mirrors `AeroSliderStylesTest`'s precedence-chain assertions) | ❌ Wave 0 |
| VSEL-02 | `toggleable(role=Switch)` semantics survive the restyle (keyboard Space toggles `checked`) | semantics/UI | `./gradlew :library:test --tests "*.AeroSwitchSemanticsTest"` (mirrors `AeroButtonSemanticsTest`) | ❌ Wave 0 |
| VSEL-03 | Selected segment equals `rest(...).pressedRecess(PRESSED_INNER_SHADOW)` exactly (value equality, catches drift) | unit | `./gradlew :library:test --tests "*.AeroSegmentedControlStylesTest"` (mirrors `AeroSliderStylesTest`'s "reconstruct expected from scratch" convention) | ❌ Wave 0 |
| VSEL-03 | Source-scan guard: `AeroSegmentedControl.kt` imports/calls `pressedRecess`, does not reimplement gradient inversion | source-scan | `./gradlew :library:test --tests "*.AeroSegmentedControlSourceTest"` (mirrors `AeroButtonSurfaceSourceTest`) | ❌ Wave 0 |
| VSEL-04 | `Modifier.selectable(role=RadioButton)` — Tab reaches every segment, Space/Enter activates | semantics/UI | `./gradlew :library:test --tests "*.AeroSegmentedControlSemanticsTest"` (mirrors `AeroButtonSemanticsTest`'s `performKeyInput`/`pressKey` pattern) | ❌ Wave 0 |
| VSEL-04 | In-bounds-only hover/focus (no `aeroGlowRing` call on individual segments) | source-scan guard | same `AeroSegmentedControlSourceTest` — assert `aeroGlowRing(` does NOT appear inside the per-segment Box's modifier chain (string-scope the segment-building lambda if feasible, else assert absence in the whole file since the control itself legitimately has none) | ❌ Wave 0 |
| VLST-01 | Selection pill is clipped (uses `aeroSurface(...)`, not raw `.background()`) | source-scan guard | `./gradlew :library:test --tests "*.AeroListItemSourceTest"` | ❌ Wave 0 |
| VLST-02 | Hover on a selected row is strictly BRIGHTER than resting-selected, never falls back to unselected-hover color — the core structural claim | unit (highest-value test this phase adds) | `./gradlew :library:test --tests "*.AeroListItemStylesTest"` — assert `resolveListItemPillStyle(selected=true, hovered=true)` != `resolveListItemPillStyle(selected=true, hovered=false)` AND its fill is `.lighten()`-equal to the selected-then-hover-transform of the latter (byte-for-byte, not just "different") | ❌ Wave 0 |
| VLST-03 | Focus visual renders in-bounds only when `onClick != null` | source-scan guard + possibly a semantics test asserting a display-only row (`onClick=null`) has no focus target | `./gradlew :library:test --tests "*.AeroListItemSourceTest"` / `*.AeroListItemSemanticsTest` | ❌ Wave 0 |
| VLST-04 | New components use `.hoverable(interactionSource)` + `collectIsHoveredAsState`-backed collection, never raw `pointerInput`/`awaitPointerEventScope` for hover | source-scan guard across `AeroSwitch.kt` + `AeroSegmentedControl.kt` | assert both files contain `.hoverable(` and do NOT contain `awaitPointerEventScope`/`pointerInput` (the `AeroRangeSlider`-only Canvas-forced anti-pattern) | ❌ Wave 0 — can combine into `AeroSwitchSourceTest`/`AeroSegmentedControlSourceTest` above rather than a fourth file |
| PRIM-14 (carried) | No `Color.Transparent` reintroduced in any of the three files | source-scan guard | assert absence of the literal string `Color.Transparent` in `AeroSwitch.kt`/`AeroSegmentedControl.kt`/`AeroListItem.kt` post-restyle | ❌ Wave 0 — fold into the per-component Source tests above |

### Sampling Rate

- **Per task commit:** module-scoped `--tests` filter for whichever component's files just changed (seconds).
- **Per wave merge:** `./gradlew :library:test` (full library module, without the showcase build) to catch cross-component regressions (e.g., a shared `theme/` file accidentally touched).
- **Phase gate:** Full `./gradlew build` (matching TOOL-05/library-wide precedent) green, PLUS the human three-theme × per-state sign-off (mirrors Phase 17-05/18-04's checkpoint) before `/gsd-verify-work`.

### Wave 0 Gaps

- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt` — pure resolver tests (VSEL-01/VSEL-02), mirrors `AeroSliderStylesTest.kt`'s structure exactly (reconstruct-expected-from-scratch convention, disabled-wins assertions, both `AeroBlue`+`Classic` schemes)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt` — `Role.Switch` + keyboard Space activation survives restyle, mirrors `AeroButtonSemanticsTest.kt`
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` — resolver value-equality tests (VSEL-03), including an explicit assertion the selected style equals `AeroSurfaceStyle.rest(colors, 4.dp).pressedRecess(PRESSED_INNER_SHADOW)` reconstructed independently
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt` — `Role.RadioButton`, per-segment Tab + Space/Enter, mirrors `AeroButtonSemanticsTest.kt`'s `performKeyInput` pattern
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` — source-scan: `pressedRecess(` present, `Color.Transparent` absent, `aeroGlowRing(` absent per-segment, `.hoverable(` present, `pointerInput`/`awaitPointerEventScope` absent
- [ ] `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt` — the VLST-02 structural-fix test (highest priority: prove hover-on-selected strictly composes rather than being suppressed)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt` — source-scan: `aeroSurface(` present (pill clipping), `Color.Transparent` absent, no `when { selected -> ...; hovered -> ... }` branch shape reintroduced
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` — source-scan: `aeroGroove(`/`aeroThumbSurface(` present, `Color.Transparent` absent, `.hoverable(` present, `pointerInput` absent, `pressedRecess` ABSENT (D-05 forbids it on the thumb)
- [ ] Per the v2.0.3/VER-06 "guard must provably FAIL on unfixed code" discipline this project enforces everywhere: every new source-scan guard above should be run once against the CURRENT (pre-restyle) file content to confirm it correctly FAILS before the restyle lands, then re-run to confirm it PASSES after — do not skip this proof step (matches `AeroButtonSurfaceSourceTest.kt`'s documented "Fail-Then-Pass Proof" precedent).

*(No test framework install needed — `kotlin.test`/`runComposeUiTest` are already wired project-wide.)*

## Security Domain

**`security_enforcement` is not explicitly set in `.planning/config.json` — treated as enabled per default, but this phase has essentially no attack surface.** `AeroSwitch`, `AeroSegmentedControl`, and `AeroListItem` are pure presentation/interaction-state components: no network calls, no persistence, no parsing of untrusted input, no authentication/session concept, no cryptography. The component boundary (`checked: Boolean`, `selected: T`, `onClick: (() -> Unit)?`) is 100% caller-owned application state — this phase never reads or writes it beyond what the pre-existing shipped code already did (milestone constraint: behavior does not change beyond the additive `interactionSource` param, D-04).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | No auth concept in a UI component library |
| V3 Session Management | No | No session concept |
| V4 Access Control | No | No authorization boundary — `enabled: Boolean` is a caller-controlled presentation flag, not a security control |
| V5 Input Validation | Marginal — `options: List<T>`/`optionLabel: (T) -> String` accept caller-supplied data | Existing behavior unchanged; `Text(..., maxLines/overflow)` is a display concern (tracked as a pre-existing, out-of-scope defect at E2/E3's backstop rows in 19-UI-SPEC.md), not an injection vector — Compose `Text` does not execute caller strings |
| V6 Cryptography | No | No cryptographic operation anywhere in this phase |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| None identified specific to this phase | — | This is a desktop UI rendering library with no network/file/auth boundary crossed by any of the three components in scope; the closest analog to a "threat" is a caller passing a pathologically long label string, which is a UI-considerations/overflow concern (already tracked as E2/E3 backstops in 19-UI-SPEC.md), not a security concern |

## Sources

### Primary (HIGH confidence — verified via direct codebase read this session)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — full file read, `drawAeroSurfaceCore`/`aeroSurface`/`aeroGlowRing`/`drawAeroGlowRing`/`drawAeroThumb`/`aeroThumbSurface`/`drawAeroGroove`/`aeroGroove`, all KDoc and ordering rules
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` — full file read, `rest`/`neutralRest`/`pressedRecess`/`flattenDisabled`/`hoverLighten`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` — full file read, `derive()` formula
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` — full file read, `resolveButtonStyle`/`outlinedStyle`/`PRESSED_INNER_SHADOW`
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` — full file read, `resolveSliderThumbStyle`/`resolveSliderTrackStyle`, thumb/track slot layering
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` — full file read, per-thumb hover via manual `pointerInput` (Canvas-forced exception, confirmed NOT applicable to Box-based Phase 19 components)
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` — full file read, `rememberAeroInteractionState`
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSwitch.kt` — full file read (current, pre-restyle)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — full file read (current, pre-restyle)
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroRadioButton.kt` — full file read, confirms `Modifier.selectable(role = Role.RadioButton)` already compiles/works in this exact repo
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — full file read (current, pre-restyle), the exact VLST-01/02 bug locations
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt`, `AeroButtonSurfaceSourceTest.kt`, `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt` — full read, test-pattern precedents for Validation Architecture
- `.planning/phases/19-selectors-lists/19-CONTEXT.md`, `19-UI-SPEC.md`, `19-DISCUSSION-LOG.md` — full read, locked decisions
- `.planning/phases/16-foundation-aero-primitives-layer/16-PATTERNS.md` — full read
- `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/ROADMAP.md`, `.planning/config.json` — full read

### Secondary (MEDIUM confidence)
- None — this phase required no external documentation lookup; all findings are grounded in direct, first-party codebase reads (the highest-confidence source available for a codebase-continuity phase)

### Tertiary (LOW confidence)
- Common Pitfall #7 (`indication = null` recommendation) — this is an inference/recommendation from this research session, not confirmed by any locked CONTEXT.md/UI-SPEC.md decision; flagged `[ASSUMED]` in the Assumptions Log (A1)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — zero new dependencies; every consumed API already compiles in this exact repo
- Architecture: HIGH — every pattern (resolver shape, primitive usage, modifier-chain ordering) already exists and is proven by Phase 17/18 sign-offs
- Pitfalls: HIGH for the six codebase-grounded pitfalls (clip ordering, corner radius, thumb layering, in-bounds glow exception, `Color.Transparent`, hoverable wiring); MEDIUM for Pitfall 7 (`indication = null`) since it is a recommendation, not a confirmed defect

**Research date:** 2026-07-27
**Valid until:** Effectively indefinite for this milestone — no external framework drift risk. Re-verify only if Phase 16/17/18's shipped primitives/resolvers are modified after this research is consumed (unlikely; those phases are marked Complete in ROADMAP.md).
