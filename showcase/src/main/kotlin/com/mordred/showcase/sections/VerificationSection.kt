package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.list.AeroListItem
import com.mordred.aero.components.range.AeroProgressBar
import com.mordred.aero.components.range.AeroRangeSlider
import com.mordred.aero.components.range.AeroSlider
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.components.selection.AeroSwitch
import com.mordred.aero.theme.AeroTheme

/**
 * Phase 20 — permanent cross-component coherence review section (SHW-15 / D-04).
 *
 * All eight components restyled across Phases 17-19 render here, side by side, in one flow, so
 * the cross-component coherence question — "do all eight read as one material family, or as eight
 * separate approximations of it" — has a single screen to be answered on. Previously the eight
 * demos were spread across [ButtonsSection], [RangeSection], [SelectionSection] and [ListSection],
 * so this question could only be answered from memory across four scroll positions.
 *
 * Each demo block below holds state tiles in the fixed order `default -> hover -> press -> focus
 * -> disabled`, so the three-theme sign-off captures are positionally comparable across themes.
 * The `default`, `hover`, `press` and `focus` tiles are, on purpose, plain enabled instances of the
 * same component with the same props — hover/press/focus are transient states reached live through
 * a real mouse and keyboard (17-04 precedent), never faked via forced/static styling. Only the
 * `disabled` tile differs in props (`enabled = false`).
 *
 * Two components omit tiles for states they do not support, rather than rendering an empty
 * placeholder:
 *  - [AeroRangeSlider] has no `focus` tile: its custom `Canvas` never acquires keyboard focus at
 *    all (deferred out of scope at 18-04) — this is a documented, human-accepted deferral, not an
 *    oversight of this section.
 *  - [AeroProgressBar] has only a `default` tile: its public signature carries no `enabled`
 *    parameter and the component has no hover/press/focus surface at all (18-03: "AeroProgressBar
 *    has no enabled/disabled state and none was added").
 *
 * This section reuses [com.mordred.showcase.ShowcaseApp]'s existing outer scrolling Column — it
 * introduces no second, nested scroll region.
 */
