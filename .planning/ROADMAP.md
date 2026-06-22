# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- ✅ **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (shipped 2026-06-22) — 2 bug fixes + `AeroDateTimeRangePicker`

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:
- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`
- `.planning/milestones/v2.0.1-ROADMAP.md`

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
| 12. Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker | v2.0.1 | 4/4 | Complete | 2026-06-22 |

---

*Roadmap last updated: 2026-06-22 — v2.0.1 shipped, Phase 12 collapsed to milestone summary*
