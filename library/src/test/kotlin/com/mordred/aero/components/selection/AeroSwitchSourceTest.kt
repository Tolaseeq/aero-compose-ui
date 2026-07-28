package com.mordred.aero.components.selection

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards for [AeroSwitch]'s 19-02 restyle, mirroring
 * [com.mordred.aero.components.range.AeroSliderSourceTest]'s `sourceFile()` convention:
 *
 * - VSEL-01: the source calls `aeroGroove(`/`aeroThumbSurface(` — the shared PRIM-07/08
 *   primitives paint the track and thumb, replacing the two flat `Box`es.
 * - VSEL-02/D-06: the source calls `aeroGlowRing(` — hover and focus use the library's outer
 *   glow on this component.
 * - The source delegates to the pure `resolveSwitchThumbStyle(`/`resolveSwitchGrooveStyle(`
 *   resolvers rather than an inline color pick.
 * - D-05: the source does NOT call the button's pressed-recess transform — a thumb riding in an
 *   already-recessed groove stays raised, it does not invert/recede further.
 * - VLST-04: hover is collected through one shared interaction source paired with the shared
 *   collector, never through raw pointer-position tracking. The old wording's claim that the
 *   toggle modifier "does not report hover for free" was refuted directly by
 *   [com.mordred.aero.components.common.HoverEmissionTest] against the library's own Compose
 *   build (WR-04) — the toggle modifier IS the switch's single hover emitter now, and that
 *   behavioural claim is covered by that suite rather than asserted here.
 * - P-01: the source suppresses the platform indication (`indication = null`) since the new
 *   custom-painted hover/press cues replace it.
 * - PRIM-14: the source never uses the fully-transparent color constant as a gradient/fade target.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), every guard below was proven to FAIL against the shipped, un-restyled `AeroSwitch.kt`
 * before being trusted — see 19-02-SUMMARY.md "Guard Fail-Then-Pass Proof".
 *
 * - 19-05/gap G2 (VSEL-02): the FOCUS `aeroGlowRing` call reads `state.focusVisible`, the HOVER
 *   `aeroGlowRing` call still reads `state.hovered` — see 19-05-SUMMARY.md "Guard Fail-Then-Pass
 *   Proof".
 */
class AeroSwitchSourceTest {

    @Test
    fun aeroSwitchUsesAeroGrooveAndAeroThumbSurfaceForItsTrackAndThumb() {
        assertTrue(
            aeroSwitchSource.contains("aeroGroove("),
            "AeroSwitch.kt must paint its track via Modifier.aeroGroove( — the recessed accent groove primitive (VSEL-01, PRIM-08)"
        )
        assertTrue(
            aeroSwitchSource.contains("aeroThumbSurface("),
            "AeroSwitch.kt must paint its thumb via Modifier.aeroThumbSurface( — the raised neutral thumb primitive (VSEL-01, PRIM-07)"
        )
    }

    @Test
    fun aeroSwitchUsesAeroGlowRingForHoverAndFocus() {
        assertTrue(
            aeroSwitchSource.contains("aeroGlowRing("),
            "AeroSwitch.kt must use Modifier.aeroGlowRing( for hover and focus on the track (VSEL-02/D-06)"
        )
    }

    @Test
    fun aeroSwitchDelegatesToItsPureResolvers() {
        assertTrue(
            aeroSwitchSource.contains("resolveSwitchThumbStyle("),
            "AeroSwitch.kt must delegate thumb style resolution to resolveSwitchThumbStyle( rather than inline color-picking"
        )
        assertTrue(
            aeroSwitchSource.contains("resolveSwitchGrooveStyle("),
            "AeroSwitch.kt must delegate groove style resolution to resolveSwitchGrooveStyle( rather than inline color-picking"
        )
    }

    @Test
    fun aeroSwitchThumbPressDoesNotUseTheButtonsPressedRecessTransform() {
        assertFalse(
            aeroSwitchSource.contains(".pressedRecess("),
            "AeroSwitch.kt's thumb resolver must not call .pressedRecess( — a thumb riding in an " +
                "already-recessed groove stays raised, it does not invert/recede further (D-05)"
        )
    }

    @Test
    fun aeroSwitchCollectsHoverThroughOneSharedInteractionSourceNotRawPointerTracking() {
        // WR-04: the explicit hoverable emitter was removed — the toggle modifier already emits
        // hover on the supplied interaction source (proven directly by HoverEmissionTest against
        // the library's own Compose build), so a second emitter would double-emit into a boolean
        // hover model that is not reference-counted.
        assertTrue(
            aeroSwitchSource.contains("rememberAeroInteractionState(interactionSource)"),
            "AeroSwitch.kt must collect hover through rememberAeroInteractionState(interactionSource) " +
                "— hover is collected through one shared interaction source paired with the shared " +
                "collector, never through raw pointer-position tracking (VLST-04)"
        )
        assertTrue(
            aeroSwitchSource.contains("interactionSource = interactionSource"),
            "AeroSwitch.kt's toggleable( call must be fed the same interactionSource the collector " +
                "reads, so the toggle modifier is the single emitter for the shared source (WR-04)"
        )
        assertFalse(
            nonCommentSource.contains(".hoverable("),
            "AeroSwitch.kt must not contain an explicit .hoverable( call — the toggle modifier " +
                "already emits hover on the supplied interaction source, so a second emitter would " +
                "double-emit into a boolean hover model that is not reference-counted (WR-04)"
        )
        assertFalse(
            aeroSwitchSource.contains("pointerInput"),
            "AeroSwitch.kt must not collect hover via a raw pointerInput block (VLST-04)"
        )
        assertFalse(
            aeroSwitchSource.contains("awaitPointerEventScope"),
            "AeroSwitch.kt must not collect hover via a raw awaitPointerEventScope loop (VLST-04)"
        )
    }

    @Test
    fun aeroSwitchSuppressesPlatformIndication() {
        assertTrue(
            aeroSwitchSource.contains("indication = null"),
            "AeroSwitch.kt must pass indication = null on toggleable — the custom groove/thumb " +
                "hover and press cues replace the platform indication (P-01)"
        )
    }

    @Test
    fun aeroSwitchDoesNotUseTheFullyTransparentColorConstant() {
        assertFalse(
            aeroSwitchSource.contains("Color.Transparent"),
            "AeroSwitch.kt must never use the fully-transparent color constant as a gradient/fade " +
                "target — fade to baseColor.copy(alpha = 0f) instead (PRIM-14)"
        )
    }

    @Test
    fun aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag() {
        assertTrue(
            aeroSwitchSource.contains("active = state.focusVisible && enabled"),
            "AeroSwitch.kt's FOCUS aeroGlowRing call must gate on state.focusVisible, not the raw " +
                "focused flag — gap G2 (VSEL-02): a mouse click must not leave a residual focus ring"
        )
        assertTrue(
            aeroSwitchSource.contains("active = state.hovered && enabled"),
            "AeroSwitch.kt's HOVER aeroGlowRing call must keep reading the raw hovered flag — the " +
                "user explicitly wants hover indication retained, gap G2 changes the focus cue only"
        )
    }

    private val aeroSwitchSource: String get() = sourceFile("AeroSwitch.kt").readText()

    /**
     * [aeroSwitchSource] with every line whose trimmed form starts with a line-comment marker, a
     * block-comment opener, or a KDoc/block-comment continuation asterisk removed — so negative
     * guards over the real code cannot be satisfied or broken by KDoc prose that merely discusses
     * a forbidden identifier.
     */
    private val nonCommentSource: String
        get() = aeroSwitchSource
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
            }
            .joinToString("\n")

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/selection/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/selection/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
