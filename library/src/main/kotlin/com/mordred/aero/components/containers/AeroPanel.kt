package com.mordred.aero.components.containers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.glassPanel

/**
 * CNT-02: Section-level Aero panel. Uses `Modifier.glassPanel` (gradient fill, no
 * shadow, no border) — suitable as the background of large content groups (a
 * settings page, a side panel, a dashboard region).
 *
 * @param modifier optional outer modifier.
 * @param cornerRadius corner radius (default 0.dp — full-width panels).
 * @param padding inner content padding (default 16.dp).
 * @param content panel content.
 */
@Composable
public fun AeroPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassPanel(cornerRadius = cornerRadius)
            .padding(padding)
    ) {
        content()
    }
}
