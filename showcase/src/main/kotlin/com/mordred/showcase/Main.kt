package com.mordred.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.components.navigation.AeroResizeHandles
import com.mordred.aero.components.navigation.AeroTitleBar
import com.mordred.aero.theme.AeroTheme

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "aero-compose-ui Showcase",
        state = windowState,
        // Win11 rule: undecorated = true ONLY. transparent MUST stay false to avoid
        // EXCEPTION_ACCESS_VIOLATION (CMP-3757 / GH#3171). Glass effect lives in glassEffect modifier.
        undecorated = true,
        transparent = false
    ) {
        AeroTheme {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    AeroTitleBar(
                        title = "aero-compose-ui Showcase",
                        windowState = windowState,
                        onCloseRequest = ::exitApplication
                    )
                    ShowcaseApp()
                }
                AeroResizeHandles(windowState)
            }
        }
    }
}
