package com.mordred.showcase

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mordred.aero.components.navigation.AeroResizeHandles
import com.mordred.aero.components.navigation.AeroTitleBar
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlinx.coroutines.delay

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

/**
 * Section the showcase renders alone, from `-Daero.section=<Name>`.
 *
 * Exists so a capture pass reaches a single section without scrolling the window by hand —
 * scrolling means injecting synthetic input into whatever desktop the reviewer is using at the
 * time. Absent or blank falls back to `null`, which renders the full page unchanged.
 */
private fun initialSection(): String? =
    System.getProperty("aero.section")?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Page of the active section to scroll to, from `-Daero.page=<N>`.
 *
 * Exists so a capture pass reaches every part of a tall section in whole, reproducible frames
 * without injecting a scroll gesture. Absent, blank or negative falls back to page 0.
 */
private fun initialPage(): Int =
    System.getProperty("aero.page")?.toIntOrNull()?.coerceAtLeast(0) ?: 0

/**
 * Whether this launch is a capture pass, from `-Daero.capture=true`.
 *
 * Exists so a capture window can be made non-focusable and stay behind the reviewer's other
 * windows, instead of stealing input focus the moment it opens.
 */
private fun captureMode(): Boolean = System.getProperty("aero.capture") == "true"

/**
 * When the narrow queue window opens, from `-Daero.secondWindow=true|=<milliseconds>`.
 *
 * Exists so a verification pass can open the SHW-17 narrow window deterministically: `"true"`
 * opens it at start (0 ms), a positive integer opens it after that delay (used to open the
 * window while the main window is being dragged), and absent or unrecognised values never
 * open it — so a default launch is unchanged.
 */
private fun secondWindowOpenDelayMs(): Long? = when (val value = System.getProperty("aero.secondWindow")) {
    "true" -> 0L
    else -> value?.toLongOrNull()?.takeIf { it > 0L }
}

/**
 * Whether the windows opt into native window management, from `-Daero.nativeChrome=false`.
 *
 * Exists so a review pass can open the pre-phase behavior (`WindowDraggableArea` drag, Compose
 * resize handles, no native subclass) as a permanent RED control for the VER-11 live checks:
 * every native-path check must fail on this launch. Absent or any other value keeps the
 * default native behavior, so a default launch is unchanged.
 */
private fun nativeChromeEnabled(): Boolean = System.getProperty("aero.nativeChrome") != "false"

fun main() {
    val section = initialSection()
    val page = initialPage()
    val capture = captureMode()
    val nativeChrome = nativeChromeEnabled()
    val secondWindowDelayMs = secondWindowOpenDelayMs()

    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp,
            // Capture frames always land centred on the primary screen at one DPI; a normal
            // launch keeps the platform's own placement.
            position = if (capture) WindowPosition(Alignment.Center) else WindowPosition.PlatformDefault
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = if (capture) "aero-compose-ui Showcase [capture]" else "aero-compose-ui Showcase",
            state = windowState,
            // Win11 rule (CMP-3757 / GH#3171): undecorated = true ONLY; transparent MUST stay false
            // to avoid EXCEPTION_ACCESS_VIOLATION. Glass effect lives in glassEffect modifier.
            undecorated = true,
            transparent = false,
            // A capture window must never take input focus.
            focusable = !capture
        ) {
            if (capture) {
                // Start behind the reviewer's other windows instead of popping to the front.
                LaunchedEffect(Unit) { window.toBack() }
            }

            WindowStateReporter(windowState, label = "main")

            var currentScheme by remember { mutableStateOf(initialScheme()) }
            AeroTheme(colorScheme = currentScheme) {
                Box(Modifier.fillMaxSize().border(1.dp, AeroTheme.colors.titleBarGradientStart)) {
                    Column(Modifier.fillMaxSize()) {
                        AeroTitleBar(
                            title = "aero-compose-ui Showcase",
                            windowState = windowState,
                            onCloseRequest = ::exitApplication,
                            nativeWindowManagement = nativeChrome
                        )
                        ShowcaseApp(
                            currentScheme = currentScheme,
                            onSchemeChange = { currentScheme = it },
                            section = section,
                            page = page
                        )
                    }
                    AeroResizeHandles(windowState)
                }
            }
        }

        var narrowOpen by remember { mutableStateOf(secondWindowDelayMs == 0L) }
        if (secondWindowDelayMs != null && secondWindowDelayMs > 0L) {
            LaunchedEffect(Unit) {
                delay(secondWindowDelayMs)
                narrowOpen = true
            }
        }
        if (narrowOpen) {
            NarrowQueueWindow(initialScheme(), capture, nativeChrome, onClose = { narrowOpen = false })
        }
    }
}
