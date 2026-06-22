# Architecture Research: AeroPanelGroup

**Domain:** Compose Desktop additive layout component (v2.0.2)
**Researched:** 2026-06-22
**Confidence:** HIGH — based on direct reading of shipped source: AeroSplitPane.kt, AeroAccordion.kt, SplitClamp.kt, AeroDragSplitter.kt, SidebarStateTest, SplitClampTest, AccordionToggle.kt.

---

## System Overview

```
+---------------------------------------------------------------------+
|  AeroPanelGroup  (public, layout/AeroPanelGroup.kt)                 |
|  BoxWithConstraints -> totalPx                                       |
|  Hybrid controlled/uncontrolled expansion (AeroAccordion pattern)   |
|  Uncontrolled size state + onLayoutChange                            |
+---------------------------------------------------------------------+
|  Internal state (SnapshotStateList, inside AeroPanelGroup)           |
|  +----------------------------------------------------------+        |
|  | sizePx[i]: Float    (weight in expanded pool)            |        |
|  | lastExpandedPx[i]: Float   (restored on re-expand)       |        |
|  | expanded[i]: Boolean   (controlled or internal)          |        |
|  +----------------------------------------------------------+        |
+---------------------------------------------------------------------+
|  Derived layout pass  (pure math, no SubcomposeLayout)               |
|  +----------------------------------------------------------+        |
|  | availableForExpanded = totalPx                           |        |
|  |     - sum(headerPx x collapsed sections)                 |        |
|  |     - sum(dividerPx x adjacent-expanded pairs)           |        |
|  | renderHeight[i] = sizePx[i] / sum(sizePx[expanded])      |        |
|  |                   x availableForExpanded                  |        |
|  +----------------------------------------------------------+        |
+---------------------------------------------------------------------+
|  Column render loop  (no weight on expanded sections)                |
|  +----------------+  +----------------+  +-------------------+      |
|  | SectionHeader  |  | Content Box    |  | PanelGroupDivider |      |
|  | 36dp glassPanel|  | .height(dp)    |  | only between      |      |
|  | caret anim     |  | clips content  |  | exp+exp pair      |      |
|  +----------------+  +----------------+  +-------------------+      |
+---------------------------------------------------------------------+
|  Reused infrastructure  (zero new code for drag/clamp)               |
|  +----------------------+  +------------------------------+         |
|  | aeroDragSplitter     |  | clampDividerPx               |         |
|  | internal/drag/       |  | internal/splitpane/          |         |
|  +----------------------+  +------------------------------+         |
+---------------------------------------------------------------------+
```

---

## Component Responsibilities

| Component | File | Responsibility |
|-----------|------|----------------|
| `AeroPanelGroup` | `layout/AeroPanelGroup.kt` | Public composable. Receives sections list. BoxWithConstraints -> totalPx. Derives heights. Renders Column of headers + content + dividers. |
| `AeroPanelSection` | `layout/AeroPanelGroup.kt` (data class) | Caller-supplied descriptor: title, icon, collapsible, resizable, content lambda. No state inside — pure data, same pattern as AeroAccordionSection. |
| `distributePx` | `layout/internal/panelgroup/PanelDistribution.kt` | Pure JVM function: `(sizePx, expanded, totalPx, headerPx, dividerPx) -> FloatArray` of rendered heights. No Compose imports. Unit-testable. |
| `shareTransferOnCollapse` | `layout/internal/panelgroup/PanelDistribution.kt` | Pure JVM: given collapsing index, returns new `sizePx` array with released share redistributed to neighbors proportionally. |
| `shareTransferOnExpand` | `layout/internal/panelgroup/PanelDistribution.kt` | Pure JVM: given expanding index and `lastExpandedPx`, returns new `sizePx` array taking share from neighbors proportionally. |
| `clampDividerPx` | `layout/internal/splitpane/SplitClamp.kt` | **Reused verbatim.** Inverted-range guard already present (v2.0.1 fix PITFALL-B). |
| `aeroDragSplitter` | `internal/drag/AeroDragSplitter.kt` | **Reused verbatim.** `Orientation.Vertical`. awaitPointerEventScope, no touchSlop. |
| `PanelGroupDivider` | `layout/AeroPanelGroup.kt` (private composable) | 8dp hit-area + 1dp Aero line + grip dots. Only rendered between two adjacent expanded sections. Mirrors `SplitPaneDivider` structure. |
| `LayoutSection.kt` | `showcase/.../sections/LayoutSection.kt` | Append AeroPanelGroup demo block. Modify existing file, no new file. |

