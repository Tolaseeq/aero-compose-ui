package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroSurfaceStyle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.glassPanel

/**
 * Phase 16 permanent Primitives gallery (D-04 sign-off vehicle, PRIM-16) — extends the
 * `FoundationSection` DemoBox-per-variant precedent. This plan (16-01) wires the first
 * primitive group (`aeroSurface`); later plans in this phase add glow-ring/thumb/groove
 * groups alongside it. Stays in the project as a living reference for Phases 17-19, not
 * thrown away after sign-off.
 */
@Composable
fun PrimitivesSection() {
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
            DemoBox(label = "aeroSurface") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .aeroSurface(
                            style = AeroSurfaceStyle.rest(colors, cornerRadius = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
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
