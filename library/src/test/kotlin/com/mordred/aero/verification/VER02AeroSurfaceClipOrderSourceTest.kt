package com.mordred.aero.verification

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * VER-02 source-scan gate: nobody bypasses [com.mordred.aero.theme.aeroSurface]'s centralized
 * clip order. Chain-aware — segments the comment-stripped source into individual modifier chains
 * ([modifierChains]) rather than scanning whole-file text, because
 * [com.mordred.aero.components.selection.AeroSegmentedControl] legitimately holds a `.clip(shape)`
 * on the outer `Row` and a separate `.aeroSurface(...)` on a per-segment `Box` in two unrelated
 * chains — a false positive an order-insensitive, chain-blind scan would produce.
 *
 * **D-07's two checkable-by-position bypasses** (both honoured by [glowRingBeforeSurfaceViolations]
 * / [clipAfterSurfaceViolations] and asserted below):
 * (a) where a chain contains BOTH, [com.mordred.aero.theme.aeroGlowRing] must precede
 * [com.mordred.aero.theme.aeroSurface] — `aeroSurface`'s own internal `.clip(shape)` erases any
 * bloom drawn after it, precisely how the glow silently vanished in 16-05 and had to be documented
 * as a USAGE CONTRACT;
 * (b) within any chain, a `.clip(` occurrence after `aeroSurface(` is a second clip layered over
 * the centralized one.
 * Hand-rolled `drawBehind` + clip glass is deliberately NOT gated (D-07) — `GlassModifiers.kt`,
 * the colour-picker internals and the overlay components draw that way legitimately, and gating it
 * would require an exception list that rots.
 *
 * **D-08 fixture proof, in this file.** Every `@Test` below asserting a NON-empty violation list is
 * the red half of the VER-06 proof (a deliberately-broken, in-file `const val` fixture — never
 * written to the source tree); every `@Test` asserting an empty list is the green half. Both halves
 * live in this class and re-execute on every build.
 *
 * **D-06 scope.** [mainSourceFiles] walks every `.kt` under the library main source root, matching
 * [com.mordred.aero.verification.VER01GradientProportionalitySourceTest]'s scope.
 */
class VER02AeroSurfaceClipOrderSourceTest {

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — RED half: fixtures that must be flagged.
    // ---------------------------------------------------------------------------------------

    @Test
    fun glowRingAfterSurfaceInSameChainIsFlagged() {
        val violations = glowRingBeforeSurfaceViolations(VIOLATING_GLOW_AFTER_SURFACE)
        assertTrue(
            violations.isNotEmpty(),
            "A chain applying aeroSurface( then aeroGlowRing( must be flagged — aeroSurface's " +
                "internal .clip(shape) erases any bloom drawn after it (VER-02)"
        )
    }

