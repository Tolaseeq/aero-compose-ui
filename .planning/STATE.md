---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: Glass Refinement
status: Awaiting next milestone
stopped_at: "Milestone v3.0 Glass Refinement archived (override_closeout — 9 acknowledged deferred items)"
last_updated: "2026-07-29T14:19:06.121Z"
last_activity: 2026-07-29
last_activity_desc: Milestone v3.0 completed and archived
progress:
  total_phases: 6
  completed_phases: 6
  total_plans: 41
  completed_plans: 41
  percent: 100
current_phase: 20
current_phase_name: verification
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-07-29 — after v3.0 Glass Refinement milestone)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** Planning the next milestone — `/gsd-new-milestone`. Phase numbering continues from 21.

## Current Position

Milestone: v3.0 Glass Refinement — SHIPPED 2026-07-29
Phase: none active
Plan: —
Status: Awaiting next milestone
Last activity: 2026-07-29 — Milestone v3.0 completed and archived

Progress: [██████████] 100% (6/6 phases, 41/41 plans)

## Deferred Items

Items acknowledged and deferred at milestone close on 2026-07-29. Closeout type: `override_closeout` — 9 known verification overrides.

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

Beyond the audit list, one requirement shipped with a recorded gap rather than a pass: **SHW-16's 125% / 200% real-OS DPI passes were explicitly waived by the maintainer** ("Принять без DPI-прогонов"). The 100% DPI three-theme coherence pass is PASSED and capture-backed; the other two were never executed.

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.
**v2.0:** 55 plans, ~49 days, 145 commits, 152 files changed, +27,406 / −2,285 lines.
**v2.0.1:** 4 plans, single-day push (2026-06-22, ~2h20m), 25 commits, 9 code files changed, +520 / −14 lines.
**v2.0.2:** 8 plans (Phases 13 + 13.1), ~1-day push (2026-06-22→23), 49 commits, 4 code files, +1,516 lines.
**v2.0.4:** 3 plans, single-day push incl. corrective release (2026-06-25→26), real RCMP root-cause fix.
**v3.0:** 41 plans / 85 tasks across 6 phases, 2026-07-22 → 2026-07-29 (8 days), 252 commits, 248 files changed (+43,770 / −2,638) of which 71 code files (+11,104 / −409). Tests 232 → 467. Per-plan durations ranged 3 min – 2h10m; the long tail was human visual sign-off rounds, not code. Per-plan metrics are archived with the phase artifacts in `.planning/milestones/v3.0-phases/`.

## Accumulated Context

### Locked decisions carried forward

Full decision log lives in PROJECT.md "Key Decisions". Rules that constrain any future work:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1); extends to ALL Popup/Dialog
- Glass effect in a single `drawBehind` block — performance baseline
- `detectDragGestures` banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03)
- `AeroScrollArea` banned inside DataTable / TreeView — raw `LazyListState + AeroScrollBar` (PITFALL-01)
- Pattern 3 is the locked answer for "animation vs. drag write the same value" (`AeroPanelGroup` precedent, reused by VRNG-09)
- Builder/DSL lambdas that side-effect into a collection must NOT be `@Composable` (v2.0.4 RCMP root cause)
- A regression guard must provably FAIL on unfixed code before it counts as a guard (v2.0.3 lesson; encoded as TOOL-04 and VER-06 in v3.0)
- **[v3.0]** One `drawAeroSurfaceCore` implementation, many exposure paths — derived primitives are `style.copy()` field swaps, never bespoke gradient code
- **[v3.0]** `aeroGlowRing` must be chained OUTSIDE any clip, before `aeroSurface` — the ordering rule for every component composing both
- **[v3.0]** Base-then-transform state resolution (selected resolves base first, hover composes second) — structurally prevents "selection suppresses hover"
- **[v3.0]** Label colour is a scheme-level property picked by surface polarity (`labelOnFilledSurface` / `labelOnOutlinedSurface`), never computed per call site from an animating fill
- **[v3.0]** focus-visible is derived from the interaction stream, not `LocalInputModeManager` — the platform mechanism only gates `Indication`, which this library disables everywhere
- **[v3.0]** Material3 must be pinned to an explicit stable coordinate — the `compose.material3` alias silently resolves to alpha on CMP 1.11.x

### Open technical debt

- **G4** — `AeroOrnamentTokens` brightness on AeroBlue/AeroDark: re-confirmed by the reviewer but deferred; the direction is to try darker values for those two themes specifically, in a foundation session
- **IN-01** — `GlassModifiers.kt` clip gap left open deliberately; blast radius is ~40 components
- **WR-01** — scratch/proof files still ship under `showcase/src/main`
- `AeroRangeSlider` has no keyboard focus tracking at all (pre-existing; adding it is new per-thumb keyboard navigation, out of scope for a render-only restyle)
- Label contrast below the WCAG 4.5:1 floor on several surfaces (see Deferred Items)
- **DROP-FIX-01** — `AeroDropdown` popup offset regression, carried since v1.0; root cause in `AeroScrollArea` (`Column.fillMaxSize()` forces 320dp under `heightIn(max=320.dp)`)

### Future requirements (deferred, sourced from the v3.0 requirements archive)

- **VIS-F01** — visual sweep of the remaining ~40 components. Eight are glass now; the rest still read as Material — the most natural successor to v3.0
- **VLST-F01** — `AeroListItem` mirror reflection along the bottom edge
- **VRNG-F01** — Win7-authentic ping-pong indeterminate progress
- **PNL-REORDER-01 / PNL-NEST-01 / PNL-KBD-01** — `AeroPanelGroup` drag-to-reorder, first-class nesting, keyboard resize
- Older candidate list: inline pickers, DataTable cell-edit/reorder/filter, TreeView DnD, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize, `AeroDateTimeRangePicker` hover-preview

### Blockers/Concerns

- **No real external consumer app tracks this library's current line.** `aska` and `satellite-control` stayed on the 2.0.4 toolchain through v3.0, so the project's historically strongest regression-catcher is unavailable. v3.0's mitigation was a minimal scratch consumer built against a published tag (VER-05) — it did catch a real defect, but it is not a full replacement for a production consumer.
- **v3.0's visual work was accepted at 100% DPI only.** Anything built on top of the primitives layer inherits an unverified assumption about fractional-density rendering.

## Session Continuity

Last session: 2026-07-29
Stopped at: Milestone v3.0 Glass Refinement archived — roadmap and requirements snapshotted to `.planning/milestones/`, phase artifacts moved to `.planning/milestones/v3.0-phases/`, version bumped to `3.0.0`
Resume file: None
Next action: `/gsd-new-milestone` — questioning → research → requirements → roadmap. Phase numbering continues from 21.

## Operator Next Steps

- Start the next milestone with /gsd-new-milestone
