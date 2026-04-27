package com.mordred.aero.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AeroThemeTest {

    /**
     * FOUND-01 default fallback — Pitfall 5 says staticCompositionLocalOf MUST default to
     * a sensible preset (NOT error()). We verify by invoking the defaultFactory directly.
     * Visual confirmation that AeroTheme switches schemes at runtime is performed manually
     * in the showcase (VALIDATION.md row 1-11).
     */
    @Test
    fun localAeroColorsDefaultsToAeroBlue() {
        val factory = LocalAeroColors.defaultFactory
        assertNotNull(factory, "LocalAeroColors must declare a defaultFactory (no error())")
        assertEquals(AeroColorScheme.AeroBlue, factory.invoke(), "default must be AeroBlue")
    }

    @Test
    fun localAeroTypographyDefaultsToDefaultInstance() {
        val factory = LocalAeroTypography.defaultFactory
        assertNotNull(factory, "LocalAeroTypography must declare a defaultFactory")
        assertEquals(AeroTypography(), factory.invoke(), "default must be AeroTypography()")
    }

    /**
     * Compile-time reachability — proves the package exposes AeroColorScheme and AeroTypography.
     * If imports break (e.g., Plan 02 used the wrong package), this test won't compile.
     */
    @Test
    fun publicApiSurfaceIsImportable() {
        val scheme: AeroColorScheme = AeroColorScheme.AeroBlue
        val typo: AeroTypography = AeroTypography()
        assertEquals(AeroColorScheme.AeroBlue, scheme)
        assertEquals(AeroTypography(), typo)
    }
}
