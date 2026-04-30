---
phase: 7
slug: shared-internal-primitives
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-30
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + kotlin-test + kotlinx-coroutines-test (already wired in `library/build.gradle.kts:21-26`) |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`) |
| **Quick run command** | `./gradlew :library:test --tests "<TestClass>"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30s (full suite — JVM unit tests only; no UI/Compose runtime tests in Phase 7) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "<TestClass>"` for the test class touched by the commit
- **After every plan wave:** Run `./gradlew :library:test` (full suite)
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds (full suite)

---

## Per-Task Verification Map

> Filled by the planner — every task that produces code MUST have an automated test entry. Tasks with no automated verification require Wave 0 stubs or manual sign-off below.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 7-01-01 | 01 | 1 | (enabling) | unit | `./gradlew :library:test --tests "AeroColorMathTest"` | ⬜ pending | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/internal/color/AeroColorMathTest.kt` — stubs for HSV→RGB→HSV round-trip (PITFALL-15)
- [ ] `library/src/test/kotlin/com/mordred/aero/internal/calendar/AeroCalendarGridLogicTest.kt` — stubs for current month / month boundary / leap year (3 scenarios)
- [ ] `library/src/test/kotlin/com/mordred/aero/internal/calendar/AeroCalendarPositionProviderTest.kt` — stubs for popup-wider-than-anchor positioning at 1024dp
- [ ] `kotlinx-datetime` 0.6.2 added to `gradle/libs.versions.toml` if absent

*Note: Drag-related smoke tests (`AeroDragSplitter`, `AeroHsvColorSquare`, `AeroHueSlider` — SC3 + SC4) are **manual-only** in this phase — no Compose UI test framework currently wired. See Manual-Only Verifications below. Wave 0 stubs cover only the JVM-unit-testable success criteria (SC1 logic + SC2).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `AeroDragSplitter` fires `onDrag` on first mouse pixel (horizontal) | SC4 | No Compose UI test framework wired in Phase 7; adding it is out of scope (per CONTEXT.md scope freeze) | Run Phase7ScratchSection in showcase app → drag horizontal splitter 1px → confirm callback fires; record screenshot |
| `AeroDragSplitter` fires `onDrag` on first mouse pixel (vertical) | SC4 | Same | Run Phase7ScratchSection → drag vertical splitter 1px → confirm callback fires |
| `AeroHsvColorSquare` responds to drag on first pixel | SC3 | Same | Run Phase7ScratchSection → drag inside HSV square 1px → confirm S/V update |
| `AeroHueSlider` responds to drag on first pixel | SC3 | Same | Run Phase7ScratchSection → drag hue thumb 1px → confirm hue updates |
| `AeroStepIndicator` renders correctly across all 3 themes (Aero, Classic, Win11-tinted) | SC5 | No visual snapshot framework wired | Run Phase7ScratchSection → toggle theme → confirm dot colors / connectors / current/completed/upcoming states distinguishable per theme |
| `AeroCalendarPositionProvider` does not clip popup wider than anchor on 1024dp window | SC1 (popup half) | Visual positioning verified by eye; logic-level test exists for math | Run Phase7ScratchSection → resize window to ~1024dp → open calendar over a 100dp-wide anchor → confirm 280dp+ calendar popup is fully visible |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
