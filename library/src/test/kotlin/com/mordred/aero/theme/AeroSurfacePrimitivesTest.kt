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
 * PRIM-06/07 verification for the 16-02 plan's expansion of the shared draw core (Task 1):
 *  - [Modifier.aeroGlowRing] renders without exception both active and inactive, and never
 *    drives itself from a `rememberInfiniteTransition` (local-DoS avoidance, PRIM-06).
 *  - [Modifier.aeroThumbSurface]/[drawAeroThumb] render a raised circular surface and reuse
 *    [drawAeroSurfaceCore]'s shared fill/gloss/bevel path — never a second gradient
 *    implementation (PRIM-07).
 *  - No new gradient in this file fades to `Color.Transparent` (PRIM-14).
 *
 * Task 2 extends this file with PRIM-08 groove coverage.
 *
 * Mirrors [GlassModifiersTest]'s two-pronged approach: render-without-exception smoke passes
 * across all three built-in [AeroColorScheme] presets, plus source-level regression assertions
 * that a passing render test alone would not distinguish from a bespoke/wrong implementation.
 */
@OptIn(ExperimentalTestApi::class)
class AeroSurfacePrimitivesTest {

    private val presets = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic,
    )

    @Test
    fun aeroGlowRingRendersWithoutExceptionWhenActiveOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        val ornaments = AeroOrnamentTokens.derive(scheme)
                        Box(
                            Modifier
                                .size(24.dp)
                                .aeroGlowRing(active = true, glowColor = ornaments.hoverGlow, cornerRadius = 12.dp)
                        )
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun aeroGlowRingRendersWithoutExceptionWhenInactiveOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        val ornaments = AeroOrnamentTokens.derive(scheme)
                        Box(
                            Modifier
                                .size(24.dp)
                                .aeroGlowRing(active = false, glowColor = ornaments.hoverGlow, cornerRadius = 12.dp)
                        )
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun aeroThumbSurfaceRendersWithoutExceptionOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .aeroThumbSurface(style = AeroSurfaceStyle.rest(scheme, cornerRadius = 9.dp))
                        )
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun aeroThumbSurfaceReusesSharedDrawCoreNotAFreshGradientConstructor() {
        val body = functionBody("aeroThumbSurface") + functionBody("drawAeroThumb")
        assertTrue(
            body.contains("drawAeroSurfaceCore"),
            "aeroThumbSurface/drawAeroThumb must call the shared drawAeroSurfaceCore, not a fresh gradient implementation (PRIM-07)"
        )
    }

    @Test
    fun noRememberInfiniteTransitionInAeroSurfacePrimitives() {
        assertFalse(
            aeroSurfacePrimitivesSource.readText().contains("rememberInfiniteTransition"),
            "aeroGlowRing must not be driven by a rememberInfiniteTransition (local-DoS avoidance, PRIM-06 prohibition)"
        )
    }

    @Test
    fun noColorTransparentInNewPrimitiveFunctionBodies() {
        // Scoped to function bodies (not KDoc prose, which legitimately names the anti-pattern
        // it warns against) — a whole-file scan would false-positive on those doc comments.
        val body = functionBody("aeroGlowRing") + functionBody("drawAeroThumb") + functionBody("aeroThumbSurface")
        assertFalse(
            Regex("""Color\.Transparent""").containsMatchIn(body),
            "New primitives must fade to baseColor.copy(alpha = 0f), never Color.Transparent (PRIM-14)"
        )
    }

    /** Extracts the source text of a `fun ... <name>(...) { ... }` declaration from AeroSurfacePrimitives.kt. */
    private fun functionBody(name: String): String {
        val source = aeroSurfacePrimitivesSource.readText()
        val markers = listOf(
            "public fun Modifier.$name(",
            "internal fun DrawScope.$name(",
            "public fun DrawScope.$name(",
            "internal fun Modifier.$name(",
        )
        val marker = markers.firstOrNull { source.contains(it) }
            ?: error("Could not locate a fun ...$name( declaration in AeroSurfacePrimitives.kt")
        val start = source.indexOf(marker)
        val nextFunIndex = Regex("""\n(public|internal) fun """).find(source, start + marker.length)?.range?.first
        val end = nextFunIndex ?: source.length
        return source.substring(start, end)
    }

    private val aeroSurfacePrimitivesSource: File
        get() {
            val candidates = listOf(
                File("src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt"),
                File("library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt"),
            )
            return candidates.firstOrNull { it.exists() }
                ?: error(
                    "AeroSurfacePrimitives.kt source not found from working dir " +
                        File(".").absolutePath
                )
        }
}
