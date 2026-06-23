# Phase 13: AeroPanelGroup - Research

**Researched:** 2026-06-22
**Domain:** Internal-precedent verification — Compose Desktop UI library additive layout component
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **API shape**: Scope-DSL (`AeroPanelGroupScope.section(key, title, ...) { content }`), NOT `List<data class>`. Mirror `AeroSidebar` DSL pattern.
- **Expansion model**: Hybrid controlled/uncontrolled exactly mirroring `AeroAccordion` (`onExpandedChange == null` → uncontrolled; non-null → controlled pure renderer). Both branches intentional — do NOT collapse.
- **Sizing state**: Store fraction/px-weight per section (stable coordinate), derive render heights from `totalPx` each recompose. NO `remember(totalPx)` key. Use `lastExpandedFraction` (NOT absolute px) for collapse/restore.
- **Animation**: 200ms `FastOutSlowInEasing` via `animateFloatAsState` for collapse/expand. Drag writes px directly with NO animation wrapper. Spike (step 1) must prove coexistence before any UI code.
- **Divider**: ONLY between two adjacent EXPANDED sections (`zipWithNext` filter). Border adjacent to a collapsed section is static — no grip, no drag, no resize cursor. Reuse `Modifier.aeroDragSplitter(Orientation.Vertical)` and `clampDividerPx` verbatim.
- **Drag loop live state**: `rememberUpdatedState(totalPx)` mandatory in drag lambda (FIXSP-01 regression guard). `SnapshotStateList` reads inside lambda are always live.
- **Header**: `glassPanel(cornerRadius = 8.dp)` surface, `AeroIcons.CaretRight` 0°→90° via `animateFloatAsState` + `graphicsLayer { rotationZ }`. Fixed internal constant ~36dp height.
- **`collapsible = false`**: No chevron, section cannot collapse, but participates in resize.
- **`resizable = false`**: Dividers rendered without grip, drag disabled.
- **`onLayoutChange`**: Fires on drag-END and on collapse/expand toggle — NOT on every drag frame.
- **TDD**: Pure logic (PanelDistribution.kt, no Compose imports) RED → GREEN before any Compose code.
- **No new Gradle dependencies**: Zero changes to `build.gradle.kts` or `libs.versions.toml`.
- **Build order is non-negotiable** (8 steps — see Architecture Patterns).
- **Win7 Aero aesthetic** (`glassPanel`, gloss/gradient/rounded/depth). NOT modern-flat.
- `detectDragGestures` BANNED. `SubcomposeLayout` BANNED. `animateContentSize` BANNED for section heights. `remember(totalPx)` key BANNED.

### Claude's Discretion

- Exact names of internal helpers and files (`PanelDistribution.kt` etc.) and signatures of pure functions.
- Exact structure of `AeroPanelGroupScope` and signature of `section(...)` (parameter order/defaults).
- Default value of `minSize` constant and exact header height constant (~36dp).
- Internal animation mechanics (one `animateFloatAsState` per section vs. derived), provided px-source-of-truth and no-extra-layout-pass rules are respected.
- Exact set and order of steps within the single phase (preserving: spike → pure-logic TDD → ... → showcase sign-off).

### Deferred Ideas (OUT OF SCOPE)

- Horizontal orientation (`orientation = horizontal`) — PNL-HORIZ-01, v2.x.
- Drag-to-reorder sections — PNL-REORDER-01.
- Nested `AeroPanelGroup` as first-class API — PNL-NEST-01.
- Keyboard resize/navigation of dividers — PNL-KBD-01.
- `headerHeight` as a public parameter.
- AeroDropdown popup-offset fix (DROP-FIX-01).

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PNL-01 | Scope-DSL: `section(key, title) { content }` | AeroSidebarScope.section() pattern verified; scope DSL is established idiom |
| PNL-02 | Collapse to ~36dp header strip; neighbors absorb height | shareTransferOnCollapse pure function; animateFloatAsState target switch |
| PNL-03 | Re-expand restores `lastExpandedFraction` | PNL-PITFALL-06 mitigation verified; store fraction not absolute px |
| PNL-04 | Fraction-based sizing survives window resize | AeroSplitPane lines 107-108 pattern verified verbatim |
| PNL-05 | Drag divider between adjacent expanded sections | aeroDragSplitter Orientation.Vertical + clampDividerPx verified verbatim |
| PNL-06 | Grip only between two expanded sections; static join elsewhere | zipWithNext filter pattern documented |
| PNL-07 | Collapse 200ms FastOutSlowInEasing; drag direct px write; coexistence spike | PNL-PITFALL-01 architecture: split intent-state from display-state |
| PNL-08 | Hybrid controlled/uncontrolled expansion | AeroAccordion lines 117-164 verified; exact pattern to mirror |
| PNL-09 | `onLayoutChange` on drag-end and toggle; internal uncontrolled sizing | Consistent with AeroSplitPane uncontrolled model |
| PNL-10 | Per-section `minSize`; N-section clamp (Σ minSizes below divider) | PNL-PITFALL-04 documented; clampPanelDividerPx new pure function |
| PNL-11 | `collapsible = false`: no chevron, participates in resize | Section flag; no special layout path needed |
| PNL-12 | `resizable = false`: dividers without grip, drag disabled | `aeroDragSplitter(enabled = false)` parameter verified |
| PNL-13 | `key` per section for stable identity | `key(section.id)` in render loop; PNL-PITFALL-08 |
| PNL-14 | Header: `glassPanel` + CaretRight 0°→90°, `leadingIcon`, `headerActions` | AeroAccordion AccordionSectionRow pattern verified |
| PNL-15 | Edge cases: all collapsed = header stack; one expanded = fills availableForExpanded | computeAvailablePx + distributePx cover this |
| PNL-16 | Pure-logic unit tests TDD pattern (PanelGroupLogicTest.kt) | SplitClampTest.kt / AccordionToggleTest.kt structure verified |
| PNL-17 | Showcase LayoutSection.kt; three-theme visual sign-off | Append-only pattern established in prior milestones |
| PNL-18 | KDoc with REQ-ID and PITFALL references | AeroSplitPane KDoc style is the template |

</phase_requirements>

---

## Summary

Phase 13 is purely additive: one new layout component built entirely from APIs already on the compile classpath. The milestone research files (SUMMARY.md, ARCHITECTURE.md, PITFALLS.md, STACK.md) are confirmed accurate after direct source reading. Every internal API the context relies on was found with exactly the signatures described.

