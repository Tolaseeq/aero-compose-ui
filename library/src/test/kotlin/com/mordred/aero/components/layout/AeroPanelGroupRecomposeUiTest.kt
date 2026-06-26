package com.mordred.aero.components.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reproduces the RCMP recompose-during-drag header duplication: a horizontal CONTROLLED
 * AeroPanelGroup whose one section's content recomposes independently while the panel is
 * interacted with. Before the fix this rendered each section's header strip N times
 * (3 sections -> 9 strips). PanelGroupLogicTest cannot catch this — it needs a real composition.
 *
 * Mirrors the failing consumer config: controlled + Orientation.Horizontal + per-section minSize
 * + defaultSize + onLayoutChange.
 */
@OptIn(ExperimentalTestApi::class)
class AeroPanelGroupRecomposeUiTest {

    /** A: independent content recompose alone (no drag) — does it duplicate headers? */
    @Test
    fun independentRecomposeDoesNotDuplicateHeaders() = runComposeUiTest {
        val tickState = mutableStateOf(0)
        setContent {
            var expanded by remember { mutableStateOf(setOf("left", "center", "right")) }
            ControlledHorizontalPanel(expanded, { expanded = it }, tickState)
        }
        waitForIdle()
        assertHeaderCounts("baseline", 1)

        repeat(10) {
            runOnUiThread { tickState.value++ }
            waitForIdle()
        }
        assertHeaderCounts("after independent recompose", 1)
    }

    /**
     * B: drag a divider while content recomposes independently between moves (the RCMP repro).
     * Before the fix this re-appended sections into the persisted DSL scope, rendering each
     * section's header strip N× (3 -> 9, 12, ... per drag move). With the non-@Composable DSL
     * lambda the section list is rebuilt once per recompose and stays at exactly 3.
     */
    @Test
    fun dragWithIndependentRecomposeDoesNotDuplicateHeaders() = runComposeUiTest {
        val tickState = mutableStateOf(0)
        setContent {
            var expanded by remember { mutableStateOf(setOf("left", "center", "right")) }
            ControlledHorizontalPanel(expanded, { expanded = it }, tickState)
        }
        waitForIdle()
        assertHeaderCounts("baseline", 1)

        // Divider between section 0 and 1 sits just left of the CenterPane header strip.
        val centerLeft = onNodeWithText("CenterPane").fetchSemanticsNode().boundsInRoot.left
        val dividerX = centerLeft - 4f
        val y = 200f

        val root = onRoot()
        root.performMouseInput {
            moveTo(Offset(dividerX, y))
            press()
        }
        repeat(10) { step ->
            root.performMouseInput {
                moveTo(Offset(dividerX + step * 5f, y))
            }
            runOnUiThread { tickState.value++ }   // independent recompose lands mid-drag
            waitForIdle()
        }
        root.performMouseInput { release() }
        waitForIdle()

        assertHeaderCounts("after drag + recompose", 1)
    }

    private fun ComposeUiTest.assertHeaderCounts(stage: String, expected: Int) {
        val left = onAllNodesWithText("LeftPane").fetchSemanticsNodes().size
        val center = onAllNodesWithText("CenterPane").fetchSemanticsNodes().size
        val right = onAllNodesWithText("RightPane").fetchSemanticsNodes().size
        assertEquals(expected, left, "$stage: LeftPane header count (got $left)")
        assertEquals(expected, center, "$stage: CenterPane header count (got $center)")
        assertEquals(expected, right, "$stage: RightPane header count (got $right)")
    }
}

@androidx.compose.runtime.Composable
private fun ControlledHorizontalPanel(
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit,
    tickState: androidx.compose.runtime.State<Int>,
) {
    // Read tick at the panel level -> AeroPanelGroup recomposes while a drag is active, exactly
    // like a real consumer whose hosting screen recomposes mid-drag (VM observables, animations).
    @Suppress("UNUSED_VARIABLE")
    val recomposeTrigger = tickState.value
    Box(Modifier.size(900.dp, 400.dp)) {
        AeroPanelGroup(
            modifier = Modifier.fillMaxSize(),
            orientation = Orientation.Horizontal,
            expandedKeys = expanded,
            onExpandedChange = onExpandedChange,
            onLayoutChange = { /* mirrors consumer config */ },
        ) {
            section(key = "left", title = "LeftPane", minSize = 220.dp, defaultSize = 250.dp) {
                // Reads tickState.value DEEP inside content only -> recomposes the content
                // independently, WITHOUT recomposing AeroPanelGroup (faithful to consumer panels
                // that read live VM state internally).
                Text("Live ${tickState.value}")
            }
            section(key = "center", title = "CenterPane", minSize = 120.dp, defaultSize = 250.dp) {
                Text("Static center")
            }
            section(key = "right", title = "RightPane", minSize = 200.dp, defaultSize = 250.dp) {
                Text("Static right")
            }
        }
    }
}
