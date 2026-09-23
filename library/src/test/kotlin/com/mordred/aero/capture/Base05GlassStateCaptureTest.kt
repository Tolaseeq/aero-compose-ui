package com.mordred.aero.capture

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.list.AeroListItem
import com.mordred.aero.components.range.AeroRangeSlider
import com.mordred.aero.components.range.AeroSlider
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.components.selection.AeroSwitch
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.verification.pixelMapsDiffer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * BASE-05: permanent hover / press / keyboard-focus / drag state-capture tests for the seven v3.0
 * glass components, in all three built-in themes (21 = 7 components x 3 themes, one method per
 * combination, mirroring [com.mordred.aero.verification.VER07ButtonStateMatrixTest]'s per-scheme
 * shape).
 *
 * Real input only — [performMouseInput]/[performKeyInput], never a hand-built
 * [androidx.compose.foundation.interaction.MutableInteractionSource] (same rule as VER07). Every
 * per-state assertion compares that state's capture against the SAME (component, theme)
 * combination's own `default` capture via the shared [pixelMapsDiffer] (imported from
 * [com.mordred.aero.verification], reused verbatim — that file carries its own D-08 falsifiability
 * proof; it is not re-derived here).
 *
 * Focus is driven by real Tab-key traversal first ([focusViaTabThenFallback]): focus is requested
 * on a sibling `before` node, then `Key.Tab` is pressed. If the resulting capture is byte-identical
 * to `default` (Tab traversal did not visibly reach the component in this harness), a direct
 * `requestFocus()` on the component itself (VER07's keyboard-acquired path) is tried as a fallback
 * where one is available. [AeroRangeSlider] has no keyboard focus tracking at all (pre-existing
 * v3.0 debt) — its `focus` state is skipped entirely rather than asserted or faked; see
 * 21-UITEST-COVERAGE.md.
 */
@OptIn(ExperimentalTestApi::class)
class Base05GlassStateCaptureTest {

    // ------------------------------------------------------------------
    // AeroButton
    // ------------------------------------------------------------------

    @Test
    fun aeroButtonAeroBlueStates() = runComposeUiTest {
        driveButtonLike("AeroButton", "AeroBlue", AeroColorScheme.AeroBlue, outlined = false)
    }

    @Test
    fun aeroButtonAeroDarkStates() = runComposeUiTest {
        driveButtonLike("AeroButton", "AeroDark", AeroColorScheme.AeroDark, outlined = false)
    }

    @Test
    fun aeroButtonClassicStates() = runComposeUiTest {
        driveButtonLike("AeroButton", "Classic", AeroColorScheme.Classic, outlined = false)
    }

    // ------------------------------------------------------------------
    // AeroOutlinedButton
    // ------------------------------------------------------------------

    @Test
    fun aeroOutlinedButtonAeroBlueStates() = runComposeUiTest {
        driveButtonLike("AeroOutlinedButton", "AeroBlue", AeroColorScheme.AeroBlue, outlined = true)
    }

    @Test
    fun aeroOutlinedButtonAeroDarkStates() = runComposeUiTest {
        driveButtonLike("AeroOutlinedButton", "AeroDark", AeroColorScheme.AeroDark, outlined = true)
    }

    @Test
    fun aeroOutlinedButtonClassicStates() = runComposeUiTest {
        driveButtonLike("AeroOutlinedButton", "Classic", AeroColorScheme.Classic, outlined = true)
    }

    // ------------------------------------------------------------------
    // AeroSwitch
    // ------------------------------------------------------------------

    @Test
    fun aeroSwitchAeroBlueStates() = runComposeUiTest {
        driveAeroSwitch("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroSwitchAeroDarkStates() = runComposeUiTest {
        driveAeroSwitch("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroSwitchClassicStates() = runComposeUiTest {
        driveAeroSwitch("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroSegmentedControl
    // ------------------------------------------------------------------

    @Test
    fun aeroSegmentedControlAeroBlueStates() = runComposeUiTest {
        driveAeroSegmentedControl("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroSegmentedControlAeroDarkStates() = runComposeUiTest {
        driveAeroSegmentedControl("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroSegmentedControlClassicStates() = runComposeUiTest {
        driveAeroSegmentedControl("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroSlider
    // ------------------------------------------------------------------

    @Test
    fun aeroSliderAeroBlueStates() = runComposeUiTest {
        driveAeroSlider("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroSliderAeroDarkStates() = runComposeUiTest {
        driveAeroSlider("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroSliderClassicStates() = runComposeUiTest {
        driveAeroSlider("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroRangeSlider
    // ------------------------------------------------------------------

    @Test
    fun aeroRangeSliderAeroBlueStates() = runComposeUiTest {
        driveAeroRangeSlider("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroRangeSliderAeroDarkStates() = runComposeUiTest {
        driveAeroRangeSlider("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroRangeSliderClassicStates() = runComposeUiTest {
        driveAeroRangeSlider("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroListItem
    // ------------------------------------------------------------------

    @Test
    fun aeroListItemAeroBlueStates() = runComposeUiTest {
        driveAeroListItem("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroListItemAeroDarkStates() = runComposeUiTest {
        driveAeroListItem("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroListItemClassicStates() = runComposeUiTest {
        driveAeroListItem("Classic", AeroColorScheme.Classic)
    }

    // ==================================================================
    // Drivers — one per component, parameterized by (theme label, scheme)
    // ==================================================================

    private fun ComposeUiTest.driveButtonLike(
        component: String,
        theme: String,
        scheme: AeroColorScheme,
        outlined: Boolean,
    ) {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Column(modifier = Modifier.testTag("capture").size(260.dp, 80.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    if (outlined) {
                        AeroOutlinedButton(text = "Label", onClick = {}, modifier = Modifier.testTag("target"))
                    } else {
                        AeroButton(text = "Label", onClick = {}, modifier = Modifier.testTag("target"))
                    }
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        val (focusImg, focusMethod) = focusViaTabThenFallback(
            component, theme, defaultImg,
            explicitFocus = { onNodeWithTag("target").requestFocus() },
        )
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "$component/$theme: focus capture must differ from default (method=$focusMethod)",
        )

        onNodeWithTag("other").requestFocus()
        waitForIdle()

        onNodeWithTag("target").performMouseInput { moveTo(center) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onNodeWithTag("target").performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        onNodeWithTag("target").performMouseInput { release() }
        waitForIdle()
        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()
    }

    private fun ComposeUiTest.driveAeroSwitch(theme: String, scheme: AeroColorScheme) {
        val component = "AeroSwitch"
        setContent {
            AeroTheme(colorScheme = scheme) {
                var checked by remember { mutableStateOf(false) }
                Column(modifier = Modifier.testTag("capture").size(120.dp, 60.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    AeroSwitch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.testTag("target"),
                    )
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        val (focusImg, focusMethod) = focusViaTabThenFallback(
            component, theme, defaultImg,
            explicitFocus = { onNodeWithTag("target").requestFocus() },
        )
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "$component/$theme: focus capture must differ from default (method=$focusMethod)",
        )

        onNodeWithTag("other").requestFocus()
        waitForIdle()

        onNodeWithTag("target").performMouseInput { moveTo(center) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onNodeWithTag("target").performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        onNodeWithTag("target").performMouseInput { release() }
        waitForIdle()
        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()

        // default-on: a real click toggles checked=true through onCheckedChange — the shipped
        // click path, not a hand-set boolean.
        onNodeWithTag("target").performClick()
        waitForIdle()
        val onImg = capture(component, theme, "default-on")
        assertTrue(pixelMapsDiffer(defaultImg, onImg), "$component/$theme: checked capture must differ from default")
    }

    private fun ComposeUiTest.driveAeroSegmentedControl(theme: String, scheme: AeroColorScheme) {
        val component = "AeroSegmentedControl"
        setContent {
            AeroTheme(colorScheme = scheme) {
                Column(modifier = Modifier.testTag("capture").size(300.dp, 70.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    AeroSegmentedControl(
                        options = listOf("A", "B", "C"),
                        selected = "A",
                        onSelect = {},
                        modifier = Modifier.testTag("target"),
                    )
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        // Focus/hover/press act on the "B" segment — the outer Row carrying the "target" tag is
        // only hoverable, not itself a focus stop; each segment's own selectable() is.
        val (focusImg, focusMethod) = focusViaTabThenFallback(
            component, theme, defaultImg,
            explicitFocus = { onNodeWithText("B").requestFocus() },
        )
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "$component/$theme: focus capture must differ from default (method=$focusMethod)",
        )

        onNodeWithTag("other").requestFocus()
        waitForIdle()

        onNodeWithText("B").performMouseInput { moveTo(center) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onNodeWithText("B").performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        onNodeWithText("B").performMouseInput { release() }
        waitForIdle()
        onNodeWithText("B").performMouseInput { exit() }
        waitForIdle()
    }

    private fun ComposeUiTest.driveAeroSlider(theme: String, scheme: AeroColorScheme) {
        val component = "AeroSlider"
        lateinit var density: Density
        setContent {
            AeroTheme(colorScheme = scheme) {
                density = LocalDensity.current
                Column(modifier = Modifier.testTag("capture").size(300.dp, 70.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    AeroSlider(
                        value = 0.3f,
                        onValueChange = {},
                        modifier = Modifier.testTag("target").width(240.dp),
                    )
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        // No explicit fallback node is available — AeroSlider's own testTag lands on the outer
        // Box, not the M3 Slider's internal focusable thumb — so only real Tab traversal is tried.
        val (focusImg, focusMethod) = focusViaTabThenFallback(component, theme, defaultImg, explicitFocus = null)
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "$component/$theme: focus capture must differ from default (method=$focusMethod)",
        )

        onNodeWithTag("other").requestFocus()
        waitForIdle()

        // M3 Slider insets the track by half the thumb's own measured width (10dp radius) on each
        // side, so the thumb's screen x is NOT a plain fraction of the whole node's width.
        val bounds = onNodeWithTag("target").fetchSemanticsNode().boundsInRoot
        val thumbRadiusPx = with(density) { 10.dp.toPx() }
        val trackInnerWidth = (bounds.width - 2 * thumbRadiusPx).coerceAtLeast(0f)
        val thumbX = bounds.left + thumbRadiusPx + 0.3f * trackInnerWidth
        val thumbY = bounds.top + bounds.height / 2f
        val thumbCenter = Offset(thumbX, thumbY)

        onRoot().performMouseInput { enter(thumbCenter) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onRoot().performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        repeat(4) { step ->
            onRoot().performMouseInput { moveTo(Offset(thumbCenter.x + (step + 1) * 10f, thumbCenter.y)) }
            waitForIdle()
        }
        val dragImg = capture(component, theme, "drag")
        assertTrue(pixelMapsDiffer(defaultImg, dragImg), "$component/$theme: drag capture must differ from default")

        onRoot().performMouseInput { release() }
        waitForIdle()
    }

    private fun ComposeUiTest.driveAeroRangeSlider(theme: String, scheme: AeroColorScheme) {
        val component = "AeroRangeSlider"
        setContent {
            AeroTheme(colorScheme = scheme) {
                Column(modifier = Modifier.testTag("capture").size(300.dp, 70.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    AeroRangeSlider(
                        value = 0.2f..0.7f,
                        onValueChange = {},
                        modifier = Modifier.testTag("target").width(240.dp),
                    )
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")
        // n/a: no keyboard focus in AeroRangeSlider (pre-existing v3.0 debt) — skipped entirely,
        // not asserted or faked. See 21-UITEST-COVERAGE.md.

        // The Canvas draws thumbs at a plain fraction of the full track width (valueToX), no
        // thumb-radius inset — unlike AeroSlider's M3-hosted track.
        val bounds = onNodeWithTag("target").fetchSemanticsNode().boundsInRoot
        val startFraction = (0.2f - 0f) / (1f - 0f)
        val thumbCenter = Offset(bounds.left + startFraction * bounds.width, bounds.top + bounds.height / 2f)

        onRoot().performMouseInput { enter(thumbCenter) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onRoot().performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        repeat(4) { step ->
            onRoot().performMouseInput { moveTo(Offset(thumbCenter.x + (step + 1) * 10f, thumbCenter.y)) }
            waitForIdle()
        }
        val dragImg = capture(component, theme, "drag")
        assertTrue(pixelMapsDiffer(defaultImg, dragImg), "$component/$theme: drag capture must differ from default")

        onRoot().performMouseInput { release() }
        waitForIdle()
    }

    private fun ComposeUiTest.driveAeroListItem(theme: String, scheme: AeroColorScheme) {
        val component = "AeroListItem"
        val selectedState = mutableStateOf(false)
        setContent {
            AeroTheme(colorScheme = scheme) {
                Column(modifier = Modifier.testTag("capture").size(300.dp, 60.dp)) {
                    Box(modifier = Modifier.testTag("before").size(4.dp).focusable())
                    AeroListItem(
                        text = "Item",
                        onClick = {},
                        selected = selectedState.value,
                        modifier = Modifier.testTag("target"),
                    )
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        val (focusImg, focusMethod) = focusViaTabThenFallback(
            component, theme, defaultImg,
            explicitFocus = { onNodeWithTag("target").requestFocus() },
        )
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "$component/$theme: focus capture must differ from default (method=$focusMethod)",
        )

        onNodeWithTag("other").requestFocus()
        waitForIdle()

        onNodeWithTag("target").performMouseInput { moveTo(center) }
        waitForIdle()
        val hoverImg = capture(component, theme, "hover")
        assertTrue(pixelMapsDiffer(defaultImg, hoverImg), "$component/$theme: hover capture must differ from default")

        onNodeWithTag("target").performMouseInput { press() }
        waitForIdle()
        val pressImg = capture(component, theme, "press")
        assertTrue(pixelMapsDiffer(defaultImg, pressImg), "$component/$theme: press capture must differ from default")

        onNodeWithTag("target").performMouseInput { release() }
        waitForIdle()
        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()

        // default-selected: recompose with selected = true via the hoisted MutableState (not a
        // click — selection in AeroListItem is caller-controlled, not toggled by clicking).
        selectedState.value = true
        waitForIdle()
        val selectedImg = capture(component, theme, "default-selected")
        assertTrue(
            pixelMapsDiffer(defaultImg, selectedImg),
            "$component/$theme: selected capture must differ from default",
        )
    }
}

/**
 * Captures the whole `capture`-tagged container, writes it via [UiCapture.write] (opt-in only,
 * D-03), and returns its [PixelMap] for comparison against that (component, theme)'s own default.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.capture(component: String, theme: String, state: String): PixelMap {
    val img = onNodeWithTag("capture").captureToImage()
    UiCapture.write(component, theme, state, img)
    return img.toPixelMap()
}

/**
 * Moves focus from a sibling `before` node onto the component under test via real Tab-key
 * traversal. If the resulting capture is byte-identical to [defaultImg] (Tab did not visibly reach
 * the component in this harness) and [explicitFocus] is non-null, falls back to calling it
 * directly (VER07's keyboard-acquired `requestFocus()` path). Returns the capture actually used
 * and a human-readable description of which method produced it, for the coverage table.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.focusViaTabThenFallback(
    component: String,
    theme: String,
    defaultImg: PixelMap,
    explicitFocus: (() -> Unit)?,
): Pair<PixelMap, String> {
    onNodeWithTag("before").requestFocus()
    waitForIdle()
    onNodeWithTag("before").performKeyInput { pressKey(Key.Tab) }
    waitForIdle()
    var img = capture(component, theme, "focus")
    var method = "tab-traversal"
    if (!pixelMapsDiffer(defaultImg, img) && explicitFocus != null) {
        try {
            explicitFocus()
            waitForIdle()
            img = capture(component, theme, "focus")
            method = "requestFocus (Tab traversal produced no visible change)"
        } catch (e: Throwable) {
            method = "tab-traversal (explicit requestFocus fallback unavailable on this node)"
        }
    }
    return img to method
}
