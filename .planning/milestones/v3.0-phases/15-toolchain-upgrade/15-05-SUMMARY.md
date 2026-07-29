---
phase: 15-toolchain-upgrade
plan: 05
subsystem: infra
tags: [compose-multiplatform, kotlin, dropShadow, innerShadow, shadow-api, phase-handoff]

# Dependency graph
requires:
  - phase: 15-toolchain-upgrade (plan 02)
    provides: "Kotlin 2.4.10 + Compose Multiplatform 1.11.1 confirmed compiling and building, Material3 pinned to explicit stable 1.9.0"
provides:
  - "A committed, compile-proven dropShadow/innerShadow scratch composable at showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt"
  - "The CORRECTED (not research-assumed) real package location for Modifier.dropShadow/innerShadow: androidx.compose.ui.draw (not androidx.compose.ui.graphics.shadow) — RESEARCH.md's MEDIUM-HIGH-confidence signature was wrong on this point"
  - "Confirmation that androidx.compose.ui.unit.DpOffset (not androidx.compose.ui.geometry.DpOffset) is the correct import"
  - "Confirmation that the Shadow constructor parameter shape/order (radius, color-or-brush, spread, offset, alpha, blendMode) matched research exactly"
affects: [16-foundation-aero-primitives]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "javap-based bytecode inspection of Gradle-cached jars used to resolve a compiler discrepancy deliberately, instead of blind trial-and-error import guessing"

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt
  modified: []

key-decisions:
  - "The research signature's package for dropShadow/innerShadow (androidx.compose.ui.graphics.shadow) was corrected to the compiler-confirmed androidx.compose.ui.draw; this correction is recorded verbatim in the file's KDoc as the Phase 16 source of truth, not silently trial-and-errored away"

patterns-established:
  - "Phase 15->16 handoff artifact pattern: a committed (not throwaway) scratch composable carrying a compile-proven API signature, consumed directly by the next phase instead of re-verified from documentation"

requirements-completed: [TOOL-07]

coverage:
  - id: D1
    description: "dropShadow/innerShadow scratch composable compiles against the real Compose Multiplatform 1.11.1 jar"
    requirement: "TOOL-07"
    verification:
      - kind: unit
        ref: "./gradlew :showcase:compileKotlin"
        status: pass
    human_judgment: false
  - id: D2
    description: "KDoc records the compiler-confirmed dropShadow/innerShadow/Shadow signature verbatim, including the corrected package location discrepancy from RESEARCH.md"
    requirement: "TOOL-07"
    verification:
      - kind: other
        ref: "grep -q 'Confirmed against Compose Multiplatform 1.11.1' showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-07-22
status: complete
---

# Phase 15 Plan 05: Compile-Verified dropShadow/innerShadow Scratch Composable Summary

**Research signature was corrected, not confirmed as-is: `Modifier.dropShadow`/`innerShadow` actually live in `androidx.compose.ui.draw` (not `androidx.compose.ui.graphics.shadow` as RESEARCH.md assumed), and `DpOffset` lives in `androidx.compose.ui.unit` (not `androidx.compose.ui.geometry`) — found and fixed via `javap` bytecode inspection of the real 1.11.1 jars, then compile-proven.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-07-22 (session start)
- **Completed:** 2026-07-22
- **Tasks:** 1 of 1
- **Files modified:** 1 (created)

