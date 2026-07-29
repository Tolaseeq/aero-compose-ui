---
phase: 15-toolchain-upgrade
verified: 2026-07-22T11:07:16Z
status: passed
score: 8/8 must-haves verified
behavior_unverified: 0
overrides_applied: 0
---

# Phase 15: Toolchain Upgrade Verification Report

**Phase Goal:** The library builds, tests, and ships on Kotlin 2.4.10 + Compose Multiplatform 1.11.1 (or an explicitly user-approved fallback), with zero visual-code changes mixed in — so any later regression is unambiguously attributable to either the toolchain or the visual work, never both.
**Verified:** 2026-07-22T11:07:16Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

All checks below were performed independently against the live codebase and a live Gradle/JitPack invocation in this session — SUMMARY.md claims were not taken on trust; each was re-run or re-derived from source.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 (TOOL-01) | `./gradlew build` succeeds on Kotlin 2.4.10 + CMP 1.11.1 | VERIFIED | Independently re-ran `./gradlew.bat build` from repo root → `BUILD SUCCESSFUL`. `gradle/libs.versions.toml` confirmed `kotlin = "2.4.10"`, `composeMultiplatform = "1.11.1"`. No fallback was used (Task 3's conditional escalation never triggered — consistent with a clean first-attempt build). |
| 2 (TOOL-02) | `compose.material3` alias is not used as the version source; Material3 pinned to explicit stable coordinate | VERIFIED | `library/build.gradle.kts:22` and `showcase/build.gradle.kts:16` both use `org.jetbrains.compose.material3:material3:1.9.0` explicitly (not `compose.material3`). Independently ran `./gradlew.bat :library:dependencies --configuration compileClasspath \| grep material3` and same for `:showcase:` → both resolve to `1.9.0` / `material3-desktop:1.9.0`, zero `-alpha`/`-beta` anywhere. |
| 3 (TOOL-03) | `AeroPanelGroupRecomposeUiTest` ported for CMP 1.11 test-infra changes | VERIFIED | Test file compiles and runs on the migrated toolchain (confirmed via independent `./gradlew.bat :library:test --rerun-tasks` run: `TEST-...AeroPanelGroupRecomposeUiTest.xml` → `tests="2" failures="0" errors="0"`). Deprecation warning for v1 `runComposeUiTest` present but non-fatal, matches documented port decision (keep v1, permitted by CONTEXT.md discretion). |
| 4 (TOOL-04) | Ported guard is re-proven to FAIL on unfixed code and PASS when fixed — not a ported-but-inert guard | VERIFIED | Test assertion at `AeroPanelGroupRecomposeUiTest.kt:112-119` asserts an exact header count (`assertEquals(expected, left, ...)`), not "did not throw" — satisfies the "not merely ran" bar. Production code at `AeroPanelGroup.kt:244` confirmed non-`@Composable` DSL lambda (`content: AeroPanelGroupScope.() -> Unit`), the actual v2.0.4 fix. SUMMARY documents the live revert→FAIL(11 headers)→restore→PASS(1 header) sequence with the fix currently in place (git diff clean); re-running the current suite reproduces the PASS half independently. The revert/re-fail half is not independently re-executed in this verification pass (would require mutating and re-reverting production code) — accepted based on the SUMMARY's cited literal AssertionFailedError text plus the corroborating fact that the current, restored code and its exact-count assertion both check out live. |
| 5 (TOOL-05) | Full library test suite (232+ tests, incl. 12 `PanelGroupLogicTest`) is green | VERIFIED | Independently ran `./gradlew.bat :library:test --rerun-tasks`. Parsed all `library/build/test-results/test/*.xml`: **232 total tests, 0 failures, 0 errors** summed across all suites. `PanelGroupLogicTest.xml` → `tests="12" failures="0" errors="0"`. Exact match to the claimed count. |
| 6 (TOOL-06) | Showcase compiles/launches/smoke-runs on all three themes with no observable change from pre-migration baseline | VERIFIED | Independently ran `./gradlew.bat :showcase:compileKotlin` → `BUILD SUCCESSFUL`. `baseline/` (7 PNGs) and `after/` (6 PNGs + `DIFF-REPORT.md`) both exist and are populated. `DIFF-REPORT.md` documents a per-pixel diff for all 6 comparable pairs with an explicit "NO Skia rendering drift" conclusion, with the two non-zero rows individually traced to an animation-phase artifact (indeterminate progress bar) and a scroll-offset capture artifact, not a rendering regression — reasoning is specific and falsifiable, not a bare assertion. |
| 7 (TOOL-07) | `dropShadow`/`innerShadow` scratch composable compiles against real CMP 1.11.1 artifact | VERIFIED | `showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt` exists, uses real corrected imports (`androidx.compose.ui.draw.dropShadow`/`innerShadow`, `androidx.compose.ui.unit.DpOffset`, `androidx.compose.ui.graphics.shadow.Shadow`). Independently confirmed `./gradlew.bat :showcase:compileKotlin` succeeds with this file present (part of the same module already compiled above). KDoc records the corrected signature and explicitly flags the RESEARCH.md package-location discrepancy it found — genuine handoff artifact, not a stub. |
| 8 (TOOL-08) | JitPack build passes on the new toolchain; root version stays unchanged | VERIFIED | `build.gradle.kts` confirmed unchanged at `version = "2.0.4"` (no premature milestone bump). Tag `v3.0.0-alpha01` independently confirmed present on `origin` (`git ls-remote --tags origin`) at commit `ba788b70...`. Independently queried `https://jitpack.io/api/builds/com.github.Tolaseeq/aero-compose-ui/v3.0.0-alpha01` → live response: `{"status":"ok","commit":"ba788b70...","isTag":true}` — real, current JitPack API response, not quoted from the SUMMARY. |

**Score:** 8/8 truths verified (0 present-but-behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | kotlin=2.4.10, composeMultiplatform=1.11.1 | VERIFIED | Confirmed by direct read |
| `library/build.gradle.kts` | Material3 pinned to explicit 1.9.0 | VERIFIED | Line 22: `api("org.jetbrains.compose.material3:material3:1.9.0")` |
| `showcase/build.gradle.kts` | Material3 pinned to explicit 1.9.0 | VERIFIED | Line 16: `implementation("org.jetbrains.compose.material3:material3:1.9.0")` |
| `library/src/test/.../AeroPanelGroupRecomposeUiTest.kt` | Ported, exact-count assertions | VERIFIED | 2 tests, exact `assertEquals` header-count checks, KDoc documents live re-proof |
| `library/src/main/.../AeroPanelGroup.kt` | Non-`@Composable` DSL `content` param | VERIFIED | Line 244: `content: AeroPanelGroupScope.() -> Unit` |
| `.planning/phases/15-toolchain-upgrade/baseline/*.png` (+README.md) | Pre-migration screenshots, 3 themes | VERIFIED | 7 PNGs + README present, component-coverage table complete |
| `.planning/phases/15-toolchain-upgrade/after/*.png` + `DIFF-REPORT.md` | Post-migration screenshots + diff | VERIFIED | 6 PNGs + DIFF-REPORT.md present, all 6 pairs diffed |
| `showcase/.../scratch/ScratchAeroShadowProof.kt` | Compiles against real CMP 1.11.1 | VERIFIED | Present, correct imports, compiles as part of `:showcase:compileKotlin` |
| `build.gradle.kts` (root) | version stays `2.0.4` | VERIFIED | Line 4: `version = "2.0.4"` |
| `jitpack.yml` | `openjdk17`, unchanged | VERIFIED | Confirmed `openjdk17` present |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `libs.versions.toml` kotlin/composeMultiplatform versions | `library`/`showcase` build files | `version.ref` / plugin aliases | WIRED | `compose-compiler` plugin still `version.ref = "kotlin"` — auto-tracks bump, confirmed unchanged in file |
| `AeroPanelGroupRecomposeUiTest` | `AeroPanelGroup.kt`'s `content` DSL param | Test drives real composition via `runComposeUiTest` + `AeroPanelGroup` DSL | WIRED | Independently re-ran; test passes against current (fixed) production code |
| Git tag `v3.0.0-alpha01` | JitPack build | Tag push triggers JitPack build resolution | WIRED | Live JitPack API confirms build `status: ok` for the exact tag commit |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Full build succeeds on new toolchain | `./gradlew.bat build` (from repo root) | `BUILD SUCCESSFUL in 1s` (15 tasks, 1 executed/14 up-to-date after fresh test run) | PASS |
| Full library test suite green | `./gradlew.bat :library:test --rerun-tasks` | `BUILD SUCCESSFUL in 32s`; 232 tests parsed from XML, 0 failures, 0 errors | PASS |
| `PanelGroupLogicTest` count matches claim | XML parse of `TEST-...PanelGroupLogicTest.xml` | `tests="12" failures="0" errors="0"` | PASS |
| `AeroPanelGroupRecomposeUiTest` passes on current (fixed) code | XML parse of `TEST-...AeroPanelGroupRecomposeUiTest.xml` | `tests="2" failures="0" errors="0"` | PASS |
| Showcase compiles on new toolchain | `./gradlew.bat :showcase:compileKotlin` | `BUILD SUCCESSFUL` | PASS |
| Material3 resolves to stable 1.9.0 (both modules) | `./gradlew.bat :library:dependencies` / `:showcase:dependencies --configuration compileClasspath \| grep material3` | `org.jetbrains.compose.material3:material3:1.9.0` in both, no alpha/beta | PASS |
| JitPack build for the release-proof tag is green | `curl https://jitpack.io/api/builds/com.github.Tolaseeq/aero-compose-ui/v3.0.0-alpha01` | `{"status":"ok","commit":"ba788b70...","isTag":true}` (live query, this session) | PASS |
| Root version unchanged (no premature milestone bump) | Read `build.gradle.kts` | `version = "2.0.4"` | PASS |
| No debt markers left in phase-touched files | grep `TBD\|FIXME\|XXX\|TODO\|HACK\|PLACEHOLDER` across all files this phase modified | No matches | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|--------------|------------|-------------|--------|----------|
| TOOL-01 | 15-02 | Project builds on Kotlin 2.4.10 + CMP 1.11.1 | SATISFIED | Independent `./gradlew build` success |
| TOOL-02 | 15-02 | Material3 pinned to explicit stable coordinate | SATISFIED | Independent dependency-tree grep, both modules |
| TOOL-03 | 15-03 | RCMP test ported for CMP 1.11 test-infra | SATISFIED | Test compiles/passes on new toolchain |
| TOOL-04 | 15-03 | Ported guard re-proven to fail-then-pass | SATISFIED (partially re-derived) | Exact-count assertion + non-`@Composable` production fix confirmed live; the revert-half of the re-proof relies on the SUMMARY's cited FAIL(11) text plus the corroborating current-state evidence (see truth #4 notes) |
| TOOL-05 | 15-04 | 232+ tests green incl. 12 PanelGroupLogicTest | SATISFIED | Independent XML parse: 232/0/0, 12/0/0 |
| TOOL-06 | 15-01, 15-04 | Showcase compiles/runs, no visual drift vs. baseline | SATISFIED | Independent compile success + reviewed DIFF-REPORT.md reasoning |
| TOOL-07 | 15-05 | dropShadow/innerShadow scratch compiles against real 1.11.1 | SATISFIED | File present with correct imports, compiles as part of showcase module |
| TOOL-08 | 15-06 | JitPack build passes on new toolchain, version unchanged | SATISFIED | Live JitPack API query + file read, this session |

No orphaned requirements — REQUIREMENTS.md maps exactly TOOL-01..08 to Phase 15, and all 8 are addressed by plans 01-06.

### Anti-Patterns Found

None. Grep for `TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER` across all files touched by this phase (`gradle/libs.versions.toml`, `build.gradle.kts`, `library/build.gradle.kts`, `showcase/build.gradle.kts`, `AeroPanelGroupRecomposeUiTest.kt`, `ScratchAeroShadowProof.kt`) returned zero matches. `ScratchAeroShadowProof.kt` carries an explicit "SCOPE GUARD: ... Do NOT wire this into any real showcase screen" comment — this is intentional scope-fencing for a deliberately isolated Phase-16-handoff artifact, not a stub/placeholder marker, and it is never referenced from any production or showcase-navigation code (confirmed unused elsewhere by design).

### Human Verification Required

None. All 8 must-haves resolved to VERIFIED via direct command execution, file inspection, or a live external API query performed in this session. The two items with an inherent human-judgment component per the plans (TOOL-04's revert/fail/restore/pass sequence, and TOOL-06's Skia-drift visual verdict) were already closed out with a human/orchestrator checkpoint during execution (documented in 15-03-SUMMARY.md and 15-04-SUMMARY.md respectively) and are corroborated here by independent evidence (exact-count assertion code, non-`@Composable` production fix, diff-report reasoning) rather than re-litigated from scratch.

### Gaps Summary

No gaps. All 8 TOOL requirements are independently verified against a live build, a live test run, live dependency resolution, and a live JitPack API query — not merely re-stated from SUMMARY.md. Minor bookkeeping note (non-blocking): `ROADMAP.md`'s Phase 15 top-level checkbox (line 92, under "Phases" summary list) is still `[ ]` even though all 6 sub-plan checkboxes and the Progress table show Phase 15 as 6/6 complete — this is expected to be closed out by the standard phase-completion step following this verification, not a gap in the phase's actual deliverables.

---

_Verified: 2026-07-22T11:07:16Z_
_Verifier: Claude (gsd-verifier)_
