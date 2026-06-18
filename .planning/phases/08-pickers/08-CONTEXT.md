# Phase 8: Pickers - Context

**Gathered:** 2026-06-18
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver six **public** picker/slider components, built on the Phase 7 internal primitives, with no breaking changes to the v1.x API:

1. **`AeroRangeSlider`** (PICK-08) — dual-thumb slider over a shared track
2. **`AeroDatePicker`** (PICK-01) — trigger + popup month calendar → `LocalDate`
3. **`AeroTimePicker`** (PICK-03) — hour/minute entry → `LocalTime`
4. **`AeroDateTimePicker`** (PICK-04) — calendar + time controls + Apply/Cancel → `LocalDateTime`
5. **`AeroDateRangePicker`** (PICK-02) — dual-month calendar, start→end selection → committed range
6. **`AeroColorPicker`** (PICK-05/06/07) — HSV square + hue strip + RGB + HEX + swatches → `Color`; alpha optional

This phase clarifies the **public API surface and UX** of these components. Architecture, drag mechanics, popup positioning, and HSV math are already locked by Phase 7 and the v2.0 milestone decisions (see Canonical References). Showcase wiring (`PickersSection`) is **Phase 11**, not here.

**Out of scope for Phase 8 (locked / handled elsewhere):**
- Inline (always-visible) date/time pickers — popup-only in v2.0 (ColorPicker is the only inline-capable picker, by explicit decision below)
- `PickersSection` / any showcase v2.0 section — Phase 11
- ColorPicker eyedropper / screen color picking — deferred (platform-specific)
- DataTable / TreeView (Phase 9), Layout components (Phase 10)
- AeroDropdown popup-offset regression (v1.0 carry-over) — explicitly out of v2.0 scope
- Any change to v1.x public components — v2.0 is additive only
- i18n / localized date formatting — caller's responsibility (formatter is caller-supplied)

</domain>

<decisions>
## Implementation Decisions

### Trigger & value display (all 4 date/time pickers)
- **Closed-state trigger** = read-only `AeroTextField` (showing the formatted value) + a trailing `AeroIconButton` (`AeroIcons.Calendar` / `AeroIcons.Clock`) that opens the popup. Consistent with existing Aero form components; gives room for placeholder and clear affordance.
- **Value formatting** = caller-supplied formatter (e.g. `formatter: (T) -> String`, or a format-string parameter) with a sensible **ISO default** (`2026-06-18`, `14:30`). i18n is the consumer's concern (matches Out of Scope).
- **State API = controlled**: `value: T? = null` + `onValueChange: (T) -> Unit`. Matches the library-wide pattern (AeroSlider, AeroTextField, AeroCheckbox). `T` is `LocalDate` / `LocalTime` / `LocalDateTime` per component.
- **Empty/placeholder**: `value == null` is a valid "empty" state; `placeholder: String` shows when null. A **clear** affordance (`AeroIcons.X`) is available behind `clearable: Boolean = false` — appears only when a value is set.
- **Selectable-date constraints**: `minDate: LocalDate? = null` + `maxDate: LocalDate? = null` **plus** an optional `selectableDates: (LocalDate) -> Boolean`. These map onto the existing `AeroCalendarGrid(isDisabled = ...)` parameter — no new calendar plumbing needed.
- **`AeroDateRangePicker` trigger** = a **single** read-only field rendering `start → end` (e.g. `2026-06-01 → 2026-06-15`) + icon button; one popup with two side-by-side calendars (per PICK-02). Not two separate fields.

