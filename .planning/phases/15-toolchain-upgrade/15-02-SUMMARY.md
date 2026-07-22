---
phase: 15-toolchain-upgrade
plan: 02
subsystem: infra
tags: [kotlin, compose-multiplatform, gradle, material3, jetbrains, toolchain]

# Dependency graph
requires:
  - phase: 15-toolchain-upgrade (plan 01)
    provides: Confirmed pre-migration baseline (Kotlin 2.1.21 + CMP 1.7.3) and pre-migration visual screenshots
provides:
  - "Kotlin 2.4.10 + Compose Multiplatform 1.11.1 confirmed compiling and building, verified empirically (not assumed)"
  - "Material3 pinned to explicit stable coordinate 1.9.0 in both library (api) and showcase (implementation) — zero alpha M3 anywhere in the build"
  - "Green light for plans 03-06 to proceed on the locked target pairing (no fallback needed)"
affects: [15-03, 15-04, 15-05, 15-06, 16-foundation-aero-primitives]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Bundled known-safe fixes applied together on first build attempt, not sequential probing (per CONTEXT.md escalation threshold / RESEARCH.md Pitfall 3)"
    - "Explicit stable Maven coordinate pin over `compose.*` alias when the alias resolves to a moving/alpha target"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - library/build.gradle.kts
    - showcase/build.gradle.kts

key-decisions:
  - "Kotlin 2.4.10 + CMP 1.11.1 pairing compiles and builds cleanly on first bundled attempt — no escalation/fallback needed; Task 3 (conditional escalation checkpoint) was skipped entirely per its own gating rule"

patterns-established:
  - "Version catalog single-source: bumping libs.versions.toml kotlin entry auto-tracks the compose-compiler plugin (version.ref = \"kotlin\") with zero separate edit"

requirements-completed: [TOOL-01, TOOL-02]

coverage:
  - id: D1
    description: "Library and showcase compile and build on Kotlin 2.4.10 + Compose Multiplatform 1.11.1 (the load-bearing, previously-unverified pairing)"
    requirement: "TOOL-01"
    verification:
      - kind: unit
        ref: "./gradlew :library:compileKotlin"
        status: pass
      - kind: integration
        ref: "./gradlew build"
        status: pass
    human_judgment: false
  - id: D2
    description: "Material3 resolves to the explicit stable coordinate 1.9.0 in both library (api-scoped) and showcase (implementation-scoped); no alpha material3 artifact remains anywhere in the build"
    requirement: "TOOL-02"
    verification:
      - kind: unit
        ref: "./gradlew :library:dependencies --configuration compileClasspath | grep material3"
        status: pass
      - kind: unit
        ref: "./gradlew :showcase:dependencies --configuration compileClasspath | grep material3"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-07-22
status: complete
---

# Phase 15 Plan 02: Toolchain Build Gate (TOOL-01/TOOL-02) Summary

**Kotlin 2.4.10 + Compose Multiplatform 1.11.1 compiles and builds cleanly on the first bundled attempt, with Material3 pinned to explicit stable 1.9.0 everywhere — no escalation, no fallback needed.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-07-22 (session start)
- **Completed:** 2026-07-22T10:23:28Z
- **Tasks:** 2 of 3 executed (Task 3 conditionally skipped — gate passed)
- **Files modified:** 3

