# Architecture Research

**Domain:** Compose Desktop UI component library — v2.0.1 patch: two fixes + AeroDateTimeRangePicker
**Researched:** 2026-06-22
**Confidence:** HIGH (all findings verified directly from source files)

---

## Overview

v2.0.1 is a patch milestone. Nothing new is added to the `:library` module structure. All three
deliverables live inside the already-established package layout.

```
library/src/main/kotlin/com/mordred/aero/
│
├── components/pickers/
│   ├── AeroDateTimePicker.kt          MODIFY — fix default formatter (Fix A)
│   ├── AeroDateRangePicker.kt         REFERENCE ONLY — pattern source for new component
│   └── AeroDateTimeRangePicker.kt     NEW — new file (New C)
│
└── components/layout/
    ├── AeroSplitPane.kt               MODIFY — fraction-preservation fix (Fix B)
    └── internal/splitpane/
        └── SplitClamp.kt             MODIFY — add coerceIn guard (Fix B, part 2)
```

No new packages, no new internal helpers, no showcase changes required for the fixes.
The new component reuses existing internal primitives exclusively.

---

## (a) AeroDateTimeRangePicker — Integration Points

### Existing Primitives Reused (zero new internal helpers needed)

| Primitive | Location | How AeroDateTimeRangePicker uses it |
|-----------|----------|-------------------------------------|
| `AeroCalendarGrid` | `pickers/internal/calendar/` | Instantiated twice — left calendar (leftMonth) and right calendar (leftMonth + 1 month). Each receives `rangeStart`/`rangeEnd` highlight params from live `rangeState`. Identical to how AeroDateRangePicker uses it. |
| `TimeFields` | `pickers/internal/` | Instantiated twice — one row for `pendingStartTime`, one row for `pendingEndTime`. Each receives its own `showSeconds`/`minuteStep` params (same values — the component has a single `showSeconds` and `minuteStep` param at top level). |
| `AeroDateRangeState` + `nextRangeState` | `pickers/AeroDateRangePicker.kt` (internal) | The sealed state machine and pure transition function are reused verbatim. The date-click logic is identical — `nextRangeState` returns a commit pair only on the second click, and the component uses that as the Apply-enable gate for dates. |
| `combineDateTime` | `pickers/AeroDateTimePicker.kt` (internal) | Called twice at Apply time: `combineDateTime(committedStart, pendingStartTime)` and `combineDateTime(committedEnd, pendingEndTime)`. |
| `AeroCalendarPositionProvider` | `components/internal/popup/` | Used as the `Popup` position provider, identical to all other pickers. |
| `PickerPopupContainer` | `pickers/internal/` | Wraps the entire popup content, identical to all other pickers. |
| `dateIsDisabled` | `pickers/AeroDatePicker.kt` (internal) | Applied per-cell in both `AeroCalendarGrid` instances; same `minDate`/`maxDate`/`selectableDates` params as AeroDateRangePicker. |
| `formatAeroDate` | `pickers/` (internal, used by AeroDateRangePicker) | Used in the default formatter for each endpoint in the trigger display. |
| `AeroTextField` + `AeroIconButton` | existing v1.0 components | Trigger field and calendar icon button — identical to all other pickers. |
| `AeroButton` + `AeroOutlinedButton` | existing v1.0 components | Cancel/Apply buttons — identical to AeroDateTimePicker. |

### New Internal State Beyond AeroDateRangePicker

AeroDateRangePicker holds: `rangeState`, `leftMonth`, `expanded`.

AeroDateTimeRangePicker adds to that:

```
var pendingStartTime: LocalTime   // initialized from startValue?.time ?? LocalTime(0,0,0)
var pendingEndTime: LocalTime     // initialized from endValue?.time ?? LocalTime(0,0,0)
```

Both are keyed on `expanded` (same as `pendingDate` in AeroDateTimePicker) so a cancelled
session never leaks pending time edits into the next open.

### The Apply Gate — Why Auto-Close Cannot Fire on Second Date Click

AeroDateRangePicker auto-closes when `nextRangeState` emits a non-null commit pair (second
date click). That works because the output type is `(LocalDate, LocalDate)` — complete at
the moment of the second click.

