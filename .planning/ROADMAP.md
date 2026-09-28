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
- ✅ **v3.1 Dependency Refresh + Hot Reload MCP** — Phase 21 (shipped 2026-09-24) — whole toolchain on latest stable (Gradle 9.7.1 / JDK 21 / Kotlin 2.4.20 / CMP 1.12.0), proven against a pre-upgrade baseline, Compose Hot Reload + MCP for agent-driven showcase QA, `3.1.0` on JitPack
- 🚧 **v3.2 Native Window Behavior** — Phase 22 (in progress) — native Windows snap/hit-testing (Aero Snap, Snap Layouts, Snap Groups, taskbar-aware maximize, DPI-aware multi-monitor) for windows on `AeroTitleBar` via a JNA `WndProc` subclass on a standard JDK 21, release `v3.2.0`

Full ship-time snapshots (milestone goal, all phase details, decisions, tech debt) are archived per milestone:

- `.planning/milestones/v1.1-ROADMAP.md` (also captures v1.0 phase definitions)
- `.planning/milestones/v2.0-ROADMAP.md`
- `.planning/milestones/v2.0.1-ROADMAP.md`
- `.planning/milestones/v2.0.2-ROADMAP.md`
- `.planning/milestones/v2.0.4-ROADMAP.md`
- `.planning/milestones/v3.0-ROADMAP.md`
- `.planning/milestones/v3.1-ROADMAP.md`

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

<details>
<summary>✅ v3.1 Dependency Refresh + Hot Reload MCP (Phase 21) — SHIPPED 2026-09-24</summary>

**Milestone Goal:** Move the whole project onto the latest stable dependency/toolchain versions, prove nothing broke, and ship `3.1.0` on JitPack. Compose Hot Reload + its MCP server installed along the way as the agent's own GUI-inspection tool — a means, not a deliverable.

- [x] **Phase 21: Migration + Release 3.1.0** — pre-upgrade `PrintWindow` + UI-test baseline, test count locked at 541 and proven red first, every bump its own gated commit (Gradle 9.7.1, JDK 21, Kotlin 2.4.20, CMP 1.12.0, coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3; Material3 still 1.9.0), Hot Reload + MCP in `:showcase` only with 27/27 non-interference measurements, 102 drift items explained and accepted, `v3.1.0` green on JitPack (14/14 plans, 2026-09-24)

**Closeout:** 24/24 requirements, verification passed. Captured at 100% DPI only — 125%/200% and the other ~40 components' interaction states stay on the unconfirmed list (VER-F01/F02). Tag-only release; published coordinate `com.github.Tolaseeq:aero-compose-ui:v3.1.0`.

Details: `.planning/milestones/v3.1-ROADMAP.md` · Requirements: `.planning/milestones/v3.1-REQUIREMENTS.md` · Phase artifacts: `.planning/milestones/v3.1-phases/` · Summary: `.planning/MILESTONES.md`
</details>

### 🚧 v3.2 Native Window Behavior (Phase 22) — IN PROGRESS

**Milestone Goal:** Windows treats windows built on `AeroTitleBar` (+ `AeroResizeHandles`) as native windows — edge/corner Aero Snap, Snap Layouts flyout, Win+arrow hotkeys, shared-border resize of a snapped pair, taskbar-aware maximize, correct behavior across 100%/150% DPI monitors, FancyZones — in every application built on the library. First consumer: Pinya (Phase 2, decision D-25) — its detachable queue window relies on Windows' own snapping and is waiting on `v3.2.0`.

**Global stop rule:** if the required behavior cannot be reached on `undecorated = true, transparent = false` on a standard JDK 21 (no JBR), work stops and the question goes to the maintainer. Switching to a decorated window, transparency, or a JBR-only API without that decision is not allowed.

