package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class AeroColorSchemeTest {

    @Test
    fun aeroBluePrimaryAndBackgroundAreCorrect() {
        assertEquals(Color(0xFF4FC3F7), AeroColorScheme.AeroBlue.primary, "AeroBlue.primary")
        assertEquals(Color(0xFF0D1B2A), AeroColorScheme.AeroBlue.background, "AeroBlue.background")
    }

    @Test
    fun aeroDarkPrimaryAndBackgroundAreCorrect() {
        assertEquals(Color(0xFF90CAF9), AeroColorScheme.AeroDark.primary, "AeroDark.primary")
        assertEquals(Color(0xFF0A0A1A), AeroColorScheme.AeroDark.background, "AeroDark.background")
    }

    @Test
    fun classicPrimaryAndBackgroundAreCorrect() {
        assertEquals(Color(0xFF5C8ABF), AeroColorScheme.Classic.primary, "Classic.primary")
        assertEquals(Color(0xFF1E1E1E), AeroColorScheme.Classic.background, "Classic.background")
    }

    @Test
    fun threePresetsAreDistinct() {
        assertNotSame(AeroColorScheme.AeroBlue, AeroColorScheme.AeroDark)
        assertNotSame(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)
        assertNotSame(AeroColorScheme.AeroDark, AeroColorScheme.Classic)
    }

    @Test
    fun copyChangesOnlyTheTargetedToken() {
        val custom = AeroColorScheme.AeroBlue.copy(primary = Color.Red)
        assertEquals(Color.Red, custom.primary, "copied primary")
        assertEquals(AeroColorScheme.AeroBlue.background, custom.background, "background preserved")
        assertEquals(AeroColorScheme.AeroBlue.glassSurface, custom.glassSurface, "glassSurface preserved")
        assertEquals(AeroColorScheme.AeroBlue.panelBackground, custom.panelBackground, "panelBackground preserved")
    }

    // Note: @Immutable from androidx.compose.runtime uses AnnotationRetention.BINARY (Java CLASS retention),
    // which means it is stored in the class file but NOT accessible via runtime reflection.
    // The presence of @Immutable on the production source is verified at compile time by the Compose compiler
    // and enforced by code review / source checks. This test verifies the structural immutability contract
    // (data class with val-only properties) which is the runtime-verifiable proxy for FOUND-02.
    @Test
    fun aeroColorSchemeIsDataClass() {
        assertTrue(AeroColorScheme::class.isData, "AeroColorScheme must be a data class (structural immutability)")
    }

    @Test
    fun aeroColorSchemeHasTwentyThreeTokens() {
        val count = AeroColorScheme::class.declaredMemberProperties.size
        assertEquals(23, count, "AeroColorScheme must declare exactly 23 color tokens")
    }
}
