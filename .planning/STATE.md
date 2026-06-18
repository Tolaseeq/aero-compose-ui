---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Stateful + Layout
status: completed
stopped_at: Completed 11-08-PLAN.md (F6 DD.MM.YYYY default + F14 same-month range tests)
last_updated: "2026-06-18T17:37:00.584Z"
last_activity: "2026-06-18 — Phase 8 plan-05 completed: public AeroDateRangePicker (PICK-02) — dual-month popup that stacks vertically below 560dp (NEW-PICK-03), range highlight via additive AeroCalendarGrid rangeStart/rangeEnd params (primary@0.15f intermediate, primary endpoints — PITFALL-09 extension), and a sealed AeroDateRangeState machine whose pure nextRangeState transition makes onRangeSelect fire exactly once per completed range and never on a partial start click (PITFALL-06). 5 unit tests + compileKotlin green; existing DatePicker/DateTimePicker/CalendarGrid tests unaffected (additive change)."
progress:
  total_phases: 11
  completed_phases: 10
  total_plans: 55
  completed_plans: 52
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-30 — v2.0 Current Milestone section added)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** v2.0 Stateful + Layout — roadmap created, ready to plan Phase 7 (Shared Internal Primitives).

## Current Position

Phase: 8 — Pickers
Plan: 08-05 (AeroDateRangePicker) complete — see Performance Metrics for per-plan progress
Status: 08-05 (AeroDateRangePicker / PICK-02) complete — dual-month range popup with responsive stacking + sealed-state single-callback machine landed (5/6 plans of phase 8 done)
Last activity: 2026-06-18 — Phase 8 plan-05 completed: public AeroDateRangePicker (PICK-02) — dual-month popup that stacks vertically below 560dp (NEW-PICK-03), range highlight via additive AeroCalendarGrid rangeStart/rangeEnd params (primary@0.15f intermediate, primary endpoints — PITFALL-09 extension), and a sealed AeroDateRangeState machine whose pure nextRangeState transition makes onRangeSelect fire exactly once per completed range and never on a partial start click (PITFALL-06). 5 unit tests + compileKotlin green; existing DatePicker/DateTimePicker/CalendarGrid tests unaffected (additive change).

```
v2.0 Progress: [███       ] 29% (1/5 phases, 5/6 plans of phase 8)
```

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

## v2.0 Roadmap Summary (2026-04-30)

| Phase | Name | Goal | Requirements | Complexity |
|-------|------|------|--------------|------------|
| 7 | Shared Internal Primitives | Internal foundation for Phases 8–10 (CalendarGrid, ColorMath, HsvSquare+HueSlider, AeroDragSplitter, StepIndicator, AeroCalendarPositionProvider) | Enabling (no owned REQs) | — |
| 8 | Pickers | 6 picker/slider components (RangeSlider + 4 date/time pickers + ColorPicker) | PICK-01..08 (8) | LARGE×2 + MEDIUM×2 + SMALL×2 |
| 9 | Data | AeroDataTable + AeroTreeView | DATA-01..06 (6) | LARGE×1 + MEDIUM×1 |
| 10 | Layout | AeroAccordion + AeroSplitPane + AeroSidebar + AeroStepperWizard | LAYO-01..09 (9) | MEDIUM×3 + SMALL×1 |
| 11 | Showcase + Sign-off | DataSection + PickersSection + LayoutSection + 16-item checklist gate | SHW-07..10 (4) | — |

**Coverage:** 27/27 v2.0 requirements mapped (PICK 8, DATA 6, LAYO 9, SHW 4)

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.

**v2.0 metrics:**