    @Test
    fun clipAfterSurfaceInSameChainIsFlagged() {
        val violations = clipAfterSurfaceViolations(VIOLATING_CLIP_AFTER_SURFACE)
        assertTrue(
            violations.isNotEmpty(),
            "A chain applying aeroSurface( then .clip( must be flagged — a second clip layered " +
                "over aeroSurface's own centralized clip (VER-02)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // VER-06 fixture proof (D-08) — GREEN half: fixtures that must pass cleanly.
    // ---------------------------------------------------------------------------------------

    @Test
    fun glowRingBeforeSurfaceInSameChainIsClean() {
        val glowViolations = glowRingBeforeSurfaceViolations(CLEAN_GLOW_BEFORE_SURFACE)
        val clipViolations = clipAfterSurfaceViolations(CLEAN_GLOW_BEFORE_SURFACE)
        assertTrue(
            glowViolations.isEmpty() && clipViolations.isEmpty(),
            "A chain applying aeroGlowRing( then aeroSurface( must pass cleanly on both detectors " +
                "(VER-02). Differs from VIOLATING_GLOW_AFTER_SURFACE by exactly call order " +
                "(VER-06/adjacency)."
        )
    }

    @Test
    fun clipBeforeSurfaceInSameChainIsClean() {
        val glowViolations = glowRingBeforeSurfaceViolations(CLEAN_CLIP_BEFORE_SURFACE)
        val clipViolations = clipAfterSurfaceViolations(CLEAN_CLIP_BEFORE_SURFACE)
        assertTrue(
            glowViolations.isEmpty() && clipViolations.isEmpty(),
            "A chain applying .clip( then aeroSurface( must pass cleanly on both detectors " +
                "(VER-02). Differs from VIOLATING_CLIP_AFTER_SURFACE by exactly call order " +
                "(VER-06/adjacency)."
        )
    }

    @Test
    fun twoUnrelatedChainsOneClipOnlyOneSurfaceOnlyAreClean() {
        val glowViolations = glowRingBeforeSurfaceViolations(CLEAN_TWO_UNRELATED_CHAINS)
        val clipViolations = clipAfterSurfaceViolations(CLEAN_TWO_UNRELATED_CHAINS)
        assertTrue(
            glowViolations.isEmpty() && clipViolations.isEmpty(),
            "A file containing both a .clip(...) chain and a separate, unrelated .aeroSurface(...) " +
                "chain must pass cleanly — only a .clip( applied AFTER aeroSurface( WITHIN THE SAME " +
                "chain is a violation (VER-02/adjacency). This is AeroSegmentedControl.kt's real shape."
        )
    }

    @Test
    fun clipOnlyInsideACommentAboveAnAeroSurfaceChainIsClean() {
        val glowViolations = glowRingBeforeSurfaceViolations(CLEAN_CLIP_ONLY_IN_COMMENT)
        val clipViolations = clipAfterSurfaceViolations(CLEAN_CLIP_ONLY_IN_COMMENT)
        assertTrue(
            glowViolations.isEmpty() && clipViolations.isEmpty(),
            "A .clip( token appearing only inside a KDoc/line comment above a chain that calls " +
                "aeroSurface( must not trip the gate — the gate scans comment-stripped source via " +
                "stripComments (VER-02/encoding). AeroButtonSurface.kt's KDoc is the real file this " +
                "protects."
        )
    }

    @Test
    fun sourceWithZeroAeroSurfaceCallSitesIsClean() {
        val glowViolations = glowRingBeforeSurfaceViolations(CLEAN_NO_AERO_SURFACE_CALLS)
        val clipViolations = clipAfterSurfaceViolations(CLEAN_NO_AERO_SURFACE_CALLS)
        assertTrue(
            glowViolations.isEmpty() && clipViolations.isEmpty(),
            "A source file containing zero aeroSurface( call sites must pass cleanly — an empty " +
                "scan set is a pass, never an error and never a vacuous failure (VER-02/empty)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Real-source scan.
    // ---------------------------------------------------------------------------------------

    @Test
    fun realLibrarySourceHasNoClipOrderViolations() {
        val files = mainSourceFiles()
        assertTrue(
            files.size > 0,
            "Scanned file count must be greater than zero — a broken cwd/path resolution must " +
                "not be able to produce a vacuous pass (VER-02). Resolved from cwd " +
                File(".").absolutePath
        )
        val violationsByFile = files.mapNotNull { file ->
            val source = file.readText()
            val violations = glowRingBeforeSurfaceViolations(source) + clipAfterSurfaceViolations(source)
            if (violations.isNotEmpty()) file.path to violations else null
        }
        assertTrue(
            violationsByFile.isEmpty(),
            "Found aeroSurface clip-order violations across the library main source root (VER-02):\n" +
                violationsByFile.joinToString("\n") { (path, fileViolations) ->
                    "$path:\n  " + fileViolations.joinToString("\n  ")
                }
        )
    }
}

/** Substring marking an `aeroSurface(` call site. Never occurs inside an unrelated identifier such
 * as `aeroThumbSurface(`/`AeroSurfaceStyle(` (case-sensitive, and `aeroThumbSurface(` interposes
 * `Thumb` between `aero` and `Surface(`) — the only place it can spuriously appear is the primitive's
 * OWN declaration line `fun Modifier.aeroSurface(...)`, which [isChainStartCandidate] excludes from
 * ever becoming a scanned chain in the first place (declarations, not call sites, are excluded via
 * their `fun ` keyword). */
private const val AERO_SURFACE_TOKEN = "aeroSurface("

/** Substring marking an `aeroGlowRing(` call site. Never occurs inside `aeroGlowRingRepeated(`
 * (`AeroSlider.kt`'s local helper) since `Repeated` interposes between `aeroGlowRing` and `(`. */
private const val AERO_GLOW_RING_TOKEN = "aeroGlowRing("

/** Substring marking a `.clip(` call site — dot-qualified so it never matches a bare `clip` word
 * appearing as part of an unrelated identifier. */
private const val CLIP_TOKEN = ".clip("

/**
 * Pure detector over SOURCE TEXT (D-08) for D-07(a): within any [modifierChains] segment
 * containing BOTH [AERO_GLOW_RING_TOKEN] and [AERO_SURFACE_TOKEN], reports a violation when ANY
 * `aeroGlowRing(` occurrence sits after the FIRST `aeroSurface(` occurrence in that same chain —
 * `aeroSurface`'s own internal `.clip(shape)` would erase the bloom drawn after it. A chain holding
 * only one of the two tokens returns no violation for it (nothing to compare order against).
 */
internal fun glowRingBeforeSurfaceViolations(source: String): List<String> {
    val violations = mutableListOf<String>()
    modifierChains(source).forEachIndexed { index, chain ->
        val surfaceIndex = chain.indexOf(AERO_SURFACE_TOKEN)
        if (surfaceIndex == -1) return@forEachIndexed
        val glowIndices = allIndicesOf(chain, AERO_GLOW_RING_TOKEN)
        if (glowIndices.isEmpty()) return@forEachIndexed
        if (glowIndices.any { it > surfaceIndex }) {
            violations.add(
                "Modifier chain #$index applies aeroGlowRing( after aeroSurface( — aeroSurface's " +
                    "internal .clip(shape) erases the bloom drawn after it (VER-02): " +
                    chainSnippet(chain)
            )
        }
    }
    return violations
}

/**
 * Pure detector over SOURCE TEXT (D-08) for D-07(b): within any [modifierChains] segment, reports
 * a violation when a `.clip(` occurrence sits after the FIRST `aeroSurface(` occurrence in that
 * same chain — a second clip layered over `aeroSurface`'s own centralized one. A chain holding no
 * `aeroSurface(` call site returns no violation (VER-02/empty).
 */
internal fun clipAfterSurfaceViolations(source: String): List<String> {
    val violations = mutableListOf<String>()
    modifierChains(source).forEachIndexed { index, chain ->
        val surfaceIndex = chain.indexOf(AERO_SURFACE_TOKEN)
        if (surfaceIndex == -1) return@forEachIndexed
        val clipIndices = allIndicesOf(chain, CLIP_TOKEN)
        if (clipIndices.any { it > surfaceIndex }) {
            violations.add(
                "Modifier chain #$index applies .clip( after aeroSurface( — a second clip layered " +
                    "over aeroSurface's own centralized clip (VER-02): " + chainSnippet(chain)
            )
        }
    }
    return violations
}

/** First 200 characters of [chain], trimmed — enough for a failure message to pinpoint the chain
 * without dumping the whole file. */
private fun chainSnippet(chain: String): String = chain.trim().take(200)

/** Every index at which [token] occurs in [text], left to right, non-overlapping. */
private fun allIndicesOf(text: String, token: String): List<Int> {
    val indices = mutableListOf<Int>()
    var from = 0
    while (true) {
        val idx = text.indexOf(token, from)
        if (idx == -1) break
        indices.add(idx)
        from = idx + token.length
    }
    return indices
}

/** Matches a line introducing a `Modifier` value — either a bare `Modifier` token (e.g. the first
 * positional argument `Modifier\n.foo()`) or a `modifier = ` assignment (e.g. `modifier = modifier`
 * / `modifier = Modifier`). */
private val MODIFIER_TOKEN_REGEX = Regex("""\bModifier\b|\bmodifier\s*=""")

/**
 * True when [line] could start a NEW modifier chain — it mentions the `Modifier` token per
 * [MODIFIER_TOKEN_REGEX] AND is not itself a function/declaration line (`fun `). The `fun ` exclusion
 * is load-bearing: without it, `public fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape:
 * Shape): Modifier = this` in `AeroSurfacePrimitives.kt` — the primitive's OWN declaration, whose
 * body legitimately applies `.clip(shape)` — would itself be picked up as a chain, and its
 * declaration text `Modifier.aeroSurface(` contains the literal substring `aeroSurface(` at an
 * index BEFORE that body's own `.clip(shape)` line, producing a false [clipAfterSurfaceViolations]
 * report against the gate's own protected primitive. Every real chain start in this codebase (an
 * assignment like `modifier = modifier` or a bare `Modifier` positional argument) never carries
 * `fun ` on the same line, so this exclusion costs nothing against genuine consumer chains.
 */
private fun isChainStartCandidate(line: String): Boolean =
    MODIFIER_TOKEN_REGEX.containsMatchIn(line) && !line.contains("fun ")

/** Net bracket-depth change contributed by [line] — every `(`/`{`/`[` counts +1, every matching
 * closer counts -1. Used to track whether a following line is inside an unbalanced bracket run
 * opened by the current chain (an argument continuation) or back at the chain's own top level. */
private fun netBracketDelta(line: String): Int {
    var delta = 0
    for (c in line) {
        when (c) {
            '(', '{', '[' -> delta++
            ')', '}', ']' -> delta--
        }
    }
    return delta
}

/**
 * Segments the comment-stripped [source] into individual modifier-chain text blocks. A chain
 * begins at a line satisfying [isChainStartCandidate] and continues through every following line
 * whose trimmed form starts with `.` (a top-level chain continuation) OR that sits inside an
 * unbalanced bracket run the chain itself opened (an argument continuation, e.g. the multi-line
 * argument list of `.clickable(...)`). Lines at a DEEPER bracket depth than the chain's own top
 * level that themselves satisfy [isChainStartCandidate] belong to a NESTED chain instead — the
 * outer chain stops there and the nested chain is picked up on the next scan pass — mirroring
 * `AeroSegmentedControl.kt`'s `.then(...)` block, which nests a `Modifier.drawBehind { ... }` chain
 * inside the outer segment's own `modifier = Modifier ... .aeroSurface(...) .then(...)` chain. A
 * chain ends at the first line that is neither a top-level `.`-continuation nor part of an
 * unbalanced bracket run it opened.
 */
internal fun modifierChains(source: String): List<String> {
    val lines = stripComments(source).lines()
    val chains = mutableListOf<String>()
    var i = 0
    while (i < lines.size) {
        if (isChainStartCandidate(lines[i])) {
            val (chainText, nextIndex) = consumeChain(lines, i)
            chains.add(chainText)
            i = nextIndex
        } else {
            i++
        }
    }
    return chains
}

/** Consumes one modifier chain starting at [startIndex] in [lines], returning its joined text and
 * the index of the first line NOT part of this chain. See [modifierChains]'s KDoc for the rules. */
private fun consumeChain(lines: List<String>, startIndex: Int): Pair<String, Int> {
    val buffer = StringBuilder()
    var depth = 0
    var i = startIndex

    buffer.append(lines[i]).append('\n')
    depth += netBracketDelta(lines[i])
    i++

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        if (depth > 0) {
            // Inside an unbalanced bracket run the chain itself opened (an argument continuation)
            // — UNLESS this line starts a nested Modifier chain of its own, in which case the
            // outer chain stops here and the nested chain is segmented separately.
            if (isChainStartCandidate(line)) break
            buffer.append(line).append('\n')
            depth += netBracketDelta(line)
            i++
            continue
        }
        // depth == 0: only a top-level `.`-continuation extends the chain.
        if (trimmed.startsWith(".")) {
            buffer.append(line).append('\n')
            depth += netBracketDelta(line)
            i++
            continue
        }
        break
    }
    return buffer.toString() to i
}

/**
 * [source] with every line whose trimmed form starts with a line-comment marker, a block-comment
 * opener, or a KDoc/block-comment continuation asterisk removed — same filter as
 * [com.mordred.aero.components.selection.AeroSwitchSourceTest]'s `nonCommentSource` and
 * [com.mordred.aero.verification.VER01GradientProportionalitySourceTest]'s copy, so a `.clip(`
 * token discussed only in KDoc prose (e.g. `AeroButtonSurface.kt`'s "an earlier `.clip()`") can
 * never satisfy or defeat this gate. Per-file copy per that file's own convention.
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
 * Every `.kt` file under the library main source root (D-06: library-wide). cwd-independent
 * resolution, matching [com.mordred.aero.verification.VER01GradientProportionalitySourceTest]'s
 * copy of the six shipped `*SourceTest.kt` guards' `sourceFile()` idiom.
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

/** RED — one chain: aeroSurface( then aeroGlowRing(. */
private const val VIOLATING_GLOW_AFTER_SURFACE = """
@Composable
private fun BadGlowOrder(state: SegmentState, style: AeroSurfaceStyle, glowColor: Color) {
    Box(
        modifier = Modifier
            .aeroSurface(style, RoundedCornerShape(4.dp))
            .aeroGlowRing(
                active = state.hovered,
                glowColor = glowColor,
                cornerRadius = 4.dp,
            )
    )
}
"""

/** GREEN — [VIOLATING_GLOW_AFTER_SURFACE]'s counterpart, differing only in call order. */
private const val CLEAN_GLOW_BEFORE_SURFACE = """
@Composable
private fun GoodGlowOrder(state: SegmentState, style: AeroSurfaceStyle, glowColor: Color) {
    Box(
        modifier = Modifier
            .aeroGlowRing(
                active = state.hovered,
                glowColor = glowColor,
                cornerRadius = 4.dp,
            )
            .aeroSurface(style, RoundedCornerShape(4.dp))
    )
}
"""

/** RED — one chain: aeroSurface( then .clip(. */
private const val VIOLATING_CLIP_AFTER_SURFACE = """
@Composable
private fun BadClipOrder(style: AeroSurfaceStyle, shape: Shape) {
    Box(
        modifier = Modifier
            .aeroSurface(style, shape)
            .clip(shape)
    )
}
"""

/** GREEN — [VIOLATING_CLIP_AFTER_SURFACE]'s counterpart, differing only in call order. */
private const val CLEAN_CLIP_BEFORE_SURFACE = """
@Composable
private fun GoodClipOrder(style: AeroSurfaceStyle, shape: Shape) {
    Box(
        modifier = Modifier
            .clip(shape)
            .aeroSurface(style, shape)
    )
}
"""

/**
 * GREEN — reproduces `AeroSegmentedControl.kt`'s real shape: an outer chain holding `.clip(` with
 * no `aeroSurface(`, and a separate, unrelated inner chain holding `aeroSurface(` with no `.clip(`
 * (VER-02/adjacency). Neither detector may confuse tokens across the two chains.
 */
private const val CLEAN_TWO_UNRELATED_CHAINS = """
@Composable
private fun TwoUnrelatedChains(style: AeroSurfaceStyle, shape: Shape, interactionSource: MutableInteractionSource) {
    Row(
        modifier = Modifier
            .clip(shape)
            .hoverable(interactionSource)
    ) {
        Box(
            modifier = Modifier
                .aeroSurface(style, shape)
        )
    }
}
"""

/**
 * GREEN — the only `.clip(` token sits inside a KDoc line above a chain that calls `aeroSurface(`
 * (VER-02/encoding; `AeroButtonSurface.kt`'s KDoc is the real file this protects).
 */
private const val CLEAN_CLIP_ONLY_IN_COMMENT = """
/**
 * An earlier `.clip()` in this chain would erase everything drawn after it; aeroSurface owns
 * clipping internally now, so no call site here applies its own `.clip(` directly.
 */
@Composable
private fun GoodDocumentedChain(style: AeroSurfaceStyle, shape: Shape) {
    Box(
        modifier = Modifier
            .aeroSurface(style, shape)
    )
}
"""

/** GREEN — zero aeroSurface( call sites; an empty scan set is a pass (VER-02/empty). */
private const val CLEAN_NO_AERO_SURFACE_CALLS = """
private fun ordinaryFunction(a: Int, b: Int): Int {
    return a + b
}
"""
