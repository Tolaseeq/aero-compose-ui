package com.mordred.aero.components.pickers.internal.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Internal Canvas composable: 2D saturation/value picker for AeroColorPicker (Phase 8).
 *
 * **Hue convention:** `hue` is in DEGREES `[0f, 360f]` (Plan-01 ADR; matches `Color.hsv(...)`).
 *
 * **PITFALL-03 mitigation:** drag uses `awaitPointerEventScope` + manual loop. The
 * Compose Desktop `detectDragGestures` API is BANNED for in-content Canvas drag because
 * its 18dp touchSlop silently breaks pixel-precise pickers. First mouse-down fires
 * `onSatValChange` immediately (click-to-set UX); every subsequent drag move fires
 * `onSatValChange` with the new s/v.
 *
 * **Visual:** 256x256 logical Canvas. Horizontal axis = saturation `[0f, 1f]`, vertical = value
 * (top = 1f, bottom = 0f). Background = `Brush.horizontalGradient(white -> Color.hsv(hue, 1f, 1f))`
 * with a `Brush.verticalGradient(transparent -> black)` overlay. Indicator = 8dp circle at the
 * (sat, val) position with a white outer stroke and a thin black inner stroke for contrast on
 * any background.
 *
 * **No glass styling here** — AeroColorPicker (Phase 8) wraps this in its glass panel; per-element
 * glass would muddy the saturation gradient.
 */
@Composable
internal fun AeroHsvColorSquare(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pureHueColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)

    Canvas(
        modifier = modifier
            .size(width = 256.dp, height = 256.dp)
            .pointerInput(hue) {
                awaitPointerEventScope {
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Click-to-set: fire on first mouse-down, no drag wait.
                        val initS = (down.position.x / width).coerceIn(0f, 1f)
                        val initV = (1f - down.position.y / height).coerceIn(0f, 1f)
                        onSatValChange(initS, initV)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val s = (change.position.x / width).coerceIn(0f, 1f)
                            val v = (1f - change.position.y / height).coerceIn(0f, 1f)
                            onSatValChange(s, v)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        // Saturation gradient: white -> pure hue.
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, pureHueColor),
            ),
        )
        // Value overlay: transparent -> black (top to bottom).
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
            ),
        )
        // Indicator: 8dp circle at (s * width, (1 - v) * height); double-stroke for contrast.
        val cx = saturation.coerceIn(0f, 1f) * size.width
        val cy = (1f - value.coerceIn(0f, 1f)) * size.height
        val radius = 8.dp.toPx()
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.Black,
            radius = radius - 2.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
