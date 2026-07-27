# Phase 19: Selectors + Lists - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-27
**Phase:** 19-selectors-lists
**Areas discussed:** AeroSwitch identity, AeroSwitch states, AeroSegmentedControl segments, AeroListItem pill

---

## AeroSwitch identity

### Where the accent color lives when checked

| Option | Description | Selected |
|--------|-------------|----------|
| Accent in groove, neutral thumb | Track groove filled with accent glass (same material as slider active fill / filled button), thumb stays neutral raised glass. Direct continuation of 18 D-01. | ✓ |
| Accent in thumb, neutral groove | Groove always neutral recessed, thumb carries the accent when checked. Diverges from slider logic; state reads worse on a 14dp circle. | |
| Accent only in the filled portion of the groove | Literal slider port — accent left of the thumb only. Cramped on 36×18 with an 18dp travel. | |

**User's choice:** Accent in groove, neutral thumb → **D-01**

### Unchecked (off) track appearance

| Option | Description | Selected |
|--------|-------------|----------|
| Neutral recessed groove | Same `drawAeroGroove` geometry in both states; only fill color animates at 150ms. One moving part. | ✓ |
| Deeper/darker groove when off | Off reads "empty" via stronger inner shadow + darker fill. Stronger without color (matters on Classic) but two animated params. | |
| Claude decides | Pick on the three-theme review. | |

**User's choice:** Neutral recessed groove → **D-02**

### Where the raised thumb's shadow goes (36×18 track, 14dp thumb, ~2dp headroom, sizes locked)

| Option | Description | Selected |
|--------|-------------|----------|
| Thumb drawn on top, outside the track clip | Groove clips itself; thumb + shadow + glow are a separate unclipped layer above. Physically honest; layout size unchanged. Same principle as 18 D-03. | ✓ |
| Everything strictly inside 36×18 | Nothing exceeds the box; shadow and glow squeezed into ~2dp. Safest for foreign layouts, but the raised look barely registers. | |
| Claude decides on review | | |

**User's choice:** Thumb drawn on top, outside the track clip → **D-03**

### InteractionSource in the public API

| Option | Description | Selected |
|--------|-------------|----------|
| Add trailing param with default | `interactionSource: MutableInteractionSource = remember { … }` on both selectors — additive, source-compatible, matches `AeroListItem`'s existing convention. | ✓ |
| Keep internal, don't touch API | Strictly 1:1 signatures, but `AeroListItem` stays the lone component exposing a source — library stays inconsistent. | |
| Claude decides | | |

**User's choice:** Add trailing param with default → **D-04**

---

## AeroSwitch states

### Press appearance — two locked precedents conflict

| Option | Description | Selected |
|--------|-------------|----------|
| Thumb stays raised, brighter gloss | Slider-thumb precedent (18 D-04): no `pressedRecess`, `glossAlpha += delta`, glow intensifies. `resolveSliderThumbStyle` already implements exactly this. | ✓ |
| Whole switch recesses | Button precedent (17 D-02): `pressedRecess` over the whole pill. But the groove is already recessed — "deeper" doesn't read at 18dp. | |
| Both: thumb brighter + groove deeper | Maximum feedback, but two simultaneous changes on a tiny control risks noise. | |

**User's choice:** Thumb stays raised, brighter gloss → **D-05**

### Where hover glow and lightening go

