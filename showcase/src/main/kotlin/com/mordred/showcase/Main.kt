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

/**
 * Scheme the showcase opens on, from `-Daero.scheme=AeroBlue|AeroDark|Classic`.
 *
 * Exists so a review pass can capture a specific theme by launching it directly, instead of
 * clicking the theme switcher — driving the switcher means injecting synthetic mouse input into
 * whatever desktop the reviewer is using at the time. Unrecognised or absent values fall back to
 * [AeroColorScheme.AeroBlue], so the default launch is unchanged.
 */
private fun initialScheme(): AeroColorScheme = when (System.getProperty("aero.scheme")) {
    "AeroDark" -> AeroColorScheme.AeroDark
    "Classic" -> AeroColorScheme.Classic
    else -> AeroColorScheme.AeroBlue
}

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
        var currentScheme by remember { mutableStateOf(initialScheme()) }
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
