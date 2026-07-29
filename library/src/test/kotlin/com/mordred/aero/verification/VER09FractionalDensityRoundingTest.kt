package com.mordred.aero.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.theme.AeroTheme
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * VER-09 gate (20-08-PLAN.md Task 4): does [AeroSegmentedControl]'s 1dp outer
 * `.border(1.dp, colors.borderDefault, shape)` survive fractional-density rounding — remaining
 * present and a SINGLE band, neither collapsed to zero device pixels nor doubled — at the two
 * fractional/high-DPI scales Windows actually ships (125%, 200%), compared against the 100%
 * baseline VER-03 already pins its own measurements at.
 *
 * **What this asserts: proportionality, not absolute pixel counts (20-08-PLAN.md correctness
 * note).** The question is never "how many device pixels wide is the border at 125%" in
 * isolation — that legitimately changes with density and asserting it verbatim would be a brittle
 * gate that fails for the wrong reason. The question is whether the SAME nominal 1dp contour
 * remains a single present band, bounded by the density-scaled stroke width plus one
 * anti-aliasing pixel of slack, at every scale tested — and whether the control's own dp-space
 * height (28.dp, VER-03's pinned baseline) reproduces exactly once converted back out of device
 * pixels, at every scale tested.
 *
 * **LIMIT — read before trusting this gate for anything beyond what it covers.**
 * [CompositionLocalProvider] substituting [LocalDensity] simulates a fractional/high density in
 * Compose's LAYOUT math only. It does NOT exercise the Windows compositor's own fractional
 * scaling of the final rendered surface, Skia rasterisation at that physical scale, or font
 * hinting — none of those run inside a JVM-hosted `runComposeUiTest`. Any artifact that only
 * appears once the compositor itself scales a rendered window (as opposed to Compose laying out
 * against a denser virtual pixel grid) is outside this gate's reach and stays with the human DPI
 * pass in plan 20-07.
 *
 * **Self-referencing colour measurement, scheme-independent.** [borderRunLengthPx] never hardcodes
 * a theme's alpha-blended `borderDefault` composite value (which depends on whatever the border is
 * drawn over). It instead reads the pixel actually rendered at the true left edge of the captured
 * node (column 0, definitionally fully-covered by the border stroke, never anti-aliased since it
 * coincides with the capture's own physical boundary) and measures how many consecutive pixels
 * from that edge remain within [COLOR_TOLERANCE] of it before the interior segment fill begins.
 *
 * **D-08 fixture proof, in this file** ([borderRunLengthPxDetectsPresenceCollapseAndDoubling]) —
 * proves the pure pixel-run counter this gate's real assertion is built on can tell apart a
 * present-and-single band, a collapsed (zero-width) band, and an implausibly wide "doubled" band,
 * so a gate that could never fail on any of those three shapes is not silently passing here
 * (VER-06).
 */
@OptIn(ExperimentalTestApi::class)
class VER09FractionalDensityRoundingTest {

    // ---------------------------------------------------------------------------------------
    // D-08 fixture proof — same leadingRun path the real per-density assertion below uses.
    // ---------------------------------------------------------------------------------------

    @Test
    fun borderRunLengthPxDetectsPresenceCollapseAndDoubling() {
        val border = Color.Black
        val fill = Color.White

        val presentAndSingle = listOf(border, fill, fill, fill, fill)
        assertEquals(
            1,
            leadingRunLength(presentAndSingle, target = presentAndSingle.first(), tolerance = COLOR_TOLERANCE),
            "D-08 fixture: a one-pixel border band followed by fill must measure exactly 1 (present, single)"
        )

        val collapsed = listOf(fill, fill, fill, fill, fill)
        assertEquals(
            0,
            leadingRunLength(collapsed, target = border, tolerance = COLOR_TOLERANCE),
            "D-08 fixture: a border rounded away to zero width (edge pixel already matches fill, " +
                "not the border reference) must measure 0 — the collapse this gate exists to catch"
        )

        val doubled = listOf(border, border, border, border, fill)
        assertEquals(
            4,
            leadingRunLength(doubled, target = doubled.first(), tolerance = COLOR_TOLERANCE),
            "D-08 fixture: an implausibly wide four-pixel band must measure 4, proving the counter " +
                "distinguishes a doubled/over-wide contour from a single-pixel one rather than " +
                "collapsing every non-zero case to the same verdict"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real component, real densities.
    // ---------------------------------------------------------------------------------------

    @Test
    fun oneDpBorderContourAndPinnedHeightSurviveFractionalDensity() {
        val densities = listOf(1f, 1.25f, 2f)

        for (density in densities) {
            var borderRunPx = -1
            var measuredHeightDp = (-1f).dp

            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(density)) {
                        AeroTheme {
                            Box {
                                AeroSegmentedControl(
                                    options = listOf("A", "B"),
                                    selected = "A",
                                    onSelect = {},
                                    modifier = Modifier.testTag("segctrl"),
                                )
                            }
                        }
                    }
                }
                waitForIdle()

                val node = onNodeWithTag("segctrl")
                measuredHeightDp = node.getUnclippedBoundsInRoot().height

                val pixelMap = node.captureToImage().toPixelMap()
                val midY = pixelMap.height / 2
                val row = (0 until pixelMap.width).map { x -> pixelMap[x, midY] }
                // Column 0 sits exactly on the layout node's true left edge — fully covered by
                // the border stroke, never anti-aliased, so it is always a faithful border
                // reference regardless of which color scheme's borderDefault composite it is.
                borderRunPx = leadingRunLength(row, target = row.first(), tolerance = COLOR_TOLERANCE)
            }

            val expectedNominalPx = with(Density(density)) { 1.dp.toPx() }
            val maxAllowedPx = ceil(expectedNominalPx).toInt() + 1

            assertTrue(
                borderRunPx >= 1,
                "density=$density: AeroSegmentedControl's 1dp border must remain present (>=1 " +
                    "device pixel) — measured $borderRunPx, a collapsed contour rounds this to 0"
            )
            assertTrue(
                borderRunPx <= maxAllowedPx,
                "density=$density: AeroSegmentedControl's 1dp border must not double past the " +
                    "density-scaled stroke width plus one AA pixel of slack (expected <= " +
                    "$maxAllowedPx px at this density) — measured $borderRunPx"
            )

            // Proportionality on the OTHER axis: the control's own dp-space height (VER-03's
            // pinned 28.dp baseline) must reproduce exactly once converted back out of whatever
            // device-pixel grid this density rounded it to — not merely close.
            assertTrue(
                abs(28.dp.value - measuredHeightDp.value) <= 0.05f,
                "density=$density: AeroSegmentedControl's measured height must still resolve to " +
                    "28.dp (VER-03's pinned baseline, +/-0.05dp) once converted back out of " +
                    "device pixels — measured $measuredHeightDp"
            )
        }
    }
}

/** Colour-distance tolerance (Euclidean over RGB, [0,1] channel range) below which two pixels are
 * considered the "same" reference colour — generous enough to absorb a single anti-aliased
 * blend step, tight enough that the interior segment fill (a materially different colour in
 * every built-in scheme) never satisfies it. */
private const val COLOR_TOLERANCE: Float = 0.08f

/**
 * Counts how many consecutive pixels from the START of [pixels] remain within [tolerance] of
 * [target], stopping at the first pixel that diverges (or at the end of the list). Pure function
 * — no Compose runtime — so the D-08 fixture proof above and the real per-density measurement
 * exercise exactly the same code path (VER-06).
 */
internal fun leadingRunLength(pixels: List<Color>, target: Color, tolerance: Float): Int {
    var count = 0
    for (pixel in pixels) {
        if (colorDistance(pixel, target) <= tolerance) count++ else break
    }
    return count
}

private fun colorDistance(a: Color, b: Color): Float {
    val dr = a.red - b.red
    val dg = a.green - b.green
    val db = a.blue - b.blue
    return sqrt(dr * dr + dg * dg + db * db)
}
