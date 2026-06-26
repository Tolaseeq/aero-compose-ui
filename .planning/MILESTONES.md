# Milestones

## v2.0.4 PanelGroup Recompose Fix (Shipped: 2026-06-26)

**Phase:** 14 (1 phase) | **Plans:** 3 + post-release root-cause fix | **Requirements:** 8/8 (RCMP-01..04, REG-01..02, REL-01..02)

**Timeline:** 2026-06-25 → 2026-06-26 (single-day push, including a corrective release)
**Git range:** `fbba375` (v2.0.3 fix attempt) → `8d2170f` (real fix) → v2.0.4 tag
**Sign-off:** ✅ CONFIRMED in the real consumer app (exactly 3 sections under recompose-while-drag); 232 library tests pass incl. a deterministic programmatic-drag UI test; 12 PanelGroupLogicTest GREEN.

**Delivered:** Eliminated header-strip duplication in horizontal CONTROLLED `AeroPanelGroup` when a divider is dragged while the hosting screen recomposes. NOTE: the first attempt shipped as **v2.0.3** but fixed the WRONG cause (a write-during-composition theory: moved `expandedState` sync to `SideEffect`, derived `expandedArr` from `isExpanded()`) — the bug still reproduced in a real consumer. The real fix shipped as **v2.0.4**. v2.0.3 remains tagged but superseded; consumers use 2.0.4.

**Real root cause (confirmed by instrumented enters/disposes + sections.size logging):** `AeroPanelGroup`'s section-DSL lambda `content` was `@Composable`, so it had its own recompose scope. During an active drag (continuous re-measure) any parent recompose re-ran that lambda independently, re-appending `section()` into the SAME persisted `AeroPanelGroupScope` — `scope.sections` grew 3→9→12→…→33 and the `key(section.key)` loop emitted ever more header strips (none disposed). `AeroPanelGroupImpl` and `BoxWithConstraints` each composed exactly once; only the list grew. Trigger needs BOTH active-drag-with-movement AND an AeroPanelGroup recompose.

**Key accomplishments:**
1. **Real root cause found & fixed (RCMP-01..04, commit 8d2170f)** — made the DSL lambda non-`@Composable` (`content: AeroPanelGroupScope.() -> Unit`, mirroring `LazyListScope`). The collection pass runs exactly once per recompose and cannot accumulate; each section's own `content` slot stays `@Composable`. Source-compatible for existing `section(){...}` usage.
2. **Deterministic regression guard (the guard 14-02 could not be)** — new `AeroPanelGroupRecomposeUiTest` drives a real `performMouseInput` drag interleaved with a mid-drag recompose via `runComposeUiTest`; asserts exactly 1 header per section (observed 11 before the fix, 1 after). Added `compose.uiTest` + `compose.desktop.currentOs` test deps.
3. **Regression guard (REG-01..02)** — vertical + uncontrolled unaffected; 12 `PanelGroupLogicTest` GREEN; Compose 1.7.3, zero new runtime dependencies. Showcase compiles; RCMP-04 demo rewritten to read its tick in the demo body so it genuinely reproduces.
4. **Release (REL-01..02)** — `build.gradle.kts` bumped `2.0.3`→`2.0.4`; annotated tag `v2.0.4` pushed; JitPack build `ok`, resolves `com.github.Tolaseeq:aero-compose-ui:2.0.4`.

**Patterns established:**
- **Builder/DSL lambdas that side-effect into a collection must NOT be `@Composable`** — a `@Composable` collection lambda gets its own recompose scope and can re-run independently, re-appending into a persisted accumulator (mirrors why `LazyListScope` is non-composable). See `project_panelgroup_composable_dsl_pitfall` memory.
- **A regression guard must provably fail on the unfixed code** — the v2.0.3 showcase repro read its counter inside `section.content()` (deep), so it never re-ran the DSL lambda and could never reproduce the bug; both human sign-offs were false-positives. Prefer a deterministic automated test driving the real trigger (red→green).

