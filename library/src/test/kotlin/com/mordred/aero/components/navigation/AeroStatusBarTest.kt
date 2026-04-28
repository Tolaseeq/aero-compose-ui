package com.mordred.aero.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AeroStatusBarTest {

    /**
     * Compile-reachability — Kotlin Compose plugin disallows function references
     * to `@Composable` functions, so we probe via the generated `<File>Kt` class.
     */
    @Test
    fun aeroStatusBarCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.navigation.AeroStatusBarKt")
        assertNotNull(cls)
        // AeroStatusBar takes a `height: Dp` parameter — Dp is a Kotlin inline class,
        // so the JVM bytecode method name gets a hash suffix (`AeroStatusBar-uFdPcIQ`
        // at the time of writing). We match by prefix to remain stable across
        // Compose / Kotlin upgrades that may change the suffix.
        val hasStatusBar = cls.methods.any { it.name.startsWith("AeroStatusBar") }
        assertEquals(true, hasStatusBar)
    }
}
