package com.mordred.aero.components.range

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * Determinate progress bar. Renders a filled track showing [progress] (0f..1f).
 *
 * @param progress Current progress clamped to 0f..1f.
 * @param modifier Layout modifier.
 * @param showPercent When true a percentage label is shown below/end of the bar.
 * @param height Bar thickness.
 */
@Composable
public fun AeroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    height: Dp = 8.dp
) {
    val colors = AeroTheme.colors
    val clamped = progress.coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(colors.surface, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(clamped)
                    .height(height)
                    .background(colors.primary, RoundedCornerShape(50))
            )
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
 * Indeterminate progress bar. Renders a 30%-wide shimmer bar that sweeps left-to-right
 * in a 1500 ms loop with [LinearEasing].
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

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
    ) {
        val offsetX = with(LocalDensity.current) { (shimmer * maxWidth.toPx()).toDp() }
        Box(
            Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .offset(x = offsetX)
                .background(colors.primary)
        )
    }
}
