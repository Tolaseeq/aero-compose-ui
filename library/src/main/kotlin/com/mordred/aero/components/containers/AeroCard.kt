package com.mordred.aero.components.containers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.glassEffect

/**
 * CNT-01: Aero glass card. Renders a glass-styled rectangle with shadow,
 * rounded corners, and a centered content slot.
 *
 * Visual styling comes entirely from `Modifier.glassEffect` (single drawBehind pass:
 * gradient + border + shadow). Content is wrapped in a `Box` so callers can use
 * `Modifier.align(...)` and `matchParentSize()` if needed.
 *
 * @param modifier optional outer modifier.
 * @param cornerRadius corner radius (default 8.dp; matches glassEffect default).
 * @param elevation shadow depth (default 4.dp; matches glassEffect default).
 * @param padding inner content padding (default 16.dp).
 * @param content card content.
 */
@Composable
public fun AeroCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassEffect(cornerRadius = cornerRadius, elevation = elevation)
            .padding(padding)
    ) {
        content()
    }
}
