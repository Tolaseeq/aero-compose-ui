package com.mordred.aero.components.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.mordred.aero.components.selection.resolveSegmentStyle
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Value-level WCAG 4.5:1 contrast regression guard (SHW-16/VER-06, 20-09, revised 2026-07-29),
 * superseding the 20-04/D-12/D-13 version of this file that measured the retired per-call-site
 * `resolveLabelColor(fillTop, fillBottom, backdrop)` mechanism. That mechanism is now gone —
 * [AeroColorScheme.labelOnFilledSurface]/[AeroColorScheme.labelOnOutlinedSurface] are resolved ONCE
 * per scheme (the maintainer's 20-08 checkpoint rule: "per theme, ONE text colour, not per element,
 * not per state"), so every number in this file is re-measured fresh against the real
 * [resolveButtonStyle]/[resolveSegmentStyle] fills using that fixed, invariant label — the OLD
 * per-fill rescue that let a different candidate win on each individual fill is gone, and carrying
 * over 20-04's table would certify numbers that no longer describe the code (this plan's own
 * "the trap to avoid").
 *
 * The [contrastRatio] below is a SEPARATE, independently-written implementation of the WCAG
 * formula — unchanged from the 20-04 version of this file, still deliberately NOT importing any
 * production contrast helper (D-13): a wrong production formula must not be able to certify itself.
 *
 * Every fill an on-fill label can land on is covered: filled button rest/hover/press (both stops,
 * all three themes), outlined equivalents, raised segment rest/hover, and recessed segment
 * rest/hover (press folds into recessed — [resolveSegmentStyle]'s `effectiveProgress` snaps ANY
 * pressed segment to full recess regardless of `selectedProgress`, so there is no separate
 * "raised-pressed" fill to measure). Disabled states are WCAG-1.4.3-exempt (below) rather than
 * asserted against the floor.
 *
 * **Revised 2026-07-29** (same-day 20-09 checkpoint): the maintainer rejected the surface-polarity
 * rule (dark label on opaque fills) on appearance and directed white on both polarities in
 * AeroBlue/AeroDark, matching Classic. That reopens most of the opaque-fill (filled button,
 * segment-raised, segment-recessed-hover) rest/hover/press cases in those two schemes below the
 * floor — accepted as ONE named, scoped, regression-bounded deviation
 * ([aeroBlueAeroDarkAcceptedSubFloorLabelDeviation]), not scattered across many small exceptions.
 * Filled/raised-segment assertions above therefore now cover Classic only — AeroBlue/AeroDark's
 * equivalent cases live in that one deviation test instead.
 *
 * **Exactly three named below-floor exceptions survive this revision** — see each exception test's
 * own KDoc for its measured values and justification. [MIN_LABEL_CONTRAST] is never lowered (D-13)
 * to accommodate any of them:
 * 1. [aeroBlueAeroDarkAcceptedSubFloorLabelDeviation] — AeroBlue/AeroDark opaque-fill white label.
 * 2. [classicFilledHoverIsTheOneAuthorizedLightTokenException] — Classic filled/raised hover, 4.455.
 * 3. [disabledSurfacesAreExemptFromTheContrastFloorPerWcag143] — WCAG 1.4.3 disabled-state exemption.
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

    // ---- Filled AeroButton — rest/hover/press, both stops. ----
    // Classic only, as of the 2026-07-29 revision (disabled excluded — see
    // disabledSurfacesAreExemptFromTheContrastFloorPerWcag143. Classic's hover case is excluded too
    // — it is authorized exception 2 of 3, asserted by name in
    // classicFilledHoverIsTheOneAuthorizedLightTokenException below rather than here). AeroBlue and
    // AeroDark's filled-button rest/hover/press now live in
    // aeroBlueAeroDarkAcceptedSubFloorLabelDeviation instead — the maintainer's white-on-both-
    // polarities decision puts most of their rest/hover/press cases below the floor, so asserting
    // them here (implying they clear) would be dishonest.

    @Test
    fun classicFilledButtonRestAndPressClearTheFloorOnBothStops() {
        val colors = AeroColorScheme.Classic
        val label = colors.labelOnFilledSurface
        listOf(
            "rest" to resolveButtonStyle(colors, outlined = false, hovered = false, pressed = false, focused = false, enabled = true),
            "press" to resolveButtonStyle(colors, outlined = false, hovered = false, pressed = true, focused = false, enabled = true),
        ).forEach { (state, style) ->
            assertClearsFloor(label, colors, style, "Classic filled-button-$state")
        }
    }

    // ---- Outlined AeroOutlinedButton — rest/hover/press, both stops, all three themes. ----

    @Test
    fun outlinedButtonRestHoverPressClearTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val label = colors.labelOnOutlinedSurface
            listOf(
                "rest" to resolveButtonStyle(colors, outlined = true, hovered = false, pressed = false, focused = false, enabled = true),
                "hover" to resolveButtonStyle(colors, outlined = true, hovered = true, pressed = false, focused = false, enabled = true),
                "press" to resolveButtonStyle(colors, outlined = true, hovered = false, pressed = true, focused = false, enabled = true),
            ).forEach { (state, style) ->
                assertClearsFloor(label, colors, style, "$name outlined-button-$state")
            }
        }
    }

    // ---- Raised (unselected) segment — rest, both stops. ----
    // Classic only, as of the 2026-07-29 revision. (Raised-pressed does not exist as a distinct
    // fill — resolveSegmentStyle snaps ANY pressed segment to full recess, so "pressed while raised"
    // measures identically to recessed below. Classic's raised-hover case is excluded — it shares
    // the FILLED button's exact hover fill (gap G5 unification) and is covered by the same
    // authorized exception 2 of 3 below.) AeroBlue/AeroDark's segment-raised rest/hover now live in
    // aeroBlueAeroDarkAcceptedSubFloorLabelDeviation instead, same reasoning as the filled button
    // above — they share the exact same fill formula (gap G5), so the same values apply.

    @Test
    fun classicRaisedSegmentRestClearsTheFloorOnBothStops() {
        val colors = AeroColorScheme.Classic
        val label = colors.labelOnFilledSurface
        val style = resolveSegmentStyle(colors, selectedProgress = 0f, hovered = false, pressed = false, enabled = true)
        assertClearsFloor(label, colors, style, "Classic segment-raised-rest")
    }

    // ---- Recessed (selected) segment — Classic clears; AeroBlue/AeroDark are the ONE segment
    // ---- exception, covered by name below rather than silently asserted here. ----

    @Test
    fun recessedSegmentClearsTheFloorInClassic() {
        val colors = AeroColorScheme.Classic
        val label = colors.labelOnFilledSurface
        listOf(
            "rest" to resolveSegmentStyle(colors, selectedProgress = 1f, hovered = false, pressed = false, enabled = true),
            "hover" to resolveSegmentStyle(colors, selectedProgress = 1f, hovered = true, pressed = false, enabled = true),
        ).forEach { (state, style) ->
            assertClearsFloor(label, colors, style, "Classic segment-recessed-$state")
        }
    }

    /**
     * AeroBlue's recessed (selected) segment at rest fully clears the floor with the white label
     * (`6.525`/`4.602`, both stops) — unlike AeroDark's equivalent case and unlike either scheme's
     * recessed-hover case, both of which are below the floor and covered by
     * [aeroBlueAeroDarkAcceptedSubFloorLabelDeviation] instead. Asserted here, normally, rather than
     * folded into that deviation, because it is NOT an exception — it is a fill that already meets
     * [MIN_LABEL_CONTRAST] on its own.
     */
    @Test
    fun recessedSegmentAlsoClearsTheFloorInAeroBlueAtRest() {
        val colors = AeroColorScheme.AeroBlue
        val label = colors.labelOnFilledSurface
        val style = resolveSegmentStyle(colors, selectedProgress = 1f, hovered = false, pressed = false, enabled = true)
        assertClearsFloor(label, colors, style, "AeroBlue segment-recessed-rest")
    }

    /**
     * **Authorized exception 1 of 3 — the ONE accepted scheme-level label-contrast deviation:
     * AeroBlue/AeroDark's white label sits below the WCAG 4.5:1 floor on most of their opaque-fill
     * (filled button, segment-raised, segment-recessed-hover) rest/hover/press cases.**
     *
     * **History.** Plan 20-09 originally shipped a surface-polarity rule here — dark label on
     * opaque fills, light on outlined — which was measurement-driven and cleared this floor on the
     * opaque polarity (that rule's own now-void exception covered only the recessed segment's
     * `fillTop`, black token, 3.218-4.228 — see git history / `20-09-SUMMARY.md`'s amendment). The
     * maintainer then reviewed that rule running live across all three themes and rejected it on
     * appearance: with a dark label on filled surfaces, AeroBlue/AeroDark read as a different visual
     * language from Classic and from the rest of the library, which is light-on-dark throughout.
     * They were shown this exact cost table, below, in writing twice — including a live run of the
     * white variant across all three themes — before approving white on both polarities
     * ("одобряю", 2026-07-29). The surface-polarity measurement that surfaced this trade-off in the
     * first place, [com.mordred.aero.verification.VER10OneLabelColorPerThemeTest], is unchanged —
     * it has always measured white as the single best candidate for every scheme; this decision
     * reverts to exactly that combined-winner measurement rather than the per-polarity rescue.
     *
     * **Full measured table** (white label, both fill stops, from the real
     * [resolveButtonStyle]/[resolveSegmentStyle] outputs — re-verify via
     * `VER10OneLabelColorPerThemeTest.printsFullPerSchemePerFillContrastTable` rather than trusting
     * this transcription):
     *
     * | Case                          | AeroBlue top / bottom     | AeroDark top / bottom     |
     * |-------------------------------|----------------------------|-----------------------------|
     * | filled-rest / segment-raised  | 3.096 / **4.597**          | 2.720 / 4.121               |
     * | filled-hover / raised-hover    | 2.801 / 3.996              | 2.493 / 3.589               |
     * | filled-press                  | **4.597** / 3.096          | 4.121 / 2.720               |
     * | segment-recessed-hover        | 5.450 / 4.002              | 4.966 / 3.553               |
     * | segment-recessed (rest)       | 6.525 / 4.602 — clears, see [recessedSegmentAlsoClearsTheFloorInAeroBlueAtRest] | 5.936 / **4.079** |
     *
     * (Bold values already clear 4.5:1 on their own — pinned as regression bounds here anyway, for
     * one uniform mechanism per state rather than splitting stops across two code paths.)
     *
     * Segment-raised shares the filled button's exact fill formula (gap G5 unification) — both
     * consumers are asserted below against the real `resolveSegmentStyle` output, not assumed
     * identical from that shared-formula fact.
     *
     * **Why accepted rather than fixed:** flipping any of these fills back to black would rescue
     * that one case at the cost of reintroducing exactly the defect plan 20-09 removed — a label
     * that changes colour depending on element or interaction state. Retuning `RECESSED_FILL_DARKEN`
     * or `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` is forbidden by those constants' own
     * KDoc (the maintainer's 19-08 acceptance of the recess/depth reading) and by this plan's
     * explicit instruction not to touch any fill constant — the remedy of darkening the opaque fills
     * (as Classic's dark `primary` already does) would remove this deviation entirely but was
     * deliberately not taken now, because it would change button appearance signed off in Phase 17
     * (tracked as a pending todo for a future plan to pick up, not silently deferred).
     *
     * **Regression bound, not a floor requirement:** each measured ratio must stay `>=` the value
     * pinned here (within [FLOAT_COMPARISON_TOLERANCE]) — accepting this cost is not the same as
     * accepting further drift below it. [MIN_LABEL_CONTRAST] itself is never touched (D-13).
     * **20-07's three-theme sign-off still judges this visually** — this test only guarantees the
     * finding cannot get worse unnoticed.
     */
    @Test
    fun aeroBlueAeroDarkAcceptedSubFloorLabelDeviation() {
        val aeroBlue = AeroColorScheme.AeroBlue
        val aeroDark = AeroColorScheme.AeroDark
        val blueLabel = aeroBlue.labelOnFilledSurface
        val darkLabel = aeroDark.labelOnFilledSurface

        // Filled button — rest/hover/press, both stops.
        assertBothStopsAuthorized(
            "AeroBlue filled-rest", blueLabel, aeroBlue,
            resolveButtonStyle(aeroBlue, outlined = false, hovered = false, pressed = false, focused = false, enabled = true),
            AEROBLUE_FILLED_REST_TOP_AUTHORIZED_RATIO, AEROBLUE_FILLED_REST_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroBlue filled-hover", blueLabel, aeroBlue,
            resolveButtonStyle(aeroBlue, outlined = false, hovered = true, pressed = false, focused = false, enabled = true),
            AEROBLUE_FILLED_HOVER_TOP_AUTHORIZED_RATIO, AEROBLUE_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroBlue filled-press", blueLabel, aeroBlue,
            resolveButtonStyle(aeroBlue, outlined = false, hovered = false, pressed = true, focused = false, enabled = true),
            AEROBLUE_FILLED_PRESS_TOP_AUTHORIZED_RATIO, AEROBLUE_FILLED_PRESS_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark filled-rest", darkLabel, aeroDark,
            resolveButtonStyle(aeroDark, outlined = false, hovered = false, pressed = false, focused = false, enabled = true),
            AERODARK_FILLED_REST_TOP_AUTHORIZED_RATIO, AERODARK_FILLED_REST_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark filled-hover", darkLabel, aeroDark,
            resolveButtonStyle(aeroDark, outlined = false, hovered = true, pressed = false, focused = false, enabled = true),
            AERODARK_FILLED_HOVER_TOP_AUTHORIZED_RATIO, AERODARK_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark filled-press", darkLabel, aeroDark,
            resolveButtonStyle(aeroDark, outlined = false, hovered = false, pressed = true, focused = false, enabled = true),
            AERODARK_FILLED_PRESS_TOP_AUTHORIZED_RATIO, AERODARK_FILLED_PRESS_BOTTOM_AUTHORIZED_RATIO,
        )

        // Raised (unselected) segment — shares the filled button's exact fill formula (gap G5),
        // asserted against the real resolveSegmentStyle output rather than assumed identical.
        assertBothStopsAuthorized(
            "AeroBlue segment-raised-rest", blueLabel, aeroBlue,
            resolveSegmentStyle(aeroBlue, selectedProgress = 0f, hovered = false, pressed = false, enabled = true),
            AEROBLUE_FILLED_REST_TOP_AUTHORIZED_RATIO, AEROBLUE_FILLED_REST_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroBlue segment-raised-hover", blueLabel, aeroBlue,
            resolveSegmentStyle(aeroBlue, selectedProgress = 0f, hovered = true, pressed = false, enabled = true),
            AEROBLUE_FILLED_HOVER_TOP_AUTHORIZED_RATIO, AEROBLUE_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark segment-raised-rest", darkLabel, aeroDark,
            resolveSegmentStyle(aeroDark, selectedProgress = 0f, hovered = false, pressed = false, enabled = true),
            AERODARK_FILLED_REST_TOP_AUTHORIZED_RATIO, AERODARK_FILLED_REST_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark segment-raised-hover", darkLabel, aeroDark,
            resolveSegmentStyle(aeroDark, selectedProgress = 0f, hovered = true, pressed = false, enabled = true),
            AERODARK_FILLED_HOVER_TOP_AUTHORIZED_RATIO, AERODARK_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )

        // Recessed (selected) segment on hover — a second, darker fill than raised. AeroDark's
        // recessed-rest also fails (unlike AeroBlue's, see recessedSegmentAlsoClearsTheFloorInAeroBlueAtRest).
        assertBothStopsAuthorized(
            "AeroBlue segment-recessed-hover", blueLabel, aeroBlue,
            resolveSegmentStyle(aeroBlue, selectedProgress = 1f, hovered = true, pressed = false, enabled = true),
            AEROBLUE_RECESSED_HOVER_TOP_AUTHORIZED_RATIO, AEROBLUE_RECESSED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark segment-recessed-rest", darkLabel, aeroDark,
            resolveSegmentStyle(aeroDark, selectedProgress = 1f, hovered = false, pressed = false, enabled = true),
            AERODARK_RECESSED_TOP_AUTHORIZED_RATIO, AERODARK_RECESSED_BOTTOM_AUTHORIZED_RATIO,
        )
        assertBothStopsAuthorized(
            "AeroDark segment-recessed-hover", darkLabel, aeroDark,
            resolveSegmentStyle(aeroDark, selectedProgress = 1f, hovered = true, pressed = false, enabled = true),
            AERODARK_RECESSED_HOVER_TOP_AUTHORIZED_RATIO, AERODARK_RECESSED_HOVER_BOTTOM_AUTHORIZED_RATIO,
        )
    }

    /**
     * **Authorized exception 2 of 3 — filled AeroButton / raised segment `fillTop` on HOVER,
     * light token, Classic.** Classic's `labelOnFilledSurface = Color.White` (its `primary` is dark,
     * so white serves every opaque fill — see [AeroColorScheme.labelOnFilledSurface]'s KDoc). The
     * hover-lightened `fillTop` measures `4.455385` against white — misses the 4.5:1 floor by
     * `0.045`, a hair's breadth. (`fillBottom` clears at `6.001`.) The raised (unselected) segment
     * shares this EXACT fill formula with the filled button (gap G5's "match the button" unification
     * — both darken `colors.primary` by the same imported constants), so this single exception
     * covers both consumers of the fill rather than being a separate per-component finding.
     *
     * **Why accepted rather than fixed:** Classic is this plan's OWN reference — "white label
     * everywhere, independent of element and state" is the target the other two schemes are brought
     * in line with, not a case to be changed (this plan's goal-backward truth). Flipping Classic's
     * filled-hover label to black would violate that invariance for the sake of one hover fill,
     * reintroducing per-state colour. The margin (0.045) is asserted as a regression bound so a
     * future change cannot silently widen it further below the floor.
     */
    @Test
    fun classicFilledHoverIsTheOneAuthorizedLightTokenException() {
        val colors = AeroColorScheme.Classic
        val label = colors.labelOnFilledSurface
        val buttonHover = resolveButtonStyle(colors, outlined = false, hovered = true, pressed = false, focused = false, enabled = true)
        val segmentRaisedHover = resolveSegmentStyle(colors, selectedProgress = 0f, hovered = true, pressed = false, enabled = true)

        assertAuthorizedException(
            "Classic filled-button-hover fillTop",
            topRatio(label, colors, buttonHover),
            CLASSIC_HOVER_TOP_AUTHORIZED_RATIO,
        )
        assertAuthorizedException(
            "Classic segment-raised-hover fillTop",
            topRatio(label, colors, segmentRaisedHover),
            CLASSIC_HOVER_TOP_AUTHORIZED_RATIO,
        )
    }

    /**
     * **Authorized exception 3 of 3 — disabled surfaces, all schemes, WCAG 1.4.3 exemption.**
     * Disabled/inactive user-interface components are explicitly exempt from the 1.4.3 (Contrast
     * Minimum) success criterion by the standard itself — this is not a defect this file certifies
     * away by measurement, it is a case the standard never asks these fills to meet. As of the
     * 2026-07-29 white-label revision, all three schemes' disabled fill now clears the floor anyway
     * (AeroBlue `7.30`, AeroDark `8.84`, Classic `9.49`) — the exemption is kept regardless, since
     * disabled controls are never REQUIRED to meet it even when they happen to. Regression bounds
     * (not a floor requirement) still guard every scheme's measured ratio so a future fill change
     * cannot silently make disabled text meaningfully worse than today without this test noticing.
     */
    @Test
    fun disabledSurfacesAreExemptFromTheContrastFloorPerWcag143() {
        val aeroBlue = AeroColorScheme.AeroBlue
        val aeroDark = AeroColorScheme.AeroDark
        val classic = AeroColorScheme.Classic

        val aeroBlueDisabledRatio = topRatio(
            aeroBlue.labelOnFilledSurface, aeroBlue,
            resolveButtonStyle(aeroBlue, outlined = false, hovered = false, pressed = false, focused = false, enabled = false),
        )
        val aeroDarkDisabledRatio = topRatio(
            aeroDark.labelOnFilledSurface, aeroDark,
            resolveButtonStyle(aeroDark, outlined = false, hovered = false, pressed = false, focused = false, enabled = false),
        )
        val classicDisabledRatio = topRatio(
            classic.labelOnFilledSurface, classic,
            resolveButtonStyle(classic, outlined = false, hovered = false, pressed = false, focused = false, enabled = false),
        )

        assertTrue(
            aeroBlueDisabledRatio >= AEROBLUE_DISABLED_AUTHORIZED_RATIO - FLOAT_COMPARISON_TOLERANCE,
            "AeroBlue filled-button-disabled: measured ratio $aeroBlueDisabledRatio must stay at " +
                "least the documented $AEROBLUE_DISABLED_AUTHORIZED_RATIO — exempt from " +
                "MIN_LABEL_CONTRAST per WCAG 1.4.3 (disabled controls) regardless, but not from a " +
                "regression bound"
        )
        assertTrue(
            aeroDarkDisabledRatio >= AERODARK_DISABLED_AUTHORIZED_RATIO - FLOAT_COMPARISON_TOLERANCE,
            "AeroDark filled-button-disabled: measured ratio $aeroDarkDisabledRatio must stay at " +
                "least the documented $AERODARK_DISABLED_AUTHORIZED_RATIO — exempt from " +
                "MIN_LABEL_CONTRAST per WCAG 1.4.3 (disabled controls) regardless, but not from a " +
                "regression bound"
        )
        assertTrue(
            classicDisabledRatio >= MIN_LABEL_CONTRAST,
            "Classic filled-button-disabled: measured ratio $classicDisabledRatio is documented as " +
                "clearing the floor comfortably even though disabled surfaces are WCAG-1.4.3-exempt " +
                "— a regression below the floor here would still be worth investigating"
        )
    }

    // ---- Shared assertion helpers. ----

    private fun assertClearsFloor(label: Color, colors: AeroColorScheme, style: AeroSurfaceStyle, description: String) {
        val compositedTop = style.fillTop.compositeOver(colors.background)
        val compositedBottom = style.fillBottom.compositeOver(colors.background)
        val topRatio = contrastRatio(label, compositedTop)
        val bottomRatio = contrastRatio(label, compositedBottom)
        assertTrue(
            topRatio >= MIN_LABEL_CONTRAST,
            "$description: label-to-fillTop contrast ratio $topRatio must be at least $MIN_LABEL_CONTRAST"
        )
        assertTrue(
            bottomRatio >= MIN_LABEL_CONTRAST,
            "$description: label-to-fillBottom contrast ratio $bottomRatio must be at least $MIN_LABEL_CONTRAST"
        )
    }

    private fun topRatio(label: Color, colors: AeroColorScheme, style: AeroSurfaceStyle): Float =
        contrastRatio(label, style.fillTop.compositeOver(colors.background))

    private fun bottomRatio(label: Color, colors: AeroColorScheme, style: AeroSurfaceStyle): Float =
        contrastRatio(label, style.fillBottom.compositeOver(colors.background))

    /** Asserts both fill stops of [style] against their own pinned regression-bound ratio — see
     * [aeroBlueAeroDarkAcceptedSubFloorLabelDeviation]. */
    private fun assertBothStopsAuthorized(
        description: String,
        label: Color,
        colors: AeroColorScheme,
        style: AeroSurfaceStyle,
        authorizedTop: Float,
        authorizedBottom: Float,
    ) {
        assertAuthorizedException("$description fillTop", topRatio(label, colors, style), authorizedTop)
        assertAuthorizedException("$description fillBottom", bottomRatio(label, colors, style), authorizedBottom)
    }

    private fun assertAuthorizedException(description: String, measured: Float, authorized: Float) {
        assertTrue(
            measured >= authorized - FLOAT_COMPARISON_TOLERANCE,
            "$description: measured ratio $measured must stay at least the authorized $authorized " +
                "(within $FLOAT_COMPARISON_TOLERANCE) — this is one of the THREE authorized " +
                "below-floor exceptions (see this test's KDoc); a regression below the authorized " +
                "value is not covered by that authorization and must fail"
        )
    }
}

