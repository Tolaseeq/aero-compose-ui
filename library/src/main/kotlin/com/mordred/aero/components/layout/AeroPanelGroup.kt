package com.mordred.aero.components.layout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.layout.internal.panelgroup.computeAvailablePx
import com.mordred.aero.components.layout.internal.panelgroup.distributePx
import com.mordred.aero.components.layout.internal.panelgroup.lastExpandedFraction
import com.mordred.aero.components.layout.internal.panelgroup.restoreFromFraction
import com.mordred.aero.components.layout.internal.panelgroup.shareTransferOnCollapse
import com.mordred.aero.components.layout.internal.panelgroup.shareTransferOnExpand
import com.mordred.aero.theme.AeroTheme

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
     * @param collapsible when false the section header does not respond to clicks.
     * @param resizable when true a drag divider can resize adjacent expanded sections (13-04).
     * @param defaultExpanded initial expanded state when no [AeroPanelGroup.initiallyExpanded]
     *   override is present.
     * @param defaultSize if non-null, the section's initial sizePx seed is derived from this Dp
     *   value rather than an equal share. Useful to pre-weight sections differently.
     * @param leadingIcon optional icon displayed left of [title] in the header.
     * @param headerActions optional trailing content placed at the end of the header row (13-05).
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
 * A vertical panel group with N sections that can be collapsed/expanded and optionally resized.
 *
 * ## Layout model
 * - Each section always renders a [HEADER_HEIGHT] strip (36dp).
 * - Expanded sections divide the remaining height proportionally via fraction-based [sizePx] state.
 * - Window resize recalculates pixel heights from the current container height every recompose
 *   without resetting proportions — fraction state has no totalPx remember-key (PITFALL-A).
 * - Collapse/expand toggles redistribute shares to/from neighbors via
 *   [shareTransferOnCollapse]/[shareTransferOnExpand]. Re-expanding restores the section's prior
 *   proportional size from [lastExpandedFraction] (PNL-PITFALL-06).
 * - A 1dp divider is rendered only between adjacent pairs of **expanded** sections; collapsed
 *   boundaries never get a divider (PNL-PITFALL-09).
 *
 * ## Uncontrolled expansion
 * This composable manages expansion state internally. A controlled path (onExpandedChange /
 * expandedKeys params) lands in plan 13-04.
 *
 * ## Deferred features
 * - Drag-resize between sections (plan 13-04).
 * - Visual polish: glassPanel header, CaretRight animation, grip dots (plan 13-05).
 *
 * REQ: PNL-01, PNL-02, PNL-03, PNL-04, PNL-13, PNL-15.
 *
 * @param modifier applied to the outermost [BoxWithConstraints].
 * @param initiallyExpanded if non-null, overrides [AeroPanelGroupScope.section] defaultExpanded
 *   flags; only keys present in this set start expanded. If null, each section uses its own
 *   defaultExpanded value.
 * @param onLayoutChange optional callback receiving the list of current render heights (in px)
 *   after each recompose — useful for external layout mirroring.
 * @param content DSL scope where sections are declared via [AeroPanelGroupScope.section].
 *
 * TODO(13-04): add onExpandedChange: ((Set<String>) -> Unit)? = null and
 *   expandedKeys: Set<String>? = null for the controlled expansion path.
 */
