package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroIconButton
import com.mordred.aero.components.containers.AeroCard
import com.mordred.aero.components.input.AeroTextField
import com.mordred.aero.components.layout.AeroAccordion
import com.mordred.aero.components.layout.AeroAccordionMode
import com.mordred.aero.components.layout.AeroAccordionSection
import com.mordred.aero.components.layout.AeroPanelGroup
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
import com.mordred.aero.icons.`internal`.Database
import com.mordred.aero.icons.`internal`.File
import com.mordred.aero.icons.`internal`.Folder
import com.mordred.aero.icons.`internal`.Gear
import com.mordred.aero.icons.`internal`.House
import com.mordred.aero.icons.`internal`.Trash
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

        // ── SplitPane nested 3-pane (FIXSP-01/02 reproducible: drag outer, inner holds & never freezes) ──
        Text("AeroSplitPane (nested 3-pane)", color = colors.labelText, style = typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            AeroSplitPane(
                orientation = AeroSplitOrientation.Horizontal,
                modifier = Modifier.fillMaxSize(),
                initialSplitFraction = 0.33f,
                start = { Box(Modifier.fillMaxSize()) { Text("Pane 1", color = colors.onSurface) } },
                end = {
                    AeroSplitPane(
                        orientation = AeroSplitOrientation.Horizontal,
                        modifier = Modifier.fillMaxSize(),
                        initialSplitFraction = 0.5f,
                        start = { Box(Modifier.fillMaxSize()) { Text("Pane 2", color = colors.onSurface) } },
                        end = { Box(Modifier.fillMaxSize()) { Text("Pane 3", color = colors.onSurface) } },
                    )
                },
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
        // F-WIZARD: wizard is surface-less and uses weight(1f, fill=false) for the active step.
        // A weight(1f) child needs a bounded max-height on its parent Column; without it the
        // step content area collapses to 0dp and the AeroTextField has no hit-testable area.
        // Fix: wrap in Box(height(200.dp)) so the wizard Column receives a finite max-height.
        var wizardName by remember { mutableStateOf("") }
        var wizardOptIn by remember { mutableStateOf(false) }
        Text("AeroStepperWizard", color = colors.labelText, style = typography.bodyMedium)
        AeroCard(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
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

        // ── AeroPanelGroup — Win7 Aero vertical sections (PNL-01..PNL-18) ──
        // Bounded parent (360.dp) required: AeroPanelGroup fills its constraints (F-WIZARD precedent).
        // Demonstrates: leadingIcon, headerActions (non-bubbling), collapsible=false, resizable=false,
        // overflowing content (clip), drag-resize between adjacent expanded sections.
        Text("AeroPanelGroup", color = colors.labelText, style = typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height(360.dp)) {
            AeroPanelGroup(modifier = Modifier.fillMaxSize()) {
                section(
                    key = "files",
                    title = "Files",
                    leadingIcon = AeroIcons.Folder,
                    headerActions = {
                        AeroIconButton(onClick = {}) {
                            Icon(
                                imageVector = AeroIcons.File,
                                contentDescription = "New file",
                                tint = AeroTheme.colors.onSurface,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(12) { idx ->
                            Text(
                                text = "document_${idx + 1}.txt",
                                color = colors.onSurface,
                                style = typography.bodyMedium,
                            )
                        }
                    }
                }
                section(
                    key = "database",
                    title = "Database",
                    leadingIcon = AeroIcons.Database,
                    collapsible = false,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("users (42 rows)", color = colors.onSurface, style = typography.bodyMedium)
                        Text("orders (128 rows)", color = colors.onSurface, style = typography.bodyMedium)
                        Text("products (56 rows)", color = colors.onSurface, style = typography.bodyMedium)
                    }
                }
                section(
                    key = "logs",
                    title = "Logs",
                    headerActions = {
                        AeroIconButton(onClick = {}) {
                            Icon(
                                imageVector = AeroIcons.Trash,
                                contentDescription = "Clear logs",
                                tint = AeroTheme.colors.onSurface,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                    resizable = false,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("[INFO] Application started", color = colors.onSurface, style = typography.bodyMedium)
                        Text("[WARN] Config missing, using defaults", color = colors.onSurface, style = typography.bodyMedium)
                        Text("[ERROR] Connection timeout", color = colors.onSurface, style = typography.bodyMedium)
                    }
                }
            }
        }
    }
}
