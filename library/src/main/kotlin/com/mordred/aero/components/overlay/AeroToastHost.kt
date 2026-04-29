package com.mordred.aero.components.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassEffect
import kotlinx.coroutines.delay

/**
 * OVL-05: Host that displays all toasts queued in [AeroToastHostState].
 *
 * Place once at app root with `Modifier.fillMaxSize()`. Toasts stack at Bottom-end
 * with 16dp inset from window edges. Per-toast LaunchedEffect drives the dismiss
 * timer; `key(data.id)` ensures animation state survives stack reorders.
 *
 * @param state the [AeroToastHostState] driving this host.
 * @param modifier optional outer modifier.
 */
@Composable
public fun AeroToastHost(
    state: AeroToastHostState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.toasts.forEach { data ->
                key(data.id) {
                    if (data.duration != AeroToastDuration.Indefinite) {
                        LaunchedEffect(data.id) {
                            delay(data.duration.millis)
                            state.dismiss(data.id, AeroToastResult.Dismissed)
                        }
                    }
                    AeroToastItem(
                        data = data,
                        onAction = { state.dismiss(data.id, AeroToastResult.ActionPerformed) },
                        onDismiss = { state.dismiss(data.id, AeroToastResult.Dismissed) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AeroToastItem(
    data: AeroToastData,
    onAction: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AeroTheme.colors
    Row(
        modifier = Modifier
            .glassEffect(cornerRadius = 6.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = data.message,
            color = colors.onSurface,
            style = AeroTheme.typography.bodyLarge,
            modifier = Modifier.width(280.dp)
        )
        if (data.action != null) {
            AeroOutlinedButton(
                text = data.action.label,
                onClick = {
                    data.action.onClick()
                    onAction()
                }
            )
        }
        AeroIconButton(onClick = onDismiss, size = 24.dp) {
            Icon(
                imageVector = AeroIcons.X,
                contentDescription = "Close toast",
                modifier = Modifier.size(14.dp),
                tint = colors.onSurface
            )
        }
    }
}
