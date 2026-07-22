package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Algorithmically-derived ornament tokens (gloss/bevel/rim/glow/groove/fill-split) consumed by
 * [AeroSurfaceStyle] presets. Never hand-author these per theme — [derive] runs [Color.lighten]/
 * [Color.darken] (RGB-mix) against the active [AeroColorScheme]'s existing tokens so the result
 * is theme-sensitive by construction, including on [AeroColorScheme.Classic]'s fully-opaque
 * tokens where an alpha-based derivation would be a silent no-op (PRIM-02).
 *
 * RED-phase stub: [derive] intentionally returns identical `Color.Black` tokens regardless of
 * [base] so AeroOrnamentTokensTest's "theme-sensitive" assertions fail before the real
 * lighten/darken-based derivation lands.
 */
@Immutable
public data class AeroOrnamentTokens(
    public val glossHighlight: Color,
    public val bevelLight: Color,
    public val bevelShadow: Color,
    public val rimLight: Color,
    public val hoverGlow: Color,
    public val grooveShadow: Color,
    public val fillSplitTop: Color,
    public val fillSplitBottom: Color,
) {
    public companion object {
        public fun derive(base: AeroColorScheme): AeroOrnamentTokens = AeroOrnamentTokens(
            glossHighlight = Color.Black,
            bevelLight = Color.Black,
            bevelShadow = Color.Black,
            rimLight = Color.Black,
            hoverGlow = Color.Black,
            grooveShadow = Color.Black,
            fillSplitTop = Color.Black,
            fillSplitBottom = Color.Black,
        )
    }
}
