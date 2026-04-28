package com.mordred.aero.components.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * LST-02: Compact pill-shaped label.
 *
 * Defaults to [AeroTheme.colors.primary] background and [AeroTheme.colors.onPrimary] text.
 * Use [Color.Unspecified] sentinel pattern so defaults are resolved inside the composable
 * (Composable context required for AeroTheme access — not permitted in default arg position).
 *
 * @param text Label text shown inside the pill.
 * @param modifier Layout modifier.
 * @param color Badge background color. Defaults to [AeroTheme.colors.primary].
 * @param contentColor Badge text color. Defaults to [AeroTheme.colors.onPrimary].
 */
@Composable
public fun AeroBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified
) {
    val resolvedBg = if (color == Color.Unspecified) AeroTheme.colors.primary else color
    val resolvedFg = if (contentColor == Color.Unspecified) AeroTheme.colors.onPrimary else contentColor

    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(resolvedBg)
            .heightIn(min = 16.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = resolvedFg,
            style = AeroTheme.typography.label
        )
    }
}
