package com.mordred.aero.components.buttons

import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.darken
import com.mordred.aero.theme.flattenDisabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) proving VBTN-05/06:
 * [resolveButtonStyle] with `outlined = true` for a given state equals
 * [AeroSurfaceStyle.outlinedStyle] applied to the filled resolution of that SAME state, for every
 * one of the five states — the outlined variant is provably "the filled style, transformed" by
 * one fixed delta, never a second independently-authored painter (17-RESEARCH.md Pattern 5).
 * Exercised over both [AeroColorScheme.AeroBlue] (translucent tokens) and [AeroColorScheme.Classic]
 * (fully-opaque tokens) per RESEARCH.md's Validation Architecture.
 */
class AeroOutlinedButtonStylesTest {

    private data class StateCase(
        val label: String,
        val hovered: Boolean,
        val pressed: Boolean,
        val focused: Boolean,
        val enabled: Boolean,
    )

    private val states = listOf(
        StateCase("rest", hovered = false, pressed = false, focused = false, enabled = true),
        StateCase("hover", hovered = true, pressed = false, focused = false, enabled = true),
        StateCase("press", hovered = false, pressed = true, focused = false, enabled = true),
        StateCase("focus", hovered = false, pressed = false, focused = true, enabled = true),
        StateCase("disabled", hovered = false, pressed = false, focused = false, enabled = false),
    )

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    /**
     * Reconstructs the BUTTON-scoped rest style [resolveButtonStyle] resolves internally (17-05
     * ROUND-2 sign-off gap-fix, FIX A + FIX B) — see [AeroButtonStylesTest.expectedButtonRest]'s
     * twin. Duplicated here (not shared across test files) because each file's magic-number
     * assertions are meant to independently pin the UI-SPEC contract, matching this file's
     * existing style (e.g. [restOutlinedMatchesTheUiSpecFixedDeltaRatios] hardcodes 0.15f).
     */
    private fun expectedButtonRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).copy(
            fillTop = colors.primary.darken(0.20f),
            fillBottom = colors.primary.darken(0.36f),
            rimAlpha = minOf(colors.glassBorder.alpha, 0.45f),
        )

    /** Theme-aware outlined rim alpha (17-05 ROUND-2 sign-off gap-fix, FIX A) — mirrors [resolveButtonStyle]'s formula. */
    private fun expectedOutlinedRimAlpha(colors: AeroColorScheme): Float =
        minOf(colors.glassBorder.alpha * 1.7f, 0.70f)

    /**
     * Excludes the "disabled" state (17-05 sign-off gap-fix): [resolveButtonStyle] no longer
     * applies `.outlinedStyle()` to the already-flattened disabled style — it applies
     * `.outlinedStyle()` FIRST, then `.flattenDisabled()` LAST, so disabled-outlined is provably
     * NOT `outlinedStyle(filled)` anymore (that was exactly the bug: flattenDisabled's
     * rimAlpha-halving/neutral-fill collapse was getting clobbered by outlinedStyle running after
     * it). See [disabledOutlinedAppliesOutlinedStyleThenFlattenDisabledInThatOrder] for the
     * disabled-specific invariant this test carves out.
     */
    private val enabledStates = states.filter { it.enabled }

    @Test
    fun outlinedEqualsOutlinedStyleAppliedToFilledForEveryEnabledState() {
        schemes.forEach { colors ->
            enabledStates.forEach { state ->
                val filled = resolveButtonStyle(
                    colors = colors,
                    outlined = false,
                    hovered = state.hovered,
                    pressed = state.pressed,
                    focused = state.focused,
                    enabled = state.enabled,
                )
                val outlined = resolveButtonStyle(
                    colors = colors,
                    outlined = true,
                    hovered = state.hovered,
                    pressed = state.pressed,
                    focused = state.focused,
                    enabled = state.enabled,
                )

                // outlinedStyle() alone no longer sets rimAlpha (17-05 ROUND-2, FIX A) — the
                // theme-aware outlined rim is overridden by resolveButtonStyle on top of it.
                assertEquals(
                    filled.outlinedStyle().copy(rimAlpha = expectedOutlinedRimAlpha(colors)),
                    outlined,
                    "outlined must equal outlinedStyle(filled) with the theme-aware outlined rim " +
                        "overridden on top, for state=${state.label}, colors=$colors",
                )
            }
        }
    }

    /**
     * FIX 3 (17-05 sign-off gap-fix): proves the disabled-outlined ordering is exactly
     * `.outlinedStyle()` applied to rest, THEN `.flattenDisabled()` applied last — the terminal
     * transform. This is the ordering that makes disabled-outlined dimmer than active-outlined
     * (see [disabledOutlinedRimIsStrictlyDimmerThanActiveOutlinedRim] below), unlike the old
     * (buggy) `flattenDisabled(...).outlinedStyle()` ordering this test guards against regressing to.
     */
    @Test
    fun disabledOutlinedAppliesOutlinedStyleThenFlattenDisabledInThatOrder() {
        schemes.forEach { colors ->
            val rest = expectedButtonRest(colors)
            val outlinedRimAlpha = expectedOutlinedRimAlpha(colors)
            val expected = rest.outlinedStyle().copy(rimAlpha = outlinedRimAlpha).flattenDisabled(colors)
            val staleOrdering = rest.flattenDisabled(colors).outlinedStyle()
            val actual = resolveButtonStyle(
                colors = colors, outlined = true,
                hovered = false, pressed = false, focused = false, enabled = false,
            )

            assertEquals(
                expected,
                actual,
                "disabled+outlined must equal outlinedStyle() (with the theme-aware outlined rim " +
                    "overridden on top), THEN flattenDisabled() applied last, for $colors",
            )
            assertNotEquals(
                staleOrdering,
                actual,
                "disabled+outlined must NOT equal the old (buggy) flattenDisabled(...).outlinedStyle() ordering for $colors",
            )
        }
    }

    /**
     * FIX 3 (17-05 sign-off gap-fix): the operator's finding — disabled outlined was "practically
     * indistinguishable from the active outlined button" — must no longer reproduce. Disabled
     * outlined's rim must be strictly dimmer than active/rest outlined's rim so the two states
     * are visually distinguishable.
     */
    @Test
    fun disabledOutlinedRimIsStrictlyDimmerThanActiveOutlinedRim() {
        schemes.forEach { colors ->
            val activeOutlined = resolveButtonStyle(
                colors = colors, outlined = true,
                hovered = false, pressed = false, focused = false, enabled = true,
            )
            val disabledOutlined = resolveButtonStyle(
                colors = colors, outlined = true,
                hovered = false, pressed = false, focused = false, enabled = false,
            )

            assertTrue(
                disabledOutlined.rimAlpha < activeOutlined.rimAlpha,
                "disabled outlined rimAlpha (${disabledOutlined.rimAlpha}) must be strictly less " +
                    "than active outlined rimAlpha (${activeOutlined.rimAlpha}) for $colors so the " +
                    "two states are distinguishable",
            )
        }
    }

    /**
     * FIX 3 (17-05 sign-off gap-fix): disabled outlined's fill must collapse to one neutral,
     * surface-blended tone (the [flattenDisabled] contract, retargeted from borderDefault to
     * surface by 17-05 ROUND-2's FIX C) — not stay the button's accent fill merely dimmed by the
     * outlined alpha multiplier.
     */
    @Test
    fun disabledOutlinedFillIsNeutralCollapseNotAccentTone() {
        schemes.forEach { colors ->
            val rest = expectedButtonRest(colors)
            val disabledOutlined = resolveButtonStyle(
                colors = colors, outlined = true,
                hovered = false, pressed = false, focused = false, enabled = false,
            )

            assertEquals(
                disabledOutlined.fillTop,
                disabledOutlined.fillBottom,
                "disabled outlined fill must collapse to a single neutral tone for $colors",
            )
            assertNotEquals(
                rest.fillTop,
                disabledOutlined.fillTop,
                "disabled outlined fill must differ from the active accent fillTop for $colors — " +
                    "it must be the neutral collapse, not the accent tone merely dimmed",
            )
        }
    }

    @Test
    fun restOutlinedMatchesTheUiSpecFixedDeltaRatios() {
        val colors = AeroColorScheme.AeroBlue
        val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)
        val outlinedRest = rest.outlinedStyle()

        // Tolerance widened to 1/255 (Color's internal 8-bit-per-channel packed representation
        // quantizes alpha on `.copy(alpha = ...)`, e.g. 0.15f round-trips as 0.14901961f) rather
        // than a tight epsilon that would false-fail on that quantization, not a logic bug.
        val colorChannelTolerance = 1f / 255f
        assertEquals(rest.fillTop.alpha * 0.15f, outlinedRest.fillTop.alpha, colorChannelTolerance, "fillTop alpha must be filled alpha x0.15")
        assertEquals(rest.fillBottom.alpha * 0.15f, outlinedRest.fillBottom.alpha, colorChannelTolerance, "fillBottom alpha must be filled alpha x0.15")
        assertEquals(0.15f, outlinedRest.glossAlpha, 0.0001f, "rest glossAlpha must map 0.22 -> ~0.15")
        // rimAlpha is deliberately left UNTOUCHED by outlinedStyle() alone (17-05 ROUND-2
        // sign-off gap-fix, FIX A) — resolveButtonStyle sets the theme-aware value on top, since
        // this pure style-to-style transform has no access to `colors`.
        assertEquals(
            rest.rimAlpha,
            outlinedRest.rimAlpha,
            "outlinedStyle() alone must leave rimAlpha untouched — resolveButtonStyle overrides it with the theme-aware value",
        )
        assertEquals(rest.bevelLight, outlinedRest.bevelLight, "bevelLight must be unchanged")
        assertEquals(rest.bevelShadow, outlinedRest.bevelShadow, "bevelShadow must be unchanged")
        assertEquals(4.dp, outlinedRest.cornerRadius, "cornerRadius must remain 4.dp")
    }

    /**
     * FIX A (17-05 ROUND-2 sign-off gap-fix): outlined keeps a stronger contour than filled for
     * the SAME theme — it has almost no fill to lean on instead, so its resolved rim must read
     * brighter than the filled button's rim, across all three real presets.
     */
    @Test
    fun outlinedRimAlphaExceedsFilledRimAlphaForSameTheme() {
        listOf(AeroColorScheme.AeroBlue, AeroColorScheme.AeroDark, AeroColorScheme.Classic).forEach { colors ->
            val filled = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = false, enabled = true,
            )
            val outlined = resolveButtonStyle(
                colors = colors, outlined = true,
                hovered = false, pressed = false, focused = false, enabled = true,
            )

            assertTrue(
                outlined.rimAlpha > filled.rimAlpha,
                "outlined rimAlpha (${outlined.rimAlpha}) must exceed filled rimAlpha (${filled.rimAlpha}) for $colors",
            )
        }
    }

    @Test
    fun pressedAndDisabledZeroedGlossStaysZeroAfterOutlinedTransform() {
        // Sanity-check the proportional gloss scale: an already-zeroed gloss (pressed/disabled)
        // must not jump back up when the outlined transform is applied on top.
        val colors = AeroColorScheme.AeroBlue

        val pressedFilled = resolveButtonStyle(
            colors = colors, outlined = false,
            hovered = false, pressed = true, focused = false, enabled = true,
        )
        val disabledFilled = resolveButtonStyle(
            colors = colors, outlined = false,
            hovered = false, pressed = false, focused = false, enabled = false,
        )

        assertEquals(0f, pressedFilled.outlinedStyle().glossAlpha, "pressed outlined gloss must stay zero")
        assertEquals(0f, disabledFilled.outlinedStyle().glossAlpha, "disabled outlined gloss must stay zero")
    }
}
