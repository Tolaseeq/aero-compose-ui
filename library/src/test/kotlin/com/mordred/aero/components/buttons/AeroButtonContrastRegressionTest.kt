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
 * Value-level WCAG 4.5:1 contrast regression guard (SHW-16/VER-06, 20-09), superseding the
 * 20-04/D-12/D-13 version of this file that measured the retired per-call-site
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
 * **Exactly three named below-floor exceptions survive this re-derivation** — see each exception
 * test's own KDoc for its measured value and justification. [MIN_LABEL_CONTRAST] is never lowered
 * (D-13) to accommodate any of them.
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

    // ---- Filled AeroButton — rest/hover/press, both stops, all three themes. ----
    // (disabled excluded here — see disabledSurfacesAreExemptFromTheContrastFloorPerWcag143.
    // Classic's hover case is excluded too — it is authorized exception 2 of 3, asserted by name
    // in classicFilledHoverIsTheOneAuthorizedLightTokenException below rather than here.)

    @Test
    fun filledButtonRestHoverPressClearTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val label = colors.labelOnFilledSurface
            listOf(
                "rest" to resolveButtonStyle(colors, outlined = false, hovered = false, pressed = false, focused = false, enabled = true),
                "hover" to resolveButtonStyle(colors, outlined = false, hovered = true, pressed = false, focused = false, enabled = true),
                "press" to resolveButtonStyle(colors, outlined = false, hovered = false, pressed = true, focused = false, enabled = true),
            ).forEach { (state, style) ->
                if (name == "Classic" && state == "hover") return@forEach
                assertClearsFloor(label, colors, style, "$name filled-button-$state")
            }
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

    // ---- Raised (unselected) segment — rest/hover, both stops, all three themes. ----
    // (Raised-pressed does not exist as a distinct fill — resolveSegmentStyle snaps ANY pressed
    // segment to full recess, so "pressed while raised" measures identically to recessed below.
    // Classic's raised-hover case is excluded — it shares the FILLED button's exact hover fill
    // (gap G5 unification) and is covered by the same authorized exception 2 of 3 below.)

    @Test
    fun raisedSegmentRestAndHoverClearTheFloorOnBothStopsInAllThreeThemes() {
        schemes.forEach { (name, colors) ->
            val label = colors.labelOnFilledSurface
            listOf(
                "rest" to resolveSegmentStyle(colors, selectedProgress = 0f, hovered = false, pressed = false, enabled = true),
                "hover" to resolveSegmentStyle(colors, selectedProgress = 0f, hovered = true, pressed = false, enabled = true),
            ).forEach { (state, style) ->
                if (name == "Classic" && state == "hover") return@forEach
                assertClearsFloor(label, colors, style, "$name segment-raised-$state")
            }
        }
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
     * **Authorized exception 1 of 3 — recessed (selected) segment `fillTop`, dark token, AeroBlue
     * and AeroDark.** Re-derived from scratch under the NEW polarity rule (superseding the 20-04
     * exception this replaces, which was derived under the retired per-fill algorithm and is no
     * longer valid — it authorized a different candidate, white, for this exact fill; that rescue
     * no longer exists once the label is fixed per scheme).
     *
     * Both schemes assign `labelOnFilledSurface = Color.Black` (AeroBlue/AeroDark's `primary` is a
     * light blue, so their OTHER opaque fills read comfortably with black — see
     * [AeroColorScheme.labelOnFilledSurface]'s KDoc). The recessed segment's fill is that same base
     * darkened further by `RECESSED_FILL_DARKEN` (0.20) on top of the pressed-recess transform,
     * which pushes `fillTop` past the point where black still clears 4.5:1:
     * - AeroBlue: rest/press `fillTop` = 3.218, hover `fillTop` = 3.853 (both < 4.5).
     * - AeroDark: rest/press `fillTop` = 3.538, hover `fillTop` = 4.228 (both < 4.5).
     * (`fillBottom` clears comfortably in every one of these cases — 4.563/5.248 AeroBlue,
     * 5.149/5.910 AeroDark — so only `fillTop` is the exception.)
     *
     * **Why this is accepted rather than fixed by flipping to white:** flipping the recessed
     * segment's label to white would make THIS ONE fill legible again at the cost of reintroducing
     * exactly the defect this plan exists to remove — a label that changes colour depending on
     * selection state, since every OTHER opaque fill in these two schemes (filled button, raised
     * segment) stays black. The maintainer's own framing names this precisely: "flipping to light
     * would reintroduce state-dependent colour — the very defect being removed." Retuning
     * `RECESSED_FILL_DARKEN` to rescue this instead is forbidden by that same constant's own KDoc
     * (the maintainer's 19-08 acceptance of the recess/depth reading, unchanged since).
     *
     * **Regression bound, not a floor requirement:** each measured ratio must stay `>=` the value
     * pinned here (within [FLOAT_COMPARISON_TOLERANCE]) — this exception can only stay flat or
     * improve, never silently get worse. [MIN_LABEL_CONTRAST] itself is never touched (D-13).
     * **20-07's three-theme sign-off still judges this visually** — this test only guarantees the
     * finding cannot get worse unnoticed.
     */
    @Test
    fun recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException() {
        val aeroBlue = AeroColorScheme.AeroBlue
        val aeroDark = AeroColorScheme.AeroDark

        assertAuthorizedException(
            "AeroBlue segment-recessed fillTop (rest/press)",
            topRatio(aeroBlue.labelOnFilledSurface, aeroBlue, resolveSegmentStyle(aeroBlue, 1f, hovered = false, pressed = false, enabled = true)),
            AEROBLUE_RECESSED_TOP_REST_AUTHORIZED_RATIO,
        )
        assertAuthorizedException(
            "AeroBlue segment-recessed fillTop (hover)",
            topRatio(aeroBlue.labelOnFilledSurface, aeroBlue, resolveSegmentStyle(aeroBlue, 1f, hovered = true, pressed = false, enabled = true)),
            AEROBLUE_RECESSED_TOP_HOVER_AUTHORIZED_RATIO,
        )
        assertAuthorizedException(
            "AeroDark segment-recessed fillTop (rest/press)",
            topRatio(aeroDark.labelOnFilledSurface, aeroDark, resolveSegmentStyle(aeroDark, 1f, hovered = false, pressed = false, enabled = true)),
            AERODARK_RECESSED_TOP_REST_AUTHORIZED_RATIO,
        )
        assertAuthorizedException(
            "AeroDark segment-recessed fillTop (hover)",
            topRatio(aeroDark.labelOnFilledSurface, aeroDark, resolveSegmentStyle(aeroDark, 1f, hovered = true, pressed = false, enabled = true)),
            AERODARK_RECESSED_TOP_HOVER_AUTHORIZED_RATIO,
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
     * away by measurement, it is a case the standard never asks these fills to meet. AeroBlue and
     * AeroDark's disabled fill (`flattenDisabled`) is dark enough that their `Black`
     * `labelOnFilledSurface` measures `2.87`/`2.37` against it; Classic's `White` token still
     * clears comfortably (`9.49`) even though it too is exempt. Regression bounds (not a floor
     * requirement) still guard AeroBlue/AeroDark's measured ratios so this exemption cannot silently
     * mask a future fill change making disabled text meaningfully worse than today.
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
                "MIN_LABEL_CONTRAST per WCAG 1.4.3 (disabled controls), but not from a regression bound"
        )
        assertTrue(
            aeroDarkDisabledRatio >= AERODARK_DISABLED_AUTHORIZED_RATIO - FLOAT_COMPARISON_TOLERANCE,
            "AeroDark filled-button-disabled: measured ratio $aeroDarkDisabledRatio must stay at " +
                "least the documented $AERODARK_DISABLED_AUTHORIZED_RATIO — exempt from " +
                "MIN_LABEL_CONTRAST per WCAG 1.4.3 (disabled controls), but not from a regression bound"
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

/** See [AeroButtonContrastRegressionTest.recessedSegmentDarkTokenIsTheOneAuthorizedSegmentException]. */
private const val AEROBLUE_RECESSED_TOP_REST_AUTHORIZED_RATIO: Float = 3.2184236f
private const val AEROBLUE_RECESSED_TOP_HOVER_AUTHORIZED_RATIO: Float = 3.853091f
private const val AERODARK_RECESSED_TOP_REST_AUTHORIZED_RATIO: Float = 3.5377297f
private const val AERODARK_RECESSED_TOP_HOVER_AUTHORIZED_RATIO: Float = 4.228463f

/** See [AeroButtonContrastRegressionTest.classicFilledHoverIsTheOneAuthorizedLightTokenException]. */
private const val CLASSIC_HOVER_TOP_AUTHORIZED_RATIO: Float = 4.455385f

/** See [AeroButtonContrastRegressionTest.disabledSurfacesAreExemptFromTheContrastFloorPerWcag143]. */
private const val AEROBLUE_DISABLED_AUTHORIZED_RATIO: Float = 2.8749816f
private const val AERODARK_DISABLED_AUTHORIZED_RATIO: Float = 2.3742561f

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
