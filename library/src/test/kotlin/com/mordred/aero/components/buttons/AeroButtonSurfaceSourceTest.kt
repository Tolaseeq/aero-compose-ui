package com.mordred.aero.components.buttons

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards mirroring [com.mordred.aero.theme.AeroSurfacePrimitivesTest]'s
 * `functionBody()` convention (applied here to the whole file, since both [AeroButton] and
 * [AeroOutlinedButton] are single-declaration thin wrappers — there is no larger file body to
 * narrow away from):
 *
 * - VBTN-03: neither [AeroButton] nor [AeroOutlinedButton]'s source contains an unclipped
 *   `drawWithContent { ... }` overlay — the old square-corner-bleed bug (17-UI-SPEC.md
 *   "VBTN-03 — hover-overlay clipping, resolved by construction").
 * - VBTN-06: both files call the one shared [AeroButtonSurface] and neither calls
 *   `aeroSurface(`/`drawAeroSurfaceCore(` directly — no duplicated painter, the non-drift
 *   guarantee the whole phase rests on.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), both guards below were proven to FAIL against a deliberately-reintroduced unclipped
 * overlay (VBTN-03) and a deliberately-duplicated direct `aeroSurface(` call (VBTN-06) before
 * being counted as real gates — see 17-03-SUMMARY.md "Guard Fail-Then-Pass Proof" for the exact
 * before/after transcript. The temporary breaking edits were reverted before this commit; this
 * file only ever asserts against the real, fixed source.
 */
class AeroButtonSurfaceSourceTest {

    @Test
    fun noUnclippedDrawWithContentOverlayInAeroButton() {
        assertFalse(
            aeroButtonSource.contains("drawWithContent"),
            "AeroButton.kt must not contain a drawWithContent overlay — the hover-brighten cue " +
                "must be one of aeroSurface()'s own clipped draws (VBTN-03)"
        )
    }

    @Test
    fun noUnclippedDrawWithContentOverlayInAeroOutlinedButton() {
        assertFalse(
            aeroOutlinedButtonSource.contains("drawWithContent"),
            "AeroOutlinedButton.kt must not contain a drawWithContent overlay — the hover-brighten " +
                "cue must be one of aeroSurface()'s own clipped draws (VBTN-03)"
        )
    }

    @Test
    fun aeroButtonCallsTheSharedSurfaceNotADuplicatedPainter() {
        assertTrue(
            aeroButtonSource.contains("AeroButtonSurface("),
            "AeroButton.kt must delegate to the shared AeroButtonSurface (VBTN-06)"
        )
        // Plain-substring check is safe here — the lowercase "aeroSurface(" (the Modifier
        // extension fn) never occurs as a substring of "AeroButtonSurface(" (capital A/S); a
        // thin wrapper calling only the shared composable will never contain this token.
        assertFalse(
            aeroButtonSource.contains("aeroSurface(") || aeroButtonSource.contains("drawAeroSurfaceCore("),
            "AeroButton.kt must not call aeroSurface(/drawAeroSurfaceCore( directly — that would " +
                "be a second, duplicated painter (VBTN-06)"
        )
    }

    @Test
    fun aeroOutlinedButtonCallsTheSharedSurfaceNotADuplicatedPainter() {
        assertTrue(
            aeroOutlinedButtonSource.contains("AeroButtonSurface("),
            "AeroOutlinedButton.kt must delegate to the shared AeroButtonSurface (VBTN-06)"
        )
        assertFalse(
            aeroOutlinedButtonSource.contains("aeroSurface(") || aeroOutlinedButtonSource.contains("drawAeroSurfaceCore("),
            "AeroOutlinedButton.kt must not call aeroSurface(/drawAeroSurfaceCore( directly — that " +
                "would be a second, duplicated painter (VBTN-06)"
        )
    }

    @Test
    fun aeroOutlinedButtonPassesOutlinedTrueToTheSharedSurface() {
        assertTrue(
            aeroOutlinedButtonSource.contains("outlined = true"),
            "AeroOutlinedButton.kt must call AeroButtonSurface(..., outlined = true) — the sole " +
                "differentiator from AeroButton, not a separately authored style (VBTN-06)"
        )
    }

