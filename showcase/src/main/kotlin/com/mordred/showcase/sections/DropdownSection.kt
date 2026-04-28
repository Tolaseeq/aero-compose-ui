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
import com.mordred.aero.components.dropdown.AeroComboBox
import com.mordred.aero.components.dropdown.AeroDropdown
import com.mordred.aero.theme.AeroTheme

@Composable
fun DropdownSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val countries = listOf("Russia", "United States", "United Kingdom", "Germany", "France", "Japan")
    var dropdownValue by remember { mutableStateOf<String?>(null) }
    var comboText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Dropdown", color = colors.onBackground, style = typography.title)

        DropdownRow(label = "AeroDropdown") {
            AeroDropdown(
                options = countries,
                selected = dropdownValue,
                onSelect = { dropdownValue = it },
                placeholder = "Select country..."
            )
            Text("= ${dropdownValue ?: "(none)"}", color = colors.labelText, style = typography.bodyMedium)
        }
        DropdownRow(label = "AeroComboBox") {
            AeroComboBox(
                text = comboText,
                onTextChange = { comboText = it },
                options = countries,
                onOptionSelect = { comboText = it }
            )
            Text("= $comboText", color = colors.labelText, style = typography.bodyMedium)
        }
    }
}

@Composable
private fun DropdownRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = AeroTheme.colors.labelText,
            style = AeroTheme.typography.bodyMedium,
            modifier = Modifier.width(140.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