@Composable
fun VerificationSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    var switchChecked by remember { mutableStateOf(true) }
    var segmentSelected by remember { mutableStateOf("Day") }
    var sliderValue by remember { mutableStateOf(0.5f) }
    var rangeValue by remember { mutableStateOf(0.2f..0.7f) }
    var listSelectedIndex by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Verification", color = colors.onBackground, style = typography.title)
        Text(
            text = "All eight restyled components, side by side, at equal visual weight — the " +
                "cross-component coherence review this milestone exists to enable. Hover, press, " +
                "and focus are transient — reviewed live across all three themes via the theme " +
                "switcher above: mouse-over for hover, click-and-hold for press, Tab to focus.",
            color = colors.labelText,
            style = typography.bodySmall,
        )

        VerificationDemoBlock(name = "AeroButton") {
            StateTile(label = STATE_DEFAULT) { AeroButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_HOVER) { AeroButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_PRESS) { AeroButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_FOCUS) { AeroButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_DISABLED) { AeroButton(text = "Action", onClick = {}, enabled = false) }
        }

        VerificationDemoBlock(name = "AeroOutlinedButton") {
            StateTile(label = STATE_DEFAULT) { AeroOutlinedButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_HOVER) { AeroOutlinedButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_PRESS) { AeroOutlinedButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_FOCUS) { AeroOutlinedButton(text = "Action", onClick = {}) }
            StateTile(label = STATE_DISABLED) { AeroOutlinedButton(text = "Action", onClick = {}, enabled = false) }
        }

        VerificationDemoBlock(name = "AeroSwitch") {
            StateTile(label = STATE_DEFAULT) { AeroSwitch(checked = switchChecked, onCheckedChange = { switchChecked = it }) }
            StateTile(label = STATE_HOVER) { AeroSwitch(checked = switchChecked, onCheckedChange = { switchChecked = it }) }
            StateTile(label = STATE_PRESS) { AeroSwitch(checked = switchChecked, onCheckedChange = { switchChecked = it }) }
            StateTile(label = STATE_FOCUS) { AeroSwitch(checked = switchChecked, onCheckedChange = { switchChecked = it }) }
            StateTile(label = STATE_DISABLED) { AeroSwitch(checked = switchChecked, onCheckedChange = {}, enabled = false) }
        }

        VerificationDemoBlock(name = "AeroSegmentedControl") {
            val options = listOf("Day", "Week", "Month")
            StateTile(label = STATE_DEFAULT) { AeroSegmentedControl(options = options, selected = segmentSelected, onSelect = { segmentSelected = it }) }
            StateTile(label = STATE_HOVER) { AeroSegmentedControl(options = options, selected = segmentSelected, onSelect = { segmentSelected = it }) }
            StateTile(label = STATE_PRESS) { AeroSegmentedControl(options = options, selected = segmentSelected, onSelect = { segmentSelected = it }) }
            StateTile(label = STATE_FOCUS) { AeroSegmentedControl(options = options, selected = segmentSelected, onSelect = { segmentSelected = it }) }
            StateTile(label = STATE_DISABLED) { AeroSegmentedControl(options = options, selected = segmentSelected, onSelect = {}, enabled = false) }
        }

        VerificationDemoBlock(name = "AeroSlider") {
            val sliderWidth = Modifier.width(96.dp)
            StateTile(label = STATE_DEFAULT) { AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = sliderWidth) }
            StateTile(label = STATE_HOVER) { AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = sliderWidth) }
            StateTile(label = STATE_PRESS) { AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = sliderWidth) }
            StateTile(label = STATE_FOCUS) { AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = sliderWidth) }
            StateTile(label = STATE_DISABLED) { AeroSlider(value = sliderValue, onValueChange = {}, modifier = sliderWidth, enabled = false) }
        }

        // AeroRangeSlider omits the focus tile entirely — its Canvas has no keyboard-focus
        // tracking at all (18-04, explicitly accepted as deferred out of scope). This is a
        // documented deferral, not a state this section forgot to wire.
        VerificationDemoBlock(name = "AeroRangeSlider") {
            val rangeWidth = Modifier.width(96.dp)
            StateTile(label = STATE_DEFAULT) { AeroRangeSlider(value = rangeValue, onValueChange = { rangeValue = it }, modifier = rangeWidth) }
            StateTile(label = STATE_HOVER) { AeroRangeSlider(value = rangeValue, onValueChange = { rangeValue = it }, modifier = rangeWidth) }
            StateTile(label = STATE_PRESS) { AeroRangeSlider(value = rangeValue, onValueChange = { rangeValue = it }, modifier = rangeWidth) }
            StateTile(label = STATE_DISABLED) { AeroRangeSlider(value = rangeValue, onValueChange = {}, modifier = rangeWidth, enabled = false) }
        }

        // AeroProgressBar renders only the default tile — its public signature carries no
        // `enabled` parameter and the component has no hover/press/focus surface at all (18-03).
        VerificationDemoBlock(name = "AeroProgressBar") {
            StateTile(label = STATE_DEFAULT) { AeroProgressBar(progress = 0.6f, modifier = Modifier.width(96.dp)) }
        }

        VerificationDemoBlock(name = "AeroListItem") {
            val listWidth = Modifier.width(160.dp)
            StateTile(label = STATE_DEFAULT) { AeroListItem(text = "Item", onClick = { listSelectedIndex = 0 }, selected = listSelectedIndex == 0, modifier = listWidth) }
            StateTile(label = STATE_HOVER) { AeroListItem(text = "Item", onClick = { listSelectedIndex = 0 }, selected = listSelectedIndex == 0, modifier = listWidth) }
            StateTile(label = STATE_PRESS) { AeroListItem(text = "Item", onClick = { listSelectedIndex = 0 }, selected = listSelectedIndex == 0, modifier = listWidth) }
            StateTile(label = STATE_FOCUS) { AeroListItem(text = "Item", onClick = { listSelectedIndex = 0 }, selected = listSelectedIndex == 0, modifier = listWidth) }
            StateTile(label = STATE_DISABLED) { AeroListItem(text = "Item", onClick = {}, enabled = false, modifier = listWidth) }
        }
    }
}

private const val STATE_DEFAULT = "default"
private const val STATE_HOVER = "hover — mouse over"
private const val STATE_PRESS = "press — click and hold"
private const val STATE_FOCUS = "focus — Tab"
private const val STATE_DISABLED = "disabled"

/**
 * One component's demo block: its bare-name caption followed by a row of [StateTile]s in the
 * fixed `default -> hover -> press -> focus -> disabled` order. Sizes to `max(caption, control)`
 * via ordinary wrap-content [Column]/[Row] sizing (no fixed-width modifier), so the caption never
 * truncates — load-bearing for [AeroSwitch], whose caption is wider than its 36.dp track.
 */
@Composable
private fun VerificationDemoBlock(name: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = name, color = AeroTheme.colors.labelText, style = AeroTheme.typography.label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
            content = { content() }
        )
    }
}

/** One state tile: a live component instance with a text label naming the state beneath it. */
@Composable
private fun StateTile(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        content()
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.label)
    }
}
