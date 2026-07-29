package com.mordred.aero.verification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * VER-08 / SHW-16 gate: [AeroSegmentedControl]'s selection-animated label colour must flip AT MOST
 * ONCE across the ~150ms selection tween — converting the mechanically-checkable half of the
 * SHW-16 sign-off checklist's label-transition row into a permanent, re-executed gate
 * (20-08-PLAN.md Task 2).
 *
 * **The flip itself is expected, not the defect.** `resolveLabelColor` derives the label from the
 * CURRENTLY-ANIMATING fill (`AeroButtonSurface.kt`), so the label legitimately flips once the fill
 * crosses the contrast midpoint partway through the tween. The defect this gate guards against is
 * an OSCILLATING label — flipping back and forth — which at ~150ms the eye cannot resolve from a
 * single clean flip (20-08-PLAN.md `<critical_correctness_note>`).
 *
 * **Real animation, real clock, real click.** [androidx.compose.ui.test.ComposeUiTest.mainClock] is
 * set to `autoAdvance = false` before the segment is clicked, so the real
 * `animateFloatAsState(tween(150, LinearEasing))` inside [AeroSegmentedControl] starts but does not
 * advance on its own; [FRAME_COUNT] manual `mainClock.advanceTimeByFrame()` steps (240ms of frame
 * budget against a 150ms animation, comfortably past settling) then walk it forward one frame at a
 * time, sampling the REAL rendered segment after every step via `captureToImage()` — never a
 * reimplementation of the tween's own time-to-value math, which would assert this test's model of
 * the animation instead of the component's actual wiring.
 *
 * **Sampling methodology.** Each frame's capture is scanned for the pixel closest to either
 * luminance extreme (0 or 1) within a small patch centered on the captured segment image —
 * [resolveLabelColor] returns only ever exactly [androidx.compose.ui.graphics.Color.Black] or
 * [androidx.compose.ui.graphics.Color.White] (`LABEL_CANDIDATE_DARK`/`LABEL_CANDIDATE_LIGHT`,
 * `AeroButtonSurface.kt`), and none of this library's shipped fill colours are anywhere near either
 * extreme, so the most-extreme pixel in the patch is always the label's own ink, never a stray fill
 * pixel, regardless of exactly where within the patch the glyph's solid interior happens to fall.
 * That extreme pixel is then bucketed to the nearer of pure black/white via a luminance-0.5
 * threshold, giving a clean two-valued sample per frame immune to sub-pixel anti-aliasing noise at
 * the glyph's own edges.
 *
 * **D-08 fixture proof, in this file** ([countLabelColorChangesFlagsAnOscillatingSequence] /
 * [countLabelColorChangesAcceptsAMonotoneSequence]), following the convention established by
 * [VER01GradientProportionalitySourceTest] — a gate that cannot fail is a false pass (VER-06). Both
 * fixtures exercise [countLabelColorChanges] directly, the exact same function the real per-scheme
 * assertions below call on their real sampled sequences.
 *
 * **Boundary.** This gate is mechanical byte-level differencing over a REAL running animation, not
 * an aesthetic judgment — whether the flip itself looks acceptable at real frame rate on real
 * hardware remains a human question 20-07 exists for (SHW-16).
 */
@OptIn(ExperimentalTestApi::class)
class VER08SegmentLabelFlipTest {

    // ---------------------------------------------------------------------------------------
    // D-08 fixture proof — same countLabelColorChanges function the real assertions use below.
    // ---------------------------------------------------------------------------------------

    @Test
    fun countLabelColorChangesFlagsAnOscillatingSequence() {
        val oscillating = listOf(
            Color.Black, Color.Black, Color.White, Color.Black, Color.White, Color.White,
        )
        val changes = countLabelColorChanges(oscillating)
        assertTrue(
            changes > 1,
            "D-08 fixture proof: a synthetic sequence that flips back and forth " +
                "(black,black,white,black,white,white) must be counted as MORE than one change " +
                "($changes counted) — proves countLabelColorChanges can actually detect the " +
                "oscillation defect this gate exists to catch (VER-08)"
        )
    }

