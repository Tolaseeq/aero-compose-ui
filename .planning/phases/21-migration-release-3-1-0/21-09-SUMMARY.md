---
phase: 21-migration-release-3-1-0
plan: 09
subsystem: infra
tags: [gradle, kotlin, kotlinx-coroutines, kotlinx-datetime, junit, dependency-catalog]

# Dependency graph
requires:
  - phase: 21-migration-release-3-1-0 (Plan 08)
    provides: Gradle 9.7.1 / JDK 21 / Kotlin 2.4.20 / Compose Multiplatform 1.12.0 / Compose Hot Reload 1.2.0 already in place, MCP connection verified
provides:
  - kotlinx-coroutines 1.11.0
  - kotlinx-datetime 0.8.0 (plain, no -0.6.x-compat) with the compiler-forced Clock rename applied to the 4 picker files
  - JUnit 6.1.3, aligned across jupiter/platform/launcher without needing an explicit junit-bom
  - Final toolchain table (all 8 components) recorded in 21-TOOLCHAIN-LOG.md
affects: [21-10, 21-11, 21-12, 21-13, 21-14, release-notes, README-min-requirements]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Empirical-tiebreaker discipline: compile against the bare version bump first, record every e:/w: line verbatim, then fix only what the compiler calls an error — never what research predicted would break"

key-files:
  created:
    - .captures/smoke/plan09-final/manifest.json
    - .captures/smoke/plan09-final/AeroBlue/Pickers-p0-c1.png
    - .captures/smoke/plan09-final/AeroBlue/Data-p0-c1.png
    - .captures/smoke/plan09-final/AeroBlue/Data-p1-c1.png
  modified:
    - gradle/libs.versions.toml
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt
    - .planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md

key-decisions:
  - "kotlinx-datetime 0.8.0 broke exactly 4 sites (Clock.System.now(), the compiler-visible errors), not the 10 additional .dayOfMonth/.monthNumber sites 21-RESEARCH.md's Pitfall 1 predicted — those remained deprecation warnings, left untouched per this task's own warnings-are-recorded-not-changed rule"
  - "JUnit 6.1.3 alignment needed no junit-bom addition: junit-jupiter's own POM already publishes org.junit:junit-bom:6.1.3 as a platform constraint Gradle resolves transitively, overriding kotlin-test-junit5's older 5.10.1/1.10.1 requests by conflict resolution"

patterns-established: []

requirements-completed: [TOOL-13, TOOL-14, TOOL-15, TOOL-12, TOOL-16, TOOL-17]

# Metrics
duration: 30min
completed: 2026-09-23
---

# Phase 21 Plan 09: Toolchain Migration — coroutines/datetime/JUnit Summary

**kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0 (plain, 4-site compiler-forced Clock rename only), and JUnit 6.1.3 (auto-aligned via junit-bom platform constraint, no build-file change needed), each its own commit, gate held at the locked 541/0 test count throughout.**

## Performance

- **Duration:** ~30 min
- **Started:** 2026-09-23T22:40:00+03:00 (approx, first file read)
- **Completed:** 2026-09-23T23:09:00+03:00
- **Tasks:** 3
- **Files modified:** 6 (2 shared across commits: `gradle/libs.versions.toml`, `21-TOOLCHAIN-LOG.md`)

## Accomplishments
- kotlinx-coroutines bumped 1.10.2 → 1.11.0, zero mechanical fixes needed
- kotlinx-datetime bumped 0.6.2 → plain 0.8.0; the compiler decided the fix scope empirically (4 sites, not the 14 research anticipated) — `import kotlinx.datetime.Clock` → `import kotlin.time.Clock` in `AeroDatePicker.kt`, `AeroDateTimePicker.kt`, `AeroDateRangePicker.kt`, `AeroDateTimeRangePicker.kt`
- JUnit bumped 5.10.0 → 6.1.3, full jupiter/platform/launcher alignment confirmed via `dependencyInsight`, achieved without touching `library/build.gradle.kts`
- Gate (`AERO_TEST_COUNT total=541 skipped=0`) held after every single bump; `MATERIAL3 OK`; final TOOL-17 smoke shows the showcase launching clean on the complete final toolchain with Pickers and Data sections rendering correctly

## Task Commits

Each task was committed atomically:

