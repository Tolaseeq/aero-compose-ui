package com.mordred.aero.components.overlay

import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Info
import com.mordred.aero.icons.`internal`.Warning
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.icons.`internal`.CheckCircle
import kotlin.test.Test
import kotlin.test.assertEquals

class AeroBannerKindTest {

    @Test
    fun definesExactlyFourKinds() {
        val names = AeroBannerKind.values().map { it.name }.toSet()
        assertEquals(setOf("Info", "Warning", "Error", "Success"), names)
    }

    @Test
    fun infoMapsToInfoIcon() {
        assertEquals(AeroIcons.Info, AeroBannerKind.Info.icon)
    }

    @Test
    fun warningMapsToWarningIcon() {
        assertEquals(AeroIcons.Warning, AeroBannerKind.Warning.icon)
    }

    @Test
    fun errorMapsToErrorIcon() {
        assertEquals(AeroIcons.XCircle, AeroBannerKind.Error.icon)
    }

    @Test
    fun successMapsToCheckCircleIcon() {
        assertEquals(AeroIcons.CheckCircle, AeroBannerKind.Success.icon)
    }
}
