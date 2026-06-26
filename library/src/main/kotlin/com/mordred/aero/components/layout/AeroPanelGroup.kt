package com.mordred.aero.components.layout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.internal.drag.aeroDragSplitter
import com.mordred.aero.components.layout.internal.panelgroup.clampPanelDividerPx
import com.mordred.aero.components.layout.internal.panelgroup.computeAvailablePx
import com.mordred.aero.components.layout.internal.panelgroup.distributePx
import com.mordred.aero.components.layout.internal.panelgroup.lastExpandedFraction
import com.mordred.aero.components.layout.internal.panelgroup.restoreFromFraction
import com.mordred.aero.components.layout.internal.panelgroup.shareTransferOnCollapse
import com.mordred.aero.components.layout.internal.panelgroup.shareTransferOnExpand
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

// Internal layout constants
private val HEADER_HEIGHT = 36.dp
private val DIVIDER_THICKNESS = 8.dp
private val DEFAULT_SECTION_MIN_SIZE = 60.dp

/**
 * Internal data class holding per-section configuration collected by [AeroPanelGroupScope.section].
 */
internal class AeroPanelSectionConfig(
    val key: String,
    val title: String,
    val minSize: Dp,
    val collapsible: Boolean,
    val resizable: Boolean,
    val defaultExpanded: Boolean,
    val defaultSize: Dp?,
    val leadingIcon: ImageVector?,
    val headerActions: (@Composable RowScope.() -> Unit)?,
    val content: @Composable () -> Unit,
)

/**
 * DSL scope used inside [AeroPanelGroup]'s content lambda.
 *
 * Declare vertical sections via [section]. Sections render top-to-bottom in declaration order.
 * The scope is constructed fresh each recompose so live Compose state is captured correctly —
 * do not store references to the scope across recompositions.
 *
 * REQ: PNL-01.
 */
public class AeroPanelGroupScope internal constructor() {

    internal val sections = mutableListOf<AeroPanelSectionConfig>()

