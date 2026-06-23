---
phase: 13-aeropanelgroup
plan: 03
subsystem: ui
tags: [compose-desktop, panel-group, boxwithconstraints, animation, fraction-state, scope-dsl]

# Dependency graph
requires:
  - phase: 13-aeropanelgroup
    plan: 02
    provides: PanelDistribution.kt pure functions (computeAvailablePx, distributePx, shareTransferOnCollapse, shareTransferOnExpand, lastExpandedFraction, restoreFromFraction)
  - phase: 13-aeropanelgroup
    plan: 01
    provides: spike findings (Pattern 3 animation coexistence, header reservation, FastOutSlowInEasing 200ms confirmed)
provides:
  - AeroPanelGroup composable: BoxWithConstraints + fraction-based sizePx state + AeroPanelGroupScope DSL + key(section.key) render loop + uncontrolled collapse/expand + 200ms FastOutSlowInEasing animation
  - AeroPanelGroupScope: public class with section() DSL function, stable key identity (PNL-13)
  - Collapse/expand toggle: shareTransferOnCollapse/Expand wired, lastExpandedFraction save/restore
  - Static 1dp dividers between adjacent expanded sections only (PNL-PITFALL-09)
affects: [13-04-plan, 13-05-plan]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AeroPanelGroupScope fresh each recompose (same as AeroSidebarScope) — live Compose state captured correctly in section() lambda"
    - "sizePx as mutableStateListOf<Float> seeded to 1f equal share or defaultSize.toPx(); no totalPx remember-key (PITFALL-A)"
    - "expandedState + lastExpandedFractionState as parallel mutableStateListOf<Boolean/Float>"
    - "Seed guard: if (sizePx.size != sections.size) re-seed all three lists — handles DSL changes after first composition"
    - "animateFloatAsState per section with label='panelHeight_<key>' reads renderHeight target; never writes sizePx (Pattern 3, PNL-PITFALL-01)"
    - "tween(durationMillis=200, easing=FastOutSlowInEasing) for collapse/expand; isDragging+snap() deferred to 13-04"
    - "weight(1f) on last expanded section only; all others use explicit .height(dp) — absorbs float rounding (PNL-PITFALL-11)"
    - "Divider rendered only where expandedState[i] && i < lastIndex && expandedState[i+1] (PNL-PITFALL-09)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt
  modified: []

key-decisions:
  - "sizePx seeds: defaultSize present → with(density){defaultSize.toPx()} as seed; absent → 1f equal share. Normalization is not needed at seed time because distributePx ratios are proportional regardless of absolute seed magnitudes."
  - "Seed guard on sizePx.size != sections.size re-initializes all three state lists. This is the minimal re-seed trigger; DSL reorder / key rename resets state, which is acceptable for uncontrolled path."
  - "animateFloatAsState target: if (expandedState[i]) renderHeights[i] else headerPx — collapsed sections animate to headerPx so they visually shrink to just their header strip."
  - "Content Box gating: render content box when isExpanded || animatedHeightPx > headerPx to keep content visible during collapse animation until it fully shrinks."
  - "isDragging flag + snap() animationSpec during drag deferred to plan 13-04 per spike findings; tween(200ms) is the only animationSpec in this plan."

patterns-established:
  - "AeroPanelGroup public signature (for 13-04/13-05 to extend): AeroPanelGroup(modifier, initiallyExpanded: Set<String>?, onLayoutChange: ((List<Float>)->Unit)?, content: @Composable AeroPanelGroupScope.()->Unit)"
  - "AeroPanelGroupScope.section signature (stable): section(key, title, minSize=60.dp, collapsible=true, resizable=true, defaultExpanded=true, defaultSize=null, leadingIcon=null, headerActions=null, content)"
  - "sizePx normalization: seeds are raw weights (1f or px from defaultSize); distributePx handles proportional math so explicit normalization is unnecessary"

requirements-completed: [PNL-01, PNL-02, PNL-03, PNL-04, PNL-13, PNL-15]

# Metrics
duration: ~2min
completed: 2026-06-23
---

# Phase 13 Plan 03: Layout Skeleton + Collapse/Expand Animation Summary

**`AeroPanelGroup.kt` (319 lines): BoxWithConstraints + fraction-state + AeroPanelGroupScope DSL + key(section.key) render loop + uncontrolled toggle + 200ms FastOutSlowInEasing collapse/expand animation via animateFloatAsState — no drag, no controlled path, no visual polish**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-06-23T05:36:08Z
- **Completed:** 2026-06-23T05:38:42Z
- **Tasks:** 2 (skeleton + animation, written as one atomic file pass)
- **Files created:** 1

## Accomplishments

