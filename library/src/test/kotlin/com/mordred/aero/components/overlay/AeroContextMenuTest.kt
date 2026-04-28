package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * `Modifier.aeroContextMenu` is an extension on `Modifier` and uses
 * `composed { ... }` — it cannot be invoked headlessly. Compile-reachability
 * via Class.forName (Compose plugin disallows function references to
 * @Composable contexts; the established pattern from 03-01, 03-02, 03-04).
 */
class AeroContextMenuTest {

    @Test
    fun aeroContextMenuFileCompiles() {
        val cls = Class.forName("com.mordred.aero.components.overlay.AeroContextMenuKt")
        assertNotNull(cls)
        assertTrue(
            cls.methods.any { it.name == "aeroContextMenu" },
            "Modifier.aeroContextMenu extension expected"
        )
    }
}
