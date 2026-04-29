package com.mordred.aero.components.overlay

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel

/**
 * OVL-06: In-flow notification banner. Caller places it inline (top of a form,
 * under a title bar, inside a section) and controls visibility via standard
 * `if (visible) AeroNotificationBanner(...)`.
 *
 * No host, no queue — purely visual.
 *
 * @param kind variant — drives icon + accent border color.
 * @param text body text.
 * @param onDismiss optional close-button callback. When null, no close button.
 * @param actions optional `RowScope` slot for additional action buttons (placed
 *   between the text and the close button).
 * @param modifier optional outer modifier.
 */
@Composable
public fun AeroNotificationBanner(
    kind: AeroBannerKind,
    text: String,
    onDismiss: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = accentForBanner(kind)
    val shape = RoundedCornerShape(4.dp)
    val colors = AeroTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel(cornerRadius = 4.dp)
            .border(width = 1.dp, color = accent.copy(alpha = 0.6f), shape = shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = kind.icon, contentDescription = null, tint = accent)
        Text(
            text = text,
            color = colors.onSurface,
            style = AeroTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (actions != null) actions()
        if (onDismiss != null) {
            AeroIconButton(onClick = onDismiss, size = 24.dp) {
                Icon(
                    imageVector = AeroIcons.X,
                    contentDescription = "Close notification",
                    modifier = Modifier.size(14.dp),
                    tint = colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun accentForBanner(kind: AeroBannerKind): Color {
    val colors = AeroTheme.colors
    return when (kind) {
        AeroBannerKind.Info    -> colors.primary
        AeroBannerKind.Warning -> Color(0xFFFFA726)
        AeroBannerKind.Error   -> colors.error
        AeroBannerKind.Success -> Color(0xFF66BB6A)
    }
}
