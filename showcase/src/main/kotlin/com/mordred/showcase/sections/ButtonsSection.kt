package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.buttons.AeroToolbar
import com.mordred.aero.components.buttons.AeroToolbarDefaults
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.CaretDown
import com.mordred.aero.icons.`internal`.CaretUp
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.theme.AeroTheme

/**
 * Phase 2 — Buttons section for the Aero showcase.
 *
 * Displays all four button variants (BTN-01..04) in a table-like layout:
 * fixed 140.dp label column on the left, variant row on the right.
 *
 * Layout follows the SectionRow pattern defined in [SectionRow]:
 * each row shows an enabled + disabled (or grouped) example.
 */
@Composable
fun ButtonsSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Buttons", color = colors.onBackground, style = typography.title)
        Text(
            text = "Hover, press, and focus are transient — reviewed live across all three themes " +
                "via the theme switcher above: mouse-over for hover, click-and-hold for press, Tab to focus.",
            color = colors.labelText,
            style = typography.bodySmall,
        )

        // Row 1: AeroButton (BTN-01 / VBTN-01..06) — enabled, disabled, long-label truncation
        SectionRow(label = "AeroButton") {
            AeroButton(text = "Save Changes", onClick = {})
            AeroButton(text = "Disabled", onClick = {}, enabled = false)
            AeroButton(
                text = "Save All Changes and Close This Dialog Window",
                onClick = {},
                modifier = Modifier.width(120.dp),
            )
        }

        // Row 2: AeroOutlinedButton (BTN-02) — enabled, disabled, long-label truncation
        SectionRow(label = "AeroOutlinedButton") {
            AeroOutlinedButton(text = "Cancel", onClick = {})
            AeroOutlinedButton(text = "Disabled", onClick = {}, enabled = false)
            AeroOutlinedButton(
                text = "Cancel All Pending Operations Immediately",
                onClick = {},
                modifier = Modifier.width(120.dp),
            )
        }

        // Row 3: AeroIconButton (BTN-03) — three icon buttons, one disabled
        SectionRow(label = "AeroIconButton") {
            AeroIconButton(onClick = {}) {
                Icon(AeroIcons.CaretUp, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
            }
            AeroIconButton(onClick = {}) {
                Icon(AeroIcons.CaretDown, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
            }
            AeroIconButton(onClick = {}, enabled = false) {
                Icon(AeroIcons.X, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
            }
        }

        // Row 4: AeroToolbar (BTN-04) — grouped icon buttons with a divider
        SectionRow(label = "AeroToolbar") {
            AeroToolbar {
                AeroIconButton(onClick = {}) { Text("B", color = colors.onSurface) }
                AeroIconButton(onClick = {}) { Text("I", color = colors.onSurface) }
                AeroToolbarDefaults.Divider()
                AeroIconButton(onClick = {}) { Text("U", color = colors.onSurface) }
                AeroIconButton(onClick = {}) { Text("S", color = colors.onSurface) }
            }
        }
    }
}

@Composable
private fun SectionRow(label: String, content: @Composable RowScope.() -> Unit) {
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
