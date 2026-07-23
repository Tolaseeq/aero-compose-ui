package com.mordred.aero.components.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aero-styled filled button (BTN-01 / VBTN-01..06).
 *
 * Thin public wrapper — all painting (two-tone fill, proportional top gloss, inner bevel,
 * outer contour, focus/hover glow) is delegated to [AeroButtonSurface]; see its KDoc for the
 * shared-surface contract and modifier-ordering rule. This wrapper only owns the filled
 * variant's locked defaults and public signature.
 *
 * @param text Label text shown on the button.
 * @param onClick Invoked when the button is clicked.
 * @param modifier Optional [Modifier] for the outer layout.
 * @param enabled Whether the button accepts input.
 * @param height Height of the button.
 * @param interactionSource Shared [MutableInteractionSource] for all state collectors.
 */
@Composable
public fun AeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 30.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    AeroButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        outlined = false,
        interactionSource = interactionSource,
    )
}
