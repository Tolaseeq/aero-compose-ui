package com.mordred.aero.theme

import androidx.compose.ui.graphics.Color

/**
 * RGB-mix color helpers used by [AeroOrnamentTokens.derive] to compute per-theme ornament
 * tokens. These MUST mix RGB channels toward white/black — never alpha-only — because
 * [AeroColorScheme.Classic]'s tokens are fully opaque (e.g. `glassSurface = Color(0xFF333333)`)
 * while [AeroColorScheme.AeroBlue]/[AeroColorScheme.AeroDark]'s equivalent tokens are
 * translucent; an alpha-based lighten/darken would be a silent no-op on Classic (PRIM-01).
 *
 * RED-phase stub: intentionally returns the input color unchanged so ColorMathTest fails
 * for the right reason before the real RGB-mix implementation lands.
 */
internal fun Color.lighten(amount: Float): Color = this

internal fun Color.darken(amount: Float): Color = this
