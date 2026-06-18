# Roadmap: aero-compose-ui

## Overview

Three phases deliver a complete Compose Desktop UI component library styled after Windows Aero. Phase 1 establishes the theme system and glass modifiers — everything else depends on it. Phase 2 builds all atomic components (buttons, inputs, selection, sliders, lists) that define reusable interaction patterns. Phase 3 completes the library with composite containers, overlays, and window chrome components that depend on atomic primitives. The showcase application grows with each phase so the library is always demonstrable.

Milestone v1.1 (Icon System) appends Phases 4–6. Milestone v2.0 (Stateful + Layout) appends Phases 7–11. Phases 1–6 are complete and not renumbered.

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28)
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30)
- 🚧 **v2.0 Stateful + Layout** — Phases 7–11 (active)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

### v1.0 (Complete)

- [x] **Phase 1: Foundation** - Theme system, glass modifiers, module structure, showcase skeleton (completed 2026-04-27)
- [x] **Phase 2: Atomic Components** - Buttons, inputs, selection controls, sliders, list items, badges (completed 2026-04-28)
- [x] **Phase 3: Composite + Navigation** - Containers, overlays, dialogs, menus, tabs, window chrome (completed 2026-04-28)

### v1.1 Icon System (Complete)

- [x] **Phase 4: AeroIcons Foundation** - 138 Phosphor Regular ImageVector constants; lazy backing-property; explicitApi (completed 2026-04-29)
- [x] **Phase 5: Component Migrations + Dependency Removal** - 11 components migrated; materialIconsExtended removed; grep gate clean (completed 2026-04-29)
- [x] **Phase 6: Showcase IconsSection** - LazyVerticalGrid of 138 icons + search; ButtonsSection migrated; three-theme visual sign-off (completed 2026-04-29)

### v2.0 Stateful + Layout (Active)

- [x] **Phase 7: Shared Internal Primitives** - Internal foundation (CalendarGrid, ColorMath, HsvSquare+HueSlider, AeroDragSplitter, StepIndicator, AeroCalendarPositionProvider) enabling Phases 8–10; no new public API (completed 2026-05-04)
- [x] **Phase 8: Pickers** - AeroRangeSlider, AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker; kotlinx-datetime:0.6.2 added
 (completed 2026-06-18)
- [ ] **Phase 9: Data** - AeroDataTable (virtualized, sortable, selectable, resizable columns) + AeroTreeView (lazy expand); components/datatable/ package
- [ ] **Phase 10: Layout** - AeroAccordion, AeroSplitPane, AeroSidebar, AeroStepperWizard; components/layout/ package
- [ ] **Phase 11: Showcase + v2.0 Visual Sign-off** - DataSection, PickersSection, LayoutSection wired into ShowcaseApp; RangeSection extended; 16-item silent-failure checklist as milestone gate

## Phase Details

### Phase 1: Foundation
**Goal**: The library module exists with a complete theme system, glass modifiers, and explicit public API — and a running showcase skeleton demonstrates theme switching
**Depends on**: Nothing (first phase)
**Requirements**: FOUND-01, FOUND-02, FOUND-03, FOUND-04, FOUND-05, FOUND-06, FOUND-07, FOUND-08, FOUND-09, FOUND-10, SHW-01, SHW-02, SHW-03
**Success Criteria** (what must be TRUE):
  1. A consumer project can add one Gradle dependency, wrap content in `AeroTheme {}`, and all child composables receive AeroBlue, AeroDark, or Classic color tokens automatically
  2. A developer can call `Modifier.glassEffect()`, `Modifier.glassPanel()`, or `Modifier.glassSurface()` and the composable renders the correct gradient + border + shadow in a single `drawBehind` pass (no overdraw)
  3. Attempting to use any non-public type from the `:library` JAR produces a compile error — `explicitApi()` is enforced
  4. The `:showcase` app launches, shows an empty component grid grouped by category, and a theme switcher toggles between AeroBlue / AeroDark / Classic instantly
  5. A developer can supply a custom `AeroColorScheme` to `AeroTheme` and all glass surfaces and text reflect the custom tokens
**Plans**: 4 plans
- [x] 01-01-PLAN.md — Gradle skeleton: root build, version catalog, :library + :showcase modules with explicitApi (completed 2026-04-27)
- [x] 01-02-PLAN.md — AeroColorScheme (23 tokens, 3 presets) + AeroTypography data classes (TDD) (completed 2026-04-27)
- [x] 01-03-PLAN.md — AeroTheme composable + LocalAero* CompositionLocals + glass modifiers (single-pass drawBehind) (completed 2026-04-27)
- [x] 01-04-PLAN.md — :showcase skeleton: Main, ShowcaseApp, ThemeSwitcher, FoundationSection, PlaceholderSection (visual checkpoint) (completed 2026-04-27)

