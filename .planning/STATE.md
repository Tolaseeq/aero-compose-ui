---
gsd_state_version: 1.0
milestone: v3.2
milestone_name: Native Window Behavior
status: executing
stopped_at: Phase 22 Plan 09 complete -- maximize-button interaction bridge; NC click toggles placement; conflict #5 settled (BTN-01, D-02)
last_updated: "2026-09-27T14:53:29.241Z"
last_activity: 2026-09-25 -- Phase 22 execution started
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 19
  completed_plans: 9
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-25 — v3.2 roadmap created)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** Phase 22 — Native Window Behavior + Release 3.2.0

## Current Position

Phase: 22 (Native Window Behavior + Release 3.2.0) — EXECUTING
Plan: 10 of 19
Status: Executing Phase 22
Last activity: 2026-09-25 -- Phase 22 execution started

## Deferred Items

Items acknowledged and deferred at milestone close on 2026-07-29 (v3.0). Closeout type: `override_closeout` — 9 known verification overrides. Explicitly out of v3.1 scope (see PROJECT.md "Явно НЕ в scope"). Re-checked at v3.1 close on 2026-09-24: the open-artifact audit reported the same 6 still-open items (5 todos + Phase 10 `human_needed`) and nothing new; carried forward unchanged.

| Category | Item | Status |
|----------|------|--------|
| uat_gap | Phase 18 — `18-UAT.md` | passed, 0 open scenarios (audit heuristic false positive) |
| uat_gap | Phase 19 — `19-UAT.md` | passed, 0 open scenarios (audit heuristic false positive) |
| verification_gap | Phase 10 — `10-VERIFICATION.md` | human_needed — v2.0-era carry-over, outside v3.0 scope |
| quick_task | `260624-k4d-aerocombobox-ontextchange-label-onoption` | unknown — filed 2026-06-24, v2.0.2 era |
| todo (ui) | `2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor` | pending |
| todo (ui) | `2026-07-29-aeroblue-aerodark-opaque-fill-label-below-wcag-floor-white-decision` | pending |
| todo (ui) | `2026-07-29-aeroradiobutton-hover-shadow-square-not-round` | pending |
| todo (build) | `2026-07-29-aerotheme-establishbackground-abi-compatibility` | pending |
| todo (testing) | `2026-07-29-contrast-regression-test-omits-outlined-and-segment-disabled-fills` | pending |

