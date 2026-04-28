package com.mordred.aero.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Provides the active [AeroColorScheme] to all descendants. Defaults to
 * [AeroColorScheme.AeroBlue] when accessed outside an `AeroTheme {}` scope —
 * this is intentional: it lets Compose Previews and unit tests render library
 * composables without explicit theme wrapping. Production callers SHOULD wrap
 * their root in `AeroTheme {}`.
 */
public val LocalAeroColors: ProvidableCompositionLocal<AeroColorScheme> =
    staticCompositionLocalOf { AeroColorScheme.AeroBlue }

/**
 * Provides the active [AeroTypography]. Default = `AeroTypography()`.
 */
public val LocalAeroTypography: ProvidableCompositionLocal<AeroTypography> =
    staticCompositionLocalOf { AeroTypography() }

/**
 * Root theme provider. Wraps content in:
 *   - [LocalAeroColors] = [colorScheme]
 *   - [LocalAeroTypography] = [typography]
 *   - [MaterialTheme] with Material3 colors/typography bridged from the Aero values,
 *     so Material3 components inside the tree pick up the same palette.
 *
 * @param colorScheme Defaults to [AeroColorScheme.AeroBlue]. Use [AeroColorScheme.copy] for custom themes.
 * @param typography Defaults to [AeroTypography]() with Aero size/weight defaults.
 */
@Composable
public fun AeroTheme(
    colorScheme: AeroColorScheme = AeroColorScheme.AeroBlue,
    typography: AeroTypography = AeroTypography(),
    content: @Composable () -> Unit
) {
    val materialColors = darkColorScheme(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        error = colorScheme.error,
        onError = colorScheme.onError
    )

    val materialTypography = Typography(
        bodyLarge = typography.bodyLarge,
        bodyMedium = typography.bodyMedium,
        bodySmall = typography.bodySmall,
        titleLarge = typography.title,
        labelMedium = typography.label
    )

    val scrollbarStyle = ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 12.dp,
        shape = RoundedCornerShape(2.dp),
        hoverDurationMillis = 150,
        unhoverColor = colorScheme.borderSelected.copy(alpha = 0.55f),
        hoverColor = colorScheme.borderSelected
    )

    CompositionLocalProvider(
        LocalAeroColors provides colorScheme,
        LocalAeroTypography provides typography,
        LocalScrollbarStyle provides scrollbarStyle
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = materialTypography,
            content = content
        )
    }
}

/**
 * Static accessor — call site: `AeroTheme.colors.primary`, `AeroTheme.typography.title`.
 * Coexists with the [AeroTheme] function above (Kotlin allows a function and an object
 * with the same name at the top level — the same pattern as Material3's `MaterialTheme`).
 */
public object AeroTheme {
    public val colors: AeroColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAeroColors.current

    public val typography: AeroTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAeroTypography.current
}
