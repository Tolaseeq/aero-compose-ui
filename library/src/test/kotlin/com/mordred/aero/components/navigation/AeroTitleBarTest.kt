package com.mordred.aero.components.navigation

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
}
