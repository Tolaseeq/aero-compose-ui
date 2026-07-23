package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for the three per-state
 * style transforms added in 17-02: [AeroSurfaceStyle.pressedRecess], [AeroSurfaceStyle.flattenDisabled],
 * [AeroSurfaceStyle.hoverLighten]. Each transform is a pure `AeroSurfaceStyle` data operation —
 * these tests assert on the returned field values directly, per RESEARCH.md's Validation
 * Architecture (pure state → style mapping, tested without composing anything).
 */
class AeroSurfaceStyleTransformsTest {

    private val testInnerShadow = Shadow(
        radius = 2.dp,
        color = Color.Black.copy(alpha = 0.35f),
    )

    @Test
    fun pressedRecessSwapsFillAndBevelDisablesGlossAndAttachesInnerShadow() {
        val rest = AeroSurfaceStyle.rest(AeroColorScheme.AeroBlue, cornerRadius = 4.dp)

        val pressed = rest.pressedRecess(testInnerShadow)

        assertEquals(rest.fillBottom, pressed.fillTop, "fillTop must become rest's fillBottom")
        assertEquals(rest.fillTop, pressed.fillBottom, "fillBottom must become rest's fillTop")
        assertEquals(rest.bevelShadow, pressed.bevelLight, "bevelLight must become rest's bevelShadow")
        assertEquals(rest.bevelLight, pressed.bevelShadow, "bevelShadow must become rest's bevelLight")
        assertEquals(0f, pressed.glossAlpha, "gloss must be fully disabled when pressed")
        assertNotNull(pressed.innerShadow, "pressed style must carry a non-null innerShadow")
        assertEquals(testInnerShadow, pressed.innerShadow)
    }

    @Test
    fun flattenDisabledCollapsesFillAndBevelZeroesGlossAndHalvesRim() {
        val rest = AeroSurfaceStyle.rest(AeroColorScheme.AeroBlue, cornerRadius = 4.dp)

        val disabled = rest.flattenDisabled(AeroColorScheme.AeroBlue)

        assertEquals(disabled.fillTop, disabled.fillBottom, "fill must collapse to one tone")
        assertEquals(disabled.bevelLight, disabled.bevelShadow, "bevel must collapse to one tone (no light/shadow split)")
        assertEquals(0f, disabled.glossAlpha, "gloss alpha must be fully disabled")
        assertEquals(0f, disabled.glossHeightFraction, "gloss height fraction must be fully disabled")
        assertApproxEquals(
            expected = rest.rimAlpha * 0.5f,
            actual = disabled.rimAlpha,
            message = "rim alpha must be halved",
        )
    }

    @Test
    fun hoverLightenBrightensBothFillStopsPreservingGlossBevelRim() {
        val rest = AeroSurfaceStyle.rest(AeroColorScheme.AeroBlue, cornerRadius = 4.dp)

        val hovered = rest.hoverLighten()

        assertTrue(luminance(hovered.fillTop) > luminance(rest.fillTop), "hovered fillTop must be brighter than rest")
        assertTrue(luminance(hovered.fillBottom) > luminance(rest.fillBottom), "hovered fillBottom must be brighter than rest")
        assertEquals(rest.glossAlpha, hovered.glossAlpha, "gloss must be unchanged by hover")
        assertEquals(rest.glossHeightFraction, hovered.glossHeightFraction, "gloss height must be unchanged by hover")
        assertEquals(rest.bevelLight, hovered.bevelLight, "bevel light must be unchanged by hover")
        assertEquals(rest.bevelShadow, hovered.bevelShadow, "bevel shadow must be unchanged by hover")
        assertEquals(rest.rimColor, hovered.rimColor, "rim color must be unchanged by hover")
        assertEquals(rest.rimAlpha, hovered.rimAlpha, "rim alpha must be unchanged by hover")
    }

    @Test
    fun classicSafetyNoTransformEverProducesAFullyTransparentFillChannel() {
        val rest = AeroSurfaceStyle.rest(AeroColorScheme.Classic, cornerRadius = 4.dp)

        val pressed = rest.pressedRecess(testInnerShadow)
        val disabled = rest.flattenDisabled(AeroColorScheme.Classic)
        val hovered = rest.hoverLighten()

        listOf(
            rest.fillTop, rest.fillBottom,
            pressed.fillTop, pressed.fillBottom,
            disabled.fillTop, disabled.fillBottom,
            hovered.fillTop, hovered.fillBottom,
        ).forEach { color ->
            assertTrue(color.alpha > 0f, "Classic-derived fill channel must never go fully transparent: $color")
        }
    }

    private fun luminance(color: Color): Float = (color.red + color.green + color.blue) / 3f

    private fun assertApproxEquals(expected: Float, actual: Float, message: String) {
        assertTrue(
            kotlin.math.abs(expected - actual) <= 0.0001f,
            "$message (expected=$expected, actual=$actual)",
        )
    }
}
