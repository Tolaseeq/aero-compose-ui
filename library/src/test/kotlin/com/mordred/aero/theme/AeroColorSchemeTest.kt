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
    fun aeroColorSchemeHasTwentyFourTokensAfterOrnamentOverrideAppend() {
        // PRIM-03: a trailing, defaulted ornamentOverride field was appended (23 -> 24).
        // The append itself is source-compatible (no existing constructor call needs edits);
        // only this structural-count assertion is updated to match.
        val count = AeroColorScheme::class.declaredMemberProperties.size
        assertEquals(24, count, "AeroColorScheme must declare 23 original color tokens + 1 ornamentOverride")
    }

    @Test
    fun ornamentOverrideDefaultsToNullOnAllPresets() {
        assertEquals(null, AeroColorScheme.AeroBlue.ornamentOverride, "AeroBlue.ornamentOverride")
        assertEquals(null, AeroColorScheme.AeroDark.ornamentOverride, "AeroDark.ornamentOverride")
        assertEquals(null, AeroColorScheme.Classic.ornamentOverride, "Classic.ornamentOverride")
    }

    @Test
    fun existingTwentyThreeArgConstructorCallStillCompilesAndDefaultsOverrideToNull() {
        // Mirrors the pre-Phase-16 23-field constructor shape verbatim (no ornamentOverride
        // arg) — proves PRIM-03 source-compatibility for pre-existing named-arg call sites.
        val scheme = AeroColorScheme(
            primary = Color(0xFF000000),
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF000000),
            onSecondary = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            background = Color(0xFF000000),
            onBackground = Color(0xFF000000),
            error = Color(0xFF000000),
            onError = Color(0xFF000000),
            cardBackground = Color(0xFF000000),
            borderDefault = Color(0xFF000000),
            borderSelected = Color(0xFF000000),
            labelText = Color(0xFF000000),
            glassSurface = Color(0xFF000000),
            glassBorder = Color(0xFF000000),
            glassHighlight = Color(0xFF000000),
            titleBarGradientStart = Color(0xFF000000),
            titleBarGradientEnd = Color(0xFF000000),
            titleBarText = Color(0xFF000000),
            buttonHover = Color(0xFF000000),
            closeButtonHover = Color(0xFF000000),
            panelBackground = Color(0xFF000000)
        )
        assertEquals(null, scheme.ornamentOverride, "trailing ornamentOverride must default to null")
    }

    @Test
    fun ornamentOverrideCanBeSetViaTrailingArgument() {
        val override = AeroOrnamentTokens.derive(AeroColorScheme.AeroBlue)
        val custom = AeroColorScheme.AeroBlue.copy(ornamentOverride = override)
        assertEquals(override, custom.ornamentOverride, "ornamentOverride must be settable via copy")
    }
}
