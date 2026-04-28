package com.mordred.showcase.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.components.selection.AeroSegmentedControl

@Composable
fun ThemeSwitcher(
    current: AeroColorScheme,
    onSelect: (AeroColorScheme) -> Unit
) {
    val themes = remember {
        listOf(
            ThemeOption("AeroBlue", AeroColorScheme.AeroBlue),
            ThemeOption("AeroDark", AeroColorScheme.AeroDark),
            ThemeOption("Classic", AeroColorScheme.Classic)
        )
    }
    val selected = themes.first { it.scheme == current }
    AeroSegmentedControl(
        options = themes,
        selected = selected,
        onSelect = { onSelect(it.scheme) },
        optionLabel = { it.name }
    )
}

private data class ThemeOption(val name: String, val scheme: AeroColorScheme)
