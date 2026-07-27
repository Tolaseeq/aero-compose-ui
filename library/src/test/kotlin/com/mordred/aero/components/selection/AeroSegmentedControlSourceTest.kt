package com.mordred.aero.components.selection

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards mirroring [com.mordred.aero.components.buttons.AeroButtonSurfaceSourceTest]'s
 * "no duplicated painter" reuse-guard shape, applied here to the whole `AeroSegmentedControl.kt`
 * file (single-declaration file, no larger body to narrow away from).
 *
 * Guards (a)-(i) below defend the requirements/decisions named in each assertion message.
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), every guard here was proven to FAIL against the shipped, un-restyled source before
 * being trusted — see 19-03-SUMMARY.md "Guard Fail-Then-Pass Proof" for the exact before/after
 * transcript.
 */
class AeroSegmentedControlSourceTest {

    @Test
    fun sourceImportsAndCallsPressedRecessCrossPackage() {
        // (a) VSEL-03: cross-package reuse — the recessed segment must import and call the
        // button package's pressedRecess transform, not re-derive an inverted gradient locally.
        assertTrue(
            aeroSegmentedControlSource.contains("import com.mordred.aero.theme.pressedRecess"),
            "AeroSegmentedControl.kt must import com.mordred.aero.theme.pressedRecess — the " +
                "recessed selected segment reuses the pressed-button transform cross-package (VSEL-03)"
        )
        assertTrue(
            aeroSegmentedControlSource.contains("pressedRecess("),
            "AeroSegmentedControl.kt must call pressedRecess( for its recessed selected segment (VSEL-03)"
        )
    }

    @Test
    fun sourceImportsPressedInnerShadowAndDoesNotRedeclareAShadowLiteral() {
        // (b) VSEL-03: PRESSED_INNER_SHADOW must be imported, never redeclared as a local Shadow(...).
        assertTrue(
            aeroSegmentedControlSource.contains("PRESSED_INNER_SHADOW"),
            "AeroSegmentedControl.kt must reference PRESSED_INNER_SHADOW — the shared inner-shadow " +
                "value imported from AeroButtonSurface.kt (VSEL-03)"
        )
        assertFalse(
            aeroSegmentedControlSource.contains("Shadow("),
            "AeroSegmentedControl.kt must not construct a local Shadow( literal — PRESSED_INNER_SHADOW " +
                "must be imported, never redeclared with copied values (VSEL-03, the drift this guard forbids)"
        )
    }

    @Test
    fun sourceDelegatesToResolveSegmentStyleAndAeroSurface() {
        // (c) the restyled component resolves through a pure resolver and paints via the shared primitive.
        assertTrue(
            aeroSegmentedControlSource.contains("resolveSegmentStyle("),
            "AeroSegmentedControl.kt must call resolveSegmentStyle( — the pure per-segment style resolver"
        )
        assertTrue(
            aeroSegmentedControlSource.contains("aeroSurface("),
            "AeroSegmentedControl.kt must paint each segment via Modifier.aeroSurface( — the shared painter"
        )
    }

    @Test
    fun sourceUsesSelectableRoleRadioButtonNotBareClickable() {
        // (d) VSEL-04/D-09: real single-select semantics replace the bare clickable.
        assertTrue(
            aeroSegmentedControlSource.contains("selectable("),
            "AeroSegmentedControl.kt must call selectable( per segment — real Role.RadioButton " +
                "semantics replacing the bare clickable (VSEL-04/D-09)"
        )
        assertTrue(
            aeroSegmentedControlSource.contains("Role.RadioButton"),
            "AeroSegmentedControl.kt's selectable( call must pass role = Role.RadioButton (VSEL-04/D-09)"
        )
        assertFalse(
            aeroSegmentedControlSource.contains("clickable("),
            "AeroSegmentedControl.kt must not contain a bare clickable( call — selectable(role = " +
                "Role.RadioButton) replaces it entirely (VSEL-04/D-09)"
        )
    }

    @Test
    fun sourceDoesNotUseAeroGlowRingOnSegments() {
        // (e) D-10: segments use in-bounds hover/focus cues only, never the outer glow-ring bloom.
        assertFalse(
            aeroSegmentedControlSource.contains("aeroGlowRing("),
            "AeroSegmentedControl.kt must not call aeroGlowRing( — segments sit flush inside the " +
                "outer Row's clip, so an outer bloom would be sliced and bleed onto neighbours (D-10)"
        )
    }

    @Test
    fun sourceCollectsHoverViaHoverableNotPointerTracking() {
        // (f) VLST-04: hover must be collected via Modifier.hoverable, never raw pointer-position tracking.
        assertTrue(
            aeroSegmentedControlSource.contains(".hoverable("),
            "AeroSegmentedControl.kt must chain .hoverable( per segment to collect hover state (VLST-04)"
        )
        assertFalse(
            aeroSegmentedControlSource.contains("pointerInput") ||
                aeroSegmentedControlSource.contains("awaitPointerEventScope"),
            "AeroSegmentedControl.kt must not collect hover via pointerInput/awaitPointerEventScope " +
                "raw pointer tracking — selectable + hoverable is the mandated pattern (VLST-04)"
        )
    }

    @Test
    fun sourcePassesIndicationNull() {
        // (g) P-01: matches AeroButtonSurface's shipped precedent — custom cues replace platform indication.
        assertTrue(
            aeroSegmentedControlSource.contains("indication = null"),
            "AeroSegmentedControl.kt's selectable( call must pass indication = null (P-01), matching " +
                "AeroButtonSurface's shipped precedent — the custom per-segment hover/press cues replace it"
        )
    }

    @Test
    fun sourceDoesNotUseFullyTransparentColorConstant() {
        // (h) PRIM-14: never Color.Transparent as a gradient stop/fade target — fade to baseColor.copy(alpha = 0f).
        assertFalse(
            aeroSegmentedControlSource.contains("Color.Transparent"),
            "AeroSegmentedControl.kt must not use Color.Transparent as a gradient stop/fade target — " +
                "fade to baseColor.copy(alpha = 0f) instead (PRIM-14); this is a flat wrong-colored " +
                "block on Classic's opaque tokens"
        )
    }

    @Test
    fun sourceDoesNotDrawTheDroppedSeparatorBox() {
        // (i) D-08/D-10: the 1.dp inter-segment separator is dropped — each segment's own contour supplies the break.
        assertFalse(
            aeroSegmentedControlSource.contains(".width(1.dp)") &&
                aeroSegmentedControlSource.contains("borderDefault.copy(alpha = 0.5f)"),
            "AeroSegmentedControl.kt must not draw the 1.dp borderDefault@0.5f separator Box between " +
                "segments — each segment's own raised or recessed contour supplies the visual break " +
                "now that every segment has its own bevel/rim (D-08/D-10)"
        )
    }

    private val aeroSegmentedControlSource: String get() = sourceFile("AeroSegmentedControl.kt").readText()

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/selection/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/selection/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
