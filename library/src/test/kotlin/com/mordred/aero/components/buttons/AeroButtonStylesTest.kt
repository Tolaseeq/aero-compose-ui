package com.mordred.aero.components.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for [resolveButtonStyle] —
 * the pure per-state resolution point wired into [AeroButtonSurface] in 17-02. Exercised over
 * both [AeroColorScheme.AeroBlue] (translucent tokens) and [AeroColorScheme.Classic] (fully-opaque
 * tokens) per RESEARCH.md's Validation Architecture.
 */
class AeroButtonStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    @Test
    fun restStateEqualsRestFactoryStyle() {
        schemes.forEach { colors ->
            val resolved = resolveButtonStyle(
                colors = colors,
                outlined = false,
                hovered = false,
                pressed = false,
                focused = false,
                enabled = true,
            )
            val expected = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)

            assertEquals(expected, resolved, "rest state must equal AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp) for $colors")
        }
    }

    @Test
    fun disabledWinsRegardlessOfHoveredPressedFocused() {
        schemes.forEach { colors ->
            val disabledOnly = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = false, enabled = false,
            )
            val disabledHovered = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = true, pressed = false, focused = false, enabled = false,
            )
            val disabledPressed = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = true, focused = false, enabled = false,
            )
            val disabledFocused = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = true, enabled = false,
            )
            val disabledAll = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = true, pressed = true, focused = true, enabled = false,
            )

            assertEquals(disabledOnly, disabledHovered, "disabled must win over hovered")
            assertEquals(disabledOnly, disabledPressed, "disabled must win over pressed")
            assertEquals(disabledOnly, disabledFocused, "disabled must win over focused")
            assertEquals(disabledOnly, disabledAll, "disabled must win over all other flags combined")

            // Disabled geometry itself is flattened, not merely the rest style passed through.
            assertEquals(0f, disabledOnly.glossAlpha, "disabled gloss must be zeroed")
            assertEquals(disabledOnly.fillTop, disabledOnly.fillBottom, "disabled fill must collapse to one tone")
        }
    }

    @Test
    fun pressedReturnsPressedRecessTransformedStyle() {
        schemes.forEach { colors ->
            val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)

            val pressed = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = true, focused = false, enabled = true,
            )

            assertEquals(rest.fillBottom, pressed.fillTop, "pressed fillTop must be rest's fillBottom (swapped)")
            assertEquals(rest.fillTop, pressed.fillBottom, "pressed fillBottom must be rest's fillTop (swapped)")
            assertEquals(0f, pressed.glossAlpha, "pressed gloss must be disabled")
            assertNotNull(pressed.innerShadow, "pressed style must carry a non-null innerShadow")
        }
    }

    @Test
    fun hoveredNotPressedReturnsHoverLightenTransformedStyle() {
        schemes.forEach { colors ->
            val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)

            val hovered = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = true, pressed = false, focused = false, enabled = true,
            )

            assertTrue(
                luminance(hovered.fillTop) > luminance(rest.fillTop),
                "hovered fillTop must be brighter than rest for $colors",
            )
            assertTrue(
                luminance(hovered.fillBottom) > luminance(rest.fillBottom),
                "hovered fillBottom must be brighter than rest for $colors",
            )
        }
    }

    @Test
    fun focusedOnlyReturnsUnchangedRestFill() {
        schemes.forEach { colors ->
            val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)

            val focusedOnly = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = true, enabled = true,
            )

            assertEquals(rest, focusedOnly, "focus-only must return the unchanged rest style — focus is carried by the glow ring, not a fill change")
        }
    }

    private fun luminance(color: Color): Float = (color.red + color.green + color.blue) / 3f
}
