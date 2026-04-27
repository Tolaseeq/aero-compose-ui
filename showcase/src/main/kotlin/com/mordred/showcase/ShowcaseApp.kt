package com.mordred.showcase

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme
import com.mordred.showcase.sections.FoundationSection
import com.mordred.showcase.sections.PlaceholderSection
import com.mordred.showcase.sections.ThemeSwitcher

@Composable
fun ShowcaseApp() {
    var currentScheme by remember { mutableStateOf(AeroColorScheme.AeroBlue) }

    AeroTheme(colorScheme = currentScheme) {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
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
                    onSelect = { currentScheme = it }
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Foundation",
                        color = colors.onBackground,
                        style = typography.title
                    )
                    FoundationSection()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceholderSection(category = "Buttons")
                    PlaceholderSection(category = "Input")
                    PlaceholderSection(category = "Selection")
                    PlaceholderSection(category = "Dropdown")
                    PlaceholderSection(category = "Range & Progress")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
