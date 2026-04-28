package com.mordred.aero.components.containers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

/**
 * CNT-04: Windows-Forms-style group box — a labeled rounded border around a content block.
 *
 * The label is inset on the top edge of the border (`Alignment.TopStart` with a small
 * horizontal start padding). Its `background = AeroTheme.colors.background` "carves out"
 * the border line behind the label so the label visually sits on the top border.
 *
 * Visual structure:
 * ```
 *   ┌── Label ─────────┐
 *   │                  │
 *   │  content         │
 *   │                  │
 *   └──────────────────┘
 * ```
 *
 * @param label label text shown on top border.
 * @param modifier outer modifier.
 * @param padding inner content padding (default 16.dp on each side).
 * @param contentTopOffset extra top padding to clear the label glyph (default 8.dp).
 * @param content content block.
 */
@Composable
public fun AeroGroupBox(
    label: String,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    contentTopOffset: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val shape = RoundedCornerShape(4.dp)

    // Pushing the bordered Box down by half the label glyph height lines the border
    // up with the label's vertical center; the label sits at the outer Box's TopStart
    // (y=0) and its center lands exactly where the border begins (y=labelHalfHeight).
    val labelHalfHeight = 7.dp

    Box(modifier = modifier) {
        // Bordered content area — pushed down so its top edge sits at the label's center.
        Box(
            modifier = Modifier
                .padding(top = labelHalfHeight)
                .border(width = 1.dp, color = colors.borderDefault, shape = shape)
                .padding(
                    PaddingValues(
                        start = padding,
                        end = padding,
                        top = padding + contentTopOffset,
                        bottom = padding
                    )
                )
        ) {
            content()
        }
        // Floating label centered on the top border — its background "carves out"
        // the border behind the label glyph.
        Text(
            text = label,
            color = colors.labelText,
            style = typography.label,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp)
                .background(colors.background)
                .padding(horizontal = 4.dp)
        )
    }
}
