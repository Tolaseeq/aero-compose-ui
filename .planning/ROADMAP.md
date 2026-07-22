# Roadmap: aero-compose-ui

A Compose Desktop UI component library styled after Windows Aero (Windows 7): glass gradient surfaces, three themes, custom window chrome, a typed `AeroIcons` set, and a growing showcase. Published as a Maven/JAR artifact (`com.mordred:aero-compose-ui`).

## Milestones

- ✅ **v1.0 MVP** — Phases 1–3 (shipped 2026-04-28) — Foundation + Atomic Components + Composite/Navigation
- ✅ **v1.1 Icon System** — Phases 4–6 (shipped 2026-04-30) — 138 `AeroIcons`, dependency removal, IconsSection
- ✅ **v2.0 Stateful + Layout** — Phases 7–11 (shipped 2026-06-18) — 12 stateful + layout components, showcase sign-off
- ✅ **v2.0.1 Picker & SplitPane Fixes** — Phase 12 (shipped 2026-06-22) — 2 bug fixes + `AeroDateTimeRangePicker`
- ✅ **v2.0.2 AeroPanelGroup** — Phases 13 + 13.1 (shipped 2026-06-23) — N-section collapsible+resizable layout, vertical + horizontal orientations
- ✅ **v2.0.4 PanelGroup Recompose Fix** — Phase 14 (shipped 2026-06-26) — horizontal-controlled recompose-during-drag duplication fix (real root cause: non-`@Composable` DSL); v2.0.3 was a superseded wrong-cause release
- 🚧 **v3.0 Glass Refinement** — Phases 15–20 (in progress) — toolchain migration (Kotlin 2.4.10 / CMP 1.11.1), repaired Aero primitives layer, eight components restyled to genuine Win7 glass

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:

- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`
- `.planning/milestones/v2.0.1-ROADMAP.md`
- `.planning/milestones/v2.0.2-ROADMAP.md`
- `.planning/milestones/v2.0.4-ROADMAP.md`

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

### 🚧 v3.0 Glass Refinement (In Progress)

**Milestone Goal:** Restyle eight Material3-looking components (`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`) into genuinely Aero-glass surfaces — on a repaired and extended shared Aero-primitives layer, on top of a migrated, current-stable Compose Multiplatform toolchain. Public API and behavior do not change. Major version because the mandatory toolchain migration raises the minimum Kotlin/Compose for all consumers.

**Phase numbering continues from 15** (project shipped through Phase 14 / v2.0.4).

- [ ] **Phase 15: Toolchain Upgrade** — Kotlin 2.4.10 + Compose Multiplatform 1.11.1, isolated from all visual work
- [ ] **Phase 16: Foundation — Aero Primitives Layer** — repaired `GlassModifiers`, `AeroSurfaceStyle`/`AeroSurfacePrimitives`, `AeroOrnamentTokens`, shared thumb/groove primitives, full-library smoke pass
- [ ] **Phase 17: Buttons** — `AeroButton`, `AeroOutlinedButton` restyled with shared internal surface
- [ ] **Phase 18: Range** — `AeroSlider` (M3 slots), `AeroRangeSlider`, `AeroProgressBar` restyled
- [ ] **Phase 19: Selectors + Lists** — `AeroSwitch`, `AeroSegmentedControl`, `AeroListItem` restyled
- [ ] **Phase 20: Verification** — showcase wiring, grep-gates, three-theme sign-off (incl. non-100% DPI pass)

## Phase Details

### Phase 15: Toolchain Upgrade

**Goal**: The library builds, tests, and ships on Kotlin 2.4.10 + Compose Multiplatform 1.11.1 (or an explicitly user-approved fallback), with zero visual-code changes mixed in — so any later regression is unambiguously attributable to either the toolchain or the visual work, never both.
**Depends on**: Phase 14 (v2.0.4, last shipped state)
**Requirements**: TOOL-01, TOOL-02, TOOL-03, TOOL-04, TOOL-05, TOOL-06, TOOL-07, TOOL-08
**Success Criteria** (what must be TRUE):

  1. `./gradlew build` succeeds on Kotlin 2.4.10 + Compose Multiplatform 1.11.1 — or, if that untested pairing fails, one of the three named fallbacks is selected only after escalating to the user (never substituted silently)
  2. `compose.material3` resolves to an explicitly pinned stable coordinate, not the alpha the bare alias would silently resolve to
  3. `AeroPanelGroupRecomposeUiTest` is ported to the CMP 1.11 test-infrastructure changes and is re-proven to FAIL when the non-`@Composable` DSL fix is temporarily reverted, then passes again once restored — a ported-but-inert guard is not acceptable
  4. The full library test suite (232+ tests, including all 12 `PanelGroupLogicTest`) is green, and the showcase compiles, launches, and smoke-runs on all three themes with no observable change from the pre-migration baseline
  5. A `dropShadow`/`innerShadow` scratch composable compiles against the real 1.11.1 artifact (not just documentation), and the JitPack build passes on the new toolchain

**Plans**: 5/6 plans executed

Plans:

- [x] 15-01-PLAN.md — Pre-migration baseline capture (before-screenshots, 3 themes) [Wave 1]
- [x] 15-02-PLAN.md — Build gate + stable Material3 1.9.0 pin (HARD STOP + escalation) [Wave 2]
- [x] 15-03-PLAN.md — RCMP test port + re-proof (fail-on-unfixed) [Wave 3]
- [x] 15-04-PLAN.md — Full 232-test suite + showcase smoke & baseline diff [Wave 4]
- [x] 15-05-PLAN.md — dropShadow/innerShadow scratch composable (Phase 16 handoff) [Wave 3]
- [ ] 15-06-PLAN.md — JitPack release proof (throwaway pre-release tag) [Wave 5]

### Phase 16: Foundation — Aero Primitives Layer

**Goal**: A single, shared, three-theme-proven Aero drawing/token layer exists so every visual component phase consumes it rather than re-deriving gradients, gloss, bevel, and grooves independently.
**Depends on**: Phase 15
**Requirements**: PRIM-01, PRIM-02, PRIM-03, PRIM-04, PRIM-05, PRIM-06, PRIM-07, PRIM-08, PRIM-09, PRIM-10, PRIM-11, PRIM-12, PRIM-13, PRIM-14, PRIM-15, PRIM-16, PRIM-17, PRIM-18
**Success Criteria** (what must be TRUE):

  1. `Color.lighten()`/`darken()` exist and `AeroOrnamentTokens.derive(base)` produces algorithmically-derived ornament tokens for all three themes, reachable through an additive `ornamentOverride` escape hatch on `AeroColorScheme` with no breaking change to existing constructor calls
  2. A single `drawAeroSurfaceCore(style, cornerPx)` backs `Modifier.aeroSurface()` (Box-owning components), a direct-call path (Canvas-owning components), `Modifier.aeroGlowRing` (hover/focus), and a raised-thumb + recessed-track-groove primitive pair — one implementation exposed multiple ways, not independent copies
  3. `GlassModifiers.kt`'s three confirmed defects are fixed: `glassSurface`'s gloss is proportional to component height (no `endY = 100f` literal), its border renders at full declared thickness (clip-order fixed), and `glassEffect(elevation)` either draws a real shadow or the dead parameter is removed
  4. Every new gradient fades toward `baseColor.copy(alpha = 0f)` (never hardcoded `Color.Transparent`) and is spot-checked on AeroBlue/AeroDark/Classic at first implementation, not deferred; geometry/brushes are built in `drawWithCache`, not rebuilt per frame
  5. A full-library smoke pass across all ~50 components (not just the eight targets) shows no regression from the `GlassModifiers.kt` fixes, `rememberAeroInteractionState()` is available from `components/common/`, and the M3 `Slider` thumb/track slot-sizing spike either confirms custom-sized slots fit cleanly or is documented as failed with the `AeroSlider` fallback noted

**Plans**: TBD

Plans:

- [ ] 16-01: TBD (planned via `/gsd:plan-phase 16` — likely needs `/gsd:research-phase` per research flag; includes dropShadow/innerShadow spike and M3 Slider slot-sizing spike as exit items)

### Phase 17: Buttons

**Goal**: `AeroButton` and `AeroOutlinedButton` read as genuine Aero glass controls with correct hover/press/focus/disabled states, while keeping keyboard activation and button semantics.
**Depends on**: Phase 16
**Requirements**: VBTN-01, VBTN-02, VBTN-03, VBTN-04, VBTN-05, VBTN-06
**Success Criteria** (what must be TRUE):

  1. `AeroButton` renders a two-tone gradient fill with a visible seam, proportional top gloss, inner bevel, and outer contour
  2. Hover shows a glow, press inverts the gradient, focus is visible, and disabled reads distinctly — all in Aero idiom, and the hover overlay is clipped to the button's rounded shape (no square corners bleeding past the corner radius)
  3. `Role.Button` semantics and keyboard activation (Space/Enter) still work after the M3 container is dropped, via `Modifier.clickable(role = Role.Button, ...)` — not a zero-semantics hand-roll
  4. `AeroOutlinedButton` shows the equivalent outlined-variant treatment and cannot visually drift from `AeroButton`, because both consume one shared internal surface composable

**Plans**: TBD

Plans:

- [ ] 17-01: TBD (planned via `/gsd:plan-phase 17` — standard pattern, plan directly)

### Phase 18: Range

**Goal**: `AeroSlider`, `AeroRangeSlider`, and `AeroProgressBar` show recessed track grooves and raised, glossy thumbs/fills, with zero regression to slider drag/keyboard/step behavior.
**Depends on**: Phase 16 (consumes Buttons' pressed-fill code only indirectly via Foundation; no hard Phase 17 dependency)
**Requirements**: VRNG-01, VRNG-02, VRNG-03, VRNG-04, VRNG-05, VRNG-06, VRNG-07, VRNG-08, VRNG-09
**Success Criteria** (what must be TRUE):

  1. `AeroSlider` keeps Material3's `Slider` and supplies custom `thumb =`/`track =` slots showing a recessed groove and a raised, glossy thumb with hover/focus — the M3 container is not removed
  2. `AeroSlider`'s drag, keyboard-arrow nudge, `steps` snapping, `onValueChangeFinished`, and semantics all behave identically to before the restyle
  3. `AeroRangeSlider` shows the same groove + raised-thumb treatment with independent hover/press per thumb, while its existing drag logic is untouched
  4. `AeroProgressBar` shows a recessed track bed and a gradient fill with gloss; the periodic sheen is present but OFF by default; indeterminate mode is restyled but keeps its existing 1500ms restart timing with no ping-pong introduced
  5. Anywhere animation and drag write the same value (slider/range-slider thumbs), the locked Pattern 3 is reused: animation reads a target-only value, drag writes directly, `isDragging` switches to `snap()`

**Plans**: TBD

Plans:

- [ ] 18-01: TBD (planned via `/gsd:plan-phase 18` — likely needs `/gsd:research-phase` per research flag)

### Phase 19: Selectors + Lists

**Goal**: `AeroSwitch` and `AeroSegmentedControl` gain their first-ever hover/press/focus states and Aero volume, and `AeroListItem`'s selection highlight is finally clipped to a proper Aero pill with a visible focus state.
**Depends on**: Phase 17 (`AeroSegmentedControl` reuses the pressed-button fill code), Phase 18 (`AeroSwitch`'s thumb/groove primitives are built and proven by Range)
**Requirements**: VSEL-01, VSEL-02, VSEL-03, VSEL-04, VLST-01, VLST-02, VLST-03, VLST-04
**Success Criteria** (what must be TRUE):

  1. `AeroSwitch` shows a recessed track groove and a raised thumb with gloss and shadow, replacing the current fully-flat rendering
  2. `AeroSwitch` has working hover, press, and focus states for the first time (currently absent entirely)
  3. `AeroSegmentedControl`'s selected segment appears recessed (inverted gradient + inner shadow) by reusing the pressed-button code from Phase 17, and the control gains hover and focus for the first time
  4. `AeroListItem`'s selection highlight is clipped to a rounded pill with gradient and rim light, hover remains visible on an already-selected row (the two states combine instead of one suppressing the other), a focus visual exists, and all newly-hover-wired components reuse `AeroListItem`'s existing `Modifier.hoverable` + `collectIsHoveredAsState` pattern rather than inventing pointer-position tracking

**Plans**: TBD

Plans:

- [ ] 19-01: TBD (planned via `/gsd:plan-phase 19` — standard pattern, plan directly)

### Phase 20: Verification

**Goal**: The milestone's visual work is demonstrated, mechanically gated, and human-approved across all three themes before shipping — closing the loop the v2.0.3 false-positive-sign-off lesson demands.
**Depends on**: Phase 17, Phase 18, Phase 19
**Requirements**: SHW-15, SHW-16, VER-01, VER-02, VER-03, VER-04, VER-05, VER-06
**Success Criteria** (what must be TRUE):

  1. The showcase demonstrates all eight restyled components in every state (default/hover/press/focus/disabled where applicable)
  2. A human three-theme sign-off (AeroBlue / AeroDark / Classic) passes, including at least one pass at a non-100% DPI scale
  3. Both new grep-gates (no pixel literals in gradient stops; no bypass of `aeroSurface()`'s centralized clip order) are proven to FAIL on deliberately-broken code before being proven to pass on the real code — matching VER-06's "provably fail on unfixed code" requirement, not "run the test"
  4. A snapshot test confirms component default sizes and corner radii match the pre-migration baseline (no silent layout creep)
  5. A UI test confirms keyboard activation works for both converted buttons, and a minimal scratch-consumer outside the showcase's own conventions builds against the new artifact

**Plans**: TBD

Plans:

- [ ] 20-01: TBD (planned via `/gsd:plan-phase 20` — standard pattern, mirrors v2.0.2/v2.0.4 three-theme sign-off precedent)

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
| 14. PanelGroup Recompose Fix | v2.0.4 | 3/3 | Complete | 2026-06-26 |
| 15. Toolchain Upgrade | v3.0 | 5/6 | In Progress|  |
| 16. Foundation — Aero Primitives Layer | v3.0 | 0/TBD | Not started | - |
| 17. Buttons | v3.0 | 0/TBD | Not started | - |
| 18. Range | v3.0 | 0/TBD | Not started | - |
| 19. Selectors + Lists | v3.0 | 0/TBD | Not started | - |
| 20. Verification | v3.0 | 0/TBD | Not started | - |

## Next Milestone

🚧 **v3.0 Glass Refinement** in progress (Phases 15–20). Next: `/gsd:plan-phase 15`.

---

*Roadmap last updated: 2026-07-21 — v3.0 Glass Refinement roadmap created (Phases 15–20, 57/57 requirements mapped).*
