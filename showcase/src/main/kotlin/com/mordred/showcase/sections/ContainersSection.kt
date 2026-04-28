package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.containers.AeroCard
import com.mordred.aero.components.containers.AeroDivider
import com.mordred.aero.components.containers.AeroGroupBox
import com.mordred.aero.components.containers.AeroPanel
import com.mordred.aero.components.containers.AeroScrollArea
import com.mordred.aero.theme.AeroTheme

/**
 * Phase 3 — Containers section: CNT-01 AeroCard, CNT-02 AeroPanel, CNT-03 AeroDivider,
 * CNT-04 AeroGroupBox, CNT-05 AeroScrollArea, CNT-06 AeroScrollBar.
 *
 * CNT-06 AeroScrollBar is implicitly demonstrated by AeroScrollArea (and the
 * Phase 2 AeroTextArea retrofit completed in Plan 03-01).
 */
@Composable
fun ContainersSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Containers", color = colors.onBackground, style = typography.title)

        SectionRow(label = "AeroCard") {
            AeroCard(modifier = Modifier.size(width = 200.dp, height = 80.dp)) {
                Text("Card content", color = colors.onSurface, style = typography.bodyLarge)
            }
        }

        SectionRow(label = "AeroPanel") {
            AeroPanel(modifier = Modifier.size(width = 200.dp, height = 60.dp)) {
                Text("Panel content", color = colors.onSurface, style = typography.bodyLarge)
            }
        }

        SectionRow(label = "AeroDivider") {
            Column(modifier = Modifier.width(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("above", color = colors.onSurface, style = typography.bodyMedium)
                AeroDivider()
                Text("below", color = colors.onSurface, style = typography.bodyMedium)
            }
            Spacer(Modifier.width(16.dp))
            Row(modifier = Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("L", color = colors.onSurface, style = typography.bodyMedium)
                AeroDivider(vertical = true, modifier = Modifier.height(40.dp))
                Text("R", color = colors.onSurface, style = typography.bodyMedium)
            }
        }

        SectionRow(label = "AeroGroupBox") {
            AeroGroupBox(label = "Settings", modifier = Modifier.width(220.dp)) {
                Text("Group content", color = colors.onSurface, style = typography.bodyLarge)
            }
        }

        SectionRow(label = "AeroScrollArea") {
            AeroScrollArea(modifier = Modifier.size(width = 200.dp, height = 80.dp)) {
                repeat(20) { i ->
                    Text("Line $i", color = colors.onSurface, style = typography.bodyLarge)
                }
            }
        }

        // CNT-06 AeroScrollBar is implicitly demonstrated by AeroScrollArea (and by AeroTextArea
        // in InputSection — Phase 2 retrofit completed in Plan 03-01). No standalone row needed.
    }
}

@Composable
private fun SectionRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
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
            verticalAlignment = Alignment.Top,
            content = content
        )
    }
}