---

## File and Package Structure

```
library/src/main/kotlin/com/mordred/aero/
  components/
    layout/
      AeroPanelGroup.kt                    NEW: public API + private composables
      internal/
        panelgroup/
          PanelDistribution.kt             NEW: distributePx, shareTransfer (pure JVM)
        splitpane/
          SplitClamp.kt                    REUSED verbatim (no changes)
    internal/
      drag/
        AeroDragSplitter.kt                REUSED verbatim (no changes)

library/src/test/kotlin/com/mordred/aero/components/layout/
  PanelDistributionTest.kt                 NEW: pure-logic tests
  SplitClampTest.kt                        EXISTING (no changes needed)
  AccordionToggleTest.kt                   EXISTING (no changes needed)

showcase/src/main/kotlin/com/mordred/showcase/sections/
  LayoutSection.kt                         MODIFIED: append AeroPanelGroup demo block
```

**Structure rationale:**
- `internal/panelgroup/` mirrors `internal/splitpane/` and `internal/accordion/`: pure-JVM logic in `internal/`, public composable is the only entry point callers see.
- No `PanelSizeState.kt` as a separate file unless test ergonomics require it — the state is simple enough to live as local `SnapshotStateList` values inside `AeroPanelGroup.kt`.
- `AeroPanelGroup.kt` co-locates the public composable, private section row, and private divider — matching `AeroAccordion.kt` which co-locates `AeroAccordion` + `AccordionSectionRow` in one file.

---

## Architectural Patterns

### Pattern 1: Fraction-as-stable-coordinate (from AeroSplitPane v2.0.1)

**What:** Store sizes as share weights in a pool, not as absolute px tied to a specific `totalPx`. When `totalPx` changes, derived heights recompute proportionally without any state reset.

**Applied to AeroPanelGroup:** `sizePx[i]` is each section's raw px weight. On each recompose inside `BoxWithConstraints`, derive:

```kotlin
val availableForExpanded = totalPx
    - sections.indices.sumOf { i -> if (!expanded[i]) headerPx.toDouble() else 0.0 }.toFloat()
    - expandedPairCount * dividerPx

val expandedSizeSum = sections.indices
    .filter { expanded[it] }
    .sumOf { sizePx[it].toDouble() }.toFloat()

val renderHeight: (Int) -> Float = { i ->
    if (!expanded[i]) headerPx
    else (sizePx[i] / expandedSizeSum) * availableForExpanded
}
```

This is never stored in `remember` — it is recomputed each recompose. Window resize changes `totalPx`, which changes `availableForExpanded`, which rescales all `renderHeight[i]` proportionally. No state is reset.

**What to avoid:** `var sizePx by remember(totalPx) { ... }` — this re-keys state on every `totalPx` change. This was PITFALL-A in AeroSplitPane; its fix (fraction-based state, no `remember(totalPx)` key) shipped in v2.0.1.

### Pattern 2: Live-state read in drag loop (from AeroSplitPane v2.0.1 fix, commit 7f38c0c)

**What:** The `onDrag` lambda passed to `aeroDragSplitter` must read `sizePx` values live at drag time, not from values captured at composition time.

