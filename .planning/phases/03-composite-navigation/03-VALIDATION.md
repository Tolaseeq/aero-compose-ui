---
phase: 3
slug: composite-navigation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-28
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Jupiter) 5.10.0 + kotlin-test |
| **Config file** | `library/build.gradle.kts` — already wired in Phase 1 (`tasks.test { useJUnitPlatform() }`) |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.<package>.*"` |
| **Full suite command** | `./gradlew :library:test :library:compileKotlin :showcase:compileKotlin` |
| **Estimated runtime** | ~25 seconds full / ~8 seconds incremental |

*Existing infrastructure is sufficient — no framework install needed. Phase 1/2 tests already pass.*

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:test --tests "com.mordred.aero.components.<package>.*"`
- **After every plan wave:** Run `./gradlew :library:test :library:compileKotlin :showcase:compileKotlin`
- **Before `/gsd:verify-work`:** Full suite green + manual showcase visual checkpoint (every Phase 1-3 component clicked through all three themes)
- **Max feedback latency:** 25 seconds

---

## Per-Task Verification Map

> Plan IDs are the recommended grouping from RESEARCH.md (final plan numbering will be assigned by gsd-planner; map will be tightened during planning).

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 3-W0-01 | 01 | 0 | OVL-03,04,07 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroPopupPositionProviderTest.*"` | ❌ W0 | ⬜ pending |
| 3-W0-02 | 06 | 0 | OVL-05 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroToastHostStateTest.*"` | ❌ W0 | ⬜ pending |
| 3-W0-03 | 04 | 0 | OVL-02 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroAlertKindTest.*"` | ❌ W0 | ⬜ pending |
| 3-W0-04 | 06 | 0 | OVL-06 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroBannerKindTest.*"` | ❌ W0 | ⬜ pending |
| 3-W0-05 | 05 | 0 | OVL-04 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroContextMenuItemTest.*"` | ❌ W0 | ⬜ pending |
| 3-W0-06 | 07 | 0 | NAV-04 | unit (Wave 0) | `./gradlew :library:test --tests "*.AeroBreadcrumbTest.*"` | ❌ W0 | ⬜ pending |
| 3-01-CNT05 | 01 | 1 | CNT-05 | compile + smoke | `./gradlew :library:test --tests "*.AeroScrollAreaTest.*"` | ❌ W0 | ⬜ pending |
| 3-01-CNT06 | 01 | 1 | CNT-06 | compile + smoke | `./gradlew :library:test --tests "*.AeroScrollBarTest.*"` | ❌ W0 | ⬜ pending |
| 3-02-CNT01 | 02 | 1 | CNT-01 | compile + visual | `./gradlew :library:compileKotlin` + showcase visual | ❌ W0 | ⬜ pending |
| 3-02-CNT02 | 02 | 1 | CNT-02 | compile + visual | as above | ❌ W0 | ⬜ pending |
| 3-02-CNT03 | 02 | 1 | CNT-03 | compile + visual | as above | ❌ W0 | ⬜ pending |
| 3-02-CNT04 | 02 | 1 | CNT-04 | compile + visual | as above | ❌ W0 | ⬜ pending |
| 3-03-NAV01 | 03 | 2 | NAV-01 | compile + manual smoke | `./gradlew :showcase:compileKotlin` + manual: drag, min, max/restore, close on Win11 | ❌ W0 | ⬜ pending |
| 3-04-OVL01 | 04 | 2 | OVL-01 | compile + manual smoke | `./gradlew :library:compileKotlin` + manual: open + Esc-close, no Win11 crash | ❌ W0 | ⬜ pending |
| 3-04-OVL02 | 04 | 2 | OVL-02 | compile + data-class + visual | `./gradlew :library:test --tests "*.AeroAlertKindTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-04-OVL08 | 04 | 2 | OVL-08 | compile + manual smoke | manual: slide-in, scrim-click + Esc dismiss | ❌ W0 | ⬜ pending |
| 3-05-OVL03 | 05 | 2 | OVL-03 | compile + position-provider + visual | `./gradlew :library:test --tests "*.AeroPopupPositionProviderTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-05-OVL04 | 05 | 2 | OVL-04 | compile + sealed-class + visual | `./gradlew :library:test --tests "*.AeroContextMenuItemTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-05-OVL07 | 05 | 2 | OVL-07 | compile + position-provider + visual | shared with 3-05-OVL03 | ✅ W0 | ⬜ pending |
| 3-06-OVL05 | 06 | 2 | OVL-05 | unit (state model) + visual | `./gradlew :library:test --tests "*.AeroToastHostStateTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-06-OVL06 | 06 | 2 | OVL-06 | compile + data-class + visual | `./gradlew :library:test --tests "*.AeroBannerKindTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-07-NAV02 | 07 | 3 | NAV-02 | compile + manual smoke | manual: top items + dropdown + cross-item hover-switch | ❌ W0 | ⬜ pending |
| 3-07-NAV03 | 07 | 3 | NAV-03 | compile + visual | `./gradlew :library:compileKotlin` + visual | ❌ W0 | ⬜ pending |
| 3-07-NAV04 | 07 | 3 | NAV-04 | unit + visual | `./gradlew :library:test --tests "*.AeroBreadcrumbTest.*"` + visual | ✅ W0 | ⬜ pending |
| 3-07-NAV05 | 07 | 3 | NAV-05 | compile + visual | `./gradlew :library:compileKotlin` + visual | ❌ W0 | ⬜ pending |
| 3-08-SHW | 08 | 4 | (showcase wiring, all phase reqs) | build smoke + manual visual checkpoint | `./gradlew :showcase:compileKotlin :showcase:run` + click-through every component in each of 3 themes | implicit | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Plan/Wave columns are advisory — the planner may consolidate or split waves. The Wave-0 unit tests remain mandatory regardless of grouping.*

