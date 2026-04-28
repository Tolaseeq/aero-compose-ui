package com.mordred.aero.components.dropdown

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.theme.AeroTheme

/**
 * Internal PopupPositionProvider that positions the popup below the anchor,
 * clamping to window bounds. If there is no room below, flips above the anchor.
 */
internal class AeroPopupPositionProvider(
    private val verticalGap: Int = 4
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Default: position popup directly below anchor, left-aligned with anchor
        val proposedX = anchorBounds.left
        val proposedY = anchorBounds.bottom + verticalGap

        // Clamp X so popup does not exceed window right edge
        val maxX = windowSize.width - popupContentSize.width
        val clampedX = proposedX.coerceIn(0, maxX.coerceAtLeast(0))

        // If popup would exceed window bottom, flip ABOVE anchor instead
        val clampedY = if (proposedY + popupContentSize.height > windowSize.height) {
            (anchorBounds.top - popupContentSize.height - verticalGap).coerceAtLeast(0)
        } else {
            proposedY
        }

        return IntOffset(clampedX, clampedY)
    }
}

/**
 * Internal composable for a single dropdown list item.
 * Renders selected/highlighted/hover states with a 150ms color tween.
 */
@Composable
internal fun AeroDropdownItem(
    text: String,
    selected: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AeroTheme.colors
    val bg by animateColorAsState(
        targetValue = when {
            selected -> colors.primary.copy(alpha = 0.3f)
            highlighted -> colors.buttonHover
            else -> Color.Transparent
        },
        animationSpec = tween(150, easing = LinearEasing),
        label = "itemBg"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = colors.onSurface, style = AeroTheme.typography.bodyLarge)
    }
}

/**
 * Internal generic popup wrapper used by both [AeroDropdown] and [AeroComboBox].
 * Handles Popup lifecycle, positioning, and glass styling.
 *
 * @param expanded Whether the popup is visible.
 * @param onDismissRequest Called when the popup should be dismissed.
 * @param anchorWidth Width of the trigger/anchor element. The popup will be at least this wide
 *   so it visually aligns with the trigger. Pass [Dp.Unspecified] to let the popup wrap content.
 * @param onKeyEvent Key event handler for keyboard navigation within the popup.
 * @param content The popup's scrollable item list.
 */
@Composable
internal fun AeroDropdownPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorWidth: Dp = Dp.Unspecified,
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    // Popup background: paint a fully-opaque base first, then the panelBackground tint on top.
    // panelBackground has 0xCC (80%) alpha which lets underlying content bleed through;
    // the opaque base layer eliminates that transparency while preserving the Aero tint.
    val popupBackground = colors.panelBackground

    // Width modifier: if anchor width is specified, set popup to exactly that width;
    // otherwise use widthIn(min = 120.dp) to wrap content with a sane minimum.
    val widthModifier = if (anchorWidth != Dp.Unspecified) {
        Modifier.widthIn(min = anchorWidth, max = anchorWidth)
    } else {
        Modifier.widthIn(min = 120.dp)
    }

    Popup(
        popupPositionProvider = remember { AeroPopupPositionProvider() },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            widthModifier
                .heightIn(max = 320.dp)
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .background(colors.background, shape)
                .background(popupBackground, shape)
                .border(1.dp, colors.glassBorder, shape)
                .onPreviewKeyEvent(onKeyEvent)
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), content = content)
        }
    }
}