### TimePicker controls (PICK-03 + time half of PICK-04)
- **Hour & minute entry = two `AeroNumberSpinner` instances** (existing component): keyboard entry + up/down buttons + built-in range clamp (0–23 h, 0–59 min). Reuses an existing component, minimal new code, Aero-styled out of the box.
- **24-hour only — 12h/AM-PM mode is NOT implemented.** This is an explicit user decision that **narrows PICK-03** (which listed an optional 12h mode). No `use12Hour` parameter; no AM/PM control. (Captured under "Specific Ideas" as a deliberate scope reduction.)
- **Seconds = optional**: `showSeconds: Boolean = false` adds a third `AeroNumberSpinner` (0–59). When false, emitted `LocalTime` has `second = 0`.
- **Minute granularity**: `minuteStep: Int = 1`, caller-configurable (e.g. 5 / 15). `AeroNumberSpinner` already supports a step, so this is free.

### ColorPicker layout & output (PICK-05/06/07)
- **Two surfaces, shared core**: `AeroColorPicker` is an **inline panel** (always visible); `AeroColorPickerButton` is a thin wrapper = swatch-style trigger + `Popup` hosting the same panel. The "inline out of scope" exclusion applies only to date/time pickers — ColorPicker is allowed inline by this decision. Gives both embed-in-form and trigger-in-toolbar usage without duplicating the panel.
- **Panel layout** (top→bottom): row of [HSV square + vertical hue strip], then preview (before/after) bar, RGB sliders (3× `AeroSlider`), HEX input (`AeroTextField`), swatch row. Classic system-picker arrangement.
- **Callback type = Compose `Color`**: `value: Color` + `onValueChange: (Color) -> Unit`. Most ergonomic for Compose consumers; alpha rides inside `Color` when `enableAlpha = true`. Internal truth stays HSV(A) float (PITFALL-15) — `Color` is a derived boundary type only.
- **Alpha**: `enableAlpha: Boolean = false` (per PICK-07) → adds alpha slider with checkerboard backdrop; HEX widens to `#RRGGBBAA`.
- **Swatches = fixed default + override**: library ships a ~16-color default set (pure primaries + neutrals) with `swatches: List<Color>? = null` to override. **Exact 16 colors are Claude's discretion**, chosen to fit the Aero palette at implementation time.

### RangeSlider (PICK-08)
- **API**: `value: ClosedFloatingPointRange<Float>` + `onValueChange: (ClosedFloatingPointRange<Float>) -> Unit` + `valueRange` + `steps`. Mirrors Material `RangeSlider` and the existing `AeroSlider` style; idiomatic for Compose.
- **Value display**: glass tooltip above each thumb during drag, behind `showTooltip: Boolean = true` — consistent with `AeroSlider`.
- Thumbs cannot cross (min separation) + most-recently-moved thumb drawn on top (PITFALL-07, locked). Drag via Phase 7 `awaitPointerEventScope` pattern (PITFALL-03, locked).

### Animations (direction agreed; final sign-off required before implementation)
- **Popup open/close** (calendar + ColorPickerButton): fade (alpha 0→1) + light scale (~0.96→1) or short slide from the trigger, ~120–150ms. Leans Aero depth/softness.
- **In-component**: drag-driven updates (RangeSlider fill, HSV square indicator) are **live with no animation lag** (drag must respond on first pixel — PITFALL-03); day-cell selection highlight and DateRangePicker range hover-preview use a **soft color transition** (~80–100ms).
- **Per project convention, the exact animation curves/durations are approved by the user individually before each is implemented.** These are the agreed starting direction, not a blanket pre-approval. The planner should schedule an animation sign-off touchpoint.

### Claude's Discretion
- Exact 16 default swatch colors (Aero-fit palette).
- ColorPicker panel exact dimensions, spacing, and whether RGB/HEX sit in one or two visual rows within the chosen top-down layout.
- HSV square / hue strip pixel sizes inside the public picker (Phase 7 defaults: 256×256 square; hue strip width).
- Default ISO format strings and the exact formatter parameter shape (`(T) -> String` vs format-string) — pick whichever reads cleanest; ISO default is locked.
- Calendar keyboard navigation depth, HEX-input commit timing (Enter vs focus-loss), and popup dismiss rules — within the locked `PopupProperties(focusable=true, dismissOnClickOutside=true)` baseline. (User declined to deep-dive these; standard behavior is fine.)
- Plan granularity / wave split (build order is locked below).

