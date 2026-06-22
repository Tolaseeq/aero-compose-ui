# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- 📋 **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (active) — 2 bug fixes + `AeroDateTimeRangePicker`

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:
- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–3) — SHIPPED 2026-04-28</summary>

- [x] **Phase 1: Foundation** — Theme system, glass modifiers, module structure, showcase skeleton (4/4 plans, 2026-04-27)
- [x] **Phase 2: Atomic Components** — Buttons, inputs, selection controls, sliders, list items, badges (6/6 plans, 2026-04-28)
- [x] **Phase 3: Composite + Navigation** — Containers, overlays, dialogs, menus, tabs, window chrome (8/8 plans, 2026-04-28)

Details: `.planning/milestones/v1.1-ROADMAP.md`
</details>

<details>
<summary>✅ v1.1 Icon System (Phases 4–6) — SHIPPED 2026-04-30</summary>

- [x] **Phase 4: AeroIcons Foundation** — 138 Phosphor Regular ImageVector constants; lazy backing-property; explicitApi (2/2 plans, 2026-04-29)
- [x] **Phase 5: Component Migrations + Dependency Removal** — 11 components migrated; materialIconsExtended removed; grep gate clean (5/5 plans, 2026-04-29)
- [x] **Phase 6: Showcase IconsSection** — LazyVerticalGrid of 138 icons + search; three-theme visual sign-off (3/3 plans, 2026-04-29)

Details: `.planning/milestones/v1.1-ROADMAP.md`
</details>

<details>
<summary>✅ v2.0 Stateful + Layout (Phases 7–11) — SHIPPED 2026-06-18</summary>

- [x] **Phase 7: Shared Internal Primitives** — CalendarGrid, ColorMath, HsvSquare+HueSlider, aeroDragSplitter, StepIndicator, CalendarPositionProvider; no new public API (3/3 plans, 2026-06-17)
- [x] **Phase 8: Pickers** — AeroRangeSlider, AeroDatePicker, AeroTimePicker, AeroDateTimePicker, AeroDateRangePicker, AeroColorPicker; kotlinx-datetime:0.6.2 (6/6 plans, 2026-06-18)
- [x] **Phase 9: Data** — AeroDataTable (virtualized, sortable, selectable, resizable) + AeroTreeView (lazy expand) (3/3 plans, 2026-06-18)
- [x] **Phase 10: Layout** — AeroAccordion, AeroSplitPane, AeroSidebar, AeroStepperWizard (4/4 plans, 2026-06-18)
- [x] **Phase 11: Showcase + v2.0 Visual Sign-off** — DataSection/PickersSection/LayoutSection; 16-item × 3-theme silent-failure checklist gate (11/11 plans, 2026-06-18)

Details: `.planning/milestones/v2.0-ROADMAP.md` · Audit: `.planning/milestones/v2.0-MILESTONE-AUDIT.md`
</details>

### v2.0.1 Picker & SplitPane Fixes (Phase 12) — active

- [x] **Phase 12: v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker** — Fix seconds trigger display, fix nested SplitPane freeze, add new range picker; all showcase demos; doc hygiene (FIXDT-01..02, FIXSP-01..04, DTR-01..08, SHW-11..14) (completed 2026-06-22)

## Phase Details

### Phase 12: v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker
**Goal**: Deliver all three v2.0.1 deliverables: (A) `AeroDateTimePicker` correctly shows seconds in the trigger when `showSeconds = true`; (B) nested N-pane `AeroSplitPane` layouts drag without snap-back or crash; (C) new `AeroDateTimeRangePicker` component ships with a dual-calendar + two time rows commit-gate picker emitting `(LocalDateTime, LocalDateTime)`. All three deliverables are verified in the showcase on three themes; doc hygiene clears the stale kotlinx-datetime note. No new dependencies, no breaking changes to v2.0 public API.
**Depends on**: Phase 11 (v2.0 shipped)
**Requirements**: FIXDT-01, FIXDT-02, FIXSP-01, FIXSP-02, FIXSP-03, FIXSP-04, DTR-01, DTR-02, DTR-03, DTR-04, DTR-05, DTR-06, DTR-07, DTR-08, SHW-11, SHW-12, SHW-13, SHW-14
**Plan-Guidance** (build order within Phase 12 — planner sequences plans in this order):
  1. **Fix A first** (FIXDT-01, FIXDT-02, SHW-12, SHW-14): Single-file change to `AeroDateTimePicker.kt` default formatter; introduce `internal fun formatAeroDateTime(ldt, showSeconds)` shared helper; clear stale doc note. The helper must exist before Fix C begins — it is the direct prerequisite for the new component's trigger format and prevents re-introducing PITFALL-H.
  2. **Fix B second** (FIXSP-01..04, SHW-13): Two-file change (`AeroSplitPane.kt` + `SplitClamp.kt`); unit test for `clampDividerPx` with inverted range written BEFORE applying the fix (mandatory). Independent from Fix A but slightly more complex — cleaner to have Fix A verified first.
  3. **New component last** (DTR-01..08, SHW-11): `AeroDateTimeRangePicker.kt` new file; inherits `formatAeroDateTime` from Fix A. Lock Apply-gate architecture decision (no auto-close on second date click, PITFALL-E) before writing composable body.
