package com.mordred.aero.components.list

import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Value-level JVM tests (no `runComposeUiTest`, no Compose runtime) for
 * [resolveListItemPillStyle] — the pure, nullable per-state resolution point wired into
 * [AeroListItem]'s pill/focus rendering in 19-01. Exercised over both
 * [AeroColorScheme.AeroBlue] (translucent tokens) and [AeroColorScheme.Classic] (fully-opaque
 * tokens) per 19-VALIDATION.md's Validation Architecture.
 *
 * Corner radius (6.dp) and the selected-fill alpha scale (0.5f) below are independently
 * hardcoded, matching [AeroListItem.kt]'s own private `PILL_CORNER_RADIUS`/
 * `SELECTED_FILL_ALPHA_SCALE` constants — mirrors `AeroSliderStylesTest`'s convention of
 * reconstructing the expected style from scratch rather than importing the value under test,
 * so a drift in either constant is caught. Expected styles are rebuilt exclusively from
 * [AeroSurfaceStyle.rest]/[AeroSurfaceStyle.neutralRest] — [resolveListItemPillStyle] is never
 * called to build an expectation.
 */
class AeroListItemStylesTest {

    private val schemes = listOf(AeroColorScheme.AeroBlue, AeroColorScheme.Classic)

    private fun expectedSelectedPill(colors: AeroColorScheme): AeroSurfaceStyle {
        val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 6.dp)
        return rest.copy(
            fillTop = rest.fillTop.copy(alpha = rest.fillTop.alpha * 0.5f),
            fillBottom = rest.fillBottom.copy(alpha = rest.fillBottom.alpha * 0.5f),
        )
    }

    private fun expectedHoverPill(colors: AeroColorScheme): AeroSurfaceStyle =
        AeroSurfaceStyle.neutralRest(colors, cornerRadius = 6.dp).hoverLighten()

    @Test
    fun restUnselectedUnhoveredReturnsNull() {
        schemes.forEach { colors ->
            val resolved = resolveListItemPillStyle(
                colors = colors,
                selected = false,
                hovered = false,
                enabled = true,
            )
            assertNull(resolved, "rest + unselected + unhovered must draw no pill (returns null) for $colors")
        }
    }

    @Test
    fun restSelectedEqualsHalfAlphaAccentRest() {
        schemes.forEach { colors ->
            val resolved = resolveListItemPillStyle(
                colors = colors,
                selected = true,
                hovered = false,
                enabled = true,
            )
            assertEquals(
                expectedSelectedPill(colors),
                resolved,
                "rest + selected must equal AeroSurfaceStyle.rest(colors, 6.dp) with both fill " +
                    "alphas scaled by exactly 0.5f for $colors",
            )
        }
    }

    @Test
    fun hoverUnselectedEqualsNeutralRestHoverLighten() {
        schemes.forEach { colors ->
            val resolved = resolveListItemPillStyle(
                colors = colors,
                selected = false,
                hovered = true,
                enabled = true,
            )
            assertEquals(
                expectedHoverPill(colors),
                resolved,
                "hover + unselected must equal neutralRest(colors, 6.dp).hoverLighten() for $colors",
            )
        }
    }

    /**
     * VLST-02, the load-bearing case: hovering an already-selected row must compose
     * (selected base transformed by hoverLighten()) rather than being suppressed by, or
     * suppressing, the selection styling. Three assertions in one method so a failure message
     * tells the whole story.
     */
    @Test
    fun hoverSelectedComposesOnTopOfSelectedBase() {
        schemes.forEach { colors ->
            val resolved = resolveListItemPillStyle(
                colors = colors,
                selected = true,
                hovered = true,
                enabled = true,
            )
            assertEquals(
                expectedSelectedPill(colors).hoverLighten(),
                resolved,
                "hover + selected must equal the selected style with hoverLighten() composed on top for $colors",
            )
            assertNotEquals(
                expectedSelectedPill(colors),
                resolved,
                "hover + selected must NOT equal the plain resting-selected style (must be strictly brighter) for $colors",
            )
            assertNotEquals(
                expectedHoverPill(colors),
                resolved,
                "hover + selected must NOT equal the unselected-hover style (selection must not be discarded) for $colors",
            )
        }
    }

    @Test
    fun disabledWinsRegardlessOfSelectedOrHovered() {
        schemes.forEach { colors ->
            val disabledUnselectedUnhovered = resolveListItemPillStyle(
                colors = colors, selected = false, hovered = false, enabled = false,
            )
            val disabledSelected = resolveListItemPillStyle(
                colors = colors, selected = true, hovered = false, enabled = false,
            )
            val disabledHovered = resolveListItemPillStyle(
                colors = colors, selected = false, hovered = true, enabled = false,
            )
            val disabledSelectedHovered = resolveListItemPillStyle(
                colors = colors, selected = true, hovered = true, enabled = false,
            )

            assertNull(
                disabledUnselectedUnhovered,
                "disabled + unselected + unhovered must still return null for $colors",
            )

            val expectedDisabledSelected = expectedSelectedPill(colors).flattenDisabled(colors)
            assertEquals(
                expectedDisabledSelected,
                disabledSelected,
                "disabled + selected must equal the selected style flattened, with no hover applied for $colors",
            )

            assertNull(
                disabledHovered,
                "disabled + unselected + hovered must return null — hover is not considered when disabled for $colors",
            )

            assertEquals(
                expectedDisabledSelected,
                disabledSelectedHovered,
                "disabled + selected + hovered must equal the SAME flattened selected style as " +
                    "disabled + selected alone — hover must not compose when disabled for $colors",
            )
            assertNotNull(disabledSelectedHovered, "sanity: disabled + selected + hovered must not be null for $colors")
        }
    }
}
