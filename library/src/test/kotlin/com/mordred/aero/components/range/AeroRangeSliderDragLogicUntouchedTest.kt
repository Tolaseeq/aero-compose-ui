package com.mordred.aero.components.range

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan guard locking VRNG-04's render-only boundary: `AeroRangeSlider`'s manual
 * `awaitPointerEventScope` drag loop (PITFALL-03 mitigation) and the pure logic functions above
 * it must never be replaced by a `detectDragGestures`-based rewrite, even when Plan 02's per-thumb
 * hover wiring is added alongside it (see 18-02-PLAN.md's untouched-by-construction boundary,
 * mirrors `18-RESEARCH.md`'s Pitfall 5 / PITFALL-03).
 *
 * Mirrors [com.mordred.aero.components.buttons.AeroButtonSurfaceSourceTest]'s cwd-independent
 * `sourceFile()` resolver and `.readText()` string-contains assertion idiom.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), both guards below were proven to FAIL against deliberately-reintroduced violations
 * (swapping `awaitPointerEventScope` for a `detectDragGestures` marker string, and deleting a
 * load-bearing logic-fn marker) before being counted as real gates — see 18-02-SUMMARY.md's
 * "Guard Fail-Then-Pass Proof" for the exact before/after transcript. The temporary breaking
 * edits were reverted before this commit; this file only ever asserts against the real, fixed
 * source.
 */
class AeroRangeSliderDragLogicUntouchedTest {

    @Test
    fun dragLoopStillUsesTheManualAwaitPointerEventScopeLoop() {
        assertTrue(
            aeroRangeSliderSource.contains("awaitPointerEventScope"),
            "AeroRangeSlider.kt must still drive drag via the manual awaitPointerEventScope loop " +
                "(PITFALL-03 mitigation) — VRNG-04's render-only boundary was violated"
        )
    }

    @Test
    fun dragLoopNeverUsesTheBannedDetectDragGesturesHelper() {
        assertFalse(
            aeroRangeSliderSource.contains("detectDragGestures"),
            "AeroRangeSlider.kt must NOT use detectDragGestures — its 18dp Desktop touchSlop " +
                "silently swallows the first pixels of pixel-precise drags (PITFALL-03)"
        )
    }

    @Test
    fun loadBearingDragLogicFunctionsAreStillPresent() {
        // snapToStep/applyThumbMove/xToValue/valueToX are also covered behaviorally by the
        // pre-existing AeroRangeSliderTest — this is an additional, cheaper source-level tripwire
        // that a diff accidentally deleting one of them (not just changing its body) is caught
        // immediately, without depending on the value-level test suite staying in sync.
        listOf("fun snapToStep(", "fun applyThumbMove(", "fun xToValue(", "fun valueToX(").forEach { marker ->
            assertTrue(
                aeroRangeSliderSource.contains(marker),
                "AeroRangeSlider.kt must still declare `$marker` — VRNG-04's drag-precision logic " +
                    "must remain byte-for-byte untouched by the render-only restyle"
            )
        }
    }

    private val aeroRangeSliderSource: String get() = sourceFile("AeroRangeSlider.kt").readText()

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/range/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/range/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