### Phase 2: Atomic Components
**Goal**: All self-contained interactive components are implemented and interactive in the showcase — hover, focus, disabled, and pressed states work correctly using theme tokens
**Depends on**: Phase 1
**Requirements**: BTN-01, BTN-02, BTN-03, BTN-04, INP-01, INP-02, INP-03, INP-04, INP-05, INP-06, SEL-01, SEL-02, SEL-03, SEL-04, SEL-05, DRP-01, DRP-02, RNG-01, RNG-02, LST-01, LST-02
**Success Criteria** (what must be TRUE):
  1. Every button variant (AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar) renders enabled/disabled/hover/pressed states with correct Aero glass styling — verifiable in the showcase
  2. Every text input (AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker) accepts keyboard input, shows focus animation, and exposes its value via state — verifiable in the showcase
  3. Every selection control (AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl) toggles state correctly and renders the correct Aero visual for each state
  4. AeroSlider moves a single thumb and emits the correct float value; AeroProgressBar displays both determinate (with %) and indeterminate (animated shimmer) modes
  5. AeroDropdown and AeroComboBox open a themed dropdown, allow item selection, and close — AeroComboBox also accepts free-text input
**Plans**: 6 plans
- [x] 02-01-PLAN.md — Buttons (BTN-01..04): AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar + ButtonsSection (completed 2026-04-28)
- [x] 02-02-PLAN.md — Text inputs (INP-01..06): AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker + InputSection (completed 2026-04-28)
- [x] 02-03-PLAN.md — Selection controls (SEL-01..05): AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl + SelectionSection (completed 2026-04-28)
- [x] 02-04-PLAN.md — Range + Lists (RNG-01..02, LST-01..02): AeroSlider, AeroProgressBar, AeroListItem, AeroBadge + RangeSection + ListSection (completed 2026-04-28)
- [x] 02-05-PLAN.md — Dropdowns (DRP-01..02): AeroDropdown + AeroComboBox + AeroPopupPositionProvider + DropdownSection (completed 2026-04-28)
- [x] 02-06-PLAN.md — Wire all sections into ShowcaseApp + ThemeSwitcher swap to AeroSegmentedControl + visual checkpoint (completed 2026-04-28)

### Phase 3: Composite + Navigation
**Goal**: All container, overlay, and navigation components are implemented — a complete Compose Desktop window can be built using only aero-compose-ui with no custom chrome or raw Material3 surfaces visible
**Depends on**: Phase 2
**Requirements**: CNT-01, CNT-02, CNT-03, CNT-04, CNT-05, CNT-06, OVL-01, OVL-02, OVL-03, OVL-04, OVL-05, OVL-06, OVL-07, OVL-08, NAV-01, NAV-02, NAV-03, NAV-04, NAV-05
**Success Criteria** (what must be TRUE):
  1. AeroCard, AeroPanel, AeroDivider, AeroGroupBox, AeroScrollArea, and AeroScrollBar render correctly with glass styling; AeroScrollArea scrolls arbitrary content and AeroScrollBar is compatible with Compose `ScrollState`
  2. AeroDialog and AeroAlertDialog appear as modal overlays; AeroToast/AeroSnackbar auto-dismiss after a configurable timeout; AeroNotificationBanner shows info/warning/error/success variants with a close button; AeroDrawer animates in from the side; AeroPopover positions next to its anchor; AeroContextMenu opens on right-click; AeroTooltip appears on hover with a delay
  3. AeroTitleBar renders the Aero gradient, draggable region, and Minimize/Maximize/Close buttons with hover effects — built inside `FrameWindowScope` using `undecorated=true` (without `transparent=true` to avoid the Win11 crash)
  4. AeroMenuBar shows top-level items with dropdown submenus; AeroTabBar switches between tabs; AeroStatusBar displays text sections and colored indicators at the bottom of the window
  5. The showcase is complete: every component from Phases 1-3 appears in the appropriate category section, is interactive (not a placeholder), and all three themes render correctly across every component
**Plans**: 8 plans
- [x] 03-01-PLAN.md — Popup infrastructure refactor + AeroScrollBar (CNT-06) + AeroScrollArea (CNT-05) + LocalScrollbarStyle wiring + Wave-0 stub tests (completed 2026-04-28)
- [x] 03-02-PLAN.md — Containers: AeroCard (CNT-01), AeroPanel (CNT-02), AeroDivider (CNT-03), AeroGroupBox (CNT-04) (completed 2026-04-28)
- [x] 03-03-PLAN.md — AeroTitleBar (NAV-01) + showcase Main.kt undecorated window + resize handles (completed 2026-04-28)
- [x] 03-04-PLAN.md — Modal overlays: AeroDialog (OVL-01), AeroAlertDialog (OVL-02), AeroDrawer (OVL-08) (completed 2026-04-28)
- [x] 03-05-PLAN.md — Anchored popups: AeroTooltip (OVL-03), AeroContextMenu (OVL-04), AeroPopover (OVL-07) (completed 2026-04-28)
- [x] 03-06-PLAN.md — Notifications: AeroToast/AeroToastHost (OVL-05), AeroNotificationBanner (OVL-06) (completed 2026-04-28)
- [x] 03-07-PLAN.md — Remaining navigation: AeroMenuBar (NAV-02), AeroStatusBar (NAV-03), AeroBreadcrumb (NAV-04), AeroTabBar (NAV-05) (completed 2026-04-28)
- [x] 03-08-PLAN.md — ShowcaseApp wiring: 3 new sections + AeroToastHost mount + theme hoisting + final visual checkpoint (completed 2026-04-28)

