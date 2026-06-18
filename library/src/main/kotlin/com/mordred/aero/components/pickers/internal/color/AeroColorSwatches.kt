package com.mordred.aero.components.pickers.internal.color

import androidx.compose.ui.graphics.Color

/**
 * Default swatch palette for `AeroColorPicker` (Phase 8, PICK-07).
 *
 * Exactly 16 Aero-fit colors: the 8 spectral primaries/secondaries, white + black,
 * a four-step gray ramp, and a pair of signature Aero blues. Consumers can override
 * via the `swatches` parameter on `AeroColorPicker` / `AeroColorPickerButton`.
 */
internal val DefaultAeroSwatches: List<Color> = listOf(
    Color(0xFFFF0000), // red
    Color(0xFFFF8C00), // orange
    Color(0xFFFFFF00), // yellow
    Color(0xFF00C000), // green
    Color(0xFF00FFFF), // cyan
    Color(0xFF0066FF), // blue
    Color(0xFF8A2BE2), // violet
    Color(0xFFFF00FF), // magenta
    Color(0xFFFFFFFF), // white
    Color(0xFFC0C0C0), // light gray
    Color(0xFF808080), // mid gray
    Color(0xFF404040), // dark gray
    Color(0xFF000000), // black
    Color(0xFF4AA3DF), // Aero sky blue
    Color(0xFF1C6BA0), // Aero deep blue
    Color(0xFFAEDFF7), // Aero glass tint
)
