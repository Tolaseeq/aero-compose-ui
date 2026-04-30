# Phase 7: Shared Internal Primitives - Context

**Gathered:** 2026-04-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Build the six shared internal primitives that two or more v2.0 public components depend on, so Phases 8–10 can implement public components without re-inventing drag logic, calendar rendering, or color math:

1. **`AeroCalendarGrid`** — internal Composable; 7-column day grid + day-of-week header + prev/next month buttons; consumed by `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`
2. **`AeroCalendarPositionProvider`** — `PopupPositionProvider` for popups wider than their anchor; consumed by all four date/time picker popups (and the ColorPicker swatch popup if needed)
3. **`AeroColorMath`** — pure-function HSV ↔ RGB ↔ HEX utility (HSV is single source of truth); consumed by `AeroColorPicker`
4. **`AeroHsvColorSquare` + `AeroHueSlider`** — Canvas-based color-selection primitives with `awaitPointerEventScope` drag; consumed by `AeroColorPicker`
5. **`Modifier.aeroDragSplitter(...)`** — Modifier extension exposing `awaitPointerEventScope` drag with cursor change; consumed by `AeroSplitPane` (Phase 10) and `AeroDataTable` column-resize (Phase 9)
6. **`AeroStepIndicator`** — internal Composable; step dots + connecting lines with current/completed/upcoming visual states; consumed by `AeroStepperWizard` (Phase 10)

**Phase 7 is an enabling phase:**
- Owns **zero** public requirements (no PICK / DATA / LAYO / SHW IDs).
- Ships **no** new public API (`explicitApi()` does not see any of the above; everything is `internal`).
- Touches **only** `library/src/`, `library/src/test/`, and a **temporary** `showcase/Phase7ScratchSection.kt` for interactive sign-off.

**Out of scope for Phase 7 (locked / handled elsewhere):**
- Public API for any picker / data / layout / showcase component — owned by Phases 8–11
- `kotlinx-datetime` usage in pickers/data — Phase 8+ wires the public API; Phase 7 only adds the dep + first-compile validation + uses `LocalDate` internally in CalendarGrid
- Showcase v2.0 sections (`DataSection`, `PickersSection`, `LayoutSection`) — Phase 11 territory; Phase 7 only adds a throwaway `Phase7ScratchSection`
- `AeroDropdown` popup-offset regression (v1.0 carry-over) — explicitly deferred per STATE.md
- Any change to v1.x public API — locked decision: v2.0 is additive, no breaking changes
- Material3 `DatePicker` integration — confirmed crashes on Compose Desktop; all pickers hand-rolled (Phase 8)
- `AeroScrollArea` use inside DataTable / TreeView — banned (PITFALL-01); Phase 9 territory but already locked
- Three-theme visual sign-off ceremony — Phase 11 milestone gate; Phase 7 only confirms StepIndicator renders sanely across themes via the scratch section

</domain>

<decisions>
## Implementation Decisions

