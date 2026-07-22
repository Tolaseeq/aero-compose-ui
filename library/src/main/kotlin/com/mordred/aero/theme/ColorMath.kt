package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color

/**
 * RGB-mix color helpers used by [AeroOrnamentTokens.derive] to compute per-theme ornament
 * tokens. These MUST mix RGB channels toward white/black — never alpha-only — because
 * [AeroColorScheme.Classic]'s tokens are fully opaque (e.g. `glassSurface = Color(0xFF333333)`)
 * while [AeroColorScheme.AeroBlue]/[AeroColorScheme.AeroDark]'s equivalent tokens are
 * translucent; an alpha-based lighten/darken would be a silent no-op on Classic (PRIM-01).
 *
 * Both mix toward the target endpoint proportionally to [amount] (0f = unchanged, 1f = full
 * White/Black), preserving the input's alpha channel exactly.
 */
internal fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

internal fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha,
)
