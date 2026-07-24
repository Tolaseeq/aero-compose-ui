package com.mordred.aero.components.range

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * WR-01 regression guard: [AeroRangeSlider]'s per-thumb hover-tracking
 * `pointerInput(enabled, valueRange){...}` block (AeroRangeSlider.kt ~295-353) restarts whenever
 * `enabled`/`valueRange` change, cancelling the running coroutine. Before the fix (commit
 * `f12e4c4`), a dangling `HoverInteraction.Enter` with no matching `Exit` left
 * `collectIsHoveredAsState()` permanently `true` on cancellation — the thumb's hover glow ring
 * would re-render lit on any later recomposition even with the pointer nowhere near it
 * (18-VERIFICATION.md's WR-01 human-verification item, 18-UAT.md).
 *
 * There is no injectable `MutableInteractionSource` on [AeroRangeSlider]'s public signature — the
 * two per-thumb sources are created internally via `remember { MutableInteractionSource() }`,
 * mirroring the sibling [AeroSlider]'s equally-internal single source — so this exercises the
 * cancellation path through the actually-rendered pixels instead of a directly-injected source:
 * `drawAeroGlowRing`'s outer "bloom" rings paint strictly OUTSIDE the thumb's own fill circle and
 * draw literally nothing when inactive (`if (!active) return`), giving a clean present/absent
 * signal at a fixed sample point just above the thumb center.
 *
 * Mirrors 18-UAT.md's WR-01 scenario: hover a thumb, flip `enabled` off then back on WITHOUT
 * moving the pointer, and assert the glow does not reappear.
 */
@OptIn(ExperimentalTestApi::class)
class AeroRangeSliderHoverCancellationTest {

    @Test
    fun hoverGlowDoesNotStickOnAfterEnabledToggleCancelsHoverPointerInput() = runComposeUiTest {
        val enabledState = mutableStateOf(true)
        val fixedValue = 0.3f..0.7f
        val fixedRange = 0f..1f
        lateinit var density: Density

        setContent {
            AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
                density = LocalDensity.current
                AeroRangeSlider(
                    value = fixedValue,
                    onValueChange = {},
                    modifier = Modifier.testTag("slider").width(300.dp),
                    enabled = enabledState.value,
                    valueRange = fixedRange,
                )
            }
        }
        waitForIdle()

        val bounds = onNodeWithTag("slider").fetchSemanticsNode().boundsInRoot
        val startFraction =
            (fixedValue.start - fixedRange.start) / (fixedRange.endInclusive - fixedRange.start)
        val thumbCenter = Offset(
            x = bounds.left + startFraction * bounds.width,
            y = bounds.top + bounds.height / 2f,
        )
        // Layer-1 bloom ring band sits at radius (thumbRadius=10dp + bloomStrokePx/2=1dp) = 11dp
        // from the thumb center — strictly outside the opaque 10dp-radius thumb fill circle, so
        // sampling here never picks up the fill color, only the glow ring's own paint (or nothing
        // when the ring is inactive).
        val ringOffsetPx = with(density) { 11.dp.toPx() }
        val sampleRoot = Offset(thumbCenter.x, thumbCenter.y - ringOffsetPx)
        val sampleLocal = sampleRoot - bounds.topLeft

        fun ringPixel(): Color {
            val pixels = onNodeWithTag("slider").captureToImage().toPixelMap()
            return pixels[sampleLocal.x.roundToInt(), sampleLocal.y.roundToInt()]
        }

        val restingColor = ringPixel()

        onRoot().performMouseInput { enter(thumbCenter) }
        waitForIdle()
        val hoveredColor = ringPixel()
        assertNotEquals(
            restingColor,
            hoveredColor,
            "sanity check: hover glow ring must paint once the pointer enters the thumb",
        )

        // Flip enabled off then back on WITHOUT moving the pointer — this cancels and relaunches
        // the hover pointerInput block twice (its keys are `enabled, valueRange`), exercising the
        // WR-01 cancellation path against a thumb that is still (from the user's perspective)
        // sitting under the pointer.
        enabledState.value = false
        waitForIdle()
        enabledState.value = true
        waitForIdle()

        val afterReEnableColor = ringPixel()
        assertEquals(
            restingColor,
            afterReEnableColor,
            "hover glow ring must not reappear after the enabled toggle cancels/relaunches the " +
                "hover pointerInput without a fresh pointer Enter/Move — WR-01 regression",
        )
    }
}
