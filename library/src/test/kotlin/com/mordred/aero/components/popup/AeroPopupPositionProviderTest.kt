package com.mordred.aero.components.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Wave-0 smoke + Wave-1 real assertions for AeroPopupPositionProvider. */
class AeroPopupPositionProviderTest {

    @Test
    fun bottomSidePositionsBelowAnchor() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Bottom, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 100, right = 200, bottom = 130),
            windowSize = IntSize(width = 1000, height = 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 200, height = 100)
        )
        assertEquals(100, pos.x)
        assertEquals(130 + 4, pos.y)
    }

    @Test
    fun topSidePositionsAboveAnchor() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Top, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 500, right = 200, bottom = 530),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 100)
        )
        assertEquals(100, pos.x)
        assertEquals(500 - 100 - 4, pos.y)
    }

    @Test
    fun bottomSideAutoFlipsToTopOnOverflow() {
        // Anchor near bottom of window with a tall popup → must flip ABOVE
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Bottom, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 920, right = 200, bottom = 950),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 200)
        )
        // Flipped: y = anchorTop - popupHeight - gap = 920 - 200 - 4 = 716
        assertTrue(pos.y < 920, "must flip above anchor when bottom would overflow; got y=${pos.y}")
    }

    @Test
    fun topSideAutoFlipsToBottomOnOverflow() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Top, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 30, right = 200, bottom = 60),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 200)
        )
        // Flipped: y = anchorBottom + gap = 60 + 4 = 64
        assertTrue(pos.y > 60, "must flip below when top would overflow; got y=${pos.y}")
    }

    @Test
    fun startSideAutoFlipsToEndOnOverflow() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Start, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 30, top = 100, right = 130, bottom = 130),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 100)
        )
        assertTrue(pos.x > 130, "must flip to End side when Start would overflow; got x=${pos.x}")
    }

    @Test
    fun endSideAutoFlipsToStartOnOverflow() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.End, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 870, top = 100, right = 970, bottom = 130),
            windowSize = IntSize(1000, 1000),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(200, 100)
        )
        assertTrue(pos.x < 870, "must flip to Start side when End would overflow; got x=${pos.x}")
    }

    @Test
    fun clampsToWindowBoundsWhenBothSidesOverflow() {
        val provider = AeroPopupPositionProvider(side = AeroPopupSide.Bottom, gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 0, top = 0, right = 100, bottom = 30),
            windowSize = IntSize(200, 200),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(500, 500)
        )
        // Clamped non-negative
        assertTrue(pos.x >= 0, "x must clamp >= 0; got ${pos.x}")
        assertTrue(pos.y >= 0, "y must clamp >= 0; got ${pos.y}")
    }
}
