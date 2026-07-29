package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.mordred.aero.components.buttons.resolveButtonStyle
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
    fun aeroColorSchemeHasTwentySixTokensAfterLabelPolarityTokensAppend() {
        // PRIM-03/SHW-16/VER-06: a trailing, defaulted ornamentOverride field was appended
        // (23 -> 24, Phase 16); labelOnFilledSurface/labelOnOutlinedSurface were appended next,
        // the same trailing-and-defaulted way (24 -> 26, 20-09). Both appends are
        // source-compatible (no existing constructor call needs edits); only this structural-count
        // assertion is updated to match.
        val count = AeroColorScheme::class.declaredMemberProperties.size
        assertEquals(
            26,
            count,
            "AeroColorScheme must declare 23 original color tokens + ornamentOverride + " +
                "labelOnFilledSurface + labelOnOutlinedSurface"
        )
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

    // ---- SHW-16/VER-06 (20-09): labelOnFilledSurface/labelOnOutlinedSurface polarity tokens. ----

    @Test
    fun builtInPresetsCarryTheDecidedPolarityTable() {
        // The maintainer's 2026-07-29 decision table (20-09, revised same day): AeroBlue/AeroDark's
        // light `primary` would make a surface-polarity split want a dark label on opaque fills and
        // a light one on their near-transparent outlined fill — that split was measured, shown to the
        // maintainer running live, and REJECTED on appearance in favor of white on both polarities in
        // both schemes, matching Classic, for cross-theme visual coherence. Classic's dark `primary`
        // means both polarities coincide on white regardless, unchanged from its historical
        // "white everywhere". See AeroColorScheme.kt's AeroBlue/AeroDark KDoc comments and
        // AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation for the
        // accepted sub-floor contrast cost of this choice.
        assertEquals(Color.White, AeroColorScheme.AeroBlue.labelOnFilledSurface, "AeroBlue.labelOnFilledSurface")
        assertEquals(Color.White, AeroColorScheme.AeroBlue.labelOnOutlinedSurface, "AeroBlue.labelOnOutlinedSurface")
        assertEquals(Color.White, AeroColorScheme.AeroDark.labelOnFilledSurface, "AeroDark.labelOnFilledSurface")
        assertEquals(Color.White, AeroColorScheme.AeroDark.labelOnOutlinedSurface, "AeroDark.labelOnOutlinedSurface")
        assertEquals(Color.White, AeroColorScheme.Classic.labelOnFilledSurface, "Classic.labelOnFilledSurface")
        assertEquals(Color.White, AeroColorScheme.Classic.labelOnOutlinedSurface, "Classic.labelOnOutlinedSurface")
    }

    @Test
    fun existingTwentyFourArgConstructorCallStillCompilesAndDefaultsLabelTokens() {
        // Mirrors existingTwentyThreeArgConstructorCallStillCompilesAndDefaultsOverrideToNull's
        // source-compatibility proof, extended to the 24-arg (post-ornamentOverride) shape: a call
        // that omits BOTH new trailing tokens must still compile and resolve non-null, sensible
        // fallbacks rather than throwing or defaulting to an invisible/transparent colour.
        // primary/background are unambiguous extremes (not AeroBlue's own borderline-luminance
        // values) so this test exercises the fallback heuristic itself, not a coincidence of a
        // real preset's exact luminance sitting close to the 0.5 split.
        val scheme = AeroColorScheme(
            primary = Color.White,
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF000000),
            onSecondary = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            background = Color.Black,
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
            panelBackground = Color(0xFF000000),
            ornamentOverride = null,
        )
        // primary is white (luminance 1.0) -> fallback picks the dark candidate;
        // background is black (luminance 0.0) -> fallback picks the light candidate.
        assertEquals(Color.Black, scheme.labelOnFilledSurface, "fallback labelOnFilledSurface for a light primary")
        assertEquals(Color.White, scheme.labelOnOutlinedSurface, "fallback labelOnOutlinedSurface for a dark background")
    }

    @Test
    fun copyOmittingBothLabelTokensStillResolvesSensiblyRatherThanThrowing() {
        // AeroColorScheme.copy() re-supplies every field from the receiver by default, so this
        // proves the FIELD ITSELF never throws/renders invisible when a caller builds a scheme
        // through copy() without touching label tokens (the fallback expression only runs when a
        // constructor call omits the argument entirely, e.g. this class's own 24-arg test above).
        val custom = AeroColorScheme.Classic.copy(primary = Color(0xFF123456))
        assertEquals(
            AeroColorScheme.Classic.labelOnFilledSurface, custom.labelOnFilledSurface,
            "copy() must preserve labelOnFilledSurface when not explicitly overridden"
        )
        assertEquals(
            AeroColorScheme.Classic.labelOnOutlinedSurface, custom.labelOnOutlinedSurface,
            "copy() must preserve labelOnOutlinedSurface when not explicitly overridden"
        )
    }

    // ---- WR-04 (20-REVIEW.md addendum 2): labelOnFilledSurface's fallback must evaluate the
    // actually-painted (darkened) fill, worst-case across both stops -- not raw, un-darkened
    // primary. D-08 fail-then-pass proof: RED half documents the retired heuristic's failure on a
    // plausible custom scheme, GREEN half proves the fixed fallback clears the floor on that exact
    // case via the real resolveButtonStyle output. ----

    @Test
    fun theOldRawPrimaryLuminanceSplitFailedTheFloorForAPlausibleCustomScheme() {
        // RED half: reproduces WR-04's own #BABABA example. The retired default expression was
        // `defaultLabelColorForSurface(primary)` -- a plain luminance split against raw, un-darkened
        // `primary`, a colour no on-fill label is ever actually painted against (both
        // resolveButtonStyle/resolveSegmentStyle always darken `primary` first). #BABABA's raw
        // luminance (0.491) sits just under the 0.5 split, so that heuristic picked White -- against
        // the REAL rest fill (both stops), White measures below the 4.5:1 floor.
        val customPrimary = Color(0xFFBABABA)
        val background = AeroColorScheme.AeroBlue.background
        val retiredHeuristicPick = if (customPrimary.luminance() > 0.5f) Color.Black else Color.White
        assertEquals(
            Color.White, retiredHeuristicPick,
            "sanity: raw-luminance split on #BABABA (0.491) must pick White -- if this changes, " +
                "the RED half no longer reproduces WR-04's documented case"
        )

        val restFill = resolveButtonStyle(
            AeroColorScheme.AeroBlue.copy(primary = customPrimary),
            outlined = false, hovered = false, pressed = false, focused = false, enabled = true,
        )
        val topRatio = contrastRatio(retiredHeuristicPick, restFill.fillTop.compositeOver(background))
        val bottomRatio = contrastRatio(retiredHeuristicPick, restFill.fillBottom.compositeOver(background))
        assertTrue(
            topRatio < 4.5f,
            "Fixture sanity check failed: the retired heuristic's White pick measured $topRatio on " +
                "the real top-stop fill for #BABABA, expected BELOW 4.5:1 -- if this fails, the " +
                "historical defect this RED half documents no longer reproduces"
        )
        assertTrue(
            bottomRatio < 4.5f,
            "Fixture sanity check failed: the retired heuristic's White pick measured $bottomRatio " +
                "on the real bottom-stop fill for #BABABA, expected BELOW 4.5:1 -- if this fails, " +
                "the historical defect this RED half documents no longer reproduces"
        )
    }

    @Test
    fun defaultLabelOnFilledSurfaceClearsTheFloorForThePreviouslyFailingCustomScheme() {
        // GREEN half: a custom scheme built via the full positional constructor with the SAME
        // #BABABA primary as the RED test above, omitting BOTH label tokens so the real,
        // production defaultLabelColorForOpaqueFill fallback resolves labelOnFilledSurface. Proves
        // the fix end-to-end against the real resolveButtonStyle output, on both stops.
        val customPrimary = Color(0xFFBABABA)
        val background = Color(0xFF0D1B2A)
        val scheme = AeroColorScheme(
            primary = customPrimary,
            onPrimary = Color(0xFF000000),
            secondary = Color(0xFF000000),
            onSecondary = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            background = background,
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
            panelBackground = Color(0xFF000000),
            ornamentOverride = null,
        )

        assertEquals(
            Color.Black, scheme.labelOnFilledSurface,
            "worst-case-across-both-stops selection must pick Black for #BABABA -- the opposite of " +
                "the retired raw-luminance heuristic's White pick (see the RED half above)"
        )

        val restFill = resolveButtonStyle(scheme, outlined = false, hovered = false, pressed = false, focused = false, enabled = true)
        val topRatio = contrastRatio(scheme.labelOnFilledSurface, restFill.fillTop.compositeOver(background))
        val bottomRatio = contrastRatio(scheme.labelOnFilledSurface, restFill.fillBottom.compositeOver(background))
        assertTrue(
            topRatio >= 4.5f,
            "labelOnFilledSurface fallback top-stop contrast $topRatio must clear the 4.5:1 floor " +
                "for the #BABABA custom scheme (WR-04)"
        )
        assertTrue(
            bottomRatio >= 4.5f,
            "labelOnFilledSurface fallback bottom-stop contrast $bottomRatio must clear the 4.5:1 " +
                "floor for the #BABABA custom scheme (WR-04)"
        )
    }

    /**
     * Independently-written WCAG 2.x contrast ratio (D-13 convention, matching every other test
     * file in this codebase) -- never imports the production `contrastRatio` this fallback itself
     * uses, so a wrong production formula cannot certify itself here.
     */
    private fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance()
        val l2 = background.luminance()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
