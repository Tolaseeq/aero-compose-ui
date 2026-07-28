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
import kotlin.test.assertEquals

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
 *
 * **VER-04 closure (Phase 20 Plan 03, D-16).** This pre-existing file — not a new keyboard-
 * activation test class — is recorded as closing VER-04. Audited against VER-04's four explicit
 * criteria:
 * - **separately asserted** — `AeroButton` and `AeroOutlinedButton` each have their own test
 *   methods below, so a single shared assertion could never pass with only one button wired.
 * - **no vacuous pass on a missing node** — every activation test now calls `assertExists()`
 *   before `requestFocus()`, so a missing button node fails with an explicit "node not found"
 *   message rather than surfacing as a confusing focus error.
 * - **order independence** — each test method runs its own independent `runComposeUiTest`
 *   composition; none shares state with another.
 * - **invocation count** — every activation test now increments an `Int` counter inside `onClick`,
 *   presses the activation key TWICE, and asserts the counter equals exactly `2` — "at least once"
 *   cannot distinguish a handler that fires once from one wired to a latch, so a boolean `clicked`
 *   flag is no longer sufficient here.
 *
 * **Deliberately NOT asserted (recorded, not silently dropped):** key-event delivery interleaved
 * with a recomposition in the same frame. `runComposeUiTest` (the v1, non-`StandardTestDispatcher`
 * API this project deliberately stays on per TOOL-04) drives a single-threaded test clock, so a
 * real-device input/recomposition race has no equivalent here — this stays outside what this test
 * can reach, and is not a gap this file can close.
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
        var clickCount = 0
        setContent {
            AeroTheme {
                AeroButton(text = "Save Changes", onClick = { clickCount++ })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Save Changes")
        node.assertExists()
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Spacebar) }
        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()

        assertEquals(2, clickCount, "Space pressed twice must invoke onClick exactly twice")
    }

    @Test
    fun aeroButtonEnterKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clickCount = 0
        setContent {
            AeroTheme {
                AeroButton(text = "Save Changes", onClick = { clickCount++ })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Save Changes")
        node.assertExists()
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Enter) }
        node.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(2, clickCount, "Enter pressed twice must invoke onClick exactly twice")
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
        var clickCount = 0
        setContent {
            AeroTheme {
                AeroOutlinedButton(text = "Cancel", onClick = { clickCount++ })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Cancel")
        node.assertExists()
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Enter) }
        node.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        assertEquals(2, clickCount, "Enter pressed twice must invoke onClick exactly twice")
    }

    @Test
    fun aeroOutlinedButtonSpaceKeyInvokesOnClickAfterFocus() = runComposeUiTest {
        var clickCount = 0
        setContent {
            AeroTheme {
                AeroOutlinedButton(text = "Cancel", onClick = { clickCount++ })
            }
        }
        waitForIdle()

        val node = onNodeWithText("Cancel")
        node.assertExists()
        node.requestFocus()
        waitForIdle()
        node.performKeyInput { pressKey(Key.Spacebar) }
        node.performKeyInput { pressKey(Key.Spacebar) }
        waitForIdle()

        assertEquals(2, clickCount, "Space pressed twice must invoke onClick exactly twice")
    }
}
