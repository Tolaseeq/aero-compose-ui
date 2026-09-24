---
phase: 21-migration-release-3-1-0
plan: 02
subsystem: testing
tags: [compose-ui-test, capture-to-image, hover, focus, drag, base-05]

# Dependency graph
requires:
  - phase: 21-01
    provides: "D-01/D-03 .captures/ gitignore rule + the opt-in-writer discretion this plan implements"
provides:
  - "BASE-05 proof: captureToImage() works in desktop runComposeUiTest on Kotlin 2.4.10 / CMP 1.11.1 (tagged node and onRoot)"
  - "UiCapture.kt — opt-in PNG writer (aero.captureDir) + shared SCHEMES list, reused by every BASE-05/D-07 capture test"
  - "library/build.gradle.kts tasks.test forwards -Paero.captureDir to the test JVM (D-03)"
  - "21 permanent hover/press/keyboard-focus/drag state-capture tests for the 7 v3.0 glass components x 3 themes (Base05GlassStateCaptureTest.kt)"
  - "9 permanent drag-capture tests for AeroSplitPane, AeroPanelGroup, AeroDataTable column resize x 3 themes (Base05DragCaptureTest.kt)"
  - "21-UITEST-COVERAGE.md — per-component x theme record of states/assertions/focus-method"