---

## v1.1 Icon System — Phase Details

### Phase 4: AeroIcons Foundation
**Goal**: The typed icon set exists and compiles — every one of the 138 Phosphor Regular ImageVector constants is accessible via `AeroIcons.*` autocomplete with lazy initialization, and the object satisfies `explicitApi()`
**Depends on**: Phase 3 (v1.0 complete; all migration targets already exist in :library)
**Requirements**: ICN-01, ICN-02, ICN-03
**Success Criteria** (what must be TRUE):
  1. A developer can write `Icon(AeroIcons.X, contentDescription = null, tint = colors.onSurface)` in any :library or :showcase file and the close-X glyph renders as a Phosphor Regular rounded-stroke vector — no text character, no Canvas draw
  2. `Icon(AeroIcons.CaretDown, ...)`, `Icon(AeroIcons.MagnifyingGlass, ...)`, `Icon(AeroIcons.Gear, ...)`, `Icon(AeroIcons.House, ...)`, `Icon(AeroIcons.Funnel, ...)` all compile and render the correct Phosphor glyphs (not their Material/Feather namesakes)
  3. `./gradlew :library:compileKotlin` passes with no `explicitApi()` errors on `AeroIcons.kt` or any `icons/` file; every public constant has an explicit `public` modifier
  4. Accessing `AeroIcons.X` for the first time does not cause a measurable startup spike — every constant uses the null-backed lazy getter pattern (`private var _X: ImageVector? = null; public val X: ImageVector get() = _X ?: loadX().also { _X = it }`), not an eager `val`
  5. Spot-check of 5 representative icons (at minimum: `X`, `CaretDown`, `MagnifyingGlass`, `Check`, `Info`) confirms `viewportWidth = 256f`, `strokeLineWidth = 16f`, `strokeLineCap = StrokeCap.Round`, `fill = Color.Transparent` in the generated Kotlin
**Phase notes:**
- Phosphor SVG source committed to `tools/phosphor-svgs/regular/` alongside a `.pin` file recording the exact `phosphor-icons/core` commit hash used for conversion
- Valkyrie CLI 1.1.1 used for batch conversion with `--output-format BackingProperty`; the Gradle build does NOT invoke Valkyrie at build time — generated `.kt` files are committed as ordinary source
**Plans**: 2 plans
- [x] 04-01-PLAN.md — Spike: doc corrections (139→138, stroke 12→16) + 7-icon vendoring + Valkyrie spike + facade authoring + first compile-pass (completed 2026-04-29)
- [x] 04-02-PLAN.md — Batch: verify questionable SVGs + bulk-fetch remaining 131 + Valkyrie full run + facade extension to 138 + final verification gate (completed 2026-04-29)

### Phase 5: Component Migrations + Dependency Removal
**Goal**: Every text glyph and Material Icons reference is replaced with `AeroIcons.*` across all :library components and tests, and `compose.materialIconsExtended` is removed from the Gradle dependency graph entirely
**Depends on**: Phase 4
**Requirements**: MIG-01, MIG-02, MIG-03, MIG-04, MIG-05, MIG-06, MIG-07, MIG-08, MIG-09, MIG-10, MIG-11, CLN-01, CLN-02, CLN-03
**Success Criteria** (what must be TRUE):
  1. `grep -rn 'Text("▲\|▼\|▶\|✕\|✓\|─\|□\|❒\|–")' library/src/` returns 0 hits; AeroSearchField's lowercase-x clear button is confirmed migrated
  2. `AeroCheckbox` renders `Icon(AeroIcons.Check, ...)` for checked state and `Icon(AeroIcons.Minus, ...)` for indeterminate; `AeroTitleBar` renders `AeroIcons.Minus`, `AeroIcons.Square`/`AeroIcons.FrameCorners`, `AeroIcons.X`
  3. `AeroSearchField` has no `SearchIcon()` Canvas composable; `AeroPasswordField` has no `EyeOpenIcon()`/`EyeClosedIcon()` Canvas composables — all deleted
  4. `./gradlew :library:compileKotlin` succeeds with `materialIconsExtended` removed; `grep -rn "androidx.compose.material.icons" library/src/` returns 0 hits
  5. JAR size after removal is documented; `AeroNumberSpinner` up/down icons are visually confirmed not sub-pixel-thin in all three themes
