package com.mordred.aero.components.selection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 19-02 regression guard, mirroring [com.mordred.aero.components.buttons.AeroButtonSemanticsTest]'s
 * shape: proves the groove/thumb restyle did not cost [AeroSwitch] its `Role.Switch` semantics or
 * `Modifier.toggleable`'s free keyboard activation (Space after focus), and that a disabled switch
 * never forwards a real pointer click to `onCheckedChange`.
 *
 * [AeroSwitch] renders no text, so nodes are located by [Modifier.testTag] rather than
 * `onNodeWithText`, unlike the button precedent.
 */
@OptIn(ExperimentalTestApi::class)
class AeroSwitchSemanticsTest {

    @Test
    fun aeroSwitchHasRoleSwitchAndToggleAction() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroSwitch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("switch"),
                )
            }
        }
        waitForIdle()

        onNodeWithTag("switch")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertHasClickAction()
    }

    @Test
    fun aeroSwitchSpaceKeyFlipsCheckedExactlyOnceAfterFocus() = runComposeUiTest {
        var checked by mutableStateOf(false)
        var invocationCount = 0
        setContent {
            AeroTheme {
                AeroSwitch(
                    checked = checked,
                    onCheckedChange = {
                        invocationCount++
                        checked = it
                    },
                    modifier = Modifier.testTag("switch"),
                )
            }
        }
        waitForIdle()

        val node = onNodeWithTag("switch")
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()

        assertEquals(1, invocationCount, "Space must flip the backing state exactly once after focus")
        assertEquals(true, checked, "checked must flip from false to true")
    }

    /**
     * 19-05/gap G2 regression: proves the focus-visible gate suppresses only the DRAWING of the
     * focus ring, never the focus stop or the Space key binding — the failure mode that would be
     * worse than the defect being fixed.
     */
    @Test
    fun aeroSwitchStaysFocusedAndSpaceStillTogglesAfterTheFocusVisibleGate() = runComposeUiTest {
        var checked by mutableStateOf(false)
        var invocationCount = 0
        setContent {
            AeroTheme {
                AeroSwitch(
                    checked = checked,
                    onCheckedChange = {
                        invocationCount++
                        checked = it
                    },
                    modifier = Modifier.testTag("switch"),
                )
            }
        }
        waitForIdle()

        val node = onNodeWithTag("switch")
        node.requestFocus()
        waitForIdle()
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))

        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()

        assertEquals(1, invocationCount, "Space must still flip the switch after the focus-visible gate")
        assertEquals(true, checked, "checked must still flip from false to true")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun aeroSwitchDisabledNeverInvokesOnCheckedChangeOnRealPointerClick() = runComposeUiTest {
        var invoked = false
        setContent {
            AeroTheme {
                AeroSwitch(
                    checked = false,
                    onCheckedChange = { invoked = true },
                    modifier = Modifier.testTag("switch"),
                    enabled = false,
                )
            }
        }
        waitForIdle()

        val bounds = onNodeWithTag("switch").fetchSemanticsNode().boundsInRoot
        val center = Offset(bounds.left + bounds.width / 2f, bounds.top + bounds.height / 2f)
        onRoot().performMouseInput {
            moveTo(center)
            press()
            release()
        }
        waitForIdle()

        assertFalse(invoked, "disabled switch must never invoke onCheckedChange")
    }
}