### Date type for CalendarGrid (locked)
- **`kotlinx-datetime:0.6.2` is added to `:library` in Phase 7 plan-01** (moved forward from Phase 8 per ROADMAP). ROADMAP §Phase 8 sentence "kotlinx-datetime:0.6.2 added" stands corrected to Phase 7.
- **First-compile validation moves to Phase 7 plan-01 acceptance criteria** (originally a Phase 8 plan-01 criterion per STATE.md "Pending Todos"). If `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 fails, fallback to `0.7.1-0.6.x-compat` is decided here, not deferred.
- **`AeroCalendarGrid` uses `kotlinx-datetime` types end-to-end:**
  - Signature: `internal fun AeroCalendarGrid(displayMonth: LocalDate, selected: LocalDate?, onDateSelected: (LocalDate) -> Unit, onMonthChange: (LocalDate) -> Unit, modifier: Modifier = Modifier, isDisabled: (LocalDate) -> Boolean = { false })`
  - Phase 8 pickers pass `LocalDate` types straight through — zero conversion at the picker boundary
  - Month arithmetic via `kotlinx.datetime.plus(1, DateTimeUnit.MONTH)` / `minus(1, DateTimeUnit.MONTH)`
- **3-scenario unit test (per ROADMAP success criterion #1)** uses `LocalDate` directly: current month (today's month), month boundary (transitioning Dec → Jan), leap year (Feb 2024). Tests live in `library/src/test/kotlin/com/mordred/aero/components/internal/AeroCalendarGridTest.kt`.

### Internal package layout — hybrid (locked)
- **`library/src/main/kotlin/com/mordred/aero/components/internal/`** (NEW shared cross-cutting bucket):
  - `drag/AeroDragSplitter.kt` — used by datatable + layout (Modifier extension)
  - `popup/AeroCalendarPositionProvider.kt` — used by all 4 date/time pickers (and possibly ColorPicker swatch popup)
- **`library/src/main/kotlin/com/mordred/aero/components/pickers/internal/`** (NEW, Phase 7 creates the package):
  - `calendar/AeroCalendarGrid.kt`
  - `color/AeroColorMath.kt`
  - `color/AeroHsvColorSquare.kt`
  - `color/AeroHueSlider.kt`
- **`library/src/main/kotlin/com/mordred/aero/components/layout/internal/`** (NEW, Phase 7 creates the package):
  - `stepper/AeroStepIndicator.kt`
- **Rationale:** picker-only primitives sit nearest their consumer (pickers/internal/); layout-only primitives sit in layout/internal/; primitives consumed by ≥2 packages live in components/internal/ to avoid awkward cross-package internal imports. AeroCalendarPositionProvider is treated as cross-cutting per the user's selection (forward-looking — any v2.x component popup wider than its anchor will reuse it).
- **`components/datatable/internal/` is NOT created in Phase 7** — Phase 9 creates it for header/row/node composables. Phase 9's column-resize wires `Modifier.aeroDragSplitter(...)` from `components/internal/drag/`.

### `Modifier.aeroDragSplitter` API shape (locked)
- **Modifier-based, NOT a content-slot Composable:**
  ```kotlin
  internal fun Modifier.aeroDragSplitter(
      orientation: Orientation, // Horizontal or Vertical
      onDrag: (deltaPx: Float) -> Unit,
      onDragEnd: () -> Unit = {},
      enabled: Boolean = true,
  ): Modifier
  ```
- **`onDrag` receives `Float` (1D delta along orientation axis), NOT `Offset`:** orientation parameter selects x or y; consumers always work in 1D (SplitPane = horizontal-or-vertical divider, DataTable column-resize = horizontal-only). Avoids consumers silently using the wrong axis.
- **The Modifier owns ONLY:**
  - `awaitPointerEventScope` + manual loop (PITFALL-03 mitigation; `detectDragGestures` is BANNED for all Canvas drag in v2.0)
  - Cursor change on hover via `pointerHoverIcon(PointerIcon.Hand)` or `Cursor(EAST_RESIZE_CURSOR / NORTH_RESIZE_CURSOR)` keyed to orientation
  - `enabled` gate (no-op when false — `AeroSplitPane` and DataTable column resize need to disable resize at min/max bounds)
- **The Modifier does NOT own:** the visual line, hit-area thickness, color, or shape. SplitPane wraps it inside a `Box(Modifier.size(...).aeroDragSplitter(...))` with its own 1dp visible line + 8–12dp invisible hit-area Box. DataTable wraps it in a column-header divider Spacer.
- **First-pixel response is verified via the scratch section's interactive smoke test** (drag any test component, observe response on first mouse movement — no slop delay).

### `AeroColorMath` shape (Claude's discretion within locked rules)
- **Locked rule (PITFALL-15 / STATE.md):** HSV is the single source of truth. `Color.hsv(...)` from `compose.ui.graphics` is the forward conversion. Inverse (`Color → HSV`) is hand-rolled.
- **API surface — Claude's discretion** (top-level pure functions or `internal object AeroColorMath { fun ... }` namespace — both fine; the round-trip test is the contract):
  - `fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float>` (or `FloatArray(3)`)
  - `fun hexToRgb(hex: String): Triple<Int, Int, Int>?` returns null on parse failure
  - `fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String`
  - All values in `[0f, 1f]` for hue/sat/val/alpha; `[0, 255]` for r/g/b ints — implementer locks the convention in plan-01.
- **Mandatory unit test (ROADMAP success criterion #2):** `hsv(0f, 1f, 1f)` → `rgb` → `hsv` returns hue within `0.001f` tolerance — PITFALL-15 drift confirmed absent at the utility level. Tests live in `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt`.

### `AeroHsvColorSquare` + `AeroHueSlider` (locked)
- **Canvas-based, drag via `Modifier.pointerInput { awaitPointerEventScope { ... } }`:**
  - First mouse-down sets the cursor immediately (no slop wait — this is the PITFALL-03 establishing pattern; if the upstream issue #343 turns out fixed in CMP 1.7.x per STATE.md "Pending Todos", a 1-minute drag test in plan-01 validates which path applies, but we ship the manual loop regardless to defuse risk).
  - Each pointer event during drag updates HSV state; consumer `AeroColorPicker` receives the updated HSV via `onHsvChange: (h: Float, s: Float, v: Float) -> Unit`.
- **Visuals:**
  - HSV square: 256×256 logical Canvas; horizontal axis = saturation, vertical = value (top-left white, bottom-left black, top-right pure hue, bottom-right black). Background uses `drawRect(brush = Brush.horizontalGradient(...))` then a `Brush.verticalGradient(black overlay)`.
  - Hue slider: vertical strip ~20–24dp wide × 256dp tall with 7-stop gradient (red → yellow → green → cyan → blue → magenta → red); current-hue indicator is a 2dp horizontal line crossing the strip.
  - Both are `internal` composables; receive `colors: AeroColorScheme = AeroTheme.colors` for thumb/indicator strokes (matches existing primitive pattern).
- **No glass styling on these primitives** — they live inside `AeroColorPicker`'s glass panel container; per-element glass would muddy the saturation gradient.

### `AeroStepIndicator` (locked)
- **Horizontal-only** (matches `AeroStepperWizard` ROADMAP §Phase 10 success-criterion #4 — "horizontal step indicator"). Vertical orientation deferred to v2.x if a real consumer appears (none in v2.0 scope).
- **Visual states (per ROADMAP success criterion #5):**
  - **Current:** filled circle in `colors.primary`; step number in `colors.onPrimary`
  - **Completed:** filled circle in `colors.primary` at `0.6f` alpha; `Icon(AeroIcons.Check)` at 12dp tinted `colors.onPrimary`
  - **Upcoming:** outlined circle (`colors.borderDefault` 1dp stroke); step number in `colors.labelText`
  - Connecting lines between dots: `colors.borderDefault` for upcoming-side, `colors.primary` for completed-side, transition at the current step
- **API:**
  ```kotlin
  internal fun AeroStepIndicator(
      currentStep: Int,
      totalSteps: Int,
      modifier: Modifier = Modifier,
      onStepClick: ((Int) -> Unit)? = null, // null = non-interactive (default for wizard)
  )
  ```
- **AeroDark contrast:** primary at `0.6f` on dark background reads acceptably (verified Phase 1+2 — `colors.primary` is `Color(0xFF4FC3F7)` in AeroBlue and `Color(0xFF6FCDFF)` in AeroDark). Three-theme visual confirmation is part of the scratch section's checklist.

### `AeroCalendarPositionProvider` (locked, per PITFALL-02 + PITFALL-08)
- **Behavior:**
  1. Default: left-aligned to `anchorBounds.left`, vertically below the anchor
  2. If `anchorBounds.left + popupContentSize.width > windowSize.width`: right-align to `anchorBounds.right`
  3. If popup exceeds window height below the anchor: flip to above (Top side)
  4. **Width overflow does NOT trigger Top/Bottom flip** (locked — fixes the existing `AeroPopupPositionProvider` `overflows` bug for wide calendars)
- **First-frame guard (PITFALL-08):** treat `popupContentSize == IntSize.Zero` as unmeasured → return `IntOffset.Zero` (off-screen at top-left, invisible on frame 1). Do NOT use the `>= windowSize` sentinel from `AeroPopupPositionProvider` — that condition is too conservative for wide calendars.
- **Calendar popup container properties:** `PopupProperties(focusable = true, dismissOnClickOutside = true)` so keyboard navigation and click-outside-to-dismiss work without extra workarounds.
- **`AeroDateRangePicker` minimum width:** ~560dp for dual-month side-by-side; on `BoxWithConstraints` < 560dp the calendars stack vertically (Phase 8 implementation detail; Phase 7 just confirms the position provider doesn't fight stacking).

### `AeroColorMath` API surface — Claude's Discretion
- Top-level functions vs `internal object` namespace vs single facade — implementer chooses what reads best. Only the round-trip-stability contract is locked.

### Sign-off approach (locked) — unit tests + temporary scratch section
- **Unit tests (mandatory, run in CI):**
  - `AeroColorMathTest.kt` — HSV ↔ RGB ↔ HEX round-trip drift test (`hsv(0f, 1f, 1f)` → rgb → hsv, hue within `0.001f`)
  - `AeroCalendarGridTest.kt` — 3 scenarios: current month renders 28–31 cells correctly, Dec → Jan boundary, leap-year Feb 2024 (29 days) vs Feb 2023 (28 days)
- **Temporary `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`** (deleted in Phase 11):
  - `AeroCalendarGrid` instance — eyes-on confirmation of layout, day-of-week header, prev/next nav
  - `AeroHsvColorSquare` + `AeroHueSlider` row — drag confirms first-pixel response (PITFALL-03 mitigation)
  - `Modifier.aeroDragSplitter` test box (horizontal + vertical) — drag confirms first-pixel response
  - `AeroStepIndicator` row showing 4 steps with currentStep stepper buttons — three-theme visual confirmation
  - `AeroCalendarPositionProvider` test: trigger button placed near right edge of a 1024dp scratch frame to verify no clipping
- **ScratchSection deletion:** Phase 11 cleanup task in the showcase plan. Single deletion point; gives Phase 8/9/10 something to compare against if a primitive bug surfaces during their implementation.
- **Touchslop spike (per STATE.md "Pending Todos"):** Phase 7 plan-01 includes a 1-minute drag test on `awaitPointerEventScope` vs `detectDragGestures` to confirm whether the upstream issue #343 is fixed in CMP 1.7.3. We ship the `awaitPointerEventScope` pattern regardless (defuses risk), but the spike documents the result for future reference.

### Carry-forward from milestone-level locked decisions (NOT re-asked)
- `awaitPointerEventScope` + manual loop is the ONLY drag pattern for Canvas drag on Compose Desktop (PITFALL-03 — locked v2.0)
- HSV is single source of truth; RGB and HEX are derived views (PITFALL-15 — locked v2.0)
- `AeroScrollArea` is BANNED inside DataTable/TreeView (PITFALL-01 — locked v2.0; not Phase 7's concern but the rule already exists)
- DataTable selection is `Set<RowKey>` + `key: (T) -> Any`, NOT `Set<Int>` (PITFALL-04 — locked v2.0; Phase 9 territory, irrelevant here but already locked)
- `Popup(...)` for all overlays — NEVER `Dialog(undecorated=true, transparent=true)` (W11-01 — locked since v1.0)
- Single-pass `drawBehind` for glass effects (carry-forward v1.0)
- `Aero` prefix for everything; PascalCase Phosphor verbatim for AeroIcons (carry-forward v1.1)
- Three themes (AeroBlue / AeroDark / Classic) supported by every visual primitive (carry-forward v1.0)
- `:library` uses `compose.desktop.common`; `:showcase` uses `currentOs` (carry-forward v1.0)
- `explicitApi()` enforced on `:library` — every primitive must be `internal` (Phase 7 ships zero `public` symbols)
- Material3 `Icon()` directly, no wrapper; explicit `tint` always (carry-forward v1.1)
- Generated/internal `.kt` files committed to `src/main/`, no build-time generation (carry-forward v1.1)
- Aero-aesthetic priority (per user memory): visual choices lean Win7 Aero (gloss/gradient/rounded/depth), not generic flat. Affects `AeroStepIndicator` filled-circle treatment (slight gradient or shadow consistent with `AeroChip`/`AeroBadge` Phase 2 precedents).

### Claude's Discretion
- **Plan granularity:** recommended 2–3 plans for Phase 7 (e.g. plan-01: dep bump + spike + ColorMath + CalendarGrid + tests; plan-02: AeroDragSplitter + AeroCalendarPositionProvider + HsvSquare/HueSlider + StepIndicator + scratch section). Planner picks the exact split; single plan also acceptable if waves stay reviewable.
- `AeroColorMath` API surface (top-level functions vs `internal object` vs facade) — picker chooses.
- Exact cursor type for `Modifier.aeroDragSplitter` hover (`PointerIcon.Hand` vs explicit AWT cursor) — picker chooses; consistent across orientations.
- HSV square exact pixel dimensions (256×256 vs 240×240 vs configurable) — picker chooses; default for `AeroColorPicker` Phase 8 stays implementer-set.
- `AeroStepIndicator` exact dot diameter, line stroke, and inter-step spacing — picker chooses within the established `AeroBadge` / `AeroChip` Phase 2 visual scale.
- Whether `AeroCalendarPositionProvider` lives in `components/internal/popup/` (chosen) vs alongside `AeroPopupPositionProvider` in `components/popup/` (would require widening the popup package's internal exposure) — chose `components/internal/popup/` per the hybrid layout selection.
- The exact deletion mechanism for `Phase7ScratchSection` in Phase 11 — likely a single line removal in `ShowcaseApp.kt` plus deleting the file; planner finalizes.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner, executor) MUST read these before planning or implementing Phase 7.**

### Phase 7 source-of-truth (project-level)
- `.planning/ROADMAP.md` §Phase 7: Shared Internal Primitives — 5 success criteria + 4 phase notes (PITFALL-02, PITFALL-03, PITFALL-15, W11-01); enabling-phase rationale; build order
- `.planning/REQUIREMENTS.md` §Coverage — Phase 7 owns 0/27 v2.0 requirements; explicit "enabling phase" framing; downstream phase requirement counts (PICK 8, DATA 6, LAYO 9, SHW 4)
- `.planning/PROJECT.md` §Current Milestone v2.0 — target features list; §Constraints (Compose Desktop / Windows primary); §Key Decisions table — v2.0 new rules (touchSlop ban, AeroScrollArea ban, RowKey selection, HSV-first); §Context (every animation + component shape requires user discussion before implementation)
- `.planning/STATE.md` §v2.0 Locked Decisions — PITFALL summary; §Pending Todos — Phase 7 touchSlop spike, Phase 8 kotlinx-datetime first-compile validation (now moved to Phase 7 plan-01); §Blockers/Concerns — PITFALL-01/03/04 risk register

### v2.0 research (authoritative — do not re-research what these cover)
- `.planning/research/SUMMARY.md` §Architecture Approach — five primitives + position provider extracted to Phase 7; §Critical Pitfalls (5 named showstoppers); §Phase Ordering Rationale; §Research Flags §Phase 7 (drag pattern + HSV math need careful upfront design)
- `.planning/research/PITFALLS.md` §PITFALL-02 (calendar popup width) — `AeroCalendarPositionProvider` design contract; §PITFALL-03 (touchSlop=18dp) — `awaitPointerEventScope` mandatory pattern with full code template; §PITFALL-08 (calendar popup first-frame flash) — `popupContentSize == IntSize.Zero` guard; §PITFALL-15 (HSV drift) — single-source-of-truth contract; §W11-01 (transparent=true ban) — applies to ALL overlays in v2.0; §"Looks done but isn't" 16-item checklist (Phase 11 gate, but Phase 7 sets the foundation for items 14, 15)
- `.planning/research/ARCHITECTURE.md` §Section "Shared internal primitives (Phase 7 — no public output, unlocks Phases 8–10)" — explicit list of 5 primitives + 1 position provider; §Reuse from v1.0/v1.1 — Phase 7 reuses `AeroPopupPositionProvider` patterns (NOT the same instance — new provider) and existing `glassPanel` modifier
- `.planning/research/STACK.md` — Kotlin 2.1.21, CMP 1.7.3, JDK 17, Gradle 8.14.3 (Phase 7 inherits, no version changes); `kotlinx-datetime:0.6.2` rationale (last stable pre-0.7.0 breaking renames)
- `.planning/research/FEATURES.md` — complexity ratings (LARGE/MEDIUM/SMALL drives downstream phase load; not Phase 7's direct concern but informs why these 6 primitives are extracted vs. inlined per consumer)

### Existing codebase — Phase 7 reads (no changes)
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt` — design reference for `AeroCalendarPositionProvider`; the new provider deliberately diverges (no `widthIn(min=anchorWidth, max=anchorWidth)` lock; no width-overflow→Top flip)
- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt` — reference for the two-layer background technique (W11-02 shadow workaround); `AeroCalendarPositionProvider` consumers will reuse this technique in Phase 8
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` — visual + interaction reference for `AeroHueSlider`; do NOT compose two `AeroSlider` instances (PITFALL-07 — Phase 8 territory but informs how `AeroHueSlider` differs from `AeroSlider`)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — token reference: `colors.{primary, onPrimary, borderDefault, labelText, surface, panelBackground, glassBorder}`; basis for `AeroStepIndicator` visual states + `AeroHsvColorSquare` indicator strokes
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — `Modifier.glassPanel(cornerRadius = 8.dp)` for the calendar popup container (Phase 8 will use it; Phase 7 confirms position provider works with it)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `AeroTheme.colors` / `AeroTheme.typography` access pattern from internal composables
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — `AeroIcons.{Check, CaretLeft, CaretRight}` for `AeroStepIndicator` completed-state icon and `AeroCalendarGrid` prev/next month buttons
- `library/build.gradle.kts` — Phase 7 plan-01 modifies this file: adds `implementation(libs.kotlinx.datetime)`; existing test infra (`junit-jupiter`, `kotlin-test`, `kotlinx-coroutines-test`, `kotlin("reflect")`) is reused for the new unit tests with no Gradle changes
- `gradle/libs.versions.toml` — Phase 7 plan-01 adds the `kotlinx-datetime = "0.6.2"` version coordinate + `kotlinx-datetime` library entry

