package com.mordred.aero.components.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Orders a (start, end) datetime range so the emitted pair satisfies start <= end by full
 * [LocalDateTime] (DTR-04). [nextRangeState] orders DATES but not times; when start date == end
 * date and startTime > endTime the pair would be chronologically inverted. Applied ONLY at the
 * Apply onClick (the sole emit site). LocalDateTime is Comparable in kotlinx-datetime 0.6.2 —
 * `<=` works directly, no Instant/timezone conversion. Pure + internal for unit testing.
 */
internal fun orderDateTimeRange(
    startDate: LocalDate,
    startTime: LocalTime,
    endDate: LocalDate,
    endTime: LocalTime,
): Pair<LocalDateTime, LocalDateTime> {
    val a = combineDateTime(startDate, startTime)
    val b = combineDateTime(endDate, endTime)
    return if (a <= b) a to b else b to a
}

/**
 * Public date+time range picker (DTR-01..08): a read-only trigger field rendering
 * `DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM` (or `…HH:MM:SS…` when [showSeconds]) with a calendar
 * icon button that opens a popup hosting TWO side-by-side calendar months above two labeled time
 * rows ("Start" / "End"), plus explicit **Cancel** / **Apply** commit buttons.
 *
 * The CRITICAL commit-gate behavior (DTR-02, PITFALL-E): clicking a day updates selection state
 * but does NOT close the popup and does NOT fire [onRangeSelect]. The time spinners likewise only
 * mutate pending state. [onRangeSelect] fires with the ordered `(start, end)` [LocalDateTime] pair
 * ONLY when the user clicks **Apply** AND the date range is fully selected (both endpoints chosen).
 * **Apply** is disabled and grayed until [AeroDateRangeState.Selected] is reached. **Cancel**
 * dismisses silently. Reopening the popup resets all pending state to the last committed values
 * (DTR-07, PITFALL-G: no cross-open leaks).
 *
 * Same-day ranges (start date == end date) are fully supported. When both dates are the same but
 * startTime > endTime, [orderDateTimeRange] silently swaps the pair so `start <= end` always holds
 * (DTR-04). No error UI — consistent with the date-swap already inside [nextRangeState].
 *
 * Both time rows render unconditionally for stable popup height from frame 1, but remain
 * `enabled = false` until [AeroDateRangeState.Selected] is reached (DTR-08, PITFALL-I). The
 * [showSeconds] and [minuteStep] params apply equally to both rows (DTR-05).
 *
 * The default trigger formatter inherits [formatAeroDateTime] from Fix A (DTR-06, PITFALL-H
 * prevention). A caller-supplied [formatter] overrides everything.
 *
 * The popup anchors via [AeroCalendarPositionProvider] inside [PickerPopupContainer] (W11-02 glass
 * surface), uses `Popup` (never `Dialog`), and never sets the transparency flag (W11-01).
 *
 * @param startValue the committed range start, or `null` for none.
 * @param endValue the committed range end, or `null` for none.
 * @param onRangeSelect fires with the ordered `(start, end)` ONLY when **Apply** is clicked with a
 *   fully selected range.
 * @param modifier applied to the trigger.
 * @param formatter renders each endpoint in the trigger; `null` (default) uses `DD.MM.YYYY HH:MM`
 *   (or `HH:MM:SS` when [showSeconds]).
 * @param placeholder shown when no range is committed.
 * @param clearable when `true` and both endpoints are set, shows an X button that calls [onClear].
 * @param onClear invoked by the clear button.
 * @param minDate inclusive lower bound; earlier dates are disabled in the grids.
 * @param maxDate inclusive upper bound; later dates are disabled in the grids.
 * @param selectableDates extra per-date predicate; `false` disables the date.
 * @param enabled when `false`, the trigger does not open the popup.
 * @param showSeconds when `true`, both time rows expose a seconds spinner.
 * @param minuteStep step size for the minute spinners on both rows.
 */
