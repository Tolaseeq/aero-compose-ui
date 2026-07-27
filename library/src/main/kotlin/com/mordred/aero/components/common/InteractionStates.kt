package com.mordred.aero.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal const val ANIMATION_DURATION_MS: Int = 150

@Composable
internal fun rememberHoverState(source: InteractionSource): State<Boolean> =
    source.collectIsHoveredAsState()

@Composable
internal fun rememberPressedState(source: InteractionSource): State<Boolean> =
    source.collectIsPressedAsState()

@Composable
internal fun rememberFocusState(source: InteractionSource): State<Boolean> =
    source.collectIsFocusedAsState()

@Composable
internal fun animatedAlpha(target: Float): State<Float> =
    animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
        label = "alpha"
    )

/**
 * Bundled hover/press/focus booleans for a single [InteractionSource] (PRIM-15).
 *
 * A single connection point so every Aero component — buttons today, switches/segmented
 * controls/sliders/range controls in later phases — reads the same three booleans instead
 * of re-deriving them. Deliberately holds only raw booleans, never resolved colors or an
 * [com.mordred.aero.theme.AeroSurfaceStyle] variant: each component decides its own fill/rim
 * from these flags (see [rememberAeroInteractionState] KDoc — a second shared abstraction
 * that also picks colors is the drift risk this type avoids).
 */
internal data class AeroInteractionState(
    val hovered: Boolean,
    val pressed: Boolean,
    val focused: Boolean,
    val focusVisible: Boolean
)

/**
 * Collects hover/press/focus from [source] into a single [AeroInteractionState].
 *
 * Wiring precedent: pair the modifier that reports these interactions (e.g.
 * `Modifier.hoverable(interactionSource)`, `Modifier.clickable(interactionSource = ...)`,
 * `Modifier.focusable(interactionSource = ...)`) with this collector exactly as
 * [com.mordred.aero.components.list.AeroListItem] pairs `Modifier.hoverable(interactionSource)`
 * with `interactionSource.collectIsHoveredAsState()` (VLST-04). This function only collects
 * state — it does not resolve colors, so callers still pick their own
 * [com.mordred.aero.theme.AeroSurfaceStyle] variant from the returned booleans.
 */
@Composable
internal fun rememberAeroInteractionState(source: InteractionSource): AeroInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val focusVisible = rememberFocusVisible(source)
    return AeroInteractionState(hovered, pressed, focused, focusVisible)
}

/**
 * Focus-visibility state (19-05, gap G2/VSEL-02) — decides whether a focus cue should be DRAWN,
 * independent of whether the element IS focused. [focused]/[hovered] mirror the raw interaction
 * stream; [pointerAcquired] is set only when a press was preceded by hover on the same element
 * (see [reduce]). [visible] is the single value components should gate their focus ring on.
 */
internal data class FocusVisibility(
    val focused: Boolean = false,
    val hovered: Boolean = false,
    val pointerAcquired: Boolean = false
) {
    val visible: Boolean get() = focused && !pointerAcquired
}

/**
 * Folds one [Interaction] into the next [FocusVisibility] state.
 *
 * Why interaction-derived, not input-mode-derived (see 19-05-PLAN.md `<planner_finding>`): the
 * platform's own focus-visible signal (`LocalInputModeManager` / `InputModeFilterIndication`,
 * shipped in `foundation-desktop-1.11.1.jar`) only gates the platform `Indication`, which this
 * library deliberately disables everywhere (`indication = null`, P-01). Whether a desktop mouse
 * press requests `InputMode.Touch` or `InputMode.Keyboard` cannot be determined from the resolved
 * artifacts, so gating on `InputMode` risks being a silent no-op on some Compose builds. Folding
 * the interaction stream itself is correct under either behaviour and needs no platform
 * assumption, at the cost of one accepted imprecision documented below.
 *
 * Load-bearing requirement: the [InteractionSource] this is folded from must be the SAME source
 * every hover/press/focus modifier on the component feeds (see
 * [com.mordred.aero.components.selection.AeroSwitch] — `toggleable` and `hoverable` share one
 * `interactionSource`). If a future edit gives them separate sources, this reducer will see a
 * press with no matching hover and silently stop suppressing the pointer-acquired cue.
 *
 * Accepted imprecision: pressing Space/Enter while the pointer happens to be resting on the
 * control (already hovered) is indistinguishable from a mouse click and will suppress the ring
 * until focus leaves and returns. This does not occur in practice — a keyboard user driving Tab
 * and Space is not simultaneously hovering the control with a mouse.
 */
internal fun FocusVisibility.reduce(interaction: Interaction): FocusVisibility = when (interaction) {
    is HoverInteraction.Enter -> copy(hovered = true)
    is HoverInteraction.Exit -> copy(hovered = false)
    is PressInteraction.Press -> if (hovered) copy(pointerAcquired = true) else this
    is FocusInteraction.Focus -> copy(focused = true)
    is FocusInteraction.Unfocus -> FocusVisibility()
    else -> this
}

/**
 * Folds [source]'s interaction stream into [FocusVisibility] and returns [FocusVisibility.visible]
 * — the value every Aero component's focus glow ring should gate on instead of the raw `focused`
 * flag (gap G2/VSEL-02). See [reduce] for the reducer's rules and the reason it is interaction-
 * derived rather than input-mode-derived.
 */
@Composable
internal fun rememberFocusVisible(source: InteractionSource): Boolean {
    var state by remember(source) { mutableStateOf(FocusVisibility()) }
    LaunchedEffect(source) {
        source.interactions.collect { interaction ->
            state = state.reduce(interaction)
        }
    }
    return state.visible
}
