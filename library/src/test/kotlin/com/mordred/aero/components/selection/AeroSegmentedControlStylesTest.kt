package com.mordred.aero.components.selection

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.PRESSED_INNER_SHADOW
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import com.mordred.aero.theme.pressedRecess
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for [resolveSegmentStyle] —
 * the pure per-segment style resolution point wired into [AeroSegmentedControl] in 19-03 Task 1.
 * Exercised over both [AeroColorScheme.AeroBlue] (translucent tokens) and
 * [AeroColorScheme.Classic] (fully-opaque tokens), mirroring
 * [com.mordred.aero.components.range.AeroSliderStylesTest]'s "reconstruct-expected-from-scratch"
 * convention.
 *
 * Corner radius is independently hardcoded as `4.dp` below, matching
 * [AeroSegmentedControl.kt]'s own private `SEGMENT_CORNER_RADIUS` constant — this file
 * deliberately never imports that constant or calls [resolveSegmentStyle] to build an expectation,
 * so a drift in either constant is caught rather than silently agreeing with itself.
 */
class AeroSegmentedControlStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    private fun expectedRaised(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)

    private fun expectedRecessed(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).pressedRecess(PRESSED_INNER_SHADOW)

    @Test
    fun unselectedEnabledNoHoverNoPressEqualsRaisedRest() {
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
                "unselected, enabled, no hover/press must equal AeroSurfaceStyle.rest(colors, 4.dp) for $colors",
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
                "VSEL-03 ANTI-DRIFT: selected (selectedProgress = 1f) must equal " +
                    "AeroSurfaceStyle.rest(colors, 4.dp).pressedRecess(PRESSED_INNER_SHADOW) exactly " +
                    "for $colors — a mismatch means the segmented control and the pressed button " +
                    "have forked",
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
}
