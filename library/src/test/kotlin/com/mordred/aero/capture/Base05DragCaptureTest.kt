package com.mordred.aero.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.datatable.AeroColumnWidth
import com.mordred.aero.components.datatable.AeroDataTable
import com.mordred.aero.components.datatable.AeroTableColumn
import com.mordred.aero.components.layout.AeroPanelGroup
import com.mordred.aero.components.layout.AeroSplitOrientation
import com.mordred.aero.components.layout.AeroSplitPane
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.verification.pixelMapsDiffer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * BASE-05: permanent drag-capture tests for [AeroSplitPane], [AeroPanelGroup] and
 * [AeroDataTable]'s column resize, in all three built-in themes (9 = 3 components x 3 themes).
 *
 * Drag is driven by the deterministic programmatic pattern proven in
 * [com.mordred.aero.components.layout.AeroPanelGroupRecomposeUiTest] (press, N x `moveTo` +
 * `waitForIdle()`, `release()`) — never `detectDragGestures`/gesture-detector helpers (PITFALL-03,
 * locked). Every `dropped` capture is compared against that same (component, theme)'s own
 * `default` capture via the shared [pixelMapsDiffer] (imported from
 * [com.mordred.aero.verification], reused verbatim).
 */
@OptIn(ExperimentalTestApi::class)
class Base05DragCaptureTest {

    // ------------------------------------------------------------------
    // AeroSplitPane
    // ------------------------------------------------------------------

