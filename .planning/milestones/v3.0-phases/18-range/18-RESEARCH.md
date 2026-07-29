# Phase 18: Range - Research

**Researched:** 2026-07-23
**Domain:** Compose Multiplatform Desktop — Material3 `Slider` custom slots + Canvas-owning draw primitives (Aero glass restyle)
**Confidence:** HIGH

## Summary

Phase 18 restyles three already-functioning components (`AeroSlider`, `AeroRangeSlider`,
`AeroProgressBar`) using ONLY the primitives Phase 16 shipped and the style-transform helpers
Phase 17 already wrote for buttons. Nothing new needs to be built at the primitives layer — the
one net-new piece of code this phase adds is a single style factory,
`AeroSurfaceStyle.neutralRest(base, cornerRadius)`, in `theme/AeroSurfaceStyle.kt`, because the
thumb/groove must be neutral-glass while `AeroSurfaceStyle.rest()` is accent(`primary`)-derived
(confirmed by direct read of `AeroOrnamentTokens.derive()` — `fillSplitTop/Bottom` AND
`bevelLight/bevelShadow` are all `base.primary`-derived, not neutral).

All three components' current surfaces were read in full (86/297/114 lines respectively) and are
short, self-contained files — this is a render-only swap, not a rewrite. `AeroSlider` keeps
Material3's `Slider` but must switch from the 6-argument overload it uses today (no `thumb=`/
`track=` params) to the `@ExperimentalMaterial3Api`-annotated 10-argument overload that exposes
`thumb: @Composable (SliderState) -> Unit` / `track: @Composable (SliderState) -> Unit`,
confirmed by direct read of the pinned `material3-desktop-1.9.0` sources jar (not memory — this
project's own TOOL-07/PRIM-18 discipline). One load-bearing wiring gap found during this research
that the CONTEXT.md/UI-SPEC.md did not fully spell out: **M3's own default `Thumb` composable is
the ONLY place `.hoverable(interactionSource)` gets attached** — the outer `Slider`/`SliderImpl`
layout wires press/drag/focus at the top level, but hover is opt-in per-thumb-composable. Our
custom `thumb =` slot MUST apply `Modifier.hoverable(interactionSource)` itself or VRNG-03's hover
state will never fire. `AeroRangeSlider` and `AeroProgressBar` need no such M3-mechanics research
— they are pure Canvas/Box files where `drawAeroSurfaceCore`/`drawAeroGroove`/`drawAeroThumb` are
called directly, with zero drag-logic touch (VRNG-04) confirmed to be entirely isolated from the
render block (drag lives in the `.pointerInput { awaitPointerEventScope { ... } }` block; render
lives in the separate `Canvas(...) { ... }` draw lambda below it).

**Primary recommendation:** Add `AeroSurfaceStyle.neutralRest()` to `theme/AeroSurfaceStyle.kt`
first (it's a foundation dependency for all three components), then restyle `AeroProgressBar`
(simplest — no drag, no M3 slot mechanics), then `AeroRangeSlider` (direct-call Canvas path, no
M3), then `AeroSlider` last (only one requiring the M3 slot-mechanics + `@OptIn` + hover-wiring
gotcha above).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Slider drag/keyboard/step value logic | API/Backend-equivalent (component logic layer) | — | `SliderState`/`AeroRangeSlider`'s own drag loop own the value math; this phase never touches it (VRNG-02/04) |
| Slider/RangeSlider/ProgressBar visual rendering | Client (Compose Desktop render tier — `DrawScope`) | — | All painting happens in `drawAeroSurfaceCore`/`drawAeroGroove`/`drawAeroThumb` calls inside `DrawScope`/`drawWithCache` — no other tier is involved (desktop app, no network/server split) |
| Style resolution (rest/hover/press/focus/disabled → `AeroSurfaceStyle`) | Client (pure Kotlin resolver fn, Compose-free) | — | Mirrors `resolveButtonStyle` — a plain function taking booleans + `AeroColorScheme`, testable with `kotlin.test` alone, no Compose runtime needed |
| Interaction state collection (hover/press/focus) | Client (Compose `InteractionSource`) | — | `rememberAeroInteractionState(source)` — per-thumb for `AeroRangeSlider` (two independent `MutableInteractionSource`s) |
| Theme/ornament token resolution | Client (`AeroColorScheme`/`AeroOrnamentTokens`) | — | Resolved once per composition via `AeroTheme.colors`, consumed by the resolver fn above |

## User Constraints (from CONTEXT.md)

<user_constraints>

### Locked Decisions

- **D-01:** Active track fill / progress fill = accent/primary two-tone glass (same family as
  Phase 17's filled `AeroButton`). Thumb = neutral raised glass nub (NOT accent-colored). Inactive
  track = recessed neutral groove. Classic Win7 identity: blue active fill, neutral movable thumb.
  Reversibility: costly.
- **D-02:** Accent fill uses the same two-tone-glass treatment as the filled button (gradient +
  top gloss + soft seam/bevel), gloss proportional to the thin track height (~30–35% per Phase 16
  D-01), never a pixel literal. Exact seam/bevel magnitude on thin track is Claude's discretion —
  err toward clean over ornate. Reversibility: reversible.
- **D-03:** Thumb hover/focus reuse Phase 17's button glow idiom: `aeroGlowRing` on hover + light
  brighten (17 D-03), constant `aeroGlowRing` on keyboard focus (17 D-04). **Load-bearing check:**
  outer glow bloom must NOT be clipped by M3 `Slider`'s internal `thumb =` slot bounds — verify at
  first implementation, respect the `aeroGlowRing`-before-`aeroSurface` ordering rule. Reversibility:
  reversible.
- **D-04:** Thumb press/drag does NOT adopt the button's recessed/inverted press (17 D-02). A
  dragged thumb STAYS RAISED (picked-up read), glow intensified/darkened, slightly brighter gloss
  as the active-drag cue. Applied independently per thumb on `AeroRangeSlider` (VRNG-05).
  Reversibility: reversible.
- **D-05:** Determinate fill is glass with a static top gloss ON by default. The animated running
  sheen is a SEPARATE optional parameter, default `false` (VRNG-07's exact boundary: static gloss
  always on, traveling highlight opt-in). Reversibility: reversible.
- **D-06:** Restyled indeterminate = single accent glass segment (same fill as determinate) sweeping
  left→right across the recessed bed, soft edges fading to `baseColor.copy(alpha = 0f)` (never
  `Color.Transparent`, PRIM-14). Keeps existing 1500ms restart timing, no ping-pong (VRNG-08).
  Exact segment width/gradient shape is Claude's discretion. Reversibility: reversible.
- **D-07:** Disabled mirrors Phase 17 D-05 "flatten to dead" — gloss/bevel removed, gradient
  collapses toward near-flat, color muted. Replaces `AeroSlider`'s `alpha = 0.4f` `SliderColors`
  disabled and `AeroRangeSlider`'s flat disabled. Because `AeroSlider` now draws its own
  `thumb=`/`track=` slots, disabled rendering lives in OUR slots — do NOT rely on M3 `SliderColors`
  for disabled appearance. Reversibility: costly.

### Claude's Discretion

- Native `Modifier.dropShadow`/`innerShadow` vs manual gradient for each depth cue — decided on
  three-theme review per Phase 16 D-02. Signatures are in `ScratchAeroShadowProof.kt`.
- Exact groove depth / track thickness / thumb size WITHIN existing default sizes (public sizes
  must not change — VRNG scope), and exact seam/bevel magnitude on thin tracks (D-02).
- Exact indeterminate segment width and gradient falloff shape (D-06).
- Exact hover-vs-focus glow differentiation on the thumb (intensity/tint), mirroring Phase 17
  D-03/D-04 discretion.
- Whether the disabled flattened surface is a distinct `AeroSurfaceStyle` variant or a transform
  of the rest style (mirror whatever Phase 17 chose for buttons).
- The existing drag-tooltip glass pill on both sliders — keep, restyle to match, or leave as-is;
  low priority, on-review.

### Deferred Ideas (OUT OF SCOPE)

- `AeroRangeSlider` accessibility semantics + keyboard support — the component currently exposes
  ZERO semantics (custom Canvas). Phase 18 stays strictly render-only (VRNG-04); adding
  `Modifier.semantics`/keyboard support is a future phase / backlog item, not folded into Range.
- `VRNG-F01` Win7-authentic ping-pong indeterminate (bar decelerating at the edges) — explicitly
  excluded by VRNG-08 (no ping-pong this milestone). Candidate for a later polish phase.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| VRNG-01 | `AeroSlider` keeps M3 `Slider`, passes custom `thumb =`/`track =` slots | Confirmed via direct source read: use the `@ExperimentalMaterial3Api`-annotated `Slider(value, onValueChange, modifier, enabled, onValueChangeFinished, colors, interactionSource, steps, thumb, track, valueRange)` overload (`Slider.kt:271-308`). `@OptIn(ExperimentalMaterial3Api::class)` required (ScratchSliderSlotSpike.kt precedent, line 91) |
| VRNG-02 | No regression: drag, keyboard arrows, `steps` snap, `onValueChangeFinished`, semantics | `SliderImpl` (`Slider.kt:744-876`) wires `.sliderSemantics(state, enabled)`, `.focusable(...)`, `.slideOnKeyEvents(...)`, tap+drag modifiers all at the OUTER Layout — independent of which `thumb=`/`track=` composables are supplied. Swapping the slots cannot regress these; `state.value`/`steps`/`onValueChangeFinished` flow through `SliderState` unchanged. **Gap found:** current `AeroSlider.kt` public API has NO `onValueChangeFinished` parameter at all — flagged as an Open Question below |
| VRNG-03 | `AeroSlider` — recessed groove, raised thumb, hover/focus on thumb | `aeroGroove(style)` for track slot, `aeroThumbSurface(style)` for thumb slot. **Hover requires explicit `Modifier.hoverable(interactionSource)` on the thumb slot's own Box** — M3's default `Thumb` applies it internally (`Slider.kt:1237`/`1294`); a from-scratch custom thumb slot does NOT get it for free |
| VRNG-04 | `AeroRangeSlider` — groove + raised thumbs, render only, drag logic untouched | `AeroRangeSlider.kt`'s drag lives entirely in `.pointerInput(enabled, valueRange, steps) { awaitPointerEventScope { ... } }` (lines 180-227); render lives in the separate `Canvas(...) { ... }` draw lambda (lines 228-274) below it — physically separate blocks, confirmed by direct read. Replace only the `drawLine`/`drawCircle` calls (lines 239-266) with `drawAeroGroove`/`drawAeroSurfaceCore`/`drawAeroThumb` direct calls |
| VRNG-05 | `AeroRangeSlider` — hover/press independently per thumb | Two independent `MutableInteractionSource`s + two `rememberAeroInteractionState()` calls, one per thumb; wire each thumb's Canvas-region hit-test to its own source. See Wiring Recipe below — no per-thumb `Modifier` chain exists on Canvas, must derive hover manually from `activeThumb`/pointer-position state already present in the file |
| VRNG-06 | `AeroProgressBar` — recessed bed, gradient fill with gloss | `drawAeroGroove` (bed, direct-call inside `drawWithCache`/`Modifier.aeroGroove`) + `drawAeroSurfaceCore`/`Modifier.aeroSurface` (fill) with `AeroSurfaceStyle.rest(colors, cornerRadius = height/2)` |
| VRNG-07 | `AeroProgressBar` — periodic sheen optional, default OFF | New optional `Boolean = false` parameter on the determinate overload; sheen is a SEPARATE overlay draw on top of the always-on static gloss, gated by this flag |
| VRNG-08 | Indeterminate restyled; 1500ms restart kept; no ping-pong | Existing `infiniteRepeatable(tween(1500, LinearEasing), RepeatMode.Restart)` (line 91-94) — timing/mode untouched, only what's drawn under the animated offset changes (`drawAeroSurfaceCore` accent segment with faded edges instead of a flat `Box.background`) |
| VRNG-09 | Pattern 3 reuse where animation + drag write the same value | `AeroPanelGroup.kt:443-451` is the canonical recipe — copy the `animateFloatAsState(targetValue = ..., animationSpec = if (isDragging) snap() else tween(...))` shape verbatim for any animated thumb-glow/gloss value that must not lag during an active per-thumb drag |

</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose Multiplatform Desktop | 1.11.1 (pinned, `library/build.gradle.kts`) | UI runtime | Already the project's locked toolchain (Phase 15) |
| `org.jetbrains.compose.material3:material3` | 1.9.0 (pinned, explicit stable coordinate, NOT the `compose.material3` alias) | `Slider`/`SliderState`/`SliderDefaults` — kept for `AeroSlider` only | VRNG-01's "repaired architecture, not full removal" decision; `RangeSlider`/M3 banned elsewhere per PITFALL-03 |

No new dependencies. `AeroRangeSlider`/`AeroProgressBar` add zero new imports beyond what's already
in `theme/AeroSurfacePrimitives.kt` and `theme/AeroSurfaceStyle.kt`.

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.foundation.hoverable` | bundled w/ CMP 1.11.1 | Hover tracking on the custom `AeroSlider` thumb slot | Only inside the `thumb =` lambda's Box — M3 no longer supplies this once the default `Thumb` composable is replaced |
| `kotlin.test` + JUnit Jupiter | already project-pinned (`library/build.gradle.kts:31-45`) | Value-level tests for the new style-resolver function(s) | Mirror `AeroButtonStylesTest.kt`'s pattern exactly |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Keeping M3 `Slider` + custom slots (VRNG-01, locked) | Full M3 removal, reimplement as Canvas like `AeroRangeSlider` | Rejected by the PRIM-18 spike (PASS verdict) — would re-promote this phase to HIGH complexity, re-deriving keyboard nudge/steps-snap/`onValueChangeFinished`/semantics from scratch (16-RESEARCH.md Pitfall 6/8). Not this phase's call to revisit — architecture is locked in STATE.md |
| Manual `.hoverable()` wiring on custom thumb | Reusing `SliderDefaults.Thumb`'s internal shrink-on-press behavior | Rejected — M3's default `Thumb` shrinks `thumbSize.width` by half on press/drag (`Slider.kt:1228-1233`); D-04 explicitly wants the thumb to STAY RAISED (not shrink/invert) on press, so the default `Thumb` composable's behavior cannot be reused even partially — must write a from-scratch thumb slot |

**Installation:** No new dependencies to install — everything consumed by this phase already ships
in `library/build.gradle.kts` and Phase 16/17's committed source.

**Version verification:** `org.jetbrains.compose.material3:material3:1.9.0` confirmed pinned at
`library/build.gradle.kts:22` (direct read); the sources jar
(`material3-desktop-1.9.0-sources.jar`) was extracted and read directly for this research —
`Slider.kt`'s custom-slot overload, `SliderImpl`'s modifier wiring, and `SliderTokens`' literal
dimension constants are all confirmed from the actual shipped 1.9.0 source, not training-data
memory or a newer M3 version's API shape.

## Package Legitimacy Audit

Not applicable — this phase adds zero new external dependencies (no `npm install`/`pip install`/
`cargo add` equivalent). All consumed APIs (`Slider`, `SliderState`, `MutableInteractionSource`,
`hoverable`) come from the already-pinned, already-vetted `material3-desktop:1.9.0` and
`androidx.compose.foundation` artifacts (Compose Multiplatform 1.11.1, pinned Phase 15). No audit
table required.

## Architecture Patterns

### System Architecture Diagram

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                     AeroTheme { }                       │
                    │        LocalAeroColors → AeroColorScheme + tokens       │
                    └──────────────────────────┬────────────────────────────┘
                                                 │ colors read once per composition
                    ┌────────────────────────────┼────────────────────────────┐
                    ▼                            ▼                            ▼
         ┌────────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
         │     AeroSlider      │      │   AeroRangeSlider    │      │   AeroProgressBar    │
         │  (M3 Slider host)   │      │  (Canvas, own drag)  │      │  (Box/Canvas, no drag)│
         └──────────┬──────────┘      └──────────┬───────────┘      └──────────┬───────────┘
                    │                            │                            │
      ┌─────────────┴─────────────┐   ┌──────────┴──────────┐      ┌──────────┴──────────┐
      │ interactionSource (1x)     │   │ 2x MutableInteraction│      │ (no interaction;    │
      │ → rememberAeroInteraction  │   │ Source (one/thumb)   │      │  animated float only)│
      │   State(source)             │   │ → per-thumb state    │      │                      │
      └─────────────┬─────────────┘   └──────────┬──────────┘      └──────────┬──────────┘
                    │ hovered/pressed/focused     │ per-thumb h/p/f            │ shimmer/target
                    ▼                            ▼                            ▼
      ┌─────────────────────────────────────────────────────────────────────────────────┐
      │             Style resolver (pure fn, Compose-free, mirrors resolveButtonStyle)    │
      │  rest = neutralRest(thumb) / rest(accent fill)  →  .hoverLighten() / press cue    │
      │                            →  .flattenDisabled() when !enabled                     │
      └─────────────────────────────────────────┬─────────────────────────────────────────┘
                                                 │ resolved AeroSurfaceStyle(s)
                    ┌────────────────────────────┼────────────────────────────┐
                    ▼                            ▼                            ▼
         thumb slot draws:              Canvas draws directly:        Canvas/Box draws directly:
         Modifier.aeroGlowRing(...)     drawAeroGroove(...)   (bed)   drawAeroGroove(...)  (bed)
           .aeroThumbSurface(style)     drawAeroSurfaceCore(...) (fill)  drawAeroSurfaceCore(...) (fill/sweep)
         track slot draws:              drawAeroThumb(...) x2 (thumbs)
         Modifier.aeroGroove(style)
                    │                            │                            │
                    └────────────────────────────┴────────────────────────────┘
                                       Final composited frame (DrawScope)
```

Entry points: `value`/`onValueChange` (AeroSlider), `value: ClosedFloatingPointRange`/
`onValueChange` (AeroRangeSlider), `progress`/no-arg-indeterminate (AeroProgressBar). Processing:
interaction-state collection → pure style resolution → primitive draw calls. Decision points:
`enabled` (disabled branch wins), `hovered`/`pressed`/`focused` precedence (mirrors
`resolveButtonStyle`'s disabled > pressed > hovered > rest chain), `isDragging` (Pattern 3 snap
vs animate). External dependency: M3 `Slider`'s internal layout (AeroSlider only) — everything
else is this library's own primitives.

### Recommended Project Structure

No new files/folders — all three components stay in their existing location:

```
library/src/main/kotlin/com/mordred/aero/
├── theme/
│   └── AeroSurfaceStyle.kt       # + neutralRest() factory (new, ~15 lines)
└── components/range/
    ├── AeroSlider.kt             # render-only edit: M3 slot swap + thumb/track composables
    ├── AeroRangeSlider.kt        # render-only edit: Canvas draw-lambda body only (lines 228-274)
    └── AeroProgressBar.kt        # render-only edit: both overloads' Box→drawAeroSurfaceCore swap
```

### Pattern 1: `neutralRest()` — the new style factory (foundation for all three components)

**What:** A parallel factory to `AeroSurfaceStyle.rest()` that derives fill/bevel from NEUTRAL
tokens (`glassHighlight`/`glassSurface`/`glassBorder`) instead of `primary`.
**When to use:** Every thumb (both sliders) and every inactive groove/bed (all three components).
**Why it must exist (the load-bearing finding):** `AeroOrnamentTokens.derive()` (confirmed by
direct read, `AeroOrnamentTokens.kt:29-49`) derives BOTH `fillSplitTop/Bottom` AND
`bevelLight/bevelShadow` from `base.primary` — so `AeroSurfaceStyle.rest()` is accent-tinted in
every field, not just the fill. Passing `rest()` unmodified to `aeroThumbSurface`/`aeroGroove`
would render an accent-tinted thumb, contradicting D-01.

**Example (UI-SPEC.md's prescribed field derivation — place in `theme/AeroSurfaceStyle.kt`, same
file as `pressedRecess`/`flattenDisabled`/`hoverLighten`, so Phase 19's `AeroSwitch` can reuse it
for its own neutral thumb without a reach-around):**
```kotlin
// theme/AeroSurfaceStyle.kt — new companion factory, alongside `rest()`
public fun neutralRest(base: AeroColorScheme, cornerRadius: Dp = 8.dp): AeroSurfaceStyle {
    val ornaments = base.ornamentOverride ?: AeroOrnamentTokens.derive(base)
    return AeroSurfaceStyle(
        fillTop = base.glassHighlight.lighten(0.12f),
        fillBottom = base.glassSurface.darken(0.06f),
        glossColor = ornaments.glossHighlight,       // already neutral — reuse as-is
        bevelLight = base.glassHighlight.lighten(0.20f),
        bevelShadow = base.glassBorder.darken(0.15f),
        rimColor = ornaments.rimLight,                // already neutral — reuse as-is
        cornerRadius = cornerRadius,
    )
}
```
Exact lighten/darken magnitudes are Claude's discretion (CONTEXT.md), spot-checked on
AeroBlue/AeroDark/Classic at first implementation (PRIM-16 carries forward) — the values above are
the UI-SPEC's concrete starting point, not a hard lock.

### Pattern 2: `AeroSlider` — M3 custom slot wiring (VRNG-01/02/03)

**What:** Swap the plain 6-arg `Slider(...)` call for the `@ExperimentalMaterial3Api` 10-arg
overload, supply `thumb =`/`track =` lambdas that call `aeroThumbSurface`/`aeroGroove` instead of
`SliderDefaults.Thumb`/`Track`.
**When to use:** `AeroSlider.kt` only.
**Example (skeleton — style resolution/state collection omitted for brevity, see Pitfalls for the
hover-wiring gotcha):**
```kotlin
// Source: material3-desktop-1.9.0 sources jar, Slider.kt:271-308 (direct read)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AeroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true,
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val state = rememberAeroInteractionState(interactionSource)   // hovered/pressed/focused

    val thumbStyle = resolveSliderThumbStyle(colors, state.hovered, state.pressed, state.focused, enabled)
    val trackStyle = resolveSliderTrackStyle(colors, enabled)      // accent, no interaction states (VRNG-03 scopes h/p/f to thumb)

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        steps = steps,
        interactionSource = interactionSource,
        thumb = {
            Box(
                Modifier
                    .size(THUMB_DIAMETER)               // read from SliderTokens at impl time, NOT hardcoded from memory
                    .hoverable(interactionSource)        // load-bearing — see Pitfall "M3 default Thumb hover gap"
                    .aeroGlowRing(active = state.focused && enabled, glowColor = colors.borderSelected, cornerRadius = THUMB_RADIUS)
                    .aeroGlowRing(active = state.hovered && enabled, glowColor = AeroOrnamentTokens.derive(colors).hoverGlow, cornerRadius = THUMB_RADIUS)
                    .aeroThumbSurface(thumbStyle)
            )
        },
        track = { sliderState ->
            // sliderState.coercedValueAsFraction available if the active-vs-inactive split needs
            // to be drawn as two adjacent aeroGroove/aeroSurface regions inside one Box; simplest
            // approach: one aeroGroove(neutralRest) full-width Box UNDER an aeroSurface(accentFill)
            // Box sized to sliderState.coercedValueAsFraction, matching AeroRangeSlider's layering.
            Box(Modifier.fillMaxWidth().height(TRACK_HEIGHT).aeroGroove(trackStyle))
        },
        valueRange = valueRange,
    )
}
```

### Pattern 3: `AeroRangeSlider` — direct-call Canvas restyle (VRNG-04/05)

**What:** Replace ONLY the draw calls inside the existing `Canvas(...) { ... }` block
(`AeroRangeSlider.kt:228-274`) — inactive `drawLine` → `drawAeroGroove`, active `drawLine` →
`drawAeroSurfaceCore` (accent style), `drawCircle` pairs → `drawAeroThumb`. The
`.pointerInput { awaitPointerEventScope { ... } }` block (lines 180-227) and every logic function
above it (`snapToStep`, `applyThumbMove`, `xToValue`, `valueToX`, `thumbToDrawFirst`) are byte-for-
byte untouched.
**When to use:** `AeroRangeSlider.kt` only.
**Per-thumb hover/press (VRNG-05) — no existing per-thumb Modifier chain on a Canvas.** The file
already tracks `activeThumb: RangeThumb?` (set during drag) — this covers PRESS but not HOVER
(mouse-over without a button down). Two independent `MutableInteractionSource`s must be created
and updated from the SAME `pointerInput` block using `PointerEventType.Move`/`Enter`/`Exit`
comparison against each thumb's computed x-position (mirror the existing nearest-thumb-to-`down`
logic at lines 188-196, but evaluated on hover-move, not just press). Feed each source through its
own `rememberAeroInteractionState()` call, matching each thumb's `drawThumb(x)` closure to its own
resolved style.
**Example (draw-block swap only):**
```kotlin
// Source: this project's existing drawAeroGroove/drawAeroSurfaceCore/drawAeroThumb (PRIM-05/07/08)
// 1. Inactive track (full width) — was: drawLine(colors.borderDefault, ..., strokeWidth = trackThickness)
drawAeroGroove(neutralTrackStyle, cornerPx = trackThickness / 2f)

// 2. Active track between thumbs — was: drawLine(colors.primary, ..., strokeWidth = trackThickness)
//    translate/scale the DrawScope to the [startX, endX] sub-region, or draw a sized sub-rect —
//    drawAeroSurfaceCore takes cornerPx only, it draws across `size` (the full receiver bounds),
//    so use `translate(left = startX) { drawAeroSurfaceCore(accentStyle, cornerPx) }` sized to
//    (endX - startX) via `clipRect`/a scoped DrawScope, mirroring how AeroPanelGroup or other
//    Canvas consumers constrain sub-region draws (verify exact idiom at implementation — this is
//    the one part of the render swap without a byte-identical Phase 16/17 precedent to copy).
drawAeroSurfaceCore(accentTrackStyle, cornerPx = trackThickness / 2f) // within a translated/clipped sub-scope

// 3. Thumbs — was: drawCircle(...) + drawCircle(..., style = Stroke(...))
fun drawThumb(x: Float, style: AeroSurfaceStyle) {
    translate(left = x - thumbRadius, top = centerY - thumbRadius) {
        drawAeroThumb(style, radiusPx = thumbRadius)
    }
}
```

### Pattern 4: `AeroProgressBar` — determinate + indeterminate restyle (VRNG-06/07/08)

**What:** Replace `Modifier.background(colors.surface, RoundedCornerShape(50))` /
`Modifier.background(colors.primary, RoundedCornerShape(50))` with `Modifier.aeroGroove(style)` /
`Modifier.aeroSurface(style, shape)` respectively — no Canvas needed, these are still Box-owning
so the Modifier-exposure path (not the direct-call path) applies here, unlike `AeroRangeSlider`.
**When to use:** `AeroProgressBar.kt`, both overloads.
**Example:**
```kotlin
// Determinate — was: .background(colors.surface, RoundedCornerShape(50)) (bed)
//                     .background(colors.primary, RoundedCornerShape(50)) (fill)
val cornerPx = height / 2  // pill shape — MUST derive from the same `height` param, never re-literal (Pitfall 5)
Box(Modifier.fillMaxWidth().height(height).aeroGroove(neutralBedStyle(colors, cornerPx))) {
    Box(
        Modifier
            .fillMaxWidth(clamped)
            .height(height)
            .aeroSurface(fillStyle, RoundedCornerShape(cornerPx))   // fillStyle = rest() when enabled
    )
    if (showRunningSheen) {
        // separate overlay draw, gated by new `showRunningSheen: Boolean = false` param (VRNG-07/D-05)
    }
}

// Indeterminate — was: BoxWithConstraints { ... .background(colors.primary) } sweeping Box
// Same drawAeroSurfaceCore accent style, but the segment's own leading/trailing edges must
// ADDITIONALLY fade via a horizontal alpha mask (D-06) — drawAeroSurfaceCore's own gradient only
// handles the vertical fill/gloss, not a horizontal edge-fade, so this needs an extra
// Brush.horizontalGradient overlay or a masked draw, evaluated at implementation (Claude's
// discretion per D-06, reviewed on three themes). Timing unchanged: infiniteRepeatable(
// tween(1500, LinearEasing), RepeatMode.Restart) — RepeatMode.Reverse is BANNED (VRNG-08).
```

### Anti-Patterns to Avoid

- **Reusing `SliderDefaults.Thumb`'s shrink-on-press sizing:** M3's own default thumb halves its
  width on press/drag (`Slider.kt:1228-1233`) — this directly contradicts D-04 ("stays raised,
  never shrinks/inverts"). Do not adapt this pattern even partially.
- **Deriving `cornerPx` independently from the clip shape's own corner value:** every primitive's
  KDoc calls this out explicitly (16-RESEARCH.md Pitfall 5) — `AeroProgressBar`'s `height / 2`
  pill radius must be the SAME value fed to both the clip `Shape` and `drawAeroSurfaceCore`'s
  `cornerPx`, never two independently-`.toPx()`'d numbers.
- **`aeroSurface(...).aeroGlowRing(...)` ordering (reversed):** `aeroSurface`'s internal
  `.clip(shape)` erases everything drawn after it in the chain — `aeroGlowRing` MUST come first
  (`Modifier.aeroGlowRing(...).aeroThumbSurface(...)`, not the reverse). This is the literal D-03
  "load-bearing check."
- **Reaching for `Modifier.aeroSurface`/`Modifier.aeroGroove` inside `AeroRangeSlider`'s Canvas:**
  these are Box-owning exposures (they call `.clip()` + `drawWithCache` on a `Modifier` chain) —
  inside an existing `Canvas` draw lambda you are ALREADY inside a `DrawScope`, so use the direct-
  call functions `drawAeroSurfaceCore`/`drawAeroGroove`/`drawAeroThumb` instead (internal-visibility,
  same package `com.mordred.aero.theme` — confirm package-visibility compiles from
  `com.mordred.aero.components.range`; if not, may need the functions promoted or a Modifier-path
  fallback verified at implementation).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Neutral-vs-accent glass surface geometry | A second gradient/bevel/rim implementation for "neutral" surfaces | `neutralRest()` factory feeding the EXISTING `drawAeroSurfaceCore`/`aeroThumbSurface`/`aeroGroove` | Same rule PRIM-07/08 already state — a different `AeroSurfaceStyle` VALUE, not a new drawing primitive |
| Hover/press/focus blur "glow" | A blur shader / extra composited layer around the thumb | `Modifier.aeroGlowRing(active, glowColor, cornerRadius)` | Already the library's poor-man's-blur solution (concentric ring bloom); zero Skia coupling risk, documented in `AeroSurfacePrimitives.kt:142-176` |
| Disabled-state fade | `.copy(alpha = 0.4f)` overlay (what all three components currently do) | `AeroSurfaceStyle.flattenDisabled(base)` | D-07 explicitly forbids the alpha-fade path — it doesn't work correctly on Classic's opaque tokens (PRIM-01/14), and doesn't produce the "dead" flattened-geometry look the button precedent established |
| Pressed/dragged glow intensification | A bespoke second glow-ring implementation for "drag" state | Same `aeroGlowRing` call with a brighter `glowColor` (e.g. `hoverGlow.lighten(0.15f)`) and `active = pressed || isDragging` | One primitive, parameterized by which token/boolean feeds it — matches D-04's UI-SPEC-prescribed contract exactly |
| Animation-vs-drag value conflict | A custom "don't animate while touched" flag system | Pattern 3 (`animateFloatAsState` reading a target-only value + `snap()` while `isDragging`) | Already solved and proven in `AeroPanelGroup.kt:443-451`; VRNG-09 explicitly mandates reuse |

**Key insight:** This entire phase is "wire existing primitives with a new style value," not "build
new rendering." The only genuinely new code is `neutralRest()` (a data-value factory, ~15 lines)
and the per-thumb hover-tracking logic on `AeroRangeSlider`'s Canvas (state bookkeeping, not
drawing).

## Common Pitfalls

### Pitfall 1: M3's custom `thumb =` slot loses hover for free
**What goes wrong:** VRNG-03's hover state silently never activates on `AeroSlider`'s thumb.
**Why it happens:** M3's `SliderDefaults.Thumb` internally calls `.hoverable(interactionSource)`
(`Slider.kt:1237`/`1294`) — this is NOT wired anywhere else in `SliderImpl`'s outer layout (press,
drag, and focus ARE wired at the outer `Layout` level via `.then(press).then(drag)` and
`.focusable(...)`, but hover is thumb-composable-scoped only). Confirmed by direct read — this is
the single most important non-obvious finding in this research.
**How to avoid:** Apply `Modifier.hoverable(interactionSource)` explicitly inside the custom
`thumb =` lambda's own Box modifier chain, using the SAME `interactionSource` instance passed to
the outer `Slider(...)` call.
**Warning signs:** Hover glow never appears on `AeroSlider`'s thumb during manual/showcase
testing despite press/focus glow working correctly.

### Pitfall 2: `@ExperimentalMaterial3Api` opt-in required for the custom-slot overload
**What goes wrong:** Compile error `This material API is experimental...` on the `thumb =`/
`track =` `Slider` overload.
**Why it happens:** Confirmed in the pinned 1.9.0 sources (`Slider.kt:272`, `@ExperimentalMaterial3Api`
annotation) — already hit and documented by the PRIM-18 spike (`ScratchSliderSlotSpike.kt:78-84`,
"DISCREPANCY NOTE").
**How to avoid:** Add `@OptIn(ExperimentalMaterial3Api::class)` to `AeroSlider`'s composable
function, exactly as the spike file does.
**Warning signs:** N/A — this is a hard compile-time gate, not a runtime surprise.

### Pitfall 3: `AeroRangeSlider`'s active-fill segment is a sub-region, not the full Canvas
**What goes wrong:** Naively calling `drawAeroSurfaceCore(accentStyle, cornerPx)` inside the
Canvas draws the accent gradient across the FULL Canvas width, not just the `[startX, endX]`
active span between the two thumbs (the original code uses `drawLine(start = Offset(startX, ...),
end = Offset(endX, ...))`, which is inherently sub-region-aware; `drawAeroSurfaceCore` draws a
`drawRoundRect` sized to the receiver `DrawScope`'s FULL `size`, with no offset/width parameters).
**Why it happens:** `drawAeroSurfaceCore`'s signature is `(style, cornerPx)` — no offset/size
params; it was designed for Box-owning callers where the Box's own size/position already IS the
sub-region (e.g. `AeroProgressBar`'s inner fill `Box.fillMaxWidth(clamped)`).
**How to avoid:** Use `translate(left = startX, top = ...) { ... }` combined with a scoped/clipped
sub-`DrawScope` (e.g. via `clipRect` or a nested `Canvas`-equivalent inset) sized to
`(endX - startX)` so `drawAeroSurfaceCore`'s internal `size.width` reads as the active span's
width, not the full track width. Verify the exact idiom (there is no byte-identical precedent in
this codebase for "call a full-bounds-drawing primitive against a sub-rect of a larger Canvas") —
flag as a first-implementation spot-check.
**Warning signs:** The active accent fill renders across the entire track regardless of thumb
positions.

### Pitfall 4: `steps` becoming a dead param on `AeroSlider` (VRNG-02's explicit forbidding)
**What goes wrong:** Custom `track =` slot draws the active/inactive split from raw
`value`/`valueRange` math instead of `sliderState.coercedValueAsFraction`, silently ignoring
step-snapping visualization (the thumb still snaps correctly since `SliderState` owns that, but
the visual fill fraction could drift from the snapped position if computed independently).
**How to avoid:** Always read the fill fraction FROM the `sliderState: SliderState` parameter the
`track =` lambda receives (`sliderState.coercedValueAsFraction`), never re-derive it from the
outer `value`/`valueRange` closure variables.
**Warning signs:** Visual fill position doesn't match thumb position at a stepped value.

### Pitfall 5: PITFALL-03 — `detectDragGestures` banned on Desktop Canvas (carried forward, unchanged)
**What goes wrong:** Any temptation to "clean up" `AeroRangeSlider`'s manual
`awaitPointerEventScope` loop during the restyle touches load-bearing drag-precision logic.
**Why it happens:** M3 `RangeSlider` and gesture-detector drag helpers have an 18dp Desktop
touchSlop that silently swallows the first pixels of pixel-precise drags (documented in
`AeroRangeSlider.kt:140-143`'s own KDoc).
**How to avoid:** VRNG-04 is explicit — this phase is RENDER-ONLY on `AeroRangeSlider`. Do not
touch `.pointerInput { awaitPointerEventScope { ... } }` (lines 180-227) at all, even to add
per-thumb hover detection (VRNG-05) — ADD new state/logic alongside it, don't refactor the
existing drag loop's structure.
**Warning signs:** Any diff touching `AeroRangeSlider.kt` lines 180-227 in a way that changes drag
timing/precision behavior, not just adds parallel hover bookkeeping.

### Pitfall 6: `PRIM-13` — no per-frame Brush rebuild on the indeterminate/sheen animated paths
**What goes wrong:** A naive "just call `drawAeroSurfaceCore` inside the animated Composable body"
rebuilds all of `drawAeroSurfaceCore`'s `Brush.verticalGradient(...)` calls every single animation
frame (indeterminate runs continuously; sheen, when enabled, also runs continuously).
**Why it happens:** `drawAeroSurfaceCore` itself already builds its brushes inline per invocation
(it's designed to be called from inside a `drawWithCache { onDrawBehind { ... } } ` block by its
callers, not to cache internally) — the CALLER is responsible for the `drawWithCache` wrapper.
**How to avoid:** Ensure the indeterminate/sheen animated draw call sites still route through
`Modifier.aeroSurface(...)`'s existing `drawWithCache` wrapper (which the Box-owning
`AeroProgressBar` uses) rather than a bespoke `drawBehind {}` — only the ANIMATED offset (the
sweep position) should change per frame; the gradient/geometry construction should stay cached
per the style value, matching PRIM-13's existing contract.
**Warning signs:** Frame-rate drop specifically during indeterminate/sheen animation, not present
during static determinate rendering.

### Pitfall 7: `Color.Transparent` anti-pattern on the indeterminate segment's edge-fade (PRIM-14)
**What goes wrong:** The indeterminate sweep segment's leading/trailing edge fade (D-06) is
implemented with a hardcoded `Color.Transparent` gradient stop.
**Why it happens:** It's the "obvious" way to fade an edge to nothing in a `Brush.horizontalGradient`.
**How to avoid:** Fade to `baseColor.copy(alpha = 0f)` — same rule as every other gradient in
`drawAeroSurfaceCore`/`drawAeroGroove`. On `AeroColorScheme.Classic` (fully-opaque tokens),
`Color.Transparent` renders a flat colored block instead of a soft fade.
**Warning signs:** Classic theme's indeterminate segment shows a hard-edged rectangle instead of a
soft-fading sweep, while AeroBlue/AeroDark look correct.

### Pitfall 8: Repro-must-exercise-the-path (v2.0.3/v2.0.4 institutional lesson)
**What goes wrong:** A regression guard (source-scan test or otherwise) that's written but never
proven to actually fail on broken code gives a false-positive sign-off.
**Why it happens:** Documented project history — `AeroPanelGroupRecomposeUiTest` initially passed
even with the bug present in v2.0.3.
**How to avoid:** Any new source-scan/value-level test this phase adds (mirroring
`AeroButtonSurfaceSourceTest.kt`/`AeroButtonStylesTest.kt`'s pattern) must be temporarily broken
(revert the fix locally), confirmed to FAIL, then the fix restored and the test confirmed to PASS
— exactly as `17-03-SUMMARY.md`'s "Guard Fail-Then-Pass Proof" documents for VBTN-03/06.
**Warning signs:** A test file added without any commit-message/summary evidence of a fail-then-
pass proof cycle.

## Code Examples

### `resolveButtonStyle`-equivalent resolver shape (mirror for the slider thumb)

```kotlin
// Source: library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:244-279
// (direct project read) — the exact precedence chain to mirror for AeroSlider/AeroRangeSlider
// thumb style resolution: disabled wins over everything; press over hover; focus carries no fill
// delta (expressed purely via the persistent aeroGlowRing focus call).
internal fun resolveSliderThumbStyle(
    colors: AeroColorScheme,
    hovered: Boolean,
    pressed: Boolean,
    isDragging: Boolean,   // D-04: press/drag treated the same — thumb stays raised either way
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.neutralRest(colors, cornerRadius = THUMB_RADIUS)
    if (!enabled) return rest.flattenDisabled(colors)
    return when {
        pressed || isDragging -> rest.copy(glossAlpha = rest.glossAlpha + 0.06f)  // D-04: brighter gloss, NOT pressedRecess
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
```

### Modifier-ordering contract (verbatim precedent, MUST match on the thumb)

```kotlin
// Source: library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:95-105
// (direct project read) — aeroGlowRing (focus, then hover) BEFORE the single aeroSurface/
// aeroThumbSurface call. This ordering is load-bearing (D-03's own explicit check).
.aeroGlowRing(active = state.focused && enabled, glowColor = colors.borderSelected, cornerRadius = 4.dp)
.aeroGlowRing(active = state.hovered && enabled, glowColor = AeroOrnamentTokens.derive(colors).hoverGlow, cornerRadius = 4.dp)
.aeroSurface(style, RoundedCornerShape(4.dp))
```

### Pattern 3 (VRNG-09) — verbatim recipe to copy

```kotlin
// Source: library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt:443-451
// (direct project read) — animateFloatAsState READS the target; it NEVER writes the drag-owned
// state. animationSpec = snap() while isDragging so the value tracks the cursor 1:1.
val animated by animateFloatAsState(
    targetValue = targetPx,
    animationSpec = if (isDragging) snap() else tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "panelHeight_${sections[i].key}",
)
```

### M3 `SliderTokens` dimension constants (direct bytecode+source read, do not re-derive from memory)

```kotlin
// Source: material3-desktop-1.9.0-sources.jar, tokens/SliderTokens.kt (direct extraction + read)
// val HandleWidth = 4.0.dp; val HandleHeight = 44.0.dp   (M3's own default thumb — a tall pill)
// val InactiveTrackHeight = 16.0.dp; val ActiveTrackHeight = 16.0.dp
// val HandleShape = ShapeKeyTokens.CornerFull
// These are M3's OWN defaults — AeroSlider's actual thumb/track dp values are NOT these (per
// 18-UI-SPEC.md's Spacing Scale, AeroSlider's own custom-slot sizing is Claude's discretion within
// existing default sizes and must itself be re-verified against the compiled artifact at
// implementation time per VER-03/TOOL-07 discipline — do not assume these M3 defaults are what
// AeroSlider should render at).
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| M3 `SliderColors` alpha-based disabled (`alpha = 0.4f`) | `AeroSurfaceStyle.flattenDisabled(base)` geometry-flatten | This phase (D-07) | `SliderColors` becomes vestigial for `AeroSlider` — still passed as a required param to the M3 `Slider(...)` call (type requirement) but its color values are never read once custom `thumb=`/`track=` slots are supplied |
| M3 6-arg `Slider(value, onValueChange, modifier, enabled, valueRange, steps, colors, interactionSource)` overload | 10-arg `@ExperimentalMaterial3Api` overload w/ `thumb=`/`track=` | This phase (VRNG-01) | Requires `@OptIn(ExperimentalMaterial3Api::class)`; the slot API *shape* itself is long-stable since pre-1.4 per 16-RESEARCH.md, only the experimental annotation is new friction |
| Flat `drawLine`/`drawCircle` on `AeroRangeSlider`'s Canvas | `drawAeroGroove`/`drawAeroSurfaceCore`/`drawAeroThumb` direct calls | This phase (VRNG-04) | Zero change to drag precision/timing — purely what gets drawn under the same coordinates |

**Deprecated/outdated:** None — this phase doesn't touch any deprecated API surface; the M3
`Slider` custom-slot API, while `@ExperimentalMaterial3Api`, is not deprecated (it's the forward-
looking, actively-maintained slot mechanism per the PRIM-18 spike's bytecode findings).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `AeroSlider`'s active/inactive track split can be rendered as two layered `aeroGroove`/`aeroSurface` Boxes inside the `track =` slot (mirroring `AeroRangeSlider`'s two-segment approach) rather than needing a bespoke masked single-draw | Pattern 2 | If M3's `track =` slot sizing/placement doesn't accommodate two overlapping child Boxes cleanly, an alternate single-Canvas-inside-the-slot approach may be needed — verify at first implementation alongside the D-03 glow-clipping check |
| A2 | `drawAeroGroove`/`drawAeroSurfaceCore`/`drawAeroThumb` (package-`internal` visibility in `com.mordred.aero.theme`) are callable from `com.mordred.aero.components.range.AeroRangeSlider` (different package) | Anti-Patterns, Pitfall 3 | Kotlin `internal` visibility is module-scoped, not package-scoped, so this should compile fine (both are in the `library` module) — flagged as [ASSUMED] only because it wasn't empirically compile-tested in this research session, unlike the M3 slot mechanics which WERE verified via extracted sources |
| A3 | Neutral-glass thumb magnitude values (`glassHighlight.lighten(0.12f)` etc., UI-SPEC's suggested starting point) will read correctly as "silvery/neutral" and visually distinct from the accent fill on all three themes without further tuning | Pattern 1 | Low risk — explicitly flagged as Claude's-discretion-with-three-theme-review in both CONTEXT.md and UI-SPEC.md, not a hidden assumption |
| A4 | The indeterminate segment's horizontal edge-fade (D-06) needs a second, separate `Brush.horizontalGradient` alpha-mask overlay beyond `drawAeroSurfaceCore`'s own vertical gradient — no existing primitive combines both axes | Pattern 4 | If wrong (e.g. a simpler compositing trick works), extra unnecessary drawing complexity only — not a correctness risk |

**If this table is empty:** N/A — see rows above. All core M3-mechanics and Phase 16/17 helper-name
claims in this document ARE verified via direct source reads (sources jar extraction, project file
reads) and are NOT tagged `[ASSUMED]`.

## Open Questions

1. **`AeroSlider`'s current public API has no `onValueChangeFinished` parameter — does VRNG-02
   require ADDING one, or is the requirement's mention of it aspirational/inapplicable?**
   - What we know: `AeroSlider.kt`'s current signature is `(value, onValueChange, modifier,
     enabled, valueRange, steps, showTooltip)` — no `onValueChangeFinished`. VRNG-02's Russian
     text lists "onValueChangeFinished" among the behaviors that "must not regress," and
     `SliderState` (which the new custom-slot overload requires) has an
     `onValueChangeFinished: (() -> Unit)?` constructor param that currently has nowhere to flow
     from since `AeroSlider` doesn't expose it.
   - What's unclear: Whether this is (a) a documentation artifact referring to internal
     `SliderState` wiring that stays `null`/unused (no regression possible since it never existed
     publicly), or (b) an implicit ask to add the parameter as new public API — which would
     conflict with the phase's "API/behavior stays 1:1" milestone constraint (CONTEXT.md "In
     scope" preamble).
   - Recommendation: Treat as (a) — do not add new public API surface. Wire
     `onValueChangeFinished = null` through to `SliderState`'s constructor (matching current
     behavior exactly), satisfying VRNG-02's "not regressed" framing without expanding the public
     signature. Flag for a quick confirm at plan-check if the planner reads this differently.

2. **Exact sub-region draw idiom for `AeroRangeSlider`'s active-fill segment (Pitfall 3).**
   - What we know: `drawAeroSurfaceCore(style, cornerPx)` draws across the full receiver `size`;
     no existing call site in the codebase constrains it to a sub-rect of a larger Canvas.
   - What's unclear: Whether `translate(left = startX) { ... }` combined with a `size`-shrunk
     nested scope (there's no direct `DrawScope.size` override without a `clipRect`/`inset`/
     custom nested-canvas wrapper) is the cleanest idiom, or whether `AeroRangeSlider` should
     switch to drawing the active segment as its own `Modifier.aeroSurface` Box positioned via
     `offset`/`width` (like `AeroProgressBar`'s fill Box) INSTEAD of a Canvas draw call — a
     partial architecture change but still "render-only."
   - Recommendation: Prototype both at first implementation; prefer whichever keeps
     `AeroRangeSlider` a single unified `Canvas` (matching its current PITFALL-03-driven all-
     Canvas structure) unless the sub-region Canvas approach proves awkward, in which case the
     Box-overlay approach (still zero drag-logic change) is an acceptable fallback.

## Environment Availability

Skipped — this phase has no external tool/service/runtime dependencies beyond the already-pinned,
already-available Compose Multiplatform Desktop 1.11.1 + Material3 1.9.0 toolchain (Phase 15
completed). No new CLI, database, or service dependency is introduced.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | `kotlin.test` + JUnit Jupiter (JUnit Platform), already configured (`library/build.gradle.kts:31,32,35,45`) |
| Config file | `library/build.gradle.kts` (`useJUnitPlatform()`, line 45) — no separate config file |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.range.*"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| VRNG-01/03 | New `resolveSliderThumbStyle`-equivalent resolver returns correct rest/hover/press/disabled `AeroSurfaceStyle` per state, across AeroBlue + Classic | unit (value-level, no Compose runtime) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroSliderStylesTest"` | ❌ Wave 0 — mirror `AeroButtonStylesTest.kt`'s exact pattern |
| VRNG-02 | Existing `AeroRangeSliderTest.kt` logic tests (`snapToStep`/`applyThumbMove`/`xToValue`/`valueToX`) still pass unchanged — proves render-only edit didn't touch logic | unit (pre-existing) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroRangeSliderTest"` | ✅ exists (`library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderTest.kt`) |
| VRNG-01/02 | `AeroSlider.kt` source contains the custom-slot `Slider(...)` call (not the plain 6-arg overload) and does NOT reference `SliderColors.disabled*` fields (D-07's "do not rely on M3 SliderColors for disabled") | unit (source-scan guard) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroSliderSourceTest"` | ❌ Wave 0 — mirror `AeroButtonSurfaceSourceTest.kt`'s file-content-assertion pattern |
| VRNG-04 | `AeroRangeSlider.kt`'s `.pointerInput { awaitPointerEventScope { ... } }` block (drag logic) is byte-identical to pre-restyle source — a diff-based or line-range source-scan guard | unit (source-scan / diff guard) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroRangeSliderDragLogicUntouchedTest"` | ❌ Wave 0 — new test, compare against a captured pre-restyle snapshot of lines 180-227 (or the extracted logic functions, already covered by the existing `AeroRangeSliderTest.kt`) |
| VRNG-07/08 | `AeroProgressBar`'s indeterminate animation spec is still `tween(1500, LinearEasing)` + `RepeatMode.Restart` (never `Reverse`), and the new `showRunningSheen` param defaults to `false` | unit (source-scan guard + default-value assertion) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroProgressBarSourceTest"` | ❌ Wave 0 |
| VRNG-09 | Pattern 3 usage (if a new `animateFloatAsState` is added for thumb glow/gloss transitions) reads `isDragging`-gated `snap()` correctly | unit (value-level, if a pure function is extracted) or manual/showcase review | manual — three-theme showcase review (this is a visual/timing behavior, not easily unit-testable without extracting pure state logic) | manual-only — justified: the animation-vs-drag interplay is inherently a Compose-runtime timing behavior; `AeroPanelGroup`'s own precedent (`PanelGroupLogicTest.kt`) tests the pure logic functions, not the `animateFloatAsState` wiring itself |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test --tests "com.mordred.aero.components.range.*"`
- **Per wave merge:** `./gradlew :library:test` (full suite, currently 232+ tests per STATE.md)
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt` — covers
      VRNG-01/03, mirrors `AeroButtonStylesTest.kt`'s value-level resolver-testing pattern exactly
      (construct expected `AeroSurfaceStyle` via `AeroSurfaceStyle.neutralRest(...)` + transforms,
      assert equality against the resolver's output, across `AeroColorScheme.AeroBlue` and
      `AeroColorScheme.Classic`)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt` — covers
      VRNG-01/02/D-07, mirrors `AeroButtonSurfaceSourceTest.kt`'s file-content-assertion pattern
      (read `AeroSlider.kt`'s source as a string, assert it contains the custom-slot call shape and
      does NOT contain `SliderColors(` disabled-alpha usage)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt` —
      covers VRNG-07/08 (RepeatMode.Restart present, RepeatMode.Reverse absent, 1500 present,
      `showRunningSheen` param defaults false)
- [ ] Per this project's own repro-must-exercise-the-path discipline (Pitfall 8 above): every new
      source-scan test above must be proven to FAIL against a deliberately-reintroduced violation
      before being trusted as a real gate — document the fail-then-pass proof in the plan's
      SUMMARY, mirroring `17-03-SUMMARY.md`'s precedent.

## Security Domain

Not applicable — `security_enforcement` is not set in `.planning/config.json` (absent = enabled by
default per this project's own convention), but this phase has NO input-handling, auth, session,
access-control, or cryptography surface. It is a pure rendering restyle of three UI controls that
already accept/emit `Float`/`ClosedFloatingPointRange<Float>` values with existing
`coerceIn`/clamping logic untouched by this phase. No ASVS category applies.

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | no (pre-existing `coerceIn`/`snapToStep` clamping untouched by this render-only phase) | — |
| V6 Cryptography | no | — |

### Known Threat Patterns for {stack}

None applicable — desktop Compose UI rendering, no network/data-persistence/auth surface touched.

## Sources

### Primary (HIGH confidence)

- `material3-desktop-1.9.0-sources.jar` (extracted directly, `Slider.kt` full read) — custom
  `thumb=`/`track=` overload signature, `SliderImpl`'s modifier-wiring order (press/drag/focus at
  outer Layout, hover only inside default `Thumb`/`Track` composables), `SliderTokens` dimension
  constants
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` (direct project read,
  full file) — `drawAeroSurfaceCore`, `Modifier.aeroSurface`, `Modifier.aeroGlowRing`,
  `drawAeroThumb`/`Modifier.aeroThumbSurface`, `drawAeroGroove`/`Modifier.aeroGroove`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` (direct project read, full
  file) — `AeroSurfaceStyle` data class, `rest()`, `pressedRecess`, `flattenDisabled`,
  `hoverLighten`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` (direct project read, full
  file) — confirmed `bevelLight`/`bevelShadow` are `primary`-derived, the finding driving
  `neutralRest()`'s necessity
- `library/src/main/kotlin/com/mordred/aero/components/common/InteractionStates.kt` (direct project
  read, full file) — `rememberAeroInteractionState`, `AeroInteractionState`, `ANIMATION_DURATION_MS`
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` (direct project
  read, full file) — `resolveButtonStyle`'s precedence chain, modifier-ordering precedent
- `library/src/main/kotlin/com/mordred/aero/components/range/{AeroSlider,AeroRangeSlider,
  AeroProgressBar}.kt` (direct project reads, full files) — current-state structure for all three
  components
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` (targeted read,
  lines 436-465) — Pattern 3's exact code shape
- `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt` (direct project
  read, full file) — PRIM-18 spike's PASS verdict and bytecode-inspection evidence
- `.planning/phases/18-range/18-CONTEXT.md`, `.planning/phases/18-range/18-UI-SPEC.md` (direct
  reads, both already checker-approved / user-locked)

### Secondary (MEDIUM confidence)

- `library/src/test/kotlin/com/mordred/aero/components/buttons/{AeroButtonStylesTest,
  AeroButtonSurfaceSourceTest}.kt`, `library/src/test/kotlin/com/mordred/aero/components/range/
  AeroRangeSliderTest.kt` (direct project reads) — test-pattern precedents for the Validation
  Architecture section

### Tertiary (LOW confidence)

None — every claim in this document traces to a direct file/source read performed during this
research session (project source, extracted M3 sources jar, or the already-approved
CONTEXT.md/UI-SPEC.md).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies, exact pinned versions confirmed by direct
  `build.gradle.kts` read
- Architecture: HIGH — M3 slot mechanics confirmed via extracted sources jar (not memory), Phase
  16/17 primitive signatures confirmed via direct file reads
- Pitfalls: HIGH — the hover-wiring gap (Pitfall 1) and sub-region-draw gap (Pitfall 3) were
  discovered BY this research session via direct source inspection, not carried forward from
  training-data assumptions

**Research date:** 2026-07-23
**Valid until:** 30 days (stable, already-pinned toolchain; no external API surface expected to
shift within this milestone's timeframe)

## RESEARCH COMPLETE