**Why it matters:** `aeroDragSplitter` keys its `pointerInput` on `(orientation, enabled)` only. The `onDrag` closure is captured once at first composition. If `sizePx[above]` changes between composition and drag (it will — that is what dragging does), a captured value is stale. This is the identical bug class as FIXSP-01.

**Concrete structure:**

```kotlin
val liveTotalPx by rememberUpdatedState(totalPx)
// sizePx is a SnapshotStateList — reads through it are always live.
// No rememberUpdatedState needed for SnapshotStateList itself.

val onDragBetween: (above: Int, below: Int, delta: Float) -> Unit = { above, below, delta ->
    val combined = sizePx[above] + sizePx[below]
    val minPx = with(density) { minSectionSize.toPx() }
    val clamped = clampDividerPx(sizePx[above], delta, minPx, combined - minPx)
    sizePx[above] = clamped
    sizePx[below] = combined - clamped
    onLayoutChange?.invoke(sizePx.toList())
}
```

`sizePx` is a `SnapshotStateList<Float>`. State list reads (`sizePx[i]`) inside the drag lambda are always live because `SnapshotStateList` reads go through Compose snapshot state — they do not capture a stale copy.

`liveTotalPx` uses `rememberUpdatedState` for the same reason as AeroSplitPane — if the parent container resizes mid-drag, `totalPx` changes, and `clampDividerPx` must use the current value.

### Pattern 3: px source-of-truth, animateFloatAsState for toggle only

**What:** One `Float` per section (`sizePx[i]`) is the single source of truth for layout. Drag writes directly to it. Toggle (collapse/expand) writes a new target value. `animateFloatAsState` drives rendered height toward the current `sizePx[i]` each frame.

**Concrete structure:**

```kotlin
// One animated float per section, targeting sizePx[i] if expanded, headerPx if collapsed
val animatedHeight = sections.indices.map { i ->
    val targetPx = if (expanded[i]) renderHeight(i) else headerPx
    animateFloatAsState(
        targetValue = targetPx,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "panelHeight_$i",
    ).value
}
```

**Why drag + animation coexist without conflict:**

When drag fires, `sizePx[above]` and `sizePx[below]` change every pointer event. `renderHeight(above)` and `renderHeight(below)` recalculate from the new `sizePx` values. `animateFloatAsState` receives a new `targetValue` every frame equal to the just-written drag position. Because the animation spec is `tween(200ms)` but the target changes every frame at ~60fps, the animation always snaps to the current target immediately — there is no lag accumulation.

When toggle fires, `expanded[i]` flips. `targetPx` for section `i` jumps from `renderHeight(i)` to `headerPx` (collapse) or from `headerPx` to `renderHeight(i)` (expand). `animateFloatAsState` smoothly interpolates over 200ms. Other sections' `sizePx` are updated by `shareTransferOnCollapse`/`shareTransferOnExpand`, so their `renderHeight` changes and their animations also trigger.

**Key spike question:** Does `animateFloatAsState` introduce any visible lag when `targetValue` changes every frame during drag? Architecturally no — `animateFloatAsState` with a `tween` spec immediately starts animating toward the new target on each frame, and since drag events arrive faster than the animation window, the rendered value tracks the target with sub-frame lag. But this must be confirmed empirically in the spike (Step 1 of build order).

**Fallback if spike fails:** Use `sizePx[i].toDp()` directly for drag rendering (no animation) and only invoke `animateFloatAsState` when `expanded[i]` changes. This requires a mode flag or a separate `isAnimating` boolean per section, adding complexity. Prefer the simpler unified approach unless the spike disproves it.

### Pattern 4: Hybrid controlled/uncontrolled expansion (from AeroAccordion)

**What:** Two explicit code paths — controlled (caller owns `expandedIndices`, component is a pure renderer) and uncontrolled (component manages internal state seeded from `initiallyExpanded`). Not collapsed into one branch.

**Applied to AeroPanelGroup:**

