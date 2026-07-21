# Pitfalls Research

**Domain:** Compose Desktop UI library — v2.0.2 milestone: AeroPanelGroup vertical collapsible+resizable N-panel layout
**Researched:** 2026-06-22
**Confidence:** HIGH (all claims grounded in actual source files and shipped project history)

> **Scope note:** This file covers pitfalls specific to building `AeroPanelGroup` / `AeroPanelSection`. The v2.0.1 pitfalls (PITFALL-A through PITFALL-J) are NOT re-documented here but are explicitly carried forward where they apply. The v2.0 pitfalls (PITFALL-01 through PITFALL-15) are referenced by ID only.

---

## Carried-Forward Mandatory Rules

These rules from prior milestones apply directly to AeroPanelGroup. Violating any of them is a regression, not a new bug.

| ID | Rule | Where It Bites AeroPanelGroup |
|----|------|-------------------------------|
| PITFALL-03 | `detectDragGestures` BANNED on Compose Desktop (touchSlop=18dp). Use `Modifier.aeroDragSplitter`. | Every drag-resize divider between expanded sections. |
| PITFALL-A | `remember(totalPx)` re-keys state on every outer drag frame. Store a stable coordinate (fraction / normalized px), derive pixels each recompose. | `AeroPanelGroup` uses `BoxWithConstraints → totalPx`. If section `sizePx` is stored raw and `remember`ed on `totalPx`, it resets on every window resize. Carry-forward of v2.0.1 FIXSP root-cause. |
| PITFALL-B | `coerceIn(min, max)` throws `IllegalArgumentException` when `min > max`. Always guard with `safeMax = max.coerceAtLeast(min)`. | With 3+ sections the cascading-squeeze case is even more likely than in 2-pane SplitPane. Use the same `clampDividerPx` helper or a new equivalent for multi-section. |
| FIXSP-01 | Drag loop must read live state (`rememberUpdatedState` pattern). Stale-captured `sizePx` causes snap-back. | Every section's size is live state — the drag lambda must read it fresh every frame, not capture a copy from the `pointerInput` lambda closure. |
| No SubcomposeLayout | No height-measurement per frame. `BoxWithConstraints → totalPx` only. | Section heights must be derived from stored fractions and `availableForExpandedPx`, NOT measured from content. |

---

## Critical Pitfalls

### PNL-PITFALL-01: animateFloatAsState target fighting direct drag writes (animation vs. drag coexistence)

**This is the #1 risk. Build order: implement the spike that resolves this BEFORE writing any visible AeroPanelGroup code.**

**What goes wrong:**

`animateFloatAsState` works by interpolating toward a *target value* every frame. If a drag handler writes to the same `sizePx` state that `animateFloatAsState` is also trying to drive toward its own target, you get two writers competing:

1. Frame N: user drags, `sizePx` written to `600f`.
2. Frame N+1: `animateFloatAsState` spring/tween produces an intermediate value (say `580f`) toward its internal target and writes it, overriding the drag.
3. Result: section height oscillates. Dragging feels like fighting rubber-band resistance. Worse, if the animation target is the *pre-drag* value, the section snaps back to where it was before the drag began.

The symmetric case: while an animation is running (collapse in progress), a simultaneous drag on an adjacent divider is trying to resize the same section. The animation's `target` and the drag's direct write fight every frame.

**Why it happens:**

`animateFloatAsState` is designed for the pattern where `targetValue` is a function of snapshot state (e.g. `if (expanded) fullSizePx else headerPx`). If `sizePx` IS the state, and both the drag AND the animation are trying to update it, you have violated the single-writer discipline.

**How to avoid:**

Split the state into two variables with clearly separated write ownership:

```kotlin
// The stable, user-set size (drag and layout logic write here):
var sizeFraction by remember { mutableStateOf(initialFraction) }

// Animation target for collapse/expand transitions only:
val animTargetFraction: Float = if (expanded) sizeFraction else 0f

// Animated display value — read-only in layout:
val displayFraction by animateFloatAsState(
    targetValue = animTargetFraction,
    animationSpec = tween(200, easing = FastOutSlowInEasing),
    label = "section_${index}_size",
)
```

**The drag handler writes only to `sizeFraction` (the user-intent state). The layout reads only `displayFraction` (the animated view of that intent).** When no animation is running (expanded and animation settled), `displayFraction == sizeFraction` and drag updates are reflected immediately with zero latency.

During a collapse, `animTargetFraction` switches to `0f`. The animation interpolates `displayFraction` toward `0f`. At no point does the animation try to write back to `sizeFraction` — it only reads it as the source of `animTargetFraction`. Drags on neighboring sections that change `sizeFraction` recalculate `animTargetFraction` correctly on the next frame.

Additionally: **disable divider drag while an animation is running on either neighbor.** Check `displayFraction != sizeFraction` (animation in progress) and short-circuit the drag handler. This prevents the user from starting a drag during a collapse animation that would corrupt the target.

