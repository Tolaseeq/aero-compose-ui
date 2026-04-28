package com.mordred.aero.components.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Search input field with a leading search icon and a conditional clear button.
 *
 * The search icon is a minimalist monochrome magnifier drawn via Canvas, tinted with
 * [AeroTheme.colors.labelText]. The trailing clear button (x) appears only when [value] is not
 * empty.
 *
 * @param value Current search text.
 * @param onValueChange Callback invoked when text changes (also called with "" on clear).
 * @param modifier Modifier applied to the outer Row.
 * @param enabled Whether the field is interactive.
 * @param placeholder Placeholder text shown when [value] is empty.
 * @param onSearch Optional callback invoked with the current value (e.g. on Enter).
 */
@Composable
public fun AeroSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Search...",
    onSearch: (String) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
    ) {
        val iconShape = RoundedCornerShape(
            topStart = 4.dp, bottomStart = 4.dp,
            topEnd = 0.dp, bottomEnd = 0.dp
        )
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 28.dp)
                .background(AeroTheme.colors.cardBackground, iconShape)
                .border(1.dp, AeroTheme.colors.borderDefault, iconShape),
            contentAlignment = Alignment.Center
        ) {
            SearchIcon()
        }
        AeroTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = placeholder,
            trailingIcon = if (value.isNotEmpty()) {
                { ClearButton(enabled = enabled, onClick = { onValueChange("") }) }
            } else null
        )
    }
}

/**
 * Minimalist magnifier icon drawn with Canvas — circle outline + diagonal handle.
 * Tinted via [AeroTheme.colors.labelText] so it adapts to any theme.
 */
@Composable
private fun SearchIcon() {
    val iconColor = AeroTheme.colors.labelText
    Canvas(modifier = Modifier.size(14.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val circleRadius = 4.dp.toPx()
        val circleCenterX = 4.5.dp.toPx()
        val circleCenterY = 4.5.dp.toPx()

        // Circle (lens)
        drawCircle(
            color = iconColor,
            radius = circleRadius,
            center = Offset(circleCenterX, circleCenterY),
            style = Stroke(width = strokeWidth)
        )

        // Handle (diagonal line from bottom-right of circle outward)
        val handleStart = Offset(
            circleCenterX + circleRadius * 0.7f,
            circleCenterY + circleRadius * 0.7f
        )
        val handleEnd = Offset(12.dp.toPx(), 12.dp.toPx())
        drawLine(
            color = iconColor,
            start = handleStart,
            end = handleEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ClearButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("x", color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyLarge)
    }
}