**Success Criteria** (what must be TRUE):
  1. Setting `showSeconds = true` on `AeroDateTimePicker`, entering non-zero seconds, and confirming shows `HH:MM:SS` in the trigger — not `HH:MM`; default `showSeconds = false` still shows `HH:MM` with no visual change
  2. A caller passing an explicit `formatter` lambda to `AeroDateTimePicker` sees their formatter used verbatim — Fix A does not override it
  3. In a 3-pane nested `AeroSplitPane` layout, dragging the outer splitter across its full range does not move the inner divider — it holds its fractional position; squeezing the inner pane below `minFirstPaneSize + minSecondPaneSize` does not throw an exception and clamps silently
  4. A single-level (non-nested) `AeroSplitPane` drag, min-clamp, and window-resize re-anchor behave identically to v2.0 — no regression
  5. Clicking a second date in `AeroDateTimeRangePicker` popup does not close it and does not call `onRangeSelect` — Apply is the sole emit and close trigger; Apply is disabled until both dates are selected; `onRangeSelect` receives a pair where `start ≤ end` including time (same-day reversed times are silently swapped)
  6. Cancelling or clicking outside `AeroDateTimeRangePicker` and reopening shows no trace of the previous partial session — pending times and range state reset cleanly; `showSeconds` and `minuteStep` apply equally to both time rows
  7. The showcase contains: (a) `PickersSection` row for `AeroDateTimeRangePicker` with live `(LocalDateTime, LocalDateTime)` label; (b) `AeroDateTimePicker` demo with `showSeconds = true` where seconds are visible in the trigger; (c) `LayoutSection` nested 3-pane demo where both splitters are freely draggable — all verified on AeroBlue / AeroDark / Classic
  8. The stale "Revisit on publish — kotlinx-datetime declared implementation" note in PROJECT.md Key Decisions is replaced with the factual record that `api(libs.kotlinx.datetime)` is already in place
**Plans**: 4 plans
  - [ ] 12-01-PLAN.md — Fix A: formatAeroDateTime helper + showSeconds-aware trigger + doc hygiene (FIXDT-01/02, SHW-14)
  - [ ] 12-02-PLAN.md — Fix B: inverted-range clamp test (TDD) + fraction-based SplitPane state (FIXSP-01..04)
  - [ ] 12-03-PLAN.md — AeroDateTimeRangePicker: Apply-gate dual-calendar datetime range picker + orderDateTimeRange (DTR-01..08)
  - [ ] 12-04-PLAN.md — Showcase demos on three themes: range picker, seconds contrast, nested 3-pane SplitPane (SHW-11/12/13)

## Progress

**Execution Order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 4/4 | Complete | 2026-04-27 |
| 2. Atomic Components | v1.0 | 6/6 | Complete | 2026-04-28 |
| 3. Composite + Navigation | v1.0 | 8/8 | Complete | 2026-04-28 |
| 4. AeroIcons Foundation | v1.1 | 2/2 | Complete | 2026-04-29 |
| 5. Component Migrations + Dep Removal | v1.1 | 5/5 | Complete | 2026-04-29 |
| 6. Showcase IconsSection | v1.1 | 3/3 | Complete | 2026-04-29 |
| 7. Shared Internal Primitives | v2.0 | 3/3 | Complete | 2026-06-17 |
| 8. Pickers | v2.0 | 6/6 | Complete | 2026-06-18 |
| 9. Data | v2.0 | 3/3 | Complete | 2026-06-18 |
| 10. Layout | v2.0 | 4/4 | Complete | 2026-06-18 |
| 11. Showcase + v2.0 Visual Sign-off | v2.0 | 11/11 | Complete | 2026-06-18 |
| 12. v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker | 4/4 | Complete   | 2026-06-22 | - |

---

*Roadmap last updated: 2026-06-22 — v2.0.1 consolidated to single Phase 12 (user revision)*
