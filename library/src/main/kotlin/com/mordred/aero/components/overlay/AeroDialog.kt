package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.components.navigation.AeroTitleBar
import com.mordred.aero.theme.AeroTheme

/**
 * OVL-01: Aero modal dialog with [AeroTitleBar] chrome. Three slots: [title],
 * [content], [buttons]. The [buttons] slot is a `RowScope` lambda — callers typically
 * place 1-3 AeroButton/AeroOutlinedButton children, right-aligned by `Arrangement.End`.
 *
 * Win11 rule: the underlying Window must keep `transparent = false` to avoid
 * `EXCEPTION_ACCESS_VIOLATION` (CMP-3757 / GH#3171). Esc dismisses.
 *
 * @param onDismissRequest invoked when the user dismisses (Esc, close button, system close).
 * @param title title slot rendered at the top of the dialog content area.
 * @param content body slot rendered between title and buttons.
 * @param buttons buttons RowScope slot, right-aligned by `Arrangement.End`.
 * @param dialogWidth window width.
 * @param dialogHeight window height.
 */
@Composable
public fun AeroDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit = {},
    dialogWidth: Dp = 420.dp,
    dialogHeight: Dp = 220.dp
) {
    val windowState = rememberWindowState(width = dialogWidth, height = dialogHeight)
    Window(
        onCloseRequest = onDismissRequest,
        state = windowState,
        // Win11 rule — see KDoc.
        undecorated = true,
        transparent = false,
        resizable = false,
        title = "AeroDialog",
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                onDismissRequest()
                true
            } else false
        }
    ) {
        val colors = AeroTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.panelBackground,
                            colors.panelBackground.copy(alpha = colors.panelBackground.alpha * 0.7f)
                        )
                    )
                )
                .border(1.dp, colors.titleBarGradientStart)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AeroTitleBar(
                    title = "",
                    windowState = windowState,
                    onCloseRequest = onDismissRequest
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    title()
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                    Row(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        horizontalArrangement = Arrangement.End,
                        content = buttons
                    )
                }
            }
        }
    }
}
