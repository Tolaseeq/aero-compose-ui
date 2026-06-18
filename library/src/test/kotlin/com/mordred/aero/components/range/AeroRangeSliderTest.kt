package com.mordred.aero.components.range

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AeroRangeSliderTest {

    private val unit = 0f..1f

    // === applyThumbMove: clamp / no-cross (steps == 0) ===

    @Test
    fun startThumbCannotMovePastEnd() {
        val current = 0.2f..0.6f
        // Try to drag start way past end.
        val result = applyThumbMove(current, RangeThumb.Start, 0.9f, unit, steps = 0)
        assertTrue(
            result.start <= result.endInclusive,
            "start must stay <= end; got ${result.start}..${result.endInclusive}",
        )
        // End is untouched.
        assertEquals(0.6f, result.endInclusive, 1e-4f)
    }

    @Test
    fun endThumbCannotMoveBelowStart() {
        val current = 0.2f..0.6f
        // Try to drag end below start.
        val result = applyThumbMove(current, RangeThumb.End, 0.05f, unit, steps = 0)
        assertTrue(
            result.start <= result.endInclusive,
            "start must stay <= end; got ${result.start}..${result.endInclusive}",
        )
        // Start is untouched.
        assertEquals(0.2f, result.start, 1e-4f)
    }

    @Test
    fun thumbsCannotOccupyExactSameValueWithoutSteps() {
        val current = 0.5f..0.5f
        // span * 0.001f == 0.001f minimum separation.
        val moveStart = applyThumbMove(current, RangeThumb.Start, 0.5f, unit, steps = 0)
        assertTrue(
            moveStart.endInclusive - moveStart.start >= 0.001f - 1e-5f,
            "minimum separation violated: ${moveStart.start}..${moveStart.endInclusive}",
        )
    }

    @Test
    fun startThumbMoveWithinRangeIsApplied() {
        val current = 0.2f..0.6f
        val result = applyThumbMove(current, RangeThumb.Start, 0.4f, unit, steps = 0)
        assertEquals(0.4f, result.start, 1e-4f)
        assertEquals(0.6f, result.endInclusive, 1e-4f)
    }

    // === snapToStep ===

    @Test
    fun snapToStepSnapsToNearestDiscretePosition() {
        // steps = 4 -> positions at 0, 0.25, 0.5, 0.75, 1.0
        assertEquals(0.25f, snapToStep(0.30f, unit, steps = 4), 1e-4f)
        assertEquals(0.5f, snapToStep(0.49f, unit, steps = 4), 1e-4f)
        assertEquals(1.0f, snapToStep(0.95f, unit, steps = 4), 1e-4f)
    }

    @Test
    fun snapToStepIsIdentityWhenStepsZero() {
        assertEquals(0.37f, snapToStep(0.37f, unit, steps = 0), 1e-4f)
    }

    @Test
    fun applyThumbMoveSnapsWhenStepsGreaterThanZero() {
        val current = 0.0f..1.0f
        // steps = 4, raw 0.30 snaps to 0.25 for start.
        val result = applyThumbMove(current, RangeThumb.Start, 0.30f, unit, steps = 4)
        assertEquals(0.25f, result.start, 1e-4f)
    }

    @Test
    fun applyThumbMoveEnforcesOneStepSeparation() {
        // steps = 4 -> one step = 0.25 minimum separation.
        val current = 0.5f..0.5f
        val result = applyThumbMove(current, RangeThumb.End, 0.5f, unit, steps = 4)
        assertTrue(
            result.endInclusive - result.start >= 0.25f - 1e-4f,
            "one-step separation violated: ${result.start}..${result.endInclusive}",
        )
    }

    // === F9 guard: chaining applyThumbMove off an already-moved range ===

    /**
     * F9 regression guard: after dragging End from 0.7 to 0.9 (so `value = 0.2..0.9`),
     * a subsequent drag of Start to 0.4 must yield `0.4..0.9` — NOT `0.4..0.7` (the stale
     * drag-start snapshot). This verifies that `applyThumbMove` chains off the LATEST value.
     */
    @Test
    fun chainedThumbMovePreservesMovedEndEndpoint() {
        val initial = 0.2f..0.7f
        // Step 1: drag End from 0.7 → 0.9.
        val afterEndMove = applyThumbMove(initial, RangeThumb.End, 0.9f, unit, steps = 0)
        assertEquals(0.2f, afterEndMove.start, 1e-4f)
        assertEquals(0.9f, afterEndMove.endInclusive, 1e-4f)
        // Step 2: drag Start to 0.4, using the UPDATED range (0.2..0.9) as `current`.
        val afterStartMove = applyThumbMove(afterEndMove, RangeThumb.Start, 0.4f, unit, steps = 0)
        assertEquals(0.4f, afterStartMove.start, 1e-4f)
        // End must reflect the value from step 1 (0.9), not the original (0.7).
        assertEquals(0.9f, afterStartMove.endInclusive, 1e-4f)
    }

    // === custom valueRange ===

    @Test
    fun applyThumbMoveHonorsNonUnitRange() {
        val range = 0f..100f
        val current = 20f..60f
        val result = applyThumbMove(current, RangeThumb.End, 80f, range, steps = 0)
        assertEquals(80f, result.endInclusive, 1e-3f)
        assertEquals(20f, result.start, 1e-3f)
    }

    // === xToValue / valueToX round-trip ===

    @Test
    fun xToValueAndValueToXRoundTrip() {
        val trackWidth = 200f
        val v = xToValue(150f, unit, trackWidth)
        assertEquals(0.75f, v, 1e-4f)
        val x = valueToX(0.75f, unit, trackWidth)
        assertEquals(150f, x, 1e-3f)
    }

    @Test
    fun xToValueClampsOutOfBounds() {
        val trackWidth = 200f
        assertEquals(0f, xToValue(-50f, unit, trackWidth), 1e-4f)
        assertEquals(1f, xToValue(500f, unit, trackWidth), 1e-4f)
    }

    // === z-order helper ===

    @Test
    fun drawFirstReturnsOtherThumbSoLastMovedDrawsOnTop() {
        // lastMoved = End -> draw Start first (End on top).
        assertEquals(RangeThumb.Start, thumbToDrawFirst(RangeThumb.End))
        // lastMoved = Start -> draw End first (Start on top).
        assertEquals(RangeThumb.End, thumbToDrawFirst(RangeThumb.Start))
    }

    @Test
    fun drawFirstDefaultsToEndLastMoved() {
        // Default lastMovedThumb is End, so Start draws first.
        assertEquals(RangeThumb.Start, thumbToDrawFirst())
    }

    private fun assertEquals(expected: Float, actual: Float, tolerance: Float) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected but got $actual (tol=$tolerance)",
        )
    }
}
