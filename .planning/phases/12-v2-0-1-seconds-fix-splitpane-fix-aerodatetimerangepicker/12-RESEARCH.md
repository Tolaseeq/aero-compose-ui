# Phase 12: v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker — Research

**Researched:** 2026-06-22
**Domain:** Compose Desktop UI library patch — 2 bug fixes + 1 new compound picker component
**Confidence:** HIGH (all findings sourced directly from repo files at exact line numbers)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **Fix A** — `AeroDateTimePicker` shows seconds in the trigger when `showSeconds = true`. Root cause confirmed: `AeroDateTimePicker.kt:76` hardcodes `"%02d:%02d"`.
- **Fix B** — nested N-pane `AeroSplitPane` (3+ panes, 2+ splitters via `end`-slot nesting) drags without snap-back or crash. Two root causes: `remember(totalPx)` at line 105 (PITFALL-A) and `coerceIn(min>max)` at `SplitClamp.kt:22` (PITFALL-B). Both files must change in one commit.
- **New component** — `AeroDateTimeRangePicker`: dual-calendar range + two time rows + Cancel/Apply commit gate, emitting `(LocalDateTime, LocalDateTime)`.
- **ZERO new dependencies** — all implementation uses the existing stack.
- **No breaking changes** to v2.0 public API.
- **Build order is fixed**: Fix A → Fix B → New Component.
- **Popup layout (new picker)**: time rows go BELOW dual calendar. Layout order: dual calendar → Start time row → End time row → Cancel/Apply.
- **Time rows rendered unconditionally**; `enabled = false` until `rangeState is Selected` (stable popup height for position provider flip logic).
- **Apply gate is mandatory** — auto-close on second date click is explicitly forbidden (PITFALL-E). `nextRangeState` commit pair discarded in day-click handler; Apply is the sole emit site.
- **Pending state keyed on `expanded`** — `remember(expanded) { ... }` for `pendingStartTime`, `pendingEndTime`, `rangeState`, and `leftMonth`.
- **Silent same-day swap (DTR-04)** — at Apply: `if (startDt > endDt) swap` using `LocalDateTime <= LocalDateTime` directly (Comparable in 0.6.2). Extract as `internal fun orderDateTimeRange(...)`.
- **Trigger format**: single-line `DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM` (or `…HH:MM:SS…` when `showSeconds = true`). Separator glyph `→` (same as `AeroDateRangePicker.kt:126`).
- **Fix A before New Component** — `formatAeroDateTime` helper introduced with Fix A, inherited by new component.
- **FIXSP-04** — unit test for `clampDividerPx` with inverted range written BEFORE the fix is applied.
- **SHW-12**: add second `AeroDateTimePicker` instance with `showSeconds = true` alongside the existing one.
- **SHW-11**: add `AeroDateTimeRangePicker` row in `PickersSection` next to `AeroDateRangePicker`, with live `(LocalDateTime, LocalDateTime)` label.
- **SHW-13**: add nested 3-pane demo in `LayoutSection` (2 splitters, inner in `end` slot). Proportions/content are Claude's discretion.
- **SHW-14**: clear the stale "Revisit on publish — kotlinx-datetime declared implementation" note in `PROJECT.md` Key Decisions. Already `api(...)` at `library/build.gradle.kts:27`.
- **W11-01** carry-forward: `Popup` not `Dialog`, never `transparent = true`.
- **PITFALL-02** carry-forward: `AeroCalendarPositionProvider` for all picker popups (not `AeroDropdownPopup`).
- **PITFALL-03** carry-forward: `awaitPointerEventScope` manual drag loop (not `detectDragGestures`). SplitPane drag modifier is unchanged by Fix B.

### Claude's Discretion

- Disabled time-row visual (greyed spinners via `TimeFields(enabled = false)`).
- Label strings ("Start" / "End" vs "Start time" / "End time") — keep terse, English.
- Nested SplitPane demo proportions/content (SHW-13).
- Exact spacing/padding inside the new popup (follow 8dp rhythm from `AeroDateTimePicker`).

### Deferred Ideas (OUT OF SCOPE)

- Hover-preview range highlight (DTR-HOVER-01) — needs `AeroCalendarGrid` API extension.
- Per-endpoint `showSeconds` / `minuteStep` overrides.
- Live inversion error UI on same-day reversed times — silent swap is sufficient.
- Inline (always-visible) picker mode.
- Timezone selection.
- `AeroDropdown` popup-offset regression — separate future milestone.
- `kotlin-datetime` version bump (0.7.x has breaking renames; no API gap in 0.6.2).

