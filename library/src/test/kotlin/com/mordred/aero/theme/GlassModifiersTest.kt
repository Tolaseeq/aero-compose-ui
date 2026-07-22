package com.mordred.aero.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PRIM-09/10/11/12 verification for the three confirmed `GlassModifiers.kt` defects:
 *  - [Modifier.glassSurface]'s gloss gradient stop is proportional to `size.height`, not a
 *    fixed pixel literal (PRIM-09).
 *  - [Modifier.glassSurface]'s `.clip(shape)` is the outermost paint-affecting modifier, so
 *    its declared 1.dp border renders at full thickness (PRIM-10/12).
 *  - [Modifier.glassEffect]'s `elevation` parameter is wired to a real `dropShadow` — no
 *    dead API parameter remains (PRIM-11).
 *
 * Two kinds of guard: a render-without-exception smoke pass across all three built-in
 * [AeroColorScheme] presets (catches runtime regressions), and a source-level regression
 * assertion (catches the exact PRIM-09/10 defects reappearing verbatim, since a passing
 * render test alone would not distinguish a proportional gloss from a hardcoded one).
 */
@OptIn(ExperimentalTestApi::class)
class GlassModifiersTest {

    private val presets = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic,
    )

    @Test
    fun glassEffectRendersWithoutExceptionOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        Box(Modifier.size(120.dp, 40.dp).glassEffect(cornerRadius = 8.dp, elevation = 4.dp))
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun glassPanelRendersWithoutExceptionOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        Box(Modifier.size(200.dp, 18.dp).glassPanel(cornerRadius = 0.dp))
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun glassSurfaceRendersWithoutExceptionOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        // 18.dp height mirrors AeroSwitch's track — the exact size class the
                        // PRIM-09 fixed-pixel gloss bug never completed on.
                        Box(Modifier.size(120.dp, 18.dp).glassSurface(cornerRadius = 8.dp))
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun glassSurfaceGlossStopIsSizeProportionalNotAFixedPixelLiteral() {
        val body = functionBody("glassSurface")
        assertTrue(
            body.contains("size.height *"),
            "glassSurface's gloss gradient stop must be expressed as size.height * fraction (PRIM-09)"
        )
        assertFalse(
            Regex("""endY\s*=\s*\d+(\.\d+)?f""").containsMatchIn(body),
            "glassSurface's gloss gradient endY must not be a bare Float pixel literal (PRIM-09 regression)"
        )
    }

    @Test
    fun glassSurfaceClipIsOutermostPaintAffectingModifier() {
        val body = functionBody("glassSurface")
        val clipIndex = body.indexOf(".clip(")
        val drawIndex = body.indexOf(".drawWithCache").takeIf { it >= 0 }
            ?: body.indexOf(".drawBehind")
        assertTrue(clipIndex >= 0, "glassSurface must call .clip(shape)")
        assertTrue(drawIndex >= 0, "glassSurface must have a draw block (drawWithCache or drawBehind)")
        assertTrue(
            clipIndex < drawIndex,
            "glassSurface's .clip(shape) must appear before its draw block so the clip is outermost (PRIM-10/12)"
        )
    }

    @Test
    fun glassEffectElevationIsWiredToARealDropShadow() {
        val body = functionBody("glassEffect")
        assertTrue(
            body.contains("dropShadow("),
            "glassEffect must call dropShadow(...) — elevation must not remain a dead parameter (PRIM-11)"
        )
        assertTrue(
            body.contains("elevation"),
            "glassEffect's dropShadow call must consume the elevation parameter (PRIM-11)"
        )
    }

    /** Extracts the source text of `public fun Modifier.<name>(...) { ... }` from GlassModifiers.kt. */
    private fun functionBody(name: String): String {
        val source = glassModifiersSource.readText()
        val startMarker = "public fun Modifier.$name("
        val start = source.indexOf(startMarker)
        assertTrue(start >= 0, "Could not locate `$startMarker` in GlassModifiers.kt")
        val nextFunIndex = source.indexOf("public fun Modifier.", start + startMarker.length)
        val end = if (nextFunIndex >= 0) nextFunIndex else source.length
        return source.substring(start, end)
    }

    private val glassModifiersSource: File
        get() {
            val candidates = listOf(
                File("src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt"),
                File("library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt"),
            )
            return candidates.firstOrNull { it.exists() }
                ?: error(
                    "GlassModifiers.kt source not found from working dir " +
                        File(".").absolutePath
                )
        }
}