### Build order (locked, from ROADMAP §Phase 8 notes)
RangeSlider → DatePicker (validates CalendarGrid wiring) → TimePicker → DateTimePicker (composition) → DateRangePicker (LARGE) → ColorPicker (LARGE).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner, executor) MUST read these before planning or implementing Phase 8.**

### Phase 8 source-of-truth (project-level)
- `.planning/ROADMAP.md` §Phase 8: Pickers — Goal, 5 success criteria, 8 phase notes (PITFALL-02/03/06/07/08/09/15 + W11-01), and the locked **build order**
- `.planning/REQUIREMENTS.md` §Pickers (PICK-01..08) — full requirement text for all six components; §Out of Scope — inline date/time pickers, eyedropper, Material3 DatePicker ban
- `.planning/PROJECT.md` §Current Milestone v2.0 (picker feature list); §Key Decisions table (v2.0 rules: touchSlop ban, HSV-first); §Context — **every component shape and animation requires user discussion/sign-off before implementation**
- `.planning/STATE.md` §v2.0 Locked Decisions; §Accumulated Context (Phase 07 locked decisions: hue in degrees [0,360]; calendar first-frame guard; `kotlinx-datetime:0.6.2` already added in Phase 7); §Pending Todos — Phase 8 `kotlinx-datetime` first-compile already satisfied in Phase 7

### Phase 7 hand-off (the primitives this phase consumes)
- `.planning/phases/07-shared-internal-primitives/07-CONTEXT.md` — locked API shapes for every primitive Phase 8 builds on (CalendarGrid signature, ColorMath functions, HsvSquare/HueSlider, aeroDragSplitter, CalendarPositionProvider, StepIndicator)
- `.planning/phases/07-shared-internal-primitives/07-SUMMARY.md` (07-01/02/03) — what actually landed + the 07-03 gap-closure fixes (CalendarGrid header pinned to 252dp; popup anchored to trigger; glass is a consumer responsibility)
- `.planning/phases/07-shared-internal-primitives/07-VERIFICATION.md` — Phase 7 sign-off state

### v2.0 research (authoritative — do not re-research what these cover)
- `.planning/research/PITFALLS.md` — **read all picker-relevant entries**: PITFALL-02 (calendar popup width), PITFALL-03 (touchSlop drag), PITFALL-06 (DateRangePicker partial state — sealed `Idle|SelectingEnd|Selected`), PITFALL-07 (RangeSlider thumb overlap), PITFALL-08 (popup first-frame flash), PITFALL-09 (AeroDark disabled cells → `labelText` token), PITFALL-15 (HSV drift), W11-01 (no `transparent=true`), W11-02 (popup shadow technique); plus the 16-item "looks done but isn't" checklist (Phase 11 gate, but items map to Phase 8 components)
- `.planning/research/SUMMARY.md` §Architecture Approach + §Critical Pitfalls + §Phase Ordering
- `.planning/research/ARCHITECTURE.md` — Phase 8 component structure; reuse of Phase 7 primitives + existing glass/popup infra
- `.planning/research/STACK.md` — `kotlinx-datetime:0.6.2` (already on classpath since Phase 7); Kotlin 2.1.21 / CMP 1.7.3 / JDK 17
- `.planning/research/FEATURES.md` — complexity ratings (DateRangePicker + ColorPicker = LARGE; DatePicker + DateTimePicker = MEDIUM; RangeSlider + TimePicker = SMALL)

