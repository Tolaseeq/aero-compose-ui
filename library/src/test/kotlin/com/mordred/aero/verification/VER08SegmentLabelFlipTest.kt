package com.mordred.aero.verification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * VER-08 / SHW-16 gate: [AeroSegmentedControl]'s label colour must NEVER change — across the
 * ~150ms selection tween AND across hover, press, focus and disabled — strengthened at 20-09
 * (SHW-16/VER-06) from this file's original "at most one change across the selection animation"
 * bar (20-08-PLAN.md Task 2).
 *
 * **Why the bar tightened from "at most one" to "never".** The original bar accepted a single
 * flip as legitimate: `resolveLabelColor` derived the label from the CURRENTLY-ANIMATING fill, so
 * it legitimately crossed the contrast midpoint once mid-tween. That per-call-site mechanism is
 * retired (20-09) — [AeroColorScheme.labelOnFilledSurface] is now a fixed property of the scheme,
 * read directly by the segment `Text`, never recomputed from the fill. The maintainer's 20-08
 * checkpoint rule is explicit: "per theme, ONE text colour, not per element, not per state." A
 * label that flips even once — whether across the animation, or between rest/hover/press/focus/
 * disabled — is now a real regression, not a tolerated legitimate transition.
 *
 * **Real animation, real clock, real click** (unchanged from the original gate).
 * [androidx.compose.ui.test.ComposeUiTest.mainClock] is set to `autoAdvance = false` before the
 * segment is clicked, so the real `animateFloatAsState(tween(150, LinearEasing))` inside
 * [AeroSegmentedControl] starts but does not advance on its own; [FRAME_COUNT] manual
 * `mainClock.advanceTimeByFrame()` steps then walk it forward one frame at a time, sampling the
 * REAL rendered segment after every step via `captureToImage()` — never a reimplementation of the
 * tween's own time-to-value math, which would assert this test's model of the animation instead of
 * the component's actual wiring.
 *
 * **Sampling methodology** (unchanged). Each frame's capture is scanned for the pixel closest to
 * either luminance extreme (0 or 1) within a small patch centered on the captured segment image —
 * [AeroColorScheme.labelOnFilledSurface] is always exactly [Color.Black] or [Color.White] for every
 * shipped scheme, and none of this library's shipped fill colours are anywhere near either extreme,
 * so the most-extreme pixel in the patch is always the label's own ink, never a stray fill pixel.
 * That extreme pixel is bucketed to the nearer of pure black/white via a luminance-0.5 threshold.
 *
 * **D-08 fixture proof, in this file** ([countLabelColorChangesFlagsAnySingleChange] /
 * [countLabelColorChangesAcceptsAConstantSequence]) — a gate that cannot fail is a false pass
 * (VER-06). Both fixtures exercise [countLabelColorChanges] directly, the exact same function the
 * real per-scheme assertions below call on their real sampled sequences.
 *
 * **Boundary.** This gate is mechanical byte-level differencing over REAL running interaction
 * states, not an aesthetic judgment — whether the (now invariant) label colour looks acceptable at
 * real frame rate on real hardware remains a human question 20-07 exists for (SHW-16).
 */
@OptIn(ExperimentalTestApi::class)
class VER08SegmentLabelFlipTest {

    // ---------------------------------------------------------------------------------------
    // D-08 fixture proof — same countLabelColorChanges function the real assertions use below.
    // ---------------------------------------------------------------------------------------

