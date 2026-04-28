package com.mordred.aero.components.range

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassEffect

/**
 * Aero-styled slider wrapping [Slider] from Material3.
 *
 * When the user drags the thumb and [showTooltip] is true, a glass tooltip appears
 * above the slider centre showing the current [value] formatted to 2 decimals.
 *
 * TODO(Phase 3): track tooltip x to thumb position — currently centred above the track.
 *
 * @param value Current value within [valueRange].
 * @param onValueChange Emits new Float values while dragging.
 * @param modifier Layout modifier.
 * @param enabled Whether interactions are enabled (disabled at 40% alpha via SliderColors).
 * @param valueRange The permitted range of [value]. Default 0f..1f.
 * @param steps Number of discrete steps (0 = continuous).
 * @param showTooltip Show a glass pill above the slider while dragging.
 */
@Composable
public fun AeroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true
) {
    val colors = AeroTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.primary,
        activeTrackColor = colors.primary,
        inactiveTrackColor = colors.borderDefault,
        disabledThumbColor = colors.primary.copy(alpha = 0.4f),
        disabledActiveTrackColor = colors.primary.copy(alpha = 0.4f),
        disabledInactiveTrackColor = colors.borderDefault.copy(alpha = 0.4f)
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (isDragged && showTooltip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .glassEffect(cornerRadius = 4.dp, elevation = 2.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "%.2f".format(value),
                    color = colors.onSurface,
                    style = AeroTheme.typography.label
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            colors = sliderColors,
            interactionSource = interactionSource
        )
    }
}
