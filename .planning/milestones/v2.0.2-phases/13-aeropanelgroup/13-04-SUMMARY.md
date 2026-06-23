---
phase: 13-aeropanelgroup
plan: 04
subsystem: ui
tags: [compose-desktop, panel-group, drag-resize, aeroDragSplitter, hybrid-expansion, kdoc]

# Dependency graph
requires:
  - phase: 13-aeropanelgroup
    plan: 03
    provides: AeroPanelGroup.kt skeleton (fraction-state, DSL, collapse/expand animation, static divider)
  - phase: 13-aeropanelgroup
    plan: 02
    provides: clampPanelDividerPx, computeAvailablePx, distributePx, shareTransferOnCollapse/Expand
  - phase: 13-aeropanelgroup
    plan: 01
    provides: spike findings — isDragging+snap(), delta scaling, rememberUpdatedState
provides:
  - AeroPanelGroup with drag resize: aeroDragSplitter + clampPanelDividerPx + rememberUpdatedState(totalPx) + isDragging snap() branch
  - AeroPanelGroup hybrid controlled/uncontrolled expansion: expandedKeys + onExpandedChange params, val controlled = onExpandedChange != null
  - PanelGroupDivider private composable: 8dp hit-area, Orientation.Vertical, hover tint
  - KDoc on public AeroPanelGroup with REQ-ID + PITFALL references
affects: [13-05-plan]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "isDragging flag: true on first onDrag call, false in onDragEnd — switches animationSpec to snap() during drag, tween(200ms) otherwise (spike finding 4)"
    - "rememberUpdatedState(totalPx) in BoxWithConstraints scope; liveTotalPx read inside drag lambda — prevents snap-back on window resize mid-drag (FIXSP-01)"
    - "onDragBetween reads sizePx[above]/sizePx[below] via SnapshotStateList subscript — always live, no captured plain-val copy"
    - "N-section minBelowPx: sum minSizes of all expanded sections from below..lastIndex, scaled to sizePx units (PNL-10, PNL-PITFALL-04)"
    - "Delta scaling: scaledDelta = delta * (expandedSizeSum / availableForExpanded); minSizes also scaled (spike finding 2)"
    - "clampPanelDividerPx carries PITFALL-B coerceAtLeast guard from PanelDistribution.kt"
    - "onLayoutChange fires at drag-end (in onDragEnd lambda) and at toggle (end of onToggle) — NOT per drag frame (PNL-09)"
    - "isDragging guard in onToggle prevents mid-drag collapse race (PNL-PITFALL-03)"
    - "Divider enabled = sections[i].resizable && sections[i+1].resizable (PNL-12)"
    - "Hybrid ownership: val controlled = onExpandedChange != null; isExpanded() and onToggle() both have intentional controlled/uncontrolled branches (PNL-08)"
    - "expandedState list synced from isExpanded() each recompose so size math (distributePx) stays consistent in controlled mode"
    - "KDoc comment 'Do not collapse to one branch' present 4 times (once in KDoc, once in isExpanded, once in onToggle section, once near syncing expandedState)"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt

key-decisions:
  - "Tasks 1 and 2 written together in one atomic file pass: no compilable intermediate state exists between drag-only and drag+controlled, so a single commit is correct"
  - "isDragging set true on first onDrag call (inside the lambda passed to PanelGroupDivider), not inside aeroDragSplitter awaitFirstDown: this is sufficient because aeroDragSplitter fires onDrag on the first positionChange after pointer-down"
  - "minBelowPx summed over all expanded sections from below..lastIndex (not just the direct below neighbor): this is the N-section Sigma-minima formula from PNL-10 and covers all collapse scenarios without a separate function"
  - "liveTotalPx declared via rememberUpdatedState(totalPx) at BoxWithConstraints scope level; read inside onDragBetween and onToggle via computeAvailablePx(liveTotalPx, ...) — single declaration covers both sites"
  - "expandedState list sync loop runs each recompose (before render loop) so controlled mode keeps the size-math lists consistent without duplication"
  - "internalExpanded also updated in uncontrolled onToggle alongside expandedState[i] so both sources of truth stay in sync in uncontrolled mode"

patterns-established:
  - "AeroPanelGroup public signature (for 13-05 to extend): AeroPanelGroup(modifier, initiallyExpanded, expandedKeys, onExpandedChange, onLayoutChange, content)"
  - "PanelGroupDivider(onDrag, onDragEnd, enabled): private composable, Orientation.Vertical, 8dp hit-area, aeroDragSplitter"
  - "onLayoutChange firing sites: drag-end (onDragEnd closure in render loop) and toggle (end of onToggle fun)"

requirements-completed: [PNL-05, PNL-06, PNL-08, PNL-09, PNL-10, PNL-18]

