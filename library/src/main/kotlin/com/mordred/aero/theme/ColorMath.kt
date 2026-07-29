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

/**
 * Darken amount applied to [AeroColorScheme.primary] to derive the filled button's rest
 * `fillTop` (17-05 ROUND-2 sign-off gap-fix, FIX B) / the raised segment's rest `fillTop`
 * (gap G5, 19-UAT.md) — both consumers import this one declaration, never a copy.
 *
 * **Relocated here from `components/buttons/AeroButtonSurface.kt` as part of the WR-04 fix
 * (20-REVIEW.md addendum 2, closed 2026-07-29).** [AeroColorScheme.labelOnFilledSurface]'s
 * fallback (`defaultLabelColorForOpaqueFill` below) needs this exact value to evaluate the
 * label against the SAME fill `resolveButtonStyle`/`resolveSegmentStyle` actually paint — the
 * `theme` package cannot import from `components.buttons` (that package already imports
 * `theme`, so a reverse import would be a real cycle) and the review's own instruction was not
 * to duplicate the literal, since duplicated constants are how these two values drift apart.
 * Moving the single declaration here — with `components/buttons`/`components/selection` now
 * importing it FROM `theme` instead of the other way around — keeps one source of truth with no
 * cycle. The value itself (`0.20f`) is unchanged; every consumer's rendered output is byte-for-byte
 * identical to before this move.
 */
internal const val FILLED_FILL_TOP_DARKEN: Float = 0.20f

/**
 * Darken amount applied to [AeroColorScheme.primary] to derive the filled button's rest
 * `fillBottom`. Relocated alongside [FILLED_FILL_TOP_DARKEN] — see its KDoc for why.
 */
internal const val FILLED_FILL_BOTTOM_DARKEN: Float = 0.36f