    /**
     * Declares a vertical section inside [AeroPanelGroup].
     *
     * @param key stable identity string; used as `key(section.key)` in the render loop so
     *   reordering or duplicate titles do not cause state loss. Must be unique within the group.
     *   REQ: PNL-13.
     * @param title header text displayed in the section header strip.
     * @param minSize minimum rendered height of the expanded content area. REQ: PNL-10.
     * @param collapsible when false the section header does not respond to clicks. REQ: PNL-11.
     * @param resizable when true a drag divider can resize this section against an adjacent
     *   expanded neighbor. Both neighbors must be resizable for drag to activate. REQ: PNL-12.
     * @param defaultExpanded initial expanded state when no [AeroPanelGroup.initiallyExpanded]
     *   override is present. Also the default when [AeroPanelGroup.expandedKeys] is null.
     * @param defaultSize if non-null, the section's initial sizePx seed is derived from this Dp
     *   value rather than an equal share. Useful to pre-weight sections differently.
     * @param leadingIcon optional icon displayed left of [title] in the header.
     * @param headerActions optional trailing content placed at the end of the header row. REQ: PNL-14.
     * @param content composable content rendered inside the expanded area.
     */
    public fun section(
        key: String,
        title: String,
        minSize: Dp = DEFAULT_SECTION_MIN_SIZE,
        collapsible: Boolean = true,
        resizable: Boolean = true,
        defaultExpanded: Boolean = true,
        defaultSize: Dp? = null,
        leadingIcon: ImageVector? = null,
        headerActions: (@Composable RowScope.() -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        sections.add(
            AeroPanelSectionConfig(
                key = key,
                title = title,
                minSize = minSize,
                collapsible = collapsible,
                resizable = resizable,
                defaultExpanded = defaultExpanded,
                defaultSize = defaultSize,
                leadingIcon = leadingIcon,
                headerActions = headerActions,
                content = content,
            )
        )
    }
}

/**
 * PNL-01: A scope-DSL vertical panel group with N independently collapsible and resizable sections.
 *
 * ## Expansion ownership model
 *
 * When [onExpandedChange] is `null`, the component manages expansion state internally
 * (uncontrolled). The [initiallyExpanded] set seeds the initial state; the caller is not
 * involved in subsequent toggles. REQ: PNL-08.
 *
 * When [onExpandedChange] is non-null, the component is a **controlled pure renderer**:
 * it reads [expandedKeys] on every recompose and calls [onExpandedChange] on every toggle,
 * but does not update any internal set — the caller must respond by updating [expandedKeys].
 * REQ: PNL-08.
 *
 * Do not collapse to one branch — both paths are intentional (matches AeroAccordion
 * hybrid-ownership convention) (PNL-08).
 *
 * ## Sizing model
 *
 * Section heights are stored as abstract proportion weights in `sizePx`
 * ([androidx.compose.runtime.SnapshotStateList]). Absolute pixel heights are derived every
 * recompose from the current container height, so window resize re-anchors proportions
 * without resetting state — the fraction coordinate has no totalPx remember-key (PITFALL-A).
 * REQ: PNL-04.
 *
 * Re-expanding a collapsed section restores its last proportional size from a saved fraction
 * (PNL-03, PNL-PITFALL-06).
 *
 * ## Divider and drag resize
 *
 * A drag grip is placed **only between two adjacent expanded sections** via a zipWithNext-style
 * render condition (PNL-06, PNL-PITFALL-09). Collapsed boundaries never receive a grip.
 *
 * Drag writes `sizePx` directly — no animation wrapper — so the boundary tracks the cursor 1:1.
 * While a drag is active the per-section animation spec switches to `snap()` to prevent the
 * `animateFloatAsState` tween from lagging behind. On release the spec reverts to
 * `tween(200ms, FastOutSlowInEasing)` for collapse/expand transitions (PNL-05, PITFALL-A).
 *
 * N-section drag clamp (pairwise model): the minimum allowed size for the section above the
 * divider is its own [AeroPanelGroupScope.section] minSize; the minimum for the section below
 * is the directly-adjacent below section's own minSize. Clamp math is delegated to
 * [clampPanelDividerPx] which carries the PITFALL-B `coerceAtLeast` guard (PNL-10,
 * PNL-PITFALL-04).
 *
 * Drag is disabled when either neighbor has `resizable = false` (PNL-12).
 *
 * ## Orientation
 *
 * Both orientations share a single internal core (`AeroPanelGroupImpl`) and the same
 * orientation-agnostic `PanelDistribution.kt` logic (state, animation, drag clamp, fraction
 * sizing). Only three things differ per orientation: the `BoxWithConstraints` measurement axis,
 * the container composable (Row vs Column), and the section/divider size modifiers. The vertical
 * behavior from Phase 13 is therefore fully preserved when `orientation = Orientation.Horizontal`
 * is used. REQ: PNL-HORIZ-01.
 *
 * ## onLayoutChange firing contract
 *
 * [onLayoutChange] fires exactly at drag-end and at each collapse/expand toggle. It does NOT
 * fire on every drag frame (PNL-09).
 *
 * @param modifier applied to the outermost [BoxWithConstraints].
 * @param orientation layout axis. [Orientation.Vertical] (default) stacks sections top-to-bottom
 *   with horizontal dividers and drag resizing height. [Orientation.Horizontal] lays sections out
 *   as side-by-side columns with vertical dividers and drag resizing width; each header becomes a
 *   ~36dp-wide full-height strip with a bottom-to-top rotated title and a chevron pointing
 *   ► when expanded, ◄ when collapsed. The expansion, sizing, clamp, and onLayoutChange
 *   semantics are identical to the vertical model — a 90° rotation of the same VS Code Side Bar
 *   behavior. Existing callers that omit this parameter are unaffected. REQ: PNL-HORIZ-01.
 * @param initiallyExpanded if non-null, overrides [AeroPanelGroupScope.section] defaultExpanded
 *   flags on first composition; only keys present in this set start expanded. If null, each
 *   section uses its own defaultExpanded value. Used in uncontrolled mode only.
 * @param expandedKeys controlled expansion set. Must be non-null when [onExpandedChange] is
 *   non-null. Each section whose [AeroPanelGroupScope.section] key is present in this set is
 *   rendered expanded; the component does not hold any internal expansion state in controlled mode.
 * @param onExpandedChange when non-null the component becomes controlled: this callback is
 *   invoked after every toggle with the new desired set of expanded keys. The caller must
 *   respond by updating [expandedKeys]. When null the component manages expansion internally.
 * @param onLayoutChange optional callback fired at drag-end and at each collapse/expand toggle
 *   with the current list of render heights in pixels (one entry per section in declaration
 *   order). REQ: PNL-09.
 * @param content DSL scope where sections are declared via [AeroPanelGroupScope.section].
 *   This lambda is intentionally **not** `@Composable` (it mirrors `LazyListScope`): it is a pure
 *   collection pass that registers sections into a fresh scope on every recompose. Each section's
 *   own `content` slot IS `@Composable` and is composed later in the render. Making this DSL lambda
 *   `@Composable` previously gave it an independent recompose scope that re-ran (e.g. during a
 *   divider drag while a parent recomposed) and re-appended sections into the same persisted scope,
 *   duplicating every section's header strip (RCMP — N sections rendered as N×). Keeping it
 *   non-composable means the section list is always rebuilt exactly once per AeroPanelGroup
 *   recompose and cannot accumulate.
 */
@Composable
public fun AeroPanelGroup(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,   // NEW — default = zero breaking change
    initiallyExpanded: Set<String>? = null,
    expandedKeys: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    onLayoutChange: ((List<Float>) -> Unit)? = null,
    content: AeroPanelGroupScope.() -> Unit,
) {
    // Collect sections once per recompose. The DSL lambda is non-@Composable so it has no
    // independent recompose scope and cannot re-run on its own and re-append (RCMP root cause).
    val scope = AeroPanelGroupScope()
    scope.content()
    AeroPanelGroupImpl(
        orientation = orientation,
        sections = scope.sections,
        modifier = modifier,
        initiallyExpanded = initiallyExpanded,
        expandedKeys = expandedKeys,
        onExpandedChange = onExpandedChange,
        onLayoutChange = onLayoutChange,
    )
}

/**
 * Internal core implementation for [AeroPanelGroup]. Contains all layout/state/drag logic.
 * The public wrapper collects DSL sections and forwards them here along with [orientation].
 *
 * Orientation branch: [Orientation.Vertical] renders a [Column] of horizontal header strips +
 * content boxes (Phase 13 shipped model). [Orientation.Horizontal] renders a [Row] of full-height
 * section columns, each with a ~36dp-wide vertical header strip (rotated title, 0°/180° chevron)
 * and a drag-resizable content area (PNL-HORIZ-01). All shared state/drag/animation logic is
 * orientation-agnostic — only the container, axis, and size modifiers differ per orientation.
 */
@Composable
internal fun AeroPanelGroupImpl(
    orientation: Orientation,
    sections: List<AeroPanelSectionConfig>,
    modifier: Modifier,
    initiallyExpanded: Set<String>?,
    expandedKeys: Set<String>?,
    onExpandedChange: ((Set<String>) -> Unit)?,
    onLayoutChange: ((List<Float>) -> Unit)?,
) {
    val density = LocalDensity.current
    val headerPx = with(density) { HEADER_HEIGHT.toPx() }
    val dividerPx = with(density) { DIVIDER_THICKNESS.toPx() }

    // Hybrid-ownership derivation (PNL-08).
    // Do not collapse to one branch — both paths are intentional
    // (matches AeroAccordion hybrid-ownership convention) (PNL-08).
    val controlled = onExpandedChange != null

    // --- Uncontrolled internal expansion state ---
    // Only written in the uncontrolled branch; ignored when controlled == true.
    var internalExpanded by remember { mutableStateOf(emptySet<String>()) }

    // --- Fraction-based size state — NO totalPx remember-key (PITFALL-A) ---
    // sizePx stores abstract proportion weights, not absolute pixels.
    // Absolute pixel heights are derived every recompose from totalPx.
    val sizePx = remember { mutableStateListOf<Float>() }
    val expandedState = remember { mutableStateListOf<Boolean>() }
    val lastExpandedFractionState = remember { mutableStateListOf<Float>() }

    // isDragging flag: true while a pointer is held on a divider. Switches animationSpec
    // to snap() so the per-section animateFloatAsState does not lag the cursor (spike finding 4,
    // DRAG ANIMATION DISABLE). Cleared in onDragEnd so all exit paths reset it.
    var isDragging by remember { mutableStateOf(false) }

    // Seed state lists on first composition or when DSL structure changes.
    if (sizePx.size != sections.size) {
        sizePx.clear()
        expandedState.clear()
        lastExpandedFractionState.clear()
        sections.forEach { sec ->
            sizePx.add(if (sec.defaultSize != null) with(density) { sec.defaultSize.toPx() } else 1f)
            val exp = when {
                initiallyExpanded != null -> sec.key in initiallyExpanded
                else -> sec.defaultExpanded
            }
            expandedState.add(exp)
            lastExpandedFractionState.add(0f)
        }
        // Also seed the internal uncontrolled set.
        internalExpanded = sections
            .filter { sec ->
                when {
                    initiallyExpanded != null -> sec.key in initiallyExpanded
                    else -> sec.defaultExpanded
                }
            }
            .map { it.key }
            .toSet()
    }

    // --- Expansion helpers ---

    // isExpanded: derived from controlled params or internal state (PNL-08).
    // Do not collapse to one branch — both paths are intentional
    // (matches AeroAccordion hybrid-ownership convention) (PNL-08).
    fun isExpanded(sec: AeroPanelSectionConfig): Boolean =
        if (controlled) expandedKeys?.contains(sec.key) == true
        else sec.key in internalExpanded

    // Sync expandedState mirror from the effective expansion source AFTER a successful commit.
    // Deferred to SideEffect so this composition pass never writes a state it also reads
    // (the write-during-composition loop that caused horizontal-controlled ×N duplication).
    // MUST NOT call onLayoutChange — that stays on drag-end + toggle only (REG-01).
    SideEffect {
        sections.forEachIndexed { i, sec ->
            val exp = isExpanded(sec)
            if (expandedState.getOrElse(i) { exp } != exp) expandedState[i] = exp
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Axis switch: horizontal reads width, vertical reads height (AeroSplitPane pattern, lines 99-102).
        val isHorizontal = orientation == Orientation.Horizontal
        val totalPx = if (isHorizontal) constraints.maxWidth.toFloat() else constraints.maxHeight.toFloat()

        // rememberUpdatedState(totalPx) so any drag lambda that was captured once always reads
        // the live container dimension — prevents snap-back after a window resize mid-drag (FIXSP-01).
        val liveTotalPx by rememberUpdatedState(totalPx)

        // Size-math reads the authoritative expansion source directly each composition
        // (NOT the SideEffect-synced mirror, which lags one frame) — RCMP-02.
        val expandedArr = BooleanArray(sections.size) { i -> isExpanded(sections[i]) }
        val availableForExpanded = computeAvailablePx(totalPx, expandedArr, headerPx, dividerPx)
        val renderHeights = distributePx(sizePx.toFloatArray(), expandedArr, totalPx, headerPx, dividerPx)

        // --- Collapse/expand toggle ---
        // Dispatches to controlled or uncontrolled branch (PNL-08).
        // Do not collapse to one branch — both paths are intentional
        // (matches AeroAccordion hybrid-ownership convention) (PNL-08).
        fun onToggle(i: Int) {
            if (!sections[i].collapsible) return
            if (isDragging) return  // PNL-PITFALL-03: guard mid-drag collapse race

            val currentExpanded = expandedState.toBooleanArray()
            val currentSizePx = sizePx.toFloatArray()
            val currentAvailable = computeAvailablePx(liveTotalPx, currentExpanded, headerPx, dividerPx)
            val secKey = sections[i].key

            if (controlled) {
                // Controlled branch: pure renderer — compute next set and notify caller.
                val currentKeys = expandedKeys ?: emptySet()
                val nextKeys = if (secKey in currentKeys) currentKeys - secKey else currentKeys + secKey
                onExpandedChange!!(nextKeys)
            } else {
                // Uncontrolled branch: update internal state and size weights.
                if (currentExpanded[i]) {
                    lastExpandedFractionState[i] = lastExpandedFraction(currentSizePx[i], currentAvailable)
                    val newSizes = shareTransferOnCollapse(currentSizePx, currentExpanded, i)
                    newSizes.forEachIndexed { idx, v -> sizePx[idx] = v }
                    expandedState[i] = false
                    internalExpanded = internalExpanded - secKey
                } else {
                    expandedState[i] = true
                    internalExpanded = internalExpanded + secKey
                    val newExpanded = expandedState.toBooleanArray()
                    val newAvailable = computeAvailablePx(liveTotalPx, newExpanded, headerPx, dividerPx)
                    val restorePx = restoreFromFraction(lastExpandedFractionState[i], newAvailable)
                    val newSizes = shareTransferOnExpand(currentSizePx, newExpanded, i, restorePx)
                    newSizes.forEachIndexed { idx, v -> sizePx[idx] = v }
                }
            }

            // onLayoutChange fires at toggle (PNL-09) — NOT per drag frame.
            onLayoutChange?.invoke(sizePx.toList())
        }

        // --- Drag resize lambda (live-state, FIXSP-01 / PITFALL-A) ---
        // onDragBetween reads sizePx[above]/sizePx[below] via SnapshotStateList — always live.
        // Delta scaling: raw pointer delta is in rendered pixels; sizePx is in abstract proportion
        // units. Scale = expandedSizeSum / availableForExpanded (spike finding 2, STATE.md).
        // clampPanelDividerPx handles N-section Σ-minima clamp with PITFALL-B guard (PNL-10).
        val onDragBetween: (Int, Int, Float) -> Unit = { above, below, delta ->
            val currentExpandedArr = expandedState.toBooleanArray()
            val currentAvailable = computeAvailablePx(liveTotalPx, currentExpandedArr, headerPx, dividerPx)
                .coerceAtLeast(1f)

            // expandedSizeSum = combined sizePx weight of the two neighbors (invariant during drag)
            val expandedIndicesAll = sizePx.indices.filter { currentExpandedArr.getOrElse(it) { false } }
            val expandedSizeSum = expandedIndicesAll.sumOf { sizePx[it].toDouble() }.toFloat()

            // Scale: convert rendered-pixel delta into sizePx proportion-unit delta.
            val scale = if (currentAvailable > 0f) expandedSizeSum / currentAvailable else 1f
            val scaledDelta = delta * scale

            // minAbovePx: above section's own minSize converted to sizePx units.
            val minAbovePx = with(density) { sections[above].minSize.toPx() } * scale

            // minBelowPx: directly-adjacent below section's own minSize (pairwise model — PNL-10, PNL-PITFALL-04).
            // Summing all expanded sections at or below would over-reserve the clamp budget, pinning the divider.
            val minBelowPx = with(density) { sections[below].minSize.toPx() } * scale

            val totalBudgetPx = sizePx[above] + sizePx[below]
            val newAbove = clampPanelDividerPx(sizePx[above], scaledDelta, minAbovePx, minBelowPx, totalBudgetPx)
            sizePx[above] = newAbove
            sizePx[below] = totalBudgetPx - newAbove
        }

        // --- Per-section animated heights (Pattern 3, PNL-PITFALL-01) ---
        // animateFloatAsState READS renderHeight as its target — it NEVER writes sizePx.
        // animationSpec = snap() while isDragging so the border tracks the cursor 1:1 (spike finding 4).
        // animationSpec = tween(200ms) for collapse/expand when isDragging is false.
        val animatedHeights = sections.indices.map { i ->
            val targetPx = if (expandedState.getOrElse(i) { false }) headerPx + renderHeights[i] else headerPx
            val animated by animateFloatAsState(
                targetValue = targetPx,
                animationSpec = if (isDragging) snap() else tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "panelHeight_${sections[i].key}",
            )
            animated
        }

        // --- Per-section caret rotations ---
        // Vertical: CaretRight rotates 0->90° on expand (Win7 Aero, points down when expanded).
        // Horizontal: CaretRight at 0° points ► (expanded), 180° points ◄ (collapsed). (Pitfall 4)
        val caretRotations = sections.indices.map { i ->
            val rotation by animateFloatAsState(
                targetValue = if (isHorizontal) {
                    if (expandedState.getOrElse(i) { false }) 0f else 180f   // expanded ►, collapsed ◄
                } else {
                    if (expandedState.getOrElse(i) { false }) 90f else 0f    // existing vertical
                },
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                label = "caret_${sections[i].key}",
            )
            rotation
        }

        // --- Find the last expanded section index for weight(1f) assignment ---
        val lastExpandedIdx = expandedState.indexOfLast { it }

        if (isHorizontal) {
            // ── HORIZONTAL BRANCH: N side-by-side columns ──────────────────────────────
            // Each section is a Row(header strip | content area) sized along the X axis.
            // animatedHeights[i] is REINTERPRETED as animated WIDTH-px in this branch —
            // the same state, shared logic, and distributePx math are fully orientation-agnostic.
            // Do not collapse to one branch — both paths are intentional (PNL-08).
            Row(modifier = Modifier.fillMaxSize()) {
                sections.forEachIndexed { i, section ->
                    key(section.key) {
                        val isExpandedNow = expandedState.getOrElse(i) { false }
                        val animatedMainPx = animatedHeights[i]   // main-axis px (width in horizontal)
                        val caretRotation = caretRotations[i]
                        val headerWidthDp = with(density) { headerPx.toDp() }

                        // ── Section outer Row: full-height, width = animated main-axis ──────
                        // All sections use explicit width(animatedMainPx) — symmetric with the
                        // vertical branch where each section header has fixed height and the Column
                        // stacks them without weight on the outer section element. Float-rounding
                        // for total column widths is already absorbed by distributePx (last-expanded
                        // remainder rule, PNL-PITFALL-02). weight(1f) belongs only on the CONTENT
                        // box within the last column (fills remaining space inside that outer Row).
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(with(density) { animatedMainPx.toDp() }),
                        ) {
                            // ── Header strip: full-height, HEADER_HEIGHT-wide (36dp) ──────────
                            // glassPanel for Win7 Aero gloss surface; clip before clickable so
                            // hover/press highlight clips to rounded glass (F-ACCORDION-HOVER).
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(HEADER_HEIGHT)       // reuse 36dp constant as WIDTH
                                    .glassPanel(cornerRadius = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (section.collapsible) Modifier.clickable { onToggle(i) }
                                        else Modifier
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Chevron at strip top: only when collapsible (PNL-11).
                                // 0° = ► (expanded), 180° = ◄ (collapsed) — horizontal model.
                                if (section.collapsible) {
                                    Spacer(Modifier.height(8.dp))
                                    Icon(
                                        imageVector = AeroIcons.CaretRight,
                                        contentDescription = null,
                                        tint = AeroTheme.colors.onSurface,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer { rotationZ = caretRotation },
                                    )
                                }

                                // Rotated title — fills remaining strip height, reads bottom-to-top.
                                // requiredWidth(maxHeight) gives the Text the column's available HEIGHT
                                // as its measured WIDTH so the full title fits on one line; rotate(-90)
                                // then turns that long horizontal line into a centered vertical strip.
                                // (Pitfall 1 — the Text must be sized along the rotation axis, not the
                                // 36dp strip width, or it truncates.)
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clipToBounds(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val titleSpan = maxHeight
                                    Text(
                                        text = section.title,
                                        style = AeroTheme.typography.bodyMedium,
                                        color = AeroTheme.colors.onSurface,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .requiredWidth(titleSpan)
                                            .rotate(-90f),
                                    )
                                }

                                // leadingIcon below title (v1.1 rule: tint always explicit).
                                section.leadingIcon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = AeroTheme.colors.onSurface,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }

                                // headerActions at bottom of strip.
                                // headerActions is @Composable RowScope.() -> Unit — hosted in Row.
                                // Non-bubbling: actions consumed by their own onClick, not toggle (PNL-14).
                                section.headerActions?.let { actions ->
                                    Spacer(Modifier.height(6.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row { actions() }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            // ── Content area — only when expanded (or mid-animation) ──────────
                            // Content width = total animated column width minus the 36dp header strip.
                            // Last expanded section uses weight(1f); others use explicit .width(dp).
                            if (isExpandedNow || animatedMainPx > headerPx) {
                                if (i == lastExpandedIdx && isExpandedNow) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .clipToBounds(),
                                    ) { section.content() }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(with(density) { animatedMainPx.toDp() } - headerWidthDp)
                                            .clipToBounds(),
                                    ) { section.content() }
                                }
                            }
                        }

                        // Vertical drag divider — only between two adjacent EXPANDED sections (PNL-06, PNL-PITFALL-09).
                        // Passes Orientation.Horizontal so aeroDragSplitter uses E_RESIZE cursor
                        // and X-axis positionChange().x delta. Drag disabled when either neighbor
                        // has resizable=false (PNL-12).
                        val nextExpanded = expandedState.getOrElse(i + 1) { false }
                        if (isExpandedNow && i < sections.lastIndex && nextExpanded) {
                            val dragEnabled = section.resizable && sections[i + 1].resizable
                            PanelGroupDivider(
                                orientation = Orientation.Horizontal,  // vertical grip, E/W cursor, X-axis delta
                                onDrag = { delta ->
                                    isDragging = true
                                    onDragBetween(i, i + 1, delta)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    // onLayoutChange fires at drag-end (PNL-09) — NOT per drag frame.
                                    onLayoutChange?.invoke(sizePx.toList())
                                },
                                enabled = dragEnabled,
                            )
                        }
                    }
                }
            }
        } else {
            // ── VERTICAL BRANCH (existing — untouched from Plan 01) ─────────────────────
            // Do not collapse to one branch — both paths are intentional (PNL-08).
            Column(modifier = Modifier.fillMaxSize()) {
                sections.forEachIndexed { i, section ->
                    key(section.key) {
                        val isExpandedNow = expandedState.getOrElse(i) { false }
                        val animatedHeightPx = animatedHeights[i]
                        val animatedHeightDp = with(density) { animatedHeightPx.toDp() }
                        val caretRotation = caretRotations[i]

                        // Header strip — fixed 36dp, always rendered.
                        // glassPanel(8.dp) for Win7 Aero gloss/gradient surface; clip before clickable
                        // so hover/press highlight clips to the rounded glass surface (F-ACCORDION-HOVER).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEADER_HEIGHT)
                                .glassPanel(cornerRadius = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (section.collapsible) Modifier.clickable { onToggle(i) }
                                    else Modifier
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // CaretRight: only when collapsible (PNL-11 — collapsible=false hides chevron).
                            // Rotates 0->90 degrees on expand (Win7 Aero caret — AeroAccordion pattern).
                            if (section.collapsible) {
                                Icon(
                                    imageVector = AeroIcons.CaretRight,
                                    contentDescription = null,
                                    tint = AeroTheme.colors.onSurface,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer { rotationZ = caretRotation },
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // leadingIcon: rendered with explicit tint (v1.1 rule).
                            section.leadingIcon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AeroTheme.colors.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Title fills remaining space so headerActions sit at the far right.
                            Text(
                                text = section.title,
                                style = AeroTheme.typography.bodyMedium,
                                color = AeroTheme.colors.onSurface,
                                modifier = Modifier.weight(1f),
                            )

                            // headerActions: rendered in BOTH collapsed and expanded states (always in header strip).
                            // Non-bubbling: the actions Row does not have the toggle clickable on it —
                            // action clicks are consumed by each action's own onClick handler and do NOT
                            // propagate to the outer Row toggle (PNL-14 / VS Code model).
                            section.headerActions?.let { actions ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    actions()
                                }
                            }
                        }

                        // Expanded content — height driven by animatedHeightPx (not raw renderHeights).
                        // Last expanded section uses weight(1f) to absorb float rounding (PNL-PITFALL-11);
                        // all others use explicit .height(dp).
                        if (isExpandedNow || animatedHeightPx > headerPx) {
                            if (i == lastExpandedIdx && isExpandedNow) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clipToBounds(),
                                ) { section.content() }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(animatedHeightDp - HEADER_HEIGHT)
                                        .clipToBounds(),
                                ) { section.content() }
                            }
                        }

                        // Drag divider — only between two adjacent EXPANDED sections (PNL-06, PNL-PITFALL-09).
                        // Drag is disabled when either neighbor has resizable = false (PNL-12).
                        val nextExpanded = expandedState.getOrElse(i + 1) { false }
                        if (isExpandedNow && i < sections.lastIndex && nextExpanded) {
                            val dragEnabled = section.resizable && sections[i + 1].resizable
                            PanelGroupDivider(
                                orientation = Orientation.Vertical,
                                onDrag = { delta ->
                                    isDragging = true
                                    onDragBetween(i, i + 1, delta)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    // onLayoutChange fires at drag-end (PNL-09) — NOT per drag frame.
                                    onLayoutChange?.invoke(sizePx.toList())
                                },
                                enabled = dragEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Internal orientation-aware drag divider rendered between two adjacent expanded sections.
 *
 * For [Orientation.Vertical] (default vertical group): renders an 8dp-tall hit-area Box with a
 * centered 1dp horizontal visual line and a [Row] of 3 grip dots — identical to the shipped
 * Phase 13 divider.
 *
 * For [Orientation.Horizontal] (horizontal group — Plan 02): renders an 8dp-wide hit-area Box
 * with a centered 1dp vertical visual line and a [Column] of 3 grip dots.
 *
 * The [aeroDragSplitter] Modifier (locked v2.0 pattern, PITFALL-03) handles cursor change,
 * `pointerHoverIcon`, and the `awaitPointerEventScope` manual loop without touchSlop delay.
 *
 * When [enabled] is false (either neighbor has `resizable = false`, PNL-12) the hit-area
 * renders the static 1dp line with no grip dots and no drag effect and no cursor change.
 *
 * Grip dots mirror the AeroSplitPane SplitPaneDivider orientation-aware pattern (PNL-05).
 */
@Composable
private fun PanelGroupDivider(
    orientation: Orientation,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    enabled: Boolean,
) {
    val isHorizontal = orientation == Orientation.Horizontal
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val hitAreaModifier = if (isHorizontal)
        Modifier.fillMaxHeight().width(DIVIDER_THICKNESS)
    else
        Modifier.fillMaxWidth().height(DIVIDER_THICKNESS)   // EXISTING vertical — unchanged

    val lineModifier = if (isHorizontal)
        Modifier.fillMaxHeight().width(1.dp)
    else
        Modifier.fillMaxWidth().height(1.dp)   // EXISTING vertical — unchanged

    Box(
        modifier = hitAreaModifier
            .hoverable(interactionSource)
            .then(
                if (enabled) Modifier.aeroDragSplitter(
                    orientation = orientation,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    enabled = true,
                ) else Modifier
            )
            .background(if (hovered && enabled) colors.buttonHover else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        // 1dp Aero visual line — centered within the hit-area.
        Box(
            modifier = lineModifier.background(colors.borderDefault),
        )

        // Grip dots: orientation-aware, only rendered when enabled (resizable=true on both neighbors,
        // PNL-05 / PNL-12). Vertical path: Row of 3 dots (horizontal grip visual — unchanged).
        // Horizontal path: Column of 3 dots (vertical grip visual — pre-wired for Plan 02).
        if (enabled) {
            if (isHorizontal) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    repeat(3) { idx ->
                        if (idx > 0) Box(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(colors.labelText),
                        )
                    }
                }
            } else {
                // EXISTING vertical grip — Row of 3 dots, unchanged
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { idx ->
                        if (idx > 0) Box(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(colors.labelText),
                        )
                    }
                }
            }
        }
    }
}
