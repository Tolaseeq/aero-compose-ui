---
phase: 22-native-window-behavior-release-3-2-0
verified: 2026-09-28T10:57:01Z
status: gaps_found
score: 15/23 must-haves verified
has_blocking_gaps: true
overrides_applied: 0
gaps:
  - truth: "Caption double-click on the drag area maximizes the window; a second double-click restores it (SNAP-04)"
    status: failed
    severity: blocking
    reason: "Real double-click never maximizes on either JVM; no code path restores WM_NCLBUTTONDBLCLK-at-HTCAPTION to DefWindowProc (the swallow logic exists only for the HTMAXBUTTON button)."
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt"
        issue: "WM_NCLBUTTONDBLCLK handled only in the HTMAXBUTTON branch; caption double-click not routed to DefWindowProc"
    missing:
      - "Forward (or answer) the caption double-click so DefWindowProc performs SC_MAXIMIZE/SC_RESTORE"
      - "Re-run S04-DBLCLICK-MAX green on JDK 21 and JBR 21"
    evidence: "22-SESSION.md both pass tables (S04-DBLCLICK-MAX FAIL); 22-HANDOFF.md blocker #1"
  - truth: "Alt+Space opens the system menu and its commands work; Close goes the same path as Alt+F4 (SNAP-05)"
    status: failed
    severity: blocking
    reason: "Menu never opens on the native window on either JVM (opt-out window proves the comparison path). Conflict C2 settled: the subclass must hand-declare GetSystemMenu/TrackPopupMenu (absent from jna-platform 5.19.1). Alt+F4 half is proven."
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/internal/windows/"
        issue: "No GetSystemMenu/TrackPopupMenu declarations or WM_SYSCOMMAND trigger anywhere in :library (grep-verified)"
    missing:
      - "Hand-declared GetSystemMenu/TrackPopupMenu + Alt+Space trigger in the subclass"
      - "Re-run the S05 family green on both JVMs"
    evidence: "22-SESSION.md S05-* FAIL both JVMs, C02-OPTOUT-ALTSPACE PASS; 22-HANDOFF.md blocker #2, C2 settled answer"
  - truth: "Dragging the shared border of two half-snapped windows resizes both (SNAP-06 border clause)"
    status: failed
    severity: blocking
    reason: "Shared-border drag resizes nothing on either JVM although both windows snap correctly first; cause unexplained by the recorded evidence."
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/internal/windows/AeroWndProc.kt"
        issue: "Behavior gap — no identified code defect yet; needs diagnosis during gap closure"
    missing:
      - "Diagnose and fix shared-border resize; re-run S06-SHARED-BORDER green on both JVMs"
    evidence: "22-SESSION.md S06-SHARED-BORDER FAIL both JVMs; 22-HANDOFF.md blocker #3"
  - truth: "Maximize button shows hover AND press states like minimize/close (BTN-01 press clause)"
    status: failed
    severity: blocking
    reason: "Hover parity proven at 0 px and real clicks toggle correctly, but the press fill differs from minimize's (strip diff 108 px, maxDelta 176) on both JVMs — exactly conflict C5's named fallback condition (FlatLaf-style re-injection of non-client mouse messages)."
    artifacts:
      - path: "library/src/main/kotlin/com/mordred/aero/internal/windows/AeroMaxButtonInteraction.kt"
        issue: "Press interaction bridged but press visual parity not achieved; C5 fallback (message re-injection) not implemented"
    missing:
      - "Implement the C5 fallback path for the press state; re-run B01-PRESS-FRAME green on both JVMs"
    evidence: "22-SESSION.md B01-PRESS-FRAME FAIL both JVMs; 22-HANDOFF.md blocker #4, C5 settled answer"
  - truth: "The full real-input session is green on BOTH JVMs — standard JDK 21 and JBR 21 (SC4 / JBR parity)"
    status: failed
    severity: blocking
    reason: "JBR-only drift in the single recorded run: caption drags (S01 x4) and Win+arrow chords (S03 x3) inert, floating glyph crop unstable (W05 616 px vs JDK 0). All green on standard JDK 21. Single run per JVM; cause unexplained — gap-closure material, not yet a confirmed JBR defect profile."
    artifacts:
      - path: ".captures/22-session/jbr/results.json"
        issue: "Recorded JBR pass with the drift rows"
    missing:
      - "Re-run or diagnose the JBR pass; explain or fix the caption-drag/hotkey inertness and glyph-crop instability"
    evidence: "22-SESSION.md § Differences between passes; 22-HANDOFF.md blocker #5; 22-UNCONFIRMED.md § 12"
  - truth: "Session check W02-CORNER-* rows read GREEN (corner resize verdicts)"
    status: partial
    severity: minor
    reason: "Behavior PROVEN by observed values (correct cursor shapes + 60x60 diagonal growth, both JVMs) but the check formula forbids the second axis for corner drags — 4 FAIL rows per JVM stay red until the formula is fixed and re-run."
    artifacts:
      - path: "tools/winprobe/Invoke-FullSession.ps1"
        issue: "Corner-drag expectation formula rejects legitimate two-axis growth"
    missing:
      - "Fix the corner formula; re-run GREEN"
    evidence: "22-SESSION.md W02-CORNER-TL/TR/BL/BR FAIL rows with observed shapes; 22-UNCONFIRMED.md § 15"
  - truth: "Session check W02-MAIN/NARROW-FLOOR rows read GREEN (minimum-size verdicts)"
    status: partial
    severity: minor
    reason: "Width clamped at exactly 320 (library) / 260 (app, D-01) — the floor works; the check also expects a height change that a left-edge drag cannot produce."
    artifacts:
      - path: "tools/winprobe/Invoke-FullSession.ps1"
        issue: "Floor check expects height delta unreachable from a left-edge drag"
    missing:
      - "Fix the floor expectation; re-run GREEN"
    evidence: "22-SESSION.md W02-MAIN-FLOOR / W02-NARROW-FLOOR rows; 22-UNCONFIRMED.md § 15"
  - truth: "Session check S07-FANCYZONES verdict matches the proven behavior"
    status: partial
    severity: minor
    reason: "On JBR the Shift-drag landed at 16,16,1432,1016 — exactly a real priority-grid zone (work area inset by the layout's 16 px spacing); the check's 2x2-grid heuristic does not model priority-grid geometry. JDK attempt started without foreground (recorded)."
    artifacts:
      - path: "tools/winprobe/Invoke-FullSession.ps1"
        issue: "FancyZones zone expectation hardcodes 2x2-grid geometry and ignores the applied layout's spacing"
    missing:
      - "Derive the expected zone from applied-layouts.json; re-run GREEN on JDK (JBR behavior already proven)"
    evidence: "22-SESSION.md S07 rows; 22-HANDOFF.md SNAP-07 row; 22-UNCONFIRMED.md § 15"
  - truth: "Session check W06-CLOSE-DURING-DRAG row reads GREEN (close-during-drag verdict)"
    status: partial
    severity: minor
    reason: "Behavior proven (closeEvent=True, main alive, v11 fail=0, no new hs_err) but the check requires an ncdestroy trace line within a 5 s window it does not reliably observe."
    artifacts:
      - path: "tools/winprobe/Invoke-FullSession.ps1"
        issue: "ncdestroy-trace observation window too strict"
    missing:
      - "Fix the trace window (or poll the trace file); re-run GREEN"
    evidence: "22-SESSION.md W06-CLOSE-DURING-DRAG rows; 22-UNCONFIRMED.md § 15"
  - truth: "Session check S03-WIN-DOWN row reads GREEN (Win+Down verdict)"
    status: partial
    severity: minor
    reason: "Ordering artifact: the check's own Win+Up landed after a restore, so zoomedAfterUp=False while the standalone S03-WIN-UP passed seconds earlier."
    artifacts:
      - path: "tools/winprobe/Invoke-FullSession.ps1"
        issue: "Win+Down check's internal Win+Up precondition races the restore step"
    missing:
      - "Fix the check ordering; re-run GREEN"
    evidence: "22-SESSION.md S03-WIN-DOWN rows; 22-UNCONFIRMED.md § 15"
