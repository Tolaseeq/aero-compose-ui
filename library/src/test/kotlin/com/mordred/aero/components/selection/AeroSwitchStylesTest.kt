package com.mordred.aero.components.selection

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for [resolveSwitchGrooveStyle]
 * / [resolveSwitchThumbStyle] — the pure per-state resolution points wired into [AeroSwitch] in
 * 19-02. Exercised over both [AeroColorScheme.AeroBlue] (translucent tokens) and
 * [AeroColorScheme.Classic] (fully-opaque tokens), mirroring `AeroSliderStylesTest`'s convention.
 *
 * Corner-radius values below are independently hardcoded (9.dp groove / 7.dp thumb), matching
 * [AeroSwitch.kt]'s own private `TRACK_CORNER_RADIUS`/`THUMB_CORNER_RADIUS` constants — the
 * expected styles are reconstructed from scratch via [AeroSurfaceStyle.neutralRest]/[AeroSurfaceStyle.rest],
 * never by calling the resolver under test, so a drift in either constant is caught.
 */
class AeroSwitchStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    private fun expectedNeutralGroove(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.neutralRest(colors, cornerRadius = 9.dp)

    private fun expectedAccentGroove(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.rest(colors, cornerRadius = 9.dp)

    private fun expectedThumbRest(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.neutralRest(colors, cornerRadius = 7.dp)

    // --- Groove ---

    @Test
    fun grooveUncheckedEnabledEqualsNeutralRest() {
        schemes.forEach { colors ->
            val resolved = resolveSwitchGrooveStyle(colors, checkedProgress = 0f, enabled = true)
            assertEquals(
                expectedNeutralGroove(colors),
                resolved,
                "unchecked (progress=0f) groove must equal neutralRest(colors, 9.dp) for $colors",
            )
        }
    }

    @Test
    fun grooveCheckedEnabledMovesOnlyTheFillStops() {
        schemes.forEach { colors ->
            val neutral = expectedNeutralGroove(colors)
            val accent = expectedAccentGroove(colors)
            val resolved = resolveSwitchGrooveStyle(colors, checkedProgress = 1f, enabled = true)

            assertEquals(accent.fillTop, resolved.fillTop, "checked groove fillTop must equal accent rest's fillTop (D-01) for $colors")
            assertEquals(accent.fillBottom, resolved.fillBottom, "checked groove fillBottom must equal accent rest's fillBottom (D-01) for $colors")
            assertEquals(neutral.glossColor, resolved.glossColor, "checked groove glossColor must stay the neutral value (D-02) for $colors")
            assertEquals(neutral.glossAlpha, resolved.glossAlpha, "checked groove glossAlpha must stay the neutral value (D-02) for $colors")
            assertEquals(neutral.bevelLight, resolved.bevelLight, "checked groove bevelLight must stay the neutral value (D-02) for $colors")
            assertEquals(neutral.bevelShadow, resolved.bevelShadow, "checked groove bevelShadow must stay the neutral value (D-02) for $colors")
            assertEquals(neutral.rimColor, resolved.rimColor, "checked groove rimColor must stay the neutral value (D-02) for $colors")
            assertEquals(neutral.cornerRadius, resolved.cornerRadius, "checked groove cornerRadius must stay the neutral value (D-02) for $colors")
        }
    }

    @Test
    fun grooveMidpointLerpsEachFillStopExactly() {
        schemes.forEach { colors ->
            val neutral = expectedNeutralGroove(colors)
            val accent = expectedAccentGroove(colors)
            val resolved = resolveSwitchGrooveStyle(colors, checkedProgress = 0.5f, enabled = true)

            assertEquals(
                lerp(neutral.fillTop, accent.fillTop, 0.5f),
                resolved.fillTop,
                "midpoint (progress=0.5f) groove fillTop must be the exact lerp for $colors",
            )
            assertEquals(
                lerp(neutral.fillBottom, accent.fillBottom, 0.5f),
                resolved.fillBottom,
                "midpoint (progress=0.5f) groove fillBottom must be the exact lerp for $colors",
            )
        }
    }

    @Test
    fun grooveDisabledEqualsEnabledResultFlattenedAtBothProgressEndpoints() {
        schemes.forEach { colors ->
            listOf(0f, 1f).forEach { progress ->
                val enabledResolved = resolveSwitchGrooveStyle(colors, checkedProgress = progress, enabled = true)
                val disabledResolved = resolveSwitchGrooveStyle(colors, checkedProgress = progress, enabled = false)
                assertEquals(
                    enabledResolved.flattenDisabled(colors),
                    disabledResolved,
                    "disabled groove at progress=$progress must equal the enabled result flattened for $colors",
                )
            }
        }
    }

    // --- Thumb ---

    @Test
    fun thumbRestEnabledEqualsNeutralRestWithNonNullDropShadow() {
        schemes.forEach { colors ->
            val rest = expectedThumbRest(colors)
            val resolved = resolveSwitchThumbStyle(colors, hovered = false, pressed = false, enabled = true)

            assertEquals(rest.fillTop, resolved.fillTop, "thumb rest fillTop must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.fillBottom, resolved.fillBottom, "thumb rest fillBottom must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.glossColor, resolved.glossColor, "thumb rest glossColor must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.glossAlpha, resolved.glossAlpha, "thumb rest glossAlpha must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.bevelLight, resolved.bevelLight, "thumb rest bevelLight must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.bevelShadow, resolved.bevelShadow, "thumb rest bevelShadow must equal neutralRest(colors, 7.dp) for $colors")
            assertEquals(rest.rimColor, resolved.rimColor, "thumb rest rimColor must equal neutralRest(colors, 7.dp) for $colors")
            assertNotNull(resolved.dropShadow, "thumb rest must carry a non-null dropShadow for $colors")
        }
    }

    /**
     * D-05 divergence: pressed must NOT swap fillTop/fillBottom like a recess would — the fill
     * stays unchanged from rest, and only glossAlpha brightens.
     */
    @Test
    fun thumbPressedKeepsUnchangedFillWithStrictlyBrighterGloss() {
        schemes.forEach { colors ->
            val restResolved = resolveSwitchThumbStyle(colors, hovered = false, pressed = false, enabled = true)
            val pressedResolved = resolveSwitchThumbStyle(colors, hovered = false, pressed = true, enabled = true)

            assertEquals(restResolved.fillTop, pressedResolved.fillTop, "pressed thumb fillTop must be unchanged, NOT swapped (D-05) for $colors")
            assertEquals(restResolved.fillBottom, pressedResolved.fillBottom, "pressed thumb fillBottom must be unchanged, NOT swapped (D-05) for $colors")
            assertTrue(
                pressedResolved.glossAlpha > restResolved.glossAlpha,
                "pressed thumb glossAlpha must be strictly greater than rest's glossAlpha for $colors",
            )
        }
    }

    @Test
    fun thumbHoveredEqualsRestWithHoverLightenApplied() {
        schemes.forEach { colors ->
            val restResolved = resolveSwitchThumbStyle(colors, hovered = false, pressed = false, enabled = true)
            val hoveredResolved = resolveSwitchThumbStyle(colors, hovered = true, pressed = false, enabled = true)

            assertEquals(
                restResolved.hoverLighten(),
                hoveredResolved,
                "hovered (not pressed) thumb must equal the rest style with hoverLighten() applied for $colors",
            )
        }
    }

    @Test
    fun thumbDisabledWinsRegardlessOfHoveredOrPressedAndCarriesNoDropShadow() {
        schemes.forEach { colors ->
            val expected = expectedThumbRest(colors).flattenDisabled(colors)

            val disabledOnly = resolveSwitchThumbStyle(colors, hovered = false, pressed = false, enabled = false)
            val disabledHovered = resolveSwitchThumbStyle(colors, hovered = true, pressed = false, enabled = false)
            val disabledPressed = resolveSwitchThumbStyle(colors, hovered = false, pressed = true, enabled = false)
            val disabledBoth = resolveSwitchThumbStyle(colors, hovered = true, pressed = true, enabled = false)

            assertEquals(expected, disabledOnly, "disabled thumb must equal neutralRest(colors, 7.dp).flattenDisabled(colors) for $colors")
            assertEquals(disabledOnly, disabledHovered, "disabled must win over hovered for $colors")
            assertEquals(disabledOnly, disabledPressed, "disabled must win over pressed for $colors")
            assertEquals(disabledOnly, disabledBoth, "disabled must win over hovered+pressed combined for $colors")
            assertNull(disabledOnly.dropShadow, "disabled thumb must not carry a dropShadow — a dead thumb must not float for $colors")
        }
    }
}