affects: [21-03, 21-04, 21-05, 21-10, 21-11, verification-plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Opt-in image-writing test property (aero.captureDir), mirroring showcase/build.gradle.kts's existing -Paero.scheme forwarding idiom, applied to tasks.test instead of a JavaExec/run task"
    - "Tab-traversal-first focus driving with a requestFocus() fallback recorded per-component in a coverage table, rather than always using VER07's direct requestFocus() shortcut"
    - "Analytic (not semantics-bounds-derived) drag-target computation for M3-hosted controls (AeroSlider's thumb inset, AeroDataTable's Fixed-width column edge) where the real hit target is not the tagged node itself"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/capture/UiCapture.kt
    - library/src/test/kotlin/com/mordred/aero/capture/Base05CaptureProofTest.kt
    - library/src/test/kotlin/com/mordred/aero/capture/Base05GlassStateCaptureTest.kt
    - library/src/test/kotlin/com/mordred/aero/capture/Base05DragCaptureTest.kt
    - .planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md
  modified:
    - library/build.gradle.kts

key-decisions:
  - "AeroSlider's real M3-hosted thumb has no explicit-node fallback for focus (its own testTag lands on the outer wrapping Box, not the M3 Slider's internal focusable node) — Tab traversal alone reached it, so no fallback was needed in practice"
  - "AeroSegmentedControl's focus/hover/press act on the 'B' segment via onNodeWithText, not the outer testTag('target') Row, because the outer Row is only hoverable (not itself a focus stop) — each segment's own selectable() is"
  - "AeroDataTable's column-resize handle position is computed analytically from the known Fixed(150.dp) first-column width rather than derived from the header text node's bounds, since the text node does not span the full column cell width"
  - "All 30 new capture tests (21 glass-state + 9 drag) produced a non-vacuous visual difference for every kept assertion on the first run — no state needed to be dropped as 'no visual change on old toolchain'"

patterns-established:
  - "UiCapture.write(component, theme, state, image) is the single opt-in image-writing entry point every future BASE-05/D-07 capture test (D-07 popup tests, later plans) should call"

requirements-completed: [BASE-05]

# Metrics
duration: ~16min
completed: 2026-09-23
---

# Phase 21 Plan 02: BASE-05 UI capture test suite (old toolchain) Summary

**captureToImage() proven working in desktop runComposeUiTest, then 30 permanent, non-vacuous Compose UI-test captures (21 hover/press/focus/drag states across 7 glass components + 9 drag states across 3 draggable components, all x 3 themes) added as the BASE-05/VER-08 regression baseline on the pre-upgrade toolchain (Kotlin 2.4.10 / CMP 1.11.1), with images written only on explicit opt-in (D-03).**

## Performance

- **Duration:** ~16 min (16:18 -> 16:34)
- **Started:** 2026-09-23T13:18:21Z (approx, prior plan's last commit)
- **Completed:** 2026-09-23T13:34:18Z (last commit)
- **Tasks:** 3/3 completed
- **Files modified/created:** 6 (1 modified, 5 created)

## Accomplishments
- `captureToImage()` is proven to work in desktop `runComposeUiTest` on this toolchain, both on a tagged node and on `onRoot()` — the BASE-05 stop rule did not trigger.
- `UiCapture.write()` writes into `.captures/<component>/<theme>/<state>.png` only when `-Paero.captureDir` is passed; an ordinary `./gradlew test` run adds zero files and leaves `git status --porcelain -- .captures` empty (D-03), proven by an explicit before/after file-count check.
- 21 permanent state-capture tests drive real hover/press/keyboard-focus (and drag, for the two sliders) through `performMouseInput`/`performKeyInput` — never a hand-built `MutableInteractionSource` — for `AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider` and `AeroListItem`, in AeroBlue/AeroDark/Classic.
- 9 permanent drag-capture tests drive `AeroSplitPane`, `AeroPanelGroup` (horizontal, left/center divider) and `AeroDataTable`'s column resize through the same deterministic programmatic-drag pattern as `AeroPanelGroupRecomposeUiTest`, in all three themes.
- `21-UITEST-COVERAGE.md` records, per (component, theme): which states were captured, which assertions are kept as a regression gate, and how keyboard focus was reached.
- Full `./gradlew :library:test --rerun` is green: 502 tests across 91 classes (up from 467 before this plan).

## Task Commits

Each task was committed atomically:

1. **Task 1: captureToImage proof-of-work + opt-in image writer (BASE-05 first step, D-03)** - `96d515c` (test)
2. **Task 2: Glass-component hover/press/keyboard-focus/drag capture tests, 3 themes (BASE-05)** - `441c0ed` (test)
3. **Task 3: Drag capture tests — AeroSplitPane, AeroPanelGroup, AeroDataTable column resize, 3 themes (BASE-05)** - `68ebb9d` (test)

_No separate TDD commits — this plan's type is `execute`, not `tdd`._

## Files Created/Modified
- `library/build.gradle.kts` - `tasks.test` forwards `-Paero.captureDir` to the test JVM's `aero.captureDir` system property (D-03)
- `library/src/test/kotlin/com/mordred/aero/capture/UiCapture.kt` (new) - `internal object UiCapture`: opt-in `write(component, theme, state, image)` (no-op unless `aero.captureDir` is set) + shared `SCHEMES` list (AeroBlue, AeroDark, Classic)
- `library/src/test/kotlin/com/mordred/aero/capture/Base05CaptureProofTest.kt` (new) - `captureToImageWorksOnTaggedNode` / `captureToImageWorksOnRoot`: proves `captureToImage()` on both `onNodeWithTag` and `onRoot()`, asserting non-zero size and >= 2 distinct colours
- `library/src/test/kotlin/com/mordred/aero/capture/Base05GlassStateCaptureTest.kt` (new) - 21 `@Test` methods (`<component><Theme>States`), one private driver per component, each asserting every interactive state's capture differs from that (component, theme)'s own default via the reused `pixelMapsDiffer`
- `library/src/test/kotlin/com/mordred/aero/capture/Base05DragCaptureTest.kt` (new) - 9 `@Test` methods (`<component><Theme>Drag`) for AeroSplitPane/AeroPanelGroup/AeroDataTable column resize
- `.planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md` (new) - 21 + 9 = 30-row coverage table

## Decisions Made
- AeroSlider's own `modifier` param lands on the outer wrapping `Box`, not the M3 `Slider`'s internal focusable thumb node, so no explicit `requestFocus()` fallback node exists for it — only real Tab-key traversal is tried, and it reached the thumb directly on the first run (no fallback needed in practice).
- AeroSegmentedControl's focus/hover/press act on the "B" segment (`onNodeWithText("B")`), not the outer `testTag("target")` `Row` — the outer Row is only `hoverable`, not itself a focus stop; each segment's own `selectable()` is.
- AeroDataTable's column-resize handle screen position is computed analytically from the known `Fixed(150.dp)` first-column width (`tableLeft + 150dp - 4dp` splitter-center inset) rather than derived from the header text node's own bounds, since the header `Text` does not span the full column cell width (it uses `weight(1f, fill = false)` inside padding).
- All 30 new capture tests produced a genuine, non-vacuous visual difference for every kept assertion on the first mandated test run — no state needed to be dropped under the plan's "no visual change on old toolchain" escape hatch.

## Deviations from Plan

None - plan executed exactly as written. No auto-fixes were needed: every mandated test run (proof-of-work, glass-state captures, drag captures, full suite) passed on the first attempt, including the AeroSlider thumb-position and AeroDataTable resize-handle geometry computed from reading the component sources rather than from any existing test precedent.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- BASE-05 is fully proven and captured on the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1): captureToImage works, hover/press/focus/drag of all 10 named components are captured in 3 themes by 30 permanent, non-vacuous tests, with opt-in-only image output (D-03 confirmed clean).
- `21-UITEST-COVERAGE.md` is ready to be extended by later plans (D-07 popup-opened-state tests) using the same `UiCapture.write` mechanism.
- These 30 tests are part of the locked test count TOOL-16 will fix in Plan 05 — the full suite is green at 502 tests across 91 classes as of this plan's last commit.
- No blockers identified for downstream plans (03-14).

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 5 created/modified files found on disk; all 3 task commits (`96d515c`, `441c0ed`, `68ebb9d`) found in git history.