# Metrics
duration: ~5min
completed: 2026-06-23
---

# Phase 13 Plan 04: Drag Resize + Controlled Expansion + KDoc Summary

**`AeroPanelGroup.kt` extended with drag resize via `aeroDragSplitter` + `clampPanelDividerPx` N-section clamp + `rememberUpdatedState(totalPx)` + `isDragging` snap() branch; hybrid controlled/uncontrolled expansion matching AeroAccordion pattern; full KDoc with REQ-ID + PITFALL references**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-06-23T05:42:15Z
- **Completed:** 2026-06-23T05:47:xx Z
- **Tasks:** 2 (drag resize + controlled expansion + KDoc, written as one atomic file pass)
- **Files modified:** 1

## Accomplishments

- `AeroPanelGroup.kt` extended from 319 to ~450 lines.
- All required drag tokens present: `aeroDragSplitter`=3, `rememberUpdatedState`=3, `clampPanelDividerPx`=4, `Orientation.Vertical`=1, `snap()`=4, `isDragging`=8.
- All banned tokens absent: `remember(totalPx)`=0, `detectDragGestures`=0, `animateContentSize`=0.
- Hybrid expansion: `onExpandedChange != null`=1 (controlled derivation); `Do not collapse to one branch`=4 (intentional both branches).
- KDoc REQ-ID refs present: PNL-08=9, PNL-04=1, PNL-06=2, PNL-10=4, PNL-09=4.
- `PanelGroupLogicTest` suite GREEN (BUILD SUCCESSFUL).
- `:library:compileKotlin` GREEN.

## Task Commits

1. **Task 1 + Task 2: drag resize + controlled expansion + KDoc** — `5cba628` (feat)

**Plan metadata:** (this commit — docs)

## Files Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — extended from 319 to ~450 lines. Added: `PanelGroupDivider` private composable, `onDragBetween` lambda, `isDragging` flag + snap() branch, `rememberUpdatedState(totalPx)`, `expandedKeys`/`onExpandedChange` params, `controlled` derivation, hybrid `isExpanded()`/`onToggle()`, full KDoc.

## Final Public AeroPanelGroup Signature

```kotlin
@Composable
public fun AeroPanelGroup(
    modifier: Modifier = Modifier,
    initiallyExpanded: Set<String>? = null,
    expandedKeys: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    onLayoutChange: ((List<Float>) -> Unit)? = null,
    content: @Composable AeroPanelGroupScope.() -> Unit,
)
```

## PanelGroupDivider Signature

```kotlin
@Composable
private fun PanelGroupDivider(
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    enabled: Boolean,
)
```

## minBelowPx Sigma-minima Computation

```kotlin
// Sum minSizes of all expanded sections at or below the divider, converted to sizePx units.
val minBelowPx = (below..sizePx.lastIndex)
    .filter { currentExpandedArr.getOrElse(it) { false } }
    .sumOf { with(density) { sections[it].minSize.toPx() }.toDouble() }
    .toFloat() * scale
```

This covers all N-section cascading-clamp scenarios (PNL-10, PNL-PITFALL-04) without a separate helper.

## onLayoutChange Firing Sites

1. **Drag-end:** `onDragEnd = { isDragging = false; onLayoutChange?.invoke(sizePx.toList()) }` — passed to `PanelGroupDivider` in the render loop.
2. **Toggle:** `onLayoutChange?.invoke(sizePx.toList())` — last statement in `onToggle(i)`, after all state updates.
3. **NOT** inside `onDragBetween` (per-frame) — the plan contract is satisfied.

## Decisions Made

**Tasks 1 and 2 written together:** No compilable intermediate state exists between drag-only and drag+controlled (both modify the same composable body and parameter list). Single commit is the correct atomic unit.

**isDragging set in onDrag lambda:** `aeroDragSplitter` fires `onDrag` on the first `positionChange` after pointer-down, so setting `isDragging = true` on first `onDrag` call is equivalent to setting it on `awaitFirstDown` for the animation-disable purpose.

**minBelowPx inline:** Computed inline in `onDragBetween` as sum over `below..lastIndex` expanded sections. More readable than a separate helper and sufficient for the N-section case.

**expandedState sync loop:** Placed before the render loop each recompose to keep `distributePx` math consistent in controlled mode, where external `expandedKeys` drives truth.

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check

### File existence

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — FOUND (modified)
- `.planning/phases/13-aeropanelgroup/13-04-SUMMARY.md` — this file

### Commit existence

- `5cba628` (feat — drag resize + controlled expansion + KDoc) — FOUND

## Self-Check: PASSED

---
*Phase: 13-aeropanelgroup*
*Completed: 2026-06-23*
