package com.mordred.aero.components.range

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards mirroring
 * [com.mordred.aero.components.buttons.AeroButtonSurfaceSourceTest]'s `sourceFile()` convention:
 *
 * - VRNG-01/03: [AeroSlider]'s source contains the custom-slot `Slider(...)` call (`thumb =`/
 *   `track =`, not the plain 6-arg default overload) with `.hoverable(` applied inside the thumb
 *   slot — the M3 default `Thumb` composable is the only place hover gets wired for free
 *   (18-RESEARCH.md Pitfall 1); a from-scratch custom slot silently loses it without this call.
 * - D-07: [AeroSlider]'s source does NOT construct `SliderColors(` with disabled-alpha fields —
 *   disabled rendering lives entirely in our own thumb/track slots via `flattenDisabled`, not M3's
 *   `SliderColors` (replaces the old `alpha = 0.4f` path removed in 18-01).
 * - D-04: [AeroSlider]'s source does NOT call `.pressedRecess(` for the thumb — a dragged thumb
 *   stays raised, never inverts/recedes like the button's pressed state.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), all three guards below were proven to FAIL against deliberately-reintroduced broken
 * source (temporary local edits, reverted before commit) before being trusted — see
 * 18-01-SUMMARY.md "Guard Fail-Then-Pass Proof".
 */
class AeroSliderSourceTest {

    @Test
    fun aeroSliderUsesCustomThumbAndTrackSlotsWithHoverableOnTheThumb() {
        assertTrue(
            aeroSliderSource.contains("thumb ="),
            "AeroSlider.kt must supply a custom thumb = slot, not the plain 6-arg Slider(...) overload (VRNG-01)"
        )
        assertTrue(
            aeroSliderSource.contains("track ="),
            "AeroSlider.kt must supply a custom track = slot, not the plain 6-arg Slider(...) overload (VRNG-01)"
        )
        assertTrue(
            aeroSliderSource.contains(".hoverable("),
            "AeroSlider.kt's custom thumb slot must apply Modifier.hoverable(interactionSource) itself — " +
                "M3's default Thumb is the only place hover is wired for free (VRNG-03)"
        )
    }

    @Test
    fun aeroSliderDoesNotRelyOnSliderColorsForDisabled() {
        assertFalse(
            aeroSliderSource.contains("SliderColors("),
            "AeroSlider.kt must not construct SliderColors( for disabled appearance — disabled lives " +
                "in our own thumb/track slots via flattenDisabled(colors) (D-07)"
        )
    }

    @Test
    fun aeroSliderThumbPressDoesNotUsePressedRecess() {
        assertFalse(
            aeroSliderSource.contains(".pressedRecess("),
            "AeroSlider.kt's thumb resolver must not call .pressedRecess( — a dragged thumb stays " +
                "raised (picked up), it does not invert/recede like a pressed button (D-04)"
        )
    }

    private val aeroSliderSource: String get() = sourceFile("AeroSlider.kt").readText()

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