### Existing codebase — Phase 7 modifies / creates
- `library/build.gradle.kts` — add `kotlinx-datetime:0.6.2` (line in `dependencies { ... }`)
- `gradle/libs.versions.toml` — add the version + library coordinate
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` (NEW)
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` (NEW)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` (NEW; creates `pickers/` package directory)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` (NEW)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` (NEW)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` (NEW)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` (NEW; creates `layout/` package directory)
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` (NEW)
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` (NEW; uses `kotlin-test` + JUnit 5 platform per existing `tasks.test { useJUnitPlatform() }`)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` (NEW; **deleted in Phase 11**)
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — add `Phase7ScratchSection()` call (deleted in Phase 11 alongside the file)

### Compose / platform reference
- `androidx.compose.ui.input.pointer.awaitPointerEventScope` + `awaitFirstDown` + manual `awaitPointerEvent` loop (PITFALL-03 mandatory pattern — full template in PITFALLS.md §PITFALL-03)
- `androidx.compose.ui.window.Popup` + `PopupPositionProvider` interface — basis for `AeroCalendarPositionProvider`
- `androidx.compose.ui.input.pointer.pointerHoverIcon(PointerIcon.Hand)` OR AWT `Cursor(Cursor.E_RESIZE_CURSOR)` for splitter hover cursor
- `androidx.compose.ui.graphics.Color.hsv(hue, saturation, value, alpha)` — forward HSV→Color conversion (Color → HSV inverse is hand-rolled in `AeroColorMath`)
- `androidx.compose.foundation.Canvas` + `drawRect(brush = Brush.horizontalGradient(...))` — basis for `AeroHsvColorSquare` and `AeroHueSlider` rendering
- `kotlinx.datetime.LocalDate`, `kotlinx.datetime.Month`, `kotlinx.datetime.DateTimeUnit.MONTH`, `kotlinx.datetime.plus`/`minus` — calendar arithmetic in `AeroCalendarGrid`

