package com.mordred.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.overlay.AeroToastHost
import com.mordred.aero.components.overlay.AeroToastHostState
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.showcase.sections.ButtonsSection
import com.mordred.showcase.sections.ContainersSection
import com.mordred.showcase.sections.DropdownSection
import com.mordred.showcase.sections.FoundationSection
import com.mordred.showcase.sections.IconsSection
import com.mordred.showcase.sections.InputSection
import com.mordred.showcase.sections.ListSection
import com.mordred.showcase.sections.DataSection
import com.mordred.showcase.sections.LayoutSection
import com.mordred.showcase.sections.NavigationSection
import com.mordred.showcase.sections.PanelGroupSpikeSection
import com.mordred.showcase.sections.OverlaysSection
import com.mordred.showcase.sections.PickersSection
import com.mordred.showcase.sections.RangeSection
import com.mordred.showcase.sections.SelectionSection
import com.mordred.showcase.sections.ThemeSwitcher

/**
 * ShowcaseApp now accepts the active color scheme from its caller (Main.kt) so that
 * the AeroTitleBar in Main.kt shares the same theme. AeroToastHost is mounted at
 * the root of this composable's Box and overlays the scrolling content.
 *
 * NOTE: ContainersSection / OverlaysSection / NavigationSection calls are added by
 * Task 4 of Plan 03-08 once the section files exist. After Task 1 of Plan 03-08
 * runs alone, the showcase shows only the existing Phase 1+2 sections.
 */
@Composable
fun ShowcaseApp(
    currentScheme: AeroColorScheme,
    onSchemeChange: (AeroColorScheme) -> Unit
) {
    // Note: we do NOT wrap in AeroTheme {} here — that wrapping happens in Main.kt
    // so the title bar participates in the same theme.
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val toastState = remember { AeroToastHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(48.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                ThemeSwitcher(
                    current = currentScheme,
                    onSelect = onSchemeChange
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Foundation",
                        color = colors.onBackground,
                        style = typography.title
                    )
                    FoundationSection()
                }

                IconsSection(toastState = toastState)

                ButtonsSection()
                InputSection()
                SelectionSection()
                DropdownSection()
                RangeSection()
                ListSection()

                ContainersSection()
                OverlaysSection(toastState = toastState)
                NavigationSection()

                DataSection()
                PickersSection()
                LayoutSection()
                // THROWAWAY: spike section for PNL-PITFALL-01 gate — remove in plan 13-05
                PanelGroupSpikeSection()

                Spacer(Modifier.height(24.dp))
            }
        }
        AeroToastHost(
            state = toastState,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
