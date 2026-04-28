package com.mordred.aero.components.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.theme.AeroTheme

/**
 * OVL-02: Confirm/error dialog with icon + default buttons keyed by [kind].
 *
 * Layout: AeroDialog -> title row (icon + title text) -> message text -> button row.
 *
 * Default buttons:
 *  - Info / Warning / Error -> single OK button (label = [confirmText])
 *  - Question                -> OK + Cancel (Cancel uses [AeroOutlinedButton])
 *
 * @param onDismissRequest dismiss handler (Esc, OS close, Cancel button).
 * @param kind alert variant.
 * @param title title text.
 * @param message body text.
 * @param confirmText label for the confirm button (default "OK").
 * @param cancelText  label for the cancel button (Question kind only; default "Cancel").
 * @param onConfirm invoked when user clicks confirm; defaults to [onDismissRequest].
 */
@Composable
public fun AeroAlertDialog(
    onDismissRequest: () -> Unit,
    kind: AeroAlertKind,
    title: String,
    message: String,
    confirmText: String = "OK",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit = onDismissRequest
) {
    val accent = accentFor(kind)
    AeroDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = null,
                    tint = accent
                )
                Text(
                    text = title,
                    style = AeroTheme.typography.title,
                    color = AeroTheme.colors.onSurface
                )
            }
        },
        content = {
            Text(
                text = message,
                color = AeroTheme.colors.onSurface,
                style = AeroTheme.typography.bodyLarge
            )
        },
        buttons = {
            if (kind == AeroAlertKind.Question) {
                AeroOutlinedButton(text = cancelText, onClick = onDismissRequest)
                Spacer(Modifier.width(8.dp))
            }
            AeroButton(text = confirmText, onClick = onConfirm)
        }
    )
}

/**
 * Resolves the accent color for an [AeroAlertKind]. Theme tokens require @Composable
 * context, so this lookup must live here rather than on the enum.
 */
@Composable
internal fun accentFor(kind: AeroAlertKind): Color {
    val colors = AeroTheme.colors
    return when (kind) {
        AeroAlertKind.Info     -> colors.primary
        AeroAlertKind.Warning  -> Color(0xFFFFA726)
        AeroAlertKind.Error    -> colors.error
        AeroAlertKind.Question -> colors.primary
    }
}
