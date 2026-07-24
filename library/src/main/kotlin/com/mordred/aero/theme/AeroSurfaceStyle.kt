package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    // Sign-off gap-fix (17-05): lowered 0.32f -> 0.22f, trimming the gloss band amplifying
    // filled-fill brightness. AeroButtonSurface.FILLED_REST_GLOSS_ALPHA must track this value.
    public val glossAlpha: Float = 0.22f,
    public val glossHeightFraction: Float = 0.32f,
    // Sign-off gap-fix (17-05): lowered 0.6f -> 0.45f, softening the rim so it no longer reads
    // brighter than the rest of the library (paired with AeroOrnamentTokens.rimLight dropping
    // its 0.10f whitening).
    public val rimAlpha: Float = 0.45f,
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

        /**
         * Neutral-glass rest-state preset (18-UI-SPEC.md Color § "Finding to confirm during
         * planning") — a parallel to [rest] that sources its fill/bevel from NEUTRAL tokens
         * ([AeroColorScheme.glassHighlight]/[AeroColorScheme.glassSurface]/
         * [AeroColorScheme.glassBorder]) rather than [AeroColorScheme.primary]. [rest] cannot be
         * reused unmodified here — [AeroOrnamentTokens.derive] derives BOTH `fillSplitTop/Bottom`
         * AND `bevelLight/bevelShadow` from `base.primary`, so passing [rest] to
         * [aeroThumbSurface]/[aeroGroove] would render an accent-tinted thumb/groove, contradicting
         * D-01's "neutral movable thumb, recessed neutral groove" Win7 slider identity.
         *
         * Placed here (not `components/range/`) so Phase 19's `AeroSwitch` can reuse it
         * cross-package for its own neutral thumb without a reach-around, mirroring [pressedRecess]'s
         * placement rationale. [glossColor]/[rimColor] reuse [AeroOrnamentTokens.glossHighlight]/
         * [AeroOrnamentTokens.rimLight] as-is — already neutral tokens, no re-derivation needed.
         * Uses [Color.lighten]/[Color.darken]'s RGB-mix (never `.copy(alpha = ...)`) so the result
         * stays correct on [AeroColorScheme.Classic]'s fully-opaque tokens (PRIM-01/14).
         *
         * Exact lighten/darken magnitudes are Claude's discretion (18-CONTEXT.md), spot-checked on
         * AeroBlue/AeroDark/Classic at first implementation (PRIM-16 carries forward) — the UI-SPEC's
         * suggested starting-point values below are used verbatim, not re-derived from memory.
         */
        public fun neutralRest(base: AeroColorScheme, cornerRadius: Dp = 8.dp): AeroSurfaceStyle {
            val ornaments = base.ornamentOverride ?: AeroOrnamentTokens.derive(base)
            return AeroSurfaceStyle(
                fillTop = base.glassHighlight.lighten(0.12f),
                fillBottom = base.glassSurface.darken(0.06f),
                glossColor = ornaments.glossHighlight,
                bevelLight = base.glassHighlight.lighten(0.20f),
                bevelShadow = base.glassBorder.darken(0.15f),
                rimColor = ornaments.rimLight,
                cornerRadius = cornerRadius,
            )
        }
    }
}

/** Hover fill-brighten factor applied by [AeroSurfaceStyle.hoverLighten] (17-UI-SPEC.md Hover row). */
private const val HOVER_LIGHTEN_AMOUNT: Float = 0.08f

/**
 * Fraction the disabled flat fill is blended toward [AeroColorScheme.surface] (17-UI-SPEC.md
 * Disabled row). Raised 0.4f -> 0.5f (17-05 ROUND-2 sign-off gap-fix, FIX C) alongside the blend
 * target's switch from [AeroColorScheme.borderDefault] to [AeroColorScheme.surface] — see
 * [flattenDisabled]'s KDoc for why blending toward the border token stood out as a light-gray
 * highlight rather than receding into the panel.
 */
private const val DISABLED_NEUTRAL_BLEND: Float = 0.5f

/** Darken amount applied to the disabled flat fill to derive its single collapsed bevel tone. */
private const val DISABLED_BEVEL_DARKEN: Float = 0.05f

