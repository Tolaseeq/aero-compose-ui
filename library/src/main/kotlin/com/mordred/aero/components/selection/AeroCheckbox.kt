package com.mordred.aero.components.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Check
import com.mordred.aero.icons.`internal`.Minus
import com.mordred.aero.theme.AeroTheme

/**
 * SEL-01: Binary checkbox backed by AeroTriStateCheckbox.
 */
@Composable
public fun AeroCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    AeroTriStateCheckbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = onCheckedChange?.let { cb -> { cb(!checked) } },
        modifier = modifier,
        enabled = enabled,
        label = label
    )
}

/**
 * SEL-01: Tri-state checkbox with animated background and border.
 * States: Off (unchecked), On (checked), Indeterminate (dash).
 */
@Composable
public fun AeroTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val colors = AeroTheme.colors

    val bgColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.On -> colors.primary
            ToggleableState.Indeterminate -> colors.primary.copy(alpha = 0.6f)
            ToggleableState.Off -> colors.cardBackground
        },
        animationSpec = tween(150, easing = LinearEasing),
        label = "checkboxBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.Off -> colors.borderDefault
            else -> colors.primary
        },
        animationSpec = tween(150, easing = LinearEasing),
        label = "checkboxBorder"
    )

    val shape = RoundedCornerShape(2.dp)

    val checkboxContent = @Composable {
        Box(
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.4f)
                .size(16.dp)
                .background(bgColor, shape)
                .border(1.dp, borderColor, shape)
                .triStateToggleable(
                    state = state,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = { onClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                ToggleableState.On -> Icon(
                    imageVector = AeroIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.onPrimary
                )
                ToggleableState.Indeterminate -> Icon(
                    imageVector = AeroIcons.Minus,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colors.onPrimary
                )
                ToggleableState.Off -> Box(Modifier)
            }
        }
    }

    if (label != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            checkboxContent()
            Text(label, color = colors.onSurface, style = AeroTheme.typography.bodyLarge)
        }
    } else {
        Box(modifier = modifier) {
            checkboxContent()
        }
    }
}
