---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: "Roadmap ready — awaiting `/gsd:plan-phase 12`"
stopped_at: Completed 12-03-PLAN.md (AeroDateTimeRangePicker composable with Apply gate, DTR-01..08)
last_updated: "2026-06-22T12:57:10.111Z"
last_activity: 2026-06-22 — Roadmap revised, 18/18 requirements consolidated to Phase 12
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 4
  completed_plans: 3
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-06-22 — v2.0.1 milestone started)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, typed `AeroIcons`, and a showcase — no manual style work or icon-pack hunting required.
**Current focus:** v2.0.1 Picker & SplitPane Fixes — single Phase 12, roadmap defined, ready to execute.

## Current Position

Phase: 12 (not started)
Plan: —
Status: Roadmap ready — awaiting `/gsd:plan-phase 12`
Last activity: 2026-06-22 — Roadmap revised, 18/18 requirements consolidated to Phase 12

```
v2.0.1 Progress: [Phase 12]
                 [ not started ]
```

## Shipped Milestones

| Version | Name | Phases | Plans | Requirements | Shipped |
|---------|------|--------|-------|--------------|---------|
| v1.0 | MVP (Foundation + Atomic + Composite/Navigation) | 1–3 | 18 | 53 | 2026-04-28 |
| v1.1 | Icon System | 4–6 | 11 | 17 | 2026-04-30 |
| v2.0 | Stateful + Layout | 7–11 | 55 | 27 | 2026-06-18 |

See `.planning/MILESTONES.md` for full accomplishments.

## v2.0.1 Roadmap Summary (2026-06-22, revised)

| Phase | Name | Goal | Requirements | Build Order (plan-level) |
|-------|------|------|--------------|--------------------------|
| 12 | v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker | Fix seconds trigger, fix nested SplitPane freeze, add `AeroDateTimeRangePicker`, all showcase demos, doc hygiene | FIXDT-01..02, FIXSP-01..04, DTR-01..08, SHW-11..14 | Fix A → Fix B → New Component |

**Coverage:** 18/18 v2.0.1 requirements mapped. One phase.

**Plan-level build order inside Phase 12:**
1. Fix A (FIXDT-01, FIXDT-02, SHW-12, SHW-14) — introduces `formatAeroDateTime` helper; must precede new component
2. Fix B (FIXSP-01..04, SHW-13) — write unit test for inverted-range `clampDividerPx` BEFORE applying the two-file fix
3. New Component (DTR-01..08, SHW-11) — lock Apply-gate architecture BEFORE writing composable body (PITFALL-E)

## v2.0 Locked Decisions (carried forward)

- `undecorated=true` BEZ `transparent=true` — Win11 EXCEPTION_ACCESS_VIOLATION rule; extends to ALL Popup/Dialog (W11-01)
- Glass effect in single `drawBehind` block — performance baseline
- `:library` uses `compose.desktop.common`, `:showcase` uses `currentOs`
- AeroIcons name verbatim from Phosphor — locked v1.1
- `Icon()` from material3 used directly; tint always explicit — locked v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time
- `detectDragGestures` banned for Canvas-based drag — use `awaitPointerEventScope` + manual loop (PITFALL-03)
- `AeroScrollArea` banned inside DataTable / TreeView — use raw `LazyListState + AeroScrollBar` (PITFALL-01)
- DataTable selection API is `Set<RowKey>` + `key: (T) -> Any`, NOT `Set<Int>` indices (PITFALL-04)
- ColorPicker internal state is HSV float tuple only; RGB and HEX are derived views (PITFALL-15)
- `AeroCalendarPositionProvider` replaces `AeroDropdownPopup` for all date picker popups (PITFALL-02)
- `kotlinx-datetime:0.6.2` is declared `api(...)` at `library/build.gradle.kts:27` — confirmed; stale `implementation` note in PROJECT.md will be cleared in Phase 12 (SHW-14)

## v2.0.1 Key Decisions (pre-roadmap, from research)

