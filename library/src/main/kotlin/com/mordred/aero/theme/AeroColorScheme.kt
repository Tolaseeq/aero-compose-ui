package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Aero color tokens. Three built-in presets are exposed as companion-object properties:
 * [AeroBlue], [AeroDark], [Classic]. Custom themes are produced via [copy].
 *
 * All composables under `AeroTheme {}` read these tokens from `LocalAeroColors`.
 */
@Immutable
public data class AeroColorScheme(
    public val primary: Color,
    public val onPrimary: Color,
    public val secondary: Color,
    public val onSecondary: Color,
    public val surface: Color,
    public val onSurface: Color,
    public val background: Color,
    public val onBackground: Color,
    public val error: Color,
    public val onError: Color,
    public val cardBackground: Color,
    public val borderDefault: Color,
    public val borderSelected: Color,
    public val labelText: Color,
    public val glassSurface: Color,
    public val glassBorder: Color,
    public val glassHighlight: Color,
    public val titleBarGradientStart: Color,
    public val titleBarGradientEnd: Color,
    public val titleBarText: Color,
    public val buttonHover: Color,
    public val closeButtonHover: Color,
    public val panelBackground: Color,
    /**
     * Escape hatch (PRIM-03): when non-null, [AeroTheme.ornaments] uses this instead of
     * algorithmically deriving ornament tokens via [AeroOrnamentTokens.derive]. Trailing and
     * defaulted so every pre-existing positional/named constructor call keeps compiling
     * unchanged — source-compatible by construction.
     */
    public val ornamentOverride: AeroOrnamentTokens? = null,
    /**
     * On-fill label colour for OPAQUE surfaces — filled button (rest/hover/press/disabled) and
     * segmented-control segments (raised AND recessed) — SHW-16/VER-06 (20-09). Resolved once as a
     * property of the scheme, never recomputed per call site from that call site's currently
     * animating fill: the maintainer's 20-08 checkpoint rule is "per theme, ONE text colour, not
     * per element, not per state." Both [com.mordred.aero.components.buttons.AeroButtonSurface]'s
     * filled `Text` and `AeroSegmentedControl`'s segment `Text` (every polarity-"opaque" case) read
     * this token directly; neither computes a colour from its own fill any more.
     *
     * Two polarities exist, not one, because AeroBlue/AeroDark's `primary` is a LIGHT blue — opaque
     * fills (which darken `primary`) stay light enough to want a DARK label, while the ~15%-alpha
     * outlined fill composites down onto the dark backdrop and wants a LIGHT label instead. Classic's
     * `primary` is dark, so both polarities happen to want white, and Classic's historically-correct
     * "white everywhere" behaviour is exactly what this two-token split reproduces rather than
     * changes (see [labelOnOutlinedSurface] for the other polarity).
     *
     * Trailing and defaulted exactly like [ornamentOverride], for the same reason: every
     * pre-existing positional/named constructor call keeps compiling unchanged. The default is
     * derived from [primary] (a rough proxy for "how light this scheme's opaque fills read") rather
     * than hard-coded, so a custom scheme built via [copy] that specifies neither label token still
     * resolves a sensible label colour instead of throwing or rendering invisible text — it will not
     * necessarily match a hand-tuned built-in value, but it will not be illegible either.
     */
    public val labelOnFilledSurface: Color = defaultLabelColorForSurface(primary),
    /**
     * On-fill label colour for OUTLINED surfaces (the outlined button variant) — see
     * [labelOnFilledSurface]'s KDoc for why this is a second, independent token rather than one
     * shared value. The outlined fill is ~15% alpha of `primary` composited over
     * [AeroColorScheme.background], so the effective surface is dominated by the (dark, on every
     * shipped scheme) backdrop rather than by `primary` itself — the default below is therefore
     * derived from [background], not [primary].
     *
     * Trailing, defaulted, and fallback-derived for the same source-compatibility reason as
     * [labelOnFilledSurface].
     */
    public val labelOnOutlinedSurface: Color = defaultLabelColorForSurface(background),
) {
    public companion object {
        public val AeroBlue: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003B5C),
            secondary = Color(0xFF81D4FA),
            onSecondary = Color(0xFF003B5C),
            surface = Color(0xCC1A3A5C),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF0D1B2A),
            onBackground = Color(0xFFE0E0E0),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0x40FFFFFF),
            borderDefault = Color(0x60FFFFFF),
            borderSelected = Color(0xFF4FC3F7),
            labelText = Color(0xFFBDBDBD),
            glassSurface = Color(0x30FFFFFF),
            glassBorder = Color(0x50FFFFFF),
            glassHighlight = Color(0x20FFFFFF),
            titleBarGradientStart = Color(0xDD1A3A6C),
            titleBarGradientEnd = Color(0xDD0D1F3C),
            titleBarText = Color(0xFFE0E8F0),
            buttonHover = Color(0x40FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xCC152A42),
            // SHW-16/VER-06 (20-09) polarity table, measured by VER10OneLabelColorPerThemeTest /
            // AeroButtonContrastRegressionTest: opaque fills (light primary, darkened) want a dark
            // label; the outlined ~15%-alpha fill composites down onto the dark backdrop and wants
            // a light label instead.
            labelOnFilledSurface = Color.Black,
            labelOnOutlinedSurface = Color.White,
        )

        public val AeroDark: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color(0xFF0D1B2A),
            secondary = Color(0xFF64B5F6),
            onSecondary = Color(0xFF0D1B2A),
            surface = Color(0xCC1A1A2E),
            onSurface = Color(0xFFCCCCCC),
            background = Color(0xFF0A0A1A),
            onBackground = Color(0xFFCCCCCC),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0x30000000),
            borderDefault = Color(0x40FFFFFF),
            borderSelected = Color(0xFF90CAF9),
            labelText = Color(0xFFAAAAAA),
            glassSurface = Color(0x20FFFFFF),
            glassBorder = Color(0x30FFFFFF),
            glassHighlight = Color(0x15FFFFFF),
            titleBarGradientStart = Color(0xDD1A1A3E),
            titleBarGradientEnd = Color(0xDD0A0A1E),
            titleBarText = Color(0xFFD0D0E0),
            buttonHover = Color(0x30FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xCC12122A),
            // Same polarity rationale as AeroBlue above (SHW-16/VER-06, 20-09).
            labelOnFilledSurface = Color.Black,
            labelOnOutlinedSurface = Color.White,
        )

        public val Classic: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF5C8ABF),
            onPrimary = Color.White,
            secondary = Color(0xFF7BA5D1),
            onSecondary = Color.White,
            surface = Color(0xFF2D2D2D),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0xFF424242),
            borderDefault = Color(0xFF555555),
            borderSelected = Color(0xFF5C8ABF),
            labelText = Color.LightGray,
            glassSurface = Color(0xFF333333),
            glassBorder = Color(0xFF555555),
            glassHighlight = Color(0xFF3A3A3A),
            titleBarGradientStart = Color(0xFF3A3A3A),
            titleBarGradientEnd = Color(0xFF2A2A2A),
            titleBarText = Color(0xFFE0E0E0),
            buttonHover = Color(0x30FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xFF2D2D2D),
            // Classic's primary is dark, so both polarities want white — the historically-correct
            // "white label everywhere" behaviour (SHW-16/VER-06, 20-09) is unchanged, now expressed
            // as two tokens that happen to coincide rather than left implicit.
            labelOnFilledSurface = Color.White,
            labelOnOutlinedSurface = Color.White,
        )
    }
}

/**
 * Fallback heuristic for [AeroColorScheme.labelOnFilledSurface]/[AeroColorScheme.labelOnOutlinedSurface]'s
 * default values — exercised only when a custom [AeroColorScheme.copy] omits both tokens, since
 * every shipped scheme sets them explicitly from
 * [com.mordred.aero.components.buttons.AeroButtonContrastRegressionTest]/
 * [com.mordred.aero.verification.VER10OneLabelColorPerThemeTest]'s measured tables. A plain
 * luminance split on the passed-in [surface] proxy (never a hard-coded literal, never a throw) —
 * a light proxy colour picks [Color.Black], a dark one picks [Color.White]. This is deliberately
 * simpler than the retired per-fill `resolveLabelColor` worst-case-across-two-stops algorithm: it
 * only has to be a SENSIBLE fallback for an unknown custom scheme, not a bit-for-bit reproduction
 * of a hand-tuned built-in value.
 */
private fun defaultLabelColorForSurface(surface: Color): Color =
    if (surface.luminance() > 0.5f) Color.Black else Color.White
