package com.mordred.aero.components.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroOrnamentTokens
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGlowRing
import com.mordred.aero.theme.aeroSurface

/**
 * Shared internal surface painting every Aero button variant (VBTN-06) — [AeroButton] (filled)
 * today, [AeroOutlinedButton] (17-03) and, cross-phase, `AeroSegmentedControl`'s recessed
 * selected segment (Phase 19, via [com.mordred.aero.theme.AeroSurfaceStyle.pressedRecess])
 * reuse this one painter so the fill/gloss/bevel/rim geometry cannot drift between variants
 * (16-RESEARCH.md "one core + additive param" precedent, `AeroPanelGroupImpl(orientation)`).
 *
 * Modifier chain ordering is load-bearing: both [aeroGlowRing] calls (focus, then hover) MUST
 * precede the single [aeroSurface] call — an earlier `.clip()` (which [aeroSurface] applies
 * internally) clips the drawing of everything after it in the chain, so `aeroSurface` before
 * `aeroGlowRing` erases the outer bloom entirely (PRIM-06 ordering rule, Phase 16 sign-off
 * regression). Every corner value in this chain is the locked `4.dp` — never either primitive's
 * own `8.dp` default (Pitfall 1).
 *
 * `Modifier.clickable(role = Role.Button, ...)` — not a zero-semantics hand-roll (the
 * `AeroRangeSlider` precedent must not repeat, VBTN-04) — carries click + Space/Enter keyboard
 * activation now that the Material3 `Button` container (which supplied this for free) is gone.
 *
 * Tracer-plan (17-01) style resolution: this single call to [AeroSurfaceStyle.rest] is the one
 * clearly-named point 17-02 replaces with full per-state (rest/hover/press/disabled) resolution
 * via `resolveButtonStyle(...)` — deliberately not stubbed with multiple fake states here.
 *
 * @param outlined Differentiates the outlined variant (17-03) from this filled default; unused
 * by the tracer's single rest-state resolution, consumed once `resolveButtonStyle`/
 * `outlinedStyle()` land in 17-02/17-03.
 */
@Composable
internal fun AeroButtonSurface(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp,
    contentPadding: PaddingValues,
    outlined: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val state = rememberAeroInteractionState(interactionSource)

    val style = AeroSurfaceStyle.rest(AeroTheme.colors, cornerRadius = 4.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .aeroGlowRing(
                active = state.focused && enabled,
                glowColor = AeroTheme.colors.borderSelected,
                cornerRadius = 4.dp,
            )
            .aeroGlowRing(
                active = state.hovered && enabled,
                glowColor = AeroOrnamentTokens.derive(AeroTheme.colors).hoverGlow,
                cornerRadius = 4.dp,
            )
            .aeroSurface(style, RoundedCornerShape(4.dp))
    ) {
        Text(
            text = text,
            style = AeroTheme.typography.bodyLarge,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(contentPadding),
        )
    }
}
