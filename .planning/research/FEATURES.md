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

---
---

# Feature Research — aero-compose-ui v2.0.1 AeroDateTimeRangePicker + Seconds Fix

**Domain:** Datetime range picker — Compose Desktop UI library (additive milestone on v2.0)
**Researched:** 2026-06-22
**Confidence:** HIGH (derived directly from existing codebase + well-established datetime-picker UX conventions)

---

## AeroDateTimeRangePicker — Feature Landscape

### Table Stakes (Users Expect These)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Two-calendar range selection | AeroDateRangePicker already provides this; the datetime variant must match | LOW | Reuse `AeroDateRangeState` + `nextRangeState` verbatim. Both calendars share `leftMonth` driver; `rightMonth = leftMonth + 1`. |
| Start time row + end time row | A range spanning time needs both endpoints timed | MEDIUM | Two `TimeFields` instances bound to `pendingStartTime` / `pendingEndTime`. Small "Start time:" / "End time:" labels distinguish rows. |
| Cancel / Apply commit-gate | AeroDateTimePicker uses this; consistency requires it on any time-bearing picker | LOW | Apply disabled until `rangeState is AeroDateRangeState.Selected`. Cancel dismisses silently, no emission. |
| `showSeconds` + `minuteStep` API parity | AeroDateTimePicker exposes both; callers expect the same on any time-bearing picker | LOW | Both `TimeFields` rows receive the same `showSeconds` and `minuteStep`. No per-endpoint overrides in this milestone. |
| Trigger field shows `DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM` | AeroDateRangePicker renders `"${formatter(start)} → ${formatter(end)}"`; adding time is the natural extension | LOW | Default formatter delegates to `formatAeroDateTime(ldt, showSeconds)` — the same new internal helper that fixes the AeroDateTimePicker seconds bug. With `showSeconds = true`: `DD.MM.YYYY HH:MM:SS → DD.MM.YYYY HH:MM:SS`. |
| `onRangeSelect: (LocalDateTime, LocalDateTime) -> Unit` fires only on Apply | Partial state must never escape to caller | LOW | Single call site guarded by Apply click AND `rangeState is Selected`. |
| Same-day start ≤ end ordering enforcement | When both dates are equal, time order determines validity | MEDIUM | Silent swap at Apply (mirrors `nextRangeState` date-swap). Extract as pure `internal fun orderDateTimeRange(...)` — unit-testable. |
| Pending state keyed on `expanded` | AeroDateTimePicker and AeroDateRangePicker both use `remember(expanded)` to isolate sessions | LOW | All four pending values keyed on `expanded`; cancelled partial selection never leaks into next open. |
| `clearable` / `onClear` / `enabled` / `placeholder` | All existing pickers have these; absence would be a gap | LOW | Mirror AeroDateRangePicker parameter signature exactly. |
| `minDate` / `maxDate` / `selectableDates` | Both calendar grids must respect date bounds | LOW | Pass same `dateIsDisabled` call to both `AeroCalendarGrid` instances. |
| `formatter: ((LocalDateTime) -> String)? = null` | Callers need custom trigger format without fighting a fixed default | LOW | Nullable: `null` uses internal default `formatAeroDateTime(ldt, showSeconds)`. Non-null: caller's formatter used as-is. |

---

### Same-Day Start ≤ End Ordering — Edge Case Detail

When `pendingStartDate == pendingEndDate`, time order determines validity.

**Do not add live-validation UI.** Enforce ordering silently at Apply time, mirroring `nextRangeState`'s existing date-swap pattern:

```kotlin
val startLdt = LocalDateTime(startDate, startTime)
val endLdt   = LocalDateTime(endDate, endTime)
val (orderedStart, orderedEnd) =
    if (startLdt <= endLdt) startLdt to endLdt else endLdt to startLdt
onRangeSelect(orderedStart, orderedEnd)
```

Extract as `internal fun orderDateTimeRange(startDate, startTime, endDate, endTime): Pair<LocalDateTime, LocalDateTime>` — pure, no Compose dependency, unit-testable.

**Cross-day case:** No special handling needed. `2025-06-07T23:59 → 2025-06-08T00:01` is valid; times are unconstrained when dates differ.

---

### Apply Gate — Partial Range Behavior

