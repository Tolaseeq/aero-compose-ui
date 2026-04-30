---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Stateful + Layout
status: defining_requirements
stopped_at: v2.0 milestone started — defining requirements
last_updated: "2026-04-30T13:30:00.000Z"
last_activity: "2026-04-30 — Milestone v2.0 Stateful + Layout started: 12 components scoped (CMPLX + ADVL); ready for requirements + roadmap"
progress:
  total_phases: null
  completed_phases: null
  total_plans: null
  completed_plans: null
  percent: null
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-30 — v2.0 Current Milestone section added)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** v2.0 Stateful + Layout — defining requirements (12 components: CMPLX + ADVL).

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-04-30 — Milestone v2.0 started, scope confirmed (full 12-component set, all maximalist scopes locked)

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

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.

**v2.0 metrics:** pending (no plans completed yet)

## Accumulated Context

### Decisions

Full decision log in PROJECT.md "Key Decisions" table. Active decisions affecting all future milestones:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1)
- Glass effect in single `drawBehind` block — performance baseline
- `:library` uses `compose.desktop.common`, `:showcase` uses `currentOs`
- AeroIcons name verbatim from Phosphor — locked v1.1
- `Icon()` from material3 used directly; tint always explicit — locked v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time
- AeroBreadcrumb `separator: String` intentionally NOT migrated to `ImageVector`

### Pending Todos

- Gap-close: AeroDropdown popup offset regression — explicitly OUT of v2.0 scope; can be picked up as a one-off after v2.0 ships, or scheduled into a v2.x milestone

### Blockers/Concerns

- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0; revisit if/when CMP version bump is on the table
- v2.0 risks (to be addressed in research / planning):
  - DataTable virtualization with column resizing has known Compose layout pitfalls (LazyColumn + horizontal scroll + measured columns)
  - DateTimePicker / DateRangePicker layout density on small windows
  - ColorPicker HSV ↔ RGB conversion math correctness (avoid drift on round-trips)
  - TreeView lazy callback contract (race conditions if callback returns asynchronously)

## Session Continuity

Last session: 2026-04-30 (milestone v2.0 scoping)
Stopped at: PROJECT.md updated with v2.0 Current Milestone section; STATE.md set to defining-requirements
Resume file: None
Next action: Continue `/gsd:new-milestone` workflow → research decision → REQUIREMENTS.md → ROADMAP.md
