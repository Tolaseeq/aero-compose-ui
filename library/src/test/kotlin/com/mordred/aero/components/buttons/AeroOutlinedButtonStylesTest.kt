package com.mordred.aero.components.buttons

import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun outlinedEqualsOutlinedStyleAppliedToFilledForEveryState() {
        schemes.forEach { colors ->
            states.forEach { state ->
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

                assertEquals(
                    filled.outlinedStyle(),
                    outlined,
                    "outlined must equal outlinedStyle(filled) for state=${state.label}, colors=$colors",
                )
            }
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
        assertEquals(0.65f, outlinedRest.rimAlpha, 0.0001f, "rimAlpha must be ~0.65 (17-05 sign-off gap-fix)")
        assertEquals(rest.bevelLight, outlinedRest.bevelLight, "bevelLight must be unchanged")
        assertEquals(rest.bevelShadow, outlinedRest.bevelShadow, "bevelShadow must be unchanged")
        assertEquals(4.dp, outlinedRest.cornerRadius, "cornerRadius must remain 4.dp")
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
