package com.mordred.aero.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.mordred.aero.theme.AeroTheme
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
    val dialogState = rememberDialogState(width = dialogWidth, height = dialogHeight)
    DialogWindow(
        onCloseRequest = onDismissRequest,
        state = dialogState,
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
        val colors = AeroTheme.colors
        // Outer Box paints a fully opaque theme background to defeat the OS-default
        // white window beneath, then overlays the glassSurface highlight.
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
                .glassSurface(cornerRadius = 0.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                        onDismissRequest()
                        true
                    } else false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AeroDialogTitleBar(
                    dialogScope = this@DialogWindow,
                    onCloseRequest = onDismissRequest,
                    onDrag = { dx, dy ->
                        val current = dialogState.position
                        if (current is WindowPosition.Absolute) {
                            dialogState.position = WindowPosition.Absolute(
                                x = current.x + dx,
                                y = current.y + dy
                            )
                        }
                    }
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        content = buttons
                    )
                }
            }
        }
    }
}

/**
 * Custom titlebar for AeroDialog — drag-to-move via pointerInput (DialogWindowScope
 * lacks the FrameWindowScope.WindowDraggableArea extension, so we update
 * dialogState.position manually) plus a close button. Min/Maximize are intentionally
 * omitted: dialogs are modal fixed-size by design.
 */
@Composable
private fun AeroDialogTitleBar(
    dialogScope: DialogWindowScope,
    onCloseRequest: () -> Unit,
    onDrag: (dx: Dp, dy: Dp) -> Unit
) {
    val colors = AeroTheme.colors
    val density = androidx.compose.ui.platform.LocalDensity.current
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        with(density) {
                            onDrag(drag.x.toDp(), drag.y.toDp())
                        }
                    }
                }
        )
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 28.dp)
                .clickable(onClick = onCloseRequest),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = colors.onSurface)
        }
    }
    @Suppress("UNUSED_EXPRESSION") dialogScope // kept for future scope-bound enhancements
}
