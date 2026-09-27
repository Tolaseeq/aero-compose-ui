package com.mordred.aero.components.navigation

import androidx.compose.ui.window.WindowPlacement
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WIN-02 / API-01: headless lock for the one resize-path-per-platform gate. The Compose drag
 * zones compose only while the window floats AND no native chrome is active — once the Windows
 * subclass answers HTLEFT..HTBOTTOMRIGHT, a second Compose-side drag path would double-handle
 * presses. On Linux/macOS `nativeChromeActive` is always false, so the handles keep working
 * exactly as before.
 */
class ResizeHandlesGateTest {

    /** API-01: the non-Windows behavior — floating window, no native chrome — composes handles. */
    @Test
    fun floatingWindowWithoutNativeChromeComposesTheHandles() {
        assertTrue(shouldComposeResizeHandles(WindowPlacement.Floating, nativeChromeActive = false))
    }

    /** WIN-02: native chrome active — the Compose handles stand down on Windows. */
    @Test
    fun floatingWindowWithNativeChromeStandsDown() {
        assertFalse(shouldComposeResizeHandles(WindowPlacement.Floating, nativeChromeActive = true))
    }

    /** WIN-02: a maximized window never composes resize handles, native or not. */
    @Test
    fun maximizedWindowNeverComposesTheHandles() {
        assertFalse(shouldComposeResizeHandles(WindowPlacement.Maximized, nativeChromeActive = false))
        assertFalse(shouldComposeResizeHandles(WindowPlacement.Maximized, nativeChromeActive = true))
    }

    /** WIN-02: a fullscreen window never composes resize handles, native or not. */
    @Test
    fun fullscreenWindowNeverComposesTheHandles() {
        assertFalse(shouldComposeResizeHandles(WindowPlacement.Fullscreen, nativeChromeActive = false))
        assertFalse(shouldComposeResizeHandles(WindowPlacement.Fullscreen, nativeChromeActive = true))
    }
}
