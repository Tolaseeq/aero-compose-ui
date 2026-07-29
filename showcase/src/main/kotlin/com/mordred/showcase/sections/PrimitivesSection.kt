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
import com.mordred.aero.theme.aeroGlowRing
import com.mordred.aero.theme.aeroGroove
import com.mordred.aero.theme.aeroSurface
import com.mordred.aero.theme.aeroThumbSurface
import com.mordred.aero.theme.glassPanel

/**
 * Phase 16 permanent Primitives gallery (D-04 sign-off vehicle, PRIM-16) — extends the
 * `FoundationSection` DemoBox-per-variant precedent. Wires all four primitive groups from
 * `AeroSurfacePrimitives.kt`: `aeroSurface` (16-01), and `aeroGlowRing` / `aeroThumbSurface` /
 * the recessed groove (16-02). Stays in the project as a living reference for Phases 17-19,
 * not thrown away after sign-off.
 */
@Composable
fun PrimitivesSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    // 20-05 IN-02: read via the AeroTheme.ornaments accessor (honors ornamentOverride, PRIM-03)
    // instead of calling AeroOrnamentTokens.derive(colors) directly.
    val ornaments = AeroTheme.ornaments

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
            DemoBox(label = "aeroGlowRing") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 80.dp)
                        .aeroGlowRing(active = true, glowColor = ornaments.hoverGlow, cornerRadius = 8.dp)
                        .aeroSurface(
                            style = AeroSurfaceStyle.rest(colors, cornerRadius = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
            DemoBox(label = "aeroThumbSurface") {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .aeroThumbSurface(style = AeroSurfaceStyle.rest(colors, cornerRadius = 12.dp))
                )
            }
            DemoBox(label = "groove") {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 12.dp)
                        .aeroGroove(style = AeroSurfaceStyle.rest(colors, cornerRadius = 6.dp))
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