</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| FIXDT-01 | `AeroDateTimePicker` trigger shows seconds when `showSeconds = true` (format `HH:MM:SS`) | Root cause confirmed at `AeroDateTimePicker.kt:76`; fix: conditional seconds branch in default formatter |
| FIXDT-02 | Explicit `formatter` parameter takes priority; fix must not break callers with custom formatter | Pattern: compute `displayText` inside composable body conditionally; apply `formatter` when provided |
| FIXSP-01 | Nested N-pane drag does not reset inner divider position | Fix: `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }` with derived `val dividerPx` |
| FIXSP-02 | No exception and no freeze when inner pane squeezed below `minFirst + minSecond` | Fix: `val safeMax = maxPx.coerceAtLeast(minFirstPx)` in `clampDividerPx` |
| FIXSP-03 | Single-level `AeroSplitPane` does not regress | Fraction-based fix preserves behavior: window resize → fraction × totalPx auto-reanchor |
| FIXSP-04 | Unit test for inverted-range `clampDividerPx` written BEFORE the fix | Test file: `SplitClampTest.kt` (extends existing); test must FAIL before fix, PASS after |
| DTR-01 | `AeroDateTimeRangePicker` public composable with trigger + popup (dual calendar + time rows + Cancel/Apply) | Template: merge `AeroDateRangePicker` structure with `AeroDateTimePicker` time row + Apply gate |
| DTR-02 | `AeroDateRangeState` + `nextRangeState` reused verbatim; day click does NOT close or emit | Confirmed: `AeroDateRangePicker.kt:49-78`; commit pair discarded in `onDayClick` |
| DTR-03 | `onRangeSelect` called exactly once, only on Apply, only when `rangeState is Selected` | Apply button: `enabled = rangeState is AeroDateRangeState.Selected`; single emit site |
| DTR-04 | Same-day reversed times silently swapped at Apply | `internal fun orderDateTimeRange(...)` — `if (startDt > endDt) endDt to startDt else startDt to endDt`; `LocalDateTime` is Comparable |
| DTR-05 | `showSeconds` and `minuteStep` applied identically to both time rows | Both `TimeFields` receive same `showSeconds` / `minuteStep` from top-level params |
| DTR-06 | Default trigger format `DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM`; `showSeconds = true` adds `:SS`; user `formatter` overrides | Compute `displayText` inside composable body with conditional seconds; same fix as FIXDT-01 |
| DTR-07 | Cancelled session does not leak into next open | All four pending values (`rangeState`, `leftMonth`, `pendingStartTime`, `pendingEndTime`) keyed on `expanded` |
| DTR-08 | Time rows rendered unconditionally; `clearable`, `onClear`, `minDate`/`maxDate`, `selectableDates`, `enabled` supported; popup uses `Popup` + `AeroCalendarPositionProvider` + `PickerPopupContainer` | `PITFALL-I`: unconditional rows for stable popup height; W11-01/PITFALL-02 carry-forward |
| SHW-11 | `PickersSection` gets `AeroDateTimeRangePicker` demo with live `(LocalDateTime, LocalDateTime)` label | Add `RangeRow` after `AeroDateRangePicker` row in `PickersSection.kt` |
| SHW-12 | `PickersSection` gets second `AeroDateTimePicker` demo with `showSeconds = true` | Add second `AeroDateTimePicker` instance beside existing one |
| SHW-13 | `LayoutSection` gets nested 3-pane demo (outer + inner in `end` slot) | Add `Box(height(...))` with nested `AeroSplitPane` in `LayoutSection.kt` after existing SplitPane demos |
| SHW-14 | Stale `implementation` note in `PROJECT.md` Key Decisions cleared | Replace with: "`api(libs.kotlinx.datetime)` confirmed at `library/build.gradle.kts:27`" |

</phase_requirements>

---

## Summary

Phase 12 is a tightly scoped patch milestone. All root causes are confirmed directly from source at exact file/line numbers. Zero new dependencies, zero new packages, zero build file changes. The only build file already correct: `library/build.gradle.kts:27` declares `api(libs.kotlinx.datetime)`.

**Fix A** is a one-line change to `AeroDateTimePicker.kt:76`: the default formatter hardcodes `"%02d:%02d"` and never consults `showSeconds`. The fix computes `displayText` inside the composable body with a conditional seconds suffix, and extracts `internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String` as a shared helper. This helper is a prerequisite for the new component — Fix A must land first.

**Fix B** has two root causes in two files that must be committed together. `AeroSplitPane.kt:105` uses `remember(totalPx)` as the divider state key; when the inner pane's `totalPx` changes on every outer drag frame, this re-initialises divider state to `initialSplitFraction`. Fix: store a fraction instead of px, derive px at render time with no remember key. `SplitClamp.kt:22` calls `coerceIn(minFirstPx, maxPx)` without checking that `maxPx >= minFirstPx`; when the nested pane is squeezed below the combined minimum, this throws `IllegalArgumentException`. Fix: guard with `val safeMax = maxPx.coerceAtLeast(minFirstPx)`. A unit test for the inverted-range case in `SplitClampTest.kt` must be written before the fix.

**New component** `AeroDateTimeRangePicker` is a structural merge of `AeroDateRangePicker` (dual calendar + state machine) and `AeroDateTimePicker` (time rows + Apply commit gate). All internal primitives already exist; the new file reuses them read-only. The critical architectural decision — locking the Apply gate before writing any composable code — prevents the main pitfall of copying `AeroDateRangePicker`'s auto-close-on-second-click pattern.

**Primary recommendation:** Implement in strict Fix A → Fix B → New Component order. Lock the Apply gate architecture of the new picker (no auto-close on second date click) as the first act before writing its composable body.

---

## Standard Stack

### Core (frozen — zero new dependencies)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Kotlin | 2.1.21 | Language | Unchanged |
| Compose Multiplatform Desktop | 1.7.3 | UI runtime | Unchanged |
| kotlinx-datetime | 0.6.2 | `LocalDate`, `LocalDateTime`, `LocalTime` in public API | Already `api(...)` at `library/build.gradle.kts:27` |
| JDK | 17 | JVM target | Unchanged |

### Test

| Library | Purpose | Config |
|---------|---------|--------|
| kotlin-test | `@Test`, assertions | `testImplementation(libs.kotlin.test)` |
| JUnit Jupiter | JUnit 5 runner | `testImplementation(libs.junit.jupiter)` |

**Installation:** None required. All dependencies already declared.

