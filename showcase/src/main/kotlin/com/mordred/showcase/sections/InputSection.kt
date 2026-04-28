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
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.components.input.*

@Composable
fun InputSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    var textValue by remember { mutableStateOf("") }
    var areaValue by remember { mutableStateOf("Multi-line\ntext\nhere") }
    var passwordValue by remember { mutableStateOf("") }
    var spinnerValue by remember { mutableStateOf(50) }
    var searchValue by remember { mutableStateOf("") }
    var pathValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Input", color = colors.onBackground, style = typography.title)

        InputRow(label = "AeroTextField") {
            AeroTextField(value = textValue, onValueChange = { textValue = it }, placeholder = "Type here", modifier = Modifier.width(200.dp))
            AeroTextField(value = "Disabled", onValueChange = {}, enabled = false, modifier = Modifier.width(120.dp))
        }
        InputRow(label = "AeroTextArea") {
            AeroTextArea(value = areaValue, onValueChange = { areaValue = it }, placeholder = "Multi-line", modifier = Modifier.width(280.dp), minHeight = 60.dp, maxHeight = 100.dp)
        }
        InputRow(label = "AeroPasswordField") {
            AeroPasswordField(value = passwordValue, onValueChange = { passwordValue = it }, modifier = Modifier.width(200.dp))
        }
        InputRow(label = "AeroNumberSpinner") {
            AeroNumberSpinner(value = spinnerValue, onValueChange = { spinnerValue = it }, min = 0, max = 100, step = 5)
            Text("= $spinnerValue", color = colors.labelText, style = typography.bodyMedium)
        }
        InputRow(label = "AeroSearchField") {
            AeroSearchField(value = searchValue, onValueChange = { searchValue = it }, modifier = Modifier.width(240.dp))
        }
        InputRow(label = "AeroFilePicker") {
            AeroFilePicker(path = pathValue, onPathChange = { pathValue = it }, modifier = Modifier.width(360.dp))
        }
    }
}

@Composable
private fun InputRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = AeroTheme.colors.labelText,
            style = AeroTheme.typography.bodyMedium,
            modifier = Modifier.width(140.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
