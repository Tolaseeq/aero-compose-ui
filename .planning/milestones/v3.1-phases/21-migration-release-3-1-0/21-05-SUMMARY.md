---
phase: 21-migration-release-3-1-0
plan: 05
subsystem: build
tags: [gradle, test-count-guard, tool-16, red-green-proof]

# Dependency graph
requires:
  - phase: 21-04
    provides: "541/93 green full-suite baseline the guard's lockedTestTotal reuses (measured live, not copied)"
provides:
  - "TOOL-16 test-count guard in library/build.gradle.kts: addTestListener + doLast enforcement, lockedTestTotal=541/lockedTestSkipped=0, red-then-green proven on a deliberately excluded test class"
  - "21-TOOLCHAIN-LOG.md — running record of every phase step, started with the Step 2 test-count-guard entry"
  - "./gradlew :library:test --rerun is the fixed gate command every later toolchain-bump plan (06+) must run before/after each bump"
affects: [21-06, 21-07, 21-08, 21-09, 21-10, 21-11, 21-12, 21-13, 21-14]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TestListener.afterSuite(suite, result) with suite.parent == null isolates the root JUnit Platform suite from per-class/per-method suites for a single authoritative total/skipped count"
    - "Casting Test.filter to the internal org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter to read commandLineIncludePatterns, since this Gradle version's public TestFilter interface only exposes build-script-configured includePatterns"

key-files:
  created:
    - .planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md
  modified:
    - library/build.gradle.kts

key-decisions:
  - "lockedTestTotal=541 / lockedTestSkipped=0 comes from an actual ./gradlew :library:test --rerun run on this session's untouched pre-upgrade toolchain (Kotlin 2.4.10/CMP 1.11.1), not from the 541 figure Plans 03/04 already reported in prose — the two independently agree, which is expected since no test code changed between Plan 04's last commit and this measurement, but the number in the build script is this plan's own live run, not a copy"
  - "grep -rc \"@Test\" library/src/test totals 545, 4 more than the Gradle-executed 541; fully explained by two KDoc prose mentions of the literal text \"@Test\" each in VER01GradientProportionalitySourceTest.kt and VER02AeroSurfaceClipOrderSourceTest.kt (describing their own RED/GREEN fixture halves) — no parameterized/disabled/repeated tests exist in the suite"
  - "filtered detection casts filter to the internal DefaultTestFilter class to read commandLineIncludePatterns, because Gradle 8.14.3's public org.gradle.api.tasks.testing.TestFilter interface (confirmed by decompiling the installed Gradle distribution's jar) does not expose that member — only includePatterns/excludePatterns are public; commandLineIncludePatterns exists only on the internal implementation class that --tests actually populates"

requirements-completed: [TOOL-16]

# Metrics
duration: ~25min
completed: 2026-09-23
---

# Phase 21 Plan 05: Test count guard — lock, red-proof, green-proof (TOOL-16) Summary

**The literal test count executed on the old toolchain (Kotlin 2.4.10 / CMP 1.11.1) — 541 tests, 0 skipped — is now locked in `library/build.gradle.kts` behind a `TestListener` + `doLast` guard that fails any unfiltered run reporting a different total or skipped count, proven to actually fail red (excluding `AeroCalendarGridTest`'s 7 methods dropped the total to 534 and threw) before being trusted, then proven green again with the build file back to its committed state.**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-09-23 (previous plan's last commit `50890dc`)
- **Completed:** 2026-09-23 (last commit `a42e2b2`)
- **Tasks:** 2/2 completed
- **Files modified/created:** 2 (1 modified, 1 created)

## Accomplishments
- Live-measured the executed test count on the untouched pre-upgrade toolchain via an actual `./gradlew :library:test --rerun` run: `AERO_TEST_COUNT total=541 skipped=0 filtered=false` — matching Plans 03/04's already-reported 541/93 figure, but measured fresh by this plan rather than copied.
- `library/build.gradle.kts` now reports and enforces the count: `addTestListener` captures the root-suite `testCount`/`skippedTestCount` (`suite.parent == null`), and `doLast` throws `GradleException("Test count guard: ...")` when an unfiltered run's total or skipped count differs from `lockedTestTotal = 541` / `lockedTestSkipped = 0`. Filtered runs (`--tests`) are reported (`filtered=true`) but never enforced.
- Explained the grep-vs-Gradle delta (545 vs 541) precisely: two files each mention the literal text `@Test` twice in KDoc prose, not in annotation position — `VER01GradientProportionalitySourceTest.kt` (11 grep / 9 real) and `VER02AeroSurfaceClipOrderSourceTest.kt` (10 grep / 8 real). 2 + 2 = 4, the exact delta. No parameterized/disabled/repeated tests exist in the suite.
- Proved the guard fails red on a deliberately excluded test class: `exclude("**/AeroCalendarGridTest*")` (7 `@Test` methods) dropped the total to 534 and the build failed with `Test count guard: executed total=534 skipped=0, locked total=541 skipped=0`, exactly as required before the guard could be trusted.
- Proved the guard passes green again once the exclusion was removed: `git diff --quiet HEAD -- library/build.gradle.kts` confirmed the file was byte-identical to the Task 1 commit before rerunning; the rerun printed `AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false` and `BUILD SUCCESSFUL`.
- Confirmed a filtered run still works and is correctly exempted: `--tests "*Base05CaptureProofTest*"` printed `filtered=true`, `total=2`, and passed.
- `21-TOOLCHAIN-LOG.md` created — starting-toolchain table plus the full Step 2 record (measured count, grep delta explanation, verbatim red line, verbatim green line, and the fixed gate command `./gradlew :library:test --rerun` for every later phase step).

