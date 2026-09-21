# Feature Research — Aero Visual Vocabulary (v2.1 Glass Refinement)

**Domain:** Desktop UI component library — Windows 7 Aero / Vista Aero visual language, applied to Compose Desktop
**Researched:** 2026-07-21
**Confidence:** MEDIUM-HIGH (device catalog) / MEDIUM (per-component proposals, original synthesis for "Aero spirit, modern execution")

> This file answers the milestone question directly: Part A is the device catalog, Part B is per-component treatment, Part C is the interaction-state matrix. The generic "Table Stakes / Differentiators / Anti-Features" template sections are populated with **visual devices**, not product features, per the milestone's actual scope.

---

## Cross-Theme Finding (read from `AeroColorScheme.kt` before proposing anything)

`AeroBlue` and `AeroDark` use **alpha-transparent** tokens for `glassSurface`/`glassBorder`/`glassHighlight` (e.g. `Color(0x30FFFFFF)`), but **`Classic` uses fully opaque solid grays** for the same tokens (e.g. `glassSurface = Color(0xFF333333)`). This is very likely intentional — `Classic` is the non-glass "Windows Classic" alternative, not a third Aero skin.

**Implication for every device below:** raised/sunken **bevel** devices (light-top-edge / dark-bottom-edge rim) read correctly in all three themes — they're the literal pre-Aero Windows 3D-bevel language, so `Classic` gets an authentic flat-3D look "for free." **Gloss/translucency/hover-glow** devices depend on real alpha blending to look glassy; over `Classic`'s opaque tokens they will still render (alpha-white-over-opaque still lightens) but will read as a plain highlight, not glass — which is correct/expected for that theme, not a bug. Any new lighten/darken helper introduced in the Foundation phase must work by RGB channel mixing (mix toward `Color.White`/`Color.Black`), not only by alpha compositing, so it produces a usable result on both alpha and opaque token sets. This is a shared-primitive dependency for **every** component below, not just one.

---

## PART A — The Aero Device Catalog

Each device is specified precisely enough to implement: geometry, gradient stops, alpha, layering. All numeric alphas/proportions below are **original design proposals** for this codebase (LOW confidence as literal Windows 7 pixel values, since Microsoft never published pixel-level msstyles specs; MEDIUM confidence as faithful-in-spirit recreations, cross-checked against the community recreation projects `7.css` and `PresentationTheme.Aero` — see Sources). The official MS Learn "Aero Style Classes, Parts, and States" page (HIGH confidence) confirms which parts/states exist but not their pixel rendering, since real Aero used PNG-based `.msstyles` art rather than a documented gradient algorithm.

### A1. Two-tone / split gradient fill — TABLE STAKES

**What:** a 4-stop vertical `Brush.verticalGradient` in two bands with a soft internal seam, not two flat halves.
- Stop 0.0f: base color lightened +18% toward white (RGB mix, not alpha) — the "raised top."
- Stop 0.48f: base color, unmodified.
- Stop 0.52f: base color, unmodified (the tiny 0.04f gap is the "seam" — see A2).
- Stop 1.0f: base color darkened −12% toward black — the "shaded bottom."
- The split sits at ~50% height, not top-weighted like Material's subtle tonal elevation gradients — Aero's split is visually close to center, which is what reads as "glass rod" rather than "flat tint."

**Complexity:** LOW (single `Brush.verticalGradient` with 4 `colorStops`).
**Dependency:** needs `Color.lighten(amount)`/`Color.darken(amount)` RGB-mix helpers (see Cross-Theme Finding) added to the Foundation phase — `.copy(alpha=)` alone cannot brighten a color, only fade it.

### A2. The seam — DIFFERENTIATOR

**What:** a single 1px horizontal line at the 50% split, color = base color lightened +6%, alpha 0.5, drawn as a 1.dp-tall `drawRect` (or a 5th gradient stop pair placed at `0.499f`/`0.501f` with a brief spike). This is the "two pieces of glass glued together" cue that separates Aero from a generic soft gradient. Cheap: one extra draw call, no blur.
**Complexity:** LOW.
**Dependency:** A1.

### A3. Top specular gloss highlight — DIFFERENTIATOR (already partially built, needs fixing)

**What:** an elliptical/curved highlight sitting in the top 40–55% of the control height, brightest at top-center, tapering to transparent — not the current linear-only `glassPanel` gradient (which is already the right idea but the sibling `glassSurface` has a hardcoded `endY = 100f` **pixel** value, a confirmed baseline bug — must become `size.height * 0.5f` or a parameter).
- Height: 45% of control height for buttons/switch/thumbs; up to 55% for large flat panels (already correct in `glassPanel`).
- Shape: for small controls (buttons, thumbs, switch), approximate the curve with a `drawOval` inset 8–10% from left/right edges and clipped to the control's own rounded-rect shape, rather than a full-width rectangle — this is what makes it read as "glass" instead of "a lighter stripe."
- Alpha: 0.35 at the highlight's own top, fading to 0 by the 45–55% mark. Use `glassHighlight` token as the color, not a hardcoded white.
**Complexity:** LOW (oval fill) to MEDIUM (if clipped precisely to a rounded-rect shape via `Path.op`).
**Dependency:** fix to `glassSurface`'s `endY` bug is a prerequisite (Foundation phase item, already identified in STATE.md baseline).

### A4. Inner rim light / bevel — TABLE STAKES

**What:** two 1.dp strokes, both **inside** the outer contour (A5), one per half of the control's perimeter:
- Top/left half: color = highlight (base +25% toward white), alpha 0.5, inset 1.dp from the outer edge.
- Bottom/right half: color = shadow (base −20% toward black), alpha 0.3, inset 1.dp from the outer edge.
- Implementation: two half-perimeter `Path` strokes (one for the top-left arc, one for bottom-right arc of the rounded rect), OR the cheaper approximation already partly present as the concept behind A1's gradient — but a literal 1px rim reads noticeably crisper than a fill gradient alone and is what separates "gradient rectangle" from "beveled chrome."
**Complexity:** MEDIUM (path-based half-stroke) — flag a LOW-complexity fallback: a single inset stroke drawn with a `Brush.verticalGradient` (light→dark) instead of two separate paths achieves ~90% of the visual for much less code, and is the recommended default.
**Dependency:** shares the `Color.lighten/darken` helper from A1.

