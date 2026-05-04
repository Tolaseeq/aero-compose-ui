package com.mordred.aero.components.pickers.internal.color

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AeroColorMathTest {

    // === Round-trip drift contract (PITFALL-15) ===

    @Test
    fun pureRedRoundTripPreservesHueWithinTolerance() {
        // Pure red: hue=0, sat=1, val=1
        val red = Color.hsv(0f, 1f, 1f)
        val (hue, _, _) = rgbToHsv(red.red, red.green, red.blue)
        assertTrue(
            abs(hue - 0f) < 0.001f,
            "hue drift on pure red round-trip; got hue=$hue"
        )
    }

    // === rgbToHsv behavior ===

    @Test
    fun pureRedHasHueZero() {
        val (h, s, v) = rgbToHsv(1f, 0f, 0f)
        assertTrue(abs(h - 0f) < 0.001f, "expected hue ~ 0; got $h")
        assertEquals(1f, s, "saturation")
        assertEquals(1f, v, "value")
    }

    @Test
    fun pureGreenHasHue120() {
        val (h, _, _) = rgbToHsv(0f, 1f, 0f)
        assertTrue(abs(h - 120f) < 0.001f, "expected hue ~ 120; got $h")
    }

    @Test
    fun pureBlueHasHue240() {
        val (h, _, _) = rgbToHsv(0f, 0f, 1f)
        assertTrue(abs(h - 240f) < 0.001f, "expected hue ~ 240; got $h")
    }

    @Test
    fun blackHasZeroHueSatAndValue() {
        val (h, s, v) = rgbToHsv(0f, 0f, 0f)
        assertEquals(0f, h)
        assertEquals(0f, s)
        assertEquals(0f, v)
    }

    @Test
    fun whiteHasZeroSaturationAndValueOne() {
        val (_, s, v) = rgbToHsv(1f, 1f, 1f)
        assertEquals(0f, s)
        assertEquals(1f, v)
    }

    // === hexToRgb behavior ===

    @Test
    fun hexToRgbAcceptsLeadingHashSixDigits() {
        assertEquals(Triple(255, 0, 0), hexToRgb("#FF0000"))
    }

    @Test
    fun hexToRgbAcceptsNoLeadingHash() {
        assertEquals(Triple(255, 0, 0), hexToRgb("FF0000"))
    }

    @Test
    fun hexToRgbExpandsThreeDigitForm() {
        assertEquals(Triple(255, 0, 0), hexToRgb("#F00"))
    }

    @Test
    fun hexToRgbDropsAlphaInEightDigitForm() {
        assertEquals(Triple(255, 0, 0), hexToRgb("#FF0000FF"))
    }

    @Test
    fun hexToRgbReturnsNullOnInvalidChars() {
        assertNull(hexToRgb("xyz"))
    }

    @Test
    fun hexToRgbReturnsNullOnWrongLength() {
        assertNull(hexToRgb("#FF"))
        assertNull(hexToRgb("#FFFF"))
    }

    // === hexToRgba behavior ===

    @Test
    fun hexToRgbaDefaultsAlphaTo255WhenAbsent() {
        val rgba = hexToRgba("#FF0000")
        assertNotNull(rgba)
        assertEquals(255, rgba[0])
        assertEquals(0,   rgba[1])
        assertEquals(0,   rgba[2])
        assertEquals(255, rgba[3])
    }

    @Test
    fun hexToRgbaParsesAlphaInEightDigitForm() {
        val rgba = hexToRgba("#FF000080")
        assertNotNull(rgba)
        assertEquals(255, rgba[0])
        assertEquals(0,   rgba[1])
        assertEquals(0,   rgba[2])
        assertEquals(128, rgba[3])
    }

    // === rgbToHex behavior ===

    @Test
    fun rgbToHexProducesUppercaseSixDigit() {
        assertEquals("FF0000", rgbToHex(255, 0, 0))
    }

    @Test
    fun rgbToHexAppendsAlphaWhenProvided() {
        assertEquals("FF000080", rgbToHex(255, 0, 0, alpha = 128))
    }
}
