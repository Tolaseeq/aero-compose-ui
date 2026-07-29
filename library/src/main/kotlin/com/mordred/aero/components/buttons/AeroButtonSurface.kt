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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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
import com.mordred.aero.theme.darken
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
 * 19-09 (WR-01): the focus `aeroGlowRing` call gates on `state.focusVisible`, not the raw
 * `focused` flag — the ring now draws for keyboard-acquired focus only and stays suppressed for a
 * pointer-acquired focus, including after the pointer leaves. This puts [AeroButton] and
 * [AeroOutlinedButton] on the same gate as `AeroSwitch`, `AeroSegmentedControl` and
 * `AeroListItem` (19-05/G2), so one gesture produces one cue class library-wide. Focusability and
 * the Space/Enter activation binding are entirely unaffected — only whether the ring is DRAWN
 * changes. The hover `aeroGlowRing` call below is untouched and continues to read the raw
 * `hovered` flag.
 *
 * @param outlined Differentiates [AeroOutlinedButton] from this filled default; [resolveButtonStyle]
 * applies [AeroSurfaceStyle.outlinedStyle]'s fixed delta (fill alpha × 0.15, gloss 0.22 → 0.15) on
 * top of the per-state filled resolution when `true` (17-03, VBTN-05/06). Rim alpha for both filled
 * and outlined is theme-aware (17-05 round-2 sign-off gap-fix) — see [resolveButtonStyle]'s KDoc.
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

    val colors = AeroTheme.colors
    val style = resolveButtonStyle(
        colors = colors,
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
                active = state.focusVisible && enabled,
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
            color = resolveLabelColor(style.fillTop, style.fillBottom, colors.background),
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
 *
 * `internal` (not `private`) specifically so `AeroSegmentedControl` (Phase 19, cross-package)
 * can import and reuse this exact value for its recessed selected segment (VSEL-03) — a copy
 * must never be made; any future retune of this constant must apply to both call sites at once.
 */
internal val PRESSED_INNER_SHADOW: Shadow = Shadow(
    radius = 2.dp,
    color = Color.Black.copy(alpha = 0.35f),
    offset = DpOffset(0.dp, 1.dp),
)

/**
 * The dark candidate [resolveLabelColor] picks between (D-12/D-13, closing the AeroButton
 * label-contrast todo). Pure `Color.Black`, not an off-black — the 20-UI-SPEC.md Color section's
 * own per-theme contrast table (point 2) was computed against the pure value, and its point 5
 * ("a hard-black or hard-white label ... is the expected, intended look") argues for it
 * explicitly. Held behind this named constant (not inlined at the call site) so a later shift
 * toward an off-black token is a one-line change behind [resolveLabelColor], never a second
 * per-component retune.
 */
internal val LABEL_CANDIDATE_DARK: Color = Color.Black

/** The light candidate [resolveLabelColor] picks between — see [LABEL_CANDIDATE_DARK]'s KDoc. */
internal val LABEL_CANDIDATE_LIGHT: Color = Color.White

/**
 * Standard WCAG 2.x contrast ratio: the lighter of the two relative luminances plus `0.05f`,
 * divided by the darker plus `0.05f`, via Compose's own [androidx.compose.ui.graphics.luminance]
 * extension (correct gamma/coefficients — never hand-rolled). Only meaningful between two fully
 * opaque colours; [resolveLabelColor] composites over [AeroColorScheme.background] before calling
 * this so an uncomposited translucent fill stop (e.g. the outlined variant's 0.15x-alpha fill)
 * never reaches it.
 */
private fun labelContrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * D-12's single source of truth for the on-fill label colour — both [AeroButtonSurface]'s `Text`
 * and `AeroSegmentedControl`'s segment `Text` (Phase 19, cross-package import) call this
 * identically. This is a colour a component ASKS FOR by resolving it algorithmically from its
 * OWN actual fill, never a colour a component author picks by eye or tunes as a per-component
 * constant — that per-component-constant path is exactly the gap-G5 lesson (19-UAT.md): the
 * segmented control's raised fill was darkened chasing contrast for a fixed label token until it
 * became a bespoke colour unlike the button's own fill. Per-component constant retuning to chase
 * contrast is forbidden; the mechanism below is the only sanctioned lever.
 *
 * [fillTop]/[fillBottom] are composited over [backdrop] via [Color.compositeOver] before either
 * candidate is measured against them — [Color.luminance] ignores alpha entirely, so an
 * uncomposited translucent stop (the outlined variant multiplies fill alpha by 0.15) would report
 * a luminance for the raw fill colour, not for what is actually visible behind it.
 *
 * For each of [LABEL_CANDIDATE_DARK]/[LABEL_CANDIDATE_LIGHT], the WORST-case ratio against the two
 * composited stops is computed (`minOf`, never an average) — a candidate that wins big on one stop
 * and fails the other must not be chosen over one that clears the floor on both, since the label
 * is legible everywhere on the gradient or it is not legible at all.
 */
internal fun resolveLabelColor(fillTop: Color, fillBottom: Color, backdrop: Color): Color {
    val compositedTop = fillTop.compositeOver(backdrop)
    val compositedBottom = fillBottom.compositeOver(backdrop)
    val darkWorstCase = minOf(
        labelContrastRatio(LABEL_CANDIDATE_DARK, compositedTop),
        labelContrastRatio(LABEL_CANDIDATE_DARK, compositedBottom),
    )
    val lightWorstCase = minOf(
        labelContrastRatio(LABEL_CANDIDATE_LIGHT, compositedTop),
        labelContrastRatio(LABEL_CANDIDATE_LIGHT, compositedBottom),
    )
    return if (darkWorstCase >= lightWorstCase) LABEL_CANDIDATE_DARK else LABEL_CANDIDATE_LIGHT
}

/** Filled → outlined fill-alpha multiplier (17-UI-SPEC.md Outlined contract, D-06). */
private const val OUTLINED_FILL_ALPHA_MULTIPLIER: Float = 0.15f

/** Filled → outlined target rest glossAlpha, used as the numerator of the proportional scale below. */
private const val OUTLINED_GLOSS_ALPHA_TARGET: Float = 0.15f

/**
 * Filled rest glossAlpha (`AeroSurfaceStyle`'s own default), the denominator of the proportional
 * scale. MUST stay equal to [AeroSurfaceStyle]'s `glossAlpha` default (0.22f, sign-off gap-fix
 * 17-05) — see [outlinedStyle]'s KDoc.
 */
private const val FILLED_REST_GLOSS_ALPHA: Float = 0.22f

/**
 * Ceiling applied to the filled rim's alpha (17-05 ROUND-2 sign-off gap-fix, FIX A). Rim color
 * already equals [AeroColorScheme.glassBorder] — this constant only caps how bright it's allowed
 * to be drawn, so themes whose native `glassBorder.alpha` is already dimmer than this (AeroBlue
 * ~0.31, AeroDark ~0.19) draw at their OWN native alpha rather than being brightened up to it,
 * while Classic's opaque (1.0) native alpha gets capped down to exactly this value (unchanged
 * from the 17-05 round-1 fix — Classic was reported as already correct). See
 * [resolveButtonStyle]'s KDoc for the `minOf(nativeRimAlpha, FILLED_RIM_ALPHA_CAP)` formula.
 */
private const val FILLED_RIM_ALPHA_CAP: Float = 0.45f

/**
 * Multiplier lifting the outlined rim above the filled rim's native alpha before the
 * [OUTLINED_RIM_ALPHA_CAP] ceiling is applied (17-05 ROUND-2 sign-off gap-fix, FIX A) — outlined
 * still leans on its rim for contour identity (it has almost no fill to lean on instead), but the
 * lift is proportional to the theme's own native brightness rather than a fixed absolute value,
 * so it never goes white-hot on the light-tokened Aero themes.
 */
private const val OUTLINED_RIM_ALPHA_MULTIPLIER: Float = 1.7f

/** Ceiling applied to the outlined rim's alpha after [OUTLINED_RIM_ALPHA_MULTIPLIER] is applied. */
private const val OUTLINED_RIM_ALPHA_CAP: Float = 0.70f

/**
 * Darken amount (via [Color.darken]'s RGB-mix, never `.copy(alpha = ...)`, correct on Classic's
 * opaque tokens) applied to [AeroColorScheme.primary] to derive the filled button's rest
 * `fillTop` (17-05 ROUND-2 sign-off gap-fix, FIX B) — the ornament-derived `fillSplitTop`
 * (`primary.lighten(0.08f)`) read too light against this fill's own content token, so the BUTTON
 * (not the shared ornament tokens the Primitives gallery reads) overrides to a darker two-tone.
 * NOTE: the label does not actually resolve to white — see [AeroButtonSurface]'s `Text`, which
 * sets no explicit `color` and therefore inherits ambient `LocalContentColor` (`onBackground`,
 * byte-identical to `onSurface` in every shipped scheme). The "white button text" framing this
 * KDoc previously carried was never true; corrected as part of closing gap G5
 * (19-UAT.md) alongside the `AeroButton`-label-contrast tracking todo.
 *
 * `internal` (not `private`) — `AeroSegmentedControl` (Phase 19, gap G5, cross-package) imports
 * this exact constant for its raised (unselected) segment fill so the two components' raised fill
 * cannot drift into two different colour languages; any future retune applies to both call sites
 * from this single declaration.
 */
internal const val FILLED_FILL_TOP_DARKEN: Float = 0.20f

/**
 * Darken amount applied to [AeroColorScheme.primary] to derive the filled button's rest
 * `fillBottom`. `internal` for the same cross-package reuse reason as [FILLED_FILL_TOP_DARKEN]
 * (gap G5).
 */
internal const val FILLED_FILL_BOTTOM_DARKEN: Float = 0.36f

/**
 * Fixed-delta transform (D-06, VBTN-05/06) turning an already-resolved filled [AeroSurfaceStyle]
 * into its outlined equivalent — never a second independently-authored style. [resolveButtonStyle]
 * calls this on top of the per-state filled resolution, so outlined provably inherits every state
 * delta by construction (17-RESEARCH.md Pattern 5).
 *
 * 17-UI-SPEC.md "Outlined AeroOutlinedButton — per-state contract": fill alpha × ~0.15 ("a
 * whisper of gloss/gradient"); glossAlpha scaled proportionally toward the rest-state target
 * ~0.15 (so an already-zeroed gloss, e.g. pressed/disabled, stays zero rather than jumping back
 * up). `bevelLight`/`bevelShadow` and `cornerRadius` are left unchanged (contour emphasis carries
 * the depth cue instead of fill).
 *
 * `rimAlpha` is deliberately left UNTOUCHED here (17-05 ROUND-2 sign-off gap-fix, FIX A) — it no
 * longer owns a fixed outlined value. [resolveButtonStyle] computes the theme-aware outlined rim
 * alpha (via `minOf(nativeRimAlpha * OUTLINED_RIM_ALPHA_MULTIPLIER, OUTLINED_RIM_ALPHA_CAP)`) and
 * sets it explicitly on top of this transform's result, so it can react to `colors` — a piece of
 * context this style-to-style transform doesn't have.
 */
internal fun AeroSurfaceStyle.outlinedStyle(): AeroSurfaceStyle = copy(
    fillTop = fillTop.copy(alpha = fillTop.alpha * OUTLINED_FILL_ALPHA_MULTIPLIER),
    fillBottom = fillBottom.copy(alpha = fillBottom.alpha * OUTLINED_FILL_ALPHA_MULTIPLIER),
    glossAlpha = glossAlpha * (OUTLINED_GLOSS_ALPHA_TARGET / FILLED_REST_GLOSS_ALPHA),
)

/**
 * Pure, Compose-free resolution point mapping (rest/hover/press/focus/disabled) → [AeroSurfaceStyle]
 * (17-UI-SPEC.md "Filled AeroButton — per-state contract"), consumed by [AeroButtonSurface].
 *
 * Precedence (disabled wins over everything, press over hover): [enabled] `false` → disabled
 * branch below; else [pressed] → [AeroSurfaceStyle.pressedRecess]; else [hovered] →
 * [AeroSurfaceStyle.hoverLighten]; else the unmodified rest style. [focused] carries no fill
 * delta here — focus is expressed purely by [AeroButtonSurface]'s persistent `aeroGlowRing`
 * call, per D-04 (kept as a resolver parameter for API symmetry/future use, not because it
 * currently branches anything).
 *
 * **Theme-aware rim alpha (17-05 ROUND-2 sign-off gap-fix, FIX A).** The rim's color already
 * equals [AeroColorScheme.glassBorder] (`AeroSurfaceStyle.rest`'s `rimColor = ornaments.rimLight
 * = base.glassBorder`) — the bug was the drawn ALPHA being a fixed 0.45 regardless of theme,
 * brighter than the native `glassBorder.alpha` on the white-based Aero themes (AeroBlue ~0.31,
 * AeroDark ~0.19) while happening to look right on Classic (native 1.0, capped to 0.45 either
 * way). `nativeRimAlpha = colors.glassBorder.alpha` is computed once per call; filled rim alpha is
 * `minOf(nativeRimAlpha, FILLED_RIM_ALPHA_CAP)` (dimmer on AeroBlue/AeroDark, unchanged on
 * Classic); outlined rim alpha is `minOf(nativeRimAlpha * OUTLINED_RIM_ALPHA_MULTIPLIER,
 * OUTLINED_RIM_ALPHA_CAP)` (outlined keeps a stronger contour than filled for the same theme,
 * since it has almost no fill to lean on instead, without ever going white-hot).
 *
 * **Theme-aware darker filled fill (17-05 ROUND-2 sign-off gap-fix, FIX B).** `rest`'s fill is
 * overridden here (BUTTON-scoped — [AeroOrnamentTokens.fillSplitTop]/`fillSplitBottom` are left
 * alone so the Primitives gallery is untouched) to `colors.primary.darken(...)` two-tone, darker
 * than the ornament-derived fill. NOTE: the label itself resolves to ambient `LocalContentColor`
 * (`onBackground` in the showcase, byte-identical to `onSurface`), not white — see
 * [FILLED_FILL_TOP_DARKEN]'s KDoc. Everything else (gloss, bevel) still comes from
 * [AeroSurfaceStyle.rest]'s ornament-derived defaults; hover/press/disabled transforms compose on
 * top of this darker rest exactly as before.
 *
 * [AeroSurfaceStyle.flattenDisabled] is the TERMINAL transform when disabled (17-05 sign-off
 * gap-fix) — not an input to [AeroSurfaceStyle.outlinedStyle]. When [outlined], `.outlinedStyle()`
 * is applied to the rest style FIRST (with its own outlined rim alpha set explicitly on top,
 * since `outlinedStyle()` itself no longer touches `rimAlpha`), then `.flattenDisabled()` is
 * applied LAST, so its rimAlpha-halving and neutral-fill collapse are the final word — disabled
 * outlined rim is always exactly half of active outlined rim, for every theme, clearly dimmer.
 * Disabled FILLED likewise carries [FILLED_RIM_ALPHA_CAP]'s theme-aware value into
 * `flattenDisabled`'s halving. No hover/press transforms are ever applied in the disabled branch.
 *
 * When enabled and [outlined] is `true`, [AeroSurfaceStyle.outlinedStyle] is applied on top of the
 * already-per-state-resolved filled style (17-03) — outlined provably inherits every state delta
 * by construction, never an independently-authored second style (VBTN-06).
 */
internal fun resolveButtonStyle(
    colors: AeroColorScheme,
    outlined: Boolean,
    hovered: Boolean,
    pressed: Boolean,
    focused: Boolean,
    enabled: Boolean,
): AeroSurfaceStyle {
    val nativeRimAlpha = colors.glassBorder.alpha
    val filledRimAlpha = minOf(nativeRimAlpha, FILLED_RIM_ALPHA_CAP)
    val outlinedRimAlpha = minOf(nativeRimAlpha * OUTLINED_RIM_ALPHA_MULTIPLIER, OUTLINED_RIM_ALPHA_CAP)

    val rest = AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp).copy(
        fillTop = colors.primary.darken(FILLED_FILL_TOP_DARKEN),
        fillBottom = colors.primary.darken(FILLED_FILL_BOTTOM_DARKEN),
        rimAlpha = filledRimAlpha,
    )
    if (!enabled) {
        val disabledBase = if (outlined) {
            rest.outlinedStyle().copy(rimAlpha = outlinedRimAlpha)
        } else {
            rest
        }
        return disabledBase.flattenDisabled(colors)
    }
    val resolved = when {
        pressed -> rest.pressedRecess(PRESSED_INNER_SHADOW)
        hovered -> rest.hoverLighten()
        else -> rest
    }
    return if (outlined) {
        resolved.outlinedStyle().copy(rimAlpha = outlinedRimAlpha)
    } else {
        resolved
    }
}
