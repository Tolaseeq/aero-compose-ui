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
- 🚧 **v3.1 Dependency Refresh + Hot Reload MCP** — Phase 21 (in progress) — forced upgrade to latest stable toolchain/deps, Compose Hot Reload + MCP server for agent-driven showcase QA, `3.1.0` on JitPack

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

### 🚧 v3.1 Dependency Refresh + Hot Reload MCP (Phase 21) — IN PROGRESS

**Milestone Goal:** Move the whole project onto the latest stable dependency/toolchain versions, prove nothing broke, and ship `3.1.0` on JitPack. Compose Hot Reload + its MCP server are installed along the way as the agent's own GUI-inspection tool — a means, not a deliverable.

**Global stop rule:** if any upgrade or JBR setup needs more than a version bump plus mechanical renames forced by the new API, or the MCP moves the real cursor / steals input focus — stop and ask the maintainer. No workarounds.

**Size:** one migration = one phase (maintainer's decision). Attributability of upgrade regressions comes from separately committed, separately test-gated steps inside the phase, not from extra phases.

- [ ] **Phase 21: Migration + Release 3.1.0** - Pre-upgrade baseline, every dependency and the toolchain on latest stable (each bump its own test-gated commit), Hot Reload + MCP installed as the inspection tool, no drift against the baseline, `3.1.0` on JitPack

## Phase Details

### Phase 21: Migration + Release 3.1.0

**Goal**: Every dependency and toolchain piece sits on its latest stable version (Gradle 9.7.1, JDK 21, Kotlin 2.4.20, Compose Multiplatform 1.12.0, kotlinx-coroutines 1.11.0, kotlinx-datetime 0.8.0 plain, JUnit 6.1.3; Material3 stays pinned at stable 1.9.0), it is demonstrated against a pre-upgrade baseline that nothing broke, and `3.1.0` is published on JitPack. Compose Hot Reload + MCP is installed in `:showcase` as the tool the agent uses to inspect the GUI itself (`.planning/research/MCP-HOWTO.md`).
**Depends on**: Nothing (only phase of v3.1)
**Requirements**: BASE-01, BASE-02, BASE-03, BASE-04, BASE-05, TOOL-09, TOOL-10, TOOL-11, TOOL-12, TOOL-13, TOOL-14, TOOL-15, TOOL-16, TOOL-17, HRM-01, HRM-02, HRM-03, VER-07, VER-08, VER-09, VER-10, REL-03, REL-04, REL-05
**Execution order inside the phase** (each step is its own commit; every upgrade step is gated by a full test run before the next):

  1. Baseline on the current toolchain (Kotlin 2.4.10 / CMP 1.11.1): theme + section launch parameter, `PrintWindow` capture helper, noise characterization, section × theme frames, UI-test state captures. The first UI-test step proves `captureToImage` works in desktop `runComposeUiTest` — if not, stop and ask
  2. Lock the literal test count on the old toolchain and prove the count guard fails red on a deliberately excluded test class
  3. Gradle 9.7.1 + JDK 21 → Kotlin 2.4.20 + CMP 1.12.0 (+ Material3 `dependencyInsight` check)
  4. Hot Reload 1.2.0 + `.mcp.json` in `:showcase` — one commit. **Human action:** the maintainer restarts Claude Code so the MCP server connects
  5. kotlinx-coroutines 1.11.0 → kotlinx-datetime 0.8.0 → JUnit 6.1.3
  6. Post-upgrade captures (MCP + `PrintWindow`) and UI-test frames compared with the baseline; unconfirmed list; hand-off to the maintainer only after the agent's own sweep
  7. Throwaway JitPack verify tag → README consumer floor → `3.1.0` + `v3.1.0` tag

**Success Criteria** (what must be TRUE):

  1. Reference frames for every showcase section × 3 themes and UI-test images of hover / press / keyboard focus / drag (interactive v3.0 glass components + `AeroSplitPane`, `AeroPanelGroup`, `AeroDataTable` column resize) exist outside `build/`, were taken on the old toolchain with a helper that never touches the real cursor or input focus, and every noisy region is named
  2. Every target version is in, each bump landed as its own commit with the full suite green at exactly the test count locked before the upgrade (guard proven red first); `dependencyInsight --dependency material3` shows no alpha on either module, including after Hot Reload is added; picker behavior and tests unchanged; the showcase builds and runs; published bytecode is class-file 65 with `org.gradle.jvm.version = 21`
  3. Hot Reload + MCP sits in `:showcase` only (`:library`'s POM / module metadata unchanged), connects after the Claude Code restart and sees the showcase; the agent has measured itself that capture, tree dump and click leave the cursor position and foreground window unchanged, including with the showcase covered or minimized — reported as a short fact, never using the server's `take_screenshot`
  4. Post-upgrade frames and UI-test images are compared with the baseline: every difference outside the named noisy regions is explained with a stated cause or fixed; everything confirmed by neither method is listed separately as "unconfirmed"; the maintainer sees the GUI only after the agent's own sweep, together with the frames and both lists
  5. A throwaway verify tag builds green on JitPack under JDK 21 / Gradle 9.7.1 before the real tag exists; README states the new consumer floor and fixes the stale toolchain line; **outward-facing, needs the maintainer's confirmation at execution time:** `3.1.0` is set in `build.gradle.kts`, `v3.1.0` is pushed, the JitPack build is `ok`, and `com.github.Tolaseeq:aero-compose-ui:3.1.0` resolves

**Plans:** 9/14 plans executed

Plans:
**Wave 1**

- [x] 21-01-PLAN.md — Step 1: showcase section/page/capture launch parameters (BASE-01) + PrintWindow capture helper, sweep driver, covered/minimized self-test (BASE-02)
- [x] 21-02-PLAN.md — Step 1: captureToImage proof-of-work, opt-in UI capture writer, hover/press/focus/drag capture tests for 10 components x 3 themes (BASE-05)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 21-03-PLAN.md — Step 1: opened-popup capture tests for all 13 Popup components x 3 themes, fixed picker values, D-08 classification (D-07, D-09)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 21-04-PLAN.md — Step 1: pre-upgrade baseline — all sections x 3 themes twice, named noise regions, UI-test reference images, comparison tool (BASE-03, BASE-04, BASE-05)

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 21-05-PLAN.md — Step 2: lock the live test count and prove the count guard red (TOOL-16)

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 21-06-PLAN.md — Step 3: Gradle 9.7.1 + stale repo removal → JDK 21 → Kotlin 2.4.20 + CMP 1.12.0, Material3 gate, showcase launch (TOOL-09..12, TOOL-17)

**Wave 6** *(blocked on Wave 5 completion)*

- [x] 21-07-PLAN.md — Step 4: Compose Hot Reload 1.2.0 + .mcp.json in :showcase only, isolation checks; maintainer restarts Claude Code (HRM-01)

**Wave 7** *(blocked on Wave 6 completion)*

- [x] 21-08-PLAN.md — Step 4: MCP connected to the hotRun showcase, test tags, reload finding, cursor/focus non-interference measured (HRM-02, HRM-03)

**Wave 8** *(blocked on Wave 7 completion)*

- [x] 21-09-PLAN.md — Step 5: kotlinx-coroutines 1.11.0 → kotlinx-datetime 0.8.0 (compiler-decided renames) → JUnit 6.1.3 (TOOL-13..15)

**Wave 9** *(blocked on Wave 8 completion)*

- [ ] 21-10-PLAN.md — Step 6: post-upgrade showcase and UI-test captures compared with the baseline, agent review (VER-07, VER-08)

**Wave 10** *(blocked on Wave 9 completion)*

- [ ] 21-11-PLAN.md — Step 6: after-only MCP inspection (D-08), drift list + unconfirmed list + offline hand-off page, single D-04 stop if drift (VER-09)

**Wave 11** *(blocked on Wave 10 completion)*

- [ ] 21-12-PLAN.md — Step 6: apply D-04 rulings, hand-off of the GUI to the maintainer after the agent's sweep (VER-10)

**Wave 12** *(blocked on Wave 11 completion)*

- [ ] 21-13-PLAN.md — Step 7: throwaway JitPack verify tag on JDK 21 / Gradle 9.7.1, published bytecode check, README consumer floor (REL-03, REL-04)

**Wave 13** *(blocked on Wave 12 completion)*

- [ ] 21-14-PLAN.md — Step 7: 3.1.0 + v3.1.0 after the maintainer's confirmation, JitPack ok, coordinate resolves (REL-05)

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
| 21. Migration + Release 3.1.0 | v3.1 | 9/14 | In Progress|  |

## Next Milestone

Not yet scoped. Candidate goals are tracked in `.planning/PROJECT.md` § "Next Milestone Goals" (v3.0 debt, VIS-F01 visual sweep of the remaining ~40 components, `AeroPanelGroup` reorder/nest/keyboard-resize, `AeroDropdown` popup-offset carry-over). Phase numbering will continue from **22** once v3.1 ships.

---

*Roadmap last updated: 2026-09-23 — v3.1 Dependency Refresh + Hot Reload MCP roadmapped (one phase, 21 — one migration = one phase per the maintainer; 24/24 requirements mapped, plans TBD). Previous: 2026-07-29 after v3.0 Glass Refinement shipped and archived (Phases 15–20, 41 plans, 57/57 requirements).*
