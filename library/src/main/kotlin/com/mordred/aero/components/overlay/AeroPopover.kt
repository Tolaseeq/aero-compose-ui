package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.popup.AeroPopupPositionProvider
import com.mordred.aero.components.popup.AeroPopupSide
import com.mordred.aero.theme.AeroTheme

/**
 * OVL-07: Aero popover. Displays arbitrary content next to an anchor element using
 * the public [AeroPopupPositionProvider] (auto-flips on overflow).
 *
 * The caller controls visibility imperatively via the [expanded] flag — typically a
 * `mutableStateOf(false)` toggled by an anchor-side button.
 *
 * **Arrow/caret pointing at anchor:** deferred to v2 per CONTEXT.md "Claude's Discretion"
 * (would require Canvas math per side; flat popover is the v1 default).
 *
 * @param expanded whether to show the popover.
 * @param onDismissRequest invoked on outside click or back press.
 * @param side which side of the anchor; defaults to [AeroPopupSide.Bottom].
 * @param gap anchor-to-popup gap in pixels.
 * @param modifier optional outer modifier.
 * @param content popover body.
 */
@Composable
public fun AeroPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    side: AeroPopupSide = AeroPopupSide.Bottom,
    gap: Int = 4,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!expanded) return
    Popup(
        popupPositionProvider = remember(side, gap) {
            AeroPopupPositionProvider(side = side, gap = gap)
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Glass-style background: opaque theme base, then a vertical-gradient panel
        // tint (top brighter → bottom faded) so text reads cleanly while preserving
        // the Aero glass look. Border tinted with titlebar-gradient hue so the
        // popover has a visible, brand-consistent edge.
        val colors = AeroTheme.colors
        val shape = RoundedCornerShape(6.dp)
        Box(
            modifier = modifier
                .shadow(elevation = 6.dp, shape = shape)
                .clip(shape)
                .background(colors.background, shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.panelBackground,
                            colors.panelBackground.copy(alpha = colors.panelBackground.alpha * 0.7f)
                        )
                    ),
                    shape
                )
                .border(1.dp, colors.titleBarGradientStart, shape)
                .padding(12.dp)
        ) {
            content()
        }
    }
}
