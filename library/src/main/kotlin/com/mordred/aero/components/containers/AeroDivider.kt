package com.mordred.aero.components.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * CNT-03: Horizontal or vertical 1.dp divider in the `borderDefault` color.
 *
 * Defaults to horizontal (full-width, 1.dp tall). Set [vertical] = true to
 * produce a 1.dp wide, full-height divider — typical for `AeroToolbar` separators.
 *
 * @param modifier optional layout modifier (use to constrain length).
 * @param vertical whether to render a vertical (true) or horizontal (false) divider.
 * @param thickness divider thickness (default 1.dp).
 */
@Composable
public fun AeroDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    thickness: Dp = 1.dp
) {
    val color = AeroTheme.colors.borderDefault
    Box(
        modifier = if (vertical) {
            modifier
                .width(thickness)
                .fillMaxHeight()
                .background(color)
        } else {
            modifier
                .fillMaxWidth()
                .height(thickness)
                .background(color)
        }
    )
}
