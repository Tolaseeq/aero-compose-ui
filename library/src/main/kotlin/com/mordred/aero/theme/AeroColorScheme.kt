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
     * **This dark-on-opaque rationale is the design case for the split, not what is currently
     * shipped.** As of 2026-07-29 AeroBlue and AeroDark set BOTH tokens to `Color.White` (see their
     * companion-object definitions below) — the maintainer rejected the dark-label-on-opaque-fills
     * result on appearance after seeing it running, in favor of visual coherence with Classic. The
     * two-token STRUCTURE this KDoc describes stands; only the two dark schemes' chosen VALUE
     * changed. See each preset's own KDoc comment for the accepted contrast cost of that choice.
     *
     * Trailing and defaulted exactly like [ornamentOverride], for the same reason: every
     * pre-existing positional/named constructor call keeps compiling unchanged. The default is
     * derived from [primary] via [defaultLabelColorForOpaqueFill] rather than hard-coded, so a
     * custom scheme built via [copy] that specifies neither label token still resolves a sensible
     * label colour instead of throwing or rendering invisible text.
     *
     * **WR-04 fix (20-REVIEW.md addendum 2, closed 2026-07-29).** The default previously evaluated
     * a plain luminance split against raw, un-darkened [primary] — a reference colour no on-fill
     * label is ever actually painted against, since both real consumers
     * ([com.mordred.aero.components.buttons.resolveButtonStyle]'s rest fill,
     * [com.mordred.aero.components.selection.resolveSegmentStyle]'s raised base) always darken
     * `primary` by `FILLED_FILL_TOP_DARKEN`/`FILLED_FILL_BOTTOM_DARKEN` first. That mismatch let a
     * plausible custom `primary` (e.g. `#BABABA`) pick the wrong candidate and measure as low as
     * 3.00:1 against the surface actually painted, contradicting this KDoc's own former "will not
     * be illegible" promise. [defaultLabelColorForOpaqueFill] now evaluates both real darkened
     * stops and picks whichever pure candidate has the better WORST-CASE ratio across both — never
     * the average, mirroring how the retired per-call-site `resolveLabelColor` reasoned — so the
     * same `#BABABA` example now resolves Black at 4.692:1 on its worst stop, clearing the 4.5:1
     * floor instead of missing it.
     */
    public val labelOnFilledSurface: Color = defaultLabelColorForOpaqueFill(primary),
    /**
     * On-fill label colour for OUTLINED surfaces (the outlined button variant) — see
     * [labelOnFilledSurface]'s KDoc for why this is a second, independent token rather than one
     * shared value. The outlined fill is ~15% alpha of `primary` composited over
     * [AeroColorScheme.background], so the effective surface is dominated by the (dark, on every
     * shipped scheme) backdrop rather than by `primary` itself — the default below is therefore
     * derived from [background], not [primary].
     *
     * **WR-04 follow-up: this fallback was checked for the same class of error and found NOT to
     * have it, so it is deliberately left unchanged.** Unlike [labelOnFilledSurface]'s fallback
     * (which evaluated raw `primary`, a colour 100% different in composition from the actually
     * painted, wholly-opaque darkened fill), this fallback's [background] proxy IS the dominant
     * 85% of the actually-painted composite (`primary` at ~15% alpha over `background`) — the
     * un-composited reference and the real one differ by construction only in that remaining ~15%
     * weight, not by an unrelated transform. A polarity flip is possible only if [background]'s own
     * luminance sits almost exactly at the 0.5 split AND [primary] is near a luminance extreme —
     * for every shipped scheme, [background] is a deliberately dark, near-black desktop-app
     * backdrop (luminance well under 0.1), far from that boundary, and a hypothetical custom scheme
     * choosing a near-0.5-luminance app background is a materially less ordinary choice than
     * [labelOnFilledSurface]'s demonstrated failure mode (an ordinary mid-tone brand `primary`).
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
            // Maintainer decision (2026-07-29): white on BOTH polarities, matching Classic. This
            // supersedes the surface-polarity rule this plan (20-09) originally shipped here — dark
            // label on opaque fills, light on outlined — which was measurement-driven and cleared
            // the 4.5:1 floor on the opaque (filled/segment) polarity. The maintainer reviewed that
            // rule running live, across all three themes, and rejected it on appearance: with a
            // dark label on filled surfaces, AeroBlue/AeroDark read as a different visual language
            // from Classic and from the rest of the library, which is light-on-dark throughout. They
            // were shown this exact cost (below) in writing twice, including a live run of the white
            // variant across all three themes, before approving it ("одобряю").
            //
            // Cost accepted: on white, AeroBlue's opaque fills (filled button rest/hover/press,
            // segment-raised, segment-recessed-hover) measure below the WCAG 4.5:1 normal-text floor
            // in most rest/hover/press cases — worst case 2.801:1 (filled-hover fillTop). See
            // AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation, which
            // pins and regression-bounds every one of these cases so none can silently get worse.
            // MIN_LABEL_CONTRAST itself is never lowered to accommodate this (D-13) — it is a
            // scoped, named, regression-bounded exception, not a change to the floor. What the white
            // choice also BUYS: it fully clears AeroBlue's recessed (selected) segment at rest/press
            // (6.525/4.602), which the black polarity rule could not without reintroducing
            // state-dependent label colour.
            //
            // The surface-polarity measurement that surfaced this trade-off in the first place is
            // retained, unmodified, as history — see VER10OneLabelColorPerThemeTest. That test
            // measures which single pure candidate (black or white) is best across every fill; it
            // has always picked white for every scheme, including AeroBlue/AeroDark. The interim
            // black/light-per-polarity split tried instead was a deliberate rejection of that
            // combined-winner measurement in favor of a per-surface rescue — this decision reverts
            // to what VER10 measured as the single best candidate all along.
            labelOnFilledSurface = Color.White,
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
            // Maintainer decision (2026-07-29): white on both polarities — same decision, same
            // date, same rationale as AeroBlue above. Cost accepted here: worst case 2.493:1
            // (filled-hover fillTop); AeroDark's recessed segment does not fully clear even at rest
            // (4.079 on fillBottom) unlike AeroBlue's, so it is also covered by
            // AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation. See
            // AeroBlue's KDoc above for the full rationale and the retained VER10 history.
            labelOnFilledSurface = Color.White,
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
 * Fallback heuristic for [AeroColorScheme.labelOnOutlinedSurface]'s default value — exercised only
 * when a custom [AeroColorScheme.copy] omits the token, since every shipped scheme sets it
 * explicitly from
 * [com.mordred.aero.components.buttons.AeroButtonContrastRegressionTest]/
 * [com.mordred.aero.verification.VER10OneLabelColorPerThemeTest]'s measured tables. A plain
 * luminance split on the passed-in [surface] proxy (never a hard-coded literal, never a throw) —
 * a light proxy colour picks [Color.Black], a dark one picks [Color.White].
 *
 * **Not used for [AeroColorScheme.labelOnFilledSurface] any more** — see
 * [defaultLabelColorForOpaqueFill] and [AeroColorScheme.labelOnFilledSurface]'s KDoc (WR-04). Still
 * used for [AeroColorScheme.labelOnOutlinedSurface]: that fallback's [background] proxy dominates
 * the real ~15%-alpha composite closely enough that this simpler split remains a reasonable
 * approximation there — see [AeroColorScheme.labelOnOutlinedSurface]'s KDoc for the full reasoning.
 */
private fun defaultLabelColorForSurface(surface: Color): Color =
    if (surface.luminance() > 0.5f) Color.Black else Color.White

/**
 * Fallback heuristic for [AeroColorScheme.labelOnFilledSurface]'s default value (WR-04,
 * 20-REVIEW.md addendum 2) — exercised only when a custom [AeroColorScheme.copy] omits the token.
 * Evaluates BOTH real darkened fill stops a filled/raised-segment surface is actually painted
 * with ([primary] darkened by [FILLED_FILL_TOP_DARKEN] and by [FILLED_FILL_BOTTOM_DARKEN] — the
 * exact transform [com.mordred.aero.components.buttons.resolveButtonStyle]/
 * [com.mordred.aero.components.selection.resolveSegmentStyle] apply), computes each pure
 * candidate's WORST-CASE (minimum) contrast ratio across both stops, and returns whichever
 * candidate's worst case is higher — never the average of the two stops, mirroring how the retired
 * per-call-site `resolveLabelColor` reasoned, so a candidate that wins on one stop and fails the
 * other cannot win here either.
 */
private fun defaultLabelColorForOpaqueFill(primary: Color): Color {
    val top = primary.darken(FILLED_FILL_TOP_DARKEN)
    val bottom = primary.darken(FILLED_FILL_BOTTOM_DARKEN)
    val blackWorstCase = minOf(contrastRatio(Color.Black, top), contrastRatio(Color.Black, bottom))
    val whiteWorstCase = minOf(contrastRatio(Color.White, top), contrastRatio(Color.White, bottom))
    return if (blackWorstCase >= whiteWorstCase) Color.Black else Color.White
}

/**
 * Standard WCAG 2.x contrast ratio (the lighter of the two relative luminances plus `0.05f`,
 * divided by the darker plus `0.05f`) — the same formula every test file in this codebase
 * independently re-derives (D-13: tests never import a production formula, so a wrong one cannot
 * certify itself). This IS production code, not a test — [defaultLabelColorForOpaqueFill] needs a
 * real ratio to pick between its two candidates, so D-13's "don't self-certify" concern does not
 * apply here; it applies to the *test* files that measure this fallback's output independently
 * (see `AeroColorSchemeTest`'s WR-04 regression coverage).
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
