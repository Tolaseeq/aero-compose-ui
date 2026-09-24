---
phase: 21-migration-release-3-1-0
plan: 06
subsystem: build
tags: [gradle, kotlin, compose-multiplatform, jdk21, material3, toolchain-migration, jitpack]

# Dependency graph
requires:
  - phase: 21-05
    provides: "TOOL-16 test-count guard (lockedTestTotal=541/lockedTestSkipped=0) in library/build.gradle.kts and the fixed gate command `./gradlew :library:test --rerun` every bump in this plan re-proves against"
provides:
  - "Gradle wrapper 9.7.1 with a pinned distributionSha256Sum; stale maven.pkg.jetbrains.space repository removed from settings.gradle.kts"
  - "JDK 21 toolchain in :library and :showcase (jvmToolchain(21)); jitpack.yml on openjdk21 with a live-fetched SDKMAN Temurin identifier; published jar bytecode verified class-file major 65, module.json org.gradle.jvm.version=21"
  - "Kotlin 2.4.20 + Compose Multiplatform 1.12.0 in gradle/libs.versions.toml, built and tested at the locked 541/0 count with zero mechanical fixes needed"
  - "tools/verify/check-material3.sh — reusable dependencyInsight-based Material3 gate across both modules' compile/runtime classpaths plus the full showcase dependency graph; confirms 1.9.0 with no alpha anywhere"
  - "TOOL-17 showcase smoke proof on the new toolchain under .captures/smoke/plan06-cmp1.12 (gitignored, not committed) — manifest + rendered Buttons section frame"
  - "21-TOOLCHAIN-LOG.md Steps 3a/3b/3c — full record of checksums, javap/module.json output, warnings (verbatim), and gate lines for all three bumps"
affects: [21-07, 21-08, 21-09, 21-10, 21-11, 21-12, 21-13, 21-14]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Each toolchain bump (Gradle, JDK, Kotlin+CMP) is isolated into its own commit, gated by ./gradlew build (+ --refresh-dependencies on the first) then ./gradlew :library:test --rerun at the locked 541/0 count before the next bump starts — same isolation lesson as v3.0 Phase 15"
    - "tools/verify/check-material3.sh: dependencyInsight-per-configuration + full :showcase:dependencies grep-for-alpha gate, reusable verbatim by Plans 07 (Hot Reload) and 09 for the same Material3-pin regression risk"
    - "SDKMAN JDK identifiers for jitpack.yml are fetched live from the candidates API and picked from the returned list, never guessed — a wrong/hallucinated identifier would permanently taint a JitPack build tag"

key-files:
  created:
    - tools/verify/check-material3.sh
  modified:
    - settings.gradle.kts
    - gradle/wrapper/gradle-wrapper.properties
    - gradle/wrapper/gradle-wrapper.jar
    - gradlew
    - gradlew.bat
    - library/build.gradle.kts
    - showcase/build.gradle.kts
    - jitpack.yml
    - gradle/libs.versions.toml
    - .planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md

key-decisions:
  - "Precondition-proved the stale maven.pkg.jetbrains.space repository was genuinely unused before removing it: a --refresh-dependencies build on the still-untouched Gradle 8.14.3 wrapper passed BUILD SUCCESSFUL with both lines already deleted from settings.gradle.kts"
  - "Gradle 9.7.1's distributionSha256Sum was fetched directly from services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256, not copied from any document"
  - "jitpack.yml's Temurin identifier (21.0.12+1.1-tem) was the highest 21.0.*-tem entry from a live SDKMAN candidates-API call, not guessed, per the package-legitimacy-style rule that a wrong identifier permanently taints a JitPack tag"
  - "Kotlin 2.4.20 + Compose Multiplatform 1.12.0 built and passed the full suite on the first attempt with zero mechanical fixes — no CMP 1.12/Kotlin 2.4.20 API removal or rename touched this codebase, and the uiTest StandardTestDispatcher default change (research Pitfall 3) did not surface any test failure or hang"
  - "Left the new CMP-1.12 LocalClipboardManager deprecation warning in showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt unfixed and recorded only — that file is outside this task's declared file list (scope boundary), and it is a non-blocking deprecation, not a compile error"

requirements-completed: [TOOL-09, TOOL-10, TOOL-11, TOOL-12, TOOL-17]

# Metrics
duration: ~50min active work (bb5708d 18:59 -> a324d05 19:08 for the three gated commits; additional time spent diagnosing/reporting an external file lock and waiting on the maintainer/orchestrator to resolve it before Task 1 could be gated)
completed: 2026-09-23
---