Apply must be `enabled = rangeState is AeroDateRangeState.Selected`.

While `rangeState` is `Idle` or `SelectingEnd`: Apply is disabled. No error message — the user is mid-selection. The disabled button is sufficient feedback. This is identical to AeroDateTimePicker's `enabled = pendingDate != null` guard.

---

### Trigger Text Format (Conventional Seconds Display)

**Without seconds (default, `showSeconds = false`):**
```
07.06.2025 14:30 → 08.06.2025 09:15
```

**With `showSeconds = true`:**
```
07.06.2025 14:30:00 → 08.06.2025 09:15:45
```

**New internal helper — also fixes the AeroDateTimePicker bug:**

```kotlin
internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String =
    if (showSeconds)
        "${formatAeroDate(ldt.date)} ${"%02d:%02d:%02d".format(ldt.hour, ldt.minute, ldt.second)}"
    else
        "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
```

**AeroDateTimePicker seconds bug root cause (confirmed in source):**
Line 76 of `AeroDateTimePicker.kt` hardcodes `%02d:%02d` regardless of `showSeconds`. Since Kotlin default parameter expressions cannot reference sibling parameters, the fix requires one of:
- Make `formatter` nullable (`((LocalDateTime) -> String)? = null`) — **recommended**, non-breaking
- Inside composable body: `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)`

Existing callers that pass an explicit formatter are unaffected. Callers relying on the default now see seconds when `showSeconds = true`.

---

### Differentiators (Valuable, Not Required)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| "Start time:" / "End time:" labels | Immediately disambiguates which row is which | LOW | Small `Text` label above each `TimeFields`. Essentially free given the two-row design. Include in core implementation. |
| Silent swap at Apply | Forgiving UX — user doesn't get an error for adjusting times in the wrong order | LOW | Already required by same-day ordering. Coherent with `nextRangeState` philosophy. |

---

### Anti-Features (Explicitly Exclude)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Hover-preview range highlight | Web pickers do this | Requires passing hover state into `AeroCalendarGrid` — new API surface, disproportionate for a patch milestone | Defer to a later milestone when `AeroCalendarGrid` is ready for it |
| Per-endpoint `showSeconds` / `minuteStep` | Superficially more flexible | Doubles API surface for an unconfirmed use case; the two time rows must visually match for coherence | Single `showSeconds` + single `minuteStep` applied to both rows |
| Live validation error on same-day time inversion | More explicit feedback | Creates distracting UI state mid-edit; user has not finished yet | Silent swap at Apply |
| Inline (always-visible) mode | Useful for form dashboards | Explicitly deferred to v2.x in PROJECT.md | Popup-only, consistent with all other pickers |
| Time zone selector | Enterprise feature | Massive scope; no TZ abstraction exists in the library | Caller handles TZ externally; component works with `LocalDateTime` only |
| Direct text input for dates / times | Power-user shortcut | Requires parsing, error handling, significant new UI — disproportionate for a patch milestone | Spinner-based `TimeFields` (existing), calendar grid for dates |
| `onStartChange` / `onEndChange` partial emission callbacks | Caller might want intermediate state | Violates commit-gate contract; leads to partial ranges leaking into caller state | Apply-only emission |

---

## Feature Dependencies

```
AeroDateTimeRangePicker
    ├── reuses ──> AeroDateRangeState + nextRangeState  (AeroDateRangePicker.kt)
    ├── reuses ──> AeroCalendarGrid                     (internal, Phase 7 primitive)
    ├── reuses ──> TimeFields                           (internal, shared by AeroTimePicker + AeroDateTimePicker)
    ├── reuses ──> dateIsDisabled                       (AeroDatePicker.kt)
    ├── reuses ──> formatAeroDate                       (AeroDatePicker.kt)
    ├── reuses ──> AeroCalendarPositionProvider         (internal popup)
    ├── reuses ──> PickerPopupContainer                 (internal)
    └── requires ──> formatAeroDateTime(ldt, showSeconds)  [NEW internal helper]

formatAeroDateTime [NEW]
    └── also fixes ──> AeroDateTimePicker trigger seconds bug (line 76)

orderDateTimeRange [NEW internal pure function]
    └── used by ──> AeroDateTimeRangePicker Apply logic
```

