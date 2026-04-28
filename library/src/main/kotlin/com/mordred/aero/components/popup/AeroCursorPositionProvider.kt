package com.mordred.aero.components.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Position provider that anchors a popup at an arbitrary cursor coordinate
 * (used by AeroContextMenu for right-click menus). The given [cursor] is in
 * window-local coordinates. Result is clamped so the popup stays within
 * the window rectangle.
 */
public class AeroCursorPositionProvider(
    private val cursor: IntOffset
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width  - popupContentSize.width ).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(cursor.x.coerceIn(0, maxX), cursor.y.coerceIn(0, maxY))
    }
}
