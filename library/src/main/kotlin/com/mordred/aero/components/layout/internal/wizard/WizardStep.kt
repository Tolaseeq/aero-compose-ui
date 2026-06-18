package com.mordred.aero.components.layout.internal.wizard

/**
 * Returns the next step index after a Next/Finish click with validate-gate applied.
 *
 * - If [valid] is `false` the caller's `onValidate` returned false; returns [current] unchanged
 *   (Next is blocked — user stays on the current step).
 * - If [valid] is `true` returns `current + 1`, clamped to [lastIndex] so this function never
 *   overflows. The composable decides whether to call `onFinish` or advance (via [isLastStep]).
 *
 * Pure function — no Compose imports, JVM-testable without instrumentation.
 */
internal fun nextStepIndex(current: Int, lastIndex: Int, valid: Boolean): Int =
    if (!valid) current else (current + 1).coerceAtMost(lastIndex)

/**
 * Returns the previous step index for a Back click.
 *
 * Clamped to 0 — Back on the first step is a no-op (stays at 0).
 *
 * Pure function — no Compose imports, JVM-testable without instrumentation.
 */
internal fun prevStepIndex(current: Int): Int = (current - 1).coerceAtLeast(0)

/**
 * Returns `true` when [current] is the last step (i.e., `current >= lastIndex`).
 *
 * Used by the wizard composable to switch Next to Finish and to guard the `onFinish` call.
 *
 * Pure function — no Compose imports, JVM-testable without instrumentation.
 */
internal fun isLastStep(current: Int, lastIndex: Int): Boolean = current >= lastIndex
