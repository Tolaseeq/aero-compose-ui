package com.mordred.aero.components.containers

import kotlin.test.Test
import kotlin.test.assertNotNull

class AeroCardTest {

    /**
     * Compile-reachability — if AeroCard is renamed/moved, this fails to load.
     *
     * Note: cannot use a `::AeroCard` function reference because Kotlin Compose
     * plugin disallows function references to `@Composable` functions. We instead
     * assert the generated `<File>Kt` class is loadable from the classpath.
     */
    @Test
    fun aeroCardCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.containers.AeroCardKt")
        assertNotNull(cls)
    }
}
