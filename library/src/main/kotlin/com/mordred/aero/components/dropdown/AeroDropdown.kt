package com.mordred.aero.components.dropdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.popup.AeroDropdownItem
import com.mordred.aero.components.popup.AeroDropdownPopup
import com.mordred.aero.theme.AeroTheme

/**
 * DRP-02: Non-editable select dropdown. Opens a popup with keyboard navigation.
 *
 * @param options List of selectable options.
 * @param selected Currently selected option, or null if nothing selected.
 * @param onSelect Callback invoked when user selects an option.
 * @param modifier Modifier for the trigger container.
 * @param enabled Whether the dropdown is interactive.
 * @param placeholder Label shown when no option is selected.
 * @param optionLabel Converts an option to its display string.
 * @param width Width of the trigger element.
 */
@Composable
public fun <T> AeroDropdown(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Select...",
    optionLabel: (T) -> String = { it.toString() },
    width: Dp = 200.dp
) {
    var expanded by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf(-1) }

    // Reset highlightedIndex to current selected index when popup opens
    LaunchedEffect(expanded) {
        if (expanded) {
            highlightedIndex = options.indexOf(selected).coerceAtLeast(0)
        }
    }

    val colors = AeroTheme.colors
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .width(width)
            .height(28.dp)
            .clip(shape)
            .background(colors.cardBackground, shape)
            .border(
                width = if (expanded) 2.dp else 1.dp,
                color = if (expanded) colors.borderSelected else colors.borderDefault,
                shape = shape
            )
            .clickable(enabled = enabled) { expanded = !expanded }
            .padding(horizontal = 8.dp)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val labelText = selected?.let(optionLabel) ?: placeholder
            val labelColor = if (selected != null) colors.onSurface else colors.labelText
            Text(
                text = labelText,
                color = labelColor,
                style = AeroTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "▼",
                color = colors.labelText,
                style = AeroTheme.typography.label
            )
        }

        AeroDropdownPopup(
            expanded = expanded,
            anchorWidth = width,
            onDismissRequest = { expanded = false },
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@AeroDropdownPopup false
                when (event.key) {
                    Key.Escape -> {
                        expanded = false
                        true
                    }
                    Key.DirectionDown -> {
                        if (options.isNotEmpty()) {
                            highlightedIndex = (highlightedIndex + 1)
                                .coerceAtMost(options.size - 1)
                                .coerceAtLeast(0)
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (options.isNotEmpty()) {
                            highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0)
                        }
                        true
                    }
                    Key.Enter -> {
                        if (highlightedIndex in options.indices) {
                            onSelect(options[highlightedIndex])
                            expanded = false
                        }
                        true
                    }
                    else -> false
                }
            }
        ) {
            options.forEachIndexed { index, opt ->
                AeroDropdownItem(
                    text = optionLabel(opt),
                    selected = opt == selected,
                    highlighted = index == highlightedIndex,
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
