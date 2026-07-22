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
 * Magnitudes below are calibration starting points (D-01's moderate ~30-35% gloss target),
 * re-reviewed at the Phase 16 three-theme sign-off — not a locked spec (16-RESEARCH.md
 * Assumption A2).
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
            glossHighlight = base.glassHighlight.lighten(0.15f),
            bevelLight = base.primary.lighten(0.25f),
            bevelShadow = base.primary.darken(0.20f),
            rimLight = base.glassBorder.lighten(0.10f),
            // Lightened past bevelLight (0.25f)/fillSplitTop (0.18f) so the glow reads as a
            // distinct luminous halo rather than blending into the surface's own top-of-fill
            // brightness (PRIM-06 — must contrast the surface it wraps, not match its hue
            // family) — but not so far past white that it reads as a harsh, near-white rim
            // (D-01 moderate-Aero target: soft focus glow, not a hard selection ring).
            hoverGlow = base.primary.lighten(0.45f),
            grooveShadow = base.surface.darken(0.15f),
            fillSplitTop = base.primary.lighten(0.18f),
            fillSplitBottom = base.primary.darken(0.12f),
        )
    }
}
