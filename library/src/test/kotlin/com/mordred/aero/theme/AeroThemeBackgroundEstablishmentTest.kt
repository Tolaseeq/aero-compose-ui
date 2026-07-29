package com.mordred.aero.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * SHW-16 / VER-05 regression guard: [AeroTheme] must establish its own themed background so a
 * naive consumer that wraps bare content in `AeroTheme { }` — with no `Surface` of their own,
 * exactly the shape of the external VER-05 scratch consumer — gets the intended dark
 * [AeroColorScheme.background], not Compose Desktop's default light window background showing
 * through underneath.
 *
 * **Composed-node assertion, not a source scan.** Both halves below actually compose `AeroTheme`
 * around a tiny, non-filling `Box` and sample a real rasterized pixel far from that content via
 * `onRoot().captureToImage()` — the same `captureToImage()`/`toPixelMap()` idiom already proven in
 * [com.mordred.aero.components.range.AeroRangeSliderHoverCancellationTest]. `onRoot()` is used
 * (not a tagged node) specifically because the defect this guards against is about what paints the
 * FULL window canvas outside whatever `content` itself chooses to draw — a node-scoped capture of
 * `content`'s own bounds could never see that gap.
 *
 * **D-08 falsifiability proof, in this file.** Per this project's own v2.0.3
 * false-positive-sign-off lesson (a gate that cannot fail is a false pass), the RED half below does
 * not touch the source tree — it drives the same composed assertion through the real, shipped
 * `establishBackground = false` opt-out this fix introduces, proving the corner pixel is NOT
 * `colorScheme.background` whenever `AeroTheme` is told not to paint one. This demonstrates the
 * sampling methodology can actually detect an unpainted background rather than vacuously always
 * matching. The GREEN half exercises the real production default (`establishBackground` omitted
 * entirely) and is the assertion that would actually catch a future regression that silently
 * removes the `Surface` this plan added.
 */
@OptIn(ExperimentalTestApi::class)
class AeroThemeBackgroundEstablishmentTest {

    /** Tiny, non-filling content — never claims the full window canvas on its own. */
    @androidx.compose.runtime.Composable
    private fun BareUnfillingContent() {
        Box(Modifier.size(4.dp))
    }

    @Test
    fun defaultEstablishBackgroundPaintsThemeBackgroundBehindBareContent() = runComposeUiTest {
        setContent {
            // establishBackground omitted -> exercises the real production default (true).
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                BareUnfillingContent()
            }
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        // Bottom-right corner of the full window canvas: strictly outside the tiny 4dp content
        // Box, which Box's default TopStart alignment places at the origin.
        val sampled = pixels[pixels.width - 1, pixels.height - 1]

        assertEquals(
            AeroColorScheme.AeroBlue.background,
            sampled,
            "AeroTheme must paint colorScheme.background across the full window canvas behind " +
                "bare, unwrapped content (SHW-16/VER-05) - a naive consumer must not see " +
                "Compose Desktop's default light window background."
        )
    }

    @Test
    fun establishBackgroundFalseLeavesTheCornerUnpainted() = runComposeUiTest {
        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue, establishBackground = false) {
                BareUnfillingContent()
            }
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val sampled = pixels[pixels.width - 1, pixels.height - 1]

        assertNotEquals(
            AeroColorScheme.AeroBlue.background,
            sampled,
            "Falsifiability half (D-08): with establishBackground = false the corner must NOT be " +
                "AeroBlue.background, proving this test's sampling methodology can actually " +
                "detect an unpainted background rather than vacuously passing regardless of " +
                "what AeroTheme does."
        )
    }
}