`formatAeroDateTime` is the only net-new shared primitive. Add it in the same pass as the AeroDateTimePicker seconds fix, before implementing `AeroDateTimeRangePicker`.

---

## MVP Definition for v2.0.1

### Build

- [ ] `internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String` — gates both the fix and the new component
- [ ] Fix `AeroDateTimePicker` — change `formatter` to `((LocalDateTime) -> String)? = null`; use `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)` in display
- [ ] `internal fun orderDateTimeRange(...)` — pure ordering function, unit-tested
- [ ] `AeroDateTimeRangePicker` with full parameter set: `startValue`, `endValue`, `onRangeSelect`, `modifier`, `formatter`, `placeholder`, `clearable`, `onClear`, `minDate`, `maxDate`, `selectableDates`, `enabled`, `showSeconds`, `minuteStep`
- [ ] Showcase entry in PickersSection demonstrating range with `showSeconds = true`

### Defer

- Hover-preview range highlight — needs `AeroCalendarGrid` API extension
- Time zone awareness — separate feature domain
- Per-endpoint `showSeconds` / `minuteStep` — no consumer request

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| `formatAeroDateTime` helper | HIGH | LOW | P1 — gates both items below |
| AeroDateTimePicker seconds fix | HIGH (bug fix) | LOW | P1 |
| `orderDateTimeRange` pure function | HIGH | LOW | P1 — include with new component |
| AeroDateTimeRangePicker core | HIGH | MEDIUM | P1 |
| Start/end time row labels | MEDIUM | LOW | P1 — include in core, essentially free |
| Showcase PickersSection entry | MEDIUM | LOW | P1 |
| Hover-preview range highlight | LOW (this milestone) | HIGH | P3 — defer |

---

## Sources

- Codebase: `AeroDateRangePicker.kt` — `nextRangeState`, `AeroDateRangeState`, trigger format pattern (`"${formatter(start)} → ${formatter(end)}"`)
- Codebase: `AeroDateTimePicker.kt` — formatter bug (line 76 hardcodes `%02d:%02d`), Apply gate pattern (`enabled = pendingDate != null`), `combineDateTime`
- Codebase: `AeroDatePicker.kt` — `formatAeroDate`, `dateIsDisabled`
- Codebase: `internal/TimeFields.kt` — `TimeFields` composable, `assembleTime`
- Codebase: `PROJECT.md` — milestone scope, deferred features, v2.0 decisions
- Convention: `nextRangeState` date-swap behavior applied to datetime ordering at Apply
- Kotlin language: default parameter expressions cannot reference sibling parameters — drives the nullable formatter fix pattern

---
*Feature research for: AeroDateTimeRangePicker UX + AeroDateTimePicker seconds display (v2.0.1)*
*Researched: 2026-06-22*

---
---

# Feature Research — aero-compose-ui v2.0.2 AeroPanelGroup

**Domain:** Resizable + collapsible multi-panel vertical layout — Compose Desktop UI library
**Researched:** 2026-06-22
**Confidence:** HIGH (industry references verified: VS Code sidebar model, react-resizable-panels v4 API, existing AeroAccordion/AeroSplitPane codebase, JetBrains tool-window patterns)

---

## Scope Framing

This research covers **one additive component** (`AeroPanelGroup` + `AeroPanelSection`) for a single-phase milestone.
The gap this fills: neither AeroAccordion (sized by content, no resize) nor AeroSplitPane (2-pane, no collapse) nor AeroSidebar (single panel) supports "N vertical sections where any section collapses to its header-strip and neighbors absorb the freed space."

The VS Code Left Sidebar (Explorer / Source Control / Outline / Timeline stacked accordion with drag-resizable borders) is the canonical reference. react-resizable-panels (bvaughn, v4) is the most-studied web equivalent.

All locked decisions from the milestone context are respected verbatim and not reconsidered.

---

## AeroPanelGroup + AeroPanelSection — Feature Landscape

### Table Stakes (Must Ship — Users Expect These)

Features whose absence makes the component feel broken or incomplete for its stated purpose.