**Key kotlinx-datetime fact (HIGH confidence, verified from source):** `LocalDateTime` implements `Comparable` in 0.6.2. The `<=` / `>=` operators work directly on `LocalDateTime` values — no `.atStartOfDayIn(TimeZone)` or `Instant` conversion needed. This enables the same-day ordering check (`if (startDt > endDt) swap`) with no helper conversion.

---

## Architecture Patterns

### Files Changed in This Phase

```
library/src/main/kotlin/com/mordred/aero/
├── components/pickers/
│   ├── AeroDateTimePicker.kt          MODIFY  Fix A — formatter + formatAeroDateTime helper
│   └── AeroDateTimeRangePicker.kt     NEW     New component (New C)
└── components/layout/
    ├── AeroSplitPane.kt               MODIFY  Fix B-1 — fraction-based state
    └── internal/splitpane/
        └── SplitClamp.kt             MODIFY  Fix B-2 — coerceAtLeast guard

showcase/src/main/kotlin/com/mordred/showcase/sections/
├── PickersSection.kt                  MODIFY  SHW-11 + SHW-12
└── LayoutSection.kt                   MODIFY  SHW-13

library/src/test/kotlin/com/mordred/aero/components/layout/
└── SplitClampTest.kt                  MODIFY  FIXSP-04 — inverted-range test (before fix)

.planning/PROJECT.md                   MODIFY  SHW-14 — doc hygiene
```

**Primitives reused read-only (zero modifications to these files):**

| Primitive | File | Used By |
|-----------|------|---------|
| `AeroDateRangeState` + `nextRangeState` | `AeroDateRangePicker.kt:49-78` | New component — range state machine, verbatim |
| `combineDateTime` | `AeroDateTimePicker.kt:205` | New component — called twice at Apply (`s.start`/`pendingStartTime` and `s.end`/`pendingEndTime`) |
| `TimeFields` + `assembleTime` | `pickers/internal/TimeFields.kt` | New component — two instances (start + end) |
| `AeroCalendarGrid` | `pickers/internal/calendar/` | New component — two instances |
| `PickerPopupContainer` | `pickers/internal/PickerPopupContainer.kt` | New component — W11-02 glass surface |
| `AeroCalendarPositionProvider` | `components/internal/popup/` | New component — popup positioning |
| `dateIsDisabled` | `AeroDatePicker.kt` | New component — per-cell in both calendar instances |
| `formatAeroDate` | `AeroDatePicker.kt:149` | New component — date portion of trigger string |
| `pxToFraction` + `fractionToPx` | `SplitClamp.kt:27-33` | SplitPane fix — unchanged helpers |
| `aeroDragSplitter` | `components/internal/drag/` | SplitPane — unchanged, not involved in Fix B root causes |

---

### Pattern 1: Fraction as Stable Coordinate (Fix B)

**What:** SplitPane divider stored as `var dividerFraction by remember { mutableStateOf(initialSplitFraction) }`. Pixel position is always derived: `val dividerPx = fractionToPx(dividerFraction, totalPx)`. No `remember` key on `totalPx`.

**When to use:** Any layout component whose position must survive parent container resize. Px is viewport-relative; fraction is viewport-independent.

**Current (buggy) code at `AeroSplitPane.kt:105`:**
```kotlin
var dividerPx by remember(totalPx) {
    mutableStateOf(fractionToPx(initialSplitFraction, totalPx))
}
```

**Fixed code:**
```kotlin
// Fraction is the stable, size-independent coordinate.
var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
// Px is derived every recompose — no remember key on totalPx.
val dividerPx = fractionToPx(dividerFraction, totalPx)

val onDrag: (Float) -> Unit = { delta ->
    val minFirstPx = with(density) { minFirstPaneSize.toPx() }
    val maxPx      = totalPx - with(density) { minSecondPaneSize.toPx() }
    val newPx      = clampDividerPx(dividerPx, delta, minFirstPx, maxPx)
    dividerFraction = pxToFraction(newPx, totalPx)
    onSplitChange?.invoke(dividerFraction)
}
```

**Why fraction works for window resize (FIXSP-03):** When `totalPx` changes (genuine window resize), `val dividerPx = dividerFraction * totalPx` is recomputed from the stored fraction — the divider moves proportionally with no state reset. This is exactly the KDoc comment's stated intent that the original `remember(totalPx)` implementation failed to achieve.

---

### Pattern 2: `clampDividerPx` Inverted-Range Guard (Fix B)

**Current (throws) at `SplitClamp.kt:22`:**
```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float =
    (currentPx + deltaPx).coerceIn(minFirstPx, maxPx)
```

**Fixed code:**
```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float {
    val safeMax = maxPx.coerceAtLeast(minFirstPx)   // guard: coerceIn(a,b) throws when a > b
    return (currentPx + deltaPx).coerceIn(minFirstPx, safeMax)
}
```

**Unit test (write BEFORE fix, must fail before fix and pass after):**
```kotlin
@Test
fun clampInvertedRangeDoesNotThrow() {
    // Inner pane squeezed: maxPx (30f) < minFirstPx (48f)
    // Without the guard this throws IllegalArgumentException
    val result = clampDividerPx(60f, 10f, minFirstPx = 48f, maxPx = 30f)
    assertEquals(48f, result, 0.01f)  // pinned to minFirstPx
}
```

---

### Pattern 3: `formatAeroDateTime` Shared Helper (Fix A prerequisite for New C)

**What:** Extract `internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String` alongside the fix. This makes the new component's trigger formatter correct by default.

**Fix at `AeroDateTimePicker.kt:76` (current):**
```kotlin
formatter: (LocalDateTime) -> String = { ldt ->
    "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
},
```

