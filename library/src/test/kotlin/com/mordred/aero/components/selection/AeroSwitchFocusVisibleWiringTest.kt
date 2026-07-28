package com.mordred.aero.components.selection

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * 19-09 (CR-02) composed reachability proof: proves the reducer defect is reachable through a
 * REAL [AeroSwitch] modifier chain, a real [MutableInteractionSource] and a real focus move
 * between two nodes — not merely through a hand-folded list of [androidx.compose.foundation.interaction.Interaction]
 * objects the way [com.mordred.aero.components.common.FocusVisibilityTest] proves the reducer
 * contract itself.
 *
 * Mirrors [com.mordred.aero.components.common.InteractionStatesTest]'s composed-test shape: one
 * hoisted [MutableInteractionSource], `state = rememberAeroInteractionState(source)` assigned to a
 * `lateinit var` inside `setContent` so the test can observe it, `waitForIdle()` after every
 * driven step.
 *
 * Never moves the pointer off the switch node anywhere in this test — the whole repro depends on
 * the pointer never leaving the control, so no hover-exit interaction is ever emitted to re-arm
 * the reducer's stale `hovered` flag.
 */
@OptIn(ExperimentalTestApi::class)
class AeroSwitchFocusVisibleWiringTest {

    @Test
    fun mouseClickAfterTabAwayAndBackWithAStationaryPointerDrawsNoFocusCue() = runComposeUiTest {
        val source = MutableInteractionSource()
        lateinit var state: com.mordred.aero.components.common.AeroInteractionState

        setContent {
            AeroTheme {
                state = rememberAeroInteractionState(source)
                Column {
                    AeroSwitch(
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("switch"),
                        interactionSource = source,
                    )
                    Box(
                        modifier = Modifier
                            .testTag("other")
                            .size(24.dp)
                            .focusable()
                    )
                }
            }
        }
        waitForIdle()

        val switch = onNodeWithTag("switch")
        val other = onNodeWithTag("other")

        // Move the pointer onto the switch and leave it resting there.
        switch.performMouseInput { moveTo(center) }
        waitForIdle()

        // Press and release on the switch (mouse-acquired press+focus).
        switch.performMouseInput { press() }
        waitForIdle()
        switch.performMouseInput { release() }
        waitForIdle()

        switch.requestFocus()
        waitForIdle()
        assertFalse(
            state.focusVisible,
            "A mouse click on the switch must not draw a focus cue (the mouse-acquired case, " +
                "which already worked before this fix)"
        )

        // Tab away — focus moves to the sibling node. The pointer is NOT moved or exited.
        other.requestFocus()
        waitForIdle()

        // Tab back to the switch.
        switch.requestFocus()
        waitForIdle()

        // Press and release again with that same resting pointer.
        switch.performMouseInput { press() }
        waitForIdle()
        switch.performMouseInput { release() }
        waitForIdle()

        assertFalse(
            state.focusVisible,
            "CR-02: a mouse press on a control the pointer never left must not draw a focus cue, " +
                "even after an intervening focus loss and re-focus"
        )
    }
}
