package com.mordred.aero.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single implementation authoring an Aero "glass" surface: two-tone fill with a soft seam,
 * a proportional top gloss band, an inset inner bevel, and an inset outer contour rim
 * (PRIM-05) — the ONLY place surface gradient/bevel/rim geometry is authored. Callable
 * directly inside an existing Canvas [DrawScope] for Canvas-owning consumers (e.g.
 * `AeroRangeSlider`, wired in a later plan), and indirectly via [Modifier.aeroSurface] for
 * Box-owning consumers.
 *
 * All gradient fades target `baseColor.copy(alpha = 0f)` (PRIM-14) — never a hardcoded
 * `Color.Transparent` — so both [AeroColorScheme.Classic]'s opaque tokens and
 * [AeroColorScheme.AeroBlue]/[AeroColorScheme.AeroDark]'s translucent tokens degrade to
 * "less of themselves," never a flat color block. Every gradient stop is expressed as
 * `size.height * fraction` / `size.width * fraction` (PRIM-09), never a bare pixel literal.
 *
 * @param cornerPx MUST be derived from the same corner value used for the component's own
 * clip [Shape] — never an independently `.toPx()`'d value (16-RESEARCH.md Pitfall 5).
 */
internal fun DrawScope.drawAeroSurfaceCore(style: AeroSurfaceStyle, cornerPx: Float) {
    val cr = CornerRadius(cornerPx, cornerPx)

    // 1. Two-tone fill with a SOFT seam (D-01: never a hard mid-surface break) — a 3-stop
    //    proportional gradient blending through the seam rather than a flat cut.
    val seamColor = lerp(style.fillTop, style.fillBottom, 0.5f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to style.fillTop,
                style.seamFraction.coerceIn(0f, 1f) to seamColor,
                1f to style.fillBottom,
            ),
            startY = 0f,
            endY = size.height,
        ),
        cornerRadius = cr,
    )

    // 2. Gloss band near the top, ~30-35% of height (D-01), fading to fully-transparent
    //    glossColor (PRIM-14: baseColor.copy(alpha = 0f), never Color.Transparent).
    if (style.glossAlpha > 0f && style.glossHeightFraction > 0f) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    style.glossColor.copy(alpha = style.glossAlpha),
                    style.glossColor.copy(alpha = 0f),
                ),
                startY = 0f,
                endY = size.height * style.glossHeightFraction,
            ),
            cornerRadius = cr,
        )
    }

    // 3. Inner bevel — light-to-shadow vertical stroke, inset by half its width so it
    //    survives the outer .clip(shape) (PRIM-10 fix pattern applied to every inset stroke).
    val bevelWidth = 1.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(style.bevelLight, style.bevelShadow),
            startY = 0f,
            endY = size.height,
        ),
        topLeft = Offset(bevelWidth / 2f, bevelWidth / 2f),
        size = Size(size.width - bevelWidth, size.height - bevelWidth),
        cornerRadius = cr,
        style = Stroke(width = bevelWidth),
    )

    // 4. Outer contour rim — solid color, inset by half its width (PRIM-10).
    val rimWidth = 1.dp.toPx()
    drawRoundRect(
        color = style.rimColor.copy(alpha = style.rimAlpha),
        topLeft = Offset(rimWidth / 2f, rimWidth / 2f),
        size = Size(size.width - rimWidth, size.height - rimWidth),
        cornerRadius = cr,
        style = Stroke(width = rimWidth),
    )
}

/**
 * Box-owning exposure of [drawAeroSurfaceCore]. `.clip(shape)` is the OUTERMOST
 * paint-affecting modifier in this chain (PRIM-12) — no per-call-site clip re-derivation.
 * Geometry/brushes are constructed from inside [androidx.compose.ui.draw.drawWithCache]'s
 * cached scope, never a bare `.drawBehind {}` (PRIM-13). Native [style]-declared
 * `dropShadow`/`innerShadow` are ordered per the proven convention: drop before fill, inner
 * after (`ScratchAeroShadowProof.kt`).
 */
public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier = this
    .let { m -> style.dropShadow?.let { m.dropShadow(shape = shape, shadow = it) } ?: m }
    .clip(shape)
    .drawWithCache {
        val cornerPx = style.cornerRadius.toPx()
        onDrawBehind { drawAeroSurfaceCore(style, cornerPx) }
    }
    .let { m -> style.innerShadow?.let { m.innerShadow(shape = shape, shadow = it) } ?: m }

// --- 16-02 Task 1 RED-phase stubs (intentionally incomplete/wrong; replaced in the GREEN commit) ---

/** RED stub — TODO(16-02 GREEN): gate on [active], double-stroke fade, drawWithCache geometry. */
public fun Modifier.aeroGlowRing(active: Boolean, glowColor: Color, cornerRadius: Dp = 8.dp): Modifier = this

/** RED stub — TODO(16-02 GREEN): must reuse [drawAeroSurfaceCore], not a fresh gradient. */
internal fun DrawScope.drawAeroThumb(style: AeroSurfaceStyle, radiusPx: Float) {
    drawCircle(color = style.fillTop, radius = radiusPx)
}

/** RED stub — TODO(16-02 GREEN): Box-owning circular exposure of [drawAeroThumb]. */
public fun Modifier.aeroThumbSurface(style: AeroSurfaceStyle): Modifier = this