**Fixed (introduce helper + correct default):**
```kotlin
internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String {
    val timePart = if (showSeconds) {
        "%02d:%02d:%02d".format(ldt.hour, ldt.minute, ldt.second)
    } else {
        "%02d:%02d".format(ldt.hour, ldt.minute)
    }
    return "${formatAeroDate(ldt.date)} $timePart"
}
```

**Updated composable body (FIXDT-02 — custom formatter still takes priority):**
```kotlin
val displayText = value?.let { ldt ->
    formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds)
} ?: ""
```

Note: because `formatter` is a `(LocalDateTime) -> String` parameter (not nullable by current signature), the actual fix is computing `displayText` with a conditional inside the composable body rather than delegating unconditionally to the lambda default. See Code Examples section for the exact resolution approach.

---

### Pattern 4: Apply-Gate Compound Picker Architecture (New C)

**What:** Day clicks update `rangeState` for highlight feedback only — commit pair from `nextRangeState` is intentionally discarded. Apply button is the sole emit + close site.

**Day-click handler (does NOT close, does NOT emit):**
```kotlin
val onDayClick: (LocalDate) -> Unit = { date ->
    val (next, _) = nextRangeState(rangeState, date)  // commit discarded
    rangeState = next
    // do NOT set expanded = false here
    // do NOT call onRangeSelect here
}
```

**Apply button (sole emit site):**
```kotlin
AeroButton(
    text = "Apply",
    enabled = rangeState is AeroDateRangeState.Selected,
    onClick = {
        val s = rangeState
        if (s is AeroDateRangeState.Selected) {
            val (startDt, endDt) = orderDateTimeRange(
                s.start, pendingStartTime, s.end, pendingEndTime
            )
            onRangeSelect(startDt, endDt)
            expanded = false
        }
    },
)
```

---

### Pattern 5: Pending State Keyed on `expanded` (New C)

**What:** All four mutable popup-internal values reinitialised from committed value on every popup open. Cancelled sessions leave no trace.

```kotlin
if (expanded) {
    var rangeState by remember(expanded) {
        mutableStateOf<AeroDateRangeState>(
            if (startValue != null && endValue != null)
                AeroDateRangeState.Selected(startValue.date, endValue.date)
            else
                AeroDateRangeState.Idle
        )
    }
    var leftMonth by remember(expanded) {
        mutableStateOf(startValue?.date ?: todayLocalDate())
    }
    var pendingStartTime by remember(expanded) {
        mutableStateOf(startValue?.time ?: LocalTime(0, 0, 0))
    }
    var pendingEndTime by remember(expanded) {
        mutableStateOf(endValue?.time ?: LocalTime(0, 0, 0))
    }
    // ...
}
```

---

### Pattern 6: Unconditional Time Row Rendering with Enabled Gate (New C)

**What:** Both `TimeFields` rows render unconditionally from the first composition. Popup height is stable from frame 1, preventing a position-provider flip-flop on 768p displays (PITFALL-I).

```kotlin
// BELOW the dual-calendar BoxWithConstraints block:
Row(verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Start", ...)
    TimeFields(
        time = pendingStartTime,
        onTimeChange = { pendingStartTime = it },
        showSeconds = showSeconds,
        minuteStep = minuteStep,
        enabled = rangeState is AeroDateRangeState.Selected,
    )
}
Row(verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("End", ...)
    TimeFields(
        time = pendingEndTime,
        onTimeChange = { pendingEndTime = it },
        showSeconds = showSeconds,
        minuteStep = minuteStep,
        enabled = rangeState is AeroDateRangeState.Selected,
    )
}
```

---

### Popup Layout Structure (New C)

```
PickerPopupContainer {
    Column(padding=8.dp, spacedBy=8.dp) {
        // Dual calendars — responsive branch
        BoxWithConstraints {
            if (maxWidth < 560.dp) Column { leftCalendar(); rightCalendar() }
            else Row { leftCalendar(); rightCalendar() }
        }
        // Start time row (unconditional, enabled=false until Selected)
        Row { Text("Start"); TimeFields(pendingStartTime, ...) }
        // End time row (unconditional, enabled=false until Selected)
        Row { Text("End");   TimeFields(pendingEndTime, ...) }
        // Commit bar
        Row { AeroOutlinedButton("Cancel"); AeroButton("Apply", enabled=rangeState is Selected) }
    }
}
```

---

### Pattern 7: Same-Day Time Ordering at Apply (New C — DTR-04)

**What:** Pure `internal fun orderDateTimeRange(...)` applied only at the Apply `onClick`. Uses `LocalDateTime <= LocalDateTime` directly (Comparable in kotlinx-datetime 0.6.2).

```kotlin
internal fun orderDateTimeRange(
    startDate: LocalDate,
    startTime: LocalTime,
    endDate: LocalDate,
    endTime: LocalTime,
): Pair<LocalDateTime, LocalDateTime> {
    val a = combineDateTime(startDate, startTime)
    val b = combineDateTime(endDate, endTime)
    return if (a <= b) a to b else b to a
}
```

This is unit-testable without Compose. The critical case: `startDate == endDate` and `startTime > endTime` must swap.

---

### Anti-Patterns to Avoid

