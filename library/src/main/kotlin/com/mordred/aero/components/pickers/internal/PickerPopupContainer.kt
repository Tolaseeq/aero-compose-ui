package com.mordred.aero.components.pickers.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

/**
 * Shared popup surface for all date/time pickers (PICK-01..04).
 *
 * Single source of truth for the W11-02 two-layer-background + [glassPanel] technique
 * lifted verbatim from `AeroDropdownPopup`. Wraps [content] in:
 *  - an opaque [AeroColorScheme.background] base layer (W11-02: undecorated Win11 windows
 *    have no compositor backdrop, so the popup must paint its own opaque base),
 *  - the translucent [AeroColorScheme.panelBackground] glass tint on top,
 *  - a 1.dp [AeroColorScheme.glassBorder] outline,
 *  - the `glassPanel` highlight gradient.
 *
 * IMPORTANT: deliberately NO elevation modifier — elevation clips on undecorated
 * Win11 popups (W11-02). Corner radius is fixed at 8.dp to match the date/time
 * picker family.
 */
@Composable
internal fun PickerPopupContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .background(colors.background, shape)
            .background(colors.panelBackground, shape)
            .border(1.dp, colors.glassBorder, shape)
            .glassPanel(cornerRadius = 8.dp)
    ) {
        content()
    }
}
