---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Stateful + Layout
status: in-progress
stopped_at: Completed 08-01-PLAN.md
last_updated: "2026-06-18T10:00:55.365Z"
last_activity: "2026-06-18 — Phase 8 plan-01 completed: public AeroRangeSlider landed (custom Canvas + awaitPointerEventScope dual-thumb drag, start<=end clamp, primary inter-thumb fill, last-moved-on-top z-order); 13 state-logic tests green; first new public v2.0 component validates the locked drag pattern (PITFALL-03)"
progress:
  total_phases: 11
  completed_phases: 7
  total_plans: 37
  completed_plans: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-30 — v2.0 Current Milestone section added)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** v2.0 Stateful + Layout — roadmap created, ready to plan Phase 7 (Shared Internal Primitives).

## Current Position

Phase: 8 — Pickers
Plan: 1 of 6 complete (08-01-range-slider done)
Status: 08-01 (AeroRangeSlider / PICK-08) complete — 5 picker plans remaining in phase
Last activity: 2026-06-18 — Phase 8 plan-01 completed: public AeroRangeSlider landed (custom Canvas + awaitPointerEventScope dual-thumb drag, start<=end clamp, primary inter-thumb fill, last-moved-on-top z-order); 13 state-logic tests green; first new public v2.0 component validates the locked drag pattern (PITFALL-03)

```
v2.0 Progress: [███       ] 26% (1/5 phases, 1/6 plans of phase 8)
```

## Shipped Milestones

| Version | Name | Phases | Plans | Requirements | Shipped |
|---------|------|--------|-------|--------------|---------|
| v1.0 | MVP (Foundation + Atomic + Composite/Navigation) | 1–3 | 18 | 53 | 2026-04-28 (informal — not archived through `/gsd:complete-milestone`) |
| v1.1 | Icon System | 4–6 | 11 | 17 | 2026-04-30 |

See `.planning/MILESTONES.md` for accomplishments and `.planning/milestones/v1.1-ROADMAP.md` for the v1.1 ship-time roadmap snapshot (also captures v1.0 phase definitions).

## v2.0 Locked Decisions (from /gsd:new-milestone scoping, 2026-04-30)

- Version: **v2.0** (major-feature drop sized like v1.0; no breaking changes to existing v1.x API)
- DataTable: full scope — sortable columns + row selection (single/multi) + row virtualization + resizable columns
- TreeView: lazy children via `onExpand` callback (not eager in model)
- Date/time pickers: 4 separate components (DatePicker, TimePicker, DateTimePicker, DateRangePicker) — no consolidation
- ColorPicker: full scope — HSV square + hue + RGB sliders + HEX input + swatches; alpha optional
- Accordion: both modes via `mode = single | multi` parameter
- SplitPane: 2-pane public API + N-pane via nesting (not recursive in API surface)
- Sidebar: new component alongside `AeroDrawer` (different mechanic — persistent vs overlay); modes = expanded + collapsed-icons + hidden
- StepperWizard: linear with per-step `onValidate: () -> Boolean` callback (no branching)
- Inline-mode date/time pickers, DataTable cell editing, TreeView drag-drop, ColorPicker eyedropper, StepperWizard branching, Sidebar drag-resize → all OUT of scope (deferred to v2.x or beyond)
- AeroDropdown popup-offset regression (v1.0 carry-over) → NOT in v2.0 scope; separate gap-closure or future milestone

## v2.0 Roadmap Summary (2026-04-30)

| Phase | Name | Goal | Requirements | Complexity |
|-------|------|------|--------------|------------|
| 7 | Shared Internal Primitives | Internal foundation for Phases 8–10 (CalendarGrid, ColorMath, HsvSquare+HueSlider, AeroDragSplitter, StepIndicator, AeroCalendarPositionProvider) | Enabling (no owned REQs) | — |
| 8 | Pickers | 6 picker/slider components (RangeSlider + 4 date/time pickers + ColorPicker) | PICK-01..08 (8) | LARGE×2 + MEDIUM×2 + SMALL×2 |
| 9 | Data | AeroDataTable + AeroTreeView | DATA-01..06 (6) | LARGE×1 + MEDIUM×1 |
| 10 | Layout | AeroAccordion + AeroSplitPane + AeroSidebar + AeroStepperWizard | LAYO-01..09 (9) | MEDIUM×3 + SMALL×1 |
| 11 | Showcase + Sign-off | DataSection + PickersSection + LayoutSection + 16-item checklist gate | SHW-07..10 (4) | — |

**Coverage:** 27/27 v2.0 requirements mapped (PICK 8, DATA 6, LAYO 9, SHW 4)

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.

**v2.0 metrics:**

| Plan | Duration | Tasks | Files | Notes |
|------|----------|-------|-------|-------|
| 07-01 logic-and-tests | ~10 min | 4 | 9 | TDD red-green per task; 27 JUnit tests across 3 new test classes; PITFALL-02 / PITFALL-08 / PITFALL-15 defused at utility level |
| 07-02 visuals-and-scratch | ~10m 33s | 5 | 8 | aeroDragSplitter + HsvColorSquare + HueSlider + StepIndicator landed; AeroPhase7Scratch aggregator in :library + thin Phase7ScratchSection wrapper in :showcase preserves locked-internal API for all 6 primitives; PITFALL-03 mitigation locked at the shared-utility level |
| Phase 07-shared-internal-primitives P03 | 3min | 2 tasks | 2 files |
| Phase 08-pickers P01 | 3min | 2 tasks | 2 files |