deferred:
  - truth: "v3.2.0 shipped on JitPack (REL-08) and DEP-01 proven on the published artifact"
    addressed_in: "Phase 22, Plan 22-18 (unexecuted by maintainer decision 2026-09-28: close session-found gaps BEFORE the release)"
    evidence: "build.gradle.kts still version = \"3.1.0\" (grep-verified); git tag list ends at v3.1.0/v3.1.0-verify01 — no v3.2.0 tag exists; 22-18-PLAN.md defines the verify-tag-then-real-tag release. Pending-by-plan, not missing work."
human_verification:
  - test: "Pick a layout in the Snap Layouts flyout with the mouse"
    expected: "The window is placed into the chosen layout region (SNAP-02 pick clause)"
    why_human: "UIA cannot see zone elements inside the flyout; the scripted check is structurally blind (22-UNCONFIRMED.md § 12)"
  - test: "Hover the taskbar button of a half-snapped pair until the group thumbnail appears, then click it"
    expected: "Snap Groups thumbnail shows both windows and restores the pair (SNAP-06 group clause)"
    why_human: "No TaskListThumbnailWnd appeared within the 2 s scripted hover window; compositor shell feature not probe-observable"
  - test: "Drag the window between a 100% and a real 150% monitor (and Win+Shift+arrow, and maximize on 150%)"
    expected: "No size jumps; caption and buttons at correct scale; hit zones match what is drawn (WIN-04)"
    why_human: "The virtual 150% display never attached on this machine — needs real hardware"
