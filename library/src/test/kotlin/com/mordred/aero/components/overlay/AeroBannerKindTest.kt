package com.mordred.aero.components.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
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
        assertEquals(Icons.Outlined.Info, AeroBannerKind.Info.icon)
    }

    @Test
    fun warningMapsToWarningIcon() {
        assertEquals(Icons.Outlined.Warning, AeroBannerKind.Warning.icon)
    }

    @Test
    fun errorMapsToErrorIcon() {
        assertEquals(Icons.Outlined.Error, AeroBannerKind.Error.icon)
    }

    @Test
    fun successMapsToCheckCircleIcon() {
        assertEquals(Icons.Outlined.CheckCircle, AeroBannerKind.Success.icon)
    }
}
