---
phase: 21-migration-release-3-1-0
verified: 2026-09-24T00:00:00Z
status: passed
score: 24/24 must-haves verified
has_blocking_gaps: false
overrides_applied: 0
---

# Phase 21: Migration + Release 3.1.0 Verification Report

**Phase Goal:** Every dependency and toolchain piece sits on its latest stable version (Gradle 9.7.1, JDK 21, Kotlin 2.4.20, Compose Multiplatform 1.12.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0 plain, JUnit 6.1.3; Material3 stays pinned at stable 1.9.0), it is demonstrated against a pre-upgrade baseline that nothing broke, and `3.1.0` is published on JitPack. Compose Hot Reload + MCP is installed in `:showcase` as the tool the agent uses to inspect the GUI itself.
**Verified:** 2026-09-24 (re-checked independently against the live repository, not just SUMMARY claims)
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (mapped to ROADMAP Phase 21 success criteria + requirement IDs)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Pre-upgrade baseline (reference frames, all 17 sections x 3 themes x pages, UI-test images) exists outside `build/`, taken on the old toolchain with a non-interfering capture helper, every noisy region named (BASE-01..05) | VERIFIED | `.captures/old-kt2.4.10-cmp1.11.1/` has 1152 files, live-recounted (unchanged since capture). `21-BASELINE.md` records toolchain `2.4.10`/CMP `1.11.1`, git sha, DPI. `21-NOISE.md` + `21-noise-regions.json` name every noise region. `tools/capture/AeroCapture.ps1` uses `PrintWindow(hwnd,hdc,2)`; `Test-NonInterference.ps1` self-tests cursor/foreground. |
| 2 | Every target dependency/toolchain version landed, each bump its own commit, full suite green at the exactly-locked test count (guard proven red first); no Material3 alpha on either module including after Hot Reload; picker behavior/tests unchanged; showcase builds/runs; published bytecode class-file 65 with `org.gradle.jvm.version=21` (TOOL-09..17) | VERIFIED | Live re-run: `AERO_TEST_COUNT total=541 skipped=0 expected=541` (matches locked count). `21-TOOLCHAIN-LOG.md` documents the guard failing red at `total=534` with excluded class, then green. `git log` shows 7 separate toolchain-bump commits (`bb5708d`, `11d1135`, `a324d05`, `bb8c716`, `883a8d3`, `19dbce3`, `1b9e847`, `65e9617`). `gradle/libs.versions.toml`: kotlin=2.4.20, composeMultiplatform=1.12.0, kotlinxCoroutines=1.11.0, kotlinxDatetime=0.8.0, junit=6.1.3. `gradle-wrapper.properties`: Gradle 9.7.1 with pinned SHA256. Live `dependencyInsight` on `:library` and `:showcase` (runtime classpath) resolves only `material3:1.9.0`/`material3-desktop:1.9.0`, no alpha. Live jar bytecode check: `javap -v` → `major version: 65`; `module.json` → `"org.gradle.jvm.version": 21` (count 2). |
| 3 | Hot Reload + MCP sits in `:showcase` only (`:library` POM/module metadata unchanged), connects after restart, sees the showcase; agent measured cursor/foreground stay unchanged across capture/tree-dump/click including covered/minimized; `take_screenshot` never used (HRM-01..03) | VERIFIED | `showcase/build.gradle.kts` applies `alias(libs.plugins.compose.hot.reload)`; `library/build.gradle.kts` has zero "hot" references; live `library` POM/module contain no hot-reload artifact. `.mcp.json` runs `:showcase:hotMcpServer` (qualified task, fixing an initial dual-JVM bug documented in `21-HRM.md`). `21-HRM.md` records `status`/`list_windows`/`get_semantic_tree` working post-restart, and a 27/27 non-interference matrix (bottom/covered/minimized) with cursor and foreground unchanged; `take_screenshot` never called. |
| 4 | Post-upgrade frames/UI-test images compared with baseline; every outside-noise difference explained/fixed; unconfirmed items listed separately; maintainer sees GUI only after agent's own sweep (VER-07..10) | VERIFIED | `21-COMPARE.md` (482 lines): re-captured 75 showcase keys + 227 UI-test keys on new toolchain, compared with baseline. `21-DRIFT.md`: 102 items in 8 causal groups, every one carries before/after/highlight paths and a named cause; maintainer's ruling («всё принимаем») recorded verbatim per item; re-compare after rulings shows `withOutside` dropping 75→66 (9 keys moved inside noise). `21-UNCONFIRMED.md` lists hover of ~40 other components, popups extending past window frame, 125%/200% scale (100%/96 DPI explicitly named as what WAS tested), non-semantic behavior, `AeroFilePicker`. `21-12-SUMMARY.md`: maintainer replied `+` (approved) after the agent's sweep was complete, in `21-HANDOFF`/index.html. |
| 5 | Throwaway verify tag green on JitPack under JDK21/Gradle 9.7.1 before the real tag; README states new consumer floor and fixes the stale toolchain line; `3.1.0` in `build.gradle.kts`, `v3.1.0` pushed, JitPack build `ok`, coordinate resolves (REL-03..05) | VERIFIED | Live: `git rev-parse v3.1.0^{commit}` = `8d3b227...`, matches `git ls-remote --tags origin v3.1.0` (`78d87d1` tag object). Live JitPack API for `v3.1.0`: `{"status":"ok","commit":"8d3b227...","isTag":true}` — commit SHA matches. `build.gradle.kts` line 4: `version = "3.1.0"`. README (live grep): package line states Kotlin 2.4.20/CMP 1.12.0/JVM21; dependency snippet `v3.1.0`; consumer-floor paragraph names Java 21/CMP 1.12/Kotlin 2.4.20/kotlinx-datetime 0.8 and fallback to v3.0.0/v2.0.4; `git show d209221` confirms the old `2.1.21`/`1.7.3`/JVM17 line was replaced. `origin/master` unchanged at `bbe3658...` (release-tag-only, maintainer's documented choice). Note: published coordinate is v-prefixed `com.github.Tolaseeq:aero-compose-ui:v3.1.0` (the v-less form returns `status:none` on JitPack) — this is the maintainer's recorded choice in `21-14-SUMMARY.md`, not a gap. |

**Score:** 5/5 roadmap success criteria verified; 24/24 requirement IDs (BASE-01..05, TOOL-09..17, HRM-01..03, VER-07..10, REL-03..05) accounted for and evidenced.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.7.1 pinned checksum | VERIFIED | `distributionUrl=...gradle-9.7.1-bin.zip`, `distributionSha256Sum` present, matches fetched hash per `21-TOOLCHAIN-LOG.md` |
| `library/build.gradle.kts`, `showcase/build.gradle.kts` | `jvmToolchain(21)` | VERIFIED | Both files contain `jvmToolchain(21)` |
| `gradle/libs.versions.toml` | Kotlin 2.4.20, CMP 1.12.0, coroutines 1.11.0, datetime 0.8.0, JUnit 6.1.3, Hot Reload 1.2.0 | VERIFIED | Live grep confirms all 6 values |
| `jitpack.yml` | JDK 21 | VERIFIED | `jdk: [openjdk21]` (per 21-06/21-13 logs); build ran on JDK 21 per live JitPack build log evidence in `21-13-SUMMARY.md` |
| `.mcp.json` | `:showcase:hotMcpServer` via `cmd /c .\gradlew.bat` | VERIFIED | File content matches exactly |
| `library/build.gradle.kts` | No Hot Reload plugin/dependency | VERIFIED | `grep -in hot` returns nothing in this file |
| `library/src/test/kotlin/com/mordred/aero/capture/*.kt` | BASE-05/D-07 UI-capture test suite | VERIFIED | `Base05CaptureProofTest.kt`, `Base05GlassStateCaptureTest.kt`, `Base05DragCaptureTest.kt`, `D07MenuPopupCaptureTest.kt`, `D07PickerPopupCaptureTest.kt`, `UiCapture.kt` all present, contribute to the 541 tests confirmed executing live |
| `tools/capture/*.ps1`, `tools/verify/check-material3.sh` | Capture/compare/handoff tooling | VERIFIED | All 6 scripts exist on disk |
| `.planning/phases/.../21-BASELINE.md, 21-NOISE.md, 21-noise-regions.json, 21-COMPARE.md, 21-DRIFT.md, 21-UNCONFIRMED.md, 21-HRM.md, 21-TOOLCHAIN-LOG.md, 21-UITEST-COVERAGE.md` | Phase documentation trail | VERIFIED | All present and read; contents cross-checked against live repo state above |
| `README.md` | v3.1.0 consumer floor, corrected toolchain line | VERIFIED | Live grep + `git show d209221` diff |
| `build.gradle.kts` | `version = "3.1.0"` | VERIFIED | Live grep, line 4 |
| `.captures/old-kt2.4.10-cmp1.11.1/` | Untouched baseline, 1152 files | VERIFIED | Live `find | wc -l` = 1152 |
| `.captures/new-kt2.4.20-cmp1.12.0/`, `.captures/diff/`, `.captures/diff-r12/`, `.captures/handoff/index.html` | Post-upgrade captures + comparison + handoff page | VERIFIED | All present; 1542 files under new-toolchain captures directory |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `library/build.gradle.kts tasks.test` | Gradle `TestListener` root suite | `addTestListener` + `doLast` enforcement | WIRED | Live run reproduces `AERO_TEST_COUNT total=541 skipped=0 expected=541`; `21-TOOLCHAIN-LOG.md` documents the guard failing on a deliberately excluded class (red proof) |
| `.mcp.json` | `:showcase hotMcpServer` task | `cmd /c .\gradlew.bat ... :showcase:hotMcpServer` (stdio) | WIRED | Content matches; `21-HRM.md` documents an initial mis-wiring (unqualified task name causing two JVMs) that was found and fixed (`883a8d3`) before the maintainer's restart, then verified connected |
| `git tag v3.1.0` | JitPack API `.../aero-compose-ui/v3.1.0` | JitPack tag build | WIRED | Live curl: `status: ok`, `commit` equals `git rev-parse v3.1.0^{commit}` exactly |
| `tools/capture/Compare-AeroCaptures.ps1 -Mode Compare` | `21-noise-regions.json` | `-NoiseJson` parameter | WIRED | `21-COMPARE.md`/`21-DRIFT.md` document actual comparison runs producing `keys=75`/`keys=200` totals consistent with the 102-item drift list |
| `21-DRIFT.md` Ruling column | Maintainer's D-04 decision | Recorded verbatim reply «всё принимаем» | WIRED | Every one of 102 rows carries the ruling; re-compare after rulings shows the expected count change (75→66 showcase withOutside) |

### Requirements Coverage

All 24 requirement IDs declared for Phase 21 in ROADMAP.md are also declared, collectively, across the 14 plans' frontmatter (`requirements:` fields), with no gaps and no orphans:

| Requirement | Source Plan(s) | Status | Evidence |
|---|---|---|---|
| BASE-01, BASE-02 | 21-01 | SATISFIED | Launch params, PrintWindow helper — files present, referenced by later plans' successful use |
| BASE-05 | 21-02, 21-03, 21-04 | SATISFIED | Capture test suite present, contributes to 541-test count |
| BASE-03, BASE-04 | 21-04 | SATISFIED | `21-BASELINE.md`, `21-NOISE.md`, `21-noise-regions.json` present with the required content |
| TOOL-16 | 21-05, 21-09 | SATISFIED | Live re-run reproduces locked count; red-run proof documented |
| TOOL-09, TOOL-10, TOOL-11, TOOL-12, TOOL-17 | 21-06, 21-07, 21-09, 21-13 | SATISFIED | Live version/toolchain checks above |
| HRM-01 | 21-07 | SATISFIED | Isolation confirmed live (no hot-reload trace in `:library`) |
| HRM-02, HRM-03 | 21-08 | SATISFIED | `21-HRM.md` connection + non-interference evidence |
| TOOL-13, TOOL-14, TOOL-15 | 21-09 | SATISFIED | Catalog versions confirmed live |
| VER-07, VER-08 | 21-10, 21-11, 21-12 | SATISFIED | `21-COMPARE.md`, `21-DRIFT.md` |
| VER-09 | 21-11 | SATISFIED | `21-UNCONFIRMED.md`, includes the required display-scale statement |
| VER-10 | 21-11, 21-12 | SATISFIED | `21-12-SUMMARY.md` maintainer approval («+») after agent's sweep |
| REL-03, REL-04 | 21-13 | SATISFIED | Live JitPack/README checks (verify-tag build itself is not re-checkable live since it's throwaway, but its SHA-matched green build and the resulting README/bytecode state are corroborated by the subsequent real-tag build, which is independently confirmed) |
| REL-05 | 21-14 | SATISFIED | Live tag/JitPack/version checks |

No orphaned requirements found — `.planning/REQUIREMENTS.md`'s "Phase 21" mapping table (24 rows) exactly matches the union of plan-declared requirement IDs.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `tools/capture/Invoke-ShowcaseSweep.ps1` | 370-417 | Page-discovery loop can record an unrequested page-0 frame when `-Pages` excludes 0 | INFO (already documented in `21-REVIEW.md` WR-01) | Capture tooling only, not library/showcase code; does not affect the phase goal (toolchain migration, baseline comparison itself used full page sets, not filtered `-Pages`) |
| `library/src/test/kotlin/com/mordred/aero/capture/Base05GlassStateCaptureTest.kt` | 565-572 | `catch (e: Throwable)` in focus-fallback helper is overly broad | INFO (already documented in `21-REVIEW.md` WR-02) | Could theoretically mask a genuine crash as "fallback unavailable"; does not affect current green run (541/541 passing, confirmed live) |
| `README.md` | License section | `_TBD._` placeholder | INFO | Pre-existing before Phase 21 (confirmed via `git show d209221` diff — this line was not touched by this phase), out of scope |

No `TBD`/`FIXME`/`XXX` debt markers were introduced by Phase 21's own commits. The two WARNING-level findings above were already surfaced by the phase's own code review (`21-REVIEW.md`) and are scoped to capture tooling and a test helper, not to the library's public API, the toolchain bumps, or the release — they do not block the phase goal.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Locked test count still holds on current HEAD | `./gradlew :library:test --rerun --console=plain` | `AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false`, `BUILD SUCCESSFUL in 1m 21s` | PASS |
| Material3 pinned, no alpha (`:library`, `:showcase`, runtime classpath) | `./gradlew :library:dependencyInsight --dependency material3 --configuration runtimeClasspath` / same for `:showcase` | Only `material3:1.9.0` / `material3-desktop:1.9.0` resolved, no alpha string | PASS |
| Published bytecode is Java 21 | `jar tf` + `javap -v` on freshly built `library-3.1.0.jar` | `major version: 65` | PASS |
| Module metadata declares JVM 21 | `grep -c '"org.gradle.jvm.version": 21' library/build/publications/maven/module.json` | `2` | PASS |
| Tag `v3.1.0` resolves on origin and locally, SHA matches JitPack | `git rev-parse v3.1.0^{commit}`, `git ls-remote --tags origin v3.1.0`, `curl jitpack.io/api/builds/.../v3.1.0` | commit `8d3b227...` matches everywhere; JitPack `status: ok` | PASS |
| `.captures/old-kt2.4.10-cmp1.11.1/` baseline untouched | `find ... | wc -l` | 1152 files (unchanged from every SUMMARY's self-check) | PASS |

### Probe Execution

No `scripts/*/tests/probe-*.sh` convention or explicitly declared probes exist in this phase's plans/summaries — the phase's own equivalent (the capture/compare tooling and the Gradle test-count guard) was exercised directly under Behavioral Spot-Checks above.

Step 7c: SKIPPED (no declared probe scripts; phase uses Gradle test gates and PowerShell capture/compare tooling instead, both independently re-run above)

### Human Verification Required

None. Per this phase's own design (VER-10 / maintainer preference — "no process inflation"), the human-facing GUI inspection was already completed by the maintainer during Plan 12's hand-off (`21-12-SUMMARY.md`: maintainer reply `+`, recorded as approval, after the agent's own sweep with `21-DRIFT.md` and `21-UNCONFIRMED.md` in hand). Re-litigating that sign-off is not required by verification rules for this phase, and this verifier was explicitly instructed not to launch the showcase or use MCP/GUI tooling. All remaining truths for this phase are file-system, git, Gradle-build and public-API (JitPack) facts, all independently re-checked live above.

### Gaps Summary

No gaps found. Every ROADMAP Phase 21 success criterion and every one of the 24 requirement IDs (BASE-01..05, TOOL-09..17, HRM-01..03, VER-07..10, REL-03..05) is backed by both the phase's own SUMMARY/log documentation AND an independent, live re-check performed during this verification (test run, dependencyInsight, bytecode inspection, git tag/JitPack API, README diff, file-count baselines). The two INFO-level findings (a PowerShell page-discovery edge case and an overly broad `catch (Throwable)` in a test helper) were already surfaced by the phase's own code review, are scoped to tooling/tests rather than the library or the release, and do not block goal achievement. The v-prefixed JitPack coordinate (`v3.1.0` rather than a bare `3.1.0`) is a recorded, deliberate maintainer decision, not a deviation.

---

_Verified: 2026-09-24_
_Verifier: Claude (gsd-verifier)_