### Existing codebase — Phase 7 primitives (Phase 8 imports these)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` — `AeroCalendarGrid(displayMonth, selected, onDateSelected, onMonthChange, modifier, isDisabled)`; consumed by DatePicker, DateRangePicker, DateTimePicker
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` — the ONLY position provider for all 4 date/time popups
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` — `rgbToHsv`, `hexToRgb`, `hexToRgba`, `rgbToHex`; the ONLY color-math layer for ColorPicker
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` — `AeroHsvColorSquare(hue, saturation, value, onSatValChange, modifier)`; hue in DEGREES [0,360]
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` — hue strip primitive
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — `Modifier.aeroDragSplitter(...)` (not used by pickers directly, but the established `awaitPointerEventScope` drag pattern reference)

### Existing codebase — v1.x components Phase 8 reuses/wraps
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` — visual/interaction reference for RangeSlider; RGB sliders in ColorPicker are 3× `AeroSlider`. Do NOT compose two AeroSliders for RangeSlider (PITFALL-07)
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` — `AeroNumberSpinner(...)` is the hour/minute/second control for TimePicker
- `library/src/main/kotlin/com/mordred/aero/components/input/AeroTextField.kt` — read-only trigger field for date/time pickers + HEX input field in ColorPicker
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt` — two-layer-background shadow technique (W11-02); calendar/color popups reuse it
- `library/src/main/kotlin/com/mordred/aero/components/selection/` — `AeroSegmentedControl` (NOT needed since 12h dropped, but reference for the pattern), `AeroIconButton` for trigger buttons
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — token reference; `labelText` for AeroDark disabled cells (PITFALL-09); `borderSelected`, `primary`, `onPrimary` for selection highlights
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassPanel(cornerRadius = 8.dp)` wraps picker popup containers + ColorPicker panel
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — `AeroIcons.{Calendar, Clock, X, CaretLeft, CaretRight}` for triggers and calendar nav

### Compose / platform reference
- `androidx.compose.ui.window.Popup` + `AeroCalendarPositionProvider` — all date/time + ColorPickerButton popups; `PopupProperties(focusable = true, dismissOnClickOutside = true)`
- `androidx.compose.ui.graphics.Color.hsv(...)` — forward HSV→Color (inverse in `AeroColorMath`)
- `kotlinx.datetime.{LocalDate, LocalTime, LocalDateTime}` — public callback types
- `androidx.compose.animation.*` (`AnimatedVisibility`, `fadeIn/Out`, `scaleIn/Out`, `animateColorAsState`) — popup + highlight animations (subject to per-animation user sign-off)

### Upstream issues
- JetBrains/compose-jb #343 — `detectDragGestures` touchSlop=18dp (basis for PITFALL-03; Phase 7 spike already confirmed the `awaitPointerEventScope` path)
- JetBrains/compose-multiplatform #3757 — Win11 `undecorated+transparent` crash (basis for W11-01)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Phase 7 primitives** (all `internal`, ready): `AeroCalendarGrid`, `AeroCalendarPositionProvider`, `AeroColorMath`, `AeroHsvColorSquare`, `AeroHueSlider` — Phase 8 wires public components on top with zero re-implementation.
- **`AeroNumberSpinner`** — drop-in hour/minute/second controls for TimePicker (keyboard + up/down + clamp + step already built).
- **`AeroTextField`** — read-only date/time trigger field; HEX input in ColorPicker.
- **`AeroSlider`** — RGB sliders in ColorPicker (3×); visual reference for RangeSlider track/thumb/tooltip.
- **`AeroIconButton`** — trigger buttons (Calendar/Clock icons).
- **`AeroDropdownPopup` two-layer background** — W11-02 shadow workaround for all picker popups.
- **`GlassModifiers.glassPanel`** — popup container + ColorPicker inline panel surface.

### Established Patterns
- **Controlled `value` + `onValueChange`** across all v1.x interactive components — Phase 8 pickers follow this (locked above).
- **`public fun Aero*`** with `explicitApi()` — every Phase 8 component is `public`; internal helpers stay `internal`. Phase 8 is where the first new public v2.0 API lands.
- **`modifier: Modifier = Modifier`** last among non-callback params; callbacks named `on<Event>`; colors pulled from `AeroTheme.colors` inside the composable.
- **Canvas drag = `awaitPointerEventScope` loop** (never `detectDragGestures`) — established Phase 7, mandatory for RangeSlider + ColorPicker square/hue.
- **Three themes** (AeroBlue/AeroDark/Classic) must render correctly — AeroDark disabled date cells use `labelText` token (PITFALL-09).

