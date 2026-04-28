package com.mordred.aero.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.glassPanel

/**
 * NAV-03: Bottom-of-window status bar. Slot-based — caller arranges Text segments,
 * separators, and indicator dots inside a `RowScope` lambda. Slot-based wins over
 * data-driven (per CONTEXT.md "Claude's Discretion") because callers naturally mix
 * Text / Icon / Spacer / colored dots, and a slot composes those without forcing a
 * data-class wrapper for every visual variant.
 *
 * Visual: full-width Row, [height] tall, with `Modifier.glassPanel` background.
 *
 * Example:
 * ```
 * AeroStatusBar {
 *     Text("Ready")
 *     Spacer(Modifier.weight(1f))
 *     Text("Ln 24, Col 13")
 *     Box(Modifier.size(8.dp).background(Color.Green, CircleShape))
 * }
 * ```
 *
 * @param modifier outer modifier.
 * @param height status bar height (default 24.dp; mordred default).
 * @param content RowScope content slot.
 */
@Composable
public fun AeroStatusBar(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .glassPanel(cornerRadius = 0.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