AeroDateTimeRangePicker's output type is `(LocalDateTime, LocalDateTime)`. The time components
are not set until the user adjusts the time spinners (or accepts the defaults). Auto-closing on
the second date click would emit the committed datetime before the user has had a chance to set
the time — this is the exact anti-pattern AeroDateTimePicker's own Apply gate prevents.

The resolution: date-click still drives `nextRangeState` as before (for range highlight
feedback), but the Apply button is the ONLY emit site. The Apply button is enabled only when
`rangeState is AeroDateRangeState.Selected` (i.e. both date endpoints are committed).

```
// Guarded onApplyClick — single emit site (parallel to AeroDateTimePicker's pendingDate guard)
val onApplyClick: () -> Unit = {
    val s = rangeState
    if (s is AeroDateRangeState.Selected) {
        onRangeSelect(
            combineDateTime(s.start, pendingStartTime),
            combineDateTime(s.end,   pendingEndTime),
        )
        expanded = false
    }
}
```

### Popup Layout

The popup stacks the dual calendars (same BoxWithConstraints / Row-vs-Column responsive layout
as AeroDateRangePicker), then adds two TimeFields rows below the calendars, then the
Cancel/Apply button row. The overall layout column:

```
PickerPopupContainer {
    Column(padding=8.dp, spacedBy=8.dp) {
        // Dual calendars (BoxWithConstraints — Row if >= 560.dp, Column if narrower)
        BoxWithConstraints { leftCalendar() + rightCalendar() }
        // Time rows
        Row { Text("From:"); TimeFields(pendingStartTime) }
        Row { Text("To:");   TimeFields(pendingEndTime) }
        // Commit bar
        Row { AeroOutlinedButton("Cancel"); AeroButton("Apply", enabled = rangeState is Selected) }
    }
}
```

"From:" / "To:" labels are plain `Text` at `AeroTheme.typography.label` color `labelText` —
no new component needed.

### Public API Shape

```kotlin
@Composable
public fun AeroDateTimeRangePicker(
    startValue: LocalDateTime?,
    endValue: LocalDateTime?,
    onRangeSelect: (start: LocalDateTime, end: LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDateTime) -> String = { ldt ->
        "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
        // NOTE: same seconds-aware fix as Fix A must be applied here too when showSeconds=true
    },
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

Full API parity with AeroDateTimePicker (`showSeconds`, `minuteStep`). The `formatter` default
must be seconds-aware from day one (apply the same fix as Fix A — see section (c)).

---

## (b) AeroSplitPane Nested Freeze — Root Cause and Fix

### The Two Root Causes (both must be fixed)

**Root cause 1 — remember(totalPx) re-keys on every outer drag**

Current code in `AeroSplitPane.kt` line 105:
```kotlin
var dividerPx by remember(totalPx) {
    mutableStateOf(fractionToPx(initialSplitFraction, totalPx))
}
```

When the outer SplitPane is dragged, the nested SplitPane's `totalPx` changes (because its
parent pane resizes). `remember(totalPx)` treats `totalPx` as a cache key — any change
discards the old `MutableState` and allocates a fresh one initialised from `initialSplitFraction`.
This is the freeze: the inner divider snaps back to 50% on every outer drag frame.

**Root cause 2 — coerceIn(minFirstPx, maxPx) throws when min > max**

Current code in `SplitClamp.kt` line 22:
```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float =
    (currentPx + deltaPx).coerceIn(minFirstPx, maxPx)
```

`coerceIn(a, b)` requires `a <= b`. The caller computes `maxPx = totalPx - minSecondPaneSize.toPx()`.
When the nested pane is squeezed below `minFirstPaneSize + minSecondPaneSize` (i.e. `totalPx <
minFirstPx + minSecondPx`), `maxPx < minFirstPx` and `coerceIn` throws
`IllegalArgumentException: Cannot coerce value to an empty range`. That exception propagates
through the Compose layout pass and freezes the splitter.

### The Clean Fix

**Fix B-1: Preserve fraction across totalPx change instead of re-keying on px**

Replace the `remember(totalPx)` with a fraction-preserving pattern. The fraction is the stable
invariant; px is derived from it. When `totalPx` changes (window resize or parent drag), the
fraction stays fixed and the px position is recalculated from it without re-initialising state.

```kotlin
// Fraction is the stable, size-independent coordinate.
var dividerFraction by remember { mutableStateOf(initialSplitFraction) }