# Phase 21 Plan 06: Gradle 9.7.1 -> JDK 21 -> Kotlin 2.4.20 + CMP 1.12.0 Summary

**Three isolated, individually-gated commits moved the whole toolchain from Gradle 8.14.3/JDK 17/Kotlin 2.4.10/CMP 1.11.1 to Gradle 9.7.1/JDK 21/Kotlin 2.4.20/CMP 1.12.0 with zero code changes and zero mechanical API fixes — every gate (`./gradlew build` then `./gradlew :library:test --rerun`) passed at the TOOL-16-locked 541/0 test count on the first attempt for each bump, and a new `tools/verify/check-material3.sh` gate confirms Material3 stayed pinned at 1.9.0 with no alpha leak anywhere.**

## Performance

- **Duration:** ~9 min of gated build/test work across the three commits (`bb5708d` 18:59:00 -> `a324d05` 19:08:11); a separate, non-productive pause occurred earlier in the session while an external VS Code Kotlin Language Server process held a file lock on `library/build/libs/library-3.0.0.jar` — resolved by the maintainer/orchestrator ending that process, not by this executor (see Deviations)
- **Started:** 2026-09-23 (previous plan's last commit `0e51cdc`)
- **Completed:** 2026-09-23 (last commit `a324d05`)
- **Tasks:** 3/3 completed
- **Files modified/created:** 11 (10 modified, 1 created), across 3 commits

## Accomplishments
- **Step 3a (TOOL-09):** Removed both `maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")` lines from `settings.gradle.kts`, proved the removal was safe on the untouched 8.14.3 wrapper first, then bumped the Gradle wrapper to 9.7.1 with a `distributionSha256Sum` fetched from `services.gradle.org`. `./gradlew --version` confirmed `Gradle 9.7.1` with a 21.0.9 launcher JVM; the gate printed `AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false`.
- **Step 3b (TOOL-10):** `jvmToolchain(17)` -> `jvmToolchain(21)` in both `library/build.gradle.kts` and `showcase/build.gradle.kts`; `jitpack.yml` moved to `openjdk21` with a live-fetched SDKMAN Temurin identifier (`21.0.12+1.1-tem`). Bytecode-inspected the published jar directly: `javap -v` on `com.mordred.aero.components.buttons.AeroButtonKt` from `library-3.0.0.jar` reports `major version: 65` (Java 21); `module.json` contains `"org.gradle.jvm.version": 21` twice (both published variants).
- **Step 3c (TOOL-11/12/17):** Bumped `kotlin` to `2.4.20` and `composeMultiplatform` to `1.12.0` in the version catalog. The full build and test suite passed on the very first attempt — no removed API, no rename, no `uiTest` dispatcher-related test failure or hang. Wrote `tools/verify/check-material3.sh`, which ran clean (`MATERIAL3 OK`) across all four `:library`/`:showcase` × compile/runtime `dependencyInsight` calls plus the full `:showcase:dependencies` graph. Ran the TOOL-17 showcase smoke via `Invoke-ShowcaseSweep.ps1` into a fresh `.captures/smoke/plan06-cmp1.12` folder — `AERO_SWEEP_DONE`, manifest records `composeMultiplatform: 1.12.0` / `jvm: 21.0.9/Microsoft`, and the captured PNG (read directly) shows the Buttons section rendering correctly in AeroBlue.
- `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` now has full Step 3a/3b/3c entries: checksums, `--version`/`javap`/`module.json` output, every build warning verbatim (or "zero" where a clean rebuild printed none), and each gate's `AERO_TEST_COUNT` line.
- Confirmed the pre-upgrade baseline under `.captures/old-kt2.4.10-cmp1.11.1/` is untouched (still 1152 files) throughout this plan, per the hard rule.

## Task Commits

Each task was committed atomically:

1. **Task 1: Drop the stale JetBrains Space repository and move to Gradle 9.7.1 (TOOL-09)** - `bb5708d` (build)
2. **Task 2: JDK 21 everywhere — toolchains and jitpack.yml; class-file 65 + module metadata (TOOL-10)** - `11d1135` (build)
3. **Task 3: Kotlin 2.4.20 + Compose Multiplatform 1.12.0, Material3 gate, showcase launch (TOOL-11, TOOL-12, TOOL-17)** - `a324d05` (build)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `settings.gradle.kts` - both stale JetBrains Space repository lines removed
- `gradle/wrapper/gradle-wrapper.properties` / `gradle-wrapper.jar` / `gradlew` / `gradlew.bat` - Gradle 9.7.1 with pinned `distributionSha256Sum`
- `library/build.gradle.kts` / `showcase/build.gradle.kts` - `jvmToolchain(17)` -> `jvmToolchain(21)`
- `jitpack.yml` - `openjdk21`, live-fetched Temurin `21.0.12+1.1-tem`
- `gradle/libs.versions.toml` - `kotlin = "2.4.20"`, `composeMultiplatform = "1.12.0"`
- `tools/verify/check-material3.sh` (new) - Material3 dependencyInsight gate, reusable by Plans 07/09
- `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` - Step 3a/3b/3c entries appended

## Decisions Made
See frontmatter `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

None — all three version bumps built and passed the full test suite on the first attempt; no mechanical API fixes, no `resolutionStrategy.force`, no test-timing changes were needed anywhere in this plan.

### External Blocker (not a Rule 1-4 deviation — resolved outside this executor)

**File lock on `library/build/libs/library-3.0.0.jar` during Task 1**
- **Found during:** Task 1, first `./gradlew build --refresh-dependencies` attempt on the bumped 9.7.1 wrapper
- **Issue:** `:library:jar` failed with `Unable to delete file 'C:\1A_WORK\ui_lib\library\build\libs\library-3.0.0.jar'`. Diagnosed (read-only, via Win32 `DeleteFile` returning `ERROR_SHARING_VIOLATION` and the Restart Manager API `RmGetList`) as an external VS Code Kotlin Language Server process (`fwcd.kotlin`, PID 30592) holding the jar open as part of its own classpath indexing.
- **Action taken:** Per the global stop rule and the explicit sandbox denial on touching that process (Claude Code's "Interfere With Workloads" classifier), this executor did not attempt to kill, inspect further, or work around the lock. Execution paused and reported the exact locked path and error back to the orchestrator.
- **Resolution:** The maintainer authorized ending the process; the orchestrator ended it and confirmed the file was no longer locked. Execution then resumed and the identical gate command succeeded unmodified — no build or code file needed any change for this.
- **Files modified:** none (no code or config change was needed to work around this — it was purely an external process holding an OS-level file handle)

---

**Total deviations:** 0 auto-fixed. 1 external environment blocker, handled entirely via stop-and-ask/orchestrator escalation per the global stop rule, with no code or config impact.
**Impact on plan:** None on scope or content — the blocker was resolved externally and the same planned commands then succeeded verbatim.

## Issues Encountered
- The second `./gradlew wrapper --gradle-version 9.7.1 ...` invocation (the one executing on 9.7.1 itself, downloading the distribution zip) hit one transient `SocketTimeoutException` against `services.gradle.org` (`networkTimeout=10000` in the properties file); an identical retry succeeded. No config change made — this is standard network flakiness, not a build defect.
- See External Blocker above for the file-lock pause during Task 1's gate.

## User Setup Required
None — no external service configuration required. (The file-lock resolution above required a one-time maintainer authorization to end an unrelated local process, already handled before this summary was written.)

## Next Phase Readiness
- Toolchain now sits at Gradle 9.7.1 / JDK 21 / Kotlin 2.4.20 / Compose Multiplatform 1.12.0 / Material3 1.9.0 (unchanged), with the full 541/0 test suite green at every step and `tools/verify/check-material3.sh` available for Plan 07 (Hot Reload) and Plan 09 to reuse verbatim against the same Material3-pin regression risk (Pitfall 2).
- `jitpack.yml` is ready for a throwaway verify tag against JDK 21/Gradle 9.7.1 (REL-03, later plan).
- No blockers identified for Plan 07 (Compose Hot Reload + MCP server), which the CONTEXT.md decisions note requires CMP >= 1.12.0 — now satisfied.
- The `.captures/old-kt2.4.10-cmp1.11.1/` pre-upgrade baseline remains untouched (1152 files) and the new `.captures/smoke/plan06-cmp1.12/` folder (gitignored, not committed) is available as an additional early on-new-toolchain reference point alongside the later full VER-07/VER-08 sweep.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 9 checked files found on disk (`tools/verify/check-material3.sh`, `settings.gradle.kts`,
`gradle/wrapper/gradle-wrapper.properties`, `library/build.gradle.kts`, `showcase/build.gradle.kts`,
`jitpack.yml`, `gradle/libs.versions.toml`, `21-TOOLCHAIN-LOG.md`, this SUMMARY); all 3 task commits
(`bb5708d`, `11d1135`, `a324d05`) found in git history.
