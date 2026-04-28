package com.mordred.showcase

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.components.navigation.AeroResizeHandles
import com.mordred.aero.components.navigation.AeroTitleBar
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme

fun main() = application {
    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "aero-compose-ui Showcase",
        state = windowState,
        // Win11 rule (CMP-3757 / GH#3171): undecorated = true ONLY; transparent MUST stay false
        // to avoid EXCEPTION_ACCESS_VIOLATION. Glass effect lives in glassEffect modifier.
        undecorated = true,
        transparent = false
    ) {
        var currentScheme by remember { mutableStateOf(AeroColorScheme.AeroBlue) }
        AeroTheme(colorScheme = currentScheme) {
            Box(Modifier.fillMaxSize().border(1.dp, AeroTheme.colors.titleBarGradientStart)) {
                Column(Modifier.fillMaxSize()) {
                    AeroTitleBar(
                        title = "aero-compose-ui Showcase",
                        windowState = windowState,
                        onCloseRequest = ::exitApplication
                    )
                    ShowcaseApp(
                        currentScheme = currentScheme,
                        onSchemeChange = { currentScheme = it }
                    )
                }
                AeroResizeHandles(windowState)
            }
        }
    }
}
