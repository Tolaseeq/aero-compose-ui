---
phase: 13-aeropanelgroup
plan: 05
subsystem: ui
tags: [compose-desktop, panel-group, aero-visual, glassPanel, carousel-rotation, headerActions, showcase, three-theme-sign-off]

# Dependency graph
requires:
  - phase: 13-aeropanelgroup
    plan: 04
    provides: AeroPanelGroup with drag resize, hybrid controlled/uncontrolled expansion, PanelGroupDivider, KDoc
  - phase: 13-aeropanelgroup
    plan: 02
    provides: PanelDistribution pure logic — distributePx, clampPanelDividerPx, computeAvailablePx
provides:
  - AeroPanelGroup Win7 Aero visual layer: glassPanel(8.dp) header, CaretRight 0->90 rotation, leadingIcon, non-bubbling headerActions, grip-dot divider
  - collapsible=false (hides chevron, disables collapse) and resizable=false (no grip, no drag) per-section flags
  - AeroPanelGroup demo block appended to showcase LayoutSection.kt
  - Throwaway PanelGroupSpikeSection.kt deleted; ShowcaseApp.kt wiring removed
  - Three-theme visual sign-off PASS: AeroBlue, AeroDark, Classic
affects: [phase-13-closure, v2.0.2-milestone]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "glassPanel(cornerRadius = 8.dp) applied to header Row via Modifier chain before clip(RoundedCornerShape(8.dp)).clickable — F-ACCORDION-HOVER clip-before-clickable precedent"
    - "CaretRight rotation: animateFloatAsState(if expanded 90f else 0f, tween(160ms, FastOutSlowInEasing)) + graphicsLayer { rotationZ = caretRotation }; Icon tint explicit (v1.1 rule)"
    - "headerActions non-bubbling: header Row carries the toggle clickable; actions Row rendered separately inside header, actions have their own onClicks that do not reach the outer clickable"
    - "Grip dots: Row of 3 x Box(3.dp) with Box(4.dp) spacers, color AeroTheme.colors.labelText, centered on divider — mirrors AeroSplitPane vertical-orientation 3-dot grip"
    - "collapsible=false guard: chevron Icon rendered only when section.collapsible is true"
    - "resizable=false guard: PanelGroupDivider enabled param false skips grip Row and aeroDragSplitter"
    - "Section target height: headerPx + renderHeights[i] (distributePx returns content-only; header must be re-added; content box subtracts HEADER_HEIGHT internally)"
    - "Drag clamp is pairwise: minBelowPx uses only the directly-adjacent below section's own minSize, not the sum of all expanded sections below the divider"

key-files:
  created:
    - .planning/phases/13-aeropanelgroup/13-05-SUMMARY.md
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
  deleted:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PanelGroupSpikeSection.kt

key-decisions:
  - "Section animatedHeights target is headerPx + renderHeights[i]: distributePx returns content-only heights; the header must be added back so the total Box fills correctly. The content sub-box subtracts HEADER_HEIGHT, keeping the content area at renderHeights[i]. Without this, the last (weight(1f)) section's animated target was content-only, making it balloon to fill remaining parent space."
  - "Drag clamp is pairwise: minBelowPx reserves only the directly-adjacent below section's own minSize. The earlier sigma-sum (all expanded sections below) over-reserved the clamp budget and pinned the divider, preventing drag past a small threshold. Pairwise model matches VS Code resize behavior — neighbor sections can cascade into their own clamps when reached."
  - "Three-theme visual sign-off approved (PNL-14, PNL-17): AeroBlue / AeroDark / Classic all pass the seven-item checklist — Aero gloss header, CaretRight 0->90 animation, leadingIcon + headerActions tint + non-bubbling + collapsed visibility, grip dots + drag, collapsible=false no-chevron, resizable=false no-grip, re-expand restores size."

patterns-established:
  - "AeroPanelGroup header: glassPanel + clip(RoundedCornerShape(8.dp)) + clickable on the outer Row; headerActions in a trailing inner Row with independent onClicks"
  - "PanelGroupDivider: enabled=false path renders 1dp Divider only (no grip Row, no aeroDragSplitter)"
  - "Section height formula: animatedTarget = headerPx + distributePx_result[i]; content box height = animatedTarget - HEADER_HEIGHT"
  - "Pairwise drag clamp: minBelowPx = below section's own minSize * scale; sigma-sum reserved only when the cascade actually occurs (each divider is clamped independently)"

requirements-completed: [PNL-05, PNL-11, PNL-12, PNL-14, PNL-17]

# Metrics
duration: ~multi-session (visual polish + showcase + sign-off + two post-sign-off bug fixes)
completed: 2026-06-23
---

# Phase 13 Plan 05: Aero Visual Polish + Showcase Demo + Three-Theme Sign-off Summary

**Win7 Aero visual layer on AeroPanelGroup (glassPanel header, CaretRight 0->90, leadingIcon, non-bubbling headerActions, grip-dot divider, collapsible/resizable flags), AeroPanelGroup demo in showcase LayoutSection, spike removed, and three-theme sign-off PASS on AeroBlue / AeroDark / Classic**

## Performance

- **Duration:** Multi-session (visual polish + showcase + sign-off + two post-sign-off bug fixes)
- **Completed:** 2026-06-23
- **Tasks:** 3 (tasks 1-2 auto; task 3 human-verify — APPROVED)
- **Files modified:** 3 modified, 1 deleted

## Accomplishments

