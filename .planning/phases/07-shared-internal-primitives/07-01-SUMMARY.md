---
phase: 07-shared-internal-primitives
plan: 01
subsystem: ui
tags: [kotlinx-datetime, hsv, popup-position-provider, calendar-grid, jvm-unit-tests]

# Dependency graph
requires:
  - phase: 04-icon-foundation
    provides: AeroIcons (CaretLeft / CaretRight extension properties used by AeroCalendarGrid header)
  - phase: 01-foundation
    provides: AeroTheme.colors / AeroTheme.typography accessors used inside the calendar composable
provides:
  - kotlinx-datetime 0.6.2 on the :library classpath (Phase 8 picker family consumes LocalDate end-to-end)
  - AeroColorMath (rgbToHsv / hexToRgb / hexToRgba / rgbToHex) — pure HSV/RGB/HEX utilities for ColorPicker
  - AeroCalendarPositionProvider — wide-popup positioning replacing AeroPopupPositionProvider for date-pickers (PITFALL-02 / PITFALL-08)
  - AeroCalendarGrid composable + daysInMonth helper — month grid backbone for AeroDatePicker / AeroDateRangePicker / AeroDateTimePicker
  - 07-SPIKE-touchslop.md — locked v2.0 decision: awaitPointerEventScope manual loop is mandatory for in-content Canvas drag
affects: [phase-08-pickers, phase-09-data, phase-10-layout]

# Tech tracking
tech-stack:
  added: [kotlinx-datetime 0.6.2]
  patterns:
    - "HSV is single source of truth — RGB / HEX are derived views (PITFALL-15)"
    - "Hue convention [0f, 360f] degrees to match Color.hsv requirePrecondition"
    - "PopupPositionProvider first-frame guard via popupContentSize == IntSize.Zero (struct equality), NOT >= windowSize"
    - "TDD red-green per task: failing test commit, then minimal-impl commit"

key-files:
  created:
    - "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt"
    - ".planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md"
  modified:
    - "gradle/libs.versions.toml — appended kotlinxDatetime version + library coordinate"
    - "library/build.gradle.kts — added implementation(libs.kotlinx.datetime)"

key-decisions:
  - "Hue convention LOCKED to degrees [0f, 360f] (NOT [0f, 1f]) — matches Color.hsv requirePrecondition; documented as ADR in AeroColorMath KDoc"
  - "AeroCalendarPositionProvider first-frame guard uses popupContentSize == IntSize.Zero, NOT >= windowSize (the broken legacy heuristic that lets wide popups slip through)"
  - "Width overflow on wide popup right-aligns to anchor.right - popup.width and never flips Top/Bottom"
  - "Touchslop spike skipped — awaitPointerEventScope manual loop is the locked v2.0 pattern regardless of upstream issue #343 status"
  - "Plan typography.body / typography.caption substituted with bodyMedium / label (the actual AeroTypography fields) — Rule 3 deviation"

patterns-established:
  - "Pure-function utilities live in components/.../internal/ subpackages with file-level top-level internal funs (matches GlassModifiers convention)"
  - "PopupPositionProvider classes are internal classes with private gap parameter — composable consumers instantiate per-call"
  - "Calendar logic helpers (daysInMonth, leap-year) exposed as internal so JVM unit tests can verify without Compose UI test harness"
  - "Compile-smoke pattern via Class.forName(...Kt) confirms a @Composable file is on classpath without rendering it"

requirements-completed: []

# Metrics
duration: 10m
completed: 2026-05-04
---

# Phase 7 Plan 1: Logic and Tests Summary

**kotlinx-datetime 0.6.2 wired in; AeroColorMath HSV utility, AeroCalendarGrid composable, and AeroCalendarPositionProvider all landed with 27 JUnit 5 tests covering PITFALL-02, PITFALL-08, and PITFALL-15.**

## Performance

- **Duration:** ~10 minutes (executor wall clock; first ./gradlew :library:compileKotlin spent ~1m43s warming the build cache)
- **Started:** 2026-05-04T15:56:39Z
- **Completed:** 2026-05-04T16:06:41Z
- **Tasks:** 4
- **Files created:** 7
- **Files modified:** 2

