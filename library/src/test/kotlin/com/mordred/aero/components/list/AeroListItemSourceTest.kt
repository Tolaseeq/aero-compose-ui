package com.mordred.aero.components.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards mirroring
 * [com.mordred.aero.components.range.AeroSliderSourceTest]'s `sourceFile()` convention:
 *
 * - VLST-01: [AeroListItem]'s source calls `aeroSurface(` — the shared painter clips the
 *   selection/hover pill, replacing the unclipped, full-bleed `.background(animatedBg)` it
 *   shipped with (D-12).
 * - D-11: [AeroListItem]'s source calls `resolveListItemPillStyle(` — the component delegates
 *   to the pure resolver rather than picking colors inline via a three-way `when` branch. A
 *   positive "delegates to the resolver" check, chosen over a fragile substring match on the
 *   removed branch shape itself.
 * - PRIM-14: [AeroListItem]'s source does NOT contain the Compose fully-transparent color
 *   constant — every gradient fade must target `baseColor.copy(alpha = 0f)` instead. Currently
 *   violated at the shipped file's line 63.
 * - D-13: [AeroListItem]'s source does NOT call `aeroGlowRing(` — list rows live inside
 *   scrolling/clipping containers that would slice an outer bloom; the focus cue is in-bounds
 *   only.
 * - VLST-04: [AeroListItem]'s source still contains `.hoverable(` and does NOT contain
 *   `pointerInput` or `awaitPointerEventScope` — hover stays wired through
 *   `Modifier.hoverable` + the interaction-source collector, never raw pointer-position
 *   tracking.
 * - P-01: [AeroListItem]'s source contains `indication = null` — the custom-painted hover/press
 *   cue replaces the platform indication rather than doubling up with it.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), every guard below was proven to FAIL against the shipped, un-restyled
 * `AeroListItem.kt` before being trusted — see 19-01-SUMMARY.md "Guard Fail-Then-Pass Proof".
 */
class AeroListItemSourceTest {

    @Test
    fun aeroListItemUsesAeroSurfaceForItsPill() {
        assertTrue(
            aeroListItemSource.contains("aeroSurface("),
            "AeroListItem.kt must call aeroSurface( to clip its selection/hover pill (VLST-01)"
        )
    }

    @Test
    fun aeroListItemDelegatesToTheResolver() {
        assertTrue(
            aeroListItemSource.contains("resolveListItemPillStyle("),
            "AeroListItem.kt must delegate pill style resolution to resolveListItemPillStyle( " +
                "rather than picking colors inline (D-11)"
        )
    }

    @Test
    fun aeroListItemDoesNotUseTheFullyTransparentColorConstant() {
        assertFalse(
            aeroListItemSource.contains("Color.Transparent"),
            "AeroListItem.kt must not use Color.Transparent as a gradient fade target — fade to " +
                "baseColor.copy(alpha = 0f) instead (PRIM-14); Color.Transparent renders a flat, " +
                "wrong-colored block on Classic's fully-opaque tokens"
        )
    }

    @Test
    fun aeroListItemDoesNotUseAeroGlowRing() {
        assertFalse(
            aeroListItemSource.contains("aeroGlowRing("),
            "AeroListItem.kt must not call aeroGlowRing( — list rows live inside scrolling/" +
                "clipping containers that would slice an outer bloom; the focus cue is an " +
                "in-bounds stroke instead (D-13)"
        )
    }

    @Test
    fun aeroListItemKeepsHoverableAndAvoidsRawPointerTracking() {
        assertTrue(
            aeroListItemSource.contains(".hoverable("),
            "AeroListItem.kt must keep Modifier.hoverable(interactionSource) as its hover source (VLST-04)"
        )
        assertFalse(
            aeroListItemSource.contains("pointerInput"),
            "AeroListItem.kt must not replace hoverable + collectIsHoveredAsState with pointerInput (VLST-04)"
        )
        assertFalse(
            aeroListItemSource.contains("awaitPointerEventScope"),
            "AeroListItem.kt must not replace hoverable + collectIsHoveredAsState with raw pointer-event tracking (VLST-04)"
        )
    }

    @Test
    fun aeroListItemSuppressesPlatformIndication() {
        assertTrue(
            aeroListItemSource.contains("indication = null"),
            "AeroListItem.kt's clickable call must pass indication = null so the platform " +
                "indication does not double up with the custom-painted hover/press cues (P-01)"
        )
    }

    private val aeroListItemSource: String get() = sourceFile("AeroListItem.kt").readText()

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/list/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/list/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
