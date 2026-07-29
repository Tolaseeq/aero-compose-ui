---
phase: 20
slug: verification
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-28
---

# Phase 20 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.0`) via `kotlin.test`, `useJUnitPlatform()`; Compose `runComposeUiTest` for measurement/semantics tests |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`); versions pinned in `gradle/libs.versions.toml` |
| **Quick run command** | `./gradlew :library:test --tests "<new test class filter>"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~60 seconds (full suite; 78 test files, 232 tests green as of Phase 15) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "<the class that task authored>"`
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd-verify-work`:** Full suite must be green **and** the mechanical gates + code review must close BEFORE the human three-theme sign-off runs (D-03 ordering)
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

> Task IDs are assigned by the planner; this table is seeded from RESEARCH.md's requirement→test map and must be reconciled against the final PLAN.md task list during execution.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | VER-01 (+VER-06) | — | N/A | unit (source-scan) | `./gradlew :library:test --tests "*VER01*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | VER-02 (+VER-06) | — | N/A | unit (source-scan) | `./gradlew :library:test --tests "*VER02*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | VER-03 (+VER-06) | — | N/A | unit (`runComposeUiTest` + value asserts) | `./gradlew :library:test --tests "*VER03*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | VER-04 | — | N/A | unit (`runComposeUiTest`) | `./gradlew :library:test --tests "AeroButtonSemanticsTest"` | ✅ exists (D-16) | ⬜ pending |
| TBD | TBD | TBD | D-13 (folded contrast fix) | — | N/A | unit (value-level WCAG ratio) | `./gradlew :library:test --tests "*ContrastRegression*"` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | SHW-15 | — | N/A | manual (visual review of showcase section) | n/a | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | SHW-16 | — | N/A | manual-only (human judgment) | n/a | n/a (protocol) | ⬜ pending |
| TBD | TBD | TBD | VER-05 | — | N/A | e2e (external project build + render) | external project `./gradlew run` + visual confirm | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/theme/VER01GradientProportionalitySourceTest.kt` — stubs for VER-01, VER-06
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/VER02AeroSurfaceClipOrderSourceTest.kt` — stubs for VER-02, VER-06
- [ ] `library/src/test/kotlin/.../VER03BaselineSizeSnapshotTest.kt` — stubs for VER-03, VER-06 (exact package is planner's discretion)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt` — stubs for D-13; reuse the `contrastRatio()` helper recoverable via `git show c35f883~1:...`
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/VerificationSection.kt` — covers SHW-15
- [ ] New external Gradle project outside this repo — covers VER-05
- [ ] Framework install: **none needed** — JUnit 5, `kotlin.test`, and `compose.uiTest` are already declared in `library/build.gradle.kts`

> Source-scan gates MUST strip comments via the `nonCommentSource` helper established in `AeroSwitchSourceTest.kt` — `AeroButtonSurface.kt`'s KDoc contains the literal `.clip()` and other files discuss the historical `endY = 100f` bug in prose.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Showcase demonstrates all eight restyled components in every state (default/hover/press/focus/disabled where applicable) | SHW-15 | Visual completeness of a demo surface cannot be asserted mechanically | Launch the showcase, walk the verification section, confirm each of the eight components renders every applicable state |
| Three-theme sign-off (AeroBlue / AeroDark / Classic) including at least one pass at non-100% DPI scale | SHW-16 | Human visual judgment across themes and DPI scaling | Follow the phase-16 `signoff-capture/` precedent: capture each theme, repeat at least one theme at a non-100% DPI scale, record the verdict |
| Scratch consumer renders (not merely compiles) all eight components | VER-05 | Rendering correctness in a foreign project is confirmed by eye; the build is automated but the render is not | Build the external project against the artifact, run it, visually confirm all eight components render |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