**Lessons / debt:** v2.0.3 was a wrong-cause release caught only by a real consumer. Diagnosis was fixed by instrumentation (counters/logs) rather than guess-and-swap. No outstanding debt; all 8 requirements satisfied in 2.0.4.

---

## v2.0.2 AeroPanelGroup (Shipped: 2026-06-23)

**Phases:** 13 + 13.1 (2 phases) | **Plans:** 8 (Phase 13: 5, Phase 13.1: 3) | **Requirements:** 18/18 v1 (PNL-01..PNL-18) + PNL-HORIZ-01 (delivered via inserted Phase 13.1)

**Timeline:** 2026-06-22 17:15 → 2026-06-23 11:01 +0300 (~1-day push)
**Git range:** `603ae57` (start milestone) → `2d7de8f` (phase 13.1 complete) — 49 commits, 11 `feat` / 7 `fix`
**Diff:** 4 code files, +1,516 (`AeroPanelGroup.kt` 818, `PanelDistribution.kt` 245, `PanelGroupLogicTest.kt` 235, `LayoutSection.kt` +218); 39 files incl. docs, +8,162 / −877
**Sign-off:** ✅ APPROVED — three-theme human visual sign-off (AeroBlue / AeroDark / Classic) on both vertical (Phase 13) and horizontal (Phase 13.1) demos; single additive-component milestone completed without a separate audit (requirements 18/18 + both phase sign-offs cover coverage + integration).

**Delivered:** One additive layout component, `AeroPanelGroup` (+ `AeroPanelSection` via scope-DSL) — N sections that fill the parent, collapse to a ~36dp header strip with neighbors absorbing the freed space, and drag-resize between adjacent expanded sections (VS Code Side Bar model). A follow-on inserted phase added a horizontal orientation variant via a shared internal core, with zero breaking changes and zero regression to the vertical behavior. No new Gradle dependencies.

**Key accomplishments:**
1. **Animation-vs-drag spike (PNL-PITFALL-01, Phase 13-01)** — Mandatory gate before any UI code. Confirmed Pattern 3: `animateFloatAsState` reads `renderHeight` as a target-only value while drag writes `sizePx` directly; `isDragging` flips the spec to `snap()` during a gesture and back to `tween(200ms, FastOutSlowInEasing)` otherwise. Collapse-then-immediate-drag produces no snap-back and no oscillation. Spike also surfaced three layout-math rules (header reservation per-section, drag-delta scaling into sizePx units, `availableForExpanded.coerceAtLeast(0f)`) ported into the real component.
2. **Pure-logic TDD (PNL-16, Phase 13-02)** — `PanelDistribution.kt` (8 zero-Compose functions: `clampPanelDividerPx`, `computeAvailablePx`, `activeDividerCount`, `distributePx`, `shareTransferOnCollapse/Expand`, `lastExpandedFraction`, `restoreFromFraction`) with `PanelGroupLogicTest.kt` — 12 GREEN JVM tests, no Compose runtime, following the `SplitClampTest`/`AccordionToggleTest` precedent. N-section clamp carries the PITFALL-B `coerceAtLeast` guard.
3. **Layout + collapse/expand + drag resize (PNL-01..13, Phase 13-03/04)** — `BoxWithConstraints` + fraction-based `mutableStateListOf` state surviving window resize (PITFALL-A, no `remember(totalPx)` key); `key(section.key)` render loop; 200ms collapse/expand animation; `aeroDragSplitter` + `clampPanelDividerPx` + `rememberUpdatedState(totalPx)` drag between expanded neighbors only; hybrid controlled/uncontrolled expansion (`onExpandedChange == null` ⇒ uncontrolled) per the `AeroAccordion` pattern; `onLayoutChange` fires on drag-end + toggle only.
4. **Win7 Aero visual + per-section flags (PNL-11/12/14, Phase 13-05)** — `glassPanel` header, `AeroIcons.CaretRight` 0°→90° rotation, optional `leadingIcon`, non-bubbling `headerActions` slot, grip-dot dividers; `collapsible = false` hides the chevron, `resizable = false` strips the grip and disables drag. Three-theme sign-off PASS; throwaway spike showcase section deleted.
5. **Horizontal orientation variant (PNL-HORIZ-01, Phase 13.1)** — Refactored to a public wrapper + internal `AeroPanelGroupImpl(orientation, ...)` core (mirrors `AeroSplitPane`), with an additive `orientation: Orientation = Orientation.Vertical` default param — zero breaking change to existing callers, all 12 logic tests unchanged/GREEN. Horizontal branch: `Row` container, `maxWidth` axis, axis-swapped modifiers, rotated bottom-to-top header strip (`requiredWidth(maxHeight)` + `rotate(-90f)`), 0°/180° chevron, E/W drag. Two showcase demos added append-only.
6. **Three-theme sign-off, both orientations (PNL-17)** — Human visual sign-off APPROVED on AeroBlue / AeroDark / Classic for the vertical demo (regression) and both horizontal demos; PNL-17 + PNL-HORIZ-01 fully closed.

