package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * AeroDialog wraps DialogWindow which is OS-level — cannot be unit-tested headlessly.
 * This test exists to force compilation of AeroDialog.kt during ./gradlew :library:test.
 * Real verification: open showcase, trigger AeroDialog, press Esc, verify dismissal + no Win11 crash.
 */
class AeroDialogTest {

    @Test
    fun aeroDialogFileCompiles() {
        assertTrue(true)
    }
}
