---
phase: 13-aeropanelgroup
plan: 01
subsystem: ui
tags: [compose-desktop, animation, drag, spike, panel-group]

# Dependency graph
requires:
  - phase: 12-splitpane-fixes
    provides: aeroDragSplitter, clampDividerPx, animateFloatAsState patterns verified in repo
provides:
  - PNL-PITFALL-01 resolved: animateFloatAsState + direct drag writes on shared sizePx state coexist with no snap-back and no oscillation
  - Four spike findings (header reservation, drag delta scaling, availableForExpanded guard, drag animation disable) for plans 13-02 and 13-04
affects: [13-02-plan, 13-03-plan, 13-04-plan, 13-05-plan]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "animateFloatAsState reads renderHeight (target-only); drag writes sizePx directly — Pattern 3 confirmed"
    - "snap() animationSpec while isDragging=true; tween(200ms, FastOutSlowInEasing) otherwise"
    - "rememberUpdatedState(totalPx) in drag lambda — carries FIXSP-01 pattern forward"
    - "BoxWithConstraints bounded height in scrolling showcase (fillMaxWidth().height()), not fillMaxSize — F-WIZARD precedent"

key-files:
  created:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PanelGroupSpikeSection.kt
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

key-decisions:
  - "PNL-PITFALL-01 RESOLVED via Pattern 3: animateFloatAsState reads renderHeight as target; drag writes sizePx directly. No snap-back, no oscillation."
  - "HEADER RESERVATION (for 13-02 PanelDistribution.kt): availableForExpanded = totalPx - (sectionCount * headerHeightPx) - (activeDividerCount * dividerThicknessPx). Reserve one header per section, not one per collapsed section."
  - "DRAG DELTA SCALING (for 13-04 drag path): raw pointer delta is in rendered pixels; sizePx values are abstract proportion units. Scale: val scale = expandedSizeSum / availableForExpanded; apply scaledDelta = delta * scale. Min-clamp must also be in sizePx units."
  - "AVAILABLE GUARD (for 13-02): availableForExpanded.coerceAtLeast(0f) prevents negative heights when all sections collapsed."
  - "DRAG ANIMATION DISABLE (required in 13-04): animateFloatAsState uses snap() while isDragging=true, tween(200ms) otherwise. Flag set true on awaitFirstDown, cleared in try/finally so all exit paths reset it."
  - "PanelGroupSpikeSection.kt and its ShowcaseApp.kt wiring are THROWAWAY — must be removed in plan 13-05."

patterns-established:
  - "Pattern 3 (Animation-vs-Drag Coexistence): single sizePx SnapshotStateList; animation reads derived renderHeight as animateFloatAsState target; drag writes sizePx directly in onDrag lambda; snap() during drag, tween(200ms) for collapse/expand"
  - "Bounded spike height in scrolling parent: Modifier.fillMaxWidth().height(Xdp), not fillMaxSize"

requirements-completed: [PNL-07]

# Metrics
duration: multi-session (spike + iterative gate fixes + human verify)
completed: 2026-06-23
---

# Phase 13 Plan 01: Animation-vs-Drag Spike Summary

**Throwaway spike proving animateFloatAsState and direct aeroDragSplitter writes coexist on shared sizePx state with no snap-back and no oscillation — PNL-PITFALL-01 RESOLVED, Pattern 3 confirmed, four layout-math rules established for plans 13-02 and 13-04**

## Performance

- **Duration:** Multi-session (spike build + iterative gate fixes across 7 commits)
- **Completed:** 2026-06-23
- **Tasks:** 3 (tasks 1 and 2 auto; task 3 human-verify gate — APPROVED)
- **Files modified:** 2

## Accomplishments

- Throwaway `PanelGroupSpikeSection.kt` built: 3 sections, `animateFloatAsState` reading `renderHeight` as animation target, `aeroDragSplitter` writing `sizePx` directly, collapse toggle for gate testing.
- Spike wired into showcase (`ShowcaseApp.kt`) and human gate passed: divider tracks cursor 1:1, collapse/expand animates over 200ms, collapse-then-immediate-drag produces no snap-back and no oscillation.
- Four spike findings recorded (below) for safe porting into `PanelDistribution.kt` (plan 13-02) and the real `AeroPanelGroup` drag path (plan 13-04).

## Task Commits

1. **Task 1: Write throwaway spike composable** - `7d5df7f` (feat)
2. **Task 2: Wire spike into showcase** - `a3cf5e6` (feat)
3. **Auto-fix: bound spike height in scrolling showcase** - `94b59af` (fix)
4. **Auto-fix: correct header reservation and drag delta scaling** - `0167e7c` (fix)
5. **Docs: append spike findings to STATE.md for 13-02** - `1b0b0ff` (docs)
6. **Auto-fix: snap size animation during drag for 1:1 cursor tracking** - `09d6952` (fix)
7. **Docs: add drag-animation-disable finding to STATE.md** - `8c09435` (docs)
8. **Task 3: Human gate — APPROVED** - (no separate commit; gate outcome recorded here)

