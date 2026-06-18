package com.mordred.aero.components.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.input.AeroTextField
import com.mordred.aero.components.internal.popup.AeroCalendarPositionProvider
import com.mordred.aero.components.pickers.internal.PickerPopupContainer
import com.mordred.aero.components.pickers.internal.TimeFields
import com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGrid
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Calendar
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Public date+time picker (PICK-04): a read-only trigger field with a calendar icon button that
 * opens a single popup hosting a month-grid calendar above an hour/minute(/second) time row, plus
 * explicit **Cancel** / **Apply** buttons.
 *
 * The CRITICAL commit-gate behavior (NEW-PICK-02): clicking a day sets *pending* state but does
 * NOT close the popup and does NOT fire [onValueChange]; adjusting the time spinners likewise only
 * mutates pending state. [onValueChange] fires with the combined [LocalDateTime] ONLY when the user
 * clicks **Apply** (and only when a pending date exists). **Cancel** dismisses silently. This
 * prevents the auto-close-before-time-set bug that a per-day-click emission would cause.
 *
 * The component composes the Phase 7 [AeroCalendarGrid] and the plan-03 [TimeFields] directly — it
 * does NOT embed [AeroDatePicker] / [AeroTimePicker]. Date constraints reuse [dateIsDisabled] from
 * [AeroDatePicker]. The popup anchors via [AeroCalendarPositionProvider] inside
 * [PickerPopupContainer] (W11-02 glass surface), uses `Popup` (never `Dialog`), and never sets the
 * transparency flag (W11-01: undecorated transparent windows crash on Win11).
 *
 * @param value the currently committed date+time, or `null` for none.
 * @param onValueChange fires with the combined [LocalDateTime] ONLY when **Apply** is clicked.
 * @param modifier applied to the trigger.
 * @param formatter renders [value] in the trigger field (default ISO).
 * @param placeholder shown when [value] is `null`.
 * @param clearable when `true` and a value is set, shows an X button that calls [onClear].
 * @param onClear invoked by the clear button.
 * @param minDate inclusive lower bound; earlier dates are disabled in the grid.
 * @param maxDate inclusive upper bound; later dates are disabled in the grid.
 * @param selectableDates extra per-date predicate; `false` disables the date.
 * @param enabled when `false`, the trigger does not open the popup.
 * @param showSeconds when `true`, the time row exposes a seconds spinner.
 * @param minuteStep step size for the minute spinner.
 */
@Composable
public fun AeroDateTimePicker(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDateTime) -> String = { it.toString() },
    placeholder: String = "Select date & time",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
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
                            imageVector = AeroIcons.Calendar,
                            contentDescription = "Open date & time picker",
                            tint = AeroTheme.colors.onSurface,
                        )
                    }
                }
            },
        )

        if (expanded) {
            // Pending state is (re)initialized from `value` every time the popup opens, keyed on
            // `expanded` so a cancelled session never leaks edits into the next open.
            var pendingDate by remember(expanded) { mutableStateOf(value?.date) }
            var pendingTime by remember(expanded) { mutableStateOf(value?.time ?: LocalTime(0, 0, 0)) }
            var displayMonth by remember(expanded) { mutableStateOf(value?.date ?: todayLocalDate()) }

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
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AeroCalendarGrid(
                            displayMonth = displayMonth,
                            selected = pendingDate,
                            // CRITICAL (NEW-PICK-02): day click only mutates pending state.
                            // It must NOT close the popup and must NOT call onValueChange.
                            onDateSelected = { date -> pendingDate = date },
                            onMonthChange = { displayMonth = it },
                            isDisabled = { date ->
                                dateIsDisabled(date, minDate, maxDate, selectableDates)
                            },
                        )

                        TimeFields(
                            time = pendingTime,
                            onTimeChange = { pendingTime = it },
                            showSeconds = showSeconds,
                            minuteStep = minuteStep,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AeroOutlinedButton(
                                text = "Cancel",
                                onClick = { expanded = false },
                            )
                            AeroButton(
                                text = "Apply",
                                enabled = pendingDate != null,
                                onClick = {
                                    val d = pendingDate
                                    if (d != null) {
                                        onValueChange(combineDateTime(d, pendingTime))
                                        expanded = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * Merges a [LocalDate] and a [LocalTime] into a single [LocalDateTime]. This is the commit-gate
 * seam for `AeroDateTimePicker` (PICK-04): the pending date and pending time live in separate
 * state holders and are only combined here when the user clicks **Apply**. Extracted as a pure,
 * `internal` function so the combine contract is unit-testable without driving the composable.
 */
internal fun combineDateTime(date: LocalDate, time: LocalTime): LocalDateTime =
    LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute, time.second)
