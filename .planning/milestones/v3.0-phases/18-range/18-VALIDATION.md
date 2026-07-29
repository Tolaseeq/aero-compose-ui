---
phase: 18
slug: range
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-07-23
---

# Phase 18 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Seeded from `18-RESEARCH.md` §"Validation Architecture".

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | `kotlin.test` + JUnit Jupiter (JUnit Platform), already configured (`library/build.gradle.kts:31,32,35,45`) |
| **Config file** | `library/build.gradle.kts` (`useJUnitPlatform()`, line 45) — no separate config file |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.range.*"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30–60 seconds (full suite is 232+ tests per STATE.md) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "com.mordred.aero.components.range.*"`
- **After every plan wave:** Run `./gradlew :library:test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** ~60 seconds

---

## Per-Task Verification Map

> Task IDs are placeholders until the planner finalizes plan/wave decomposition; the requirement→test binding below is authoritative.

| Req ID | Behavior | Test Type | Automated Command | File Exists |
|--------|----------|-----------|-------------------|-------------|
| VRNG-01/03 | New `resolveSliderThumbStyle`-equivalent resolver returns correct rest/hover/press/disabled `AeroSurfaceStyle` per state, across AeroBlue + Classic | unit (value-level, no Compose runtime) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroSliderStylesTest"` | ❌ W0 — mirror `AeroButtonStylesTest.kt` |
| VRNG-02 | Existing `AeroRangeSliderTest.kt` logic tests (`snapToStep`/`applyThumbMove`/`xToValue`/`valueToX`) still pass unchanged — proves render-only edit didn't touch logic | unit (pre-existing) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroRangeSliderTest"` | ✅ exists |
| VRNG-01/02 | `AeroSlider.kt` uses the custom-slot `Slider(...)` call (not plain 6-arg overload) and does NOT reference `SliderColors.disabled*` (D-07) | unit (source-scan guard) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroSliderSourceTest"` | ❌ W0 — mirror `AeroButtonSurfaceSourceTest.kt` |
| VRNG-04 | `AeroRangeSlider.kt`'s `awaitPointerEventScope` drag block is byte-identical to pre-restyle source | unit (source-scan / diff guard) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroRangeSliderDragLogicUntouchedTest"` | ❌ W0 |
| VRNG-07/08 | `AeroProgressBar` indeterminate spec still `tween(1500, LinearEasing)` + `RepeatMode.Restart` (never `Reverse`); new `showRunningSheen` defaults `false` | unit (source-scan + default-value assertion) | `./gradlew :library:test --tests "com.mordred.aero.components.range.AeroProgressBarSourceTest"` | ❌ W0 |
| VRNG-05/09 | Per-thumb interaction state wiring + Pattern 3 (`isDragging`-gated `snap()`) — visual/timing, if a pure state function is extracted it is unit-tested; otherwise manual | unit (if pure fn extracted) / manual | manual — three-theme showcase review | manual-only (justified below) |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderStylesTest.kt` — VRNG-01/03; mirror `AeroButtonStylesTest.kt`'s value-level resolver pattern (construct expected `AeroSurfaceStyle` via `AeroSurfaceStyle.neutralRest(...)` + transforms, assert equality against resolver output, across `AeroColorScheme.AeroBlue` and `AeroColorScheme.Classic`)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt` — VRNG-01/02/D-07; mirror `AeroButtonSurfaceSourceTest.kt`'s file-content-assertion pattern
- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt` — VRNG-07/08 (RepeatMode.Restart present, RepeatMode.Reverse absent, 1500 present, `showRunningSheen` default false)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderDragLogicUntouchedTest.kt` — VRNG-04 drag-block source guard
- [ ] **Repro-must-exercise-the-path discipline:** every new source-scan test above must be proven to FAIL against a deliberately-reintroduced violation before being trusted as a real gate — document the fail-then-pass proof in the plan's SUMMARY, mirroring `17-03-SUMMARY.md`.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Recessed groove + raised glossy thumb + hover/focus glow read correctly on all three themes | VRNG-01/03/05/06 | Visual fidelity is not unit-testable; three-theme review is the project's mandated gate | Launch showcase, exercise slider/range/progress across AeroBlue + Classic + (third theme), verify against UI-SPEC per-state visuals |
| D-04 dragged thumb "stays raised + glow intensifies" (not pushed-in) | VRNG-05 | Compose-runtime interaction timing | Drag each thumb; confirm raised state persists and glow intensifies per thumb independently |
| Pattern 3 animation-vs-drag interplay (VRNG-09) | VRNG-09 | `animateFloatAsState` wiring is a Compose-runtime timing behavior; `AeroPanelGroup`'s own precedent tests pure logic, not the `animateFloatAsState` wiring | Drag while an animation target is set; confirm no fighting/jitter, drag writes directly, `snap()` engages while dragging |
| Indeterminate segment sweep (1500ms restart, no ping-pong, soft `alpha=0f` edges) | VRNG-08 | Visual/timing | Observe indeterminate bar; confirm single left→right sweep, 1500ms restart, no ping-pong, edges fade (not `Color.Transparent` block on Classic) |

---

## Security Domain

**Not applicable.** This phase has NO input-handling, auth, session, access-control, or cryptography surface. It is a pure rendering restyle of three UI controls that already accept/emit `Float`/`ClosedFloatingPointRange<Float>` values with existing `coerceIn`/`snapToStep` clamping logic untouched by this phase. No ASVS category applies (V2/V3/V4/V5 all `no`; V5 input clamping is pre-existing and untouched).

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
