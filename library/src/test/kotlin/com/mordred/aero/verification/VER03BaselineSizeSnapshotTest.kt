package com.mordred.aero.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.mordred.aero.components.buttons.AeroButton
import com.mordred.aero.components.buttons.AeroOutlinedButton
import com.mordred.aero.components.range.AeroProgressBar
import com.mordred.aero.components.range.AeroRangeSlider
import com.mordred.aero.components.selection.AeroSegmentedControl
import com.mordred.aero.components.selection.AeroSwitch
import com.mordred.aero.theme.AeroTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * VER-03 snapshot gate: every measured default size and value-level corner radius must stay
 * EXACTLY equal to its pre-migration [BASELINE] value — the real numbers read directly out of git
 * tag `v2.0.4` (D-10), never a frozen-current snapshot of today's code, since a frozen-current
 * baseline is structurally unable to detect drift that already happened during Phases 15-19.
 *
 * **[BASELINE] is keyed by `"<Component>.<property>"` identity, never by list position** — three
 * of its fourteen entries happen to share the same numeric value with a sibling entry (4.dp:
 * `AeroButton.cornerRadius` / `AeroOutlinedButton.cornerRadius` / `AeroSegmentedControl.cornerRadius`;
 * 28.dp: `AeroOutlinedButton.height` / `AeroSegmentedControl.height`; 36.dp: `AeroSwitch.trackWidth` /
 * `AeroListItem.rowMinHeight`) and each is still asserted completely independently — see
 * [twoCoincidentBaselineKeysDriftingOppositeDirectionsAreBothCaught] below for the fixture proving a
 * comparator that summed instead of comparing per key would miss a real regression here.
 *
 * **Measurement density is pinned** (`CompositionLocalProvider(LocalDensity provides Density(1f))`)
 * so the dp readback is deterministic and an EXACT comparison is structurally possible, never a
 * fuzzy-margin comparison that could silently absorb sub-dp rounding drift.
 *
 * **Two documented findings — recorded, not hidden. No further exception is authorized.**
 * - `AeroListItem`'s row height moved from a FIXED `.height(36.dp)` at v2.0.4 to today's
 *   `.heightIn(min = ROW_MIN_HEIGHT)` (`ROW_MIN_HEIGHT = 36.dp`) — the ONE approved VER-03
 *   exception, citing the G1 closure at Phase 19 Plan 06 (`19-06`: the row had to grow so a
 *   secondary line or wrapped label fits inside the selection pill) and the maintainer's decision
 *   not to reopen it. This gate therefore asserts the constant is still exactly 36.dp AND that the
 *   declaration is a `heightIn(min = …)` floor, never a fixed height again.
 * - `AeroSlider` declared NO numeric size default of its own at v2.0.4 — direct inspection of
 *   `git show v2.0.4:library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt`
 *   confirms it wrapped Material3's `Slider` with `SliderDefaults.colors(...)` only, so no
 *   pre-migration number exists for `THUMB_DIAMETER = 20.dp` / `TRACK_HEIGHT = 4.dp` to be compared
 *   against. Those values are Phase-18-approved substitutions matching `AeroRangeSlider`'s locked
 *   dimensions (`18-01`), not silent creep, and `AeroSlider` is deliberately absent from [BASELINE]
 *   rather than silently included with a fabricated number.
 *
 * **No other exception is authorized by this plan.** Every other entry in [BASELINE] must match
 * exactly; a one-dp divergence anywhere else turns this gate RED, and the fix is to correct the
 * drifted component, never to add a new table entry to make it pass again.
 *
 * **D-08 fixture proof, in this file** (VER-06) — mirrors the three-layer shape this phase's Wave 1
 * gates ([com.mordred.aero.verification.VER01GradientProportionalitySourceTest],
 * [com.mordred.aero.verification.VER02AeroSurfaceClipOrderSourceTest]) established and the user
 * approved at their Task 1 checkpoint: a pure comparison function
 * ([baselineDeviations]) proven RED against off-baseline and empty fixtures and GREEN against an
 * exact-match fixture, all living in this class and re-executing on every build, plus the REAL
 * assertion against composed nodes and comment-stripped source text.
 */
