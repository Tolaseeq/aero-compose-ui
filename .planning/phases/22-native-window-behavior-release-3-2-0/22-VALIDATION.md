---
phase: 22
slug: native-window-behavior-release-3-2-0
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-09-25
---

# Phase 22 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 6.1.3 via `kotlin.test` (pure-JVM math/registry tests); Compose `runComposeUiTest` (title-bar region reporting) |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }` + `lockedTestTotal` / `lockedTestSkipped` guard) |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.internal.windows.*"` (once the package exists) |
| **Full suite command** | `./gradlew test` (only `:library` has a test source set) |
| **Live-window harness** | opt-in, outside the default suite (shape per CONTEXT.md Claude's Discretion; research recommends a Kotlin `main()` under `tools/`) — real HWND probes via `SendMessage(WM_NCHITTEST)`, `GetWindowLongPtr(GWL_STYLE)`, `GetWindowRect` vs `MONITORINFO.rcWork`, `SHAppBarMessage` |
| **Estimated runtime** | one full-suite run (measure at the first task) |

---

## Sampling Rate

- **After every task commit:** full `./gradlew test` at the locked count — the project's standing rule since v3.1, stricter than quick-run sampling.
- **After every plan wave:** full `./gradlew test` + the live-window harness against the showcase window(s).
- **Before `/bm:verify-work`:** full suite green at exactly the (named-commit) locked count; every VER-11 check shown RED on the old behavior and GREEN on the new; VER-12 full session done on JDK 21 and JBR 21.
- **Max feedback latency:** one full-suite run.

---

## Per-Task Verification Map

Requirement-level map from RESEARCH.md § Validation Architecture. The planner assigns task IDs; the executor fills Status.

| Task | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|------|-------------|----------|-----------|-------------------|-------------|--------|
| TBD | API-01 | existing `AeroTitleBar(...)` / `AeroResizeHandles(...)` call sites compile unchanged; no JNA type in public signatures | build + grep | `./gradlew :library:compileKotlin :showcase:compileKotlin`; public-API grep for `com.sun.jna` = 0 | ✅ call sites exist | ⬜ pending |
| TBD | DEP-01 | JNA + jna-platform 5.19.1 as `implementation` in `:library` | build + grep | `grep -n "libs.jna" library/build.gradle.kts` shows `implementation(` | ❌ W0 | ⬜ pending |
| TBD | SNAP-01..04, BTN-02, API-02 | hit-test classification: caption / min / max / close / marked-interactive / client / 8 edges+corners | unit | `./gradlew :library:test --tests "*HitTest*"` | ❌ W0 | ⬜ pending |
| TBD | WIN-01, WIN-04 | DPI frame-thickness formula; maximized client rect vs work area incl. taskbar on each edge + auto-hide inset | unit | `./gradlew :library:test --tests "*Win32Dpi*" --tests "*MaximizedRect*"` | ❌ W0 | ⬜ pending |
| TBD | D-01 | min-size resolution: app `minimumSize` if set, else 320×240 dp; physical px = dp × scale | unit | `./gradlew :library:test --tests "*MinSize*"` | ❌ W0 | ⬜ pending |
| TBD | WIN-06 | region registry: per-window state, no torn reads under concurrent writer/reader | unit (stress) | `./gradlew :library:test --tests "*HitTestRegionRegistry*"` | ❌ W0 | ⬜ pending |
| TBD | VER-11 / WIN-03 | style bits read-back (`WS_CAPTION`/`WS_SYSMENU`/`WS_THICKFRAME`/min/max box) — RED on old, GREEN on new | live-window harness | harness `styles` check | ❌ W0 | ⬜ pending |
| TBD | VER-11 / SNAP / WIN-02 / BTN-02 | `WM_NCHITTEST` answers at title bar, 3 buttons, edges, corners, client, marked element — RED (old: `HTCLIENT`) → GREEN; `nativeWindowManagement = false` window reproduces old answers | live-window harness | harness `hittest` check | ❌ W0 | ⬜ pending |
| TBD | VER-11 / WIN-01 | maximized `GetWindowRect` == monitor `rcWork` (minus auto-hide edge inset when auto-hide is on) | live-window harness | harness `maximize` check | ❌ W0 | ⬜ pending |
| TBD | API-03 | opt-out window keeps today's behavior (no subclass, Compose drag/resize) | live-window harness + unit | harness on the opt-out window | ❌ W0 | ⬜ pending |
| TBD | SHW-17 | showcase opens second narrow window (~300 px, own minimum per D-01) with a marked interactive header element | build + live | `./gradlew :showcase:compileKotlin`; harness finds 2 HWNDs | ❌ W0 | ⬜ pending |
| TBD | VER-14 | locked test count raised by a commit naming the reason; guard still proven | build | `./gradlew :library:test --rerun` → `AERO_TEST_COUNT total=<new>` | ✅ mechanism (`ffece58`) | ⬜ pending |
| TBD | REL-06/07 | README window-behavior section; "Aero Snap limitation" KDoc removed/rewritten | grep | `grep -c "Aero Snap limitation" .../AeroTitleBar.kt` = 0; README section present | ✅ files exist | ⬜ pending |
| TBD | REL-08 / DEP-01 | `v3.2.0` resolves on JitPack (verify tag first) | remote | JitPack build-status API → `ok` for verify tag, then `v3.2.0` | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] RED baseline: live-window probe of today's unmodified `WindowDraggableArea` showcase window (style bits, `WM_NCHITTEST` answers, maximized rect) captured **before** any new hit-test code
- [ ] `gradle/libs.versions.toml` + `library/build.gradle.kts` — JNA 5.19.1 (`implementation`), legitimacy already verified (CONTEXT.md D-03)
- [ ] `library/src/test/kotlin/com/mordred/aero/internal/windows/` — new test package
- [ ] Live-window opt-in harness (not part of `gradlew test`)
- [ ] UI Automation Snap Layouts flyout detector under `tools/` (CONTEXT.md D-04)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Early gate: hover max → Snap Layouts flyout; drag title to edge → snaps | VER-12 (early), SNAP-01/02 | needs real OS pointer input; OS draws the flyout | warn maintainer, get "ok", 1–2 min real input on the spike window, flyout recorded via UIA (D-04); fail → stop and ask |
| Full snap / hotkeys / Alt+Space / double-click / shared border / Snap Groups / FancyZones / cross-monitor DPI / auto-hide + visible taskbar | VER-12 (full), SNAP-01..07, WIN-01..06, BTN-01..02 | real OS input; virtual 150% display + PowerToys installed temporarily | warn + "ok"; run on standard JDK 21 (`run`) and JBR 21 (`hotRun`); remove display driver, restore auto-hide, PowerToys removed on maintainer's word |
| Agent self-inspection before hand-off | VER-13 | visual judgement of `PrintWindow` frames | agent reviews frames of every window/state first; hand-off = frames + VER-11 results + VER-12 results + "unconfirmed" list (incl. Windows 10) |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency ≤ one full-suite run
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
