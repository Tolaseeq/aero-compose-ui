package com.mordred.aero.verification

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * VER-01 source-scan gate: every gradient's explicit END-stop coordinate must derive from
 * `size.` (proportional), never a bare pixel literal or a named pixel constant carrying no
 * `size.` — the PRIM-09 defect where `glassSurface`'s gloss gradient hardcoded `endY = 100f`
 * and therefore never completed on short controls, banding instead of fading cleanly.
 *
 * **D-05's rule and its two carve-outs** (both honoured by [gradientEndStopViolations] and
 * asserted below): (a) a gradient declaring NO explicit stop arguments is legal — it already
 * spans the full bounds; (b) START coordinates (`startY`, `startX`, `start =`) are outside the
 * rule as D-05 words it, so the shipped `startY = 0f` arguments across the codebase stay legal.
 * Colour-stop fraction lists (`colors = listOf(...)`, `colorStops = arrayOf(0f to …, 1f to …)`)
 * are never coordinates and are never inspected — [com.mordred.aero.components.range.AeroProgressBar]'s
 * `colorStops = arrayOf(0f to …, 1f to …)` calls are the concrete file that would misfire under a
 * naive numeric-literal search.
 *
 * **D-06 scope.** [mainSourceFiles] walks every `.kt` under the library main source root, not
 * just the eight restyled v3.0 components — only 18 gradient call sites across 9 files exist
 * (20-RESEARCH.md), so library-wide scope costs nothing and binds everything written after this
 * plan.
 *
 * **D-08 fixture proof, in this file.** Per this project's own v2.0.3 false-positive-sign-off
 * lesson (repro-must-exercise-the-path, VER-06 as its own new requirement this milestone), a gate
 * that cannot fail is a false pass. Every `@Test` below asserting a NON-empty violation list is
 * the red half of that proof (a deliberately-broken, in-file `const val` fixture — never written
 * to the source tree); every `@Test` asserting an empty list is the green half. Both halves live
 * in this class and re-execute on every build, superseding the Phase 17 precedent of a one-time
 * transcript recorded only in a SUMMARY.
 */
class VER01GradientProportionalitySourceTest {

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — RED half: fixtures that must be flagged.
    // ---------------------------------------------------------------------------------------

    @Test
    fun barePixelLiteralEndStopIsFlagged() {
        val violations = gradientEndStopViolations(VIOLATING_BARE_PIXEL_LITERAL)
        assertTrue(
            violations.isNotEmpty(),
            "A gradient whose endY is a bare pixel literal (e.g. `endY = 100f`) must be flagged " +
                "— this is the historical PRIM-09 defect the gate exists to catch (VER-01)"
        )
    }

