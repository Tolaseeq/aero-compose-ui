# Project Research Summary

**Project:** aero-compose-ui — v2.0 Stateful + Layout
**Domain:** Compose Desktop UI component library (Windows Aero / glassmorphism visual style)
**Researched:** 2026-04-30
**Confidence:** HIGH

## Executive Summary

v2.0 adds **12 new components** (8 complex stateful + 4 advanced layout) to a mature Compose Desktop library. Research across stack, features, architecture, and pitfalls converged on a clear story: this milestone is mostly **hand-rolled work over existing primitives**, with exactly **one new dependency** (`kotlinx-datetime:0.6.2` for the four date/time pickers) and a small set of **shared internal helpers** that must be built first to avoid per-component duplication.

The dominant risk is not "too much new tech" — it's **silent-failure layout/state bugs** specific to Compose Desktop. Two pitfalls (PITFALL-01: `LazyColumn` inside `AeroScrollArea` kills DataTable virtualization; PITFALL-03: Desktop `touchSlop = 18dp` silently breaks all `detectDragGestures` for Canvas drags affecting ColorPicker, RangeSlider, DataTable column resize) are showstoppers that compile and run but produce fundamentally wrong behavior. Three components are **LARGE** by feature scope (DataTable, DateRangePicker, ColorPicker) and deserve dedicated phases; the other nine cluster naturally into shared-primitive phases.

The recommended approach: **Phase 7 builds the five shared internal primitives** (CalendarGrid, ColorMath, HsvSquare+HueSlider, DragSplitter, StepIndicator) as the foundation. **Phases 8–10 then build the public components** in dependency order, with the three LARGE components getting isolated phases and the SMALL/MEDIUM ones grouped. The single visual checkpoint at the end of v2.0 must include a 16-item "looks done but isn't" checklist (drag response, three-theme contrast, disabled state, `transparent=true` grep gate) to catch the silent failures.

## Key Findings

### Recommended Stack

The library is already on a stable Kotlin/Compose Desktop foundation (Kotlin 2.1.21, CMP 1.7.3, JDK 17, Material 3 wrapper). v2.0 does NOT change this. **Only one new dependency is needed**, and it is for the date/time picker family. Everything else — color math, split pane drag, calendar grid, virtualization wiring — is hand-rolled using primitives already present in Compose Desktop or in the existing aero-compose-ui codebase.

