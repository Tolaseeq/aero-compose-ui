package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AeroDrawerTest {

    @Test
    fun aeroDrawerSideEnumHasStartAndEnd() {
        assertEquals(setOf("Start", "End"), AeroDrawerSide.values().map { it.name }.toSet())
    }

    @Test
    fun aeroDrawerCompileSmoke() {
        // Probe via reflection — Kotlin Compose plugin disallows function refs to @Composable functions.
        val cls = Class.forName("com.mordred.aero.components.overlay.AeroDrawerKt")
        assertNotNull(cls)
    }
}