## Accumulated Context

### Decisions

Full decision log in PROJECT.md "Key Decisions" table. Active decisions affecting all future milestones:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1); extends to ALL Popup/Dialog in v2.0 (W11-01)
- Glass effect in single `drawBehind` block — performance baseline
- `:library` uses `compose.desktop.common`, `:showcase` uses `currentOs`
- AeroIcons name verbatim from Phosphor — locked v1.1
- `Icon()` from material3 used directly; tint always explicit — locked v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time
- AeroBreadcrumb `separator: String` intentionally NOT migrated to `ImageVector`
- **v2.0 new:** `detectDragGestures` is banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03, touchSlop=18dp)
- **v2.0 new:** `AeroScrollArea` is banned inside DataTable / TreeView — use raw `LazyListState + AeroScrollBar` (PITFALL-01)
- **v2.0 new:** DataTable selection API is `Set<RowKey>` + `key: (T) -> Any`, NOT `Set<Int>` indices (PITFALL-04)
- **v2.0 new:** ColorPicker internal state is HSV float tuple only; RGB and HEX are derived views (PITFALL-15)
- **v2.0 new:** `AeroCalendarPositionProvider` (new, Phase 7) replaces `AeroDropdownPopup` for all date picker popups (PITFALL-02)
- **v2.0 new:** `kotlinx-datetime:0.6.2` added in Phase 8 as the only new dependency
- [Phase 07]: Hue convention LOCKED to degrees [0f, 360f] (matches Color.hsv requirePrecondition)
- [Phase 07]: AeroCalendarPositionProvider first-frame guard uses popupContentSize == IntSize.Zero, NOT >= windowSize
- [Phase 07]: Width overflow on wide popup right-aligns and never flips Top/Bottom
- [Phase 07]: Touchslop spike skipped — awaitPointerEventScope manual loop is the locked v2.0 pattern regardless
- [Phase 07]: [Phase 07] Phase7ScratchSection demo body lives in :library (AeroPhase7Scratch.kt, public, deleted Phase 11) NOT :showcase — Kotlin internal is module-scoped, so the showcase imports a thin public wrapper to keep all 6 Phase 7 primitives internal
- [Phase 07]: [Phase 07] Modifier.aeroDragSplitter is the locked v2.0 in-content drag pattern: awaitPointerEventScope + manual loop, cursor change keyed to Orientation, change.consume() only on actual delta (release uncconsumed)
- [Phase 07]: AeroCalendarGrid header Row pinned to Modifier.width(252.dp); outer Column wrapContentWidth — header now matches 7x36dp day-grid width exactly
- [Phase 07]: Architecture B reconfirmed: AeroCard glass wrappers in scratch demo only — AeroCalendarGrid + AeroStepIndicator remain surface-less; glass is a consumer responsibility
- [Phase 08-pickers]: AeroRangeSlider tooltip is an overlaid glassEffect Box positioned by thumb x (AeroSlider pill approach), not in-Canvas text; behind showTooltip, only for the active thumb
- [Phase 08-pickers]: lastMovedThumb inits to RangeThumb.End so the Start thumb draws on top when both thumbs compress (PITFALL-07); thumbToDrawFirst helper makes z-order unit-testable

### Pending Todos

- Gap-close: AeroDropdown popup offset regression — explicitly OUT of v2.0 scope; can be picked up as a one-off after v2.0 ships, or scheduled into a v2.x milestone
- Phase 7 research: upstream `touchSlop` issue #343 may have been silently fixed in CMP 1.7.x — 1-minute drag test at Phase 7 plan-01 confirms which path to take
- Phase 8 research: `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 first-compile validation — Phase 8 plan-01 acceptance criterion; fallback is `0.7.1-0.6.x-compat`
- Phase 9 research: `rememberScrollbarAdapter(LazyListState)` API surface verification in CMP 1.7.3

### Blockers/Concerns

- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0; revisit if/when CMP version bump is on the table
- v2.0 risks (see ROADMAP.md Phase notes and PITFALLS.md for full detail):
  - PITFALL-01 (silent showstopper): LazyColumn inside AeroScrollArea destroys DataTable virtualization
  - PITFALL-03 (silent showstopper): `detectDragGestures` touchSlop=18dp silently breaks ColorPicker HSV drag, RangeSlider thumbs, DataTable column resize — resolved by Phase 7 shared `awaitPointerEventScope` utility
  - PITFALL-04: DataTable selection-by-index becomes stale after sort — API design must lock `Set<RowKey>` in Phase 9 plan-01

## Session Continuity

Last session: 2026-06-18T10:00:27.710Z
Stopped at: Completed 08-01-PLAN.md
Resume file: None
Next action: `/gsd:verify-work` (Phase 7 complete) → `/gsd:plan-phase 8` — Pickers
