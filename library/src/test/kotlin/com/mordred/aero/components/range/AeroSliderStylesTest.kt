package com.mordred.aero.components.range

import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for [resolveSliderThumbStyle]
 * / [resolveSliderTrackStyle] — the pure per-state resolution points wired into [AeroSlider]'s
 * custom `thumb =`/`track =` slots in 18-01. Exercised over both [AeroColorScheme.AeroBlue]
 * (translucent tokens) and [AeroColorScheme.Classic] (fully-opaque tokens) per 18-RESEARCH.md's
 * Validation Architecture.
 *
 * Corner-radius values below are independently hardcoded (10.dp thumb / 2.dp track), matching
 * [AeroSlider.kt]'s own private `THUMB_RADIUS`/`TRACK_CORNER_RADIUS` constants — mirrors
 * `AeroButtonStylesTest`'s convention of reconstructing the expected style from scratch rather
 * than importing the value under test, so a drift in either constant is caught.
 */
class AeroSliderStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    private fun expectedThumbRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.neutralRest(colors, cornerRadius = 10.dp)

    private fun expectedTrackRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 2.dp)

    @Test
    fun thumbRestStateEqualsThemeAwareNeutralRest() {
        schemes.forEach { colors ->
            val resolved = resolveSliderThumbStyle(
                colors = colors,
                hovered = false,
                pressed = false,
                isDragging = false,
                focused = false,
                enabled = true,
            )
            assertEquals(
                expectedThumbRest(colors),
                resolved,
                "rest state must equal the theme-aware neutralRest thumb style (D-01) for $colors",
            )
        }
    }

    @Test
    fun thumbDisabledWinsRegardlessOfHoveredPressedFocused() {
        schemes.forEach { colors ->
            val disabledOnly = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = false, isDragging = false, focused = false, enabled = false,
            )
            val disabledHovered = resolveSliderThumbStyle(
                colors = colors,
                hovered = true, pressed = false, isDragging = false, focused = false, enabled = false,
            )
            val disabledPressed = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = true, isDragging = false, focused = false, enabled = false,
            )
            val disabledDragging = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = false, isDragging = true, focused = false, enabled = false,
            )
            val disabledFocused = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = false, isDragging = false, focused = true, enabled = false,
            )
            val disabledAll = resolveSliderThumbStyle(
                colors = colors,
                hovered = true, pressed = true, isDragging = true, focused = true, enabled = false,
            )

            val expected = expectedThumbRest(colors).flattenDisabled(colors)

            assertEquals(expected, disabledOnly, "disabled must equal neutralRest(...).flattenDisabled(colors)")
            assertEquals(disabledOnly, disabledHovered, "disabled must win over hovered")
            assertEquals(disabledOnly, disabledPressed, "disabled must win over pressed")
            assertEquals(disabledOnly, disabledDragging, "disabled must win over isDragging")
            assertEquals(disabledOnly, disabledFocused, "disabled must win over focused")
            assertEquals(disabledOnly, disabledAll, "disabled must win over all other flags combined")
        }
    }

    /**
     * D-04's divergence from the button precedent: pressed/dragging must NOT swap fillTop/fillBottom
     * like [com.mordred.aero.theme.pressedRecess] does — the thumb stays raised, "picked up," with
     * only a brighter gloss as the cue.
     */
    @Test
    fun pressedOrDraggingKeepsUnchangedFillWithBrighterGloss() {
        schemes.forEach { colors ->
            val rest = expectedThumbRest(colors)

            val pressed = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = true, isDragging = false, focused = false, enabled = true,
            )
            val dragging = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = false, isDragging = true, focused = false, enabled = true,
            )

            listOf(pressed, dragging).forEach { resolved ->
                assertEquals(rest.fillTop, resolved.fillTop, "pressed/dragging fillTop must be unchanged (NOT swapped) for $colors")
                assertEquals(rest.fillBottom, resolved.fillBottom, "pressed/dragging fillBottom must be unchanged (NOT swapped) for $colors")
                assertTrue(resolved.glossAlpha > rest.glossAlpha, "pressed/dragging glossAlpha must be brighter than rest for $colors")
            }
        }
    }

    @Test
    fun hoveredNotPressedReturnsHoverLightenTransformedStyle() {
        schemes.forEach { colors ->
            val rest = expectedThumbRest(colors)

            val hovered = resolveSliderThumbStyle(
                colors = colors,
                hovered = true, pressed = false, isDragging = false, focused = false, enabled = true,
            )

            assertEquals(rest.hoverLighten(), hovered, "hovered (not pressed/dragging) must equal neutralRest(...).hoverLighten() for $colors")
        }
    }

    @Test
    fun focusedOnlyReturnsUnchangedRestStyle() {
        schemes.forEach { colors ->
            val rest = expectedThumbRest(colors)

            val focusedOnly = resolveSliderThumbStyle(
                colors = colors,
                hovered = false, pressed = false, isDragging = false, focused = true, enabled = true,
            )

            assertEquals(rest, focusedOnly, "focus-only must return the unchanged rest style — focus is carried by the glow ring, not a fill change")
        }
    }

    @Test
    fun trackStyleEqualsThemeAwareAccentRestWhenEnabled() {
        schemes.forEach { colors ->
            val resolved = resolveSliderTrackStyle(colors, enabled = true)
            assertEquals(
                expectedTrackRest(colors),
                resolved,
                "enabled track style must equal the theme-aware accent AeroSurfaceStyle.rest (D-01) for $colors",
            )
        }
    }

    @Test
    fun trackStyleFlattensWhenDisabled() {
        schemes.forEach { colors ->
            val resolved = resolveSliderTrackStyle(colors, enabled = false)
            assertEquals(
                expectedTrackRest(colors).flattenDisabled(colors),
                resolved,
                "disabled track style must equal accent rest flattened (D-07) for $colors",
            )
        }
    }
}