```kotlin
// In the divider's onDrag lambda:
val upperAnimDone = upperDisplay.value == upperSection.sizeFraction
val lowerAnimDone = lowerDisplay.value == lowerSection.sizeFraction
if (!upperAnimDone || !lowerAnimDone) return@onDrag  // animation in flight — skip drag
```

**Warning signs:**
- Section height oscillates or vibrates when dragging immediately after a collapse/expand toggle.
- A section that was being animated snaps to an unexpected size when the user drags a neighboring divider.
- Dragging a divider during a running animation causes both sections to jump to their pre-animation sizes.

**Recommended unit test:** None directly (this is Compose animation state, not pure logic). Verify via manual smoke test during spike: collapse a section, immediately drag the next divider — no snap-back, no oscillation. Also verify: complete an animation, then drag — `displayFraction` matches `sizeFraction` instantly.

**Phase to address:** Phase 13, step 1 — animation-vs-drag spike. This must be resolved before any other AeroPanelGroup implementation step. The spike should produce a minimal testable composable that exercises the exact state split described above.

---

### PNL-PITFALL-02: Float drift / rounding when normalizing px shares across window resize

**What goes wrong:**

`availableForExpandedPx = totalPx - Σ(collapsedHeaderPx) - Σ(dividerPx)` is computed in floating-point. Each expanded section holds a `sizeFraction` in `[0f..1f]` relative to `availableForExpandedPx`. On window resize, the fractions are restored: `sizePx_i = sizeFraction_i * availableForExpandedPx`. With N sections, floating-point rounding accumulates: `Σ(sizePx_i)` may differ from `availableForExpandedPx` by 0.5–2px depending on N and the exact fractions.

This 2px gap or overflow causes either a visible gap below the last section or an invisible 2px overflow that causes the group to exceed its container by a sub-pixel, which Compose silently clips.

A more severe variant: after several resize + collapse + expand cycles, fractions drift away from summing to 1.0 due to repeated `pxToFraction(clampedPx, newAvailable)` calls on different `available` values. After enough cycles the sections collectively represent 98% or 102% of available space.

**Why it happens:**

`sizeFraction_i = sizePx_i / availableForExpandedPx` is called every drag frame and after every collapse/expand. Each call introduces a tiny rounding error. Over time these errors compound — the same drift pattern seen in ColorPicker HSV/RGB round-trips (PITFALL-15, v2.0).

**How to avoid:**

1. **Last-section remainder rule:** Never compute `sizePx` for the last expanded section from its fraction. Instead: `lastSizePx = availableForExpandedPx - Σ(sizePx_0..N-2)`. This makes the last section absorb all rounding error and guarantees `Σ = totalAvailable` exactly.

2. **Re-normalize after every structural change:** After any collapse, expand, or drag-commit, re-compute all fractions from the current pixel values and then re-normalize so `Σ(fractions) == 1.0f`. A cheap re-normalize: `val sum = fractions.sum(); fractions = fractions.map { it / sum }`.

3. **Integer pixel arithmetic for layout:** When passing sizes to `Modifier.height(...)`, use `(sizePx).roundToInt().dp` converted with `density.toDp()` rather than raw `Float.dp`. Sub-pixel heights in `Modifier.height` cause hairline gaps on non-integer dp values.

**Recommended unit test:**

```
PanelDistributionTest.kt:
- "sum of section sizes equals totalAvailable after 100 random resize events" — generate random totalPx in [400..1200], random fractions normalized to 1.0, compute sizePx for each, assert abs(Σ - available) < 1.0f
- "last-section remainder exactly fills remaining space" — fixed fractions [0.3f, 0.3f, last=remainder], assert lastSizePx == available - first - second exactly
- "fractions stay normalized after 50 collapse/expand cycles" — toggle sections randomly, assert Σfractions stays in [0.99..1.01] throughout
```

**Phase to address:** Phase 13, step 2 — core layout logic (PanelDistribution.kt pure-logic file), written and TDD-locked before composable scaffolding.

---

### PNL-PITFALL-03: Divider placement bug when a neighbor collapses mid-drag

**What goes wrong:**

While the user is mid-drag on the divider between sections A and B, section C collapses (e.g., via a keyboard shortcut or programmatic toggle). The collapse removes C's share from `availableForExpandedPx`. Section B (the lower neighbor of the dragged divider) is now being re-measured against a smaller `available`. The divider's drag delta is still being applied in terms of the OLD `available`. Result: the divider position is physically wrong — it is placed outside the updated layout coordinates.

A related variant: the user drags the A/B divider while section B's collapse animation is still running. The animate target for B is moving toward `headerPx` while the drag is trying to assign B a larger size. One of two bad outcomes: B keeps collapsing despite the drag (animation wins), or B expands to the drag position and then the animation snaps it back (drag wins temporarily, animation corrects wrong).

**Why it happens:**

The drag delta is a pixel offset in the coordinate space of the group at drag-start. If the layout geometry changes mid-drag (collapse of any section), the coordinate space shifts but the delta does not compensate.

