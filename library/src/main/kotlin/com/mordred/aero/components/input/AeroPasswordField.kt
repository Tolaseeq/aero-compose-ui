package com.mordred.aero.components.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Eye
import com.mordred.aero.icons.`internal`.EyeSlash
import com.mordred.aero.theme.AeroTheme

/**
 * Password input field with show/hide toggle.
 *
 * The border animates from [AeroTheme.colors.borderDefault] to [AeroTheme.colors.borderSelected]
 * over 150 ms with LinearEasing on focus. Clicking the trailing icon toggles between masked
 * and visible text.
 *
 * The toggle icon is rendered via [Icon] using [AeroIcons.Eye] (masked state) or
 * [AeroIcons.EyeSlash] (visible state), tinted with [AeroTheme.colors.labelText].
 *
 * @param value Current password value.
 * @param onValueChange Callback invoked when text changes.
 * @param modifier Modifier applied to the overall component.
 * @param enabled Whether the field is interactive.
 * @param placeholder Placeholder text shown when [value] is empty.
 * @param interactionSource Interaction source for focus/hover state tracking.
 */
@Composable
public fun AeroPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Password",
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)
    val focused by interactionSource.collectIsFocusedAsState()
    var visible by remember { mutableStateOf(false) }

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
        singleLine = true,
        interactionSource = interactionSource,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = AeroTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier
            .height(28.dp)
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
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = enabled) { visible = !visible },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (visible) AeroIcons.EyeSlash else AeroIcons.Eye,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        modifier = Modifier.size(14.dp),
                        tint = colors.labelText
                    )
                }
            }
        }
    )
}
