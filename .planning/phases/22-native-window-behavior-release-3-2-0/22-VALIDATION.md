---
phase: 22
slug: native-window-behavior-release-3-2-0
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-09-25
---

# Phase 22 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 6.1.3 via `kotlin.test` (pure-JVM math/registry tests); Compose `runComposeUiTest` (title-bar button pixel parity) |
| **Config file** | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }` + `lockedTestTotal` / `lockedTestSkipped` guard) |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.internal.windows.*"` (exists from Plan 13; filtered runs are exempt from the count guard) |
| **Full suite command** | `./gradlew :library:test --rerun` (only `:library` has a test source set) |
| **Live-window harness** | `tools/winprobe/Invoke-WinProbe.ps1` (PowerShell 5.1 + C#, Plan 01) — opt-in, never part of `gradlew test`; cross-process `WM_NCHITTEST` through the real child→frame chain, `GetWindowLongPtr(GWL_STYLE)`, maximized client rect vs `MONITORINFO.rcWork`, `SHAppBarMessage`, SetWindowPos min-size clamp, UIA summary. `-AssertV11` = GREEN gate, `-ExpectRed` = RED gate |
| **Real-input tooling** | `tools/winprobe/RealInput.ps1` (guarded SendInput), `Watch-SnapFlyout.ps1` (UIA flyout watcher, D-04), `Invoke-EarlyGate.ps1`, `SessionEnv.ps1`, `Invoke-FullSession.ps1` — every input function requires the session object built from the maintainer's verbatim "ok"; all have `-DryRun` |
| **Estimated runtime** | full suite: one run per task commit (measured in Plan 02 Task 1); probe run ≈ showcase launch + < 30 s |

---

## Sampling Rate

- **After every task commit:** full `./gradlew :library:test --rerun` at the locked count — the project's standing rule since v3.1, stricter than quick-run sampling. Until Plan 13 the locked count is 541; Plan 13 raises it in two named commits (VER-14).
- **After every plan that touches native behavior (02, 05, 07, 09, 10, 11, 12):** the live-window harness against the showcase window(s), results written to 22-NOTES.md.
- **Before `/bm:verify-work`:** full suite green at exactly the (named-commit) locked count; every VER-11 check shown RED (Plan 01 baseline, `-Paero.nativeChrome=false` control) and GREEN (Plan 16 final run); VER-12 full session done on JDK 21 and JBR 21 (Plan 15).
- **Max feedback latency:** one full-suite run.

---

## Per-Task Verification Map

| Task | Requirement | Behavior | Test Type | Automated Command | File Exists | Status |
|------|-------------|----------|-----------|-------------------|-------------|--------|
| 22-01-T2, 22-01-T3 | VER-11 (RED half) | probe harness; every V11 check FAILS on the unmodified window | live-window harness | `Invoke-WinProbe.ps1 -Launch run -ExpectRed` → `RED OK` | ❌ W0 (Plan 01) | ⬜ pending |
| 22-02-T1 | DEP-01 | JNA + jna-platform 5.19.1 as `implementation` in `:library`; not on showcase compile classpath | build + grep | `./gradlew :library:dependencies --configuration runtimeClasspath \| grep jna` and `:showcase:dependencies --configuration compileClasspath` has no jna | ❌ W0 (Plan 02) | ⬜ pending |
| 22-02-T2, 22-10-T1 | invariants (PITFALLS 1/2/3/7/16) | HWND-keyed strong callback refs, CallWindowProc passthrough, idempotent install, no bare SetWindowLong | grep + live | `! grep -rnE "SetWindowLong\(\|GetWindowLong\(" library/src`; 22-10-T2 GC stress + 3 reloads | ❌ (Plan 02) | ⬜ pending |
| 22-02-T3 | SNAP-01/02 spike, C1/C4/C7 | caption=2 / max=9 through the chain on JDK 21 and JBR 21; placement sync; reload idempotency | live-window harness | `Invoke-WinProbe.ps1 -Launch run` and `-Launch hotRun` | ❌ (Plan 02) | ⬜ pending |
| 22-04-T3 | VER-12 early, SNAP-01/02, D-04 | flyout via UIA vs positive control; drag-to-edge snap; restore | real input (authorized) | `Invoke-EarlyGate.ps1 -AuthorizedBy …` → `GATE PASS` | ❌ (Plan 03) | ⬜ pending |
| 22-05-T3 | VER-11 / WIN-03 | V11-STYLE; title band identical to baseline; no white strip / ghost caption | live-window harness | `Invoke-WinProbe.ps1 -Report styles,v11` + `Compare-WinProbeRegion` | ❌ (Plan 01) | ⬜ pending |
| 22-05-T3 (or 22-15-T3 per F9) | VER-11 / WIN-01 | maximized client == rcWork; auto-hide edge left uncovered | live-window harness | V11-MAX-WORKAREA, V11-AUTOHIDE-EDGE | ❌ (Plan 01) | ⬜ pending |
| 22-06-T3 | WIN-03 / C6 | chosen corner/shadow applied; DWM attribute read back | live (trace) | `-Paero.chromeTrace=true` read-back line | ❌ (Plan 06) | ⬜ pending |
| 22-07-T3, 22-11-T3, 22-12-T3 | VER-11 / SNAP / WIN-02 / BTN-02 | hit-test answers at caption, 3 buttons, 8 edges/corners, client, leading, marked element — main and narrow windows | live-window harness | `Invoke-WinProbe.ps1 -Windows main,narrow -AssertV11` | ❌ (Plans 01/08) | ⬜ pending |
| 22-09-T3 | BTN-01 / WIN-05 / C5 | NC down/up at HTMAXBUTTON toggles placement; no classic-button paint | live-window harness | `Send-WinProbeMessage -Post` + reporter placement | ❌ (Plan 01) | ⬜ pending |
| 22-10-T2 | WIN-05 / VER-11 | WindowState follows OS changes; WNDPROC survives; GC stress; UIA summary equals baseline | live-window harness | probe sync steps + `jcmd GC.run` loop | ❌ (Plan 01) | ⬜ pending |
| 22-11-T3 | D-01 / WIN-02 | min-size floor: main 320×240 dp, narrow 260×200 (app's own) | live-window harness | V11-MINSIZE, V11-N-MINSIZE | ❌ (Plans 01/08) | ⬜ pending |
| 22-12-T3, 22-16-T3 | API-03 / VER-11 RED control | opt-out window reproduces pre-phase answers and style | live-window harness | `-GradleProps -Paero.nativeChrome=false -ExpectRed` → `RED OK` | ❌ (Plan 12) | ⬜ pending |
| 22-12-T1 | API-01 | old call sites compile unchanged; no JNA in public API | build + grep | `./gradlew :library:compileKotlin :showcase:compileKotlin` with untouched showcase; JNA only under internal/windows/ | ✅ call sites exist | ⬜ pending |
| 22-08-T1, 22-08-T2, 22-12-T3 | SHW-17 / WIN-06 | second narrow window (300 dp, own minimum, marked element); two independent installs; close one keeps the other | build + live | `./gradlew :showcase:compileKotlin`; probe finds 2 HWNDs, WM_CLOSE on narrow | ❌ (Plan 08) | ⬜ pending |
| 22-13-T1 | SNAP-01..04, BTN-02, API-02 | `classifyHitTest` table (caption / buttons / interactive / client / 8 bands / maximized) | unit | `./gradlew :library:test --tests "*HitTestClassificationTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T1 | WIN-01, WIN-04 | maximized client rect incl. taskbar on each edge + auto-hide inset; DPI band formula at 100/125/150/200 % | unit | `./gradlew :library:test --tests "*Win32GeometryTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T1 | D-01 | app minimum if set, else 320×240 dp; px = dp × scale; never overrides the app | unit | `./gradlew :library:test --tests "*MinimumSizeTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T1 | WIN-06 | registry: no torn reads, per-window isolation | unit (stress) | `./gradlew :library:test --tests "*HitTestRegionRegistryTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T1 | PITFALLS 7 / WIN-02 gate | dispatchSafely fallback; resize-handle gate predicate | unit | `--tests "*WndProcSupportTest*" --tests "*ResizeHandlesGateTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T2 | BTN-01 / D-02 | bridged hover/press pixel-identical to Compose-driven, 3 themes | UI test | `./gradlew :library:test --tests "*TitleBarButtonParityTest*"` | ❌ (Plan 13) | ⬜ pending |
| 22-13-T1, 22-13-T2 | VER-14 | locked count raised by commits naming the reason; guard failure recorded before each raise | build | `./gradlew :library:test --rerun` → `AERO_TEST_COUNT total=<N> … expected=<N>` | ✅ mechanism (`ffece58`) | ⬜ pending |
| 22-15-T3 | VER-12 full, SNAP-01..07, WIN-01..06, BTN-01..02, C2, C3 | two-pass real-input suite (JDK 21 `run`, JBR 21 `hotRun`) | real input (authorized) | `Invoke-FullSession.ps1 -AuthorizedBy … -Pass Both -Phase All` | ❌ (Plan 14) | ⬜ pending |
| 22-16-T2 | VER-12 teardown | driver removed, auto-hide restored, PowerToys per word | env check | `SessionEnv.ps1 -SelfTest`; `Screen.AllScreens.Count` = pre-session | ❌ (Plan 14) | ⬜ pending |
| 22-16-T3 | VER-13 / VER-11 | agent frame review; final GREEN + RED; unconfirmed list names Windows 10 | live + docs | `-AssertV11` + `-ExpectRed`; `grep -c "Windows 10" 22-UNCONFIRMED.md` | ❌ (Plan 16) | ⬜ pending |
| 22-17-T1, 22-17-T2 | REL-06 / REL-07 | README window-behavior section; "Aero Snap limitation" KDoc removed | grep | `grep -c "Aero Snap limitation" …/AeroTitleBar.kt` = 0; `grep -c "## Windows window behavior" README.md` ≥ 1 | ✅ files exist | ⬜ pending |
| 22-18-T1, 22-18-T3 | REL-08 / DEP-01 | verify tag ok on JitPack; POM/module JNA runtime-only; one JNA version in a consumer; `v3.2.0` resolves | remote | JitPack build-status API; scratch consumer `dependencies` | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] RED baseline: live-window probe of today's unmodified `WindowDraggableArea` showcase window (style bits, hit-test chain answers, maximized rect, min-size, UIA summary) captured **before** any new hit-test code — **22-01-T3**
- [ ] Live-window opt-in harness (not part of `gradlew test`) — **22-01-T2** (PowerShell + C# under `tools/winprobe/`, Claude's Discretion decision recorded in 22-01)
- [ ] `gradle/libs.versions.toml` + `library/build.gradle.kts` — JNA 5.19.1 (`implementation`), legitimacy already verified (D-03) — **22-02-T1**
- [ ] UI Automation Snap Layouts flyout detector under `tools/` (D-04) — **22-03-T1**
- [ ] `library/src/test/kotlin/com/mordred/aero/internal/windows/` — new test package — **22-13-T1** (ROADMAP Step 10 places the headless tests here; every earlier step is gated by the existing 541 tests + the live harness)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Early gate: hover max → Snap Layouts flyout; drag title to edge → snaps | VER-12 (early), SNAP-01/02 | needs real OS pointer input; OS draws the flyout | 22-04-T2 warn + "ok" (checkpoint:human-action), 22-04-T3 scripted ≤ 2 min, flyout recorded via UIA against a Notepad positive control (D-04); fail → stop and ask |
| Corner / shadow look | WIN-03, C6 | compositor effect invisible to PrintWindow; the look is the maintainer's choice | 22-06-T2: real window + Visual Companion ВАРИАНТ A/B/C; choice applied and read back in 22-06-T3 |
| Full snap / hotkeys / Alt+Space / double-click / shared border / Snap Groups / FancyZones / cross-monitor DPI / auto-hide + visible taskbar | VER-12 (full), SNAP-01..07, WIN-01..06, BTN-01..02 | real OS input; virtual 150 % display + PowerToys installed temporarily | 22-15-T2 consent + "ok"; 22-15-T3 two passes (JDK 21 `run`, JBR 21 `hotRun`); 22-16-T1/T2 teardown on UAC + the maintainer's PowerToys word |
| Agent self-inspection before hand-off | VER-13 | visual judgement of `PrintWindow` frames | 22-16-T3: agent reviews every frame first; hand-off = frames + VER-11 + VER-12 + unconfirmed list (incl. Windows 10) |
| Release decision | REL-08 | outward-facing | 22-18-T2 (`hold` / `release-tag-only` / `release-full`) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency ≤ one full-suite run
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** planned 2026-09-25 (execution fills Status)
