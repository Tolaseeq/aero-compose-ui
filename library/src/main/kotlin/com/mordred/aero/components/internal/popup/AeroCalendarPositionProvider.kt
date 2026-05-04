package com.mordred.aero.components.internal.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * `PopupPositionProvider` for popups whose content is wider than their anchor —
 * specifically the four date/time pickers in Phase 8 (and ColorPicker swatch popup if needed).
 *
 * **PITFALL-02 (calendar popup width clip):** existing `AeroPopupPositionProvider` width-locks
 * the popup to the anchor's width via `widthIn(min=anchorWidth, max=anchorWidth)` (in its consumer
 * `AeroDropdownPopup`). A 320dp calendar on a 240dp trigger would clip. This provider does NOT
 * width-lock — popups render at their natural width.
 *
 * **PITFALL-08 (first-frame popup flash):** existing provider uses `popupContentSize >= windowSize`
 * as the unmeasured guard. On a 1280dp window a 560dp wide-calendar popup never trips this guard
 * (popup is 0x0 then 560x400). Result: position computed from `(0,0)` on frame 1, then jumps. We use
 * `popupContentSize == IntSize.Zero` (struct equality) instead — the only condition that reliably
 * means "unmeasured".
 *
 * **Locked behaviors:**
 * 1. Default: left-aligned to `anchorBounds.left`, vertically below the anchor.
 * 2. Horizontal overflow (`anchorBounds.left + popupContentSize.width > windowSize.width`):
 *    right-align to `anchorBounds.right - popupContentSize.width` (clamped to 0).
 * 3. Vertical overflow below (`yBelow + popupContentSize.height > windowSize.height`): flip to above
 *    (`anchorBounds.top - popupContentSize.height - gap`, clamped to 0).
 * 4. **Width overflow does NOT trigger Top/Bottom flip** — only horizontal re-anchoring.
 * 5. First-frame guard: `popupContentSize == IntSize.Zero` returns `IntOffset.Zero`.
 *
 * @param gap pixel offset between anchor edge and popup edge along the vertical axis.
 */
internal class AeroCalendarPositionProvider(
    private val gap: Int = 4,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // First-frame guard (PITFALL-08): unmeasured popup -> off-screen.
        if (popupContentSize == IntSize.Zero) return IntOffset.Zero

        // Horizontal: prefer left-aligned to anchor.left; right-align if it would overflow.
        val xLeft = anchorBounds.left
        val xRight = anchorBounds.right - popupContentSize.width
        val x = if (xLeft + popupContentSize.width <= windowSize.width) {
            xLeft
        } else {
            xRight.coerceAtLeast(0)
        }

        // Vertical: prefer below; flip above if below would overflow.
        val yBelow = anchorBounds.bottom + gap
        val yAbove = anchorBounds.top - popupContentSize.height - gap
        val y = if (yBelow + popupContentSize.height <= windowSize.height) {
            yBelow
        } else {
            yAbove.coerceAtLeast(0)
        }

        return IntOffset(x, y)
    }
}
