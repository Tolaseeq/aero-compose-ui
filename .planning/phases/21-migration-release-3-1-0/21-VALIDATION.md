---
phase: 21
slug: migration-release-3-1-0
status: draft
nyquist_compliant: false
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

| Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|-------------|----------|-----------|-------------------|-------------|--------|
| BASE-05 | `captureToImage` works in desktop `runComposeUiTest` (first-step proof) | Compose UI test | `./gradlew :library:test --tests "*Base05*"` | ❌ W0 | ⬜ pending |
| BASE-05 / D-07 | state + opened-popup captures written only on explicit opt-in (D-03) | Compose UI test | `./gradlew test` leaves `git status` clean and `.captures/` untouched | ❌ W0 | ⬜ pending |
| TOOL-16 | exact test count locked; guard proven red on an excluded class | build verification | `./gradlew test` (guard task) + one temporary-exclude run that must fail | ❌ W0 | ⬜ pending |
| TOOL-12 | Material3 resolves to 1.9.0 on both modules after every bump incl. Hot Reload | build verification | `./gradlew :library:dependencyInsight --dependency material3 --configuration runtimeClasspath` and the same for `:showcase` | ❌ W0 | ⬜ pending |
| TOOL-14 | picker / calendar behavior unchanged after kotlinx-datetime 0.8.0 renames | unit | `./gradlew :library:test --tests "com.mordred.aero.components.pickers.*" --tests "*AeroCalendarGridTest*"` + `./gradlew :showcase:compileKotlin` | ✅ existing | ⬜ pending |
| TOOL-10 | published bytecode is class-file 65, module metadata `org.gradle.jvm.version = 21` | build verification | `javap -v` on a class from the published jar; grep `org.gradle.jvm.version` in `.module` | ❌ W0 | ⬜ pending |
| TOOL-17 | showcase builds and runs on the new toolchain | build + launch | `./gradlew :showcase:compileKotlin` + launch with `-Paero.scheme` / `-Paero.section`, window found by the BASE-02 helper | ❌ W0 | ⬜ pending |
| HRM-01 | `:library` POM / module metadata unchanged after Hot Reload is added | build verification | `git diff --stat -- library/build.gradle.kts` empty + before/after diff of `generatePomFileForMavenPublication` / `publishToMavenLocal` output | ❌ W0 | ⬜ pending |

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

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < one full-suite run
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