**Patterns established:**
- **Public-wrapper + internal-core split for orientation** — `AeroPanelGroupImpl(orientation)` holds all layout/state/drag logic; only three branch points (BoxWithConstraints axis, Row/Column container, section axis modifiers) differ. The reuse template for any future orientation-symmetric layout.
- **Pattern 3 two-writer coexistence** — animation owns a read-only target, drag owns the source-of-truth state, an `isDragging` flag switches the animation spec. The locked answer to "animate vs. drag the same value."
- **Rotated header strip** — `BoxWithConstraints` + `requiredWidth(maxHeight)` + `rotate(-90f)` for bottom-to-top text in a fixed-width strip (supersedes `graphicsLayer`-only and `placeRelativeWithLayer` approaches that were abandoned during sign-off).

**Issues resolved during milestone:**
- **GAP-1 / GAP-2 (Phase 13.1-03 sign-off)** — Horizontal vertical-title rendering and last-column float-rounding distribution were caught at three-theme sign-off and fixed (rotated `requiredWidth` title; explicit `distributePx` width for non-last columns, `weight(1f)` only on the last column's content). Reaffirms the visual gate's value.

**Technical debt incurred:** None functional. Deferred (acknowledged, out of scope): drag-to-reorder sections (PNL-REORDER-01), nested `AeroPanelGroup` first-class API (PNL-NEST-01), keyboard resize (PNL-KBD-01). Carry-over still open: AeroDropdown popup-offset regression (DROP-FIX-01, future milestone).

---

## v2.0.1 Picker & SplitPane Fixes (Shipped: 2026-06-22)

**Phases:** 12 (1 phase) | **Plans:** 4 (Phase 12: 4) | **Tasks:** 10 atomic commits | **Requirements:** 18/18 (FIXDT-01/02, FIXSP-01..04, DTR-01..08, SHW-11..14)

**Timeline:** 2026-06-22 (single-day push, ~2h20m execution: 14:05 → 16:23 +0300)
**Git range:** `b684382` (start milestone) → `2c6202c` (phase complete) — 25 commits, 5 `feat` / 3 `fix`
**Diff:** 9 code files changed, +520 / −14 (30 files incl. docs, +4,440 / −1,188)
**Verification:** ✅ PASSED — 18/18 must-haves verified (`.planning/phases/12-.../12-VERIFICATION.md`); single-phase patch milestone completed without a separate audit (verification covered requirements + integration + showcase sign-off)

**Delivered:** A patch milestone fixing two known bugs and adding one additive component — zero new dependencies, no breaking changes to the v2.0 public API. `AeroDateTimePicker` now shows seconds in its trigger; nested N-pane `AeroSplitPane` layouts drag without snap-back or crash; and the new `AeroDateTimeRangePicker` ships with a dual-calendar + two-time-row Apply-gate, emitting `(LocalDateTime, LocalDateTime)`. All three deliverables verified in the showcase on AeroBlue / AeroDark / Classic.

**Key accomplishments:**
1. **Fix A — seconds in trigger (FIXDT-01/02)** — Introduced `internal fun formatAeroDateTime(ldt, showSeconds)` as the single source of truth for datetime trigger strings, and changed `AeroDateTimePicker`'s `formatter` param to nullable with body-level dispatch (`formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)`). Seconds now render when `showSeconds = true`; an explicit caller formatter is used verbatim (no breaking change). Three new unit tests lock the behaviour.
2. **Fix B — nested SplitPane freeze (FIXSP-01..04, TDD)** — Converted `AeroSplitPane` from `remember(totalPx)` px-state to fraction-based divider state with `val dividerPx` derived each recompose, so an outer-splitter drag no longer re-keys and resets the inner divider (PITFALL-A). Guarded `clampDividerPx` with `val safeMax = maxPx.coerceAtLeast(minFirstPx)` before `coerceIn`, eliminating the `IllegalArgumentException` when an inner pane is squeezed below combined minima (PITFALL-B). Inverted-range test written RED before the fix (`38e0de6`), green after (`f4de00f`).
3. **New `AeroDateTimeRangePicker` (DTR-01..08)** — Apply-gate dual-calendar datetime range picker reusing `AeroDateRangeState` + `nextRangeState` verbatim. `onDayClick` discards the commit pair; the sole `onRangeSelect` emit + close site is the Apply button, gated by `rangeState is Selected`. `orderDateTimeRange` silently swaps same-day reversed times via `LocalDateTime` Comparable (no Instant conversion). Two unconditional `TimeFields` rows (enabled-gated) keep popup height stable for position-flip logic; four `remember(expanded)` blocks prevent cross-open state leaks. Six unit tests.
4. **Showcase + three-theme sign-off (SHW-11..13)** — `PickersSection` gained an `AeroDateTimeRangePicker` row with a live `(LocalDateTime, LocalDateTime)` label and a `showSeconds=true` vs `false` contrast pair; `LayoutSection` gained a nested 3-pane `AeroSplitPane` demo (inner in outer `end` slot) that reproduces FIXSP-01/02. Human sign-off PASSED on all three themes.
5. **Doc hygiene (SHW-14)** — The stale "Revisit on publish — kotlinx-datetime declared implementation" note in `PROJECT.md` Key Decisions was corrected to the factual record that `api(libs.kotlinx.datetime)` was already in place (no transitive leak).

**Patterns established:**
- **Nullable-formatter dispatch** (`formatter: ((T) -> String)? = null`, body-level `?:` fallback) — avoids PITFALL-H where a default-lambda param can't close over a `showSeconds` declared after it; now the convention for both `AeroDateTimePicker` and `AeroDateTimeRangePicker`.
- **Fraction-as-stable-coordinate** for resizable dividers — store the fraction, derive px each recompose; survives `totalPx` changes without reset. The locked fix for any viewport-dependent divider state.
- **Dual Apply-gate picker** — two endpoints sharing the same pending-state discipline as `AeroDateTimePicker`, extended over the `AeroDateRangeState` machine.

**Issues resolved during milestone:**
- **SplitPane drag regression (`7f38c0c`)** — caught during three-theme sign-off: after the fraction-state conversion, the `aeroDragSplitter` loop was still reading a stale captured `dividerFraction` from the `pointerInput` lambda, snapping the inner splitter back (an FIXSP-01 regression). Fixed by reading live state in the drag loop (`rememberUpdatedState` pattern, same root cause as the Phase 11 `AeroRangeSlider` F9 bug). This is exactly the class of silent failure the visual sign-off gate exists to catch.

**Technical debt incurred:** None functional. (Carry-over from prior milestones still open: AeroDropdown popup-offset regression — explicitly OUT of v2.0.1 scope, future milestone.)

---

## v2.0 Stateful + Layout (Shipped: 2026-06-18)

**Phases:** 7–11 (5 phases) | **Plans:** 27 (Phase 7: 3, Phase 8: 6, Phase 9: 3, Phase 10: 4, Phase 11: 11) | **Requirements:** 27/27 (PICK-01..08, DATA-01..06, LAYO-01..09, SHW-07..10)

**Timeline:** 2026-04-30 → 2026-06-18 (~49 days elapsed, concentrated execution)
**Git range:** `2a4eaae` (feat 07-01) → `4d902cb` (HEAD) — 145 commits, 47 `feat`
**Diff:** 152 files changed, +27,406 / −2,285
**Audit:** ✅ PASSED — 27/27 requirements (3-source cross-referenced), 0 integration blockers, E2E showcase compiles + wires end-to-end (`.planning/milestones/v2.0-MILESTONE-AUDIT.md`)

**Delivered:** 12 new stateful + layout components (8 complex stateful, 4 advanced layout) added on top of an internal-primitives foundation, no breaking changes to the v1.x public API. `kotlinx-datetime:0.6.2` is the only new dependency. The showcase gained DataSection, PickersSection, and LayoutSection, and passed the 16-item × 3-theme "looks done but isn't" silent-failure checklist (48 cells) as the formal milestone sign-off gate.

**Key accomplishments:**
1. **Shared internal primitives (Phase 7)** — `AeroCalendarGrid`, `AeroColorMath` (HSV single-source-of-truth, round-trip drift-free), `AeroHsvColorSquare` + `AeroHueSlider`, `Modifier.aeroDragSplitter`, `AeroStepIndicator`, and `AeroCalendarPositionProvider` built and tested (27 JUnit tests) so Phases 8–10 share one drag pattern, one calendar renderer, and one color-math layer with no per-component duplication.
2. **Pickers (Phase 8)** — Six public components: `AeroRangeSlider`, `AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker`, `AeroColorPicker`. PITFALL-03 (touchSlop drag), PITFALL-06 (partial-range leak via sealed state machine), PITFALL-15 (HSV drift), and PITFALL-02/08 (popup clipping/first-frame jump) all defused; `kotlinx-datetime:0.6.2` added.
3. **Data (Phase 9)** — `AeroDataTable` (virtualized LazyColumn, 3-position column sort, `Set<RowKey>` Ctrl/Shift multi-selection surviving sort, drag-resize columns with min-width clamp) and `AeroTreeView` (once-only lazy `onExpand` via `SnapshotStateMap` above LazyColumn). PITFALL-01/04/05 resolved.
4. **Layout (Phase 10)** — `AeroAccordion` (single/multi, lifted state), `AeroSplitPane` (clamped divider, 8dp hit-area), `AeroSidebar` (3-mode `animateDpAsState`, scope DSL, collapsed tooltips), `AeroStepperWizard` (`onValidate` commit-gate on click only, Back-navigation preserves composable state). PITFALL-11/12/13/14 resolved.
5. **Showcase + v2.0 visual sign-off (Phase 11)** — DataSection / PickersSection / LayoutSection wired into ShowcaseApp, RangeSection extended; Phase 7 scratch files deleted cleanly. The 16-item × 3-theme checklist FAILED first pass (16 defects), all closed across gap-plans 11-06…11-11, then re-verified — all 48 cells PASS (SHW-10 gate satisfied).

**Locked v2.0 decisions (carried forward):**
- `detectDragGestures` banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03, touchSlop=18dp); `Modifier.aeroDragSplitter` is the shared utility
- `AeroScrollArea` banned inside DataTable / TreeView — raw `LazyListState + AeroScrollBar` (PITFALL-01)
- DataTable selection API is `Set<RowKey>` + caller `key: (T) -> Any`, never `Set<Int>` indices (PITFALL-04)
- ColorPicker internal state is HSV float tuple only; RGB and HEX are derived views (PITFALL-15)
- `AeroCalendarPositionProvider` (Phase 7) replaces `AeroDropdownPopup` for all date-picker popups (PITFALL-02)
- All v2.0 overlays use `Popup(...)`, never `Dialog(transparent=true)` (W11-01, grep-gated)
- `kotlinx-datetime:0.6.2` is the only new dependency

