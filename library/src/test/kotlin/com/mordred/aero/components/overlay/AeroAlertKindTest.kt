package com.mordred.aero.components.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
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
        assertEquals(Icons.Outlined.Info, AeroAlertKind.Info.icon)
    }

    @Test
    fun warningMapsToWarningIcon() {
        assertEquals(Icons.Outlined.Warning, AeroAlertKind.Warning.icon)
    }

    @Test
    fun errorMapsToErrorIcon() {
        assertEquals(Icons.Outlined.Error, AeroAlertKind.Error.icon)
    }

    @Test
    fun questionMapsToHelpOutlineIcon() {
        assertEquals(Icons.Outlined.HelpOutline, AeroAlertKind.Question.icon)
    }
}
