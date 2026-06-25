---
phase: 14
slug: panelgroup-recompose-fix-v2-0-3-release
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-25
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter (JUnit 5) via `useJUnitPlatform()` |
| **Config file** | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.layout.PanelGroupLogicTest"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30 seconds (quick), ~60s (full) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "*.PanelGroupLogicTest"`
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd:verify-work`:** Full suite must be green + manual visual sign-off complete
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 14-01-* | 01 | 1 | RCMP-02 | static (grep) | `grep -n "expandedState.toBooleanArray\|expandedArr" library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` | ✅ exists | ⬜ pending |
| 14-01-* | 01 | 1 | RCMP-03 | static (grep) | `grep -n "expandedState\[" library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` (only SideEffect + seed sites) | ✅ exists | ⬜ pending |
| 14-01-* | 01 | 1 | REG-02 | unit | `./gradlew :library:test --tests "*.PanelGroupLogicTest"` (12 GREEN) | ✅ exists | ⬜ pending |
| 14-01-* | 01 | 1 | RCMP-01 | manual visual | drag divider w/ live counter ticking; N sections → exactly N strips | N/A | ⬜ pending |
| 14-01-* | 01 | 1 | REG-01 | manual visual | vertical/uncontrolled byte-identical; `onLayoutChange` not per-frame | N/A | ⬜ pending |
| 14-02-* | 02 | 2 | RCMP-04 | manual visual | repro demo present + reproduces×N before / clean after | ❌ W0: add repro block | ⬜ pending |
| 14-03-* | 03 | 3 | REL-01 | static (grep) | `grep 'version = ' build.gradle.kts` shows `2.0.3` | N/A | ⬜ pending |
| 14-03-* | 03 | 3 | REL-02 | manual | JitPack build log resolves `com.github.Tolaseeq:aero-compose-ui:2.0.3` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*
*Plan/Wave/Task IDs are indicative — final IDs assigned by the planner; the verification mapping per requirement is authoritative.*

---

## Wave 0 Requirements

- [ ] No new test files needed — the fix is pure-logic changes to a composable; the existing 12 JVM tests cover `PanelDistribution.kt`, which is unchanged.
- [ ] RCMP-04 repro block in `LayoutSection.kt` — added as part of the fix plan (a permanent labeled showcase demo, not a test file).

*Existing infrastructure covers all automatically-testable phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| N sections render as exactly N header strips under recompose-during-drag | RCMP-01 | No Compose UI / instrumented test infra (mirrors v2.0.1 / Phase 13 precedent) | Open showcase → LayoutSection horizontal controlled PanelGroup; with the live counter ticking, drag the divider; confirm 3 sections render as exactly 3 header strips (not ×N) |
| Repro demo reproduces ×N before fix, clean after | RCMP-04 | Visual repro, no UI test infra | Same demo; before fix witness ×N duplication on drag; after fix confirm clean N |
| Vertical + uncontrolled byte-identical; `onLayoutChange` not per-frame | REG-01 | Visual/behavioral equivalence, no UI test infra | Drag vertical + uncontrolled divider with counter ticking; confirm drag-resize, collapse/expand animations (`snap()` during drag, `tween(200ms, FastOutSlowInEasing)` after), window-resize proportion preservation unchanged; confirm `onLayoutChange` fires on drag-end + toggle only (counter/log), never per frame |
| JitPack resolves the v2.0.3 artifact | REL-02 | External service build, no local automation | Open JitPack build log for `Tolaseeq/aero-compose-ui` tag `v2.0.3`; confirm `com.github.Tolaseeq:aero-compose-ui:2.0.3` resolves |

*Static-inspection requirements (RCMP-02, RCMP-03, REL-01) are grep-verifiable — see Per-Task Verification Map.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify (unit/static) or are listed as manual-only with instructions
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify (static grep + JVM tests cover the fix tasks)
- [ ] Wave 0 covers all MISSING references (RCMP-04 repro block)
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