**Size:** one phase (22) — one goal, one phase (maintainer's rule). The steps below are ordered, separately-committed steps inside that one phase, riskiest first — not separate phases.

**Real-input rule (carried from v3.1):** anything needing real mouse/keyboard goes through two maintainer-supervised sessions, each preceded by a warning and an explicit "ok" and closed with "you can come back": an early 1–2 minute session right after the first draft (does Windows even show the Snap Layouts flyout and snap a Compose window at all — if not, stop and ask before building anything further), and a full session at the end of the phase covering everything that has no message-based, headless-provable trigger.

- [ ] **Phase 22: Native Window Behavior + Release 3.2.0** - A JNA `WndProc` subclass answers Windows' hit-testing/frame/non-client questions for `AeroTitleBar` windows so Snap, Snap Layouts, Snap Groups, taskbar-aware maximize and FancyZones all work natively; verified live on Windows 11 on both a standard JDK 21 and JBR 21; `v3.2.0` released on JitPack

## Phase Details

### Phase 22: Native Window Behavior + Release 3.2.0

**Goal**: Windows treats every window built on `AeroTitleBar`/`AeroResizeHandles` as a native window — snapping, Snap Layouts, hotkeys, shared-border resize, taskbar-aware maximize, multi-monitor DPI moves, FancyZones — on a standard JDK 21 without JBR, and the behavior is released as `v3.2.0`.
**Depends on**: Nothing (only phase of v3.2)
**Requirements**: SNAP-01, SNAP-02, SNAP-03, SNAP-04, SNAP-05, SNAP-06, SNAP-07, WIN-01, WIN-02, WIN-03, WIN-04, WIN-05, WIN-06, BTN-01, BTN-02, API-01, API-02, API-03, API-04, DEP-01, SHW-17, VER-11, VER-12, VER-13, VER-14, REL-06, REL-07, REL-08

**Conflicts to settle empirically** (`.planning/research/SUMMARY.md` § "Conflicts to Settle Empirically" — resolve each by a cheap first-hand check during execution, not by more desk research):

  1. Which style bits `undecorated = true` actually yields on JDK 21 — read back `GetWindowLongPtr(hwnd, GWL_STYLE)` on the unmodified window before writing any hit-test code
  2. Whether Alt+Space is automatic once `WS_SYSMENU`/`WS_CAPTION` are present, or needs explicit `GetSystemMenu`/`TrackPopupMenu` handling — and specifically whether Move/Size work once hit-testing changes
  3. Auto-hide taskbar inset size (1px vs Windows Terminal's 2px) and per-edge detection method (`SHAppBarMessage(ABM_GETAUTOHIDEBAREX)`)
  4. Whether `WindowState.placement` stays in sync automatically once `WM_SIZE` is forwarded via `CallWindowProc`, or needs an explicit push from the WndProc on OS-native maximize/restore/snap transitions
  5. Maximize-button interaction: a direct native → `State<Boolean>` EDT-hop bridge (`AeroMaxButtonInteraction`, try first) vs. FlatLaf-style re-injection of non-client mouse messages as ordinary client messages (fallback only)
  6. Whether `DwmExtendFrameIntoClientArea`/`DWMWA_WINDOW_CORNER_PREFERENCE` are needed at all, or rounded corners/shadow come free once `WS_CAPTION` is kept — resolve with a headless `SC_MAXIMIZE`/`SC_RESTORE` capture comparison before writing any DWM-attribute code
  7. Whether JBR's own custom-window-decoration machinery interferes with the subclass under `:showcase`'s `hotRun` — grep for existing `JBR`/`CustomWindowDecoration` usage first, then compare a cold `run` against `hotRun`

**Execution order inside the phase** (ordered, separately-committed steps, riskiest first — from `.planning/research/SUMMARY.md` § "Implications for Roadmap"; each step lands its own commit under the existing test suite):

  1. **Spike (existential-risk gate).** Minimal JNA `WindowProc` subclass on a standard JDK 21 (no JBR): read back the unmodified style bits (conflict #1), add the missing `WS_CAPTION | WS_THICKFRAME` (without them Snap silently does nothing), answer `HTCAPTION` over the title row except one hardcoded `HTMAXBUTTON` rect. Settles conflicts #1, #4, #7. In the early real-input session, hover the hardcoded rect and drag the window by its title to a screen edge: the Snap Layouts flyout must appear and the window must snap — a correct `HTMAXBUTTON` answer alone does not prove it; if either fails, stop and report before building anything further. Wires the JNA callback-lifetime container and the "unhandled → `DefWindowProc`" fallback as structural invariants from the start
  2. **Window styles.** Make the spike's style change production-grade: ensure `WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX` via `SetWindowLongPtr(GWL_STYLE, ...)` + `SetWindowPos(..., SWP_FRAMECHANGED)`; verify via read-back before any hit-test debugging (WIN-03)
  3. **Frame removal.** `WM_NCCALCSIZE`: 0 inset for the floating state, DPI-scaled frame-thickness inset when maximized (avoids the taskbar-overhang bug), plus the auto-hide-taskbar edge margin from conflict #3 (WIN-01, WIN-03)
  4. **Region registry + real hit-testing.** `HitTestRegionRegistry`/`HitTestSnapshot`, `onGloballyPositioned` wiring in `AeroTitleBar` for caption/button/interactive (`leading`) rects, replacing Step 1's hardcoded rect (SNAP-01..07, BTN-02, API-02)
  5. **Maximize-button interaction bridge.** `TrackMouseEvent(TME_NONCLIENT | TME_LEAVE)` re-armed on every move, `AeroMaxButtonInteraction` state via an EDT hop, Compose's own hover/click suppressed for this one button only (BTN-01, conflict #5)
  6. **"Don't break what works" checkpoint.** Audit every unhandled message reaches `CallWindowProc`; re-run the full 541-test suite; confirm `WindowState.placement`/size/position, focus, minimize animation and Alt+Space are unchanged from before the phase (SNAP-05, WIN-05, conflict #2's remaining half)
  7. **`AeroResizeHandles` Windows no-op + edge/corner classification.** Native `WM_NCHITTEST` resize codes take over on Windows; the Compose-side `pointerInput` handlers stay unchanged on non-Windows; verified against the narrow ~300px window specifically (WIN-02, WIN-06)
  8. **Public API surface.** `nativeWindowManagement: Boolean = true` opt-out, `markAeroTitleBarInteractive()`, `rememberAeroWindowChrome()` — all additive, source-compatible; multi-window smoke test (main + narrow queue window, open-while-dragging, close-during-drag) (API-01..04, WIN-06, SHW-17)
  9. **DPI/multi-monitor + taskbar pass.** 100%/150% scaling via the temporarily installed virtual display, drag across differently-scaled monitors, snapped-pair shared border, Snap Groups and FancyZones smoke checks, visible-taskbar case with auto-hide temporarily disabled (WIN-01, WIN-04, SNAP-06, SNAP-07)
  10. **Tests + proof + release plumbing.** Headless unit tests (region registry, hit-test math, DPI-thickness formula), a live-window opt-in harness promoted from Step 1's spike script, showcase wiring (SHW-17), test-count guard bumped with a commit naming the reason, capture-based before/after proof, then the full real-input session (VER-11..13), README + KDoc updates (REL-06/07), `v3.2.0` release (REL-08, DEP-01)

**Success Criteria** (what must be TRUE):

  1. Сразу после первого черновика — ранняя сессия с настоящими мышью и клавиатурой (1–2 минуты, после предупреждения мейнтейнера и его «ок»): наведение на «развернуть» окна Compose показывает меню раскладок Windows 11, перетаскивание к краю прилепляет окно. Если нет — фаза останавливается и уходит вопрос мейнтейнеру, прежде чем строить что-либо дальше
  2. На каждом окне на `AeroTitleBar`/`AeroResizeHandles` (включая узкое ~300 px и несколько окон одновременно) снаппинг работает как у родного окна: прилипание к краю/углу с восстановлением при отрыве, Snap Layouts по наведению, Win+стрелки, двойной щелчок, Alt+Space с рабочим «закрыть» тем же путём, что Alt+F4, общая граница прилипшей пары, Snap Groups в панели задач, подхват FancyZones
  3. Развёрнутое окно занимает рабочую область монитора без перекрытия панели задач (в том числе автоскрываемой), окно без следов системной рамки, переход между мониторами 100% ↔ 150% не даёт скачков размера, а индикатор «развернуть/восстановить» и `WindowState.placement` совпадают с реальным состоянием окна; публичные вызовы `AeroTitleBar`/`AeroResizeHandles` компилируются без изменений, потребитель может пометить кликабельный элемент в шапке, полностью отключить нативное поведение для окна и получить то же поведение для своей шапки без `AeroTitleBar`, не-Windows ведёт себя как раньше
  4. Полная сессия в конце фазы с настоящими мышью и клавиатурой на живом Windows 11 проходит дважды — на обычном JDK 21 и на JBR 21 (витрина под Hot Reload) — и агент сам осматривает окна кадрами `PrintWindow` до того, как их увидит мейнтейнер
  5. Список «не подтверждено» явно называет всё, что не проверено, включая поведение на Windows 10 — не выдаётся за пройденное
  6. `v3.2.0` опубликован и резолвится на JitPack (`com.github.Tolaseeq:aero-compose-ui:v3.2.0`); README и KDoc обновлены, оговорка «Aero Snap limitation» снята; все существующие тесты зелёные под залоченным (и при необходимости поднятым именованным коммитом) числом

**Plans:** 22/26 plans executed

Plans:
**Wave 1**

- [x] 22-01-PLAN.md — Live-window probe (tools/winprobe), window-state reporter, RED baseline of the unmodified window (conflicts #1, #7a; findings F8–F11)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 22-02-PLAN.md — JNA 5.19.1 (DEP-01) + Step 1 spike: native WndProc subclass with GC-safe registry, passthrough, idempotency, child-HWND HTTRANSPARENT; conflicts #4, #7
- [x] 22-03-PLAN.md — Guarded real-input driver, UIA Snap Layouts watcher (D-04), scripted early gate, vetted session environment

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 22-04-PLAN.md — Step 1 existential gate: early real-input session (warn → "ok" → flyout + snap) — stop and ask on failure

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 22-05-PLAN.md — Steps 2–3: sticky frame styles, frame removal, taskbar-aware maximize with auto-hide inset (conflict #3 detection)
- [x] 22-08-PLAN.md — SHW-17 narrow second window fixture (own minimum, D-01) + probe multi-window geometry

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 22-06-PLAN.md — Corners/shadow: agent self-review, maintainer picks ВАРИАНТ A/B/C in the Visual Companion, DWM policy applied (conflict #6)

**Wave 6** *(blocked on Wave 5 completion)*

- [x] 22-07-PLAN.md — Step 4: immutable hit-test region registry, live regions from AeroTitleBar, WindowDraggableArea only on the legacy path

**Wave 7** *(blocked on Wave 6 completion)*

- [x] 22-09-PLAN.md — Step 5: maximize-button interaction bridge via EDT hop into the button's own interaction source (conflict #5, D-02)

**Wave 8** *(blocked on Wave 7 completion)*

- [x] 22-10-PLAN.md — Step 6: passthrough audit, WindowState sync (conflict #4 action), HWND churn hardening, GC / Hot Reload / UIA checks

**Wave 9** *(blocked on Wave 8 completion)*

- [x] 22-11-PLAN.md — Step 7: native edge/corner resize, AeroResizeHandles Windows no-op, D-01 minimum size on both paths

**Wave 10** *(blocked on Wave 9 completion)*

- [x] 22-12-PLAN.md — Step 8: nativeWindowManagement opt-out, markAeroTitleBarInteractive, permanent RED control, multi-window checks

**Wave 11** *(blocked on Wave 10 completion)*

- [x] 22-19-PLAN.md — Step 8 (cont.): public rememberAeroWindowChrome for custom title bars (API-04, D-05); AeroTitleBar rebuilt on it; live proof (wave 11, before 22-13)

**Wave 12** *(blocked on Wave 11 completion)*

- [x] 22-13-PLAN.md — Step 10a: headless unit, pixel-parity and custom-title-bar API tests with mutation proofs; locked test count raised by named commits (VER-14)
- [x] 22-14-PLAN.md — Full-session environment helpers and two-pass real-input check suite (dry-run proven)

**Wave 13** *(blocked on Wave 12 completion)*

- [x] 22-15-PLAN.md — Steps 9–10: full VER-12 session on JDK 21 and JBR 21 (consent → setup → both passes; conflicts #2, #3 reveal)

**Wave 14** *(blocked on Wave 13 completion)*

- [x] 22-16-PLAN.md — Teardown on the maintainer's word + VER-13 hand-off (final VER-11 green/red, unconfirmed list incl. Windows 10)

**Wave 15** *(blocked on Wave 14 completion)*

- [x] 22-17-PLAN.md — README "Windows window behavior" (incl. custom title bar API) + KDoc; "Aero Snap limitation" removed (REL-06, REL-07, API-04)

**Wave 16** *(blocked on Wave 15 completion)*

- [ ] 22-18-PLAN.md — Release 3.2.0: version bump, verify tag on JitPack (D-06), DEP-01 on the published POM, maintainer's choice, real tag (REL-08). *Executes AFTER the gap-closure round (22-20..22-26) per the maintainer's 2026-09-28 decision, despite its original wave number*

**Gap-closure waves 17–21** *(added 2026-09-28 from 22-VERIFICATION.md: 5 blocking gaps + 5 check-formula artifacts; `gap_closure: true` on every plan)*

**Wave 17**

- [x] 22-20-PLAN.md — SNAP-05 + SNAP-04 (blockers #2, #1): hand-declared GetSystemMenu/TrackPopupMenu + WM_SYSCOMMAND(SC_KEYMENU) menu handling; WM_NCLBUTTONDBLCLK at HTCAPTION → DefWindowProc; headless posted-message proofs + routing guards (VER-14)
- [x] 22-21-PLAN.md — JBR drift (blocker #5): headless JDK-vs-JBR probe battery on the current build, mined 22-15 traces, mechanism verdict, and the folded JBR-only real-input confirmation spec for 22-25
- [x] 22-22-PLAN.md — Check-formula fixes (W02 corner second axis, W02 floor height, S07 priority-grid geometry from applied-layouts.json, W06 ncdestroy trace window, S03-WIN-DOWN deterministic precondition) + a -Checks subset parameter for the short re-verification session

**Wave 18** *(blocked on Wave 17 completion)*

- [x] 22-23-PLAN.md — BTN-01 press-fill parity (blocker #4): headless diagnosis (press origin / emission timing vs C5's FlatLaf re-injection fallback), fix on the evidence-selected route, proof pack, guards

**Wave 19** *(blocked on Wave 18 completion)*

- [ ] 22-24-PLAN.md — SNAP-06 shared-border resize (blocker #3): mine the session NCCALCSIZE traces, headless border-geometry diagnosis (ownership, band coverage, the snapped-IsZoomed clamp suspect), fix per verdict, V11 regression battery

**Wave 20** *(blocked on Waves 17–19 completion)*

- [ ] 22-25-PLAN.md — Short re-verification session on both JVMs (affected checks only + the JBR drift rows + formula-fixed rows; headless V11 sweep first; NO virtual display — W04 stays UNCONFIRMED; PowerToys kept; honest measured-time estimate) — checkpoint:human-action, VER-12 protocol

**Wave 21** *(blocked on Wave 20 completion)*

- [ ] 22-26-PLAN.md — Post-fix README/KDoc from the new PASS rows (REL-06/07 update), requirement checkbox flips proven by the re-verification, 22-HANDOFF/22-UNCONFIRMED/22-NOTES consolidation; REL-08 stays with 22-18

**UI hint**: yes

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation | v1.0 | 4/4 | Complete | 2026-04-27 |
| 2. Atomic Components | v1.0 | 6/6 | Complete | 2026-04-28 |
| 3. Composite + Navigation | v1.0 | 8/8 | Complete | 2026-04-28 |
| 4. AeroIcons Foundation | v1.1 | 2/2 | Complete | 2026-04-29 |
| 5. Component Migrations + Dep Removal | v1.1 | 5/5 | Complete | 2026-04-29 |
| 6. Showcase IconsSection | v1.1 | 3/3 | Complete | 2026-04-30 |
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
| 21. Migration + Release 3.1.0 | v3.1 | 14/14 | Complete | 2026-09-24 |
| 22. Native Window Behavior + Release 3.2.0 | v3.2 | 22/26 | In Progress|  |

## Next Milestone

Not yet scoped. Candidate goals are tracked in `.planning/PROJECT.md` § "Next Milestone Goals" (v3.0 debt, VIS-F01 visual sweep of the remaining ~40 components, VER-F02 125%/200% DPI, VER-F03 external scratch consumer, `AeroPanelGroup` reorder/nest/keyboard-resize, `AeroDropdown` popup-offset carry-over). Phase numbering will continue from **23** once v3.2 ships.

---

*Roadmap last updated: 2026-09-28 — Phase 22 gap-closure round planned (plans 22-20..22-26, waves 17-21, from 22-VERIFICATION.md; the 22-18 release executes after gap closure per the maintainer's decision). Previous: 2026-09-25 — v3.2 Native Window Behavior roadmapped (one phase, 22 — one goal = one phase per the maintainer's rule; 27/27 requirements mapped, plans TBD).*
