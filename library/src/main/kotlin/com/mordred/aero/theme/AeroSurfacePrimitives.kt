package com.mordred.aero.theme

import androidx.compose.foundation.shape.CircleShape
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

/** Width of [aeroGlowRing]'s inner, crisp stroke. */
private val GLOW_RING_INNER_STROKE = 1.5.dp

/** Width of [aeroGlowRing]'s outer, wider, fainter stroke. */
private val GLOW_RING_OUTER_STROKE = 4.dp

/** How far [aeroGlowRing]'s outer stroke sits outside the layout bounds (Pitfall 7 — drawn
 * outside bounds via unclipped draw, never by growing the component's declared size). */
private val GLOW_RING_OUTSET = 3.dp

/**
 * Hover/focus glow ring (PRIM-06) — a double-stroke approximation (inner crisp stroke + outer
 * wider, fainter stroke fading to `glowColor.copy(alpha = 0f)`, never `Color.Transparent`,
 * PRIM-14) standing in for a real Gaussian blur (16-RESEARCH.md Don't-Hand-Roll: zero extra
 * composited layer, zero Skia-type coupling risk).
 *
 * Gated on [active] — when `false` the draw block contributes nothing (no draw calls at all),
 * so an inactive glow ring costs a single boolean branch, never a `rememberInfiniteTransition`
 * or other always-on animation (T-16-03, PRIM-06 prohibition).
 *
 * The outer stroke is drawn OUTSIDE this modifier's own layout bounds via an unclipped
 * `drawWithCache { onDrawBehind { ... } }` block (Compose's draw phase isn't clipped to layout
 * bounds by default) — never by padding/growing the declared size (Pitfall 7). Callers that
 * need the ring visually inset instead may simply pass a smaller [cornerRadius]/size.
 *
 * @param glowColor Already-resolved color (e.g. `AeroOrnamentTokens.hoverGlow`) — this function
 * does not read [LocalAeroColors] itself, matching [aeroSurface]'s "caller resolves the theme,
 * primitive just draws" convention.
 * @param cornerRadius MUST match the same corner value used for the component's own clip shape
 * (16-RESEARCH.md Pitfall 5) — never an independently derived radius.
 */
public fun Modifier.aeroGlowRing(active: Boolean, glowColor: Color, cornerRadius: Dp = 8.dp): Modifier =
    this.drawWithCache {
        val cornerPx = cornerRadius.toPx()
        val innerStrokePx = GLOW_RING_INNER_STROKE.toPx()
        val outerStrokePx = GLOW_RING_OUTER_STROKE.toPx()
        val outsetPx = GLOW_RING_OUTSET.toPx()
        onDrawBehind {
            if (!active) return@onDrawBehind

            // Inner crisp stroke — hugs the shape's own bounds.
            drawRoundRect(
                color = glowColor,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = innerStrokePx),
            )

            // Outer wider, fainter stroke — offset outward beyond the layout bounds and faded
            // via a radial brush toward glowColor.copy(alpha = 0f) (PRIM-14).
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowColor.alpha * 0.6f),
                        glowColor.copy(alpha = 0f),
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = (maxOf(size.width, size.height) / 2f) + outsetPx + outerStrokePx,
                ),
                topLeft = Offset(-outsetPx, -outsetPx),
                size = Size(size.width + outsetPx * 2f, size.height + outsetPx * 2f),
                cornerRadius = CornerRadius(cornerPx + outsetPx, cornerPx + outsetPx),
                style = Stroke(width = outerStrokePx),
            )
        }
    }

/**
 * Raised-thumb primitive (PRIM-07) — the circle-shape special case of [drawAeroSurfaceCore].
 * Reuses the exact same fill/gloss/bevel geometry by passing [radiusPx] as the corner radius
 * of a square draw area: when the receiver's `size` is `2*radiusPx` square, every corner
 * radius collapsing to half the side length degenerates the rounded-rect fill/gloss/bevel/rim
 * into a circle — no second gradient implementation (PRIM-07 prohibition).
 */
internal fun DrawScope.drawAeroThumb(style: AeroSurfaceStyle, radiusPx: Float) {
    drawAeroSurfaceCore(style, cornerPx = radiusPx)
}

/**
 * Box-owning exposure of [drawAeroThumb] — clips to a circle sized by the smaller of the box's
 * width/height, matching [aeroSurface]'s `.clip(shape)`-outermost / `drawWithCache` conventions
 * (PRIM-12/13). Native [style]-declared `dropShadow`/`innerShadow` follow the same drop-before-
 * fill, inner-after ordering as [aeroSurface].
 */
public fun Modifier.aeroThumbSurface(style: AeroSurfaceStyle): Modifier = this
    .let { m -> style.dropShadow?.let { m.dropShadow(shape = CircleShape, shadow = it) } ?: m }
    .clip(CircleShape)
    .drawWithCache {
        val radiusPx = minOf(size.width, size.height) / 2f
        onDrawBehind { drawAeroThumb(style, radiusPx) }
    }
    .let { m -> style.innerShadow?.let { m.innerShadow(shape = CircleShape, shadow = it) } ?: m }

// --- 16-02 Task 2 RED-phase stub (intentionally incomplete/wrong; replaced in the GREEN commit) ---

/** RED stub — TODO(16-02 Task 2 GREEN): must reuse [drawAeroSurfaceCore], not a fresh gradient. */
internal fun DrawScope.drawAeroGroove(style: AeroSurfaceStyle, cornerPx: Float) {
    drawRoundRect(color = style.bevelShadow, cornerRadius = CornerRadius(cornerPx, cornerPx))
}

/** RED stub — TODO(16-02 Task 2 GREEN): Box-owning exposure of [drawAeroGroove]. */
public fun Modifier.aeroGroove(style: AeroSurfaceStyle): Modifier = this