**Core technologies:**
- **`org.jetbrains.kotlinx:kotlinx-datetime:0.6.2`** (NEW) — `LocalDate` / `LocalTime` / `LocalDateTime` as the public picker API types and for month arithmetic. v0.6.2 is the last stable release before the 0.7.0 breaking renames; safe with Kotlin 2.1.21 per Kotlin's backward-compat policy.
- **`androidx.compose.foundation.lazy.LazyColumn` + `rememberScrollbarAdapter(LazyListState)`** (existing) — DataTable virtualization. Header is a plain `Row` outside the LazyColumn sharing the same `ScrollState` (NOT `stickyHeader` — JetBrains bugs #3016, #2940).
- **`Color.hsv(hue, sat, val, alpha)`** (existing in `compose.ui.graphics`) — ColorPicker forward conversion. Inverse (Color → HSV) is ~20 lines of math, hand-rolled.
- **`awaitPointerEventScope` + manual loop** (existing in `androidx.compose.ui.input.pointer`) — replaces `detectDragGestures` for ALL drag interactions on Desktop (touchSlop 18dp issue, JetBrains/compose-jb #343).
- **`Popup` + `LazyVerticalGrid` (7-column)** (existing) — calendar grid for all 4 date/time pickers. Material3 `DatePicker` is NOT viable (Android-only internals; community-confirmed crashes on Compose Desktop).

Explicitly NOT added: `components-splitpane-desktop` (no stable 1.7.x; hand-roll ~80 lines), color-picker libraries (Android-only or wrong visual contract), Material3 `DatePicker` (crashes).

Full stack details: `.planning/research/STACK.md`

### Expected Features

Anti-features matter more than features here — every component has 1-2 footguns that produce real bugs if naively included. The features themselves are well-understood across WPF/JavaFX/Qt/Flutter/MUI X (HIGH consensus).

**Must have (table stakes per locked v2.0 scope):**
- DataTable: sortable headers, single/multi row selection (Ctrl/Shift), virtualized rows, resizable columns
- TreeView: lazy children via `onExpand`, expand/collapse, optional icons
- Date/time pickers: popup-based UI for all 4 (DatePicker, TimePicker, DateTimePicker, DateRangePicker)
- ColorPicker: HSV square + hue + RGB sliders + HEX input + swatches
- RangeSlider: dual-thumb (composition over AeroSlider)
- Accordion: `mode = single | multi`
- SplitPane: 2-pane public API + N-pane via nesting
- Sidebar: expanded / collapsed-icons / hidden modes
- StepperWizard: linear with per-step `onValidate`

**Critical anti-features (hard-exclude, will produce bugs if included):**
- DataTable: cell editing, column reordering, filtering UI (API surface explosion)
- DateTimePicker: auto-close on date selection before time is set (premature `onChange` fire)
- DateRangePicker: auto-swap start/end when user picks end before start (breaks user mental model)
- ColorPicker: round-tripping HSV→RGB→HSV on each edit (color drift bug)
- StepperWizard: destroying step composable state on Back navigation (forces form re-entry)
- SplitPane: 1dp divider with no invisible hit area (impossible to grab)

**Already deferred (out of scope, per PROJECT.md):**
- Inline-mode date/time pickers — only popup
- DataTable cell editing, column reorder, filtering UI
- TreeView drag-and-drop
- ColorPicker eyedropper
- StepperWizard branching
- AeroSidebar drag-to-resize width

**Complexity ratings (drives phase load balancing):**
- **LARGE (3):** AeroDataTable, AeroDateRangePicker, AeroColorPicker — each deserves an isolated or near-isolated phase
- **MEDIUM (5):** AeroDatePicker, AeroDateTimePicker, AeroTreeView, AeroSidebar, AeroStepperWizard, AeroSplitPane
- **SMALL (3):** AeroRangeSlider, AeroTimePicker, AeroAccordion — bundle with larger items

Full feature breakdown per component: `.planning/research/FEATURES.md`

### Architecture Approach

Four new packages under `com.mordred.aero.components` + one extension to existing `range/`. No new Gradle modules — everything stays in `:library` per locked v2.0 decision. The critical insight: **five shared internal helpers must be extracted first** to prevent per-component duplication and inconsistency.

**New packages:**
1. `components/range/` (extension) — `AeroRangeSlider.kt` joins existing AeroSlider here
2. `components/pickers/` (NEW) — All 4 date/time pickers + ColorPicker + `internal/` for shared helpers
3. `components/datatable/` (NEW) — AeroDataTable + AeroTreeView + `internal/` for header/row/node composables
4. `components/layout/` (NEW) — AeroAccordion + AeroSplitPane + AeroSidebar + AeroStepperWizard + `internal/`

**Shared internal primitives (Phase 7 — no public output, unlocks Phases 8–10):**
1. **`AeroCalendarGrid`** — month grid with prev/next nav; used by DatePicker + DateRangePicker; DateTimePicker reuses via composition
2. **`AeroColorMath`** — pure HSV↔RGB↔HEX functions; HSV-first state to prevent drift; unit-testable
3. **`AeroHsvColorSquare` + `AeroHueSlider`** — Canvas-based color selection primitives for ColorPicker (use `awaitPointerEventScope`, NOT `detectDragGestures`)
4. **`AeroDragSplitter`** — draggable divider used by SplitPane and DataTable column resize; built on `awaitPointerEventScope`
5. **`AeroStepIndicator`** — step dot/line row used by StepperWizard
6. **`AeroCalendarPositionProvider`** — calendar popups exceed trigger width; cannot reuse `AeroDropdownPopup` width-locking pattern

**Reuse from v1.0/v1.1:**
- `AeroPopupPositionProvider` (popup placement for DatePicker/TimePicker triggers, but NOT for the calendar popup width itself)
- `AeroSlider` color pattern (RangeSlider wraps Material3 RangeSlider with same `SliderDefaults.colors()`)
- `AeroScrollBar` + raw `LazyListState` (DataTable virtualization — **NOT `AeroScrollArea`**, see PITFALL-01)
- `AeroTooltip` (Sidebar collapsed-mode icon labels)
- `AeroTextField` (ColorPicker HEX input + TimePicker hour/minute fields)
- `glassPanel(cornerRadius=8.dp)` + `border(glassBorder)` (all popup panels)

**Showcase wiring:**
- 3 new Section files: `DataSection.kt`, `PickersSection.kt`, `LayoutSection.kt`
- 1 row added to existing `RangeSection.kt` for AeroRangeSlider
- `ShowcaseApp.kt` adds 3 calls (`DataSection()`, `PickersSection()`, `LayoutSection()`) — no structural changes to existing sections

Full architecture: `.planning/research/ARCHITECTURE.md`

### Critical Pitfalls

15 named pitfalls + 2 Win11-specific are documented in PITFALLS.md. The two showstoppers are silent failures (compiles and runs, behavior is wrong); the rest are scoped to specific phases.

1. **PITFALL-01: `LazyColumn` inside `AeroScrollArea` kills DataTable virtualization.** Wrapping `LazyColumn` in `verticalScroll` (which AeroScrollArea uses) gives infinite vertical constraints → all rows render eagerly → no virtualization. Same root cause family as the v1.0 AeroDropdown popup-offset regression. **Prevention:** DataTable must use raw `LazyListState + AeroScrollBar` directly, NOT AeroScrollArea. Document the exclusion in DataTable KDoc and the `internal/` README.

2. **PITFALL-03: Compose Desktop `touchSlop = 18dp` breaks all `detectDragGestures` for Canvas drags.** Mouse movement between events is 1–3px, always below the 18dp threshold; `detectDragGestures` callback never fires. Affects AeroColorPicker HSV square, AeroRangeSlider dual thumbs, AeroDataTable column resize. Confirmed upstream JetBrains/compose-jb #343. **Prevention:** Use `awaitPointerEventScope` + manual loop. Establish the pattern as a shared utility (`AeroDragSplitter` and similar) in Phase 7 — never let an executor "discover" this mid-phase.

3. **PITFALL-02: Calendar popup width vs trigger width mismatch.** `AeroDropdownPopup` locks width to `anchorWidth`. A 300dp calendar popup on a 240dp trigger field will mispositon; AeroDateRangePicker at ~560dp is acute. **Prevention:** Write `AeroCalendarPositionProvider` as the FIRST artifact in the pickers phase — all 4 picker components depend on it.

4. **PITFALL-04: DataTable selection stored as `Set<Int>` indices becomes stale after sort.** Row 5 before sort is not row 5 after sort; selection silently jumps. **Prevention:** Use `Set<RowKey>` with caller-supplied `key: (T) -> Any`. This is an API design decision — must be locked at planning time, not discovered after API ships (would be a breaking change).

5. **PITFALL-05: TreeView `onExpand` re-fires when node scrolls out and back in** (LazyColumn composable disposal). Lazy callback gets called multiple times for the same node, potentially causing duplicate fetches. **Prevention:** `childrenLoaded: Boolean` lives in a `SnapshotStateMap` above the LazyColumn, NOT in the node's composable.

Plus: HSV round-trip drift (PITFALL — ColorPicker), AeroDateRangePicker dual-calendar layout density on small windows, cross-theme contrast on date-picker disabled cells, accordion state ownership traps, sidebar tooltip flicker on collapse animation, Win11 `transparent=true` rule extends to `DialogWindow` for any picker that uses Dialog instead of Popup.

Full pitfall list with phase mapping + 16-item "looks done but isn't" checklist: `.planning/research/PITFALLS.md`

## Implications for Roadmap

The four researchers converged on a 4-phase structure for v2.0. Phase numbering continues from v1.1 (last phase was 6).

### Phase 7: Shared Internal Primitives

**Rationale:** Five primitives (CalendarGrid, ColorMath helpers, HsvSquare+HueSlider, DragSplitter, StepIndicator) + one position provider (AeroCalendarPositionProvider) are dependencies for ≥2 downstream components each. Building them per-component duplicates work, guarantees inconsistency, and burns time when (not if) the touchSlop / popup-width / HSV-drift pitfalls re-surface in each component.
**Delivers:** No public components shipped. New `internal/` files in `components/pickers/`, `components/datatable/`, `components/layout/`. Unit tests for `AeroColorMath` (HSV↔RGB round-trip stability).
**Addresses:** Architecture extraction discipline; pre-emptively defuses PITFALL-02 (popup width), PITFALL-03 (touchSlop), HSV drift.
**Avoids:** Per-component drag-pattern divergence; per-component calendar-grid divergence; HSV round-trip drift.

### Phase 8: Pickers (Date / Time / Color)

**Rationale:** All five picker components (DatePicker, TimePicker, DateTimePicker, DateRangePicker, ColorPicker) plus RangeSlider depend on Phase 7 primitives. Date pickers also need `kotlinx-datetime:0.6.2` (the only new dep). RangeSlider is trivial (composition over AeroSlider) and can ride along with TimePicker (also SMALL) for phase load balancing.
**Delivers:** AeroRangeSlider, AeroTimePicker, AeroDatePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker. Build order: RangeSlider → DatePicker (validates CalendarGrid) → TimePicker → DateTimePicker (composition) + DateRangePicker (LARGE) + ColorPicker (LARGE).
**Uses:** `kotlinx-datetime:0.6.2` (Stack); CalendarGrid, ColorMath, HsvSquare+HueSlider, AeroCalendarPositionProvider (Phase 7); existing `AeroSlider`, `AeroTextField`, `AeroPopupPositionProvider`, `glassPanel`, `AeroIcons.{CaretLeft,CaretRight}`.
**Implements:** `components/pickers/` package; RangeSlider added to existing `components/range/`.

### Phase 9: Data (DataTable + TreeView)

**Rationale:** AeroDataTable is the LARGEST component in v2.0 — virtualization + selection + sort + resize is enough work for a dedicated phase. AeroTreeView shares the LazyColumn + lazy-loading pattern but is MEDIUM complexity; pairing it lets one phase own the "lazily-rendered hierarchical/tabular data" mental model. Both depend on `AeroDragSplitter` (Phase 7) and on the raw `LazyListState + AeroScrollBar` integration (NOT AeroScrollArea — PITFALL-01).
**Delivers:** AeroDataTable, AeroTreeView. DataTable selection-by-key API locked in plan-01 (PITFALL-04).
**Uses:** `LazyColumn`, `LazyListState`, `rememberScrollbarAdapter`, `AeroScrollBar`, `AeroDragSplitter` (Phase 7). NOT `AeroScrollArea`.
**Implements:** `components/datatable/` package.

### Phase 10: Layout (Accordion + SplitPane + Sidebar + StepperWizard)

**Rationale:** The four advanced layout components are all MEDIUM/SMALL with no LARGE outliers. They share a "structural composable that owns child layout" pattern. SplitPane uses `AeroDragSplitter` (Phase 7); StepperWizard uses `AeroStepIndicator` (Phase 7); Accordion and Sidebar are mostly state + transitions over existing primitives.
**Delivers:** AeroAccordion (single + multi modes), AeroSplitPane (2-pane + nesting docs), AeroSidebar (expanded/collapsed/hidden), AeroStepperWizard (linear + per-step onValidate).
**Uses:** AeroDragSplitter, AeroStepIndicator (Phase 7); AeroTooltip (Sidebar collapsed mode); existing animation primitives; glass modifiers.
**Implements:** `components/layout/` package.

### Phase 11: Showcase Sections + v2.0 Visual Sign-off

**Rationale:** v1.1 demonstrated the value of treating the visual checkpoint as a first-class plan with its own SUMMARY.md (Phase 6 P03 — "three-theme visual sign-off"). v2.0 has 12 components and a 16-item "looks done but isn't" checklist — it deserves a dedicated phase, not a tail-end plan in Phase 10.
**Delivers:** `DataSection.kt`, `PickersSection.kt`, `LayoutSection.kt` wired into `ShowcaseApp.kt`; `RangeSection.kt` extended with RangeSlider row. Three-theme visual sign-off (AeroBlue / AeroDark / Classic) with the 16-item checklist from PITFALLS.md as the formal milestone gate.
**Avoids:** Silent layout failures slipping past code-only verification (PITFALL-01, PITFALL-03, cross-theme contrast traps).

### Phase Ordering Rationale

- **Phase 7 first because primitives.** Five components (DataTable, ColorPicker, RangeSlider, SplitPane, DateRangePicker) need shared drag/calendar/color helpers. Building them in a foundation phase prevents drift and pre-empts the silent-failure pitfalls (touchSlop, popup width, HSV drift).
- **Phase 8 (Pickers) second because new dependency.** `kotlinx-datetime:0.6.2` adds in this phase. All 5 pickers plus the SMALL RangeSlider land here. Build order inside the phase: RangeSlider → DatePicker → TimePicker → composition pickers (DateTimePicker, DateRangePicker) → ColorPicker.
- **Phase 9 (Data) isolated because LARGE.** DataTable is the heaviest single component. Pairing only with TreeView (which shares LazyColumn patterns) avoids overload.
- **Phase 10 (Layout) is naturally cohesive.** Accordion + SplitPane + Sidebar + StepperWizard all share the "structural composable" mental model and have no LARGE outlier.
- **Phase 11 (Showcase + sign-off) mirrors v1.1's successful visual-checkpoint-as-plan convention.** With 12 new components + 16-item silent-failure checklist, it cannot be a tail plan in Phase 10.

### Research Flags

Phases likely needing deeper research during planning (`gsd-phase-researcher` recommended):
- **Phase 7:** Shared `awaitPointerEventScope` drag pattern + HSV math correctness need careful design upfront. Touchscreen vs mouse semantics on Compose Desktop need confirmation.
- **Phase 8:** `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 compile validation at first use; calendar popup density on AeroDateRangePicker (dual-month layout) needs visual mockup before implementation.
- **Phase 9:** `LazyColumn` + horizontal scroll + measured columns triple-constraint (PITFALL-01 family); confirm `rememberScrollbarAdapter(LazyListState)` API surface in CMP 1.7.3.

Phases with standard patterns (skip phase research):
- **Phase 10:** Layout components are well-understood patterns; existing aero-compose-ui conventions (animation, glass, tooltip) cover everything needed.
- **Phase 11:** Showcase wiring follows v1.0/v1.1 pattern verbatim.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | `kotlinx-datetime` 0.6.2 verified on Maven Central; SplitPane absence verified; Compose built-ins verified in CMP 1.7.3. One MEDIUM: exact `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 compatibility inferred from policy, validate at first compile. |
| Features | HIGH | Cross-verified across WPF, JavaFX, Qt, Flutter, MUI X, AG Grid, W3C ARIA, NN/g — strong consensus on table stakes and anti-features. |
| Architecture | HIGH | All findings derived from direct source inspection of existing `AeroScrollArea.kt`, `AeroDropdownPopup.kt`, `AeroPopupPositionProvider.kt`, `AeroSlider.kt`. |
| Pitfalls | HIGH | All 15+2 pitfalls grounded in either source code, confirmed upstream issues (#343, #3016, #2940, #3757), or token-level color math from `AeroColorScheme.kt`. |

**Overall confidence:** HIGH

### Gaps to Address

- **`kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 first-compile validation** — should be a Phase 8 plan-01 acceptance criterion. If it fails, fallback is `kotlinx-datetime:0.7.1-0.6.x-compat` (documented upgrade path).
- **AeroDateRangePicker minimum popup width** — research estimates ~560dp for dual-month layout; needs a visual mockup at Phase 8 design time before implementing the layout. Mitigation strategy: `BoxWithConstraints` to fall back to vertical stack on narrow windows.
- **`rememberScrollbarAdapter(LazyListState)` API verification** — HIGH probability available since CMP 1.4; validate at Phase 9 plan-01.
- **Whether upstream `touchSlop` issue #343 was silently fixed in CMP 1.7.x** — 1-minute drag test at Phase 7 plan-01 confirms which path to take.
- **Compose Desktop `LazyColumn` + horizontal scroll triple-constraint behavior** — needs a Phase 9 spike to validate the "header Row + body LazyColumn share same horizontal ScrollState" pattern before committing to it.

## Sources

### Primary (HIGH confidence)
- Existing aero-compose-ui source: `library/src/main/kotlin/com/mordred/aero/components/{containers/AeroScrollArea.kt, popup/AeroDropdownPopup.kt, popup/AeroPopupPositionProvider.kt, range/AeroSlider.kt, theme/AeroColorScheme.kt, theme/GlassModifiers.kt}`
- JetBrains/compose-jb tracker: #343 (touchSlop), #3016 + #2940 (stickyHeader), #3757 (Win11 transparent crash)
- Maven Central: `kotlinx-datetime` 0.6.2 (latest stable pre-0.7.0)
- W3C ARIA APG: tree, dialog, slider patterns
- Compose Desktop official docs (LazyColumn, Popup, awaitPointerEventScope)

### Secondary (MEDIUM confidence)
- WPF, JavaFX, Qt Widgets/QML feature comparisons (cross-framework table-stakes consensus)
- MUI X / AG Grid / react-datepicker GitHub issue trackers (anti-features sourcing)
- Kotlin Slack #compose-desktop community confirmation: Material3 `DatePicker` crashes on Desktop
- NN/g UX research on date picker patterns

### Tertiary (LOW confidence)
- Exact `AeroDateRangePicker` min width estimate (~560dp) — extrapolated from dp/cell ratios, validate with mockup
- `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 compile compat inferred from Kotlin's backward-compat policy, no explicit incompatibility found

---
*Research completed: 2026-04-30*
*Ready for roadmap: yes*