## Accomplishments
- Bumped `gradle/libs.versions.toml`: `kotlin 2.1.21 -> 2.4.10`, `composeMultiplatform 1.7.3 -> 1.11.1`
- Replaced the `compose.material3` alias (which resolves to `1.11.0-alpha07` at CMP 1.11.x) with the explicit stable coordinate `org.jetbrains.compose.material3:material3:1.9.0` in both `library/build.gradle.kts` (`api`-scoped) and `showcase/build.gradle.kts` (`implementation`-scoped)
- Confirmed (without editing) `compose-compiler` plugin still `version.ref = "kotlin"`, auto-tracking the new Kotlin version
- Ran the load-bearing build gate for real: `./gradlew :library:compileKotlin` (BUILD SUCCESSFUL, ~2m3s) then `./gradlew build` (BUILD SUCCESSFUL, ~13s) — the Kotlin 2.4.10 + CMP 1.11.1 pairing, never shipped matched by JetBrains, compiles and builds without any additional fixes beyond the three bundled known-safe ones
- Verified TOOL-02 empirically: `./gradlew :library:dependencies --configuration compileClasspath | grep material3` and the same for `:showcase:dependencies` both show `org.jetbrains.compose.material3:material3:1.9.0` (and its `material3-desktop:1.9.0` transitive) with no `-alpha`/`-beta` suffix anywhere
- Re-confirmed the prior Popup/PopupProperties audit (cheap regression re-check, not fresh investigation): all 14 real `Popup(` call sites in `library/src/main` still reference `PopupProperties` explicitly

## Task Commits

Each task was committed atomically:

1. **Task 1: Bump toolchain and pin stable Material3 (the three known-safe fixes, bundled)** - `eaf3af9` (feat)
2. **Task 2: Run the build gate and verify Material3 resolves to stable 1.9.0** - no file changes (verification-only task); folded into this documentation commit
3. **Task 3: CONDITIONAL escalation** - SKIPPED (gate passed on Task 2; per the task's own instruction to skip entirely when the build succeeds)

**Plan metadata:** (this commit, following SUMMARY.md creation)

## Files Created/Modified
- `gradle/libs.versions.toml` - `kotlin` bumped `2.1.21 -> 2.4.10`; `composeMultiplatform` bumped `1.7.3 -> 1.11.1`; `compose-compiler` plugin line left unchanged (already `version.ref = "kotlin"`)
- `library/build.gradle.kts` - `api(compose.material3)` replaced with `api("org.jetbrains.compose.material3:material3:1.9.0")`; all other `api(compose.*)` lines unchanged
- `showcase/build.gradle.kts` - `implementation(compose.material3)` replaced with `implementation("org.jetbrains.compose.material3:material3:1.9.0")`

## Decisions Made
- No fallback was needed. The locked target pairing (Kotlin 2.4.10 + CMP 1.11.1) compiled and built successfully on the very first attempt with the three bundled known-safe fixes (Material3 pin in both modules + compose-compiler version-ref confirmation) — Task 3's conditional escalation checkpoint was never engaged, per its own explicit "SKIP THIS TASK ENTIRELY if the Task 2 build gate PASSED" instruction.
- No architectural or scope deviation occurred: the diff touches exactly the three build files named in the plan; zero rendering/drawing/component-behavior code was touched, honoring the plan's SCOPE GUARD.

## Deviations from Plan

None - plan executed exactly as written. The build gate passed on the first bundled attempt; no fourth fix, no fallback substitution, no scope expansion.

## Issues Encountered

None. Gradle emitted only pre-existing deprecation warnings unrelated to this plan's scope (e.g. `compose.foundation`/`compose.ui` alias deprecation notices, a Gradle-8.14.3-vs-Kotlin-2.5.0-floor warning, `runComposeUiTest` v1 deprecation warning in `AeroPanelGroupRecomposeUiTest.kt`) — all expected per RESEARCH.md's "State of the Art" table and explicitly out of this plan's scope (the `runComposeUiTest` v1->v2 port is plan 03's TOOL-03/TOOL-04 work, not this plan's).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 03-06 can proceed on the confirmed toolchain: Kotlin 2.4.10 + Compose Multiplatform 1.11.1, Material3 pinned to stable 1.9.0, with zero fallback substitution.
- Plan 03 (test-infra port: `AeroPanelGroupRecomposeUiTest` for the `Unconfined`->`Standard` `TestDispatcher` default change, TOOL-03/TOOL-04) can proceed directly — the deprecation warning observed this session (`runComposeUiTest` v1 deprecated in favor of v2) is exactly the expected signal RESEARCH.md predicted, not a new blocker.
- No blockers or concerns carried forward from this plan.

---
*Phase: 15-toolchain-upgrade*
*Completed: 2026-07-22*
