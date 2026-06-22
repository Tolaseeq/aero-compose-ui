# Project Research Summary

**Project:** aero-compose-ui v2.0.1
**Domain:** Compose Desktop UI component library -- patch milestone (2 bug fixes + 1 new component)
**Researched:** 2026-06-22
**Confidence:** HIGH

---

## Executive Summary

v2.0.1 is a tightly scoped patch milestone with two confirmed bug fixes and one new component that is architecturally a composition of existing primitives. All three deliverables are implementable with zero new dependencies and zero build file changes. kotlinx-datetime 0.6.2 is already declared api-scoped at library/build.gradle.kts:27; the stale Revisit-on-publish note in PROJECT.md Key Decisions is factually wrong and must be cleared as doc hygiene -- this is the only administrative action for the milestone. LocalDateTime implements Comparable in 0.6.2, so <= and >= operators work directly for ordering range endpoints with no helper conversion needed.

The two bug fixes have confirmed root causes with minimal, surgical fixes. Fix A (AeroDateTimePicker seconds not shown in trigger) is a single-site change to the default formatter at line 76 -- the lambda closes over showSeconds but currently ignores it; the fix computes displayText inside the composable body with a conditional seconds suffix. Fix B (AeroSplitPane nested freeze) has two root causes that must land together: remember(totalPx) re-keys divider state on every outer-drag frame (cause of snap-back), and coerceIn(min, max) throws IllegalArgumentException when the inner pane is squeezed below minFirst + minSecond (cause of the freeze). A unit test for clampDividerPx with an inverted range must be written before the fix is applied.

AeroDateTimeRangePicker is a structural merge of AeroDateRangePicker (dual calendar + AeroDateRangeState state machine) and AeroDateTimePicker (time rows + Apply commit gate), emitting (LocalDateTime, LocalDateTime). Every internal primitive needed -- AeroCalendarGrid, TimeFields, nextRangeState, combineDateTime, AeroCalendarPositionProvider, PickerPopupContainer -- already exists and is reused read-only. The new formatAeroDateTime(ldt, showSeconds) internal helper, introduced as part of Fix A, is shared by the new component, making Fix A the natural prerequisite.

---

## Key Findings

### Recommended Stack

**Zero new dependencies. Zero build file changes.**

| Technology | Version | Status |
|------------|---------|--------|
| Kotlin | 2.1.21 | Unchanged |
| Compose Desktop | 1.7.3 | Unchanged |
| kotlinx-datetime | 0.6.2 | Already api-scoped at library/build.gradle.kts:27 -- confirmed |
| JDK | 17 | Unchanged |

**Dependency configuration resolved (orchestrator-verified fact):** library/build.gradle.kts:27 declares api(libs.kotlinx.datetime). The Architecture/Pitfalls still-implementation/publish-debt note is WRONG. The publish-time transitive-type leak is ALREADY resolved. The only residual action is doc hygiene: clear the stale Revisit-on-publish note from PROJECT.md Key Decisions. Zero new dependencies for this milestone.

**LocalDateTime ordering:** LocalDateTime implements Comparable in 0.6.2. The <= / >= Kotlin operators work directly -- no .atStartOfDayIn(TimeZone) conversion or Instant arithmetic needed. The existing nextRangeState idiom (if (clicked >= current.start)) applies identically to LocalDateTime.

**Explicitly excluded for this milestone:**
- kotlinx-datetime version bump -- 0.7.x introduced breaking renames; no API gap exists in 0.6.2
- java.time types in picker signatures -- breaks API parity with existing pickers
- Any new Gradle module -- single :library module constraint through v2.x

### Expected Features

**Fix A -- AeroDateTimePicker seconds trigger (regression fix):**
- Root cause confirmed: AeroDateTimePicker.kt line 76 default formatter hardcodes %02d:%02d and never consults showSeconds
- Minimal fix: compute displayText inside composable body with conditional seconds suffix; introduce internal fun formatAeroDateTime(ldt, showSeconds) shared helper
- Existing callers passing an explicit formatter are unaffected; callers relying on the default now correctly see seconds when showSeconds = true

