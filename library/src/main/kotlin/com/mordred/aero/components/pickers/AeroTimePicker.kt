package com.mordred.aero.components.pickers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.components.input.AeroTextField
import com.mordred.aero.components.internal.popup.AeroCalendarPositionProvider
import com.mordred.aero.components.pickers.internal.PickerPopupContainer
import com.mordred.aero.components.pickers.internal.TimeFields
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Clock
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.LocalTime

/**
 * Public 24-hour time picker (PICK-03): a read-only trigger field with a clock icon button that
 * opens a popup hosting hour / minute (/ optional second) spinners. Adjusting any spinner fires
 * [onValueChange] with the reassembled [LocalTime]; there is no Apply gate (that belongs only to
 * `AeroDateTimePicker`, plan 04). The popup stays open while the user adjusts and dismisses on
 * click-outside.
 *
 * 24-hour only — there is intentionally no `use12Hour` / AM-PM parameter (descoped per user
 * decision). When [showSeconds] is `false` the emitted [LocalTime] always has `second == 0`.
 *
 * The popup anchors to the trigger via [AeroCalendarPositionProvider] and renders inside
 * [PickerPopupContainer] (W11-02 two-layer glass surface). Uses `Popup` (never `Dialog`) and never
 * sets the transparency flag (W11-01: undecorated transparent windows crash on Win11).
 *
 * @param value the currently selected time, or `null` for none.
 * @param onValueChange fires with the [LocalTime] as the user adjusts any spinner.
 * @param modifier applied to the trigger.
 * @param formatter renders [value] in the trigger field (default ISO, e.g. `14:30`).
 * @param placeholder shown when [value] is `null`.
 * @param clearable when `true` and a value is set, shows an X button that calls [onClear].
 * @param onClear invoked by the clear button.
 * @param enabled when `false`, the trigger does not open the popup.
 * @param showSeconds when `true`, adds a third (seconds) spinner.
 * @param minuteStep step size for the minute spinner.
 */
@Composable
public fun AeroTimePicker(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalTime) -> String = { it.toString() },
    placeholder: String = "Select time",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true,
    showSeconds: Boolean = false,
    minuteStep: Int = 1,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = value?.let(formatter) ?: ""

    Box(modifier = modifier) {
        AeroTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            placeholder = placeholder,
            modifier = Modifier,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (clearable && value != null) {
                        AeroIconButton(
                            onClick = { onClear?.invoke() },
                            enabled = enabled,
                            size = 24.dp,
                        ) {
                            Icon(
                                imageVector = AeroIcons.X,
                                contentDescription = "Clear",
                                tint = AeroTheme.colors.onSurface,
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                    }
                    AeroIconButton(
                        onClick = { if (enabled) expanded = !expanded },
                        enabled = enabled,
                        size = 24.dp,
                    ) {
                        Icon(
                            imageVector = AeroIcons.Clock,
                            contentDescription = "Open time picker",
                            tint = AeroTheme.colors.onSurface,
                        )
                    }
                }
            },
        )

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
                PickerPopupContainer {
                    Box(Modifier.padding(12.dp)) {
                        TimeFields(
                            time = value ?: LocalTime(0, 0, 0),
                            onTimeChange = { onValueChange(it) },
                            enabled = enabled,
                            showSeconds = showSeconds,
                            minuteStep = minuteStep,
                        )
                    }
                }
            }
        }
    }
}