    @Test
    fun countLabelColorChangesAcceptsAMonotoneSequence() {
        val monotone = listOf(
            Color.Black, Color.Black, Color.Black, Color.White, Color.White, Color.White,
        )
        val changes = countLabelColorChanges(monotone)
        assertTrue(
            changes <= 1,
            "D-08 fixture proof: a synthetic sequence that flips exactly once " +
                "(black,black,black,white,white,white) must be counted as AT MOST one change " +
                "($changes counted) — proves countLabelColorChanges does not vacuously reject the " +
                "expected, correct single flip (VER-08)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real animation, all three schemes.
    // ---------------------------------------------------------------------------------------

    @Test
    fun labelFlipsAtMostOnceAcrossSelectionAnimationInAeroBlue() =
        runSegmentLabelFlipTest("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun labelFlipsAtMostOnceAcrossSelectionAnimationInAeroDark() =
        runSegmentLabelFlipTest("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun labelFlipsAtMostOnceAcrossSelectionAnimationInClassic() =
        runSegmentLabelFlipTest("Classic", AeroColorScheme.Classic)

    private fun runSegmentLabelFlipTest(schemeName: String, scheme: AeroColorScheme) =
        runComposeUiTest {
            mainClock.autoAdvance = false
            val selectedState = mutableStateOf(0)

            setContent {
                AeroTheme(colorScheme = scheme) {
                    AeroSegmentedControl(
                        options = listOf(0, 1),
                        selected = selectedState.value,
                        onSelect = { selectedState.value = it },
                        optionLabel = { LABEL_GLYPH },
                    )
                }
            }
            waitForIdle()

            // The second segment (index 1) — currently unselected. Located by its glyph text via
            // onAllNodesWithText, disambiguated by index since both segments render the identical
            // solid-block glyph (VER-08 sampling methodology, see this class's KDoc).
            val target = onAllNodesWithText(LABEL_GLYPH).get(1)

            target.performMouseInput {
                moveTo(center)
                press()
                release()
            }
            waitForIdle()

            val samples = mutableListOf<Color>()
            repeat(FRAME_COUNT) {
                mainClock.advanceTimeByFrame()
                waitForIdle()
                val pixels = target.captureToImage().toPixelMap()
                samples.add(bucketizeToNearestExtreme(extremeLuminancePixelNearCenter(pixels)))
            }

            assertTrue(
                samples.isNotEmpty(),
                "VER-08 ($schemeName): sampled frame count must be non-zero — a clock that never " +
                    "advanced must not be able to pass this gate vacuously"
            )

            val changes = countLabelColorChanges(samples)
            assertTrue(
                changes <= 1,
                "VER-08 ($schemeName): the segment label must change colour AT MOST ONCE across " +
                    "the whole selection animation ($changes changes counted across " +
                    "${samples.size} sampled frames) — an oscillating label is the actual defect " +
                    "the eye cannot resolve at ~150ms, per this class's KDoc"
            )
        }
}

/** Solid-block glyph (U+2588 FULL BLOCK) both segments render — see this file's sampling KDoc. */
private const val LABEL_GLYPH = "█"

/**
 * Manual clock steps walked with `mainClock.autoAdvance = false` — 240ms of frame budget (at the
 * clock's own per-frame duration) against the shipped 150ms `tween`, comfortably past settling so
 * the sampled sequence's tail is guaranteed to have reached the animation's resting value.
 */
private const val FRAME_COUNT: Int = 15

/**
 * Scans a small patch centered on [pixels] for the pixel whose luminance sits closest to either
 * extreme (0 or 1) — see this file's class KDoc for why this is always the label's own ink rather
 * than a stray fill pixel, regardless of exactly where the glyph's solid interior falls within the
 * patch.
 */
internal fun extremeLuminancePixelNearCenter(pixels: PixelMap): Color {
    val cx = pixels.width / 2
    val cy = pixels.height / 2
    val radius = maxOf(1, minOf(6, cx, cy) - 1)
    var best: Color? = null
    var bestDistanceToExtreme = Float.MAX_VALUE
    for (dy in -radius..radius) {
        for (dx in -radius..radius) {
            val x = cx + dx
            val y = cy + dy
            if (x !in 0 until pixels.width || y !in 0 until pixels.height) continue
            val lum = pixels[x, y].luminance()
            val distanceToExtreme = minOf(lum, 1f - lum)
            if (distanceToExtreme < bestDistanceToExtreme) {
                bestDistanceToExtreme = distanceToExtreme
                best = pixels[x, y]
            }
        }
    }
    return best ?: error("extremeLuminancePixelNearCenter sampled zero pixels (VER-08)")
}

/**
 * Buckets a sampled colour to whichever of pure black/white it sits closer to (luminance 0.5
 * threshold) — immune to sub-pixel anti-aliasing noise at the glyph's own edges, since
 * [resolveLabelColor][com.mordred.aero.components.buttons.resolveLabelColor] only ever returns
 * exactly one of those two values.
 */
internal fun bucketizeToNearestExtreme(color: Color): Color =
    if (color.luminance() < 0.5f) Color.Black else Color.White

/**
 * Counts the number of colour CHANGES between consecutive samples — a monotone single-flip
 * sequence (e.g. black,black,white,white) counts 1; an oscillating sequence (e.g.
 * black,white,black) counts 2 or more. Pure function, no Compose dependency — the exact same
 * function the D-08 fixture proof above and the real per-scheme assertions both call.
 */
internal fun countLabelColorChanges(samples: List<Color>): Int {
    var changes = 0
    for (i in 1 until samples.size) {
        if (samples[i] != samples[i - 1]) changes++
    }
    return changes
}