/**
 * WCAG 1.4.3 floor for NORMAL-size text contrast (4.5:1) — the button/segment label is 14sp
 * regular body text, neither large-text-exempt nor a graphical UI-component in the 3:1-floor
 * sense. Never lowered under any circumstance (D-13).
 */
private const val MIN_LABEL_CONTRAST: Float = 4.5f

/**
 * See [AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation] — the one
 * accepted scheme-level deviation. Measured 2026-07-29 via
 * `VER10OneLabelColorPerThemeTest.printsFullPerSchemePerFillContrastTable`'s STANDARD_OUT (white
 * label, both fill stops, real `resolveButtonStyle`/`resolveSegmentStyle` output) — NOT
 * hand-guessed, and NOT inherited from the retired black-token version of this exception (the void
 * `3.2184236f`/`3.853091f`/`3.5377297f`/`4.228463f` recessed-segment-only numbers this replaces).
 */
private const val AEROBLUE_FILLED_REST_TOP_AUTHORIZED_RATIO: Float = 3.096348f
private const val AEROBLUE_FILLED_REST_BOTTOM_AUTHORIZED_RATIO: Float = 4.5972657f
private const val AEROBLUE_FILLED_HOVER_TOP_AUTHORIZED_RATIO: Float = 2.8013132f
private const val AEROBLUE_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO: Float = 3.9962833f
private const val AEROBLUE_FILLED_PRESS_TOP_AUTHORIZED_RATIO: Float = 4.5972657f
private const val AEROBLUE_FILLED_PRESS_BOTTOM_AUTHORIZED_RATIO: Float = 3.096348f
private const val AEROBLUE_RECESSED_HOVER_TOP_AUTHORIZED_RATIO: Float = 5.4501696f
private const val AEROBLUE_RECESSED_HOVER_BOTTOM_AUTHORIZED_RATIO: Float = 4.00162f

