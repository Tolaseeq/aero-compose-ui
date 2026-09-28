package com.mordred.aero.components.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.mordred.aero.internal.windows.NativeChromeStatus
import com.mordred.aero.internal.windows.resolveComposeMinimumDp
import java.awt.Cursor

/**
 * WIN-02: the drag zones compose only on the platform path that owns resizing —
 * Floating placement AND no active native chrome (once the Windows subclass answers
 * HTLEFT..HTBOTTOMRIGHT for this window, Windows owns resizing and a second,
 * Compose-side drag path must not exist).
 */
internal fun shouldComposeResizeHandles(placement: WindowPlacement, nativeChromeActive: Boolean): Boolean =
    placement == WindowPlacement.Floating && !nativeChromeActive

/**
 * Eight invisible resize-zone Boxes (4 edges + 4 corners) for an undecorated
 * Compose window. Each zone:
 *  - displays a native resize cursor on hover (N / S / E / W / NE / NW / SE / SW)
 *  - on drag, mutates `windowState.size` (and `windowState.position` for
 *    top/left/top-corner zones, so the anchored opposite edge stays fixed)
 *
 * On Windows, while native window management is active for this window (an
 * `AeroTitleBar(..., nativeWindowManagement = true)` composition with a successful
 * native install), this composable composes nothing: Windows itself resizes the
 * window from every edge and corner with the system cursors, and a second,
 * Compose-side drag path must not exist. On Linux/macOS — and on Windows for a
 * window opted out via `nativeWindowManagement = false` or after a failed install —
 * the eight zones work exactly as before. The zones are also skipped while
 * `windowState.placement != WindowPlacement.Floating` (resizing a Maximized window
 * doesn't make sense).
 *
 * Zone dimensions:
 *  - Edge bands: 4.dp thick, run the length of their side (corners overlay on top
 *    so corner zones win on intersection by virtue of being painted later).
 *  - Corners: 8.dp × 8.dp, anchored at each corner of the window.
 *
 * Minimum size: the app's own AWT `window.minimumSize` when set, otherwise
 * 320 × 240 dp (D-01) — read at drag time, so a minimum the app sets later is
 * honored.
 *
 * Place this composable inside the same Box as your title bar + content, e.g.:
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     Column(...) { AeroTitleBar(...); ShowcaseApp() }
 *     AeroResizeHandles(windowState)   // overlay last so it sits on top
 * }
 * ```
 *
 * Visibility: declared `public` because `:showcase` is a separate Gradle module
 * and Kotlin `internal` is per-module — `internal` would block showcase access.
 */
