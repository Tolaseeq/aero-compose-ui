package com.mordred.aero.verification

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.mordred.aero.components.buttons.resolveButtonStyle
import com.mordred.aero.components.selection.resolveSegmentStyle
import com.mordred.aero.theme.AeroColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * VER-10 / SHW-16 measurement gate (20-09 Task 1) — NOT yet a production change. Before the
 * per-call-site `resolveLabelColor` mechanism is retired in favour of one resolved colour per
 * scheme (the 20-08 checkpoint rule), this test enumerates EVERY fill an on-fill label actually
 * lands on today — filled/outlined button rest/hover/press/disabled, and segment
 * raised/recessed/hover/press — across all three shipped schemes, and computes the worst-case
 * WCAG ratio each of the two pure candidates ([Color.Black]/[Color.White]) achieves across that
 * entire per-scheme fill set.
 *
 * **Why measure before choosing.** The old per-call-site algorithm rescued individual fills by
 * picking a different candidate for each one. A single scheme-wide colour gets no such rescue —
 * whichever candidate wins here is what EVERY fill in that scheme must live with. Carrying over
 * 20-04's per-fill contrast table would certify numbers that no longer describe the code once
 * that rescue is gone (this plan's own `<the_trap>`); these numbers are re-derived fresh, from
 * the real [resolveButtonStyle]/[resolveSegmentStyle] outputs, not hand-computed or inherited.
 *
 * **No production code changes here** (20-09 `<order_discipline>`) — Task 2 reads the winner
 * this test pins per scheme and writes it into [AeroColorScheme]; Task 3 re-derives the
 * surviving-exception list against that same winner. This file is the numbers those two tasks are
 * built on, asserted so it re-executes rather than a one-off scratch calculation.
 *
 * The independent [contrastRatio] below deliberately does NOT import the production
 * `labelContrastRatio` (D-13 convention, `AeroButtonContrastRegressionTest`) — a wrong production
 * formula must not be able to certify itself here either.
 */
class VER10OneLabelColorPerThemeTest {

    // ---- D-08 fixture proof: contrastRatio itself is provably not inert before it is trusted. ----

    @Test
    fun fixtureBlackOnWhiteMeasuresWellAboveTheFloor() {
        val ratio = contrastRatio(Color.Black, Color.White)
        assertTrue(
            ratio > MIN_LABEL_CONTRAST,
            "Fixture sanity check failed: black-on-white measured $ratio, expected well above " +
                "$MIN_LABEL_CONTRAST — if this fails, contrastRatio() itself is broken and every " +
                "other assertion in this file is meaningless"
        )
    }

    @Test
    fun fixtureMidGreyOnMidGreyMeasuresBelowTheFloor() {
        val midGrey = Color(0xFF808080)
        val ratio = contrastRatio(midGrey, midGrey)
        assertTrue(
            ratio < MIN_LABEL_CONTRAST,
            "Fixture sanity check failed: mid-grey-on-mid-grey measured $ratio, expected below " +
                "$MIN_LABEL_CONTRAST — if this fails, contrastRatio() itself is broken and every " +
                "other assertion in this file is meaningless"
        )
    }

    // ---- The real measurement: every fill, both candidates, all three schemes. ----

    @Test
    fun measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInAeroBlue() =
        assertPerSchemeMeasurement(
            "AeroBlue",
            AeroColorScheme.AeroBlue,
            expectedDarkWorstCase = AEROBLUE_DARK_WORST_CASE,
            expectedLightWorstCase = AEROBLUE_LIGHT_WORST_CASE,
            expectedWinner = AEROBLUE_WINNER,
        )

    @Test
    fun measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInAeroDark() =
        assertPerSchemeMeasurement(
            "AeroDark",
            AeroColorScheme.AeroDark,
            expectedDarkWorstCase = AERODARK_DARK_WORST_CASE,
            expectedLightWorstCase = AERODARK_LIGHT_WORST_CASE,
            expectedWinner = AERODARK_WINNER,
        )

    @Test
    fun measuresWorstCaseContrastOfBothCandidatesAcrossEveryFillInClassic() =
        assertPerSchemeMeasurement(
            "Classic",
            AeroColorScheme.Classic,
            expectedDarkWorstCase = CLASSIC_DARK_WORST_CASE,
            expectedLightWorstCase = CLASSIC_LIGHT_WORST_CASE,
            expectedWinner = CLASSIC_WINNER,
        )

    /**
     * Emits the full per-fill/per-candidate table for all three schemes to stdout (`--info` or a
     * failing run surfaces it) — the human-readable form of the same data the pinned per-scheme
     * tests above assert numerically, satisfying this plan's "full table visible in test output"
     * verification.
     */
    @Test
    fun printsFullPerSchemePerFillContrastTable() {
        val report = StringBuilder()
        SCHEMES.forEach { (name, colors) ->
            report.appendLine("=== $name ===")
            fillCases(colors).forEach { case ->
                val top = case.fillTop.compositeOver(colors.background)
                val bottom = case.fillBottom.compositeOver(colors.background)
                report.appendLine(
                    "  ${case.label}: " +
                        "black(top=${contrastRatio(Color.Black, top)}, bottom=${contrastRatio(Color.Black, bottom)}) " +
                        "white(top=${contrastRatio(Color.White, top)}, bottom=${contrastRatio(Color.White, bottom)})"
                )
            }
        }
        println(report.toString())
        assertTrue(report.isNotEmpty(), "VER-10 table must not be empty")
    }

    private fun assertPerSchemeMeasurement(
        name: String,
        colors: AeroColorScheme,
        expectedDarkWorstCase: Float,
        expectedLightWorstCase: Float,
        expectedWinner: Color,
    ) {
        val cases = fillCases(colors)
        assertTrue(cases.isNotEmpty(), "$name: fill-case enumeration must not be empty")

        var darkWorstCase = Float.MAX_VALUE
        var lightWorstCase = Float.MAX_VALUE
        cases.forEach { case ->
            val top = case.fillTop.compositeOver(colors.background)
            val bottom = case.fillBottom.compositeOver(colors.background)
            darkWorstCase = minOf(
                darkWorstCase,
                contrastRatio(Color.Black, top),
                contrastRatio(Color.Black, bottom),
            )
            lightWorstCase = minOf(
                lightWorstCase,
                contrastRatio(Color.White, top),
                contrastRatio(Color.White, bottom),
            )
        }
        val winner = if (darkWorstCase >= lightWorstCase) Color.Black else Color.White

        assertEquals(
            expectedDarkWorstCase,
            darkWorstCase,
            FLOAT_TOLERANCE,
            "$name: measured worst-case ratio for the BLACK candidate across every enumerated " +
                "fill changed from the pinned $expectedDarkWorstCase to $darkWorstCase — re-derive " +
                "and update the pinned constant only if this is an intentional algorithm change"
        )
        assertEquals(
            expectedLightWorstCase,
            lightWorstCase,
            FLOAT_TOLERANCE,
            "$name: measured worst-case ratio for the WHITE candidate across every enumerated " +
                "fill changed from the pinned $expectedLightWorstCase to $lightWorstCase — re-derive " +
                "and update the pinned constant only if this is an intentional algorithm change"
        )
        assertEquals(
            expectedWinner,
            winner,
            "$name: the better single scheme-wide candidate flipped from the pinned expectation " +
                "— this is exactly the kind of silent wrong-direction regression VER-10 exists to catch"
        )
    }
}

/** One fill an on-fill label can actually land on, tagged with a human-readable description. */
private data class FillCase(val label: String, val fillTop: Color, val fillBottom: Color)

private val SCHEMES = listOf(
    "AeroBlue" to AeroColorScheme.AeroBlue,
    "AeroDark" to AeroColorScheme.AeroDark,
    "Classic" to AeroColorScheme.Classic,
)

/**
 * Every fill this plan's `<the_trap>` names explicitly: filled-button rest/hover/press/disabled,
 * outlined equivalents, and segment raised/recessed/hover/press — built from the REAL
 * [resolveButtonStyle]/[resolveSegmentStyle] resolvers, never a hand re-derivation of their fills.
 */
private fun fillCases(colors: AeroColorScheme): List<FillCase> {
    val cases = mutableListOf<FillCase>()

    listOf(false, true).forEach { outlined ->
        val prefix = if (outlined) "outlined" else "filled"
        val rest = resolveButtonStyle(colors, outlined, hovered = false, pressed = false, focused = false, enabled = true)
        val hover = resolveButtonStyle(colors, outlined, hovered = true, pressed = false, focused = false, enabled = true)
        val press = resolveButtonStyle(colors, outlined, hovered = false, pressed = true, focused = false, enabled = true)
        val disabled = resolveButtonStyle(colors, outlined, hovered = false, pressed = false, focused = false, enabled = false)
        cases += FillCase("$prefix-rest", rest.fillTop, rest.fillBottom)
        cases += FillCase("$prefix-hover", hover.fillTop, hover.fillBottom)
        cases += FillCase("$prefix-press", press.fillTop, press.fillBottom)
        cases += FillCase("$prefix-disabled", disabled.fillTop, disabled.fillBottom)
    }

    val raised = resolveSegmentStyle(colors, selectedProgress = 0f, hovered = false, pressed = false, enabled = true)
    val raisedHover = resolveSegmentStyle(colors, selectedProgress = 0f, hovered = true, pressed = false, enabled = true)
    val recessed = resolveSegmentStyle(colors, selectedProgress = 1f, hovered = false, pressed = false, enabled = true)
    val recessedHover = resolveSegmentStyle(colors, selectedProgress = 1f, hovered = true, pressed = false, enabled = true)
    val pressedSegment = resolveSegmentStyle(colors, selectedProgress = 0f, hovered = false, pressed = true, enabled = true)
    cases += FillCase("segment-raised", raised.fillTop, raised.fillBottom)
    cases += FillCase("segment-raised-hover", raisedHover.fillTop, raisedHover.fillBottom)
    cases += FillCase("segment-recessed", recessed.fillTop, recessed.fillBottom)
    cases += FillCase("segment-recessed-hover", recessedHover.fillTop, recessedHover.fillBottom)
    cases += FillCase("segment-press", pressedSegment.fillTop, pressedSegment.fillBottom)

    return cases
}

/**
 * WCAG 1.4.3 floor for normal-size text (4.5:1) — same value and rationale as
 * `AeroButtonContrastRegressionTest`'s `MIN_LABEL_CONTRAST`. Never lowered (D-13).
 */
private const val MIN_LABEL_CONTRAST: Float = 4.5f

/** Float-comparison slack absorbing ULP-level RGB-mix/luminance noise across JVM builds. */
private const val FLOAT_TOLERANCE: Float = 0.001f

// ---- Pinned measurements (re-derived fresh by this file, never carried over from 20-04). ----
// Fill in below by running `./gradlew :library:test --tests "*VER10*"` and reading the printed
// table / failure diff from printsFullPerSchemePerFillContrastTable — these are NOT hand-guessed.

// Measured 2026-07-29 via printsFullPerSchemePerFillContrastTable's STANDARD_OUT (this run).
// The dominant floor-breakers: outlined's near-transparent fill (composited colour close to the
// dark backdrop) tanks BLACK everywhere; filled/segment HOVER's lightened fillTop tanks WHITE.
// Neither pure candidate clears MIN_LABEL_CONTRAST across the full enumerated set in ANY scheme —
// see this file's class KDoc and this plan's own return-to-orchestrator table/finding.
private const val AEROBLUE_DARK_WORST_CASE: Float = 1.4220285f
private const val AEROBLUE_LIGHT_WORST_CASE: Float = 2.8013132f
private val AEROBLUE_WINNER: Color = Color.White

private const val AERODARK_DARK_WORST_CASE: Float = 1.2396675f
private const val AERODARK_LIGHT_WORST_CASE: Float = 2.4926789f
private val AERODARK_WINNER: Color = Color.White

private const val CLASSIC_DARK_WORST_CASE: Float = 1.3945937f
private const val CLASSIC_LIGHT_WORST_CASE: Float = 4.455385f
private val CLASSIC_WINNER: Color = Color.White

/**
 * Standard WCAG 2.x contrast ratio, independently written (not importing production
 * `labelContrastRatio`, D-13 convention) — see this file's class KDoc.
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
