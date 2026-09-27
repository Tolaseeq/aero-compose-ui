package com.mordred.aero.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Compile-only test — AeroTitleBar requires FrameWindowScope receiver and cannot
 * be invoked headlessly. Manual smoke is the primary verification (see VALIDATION.md).
 *
 * This test exists so that a `./gradlew :library:test` run touches the test class
 * file, which transitively forces compilation of AeroTitleBar.kt.
 */
class AeroTitleBarTest {

    @Test
    fun aeroTitleBarFileCompiles() {
        // If AeroTitleBar.kt fails to compile, this test class also fails to compile,
        // and the test phase reports a build failure. That's the contract.
        assertTrue(true)
    }

    /**
     * API-01: the pre-phase call shapes still compile unchanged, and the added surface
     * (the `nativeWindowManagement` opt-out and `Modifier.markAeroTitleBarInteractive()`)
     * compiles too. Compilation of [preservedCallShapes] below is the gate — if any shape
     * regressed, this file would stop compiling and the test phase would fail.
     */
    @Test
    fun prePhaseCallShapesStillCompile() {
        assertTrue(true)
    }

    @Composable
    @Suppress("unused")
    private fun FrameWindowScope.preservedCallShapes(ws: WindowState) {
        AeroTitleBar("t", ws, {})
        AeroTitleBar(title = "t", windowState = ws, onCloseRequest = {}, leading = null, modifier = Modifier)
        AeroResizeHandles(ws)
        AeroTitleBar("t", ws, {}, nativeWindowManagement = false)
        Box(Modifier.markAeroTitleBarInteractive())
    }
}
