package com.mordred.aero.capture

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onRoot
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.verification.pixelMapsDiffer
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertTrue

/**
 * Opt-in PNG writer shared by every BASE-05/D-07 capture test (D-03).
 *
 * [dir] is `null` unless the `aero.captureDir` system property is explicitly passed (forwarded
 * from the `-Paero.captureDir=<path>` Gradle property by `library/build.gradle.kts`'s
 * `tasks.test` block) — so an ordinary `./gradlew test` run never writes into the working tree.
 */
internal object UiCapture {

    /** Root directory for this run's images, or `null` when opt-in was not requested (D-03). */
    val dir: File? = System.getProperty("aero.captureDir")?.let(::File)

    /**
     * Writes [image] to `<dir>/<component>/<theme>/<state>.png`, creating parent directories as
     * needed. No-op when [dir] is `null` — the default, non-opt-in path (D-03).
     */
    fun write(component: String, theme: String, state: String, image: ImageBitmap) {
        val root = dir ?: return
        val target = File(File(root, component), theme)
        target.mkdirs()
        val file = File(target, "$state.png")
        ImageIO.write(image.toAwtImage(), "png", file)
    }

    /** The three built-in color schemes, in the order every capture test iterates them. */
    val SCHEMES: List<Pair<String, AeroColorScheme>> = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic,
    )
}

/**
 * Captures the current full-window state, including any open [androidx.compose.ui.window.Popup]
 * layer (D-07). Empirically decided by `D07MenuPopupCaptureTest`'s AeroDropdown probe (Plan
 * 21-03 Task 1): M1 (`onRoot()`) is tried first and works whenever exactly one semantics root
 * exists (no popup open); M2 (`onAllNodes(isRoot())`, picking the most-recently-composed root) is
 * the fallback M1's `AssertionError` triggers once a `Popup` adds a second root. Shared by every
 * D-07 test file (menu/overlay and picker popups).
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

/**
 * Writes both captures via [UiCapture.write] and asserts the opened capture actually differs from
 * the closed one — the shared D-07 assertion every popup test (menu/overlay and picker) reuses.
 */
internal fun assertOpenedDiffers(component: String, theme: String, closed: ImageBitmap, opened: ImageBitmap) {
    UiCapture.write(component, theme, "closed", closed)
    UiCapture.write(component, theme, "opened", opened)
    assertTrue(
        pixelMapsDiffer(closed.toPixelMap(), opened.toPixelMap()),
        "D-07: $component/$theme's opened popup capture must differ from its closed capture"
    )
}
