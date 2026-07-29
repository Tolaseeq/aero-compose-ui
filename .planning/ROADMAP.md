# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- ✅ **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (shipped 2026-06-22) — 2 bug fixes + `AeroDateTimeRangePicker`
- ✅ **v2.0.2 AeroPanelGroup** — Phases 13 + 13.1 (shipped 2026-06-23) — N-section collapsible+resizable layout, vertical + horizontal orientations
- ✅ **v2.0.4 PanelGroup Recompose Fix** — Phase 14 (shipped 2026-06-26) — horizontal-controlled recompose-during-drag duplication fix (real root cause: non-`@Composable` DSL); v2.0.3 was a superseded wrong-cause release
- ✅ **v3.0 Glass Refinement** — Phases 15–20 (shipped 2026-07-29) — toolchain migration (Kotlin 2.4.10 / CMP 1.11.1), repaired + extended Aero primitives layer, eight components restyled to genuine Win7 glass
- 📋 **v3.x / next** — not yet scoped (`/gsd-new-milestone`)

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:

- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`
- `.planning/milestones/v2.0.1-ROADMAP.md`
- `.planning/milestones/v2.0.2-ROADMAP.md`
- `.planning/milestones/v2.0.4-ROADMAP.md`
- `.planning/milestones/v3.0-ROADMAP.md`

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

<details>
<summary>✅ v2.0.4 PanelGroup Recompose Fix (Phase 14) — SHIPPED 2026-06-26</summary>

**Milestone Goal:** Eliminate header-strip duplication in horizontal CONTROLLED `AeroPanelGroup` when a divider is dragged while the hosting screen recomposes. Single phase (user-scoped). No breaking changes; zero new runtime dependencies; Compose stays 1.7.3.

- [x] **Phase 14: PanelGroup Recompose Fix** — shipped as v2.0.4 (RCMP-01..04, REG-01..02, REL-01..02)

**Note:** the originally-planned v2.0.3 release fixed the WRONG cause (a write-during-composition theory: `SideEffect` sync + `isExpanded()`-derived size-math) and the bug still reproduced in a real consumer. The REAL root cause was the `@Composable` section-DSL lambda re-running independently during an active drag and re-appending `section()` into the persisted `AeroPanelGroupScope` (`scope.sections` grew 3→9→…). Fixed in v2.0.4 by making the DSL lambda non-`@Composable` (like `LazyListScope`), guarded by a deterministic `runComposeUiTest` programmatic-drag test (11→1 headers). Confirmed in the consumer app. v2.0.3 remains tagged but superseded.

Details: `.planning/milestones/v2.0.4-ROADMAP.md` · Summary: `.planning/MILESTONES.md` · Retrospective: `.planning/RETROSPECTIVE.md`
</details>

<details>
<summary>✅ v3.0 Glass Refinement (Phases 15–20) — SHIPPED 2026-07-29</summary>

**Milestone Goal:** Restyle eight Material3-looking components (`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`) into genuinely Aero-glass surfaces — on a repaired and extended shared Aero-primitives layer, on top of a migrated, current-stable Compose Multiplatform toolchain. Public API and behavior did not change. Major version because the mandatory toolchain migration raises the minimum Kotlin/Compose for all consumers.

- [x] **Phase 15: Toolchain Upgrade** — Kotlin 2.4.10 + Compose Multiplatform 1.11.1, Material3 pinned to stable 1.9.0, RCMP guard ported and re-proven non-inert, migration proven behaviorally and visually inert (6/6 plans, 2026-07-22)
- [x] **Phase 16: Foundation — Aero Primitives Layer** — repaired `GlassModifiers`, `AeroSurfaceStyle`/`AeroSurfacePrimitives`, derived `AeroOrnamentTokens`, shared thumb/groove/glow primitives, full-library smoke pass (5/5 plans, 2026-07-23)
- [x] **Phase 17: Buttons** — `AeroButton`, `AeroOutlinedButton` restyled on one shared internal `AeroButtonSurface`; M3 container dropped, `Role.Button` + keyboard kept (5/5 plans, 2026-07-23)
- [x] **Phase 18: Range** — `AeroSlider` (M3 custom slots), `AeroRangeSlider` (Canvas, drag logic byte-identical), `AeroProgressBar` restyled (4/4 plans, 2026-07-24)
- [x] **Phase 19: Selectors + Lists** — `AeroSwitch`, `AeroSegmentedControl`, `AeroListItem` restyled; first-ever hover/press/focus; shared focus-visible mechanism (12/12 plans, 2026-07-28)
- [x] **Phase 20: Verification** — showcase wiring, fail-then-pass grep gates, size/radius snapshot, external scratch consumer, three-theme sign-off (9/9 plans, 2026-07-29)

**Closeout:** `override_closeout` — SHW-16's 125%/200% DPI passes explicitly waived by the maintainer, plus 9 acknowledged deferred items (STATE.md § Deferred Items).

Details: `.planning/milestones/v3.0-ROADMAP.md` · Requirements: `.planning/milestones/v3.0-REQUIREMENTS.md` · Phase artifacts: `.planning/milestones/v3.0-phases/` · Summary: `.planning/MILESTONES.md`
</details>

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
| 13.1. AeroPanelGroup horizontal orientation | v2.0.2 | 3/3 | Complete | 2026-06-23 |
| 14. PanelGroup Recompose Fix | v2.0.4 | 3/3 | Complete | 2026-06-26 |
| 15. Toolchain Upgrade | v3.0 | 6/6 | Complete | 2026-07-22 |
| 16. Foundation — Aero Primitives Layer | v3.0 | 5/5 | Complete | 2026-07-23 |
| 17. Buttons | v3.0 | 5/5 | Complete | 2026-07-23 |
| 18. Range | v3.0 | 4/4 | Complete | 2026-07-24 |
| 19. Selectors + Lists | v3.0 | 12/12 | Complete | 2026-07-28 |
| 20. Verification | v3.0 | 9/9 | Complete | 2026-07-29 |

## Next Milestone

Not yet scoped. Run `/gsd-new-milestone` — questioning → research → requirements → roadmap. Phase numbering continues from **21**.

Candidate carry-overs for scoping (full list in PROJECT.md and `.planning/todos/pending/`):

- Deferred from v3.0: G4 ornament-token brightness on AeroBlue/AeroDark; `AeroRangeSlider` keyboard focus; IN-01 `GlassModifiers.kt` clip gap (~40-component blast radius); scratch/proof files still under `showcase/src/main`; label-contrast WCAG floor
- VIS-F01 — visual sweep of the remaining ~40 components; VLST-F01 list-item mirror reflection; VRNG-F01 Win7 ping-pong indeterminate
- DROP-FIX-01 — `AeroDropdown` popup offset regression (carried since v1.0)
- `AeroPanelGroup`: PNL-REORDER-01, PNL-NEST-01, PNL-KBD-01

---

*Roadmap last updated: 2026-07-29 — v3.0 Glass Refinement shipped and archived (Phases 15–20, 41 plans, 57/57 requirements).*