---

# Phase 22: Native Window Behavior + Release 3.2.0 — Verification Report

**Phase Goal (ROADMAP.md):** Windows treats windows built on `AeroTitleBar` (+ `AeroResizeHandles`) as native windows — edge/corner Aero Snap, Snap Layouts flyout, Win+arrow hotkeys, shared-border resize of a snapped pair, taskbar-aware maximize, correct behavior across 100%/150% DPI monitors, FancyZones — in every application built on the library; verified live on Windows 11 on a standard JDK 21 and JBR 21; shipped as `v3.2.0` on JitPack.
**Verified:** 2026-09-28T10:57:01Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

Derived from ROADMAP Success Criteria 1-6 and the goal sentence; behavior evidence is the
22-15 real-input session (both JVMs), the 22-16 final VER-11 run, and codebase inspection.

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Early real-input gate: flyout appears + edge snap works on the first draft (SC1) | VERIFIED | 22-04 `GATE PASS` (third run, DefWindowProc control AND Compose window); 22-HANDOFF.md VER-12 row |
| 2 | Edge/corner snap + drag-away restore, incl. narrow window and several windows (SNAP-01) | VERIFIED | S01-LEFT/RIGHT/QUARTER/TOP/DRAG-AWAY-RESTORE all PASS on JDK 21 (exact halves/quarter of rcWork, restore 1200x800); JBR drift carried as truth #17 |
| 3 | Snap Layouts flyout on maximize-button hover (SNAP-02 flyout clause) | VERIFIED | S02-FLYOUT PASS both JVMs (UIA/event signature on explorer's XamlExplorerHostIslandWindow, ~610-720 ms); layout-PICK unconfirmed → human item (UIA blind to flyout zones) |
| 4 | Win+arrow hotkeys behave natively (SNAP-03) | VERIFIED | S03-WIN-LEFT/RIGHT/UP PASS on JDK (half-screen rects, placement=Maximized); WIN-DOWN FAIL is an ordering artifact (minor gap M5); JBR drift = truth #17 |
| 5 | Caption double-click maximizes/restores (SNAP-04) | FAILED | S04-DBLCLICK-MAX FAIL on JDK 21 AND JBR 21 — never maximizes; no caption-dblclk forwarding in AeroWndProc (grep) |
| 6 | Alt+Space system menu opens; commands work; Close == Alt+F4 path (SNAP-05) | FAILED | S05-* FAIL both JVMs (menu never opens on the native window); Alt+F4 half PROVEN both; opt-out window opens it (C02 PASS) — C2 settled, fix = hand-declared GetSystemMenu/TrackPopupMenu |
| 7 | Shared-border resize of a snapped pair (SNAP-06 border clause) | FAILED | S06-SHARED-BORDER FAIL both JVMs (mainRightDelta=0 narrowLeftDelta=0 though both snapped first); unexplained |
| 8 | Snapped pair visible as a Snap Group in the taskbar (SNAP-06 group clause) | UNCERTAIN (unverifiable_runtime) | S06-SNAP-GROUP UNCONFIRMED both passes — no TaskListThumbnailWnd within the 2 s hover; needs human observation |
| 9 | FancyZones snaps the window by zone (SNAP-07) | VERIFIED | JBR Shift-drag landed at 16,16,1432,1016 — exactly a real priority-grid zone read from PowerToys applied-layouts.json; FAIL verdict is the check's 2x2 heuristic (minor gap M3); JDK attempt lacked foreground (recorded) |
| 10 | Taskbar-aware maximize incl. auto-hide edge (WIN-01) | VERIFIED | W01-MAX-VISIBLE-TASKBAR + W01-AUTOHIDE-REVEAL PASS both JVMs (client == rcWork; reveal ~232 ms over the 2 px inset); headless V11-MAX-WORKAREA/AUTOHIDE-EDGE PASS |
| 11 | Native resize on every edge/corner incl. narrow window + min floors (WIN-02) | VERIFIED | All four edges PASS both JVMs (system cursors + 60 px single-axis); corners: correct shapes + 60x60 (formula artifacts — minor gaps M1/M2); floors clamp at exactly 320/260 |
| 12 | No system-frame traces; corner/shadow look per maintainer choice (WIN-03) | VERIFIED | W03-FRAMES PASS both (floating title band 0 px vs pre-phase baseline); W03-CORNERS PASS (152/150 DWMWCP_DONOTROUND read-backs); Win32Dwm.kt implements the policy; 22-16 final FULL-FRAME 0 px, all three schemes |
| 13 | 100% <-> 150% DPI moves keep sane size (WIN-04) | UNCERTAIN (unverifiable_runtime) | W04 trio UNCONFIRMED on both passes — the virtual 150% display never attached on this machine (contingency); needs real hardware |
| 14 | Maximize/restore glyph + WindowState.placement match real state (WIN-05) | VERIFIED | W05-PLACEMENT 17/17 samples both JVMs; glyph switch proven (616 px crop diff); JBR floating-crop instability folded into truth #17 |
| 15 | Multiple windows independent, narrow + interactive header, close-safe (WIN-06) | VERIFIED | W06-INDEPENDENT + W06-OPEN-WHILE-DRAGGING PASS both; W06-CLOSE-DURING-DRAG behavior proven (closeEvent=True, main alive, v11 fail=0) — FAIL is the 5 s trace window (minor gap M4) |
| 16 | Maximize button parity: hover + press + click like minimize (BTN-01) | FAILED | Hover parity 0 px both JVMs, real click toggles Maximized/Floating both — but B01-PRESS-FRAME FAIL both (strip diff 108 px): press fill differs; C5's named fallback condition |
| 17 | Full session green on BOTH JDK 21 and JBR 21 (SC4 / JBR parity) | FAILED | JBR-only drift in the single run: S01 x4 caption drags + S03 x3 hotkeys inert, W05 floating crop unstable (616 px vs JDK 0); all green on standard JDK 21; cause unexplained — gap-closure material |
| 18 | Minimize/close/leading/marked clicks are ordinary clicks, no drag (BTN-02) | VERIFIED | B02-MIN/LEADING/MARKED/CLOSE-CLICK all PASS both JVMs |
| 19 | Public API additive + opt-out + custom title bar, source-compatible, no JNA in signatures (API-01..04) | VERIFIED | Code read: AeroWindowChrome.kt real API (markAeroTitleBarInteractive, rememberAeroWindowChrome + captionArea/captionExclude/maximizeButtonArea, inert paths); AeroTitleBar.kt:100 built on it; showcase compiles byte-unchanged (d85bc86); PublicApiNoJnaTest; API-03 permanent RED control re-proven final 22-16 |
| 20 | Non-Windows behaves as before | VERIFIED | ResizeHandles gates on active native chrome (Windows no-op path documented in-file); legacy WindowDraggableArea branch only; locked suite compiles both paths. Runtime on Linux/macOS unconfirmed (no machine) — 22-UNCONFIRMED.md § 4 |
| 21 | Verification infrastructure honest and reproducible (VER-11..14) | VERIFIED | Final GREEN 22-16: main 18/18, narrow 8/8 (`v11-green-console.log` re-read this verification: `V11 SUMMARY pass=18 fail=0`, `narrow pass=8 fail=0`); RED counterparts (Plan-01 baseline 0/18, opt-out 0/18+0/8); VER-12 both passes with consent + verified teardown; VER-13 handoff + unconfirmed list; VER-14 lockedTestTotal=592 in library/build.gradle.kts with count guard, raises in named commits |
| 22 | README + KDoc updated, "Aero Snap limitation" removed (REL-06/07) | VERIFIED | README § "Windows window behavior" exists; grep "Aero Snap limitation" over README + library sources: zero matches; KDoc on AeroTitleBar/AeroResizeHandles/AeroWindowChrome present |
| 23 | v3.2.0 published and resolving on JitPack (REL-08, SC6) | PENDING-BY-PLAN | build.gradle.kts `version = "3.1.0"`; git tags end at v3.1.0-verify01 — no v3.2.0 tag. Plan 22-18 deliberately unexecuted: maintainer decision 2026-09-28 to close gaps before release. Deferred, not counted as a gap |

**Score:** 15/23 truths verified (5 FAILED, 2 UNCERTAIN-runtime, 1 pending-by-plan)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | REL-08 release (version 3.2.0, verify tag, real tag, JitPack ok) + DEP-01 on the published POM | Phase 22, Plan 22-18 (unexecuted by maintainer decision 2026-09-28 — gaps close first) | 22-18-PLAN.md frontmatter requirements [REL-08, DEP-01]; build.gradle.kts still 3.1.0; no tag |

Pending-by-plan note: REL-08 and the DEP-01 artifact-level clauses are sequenced AFTER gap
closure by the maintainer's explicit decision. They are not missing work and not counted in
`gaps`, but the phase cannot reach `passed` while they remain open — the goal sentence ends
with "Shipped as v3.2.0 on JitPack".

### Required Artifacts (three levels + data flow)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/.../internal/windows/AeroWndProc.kt` | WndProc subclass: owned messages, passthrough | VERIFIED | 426 lines; WM_NCCALCSIZE/WM_NCHITTEST/NC-button branches; `CallWindowProc(previous,...)` fallback structural invariant; HTMAXBUTTON hover bridge + button-down swallow |
| `library/.../internal/windows/NativeWindowChromeRegistry.kt` | GC-safe install/release, multi-window | VERIFIED | 396 lines; acquire/release handle API used by AeroWindowChrome.kt:117 |
| `library/.../internal/windows/HitTestRegionRegistry.kt` | Live region snapshot | VERIFIED | 120 lines; publishCaption/publishInteractive/publishRole consumed by WndProc hit-test |
| `library/.../internal/windows/Win32Dwm.kt` | Corner policy (C6) | VERIFIED | 111 lines; DWMWA_WINDOW_CORNER_PREFERENCE=33, DWMWCP_* constants, SQUARE_NO_SHADOW chosen look |
| `library/.../internal/windows/Win32Geometry.kt`, `MinimumSize.kt`, `HitTestClassification.kt`, `WndProcSupport.kt`, `Win32Interop.kt`, `Win32Chrome.kt`, `NativeChromeStatus.kt` | Supporting mechanics | VERIFIED | All present, substantive (35-156 lines each), no debt markers |
| `library/.../internal/windows/AeroMaxButtonInteraction.kt` | Max-button interaction bridge (C5) | VERIFIED (partial-behavior) | 111 lines, EDT-hop into MutableInteractionSource; press parity gap B4 is behavioral, not a stub |
| `library/.../components/navigation/AeroWindowChrome.kt` | Public API-02/API-04 | VERIFIED | markAeroTitleBarInteractive + rememberAeroWindowChrome + AeroWindowChromeState with captionArea/captionExclude/maximizeButtonArea and inert fallbacks |
| `library/.../components/navigation/AeroTitleBar.kt` | Built on the public chrome API | VERIFIED | Line 100 `rememberAeroWindowChrome(windowState, nativeWindowManagement)`; captionArea/captionExclude/maximizeButtonArea wiring at lines 164/210 |
| `library/.../components/navigation/ResizeHandles.kt` | Windows no-op when native chrome active | VERIFIED | Gated on floating-placement AND no-active-native-chrome; pointerInput paths remain for non-Windows/legacy |
| Test layer (22-13) | Headless tests for the new mechanics | VERIFIED | internal/windows: HitTestClassificationTest(13), HitTestRegionRegistryTest(3), MinimumSizeTest(5), Win32GeometryTest(7), WndProcSupportTest(4); navigation: AeroWindowChromeStateTest(4), TitleBarButtonParityTest(9), ResizeHandlesGateTest(4); AeroTitleBarTest |
| `tools/winprobe/*` | Live-window probe + session driver | VERIFIED | 9 scripts incl. WinProbe.ps1, Invoke-WinProbe.ps1, Invoke-FullSession.ps1, Invoke-EarlyGate.ps1 |
| `showcase/.../Main.kt` narrow window | SHW-17 fixture | VERIFIED | `-Daero.secondWindow` deterministic open + reopen delay; exercised by every session pass |
| `gradle/libs.versions.toml` + `library/build.gradle.kts` | JNA 5.19.1 implementation scope (DEP-01) | VERIFIED | jna = "5.19.1"; `implementation(libs.jna)` / `implementation(libs.jna.platform)` with the not-in-public-signatures comment |
| `build.gradle.kts` version | `version = "3.2.0"` (REL-08) | NOT DONE (deferred) | Currently `3.1.0` — Plan 22-18 pending-by-plan |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| AeroTitleBar composition | rememberAeroWindowChrome | direct call (AeroTitleBar.kt:100) | WIRED |
| rememberAeroWindowChrome | NativeWindowChromeRegistry.acquire/release | DisposableEffect (AeroWindowChrome.kt:116-121) | WIRED |
| markAeroTitleBarInteractive / caption members | HitTestRegionRegistry | onGloballyPositioned publish + onDispose remove | WIRED |
| AeroWndProc WM_NCHITTEST | HitTestRegionRegistry snapshot | live region read per message | WIRED (proven at runtime by VER-11 HT rows) |
| AeroWndProc HTMAXBUTTON | AeroMaxButtonInteraction | NC hover/press bridge (EDT hop) | WIRED (press-parity gap B4 is visual, not wiring) |
| Session driver | live window + results.json | Invoke-FullSession.ps1 | WIRED (recorded runs + captures present) |

### Data-Flow Trace (Level 4)

The "data" of this phase is the hit-test region flow Compose layout -> registry -> WndProc:

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| AeroWndProc WM_NCHITTEST answer | region snapshot | Compose onGloballyPositioned boundsInWindow | Yes — VER-11 GREEN 18/18 proves the end-to-end answer at runtime (caption/min/max/close/leading/marked/edges/corners/client/boundary rows) | FLOWING |
| Max-button hover/press state | MutableInteractionSource | native NC messages via AeroMaxButtonInteraction | Yes — hover parity 0 px, click toggles placement (both JVMs) | FLOWING (press fill gap = B4) |

### Behavioral Spot-Checks (static, this verification)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| VER-11 final GREEN readable | grep `V11 SUMMARY` .captures/22-final/v11-green-console.log | `pass=18 fail=0`, `narrow pass=8 fail=0` | PASS |
| Test layer enumerates | grep -c `@Test` on the 8 new test files | 49 test functions present | PASS |
| Corner policy implemented | grep DWMWA/DWMWCP Win32Dwm.kt | constants + 3 looks + applied choice | PASS |
| "Aero Snap limitation" removed | grep over README + library sources | zero matches | PASS |
| No premature release tag | `git tag` | ends at v3.1.0-verify01 | PASS (consistent with 22-18 held) |
| Debt markers in phase code | grep TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER | zero matches in internal/windows + navigation chrome files | PASS |
| Full `:library:test` run | not run by verifier | — | SKIP — Gradle run exceeds the spot-check budget; recorded evidence: 22-15 readiness full `--rerun` BUILD SUCCESSFUL with `AERO_TEST_COUNT total=592` and the locked-count guard in library/build.gradle.kts |

### Probe Execution

| Probe | Command | Result | Status |
|-------|---------|--------|--------|
| tools/winprobe/* (live-window probes) | not run | — | SKIP — probes require a live GUI window, a real display and cursor/keyboard injection; running them here would violate the no-side-effects contract. Recorded results re-read instead: `.captures/22-final/v11-green-console.log` (GREEN) + `.captures/22-session/{jdk,jbr}/results.json` (session, per-pass totals 31/54 and 24/54) |

### Requirements Coverage (28 IDs)

Union of `requirements:` across all 19 PLANs equals exactly the 28 IDs below — no orphans,
none unclaimed.

| Requirement | Source Plan(s) | Description (abbr.) | Status | Evidence |
|-------------|----------------|---------------------|--------|----------|
| SNAP-01 | 22-15 | Edge/corner/top snap + drag-away restore | SATISFIED on JDK 21; JBR drift = gap B5 | S01 x5 PASS JDK; JBR FAIL (drift) |
| SNAP-02 | 22-03/04/15 | Flyout on hover; layout pick places window | PARTIAL — flyout SATISFIED both JVMs; pick NEEDS HUMAN (UIA blind) | S02-FLYOUT PASS; S02-LAYOUT-PICK UNCONFIRMED |
| SNAP-03 | 22-15 | Win+arrows | SATISFIED on JDK (WIN-DOWN = minor M5); JBR drift = gap B5 | S03 rows |
| SNAP-04 | 22-15 | Caption double-click maximize/restore | BLOCKED — gap B1 | FAIL both JVMs |
| SNAP-05 | 22-15 | Alt+Space menu; commands; Close == Alt+F4 | BLOCKED — gap B2 (Alt+F4 half proven) | S05 family FAIL both |
| SNAP-06 | 22-15 | Shared-border resize; Snap Groups | BLOCKED (border, gap B3); group NEEDS HUMAN | S06 rows |
| SNAP-07 | 22-15 | FancyZones zone snap | SATISFIED (verdict = minor M3) | JBR rect = real priority-grid zone |
| WIN-01 | 22-05/15 | Taskbar-aware maximize incl. auto-hide | SATISFIED | W01 both JVMs + headless V11 |
| WIN-02 | 22-11/15 | Native edge/corner resize; Windows no-op; floors | SATISFIED (formula fixes = minor M1/M2) | W02 observed values |
| WIN-03 | 22-05/06/15 | No frame traces; corner look per maintainer | SATISFIED | W03 both; 22-16 full-frame 0 px |
| WIN-04 | 22-15 | 100% <-> 150% DPI moves | NEEDS HUMAN (environment) — no 150% display existed | W04 trio UNCONFIRMED |
| WIN-05 | 22-10/15 | Glyph + placement match real state | SATISFIED on JDK; JBR crop = part of gap B5 | W05 17/17 both |
| WIN-06 | 22-08/12/15 | Multi-window independence | SATISFIED | W06 (+ trace-window fix = minor M4) |
| BTN-01 | 22-09/15 | Max button hover/press/click parity | BLOCKED (press clause) — gap B4; hover+click SATISFIED | B01 rows |
| BTN-02 | 22-07/11/15 | Min/close/leading/marked clicks | SATISFIED | B02 x4 both JVMs |
| API-01 | 22-07/12 | Source compatibility; no JNA in signatures | SATISFIED | d85bc86; PublicApiNoJnaTest |
| API-02 | 22-12 | markAeroTitleBarInteractive | SATISFIED | RED->PASS + real marked-click both JVMs |
| API-03 | 22-12 | Opt-out restores legacy behavior | SATISFIED | permanent RED control, re-proven 22-16 |
| API-04 | 22-19 | rememberAeroWindowChrome for custom title bars | SATISFIED (live through AeroTitleBar's layout; other shapes headless-only — 22-UNCONFIRMED.md § 13) | 22-19 + API-04 attribution line in both pass headers |
| DEP-01 | 22-02 (+18) | JNA 5.19.1 implementation scope, no conflict | SATISFIED at source level; artifact-level clauses deferred with 22-18 | libs.versions.toml + build.gradle.kts grep |
| SHW-17 | 22-08 | Narrow ~300 px showcase window | SATISFIED | Main.kt fixture + every session pass |
| VER-11 | 22-01/12/16 | Headless-live checks with RED lineage | SATISFIED | 18/18 + 8/8 GREEN, RED counterparts |
| VER-12 | 22-03/15 | Two supervised real-input sessions, both JVMs; teardown | SATISFIED (sessions ran per protocol; their FINDINGS are the gaps above) | consent timestamps; verified teardown |
| VER-13 | 22-16 | Agent-first frame review + honest handoff | SATISFIED | 22-HANDOFF.md + 22-UNCONFIRMED.md |
| VER-14 | 22-13 | Tests green under locked count | SATISFIED | lockedTestTotal=592 + guard; readiness build |
| REL-06 | 22-17 | README window-behavior section | SATISFIED | section verified this run |
| REL-07 | 22-17 | KDoc; caveat removed | SATISFIED | grep zero |
| REL-08 | 22-18 (NOT executed) | v3.2.0 on JitPack | PENDING-BY-PLAN (deferred) | version 3.1.0; no tag |

REQUIREMENTS.md checkbox note: WIN-01/02/03/05, SNAP-07 and BTN-02 still read `Pending`
there although the session evidence proves them; SNAP-02/06 keep Pending honestly (unconfirmed
sub-clauses). The checkbox flips presumably wait for the clean re-verification — flagged as
information, not a gap.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none in phase code) | — | No TBD/FIXME/XXX/TODO/HACK/PLACEHOLDER markers in internal/windows or the chrome/navigation files | — | — |
| repo root (untracked) | — | `hs_err_pid*.log`, `replay_pid*.log`, `showcase/hs_err_pid33744.log` — JVM crash residue from session runs, not committed | Info | Add to .gitignore or delete; no code impact |

### Human Verification Required

1. **Snap Layouts layout pick** — hover the maximize button, let the flyout open, click a zone.
   Expected: the window lands in that layout region (SNAP-02 pick clause).
   Why human: UIA cannot see zone elements inside the flyout; the scripted check is structurally blind.
2. **Snap Groups thumbnail** — half-snap two windows, hover their taskbar button past 2 s.
   Expected: a group thumbnail showing both; clicking restores the pair (SNAP-06 group clause).
   Why human: no TaskListThumbnailWnd observed within the scripted window; shell feature.
3. **WIN-04 on real hardware** — drag/Win+Shift+arrow/maximize between a 100% and a real 150% monitor.
   Expected: no size jumps, correct scale, hit zones match the drawing.
   Why human: the virtual 150% display never attached on this machine.
4. **Windows 10 spot-check** (no machine exists) and **non-Windows runtime** (Linux/macOS) —
   named in 22-UNCONFIRMED.md § 1/§ 4, not presented as passed anywhere.
5. **Visual corner/shadow look and minimize animation** — compositor effects invisible to
   PrintWindow; maintainer-observed only (22-UNCONFIRMED.md § 5/§ 6).

### Gaps Summary

The phase is functionally far along and structurally clean: the WndProc mechanics, region
registry, public API, test layer, probe tooling, docs and the entire RED/GREEN verification
discipline all exist, are wired, and are proven at runtime on the primary JDK 21 path
(VER-11 18/18 + 8/8, edge snaps, flyout, hotkeys, FancyZones, taskbar maximize, frame parity,
multi-window). Five real library gaps block the goal sentence and hold the release
(22-HANDOFF.md blockers #1-#5): SNAP-04 caption double-click, SNAP-05 Alt+Space system menu
(known fix path: hand-declared GetSystemMenu/TrackPopupMenu), SNAP-06 shared-border resize,
BTN-01 press-fill parity (C5 fallback), and the JBR-pass drift. Five check-formula fixes are
minor but needed so the re-verification reads green where behavior is already proven
(corner axis, floor height, FancyZones priority-grid geometry, ncdestroy trace window,
Win+Down ordering). REL-08 (the v3.2.0 release itself) is pending-by-plan behind gap closure
per the maintainer's 2026-09-28 decision — deferred, not a gap, but the phase cannot be
`passed` until it lands.

---

_Verified: 2026-09-28T10:57:01Z_
_Verifier: Claude (gsd-verifier)_