private const val AERODARK_FILLED_REST_TOP_AUTHORIZED_RATIO: Float = 2.7195716f
private const val AERODARK_FILLED_REST_BOTTOM_AUTHORIZED_RATIO: Float = 4.1210356f
private const val AERODARK_FILLED_HOVER_TOP_AUTHORIZED_RATIO: Float = 2.4926789f
private const val AERODARK_FILLED_HOVER_BOTTOM_AUTHORIZED_RATIO: Float = 3.5886981f
private const val AERODARK_FILLED_PRESS_TOP_AUTHORIZED_RATIO: Float = 4.1210356f
private const val AERODARK_FILLED_PRESS_BOTTOM_AUTHORIZED_RATIO: Float = 2.7195716f
private const val AERODARK_RECESSED_TOP_AUTHORIZED_RATIO: Float = 5.93601f
private const val AERODARK_RECESSED_BOTTOM_AUTHORIZED_RATIO: Float = 4.078655f
private const val AERODARK_RECESSED_HOVER_TOP_AUTHORIZED_RATIO: Float = 4.966343f
private const val AERODARK_RECESSED_HOVER_BOTTOM_AUTHORIZED_RATIO: Float = 3.553366f

/** See [AeroButtonContrastRegressionTest.classicFilledHoverIsTheOneAuthorizedLightTokenException]. */
private const val CLASSIC_HOVER_TOP_AUTHORIZED_RATIO: Float = 4.455385f

