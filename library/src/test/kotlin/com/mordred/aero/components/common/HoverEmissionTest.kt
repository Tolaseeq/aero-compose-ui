package com.mordred.aero.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.list.AeroListItem
import com.mordred.aero.components.selection.AeroSwitch
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WR-04 premise proof. WR-04 claims the three interaction modifiers used across the library's
 * restyled selection/list components — `toggleable` (`AeroSwitch`), `selectable`
 * (`AeroSegmentedControl`, per segment) and `clickable` (`AeroListItem`, clickable path) — already
 * emit hover on the [MutableInteractionSource] they are given, which is why an additional explicit
 * `Modifier.hoverable(interactionSource)` chained on the same source is a duplicate emitter. That
 * claim was made against a decompiled desktop artifact, not this library's own build.
 *
 * These three tests turn the claim into a check that runs against whatever Compose version the
 * library actually builds with, so a future toolchain bump that changes this behaviour fails here
 * rather than silently deleting hover from three shipped components. They must pass against the
 * CURRENT, unmodified [toggleable]/[selectable]/[clickable] modifiers — nothing in this file
 * exercises library production code, so nothing needs removing first.
 */
@OptIn(ExperimentalTestApi::class)
class HoverEmissionTest {

    @Test
    fun toggleableEmitsHoverOnItsSuppliedInteractionSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        lateinit var hovered: State<Boolean>