### Upstream issues (referenced from research)
- JetBrains/compose-jb #343 — `detectDragGestures` `touchSlop=18dp` makes mouse drag laggy on Desktop (basis for PITFALL-03; Phase 7 plan-01 spike confirms current CMP 1.7.3 status)
- JetBrains/compose-multiplatform #3757 — Win11 `undecorated=true` + `transparent=true` `EXCEPTION_ACCESS_VIOLATION` (basis for W11-01; not Phase 7's concern but the rule applies to any Phase 8+ Popup-bearing component)
- JetBrains/compose-multiplatform #3016, #2940 — `stickyHeader` bugs in LazyColumn (basis for PITFALL-01 and Phase 9's "raw `Row` header outside LazyColumn" approach; not Phase 7's concern)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AeroPopupPositionProvider.kt` (popup/)** — design reference; `AeroCalendarPositionProvider` deliberately diverges (no width-lock, no width-overflow flip-Top). Same `PopupPositionProvider` interface contract.
- **`AeroDropdownPopup.kt` (popup/)** — two-layer-background technique for W11-02 shadow workaround; Phase 8 picker popup containers will reuse this; Phase 7's position provider must not fight it.
- **`AeroSlider.kt` (range/)** — visual reference for `AeroHueSlider` thumb/track styling; do NOT instantiate `AeroSlider` inside the hue slider (independent Material3 state would conflict).
- **`AeroColorScheme.kt` (theme/)** — all 23 tokens already established; Phase 7 needs zero new tokens. `colors.primary / onPrimary / borderDefault / labelText` for `AeroStepIndicator`; `colors.glassBorder / surface / panelBackground` for HSV square/hue slider container styling (when consumed by `AeroColorPicker` Phase 8).
- **`GlassModifiers.kt` (theme/)** — `Modifier.glassPanel(cornerRadius = 8.dp)` will wrap calendar popup content in Phase 8; Phase 7 only confirms `AeroCalendarPositionProvider` plays nicely with it.
- **`AeroIcons.kt` (icons/)** — `AeroIcons.Check` (StepIndicator completed state), `AeroIcons.CaretLeft` / `AeroIcons.CaretRight` (CalendarGrid prev/next month buttons). Existing v1.1 facade; no extension needed.
- **`AeroTheme.kt` (theme/)** — `AeroTheme.colors` / `AeroTheme.typography` access pattern; internal composables use it identically to public ones.
- **JUnit 5 + kotlin-test + kotlinx-coroutines-test + kotlin-reflect** — already wired in `library/build.gradle.kts:21-26` with `tasks.test { useJUnitPlatform() }`. Phase 7 unit tests slot in with zero Gradle work.
- **`material3.Icon`** — already on `:library` classpath; v1.1 confirmed. `AeroStepIndicator` uses it for the completed-state checkmark.

