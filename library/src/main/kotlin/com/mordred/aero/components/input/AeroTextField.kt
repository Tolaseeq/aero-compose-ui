package com.mordred.aero.components.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Single-line text input with Aero-styled border and focus animation.
 *
 * The border animates from [AeroTheme.colors.borderDefault] to [AeroTheme.colors.borderSelected]
 * over 150 ms with LinearEasing on focus. Border width expands from 1 dp to 2 dp on focus.
 *
 * @param value Current text value.
 * @param onValueChange Callback invoked when text changes.
 * @param modifier Modifier applied to the overall component.
 * @param enabled Whether the field is interactive.
 * @param readOnly Whether the field is read-only (no editing).
 * @param placeholder Placeholder text shown when [value] is empty.
 * @param trailingIcon Optional composable slot placed at the trailing end of the field.
 * @param height Overall height of the field (default 28.dp).
 * @param interactionSource Interaction source for focus/hover state tracking.
 */
@Composable
public fun AeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String = "",
    trailingIcon: (@Composable () -> Unit)? = null,
    height: Dp = 28.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)
    val focused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (!enabled) colors.borderDefault.copy(alpha = 0.4f)
                      else if (focused) colors.borderSelected
                      else colors.borderDefault,
        animationSpec = tween(150, easing = LinearEasing),
        label = "border"
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (focused && enabled) 2f else 1f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "borderWidth"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = AeroTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier
            .height(height)
            .background(colors.cardBackground, shape)
            .border(borderWidth.dp, borderColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.4f),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.labelText.copy(alpha = 0.5f),
                            style = AeroTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Spacer(Modifier.width(4.dp))
                    trailingIcon()
                }
            }
        }
    )
}
