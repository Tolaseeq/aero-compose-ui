package com.mordred.showcase.sections

// THROWAWAY: Animation-vs-drag coexistence spike for PNL-PITFALL-01.
// Delete this file in plan 13-05 (showcase sign-off) once the real AeroPanelGroup demo replaces it.
//
// Note: aeroDragSplitter and clampDividerPx are `internal` to :library and not accessible from
// :showcase. This self-contained spike inlines equivalent implementations so no library code is added.

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import java.awt.Cursor

/**
 * THROWAWAY spike composable proving PNL-PITFALL-01 (animation-vs-drag coexistence).
 *
 * Architecture: `animateFloatAsState` READS `sizePx[i]` via `renderHeight(i)` as its target
 * (never writes it). The inline drag splitter WRITES `sizePx[above]`/`sizePx[below]` directly.
 * These operate on different sides of the state — no conflict, no oscillation.
 *
 * `aeroDragSplitter` and `clampDividerPx` are `internal` to :library; this spike inlines
 * equivalent logic to keep the file self-contained per plan spec.
 *
 * Must be deleted in plan 13-05 once `AeroPanelGroup` demo replaces it.
 */
@Composable
fun PanelGroupSpikeSection() {
    val colors = AeroTheme.colors

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(640.dp)) {
        val totalPx = constraints.maxHeight.toFloat()

        // State: no key on remember — stable across window resize (PITFALL-A guard).
        val sizePx = remember { mutableStateListOf(300f, 300f, 300f) }
        val expanded = remember { mutableStateListOf(true, true, true) }

        // Derived each recompose (NOT stored):
        val density = LocalDensity.current
        val headerHeightPx = with(density) { 36.dp.toPx() }
        val dividerThicknessPx = with(density) { 8.dp.toPx() }
        val activeDividerCount = expanded.zipWithNext().count { (a, b) -> a && b }
        // BUG-1 FIX (PNL-PITFALL-01 spike finding): every section renders a header regardless of
        // expanded state, so reserve one headerHeightPx per section, not just per collapsed section.
        // Previously: collapsedCount * headerHeightPx — wrong, left expanded headers unaccounted for.
        val totalHeaderPx = sizePx.size * headerHeightPx
        val availableForExpanded = (totalPx - totalHeaderPx - activeDividerCount * dividerThicknessPx)
            .coerceAtLeast(0f)
        val expandedSizeSum = sizePx.indices.filter { expanded[it] }.sumOf { sizePx[it].toDouble() }.toFloat()

        fun renderHeight(i: Int): Float =
            if (!expanded[i]) headerHeightPx
            else if (expandedSizeSum > 0f) (sizePx[i] / expandedSizeSum) * availableForExpanded
            else availableForExpanded

        // animateFloatAsState READS renderHeight(i) as target — NEVER writes sizePx (PITFALL-01 key rule).
        val animatedHeight = sizePx.indices.map { i ->
            animateFloatAsState(
                targetValue = renderHeight(i),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "spikeHeight_$i"
            ).value
        }

        // Live-state drag lambda: liveTotalPx wraps totalPx for stale-capture safety (FIXSP-01).
        // sizePx is SnapshotStateList — reads inside lambda are always live; no rememberUpdatedState needed.
        @Suppress("UNUSED_VARIABLE")
        val liveTotalPx by rememberUpdatedState(totalPx)
        val onDragBetween: (Int, Int, Float) -> Unit = { above, below, delta ->
            val combined = sizePx[above] + sizePx[below]
            // BUG-2 FIX (PNL-PITFALL-01 spike finding): `delta` arrives in rendered pixels but sizePx
            // is in abstract units. The rendered height of a section is:
            //   renderHeight(i) = (sizePx[i] / expandedSizeSum) * availableForExpanded
            // So 1 rendered pixel == (expandedSizeSum / availableForExpanded) sizePx units.
            // Scaling delta before applying ensures the divider tracks the cursor 1:1.
            // `combined` (above + below) is constant during the gesture, so expandedSizeSum is
            // unchanged by this drag — the scale factor is stable for the whole gesture.
            val scale = if (availableForExpanded > 0f) expandedSizeSum / availableForExpanded else 1f
            val scaledDelta = delta * scale
            // Min clamp must also be in sizePx units (rendered minPx converted by the same scale).
            val minRenderedPx = with(density) { 60.dp.toPx() }
            val minSizeUnits = minRenderedPx * scale
            // Inline clampDividerPx: safeMax guard prevents coerceIn throw when combined < 2*minSizeUnits (PITFALL-B)
            val safeMax = (combined - minSizeUnits).coerceAtLeast(minSizeUnits)
            val newAbove = (sizePx[above] + scaledDelta).coerceIn(minSizeUnits, safeMax)
            sizePx[above] = newAbove
            sizePx[below] = combined - newAbove
        }

        Column(modifier = Modifier.fillMaxSize()) {
            val lastIndex = sizePx.lastIndex
            sizePx.indices.forEach { i ->
                // Header strip: click toggles expanded[i] (collapse/expand with 200ms animation)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(colors.panelBackground)
                        .clickable { expanded[i] = !expanded[i] }
                ) {
                    Text(
                        text = "Section $i  [${if (expanded[i]) "expanded" else "collapsed"}]  — click to toggle",
                        color = colors.onSurface
                    )
                }

                // Content area (only when expanded), height driven by animatedHeight
                if (expanded[i]) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { animatedHeight[i].toDp() })
                            .background(colors.surface)
                    ) {
                        Text("Content of section $i", color = colors.onSurface)
                    }
                }

                // Divider: only between two adjacent EXPANDED sections (PITFALL-09 guard)
                // Uses inline awaitPointerEventScope drag (mirrors aeroDragSplitter — PITFALL-03 fix)
                if (expanded[i] && i < lastIndex && expanded[i + 1]) {
                    val iCapture = i
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(colors.borderDefault)
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
                            .pointerInput(Orientation.Vertical, true) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitFirstDown(requireUnconsumed = false)
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (!change.pressed) break
                                            val delta = change.positionChange().y
                                            if (delta != 0f) {
                                                onDragBetween(iCapture, iCapture + 1, delta)
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}