**Fix B -- AeroSplitPane nested freeze (regression fix, two root causes):**
- Root cause 1 (AeroSplitPane.kt:105): remember(totalPx) re-keys divider state on every frame when outer pane resizes; fix: var dividerFraction by remember { mutableStateOf(initialSplitFraction) } with val dividerPx = fractionToPx(dividerFraction, totalPx) derived each recompose
- Root cause 2 (SplitClamp.kt:22): coerceIn(minFirstPx, maxPx) throws when maxPx < minFirstPx; fix: val safeMax = maxPx.coerceAtLeast(minFirstPx) guard before coerceIn
- Both files must change in the same commit; write unit test for inverted-range case in clampDividerPx first

**AeroDateTimeRangePicker (new component -- table stakes):**

| Feature | Implementation note |
|---------|---------------------|
| Dual calendar (AeroCalendarGrid x2) | Reused verbatim from AeroDateRangePicker |
| AeroDateRangeState + nextRangeState | Reused verbatim; date-level state machine unchanged |
| Two TimeFields rows | pendingStartTime / pendingEndTime, both keyed on expanded |
| Apply commit gate | enabled = rangeState is AeroDateRangeState.Selected; auto-close on second date click prohibited |
| Same-day ordering | Silent swap at Apply using LocalDateTime <= LocalDateTime; extract as internal fun orderDateTimeRange for unit testing |
| Pending state isolation | All four pending values keyed on expanded; cancelled partial session never leaks |
| TimeFields rendered unconditionally | enabled = false until dates chosen; popup height stable from frame 1 |
| Trigger format | DD.MM.YYYY HH:MM[:SS] -> DD.MM.YYYY HH:MM[:SS] |
| showSeconds + minuteStep parity | Same values applied to both time rows; no per-endpoint overrides |
| Full parameter set | startValue, endValue, onRangeSelect, modifier, formatter, placeholder, clearable, onClear, minDate, maxDate, selectableDates, enabled, showSeconds, minuteStep |

**Anti-features (explicitly out of scope for v2.0.1):**

| Excluded | Reason |
|----------|--------|
| Hover-preview range highlight | Needs AeroCalendarGrid API extension; disproportionate for a patch milestone |
| Per-endpoint showSeconds / minuteStep | Doubles API surface; no confirmed use case |
| Live inversion error UI on same-day time reversal | Silent swap at Apply is simpler and sufficient |
| Inline (always-visible) mode | Explicitly deferred to v2.x per PROJECT.md |
| Timezone selector | Separate feature domain; component works with LocalDateTime only |
| onStartChange / onEndChange partial emission | Violates commit-gate contract |

### Architecture Approach

v2.0.1 touches exactly four files. No new packages, no structural changes.

**Files changed:**

| File | Change |
|------|--------|
| components/pickers/AeroDateTimePicker.kt | Fix A: default formatter seconds branch (1-site change) |
| components/layout/AeroSplitPane.kt | Fix B-1: replace remember(totalPx) with fraction-based state |
| components/layout/internal/splitpane/SplitClamp.kt | Fix B-2: coerceAtLeast guard in clampDividerPx |
| components/pickers/AeroDateTimeRangePicker.kt | NEW file: full new component |

**Primitives reused read-only (zero modifications):**
AeroCalendarGrid, TimeFields, AeroDateRangeState, nextRangeState, combineDateTime, AeroCalendarPositionProvider, PickerPopupContainer, dateIsDisabled, formatAeroDate, pxToFraction, fractionToPx, aeroDragSplitter

**Key architectural patterns:**
1. Fraction as stable coordinate -- SplitPane divider stored as fraction [0..1]; px derived at render time (val dividerPx = dividerFraction * totalPx). Fraction does not change when container resizes.
2. Apply gate for compound pickers -- never auto-close when multiple independent inputs must be combined before the value is complete.
3. Sealed state machine for range selection -- in AeroDateTimeRangePicker the commit pair from nextRangeState is discarded in the day-click handler; Apply is the sole emit site.

### Critical Pitfalls

1. **remember(totalPx) re-keys nested SplitPane on every outer drag (PITFALL-A)** -- Confirmed at AeroSplitPane.kt:105. Fix: fraction-based state with no remember key; derive px each recompose. PITFALL-A and PITFALL-B must land together.

2. **coerceIn(min, max) crash when nested pane squeezed (PITFALL-B)** -- Confirmed at SplitClamp.kt:22. Fix: val safeMax = maxPx.coerceAtLeast(minFirstPx). Write unit test for inverted range before applying fix.

