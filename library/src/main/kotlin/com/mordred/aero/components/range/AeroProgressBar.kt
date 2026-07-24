package com.mordred.aero.components.range

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGroove
import com.mordred.aero.theme.aeroSurface

/**
 * Fraction of the running-sheen highlight's own width its leading/trailing edges are already
 * transparent by construction (D-05, VRNG-07) — a plain 3-stop `Brush.horizontalGradient`
 * fading `glossColor` in and back out, distinct from the indeterminate segment's separate
 * edge-mask overlay below (this one fades ITS OWN body, the indeterminate mask fades the
 * already-opaque accent fill).
 */
private const val RUNNING_SHEEN_WIDTH_FRACTION = 0.25f

/** Peak alpha of the running-sheen highlight at its own center (D-05 "traveling highlight"). */
private const val RUNNING_SHEEN_ALPHA = 0.35f

/** Restart duration of the optional running-sheen sweep (Claude's discretion, D-05/VRNG-07) —
 * deliberately distinct from the indeterminate's locked 1500ms so the two never read as the same
 * animation when both happen to be visible in a showcase side-by-side. */
private const val RUNNING_SHEEN_DURATION_MS = 1800

/**
 * Fraction of the indeterminate sweep segment's own width its leading/trailing edges fade over
 * (D-06) — applied as a `BlendMode.DstIn` alpha mask ON TOP of the already-painted accent
 * `aeroSurface` fill, since `drawAeroSurfaceCore`'s own gradient is vertical-only (16-RESEARCH.md
 * Assumption A4). Fades to `baseColor.copy(alpha = 0f)`, never `Color.Transparent` (PRIM-14).
 */
private const val INDETERMINATE_EDGE_FADE_FRACTION = 0.35f

/**
 * Determinate progress bar. Renders a filled track showing [progress] (0f..1f) over a recessed
 * neutral [aeroGroove] bed, with an accent two-tone-glass [aeroSurface] fill carrying a static
 * top gloss ON by default (VRNG-06, D-05).
 *
 * @param progress Current progress clamped to 0f..1f.
 * @param modifier Layout modifier.
 * @param showPercent When true a percentage label is shown below/end of the bar.
 * @param height Bar thickness.
 * @param showRunningSheen When true, an additional traveling-highlight overlay sweeps across the
 * fill ON TOP of the always-on static gloss (D-05, VRNG-07). Defaults to `false` — the static
 * gloss alone remains the default determinate look.
 */
@Composable
public fun AeroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    height: Dp = 8.dp,
    showRunningSheen: Boolean = false
) {
    val colors = AeroTheme.colors
    val clamped = progress.coerceIn(0f, 1f)
    // Pill radius — MUST feed both the clip Shape and every AeroSurfaceStyle's cornerRadius
    // field below, never a second independently-derived radius (16-RESEARCH.md Pitfall 5).
    val cornerPx = height / 2

    Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .aeroGroove(AeroSurfaceStyle.neutralRest(colors, cornerPx))
        ) {
            val fillStyle = AeroSurfaceStyle.rest(colors, cornerPx)
            Box(
                Modifier
                    .fillMaxWidth(clamped)
                    .height(height)
                    .aeroSurface(fillStyle, RoundedCornerShape(cornerPx))
            ) {
                if (showRunningSheen) {
                    RunningSheenOverlay(style = fillStyle, cornerPx = cornerPx)
                }
            }
        }
        if (showPercent) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "${(clamped * 100).toInt()}%",
                    color = colors.labelText,
                    style = AeroTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Optional traveling-highlight overlay (D-05, VRNG-07) drawn ON TOP of the fill's already-painted
 * static gloss — never a replacement of it. The highlight `Brush` is built once inside
 * [drawWithCache]'s cached scope (keyed on size only); the animated [sweep] value is read purely
 * to compute an outer [Modifier.offset], mirroring the indeterminate sweep's own offset-driven
 * motion below so no per-frame Brush rebuild happens on the animated path (PRIM-13/Pitfall 6).
 */
@Composable
private fun BoxScope.RunningSheenOverlay(style: AeroSurfaceStyle, cornerPx: Dp) {
    val transition = rememberInfiniteTransition(label = "runningSheen")
    val sweep by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(RUNNING_SHEEN_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runningSheenX"
    )
    BoxWithConstraints(
        Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(cornerPx))
    ) {
        val offsetX = with(LocalDensity.current) { (sweep * maxWidth.toPx()).toDp() }
        Box(
            Modifier
                .fillMaxWidth(RUNNING_SHEEN_WIDTH_FRACTION)
                .fillMaxHeight()
                .offset(x = offsetX)
                .drawWithCache {
                    val brush = Brush.horizontalGradient(
                        colors = listOf(
                            style.glossColor.copy(alpha = 0f),
                            style.glossColor.copy(alpha = RUNNING_SHEEN_ALPHA),
                            style.glossColor.copy(alpha = 0f),
                        ),
                    )
                    onDrawBehind { drawRect(brush = brush) }
                }
        )
    }
}

/**
 * Indeterminate progress bar. Restyled to a single accent-glass segment (same fill treatment as
 * the determinate fill) sweeping left-to-right over a recessed neutral [aeroGroove] bed — the
 * canonical loading-state representation for this component family (VRNG-08). Keeps the EXACT
 * existing `1500 ms` [RepeatMode.Restart] timing — `RepeatMode.Reverse`/ping-pong is banned.
 *
 * @param modifier Layout modifier.
 * @param height Bar thickness.
 */
@Composable
public fun AeroProgressBar(
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val colors = AeroTheme.colors
    val cornerPx = height / 2

    val transition = rememberInfiniteTransition(label = "indeterminate")
    val shimmer by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val sweepStyle = AeroSurfaceStyle.rest(colors, cornerPx)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .aeroGroove(AeroSurfaceStyle.neutralRest(colors, cornerPx))
    ) {
        val offsetX = with(LocalDensity.current) { (shimmer * maxWidth.toPx()).toDp() }
        Box(
            Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .offset(x = offsetX)
                .aeroSurface(sweepStyle, RoundedCornerShape(cornerPx))
                // Isolates this segment onto its own offscreen layer (WR-02) before the DstIn
                // mask below runs — Modifier.clip alone (inside aeroSurface) doesn't guarantee
                // an isolated compositing layer under CompositingStrategy.Auto, so without this
                // the mask's "Dst" read can include the parent groove already painted underneath
                // on translucent ("glass") themes, fading the groove's contribution too.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    // Horizontal alpha-mask overlay (D-06) — drawAeroSurfaceCore's own gradient
                    // is vertical only, so the segment's leading/trailing edges need a SECOND
                    // Brush.horizontalGradient fading to baseColor.copy(alpha = 0f) (never
                    // Color.Transparent, PRIM-14), applied via BlendMode.DstIn so only the
                    // already-painted fill's alpha is masked, not its color. Built once here,
                    // independent of the animated `shimmer` value driving the outer offset above
                    // (PRIM-13/Pitfall 6 — no per-frame Brush rebuild).
                    val edgeMask = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to sweepStyle.fillTop.copy(alpha = 0f),
                            INDETERMINATE_EDGE_FADE_FRACTION to sweepStyle.fillTop.copy(alpha = 1f),
                            (1f - INDETERMINATE_EDGE_FADE_FRACTION) to sweepStyle.fillTop.copy(alpha = 1f),
                            1f to sweepStyle.fillTop.copy(alpha = 0f),
                        ),
                    )
                    onDrawBehind { drawRect(brush = edgeMask, blendMode = BlendMode.DstIn) }
                }
        )
    }
}
