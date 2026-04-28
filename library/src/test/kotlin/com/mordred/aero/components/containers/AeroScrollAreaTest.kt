package com.mordred.aero.components.containers

import kotlin.test.Test
import kotlin.test.assertNotNull

class AeroScrollAreaTest {

    /** Compile-reachability — if AeroScrollArea is renamed/moved, this fails. */
    @Test
    fun aeroScrollAreaCompileSmoke() {
        val ref = ::AeroScrollArea
        assertNotNull(ref)
    }
}