    @Test
    fun aeroSplitPaneAeroBlueDrag() = runComposeUiTest {
        driveAeroSplitPane("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroSplitPaneAeroDarkDrag() = runComposeUiTest {
        driveAeroSplitPane("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroSplitPaneClassicDrag() = runComposeUiTest {
        driveAeroSplitPane("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroPanelGroup
    // ------------------------------------------------------------------

    @Test
    fun aeroPanelGroupAeroBlueDrag() = runComposeUiTest {
        driveAeroPanelGroup("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroPanelGroupAeroDarkDrag() = runComposeUiTest {
        driveAeroPanelGroup("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroPanelGroupClassicDrag() = runComposeUiTest {
        driveAeroPanelGroup("Classic", AeroColorScheme.Classic)
    }

    // ------------------------------------------------------------------
    // AeroDataTable (column resize)
    // ------------------------------------------------------------------

    @Test
    fun aeroDataTableAeroBlueDrag() = runComposeUiTest {
        driveAeroDataTable("AeroBlue", AeroColorScheme.AeroBlue)
    }

    @Test
    fun aeroDataTableAeroDarkDrag() = runComposeUiTest {
        driveAeroDataTable("AeroDark", AeroColorScheme.AeroDark)
    }

    @Test
    fun aeroDataTableClassicDrag() = runComposeUiTest {
        driveAeroDataTable("Classic", AeroColorScheme.Classic)
    }

    // ==================================================================
    // Drivers
    // ==================================================================

    private fun ComposeUiTest.driveAeroSplitPane(theme: String, scheme: AeroColorScheme) {
        val component = "AeroSplitPane"
        var lastFraction = -1f
        lateinit var density: Density
        setContent {
            AeroTheme(colorScheme = scheme) {
                density = LocalDensity.current
                Box(modifier = Modifier.testTag("capture").size(600.dp, 300.dp)) {
                    AeroSplitPane(
                        start = {
                            Box(Modifier.testTag("start").fillMaxSize().background(scheme.surface)) {
                                Text("Start", color = scheme.onSurface)
                            }
                        },
                        end = {
                            Box(Modifier.fillMaxSize().background(scheme.surface)) {
                                Text("End", color = scheme.onSurface)
                            }
                        },
                        modifier = Modifier.testTag("target").fillMaxSize(),
                        orientation = AeroSplitOrientation.Horizontal,
                        initialSplitFraction = 0.5f,
                        onSplitChange = { lastFraction = it },
                    )
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        val startRight = onNodeWithTag("start").fetchSemanticsNode().boundsInRoot.right
        val halfDividerPx = with(density) { 4.dp.toPx() }
        val dividerX = startRight + halfDividerPx
        val targetBounds = onNodeWithTag("target").fetchSemanticsNode().boundsInRoot
        val y = targetBounds.top + targetBounds.height / 2f

        val root = onRoot()
        root.performMouseInput {
            moveTo(Offset(dividerX, y))
            press()
        }
        repeat(10) { step ->
            root.performMouseInput { moveTo(Offset(dividerX + (step + 1) * 5f, y)) }
            waitForIdle()
        }
        capture(component, theme, "dragging")
        root.performMouseInput { release() }
        waitForIdle()
        val droppedImg = capture(component, theme, "dropped")

        assertTrue(
            lastFraction >= 0f && lastFraction != 0.5f,
            "$component/$theme: onSplitChange must report a fraction different from the initial 0.5f (got $lastFraction)",
        )
        assertTrue(pixelMapsDiffer(defaultImg, droppedImg), "$component/$theme: dropped capture must differ from default")
    }

    private fun ComposeUiTest.driveAeroPanelGroup(theme: String, scheme: AeroColorScheme) {
        val component = "AeroPanelGroup"
        var lastLayout: List<Float>? = null
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(modifier = Modifier.testTag("capture").size(900.dp, 400.dp)) {
                    AeroPanelGroup(
                        modifier = Modifier.testTag("target").fillMaxSize(),
                        orientation = Orientation.Horizontal,
                        initiallyExpanded = setOf("left", "center", "right"),
                        onLayoutChange = { lastLayout = it },
                    ) {
                        section(key = "left", title = "LeftPane", minSize = 220.dp, defaultSize = 250.dp) {
                            Text("Left content")
                        }
                        section(key = "center", title = "CenterPane", minSize = 120.dp, defaultSize = 250.dp) {
                            Text("Center content")
                        }
                        section(key = "right", title = "RightPane", minSize = 200.dp, defaultSize = 250.dp) {
                            Text("Right content")
                        }
                    }
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        // Divider between "left" and "center" sits just left of the CenterPane header strip —
        // same formula as AeroPanelGroupRecomposeUiTest's own repro.
        val centerLeft = onNodeWithText("CenterPane").fetchSemanticsNode().boundsInRoot.left
        val dividerX = centerLeft - 4f
        val targetBounds = onNodeWithTag("target").fetchSemanticsNode().boundsInRoot
        val y = targetBounds.top + targetBounds.height / 2f

        val root = onRoot()
        root.performMouseInput {
            moveTo(Offset(dividerX, y))
            press()
        }
        repeat(10) { step ->
            root.performMouseInput { moveTo(Offset(dividerX + (step + 1) * 5f, y)) }
            waitForIdle()
        }
        capture(component, theme, "dragging")
        root.performMouseInput { release() }
        waitForIdle()
        val droppedImg = capture(component, theme, "dropped")

        val layout = lastLayout
        assertTrue(layout != null, "$component/$theme: onLayoutChange must fire at drag-end")
        assertTrue(
            layout.orEmpty().toSet().size > 1,
            "$component/$theme: onLayoutChange's reported sizes must have changed away from the " +
                "uniform initial equal-defaultSize split (got $layout)",
        )
        assertTrue(pixelMapsDiffer(defaultImg, droppedImg), "$component/$theme: dropped capture must differ from default")

        // RCMP invariant: each section header still occurs exactly once after the drag.
        val leftCount = onAllNodesWithText("LeftPane").fetchSemanticsNodes().size
        val centerCount = onAllNodesWithText("CenterPane").fetchSemanticsNodes().size
        val rightCount = onAllNodesWithText("RightPane").fetchSemanticsNodes().size
        assertTrue(leftCount == 1, "$component/$theme: LeftPane header must occur exactly once (got $leftCount)")
        assertTrue(centerCount == 1, "$component/$theme: CenterPane header must occur exactly once (got $centerCount)")
        assertTrue(rightCount == 1, "$component/$theme: RightPane header must occur exactly once (got $rightCount)")
    }

    private data class DragTableRow(val id: Int, val name: String, val amount: Int)

    private fun ComposeUiTest.driveAeroDataTable(theme: String, scheme: AeroColorScheme) {
        val component = "AeroDataTable"
        val rows = (1..5).map { DragTableRow(it, "Row $it", it * 10) }
        val columns = listOf(
            AeroTableColumn<DragTableRow>(
                header = "Col1",
                width = AeroColumnWidth.Fixed(150.dp),
                cell = { Text(it.name) },
            ),
            AeroTableColumn<DragTableRow>(
                header = "Col2",
                width = AeroColumnWidth.Fixed(150.dp),
                cell = { Text(it.amount.toString()) },
            ),
            AeroTableColumn<DragTableRow>(
                header = "Col3",
                width = AeroColumnWidth.Weight(1f),
                cell = { Text(it.id.toString()) },
            ),
        )
        lateinit var density: Density
        setContent {
            AeroTheme(colorScheme = scheme) {
                density = LocalDensity.current
                Box(modifier = Modifier.testTag("capture").size(600.dp, 300.dp)) {
                    AeroDataTable(
                        data = rows,
                        columns = columns,
                        key = { it.id },
                        modifier = Modifier.testTag("target").fillMaxSize(),
                    )
                }
            }
        }
        waitForIdle()

        val defaultImg = capture(component, theme, "default")

        // Resize handle sits at the right edge of the first column's cell, inset by half the
        // splitter's own 8dp hit-width (AeroTableHeader.kt: Modifier.width(8.dp).align(CenterEnd)
        // inside the column's Box, so its center is 4dp inside the column's right edge). Column 1
        // is AeroColumnWidth.Fixed(150.dp) with plenty of total width (600dp) to honor it exactly,
        // so this analytic position is exact rather than approximate.
        val bounds = onNodeWithTag("target").fetchSemanticsNode().boundsInRoot
        val col1WidthPx = with(density) { 150.dp.toPx() }
        val handleInsetPx = with(density) { 4.dp.toPx() }
        val handleX = bounds.left + col1WidthPx - handleInsetPx
        val rowHeightPx = with(density) { 36.dp.toPx() }
        val handleY = bounds.top + rowHeightPx / 2f

        val secondHeaderLeftBefore = onNodeWithText("Col2").fetchSemanticsNode().boundsInRoot.left

        val root = onRoot()
        root.performMouseInput {
            moveTo(Offset(handleX, handleY))
            press()
        }
        repeat(6) { step ->
            root.performMouseInput { moveTo(Offset(handleX + (step + 1) * 10f, handleY)) }
            waitForIdle()
        }
        capture(component, theme, "dragging")
        root.performMouseInput { release() }
        waitForIdle()
        val droppedImg = capture(component, theme, "dropped")

        val secondHeaderLeftAfter = onNodeWithText("Col2").fetchSemanticsNode().boundsInRoot.left
        assertTrue(
            secondHeaderLeftAfter > secondHeaderLeftBefore,
            "$component/$theme: Col2's left edge must move right after widening Col1 by drag " +
                "(before=$secondHeaderLeftBefore, after=$secondHeaderLeftAfter)",
        )
        assertTrue(pixelMapsDiffer(defaultImg, droppedImg), "$component/$theme: dropped capture must differ from default")
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.capture(component: String, theme: String, state: String) =
    onNodeWithTag("capture").captureToImage().let { img ->
        UiCapture.write(component, theme, state, img)
        img.toPixelMap()
    }
