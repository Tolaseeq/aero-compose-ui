# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- ✅ **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (shipped 2026-06-22) — 2 bug fixes + `AeroDateTimeRangePicker`
- 🚧 **v2.0.2 AeroPanelGroup** — Phase 13 (in progress) — N-section vertical collapsible+resizable layout

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

### ✅ v2.0.2 AeroPanelGroup (Phase 13 Complete — ready for tagging)

**Milestone Goal:** Add one additive layout component `AeroPanelGroup` (+ `AeroPanelSection`) — N vertical sections that fill the parent container height, collapse to a header strip on click (neighbors absorb freed height), and support drag-resize between adjacent expanded sections (VS Code Side Bar model). Strictly one phase, no breaking changes to v2.x API.

- [x] **Phase 13: AeroPanelGroup** — Full `AeroPanelGroup` + `AeroPanelSection` with collapse/expand animation, drag-resize between expanded neighbors, fraction-based size state, hybrid controlled/uncontrolled API, Win7 Aero header styling, unit tests, and showcase sign-off (PNL-01..PNL-18) (completed 2026-06-23)

## Phase Details

### Phase 13: AeroPanelGroup
**Goal**: Developers and users have a fully functional `AeroPanelGroup` component: N vertical sections fill their parent height, any section collapses to a ~36dp header strip with neighbors absorbing the freed space, adjacent expanded sections resize by drag, and the whole component ships with pure-logic unit tests, KDoc, and a three-theme showcase sign-off.
**Depends on**: Phase 12 (all shipping infrastructure in place; `aeroDragSplitter`, `clampDividerPx`, `animateFloatAsState` patterns all verified in repo)
**Requirements**: PNL-01, PNL-02, PNL-03, PNL-04, PNL-05, PNL-06, PNL-07, PNL-08, PNL-09, PNL-10, PNL-11, PNL-12, PNL-13, PNL-14, PNL-15, PNL-16, PNL-17, PNL-18
**Success Criteria** (what must be TRUE):
  1. A user collapses an expanded section and its neighbors immediately absorb the freed height proportionally; re-expanding restores the prior size from `lastExpandedFraction` and the neighbors shrink back accordingly.
  2. A user drags a divider between two adjacent expanded sections and both sections resize in real time with no animation lag; the divider is absent (no grip, no cursor change) wherever a collapsed section is adjacent.
  3. The component survives window resize without section proportions resetting — sizes are fraction-based and re-derive from the new `totalPx` each recompose, matching the AeroSplitPane precedent.
  4. Three-theme visual sign-off passes on AeroBlue / AeroDark / Classic: glassPanel header, CaretRight chevron rotating 0°→90° on expand, optional `leadingIcon` and `headerActions` slot, grip dots on drag dividers — all consistent with Win7 Aero aesthetic.
  5. All pure-logic functions (`distributePx`, `shareTransferOnCollapse/Expand`, `computeAvailablePx`, `clampPanelDividerPx`) have unit tests that run without a Compose runtime, following the `SplitClampTest`/`AccordionToggleTest` TDD pattern.

**Build order within Phase 13 (non-negotiable — inherited from research SUMMARY.md):**

| Step | Name | Gate before proceeding |
|------|------|------------------------|
| 1 | Animation-vs-drag SPIKE (mandatory gate) | Drag writes `sizePx` instantly; toggle animates 200ms; collapse-then-immediate-drag produces no snap-back or oscillation. PNL-PITFALL-01 resolved. |
| 2 | Pure logic + TDD — `PanelDistribution.kt` + `PanelGroupLogicTest.kt` | All tests GREEN; covers `distributePx`, `shareTransferOnCollapse/Expand`, `computeAvailablePx`, `clampPanelDividerPx`, `lastExpandedFraction` restore. Pitfalls covered: PNL-PITFALL-04, PNL-PITFALL-05, PNL-PITFALL-06, PNL-PITFALL-07, PITFALL-A, PITFALL-B. |
| 3 | Layout skeleton (no animation, no drag) | `BoxWithConstraints` + `mutableStateListOf` fraction state; window resize redistributes heights correctly; `key(section.id)` in render loop (PNL-PITFALL-08); no drag divider on collapsed boundary (PNL-PITFALL-09); no `animateContentSize` (PNL-PITFALL-10); `pointerInput(Unit)` + `rememberUpdatedState` (FIXSP-01 carry-forward). |
| 4 | Collapse/expand animation | 200ms `FastOutSlowInEasing` via `animateFloatAsState`; concurrent animations on multiple sections do not conflict; mid-drag collapse race guarded by dragging flag (PNL-PITFALL-03). |
| 5 | Drag resize | `aeroDragSplitter` + `clampPanelDividerPx` (N-section Σminima clamp); instant px writes; no snap-back after window resize during drag; `rememberUpdatedState(totalPx)` mandatory (PITFALL-A). |
| 6 | Controlled expansion path + KDoc | Both controlled/uncontrolled branches present per `AeroAccordion` pattern; KDoc references REQ-IDs and PITFALLs; do-not-collapse-to-one-branch comment in place (PNL-08). |
| 7 | Aero visual polish | `glassPanel` header; `AeroIcons.CaretRight` 0°→90° rotation; `headerActions` slot; grip dots on `PanelGroupDivider`; `collapsible=false` hides chevron (PNL-11); `resizable=false` strips grip and disables drag (PNL-12); three-theme visual check. |
| 8 | Showcase demo + sign-off | `LayoutSection.kt` updated with `AeroPanelGroup` demo; three-theme visual sign-off PASSED on AeroBlue / AeroDark / Classic. |

