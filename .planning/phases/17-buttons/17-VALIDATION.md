---
phase: 17
slug: buttons
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-23
---

# Phase 17 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Source of truth: `17-RESEARCH.md` § Validation Architecture (this file is the governance mirror consumed by validate-phase / audit-milestone).

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | `kotlin.test` (JVM) + `androidx.compose.ui.test` (`@ExperimentalTestApi`, v1 `runComposeUiTest` — kept over the v2 API per TOOL-03/TOOL-04, confirmed working at CMP 1.11.1) |
| **Config file** | none dedicated — default Gradle `tasks.test {}` block, `library/build.gradle.kts:44` |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.buttons.*"` |
| **Full suite command** | `./gradlew :library:test` (232+ tests baseline as of Phase 15/16 — must stay green) |
| **Estimated runtime** | ~quick: seconds (pure-fn) · full suite: ~1–2 min |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "com.mordred.aero.components.buttons.*"`
- **After every plan wave:** Run `./gradlew :library:test` (full suite)
- **Before `/gsd-verify-work`:** Full suite green **AND** three-theme × 5-state human sign-off in the showcase (mandatory — see Manual-Only Verifications)
- **Max feedback latency:** < 30 seconds (pure-function tests dominate the quick loop)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 17-01 | 01 | 1 | VBTN-01 | — | N/A (client-side UI) | integration (`runComposeUiTest`) | `./gradlew :library:test --tests "*AeroButtonTest.rendersWithoutException*"` | ❌ W0 | ⬜ pending |
| 17-01 | 01 | 1 | VBTN-04 | — | N/A | integration (semantics) | `./gradlew :library:test --tests "*AeroButtonTest*"` | ❌ W0 | ⬜ pending |
| 17-01 | 01 | 1 | VBTN-06 | — | N/A | unit (source-scan guard) | `./gradlew :library:test --tests "*AeroButtonSurfaceTest*"` | ❌ W0 | ⬜ pending |
| 17-02 | 02 | 2 | VBTN-01 | — | N/A | unit (pure fn) | `./gradlew :library:test --tests "*AeroButtonStylesTest*"` | ❌ W0 | ⬜ pending |
| 17-02 | 02 | 2 | VBTN-02 | — | N/A | unit (pure fn — lighten/swap/flatten deltas) | `./gradlew :library:test --tests "*AeroButtonStylesTest*"` | ❌ W0 | ⬜ pending |
| 17-03 | 03 | 3 | VBTN-03 | — | N/A | unit (source-scan: no unclipped overlay) | `./gradlew :library:test --tests "*AeroButtonTest*NoUnclippedOverlay*"` | ❌ W0 | ⬜ pending |
| 17-03 | 03 | 3 | VBTN-04 | — | N/A | integration (`performKeyInput` Space/Enter) | `./gradlew :library:test --tests "*AeroButtonTest*KeyboardActivation*"` | ❌ W0 (spike compile-proof first) | ⬜ pending |
| 17-03 | 03 | 3 | VBTN-05 | — | N/A | unit (pure fn — fixed delta per state) | `./gradlew :library:test --tests "*AeroOutlinedButtonStylesTest*"` | ❌ W0 | ⬜ pending |
| 17-03 | 03 | 3 | VBTN-06 | — | N/A | unit (source-scan: shared surface, no drift) | `./gradlew :library:test --tests "*AeroButtonSurfaceTest*"` | ❌ W0 | ⬜ pending |
| 17-04 | 04 | 4 | VBTN-02, VBTN-05 | — | N/A | manual (showcase demo rows) | n/a — feeds the human sign-off | ❌ W0 | ⬜ pending |
| 17-05 | 05 | 5 | VBTN-01, VBTN-02, VBTN-05 | — | N/A | manual (three-theme × 5-state sign-off) | n/a — blocking human-verify checkpoint | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` — pure-fn value-level tests for VBTN-01/02 style resolution (no Compose runtime)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt` — VBTN-05 delta-transform tests
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt` — `runComposeUiTest` render/semantics/keyboard tests for VBTN-01/03/04
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceTest.kt` — VBTN-06 source-scan regression guard (both public buttons call the one shared internal composable)
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` — extend demo rows (currently only enabled/disabled) to exercise all 5 states × both variants for the mandatory three-theme sign-off
- [ ] Compile-proof spike for `performKeyInput`/`pressKey`/`Key.Enter`/`Key.Spacebar` against the CMP 1.11.1 desktop test artifact (mirrors `ScratchAeroShadowProof.kt`) — BEFORE the real VBTN-04 keyboard test (Pitfall 6)

**Repro-must-exercise-the-path requirement (project lesson, v2.0.3/v2.0.4):** the VBTN-03 (no-unclipped-overlay) and VBTN-06 (shared-surface) regression guards must each be proven to actually FAIL against the current pre-fix source (or a deliberately reintroduced duplicate-painter / unclipped overlay) before counting as real gates — a guard that passes against both broken and fixed code is not a guard (TOOL-04/VER-06 precedent).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Three-theme × 5-state visual review reads as genuine Aero (glow/recess/flatten depth, not just alpha changes) | VBTN-02 | Glass depth / gloss / recess perception cannot be asserted at the pixel level with confidence; requires human judgment across Aero, Classic, and the third theme preset | Launch showcase → ButtonsSection → for each of {Aero, Classic, 3rd} theme, exercise filled + outlined × {rest, hover, press, focus, disabled}; confirm each state reads in Aero idiom. Blocking checkpoint 17-05; partial-theme approval prohibited. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] VBTN-03 & VBTN-06 guards proven-to-fail-first against pre-fix source
- [ ] `nyquist_compliant: true` set in frontmatter (by validate-phase, after guards proven)

**Approval:** pending
