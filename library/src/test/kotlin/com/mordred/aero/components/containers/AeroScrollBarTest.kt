package com.mordred.aero.components.containers

import kotlin.test.Test
import kotlin.test.assertNotNull

class AeroScrollBarTest {

    /**
     * Compile-reachability — see AeroScrollAreaTest for rationale on using
     * `Class.forName` instead of `::AeroScrollBar` function reference.
     */
    @Test
    fun aeroScrollBarCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.containers.AeroScrollBarKt")
        assertNotNull(cls)
    }
}