**How to avoid:**

1. **Cancel any in-flight divider drag when a toggle event fires.** In `awaitPointerEventScope`, check a `dragEnabled` flag before applying each delta. The toggle handler sets `dragEnabled = false` and the pointer loop exits at its next frame. This is equivalent to how touch cancel works in mobile scroll conflict resolution.

2. **Lock toggles while a drag is active.** Use a `dragging: Boolean` state flag set to `true` on `AwaitFirstDown` and `false` on drag end or pointer up. The toggle button's `clickable` is disabled (or the toggle lambda is guarded) while `dragging`. This is the simpler approach and preferred for this component.

3. **Re-compute available space from scratch on every drag frame** (not once at drag-start). The drag lambda must call `computeAvailablePx(totalPx, sections)` every frame, not capture it at drag-start. This ensures if a structural change fires concurrently, the next drag delta uses the updated space.

Recommendation: use approach #3 (live computation of available space every frame) because it handles both race conditions generically. The `computeAvailablePx` function is cheap (N multiplications, no allocation).

**Warning signs:**
- After collapsing section C while dragging A/B divider, the divider jumps to an incorrect position.
- The bottom section overflows the group container (visible clip) after a mid-drag structural change.

**Recommended unit test:** Pure-logic unit test is not directly applicable (this is a Compose concurrency concern). Smoke-test in showcase: collapse a section programmatically via a button while dragging a divider in the same group.

**Phase to address:** Phase 13, step 3 — drag-resize logic implementation. Use `dragging` flag to guard toggles as a primary safety mechanism.

---

### PNL-PITFALL-04: Min-size clamp failures with 3+ sections — cascading squeeze and inverted ranges

**This is the N-section generalization of PITFALL-B from v2.0.1.**

**What goes wrong:**

With 3+ expanded sections, dragging the divider between sections 1 and 2 downward squeezes section 2. Section 2 reaches its `minSize`. The remaining squeeze must be absorbed by section 3. But section 3 also has a `minSize`. If the user continues dragging past the point where `section2.size + section3.size == minSize2 + minSize3`, the multi-section clamp must refuse to move the divider further, but a naive clamp of each section independently fails:

- `maxPx_for_divider_1_2 = availableForExpanded - minSize2 - minSize3`
- If `availableForExpanded < minSize1 + minSize2 + minSize3` (window shrunk), `maxPx < minPx` → crash, exactly like PITFALL-B.

With N sections the cascading squeeze is more complex: dragging divider i can potentially violate min-sizes of sections i+1 through N-1.

Additionally, the multi-section clamp must not just protect the directly adjacent sections — it must compute the **total minimum remaining below the divider** (`Σ minSize_j for j = i+1..N-1`) and use that as the upper bound for divider i.

**Why it happens:**

Two-pane SplitPane only ever looks at one neighbor per side. N-panel divider between sections i and i+1 must be aware of ALL sections on each side of it. Applying the SplitPane clamp directly to adjacent sections only is a natural but incorrect simplification.

**How to avoid:**

Define a `clampSectionDividerPx` function that takes the full sections array and the divider index:

```kotlin
internal fun clampPanelDividerPx(
    dividerIndex: Int,  // divider between section[dividerIndex] and section[dividerIndex+1]
    currentPx: Float,
    deltaPx: Float,
    sections: List<PanelSectionState>,  // only expanded sections
    availablePx: Float,
): Float {
    // min from above: all expanded sections ABOVE the divider have their own minimums
    val minPx = sections.take(dividerIndex + 1).sumOf { it.minSizePx.toDouble() }.toFloat()
    // max from below: all expanded sections BELOW (including direct lower neighbor)
    val minBelowPx = sections.drop(dividerIndex + 1).sumOf { it.minSizePx.toDouble() }.toFloat()
    val maxPx = (availablePx - minBelowPx).coerceAtLeast(minPx)  // PITFALL-B guard
    return (currentPx + deltaPx).coerceIn(minPx, maxPx)
}
```

