# Feature Research — aero-compose-ui v2.0 Stateful + Layout

**Domain:** Desktop UI component library — complex stateful + advanced layout components
**Researched:** 2026-04-30
**Confidence:** HIGH (cross-verified across WPF, JavaFX, Qt, Flutter, MUI X, AG Grid, NN/g UX research)

---

## Shared Primitives (DRY across multiple components)

Before per-component breakdowns, the following reusable building blocks appear in two or more components.
Build these once; do not duplicate.

| Primitive | Used By | Notes |
|-----------|---------|-------|
| **Calendar grid** (month grid, prev/next nav, day cells) | AeroDatePicker, AeroDateRangePicker, AeroDateTimePicker | Single source — DateRangePicker uses two instances |
| **Popup anchor + dismiss logic** (open/close on trigger click, Escape closes, click-outside closes) | AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker | Reuse existing AeroPopover dismiss contract |
| **Single-thumb slider track + thumb** | AeroRangeSlider (two thumbs), AeroColorPicker (hue strip + alpha strip) | AeroSlider already exists — compose/extend it |
| **Scroll area + scrollbar** | AeroDataTable (both axes), AeroTreeView (vertical), AeroSidebar (overflow items) | AeroScrollArea + AeroScrollBar already exist in library |
| **Text field with validation** | AeroColorPicker (HEX input, RGB inputs) | AeroTextField already exists in library |
| **Expand/collapse animated section** | AeroAccordion (sections), AeroTreeView (node children), AeroSidebar (mode transition) | Share animation curve; AnimatedVisibility or explicit tween |
| **Divider line** | AeroDataTable (column resizer), AeroSplitPane (pane divider), AeroAccordion (section borders) | AeroDivider already exists — extend with drag affordance for resizable variants |

---

## 1. AeroDataTable

**Complexity: LARGE**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Sticky column headers (do not scroll vertically) | Users must always know which column they are reading | Headers in a separate non-scrolling composable above LazyColumn content |
| Click header to sort (asc → desc → none cycle) | Universal in every grid from Excel to web data tables | Visual sort indicator (caret up/down) in header; only one column sorted at a time in v2.0 |
| Sort direction indicator | Users cannot understand which column is active sort without it | Show directional AeroIcon in header cell; dim/hide on unsorted columns |
| Single row selection (click to select, click again or click empty = deselect) | Minimum for any read-only table used in desktop apps | Highlight selected row with theme selection color |
| Multi-row selection via Ctrl+click and Shift+click | Expected in any desktop table — WPF DataGrid, JavaFX TableView, Qt QTreeView all support this | Ctrl+click toggles individual rows; Shift+click selects a contiguous range |
| Virtualized row rendering (LazyColumn) | Required for tables with 100+ rows — non-virtualized table hangs or janks | LazyColumn is the correct primitive; all rows are same height (fixed-height rows simplify measurement) |
| Horizontal scroll when columns exceed viewport width | Column-rich tables are common in desktop apps | Shared horizontal ScrollState across header row and data rows — critical correctness requirement |
| Resizable columns (drag splitter between header cells) | Expected in professional desktop grids (WPF, Qt, Swing) | Drag handle overlaid on header divider; columns have min width constraint (e.g., 40dp) |
| Empty state slot | Table with no data must show something; blank rectangle is confusing | Caller-provided `emptyContent: @Composable () -> Unit` slot |
| Loading state slot | Data may arrive asynchronously; spinner placeholder expected | Caller-provided `loadingContent` slot or simple progress bar |

### Differentiators (for this library context)

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Aero-styled row selection highlight (glass tint) | Stays on-brand with Aero aesthetic | Semi-transparent selection layer using `glassSurface` modifier tint |
| Column min-width enforcement during resize | Prevents columns collapsing to 0 and hiding data — common footgun in naive implementations | Enforce minimum 40dp per column during drag |
| Stable sort (original order preserved within equal keys) | Professional apps expect stable sort | Use Kotlin's `sortedWith` which is stable |
| Caller controls data — no internal data model | Library does not own the data source; caller passes `List<T>` | Simpler API; caller handles filtering, fetching, pagination |

### Anti-Features (explicitly exclude)

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Cell editing / inline edit mode | Massively increases API surface, focus management complexity, and commit/cancel logic — out of scope in v2.0 per locked decisions | Read-only render; caller overlays an editor if needed |
| Column reordering (drag to rearrange) | Drag-column-reorder + drag-column-resize simultaneously is a major UX and hit-test complexity trap | Resize only in v2.0 |
| Built-in filtering UI (filter row or column filter popup) | Filtering UX varies wildly per use case; baking it in forces choices the caller should make | Caller filters `List<T>` before passing; table is pure display |
| Multi-column sort (Shift+click header) | Sorting by multiple columns is confusing when there is no visual priority ranking; single-column sort covers 95% of cases | Single-column sort only |
| Frozen/pinned columns | Complex layout: frozen columns require separate composables with synchronized scroll state — double the complexity of resizing | Not in v2.0 |
| Pagination controls | Pagination is app-level logic; embedding it couples the table to an opinionated navigation pattern | Caller adds pagination above/below the table |
| Auto-resize columns on double-click header divider | Requires measuring all cell text widths — expensive and complex | Manual drag resize only |

