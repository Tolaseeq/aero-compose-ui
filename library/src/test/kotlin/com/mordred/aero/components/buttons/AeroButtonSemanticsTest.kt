package com.mordred.aero.components.buttons

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * VBTN-04 — proves `Role.Button` semantics are present and that Space/Enter keyboard activation
 * both invoke `onClick`, now that the Material3 `Button`/`OutlinedButton` containers (which
 * supplied this for free) are gone in favor of `Modifier.clickable(role = Role.Button, ...)`. Not
 * a visual-review claim — an automated semantics + key-input assertion (17-UI-SPEC.md "must not
 * count VBTN-04 without this test").
 *
 * Compile-proof for the exact CMP 1.11.1 desktop test-API signatures used below —
 * `performKeyInput`/`pressKey` (`androidx.compose.ui.test`), `Key.Enter`/`Key.Spacebar`
 * (`androidx.compose.ui.input.key`), and `requestFocus()` on a `SemanticsNodeInteraction` — was
 * done by direct `javap` bytecode inspection of the real `ui-test-desktop-1.11.1.jar` /
 * `ui-desktop-1.11.1.jar` dependency jars pulled into the Gradle cache for this build, before
 * writing this test (17-RESEARCH.md Pitfall 6, mirroring the project's own TOOL-07
 * `dropShadow`/`innerShadow` package-correction precedent from Phase 15). All four symbols
 * resolved exactly as assumed; no package-location surprise this time. See 17-03-SUMMARY.md for
 * the recorded javap output.
 */
@OptIn(ExperimentalTestApi::class)
class AeroButtonSemanticsTest {

    @Test
    fun aeroButtonHasRoleButtonAndClickAction() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroButton(text = "Save Changes", onClick = {})
            }
        }
        waitForIdle()

        onNodeWithText("Save Changes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun aeroButtonSpaceKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clicked = false
        setContent {
            AeroTheme {
                AeroButton(text = "Save Changes", onClick = { clicked = true })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Save Changes")
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()

        assertTrue(clicked, "Space must invoke onClick once the button node is focused")
    }

    @Test
    fun aeroButtonEnterKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clicked = false
        setContent {
            AeroTheme {
                AeroButton(text = "Save Changes", onClick = { clicked = true })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Save Changes")
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(clicked, "Enter must invoke onClick once the button node is focused")
    }

    @Test
    fun aeroOutlinedButtonHasRoleButtonAndClickAction() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroOutlinedButton(text = "Cancel", onClick = {})
            }
        }
        waitForIdle()

        onNodeWithText("Cancel")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }

    @Test
    fun aeroOutlinedButtonEnterKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clicked = false
        setContent {
            AeroTheme {
                AeroOutlinedButton(text = "Cancel", onClick = { clicked = true })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Cancel")
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertTrue(clicked, "Enter must invoke onClick once the outlined button node is focused")
    }
}
