# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- ✅ **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (shipped 2026-06-22) — 2 bug fixes + `AeroDateTimeRangePicker`
- ✅ **v2.0.2 AeroPanelGroup** — Phases 13 + 13.1 (shipped 2026-06-23) — N-section collapsible+resizable layout, vertical + horizontal orientations
- 🚧 **v2.0.3 PanelGroup Recompose Fix** — Phase 14 (in progress) — horizontal-controlled recompose-during-drag duplication fix + JitPack release

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:
- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`
- `.planning/milestones/v2.0.1-ROADMAP.md`
- `.planning/milestones/v2.0.2-ROADMAP.md`

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

<details>
<summary>✅ v2.0.1 Picker & SplitPane Fixes (Phase 12) — SHIPPED 2026-06-22</summary>

- [x] **Phase 12: v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker** — Fix seconds trigger display, fix nested SplitPane freeze, add new range picker; all showcase demos; doc hygiene (FIXDT-01..02, FIXSP-01..04, DTR-01..08, SHW-11..14) (4/4 plans, completed 2026-06-22)

Details: `.planning/milestones/v2.0.1-ROADMAP.md` · Summary: `.planning/MILESTONES.md`
</details>

<details>
<summary>✅ v2.0.2 AeroPanelGroup (Phases 13 + 13.1) — SHIPPED 2026-06-23</summary>

- [x] **Phase 13: AeroPanelGroup** — Full `AeroPanelGroup` + `AeroPanelSection`: N vertical sections fill the parent, collapse to a ~36dp header strip with neighbors absorbing freed height, drag-resize between adjacent expanded sections, fraction-based size state, hybrid controlled/uncontrolled API, Win7 Aero header, pure-logic unit tests, three-theme sign-off (PNL-01..PNL-18) (5/5 plans, 2026-06-23)
- [x] **Phase 13.1: Horizontal orientation variant (INSERTED)** — Shared internal `AeroPanelGroupImpl(orientation)` core + additive `orientation: Orientation = Orientation.Vertical` default param; N side-by-side columns, vertical dividers, drag-resizes width, rotated header strip + 0°/180° chevron; zero breaking change, zero vertical regression (PNL-HORIZ-01) (3/3 plans, 2026-06-23)

Details: `.planning/milestones/v2.0.2-ROADMAP.md` · Summary: `.planning/MILESTONES.md`
</details>

### 🚧 v2.0.3 PanelGroup Recompose Fix (In Progress)

**Milestone Goal:** A patch release that eliminates section duplication in `AeroPanelGroup` when `Orientation.Horizontal` + controlled mode coincides with a section's content recomposing during an active divider drag — the root cause being an in-composition write to observed snapshot-state (`expandedState`) that is read in the same `BoxWithConstraints` pass. Then ship v2.0.3 on JitPack. Single phase (user-scoped). No breaking changes to the v2.x API; zero new dependencies; Compose stays 1.7.3.

- [ ] **Phase 14: PanelGroup Recompose Fix + v2.0.3 Release** — Compute `expandedArr` from `isExpanded()` each composition; move `expandedState` sync into `SideEffect`; ensure the seed-block doesn't mutate read-in-same-composition state; minimal showcase repro; vertical + uncontrolled regression-guarded; bump + tag + JitPack release (RCMP-01..04, REG-01..02, REL-01..02)

## Phase Details

### Phase 14: PanelGroup Recompose Fix + v2.0.3 Release
**Goal**: Eliminate horizontal-controlled section duplication under recompose-during-drag by removing the in-composition write to observed snapshot-state, with the vertical/uncontrolled paths byte-identical, then release v2.0.3 on JitPack.
**Depends on**: Phase 13.1 (the horizontal `AeroPanelGroupImpl` core this fix edits)
**Requirements**: RCMP-01, RCMP-02, RCMP-03, RCMP-04, REG-01, REG-02, REL-01, REL-02
**Success Criteria** (what must be TRUE):
  1. In `Orientation.Horizontal` + controlled mode, with a section's content recomposing during an active divider drag, sections do NOT duplicate — `N` declared `section(...)` render as exactly `N` header strips (previously `N`→×`N`, e.g. 3→9).
  2. No in-composition write to observed snapshot-state (`expandedState`/`sizePx`) that the same `BoxWithConstraints` pass reads: size-math reads `isExpanded()` directly each composition, `expandedState` sync is moved to `SideEffect`, and the seed-block does not mutate read-in-same-composition state (statically verifiable).
  3. Vertical + uncontrolled showcase behavior is byte-identical — drag-resize, collapse/expand animations (`snap()` during drag, `tween(200ms, FastOutSlowInEasing)` after), `onLayoutChange` on drag-end + toggle only, window-resize proportion preservation; the 12 `PanelGroupLogicTest` JVM tests stay GREEN; Compose stays 1.7.3 with zero new dependencies.
  4. A minimal showcase repro in `LayoutSection.kt` (horizontal controlled `AeroPanelGroup` whose one section's content reads an external `mutableStateOf` that changes during a divider drag) reproduces the duplication before the fix and renders clean after.
  5. Release: `build.gradle.kts` version bumped `2.0.2`→`2.0.3`, tag `v2.0.3` created and pushed to `Tolaseeq/aero-compose-ui`, and JitPack resolves `com.github.Tolaseeq:aero-compose-ui:2.0.3`.
**Plans**: TBD

Plans:
- [ ] 14-01: TBD (planned via `/gsd:plan-phase 14`)

## Progress

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
| 12. Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker | v2.0.1 | 4/4 | Complete | 2026-06-22 |
| 13. AeroPanelGroup | v2.0.2 | 5/5 | Complete | 2026-06-23 |
| 13.1. AeroPanelGroup horizontal orientation variant | v2.0.2 | 3/3 | Complete | 2026-06-23 |
| 14. PanelGroup Recompose Fix + v2.0.3 Release | v2.0.3 | 0/TBD | Not started | - |

## Next Milestone

🚧 **v2.0.3 PanelGroup Recompose Fix** is active (Phase 14). Next: `/gsd:plan-phase 14`.

---

*Roadmap last updated: 2026-06-25 — v2.0.3 PanelGroup Recompose Fix scoped; Phase 14 added (8 requirements, 100% coverage).*