```kotlin
val controlled = onExpandedChange != null

val isExpanded: (Int) -> Boolean = { i ->
    if (controlled) expandedIndices?.contains(i) == true
    else i in internalExpanded
}

val onToggle: (Int) -> Unit = { i ->
    if (controlled) {
        val next = if (i in (expandedIndices ?: emptySet()))
            (expandedIndices ?: emptySet()) - i
        else
            (expandedIndices ?: emptySet()) + i
        onExpandedChange!!(next)
    } else {
        internalExpanded = if (i in internalExpanded)
            internalExpanded - i
        else
            internalExpanded + i
    }
}
```

**KDoc must state:** "Do not collapse to one branch — both paths are intentional (matches AeroAccordion hybrid-ownership convention)."

**Size state is always uncontrolled.** Only expansion is hybrid. Sizes are exposed via `onLayoutChange: ((List<Float>) -> Unit)?` for the caller to persist and restore — but the caller never feeds sizes back in per-frame. This avoids the round-trip drag performance problem noted in AeroSplitPane KDoc ("A controlled drag would fire onSplitChange every frame, requiring the caller to hold and re-apply the split position on every mouse move").

### Pattern 5: Divider placement as derived index pairs (no divider state)

**What:** Divider positions are never stored. They are derived each recompose by walking the section list and rendering a divider only where `isExpanded(i) && isExpanded(i + 1)`.

**Concrete render loop:**

```kotlin
Column(modifier = modifier.fillMaxWidth()) {
    sections.forEachIndexed { i, section ->
        SectionHeader(
            title = section.title,
            expanded = isExpanded(i),
            onToggle = { onToggle(i) },
        )
        if (isExpanded(i)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { animatedHeight[i].toDp() })
            ) { section.content() }
        }
        // Divider only between two adjacent expanded sections
        if (isExpanded(i) && i < sections.lastIndex && isExpanded(i + 1)) {
            PanelGroupDivider(
                onDrag = { delta -> onDragBetween(i, i + 1, delta) },
            )
        }
    }
}
```

When section `i` collapses, `isExpanded(i)` becomes false on the same recompose that updates `expanded[i]`. The dividers adjacent to section `i` disappear in the same frame as the collapse begins. No divider state needs cleanup.

---

## Data Flow

### Collapse Toggle

```
User clicks header[i]
    |
    v
onToggle(i)  ->  expanded[i] = false
                 lastExpandedPx[i] = sizePx[i]   (save before redistribution)
                 sizePx = shareTransferOnCollapse(sizePx, expanded, i, headerPx)
    |
    v
Recompose:
  renderHeight[i] = headerPx  (expanded=false)
  renderHeight[neighbors] = increased (received i's share)
  animatedHeight[i]: targetValue = headerPx  ->  smooth 200ms collapse
  animatedHeight[neighbors]: targetValue increased  ->  smooth 200ms grow
  PanelGroupDivider adjacent to i: disappears (isExpanded(i) = false)
```

### Expand Toggle

```
User clicks header[i]
    |
    v
onToggle(i)  ->  expanded[i] = true
                 sizePx = shareTransferOnExpand(sizePx, expanded, i, lastExpandedPx[i])
    |
    v
Recompose:
  renderHeight[i] = derived from lastExpandedPx[i] weight
  renderHeight[neighbors] = decreased (gave share to i)
  animatedHeight[i]: targetValue = renderHeight[i]  ->  smooth 200ms expand
  animatedHeight[neighbors]: targetValue decreased  ->  smooth 200ms shrink
  PanelGroupDivider: appears between i and its expanded neighbors
```

### Drag Resize

```
User drags PanelGroupDivider between section[above] and section[below]
    |
    v
aeroDragSplitter.onDrag(delta)
    |
    v
onDragBetween(above, below, delta):
    combined = sizePx[above] + sizePx[below]
    clamped = clampDividerPx(sizePx[above], delta, minPx, combined - minPx)
    sizePx[above] = clamped            (direct write, no animation wrapper)
    sizePx[below] = combined - clamped
    onLayoutChange?.invoke(sizePx.toList())
    |
    v
Recompose:
  renderHeight[above], renderHeight[below] recalculate from new sizePx weights
  animatedHeight[above]: targetValue = renderHeight[above]  ->  chases drag instantly
  animatedHeight[below]: same
  (all other sections unaffected)
```

