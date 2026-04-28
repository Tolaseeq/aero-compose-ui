package com.mordred.aero.components.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.mordred.aero.theme.glassSurface

/**
 * OVL-01: Aero modal dialog backed by an OS-level [DialogWindow].
 *
 * Three slots: [title], [content], [buttons]. The [buttons] slot is a `RowScope`
 * lambda — callers typically place 1-3 [com.mordred.aero.components.buttons.AeroButton]
 * / [com.mordred.aero.components.buttons.AeroOutlinedButton] children. The buttons row
 * is right-aligned via `Arrangement.End`.
 *
 * **Win11 rule:** the underlying DialogWindow uses `undecorated = true` ONLY.
 * `transparent = false` is mandatory to avoid EXCEPTION_ACCESS_VIOLATION
 * (CMP-3757 / GH#3171). The visual glass containment comes from
 * `Modifier.glassSurface` on the inner Box, NOT from window transparency.
 *
 * **Esc key:** dismissed via `onPreviewKeyEvent`.
 *
 * @param onDismissRequest invoked when the user dismisses (Esc, scrim, system close).
 * @param title title slot — rendered at the top of the dialog.
 * @param content body slot — rendered between title and buttons.
 * @param buttons buttons RowScope slot — right-aligned by `Arrangement.End`.
 * @param dialogWidth dialog window width (default 480.dp).
 * @param dialogHeight dialog window height (default 320.dp).
 */
@Composable
public fun AeroDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit = {},
    dialogWidth: Dp = 480.dp,
    dialogHeight: Dp = 320.dp
) {
    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = rememberDialogState(width = dialogWidth, height = dialogHeight),
        // Win11 rule — see KDoc:
        undecorated = true,
        transparent = false,
        resizable = false,
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                onDismissRequest()
                true
            } else false
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .glassSurface(cornerRadius = 8.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                title()
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    content = buttons
                )
            }
        }
    }
}