- **`remember(totalPx)` for divider state:** Re-keys on every outer drag frame in nested layouts. Use fraction-based state instead (PITFALL-A).
- **`coerceIn(min, max)` without range-validity guard:** Throws `IllegalArgumentException` when nested pane is squeezed below combined minimum (PITFALL-B).
- **Auto-close on second date click in datetime range picker:** Closes before user sets time; copied from `AeroDateRangePicker.kt:200` which is correct for date-only but wrong here (PITFALL-E).
- **Hardcoded `"%02d:%02d"` in a composable with `showSeconds` parameter:** Committed seconds are silently truncated in trigger display (PITFALL-H). Always branch on `showSeconds`.
- **Pending state without `remember(expanded)` key:** Cancelled edits leak into the next popup open (PITFALL-G).
- **Conditional time row rendering based on `rangeState`:** Popup height changes after date selection, causing position-provider jump on 768p displays (PITFALL-I).
- **`detectDragGestures` for SplitPane drag:** Banned (PITFALL-03 carry-forward). Use `awaitPointerEventScope` + manual loop (`aeroDragSplitter` modifier already handles this — unchanged by Fix B).

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Date-level range state machine | Custom two-click tracking | `AeroDateRangeState` + `nextRangeState` (verbatim) | Already unit-tested, handles all edge cases (same-month, reverse-click ordering, single-day) |
| Date + time merge | Custom LocalDateTime constructor call inline | `combineDateTime(date, time)` | Captures the correct year/month/day/hour/minute/second decomposition contract |
| Calendar month grid | Custom grid layout | `AeroCalendarGrid` with `rangeStart`/`rangeEnd` params | Handles disabled dates, range highlight, month navigation |
| Hour/minute/second spinner row | Custom spinners | `TimeFields` with `assembleTime` | Handles 24h-only, `minuteStep`, seconds visibility, spinner state sync |
| Glass popup surface | Custom background layers | `PickerPopupContainer` | W11-02 two-layer background pattern; no elevation (clips on Win11 undecorated) |
| Popup positioning with overflow handling | Custom position logic | `AeroCalendarPositionProvider(gap = 4)` | First-frame guard, horizontal right-align overflow, vertical flip-above logic |
| Date format `DD.MM.YYYY` | Custom format string | `formatAeroDate(date)` | Single source of truth for all picker date strings |
| Datetime format with seconds | Custom conditional format | `formatAeroDateTime(ldt, showSeconds)` | Introduced by Fix A; prevents re-introducing PITFALL-H |
| Date availability check | Inline min/max/predicate logic | `dateIsDisabled(date, minDate, maxDate, selectableDates)` | Pure, unit-tested predicate shared across all date pickers |
| Fraction↔px conversion | Inline arithmetic | `fractionToPx` / `pxToFraction` | `pxToFraction` has divide-by-zero guard; consistent with SplitPane contract |

---

## Common Pitfalls

### Pitfall A: `remember(totalPx)` Re-Keys Nested SplitPane Divider State

**What goes wrong:** `AeroSplitPane.kt:105` — `var dividerPx by remember(totalPx) { mutableStateOf(fractionToPx(initialSplitFraction, totalPx)) }`. When the outer SplitPane is dragged, the inner SplitPane receives a new `totalPx` from `BoxWithConstraints`. `remember(totalPx)` treats this as a new key, discards the current state, and reinitialises to `fractionToPx(initialSplitFraction, totalPx)` — the inner divider snaps to the initial fraction on every drag frame.

**Why it happens:** The comment says "reinitialise on totalPx change to preserve the current fraction" — but the implementation reinitialises from `initialSplitFraction`, not from the current fraction. A window resize (the intended trigger) is rare and large; a parent-drag trigger is continuous and small. The two cases were conflated.

**How to avoid:** Store fraction, derive px. No `remember` key needed. On genuine window resize, `val dividerPx = dividerFraction * totalPx` auto-recomputes from the stored fraction.

**Warning signs:** Dragging the outer splitter causes the inner splitter to snap to 50% (or `initialSplitFraction`).

### Pitfall B: `coerceIn(min, max)` Crashes When Nested Pane Squeezed

**What goes wrong:** `SplitClamp.kt:22` — `coerceIn(minFirstPx, maxPx)` with `maxPx = totalPx - minSecondPaneSize.toPx()`. When `totalPx < minFirstPx + minSecondPx`, `maxPx < minFirstPx`, and Kotlin's `Float.coerceIn` throws `IllegalArgumentException: Cannot coerce value to an empty range`. The exception propagates through the Compose layout pass and freezes the inner splitter.

**How to avoid:** `val safeMax = maxPx.coerceAtLeast(minFirstPx)` before `coerceIn`. Write unit test (FIXSP-04) before applying the fix.

**Warning signs:** `IllegalArgumentException: Cannot coerce value to an empty range` in logs; inner splitter becomes unmoveable after outer splitter pushed far in one direction.

### Pitfall C: Float Churn from HiDPI Sub-Pixel `totalPx` Jitter

**What goes wrong:** On 1.25× or 1.5× display scale, `constraints.maxWidth.toFloat()` can oscillate between values (e.g. `623.0f` / `623.5f`) due to weight-based layout rounding. With `remember(totalPx)`, this causes spurious state resets even without drag.

**How to avoid:** Fully eliminated by the Pitfall-A fix (fraction-based state with no remember key). If `remember(totalPx)` is retained for any reason, use `remember(constraints.maxWidth)` (Int key, no jitter).

### Pitfall D: Single-Level SplitPane Regression After Nested Fix

**What goes wrong:** The fix could inadvertently break window-resize behaviour for single-level panes. Without a remember key, the fraction does NOT reset on window resize — this is correct. The pixel position recomputes from the stored fraction via `val dividerPx = dividerFraction * totalPx`. This is the correct v2.0 behaviour; the divider stays at the same visual fraction.

**Warning signs:** After fix, single-level pane at 30% resets to 50% on window resize (would indicate `dividerPx` is still state, not derived).

### Pitfall E: Auto-Close on Second Date Click (Most Critical for New C)