### Window Resize

```
Parent container resizes  ->  BoxWithConstraints recomposes with new totalPx
    |
    v
availableForExpanded recalculates from new totalPx
renderHeight[i] = (sizePx[i] / expandedSizeSum) * availableForExpanded
    |
    v
All sections reflow proportionally.
sizePx weights unchanged (no state reset).
No animation triggered (renderHeight changes continuously during resize).
```

---

## New vs Reused Code

### Reused Verbatim

| Item | File | Usage in AeroPanelGroup |
|------|------|------------------------|
| `aeroDragSplitter` | `internal/drag/AeroDragSplitter.kt` | Applied to `PanelGroupDivider` with `Orientation.Vertical` |
| `clampDividerPx` | `layout/internal/splitpane/SplitClamp.kt` | Called in `onDragBetween`. Inverted-range guard already present. |

`fractionToPx` and `pxToFraction` from SplitClamp.kt are NOT needed — PanelGroup uses raw px weights, not 0..1 fractions.

### New Code Required

| File | Type | Content |
|------|------|---------|
| `layout/AeroPanelGroup.kt` | Compose | `AeroPanelSection` data class; `AeroPanelGroup` public composable; `AeroPanelSectionRow` private composable; `PanelGroupDivider` private composable |
| `layout/internal/panelgroup/PanelDistribution.kt` | Pure JVM | `distributePx`, `shareTransferOnCollapse`, `shareTransferOnExpand` — no Compose imports |
| `layout/PanelDistributionTest.kt` (test source) | JVM test | All pure-logic scenarios (see table below) |

---

## Pure/Unit-Testable Logic

**Convention (from existing codebase):** Functions with no Compose imports live in `internal/` packages and are tested with plain `kotlin.test` — no Compose test runner required. Pattern: `SplitClamp.kt` (tested in `SplitClampTest`), `AccordionToggle.kt` (tested in `AccordionToggleTest`).

### Functions to isolate in PanelDistribution.kt

| Function | Inputs | Outputs | Key test scenarios |
|----------|--------|---------|-------------------|
| `distributePx` | `sizePx: FloatArray, expanded: BooleanArray, totalPx: Float, headerPx: Float, dividerPx: Float` | `FloatArray` rendered heights | 3 equal sections all expanded; 1 collapsed; all collapsed -> all headerPx; window resize keeps ratios |
| `shareTransferOnCollapse` | `sizePx: FloatArray, expanded: BooleanArray, index: Int, headerPx: Float` | new `sizePx: FloatArray` | Middle collapses -> both neighbors gain proportionally; first collapses -> right gains all; last collapses -> left gains all; only expanded section collapses -> no redistribution (nothing to give to) |
| `shareTransferOnExpand` | `sizePx: FloatArray, expanded: BooleanArray, index: Int, restorePx: Float` | new `sizePx: FloatArray` | Restore when neighbor has room; restore clamped when neighbor would hit minSize; restore when only one neighbor exists |

Additional test for drag budget: `combined = a + b; clamped = clampDividerPx(a, delta, min, combined - min)` with combined < 2×min (inverted range, same class as PITFALL-B).

### Functions that stay in the Compose layer (not unit-testable in isolation)

- `animateFloatAsState` usage — Compose runtime required
- `BoxWithConstraints` — Compose layout required
- `aeroDragSplitter` modifier — Compose pointer input required
- `glassPanel` modifier — Compose draw required

---

## Anti-Patterns

### Anti-Pattern 1: `remember(totalPx)` key on size state