### A5. Outer contour / border — TABLE STAKES

**What:** crisp 1.dp stroke at the exact control bounds (not bounds-centered and then clipped away, which is the current confirmed `glassSurface` bug — the stroke must be drawn either before `.clip()` with `.border()`, or inset by 0.5px so its outer half survives clipping). Color = `glassBorder` token, alpha as defined per theme (already correctly opaque-enough in all three themes). Corner radius matches the control's own shape.
**Complexity:** LOW (this is a bug fix to existing code, not a new device).
**Dependency:** none — Foundation-phase fix, blocks every component that currently uses `glassSurface`.

### A6. Hover glow — DIFFERENTIATOR

**What:** NOT a flat white rect over the whole bounding box (the confirmed `AeroButton` bug — corners get squared off because the hover overlay ignores the shape's own clip). Two-part device:
1. Fill brighten: mix the base fill +8% toward white (uses A1's helper), applied only within the already-clipped shape.
2. Outer glow ring: a second 1.5–2.dp stroke drawn just outside A5's contour, color = `primary` mixed toward white, alpha 0.25, optionally doubled with a second wider/fainter stroke (3.dp, alpha 0.12) to fake a soft blur without a real `RenderEffect` blur (keeps cost to 2 extra stroke draws, no GPU blur pass — matches the "single `drawBehind` block, avoid overdraw/iGPU collapse" performance constraint already locked in PROJECT.md).
**Complexity:** LOW-MEDIUM.
**Dependency:** A1 helper; must be clipped to shape (fixes the AeroButton corner-square bug as a side effect).

### A7. Pressed / inset state — TABLE STAKES

**What:** gradient inversion, not just a uniform darken. Swap A1's stop order (dark at top, light at bottom) so the control reads as pushed into the surface. Combine with: gloss (A3) alpha reduced to ~0.15 (glass catches less light when depressed), seam (A2) alpha reduced to ~0.25. Optionally combine with the existing 0.97 scale-down animation already used by `AeroButton` — the two devices are complementary (scale = physical give, inversion = light response) and neither alone reads as strongly as both together.
**Complexity:** LOW (same gradient helper, reordered stops via a `pressed: Boolean` branch).
**Dependency:** A1.

### A8. Focus indication — DIFFERENTIATOR

**What:** Aero idiom is a soft glow ring, not a hard uniform-width square-cornered border (the current `AeroButton` focus implementation uses a hardcoded `RoundedCornerShape(4.dp)` border regardless of the button's actual shape — a second confirmed mismatch bug once corner radii diverge across components). Proposed: two concentric strokes outside A5 — inner 1.5.dp crisp stroke, color `borderSelected`, alpha 0.9; outer 3.dp stroke, same color, alpha 0.25 (same soft-glow-via-double-stroke trick as A6, reusable helper). Corner radius must always be read from the control's own shape parameter, never hardcoded.
**Complexity:** LOW.
**Dependency:** shares the double-stroke glow helper with A6 — recommend building ONE shared `Modifier.aeroGlowRing(color, shape, intensity)` primitive in the Foundation phase and calling it for both hover and focus with different color/intensity, rather than writing the glow twice.

### A9. Drop shadow usage — controls vs. panels — TABLE STAKES (controls) / ANTI-FEATURE (panels)

**What:** small interactive controls (buttons, switch thumb, slider thumb) get a tight, small shadow: 2–3.dp blur, 1.dp Y-offset, alpha 0.20–0.25, `color = Color.Black` — this is what makes them read as sitting above the surface. Large panels/surfaces (`glassPanel`, cards) should **not** get an equivalent shadow per control instance — that was already a locked v1.0-era decision (glass panels get depth from the gradient/border alone) and remains correct; the `elevation` parameter on `glassEffect` is currently dead code (imports `shadow` but never applies it) and should either be wired up ONLY for small-control use or removed/renamed to avoid implying panel-level shadow support that isn't wanted.
**Complexity:** LOW (real `Modifier.shadow()` is fine at this small scale/count — 8 components, not hundreds of list rows all shadowed at once. `AeroListItem`, which CAN appear hundreds of times in a `LazyColumn`, should NOT get a per-row `shadow()` — use the rim-stroke-only look instead, no blur shadow, to protect virtualized-list performance.)
**Dependency:** none, but must be scoped correctly (see per-component notes) to avoid the exact overdraw/iGPU risk already flagged as a project-level performance constraint.

### A10. Track "groove" / inset well — TABLE STAKES (for Slider, RangeSlider, ProgressBar, Switch track)

**What:** the visual inverse of A1 — a recessed channel. 3-stop vertical gradient: darker at the very top edge (base −15% toward black), lighter by mid-height (base, unmodified), very slightly lighter at the bottom edge (base +4%) — the bottom gets a hint of bounce-light the way a real carved channel would. Add a 1.dp inner-top stroke, color = shadow tone, alpha 0.35 (the "the wall of the groove casts a shadow into the channel" cue) and NO gloss oval (A3) — grooves don't get a specular highlight, only surfaces that face the viewer/light do. Pill shape (`RoundedCornerShape(50)`) for Slider/RangeSlider/ProgressBar tracks; the Switch's off-track is the same device.
**Complexity:** LOW.
**Dependency:** A1's lighten/darken helper, reused inverted.

### A11. Win7 progress bar specifics — DIFFERENTIATOR (fill gradient + static gloss) / RISK-FLAGGED (animated sheen)

- **Fill body:** A1's two-tone gradient using `primary` as base, NOT flat. A static top gloss line (A3, but simplified to a flat 2px highlight line rather than a full oval, since the fill region is thin) sits along the top edge of the filled portion only.
- **Animated sheen sweep (differentiator, use sparingly):** a soft-edged bright band (linear gradient: transparent → white@0.35 → transparent, ~35–45% of the bar's width) that sweeps left-to-right periodically — NOT continuously. Real Windows 7/Vista behavior sweeps once every few seconds with idle time between passes, not a tight seamless loop. Recommend: `keyframes` spec with a hold segment — animate the sweep position 0→1 over ~1200ms, then hold at "off-screen" for ~2500ms before repeating, `RepeatMode.Restart`, total cycle ~3700ms. This is cheap (one float animation, gated to visible determinate bars only) and avoids the anti-feature risk below.
- **Anti-feature risk:** an always-on, tight-loop shimmer (like the *current* indeterminate implementation's 1500ms `RepeatMode.Restart` with no pause) reads as a loading skeleton, not Aero polish, if applied to every determinate bar in a dense UI (e.g. a DataTable full of progress cells). Recommend making the sheen an explicit opt-in parameter (default on for a single/hero progress bar use case, but flag this as a requirements-stage decision, not a foregone default) rather than unconditional.
- **Marquee/indeterminate:** authentic Win7 marquee is a single glossy block bouncing back and forth (ping-pong), not a one-directional restart loop. The **current** implementation (30%-wide bar, 1500ms linear `RepeatMode.Restart`) is closer to a generic Material marquee than Aero. Two options for requirements stage: (a) restyle only (keep the loop timing, apply A1+A3 gradient/gloss to the moving block) — LOW complexity, "modern execution" reading; (b) switch to ping-pong bounce with `RepeatMode.Reverse` and `FastOutSlowInEasing` — slightly more Aero-authentic but edges toward literal pixel-recreation, which the user's fidelity target explicitly deprioritizes. **Recommendation: option (a).**
**Complexity:** MEDIUM (gradient+gloss layering is simple; the two concurrent animations — width/position for determinate value changes plus the periodic sheen — need careful `LaunchedEffect`/`InfiniteTransition` composition so they don't fight).
**Dependency:** A1, A10, the shared thumb/fill primitive proposed for Slider (component reuse opportunity — see Part B).

### A12. Slider thumb ("pointer") shape and dimensionality — DIFFERENTIATOR

**What:** literal Win7 `TKP_THUMB` was a flag/arrow-shaped pointer (asymmetric, pointed toward the track) in the base Windows Classic style; Aero's rendition softened this into a rounded glossy nub, closer to a small button than an arrow. Given the "Aero spirit, modern execution" target, recommend NOT recreating the arrow/flag silhouette (would tip into dated skeuomorphism) — instead: a circular/oval glossy thumb, 16–18.dp diameter, using the SAME raised-surface language as the button (A1 gradient, A3 gloss oval, A4 bevel rim, A5 contour, A9 small drop shadow). This deliberately unifies "anything you can grab and drag" (Slider/RangeSlider thumb, Switch thumb) under one shared visual primitive rather than inventing a bespoke shape per component.
**Complexity:** MEDIUM (Canvas-drawn circle with all A1–A5+A9 layers — moderate code, but should be ONE shared private helper, not duplicated per component).
**Dependency:** A1, A3, A4, A5, A9 — this is the "raised thumb" shared primitive referenced repeatedly in Part B.

### A13. Other ornamentation

- **Corner radius norms — TABLE STAKES:** Aero buttons/panels used *small-to-moderate* rounding (roughly 2–6px at 96dpi, i.e. small in Compose `dp` terms too), NOT full pill shapes. Pill/fully-rounded shapes are correct ONLY for track-style controls (slider/progress/switch track) which is where Aero itself used them (Zune/WMP-era sliders, volume controls). **Anti-feature: turning `AeroButton` into a pill.** That reads as iOS/Material, not Aero, and is the single fastest way to lose "recognizably Aero" — corner radius should stay in the 4–6.dp range for buttons/segmented control/list-item selection pill, not 50%.
- **Separator/divider treatment — DIFFERENTIATOR, LOW complexity:** the same light-top/dark-bottom double-1px-line idea from A4 doubles as a divider device (e.g. between `AeroSegmentedControl` segments) — draw a 1px highlight line immediately followed by a 1px shadow line rather than a single flat divider color. Cheap, reuses no new primitive.
- **Reflections (mirrored bottom-edge glass reflection) — ANTI-FEATURE for these 8 components.** Real Aero taskbar buttons/window chrome sometimes got a faint upward mirror reflection; applying it to small form controls (buttons, switches, list rows) is the classic "2007 Web 2.0 badge" over-reach the milestone explicitly wants to avoid. Skip entirely except optionally as a single very-subtle 1px lighter line at the very bottom of the `AeroListItem` selection pill (not a full mirror) — flagged as optional/low-priority, not required.
- **Noise/grain texture — ANTI-FEATURE.** Not actually part of the *control* rendering language in real Aero (grain/noise was more associated with some third-party Vista Basic themes, not stock Aero glass); adding it here would be pure invented skeuomorphism, plus a real per-pixel noise draw is expensive at Compose Desktop's `drawBehind` scale for hundreds of `AeroListItem` rows. Do not implement.
- **Real backdrop blur — ANTI-FEATURE, already out of scope per PROJECT.md** ("Настоящий DWM Aero blur через JNI/WinAPI — симуляция через градиенты визуально достаточна"). All of the above devices simulate glass through gradient/alpha, never through an actual blur pass. Reinforcing this here because a per-component temptation to add `RenderEffect.createBlurEffect` for hover glow specifically should be resisted — A6/A8's double-stroke trick achieves a "soft glow" look without a real blur.

---

## PART B — Per-Component Treatment

Layering order is listed bottom (drawn first) → top (drawn last). Complexity and dependencies are called out per item so the requirements stage can scope REQ-IDs and the roadmapper can order the enabling phase correctly.

### B1. `AeroButton`

**Layering (bottom→top):** A9 shadow → A1 two-tone fill (clipped to shape) → A10 n/a → A2 seam → A4 inner bevel rim → A3 top gloss oval (clipped) → A5 outer contour → [state layer: A6 hover glow OR A7 pressed inversion, mutually exclusive] → A8 focus glow ring (drawn outside A5, not clipped) → content (text).

| Device | Category | Notes |
|---|---|---|
| A1 two-tone fill | Table stakes | Replaces flat `primary@0.8f` M3 fill |
| A2 seam | Differentiator | Cheap, high signal |
| A3 top gloss | Table stakes | Fixes the current complete absence of gloss |
| A4 inner bevel | Table stakes | This + A1 are what most separates it from M3 `Button` |
| A5 outer contour | Table stakes | Currently absent at rest (M3 filled button has no border) |
| A6 hover glow | Differentiator | Also a **bug fix**: current hover draws an unclipped rect over square bounds even though the button is rounded |
| A7 pressed inversion | Differentiator | Complements existing 0.97 scale, doesn't replace it |
| A8 focus glow | Table stakes | Also a **bug fix**: current focus border is a hardcoded 4.dp corner radius, independent of the button's actual shape |
| A9 shadow | Differentiator | Small/tight only — do not oversize |

**Complexity:** MEDIUM. Full custom `drawBehind`/Canvas replacing M3's `containerColor` styling; state logic (hover/pressed/focused/disabled) already exists via `rememberHoverState`/`rememberPressedState`/`rememberFocusState` — only the drawing changes, not the interaction plumbing.
**Dependencies:** Foundation-phase `Color.lighten/darken` helper (A1), shared `aeroGlowRing` primitive (A6/A8), `glassSurface`/`glassEffect` bug fixes (A3 `endY`, A5 clip-order, A9 dead `elevation` param) MUST land first since this component will be the first consumer that exercises all of them.

### B2. `AeroOutlinedButton`

**Layering:** A9 (very light shadow, optional) → A1 fill but at low alpha (glass tint, e.g. `glassSurface` gradient rather than opaque `primary`) → A4 (subtle, lower contrast than filled button) → A3 (thin gloss line, alpha ~0.2, secondary-action restraint) → A5 outer contour (this IS the current 1.dp border, upgrade only) → A6/A7 state → A8 focus.

| Device | Category | Notes |
|---|---|---|
| A1 low-alpha fill | Table stakes | Currently fully transparent — reads as "M3 outlined," needs *some* glass tint at rest |
| A5 contour | Table stakes | Already present (1.dp `glassBorder`) — keep, just make sure it survives clip (A5 bug) |
| A3/A4 | Differentiator | Keep intensity lower than `AeroButton` — this is deliberately the "quieter" sibling |
| A6/A7/A8 | Table stakes | Currently **zero** — same bug class as `AeroButton`'s hover |

**Complexity:** LOW-MEDIUM. Can reuse the exact same shared drawing primitive as `AeroButton` with a `filled: Boolean` / `fillAlpha` parameter rather than a separate implementation — recommend building ONE internal `AeroButtonSurface` composable/modifier consumed by both public components.
**Dependencies:** same as B1; should literally share code with B1, not duplicate it (risk otherwise: the two buttons visually drift apart over time, same class of risk called out in `project_panelgroup_composable_dsl_pitfall`-style lessons about shared logic).

### B3. `AeroSwitch`

Largest gap: zero border/shadow/gloss AND zero hover/press/focus states.

**Layering — track (off):** A10 groove (recessed, pill) → A5 thin contour.
**Layering — track (on):** A1 raised two-tone fill using `primary` (pill) → A2 seam (optional, pill tracks are thin — may omit if visually cluttered at 18.dp height) → A3 thin gloss line → A5 contour.
**Layering — thumb (always):** A12 shared raised-thumb primitive (A1+A3+A4+A5+A9 circle) sized to fit the 18.dp track (thumb ~14.dp, matching current size).
**State layer:** A6 hover glow ring around the thumb only (not the whole track) → A7 pressed = thumb gradient invert + scale 0.92 → A8 focus glow around the whole track (not just the thumb, since focus targets the whole control) → disabled = flatten everything to single-tone @0.4 alpha, remove A3/A6/A7/A8 entirely.

| Device | Category | Notes |
|---|---|---|
| A10 groove (off-track) | Table stakes | Currently a flat `borderDefault`-colored box |
| A1 raised fill (on-track) | Table stakes | Currently a flat `primary`-colored box |
| A12 thumb | Table stakes | Currently a flat circle, no shadow/bevel/gloss at all |
| A6 hover | Table stakes (gap) | **Currently 100% absent** — biggest single gap of the eight components |
| A7 pressed | Table stakes (gap) | **Currently 100% absent** |
| A8 focus | Table stakes (gap) | **Currently 100% absent** — switch isn't even focusable-styled today |

**Complexity:** MEDIUM. No Win7 switch precedent exists (correctly noted in the milestone brief) — this is the one component built by extrapolation rather than direct reference, but every device used (groove, raised thumb, hover glow, pressed invert, focus glow) is drawn from the existing catalog, so it should still feel visually "of a piece" with the other seven.
**Dependencies:** A10 groove primitive (shared with ProgressBar/Slider track), A12 thumb primitive (shared with Slider/RangeSlider thumb) — **this component cannot be implemented before the shared groove and thumb primitives exist**, making it a natural "second wave" component after Button/OutlinedButton prove the Foundation layer.

### B4. `AeroSegmentedControl`

Ancestor: Office 2007 ribbon toggle-button groups / Win7 toolbar tab strips, not a literal Win7 control.

**Layering — outer container:** A5 contour around the whole strip (upgrade from the current plain 1.dp) → optional very subtle A1 fill for the whole strip background (raised toolbar look) at low alpha.
**Layering — per unselected segment:** transparent at rest; A6 hover glow **clipped to that segment's own sub-rect** (currently: no hover at all, and even if added naively would need clipping per-segment, not the whole row).
**Layering — selected segment:** here the metaphor diverges from the button: real ribbon/toolbar "pressed/active" toggle buttons render as a **recessed** glassy highlight, not a raised bevel — reuse A7 (pressed-style inversion) as the AT-REST look for the selected segment, plus a persistent (non-hover-gated) low-intensity glow ring (A8-style, but using `primary` not `borderSelected`, to denote "active" rather than "focused") so the selected segment stays visually distinct even without focus.
**Layering — separators between segments:** A13 double-line groove divider (1px light + 1px dark), not a flat single-color line.
**State layer:** A6 hover on unselected segments; A8 real keyboard-focus glow around the currently-focused segment (independent from the "active" glow the selected segment always shows).

| Device | Category | Notes |
|---|---|---|
| A5 contour (whole strip) | Table stakes | Already present, needs bug-fix parity with A5 |
| A6 hover (per segment) | Table stakes (gap) | **Currently 100% absent** |
| A7-as-selected-state | Differentiator | Deliberate reuse of the "pressed" device as a persistent "active" look — worth flagging to requirements stage as a design call, not a Windows-verified fact |
| A13 divider | Differentiator | Cheap, meaningfully upgrades the currently flat `1.dp` separators |
| A8 focus (per segment) | Table stakes (gap) | **Currently 100% absent** |

**Complexity:** MEDIUM. Per-segment clipping for hover/active states is the main new work; the shared "recessed" gradient can be the exact same code path as `AeroButton`'s A7 pressed state, parameterized rather than reimplemented.
**Dependencies:** A6/A8 glow primitive, A7 inversion helper (shared with Button) — should not require any NEW primitive beyond what B1/B2 already need, making this a good "third wave" component (visual-only, reuses everything).

### B5. `AeroSlider`

Currently a thin M3 `Slider` wrapper — full custom rewrite, following the same rationale already used for `AeroRangeSlider` (M3 banned per locked `PITFALL-03`: `detectDragGestures`/M3 slider internals don't give the drag control needed; `awaitPointerEventScope` + manual loop is the locked pattern).

**Layering — track (unfilled portion, right of thumb):** A10 groove.
**Layering — track (filled portion, left of thumb):** A1 raised two-tone fill using `primary`, thin static A3 gloss line along the top edge (same idea as A11's progress-bar fill, in fact this should share code with `AeroProgressBar`'s fill renderer).
**Layering — thumb:** A12 shared raised-thumb primitive.
**State layer:** A6 hover glow around thumb only → A7 pressed = thumb scale-down + gradient invert, track-fill brightens slightly → A8 focus glow around thumb → disabled = flatten all, 0.4 alpha, no gloss/glow.

| Device | Category | Notes |
|---|---|---|
| A10 groove | Table stakes | Currently M3-drawn flat track |
| A1 filled-portion gradient | Table stakes | Currently M3-drawn flat filled track |
| A12 thumb | Table stakes | Currently M3-drawn flat circle |
| A6/A7/A8 on thumb | Table stakes (gap) | M3 gives *some* default ripple/hover but none of it is Aero-styled; effectively a gap for this milestone's purposes |

**Complexity:** HIGH — this is the only one of the eight requiring both a full M3→custom rewrite AND new drag-handling code (not purely a visual reskin like B3/B4). Mitigated by directly reusing `AeroRangeSlider`'s existing `awaitPointerEventScope` drag pattern (already proven, already passes the project's locked Canvas-drag pitfall guard) rather than inventing a new one.
**Dependencies:** A10 groove, A12 thumb (shared with Switch/RangeSlider), the existing `aeroDragSplitter`-style drag utility pattern from `RangeSlider`/`SplitPane`/`ColorPicker`/`DataTable` (already a locked v2.0 convention, reusable as-is). **This is the component most likely to need its own research-phase deep-dive at planning time** given the M3-removal scope, even though the visual devices themselves are fully specified here.

### B6. `AeroRangeSlider`

Already Canvas-based (no M3 to remove) — **pure visual upgrade**, drag logic untouched.

**Layering — track (below/above/between thumbs):** A10 groove for the two end segments; A1 raised fill for the segment BETWEEN the two thumbs (the "selected range").
**Layering — thumbs (×2):** A12 shared raised-thumb primitive, replacing the current flat `drawCircle` + 2.dp ring.
**State layer:** A6/A7/A8 **per thumb independently** (currently: zero hover/press states on either thumb — a confirmed gap in the baseline findings). The existing drag tooltip (`glassEffect` pill, already implemented) is unaffected by this milestone.

| Device | Category | Notes |
|---|---|---|
| A10 groove (outer segments) | Table stakes | Currently flat 4.dp `drawLine` |
| A1 raised fill (between-thumb segment) | Table stakes | Currently flat 4.dp `drawLine`, same color as the rest of the track — no visual distinction of the selected range beyond color |
| A12 thumbs | Table stakes | Currently flat `drawCircle` + ring, must literally share the drawing helper with B5's thumb to avoid the two sliders drifting apart visually |
| A6/A7/A8 per-thumb | Table stakes (gap) | **Currently 100% absent on both thumbs** |

**Complexity:** MEDIUM (lower than B5 — visual-only, no drag rewrite, no M3 removal).
**Dependencies:** A10, A12 (must be literally shared, not reimplemented, with B5) — recommend this be the SECOND consumer of the shared thumb/groove primitives (right after B5 builds them), so any bugs found get fixed once for both sliders.

### B7. `AeroProgressBar`

Two variants (determinate value-based overload, indeterminate marquee overload) — both currently flat `Box`es.

**Layering — determinate:** A10 groove (track) → A1 raised fill for the progress amount → static A3 gloss line along top of the filled portion → optional A11 periodic sheen sweep (flag as opt-in per A11's anti-feature-risk note, default recommendation deferred to requirements stage).
**Layering — indeterminate:** A10 groove (full-width, always visible as "empty") → the moving 30%-wide block restyled with A1+A3 (currently a flat `colors.primary` box) — keep the existing 1500ms `RepeatMode.Restart` timing (A11's "option (a): restyle only" recommendation) rather than switching to ping-pong bounce.

| Device | Category | Notes |
|---|---|---|
| A10 groove | Table stakes | Currently flat `colors.surface` box, no inset/recessed cue at all |
| A1 fill gradient | Table stakes | Currently flat `colors.primary` box |
| A3 static gloss (fill top edge) | Differentiator | Cheap, high visual payoff for a control with almost no other detail |
| A11 periodic sheen | Differentiator (risk-flagged) | See A11 — recommend opt-in, not default-on everywhere; genuine risk of "loading skeleton" look at high multiplicity |
| Indeterminate restyle | Table stakes | Timing unchanged, only the moving block's fill gets A1+A3 |

**Complexity:** MEDIUM (gradient/gloss layering is simple; if the opt-in sheen is built, two concurrent animation states need clean composition).
**Dependencies:** A1, A10 — should share the exact fill-gradient renderer with B5's filled-track segment (same visual language: "a raised bar of primary color inside a recessed groove").

### B8. `AeroListItem`

Reference point given directly by the user: Win7 Explorer selection = rounded translucent blue pill with lighter rim + subtle gradient.

**Layering:** clip the ENTIRE row background draw to an inset rounded rect (e.g. 2.dp horizontal inset from the row's full bounds, 8.dp corner radius) — this alone fixes the confirmed baseline bug ("not clipped → hard-edged full-bleed flat highlight").
- **Selected (not hovered):** A1-style two-tone glass pill using `primary` at low alpha (top stop ~0.22, bottom stop ~0.14 — NOT opaque, this is a translucent highlight over content, not a filled button) → A5-equivalent 1.dp rim, color `primary` at alpha ~0.5 (lighter/more saturated than the fill, matching the "lighter rim" the user specifically called out) → thin A3 gloss line (alpha ~0.15, 40% height — subtle, this is a list row not a button).
- **Hovered (not selected):** flatter single-tone tint derived from `buttonHover`, same clip/inset, a lighter 1.dp rim but NO gloss line — hover should read as "lighter/quieter" than selected, not competing with it.
- **Selected AND hovered (the confirmed baseline gap — "selected beats hover so hovering a selected row shows nothing"):** selected pill stays but its rim brightens slightly (alpha 0.5→0.65) and gloss alpha bumps slightly (0.15→0.22) — a real, visible delta between "resting-selected" and "hovering-a-selected-row," which currently does not exist at all.
- **Focused:** thin 1.dp inner glow ring (A8-style but restrained — 1.dp inset, no double-stroke blur trick needed at this small a scale), currently completely absent.

| Device | Category | Notes |
|---|---|---|
| Clip to inset rounded rect | Table stakes | This single fix (not even a new device) resolves the "square corners on a rounded design system" bug |
| A1-style low-alpha pill (selected) | Table stakes | Directly matches the user-supplied Explorer reference |
| Lighter rim on pill | Table stakes | Explicitly called out by the user as part of the reference |
| A3 gloss (selected only) | Differentiator | Subtle — this is the one place a full gloss line risks looking excessive; keep alpha low |
| Combined selected+hover delta | Table stakes (gap) | **Currently 100% absent** — closes a named, explicit baseline bug |
| Focus ring | Table stakes (gap) | **Currently 100% absent** |
| Full shadow (A9) on the row | Anti-feature (here) | Rows can appear hundreds of times in a virtualized list — a per-row blurred `shadow()` is a real overdraw risk; rim-stroke only, no shadow |

**Complexity:** LOW-MEDIUM. The drawing itself is simple (one clipped background swap); the main nuance is the state-combination logic (rest/hover/selected/selected+hover/focused/disabled all need distinct, correctly-prioritized branches — currently `selected` short-circuits `hovered` entirely with no combined case).
**Dependencies:** the corrected `glassSurface` (A5 clip-order bug fix) is the natural primitive to reuse here, parameterized with the selected-pill's specific fill/rim colors — this component should be a straightforward "final wave" consumer of the Foundation layer, no new primitive required beyond what buttons/sliders already establish.

---

## Feature Dependencies

```
[Color.lighten/darken RGB-mix helper]  (Foundation)
    ├──required by──> [A1 two-tone fill]
    ├──required by──> [A4 inner bevel rim]
    ├──required by──> [A7 pressed inversion]
    └──required by──> [A10 track groove]

[glassSurface bug fixes: endY proportional, clip-order, elevation wiring]  (Foundation)
    ├──blocks──> AeroButton (B1)          — first real consumer, surfaces any remaining bugs
    ├──blocks──> AeroOutlinedButton (B2)
    └──blocks──> AeroListItem (B8)        — reuses glassSurface directly, parameterized

[Modifier.aeroGlowRing shared primitive]  (Foundation, built alongside A6/A8)
    ├──required by──> AeroButton hover + focus (B1)
    ├──required by──> AeroOutlinedButton hover + focus (B2)
    ├──required by──> AeroSwitch hover + focus (B3)
    ├──required by──> AeroSegmentedControl hover + focus (B4)
    ├──required by──> AeroSlider hover + focus on thumb (B5)
    ├──required by──> AeroRangeSlider hover + focus per thumb (B6)
    └──required by──> AeroListItem focus ring (B8)

[Shared "raised thumb" primitive (A12)]
    ├──required by──> AeroSwitch thumb (B3)
    ├──required by──> AeroSlider thumb (B5)
    └──required by──> AeroRangeSlider thumbs ×2 (B6)

[Shared "track groove" primitive (A10)]
    ├──required by──> AeroSwitch off-track (B3)
    ├──required by──> AeroSlider unfilled track (B5)
    ├──required by──> AeroRangeSlider outer segments (B6)
    └──required by──> AeroProgressBar track (B7)

[Shared "raised fill / active segment" renderer (A1 applied to a fill region)]
    ├──required by──> AeroSlider filled track (B5)
    ├──required by──> AeroRangeSlider between-thumb segment (B6)
    └──required by──> AeroProgressBar determinate fill (B7)

[AeroButton (B1) surface primitive]  ──enhances/is reused by──> AeroOutlinedButton (B2)
[AeroButton (B1) A7 pressed-inversion code]  ──reused as "active" state by──> AeroSegmentedControl selected segment (B4)
[AeroRangeSlider (B6) existing awaitPointerEventScope drag loop]  ──pattern reused by──> AeroSlider (B5) full rewrite

[A11 periodic sheen animation utility]  ──optional enhancement of──> AeroProgressBar (B7) only
```

### Dependency Notes

- **Foundation-first is confirmed correct** (already locked in `.planning/STATE.md`'s v2.1 scoping decisions, mirroring the successful v2.0 Phase 7 enabling-phase pattern): the `Color.lighten/darken` helper, the fixed `glassSurface`/`glassEffect`, and the shared `aeroGlowRing` primitive are each consumed by 5+ of the 8 components. Building any component before these exist means rework.
- **Thumb and groove primitives (A10/A12) gate three components each** (`AeroSwitch`, `AeroSlider`, `AeroRangeSlider` for the thumb; `AeroSwitch`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar` for the groove) — these should be built once, in the Foundation phase or immediately after it, not per-component. This is the single highest-leverage shared-primitive investment in the whole milestone.
- **`AeroSlider` (B5) is the outlier** — it is the only component requiring an actual M3-removal + new drag-handling implementation (HIGH complexity vs. MEDIUM/LOW for the rest). It should be sequenced to directly follow `AeroRangeSlider`'s existing pattern rather than being built in isolation, and may warrant its own phase-level research pass given the drag-handling risk class already documented (`PITFALL-03`).
- **`AeroButton`/`AeroOutlinedButton` should share one internal surface primitive**, not two independent implementations — same rationale as every other "shared code, not shared visual coincidence" lesson already in this project's memory (PanelGroup DSL lesson: divergent copies of the same logic silently drift).

---

## MVP Definition (mapped to phase-ordering intent, not literal ship gates)

### Foundation wave (must land first — blocks everything else)
- [ ] `Color.lighten/darken` RGB-mix helpers
- [ ] `glassSurface` bug fixes: proportional `endY`, correct clip/border order, wired-up `elevation`
- [ ] `Modifier.aeroGlowRing(color, shape, intensity)` shared hover/focus primitive
- [ ] Shared "raised thumb" drawing primitive (A12)
- [ ] Shared "track groove" drawing primitive (A10)
- [ ] New `AeroColorScheme` tokens as needed for lightened/darkened/glow variants per theme (verify all three themes, not just AeroBlue, per the Cross-Theme Finding)

### Second wave (straightforward consumers of the Foundation layer)
- [ ] `AeroButton` — full device set (B1)
- [ ] `AeroOutlinedButton` — shares B1's primitive (B2)
- [ ] `AeroListItem` — reuses fixed `glassSurface`, clip + state-combination fix (B8)

### Third wave (depends on thumb/groove primitives existing)
- [ ] `AeroSwitch` — full device set incl. first-ever hover/press/focus (B3)
- [ ] `AeroRangeSlider` — visual-only upgrade, no drag rewrite (B6)
- [ ] `AeroSegmentedControl` — visual-only, reuses Button's pressed/glow code (B4)

### Fourth wave (highest complexity, sequence last within the milestone)
- [ ] `AeroSlider` — full M3 removal + custom drag (B5)
- [ ] `AeroProgressBar` — including the opt-in periodic sheen decision (B7)

### Explicit decision points to carry into REQUIREMENTS.md
- [ ] `AeroProgressBar` periodic sheen: opt-in parameter vs. always-on (A11) — recommend opt-in
- [ ] `AeroProgressBar` indeterminate: restyle-only (keep current 1500ms loop) vs. ping-pong bounce (A11) — recommend restyle-only
- [ ] `AeroSegmentedControl` selected-segment metaphor: recessed/"active" (recommended, B4) vs. raised/"elevated" — pick one, don't mix
- [ ] `AeroListItem` bottom-edge mirror reflection: optional/low-priority, likely defer

---

## PART C — Interaction-State Matrix

States: **Rest** / **Hover** / **Pressed** / **Focused** / **Disabled** / **Selected-or-Active** (where applicable). "GAP" = confirmed zero implementation today per the STATE.md baseline table.

| Component | Rest | Hover | Pressed | Focused | Disabled | Selected/Active |
|---|---|---|---|---|---|---|
| **AeroButton** | A1 two-tone fill + A2 seam + A3 gloss + A4 bevel + A5 contour + A9 shadow | + A6 glow ring, fill brightened +8%, **clipped** (fixes corner-square bug) | A7 gradient inversion, gloss alpha→0.15, seam alpha→0.25, existing 0.97 scale kept | A8 double-stroke glow ring, shape-matched radius (fixes hardcoded-radius bug) | Flatten to single tone @0.4 alpha, no gloss/bevel/shadow | n/a (stateless action button) |
| **AeroOutlinedButton** | A1 low-alpha glass tint fill + thin A3/A4 + A5 contour (upgrade of existing border) | Same A6 pattern as Button, lower intensity | Same A7 pattern, lower intensity | Same A8 pattern as Button | Flatten fill to near-zero alpha, contour @0.4 | n/a |
| **AeroSwitch** | Off: A10 groove + thumb A12. On: A1 raised pill fill + thumb A12 | **[GAP today]** A6 glow ring around thumb only | **[GAP today]** A7 thumb invert + scale 0.92 | **[GAP today]** A8 glow around whole track | Flatten everything @0.4, strip A3/A6/A7/A8 | Track fill state (on/off) IS the "selected" axis — on = A1 raised, off = A10 groove |
| **AeroSegmentedControl** | Unselected: transparent + A5 outer contour. Selected: A7-style recessed fill + persistent low-intensity active glow | **[GAP today]** A6 glow, clipped to that segment only | Momentary A7 on click, same as selected-at-rest look intensified briefly | **[GAP today]** A8 glow around the focused segment specifically (independent of which is selected) | Flatten all segments @0.4, no active glow | Selected segment = persistent A7 recessed fill + always-on low-intensity glow ring (distinct from focus glow) |
| **AeroSlider** | A10 groove (unfilled) + A1 raised fill (filled) + A12 thumb | **[GAP today — M3-drawn]** A6 glow around thumb | **[GAP today]** A7 thumb invert + scale, track-fill brightens slightly | **[GAP today]** A8 glow around thumb | Flatten track + thumb @0.4, no gloss | n/a (continuous value, no discrete selected state) |
| **AeroRangeSlider** | A10 groove (outer segments) + A1 raised fill (between-thumb segment) + A12 thumbs ×2 | **[GAP today]** A6 per-thumb, independently | **[GAP today]** A7 per active thumb (`lastMovedThumb` already tracked in code — reuse for which thumb gets the pressed look) | **[GAP today]** A8 per focused thumb | Flatten all @0.4 | Between-thumb segment IS the "active/selected" range, always shown via A1 raised fill vs. A10 groove on the outer segments |
| **AeroProgressBar** | Determinate: A10 groove + A1 fill + static A3 gloss. Indeterminate: A10 groove + restyled moving block | n/a (non-interactive control) | n/a | n/a (not focusable) | Flatten fill @0.4, freeze/hide sheen and moving block | n/a — "value" is the closest analog to state, already the component's whole purpose |
| **AeroListItem** | Clipped inset rounded rect, transparent bg | Flat single-tone tint (from `buttonHover`), lighter 1.dp rim, no gloss | n/a (click is instantaneous, no distinct pressed visual currently planned — flag as optional) | **[GAP today]** thin 1.dp inner glow ring | Flatten row content @0.4, no rim/gloss | A1-style low-alpha glass pill + lighter rim (alpha 0.5) + subtle A3 gloss; **selected+hover combo [GAP today]** — currently hover renders nothing on a selected row; proposed fix bumps rim/gloss alpha slightly so the combination is visibly distinct from resting-selected |

### Gap Summary (directly from the milestone brief + STATE.md baseline, confirmed by reading each component file)

- **`AeroSwitch`:** zero hover, zero pressed, zero focus. Confirmed in `AeroSwitch.kt` — the composable has no `MutableInteractionSource` parameter at all, no `hoverable`/`focusable` modifiers, only `toggleable`.
- **`AeroSegmentedControl`:** zero hover, zero focus (pressed/selected exist only as a flat color swap).
- **`AeroSlider` / `AeroRangeSlider` thumbs:** zero hover, zero focus on the draggable thumb itself (the slider components have interaction plumbing for drag, but not for hover/focus glow).
- **`AeroListItem`:** zero focus visual; selected+hover is not a distinct state (selected short-circuits hover in the current `when` branch — confirmed in `AeroListItem.kt`'s `animatedBg` calculation).
- **`AeroProgressBar`:** not interactive by nature (no gap here — included in the matrix for completeness/disabled-state parity only).

These five gaps are, per the milestone brief's own framing, "a large part of why they read as flat" — and per this research, all five are closeable using the SAME `aeroGlowRing` primitive (A6/A8) and the SAME state-branch pattern already proven in `AeroButton`'s `rememberHoverState`/`rememberPressedState`/`rememberFocusState` helpers (which exist in the codebase today and are not themselves broken — they're just not called from the five gapped components).

---

## Sources

**Official / HIGH confidence:**
- [Aero Style Classes, Parts, and States — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/controls/aero-style-classes-parts-and-states) — confirms existence and naming of `BP_PUSHBUTTON`, `TKP_THUMB`/`TKP_TRACK` (Trackbar), `PP_FILL`/`PP_TRANSPARENTBAR` (Progress, with `PBFS_ERROR`/`PBFS_PARTIAL`/`PBFS_PAUSED` states), confirming these are real, distinct msstyles parts — not confirming pixel-level rendering, which Microsoft never published (art was PNG-based, not algorithmic).
- [Visual Styles Overview — Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/controls/visual-styles-overview) — `.msstyles` structure, PNG-based art in Vista+.
- [Selection Appearance — MSDN archive design guidelines](https://learn.microsoft.com/en-us/previous-versions/ms997622(v=msdn.10)) — confirms selection highlighting is meant to use system highlight color + a distinguishing border/rim, generic Windows guidance (pre-dates Aero specifically but establishes the "rim distinguishes selection" principle referenced in B8).

**Community recreation / MEDIUM confidence (cross-checked, not official, but widely used and visually validated against real Windows 7):**
- [7.css — CSS framework for recreating Windows 7 UI](https://khang-nd.github.io/7.css/) — confirms button vertical two-shade gradient shifting to blues when pressed, progress bar `animate`/`marquee`/`paused`/`error` states, checkbox/radio "sunken panel" treatment, list "sunken" borders — corroborates the raised/pressed inversion (A7) and groove (A10) devices independently.
- [PresentationTheme.Aero — WPF Aero theme recreation](https://github.com/gix/PresentationTheme.Aero) and [WpfThemeGenerator ButtonChrome.xaml](https://github.com/Athari/WpfThemeGenerator/blob/master/Alba.WpfThemeGenerator/Themes/AeroAlt/Controls/ButtonChrome.xaml) — corroborates that Aero button chrome is a multi-layer gradient+bevel construction (not a flat fill), matching the A1/A4 layering proposed here.
- [Windows Aero — Wikipedia](https://en.wikipedia.org/wiki/Windows_Aero) — general Aero design-language context.

**Original synthesis (this document) / LOW-MEDIUM confidence — explicitly flagged as design proposals, not sourced Windows facts:**
- All specific numeric values (alpha percentages, dp sizes, gradient stop positions, animation durations) in Parts A/B/C are original proposals for `aero-compose-ui`, calibrated to the user's stated "Aero spirit, modern execution" fidelity target rather than literal pixel-measurement of Windows 7. No official Microsoft source publishes exact gradient-stop/alpha values for Aero controls (the visual style shipped as rendered PNG art, not a documented formula), so any claim of exact reproduction would be false confidence — these numbers should be treated as an implementable starting point to be refined during the three-theme visual sign-off, consistent with this project's existing practice (`.planning/STATE.md` shows multiple prior milestones where sign-off caught and fixed visual specifics not fully nailed down in planning).

**Project-internal sources (read per the mandatory files_to_read list):**
- `.planning/PROJECT.md` — milestone scope, fidelity target, out-of-scope boundary (no real DWM blur).
- `.planning/STATE.md` — baseline findings table (per-component current-state gaps), confirmed `GlassModifiers` bugs.
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — confirmed `endY = 100f` hardcoded pixel bug, dead `elevation` param, clip-order border-loss bug.
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — confirmed opaque-vs-alpha token divergence between `Classic` and `AeroBlue`/`AeroDark` (Cross-Theme Finding).
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt`, `.../selection/AeroSwitch.kt`, `.../range/AeroProgressBar.kt`, `.../list/AeroListItem.kt` — confirmed each specific baseline bug cited above (unclipped hover rect, zero-state switch, flat progress fill, unclipped list selection, selected-beats-hover branch).

---
*Feature research for: Windows 7 Aero visual vocabulary applied to `aero-compose-ui` v2.1 Glass Refinement*
*Researched: 2026-07-21*
