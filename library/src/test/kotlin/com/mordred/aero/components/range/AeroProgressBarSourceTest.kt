package com.mordred.aero.components.range

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Source-scan regression guards mirroring
 * [com.mordred.aero.components.buttons.AeroButtonSurfaceSourceTest]'s `sourceFile()` convention:
 *
 * - VRNG-08: [AeroProgressBar]'s indeterminate animation spec is still `tween(1500, LinearEasing)`
 *   + `RepeatMode.Restart`, and NEVER `RepeatMode.Reverse` — no ping-pong.
 * - VRNG-07/D-05: the new `showRunningSheen` parameter defaults to `false` — the static gloss
 *   stays on, the traveling highlight is opt-in.
 * - PRIM-14: no `Color.Transparent` gradient stop anywhere in the file's real code — every fade
 *   targets `baseColor.copy(alpha = 0f)` instead.
 *
 * `sourceCode` strips comment lines/blocks (both `//` line comments and `/* ... */`/`/** ... */`
 * block comments) BEFORE the content assertions run — this file's own KDoc prose intentionally
 * documents the `RepeatMode.Reverse`/`Color.Transparent` anti-patterns by name (so a future reader
 * knows what NOT to write), which would otherwise make the `assertFalse` guards below trivially
 * self-fail against the real, correct source. Stripping comments first ensures these guards only
 * ever inspect the actual executable code.
 *
 * Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path,
 * VER-06), all guards below were proven to FAIL against deliberately-reintroduced broken source
 * (temporary local edits, reverted before commit) before being trusted — see this plan's SUMMARY
 * "Guard Fail-Then-Pass Proof", mirroring `17-03-SUMMARY.md`'s precedent.
 */
class AeroProgressBarSourceTest {

    @Test
    fun indeterminateKeepsThe1500msRestartTimingWithNoPingPong() {
        assertTrue(
            codeOnly.contains("RepeatMode.Restart"),
            "AeroProgressBar.kt must keep RepeatMode.Restart for the indeterminate sweep (VRNG-08)"
        )
        assertTrue(
            codeOnly.contains("1500"),
            "AeroProgressBar.kt must keep the indeterminate sweep's existing 1500ms restart timing (VRNG-08)"
        )
        assertFalse(
            codeOnly.contains("RepeatMode.Reverse"),
            "AeroProgressBar.kt must NEVER use RepeatMode.Reverse — ping-pong is explicitly banned (VRNG-08)"
        )
    }

    @Test
    fun showRunningSheenParamDefaultsFalse() {
        assertTrue(
            codeOnly.contains("showRunningSheen: Boolean = false"),
            "AeroProgressBar.kt's determinate overload must declare showRunningSheen: Boolean = false " +
                "— static gloss always on, the traveling highlight is opt-in (VRNG-07/D-05)"
        )
    }

    @Test
    fun noColorTransparentAntiPatternInRealCode() {
        assertFalse(
            codeOnly.contains("Color.Transparent"),
            "AeroProgressBar.kt must never use the Color.Transparent constant in a gradient fade — " +
                "fade to baseColor.copy(alpha = 0f) instead, which also degrades correctly on " +
                "AeroColorScheme.Classic's fully-opaque tokens (PRIM-14)"
        )
    }

    @Test
    fun fillNoLongerUsesFlatBackgroundColorPrimary() {
        assertFalse(
            codeOnly.contains(".background(colors.primary"),
            "AeroProgressBar.kt's fill must no longer use a flat .background(colors.primary, ...) — " +
                "it must route through the accent aeroSurface(...) fill instead (VRNG-06)"
        )
        assertTrue(
            codeOnly.contains("aeroGroove(") && codeOnly.contains("aeroSurface("),
            "AeroProgressBar.kt must paint its recessed bed via aeroGroove( and its accent fill via " +
                "aeroSurface( (VRNG-06)"
        )
    }

    private val progressBarSource: String get() = sourceFile("AeroProgressBar.kt").readText()

    /** [progressBarSource] with all `//` line comments and `/* ... */`/`/** ... */` block comments
     * stripped, so this file's own KDoc prose about banned patterns cannot self-invalidate the
     * guards above. */
    private val codeOnly: String get() = stripComments(progressBarSource)

    private fun stripComments(source: String): String {
        val noBlockComments = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return noBlockComments.lineSequence().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.substring(0, idx) else line
        }
    }

    /** cwd-independent resolution — Gradle's test task cwd varies between `library/` and repo root. */
    private fun sourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/kotlin/com/mordred/aero/components/range/$name"),
            File("library/src/main/kotlin/com/mordred/aero/components/range/$name"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Could not locate $name from cwd ${File(".").absolutePath} (tried: $candidates)")
    }
}