@Composable
public fun AeroPanelGroup(
    modifier: Modifier = Modifier,
    initiallyExpanded: Set<String>? = null,
    onLayoutChange: ((List<Float>) -> Unit)? = null,
    content: @Composable AeroPanelGroupScope.() -> Unit,
) {
    // Collect sections fresh each recompose (AeroSidebar pattern).
    val scope = AeroPanelGroupScope()
    scope.content()
    val sections = scope.sections

    val density = LocalDensity.current
    val headerPx = with(density) { HEADER_HEIGHT.toPx() }
    val dividerPx = with(density) { DIVIDER_THICKNESS.toPx() }

    // --- Fraction-based state — NO totalPx remember-key (PITFALL-A) ---
    // sizePx stores abstract proportion weights, not absolute pixels.
    // Absolute pixel heights are derived every recompose from totalPx.
    val sizePx = remember { mutableStateListOf<Float>() }
    val expandedState = remember { mutableStateListOf<Boolean>() }
    val lastExpandedFractionState = remember { mutableStateListOf<Float>() }

    // Seed state lists when sections count changes (first composition or DSL change).
    if (sizePx.size != sections.size) {
        sizePx.clear()
        expandedState.clear()
        lastExpandedFractionState.clear()
        sections.forEach { sec ->
            // defaultSize in Dp → seed sizePx with its px value; else use 1f (equal share).
            sizePx.add(if (sec.defaultSize != null) with(density) { sec.defaultSize.toPx() } else 1f)
            val exp = if (initiallyExpanded != null) sec.key in initiallyExpanded else sec.defaultExpanded
            expandedState.add(exp)
            lastExpandedFractionState.add(0f)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalPx = constraints.maxHeight.toFloat()

        val expandedArr = expandedState.toBooleanArray()
        val availableForExpanded = computeAvailablePx(totalPx, expandedArr, headerPx, dividerPx)
        val renderHeights = distributePx(sizePx.toFloatArray(), expandedArr, totalPx, headerPx, dividerPx)

        // Notify caller of current layout (optional).
        onLayoutChange?.invoke(renderHeights.toList())

        // --- Collapse/expand toggle (uncontrolled) ---
        fun onToggle(i: Int) {
            if (!sections[i].collapsible) return
            val currentExpanded = expandedState.toBooleanArray()
            val currentSizePx = sizePx.toFloatArray()
            val currentAvailable = computeAvailablePx(totalPx, currentExpanded, headerPx, dividerPx)

            if (currentExpanded[i]) {
                // Collapsing: save fraction before collapse, transfer share to neighbors.
                lastExpandedFractionState[i] = lastExpandedFraction(currentSizePx[i], currentAvailable)
                val newSizes = shareTransferOnCollapse(currentSizePx, currentExpanded, i)
                newSizes.forEachIndexed { idx, v -> sizePx[idx] = v }
                expandedState[i] = false
            } else {
                // Expanding: restore prior fraction, take share from donors.
                expandedState[i] = true
                val newExpanded = expandedState.toBooleanArray()
                val newAvailable = computeAvailablePx(totalPx, newExpanded, headerPx, dividerPx)
                val restorePx = restoreFromFraction(lastExpandedFractionState[i], newAvailable)
                val newSizes = shareTransferOnExpand(currentSizePx, newExpanded, i, restorePx)
                newSizes.forEachIndexed { idx, v -> sizePx[idx] = v }
            }
        }

        // --- Per-section animated heights (Plan 13-03 Task 2: 200ms FastOutSlowInEasing) ---
        // animateFloatAsState READS the derived renderHeight as its target — it NEVER writes
        // sizePx (Pattern 3 from 13-01 spike, PNL-PITFALL-01).
        // isDragging / snap() support lands in plan 13-04 alongside the drag gesture.
        @Suppress("UNCHECKED_CAST")
        val animatedHeights = sections.indices.map { i ->
            val targetPx = if (expandedState.getOrElse(i) { false }) renderHeights[i] else headerPx
            val animated by animateFloatAsState(
                targetValue = targetPx,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "panelHeight_${sections[i].key}",
            )
            animated
        }

        // --- Find the last expanded section index for weight(1f) assignment ---
        val lastExpandedIdx = expandedState.indexOfLast { it }

        Column(modifier = Modifier.fillMaxSize()) {
            sections.forEachIndexed { i, section ->
                key(section.key) {
                    val isExpanded = expandedState.getOrElse(i) { false }
                    val animatedHeightPx = animatedHeights[i]
                    val animatedHeightDp = with(density) { animatedHeightPx.toDp() }

                    // Header strip — fixed 36dp, always rendered.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT)
                            .then(
                                if (section.collapsible) Modifier.clickable { onToggle(i) }
                                else Modifier
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        section.leadingIcon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AeroTheme.colors.onSurface,
                                modifier = Modifier.size(16.dp).padding(end = 6.dp),
                            )
                        }
                        Text(
                            text = section.title,
                            style = AeroTheme.typography.bodyMedium,
                            color = AeroTheme.colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        section.headerActions?.let { actions ->
                            Row { actions() }
                        }
                    }

                    // Expanded content — height driven by animatedHeightPx (not raw renderHeights).
                    // Last expanded section uses weight(1f) to absorb float rounding (PNL-PITFALL-11);
                    // all others use explicit .height(dp).
                    if (isExpanded || animatedHeightPx > headerPx) {
                        if (i == lastExpandedIdx && isExpanded) {
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

                    // Static 1dp divider — only between adjacent EXPANDED sections (PNL-PITFALL-09).
                    val nextExpanded = expandedState.getOrElse(i + 1) { false }
                    if (isExpanded && i < sections.lastIndex && nextExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AeroTheme.colors.borderDefault),
                        )
                    }
                }
            }
        }
    }
}
