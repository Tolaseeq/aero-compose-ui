package com.mordred.aero.components.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AeroTabBarTest {

    /**
     * Compile-reachability — Kotlin Compose plugin disallows function references
     * to `@Composable` functions, so we probe via the generated `<File>Kt` class.
     */
    @Test
    fun aeroTabBarCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.navigation.AeroTabBarKt")
        assertNotNull(cls)
        val hasTabBar = cls.methods.any { it.name == "AeroTabBar" }
        assertEquals(true, hasTabBar)
    }

    @Test
    fun aeroTabDataClassEquality() {
        val t1 = AeroTab(label = "Home")
        val t2 = AeroTab(label = "Home")
        assertEquals(t1, t2)
    }
}