**Key pitfalls to watch (from SUMMARY.md research):**
- PNL-PITFALL-01: `animateFloatAsState` vs. direct drag writes — spike resolves this first.
- PNL-PITFALL-04: N-section cascading clamp crash (`coerceIn(min>max)` throw) — TDD RED before fix.
- PITFALL-A: `remember(totalPx)` re-key resets state on window resize — use `remember { mutableStateListOf(...) }` with no key.
- FIXSP-01 carry-forward: stale capture in drag lambda — `rememberUpdatedState` mandatory.
- PNL-PITFALL-06: `lastExpandedPx` overflow after window shrink — store `lastExpandedFraction`, restore as fraction of current `availableForExpanded`.
- PNL-PITFALL-08: section state re-key on list reorder — `key(section.id)` in render loop.

**No new Gradle dependencies required.** All APIs (`BoxWithConstraints`, `aeroDragSplitter`, `clampDividerPx`, `animateFloatAsState`, `glassPanel`, `AeroIcons.*`) are already on the compile classpath.

**Plans**: 5 plans (one per build-order spine; spike + pure-logic GREEN gate all Compose work)
- [ ] 13-01-PLAN.md — Animation-vs-drag SPIKE (mandatory gate, PNL-PITFALL-01)
- [ ] 13-02-PLAN.md — Pure-logic TDD: PanelDistribution.kt + PanelGroupLogicTest.kt (RED→GREEN)
- [ ] 13-03-PLAN.md — Layout skeleton + 200ms collapse/expand animation (scope-DSL, fraction state)
- [ ] 13-04-PLAN.md — Drag resize (N-section clamp) + hybrid controlled/uncontrolled + KDoc
- [ ] 13-05-PLAN.md — Aero visual polish + showcase demo + three-theme sign-off

## Progress

**Execution Order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13

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
| 13. AeroPanelGroup | v2.0.2 | Complete    | 2026-06-23 | 2026-06-23 |
| 13.1. AeroPanelGroup horizontal orientation variant | 2/3 | In Progress|  | — |

---

*Roadmap last updated: 2026-06-23 — Phase 13.1 planned (3 plans, horizontal orientation variant)*

### Phase 13.1: AeroPanelGroup horizontal orientation variant (INSERTED)

**Goal:** Add a horizontal orientation variant to the shipped `AeroPanelGroup` (PNL-HORIZ-01): N sections as side-by-side columns, vertical dividers, drag resizes WIDTH, collapsed column = thin ~36dp vertical header strip — a 90° rotation of the verified Phase 13 vertical model with ZERO breaking changes to the v2.x API and ZERO regression to the vertical behavior. Delivered by refactoring `AeroPanelGroup.kt` into a shared internal core (`AeroPanelGroupImpl(orientation, ...)`) plus an additive `orientation: Orientation = Orientation.Vertical` public default param.
**Requirements**: PNL-HORIZ-01 (primary); PNL-01..PNL-18 (inherited behavior preserved through the core-extraction refactor — regression-verified)
**Depends on:** Phase 13
**Plans:** 2/3 plans executed

Plans:
- [ ] 13.1-01-PLAN.md — Extract vertical core to internal `AeroPanelGroupImpl(orientation, ...)` + additive `orientation` param; orientation-aware `PanelGroupDivider` (regression firewall — vertical body & 12 logic tests unchanged/GREEN)
- [ ] 13.1-02-PLAN.md — Horizontal orientation branch: `Row` container, `maxWidth` axis, axis-swapped modifiers, rotated header strip + 0°/180° chevron, horizontal drag, KDoc (PNL-HORIZ-01)
- [ ] 13.1-03-PLAN.md — Append-only horizontal showcase demo + vertical regression gate + three-theme sign-off on BOTH demos (PNL-17, PNL-HORIZ-01)