## Accomplishments

- kotlinx-datetime 0.6.2 ↔ Kotlin 2.1.21 compatibility validated by `./gradlew :library:compileKotlin` — first-compile acceptance criterion satisfied; fallback to `0.7.1-0.6.x-compat` not needed.
- AeroColorMath utility: `rgbToHsv`, `hexToRgb`, `hexToRgba`, `rgbToHex` — all `internal`, all top-level functions in `com.mordred.aero.components.pickers.internal.color`. Round-trip drift contract `Color.hsv(0,1,1) → rgb → rgbToHsv` returns hue within `0.001f` (PITFALL-15 absent at the utility level).
- AeroCalendarPositionProvider: PopupPositionProvider implementation that does NOT width-lock and uses `popupContentSize == IntSize.Zero` as the first-frame guard. Width overflow re-anchors horizontally (anchor.right − popup.width) without flipping Top/Bottom. PITFALL-02 (width-clip) + PITFALL-08 (first-frame flash) both defused.
- AeroCalendarGrid composable: locked signature, kotlinx-datetime LocalDate end-to-end, prev/next month buttons render `AeroIcons.CaretLeft` / `AeroIcons.CaretRight`. `daysInMonth(LocalDate)` helper is `internal` so the test class can verify Gregorian leap-year rule without driving the composable.
- 27 tests across 3 new test classes (AeroColorMathTest 16, AeroCalendarPositionProviderTest 4, AeroCalendarGridTest 7) all green; full `./gradlew :library:test` passes with no warnings introduced by new files.

## Task Commits

Each task was committed atomically. TDD tasks (2, 3, 4) have separate RED (failing test) and GREEN (implementation) commits.

1. **Task 1: kotlinx-datetime 0.6.2 + touchslop spike** — `2a4eaae` (chore)
2. **Task 2: AeroColorMath utility (TDD)**
   - RED: `b48ecd5` (test) — 16 tests added, compileTestKotlin fails
   - GREEN: `cb18cc6` (feat) — implementation added, all 16 tests pass
3. **Task 3: AeroCalendarPositionProvider (TDD)**
   - RED: `130da39` (test) — 4 tests added, compileTestKotlin fails
   - GREEN: `7c421ad` (feat) — provider class added, all 4 tests pass
4. **Task 4: AeroCalendarGrid composable (TDD)**
   - RED: `719a372` (test) — 7 tests added, compileTestKotlin fails
   - GREEN: `35f56c7` (feat) — composable + daysInMonth helper added, all 7 tests pass; redundant `else` branches in exhaustive `when` blocks cleaned up before commit

## Files Created/Modified

- `gradle/libs.versions.toml` — appended `kotlinxDatetime = "0.6.2"` and `kotlinx-datetime` library entry
- `library/build.gradle.kts` — added `implementation(libs.kotlinx.datetime)` after `kotlinx-coroutines-core`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` — HSV/RGB/HEX utilities + locked hue ADR in KDoc
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` — wide-popup PopupPositionProvider
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` — month-grid composable + `daysInMonth` helper + Gregorian leap-year function
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` — 16 tests
- `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` — 4 tests
- `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` — 7 tests (incl. compile-smoke)
- `.planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md` — locked-v2.0-pattern spike note

## Decisions Made

