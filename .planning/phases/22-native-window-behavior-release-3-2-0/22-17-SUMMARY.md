---
phase: 22-native-window-behavior-release-3-2-0
plan: 17
subsystem: docs
tags: [rel-06, rel-07, readme, kdoc, native-window, jna, v3.2.0]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0 (Plan 16)
    provides: 22-HANDOFF.md (what is proven PASS vs FAIL) and 22-UNCONFIRMED.md — the documentation source of truth
  - phase: 22-native-window-behavior-release-3-2-0 (Plan 19)
    provides: the public rememberAeroWindowChrome / AeroWindowChromeState API that the README custom-title-bar section documents
provides:
  - README `## Windows window behavior` section + `v3.2.0` dependency snippet and toolchain paragraph
  - KDoc for AeroTitleBar / AeroResizeHandles / AeroWindowChrome describing the shipped native behavior, with the "Aero Snap limitation" caveat and the "whole row is wrapped in WindowDraggableArea" sentence removed
  - Verified-only wording everywhere: known gaps (double-click, Alt+Space, shared border, max-button press fill, JBR drift) stated as gaps, Windows 10 and non-100% scaling named untested
affects: [22-18 release plan (REL-08), 22 gap-closure round (docs updated after fixes), consumers upgrading to v3.2.0]

# Tech tracking
tech-stack:
  added: [] # docs-only plan; JNA 5.19.1 named in README, added to the project in 22-02
  patterns:
    - "Consumer docs are written strictly from 22-HANDOFF PASS rows: every FAIL/UNCONFIRMED behavior is either omitted or stated as a known gap / untested, never implied working"