The single unresolved uncertainty is empirical: whether `animateFloatAsState` with per-frame target updates (during drag) introduces visible lag. The architectural argument (target changes every frame faster than the 200ms window, so animation always snaps to current target) is sound, and the PITFALLS.md correctly documents the split intent/display-state fallback. The spike (build step 1) resolves this definitively before any UI code is written.

All other decisions are direct ports of shipped and verified patterns. The plan can be written with HIGH confidence across the board.

**Primary recommendation:** Follow the 8-step build order from CONTEXT.md/STATE.md without deviation. Write the spike composable first in the showcase (throwaway), then write PanelDistributionTest.kt RED before PanelDistribution.kt.

---

## Standard Stack

### Core (All Already on Compile Classpath — Zero New Dependencies)

| API | Location | Purpose | Verified |
|-----|----------|---------|---------|
| `BoxWithConstraints` | `androidx.compose.foundation.layout` | Read `constraints.maxHeight.toFloat()` as `totalPx` once at composition | YES — used identically in AeroSplitPane line 98 |
| `animateFloatAsState` + `tween(200, FastOutSlowInEasing)` | `androidx.compose.animation.core` | Animate section height on collapse/expand; animate caret rotation | YES — used in AeroAccordion line 198 (tween 160ms) and AeroSidebar line 72 (tween 200ms) |
| `Modifier.aeroDragSplitter(orientation, onDrag, onDragEnd, enabled)` | `com.mordred.aero.components.internal.drag` | Drag resize between adjacent expanded sections | YES — signature verified: `fun Modifier.aeroDragSplitter(orientation: Orientation, onDrag: (deltaPx: Float) -> Unit, onDragEnd: () -> Unit = {}, enabled: Boolean = true)` |
| `clampDividerPx(currentPx, deltaPx, minFirstPx, maxPx)` | `com.mordred.aero.components.layout.internal.splitpane` | Clamp drag position; inverted-range guard already inside | YES — `SplitClamp.kt` line 22: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` |
| `rememberUpdatedState(totalPx)` | `androidx.compose.runtime` | Live totalPx in drag lambda | YES — AeroSplitPane line 116: `val liveTotalPx by rememberUpdatedState(totalPx)` |
| `mutableStateListOf` | `androidx.compose.runtime` | Per-section sizePx, expanded, lastExpandedFraction state list | YES |
| `Modifier.glassPanel(cornerRadius = 8.dp)` | `com.mordred.aero.theme` | Header strip glass surface | YES — GlassModifiers.kt line 54: `fun Modifier.glassPanel(cornerRadius: Dp = 0.dp)` |
| `AeroIcons.CaretRight` | `com.mordred.aero.icons` (extension property on AeroIcons object) | Chevron in header, rotates 0°→90° | YES — CaretRight.kt line 12: `public val AeroIcons.CaretRight: ImageVector` |
| `Modifier.graphicsLayer { rotationZ = caretRotation }` | `androidx.compose.ui` | Caret rotation without layout pass | YES — AeroAccordion line 241 |
| `LocalDensity.current` | `androidx.compose.ui.platform` | Convert dp constants to px once at composition | YES — AeroSplitPane line 87 |
| `kotlin.test` (JUnit5) | Test classpath | Pure-logic unit tests without Compose runtime | YES — SplitClampTest.kt, AccordionToggleTest.kt both use `kotlin.test.Test` |

### Confirmed Versions (from gradle/libs.versions.toml)

| Technology | Version |
|------------|---------|
| Kotlin | 2.1.21 |
| Compose Multiplatform | 1.7.3 |
| JDK | 17 |

**Installation:** None. Zero Gradle changes.

### Internal Utilities to Reuse Verbatim

| Utility | Exact Path | Reuse Pattern |
|---------|-----------|---------------|
| `aeroDragSplitter` | `library/.../components/internal/drag/AeroDragSplitter.kt` | Apply to 8dp hit-area Box with `Orientation.Vertical`. Uses `awaitPointerEventScope` + `positionChange().y`. `pointerInput(orientation, enabled)` — do NOT add extra keys. |
| `clampDividerPx` | `library/.../components/layout/internal/splitpane/SplitClamp.kt` | Call directly for 2-section adjacent drag. NEW `clampPanelDividerPx` wraps it for N-section case. |
| `SplitPaneDivider` visual pattern | `AeroSplitPane.kt` lines 179-232 | Private composable template: 8dp hit-area Box + `hoverable` + `aeroDragSplitter` + `background(if (hovered) buttonHover else Transparent)` + 1dp visual line + 3-dot grip in a `Row` (for vertical orientation). |

### NOT to Reuse (Despite Being in SplitClamp.kt)

`fractionToPx` and `pxToFraction` are NOT needed. PanelGroup stores absolute px weights, not 0..1 fractions. Using them imports a false analogy.

---

## Architecture Patterns

### Recommended File Structure

```
library/src/main/kotlin/com/mordred/aero/
  components/
    layout/
      AeroPanelGroup.kt                    NEW: public composables + AeroPanelGroupScope
      internal/
        panelgroup/
          PanelDistribution.kt             NEW: pure JVM logic (no Compose imports)
        splitpane/
          SplitClamp.kt                    REUSED verbatim (zero changes)
    internal/
      drag/
        AeroDragSplitter.kt                REUSED verbatim (zero changes)

library/src/test/kotlin/com/mordred/aero/components/layout/
  PanelGroupLogicTest.kt                   NEW: pure-logic TDD tests
  SplitClampTest.kt                        EXISTING (no changes)
  AccordionToggleTest.kt                   EXISTING (no changes)

showcase/src/main/kotlin/com/mordred/showcase/sections/
  LayoutSection.kt                         MODIFIED: append-only AeroPanelGroup demo block
```

### Pattern 1: Fraction-as-Stable-Coordinate (from AeroSplitPane, verified)

Store section sizes as raw px weights with NO `remember(totalPx)` key. Derive render heights from current `totalPx` each recompose. This is the PITFALL-A fix shipped in v2.0.1.

Verified source (AeroSplitPane.kt lines 107-108):
```kotlin
// Fraction is the stable, viewport-independent coordinate. Pixel position is derived every
// recompose from totalPx, so a parent resize (or a parent-drag totalPx change in nested
// layouts) re-anchors proportionally instead of resetting to initialSplitFraction (PITFALL-A).
var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
val dividerPx = fractionToPx(dividerFraction, totalPx)
```

For AeroPanelGroup (N-section generalization):
```kotlin
// In BoxWithConstraints:
val totalPx = constraints.maxHeight.toFloat()
// State: remember with NO key — stable across window resize
val sizePx = remember { mutableStateListOf(*initialSizes) }
val expanded = remember { mutableStateListOf(*initialExpanded) }
val lastExpandedFraction = remember { mutableStateListOf(*initialFractions) }

