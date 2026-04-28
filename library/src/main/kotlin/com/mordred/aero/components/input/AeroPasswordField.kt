package com.mordred.aero.components.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Password input field with show/hide toggle.
 *
 * The border animates from [AeroTheme.colors.borderDefault] to [AeroTheme.colors.borderSelected]
 * over 150 ms with LinearEasing on focus. Clicking the trailing icon toggles between masked
 * and visible text.
 *
 * The toggle icon is a minimalist monochrome eye glyph drawn via Canvas, tinted with
 * [AeroTheme.colors.labelText] so it adapts to any theme. No colored emoji are used.
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
                    if (visible) {
                        EyeOpenIcon(tint = colors.labelText)
                    } else {
                        EyeClosedIcon(tint = colors.labelText)
                    }
                }
            }
        }
    )
}

/**
 * Minimalist open-eye icon: almond-shaped outline + small filled iris circle.
 * Monochrome, tinted via [tint].
 */
@Composable
private fun EyeOpenIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.4.dp.toPx()
        val cx = w / 2f
        val cy = h / 2f

        // Almond / lens shape drawn as two arcs forming an eye outline
        val path = Path().apply {
            moveTo(1.dp.toPx(), cy)
            cubicTo(
                cx - 1.dp.toPx(), cy - 4.dp.toPx(),
                cx + 1.dp.toPx(), cy - 4.dp.toPx(),
                w - 1.dp.toPx(), cy
            )
            cubicTo(
                cx + 1.dp.toPx(), cy + 4.dp.toPx(),
                cx - 1.dp.toPx(), cy + 4.dp.toPx(),
                1.dp.toPx(), cy
            )
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Iris dot
        drawCircle(color = tint, radius = 1.8.dp.toPx(), center = Offset(cx, cy))
    }
}

/**
 * Minimalist closed-eye (masked) icon: almond outline with a diagonal slash across it.
 * Monochrome, tinted via [tint].
 */
@Composable
private fun EyeClosedIcon(tint: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.4.dp.toPx()
        val cx = w / 2f
        val cy = h / 2f

        // Almond outline (same as open eye)
        val path = Path().apply {
            moveTo(1.dp.toPx(), cy)
            cubicTo(
                cx - 1.dp.toPx(), cy - 4.dp.toPx(),
                cx + 1.dp.toPx(), cy - 4.dp.toPx(),
                w - 1.dp.toPx(), cy
            )
            cubicTo(
                cx + 1.dp.toPx(), cy + 4.dp.toPx(),
                cx - 1.dp.toPx(), cy + 4.dp.toPx(),
                1.dp.toPx(), cy
            )
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Diagonal slash to indicate hidden/masked
        drawLine(
            color = tint,
            start = Offset(3.dp.toPx(), 3.dp.toPx()),
            end = Offset(w - 3.dp.toPx(), h - 3.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
