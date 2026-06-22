package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.pickers.AeroColorPickerButton
import com.mordred.aero.components.pickers.AeroDatePicker
import com.mordred.aero.components.pickers.AeroDateRangePicker
import com.mordred.aero.components.pickers.AeroDateTimePicker
import com.mordred.aero.components.pickers.AeroDateTimeRangePicker
import com.mordred.aero.components.pickers.AeroTimePicker
import com.mordred.aero.components.range.AeroRangeSlider
import com.mordred.aero.theme.AeroTheme
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Composable
fun PickersSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var dateValue by remember { mutableStateOf<LocalDate?>(null) }
    var timeValue by remember { mutableStateOf<LocalTime?>(null) }
    var dateTimeValue by remember { mutableStateOf<LocalDateTime?>(null) }
    var rangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var rangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    var colorValue by remember { mutableStateOf(Color(0xFF4FC3F7)) }
    var sliderRange by remember { mutableStateOf(0.2f..0.7f) }
    var dtRangeStart by remember { mutableStateOf<LocalDateTime?>(null) }
    var dtRangeEnd by remember { mutableStateOf<LocalDateTime?>(null) }
    var dtSecondsValue by remember { mutableStateOf<LocalDateTime?>(null) }

    // F13: disabled-date demo bounds — only June 2026 selectable
    val minD = LocalDate(2026, 6, 1)
    val maxD = LocalDate(2026, 6, 30)
    var boundedDate by remember { mutableStateOf<LocalDate?>(null) }
    var bStart by remember { mutableStateOf<LocalDate?>(null) }
    var bEnd by remember { mutableStateOf<LocalDate?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pickers", color = colors.onBackground, style = typography.title)

        // F8: compact trigger width (220.dp); F6: DD.MM.YYYY value preview
        RangeRow(label = "AeroDatePicker") {
            AeroDatePicker(
                value = dateValue,
                onValueChange = { dateValue = it },
                modifier = Modifier.width(220.dp)
            )
            Text(
                text = dateValue?.let { "%02d.%02d.%04d".format(it.dayOfMonth, it.monthNumber, it.year) } ?: "—",
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }

        // F8: compact trigger width (220.dp); F7: showSeconds = true
        RangeRow(label = "AeroTimePicker") {
            AeroTimePicker(
                value = timeValue,
                onValueChange = { timeValue = it },
                showSeconds = true,
                modifier = Modifier.width(220.dp)
            )
            Text(timeValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        // F8: compact trigger width (220.dp); F7: showSeconds = true; F6: value text raw for HH:MM:SS
        RangeRow(label = "AeroDateTimePicker (showSeconds=true)") {
            AeroDateTimePicker(
                value = dateTimeValue,
                onValueChange = { dateTimeValue = it },
                showSeconds = true,
                modifier = Modifier.width(220.dp)
            )
            Text(dateTimeValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        RangeRow(label = "AeroDateTimePicker (showSeconds=false)") {
            AeroDateTimePicker(
                value = dtSecondsValue,
                onValueChange = { dtSecondsValue = it },
                showSeconds = false,
                modifier = Modifier.width(220.dp),
            )
            Text(dtSecondsValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        // F8: compact trigger width (220.dp); F6: DD.MM.YYYY range value preview
        RangeRow(label = "AeroDateRangePicker") {
            AeroDateRangePicker(
                startValue = rangeStart,
                endValue = rangeEnd,
                onRangeSelect = { start, end -> rangeStart = start; rangeEnd = end },
                modifier = Modifier.width(220.dp)
            )
            Text(
                text = if (rangeStart != null && rangeEnd != null)
                    "${"%02d.%02d.%04d".format(rangeStart!!.dayOfMonth, rangeStart!!.monthNumber, rangeStart!!.year)} → ${"%02d.%02d.%04d".format(rangeEnd!!.dayOfMonth, rangeEnd!!.monthNumber, rangeEnd!!.year)}"
                else "—",
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }

        RangeRow(label = "AeroDateTimeRangePicker") {
            AeroDateTimeRangePicker(
                startValue = dtRangeStart,
                endValue = dtRangeEnd,
                onRangeSelect = { start, end -> dtRangeStart = start; dtRangeEnd = end },
                showSeconds = true,
                modifier = Modifier.width(280.dp),
            )
            Text(
                text = if (dtRangeStart != null && dtRangeEnd != null)
                    "${dtRangeStart} → ${dtRangeEnd}"
                else "—",
                color = colors.labelText,
                style = typography.bodyMedium,
            )
        }

        // F13: DatePicker with min/max — only June 2026 selectable; disabled cells visible for AeroDark check (PITFALL-09)
        RangeRow(label = "DatePicker (min/max)") {
            AeroDatePicker(
                value = boundedDate,
                onValueChange = { boundedDate = it },
                minDate = minD,
                maxDate = maxD,
                modifier = Modifier.width(220.dp)
            )
            Text(
                text = boundedDate?.let { "%02d.%02d.%04d".format(it.dayOfMonth, it.monthNumber, it.year) } ?: "—",
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }

        // F13: DateRangePicker with min/max — only June 2026 selectable; disabled cells visible for AeroDark check (PITFALL-09)
        RangeRow(label = "RangePicker (min/max)") {
            AeroDateRangePicker(
                startValue = bStart,
                endValue = bEnd,
                onRangeSelect = { s, e -> bStart = s; bEnd = e },
                minDate = minD,
                maxDate = maxD,
                modifier = Modifier.width(220.dp)
            )
            Text(
                text = if (bStart != null && bEnd != null)
                    "${"%02d.%02d.%04d".format(bStart!!.dayOfMonth, bStart!!.monthNumber, bStart!!.year)} → ${"%02d.%02d.%04d".format(bEnd!!.dayOfMonth, bEnd!!.monthNumber, bEnd!!.year)}"
                else "—",
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }

        RangeRow(label = "AeroColorPicker") {
            AeroColorPickerButton(value = colorValue, onValueChange = { colorValue = it })
            Text(
                text = "#%02X%02X%02X".format(
                    (colorValue.red * 255).roundToInt(),
                    (colorValue.green * 255).roundToInt(),
                    (colorValue.blue * 255).roundToInt()
                ),
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }

        RangeRow(label = "AeroRangeSlider") {
            AeroRangeSlider(value = sliderRange, onValueChange = { sliderRange = it }, modifier = Modifier.width(240.dp))
            Text("${"%.2f".format(sliderRange.start)} → ${"%.2f".format(sliderRange.endInclusive)}", color = colors.labelText, style = typography.bodyMedium)
        }
    }
}

@Composable
private fun RangeRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(160.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
