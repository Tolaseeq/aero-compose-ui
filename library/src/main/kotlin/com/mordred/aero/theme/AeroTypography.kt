package com.mordred.aero.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typographic scale for Aero components. Sizes match the mordred reference application.
 * Color is intentionally NOT baked into these styles — it is applied at the call site
 * via `LocalContentColor` or an explicit `color` parameter so the same scale can be
 * reused across the three themes.
 */
@Immutable
public data class AeroTypography(
    public val title: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    public val bodyLarge: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    public val bodyMedium: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    public val bodySmall: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    public val label: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
)
