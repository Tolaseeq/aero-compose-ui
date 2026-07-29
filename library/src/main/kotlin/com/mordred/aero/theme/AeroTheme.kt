package com.mordred.aero.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
 *   - (when [establishBackground] is `true`, the default) a `Modifier.fillMaxSize().background(...)`
 *     [Box] painted [AeroColorScheme.background] — see below.
 *
 * ## Why this background exists (SHW-16 / VER-05)
 *
 * `AeroTheme` used to provide only composition locals and painted nothing itself. Every shipped
 * screen (the showcase) happened to paint its OWN `Surface(color = colors.background)` on top of
 * it, which hid the gap. A stranger who does exactly what the doc above implies — wraps bare
 * content in `AeroTheme {}` with no Surface of their own, e.g. `AeroTheme { Column { ... } }` —
 * got Compose Desktop's default light window background showing through, with label colors
 * designed for a dark backdrop (e.g. AeroBlue's `labelText = 0xFFBDBDBD`) rendered on top of it,
 * barely legible. Confirmed via the VER-05 external scratch-consumer human-verify checkpoint.
 *
 * **Plain [Box] + `.background(...)`, deliberately NOT Material3's `Surface`.** `Surface`'s
 * internal container sets `propagateMinConstraints = true` — fine when its content is itself a
 * `fillMaxSize()` layout (as in the showcase's own root `Surface`), but every one of the ~40
 * library components' previews/unit-tests that pass a single non-filling composable directly as
 * `AeroTheme`'s content (e.g. a bare `AeroListItem` with `Modifier.heightIn(min = 36.dp)`) got
 * that minimum FORCED UP to the full window/root height, because the propagated min constraint
 * equals the Surface's own (window-sized) min. Caught by two real test failures during this fix
 * (`AeroListItemLayoutTest`, `AeroRangeSliderHoverCancellationTest`) before landing on this plain
 * `Box` (default `propagateMinConstraints = false`), which reproduces exactly the loose
 * (`min = 0`) constraints `content` received before this change existed.
 *
 * [establishBackground] defaults to `true` so every EXISTING call site (~40 components' previews,
 * unit tests, and the showcase itself) picks up the fix with zero signature changes required at
 * the call site — this is a public API addition, not a breaking change. It stays a parameter
 * (not an unconditional paint) rather than an always-on `fillMaxSize()` because `AeroTheme {}` is
 * also a legitimate way to theme a constrained slot embedded inside a foreign layout that already
 * owns its own background; forcing a same-size opaque fill in that case would paint over — not
 * blend with — whatever the host already drew there. A consumer in that position passes
 * `establishBackground = false` to opt out.
 *
 * @param colorScheme Defaults to [AeroColorScheme.AeroBlue]. Use [AeroColorScheme.copy] for custom themes.
 * @param typography Defaults to [AeroTypography]() with Aero size/weight defaults.
 * @param establishBackground When `true` (default), wraps [content] in a `Modifier.fillMaxSize()`
 *   [Box] painted [AeroColorScheme.background], so a bare, unwrapped consumer still gets the
 *   intended dark surface. Set `false` only when embedding `AeroTheme {}` inside a slot whose
 *   background is already painted by the caller.
 */
@Composable
public fun AeroTheme(
    colorScheme: AeroColorScheme = AeroColorScheme.AeroBlue,
    typography: AeroTypography = AeroTypography(),
    establishBackground: Boolean = true,
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
            typography = materialTypography
        ) {
            if (establishBackground) {
                // Plain Box, not Material3 Surface — see the establishBackground KDoc above for
                // why Surface's propagateMinConstraints = true is unsafe here.
                Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
                    content()
                }
            } else {
                content()
            }
        }
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

    /**
     * Active [AeroOrnamentTokens] — resolves [AeroColorScheme.ornamentOverride] when the
     * current color scheme sets one, otherwise algorithmically derives tokens via
     * [AeroOrnamentTokens.derive] (PRIM-03 escape hatch honored).
     */
    public val ornaments: AeroOrnamentTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAeroColors.current.let { it.ornamentOverride ?: AeroOrnamentTokens.derive(it) }
}
