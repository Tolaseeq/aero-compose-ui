package com.mordred.aero.components.containers

import kotlin.test.Test
import kotlin.test.assertNotNull

class AeroGroupBoxTest {

    /**
     * Compile-reachability — if AeroGroupBox is renamed/moved, this fails to load.
     *
     * Note: cannot use a `::AeroGroupBox` function reference because Kotlin Compose
     * plugin disallows function references to `@Composable` functions. We instead
     * assert the generated `<File>Kt` class is loadable from the classpath.
     */
    @Test
    fun aeroGroupBoxCompileSmoke() {
        val cls = Class.forName("com.mordred.aero.components.containers.AeroGroupBoxKt")
        assertNotNull(cls)
    }
}