## Task Commits

Each task was committed atomically:

1. **Task 1: Count reporting, live measurement and locked constants (TOOL-16)** - `ffece58` (build)
2. **Task 2: Prove the guard red on an excluded class, then green; start the toolchain log (TOOL-16)** - `a42e2b2` (docs)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `library/build.gradle.kts` - `lockedTestTotal`/`lockedTestSkipped` top-level constants (541/0), `addTestListener` root-suite count capture, `doLast` `AERO_TEST_COUNT` report line + `GradleException` enforcement on unfiltered drift
- `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` (new) - starting toolchain table + Step 2 (test count guard) full record

## Decisions Made
- `lockedTestTotal = 541` / `lockedTestSkipped = 0`: this plan's own live `./gradlew :library:test --rerun` run on the still-untouched Kotlin 2.4.10/CMP 1.11.1 toolchain, confirmed by precondition check before starting (catalog still read `kotlin = "2.4.10"`, `composeMultiplatform = "1.11.1"`, wrapper 8.14.3).
- Filtered-run detection casts `Test.filter` to the internal `org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter` to read `commandLineIncludePatterns` (see Deviations — Rule 3 below); this is a build-script-internal implementation detail, not part of the published library's public API surface, so it carries no consumer-facing risk.
- The guard's grep-vs-Gradle delta (545 vs 541) is fully attributed to two files' own KDoc prose mentioning the literal string `@Test`, not to any parameterized/disabled test mechanism — documented in `21-TOOLCHAIN-LOG.md` rather than left as an unexplained gap.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `java.util.concurrent.atomic.AtomicLong` unresolved inside `tasks.test { }`**
- **Found during:** Task 1, first reporting-only build
- **Issue:** `library/build.gradle.kts` already has a top-level `java { withSourcesJar() }` extension-accessor block; inside the Kotlin DSL script, the bare identifier `java` in `java.util.concurrent.atomic.AtomicLong(-1)` resolved to that `JavaPluginExtension` accessor rather than the `java` package, producing `Unresolved reference: util`.
- **Fix:** Added `import java.util.concurrent.atomic.AtomicLong` at the top of the file and used the bare `AtomicLong(-1)` constructor inside `tasks.test { }` — import resolution is unaffected by the local `java` extension-accessor shadowing.
- **Files modified:** `library/build.gradle.kts`
- **Verification:** Re-ran `./gradlew :library:test --rerun --console=plain`; the `Unresolved reference: util` compile error was gone.
- **Committed in:** `ffece58` (Task 1 commit)

**2. [Rule 3 - Blocking] `filter.commandLineIncludePatterns` unresolved — not on this Gradle version's public `TestFilter` interface**
- **Found during:** Task 1, first reporting-only build (same run as deviation 1)
- **Issue:** The plan's interface note (and 21-RESEARCH.md's sketch) assumed `TestFilter.commandLineIncludePatterns` is a public member usable directly. Decompiling the actual installed `gradle-testing-base-8.14.3.jar` (`javap` on `org.gradle.api.tasks.testing.TestFilter`) showed the public interface exposes only `includePatterns`/`excludePatterns` plus the `includeTestsMatching`/`excludeTestsMatching` mutators — `commandLineIncludePatterns` exists solely on the internal implementation class `org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter`, which is what `--tests` actually populates at the CLI layer.
- **Fix:** `(filter as? org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter)?.commandLineIncludePatterns?.isNotEmpty() ?: false`, OR'd with the existing `filter.includePatterns.isNotEmpty()` public check, so filtered-run detection covers both build-script-configured filters and CLI `--tests` filters.
- **Files modified:** `library/build.gradle.kts`
- **Verification:** `./gradlew :library:test --rerun --tests "*Base05CaptureProofTest*"` printed `filtered=true`; the unfiltered full-suite run printed `filtered=false` — both correctly detected.
- **Committed in:** `ffece58` (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (both Rule 3 blocking, both discovered and fixed in the same reporting-only build before any locked constant was written, so no downstream number was affected).

## Issues Encountered
None beyond the two auto-fixed compile-time API mismatches above.

## User Setup Required
None — no external service configuration required.

## Next Phase Readiness
- TOOL-16's gate exists before the first version bump: the executed-test count (541/0) is locked from a live pre-upgrade run, and the guard has been directly observed failing on a deliberately missing test class and passing again once removed.
- `21-TOOLCHAIN-LOG.md` is started and ready for every later plan (06+) to append its own step entry (measured count before/after, gate command output) as the phase's running record.
- The fixed gate command `./gradlew :library:test --rerun` is now documented as the mandatory check after every subsequent dependency/toolchain bump — any drift in the executed count will fail the build immediately rather than silently passing (the exact Pitfall 11 risk this requirement exists to close).
- No blockers identified for downstream plans (06-14). Plan 06 (the first actual version bump) can proceed directly against this locked baseline.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 2 files (1 created, 1 modified) found on disk; both task commits (`ffece58`, `a42e2b2`) found in git history.
