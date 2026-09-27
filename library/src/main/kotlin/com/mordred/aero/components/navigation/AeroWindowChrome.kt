package com.mordred.aero.components.navigation

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.mordred.aero.internal.windows.PxRect
import com.mordred.aero.internal.windows.WindowRegionsDirectory
import com.mordred.aero.internal.windows.isWindowsOs
import kotlin.math.roundToInt

/**
 * API-02 / BTN-02: marks this element as interactive content sitting over the window's
 * title bar, so on the native-chrome path Windows classifies its pixels as client area
 * (`HTCLIENT`) instead of caption (`HTCAPTION`): the element keeps its ordinary Compose
 * pointer input (e.g. `clickable`) instead of starting a window drag.
 *
 * Typical use: an element the app overlays on the `AeroTitleBar` row — e.g. Pinya's
 * "return the queue" button on its detached queue window. The `AeroTitleBar`'s own
 * `leading` slot is already interactive without this modifier.
 *
 * No effect off Windows, and inert on any window where no native subclass is installed
 * (`AeroTitleBar(..., nativeWindowManagement = false)`, or a failed install): nothing
 * reads the published region there. Outside a window composition (`LocalAwtWindow` null,
 * e.g. tests) the receiver is returned unchanged.
 *
 * The element's live bounds are published to the window's hit-test region registry on
 * every layout pass and removed when the element leaves composition, so a hidden or
 * moved element stops claiming pixels immediately.
 */
@OptIn(ExperimentalComposeUiApi::class)
public fun Modifier.markAeroTitleBarInteractive(): Modifier {
    if (!isWindowsOs) return this
    return composed {
        val window = LocalAwtWindow.current ?: return@composed this
        val regions = remember(window) { WindowRegionsDirectory.forWindow(window) }
        val id = remember(regions) { regions.newInteractiveId() }
        DisposableEffect(regions, id) {
            onDispose { regions.publishInteractive(id, null) }
        }
        this.onGloballyPositioned { coordinates ->
            regions.publishInteractive(id, coordinates.boundsInWindow(clipBounds = true).toPxRect())
        }
    }
}

private fun Rect.toPxRect(): PxRect =
    PxRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
