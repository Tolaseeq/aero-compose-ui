package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Declarative description of an Aero "glass" surface, consumed by [drawAeroSurfaceCore] /
 * [Modifier.aeroSurface]. Field shape is Claude's discretion (16-RESEARCH.md Assumption A4),
 * calibrated to D-01's moderate "spirit of Aero" target: a soft two-tone fill seam, a gloss
 * band occupying ~30-35% of the shape's height, and a subtle inner bevel/rim — never a hard
 * mid-surface break, never Feather-style flat outline or generic Material3-flat.
 *
 * This is the production default all Phase 17-19 components inherit — not a "v1"/placeholder
 * (D-01 costly-reversibility). Every gradient built from these fields fades toward the field's
 * own `.copy(alpha = 0f)`, never a hardcoded [Color.Transparent] (PRIM-14) — see
 * [drawAeroSurfaceCore].
 */
@Immutable
public data class AeroSurfaceStyle(
    public val fillTop: Color,
    public val fillBottom: Color,
    public val glossColor: Color,
    public val bevelLight: Color,
    public val bevelShadow: Color,
    public val rimColor: Color,
    public val seamFraction: Float = 0.5f,
    public val glossAlpha: Float = 0.32f,
    public val glossHeightFraction: Float = 0.32f,
    public val rimAlpha: Float = 0.6f,
    public val cornerRadius: Dp = 8.dp,
    public val dropShadow: Shadow? = null,
    public val innerShadow: Shadow? = null,
) {
    public companion object {
        /**
         * Rest-state preset built from [AeroOrnamentTokens] (honoring [AeroColorScheme.ornamentOverride]
         * when set) — the production default consumed by the Primitives gallery and, from
         * Phase 17 onward, every restyled component's own-surface paint.
         */
        public fun rest(base: AeroColorScheme, cornerRadius: Dp = 8.dp): AeroSurfaceStyle {
            val ornaments = base.ornamentOverride ?: AeroOrnamentTokens.derive(base)
            return AeroSurfaceStyle(
                fillTop = ornaments.fillSplitTop,
                fillBottom = ornaments.fillSplitBottom,
                glossColor = ornaments.glossHighlight,
                bevelLight = ornaments.bevelLight,
                bevelShadow = ornaments.bevelShadow,
                rimColor = ornaments.rimLight,
                cornerRadius = cornerRadius,
            )
        }
    }
}
