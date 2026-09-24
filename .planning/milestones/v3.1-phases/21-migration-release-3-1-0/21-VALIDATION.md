---
phase: 21
slug: migration-release-3-1-0
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-09-23
---

# Phase 21 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit (5.x now → 6.1.3 after TOOL-15) via `kotlin.test`; Compose `runComposeUiTest` for UI tests |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`) |
| **Quick run command** | `./gradlew :library:test --tests "<scoped pattern>"` |
| **Full suite command** | `./gradlew test` (only `:library` has a test source set) |
| **Estimated runtime** | measure at the start of the phase (TOOL-16 lock run) |

---

## Sampling Rate

- **After every task commit:** full `./gradlew test` — the phase's locked rule is "every bump gated by a full test run", stricter than quick-run sampling.
- **After every plan wave:** full `./gradlew test` at the TOOL-16 locked count.
- **Before `/bm:verify-work`:** full suite green at exactly the locked count, guard proven red earlier.
- **Max feedback latency:** one full-suite run.

---

## Per-Task Verification Map

Requirement-level map from RESEARCH.md § Validation Architecture. The planner assigns task IDs; the executor fills Status.

| Task | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|------|-------------|----------|-----------|-------------------|-------------|--------|
| 21-01-T1 | BASE-01 | section / page / capture launch params; default page unchanged | build | `./gradlew :showcase:compileKotlin` + `grep -c 'shows("' ShowcaseApp.kt` = 17 | ❌ W0 | ⬜ pending |
| 21-01-T2 | BASE-02 | helper has no input/activation/screen-scrape API; pixel primitives proven | script self-test | `Test-AeroPixelsSelf` → `AERO_PIXELS_SELFTEST PASS` + banned-API grep = 0 | ❌ W0 | ⬜ pending |
| 21-01-T3 | BASE-02 | PrintWindow capture works covered and minimized; cursor/foreground unchanged | live self-test | `Invoke-ShowcaseSweep.ps1 -SelfTest` → `AERO_SELFTEST PASS` | ❌ W0 | ⬜ pending |
| 21-02-T1 | BASE-05 | `captureToImage` works in desktop `runComposeUiTest` (first-step proof; stop if not) | Compose UI test | `./gradlew :library:test --rerun --tests "*Base05CaptureProofTest*"` | ❌ W0 | ⬜ pending |
| 21-02-T1 | BASE-05 / D-03 | images written only on opt-in | Compose UI test + fs check | plain run leaves `.captures` file count and `git status` unchanged | ❌ W0 | ⬜ pending |
| 21-02-T2/T3 | BASE-05 | hover/press/focus/drag of 10 components x 3 themes differ from default | Compose UI test | `./gradlew :library:test --rerun --tests "*Base05*"` | ❌ W0 | ⬜ pending |
| 21-03-T1 | BASE-05 / D-07 | popup capture method decided (probe) | Compose UI test | `./gradlew :library:test --rerun --tests "*D07MenuPopupCaptureTest*"` | ❌ W0 | ⬜ pending |
| 21-03-T2/T3 | BASE-05 / D-07 / D-09 | 13 Popup components opened x 3 themes, fixed picker values | Compose UI test | `./gradlew :library:test --rerun --tests "*D07*"` | ❌ W0 | ⬜ pending |
| 21-04-T1 | BASE-03 | comparison / noise tool detects, localises and noise-classifies diffs | script self-test | `Compare-AeroCaptures.ps1 -Mode SelfTest` → `AERO_COMPARE_SELFTEST PASS` | ❌ W0 | ⬜ pending |
| 21-04-T2/T3 | BASE-03 / BASE-04 / BASE-05 | baseline frames + named noise regions on the old toolchain | capture run | manifests exist; `21-noise-regions.json` has `showcase` and `uitest` | ❌ W0 | ⬜ pending |
| 21-05-T1/T2 | TOOL-16 | exact test count locked; guard proven red on an excluded class | build verification | `./gradlew :library:test --rerun` → `AERO_TEST_COUNT total=<locked>`; red run recorded | ❌ W0 | ⬜ pending |
| 21-06-T1 | TOOL-09 | Gradle 9.7.1, stale repo gone, refresh build green | build | `./gradlew --version`; `./gradlew build --refresh-dependencies` | ✅ | ⬜ pending |
| 21-06-T2 | TOOL-10 | class-file 65, module metadata `org.gradle.jvm.version = 21` | build verification | `javap -v` major 65; grep `module.json` | ❌ W0 | ⬜ pending |
| 21-06-T3 | TOOL-11 / TOOL-12 / TOOL-17 | Kotlin 2.4.20 + CMP 1.12.0 green at locked count; Material3 1.9.0 both modules; showcase launches | build verification | `bash tools/verify/check-material3.sh` → `MATERIAL3 OK`; gate; driver smoke | ❌ W0 | ⬜ pending |
| 21-07-T2 | HRM-01 / TOOL-12 | `:library` POM / module metadata unchanged after Hot Reload; Material3 clean; stdio clean | build verification | POM + filtered module.json diff empty; `check-material3.sh`; JSON-only stdout | ❌ W0 | ⬜ pending |
| 21-08-T1/T2 | HRM-02 / HRM-03 | MCP sees chosen section/theme; cursor/foreground unchanged in 27 measurements | agent-measured | `Test-NonInterference.ps1` before/after each MCP call | ❌ W0 | ⬜ pending |
| 21-09-T2 | TOOL-14 | picker / calendar behaviour unchanged after kotlinx-datetime 0.8.0 | unit | `./gradlew :library:test --rerun --tests "com.mordred.aero.components.pickers.*" --tests "*AeroCalendarGridTest*"` + `./gradlew :showcase:compileKotlin` | ✅ existing | ⬜ pending |
| 21-09-T3 | TOOL-15 | jupiter + platform launcher aligned on 6.1.3 | build verification | `dependencyInsight --configuration testRuntimeClasspath` | ✅ | ⬜ pending |
| 21-10-T1/T2 | VER-07 / VER-08 | new-toolchain frames compared with baseline; agent reviewed | capture + compare | `Compare-AeroCaptures.ps1 -Mode Compare` outputs cover every baseline key | ❌ W0 | ⬜ pending |
| 21-11-T2 | VER-09 / D-06 | lists complete; offline page resolves every image | script check | `New-HandoffPage.ps1` → `AERO_HANDOFF OK` | ❌ W0 | ⬜ pending |
| 21-13-T1 | REL-03 / TOOL-10 | verify tag green on JitPack, SHA match, published major 65 | remote | JitPack build API `status: ok` | — | ⬜ pending |
| 21-13-T2 | REL-04 | README floor + stale lines fixed | grep | `grep -c "2.1.21" README.md` = 0 etc. | ✅ | ⬜ pending |
| 21-14-T2 | REL-05 | `3.1.0` in tagged commit, JitPack ok, coordinate resolves (after confirmation) | remote | JitPack API + POM HTTP 200 | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] BASE-05 proof-of-work test (`captureToImage` in desktop `runComposeUiTest`) — first file written; stop and ask if it fails
- [ ] Opened-popup capture probe (D-07 first task) — decides UI test vs. the D-08 per-component fallback
- [ ] TOOL-16 test-count guard task
- [ ] Scripted Material3 `dependencyInsight` check for both modules
- [ ] `:library` POM / module-metadata before/after snapshot for HRM-01

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| MCP server connects and sees the showcase | HRM-01..03 | needs the maintainer to restart Claude Code after `.mcp.json` lands | maintainer restarts Claude Code; agent calls the MCP tree dump and finds showcase nodes |
| capture / tree dump / click leave cursor and foreground window unchanged | HRM-03 | measured by the agent on the live desktop | agent records cursor position + foreground HWND before/after each call, incl. covered / minimized showcase |
| post-upgrade frames vs. baseline, drift list | VER-07..10 | pixel diff is an analysis tool, not a gate (locked) | agent's full sweep → one D-04 stop with the D-06 HTML page |
| JitPack verify tag green, `3.1.0` resolves | REL-03..05 | remote build; `3.1.0` / `v3.1.0` need maintainer confirmation | push `v3.1.0-verifyNN`, read JitPack build log; after confirmation push `v3.1.0` and resolve the coordinate |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (BASE-05 proof → 21-02 T1; popup probe → 21-03 T1; TOOL-16 guard → 21-05 before the first bump in 21-06; `check-material3.sh` → created in 21-06 T3 before its use in 21-07 / 21-09; HRM-01 POM snapshot → 21-07 T1 before T2)
- [x] No watch-mode flags
- [x] Feedback latency ≤ one full-suite run (local test sampling; JitPack polling in 21-13 / 21-14 is a remote-build wait, not test feedback)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-23 (plan-checker iteration 2)