**Issues deferred to follow-up (tech debt — all documentation hygiene or advisory, no deferred functional work):**
- SUMMARY frontmatter `requirements-completed` gaps: PICK-03, DATA-05/06, LAYO-03/04/08/09 absent from plan SUMMARYs (all verified SATISFIED in phase VERIFICATIONs)
- Phase 8 verification filed as `08-VERIFICATION-REPORT.md`, not the glob-standard `08-VERIFICATION.md`
- `kotlinx-datetime` declared `implementation` not `api` — picker public signatures expose `kotlinx.datetime.*`; transitive type would leak for a PUBLISHED library; address at publish/POM step
- Cosmetic: duplicate import (`AeroAccordion.kt:21,24`), stale KDoc reference to deleted scratch section (`AeroStepIndicator.kt:45`)
- LAYO-06 API deviation: per-item `onClick:()->Unit` in `AeroSidebarScope.item()` instead of sidebar-level `onItemClick:(ItemKey)->Unit` — intentional, strictly more flexible
- Nyquist: Phases 7, 8, 11 not formally `nyquist_compliant` (substantial JUnit coverage exists — 27 + 65 tests; formal validation contract not completed) — optional `/gsd:validate-phase 7|8|11`
- Carry-over: AeroDropdown popup-offset regression (v1.0) — explicitly OUT of v2.0 scope; future gap-closure or v2.x