The `coerceAtLeast(minPx)` guard (identical to PITFALL-B's fix) prevents the inverted-range crash when the window is squeezed below `Σ minSizes`.

**Recommended unit test:**

```
PanelClampTest.kt (analogous to SplitClampTest.kt):
- "clamp divider 1 in 3-section: dragging past section-2 minimum stops at section-2 minSize" — 3 sections, drag divider 1 to push section 2 below minimum, assert result == section2.minSize
- "clamp divider 1 in 3-section: dragging past both section-2 and section-3 combined minimum stops at min1 boundary" — available = 200px, min1=60, min2=60, min3=60, assert no throw and result clamped
- "inverted range (window too narrow for all minimums) does not throw" — available=100px, three sections each with minSizePx=60, assert no IllegalArgumentException
- "divider 0 upper bound accounts for all sections below" — 4 expanded sections, drag divider 0 to maximum, assert minPx below = Σ(min1..min3)
```

**Phase to address:** Phase 13, step 2 — PanelClamp.kt pure-logic file, written and TDD-locked before composable scaffolding. Mirror the SplitClamp/SplitClampTest pattern exactly.

---

### PNL-PITFALL-05: Share-transfer rounding so Σheights ≠ totalPx (gap or overflow)

**What goes wrong:**

When a section collapses, its `sizePx` is redistributed among the remaining expanded sections proportionally. If redistribution is done by dividing the freed pixels equally or proportionally and accumulating them section by section using floating-point arithmetic, the final Σ will differ from `availableForExpandedPx` by 0–2px due to rounding.

This is distinct from the window-resize drift in PNL-PITFALL-02: that pitfall is about fraction-px round-trips across resizes. This pitfall is about the single redistribution event: collapse section C, distribute `C.sizePx` to A and B. `A.sizePx + B.sizePx` should exactly equal the old `A.sizePx + B.sizePx + C.sizePx`, but floating-point prevents this.

The visual result is a 1–2px hairline gap after the last expanded section, or the last section overflowing by 1px.

**Why it happens:**

```kotlin
val share = collapsedSizePx / expandedNeighbors.size  // floating-point division
expandedNeighbors.forEach { it.sizePx += share }       // each += loses a sub-pixel
```

After N sections receive `share`, `Σ(share * N) < collapsedSizePx` by rounding error.

**How to avoid:**

Use the **largest-remainder method**: distribute the integer portion first, then give the remainder to the last recipient:

```kotlin
internal fun distributeCollapsedPx(
    collapsedPx: Float,
    recipients: List<PanelSectionState>,
): List<Float> {
    if (recipients.isEmpty()) return emptyList()
    val baseShare = collapsedPx / recipients.size
    val sizes = recipients.map { it.sizePx + baseShare }.toMutableList()
    // Correct rounding error: last section absorbs residual
    val distributed = sizes.sum() - recipients.sumOf { it.sizePx.toDouble() }.toFloat()
    sizes[sizes.lastIndex] += (collapsedPx - distributed)
    return sizes
}
```

Alternatively, after every structural change, apply the re-normalization + last-section remainder rule from PNL-PITFALL-02. These two mitigations together guarantee `Σ = available` to floating-point precision.

**Recommended unit test:**

```
PanelDistributionTest.kt:
- "distributeCollapsedPx: sum of new sizes == original Σ + collapsedPx exactly" — 3 recipients with random sizes, collapse one, assert Σ_new == Σ_old + collapsedPx within Float.EPSILON
- "share-transfer conserves px: collapse and immediate re-expand returns to within 1px" — collapse section B (transfers to A+C), expand B with lastExpandedPx, assert A+B+C sizes ≈ original within 1px tolerance
```

**Phase to address:** Phase 13, step 2 — PanelDistribution.kt pure-logic file, TDD-locked alongside PanelClamp.kt.

---

### PNL-PITFALL-06: `lastExpandedPx` restore when available height has shrunk

**What goes wrong:**

`lastExpandedPx` is the section's size at the moment it was collapsed. When expanded again, it is restored as the initial size. But between collapse and re-expand, the window may have been resized (shrunk). If `lastExpandedPx > availableForExpandedPx_at_restore`, restoring it would push the section above the available space and the layout overflows.

Additionally, if two sections are simultaneously collapsed and then re-expanded in different order, restoring both `lastExpandedPx` values may together exceed `availableForExpandedPx` even if each individually would not.

**Why it happens:**

`lastExpandedPx` is captured as an absolute pixel value at collapse time. It is not a fraction of `available`. The available space at re-expand time may differ from available space at collapse time.

**How to avoid:**

1. **Store `lastExpandedFraction`** (ratio to `availableForExpandedPx` at collapse time), not `lastExpandedPx`. On re-expand, `restoredPx = lastExpandedFraction * currentAvailableForExpandedPx`. This mirrors the fraction-based state discipline of AeroSplitPane (PITFALL-A fix).

2. **Clamp the restored size to the current available:** even with fractions, after restoring multiple sections simultaneously, do one full re-normalize pass so `Σ(restoredFractions) ≤ 1.0`.

3. **Minimum viable restore:** if `currentAvailableForExpandedPx < all_minSizes_combined`, restore each section at its `minSizePx` and distribute any remainder evenly. Never restore a section below its `minSizePx`.

**Recommended unit test:**

```
PanelDistributionTest.kt:
- "restore after shrunk window: restoredPx does not exceed availableForExpandedPx" — collapse at available=800px, shrink to 400px, re-expand, assert restored sizePx <= 400px - otherSectionsMin
- "restore lastExpandedFraction: restored fraction * new available gives proportionally correct size" — collapse at fraction 0.4f in 800px available, re-expand at 600px available, assert restoredPx == 0.4f * 600px ≈ 240px
- "multi-restore: two sections restored simultaneously do not overflow" — two collapsed sections with lastExpandedFraction 0.6f each, re-expand both, assert Σ == 1.0f after normalize
```

**Phase to address:** Phase 13, step 2 — PanelDistribution.kt / PanelSectionState.kt. Use `lastExpandedFraction`, not `lastExpandedPx`, as the stored value.

---

### PNL-PITFALL-07: Off-by-one with collapsed-header and divider height accounting

**What goes wrong:**

`availableForExpandedPx = totalPx - Σ(collapsedHeaderPx) - Σ(dividerPx)`.

The off-by-one class:
- A divider exists ONLY between two expanded sections. A divider between an expanded section and a collapsed section is replaced by a static visual join (no drag, no height consumption). If all dividers are counted unconditionally regardless of collapse state, `availableForExpandedPx` is over-subtracted, causing the expanded sections to collectively be shorter than they should be.
- When the last section is collapsed, the header is at the bottom of the group. If the group is `Column` with no explicit bottom padding, there is no divider after it and no extra space — but if the group tries to draw a post-last-section divider (wrong loop boundary), it draws one extra 1dp line at the very bottom.
- Header height in dp must be converted to px using the current `density` ONCE, not re-converted each frame (float drift if density rounds differently under different recompose paths).

**Why it happens:**

Loop-boundary errors when computing "how many dividers exist given N sections in various collapsed states" are subtle. The correct count is: number of pairs of adjacent EXPANDED sections. This is NOT `expandedCount - 1` in general — if two expanded sections are separated by a collapsed section, they do NOT share a draggable divider (the collapsed header sits between them).

Correct formula:
```kotlin
// Count dividers = number of i where section[i] is expanded AND section[i+1] is expanded
val activeDividerCount = sections.zipWithNext().count { (a, b) -> a.expanded && b.expanded }
```

**How to avoid:**

Compute `availableForExpandedPx` using the active-divider count above. Convert all dp constants to px once, outside the hot recompose path. Store `headerHeightPx: Float` as a `remember`ed value computed once at first composition from `density` and `headerHeightDp`.

Additionally: render dividers only between pairs of adjacent expanded sections. Iterate over `sections.zipWithNext()` and conditionally render the drag divider or the static join based on the pair's expansion state.

**Recommended unit test:**

```
PanelDistributionTest.kt:
- "activeDividerCount: EECE pattern gives 1 divider (between E1 and E2, not E2 and E3 because C separates them)" — 4 sections [E, E, C, E], assert count == 1 (only pair 0-1)
- "availableForExpandedPx never negative for any collapse combination" — exhaustive test over all 2^N collapse combinations for N=4, assert available >= 0 in all cases
- "header height px is consistent across calls" — same density, same headerDp, call twice, assert equal
```

**Phase to address:** Phase 13, step 2 — `computeAvailablePx` pure-logic function, TDD-locked. Step 3 — conditional divider rendering in composable.

---

### PNL-PITFALL-08: Recomposition and re-key resets of section state

**This is the N-section generalization of PITFALL-A and PITFALL-D from v2.0.1.**

**What goes wrong:**

Several patterns cause Compose to reset per-section state:

1. **`remember(totalPx)` re-key** (direct carry from PITFALL-A): if `sizeFraction` is initialized in a `remember(totalPx)` block, every window resize resets all section fractions to their initial values. The PITFALL-A fix applies: `remember { mutableStateOf(initialFraction) }` with no key.

2. **Sections list structural change**: if `AeroPanelGroup` receives `sections: List<AeroPanelSectionConfig>` and the caller reorders or re-creates the list (e.g., wrapping in `remember` incorrectly on the caller side), Compose will consider it a new list and re-key the `forEachIndexed` loop's composable. All section state resets. Mitigation: document that section configs must be stable (`remember`ed by the caller), or use `key(section.id)` inside the loop.

3. **`key(index)` vs `key(id)` for section loops**: if the composable loop uses positional keys `key(index)` and the caller inserts a section at position 0, all existing sections shift index and their remembered state (sizeFraction, expanded) is rebound to the wrong section. Use `key(section.id)` (a stable, caller-supplied identifier).

4. **`pointerInput` re-creation invalidates in-flight drag**: `Modifier.pointerInput(key1, key2)` recreates the pointer handler when keys change. If `onDrag` lambda or `orientation` is used as a key, any recomposition that changes `onDrag` (e.g., because `onDrag` captures state that changed) cancels the current drag. Use `Modifier.pointerInput(Unit)` with `rememberUpdatedState` for all values read inside the handler (same discipline as AeroSplitPane line 116–124).

**Why it happens:**

All four variants stem from the same root: Compose's remember key system treats any change to a key as "this is a new invocation, reset state." The PITFALL-A fix for SplitPane solved case #1 for a single component. AeroPanelGroup has N components sharing a group-level `BoxWithConstraints`, making cases #2, #3, and #4 new surfaces.

**How to avoid:**

- Fraction state: `remember { mutableStateOf(...) }` with no key everywhere, mirroring AeroSplitPane.
- Section loop: `key(section.id)` not `key(index)`. Define `AeroPanelSectionConfig.id: String` in the public API.
- Drag modifier: `Modifier.pointerInput(Unit)` + `rememberUpdatedState` for all captured values. No lambda closures on drag-critical state.
- Document in KDoc that the `sections` list passed to `AeroPanelGroup` must be stable across recompositions (use `remember` at the call site).

**Warning signs:**
- All section sizes reset to initial fractions on window resize (PITFALL-A variant).
- Collapsing section 0 causes section 1 to forget its size (index re-key variant).
- Dragging a divider snaps back when any other state changes during the drag (stale-capture variant).

**Phase to address:** Phase 13, step 1 (spike) — confirm `remember { }` discipline. Step 3 — use `key(section.id)` in composable loop. Step 3 — `pointerInput(Unit)` + `rememberUpdatedState`.

---

## Moderate Pitfalls

### PNL-PITFALL-09: Divider rendered between a collapsed and an expanded section

**What goes wrong:**

A collapsed section has a fixed header height (~36dp). Its boundary with an adjacent expanded section is a static visual join — NOT a draggable divider. If a drag divider is rendered here anyway, the user gets a cursor change and drag affordance on a boundary they cannot meaningfully resize (dragging would change the header height, which is fixed by design).

**How to avoid:**

Render the `AeroDragSplitter` divider only between pairs of adjacent EXPANDED sections. Between an expanded section and a collapsed header (either order), render a static 1dp separator with no drag modifier and no cursor change. This follows the VS Code sidebar model: collapsed items are fixed-height entries in a list; drag handles only appear between resizable items.

**Phase to address:** Phase 13, step 3 — composable rendering loop.

---

### PNL-PITFALL-10: `animateContentSize` used instead of explicit `sizeFraction`-driven height

**What goes wrong:**

`AeroAccordion` uses `animateContentSize` for its content area — appropriate because the accordion content has unknown/dynamic height determined by its children. If `AeroPanelGroup` uses `animateContentSize` for section height, it re-introduces height measurement per frame for the animation, violating the no-SubcomposeLayout / no-height-measurement-per-frame rule.

More concretely: `animateContentSize` works by measuring the content before and after the state change and interpolating. For AeroPanelGroup sections, the section height is NOT determined by content height — it is determined by the explicit `sizePx` stored in state. Using `animateContentSize` would make the height content-driven and break the explicit px control that drag-resize requires.

**How to avoid:**

Use `Modifier.height(with(density) { displayFraction * availableForExpandedPx }.toDp())` driven by the `animateFloatAsState` display fraction (PNL-PITFALL-01 pattern). The section height is always explicit. No `animateContentSize`. The content inside the section should use `Modifier.fillMaxHeight()` and scroll internally if needed.

**Phase to address:** Phase 13, step 3 — section composable.

---

### PNL-PITFALL-11: `Modifier.weight` used for section heights in the group Column

**What goes wrong:**

`Modifier.weight(fraction)` in a `Column` recalculates ALL weighted children's heights on every recomposition. During a drag (which recomposes every pointer event), every section in the group recomposes its height measurement. For N=5+ sections this creates N unnecessary layout passes per drag frame.

The SplitPane solved this by giving the FIRST pane an explicit `Modifier.height(dividerPx.toDp())` and the SECOND pane `Modifier.weight(1f)` — only one weighted item. For N sections, the same pattern is: give sections 0 through N-2 explicit heights (`Modifier.height(sizePx.toDp())`), and give only the last section `Modifier.weight(1f)`. This also naturally implements the last-section remainder rule from PNL-PITFALL-02.

**How to avoid:**

Give sections [0..N-2] explicit `Modifier.height(displaySizeDp)` and section [N-1] `Modifier.weight(1f)`. The last section's visual size will be exactly the remaining space — no overflow, no gap, no drift.

Exception: when the last section is COLLAPSED, it must have an explicit header height, not `weight(1f)`. In that case, give the last EXPANDED section `weight(1f)` (which may not be the array-last section). Implementation: find `lastExpandedIndex`, give it `weight(1f)`, give all others explicit heights.

**Phase to address:** Phase 13, step 3 — section layout in composable.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Store `lastExpandedPx` as absolute px (not fraction) | Simple, obvious | Re-expand after window shrink overflows the group | Never |
| `remember(totalPx)` to initialize sizeFraction | "Resets on resize" reads like the right thing | Resets on every drag frame in any parent that resizes sections | Never |
| Count all `N-1` dividers regardless of collapse state | Simple formula | Over-subtracts available space; sections collectively too short | Never |
| `animateContentSize` for section height | Already exists in codebase for accordion | Measures content height per frame; breaks explicit px control for drag-resize | Never for AeroPanelGroup |
| `Modifier.weight(fraction)` for ALL sections | Less code | N layout passes per drag frame | Never for drag-active sections |
| `detectDragGestures` for divider drag | Standard API | touchSlop=18dp silently breaks drag on Compose Desktop (PITFALL-03) | Never on Compose Desktop |
| Write sizePx directly from `animateFloatAsState` target | Single state variable | Animation and drag fight for ownership every frame (PNL-PITFALL-01) | Never |
| Disable drag entirely during animations | Sidesteps conflict | Poor UX — user must wait 200ms between every action | Never as primary strategy; use as safety guard for animation-in-flight only |

---

## "Looks Done But Isn't" Checklist

Items that will appear complete in a demo but hide the listed failure in real use.

- [ ] **Animation-vs-drag coexistence:** Demo works when actions are sequential (wait for animation, then drag). Test: collapse a section, IMMEDIATELY drag the adjacent divider — no snap-back, no oscillation.
- [ ] **Multi-section clamp no-throw:** Demo works with large sections. Test: resize window to near minimum, verify no `IllegalArgumentException` in log.
- [ ] **Σ heights == totalPx:** Demo looks correct at demo resolution. Test: resize window through a large range (200px to 1200px), inspect for hairline gaps or bottom overflow.
- [ ] **lastExpandedFraction restore after shrink:** Demo tested at one window size. Test: collapse section, shrink window significantly, re-expand — section must not overflow the group.
- [ ] **mid-drag collapse:** Demo only uses keyboard/clicks, not concurrent gestures. Test: press divider, hold drag, click collapse button — no divider teleport.
- [ ] **Section ID stability:** Demo uses a fixed list. Test: reorder sections list at call site — verify section state (sizeFraction, expanded) follows the ID, not the position.
- [ ] **Three-theme visual sign-off:** AeroBlue / AeroDark / Classic must all render headers with glassPanel modifier, chevron CaretRight, and correct color tokens.
- [ ] **No `detectDragGestures`:** Grep all new files — zero results.
- [ ] **No `SubcomposeLayout` or `MeasurePolicy` with content measurement:** Grep all new files — zero results.
- [ ] **onLayoutChange fires once per structural event:** Not once per animation frame, not on every drag delta — only on user-committed size changes (drag end, expand, collapse).

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| PNL-PITFALL-01: animation vs drag conflict discovered mid-implementation | HIGH if state is entangled; LOW if spike resolved it upfront | Split into two state vars (intent vs. display) as described. This is a structural state change — all subsequent code assumes the split. Recover early (spike), not during step 3. |
| PNL-PITFALL-02: float drift discovered in visual review | LOW | Add re-normalize pass + last-section remainder rule to PanelDistribution.kt. Pure-logic change, no composable edits. |
| PNL-PITFALL-03: mid-drag collapse teleport discovered in QA | LOW-MEDIUM | Add `dragging` flag guard to toggle handler. One-line change per toggle site. |
| PNL-PITFALL-04: 3-section clamp crash discovered in QA | LOW | Replace per-divider clamp with `clampPanelDividerPx` (uses full sections array). TDD-add the inverted-range test RED first. |
| PNL-PITFALL-05: share-transfer gap discovered visually | LOW | Apply largest-remainder method to `distributeCollapsedPx`. Pure-logic change. |
| PNL-PITFALL-06: lastExpandedPx overflow after shrink | LOW | Change stored type from `lastExpandedPx: Float` to `lastExpandedFraction: Float`. One structural type change in PanelSectionState, no API change. |
| PNL-PITFALL-07: wrong divider count subtraction | LOW | Fix `computeAvailablePx` to use `activeDividerCount` (zipWithNext filter). One-line change + re-run unit tests. |
| PNL-PITFALL-08: state re-key on section reorder | MEDIUM if IDs not in API | Add `id: String` to `AeroPanelSectionConfig` and replace `key(index)` with `key(section.id)`. API addition but non-breaking if `id` has a default (e.g., `title`). |

---

## Pitfall-to-Phase Mapping

All pitfalls for v2.0.2 are addressed in Phase 13 (single phase). The ordering within Phase 13 steps matters:

| Pitfall | Build Step | Verification |
|---------|------------|--------------|
| PNL-PITFALL-01: animation vs drag | Step 1 — spike | Spike composable: collapse + immediate drag, no snap-back |
| PNL-PITFALL-08: re-key resets | Step 1 — spike (confirm `remember{}` discipline) + Step 3 (`key(section.id)`) | Window resize: fractions preserved; section reorder: state follows ID |
| PNL-PITFALL-04: N-section clamp crash | Step 2 — PanelClamp.kt (TDD, RED before GREEN) | Unit test: inverted-range no-throw for 3+ sections |
| PNL-PITFALL-02: float drift on resize | Step 2 — PanelDistribution.kt (TDD) | Unit test: Σ sizes == available after 100 random resize events |
| PNL-PITFALL-05: share-transfer rounding | Step 2 — PanelDistribution.kt (TDD) | Unit test: distributeCollapsedPx sum conserves px |
| PNL-PITFALL-06: lastExpandedPx overflow | Step 2 — PanelSectionState.kt (store fraction, not px) | Unit test: restore after shrink does not exceed available |
| PNL-PITFALL-07: divider count off-by-one | Step 2 — computeAvailablePx (TDD) | Unit test: EECE pattern gives 1 active divider, not 3 |
| PNL-PITFALL-03: mid-drag collapse race | Step 3 — drag handler + toggle guard | Smoke test: hold drag + click collapse button concurrently |
| PNL-PITFALL-09: drag divider on collapsed boundary | Step 3 — conditional divider rendering | Visual: no cursor change on collapsed/expanded boundary |
| PNL-PITFALL-10: animateContentSize misuse | Step 3 — section composable (explicit height only) | Grep: zero `animateContentSize` in AeroPanelGroup files |
| PNL-PITFALL-11: weight for all sections | Step 3 — section layout | Performance: N-section drag produces no per-section layout pass |
| PITFALL-03 (carry-forward) | Step 3 — drag modifier | Grep: zero `detectDragGestures` in new files |
| PITFALL-A (carry-forward) | Step 2 — fraction state | Window resize test: all fractions preserved |
| PITFALL-B (carry-forward) | Step 2 — PanelClamp.kt | Inverted-range unit test (TDD, RED before GREEN) |
| FIXSP-01 (carry-forward) | Step 3 — `pointerInput(Unit)` + `rememberUpdatedState` | Drag test: resize window during active drag, no snap-back |

---

## Recommended Pure-Logic Unit Test File

Create `PanelGroupLogicTest.kt` (analogous to `SplitClampTest.kt`) covering:

```
PanelClampTest (nested class or separate file):
- clampPanelDividerPx: divider in 3-section, lower neighbor at minSize stops at boundary
- clampPanelDividerPx: inverted range (available < Σ minSizes) does not throw
- clampPanelDividerPx: divider 0 upper bound == available - Σ(min_1..min_N-1)

PanelDistributionTest:
- distributeCollapsedPx: sum conserves px within Float.EPSILON
- re-normalize: Σ fractions == 1.0f after normalize
- last-section remainder: Σ sizePx == availableForExpandedPx exactly
- computeAvailablePx: EECE gives 1 active divider, not 3
- computeAvailablePx: all-collapsed gives available == 0 (or total minus all headers)
- computeAvailablePx: never negative for any 2^N collapse combination (N ≤ 5)

PanelRestoreTest:
- lastExpandedFraction restore: restoredPx = fraction * newAvailable, not fraction * oldAvailable
- restore after shrink: restoredPx ≤ availableForExpandedPx - otherSectionsMinPx
- multi-restore: Σ restored fractions ≤ 1.0f after normalize
```

All these functions must be pure JVM (no Compose imports) — same constraint as `SplitClamp.kt`. This enables fast unit tests without a Compose runtime.

---

## Sources

- `AeroSplitPane.kt` (read 2026-06-22) — lines 107–124 confirm fraction-based state + `rememberUpdatedState` pattern; line 116 confirms `rememberUpdatedState(totalPx)` discipline; `aeroDragSplitter` confirmed as the drag modifier
- `SplitClamp.kt` (read 2026-06-22) — confirms `safeMax = maxPx.coerceAtLeast(minFirstPx)` guard (PITFALL-B fix); `fractionToPx` / `pxToFraction` pure helpers
- `SplitClampTest.kt` (read 2026-06-22) — confirms TDD pattern for pure-logic clamp tests; `clampInvertedRangeDoesNotThrow` test is the direct template for PNL-PITFALL-04 unit test
- `AeroAccordion.kt` (read 2026-06-22) — confirms `animateFloatAsState` (caret rotation) + `animateContentSize` (height) pattern; confirms why `animateContentSize` is NOT appropriate for size-driven sections (content-height vs. explicit-px distinction)
- `.planning/PROJECT.md` (read 2026-06-22) — Key Decisions table: PITFALL-03 (`detectDragGestures` ban), PITFALL-A (fraction-based SplitPane), PITFALL-B (inverted-range clamp), FIXSP-01 (live state in drag loop), no SubcomposeLayout rule; v2.0.2 AeroPanelGroup spec including spike-first mandate
- `.planning/research/PITFALLS.md` prior version (read 2026-06-22) — PITFALL-A through PITFALL-J documented in detail for v2.0.1; all carry-forward rules inherited from this file
- Kotlin stdlib KDoc: `Float.coerceIn(minimumValue, maximumValue)` — "Throws IllegalArgumentException if minimumValue is greater than maximumValue" — confirmed PITFALL-B and PNL-PITFALL-04 root cause
- VS Code Side Bar layout model (design reference from milestone context) — collapse → fixed-height strip; drag handles only between expanded neighbors; share redistribution on collapse

---
*Pitfalls research for: aero-compose-ui v2.0.2 AeroPanelGroup vertical collapsible+resizable N-panel layout*
*Researched: 2026-06-22*
