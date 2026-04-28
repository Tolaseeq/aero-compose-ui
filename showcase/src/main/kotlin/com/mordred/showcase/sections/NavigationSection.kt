package com.mordred.showcase.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.navigation.AeroBreadcrumb
import com.mordred.aero.components.navigation.AeroBreadcrumbItem
import com.mordred.aero.components.navigation.AeroMenuBar
import com.mordred.aero.components.navigation.AeroMenuItem
import com.mordred.aero.components.navigation.AeroStatusBar
import com.mordred.aero.components.navigation.AeroTab
import com.mordred.aero.components.navigation.AeroTabBar
import com.mordred.aero.components.navigation.AeroTopLevelMenu
import com.mordred.aero.theme.AeroTheme

/**
 * Phase 3 — Navigation section: NAV-02..05.
 *
 * NAV-01 AeroTitleBar is mounted in Main.kt (top of the window) — see the
 * "AeroTitleBar" reminder row below.
 */
@Composable
fun NavigationSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var selectedTab by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Navigation", color = colors.onBackground, style = typography.title)

        // NAV-01 AeroTitleBar is mounted in Main.kt — point that out via comment row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AeroTitleBar",
                color = colors.labelText,
                style = typography.bodyMedium,
                modifier = Modifier.width(140.dp)
            )
            Text(
                text = "Mounted at the top of this window — see Main.kt",
                color = colors.onSurface,
                style = typography.bodyLarge
            )
        }

        SectionRow(label = "AeroMenuBar") {
            Box(modifier = Modifier.width(420.dp)) {
                AeroMenuBar(
                    menus = listOf(
                        AeroTopLevelMenu(
                            label = "File",
                            items = listOf(
                                AeroMenuItem.Action("New", {}),
                                AeroMenuItem.Action("Open...", {}),
                                AeroMenuItem.Divider,
                                AeroMenuItem.Action("Exit", {})
                            )
                        ),
                        AeroTopLevelMenu(
                            label = "Edit",
                            items = listOf(
                                AeroMenuItem.Action("Cut", {}),
                                AeroMenuItem.Action("Copy", {}),
                                AeroMenuItem.Action("Paste", {})
                            )
                        ),
                        AeroTopLevelMenu(
                            label = "Help",
                            items = listOf(
                                AeroMenuItem.Action("About", {})
                            )
                        )
                    )
                )
            }
        }

        SectionRow(label = "AeroBreadcrumb") {
            AeroBreadcrumb(
                items = listOf(
                    AeroBreadcrumbItem("Home"),
                    AeroBreadcrumbItem("Documents"),
                    AeroBreadcrumbItem("Reports"),
                    AeroBreadcrumbItem("2026-Q2")
                ),
                onItemClick = { _, _ -> }
            )
        }

        SectionRow(label = "AeroTabBar") {
            Box(modifier = Modifier.width(440.dp)) {
                AeroTabBar(
                    tabs = listOf(
                        AeroTab("Overview"),
                        AeroTab("Details"),
                        AeroTab("Activity"),
                        AeroTab("Settings")
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }
        }

        SectionRow(label = "AeroStatusBar") {
            Box(modifier = Modifier.width(520.dp)) {
                AeroStatusBar {
                    Text("Ready", color = colors.onSurface, style = typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("Ln 24, Col 13", color = colors.onSurface, style = typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF66BB6A))
                    )
                }
            }
        }
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
