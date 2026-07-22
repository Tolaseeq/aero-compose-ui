package com.mordred.aero.theme

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PRIM-02 verification. [AeroOrnamentTokens.derive] must be non-null and theme-sensitive for
 * all three built-in [AeroColorScheme] presets — including [AeroColorScheme.Classic], whose
 * base tokens are fully opaque and would silently fail to derive distinct ornaments under an
 * alpha-only implementation (see [ColorMathTest]).
 */
class AeroOrnamentTokensTest {

    @Test
    fun deriveReturnsNonNullTokensForAllThreePresets() {
        assertNotNull(AeroOrnamentTokens.derive(AeroColorScheme.AeroBlue))
        assertNotNull(AeroOrnamentTokens.derive(AeroColorScheme.AeroDark))
        assertNotNull(AeroOrnamentTokens.derive(AeroColorScheme.Classic))
    }

    @Test
    fun deriveIsThemeSensitiveAeroBlueVsAeroDark() {
        val blue = AeroOrnamentTokens.derive(AeroColorScheme.AeroBlue)
        val dark = AeroOrnamentTokens.derive(AeroColorScheme.AeroDark)
        assertNotEquals(blue, dark, "derive(AeroBlue) must differ from derive(AeroDark)")
    }

    @Test
    fun deriveIsThemeSensitiveAeroBlueVsClassic() {
        val blue = AeroOrnamentTokens.derive(AeroColorScheme.AeroBlue)
        val classic = AeroOrnamentTokens.derive(AeroColorScheme.Classic)
        assertNotEquals(
            blue,
            classic,
            "derive(AeroBlue) must differ from derive(Classic) even though Classic's base tokens are opaque"
        )
    }

    @Test
    fun deriveIsThemeSensitiveAeroDarkVsClassic() {
        val dark = AeroOrnamentTokens.derive(AeroColorScheme.AeroDark)
        val classic = AeroOrnamentTokens.derive(AeroColorScheme.Classic)
        assertNotEquals(dark, classic, "derive(AeroDark) must differ from derive(Classic)")
    }

    @Test
    fun deriveOnClassicProducesTokensDistinctFromItsOwnOpaqueBaseTokens() {
        // Classic's glassHighlight is a fully opaque literal (Color(0xFF3A3A3A)); an
        // alpha-only derivation would be a silent no-op here (PRIM-02 anti-pattern). RGB-mix
        // must actually move the channel values away from the raw base token.
        val tokens = AeroOrnamentTokens.derive(AeroColorScheme.Classic)
        assertNotEquals(
            AeroColorScheme.Classic.glassHighlight,
            tokens.glossHighlight,
            "glossHighlight must differ from Classic's raw glassHighlight token"
        )
    }

    /**
     * Regression guard (16-05 UAT gap-fix): `hoverGlow` must read as a distinctly brighter halo
     * than the surface fill it wraps, on all three presets — not merely "not equal" but
     * perceptibly lighter, since `aeroGlowRing`'s inner stroke is drawn directly against
     * `fillSplitTop`. A prior derivation (`primary.lighten(0.30f)` vs. `fillSplitTop`'s
     * `primary.lighten(0.18f)`) was "not equal" yet visually indistinguishable from the surface.
     *
     * Threshold calibrated to `hoverGlow = primary.lighten(0.45f)` (16-05 second UAT gap-fix:
     * the 0.75f derivation was meaningfully brighter but read as a harsh near-white rim). The
     * worst-case preset (AeroDark, whose `primary` is already close to white) still clears this
     * bar with margin — 0.04f remains well above the ~0.01-0.02f perceptual JND, so the guard
     * still fails a glow that has regressed back to blending into its surface.
     */
    @Test
    fun hoverGlowIsMeaningfullyBrighterThanFillSplitTopOnAllThreePresets() {
        val presets = listOf(
            AeroColorScheme.AeroBlue,
            AeroColorScheme.AeroDark,
            AeroColorScheme.Classic,
        )
        presets.forEach { scheme ->
            val tokens = AeroOrnamentTokens.derive(scheme)
            val glowLuminance = luminance(tokens.hoverGlow)
            val fillLuminance = luminance(tokens.fillSplitTop)
            assertTrue(
                glowLuminance - fillLuminance > 0.04f,
                "hoverGlow (luminance=$glowLuminance) must be meaningfully brighter than " +
                    "fillSplitTop (luminance=$fillLuminance) for $scheme so the glow ring reads " +
                    "as a distinct halo rather than blending into the surface it wraps"
            )
        }
    }

    private fun luminance(color: androidx.compose.ui.graphics.Color): Float =
        (color.red + color.green + color.blue) / 3f
}
