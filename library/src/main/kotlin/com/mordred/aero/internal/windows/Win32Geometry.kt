package com.mordred.aero.internal.windows

import kotlin.math.roundToInt

/**
 * WIN-01: pure maximized-client-rect math and frame-thickness helpers. No JNA/Compose
 * imports — every value here is a plain Int, so this is unit-testable headlessly, the same
 * discipline as `WndProcSupport.kt` / `PanelDistribution.kt`.
 */

internal data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

internal enum class ScreenEdge { Left, Top, Right, Bottom }

internal const val AUTO_HIDE_INSET_PX: Int = 2

/**
 * WIN-01 / PITFALLS 8+9: the intersection of [proposed] with [workArea] removes the
 * `WS_THICKFRAME` sizing-border overhang past the monitor edge (Pitfall 8) — robust whether
 * AWT or `DefWindowProc` produced the overhanging proposed rect — then each edge present in
 * [autoHideEdges] is moved inward by [autoHideInsetPx] so an auto-hide taskbar on that edge is
 * never fully swallowed. `rcWork` alone cannot answer this: with auto-hide on, Windows reports
 * `rcWork == rcMonitor` (22-NOTES.md C3), so the inset must be applied explicitly per detected
 * edge, not inferred from the work-area rect.
 */
internal fun maximizedClientRect(
    proposed: PxRect,
    workArea: PxRect,
    autoHideEdges: Set<ScreenEdge>,
    autoHideInsetPx: Int = AUTO_HIDE_INSET_PX,
): PxRect {
    var left = maxOf(proposed.left, workArea.left)
    var top = maxOf(proposed.top, workArea.top)
    var right = minOf(proposed.right, workArea.right)
    var bottom = minOf(proposed.bottom, workArea.bottom)

    if (ScreenEdge.Left in autoHideEdges) left += autoHideInsetPx
    if (ScreenEdge.Top in autoHideEdges) top += autoHideInsetPx
    if (ScreenEdge.Right in autoHideEdges) right -= autoHideInsetPx
    if (ScreenEdge.Bottom in autoHideEdges) bottom -= autoHideInsetPx

    return PxRect(left, top, right, bottom)
}

/**
 * WIN-01: sizing-border thickness in px at the window's current DPI — the sum of the sizing
 * frame metric and the padded-border metric (`GetSystemMetricsForDpi(SM_CXSIZEFRAME` /
 * `SM_CYSIZEFRAME)` + `SM_CXPADDEDBORDER`), both already DPI-scaled by the caller.
 */
internal fun resizeFramePx(sizeFramePx: Int, paddedBorderPx: Int): Int = sizeFramePx + paddedBorderPx

/**
 * WIN-02: Compose Multiplatform's own undecorated resizer thickness
 * (`WindowDecorationDefaults.ResizerThickness`), mirrored here so the native band fully
 * shadows CMP's resizer — no Compose-side resize zone can sit outside the native band and
 * receive the press (no double resize path, T-22-25).
 */
internal const val CMP_UNDECORATED_RESIZER_DP: Int = 8

/**
 * WIN-02 / PITFALLS 13: the edge/corner resize-band thickness in physical px — the larger of
 * the DPI-scaled system sizing frame ([resizeFramePx] of the SM_CXSIZEFRAME /
 * SM_CXPADDEDBORDER metrics at the window's current DPI) and CMP's own 8 dp undecorated
 * resizer at the same scale.
 */
internal fun resizeBandPx(sizeFramePx: Int, paddedBorderPx: Int, scale: Float): Int =
    maxOf(resizeFramePx(sizeFramePx, paddedBorderPx), (CMP_UNDECORATED_RESIZER_DP * scale).roundToInt())
