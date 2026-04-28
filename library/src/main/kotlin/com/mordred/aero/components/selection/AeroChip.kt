package com.mordred.aero.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * SEL-04: Filter chip with animated selected/unselected styling.
 *
 * Selected: primary.copy(alpha=0.25f) bg, primary border, primary text.
 * Unselected: cardBackground.copy(alpha=0.3f) bg, borderDefault.copy(0.5f) border, labelText.
 * Ported from MordredChip with AeroTheme token substitution.
 */
@Composable
public fun AeroChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.25f) else colors.cardBackground.copy(alpha = 0.3f),
        animationSpec = tween(150, easing = LinearEasing),
        label = "chipBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.borderDefault.copy(alpha = 0.5f),
        animationSpec = tween(150, easing = LinearEasing),
        label = "chipBorder"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.labelText,
        animationSpec = tween(150, easing = LinearEasing),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(22.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(4.dp))
            }
            Text(label, style = AeroTheme.typography.label, color = textColor)
        }
    }
}
