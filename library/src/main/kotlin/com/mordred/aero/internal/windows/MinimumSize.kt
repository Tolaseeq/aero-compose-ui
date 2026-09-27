package com.mordred.aero.internal.windows

import kotlin.math.roundToInt

/**
 * D-01 (22-CONTEXT.md): "Default resize floor stays 320×240 (dp) for every window on
 * `AeroTitleBar` / `AeroResizeHandles`, including once resizing is handed to Windows — apps
 * that change nothing behave exactly as today. An app may set its own minimum through the
 * standard AWT `window.minimumSize` (`Component.isMinimumSizeSet()` is true); the library
 * then honors that value instead of 320×240, smaller or larger. On the native Windows path
 * the floor is applied in physical pixels at the window's current DPI (320 dp × scale), and
 * the library must not overwrite the app's own `window.minimumSize`. The same resolution
 * rule (app minimum if set, else 320×240) applies to the Compose-side handles."
 *
 * Pure resolution for both paths — no JNA, no Compose imports. The library never writes a
 * window minimum size (T-22-24): it only answers WM_GETMINMAXINFO on the native path and
 * clamps its own Compose drags on the other platforms.
 */
internal const val DEFAULT_MIN_WIDTH_DP: Int = 320
internal const val DEFAULT_MIN_HEIGHT_DP: Int = 240

/**
 * D-01 native path: the ptMinTrackSize pair to leave in `WM_GETMINMAXINFO`'s MINMAXINFO.
 * [appMinimumSet] true — the AWT-filled values unchanged (the app's own minimum, already
 * applied by AWT); false — per axis, the larger of the AWT-filled value and the default
 * floor converted to physical px at [scale] (the window's current DPI / 96).
 */
internal fun resolveMinimumTrackSizePx(
    appMinimumSet: Boolean,
    awtFilledWidthPx: Int,
    awtFilledHeightPx: Int,
    scale: Float,
): Pair<Int, Int> =
    if (appMinimumSet) {
        awtFilledWidthPx to awtFilledHeightPx
    } else {
        maxOf(awtFilledWidthPx, (DEFAULT_MIN_WIDTH_DP * scale).roundToInt()) to
            maxOf(awtFilledHeightPx, (DEFAULT_MIN_HEIGHT_DP * scale).roundToInt())
    }

/**
 * D-01 Compose path (Linux/macOS, and Windows windows whose native chrome is not active):
 * the minimum the drag zones clamp to — the app's own minimum when set, the default floor
 * otherwise. Behavior-neutral for apps that set nothing, which already got 320×240.
 */
internal fun resolveComposeMinimumDp(
    appMinimumSet: Boolean,
    appMinWidth: Int,
    appMinHeight: Int,
): Pair<Float, Float> =
    if (appMinimumSet) {
        appMinWidth.toFloat() to appMinHeight.toFloat()
    } else {
        DEFAULT_MIN_WIDTH_DP.toFloat() to DEFAULT_MIN_HEIGHT_DP.toFloat()
    }
