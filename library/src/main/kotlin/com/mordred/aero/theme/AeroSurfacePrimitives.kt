package com.mordred.aero.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Width of each concentric ring composing [aeroGlowRing]'s outer soft bloom. */
private val GLOW_RING_BLOOM_STROKE = 2.dp

/** Outward step between each successive bloom ring (Pitfall 7 — drawn outside bounds via
 * unclipped draw, never by growing the component's declared size). */
private val GLOW_RING_BLOOM_STEP = 2.5.dp

/** Number of concentric bloom rings approximating the glow's soft outward falloff. */
private const val GLOW_RING_BLOOM_LAYERS = 4

/** Alpha multiplier applied to [glowColor]'s own alpha for the innermost (closest-to-surface)
 * bloom ring. */
private const val GLOW_RING_BLOOM_BASE_ALPHA = 0.9f

/** Alpha multiplier applied between each successive bloom ring, so the ring nearest the surface
 * reads brightest and the outermost fades toward nothing. */
private const val GLOW_RING_BLOOM_FALLOFF = 0.55f

/**
 * Hover/focus glow ring (PRIM-06) — a double-stroke approximation (inner crisp stroke + an outer
 * soft bloom) standing in for a real Gaussian blur (16-RESEARCH.md Don't-Hand-Roll: zero extra
 * composited layer, zero Skia-type coupling risk). The outer bloom is itself a poor-man's blur:
 * [GLOW_RING_BLOOM_LAYERS] concentric solid-color rings, each stepping [GLOW_RING_BLOOM_STEP]
 * further outside the layout bounds than the last and fading by [GLOW_RING_BLOOM_FALLOFF] per
 * ring — never `Color.Transparent` (PRIM-14), and never a single radial gradient centered on the
 * box, whose bright stop lands in the card interior rather than at the actual perimeter stroke
 * location for anything but a perfect square.
 *
 * Gated on [active] — when `false` the draw block contributes nothing (no draw calls at all),
 * so an inactive glow ring costs a single boolean branch, never a `rememberInfiniteTransition`
 * or other always-on animation (T-16-03, PRIM-06 prohibition).
 *
 * Both the inner stroke and the bloom rings are drawn OUTSIDE this modifier's own layout bounds
 * via an unclipped `drawWithCache { onDrawBehind { ... } }` block (Compose's draw phase isn't
 * clipped to layout bounds by default) — never by padding/growing the declared size (Pitfall 7).
 * Callers that need the ring visually inset instead may simply pass a smaller
 * [cornerRadius]/size.
 *
 * @param glowColor Already-resolved color (e.g. `AeroOrnamentTokens.hoverGlow`) — this function
 * does not read [LocalAeroColors] itself, matching [aeroSurface]'s "caller resolves the theme,
 * primitive just draws" convention. [AeroOrnamentTokens.hoverGlow] is deliberately lightened well
 * past the surface fill/bevel tokens so this reads as a distinct luminous halo, not a shade of
 * the surface it wraps.
 * @param cornerRadius MUST match the same corner value used for the component's own clip shape
 * (16-RESEARCH.md Pitfall 5) — never an independently derived radius.
 */
