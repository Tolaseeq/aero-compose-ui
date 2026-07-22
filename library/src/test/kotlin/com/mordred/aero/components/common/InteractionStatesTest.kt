package com.mordred.aero.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PRIM-15 verification: [rememberAeroInteractionState] bundles hover/press/focus booleans
 * that track a shared [MutableInteractionSource], mirroring the
 * `com.mordred.aero.components.list.AeroListItem` `Modifier.hoverable(interactionSource)` +
 * `collectIsHoveredAsState()` wiring precedent.
 */
@OptIn(ExperimentalTestApi::class)
class InteractionStatesTest {

    @Test
    fun rememberAeroInteractionStateTracksHover() = runComposeUiTest {
        lateinit var state: AeroInteractionState
        val interactionSource = MutableInteractionSource()

        setContent {
            state = rememberAeroInteractionState(interactionSource)
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    .hoverable(interactionSource)
            )
        }
        waitForIdle()
        assertFalse(state.hovered, "hovered should start false")

        onNodeWithTag("target").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(state.hovered, "hovered should become true after the pointer enters")

        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()
        assertFalse(state.hovered, "hovered should become false after the pointer exits")
    }

    @Test
    fun rememberAeroInteractionStateTracksPress() = runComposeUiTest {
        lateinit var state: AeroInteractionState
        val interactionSource = MutableInteractionSource()

        setContent {
            state = rememberAeroInteractionState(interactionSource)
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {}
            )
        }
        waitForIdle()
        assertFalse(state.pressed, "pressed should start false")

        onNodeWithTag("target").performMouseInput {
            moveTo(center)
            press()
        }
        waitForIdle()
        assertTrue(state.pressed, "pressed should become true while the pointer is down")

        onNodeWithTag("target").performMouseInput { release() }
        waitForIdle()
        assertFalse(state.pressed, "pressed should become false after the pointer is released")
    }

    @Test
    fun rememberAeroInteractionStateTracksFocus() = runComposeUiTest {
        lateinit var state: AeroInteractionState
        val interactionSource = MutableInteractionSource()

        setContent {
            state = rememberAeroInteractionState(interactionSource)
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    .focusable(interactionSource = interactionSource)
            )
        }
        waitForIdle()
        assertFalse(state.focused, "focused should start false")

        onNodeWithTag("target").requestFocus()
        waitForIdle()
        assertTrue(state.focused, "focused should become true once the element gains focus")
    }
}
