package com.mordred.aero.components.pickers

import com.mordred.aero.components.pickers.internal.color.DefaultAeroSwatches
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PICK-05/06/07 unit coverage for the pure HSV<->Color<->HEX derivation layer that
 * backs `AeroColorPicker`. The UI wiring itself is exercised by the showcase; these
 * tests lock the no-drift state-machine contract (PITFALL-15) that the panel depends on.
 */
class AeroColorPickerTest {

    // === safeHsvColor never throws (NEW-PICK-01) ===

    @Test
    fun safeHsvColorDoesNotThrowOnHue360() {
        // Color.hsv requirePrecondition-throws on hue==360f without coercion; wrapper must not.
        val c = safeHsvColor(360f, 1f, 1f)
        assertTrue(c.red in 0f..1f)
    }

    @Test
    fun safeHsvColorClampsNegativeHueAndOutOfRangeChannels() {
        val c = safeHsvColor(-50f, 2f, -1f, 5f)
        assertEquals(1f, c.alpha, "alpha should clamp to 1f")
        assertTrue(c.red in 0f..1f && c.green in 0f..1f && c.blue in 0f..1f)
    }

    // === Round-trip drift gate (PITFALL-15) ===

    @Test
    fun pureRedHsvDerivesToFF0000() {
        assertEquals("FF0000", hsvToHex(0f, 1f, 1f, 1f, includeAlpha = false))
    }

    @Test
    fun saturationRoundTripDoesNotDriftFromFF0000() {
        // Start at pure red, drive saturation down to 0.5 then back to 1.0;
        // HSV is the single source of truth, so HEX must read "FF0000" again.
        val start = hsvToHex(0f, 1f, 1f, 1f, includeAlpha = false)
        assertEquals("FF0000", start)
        val midway = hsvToHex(0f, 0.5f, 1f, 1f, includeAlpha = false)
        assertTrue(midway != "FF0000", "sanity: 50% saturation should differ from pure red")
        val back = hsvToHex(0f, 1f, 1f, 1f, includeAlpha = false)
        assertEquals("FF0000", back, "round-trip drift: sat 1.0 -> 0.5 -> 1.0 must restore FF0000")
    }

    // === hexToHsv parsing ===

    @Test
    fun hexToHsvParsesPureRedWithinTolerance() {
        val hsv = hexToHsv("#FF0000")
        assertNotNull(hsv)
        assertTrue(abs(hsv[0] - 0f) < 0.001f, "hue ~ 0; got ${hsv[0]}")
        assertTrue(abs(hsv[1] - 1f) < 0.001f, "sat ~ 1; got ${hsv[1]}")
        assertTrue(abs(hsv[2] - 1f) < 0.001f, "value ~ 1; got ${hsv[2]}")
        assertTrue(abs(hsv[3] - 1f) < 0.001f, "alpha ~ 1; got ${hsv[3]}")
    }

    @Test
    fun hexToHsvAcceptsNoLeadingHash() {
        assertNotNull(hexToHsv("FF0000"))
    }

    @Test
    fun hexToHsvReturnsNullOnPartialInput() {
        assertNull(hexToHsv("#FF"), "3-char cleaned length must be rejected")
        assertNull(hexToHsv("#FFFF"), "4-char cleaned length must be rejected")
    }

    @Test
    fun hexToHsvParsesEightDigitAlpha() {
        val hsv = hexToHsv("#FF000080")
        assertNotNull(hsv)
        assertTrue(abs(hsv[3] - 128f / 255f) < 0.005f, "alpha ~ 0.502; got ${hsv[3]}")
    }

    // === alpha length contract ===

    @Test
    fun hsvToHexProducesSixCharsWhenAlphaExcluded() {
        assertEquals(6, hsvToHex(120f, 1f, 1f, 0.5f, includeAlpha = false).length)
    }

    @Test
    fun hsvToHexProducesEightCharsWhenAlphaIncluded() {
        assertEquals(8, hsvToHex(120f, 1f, 1f, 0.5f, includeAlpha = true).length)
    }

    // === default swatch palette ===

    @Test
    fun defaultSwatchesHasExactlySixteenColors() {
        assertEquals(16, DefaultAeroSwatches.size)
    }
}