// Derived each recompose (NOT stored):
val collapsedHeaderCount = expanded.count { !it }
val activeDividerCount = expanded.zipWithNext().count { (a, b) -> a && b }
val availableForExpanded = totalPx
    - collapsedHeaderCount * headerHeightPx
    - activeDividerCount * dividerThicknessPx
val expandedSizeSum = sizePx.indices.filter { expanded[it] }.sumOf { sizePx[it].toDouble() }.toFloat()
fun renderHeight(i: Int): Float =
    if (!expanded[i]) headerHeightPx
    else if (expandedSizeSum > 0f) (sizePx[i] / expandedSizeSum) * availableForExpanded
    else availableForExpanded  // only one section left expanded
```

### Pattern 2: Live-State in Drag Loop (from AeroSplitPane v2.0.1 fix FIXSP-01, verified)

Verified source (AeroSplitPane.kt lines 115-124):
```kotlin
// aeroDragSplitter keys its pointerInput on (orientation, enabled) only, so the onDrag
// closure below is captured ONCE and never recreated. It must therefore read live state,
// not frozen locals: dividerFraction is a MutableState (read live through its delegate),
// and totalPx is wrapped in rememberUpdatedState so a parent resize / outer-pane drag in
// nested layouts is always reflected. Capturing the plain `dividerPx` val here instead
// re-introduces the F9 stale-capture bug (divider jitters and snaps back to its start).
val liveTotalPx by rememberUpdatedState(totalPx)
val onDrag: (Float) -> Unit = { delta ->
    val minFirstPx = with(density) { minFirstPaneSize.toPx() }
    val maxPx = liveTotalPx - with(density) { minSecondPaneSize.toPx() }
    val currentPx = fractionToPx(dividerFraction, liveTotalPx)
    val newPx = clampDividerPx(currentPx, delta, minFirstPx, maxPx)
    dividerFraction = pxToFraction(newPx, liveTotalPx)
    onSplitChange?.invoke(dividerFraction)
}
```

For AeroPanelGroup drag lambda:
```kotlin
val liveTotalPx by rememberUpdatedState(totalPx)
// sizePx is SnapshotStateList — reads inside lambda are ALWAYS live (no rememberUpdatedState needed)
val onDragBetween: (above: Int, below: Int, delta: Float) -> Unit = { above, below, delta ->
    val combined = sizePx[above] + sizePx[below]
    val minPx = with(density) { minSectionSize.toPx() }
    // For N-section: compute minBelow = Σ minSizes of all sections below divider
    val newAbove = clampDividerPx(sizePx[above], delta, minPx, combined - minPx)
    sizePx[above] = newAbove
    sizePx[below] = combined - newAbove
    // onLayoutChange fires only on onDragEnd (not here)
}
```

### Pattern 3: Animation-vs-Drag Coexistence (PNL-PITFALL-01 — THE SPIKE QUESTION)

**Recommended state shape** (from PITFALLS.md — architecturally sound, empirically unverified):

The key insight: `animateFloatAsState` does NOT write to sizePx — it only READS it as the `targetValue`. Drag writes to `sizePx`. These are different writers for different state. No conflict.

```kotlin
// sizePx[i] is the single source of truth (drag writes here)
// renderHeight(i) is derived from sizePx (see Pattern 1 above)
// animateFloatAsState READS renderHeight(i) as its target — does NOT write back

val animatedHeight = sections.indices.map { i ->
    animateFloatAsState(
        targetValue = if (expanded[i]) renderHeight(i) else headerHeightPx,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "panelHeight_$i",
    ).value
}
// Layout uses animatedHeight[i] for Modifier.height()
```

**Why drag and animation coexist without oscillation:** During drag, `sizePx[above]` changes every pointer event → `renderHeight(above)` recalculates → `animateFloatAsState` receives a new `targetValue` every frame. Because drag events arrive at ~60fps and the tween is 200ms, the animation always chases the current target with sub-frame lag. The animation never overwrites `sizePx` — it only reads it.

**Fallback if spike fails** (adds ~20 lines):
```kotlin
// Split intent-state from display-state
var sizeFraction by remember { mutableStateOf(initialFraction) }
val animTargetFraction = if (expanded) sizeFraction else 0f
val displayFraction by animateFloatAsState(targetValue = animTargetFraction, ...)

// Guard: disable drag during animation in-flight on either neighbor
val onDragBetween = { above, below, delta ->
    val upperDone = animatedHeight[above] == renderHeight(above)
    val lowerDone = animatedHeight[below] == renderHeight(below)
    if (!upperDone || !lowerDone) return@onDrag  // animation in flight
    // ... apply drag
}
```

### Pattern 4: Hybrid Controlled/Uncontrolled Expansion (from AeroAccordion, verified)

Verified source (AeroAccordion.kt lines 117-164). The exact pattern:

```kotlin
// Line 117:
val controlled = onExpandedChange != null

// Lines 129-138 (expansion state derivation):
val expanded: Boolean = if (controlled) {
    when (mode) {
        AeroAccordionMode.Single -> expandedIndex == index
        AeroAccordionMode.Multi  -> expandedIndices?.contains(index) == true
    }
} else {
    when (mode) {
        AeroAccordionMode.Single -> internalExpandedSingle == index
        AeroAccordionMode.Multi  -> index in internalExpandedSet
    }
}
```

For AeroPanelGroup (Multi only, key-based, not index-based):
```kotlin
val controlled = onExpandedChange != null
var internalExpanded by remember { mutableStateOf(initiallyExpanded) }  // Set<String> of keys

fun isExpanded(key: String): Boolean =
    if (controlled) expandedKeys?.contains(key) == true
    else key in internalExpanded

fun onToggle(key: String) {
    if (controlled) {
        val next = if (key in (expandedKeys ?: emptySet()))
            (expandedKeys ?: emptySet()) - key
        else
            (expandedKeys ?: emptySet()) + key
        onExpandedChange!!(next)
    } else {
        internalExpanded = if (key in internalExpanded)
            internalExpanded - key
        else
            internalExpanded + key
    }
}
// KDoc MUST say: "Do not collapse to one branch — both paths are intentional (AeroAccordion convention)"
```

### Pattern 5: Scope-DSL (from AeroSidebarScope, verified)

Verified source (AeroSidebarState.kt lines 116-230). The DSL pattern:

```kotlin
// AeroSidebarScope is constructed fresh each recompose (PITFALL-11 contract from Phase 10):
public class AeroSidebarScope internal constructor(internal val mode: AeroSidebarMode) {
    @Composable
    public fun item(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) { ... }

