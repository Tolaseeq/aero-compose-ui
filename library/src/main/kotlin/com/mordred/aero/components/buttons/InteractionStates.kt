package com.mordred.aero.components.buttons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

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
