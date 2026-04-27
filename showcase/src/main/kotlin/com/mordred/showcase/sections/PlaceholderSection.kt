package com.mordred.showcase.sections

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mordred.aero.theme.AeroTheme

/**
 * Future-phase row. Renders exactly one Text line in `labelText` color and `bodyMedium`
 * weight: e.g. "Buttons — coming Phase 2...". Phase 2 will replace these calls with real
 * sections one by one (SHW-03).
 */
@Composable
fun PlaceholderSection(category: String) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Text(
        text = "$category — coming Phase 2...",
        color = colors.labelText,
        style = typography.bodyMedium
    )
}
