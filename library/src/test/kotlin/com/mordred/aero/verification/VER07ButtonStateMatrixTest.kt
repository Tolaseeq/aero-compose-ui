package com.mordred.aero.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * VER-07 / SHW-16 gate: per-state matrix for [AeroButton] and [AeroOutlinedButton], across all
 * three built-in color schemes, converting the mechanically-checkable half of the SHW-16 sign-off
 * checklist's state-matrix row into a permanent, re-executed gate (20-08-PLAN.md Task 1).
 *
 * **Real input only.** Hover is driven by `performMouseInput { moveTo(center) }`, press by a held
 * pointer (`press()`/`release()`), and focus by [androidx.compose.ui.test.requestFocus] called
 * BEFORE any hover/press has occurred on the node — per
 * [com.mordred.aero.components.common.FocusVisibility]'s reducer, `pointerAcquired` (which
 * suppresses the visible focus ring) is only set by a [androidx.compose.foundation.interaction.PressInteraction.Press]
 * that arrives while already hovered, so calling `requestFocus()` first, with nothing yet hovered
 * or pressed, reaches the same keyboard-acquired `focusVisible = true` path real Tab-traversal
 * reaches, without needing a synthetic key-event sequence. None of the five states is ever produced
 * by hand-constructing an [androidx.compose.foundation.interaction.InteractionSource] state — doing
 * so would assert this test's own model of the component instead of the component's actual wiring
 * (20-08-PLAN.md `<critical_correctness_note>`).
 *
 * **Per-test ordering is load-bearing.** Focus is captured FIRST (immediately after the default
 * capture), before hover/press ever run, so `pointerAcquired` is guaranteed false. Focus is then
 * explicitly moved to a sibling `other` node (mirroring
 * [com.mordred.aero.components.selection.AeroSwitchFocusVisibleWiringTest]'s idiom) — which emits
 * `FocusInteraction.Unfocus` on the button and resets its `focused`/`pointerAcquired` fields — before
 * hover and press run, so the hover/press captures are never contaminated by a lingering visible
 * focus ring. Disabled runs last, after the pointer has been released and exited.
 *
 * **Whole-container pixel comparison, not a single sampled point.** [pixelMapsDiffer] compares
 * every pixel of a fixed-size `capture` container wrapping the button (generously padded — the
 * button's own focus/hover glow bloom is documented to paint "beyond the layout bounds" in
 * [com.mordred.aero.theme.AeroSurfacePrimitives.drawAeroGlowRing]'s KDoc) so no assumption about
 * exactly where a state's visual delta lands is required to catch it.
 *
 * **D-08 fixture proof, in this file** ([pixelMapsDifferDetectsGenuinelyDifferentCaptures] /
 * [pixelMapsDifferReturnsFalseOnIndistinguishableCaptures]), following the convention established by
 * [VER01GradientProportionalitySourceTest] — a gate that cannot fail is a false pass (VER-06). Both
 * fixtures exercise the exact same `captureToImage()`/`toPixelMap()`/[pixelMapsDiffer] path the real
 * per-state assertions below use, proving the comparator can both detect a genuine difference and
 * correctly report "no difference" on truly indistinguishable captures — the latter is what would
 * make a real per-state assertion correctly fail red if a state were ever left unwired and rendered
 * byte-identical to default.
 *
 * **Absent-node safety.** Each per-scheme test asserts the `btn` node exists before driving any
 * input — a missing component fails this gate outright rather than passing vacuously, since every
 * subsequent `onNodeWithTag("btn")` call would already throw, but the explicit
 * [assertExists] makes that failure mode the FIRST thing checked, with a message naming exactly
 * which component/scheme combination is missing.
 *
 * **Boundary.** This gate is mechanical byte-level differencing, not an aesthetic judgment — whether
 * the resulting hover/press/disabled looks match the design intent, and whether the eight components
 * read as one coherent material family, remain the human questions 20-07 exists for (SHW-16).
 */
@OptIn(ExperimentalTestApi::class)
class VER07ButtonStateMatrixTest {

    // ---------------------------------------------------------------------------------------
    // D-08 fixture proof — same captureToImage()/toPixelMap()/pixelMapsDiffer path as below.
    // ---------------------------------------------------------------------------------------

    @Test
    fun pixelMapsDifferDetectsGenuinelyDifferentCaptures() = runComposeUiTest {
        setContent {
            Row {
                Box(Modifier.testTag("red").size(20.dp).background(Color.Red))
                Box(Modifier.testTag("blue").size(20.dp).background(Color.Blue))
            }
        }
        waitForIdle()
        val redMap = onNodeWithTag("red").captureToImage().toPixelMap()
        val blueMap = onNodeWithTag("blue").captureToImage().toPixelMap()
        assertTrue(
            pixelMapsDiffer(redMap, blueMap),
            "D-08 fixture proof: a solid red capture and a solid blue capture must be detected as " +
                "different, proving pixelMapsDiffer can actually detect a genuine difference rather " +
                "than vacuously always returning false (VER-07)"
        )
    }

    @Test
    fun pixelMapsDifferReturnsFalseOnIndistinguishableCaptures() = runComposeUiTest {
        setContent {
            Row {
                Box(Modifier.testTag("redA").size(20.dp).background(Color.Red))
                Box(Modifier.testTag("redB").size(20.dp).background(Color.Red))
            }
        }
        waitForIdle()
        val a = onNodeWithTag("redA").captureToImage().toPixelMap()
        val b = onNodeWithTag("redB").captureToImage().toPixelMap()
        assertFalse(
            pixelMapsDiffer(a, b),
            "D-08 fixture proof: two indistinguishable (both solid red) captures must compare " +
                "equal, proving pixelMapsDiffer is not vacuously always-true — the exact failure " +
                "mode that would let a state that renders byte-identical to default pass silently " +
                "(VER-07)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // AeroButton (filled) — all three schemes.
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroButtonAeroBlueStateMatrix() =
        runButtonStateMatrix("AeroButton/AeroBlue", AeroColorScheme.AeroBlue, outlined = false)

    @Test
    fun aeroButtonAeroDarkStateMatrix() =
        runButtonStateMatrix("AeroButton/AeroDark", AeroColorScheme.AeroDark, outlined = false)

    @Test
    fun aeroButtonClassicStateMatrix() =
        runButtonStateMatrix("AeroButton/Classic", AeroColorScheme.Classic, outlined = false)

    // ---------------------------------------------------------------------------------------
    // AeroOutlinedButton — all three schemes. A SEPARATE set of tests from AeroButton's above —
    // never a shared assertion that could pass with only one of the two wired (20-08-PLAN.md).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroOutlinedButtonAeroBlueStateMatrix() =
        runButtonStateMatrix("AeroOutlinedButton/AeroBlue", AeroColorScheme.AeroBlue, outlined = true)

    @Test
    fun aeroOutlinedButtonAeroDarkStateMatrix() =
        runButtonStateMatrix("AeroOutlinedButton/AeroDark", AeroColorScheme.AeroDark, outlined = true)

    @Test
    fun aeroOutlinedButtonClassicStateMatrix() =
        runButtonStateMatrix("AeroOutlinedButton/Classic", AeroColorScheme.Classic, outlined = true)

    /**
     * Drives all five states through real input for one (component, scheme) combination and
     * asserts each differs from that same combination's own default capture. See this class's
     * KDoc for the ordering rationale and why each state is driven the way it is.
     */
    private fun runButtonStateMatrix(
        label: String,
        scheme: AeroColorScheme,
        outlined: Boolean,
    ) = runComposeUiTest {
        val enabledState = mutableStateOf(true)

        setContent {
            AeroTheme(colorScheme = scheme) {
                Column(modifier = Modifier.testTag("capture").size(260.dp, 80.dp)) {
                    if (outlined) {
                        AeroOutlinedButton(
                            text = "Label",
                            onClick = {},
                            modifier = Modifier.testTag("btn"),
                            enabled = enabledState.value,
                        )
                    } else {
                        AeroButton(
                            text = "Label",
                            onClick = {},
                            modifier = Modifier.testTag("btn"),
                            enabled = enabledState.value,
                        )
                    }
                    // Sibling focus target only — no background, contributes zero pixels to the
                    // capture, exists solely so focus can be moved definitively away from `btn`
                    // (mirrors AeroSwitchFocusVisibleWiringTest's `other` node).
                    Box(modifier = Modifier.testTag("other").size(4.dp).focusable())
                }
            }
        }
        waitForIdle()

        onNodeWithTag("btn").assertExists(
            "VER-07: $label must be present in the semantics tree — a missing component fails " +
                "this gate rather than passing vacuously on a missing node"
        )

        fun capture(): PixelMap = onNodeWithTag("capture").captureToImage().toPixelMap()

        val defaultImg = capture()

        // FOCUS — driven first, before any hover/press, so pointerAcquired is guaranteed false
        // and requestFocus() reaches the same keyboard-acquired focusVisible = true path real
        // Tab-traversal reaches.
        onNodeWithTag("btn").requestFocus()
        waitForIdle()
        val focusImg = capture()
        assertTrue(
            pixelMapsDiffer(defaultImg, focusImg),
            "VER-07: $label's keyboard-focused capture must differ from its default capture " +
                "(the focus glow ring must actually paint)"
        )

        // Move focus away — emits FocusInteraction.Unfocus on `btn`, resetting focused/
        // pointerAcquired so the hover/press captures below are never contaminated by a
        // lingering visible focus ring.
        onNodeWithTag("other").requestFocus()
        waitForIdle()

        // HOVER — real performMouseInput moveTo, never a hand-constructed InteractionSource.
        onNodeWithTag("btn").performMouseInput { moveTo(center) }
        waitForIdle()
        val hoverImg = capture()
        assertTrue(
            pixelMapsDiffer(defaultImg, hoverImg),
            "VER-07: $label's hovered capture must differ from its default capture"
        )

        // PRESS — held pointer, real input.
        onNodeWithTag("btn").performMouseInput { press() }
        waitForIdle()
        val pressImg = capture()
        assertTrue(
            pixelMapsDiffer(defaultImg, pressImg),
            "VER-07: $label's pressed capture must differ from its default capture"
        )

        onNodeWithTag("btn").performMouseInput { release() }
        waitForIdle()
        onNodeWithTag("btn").performMouseInput { exit() }
        waitForIdle()

        // DISABLED — driven last, after the pointer has been released and exited.
        enabledState.value = false
        waitForIdle()
        val disabledImg = capture()
        assertTrue(
            pixelMapsDiffer(defaultImg, disabledImg),
            "VER-07: $label's disabled capture must differ from its default capture"
        )
    }
}

/**
 * Pure comparator over two [PixelMap]s — returns `true` the moment any pixel differs (or the
 * dimensions differ), `false` only when every pixel matches exactly. Used identically by the
 * D-08 fixture proof above and every real per-state assertion in this file, so both exercise the
 * same code path (D-08).
 */
internal fun pixelMapsDiffer(a: PixelMap, b: PixelMap): Boolean {
    if (a.width != b.width || a.height != b.height) return true
    for (y in 0 until a.height) {
        for (x in 0 until a.width) {
            if (a[x, y] != b[x, y]) return true
        }
    }
    return false
}