### Established Patterns
- **Internal-only composables sit under `components/<package>/internal/`** — established by `icons/internal/` (138 generated icon backing files, Phase 4). Phase 7 extends this convention to picker/layout/cross-cutting buckets.
- **`internal fun` (no `public`) — `explicitApi()` requires explicit visibility on every declaration.** Phase 7 ships zero `public` symbols.
- **Composable parameter convention:** `modifier: Modifier = Modifier` last among non-callbacks; callbacks named `on<Event>` (e.g. `onDateSelected`, `onDrag`, `onDragEnd`); colors pulled from `AeroTheme.colors` inside the composable, not parameterized (matches v1.0/v1.1 pattern).
- **Canvas-based primitive pattern:** `Canvas(modifier = Modifier.size(...).pointerInput(Unit) { awaitPointerEventScope { ... } })` — established by existing AeroSlider thumb. Phase 7's `AeroHsvColorSquare` and `AeroHueSlider` follow this.
- **Pure-utility files use top-level functions** (no class wrapper unless state is involved) — established by `theme/GlassModifiers.kt` (extension functions on Modifier). Suggests `AeroColorMath` defaults to top-level functions but `internal object AeroColorMath { ... }` is also acceptable.
- **Unit tests under `library/src/test/kotlin/<mirror-of-main-path>/<NameTest>.kt`** — established by existing test directory; JUnit 5 idioms (`@Test`, `assertEquals`, `assertTrue`).
- **Showcase section files use `@Composable fun XxxSection()` with no required params** (or one optional state param) — established by all existing sections (`FoundationSection`, `ButtonsSection`, etc.). `Phase7ScratchSection()` follows.

