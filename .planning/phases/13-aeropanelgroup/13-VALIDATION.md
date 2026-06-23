---
phase: 13
slug: aeropanelgroup
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-23
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | kotlin.test (JUnit) — existing, no setup needed (`testImplementation(kotlin("test"))` already present) |
| **Config file** | `library/build.gradle.kts` |
| **Quick run command** | `./gradlew :library:test --tests "*.PanelGroupLogicTest"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30 seconds (pure-logic tests; no Compose runtime) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "*.PanelGroupLogicTest"`
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 13-pure-01 | logic | — | PNL-04 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.distributePxWindowResizePreservesRatios"` | ❌ W0 | ⬜ pending |
| 13-pure-02 | logic | — | PNL-02, PNL-03 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.shareTransferOnCollapseConservesPx"` | ❌ W0 | ⬜ pending |
| 13-pure-03 | logic | — | PNL-10 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.clampPanelDividerPxInvertedRangeNoThrow"` | ❌ W0 | ⬜ pending |
| 13-pure-04 | logic | — | PNL-06 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.activeDividerCountEECEGivesOne"` | ❌ W0 | ⬜ pending |
| 13-pure-05 | logic | — | PNL-03 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.restoreAfterShrinkDoesNotExceedAvailable"` | ❌ W0 | ⬜ pending |
| 13-pure-06 | logic | — | PNL-15 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest.allCollapsedAvailableIsZero"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

> Plan/Wave columns are filled by the planner — the pure-logic suite is the TDD foundation (build step 2) and gates all Compose layout/drag/animation work that follows.

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/PanelGroupLogicTest.kt` — RED-first stubs covering PNL-02, PNL-03, PNL-04, PNL-06, PNL-10, PNL-15 and pitfalls PNL-PITFALL-02/04/05/06/07
- [ ] Framework: none needed — `kotlin.test` already configured (template files: `SplitClampTest.kt`, `AccordionToggleTest.kt`)

*No new test infrastructure — existing kotlin.test setup covers all pure-logic phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `animateFloatAsState` target + direct drag write coexist on one size variable without snap-back/oscillation | PNL-07 (spike gate) | Compose animation runtime — no pure-logic equivalent; this is the build step 1 SPIKE gate | Run showcase; collapse a section then immediately drag an adjacent divider — observe no snap-back, no oscillation. PNL-PITFALL-01 resolved. |
| Three-theme visual sign-off (glassPanel header, CaretRight 0°→90°, grip dots, leadingIcon/headerActions slots) | PNL-14, PNL-05, success criterion 4 | Visual aesthetic judgment against Win7 Aero — not machine-checkable | Run showcase LayoutSection on AeroBlue / AeroDark / Classic; confirm aero gloss/gradient/depth consistent with AeroSidebar/AeroAccordion. |
| Window-resize during active drag produces no proportion reset or snap-back | PNL-04, success criterion 3 | Requires live window resize + drag interaction | Drag a divider, resize window mid-drag; section proportions re-derive from new totalPx with no reset. |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
