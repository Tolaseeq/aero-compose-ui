package com.mordred.aero.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
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
}