| Option | Description | Selected |
|--------|-------------|----------|
| Glow on the whole track + thumb lightening | Glow ring outlines the pill (where the cursor actually is, `toggleable`'s hot zone) plus `hoverLighten` on the thumb. Closest to 17 D-03 "the whole control lights up". | ✓ |
| Glow only around the thumb | Matches the slider thumb exactly (18 D-03), but the lit area doesn't match the hover target — hover an edge, the far-end thumb lights up. | |
| Claude decides on review | | |

**User's choice:** Glow on the whole track + thumb lightening → **D-06**

### Animation policy — phases 17–18 snap, phase 19 components currently animate

| Option | Description | Selected |
|--------|-------------|----------|
| Keep 150ms on semantic transitions, states instant | `checked`/`selected` keep `tween(150)` (current behavior, milestone requires 1:1); hover/press/focus resolve instantly like the buttons and sliders. | ✓ |
| Animate everything including hover/press | Closer to Aero, but diverges from two shipped phases and would mean animating a whole `AeroSurfaceStyle` — risk to the `drawWithCache` baseline (PRIM-13). | |
| Drop animation entirely | Full uniformity with 17–18, but changes observable behavior — violates the milestone's "behavior 1:1" constraint. | |

**User's choice:** Keep 150ms on semantic transitions, states instant → **D-07**

---

## AeroSegmentedControl segments

### Unselected segment appearance (selected-is-recessed was already locked)

| Option | Description | Selected |
|--------|-------------|----------|
| Raised glass — a strip of buttons | Unselected resolve the button rest style; selected is `pressedRecess` on the same base. Maximal raised→recessed contrast, literal Win7 toolbar idiom, most direct reuse of Phase 17 code. | ✓ |
| Flat neutral fill inside the frame | Depth only on the selected segment; control stays light. Closer to current behavior. | |
| Unselected stay transparent | Minimal change, but the control risks still reading Material inside. | |

**User's choice:** Raised glass — a strip of buttons → **D-08**

### Keyboard and focus model (segments currently have bare `.clickable`, no role, no focus)

| Option | Description | Selected |
|--------|-------------|----------|
| Per-segment focus via Tab, `role = RadioButton` | `Modifier.selectable(selected, role = Role.RadioButton, …)` — focus, Space/Enter, semantics all free. Matches VBTN-04's "no zero-semantics repeat". Cost: N Tab stops. | ✓ |
| One focus stop, arrow keys select | Standard radiogroup navigation, but needs hand-rolled `onKeyEvent` + custom semantics group — a behavior change, not a visual one. | |
| Focus visual only, semantics untouched | Closes VSEL-04 literally while preserving the accessibility gap the milestone already promised not to repeat. | |

**User's choice:** Per-segment focus via Tab, `role = RadioButton` → **D-09**
**Notes:** Arrow-key roving focus recorded as a deferred idea rather than dropped.

### Hover/focus rendering vs. the parent clip

| Option | Description | Selected |
|--------|-------------|----------|
| In-bounds cues: hover = lightening, focus = inner rim | Everything inside the clip; no outer bloom to be sliced by the frame or bleed onto neighbouring segments. | ✓ |
| Glow on the outer control frame | Consistent with the library's glow language, but doesn't show *which* segment has focus — bad with per-segment Tab. | |
| Hybrid: inner rim + outer ring | Shows both "where am I" and "what's active", but two simultaneous effects risk noise. | |

**User's choice:** In-bounds cues → **D-10**

### Recessed segment's color

| Option | Description | Selected |
|--------|-------------|----------|
| Accent recessed glass | Literally a pressed `AeroButton` — double signal (depth + color), most direct Phase 17 reuse. Text must go light. | |
| Neutral recessed, accent in the text | Depth carries selection; accent stays on the label (as today). Same transform, different base. | |
| Claude decides on review | Pick whichever reads unambiguously with legible text on all three themes. | ✓ |

**User's choice:** Claude decides on review

---

## AeroListItem pill

### Selection pill geometry

| Option | Description | Selected |
|--------|-------------|----------|
| Inset on all four sides | Floating pill, rows don't touch — Win7 Explorer idiom. | |
| Vertical inset only, full width | Edge-to-edge pill with rounded corners; suits lists already inside a padded panel. | |
| Claude decides on review | Pick inset and radius that read correctly in the showcase across three themes. | ✓ |

**User's choice:** Claude decides on review

### Combining hover with selection (VLST-02)

| Option | Description | Selected |
|--------|-------------|----------|
| Same `hoverLighten` on top of either base | Base resolves from `selected`, then `hoverLighten()` composes on top — same shape as `resolveButtonStyle`. Suppression becomes structurally impossible. | ✓ (Claude, per user instruction) |
| Different channels: selection = fill, hover = rim | Two orthogonal signals, obvious combined state, but hover would look unlike hover everywhere else in the library. | |
| Two stacked pill layers | Simple to implement, but a second layer over glass risks muddy color on Classic's opaque tokens. | |

**User's choice:** *"Я абсолютно не понимаю большинства подобных вопросов без визуальных примеров. Либо как-то приводи визуальный пример, либо делай как знаешь и будем править на фазе выполнения."*

**Notes:** The user declined to arbitrate fine-grained rendering questions posed as prose and delegated them, with corrections to happen during execution. Remaining `AeroListItem` decisions (**D-11** transform composition, **D-12** shared pill geometry for hover and selection, **D-13** in-bounds focus visual) were taken by Claude on that authority and are recorded in CONTEXT.md with their rationale. D-11 in particular is structural rather than aesthetic — it removes the bug class VLST-02 describes. Saved as durable feedback: ask identity questions in text, mock up or delegate pixel-level ones.

---

## Claude's Discretion

- Selection-pill inset and corner radius.
- The recessed segment's color (accent-recessed vs. neutral-recessed with accent text).
- Native `dropShadow`/`innerShadow` vs. manual gradient per depth cue (per 16 D-02).
- Groove depth, thumb gloss magnitude, how far the thumb's shadow may exceed 36×18dp.
- Hover-vs-focus differentiation on each component, in-bounds only for the segmented control and list item.
- Whether raised/recessed segment styles are new resolvers or `resolveButtonStyle` with a parameter (constraint: one shared painter).
- Exact `flattenDisabled` values for all three components.
- Fate of the 1dp segment separators once segments have their own contour.

## Deferred Ideas

- Arrow-key roving focus for `AeroSegmentedControl` — behavior change, own future phase.
- `AeroRangeSlider` accessibility semantics + keyboard support — still open from Phase 18.
- `AeroListItem` bottom mirror reflection — out of scope at milestone level.
- `AeroIconButton` / `AeroColorPickerButton` / `AeroRadioButton` Aero pass — outside the eight-target set.