Beyond the audit list, one requirement shipped with a recorded gap rather than a pass: **SHW-16's 125% / 200% real-OS DPI passes were explicitly waived by the maintainer** ("Принять без DPI-прогонов"). The 100% DPI three-theme coherence pass is PASSED and capture-backed; the other two were never executed. VER-F02 (v3.1 Future Requirements) tracks re-attempting this.

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.
**v2.0:** 55 plans, ~49 days, 145 commits, 152 files changed, +27,406 / −2,285 lines.
**v2.0.1:** 4 plans, single-day push (2026-06-22, ~2h20m), 25 commits, 9 code files changed, +520 / −14 lines.
**v2.0.2:** 8 plans (Phases 13 + 13.1), ~1-day push (2026-06-22→23), 49 commits, 4 code files, +1,516 lines.
**v2.0.4:** 3 plans, single-day push incl. corrective release (2026-06-25→26), real RCMP root-cause fix.
**v3.0:** 41 plans / 85 tasks across 6 phases, 2026-07-22 → 2026-07-29 (8 days), 252 commits, 248 files changed (+43,770 / −2,638) of which 71 code files (+11,104 / −409). Tests 232 → 467. Per-plan durations ranged 3 min – 2h10m; the long tail was human visual sign-off rounds, not code. Per-plan metrics are archived with the phase artifacts in `.planning/milestones/v3.0-phases/`.
**v3.1:** 14 plans / 35 tasks in 1 phase (21), 2026-09-21 → 2026-09-24 (execution 09-23 → 09-24), 65 commits, 91 files changed (+17,896 / −1,385) of which 31 non-planning files (+4,155 / −122). Tests 467 → 541. The long tail was the capture sweeps and the drift review, not code: the only source edit the upgrade forced was four `Clock` imports. Per-plan metrics are archived with the phase artifacts in `.planning/milestones/v3.1-phases/`.
**v3.2:** in progress — 1 phase (22), 19 plans roadmapped. Plan 01 (live-window probe harness + RED baseline), 2026-09-25, 19 min, 3 tasks + 1 fix commit, 6 files (4 created, 2 modified). Plan 02 (JNA dependency + native WndProc subclass spike), 2026-09-25, 35 min, 3 tasks, 9 files (5 created, 4 modified). Plan 03 (VER-12 real-input tooling + session environment vetting), 2026-09-25, 9 min, 3 tasks, 4 files created. Plan 08 (SHW-17 narrow second window fixture + probe narrow checks, run ahead of 06/07 per wave 4 parallelization), 2026-09-26, 13 min, 2 tasks, 5 files (1 created, 4 modified). Plan 07 (live hit-test region registry + classifier, AeroTitleBar live-region publishing, single native drag path; 4/4 title V11 checks PASS at two window sizes), 2026-09-27, ~45 min, 3 tasks, 8 files (3 created incl. git-ignored probe script, 5 modified). Plan 09 (maximize-button interaction bridge: native NC mouse messages fed as real Hover/Press interactions into the shared MutableInteractionSource; posted NC click toggles placement Maximized/Floating live; title band 0 px diff vs baseline; conflict #5 settled), 2026-09-27, ~25 min, 3 tasks, 6 files (1 created incl. git-ignored probe script, 5 modified).

## Accumulated Context

### Locked decisions carried forward

Full decision log lives in PROJECT.md "Key Decisions". Rules that constrain any future work:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1); extends to ALL Popup/Dialog
- Glass effect in a single `drawBehind` block — performance baseline
- `detectDragGestures` banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03)
- `AeroScrollArea` banned inside DataTable / TreeView — raw `LazyListState + AeroScrollBar` (PITFALL-01)
- Pattern 3 is the locked answer for "animation vs. drag write the same value" (`AeroPanelGroup` precedent, reused by VRNG-09)
- Builder/DSL lambdas that side-effect into a collection must NOT be `@Composable` (v2.0.4 RCMP root cause)
- A regression guard must provably FAIL on unfixed code before it counts as a guard (v2.0.3 lesson; encoded as TOOL-04/VER-06 in v3.0, and again as TOOL-16 in v3.1)
- **[v3.0]** One `drawAeroSurfaceCore` implementation, many exposure paths — derived primitives are `style.copy()` field swaps, never bespoke gradient code
- **[v3.0]** `aeroGlowRing` must be chained OUTSIDE any clip, before `aeroSurface` — the ordering rule for every component composing both
- **[v3.0]** Base-then-transform state resolution (selected resolves base first, hover composes second) — structurally prevents "selection suppresses hover"
- **[v3.0]** Label colour is a scheme-level property picked by surface polarity (`labelOnFilledSurface` / `labelOnOutlinedSurface`), never computed per call site from an animating fill
- **[v3.0]** focus-visible is derived from the interaction stream, not `LocalInputModeManager` — the platform mechanism only gates `Indication`, which this library disables everywhere
- **[v3.0]** Material3 must be pinned to an explicit stable coordinate — the `compose.material3` alias silently resolves to alpha on CMP 1.11.x (re-checked again after Hot Reload devtools is added in v3.1 Phase 21 — a new leak vector)
- **[v3.1]** Dependency-upgrade risk stays isolated from drawing-code risk — same lesson as v3.0 Phase 15, applied again: each toolchain/dependency bump in Phase 21 is its own commit, gated by a full test run, before the next
- **[v3.1]** `PrintWindow(hwnd, hdc, 2)` is the only sanctioned window-capture method for before/after comparison — the MCP server's own `take_screenshot` tool is a real `java.awt.Robot` screen scrape (confirmed by source read at Hot Reload tag `v1.2.0`) and is banned project-wide: it captured the maintainer's personal browser once already
- **[v3.1]** `.mcp.json` must invoke `cmd /c .\gradlew.bat ...` with the explicit path — bare `gradlew.bat`/`./gradlew` fail on this machine (`NoDefaultCurrentDirectoryInExePath=1`, no POSIX shebang spawn on Windows)
- **[v3.1]** `.mcp.json` must name the module: `:showcase:hotMcpServer`. The bare `hotMcpServer` starts one MCP JVM per module on one shared stdio (random routing, calls hanging to the 1800 s timeout; fixed in `883a8d3`)
- **[v3.1]** `.planning/research/MCP-HOWTO.md` (maintainer-verified, first-hand) wins over the desk-research files (SUMMARY.md / STACK / FEATURES / ARCHITECTURE / PITFALLS) wherever they disagree
- **[v3.1]** BASE-03/BASE-04/BASE-05 pre-upgrade baseline (Phase 21 Plan 04) is captured and named: two independent showcase sweeps (75 frames each, 96 DPI) found only `AeroProgressBar` indeterminate shimmer and `LayoutSection`'s 30fps recompose-drive counter as showcase noise (9/75 keys, all <0.5% frame area); UI-test captures (200 keys) carry zero run-to-run noise. `21-noise-regions.json`/`21-BASELINE.md`/`21-NOISE.md` are the D-05 threshold source for every post-upgrade VER-07/VER-08 comparison
- **[v3.2, pending — not yet empirically confirmed]** Native window behavior lives in the library (`AeroTitleBar`), not in a consumer app — Pinya D-25 needs every library consumer to get snapping; the Pinya executor does not touch this repo
- **[v3.2, pending]** Real mouse/keyboard verification only after the maintainer's warning and "ok": an early 1–2 minute session right after the first draft (does Windows even show the Snap Layouts flyout on a Compose window — the existential risk), and a full session at the end of the phase; the v3.1 "never touch the maintainer's input" default still applies
- **[v3.2, 22-01]** `V11-MAX-WORKAREA` needs a per-edge tolerance (auto-hide edge: 1-4px uncovered; other edges: ≤1px) — a blanket ≤4px-on-every-edge tolerance false-positive-passed on the unmodified window, since CMP's own maximize already fills the monitor exactly when the taskbar auto-hides
- **[v3.2, 22-01]** F9: a probe-driven `SC_MAXIMIZE` never takes the foreground on this machine (measured 3×) — maximize-dependent VER-11 checks run headless without `-SkipMaximize`
- **[v3.2, 22-01]** F8: the native hit-test subclass must attach to the child HWND (`SunAwtCanvas`, Skiko's `HardwareLayer`), not the frame alone — it covers the whole client area and answers `WM_NCHITTEST` first
- **[v3.2, 22-02]** JNA/jna-platform 5.19.1 checksums re-confirmed against the resolved local Gradle cache jars, matching D-03 exactly; `implementation` scope only, zero leakage onto `:showcase`'s compile classpath
- **[v3.2, 22-02]** C1 (after): all five style bits present post-install (`WS_CAPTION`/`WS_SYSMENU`/`WS_THICKFRAME`/`WS_MINIMIZEBOX`/`WS_MAXIMIZEBOX`); `V11-HT-CAPTION` and `V11-HT-MAX` now PASS through the real child-to-frame `HTTRANSPARENT` bounce
- **[v3.2, 22-02]** C4: `WindowState.placement` still syncs `Maximized`/`Floating` with zero explicit push code even with the native subclass live and answering `WM_NCHITTEST`/`WM_NCCALCSIZE` — `CallWindowProc` passthrough for `WM_SIZE`/`WM_SYSCOMMAND` does not disturb AWT's own sync pipeline
- **[v3.2, 22-02]** C7 runtime: cold standard-JDK 21 and hot JBR 21 produce byte-identical style/hit-test/maximize answers; three Hot Reload cycles held exactly one live install with unchanged hit-test answers throughout, no WNDPROC stacking
- **[v3.2, 22-02]** New finding: the spike's unconditional `WM_NCCALCSIZE` → 0 handler produces an 8px maximized overhang past the monitor on every edge — expected, deferred to a later plan, not a regression
- **[v3.2, 22-03]** Every SendInput-capable function in `tools/winprobe/RealInput.ps1` requires an `AeroInputSession` that only a non-empty `-AuthorizedBy` can create; `-DryRun` sessions log the planned action and send nothing — proven live (cursor unchanged before/after both self-tests and the early-gate dry run)
- **[v3.2, 22-03]** Vetted VER-12 full-session candidates, nothing installed: VirtualDrivers/Virtual-Display-Driver 25.7.23 (MttVDD) — Authenticode Valid on installer, driver DLL and catalog (SignPath Foundation via GlobalSign GCC R45 CodeSigning CA 2020), no test-signing needed; PowerToys v0.101.2362.0 `PowerToysUserSetup` (per-user, no UAC) — Authenticode Valid Microsoft Corporation. Full commands/hashes in `22-SESSION-ENV.md`
- **[v3.2, 22-03]** `winget` is absent on this dev machine, confirming 22-RESEARCH.md's own fallback path (GitHub release asset + Authenticode check) for PowerToys vetting
- **[v3.2, 22-08]** Windows honors AWT `minimumSize` natively on the native-chrome path (a probe 50x50 request on the narrow window clamped to exactly the app-set 260x200 with zero library floor code) — Plan 11's default 320x240dp floor must not clobber an app-set minimum (D-01)
- **[v3.2, 22-08]** The unmarked "Вернуть" overlay's `V11-N-HT-MARKED-BOUNDARY` FAIL (`marked=2,captionLeftOfMarked=2`, both HTCAPTION) is the standing RED for API-02; Plan 12's `markAeroTitleBarInteractive()` is what turns it GREEN
- **[v3.2, 22-07]** Hit-testing answers from `AeroTitleBar`'s live layout: copy-on-write `HitTestSnapshot` swapped through `AtomicReference` (the WndProc does one volatile read per `WM_NCHITTEST`, never locks, never touches Compose state); 4/4 title-bar V11 checks PASS at 1200x800 AND after a `SetWindowPos` resize to 900x600; the spike's hardcoded `classifySpikeTitleRow` is deleted; `WindowDraggableArea` remains only on the non-Windows / install-failed path
- **[v3.2, 22-07]** BTN-02's ordinary-click proof stays with Plan 15 real input: posted `WM_LBUTTONDOWN`/`UP` at the `min` point did not reach Compose within 5s (recorded, not faked); the HTCLIENT classification that admits real clicks is proven by `V11-HT-MIN-BOUNDARY`/`V11-HT-CLOSE-BOUNDARY` PASS
- **[v3.2, 22-07]** CMP 1.12.0 turned `LayoutCoordinates.boundsInWindow()` into a deprecated extension forwarding to `boundsInWindow(clipBounds = true)` — region publishing calls the explicit-parameter form (`clipBounds = true` IS the pre-1.12 no-arg semantics)
- **[v3.2, 22-09]** C5 SETTLED: the max-button bridge emits real `HoverInteraction`/`PressInteraction` objects into the `MutableInteractionSource` shared with the unchanged `hoverable` + `clickable` chain (D-02 parity structural, not a matching exercise); native → Compose crosses threads only through `SwingUtilities.invokeLater`, the native side keeps two plain booleans; `TrackMouseEvent(TME_LEAVE|TME_NONCLIENT)` is re-armed on every `WM_NCMOUSEMOVE` over the button; NC down/up/dblclk at HTMAXBUTTON return 0 without `CallWindowProc`; `WM_NCMOUSEMOVE` still forwards (the 22-04 flyout path is unregressed). FlatLaf-style re-injection recorded as not needed unless Plan 15's real-hover frames mismatch
- **[v3.2, 22-09]** Posted `WM_NCLBUTTONDOWN`/`UP` (wParam HTMAXBUTTON, lParam = the `max` point in SCREEN coordinates) toggles the reporter placement Maximized ↔ Floating live through today's `onClick` code path, and like F9's SC_MAXIMIZE never takes the foreground on this machine — BTN-01's hover clause stays unproven until Plan 13's pixel test and Plan 15's real-hover frames (TRACKMOUSEEVENT reports leave instantly while the real cursor is elsewhere, so hover cannot be proven headlessly)
- **[v3.2, 22-09]** jna-platform 5.19.1 genuinely ships no `TRACKMOUSEEVENT` class (verified against the resolved jar) — the hand-declared Structure + `TrackMouseEvent` in `Win32Interop.kt` is required, not precautionary

### Open technical debt

- **G4** — `AeroOrnamentTokens` brightness on AeroBlue/AeroDark: re-confirmed by the reviewer but deferred; the direction is to try darker values for those two themes specifically, in a foundation session
- **IN-01** — `GlassModifiers.kt` clip gap left open deliberately; blast radius is ~40 components
- **WR-01** — scratch/proof files still ship under `showcase/src/main`
- `AeroRangeSlider` has no keyboard focus tracking at all (pre-existing; adding it is new per-thumb keyboard navigation, out of scope for a render-only restyle)
- Label contrast below the WCAG 4.5:1 floor on several surfaces (see Deferred Items)
- **DROP-FIX-01** — `AeroDropdown` popup offset regression, carried since v1.0; root cause in `AeroScrollArea` (`Column.fillMaxSize()` forces 320dp under `heightIn(max=320.dp)`)

### Future requirements (deferred, sourced from the v3.0 + v3.1 requirements archives)

- **v3.1 Phase 21 code review** (`21-REVIEW.md`, 0 critical, tooling/test-side only): WR-01 `-Pages` capture filter still records an unrequested page-0 frame; WR-02 broad `catch (Throwable)` in the UI-test focus-fallback helper; IN-01 duplicated `aero.*` forwarding block in `showcase/build.gradle.kts`; IN-02 test-count guard relies on an internal Gradle API class
- CMP 1.12.0 deprecates `LocalClipboardManager` (`IconsSection.kt`)

- **VIS-F01** — visual sweep of the remaining ~40 components. Eight are glass now; the rest still read as Material — the most natural successor to v3.1
- **VLST-F01** — `AeroListItem` mirror reflection along the bottom edge
- **VRNG-F01** — Win7-authentic ping-pong indeterminate progress
- **PNL-REORDER-01 / PNL-NEST-01 / PNL-KBD-01** — `AeroPanelGroup` drag-to-reorder, first-class nesting, keyboard resize
- **VER-F01** — hover/focus/drag UI-test snapshots (BASE-05 mechanism) extended to the remaining ~40 components — natural part of VIS-F01
- **VER-F02** — showcase frames at 125% / 200% display scaling (gap carried from v3.0's SHW-16)
- **VER-F03** — external scratch consumer built against the published `3.1.0` tag
- **VER-F04** (v3.2 Future Requirements) — verification on real Windows 10 (no Windows 10 machine available for this milestone)
- **DLG-F01** (v3.2 Future Requirements) — native window behavior for `AeroDialog` (separate undecorated window, no `AeroTitleBar`)
- Older candidate list: inline pickers, DataTable cell-edit/reorder/filter, TreeView DnD, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, `AeroDateTimeRangePicker` hover-preview

### Blockers/Concerns

- **No consumer app is on `v3.1.0` yet.** Six apps in `C:\1A_WORK` consume `v3.0.0`: `aska`, `oper`, `satellite-control`, `pinya`, `2_encoder_buildomator`, `2_encoder_ccsdd`. Each got an upgrade item in its own workflow on 2026-09-24 (bm todo / Kiro brief / Superpowers spec draft). `aska`, `oper` and `2_encoder_buildomator` carry a `strictly("0.7.1-0.6.x-compat")` kotlinx-datetime constraint made for v3.0.0's pickers, which must go. v3.1 itself accepted "tests + agent showcase sweep" instead of a consumer gate (maintainer's choice); the first real consumer upgrade is the practical VER-F03.
- **Visual verification exists at 100% DPI (96) only** — v3.0 and v3.1 both named it rather than implying full-scale coverage. VER-F02 tracks 125% / 200%.
- **No Windows 10 machine available for v3.2.** Everything Windows-10-specific goes on the "unconfirmed" list per VER-13/VER-F04, not tested or silently assumed identical to Windows 11.
- **Seven research conflicts flagged in `.planning/research/SUMMARY.md` § "Conflicts to Settle Empirically"** are not pre-resolved and must be settled by first-hand checks during Phase 22 execution, not more desk research (style bits, Alt+Space automaticity, auto-hide inset size, `WindowState.placement` sync, max-button interaction approach, DWM corner/shadow necessity, JBR interference under `hotRun`). Listed in full in ROADMAP.md Phase 22 detail.

Resolved at v3.1 close: the five research conflicts from `.planning/research/SUMMARY.md` were all settled empirically in Phase 21 (kotlinx-datetime 0.8.0 needed four `Clock` imports; JBR 21 runs `hotRun`; Hot Reload task is `:showcase:hotMcpServer`; Gradle 9.7.1 runs Kotlin 2.4.20 green; capture is `PrintWindow`).

## Session Continuity

Last session: 2026-09-27T14:53:29.232Z
Stopped at: Phase 22 Plan 09 complete -- maximize-button interaction bridge; NC click toggles placement; conflict #5 settled (BTN-01, D-02)
Resume file: None
Next action: `/bm:discuss-phase 22` or `/bm:plan-phase 22`

### Rules that outlive Phase 21

- NEVER delete, move or overwrite anything under `.captures/`. `.captures/old-kt2.4.10-cmp1.11.1/` (1152 files) is the only pre-upgrade baseline and cannot be recreated.
- GUI runs must tolerate the maintainer working: a capture/MCP guard fails only when the app's own process takes the foreground (`9834e8c`). Never ask the maintainer to keep hands off the PC.
- `mcp__compose-hot-reload__*` tools are not available to `bm:gsd-executor`; MCP-driven work runs in the orchestrator session. Executor background jobs die when the executor returns, so long runs (showcase sweeps) run in the orchestrator's background or in executor foreground calls.
- A focusable library `Window` (AeroDialog) opened through MCP can take the foreground from an idle maintainer; watch the foreground owner and close the app right after the capture.

## Operator Next Steps

- Start Phase 22 with `/bm:discuss-phase 22` (or `/bm:plan-phase 22` to plan directly)
- Release is tag-only: origin master is still at `bbe3658`; the local milestone commits are not pushed (maintainer's choice, as in v3.0).