    @Test
    fun namedPixelConstantEndStopIsFlagged() {
        val violations = gradientEndStopViolations(VIOLATING_NAMED_PIXEL_CONSTANT)
        assertTrue(
            violations.isNotEmpty(),
            "A gradient whose endY is a named constant carrying no `size.` must be flagged — per " +
                "D-05 this is the identical bug wearing a named constant, which a bare " +
                "numeric-literal search would miss (VER-01)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — GREEN half: fixtures that must pass cleanly.
    // ---------------------------------------------------------------------------------------

    @Test
    fun sizeRelativeEndStopIsClean() {
        val violations = gradientEndStopViolations(CLEAN_SIZE_RELATIVE_END_STOP)
        assertTrue(
            violations.isEmpty(),
            "A gradient whose endY derives from `size.` must pass cleanly (VER-01). Differs from " +
                "VIOLATING_BARE_PIXEL_LITERAL by exactly the endY expression (VER-06/adjacency)."
        )
    }

    @Test
    fun namedConstantSizeRelativeEndStopIsClean() {
        val violations = gradientEndStopViolations(CLEAN_NAMED_CONSTANT_SIZE_RELATIVE)
        assertTrue(
            violations.isEmpty(),
            "A gradient whose endY derives from `size.` must pass cleanly even when an unused " +
                "named pixel constant sits alongside it (VER-01). Differs from " +
                "VIOLATING_NAMED_PIXEL_CONSTANT by exactly the endY expression (VER-06/adjacency)."
        )
    }

    @Test
    fun gradientWithNoExplicitStopsIsClean() {
        val violations = gradientEndStopViolations(CLEAN_NO_EXPLICIT_STOPS)
        assertTrue(
            violations.isEmpty(),
            "A gradient declaring no explicit stop arguments at all must pass cleanly — it " +
                "already spans the full bounds (VER-01/D-05 carve-out a)"
        )
    }

    @Test
    fun sizeRelativeEndStopBesideUnrelatedNumericLiteralIsClean() {
        val violations = gradientEndStopViolations(CLEAN_SIZE_RELATIVE_WITH_ADJACENT_LITERAL)
        assertTrue(
            violations.isEmpty(),
            "A `size.`-relative endY beside an unrelated bare numeric literal in an adjacent " +
                "argument on the SAME source line must not be flagged — only the named end-stop " +
                "argument itself is inspected (VER-01/adjacency)"
        )
    }

    @Test
    fun sourceWithZeroGradientCallSitesIsClean() {
        val violations = gradientEndStopViolations(CLEAN_NO_GRADIENT_CALLS)
        assertTrue(
            violations.isEmpty(),
            "A source file containing zero gradient call sites must pass cleanly — an empty scan " +
                "set is a pass, never an error and never a vacuous failure (VER-01/empty)"
        )
    }

    @Test
    fun pixelLiteralOnlyInsideACommentIsClean() {
        val violations = gradientEndStopViolations(CLEAN_PIXEL_LITERAL_ONLY_IN_COMMENT)
        assertTrue(
            violations.isEmpty(),
            "A pixel literal that appears only inside a KDoc/line comment must not trip the gate " +
                "— the gate scans comment-stripped source via stripComments (VER-01/encoding)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real-source scan.
    // ---------------------------------------------------------------------------------------

    @Test
    fun realLibrarySourceHasNoGradientEndStopViolations() {
        val files = mainSourceFiles()
        assertTrue(
            files.size > 0,
            "Scanned file count must be greater than zero — a broken cwd/path resolution must " +
                "not be able to produce a vacuous pass (VER-01). Resolved from cwd " +
                File(".").absolutePath
        )
        val violationsByFile = files.mapNotNull { file ->
            val violations = gradientEndStopViolations(file.readText())
            if (violations.isNotEmpty()) file.path to violations else null
        }
        assertTrue(
            violationsByFile.isEmpty(),
            "Found gradient end-stop violations across the library main source root (VER-01):\n" +
                violationsByFile.joinToString("\n") { (path, fileViolations) ->
                    "$path:\n  " + fileViolations.joinToString("\n  ")
                }
        )
    }
}

/**
 * Pure detector over SOURCE TEXT (never a [File]) — the fixture proof above and the real-source
 * scan therefore exercise exactly the same code path (D-08).
 *
 * Strips comments first via [stripComments] so a pixel literal discussed only in KDoc/line-comment
 * prose neither raises a false positive nor masks a real violation. Then, for every
 * `Brush.verticalGradient(`, `Brush.horizontalGradient(` and `Brush.linearGradient(` occurrence,
 * takes that call's balanced-parenthesis extent and inspects ONLY its explicitly-named END-stop
 * coordinate arguments — `endY =`, `endX =`, and `linearGradient`'s `end =` — each argument's
 * expression text taken up to the next comma at the extent's top nesting level. START coordinates
 * (`startY`, `startX`, `start =`) and colour-stop lists (`colors = listOf(...)`,
 * `colorStops = arrayOf(...)`) are never inspected (D-05).
 *
 * Returns one descriptive violation string per offending end-stop argument; an empty list means
 * every explicit end stop in [source] derives from `size.` (or there were none, D-05 carve-out a).
 */
internal fun gradientEndStopViolations(source: String): List<String> {
    val stripped = stripComments(source)
    val violations = mutableListOf<String>()
    for (match in GRADIENT_CALL_REGEX.findAll(stripped)) {
        val fnName = match.groupValues[1]
        val openParenIndex = match.range.last
        val extent = balancedParenExtent(stripped, openParenIndex)
        for (arg in splitTopLevelArgs(extent)) {
            val trimmedArg = arg.trim()
            if (trimmedArg.isEmpty()) continue
            val eqIndex = trimmedArg.indexOf('=')
            if (eqIndex == -1) continue
            val argName = trimmedArg.substring(0, eqIndex).trim()
            if (argName != "endY" && argName != "endX" && argName != "end") continue
            val valueText = trimmedArg.substring(eqIndex + 1).trim()
            if (!valueText.contains("size.")) {
                violations.add(
                    "Brush.$fnName end-stop argument '$argName = $valueText' does not derive " +
                        "from size. (VER-01)"
                )
            }
        }
    }
    return violations
}

/** Matches the opening of every gradient factory call this gate inspects. */
private val GRADIENT_CALL_REGEX =
    Regex("""Brush\.(verticalGradient|horizontalGradient|linearGradient)\(""")

/**
 * Returns the text strictly between the '(' at [openParenIndex] in [text] and its matching ')',
 * tracking paren depth only — nested braces/brackets inside a balanced argument (e.g.
 * `colorStops = arrayOf(0f to a, 1f to b)`) never confuse this since every '(' is matched
 * regardless of what brace/bracket context it sits inside.
 */
private fun balancedParenExtent(text: String, openParenIndex: Int): String {
    var depth = 0
    var i = openParenIndex
    val start = openParenIndex + 1
    while (i < text.length) {
        when (text[i]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return text.substring(start, i)
            }
        }
        i++
    }
    return text.substring(start)
}

/**
 * Splits [text] on commas at nesting depth zero only — a comma inside a nested `(`/`[`/`{`
 * region (e.g. inside `arrayOf(0f to a, 1f to b)`) stays part of its enclosing top-level argument
 * rather than being treated as a new one.
 */
private fun splitTopLevelArgs(text: String): List<String> {
    val args = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in text.indices) {
        when (text[i]) {
            '(', '[', '{' -> depth++
            ')', ']', '}' -> depth--
            ',' -> if (depth == 0) {
                args.add(text.substring(start, i))
                start = i + 1
            }
        }
    }
    args.add(text.substring(start))
    return args
}

/**
 * [source] with every line whose trimmed form starts with a line-comment marker, a block-comment
 * opener, or a KDoc/block-comment continuation asterisk removed — the same filter
 * [com.mordred.aero.components.selection.AeroSwitchSourceTest]'s `nonCommentSource` applies, so a
 * pixel literal discussed inside KDoc or a line comment can never satisfy or defeat a guard over
 * the real code. Per-file copy (VER-02's gate carries its own identical copy) rather than a shared
 * top-level helper, so each gate file reads standalone.
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
 * Every `.kt` file under the library main source root (D-06: library-wide, not just the eight
 * restyled components). cwd-independent resolution — Gradle's test task cwd varies between
 * `library/` and the repo root, mirroring the six shipped `*SourceTest.kt` guards' `sourceFile()`
 * idiom. Per-file copy (VER-02's gate carries its own identical copy).
 */
private fun mainSourceFiles(): List<File> {
    val root = listOf(
        File("src/main/kotlin"),
        File("library/src/main/kotlin"),
    ).firstOrNull { it.exists() }
        ?: error(
            "Could not locate the library main source root from cwd ${File(".").absolutePath} " +
                "(tried: src/main/kotlin, library/src/main/kotlin)"
        )
    return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
}

// =============================================================================================
// VER-06 fixtures (D-08) — in-file string constants only, never written to the source tree.
// =============================================================================================

/** RED — bare pixel-literal endY (the historical PRIM-09 defect). */
private const val VIOLATING_BARE_PIXEL_LITERAL = """
private fun DrawScope.paintBadGradient() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f,
            endY = 100f,
        ),
    )
}
"""

/** GREEN — [VIOLATING_BARE_PIXEL_LITERAL]'s counterpart, differing only in the endY expression. */
private const val CLEAN_SIZE_RELATIVE_END_STOP = """
private fun DrawScope.paintGoodGradient() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f,
            endY = size.height,
        ),
    )
}
"""

/** RED — endY set to a named all-caps constant carrying no `size.` (D-05: same bug, named form). */
private const val VIOLATING_NAMED_PIXEL_CONSTANT = """
private const val FIXED_GLOSS_HEIGHT = 100f

private fun DrawScope.paintBadGradientNamed() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f,
            endY = FIXED_GLOSS_HEIGHT,
        ),
    )
}
"""

/**
 * GREEN — [VIOLATING_NAMED_PIXEL_CONSTANT]'s counterpart, differing only in the endY expression
 * (the unused constant declaration is kept verbatim so the two fixtures differ by exactly one
 * property, VER-06/adjacency).
 */
private const val CLEAN_NAMED_CONSTANT_SIZE_RELATIVE = """
private const val FIXED_GLOSS_HEIGHT = 100f

private fun DrawScope.paintBadGradientNamed() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f,
            endY = size.height,
        ),
    )
}
"""

/** GREEN — a gradient declaring no explicit stop arguments at all (D-05 carve-out a). */
private const val CLEAN_NO_EXPLICIT_STOPS = """
private fun DrawScope.paintFullBoundsGradient() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
        ),
    )
}
"""

/**
 * GREEN — a `size.`-relative endY beside an unrelated bare numeric literal in an adjacent
 * argument on the same source line (VER-01/adjacency: the gate must not flag the unrelated
 * literal just because it shares a line with the inspected end-stop argument).
 */
private const val CLEAN_SIZE_RELATIVE_WITH_ADJACENT_LITERAL = """
private fun DrawScope.paintGoodGradientWithNeighbor() {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f, endY = size.height, unrelatedMagicNumber = 100f,
        ),
    )
}
"""

/** GREEN — zero gradient call sites; an empty scan set is a pass (VER-01/empty). */
private const val CLEAN_NO_GRADIENT_CALLS = """
private fun ordinaryFunction(a: Int, b: Int): Int {
    return a + b
}
"""

/**
 * GREEN — the only pixel-literal end stop sits inside a line comment above a compliant call
 * (VER-01/encoding: the gate scans comment-stripped source, so this must not trip it).
 */
private const val CLEAN_PIXEL_LITERAL_ONLY_IN_COMMENT = """
private fun DrawScope.paintGoodGradientDocumented() {
    // Historically this used endY = 100f; now proportional per PRIM-09.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White, Color.Black),
            startY = 0f,
            endY = size.height,
        ),
    )
}
"""
