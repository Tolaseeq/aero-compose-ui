package com.mordred.aero.components.pickers.internal.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretLeft
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Internal month-grid composable for date pickers. Renders:
 *   - header row: prev-month button + month/year label + next-month button
 *   - day-of-week row: Mo Tu We Th Fr Sa Su (English short labels; i18n deferred)
 *   - 6x7 day-cell grid: filled with the days of `displayMonth.month`,
 *     leading/trailing slots are blank (Phase 8 may opt to show adjacent-month days).
 *
 * Consumed by AeroDatePicker, AeroDateRangePicker, AeroDateTimePicker (all Phase 8).
 *
 * @param displayMonth any `LocalDate` whose month/year drive the grid (day-of-month is ignored).
 * @param selected the currently selected date, or `null` for none.
 * @param onDateSelected fires when the user clicks a day cell.
 * @param onMonthChange fires when the user clicks prev/next month — receives the new displayMonth.
 * @param isDisabled returns `true` for dates that should be non-interactive and dimmed.
 */
@Composable
internal fun AeroCalendarGrid(
    displayMonth: LocalDate,
    selected: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: (LocalDate) -> Boolean = { false },
) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    // First day of the displayed month, plus its day-of-week (Mon=0..Sun=6).
    val firstOfMonth = LocalDate(displayMonth.year, displayMonth.monthNumber, 1)
    val firstDow = (firstOfMonth.dayOfWeek.isoDayNumber - 1) // ISO: Mon=1..Sun=7 -> 0..6
    val daysInMonth = daysInMonth(displayMonth)

    Column(modifier = modifier.wrapContentWidth().padding(8.dp)) {

        // --- Header row: prev / month-year label / next ---
        Row(
            modifier = Modifier.width(252.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onMonthChange(displayMonth.minus(1, DateTimeUnit.MONTH)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AeroIcons.CaretLeft,
                    contentDescription = "Previous month",
                    tint = colors.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = "${displayMonth.month.englishName()} ${displayMonth.year}",
                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onMonthChange(displayMonth.plus(1, DateTimeUnit.MONTH)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AeroIcons.CaretRight,
                    contentDescription = "Next month",
                    tint = colors.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.size(4.dp))

        // --- Day-of-week header (Mon..Sun, English short) ---
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = typography.label,
                        color = colors.labelText,
                    )
                }
            }
        }

        // --- 6x7 day-cell grid ---
        // We fill exactly 42 slots: leading prev-month days, this-month days, trailing next-month days.
        var dayCounter = 1
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val slotIndex = row * 7 + col
                    val isLeadingBlank = slotIndex < firstDow
                    val isTrailingBlank = dayCounter > daysInMonth
                    if (isLeadingBlank || isTrailingBlank) {
                        // Leading slot: adjacent-month day at reduced opacity, non-interactive.
                        // For Phase 7 we keep these slots blank (Phase 8 may opt to show adjacent days).
                        Box(modifier = Modifier.size(36.dp))
                    } else {
                        val cellDate = LocalDate(displayMonth.year, displayMonth.monthNumber, dayCounter)
                        val disabled = isDisabled(cellDate)
                        val isSelected = (selected != null && selected == cellDate)
                        DayCell(
                            day = dayCounter,
                            isSelected = isSelected,
                            isDisabled = disabled,
                            onClick = { if (!disabled) onDateSelected(cellDate) },
                        )
                        dayCounter++
                    }
                }
            }
            if (dayCounter > daysInMonth && row >= 4) {
                // Stop drawing trailing rows once month is exhausted (rows 5/6 may be empty).
                break
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val bg = when {
        isSelected -> colors.primary
        else       -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        isDisabled -> colors.labelText
        isSelected -> colors.onPrimary
        else       -> colors.onSurface
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = !isDisabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = typography.bodyMedium,
            color = fg,
        )
    }
}

/**
 * Returns 28..31 for the month/year of [date]. Uses Gregorian leap-year rule directly
 * (Phase 7 ADR: avoid platform-locale dependencies). Exposed as `internal` so the
 * calendar-grid test can verify leap-year handling without driving the full composable.
 */
internal fun daysInMonth(date: LocalDate): Int {
    val isLeap = isLeapYear(date.year)
    return when (date.month) {
        Month.JANUARY,   Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST,    Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL,     Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY   -> if (isLeap) 29 else 28
    }
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun Month.englishName(): String = when (this) {
    Month.JANUARY -> "January"; Month.FEBRUARY -> "February"; Month.MARCH -> "March"
    Month.APRIL -> "April";     Month.MAY -> "May";           Month.JUNE -> "June"
    Month.JULY -> "July";       Month.AUGUST -> "August";     Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"; Month.NOVEMBER -> "November"; Month.DECEMBER -> "December"
}
