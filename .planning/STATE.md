---
gsd_state_version: 1.0
milestone: v3.1
milestone_name: Dependency Refresh + Hot Reload MCP
status: executing
stopped_at: Completed 21-14-PLAN.md (3.1.0 released, tag only)
last_updated: "2026-09-24T14:08:44.952Z"
last_activity: 2026-09-23 -- Phase 21 execution started
progress:
  total_phases: 1
  completed_phases: 1
  total_plans: 14
  completed_plans: 14
  percent: 100
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-09-23 — v3.1 roadmap created)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** Phase 21 — migration-release-3-1-0

## Current Position

Phase: 21 (migration-release-3-1-0) — EXECUTING
Plan: 14 of 14
Status: Executing Phase 21
Last activity: 2026-09-23 -- Phase 21 execution started

## Deferred Items

Items acknowledged and deferred at milestone close on 2026-07-29 (v3.0). Closeout type: `override_closeout` — 9 known verification overrides. Explicitly out of v3.1 scope (see PROJECT.md "Явно НЕ в scope").

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
**v3.1:** in progress — 1 phase (21), 14 plans planned.

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

### Open technical debt

- **G4** — `AeroOrnamentTokens` brightness on AeroBlue/AeroDark: re-confirmed by the reviewer but deferred; the direction is to try darker values for those two themes specifically, in a foundation session
- **IN-01** — `GlassModifiers.kt` clip gap left open deliberately; blast radius is ~40 components
- **WR-01** — scratch/proof files still ship under `showcase/src/main`
- `AeroRangeSlider` has no keyboard focus tracking at all (pre-existing; adding it is new per-thumb keyboard navigation, out of scope for a render-only restyle)
- Label contrast below the WCAG 4.5:1 floor on several surfaces (see Deferred Items)
- **DROP-FIX-01** — `AeroDropdown` popup offset regression, carried since v1.0; root cause in `AeroScrollArea` (`Column.fillMaxSize()` forces 320dp under `heightIn(max=320.dp)`)

### Future requirements (deferred, sourced from the v3.0 + v3.1 requirements archives)

- **VIS-F01** — visual sweep of the remaining ~40 components. Eight are glass now; the rest still read as Material — the most natural successor to v3.1
- **VLST-F01** — `AeroListItem` mirror reflection along the bottom edge
- **VRNG-F01** — Win7-authentic ping-pong indeterminate progress
- **PNL-REORDER-01 / PNL-NEST-01 / PNL-KBD-01** — `AeroPanelGroup` drag-to-reorder, first-class nesting, keyboard resize
- **VER-F01** — hover/focus/drag UI-test snapshots (BASE-05 mechanism) extended to the remaining ~40 components — natural part of VIS-F01
- **VER-F02** — showcase frames at 125% / 200% display scaling (gap carried from v3.0's SHW-16)
- **VER-F03** — external scratch consumer built against the published `3.1.0` tag
- Older candidate list: inline pickers, DataTable cell-edit/reorder/filter, TreeView DnD, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, `AeroDateTimeRangePicker` hover-preview

### Blockers/Concerns

- **No real external consumer app tracks this library's current line.** `aska` and `satellite-control` stayed on the 2.0.4 toolchain through v3.0, so the project's historically strongest regression-catcher is unavailable; v3.1 explicitly accepts "tests + agent showcase sweep" instead of an external consumer gate (maintainer's choice, see PROJECT.md).
- **v3.0's visual work was accepted at 100% DPI only.** v3.1's own verification (Phase 21 / VER-09) must name the DPI scale it ran at rather than silently implying full-scale coverage — the same gap, not yet closed.
- **Five research conflicts flagged in `.planning/research/SUMMARY.md`** are not pre-resolved: kotlinx-datetime 0.8.0 compile-break vs. no-op (Phase 21), JBR Java-21-only vs. JBR-25-default (Phase 21), exact Hot Reload task names (Phase 21), Gradle 9.7.1 vs. Kotlin 2.4.20's documented 9.7.0 ceiling (Phase 21), and the `take_screenshot` capture mechanism (settled — see Locked decisions). Each has a named cheapest empirical check; `MCP-HOWTO.md` (maintainer-verified) already resolves several of these in practice and wins on conflict.

## Session Continuity

Last session: 2026-09-24T14:08:44.931Z
Stopped at: Completed 21-14-PLAN.md (3.1.0 released, tag only)
Resume file: None
Next action: phase 21 verification

### Orchestrator rules for the rest of Phase 21

- Run every executor SEQUENTIALLY on the main working tree — no `isolation="worktree"`. All image artifacts use the absolute path `C:\1A_WORK\ui_lib\.captures\`, which does not exist in a worktree (D-01), and the D-03 `.captures` file-count proofs race if two plans run at once.
- NEVER delete, move or overwrite anything under `.captures/`. `.captures/old-kt2.4.10-cmp1.11.1/` (1152 files) is the only pre-upgrade baseline and cannot be recreated on the new toolchain. Put this rule verbatim into every executor prompt — the 21-03 executor once wiped `.captures` "after inspection" (smoke frames only, before the baseline existed).
- Plans 21-08 and 21-11 need the `mcp__compose-hot-reload__*` tools. The `bm:gsd-executor` agent type has no MCP tools, so run those plans inline in the orchestrator session (or with an agent type that has full tool access).
- If a Gradle build fails with `Unable to delete file ...\library\build\libs\library-*.jar`, the usual holder is the VS Code Kotlin language server (`fwcd.kotlin`, a `java.exe` whose command line contains `org.javacs.kt.MainKt`). Ask the maintainer before ending it; executors must not touch external processes.

## Operator Next Steps

- None pending. Continue with `/bm:execute-phase 21` (Plan 21-09).
