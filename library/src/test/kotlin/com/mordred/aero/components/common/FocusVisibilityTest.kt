package com.mordred.aero.components.common

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure JVM tests (no `runComposeUiTest`, no Compose runtime — [Interaction] subtypes are plain
 * constructible classes) for [FocusVisibility]/[FocusVisibility.reduce] — the 19-05 gap-closure
 * mechanism that decides whether a control's focus glow ring should be DRAWN, gating gap G2
 * (VSEL-02) without touching whether the control IS focused or keyboard-operable.
 *
 * Convention mirrors [com.mordred.aero.components.range.AeroSliderStylesTest]: fold a list of
 * interactions through the reducer and assert on the terminal state, no Compose runtime involved.
 */
class FocusVisibilityTest {

    private fun fold(vararg interactions: Interaction): FocusVisibility =
        interactions.fold(FocusVisibility()) { state, interaction -> state.reduce(interaction) }

    @Test
    fun tabFocusAloneIsVisible() {
        val result = fold(FocusInteraction.Focus())
        assertTrue(result.visible, "A bare keyboard-focus sequence (no press, no hover) must be visible")
    }

    @Test
    fun mouseClickPressBeforeFocusIsNotVisible() {
        val press = PressInteraction.Press(Offset.Zero)
        val result = fold(
            HoverInteraction.Enter(),
            press,
            FocusInteraction.Focus(),
        )
        assertFalse(result.visible, "Mouse click (hover, press, then focus) must not show the focus ring")
    }

    @Test
    fun mouseClickFocusBeforePressIsNotVisible() {
        val press = PressInteraction.Press(Offset.Zero)
        val result = fold(
            HoverInteraction.Enter(),
            FocusInteraction.Focus(),
            press,
        )
        assertFalse(
            result.visible,
            "Mouse click (hover, focus, then press) must not show the focus ring — the gate must " +
                "not depend on which of Focus/Press Compose emits first"
        )
    }

    @Test
    fun reportedSymptomResidualRingAfterPointerExitIsSuppressed() {
        val enter = HoverInteraction.Enter()
        val press = PressInteraction.Press(Offset.Zero)
        val result = fold(
            enter,
            press,
            FocusInteraction.Focus(),
            HoverInteraction.Exit(enter),
        )
        assertFalse(
            result.visible,
            "The reported G2 symptom: after a mouse click, moving the pointer away must NOT bring " +
                "back a residual focus ring"
        )
    }

    @Test
    fun idempotencyASecondPressAfterMouseClickStaysNotVisible() {
        val enter = HoverInteraction.Enter()
        val firstPress = PressInteraction.Press(Offset.Zero)
        val result = fold(
            enter,
            firstPress,
            FocusInteraction.Focus(),
            PressInteraction.Press(Offset.Zero),
        )
        assertFalse(
            result.visible,
            "VSEL-02 idempotency probe: clicking the same switch a second time in a row must leave " +
                "no residual focus cue either time"
        )
    }

    @Test
    fun recoveryUnfocusThenFreshFocusWithNoHoverIsVisibleAgain() {
        val enter = HoverInteraction.Enter()
        val press = PressInteraction.Press(Offset.Zero)
        val focus = FocusInteraction.Focus()
        val result = fold(
            enter,
            press,
            focus,
            FocusInteraction.Unfocus(focus),
            FocusInteraction.Focus(),
        )
        assertTrue(
            result.visible,
            "After Unfocus resets the state, a fresh Focus with no hover must show the ring again"
        )
    }

    @Test
    fun keyboardActivationWhileFocusedStaysVisible() {
        val result = fold(
            FocusInteraction.Focus(),
            PressInteraction.Press(Offset.Zero),
        )
        assertTrue(
            result.visible,
            "Space/Enter on a Tab-focused control (press with no preceding hover-enter) must not " +
                "extinguish its own ring"
        )
    }

    @Test
    fun concurrencyHoverAfterKeyboardFocusStaysVisibleAndHoveredIsTrue() {
        val result = fold(
            FocusInteraction.Focus(),
            HoverInteraction.Enter(),
        )
        assertTrue(
            result.visible,
            "VSEL-02 concurrency probe: hover arriving after keyboard focus must not suppress the " +
                "focus ring"
        )
        assertTrue(
            result.hovered,
            "VSEL-02 concurrency probe: the hover flag must independently read true so both cues " +
                "can draw at once"
        )
    }

    @Test
    fun mouseClickAfterATabAwayAndBackWithTheStationaryPointerStaysSuppressed() {
        val enter = HoverInteraction.Enter()
        val firstPress = PressInteraction.Press(Offset.Zero)
        val focus = FocusInteraction.Focus()
        val result = fold(
            enter,
            firstPress,
            focus,
            FocusInteraction.Unfocus(focus),
            FocusInteraction.Focus(),
            PressInteraction.Press(Offset.Zero),
        )
        assertFalse(
            result.visible,
            "CR-02: a mouse press must stay suppressed even though the hover enter that preceded " +
                "it arrived before an intervening focus loss — the pointer never left, so no " +
                "hover exit was ever emitted to re-arm the guard"
        )
    }

    @Test
    fun defaultStateIsNotVisible() {
        val result = FocusVisibility()
        assertEquals(false, result.focused)
        assertEquals(false, result.hovered)
        assertEquals(false, result.pointerAcquired)
        assertFalse(result.visible, "A brand-new FocusVisibility (nothing has happened yet) must not be visible")
    }
}
