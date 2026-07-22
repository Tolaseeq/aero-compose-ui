package com.mordred.showcase.scratch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * PHASE 15 -> PHASE 16 HANDOFF ARTIFACT (TOOL-07).
 *
 * Confirmed against Compose Multiplatform 1.11.1 real jar on 2026-07-22 by direct
 * bytecode inspection (javap) of the actual dependency jars pulled into the Gradle cache
 * for this build, AND by a successful `./gradlew :showcase:compileKotlin` using the
 * corrected imports below. The RESEARCH.md signature was WRONG on package location for
 * the two Modifier extension functions — this is the discrepancy TOOL-07 exists to catch:
 *
 * DISCREPANCY FOUND (correcting RESEARCH.md's MEDIUM-HIGH-confidence signature):
 * - `Modifier.dropShadow(shape, shadow)` and `Modifier.innerShadow(shape, shadow)` (plus
 *   their `DropShadowScope`/`InnerShadowScope` lambda-overload siblings) live in package
 *   **`androidx.compose.ui.draw`** (file `ui-desktop-1.11.1.jar!/androidx/compose/ui/draw/ShadowKt.class`),
 *   NOT `androidx.compose.ui.graphics.shadow` as RESEARCH.md's Code Examples stated. Only
 *   the `Shadow` value class itself lives in `androidx.compose.ui.graphics.shadow.Shadow`
 *   (`ui-graphics-desktop-1.11.1.jar`) — the research got the `Shadow` class package right
 *   but the two modifier-function packages wrong.
 * - `DpOffset` lives in **`androidx.compose.ui.unit`**, not `androidx.compose.ui.geometry`
 *   (the research skeleton's import was wrong for this type too).
 *
 * CONFIRMED WORKING SIGNATURE (verbatim, corrected):
 * ```kotlin
 * // package androidx.compose.ui.draw
 * fun Modifier.dropShadow(shape: Shape, shadow: Shadow): Modifier
 * fun Modifier.dropShadow(shape: Shape, block: DropShadowScope.() -> Unit): Modifier
 * fun Modifier.innerShadow(shape: Shape, shadow: Shadow): Modifier
 * fun Modifier.innerShadow(shape: Shape, block: InnerShadowScope.() -> Unit): Modifier
 *
 * // package androidx.compose.ui.graphics.shadow (this part of the research WAS correct)
 * class Shadow(
 *     radius: Dp,
 *     color: Color = Color.Black,
 *     spread: Dp = 0.dp,
 *     offset: DpOffset = DpOffset.Zero,   // androidx.compose.ui.unit.DpOffset
 *     alpha: Float = 1f,
 *     blendMode: BlendMode = DefaultBlendMode,
 * )
 * // plus a Brush-based overload (radius, brush, spread, offset, alpha, blendMode) — same
 * // parameter order, confirmed present via javap on the same Shadow.class.
 * ```
 * Constructor parameter shape/order (radius, color-or-brush, spread, offset, alpha,
 * blendMode) matched the research exactly once resolved. Phase 16 should import
 * `dropShadow`/`innerShadow` from `androidx.compose.ui.draw` and `Shadow` from
 * `androidx.compose.ui.graphics.shadow` — this is the corrected, compile-proven basis.
 *
 * Ordering rule proven here: `dropShadow` precedes `.background()` (shadow drawn behind
 * the fill); `innerShadow` follows `.background()` (drawn on top, recessed).
 *
 * SCOPE GUARD: this is a compile-proof scratch, not a production primitive. Do NOT wire
 * this into any real showcase screen or component (see 15-05-PLAN.md).
 */
@Composable
internal fun ScratchAeroShadowProof() {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(120.dp, 40.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 6.dp,
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = DpOffset(0.dp, 2.dp),
                ),
            )
            .background(color = Color(0xFF3A6EA5), shape = shape)
            .innerShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 2.dp,
                    color = Color.White.copy(alpha = 0.45f),
                    offset = DpOffset(0.dp, 1.dp),
                ),
            ),
    )
}