| Feature | Why Expected | Complexity | Observable User Action |
|---------|--------------|------------|------------------------|
| **N sections in a vertical column filling available height** | The component's raison d'etre — unlike AeroAccordion which sizes by content, panels must fill the parent | MEDIUM | User places AeroPanelGroup; it occupies the full height of its parent container and divides it among expanded sections |
| **Click section header to collapse — header strip remains visible** | VS Code model; AeroAccordion pattern already establishes this expectation in the library | LOW | Click header → section body animates to zero height; only header strip (~36dp) remains; neighbors grow to fill the freed space |
| **Click collapsed header to expand — restores prior size** | Symmetric with collapse; size memory is the defining difference from a pure accordion | MEDIUM | Click collapsed header → section body animates back to its size before collapsing; neighbors shrink accordingly |
| **Drag border between two adjacent expanded sections to resize them** | VS Code Left Sidebar does this; react-resizable-panels does this; users with muscle-memory from AeroSplitPane expect it | MEDIUM | Mouse-down on divider between two expanded sections → drag up/down changes their relative sizes; cursor changes to N_RESIZE; divider has 8dp hit-area (same as AeroSplitPane) |
| **Draggable divider exists ONLY between two expanded neighbors** | The divider between an expanded section and a collapsed section is a static join, not a drag target — VS Code model | LOW | User cannot accidentally drag the boundary next to a collapsed header; cursor does not change on hover of a static join |
| **Neighbor space redistribution on collapse** | Freed space does not disappear — it is given proportionally to the remaining expanded sections | MEDIUM | Section B collapses; sections A and C (both expanded) each grow; total height remains constant |
| **Neighbor space redistribution on expand** | Space is taken proportionally from the other expanded sections | MEDIUM | Section B expands back; sections A and C shrink proportionally; total height remains constant |
| **Min-size clamping per section** | Prevents drag from making a section too small to be useful; same as `AeroSplitPane` clamp | LOW | Drag divider: section cannot be dragged below its `minSize`; divider stops/bounces at that threshold |
| **`collapsible = false` per section** | Some sections must always be visible (e.g., a mandatory output panel); locked decision | LOW | No chevron rendered in that section's header; header click does nothing; section always participates in resize |
| **`resizable = false` per section (or group-level)** | Some layouts need collapse/expand but no drag resize — pure accordion-style redistribution | LOW | No draggable dividers rendered anywhere when `resizable = false`; all borders are static; space redistribution still works on collapse/expand |
| **Win7 Aero header styling** | Library aesthetic rule; headers must use `glassPanel` modifier, matching AeroAccordion visual language | LOW | Header shows gloss gradient, rounded corners, theme-appropriate background; matches AeroAccordion header appearance |
| **CaretRight chevron in header, rotates 0°→90° on expand** | Established library convention from AeroAccordion | LOW | Collapsed: caret points right (0°); expanded: caret points down (90°); animates via `animateFloatAsState` at 200ms |
| **Collapse/expand animation via `animateFloatAsState` on section height** | AeroSidebar uses 200ms FastOutSlowInEasing; AeroAccordion uses 160ms; PanelGroup must follow same family | MEDIUM | Collapse/expand is visually smooth, ~200ms, FastOutSlowInEasing; no jank during animation |
| **Drag produces no animation — direct px writes** | VS Code and react-resizable-panels both do this; animation during drag makes the component feel laggy | LOW | Mouse drag on divider: sizes update immediately, no easing; only toggle (click header) animates |
| **Hybrid controlled/uncontrolled expansion API** | Locked decision: must match AeroAccordion pattern exactly | MEDIUM | Uncontrolled (default): component manages which sections are expanded; Controlled: caller passes `expandedIndices` + `onExpandedChange`; both branches are intentional |
| **`onLayoutChange` callback for persist/restore of section sizes** | Locked decision; caller needs to persist layout across sessions | LOW | Fires with a `List<Float>` of current section size fractions after every drag-end or collapse/expand; caller stores these and passes back as `initialSizes` on next composition |
| **`initialSizes: List<Float>` parameter for restore** | Symmetric with `onLayoutChange`; without it the persist/restore round-trip is incomplete | LOW | Caller passes previously persisted sizes; group initializes sections to those proportions |
| **Section content composable slot** | PanelGroup is a layout shell; content is arbitrary | LOW | `AeroPanelSection(title = "Explorer") { /* arbitrary content */ }` — standard slot pattern |
| **Section title displayed in header** | Header must identify the section when collapsed | LOW | `title: String` per section; rendered as `Text` in header row |
| **Showcase entry in LayoutSection** | Library convention: every new component gets a showcase demo | LOW | LayoutSection contains an AeroPanelGroup demo showing 3+ sections with mix of collapsed/expanded, verifiable across three themes |

