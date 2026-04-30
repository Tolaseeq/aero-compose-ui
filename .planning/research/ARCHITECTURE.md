# Architecture Research

**Domain:** Compose Desktop UI component library — v2.0 Stateful + Layout integration
**Researched:** 2026-04-30
**Confidence:** HIGH (all findings verified directly from source files)

---

## Standard Architecture

### System Overview

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                           :showcase (currentOs)                               │
│  ShowcaseApp.kt — verticalScroll Column — one Section composable per group    │
│  DataSection  PickersSection  LayoutSection  (new v2.0 sections)              │
│  + existing: Buttons/Input/Selection/Dropdown/Range/List/Containers/Overlays  │
├───────────────────────────────────────────────────────────────────────────────┤
│                   :library (compose.desktop.common — platform-neutral JAR)    │
│                                                                               │
│  Public API Layer                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │
│  │  components  │ │  components  │ │  components  │ │  components  │        │
│  │  /datatable  │ │  /pickers    │ │  /layout     │ │  /range      │        │
│  │  AeroDataTable│ │ AeroDatePicker│ │ AeroAccordion│ │AeroRangeSlider│       │
│  │  AeroTreeView│ │ AeroTimePicker│ │ AeroSplitPane│ │  (extends    │        │
│  │              │ │AeroDateTimePkr│ │ AeroSidebar  │ │  AeroSlider) │        │
│  │              │ │AeroDateRangePk│ │AeroStepperWiz│ │              │        │
│  │              │ │AeroColorPicker│ │              │ │              │        │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘        │
│         │                │                │                │                 │
│  Internal Shared Primitives (new — to be extracted in Phase 7)               │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  components/internal/                                                │    │
│  │  AeroCalendarGrid  AeroDragSplitter  AeroHsvColorSquare              │    │
│  │  AeroHueSlider     AeroColorMath     AeroStepIndicator               │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
│                                                                               │
│  Existing Reusable Infrastructure (v1.0/v1.1 — unchanged)                    │
│  ┌──────────────────────────────────────────────────────────────────────┐    │
│  │  popup/AeroPopupPositionProvider  popup/AeroPopupSide                │    │
│  │  containers/AeroScrollArea  containers/AeroScrollBar                 │    │
│  │  range/AeroSlider  input/AeroSearchField  input/AeroTextField        │    │
│  │  theme/AeroTheme  theme/glassEffect  theme/glassPanel                │    │
│  │  theme/glassSurface  icons/AeroIcons                                 │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Lives In |
|-----------|---------------|----------|
| AeroDataTable | Virtualized rows (LazyColumn), sortable headers, single/multi selection, resizable columns | components/datatable/ |
| AeroTreeView | Hierarchical lazy-load tree, expand/collapse, optional icons | components/datatable/ |
| AeroDatePicker | Popup calendar, single date selection | components/pickers/ |
| AeroTimePicker | Hour+minute drum/scroll selection | components/pickers/ |
| AeroDateTimePicker | Combines DatePicker + TimePicker in a single popup | components/pickers/ |
| AeroDateRangePicker | Dual-calendar range selection | components/pickers/ |
| AeroColorPicker | HSV square + hue bar + RGB sliders + HEX field + swatches | components/pickers/ |
| AeroRangeSlider | Dual-thumb slider extending AeroSlider visual | components/range/ |
| AeroAccordion | Collapsible sections, single/multi mode | components/layout/ |
| AeroSplitPane | 2-pane drag divider, horizontal/vertical | components/layout/ |
| AeroSidebar | Persistent side navigation, 3 modes | components/layout/ |
| AeroStepperWizard | Linear step process, per-step validation | components/layout/ |

---

## Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
├── theme/                          # UNCHANGED — v1.0 foundation
│   ├── AeroColorScheme.kt
│   ├── AeroTheme.kt
│   ├── AeroTypography.kt
│   └── GlassModifiers.kt
├── icons/                          # UNCHANGED — v1.1 AeroIcons
│   ├── AeroIcons.kt
│   └── internal/                   # 138 generated ImageVector files
├── components/
│   ├── buttons/                    # UNCHANGED
│   ├── input/                      # UNCHANGED
│   ├── selection/                  # UNCHANGED
│   ├── dropdown/                   # UNCHANGED
│   ├── range/
│   │   ├── AeroSlider.kt           # UNCHANGED
│   │   ├── AeroProgressBar.kt      # UNCHANGED
│   │   └── AeroRangeSlider.kt      # NEW — Phase 8 (trivial: wraps Material3 RangeSlider)
│   ├── list/                       # UNCHANGED
│   ├── containers/                 # UNCHANGED
│   ├── overlay/                    # UNCHANGED
│   ├── navigation/                 # UNCHANGED
│   ├── popup/                      # UNCHANGED — shared popup infrastructure
│   │
│   ├── datatable/                  # NEW package — Phase 9
│   │   ├── AeroDataTable.kt        # Public: table with virtualized rows, sort, selection
│   │   ├── AeroTreeView.kt         # Public: lazy hierarchical tree
│   │   ├── AeroDataTableColumn.kt  # Public data class — column descriptor
│   │   └── internal/
│   │       ├── AeroTableHeader.kt  # Internal — sortable header row
│   │       ├── AeroTableRow.kt     # Internal — selectable row composable
│   │       └── AeroTreeNode.kt     # Internal — single tree node composable
│   │
│   ├── pickers/                    # NEW package — Phase 8
│   │   ├── AeroDatePicker.kt       # Public
│   │   ├── AeroTimePicker.kt       # Public
│   │   ├── AeroDateTimePicker.kt   # Public (depends on DatePicker + TimePicker)
│   │   ├── AeroDateRangePicker.kt  # Public (depends on DatePicker)
│   │   ├── AeroColorPicker.kt      # Public
│   │   └── internal/
│   │       ├── AeroCalendarGrid.kt # Internal — shared by DatePicker + DateRangePicker
│   │       ├── AeroHsvColorSquare.kt  # Internal — HSV 2D gradient square
│   │       ├── AeroHueSlider.kt    # Internal — horizontal hue strip (custom Canvas)
│   │       └── AeroColorMath.kt    # Internal — HSV↔RGB↔HEX conversions (pure functions)
│   │
│   └── layout/                     # NEW package — Phase 10
│       ├── AeroAccordion.kt        # Public
│       ├── AeroSplitPane.kt        # Public
│       ├── AeroSidebar.kt          # Public (persistent, NOT overlay — different from AeroDrawer)
│       ├── AeroStepperWizard.kt    # Public
│       └── internal/
│           ├── AeroDragSplitter.kt # Internal — draggable divider shared by SplitPane + (future TableColumn resize shares a different pattern)
│           └── AeroStepIndicator.kt # Internal — step dot/line row shared by StepperWizard
```

```
showcase/src/main/kotlin/com/mordred/showcase/
├── ShowcaseApp.kt                  # Extend: add DataSection, PickersSection, LayoutSection calls
└── sections/
    ├── (existing 12 sections)      # UNCHANGED
    ├── RangeSection.kt             # EXTEND: add AeroRangeSlider demo row
    ├── DataSection.kt              # NEW — AeroDataTable + AeroTreeView demos
    ├── PickersSection.kt           # NEW — DatePicker, TimePicker, DateTimePicker,
    │                               #        DateRangePicker, ColorPicker demos
    └── LayoutSection.kt            # NEW — Accordion, SplitPane, Sidebar, StepperWizard demos
