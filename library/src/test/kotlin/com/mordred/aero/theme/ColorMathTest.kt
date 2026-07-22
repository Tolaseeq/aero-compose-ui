package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PRIM-01 verification. [Color.lighten]/[Color.darken] must RGB-mix toward White/Black
 * (never alpha-only) so they visibly affect [AeroColorScheme.Classic]'s fully-opaque tokens,
 * not just [AeroColorScheme.AeroBlue]/[AeroColorScheme.AeroDark]'s translucent ones.
 */
class ColorMathTest {

    @Test
    fun lightenByZeroReturnsInputUnchanged() {
        val input = Color(0xFF334455)
        assertEquals(input, input.lighten(0f), "lighten(0f) must be a no-op")
    }

    @Test
    fun darkenByZeroReturnsInputUnchanged() {
        val input = Color(0xFF334455)
        assertEquals(input, input.darken(0f), "darken(0f) must be a no-op")
    }

    @Test
    fun lightenByOneReachesWhitePreservingAlpha() {
        val input = Color(0xFF333333)
        val result = input.lighten(1f)
        assertEquals(Color.White.copy(alpha = input.alpha), result, "lighten(1f) must reach white")
    }

    @Test
    fun darkenByOneReachesBlackPreservingAlpha() {
        val input = Color(0xFF333333)
        val result = input.darken(1f)
        assertEquals(Color.Black.copy(alpha = input.alpha), result, "darken(1f) must reach black")
    }

    @Test
    fun midAmountLightenProducesValueStrictlyBetweenInputAndWhite() {
        // Opaque, like Classic's glassSurface = Color(0xFF333333) — proves this is RGB-mix,
        // not an alpha trick (alpha tricks are invisible on Classic's opaque tokens).
        val input = Color(0xFF333333)
        val result = input.lighten(0.5f)
        assertTrue(result.red > input.red && result.red < 1f, "red must move strictly toward white")
        assertTrue(result.green > input.green && result.green < 1f, "green must move strictly toward white")
        assertTrue(result.blue > input.blue && result.blue < 1f, "blue must move strictly toward white")
        assertEquals(input.alpha, result.alpha, "alpha must be preserved (RGB-mix, not alpha manipulation)")
    }

    @Test
    fun midAmountDarkenProducesValueStrictlyBetweenInputAndBlack() {
        val input = Color(0xFF333333)
        val result = input.darken(0.5f)
        assertTrue(result.red < input.red && result.red > 0f, "red must move strictly toward black")
        assertTrue(result.green < input.green && result.green > 0f, "green must move strictly toward black")
        assertTrue(result.blue < input.blue && result.blue > 0f, "blue must move strictly toward black")
        assertEquals(input.alpha, result.alpha, "alpha must be preserved")
    }

    @Test
    fun alphaIsAlwaysPreservedNeverManipulated() {
        val translucent = Color(0x30FFFFFF) // shaped like AeroBlue.glassSurface
        assertEquals(translucent.alpha, translucent.lighten(0.4f).alpha, "lighten must not touch alpha")
        assertEquals(translucent.alpha, translucent.darken(0.4f).alpha, "darken must not touch alpha")
    }
}