/** Multiplier applied to [AeroSurfaceStyle.rimAlpha] when disabled (17-UI-SPEC.md Disabled row). */
private const val DISABLED_RIM_ALPHA_MULTIPLIER: Float = 0.5f

/**
 * Recessed/pressed transform (D-02) — the exact field-swap idiom [drawAeroGroove] already uses
 * (`AeroSurfacePrimitives.kt` lines 259-269), lifted into a pure extension fn here (not
 * `components/buttons/`) so Phase 19's `AeroSegmentedControl` can import it cross-package for its
 * recessed-selected-segment fill without a reach-around into the button package.
 *
 * Swaps [fillTop]/[fillBottom] and [bevelLight]/[bevelShadow], disables gloss (a recessed surface
 * doesn't catch a top highlight), and attaches [innerShadow] — [Modifier.aeroSurface] applies a
 * style's `innerShadow` automatically, so no extra draw call is needed at the call site.
 */
internal fun AeroSurfaceStyle.pressedRecess(innerShadow: Shadow): AeroSurfaceStyle = copy(
    fillTop = fillBottom,
    fillBottom = fillTop,
    bevelLight = bevelShadow,
    bevelShadow = bevelLight,
    glossAlpha = 0f,
    innerShadow = innerShadow,
)

/**
 * Disabled transform (D-05) — flattens the geometry itself rather than merely fading it: the
 * two-tone fill collapses to one tone (blended toward [base]'s [AeroColorScheme.surface], not just
 * the two fill stops averaged) so a disabled accent-colored button desaturates toward the theme's
 * own dark panel tone instead of reading as a dimmed version of its own hue; bevel collapses to a
 * single tone (no light/shadow split, reading as "dead"); gloss is fully disabled; the rim halves
 * in strength.
 *
 * Blends toward [AeroColorScheme.surface] rather than [AeroColorScheme.borderDefault] (17-05
 * ROUND-2 sign-off gap-fix, FIX C) — `borderDefault` is a translucent WHITE token on the Aero
 * themes, so blending toward it produced a light-gray fill that stood out as a highlight against
 * the dark background instead of receding, the opposite of "disabled." `surface` is each theme's
 * own dark panel tone, so the collapsed fill recedes into the surrounding chrome instead of
 * glowing. [DISABLED_NEUTRAL_BLEND] is raised alongside this switch (0.4f -> 0.5f) for a clearer
 * merge with the surface. Still a genuine "dead" look — gloss off, single bevel tone, rim halved —
 * just not a light-gray standout.
 *
 * Never uses `.copy(alpha = ...)` for the flatten — [Color.lighten]/[Color.darken]/[lerp] mix RGB
 * channels directly so the result stays correct on [AeroColorScheme.Classic]'s fully-opaque tokens
 * (PRIM-14/PRIM-01 no-silent-no-op rule).
 */
internal fun AeroSurfaceStyle.flattenDisabled(base: AeroColorScheme): AeroSurfaceStyle {
    val collapsedFill = lerp(fillTop, fillBottom, 0.5f)
    val neutralFill = lerp(collapsedFill, base.surface, DISABLED_NEUTRAL_BLEND)
    val flatBevel = neutralFill.darken(DISABLED_BEVEL_DARKEN)
    return copy(
        fillTop = neutralFill,
        fillBottom = neutralFill,
        glossAlpha = 0f,
        glossHeightFraction = 0f,
        bevelLight = flatBevel,
        bevelShadow = flatBevel,
        rimAlpha = rimAlpha * DISABLED_RIM_ALPHA_MULTIPLIER,
    )
}

/**
 * Hover transform — brightens both fill stops by [HOVER_LIGHTEN_AMOUNT] via [Color.lighten]'s
 * RGB-mix (never `.copy(alpha = ...)`, correct on Classic's opaque tokens) so the whole fill body
 * lights up (D-03), while gloss/bevel/rim structure is preserved unchanged — the hover glow ring
 * is a separate, independently-chained [aeroGlowRing] cue, not part of this fill transform.
 */
internal fun AeroSurfaceStyle.hoverLighten(): AeroSurfaceStyle = copy(
    fillTop = fillTop.lighten(HOVER_LIGHTEN_AMOUNT),
    fillBottom = fillBottom.lighten(HOVER_LIGHTEN_AMOUNT),
)
