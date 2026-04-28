package com.mordred.aero.components.popup

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.theme.AeroTheme

/**
 * Public composable for a single dropdown list item.
 * Renders selected/highlighted/hover states with a 150ms color tween.
 *
 * Reusable by [AeroDropdownPopup] and any other list-style popup
 * (e.g. AeroDropdown, AeroComboBox, AeroMenuBar, AeroPopover, AeroContextMenu).
 */
@Composable
public fun AeroDropdownItem(
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
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = colors.onSurface, style = AeroTheme.typography.bodyLarge)
    }
}

/**
 * Public reusable popup wrapper for anchored dropdowns / menus.
 *
 * Used by `AeroDropdown`, `AeroComboBox`, and `AeroMenuBar`. Handles popup lifecycle,
 * position-and-flip via [AeroPopupPositionProvider], and Aero glass styling. The
 * scrollable content is hosted in a `Modifier.verticalScroll` Column inside a Box that
 * carries both `widthIn` and `heightIn(max = 320.dp)` so the popup hugs its content.
 *
 * @param expanded Whether the popup is visible.
 * @param onDismissRequest Called when the popup should be dismissed.
 * @param anchorWidth Width of the trigger/anchor element. The popup will be at least this wide
 *   so it visually aligns with the trigger. Pass [Dp.Unspecified] for a 120-320.dp range.
 * @param side Side relative to the anchor on which to place the popup. Auto-flips on overflow.
 * @param onKeyEvent Key event handler for keyboard navigation within the popup.
 * @param content The popup's scrollable item list.
 */
@Composable
public fun AeroDropdownPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorWidth: Dp = Dp.Unspecified,
    side: AeroPopupSide = AeroPopupSide.Bottom,
    onKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    val widthModifier = if (anchorWidth != Dp.Unspecified) {
        Modifier.widthIn(min = anchorWidth, max = anchorWidth)
    } else {
        Modifier.widthIn(min = 120.dp, max = 320.dp)
    }

    Popup(
        popupPositionProvider = remember(side) { AeroPopupPositionProvider(side = side) },
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
                .background(colors.panelBackground, shape)
                .border(1.dp, colors.glassBorder, shape)
                .padding(vertical = 4.dp)
                .onPreviewKeyEvent(onKeyEvent)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), content = content)
        }
    }
}