---

## v1.1 Icon System (Shipped: 2026-04-30)

**Phases:** 4–6 (3 phases) | **Plans:** 11 (Phase 4: 2, Phase 5: 6, Phase 6: 3) | **Requirements:** 17/17 (ICN-01..03, MIG-01..11, CLN-01..03, SHW-04..06)

**Timeline:** 2026-04-29 (single-day push, ~10 h: 08:40 → 18:26 +0300)
**Git range:** `19dd352` → `80ef218` (60 commits, 20 `feat`)
**Diff:** 340 files changed, +20,212 / −477 (most insertions are 138 generated `ImageVector` builders in `library/src/main/kotlin/com/mordred/aero/icons/internal/`)

**Delivered:** A typed `AeroIcons` set of 138 Phosphor Regular `ImageVector` constants replaces every text glyph and every `Icons.Outlined.*` usage in the library; `compose.materialIconsExtended` is gone from the Gradle dependency graph; the showcase has a searchable `IconsSection` grid signed off across all three themes.

**Key accomplishments:**
1. **AeroIcons Foundation (Phase 4)** — 138 Phosphor Regular `ImageVector` constants generated via Valkyrie 1.1.1 (`--output-format BackingProperty`), lazy backing-property pattern, `explicitApi()`-clean. Phosphor naming preserved verbatim (`X` not `Close`, `CaretDown` not `ChevronDown`, `MagnifyingGlass` not `Search`, `Gear` not `Settings`, `House` not `Home`, `Funnel` not `Filter`, `EyeSlash` not `EyeOff`).
2. **Component Migrations (Phase 5, Waves 1–3)** — All 11 components migrated: `AeroCheckbox`, `AeroDropdown`, `AeroNumberSpinner`, `AeroContextMenu`, `AeroToastHost`, `AeroNotificationBanner`, `AeroAlertKind`, `AeroBannerKind` (Wave 1); `AeroSearchField` and `AeroPasswordField` Canvas composables deleted in favor of `Icon(AeroIcons.*)` (Wave 2); `AeroTitleBar` `TitleBarButton` restructured to take `ImageVector` (Wave 3).
3. **Dependency Cleanup (Phase 5, Waves 4–5)** — `AeroAlertKindTest` and `AeroBannerKindTest` rewritten to assert `AeroIcons.*` instances (CLN-01); `compose.materialIconsExtended` line removed from `library/build.gradle.kts` (CLN-02); grep gate clean (CLN-03). `compileClasspath` shed `material-icons-extended-desktop-1.7.3.jar` (~36 MB).
4. **Showcase IconsSection (Phase 6, Plan 01)** — `IconsSection.kt`: 138-entry `LazyVerticalGrid` with live `AeroSearchField` filter (case-insensitive substring), glassSurface cells, click-to-copy + toast feedback, "not found" empty state. Wired into `ShowcaseApp.kt` between `FoundationSection` and `ButtonsSection`.
5. **ButtonsSection migration (Phase 6, Plan 02)** — Showcase `AeroIconButton` row demos converted from `Text("▲"/"▼"/"×")` to real `Icon(AeroIcons.{CaretUp,CaretDown,X})` at 14dp; SHW-06 grep gate closed.
6. **Three-theme visual sign-off (Phase 6, Plan 03)** — All AeroBlue / AeroDark / Classic items PASS in formal visual checkpoint; v1.1 milestone signed off 2026-04-29. AeroNumberSpinner Phase 5 visual approval confirmed (12dp/14dp slot, no regression).

