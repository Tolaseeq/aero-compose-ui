package com.mordred.aero.components.pickers

import androidx.compose.ui.graphics.Color
import com.mordred.aero.components.pickers.internal.color.hexToRgba
import com.mordred.aero.components.pickers.internal.color.rgbToHex
import com.mordred.aero.components.pickers.internal.color.rgbToHsv
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Pure HSV<->Color<->HEX derivation helpers (PITFALL-15).
//
// AeroColorPicker stores ONLY HSV(A) floats as state; RGB and HEX are derived
// views produced on every emit. These helpers are pure and unit-testable
// without a Compose UI harness.
// ---------------------------------------------------------------------------

/**
 * Builds a [Color] from HSV(A), coercing every channel into its legal range first.
 *
 * NEW-PICK-01: `Color.hsv` `requirePrecondition`-throws on hue outside `[0f, 360f]`.
 * This wrapper guarantees no throw — hue is clamped to `[0f, 360f]`, sat/value/alpha
 * to `[0f, 1f]`.
 */
internal fun safeHsvColor(hue: Float, sat: Float, v: Float, alpha: Float = 1f): Color =
    Color.hsv(
        hue.coerceIn(0f, 360f),
        sat.coerceIn(0f, 1f),
        v.coerceIn(0f, 1f),
        alpha.coerceIn(0f, 1f),
    )

/**
 * Derives an uppercase HEX string (no `#`) from HSV(A).
 * @return `RRGGBB` when [includeAlpha] is false, `RRGGBBAA` when true.
 */
internal fun hsvToHex(hue: Float, sat: Float, v: Float, alpha: Float, includeAlpha: Boolean): String {
    val color = safeHsvColor(hue, sat, v, alpha)
    val r = (color.red * 255f).roundToInt()
    val g = (color.green * 255f).roundToInt()
    val b = (color.blue * 255f).roundToInt()
    val a = (color.alpha * 255f).roundToInt()
    return rgbToHex(r, g, b, if (includeAlpha) a else null)
}

/**
 * Parses a HEX string into `[hue, sat, value, alpha]`. Accepts an optional leading `#`;
 * only 6-digit (`RRGGBB`) and 8-digit (`RRGGBBAA`) cleaned forms are accepted — any other
 * length (e.g. the partial `#FF`) returns `null`.
 */
internal fun hexToHsv(hex: String): FloatArray? {
    val cleaned = hex.removePrefix("#").trim()
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val rgba = hexToRgba(cleaned) ?: return null
    val (hue, sat, value) = rgbToHsv(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f)
    return floatArrayOf(hue, sat, value, rgba[3] / 255f)
}