**What people do:** `var sizePx by remember(totalPx) { mutableStateOf(totalPx / sections.size) }`

**Why it's wrong:** Resets all section sizes to equal distribution on every window resize or parent-pane drag. This was PITFALL-A, the root cause of the AeroSplitPane nested-freeze bug fixed in v2.0.1.

**Do this instead:** `val sizePx = remember { mutableStateListOf(*initialSizes) }` with no key. Derive render heights from current `totalPx` on each recompose.

### Anti-Pattern 2: Stale capture in aeroDragSplitter closure

**What people do:** `val h = sizePx[i]` as a plain local inside the composable, closed over in `onDrag`.

**Why it's wrong:** `aeroDragSplitter` captures `onDrag` once (keyed on `(orientation, enabled)`). The captured `h` is stale after the first drag event fires. Same bug class as FIXSP-01 (commit 7f38c0c).

**Do this instead:** Read `sizePx[above]` and `sizePx[below]` directly inside the `onDrag` lambda. `SnapshotStateList` reads inside lambdas are always live. Wrap `totalPx` in `rememberUpdatedState` for the clamp bound.

### Anti-Pattern 3: SubcomposeLayout or height measurement

**What people do:** `SubcomposeLayout` to measure content, or `onGloballyPositioned` to read heights.

**Why it's wrong:** Extra layout pass per frame. One-frame lag. Oscillation when combined with animation. Explicitly rejected in AeroAccordion KDoc.

**Do this instead:** Fix header height as constant (`36.dp.toPx()`), fix divider height as constant (`8.dp.toPx()`). Derive all heights arithmetically inside `BoxWithConstraints`. Content clips within its assigned height box.

### Anti-Pattern 4: `animateContentSize` on expanded sections

**What people do:** Reuse `Modifier.animateContentSize()` from AeroAccordion on the content box.

**Why it's wrong:** `animateContentSize` requires content measurement and fights explicit `.height(dp)` constraints. It also intercepts drag writes and re-animates them, making drag feel sluggish or incorrect.

**Do this instead:** `animateFloatAsState` per section, apply result as explicit `.height(with(density) { animatedPx.toDp() })`. Compatible with both drag and toggle.

### Anti-Pattern 5: Separate divider state map

**What people do:** `val dividerPositions = remember { mutableMapOf<Pair<Int,Int>, Float>() }` as a secondary structure.

**Why it's wrong:** Requires synchronization with `expanded[]` and `sizePx[]`. Any desync (collapse fires, divider map not cleaned) causes rendering bugs.

**Do this instead:** Derive divider existence inline: render divider only when `isExpanded(i) && isExpanded(i+1)`. No divider state needed.

---

## Build Order for Phase 13

**Lead with the animation-vs-drag risk spike.** This is the only architectural unknown — everything else ports directly from existing patterns.

### Step 1: SPIKE — animateFloatAsState + direct drag write coexistence (first task)

Create a minimal proof-of-concept in the showcase (throwaway, NOT in library) with:
- 3 hardcoded `mutableStateListOf` sections
- `animateFloatAsState` per section targeting current `sizePx[i]`
- One real `aeroDragSplitter` between sections 0 and 1 (Orientation.Vertical)
- One toggle button simulating collapse of section 1

**Gate:** Drag is instant (no visible lag). Collapse animates smoothly over 200ms. Dragging during a mid-animation collapse does not corrupt heights. If gate passes: proceed with Pattern 3 as designed. If `animateFloatAsState` introduces visible lag during drag: use direct `sizePx[i].toDp()` for drag rendering and only invoke `animateFloatAsState` when `expanded[i]` flips.

### Step 2: Pure logic + tests (TDD)

Write `PanelDistribution.kt` (no Compose imports). Write `PanelDistributionTest.kt` covering all scenarios in the table above. All tests GREEN before writing any Compose code. Mirrors Phase 12 TDD approach (RED test written before fix for `clampDividerPx`).