- **formatAeroDateTime internal helper** — introduced in Phase 12 Fix A; shared by `AeroDateTimeRangePicker`; prevents re-introducing PITFALL-H in the new component
- **Apply gate architecture for AeroDateTimeRangePicker** — `nextRangeState` commit pair discarded in day-click handler; only Apply button triggers `onRangeSelect` + `expanded = false`; must be locked before writing composable code (PITFALL-E)
- **Fraction-based SplitPane divider state** — `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }` with `val dividerPx` derived each recompose; no `remember(totalPx)` key (PITFALL-A fix)
- **clampDividerPx guard** — `val safeMax = maxPx.coerceAtLeast(minFirstPx)` before `coerceIn`; unit test for inverted range written BEFORE fix is applied (PITFALL-B fix)
- **Fix A before new component** — `formatAeroDateTime` must exist before writing `AeroDateTimeRangePicker` trigger format to avoid duplicating the bug pattern

## Performance Metrics

**v1.0:** 26 plans, ~3 days, average ~7–25 min per plan.
**v1.1:** 11 plans, single-day push (2026-04-29, ~10 h), 60 commits, 340 files changed, +20,212 / −477 lines.
**v2.0:** 55 plans, ~49 days, 145 commits, 152 files changed, +27,406 / −2,285 lines.
**v2.0.1:** 1 plan completed — Fix A (12-01); 4 min, 3 tasks, 3 files.

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
- [Phase 07]: Phase7ScratchSection demo body lives in :library (AeroPhase7Scratch.kt, public, deleted Phase 11) NOT :showcase — Kotlin internal is module-scoped, so the showcase imports a thin public wrapper to keep all 6 Phase 7 primitives internal
- [Phase 07]: Modifier.aeroDragSplitter is the locked v2.0 in-content drag pattern: awaitPointerEventScope + manual loop, cursor change keyed to Orientation, change.consume() only on actual delta (release unconsumed)
- [Phase 07]: AeroCalendarGrid header Row pinned to Modifier.width(252.dp); outer Column wrapContentWidth — header now matches 7x36dp day-grid width exactly
- [Phase 07]: Architecture B reconfirmed: AeroCard glass wrappers in scratch demo only — AeroCalendarGrid + AeroStepIndicator remain surface-less; glass is a consumer responsibility
- [Phase 08-pickers]: AeroRangeSlider tooltip is an overlaid glassEffect Box positioned by thumb x (AeroSlider pill approach), not in-Canvas text; behind showTooltip, only for the active thumb
- [Phase 08-pickers]: lastMovedThumb inits to RangeThumb.End so the Start thumb draws on top when both thumbs compress (PITFALL-07); thumbToDrawFirst helper makes z-order unit-testable
- [Phase 08-pickers]: PickerPopupContainer is the single shared W11-02 popup surface (two-layer background + glassPanel, no elevation) for all 4 date/time pickers; cornerRadius locked 8.dp
- [Phase 08-pickers]: AeroDatePicker min/max bounds are inclusive; dateIsDisabled extracted as pure internal predicate for unit testability
- [Phase 08]: AeroColorPicker HSV(A) floats are the single source of truth; RGB/HEX derived per emit, never stored (PITFALL-15) — drift gate test locks it
- [Phase 08]: safeHsvColor is the only Color.hsv entry point in the picker; coerces hue to [0,360] to avoid the requirePrecondition throw (NEW-PICK-01)
- [Phase 08]: AeroTimePicker is 24-hour only — no use12Hour/AM-PM parameter (descoped per user decision), enforced by a zero-match grep gate
- [Phase 08]: TimeFields (internal spinner row) + assembleTime (pure clamp/assemble helper) are the reuse seam consumed by AeroDateTimePicker (plan 04); TimePicker emits LocalTime on every spinner change with no Apply gate
- [Phase 08]: AeroDateTimePicker (PICK-04) composes AeroCalendarGrid + TimeFields directly with a pending-state Apply/Cancel commit gate; the only onValueChange() call site is the Apply onClick (combineDateTime) — day clicks and time edits never close or emit (NEW-PICK-02)
- [Phase 08-pickers]: AeroCalendarGrid range params are additive (rangeStart/rangeEnd: LocalDate? = null after selected); intermediate cells render primary@0.15f, endpoints primary (PITFALL-09 extension, AeroDark-readable)
- [Phase 08-pickers]: AeroDateRangePicker (PICK-02) uses sealed AeroDateRangeState + pure nextRangeState((state,clicked) -> (next, commit?)); the non-null commit is the SOLE guard around the single onRangeSelect() call, so it fires exactly once per completed range and never on a start-only click (PITFALL-06, unit-tested without Compose); stacking threshold locked at maxWidth < 560.dp (NEW-PICK-03); leftMonth drives both months, rightMonth = leftMonth + 1
- [Phase 09-01]: AeroScrollBar(LazyListState) additive overload (Option A) — keeps existing callers intact, DataTable/TreeView bypass AeroScrollArea PITFALL-01
- [Phase 09-01]: Selection locked as Set<Any> + caller key fn at type level (PITFALL-04) — zero Set<Int> in datatable package
- [Phase 09-01]: resolveColumnWidths uses pxPerDp: Float not Compose Density for pure JVM testability
- [Phase 09-03]: AeroTreeView uses flatten-and-replace pattern: fixed rowHeight items, no AnimatedVisibility, children are separate LazyColumn items
- [Phase 09-03]: SnapshotStateMap<Any, NodeState> above LazyColumn + toggleNode pure guard locks DATA-06/PITFALL-05: onExpand fires exactly once per node on first expand
- [Phase 09-data]: KDoc strings must not mention forbidden grep-gate tokens (AeroScrollArea, stickyHeader, detectDragGestures) — use paraphrases to avoid false positives in package-level verification checks
- [Phase 10-02]: AeroAccordion hybrid ownership: onExpandedChange null = uncontrolled (mutableStateOf Set<Int>); non-null = controlled pure renderer — matches AeroDataTable hybrid-sort pattern
- [Phase 10-layout]: AeroSidebarMode enum + targetWidthForMode consolidated in AeroSidebarState.kt; SideEffect (not LaunchedEffect) syncs animateDpAsState to state.widthState each frame; AeroSidebarScope constructed fresh each recompose with current mode — PITFALL-11 contract locked
- [Phase 10-layout]: BoxWithConstraints used once for measurement; dividerPx state updated only on drag — no SubcomposeLayout per frame
- [Phase 10-layout]: AeroSplitPane keyboard nudge deferred to v2.x per CONTEXT.md Claude's Discretion
- [Phase 11]: kotlinx-datetime added as direct :showcase dependency (Rule 3 auto-fix — DataSection.kt pre-existing blocker; transitive from :library was insufficient for direct source use)
- [Phase 11]: Phase7ScratchSection call removed and three new section calls added in-place; scratch wrapper and aggregator deleted without breaking any other module dependency
- [Phase 11]: AeroHsvColorSquare sized to 220dp so 24dp hue slider fits in 280dp bounded panel (F12 fix)
- [Phase 11]: Popup AeroColorPicker wrapped in Box to prevent full-window stretch; 280.dp panel width is the W11-02 glass surface (F10 fix)
- [Phase 11]: Accordion divider inset 8dp horizontal matching glassPanel cornerRadius to avoid overflowing rounded bg (F11 fix)
- [Phase 11]: F4: clickable moved to outer cell Box in AeroTableHeader; resize splitter at CenterEnd on its own inner Box — sort and resize targets do not interfere
- [Phase 11]: F5: AeroTreeNode row-level clickable calls onExpandClick() when isExpandable — safe because it routes through toggleNode() PITFALL-05 once-only guard in AeroTreeView
- [Phase 11-showcase-v2-0-visual-sign-off]: formatAeroDate in AeroDatePicker.kt as package-internal helper used by all three date pickers for DD.MM.YYYY default (F6)
- [Phase 11-showcase-v2-0-visual-sign-off]: F14 required no state-machine changes: nextRangeState already committed same-month ranges; both calendar grids route onDateSelected=onDayClick; fix was verification + tests only
- [Phase 11]: positionChange() chosen for aeroDragSplitter delta — single-frame intra-event API, immune to hit-area Box relocation between frames (F3/F15 root cause)
- [Phase 11]: rememberUpdatedState(value) in AeroRangeSlider drag loop so applyThumbMove always reads the latest committed range (F9 root cause — stale captured value in pointerInput lambda)
- [Phase 11-showcase-v2-0-visual-sign-off]: F-WIZARD root cause: AeroStepperWizard is intentionally surface-less; active step uses weight(1f,fill=false) inside a plain Column — needs caller-provided bounded parent height; showcase fix is Box(height(200.dp)); no library edit needed
- [Phase 11-showcase-v2-0-visual-sign-off]: F-ACCORDION-HOVER: .clip(RoundedCornerShape(8.dp)) added before .clickable in AeroAccordion.kt so hover/press highlight clips to rounded glass surface
- [Phase 12-01]: formatAeroDateTime internal helper in AeroDateTimePicker.kt; formatter param changed to ((LocalDateTime)->String)?=null; displayText dispatches via formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds) — resolves FIXDT-01/02, provides shared helper for AeroDateTimeRangePicker (PITFALL-H prevention)
- [Phase 12-01]: kotlinx-datetime declared api(libs.kotlinx.datetime) confirmed; PROJECT.md stale implementation/Revisit note corrected to factual api record (SHW-14)
- [Phase 12]: Fraction-based SplitPane divider state: var dividerFraction by remember with val dividerPx derived each recompose; no remember(totalPx) key eliminates nested-drag snap-back (PITFALL-A fix, FIXSP-01)
- [Phase 12]: clampDividerPx guard: val safeMax = maxPx.coerceAtLeast(minFirstPx) before coerceIn prevents IllegalArgumentException when inner pane squeezed below combined minima (PITFALL-B fix, FIXSP-02)
- [Phase 12]: Apply gate (PITFALL-E prevention): nextRangeState commit pair discarded in onDayClick; onRangeSelect fires ONLY at Apply onClick guarded by rangeState is Selected (DTR-02/03)
- [Phase 12]: orderDateTimeRange applied at sole emit site: LocalDateTime Comparable a<=b swap for same-day reversed times (DTR-04); no Instant conversion needed in kotlinx-datetime 0.6.2
- [Phase 12]: Both TimeFields rows rendered unconditionally; enabled=false until rangeState is Selected — stable popup height for AeroCalendarPositionProvider flip logic (DTR-08, PITFALL-I)

### Pending Todos

- Gap-close: AeroDropdown popup offset regression — explicitly OUT of v2.0.1 scope; future milestone
- v2.0.1 Phase 12 (Fix B): write unit test for `clampDividerPx` with inverted range BEFORE applying the two-file fix (mandatory per research SUMMARY.md)
- v2.0.1 Phase 12 (New Component): lock Apply-gate architecture decision (no auto-close on second date click) BEFORE writing the composable body (PITFALL-E)
- v2.0.1 Phase 12 (New Component): confirm `nextRangeState` and `combineDateTime` are accessible from `AeroDateTimeRangePicker.kt` package (same package = no extra action; different package = make internal accessible)

### Blockers/Concerns

- None blocking Phase 12. All root causes confirmed from source with exact file/line numbers (research confidence: HIGH).
- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0; revisit only if CMP version bump is on the table.

## Session Continuity

Last session: 2026-06-22T12:57:01.084Z
Stopped at: Completed 12-03-PLAN.md (AeroDateTimeRangePicker composable with Apply gate, DTR-01..08)
Resume file: None
Next action: `/gsd:plan-phase 12`
