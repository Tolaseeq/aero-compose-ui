---
phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker
verified: 2026-06-22T17:00:00Z
status: passed
score: 18/18 must-haves verified
re_verification: false
---

# Phase 12: v2.0.1 Seconds Fix / SplitPane Fix / AeroDateTimeRangePicker Verification Report

**Phase Goal:** Fix seconds trigger in AeroDateTimePicker, fix nested SplitPane freeze/crash, add AeroDateTimeRangePicker component, all showcase demos, doc hygiene.
**Verified:** 2026-06-22
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Setting `showSeconds = true` on AeroDateTimePicker shows HH:MM:SS in the trigger | VERIFIED | `formatAeroDateTime` branches on `showSeconds`; displayText = `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)` at AeroDateTimePicker.kt:88 |
| 2 | Default `showSeconds = false` still shows HH:MM with no visual change | VERIFIED | `"%02d:%02d".format(ldt.hour, ldt.minute)` in the `else` branch of `formatAeroDateTime` (line 207-209); unit-tested in `formatAeroDateTimeOmitsSecondsWhenDisabled` |
| 3 | Custom formatter used verbatim — Fix A does not override it | VERIFIED | Nullable `formatter: ((LocalDateTime) -> String)? = null`; dispatch is `formatter?.invoke(ldt) ?: formatAeroDateTime(...)` — non-null formatter short-circuits; `customFormatterTakesPriorityOverDefault` test passes |
| 4 | `internal fun formatAeroDateTime(ldt, showSeconds)` helper exists for new component to inherit | VERIFIED | Present at AeroDateTimePicker.kt:204 as `internal`; same package as AeroDateTimeRangePicker.kt — accessible without visibility changes |
| 5 | Stale "Revisit на publish" note in PROJECT.md replaced with factual `api(...)` record | VERIFIED | Grep for "Revisit на publish" returns no matches; PROJECT.md line 170 contains `api(libs.kotlinx.datetime)` |
| 6 | 3-pane nested AeroSplitPane: dragging outer splitter does not move inner divider | VERIFIED | `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }` — no `remember(totalPx)` key anywhere in AeroSplitPane.kt; regression fix in 7f38c0c adds `rememberUpdatedState(totalPx)` and reads `currentPx = fractionToPx(dividerFraction, liveTotalPx)` inside drag closure |
| 7 | Squeezing inner pane below minFirstPaneSize + minSecondPaneSize does not throw | VERIFIED | `clampDividerPx`: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` followed by `coerceIn(minFirstPx, safeMax)` — prevents IllegalArgumentException when maxPx < minFirstPx |
| 8 | Single-level AeroSplitPane drag/clamp/resize re-anchor unchanged from v2.0 | VERIFIED | Fraction-based state with derived `val dividerPx = fractionToPx(dividerFraction, totalPx)` — existing clamp tests (clampAtMinLeftEdge, clampAtMaxRightEdge, freeMoveInRange) all still present and structurally identical |
| 9 | Inverted-range unit test written before fix (TDD red precondition) | VERIFIED | Commit 38e0de6 adds `clampInvertedRangeDoesNotThrow` to SplitClampTest.kt; commit f4de00f applies the guard — separate commits confirm RED before GREEN |
| 10 | Clicking second date does not close popup and does not call onRangeSelect | VERIFIED | `onDayClick` discards commit via `val (next, _) = nextRangeState(rangeState, date)` (AeroDateTimeRangePicker.kt:200); no `onRangeSelect` call and no `expanded = false` inside `onDayClick` |
| 11 | Apply is disabled until both dates selected (rangeState is Selected) | VERIFIED | `AeroButton(text = "Apply", enabled = rangeState is AeroDateRangeState.Selected, ...)` at line 315; `if (commit != null)` auto-close pattern is absent |
| 12 | onRangeSelect receives ordered pair with start <= end; same-day reversed times silently swapped | VERIFIED | `orderDateTimeRange` at Apply onClick (line 319): `if (a <= b) a to b else b to a`; 4 unit tests cover swap, ordered, cross-day, equal cases |
| 13 | Cancel/click-outside resets pending state; reopening shows no trace | VERIFIED | Four `remember(expanded)` blocks (rangeState, leftMonth, pendingStartTime, pendingEndTime) re-initialize from committed values on each open |
| 14 | showSeconds and minuteStep apply equally to both time rows | VERIFIED | Both `TimeFields(...)` calls pass `showSeconds = showSeconds, minuteStep = minuteStep` (lines 277-278, 297-298) |
| 15 | Default trigger renders DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM; user formatter overrides | VERIFIED | `val fmt: (LocalDateTime) -> String = formatter ?: { formatAeroDateTime(it, showSeconds) }` at line 127; trigger: `"${fmt(startValue)} → ${fmt(endValue)}"` |
| 16 | Time rows render unconditionally; enabled=false until Selected | VERIFIED | Both `TimeFields` rows are outside any conditional branch (unconditional); `enabled = rangeState is AeroDateRangeState.Selected` on each |
| 17 | PickersSection has AeroDateTimeRangePicker row with live label + showSeconds contrast demos | VERIFIED | PickersSection.kt imports `AeroDateTimeRangePicker`; `RangeRow(label = "AeroDateTimeRangePicker")` with `onRangeSelect` wired to `dtRangeStart`/`dtRangeEnd`; two AeroDateTimePicker rows labeled `(showSeconds=true)` and `(showSeconds=false)` |
| 18 | LayoutSection has nested 3-pane AeroSplitPane (inner in end slot) | VERIFIED | LayoutSection.kt lines 88-106: outer `initialSplitFraction = 0.33f`, inner `AeroSplitPane` in `end = { ... }` with `initialSplitFraction = 0.5f` |

**Score:** 18/18 truths verified

---

### Required Artifacts

| Artifact | Provides | Status | Details |
|----------|----------|--------|---------|
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` | `formatAeroDateTime` helper + nullable formatter + showSeconds-aware displayText | VERIFIED | Contains `internal fun formatAeroDateTime(`, `formatter: ((LocalDateTime) -> String)? = null`, `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)`; no `"%02d:%02d".format` in default path outside helper |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt` | Unit tests for formatAeroDateTime (seconds on/off) and custom-formatter priority | VERIFIED | Contains `formatAeroDateTimeOmitsSecondsWhenDisabled`, `formatAeroDateTimeIncludesSecondsWhenEnabled`, `customFormatterTakesPriorityOverDefault`; 6 total tests |
| `.planning/PROJECT.md` | Corrected kotlinx-datetime decision row | VERIFIED | Contains `api(libs.kotlinx.datetime)` at line 170; stale "Revisit на publish" absent |
| `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` | Inverted-range no-throw test (FIXSP-04) | VERIFIED | Contains `clampInvertedRangeDoesNotThrow`; asserts `assertEquals(48f, result, 0.01f)`; 7 total tests |
| `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` | clampDividerPx with coerceAtLeast guard | VERIFIED | Contains `val safeMax = maxPx.coerceAtLeast(minFirstPx)` and `coerceIn(minFirstPx, safeMax)` |
| `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` | Fraction-based divider state; regression fix with rememberUpdatedState | VERIFIED | Contains `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }`, `val dividerPx = fractionToPx(dividerFraction, totalPx)`, `val liveTotalPx by rememberUpdatedState(totalPx)`, `currentPx = fractionToPx(dividerFraction, liveTotalPx)`; NO `remember(totalPx)` anywhere |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt` | Public `AeroDateTimeRangePicker` composable + `orderDateTimeRange` helper | VERIFIED | 335 lines; contains `public fun AeroDateTimeRangePicker(`, `internal fun orderDateTimeRange(`; Apply-gate architecture correct; single `onRangeSelect` call site |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePickerTest.kt` | orderDateTimeRange swap tests (DTR-04) + Apply-gate logic (DTR-03) | VERIFIED | 6 tests: `sameDayReversedTimesSwapToOrdered`, `sameDayOrderedTimesUnchanged`, `crossDayDateDominatesOverTimeOfDay`, `equalDateTimesUnchanged`, `applyDisabledAfterOnlyStartClick`, `applyEnabledForSingleDayRange` |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt` | AeroDateTimeRangePicker row (SHW-11) + showSeconds contrast demos (SHW-12) | VERIFIED | Import present; `RangeRow(label = "AeroDateTimeRangePicker")` with live label; state holders `dtRangeStart`, `dtRangeEnd`, `dtSecondsValue`; both `(showSeconds=true)` and `(showSeconds=false)` rows present |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` | Nested 3-pane AeroSplitPane demo (SHW-13) | VERIFIED | Label "AeroSplitPane (nested 3-pane)"; outer `initialSplitFraction = 0.33f`; inner `AeroSplitPane` in `end = {` slot with `initialSplitFraction = 0.5f` |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| AeroDateTimePicker composable body | `formatAeroDateTime(ldt, showSeconds)` | `formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)` | WIRED | Line 88 in AeroDateTimePicker.kt; pattern exact match |
| AeroSplitPane onDrag | `clampDividerPx + pxToFraction` | `dividerFraction = pxToFraction(newPx, liveTotalPx)` | WIRED | Lines 121-122 in AeroSplitPane.kt; live state via rememberUpdatedState for totalPx, direct MutableState read for dividerFraction |
| AeroSplitPane render | `fractionToPx(dividerFraction, totalPx)` | `val dividerPx = fractionToPx(dividerFraction, totalPx)` | WIRED | Line 108; layout reads this derived val; no `remember(totalPx)` present |
| Apply button onClick | `orderDateTimeRange + onRangeSelect` | Sole emit site guarded by `rangeState is AeroDateRangeState.Selected` | WIRED | Lines 316-322; only one `onRangeSelect(` in the entire file; `if (commit != null)` auto-close (PITFALL-E) is absent |
| Trigger displayText | `formatAeroDateTime(ldt, showSeconds)` | Default formatter inherited from Fix A | WIRED | Lines 127-131; `formatter ?: { formatAeroDateTime(it, showSeconds) }` |
| Both TimeFields rows | `rangeState is AeroDateRangeState.Selected` | enabled gate | WIRED | Lines 278 and 298; three occurrences total (Start row, End row, Apply button) |
| AeroDateTimeRangePicker demo | live label state | `onRangeSelect` updates remembered `(LocalDateTime, LocalDateTime)` pair | WIRED | PickersSection.kt line 123: `onRangeSelect = { start, end -> dtRangeStart = start; dtRangeEnd = end }`; displayed at lines 128-130 |
| Nested SplitPane demo | outer end slot hosts inner AeroSplitPane | `end = { AeroSplitPane(...) }` | WIRED | LayoutSection.kt line 96-104; inner pane confirmed in end slot |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| FIXDT-01 | 12-01 | showSeconds-aware trigger format | SATISFIED | `formatAeroDateTime` branches on `showSeconds`; wired in displayText |
| FIXDT-02 | 12-01 | Custom formatter used verbatim | SATISFIED | Nullable param; `formatter?.invoke(ldt)` short-circuits default |
| FIXSP-01 | 12-02 | Nested inner divider holds fractional position | SATISFIED | Fraction-based state; no `remember(totalPx)` re-key; regression 7f38c0c uses `rememberUpdatedState` |
| FIXSP-02 | 12-02 | No throw on inverted-range clamp | SATISFIED | `coerceAtLeast(minFirstPx)` guard in SplitClamp.kt |
| FIXSP-03 | 12-02 | Single-level drag/clamp/resize unchanged | SATISFIED | Derived px from fraction; existing clamp tests structurally preserved |
| FIXSP-04 | 12-02 | Inverted-range test written before fix (TDD red) | SATISFIED | Commits 38e0de6 (test RED) then f4de00f (fix GREEN) — separate commits in git log |
| DTR-01 | 12-03 | Public AeroDateTimeRangePicker composable | SATISFIED | Public composable in AeroDateTimeRangePicker.kt; dual calendar + time rows + Cancel/Apply |
| DTR-02 | 12-03 | Day click does not close/emit — commit gate | SATISFIED | `onDayClick` discards commit pair; no `onRangeSelect` inside it |
| DTR-03 | 12-03 | `onRangeSelect` fires exactly once — only at Apply | SATISFIED | Single `onRangeSelect(` call site; guarded by `rangeState is Selected` |
| DTR-04 | 12-03 | Same-day reversed times silently swapped | SATISFIED | `orderDateTimeRange`: `if (a <= b) a to b else b to a`; 4 unit tests green |
| DTR-05 | 12-03 | showSeconds and minuteStep apply to both time rows | SATISFIED | Both `TimeFields` calls pass identical `showSeconds = showSeconds, minuteStep = minuteStep` |
| DTR-06 | 12-03 | Default trigger uses formatAeroDateTime; formatter overrides | SATISFIED | `formatter ?: { formatAeroDateTime(it, showSeconds) }` in trigger displayText |
| DTR-07 | 12-03 | Pending state keyed on `expanded`; no cross-open leaks | SATISFIED | Four `remember(expanded)` blocks confirmed |
| DTR-08 | 12-03 | Time rows unconditional; Popup+AeroCalendarPositionProvider; all params | SATISFIED | Both TimeFields outside conditional; Popup (not Dialog); PickerPopupContainer; clearable/onClear/minDate/maxDate/selectableDates/enabled all present in signature |
| SHW-11 | 12-04 | AeroDateTimeRangePicker showcase row with live label | SATISFIED | PickersSection.kt: row present; live label with dtRangeStart/dtRangeEnd state; human sign-off approved |
| SHW-12 | 12-04 | showSeconds=true shows seconds in trigger; contrast demo | SATISFIED | Two AeroDateTimePicker rows with explicit labels; human sign-off approved |
| SHW-13 | 12-04 | Nested 3-pane AeroSplitPane demo; FIXSP-01/02 verifiable | SATISFIED | LayoutSection.kt: nested structure confirmed; human sign-off approved (including after regression fix 7f38c0c) |
| SHW-14 | 12-01 | PROJECT.md stale note removed | SATISFIED | "Revisit на publish" absent; `api(libs.kotlinx.datetime)` present at line 170 |

All 18 requirements SATISFIED. No orphaned requirements.

---

### Anti-Patterns Found

None found. Full scan of all modified files produced no stubs, empty implementations, TODO/FIXME markers, console.log-only handlers, or hardcoded placeholder returns.

Specific checks:
- AeroDateTimePicker.kt: no hardcoded `"%02d:%02d"` in the default displayText path (only inside formatAeroDateTime's else branch, which is the correct implementation)
- AeroDateTimeRangePicker.kt: no `if (commit != null)` auto-close (PITFALL-E); no `return null`; no `return {}` 
- AeroSplitPane.kt: no `remember(totalPx)` present anywhere in file
- SplitClamp.kt: no bare `coerceIn(minFirstPx, maxPx)` — safeMax guard in place

---

### Human Verification (Completed)

SHW-11, SHW-12, and SHW-13 were verified via human visual sign-off (per plan 12-04 design — no automated UI test harness in scope). The sign-off covered all three themes (AeroBlue, AeroDark, Classic) and was approved by the user. A SplitPane drag regression found during sign-off was diagnosed and fixed in commit `7f38c0c` before approval was given.

The following items remain human-observable but were already signed off:

1. **SHW-12 (seconds in trigger):** `AeroDateTimePicker (showSeconds=true)` — entering a non-zero seconds value and clicking Apply must show `DD.MM.YYYY HH:MM:SS` in the trigger. Contrast with `(showSeconds=false)` instance. — **Approved on three themes.**

2. **SHW-11 (AeroDateTimeRangePicker UX):** Apply-gate behavior (no auto-close on second date click), time row enable/disable, live label update, Cancel resets state, same-day swap. — **Approved on three themes.**

3. **SHW-13 (nested SplitPane):** Outer splitter dragged full range — inner splitter holds fractional position. Inner pane squeezed to minimum — no freeze or crash. — **Approved on three themes after regression fix.**

---

### Notable: Regression Fix During Execution

During plan 12-04 sign-off, a drag regression was found in the nested SplitPane demo. The root cause was a stale captured value in the `pointerInput` drag closure: `dividerPx` had been converted to a plain `val Float` as part of the fraction-based refactor, so the closure captured its initial value. Every drag computed `initialPx + per-frame-delta` and snapped back.

Fix in commit `7f38c0c`:
- Reads `currentPx = fractionToPx(dividerFraction, liveTotalPx)` inside the closure (live MutableState read)
- Wraps `totalPx` in `rememberUpdatedState` as `liveTotalPx` so outer-pane drag propagates to inner pane

This fix is present in the current codebase and was confirmed during sign-off. The FIXSP-01 requirement is fully satisfied.

---

### Gaps Summary

No gaps. All 18 requirements are implemented, substantive, and wired. All artifacts exist with correct content. All key links are confirmed in code. Human sign-off was completed and approved.

---

_Verified: 2026-06-22_
_Verifier: Claude (gsd-verifier)_
