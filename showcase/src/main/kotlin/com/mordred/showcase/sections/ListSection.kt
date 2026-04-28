package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
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
import com.mordred.aero.components.list.AeroBadge
import com.mordred.aero.components.list.AeroListItem
import com.mordred.aero.theme.AeroTheme

@Composable
fun ListSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var selectedIndex by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lists & Data Display", color = colors.onBackground, style = typography.title)

        ListRow(label = "AeroListItem") {
            Column(Modifier.width(360.dp)) {
                listOf("Inbox", "Sent", "Drafts").forEachIndexed { i, item ->
                    AeroListItem(
                        text = item,
                        onClick = { selectedIndex = i },
                        selected = selectedIndex == i,
                        secondaryText = "secondary line",
                        trailingContent = { AeroBadge(text = "${i + 1}") }
                    )
                }
            }
        }
        ListRow(label = "AeroBadge") {
            AeroBadge(text = "12")
            AeroBadge(text = "NEW")
            AeroBadge(text = "!", color = colors.error, contentColor = colors.onError)
        }
    }
}

@Composable
private fun ListRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(140.dp).padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