1. **Task 1: kotlinx-coroutines 1.11.0 (TOOL-13)** - `19dbce3` (build)
2. **Task 2: kotlinx-datetime 0.8.0 plain — compiler-forced renames (TOOL-14)** - `1b9e847` (build)
3. **Task 3: JUnit 6.1.3 aligned; final Material3 and showcase launch checks (TOOL-15, TOOL-12, TOOL-17)** - `65e9617` (build)

## Files Created/Modified
- `gradle/libs.versions.toml` - `kotlinxCoroutines` 1.10.2→1.11.0, `kotlinxDatetime` 0.6.2→0.8.0 (plain), `junit` 5.10.0→6.1.3
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt` - `import kotlinx.datetime.Clock` → `import kotlin.time.Clock`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` - same rename
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` - same rename
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt` - same rename
- `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` - Steps 5a/5b/5c appended: verbatim compiler output, dependencyInsight summary, final toolchain table
- `.captures/smoke/plan09-final/` - fresh TOOL-17 smoke sweep (3 frames, AeroBlue Pickers+Data); pre-upgrade baseline (`.captures/old-kt2.4.10-cmp1.11.1/`, 1152 files) untouched

## Decisions Made
- Treated the kotlinx-datetime 0.8.0 compile run as the sole source of truth for scope, per the plan's explicit "the compiler decides" rule — 21-RESEARCH.md Pitfall 1 predicted 14 candidate sites (4 `Clock` imports + 10 `.dayOfMonth`/`.monthNumber` call sites); the actual compiler-reported `e:` errors were only the 4 `Clock` imports. The 10 `.dayOfMonth`/`.monthNumber` sites compile as deprecation warnings in 0.8.0, not errors, and were left untouched per the plan's own "warnings are recorded, not changed" instruction. This is recorded as an assumption drift (see below), not a deviation — the plan itself anticipated and named this exact possibility as "SUMMARY Conflict 1 — empirical tiebreaker."
- JUnit alignment via the transitive `junit-bom` platform constraint already present in `junit-jupiter`'s POM, rather than adding an explicit `junit-bom` catalog entry — the plan's sanctioned BOM fallback was available but not needed; `dependencyInsight` confirmed 6.1.3 everywhere without it.

## Deviations from Plan

None — plan executed exactly as written. The narrower-than-predicted kotlinx-datetime fix scope and the not-needed JUnit BOM are both outcomes the plan itself pre-authorized ("the compiler decides", "the only sanctioned fix is the JUnit BOM... if they are not aligned") — neither required Rule 1-4 judgment calls outside the plan's own instructions.

## Assumption Drift (advisory)

- **Found during:** Task 2 (kotlinx-datetime 0.8.0)
- **Planned assumption:** 21-RESEARCH.md's Pitfall 1 / "Exact kotlinx-datetime rename sites" section listed 14 candidate sites (4 `Clock` imports + 10 `.dayOfMonth`/`.monthNumber` call sites across library main, library test, and showcase) as the expected mechanical-fix scope for TOOL-14.
- **Actual:** The compiler reported only 4 `e:` errors (the `Clock.System` unresolved-reference sites); the 10 `.dayOfMonth`/`.monthNumber` sites compile as `w:` deprecation warnings in kotlinx-datetime 0.8.0, not errors.
- **Why:** kotlinx-datetime 0.8.0 kept `.dayOfMonth`/`.monthNumber` as deprecated-but-functional extension properties rather than removing them outright; only `kotlinx.datetime.Clock` was hard-removed in favor of `kotlin.time.Clock`. This was exactly the plan's own named "SUMMARY Conflict 1 — empirical tiebreaker," so the plan's own rule ("fix ONLY the sites the compiler reported as errors... Warnings are recorded, not changed") governed and was followed. No architectural or scope decision was needed — the narrower outcome is a strictly smaller edit than the research anticipated.

## Issues Encountered
None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

All three ROADMAP step-5 dependency bumps are complete; the full final toolchain (Gradle 9.7.1, JDK 21, Kotlin 2.4.20, Compose Multiplatform 1.12.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3, Material3 1.9.0 pinned, Compose Hot Reload 1.2.0) is recorded in `21-TOOLCHAIN-LOG.md` and confirmed by a live showcase smoke sweep. No blockers for the remaining Phase 21 plans (showcase-wide visual sweep / release steps).

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All created/modified files confirmed present on disk; all 4 commits (`19dbce3`, `1b9e847`, `65e9617`, `97e4761`) confirmed present in `git log`.
