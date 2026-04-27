package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassEffect
import com.mordred.aero.theme.glassPanel
import com.mordred.aero.theme.glassSurface

/**
 * Phase 1 visual proof. Three demo boxes — one per glass modifier — sit on a glassPanel
 * background. Captions identify which modifier each box demonstrates.
 *
 * Layout: glassPanel wraps a Row containing three boxes (glassEffect, glassPanel-on-top,
 * glassSurface). Each box is at least 120.dp × 80.dp per UI-SPEC §Spacing Scale exception.
 * Caption sits below each box using AeroTypography.label.
 */
@Composable
fun FoundationSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(cornerRadius = 8.dp)
            .padding(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            DemoBox(label = "glassEffect") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .glassEffect(cornerRadius = 8.dp, elevation = 4.dp)
                )
            }
            DemoBox(label = "glassPanel") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .glassPanel(cornerRadius = 8.dp)
                )
            }
            DemoBox(label = "glassSurface") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .glassSurface(cornerRadius = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DemoBox(
    label: String,
    content: @Composable () -> Unit
) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
        Text(
            text = label,
            color = colors.labelText,
            style = typography.label
        )
    }
}
