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
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.range.AeroProgressBar
import com.mordred.aero.components.range.AeroSlider
import com.mordred.aero.theme.AeroTheme

@Composable
fun RangeSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var sliderValue by remember { mutableStateOf(0.5f) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Range & Progress", color = colors.onBackground, style = typography.title)

        RangeRow(label = "AeroSlider") {
            AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = Modifier.width(240.dp))
            Text("= ${"%.2f".format(sliderValue)}", color = colors.labelText, style = typography.bodyMedium)
        }
        RangeRow(label = "AeroProgressBar (det)") {
            AeroProgressBar(progress = sliderValue, modifier = Modifier.width(240.dp))
        }
        RangeRow(label = "AeroProgressBar (ind)") {
            AeroProgressBar(modifier = Modifier.width(240.dp))
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
