package com.mordred.aero.components.internal.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Phase 7 plan-01 — covers PITFALL-02 (width-clip) and PITFALL-08 (first-frame guard). */
class AeroCalendarPositionProviderTest {

    @Test
    fun firstFrameUnmeasuredPopupReturnsIntOffsetZero() {
        // PITFALL-08: when popupContentSize == IntSize.Zero we MUST return IntOffset.Zero.
        // This keeps the popup off-screen on frame 1 instead of flashing at a position
        // computed from a 0x0 size.
        val provider = AeroCalendarPositionProvider(gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 100, right = 200, bottom = 130),
            windowSize = IntSize(width = 1024, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize.Zero,
        )
        assertEquals(IntOffset.Zero, pos, "first-frame guard must return IntOffset.Zero")
    }

    @Test
    fun widePopupNearLeftEdgePositionsBelowAndLeftAligned() {
        // SC1: AeroCalendarPositionProvider positions a popup wider than its anchor without clipping.
        // 100dp anchor -> 560dp popup -> fits left-aligned on a 1024dp window.
        val provider = AeroCalendarPositionProvider(gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 100, right = 200, bottom = 130),
            windowSize = IntSize(width = 1024, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 560, height = 400),
        )
        assertEquals(100, pos.x, "left-aligned to anchorBounds.left")
        assertEquals(130 + 4, pos.y, "below anchor with gap=4")
        // No clip: popup fully visible inside window.
        assertTrue(pos.x + 560 <= 1024, "popup right edge inside window")
    }

    @Test
    fun widePopupNearRightEdgeRightAlignsWithoutClip() {
        // SC1 (right-edge case): 100dp anchor near right edge; 560dp popup would overflow if left-aligned.
        // Must right-align to anchorBounds.right - popupWidth, NOT flip Top/Bottom.
        val provider = AeroCalendarPositionProvider(gap = 4)
        val anchor = IntRect(left = 900, top = 100, right = 1000, bottom = 130)
        val popupSize = IntSize(width = 560, height = 400)
        val pos = provider.calculatePosition(
            anchorBounds = anchor,
            windowSize = IntSize(width = 1024, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupSize,
        )
        assertEquals(1000 - 560, pos.x, "right-aligned to anchor.right - popup.width")
        assertEquals(130 + 4, pos.y, "still below anchor — width overflow does NOT flip Top/Bottom")
        // No clip:
        assertTrue(pos.x >= 0, "popup left edge inside window")
        assertTrue(pos.x + popupSize.width <= 1024, "popup right edge inside window")
    }

    @Test
    fun overflowingBelowAnchorFlipsAbove() {
        // Vertical overflow: 600dp-tall anchor.top, 400dp popup, 800dp window -> flip above.
        val provider = AeroCalendarPositionProvider(gap = 4)
        val pos = provider.calculatePosition(
            anchorBounds = IntRect(left = 100, top = 600, right = 200, bottom = 630),
            windowSize = IntSize(width = 1024, height = 800),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 400),
        )
        // yBelow = 630 + 4 = 634; 634 + 400 = 1034 > 800 -> flip above
        // yAbove = 600 - 400 - 4 = 196
        assertEquals(196, pos.y, "must flip above when below would overflow window")
    }
}
