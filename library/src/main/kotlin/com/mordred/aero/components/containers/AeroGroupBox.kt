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

    Box(modifier = modifier.padding(top = 8.dp)) {
        // Bordered content area — top padding leaves room for the floating label
        Box(
            modifier = Modifier
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
        // Floating label, drawn over the top border with a small horizontal inset.
        // The opaque background "carves out" the border behind the label glyph.
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