@Composable
public fun AeroDateTimeRangePicker(
    startValue: LocalDateTime?,
    endValue: LocalDateTime?,
    onRangeSelect: (start: LocalDateTime, end: LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: ((LocalDateTime) -> String)? = null,
    placeholder: String = "Select date & time range",
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
    val displayText = if (startValue != null && endValue != null) {
        val fmt: (LocalDateTime) -> String = formatter ?: { formatAeroDateTime(it, showSeconds) }
        "${fmt(startValue)} → ${fmt(endValue)}"
    } else {
        ""
    }

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
                    if (clearable && startValue != null && endValue != null) {
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
                            contentDescription = "Open date & time range picker",
                            tint = AeroTheme.colors.onSurface,
                        )
                    }
                }
            },
        )

        if (expanded) {
            // Pending state is (re)initialized from committed values every time the popup opens,
            // keyed on `expanded` so a cancelled/click-outside session never leaks into the next
            // open (DTR-07, PITFALL-G).
            var rangeState by remember(expanded) {
                mutableStateOf<AeroDateRangeState>(
                    if (startValue != null && endValue != null) {
                        AeroDateRangeState.Selected(startValue.date, endValue.date)
                    } else {
                        AeroDateRangeState.Idle
                    },
                )
            }
            var leftMonth by remember(expanded) { mutableStateOf(startValue?.date ?: todayLocalDate()) }
            var pendingStartTime by remember(expanded) { mutableStateOf(startValue?.time ?: LocalTime(0, 0, 0)) }
            var pendingEndTime by remember(expanded) { mutableStateOf(endValue?.time ?: LocalTime(0, 0, 0)) }

            // Derive highlight endpoints: while picking end, both collapse to start for a single-
            // point highlight; Idle shows nothing.
            val (hlStart, hlEnd) = when (val s = rangeState) {
                is AeroDateRangeState.Selected -> s.start to s.end
                is AeroDateRangeState.SelectingEnd -> s.start to s.start
                AeroDateRangeState.Idle -> null to null
            }

            // Apply gate (DTR-02, PITFALL-E — CRITICAL): day clicks update selection state ONLY.
            // The commit pair is intentionally discarded — no onRangeSelect here, no expanded=false.
            val onDayClick: (LocalDate) -> Unit = { date ->
                val (next, _) = nextRangeState(rangeState, date)
                rangeState = next
            }

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
                        // Dual-calendar responsive branch: side-by-side on wide screens,
                        // stacked vertically on narrow (< 560dp) screens (NEW-PICK-03).
                        BoxWithConstraints {
                            val rightMonth = leftMonth.plus(1, DateTimeUnit.MONTH)
                            val leftCalendar: @Composable () -> Unit = {
                                AeroCalendarGrid(
                                    displayMonth = leftMonth,
                                    selected = null,
                                    rangeStart = hlStart,
                                    rangeEnd = hlEnd,
                                    onDateSelected = onDayClick,
                                    onMonthChange = { leftMonth = it },
                                    isDisabled = { date ->
                                        dateIsDisabled(date, minDate, maxDate, selectableDates)
                                    },
                                )
                            }
                            val rightCalendar: @Composable () -> Unit = {
                                AeroCalendarGrid(
                                    displayMonth = rightMonth,
                                    selected = null,
                                    rangeStart = hlStart,
                                    rangeEnd = hlEnd,
                                    onDateSelected = onDayClick,
                                    onMonthChange = { leftMonth = it.minus(1, DateTimeUnit.MONTH) },
                                    isDisabled = { date ->
                                        dateIsDisabled(date, minDate, maxDate, selectableDates)
                                    },
                                )
                            }

                            if (maxWidth < 560.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    leftCalendar()
                                    rightCalendar()
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    leftCalendar()
                                    rightCalendar()
                                }
                            }
                        }

                        // Start time row — unconditional (DTR-08, PITFALL-I: stable popup height).
                        // enabled = false until date range is fully Selected (DTR-05).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Start",
                                style = AeroTheme.typography.bodyMedium,
                                color = AeroTheme.colors.labelText,
                            )
                            TimeFields(
                                time = pendingStartTime,
                                onTimeChange = { pendingStartTime = it },
                                showSeconds = showSeconds,
                                minuteStep = minuteStep,
                                enabled = rangeState is AeroDateRangeState.Selected,
                            )
                        }

                        // End time row — unconditional (DTR-08, PITFALL-I: stable popup height).
                        // enabled = false until date range is fully Selected (DTR-05).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "End",
                                style = AeroTheme.typography.bodyMedium,
                                color = AeroTheme.colors.labelText,
                            )
                            TimeFields(
                                time = pendingEndTime,
                                onTimeChange = { pendingEndTime = it },
                                showSeconds = showSeconds,
                                minuteStep = minuteStep,
                                enabled = rangeState is AeroDateRangeState.Selected,
                            )
                        }

                        // Commit row: Cancel dismisses silently; Apply is the SOLE onRangeSelect
                        // call site (DTR-03), guarded by rangeState is Selected. orderDateTimeRange
                        // ensures start <= end by full LocalDateTime before emitting (DTR-04).
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
                                enabled = rangeState is AeroDateRangeState.Selected,
                                onClick = {
                                    val s = rangeState
                                    if (s is AeroDateRangeState.Selected) {
                                        val (sdt, edt) = orderDateTimeRange(s.start, pendingStartTime, s.end, pendingEndTime)
                                        onRangeSelect(sdt, edt)
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
