package com.mordred.aero.components.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AeroCursorPositionProviderTest {

    @Test
    fun positionsPopupAtCursor() {
        val provider = AeroCursorPositionProvider(cursor = IntOffset(150, 250))
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(0, 0, 100, 100),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(120, 80)
        )
        assertEquals(150, pos.x)
        assertEquals(250, pos.y)
    }

    @Test
    fun clampsCursorToWindowBoundsWhenPopupExceedsRight() {
        val provider = AeroCursorPositionProvider(cursor = IntOffset(950, 100))
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(0, 0, 0, 0),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 100)
        )
        // 950 + 200 = 1150 > 1000 → clamp to 800 (1000 - 200)
        assertTrue(pos.x + 200 <= 1000, "popup must fit horizontally; got x=${pos.x}")
    }
}
