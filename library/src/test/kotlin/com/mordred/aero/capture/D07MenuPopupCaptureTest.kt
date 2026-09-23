package com.mordred.aero.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.dropdown.AeroDropdown
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
}

/**
 * Captures the current full-window state, including any open [androidx.compose.ui.window.Popup]
 * layer. Empirically decided by [D07MenuPopupCaptureTest]'s AeroDropdown probe (Task 1): M1
 * (`onRoot()`) is tried first and works whenever exactly one semantics root exists (no popup open);
 * M2 (`onAllNodes(isRoot())`, picking the most-recently-composed root) is the fallback M1's
 * `AssertionError` triggers once a `Popup` adds a second root. Shared by every D-07 test file
 * (menu/overlay and picker popups).
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.captureOpened(): ImageBitmap = try {
    onRoot().captureToImage()
} catch (unused: AssertionError) {
    val roots = onAllNodes(isRoot())
    val count = roots.fetchSemanticsNodes().size
    require(count > 0) { "D-07: captureOpened() found zero roots via onAllNodes(isRoot()) (M2)" }
    roots[count - 1].captureToImage()
}
