---
phase: 19
slug: selectors-lists
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-27
---

# Phase 19 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Source: `19-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | `kotlin.test` (JUnit-backed) for pure-function/resolver tests; `androidx.compose.ui.test.runComposeUiTest` (`@OptIn(ExperimentalTestApi::class)`, CMP 1.11) for semantics/keyboard tests — both already wired project-wide (`AeroSliderStylesTest.kt`, `AeroButtonSemanticsTest.kt`) |
| **Config file** | none dedicated — standard Gradle `test` source set (`library/src/test/kotlin/...`) |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.selection.*"` / `--tests "com.mordred.aero.components.list.*"` |
| **Full suite command** | `./gradlew build` (TOOL-05/VER precedent — 232+ test baseline) |
| **Estimated runtime** | ~10–30 seconds (module-scoped filter); full build minutes |

---

## Sampling Rate

- **After every task commit:** Run the module-scoped filter for whichever component's files just changed — `./gradlew :library:test --tests "*.Aero{Component}*Test"`
- **After every plan wave:** Run `./gradlew :library:test` (full library module, without showcase) to catch cross-component regressions in shared `theme/` files
- **Before `/gsd-verify-work`:** Full `./gradlew build` green, PLUS human three-theme × per-state visual sign-off (Phase 17-05 / 18-04 checkpoint precedent)
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

> Filled by `/gsd-validate-phase` once task IDs exist. Requirement → test-command mapping is already fixed below.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | 0 | VSEL-01, VSEL-02 | — | N/A | unit | `./gradlew :library:test --tests "*.AeroSwitchStylesTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VSEL-02 | — | N/A | semantics | `./gradlew :library:test --tests "*.AeroSwitchSemanticsTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VSEL-01, VLST-04, PRIM-14 | — | N/A | source-scan | `./gradlew :library:test --tests "*.AeroSwitchSourceTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VSEL-03 | — | N/A | unit | `./gradlew :library:test --tests "*.AeroSegmentedControlStylesTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VSEL-04 | — | N/A | semantics | `./gradlew :library:test --tests "*.AeroSegmentedControlSemanticsTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VSEL-03, VSEL-04, VLST-04, PRIM-14 | — | N/A | source-scan | `./gradlew :library:test --tests "*.AeroSegmentedControlSourceTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VLST-02 | — | N/A | unit | `./gradlew :library:test --tests "*.AeroListItemStylesTest"` | ❌ W0 | ⬜ pending |
| TBD | TBD | 0 | VLST-01, VLST-03, PRIM-14 | — | N/A | source-scan | `./gradlew :library:test --tests "*.AeroListItemSourceTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchStylesTest.kt` — resolver tests (VSEL-01/VSEL-02); mirrors `AeroSliderStylesTest.kt` (reconstruct-expected-from-scratch, disabled-wins, both `AeroBlue` + `Classic` schemes)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSemanticsTest.kt` — `Role.Switch` + keyboard Space toggles `checked` survives restyle; mirrors `AeroButtonSemanticsTest.kt`
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` — source-scan: `aeroGroove(` / `aeroThumbSurface(` present, `Color.Transparent` absent, `.hoverable(` present, `pointerInput` absent, `pressedRecess` ABSENT (D-05 forbids it on the thumb)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` — value-equality that selected style == `AeroSurfaceStyle.rest(colors, 4.dp).pressedRecess(PRESSED_INNER_SHADOW)` reconstructed independently (VSEL-03)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSemanticsTest.kt` — `Role.RadioButton`, per-segment Tab + Space/Enter; mirrors `AeroButtonSemanticsTest.kt`'s `performKeyInput`/`pressKey` pattern
- [ ] `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt` — source-scan: `pressedRecess(` present, no per-segment `aeroGlowRing(`, `.hoverable(` present, `pointerInput`/`awaitPointerEventScope` absent, `Color.Transparent` absent
- [ ] `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt` — **highest priority**: the VLST-02 structural-fix test proving hover-on-selected composes (strictly brighter than resting-selected, byte-equal to the lighten-transform) rather than being suppressed
- [ ] `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt` — source-scan: `aeroSurface(` present (pill clipping), no reintroduced `when { selected -> …; hovered -> … }` branch shape, `Color.Transparent` absent
- [ ] **Fail-then-pass proof (VER-06 discipline):** every new source-scan guard must be run once against the CURRENT pre-restyle file to confirm it FAILS, then re-run post-restyle to confirm it PASSES. Precedent: `AeroButtonSurfaceSourceTest.kt`'s documented "Fail-Then-Pass Proof".

*No test-framework install needed — `kotlin.test` / `runComposeUiTest` are already wired project-wide.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Switch groove recess + raised thumb gloss/bevel/shadow reads as Aero volume | VSEL-01 | Pixel-diff tooling ruled out at milestone level (Roborazzi/Paparazzi are Android-only; this is CMP desktop) | Run showcase, view `AeroSwitch` in all three themes (AeroBlue, Classic, third scheme) at rest/hover/press/focus/disabled; confirm groove reads recessed and thumb reads raised |
| Selected segment reads recessed vs unselected siblings | VSEL-03 | Same — visual judgement | Showcase `AeroSegmentedControl`, all three themes, cycle selection; confirm inverted gradient + inner shadow matches Phase 17 pressed-button appearance |
| Selection pill + rim light reads as a clipped Aero pill; hover-on-selected is visibly brighter | VLST-01, VLST-02 | Same — visual judgement of the combined state | Showcase list, select a row, then hover that same row; confirm the highlight brightens rather than changing to the unselected-hover treatment |
| Focus visuals are in-bounds (no clipped/cut-off glow) on segmented control and list rows | VSEL-04, VLST-03 | Both sit inside an outer `.clip()` — documented exception to the library's glow language | Tab through segments and rows in all three themes; confirm no focus ring is visually truncated at the container edge |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
