package com.mordred.aero.components.selection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.PRESSED_INNER_SHADOW
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.darken
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import com.mordred.aero.theme.pressedRecess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for [resolveSegmentStyle] —
 * the pure per-segment style resolution point wired into [AeroSegmentedControl] in 19-03 Task 1.
 * Exercised over all three shipped schemes — [AeroColorScheme.AeroBlue] (translucent tokens),
 * [AeroColorScheme.AeroDark] (the scheme where CR-01's label-legibility defect measured worst) and
 * [AeroColorScheme.Classic] (fully-opaque tokens) — mirroring
 * [com.mordred.aero.components.range.AeroSliderStylesTest]'s "reconstruct-expected-from-scratch"
 * convention.
 *
 * Corner radius is independently hardcoded as `4.dp` below, matching
 * [AeroSegmentedControl.kt]'s own private `SEGMENT_CORNER_RADIUS` constant, and the raised/recessed
 * darken literals below are likewise hardcoded to match `RAISED_FILL_TOP_DARKEN`/
 * `RAISED_FILL_BOTTOM_DARKEN`/`RECESSED_FILL_DARKEN` (CR-01) — this file deliberately never
 * imports any of those constants or calls [resolveSegmentStyle] to build an expectation, so a
 * drift in either place is caught rather than silently agreeing with itself.
 */
class AeroSegmentedControlStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.AeroDark, AeroColorScheme.Classic)

    private fun expectedRaised(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).copy(
            fillTop = colors.primary.darken(0.58f),
            fillBottom = colors.primary.darken(0.61f),
        )

    private fun expectedRecessed(colors: AeroColorScheme): AeroSurfaceStyle {
        val pressedTransform = expectedRaised(colors).pressedRecess(PRESSED_INNER_SHADOW)
        return pressedTransform.copy(
            fillTop = pressedTransform.fillTop.darken(0.20f),
            fillBottom = pressedTransform.fillBottom.darken(0.20f),
        )
    }

    @Test
    fun unselectedEnabledNoHoverNoPressEqualsTheDarkenedRaisedBase() {
        schemes.forEach { colors ->
            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            assertEquals(
                expectedRaised(colors),
                resolved,
                "unselected, enabled, no hover/press must equal the rest preset with both fill " +
                    "stops replaced by the darkened accent token for $colors — a bare, unmodified " +
                    "rest preset here is the CR-01 illegible-label defect",
            )
        }
    }

    @Test
    fun selectedEqualsIndependentlyReconstructedPressedRecess() {
        schemes.forEach { colors ->
            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            assertEquals(
                expectedRecessed(colors),
                resolved,
                "VSEL-03 ANTI-DRIFT: selected (selectedProgress = 1f) must equal the darkened " +
                    "raised base put through pressedRecess(PRESSED_INNER_SHADOW) and then " +
                    "recess-darkened exactly for $colors — a mismatch means the segmented control " +
                    "and the pressed button have forked",
            )
        }
    }

    @Test
    fun midpointLerpsEachFillStopExactly() {
        schemes.forEach { colors ->
            val raised = expectedRaised(colors)
            val recessed = expectedRecessed(colors)

            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0.5f,
                hovered = false,
                pressed = false,
                enabled = true,
            )

            assertEquals(
                lerp(raised.fillTop, recessed.fillTop, 0.5f),
                resolved.fillTop,
                "midpoint fillTop must equal the exact lerp of raised/recessed fillTop at 0.5f for $colors",
            )
            assertEquals(
                lerp(raised.fillBottom, recessed.fillBottom, 0.5f),
                resolved.fillBottom,
                "midpoint fillBottom must equal the exact lerp of raised/recessed fillBottom at 0.5f for $colors",
            )
        }
    }

    @Test
    fun pressedWhileUnselectedEqualsRecessedStyle() {
        schemes.forEach { colors ->
            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0f,
                hovered = false,
                pressed = true,
                enabled = true,
            )
            assertEquals(
                expectedRecessed(colors),
                resolved,
                "pressed while unselected must equal the recessed style — press reaches full depth " +
                    "immediately (UI-SPEC Press row) for $colors",
            )
        }
    }

    @Test
    fun pressedWhileSelectedEqualsSelectedResult() {
        schemes.forEach { colors ->
            val selectedResult = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            val pressedAndSelected = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = false,
                pressed = true,
                enabled = true,
            )
            assertEquals(
                selectedResult,
                pressedAndSelected,
                "pressed while already selected must be a visual no-op — an already-recessed " +
                    "segment cannot press deeper for $colors",
            )
        }
    }

    @Test
    fun hoveredUnselectedEqualsRaisedRestHoverLighten() {
        schemes.forEach { colors ->
            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0f,
                hovered = true,
                pressed = false,
                enabled = true,
            )
            assertEquals(
                expectedRaised(colors).hoverLighten(),
                resolved,
                "hovered + unselected must equal rest(colors, 4.dp).hoverLighten() for $colors",
            )
        }
    }

    @Test
    fun hoveredSelectedEqualsRecessedHoverLightenNeverUnselectedHoverStyle() {
        schemes.forEach { colors ->
            val resolved = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = true,
                pressed = false,
                enabled = true,
            )
            assertEquals(
                expectedRecessed(colors).hoverLighten(),
                resolved,
                "hovered + selected must equal the recessed style with hoverLighten() composed on " +
                    "top — hover composes rather than replaces (D-11 shape) for $colors",
            )
            val unselectedHoverStyle = expectedRaised(colors).hoverLighten()
            assert(resolved != unselectedHoverStyle) {
                "hovered + selected must NEVER equal the unselected-hover style for $colors"
            }
        }
    }

    @Test
    fun disabledWinsRegardlessOfSelectedProgressOrHoveredOrPressed() {
        schemes.forEach { colors ->
            val disabledUnselected = resolveSegmentStyle(
                colors = colors, selectedProgress = 0f, hovered = false, pressed = false, enabled = false,
            )
            val disabledSelected = resolveSegmentStyle(
                colors = colors, selectedProgress = 1f, hovered = false, pressed = false, enabled = false,
            )
            val disabledSelectedHovered = resolveSegmentStyle(
                colors = colors, selectedProgress = 1f, hovered = true, pressed = false, enabled = false,
            )
            val disabledUnselectedPressed = resolveSegmentStyle(
                colors = colors, selectedProgress = 0f, hovered = false, pressed = true, enabled = false,
            )
            val disabledEverything = resolveSegmentStyle(
                colors = colors, selectedProgress = 1f, hovered = true, pressed = true, enabled = false,
            )

            assertEquals(
                expectedRaised(colors).flattenDisabled(colors),
                disabledUnselected,
                "disabled + unselected must equal raised rest flattened for $colors",
            )
            assertEquals(
                expectedRecessed(colors).flattenDisabled(colors),
                disabledSelected,
                "disabled + selected must equal recessed style flattened (recess geometry stays " +
                    "visible, just dead-toned) for $colors",
            )
            assertEquals(
                disabledSelected,
                disabledSelectedHovered,
                "disabled must win over hovered — no hover brightening when disabled for $colors",
            )
            assertEquals(
                expectedRecessed(colors).flattenDisabled(colors),
                disabledUnselectedPressed,
                "disabled + pressed (unselected) must still resolve to recessed-then-flattened, " +
                    "since press reaches full depth before the disabled short-circuit for $colors",
            )
            assertEquals(
                disabledSelected,
                disabledEverything,
                "disabled must win over every other flag combined for $colors",
            )
        }
    }

    @Test
    fun recessedFillIsStrictlyDarkerThanRaisedFillNeverJustExchanged() {
        // Gap G3 (VSEL-03): exchanging the two fill stops preserves their mean exactly, so a
        // recessed segment that is not darker than its raised neighbour cannot satisfy "exactly
        // one visibly pushed in" — this proves the DIRECTION of the value change, not the exact
        // magnitude (which remains Claude's discretion, retunable at sign-off).
        schemes.forEach { colors ->
            val unselected = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 0f,
                hovered = false,
                pressed = false,
                enabled = true,
            )
            val selected = resolveSegmentStyle(
                colors = colors,
                selectedProgress = 1f,
                hovered = false,
                pressed = false,
                enabled = true,
            )

            val unselectedLuminanceSum = unselected.fillTop.luminance() + unselected.fillBottom.luminance()
            val selectedLuminanceSum = selected.fillTop.luminance() + selected.fillBottom.luminance()

            assertTrue(
                selectedLuminanceSum < unselectedLuminanceSum,
                "the recessed (selected) segment's total fill luminance ($selectedLuminanceSum) must " +
                    "be strictly below the raised (unselected) segment's ($unselectedLuminanceSum) for " +
                    "$colors — a recessed segment that is not darker than its raised neighbours cannot " +
                    "satisfy VSEL-03's 'exactly one visibly pushed in' (gap G3)",
            )
            assertEquals(
                0f,
                selected.glossAlpha,
                "the recessed (selected) segment must have glossAlpha = 0f — gloss suppression is " +
                    "part of the imported pressedRecess transform and must survive the fill " +
                    "correction unchanged for $colors",
            )
        }
    }

    @Test
    fun everySegmentFillKeepsTheOnSurfaceLabelAboveTheMinimumContrastRatio() {
        // CR-01: the label is always colors.onSurface at full alpha, so if either resolved fill
        // stop drops too close to it in value, the label is present, correctly coloured, and
        // invisible. Both endpoints of the selection axis are checked, because pressedRecess
        // exchanges fillTop/fillBottom — the top stop is the lighter (worst-case) one at the
        // unselected end, but the bottom stop is the lighter one once fully selected.
        schemes.forEach { colors ->
            listOf(0f, 1f).forEach { selectedProgress ->
                val resolved = resolveSegmentStyle(
                    colors = colors,
                    selectedProgress = selectedProgress,
                    hovered = false,
                    pressed = false,
                    enabled = true,
                )

                // The ratio below is only meaningful between opaque colours; a translucent fill
                // stop here would mean this guard has silently stopped measuring anything real.
                assertEquals(
                    1f,
                    resolved.fillTop.alpha,
                    "fillTop must be fully opaque for $colors at selectedProgress=$selectedProgress " +
                        "— the contrast ratio below is only meaningful between opaque colours",
                )
                assertEquals(
                    1f,
                    resolved.fillBottom.alpha,
                    "fillBottom must be fully opaque for $colors at selectedProgress=$selectedProgress " +
                        "— the contrast ratio below is only meaningful between opaque colours",
                )

                val topRatio = contrastRatio(colors.onSurface, resolved.fillTop)
                val bottomRatio = contrastRatio(colors.onSurface, resolved.fillBottom)

                assertTrue(
                    topRatio >= MIN_LABEL_CONTRAST,
                    "label-to-fillTop contrast ratio $topRatio for $colors at " +
                        "selectedProgress=$selectedProgress must be at least $MIN_LABEL_CONTRAST — " +
                        "an unselected segment whose label does not clear this ratio is the CR-01 " +
                        "defect: a label that is present, correctly coloured, and invisible",
                )
                assertTrue(
                    bottomRatio >= MIN_LABEL_CONTRAST,
                    "label-to-fillBottom contrast ratio $bottomRatio for $colors at " +
                        "selectedProgress=$selectedProgress must be at least $MIN_LABEL_CONTRAST — " +
                        "an unselected segment whose label does not clear this ratio is the CR-01 " +
                        "defect: a label that is present, correctly coloured, and invisible",
                )
            }
        }
    }
}

/**
 * WCAG 1.4.3 floor for NORMAL-size text contrast (4.5:1) — not the 3.0:1 non-text/large-text
 * floor this guard used before WR-12. The segment label is 14sp regular body text: it is neither
 * large-text-exempt (that carve-out needs ~18pt regular or ~14pt bold) nor a graphical "UI
 * component" in the 3:1-floor sense (borders, icons, focus indicators) — it is rendered text, so
 * the ratio that actually governs its legibility is 4.5:1, matching every other text-bearing
 * component in this library (button, list, switch), each of which clears 4.5:1+ via its own
 * darken constants. This is still a value-level guard on the resolved fill stops (not a claim
 * about the composited pixel after gloss/bevel/rim are painted on top) — see coverage item D3 in
 * 19-10-SUMMARY.md for that separate, human-judged claim.
 */
private const val MIN_LABEL_CONTRAST: Float = 4.5f

/**
 * Standard WCAG 2.x contrast ratio: the lighter of the two relative luminances plus `0.05f`,
 * divided by the darker plus `0.05f`. Only meaningful between two fully opaque colours — callers
 * assert opacity first.
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
