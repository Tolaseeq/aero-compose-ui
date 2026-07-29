package com.mordred.aero.components.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.mordred.aero.components.selection.resolveSegmentStyle
import com.mordred.aero.theme.AeroColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Value-level WCAG 4.5:1 contrast regression guard for [resolveLabelColor] (D-12/D-13, 20-04),
 * closing the AeroButton label-contrast todo filed alongside gap G5
 * (`.planning/todos/pending/2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor.md`).
 *
 * This is the D-13 authority on whether the mechanism actually clears the floor — the UI-SPEC's
 * own per-theme table (20-UI-SPEC.md Color section) was HAND-computed and flags AeroBlue's
 * filled-rest `fillBottom` margin (4.56 against black) as needing programmatic confirmation. This
 * test runs [resolveLabelColor]'s own chosen candidate against the real [Color.darken] RGB-mix
 * math the production styles actually produce, not a hand re-derivation.
 *
 * The [contrastRatio] below is a SEPARATE, independently-written implementation of the WCAG
 * formula — recovered verbatim (per this plan's `<read_first>`) from the retired guard at
 * `git show c35f883~1:.../AeroSegmentedControlStylesTest.kt` — deliberately NOT importing the
 * production [labelContrastRatio]. A wrong production formula must not be allowed to certify
 * itself: if [labelContrastRatio] had a sign error or a wrong luminance offset, importing it here
 * would make this guard pass no matter what colour [resolveLabelColor] actually returned.
 *
 * Both stops, all three shipped themes, all three resolved-style cases (filled button rest,
 * raised/unselected segment, recessed/selected segment) are asserted — the literal D-13 acceptance
 * bar. Fixing only the theme that was furthest below the floor does not satisfy it.
 */
class AeroButtonContrastRegressionTest {

    private val schemes = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic,
    )

    // ---- Fixture proof (D-08 style): this guard is provably not inert before it is trusted. ----

    @Test
    fun fixtureMidGreyOnMidGreyMeasuresBelowTheFloor() {
        val midGrey = Color(0xFF808080)
        val ratio = contrastRatio(midGrey, midGrey)
        assertTrue(
            ratio < MIN_LABEL_CONTRAST,
            "Fixture sanity check failed: mid-grey-on-mid-grey measured $ratio, expected it to " +
                "measure BELOW $MIN_LABEL_CONTRAST — if this fails, contrastRatio() itself is broken " +
                "and every other assertion in this file is meaningless"
        )
    }

    @Test
    fun fixtureBlackOnWhiteMeasuresWellAboveTheFloor() {
        val ratio = contrastRatio(Color.Black, Color.White)
        assertTrue(
            ratio > MIN_LABEL_CONTRAST,
            "Fixture sanity check failed: black-on-white measured $ratio, expected it to measure " +
                "well ABOVE $MIN_LABEL_CONTRAST — if this fails, contrastRatio() itself is broken and " +
                "every other assertion in this file is meaningless"
        )
    }

    // ---- Filled AeroButton at rest — both stops, all three themes, plus expected-candidate check. ----

    @Test
    fun filledButtonRestClearsTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val style = resolveButtonStyle(
                colors = colors,
                outlined = false,
                hovered = false,
                pressed = false,
                focused = false,
                enabled = true,
            )
            val label = resolveLabelColor(style.fillTop, style.fillBottom, colors.background)
            val compositedTop = style.fillTop.compositeOver(colors.background)
            val compositedBottom = style.fillBottom.compositeOver(colors.background)
            val topRatio = contrastRatio(label, compositedTop)
            val bottomRatio = contrastRatio(label, compositedBottom)

            assertTrue(
                topRatio >= MIN_LABEL_CONTRAST,
                "$name filled-button-rest: label-to-fillTop contrast ratio $topRatio must be at " +
                    "least $MIN_LABEL_CONTRAST"
            )
            assertTrue(
                bottomRatio >= MIN_LABEL_CONTRAST,
                "$name filled-button-rest: label-to-fillBottom contrast ratio $bottomRatio must be " +
                    "at least $MIN_LABEL_CONTRAST"
            )
        }
    }

    @Test
    fun filledButtonRestPicksTheExpectedCandidatePerTheme() {
        // 20-UI-SPEC.md's pre-computed table: AeroBlue -> black, AeroDark -> black, Classic -> white.
        // Catches a silent flip in the wrong direction even when the ratio floor still happens to hold.
        val expected = mapOf(
            "AeroBlue" to LABEL_CANDIDATE_DARK,
            "AeroDark" to LABEL_CANDIDATE_DARK,
            "Classic" to LABEL_CANDIDATE_LIGHT,
        )
        schemes.forEach { (name, colors) ->
            val style = resolveButtonStyle(
                colors = colors,
                outlined = false,
                hovered = false,
                pressed = false,
                focused = false,
                enabled = true,
            )
            val label = resolveLabelColor(style.fillTop, style.fillBottom, colors.background)
            assertEquals(
                expected.getValue(name),
                label,
                "$name filled-button-rest: resolveLabelColor must pick the UI-SPEC's pre-computed " +
                    "candidate for this theme — a flip here is a silent wrong-direction regression " +
                    "even if the ratio floor still happens to hold"
            )
        }
    }

    // ---- Raised (unselected) segment — both stops, all three themes. No candidate expectation. ----

    @Test
    fun raisedSegmentClearsTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val style = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            val label = resolveLabelColor(style.fillTop, style.fillBottom, colors.background)
            val compositedTop = style.fillTop.compositeOver(colors.background)
            val compositedBottom = style.fillBottom.compositeOver(colors.background)
            val topRatio = contrastRatio(label, compositedTop)
            val bottomRatio = contrastRatio(label, compositedBottom)

            assertTrue(
                topRatio >= MIN_LABEL_CONTRAST,
                "$name raised-segment: label-to-fillTop contrast ratio $topRatio must be at least " +
                    "$MIN_LABEL_CONTRAST"
            )
            assertTrue(
                bottomRatio >= MIN_LABEL_CONTRAST,
                "$name raised-segment: label-to-fillBottom contrast ratio $bottomRatio must be at " +
                    "least $MIN_LABEL_CONTRAST"
            )
        }
    }

    // ---- Recessed (selected) segment — both stops, all three themes. No candidate expectation. ----

    /**
     * **Open finding (20-04-SUMMARY.md), routed to the 20-07 three-theme sign-off as a visual
     * judgment — deliberately left red, not silently downgraded.** AeroDark's recessed (selected)
     * segment fails on `fillBottom`: [resolveLabelColor]'s own worst-case-optimal choice (white,
     * 4.0787) is still below [MIN_LABEL_CONTRAST] — proven not fixable by the candidate flip alone
     * (black's worst-case there is 3.5377, strictly worse). The ONE sanctioned remedy this plan
     * authorizes (widening `FILLED_FILL_BOTTOM_DARKEN` slightly) is scoped to the filled AeroButton
     * rest case only, per this task's `<action>` step 2 — it does not apply here. Retuning
     * `RECESSED_FILL_DARKEN` or introducing a segment-specific darken constant to chase this floor
     * is explicitly FORBIDDEN (D-12; the maintainer's 19-08 acceptance of the recess/depth reading).
     * This assertion is intentionally left failing so the gap stays visible rather than being
     * quietly weakened or deleted.
     */
    @Test
    fun recessedSegmentClearsTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val style = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            val label = resolveLabelColor(style.fillTop, style.fillBottom, colors.background)
            val compositedTop = style.fillTop.compositeOver(colors.background)
            val compositedBottom = style.fillBottom.compositeOver(colors.background)
            val topRatio = contrastRatio(label, compositedTop)
            val bottomRatio = contrastRatio(label, compositedBottom)

            assertTrue(
                topRatio >= MIN_LABEL_CONTRAST,
                "$name recessed-segment: label-to-fillTop contrast ratio $topRatio must be at " +
                    "least $MIN_LABEL_CONTRAST"
            )
            assertTrue(
                bottomRatio >= MIN_LABEL_CONTRAST,
                "$name recessed-segment: label-to-fillBottom contrast ratio $bottomRatio must be " +
                    "at least $MIN_LABEL_CONTRAST"
            )
        }
    }
}

/**
 * WCAG 1.4.3 floor for NORMAL-size text contrast (4.5:1) — the button/segment label is 14sp
 * regular body text, neither large-text-exempt nor a graphical UI-component in the 3:1-floor
 * sense. Never lowered under any circumstance (D-13).
 */
private const val MIN_LABEL_CONTRAST: Float = 4.5f

/**
 * Standard WCAG 2.x contrast ratio: the lighter of the two relative luminances plus `0.05f`,
 * divided by the darker plus `0.05f`. Deliberately test-local and independently written (recovered
 * verbatim from the retired `AeroSegmentedControlStylesTest.kt` guard, `git show c35f883~1`) —
 * NEVER imports the production [labelContrastRatio], so a wrong production formula cannot certify
 * itself. Only meaningful between two fully opaque colours — callers composite over an opaque
 * backdrop first.
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
