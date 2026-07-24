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
import com.mordred.aero.components.range.AeroRangeSlider
import com.mordred.aero.components.range.AeroSlider
import com.mordred.aero.theme.AeroTheme

/**
 * Phase 18 — Range section for the Aero showcase.
 *
 * Displays [AeroSlider], [AeroRangeSlider], and [AeroProgressBar] across every reviewable
 * state (VRNG-03/05/06/08): enabled + `enabled = false` (D-07 flatten-to-dead) for both sliders,
 * and determinate (default gloss + opt-in `showRunningSheen`) + indeterminate for the progress
 * bar. Hover, focus, and drag are transient — reviewed live across all three themes via the
 * theme switcher above the section list, never forced via static styling.
 */
@Composable
fun RangeSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var sliderValue by remember { mutableStateOf(0.5f) }
    var rangeValue by remember { mutableStateOf(0.2f..0.7f) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Range & Progress", color = colors.onBackground, style = typography.title)
        Text(
            text = "Hover, focus, and drag are transient — reviewed live across all three themes " +
                "via the theme switcher above: mouse-over a thumb for hover, click-and-drag for " +
                "press, Tab to focus.",
            color = colors.labelText,
            style = typography.bodySmall,
        )

        // AeroSlider (VRNG-01/03) — enabled + disabled (D-07 flatten-to-dead)
        RangeRow(label = "AeroSlider") {
            AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = Modifier.width(200.dp))
            Text("= ${"%.2f".format(sliderValue)}", color = colors.labelText, style = typography.bodyMedium)
        }
        RangeRow(label = "AeroSlider (disabled)") {
            AeroSlider(
                value = sliderValue,
                onValueChange = {},
                modifier = Modifier.width(200.dp),
                enabled = false
            )
        }

        // AeroRangeSlider (VRNG-04/05) — enabled + disabled (D-07 flatten-to-dead), independent
        // per-thumb hover/press/focus reviewed live via the theme switcher.
        RangeRow(label = "AeroRangeSlider") {
            AeroRangeSlider(
                value = rangeValue,
                onValueChange = { rangeValue = it },
                modifier = Modifier.width(200.dp)
            )
            Text(
                text = "${"%.2f".format(rangeValue.start)} → ${"%.2f".format(rangeValue.endInclusive)}",
                color = colors.labelText,
                style = typography.bodyMedium
            )
        }
        RangeRow(label = "AeroRangeSlider (disabled)") {
            AeroRangeSlider(
                value = rangeValue,
                onValueChange = {},
                modifier = Modifier.width(200.dp),
                enabled = false
            )
        }

        // AeroProgressBar determinate (VRNG-06) — default static gloss, then opt-in
        // showRunningSheen=true traveling highlight layered on top of it (VRNG-07, D-05).
        // No disabled variant: AeroProgressBar has no `enabled` parameter — the UI-SPEC's
        // Disabled row scopes to AeroSlider/AeroRangeSlider only (confirmed 18-03-SUMMARY.md),
        // and this plan's own prohibitions forbid adding new public API beyond showRunningSheen.
        RangeRow(label = "AeroProgressBar (det)") {
            AeroProgressBar(progress = sliderValue, modifier = Modifier.width(200.dp))
        }
        RangeRow(label = "AeroProgressBar (det, sheen)") {
            AeroProgressBar(progress = sliderValue, modifier = Modifier.width(200.dp), showRunningSheen = true)
        }

        // AeroProgressBar indeterminate (VRNG-08) — single accent-glass sweep, 1500ms restart.
        RangeRow(label = "AeroProgressBar (ind)") {
            AeroProgressBar(modifier = Modifier.width(200.dp))
        }
    }
}

@Composable
private fun RangeRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(180.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
