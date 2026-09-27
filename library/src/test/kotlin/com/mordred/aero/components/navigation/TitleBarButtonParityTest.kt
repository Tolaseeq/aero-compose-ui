package com.mordred.aero.components.navigation

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.internal.Square
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.verification.pixelMapsDiffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BTN-01 / D-02: pixel parity of the maximize button driven through the bridged
 * [MutableInteractionSource] (the one the native WM_NCHITTEST path feeds its hover / press
 * interactions into) against the same [TitleBarButton] driven by real Compose pointer input —
 * in all three built-in themes.
 *
 * Both buttons sit in one composition; the reference is driven by `performMouseInput`
 * (`moveTo(center)` for hover, `moveTo(center); press()` for press) exactly like
 * [com.mordred.aero.verification.VER07ButtonStateMatrixTest], while the bridged variant
 * receives `HoverInteraction.Enter` / `PressInteraction.Press` emitted into the shared source
 * via `runOnIdle` — the same objects the native side posts through
 * `AeroMaxButtonInteraction.interactionSource`. With `mainClock.autoAdvance = false` and one
 * identical clock advance for both variants, the captures must be pixel-identical: the bridged
 * button must not render one pixel differently from the Compose-driven one.
 *
 * Each state also asserts the state visibly paints (differs from that theme's own rest
 * capture), so a change that broke both variants identically could not pass vacuously.
 */
@OptIn(ExperimentalTestApi::class)
class TitleBarButtonParityTest {

    private enum class ParityState { REST, HOVER, PRESS }

    // ------------------------------------------------------------------
    // Hover — all three schemes.
    // ------------------------------------------------------------------

    @Test
    fun hoverParityAeroBlue() = runParity("AeroBlue/hover", AeroColorScheme.AeroBlue, ParityState.HOVER)

    @Test
    fun hoverParityAeroDark() = runParity("AeroDark/hover", AeroColorScheme.AeroDark, ParityState.HOVER)

    @Test
    fun hoverParityClassic() = runParity("Classic/hover", AeroColorScheme.Classic, ParityState.HOVER)

    // ------------------------------------------------------------------
    // Press — all three schemes.
    // ------------------------------------------------------------------

    @Test
    fun pressParityAeroBlue() = runParity("AeroBlue/press", AeroColorScheme.AeroBlue, ParityState.PRESS)

    @Test
    fun pressParityAeroDark() = runParity("AeroDark/press", AeroColorScheme.AeroDark, ParityState.PRESS)

    @Test
    fun pressParityClassic() = runParity("Classic/press", AeroColorScheme.Classic, ParityState.PRESS)

    // ------------------------------------------------------------------
    // Rest (control) — all three schemes.
    // ------------------------------------------------------------------

    @Test
    fun restParityAeroBlue() = runParity("AeroBlue/rest", AeroColorScheme.AeroBlue, ParityState.REST)

    @Test
    fun restParityAeroDark() = runParity("AeroDark/rest", AeroColorScheme.AeroDark, ParityState.REST)

    @Test
    fun restParityClassic() = runParity("Classic/rest", AeroColorScheme.Classic, ParityState.REST)

    private fun runParity(label: String, scheme: AeroColorScheme, state: ParityState) = runComposeUiTest {
        mainClock.autoAdvance = false
        val bridgedSource = MutableInteractionSource()

        setContent {
            AeroTheme(colorScheme = scheme) {
                Row {
                    // Reference: own remembered source, real Compose pointer input.
                    TitleBarButton(
                        icon = AeroIcons.Square,
                        hoverColor = scheme.buttonHover,
                        contentDescription = "Maximize window",
                        onClick = {},
                        modifier = Modifier.testTag("ref"),
                    )
                    // Bridged: the shared source a natively-fed caller passes in.
                    TitleBarButton(
                        icon = AeroIcons.Square,
                        hoverColor = scheme.buttonHover,
                        contentDescription = "Maximize window",
                        onClick = {},
                        modifier = Modifier.testTag("bridged"),
                        interactionSource = bridgedSource,
                    )
                }
            }
        }
        waitForIdle()

        fun capture(tag: String): PixelMap = onNodeWithTag(tag).captureToImage().toPixelMap()

        val restRef = capture("ref")
        val restBridged = capture("bridged")
        assertFalse(
            pixelMapsDiffer(restRef, restBridged),
            "$label rest: the two variants must start pixel-identical",
        )

        when (state) {
            ParityState.REST -> {
                // Control only — nothing further driven.
            }
            ParityState.HOVER -> {
                onNodeWithTag("ref").performMouseInput { moveTo(center) }
                runOnIdle { bridgedSource.tryEmit(HoverInteraction.Enter()) }
                // With autoAdvance disabled a recomposition only runs once a frame is clocked
                // through, so both variants get one identical advance before the capture.
                mainClock.advanceTimeBy(100)
                waitForIdle()
                val hoverRef = capture("ref")
                val hoverBridged = capture("bridged")
                assertTrue(
                    pixelMapsDiffer(restRef, hoverRef),
                    "$label hover: the real Compose hover must visibly paint (non-vacuous control)",
                )
                assertFalse(
                    pixelMapsDiffer(hoverRef, hoverBridged),
                    "$label hover: the bridged button must render pixel-identically to the Compose-driven one",
                )
            }
            ParityState.PRESS -> {
                val bridgedSize = onNodeWithTag("bridged").fetchSemanticsNode().size
                val bridgedCenter = Offset(bridgedSize.width / 2f, bridgedSize.height / 2f)
                onNodeWithTag("ref").performMouseInput { moveTo(center); press() }
                runOnIdle {
                    bridgedSource.tryEmit(HoverInteraction.Enter())
                    bridgedSource.tryEmit(PressInteraction.Press(bridgedCenter))
                }
                // One identical clock advance for both variants: any time-driven visual (the
                // shared indication) settles to the same frame in both captures.
                mainClock.advanceTimeBy(500)
                waitForIdle()
                val pressRef = capture("ref")
                val pressBridged = capture("bridged")
                assertTrue(
                    pixelMapsDiffer(restRef, pressRef),
                    "$label press: the real Compose press must visibly paint (non-vacuous control)",
                )
                assertFalse(
                    pixelMapsDiffer(pressRef, pressBridged),
                    "$label press: the bridged button must render pixel-identically to the Compose-driven one",
                )
            }
        }
    }
}
