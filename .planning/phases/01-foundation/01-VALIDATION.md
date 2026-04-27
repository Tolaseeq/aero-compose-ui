---
phase: 1
slug: foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-27
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | kotlin("test") + JUnit 5 (Jupiter) — Wave 0 installs |
| **Config file** | `library/build.gradle.kts` — Wave 0 adds test dependencies |
| **Quick run command** | `./gradlew :library:test` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd:verify-work`:** Full suite must be green + manual showcase visual check
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01 | 01 | 0 | FOUND-02..06 | unit | `./gradlew :library:test --tests "*.AeroColorSchemeTest.*"` | ❌ W0 | ⬜ pending |
| 1-02 | 01 | 0 | FOUND-01 | unit | `./gradlew :library:test --tests "*.AeroThemeTest.*"` | ❌ W0 | ⬜ pending |
| 1-03 | 01 | 0 | FOUND-09 | unit | `./gradlew :library:test --tests "*.AeroTypographyTest.*"` | ❌ W0 | ⬜ pending |
| 1-04 | 01 | 1 | FOUND-02 | unit | `./gradlew :library:test` | ✅ W0 | ⬜ pending |
| 1-05 | 01 | 1 | FOUND-03..05 | unit | `./gradlew :library:test` | ✅ W0 | ⬜ pending |
| 1-06 | 01 | 1 | FOUND-09 | unit | `./gradlew :library:test` | ✅ W0 | ⬜ pending |
| 1-07 | 01 | 1 | FOUND-01 | unit | `./gradlew :library:test` | ✅ W0 | ⬜ pending |
| 1-08 | 01 | 1 | FOUND-07..08 | build smoke | `./gradlew :library:compileKotlin` | implicit | ⬜ pending |
| 1-09 | 01 | 1 | FOUND-10 | build enforcement | `./gradlew :library:compileKotlin` | implicit | ⬜ pending |
| 1-10 | 02 | 2 | SHW-01..03 | build smoke | `./gradlew :showcase:compileKotlin` | implicit | ⬜ pending |
| 1-11 | 02 | 2 | SHW-02 | manual | Visual — run `./gradlew :showcase:run`, toggle themes | manual | ⬜ pending |
| 1-12 | 02 | 2 | FOUND-07 | manual | Layout Inspector overdraw view in showcase | manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt` — stubs covering FOUND-02..06
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeTest.kt` — stub covering FOUND-01
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/AeroTypographyTest.kt` — stub covering FOUND-09
- [ ] Test dependencies added to `library/build.gradle.kts`:
  ```kotlin
  testImplementation(kotlin("test"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  ```

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| glassEffect uses single drawBehind pass (no overdraw) | FOUND-07 | Requires Layout Inspector overdraw view — no automated tool in scope | Run `./gradlew :showcase:run`, open Layout Inspector, enable "Show Overdraw", verify glass demo boxes show no red/dark-red overdraw |
| Theme switcher toggles AeroBlue / AeroDark / Classic instantly | SHW-02 | Requires visual comparison — screenshot diffing not in scope for Phase 1 | Run `./gradlew :showcase:run`, click each theme button, verify background, glass boxes, and text colors change correctly |
| Win11 undecorated crash validation | STATE.md Phase 3 blocker | Runtime crash on native code path — not catchable by JUnit | Add minimal `Window(undecorated = true)` to showcase, run on Windows 11, verify no `EXCEPTION_ACCESS_VIOLATION` crash |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