key-files:
  created: []
  modified:
    - README.md (## Windows window behavior + subsections, v3.2.0 snippet, JNA tech-stack bullet)
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt (KDoc only)
    - library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt (KDoc only)
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt (KDoc only)

key-decisions:
  - "The README works list carries only 22-HANDOFF PASS rows; the plan's double-click, Alt+Space-menu and multi-monitor-DPI items moved to a 'Known gaps'/'not verified' wording because 22-HANDOFF shows them FAIL/UNCONFIRMED (orchestrator rule: document only what the hand-off proved)"
  - "Drag snapping and Win+arrow keys are qualified 'verified with the standard JDK 21' and the single JBR drift run is named as a gap (blocker #5, unexplained)"
  - "README headings `### Usage`/`### Theming` promoted to `##` so the new `## Windows window behavior` section (required immediately before Usage) does not orphan them in the heading hierarchy"

patterns-established:
  - "KDoc describes the mechanism and routes behavior claims to the README verified-matrix instead of duplicating verification caveats per symbol"

requirements-completed: [REL-06, REL-07, API-04]

# Metrics
duration: 16min
completed: 2026-09-28
---

# Phase 22 Plan 17: REL-06 README + REL-07 KDoc Summary

**Consumer-facing documentation of the v3.2 native window behavior: a README section listing only hand-off-proven behavior with known gaps and untested platforms named, and KDoc for AeroTitleBar / AeroResizeHandles / AeroWindowChrome with the stale "Aero Snap limitation" caveat removed**

## Performance

- **Duration:** ~16 min
- **Started:** 2026-09-28T10:42:20Z
- **Completed:** 2026-09-28T10:58:00Z
- **Tasks:** 2 (2/2 executed, 0 checkpoints)
- **Files modified:** 4 tracked (README.md + 3 Kotlin KDoc files)

## Accomplishments
- README names `v3.2.0` in the dependency snippet and toolchain paragraph, and names `v3.1.0` as the last release without native window management (REL-06)
- New `## Windows window behavior` section: verified works list, known gaps, Windows 10 vs 11 differences (Win10 untested, 100% scaling only), `markAeroTitleBarInteractive` snippet, `nativeWindowManagement = false` opt-out, the D-01 minimum-size rule, the JNA 5.19.1 dependency note (transitive runtime, off the compile classpath, single-version resolution, no JBR needed), and a `### Custom title bar` subsection documenting `rememberAeroWindowChrome` with a Kotlin snippet covering `captionArea` / `captionExclude` / `maximizeButtonArea` / `maximizeInteractionSource` / `isNative` (API-04, D-05)
- KDoc updated in all three files; `grep -c "Aero Snap limitation" AeroTitleBar.kt` = 0 and the "whole row is wrapped in `WindowDraggableArea`" sentence is gone (REL-07); `@param nativeWindowManagement` documents the opt-out
- `git diff` on library sources touches comment lines only; `:library:compileKotlin` green; full suite green at the locked count: `AERO_TEST_COUNT total=592 skipped=0 expected=592`

## Task Commits

Each task was committed atomically:

1. **Task 1: README — Windows window behavior section, v3.2.0 snippet (REL-06)** - `4358fd8` (docs)
2. **Task 2: KDoc for AeroTitleBar, AeroResizeHandles and the marker (REL-07)** - `89e2197` (docs)

## Files Created/Modified
- `README.md` - v3.2.0 snippet + toolchain paragraph, the whole Windows-window-behavior section (works list, gaps, Win10/11, interactive marking, opt-out, minimum size, JNA note, custom title bar), JNA tech-stack bullet
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroTitleBar.kt` - KDoc rewritten for the native path; limitation paragraph and draggable-row sentence removed
- `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` - KDoc: composes nothing on the Windows native path; zones and D-01 minimum elsewhere
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt` - KDoc: captionArea no longer claims double-click maximize; existing member docs kept in final form

## Decisions Made
- Verified-only wording (orchestrator rule 10, T-22-30): every README/KDoc claim was cross-checked against 22-HANDOFF PASS rows before writing; the five known-failed behaviors appear only as "Known gaps", never as working
- `Usage`/`Theming` heading promotion (see Deviations #1)
- JBR wording kept factual and minimal: one sentence in Known gaps naming the single drift run, matching 22-UNCONFIRMED § 12 (not presented as a confirmed JBR defect profile)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - doc structure] Promoted `### Usage` and `### Theming` to `##` headings**
- **Found during:** Task 1 (README edit)
- **Issue:** the plan requires `## Windows window behavior` immediately after the toolchain paragraph and before `### Usage`; inserted literally, `### Usage`/`### Theming` would become subsections of the window-behavior section
- **Fix:** promoted both to top-level `##` headings; the new section keeps the required position and the heading hierarchy stays semantic
- **Files modified:** README.md
- **Verification:** `grep -n "^## \|^### " README.md` shows the section at line 90 between the toolchain paragraph and `## Usage` (line 242)
- **Committed in:** 4358fd8 (Task 1 commit)

**2. [Rule 1 - overclaiming bug] Plan's works-list named FAIL/UNCONFIRMED behaviors**
- **Found during:** Task 1 and Task 2 (all doc text)
- **Issue:** the plan's Task 1 list included "double-click", "Alt+Space menu" and "moving between monitors with different scaling", and Task 2's KDoc sketch named double-click and Alt+Space — SNAP-04/SNAP-05 FAIL, WIN-04 UNCONFIRMED, JBR drift per 22-HANDOFF; documenting them as working violates REL-06/REL-07's "only PASS" must-have and T-22-30
- **Fix:** works list carries only PASS rows (snapping/Win+arrows qualified "verified with the standard JDK 21"); a "Known gaps in this release" sentence names double-click, Alt+Space, shared-border resize, max-button press fill and the JBR drift run; multi-monitor/non-100% scaling moved to the "not verified" wording; existing KDoc claims of "double-click maximize" in `captionArea` docs and the internal "D-02 parity by construction" claim in `TitleBarButton` KDoc removed
- **Files modified:** README.md, AeroTitleBar.kt, AeroWindowChrome.kt
- **Verification:** acceptance criterion "no behavior listed as working has a FAIL/UNCONFIRMED row" checked row-by-row against 22-HANDOFF.md; `grep -rn "double-click" library/src/main/kotlin/com/mordred/aero/components/navigation/` returns nothing
- **Committed in:** 4358fd8, 89e2197 (both task commits)

---

**Total deviations:** 2 auto-fixed (1 blocking doc-structure, 1 overclaiming correctness)
**Impact on plan:** None beyond wording/structure; the plan's placement and coverage requirements are all met.

## Assumption Drift (advisory)

- **Found during:** Tasks 1-2. **Planned:** the plan's `<action>` prose assumed double-click, Alt+Space menu and multi-monitor DPI moves were documentable as working. **Actual:** 22-HANDOFF lists SNAP-04/SNAP-05 as FAIL blockers and WIN-04 as UNCONFIRMED (the 150% display never attached). **Why:** the plan was written against the milestone's target scope, while the hand-off is the post-session truth; docs follow the hand-off (orchestrator rule 10). Advisory only — the same divergence is deviation #2 above.

## Issues Encountered
None - both verify commands passed on the first run.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- REL-06 and REL-07 are done; Plan 18 (REL-08 version bump + tag + JitPack) is the remaining plan
- The gap-closure round for the five FAIL blockers will need to update the README "Known gaps" sentence and the KDoc wording (double-click / Alt+Space / shared border / press fill / JBR drift) after fixes land
- The locked test count stays 592; no code changed in this plan

## Self-Check: PASSED

All 5 modified/created files exist on disk; both task commits (4358fd8, 89e2197) found in git log.

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-28*
