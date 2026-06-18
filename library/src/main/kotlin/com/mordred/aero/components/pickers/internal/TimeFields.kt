package com.mordred.aero.components.pickers.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.input.AeroNumberSpinner
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.LocalTime

/**
 * Internal hour / minute (/ optional second) spinner row shared by [com.mordred.aero.components.pickers.AeroTimePicker]
 * (PICK-03) and `AeroDateTimePicker` (plan 04). 24-hour only — there is no 12h / AM-PM mode.
 *
 * Each field is an [AeroNumberSpinner] clamped to its valid range (hour 0..23, minute/second 0..59).
 * The spinner callbacks are passed straight through to [onTimeChange] with no debounce / transform —
 * wrapping them desyncs the spinner's `remember(value)` text state (NEW-PICK-04).
 *
 * @param time the current time displayed across the spinners.
 * @param onTimeChange fires with the reassembled [LocalTime] whenever any field changes.
 * @param modifier applied to the outer [Row].
 * @param enabled when `false`, all spinners are non-interactive.
 * @param showSeconds when `true`, a third (seconds) spinner is shown; when `false` the emitted
 *   [LocalTime] always has `second == 0`.
 * @param minuteStep step size for the minute spinner.
 */
@Composable
internal fun TimeFields(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSeconds: Boolean = false,
    minuteStep: Int = 1,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AeroNumberSpinner(
            value = time.hour,
            onValueChange = { h -> onTimeChange(assembleTime(h, time.minute, time.second, showSeconds)) },
            min = 0,
            max = 23,
            step = 1,
            enabled = enabled,
        )

        Text(
            text = ":",
            style = AeroTheme.typography.bodyLarge,
            color = AeroTheme.colors.onSurface,
        )

        AeroNumberSpinner(
            value = time.minute,
            onValueChange = { m -> onTimeChange(assembleTime(time.hour, m, time.second, showSeconds)) },
            min = 0,
            max = 59,
            step = minuteStep,
            enabled = enabled,
        )

        if (showSeconds) {
            Text(
                text = ":",
                style = AeroTheme.typography.bodyLarge,
                color = AeroTheme.colors.onSurface,
            )

            AeroNumberSpinner(
                value = time.second,
                onValueChange = { s -> onTimeChange(assembleTime(time.hour, time.minute, s, true)) },
                min = 0,
                max = 59,
                step = 1,
                enabled = enabled,
            )
        }
    }
}

/**
 * Reassembles a [LocalTime] from raw spinner values, clamping each component to its valid range
 * (hour 0..23, minute 0..59, second 0..59). When [showSeconds] is `false` the result always has
 * `second == 0`. Pure and `internal` so [AeroTimePicker]'s emission contract is unit-testable.
 */
internal fun assembleTime(hour: Int, minute: Int, second: Int, showSeconds: Boolean): LocalTime =
    LocalTime(
        hour.coerceIn(0, 23),
        minute.coerceIn(0, 59),
        if (showSeconds) second.coerceIn(0, 59) else 0,
    )