**What goes wrong:** Copying `AeroDateRangePicker.kt:200`'s `if (commit != null) { onRangeSelect(...); expanded = false }` into `AeroDateTimeRangePicker`. The datetime range is NOT complete at second date click — times have not been set. Popup closes before user sees time rows.

**How to avoid:** Discard the `commit` pair from `nextRangeState` in the day-click handler. Use it only as the Apply-enable gate condition. Lock this decision BEFORE writing the composable body.

### Pitfall F: Same-Day Reversed Times in Emitted Range

**What goes wrong:** `nextRangeState` orders dates (`start <= end`) but not times. When `startDate == endDate` and `startTime > endTime`, the emitted pair is chronologically inverted.

**How to avoid:** Extract `internal fun orderDateTimeRange(...)` applied at the Apply `onClick`. `LocalDateTime` is Comparable in 0.6.2 — `<=` works directly.

### Pitfall G: Pending Time State Leaking Across Popup Opens

**What goes wrong:** Without `remember(expanded)` key, `pendingStartTime` / `pendingEndTime` survive popup close. A cancelled time edit appears in the next open.

**How to avoid:** All four pending values keyed on `expanded`. Pattern established at `AeroDateTimePicker.kt:132-133`.

### Pitfall H: `showSeconds` Not Flowing to Trigger Formatter

**What goes wrong:** Default formatter lambda hardcodes `"%02d:%02d"`. `showSeconds` is accessible in the composable body but not inside a parameter default expression (evaluated at call site).

**How to avoid:** Compute `displayText` inside the composable body with a conditional. For `AeroDateTimePicker`: introduce `formatAeroDateTime(ldt, showSeconds)`. For the new component: use the same helper from day one.

### Pitfall I: Popup Height Jump When Time Rows Rendered Conditionally

**What goes wrong:** If `TimeFields` rows are shown only after dates are selected, popup height changes between frame 1 (two calendars) and the post-selection frame (two calendars + two time rows). `AeroCalendarPositionProvider` computed position from the smaller size, then the popup jumps on the 768p flip threshold.

**How to avoid:** Render both `TimeFields` rows unconditionally. Use `enabled = false` for disabled appearance until `rangeState is Selected`.

---

## Code Examples

### Fix A — `formatAeroDateTime` helper and corrected `displayText`

```kotlin
// Source: library/src/.../pickers/AeroDateTimePicker.kt (after fix)
// Internal helper — same package as formatAeroDate; accessible to AeroDateTimeRangePicker
internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String {
    val timePart = if (showSeconds) {
        "%02d:%02d:%02d".format(ldt.hour, ldt.minute, ldt.second)
    } else {
        "%02d:%02d".format(ldt.hour, ldt.minute)
    }
    return "${formatAeroDate(ldt.date)} $timePart"
}

// In the AeroDateTimePicker composable body (replaces value?.let(formatter)):
// Custom formatter takes priority (FIXDT-02); default respects showSeconds (FIXDT-01)
val displayText = value?.let { ldt ->
    formatter(ldt).takeIf { /* caller passed non-default */ } ?: formatAeroDateTime(ldt, showSeconds)
} ?: ""
```

Note: since `formatter` has a non-null default, the practical implementation is to check inside the composable whether the caller supplied a custom formatter. The cleanest approach used by sibling pickers: make `formatter: ((LocalDateTime) -> String)? = null` nullable, then `value?.let { ldt -> formatter?.invoke(ldt) ?: formatAeroDateTime(ldt, showSeconds) } ?: ""`. Alternatively, keep the non-nullable default and compute `displayText` directly inside the body using `showSeconds` — the existing `formatter` parameter is effectively bypassed in the default case (a minor API consideration that FIXDT-02 requires not to break callers with custom formatters).

### Fix B-1 — Fraction-Based SplitPane State (full `onDrag` block)

```kotlin
// Source: library/src/.../layout/AeroSplitPane.kt (after fix)
BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val totalPx = if (orientation == AeroSplitOrientation.Horizontal)
        constraints.maxWidth.toFloat()
    else
        constraints.maxHeight.toFloat()

    var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
    val dividerPx = fractionToPx(dividerFraction, totalPx)  // derived, not stored

    val onDrag: (Float) -> Unit = { delta ->
        val minFirstPx = with(density) { minFirstPaneSize.toPx() }
        val maxPx      = totalPx - with(density) { minSecondPaneSize.toPx() }
        val newPx      = clampDividerPx(dividerPx, delta, minFirstPx, maxPx)
        dividerFraction = pxToFraction(newPx, totalPx)
        onSplitChange?.invoke(dividerFraction)
    }
    // ... layout unchanged
}
```

### Fix B-2 — `clampDividerPx` with Guard

```kotlin
// Source: library/src/.../layout/internal/splitpane/SplitClamp.kt (after fix)
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float {
    val safeMax = maxPx.coerceAtLeast(minFirstPx)
    return (currentPx + deltaPx).coerceIn(minFirstPx, safeMax)
}
```

### FIXSP-04 — Unit Test (write BEFORE fix, must fail first)

```kotlin
// Source: library/src/test/.../layout/SplitClampTest.kt (new test added to existing file)
@Test
fun clampInvertedRangeDoesNotThrow() {
    // Arrange: maxPx (30f) < minFirstPx (48f) — inverted range
    // Without the safeMax guard, coerceIn(48f, 30f) throws IllegalArgumentException
    val result = clampDividerPx(currentPx = 60f, deltaPx = 10f, minFirstPx = 48f, maxPx = 30f)
    // After fix: safeMax = max(30f, 48f) = 48f; coerceIn(48f, 48f) = 48f
    assertEquals(48f, result, 0.01f)
}
```