    @Test
    fun countLabelColorChangesFlagsAnySingleChange() {
        val singleFlip = listOf(Color.Black, Color.Black, Color.Black, Color.White)
        val changes = countLabelColorChanges(singleFlip)
        assertTrue(
            changes != 0,
            "D-08 fixture proof: a synthetic sequence with even ONE colour change " +
                "(black,black,black,white) must be counted as non-zero ($changes counted) — the " +
                "strengthened VER-08 bar is NEVER, so this gate must be able to reject a single " +
                "flip, not just an oscillation"
        )
    }

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
    fun countLabelColorChangesAcceptsAConstantSequence() {
        val constant = listOf(Color.Black, Color.Black, Color.Black, Color.Black)
        val changes = countLabelColorChanges(constant)
        assertTrue(
            changes == 0,
            "D-08 fixture proof: a synthetic sequence with NO colour change (constant black) must " +
                "be counted as exactly zero ($changes counted) — proves countLabelColorChanges does " +
                "not vacuously reject the expected, correct invariant case (VER-08)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real animation, all three schemes — the label must NEVER change, not merely flip once.
    // ---------------------------------------------------------------------------------------

    @Test
    fun labelNeverChangesAcrossSelectionAnimationInAeroBlue() =
        runSegmentLabelFlipTest("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun labelNeverChangesAcrossSelectionAnimationInAeroDark() =
        runSegmentLabelFlipTest("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun labelNeverChangesAcrossSelectionAnimationInClassic() =
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
                samples.add(sampleLabelColor(target))
            }

            assertTrue(
                samples.isNotEmpty(),
                "VER-08 ($schemeName): sampled frame count must be non-zero — a clock that never " +
                    "advanced must not be able to pass this gate vacuously"
            )

            val changes = countLabelColorChanges(samples)
            assertTrue(
                changes == 0,
                "VER-08 ($schemeName): the segment label must NEVER change colour across the whole " +
                    "selection animation ($changes changes counted across ${samples.size} sampled " +
                    "frames) — the label now reads a fixed AeroColorScheme.labelOnFilledSurface " +
                    "token, so any observed change is a real regression, not a legitimate midpoint " +
                    "crossing (20-09, superseding this gate's original at-most-one bar)"
            )
        }

    // ---------------------------------------------------------------------------------------
    // Real hover/press/focus/disabled, all three schemes — the label must NEVER change, and must
    // match across BOTH the raised and recessed segment (same polarity, same token, SHW-16/VER-06).
    // ---------------------------------------------------------------------------------------

    @Test
    fun labelNeverChangesAcrossHoverPressFocusOrDisabledInAeroBlue() =
        runStateInvarianceTest("AeroBlue", AeroColorScheme.AeroBlue)

    @Test
    fun labelNeverChangesAcrossHoverPressFocusOrDisabledInAeroDark() =
        runStateInvarianceTest("AeroDark", AeroColorScheme.AeroDark)

    @Test
    fun labelNeverChangesAcrossHoverPressFocusOrDisabledInClassic() =
        runStateInvarianceTest("Classic", AeroColorScheme.Classic)

    private fun runStateInvarianceTest(schemeName: String, scheme: AeroColorScheme) =
        runComposeUiTest {
            val enabledState = mutableStateOf(true)

            setContent {
                AeroTheme(colorScheme = scheme) {
                    AeroSegmentedControl(
                        options = listOf(0, 1),
                        selected = 0,
                        onSelect = {},
                        enabled = enabledState.value,
                        optionLabel = { LABEL_GLYPH },
                    )
                }
            }
            waitForIdle()

            // Segment 0 is selected (recessed); segment 1 is unselected (raised) — both are the
            // OPAQUE-fill polarity and must therefore read the identical scheme-level label token.
            val recessed = onAllNodesWithText(LABEL_GLYPH).get(0)
            val raised = onAllNodesWithText(LABEL_GLYPH).get(1)

            val samples = mutableListOf<Color>()
            samples += sampleLabelColor(raised) // rest
            samples += sampleLabelColor(recessed) // rest, recessed polarity peer

            raised.performMouseInput { moveTo(center) }
            waitForIdle()
            samples += sampleLabelColor(raised) // hover

            raised.performMouseInput { press() }
            waitForIdle()
            samples += sampleLabelColor(raised) // press
            raised.performMouseInput { release() }
            waitForIdle()

            raised.requestFocus()
            waitForIdle()
            samples += sampleLabelColor(raised) // keyboard-acquired focus

            enabledState.value = false
            waitForIdle()
            samples += sampleLabelColor(raised) // disabled, raised
            samples += sampleLabelColor(recessed) // disabled, recessed

            val distinctColors = samples.toSet()
            assertEquals(
                1,
                distinctColors.size,
                "VER-08 ($schemeName): the segment label must read the SAME colour across " +
                    "rest/hover/press/focus/disabled and across BOTH the raised and recessed " +
                    "segment ($distinctColors observed across ${samples.size} samples) — the label " +
                    "is a fixed per-scheme token (AeroColorScheme.labelOnFilledSurface), not a " +
                    "per-state or per-component computation (SHW-16/VER-06)"
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

/** Captures [node] and buckets its sampled label pixel to the nearer of pure black/white. */
@OptIn(ExperimentalTestApi::class)
private fun sampleLabelColor(node: SemanticsNodeInteraction): Color =
    bucketizeToNearestExtreme(extremeLuminancePixelNearCenter(node.captureToImage().toPixelMap()))

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
 * [com.mordred.aero.theme.AeroColorScheme.labelOnFilledSurface] only ever holds exactly one of
 * those two values on every shipped scheme.
 */
internal fun bucketizeToNearestExtreme(color: Color): Color =
    if (color.luminance() < 0.5f) Color.Black else Color.White

/**
 * Counts the number of colour CHANGES between consecutive samples — a constant sequence (e.g.
 * black,black,black) counts 0; any flip (single or oscillating) counts 1 or more. Pure function,
 * no Compose dependency — the exact same function the D-08 fixture proof above and the real
 * per-scheme assertions both call.
 */
internal fun countLabelColorChanges(samples: List<Color>): Int {
    var changes = 0
    for (i in 1 until samples.size) {
        if (samples[i] != samples[i - 1]) changes++
    }
    return changes
}
