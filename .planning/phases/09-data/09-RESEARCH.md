# Phase 9: Data (AeroDataTable + AeroTreeView) — Research

**Researched:** 2026-06-18
**Domain:** Compose Desktop virtualized table + lazy-expand tree; CMP 1.7.3 / Kotlin 2.1.21
**Confidence:** HIGH — all locked decisions confirmed; open question resolved with 2 sources

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- Column cell = composable slot `cell: @Composable (T) -> Unit` + `textColumn(...)` convenience helper
- Column width = sealed `AeroColumnWidth` { Fixed(Dp) | Weight(Float) } + `minWidth: Dp = 40.dp`
- Columns = `columns: List<AeroTableColumn<T>>` (plain data class, no DSL)
- Sort = hybrid: uncontrolled by default via `sortKey: ((T) -> Comparable<*>)?`; controlled if `onSortChange` supplied; asc → desc → none
- Selection = controlled `selectedKeys: Set<Any>` + `onSelectionChange`; Ctrl/Shift logic internal; Shift-anchor = private internal `remember`; `selectionMode = none | single | multi`
- Tree model = functions: `rootNodes: List<T>` + `children: (T) -> List<T>` + `isExpandable: (T) -> Boolean` + `key: (T) -> Any`; expand state in tree-level SnapshotStateMap; `onExpand: (T)->Unit` fires exactly once per node
- Fixed `rowHeight: Dp = 36.dp`; empty state slot + default
- Glass header + thin dividers (Win7 Aero aesthetic, not generic-flat)
- Horizontal scroll: header Row and data LazyColumn SHARE one ScrollState via `horizontalScroll`; NO `stickyHeader` (JetBrains bugs #3016, #2940)
- Build order recommendation: DataTable → TreeView

### Claude's Discretion

- Exact name/signature of `textColumn(...)` convenience helper
- Optional `initialSortColumn`/`initialSortDirection` parameter
- Exact `indentPerLevel`, paddings, glass-header tokens, divider alpha, hit-area width for drag-splitter
- `onRowClick` for `selectionMode = none`
- Wave split granularity (DataTable first, then TreeView)
- Default empty-state text and typography

### Deferred Ideas (OUT OF SCOPE)

- Cell editing (DATA-EDIT-01)
- Column reordering (DATA-REORDER-01)
- Per-column filter UI (DATA-FILTER-01)
- TreeView drag-and-drop (TREE-DND-01)
- TreeView built-in single/multi-select
- DataTable density-enum
- Sticky / frozen first column
- Showcase wiring (DataSection) — Phase 11
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DATA-01 | AeroDataTable renders tabular data with column headers and row virtualization (LazyColumn); thousands of rows without fps drop | LazyColumn + LazyListState + AeroScrollBar(LazyListState overload) confirmed; PITFALL-01 defused by design |
| DATA-02 | Column header click sorts asc→desc→none; only active column shows CaretUp/CaretDown | `animateFloatAsState` + `graphicsLayer { rotationZ }` confirmed idiomatic; `AeroIcons.{CaretUp,CaretDown}` |
| DATA-03 | Selection: none/single/multi; Ctrl-click toggle, Shift-click range; stored as Set<RowKey> with caller key fn | `Modifier.onClick(keyboardModifiers = { isCtrlPressed })` confirmed; PITFALL-04 defused by API design |
| DATA-04 | Resizable columns via drag-splitter; minimum width 40dp; no column collapses to 0 | `Modifier.aeroDragSplitter(Orientation.Horizontal, ...)` confirmed present and correct |
| DATA-05 | AeroTreeView renders hierarchy; expand/collapse with CaretRight/CaretDown indicator; animates | Flatten-to-LazyList pattern confirmed; `animateFloatAsState + graphicsLayer` for caret; `AnimatedVisibility` for children |
| DATA-06 | Lazy children via onExpand; fires exactly once per node regardless of scroll | `SnapshotStateMap` at tree level (above LazyColumn) confirmed as the guard mechanism; PITFALL-05 defused |
</phase_requirements>

---

## Summary

Phase 9 is an **implementation-phase** research task — the architecture, API surface, and all major technical decisions were locked during the v2.0 milestone research. This research's job was to:
1. Verify the `rememberScrollbarAdapter(LazyListState)` API in CMP 1.7.3 (STATE.md open question)
2. Confirm or caveat each locked decision with exact API detail the executor needs
3. Answer 6 specific implementation questions the CONTEXT.md surfaced

**ALL locked decisions are confirmed technically feasible.** Two caveats require action: (a) `AeroScrollBar` currently accepts only `ScrollState` and needs a new `LazyListState`-based overload or the DataTable/TreeView must use `VerticalScrollbar` directly with `rememberScrollbarAdapter(lazyListState)`; (b) `Modifier.onClick(keyboardModifiers)` is `@ExperimentalFoundationApi` and requires an opt-in annotation at call site.

**Primary recommendation:** Build DataTable first (plan-01 = API+state design, plan-02 = LazyColumn virtualization + header, plan-03 = sort + selection, plan-04 = column resize); TreeView second (plan-05 = flatten+expand+animation); verify all six requirements against the checklist at end of each wave.

---

## Standard Stack

### Core (all already on classpath — no new dependencies)

| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| `androidx.compose.foundation.lazy.LazyColumn` | CMP 1.7.3 | Virtualized row/node rendering | `items(key = keyFn)` mandatory |
| `androidx.compose.foundation.lazy.rememberLazyListState` | CMP 1.7.3 | LazyListState for scrollbar adapter | Pass to both LazyColumn and adapter |
| `androidx.compose.foundation.rememberScrollbarAdapter(LazyListState)` | CMP 1.7.3 | Scrollbar adapter for LazyListState | **See "Scrollbar Adapter" section** |
| `androidx.compose.foundation.VerticalScrollbar` | CMP 1.7.3 | Native-desktop styled scrollbar | Paired with `rememberScrollbarAdapter(lazyListState)` |
| `androidx.compose.foundation.ScrollState` + `Modifier.horizontalScroll` | CMP 1.7.3 | Shared horizontal scroll (header + body) | One state shared between header Row and each data row |
| `androidx.compose.foundation.gestures.Orientation` | CMP 1.7.3 | Required by `aeroDragSplitter` | Already imported in AeroDragSplitter.kt |
| `androidx.compose.animation.animateFloatAsState` | CMP 1.7.3 | Caret rotation animation | Sort indicator + tree expand caret |
| `androidx.compose.animation.AnimatedVisibility` + `expandVertically` | CMP 1.7.3 | Tree node child expand animation | See PITFALL-NEW-01 for LazyColumn gotcha |
| `androidx.compose.runtime.snapshots.SnapshotStateMap` | CMP 1.7.3 | Tree expand + childrenLoaded state (tree-level) | Must live above LazyColumn |
| `androidx.compose.foundation.onClick` | CMP 1.7.3 | Modifier-based click with keyboard modifier detection | `@ExperimentalFoundationApi` — requires opt-in |
| `com.mordred.aero.components.internal.drag.aeroDragSplitter` | Phase 7 | Column resize drag (PITFALL-03 defused) | Internal, already in codebase |
| `com.mordred.aero.theme.GlassModifiers.glassPanel` | v1.0 | Glass-gradient table header | `glassPanel(cornerRadius = 0.dp)` for full-width strip |
| `com.mordred.aero.icons.AeroIcons` | v1.1 | `CaretUp`/`CaretDown` sort; `CaretRight` tree expand | Tint always explicit |

### Version Verification

`rememberScrollbarAdapter(LazyListState)` is present in CMP 1.7.3:
- Source: `androidx.compose.foundation.Scrollbar.skiko.kt` in `compose-multiplatform-core` (jb-main branch)
- Source: Official JetBrains Compose Desktop scrollbar docs (updated May 2025) showing `rememberScrollbarAdapter(scrollState = state)` where `state = rememberLazyListState()`

The old overload returning `ScrollbarAdapter` (v1) is `@Deprecated`. The current overload returns `androidx.compose.foundation.v2.ScrollbarAdapter`. **Both still compile** — the deprecation produces a warning, not an error. The `VerticalScrollbar` composable has overloads for both types.

---

## Architecture Patterns

### Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
└── components/
    └── datatable/
        ├── AeroDataTable.kt          # Public — table with virtualized rows, sort, selection, resize
        ├── AeroTreeView.kt           # Public — lazy hierarchical tree with expand/collapse
        ├── AeroTableColumn.kt        # Public — sealed AeroColumnWidth + AeroTableColumn<T> data class
        └── internal/
            ├── AeroTableHeader.kt    # Internal — glass header row with sort carets + drag splitters
            ├── AeroTableRow.kt       # Internal — selectable row composable (four-state color)
            └── AeroTreeNode.kt       # Internal — single tree node (indent, caret, nodeContent slot)
```

### Pattern 1: Scrollbar Adapter Wiring (HIGHEST-PRIORITY OPEN QUESTION — RESOLVED)

**Question:** Does `rememberScrollbarAdapter(LazyListState)` exist in CMP 1.7.3?

**Answer: YES.** ✅

**Exact signature (current, non-deprecated):**
```kotlin
// Package: androidx.compose.foundation
// Returns: androidx.compose.foundation.v2.ScrollbarAdapter
@Composable
fun rememberScrollbarAdapter(
    scrollState: LazyListState
): androidx.compose.foundation.v2.ScrollbarAdapter
```

**Deprecation status:** The old overload returning `ScrollbarAdapter` (v1) is deprecated. The `@Deprecated` annotation redirects to the same-named function returning `v2.ScrollbarAdapter`. Both compile in CMP 1.7.3 — no hard break, only a warning.

**Critical finding: `AeroScrollBar` currently only accepts `ScrollState`.**
```kotlin
// CURRENT (AeroScrollBar.kt line 25):
public fun AeroScrollBar(scrollState: ScrollState, modifier: Modifier = Modifier)
```
This signature **cannot** be used by DataTable/TreeView because those need `LazyListState`. 

**Two options for Phase 9:**

Option A — Add new overload to `AeroScrollBar` (preferred, additive, v2.0 additive rule):
```kotlin
@Composable
public fun AeroScrollBar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(lazyListState),
        modifier = modifier
    )
}
```

Option B — Use `VerticalScrollbar` directly inside DataTable/TreeView internal code (no public API change):
```kotlin
// Inside AeroDataTable internal layout:
VerticalScrollbar(
    adapter = rememberScrollbarAdapter(lazyListState),
    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
)
```

**Recommendation: Option A** — adds a public overload of the existing `AeroScrollBar` API, consistent with the v2.0 "additive only" rule, and gives consumers a consistent way to attach the library's styled scrollbar to their own `LazyColumn` lists.

**Fixed rowHeight = 36.dp note:** The v2 `rememberScrollbarAdapter(LazyListState)` does NOT require `itemCount` or `averageItemSize` parameters (those were removed from the API in the v2 redesign per JetBrains/compose-jb #181). The adapter reads `LazyListState` directly. Fixed item heights improve scrollbar accuracy but are not required by the API.

**Wiring pattern:**
```kotlin
// Source: JetBrains Compose Desktop official scrollbar docs (kotlinlang.org, verified 2026-06-18)
@Composable
fun AeroDataTable(/* params */) {
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column {
            // Header row — shares horizontalScrollState
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(cornerRadius = 0.dp)
                    .horizontalScroll(horizontalScrollState, enabled = false) // driven by body
            ) {
                // header cells with aeroDragSplitter
            }
            // Data rows — each row shares horizontalScrollState
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp) // space for scrollbar
            ) {
                items(items = sortedData, key = { keyFn(it) }) { item ->
                    AeroTableRow(
                        item = item,
                        columns = columns,
                        columnWidths = columnWidths,
                        horizontalScrollState = horizontalScrollState,
                        /* ... */
                    )
                }
            }
        }
        // Vertical scrollbar on LazyListState
        AeroScrollBar(
            lazyListState = lazyListState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
```

**Sources:**
- JetBrains official Compose Desktop scrollbar docs (kotlinlang.org/docs/multiplatform/compose-desktop-scrollbars.html) — HIGH confidence
- compose-multiplatform-core/Scrollbar.skiko.kt (jb-main branch) — HIGH confidence

---

### Pattern 2: Shared Horizontal ScrollState (Header + Body)

**Question:** How to share one horizontal `ScrollState` between a header `Row` and the data `LazyColumn`?

**Answer:** ✅ Standard pattern — confirmed idiomatic.

Each LazyColumn item row applies `Modifier.horizontalScroll(sharedState)`. The header `Row` applies `Modifier.horizontalScroll(sharedState, enabled = false)` — the `enabled = false` prevents the header from accepting scroll gestures directly (user scrolls via the body rows; the header follows). The column widths are computed in a `remember(columns)` block once and accessed by both header and rows.

```kotlin
// Shared state — created at DataTable composition level
val horizontalScrollState = rememberScrollState()

// Header (outside LazyColumn)
Row(
    Modifier
        .fillMaxWidth()
        .horizontalScroll(horizontalScrollState, enabled = false)
) {
    columnWidths.forEachIndexed { i, width ->
        AeroTableHeaderCell(
            column = columns[i],
            width = width,
            sortState = sortState,
            onSortClick = { /* ... */ },
            onResizeDrag = { delta -> /* update columnWidths[i] */ }
        )
    }
}

// LazyColumn body — each row scrolls the shared state
LazyColumn(state = lazyListState) {
    items(sortedData, key = { keyFn(it) }) { item ->
        Row(
            Modifier.horizontalScroll(horizontalScrollState) // same state, enabled = true (default)
        ) {
            columnWidths.forEachIndexed { i, width ->
                Box(Modifier.width(width).height(rowHeight)) {
                    columns[i].cell(item)
                }
            }
        }
    }
}
```

**Gotcha — column width computation:** Column widths (`List<Dp>` from `Fixed(dp)` + weight-expanded) MUST be computed in `remember(columns, availableWidth)`, NOT inside each row recomposition. The `remember` key should include `availableWidth` (from `BoxWithConstraints` or `onGloballyPositioned`) so resize-triggered recomposition recomputes widths once, not per-row.

```kotlin
// Correct: computed once per column list + available width change
val columnWidths: List<Dp> = remember(columns, totalWidthPx) {
    resolveColumnWidths(columns, totalWidthPx, density)
}
```

**Source:** PITFALLS.md Performance Traps ("DataTable: measuring all column widths on every recomposition"), cross-confirmed with Compose `remember()` best practices docs. — HIGH confidence.

---

### Pattern 3: Ctrl/Shift Click Detection for Multi-Selection

**Question:** What is the current non-deprecated API for reading keyboard modifiers at click time in CMP 1.7.3 Desktop?

**Answer:** ✅ `Modifier.onClick(keyboardModifiers)` from `androidx.compose.foundation` — confirmed with exact imports.

**API details:**
- Import: `androidx.compose.foundation.onClick`
- Annotation: `@OptIn(ExperimentalFoundationApi::class)` — the API is experimental but has been stable in practice since CMP 1.2; no deprecation flag, no planned removal in 1.7.x.
- The `keyboardModifiers` parameter is a lambda `PointerKeyboardModifiers.() -> Boolean`
- Properties: `isCtrlPressed`, `isShiftPressed`, `isAltPressed`, `isMetaPressed`

```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-mouse-events.html
// Verified 2026-06-18 against official JetBrains docs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onClick
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AeroTableRow(
    item: T,
    isSelected: Boolean,
    onSingleClick: () -> Unit,
    onCtrlClick: () -> Unit,
    onShiftClick: () -> Unit,
    /* ... */
) {
    Row(
        modifier = Modifier
            // IMPORTANT: order matters — keyboardModifiers handlers BEFORE plain handler
            .onClick(keyboardModifiers = { isCtrlPressed }) { onCtrlClick() }
            .onClick(keyboardModifiers = { isShiftPressed }) { onShiftClick() }
            .onClick { onSingleClick() }
            // ... background, height, etc.
    ) { /* cells */ }
}
```

**Alternative: `LocalWindowInfo.current.keyboardModifiers`** — usable inside `pointerInput` / `awaitPointerEventScope`. This is the stable (non-experimental) path. However `Modifier.onClick` is cleaner for click-only detection. For DataTable rows, where we need to compose the click handler as a Composable tree, `Modifier.onClick` is preferred.

**Shift-anchor implementation note:** The Shift-anchor (last-selected row) is `private val shiftAnchorKey by remember { mutableStateOf<Any?>(null) }` inside the DataTable composable. On Shift-click at row with `key = K`, the range is computed from the current displayed sorted list between `shiftAnchorKey` and `K` (inclusive). The anchor updates on every single-click and Ctrl-click-that-adds. It does NOT update on Shift-click itself.

---

### Pattern 4: Caret Rotation via `graphicsLayer { rotationZ }`

**Question:** Is `animateFloatAsState` + `graphicsLayer { rotationZ }` idiomatic and free of `items(key=)` gotchas?

**Answer:** ✅ Confirmed idiomatic. No special gotchas with `items(key=)`.

```kotlin
// Sort indicator in table header
val rotation by animateFloatAsState(
    targetValue = when (sortDirection) {
        SortDirection.Asc -> 0f    // CaretUp icon default orientation
        SortDirection.Desc -> 180f  // CaretUp rotated = pointing down
        SortDirection.None -> 0f
    },
    animationSpec = tween(durationMillis = 100),
    label = "sortCaret"
)
Icon(
    imageVector = AeroIcons.CaretUp,
    contentDescription = null,
    tint = colors.onSurface,
    modifier = Modifier
        .graphicsLayer { rotationZ = rotation }
        .alpha(if (sortDirection == SortDirection.None) 0f else 1f)
)

// Tree expand caret
val caretRotation by animateFloatAsState(
    targetValue = if (isExpanded) 90f else 0f,  // CaretRight → 90° = pointing down
    animationSpec = tween(durationMillis = 150),
    label = "treeCaret"
)
Icon(
    imageVector = AeroIcons.CaretRight,
    contentDescription = null,
    tint = colors.onSurface,
    modifier = Modifier.graphicsLayer { rotationZ = caretRotation }
)
```

`items(key = keyFn)` in LazyColumn means Compose associates the composable instance with the key, not the position. Animation state (`animateFloatAsState`) is remembered per-key — when the user expands a node at position 5 and then scrolls so a different node occupies position 5, the animation state correctly stays with the original node (via the key). **This is why `items(key=)` is mandatory** — without it, animation state leaks between items on scroll. — HIGH confidence.

---

### Pattern 5: Tree Flattening for LazyColumn

**Question:** Standard pattern for rendering a hierarchy inside a single LazyColumn?

**Answer:** ✅ Flatten to `List<FlatNode<T>>` before rendering. Recompute when expand map changes.

```kotlin
// FlatNode carries depth so rows can indent correctly
data class FlatNode<T>(
    val item: T,
    val depth: Int,
    val key: Any,
    val isExpandable: Boolean,
    val isExpanded: Boolean
)

// Flatten function — pure, called in remember(expandedKeys, rootNodes)
internal fun <T> flattenTree(
    nodes: List<T>,
    depth: Int = 0,
    expandedKeys: Set<Any>,
    childrenFn: (T) -> List<T>,
    isExpandableFn: (T) -> Boolean,
    keyFn: (T) -> Any
): List<FlatNode<T>> = buildList {
    for (node in nodes) {
        val k = keyFn(node)
        val expanded = k in expandedKeys
        add(FlatNode(node, depth, k, isExpandableFn(node), expanded))
        if (expanded) {
            addAll(flattenTree(childrenFn(node), depth + 1, expandedKeys, childrenFn, isExpandableFn, keyFn))
        }
    }
}

// In AeroTreeView composable:
val expandedKeys: Set<Any> = expandStateMap.keys.filter { expandStateMap[it]?.isExpanded == true }.toSet()
val flatNodes: List<FlatNode<T>> = remember(expandedKeys, rootNodes) {
    flattenTree(rootNodes, 0, expandedKeys, children, isExpandable, key)
}

LazyColumn(state = lazyListState) {
    items(flatNodes, key = { it.key }) { flatNode ->
        AeroTreeNode(
            flatNode = flatNode,
            onExpand = { /* update expandStateMap + fire onExpand once */ },
            nodeContent = nodeContent,
            indentPerLevel = 16.dp,
        )
    }
}
```

`items(key = { it.key })` is **mandatory** — without it, expand/collapse causes every visible item to recompose unnecessarily. With it, Compose reuses composables for unchanged nodes and only recomposes the changed subtree.

**Source:** PITFALLS.md Performance Traps; ComposeTree library design observation (m-sasha/ComposeTree). — HIGH confidence.

---

### Pattern 6: `AnimatedVisibility`/`expandVertically` for Tree Node Children

**Question:** Any virtualization gotcha with expand animation inside LazyColumn items?

**Answer:** ⚠️ Caution required — `AnimatedVisibility` / `animateContentSize` inside a LazyColumn item can cause height measurement instability.

**The fundamental issue:** LazyColumn needs to know item height at layout time. If a LazyColumn item contains an `AnimatedVisibility` that expands, the item height changes during the animation. Compose handles this with `animateItemPlacement` for position changes, but the expanding item itself may cause a brief layout jank as neighbors jump.

**Recommended approach for AeroTreeView:** The tree uses the *flatten-and-replace* pattern (Pattern 5), which avoids this problem entirely:
- When a node expands, the flat list grows (child items are added after the parent item).
- When a node collapses, child items are removed from the flat list.
- LazyColumn's `animateItem()` (in newer CMP) or just the key-based recomposition handles the insertion/removal.
- **No `AnimatedVisibility` wrapping children inside a single item** — children are separate LazyColumn items.

The caret rotation animation (`graphicsLayer { rotationZ }`) is on the icon inside the node row, which has fixed height = `rowHeight`. No height change, no measurement issue.

**For the expand indicator animation only (caret):** `animateFloatAsState` on `rotationZ` — confirmed safe, zero measurement impact.

**If animate-content-size is desired for the node row itself:** Don't. Fixed `rowHeight: Dp = 36.dp` is locked; the node row height does not change on expand. Only the flat list changes length.

**Source:** Google Issue Tracker #165921895 (AnimatedVisibility in LazyColumn height instability); Compose docs on `animateItem()`. — MEDIUM confidence (cannot read the issue tracker directly; cross-confirmed from community knowledge and the locked fixed-rowHeight design which sidesteps the problem).

---

### Pattern 7: DataTable Four-State Selection Color (PITFALL-10)

```kotlin
// In AeroTableRow:
val isHovered by interactionSource.collectIsHoveredAsState()

val rowBackground = remember(isSelected, isHovered) {
    when {
        isSelected && isHovered ->
            colors.borderSelected.copy(alpha = 0.15f)
                .compositeOver(colors.buttonHover)  // stack: selected + hover overlay
        isSelected -> colors.borderSelected.copy(alpha = 0.15f)
        isHovered -> colors.buttonHover
        else -> Color.Transparent
    }
}
```

**Token mapping (from AeroColorScheme.kt — verified):**

| Theme | `borderSelected` | `buttonHover` |
|-------|-----------------|---------------|
| AeroBlue | `Color(0xFF4FC3F7)` | `Color(0x40FFFFFF)` |
| AeroDark | `Color(0xFF90CAF9)` | `Color(0x30FFFFFF)` |
| Classic | `Color(0xFF5C8ABF)` | `Color(0x30FFFFFF)` |

Selected row bg = `borderSelected.copy(0.15f)` — distinctly blue/teal, not ambiguous with white-hover overlay. Validate in AeroDark where token contrast is lowest.

---

### Anti-Patterns to Avoid

- **`AeroScrollArea` inside DataTable/TreeView** — wraps `Column.verticalScroll`, gives LazyColumn infinite height → all rows render, no virtualization. grep gate: 0 hits. (PITFALL-01)
- **`stickyHeader` for the table header** — causes VerticalScrollbar flicker (JetBrains #3016, #2940). Use external `Row` + shared `ScrollState` instead.
- **`detectDragGestures` for column resize** — touchSlop=18dp silently prevents first-pixel drag response. Use `aeroDragSplitter`. (PITFALL-03)
- **`Set<Int>` for selection** — indices become stale after sort. Use `Set<Any>` with caller `key: (T) -> Any`. (PITFALL-04)
- **`childrenLoaded` inside node composable** — LazyColumn disposes composables on scroll-out; `onExpand` re-fires when node scrolls back in. Keep `SnapshotStateMap` above LazyColumn. (PITFALL-05)
- **`primary.copy(alpha = 0.2f)` for selection** — same as AeroListItem hover color; selection and hover become visually identical in AeroBlue. Use `borderSelected.copy(0.15f)`. (PITFALL-10)
- **Column widths computed per-row** — triggers full recomposition on every drag delta at 8+ columns. Use `remember(columns, availableWidth)`. (Performance Traps section)

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Column resize drag | Custom `detectDragGestures` or raw `pointerInput` loop | `Modifier.aeroDragSplitter(Orientation.Horizontal, ...)` (Phase 7, internal) | touchSlop=18dp would silently break; cursor change included; PITFALL-03 already defused |
| Vertical scrollbar on LazyColumn | Custom scrollbar composable | `AeroScrollBar(lazyListState)` (add new overload) OR `VerticalScrollbar(rememberScrollbarAdapter(lazyListState))` directly | AeroScrollBar already styled via `LocalScrollbarStyle`; no new styling code |
| Selection key stability across sort | `Set<Int>` with index remapping after sort | `Set<Any>` with caller-supplied `key: (T) -> Any` lambda | Any remapping logic will have off-by-one errors across sort; keys are O(1) lookup |
| Keyboard modifier detection | `java.awt.KeyboardFocusManager` or `KeyEvent` listener | `Modifier.onClick(keyboardModifiers = { isCtrlPressed })` from `androidx.compose.foundation` | Platform-level listener is not Compose-reactive and won't integrate with composition lifecycle |
| Tree node expand guard | Boolean flag in node composable (reset on scroll-out) | `SnapshotStateMap<NodeKey, NodeState>` at tree level | LazyColumn disposes composables; node-level state is lost on scroll-out; tree-level map survives |

---

## Common Pitfalls

### PITFALL-01: `AeroScrollArea` Destroys Virtualization (CONFIRMED SHOWSTOPPER)

**What goes wrong:** `AeroScrollArea` is `Column(Modifier.verticalScroll(...))` — gives LazyColumn an infinite height constraint → all rows materialize. No virtualization, frame drops at 100+ rows.

**Detection:** Row count above fold equals total row count. `MeasureException` in logs.

**Prevention:** DataTable owns its own `LazyColumn(state = lazyListState)` + `AeroScrollBar(lazyListState)`. grep gate: `AeroScrollArea` inside AeroDataTable.kt = 0 hits.

---

### PITFALL-03: `detectDragGestures` touchSlop=18dp Silently Breaks Column Resize (DEFUSED)

Already defused by `Modifier.aeroDragSplitter` (Phase 7). No new `detectDragGestures` in Phase 9. grep gate: `detectDragGestures` in datatable/ package = 0 hits.

---

### PITFALL-04: Selection Indices Stale After Sort (API DESIGN — LOCKED BEFORE IMPLEMENT)

**Prevention:** Selection API must use `Set<Any>` + `key: (T) -> Any` in plan-01. This is a breaking-change-level API decision that cannot be corrected post-ship.

---

### PITFALL-05: TreeView `onExpand` Fires on Every Scroll-Back (DEFUSED BY DESIGN)

**Prevention:** `SnapshotStateMap<Any, NodeState>` at tree level holds `expanded: Boolean` and `childrenLoaded: Boolean`. The `onExpand` guard:

```kotlin
// In AeroTreeView expand handler:
fun onNodeExpandClick(nodeKey: Any, item: T) {
    val state = expandStateMap[nodeKey] ?: NodeState(expanded = false, childrenLoaded = false)
    val newExpanded = !state.expanded
    expandStateMap[nodeKey] = state.copy(expanded = newExpanded)
    if (newExpanded && !state.childrenLoaded) {
        expandStateMap[nodeKey] = expandStateMap[nodeKey]!!.copy(childrenLoaded = true)
        onExpand(item)  // fires EXACTLY ONCE per node
    }
}
```

---

### PITFALL-10: Selection vs Hover Token Confusion (CONFIRMED — LOCKED TOKEN)

Selected = `colors.borderSelected.copy(alpha = 0.15f)`. NOT `colors.primary.copy(alpha = 0.2f)` (that's AeroListItem's token — wrong for DataTable). Four-state color scheme verified against all three AeroColorScheme presets.

---

### PITFALL-NEW-01: `AnimatedVisibility` Height Instability in LazyColumn

**What goes wrong:** If tree children are rendered inside a single LazyColumn item using `AnimatedVisibility(visible = isExpanded)`, the item height changes during animation. LazyColumn cannot pre-measure future item heights, causing neighbors to jump position.

**Prevention:** Use the flatten-and-replace pattern (Pattern 5). Children become separate LazyColumn items inserted/removed in the flat list. Fixed `rowHeight = 36.dp` applies to every item; no height change during animation.

---

### PITFALL-NEW-02: `AeroScrollBar` Missing `LazyListState` Overload

**What goes wrong:** `AeroScrollBar` currently accepts only `ScrollState`. Passing `LazyListState` directly to it does not compile.

**Prevention:** Add a `LazyListState` overload to `AeroScrollBar` in plan-01 (API design plan). This is an additive change (new overload, no breaking changes). Alternatively use `VerticalScrollbar` directly inside DataTable/TreeView internal composables — but adding the `AeroScrollBar` overload is cleaner and consistent.

---

### PITFALL-NEW-03: `Modifier.onClick(keyboardModifiers)` is `@ExperimentalFoundationApi`

**What goes wrong:** Forgetting to add `@OptIn(ExperimentalFoundationApi::class)` produces a compile error for every `AeroTableRow.kt` that uses the keyboard-modifier click API.

**Prevention:** Add `@OptIn(ExperimentalFoundationApi::class)` to `AeroTableRow.kt` (internal file, not public surface). Import: `androidx.compose.foundation.ExperimentalFoundationApi`. No public API surface is affected.

---

## Code Examples

### Scrollbar Adapter Wiring (VERIFIED)

```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-scrollbars.html
// Pattern for DataTable/TreeView vertical scrollbar

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter

@Composable
fun AeroDataTable(/* ... */) {
    val lazyListState = rememberLazyListState()
    Box(modifier = modifier) {
        Column {
            // header row (outside LazyColumn) ...
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            ) {
                items(sortedData, key = { keyFn(it) }) { /* ... */ }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
        // OR use AeroScrollBar(lazyListState) once the overload is added
    }
}
```

### Ctrl/Shift Click for Multi-Selection (VERIFIED)

```kotlin
// Source: kotlinlang.org/docs/multiplatform/compose-desktop-mouse-events.html

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onClick
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AeroTableRow(
    key: Any,
    isSelected: Boolean,
    selectionMode: SelectionMode,
    onSingleClick: () -> Unit,
    onCtrlClick: () -> Unit,    // toggle this row
    onShiftClick: () -> Unit,   // range from anchor to this row
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colors = AeroTheme.colors

    val bg = when {
        isSelected && isHovered -> colors.borderSelected.copy(0.15f)
            .compositeOver(colors.buttonHover)
        isSelected -> colors.borderSelected.copy(0.15f)
        isHovered  -> colors.buttonHover
        else       -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(bg)
            .hoverable(interactionSource)
            .then(when (selectionMode) {
                SelectionMode.None -> Modifier
                SelectionMode.Single, SelectionMode.Multi ->
                    Modifier
                        .onClick(keyboardModifiers = { isCtrlPressed },
                                 interactionSource = interactionSource) { onCtrlClick() }
                        .onClick(keyboardModifiers = { isShiftPressed },
                                 interactionSource = interactionSource) { onShiftClick() }
                        .onClick(interactionSource = interactionSource) { onSingleClick() }
            })
    ) { content() }
}
```

### Tree Expand State Guard (VERIFIED — PITFALL-05 defused)

```kotlin
// Source: PITFALLS.md §PITFALL-05 + Pattern 5 above

data class NodeState(val isExpanded: Boolean, val childrenLoaded: Boolean)

@Composable
fun AeroTreeView(
    rootNodes: List<T>,
    children: (T) -> List<T>,
    isExpandable: (T) -> Boolean,
    key: (T) -> Any,
    onExpand: (T) -> Unit,
    nodeContent: @Composable (T) -> Unit,
    /* ... */
) {
    // CRITICAL: lives ABOVE LazyColumn — survives item disposal
    val expandStateMap = remember { mutableStateMapOf<Any, NodeState>() }
    val lazyListState = rememberLazyListState()

    val expandedKeys: Set<Any> = remember(expandStateMap.keys.size) {
        expandStateMap.entries.filter { it.value.isExpanded }.map { it.key }.toSet()
    }
    val flatNodes = remember(expandedKeys, rootNodes) {
        flattenTree(rootNodes, 0, expandedKeys, children, isExpandable, key)
    }

    Box {
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize().padding(end = 12.dp)) {
            items(flatNodes, key = { it.key }) { flatNode ->
                AeroTreeNode(
                    flatNode = flatNode,
                    onExpandClick = {
                        val nodeKey = flatNode.key
                        val state = expandStateMap[nodeKey]
                            ?: NodeState(isExpanded = false, childrenLoaded = false)
                        val nowExpanded = !state.isExpanded
                        expandStateMap[nodeKey] = state.copy(isExpanded = nowExpanded)
                        if (nowExpanded && !state.childrenLoaded) {
                            expandStateMap[nodeKey] = expandStateMap[nodeKey]!!
                                .copy(childrenLoaded = true)
                            onExpand(flatNode.item)  // fires EXACTLY ONCE
                        }
                    },
                    nodeContent = nodeContent,
                )
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
```

### Column Width Resolution

```kotlin
// Internal helper — pure function, called in remember(columns, totalWidthPx)
internal fun resolveColumnWidths(
    columns: List<AeroTableColumn<*>>,
    totalWidthPx: Float,
    density: Density
): List<Dp> {
    val fixedPx = columns.sumOf { col ->
        when (val w = col.width) {
            is AeroColumnWidth.Fixed -> with(density) { w.dp.toPx().toDouble() }
            is AeroColumnWidth.Weight -> 0.0
        }
    }.toFloat()
    val totalWeight = columns.sumOf { col ->
        when (val w = col.width) { is AeroColumnWidth.Weight -> w.value.toDouble(); else -> 0.0 }
    }.toFloat()
    val remainingPx = maxOf(0f, totalWidthPx - fixedPx)

    return columns.map { col ->
        when (val w = col.width) {
            is AeroColumnWidth.Fixed -> maxOf(w.dp, col.minWidth)
            is AeroColumnWidth.Weight -> {
                val computed = with(density) { (remainingPx * w.value / totalWeight).toDp() }
                maxOf(computed, col.minWidth)
            }
        }
    }
}
```

---

## Locked Decision Status

| Decision | Status | Notes |
|----------|--------|-------|
| `cell: @Composable (T) -> Unit` slot | ✅ confirmed | Standard Compose composable slot pattern |
| `AeroColumnWidth` sealed class | ✅ confirmed | `Fixed(Dp)` / `Weight(Float)` + `minWidth: Dp` |
| `columns: List<AeroTableColumn<T>>` | ✅ confirmed | Plain data class, no DSL overhead |
| Sort = hybrid uncontrolled/controlled | ✅ confirmed | Must document in KDoc per CONTEXT.md requirement |
| Selection = controlled `selectedKeys: Set<Any>` | ✅ confirmed | PITFALL-04 defused; key-stable after sort |
| Shift-anchor = internal `remember` | ✅ confirmed | Not in public API; computed against displayed sorted order |
| Tree model = functions (not node wrappers) | ✅ confirmed | Correct for mutable lazy-load; avoids immutable tree rebuild |
| `onExpand` fires exactly once per node | ✅ confirmed | `SnapshotStateMap` guard + `childrenLoaded` flag |
| `rowHeight: Dp = 36.dp` fixed | ✅ confirmed | Enables stable scrollbar math; avoids AnimatedVisibility height issue |
| NO `stickyHeader` | ✅ confirmed | JetBrains #3016 + #2940 still open in CMP 1.7.x |
| Glass header via `glassPanel` | ✅ confirmed | `Modifier.glassPanel(cornerRadius = 0.dp)` for full-width strip |
| `AeroIcons.CaretUp`/`CaretDown` sort; `CaretRight` tree | ✅ confirmed | `graphicsLayer { rotationZ }` + `animateFloatAsState` |
| `aeroDragSplitter` for column resize | ✅ confirmed | Phase 7 — internal, Orientation.Horizontal, 1D Float delta |
| NO `AeroScrollArea` inside DataTable/TreeView | ✅ confirmed | grep gate required in plan |
| `rememberScrollbarAdapter(LazyListState)` exists in CMP 1.7.3 | ✅ confirmed | Current (v2) overload returns `v2.ScrollbarAdapter`; old overload @Deprecated but still compiles |
| `AeroScrollBar` needs `LazyListState` overload | ⚠️ NEW ACTION | Currently only accepts `ScrollState`; Phase 9 plan-01 adds new overload OR uses `VerticalScrollbar` directly |
| `Modifier.onClick(keyboardModifiers)` for Ctrl/Shift | ✅ confirmed | `@ExperimentalFoundationApi`; requires opt-in in `AeroTableRow.kt` |

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (existing project convention, confirmed in Phase 7/8 test files) |
| Config file | none — standard Gradle `testImplementation` wiring |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*" -x` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DATA-01 | Virtualization: row count > visible count when 1000 items | manual smoke + unit | `./gradlew :library:test --tests "*AeroDataTableTest*"` | ❌ Wave 0 |
| DATA-02 | Sort: header click → asc→desc→none state machine | unit (pure state logic) | `./gradlew :library:test --tests "*SortStateTest*"` | ❌ Wave 0 |
| DATA-03 | Selection: Ctrl-toggle, Shift-range, key stability after sort | unit (pure selection logic) | `./gradlew :library:test --tests "*SelectionLogicTest*"` | ❌ Wave 0 |
| DATA-04 | Column resize: min-width clamp, delta accumulation | unit (pure width math) | `./gradlew :library:test --tests "*ColumnWidthTest*"` | ❌ Wave 0 |
| DATA-05 | Tree flatten: expand/collapse produces correct flat list | unit (pure flatten fn) | `./gradlew :library:test --tests "*TreeFlattenTest*"` | ❌ Wave 0 |
| DATA-06 | onExpand fires once: fire on first expand, NOT on scroll-back | unit (NodeState machine) | `./gradlew :library:test --tests "*NodeStateTest*"` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :library:compileKotlin`
- **Per wave merge:** `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*"`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/SortStateTest.kt` — covers DATA-02 sort state machine (asc → desc → none → asc cycle; only one column sorts at a time)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/SelectionLogicTest.kt` — covers DATA-03 (Ctrl-toggle, Shift-range from anchor, key stability across sort)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/ColumnWidthTest.kt` — covers DATA-04 (Fixed/Weight resolution, minWidth clamp, resolveColumnWidths pure fn)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/TreeFlattenTest.kt` — covers DATA-05 (flattenTree with depth=0/1/2; expand/collapse; empty children)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/NodeStateTest.kt` — covers DATA-06 (childrenLoaded guard: onExpand fires on first expand; does not fire on second expand call or scroll-back)

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `rememberScrollbarAdapter(LazyListState, itemCount, averageItemSize)` | `rememberScrollbarAdapter(LazyListState)` — no extra params | CMP ~1.4 v2 API | v2 adapter reads LazyListState directly; fixed-height items still work better for accuracy |
| Old `ScrollbarAdapter` return type | `androidx.compose.foundation.v2.ScrollbarAdapter` | CMP ~1.4 | Old type deprecated but not removed; both overloads still compile in 1.7.3 |
| `detectDragGestures` for Canvas drag | `awaitPointerEventScope` manual loop (`aeroDragSplitter`) | Established PITFALL-03 workaround | No first-pixel drag lag on Desktop |
| `stickyHeader` for table header row | External `Row` + shared `ScrollState` | JetBrains #3016/#2940 remain open | No scrollbar flicker |

**Deprecated / outdated:**
- `ScrollbarAdapter` (v1, no package qualifier): Deprecated in favor of `v2.ScrollbarAdapter`. Still compiles in CMP 1.7.3 — produces warning, not error.
- `itemCount`/`averageItemSize` parameters on `rememberScrollbarAdapter`: Removed in v2 API. Not available when using the current overloads.

---

## Open Questions

1. **Should `AeroScrollBar` get a `LazyListState` overload (Option A) or should DataTable/TreeView use `VerticalScrollbar` directly (Option B)?**
   - What we know: Phase 9 adds the first lazy-list components; Option A is additive and consistent; Option B avoids touching a v1.0 public component.
   - What's unclear: User preference on API surface.
   - Recommendation: Option A — add `AeroScrollBar(lazyListState: LazyListState, modifier: Modifier = Modifier)` overload. Planner should make this plan-01 task 1. No behavioral impact on existing `AeroScrollBar(ScrollState)` callers.

2. **`remember(expandedKeys, rootNodes)` — is `expandedKeys` derived from `expandStateMap` correctly reactive?**
   - What we know: `expandStateMap` is a `SnapshotStateMap`; Compose tracks reads from it automatically. Passing `expandedKeys` as a `remember` key requires converting the map to a stable key (e.g., `expandStateMap.keys.size` or a derived state).
   - Recommendation: Use `derivedStateOf { expandStateMap.entries.filter { it.value.isExpanded }.map { it.key }.toSet() }` to derive `expandedKeys` — this ensures `flatNodes` only recomputes when expand state actually changes, not on any SnapshotStateMap write.

---

## Sources

### Primary (HIGH confidence)

- `kotlinlang.org/docs/multiplatform/compose-desktop-scrollbars.html` — confirmed `rememberScrollbarAdapter(LazyListState)` overload, exact wiring pattern with `LazyColumn` + `VerticalScrollbar` (fetched 2026-06-18)
- `github.com/JetBrains/compose-multiplatform-core/blob/jb-main/.../Scrollbar.skiko.kt` — confirmed v1/v2 overload signatures, `@Deprecated` annotations, `v2.ScrollbarAdapter` return type (fetched 2026-06-18)
- `kotlinlang.org/docs/multiplatform/compose-desktop-mouse-events.html` — confirmed `Modifier.onClick(keyboardModifiers)`, `@ExperimentalFoundationApi`, `isCtrlPressed`/`isShiftPressed` property names, import path `androidx.compose.foundation.onClick` (fetched 2026-06-18)
- `.planning/research/PITFALLS.md` — PITFALL-01/03/04/05/10 + Performance Traps (project-authoritative, HIGH confidence)
- `.planning/research/ARCHITECTURE.md` — Phase 9 component structure, package layout, integration risks (project-authoritative)
- `.planning/research/STACK.md` — CMP 1.7.3 / Kotlin 2.1.21 / JDK 17 stack confirmation (project-authoritative)
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — exact `aeroDragSplitter` signature confirmed (source-of-truth)
- `library/src/main/kotlin/com/mordred/aero/components/containers/AeroScrollBar.kt` — confirmed current `ScrollState`-only signature; PITFALL-NEW-02 identified
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — confirmed exact token values for four-state selection color scheme (source-of-truth)

### Secondary (MEDIUM confidence)

- `JetBrains/compose-multiplatform/issues/3016` + `#2940` — stickyHeader + VerticalScrollbar flicker confirmed open (NOT fixed in CMP 1.7.3)
- `JetBrains/compose-jb/issues/181` — `itemCount`/`averageItemSize` removed from v2 scrollbar adapter API
- `JetBrains/compose-jb/issues/343` — touchSlop=18dp on Desktop with `detectDragGestures` (PITFALL-03 root cause; defused by `aeroDragSplitter`)
- `m-sasha/ComposeTree` — confirms flatten-to-LazyList is the standard pattern for tree rendering in Compose Desktop

### Tertiary (LOW confidence — not load-bearing)

- Google Issue Tracker #165921895 — `AnimatedVisibility` height instability in LazyColumn (inaccessible without sign-in; confirmed via community knowledge + PITFALL-NEW-01 mitigation is the fixed-rowHeight design that sidesteps the problem)

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| `rememberScrollbarAdapter(LazyListState)` exists in CMP 1.7.3 | HIGH | Two independent sources: official JetBrains docs + source code |
| `AeroScrollBar` needs `LazyListState` overload | HIGH | Source read of `AeroScrollBar.kt` confirms `ScrollState`-only signature |
| `Modifier.onClick(keyboardModifiers)` for Ctrl/Shift | HIGH | Official JetBrains docs confirmed with imports and experimental annotation |
| Shared horizontal ScrollState pattern | HIGH | Standard Compose pattern; confirmed in docs; PITFALL (no stickyHeader) documented |
| Flatten-to-flat-list for tree | HIGH | Standard pattern; cross-confirmed ComposeTree reference + PITFALLS |
| `animateFloatAsState` + `graphicsLayer { rotationZ }` | HIGH | Standard Compose animation pattern; `items(key=)` interaction confirmed |
| `AnimatedVisibility` height issue in LazyColumn | MEDIUM | Cannot read Issue Tracker directly; mitigated by fixed-rowHeight design decision |

**Research date:** 2026-06-18
**Valid until:** 2026-07-18 (30 days — CMP 1.7.x is stable; no major API churn expected)
