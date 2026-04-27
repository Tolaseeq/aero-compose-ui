package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.theme.AeroTheme

@Composable
fun ThemeSwitcher(
    current: AeroColorScheme,
    onSelect: (AeroColorScheme) -> Unit
) {
    val colors = AeroTheme.colors
    val themes = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.forEach { (name, scheme) ->
            val active = current == scheme
            Button(
                onClick = { onSelect(scheme) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) colors.primary else colors.surface,
                    contentColor = if (active) colors.onPrimary else colors.onSurface
                )
            ) {
                Text(name)
            }
        }
    }
}
