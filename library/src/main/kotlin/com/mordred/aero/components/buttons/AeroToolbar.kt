package com.mordred.aero.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

/**
 * Aero-styled horizontal toolbar with glass background (BTN-04).
 *
 * Place [AeroIconButton] composables (and optional [AeroToolbarDefaults.Divider]) inside
 * the content block. The toolbar renders a [glassPanel] background at 40.dp height.
 *
 * @param modifier Optional [Modifier] applied after the height and glass modifiers.
 * @param showDividers Reserved for future auto-injection of dividers between children.
 *   Currently a no-op — consumers add dividers manually via [AeroToolbarDefaults.Divider].
 * @param content Row-scoped content lambda. Place [AeroIconButton]s and
 *   [AeroToolbarDefaults.Divider]s here.
 */
@Composable
public fun AeroToolbar(
    modifier: Modifier = Modifier,
    showDividers: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .then(modifier)
            .glassPanel(cornerRadius = 4.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

/**
 * Helper composable for toolbar vertical dividers.
 *
 * Renders a 1.dp-wide full-height rule in [AeroTheme.colors.borderDefault] at 50% alpha.
 * Use inside [AeroToolbar]'s content lambda to visually separate groups of [AeroIconButton]s.
 */
public object AeroToolbarDefaults {
    @Composable
    public fun Divider() {
        val colors = AeroTheme.colors
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(colors.borderDefault.copy(alpha = 0.5f))
        )
    }
}
