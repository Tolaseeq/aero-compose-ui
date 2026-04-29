package com.mordred.aero.components.overlay

import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Info
import com.mordred.aero.icons.`internal`.Warning
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.icons.`internal`.Question
import kotlin.test.Test
import kotlin.test.assertEquals

class AeroAlertKindTest {

    @Test
    fun definesExactlyFourKinds() {
        val names = AeroAlertKind.values().map { it.name }.toSet()
        assertEquals(setOf("Info", "Warning", "Error", "Question"), names)
    }

    @Test
    fun infoMapsToInfoIcon() {
        assertEquals(AeroIcons.Info, AeroAlertKind.Info.icon)
    }

    @Test
    fun warningMapsToWarningIcon() {
        assertEquals(AeroIcons.Warning, AeroAlertKind.Warning.icon)
    }

    @Test
    fun errorMapsToErrorIcon() {
        assertEquals(AeroIcons.XCircle, AeroAlertKind.Error.icon)
    }

    @Test
    fun questionMapsToHelpOutlineIcon() {
        assertEquals(AeroIcons.Question, AeroAlertKind.Question.icon)
    }
}