```

### Structure Rationale

- **`components/datatable/`:** DataTable and TreeView both deal with hierarchical/tabular data and share a conceptual "row" rendering model. They are heavier than atomic components but do not overlap with picker or layout concerns. Separate package from `list/` because they introduce new infrastructure (LazyColumn virtualization, selection state).
- **`components/pickers/`:** All five picker components share calendar and time-selection primitives. The `internal/` sub-package isolates shared helpers that must not become public API.
- **`components/layout/`:** Accordion, SplitPane, Sidebar, and StepperWizard are structural layout components — they arrange other components, not data or selections. Grouping them together mirrors how `containers/` and `navigation/` were grouped in v1.0.
- **`components/range/AeroRangeSlider.kt`:** Placed in existing `range/` package (not a new package) because it is a direct extension of `AeroSlider`. The package already contains the slider primitive — a dual-thumb variant belongs there, not in a new group.

---

## Architectural Patterns

### Pattern 1: Existing Popup Infrastructure for Pickers

**What:** All picker popups (`AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker`, `AeroColorPicker`) open via `Popup()` anchored by `AeroPopupPositionProvider(side = AeroPopupSide.Bottom)`. This is identical to how `AeroDropdown`, `AeroComboBox`, `AeroTooltip`, `AeroPopover`, and `AeroContextMenu` work.

**When to use:** Any picker that opens from a trigger button/field needs a positioned popup. Do NOT use `Dialog` for pickers — they should dismiss on outside click exactly like `AeroDropdown`.

**Confidence:** HIGH — `AeroPopupPositionProvider` is already public API with auto-flip. The existing `AeroDropdown` and `AeroComboBox` both use `Popup(popupPositionProvider = remember { AeroPopupPositionProvider() }, properties = PopupProperties(focusable = true, dismissOnClickOutside = true))`. Pickers follow the same pattern verbatim.

**Trade-offs:** The auto-flip logic handles screen-edge cases. `AeroDateRangePicker` with a double calendar will be wide (~480dp) — the position provider clamps to window bounds, but on very small windows it may overlap itself. This is acceptable given the "desktop-first" constraint.

```kotlin
// DatePicker popup — same pattern as AeroDropdown
Popup(
    popupPositionProvider = remember { AeroPopupPositionProvider(side = AeroPopupSide.Bottom, gap = 4) },
    onDismissRequest = { expanded = false },
    properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
) {
    Box(Modifier.width(280.dp).glassPanel(cornerRadius = 8.dp).border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))) {
        AeroCalendarGrid(/* ... */)
    }
}
```

### Pattern 2: Internal Shared Calendar Grid

**What:** `AeroCalendarGrid` is an `internal` composable that renders a month grid (7-column day grid with prev/next navigation). It is used directly by `AeroDatePicker` (single selection mode) and `AeroDateRangePicker` (range highlight mode). The mode difference is expressed via a parameter, not a type hierarchy.

**When to use:** Any component that needs to display a calendar month grid. Do NOT copy-paste the grid into both DatePicker and DateRangePicker — the two components differ only in how they handle selection state, not in how the grid is drawn.

**Signature sketch:**
```kotlin
// Internal — not part of public API
@Composable
internal fun AeroCalendarGrid(
    displayMonth: YearMonth,          // java.time.YearMonth (JDK 17 — already in classpath)
    selectedDates: Set<LocalDate>,    // empty, 1-item (DatePicker), or multi-item (range)
    rangeStart: LocalDate?,           // non-null when in range mode
    rangeEnd: LocalDate?,
    onDateClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Why `java.time`:** JDK 17 is already the target. `java.time.LocalDate` / `YearMonth` are available without additional dependencies. Do NOT add `kotlinx-datetime` — unnecessary dependency for a library that already requires JDK 17.

### Pattern 3: AeroRangeSlider as Material3 RangeSlider Wrapper

**What:** Compose Desktop 1.7.3 includes Material3's `RangeSlider` composable (`androidx.compose.material3.RangeSlider`). `AeroRangeSlider` wraps it exactly as `AeroSlider` wraps `Slider` — applying the same `SliderDefaults.colors()` with Aero tokens, and optionally showing a dragging tooltip.

**When to use:** This is the correct pattern. Do NOT implement dual-thumb drag logic from scratch in Canvas — Material3 already provides `RangeSlider` with correct accessibility semantics.

**Confidence:** MEDIUM — Material3 `RangeSlider` API is available in CMP 1.7.x. The `AeroSlider` wrapper pattern (lines 51–86 in `AeroSlider.kt`) maps directly. Risk: tooltip positioning for two thumbs is non-trivial; v2.0 can omit per-thumb tooltips and show a range label centered above the track (same as the existing `TODO(Phase 3)` in `AeroSlider`).

```kotlin
@Composable
public fun AeroRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
)
```

### Pattern 4: AeroDataTable — LazyColumn + Horizontal Scroll

**What:** `AeroDataTable` renders a fixed header row above a `LazyColumn` of data rows. Horizontal scrolling is shared between header and body via a `ScrollState` (not `LazyRow` — the header must be pinned). Column widths are held in a `remember { mutableStateListOf<Dp>() }` so drag-resize updates all rows simultaneously via recomposition.

**When to use:** This is the only viable approach for virtualized + horizontally-scrollable + sortable tables in Compose Desktop 1.7.x. `LazyHorizontalGrid` is wrong here — rows are independent units, not grid cells.

**Confidence:** MEDIUM — pattern inferred from Compose Desktop table community examples; no official blessed template exists. The specific risk (see Integration Risks below) is the `fillMaxWidth()` interaction inside a parent `verticalScroll` Column in the showcase.

### Pattern 5: AeroAccordion Internal State

**What:** Accordion state is held inside the component when `mode = AccordionMode.Single`: a single `var openIndex by remember { mutableStateOf(-1) }`. For `mode = AccordionMode.Multi`: a `remember { mutableStateSetOf<Int>() }`. The public API exposes `AeroAccordionItem(title, content)` data class. Caller does not manage state — the component is self-contained, similar to `AeroTabBar`.

**When to use:** Use for collapsible sections. Do NOT expose a state hoisting variant in v2.0 — keep it simple. State hoisting can be added in v2.x if a consumer requests it.

### Pattern 6: AeroSidebar — Fixed Widths, AnimatedContent

**What:** `AeroSidebar` is a `Column` with three modes: `Expanded` (icon+label, ~200dp), `Collapsed` (icon-only, ~52dp), `Hidden` (0dp, offscreen). Width transitions use `animateDpAsState`. `AeroSidebar` is NOT a popup — it is placed in a `Row` alongside the main content area by the caller. This is the fundamental difference from `AeroDrawer` (which is a full-window popup overlay).

```
// Caller layout pattern — NOT implemented inside AeroSidebar:
Row(Modifier.fillMaxSize()) {
    AeroSidebar(mode = sidebarMode, items = navItems, onItemClick = { ... })
    // main content
    Box(Modifier.weight(1f)) { /* page content */ }
}
```

**Confidence:** HIGH — this matches the locked decision "persistent bokovaya navigatsiya" vs "AeroDrawer overlay mechanic" distinction established in STATE.md.

---

## Data Flow

### Picker State Flow

```
Trigger Button (click)
    ↓
var expanded by remember { mutableStateOf(false) }
    ↓
Popup(AeroPopupPositionProvider) shown when expanded == true
    ↓
AeroCalendarGrid / AeroHsvColorSquare / time drum
    ↓ (user selection)
onDateSelect(LocalDate) / onColorChange(Color)
    ↓
Caller's state updated; expanded = false (optional auto-close)
```

### DataTable Sort/Selection Flow

```
AeroDataTable(columns, rows, onRowClick, onSort)
    ↓
Header cell click → sortColumn / sortAscending state flip → caller's onSort callback
    ↓ (caller re-sorts data, passes new rows list)
LazyColumn re-renders sorted rows
    ↓
Row click → selectedRows state update (Set<Int>) → onRowClick callback
```

### AeroSplitPane Drag Flow

```
AeroSplitPane(orientation, first, second)
    ↓
Internal var splitFraction by remember { mutableStateOf(0.5f) }
    ↓
AeroDragSplitter (internal) — pointerInput drag → splitFraction update
    ↓
Layout: first composable at weight(splitFraction), second at weight(1f - splitFraction)
```

---

## Build Order (Explicit Dependencies)

Phase numbers are proposed (7–10). Each phase is a prerequisite for the next.

### Phase 7: Shared Primitives (No New Public Components)

**Rationale:** Extract internal helpers before the components that depend on them. This phase has zero user-visible output but enables Phases 8–10 to be implemented cleanly without copy-paste.

**Deliverables:**
1. `components/pickers/internal/AeroCalendarGrid.kt` — shared by DatePicker (Phase 8) and DateRangePicker (Phase 8)
2. `components/pickers/internal/AeroColorMath.kt` — pure functions: `hsvToRgb()`, `rgbToHsv()`, `hexToColor()`, `colorToHex()`; correctness-tested; no UI
3. `components/pickers/internal/AeroHsvColorSquare.kt` — Canvas-based HSV 2D gradient square; pointer drag → HSV float pair
4. `components/pickers/internal/AeroHueSlider.kt` — Canvas-based horizontal hue gradient strip (0..360); pointer drag → hue float
5. `components/layout/internal/AeroDragSplitter.kt` — thin draggable divider strip; `pointerInput` pointer tracking; emits `Float` delta; shared by SplitPane
6. `components/layout/internal/AeroStepIndicator.kt` — row of step dots/lines with active/complete/pending states; used by StepperWizard

**Dependencies:** None (pure internal, no public API dependency)

### Phase 8: Pickers + RangeSlider

**Rationale:** All five picker components plus RangeSlider. Pickers depend on Phase 7 primitives. RangeSlider depends on nothing new (Material3 RangeSlider already in classpath). Can be split into sub-plans:

**Order within phase (hard dependencies):**
1. `AeroRangeSlider` — first, trivial; no Phase 7 dependency
2. `AeroDatePicker` — requires `AeroCalendarGrid` from Phase 7
3. `AeroTimePicker` — independent of calendar; requires only AeroTheme + AeroTextField-style input
4. `AeroDateRangePicker` — requires `AeroCalendarGrid` from Phase 7; does NOT depend on AeroDatePicker (shares grid primitive directly)
5. `AeroDateTimePicker` — requires `AeroDatePicker` AND `AeroTimePicker` (composes them); must come last in phase
6. `AeroColorPicker` — requires `AeroHsvColorSquare`, `AeroHueSlider`, `AeroColorMath` from Phase 7; also reuses `AeroSlider` (existing) for RGB sliders and `AeroTextField` (existing) for HEX input

**Dependency graph:**
```
Phase 7 primitives
    ├──→ AeroDatePicker
    ├──→ AeroDateRangePicker (parallel to DatePicker)
    ├──→ AeroColorPicker
    │       └── reuses: AeroSlider (v1.0), AeroTextField (v1.0)
    └──→ AeroTimePicker (independent of calendar)
             └──→ AeroDateTimePicker (requires both DatePicker + TimePicker done)

AeroRangeSlider (independent — no Phase 7 dependency, runs first)
```

### Phase 9: Data Components (DataTable + TreeView)

**Rationale:** DataTable and TreeView are the most complex implementations in v2.0. They come after pickers so the risk surface is distributed. They have no dependency on Phase 8 pickers, but placing them after reduces parallel complexity risk.

**Order within phase:**
1. `AeroTreeView` — simpler than DataTable; only `LazyColumn` + recursion + expand state; no column layout
2. `AeroDataTable` — most complex; LazyColumn + horizontal scroll + column width state + sort + selection

**Dependencies:** `AeroScrollArea`/`AeroScrollBar` (existing v1.0), `AeroIcons` (existing v1.1 — sort indicators)

### Phase 10: Layout Components

**Rationale:** Accordion, SplitPane, Sidebar, and StepperWizard are the lowest-risk group — they are primarily structural and depend on existing primitives. StepperWizard requires `AeroStepIndicator` from Phase 7.

**Order within phase:** All four are parallelizable within a plan (no inter-dependencies among them). Suggested plan split: Accordion + SplitPane in one plan (drag splitter shared), Sidebar + StepperWizard in another.

**Dependencies:**
- `AeroAccordion` → `animateFloatAsState` (stdlib), `AeroTheme`, `glassSurface` — no new dependencies
- `AeroSplitPane` → `AeroDragSplitter` from Phase 7
- `AeroSidebar` → `animateDpAsState`, `AeroIcons`, `AeroTooltip` (existing OVL-03) for collapsed mode tooltips
- `AeroStepperWizard` → `AeroStepIndicator` from Phase 7, `AeroButton` (existing)

---

## Integration Points

### Reuse of Existing Primitives

| Existing Primitive | Reused By | How |
|-------------------|-----------|-----|
| `AeroPopupPositionProvider` | DatePicker, TimePicker, DateTimePicker, DateRangePicker, ColorPicker | Identical popup pattern as AeroDropdown — `Popup(popupPositionProvider = remember { AeroPopupPositionProvider() })` |
| `AeroScrollArea` / `AeroScrollBar` | DataTable, TreeView | TreeView: `AeroScrollArea` for vertical overflow. DataTable: NOT `AeroScrollArea` (see Integration Risks) — needs custom `Box` + horizontal + vertical scroll coordination |
| `AeroSlider` (colors pattern) | AeroRangeSlider, AeroColorPicker (RGB channel sliders) | AeroRangeSlider wraps Material3 RangeSlider using same `SliderDefaults.colors()` call. ColorPicker reuses `AeroSlider` directly for R, G, B channel sliders |
| `AeroSearchField` | AeroColorPicker (HEX text input — not search, but same field style) | Use `AeroTextField` not `AeroSearchField` — HEX input has no clear/search semantics |
| `AeroTextField` | AeroColorPicker (HEX input), AeroTimePicker (hour/minute fields) | Direct reuse — existing component |
| `glassEffect` / `glassPanel` / `glassSurface` | All new components | All popup panels use `glassPanel(cornerRadius=8.dp)` + `border(glassBorder)`. DataTable header row: `glassPanel(cornerRadius=0.dp)`. Accordion panel: `glassSurface(cornerRadius=8.dp)` |
| `AeroTooltip` | AeroSidebar (collapsed mode) | When sidebar is collapsed, icon-only items need a hover tooltip — delegate to existing `AeroTooltip` composable |
| `AeroButton` | AeroDatePicker (month nav prev/next as AeroIconButton), AeroStepperWizard (Next/Back/Finish buttons) | Direct reuse |
| `AeroIcons` | All new components | Sort ascending: `AeroIcons.CaretUp`, sort descending: `AeroIcons.CaretDown`, expand: `AeroIcons.CaretRight` / `AeroIcons.CaretDown`, picker calendar icon: `AeroIcons.CalendarBlank`, color swatch: `AeroIcons.Palette`, check (stepper completed): `AeroIcons.Check` |

### New Internal Helpers Required

| Helper | Package | Purpose | Used By |
|--------|---------|---------|---------|
| `AeroCalendarGrid` | `pickers/internal/` | Month grid composable — 7-col day layout, prev/next month nav, selection highlighting | AeroDatePicker, AeroDateRangePicker |
| `AeroHsvColorSquare` | `pickers/internal/` | Canvas-drawn 2D HSV gradient (saturation × value), drag-to-pick | AeroColorPicker |
| `AeroHueSlider` | `pickers/internal/` | Canvas-drawn horizontal hue gradient strip (360°), drag-to-pick | AeroColorPicker |
| `AeroColorMath` | `pickers/internal/` | Pure functions: `hsvToRgb`, `rgbToHsv`, `hexToColor`, `colorToHex`; round-trip drift prevention | AeroColorPicker |
| `AeroDragSplitter` | `layout/internal/` | Draggable divider strip using `pointerInput`; emits `onDrag(delta: Float)` | AeroSplitPane, (also usable by DataTable column resize — different call site) |
| `AeroStepIndicator` | `layout/internal/` | Row of step dots connected by lines, 3 states: pending/active/complete | AeroStepperWizard |
| `AeroTableHeader` | `datatable/internal/` | Sortable header row; column width list; sort chevrons via AeroIcons | AeroDataTable |
| `AeroTableRow` | `datatable/internal/` | Single data row; selection highlight via `colors.borderSelected` background | AeroDataTable |
| `AeroTreeNode` | `datatable/internal/` | Single tree node with indent, expand chevron, optional icon | AeroTreeView |

### Showcase Wiring

**ShowcaseApp.kt changes (minimal):**
```kotlin
// Add three new section calls at the end of the Column, before the final Spacer:
DataSection()          // NEW — Phase 9
PickersSection()       // NEW — Phase 8
LayoutSection()        // NEW — Phase 10
```

**RangeSection.kt** — extend (not new file): Add a `RangeRow("AeroRangeSlider") { ... }` demo row. This is ~5 lines. The existing file pattern (RangeRow helper) is already set up for this.

**New Section files:**

| File | New/Extend | Phase | Content |
|------|-----------|-------|---------|
| `DataSection.kt` | NEW | 9 | AeroDataTable demo (5-col, 20 rows, sort + selection demo), AeroTreeView demo (3-level hierarchy) |
| `PickersSection.kt` | NEW | 8 | AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker — each with a trigger button + value display |
| `LayoutSection.kt` | NEW | 10 | AeroAccordion (3 sections, single mode), AeroSplitPane (horizontal + vertical examples), AeroSidebar (mode toggle demo), AeroStepperWizard (3-step linear flow) |
| `RangeSection.kt` | EXTEND | 8 | Add AeroRangeSlider row to existing RangeSection |

---

## Integration Risks

### Risk 1: DataTable Virtualization vs. ShowcaseApp `verticalScroll` Column — CRITICAL

**Problem:** `ShowcaseApp` wraps its entire content in `Column(Modifier.verticalScroll(...))`. A `LazyColumn` inside a `verticalScroll` parent is a known Compose crash: `LazyColumn` cannot measure its height when placed inside an already-scrollable container. Phase 6 notes already document this pitfall for `LazyVerticalGrid` (the `IconsSection` fix required `Modifier.height(400.dp)` to give the grid a bounded height).

**The same issue applies to DataTable's internal `LazyColumn`.**

**Mitigation:** `AeroDataTable` must accept a `Modifier` with an explicit `heightIn(max = N.dp)` from the caller, and its internal `LazyColumn` must use `Modifier.fillMaxWidth()` (not `fillMaxSize()`). In `DataSection.kt` the showcase must call `AeroDataTable(modifier = Modifier.height(300.dp), ...)`. This is the same pattern as `IconsSection` — document it in KDoc and the PITFALLS research file.

**Alternative:** `AeroDataTable` could use a non-lazy `Column` for small datasets, but that defeats the virtualization requirement. Stay with LazyColumn + bounded height.

### Risk 2: AeroScrollArea Not Usable Directly in DataTable

**Problem:** `AeroScrollArea` uses `Column.verticalScroll(state)` internally (line 33 in `AeroScrollArea.kt`), which means it is also a `Column`-based scroller. DataTable needs a `LazyColumn` for row virtualization AND synchronized horizontal scroll between the header row and the data area. This requires a custom layout, not `AeroScrollArea`.

**Mitigation:** DataTable does NOT use `AeroScrollArea`. It uses a raw `Box` with:
- A horizontally-scrolled header `Row` sharing a `ScrollState` with the body
- A `LazyColumn` for the body, where each row is also horizontally scrolled with the shared `ScrollState`
- `AeroScrollBar` (the existing component) attached to the vertical `LazyListState`

**AeroScrollArea is appropriate for TreeView** (TreeView rows are variable-height composables in a Column — not virtualized, or can be lazily composed with a LazyColumn that has bounded height).

### Risk 3: AeroDateRangePicker Width on Small Windows

**Problem:** A dual-calendar layout for `AeroDateRangePicker` will need ~480–520dp width (two 240dp month calendars side by side). On small windows, `AeroPopupPositionProvider.clamp()` will prevent horizontal overflow, but the popup may not fully display if the window itself is narrower than 480dp.

**Mitigation:** Design the range picker to stack calendars vertically on small widths (responsive within the popup via `BoxWithConstraints`), or document a minimum window width of 600dp for `AeroDateRangePicker` use. The vertical stack is the preferred mitigation.

### Risk 4: AeroColorPicker HSV↔RGB Round-Trip Drift

**Problem:** Float-based HSV→RGB→HSV conversions accumulate rounding error. If the user drags the HSV square and the result is fed back into the square's displayed position via HSV→RGB→HSV, the hue/saturation position drifts visually.

**Mitigation:** `AeroColorMath` must hold the authoritative HSV triple in state and only convert to RGB for display/callback purposes — never convert back from RGB to HSV as a round-trip. The state shape is `hsv: Triple<Float, Float, Float>` not `color: Color`. RGB sliders modify hsv by converting the RGB component back through the non-drifting path. This is specified in the `AeroColorMath` design; the phase plan must enforce it.

### Risk 5: AeroSidebar Width Animation vs. Compose Layout

**Problem:** `animateDpAsState` on the sidebar width triggers re-layouts on every animation frame. In a `Row(Modifier.fillMaxSize())` where the sidebar and content area share the full width, each frame shift causes the content area to recompose. This is acceptable for a smooth transition (the animation is short, ~200ms) but must use `wrapContentWidth(unbounded = false)` correctly to avoid content flashing.

**Mitigation:** Use `animateFloatAsState` on a `weight()` fraction rather than animating absolute `Dp` — weight-based layout avoids absolute-size re-measure on every frame. Alternatively, accept the Dp animation and ensure the content area uses `Modifier.weight(1f)` so Compose handles the resize efficiently. This is the same as AeroDrawer (which uses `offsetFraction` animation).

### Risk 6: AeroTreeView Asynchronous `onExpand` Callback

**Problem:** The spec says "lazy children via `onExpand` callback." If `onExpand` is asynchronous (caller loads children from disk/network and updates the model), there's a race condition: the user clicks expand, the callback fires, the node is immediately marked expanded (spinner shown), then children arrive. If the callback is synchronous, this is simpler but blocks UI thread.

**Mitigation:** Define `onExpand` as synchronous with a loading-state return: `onExpand: (nodeId: Any) -> Unit`. The callback is responsible for updating the tree model passed to AeroTreeView (the component is stateless with respect to children — it just renders `nodes: List<AeroTreeNode>`). This means the caller manages async loading and updates `nodes` via its own state — the component never awaits. Document this clearly in KDoc.

### Risk 7: AeroDataTable Column Resize + LazyColumn Interaction

**Problem:** When the user drag-resizes a column, all visible LazyColumn rows must update their cell widths synchronaneously. If column widths are held in `SnapshotStateList<Dp>`, each drag delta triggers `SnapshotStateList.set()` which causes the entire LazyColumn to recompose (not just the visible rows). At high drag speeds (many events/second), this may cause jank.

**Mitigation:** Use `derivedStateOf` for the width snapshot and debounce or throttle the drag update to once per frame via `pointerInput` with `awaitDragOrCancellation()` at the Compose coroutine scope. The `AeroDragSplitter` internal component must be designed with this in mind from the start. If jank is observed during visual checkpoint, fall back to `animateFloatAsState` snap (no smooth drag).

---

## Anti-Patterns

### Anti-Pattern 1: New Gradle Module for Pickers or DataTable

**What people do:** Create `:datepickers` or `:datatable` Gradle modules to isolate new components.
**Why it's wrong:** Locked decision — all components stay in `:library`. Cross-module `internal` is not enforced. Creating a module would require consumers to add separate dependencies and breaks the "one dependency" core value.
**Do this instead:** Use `internal` visibility modifier + `components/{group}/internal/` subpackage convention within `:library`. The Gradle module boundary stays where it is.

### Anti-Pattern 2: Using AeroScrollArea Inside DataTable

**What people do:** Wrap DataTable rows in `AeroScrollArea` for "free" scrollbar.
**Why it's wrong:** `AeroScrollArea` is a `Column.verticalScroll()` container — it cannot contain `LazyColumn`. DataTable needs virtualization and synchronized horizontal scroll that `AeroScrollArea` does not support.
**Do this instead:** Build DataTable with its own `LazyColumn(state = lazyListState)` + `AeroScrollBar(scrollState = lazyListState)` directly.

### Anti-Pattern 3: AeroDialog for Pickers

**What people do:** Open date/color pickers inside `AeroDialog` for a "guaranteed visible" experience.
**Why it's wrong:** Dialog is a modal overlay requiring an explicit dismiss action — picker UX convention is dismiss-on-outside-click, like a dropdown. Dialog also does not anchor to the trigger element.
**Do this instead:** Use `Popup(AeroPopupPositionProvider, PopupProperties(dismissOnClickOutside=true))` — the same popup infrastructure as `AeroDropdown`.

### Anti-Pattern 4: java.util.Calendar or Custom Date Math

**What people do:** Implement date navigation (prev/next month, leap year, day-of-week offsets) by hand or using `java.util.Calendar`.
**Why it's wrong:** `java.util.Calendar` is mutable, thread-unsafe, and error-prone. Custom date math for month boundaries is routinely wrong on edge cases (Feb 28/29, year boundaries).
**Do this instead:** Use `java.time.LocalDate` / `java.time.YearMonth` from JDK 17 stdlib. Already in classpath. `YearMonth.atDay(1).dayOfWeek` gives the first day offset; `YearMonth.lengthOfMonth()` gives the correct number of days including leap year.

### Anti-Pattern 5: Eager HSV Color State Converted to Color Early

**What people do:** Store the current color as `var color by remember { mutableStateOf(Color.Red) }` and re-derive HSV position from it on each recompose.
**Why it's wrong:** `Color` → HSV conversion on every recompose is wasteful, and round-trip drift causes the hue/saturation cursor to visually wander.
**Do this instead:** Store `var hsv by remember { mutableStateOf(Triple(0f, 1f, 1f)) }` as the source of truth. Derive `Color` from it once for the callback and display. RGB sliders independently modify their own component of HSV without converting back through `Color`.

---

## Scaling Considerations

This is a library, not a service — "scaling" means component count and consumer adoption complexity, not users.

| Scale | Approach |
|-------|----------|
| Current (v2.0 — 12 new components) | Single `:library` module, single JAR. All components in `com.mordred.aero.components.{group}` — no structural change to Gradle. |
| Future (30+ components, icon-only consumers) | Extract `com.mordred:aero-icons` as separate Gradle subproject; `com.mordred:aero-compose-ui-core` (atomic) + `com.mordred:aero-compose-ui-complex` (datatable/pickers). Not for v2.0. |
| JAR size concern | v2.0 adds ~12 composable files + ~6 internal helpers. Est. JAR growth: < 0.05 MB (all Kotlin bytecode; no bitmaps). Not a concern. |

---

## Sources

- `library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt` — confirmed public API shape, auto-flip behavior, gap parameter (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt` — confirmed `SliderDefaults.colors()` pattern for `AeroRangeSlider` extension (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollArea.kt` — confirmed `Column.verticalScroll()` implementation; established why DataTable cannot reuse it (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt` — confirmed `Popup(AeroPopupPositionProvider)` pattern all pickers should follow (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroDrawer.kt` — confirmed animation + offset pattern for AeroSidebar width transitions (HIGH confidence)
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — confirmed `Column.verticalScroll` root structure; established DataTable bounded-height requirement (HIGH confidence)
- `.planning/milestones/v1.1-ROADMAP.md` Phase 6 notes — `LazyVerticalGrid` bounded-height pitfall documented; same constraint applies to DataTable LazyColumn (HIGH confidence)
- `.planning/PROJECT.md` — v2.0 locked decisions, out-of-scope items, constraints (HIGH confidence)
- `.planning/STATE.md` — confirmed AeroSidebar vs AeroDrawer mechanic distinction, DataTable risks (HIGH confidence)

---

*Architecture research for: aero-compose-ui v2.0 Stateful + Layout integration*
*Researched: 2026-04-30*