    /**
     * 19-09 (WR-01) regression guard: the button family's focus glow must gate on the same
     * `focusVisible` value AeroSwitch/AeroSegmentedControl/AeroListItem already use (mirroring
     * [com.mordred.aero.components.selection.AeroSwitchSourceTest]'s
     * `aeroSwitchGatesOnlyTheFocusRingOnFocusVisibleLeavingHoverOnTheRawFlag` guard shape), so a
     * mouse click does not leave a residual focus glow after the pointer moves away, while hover
     * keeps reading the raw flag because the developer explicitly wants hover indication retained.
     */
    @Test
    fun aeroButtonSurfaceGatesOnlyTheFocusGlowOnFocusVisibleLeavingHoverOnTheRawFlag() {
        assertTrue(
            aeroButtonSurfaceSource.contains("active = state.focusVisible && enabled"),
            "AeroButtonSurface.kt's FOCUS aeroGlowRing call must gate on state.focusVisible, not " +
                "the raw focused flag (WR-01) — a mouse click must not leave a residual focus " +
                "glow after the pointer moves away"
        )
        assertTrue(
            aeroButtonSurfaceSource.contains("active = state.hovered && enabled"),
            "AeroButtonSurface.kt's HOVER aeroGlowRing call must keep reading the raw hovered " +
                "flag — the developer explicitly wants hover indication retained, WR-01 changes " +
                "the focus cue only"
        )
    }

    /**
     * SHW-16/VER-06 (20-09), superseding the D-12/D-13 (20-04) `resolveLabelColor` guard this test
     * previously asserted: the button's `Text` must read its label colour from the SCHEME
     * ([com.mordred.aero.theme.AeroColorScheme.labelOnFilledSurface] /
     * `labelOnOutlinedSurface`), resolved once per theme — never recomputed per call site from the
     * currently-animating fill via the retired `resolveLabelColor(style.fillTop, style.fillBottom,
     * ...)` mechanism, which is exactly the state-dependent label-colour flip the maintainer's
     * 20-08 checkpoint rule forbids. Mirrors
     * [com.mordred.aero.components.selection.AeroSegmentedControlSourceTest]'s
     * `sourceLabelReadsTheSchemeLevelTokenNeverAPerComponentLiteralOrTheRetiredResolver` guard, so
     * neither component can silently drift back to a per-fill or ambient label colour.
     */
    @Test
    fun aeroButtonSurfaceTextReadsTheSchemeLevelLabelToken() {
        assertTrue(
            aeroButtonSurfaceSource.contains("colors.labelOnOutlinedSurface"),
            "AeroButtonSurface.kt's Text( call must read colors.labelOnOutlinedSurface for the " +
                "outlined polarity — a scheme-level token resolved once, never a per-fill " +
                "computation (SHW-16/VER-06)"
        )
        assertTrue(
            aeroButtonSurfaceSource.contains("colors.labelOnFilledSurface"),
            "AeroButtonSurface.kt's Text( call must read colors.labelOnFilledSurface for the " +
                "opaque-fill polarity — a scheme-level token resolved once, never a per-fill " +
                "computation (SHW-16/VER-06)"
        )
        assertFalse(
            aeroButtonSurfaceSource.contains("resolveLabelColor("),
            "AeroButtonSurface.kt must not call the retired per-fill resolveLabelColor(...) — it " +
                "recomputed the label from the currently-animating fill, reintroducing the " +
                "state-dependent label-colour flip 20-09 removes (SHW-16/VER-06)"
        )
    }

    private val aeroButtonSource: String get() = sourceFile("AeroButton.kt").readText()
    private val aeroOutlinedButtonSource: String get() = sourceFile("AeroOutlinedButton.kt").readText()
    private val aeroButtonSurfaceSource: String get() = sourceFile("AeroButtonSurface.kt").readText()

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/buttons/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/buttons/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