// Px is always derived. No remember key on totalPx.
val dividerPx = fractionToPx(dividerFraction, totalPx)

val onDrag: (Float) -> Unit = { delta ->
    val minFirstPx = with(density) { minFirstPaneSize.toPx() }
    val maxPx = totalPx - with(density) { minSecondPaneSize.toPx() }
    val newPx = clampDividerPx(dividerPx, delta, minFirstPx, maxPx)
    dividerFraction = pxToFraction(newPx, totalPx)
    onSplitChange?.invoke(dividerFraction)
}
```

`pxToFraction` already guards `totalPx <= 0f` (returns `0f`). `dividerPx` used in layout is
now a plain `val`, not a `var` state — no state allocation on `totalPx` change at all.

This approach matches the existing KDoc comment intention ("Reinitialised on totalPx change to
preserve the current fraction") but the current implementation achieves the opposite of that
intention: it re-initialises from `initialSplitFraction` (the constructor argument), not from
the current fraction.

**Fix B-2: Guard coerceIn when min > max**

In `SplitClamp.kt`, add a guard before the `coerceIn` call:

```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float {
    // When the container is too small to satisfy both minimums, pin to minFirstPx.
    // This avoids coerceIn(a, b) throwing when a > b.
    val safeMax = maxPx.coerceAtLeast(minFirstPx)
    return (currentPx + deltaPx).coerceIn(minFirstPx, safeMax)
}
```

`coerceAtLeast(minFirstPx)` means: if `maxPx` has gone below `minFirstPx`, clamp it up to
`minFirstPx`. `coerceIn(minFirstPx, minFirstPx)` is valid (returns `minFirstPx`). The visual
result is that both panes are forced to their minimums when the container is too small — no
exception, no freeze.

### Files Modified for Fix B

| File | Change |
|------|--------|
| `components/layout/AeroSplitPane.kt` | Replace `var dividerPx by remember(totalPx) { ... }` pattern with `var dividerFraction by remember { ... }` + derived `val dividerPx`. Update `onDrag` to write back to `dividerFraction`. Remove `fractionToPx` call from remember initialiser (still used in onDrag via derived). |
| `components/layout/internal/splitpane/SplitClamp.kt` | Add `val safeMax = maxPx.coerceAtLeast(minFirstPx)` guard before `coerceIn`. |

`pxToFraction` and `fractionToPx` in `SplitClamp.kt` are unchanged — they remain correct
pure helpers.

`AeroDragSplitter.kt` is unchanged — the drag modifier is not involved in the root cause.

---

## (c) AeroDateTimePicker Seconds Formatter Fix

### Where the Bug Lives

`AeroDateTimePicker.kt` line 76:
```kotlin
formatter: (LocalDateTime) -> String = { ldt ->
    "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
},
```

The format string is `HH:MM` unconditionally. `showSeconds` is a separate parameter on the
same composable but is never consulted by the default formatter. When the user commits a
datetime with seconds (e.g. 14:30:45), the trigger displays "14:30" — the `:45` is silently
dropped from display even though `combineDateTime` correctly preserved it in the `LocalDateTime`
value.

### The Fix

The default formatter lambda must close over `showSeconds` and branch on it:

```kotlin
formatter: (LocalDateTime) -> String = { ldt ->
    val timePart = if (showSeconds) {
        "%02d:%02d:%02d".format(ldt.hour, ldt.minute, ldt.second)
    } else {
        "%02d:%02d".format(ldt.hour, ldt.minute)
    }
    "${formatAeroDate(ldt.date)} $timePart"
},
```

Because the default formatter is a lambda expression in the parameter default position, it
closes over the `showSeconds` parameter of the same function — this is valid Kotlin. No new
internal function or state needed.

### File Modified

| File | Change |
|------|--------|
| `components/pickers/AeroDateTimePicker.kt` | Replace the `formatter` default lambda on line 76. One-line change to the format string (add `:SS` branch). The parameter signature, the popup logic, `combineDateTime`, and `assembleTime` are all unchanged. |

### Same Fix Must Be Applied to AeroDateTimeRangePicker Default Formatter

The new component's default formatter should be seconds-aware from the start — do not
introduce the same bug into the new file. Apply the same `if (showSeconds)` branch in its
default `formatter` parameter.

---

## Build Order

The three items are architecturally independent. Neither fix depends on the new component,
and the new component does not depend on either fix. Recommended order is fix-first for
safety (prevents shipping the new component before the existing broken behavior in
`AeroDateTimePicker` is corrected, since `AeroDateTimeRangePicker` will have the same
`showSeconds` parameter and should never have this bug).

| Step | Item | Files Changed | Rationale |
|------|------|---------------|-----------|
| 1 | Fix A — seconds formatter | `AeroDateTimePicker.kt` (1 line) | Smallest possible change; zero risk; fixes visible regression in existing component. Do first so the new component can be written with the correct pattern already established. |
| 2 | Fix B — SplitPane nested freeze | `AeroSplitPane.kt`, `SplitClamp.kt` | Two-part fix; both files must be changed together or the exception still fires (B-2) or the re-key still fires (B-1). Can be done in any order relative to Fix A — they are in different packages. |
| 3 | New — AeroDateTimeRangePicker | `AeroDateTimeRangePicker.kt` (new file) | Last because: (a) largest surface area; (b) benefits from Fix A being done first (seconds-aware formatter pattern is already established); (c) if it ships with a defect it does not break any existing component. |

### New vs Modified Files — Complete List

**Modified:**

- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt`
  — Fix A: default formatter seconds branch
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt`
  — Fix B-1: fraction-preservation pattern (replaces remember(totalPx))
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt`
  — Fix B-2: coerceIn guard

