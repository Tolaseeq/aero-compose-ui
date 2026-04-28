package com.mordred.aero.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * SEL-05: Segmented control enforcing exactly one selected option.
 *
 * Outer row with borderDefault border and clip. Each option animates
 * its background between primary.copy(0.3f) (selected) and Transparent (unselected).
 * Text animates between primary (selected) and onSurface (unselected).
 * Dividers (1dp) are drawn between options.
 */
@Composable
public fun <T> AeroSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() }
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(28.dp)
            .border(1.dp, colors.borderDefault, shape)
            .clip(shape)
    ) {
        options.forEachIndexed { index, opt ->
            val isSelected = (opt == selected)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary.copy(alpha = 0.3f) else Color.Transparent,
                animationSpec = tween(150, easing = LinearEasing),
                label = "segBg_$index"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.onSurface,
                animationSpec = tween(150, easing = LinearEasing),
                label = "segText_$index"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(bgColor)
                    .clickable(enabled = enabled) { onSelect(opt) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel(opt),
                    color = textColor,
                    style = AeroTheme.typography.bodyLarge
                )
            }

            // Draw separator between options (skip after the last)
            if (index < options.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(colors.borderDefault.copy(alpha = 0.5f))
                )
            }
        }
    }
}
