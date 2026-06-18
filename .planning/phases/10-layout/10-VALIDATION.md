---
phase: 10
slug: layout
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-18
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | `kotlin.test` (JUnit on JVM) — established, no new setup |
| **Config file** | `library/build.gradle.kts` — `testImplementation(kotlin("test"))` already present |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.layout.*"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~20–40 seconds (pure JVM unit tests, no Compose UI harness) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "com.mordred.aero.components.layout.*"` (plus `./gradlew :library:compileKotlin` for composable tasks)
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~40 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | LAYO-03, LAYO-04 | unit (pure fn) | `./gradlew :library:test --tests "com.mordred.aero.components.layout.SplitClampTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 10-01-02 | 01 | 1 | LAYO-03, LAYO-04 | compile | `./gradlew :library:compileKotlin` | n/a (composable; drag verified Phase 7 SC4) | ⬜ pending |
| 10-02-01 | 02 | 1 | LAYO-02 | unit (pure fn) | `./gradlew :library:test --tests "com.mordred.aero.components.layout.AccordionToggleTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 10-02-02 | 02 | 1 | LAYO-01, LAYO-02 | compile | `./gradlew :library:compileKotlin` | n/a (animation visual) | ⬜ pending |
| 10-03-01 | 03 | 1 | LAYO-05, LAYO-06, LAYO-07 | unit (pure fn) | `./gradlew :library:test --tests "com.mordred.aero.components.layout.SidebarStateTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 10-03-02 | 03 | 1 | LAYO-05, LAYO-06, LAYO-07 | compile | `./gradlew :library:compileKotlin` | n/a (width animation visual) | ⬜ pending |
| 10-04-01 | 04 | 1 | LAYO-09 | unit (pure fn) | `./gradlew :library:test --tests "com.mordred.aero.components.layout.WizardStepTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 10-04-02 | 04 | 1 | LAYO-08, LAYO-09 | compile | `./gradlew :library:compileKotlin` | n/a (indicator visual; verified Phase 7 SC5) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Each plan's Task 1 is its own Wave 0 — it creates the test scaffold AND the pure logic it covers, in the same TDD task (behavior block first). No plan goes 3 consecutive tasks without an automated verify (every plan has exactly 2 tasks; Task 1 is unit-tested, Task 2 is compile-gated).

---

## Wave 0 Requirements

Each plan creates its own pure-logic test in its Task 1 (no separate Wave 0 plan needed — the four components are independent and each has a small pure core):

- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` — LAYO-03 clamp + fraction conversion (plan 01, task 1)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/AccordionToggleTest.kt` — LAYO-02 single/multi toggle (plan 02, task 1)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/SidebarStateTest.kt` — LAYO-05/06 mode→width mapping (plan 03, task 1)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/WizardStepTest.kt` — LAYO-09 validate gate + advance + back (plan 04, task 1)

Test framework already installed (`kotlin.test`, 40+ existing test files). No framework install needed.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Accordion expand/collapse animation smoothness | LAYO-01 | Animation timing is visual | Phase 11 showcase: expand/collapse a section, confirm `animateContentSize` smoothness (no jank) |
| SplitPane drag first-pixel response + cursor | LAYO-04 | Pointer/cursor behavior | Drag verified for the shared `aeroDragSplitter` primitive in Phase 7 SC4; re-confirm in Phase 11 showcase |
| Sidebar width transition + collapsed tooltip | LAYO-05, LAYO-06 | Width animation + hover tooltip are visual | Phase 11 showcase: toggle modes, hover collapsed icon (tooltip appears after 600ms) |
| StepIndicator current/completed/upcoming render | LAYO-08 | Visual state distinction | Verified in Phase 7 SC5; re-confirm in Phase 11 showcase across 3 themes |
| Three-theme correctness (AeroBlue/AeroDark/Classic) | all LAYO | Cross-theme contrast is eyes-on | Phase 10 four-component visual review + Phase 11 16-item checklist |

Pure-function behavior (toggle coordination, clamp, validate gate, step transition, width mapping) is fully automated above.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (every plan = 2 tasks, both gated)
- [x] Wave 0 covers all MISSING references (each plan creates its own pure-logic test in Task 1)
- [x] No watch-mode flags
- [x] Feedback latency < 40s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-18
