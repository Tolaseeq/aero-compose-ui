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
 * - G2/VLST-03: [AeroListItem]'s source gates its in-bounds focus stroke on
 *   `state.focusVisible && enabled && onClick != null` — a pointer-acquired focus must not draw
 *   the stroke, while the row stays focusable and clickable either way (19-05/19-06).
 * - G1: [AeroListItem]'s source (comments stripped) still carries the row-growth contract —
 *   `heightIn(min = ROW_MIN_HEIGHT)` and `matchParentSize()` present, and neither `maxLines` nor
 *   `TextOverflow` present — truncation was explicitly rejected because it would mask the layout
 *   defect rather than fix it (19-06).
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), every guard below was proven to FAIL against the shipped, un-restyled
 * `AeroListItem.kt` before being trusted — see 19-01-SUMMARY.md "Guard Fail-Then-Pass Proof"
 * (and 19-06-SUMMARY.md "Guard Fail-Then-Pass Proof" for the two guards added in this plan).
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

    @Test
    fun aeroListItemGatesTheFocusStrokeOnFocusVisible() {
        assertTrue(
            aeroListItemSource.contains("state.focusVisible && enabled && onClick != null"),
            "AeroListItem.kt must gate its in-bounds focus stroke on " +
                "state.focusVisible && enabled && onClick != null so a pointer-acquired focus " +
                "does not draw the stroke, while the row stays focusable and clickable either " +
                "way (G2)"
        )
    }

    @Test
    fun aeroListItemRowGrowthContractSurvives() {
        assertTrue(
            nonCommentSource.contains("heightIn(min = ROW_MIN_HEIGHT)"),
            "AeroListItem.kt must keep .heightIn(min = ROW_MIN_HEIGHT) as its row height " +
                "modifier — truncation was explicitly rejected because it would mask the " +
                "layout defect rather than fix it (G1)"
        )
        assertTrue(
            nonCommentSource.contains("matchParentSize()"),
            "AeroListItem.kt must keep matchParentSize() on the pill/focus Boxes so they grow " +
                "with the row's resolved height instead of collapsing to zero (G1)"
        )
        assertFalse(
            nonCommentSource.contains("maxLines"),
            "AeroListItem.kt must not introduce maxLines — truncation was explicitly rejected " +
                "because it would mask the layout defect rather than fix it (G1)"
        )
        assertFalse(
            nonCommentSource.contains("TextOverflow"),
            "AeroListItem.kt must not introduce TextOverflow — truncation was explicitly " +
                "rejected because it would mask the layout defect rather than fix it (G1)"
        )
    }

    private val aeroListItemSource: String get() = sourceFile("AeroListItem.kt").readText()

    /**
     * [aeroListItemSource] with every comment line (KDoc `*`, line `//`, or a block-comment
     * opener) removed, so negative guards over this property cannot be satisfied or broken by
     * KDoc prose — this file's own KDoc mentions `matchParentSize` and `heightIn` by name.
     */
    private val nonCommentSource: String
        get() = aeroListItemSource
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
            }
            .joinToString("\n")

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