        setContent {
            hovered = source.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    // Deliberately no explicit Modifier.hoverable( — toggleable alone must emit hover.
                    .toggleable(
                        value = false,
                        interactionSource = source,
                        indication = null,
                        onValueChange = {},
                    )
            )
        }
        waitForIdle()
        assertFalse(
            hovered.value,
            "hovered should start false — if this assertion fails, the premise behind removing the " +
                "redundant hover modifier from AeroSwitch is false and Task 3's removal must not be " +
                "performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            hovered.value,
            "toggleable must emit hover on its supplied interaction source when the pointer enters — " +
                "if this assertion fails, the premise behind removing the redundant hover modifier " +
                "from AeroSwitch is false and Task 3's removal must not be performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()
        assertFalse(
            hovered.value,
            "toggleable must emit hover-exit on its supplied interaction source when the pointer " +
                "leaves — if this assertion fails, the premise behind removing the redundant hover " +
                "modifier from AeroSwitch is false and Task 3's removal must not be performed for it " +
                "(WR-04)"
        )
    }

    @Test
    fun selectableEmitsHoverOnItsSuppliedInteractionSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        lateinit var hovered: State<Boolean>

        setContent {
            hovered = source.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    // Deliberately no explicit Modifier.hoverable( — selectable alone must emit hover.
                    .selectable(
                        selected = false,
                        interactionSource = source,
                        indication = null,
                        onClick = {},
                    )
            )
        }
        waitForIdle()
        assertFalse(
            hovered.value,
            "hovered should start false — if this assertion fails, the premise behind removing the " +
                "redundant per-segment hover modifier from AeroSegmentedControl is false and Task 3's " +
                "removal must not be performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            hovered.value,
            "selectable must emit hover on its supplied interaction source when the pointer enters — " +
                "if this assertion fails, the premise behind removing the redundant per-segment hover " +
                "modifier from AeroSegmentedControl is false and Task 3's removal must not be " +
                "performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()
        assertFalse(
            hovered.value,
            "selectable must emit hover-exit on its supplied interaction source when the pointer " +
                "leaves — if this assertion fails, the premise behind removing the redundant " +
                "per-segment hover modifier from AeroSegmentedControl is false and Task 3's removal " +
                "must not be performed for it (WR-04)"
        )
    }

    @Test
    fun clickableEmitsHoverOnItsSuppliedInteractionSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        lateinit var hovered: State<Boolean>

        setContent {
            hovered = source.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .testTag("target")
                    .size(40.dp)
                    // Deliberately no explicit Modifier.hoverable( — clickable alone must emit hover.
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        onClick = {},
                    )
            )
        }
        waitForIdle()
        assertFalse(
            hovered.value,
            "hovered should start false — if this assertion fails, the premise behind removing the " +
                "redundant hover modifier from AeroListItem's clickable path is false and Task 3's " +
                "removal must not be performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            hovered.value,
            "clickable must emit hover on its supplied interaction source when the pointer enters — " +
                "if this assertion fails, the premise behind removing the redundant hover modifier " +
                "from AeroListItem's clickable path is false and Task 3's removal must not be " +
                "performed for it (WR-04)"
        )

        onNodeWithTag("target").performMouseInput { exit() }
        waitForIdle()
        assertFalse(
            hovered.value,
            "clickable must emit hover-exit on its supplied interaction source when the pointer " +
                "leaves — if this assertion fails, the premise behind removing the redundant hover " +
                "modifier from AeroListItem's clickable path is false and Task 3's removal must not " +
                "be performed for it (WR-04)"
        )
    }

    /**
     * WR-04 component-level proof: after removing [AeroSwitch]'s explicit hover emitter, hover
     * must still work — proven by driving real pointer input against the real component, not
     * asserted from a decompiled artifact. Note: this does not cover [AeroSegmentedControl]'s
     * per-segment hover — its segment sources are created inside the component and are not
     * reachable from a test; that component's coverage is the generic
     * [selectableEmitsHoverOnItsSuppliedInteractionSource] premise test plus the human
     * confirmation in plan 19-12.
     */
    @Test
    fun aeroSwitchStillReportsHoverWithNoExplicitHoverModifier() = runComposeUiTest {
        val source = MutableInteractionSource()
        lateinit var hovered: State<Boolean>

        setContent {
            AeroTheme {
                hovered = source.collectIsHoveredAsState()
                AeroSwitch(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.testTag("switch"),
                    interactionSource = source,
                )
            }
        }
        waitForIdle()

        onNodeWithTag("switch").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            hovered.value,
            "AeroSwitch must still report hover after its explicit hover emitter was removed — if " +
                "this fails, the emitter removal deleted a shipped behaviour (WR-04)"
        )

        onNodeWithTag("switch").performMouseInput { exit() }
        waitForIdle()
        assertFalse(
            hovered.value,
            "AeroSwitch must report hover-exit after its explicit hover emitter was removed — if " +
                "this fails, the emitter removal deleted a shipped behaviour (WR-04)"
        )
    }

    /**
     * WR-04 component-level proof for [AeroListItem]'s two hover paths: after moving the explicit
     * hover emitter into the else-branch of the click-handler condition, BOTH the clickable path
     * (a row given an `onClick`) and the display-only path (a row given none) must still report
     * hover — proven by driving real pointer input against each, not asserted from a decompiled
     * artifact.
     */
    @Test
    fun aeroListItemReportsHoverOnBothTheClickableAndTheDisplayOnlyPath() = runComposeUiTest {
        val clickableSource = MutableInteractionSource()
        val displayOnlySource = MutableInteractionSource()
        lateinit var clickableHovered: State<Boolean>
        lateinit var displayOnlyHovered: State<Boolean>

        setContent {
            AeroTheme {
                clickableHovered = clickableSource.collectIsHoveredAsState()
                displayOnlyHovered = displayOnlySource.collectIsHoveredAsState()
                Column {
                    AeroListItem(
                        text = "Clickable row",
                        onClick = {},
                        modifier = Modifier.testTag("clickableRow"),
                        interactionSource = clickableSource,
                    )
                    AeroListItem(
                        text = "Display-only row",
                        onClick = null,
                        modifier = Modifier.testTag("displayOnlyRow"),
                        interactionSource = displayOnlySource,
                    )
                }
            }
        }
        waitForIdle()

        onNodeWithTag("clickableRow").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            clickableHovered.value,
            "AeroListItem's clickable path must still report hover after the emitter rewiring — if " +
                "this fails, the emitter removal deleted a shipped behaviour (WR-04)"
        )
        onNodeWithTag("clickableRow").performMouseInput { exit() }
        waitForIdle()

        onNodeWithTag("displayOnlyRow").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(
            displayOnlyHovered.value,
            "AeroListItem's display-only path must still report hover — its explicit hover emitter " +
                "must be retained precisely because there is no interaction modifier on that path to " +
                "emit hover for it (WR-04)"
        )
        onNodeWithTag("displayOnlyRow").performMouseInput { exit() }
        waitForIdle()
        assertFalse(
            displayOnlyHovered.value,
            "AeroListItem's display-only path must report hover-exit after the pointer leaves (WR-04)"
        )
    }
}
