package com.mordred.aero.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.Text
import com.mordred.aero.components.popup.AeroDropdownItem
import com.mordred.aero.components.popup.AeroDropdownPopup
import com.mordred.aero.components.popup.AeroPopupPositionProvider
import com.mordred.aero.theme.AeroTheme

/**
 * DRP-01: ComboBox combining free-text input with a filtered suggestions popup.
 *
 * KDoc: AeroComboBox popup is non-focusable for text-field continuity — keyboard nav
 * within popup is not supported in v1; mouse-click selection works.
 *
 * @param text Current text value in the field.
 * @param onTextChange Callback for text changes.
 * @param options Full list of candidate strings to filter from.
 * @param onOptionSelect Callback when a suggestion is selected.
 * @param modifier Modifier for the outer container.
 * @param enabled Whether the field is interactive.
 * @param placeholder Placeholder shown when text is empty.
 * @param width Width of the component.
 */
@Composable
public fun AeroComboBox(
    text: String,
    onTextChange: (String) -> Unit,
    options: List<String>,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Type or select...",
    width: Dp = 240.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var expanded by remember { mutableStateOf(false) }

    // Case-insensitive filter
    val filtered = options.filter { it.contains(text, ignoreCase = true) }

    // Auto-open popup when focused and there are matching options
    LaunchedEffect(focused, text) {
        expanded = focused && options.any { it.contains(text, ignoreCase = true) }
    }

    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    Box(modifier = modifier.width(width)) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            singleLine = true,
            enabled = enabled,
            interactionSource = interactionSource,
            textStyle = AeroTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(shape)
                .background(colors.cardBackground, shape)
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) colors.borderSelected else colors.borderDefault,
                    shape = shape
                )
                .alpha(if (enabled) 1f else 0.4f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        expanded = false
                        true
                    } else false
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.labelText,
                            style = AeroTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Non-focusable popup so text field retains focus; mouse-click selection works.
        // Two-layer background: fully-opaque base (colors.background) under the panelBackground
        // tint so nothing behind the popup bleeds through. Width is clamped to anchor width.
        if (expanded && filtered.isNotEmpty()) {
            val popupShape = RoundedCornerShape(4.dp)
            Popup(
                popupPositionProvider = remember { AeroPopupPositionProvider() },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                Box(
                    Modifier
                        .widthIn(min = width, max = width)
                        .heightIn(max = 240.dp)
                        .clip(popupShape)
                        .background(colors.background, popupShape)
                        .background(colors.panelBackground, popupShape)
                        .border(1.dp, colors.glassBorder, popupShape)
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        filtered.forEach { opt ->
                            AeroDropdownItem(
                                text = opt,
                                selected = opt == text,
                                highlighted = false,
                                onClick = {
                                    onOptionSelect(opt)
                                    onTextChange(opt)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
