package com.mordred.aero.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue

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
    val focused: Boolean
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
    return AeroInteractionState(hovered, pressed, focused)
}
