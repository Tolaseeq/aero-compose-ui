package com.mordred.aero.components.buttons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Aero-styled square icon button (BTN-03).
 *
 * Hover: [AeroTheme.colors.buttonHover] background drawn via a Box overlay (no M3 ripple).
 * Pressed: scale 0.97f via animateFloatAsState, 150ms LinearEasing.
 * Focus: 2.dp [AeroTheme.colors.borderSelected] border.
 * Disabled: 0.4 alpha on the outer Box and content color.
 *
 * `indication = null` is intentional — hover/pressed states are drawn manually;
 * the M3 ripple is suppressed to avoid a double-effect.
 *
 * @param onClick Invoked when the button is clicked.
 * @param modifier Optional [Modifier] for the outer layout.
 * @param enabled Whether the button accepts input.
 * @param size Square size of the button (default 32.dp).
 * @param interactionSource Shared [MutableInteractionSource] for state collectors.
 * @param content The icon or other composable to center inside the button.
 */
@Composable
public fun AeroIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 32.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
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

    val shape = RoundedCornerShape(4.dp)

    val focusBorderModifier = if (focused && enabled) {
        Modifier.border(2.dp, colors.borderSelected, shape)
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.4f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(focusBorderModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Hover background overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = if (hovered && enabled) colors.buttonHover else Color.Transparent,
                    shape = shape
                )
        )
        // Icon content with disabled content color propagation
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.4f)
        ) {
            content()
        }
    }
}