@OptIn(ExperimentalTestApi::class)
class VER03BaselineSizeSnapshotTest {

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — GREEN half.
    // ---------------------------------------------------------------------------------------

    @Test
    fun cleanFixtureMatchingBaselineIsEmpty() {
        val violations = baselineDeviations(BASELINE, BASELINE)
        assertTrue(
            violations.isEmpty(),
            "A measured map identical to BASELINE must produce zero deviations (VER-03)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — RED half.
    // ---------------------------------------------------------------------------------------

    @Test
    fun oneKeyOneDpAboveBaselineIsFlagged() {
        val fixture = baselineShiftedBy("AeroButton.height", shiftDp = 1f)
        val violations = baselineDeviations(fixture, BASELINE)
        assertTrue(
            violations.isNotEmpty() && violations.any { it.contains("AeroButton.height") },
            "A measured value exactly 1dp ABOVE its baseline must be flagged — exact-equality " +
                "comparison, no fuzzy margin (VER-03). Differs from the clean fixture by " +
                "exactly one key's value (VER-06/adjacency)."
        )
    }

    @Test
    fun oneKeyOneDpBelowBaselineIsFlagged() {
        val fixture = baselineShiftedBy("AeroSwitch.trackHeight", shiftDp = -1f)
        val violations = baselineDeviations(fixture, BASELINE)
        assertTrue(
            violations.isNotEmpty() && violations.any { it.contains("AeroSwitch.trackHeight") },
            "A measured value exactly 1dp BELOW its baseline must be flagged — exact-equality " +
                "comparison, no fuzzy margin (VER-03). Differs from the clean fixture by " +
                "exactly one key's value (VER-06/adjacency)."
        )
    }

    @Test
    fun emptyMeasuredMapIsFlagged() {
        val violations = baselineDeviations(emptyMap(), BASELINE)
        assertTrue(
            violations.isNotEmpty(),
            "An empty measurement set must fail rather than trivially pass (VER-03)"
        )
    }

    @Test
    fun twoCoincidentBaselineKeysDriftingOppositeDirectionsAreBothCaught() {
        // AeroButton.cornerRadius and AeroSegmentedControl.cornerRadius share the identical 4.dp
        // baseline (VER-03's own "two components whose baseline values happen to coincide" edge
        // report). A comparator that summed/averaged deviations instead of comparing each key
        // independently would see the total unchanged (5.dp + 3.dp == 4.dp + 4.dp) and wrongly
        // pass; baselineDeviations is keyed by identity and must flag BOTH regardless of list
        // position (VER-03/adjacency).
        val fixture = BASELINE.toMutableMap()
        fixture["AeroButton.cornerRadius"] = 5.dp
        fixture["AeroSegmentedControl.cornerRadius"] = 3.dp
        val violations = baselineDeviations(fixture, BASELINE)
        assertTrue(
            violations.any { it.contains("AeroButton.cornerRadius") } &&
                violations.any { it.contains("AeroSegmentedControl.cornerRadius") },
            "Both coincident-baseline keys must be flagged independently, keyed by component " +
                "identity, not cancelled out by an aggregate comparison (VER-03): $violations"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real measurement + source scan.
    // ---------------------------------------------------------------------------------------

    @Test
    fun realMeasuredSizesAndRadiiMatchV204Baseline() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                AeroTheme {
                    Column(Modifier.width(400.dp)) {
                        AeroButton(
                            text = "Save",
                            onClick = {},
                            modifier = Modifier.testTag("AeroButton.height"),
                        )
                        AeroOutlinedButton(
                            text = "Cancel",
                            onClick = {},
                            modifier = Modifier.testTag("AeroOutlinedButton.height"),
                        )
                        AeroSwitch(
                            checked = false,
                            onCheckedChange = {},
                            modifier = Modifier.testTag("AeroSwitch.track"),
                        )
                        AeroSegmentedControl(
                            options = listOf("A", "B"),
                            selected = "A",
                            onSelect = {},
                            modifier = Modifier.testTag("AeroSegmentedControl.height"),
                        )
                        AeroProgressBar(
                            progress = 0.5f,
                            showPercent = false,
                            modifier = Modifier.testTag("AeroProgressBar.height"),
                        )
                        AeroRangeSlider(
                            value = 0.2f..0.8f,
                            onValueChange = {},
                            modifier = Modifier.testTag("AeroRangeSlider.height"),
                        )
                    }
                }
            }
        }
        waitForIdle()

        // Sizes read off the composed layout (step 2) — the point is MEASURING, not reading the
        // declared number back: this is how AeroListItem's v3.0 row growth was originally caught,
        // where the declared constant was right and the composed row grew past it anyway.
        val measuredComposed = mapOf(
            "AeroButton.height" to onNodeWithTag("AeroButton.height").getUnclippedBoundsInRoot().height,
            "AeroOutlinedButton.height" to onNodeWithTag("AeroOutlinedButton.height").getUnclippedBoundsInRoot().height,
            "AeroSwitch.trackWidth" to onNodeWithTag("AeroSwitch.track").getUnclippedBoundsInRoot().width,
            "AeroSwitch.trackHeight" to onNodeWithTag("AeroSwitch.track").getUnclippedBoundsInRoot().height,
            "AeroSegmentedControl.height" to onNodeWithTag("AeroSegmentedControl.height").getUnclippedBoundsInRoot().height,
            "AeroProgressBar.height" to onNodeWithTag("AeroProgressBar.height").getUnclippedBoundsInRoot().height,
            "AeroRangeSlider.height" to onNodeWithTag("AeroRangeSlider.height").getUnclippedBoundsInRoot().height,
        )

        // Value-level radii/sizes (step 3) — a corner radius never participates in layout and
        // cannot be read off a composed node, and the constants that carry them are private, so
        // these are asserted from comment-stripped source text via the cwd-independent sourceFile()
        // resolver, mirroring Plan 20-01's stripComments shape.
        val buttonSurfaceSource = sourceFile("com/mordred/aero/components/buttons/AeroButtonSurface.kt").readText()
        val switchSource = sourceFile("com/mordred/aero/components/selection/AeroSwitch.kt").readText()
        val segmentedSource = sourceFile("com/mordred/aero/components/selection/AeroSegmentedControl.kt").readText()
        val listItemSource = sourceFile("com/mordred/aero/components/list/AeroListItem.kt").readText()

        val strippedButtonSurface = stripComments(buttonSurfaceSource)
        val strippedSwitch = stripComments(switchSource)
        val strippedSegmented = stripComments(segmentedSource)
        val strippedListItem = stripComments(listItemSource)

        // Exact declaration substrings — a KDoc mentioning a value cannot satisfy these (comments
        // are stripped first), and a reformatted-but-drifted declaration cannot slip past silently.
        assertTrue(
            strippedButtonSurface.contains("RoundedCornerShape(4.dp)"),
            "AeroButtonSurface.kt must declare RoundedCornerShape(4.dp) verbatim (VER-03) — shared " +
                "by AeroButton and AeroOutlinedButton, both painted by this one surface."
        )
        assertTrue(
            strippedSwitch.contains("TRACK_CORNER_RADIUS = 9.dp"),
            "AeroSwitch.kt must declare TRACK_CORNER_RADIUS = 9.dp verbatim (VER-03) — half of the " +
                "18.dp track height, geometrically identical to v2.0.4's RoundedCornerShape(50) pill."
        )
        assertTrue(
            strippedSwitch.contains("THUMB_CORNER_RADIUS = 7.dp"),
            "AeroSwitch.kt must declare THUMB_CORNER_RADIUS = 7.dp verbatim (VER-03) — half of the " +
                "14.dp thumb diameter, geometrically identical to v2.0.4's RoundedCornerShape(50) pill."
        )
        assertTrue(
            strippedSwitch.contains(".size(14.dp)"),
            "AeroSwitch.kt must declare .size(14.dp) verbatim for the thumb (VER-03)"
        )
        assertTrue(
            strippedSegmented.contains("SEGMENT_CORNER_RADIUS = 4.dp"),
            "AeroSegmentedControl.kt must declare SEGMENT_CORNER_RADIUS = 4.dp verbatim (VER-03)"
        )
        assertTrue(
            strippedListItem.contains("ROW_MIN_HEIGHT: Dp = 36.dp"),
            "AeroListItem.kt must declare ROW_MIN_HEIGHT: Dp = 36.dp verbatim (VER-03)"
        )
        assertTrue(
            strippedListItem.contains("heightIn(min"),
            "AeroListItem.kt's row height must remain a MINIMUM (heightIn(min = …)), not a fixed " +
                "height — the one approved VER-03 exception (G1, 19-06)."
        )

        val measuredSourceLevel = mapOf(
            "AeroButton.cornerRadius" to dpLiteralAfter(buttonSurfaceSource, "RoundedCornerShape("),
            "AeroOutlinedButton.cornerRadius" to dpLiteralAfter(buttonSurfaceSource, "RoundedCornerShape("),
            "AeroSwitch.thumbSize" to dpLiteralAfter(switchSource, ".size("),
            "AeroSwitch.trackCornerRadius" to dpLiteralAfter(switchSource, "TRACK_CORNER_RADIUS ="),
            "AeroSwitch.thumbCornerRadius" to dpLiteralAfter(switchSource, "THUMB_CORNER_RADIUS ="),
            "AeroSegmentedControl.cornerRadius" to dpLiteralAfter(segmentedSource, "SEGMENT_CORNER_RADIUS ="),
            "AeroListItem.rowMinHeight" to dpLiteralAfter(listItemSource, "ROW_MIN_HEIGHT: Dp ="),
        )

        val measured = measuredComposed + measuredSourceLevel
        assertEquals(
            BASELINE.keys,
            measured.keys,
            "measured.keys must equal BASELINE.keys, reordering must not change the verdict (VER-03)"
        )
        assertTrue(
            measured.isNotEmpty(),
            "measured map must be non-empty — an empty measurement set must never pass vacuously (VER-03)"
        )

        val deviations = baselineDeviations(measured, BASELINE)
        assertTrue(
            deviations.isEmpty(),
            "Found VER-03 baseline deviations against the v2.0.4 tag (D-10):\n" +
                deviations.joinToString("\n")
        )
    }
}

/**
 * The real pre-migration values (D-10), read directly out of git tag `v2.0.4` — never a frozen
 * snapshot of current code. Keyed by `"<Component>.<property>"` identity so list position never
 * affects the verdict and coincident values (see the class KDoc) are asserted independently.
 */
private val BASELINE: Map<String, Dp> = mapOf(
    "AeroButton.height" to 30.dp,
    "AeroButton.cornerRadius" to 4.dp,
    "AeroOutlinedButton.height" to 28.dp,
    "AeroOutlinedButton.cornerRadius" to 4.dp,
    "AeroSwitch.trackWidth" to 36.dp,
    "AeroSwitch.trackHeight" to 18.dp,
    "AeroSwitch.thumbSize" to 14.dp,
    "AeroSwitch.trackCornerRadius" to 9.dp,
    "AeroSwitch.thumbCornerRadius" to 7.dp,
    "AeroSegmentedControl.height" to 28.dp,
    "AeroSegmentedControl.cornerRadius" to 4.dp,
    "AeroProgressBar.height" to 8.dp,
    "AeroRangeSlider.height" to 48.dp,
    "AeroListItem.rowMinHeight" to 36.dp,
)

/**
 * Pure comparison over two `Map<String, Dp>`s (D-08) — the fixture proof above and the real
 * assertion in [VER03BaselineSizeSnapshotTest.realMeasuredSizesAndRadiiMatchV204Baseline] therefore
 * exercise exactly the same code path.
 *
 * Keyed strictly by identity, never by position or by value: iterates [expected]'s own keys and
 * looks each one up directly in [measured], so two entries whose expected values happen to coincide
 * (see the class KDoc) are still compared completely independently. Every comparison is EXACT `Dp`
 * equality — no fuzzy margin, so sub-dp rounding drift is never silently absorbed. An empty
 * [measured] map always yields a non-empty result, never a vacuous pass.
 */
internal fun baselineDeviations(measured: Map<String, Dp>, expected: Map<String, Dp>): List<String> {
    val violations = mutableListOf<String>()
    if (measured.isEmpty()) {
        violations.add(
            "Measured map is empty — an empty measurement set must never pass vacuously (VER-03)"
        )
    }
    for ((key, expectedValue) in expected) {
        val actualValue = measured[key]
        when {
            actualValue == null ->
                violations.add("Missing measurement for '$key' (expected $expectedValue) (VER-03)")
            actualValue != expectedValue ->
                violations.add("'$key' measured $actualValue, expected $expectedValue (VER-03)")
        }
    }
    return violations
}

/** [BASELINE] with exactly one key's value shifted by [shiftDp] dp — everything else stays
 * byte-identical to [BASELINE] (VER-06/adjacency), used by the boundary fixture tests above. */
private fun baselineShiftedBy(key: String, shiftDp: Float): Map<String, Dp> {
    val shifted = BASELINE.toMutableMap()
    shifted[key] = (BASELINE.getValue(key).value + shiftDp).dp
    return shifted
}

/**
 * Extracts the `Dp` literal immediately following [marker] in [source] (comment-stripped first via
 * [stripComments]) — e.g. marker `"TRACK_CORNER_RADIUS ="` against
 * `private val TRACK_CORNER_RADIUS = 9.dp` yields `9.dp`. Used for every [BASELINE] key that cannot
 * be read off a composed node (VER-03 step 3): a private corner-radius/size constant is invisible
 * to layout measurement, and a corner radius never participates in layout at all.
 */
private fun dpLiteralAfter(source: String, marker: String): Dp {
    val stripped = stripComments(source)
    val regex = Regex(Regex.escape(marker) + """\s*([0-9]+(?:\.[0-9]+)?)\.dp""")
    val match = regex.find(stripped)
        ?: error("VER-03: could not find '$marker <N>.dp' in comment-stripped source")
    return match.groupValues[1].toFloat().dp
}

/**
 * [source] with every line whose trimmed form starts with a line-comment marker, a block-comment
 * opener, or a KDoc/block-comment continuation asterisk removed — same filter as
 * [com.mordred.aero.verification.VER01GradientProportionalitySourceTest]'s and
 * [com.mordred.aero.verification.VER02AeroSurfaceClipOrderSourceTest]'s own copies, so a dp literal
 * discussed only in KDoc/line-comment prose can never satisfy or defeat this gate. Per-file copy
 * per those files' own convention.
 */
private fun stripComments(source: String): String =
    source
        .lineSequence()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

/**
 * Locates a single library main-source file by [relativePath] — cwd-independent resolution,
 * mirroring [com.mordred.aero.verification.VER01GradientProportionalitySourceTest]'s
 * `mainSourceFiles()` idiom but for one specific file rather than a whole-tree walk, since VER-03's
 * value-level assertions each target one known file.
 */
internal fun sourceFile(relativePath: String): File {
    val candidates = listOf(
        File("src/main/kotlin/$relativePath"),
        File("library/src/main/kotlin/$relativePath"),
    )
    return candidates.firstOrNull { it.exists() }
        ?: error(
            "Could not locate $relativePath from cwd ${File(".").absolutePath} " +
                "(tried: ${candidates.joinToString { it.path }})"
        )
}
