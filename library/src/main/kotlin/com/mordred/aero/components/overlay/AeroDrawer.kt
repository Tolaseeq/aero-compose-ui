package com.mordred.aero.components.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.theme.AeroTheme
import kotlin.math.roundToInt

/** Position provider that places a popup over the entire host window. */
private object FullWindowPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(0, 0)
}

/** Side from which an [AeroDrawer] slides in. */
public enum class AeroDrawerSide { Start, End }

/**
 * OVL-08: Sliding side drawer (modal). Covers the parent window with a scrim while open.
 *
 * Animation: 220ms LinearEasing tween on offset.x; matching tween on scrim alpha.
 * Dismiss: scrim click + Esc.
 *
 * **Closed-but-composed pitfall:** when fully closed AND the slide animation has
 * completed, this composable early-returns so it does not consume pointer input
 * over the main UI.
 *
 * Top/Bottom drawer variants are deferred to v2 — current API exposes only Start/End.
 *
 * @param open whether the drawer is shown.
 * @param onDismissRequest invoked when scrim is clicked or Esc is pressed.
 * @param side which side the drawer slides in from.
 * @param drawerWidth panel width (caller-controlled; default 280.dp).
 * @param content panel content; rendered inside a [Column].
 */
@Composable
public fun AeroDrawer(
    open: Boolean,
    onDismissRequest: () -> Unit,
    side: AeroDrawerSide = AeroDrawerSide.Start,
    drawerWidth: Dp = 280.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val offsetFraction by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "drawerOffsetFraction"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (open) 0.5f else 0f,
        animationSpec = tween(durationMillis = 220, easing = LinearEasing),
        label = "drawerScrimAlpha"
    )

    // Closed-but-composed pitfall: return when fully closed AND animation done.
    if (!open && offsetFraction == 1f) return

    val signedOffset = if (side == AeroDrawerSide.Start) -offsetFraction else offsetFraction
    val align = if (side == AeroDrawerSide.Start) Alignment.CenterStart else Alignment.CenterEnd

    val scrimInteraction = remember { MutableInteractionSource() }
    val colors = AeroTheme.colors
    val panelTint = colors.panelBackground.copy(
        alpha = colors.panelBackground.alpha * 0.7f
    )
    val highlight = colors.glassHighlight

    Popup(
        popupPositionProvider = FullWindowPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                        onDismissRequest()
                        true
                    } else false
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
            Box(
                modifier = Modifier
                    .align(align)
                    .offset {
                        IntOffset(
                            x = (signedOffset * drawerWidth.toPx()).roundToInt(),
                            y = 0
                        )
                    }
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .drawBehind {
                        drawRect(color = panelTint)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(highlight, Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.4f
                            )
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(content = content)
            }
        }
    }
}