| Plan | Duration | Tasks | Files | Notes |
|------|----------|-------|-------|-------|
| 07-01 logic-and-tests | ~10 min | 4 | 9 | TDD red-green per task; 27 JUnit tests across 3 new test classes; PITFALL-02 / PITFALL-08 / PITFALL-15 defused at utility level |
| 07-02 visuals-and-scratch | ~10m 33s | 5 | 8 | aeroDragSplitter + HsvColorSquare + HueSlider + StepIndicator landed; AeroPhase7Scratch aggregator in :library + thin Phase7ScratchSection wrapper in :showcase preserves locked-internal API for all 6 primitives; PITFALL-03 mitigation locked at the shared-utility level |
| Phase 07-shared-internal-primitives P03 | 3min | 2 tasks | 2 files |
| Phase 08-pickers P01 | 3min | 2 tasks | 2 files |
| Phase 08-pickers P02 | 4min | 3 tasks | 3 files |
| Phase 08 P06 | 5min | 3 tasks | 4 files |
| Phase 08 P03 | 6min | 3 tasks | 3 files |
| Phase 08 P04 | 2min | 2 tasks | 2 files |
| Phase 08-pickers P05 | 3min | 3 tasks | 3 files |
| Phase 09-data P01 | 4min | 5 tasks | 9 files |
| Phase 09-data P03 | 5min | 4 tasks | 7 files |
| Phase 09-data P02 | ~10min | 3 tasks | 4 files |
| Phase 10-layout P02 | 11 | 2 tasks | 5 files |
| Phase 10-layout P03 | 11 | 2 tasks | 4 files |
| Phase 10-layout P01 | 13min | 2 tasks | 3 files |
| Phase 10-layout P04 | 13 | 2 tasks | 3 files |
| Phase 11 P01 | 3min | 2 tasks | 3 files |
| Phase 11 P02 | 4min | 2 tasks | 2 files |
| Phase 11-showcase-v2-0-visual-sign-off P03 | 2min | 2 tasks | 1 files |
| Phase 11-showcase-v2-0-visual-sign-off P04 | 1min | 2 tasks | 3 files |
| Phase 11 P09 | 3min | 3 tasks | 3 files |
| Phase 11-showcase-v2-0-visual-sign-off P07 | 3min | 3 tasks | 4 files |
| Phase 11-showcase-v2-0-visual-sign-off P08 | 3min | 2 tasks | 5 files |

## Accumulated Context

### Decisions

