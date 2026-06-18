package com.mordred.aero.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.input.AeroTextField
import com.mordred.aero.components.pickers.internal.color.AeroHsvColorSquare
import com.mordred.aero.components.pickers.internal.color.AeroHueSlider
import com.mordred.aero.components.pickers.internal.color.DefaultAeroSwatches
import com.mordred.aero.components.pickers.internal.color.hexToRgba
import com.mordred.aero.components.pickers.internal.color.rgbToHex
import com.mordred.aero.components.pickers.internal.color.rgbToHsv
import com.mordred.aero.components.range.AeroSlider
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassPanel
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

// ---------------------------------------------------------------------------
// Public inline color-picker panel (PICK-05/06/07).
// ---------------------------------------------------------------------------

/**
 * Inline color-picker panel wiring the HSV square, hue strip, R/G/B sliders, a HEX
 * field, an optional alpha slider, and a swatch row — all kept in sync from a single
 * HSV(A) source of truth (PITFALL-15). RGB and HEX are derived per emit; no `Color`
 * is ever stored as state (only HSV(A) floats plus a one-shot `before` snapshot for
 * the preview bar).
 *
 * @param value the current color (read once to seed HSV(A) state; subsequent external
 *   changes are not re-seeded — the panel owns its interaction state).
 * @param onValueChange emits the derived [Color] whenever any control moves.
 * @param modifier layout modifier applied to the glass panel.
 * @param enableAlpha when true, adds a checkerboard alpha slider and widens HEX to
 *   `#RRGGBBAA`; when false the emitted color is fully opaque.
 * @param swatches preset colors shown in the swatch row (defaults to [DefaultAeroSwatches]).
 * @param enabled whether the controls are interactive.
 */
@Composable
public fun AeroColorPicker(
    value: Color,
    onValueChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enableAlpha: Boolean = false,
    swatches: List<Color> = DefaultAeroSwatches,
    enabled: Boolean = true,
) {
    val seed = remember { rgbToHsv(value.red, value.green, value.blue) }
    var hue by remember { mutableFloatStateOf(seed.first) }
    var saturation by remember { mutableFloatStateOf(seed.second) }
    var brightness by remember { mutableFloatStateOf(seed.third) }
    var alpha by remember { mutableFloatStateOf(value.alpha) }

    // HEX field local edit buffer; null while not being edited (shows derived value).
    var hexDraft by remember { mutableStateOf<String?>(null) }

    val beforeColor = remember { value }
    val currentColor = safeHsvColor(hue, saturation, brightness, if (enableAlpha) alpha else 1f)

    // Derived emit — never stored.
    LaunchedEffect(hue, saturation, brightness, alpha) {
        onValueChange(safeHsvColor(hue, saturation, brightness, if (enableAlpha) alpha else 1f))
    }

    fun commitHex(text: String) {
        hexToHsv(text)?.let {
            hue = it[0].coerceIn(0f, 360f)
            saturation = it[1]
            brightness = it[2]
            if (enableAlpha) alpha = it[3]
        }
        hexDraft = null
    }

    Column(
        modifier = modifier
            .width(280.dp)
            .glassPanel(cornerRadius = 8.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1. HSV square + hue strip.
        // F12: AeroHsvColorSquare sized to 220dp so the 24dp hue slider fits in 280dp panel
        // (220 + 8 spacedBy + 24 hue + 12+12 outer padding = 276dp, fits within 280dp column).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroHsvColorSquare(
                hue = hue,
                saturation = saturation,
                value = brightness,
                onSatValChange = { s, v ->
                    if (enabled) { saturation = s; brightness = v }
                },
                modifier = Modifier.size(220.dp),
            )
            AeroHueSlider(
                hue = hue,
                onHueChange = { if (enabled) hue = it },
            )
        }

        // 2. Before / after preview bar — labelled Original/Current (F10).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Original", color = AeroTheme.colors.labelText, style = AeroTheme.typography.label)
                Box(Modifier.fillMaxWidth().height(20.dp).background(beforeColor))
            }
            Column(Modifier.weight(1f)) {
                Text("Current", color = AeroTheme.colors.labelText, style = AeroTheme.typography.label)
                Box(Modifier.fillMaxWidth().height(20.dp).background(currentColor))
            }
        }

        // 3. R / G / B sliders driven by the derived currentColor (each valueRange = 0f..255f).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", color = AeroTheme.colors.onSurface, style = AeroTheme.typography.label)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                AeroSlider(
                    value = currentColor.red * 255f,
                    onValueChange = { r ->
                        val (h, s, v) = rgbToHsv(r / 255f, currentColor.green, currentColor.blue)
                        hue = h.coerceIn(0f, 360f); saturation = s; brightness = v
                    },
                    valueRange = 0f..255f,
                    enabled = enabled,
                    showTooltip = false,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = AeroTheme.colors.onSurface, style = AeroTheme.typography.label)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                AeroSlider(
                    value = currentColor.green * 255f,
                    onValueChange = { g ->
                        val (h, s, v) = rgbToHsv(currentColor.red, g / 255f, currentColor.blue)
                        hue = h.coerceIn(0f, 360f); saturation = s; brightness = v
                    },
                    valueRange = 0f..255f,
                    enabled = enabled,
                    showTooltip = false,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", color = AeroTheme.colors.onSurface, style = AeroTheme.typography.label)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f)) {
                AeroSlider(
                    value = currentColor.blue * 255f,
                    onValueChange = { b ->
                        val (h, s, v) = rgbToHsv(currentColor.red, currentColor.green, b / 255f)
                        hue = h.coerceIn(0f, 360f); saturation = s; brightness = v
                    },
                    valueRange = 0f..255f,
                    enabled = enabled,
                    showTooltip = false,
                )
            }
        }

        // 4. HEX input.
        AeroTextField(
            value = hexDraft ?: "#" + hsvToHex(hue, saturation, brightness, alpha, enableAlpha),
            onValueChange = { hexDraft = it },
            modifier = Modifier
                .width(140.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        hexDraft?.let { commitHex(it) }
                        true
                    } else {
                        false
                    }
                }
                .onFocusChanged { focus ->
                    if (!focus.isFocused) hexDraft?.let { commitHex(it) }
                },
            enabled = enabled,
            placeholder = if (enableAlpha) "#RRGGBBAA" else "#RRGGBB",
        )

        // 5. Optional alpha slider over a checkerboard backdrop.
        if (enableAlpha) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", color = AeroTheme.colors.onSurface, style = AeroTheme.typography.label)
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f).drawBehind { drawCheckerboard() }) {
                    AeroSlider(
                        value = alpha,
                        onValueChange = { if (enabled) alpha = it },
                        valueRange = 0f..1f,
                        enabled = enabled,
                    )
                }
            }
        }

        // 6. Swatch row.
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            swatches.forEach { sw ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(sw)
                        .clickable(enabled = enabled) {
                            val (h, s, v) = rgbToHsv(sw.red, sw.green, sw.blue)
                            hue = h.coerceIn(0f, 360f); saturation = s; brightness = v
                            if (enableAlpha) alpha = sw.alpha
                            hexDraft = null
                        },
                )
            }
        }
    }
}

/** Draws an 8dp alternating white / light-gray checkerboard behind the alpha slider. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheckerboard() {
    val tile = 8.dp.toPx()
    val cols = (size.width / tile).toInt() + 1
    val rows = (size.height / tile).toInt() + 1
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val light = (row + col) % 2 == 0
            drawRect(
                color = if (light) Color.White else Color(0xFFCCCCCC),
                topLeft = Offset(col * tile, row * tile),
                size = Size(tile, tile),
            )
        }
    }
}