---

### Differentiators (Nice to Have — In Scope if Cheap)

Features that are not expected by default but add meaningful value with low implementation cost given the existing primitives.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **`headerActions: @Composable RowScope.() -> Unit` slot per section** | VS Code section headers have action icons (e.g., "+" to add item, "..." overflow menu); adds real utility for IDE-like UIs | LOW | Rendered right-aligned in the header row, before the chevron; does not interfere with collapse toggle on the title/chevron click zone. Cheap: just a trailing slot in the header Row. |
| **`leadingIcon: ImageVector?` per section** | Matches AeroAccordion API; lets caller put a domain icon next to the section title | LOW | If non-null, render `Icon(AeroIcons.*)` left of title with explicit tint; same pattern as AeroAccordion. Essentially free. |
| **Unit tests for pure layout logic** | Established library quality gate: SplitClampTest, AccordionToggleTest, SidebarStateTest patterns already exist | LOW | Test: space redistribution math, min-size clamp, `lastExpandedPx` restore on re-expand, `onLayoutChange` emission. No Compose runtime needed for these functions if extracted as pure functions. |

---

### Anti-Features (Explicitly Out — Do Not Build)

Features that might be requested but are out of scope for this single-phase milestone. Documenting these prevents scope creep during requirements definition.

| Anti-Feature | Why Excluded | Alternative / What to Do Instead |
|-------------|--------------|-----------------------------------|
| **Horizontal orientation** | Locked decision: vertical-only in v2.0.2. Horizontal is deferred to a future milestone. | Caller uses AeroSplitPane (Horizontal orientation) for horizontal splits |
| **Drag-reorder of sections** | Requires hit-test between section headers during drag, scroll-during-drag, and index mutation — a distinct feature domain with high complexity. Not part of VS Code's drag model for section panels. | Sections are declared statically in order; caller controls order at composition time |
| **Nested AeroPanelGroups** | Two layers of N-section collapse+resize creates compounded state coordination complexity. Not in the VS Code model. | Caller can compose AeroPanelGroup inside a section's content slot; library does not explicitly support or test cross-group state |
| **Programmatic collapse/expand via imperative handle** | React-resizable-panels provides `collapse()` / `expand()` on a ref; for this milestone, toggle is only via header click or controlled API. Imperative handle adds API surface with no demonstrated consumer need. | Caller uses the controlled API (`expandedIndices` + `onExpandedChange`) to programmatically change state |
| **Double-click divider to collapse/expand** | react-resizable-panels has this; VS Code does not. Adds complexity to the drag handler (distinguish click from double-click). No consumer request. | Single-click chevron is the collapse affordance |
| **`maxSize` per section** | Limits how large a section can grow. Increases clamp complexity; no VS Code parallel. Consumer can constrain via content (e.g., a fixed-height scroll area inside the section). | Not added; minSize clamping is sufficient |
| **`disabled` per section** | Disabling a section (no interact, no visual affordance) is a distinct concept from `collapsible = false`. Not in the locked decisions. | Use `collapsible = false` to prevent collapse; content inside the section handles its own disabled state |
| **Scroll within AeroPanelGroup itself** | If all sections are taller than the group, do not add a scroll. Sections size themselves to fill available space — scroll belongs inside section content. | Caller puts `AeroScrollArea` inside a section's content slot if the section content is taller than the section |
| **Keyboard resize of dividers** | react-resizable-panels supports arrow-key resize of focused separators for accessibility. High implementation cost for a Compose Desktop-only library without a clear accessibility requirement in scope. | Drag mouse interaction only; deferred to future |
| **Animation during drag (easing on drag delta)** | Locked decision: drag writes px directly without animation. Animation during drag makes the divider feel "drunk." | Only toggle (collapse/expand) animates |

---

## Feature Dependencies