3. **Auto-close on second date click replicated from AeroDateRangePicker (PITFALL-E)** -- The if (commit != null) { expanded = false } pattern at AeroDateRangePicker.kt:200 must NOT be copied. First design decision -- lock before writing composable code.

4. **showSeconds not flowing to trigger formatter (PITFALL-H)** -- Root cause of Fix A; must not be replicated in new component. Compute displayText inside composable body; never use unconditional %02d:%02d in a picker with showSeconds.

5. **Pending time state leaking across popup opens (PITFALL-G)** -- Key pendingStartTime and pendingEndTime on expanded. Established pattern in AeroDateTimePicker.kt:132-133.

6. **Popup height jump when time rows rendered conditionally (PITFALL-I)** -- Render both TimeFields rows unconditionally; use enabled = false until dates selected. Layout stable from frame 1 for position provider flip logic on 768p displays.

7. **Same-day reversed times in emitted range (PITFALL-F)** -- At Apply: val (s, e) = if (startLdt <= endLdt) startLdt to endLdt else endLdt to startLdt. LocalDateTime <= LocalDateTime works directly in 0.6.2.

---

## Implications for Roadmap

All three deliverables are architecturally independent. Recommended build order: Fix A -> Fix B -> New Component.

### Phase 1: Fix A -- AeroDateTimePicker Seconds Trigger

**Rationale:** Smallest change (1 file, 1 site); fixes visible regression; establishes formatAeroDateTime helper inherited by the new component. Must land before new component to avoid writing the same bug pattern twice.

**Delivers:** Correct HH:MM:SS display when showSeconds = true. internal fun formatAeroDateTime(ldt, showSeconds) available for reuse.

**Addresses:** PITFALL-H

**Files changed:** AeroDateTimePicker.kt only

**Verification:** showSeconds = true, set seconds = 45, Apply -- trigger shows HH:MM:45. Default showSeconds = false still shows HH:MM (regression check required).

**Research flag:** None -- single-file, single-pattern fix.

---

### Phase 2: Fix B -- AeroSplitPane Nested Freeze

**Rationale:** Two-part fix that must land together; independent from Fix A. Placed second so Fix A is verifiable before tackling the two-file SplitPane change.

**Delivers:** 3-pane layouts work -- inner divider holds position during outer drag; no crash when squeezed. Single-level behavior unchanged.

**Addresses:** PITFALL-A, PITFALL-B, PITFALL-C (eliminated implicitly by PITFALL-A fix), PITFALL-D (single-level regression guard)

**Files changed:** AeroSplitPane.kt + SplitClamp.kt (must change together)

**Pre-fix requirement:** Write unit test for clampDividerPx with inverted range (minFirstPx > maxPx) before applying the fix.

**Verification:** 3-pane showcase -- drag outer splitter, inner divider holds position. Squeeze inner pane below 96dp -- no IllegalArgumentException. Window resize -- single-level divider fraction preserved.

**Research flag:** None -- both root causes confirmed from source with exact line numbers.

---

### Phase 3: New Component -- AeroDateTimeRangePicker

**Rationale:** Largest surface area; benefits from Fix A being complete (formatter pattern established). Placed last for defect containment.

**First design decision (lock before writing composable code):** Apply gate architecture -- nextRangeState commit pair discarded in day-click handler; only Apply button triggers onRangeSelect + expanded = false.

**Delivers:** AeroDateTimeRangePicker with (LocalDateTime, LocalDateTime) output, dual-calendar range, dual time rows, Apply gate, same-day ordering, trigger format DD.MM.YYYY HH:MM[:SS] -> DD.MM.YYYY HH:MM[:SS]. Showcase entry in PickersSection.kt.

**New internal code:**
- formatAeroDateTime -- from Phase 1, already exists when Phase 3 begins
- internal fun orderDateTimeRange(startDate, startTime, endDate, endTime): Pair -- pure, unit-testable; applied at Apply onClick

**Reused read-only:** AeroCalendarGrid x2, TimeFields x2, AeroDateRangeState, nextRangeState, combineDateTime x2, AeroCalendarPositionProvider, PickerPopupContainer

**Addresses:** PITFALL-E, PITFALL-F, PITFALL-G, PITFALL-H (range trigger), PITFALL-I

