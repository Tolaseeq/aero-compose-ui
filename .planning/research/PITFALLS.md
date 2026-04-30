# Pitfalls Research

**Domain:** Compose Desktop UI library — adding v2.0 Stateful + Layout components to aero-compose-ui
**Researched:** 2026-04-30
**Confidence:** HIGH (grounded in actual codebase reading + confirmed upstream issues)

---

## Critical Pitfalls

### PITFALL-01: LazyColumn inside AeroScrollArea (DataTable virtualization destroyed)

**Components:** AeroDataTable
**Failure mode:** Compiles but wrong behavior — rows are never virtualized; all rows render at once; frame drops on 500+ rows.

**What goes wrong:**
`AeroScrollArea` wraps a `Column` with `Modifier.verticalScroll(state)`. If AeroDataTable is naively implemented by placing a `LazyColumn` inside `AeroScrollArea`, Compose gives the `LazyColumn` an infinite-height constraint (because its parent is a scrollable `Column`). Compose crashes at runtime with *"LazyColumn does not support scrollable containers as parents"* or silently falls back to composing all items, destroying virtualization.

**Why it happens:**
Developers reach for `AeroScrollArea` by habit because it gives the Aero-styled scrollbar automatically. The existing v1.0 pattern (wrap content in `AeroScrollArea`) is wrong for any lazy list. This exact pattern already caused the AeroDropdown popup offset regression in v1.0 (`AeroScrollArea` → `Column.fillMaxWidth()` + unbounded height forcing the 320dp cap outward).

**Root cause:** `Modifier.verticalScroll` imposes infinite max height on children, which breaks `LazyColumn`'s height measurement entirely.

**Prevention:**
- AeroDataTable MUST own its own `LazyColumn` with `LazyListState` and pair it with a standalone `AeroScrollBar` (the existing `AeroScrollBar` composable already accepts a `ScrollState`-alike via `rememberScrollbarAdapter`). Do NOT use `AeroScrollArea`.
- Provide a `rememberLazyListState()` and pass it to both `LazyColumn` and `AeroScrollBar(lazyListState)`. Add a `LazyScrollbarAdapter` — Compose Desktop Foundation's `rememberScrollbarAdapter` overload accepts `LazyListState`.
- Add a `Modifier.fillMaxHeight()` (or caller-supplied height constraint) to the DataTable outer container so `LazyColumn` receives a bounded max height.

**Warning signs:** If the DataTable renders all rows regardless of visible area, or if a `MeasureException` appears in logs mentioning "infinite constraints", this pitfall has been hit.

**Phase to address:** DataTable implementation phase (Phase 1 of v2.0). Must be decided in the plan before writing a single row of DataTable layout code.

---

### PITFALL-02: AeroPopupPositionProvider clamps calendar popup to trigger width

**Components:** AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker
**Failure mode:** Compiles but wrong behavior — calendar popup is clipped or repositioned to an unusable offset because the calendar is wider than the trigger field.

**What goes wrong:**
`AeroPopupPositionProvider` positions the popup left-aligned to `anchorBounds.left`, which is correct for dropdowns that match trigger width. A calendar panel is typically 300–320 dp wide; `AeroDateRangePicker` with two months side-by-side is 560–640 dp. The `clamp()` in `AeroPopupPositionProvider` will push the calendar leftward if it overflows the right edge of the window, but if the trigger is near the right edge, the calendar may partially underflow the left edge. The `overflows()` check will flip to `AeroPopupSide.Top` (not Start/End), moving the calendar above the field rather than to the side — correct for narrow dropdowns, wrong for wide calendars.

**Root cause:**
`AeroDropdownPopup` uses `widthIn(min = anchorWidth, max = anchorWidth)` — it locks the popup to trigger width. Calendar pickers need `widthIn(min = calendarWidth)` with a separate horizontal-alignment strategy. The existing `AeroPopupPositionProvider` was designed for same-width dropdowns.

**Prevention:**
- Do NOT reuse `AeroDropdownPopup` for date pickers. Use raw `Popup(popupPositionProvider = ...)` with a new `AeroCalendarPositionProvider` that:
  1. Places the popup left-aligned to `anchorBounds.left` by default.
  2. If `anchorBounds.left + calendarWidth > windowSize.width`, right-aligns to `anchorBounds.right` instead.
  3. Never flips to Top/Bottom just because width overflows — only flips to Top/Bottom when *height* overflows.
- `AeroDateRangePicker` specifically needs a minimum width of ~560 dp and should check horizontal overflow before vertical.
- Keep `PopupProperties(focusable = true)` for all date pickers so keyboard navigation (arrow keys, Enter, Esc) works without separate workarounds.

**Warning signs:** Calendar appears right-edge clipped on a narrow window, or the calendar jumps above the trigger field when the trigger is placed near the right edge of the window.

**Phase to address:** Date/time pickers implementation phase. The `AeroCalendarPositionProvider` must be written as the first task in that phase, before any calendar grid rendering.

---

### PITFALL-03: Desktop touchSlop breaks drag on Canvas components (ColorPicker HSV + RangeSlider + DataTable column resize)

**Components:** AeroColorPicker (HSV square drag, hue bar drag), AeroRangeSlider (dual thumb drag), AeroDataTable (column resize splitter drag)
**Failure mode:** Compiles, no crash — drag silently does nothing. User clicks and holds but the drag never starts.

