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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Selection state machine for [AeroDateRangePicker] (PICK-02). A sealed type makes the three
 * legal states explicit and lets [nextRangeState] decide — in one place — when a completed range
 * may be emitted. This is the crux of PITFALL-06: `onRangeSelect` must fire EXACTLY once per
 * completed range and NEVER after only a start click.
 *
 *  - [Idle]:         nothing selected yet.
 *  - [SelectingEnd]: a start date is chosen; awaiting the end click.
 *  - [Selected]:     an ordered (start, end) range is committed.
 */
internal sealed interface AeroDateRangeState {
    data object Idle : AeroDateRangeState
    data class SelectingEnd(val start: LocalDate) : AeroDateRangeState
    data class Selected(val start: LocalDate, val end: LocalDate) : AeroDateRangeState
}

/**
 * Pure transition for the range selection state machine (testable without Compose).
 *
 * Returns the next [AeroDateRangeState] and, ONLY when a range is committed (the
 * [AeroDateRangeState.SelectingEnd] -> [AeroDateRangeState.Selected] transition), the ordered
 * `(start, end)` pair to emit. For every other transition the second element is `null`, which the
 * composable uses as the guard around the single `onRangeSelect(` call site (PITFALL-06):
 *
 *  - From [AeroDateRangeState.Idle] or [AeroDateRangeState.Selected]: a click begins a new range
 *    -> `SelectingEnd(clicked)`, emit `null` (NOT a completed range).
 *  - From [AeroDateRangeState.SelectingEnd]: the click completes the range. The two dates are
 *    ordered so `start <= end` regardless of click order -> `Selected(s, e)`, emit `(s, e)`.
 */
internal fun nextRangeState(
    current: AeroDateRangeState,
    clicked: LocalDate,
): Pair<AeroDateRangeState, Pair<LocalDate, LocalDate>?> = when (current) {
    is AeroDateRangeState.Idle,
    is AeroDateRangeState.Selected -> AeroDateRangeState.SelectingEnd(clicked) to null
    is AeroDateRangeState.SelectingEnd -> {
        val (s, e) = if (clicked >= current.start) current.start to clicked else clicked to current.start
        AeroDateRangeState.Selected(s, e) to (s to e)
    }
}

/**
 * Public date-range picker (PICK-02): a read-only trigger field rendering `start → end` with a
 * calendar icon button that opens a popup of TWO side-by-side calendar months (the right month is
 * always the left month + 1). On windows narrower than ~560dp (two 268dp calendars + gap) the two
 * months stack vertically (NEW-PICK-03).
 *
 * Selection is driven by the sealed [AeroDateRangeState] machine via [nextRangeState]: the first
 * day click chooses the start (no callback), and the second click completes the range. The single
 * `onRangeSelect(` call site is guarded by the non-null commit pair returned from [nextRangeState],
 * so [onRangeSelect] fires EXACTLY once per completed range and NEVER after only a start click
 * (PITFALL-06). Intermediate cells highlight via [AeroCalendarGrid]'s additive `rangeStart`/
 * `rangeEnd` params (primary@0.15f — readable on AeroDark, PITFALL-09 extension).
 *
 * The popup anchors via [AeroCalendarPositionProvider] inside [PickerPopupContainer] (W11-02 glass
 * surface), uses `Popup` (never `Dialog`), and never sets the transparency flag (W11-01).
 *
 * @param startValue the committed range start, or `null` for none.
 * @param endValue the committed range end, or `null` for none.
 * @param onRangeSelect fires with the ordered `(start, end)` ONLY when a range is completed.
 * @param modifier applied to the trigger.
 * @param formatter renders each endpoint in the trigger field (default DD.MM.YYYY).
 * @param placeholder shown when no range is set.
 * @param clearable when `true` and a range is set, shows an X button that calls [onClear].
 * @param onClear invoked by the clear button.
 * @param minDate inclusive lower bound; earlier dates are disabled in the grids.
 * @param maxDate inclusive upper bound; later dates are disabled in the grids.
 * @param selectableDates extra per-date predicate; `false` disables the date.
 * @param enabled when `false`, the trigger does not open the popup.
 */
@Composable
public fun AeroDateRangePicker(
    startValue: LocalDate?,
    endValue: LocalDate?,
    onRangeSelect: (start: LocalDate, end: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDate) -> String = { formatAeroDate(it) },
    placeholder: String = "Select range",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (startValue != null && endValue != null) {
        "${formatter(startValue)} → ${formatter(endValue)}"
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
                            contentDescription = "Open range calendar",
                            tint = AeroTheme.colors.onSurface,
                        )
                    }
                }
            },
        )

        if (expanded) {
            // Selection state is (re)initialized from the committed range every time the popup
            // opens, keyed on `expanded` so a partial (start-only) selection never leaks across
            // opens. leftMonth drives both months: the right month is always leftMonth + 1.
            var rangeState by remember(expanded) {
                mutableStateOf<AeroDateRangeState>(
                    if (startValue != null && endValue != null) {
                        AeroDateRangeState.Selected(startValue, endValue)
                    } else {
                        AeroDateRangeState.Idle
                    },
                )
            }
            var leftMonth by remember(expanded) { mutableStateOf(startValue ?: todayLocalDate()) }

            // Derive the highlight endpoints from the live selection state: while picking the end,
            // both endpoints collapse to the start so only the start endpoint shows.
            val (hlStart, hlEnd) = when (val s = rangeState) {
                is AeroDateRangeState.Selected -> s.start to s.end
                is AeroDateRangeState.SelectingEnd -> s.start to s.start
                AeroDateRangeState.Idle -> null to null
            }

            // CRITICAL (PITFALL-06): the SOLE onRangeSelect( call site, guarded by a non-null
            // commit. A first (start) click yields commit == null and emits nothing.
            val onDayClick: (LocalDate) -> Unit = { date ->
                val (next, commit) = nextRangeState(rangeState, date)
                rangeState = next
                if (commit != null) {
                    onRangeSelect(commit.first, commit.second)
                    expanded = false
                }
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
                    BoxWithConstraints(modifier = Modifier.padding(8.dp)) {
                        val rightMonth = leftMonth.plus(1, DateTimeUnit.MONTH)
                        val leftCalendar: @Composable () -> Unit = {
                            AeroCalendarGrid(
                                displayMonth = leftMonth,
                                selected = null,
                                rangeStart = hlStart,
                                rangeEnd = hlEnd,
                                onDateSelected = onDayClick,
                                // Keep both months in lockstep: leftMonth drives, rightMonth derives.
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
                                // Navigating the right month shifts the shared window by deriving left.
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
                }
            }
        }
    }
}

private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