**Verification (from PITFALLS.md Looks-Done-But-Isnt checklist):**
- Second date click -- popup stays open, time rows visible
- Apply disabled until both dates selected
- Cancel -- onRangeSelect not called
- Same date, startTime=15:00, endTime=08:00, Apply -- emitted start.time < end.time
- Cancel after time edit -- reopen shows committed value or 00:00, not cancelled edit
- showSeconds = true, non-zero seconds, Apply -- trigger shows HH:MM:SS -> HH:MM:SS
- showSeconds = false (default) -- trigger shows HH:MM
- Three-theme visual (AeroBlue, AeroDark, Classic)
- No transparent = true in new file (W11-01 carry-forward)

**Research flag:** None -- structural composition of two existing components; all patterns confirmed from codebase.

---

### Phase Ordering Rationale

- Fix A before New Component: formatAeroDateTime lands with Fix A; new component inherits it without re-introducing the seconds bug
- Fix B after Fix A: independent but slightly more complex; cleaner to have Fix A verified first
- New Component last: largest surface area; defect containment; formatter pattern already established
- Each phase independently verifiable and committable in isolation

### Research Flags

All three phases have HIGH-confidence findings from direct source inspection. No phase requires /gsd:research-phase.

- Phase 1 (Fix A): Standard Compose default parameter pattern -- well-understood
- Phase 2 (Fix B): Standard Compose state management; root causes confirmed with exact line numbers
- Phase 3 (New Component): Structural composition confirmed from codebase; no implementation unknowns

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | api(libs.kotlinx.datetime) confirmed at library/build.gradle.kts:27; all versions confirmed from libs.versions.toml |
| Features | HIGH | Bug root causes confirmed from source line numbers; AeroDateTimeRangePicker features derived from established codebase patterns |
| Architecture | HIGH | All reused primitives confirmed as existing internal components; popup layout derived from AeroDateRangePicker pattern |
| Pitfalls | HIGH | Both SplitPane root causes confirmed from source (line 105 / line 22); formatter bug confirmed at line 76; all pitfall patterns are established Compose anti-patterns |

**Overall confidence:** HIGH

### Gaps to Address

- **Doc hygiene only:** Clear the stale Revisit-on-publish note from PROJECT.md Key Decisions. The build file already has api; the note is factually wrong.
- **nextRangeState visibility:** Confirm package-level internal access from AeroDateTimeRangePicker.kt before coding Phase 3. If same package, no action needed.
- **combineDateTime visibility:** Same consideration -- confirm during Phase 3 setup.

---

## Sources

### Primary (HIGH confidence)

- library/build.gradle.kts:27 -- api(libs.kotlinx.datetime) confirmed
- gradle/libs.versions.toml -- kotlinxDatetime = 0.6.2, Kotlin 2.1.21, Compose Desktop 1.7.3
- components/pickers/AeroDateTimePicker.kt:76 -- formatter bug confirmed; remember(expanded) at lines 132-133; Apply gate at lines 178-188
- components/pickers/AeroDateRangePicker.kt -- AeroDateRangeState, nextRangeState, dual-calendar layout, auto-close at line 200 confirmed
- components/layout/AeroSplitPane.kt:105 -- remember(totalPx) re-key confirmed
- components/layout/internal/splitpane/SplitClamp.kt:22 -- coerceIn without guard confirmed
- components/pickers/internal/TimeFields.kt -- showSeconds, minuteStep, assembleTime confirmed
- components/pickers/internal/calendar/AeroCalendarGrid.kt -- range params, isDisabled, onMonthChange confirmed
- components/pickers/internal/PickerPopupContainer.kt -- glass popup surface confirmed
- components/internal/popup/AeroCalendarPositionProvider.kt -- popup position provider confirmed
- https://github.com/Kotlin/kotlinx-datetime/blob/v0.6.2/core/common/src/LocalDateTime.kt -- LocalDateTime Comparable confirmed at source level
- .planning/PROJECT.md -- v2.0.1 scope, root-cause descriptions, Key Decisions table

### Secondary (MEDIUM confidence)

- MUI X DateRangePicker, daterangepicker.com -- inform anti-feature decisions (defer hover-preview, inline mode)
- Kotlin stdlib KDoc -- Float.coerceIn throws IllegalArgumentException when minimumValue > maximumValue

---

*Research completed: 2026-06-22*
*Ready for roadmap: yes*