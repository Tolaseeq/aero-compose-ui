---
phase: 05-component-migrations
plan: "05"
subsystem: ui
tags: [gradle, dependency-removal, jar-size, CLN-02, CLN-03, wave-5]

# Dependency graph
requires:
  - phase: 05-component-migrations
    provides: "Wave 4 test rewrites (CLN-01) — no material.icons references in library/src/test/"
provides:
  - "library/build.gradle.kts without compose.materialIconsExtended (CLN-02)"
  - "CLN-03 verified: zero androidx.compose.material.icons references across library/src/"
  - "JAR-size delta documented: 37768805 bytes (~36.02 MB) removed from compileClasspath"
  - "Phase 5 closure: 05-SUMMARY.md written"
affects: [phase-06-showcase, STATE.md, ROADMAP.md]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Thin library JAR does not embed transitive deps; dependency size delta is classpath-level"

key-files:
  created:
    - .planning/phases/05-component-migrations/05-05-SUMMARY.md
    - .planning/phases/05-component-migrations/05-SUMMARY.md
  modified:
    - library/build.gradle.kts
    - .planning/STATE.md

key-decisions:
  - "Library JAR (1007523 bytes) is a thin compile-output JAR; the meaningful size removal is materialIconsExtended off the compileClasspath (~36 MB dep)"
  - "Post-removal library JAR is byte-identical to pre-removal: confirms Wave 5 only modifies the classpath, not the library's compiled classes"

patterns-established:
  - "Wave 5 dep-removal gate: Wave 4 test green + 5-grep CLN-03 suite + compileKotlin = safe removal"

requirements-completed: [CLN-02, CLN-03]

# Metrics
duration: 15min
completed: 2026-04-29
---

# Phase 05 Plan 05: Wave 5 Dep Removal — materialIconsExtended Removed, Phase 5 Closed

**`compose.materialIconsExtended` removed from `library/build.gradle.kts`; all 5 CLN-03 grep sweeps return 0 hits; `material-icons-extended-desktop-1.7.3.jar` (~36 MB) eliminated from `:library` compileClasspath**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-29T10:24:37Z
- **Completed:** 2026-04-29
- **Tasks:** 4 (Task 1 + 3: measurement/verification; Task 2: dep removal; Task 4: docs)
- **Files modified:** 2 (library/build.gradle.kts, .planning/STATE.md) + 2 created (summaries)

## Accomplishments

- Single-line deletion of `implementation(compose.materialIconsExtended)` from `library/build.gradle.kts`
- `./gradlew :library:compileKotlin` exits 0 without the dep — no missed migration sites
- `./gradlew :library:test` exits 0 after dep removal — full test suite green
- All 5 CLN-03 grep sweeps return 0 hits across `library/src/`
- `material-icons-extended-desktop-1.7.3.jar` (~36.02 MB) no longer on `:library` compileClasspath

## JAR Size Delta

| Metric | Value |
|--------|-------|
| Pre-removal library JAR (post-Wave-4) | 1,007,523 bytes (~0.96 MB) |
| Post-removal library JAR (post-CLN-02) | 1,007,523 bytes (~0.96 MB) |
| Library JAR delta | 0 bytes — expected (thin JAR, no embedded deps) |
| `material-icons-extended-desktop` dep removed from classpath | 37,768,805 bytes (~36.02 MB) |
| ROADMAP expected delta | ~6–8 MB |
| Within expected range? | No — actual is ~36 MB (larger than estimate); delta is classpath-level, not thin-JAR-level. The ROADMAP estimate assumed a fat JAR or classpath-embedded measurement. The actual dep JAR is 36 MB. The library's own thin JAR does not embed dependencies. |

**Note on measurement methodology:** The plan calls for `./gradlew :library:jar` size measurement. The `:library:jar` task produces a thin JAR of library classes only — it does not embed transitive dependencies. The library thin JAR is unchanged at 1,007,523 bytes because Wave 5 removes a classpath dependency, not library source files. The meaningful delta is that `material-icons-extended-desktop-1.7.3.jar` (37,768,805 bytes / ~36.02 MB) is no longer required on the `:library` compileClasspath (verified by `./gradlew :library:dependencies --configuration compileClasspath | grep materialIcons` returning 0 hits).

## CLN-03 Grep Suite Results

| Grep | Command | Result |
|------|---------|--------|
| 1 - Material Icons imports | `grep -rn "androidx.compose.material.icons" library/src/` | 0 hits |
| 2 - Text glyph sweep | `grep -rn 'Text("▲"|"▼"|"▶"|"✕"|"✓"|"─"…)' library/src/` | 0 hits |
| 3 - Text("x") clear button | `grep -rn 'Text("x"' library/src/` | 0 hits |
| 4 - Icons.Outlined (defensive) | `grep -rn 'Icons.Outlined' library/src/` | 0 hits |
| 5 - Canvas fn names | `grep -rn 'SearchIcon\|EyeOpenIcon\|EyeClosedIcon' library/src/main/` | 0 hits |

## Task Commits

1. **Task 1: JAR-size baseline** - no commit (measurement task)
2. **Task 2: Remove materialIconsExtended (CLN-02)** - `7ea31c7` (chore)
3. **Task 3: CLN-03 grep verification** - no commit (pure verification)
4. **Task 4: Phase closure docs** - committed with metadata

## Files Created/Modified

- `library/build.gradle.kts` — removed `implementation(compose.materialIconsExtended)` (line 15)
- `.planning/phases/05-component-migrations/05-05-SUMMARY.md` — this file
- `.planning/phases/05-component-migrations/05-SUMMARY.md` — phase closure summary
- `.planning/STATE.md` — Performance Metrics + Current Position updated

## Decisions Made

- Library thin JAR measurement is correct per plan (`./gradlew :library:jar`). The 0-byte delta in the thin JAR is expected behavior — thin JARs don't embed transitive deps. The actual dep removal effect is the elimination of `material-icons-extended-desktop-1.7.3.jar` from the compileClasspath (verified via dependency tree).

## Deviations from Plan

None — plan executed exactly as written. The JAR-size delta interpretation (thin JAR vs classpath) is documented in the summary rather than a deviation from execution steps.

## Issues Encountered

None.

## Next Phase Readiness

- Phase 5 complete: all 14 requirements (MIG-01..11, CLN-01..03) satisfied
- Phase 6 (Showcase IconsSection) is now unblocked
- No blockers; no open items from Phase 5 scope

## Self-Check: PASSED

- [x] `library/build.gradle.kts` has 0 materialIconsExtended references
- [x] `compose.material3` retained in build.gradle.kts
- [x] `05-05-SUMMARY.md` exists (5986 bytes)
- [x] `05-SUMMARY.md` exists (10251 bytes)
- [x] STATE.md `JAR size:` line present
- [x] STATE.md `stopped_at` references Phase 5 complete
- [x] STATE.md `completed_phases: 5`
- [x] Commit `7ea31c7` (chore CLN-02) exists
- [x] Commit `9a1817d` (docs closure) exists
- [x] Commit `b6d2fa3` (STATE/ROADMAP/REQUIREMENTS) exists

---
*Phase: 05-component-migrations*
*Completed: 2026-04-29*