**Locked v1.1 decisions (carried forward):**
- AeroIcons source: Phosphor Regular (not Feather) — softer rounded stroke matches Win7-toolbar-glyph aesthetic
- Lazy backing-property pattern mandatory for all 138 constants — eager `val` at this scale causes measurable startup spike
- Phosphor naming preserved verbatim (no Material/Feather aliases)
- `Icon()` from material3 used directly — no custom `AeroIcon()` wrapper; tint always passed explicitly in library code
- Phase 5 wave ordering is mandatory: Waves 1+2 → Wave 3 → Wave 4 → Wave 5 (dep removal cannot precede test rewrites)
- AeroBreadcrumb `separator: String` intentionally NOT migrated to `ImageVector` in v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time; Phosphor SVG source committed to `tools/phosphor-svgs/regular/` with `.pin` file recording exact upstream SHA

**Issues resolved during milestone:**
- AeroNumberSpinner sub-pixel pitfall: Phosphor stroke at 10dp ≈ 0.63dp (sub-pixel at 96 DPI) → mitigated by raising button slot to 14dp; visual checkpoint confirmed in AeroDark
- Valkyrie 1.1.1 generated Shape A (extension properties) → required `--explicit-mode=true` flag and post-generation fix (`defaultWidth=256`→`24.dp`, AeroIcons import)
- Phosphor mixed-fill icons (DotsThree, Warning dot, Info dot, Bug eyes, etc.) correctly retain `fill=SolidColor(Color.Black)` — `ColorFilter.tint` replaces all colors at render time via `Icon(tint=...)`

**Issues deferred to follow-up:**
- AeroDropdown popup offset regression (carried over from v1.0 03-03 manual checkpoint; root cause `AeroScrollArea` `Column.fillMaxSize()`) — does not block v1.1 ship
- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0, no v1.1 action

**Technical debt incurred:**
- 138 explicit `internal.*` extension property imports per migration file (no wildcard) — consistent with Phase 5 precedent; no resolution planned for v1.x
- v1.0 was never formally archived through `/gsd:complete-milestone`. Phases 1–3 history is captured in `.planning/milestones/v1.1-ROADMAP.md` (full ROADMAP snapshot at v1.1 ship time) but not in a dedicated `v1.0-ROADMAP.md` archive.

---