---

## Wave 0 Requirements

Wave 0 must produce stub tests so subsequent waves have something to keep green. Stubs that compile + reference the planned public API are sufficient at Wave 0; assertions are filled in by the wave that ships the component.

- [ ] `library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt` — position + auto-flip unit tests (shared OVL-03, OVL-04, OVL-07)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroToastHostStateTest.kt` — state-model lifecycle: enqueue, auto-dismiss after duration, evict-oldest at stack limit
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroAlertKindTest.kt` — every `AeroAlertKind` variant maps to icon + color
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroBannerKindTest.kt` — same pattern for `AeroBannerKind`
- [ ] `library/src/test/kotlin/com/mordred/aero/components/overlay/AeroContextMenuItemTest.kt` — sealed-class hierarchy structural test
- [ ] `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroBreadcrumbTest.kt` — `onItemClick(index, item)` emits correct index
- [ ] Compile-reachability stubs for all remaining public composables (one `Test` per component, mirroring the Phase 1 pattern in `AeroThemeTest`)

*No new test dependencies — JUnit 5 + kotlin-test from Phase 1 already cover all needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AeroDialog opens DialogWindow, Esc closes, no Win11 crash | OVL-01 | DialogWindow is OS-level — no headless unit test | Run showcase, open dialog from showcase, press Esc, verify dialog closes and no `EXCEPTION_ACCESS_VIOLATION`. Repeat with `undecorated=true` only (never `transparent=true`). |
| AeroDrawer slides in from edge, scrim click + Esc dismiss | OVL-08 | Animation + interaction — visual decisive | Toggle drawer in showcase, verify slide-in animation, click scrim → dismisses, press Esc → dismisses |
| AeroTitleBar drag region, Min/Max/Restore/Close, no transparent flag | NAV-01 | Window chrome — needs real `FrameWindowScope` on Windows 11 | Showcase Main.kt switches to `Window(undecorated = true, transparent = false)`. Manually: drag the window by titlebar, click Minimize → window iconifies, click Maximize → toggles `WindowPlacement.Maximized`/`Floating`, click Close → app exits. Verify zero crashes on Windows 11. |
| AeroMenuBar dropdown + cross-item hover-switch | NAV-02 | Interaction-heavy hover state model | Click File menu → opens dropdown. Move mouse to Edit item without closing → Edit dropdown opens, File closes. Click outside → both close. |
| AeroToast stacked queue + auto-dismiss + manual close | OVL-05 | Animation timing + lifecycle visual | Trigger 3 toasts in showcase, verify they stack, oldest auto-dismisses after configured duration, close button dismisses immediately. Trigger >5 → oldest evicts. |
| AeroPopover / AeroTooltip / AeroContextMenu auto-flip on overflow | OVL-03, OVL-04, OVL-07 | Position logic verified in unit; visual confirms theme + anchor | Resize showcase window so anchor hits viewport edge, hover/click → popup appears on opposite side instead of clipping |
| Showcase visual checkpoint — all three themes across all components | All Phase 1-3 reqs | Visual diff not in scope — the project's primary verification primitive | Run `./gradlew :showcase:run`, click through every component in Containers / Overlays / Navigation sections, toggle AeroBlue → AeroDark → Classic for each, verify glass styling and color tokens render correctly with no Material3 surfaces visible |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (7 stub files above)
- [ ] No watch-mode flags
- [ ] Feedback latency < 25s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