    @Composable
    public fun section(label: String) { ... }

    @Composable
    public fun divider() { ... }
}
// Usage: AeroSidebarScope(mode = state.mode).content()
```

For AeroPanelGroup, the scope collects section registrations:
```kotlin
public class AeroPanelGroupScope internal constructor() {
    internal val sections = mutableListOf<AeroPanelSectionConfig>()

    @Composable
    public fun section(
        key: String,
        title: String,
        minSize: Dp = DEFAULT_SECTION_MIN_SIZE,
        collapsible: Boolean = true,
        defaultExpanded: Boolean = true,
        leadingIcon: ImageVector? = null,
        headerActions: (@Composable RowScope.() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) { sections.add(AeroPanelSectionConfig(key, title, minSize, collapsible, defaultExpanded, leadingIcon, headerActions, content)) }
}
```

### Pattern 6: SplitPaneDivider Visual Structure (verified, to mirror for PanelGroupDivider)

Verified source (AeroSplitPane.kt lines 179-232). Key structure for PanelGroupDivider (vertical orientation — uses Row for dots):

```kotlin
// The divider uses Row of 3 dots (not Column) for vertical orientation (horizontal drag line)
Row(
    modifier = Modifier.align(Alignment.Center),
    verticalAlignment = Alignment.CenterVertically,
) {
    repeat(3) { idx ->
        if (idx > 0) Box(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.size(3.dp).background(colors.labelText))
    }
}
```

Full PanelGroupDivider structure: 8dp hit-area height + `hoverable` + `aeroDragSplitter(Orientation.Vertical, onDrag)` + `background(if (hovered) buttonHover else Transparent)` + centered 1dp horizontal `Box(colors.borderDefault)` + 3 horizontal dots in a Row.

### Pattern 7: Header Row (from AeroAccordion AccordionSectionRow, verified)

Verified source (AeroAccordion.kt lines 192-259). Key elements:

```kotlin
val caretRotation by animateFloatAsState(
    targetValue = if (expanded) 90f else 0f,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "caretRotation",
)
// ...
Row(
    modifier = Modifier
        .fillMaxWidth()
        .glassPanel(cornerRadius = 8.dp)
        .clip(RoundedCornerShape(8.dp))   // F-ACCORDION-HOVER: clip before clickable
        .clickable { onToggle() }
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) { ... }
// Caret:
Icon(
    imageVector = AeroIcons.CaretRight,
    contentDescription = null,
    tint = AeroTheme.colors.onSurface,
    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = caretRotation },
)
```

For AeroPanelGroup header: same structure, but header height is fixed ~36dp (NOT content-sized), and `headerActions` slot goes to the RIGHT in a separate non-propagating click zone.

### Pattern 8: glassPanel Modifier Signature (verified)

Verified source (GlassModifiers.kt lines 54-73):
```kotlin
@Composable
public fun Modifier.glassPanel(cornerRadius: Dp = 0.dp): Modifier {
    val colors = LocalAeroColors.current
    val panelBackground = colors.panelBackground
    val glassHighlight = colors.glassHighlight
    return this.drawBehind {
        val cornerPx = cornerRadius.toPx()
        val cr = CornerRadius(cornerPx, cornerPx)
        drawRoundRect(color = panelBackground, cornerRadius = cr)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(glassHighlight, Color.Transparent),
                startY = 0f, endY = size.height * 0.55f
            ),
            cornerRadius = cr
        )
    }
}
```

Default is `cornerRadius = 0.dp`. For section headers (flush full-width strips), use `glassPanel()` (default 0.dp). For rounder look matching Accordion, use `glassPanel(cornerRadius = 8.dp)` — the CONTEXT.md specifies `cornerRadius = 8.dp`.

### Pattern 9: AeroIcons.CaretRight (verified)

Verified source (CaretRight.kt line 12):
```kotlin
// Extension property on AeroIcons companion object — NOT a nested class
public val AeroIcons.CaretRight: ImageVector
    get() { ... }
