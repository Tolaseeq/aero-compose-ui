package com.mordred.aero.components.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Public position provider for any anchored Aero popup.
 *
 * Auto-flips to the opposite side when the chosen side would overflow the window:
 *  - Bottom → Top  (and vice versa)
 *  - Start  → End  (and vice versa)
 *
 * After flipping, the result is clamped to non-negative offsets and to fit within
 * window bounds where possible.
 *
 * @param side desired side relative to the anchor; defaults to [AeroPopupSide.Bottom].
 * @param gap pixel gap between anchor and popup; defaults to 4.
 */
public class AeroPopupPositionProvider(
    private val side: AeroPopupSide = AeroPopupSide.Bottom,
    private val gap: Int = 4
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // First-measurement quirk: Compose Desktop calls calculatePosition once with
        // popupContentSize ≈ windowSize before the popup content has actually measured.
        // Both the auto-flip branch (treats placeholder as overflow → flips & clamps to
        // screen origin) AND the final clamp (maxY = windowSize - popup.height becomes
        // 0 → forces y=0) conspire to render a window-sized rectangle at (0, 0) for one
        // frame — the "huge menu flickers at the top" the user has reported four times.
        // Park the placeholder pass off-screen entirely so nothing is visible until the
        // real measurement arrives.
        val unmeasured = popupContentSize.width >= windowSize.width ||
                         popupContentSize.height >= windowSize.height
        if (unmeasured) {
            return IntOffset(windowSize.width + popupContentSize.width,
                             windowSize.height + popupContentSize.height)
        }
        val proposed = primaryFor(side, anchorBounds, popupContentSize)
        val flipped = if (overflows(proposed, windowSize, popupContentSize)) {
            primaryFor(opposite(side), anchorBounds, popupContentSize)
        } else proposed
        return clamp(flipped, windowSize, popupContentSize)
    }

    private fun primaryFor(
        s: AeroPopupSide, anchor: IntRect, popup: IntSize
    ): IntOffset = when (s) {
        AeroPopupSide.Bottom -> IntOffset(anchor.left, anchor.bottom + gap)
        AeroPopupSide.Top    -> IntOffset(anchor.left, anchor.top - popup.height - gap)
        AeroPopupSide.Start  -> IntOffset(anchor.left - popup.width - gap, anchor.top)
        AeroPopupSide.End    -> IntOffset(anchor.right + gap, anchor.top)
    }

    private fun opposite(s: AeroPopupSide): AeroPopupSide = when (s) {
        AeroPopupSide.Top    -> AeroPopupSide.Bottom
        AeroPopupSide.Bottom -> AeroPopupSide.Top
        AeroPopupSide.Start  -> AeroPopupSide.End
        AeroPopupSide.End    -> AeroPopupSide.Start
    }

    private fun overflows(p: IntOffset, win: IntSize, popup: IntSize): Boolean {
        val outRight  = p.x + popup.width  > win.width
        val outBottom = p.y + popup.height > win.height
        val outLeft   = p.x < 0
        val outTop    = p.y < 0
        return outRight || outBottom || outLeft || outTop
    }

    private fun clamp(p: IntOffset, win: IntSize, popup: IntSize): IntOffset {
        val maxX = (win.width  - popup.width ).coerceAtLeast(0)
        val maxY = (win.height - popup.height).coerceAtLeast(0)
        val cx = p.x.coerceIn(0, maxX)
        val cy = p.y.coerceIn(0, maxY)
        return IntOffset(cx, cy)
    }
}
