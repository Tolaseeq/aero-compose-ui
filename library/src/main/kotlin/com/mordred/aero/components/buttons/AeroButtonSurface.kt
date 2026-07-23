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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mordred.aero.components.common.rememberAeroInteractionState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroOrnamentTokens
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroGlowRing
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.flattenDisabled
import com.mordred.aero.theme.hoverLighten
import com.mordred.aero.theme.pressedRecess

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
 * Style resolution now goes through [resolveButtonStyle] (17-02), replacing the 17-01 tracer's
 * single rest-only call — every recomposition resolves the current rest/hover/press/disabled
 * style from [rememberAeroInteractionState]'s booleans; focus carries no fill delta (it is
 * expressed purely by the persistent [aeroGlowRing] focus call above).
 *
 * @param outlined Differentiates the outlined variant (17-03) from this filled default; threaded
 * through [resolveButtonStyle] but currently resolves identically to filled — the outlined delta
 * (fill alpha × 0.15, gloss 0.32 → 0.15, rim 0.6 → 0.85) is applied in 17-03.
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

    val style = resolveButtonStyle(
        colors = AeroTheme.colors,
        outlined = outlined,
        hovered = state.hovered,
        pressed = state.pressed,
        focused = state.focused,
        enabled = enabled,
    )

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

/**
 * Inner-shadow applied to a pressed/recessed button style (17-UI-SPEC.md Press row) — a native
 * [Shadow] (Phase 16 D-02 precedent), not a manual gradient.
 */
private val PRESSED_INNER_SHADOW: Shadow = Shadow(
    radius = 2.dp,
    color = Color.Black.copy(alpha = 0.35f),
    offset = DpOffset(0.dp, 1.dp),
)

/**
 * Pure, Compose-free resolution point mapping (rest/hover/press/focus/disabled) → [AeroSurfaceStyle]
 * (17-UI-SPEC.md "Filled AeroButton — per-state contract"), consumed by [AeroButtonSurface].
 *
 * Precedence (disabled wins over everything, press over hover): [enabled] `false` →
 * [AeroSurfaceStyle.flattenDisabled]; else [pressed] → [AeroSurfaceStyle.pressedRecess]; else
 * [hovered] → [AeroSurfaceStyle.hoverLighten]; else the unmodified rest style. [focused] carries
 * no fill delta here — focus is expressed purely by [AeroButtonSurface]'s persistent
 * `aeroGlowRing` call, per D-04 (kept as a resolver parameter for API symmetry/future use, not
 * because it currently branches anything).
 *
 * [outlined] is threaded through but not yet consumed — it resolves identically to the filled
 * style until 17-03 applies the outlined fixed-delta transform on top.
 */
internal fun resolveButtonStyle(
    colors: AeroColorScheme,
    outlined: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)
    return when {
        !enabled -> rest.flattenDisabled(colors)
        pressed -> rest.pressedRecess(PRESSED_INNER_SHADOW)
        hovered -> rest.hoverLighten()
        else -> rest
    }
}