**Plans**: 5 plans
- [x] 05-01-PLAN.md — Wave 1: internal-only Text-glyph and Material-icon swaps (MIG-01..03, MIG-05..07, MIG-10, MIG-11) + inline AeroNumberSpinner visual checkpoint (completed 2026-04-29)
- [x] 05-02-PLAN.md — Wave 2: Canvas composable deletions (MIG-08, MIG-09) (completed 2026-04-29)
- [x] 05-03-PLAN.md — Wave 3: AeroTitleBar private restructure (MIG-04) (completed 2026-04-29)
- [x] 05-04-PLAN.md — Wave 4: Test rewrites (CLN-01) — gate for Wave 5 (completed 2026-04-29)
- [x] 05-05-PLAN.md — Wave 5: Dependency removal (CLN-02, CLN-03) + JAR-size pre/post measurement (completed 2026-04-29)

### Phase 6: Showcase IconsSection
**Goal**: The showcase has a scrollable, searchable grid of all 138 AeroIcons that serves as the visual sign-off checkpoint for the entire v1.1 milestone across all three themes
**Depends on**: Phase 5
**Requirements**: SHW-04, SHW-05, SHW-06
**Success Criteria** (what must be TRUE):
  1. `IconsSection` is visible in the running showcase after `FoundationSection`; it displays all 138 icons in a `LazyVerticalGrid(GridCells.Adaptive(80.dp))` with each cell showing the icon at 24dp and its Kotlin identifier name below
  2. Typing a query into the `AeroSearchField` filters the grid in real-time (case-insensitive substring); typing `"caret"` shows only the four CaretX icons; clearing restores all 138; an empty result shows a "not found" message
  3. Switching to AeroDark: all 138 icons are visible with correct contrast — no black or invisible icons; `AeroNumberSpinner` disabled-state icons confirmed visible in AeroDark
  4. Switching to Classic: all 138 icons render with correct tint; stroke-weight check passes eye-on-screen for representative icons
  5. `ButtonsSection` demo `AeroIconButton` cells use `Icon(AeroIcons.{CaretUp,CaretDown,X})` — no text glyphs remain; grep returns 0 hits
**Plans**: 3 plans
- [x] 06-01-PLAN.md — Create IconsSection.kt (138-icon grid + search/filter + click-to-copy) and wire into ShowcaseApp.kt (SHW-04, SHW-05) (completed 2026-04-29)
- [x] 06-02-PLAN.md — Migrate ButtonsSection.kt three text glyphs to Icon(AeroIcons.{CaretUp,CaretDown,X}) (SHW-06) (completed 2026-04-29)
- [x] 06-03-PLAN.md — Three-theme visual checkpoint (AeroBlue + AeroDark + Classic) — v1.1 milestone sign-off gate (completed 2026-04-29)

---

## v2.0 Stateful + Layout — Phase Details

### Phase 7: Shared Internal Primitives
**Goal**: All shared internal helpers that two or more v2.0 public components depend on are built, tested, and stable — Phases 8, 9, and 10 can proceed without per-component duplication of drag logic, calendar rendering, or color math
**Depends on**: Phase 6 (v1.1 complete)
**Requirements**: (enabling phase — no public requirements owned here; all 27 v2.0 requirements are owned by Phases 8–11. This phase delivers the internal infrastructure without which PICK-01..08 calendar/color work, DATA-04 column-resize drag, and LAYO-03..04/08..09 splitter+wizard indicator would each re-invent the same patterns with divergent bugs)
**Success Criteria** (what must be TRUE):
  1. `AeroCalendarGrid` internal composable renders a month grid (7-column day cells, prev/next month buttons, day-of-week header) and passes a 3-scenario unit test (current month, month boundary, leap year); `AeroCalendarPositionProvider` positions a popup wider than its anchor without clipping on a 1024dp simulated window
  2. `AeroColorMath` pure-function utility passes a round-trip unit test: `hsv(0f, 1f, 1f)` (pure red) converted to RGB and back to HSV returns hue within 0.001f tolerance — PITFALL-15 drift is confirmed absent at the utility level
  3. `AeroHsvColorSquare` and `AeroHueSlider` internal Canvas composables respond to drag on the first pixel of mouse movement (verified by a drag-start smoke test using `awaitPointerEventScope` loop — PITFALL-03 touchSlop pattern is established and working)
  4. `AeroDragSplitter` internal composable fires `onDrag` on first mouse movement in both horizontal and vertical orientations — the shared `awaitPointerEventScope` drag utility (that defuses PITFALL-03 for DataTable column resize and SplitPane) is confirmed working
  5. `AeroStepIndicator` internal composable renders step dots with connecting lines and visually distinguishes current / completed / upcoming states across all three themes