public fun Modifier.aeroGlowRing(active: Boolean, glowColor: Color, cornerRadius: Dp = 8.dp): Modifier =
    this.drawWithCache {
        val cornerPx = cornerRadius.toPx()
        val innerStrokePx = GLOW_RING_INNER_STROKE.toPx()
        val bloomStrokePx = GLOW_RING_BLOOM_STROKE.toPx()
        val bloomStepPx = GLOW_RING_BLOOM_STEP.toPx()
        onDrawBehind {
            if (!active) return@onDrawBehind

            // Inner crisp stroke — hugs the shape's own bounds.
            drawRoundRect(
                color = glowColor,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = innerStrokePx),
            )

            // Outer soft bloom — concentric rings stepping outward beyond the layout bounds,
            // each fainter than the last. Solid per-ring color (not a gradient) guarantees
            // non-trivial brightness immediately outside the surface's own edge, regardless of
            // the box's aspect ratio.
            var ringAlpha = glowColor.alpha * GLOW_RING_BLOOM_BASE_ALPHA
            for (layer in 1..GLOW_RING_BLOOM_LAYERS) {
                // The first ring's inner edge sits AT the surface's own boundary (offset by only
                // its own half-width) so there is no dead gap between the surface edge and where
                // the bloom starts contributing visible brightness.
                val ringOffsetPx = (bloomStrokePx / 2f) + bloomStepPx * (layer - 1)
                drawRoundRect(
                    color = glowColor.copy(alpha = ringAlpha),
                    topLeft = Offset(-ringOffsetPx, -ringOffsetPx),
                    size = Size(size.width + ringOffsetPx * 2f, size.height + ringOffsetPx * 2f),
                    cornerRadius = CornerRadius(cornerPx + ringOffsetPx, cornerPx + ringOffsetPx),
                    style = Stroke(width = bloomStrokePx),
                )
                ringAlpha *= GLOW_RING_BLOOM_FALLOFF
            }
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

/** Fraction of [drawAeroGroove]'s height its darkened top-edge inner-shadow cue fades over. */
private const val GROOVE_SHADOW_CUE_FRACTION: Float = 0.4f

/** Alpha multiplier applied to `style.bevelShadow` at the inner-shadow cue's darkest point. */
private const val GROOVE_SHADOW_CUE_ALPHA: Float = 0.5f

/**
 * Recessed track-groove primitive (PRIM-08) — reuses [drawAeroSurfaceCore] with fill and bevel
 * direction swapped so the shared fill/gloss/bevel/rim geometry reads as carved-in rather than
 * raised, with gloss disabled (a recess doesn't catch a top highlight the way a raised surface
 * does) — no bespoke gradient/bevel implementation (PRIM-08 prohibition). Adds a darkened
 * inner-shadow cue hugging the top edge (`bevelShadow`-derived, per 16-RESEARCH.md FEATURES A10)
 * reinforcing the "light doesn't reach the bottom of the recess" read.
 *
 * Callable directly inside an existing Canvas [DrawScope] (matching [drawAeroSurfaceCore]'s dual
 * Modifier/direct-Canvas convention), for Canvas-owning consumers such as `AeroRangeSlider`-style
 * call sites, and indirectly via [Modifier.aeroGroove] for Box-owning consumers.
 */
internal fun DrawScope.drawAeroGroove(style: AeroSurfaceStyle, cornerPx: Float) {
    drawAeroSurfaceCore(
        style = style.copy(
            fillTop = style.fillBottom,
            fillBottom = style.fillTop,
            bevelLight = style.bevelShadow,
            bevelShadow = style.bevelLight,
            glossAlpha = 0f,
        ),
        cornerPx = cornerPx,
    )

    // Inner-shadow cue — fades to bevelShadow.copy(alpha = 0f) (PRIM-14: never the flat
    // fully-transparent color constant).
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                style.bevelShadow.copy(alpha = style.bevelShadow.alpha * GROOVE_SHADOW_CUE_ALPHA),
                style.bevelShadow.copy(alpha = 0f),
            ),
            startY = 0f,
            endY = size.height * GROOVE_SHADOW_CUE_FRACTION,
        ),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
    )
}

/**
 * Box-owning exposure of [drawAeroGroove], matching [aeroSurface]'s `.clip(shape)`-outermost /
 * `drawWithCache` conventions (PRIM-12/13). [style]'s own `cornerRadius` field drives both the
 * clip shape and the draw geometry — never two independently-derived radii (16-RESEARCH.md
 * Pitfall 5).
 */
public fun Modifier.aeroGroove(style: AeroSurfaceStyle): Modifier {
    val shape = RoundedCornerShape(style.cornerRadius)
    return this
        .clip(shape)
        .drawWithCache {
            val cornerPx = style.cornerRadius.toPx()
            onDrawBehind { drawAeroGroove(style, cornerPx) }
        }
}