**New:**

- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt`
  — New component: AeroDateTimeRangePicker

No other files require modification. All internal primitives (`AeroCalendarGrid`, `TimeFields`,
`AeroDateRangeState`, `nextRangeState`, `combineDateTime`, `AeroCalendarPositionProvider`,
`PickerPopupContainer`, `dateIsDisabled`, `formatAeroDate`) are reused read-only.

---

## Architectural Patterns

### Pattern 1: Fraction as Stable Coordinate (SplitPane Fix)

**What:** Internal position state held as a fraction [0..1], not as px. Px is always derived
at render time from `fraction * totalPx`. The fraction does not change when the container
resizes — only the px expression changes.

**When to use:** Any layout component whose internal position must survive parent resize. Px
coordinates are viewport-relative; fractions are viewport-independent. This is the correct
mental model for "preserve position across window resize."

**Trade-offs:** One extra float multiplication per layout pass (negligible). Slightly less
intuitive to debug than a raw px value, but the KDoc comment already stated the intention to
preserve fraction — the original implementation just failed to implement it correctly.

### Pattern 2: Apply Gate for Compound Pickers

**What:** The popup does not auto-close and does not emit on partial user input. It only emits
when the user explicitly clicks Apply, and only when all required fields have been set. In
AeroDateTimePicker the gate is `pendingDate != null`. In AeroDateTimeRangePicker the gate is
`rangeState is AeroDateRangeState.Selected`.

**When to use:** Any picker whose output combines more than one independent input (date + time,
or two datetimes). Single-field pickers (`AeroDatePicker`, `AeroTimePicker`) can auto-close
because there is nothing to "combine."

**Trade-offs:** Adds Cancel/Apply UI cost. For range+time this is unavoidable — the alternative
(auto-close on second date click) would emit incorrect data before the user sets the time.

### Pattern 3: Sealed State Machine for Range Selection

**What:** `AeroDateRangeState` (Idle / SelectingEnd / Selected) + pure `nextRangeState`
transition. All click handling funnels through one function that returns the next state plus
an optional commit pair. The composable has exactly one `onRangeSelect(` call site, guarded
by commit != null.

**When to use:** Any two-step selection UX where the intermediate state (one endpoint chosen)
must be represented explicitly and the callback must fire exactly once per completed selection.
AeroDateTimeRangePicker inherits this pattern verbatim.

---

## Anti-Patterns

### Anti-Pattern 1: remember(totalPx) for Divider Px State

**What people do:** Store divider position as px and re-initialise from `initialSplitFraction`
when `totalPx` changes.
**Why it's wrong:** Any parent resize (including another SplitPane dragging) discards the
current position and resets to the constructor default. In nested panes this fires on every
drag frame.
**Do this instead:** Store divider position as a fraction. Derive px = fraction * totalPx at
render time without any `remember` key.

### Anti-Pattern 2: Auto-Close on Second Date Click in a DateTime Range Picker

**What people do:** Reuse AeroDateRangePicker's `if (commit != null) { ... expanded = false }`
pattern verbatim in AeroDateTimeRangePicker.
**Why it's wrong:** The datetime range is not complete at the moment of the second date click —
the time components have not been confirmed yet. Auto-closing emits stale or default time values.
**Do this instead:** Let date clicks update rangeState for highlight feedback only. Emit only
on explicit Apply click, guarded by `rangeState is Selected`.

### Anti-Pattern 3: Hardcoded Time Format String in Default Formatter

**What people do:** Write `"%02d:%02d".format(ldt.hour, ldt.minute)` unconditionally in the
default formatter for a picker that has a `showSeconds` parameter.
**Why it's wrong:** The committed `LocalDateTime` contains the correct second value (because
`combineDateTime` and `assembleTime` handle it), but it is silently truncated in the trigger
display. The user sees "14:30" and has no feedback that the ":45" they set was recorded.
**Do this instead:** Branch on `showSeconds` in the default formatter lambda.

---

## Integration Points — v2.0.1 Scope

### Primitives Reused Read-Only (no changes to these files)

| Primitive | Used By | Usage |
|-----------|---------|-------|
| `AeroCalendarGrid` | AeroDateTimeRangePicker | Two instances, same as AeroDateRangePicker |
| `TimeFields` | AeroDateTimeRangePicker | Two instances (start + end), same as AeroDateTimePicker |
| `AeroDateRangeState` + `nextRangeState` | AeroDateTimeRangePicker | Verbatim — no modification |
| `combineDateTime` | AeroDateTimeRangePicker | Called twice at Apply |
| `AeroCalendarPositionProvider` | AeroDateTimeRangePicker | Identical popup positioning |
| `PickerPopupContainer` | AeroDateTimeRangePicker | Identical glass surface wrapper |
| `dateIsDisabled` | AeroDateTimeRangePicker | Per-cell in both calendar instances |
| `formatAeroDate` | AeroDateTimeRangePicker | Trigger display formatter |
| `pxToFraction` + `fractionToPx` | AeroSplitPane (post-fix) | Unchanged helpers, still correct |
| `aeroDragSplitter` | AeroSplitPane (post-fix) | Unchanged — not involved in root cause |

### Showcase

No showcase changes are required for the two fixes (they are invisible behavioral corrections).
The new `AeroDateTimeRangePicker` should be added to `PickersSection.kt` — a single new demo
row alongside the existing `AeroDateRangePicker` row. This is one showcase change, not part of
the library fixes.

---

## Sources

- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` — confirmed bug location (line 76 formatter), confirmed existing pending-state pattern, confirmed combineDateTime (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` — confirmed AeroDateRangeState, nextRangeState, dual-calendar layout, popup pattern (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` — confirmed remember(totalPx) re-key pattern (line 105), confirmed maxPx computation (line 111) (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` — confirmed coerceIn(minFirstPx, maxPx) without guard (line 22) (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — confirmed not involved in root cause; release events not consumed (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/TimeFields.kt` — confirmed TimeFields signature (showSeconds, minuteStep), assembleTime behavior (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` — confirmed rangeStart/rangeEnd params, isDisabled param, onMonthChange (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/PickerPopupContainer.kt` — confirmed glass popup surface wrapper (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` — confirmed popup position provider (HIGH confidence)
- `.planning/PROJECT.md` — confirmed v2.0.1 scope, existing decisions (HIGH confidence)

---

*Architecture research for: aero-compose-ui v2.0.1 patch integration*
*Researched: 2026-06-22*
