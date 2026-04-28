package com.mordred.aero.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * SEL-03: Toggle switch with 150ms animated thumb position and track color.
 *
 * Track: 36x18dp rounded rect. Thumb: 14x14dp circle.
 * Thumb animates from x=2dp (off) to x=20dp (on) via animateFloatAsState tween(150).
 */
@Composable
public fun AeroSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(50)

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.borderDefault,
        animationSpec = tween(150, easing = LinearEasing),
        label = "switchTrack"
    )

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "switchThumb"
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .width(36.dp)
            .height(18.dp)
            .clip(shape)
            .background(trackColor, shape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) }
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((2.dp + 18.dp * thumbProgress).roundToPx(), 0) }
                .size(14.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.surface, RoundedCornerShape(50))
        )
    }
}