### `orderDateTimeRange` — Pure Helper for DTR-04

```kotlin
// Source: library/src/.../pickers/AeroDateTimeRangePicker.kt (new file)
internal fun orderDateTimeRange(
    startDate: LocalDate,
    startTime: LocalTime,
    endDate: LocalDate,
    endTime: LocalTime,
): Pair<LocalDateTime, LocalDateTime> {
    val a = combineDateTime(startDate, startTime)
    val b = combineDateTime(endDate, endTime)
    return if (a <= b) a to b else b to a
}
```

### New Component — Public API Signature

```kotlin
// Source: library/src/.../pickers/AeroDateTimeRangePicker.kt (new file)
@Composable
public fun AeroDateTimeRangePicker(
    startValue: LocalDateTime?,
    endValue: LocalDateTime?,
    onRangeSelect: (start: LocalDateTime, end: LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: ((LocalDateTime) -> String)? = null,   // null = use formatAeroDateTime default
    placeholder: String = "Select date & time range",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
    showSeconds: Boolean = false,
    minuteStep: Int = 1,
)
```

### Showcase Nested SplitPane Demo (SHW-13)

```kotlin
// Source: showcase/src/.../sections/LayoutSection.kt (addition)
Text("AeroSplitPane (nested 3-pane)", color = colors.labelText, style = typography.bodyMedium)
Box(Modifier.fillMaxWidth().height(240.dp)) {
    AeroSplitPane(
        orientation = AeroSplitOrientation.Horizontal,
        modifier = Modifier.fillMaxSize(),
        initialSplitFraction = 0.33f,
        start = { Box(Modifier.fillMaxSize()) { Text("Pane 1", color = colors.onSurface) } },
        end = {
            AeroSplitPane(
                orientation = AeroSplitOrientation.Horizontal,
                modifier = Modifier.fillMaxSize(),
                initialSplitFraction = 0.5f,
                start = { Box(Modifier.fillMaxSize()) { Text("Pane 2", color = colors.onSurface) } },
                end   = { Box(Modifier.fillMaxSize()) { Text("Pane 3", color = colors.onSurface) } },
            )
        },
    )
}
```

---

## State of the Art

| Old Approach | Current Approach | Applies To |
|--------------|------------------|------------|
| `remember(totalPx)` float key for divider state | `remember { mutableStateOf(fraction) }` + derived `val px` | `AeroSplitPane.kt` Fix B |
| `coerceIn(min, max)` without range validity check | `val safeMax = max.coerceAtLeast(min); coerceIn(min, safeMax)` | `SplitClamp.kt` Fix B |
| Default formatter hardcodes `HH:MM` ignoring `showSeconds` | `formatAeroDateTime(ldt, showSeconds)` computed in composable body | `AeroDateTimePicker.kt` Fix A |
| `AeroDateRangePicker` auto-close on second click (correct for date-only) | Apply gate (required for datetime — times not yet set at second click) | `AeroDateTimeRangePicker` New C |

**Deprecated/outdated in this phase:**
- `remember(totalPx) { mutableStateOf(fractionToPx(initialSplitFraction, totalPx)) }` — replaced by fraction pattern.
- Hardcoded `"%02d:%02d".format(ldt.hour, ldt.minute)` as the sole default formatter — replaced by `formatAeroDateTime`.

---

## Open Questions

1. **`formatter` parameter nullability in `AeroDateTimeRangePicker`**
   - What we know: `AeroDateTimePicker` has `formatter: (LocalDateTime) -> String` with a default lambda that hardcodes `HH:MM` (the bug). The fix either: (a) makes `formatter` nullable so `null` means "use internal default with showSeconds", or (b) keeps it non-nullable and computes `displayText` inside the body with a conditional ignoring the default.
   - What's unclear: Option (b) changes meaning — caller passes `{ ldt -> ... }` and the composable ignores it if it detects that it's the "default". Option (a) is a minor API surface change (nullable vs. non-nullable parameter).
   - Recommendation: For `AeroDateTimePicker` (FIXDT-02), keep `formatter` non-nullable to avoid a breaking change. Compute `displayText = value?.let { formatAeroDateTime(it, showSeconds).let { s -> formatter(value).takeIf { formatter != defaultFormatter } ?: s } } ?: ""` — but this is awkward. Simpler: keep `formatter` non-nullable, change the default to `{ ldt -> formatAeroDateTime(ldt, showSeconds) }` (the lambda captures `showSeconds` from the composable parameter). **This is valid Kotlin — a default parameter lambda can close over preceding parameters.** For `AeroDateTimeRangePicker` (new file), use nullable `formatter` for clarity.

2. **`nextRangeState` and `combineDateTime` package visibility**
   - What we know: Both are `internal` and declared in `com.mordred.aero.components.pickers`. `AeroDateTimeRangePicker.kt` will be in the same package.
   - What's unclear: Confirm at start of New C implementation that the new file's package declaration matches.
   - Recommendation: Verify with a quick package-declaration check at the top of `AeroDateRangePicker.kt`. Same package = no extra action needed (confirmed from `AeroDateRangePicker.kt:1`: `package com.mordred.aero.components.pickers`).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | kotlin-test + JUnit Jupiter 5 |
