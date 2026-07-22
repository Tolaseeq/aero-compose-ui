---
phase: 15
slug: toolchain-upgrade
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-22
---

# Phase 15 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) via `useJUnitPlatform()`, `kotlin.test` assertions; Compose `compose.uiTest` (`runComposeUiTest`, `@OptIn(ExperimentalTestApi::class)`) for the one UI-level test |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`) — no separate JUnit config file |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.layout.AeroPanelGroupRecomposeUiTest"` |
| **Full suite command** | `./gradlew :library:test` (232 tests, incl. 12 `PanelGroupLogicTest`) |
| **Estimated runtime** | ~120 seconds (full suite, cold-ish daemon) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:compileKotlin` (after version-bump/pin changes) or `./gradlew :library:test --tests "*AeroPanelGroupRecomposeUiTest*"` (after test-port/re-proof work)
- **After every plan wave:** Run `./gradlew :library:test` (full 232-test suite)
- **Before `/gsd:verify-work`:** Full suite green + showcase three-theme smoke + JitPack build green
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 15-01-xx | 01 | 1 | TOOL-06 | manual (baseline capture) | `./gradlew :showcase:run` (BEFORE migration) | N/A | ⬜ pending |
| 15-02-xx | 02 | 2 | TOOL-01 | build/compile | `./gradlew :library:compileKotlin` | N/A | ⬜ pending |
| 15-02-xx | 02 | 2 | TOOL-02 | build/dependency-tree | `./gradlew :library:dependencies --configuration compileClasspath \| grep material3` | N/A | ⬜ pending |
| 15-03-xx | 03 | 3 | TOOL-03/04 | UI/manual-assisted | `./gradlew :library:test --tests "*AeroPanelGroupRecomposeUiTest*"` (run twice: DSL fix reverted, then restored) | ✅ | ⬜ pending |
| 15-04-xx | 04 | 4 | TOOL-05 | unit + UI | `./gradlew :library:test` | ✅ | ⬜ pending |
| 15-04-xx | 04 | 4 | TOOL-06 | manual (baseline diff) | `./gradlew :showcase:run` (compare vs. before) | N/A | ⬜ pending |
| 15-05-xx | 05 | 4 | TOOL-07 | build/compile | `./gradlew :showcase:compileKotlin` | ❌ W0 | ⬜ pending |
| 15-06-xx | 06 | 5 | TOOL-08 | external CI | `git tag v3.0.0-alpha01 && git push origin v3.0.0-alpha01` → check JitPack log | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Task IDs are placeholders (`xx`) — planner assigns exact IDs. Wave/plan mapping is indicative; final waves are set in the PLAN.md frontmatter.*

---

## Wave 0 Requirements

- [ ] Scratch composable file for `dropShadow`/`innerShadow` proof (TOOL-07) — does not exist yet; this phase creates and commits it (placement is Claude's discretion per CONTEXT.md)
- [ ] Before-migration baseline screenshots (three themes: AeroBlue / AeroDark / Classic) — must be captured **before** the version bump runs, or the "before" state is lost

*The RCMP test file, the full 232-test suite, and the JitPack config all already exist — port/verify targets, not from-scratch builds.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Three-theme showcase visual parity vs. pre-migration baseline | TOOL-06 | No Compose Desktop screenshot-regression framework exists (both flagship frameworks are Android-only; pixel-diffing Skia output is flaky across machines/GPUs) | Capture showcase screenshots on AeroBlue/AeroDark/Classic BEFORE the bump; after migration, re-capture and human-compare for rendering drift (Skia m126→m138→m144) |
| RCMP guard asserts exactly 1 header per section post-drag | TOOL-04 | The `Unconfined`→`Standard` dispatcher-default change is exactly the timing class that produced this project's prior false-positive sign-off; "still green" alone is insufficient | Run the re-proof: revert the non-`@Composable` DSL fix → confirm test FAILS with header duplication (N>1) → restore fix → confirm PASS; human-inspect the assertion, not just pass/fail |
| JitPack build on the new toolchain | TOOL-08 | External CI service, triggered by tag push — not a local test | Push `v3.0.0-alpha01` pre-release tag; check JitPack build log for the new artifact coordinate `com.github.Tolaseeq:aero-compose-ui:v3.0.0-alpha01` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (scratch composable, baseline screenshots)
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