- **Hue convention `[0f, 360f]`** (locked ADR in `AeroColorMath.kt` KDoc). Rationale: `androidx.compose.ui.graphics.Color.hsv(...)` calls `requirePrecondition(hue in 0f..360f)` and would throw `IllegalArgumentException` if hue were normalized to `[0,1]`. Locking degrees here eliminates `* 360f` boundary multiplications at every call site in Phase 8 (`AeroHsvColorSquare`, `AeroHueSlider`).
- **First-frame guard via `IntSize.Zero` struct equality** (in `AeroCalendarPositionProvider`). Rationale: the legacy heuristic `popupContentSize >= windowSize` does not trip for wide-but-finite calendars (560×400 < 1024×800), so a 0×0 first-frame computes garbage offsets and flashes. `IntSize.Zero` is the only condition that reliably means "unmeasured".
- **Width overflow does NOT flip Top/Bottom.** A wide popup near the right edge right-aligns horizontally (`anchor.right − popup.width`) and stays below the anchor. This keeps the picker visually anchored even on narrow screens.
- **Touchslop spike skipped.** `awaitPointerEventScope` + manual loop is the locked v2.0 pattern regardless of upstream JetBrains/compose-jb #343 status; documented in `07-SPIKE-touchslop.md`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] Substituted typography.bodyMedium / typography.label for plan's typography.body / typography.caption**
- **Found during:** Task 4 (AeroCalendarGrid composable)
- **Issue:** Plan source listing references `AeroTheme.typography.body` (for the month/year label and day cells) and `AeroTheme.typography.caption` (for the day-of-week header). `AeroTypography` exposes `title`, `bodyLarge`, `bodyMedium`, `bodySmall`, `label` — there is no `body` or `caption` field, so the listing would have failed to compile.
- **Fix:** Used `typography.bodyMedium` for the month/year header + day cells, and `typography.label` for the Mon..Sun row. Both are visually equivalent to the plan's intent (medium body for primary text, smaller emphatic label for column headers).
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt`
- **Verification:** `./gradlew :library:test` exits 0; compile-smoke test in `AeroCalendarGridTest` confirms the file is on the classpath.
- **Committed in:** `35f56c7` (Task 4 GREEN commit)

**2. [Rule 1 — Bug/cleanup] Removed redundant `else` branches in exhaustive `when` blocks**
- **Found during:** Task 4 (after first GREEN compile)
- **Issue:** Kotlin compiler emitted 2 warnings: `'when' is exhaustive so 'else' is redundant here.` for `daysInMonth` and `Month.englishName()`. The plan listing kept defensive `else` branches that the compiler treated as dead code under the kotlinx-datetime `Month` enum.
- **Fix:** Removed the `else -> 30` and `else -> name` branches. Both `when`s are exhaustive over `Month` values (12 entries) so the compiler is satisfied.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt`
- **Verification:** `./gradlew :library:test :library:compileKotlin` runs clean; no warnings from the new file.
- **Committed in:** `35f56c7` (folded into Task 4 GREEN commit before push, since the warning surfaced after Write but before commit)

---

**Total deviations:** 2 auto-fixed (1 blocking — wrong API names; 1 minor cleanup — exhaustive-when warning)
**Impact on plan:** Both auto-fixes preserve the plan's stated behavior; no scope change. The typography substitution does not affect the locked component contract or visuals — it just routes through the actual API. PITFALL-02 / PITFALL-08 / PITFALL-15 mitigations all remain intact.

## Issues Encountered

- The pre-existing `AeroToastHostState` warning (non-public primary constructor exposed via generated `copy()` method) appeared in the compile log on first build. Out of scope for plan-01 (different file, predates v2.0). Not addressed; logged here for visibility. No deferred-items.md was created since this is a single isolated pre-existing warning, not a new discovery.

## User Setup Required

None — no external service configuration required. `./gradlew :library:test` runs locally on JVM.

## Next Phase Readiness

- Plan-02 (visuals + scratch) can now consume `AeroColorMath` for the AeroHsvColorSquare/AeroHueSlider visuals and `AeroCalendarGrid` for the scratch-section eyes-on confirmation; both APIs are stable and tested.
- Phase 8 picker family inherits a green compile gate: kotlinx-datetime is on the classpath, the position provider replaces the broken AeroDropdownPopup heuristic for date popups, and HSV math has zero-drift round-trip.
- No blockers; existing `AeroPopupPositionProvider.kt` is intentionally untouched (verified via `git diff --quiet`) so v1.x dropdown callers remain unaffected.

## Self-Check: PASSED

Verification confirmed against acceptance criteria:
- All 7 created files exist at the locked paths.
- All commit hashes resolve via `git log --oneline`: `2a4eaae`, `b48ecd5`, `cb18cc6`, `130da39`, `7c421ad`, `719a372`, `35f56c7`.
- `./gradlew :library:test` exits 0 (full suite green).
- `./gradlew :library:compileKotlin` exits 0.
- `git diff --quiet HEAD library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt` reports unchanged.

---
*Phase: 07-shared-internal-primitives*
*Completed: 2026-05-04*