### Integration Points
- **No new public API surface in `:library`** — `explicitApi()` sees zero new public symbols. Verified by `./gradlew :library:compileKotlin` passing without `explicitApi()` errors on any new file.
- **`kotlinx-datetime:0.6.2` lands in `library/build.gradle.kts`** — single line addition under `dependencies { implementation(libs.kotlinx.datetime) }`. Version catalog in `gradle/libs.versions.toml`.
- **`ShowcaseApp.kt` insertion point for `Phase7ScratchSection()`** — at the end of the existing section list (after the v1.x sections; before any v2.0 sections that don't exist yet). Single line + one import. Both line and import are removed in Phase 11.
- **Phase 8 wiring** — `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker` will import `AeroCalendarGrid` and `AeroCalendarPositionProvider`; `AeroColorPicker` will import `AeroColorMath`, `AeroHsvColorSquare`, `AeroHueSlider`. All from the new `components/internal/` and `components/pickers/internal/` packages — Phase 8 plan-01 verifies the imports compile.
- **Phase 9 wiring** — `AeroDataTable` column-resize will use `Modifier.aeroDragSplitter(orientation = Horizontal, ...)` from `components/internal/drag/`. Phase 9 plan-01 verifies.
- **Phase 10 wiring** — `AeroSplitPane` will use `Modifier.aeroDragSplitter(orientation = ...)`; `AeroStepperWizard` will use `AeroStepIndicator(currentStep, totalSteps)` from `components/layout/internal/`. Phase 10 plan-01 verifies.

### Things Phase 7 must NOT touch
- Any v1.x public component — `AeroButton`, `AeroTextField`, `AeroSlider`, `AeroDropdown`, `AeroDialog`, `AeroTitleBar`, etc. All Phase 1–3 territory; locked.
- `AeroIcons` facade or its 138 icon files — Phase 4/5 territory; locked.
- `AeroColorScheme` token list — locked at 23 tokens for v1.x and v2.0 (per Phase 6 carry-forward + STATE.md). Phase 7 reuses existing tokens; does NOT add new ones.
- `AeroBreadcrumb.separator: String` — locked v1.1 exclusion.
- Existing `AeroPopupPositionProvider` — Phase 7 writes a NEW provider (`AeroCalendarPositionProvider`); does NOT modify the existing one (it stays correct for `AeroDropdownPopup` and `AeroComboBox`).
- The existing `AeroDropdown` popup-offset regression — explicitly out of v2.0 scope per STATE.md; not a Phase 7 concern.
- Showcase v2.0 Section files (`DataSection`, `PickersSection`, `LayoutSection`) — Phase 11 territory; Phase 7 only adds and later removes the temporary `Phase7ScratchSection`.
- `:library` package layout outside the new `components/internal/`, `components/pickers/internal/`, `components/layout/internal/` directories — Phase 7 does not reshuffle existing files.

### File-modification manifest (Phase 7 produces)
**Created in `:library` main source:**
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt`
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt`
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt`

**Created in `:library` test source:**
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt`
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt`

**Created in `:showcase` (temporary, deleted in Phase 11):**
- `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`

**Modified (in place):**
- `library/build.gradle.kts` — add `implementation(libs.kotlinx.datetime)` to `dependencies { ... }`
- `gradle/libs.versions.toml` — add `kotlinx-datetime = "0.6.2"` version + library entry
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — add `Phase7ScratchSection()` call + import (both removed in Phase 11)

**Planning artifacts:**
- `.planning/phases/07-shared-internal-primitives/07-RESEARCH.md` (gsd-phase-researcher output)
- `.planning/phases/07-shared-internal-primitives/07-PLAN-*.md` (~2–3 plans expected)
- `.planning/phases/07-shared-internal-primitives/07-SUMMARY.md` per plan (post-execution)
- `.planning/phases/07-shared-internal-primitives/07-VERIFICATION.md` (post-execution)

</code_context>

<specifics>
## Specific Ideas

- **"Pull `kotlinx-datetime` forward to Phase 7"** — surfaced as the cleanest path because the leap-year unit test in ROADMAP success-criterion #1 is hard to write without a real date type, and Phase 7 is exactly where the calendar abstraction lives. Re-decoring the dep through `java.time` for one phase only to convert again in Phase 8 added integration cost without payoff. The roadmap text amendment is small (one sentence in §Phase 8, one sentence in §Phase 7).
- **First-compile validation in plan-01** — STATE.md "Pending Todos" lists "Phase 8 plan-01" as the original location; moving it here is a natural consequence of moving the dep itself. If `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 fails (unlikely per Kotlin's backward-compat policy), the documented fallback is `0.7.1-0.6.x-compat` per STACK.md.
- **`Modifier.aeroDragSplitter` is a Modifier, not a content-slot Composable** — both consumers (SplitPane, DataTable column-resize) have very different visual treatments (full-bleed 1dp line vs narrow header divider) and very different hit-area geometry. A content-slot Composable would either grow arms (parameters for hit-area thickness, visual mode, content alignment) or force one consumer to fight the API. Modifier keeps the contract minimal: drag logic + cursor + nothing else.
- **`onDrag: (Float) -> Unit` (1D delta), not `(Offset) -> Unit`** — both consumers operate in 1D along the orientation axis; 2D Offset would be flexibility neither uses. The orientation parameter on the Modifier picks the axis once at composition; the callback signature reflects that.
- **`Phase7ScratchSection` as a sign-off ceremony** — Phase 7 has no public API and no real showcase artifact, so "done" is harder to pin down than v1.0/v1.1 phases. The scratch section gives the user a 5-minute eyes-on confirmation across all six primitives in one spot; unit tests cover the two ROADMAP-mandated tests; combined they're a verifiable artifact. Phase 11 cleanup (single deletion point) keeps the showcase v2.0 final state clean — no scratch code ships in v2.0.
- **Hybrid package layout matches the consumer-mental-model** — pickers' internal helpers live with pickers; layout's helpers with layout; truly cross-cutting (DragSplitter, CalendarPositionProvider) lives in a shared bucket. Avoids the "one big internal/" warehouse problem and the "where does this even live?" problem when something is genuinely shared.
- **All Phase 7 visuals must hold up across three themes** — even though there's no formal three-theme checkpoint until Phase 11, the scratch section exercises StepIndicator and HSV square in all three themes via the existing `ThemeSwitcher` at the top of `ShowcaseApp.kt`. Catches AeroDark contrast issues early (PITFALL-09 family), where they're cheap to fix.

</specifics>

<deferred>
## Deferred Ideas

- **Vertical orientation for `AeroStepIndicator`** — current scope is horizontal-only (matches `AeroStepperWizard` Phase 10 success criteria). If a v2.x consumer needs a vertical step indicator (e.g. a long onboarding wizard that overflows horizontally), the orientation parameter can be added without breaking the existing API.
- **Public API for any Phase 7 primitive** — explicitly off the table for v2.0. If Phase 8/9/10 reveals that consumers want, e.g., a public `AeroCalendarGrid`, that becomes a v2.x or v3 enhancement with proper API design.
- **`AeroEyedropperButton` for `AeroColorPicker`** — already deferred at the milestone level (PROJECT.md "Out of Scope"); reiterated here because Phase 7's HSV primitives could trivially extend to support it. Not a Phase 7 concern.
- **A reusable `Modifier.aeroDragHandle(orientation, hitAreaPx, ...)` that bundles drag + invisible hit-area** — could subsume both `aeroDragSplitter` and any future drag-grip use. Not in scope; current consumers wrap manually. Re-visit if a third consumer appears in v2.x.
- **Compose UI screenshot tests / Paparazzi infra to automate the scratch-section visual check** — would replace the eyes-on confirmation with deterministic CI. Out of scope per Phase 6 carry-forward decision; the entire v2.0 milestone uses eyes-on visual sign-off.
- **`AeroPopupPositionProvider` rewrite to consolidate with `AeroCalendarPositionProvider`** — the two will overlap conceptually. Consolidating risks regressing the existing dropdown popup behavior; Phase 7 keeps them separate. A v2.x cleanup phase could merge them.
- **Adopting `kotlinx-datetime:0.7.x` (the breaking-rename version)** — Phase 7 pins `0.6.2`; upgrading to 0.7.x is a separate decision once the API stabilizes upstream. STACK.md notes the upgrade path.
- **Fixing the v1.0 `AeroDropdown` popup-offset regression at the same time as adding `AeroCalendarPositionProvider`** — tempting since both touch popup positioning, but explicitly out of v2.0 scope per STATE.md. Tracked separately for gap-closure.

</deferred>

---

*Phase: 07-shared-internal-primitives*
*Context gathered: 2026-04-30*
