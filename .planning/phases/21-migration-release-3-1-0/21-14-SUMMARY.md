---
phase: 21-migration-release-3-1-0
plan: 14
subsystem: release
tags: [release, jitpack, tag, rel-05]

# Dependency graph
requires:
  - phase: 21-13
    provides: "green JitPack verify build v3.1.0-verify01 on JDK 21 / Gradle 9.7.1, README consumer floor"
provides:
  - "aero-compose-ui 3.1.0 published on JitPack as com.github.Tolaseeq:aero-compose-ui:v3.1.0 (tag only; origin master unchanged)"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - build.gradle.kts

key-decisions:
  - "Maintainer chose `release-tag-only` (as in v3.0): only the v3.1.0 tag was pushed; origin master stays at bbe3658"
  - "Published coordinate is the v-prefixed `com.github.Tolaseeq:aero-compose-ui:v3.1.0` (maintainer's choice); JitPack does not resolve the v-less `3.1.0` (API status `none`), which matches the verify-tag pre-check"

requirements-completed: [REL-05]

# Metrics
duration: about 15 min after the maintainer's choice
completed: 2026-09-24
---

# Phase 21 Plan 14: Release 3.1.0 Summary

**aero-compose-ui 3.1.0 is published: `version = "3.1.0"` was committed, tag `v3.1.0` pushed alone, and JitPack built it green on Java 21 / Gradle 9.7.1. The coordinate `com.github.Tolaseeq:aero-compose-ui:v3.1.0` serves the POM (HTTP 200).**

## Performance

- **Duration:** about 15 min
- **Completed:** 2026-09-24T17:07 local
- **Tasks:** 2/2
- **Files modified:** 1

## Task 1: maintainer's choice (verbatim)

Asked through two questions (option, coordinate). Answers:
- Option: «release-tag-only (Как в v3.0)»
- Coordinate: «…:v3.1.0 (Рекомендуется)»

Preconditions confirmed before asking: tree clean apart from the known untracked items, `build.gradle.kts` `version = "3.0.0"`, no `v3.1.0` tag locally or on origin.

## Task 2: release

- Gate: `AERO_TEST_COUNT total=541 skipped=0 expected=541`, `BUILD SUCCESSFUL in 48s`.
- `build.gradle.kts`: `version = "3.0.0"` -> `"3.1.0"` (only that line), commit `8d3b227` `chore(release): 3.1.0`.
- `git tag -a v3.1.0 -m "aero-compose-ui 3.1.0"` on `8d3b227`; `git show v3.1.0:build.gradle.kts` has `version = "3.1.0"`.
- `git push origin v3.1.0` → `* [new tag] v3.1.0 -> v3.1.0` (tag object `78d87d1`). Origin master before and after: `bbe3658cba7a25643a51f5632d88f71636976346`, unchanged.
- JitPack API: `{"version":"v3.1.0","status":"ok","commit":"8d3b227d079d975b982983fbae52c5bb43e7f365","isTag":true}`; commit equals `git rev-parse v3.1.0^{commit}`.
- Build log: `Gradle 9.7.1`, `Launcher JVM: 21.0.2`, `BUILD SUCCESSFUL in 45s`. Published: `aero-compose-ui-v3.1.0.jar`, `-sources.jar`, `.module`, `.pom`.
- Resolution: `.../com/github/Tolaseeq/aero-compose-ui/v3.1.0/aero-compose-ui-v3.1.0.pom` → HTTP 200. The v-less API `.../aero-compose-ui/3.1.0` → `status: none`, so the README's `:v3.1.0` is the coordinate.
- Tags on origin afterwards: `v3.0.0`, `v3.0.0-alpha01`, `v3.0.0-verify01`, `v3.0.0-verify02`, `v3.1.0-verify01`, `v3.1.0`. Nothing deleted or re-pointed, no force push.

## Task Commits

1. **Release version** - `8d3b227` (chore), tag `v3.1.0`

## Deviations from Plan

- Run inline by the orchestrator (a short, outward-facing step, with the maintainer at hand). The choice was asked as two structured questions instead of a free-text resume signal; the answers name the option and the coordinate explicitly.

**Total deviations:** 1, no scope change.

## Issues Encountered

- Earlier in the phase (Plan 13), `git push` used a Git Credential Manager entry for a different GitHub account. At the maintainer's explicit request the stored github.com credential was switched to `Tolaseeq`; this push used it without further changes.

## Next Phase Readiness

Phase 21 plans complete. Phase verification and milestone close remain.

## Self-Check: PASSED

- `grep 'version = "3.1.0"' build.gradle.kts` matches; `git rev-parse -q --verify refs/tags/v3.1.0` resolves to the tag
- JitPack `v3.1.0` status `ok`, commit matches; POM HTTP 200
- `.captures/old-kt2.4.10-cmp1.11.1` untouched (1152 files)
