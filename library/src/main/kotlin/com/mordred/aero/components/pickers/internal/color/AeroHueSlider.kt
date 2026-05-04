package com.mordred.aero.components.pickers.internal.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Internal Canvas composable: vertical hue strip for AeroColorPicker (Phase 8).
 *
 * **Hue convention:** `hue` is DEGREES `[0f, 360f]`. The strip is laid out top->bottom = 0deg->360deg.
 *
 * **PITFALL-03 mitigation:** drag uses `awaitPointerEventScope` + manual loop. First mouse-down
 * fires `onHueChange` immediately; every drag move fires `onHueChange` with the new hue.
 *
 * **Visual:** 24dp x 256dp Canvas, 7-stop vertical gradient (Red -> Yellow -> Green -> Cyan -> Blue
 * -> Magenta -> Red). 2dp horizontal indicator line at `y = (hue / 360f) * size.height`, drawn in
 * `colors.borderSelected` for contrast across all three themes.
 */
@Composable
internal fun AeroHueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
            ),
        )
    }
    Canvas(
        modifier = modifier
            .size(width = 24.dp, height = 256.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val height = size.height.toFloat()
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Click-to-set: fire on first mouse-down.
                        val initHue = ((down.position.y / height) * 360f).coerceIn(0f, 360f)
                        onHueChange(initHue)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val h = ((change.position.y / height) * 360f).coerceIn(0f, 360f)
                            onHueChange(h)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        drawRect(brush = gradient)
        // Indicator: 2dp horizontal line at the current hue position.
        val y = (hue.coerceIn(0f, 360f) / 360f) * size.height
        drawLine(
            color = colors.borderSelected,
            start = Offset(0f, y),
            end   = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