- `AeroPanelGroup.kt` created at 319 lines — exceeds 120-line minimum.
- All six required tokens present: `AeroPanelGroupScope`, `section(`, `AeroPanelGroup(`, `BoxWithConstraints`, `key(section.key)`, `computeAvailablePx`, `distributePx`, `shareTransferOnCollapse`, `shareTransferOnExpand`, `clipToBounds`, `weight(1f)`.
- All three banned tokens absent: `remember(totalPx)=0`, `animateContentSize=0`, `detectDragGestures=0`.
- `animateFloatAsState` + `tween(durationMillis = 200` + `FastOutSlowInEasing` all present (Task 2).
- `PanelGroupLogicTest` suite GREEN (12 tests, BUILD SUCCESSFUL).
- `:library:compileKotlin` GREEN.

## Task Commits

1. **Task 1 + Task 2: AeroPanelGroup skeleton + 200ms animation** - `b1a9cd8` (feat)

**Plan metadata:** (this commit — docs)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — 319 lines. Public `AeroPanelGroup` composable + `AeroPanelGroupScope` DSL + `AeroPanelSectionConfig` internal data class. Full uncontrolled collapse/expand with fraction-save/restore. Per-section `animateFloatAsState` with 200ms `FastOutSlowInEasing`. All pure-logic calls delegated to `PanelDistribution.kt`.

## Decisions Made

**sizePx seed approach:** `defaultSize != null` → seed with `defaultSize.toPx()`; else seed with `1f`. No explicit normalization required — `distributePx` works proportionally regardless of absolute magnitudes, so seeding all sections with `1f` distributes equal shares naturally.

**Seed guard:** `if (sizePx.size != sections.size)` re-seeds all three parallel state lists. This handles the first composition (empty lists) and DSL structural changes without a secondary mechanism.

**animateFloatAsState target when collapsed:** `headerPx` (not 0f). Collapsed sections still render a 36dp header, so animating toward `headerPx` produces the correct visual — the content area shrinks to zero, not the whole row.

**Content Box gating:** `if (isExpanded || animatedHeightPx > headerPx)` — keeps the content Box mounted during the outgoing collapse animation until the animated height fully reaches `headerPx`. This prevents content from disappearing on the first frame of collapse.

**Tasks 1 and 2 written together:** Both build the same file; writing animation (Task 2) alongside the skeleton (Task 1) in one pass avoids a rewrite of the content Box height wiring. Both acceptance criteria verified separately before committing.

## Deviations from Plan

None — plan executed exactly as written. Tasks 1 and 2 are committed in a single atomic commit (same file, additive); this matches the spirit of per-task commits since there is no intermediate compilable state between the skeleton-without-animation and the animation addition.

## Issues Encountered

Two occurrences of `remember(totalPx)` in comments triggered the grep gate. Reworded to `totalPx remember-key` in comments so the gate correctly returns 0 while preserving the informational text.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

Plan 13-04 (drag resize) may begin. Key contracts for wiring:

**`AeroPanelGroup` public signature:**
```kotlin
@Composable
public fun AeroPanelGroup(
    modifier: Modifier = Modifier,
    initiallyExpanded: Set<String>? = null,
    onLayoutChange: ((List<Float>) -> Unit)? = null,
    content: @Composable AeroPanelGroupScope.() -> Unit,
)
```

**`AeroPanelGroupScope.section` signature:**
```kotlin
public fun section(
    key: String,
    title: String,
    minSize: Dp = 60.dp,
    collapsible: Boolean = true,
    resizable: Boolean = true,
    defaultExpanded: Boolean = true,
    defaultSize: Dp? = null,
    leadingIcon: ImageVector? = null,
    headerActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
)
```

**Internal state lists (plan 13-04 drag path must access via hoisted state or new remember blocks):**
- `sizePx: SnapshotStateList<Float>` — proportion weights; drag writes here directly (Pattern 3)
- `expandedState: SnapshotStateList<Boolean>`
- `lastExpandedFractionState: SnapshotStateList<Float>`

**Plan 13-04 must add:**
- `isDragging by remember { mutableStateOf(false) }` flag
- `snap()` animationSpec branch when `isDragging == true` (spike finding 4)
- `aeroDragSplitter` gesture on divider `Box` with delta scaling (`scale = expandedSizeSum / availableForExpanded`)
- `clampPanelDividerPx` from `PanelDistribution.kt`
- `rememberUpdatedState(totalPx)` in drag lambda (FIXSP-01 pattern)
- Controlled expansion path: `onExpandedChange` + `expandedKeys` params

**Plan 13-05 must add:**
- `glassPanel` header styling, CaretRight 0→90° rotation, grip dots, `headerActions` rendering
- Delete `PanelGroupSpikeSection.kt` and its `ShowcaseApp.kt` wiring
- Three-theme showcase demo

## Self-Check

### File existence

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — FOUND (319 lines)
- `.planning/phases/13-aeropanelgroup/13-03-SUMMARY.md` — this file

### Commit existence

- `b1a9cd8` (feat — AeroPanelGroup skeleton + animation) — FOUND

## Self-Check: PASSED

---
*Phase: 13-aeropanelgroup*
*Completed: 2026-06-23*