```
AeroPanelGroup / AeroPanelSection
    ├── reuses ──> aeroDragSplitter (Modifier)        (AeroDragSplitter.kt — Phase 7 primitive)
    ├── reuses ──> clampDividerPx / SplitClamp        (SplitClamp.kt — internal splitpane)
    ├── reuses ──> animateFloatAsState + tween         (Compose animation)
    ├── reuses ──> BoxWithConstraints                  (same pattern as AeroSplitPane)
    ├── reuses ──> glassPanel modifier                 (GlassModifiers.kt)
    ├── reuses ──> AeroIcons.CaretRight                (chevron, same as AeroAccordion)
    └── follows ──> AeroAccordion hybrid controlled/uncontrolled pattern

Collapse/expand toggle
    └── animates ──> section sizePx via animateFloatAsState (200ms FastOutSlowInEasing)

Drag resize
    └── writes ──> section sizePx directly (no animation, same as AeroSplitPane drag)

Space redistribution (collapse/expand)
    └── requires ──> availableForExpanded = totalPx − Σ(collapsed header heights) − Σ(divider thicknesses)
    └── section size stored as fraction of availableForExpanded (survives window resize, same as AeroSplitPane fraction pattern)

lastExpandedPx (restore on re-expand)
    └── stored per section — does not survive controlled remount; lives in internal state alongside fraction
```

### Dependency Notes

- `aeroDragSplitter` already handles cursor change, `awaitPointerEventScope` (no touchSlop), and the `onDrag(deltaPx)` callback. No changes needed to it.
- `clampDividerPx` already has the inverted-range guard from FIXSP-02. AeroPanelGroup uses a different clamp formula (per-section min, not total min), but the guard pattern is the same — extract or adapt.
- The `animateFloatAsState` approach (not `animateContentSize`) is required because section heights are px values that must be co-animated with neighbors shrinking/growing in the opposite direction. `animateContentSize` on a single section would not coordinate with neighbors.
- `BoxWithConstraints` gives `totalPx` at composition; this avoids `SubcomposeLayout` overhead confirmed by AeroSplitPane history.

---

## MVP Definition for v2.0.2

This is the only phase, so "MVP" = "what ships."

### Build (Table Stakes + Cheap Differentiators)

- [ ] `AeroPanelGroup` composable: `sections` list, `modifier`, `minSectionSize`, `initialSizes`, `expandedIndices` (controlled), `onExpandedChange` (controlled), `onLayoutChange`
- [ ] `AeroPanelSection` data class: `title`, `content`, `collapsible`, `resizable`, `leadingIcon`, `headerActions`
- [ ] Internal: fraction-based size state per section; `lastExpandedFraction` per section for restore
- [ ] Internal: `availableForExpanded` computation; proportional redistribution on toggle
- [ ] Internal: draggable divider rendered only between two adjacent expanded sections
- [ ] Internal: static join rendered between expanded and collapsed neighbors (no hit-area, no cursor change)
- [ ] Internal: pure redistribution functions extracted for unit tests (no Compose runtime dependency)
- [ ] Unit tests: space redistribution, min-size clamp, last-size restore, `onLayoutChange` emission
- [ ] Showcase: `LayoutSection.kt` AeroPanelGroup demo — 3+ sections, at least one `collapsible = false`, at least one with `headerActions`, initial collapsed state demonstrating the strip; three-theme visual sign-off

### Defer

- Horizontal orientation — next milestone candidate
- Keyboard resize of dividers — accessibility future work
- Imperative collapse/expand handle — no consumer request yet
- `maxSize` per section — not needed for known use cases

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| N sections fill height, collapse to strip | HIGH | MEDIUM | P1 — core behavior |
| Neighbor space redistribution on toggle | HIGH | MEDIUM | P1 — without this, collapse just hides content |
| Drag resize between expanded neighbors | HIGH | MEDIUM | P1 — VS Code model requirement |
| `collapsible = false` flag | HIGH | LOW | P1 — locked decision |
| `resizable = false` flag | MEDIUM | LOW | P1 — locked decision |
| Collapse/expand animation (200ms) | HIGH | MEDIUM | P1 — library animation standard |
| Hybrid controlled/uncontrolled expansion API | HIGH | MEDIUM | P1 — locked decision |
| `onLayoutChange` + `initialSizes` persist/restore | HIGH | LOW | P1 — locked decision |
| `headerActions` slot | MEDIUM | LOW | P1 — cheap differentiator, include |
| `leadingIcon` per section | LOW | LOW | P1 — essentially free, matches AeroAccordion API |
| Unit tests for pure logic | HIGH | LOW | P1 — library quality gate |
| Showcase demo | HIGH | LOW | P1 — library convention |
| Keyboard resize | LOW | HIGH | P3 — defer |
| Horizontal orientation | LOW (this milestone) | HIGH | P3 — explicitly deferred |

