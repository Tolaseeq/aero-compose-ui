package com.mordred.aero.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassSurface

/**
 * Numeric spinner with increment/decrement buttons.
 *
 * The text field accepts digit input and optional leading minus. Values are clamped to
 * [min]..[max] via [coerceIn]. Buttons step up/down by [step] and clamp with [coerceAtMost]
 * / [coerceAtLeast].
 *
 * @param value Current integer value.
 * @param onValueChange Callback invoked with a new clamped value.
 * @param modifier Modifier applied to the outer Row.
 * @param enabled Whether the spinner is interactive.
 * @param min Minimum allowed value (inclusive).
 * @param max Maximum allowed value (inclusive).
 * @param step Step size for ▲▼ buttons.
 */
@Composable
public fun AeroNumberSpinner(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    step: Int = 1
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    // Hoist text state to allow intermediate editing (e.g. empty string or "-")
    var textState by remember(value) { mutableStateOf(value.toString()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .pointerInput(value, min, max, step, enabled) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll && enabled) {
                            val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (deltaY < 0f) {
                                onValueChange((value + step).coerceIn(min, max))
                            } else if (deltaY > 0f) {
                                onValueChange((value - step).coerceIn(min, max))
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        // Number text field
        BasicTextField(
            value = textState,
            onValueChange = { text ->
                // Accept intermediate states (empty or just minus sign)
                if (text.isEmpty() || text == "-") {
                    textState = text
                } else {
                    val parsed = text.toIntOrNull()
                    if (parsed != null) {
                        val clamped = parsed.coerceIn(min, max)
                        textState = clamped.toString()
                        onValueChange(clamped)
                    }
                }
            },
            enabled = enabled,
            singleLine = true,
            textStyle = AeroTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .width(60.dp)
                .height(28.dp)
                .background(colors.cardBackground, shape)
                .border(1.dp, colors.borderDefault, shape)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(Modifier.width(4.dp))

        // ▲ / ▼ buttons
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Up button
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 12.dp)
                    .glassSurface(cornerRadius = 2.dp)
                    .clickable(enabled = enabled) {
                        onValueChange((value + step).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▲", fontSize = 8.sp, color = colors.onSurface)
            }
            // Down button
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 12.dp)
                    .glassSurface(cornerRadius = 2.dp)
                    .clickable(enabled = enabled) {
                        onValueChange((value - step).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("▼", fontSize = 8.sp, color = colors.onSurface)
            }
        }
    }
}
