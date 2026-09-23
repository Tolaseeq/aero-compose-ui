package com.mordred.aero.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.dropdown.AeroComboBox
import com.mordred.aero.components.dropdown.AeroDropdown
import com.mordred.aero.components.navigation.AeroMenuBar
import com.mordred.aero.components.navigation.AeroMenuItem
import com.mordred.aero.components.navigation.AeroTopLevelMenu
import com.mordred.aero.components.overlay.AeroContextMenuItem
import com.mordred.aero.components.overlay.AeroDrawer
import com.mordred.aero.components.overlay.AeroPopover
import com.mordred.aero.components.overlay.AeroTooltip
import com.mordred.aero.components.overlay.aeroContextMenu
import com.mordred.aero.components.pickers.AeroColorPickerButton
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.verification.pixelMapsDiffer
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * D-07: opened-state UI-test coverage for every component whose source contains `Popup(`, three
 * themes, captured on the pre-upgrade toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1) so
 * there is a before-baseline for the post-upgrade drift sweep. Popups are opened only through real
 * Compose UI-test input (`performClick`/`performMouseInput`/`performKeyInput`) — never
 * `java.awt.Robot` or any OS-level input.
 *
 * **Probe result (Task 1, empirical per RESEARCH.md Pitfall 5 / Open Question 1):** a closed
 * `AeroDropdown` has exactly one semantics root, so `onRoot().captureToImage()` (method M1) works
 * directly. The instant a `Popup` is open, Compose Multiplatform Desktop adds a second semantics
 * root (both report the full window bounds on this toolchain's `SkikoComposeUiTest`), and `onRoot()`
 * then throws `AssertionError` ("Expected exactly '1' node but found '2' nodes that satisfy:
 * (isRoot)") rather than picking one — so M1 alone cannot capture an opened popup. [captureOpened]
 * catches that `AssertionError` and falls back to M2 (`onAllNodes(isRoot())`, picking the
 * last-added root — the most recently composed `Popup` layer); that fallback's `captureToImage()`
 * rasterizes the same full-window Skia surface the main content paints onto (desktop popups are
 * layered onto one shared canvas, not a separate off-screen bitmap), so it captures the popup pixels
 * too. Every AeroDropdown test below confirms this: `pixelMapsDiffer(closed, opened)` is true, so
 * `captureOpened()` (M1 for single-root states, automatic M2 fallback for popup-open states) is the
 * method every other D-07 test in this file and in `D07PickerPopupCaptureTest` reuses.
 */
@OptIn(ExperimentalTestApi::class)
class D07MenuPopupCaptureTest {

    // ---------------------------------------------------------------------------------------
    // AeroDropdown — probe: decides M1 vs M2 vs D-08 empirically (Task 1).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDropdownAeroBlueOpened() = runDropdownOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDropdownAeroDarkOpened() = runDropdownOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDropdownClassicOpened() = runDropdownOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDropdownOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(480.dp, 360.dp).padding(16.dp)) {
                    AeroDropdown(
                        options = listOf("Alpha", "Beta", "Gamma"),
                        selected = "Beta",
                        onSelect = {},
                        modifier = Modifier.testTag("trigger"),
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performClick()
        waitForIdle()

        onNodeWithText("Gamma").assertExists(
            "D-07: AeroDropdown/$theme must expose its opened popup's \"Gamma\" option in the " +
                "semantics tree after a real click"
        )

        val opened = captureOpened()

        UiCapture.write("AeroDropdown", theme, "closed", closed)
        UiCapture.write("AeroDropdown", theme, "opened", opened)

        assertTrue(
            pixelMapsDiffer(closed.toPixelMap(), opened.toPixelMap()),
            "D-07: AeroDropdown/$theme's opened popup capture must differ from its closed capture " +
                "(captureOpened()'s M2 fallback — onAllNodes(isRoot()) — confirmed to reach the " +
                "popup layer once onRoot() throws for the second root a Popup adds)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // AeroComboBox — opened via a real click on the text field (shouldAutoOpen: focused with
    // any text, including empty, matches every option) (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroComboBoxAeroBlueOpened() = runComboBoxOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroComboBoxAeroDarkOpened() = runComboBoxOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroComboBoxClassicOpened() = runComboBoxOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runComboBoxOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                var text by remember { mutableStateOf("") }
                Box(Modifier.testTag("host").size(480.dp, 360.dp).padding(16.dp)) {
                    AeroComboBox(
                        text = text,
                        onTextChange = { text = it },
                        options = listOf("Alpha", "Beta", "Gamma"),
                        onOptionSelect = {},
                        modifier = Modifier.testTag("trigger"),
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performClick()
        waitForIdle()

        onNodeWithText("Gamma").assertExists(
            "D-07: AeroComboBox/$theme must expose a suggestion option after a real click focuses " +
                "the field"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroComboBox", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroContextMenu — opened via a real right-click (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroContextMenuAeroBlueOpened() = runContextMenuOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroContextMenuAeroDarkOpened() = runContextMenuOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroContextMenuClassicOpened() = runContextMenuOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runContextMenuOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(
                    Modifier
                        .testTag("area")
                        .size(220.dp, 120.dp)
                        .aeroContextMenu(
                            listOf(
                                AeroContextMenuItem.Action("Copy", {}),
                                AeroContextMenuItem.Action("Paste", {}),
                            )
                        )
                )
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("area").performMouseInput { rightClick(center) }
        waitForIdle()

        onNodeWithText("Paste").assertExists(
            "D-07: AeroContextMenu/$theme must expose its \"Paste\" item after a real right-click"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroContextMenu", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroMenuBar — opened via a real click on the "File" top-level label (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroMenuBarAeroBlueOpened() = runMenuBarOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroMenuBarAeroDarkOpened() = runMenuBarOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroMenuBarClassicOpened() = runMenuBarOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runMenuBarOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(480.dp, 360.dp)) {
                    AeroMenuBar(
                        menus = listOf(
                            AeroTopLevelMenu(
                                "File",
                                listOf(
                                    AeroMenuItem.Action("Open", {}),
                                    AeroMenuItem.Action("Exit", {}),
                                )
                            ),
                            AeroTopLevelMenu("Edit", listOf(AeroMenuItem.Action("Undo", {}))),
                        )
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithText("File").performClick()
        waitForIdle()

        onNodeWithText("Exit").assertExists(
            "D-07: AeroMenuBar/$theme must expose the \"File\" menu's \"Exit\" item after a real " +
                "click on the top-level label"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroMenuBar", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroTooltip — opened via a real hover held past the 600ms show delay (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroTooltipAeroBlueOpened() = runTooltipOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroTooltipAeroDarkOpened() = runTooltipOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroTooltipClassicOpened() = runTooltipOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runTooltipOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                Box(Modifier.testTag("host").size(480.dp, 360.dp).padding(16.dp)) {
                    AeroTooltip(text = "Tooltip text") {
                        AeroButton(text = "Hover me", onClick = {}, modifier = Modifier.testTag("trigger"))
                    }
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(700)
        waitForIdle()

        onNodeWithText("Tooltip text").assertExists(
            "D-07: AeroTooltip/$theme must show its text after a real hover held past the 600ms " +
                "show delay"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroTooltip", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroPopover — opened via a real click on an anchor-side button (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroPopoverAeroBlueOpened() = runPopoverOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroPopoverAeroDarkOpened() = runPopoverOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroPopoverClassicOpened() = runPopoverOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runPopoverOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                var expanded by remember { mutableStateOf(false) }
                Box(Modifier.testTag("host").size(640.dp, 400.dp).padding(16.dp)) {
                    AeroButton(
                        text = "Open",
                        onClick = { expanded = true },
                        modifier = Modifier.testTag("trigger"),
                    )
                    AeroPopover(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Text("Popover body")
                    }
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performClick()
        waitForIdle()

        onNodeWithText("Popover body").assertExists(
            "D-07: AeroPopover/$theme must show its body after a real click on the anchor button"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroPopover", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroDrawer — opened via a real click, animation advanced past the 220ms slide (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroDrawerAeroBlueOpened() = runDrawerOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroDrawerAeroDarkOpened() = runDrawerOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroDrawerClassicOpened() = runDrawerOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runDrawerOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                var open by remember { mutableStateOf(false) }
                Box(Modifier.testTag("host").size(640.dp, 400.dp)) {
                    AeroButton(
                        text = "Open drawer",
                        onClick = { open = true },
                        modifier = Modifier.testTag("trigger"),
                    )
                    AeroDrawer(open = open, onDismissRequest = { open = false }, drawerWidth = 240.dp) {
                        Text("Drawer body")
                    }
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performClick()
        mainClock.advanceTimeBy(400)
        waitForIdle()

        onNodeWithText("Drawer body").assertExists(
            "D-07: AeroDrawer/$theme must show its body after a real click, once the 220ms slide " +
                "animation has finished"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroDrawer", theme, closed, opened)
    }

    // ---------------------------------------------------------------------------------------
    // AeroColorPickerButton — opened via a real click on the swatch trigger (Task 2).
    // ---------------------------------------------------------------------------------------

    @Test
    fun aeroColorPickerButtonAeroBlueOpened() =
        runColorPickerButtonOpenedCapture("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun aeroColorPickerButtonAeroDarkOpened() =
        runColorPickerButtonOpenedCapture("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun aeroColorPickerButtonClassicOpened() =
        runColorPickerButtonOpenedCapture("Classic", AeroColorScheme.Classic)

    private fun runColorPickerButtonOpenedCapture(theme: String, scheme: AeroColorScheme) = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = scheme) {
                var value by remember { mutableStateOf(Color(0xFF3366CC)) }
                Box(Modifier.testTag("host").size(480.dp, 360.dp).padding(16.dp)) {
                    AeroColorPickerButton(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.testTag("trigger"),
                    )
                }
            }
        }
        waitForIdle()

        val closed = captureOpened()

        onNodeWithTag("trigger").performClick()
        waitForIdle()

        onNodeWithText("Original").assertExists(
            "D-07: AeroColorPickerButton/$theme must show its panel's \"Original\" label after a " +
                "real click on the swatch trigger"
        )

        val opened = captureOpened()

        assertOpenedDiffers("AeroColorPickerButton", theme, closed, opened)
    }
}