/**
 * See [AeroButtonContrastRegressionTest.disabledSurfacesAreExemptFromTheContrastFloorPerWcag143].
 * Re-measured 2026-07-29 against the white label (replacing the void black-token values
 * `2.8749816f`/`2.3742561f` this exemption previously guarded) — both now comfortably clear
 * [MIN_LABEL_CONTRAST] anyway, though the WCAG 1.4.3 exemption is kept regardless.
 */
private const val AEROBLUE_DISABLED_AUTHORIZED_RATIO: Float = 7.304394f
private const val AERODARK_DISABLED_AUTHORIZED_RATIO: Float = 8.844874f

/**
 * Float-comparison slack for every authorized-exception regression bound above — generous enough
 * to absorb ULP-level differences in the RGB-mix/luminance math across JVM builds, tight enough
 * that a real regression cannot hide inside it.
 */
private const val FLOAT_COMPARISON_TOLERANCE: Float = 0.001f

/**
 * Standard WCAG 2.x contrast ratio: the lighter of the two relative luminances plus `0.05f`,
 * divided by the darker plus `0.05f`. Deliberately test-local and independently written — NEVER
 * imports any production contrast helper (D-13), so a wrong production formula cannot certify
 * itself here. Only meaningful between two fully opaque colours — callers composite over an
 * opaque backdrop first.
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