### Dependencies on existing components

- `AeroScrollArea` + `AeroScrollBar` — both scroll axes
- `AeroIcons` — sort carets, column resize cursor hint
- `AeroDivider` — extended for drag-resizable column splitter
- Theme tokens — selection highlight color, header background

---

## 2. AeroTreeView

**Complexity: MEDIUM**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Expand/collapse nodes by clicking the toggle icon | Core interaction in every tree (Windows Explorer, VS Code file tree, JavaFX TreeView) | Toggle icon (caret/arrow) rotates on expand |
| Indentation per depth level | Required to show hierarchy visually | Fixed indent step (e.g., 16dp per level); configurable via parameter |
| Lazy children loading via `onExpand` callback | Expected for trees backed by async data sources (filesystem, network API) | `onExpand(node) -> Unit` fires when a node is expanded for the first time; caller calls back with children |
| Loading indicator per node | When `onExpand` is async, node must show that children are being fetched | Spinner or skeleton row under expanding node |
| Optional node icon slot | Most trees show file/folder icons or domain icons | `leadingIcon: @Composable (() -> Unit)?` per node |
| Node selection (single click selects) | Tree selection drives detail panel in master-detail layouts | Selected node highlighted; `onSelect(node)` callback |
| Collapse all descendants when parent collapses | Collapsing a parent should hide all its children recursively | State management: expanded-node set, not tree node state |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Error state per node | If `onExpand` throws or returns empty after a delay, show error inline | `onError` slot or error-state per node; prevents silent failures |
| Expandable node distinguishable from leaf | Users need to know which nodes have children before expanding | `hasChildren: Boolean` or `childCount: Int?` parameter; render toggle only when `hasChildren = true` |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Drag-and-drop node reordering | Requires hit-testing between nodes, scroll-while-drag, and complex tree model mutations — locked out in v2.0 | Locked: caller manages reordering externally |
| Checkbox multi-select with tri-state (partial parent) | Tri-state checkbox logic (all/some/none children selected) is significant complexity — WPF does it wrong in half its implementations | Single-node selection only in v2.0; multi-select deferred |
| Inline rename / edit node label | Same footgun as DataTable cell editing — out of scope | Read-only labels |
| Auto-expand to search result | Requires traversal + programmatic expand — add only if consumer demand surfaces | Not in v2.0 |

### Dependencies on existing components

- `AeroScrollArea` + `AeroScrollBar` — vertical scroll for deep trees
- `AeroIcons` — expand caret, loading spinner, optional node icons
- `AnimatedVisibility` (Compose) — animate expand/collapse of children
- Theme tokens — selected node highlight, indent line color (optional decorative line)

---

## 3. Date/Time Picker Group

**Shared Primitive:** Calendar grid — a month view with day cells, prev/next month buttons, and a month+year header. Used by AeroDatePicker, AeroDateRangePicker, and AeroDateTimePicker. Build once as an internal `CalendarGrid` composable.

**Shared Primitive:** Popup anchor — open on trigger click, Escape closes, click-outside closes. Reuse AeroPopover dismiss contract.

---

### 3a. AeroDatePicker

**Complexity: MEDIUM**

#### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Popup calendar opens on trigger click | Universal date picker pattern (WPF DatePicker, Qt QDateEdit, MUI DatePicker) | Popup anchored below trigger field |
| Month grid: 7-column day grid, days of week header | Standard calendar layout | Mon–Sun header row; 5–6 week rows per month |
| Prev/next month navigation (chevron buttons) | Required to navigate to target month | AeroIcons.CaretLeft / CaretRight |
| Today highlight | Orientation marker — users need to know where "now" is | Ring/border on today cell, not fill (fill = selected) |
| Selected day highlight | Shows currently picked date | Filled circle or rounded rect using theme accent |
| Click day to select and close popup | Primary interaction | `onDateSelected(LocalDate)` callback; popup closes on selection |
| Escape closes without selection | Standard popup dismiss | Return focus to trigger |
| Display selected date in trigger field | Feedback of current value | Formatted date string (e.g., "YYYY-MM-DD" or locale-aware) |
| Null/no-selection state | Date fields start empty before user picks | Trigger shows placeholder; `value: LocalDate?` |
| Year navigation (click year label → year picker or prev/next arrows) | Jumping years by clicking next-month 24 times is a known UX pain point | Year picker row or fast-scroll; simple prev/next year arrows are acceptable minimum |

#### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| `disabledDates: Set<LocalDate>` or `disableBefore`/`disableAfter` | Blocks invalid date selection at the picker level | Grayed-out day cells, not clickable |
| Aero-glass popup surface | On-brand with library aesthetic | Use `glassSurface` modifier on popup container |

#### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Inline (always-visible) calendar mode | Requires layout space reserved permanently; use case niche for desktop; locked out in v2.0 | Popup only |
| Free-text typed date in trigger field | Parsing arbitrary user input requires locale-aware parsing + validation state + error feedback — substantial complexity | Display-only trigger (or read-only AeroTextField); manual entry deferred |
| Week numbers in calendar | Adds visual clutter; niche use (project management tools only) | Out |
| Multiple calendars in single-date picker | Confusing for single date selection | Only for DateRangePicker |

#### Dependencies on existing components

- `AeroPopover` (or popup anchor logic) — positioning + dismiss
- `AeroIconButton` — prev/next month/year buttons
- `AeroIcons` — CaretLeft, CaretRight
- `AeroTextField` (display-only trigger) or custom trigger composable
- Theme tokens — accent for selected day, today ring color

---

### 3b. AeroTimePicker

**Complexity: SMALL**

#### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Hours + minutes selection | Minimum time precision for desktop apps | Two-column layout (HH : MM) |
| 24-hour mode by default | Desktop apps (especially non-US) use 24h; 12h is secondary | Parameter: `use24Hour: Boolean = true` |
| Spinner/scroll wheels OR +/- buttons per unit | Both are common in desktop apps (Qt QTimeEdit uses up/down spin; mobile uses drum rolls) | For desktop: up/down arrow buttons (+/-) on each column; simpler than drum-scroll |
| Display selected time in trigger field | Feedback | Formatted HH:MM string |
| Popup opens on trigger click, Escape/click-outside closes | Standard popup pattern | Same as DatePicker |
| Null/no-selection state | Time fields start empty | `value: LocalTime?` |

#### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Keyboard increment/decrement in popup (Up/Down arrows) | Desktop power users expect keyboard input | Arrow keys increment/decrement focused unit |
| 12h AM/PM toggle | Needed for en-US apps | `use24Hour = false` shows AM/PM toggle |

#### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Drum-roll (momentum) scroll for hours/minutes | Feels wrong on desktop with mouse; is a mobile pattern | Use +/- buttons; no inertia scroll |
| Analog clock face | Analog clock selection is hard to use with a mouse for precision; works on touch only | +/- button columns |
| Seconds field by default | Seconds are rarely needed in desktop business apps | `showSeconds: Boolean = false`; off by default |

#### Dependencies on existing components

- `AeroPopover` / popup anchor
- `AeroIconButton` — up/down increment buttons
- `AeroIcons` — CaretUp, CaretDown
- Theme tokens

---

### 3c. AeroDateTimePicker

**Complexity: MEDIUM** (composition, not novel implementation)

**Architecture note:** This is a composition of the DatePicker calendar grid and the TimePicker +/- controls in a single popup. Do NOT duplicate — import internal `CalendarGrid` and time-unit columns.

#### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| All table stakes of DatePicker | See 3a | Calendar grid in popup |
| All table stakes of TimePicker | See 3b | Time units in same popup, below or beside calendar |
| Date and time displayed in single trigger field | One field, one value (`LocalDateTime`) | "YYYY-MM-DD HH:MM" formatted |
| Confirm / Apply button | Combined date+time requires explicit commit (user may set date first, then time; shouldn't fire on every change) | "Apply" button in popup footer closes and emits `onDateTimeSelected(LocalDateTime)` |
| Cancel button | User should be able to open picker, browse, and dismiss without committing | Escape or Cancel button reverts to previous value |

#### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Auto-close on date selection (before time is set) | DateTimePicker must stay open until both date and time are set; auto-close fires onChange prematurely | Explicit Apply button; popup stays open until confirmed |
| Separate popup for date and separate for time | Requires two trigger fields and two popups — confusing | Single popup combining both |

---

### 3d. AeroDateRangePicker

**Complexity: LARGE**

#### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Dual calendar (two months side-by-side) | Industry standard for range picking (daterangepicker.com, MUI X DateRangePicker, Ant Design) | Left calendar = start month; right = next month by default |
| Click start date, then click end date | Two-click selection flow | First click sets `startDate`, second click sets `endDate` and closes; or use Apply button |
| Range highlight (all days between start and end) | Visual confirmation of the selected range | Fill or tint on cells between start and end; hover preview shows tentative range |
| Hover preview of range before second click | Expected in modern range pickers | As user hovers after first click, highlight tentative range up to hover position |
| Prev/next month navigation advances both calendars together | Otherwise dual calendars can end up showing same month | Left calendar next-month → both advance by one |
| Independent navigation possible (unlinked calendars option) | Power users may want to navigate left and right calendar independently | `linkedCalendars: Boolean = true` default; when false each navigates independently |
| Start date must be <= end date (constraint enforced) | Selecting end before start is a footgun | After first click, disable all dates before selected start on right calendar |
| Confirm / Cancel buttons | Range selection requires explicit commit | Apply + Cancel buttons in popup footer |
| Display range in trigger field(s) | Two fields (Start / End) or one combined field | Two separate `AeroTextField` triggers OR a single "Start → End" display field |
| Null start / null end states | Range starts with no selection | Both fields empty; display placeholder "YYYY-MM-DD" |

#### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| `disabledDates` and `disableBefore`/`disableAfter` | Essential for booking systems (no past dates, no blocked dates) | Same API as DatePicker; grayed-out cells |
| Predefined range shortcuts (Today, Last 7 Days, This Month) | Reduces clicks for common ranges | Caller-provided via `shortcuts: List<RangeShortcut>` slot — keeps library generic |

#### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Single calendar for range picking | Requires awkward "first click = start, second click in same month = end" with no visual separation of months | Dual calendar always |
| Auto-swap start/end if user picks end before start | Silently reordering breaks user mental model; user thinks they selected the wrong range | Block end date selection before start date; force start-first flow |
| Free text range entry in fields | Two-field text parsing with date validation and range coherence checking is complex | Display-only trigger fields |

#### Dependencies on existing components

- Calendar grid primitive (shared with DatePicker)
- `AeroPopover` / popup anchor
- `AeroTextField` (display-only triggers for start/end)
- `AeroButton` — Apply, Cancel
- `AeroIconButton` — prev/next month
- Theme tokens — range fill color, range edge accent

---

## 4. AeroColorPicker

**Complexity: LARGE**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| HSV saturation-value square (2D gradient field) | Standard color picker pattern (Windows Color Picker, Photoshop, Figma, Electron apps) | X axis = saturation 0→100%; Y axis = value 100→0% (white top-left, black bottom-right, pure hue top-right) |
| Hue strip (vertical or horizontal rainbow gradient) | Selects the base hue; HSV square updates to reflect chosen hue | Vertical strip recommended (more thumb precision); slider thumb on strip |
| RGB sliders (R, G, B each 0–255) | Users who work with RGB values must be able to enter them precisely | Three sliders with numeric readouts; any slider change updates HSV square and hex |
| HEX text input | Standard — every designer and developer uses hex codes | `AeroTextField` accepting "#RRGGBB"; update HSV + RGB on commit (Enter or Tab) |
| Color preview swatch | Users need to see the current selected color | A filled rectangle showing the current color; split old/new is a differentiator |
| Predefined swatches palette | Quick access to brand colors or common values | Caller-provided `swatches: List<Color>`; render as clickable squares |
| Bidirectional sync: any input updates all others | HSV ↔ RGB ↔ HEX must stay consistent on every change | Canonical internal representation is HSV float; convert to RGB/HEX for display |
| Optional alpha channel (opacity) | Some use cases (UI backgrounds, overlays) need alpha | `showAlpha: Boolean = false` by default; shows alpha strip + alpha hex digit |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Split preview (old color / new color) | Designer UX standard: compare new to original before confirming | Show previous value in left half of preview swatch |
| Aero glass container around the picker | On-brand | `glassSurface` on popup/panel container |
| HEX input accepts 3-char shorthand (#RGB → #RRGGBB) | Power-user convenience | Normalize on commit |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Eyedropper (screen color pick) | Requires platform-specific APIs (AWT Robot on JVM) that are unreliable and have permission issues on macOS/Linux; locked out in v2.0 | Out of scope; document as future v2.x |
| CMYK or LAB sliders | Niche color models add complexity without covering common desktop app use cases | RGB + HEX covers the target audience |
| Free-form HSV numeric inputs (editable) | HSV values are not intuitive to type; users use them for selection, not entry | HSV square and hue strip are always gesture-controlled; only RGB + HEX have text inputs |
| Opacity slider shown by default | Most components that embed a color picker don't need alpha (solid colors) | `showAlpha = false` default; caller opts in |
| HSV ↔ RGB round-trip drift | Floating-point HSV → RGB → HSV introduces drift on repeated edits (e.g., pure red drifts to R=254) | Maintain canonical HSV float internally; only convert for display, not for round-trips |

### Implementation Note: HSV ↔ RGB math

HSV → RGB: standard Hue-to-sector formula. RGB → HSV: `V = max(R,G,B)`, `S = (V - min) / V`, `H = sector formula`. Use `Float` precision throughout. Do not convert back from RGB to HSV when the user edits RGB sliders — instead update HSV directly from the RGB values to avoid drift. This is a known pitfall cited in STATE.md.

### Dependencies on existing components

- `AeroSlider` (compose/extend) — hue strip, R/G/B sliders, alpha strip
- `AeroTextField` — HEX input, RGB numeric inputs
- `AeroIcons` — copy hex button (optional)
- `glassSurface` modifier — popup container
- Theme tokens — border colors for swatch cells

---

## 5. AeroRangeSlider

**Complexity: SMALL** (composition over AeroSlider)

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Two thumbs on a shared track (min thumb + max thumb) | Core definition of a range slider; all frameworks (WPF RangeSlider, MUI Slider range, Chakra RangeSlider) have this | `value: ClosedFloatingPointRange<Float>`, `onValueChange(ClosedFloatingPointRange<Float>)` |
| Track segment between thumbs filled / highlighted | Visual indication of the selected range | Fill color between min and max thumb; unfilled on outer segments |
| Min thumb cannot cross max thumb (constraint enforced) | Universal constraint — min value must always be <= max value | Clamp: when dragging min thumb, cap at max thumb position minus `stepSize`; vice versa |
| Click track to move nearest thumb | When user clicks on the track, the closest thumb moves to that position | Standard behavior from WPF, Chakra, MUI |
| `valueRange` (min/max of the full slider) | Callers set the full range (e.g., 0f..100f, 0f..1000f) | Parameter `valueRange: ClosedFloatingPointRange<Float>` |
| `steps` parameter (discrete steps) | Same as AeroSlider — caller can lock to discrete values | Optional; 0 = continuous |
| Keyboard on each thumb (arrow keys when focused) | Desktop usability requirement | Tab to switch focus between thumbs; arrow keys adjust focused thumb |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Composition over AeroSlider internals | Code reuse — no duplication of track drawing, theme, or accessibility wiring | Extend AeroSlider or extract shared `SliderTrack` internal composable |
| Labels on min/max thumb showing current value | Useful in data filter scenarios | `showLabels: Boolean = false` optional tooltip above each thumb |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Thumbs allowed to cross (swap semantics) | Swapping min/max thumbs on cross is counterintuitive — some implementations do it silently and users are confused | Hard clamp: min never >= max; thumbs bounce off each other |
| Equal value disallowed (min == max) | Some ranges legitimately collapse to a point (e.g., filter with single exact value) | Allow min == max by default; caller can set `minGap` if they need separation |
| Independent track segments per thumb (not a unified track) | Looks wrong visually; users expect one track | Single track composable with two overlay thumbs |

### Dependencies on existing components

- `AeroSlider` — extend or share `SliderTrack` internal
- Theme tokens — thumb color, fill color, track color

---

## 6. AeroAccordion

**Complexity: SMALL**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Clickable header that expands/collapses section body | Core accordion behavior (Bootstrap Accordion, MUI Accordion, WinUI Expander) | Header row with label + expand icon; body revealed below |
| Expand icon rotates on state change | Visual feedback that the action succeeded | 90° or 180° rotation (CaretRight → CaretDown); animated |
| `mode = single` — only one section open at a time | FAQ use case; sidebar settings panels | Opening a new section collapses the currently open one |
| `mode = multi` — multiple sections open simultaneously | Dashboard use case; settings pages where sections are independent | No auto-collapse |
| Animated expand/collapse (height transition) | Jarring instant show/hide is considered broken in 2025 | AnimatedVisibility or height animation; share animation curve |
| Caller-provided section content (slot) | Accordion is a layout shell; content is arbitrary | `content: @Composable () -> Unit` per section |
| Caller-provided header content (slot or string) | Headers may include icons, badges, custom layouts | `header: @Composable () -> Unit` or `title: String` + optional `leadingIcon` |
| Disabled section state | Some sections may not be interactive based on app state | `enabled: Boolean = true` per section; grayed header, no click response |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| `initiallyExpanded` per section | Sections can start open | `defaultExpanded: Boolean = false` per section |
| Controlled vs uncontrolled state | Caller can manage expand state externally or let accordion manage internally | `expanded: Boolean?` + `onExpandedChange` for controlled; internal state for uncontrolled |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| "Expand all" / "Collapse all" buttons baked into the component | Tempting but adds opinionated UI chrome that clashes with caller layouts; NN/g notes it confuses screen readers | Expose state control API so caller can implement Expand All externally |
| Nested accordions with shared single-mode state | Single-mode across nested accordions is impossible to reason about (which level owns the state?) | Nested accordions each have independent state; document this explicitly |
| No animation (instant show/hide) | Feels broken in 2025 — every major framework animates accordion | Mandatory animation |

### Dependencies on existing components

- `AeroIcons` — CaretDown or ChevronDown for expand indicator
- `AeroDivider` — optional separator between sections
- `AnimatedVisibility` (Compose built-in)
- Theme tokens — header background, border color

---

## 7. AeroSplitPane

**Complexity: MEDIUM**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Two panes with a draggable divider | Core behavior — JSplitPane (Java Swing), WPF GridSplitter, Qt QSplitter | Divider position is the only mutable state |
| `orientation = horizontal` (left \| right panes) | Standard side-by-side layout | Left pane + divider + right pane |
| `orientation = vertical` (top \| bottom panes) | Standard top/bottom layout | Top pane + divider + bottom pane |
| Drag divider to resize panes | Primary interaction | Mouse press + drag on divider; divider moves, panes resize proportionally |
| Minimum size constraint per pane | Prevents panes collapsing to 0 — Java Swing uses component min-size; WPF uses `MinWidth` on columns | `minSize: Dp = 60.dp` per pane; divider snaps/bounces at min |
| Visual hover affordance on divider | Cursor changes to resize cursor; divider widens slightly on hover | Resize cursor on divider hit area; larger hit area than visual width |
| N-pane via nesting | Caller composes `AeroSplitPane` inside another `AeroSplitPane`'s pane slot | Public API stays 2-pane; nesting is caller responsibility |
| Controlled divider position | Caller can set initial divider position | `initialDividerFraction: Float = 0.5f` (0..1 range) |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Collapse pane via double-click divider | Power-user shortcut to minimize a pane; common in IDEs (IntelliJ, VS Code) | Optional `collapseOnDoubleClick: Boolean = false` |
| Divider position `onDividerChange` callback | Caller can persist divider position across sessions | `onDividerFractionChange: (Float) -> Unit` |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Divider with no visible handle when at rest | 1px divider line with no hover expansion is nearly impossible to hit with a mouse — documented WPF/Swing antipattern | Large invisible hit area (e.g., 8dp) centered on a 1dp visible line |
| Recursive/tree API for N-pane | Complex API surface and layout measurement problems | 2-pane public API + nesting is the correct abstraction |
| Fixed pixel size divider position (not fraction) | Fixed px breaks on window resize | Store as fraction (0..1); compute px at layout time |
| Pane collapses to 0 silently | Data in the pane becomes inaccessible — documented Swing antipattern | Enforce min-size; divider bounces at constraint |
| Per-pane drag-to-resize per-pane width (locked out) | Locked in v2.0 locked decisions — OUT | Fixed-width via fraction |

### Dependencies on existing components

- `AeroDivider` — extended with drag handle behavior
- Theme tokens — divider color, hover tint

---

## 8. AeroSidebar

**Complexity: MEDIUM**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| `expanded` mode: icon + label per item | Standard sidebar with text labels (VS Code sidebar expanded, Slack sidebar) | Width ~240dp; icon + label in each row |
| `collapsed` mode: icon only per item | Collapsed rail (VS Code activity bar, Android Navigation Rail) | Width ~56dp; icon centered |
| Tooltip on hover in collapsed mode | Users don't know what unlabeled icons mean — tooltip is mandatory for collapsed | Show item label as tooltip on icon hover; use existing AeroTooltip |
| `hidden` mode: sidebar not rendered / zero width | Full-screen focus mode; drawer replaces it when hidden | `mode = hidden` removes sidebar from layout entirely (not just visibility) |
| Mode toggle button (to switch expanded↔collapsed) | Standard UX — hamburger or chevron button at top or bottom of sidebar | Caller controls mode via `mode` parameter; library provides internal toggle button optionally |
| Item selection highlight | Active/selected nav item must be visually distinct | Selected item gets accent background or glass highlight |
| Scroll for overflow items | Sidebars with many items need vertical scroll | `AeroScrollArea` wrapping item list |
| Separator / section header support | Group related nav items (e.g., "Main" / "Settings") | `AeroSidebar.Separator` and `AeroSidebar.Header` child composables |
| Persistent (not overlay) mechanic | Sidebar pushes content to the right; does not overlay it — this is the key distinction from AeroDrawer | Content area shrinks/grows as sidebar mode changes |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| Animated mode transition (width animation) | Smooth expand/collapse instead of instant reflow | `animateContentSize()` or explicit width tween |
| Bottom-anchored items (e.g., Settings, Profile) | Common sidebar pattern (VS Code has gear icon at bottom) | `stickyBottom` slot composable |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Drag-to-resize sidebar width | Locked out in v2.0; fixed widths for expanded/collapsed modes | Parameter `expandedWidth: Dp = 240.dp` if caller needs different fixed width |
| Sidebar as overlay (covers content) | That is `AeroDrawer` — these are distinct components with different mechanics; conflating them creates confusion | Keep separate: AeroSidebar = persistent push; AeroDrawer = overlay slide |
| Items defined via data model (not composable slots) | Forces caller into library's data model; any icon or label variation requires workarounds | Caller passes `content: @Composable ColumnScope.() -> Unit` composable slot for items |
| Auto-collapse to hidden on small window width | Responsive behavior based on window width adds complexity and is unpredictable for library consumers | Caller controls mode; expose `mode` as a parameter driven by caller state |

### Dependencies on existing components

- `AeroTooltip` — labels in collapsed mode
- `AeroScrollArea` + `AeroScrollBar` — overflow item list
- `AeroIcons` — toggle icons (PanelLeft, PanelRight or similar)
- `AeroDivider` — section separators
- Theme tokens — selected item highlight, sidebar background

---

## 9. AeroStepperWizard

**Complexity: MEDIUM**

### Table Stakes

| Feature | Why Expected | Notes |
|---------|--------------|-------|
| Visual step indicator (step bar at top or side) | Users need progress orientation — "Step 2 of 5" or visual node strip | Horizontal node strip: circles with step numbers connected by lines |
| Step labels below/beside each node | Numbers alone are insufficient — users need to know what each step is | `title: String` per step |
| Current step highlight | Active step clearly distinguished from past/future | Accent color on active node; filled circle for completed, outlined for future |
| Completed step indicator | Users need to know which steps are done | Checkmark icon or filled node for completed steps |
| Next / Back navigation buttons | Primary navigation | "Next" (disabled if validation fails), "Back" (always enabled unless on step 1), "Finish" on last step |
| Per-step `onValidate: () -> Boolean` callback | Blocks "Next" if validation returns false | Caller returns false → Next button stays disabled or shows error; caller returns true → advance |
| Per-step arbitrary content slot | Wizard steps contain arbitrary forms or content | `content: @Composable () -> Unit` per step |
| Linear progression only | Steps 1→2→3→...→N; no skipping forward | Clicking a future step node does nothing or is disabled |
| Back navigation always allowed | Blocking back navigation is a documented major UX anti-pattern (users need to fix earlier step data) | Back always available; previous step's content preserved |

### Differentiators

| Feature | Value Proposition | Notes |
|---------|-------------------|-------|
| `onStepChange(from: Int, to: Int)` callback | Caller can react to any step transition | Useful for analytics, save-on-step-change |
| Step completion status exposed externally | Caller may want to show a summary panel based on step states | `stepStates: List<StepState>` observable |
| Async validation support | `onValidate` may need to call an API | `onValidate: suspend () -> Boolean`; show loading state on Next button during async check |

### Anti-Features

| Anti-Feature | Why It's a Footgun | What to Do Instead |
|-------------|--------------------|--------------------|
| Blocking Back navigation | Users constantly need to go back and fix earlier steps — blocking back is the #1 wizard UX complaint per NN/g research | Back is always enabled |
| Showing all validation errors only on the last step | Users cannot fix errors until the end, then must scroll back — cited as a top frustration in wizard UX research | Validate per-step at "Next" click; surface errors immediately |
| Branching (non-linear) steps | Locked out in v2.0; branching logic belongs in the application, not the component | Caller manages conditional content within a step instead of branching the step graph |
| Auto-advance to next step on completion | Surprising behavior that disorients users | Always require explicit "Next" button click |
| Step count visible but step purpose unclear | "Step 3 of 7" with no labels leaves users anxious | Require `title` per step; not optional |
| Destroying step content on navigation | If a step's form state is lost when user goes Back, they must re-enter everything — this is the v2.0 footgun to avoid | Preserve composable state across step navigation; do not re-compose from scratch on Back |

### Dependencies on existing components

- `AeroIcons` — Check (completed step), warning/error indicator
- `AeroDivider` — connecting lines between step nodes
- `AeroButton` — Next, Back, Finish
- Theme tokens — active step accent, completed step color, future step muted color

---

## Component Complexity Summary

| Component | Complexity | Primary Reason |
|-----------|------------|----------------|
| AeroDataTable | LARGE | Dual-axis scroll + virtualization + column resize + selection — Compose layout pitfalls (see STATE.md) |
| AeroTreeView | MEDIUM | Lazy load contract + recursive expand state + animated children |
| AeroDatePicker | MEDIUM | Calendar grid primitive + popup + date math |
| AeroTimePicker | SMALL | +/- spin columns; no novel layout |
| AeroDateTimePicker | MEDIUM | Composition of calendar + time; explicit Apply/Cancel required |
| AeroDateRangePicker | LARGE | Dual calendar + hover preview + range constraint + Apply/Cancel |
| AeroColorPicker | LARGE | HSV 2D drag + hue drag + bidirectional sync + multi-input layout + HSV↔RGB math correctness |
| AeroRangeSlider | SMALL | Composition over AeroSlider; two-thumb constraint math |
| AeroAccordion | SMALL | Animated expand/collapse + single/multi mode state |
| AeroSplitPane | MEDIUM | Drag divider + min-size constraint + fraction-based state |
| AeroSidebar | MEDIUM | Three-mode width animation + tooltip in collapsed + persistent push mechanic |
| AeroStepperWizard | MEDIUM | Per-step validation contract + state preservation across navigation |

**Load suggestion for phase planning:**
- Group SMALL items together (RangeSlider, TimePicker, Accordion) → one phase
- MEDIUM items can be paired (DatePicker+DateTimePicker, TreeView+Accordion, Sidebar+SplitPane, StepperWizard+DateTimePicker)
- LARGE items need dedicated phases or generous time: DataTable, DateRangePicker, ColorPicker

---

## Feature Dependencies (cross-component)

```
CalendarGrid (internal primitive)
    ├──used by──> AeroDatePicker
    ├──used by──> AeroDateTimePicker (+ TimePicker columns)
    └──used by──> AeroDateRangePicker (× 2 instances)

AeroSlider (existing v1.x)
    ├──extended by──> AeroRangeSlider (two thumbs)
    └──extended by──> AeroColorPicker (hue strip, alpha strip, RGB sliders)

AeroTextField (existing v1.x)
    └──used by──> AeroColorPicker (HEX input, RGB inputs)

AeroPopover / popup anchor (existing v1.x)
    ├──used by──> AeroDatePicker
    ├──used by──> AeroTimePicker
    ├──used by──> AeroDateTimePicker
    ├──used by──> AeroDateRangePicker
    └──used by──> AeroColorPicker (when used as popup variant)

AeroScrollArea + AeroScrollBar (existing v1.x)
    ├──used by──> AeroDataTable (both axes)
    ├──used by──> AeroTreeView (vertical)
    └──used by──> AeroSidebar (item overflow)

AeroTooltip (existing v1.x)
    └──used by──> AeroSidebar (icon labels in collapsed mode)

AnimatedVisibility / expand animation
    ├──used by──> AeroAccordion (section body)
    ├──used by──> AeroTreeView (node children)
    └──used by──> AeroSidebar (mode transition width)

Expand/collapse arrow icon (animated rotation)
    ├──used by──> AeroAccordion (section header caret)
    └──used by──> AeroTreeView (node toggle caret)
```

**Build order implication:** CalendarGrid primitive must exist before DatePicker, DateTimePicker, DateRangePicker are started. AeroSlider extension must be stable before ColorPicker and RangeSlider are started.

---

## Sources

- WPF DataGrid behavior and virtualization: Syncfusion WPF DataGrid docs, Microsoft DataGrid guidance (keyboard navigation and selection)
- JavaFX TableView / TreeView: OpenJFX TreeTableView UX docs, OpenJDK Wiki TreeView UX documentation
- Qt QTreeView: Qt 6.x QML TreeView docs, Qt Widgets QTreeView class reference
- Range slider expected behavior: W3C WAI-ARIA Slider (Multi-Thumb) Pattern, Open UI Enhanced Range Input explainer
- Date picker UX patterns: Nielsen Norman Group "Date-Input Form Fields" article, Smashing Magazine "Designing The Perfect Date And Time Picker" (2017, still the canonical reference), Eleken "Time Picker UX" 2025
- DateRangePicker patterns: daterangepicker.com docs, MUI X DateRangePicker docs, React Suite DateRangePicker docs
- Color picker UX: Elastic EUI ColorPicker docs, DHIS2 design system color-picker spec, Telerik Design System ColorPicker overview
- Accordion UX: Nielsen Norman Group "Accordions on Desktop: When and How to Use", Medium "Multi-expand vs auto-collapse accordions", LogRocket "Designing effective accordion UIs"
- Sidebar UX: UX Planet "Best UX Practices for Designing a Sidebar", alfdesigngroup.com "Sidebar Design for Web Apps" 2026
- Stepper/Wizard UX: Telerik "Master UX Processes with Blazor Stepper", MUI React Stepper docs, Lollypop "Beyond the Progress Bar" 2026
- Split pane: Apple Human Interface Guidelines "Split views", Java Swing JSplitPane tutorial (Oracle), Mantine split-pane implementation notes
- Data table UX: Pencil & Paper "UX Pattern Analysis: Enterprise Data Tables", UX Planet "Best Practices for Usable Data Tables", AG Grid keyboard interaction docs, MUI X DataGrid sorting docs

---
*Feature research for: aero-compose-ui v2.0 Stateful + Layout (12 components)*
*Researched: 2026-04-30*
