package com.mordred.showcase.sections

import androidx.compose.runtime.Composable
import com.mordred.aero.scratch.AeroPhase7Scratch

/**
 * **TEMPORARY — deleted in Phase 11.**
 *
 * Showcase entry point for the Phase 7 scratch / sign-off surface. The actual scratch
 * implementation lives at `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`
 * because it directly invokes `internal` Phase 7 primitives (Plan-01 ADR locks every primitive
 * `internal`; Kotlin's `internal` is module-scoped, so the showcase can only consume them
 * through a single `public` aggregator inside the same library module).
 *
 * Phase 11 cleanup:
 * 1. Delete `library/.../scratch/AeroPhase7Scratch.kt`.
 * 2. Delete this file.
 * 3. Remove the `Phase7ScratchSection()` call + import from `ShowcaseApp.kt`.
 */
@Composable
public fun Phase7ScratchSection() {
    AeroPhase7Scratch()
}
