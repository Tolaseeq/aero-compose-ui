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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pickers", color = colors.onBackground, style = typography.title)

        RangeRow(label = "AeroDatePicker") {
            AeroDatePicker(value = dateValue, onValueChange = { dateValue = it })
            Text(dateValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        RangeRow(label = "AeroTimePicker") {
            AeroTimePicker(value = timeValue, onValueChange = { timeValue = it })
            Text(timeValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        RangeRow(label = "AeroDateTimePicker") {
            AeroDateTimePicker(value = dateTimeValue, onValueChange = { dateTimeValue = it })
            Text(dateTimeValue?.toString() ?: "—", color = colors.labelText, style = typography.bodyMedium)
        }

        RangeRow(label = "AeroDateRangePicker") {
            AeroDateRangePicker(
                startValue = rangeStart,
                endValue = rangeEnd,
                onRangeSelect = { start, end -> rangeStart = start; rangeEnd = end }
            )
            Text(
                text = if (rangeStart != null && rangeEnd != null) "$rangeStart → $rangeEnd" else "—",
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