| Config file | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.layout.SplitClampTest"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FIXSP-04 | `clampDividerPx` with inverted range does not throw, returns minFirstPx | unit | `./gradlew :library:test --tests "*.SplitClampTest.clampInvertedRangeDoesNotThrow"` | ❌ Wave 0 — add to `SplitClampTest.kt` |
| FIXDT-01 | `formatAeroDateTime(ldt, showSeconds=true)` includes `HH:MM:SS` | unit | `./gradlew :library:test --tests "*.AeroDateTimePickerTest"` | Extend existing `AeroDateTimePickerTest.kt` |
| DTR-04 | `orderDateTimeRange` with same-date reversed times returns ordered pair | unit | `./gradlew :library:test --tests "*.AeroDateTimeRangePickerTest"` | ❌ Wave 0 — new `AeroDateTimeRangePickerTest.kt` |
| DTR-03 | `onRangeSelect` fires exactly once (Apply gate contract) | unit (pure logic) | `./gradlew :library:test --tests "*.AeroDateTimeRangePickerTest"` | ❌ Wave 0 — same new test file |
| FIXSP-01/02/03 | SplitPane fraction behavior, single-level regression | unit (pure math) | `./gradlew :library:test --tests "*.SplitClampTest"` | Extend existing |
| FIXDT-02 | Custom `formatter` param takes priority (no breaking change) | unit | Extend `AeroDateTimePickerTest.kt` | Extend existing |
| SHW-11..14 | Visual demos visible on three themes | manual | Run `:showcase` app | — |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test`
- **Per wave merge:** `./gradlew :library:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] New test method `clampInvertedRangeDoesNotThrow` in `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` — covers FIXSP-04; must be written BEFORE applying Fix B (requirement).
- [ ] New test methods for `formatAeroDateTime` in `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimePickerTest.kt` — covers FIXDT-01.
- [ ] New file `library/src/test/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePickerTest.kt` — covers DTR-03 (Apply gate), DTR-04 (`orderDateTimeRange` same-day swap), and the `nextRangeState`-derived Apply-enable logic.

---

## Sources

### Primary (HIGH confidence — all sourced directly from repo)

- `library/src/main/kotlin/.../pickers/AeroDateTimePicker.kt` — `formatter` bug at line 76; `remember(expanded)` pattern at lines 132-133; Apply gate at lines 178-188; `combineDateTime` at line 205
- `library/src/main/kotlin/.../pickers/AeroDateRangePicker.kt` — `AeroDateRangeState`, `nextRangeState` (lines 49-78); auto-close at line 200; dual-calendar `BoxWithConstraints` / 560dp branch (lines 245-255); `→` trigger format at line 126
- `library/src/main/kotlin/.../layout/AeroSplitPane.kt` — `remember(totalPx)` bug at line 105; `maxPx` computation at lines 110-112
- `library/src/main/kotlin/.../layout/internal/splitpane/SplitClamp.kt` — `clampDividerPx` without guard at line 22; `pxToFraction` zero-guard at line 33
- `library/src/main/kotlin/.../pickers/internal/TimeFields.kt` — `showSeconds`, `minuteStep`, `enabled`, `assembleTime` signature confirmed
- `library/src/main/kotlin/.../pickers/internal/PickerPopupContainer.kt` — W11-02 two-layer background; no elevation; `cornerRadius = 8.dp`
- `library/src/main/kotlin/.../internal/popup/AeroCalendarPositionProvider.kt` — `popupContentSize == IntSize.Zero` first-frame guard; vertical flip logic confirmed
- `library/src/main/kotlin/.../pickers/AeroDatePicker.kt` — `formatAeroDate` at line 149; `dateIsDisabled` confirmed
- `library/build.gradle.kts:27` — `api(libs.kotlinx.datetime)` confirmed
- `library/src/test/kotlin/.../layout/SplitClampTest.kt` — existing test coverage; confirms test package and style
- `showcase/src/main/kotlin/.../sections/PickersSection.kt` — existing `AeroDateTimePicker` demo at lines 78-86; existing `AeroDateRangePicker` demo at lines 89-103; `RangeRow` helper
- `showcase/src/main/kotlin/.../sections/LayoutSection.kt` — existing single-level SplitPane demos; `Box(height)` wrapper pattern

### Secondary (MEDIUM confidence)

- `.planning/research/PITFALLS.md` — PITFALL-A through PITFALL-J, all cross-referenced to source lines above
- `.planning/research/ARCHITECTURE.md` — file-change map, popup layout diagram, API shape
- `.planning/research/SUMMARY.md` — executive summary, confidence assessment
- Kotlin stdlib KDoc: `Float.coerceIn(minimumValue, maximumValue)` — "Throws IllegalArgumentException if minimumValue is greater than maximumValue" (PITFALL-B basis)

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Standard stack | HIGH | All dependency versions confirmed from `build.gradle.kts` and `libs.versions.toml` |
| Fix A (formatter) | HIGH | Bug line confirmed from source (line 76); fix pattern established in Kotlin; helper pattern consistent with `formatAeroDate` |
| Fix B (SplitPane) | HIGH | Both root causes confirmed at exact line numbers; `Float.coerceIn` throw documented in stdlib KDoc |
| New component architecture | HIGH | All reused primitives confirmed as existing `internal` functions in-package; popup layout derived verbatim from `AeroDateRangePicker` + `AeroDateTimePicker` |
| Pitfalls | HIGH | All sourced from actual source files; no third-party research needed |
| Showcase changes | HIGH | Target files read; `RangeRow` helper pattern confirmed |

**Research date:** 2026-06-22
**Valid until:** 2026-07-22 (stable stack; fraction-based pattern is correct regardless of CMP version changes)

---

*Phase 12 research: aero-compose-ui v2.0.1 — all three deliverables investigated.*
*Source-grounded, zero external research required — all findings from direct codebase inspection.*
