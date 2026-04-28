package com.mordred.showcase.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.components.selection.*

@Composable
fun SelectionSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    var checked by remember { mutableStateOf(false) }
    var triState by remember { mutableStateOf(ToggleableState.Indeterminate) }
    var radio by remember { mutableStateOf("Option A") }
    var switched by remember { mutableStateOf(true) }
    var chipSelected by remember { mutableStateOf(false) }
    var segValue by remember { mutableStateOf("Day") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Selection", color = colors.onBackground, style = typography.title)

        SelRow(label = "AeroCheckbox") {
            AeroCheckbox(checked = checked, onCheckedChange = { checked = it }, label = "Enabled")
            AeroCheckbox(checked = true, onCheckedChange = {}, enabled = false, label = "Disabled")
            AeroTriStateCheckbox(state = triState, onClick = {
                triState = when (triState) { ToggleableState.Off -> ToggleableState.On; ToggleableState.On -> ToggleableState.Indeterminate; else -> ToggleableState.Off }
            }, label = "Tri-state")
        }
        SelRow(label = "AeroRadioGroup") {
            AeroRadioGroup(options = listOf("Option A", "Option B", "Option C"), selected = radio, onSelect = { radio = it })
        }
        SelRow(label = "AeroSwitch") {
            AeroSwitch(checked = switched, onCheckedChange = { switched = it })
            AeroSwitch(checked = false, onCheckedChange = {}, enabled = false)
        }
        SelRow(label = "AeroChip") {
            AeroChip(label = "Filter", selected = chipSelected, onClick = { chipSelected = !chipSelected })
            AeroChip(label = "Disabled", selected = true, onClick = {}, enabled = false)
        }
        SelRow(label = "AeroSegmentedControl") {
            AeroSegmentedControl(options = listOf("Day", "Week", "Month"), selected = segValue, onSelect = { segValue = it })
        }
    }
}

@Composable
private fun SelRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(140.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
