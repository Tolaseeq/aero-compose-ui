package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.containers.AeroCard
import com.mordred.aero.components.input.AeroTextField
import com.mordred.aero.components.layout.AeroAccordion
import com.mordred.aero.components.layout.AeroAccordionMode
import com.mordred.aero.components.layout.AeroAccordionSection
import com.mordred.aero.components.layout.AeroSidebar
import com.mordred.aero.components.layout.AeroSidebarMode
import com.mordred.aero.components.layout.AeroSplitOrientation
import com.mordred.aero.components.layout.AeroSplitPane
import com.mordred.aero.components.layout.AeroStepperWizard
import com.mordred.aero.components.layout.AeroWizardStep
import com.mordred.aero.components.layout.rememberAeroSidebarState
import com.mordred.aero.components.selection.AeroCheckbox
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Bell
import com.mordred.aero.icons.`internal`.Gear
import com.mordred.aero.icons.`internal`.House
import com.mordred.aero.theme.AeroTheme

@Composable
fun LayoutSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Layout", color = colors.onBackground, style = typography.title)

        // ── Accordion side-by-side ───────────────────────────────────────────
        val accordionSections = remember {
            listOf(
                AeroAccordionSection(title = "Section A") { Text("Content of section A", color = colors.onSurface) },
                AeroAccordionSection(title = "Section B") { Text("Content of section B", color = colors.onSurface) },
                AeroAccordionSection(title = "Section C") { Text("Content of section C", color = colors.onSurface) }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("single", color = colors.labelText, style = typography.bodyMedium)
                AeroAccordion(sections = accordionSections, mode = AeroAccordionMode.Single, modifier = Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f)) {
                Text("multi", color = colors.labelText, style = typography.bodyMedium)
                AeroAccordion(sections = accordionSections, mode = AeroAccordionMode.Multi, modifier = Modifier.fillMaxWidth())
            }
        }

        // ── SplitPane horizontal ─────────────────────────────────────────────
        Text("AeroSplitPane (horizontal)", color = colors.labelText, style = typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            AeroSplitPane(
                orientation = AeroSplitOrientation.Horizontal,
                modifier = Modifier.fillMaxSize(),
                start = { Box(Modifier.fillMaxSize()) { Text("Left pane", color = colors.onSurface) } },
                end = { Box(Modifier.fillMaxSize()) { Text("Right pane", color = colors.onSurface) } }
            )
        }

        // ── SplitPane vertical ───────────────────────────────────────────────
        Text("AeroSplitPane (vertical)", color = colors.labelText, style = typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            AeroSplitPane(
                orientation = AeroSplitOrientation.Vertical,
                modifier = Modifier.fillMaxSize(),
                start = { Box(Modifier.fillMaxSize()) { Text("Top pane", color = colors.onSurface) } },
                end = { Box(Modifier.fillMaxSize()) { Text("Bottom pane", color = colors.onSurface) } }
            )
        }

        // ── AeroSidebar — top-level Row sibling (PITFALL-11) ─────────────────
        val sidebarState = rememberAeroSidebarState(AeroSidebarMode.Expanded)
        Text("AeroSidebar (toggle reflows adjacent content)", color = colors.labelText, style = typography.bodyMedium)
        AeroButton(
            text = "Toggle mode: ${sidebarState.mode}",
            onClick = {
                sidebarState.mode = when (sidebarState.mode) {
                    AeroSidebarMode.Expanded  -> AeroSidebarMode.Collapsed
                    AeroSidebarMode.Collapsed -> AeroSidebarMode.Hidden
                    AeroSidebarMode.Hidden    -> AeroSidebarMode.Expanded
                }
            }
        )
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            Row(Modifier.fillMaxSize()) {
                AeroSidebar(state = sidebarState) {
                    item(AeroIcons.House, "Home", selected = true) {}
                    item(AeroIcons.Gear, "Settings", selected = false) {}
                    divider()
                    item(AeroIcons.Bell, "Alerts", selected = false) {}
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Text("Adjacent content — reflows as sidebar animates", color = colors.onSurface)
                }
            }
        }

        // ── AeroStepperWizard — 3 steps, step 1 onValidate gate ─────────────
        var wizardName by remember { mutableStateOf("") }
        var wizardOptIn by remember { mutableStateOf(false) }
        Text("AeroStepperWizard", color = colors.labelText, style = typography.bodyMedium)
        AeroCard(modifier = Modifier.fillMaxWidth()) {
            AeroStepperWizard(
                steps = listOf(
                    AeroWizardStep(
                        label = "Identifier",
                        content = {
                            AeroTextField(value = wizardName, onValueChange = { wizardName = it }, placeholder = "Session name")
                        },
                        canProceed = wizardName.isNotBlank(),
                        onValidate = { wizardName.isNotBlank() }
                    ),
                    AeroWizardStep(
                        label = "Options",
                        content = {
                            AeroCheckbox(checked = wizardOptIn, onCheckedChange = { wizardOptIn = it }, label = "Enable notifications")
                        }
                    ),
                    AeroWizardStep(
                        label = "Summary",
                        content = {
                            Text("Name: $wizardName · Opt-in: $wizardOptIn", color = colors.onSurface)
                        }
                    )
                ),
                onFinish = {}
            )
        }
    }
}
