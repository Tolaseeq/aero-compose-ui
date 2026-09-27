package com.mordred.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.components.navigation.AeroResizeHandles
import com.mordred.aero.components.navigation.AeroTitleBar
import com.mordred.aero.components.navigation.markAeroTitleBarInteractive
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import java.awt.Dimension

/**
 * SHW-17 fixture: a second, narrow (300 × 480 dp) window mirroring Pinya's detached request
 * queue — an independent top-level window with its own `AeroTitleBar` + `AeroResizeHandles`,
 * a waiting-requests counter badge in the title bar's leading slot, and a "Вернуть" action
 * element overlaid on the title bar row.
 *
 * The window sets its own AWT minimum size (260 × 200, below the library's 320 × 240 dp floor)
 * so the app-provided minimum is honored instead of the default. In [capture] mode the window
 * is non-focusable and starts behind the reviewer's other windows, exactly like the main
 * capture window, and gets a distinct `[capture]` title so the probe can find each window by
 * exact title.
 *
 * Every externally observable interaction prints an `AERO_EVENT` line (gated by
 * `-Daero.windowState=true`): `name=leading` for the badge click, `name=marked` for the
 * overlay click, `close-request label=narrow` for either close path. The overlay is marked
 * interactive via `markAeroTitleBarInteractive()` (API-02), so on the native path its pixels
 * answer HTCLIENT and it keeps its ordinary clicks; [nativeChrome] = false opens the
 * pre-phase behavior for this window instead (the permanent RED control).
 */
@Composable
public fun ApplicationScope.NarrowQueueWindow(scheme: AeroColorScheme, capture: Boolean, nativeChrome: Boolean, onClose: () -> Unit) {
    val state = rememberWindowState(
        width = 300.dp,
        height = 480.dp,
        // Capture frames must land at a fixed, known position; a normal launch keeps the
        // platform's own placement.
        position = if (capture) WindowPosition(Alignment.CenterEnd) else WindowPosition.PlatformDefault
    )
    val requestClose: () -> Unit = {
        reportShowcaseEvent("close-request", "label=narrow")
        onClose()
    }
    Window(
        onCloseRequest = requestClose,
        title = if (capture) "aero-compose-ui Queue [capture]" else "aero-compose-ui Queue",
        state = state,
        // Same Win11 rule as the main window: undecorated = true ONLY, transparent MUST stay
        // false (CMP-3757 / GH#3171).
        undecorated = true,
        transparent = false,
        // A capture window must never take input focus.
        focusable = !capture
    ) {
        LaunchedEffect(Unit) {
            // The app's own minimum, below the library's 320 × 240 dp floor, so this window
            // opens at ~300 px and can be resized back down to it.
            window.minimumSize = Dimension(260, 200)
            if (capture) {
                // Start behind the reviewer's other windows instead of popping to the front.
                window.toBack()
            }
        }

        WindowStateReporter(state, label = "narrow")

        AeroTheme(scheme) {
            Box(Modifier.fillMaxSize().background(AeroTheme.colors.background)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(32.dp)) {
                        AeroTitleBar(
                            title = "Queue",
                            windowState = state,
                            onCloseRequest = requestClose,
                            nativeWindowManagement = nativeChrome,
                            leading = {
                                Box(
                                    Modifier
                                        .size(24.dp, 20.dp)
                                        .clickable { reportShowcaseEvent("click", "name=leading") }
                                ) {
                                    Text(
                                        text = "3",
                                        color = AeroTheme.colors.titleBarText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        )
                        // "Вернуть" action element overlaid on the title bar row, right edge
                        // 152 dp from the window's right edge (8 dp padding + 3 × 46 dp buttons
                        // + 6 dp gap).
                        Box(
                            Modifier
                                .markAeroTitleBarInteractive()
                                .align(Alignment.TopEnd)
                                .padding(top = 5.dp, end = 152.dp)
                                .size(60.dp, 22.dp)
                                .clickable { reportShowcaseEvent("click", "name=marked") }
                        ) {
                            Text(
                                text = "Вернуть",
                                color = AeroTheme.colors.titleBarText,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        for (i in 1..5) {
                            Text(
                                text = "Request $i",
                                color = AeroTheme.colors.onBackground,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                AeroResizeHandles(state)
            }
        }
    }
}
