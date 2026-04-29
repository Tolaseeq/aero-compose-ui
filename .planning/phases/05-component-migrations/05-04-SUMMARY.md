---
phase: 05-component-migrations
plan: 04
subsystem: test
tags: [test-rewrite, icons, migration, wave-4, gate]
dependency_graph:
  requires: [05-01, 05-02, 05-03]
  provides: [wave-5-gate-open, CLN-01]
  affects: [AeroAlertKindTest, AeroBannerKindTest]
tech_stack:
  added: []
  patterns: [explicit-internal-imports, aeroicons-facade-pattern]
key_files:
  created: []
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt
decisions:
  - "Explicit internal.* imports required in test files (same as production enums) — AeroIcons extension properties not visible via facade alone"
metrics:
  duration: 5min
  completed: "2026-04-29"
  tasks_completed: 3
  files_modified: 2
requirements_closed: [CLN-01]
---

# Phase 05 Plan 04: Wave 4 Test Rewrites — AeroAlertKindTest + AeroBannerKindTest Summary

Wave 4 gate plan: rewrote both enum-mapping test files to assert `AeroIcons.*` instances instead of `Icons.Outlined.*`, opening the Wave 5 dep-removal gate with full `:library:test` green.

## What Was Built

- `AeroAlertKindTest.kt` — 4 icon-mapping assertions rewritten to `assertEquals(AeroIcons.{Info|Warning|XCircle|Question}, ...)`, all `androidx.compose.material.icons` imports removed
- `AeroBannerKindTest.kt` — 4 icon-mapping assertions rewritten to `assertEquals(AeroIcons.{Info|Warning|XCircle|CheckCircle}, ...)`, all `androidx.compose.material.icons` imports removed
- `definesExactlyFourKinds()` count tests in both files: unchanged

## Gate Verification Results

| Check | Result |
|-------|--------|
| `grep -rn "Icons.Outlined" library/src/test/` | 0 hits |
| `grep -rn "androidx.compose.material.icons" library/src/test/` | 0 hits |
| `./gradlew :library:test --tests "*AeroAlertKindTest"` | EXIT 0 |
| `./gradlew :library:test --tests "*AeroBannerKindTest"` | EXIT 0 |
| `./gradlew :library:test` (full suite) | EXIT 0 |

**Wave 5 gate: OPEN** — dep removal (Plan 05-05) can proceed without test compilation failures.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Explicit `internal.*` imports required in test files**

- **Found during:** Task 1 (first compile attempt after writing AeroAlertKindTest.kt)
- **Issue:** `AeroIcons.Info`, `AeroIcons.Warning`, `AeroIcons.XCircle`, `AeroIcons.Question` were "Unresolved reference" in test compilation — the `AeroIcons` facade alone does not expose the extension properties without explicit `internal.*` imports, same as in production enum files
- **Fix:** Added `import com.mordred.aero.icons.internal.Info` etc. to both test files, matching the pattern used in AeroAlertKind.kt and AeroBannerKind.kt (established in 05-01)
- **Files modified:** Both test files
- **Pattern note:** This is the recurring deviation documented across Plans 05-01/02/03; expected for all files using AeroIcons extension properties

## Commits

| Commit | Message | Files |
|--------|---------|-------|
| `163784a` | `test(05-04-CLN-01a): rewrite AeroAlertKindTest to assert AeroIcons.* instances` | AeroAlertKindTest.kt |
| `c4d19de` | `test(05-04-CLN-01b): rewrite AeroBannerKindTest to assert AeroIcons.* instances` | AeroBannerKindTest.kt |

## Self-Check: PASSED
