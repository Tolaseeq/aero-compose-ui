package com.mordred.aero.scratch

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mordred.aero.components.`internal`.drag.aeroDragSplitter
import com.mordred.aero.components.`internal`.popup.AeroCalendarPositionProvider
import com.mordred.aero.components.layout.`internal`.stepper.AeroStepIndicator
import com.mordred.aero.components.pickers.`internal`.calendar.AeroCalendarGrid
import com.mordred.aero.components.pickers.`internal`.color.AeroHsvColorSquare
import com.mordred.aero.components.pickers.`internal`.color.AeroHueSlider
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.LocalDate

/**
 * **TEMPORARY — deleted in Phase 11.**
 *
 * Eyes-on confirmation surface for all 6 Phase 7 internal primitives:
 * - AeroCalendarGrid (logic + rendering)
 * - AeroHsvColorSquare + AeroHueSlider (Canvas drag, PITFALL-03 mitigation)
 * - Modifier.aeroDragSplitter (horizontal + vertical drag)
 * - AeroStepIndicator (current/completed/upcoming across all 3 themes)
 * - AeroCalendarPositionProvider (wide popup near right edge of 1024dp scratch frame)
 *
 * Why this lives in `:library` (not `:showcase` as the plan source listing suggested): the
 * 6 Phase 7 primitives are `internal` (Plan-01 ADR + 07-CONTEXT.md §carry-forward rules:
 * "explicitApi() enforced on :library — every primitive must be internal"). Kotlin's
 * `internal` is module-scoped, so the showcase module cannot import them directly. To keep
 * the locked-internal contract intact, the demo aggregator lives here and the showcase
 * module renders it via a thin wrapper (`Phase7ScratchSection` in
 * `showcase/.../sections/Phase7ScratchSection.kt`). This file is THE entire scratch
 * implementation; the showcase wrapper is one line.
 *
 * Phase 11 cleanup: delete this file (`AeroPhase7Scratch.kt`), delete the showcase wrapper
 * (`Phase7ScratchSection.kt`), and remove the `Phase7ScratchSection()` call + import from
 * `ShowcaseApp.kt`.
 */
@Composable
public fun AeroPhase7Scratch() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Phase 7 Scratch (TEMPORARY — deleted Phase 11)",
            color = colors.onBackground,
            style = typography.title.copy(fontWeight = FontWeight.SemiBold),
        )

        // 1. AeroCalendarGrid demo
        CalendarGridDemo()

        // 2. HSV square + hue slider demo
        HsvDemo()

        // 3. aeroDragSplitter demo (horizontal + vertical)
        DragSplitterDemo()

        // 4. Step indicator demo
        StepIndicatorDemo()

        // 5. AeroCalendarPositionProvider — wide popup near right edge of 1024dp frame
        CalendarPopupDemo()
    }
}

@Composable
private fun CalendarGridDemo() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var displayMonth by remember { mutableStateOf(LocalDate(2026, 4, 1)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AeroCalendarGrid", color = colors.onBackground, style = typography.bodyMedium)
        Box(
            modifier = Modifier
                .background(colors.panelBackground)
                .padding(8.dp),
        ) {
            AeroCalendarGrid(
                displayMonth = displayMonth,
                selected = selected,
                onDateSelected = { selected = it },
                onMonthChange = { displayMonth = it },
            )
        }
        Text(
            text = "Selected: ${selected ?: "(none)"}",
            color = colors.labelText,
            style = typography.label,
        )
    }
}

@Composable
private fun HsvDemo() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var hue by remember { mutableStateOf(0f) }              // [0f, 360f]
    var saturation by remember { mutableStateOf(1f) }       // [0f, 1f]
    var value by remember { mutableStateOf(1f) }            // [0f, 1f]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AeroHsvColorSquare + AeroHueSlider", color = colors.onBackground, style = typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AeroHsvColorSquare(
                hue = hue,
                saturation = saturation,
                value = value,
                onSatValChange = { s, v -> saturation = s; value = v },
            )
            AeroHueSlider(
                hue = hue,
                onHueChange = { hue = it },
            )
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .background(Color.hsv(hue.coerceIn(0f, 360f), saturation, value)),
            )
        }
        Text(
            text = "h=${"%.1f".format(hue)} s=${"%.2f".format(saturation)} v=${"%.2f".format(value)}",
            color = colors.labelText,
            style = typography.label,
        )
    }
}

@Composable
private fun DragSplitterDemo() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var hPos by remember { mutableStateOf(0f) }
    var vPos by remember { mutableStateOf(0f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Modifier.aeroDragSplitter", color = colors.onBackground, style = typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Horizontal splitter demo
            Column {
                Text("Horizontal", color = colors.labelText, style = typography.label)
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(16.dp)
                        .background(colors.surface)
                        .aeroDragSplitter(
                            orientation = Orientation.Horizontal,
                            onDrag = { delta -> hPos += delta },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.width(160.dp).height(1.dp).background(colors.borderDefault))
                }
                Text(
                    text = "hPos = ${"%.1f".format(hPos)}px",
                    color = colors.labelText,
                    style = typography.label,
                )
            }
            // Vertical splitter demo
            Column {
                Text("Vertical", color = colors.labelText, style = typography.label)
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(80.dp)
                        .background(colors.surface)
                        .aeroDragSplitter(
                            orientation = Orientation.Vertical,
                            onDrag = { delta -> vPos += delta },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.width(1.dp).height(80.dp).background(colors.borderDefault))
                }
                Text(
                    text = "vPos = ${"%.1f".format(vPos)}px",
                    color = colors.labelText,
                    style = typography.label,
                )
            }
        }
    }
}

@Composable
private fun StepIndicatorDemo() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AeroStepIndicator (toggle theme to verify all 3)", color = colors.onBackground, style = typography.bodyMedium)
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            AeroStepIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (currentStep > 0) currentStep-- },
                enabled = currentStep > 0,
            ) { Text("Prev") }
            Button(
                onClick = { if (currentStep < totalSteps - 1) currentStep++ },
                enabled = currentStep < totalSteps - 1,
            ) { Text("Next") }
            Text(
                text = "step ${currentStep + 1} / $totalSteps",
                color = colors.labelText,
                style = typography.label,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CalendarPopupDemo() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var open by remember { mutableStateOf(false) }
    var displayMonth by remember { mutableStateOf(LocalDate(2026, 4, 1)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "AeroCalendarPositionProvider — trigger near right edge of 1024dp frame",
            color = colors.onBackground,
            style = typography.bodyMedium,
        )
        // 1024dp scratch frame; trigger button placed near right edge so popup must right-align.
        Box(
            modifier = Modifier
                .width(1024.dp)
                .height(64.dp)
                .background(colors.surface),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Button(
                onClick = { open = !open },
                modifier = Modifier.padding(end = 16.dp),
            ) {
                Text(if (open) "Close calendar" else "Open calendar")
            }
            if (open) {
                Popup(
                    popupPositionProvider = AeroCalendarPositionProvider(),
                    onDismissRequest = { open = false },
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                ) {
                    Box(
                        modifier = Modifier
                            .background(colors.panelBackground)
                            .padding(8.dp),
                    ) {
                        AeroCalendarGrid(
                            displayMonth = displayMonth,
                            selected = selected,
                            onDateSelected = { selected = it; open = false },
                            onMonthChange = { displayMonth = it },
                        )
                    }
                }
            }
        }
        Text(
            text = "selected (popup): ${selected ?: "(none)"}",
            color = colors.labelText,
            style = typography.label,
        )
    }
}