## Accomplishments
- Created `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt`, a `Box` demonstrating `dropShadow` before `.background(...)` and `innerShadow` after it
- First compile attempt (using RESEARCH.md's literal signature/imports) FAILED with `Unresolved reference` errors for `dropShadow`, `innerShadow`, and `DpOffset` — this was investigated deliberately via `javap` bytecode inspection of the actual jars in the Gradle cache (`ui-desktop-1.11.1.jar`, `ui-graphics-desktop-1.11.1.jar`), not fixed by blind trial-and-error
- Root cause found: the two `Modifier` extension functions (`dropShadow`, `innerShadow`) and their `DropShadowScope`/`InnerShadowScope` lambda-overload siblings are compiled into `androidx.compose.ui.draw.ShadowKt` (module `ui`), not into the `androidx.compose.ui.graphics.shadow` package (module `ui-graphics`) as RESEARCH.md's Code Examples stated. Only the `Shadow` value class itself is correctly in `androidx.compose.ui.graphics.shadow.Shadow`. `DpOffset` was also mis-imported from `androidx.compose.ui.geometry` instead of `androidx.compose.ui.unit`.
- Corrected the imports (`androidx.compose.ui.draw.dropShadow`, `androidx.compose.ui.draw.innerShadow`, `androidx.compose.ui.unit.DpOffset`, keeping `androidx.compose.ui.graphics.shadow.Shadow` as-is) and re-ran `./gradlew :showcase:compileKotlin` — BUILD SUCCESSFUL
- The `Shadow` constructor's parameter shape/order (`radius, color/brush, spread, offset, alpha, blendMode`, all with the research-stated defaults) matched RESEARCH.md exactly, confirmed by `javap -p` on `Shadow.class` — no discrepancy there
- Updated the file's KDoc to record the corrected, compiler-confirmed signature verbatim, explicitly flagging the package-location discrepancy as the finding for the Phase 16 planner to read (per TOOL-07's own point: this discrepancy IS the deliverable, not something to hide)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create the scratch composable, compile-verify it, and record the confirmed signature** - `1b56f5b` (feat)

**Plan metadata:** (this commit, following SUMMARY.md creation)

## Files Created/Modified
- `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` - New scratch composable; compiles against the real CMP 1.11.1 jar; KDoc records the corrected `dropShadow`/`innerShadow`/`Shadow`/`DpOffset` signature and package locations for Phase 16 to consume directly

## Decisions Made
- Corrected the research's package-location assumption in the committed artifact rather than leaving RESEARCH.md's original (now-known-wrong) signature unaddressed — the KDoc is now the canonical, compile-proven reference Phase 16 reads, superseding RESEARCH.md's Code Examples on this specific point
- Used `javap` bytecode inspection of the actual Gradle-cached jars to pinpoint the exact real package/class locations before touching the source file a second time — this was a deliberate, evidence-based correction, not iterative trial-and-error

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected wrong import packages for `dropShadow`/`innerShadow`/`DpOffset`**
- **Found during:** Task 1 (initial compile attempt using RESEARCH.md's literal skeleton)
- **Issue:** RESEARCH.md's Code Examples imported `dropShadow`/`innerShadow` from `androidx.compose.ui.graphics.shadow` and `DpOffset` from `androidx.compose.ui.geometry`; the real 1.11.1 jar does not expose those symbols at those paths (`Unresolved reference` compile errors)
- **Fix:** Inspected the real jars via `javap -p` (`ui-desktop-1.11.1.jar` and `ui-graphics-desktop-1.11.1.jar` from the Gradle module cache) to find the actual declaring classes, then corrected the imports to `androidx.compose.ui.draw.dropShadow`, `androidx.compose.ui.draw.innerShadow`, and `androidx.compose.ui.unit.DpOffset`
- **Files modified:** `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt`
- **Verification:** `./gradlew :showcase:compileKotlin` — BUILD SUCCESSFUL after the correction
- **Committed in:** `1b56f5b` (Task 1 commit — the file was only committed once it compiled)

---

**Total deviations:** 1 auto-fixed (1 bug/discrepancy-resolution, which is the plan's entire point per TOOL-07)
**Impact on plan:** This is not scope creep — the discrepancy the compiler surfaced (wrong package for the Modifier extension functions and for `DpOffset`) is exactly the finding TOOL-07 exists to catch and record. No trial-and-error guessing occurred; the correction was derived from direct bytecode inspection of the real artifact before any second compile attempt.

## Issues Encountered
None beyond the documented deviation above — that discrepancy was expected in kind (RESEARCH.md flagged the shadow signature as only MEDIUM-HIGH confidence, "verify at build time") even though the specific shape of the discrepancy (package location, not parameter shape) wasn't predicted.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 16's Aero-primitives work (`GlassModifiers`/`AeroSurfacePrimitives`) can import `Modifier.dropShadow`/`Modifier.innerShadow` from `androidx.compose.ui.draw` and `Shadow` from `androidx.compose.ui.graphics.shadow` directly, source-verified rather than re-verified from documentation.
- No blockers or concerns carried forward from this plan.
- Remaining Phase 15 work: plan 06 (JitPack pre-release tag / TOOL-08), per the roadmap sequence.

## Self-Check: PASSED

- FOUND: showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt
- FOUND: 1b56f5b (Task 1 commit)

---
*Phase: 15-toolchain-upgrade*
*Completed: 2026-07-22*
