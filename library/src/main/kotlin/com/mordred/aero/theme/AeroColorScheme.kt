package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Aero color tokens. Three built-in presets are exposed as companion-object properties:
 * [AeroBlue], [AeroDark], [Classic]. Custom themes are produced via [copy].
 *
 * All composables under `AeroTheme {}` read these tokens from `LocalAeroColors`.
 */
@Immutable
public data class AeroColorScheme(
    public val primary: Color,
    public val onPrimary: Color,
    public val secondary: Color,
    public val onSecondary: Color,
    public val surface: Color,
    public val onSurface: Color,
    public val background: Color,
    public val onBackground: Color,
    public val error: Color,
    public val onError: Color,
    public val cardBackground: Color,
    public val borderDefault: Color,
    public val borderSelected: Color,
    public val labelText: Color,
    public val glassSurface: Color,
    public val glassBorder: Color,
    public val glassHighlight: Color,
    public val titleBarGradientStart: Color,
    public val titleBarGradientEnd: Color,
    public val titleBarText: Color,
    public val buttonHover: Color,
    public val closeButtonHover: Color,
    public val panelBackground: Color
) {
    public companion object {
        public val AeroBlue: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003B5C),
            secondary = Color(0xFF81D4FA),
            onSecondary = Color(0xFF003B5C),
            surface = Color(0xCC1A3A5C),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF0D1B2A),
            onBackground = Color(0xFFE0E0E0),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0x40FFFFFF),
            borderDefault = Color(0x60FFFFFF),
            borderSelected = Color(0xFF4FC3F7),
            labelText = Color(0xFFBDBDBD),
            glassSurface = Color(0x30FFFFFF),
            glassBorder = Color(0x50FFFFFF),
            glassHighlight = Color(0x20FFFFFF),
            titleBarGradientStart = Color(0xDD1A3A6C),
            titleBarGradientEnd = Color(0xDD0D1F3C),
            titleBarText = Color(0xFFE0E8F0),
            buttonHover = Color(0x40FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xCC152A42)
        )

        public val AeroDark: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color(0xFF0D1B2A),
            secondary = Color(0xFF64B5F6),
            onSecondary = Color(0xFF0D1B2A),
            surface = Color(0xCC1A1A2E),
            onSurface = Color(0xFFCCCCCC),
            background = Color(0xFF0A0A1A),
            onBackground = Color(0xFFCCCCCC),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0x30000000),
            borderDefault = Color(0x40FFFFFF),
            borderSelected = Color(0xFF90CAF9),
            labelText = Color(0xFFAAAAAA),
            glassSurface = Color(0x20FFFFFF),
            glassBorder = Color(0x30FFFFFF),
            glassHighlight = Color(0x15FFFFFF),
            titleBarGradientStart = Color(0xDD1A1A3E),
            titleBarGradientEnd = Color(0xDD0A0A1E),
            titleBarText = Color(0xFFD0D0E0),
            buttonHover = Color(0x30FFFFFF),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xCC12122A)
        )

        public val Classic: AeroColorScheme = AeroColorScheme(
            primary = Color(0xFF5C8ABF),
            onPrimary = Color.White,
            secondary = Color(0xFF7BA5D1),
            onSecondary = Color.White,
            surface = Color(0xFF2D2D2D),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0),
            error = Color(0xFFEF5350),
            onError = Color.White,
            cardBackground = Color(0xFF424242),
            borderDefault = Color(0xFF555555),
            borderSelected = Color(0xFF5C8ABF),
            labelText = Color.LightGray,
            glassSurface = Color(0xFF333333),
            glassBorder = Color(0xFF555555),
            glassHighlight = Color(0xFF3A3A3A),
            titleBarGradientStart = Color(0xFF3A3A3A),
            titleBarGradientEnd = Color(0xFF2A2A2A),
            titleBarText = Color(0xFFE0E0E0),
            buttonHover = Color(0xFF4A4A4A),
            closeButtonHover = Color(0xFFE81123),
            panelBackground = Color(0xFF2D2D2D)
        )
    }
}