**Phase notes:**
  - PITFALL-03 (touchSlop=18dp): `awaitPointerEventScope` + manual loop is the ONLY correct drag pattern for all Canvas-based drag in v2.0. Establish as `AeroDragSplitter` shared utility here — never let an executor discover this mid-Phase 8 or 9.
  - PITFALL-02 (calendar popup width): `AeroCalendarPositionProvider` must be the first artifact — all 4 date/time pickers depend on it. Do NOT reuse `AeroDropdownPopup` (width-locked to anchor width).
  - PITFALL-15 (HSV drift): `AeroColorMath` must use HSV as single source of truth internally; RGB and HEX are derived views only. Unit tests validate this before Phase 8 ColorPicker work begins.
  - W11-01 (transparent=true): Any overlay in v2.0 uses `Popup(...)`, NOT `Dialog(undecorated=true, transparent=true)`. Rule applies to all phases; pre-flight grep is established here as the pattern.
**Plans**: 3 plans (2 complete + 1 gap-closure)
- [x] 07-01-PLAN.md — Logic + tests: kotlinx-datetime 0.6.2 add, touchslop spike, AeroColorMath + round-trip test, AeroCalendarPositionProvider + 4-scenario test, AeroCalendarGrid + 3-scenario test (green CI gate before any Canvas/drag code) — completed 2026-05-04, see 07-01-SUMMARY.md
- [x] 07-02-PLAN.md — Visuals + scratch: Modifier.aeroDragSplitter, AeroHsvColorSquare, AeroHueSlider, AeroStepIndicator, Phase7ScratchSection wired into ShowcaseApp.kt for SC3+SC4+SC5 eyes-on verification — completed 2026-05-04, see 07-02-SUMMARY.md
- [ ] 07-03-gap-closure-PLAN.md — Gap closure (UAT 2026-05-04): fix AeroCalendarGrid header stretching past day grid (gap-01, BLOCKING); glass-wrap StepIndicator + calendar-popup scratch demos (gaps 02/03); swap raw Material Buttons to AeroButton/AeroOutlinedButton at intrinsic width (gap-04)

### Phase 8: Pickers
**Goal**: All six picker components — AeroRangeSlider, AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, and AeroColorPicker — are publicly available in the library with correct behavior, using Phase 7 primitives and kotlinx-datetime:0.6.2
**Depends on**: Phase 7
**Requirements**: PICK-01, PICK-02, PICK-03, PICK-04, PICK-05, PICK-06, PICK-07, PICK-08
**Success Criteria** (what must be TRUE):
  1. `AeroRangeSlider` renders two draggable thumbs on a shared track; each thumb responds on the first pixel of mouse movement (no touchSlop delay — PITFALL-03 resolved); start thumb cannot cross end thumb; the range fill between thumbs updates live during drag (PICK-08)
  2. `AeroDatePicker` opens a popup calendar on trigger click; clicking a day closes the popup and calls `onDateSelected` with the correct `kotlinx.datetime.LocalDate`; prev/next month navigation works; today and selected day are visually distinct; placing the trigger near the right edge of a 1024dp window does not clip the calendar (PITFALL-02 resolved via `AeroCalendarPositionProvider`) (PICK-01)
  3. `AeroTimePicker` shows hour and minute fields (24h default, 12h optional); values clamp to valid ranges (0–23 h, 0–59 min); callback fires with `kotlinx.datetime.LocalTime` (PICK-03). `AeroDateTimePicker` popup stays open after date selection until `Apply` is clicked; callback fires with `kotlinx.datetime.LocalDateTime` only on Apply (PICK-04)
  4. `AeroDateRangePicker` shows two calendar months side-by-side; clicking start then end correctly highlights the intermediate days; `onRangeSelect` is NOT called after only a start click (partial state is NOT leaked — PITFALL-06); on windows narrower than ~560dp the calendars stack vertically (PICK-02)
  5. `AeroColorPicker` HSV square drag updates the color on first pixel movement; dragging the hue strip changes the hue displayed in the HSV square; all five controls (HSV square, hue strip, R/G/B sliders, HEX input) stay mutually synchronized without drift — typing `#FF0000`, dragging saturation to 50%, and dragging back to 100% reads `#FF0000` (PITFALL-15 resolved); swatches set the active color; `enableAlpha = true` adds the alpha slider (PICK-05, PICK-06, PICK-07)
