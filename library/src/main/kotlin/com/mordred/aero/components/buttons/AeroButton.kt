package com.mordred.aero.components.buttons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
 * Aero-styled filled button (BTN-01).
 *
 * All colors are read from [AeroTheme.colors] — no hardcoded values.
 * Hover: [AeroTheme.colors.buttonHover] overlay drawn on top of the button.
 * Pressed: scale 0.97f via animateFloatAsState, 150ms LinearEasing.
 * Focus: 2.dp [AeroTheme.colors.borderSelected] outer border.
 * Disabled: 0.4 alpha on container and content.
 *
 * @param text Label text shown on the button.
 * @param onClick Invoked when the button is clicked.
 * @param modifier Optional [Modifier] for the outer layout.
 * @param enabled Whether the button accepts input.
 * @param height Height of the button.
 * @param interactionSource Shared [MutableInteractionSource] for all state collectors.
 */
@Composable
public fun AeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 30.dp,
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

    val focusModifier = if (focused && enabled) {
        Modifier.border(2.dp, colors.borderSelected, RoundedCornerShape(4.dp))
    } else {
        Modifier
    }

    val buttonHoverColor = colors.buttonHover

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(focusModifier)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Button(
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary.copy(alpha = 0.8f),
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.4f),
                    disabledContentColor = colors.onPrimary.copy(alpha = 0.4f)
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
