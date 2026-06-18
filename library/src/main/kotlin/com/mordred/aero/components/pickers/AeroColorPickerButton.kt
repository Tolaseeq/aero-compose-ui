package com.mordred.aero.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.internal.popup.AeroCalendarPositionProvider
import com.mordred.aero.components.pickers.internal.color.DefaultAeroSwatches
import com.mordred.aero.theme.AeroTheme

/**
 * Swatch-trigger color picker (PICK-05/06/07). Renders a small color swatch reflecting
 * the current [value]; clicking it opens an anchored [Popup] hosting the full
 * [AeroColorPicker] panel.
 *
 * NEW-PICK-05: the `Popup` lives INSIDE the trigger `Box` so it anchors to the swatch.
 * W11-01: uses `Popup` (never an undecorated transparent `Dialog`).
 *
 * @param value the current color shown on the swatch and seeded into the panel.
 * @param onValueChange emits the derived [Color] as the user edits in the panel.
 * @param modifier layout modifier applied to the trigger swatch.
 * @param enableAlpha forwarded to [AeroColorPicker]; adds the alpha slider + `#RRGGBBAA`.
 * @param swatches preset colors for the panel's swatch row.
 * @param enabled whether the trigger and panel are interactive.
 */
@Composable
public fun AeroColorPickerButton(
    value: Color,
    onValueChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enableAlpha: Boolean = false,
    swatches: List<Color> = DefaultAeroSwatches,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(value)
            .border(1.dp, AeroTheme.colors.glassBorder, shape)
            .clickable(enabled = enabled) { expanded = !expanded },
    ) {
        if (expanded) {
            Popup(
                popupPositionProvider = remember { AeroCalendarPositionProvider(gap = 4) },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                AeroColorPicker(
                    value = value,
                    onValueChange = onValueChange,
                    enableAlpha = enableAlpha,
                    swatches = swatches,
                    enabled = enabled,
                )
            }
        }
    }
}
