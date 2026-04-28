package com.mordred.aero.components.buttons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mordred.aero.theme.AeroTheme

/**
 * Aero-styled outlined button (BTN-02).
 *
 * Transparent background with a themed border. All colors read from [AeroTheme.colors].
 * Hover: [AeroTheme.colors.buttonHover] overlay via drawWithContent.
 * Pressed: scale 0.97f via animateFloatAsState, 150ms LinearEasing.
 * Focus: border widens to 2.dp in [AeroTheme.colors.borderSelected].
 * Disabled: 0.4 alpha on content and border.
 *
 * @param text Label text shown on the button.
 * @param onClick Invoked when the button is clicked.
 * @param modifier Optional [Modifier] for the outer layout.
 * @param enabled Whether the button accepts input.
 * @param height Height of the button.
 * @param interactionSource Shared [MutableInteractionSource] for all state collectors.
 */
@Composable
public fun AeroOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 28.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors

    val hovered by rememberHoverState(interactionSource)
    val pressed by rememberPressedState(interactionSource)
    val focused by rememberFocusState(interactionSource)

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
        label = "pressedScale"
    )

    val borderColor = when {
        !enabled -> colors.borderDefault.copy(alpha = 0.4f)
        focused -> colors.borderSelected
        else -> colors.glassBorder
    }
    val borderWidth = if (focused && enabled) 2.dp else 1.dp

    val buttonHoverColor = colors.buttonHover

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .height(height)
                    .drawWithContent {
                        drawContent()
                        if (hovered && enabled) {
                            drawRect(buttonHoverColor)
                        }
                    },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                border = BorderStroke(width = borderWidth, color = borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.onSurface,
                    disabledContentColor = colors.onSurface.copy(alpha = 0.4f)
                ),
                interactionSource = interactionSource
            ) {
                Text(
                    text = text,
                    style = AeroTheme.typography.bodyLarge,
                    fontSize = 14.sp
                )
            }
        }
    }
}