### Integration Points
- New public components land in `components/pickers/` (RangeSlider may stay in `components/range/` alongside `AeroSlider` — planner decides) consuming `components/pickers/internal/` + `components/internal/popup/`.
- `kotlinx-datetime:0.6.2` already on the `:library` classpath (added Phase 7) — no Gradle changes needed for the date types.
- No new `AeroColorScheme` tokens — Phase 8 reuses the existing 23 tokens.
- Showcase wiring deferred to Phase 11 (`PickersSection`); Phase 8 ships library code + unit tests only (any interactive eyes-on uses the temporary Phase 7 scratch section as a comparison point until Phase 11).

### Things Phase 8 must NOT touch
- v1.x public components (additive milestone only).
- `AeroColorScheme` token list (locked at 23).
- The existing `AeroPopupPositionProvider` (used by AeroDropdown/AeroComboBox) — pickers use the NEW `AeroCalendarPositionProvider`.
- `Phase7ScratchSection` lifecycle — its deletion is Phase 11's job.

</code_context>

<specifics>
## Specific Ideas

- **TimePicker is 24-hour only — a deliberate scope reduction.** The user explicitly dropped the optional 12h/AM-PM mode that PICK-03 listed. Rationale: desktop forms in this library's target domain are 24h; the AM/PM control adds API surface and a state branch for little payoff. Treat the 12h clause of PICK-03 as descoped, not unimplemented-by-accident. If a consumer ever needs 12h, it's a clean additive parameter later.
- **ColorPicker ships in two forms (panel + button) from one core.** Inline `AeroColorPicker` covers "embed the picker in a settings panel"; `AeroColorPickerButton` covers "a swatch in a toolbar that opens the picker". Building the button as a thin `Popup` wrapper around the same panel avoids two implementations and keeps behavior identical.
- **Caller-supplied date/time formatter, ISO default.** Keeps i18n out of the library (matches Out of Scope) while letting consumers render `18 Jun 2026` or `18.06.2026` without the library guessing a locale.
- **`Color` is the ColorPicker boundary type, HSV is the internal truth.** The callback emits `Color` for ergonomics, but the picker never stores `Color` as state — it derives `Color` from the HSV(A) float tuple on every emit (PITFALL-15). This must be explicit in the plan so an executor doesn't "simplify" it into dual state.
- **Animations are agreed in direction but each needs explicit user sign-off before coding** — per the standing project convention that every animation is approved individually. The directions captured here (fade+scale popup ~120-150ms; live drag; soft ~80-100ms highlights) are the starting point for that sign-off, not a substitute for it.

</specifics>

<deferred>
## Deferred Ideas

- **12-hour / AM-PM TimePicker mode** — dropped from v2.0 by user decision (see Specifics). Re-add as an additive `use12Hour`/`hourFormat` parameter in a later milestone if a consumer asks.
- **Calendar keyboard-navigation richness** (arrow-key grid traversal, type-to-jump) — standard focusable popup behavior is enough for v2.0; richer keyboard UX is a v2.x polish item.
- **Inline (always-visible) date/time pickers** — still out of v2.0 scope (only ColorPicker is inline-capable); date/time inline mode remains a v3+ item (PICK-INL-01).
- **ColorPicker eyedropper / screen color picking** — platform-specific, deferred (COLOR-EYE-01).
- **Localized / locale-aware default formatting** — library stays ISO-default + caller formatter; a locale-aware helper could ship later if demand appears.

</deferred>

---

*Phase: 08-pickers*
*Context gathered: 2026-06-18*