---

## Competitor / Reference Behavior Summary

For REQ-ID definition reference: observed behaviors across VS Code, react-resizable-panels v4, and JetBrains tool windows.

| Behavior | VS Code Left Sidebar | react-resizable-panels v4 | JetBrains Tool Windows | Our Approach |
|----------|---------------------|--------------------------|----------------------|--------------|
| Collapse to header strip | Yes — collapses upward | Yes — `collapsible` prop, collapses to `collapsedSize` (default 0) | Yes — docked tool windows collapse to tab strip | Yes — collapses to ~36dp header |
| Restore prior size on expand | Yes — remembers height | Yes — library remembers last size internally | Yes — tool window remembers size | Yes — `lastExpandedFraction` per section |
| Neighbor fills freed space | Yes | Yes — proportional redistribution | Yes | Yes — proportional redistribution |
| Drag border between expanded sections | Yes | Yes — PanelResizeHandle | Yes — drag sash between adjacent tool windows | Yes — via `aeroDragSplitter` |
| No drag target next to collapsed section | Yes | Depends on config | Yes | Yes — static join |
| `onLayout` callback | Yes (via resize observer) | Yes — `onLayoutChange` (fires on drag end) and `onLayoutChanged` | Via layout persistence APIs | Yes — `onLayoutChange` fires after drag-end or toggle |
| Min-size clamping | Yes | Yes — `minSize` prop | Yes | Yes — `minSectionSize: Dp` per group (or per section) |
| Controlled expansion | No (view visibility is always uncontrolled in VS Code sidebar) | Yes — imperative `collapse()` / `expand()` via ref | No | Yes — hybrid per AeroAccordion pattern |
| Header action buttons | Yes (+ and ... per section) | Not built-in | Yes (gear icon in tool window title) | Yes — `headerActions` slot |
| Animation on toggle | Yes — smooth height transition | No built-in animation (library is headless) | Yes | Yes — `animateFloatAsState` 200ms |
| Animation on drag | No | No | No | No — direct px writes |

---

## Sources

- VS Code Left Sidebar behavior: `github.com/microsoft/vscode/issues/204250` (collapse direction discussion), VS Code UX Guidelines `code.visualstudio.com/api/ux-guidelines/sidebars`
- react-resizable-panels v4 API: `github.com/bvaughn/react-resizable-panels` README + CHANGELOG — confirmed `collapsible`, `collapsedSize`, `minSize`, `defaultSize`, `onResize`, `groupRef.getLayout()`/`setLayout()`; `onCollapse`/`onExpand` removed in v4 in favor of `onResize`; `onLayoutChanged` fires after resize completes (MEDIUM confidence via WebFetch of changelog)
- Existing codebase: `AeroAccordion.kt` — hybrid controlled/uncontrolled pattern, `animateFloatAsState` at 160ms, CaretRight 0°→90°, `glassPanel` header
- Existing codebase: `AeroSplitPane.kt` — `BoxWithConstraints` + fraction state + `aeroDragSplitter` + `clampDividerPx`; fraction-over-px design rationale and `rememberUpdatedState` live-read pattern
- Existing codebase: `AeroDragSplitter.kt` — confirmed `Orientation.Vertical` → `N_RESIZE_CURSOR`, `awaitPointerEventScope` no-touchSlop loop
- Existing codebase: `SplitClamp.kt` — `clampDividerPx` with `coerceAtLeast` inverted-range guard
- JetBrains tool window docs: `jetbrains.com/help/idea/tool-windows.html` — tool window resize, per-window size memory, side-by-side stacking
- PROJECT.md: locked decisions, `AeroPanelGroup` target features, deferred list

---
*Feature research for: AeroPanelGroup + AeroPanelSection (v2.0.2)*
*Researched: 2026-06-22*