**What goes wrong:**
`detectDragGestures` in Compose Desktop uses `DesktopViewConfiguration.touchSlop = 18dp`. Mouse movements are typically 1–3px per event. The gesture is cancelled before it starts because the accumulated delta never reaches 18dp between `awaitFirstDown` and the slop threshold check (`awaitTouchSlopOrCancellation`). This is a confirmed upstream issue (JetBrains/compose-multiplatform issue #343, not yet fixed as of CMP 1.7.3).

**Root cause:**
`detectDragGestures` is designed for touch screens where a thumb moves 18px before it's clear the user is dragging (not tapping). On Desktop with a mouse, the pointer reports exact pixel positions and events fire very frequently — 18dp of cumulative movement represents a large deliberate mouse drag that feels laggy.

**Prevention:**
For all Canvas-based drag interaction in v2.0 components, use the lower-level `awaitPointerEventScope` + `awaitFirstDown` + custom loop instead of `detectDragGestures`:

```kotlin
Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            // handle immediately on first down — no slop wait
            var position = down.position
            onDragStart(position)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (change.pressed) {
                    position = change.position
                    change.consume()
                    onDrag(position)
                } else {
                    onDragEnd()
                    break
                }
            }
        }
    }
}
```

This bypasses `detectDragGestures` entirely and fires on every pointer event after `awaitFirstDown`.

**Warning signs:** HSV square cursor position doesn't update during drag; RangeSlider thumbs don't move on mouse drag; DataTable column splitter doesn't respond to drag.

**Phase to address:** Every phase that implements a drag-heavy component. Must be established as a shared utility (`AeroPointerDragUtil` or inline pattern) in the first v2.0 phase that touches drag (DataTable or ColorPicker, whichever comes first).

---

### PITFALL-04: DataTable selection indices become stale after sort

**Components:** AeroDataTable
**Failure mode:** Compiles and appears to work — but selected rows jump to wrong rows after the user clicks a column header to sort.

**What goes wrong:**
If selection state is stored as `Set<Int>` (row indices into the displayed list), sorting the data produces a new order and old indices now point to different rows. The user selected row 3 (which contained "Saturn"), but after sorting alphabetically row 3 is now "Uranus". The highlight appears on the wrong row; nothing crashes.

**Root cause:**
Index-based selection is tempting because LazyColumn works with indices, but DataTable data is inherently sortable, so the displayed index is unstable.

**Prevention:**
- Selection state MUST be `Set<RowKey>` where `RowKey` is a stable identifier from the row data itself (e.g., the row object's identity, or an explicit `id: K` parameter the caller provides via `AeroTableColumn<T, K>`).
- The DataTable API should require callers to supply a `key: (T) -> Any` lambda (mirrors `LazyColumn`'s `items(key = ...)` parameter).
- On sort, the displayed list reorders but `selectedKeys: Set<Any>` is unchanged — rows with matching keys simply receive the selected highlight regardless of their current position.
- Use the same `key` in `LazyColumn`'s `items(key = key)` for correct item reuse during recomposition.

**Warning signs:** If unit tests verify "select row at index 2, sort, verify row at index 2 still highlighted" — that test would pass but verify the wrong thing. Write tests that verify "select row with key X, sort, verify row with key X still highlighted".

**Phase to address:** DataTable state design — must be locked in the API design discussion before any implementation.

---

### PITFALL-05: TreeView lazy callback called on every recomposition (no debounce / key guard)

**Components:** AeroTreeView
**Failure mode:** Compiles but wrong behavior — `onExpand` fires repeatedly for already-expanded nodes, causing duplicate network/IO calls or flickering children.

**What goes wrong:**
If the `LaunchedEffect` that triggers child loading uses `expanded` (a `Boolean` in node state) as its key without also checking whether children are already loaded, every recomposition that touches the node (hover highlight, scroll into view, parent re-render) can re-fire the effect. Even if `LaunchedEffect(expanded)` is used correctly, if the node is removed from the composition (scrolled off in a LazyColumn) and then scrolled back, the effect re-fires with the same key value because the composable was disposed and recomposed from scratch.

**Root cause:**
`LazyColumn` disposes composables when they scroll out of the viewport. When the node scrolls back in, Compose recomposes it from scratch. `LaunchedEffect(expanded = true)` sees `true` and re-fires `onExpand`.

**Prevention:**
- Keep `childrenLoaded: Boolean` in the node's state alongside `expanded: Boolean`.
- `LaunchedEffect(expanded)` body: `if (expanded && !node.childrenLoaded) { onExpand(node); node.childrenLoaded = true }`.
- Use a single `SnapshotStateMap<NodeKey, NodeState>` at the tree level (not in each node composable) to hold expanded + childrenLoaded. This state survives LazyColumn item disposal because it lives above the LazyColumn.
- The `onExpand` callback should be idempotent on the caller side as a defensive fallback, but the library must not rely on that.

**Warning signs:** Passing a logging lambda as `onExpand` and seeing duplicate log lines for the same node after scrolling.

**Phase to address:** TreeView implementation phase. State shape must be finalized before implementation begins.

---

### PITFALL-06: DateRangePicker partial state when only start date is selected

**Components:** AeroDateRangePicker
**Failure mode:** Compiles but subtle UX issue that is hard to recover from once the API is published — the `onRangeSelect` callback receives a half-valid value.

**What goes wrong:**
A `DateRange(start: LocalDate, end: LocalDate)` return type forces a valid range. If the library calls `onRangeSelect` with a synthesized `DateRange(start, start)` when only the start is picked, callers who check `start == end` to detect "no range" have a fragile convention. Alternatively, if the library uses `DateRange(start: LocalDate?, end: LocalDate?)` and calls the callback after each click, callers receive two calls per range selection — one with `end = null` (incomplete) and one complete.

**Root cause:**
The interaction model for range selection is: click 1 = set start; click 2 = set end. Between clicks, the state is partial. The API must represent partial state without leaking it to callers as a completed range.

**Prevention:**
- Use a sealed type for the pending state: `AeroDateRangeState = Idle | SelectingEnd(start) | Selected(start, end)`.
- Expose it as an observable `val rangeState: AeroDateRangeState` that the host can read for rendering (show a "partial" indicator in its own UI).
- Only call `onRangeSelect(start, end)` when both dates are committed (state transitions to `Selected`).
- Internally, while in `SelectingEnd`, highlight the start date and show a hover preview of the potential end date on mouse-over — requires tracking `hoveredDate` as separate internal state.

**Warning signs:** If `onRangeSelect` is called with `start == end`, the caller-facing API has leaked partial state.

**Phase to address:** Date/time pickers design phase (API surface discussion). Must be settled before calendar grid implementation.

---

## Moderate Pitfalls

### PITFALL-07: AeroRangeSlider dual thumb overlap — smaller thumb permanently trapped

**Components:** AeroRangeSlider
**Failure mode:** Compiles and mostly works — but when the two thumbs overlap, dragging either one can "pass through" the other, making it impossible to separate them.

**What goes wrong:**
If both thumbs share a single `pointerInput` zone computed from the track, the first thumb's hit area overlaps the second when they converge. Whichever thumb's composable is drawn on top captures all pointer events. The lower thumb becomes unreachable by mouse.

**Root cause:** No minimum separation constraint, no Z-order strategy for overlapping thumbs.

**Prevention:**
- Enforce a minimum separation: `endValue = max(endValue, startValue + minStepGap)` on every drag update, where `minStepGap` is at least one step value (or a fixed dp-converted fraction if continuous).
- When thumbs are within 4dp of each other, give the thumb that was most recently moved a higher Z-order (drawn last, so its hit area wins pointer events). Track `lastMovedThumb: Thumb` in state.
- Use `AeroSlider`'s visual as a reference but do NOT try to compose two `AeroSlider` instances — they each own a full Material3 `Slider` which has its own interaction state and will not coordinate.

**Warning signs:** End value equals start value and dragging either thumb immediately snaps it back; no visual indication which thumb is on top.

**Phase to address:** AeroRangeSlider implementation phase.

---

### PITFALL-08: AeroPopupPositionProvider `unmeasured` sentinel fires on first frame of calendar popup

**Components:** AeroDatePicker, AeroDateRangePicker, AeroColorPicker
**Failure mode:** Compiles, no crash — but the popup appears at the window corner for one frame before snapping to the correct position, causing a visible flash.

**What goes wrong:**
`AeroPopupPositionProvider.calculatePosition()` returns `IntOffset(windowSize.width + popupContentSize.width, windowSize.height + popupContentSize.height)` when `popupContentSize >= windowSize` (the `unmeasured` guard). On the very first frame after `expanded = true`, Compose has not yet measured the popup — `popupContentSize` is `(0, 0)`. Neither condition `>= windowSize` is true, so the sentinel does NOT fire. Instead, `primaryFor()` runs with `popup = (0, 0)` and returns `IntOffset(anchor.left, anchor.bottom + gap)` — which happens to be correct. **However**, if the calendar content is loaded asynchronously or has a complex layout, the first measurement may return a size smaller than the final size, causing a layout jump on frame 2.

For `AeroDateRangePicker` specifically: the popup is wide (~560dp). On a typical 1280dp window, `popupContentSize.width (560) < windowSize.width (1280)`, so the sentinel never fires even when the popup hasn't measured yet. The guard condition `>= windowSize` was designed for dropdowns (max 320dp) and is too conservative for wide calendars.

**Prevention:**
- In the calendar position provider, treat `popupContentSize == IntSize.Zero` as unmeasured (not `>= windowSize`).
- Add `if (popupContentSize == IntSize.Zero) return IntOffset.Zero` as the unmeasured guard (places popup off-screen at top-left, invisible on first frame).
- Keep the calendar popup's layout structure non-lazy (all cells rendered eagerly) so measurement is stable on frame 1. For `AeroDateRangePicker`, two fixed-size month grids will always measure to the same size — no async concern.

**Warning signs:** Calendar popup jumps position between frame 1 and frame 2 during visual checkpoint.

**Phase to address:** Date/time pickers implementation phase, during the position provider work (PITFALL-02 mitigation).

---

### PITFALL-09: AeroDark contrast failure on disabled date cells

**Components:** AeroDatePicker, AeroDateRangePicker
**Failure mode:** Subtle UX issue — disabled dates (past dates, or dates outside the selectable range) are invisible in AeroDark.

**What goes wrong:**
AeroDark's `onSurface` is `Color(0xFFCCCCCC)` and `background` is `Color(0xFF0A0A1A)`. Disabled cells at `0.4f` alpha render as `Color(0xFFCCCCCC).copy(alpha = 0.4f)` on the dark background — WCAG contrast ratio is approximately 1.6:1, which is below readable. The cell exists but appears as near-invisible grey on dark.

**Root cause:** All v1.0 components use the `0.4f` alpha convention for disabled state (see `AeroSlider`, `AeroListItem`). This works in AeroBlue (lighter backgrounds) but fails in AeroDark at cell scale (calendar cells are ~32×32dp, smaller than list items).

**Prevention:**
- Date cells in AeroDark should use `labelText` (`Color(0xFFAAAAAA)`) for disabled text instead of `onSurface.copy(alpha = 0.4f)`. This gives approximately 3:1 contrast on the `0x0A0A1A` background.
- In Classic, `labelText = Color.LightGray` at 0.4f alpha → `Color(0xFFD3D3D3).copy(alpha = 0.4f)` on `Color(0xFF1E1E1E)` background — also borderline. Use `borderDefault` (`Color(0xFF555555)`) as disabled cell text color in Classic.
- The three-theme visual checkpoint for date pickers must explicitly include a "disabled cells are visible in all three themes" verification item.

**Warning signs:** Visual checkpoint: disabled dates blend into background in AeroDark or Classic.

**Phase to address:** Date/time pickers visual checkpoint plan. Flag during design phase, verify during visual sign-off.

---

### PITFALL-10: AeroDataTable selection highlight conflicts with buttonHover token

**Components:** AeroDataTable
**Failure mode:** Subtle UX issue — selected rows and hovered-but-unselected rows look identical in AeroBlue.

**What goes wrong:**
`AeroListItem` uses `colors.primary.copy(alpha = 0.2f)` for selected and `colors.buttonHover` for hover. In AeroBlue: `primary = Color(0xFF4FC3F7)` at 0.2f alpha = `Color(0x334FC3F7)`. `buttonHover = Color(0x40FFFFFF)`. These render as similar light-blue/white overlays on the dark `0xCC1A3A5C` surface. A selected-and-hovered row must not merge the two states visually into an ambiguous color.

**Root cause:** `buttonHover = Color(0x40FFFFFF)` is a neutral white overlay; `primary.copy(0.2f)` is a blue tint. They are close enough in luminance that when overlaid (selected + hovered), the visual distinction is subtle.

**Prevention:**
- For DataTable rows, selected state should use `colors.borderSelected.copy(alpha = 0.15f)` as background (a more saturated color token already designated for selection, distinct from `buttonHover`).
- Hover-on-selected row: add `colors.buttonHover` on top of the selected background (stacked alpha, not replacing). This produces a visually distinct "selected + hovered" vs "selected" vs "hovered" vs "normal" — four readable states.
- Validate all four states in the three-theme visual checkpoint. AeroDark is the most likely to collapse states (all tokens have very low alpha).

**Warning signs:** In the showcase DataTable demo, clicking a row and then hovering another looks the same as having two selected rows.

**Phase to address:** DataTable implementation phase. Establish the four-state color scheme in the design discussion before writing row composable code.

---

### PITFALL-11: AeroSidebar width animation conflict with AeroSplitPane left pane

**Components:** AeroSidebar + AeroSplitPane (when composed together by caller)
**Failure mode:** Compiles but layout thrashes — SplitPane divider position jumps when sidebar collapses.

**What goes wrong:**
`AeroSidebar` in `expanded → collapsed` transition changes its `width` from (e.g.) 220dp to 56dp. If the caller places `AeroSidebar` inside the left pane of `AeroSplitPane`, the SplitPane measures the left pane's desired width, caches the divider offset in `dp`, and does not participate in the sidebar's animation. The sidebar animates its content width but the `SplitPane` divider stays fixed, causing the sidebar content to clip, or the SplitPane to fight the sidebar's `animateFloatAsState` with its own static constraints.

**Root cause:** `AeroSplitPane` will hold divider position in its own state (a `Dp` offset from the leading edge). If the sidebar inside the pane animates its own width independently, the two width signals are not coordinated.

**Prevention:**
- Document clearly in `AeroSidebar` KDoc: do NOT place inside a SplitPane left pane. Sidebar is a top-level layout sibling, not a nested child.
- `AeroSidebar`'s three modes (expanded/collapsed/hidden) produce fixed target widths — the caller should use `AeroSidebar`'s `currentWidth` observable (a state that animates) to drive any adjacent layout. Expose `val currentWidthDp: State<Dp>` from `AeroSidebar`'s state object.
- Provide a usage example in KDoc showing `Row { AeroSidebar(...); SplitPane(...) }` with `sidebarState.currentWidthDp` wired to `AeroSplitPane`'s initial divider position.

**Warning signs:** Demo in showcase places sidebar inside a SplitPane pane → visible clip during collapse animation.

**Phase to address:** AeroSidebar + AeroSplitPane implementation phases. The `currentWidthDp` API contract must be established in `AeroSidebar` first.

---

### PITFALL-12: AeroStepperWizard onValidate called during recomposition

**Components:** AeroStepperWizard
**Failure mode:** Compiles but wrong behavior — form validation runs on every recomposition, not only on "Next" button press, causing stuttering or unexpected state side-effects.

**What goes wrong:**
If `onValidate: () -> Boolean` is called inside the composable body (e.g., `val isValid = onValidate()`) to derive whether the "Next" button is enabled, it is called on every recomposition — including hover events, scroll events, or sibling state changes. If `onValidate` reads ViewModel state, this triggers ViewModel reads on every frame, potentially causing infinite recomposition loops if the validation itself writes back to state.

**Root cause:** Calling side-effectful or expensive functions in composable body.

**Prevention:**
- `onValidate` must only be invoked in response to a user action (Next button click). The button is always rendered; on click, `val valid = onValidate(); if (valid) advanceStep()`.
- "Next" button disabled state should be derived from a `canProceed: Boolean` parameter that the **caller** provides based on their own state — not from calling `onValidate` in the composable body. `onValidate` is a gate, not a live signal.
- Alternatively, expose `onValidate` as a coroutine suspend function to allow async validation (show a loading state while `onValidate` runs).

**Warning signs:** If visual validation feedback (error borders on fields) flickers during hover, `onValidate` is being called outside of user actions.

**Phase to address:** AeroStepperWizard implementation phase.

---

### PITFALL-13: AeroAccordion single-mode state stored in child composable (not lifted)

**Components:** AeroAccordion
**Failure mode:** Compiles but wrong behavior — in `mode = single`, opening section B does not close section A.

**What goes wrong:**
If each accordion section manages its own `expanded: Boolean` internal state (`var expanded by remember { mutableStateOf(false) }`), sections cannot coordinate. Single-mode requires that opening any section closes all others — this is only possible if expansion state is held at the parent (accordion) level, not in individual section composables.

**Root cause:** Per-section internal state is the natural first instinct ("just like AeroDrawer is self-contained") but accordion sections are not independent.

**Prevention:**
- `AeroAccordion` holds `expandedIndex: Int?` (single mode) or `expandedIndices: Set<Int>` (multi mode) in its own state.
- Each section composable receives `expanded: Boolean` and `onToggle: () -> Unit` as parameters.
- The toggle logic lives in `AeroAccordion`: `onToggle = { if (mode == Single) expandedIndex = if (expandedIndex == i) null else i }`.
- The section content slot is a `@Composable () -> Unit` lambda, not an `AeroAccordionSection` class — avoids DSL overhead.

**Warning signs:** In the showcase demo, clicking two section headers in quick succession leaves both open in single mode.

**Phase to address:** AeroAccordion implementation phase. State architecture must be decided before writing any section composable.

---

### PITFALL-14: AeroSplitPane divider drag escaping pane bounds (no clamp)

**Components:** AeroSplitPane
**Failure mode:** Compiles but wrong behavior — divider can be dragged beyond the edges, collapsing one pane to zero or negative width/height.

**What goes wrong:**
Divider position is stored as a pixel offset. If no minimum pane size is enforced, the divider can be dragged to position 0 or to `totalSize`, making one pane invisible. The opposite pane then fills all space. This is visually broken and may trigger `0dp` layout issues in the content composable inside the collapsed pane.

**Root cause:** `pointerInput` drag delta accumulation without clamping.

**Prevention:**
- Clamp divider position: `dividerPx = (dividerPx + delta).coerceIn(minPaneSize.toPx(), totalSize - minPaneSize.toPx())`.
- Expose `minFirstPaneSize: Dp = 48.dp` and `minSecondPaneSize: Dp = 48.dp` as parameters.
- Use `onPreviewKeyEvent` on the divider to support arrow key nudging (4dp per keypress) — desktop users expect keyboard-accessible resize.

**Warning signs:** During showcase testing, dragging divider all the way to one edge and then the content composable inside the zero-width pane throws a layout exception.

**Phase to address:** AeroSplitPane implementation phase.

---

### PITFALL-15: ColorPicker HSV ↔ RGB round-trip drift

**Components:** AeroColorPicker
**Failure mode:** Compiles and appears correct — but after moving HSV sliders and reading back RGB, the HEX value drifts by 1–2 units per round-trip.

**What goes wrong:**
HSV → RGB conversion uses floating point; `(hue * 255 / 360).roundToInt()` introduces rounding error. If the HEX input then feeds back RGB → HSV, the hue drifts. After several round-trips (drag HSV → read HEX → parse HEX → set RGB → convert to HSV), the internal state drifts from the user's intended value.

**Root cause:** Multiple lossless floating-point conversions with integer clamping at each step.

**Prevention:**
- Maintain a **single source of truth**: internal state is always `(hue: Float, saturation: Float, value: Float, alpha: Float)` in the `[0f, 1f]` range.
- RGB sliders and HEX input are derived views: when RGB slider moves, convert RGB → HSV immediately and store the HSV result. Never store both HSV and RGB simultaneously.
- HEX input: on commit (Enter / focus lost), parse HEX to RGB, convert RGB → HSV, store. On display, derive HEX from current HSV. Do NOT keep HEX as a separate state field — it drifts.
- Use `kotlin.math` functions (`floor`, not `round`) for HSV sector computation to match standard HSV formulas.

**Warning signs:** Start at `#FF0000` (red), drag the saturation slider slightly, then type `#FF0000` back in — if HSV values read differently than the initial state, drift is present.

**Phase to address:** AeroColorPicker implementation phase. HSV ↔ RGB math should be written as a pure-function utility with unit tests before the UI is built.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Use `AeroScrollArea` for DataTable | Instant scrollbar styling | All rows rendered, virtualization gone, crashes on infinite constraints | Never |
| Store selection as `Set<Int>` for DataTable | Simple index math | Indices become stale after sort; wrong rows highlighted | Never |
| Reuse `AeroDropdownPopup` for calendar popups | Zero new infrastructure | Calendar clipped / mispositioning on wide pickers; width-lock bug | Never |
| Call `onValidate()` in composable body for StepperWizard | Easy to derive button enabled state | Validation fires on every recomposition; potential infinite loop | Never |
| Keep per-section expanded state in AeroAccordion child | Self-contained section composable | Single-mode coordination impossible without prop-drilling or callbacks | Never |
| `detectDragGestures` for HSV/RangeSlider Canvas drag | Standard API | Silent drag failure on Desktop due to touchSlop=18dp | Never for Canvas drag on Desktop |
| HSV + RGB as dual state in ColorPicker | Easier slider binding | Round-trip drift, state divergence after HEX input | Never |
| Divider offset in SplitPane without clamp | Simple delta accumulation | Pane collapses to zero, layout exception from zero-width content | Never |

---

## Integration Gotchas

Integration pitfalls specific to adding v2.0 components to the existing aero-compose-ui codebase.

| Integration Point | Common Mistake | Correct Approach |
|-------------------|----------------|------------------|
| `AeroScrollArea` + lazy lists | Nest `LazyColumn` inside `AeroScrollArea` (v1.0 pattern) | `LazyColumn` with standalone `AeroScrollBar(lazyListState)` via `rememberScrollbarAdapter(LazyListState)` |
| `AeroDropdownPopup` for date pickers | Reuse existing popup infrastructure (same `heightIn(max=320.dp)` cap) | New `Popup(popupPositionProvider = AeroCalendarPositionProvider(...))` with no height cap |
| `AeroSlider` as base for `AeroRangeSlider` | Compose two `AeroSlider` instances on the same track | Custom Canvas track + two independent thumb hitboxes; `AeroSlider` internal state would conflict |
| `AeroDrawer` pattern for `AeroSidebar` | Copy AeroDrawer's `FullWindowPositionProvider` popup approach | `AeroSidebar` is not a modal overlay — it is an in-layout `Box` with animated width, no `Popup` |
| `buttonHover` token for DataTable row selection | Use same `colors.primary.copy(alpha = 0.2f)` as `AeroListItem` | Use `colors.borderSelected.copy(alpha = 0.15f)` for selection to distinguish from `buttonHover` |
| Glass modifiers on popup calendar content | Apply `glassEffect`/`glassSurface` directly to calendar grid cells | Apply glass surface to the calendar popup container; cells use `cardBackground` for individual day cells |
| `AeroIcons.*` for DataTable sort indicator | Use text `▲`/`▼` glyphs (old v1.0 pattern) | `Icon(AeroIcons.CaretUp, tint = ...)` / `Icon(AeroIcons.CaretDown, tint = ...)` — v1.1 rule |
| `AeroIcons.*` for TreeView expand/collapse | Use text `+`/`-` | `Icon(AeroIcons.CaretRight, tint = ...)` rotated 90° when expanded via `graphicsLayer { rotationZ = angle }` |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| DataTable: no `key` in `LazyColumn items()` | Every sort recomposes all visible rows; jank on 200+ rows | `items(items = sortedRows, key = { keyFn(it) })` | Visible at 100+ rows |
| ColorPicker: recompose entire picker on every drag event | Frame drops during HSV drag; CPU spike visible in profiler | Isolate HSV state in `derivedStateOf`; canvas redraws only when HSV changes | Immediate on drag |
| TreeView: non-lazy child rendering | All children of expanded nodes rendered at once even if offscreen | Wrap entire tree in `LazyColumn`; each node is a `LazyColumn` item | Visible at 50+ nodes |
| DataTable: measuring all column widths on every recomposition | Jank on column count > 8 | Compute column widths in `remember(columns, data) {}` block; skip on row recomposition | 8+ columns with variable data |
| SplitPane: `SubcomposeLayout` for dynamic pane measurement | Sub-compose overhead on every drag event | Use `BoxWithConstraints` for initial measurement; store divider in `Dp` state; recalculate only on divider drag | Constant on every drag if SubcomposeLayout used naively |
| AeroAccordion: animating max-height for expand/collapse | `animateIntAsState(maxHeight)` requires measuring actual content height, causing multiple layout passes | Use `animateFloatAsState` on `scaleY` + `clip(RectangleShape)` or `animateContentSize()` from Foundation | Visible as layout jank on slow expand |

---

## Win11 / Undecorated Window Pitfalls

### W11-01: No `transparent=true` — the locked rule applies to Dialogs and child Windows too

**Components:** AeroDatePicker, AeroColorPicker (if implemented as Dialog instead of Popup)
**Failure mode:** Won't run — `EXCEPTION_ACCESS_VIOLATION` crash on Window creation.

**What goes wrong:**
The rule `undecorated=true` BEZ `transparent=true` is locked for the main window (Win11 issue #3757). However, if any v2.0 component is naively implemented as a `Dialog(undecorated=true, transparent=true)` (seeking a non-standard transparent container), the same crash occurs.

**Prevention:**
- All popup calendar, color picker, and time picker overlays use `Popup(...)`, NOT `Dialog(...)` or a new `Window(...)`.
- The `AeroDrawer` pattern (full-window `Popup` with `FullWindowPositionProvider`) is the approved approach for modal overlays — use the same `Popup` + scrim pattern if modal behavior is needed.
- No v2.0 component creates a new `Window` or a `Dialog` with `undecorated=true` or `transparent=true`.
- This rule must appear in the KDoc of every overlay composable: `// Do not use Dialog(transparent=true) — Win11 EXCEPTION_ACCESS_VIOLATION (CMP issue #3757)`.

**Phase to address:** Every phase that implements a popup-bearing component. Pre-flight checklist item for each phase.

---

### W11-02: Popup shadow rendering on undecorated window

**Components:** AeroDatePicker, AeroColorPicker, any calendar popup
**Failure mode:** Subtle UX issue — `Modifier.shadow(elevation = 8.dp)` on a `Popup` content box renders with a hard edge or no shadow on Win11 when the main window is `undecorated=true` without `transparent=true`.

**What goes wrong:**
On Win11, `Popup` composables render in their own skiko layer. Without `transparent=true` on the host window, the shadow drawn by `Modifier.shadow` clips at the popup's bounding box — the shadow has no surface to render on outside the box. The result is a sharp edge with no visual depth despite the `elevation` setting.

**Root cause:** Compose Desktop `Popup` creates a child AWT window for the popup content. Without the host being transparent, the shadow alpha blend has no compositing target outside the popup window bounds.

**Prevention:**
- Use the existing `AeroDropdownPopup` technique: simulate shadow via `border + glassBorder` + a slightly darker background, rather than `Modifier.shadow`. This is already the pattern in `AeroDropdownPopup` (which uses `.shadow(elevation = 8.dp, shape = shape)` before `.background`). Verify this renders acceptably during visual checkpoint.
- For wider calendar popups, add an explicit `panelBackground` fill layer (two `background` calls: `colors.background` fully opaque first, then `colors.panelBackground` on top) so the popup has an opaque backdrop that won't show through. This is already the established two-layer technique from `AeroDropdownPopup` and `AeroComboBox`.
- If shadow is truly required, use a custom `drawBehind` shadow simulation (draw blurred rect manually) rather than `Modifier.shadow`.

**Phase to address:** Date/time pickers visual checkpoint plan.

---

## "Looks Done But Isn't" Checklist

- [ ] **AeroDataTable virtualization:** Verify row count above the fold does NOT equal total row count when data > 50 rows. If all rows are mounted, `LazyColumn` is not actually lazy.
- [ ] **AeroDataTable selection after sort:** Select row with key X, sort descending, verify row with key X is still highlighted at its new position.
- [ ] **AeroDataTable column resize:** Drag a column splitter past the available width — verify other columns reflow and no column goes to zero width.
- [ ] **AeroTreeView lazy callback:** Open a node, scroll it off screen, scroll back — verify `onExpand` is NOT called again.
- [ ] **AeroDatePicker popup position:** Place the trigger field at the right edge of a 1024dp window — verify calendar does not clip.
- [ ] **AeroDateRangePicker partial state:** Click only start date, close without selecting end — verify `onRangeSelect` was NOT called.
- [ ] **AeroColorPicker round-trip:** Set to `#FF0000`, drag saturation to 50%, drag back to 100% — verify HEX still reads `#FF0000` (no drift).
- [ ] **AeroRangeSlider thumb overlap:** Drag start thumb to equal end thumb — verify thumbs do not permanently merge (one becomes unreachable).
- [ ] **AeroAccordion single mode:** Open section B while section A is open — verify A closes.
- [ ] **AeroSplitPane clamp:** Drag divider to far edge — verify pane does not collapse to zero.
- [ ] **AeroSidebar + adjacent layout:** Collapse sidebar — verify adjacent content reflows to use reclaimed space.
- [ ] **AeroStepperWizard validation:** Tab through fields without clicking Next — verify `onValidate` is NOT called during focus movement.
- [ ] **All pickers: AeroDark disabled cells:** Verify disabled date cells are readable (not invisible) in AeroDark theme.
- [ ] **All drag components: Desktop drag response:** Verify HSV square, RangeSlider thumbs, and DataTable column splitters all respond on first mouse movement (no slop delay). If drag requires a ~18px movement before activating, `detectDragGestures` touchSlop pitfall is present.
- [ ] **No `transparent=true`:** Grep `transparent = true` in all new v2.0 files — must be zero results.
- [ ] **No `AeroScrollArea` wrapping LazyColumn:** Grep for `AeroScrollArea` inside any DataTable or TreeView file — must be zero results.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| AeroScrollArea wrapping LazyColumn discovered during execute | MEDIUM | Remove AeroScrollArea; wire LazyListState to AeroScrollBar; add height constraint to DataTable outer box. One plan's work. |
| Selection using index instead of key discovered after DataTable ships | HIGH | API breaking change — `selectedKeys: Set<Any>` replaces `selectedIndices: Set<Int>`; callers must update. Avoid by fixing in design phase. |
| `detectDragGestures` touchSlop discovered during ColorPicker testing | LOW | Replace with `awaitPointerEventScope` custom loop. 1–2 hours per affected component. Non-breaking API change. |
| Calendar popup clipping discovered during visual checkpoint | LOW | Write `AeroCalendarPositionProvider`, swap position provider in `Popup()` call. No API change needed. |
| HSV drift discovered after ColorPicker ships | MEDIUM | Requires internal refactor of state model (single source of truth). API surface unchanged but behavior changes. |
| Accordion single-mode not working discovered during showcase | LOW | State lift from section composable to accordion parent. Internal-only change. |
| `transparent=true` crash discovered on Win11 test machine | HIGH | Must remove `transparent=true` from any overlay/dialog; may require rearchitecting the overlay approach. |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| PITFALL-01: LazyColumn in AeroScrollArea | DataTable design/API phase | Grep for `AeroScrollArea` in DataTable; row count check with 100-row dataset |
| PITFALL-02: Calendar popup width mismatch | Pickers design phase — write `AeroCalendarPositionProvider` first | Visual checkpoint: trigger near right edge of 1024dp window |
| PITFALL-03: touchSlop drag failure | First drag-component phase (DataTable column resize or ColorPicker, whichever is first) | Drag test: 1px mouse move must register |
| PITFALL-04: Selection lost on sort | DataTable design/API phase | Automated test: select by key, sort, assert key still selected |
| PITFALL-05: TreeView lazy callback repeat | TreeView design/API phase | Log `onExpand` calls; scroll node off-screen and back |
| PITFALL-06: DateRangePicker partial state leak | Pickers design/API phase | Assert `onRangeSelect` call count == 1 per complete range selection |
| PITFALL-07: RangeSlider thumb overlap | RangeSlider implementation phase | Drag thumbs to same position; verify both remain draggable |
| PITFALL-08: Calendar popup first-frame flash | Pickers implementation phase | Record visual: popup must not jump on frame 1 |
| PITFALL-09: AeroDark disabled cells | Pickers visual checkpoint phase | Three-theme visual sign-off checklist item |
| PITFALL-10: Selection vs hover token conflict | DataTable visual checkpoint phase | Four-state hover/select demo in showcase; AeroDark verification |
| PITFALL-11: Sidebar + SplitPane layout conflict | Sidebar implementation phase | KDoc example; showcase demo keeps Sidebar as top-level sibling |
| PITFALL-12: onValidate in composable body | StepperWizard design/API phase | Log `onValidate` calls; verify zero calls during hover events |
| PITFALL-13: Accordion state not lifted | Accordion design phase (before implementation) | Single-mode demo: click two headers, verify only one stays open |
| PITFALL-14: SplitPane no clamp | SplitPane implementation phase | Drag divider to edge; verify 48dp minimum pane size enforced |
| PITFALL-15: ColorPicker HSV drift | ColorPicker math utility phase | Unit test: set #FF0000, convert HSV→RGB→HSV, assert no drift |
| W11-01: transparent=true on Dialog | Every overlay-bearing phase | Pre-flight grep: `transparent = true` must be zero results |
| W11-02: Shadow clipping on undecorated | Pickers visual checkpoint phase | Visual check: popup border/depth visible without `Modifier.shadow` |

---

## Sources

- Actual `AeroScrollArea.kt` source (confirms `Column + verticalScroll` pattern; root cause of v1.0 AeroDropdown popup regression documented in `.planning/MILESTONES.md`)
- Actual `AeroDropdownPopup.kt` source (confirms `widthIn(min = anchorWidth, max = anchorWidth)` width-lock; `heightIn(max = 320.dp)` cap)
- Actual `AeroPopupPositionProvider.kt` source (confirms `unmeasured` guard logic; `overflows` checks both axes indiscriminately)
- Actual `AeroColorScheme.kt` source (exact alpha values for all three themes; basis for PITFALL-09 and PITFALL-10 contrast analysis)
- Actual `AeroSlider.kt` source (confirms single `MutableInteractionSource`; basis for PITFALL-07 "don't compose two AeroSliders")
- Actual `AeroDrawer.kt` source (confirms `FullWindowPositionProvider` pattern; approved modal overlay technique for Win11)
- Actual `AeroListItem.kt` source (confirms `primary.copy(alpha = 0.2f)` selection pattern; basis for PITFALL-10)
- `.planning/MILESTONES.md` — v1.1 shipped lessons: AeroNumberSpinner sub-pixel pitfall, AeroDropdown popup-offset root cause, wave ordering lessons
- `.planning/RETROSPECTIVE.md` — Key lesson 1: "compute pixel-stroke risks at planning time, not at execution"
- `.planning/PROJECT.md` — v2.0 locked decisions, `undecorated=true` BEZ `transparent=true` rule, AeroDropdown popup offset root cause
- JetBrains/compose-multiplatform issue #3757 — Win11 `undecorated+transparent` EXCEPTION_ACCESS_VIOLATION (confirmed open)
- JetBrains/compose-jb issue #343 — `detectDragGestures` unusable on Desktop due to `DesktopViewConfiguration.touchSlop = 18` (MEDIUM confidence — confirmed in search results, known issue)
- JetBrains/compose-multiplatform issue #3333 — LazyColumn cannot auto-scroll in SelectionContainer (related: LazyColumn in scrollable parent constraints)

---
*Pitfalls research for: aero-compose-ui v2.0 Stateful + Layout components*
*Researched: 2026-04-30*
