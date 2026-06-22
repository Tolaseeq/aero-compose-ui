package com.mordred.aero.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.internal.drag.aeroDragSplitter
import com.mordred.aero.components.layout.internal.splitpane.clampDividerPx
import com.mordred.aero.components.layout.internal.splitpane.fractionToPx
import com.mordred.aero.components.layout.internal.splitpane.pxToFraction
import com.mordred.aero.theme.AeroTheme

/**
 * Orientation of the [AeroSplitPane] divider.
 *
 * - [Horizontal]: divider runs vertically (splits left ↔ right). Resize cursor = E_RESIZE.
 * - [Vertical]: divider runs horizontally (splits top ↔ bottom). Resize cursor = N_RESIZE.
 */
public enum class AeroSplitOrientation { Horizontal, Vertical }

/**
 * LAYO-03, LAYO-04: A two-pane layout with a draggable divider.
 *
 * ## Ownership model — uncontrolled + optional callback
 * The divider position is held internally as a px value (PITFALL-14 clamp). This is intentionally
 * **not** a hybrid/controlled component: a controlled drag would fire [onSplitChange] every frame,
 * requiring the caller to hold and re-apply the split position on every mouse move.
 * Use [onSplitChange] to persist the last position or drive external UI.
 *
 * ## N-pane layout
 * The public API is 2-pane. N-pane grids are achieved by the caller nesting `AeroSplitPane`
 * composables inside the [start] or [end] slots (locked v2.0 design, scoped in CONTEXT.md).
 *
 * ## Minimum sizes
 * [minFirstPaneSize] and [minSecondPaneSize] default to 48.dp. Neither pane can be dragged below
 * its minimum — dragging to either edge stops cleanly without layout exceptions (LAYO-03 clamp).
 *
 * ## Divider interaction
 * The 1dp visual line is surrounded by an invisible 8dp hit-area. The [aeroDragSplitter] Modifier
 * (Phase 7) owns: cursor change (E_RESIZE_CURSOR for Horizontal, N_RESIZE_CURSOR for Vertical),
 * `pointerHoverIcon`, and the `awaitPointerEventScope` manual loop that fires on the **first**
 * mouse movement (no touchSlop delay — PITFALL-03 already defused in Phase 7).
 *
 * @param start content for the first pane (left for [AeroSplitOrientation.Horizontal], top for Vertical).
 * @param end content for the second pane (right for Horizontal, bottom for Vertical).
 * @param modifier outer Modifier applied to the root container.
 * @param orientation split direction — Horizontal (default) or Vertical.
 * @param initialSplitFraction initial divider position as a fraction of total size [0f..1f]. Default 0.5f.
 * @param minFirstPaneSize minimum size of the [start] pane. Default 48.dp (LAYO-03 clamp).
 * @param minSecondPaneSize minimum size of the [end] pane. Default 48.dp (LAYO-03 clamp).
 * @param onSplitChange optional callback fired on every drag event with the new fraction [0f..1f].
 */
@Composable
public fun AeroSplitPane(
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    orientation: AeroSplitOrientation = AeroSplitOrientation.Horizontal,
    initialSplitFraction: Float = 0.5f,
    minFirstPaneSize: Dp = 48.dp,
    minSecondPaneSize: Dp = 48.dp,
    onSplitChange: ((Float) -> Unit)? = null,
) {
    val density = LocalDensity.current

    // Map AeroSplitOrientation -> Compose Orientation for aeroDragSplitter
    val composeOrientation = when (orientation) {
        AeroSplitOrientation.Horizontal -> Orientation.Horizontal
        AeroSplitOrientation.Vertical   -> Orientation.Vertical
    }

    // BoxWithConstraints runs once at composition (not on every drag recomposition).
    // Drag only updates dividerPx state, which causes an efficient re-layout from stored px.
    // This pattern avoids SubcomposeLayout overhead on drag frames (PITFALL research §perf).
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalPx = if (orientation == AeroSplitOrientation.Horizontal)
            constraints.maxWidth.toFloat()
        else
            constraints.maxHeight.toFloat()

        // Fraction is the stable, viewport-independent coordinate. Pixel position is derived every
        // recompose from totalPx, so a parent resize (or a parent-drag totalPx change in nested
        // layouts) re-anchors proportionally instead of resetting to initialSplitFraction (PITFALL-A).
        var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
        val dividerPx = fractionToPx(dividerFraction, totalPx)

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

        if (orientation == AeroSplitOrientation.Horizontal) {
            Row(modifier = Modifier.fillMaxSize()) {
                // First pane — fixed width derived from dividerPx (NOT weight — weight recalculates both slots on drag)
                Box(
                    modifier = Modifier
                        .width(with(density) { dividerPx.toDp() })
                        .fillMaxHeight()
                ) { start() }

                // 8dp hit-area with 1dp Aero visual line + grip nasechki (horizontal orientation)
                SplitPaneDivider(
                    orientation = composeOrientation,
                    onDrag = onDrag,
                    modifier = Modifier.width(8.dp).fillMaxHeight(),
                    visualModifier = Modifier.width(1.dp).fillMaxHeight(),
                    isHorizontal = true,
                )

                // Second pane — weight(1f) fills remaining width
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { end() }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // First pane — fixed height from dividerPx (NOT weight)
                Box(
                    modifier = Modifier
                        .height(with(density) { dividerPx.toDp() })
                        .fillMaxWidth()
                ) { start() }

                // 8dp hit-area with 1dp Aero visual line + grip nasechki (vertical orientation)
                SplitPaneDivider(
                    orientation = composeOrientation,
                    onDrag = onDrag,
                    modifier = Modifier.height(8.dp).fillMaxWidth(),
                    visualModifier = Modifier.height(1.dp).fillMaxWidth(),
                    isHorizontal = false,
                )

                // Second pane — weight(1f) fills remaining height
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) { end() }
            }
        }
    }
}

/**
 * Internal divider composable: 8dp hit-area Box containing the 1dp Aero visual line
 * and grip nasechki (3 dots), with hover tint via `buttonHover` overlay.
 *
 * The [aeroDragSplitter] Modifier handles all drag events, cursor change, and
 * `pointerHoverIcon` — no manual drag loop or gesture detection needed here (PITFALL-03).
 */
@Composable
private fun SplitPaneDivider(
    orientation: Orientation,
    onDrag: (Float) -> Unit,
    modifier: Modifier,
    visualModifier: Modifier,
    isHorizontal: Boolean,
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .aeroDragSplitter(orientation = orientation, onDrag = onDrag)
            .background(if (hovered) colors.buttonHover else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        // 1dp Aero visual line
        Box(modifier = visualModifier.background(colors.borderDefault))

        // Grip nasechki — 3 small dots centered on the line, in labelText color
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
                            .background(colors.labelText)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { idx ->
                    if (idx > 0) Box(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(colors.labelText)
                    )
                }
            }
        }
    }
}