**Plan metadata:** (this commit)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/PanelGroupSpikeSection.kt` — THROWAWAY spike composable (3 sections, animateFloatAsState + aeroDragSplitter on shared sizePx). Must be deleted in plan 13-05.
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — Temporary wiring for spike gate. Must be removed in plan 13-05 when real AeroPanelGroup demo replaces it.

## Decisions Made

**Gate outcome: PNL-PITFALL-01 RESOLVED — PASS.** Pattern 3 as designed: `animateFloatAsState` reads `renderHeight` as its target (never writes `sizePx`); drag writes `sizePx` directly in the `onDrag` lambda. Confirmed by human gate: divider tracks cursor 1:1, collapse/expand animates 200ms, collapse-then-drag produces no snap-back and no oscillation.

**Four spike findings to carry into plans 13-02 and 13-04:**

1. **HEADER RESERVATION** — `availableForExpanded` must reserve one header per section (all sections, not just collapsed): `availableForExpanded = totalPx - (sectionCount * headerHeightPx) - (activeDividerCount * dividerThicknessPx)`. Every section always renders its header regardless of expanded state; reserving only collapsed-section headers caused bottom sections to clip inside a bounded parent.

2. **DRAG DELTA SCALING** — Raw pointer delta from `aeroDragSplitter` is in rendered pixels; `sizePx` values are abstract proportion units. Before applying: `val scale = expandedSizeSum / availableForExpanded` (constant for a single drag gesture since `combined = above + below` is invariant). Apply `scaledDelta = delta * scale`. The min-clamp must also be converted to sizePx units: `minSizeUnits = minRenderedPx * scale`. The PITFALL-B `safeMax` guard (coerceAtLeast) is preserved, now in sizePx unit space.

3. **AVAILABLE GUARD** — `availableForExpanded` must be guarded with `.coerceAtLeast(0f)` to prevent negative heights when all sections are collapsed.

4. **DRAG ANIMATION DISABLE (required in plan 13-04)** — `animateFloatAsState` must switch to `snap()` while a drag is active, and revert to `tween(200ms, FastOutSlowInEasing)` otherwise. Pattern: `var isDragging by remember { mutableStateOf(false) }` flag; set true on `awaitFirstDown`, reset false in `try/finally` around the inner drag loop so all exit paths (release or `change == null`) clear it. With `snap()`, `animatedHeight == renderHeight` instantly during drag — divider tracks cursor 1:1. On release `renderHeight` already equals `animatedHeight` so there is no catch-up tween or positional jump. Collapse/expand toggles (no drag) still animate over 200ms via the `else` branch.

**Bounded height in scrolling parent** — A spike placed inside a scrolling showcase needs `Modifier.fillMaxWidth().height(Xdp)`, not `fillMaxSize`. Same precedent as Phase 11 F-WIZARD.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Spike height crashed with infinite constraints in scrolling showcase**
- **Found during:** Task 2 (wiring into showcase)
- **Issue:** `BoxWithConstraints` in a vertically scrolling Column received unbounded height (`maxHeight = Constraints.Infinity`), causing `constraints.maxHeight.toFloat()` to be unusable.
- **Fix:** Wrapped spike in `Box(Modifier.fillMaxWidth().height(500.dp))` per F-WIZARD precedent.
- **Files modified:** `PanelGroupSpikeSection.kt`
- **Committed in:** `94b59af`

**2. [Rule 1 - Bug] Header reservation formula was wrong — bottom section clipped**
- **Found during:** Task 3 (human gate observation)
- **Issue:** `availableForExpanded` subtracted only collapsed-section headers; all sections always render a header strip, so expanded sections' headers also consume height.
- **Fix:** Changed to `totalPx - (sectionCount * headerHeightPx) - (activeDividerCount * dividerThicknessPx)`.
- **Files modified:** `PanelGroupSpikeSection.kt`
- **Committed in:** `0167e7c`

**3. [Rule 1 - Bug] Drag delta was in rendered pixels but sizePx is in proportion units — divider drifted**
- **Found during:** Task 3 (human gate observation)
- **Issue:** Applying raw pointer delta directly to `sizePx` caused the divider to drift (not track 1:1) because rendered height and sizePx units differ by a scale factor.
- **Fix:** Applied `scale = expandedSizeSum / availableForExpanded`; used `scaledDelta` and `minSizeUnits` in sizePx unit space.
- **Files modified:** `PanelGroupSpikeSection.kt`
- **Committed in:** `0167e7c`

**4. [Rule 1 - Bug] animateFloatAsState tween during drag caused snap-back**
- **Found during:** Task 3 (human gate observation)
- **Issue:** With `tween(200ms)` always active, the animation target chased the drag value on every frame, creating lag and apparent snap-back on release.
- **Fix:** Introduced `isDragging` flag; switched to `snap()` while dragging, `tween(200ms)` otherwise.
- **Files modified:** `PanelGroupSpikeSection.kt`
- **Committed in:** `09d6952`

---

**Total deviations:** 4 auto-fixed (all Rule 1 — bugs found during gate execution)
**Impact on plan:** All fixes were necessary for the gate to pass. The drag-animation-disable finding (fix 4) is a required port into plan 13-04 and is documented in STATE.md.

## Issues Encountered

Spike needed three rounds of layout-math corrections before the human gate passed. All corrections are now codified as explicit rules in the key decisions above and in STATE.md, so plans 13-02 and 13-04 can implement them directly without rediscovery.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- PNL-PITFALL-01 gate passed: all Compose layout/drag/animation work in later plans is unblocked.
- Plan 13-02 (pure-logic TDD, `PanelDistribution.kt`) may begin immediately. Must apply finding 1 (header reservation) and finding 2 (drag delta scaling) to the pure logic.
- Plan 13-04 (drag resize) must apply finding 4 (drag animation disable with `isDragging` + `snap()`).
- `PanelGroupSpikeSection.kt` and its `ShowcaseApp.kt` wiring are THROWAWAY and must be removed in plan 13-05.

---
*Phase: 13-aeropanelgroup*
*Completed: 2026-06-23*
