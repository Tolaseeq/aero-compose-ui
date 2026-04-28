package com.mordred.aero.components.overlay

import kotlin.test.Test
import kotlin.test.assertNotNull

class AeroNotificationBannerTest {

    @Test
    fun aeroNotificationBannerCompileSmoke() {
        // Reachability check: if AeroNotificationBanner.kt fails to compile,
        // this test won't compile either, exposing the build break.
        val ref = Class.forName("com.mordred.aero.components.overlay.AeroNotificationBannerKt")
        assertNotNull(ref)
    }
}