- `AeroPanelGroup.kt` fully Aero-styled: `glassPanel(cornerRadius = 8.dp)` header surface, `AeroIcons.CaretRight` rotating 0->90 degrees via `animateFloatAsState` + `graphicsLayer { rotationZ }`, `leadingIcon` slot with explicit tint, `headerActions` non-bubbling trailing slot visible in both collapsed and expanded states.
- `PanelGroupDivider` extended with 3-dot grip Row (mirroring AeroSplitPane precedent); `enabled=false` path renders a static 1dp line with no grip and no `aeroDragSplitter`.
- `collapsible=false` sections hide the chevron and do not toggle on header click; `resizable=false` boundaries have no grip and are non-draggable.
- AeroPanelGroup demo block appended to `showcase/LayoutSection.kt` (append-only): 3+ sections, `leadingIcon`, `headerActions` with non-bubbling click, `collapsible=false` section, `resizable=false` boundary, overflow-clipping content, bounded-height parent `Box(Modifier.height(360.dp))`.
- Throwaway `PanelGroupSpikeSection.kt` deleted; `ShowcaseApp.kt` wiring removed.
- Two post-sign-off bugs found and fixed before phase closure (commit `c367322`): section target height formula corrected; drag clamp changed from sigma-sum to pairwise.
- Three-theme visual sign-off PASS: AeroBlue, AeroDark, Classic — all seven checklist items confirmed per PNL-14, PNL-17.

## Task Commits

1. **Task 1: Aero visual polish** — `94c84ba` (feat)
2. **Task 2: Showcase demo + spike removal** — `14b22cd` (feat)
3. **Task 3: Three-theme human sign-off — APPROVED** — (no commit; outcome recorded here)
4. **Post sign-off bug fix: section height + pairwise drag clamp** — `c367322` (fix)

**Plan metadata:** (this commit — docs)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — Added Aero visual layer: glassPanel header, CaretRight rotation, leadingIcon, headerActions non-bubbling slot, grip-dot divider, collapsible/resizable guards. Corrected section height formula (headerPx + renderHeights[i]) and drag clamp (pairwise).
- `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — AeroPanelGroup demo block appended (existing demos unchanged).
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — PanelGroupSpikeSection wiring removed.
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PanelGroupSpikeSection.kt` — DELETED (throwaway spike, per 13-01-SUMMARY.md intent).

## Decisions Made

**Section target height is `headerPx + renderHeights[i]`:** `distributePx` returns content-only heights. The animated target for a section's total Box must add the header height back. The content sub-box then subtracts `HEADER_HEIGHT`, so the content area is exactly `renderHeights[i]`. The earlier formula used `renderHeights[i]` as the total target, which left the last `weight(1f)` section's animated target too small — it then filled remaining parent space, making it balloon relative to its siblings.

**Drag clamp is pairwise (directly-adjacent below section only):** `minBelowPx` reserves the directly-adjacent below section's own `minSize` converted to sizePx units, not the sum of all expanded sections below the divider. The sigma-sum model over-reserved the clamp budget (every below-section's minimum counted against every divider), pinning dividers after minimal drag movement. Each divider is clamped independently; cascading into a neighbor's minimum only occurs if that neighbor is actually reached.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Section total height formula was content-only — last section ballooned**
- **Found during:** Task 3 (three-theme sign-off visual inspection)
- **Issue:** `animatedHeights` target was set to `renderHeights[i]` (content-only, as returned by `distributePx`). The content sub-box subtracted `HEADER_HEIGHT` from this value, giving it a negative or near-zero height. The last `weight(1f)` section filled the remaining parent, making it visibly larger than intended.
- **Fix:** Changed target to `headerPx + renderHeights[i]`. Content sub-box `animatedHeightDp - HEADER_HEIGHT` now correctly equals `renderHeights[i]`.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt`
- **Committed in:** `c367322`

**2. [Rule 1 - Bug] Sigma-sum drag clamp over-reserved the clamp budget — divider was pinned**
- **Found during:** Task 3 (three-theme sign-off drag interaction)
- **Issue:** `minBelowPx` summed `minSize` for all expanded sections from `below..lastIndex`. With multiple sections below the divider, this sum far exceeded any single section's minimum, causing `clampPanelDividerPx` to pin the divider and refuse drag movement past a small delta.
- **Fix:** Changed to pairwise model — `minBelowPx` uses only the directly-adjacent below section's own `minSize * scale`. Neighbors are clamped when their own divider is reached, not pre-emptively from above.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt`
- **Committed in:** `c367322`

---

**Total deviations:** 2 auto-fixed (both Rule 1 — bugs found during sign-off and corrected before sign-off approval)
**Impact on plan:** Both fixes required for correct resize behavior. No scope creep. `clampPanelDividerPx` pure function and its unit tests are unchanged; the change is at the call site (how `minBelowPx` is computed).

## Three-Theme Sign-off

| Theme | Aero gloss header | CaretRight 0->90 | leadingIcon + headerActions | Grip dots + drag | collapsible=false | resizable=false | Re-expand restores size |
|-------|-------------------|------------------|-----------------------------|-----------------|-------------------|-----------------|------------------------|
| AeroBlue | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| AeroDark | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Classic | PASS | PASS | PASS | PASS | PASS | PASS | PASS |

**Sign-off result: PASS — PNL-14 (headerActions) and PNL-17 (three-theme visual sign-off) approved.**

## Issues Encountered

Two bugs found during sign-off (section height double-subtraction; sigma-sum drag clamp) were fixed in commit `c367322` before the PASS was recorded. No issues remain.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Phase 13 (AeroPanelGroup) is complete. All 18 requirements (PNL-01..PNL-18) delivered.
- v2.0.2 milestone is ready for tagging.
- All phase 13 success criteria observable: collapse/expand redistributes height; drag resize works 1:1; window resize preserves proportions; three-theme Aero visual pass; all pure-logic tests GREEN.

---
*Phase: 13-aeropanelgroup*
*Completed: 2026-06-23*
