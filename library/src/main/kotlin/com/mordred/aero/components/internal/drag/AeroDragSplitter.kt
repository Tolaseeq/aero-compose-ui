package com.mordred.aero.components.internal.drag

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.Cursor

/**
 * Modifier extension exposing `awaitPointerEventScope` drag with cursor change.
 *
 * **PITFALL-03 (touchSlop=18dp):** Compose Desktop's `detectDragGestures` and
 * `awaitDragOrCancellation` enforce an 18dp touchSlop before firing the first drag event,
 * which silently breaks Canvas-based mouse drag on Desktop (issue JetBrains/compose-jb #343,
 * unresolved as of CMP 1.7.3). This Modifier uses `awaitPointerEventScope` + manual
 * event loop — `onDrag` fires on the FIRST mouse-move event after pointer-down, not after
 * 18dp of accumulated movement. **`detectDragGestures` is BANNED for all Canvas drag in v2.0.**
 *
 * **Locked design** (per 07-CONTEXT.md):
 * - Modifier-based, NOT a content-slot Composable. Both consumers (AeroSplitPane Phase 10,
 *   AeroDataTable column-resize Phase 9) have very different visual treatments + hit-area
 *   geometry; a content-slot Composable would either grow arms or force one consumer to
 *   fight the API.
 * - `onDrag` receives `Float` (1D delta along the orientation axis), NOT `Offset`. Both
 *   consumers operate in 1D — orientation parameter selects the axis at composition time.
 * - The Modifier owns ONLY: drag loop + cursor + enabled gate. Consumers wrap with their
 *   own visual line, hit-area thickness, color, and shape.
 *
 * **Consume semantics:** `change.consume()` is called only when we actually applied a
 * drag delta (`pressed && delta != 0f`). The release event is NOT consumed — parents
 * (e.g. nested SplitPanes) must see the pointer release.
 *
 * @param orientation drag axis selector. Horizontal -> reports `cur.x - prev.x`; Vertical -> `cur.y - prev.y`.
 * @param onDrag fires on every mouse move with non-zero delta along the orientation axis.
 * @param onDragEnd fires once when `change.pressed` becomes false at the end of a drag sequence.
 * @param enabled when false, no pointer input is attached (consumer can disable at min/max bounds).
 */
internal fun Modifier.aeroDragSplitter(
    orientation: Orientation,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val cursor = remember(orientation) {
        when (orientation) {
            Orientation.Horizontal -> PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
            Orientation.Vertical   -> PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))
        }
    }
    this
        .pointerHoverIcon(cursor)
        .pointerInput(orientation, enabled) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var prev = down.position
                    // Inner loop: process every event until pressed=false.
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            onDragEnd()
                            break
                        }
                        val cur = change.position
                        val delta = when (orientation) {
                            Orientation.Horizontal -> cur.x - prev.x
                            Orientation.Vertical   -> cur.y - prev.y
                        }
                        if (delta != 0f) {
                            onDrag(delta)
                            change.consume()  // only consume when we actually handled the move
                        }
                        prev = cur
                    }
                }
            }
        }
}
