package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.popup.AeroPopupPositionProvider
import com.mordred.aero.components.popup.AeroPopupSide
import com.mordred.aero.theme.AeroTheme
import kotlinx.coroutines.delay

/**
 * OVL-03: Tooltip displayed above an anchor element after a 600ms hover delay.
 *
 * - **In-delay:** 600ms — established UX threshold for "user paused on element".
 * - **Out-delay:** 0ms — tooltip dismisses immediately on hover-leave.
 * - **Position:** [AeroPopupSide.Top] with auto-flip to Bottom on overflow.
 * - **Behavior:** anchor-fixed (does NOT follow the cursor).
 * - **Pitfall:** uses `PopupProperties(focusable = false)` so the popup does NOT
 *   steal hover state from the anchor.
 *
 * Two API entry points:
 *  1. Wrapper: `AeroTooltip(text = "...") { AnchorElement(...) }` — wraps an arbitrary
 *     anchor composable.
 *  2. Convenience extension: `Modifier.aeroTooltip("...")` — apply directly to any composable.
 *
 * @param text the tooltip's body text.
 * @param content the anchor composable (when using wrapper form).
 */
@Composable
public fun AeroTooltip(
    text: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(hovered) {
        if (hovered) {
            delay(600)
            visible = true
        } else {
            visible = false
        }
    }

    Box(modifier = Modifier.hoverable(interactionSource)) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider = remember {
                    AeroPopupPositionProvider(side = AeroPopupSide.Top, gap = 4)
                },
                properties = PopupProperties(focusable = false)
            ) {
                TooltipBody(text)
            }
        }
    }
}

/** Convenience extension — equivalent to wrapping the receiver in [AeroTooltip]. */
public fun Modifier.aeroTooltip(text: String): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(hovered) {
        if (hovered) {
            delay(600)
            visible = true
        } else {
            visible = false
        }
    }

    if (visible) {
        Popup(
            popupPositionProvider = remember {
                AeroPopupPositionProvider(side = AeroPopupSide.Top, gap = 4)
            },
            properties = PopupProperties(focusable = false)
        ) {
            TooltipBody(text)
        }
    }
    this.hoverable(interactionSource)
}

@Composable
private fun TooltipBody(text: String) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(colors.panelBackground, shape)
            .border(1.dp, colors.glassBorder, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = colors.onSurface,
            style = AeroTheme.typography.bodyMedium
        )
    }
}
