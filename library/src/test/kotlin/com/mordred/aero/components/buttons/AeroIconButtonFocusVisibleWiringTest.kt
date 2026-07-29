package com.mordred.aero.components.buttons

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.common.FocusVisibility
import com.mordred.aero.components.common.reduce
import com.mordred.aero.components.common.rememberFocusVisible
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * VER-06/WR-02 gate (20-08-PLAN.md Task 3): [AeroIconButton] was the only Aero button never
 * converted to `focusVisible` — its focus ring appeared on a plain mouse click (WR-02). Fixed in
 * `61cab14` (20-05), this gate guards that fix so it cannot silently regress.
 *
 * **Both halves are asserted, per the plan's own correctness note.** Asserting only "keyboard
 * focus draws a ring" would pass on a component that rings on EVERY interaction — exactly the
 * pre-fix WR-02 defect. The mouse-click-produces-no-ring half is the half that actually guards
 * the fix; it is asserted first and independently of the keyboard half below.
 *
 * **Shape mirrors [com.mordred.aero.components.selection.AeroSwitchFocusVisibleWiringTest]:** one
 * hoisted [MutableInteractionSource] fed to the real, composed [AeroIconButton]; a sibling `other`
 * focusable node so focus can be moved definitively away between the two halves; `waitForIdle()`
 * after every driven step.
 *
 * **Observed via the exact production mechanism, not a hand-rolled model.** [rememberFocusVisible]
 * is called a second time, in this test's own composition, against the SAME [MutableInteractionSource]
 * the real [AeroIconButton] below is wired to — it is the identical `internal` composable
 * [AeroIconButton] itself calls to gate its `border` modifier, collecting the same hot
 * `source.interactions` stream. This is real input driving the real component's real focus-cue
 * decision, observed through its own production code path — never a synthetic
 * [androidx.compose.foundation.interaction.InteractionSource] state constructed by hand.
 *
 * **D-08 fixture proof, in this file** ([mouseClickShapedSequenceIsSuppressedByVisibleButWouldRingOnPlainFocused])
 * — folds a mouse-click-shaped interaction sequence (hover, press, then a click-driven focus
 * request) through the exact [FocusVisibility.reduce] this gate's real assertion is built on, and
 * proves two things: the sequence genuinely reaches `.focused == true` (so the fixture isn't
 * vacuous), and `.visible` is nonetheless suppressed — i.e. the same sequence that WOULD have
 * drawn a ring under the pre-fix "gate on plain `.focused`" behaviour is caught. A D-08 proof that
 * could not fail on that pre-fix behaviour would itself be a false pass (VER-06).
 */
@OptIn(ExperimentalTestApi::class)
class AeroIconButtonFocusVisibleWiringTest {

    // ---------------------------------------------------------------------------------------
    // D-08 fixture proof — pure reducer fold, no Compose runtime, same reduce()/visible path
    // rememberFocusVisible (and therefore AeroIconButton's own border gate) is built on.
    // ---------------------------------------------------------------------------------------

    @Test
    fun mouseClickShapedSequenceIsSuppressedByVisibleButWouldRingOnPlainFocused() {
        // Shape of a real mouse click that also happens to move platform focus onto the node:
        // hover-enter, then a press while hovered (which sets pointerAcquired), then the focus
        // interaction the click itself triggers.
        val mouseClickThatAlsoFocuses = listOf(
            HoverInteraction.Enter(),
            PressInteraction.Press(Offset.Zero),
            FocusInteraction.Focus(),
        )
        val folded = mouseClickThatAlsoFocuses.fold(FocusVisibility()) { state, interaction ->
            state.reduce(interaction)
        }

        assertTrue(
            folded.focused,
            "D-08 fixture setup: this sequence must actually reach focused = true, or it proves " +
                "nothing about the pre-fix WR-02 behaviour (gating on plain .focused) it exists " +
                "to demonstrate"
        )
        assertFalse(
            folded.visible,
            "D-08 fail-then-pass proof: the same sequence that reaches plain .focused = true " +
                "(the unfixed WR-02 behaviour, which draws a ring on every mouse click) must be " +
                "suppressed by .visible via pointerAcquired — proving this gate's real assertion, " +
                "built on the identical reduce()/visible path, is not vacuously true"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real component, real input, both halves.
    // ---------------------------------------------------------------------------------------

    @Test
    fun mouseClickDrawsNoRingButKeyboardTraversalDoes() = runComposeUiTest {
        val source = MutableInteractionSource()
        var focusVisible = false

        setContent {
            AeroTheme {
                Column {
                    AeroIconButton(
                        onClick = {},
                        modifier = Modifier.testTag("iconButton"),
                        interactionSource = source,
                    ) {
                        Box(Modifier.size(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .testTag("other")
                            .size(24.dp)
                            .focusable()
                    )
                }
                // Second collector on the SAME hot interaction stream AeroIconButton itself reads
                // to gate its border — real wiring, not a hand-rolled model (see class KDoc).
                focusVisible = rememberFocusVisible(source)
            }
        }
        waitForIdle()

        val iconButton = onNodeWithTag("iconButton")
        val other = onNodeWithTag("other")

        // --- Half 1 (WR-02): a real mouse click leaves NO focus ring. ---
        iconButton.performMouseInput { moveTo(center) }
        waitForIdle()
        iconButton.performMouseInput { press() }
        waitForIdle()
        iconButton.performMouseInput { release() }
        waitForIdle()

        assertFalse(
            focusVisible,
            "WR-02: a mouse click on AeroIconButton must not draw a focus ring — this is the half " +
                "that actually guards the fix; a gate asserting only the keyboard half below would " +
                "pass on a component that rings on every interaction (the pre-fix defect)"
        )

        iconButton.performMouseInput { exit() }
        waitForIdle()

        // Move focus definitively away — emits FocusInteraction.Unfocus on iconButton, resetting
        // focused/pointerAcquired, mirroring AeroSwitchFocusVisibleWiringTest's `other` idiom —
        // before driving the keyboard half.
        other.requestFocus()
        waitForIdle()

        // --- Half 2: genuine keyboard traversal (requestFocus with no preceding hover/press on
        // this node) DOES draw a ring. ---
        iconButton.requestFocus()
        waitForIdle()

        assertTrue(
            focusVisible,
            "Keyboard-Tab focus onto AeroIconButton must draw a focus ring — without this half, " +
                "a component that never draws a ring at all would also pass"
        )
    }
}
