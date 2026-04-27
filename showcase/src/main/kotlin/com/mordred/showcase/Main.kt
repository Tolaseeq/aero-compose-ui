package com.mordred.showcase

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "aero-compose-ui Showcase",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        ShowcaseApp()
    }
}
