package com.mordred.aero.components.pickers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGrid
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Calendar
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Public date picker (PICK-01): a read-only trigger field with a calendar icon button that
 * opens a month-grid popup. Selecting a day fires [onValueChange] with the chosen
 * [LocalDate] and closes the popup. Prev/next month navigation lives inside the popup grid.
 *
 * Out-of-range dates ([minDate]/[maxDate]) and dates rejected by [selectableDates] are
 * dimmed and non-interactive (see [dateIsDisabled]).
 *
 * The popup anchors to the trigger via [AeroCalendarPositionProvider] (PITFALL-02: a wide
 * calendar near the right edge of the window right-aligns instead of clipping) and renders
 * inside [PickerPopupContainer] (W11-02 two-layer glass surface). Uses `Popup` (never
 * `Dialog`) and never sets the transparency flag (W11-01: undecorated transparent windows
 * crash on Win11).
 *
 * @param value the currently selected date, or `null` for none.
 * @param onValueChange fires with the [LocalDate] the user clicks.
 * @param modifier applied to the trigger.
 * @param formatter renders [value] in the trigger field (default DD.MM.YYYY).
 * @param placeholder shown when [value] is `null`.
 * @param clearable when `true` and a value is set, shows an X button that calls [onClear].
 * @param onClear invoked by the clear button.
 * @param minDate inclusive lower bound; earlier dates are disabled.
 * @param maxDate inclusive upper bound; later dates are disabled.
 * @param selectableDates extra per-date predicate; `false` disables the date.
 * @param enabled when `false`, the trigger does not open the popup.
 */
@Composable
public fun AeroDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDate) -> String = { formatAeroDate(it) },
    placeholder: String = "Select date",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
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
                            imageVector = AeroIcons.Calendar,
                            contentDescription = "Open calendar",
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
                    var displayMonth by remember(value) { mutableStateOf(value ?: todayLocalDate()) }
                    AeroCalendarGrid(
                        displayMonth = displayMonth,
                        selected = value,
                        onDateSelected = { date ->
                            onValueChange(date)
                            expanded = false
                        },
                        onMonthChange = { displayMonth = it },
                        isDisabled = { date ->
                            dateIsDisabled(date, minDate, maxDate, selectableDates)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Full-date display helper (F6 default). Formats a [LocalDate] as DD.MM.YYYY (e.g. 07.06.2025).
 * All three date pickers use this as their default [formatter] — calendar navigation headers
 * (month-name + year) are unaffected.
 */
internal fun formatAeroDate(date: LocalDate): String =
    "%02d.%02d.%04d".format(date.dayOfMonth, date.monthNumber, date.year)

/**
 * Composes the date-availability constraints for [AeroDatePicker]. A date is disabled when it
 * falls before [minDate], after [maxDate], or is rejected by [selectableDates]. Extracted as a
 * pure, `internal` function so it is unit-testable without driving the composable.
 */
internal fun dateIsDisabled(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    selectableDates: (LocalDate) -> Boolean,
): Boolean =
    (minDate != null && date < minDate) ||
        (maxDate != null && date > maxDate) ||
        !selectableDates(date)

private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
