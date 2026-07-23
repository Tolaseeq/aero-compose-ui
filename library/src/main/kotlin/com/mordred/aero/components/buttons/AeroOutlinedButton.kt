package com.mordred.aero.components.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aero-styled outlined button (BTN-02 / VBTN-01..06).
 *
 * Thin public wrapper — all painting is delegated to [AeroButtonSurface] with `outlined = true`;
 * see its KDoc for the shared-surface contract and modifier-ordering rule. The outlined variant
 * is a fixed delta of the filled style ([AeroSurfaceStyle.outlinedStyle], 17-UI-SPEC.md "Outlined
 * AeroOutlinedButton — per-state contract") — never a second independent painter, which is what
 * makes VBTN-06's "cannot visually drift from [AeroButton]" guarantee true by construction. This
 * wrapper only owns the outlined variant's locked defaults and public signature.
 *
 * @param text Label text shown on the button.
 * @param onClick Invoked when the button is clicked.
 * @param modifier Optional [Modifier] for the outer layout.
 * @param enabled Whether the button accepts input.
 * @param height Height of the button.
 * @param interactionSource Shared [MutableInteractionSource] for all state collectors.
 */
@Composable
public fun AeroOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 28.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    AeroButtonSurface(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = height,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        outlined = true,
        interactionSource = interactionSource,
    )
}
