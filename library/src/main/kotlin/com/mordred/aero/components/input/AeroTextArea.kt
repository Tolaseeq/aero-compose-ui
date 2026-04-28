package com.mordred.aero.components.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.containers.AeroScrollBar
import com.mordred.aero.theme.AeroTheme

/**
 * Multi-line text area with Aero-styled focus border and vertical scrolling.
 *
 * The border animates from [AeroTheme.colors.borderDefault] to [AeroTheme.colors.borderSelected]
 * over 150 ms with LinearEasing on focus. An [AeroScrollBar] overlay is attached to the
 * same scroll state as the underlying `BasicTextField`, so when content overflows the
 * field the user sees an Aero-styled vertical scrollbar.
 *
 * @param value Current text value.
 * @param onValueChange Callback invoked when text changes.
 * @param modifier Modifier applied to the outer box.
 * @param enabled Whether the field is interactive.
 * @param placeholder Placeholder text shown when [value] is empty.
 * @param minHeight Minimum height of the text area (default 80.dp).
 * @param maxHeight Maximum height before vertical scroll activates (default 240.dp).
 * @param interactionSource Interaction source for focus/hover state tracking.
 */
@Composable
public fun AeroTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    minHeight: Dp = 80.dp,
    maxHeight: Dp = 240.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)
    val focused by interactionSource.collectIsFocusedAsState()
    val scrollState = rememberScrollState()

    val borderColor by animateColorAsState(
        targetValue = if (!enabled) colors.borderDefault.copy(alpha = 0.4f)
                      else if (focused) colors.borderSelected
                      else colors.borderDefault,
        animationSpec = tween(150, easing = LinearEasing),
        label = "border"
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (focused && enabled) 2f else 1f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "borderWidth"
    )

    Box(
        modifier = modifier
            .heightIn(min = minHeight, max = maxHeight)
            .background(colors.cardBackground, shape)
            .border(borderWidth.dp, borderColor, shape)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = false,
            maxLines = Int.MAX_VALUE,
            interactionSource = interactionSource,
            textStyle = AeroTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 14.dp)        // reserve space so cursor never sits behind the scrollbar
                .padding(8.dp)
                .verticalScroll(scrollState),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.labelText.copy(alpha = 0.5f),
                            style = AeroTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            }
        )
        AeroScrollBar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}