@Composable
public fun FrameWindowScope.AeroResizeHandles(windowState: WindowState) {
    val nativeChromeActive by NativeChromeStatus.activeState(window)
    if (!shouldComposeResizeHandles(windowState.placement, nativeChromeActive)) return

    val edge = 4.dp
    val corner = 8.dp

    // Native cursor icons (built once, reused per recomposition)
    val cursorN  = remember { PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))  }
    val cursorS  = remember { PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))  }
    val cursorE  = remember { PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))  }
    val cursorW  = remember { PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR))  }
    val cursorNE = remember { PointerIcon(Cursor(Cursor.NE_RESIZE_CURSOR)) }
    val cursorNW = remember { PointerIcon(Cursor(Cursor.NW_RESIZE_CURSOR)) }
    val cursorSE = remember { PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)) }
    val cursorSW = remember { PointerIcon(Cursor(Cursor.SW_RESIZE_CURSOR)) }

    Box(Modifier.fillMaxSize()) {
        // ── EDGES ─────────────────────────────────────────────────────────
        // Top edge — drag changes height (negative) AND position.y
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(edge)
                .pointerHoverIcon(cursorN)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val dyDp = drag.y.toDp().value
                        val newH = (windowState.size.height.value - dyDp).coerceAtLeast(minH).dp
                        val deltaApplied = windowState.size.height.value - newH.value
                        windowState.size = DpSize(windowState.size.width, newH)
                        val curPos = windowState.position
                        if (curPos is WindowPosition.Absolute) {
                            windowState.position = WindowPosition(curPos.x, (curPos.y.value + deltaApplied).dp)
                        }
                    }
                }
        )

        // Bottom edge — drag changes height
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(edge)
                .pointerHoverIcon(cursorS)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val newH = (windowState.size.height.value + drag.y.toDp().value).coerceAtLeast(minH).dp
                        windowState.size = DpSize(windowState.size.width, newH)
                    }
                }
        )

        // Right edge — drag changes width
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(edge)
                .fillMaxHeight()
                .pointerHoverIcon(cursorE)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val newW = (windowState.size.width.value + drag.x.toDp().value).coerceAtLeast(minW).dp
                        windowState.size = DpSize(newW, windowState.size.height)
                    }
                }
        )

        // Left edge — drag changes width (negative) AND position.x
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(edge)
                .fillMaxHeight()
                .pointerHoverIcon(cursorW)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val dxDp = drag.x.toDp().value
                        val newW = (windowState.size.width.value - dxDp).coerceAtLeast(minW).dp
                        val deltaApplied = windowState.size.width.value - newW.value
                        windowState.size = DpSize(newW, windowState.size.height)
                        val curPos = windowState.position
                        if (curPos is WindowPosition.Absolute) {
                            windowState.position = WindowPosition((curPos.x.value + deltaApplied).dp, curPos.y)
                        }
                    }
                }
        )

        // ── CORNERS ──────────────────────────────────────────────────────
        // Top-Left corner — NW resize: width-, height-, position.x+, position.y+
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(corner)
                .pointerHoverIcon(cursorNW)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val dxDp = drag.x.toDp().value
                        val dyDp = drag.y.toDp().value
                        val newW = (windowState.size.width.value  - dxDp).coerceAtLeast(minW).dp
                        val newH = (windowState.size.height.value - dyDp).coerceAtLeast(minH).dp
                        val dxApplied = windowState.size.width.value  - newW.value
                        val dyApplied = windowState.size.height.value - newH.value
                        windowState.size = DpSize(newW, newH)
                        val curPos = windowState.position
                        if (curPos is WindowPosition.Absolute) {
                            windowState.position = WindowPosition(
                                (curPos.x.value + dxApplied).dp,
                                (curPos.y.value + dyApplied).dp
                            )
                        }
                    }
                }
        )

        // Top-Right corner — NE resize: width+, height-, position.y+
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(corner)
                .pointerHoverIcon(cursorNE)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val dyDp = drag.y.toDp().value
                        val newW = (windowState.size.width.value  + drag.x.toDp().value).coerceAtLeast(minW).dp
                        val newH = (windowState.size.height.value - dyDp).coerceAtLeast(minH).dp
                        val dyApplied = windowState.size.height.value - newH.value
                        windowState.size = DpSize(newW, newH)
                        val curPos = windowState.position
                        if (curPos is WindowPosition.Absolute) {
                            windowState.position = WindowPosition(curPos.x, (curPos.y.value + dyApplied).dp)
                        }
                    }
                }
        )

        // Bottom-Left corner — SW resize: width-, height+, position.x+
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(corner)
                .pointerHoverIcon(cursorSW)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val dxDp = drag.x.toDp().value
                        val newW = (windowState.size.width.value  - dxDp).coerceAtLeast(minW).dp
                        val newH = (windowState.size.height.value + drag.y.toDp().value).coerceAtLeast(minH).dp
                        val dxApplied = windowState.size.width.value - newW.value
                        windowState.size = DpSize(newW, newH)
                        val curPos = windowState.position
                        if (curPos is WindowPosition.Absolute) {
                            windowState.position = WindowPosition((curPos.x.value + dxApplied).dp, curPos.y)
                        }
                    }
                }
        )

        // Bottom-Right corner — SE resize: width+, height+
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(corner)
                .pointerHoverIcon(cursorSE)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        val (minW, minH) = resolveComposeMinimumDp(
                            window.isMinimumSizeSet, window.minimumSize.width, window.minimumSize.height,
                        )
                        val newW = (windowState.size.width.value  + drag.x.toDp().value).coerceAtLeast(minW).dp
                        val newH = (windowState.size.height.value + drag.y.toDp().value).coerceAtLeast(minH).dp
                        windowState.size = DpSize(newW, newH)
                    }
                }
        )
    }
}