**Phase notes:**
  - PITFALL-03 (touchSlop): ALL drag interactions in this phase — HSV square drag (PICK-05), hue strip drag (PICK-05), RangeSlider thumbs (PICK-08) — MUST use `awaitPointerEventScope` loop from Phase 7. `detectDragGestures` is banned for Canvas drag on Desktop.
  - PITFALL-02 + PITFALL-08 (popup positioning + first-frame flash): `AeroCalendarPositionProvider` from Phase 7 is the only position provider for all 4 date/time picker popups. Guard against `popupContentSize == IntSize.Zero` (not `>= windowSize`) to avoid first-frame position jump.
  - PITFALL-06 (DateRangePicker partial state): `onRangeSelect` must only fire when both start and end are committed. Internal state is a sealed type: `Idle | SelectingEnd(start) | Selected(start, end)`.
  - PITFALL-15 (HSV drift): `AeroColorMath` from Phase 7 is the only math layer. ColorPicker holds `(hue, saturation, value, alpha)` as internal truth; RGB sliders and HEX input are derived. Never store both HSV and RGB simultaneously.
  - PITFALL-07 (RangeSlider thumb overlap): Enforce minimum separation; draw the most-recently-moved thumb last (highest Z-order).
  - PITFALL-09 (AeroDark disabled cells): Disabled date cells in AeroDark must use `labelText` token, not `onSurface.copy(alpha=0.4f)` — confirmed during Phase 8 visual review, verified formally in Phase 11.
  - W11-01: All picker popups use `Popup(...)`, NOT `Dialog(transparent=true)`. Grep gate per plan.
  - Build order within phase: RangeSlider → DatePicker (validates CalendarGrid) → TimePicker → DateTimePicker (composition) → DateRangePicker (LARGE) → ColorPicker (LARGE).
**Plans**: 6 plans
- [ ] 08-01-PLAN.md — AeroRangeSlider (PICK-08): custom Canvas dual-thumb, awaitPointerEventScope, no-cross + z-order [wave 1]
- [ ] 08-02-PLAN.md — AeroDatePicker (PICK-01) + shared PickerPopupContainer (W11-02 popup surface) [wave 1]
- [ ] 08-03-PLAN.md — AeroTimePicker (PICK-03): TimeFields spinner row, 24h only, LocalTime [wave 2]
- [ ] 08-04-PLAN.md — AeroDateTimePicker (PICK-04): calendar+time, Apply/Cancel commit gate [wave 3]
- [ ] 08-05-PLAN.md — AeroDateRangePicker (PICK-02): dual-month, sealed state, responsive stacking + AeroCalendarGrid range extension [wave 4]
- [ ] 08-06-PLAN.md — AeroColorPicker + AeroColorPickerButton (PICK-05/06/07): HSV-truth panel, swatches, alpha [wave 1]

### Phase 9: Data
**Goal**: AeroDataTable and AeroTreeView are publicly available in the library — the table virtualizes thousands of rows without fps loss, supports Ctrl/Shift multi-selection by stable key, sorts by column header, and allows column width drag-resize; the tree lazily loads children via callback exactly once per node regardless of scroll behavior
**Depends on**: Phase 7 (AeroDragSplitter), Phase 8 (no hard dependency — can start after Phase 7 if Phase 8 runs in parallel, but sequential is preferred)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04, DATA-05, DATA-06
**Success Criteria** (what must be TRUE):
  1. `AeroDataTable` with 1,000 rows renders without observable fps drop; at rest with 60 rows visible, the LazyColumn item count above the fold does NOT equal 1,000 (virtualization is confirmed active — PITFALL-01); `AeroScrollArea` is NOT used inside DataTable — raw `LazyListState + AeroScrollBar` are wired directly (DATA-01)
  2. Clicking a column header cycles sort asc → desc → none; only one column shows a sort indicator (`AeroIcons.CaretUp` or `AeroIcons.CaretDown`) at any time; the indicator is absent on unsorted columns (DATA-02)
  3. Single-click selects a row; Ctrl-click toggles an individual row; Shift-click selects a contiguous range from the last-selected row. After sorting, previously selected rows remain highlighted at their new positions — selection is stored as `Set<RowKey>` via caller-supplied `key: (T) -> Any`, NOT as `Set<Int>` indices (PITFALL-04 resolved) (DATA-03)
  4. Dragging a column header divider resizes that column; the adjacent column reflows; no column collapses below its minimum width (40dp); the drag responds on first mouse movement (PITFALL-03 resolved via `AeroDragSplitter` from Phase 7) (DATA-04)
  5. `AeroTreeView` renders a hierarchy; clicking a node's expand indicator toggles it open/closed with animation; opening a node for the first time calls `onExpand` exactly once; scrolling a node off-screen and back does NOT re-fire `onExpand` (PITFALL-05 resolved — `childrenLoaded` lives in `SnapshotStateMap` above LazyColumn) (DATA-05, DATA-06)
