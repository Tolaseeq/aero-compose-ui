package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AeroPopover wraps androidx.compose.ui.window.Popup which is OS-mediated —
 * cannot be unit-tested headlessly. Compile-reachability via Class.forName
 * (Compose plugin disallows function references to @Composable functions —
 * established pattern from Plans 03-01, 03-02, 03-04).
 */
class AeroPopoverTest {

    @Test
    fun aeroPopoverCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.overlay.AeroPopoverKt")
        assertNotNull(cls)
        assertTrue(cls.methods.any { it.name == "AeroPopover" })
    }
}
