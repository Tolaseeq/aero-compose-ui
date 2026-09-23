package com.mordred.aero.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * BASE-05 first-step proof: [androidx.compose.ui.test.captureToImage] must be proven to work in
 * desktop `runComposeUiTest` on this toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1)
 * BEFORE any other capture test is written. Per the locked BASE-05 stop rule, if either test below
 * throws, is unsupported, or returns a blank/zero-size image, execution must stop and ask the
 * maintainer — no substitute capture mechanism may be used.
 *
 * Both tests additionally exercise [UiCapture.write] under the D-03 opt-in writer, proving the
 * two mechanisms this entire phase's BASE-05/D-07 tests depend on together, in one place, first.
 */
@OptIn(ExperimentalTestApi::class)
class Base05CaptureProofTest {

    @Test
    fun captureToImageWorksOnTaggedNode() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                Box(modifier = Modifier.testTag("capture").size(200.dp, 60.dp)) {
                    AeroButton(text = "Label", onClick = {})
                }
            }
        }
        waitForIdle()

        val image = onNodeWithTag("capture").captureToImage()
        assertTrue(image.width > 0, "BASE-05 proof: tagged-node capture must have non-zero width")
        assertTrue(image.height > 0, "BASE-05 proof: tagged-node capture must have non-zero height")
        val pixels = image.toPixelMap()
        assertTrue(
            distinctColorCount(pixels) >= 2,
            "BASE-05 proof: tagged-node capture must not be blank — it must contain at least 2 " +
                "distinct colours (AeroButton's fill against its background)",
        )
        UiCapture.write("Base05Proof", "AeroBlue", "tagged", image)
    }

    @Test
    fun captureToImageWorksOnRoot() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                Box(modifier = Modifier.testTag("capture").size(200.dp, 60.dp)) {
                    AeroButton(text = "Label", onClick = {})
                }
            }
        }
        waitForIdle()

        val image = onRoot().captureToImage()
        assertTrue(image.width > 0, "BASE-05 proof: root capture must have non-zero width")
        assertTrue(image.height > 0, "BASE-05 proof: root capture must have non-zero height")
        val pixels = image.toPixelMap()
        assertTrue(
            distinctColorCount(pixels) >= 2,
            "BASE-05 proof: root capture must not be blank — it must contain at least 2 distinct " +
                "colours (the themed background plus AeroButton's fill)",
        )
        UiCapture.write("Base05Proof", "AeroBlue", "root", image)
    }
}

/** Counts distinct colours present in [pixels], stopping early once 2 are found (cheap enough). */
private fun distinctColorCount(pixels: PixelMap): Int {
    val seen = HashSet<Int>()
    for (y in 0 until pixels.height) {
        for (x in 0 until pixels.width) {
            seen.add(pixels[x, y].toArgb())
            if (seen.size >= 2) return seen.size
        }
    }
    return seen.size
}
