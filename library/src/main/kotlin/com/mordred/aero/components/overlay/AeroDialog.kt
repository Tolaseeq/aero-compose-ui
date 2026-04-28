package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.theme.AeroTheme

/**
 * OVL-01: Aero modal dialog backed by a [Window] (not DialogWindow) so we can mount a
 * full Aero titlebar with drag + minimize + close. Three slots: [title], [content],
 * [buttons]. The [buttons] slot is a `RowScope` lambda — callers typically place 1-3
 * AeroButton/AeroOutlinedButton children, right-aligned via `Arrangement.End`.
 *
 * **Window choice:** `Window` (instead of `DialogWindow`) gives us `FrameWindowScope`,
 * which is required for `WindowDraggableArea` (smooth, native window drag with no
 * recomposition cost) and `WindowState.isMinimized` (the user-requested minimize button).
 * The trade-off: `Window` is non-modal at the OS level. For showcase purposes that's
 * acceptable; callers needing OS modality can wrap the trigger in their own state guard.
 *
 * **Win11 rule:** the underlying Window uses `undecorated = true`, `transparent = false`
 * to avoid `EXCEPTION_ACCESS_VIOLATION` (CMP-3757 / GH#3171).
 *
 * **Esc key:** dismissed via `onPreviewKeyEvent`.
 *
 * @param onDismissRequest invoked when the user dismisses (Esc, close button, system close).
 * @param title title slot — rendered at the top of the dialog content area.
 * @param content body slot — rendered between title and buttons.
 * @param buttons buttons RowScope slot — right-aligned by `Arrangement.End`.
 * @param dialogWidth window width (default 420.dp). Pass smaller values for compact alerts.
 * @param dialogHeight window height (default 220.dp — fits a single-line message + buttons).
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
    var isMinimized by remember { mutableStateOf(false) }
    windowState.isMinimized = isMinimized

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
        // Outer Box paints a fully opaque theme background to defeat the OS-default
        // white window beneath, then overlays the panel-tint gradient.
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.panelBackground,
                            colors.panelBackground.copy(alpha = colors.panelBackground.alpha * 0.85f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AeroDialogTitleBar(
                    onMinimizeRequest = { isMinimized = true },
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

/**
 * Aero-styled titlebar for AeroDialog. Drag area uses [WindowDraggableArea] (native AWT
 * drag — no per-pixel recomposition lag), plus minimize and close buttons. Maximize is
 * deliberately omitted because dialogs are fixed-size.
 */
@Composable
private fun androidx.compose.ui.window.FrameWindowScope.AeroDialogTitleBar(
    onMinimizeRequest: () -> Unit,
    onCloseRequest: () -> Unit
) {
    val colors = AeroTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowDraggableArea(modifier = Modifier.weight(1f).fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .clickable(onClick = onMinimizeRequest),
            contentAlignment = Alignment.Center
        ) {
            Text("─", color = colors.onSurface)
        }
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .clickable(onClick = onCloseRequest),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = colors.onSurface)
        }
    }
}