### Step 3: Layout skeleton (no animation, no drag)

- `AeroPanelSection` data class
- `BoxWithConstraints` -> `totalPx`
- `Column` render loop: static header rows + content boxes at computed heights
- Expand/collapse toggle (uncontrolled path only)
- Static `PanelGroupDivider` between expanded pairs (no drag yet)
- **Gate:** Window resize redistributes heights proportionally. Collapse/expand changes rendered heights correctly.

### Step 4: Collapse/expand animation

- `animateFloatAsState` per section (confirmed safe in Step 1)
- Wire `shareTransferOnCollapse` / `shareTransferOnExpand`
- `lastExpandedPx` save/restore
- **Gate:** 200ms FastOutSlowInEasing animation visible. Multiple concurrent animations (collapse A while expanding B) do not conflict.

### Step 5: Drag resize

- Wire `aeroDragSplitter` to `PanelGroupDivider` (Orientation.Vertical)
- `onDragBetween` with `rememberUpdatedState(totalPx)` pattern
- `onLayoutChange` callback
- **Gate:** Drag is instant. After window resize, drag does not snap back (live-state read confirmed working).

### Step 6: Controlled expansion path + KDoc

- Add `onExpandedChange` / `expandedIndices` parameters
- KDoc on `AeroPanelGroup` with REQ-ID and PITFALL references
- "Do not collapse to one branch" comment present

### Step 7: Aero visual polish

- `glassPanel` on header rows (matching AeroAccordion)
- Caret `AeroIcons.CaretRight` 0->90 via `animateFloatAsState` (matching AeroAccordion)
- `headerActions` slot (optional trailing composable in header row)
- Grip dots on `PanelGroupDivider` (matching `SplitPaneDivider` nasechki style)
- Three-theme visual check

### Step 8: Showcase demo + sign-off

- Append `AeroPanelGroup` demo block to `LayoutSection.kt`
- Three-theme visual sign-off

---

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| `animateFloatAsState` lag during rapid drag | Medium | High | Spike in Step 1. Fallback: skip animation during drag, only animate on toggle. |
| Stale capture in `aeroDragSplitter` closure | High (known class) | High | `rememberUpdatedState` pattern per FIXSP-01. Test: drag after resize must not snap. |
| Inverted range in clamp (two sections squeezed below 2×minSize) | Medium | Medium | `clampDividerPx` has `coerceAtLeast` guard already. Add test: `combined < 2×minSize`. |
| All-sections-collapsed state | Low | Medium | `distributePx` must return `headerPx` for all. Pure-logic test required. |
| Collapse animation conflicts with concurrent drag on same pair | Low | Medium | Guard: divider disappears the frame `expanded[i]` flips. Mid-collapse sections have no adjacent draggable divider. |
| `shareTransferOnExpand` takes too much from a small neighbor | Low | Low | Clamp neighbor's resulting `sizePx` to minimum before finalizing transfer. Pure-logic test. |

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|-----------|-------|
| State model (sizePx normalization) | HIGH | Direct port of AeroSplitPane fraction pattern; source read |
| Live-state drag pattern | HIGH | Direct read of FIXSP-01 fix and `rememberUpdatedState` in AeroSplitPane.kt line 116 |
| `animateFloatAsState` for toggle | HIGH | Used in AeroAccordion (`caretRotation`) and AeroSidebar; source read |
| `animateFloatAsState` + drag coexistence | MEDIUM | Architecturally sound; not previously tested in this codebase. Spike required. |
| Pure logic separation | HIGH | Mirrors SplitClamp.kt and AccordionToggle.kt exactly; source read |
| `clampDividerPx` reuse | HIGH | Inverted-range guard present; source read of SplitClamp.kt line 22 |
| `aeroDragSplitter` reuse | HIGH | Orientation.Vertical path exists; source read |

---

*Architecture research for: AeroPanelGroup (v2.0.2 additive layout component)*
*Researched: 2026-06-22*
