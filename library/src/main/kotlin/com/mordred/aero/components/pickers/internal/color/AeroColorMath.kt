package com.mordred.aero.components.pickers.internal.color

import kotlin.math.max
import kotlin.math.min

/**
 * Pure HSV / RGB / HEX conversion utilities for AeroColorPicker (Phase 8).
 *
 * **HUE CONVENTION (locked Phase 7 plan-01 ADR):**
 * - Hue is in DEGREES `[0f, 360f]` — matches `androidx.compose.ui.graphics.Color.hsv(...)`
 *   which `requirePrecondition`-throws on hue outside `[0f..360f]`.
 * - Saturation, Value, Alpha are in `[0f, 1f]`.
 * - RGB ints are in `[0, 255]`; RGB floats are in `[0f, 1f]` (used by `rgbToHsv`).
 *
 * **PITFALL-15 (HSV drift):** HSV is the single source of truth in `AeroColorPicker`'s
 * internal state. RGB and HEX are derived views — never store both simultaneously.
 * The unit test `AeroColorMathTest.pureRedRoundTripPreservesHueWithinTolerance` validates
 * that `Color.hsv(0,1,1) -> rgb -> rgbToHsv` preserves hue within `0.001f`.
 */

/**
 * RGB -> HSV conversion.
 * @param r red in `[0f, 1f]`
 * @param g green in `[0f, 1f]`
 * @param b blue in `[0f, 1f]`
 * @return Triple(hue in `[0f, 360f]`, saturation in `[0f, 1f]`, value in `[0f, 1f]`).
 */
internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    val h = when {
        delta == 0f -> 0f
        cMax == r   -> 60f * (((g - b) / delta) % 6f)
        cMax == g   -> 60f * (((b - r) / delta) + 2f)
        else        -> 60f * (((r - g) / delta) + 4f)
    }
    val hPositive = if (h < 0f) h + 360f else h

    val s = if (cMax == 0f) 0f else delta / cMax
    val v = cMax
    return Triple(hPositive, s, v)
}

/**
 * HEX -> RGB. Accepts "#RGB", "#RRGGBB", "#RRGGBBAA" (alpha dropped) with or without leading '#'.
 * @return Triple(r, g, b) in `[0, 255]`. Returns `null` on parse failure.
 */
internal fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
    val cleaned = hex.removePrefix("#").trim()
    return when (cleaned.length) {
        3 -> {
            val r = cleaned[0].digitToIntOrNull(16) ?: return null
            val g = cleaned[1].digitToIntOrNull(16) ?: return null
            val b = cleaned[2].digitToIntOrNull(16) ?: return null
            Triple(r * 17, g * 17, b * 17)  // expand "F" -> 0xFF
        }
        6, 8 -> {
            val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
            val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
            val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
            Triple(r, g, b)
        }
        else -> null
    }
}

/**
 * HEX -> RGBA. Accepts the same inputs as [hexToRgb]; when the source has no alpha,
 * alpha defaults to 255 (opaque).
 * @return `intArrayOf(r, g, b, a)` in `[0, 255]`. Returns `null` on parse failure.
 */
internal fun hexToRgba(hex: String): IntArray? {
    val cleaned = hex.removePrefix("#").trim()
    return when (cleaned.length) {
        3 -> {
            val r = cleaned[0].digitToIntOrNull(16) ?: return null
            val g = cleaned[1].digitToIntOrNull(16) ?: return null
            val b = cleaned[2].digitToIntOrNull(16) ?: return null
            intArrayOf(r * 17, g * 17, b * 17, 255)
        }
        6 -> {
            val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
            val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
            val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
            intArrayOf(r, g, b, 255)
        }
        8 -> {
            val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
            val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
            val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
            val a = cleaned.substring(6, 8).toIntOrNull(16) ?: return null
            intArrayOf(r, g, b, a)
        }
        else -> null
    }
}

/**
 * RGB ints (0..255) -> uppercase HEX without '#'. If [alpha] is non-null, the result is `RRGGBBAA`.
 */
internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String {
    val rgb = "%02X%02X%02X".format(r, g, b)
    return if (alpha == null) rgb else "$rgb${"%02X".format(alpha)}"
}
