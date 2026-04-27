package com.mordred.aero.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Card / element-level glass surface. Renders shadow then gradient fill + border in a
 * single `drawBehind` block (no overdraw, FOUND-07). Reads colors from [LocalAeroColors] —
 * MUST be invoked inside `AeroTheme {}`.
 *
 * @param cornerRadius corner radius of the rounded rect (default 8.dp).
 * @param elevation shadow depth (default 4.dp).
 */
@Composable
public fun Modifier.glassEffect(
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp
): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val glassSurface = colors.glassSurface
    val glassBorder = colors.glassBorder
    return this
        .shadow(elevation, shape)
        .drawBehind {
            val cornerPx = cornerRadius.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)
            // Single-pass: gradient fill + border stroke, both inside drawBehind.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassSurface,
                        glassSurface.copy(alpha = glassSurface.alpha * 0.5f)
                    )
                ),
                cornerRadius = cr
            )
            drawRoundRect(
                color = glassBorder,
                cornerRadius = cr,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)
}

/**
 * Section-level background. Renders gradient fill only — no shadow, no border.
 * Reads [LocalAeroColors] — MUST be invoked inside `AeroTheme {}`.
 *
 * @param cornerRadius corner radius (default 0.dp — typical for full-width panels).
 */
@Composable
public fun Modifier.glassPanel(
    cornerRadius: Dp = 0.dp
): Modifier {
    val colors = LocalAeroColors.current
    val panelBackground = colors.panelBackground
    return this.drawBehind {
        val cornerPx = cornerRadius.toPx()
        val cr = CornerRadius(cornerPx, cornerPx)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    panelBackground,
                    panelBackground.copy(alpha = panelBackground.alpha * 0.8f)
                )
            ),
            cornerRadius = cr
        )
    }
}

/**
 * Mid-level surface. Renders highlight gradient + border in a single drawBehind pass.
 * Reads [LocalAeroColors] — MUST be invoked inside `AeroTheme {}`.
 */
@Composable
public fun Modifier.glassSurface(
    cornerRadius: Dp = 8.dp
): Modifier {
    val colors = LocalAeroColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val glassHighlight = colors.glassHighlight
    val glassBorder = colors.glassBorder
    return this
        .drawBehind {
            val cornerPx = cornerRadius.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(glassHighlight, Color.Transparent),
                    startY = 0f,
                    endY = 100f
                ),
                cornerRadius = cr
            )
            drawRoundRect(
                color = glassBorder,
                cornerRadius = cr,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)
}
