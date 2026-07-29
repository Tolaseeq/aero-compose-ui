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
    fun sourceHasExactlyOneHoverEmitterAndNoRawPointerTracking() {
        // (f) WR-04: the per-segment duplicate hoverable emitter was removed because the selectable
        // modifier already emits hover on the segment source (proven by HoverEmissionTest); the
        // outer strip's hoverable call must stay because the control-level source has no other
        // emitter. VLST-04: hover must still be collected via Modifier.hoverable, never raw
        // pointer-position tracking.
        val hoverableCount = nonCommentSource
            .split(".hoverable(")
            .size - 1
        assertTrue(
            hoverableCount == 1,
            "AeroSegmentedControl.kt must contain exactly ONE .hoverable( call — the outer strip's. " +
                "The per-segment duplicate was removed because the selectable modifier already " +
                "emits hover on the segment source (WR-04); the outer call must stay because the " +
                "control-level source has no other emitter. Found $hoverableCount occurrences."
        )
        assertTrue(
            nonCommentSource.contains("interactionSource = segSource"),
            "AeroSegmentedControl.kt must pass interactionSource = segSource to the per-segment " +
                "selectable( call — pinning the per-segment source-sharing the focus-visibility " +
                "reducer depends on"
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

    @Test
    fun sourceLabelUsesTheSharedResolveLabelColorMechanismNeverAPerComponentLiteral() {
        // (j) D-12/D-13 (20-04), superseding the G5-era "inherits ambient LocalContentColor"
        // guard this replaces: BOTH labels (button and segment) must come from ONE shared
        // resolver function, never a per-component literal or ambient-inheritance mechanism. The
        // G5 fix (no explicit color = param, inheriting ambient LocalContentColor) was correct in
        // spirit — one shared mechanism, not a per-component constant — but ambient resolution
        // never actually cleared the WCAG 4.5:1 floor (the todo filed alongside G5's own
        // closure), so this guard is updated again to require the label ASK for its colour
        // algorithmically via the shared resolveLabelColor(...) call, imported cross-package from
        // AeroButtonSurface.kt (D-12). Comment lines are stripped first (nonCommentSource) so
        // these guards cannot be satisfied or broken by KDoc prose describing rejected behaviour.
        assertTrue(
            nonCommentSource.contains("import com.mordred.aero.components.buttons.resolveLabelColor"),
            "AeroSegmentedControl.kt must import com.mordred.aero.components.buttons.resolveLabelColor " +
                "— the segment label must use the SAME shared resolver AeroButtonSurface's Text( does, " +
                "never a re-derived per-component mechanism (D-12)"
        )
        assertTrue(
            nonCommentSource.contains("color = resolveLabelColor("),
            "AeroSegmentedControl.kt's segment Text( call must pass color = resolveLabelColor(...) " +
                "— the label colour must be ASKED FOR algorithmically from the shared resolver, never " +
                "left to ambient inheritance or a per-component literal (D-12/D-13)"
        )
        assertFalse(
            nonCommentSource.contains("color = colors.onSurface"),
            "AeroSegmentedControl.kt's segment Text( call must NOT pass an explicit " +
                "color = colors.onSurface parameter — the label colour comes from the shared " +
                "resolveLabelColor(...) mechanism, not a re-specified literal token"
        )
        assertFalse(
            nonCommentSource.contains("colors.surface"),
            "AeroSegmentedControl.kt must not reference colors.surface as a label colour — that " +
                "background/panel token carries an 0xCC alpha on AeroBlue/AeroDark, so using it for " +
                "text also renders the label ~80% opaque (gap G3, VSEL-03, fault (a) background " +
                "token used for text and fault (b) that token's alpha bleeding onto text)"
        )
        assertFalse(
            nonCommentSource.contains("animateColorAsState"),
            "AeroSegmentedControl.kt must not animate a per-segment label colour via " +
                "animateColorAsState — the resolved fill already drives resolveLabelColor(...) on " +
                "every recomposition, so a separate colour animation would recreate gap G3's defect " +
                "class on a different theme (gap G3/G5, VSEL-03)"
        )
    }

    @Test
    fun sourceGatesTheFocusStrokeOnFocusVisible() {
        // (k) gap G2: the per-segment in-bounds focus stroke must draw only for keyboard-acquired
        // focus (segState.focusVisible), not the raw focused flag — a pointer-acquired focus must
        // not draw the stroke, while every segment stays its own Tab stop and Space/Enter still
        // activates it (VSEL-04). See AeroSegmentedControlSemanticsTest for the unaffected
        // Role.RadioButton/Tab-stop/Space/Enter assertions this gate must not weaken.
        assertTrue(
            aeroSegmentedControlSource.contains("segState.focusVisible && enabled"),
            "AeroSegmentedControl.kt must gate its per-segment in-bounds focus stroke on " +
                "segState.focusVisible && enabled so a pointer-acquired focus does not draw the " +
                "stroke, while every segment stays its own Tab stop and Space/Enter still selects " +
                "it (gap G2, VSEL-04)"
        )
    }

    @Test
    fun sourceKeysEachSegmentOnItsOwnIdentity() {
        // (l) WR-03: per-segment remembered state and the in-flight selection tween must be keyed
        // to that segment's own identity, not to its slot position, so inserting, removing or
        // reordering options cannot transfer one option's interaction and animation state to another.
        assertTrue(
            nonCommentSource.contains("key(index, opt)"),
            "AeroSegmentedControl.kt must wrap each segment's body in key(index, opt) — without it, " +
                "per-segment remembered state and the in-flight selection tween are memoised by slot " +
                "position, so inserting, removing or reordering options transfers one option's " +
                "interaction and animation state to another (WR-03)"
        )
    }

    private val aeroSegmentedControlSource: String get() = sourceFile("AeroSegmentedControl.kt").readText()

    /**
     * [aeroSegmentedControlSource] with every line whose trimmed form starts with a line-comment
     * marker, a block-comment opener, or a KDoc/block-comment continuation asterisk removed — so
     * negative guards over the real code cannot be satisfied or broken by KDoc prose that merely
     * discusses a forbidden identifier (e.g. explaining why `colors.surface` was rejected).
     */
    private val nonCommentSource: String
        get() = aeroSegmentedControlSource
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
            }
            .joinToString("\n")

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
