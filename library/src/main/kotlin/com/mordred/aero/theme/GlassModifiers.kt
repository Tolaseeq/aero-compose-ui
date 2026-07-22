package com.mordred.aero.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Fraction of [Modifier.glassSurface]'s height the gloss highlight fades over (PRIM-09). */
private const val GLASS_SURFACE_GLOSS_FRACTION: Float = 0.32f

/** Fraction of [Modifier.glassPanel]'s height the gloss highlight fades over. */
private const val GLASS_PANEL_GLOSS_FRACTION: Float = 0.55f

/**
 * Card / element-level glass surface. Renders shadow + gradient fill + border.
 * Reads colors from [LocalAeroColors] — MUST be invoked inside `AeroTheme {}`.
 *
 * @param cornerRadius corner radius of the rounded rect.
 * @param elevation shadow depth — wired to a real [dropShadow] (PRIM-11: this parameter used
 * to be accepted but never applied; it now draws a genuine drop shadow behind the fill).
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
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = elevation,
                color = Color.Black.copy(alpha = 0.25f)
            )
        )
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    glassSurface,
                    glassSurface.copy(alpha = glassSurface.alpha * 0.5f)
                )
            ),
            shape = shape
        )
        .border(width = 1.dp, color = glassBorder, shape = shape)
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
    val glassHighlight = colors.glassHighlight
    return this.drawWithCache {
        val cornerPx = cornerRadius.toPx()
        val cr = CornerRadius(cornerPx, cornerPx)
        val glossBrush = Brush.verticalGradient(
            colors = listOf(glassHighlight, Color.Transparent),
            startY = 0f,
            endY = size.height * GLASS_PANEL_GLOSS_FRACTION
        )
        onDrawBehind {
            drawRoundRect(color = panelBackground, cornerRadius = cr)
            drawRoundRect(brush = glossBrush, cornerRadius = cr)
        }
    }
}

/**
 * Mid-level surface. Renders a proportional highlight gloss + a full-thickness border in a
 * single cached draw pass. Reads [LocalAeroColors] — MUST be invoked inside `AeroTheme {}`.
 *
 * Two defects fixed here:
 *  - PRIM-09: the gloss gradient's `endY` stop is `size.height * `[GLASS_SURFACE_GLOSS_FRACTION]
 *    (proportional), not a fixed pixel literal — it now completes on short controls (e.g. an
 *    18.dp `AeroSwitch` track) instead of banding at a fixed pixel distance.
 *  - PRIM-10/12: `.clip(shape)` is applied first (the outermost paint-affecting modifier) so
 *    the border stroke is never clipped away; the stroke is additionally inset by half its
 *    width as a belt-and-suspenders guard so its full declared 1.dp thickness renders
 *    regardless of clip ordering.
 *
 * Gloss/border geometry is built once in [drawWithCache] rather than reallocated every frame
 * inside `drawBehind` (PRIM-13 discipline).
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
        .clip(shape)
        .drawWithCache {
            val cornerPx = cornerRadius.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)
            val strokeWidthPx = 1.dp.toPx()
            val strokeInset = strokeWidthPx / 2f
            val borderCornerPx = (cornerPx - strokeInset).coerceAtLeast(0f)
            val borderTopLeft = Offset(strokeInset, strokeInset)
            val borderSize = Size(
                width = (size.width - strokeWidthPx).coerceAtLeast(0f),
                height = (size.height - strokeWidthPx).coerceAtLeast(0f)
            )
            val glossBrush = Brush.verticalGradient(
                colors = listOf(glassHighlight, Color.Transparent),
                startY = 0f,
                endY = size.height * GLASS_SURFACE_GLOSS_FRACTION
            )
            onDrawBehind {
                drawRoundRect(brush = glossBrush, cornerRadius = cr)
                drawRoundRect(
                    color = glassBorder,
                    topLeft = borderTopLeft,
                    size = borderSize,
                    cornerRadius = CornerRadius(borderCornerPx, borderCornerPx),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
}
