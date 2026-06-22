---
phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
plan: 02
subsystem: ui
tags: [kotlin, compose-desktop, splitpane, layout, tdd]

# Dependency graph
requires:
  - phase: 10-layout
    provides: AeroSplitPane composable, aeroDragSplitter modifier, SplitClamp pure helpers
provides:
  - clampDividerPx guarded against inverted-range throw (PITFALL-B fix, FIXSP-02)
  - AeroSplitPane fraction-based divider state with no remember(totalPx) re-key (PITFALL-A fix, FIXSP-01)
  - Unit test clampInvertedRangeDoesNotThrow locking the guard (FIXSP-04)
affects:
  - 12-03-AeroDateTimeRangePicker (nested AeroSplitPane used in showcase is now stable)
  - 12-04-showcase (SHW-13 visual verification of nested drag behaviour)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Fraction-as-stable-coordinate: store divider as fraction, derive px each recompose — survives totalPx changes without reset"
    - "Inverted-range guard: coerceAtLeast(minFirstPx) before coerceIn to handle squeezed inner panes"

key-files:
  created: []
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt

key-decisions:
  - "Fraction-based SplitPane divider state: var dividerFraction by remember { mutableStateOf(initialSplitFraction) } with val dividerPx derived each recompose; no remember(totalPx) key (PITFALL-A fix)"
  - "clampDividerPx guard: val safeMax = maxPx.coerceAtLeast(minFirstPx) before coerceIn; unit test for inverted range written BEFORE fix (FIXSP-04 TDD mandate)"

patterns-established:
  - "TDD RED-GREEN for pure helpers: write failing test in one commit, apply fix in next commit — proves the test actually caught the bug"

requirements-completed: [FIXSP-01, FIXSP-02, FIXSP-03, FIXSP-04]

# Metrics
duration: 4min
completed: 2026-06-22
---

# Phase 12 Plan 02: Fix B — SplitPane Nested-Drag Freeze + Inverted-Range Crash Summary

**Fraction-based AeroSplitPane divider state + coerceAtLeast guard in clampDividerPx eliminate nested-pane snap-back (FIXSP-01) and inner-pane squeeze crash (FIXSP-02) without changing single-level behaviour (FIXSP-03)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-06-22T12:44:18Z
- **Completed:** 2026-06-22T12:48:47Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Inverted-range test `clampInvertedRangeDoesNotThrow` written first (RED), confirmed failing with `IllegalArgumentException`, then passing after the guard (FIXSP-04 TDD cycle complete)
- `clampDividerPx` guarded: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` prevents `coerceIn(a, b)` throw when `b < a` — eliminates FIXSP-02 crash in nested squeeze
- `AeroSplitPane` converted from `remember(totalPx)` px state to fraction-based state with derived `val dividerPx` — outer drag no longer re-keys inner divider to `initialSplitFraction` (FIXSP-01)
- All 7 SplitClampTest tests green; `compileKotlin` clean

## Task Commits

Each task was committed atomically:

1. **Task 1 (RED): Write inverted-range clamp test** - `38e0de6` (test)
2. **Task 2 (GREEN): Guard clampDividerPx + convert AeroSplitPane to fraction state** - `f4de00f` (fix)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` - Added `clampInvertedRangeDoesNotThrow` test method (RED→GREEN)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` - `clampDividerPx` body: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` + `coerceIn(minFirstPx, safeMax)`
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` - Replaced `remember(totalPx)` px state with `var dividerFraction by remember { ... }` + `val dividerPx = fractionToPx(dividerFraction, totalPx)`; onDrag writes back to `dividerFraction`

## Decisions Made
- Fraction-based state: the stable, viewport-independent coordinate — no `remember(totalPx)` re-key means nested inner divider holds its relative position during outer drag (PITFALL-A fix)
- `coerceAtLeast` guard: when inner pane is squeezed below combined minima, `maxPx < minFirstPx`; `safeMax` collapses the range to a single point rather than throwing (PITFALL-B fix)
- Both fixes committed together because neither fix alone produces correct behaviour — clamp fix without fraction state still allows snap-back; fraction state without clamp fix still throws on deep squeeze

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

Initial `./gradlew :library:test --tests "*.SplitClampTest.clampInvertedRangeDoesNotThrow"` failed to compile due to unresolved `formatAeroDateTime` in `AeroDateTimePickerTest.kt`. On investigation the function already exists at `AeroDateTimePicker.kt:204` — this was a stale Gradle daemon cache issue. Running `--rerun-tasks` and a fresh `compileTestKotlin` confirmed the test suite compiles and the targeted test fails (RED) as expected.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- FIXSP-01/02/03/04 structurally complete; behavioural visual verification of nested drag and squeeze happens in plan 12-04 (SHW-13)
- `AeroSplitPane` API unchanged — no callers need updating
- Plan 12-03 (AeroDateTimeRangePicker) can proceed; no SplitPane dependency there

---
*Phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker*
*Completed: 2026-06-22*