```

Usage: `AeroIcons.CaretRight` — same access pattern as in AeroAccordion.

### 8-Step Build Order (Non-Negotiable)

| Step | Deliverable | Gate Before Proceeding |
|------|------------|----------------------|
| 1 | **Animation-vs-drag SPIKE** — throwaway showcase composable with 3 sections, `animateFloatAsState` targeting `sizePx[i]`, one `aeroDragSplitter` between sections 0-1, one toggle button | Drag is instant; collapse animates 200ms; immediate post-collapse drag causes no snap-back or oscillation |
| 2 | **Pure logic TDD** — `PanelDistribution.kt` (no Compose imports) + `PanelGroupLogicTest.kt` RED→GREEN | All tests GREEN covering: `distributePx`, `shareTransferOnCollapse`, `shareTransferOnExpand`, `computeAvailablePx`, `clampPanelDividerPx`, `lastExpandedFraction` restore |
| 3 | **Layout skeleton** — `BoxWithConstraints`, `mutableStateListOf` fraction state, `key(section.id)` render loop, static dividers (no drag yet) | Window resize redistributes heights proportionally; collapse/expand changes heights correctly |
| 4 | **Collapse/expand animation** — 200ms `FastOutSlowInEasing`, `shareTransferOnCollapse`/`shareTransferOnExpand`, `lastExpandedFraction` save/restore | 200ms animation visible; concurrent animations on multiple sections do not conflict |
| 5 | **Drag resize** — `aeroDragSplitter` on `PanelGroupDivider`, `rememberUpdatedState(totalPx)`, `onLayoutChange` on drag-end | Drag is instant; post-window-resize drag does not snap back |
| 6 | **Controlled expansion path + KDoc** — `onExpandedChange`/`expandedKeys` parameters, KDoc with REQ-ID + PITFALL refs | Both controlled and uncontrolled branches present; KDoc has "do not collapse to one branch" comment |
| 7 | **Aero visual polish** — `glassPanel(8.dp)` header, CaretRight 0→90°, `headerActions` slot, grip dots, `collapsible`/`resizable` flags | Win7 Aero glass gloss on headers; caret animates; `headerActions` visible in both states; no drag cursor on collapsed boundaries |
| 8 | **Showcase demo + sign-off** — append to `LayoutSection.kt`; three-theme visual sign-off | AeroBlue / AeroDark / Classic all pass visual inspection |

### Anti-Patterns to Avoid

- **`remember(totalPx)` on size state**: Resets all section sizes on every window resize (PITFALL-A). Use `remember { mutableStateListOf(...) }` with no key.
- **Stale capture in drag lambda**: Reading `sizePx[i]` as a plain local val in composable scope. `SnapshotStateList` reads inside the lambda are always live — read directly through `sizePx[i]`.
- **`animateContentSize`**: Requires content measurement, fights explicit `.height(dp)`, incompatible with px-driven section heights. AeroAccordion uses it (content-sized), AeroPanelGroup must NOT.
- **`Modifier.weight(fraction)` for all sections**: N layout passes per drag frame. Give sections [0..N-2] explicit `Modifier.height(dp)`, last expanded section `Modifier.weight(1f)` (last-section remainder rule).
- **Separate divider state map**: Requires sync with `expanded[]`. Instead, derive divider existence inline from `isExpanded(i) && isExpanded(i+1)`.
- **`detectDragGestures`**: BANNED (PITFALL-03, touchSlop=18dp breaks first-pixel mouse drag on Desktop).
- **`SubcomposeLayout`**: BANNED — use `BoxWithConstraints` + arithmetic.
- **Storing `lastExpandedPx` (absolute px)**: Overflows after window shrink. Store `lastExpandedFraction` instead.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Drag resize with cursor change | Custom `pointerInput` loop | `Modifier.aeroDragSplitter(Orientation.Vertical, onDrag)` from `AeroDragSplitter.kt` | Already handles awaitPointerEventScope, cursor change, positionChange() single-frame delta, consume semantics (F3/F15 fixes) |
| Divider px clamp with inverted-range safety | Custom `coerceIn` call | `clampDividerPx(currentPx, deltaPx, minFirstPx, maxPx)` from `SplitClamp.kt` | Already has `safeMax = maxPx.coerceAtLeast(minFirstPx)` PITFALL-B guard on line 22 |
| Glass header surface | Custom `drawBehind` gradient | `Modifier.glassPanel(cornerRadius = 8.dp)` | Single drawBehind pass, panelBackground + glassHighlight gradient; performance baseline |
| Chevron icon | Custom SVG path | `AeroIcons.CaretRight` (extension property, `public val AeroIcons.CaretRight: ImageVector`) | Phosphor-sourced, already vendored, matches naming convention |
| Caret rotation animation | Custom tween setup | `animateFloatAsState(if (expanded) 90f else 0f, tween(160, FastOutSlowInEasing))` + `graphicsLayer { rotationZ = value }` | Exact AeroAccordion pattern; 200ms for section height, 160ms for caret (or match accordion's 160ms) |
| N-section drag clamp | Custom per-section loop | New `clampPanelDividerPx` (wraps `clampDividerPx` idea, takes full sections array for Σ-minSize accounting) | Pure-logic function tested TDD before Compose code |
| Px redistribution on collapse/expand | Inline ad-hoc math | `shareTransferOnCollapse` / `shareTransferOnExpand` in `PanelDistribution.kt` | Pure JVM, unit-tested, no Compose dependency |

**Key insight:** The entire drag infrastructure, visual system, and animation spec are already shipped. The only genuinely new code is PanelDistribution.kt (pure JVM) and AeroPanelGroup.kt (Compose). Everything else is verbatim reuse.

---

## Common Pitfalls

### PNL-PITFALL-01: Animation vs. Drag State Conflict
**Current code reference:** No current code — this is the empirical unknown. Architecture research in PITFALLS.md is the guide.
**What goes wrong:** If `animateFloatAsState` and drag both try to write to the same `sizePx` state, the animation overwrites drag values on each frame → oscillation, snap-back.
**Correct architecture:** `animateFloatAsState` READS `sizePx` as its `targetValue`. Drag WRITES to `sizePx`. These are different sides — no conflict. Animation never writes to `sizePx`.
**Spike gate:** Collapse section A, then IMMEDIATELY drag the adjacent divider — no snap-back, no oscillation.
**Fallback:** Split into `sizeFraction` (intent, drag writes) and `displayFraction` (animated read-only). Gate drag on `displayFraction != sizeFraction`. Adds ~20 lines.
**Build step:** 1 (mandatory first).

### PNL-PITFALL-04: N-Section Cascading Clamp Crash
**Current code reference:** `SplitClampTest.kt` line 52: `clampInvertedRangeDoesNotThrow` — exact template.
**What goes wrong:** With 3+ expanded sections, `coerceIn(minPx, maxPx)` where `maxPx < minPx` throws `IllegalArgumentException` (same class as PITFALL-B fixed in v2.0.1).
**Root cause:** Naive 2-pane clamp only looks at direct neighbors. Must sum Σ minSizes of ALL sections below/above the divider.
**Fix:**
```kotlin
internal fun clampPanelDividerPx(
    aboveSizePx: Float, deltaPx: Float,
    minAbovePx: Float,   // Σ minSizes of all expanded sections above divider (including direct upper)
    minBelowPx: Float,   // Σ minSizes of all expanded sections below divider
    totalBudgetPx: Float, // sum of aboveSizePx + belowSizePx (not totalPx)
): Float {
    val maxAbovePx = (totalBudgetPx - minBelowPx).coerceAtLeast(minAbovePx)  // PITFALL-B guard
    return (aboveSizePx + deltaPx).coerceIn(minAbovePx, maxAbovePx)
}
```
**TDD test (RED first):** `available=100px, three sections each minSizePx=60 → no IllegalArgumentException`.
**Build step:** 2 (TDD in PanelGroupLogicTest.kt).

### PITFALL-A (Carry-Forward): `remember(totalPx)` Re-Keys Size State
**Current code reference:** AeroSplitPane.kt lines 107-108 and their KDoc — the fix is already there.
**What goes wrong:** `remember(totalPx) { mutableStateListOf(...) }` resets all section sizes to initial values on every window resize or parent pane drag.
**Fix:** `val sizePx = remember { mutableStateListOf(*initialSizes) }` — no key, always.
**Build step:** All steps — never add a key to size state.

### PITFALL-B (Carry-Forward): Inverted Range in `clampDividerPx`
**Current code reference:** SplitClamp.kt line 22: `val safeMax = maxPx.coerceAtLeast(minFirstPx)`.
**What goes wrong:** When window is squeezed below `Σ minSizes`, `coerceIn(min, max)` throws if `max < min`.
**Fix:** Already in `clampDividerPx`. For N-section version, replicate the same guard in `clampPanelDividerPx`.
**Build step:** 2 (TDD).

### FIXSP-01 (Carry-Forward): Stale Capture in Drag Lambda
**Current code reference:** AeroSplitPane.kt line 116: `val liveTotalPx by rememberUpdatedState(totalPx)` and its KDoc comment ("re-introduces the F9 stale-capture bug").
**What goes wrong:** `aeroDragSplitter` captures `onDrag` once (keyed on `orientation, enabled`). A plain `val totalPx` read in the composable and closed over in `onDrag` is the stale value from first composition.
**Fix:** `rememberUpdatedState(totalPx)` for totalPx. `SnapshotStateList` reads inside the lambda are always live — no extra wrapper needed for `sizePx[i]`.
**Build step:** 5 (drag resize implementation).

### PNL-PITFALL-06: `lastExpandedPx` Overflows After Window Shrink
**Current code reference:** Documented in PITFALLS.md. No current misimplementation to reference.
**What goes wrong:** `lastExpandedPx` captured as absolute px at collapse time. Window shrinks. Re-expand tries to restore that absolute px into a smaller `availableForExpanded` → overflow.
**Fix:** Store `lastExpandedFraction = sizePx[i] / availableForExpanded` at collapse time. Restore as `lastExpandedFraction * currentAvailableForExpanded`.
**Build step:** 4 (collapse/expand animation).

### PNL-PITFALL-07: Wrong Active Divider Count in `computeAvailablePx`
**Current code reference:** AeroSplitPane.kt only has 1 divider, so this is new territory.
**What goes wrong:** Counting `expandedCount - 1` as divider count is wrong when sections alternate E/C/E. Two non-adjacent expanded sections do NOT share a divider.
**Fix:**
```kotlin
val activeDividerCount = expanded.zipWithNext().count { (a, b) -> a && b }
```
This is the CONTEXT.md-mandated formula. Pattern E/E/C/E gives 1 active divider (only pair 0-1), not 3.
**Build step:** 2 (TDD in `computeAvailablePx`).

### PNL-PITFALL-08: Section State Re-Key on List Reorder
**Current code reference:** AeroSplitPane.kt doesn't have multiple sections, so this is new.
**What goes wrong:** Using `key(index)` in the render loop resets section state (sizeFraction, expanded) when sections are reordered or inserted.
**Fix:** `key(section.key)` in the render loop where `key` is the caller-supplied stable string identifier.
**Build step:** 3 (layout skeleton).

### PNL-PITFALL-09: Drag Grip on Collapsed/Expanded Boundary
**What goes wrong:** Rendering a `PanelGroupDivider` between an expanded section and a collapsed header. User sees resize cursor on a non-resizable boundary.
**Fix:** Conditional in render loop: `if (isExpanded(i) && i < sections.lastIndex && isExpanded(i+1))`. Static 1dp line only otherwise.
**Build step:** 3 (layout skeleton).

### PNL-PITFALL-10: `animateContentSize` on Section Height
**Current code reference:** AeroAccordion.kt line 250: `Modifier.animateContentSize(...)` — this is correct for content-sized accordion but WRONG for AeroPanelGroup.
**What goes wrong:** `animateContentSize` measures content height; AeroPanelGroup height is NOT content-determined but px-state-determined. Using it fights explicit `.height(dp)` constraints and intercepts drag writes.
**Fix:** `Modifier.height(with(density) { animatedHeight[i].toDp() })` where `animatedHeight[i]` comes from `animateFloatAsState`.
**Build step:** 3 (layout skeleton).

### PNL-PITFALL-11: `Modifier.weight` for All Sections
**What goes wrong:** All sections with `weight(fraction)` causes N layout passes per drag frame.
**Fix:** Sections [0..N-2]: explicit `Modifier.height(displaySizeDp)`. Last EXPANDED section: `Modifier.weight(1f)` (absorbs float rounding, guarantees `Σ = availableForExpanded`). When last section is collapsed, find `lastExpandedIndex` and give it `weight(1f)`.
**Build step:** 3 (layout skeleton).

### PNL-PITFALL-02: Float Drift / Rounding on Window Resize
**What goes wrong:** `Σ(sizePx[i])` may differ from `availableForExpanded` by 0.5–2px after repeated resize events due to float rounding.
**Fix:** Last-section remainder rule (last expanded section gets `weight(1f)` which naturally absorbs rounding) OR explicit re-normalize after each structural change. The `weight(1f)` approach (PNL-PITFALL-11 fix) simultaneously solves this.
**Build step:** 2 (pure-logic test) and 3 (layout).

### PNL-PITFALL-03: Mid-Drag Collapse Race
**What goes wrong:** User mid-drags divider A/B; programmatic toggle collapses B. Delta still applies in old coordinate space. Divider teleports.
**Fix:** Use a `dragging: Boolean` state flag. Toggle handler is guarded by `if (!dragging)`. Set `dragging = true` on first pointer down in `aeroDragSplitter` `onDrag`, `false` on `onDragEnd`. OR re-compute `availableForExpanded` live every drag frame (already done by reading `sizePx` via SnapshotStateList).
**Build step:** 5 (drag resize).

### PNL-PITFALL-05: Share-Transfer Rounding Gap
**What goes wrong:** Distributing a collapsed section's `sizePx` to N neighbors in floating-point: `Σ(share * N) < collapsedSizePx` by rounding error → hairline gap at bottom.
**Fix:** Last-neighbor absorbs remainder: compute share for all but last, last gets `collapsedSizePx - Σ(others' share)`.
**Build step:** 2 (TDD in `shareTransferOnCollapse`).

---

## Code Examples

### Verified: clampDividerPx with safeMax guard (SplitClamp.kt)
```kotlin
// SplitClamp.kt lines 21-24 — reuse verbatim for 2-section adjacent drag
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float {
    val safeMax = maxPx.coerceAtLeast(minFirstPx)   // guard: coerceIn(a,b) throws when a > b (PITFALL-B)
    return (currentPx + deltaPx).coerceIn(minFirstPx, safeMax)
}
```

### Verified: aeroDragSplitter signature (AeroDragSplitter.kt)
```kotlin
// Full signature — note onDragEnd and enabled have defaults
internal fun Modifier.aeroDragSplitter(
    orientation: Orientation,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,
): Modifier = composed { ... }
// Usage for PanelGroupDivider:
Modifier.aeroDragSplitter(orientation = Orientation.Vertical, onDrag = { delta -> onDragBetween(above, below, delta) }, onDragEnd = { onLayoutChange?.invoke(sizePx.toList()) })
```

### Verified: BoxWithConstraints + fraction state pattern (AeroSplitPane.kt)
```kotlin
// Lines 98-108 — the exact pattern to mirror
BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val totalPx = if (orientation == AeroSplitOrientation.Horizontal)
        constraints.maxWidth.toFloat()
    else
        constraints.maxHeight.toFloat()

    var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
    val dividerPx = fractionToPx(dividerFraction, totalPx)
    // ...
}
// For AeroPanelGroup: totalPx = constraints.maxHeight.toFloat() (vertical only)
```

### Verified: SplitClampTest structure (template for PanelGroupLogicTest)
```kotlin
// AccordionToggleTest.kt — exact file structure to mirror
class AccordionToggleTest {
    @Test
    fun singleOpenNewSectionClosesPrevious() {
        assertEquals(1, accordionToggleSingle(expandedIndex = 0, clickedIndex = 1))
    }
    // ... each @Test is a pure function call with assertEquals
}
// PanelGroupLogicTest.kt: same pattern — pure function calls, no Compose runtime
```

### Verified: animateFloatAsState caret rotation (AeroAccordion.kt)
```kotlin
// AccordionSectionRow lines 198-202
val caretRotation by animateFloatAsState(
    targetValue = if (expanded) 90f else 0f,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "caretRotation",
)
// ...
Icon(
    imageVector = AeroIcons.CaretRight,
    contentDescription = null,
    tint = AeroTheme.colors.onSurface,
    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = caretRotation },
)
```

### Verified: AeroSidebar scope DSL construction (AeroSidebar.kt line 92)
```kotlin
// Scope is constructed fresh each recompose (PITFALL-11 contract)
AeroSidebarScope(mode = state.mode).content()
// For AeroPanelGroup: scope collects registrations, then layout is derived
```

### Verified: AeroAccordion controlled/uncontrolled hybrid (lines 117-165)
```kotlin
val controlled = onExpandedChange != null
// Uncontrolled internal state:
var internalExpandedSet by remember { mutableStateOf(initiallyExpanded.toSet()) }
// Controlled derivation:
val expanded: Boolean = if (controlled) {
    expandedIndices?.contains(index) == true
} else {
    index in internalExpandedSet
}
val onToggle: () -> Unit = {
    if (controlled) {
        onExpandedChange!!(accordionToggleMulti(expandedIndices ?: emptySet(), index))
    } else {
        internalExpandedSet = accordionToggleMulti(internalExpandedSet, index)
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | Source |
|--------------|------------------|--------|
| `remember(totalPx)` on divider state | `remember { ... }` with no key; derive px each recompose | AeroSplitPane v2.0.1 FIXSP-01 (commit 7f38c0c) |
| `detectDragGestures` for splitter drag | `Modifier.aeroDragSplitter` with `awaitPointerEventScope` | Phase 7 locked decision (PITFALL-03) |
| `coerceIn(min, max)` without guard | `safeMax = max.coerceAtLeast(min)` before `coerceIn` | SplitClamp.kt PITFALL-B fix (v2.0.1) |
| `animateContentSize` for section heights | `animateFloatAsState` targeting explicit px, `Modifier.height(dp)` | AeroPanelGroup new requirement |
| Store absolute `lastExpandedPx` | Store `lastExpandedFraction = px / available` | PNL-PITFALL-06 (new for AeroPanelGroup) |

**Deprecated/not applicable:**
- `fractionToPx` / `pxToFraction` from SplitClamp.kt: bilateral 2-pane model only; not applicable to N-section px redistribution.
- `animateContentSize`: correct for content-sized AeroAccordion; BANNED for px-driven AeroPanelGroup sections.

---

## Open Questions

1. **Animation-vs-drag coexistence in this codebase (PNL-PITFALL-01)**
   - What we know: Architecture is sound (animateFloatAsState reads sizePx as target, drag writes to sizePx — different sides). Fallback state shape documented.
   - What's unclear: Whether the practical behavior on 60fps desktop with 200ms tween shows any visible lag during rapid drag.
   - Recommendation: The spike (build step 1) is the definitive answer. Proceed to step 2 only after the spike gate passes.

2. **Exact `section(...)` parameter signature (Claude's Discretion)**
   - What we know: `key`, `title`, `minSize`, `collapsible`, `defaultExpanded`, `leadingIcon`, `headerActions`, `content` — all required per CONTEXT.md. Parameter order and defaults are discretion.
   - Recommendation: Match AeroAccordionSection parameter order where it overlaps (`title`, `leadingIcon`, `content`); put `key` first (mandatory), `collapsible` and `defaultExpanded` before optional slots.

3. **`resizable = false` scope (section-level vs group-level)**
   - What we know: CONTEXT.md says `resizable = false` per section disables dividers. Per-section flag is more granular than a group-level parameter.
   - What's unclear: Whether `resizable` on the GROUP (not per-section) is also needed.
   - Recommendation: Per-section `resizable: Boolean = true` in `section(...)` parameters. Group-level equivalent is not mentioned in requirements.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | kotlin.test (JUnit) — existing, no setup needed |
| Config file | `library/build.gradle.kts` — `testImplementation(kotlin("test"))` already present |
| Quick run command | `./gradlew :library:test --tests "*.PanelGroupLogicTest"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PNL-07 (spike) | animateFloatAsState + drag coexist without snap-back | manual smoke test | Manual only — Compose animation, no pure-logic equivalent | N/A |
| PNL-04 | Fraction-based sizing survives window resize | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.distributePxWindowResizePreservesRatios"` | ❌ Wave 0 |
| PNL-02/03 | Share transfer on collapse/expand conserves px | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.shareTransferOnCollapseConservesPx"` | ❌ Wave 0 |
| PNL-10 | N-section clamp no throw on inverted range | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.clampPanelDividerPxInvertedRangeNoThrow"` | ❌ Wave 0 |
| PNL-06 (PNL-PITFALL-07) | activeDividerCount = zipWithNext filter | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.activeDividerCountEECEGivesOne"` | ❌ Wave 0 |
| PNL-03 (PNL-PITFALL-06) | lastExpandedFraction restore after shrink | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.restoreAfterShrinkDoesNotExceedAvailable"` | ❌ Wave 0 |
| PNL-15 | All collapsed → available == 0 for expanded sections | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.allCollapsedAvailableIsZero"` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test --tests "*.PanelGroupLogicTest"`
- **Per wave merge:** `./gradlew :library:test`
- **Phase gate:** Full suite green before showcase sign-off (build step 8)

### Wave 0 Gaps

- [ ] `library/src/test/.../components/layout/PanelGroupLogicTest.kt` — covers PNL-02, PNL-03, PNL-04, PNL-10, PNL-15 and pitfalls 02, 04, 05, 06, 07
- [ ] Framework and conftest: none needed — `kotlin.test` already configured (see SplitClampTest.kt and AccordionToggleTest.kt)

---

## Sources

### Primary (HIGH confidence — in-repo source files, read 2026-06-22)

- `library/.../components/layout/AeroSplitPane.kt` — lines 98-170 confirmed: BoxWithConstraints pattern, `var dividerFraction by remember { mutableStateOf(...) }` no key, `val liveTotalPx by rememberUpdatedState(totalPx)` (line 116), `aeroDragSplitter` usage, `clampDividerPx` call, `weight(1f)` second pane, SplitPaneDivider structure with 3-dot grip in Row (vertical orientation)
- `library/.../components/layout/internal/splitpane/SplitClamp.kt` — lines 21-24 confirmed: `clampDividerPx` signature, `safeMax = maxPx.coerceAtLeast(minFirstPx)` PITFALL-B guard
- `library/.../components/internal/drag/AeroDragSplitter.kt` — full file confirmed: `fun Modifier.aeroDragSplitter(orientation, onDrag, onDragEnd={}, enabled=true)`, `pointerInput(orientation, enabled)`, `positionChange()` for delta, no touchSlop, PITFALL-03 documented in KDoc
- `library/.../components/layout/AeroAccordion.kt` — lines 107-259 confirmed: hybrid controlled/uncontrolled, `val controlled = onExpandedChange != null`, `animateFloatAsState` caret rotation, `tween(160, FastOutSlowInEasing)`, `glassPanel(cornerRadius = 8.dp)`, `.clip(RoundedCornerShape(8.dp))` before `.clickable`, `AeroIcons.CaretRight` + `graphicsLayer { rotationZ }`, `animateContentSize` for content (NOT section height — negative precedent)
- `library/.../components/layout/AeroSidebarState.kt` — lines 116-230 confirmed: `class AeroSidebarScope internal constructor(internal val mode: AeroSidebarMode)`, `fun section(label: String)`, `fun item(...)`, `fun divider()` — scope DSL pattern
- `library/.../components/layout/AeroSidebar.kt` — lines 63-101 confirmed: `content: @Composable AeroSidebarScope.() -> Unit`, `AeroSidebarScope(mode = state.mode).content()`, `animateDpAsState(tween(200, FastOutSlowInEasing))` — 200ms easing precedent
- `library/.../theme/GlassModifiers.kt` — lines 54-73 confirmed: `fun Modifier.glassPanel(cornerRadius: Dp = 0.dp)` — default 0.dp, single `drawBehind` pass
- `library/.../icons/internal/CaretRight.kt` — line 12 confirmed: `public val AeroIcons.CaretRight: ImageVector` (extension property on AeroIcons companion object)
- `library/src/test/.../SplitClampTest.kt` — confirmed: `kotlin.test.Test`, `assertEquals`, pure function call structure, `clampInvertedRangeDoesNotThrow` test as template
- `library/src/test/.../AccordionToggleTest.kt` — confirmed: same structure, `accordionToggleSingle`/`Multi` calls
- `library/src/test/.../SidebarStateTest.kt` — confirmed: `targetWidthForMode` pure function test pattern
- `.planning/research/SUMMARY.md`, `ARCHITECTURE.md`, `PITFALLS.md`, `STACK.md` — all confirmed consistent with source files; no drift detected

### Secondary (MEDIUM confidence — design references cited in prior research)

- VS Code Left Sidebar UX (collapse to strip, grip only between expanded neighbors, headerActions visible on collapsed strip)
- react-resizable-panels v4 (collapsible, minSize, onLayoutChange after drag-end only)

---

## Drift Report

**No drift found between CONTEXT.md assumptions and current source code.**

Specific assertions verified:

| CONTEXT.md Claim | Verified Against Source | Status |
|-----------------|------------------------|--------|
| "mirror AeroSplitPane fraction model" | AeroSplitPane.kt lines 107-108: `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }` | CONFIRMED |
| "clampDividerPx has inverted-range guard PITFALL-B inside" | SplitClamp.kt line 22: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` | CONFIRMED |
| "aeroDragSplitter(Orientation.Vertical, onDrag)" is the drag pattern | AeroDragSplitter.kt: `internal fun Modifier.aeroDragSplitter(orientation: Orientation, onDrag: (deltaPx: Float) -> Unit, ...)` | CONFIRMED |
| "AeroAccordion controlled/uncontrolled hybrid (onExpandedChange == null → uncontrolled)" | AeroAccordion.kt line 117: `val controlled = onExpandedChange != null` | CONFIRMED |
| "glassPanel(cornerRadius = 8.dp) for header" | GlassModifiers.kt line 54: `fun Modifier.glassPanel(cornerRadius: Dp = 0.dp)` — parameter exists | CONFIRMED |
| "AeroIcons.CaretRight with graphicsLayer rotationZ" | CaretRight.kt line 12: `public val AeroIcons.CaretRight: ImageVector`; AeroAccordion.kt line 241: `graphicsLayer { rotationZ = caretRotation }` | CONFIRMED |
| "rememberUpdatedState(totalPx) in drag loop (FIXSP-01)" | AeroSplitPane.kt line 116: `val liveTotalPx by rememberUpdatedState(totalPx)` | CONFIRMED |
| "SplitClampTest / AccordionToggleTest as TDD templates" | Both files exist and use `kotlin.test.Test` + pure function assertions | CONFIRMED |
| "AeroSidebar scope DSL (AeroSidebarScope, section(), item())" | AeroSidebarState.kt lines 116+: `class AeroSidebarScope internal constructor(val mode: AeroSidebarMode)` with `@Composable fun section(label: String)` | CONFIRMED |
| "`pointerInput(orientation, enabled)` — do not add extra keys" | AeroDragSplitter.kt line 58: `.pointerInput(orientation, enabled)` | CONFIRMED |

One clarification: `AeroSidebarScope` does NOT have a `section(key, title) { content }` method — it has `section(label: String)` (a section header label, not a content slot). The AeroPanelGroupScope `section(key, title) { content }` is a new DSL shape that collects registrations, different from AeroSidebar's `section()` which is just a label row. The context's "mirror AeroSidebar scope DSL" refers to the scope-based DSL IDIOM, not the exact method signature — this is correctly understood and the CONTEXT.md DSL description is accurate.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all APIs verified against current source files
- Architecture: HIGH (one empirical gap) — all patterns are direct ports of shipped code; animation-vs-drag coexistence is architecturally sound but requires the spike
- Pitfalls: HIGH — all grounded in current source code (SplitClamp.kt PITFALL-B guard read verbatim, AeroSplitPane FIXSP-01 fix read verbatim)

**Research date:** 2026-06-22
**Valid until:** 2026-07-22 (stable library, 30-day window; no external ecosystem changes affect this)
