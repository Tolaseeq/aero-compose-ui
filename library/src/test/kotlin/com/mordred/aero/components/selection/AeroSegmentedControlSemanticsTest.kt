package com.mordred.aero.components.selection

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithText
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
 * VSEL-04 — proves per-segment `Role.RadioButton` semantics are present and that Space/Enter
 * keyboard activation both invoke `onSelect` for EVERY segment (not a single sampled node), now
 * that the bare `.clickable(...)` (no role, no semantics) is replaced by
 * `Modifier.selectable(role = Role.RadioButton, ...)`. Mirrors
 * [com.mordred.aero.components.buttons.AeroButtonSemanticsTest]'s `runComposeUiTest` +
 * `performKeyInput`/`pressKey` shape; the loop-over-N-segment-nodes assertion shape has no
 * single-node precedent in the button test (19-PATTERNS.md).
 *
 * Arrow-key roving focus is deliberately NOT tested here — deferred (D-09), N Tab stops for an
 * N-segment control is the accepted trade-off this phase ships with.
 */
@OptIn(ExperimentalTestApi::class)
class AeroSegmentedControlSemanticsTest {

    private val options = listOf("Day", "Week", "Month")

    @Test
    fun everySegmentHasRoleRadioButtonAndExactlyOneReportsSelected() = runComposeUiTest {
        setContent {
            AeroTheme {
                AeroSegmentedControl(options = options, selected = "Day", onSelect = {})
            }
        }
        waitForIdle()

        options.forEach { label ->
            onNodeWithText(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
        onNodeWithText("Day").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        onNodeWithText("Week").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        onNodeWithText("Month").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun everySegmentIsIndependentlyFocusableAndActivatesViaSpace() = runComposeUiTest {
        val activatedInOrder = mutableListOf<String>()
        setContent {
            AeroTheme {
                AeroSegmentedControl(
                    options = options,
                    selected = "Day",
                    onSelect = { activatedInOrder.add(it) },
                )
            }
        }
        waitForIdle()

        options.forEach { label ->
            val node = onNodeWithText(label)
            node.requestFocus()
            waitForIdle()
            node.performKeyInput { pressKey(Key.Spacebar) }
            waitForIdle()
        }

        assertEquals(
            options,
            activatedInOrder,
            "Space must activate each of the three segments independently, invoking onSelect with " +
                "that segment's own option, for every segment in the control",
        )
    }

    @Test
    fun everySegmentIsIndependentlyFocusableAndActivatesViaEnter() = runComposeUiTest {
        val activatedInOrder = mutableListOf<String>()
        setContent {
            AeroTheme {
                AeroSegmentedControl(
                    options = options,
                    selected = "Day",
                    onSelect = { activatedInOrder.add(it) },
                )
            }
        }
        waitForIdle()

        options.forEach { label ->
            val node = onNodeWithText(label)
            node.requestFocus()
            waitForIdle()
            node.performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
        }

        assertEquals(
            options,
            activatedInOrder,
            "Enter must activate each of the three segments independently, invoking onSelect with " +
                "that segment's own option, for every segment in the control",
        )
    }

    /**
     * A disabled selectable node exposes no `RequestFocus` semantics action (proven empirically —
     * calling `requestFocus()` on it throws), so this test drives a real pointer gesture instead
     * of Space/Enter, mirroring `AeroSwitchSemanticsTest`'s
     * `aeroSwitchDisabledNeverInvokesOnCheckedChangeOnRealPointerClick` precedent (19-02).
     */
    @Test
    fun disabledControlNeverInvokesOnSelect() = runComposeUiTest {
        var invoked = false
        setContent {
            AeroTheme {
                AeroSegmentedControl(
                    options = options,
                    selected = "Day",
                    onSelect = { invoked = true },
                    enabled = false,
                )
            }
        }
        waitForIdle()

        onNodeWithText("Week").performMouseInput {
            moveTo(center)
            press()
            release()
        }
        waitForIdle()

        assertFalse(invoked, "A disabled AeroSegmentedControl must never invoke onSelect")
    }
}