Full decision log in PROJECT.md "Key Decisions" table. Active decisions affecting all future milestones:

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule (locked since Phase 1); extends to ALL Popup/Dialog in v2.0 (W11-01)
- Glass effect in single `drawBehind` block — performance baseline
- `:library` uses `compose.desktop.common`, `:showcase` uses `currentOs`
- AeroIcons name verbatim from Phosphor — locked v1.1
- `Icon()` from material3 used directly; tint always explicit — locked v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time
- AeroBreadcrumb `separator: String` intentionally NOT migrated to `ImageVector`
- **v2.0 new:** `detectDragGestures` is banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03, touchSlop=18dp)
- **v2.0 new:** `AeroScrollArea` is banned inside DataTable / TreeView — use raw `LazyListState + AeroScrollBar` (PITFALL-01)
- **v2.0 new:** DataTable selection API is `Set<RowKey>` + `key: (T) -> Any`, NOT `Set<Int>` indices (PITFALL-04)
- **v2.0 new:** ColorPicker internal state is HSV float tuple only; RGB and HEX are derived views (PITFALL-15)
- **v2.0 new:** `AeroCalendarPositionProvider` (new, Phase 7) replaces `AeroDropdownPopup` for all date picker popups (PITFALL-02)
- **v2.0 new:** `kotlinx-datetime:0.6.2` added in Phase 8 as the only new dependency
- [Phase 07]: Hue convention LOCKED to degrees [0f, 360f] (matches Color.hsv requirePrecondition)
- [Phase 07]: AeroCalendarPositionProvider first-frame guard uses popupContentSize == IntSize.Zero, NOT >= windowSize
- [Phase 07]: Width overflow on wide popup right-aligns and never flips Top/Bottom
- [Phase 07]: Touchslop spike skipped — awaitPointerEventScope manual loop is the locked v2.0 pattern regardless
- [Phase 07]: [Phase 07] Phase7ScratchSection demo body lives in :library (AeroPhase7Scratch.kt, public, deleted Phase 11) NOT :showcase — Kotlin internal is module-scoped, so the showcase imports a thin public wrapper to keep all 6 Phase 7 primitives internal
- [Phase 07]: [Phase 07] Modifier.aeroDragSplitter is the locked v2.0 in-content drag pattern: awaitPointerEventScope + manual loop, cursor change keyed to Orientation, change.consume() only on actual delta (release uncconsumed)
- [Phase 07]: AeroCalendarGrid header Row pinned to Modifier.width(252.dp); outer Column wrapContentWidth — header now matches 7x36dp day-grid width exactly
- [Phase 07]: Architecture B reconfirmed: AeroCard glass wrappers in scratch demo only — AeroCalendarGrid + AeroStepIndicator remain surface-less; glass is a consumer responsibility
- [Phase 08-pickers]: AeroRangeSlider tooltip is an overlaid glassEffect Box positioned by thumb x (AeroSlider pill approach), not in-Canvas text; behind showTooltip, only for the active thumb
- [Phase 08-pickers]: lastMovedThumb inits to RangeThumb.End so the Start thumb draws on top when both thumbs compress (PITFALL-07); thumbToDrawFirst helper makes z-order unit-testable
- [Phase 08-pickers]: [Phase 08] PickerPopupContainer is the single shared W11-02 popup surface (two-layer background + glassPanel, no elevation) for all 4 date/time pickers; cornerRadius locked 8.dp
- [Phase 08-pickers]: [Phase 08] AeroDatePicker min/max bounds are inclusive; dateIsDisabled extracted as pure internal predicate for unit testability
- [Phase 08]: [Phase 08] AeroColorPicker HSV(A) floats are the single source of truth; RGB/HEX derived per emit, never stored (PITFALL-15) — drift gate test (sat 1.0->0.5->1.0 restores FF0000) locks it
- [Phase 08]: [Phase 08] safeHsvColor is the only Color.hsv entry point in the picker; coerces hue to [0,360] to avoid the requirePrecondition throw (NEW-PICK-01)
- [Phase 08]: [Phase 08] AeroTimePicker is 24-hour only — no use12Hour/AM-PM parameter (descoped per user decision), enforced by a zero-match grep gate
- [Phase 08]: [Phase 08] TimeFields (internal spinner row) + assembleTime (pure clamp/assemble helper) are the reuse seam consumed by AeroDateTimePicker (plan 04); TimePicker emits LocalTime on every spinner change with no Apply gate
- [Phase 08]: [Phase 08] AeroDateTimePicker (PICK-04) composes AeroCalendarGrid + TimeFields directly with a pending-state Apply/Cancel commit gate; the only onValueChange( call site is the Apply onClick (combineDateTime) — day clicks and time edits never close or emit (NEW-PICK-02)
- [Phase 08-pickers]: [Phase 08] AeroCalendarGrid range params are additive (rangeStart/rangeEnd: LocalDate? = null after selected); intermediate cells render primary@0.15f, endpoints primary (PITFALL-09 extension, AeroDark-readable); DatePicker/DateTimePicker callers and Phase 7 grid tests unaffected
- [Phase 08-pickers]: [Phase 08] AeroDateRangePicker (PICK-02) uses sealed AeroDateRangeState + pure nextRangeState((state,clicked) -> (next, commit?)); the non-null commit is the SOLE guard around the single onRangeSelect( call, so it fires exactly once per completed range and never on a start-only click (PITFALL-06, unit-tested without Compose); stacking threshold locked at maxWidth < 560.dp (NEW-PICK-03); leftMonth drives both months, rightMonth = leftMonth + 1
- [Phase 09-01]: AeroScrollBar(LazyListState) additive overload (Option A) — keeps existing callers intact, DataTable/TreeView bypass AeroScrollArea PITFALL-01
- [Phase 09-01]: Selection locked as Set<Any> + caller key fn at type level (PITFALL-04) — zero Set<Int> in datatable package
- [Phase 09-01]: resolveColumnWidths uses pxPerDp: Float not Compose Density for pure JVM testability
- [Phase 09-03]: AeroTreeView uses flatten-and-replace pattern: fixed rowHeight items, no AnimatedVisibility, children are separate LazyColumn items
- [Phase 09-03]: SnapshotStateMap<Any, NodeState> above LazyColumn + toggleNode pure guard locks DATA-06/PITFALL-05: onExpand fires exactly once per node on first expand
- [Phase 09-data]: KDoc strings must not mention forbidden grep-gate tokens (AeroScrollArea, stickyHeader, detectDragGestures) — use paraphrases to avoid false positives in package-level verification checks
- [Phase 09-data]: [Phase 09-02] AeroIcons extension props in com.mordred.aero.icons.`internal` require explicit named imports; AeroIcons object alone does not expose them
- [Phase 10-02]: AeroAccordion hybrid ownership: onExpandedChange null = uncontrolled (mutableStateOf Set<Int>); non-null = controlled pure renderer — matches AeroDataTable hybrid-sort pattern
- [Phase 10-layout]: AeroSidebarMode enum + targetWidthForMode consolidated in AeroSidebarState.kt; SideEffect (not LaunchedEffect) syncs animateDpAsState to state.widthState each frame; AeroSidebarScope constructed fresh each recompose with current mode — PITFALL-11 contract locked
- [Phase 10-layout]: BoxWithConstraints used once for measurement; dividerPx state updated only on drag — no SubcomposeLayout per frame (PITFALL research §perf)
- [Phase 10-layout]: AeroSplitPane keyboard nudge deferred to v2.x per CONTEXT.md Claude's Discretion (focusable+onPreviewKeyEvent integration out of scope)
- [Phase 11]: kotlinx-datetime added as direct :showcase dependency (Rule 3 auto-fix — DataSection.kt pre-existing blocker; transitive from :library was insufficient for direct source use)
- [Phase 11]: DataSection.kt written as complete file combining Task 1+2 in single Write; LayoutSection.kt pre-existing compile errors deferred to plan 11-03 scope
- [Phase 11]: AeroButton param is text: String (not a content lambda), package is buttons plural — confirmed from source during compile fix
- [Phase 11]: Phase7ScratchSection call removed and three new section calls added in-place (after NavigationSection, before Spacer); scratch wrapper and aggregator deleted without breaking any other module dependency
- [Phase 11]: AeroHsvColorSquare sized to 220dp so 24dp hue slider fits in 280dp bounded panel (F12 fix)
- [Phase 11]: Popup AeroColorPicker wrapped in Box to prevent full-window stretch; 280.dp panel width is the W11-02 glass surface (F10 fix)
- [Phase 11]: Accordion divider inset 8dp horizontal matching glassPanel cornerRadius to avoid overflowing rounded bg (F11 fix)
- [Phase 11]: F4: clickable moved to outer cell Box in AeroTableHeader; resize splitter at CenterEnd on its own inner Box — sort and resize targets do not interfere
- [Phase 11]: F5: AeroTreeNode row-level clickable calls onExpandClick() when isExpandable — safe because it routes through toggleNode() PITFALL-05 once-only guard in AeroTreeView
- [Phase 11]: F-RESIZE: coerceIn(minDp, maxDp) where maxDp = availableDp - othersMinDp; left over-shrink for Name column deferred to 11-10 showcase config (larger minWidth)
- [Phase 11-showcase-v2-0-visual-sign-off]: formatAeroDate in AeroDatePicker.kt as package-internal helper used by all three date pickers for DD.MM.YYYY default (F6)
- [Phase 11-showcase-v2-0-visual-sign-off]: F14 required no state-machine changes: nextRangeState already committed same-month ranges; both calendar grids route onDateSelected=onDayClick; fix was verification + tests only

### Pending Todos

- Gap-close: AeroDropdown popup offset regression — explicitly OUT of v2.0 scope; can be picked up as a one-off after v2.0 ships, or scheduled into a v2.x milestone
- Phase 7 research: upstream `touchSlop` issue #343 may have been silently fixed in CMP 1.7.x — 1-minute drag test at Phase 7 plan-01 confirms which path to take
- Phase 8 research: `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 first-compile validation — Phase 8 plan-01 acceptance criterion; fallback is `0.7.1-0.6.x-compat`
- Phase 9 research: `rememberScrollbarAdapter(LazyListState)` API surface verification in CMP 1.7.3

### Blockers/Concerns

- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0; revisit if/when CMP version bump is on the table
- v2.0 risks (see ROADMAP.md Phase notes and PITFALLS.md for full detail):
  - PITFALL-01 (silent showstopper): LazyColumn inside AeroScrollArea destroys DataTable virtualization
  - PITFALL-03 (silent showstopper): `detectDragGestures` touchSlop=18dp silently breaks ColorPicker HSV drag, RangeSlider thumbs, DataTable column resize — resolved by Phase 7 shared `awaitPointerEventScope` utility
  - PITFALL-04: DataTable selection-by-index becomes stale after sort — API design must lock `Set<RowKey>` in Phase 9 plan-01

## Session Continuity

Last session: 2026-06-18T17:37:00.577Z
Stopped at: Completed 11-08-PLAN.md (F6 DD.MM.YYYY default + F14 same-month range tests)
Resume file: None
Next action: `/gsd:plan-phase 11 --gaps` — create gap-closure plans for the 16 defects (gap plans may edit component source)
