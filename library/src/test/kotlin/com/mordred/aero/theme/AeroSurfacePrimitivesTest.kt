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
 * PRIM-06/07/08 verification for the 16-02 plan's expansion of the shared draw core:
 *  - [Modifier.aeroGlowRing] renders without exception both active and inactive, and never
 *    drives itself from a `rememberInfiniteTransition` (local-DoS avoidance, PRIM-06).
 *  - [Modifier.aeroThumbSurface]/[drawAeroThumb] render a raised circular surface and reuse
 *    [drawAeroSurfaceCore]'s shared fill/gloss/bevel path — never a second gradient
 *    implementation (PRIM-07).
 *  - [Modifier.aeroGroove]/`drawAeroGroove` render a recessed track bed, callable both via
 *    Modifier and directly inside a Canvas DrawScope, and also reuse [drawAeroSurfaceCore]
 *    (PRIM-08).
 *  - No new gradient in this file fades to `Color.Transparent` (PRIM-14).
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
    fun aeroGrooveRendersWithoutExceptionOnAllThreePresets() {
        presets.forEach { (_, scheme) ->
            runComposeUiTest {
                setContent {
                    AeroTheme(colorScheme = scheme) {
                        Box(
                            Modifier
                                .size(width = 120.dp, height = 8.dp)
                                .aeroGroove(style = AeroSurfaceStyle.rest(scheme, cornerRadius = 4.dp))
                        )
                    }
                }
                waitForIdle()
            }
        }
    }

    @Test
    fun aeroGrooveReusesSharedDrawCoreNotAFreshGradientConstructor() {
        val body = functionBody("aeroGroove") + functionBody("drawAeroGroove")
        assertTrue(
            body.contains("drawAeroSurfaceCore"),
            "aeroGroove/drawAeroGroove must call the shared drawAeroSurfaceCore, not a fresh gradient implementation (PRIM-08)"
        )
    }

    @Test
    fun drawAeroGrooveHasADrawScopeReceiverCallableInsideCanvas() {
        val source = aeroSurfacePrimitivesSource.readText()
        assertTrue(
            source.contains("fun DrawScope.drawAeroGroove("),
            "drawAeroGroove must be a DrawScope extension function so Canvas-owning consumers " +
                "(e.g. AeroRangeSlider-style call sites) can call it directly, matching " +
                "drawAeroSurfaceCore's dual Modifier/direct-Canvas convention (PRIM-08)"
        )
    }

    @Test
    fun noColorTransparentInGrooveFunctionBodies() {
        val body = functionBody("aeroGroove") + functionBody("drawAeroGroove")
        assertFalse(
            Regex("""Color\.Transparent""").containsMatchIn(body),
            "The groove primitive must fade to baseColor.copy(alpha = 0f), never Color.Transparent (PRIM-14)"
        )
    }

    @Test
    fun noRememberInfiniteTransitionInAeroGlowRingBody() {
        // Scoped to the function body (not KDoc prose, which legitimately names the
        // anti-pattern it warns against) — a whole-file scan would false-positive on that doc.
        // Covers both the Modifier wrapper and drawAeroGlowRing (the direct-DrawScope
        // implementation it delegates to, added 18-04) — the real draw logic lives in the latter.
        val body = functionBody("aeroGlowRing") + functionBody("drawAeroGlowRing")
        assertFalse(
            body.contains("rememberInfiniteTransition"),
            "aeroGlowRing must not be driven by a rememberInfiniteTransition (local-DoS avoidance, PRIM-06 prohibition)"
        )
    }

    @Test
    fun aeroGlowRingOuterBloomIsNotASingleGradientCenteredOnTheBox() {
        // Regression guard (16-05 UAT gap-fix): a single Brush.radialGradient centered at the
        // box's own center fades to near-zero alpha by the time it reaches the actual perimeter
        // stroke location for anything but a perfect square — measured as byte-identical to
        // background immediately outside an aeroGlowRing card. The outer bloom must instead be
        // solid-color concentric rings so brightness lands AT the perimeter. Scoped to both the
        // Modifier wrapper and drawAeroGlowRing (18-04), where the bloom draw calls now live.
        val body = functionBody("aeroGlowRing") + functionBody("drawAeroGlowRing")
        assertFalse(
            Regex("""Brush\.radialGradient""").containsMatchIn(body),
            "aeroGlowRing's outer bloom must not rely on a single radial gradient centered on " +
                "the box (16-05 UAT gap-fix regression guard)"
        )
    }

    @Test
    fun aeroGlowRingOuterBloomUsesMultipleConcentricRingsNotASingleStroke() {
        // A single stroke cannot produce a soft outward falloff — the bloom needs several rings.
        val source = aeroSurfacePrimitivesSource.readText()
        val layerCount = Regex("""GLOW_RING_BLOOM_LAYERS\s*=\s*(\d+)""")
            .find(source)?.groupValues?.get(1)?.toInt()
            ?: error("Could not find GLOW_RING_BLOOM_LAYERS constant in AeroSurfacePrimitives.kt")
        assertTrue(
            layerCount >= 3,
            "aeroGlowRing's outer bloom must use several concentric rings (poor-man's blur) to " +
                "produce a soft outward falloff, not a single stroke"
        )
    }

    @Test
    fun aeroGlowRingFirstBloomRingTouchesTheSurfaceEdgeWithNoDeadGap() {
        // Regression guard (16-05 UAT gap-fix): the first bloom ring's offset must be anchored
        // to the surface's own edge (its own half-width), not pushed several dp away by a fixed
        // outset before any bloom appears — the exact "zero brightness immediately outside the
        // card" defect measured during the three-theme sign-off.
        // The bloom-ring draw logic lives in drawAeroGlowRing (the direct-DrawScope
        // implementation Modifier.aeroGlowRing delegates to, added for AeroRangeSlider's
        // per-thumb glow parity, 18-04) — assert against that function, not the thin Modifier
        // wrapper which no longer contains the bloom math itself.
        val body = functionBody("drawAeroGlowRing")
        assertTrue(
            body.contains("bloomStrokePx / 2f"),
            "The first bloom ring must start at the surface's own edge (offset by only its own " +
                "half-stroke-width), not a fixed dead-zone outset (16-05 UAT gap-fix regression guard)"
        )
    }

    @Test
    fun noColorTransparentInNewPrimitiveFunctionBodies() {
        // Scoped to function bodies (not KDoc prose, which legitimately names the anti-pattern
        // it warns against) — a whole-file scan would false-positive on those doc comments.
        val body = functionBody("aeroGlowRing") + functionBody("drawAeroGlowRing") +
            functionBody("drawAeroThumb") + functionBody("aeroThumbSurface")
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
