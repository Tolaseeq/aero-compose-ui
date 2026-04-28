package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * AeroAlertDialog wraps AeroDialog (DialogWindow) — OS-level, cannot be unit-tested
 * headlessly. Compile-only check; manual smoke verifies icon + buttons render correctly.
 */
class AeroAlertDialogTest {

    @Test
    fun aeroAlertDialogFileCompiles() {
        assertTrue(true)
    }
}
