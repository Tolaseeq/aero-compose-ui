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

/**
 * 19-04: extended with state-matrix rows for [AeroSwitch] and [AeroSegmentedControl] so every
 * state named in 19-04-PLAN.md's `must_haves.truths` is reachable by a reviewer with a mouse and
 * a Tab key, without editing code — rest/hover/press/focus/disabled for AeroSwitch (VSEL-01,
 * VSEL-02), and the recessed selected segment / raised unselected siblings / per-segment hover /
 * per-segment focus / disabled / N=1 / long-label overflow backstop for AeroSegmentedControl
 * (VSEL-03, VSEL-04). Hover, press, and focus are transient — produced by the reviewer
 * interacting with the live controls, never forced via static styling.
 */
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
    var longSegValue by remember { mutableStateOf(LONG_SEGMENT_LABEL) }

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

        Text(
            text = "AeroSwitch — live toggle captioned \"Notifications\" (hover/press/focus are live via " +
                "mouse + Tab), pinned checked/unchecked rest states, and disabled in both the neutral-" +
                "flattened (unchecked) and accent-flattened (checked) grooves.",
            color = colors.labelText,
            style = typography.bodySmall,
        )
        SelRow(label = "AeroSwitch") {
            AeroSwitch(checked = switched, onCheckedChange = { switched = it })
            Text("Notifications", color = colors.onBackground, style = typography.bodyMedium)
        }
        SelRow(label = "AeroSwitch (pinned)") {
            AeroSwitch(checked = true, onCheckedChange = {})
            AeroSwitch(checked = false, onCheckedChange = {})
        }
        SelRow(label = "AeroSwitch (disabled)") {
            AeroSwitch(checked = false, onCheckedChange = {}, enabled = false)
            AeroSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }

        SelRow(label = "AeroChip") {
            AeroChip(label = "Filter", selected = chipSelected, onClick = { chipSelected = !chipSelected })
            AeroChip(label = "Disabled", selected = true, onClick = {}, enabled = false)
        }

        Text(
            text = "AeroSegmentedControl — live \"Day\"/\"Week\"/\"Month\" strip, a disabled strip, a " +
                "single-option (N=1) strip, and a strip with a deliberately long middle-segment label " +
                "to exercise the E2 overflow backstop now that the inter-segment separator is dropped.",
            color = colors.labelText,
            style = typography.bodySmall,
        )
        SelRow(label = "AeroSegmentedControl") {
            AeroSegmentedControl(options = listOf("Day", "Week", "Month"), selected = segValue, onSelect = { segValue = it })
        }
        SelRow(label = "AeroSegmentedControl (disabled)") {
            AeroSegmentedControl(options = listOf("Day", "Week", "Month"), selected = segValue, onSelect = {}, enabled = false)
        }
        SelRow(label = "AeroSegmentedControl (N=1)") {
            AeroSegmentedControl(options = listOf("Solo"), selected = "Solo", onSelect = {})
        }
        SelRow(label = "AeroSegmentedControl (long label)") {
            AeroSegmentedControl(
                options = listOf("Day", LONG_SEGMENT_LABEL, "Month"),
                selected = longSegValue,
                onSelect = { longSegValue = it }
            )
        }
    }
}

/** Deliberately long (>=60 char) segment label exercising the E2 overflow/long-text backstop (19-UI-SPEC.md). */
private const val LONG_SEGMENT_LABEL =
    "This Quarter Including All Scheduled Recurring And One Time Events"

@Composable
private fun SelRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(140.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
