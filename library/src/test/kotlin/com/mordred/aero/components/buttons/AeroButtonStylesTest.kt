package com.mordred.aero.components.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.darken
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

    /**
     * Reconstructs the BUTTON-scoped rest style [resolveButtonStyle] resolves internally (17-05
     * ROUND-2 sign-off gap-fix, FIX A + FIX B) — [AeroSurfaceStyle.rest]'s ornament-derived fill
     * overridden to a darker `primary`-derived two-tone, and its rimAlpha capped to
     * `minOf(glassBorder.alpha, 0.45)` — so tests can assert against the exact expected values
     * without duplicating [resolveButtonStyle]'s private constants.
     */
    private fun expectedButtonRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).copy(
            fillTop = colors.primary.darken(0.20f),
            fillBottom = colors.primary.darken(0.36f),
            rimAlpha = minOf(colors.glassBorder.alpha, 0.45f),
        )

    @Test
    fun restStateEqualsThemeAwareButtonRest() {
        schemes.forEach { colors ->
            val resolved = resolveButtonStyle(
                colors = colors,
                outlined = false,
                hovered = false,
                pressed = false,
                focused = false,
                enabled = true,
            )
            val expected = expectedButtonRest(colors)

            assertEquals(
                expected,
                resolved,
                "rest state must equal the theme-aware button rest (17-05 round-2: darker primary-derived " +
                    "fill + native-capped rim alpha) for $colors",
            )
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
            val rest = expectedButtonRest(colors)

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
            val rest = expectedButtonRest(colors)

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
            val rest = expectedButtonRest(colors)

            val focusedOnly = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = true, enabled = true,
            )

            assertEquals(rest, focusedOnly, "focus-only must return the unchanged rest style — focus is carried by the glow ring, not a fill change")
        }
    }

    /**
     * FIX A (17-05 ROUND-2 sign-off gap-fix): Classic's native `glassBorder` alpha is opaque
     * (1.0), so capping it at 0.45 leaves it EXACTLY where the round-1 fix left it — the operator
     * explicitly reported Classic already looked right and must not regress. AeroBlue's native
     * `glassBorder` alpha (~0.31) is dimmer than the 0.45 cap, so it must draw at its OWN native
     * alpha rather than being brightened up to the cap — this is the rim-too-bright fix.
     */
    @Test
    fun filledRimAlphaIsCappedAtNativeGlassBorderAlphaPerTheme() {
        val classic = resolveButtonStyle(
            colors = AeroColorScheme.Classic, outlined = false,
            hovered = false, pressed = false, focused = false, enabled = true,
        )
        val aeroBlue = resolveButtonStyle(
            colors = AeroColorScheme.AeroBlue, outlined = false,
            hovered = false, pressed = false, focused = false, enabled = true,
        )

        assertEquals(0.45f, classic.rimAlpha, 0.0001f, "Classic filled rimAlpha must stay 0.45 (unchanged — was already correct)")
        assertTrue(
            aeroBlue.rimAlpha < 0.45f,
            "AeroBlue filled rimAlpha (${aeroBlue.rimAlpha}) must be dimmer than the 0.45 cap — it must draw at its own native glassBorder alpha",
        )
        assertEquals(
            AeroColorScheme.AeroBlue.glassBorder.alpha,
            aeroBlue.rimAlpha,
            0.0001f,
            "AeroBlue filled rimAlpha must equal its own native glassBorder alpha (below the cap)",
        )
    }

    /**
     * FIX B (17-05 ROUND-2 sign-off gap-fix): the operator asked for a darker filled fill so
     * white button text stays legible against the light-blue Aero primaries — assert the
     * resolved rest fill is darker than the theme's own `primary` for both light-primary Aero
     * themes (AeroBlue, AeroDark).
     */
    @Test
    fun filledFillIsDarkerThanPrimaryOnLightAeroThemes() {
        listOf(AeroColorScheme.AeroBlue, AeroColorScheme.AeroDark).forEach { colors ->
            val rest = resolveButtonStyle(
                colors = colors, outlined = false,
                hovered = false, pressed = false, focused = false, enabled = true,
            )

            assertTrue(
                luminance(rest.fillTop) < luminance(colors.primary),
                "filled rest fillTop must be darker than $colors's primary (fillTop luminance=${luminance(rest.fillTop)}, primary luminance=${luminance(colors.primary)})",
            )
            assertTrue(
                luminance(rest.fillBottom) < luminance(colors.primary),
                "filled rest fillBottom must be darker than $colors's primary (fillBottom luminance=${luminance(rest.fillBottom)}, primary luminance=${luminance(colors.primary)})",
            )
        }
    }

    private fun luminance(color: Color): Float = (color.red + color.green + color.blue) / 3f
}
