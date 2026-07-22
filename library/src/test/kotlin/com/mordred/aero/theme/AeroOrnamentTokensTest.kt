package com.mordred.aero.theme

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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
}