**Phase notes:**
  - PITFALL-01 (LazyColumn in AeroScrollArea): DataTable MUST own its own `LazyColumn` with `LazyListState`, paired with standalone `AeroScrollBar(rememberScrollbarAdapter(lazyListState))`. `AeroScrollArea` is forbidden inside DataTable — add this as a KDoc comment and a grep gate in the plan.
  - PITFALL-03 (touchSlop): DataTable column resize uses `AeroDragSplitter` from Phase 7 (already resolved). No new `detectDragGestures` calls for drag in this phase.
  - PITFALL-04 (stale selection indices): Selection API is `Set<RowKey>` + `key: (T) -> Any`. This is an API design decision — must be locked in plan-01 before any implementation. Changing it post-ship is a breaking change.
  - PITFALL-05 (TreeView lazy callback repeat): `childrenLoaded: Boolean` lives in `SnapshotStateMap<NodeKey, NodeState>` at tree level, NOT inside node composables. LazyColumn item disposal cannot reset it.
  - PITFALL-10 (selection vs hover token): DataTable selected rows use `colors.borderSelected.copy(alpha = 0.15f)`, NOT `colors.primary.copy(alpha = 0.2f)`. Four-state color scheme (normal / hover / selected / selected+hover) locked in plan-01 design.
  - Horizontal scroll: header `Row` and data `LazyColumn` share one `ScrollState` — they scroll horizontally together. Do NOT use `stickyHeader` (JetBrains bugs #3016, #2940).
**Plans**: 3 plans
- [ ] 09-01-PLAN.md — Foundation: AeroScrollBar(LazyListState) overload + public column/table types + pure sort/selection/column-width logic with JUnit tests (locks PITFALL-04) [wave 1]
- [ ] 09-02-PLAN.md — AeroDataTable (DATA-01..04): virtualized LazyColumn + AeroScrollBar, glass header with 3-position sort + aeroDragSplitter resize, four-state Ctrl/Shift selection [wave 2]
- [ ] 09-03-PLAN.md — AeroTreeView (DATA-05..06): pure flattenTree + NodeState guard (JUnit) then function-model lazy tree with SnapshotStateMap once-only onExpand [wave 2]

### Phase 10: Layout
**Goal**: AeroAccordion, AeroSplitPane, AeroSidebar, and AeroStepperWizard are publicly available in the library — the accordion coordinates single/multi open state correctly, the split pane resizes both panes without collapse, the sidebar animates between three modes with tooltip labels in collapsed state, and the wizard advances only when per-step validation passes while preserving state on Back
**Depends on**: Phase 7 (AeroDragSplitter for SplitPane, AeroStepIndicator for StepperWizard)
**Requirements**: LAYO-01, LAYO-02, LAYO-03, LAYO-04, LAYO-05, LAYO-06, LAYO-07, LAYO-08, LAYO-09
**Success Criteria** (what must be TRUE):
  1. `AeroAccordion` in `mode = single`: opening section B while section A is open closes A before B opens — clicking two headers in sequence leaves exactly one section open; in `mode = multi` both can be open simultaneously; expand/collapse is animated, not instant (LAYO-01, LAYO-02)
  2. `AeroSplitPane` divider responds to drag on first mouse movement; dragging to either edge stops at `minFirstPaneSize` / `minSecondPaneSize` (both default 48dp) — neither pane collapses to zero (PITFALL-14 resolved); cursor changes to resize cursor on divider hover; the extended hit-area (~8–12dp) makes the 1dp visual line grabbable without pixel-hunting (LAYO-03, LAYO-04)
  3. `AeroSidebar` transitions between `expanded` (~240dp, icon+label), `collapsed` (~48dp, icon+tooltip), and `hidden` (0dp, not in layout); each transition is animated via `animateDpAsState`; hovering an icon in collapsed mode shows the item label in an `AeroTooltip`; the active item is highlighted with primary color; clicking any item calls `onItemClick` (LAYO-05, LAYO-06, LAYO-07)
  4. `AeroStepperWizard` renders the `AeroStepIndicator` (Phase 7) showing current/completed/upcoming step states; clicking Next on a step where `onValidate()` returns `false` keeps the user on the current step — Next is blocked; navigating Back and returning to a previously filled step preserves all composable state (step content is NOT recomposed from scratch on Back navigation — PITFALL-12 avoided by invoking `onValidate` only on button click, not in composable body) (LAYO-08, LAYO-09)
  5. All four components render correctly in all three themes (AeroBlue / AeroDark / Classic) with no visual regressions on the existing v1.x component set — confirmed by a targeted four-component visual review before handing off to Phase 11
**Phase notes:**
  - PITFALL-13 (Accordion state not lifted): Expansion state for ALL sections lives in `AeroAccordion` parent state — `expandedIndex: Int?` (single) or `expandedIndices: Set<Int>` (multi). Individual section composables receive `expanded: Boolean` and `onToggle: () -> Unit` as parameters. Never let section composables hold their own internal `expanded` state.
  - PITFALL-14 (SplitPane no clamp): Divider position is always clamped: `dividerPx.coerceIn(minFirstPaneSize.toPx(), totalSize - minSecondPaneSize.toPx())`. Expose `minFirstPaneSize: Dp = 48.dp` and `minSecondPaneSize: Dp = 48.dp` as parameters.
  - PITFALL-11 (Sidebar + SplitPane composition): `AeroSidebar` must NOT be placed inside a `AeroSplitPane` pane. Document in KDoc with a correct usage example showing `Row { AeroSidebar(...); content() }`. Showcase demo must keep Sidebar as a top-level layout sibling.
  - PITFALL-12 (onValidate in composable body): `onValidate` is called ONLY in the Next button `onClick` handler. `canProceed` parameter (caller-driven Boolean) controls the button's enabled state for live UI feedback — `onValidate` is a commit gate, not a live signal.
  - PITFALL-03 (touchSlop): `AeroSplitPane` divider uses `AeroDragSplitter` from Phase 7. No new drag implementations in this phase.
**Plans**: TBD

### Phase 11: Showcase + v2.0 Visual Sign-off
**Goal**: Every v2.0 component is demonstrated in the showcase with realistic mock data, and the full 16-item "looks done but isn't" silent-failure checklist from PITFALLS.md passes across all three themes — this checklist is the formal gate for v2.0 milestone sign-off
**Depends on**: Phases 8, 9, 10 (all public components must exist before showcase wiring and sign-off)
**Requirements**: SHW-07, SHW-08, SHW-09, SHW-10
**Success Criteria** (what must be TRUE):
  1. `DataSection` is live in the showcase with an `AeroDataTable` of ~100 mock rows and ~5–6 columns (mixed types: text, number, date, status badge); sort, Ctrl/Shift multi-selection, and column resize are all exercisable interactively; selecting row with key X, sorting, and verifying X remains highlighted — PITFALL-04 confirmed absent in the showcase (SHW-07)
  2. `PickersSection` contains all five picker components (DatePicker, TimePicker, DateTimePicker, DateRangePicker, ColorPicker) and `AeroRangeSlider`; each picker displays its current value as a `Text` below the component for UAT callback verification; all pickers open and close correctly in the running showcase (SHW-08)
  3. `LayoutSection` contains `AeroAccordion` (single + multi examples side-by-side), `AeroSplitPane` (horizontal + vertical), `AeroSidebar` with a mode toggle button, and `AeroStepperWizard` with 3–4 steps including at least one step with a validation gate; all four components are interactive in the showcase (SHW-09)
  4. The full 16-item "looks done but isn't" checklist from PITFALLS.md passes eye-on-screen in all three themes (AeroBlue / AeroDark / Classic): virtualization verified, selection-after-sort verified, drag response on first movement verified (HSV square + RangeSlider + DataTable column splitter), AeroDark disabled cells readable, `transparent=true` grep returns zero results, `AeroScrollArea` inside DataTable/TreeView grep returns zero results — all 16 items PASS before v2.0 milestone is signed off (SHW-10)
  5. `RangeSection` in the existing showcase is extended with a row demonstrating `AeroRangeSlider`; no structural changes to existing section files; `ShowcaseApp.kt` receives exactly three new calls: `DataSection()`, `PickersSection()`, `LayoutSection()`
**Phase notes:**
  - PITFALL-09 (AeroDark disabled cells): Three-theme visual sign-off must explicitly verify disabled date cells are visible in AeroDark. Use `labelText` token for disabled cells, not `onSurface.copy(alpha=0.4f)`.
  - PITFALL-10 (selection vs hover): DataTable showcase must exercise all four row states (normal / hover / selected / selected+hover) in AeroDark where token contrast is lowest.
  - W11-01 (transparent=true grep gate): `grep -rn "transparent = true"` across all new v2.0 source files must return 0 hits. This is a mandatory pre-sign-off check.
  - W11-02 (shadow clipping): Popup depth uses border + glassBorder + two-layer background (existing `AeroDropdownPopup` technique), NOT `Modifier.shadow` — confirmed during visual sign-off.
  - The 16-item checklist from PITFALLS.md is the formal sign-off gate. Do not mark Phase 11 complete without every item checked PASS.
  - Showcase wiring follows v1.0/v1.1 pattern: new Section files added, `ShowcaseApp.kt` receives new calls, no structural changes to existing sections.
**Plans**: TBD

---

## Progress

**Execution Order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 4/4 | Complete | 2026-04-27 |
| 2. Atomic Components | v1.0 | 6/6 | Complete | 2026-04-28 |
| 3. Composite + Navigation | v1.0 | 8/8 | Complete | 2026-04-28 |
| 4. AeroIcons Foundation | v1.1 | 2/2 | Complete | 2026-04-29 |
| 5. Component Migrations + Dep Removal | v1.1 | 5/5 | Complete | 2026-04-29 |
| 6. Showcase IconsSection | v1.1 | 3/3 | Complete | 2026-04-29 |
| 7. Shared Internal Primitives | 3/3 | Complete   | 2026-06-17 | - |
| 8. Pickers | 6/6 | Complete   | 2026-06-18 | - |
| 9. Data | 1/3 | In Progress|  | - |
| 10. Layout | v2.0 | 0/TBD | Not started | - |
| 11. Showcase + v2.0 Visual Sign-off | v2.0 | 0/TBD | Not started | - |
