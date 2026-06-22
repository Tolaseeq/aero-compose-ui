---
phase: 12
slug: v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-22
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | kotlin-test + JUnit Jupiter 5 |
| **Config file** | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.layout.SplitClampTest"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30 seconds (unit only; pure-logic tests, no Compose UI harness) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test`
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-xx | 01 | 1 | FIXDT-01 | unit | `./gradlew :library:test --tests "*.AeroDateTimePickerTest"` | ⚠️ extend existing | ⬜ pending |
| 12-01-xx | 01 | 1 | FIXDT-02 | unit | `./gradlew :library:test --tests "*.AeroDateTimePickerTest"` | ⚠️ extend existing | ⬜ pending |
| 12-02-xx | 02 | 1 | FIXSP-04 | unit | `./gradlew :library:test --tests "*.SplitClampTest.clampInvertedRangeDoesNotThrow"` | ❌ W0 | ⬜ pending |
| 12-02-xx | 02 | 1 | FIXSP-01/02/03 | unit | `./gradlew :library:test --tests "*.SplitClampTest"` | ⚠️ extend existing | ⬜ pending |
| 12-03-xx | 03 | 2 | DTR-04 | unit | `./gradlew :library:test --tests "*.AeroDateTimeRangePickerTest"` | ❌ W0 | ⬜ pending |
| 12-03-xx | 03 | 2 | DTR-03 | unit | `./gradlew :library:test --tests "*.AeroDateTimeRangePickerTest"` | ❌ W0 | ⬜ pending |
| 12-04-xx | 04 | 3 | SHW-11..14 | manual | Run `:showcase` app on 3 themes | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · ❌ W0 = file/method created in Wave 0*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` — add `clampInvertedRangeDoesNotThrow` covering FIXSP-04. **Must be written BEFORE applying Fix B** (FIXSP-04 mandates the red test precedes the fix).
- [ ] `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt` — add `formatAeroDateTime` cases (seconds on/off) covering FIXDT-01, plus a case proving an explicit `formatter` overrides the default (FIXDT-02). Extend if the file exists; create if missing.
- [ ] `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePickerTest.kt` — new file covering DTR-03 (Apply-gate single-emit contract via pure logic) and DTR-04 (`orderDateTimeRange` same-day reversed-time swap), plus `nextRangeState`-derived Apply-enable conditions for narrow cases (same-month, single-day).

*Framework already present (kotlin-test + JUnit Jupiter 5 via `useJUnitPlatform()`); no install required.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Seconds visible in `AeroDateTimePicker` trigger when `showSeconds=true` | SHW-12 | Renders Compose trigger text; no UI test harness in scope | Run `:showcase`, open the `showSeconds=true` instance, enter non-zero seconds, Apply → trigger reads `HH:MM:SS` |
| Nested 3-pane SplitPane: outer splitter drags full range, inner holds position, no crash on squeeze | SHW-13 | Pointer-drag interaction; no Compose UI test in scope | Run `:showcase` LayoutSection nested demo, drag outer splitter across range, squeeze inner below min — no snap-back, no exception |
| `AeroDateTimeRangePicker` row with live `(LocalDateTime, LocalDateTime)` label | SHW-11 | Visual + interactive popup | Run `:showcase` PickersSection, pick a range + times, Apply → label updates |
| All demos correct on AeroBlue / AeroDark / Classic | SHW-11/12/13 | Visual theming verification | Switch theme selector, re-check each demo on all three themes |
| PROJECT.md kotlinx-datetime note corrected | SHW-14 | Doc edit, not runtime | Grep PROJECT.md Key Decisions — stale "Revisit on publish" note gone, factual `api(libs.kotlinx.datetime)` record present |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
