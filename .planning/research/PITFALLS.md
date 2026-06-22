# Pitfalls Research

**Domain:** Compose Desktop UI library — v2.0.1 milestone: SplitPane nested freeze fix + AeroDateTimeRangePicker new component + AeroDateTimePicker seconds-in-trigger fix
**Researched:** 2026-06-22
**Confidence:** HIGH (all claims grounded in actual source files read above)

> **Scope note:** This file covers only the three items in v2.0.1. The v2.0 pitfalls (PITFALL-01 through PITFALL-15, W11-01, W11-02) remain locked and are not re-documented here. Carry-forward rules referenced inline where a new pitfall extends a previous one.

---

## Critical Pitfalls

### PITFALL-A: `remember(totalPx)` re-keys nested SplitPane divider on every outer drag

**Components:** AeroSplitPane (nested configuration — 3+ panes, 2+ splitters)

**What goes wrong:**
`AeroSplitPane.kt` line 105 keys the divider state on `totalPx`:

```kotlin
var dividerPx by remember(totalPx) {
    mutableStateOf(fractionToPx(initialSplitFraction, totalPx))
}
```

When the outer (left) splitter is dragged, the inner (right) `AeroSplitPane` receives a new constraint from its parent `Box(Modifier.weight(1f))`. `BoxWithConstraints` re-reads `constraints.maxWidth.toFloat()` and produces a new `totalPx`. Because `remember(totalPx)` uses `totalPx` as its key, Compose invalidates the remembered state and reinitialises `dividerPx` to `fractionToPx(initialSplitFraction, totalPx)` — the inner divider snaps back to `initialSplitFraction` on every outer drag frame.

**Why it happens:**
The comment at line 103–104 says "Reinitialised on totalPx change (window resize) to preserve the current fraction." This is correct intent for a window resize (infrequent, large Δ) but wrong for being inside a dragged end-slot, where `totalPx` changes continuously on every drag event. The two cases were not distinguished.

**How to avoid:**
Store the divider as a fraction, not as an absolute px, and only use `totalPx` for rendering, not as a remember key:

```kotlin
var dividerFraction by remember { mutableStateOf(initialSplitFraction) }
val dividerPx = dividerFraction * totalPx   // derived, not remembered

val onDrag: (Float) -> Unit = { delta ->
    val minFirstPx = with(density) { minFirstPaneSize.toPx() }
    val maxPx      = totalPx - with(density) { minSecondPaneSize.toPx() }
    val rawPx      = (dividerFraction * totalPx + delta).coerceIn(minFirstPx.coerceAtMost(maxPx), maxPx.coerceAtLeast(minFirstPx))
    dividerFraction = if (totalPx > 0f) rawPx / totalPx else dividerFraction
    onSplitChange?.invoke(dividerFraction)
}
```

`dividerFraction` is never re-keyed; it updates only when the user drags. `totalPx` is used at render time to derive `dividerPx` on every recomposition — correct, because the pane has genuinely resized and the divider should honour the same fraction.

**Warning signs:**
- Dragging the outer splitter causes the inner splitter to snap back to the centre (50%) or to `initialSplitFraction`.
- The inner splitter cannot be held in position while the outer one moves.

**Phase to address:** SplitPane nested freeze fix (v2.0.1, Phase 12 or first fix phase).

**Regression risk to v2.0 API:** None. `dividerFraction` is fully internal state; public API parameters `initialSplitFraction`, `minFirstPaneSize`, `minSecondPaneSize`, and `onSplitChange` are unchanged. Single-level use is unaffected because without a parent splitter, `totalPx` changes only on window resize (the intended trigger).

---

### PITFALL-B: `coerceIn(min, max)` crash when nested pane is squeezed below `minFirst + minSecond`

**Components:** AeroSplitPane + `SplitClamp.kt:clampDividerPx`

**What goes wrong:**
`SplitClamp.kt` line 22:

```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float =
    (currentPx + deltaPx).coerceIn(minFirstPx, maxPx)
```

The caller (`AeroSplitPane.kt` lines 110–112) computes:

```kotlin
val minFirstPx = with(density) { minFirstPaneSize.toPx() }
val maxPx = totalPx - with(density) { minSecondPaneSize.toPx() }
```

When the nested pane's `totalPx` is squeezed below `minFirstPaneSize.toPx() + minSecondPaneSize.toPx()` (default 48.dp + 48.dp = 96.dp at 1× density), `maxPx` becomes less than `minFirstPx`. Kotlin's `Float.coerceIn(minimumValue, maximumValue)` throws `IllegalArgumentException: Cannot coerce value to an empty range: maximum X is less than minimum Y`. This is the immediate cause of the "right splitter freeze" — the exception propagates and the composable stops rendering.

**Why it happens:**
The clamp formula assumes `totalPx >= minFirstPx + minSecondPx`. This is true for a top-level SplitPane (the window cannot be smaller than 96dp without the user manually shrinking it below usable size). It is NOT guaranteed for a nested inner SplitPane whose `totalPx` is controlled by the outer SplitPane's drag — the outer splitter can freely push the inner pane below the combined minimum.

**How to avoid:**
Guard the clamp call so `min <= max` is always satisfied before calling `coerceIn`:

```kotlin
// In onDrag lambda (AeroSplitPane.kt):
val minFirstPx = with(density) { minFirstPaneSize.toPx() }
val minSecondPx = with(density) { minSecondPaneSize.toPx() }
val maxPx = (totalPx - minSecondPx).coerceAtLeast(minFirstPx)
```

Using `coerceAtLeast(minFirstPx)` on `maxPx` ensures `maxPx >= minFirstPx` always. When the inner pane is too narrow to honour both minimums simultaneously, the first pane's minimum takes precedence and the second pane is silently squeezed — a graceful visual degradation rather than a crash. Alternatively, clamp `clampDividerPx` itself:

```kotlin
internal fun clampDividerPx(currentPx: Float, deltaPx: Float, minFirstPx: Float, maxPx: Float): Float {
    val safeMax = maxPx.coerceAtLeast(minFirstPx)  // guard against inverted range
    return (currentPx + deltaPx).coerceIn(minFirstPx, safeMax)
}
```

Fixing in `clampDividerPx` is preferable because it is the only call site for the clamp and is already unit-tested.

**Warning signs:**
- `IllegalArgumentException: Cannot coerce value to an empty range` in the Compose error log.
- The right (inner) splitter becomes unmoveable after the outer splitter is dragged past the point where `innerTotalPx < 96.dp`.
- Application appears frozen because Compose swallows the exception per-frame and stops composing the affected subtree.

**Phase to address:** SplitPane nested freeze fix (v2.0.1). Fix both the `coerceAtLeast` guard in `clampDividerPx` and the remember-key issue (PITFALL-A) in the same change — they are both part of the same root-cause investigation.

**Regression risk to v2.0 API:** None for single-level use. For nested use, the new behaviour (inner pane gracefully squeezed) is strictly better than a crash. The `coerceAtLeast` change in `clampDividerPx` is backward-compatible because the affected code path (inverted range) previously threw.

---

### PITFALL-C: Float churn — sub-pixel `totalPx` jitter causes spurious `remember(totalPx)` invalidation even on window-stable layouts

**Components:** AeroSplitPane (any depth), especially on HiDPI displays

**What goes wrong:**
`constraints.maxWidth.toFloat()` returns a pixel value that Compose derives from the dp constraint via the current density. On HiDPI displays (1.25×, 1.5× scale), `BoxWithConstraints` can report values like `1279.5f` or `1280.0f` for the same logical size depending on the recomposition path. If `totalPx` oscillates between two float values (e.g., `623.0f` and `623.5f`) due to layout rounding in the parent row, `remember(totalPx)` is invalidated on alternating frames even though the window has not resized.

This is a secondary amplifier of PITFALL-A: even after fixing the nested-drag reset, float churn can still cause spurious resets during hover or other non-drag recompositions if `remember(totalPx)` is retained as the key.

**Why it happens:**
`Int`-based constraints (`constraints.maxWidth: Int`) are exact. `toFloat()` is lossless for integers. However, when a parent `Row` uses `Modifier.weight(1f)` for the end pane and then the inner `BoxWithConstraints` measures, the weight-based size may be a fractional dp that Compose rounds differently depending on the measurement pass. On desktop the density is not always exactly 1.0 — it follows the OS display scale.

**How to avoid:**
This pitfall is entirely eliminated by the PITFALL-A fix. Switching from `remember(totalPx)` with float key to `remember { mutableStateOf(initialSplitFraction) }` with no key removes all float-based remember invalidation. The fraction is updated intentionally in `onDrag` only.

If the `remember(totalPx)` approach is retained for any reason (e.g., for a "re-anchor on resize" feature), use an integer key: `remember(constraints.maxWidth)` instead of `remember(totalPx)`. `constraints.maxWidth` is `Int` and does not suffer float jitter.

**Warning signs:**
- Divider position resets intermittently on hover over other components (no drag involved).
- On 1.25× or 1.5× Windows display scale, the inner divider resets more frequently than on 1× displays.

**Phase to address:** SplitPane nested freeze fix (v2.0.1). Addressed implicitly by the PITFALL-A fix; called out explicitly to prevent a partial fix that replaces `remember(totalPx)` with `remember(constraints.maxWidth)` but does not switch to fraction-based state.

---

### PITFALL-D: Single-level SplitPane regression — fraction-based fix must still re-anchor on genuine window resize

**Components:** AeroSplitPane (single-level, v2.0 shipped behaviour)

**What goes wrong:**
The v2.0 comment at line 103–104 documents the intent: "Reinitialised on totalPx change (window resize) to preserve the current fraction." If the fix for PITFALL-A simply removes the `remember` key entirely, a window resize no longer re-anchors the divider to the current fraction. Instead, `dividerPx = dividerFraction * totalPx` is computed on every recomposition from the current fraction — which already achieves the desired re-anchor behaviour. However, if `dividerFraction` is `remember`ed without a key AND the component is removed from the composition and recomposed (e.g., tab switching), `dividerFraction` resets to `initialSplitFraction` because Compose disposes the remembered state on removal.

The correct behaviour: fraction persists across drags; fraction is NOT reset by `totalPx` changes; fraction IS reset to `initialSplitFraction` only on component disposal and re-entry (which is correct — the caller controls `initialSplitFraction` if they want persistence across tab switches).

**How to avoid:**
Use `remember { mutableStateOf(initialSplitFraction) }` with no key. On window resize, `dividerPx = dividerFraction * totalPx` is recomputed automatically from the stored fraction — the divider moves proportionally without any state reset. This matches the documented intent without re-keying. Write a single-level visual regression test: resize the window, verify divider stays at the same visual fraction.

**Warning signs:**
- After applying the nested-freeze fix, a single-level SplitPane at 30% resets to 50% on window resize (indicates `remember` key was removed but `dividerFraction` was not used as the render basis).
- Alternatively, a nested SplitPane continues to reset inner divider on outer drag (indicates the fraction approach was adopted but `dividerPx` is still being used as the state variable rather than derived).

**Phase to address:** SplitPane nested freeze fix (v2.0.1). Verified by adding the existing single-level showcase demo to the regression checklist.

---

### PITFALL-E: `AeroDateTimeRangePicker` must NOT auto-close on second date click — Apply gate is mandatory

**Components:** AeroDateTimeRangePicker (new)

**What goes wrong:**
`AeroDateRangePicker` (the date-only version) auto-closes on second click: `nextRangeState` returns a non-null commit and `expanded = false` is set immediately (line 200 in `AeroDateRangePicker.kt`). Copying this pattern to `AeroDateTimeRangePicker` would close the popup before the user sets start/end times, discarding the time selections entirely.

The structural difference: `AeroDateRangePicker` emits `(LocalDate, LocalDate)` — times are irrelevant, so auto-close is correct UX. `AeroDateTimeRangePicker` emits `(LocalDateTime, LocalDateTime)` — the user must set two dates AND two times before committing. Only the Apply button should trigger `onRangeSelect` and `expanded = false`.

**Why it happens:**
`AeroDateTimeRangePicker` is built by analogy with `AeroDateRangePicker`. The `nextRangeState` transition function returns a non-null commit pair on the second click, which is the natural signal to close. A developer copying the range picker pattern will add `if (commit != null) { onRangeSelect(...); expanded = false }` matching the date-only picker — incorrect for the datetime variant.

**How to avoid:**
In `AeroDateTimeRangePicker`, decouple the range-completion signal from the close-and-emit action:

```kotlin
// Day click — updates pending range state only, NEVER closes, NEVER emits
val onDayClick: (LocalDate) -> Unit = { date ->
    val (next, _) = nextRangeState(rangeState, date)  // commit pair intentionally discarded
    rangeState = next
    // do NOT set expanded = false here
    // do NOT call onRangeSelect here
}

// Apply button — SOLE emit site
AeroButton(
    text = "Apply",
    enabled = rangeState is AeroDateRangeState.Selected,
    onClick = {
        val s = rangeState
        if (s is AeroDateRangeState.Selected) {
            val startDt = LocalDateTime(s.start, pendingStartTime)
            val endDt   = LocalDateTime(s.end,   pendingEndTime)
            // ordering guard (PITFALL-F) applied here
            onRangeSelect(startDt, endDt)
            expanded = false
        }
    },
)
```

The Apply button is disabled until both dates are chosen (`rangeState is Selected`). Cancel sets `expanded = false` without emitting. This is the same commit-gate pattern used in `AeroDateTimePicker` (single picker) — see line 178–188 in `AeroDateTimePicker.kt`.

**Warning signs:**
- Popup closes immediately after clicking the second date without showing the time rows.
- `onRangeSelect` fires before the user touches any time spinners.

**Phase to address:** AeroDateTimeRangePicker implementation (v2.0.1). This is the first design decision — the commit-gate architecture must be locked before writing any composable code.

---

### PITFALL-F: Same-day range with reversed times produces chronologically inverted `LocalDateTime` pair

**Components:** AeroDateTimeRangePicker (new)

**What goes wrong:**
`nextRangeState` orders the dates: `if (clicked >= current.start) current.start to clicked else clicked to current.start`. This guarantees `startDate <= endDate`. However, it does NOT guarantee `startDateTime <= endDateTime` when `startDate == endDate`. If the user selects the same date twice and sets `startTime = 14:30` and `endTime = 09:00`, the emitted pair is `(2026-06-22T14:30, 2026-06-22T09:00)` — chronologically inverted.

**Why it happens:**
The date ordering from `nextRangeState` is correct for date-only ranges. For datetime ranges, time must be included in the ordering comparison. The Apply button assembles `LocalDateTime(s.start, pendingStartTime)` and `LocalDateTime(s.end, pendingEndTime)`, but if `s.start == s.end`, the time order is whatever the user set — not validated.

**How to avoid:**
At the Apply button's onClick, after assembling both `LocalDateTime` values, swap if needed:

```kotlin
val (startDt, endDt) = if (startDtRaw <= endDtRaw) {
    startDtRaw to endDtRaw
} else {
    endDtRaw to startDtRaw
}
onRangeSelect(startDt, endDt)
```

`LocalDateTime` implements `Comparable` in `kotlinx-datetime 0.6.2` — the `<=` operator works directly. This ensures the emitted pair is always chronologically ordered regardless of click order and time settings.

Alternatively, document a stricter UX: disable the Apply button when `startDt > endDt` and show an inline warning. This is more explicit but adds visual complexity for a rare edge case. The swap approach is simpler and matches how `nextRangeState` handles reversed date clicks.

**Warning signs:**
- Unit test: select same date twice, set `startTime = 15:00` and `endTime = 08:00`, click Apply — verify emitted `(start, end)` has `start < end`.
- Production sign: callers receiving inverted ranges and calculating negative duration.

**Phase to address:** AeroDateTimeRangePicker implementation (v2.0.1). Add the ordering guard to `combineRangeDateTime` (a pure internal function, unit-testable — analogous to `combineDateTime` in `AeroDateTimePicker.kt`).

---

### PITFALL-G: Pending start/end time state leaking across popup opens

**Components:** AeroDateTimeRangePicker (new)

**What goes wrong:**
If `pendingStartTime` and `pendingEndTime` are `remember`ed without a key, they survive across popup open/close cycles. The user: (1) opens the popup, adjusts start time to 10:00, clicks Cancel; (2) opens the popup again — the time row shows 10:00 instead of the committed value (or midnight if no range was committed). The leaked pending state makes the picker appear to have pre-selected times the user did not confirm.

`AeroDateTimePicker.kt` already solves this correctly at line 132–133:

```kotlin
var pendingDate by remember(expanded) { mutableStateOf(value?.date) }
var pendingTime by remember(expanded) { mutableStateOf(value?.time ?: LocalTime(0, 0, 0)) }
```

The `remember(expanded)` key ensures that when `expanded` flips from `false` to `true` (popup opens), the pending state is reinitialised from the committed value.

**How to avoid:**
Apply the same `remember(expanded)` pattern to both time fields in `AeroDateTimeRangePicker`:

```kotlin
var pendingStartTime by remember(expanded) {
    mutableStateOf(startValue?.time ?: LocalTime(0, 0, 0))
}
var pendingEndTime by remember(expanded) {
    mutableStateOf(endValue?.time ?: LocalTime(0, 0, 0))
}
```

And analogously for the range state and `leftMonth` (already done in `AeroDateRangePicker.kt` line 174 and 183 — carry the same keys forward).

**Warning signs:**
- Click Apply with `startTime=10:00`, then open the picker again with Cancel — the time row should show the last committed time (or 00:00 if no prior commit), not the cancelled edit.
- Time spinners show stale values from a previous partial session.

**Phase to address:** AeroDateTimeRangePicker implementation (v2.0.1). The `remember(expanded)` keying pattern is already established in the codebase — do not deviate from it.

---

### PITFALL-H: `showSeconds` not flowing to the trigger formatter — the root cause of the v2.0.1 seconds bug, and must not be replicated in the range picker

**Components:** AeroDateTimePicker (fix), AeroDateTimeRangePicker (new — must not repeat the bug)

**What goes wrong (AeroDateTimePicker bug):**
`AeroDateTimePicker.kt` line 76:

```kotlin
formatter: (LocalDateTime) -> String = { ldt ->
    "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
},
```

The default formatter hardcodes `HH:MM`. Even when `showSeconds = true`, the trigger displays `"22.06.2026 14:30"` rather than `"22.06.2026 14:30:45"`. The seconds are correctly committed (they flow through `TimeFields` → `combineDateTime` → `onValueChange`) but are never rendered.

**Root cause:** The formatter lambda is a parameter default that is evaluated at the call site — it captures nothing. The `showSeconds` parameter is in scope in the composable body but is not accessible inside a default parameter expression. The fix must either: (a) compute `displayText` inside the composable body using `showSeconds`, not via a default formatter lambda; or (b) always include seconds in the formatter and let callers who want HH:MM override it.

**Correct fix (option a — minimal surface change):**

```kotlin
val displayText = value?.let { ldt ->
    val base = "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
    if (showSeconds) "$base:${"%02d".format(ldt.second)}" else base
} ?: ""
```

This replaces the `value?.let(formatter)` call with a conditional that respects `showSeconds`, while still applying a custom `formatter` if one was supplied. The public API (`formatter` parameter) is preserved; the default behaviour is corrected.

**Same bug must not be replicated in AeroDateTimeRangePicker:**
`AeroDateTimeRangePicker` will have a `formatter: (LocalDateTime) -> String` parameter (or a `startFormatter`/`endFormatter` pair). Its default must also respect `showSeconds`. Apply the same fix pattern: compute `displayText` conditionally inside the composable body rather than relying on a default formatter lambda.

**Warning signs:**
- `AeroDateTimePicker(showSeconds = true)` — enter 14:30:45, click Apply, trigger shows "14:30" (missing seconds).
- Unit test: `combineDateTime` returns a `LocalDateTime` with `second = 45`; `formatter(ldt)` returns a string not containing "45".

**Phase to address:** AeroDateTimePicker fix (v2.0.1, Phase 12 or first fix phase). AeroDateTimeRangePicker implementation (v2.0.1) — incorporate the correct pattern from the outset.

---

## Moderate Pitfalls

### PITFALL-I: Popup height insufficient for two time rows under two calendars

**Components:** AeroDateTimeRangePicker (new)

**What goes wrong:**
`AeroDateRangePicker` uses `BoxWithConstraints` inside `PickerPopupContainer` and switches from Row (≥560dp) to Column (<560dp) layout for the two calendars. Adding two `TimeFields` rows below the calendars increases popup height by approximately 48–56dp per time row (one `AeroNumberSpinner` row plus spacing). On a 1080p display, the extra ~100dp is fine. On a 768p display, the popup may extend below the screen edge.

`AeroCalendarPositionProvider` positions below the trigger by default and flips above if height overflows. The flip logic handles this correctly — but only if `popupContentSize` is fully measured. If the two `TimeFields` rows are conditionally rendered (e.g., only after dates are picked), the popup size changes between the first measurement and the settled size, causing a layout jump (related to PITFALL-08 from v2.0).

**How to avoid:**
Always render both `TimeFields` rows unconditionally regardless of whether dates have been picked yet. The time spinners can be visually disabled (`enabled = false`) until both dates are selected, but they must be present in the layout from the first composition so popup height is stable on frame 1. Do not conditionally include or exclude the time rows based on `rangeState`.

**Warning signs:**
- Popup jumps vertically between frame 1 and frame 2 when the time rows appear after date selection.
- On 768p displays, popup bottom clips off-screen even though `AeroCalendarPositionProvider` should have flipped above.

**Phase to address:** AeroDateTimeRangePicker implementation (v2.0.1). Layout decision: render time rows unconditionally, disable spinners until dates are selected.

---

### PITFALL-J: `kotlinx-datetime` declared `implementation`, not `api` — affects `(LocalDateTime, LocalDateTime)` in AeroDateTimeRangePicker's public signature

**Components:** AeroDateTimeRangePicker (new) — publication concern

**What goes wrong:**
`.planning/PROJECT.md` Key Decisions table flags: "`kotlinx-datetime` declared `implementation`, not `api` — ⚠️ Revisit on publish — picker signatures expose `kotlinx.datetime.*`; for PUBLISHED library transitive type will leak (address at POM step)."

`AeroDateTimeRangePicker` will add one more public API parameter typed `(LocalDateTime, LocalDateTime)`. This is the same concern as `AeroDateTimePicker`, `AeroDatePicker`, and `AeroDateRangePicker` — but one more surface. The issue is not a compilation failure during library development; it is a Gradle dependency resolution failure for consumers who do not explicitly add `kotlinx-datetime` to their own build.

**How to avoid:**
This is a pre-existing flagged issue, not newly introduced by the range picker. The range picker does not change the remediation: the POM step (when publishing) must either change the Gradle declaration to `api(libs.kotlinx.datetime)` or document that consumers must add `kotlinx-datetime` as a direct dependency. Address at the same time as the existing pickers, not specially for the range picker. Do not block the v2.0.1 implementation on this — flag in the implementation phase KDoc and REQUIREMENTS.

**Phase to address:** Publication / POM step (already flagged in PROJECT.md, not v2.0.1 scope). Mentioned here to prevent "this is new with the range picker" misattribution.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Keep `remember(totalPx)` float key in SplitPane | Simple "reset on resize" | Re-keys on every outer drag frame in nested use; divider snaps to initial fraction continuously | Never for nested use; fix by switching to fraction state |
| Skip `coerceAtLeast` guard in `clampDividerPx` | Shorter code | `IllegalArgumentException` crash when nested pane squeezed below combined minimum | Never |
| Auto-close AeroDateTimeRangePicker on second date click (copy DateRangePicker pattern) | Zero new button infrastructure | Popup closes before user can set times; time selection silently lost | Never |
| Default formatter for AeroDateTimeRangePicker ignores `showSeconds` | Simple lambda default | Seconds never appear in trigger even when `showSeconds = true`; replicates the existing AeroDateTimePicker bug | Never |
| `pendingStartTime`/`pendingEndTime` without `remember(expanded)` key | Slightly less code | Cancelled time edits leak into next open session; confusing UX | Never |
| Conditionally rendering time rows based on date selection state | Visually cleaner "before dates" state | Popup size changes after dates picked, causing position jump | Never |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| `remember(totalPx: Float)` invalidating on HiDPI sub-pixel jitter | Inner divider resets during hover events with no drag (0 user interaction) | Switch to fraction-based state with no remember key (PITFALL-A fix) | On any HiDPI display (1.25×, 1.5× scale); constant |
| Two `AeroCalendarGrid` composables + two `TimeFields` rows in one `Popup` | Popup composing ~400+ day-cells + 6 spinners per recomposition | All calendar grid cells are non-lazy and stateless — no issue. `TimeFields` is a Row of 2–3 spinners — negligible | No threshold issue; composition is fast. Main concern is popup HEIGHT (PITFALL-I), not CPU |

---

## "Looks Done But Isn't" Checklist

These items are the v2.0.1-specific verification checks. They extend the v2.0 checklist in the previous PITFALLS.md (those checks remain valid for regression purposes).

- [ ] **AeroDateTimePicker seconds in trigger:** Set `showSeconds = true`, pick a time with non-zero seconds, click Apply — trigger field must show `HH:MM:SS`, not `HH:MM`.
- [ ] **AeroDateTimePicker single-level regression:** Without `showSeconds`, trigger still shows `HH:MM` (fix must not break the default case).
- [ ] **SplitPane nested — inner divider stability:** 3-pane layout (outer + inner). Drag outer splitter left/right — inner divider MUST remain at its last user-set position.
- [ ] **SplitPane nested — coerceIn crash:** Squeeze inner pane below 96dp by dragging outer splitter far right — no `IllegalArgumentException` in log; inner splitter gracefully stops.
- [ ] **SplitPane nested — release and re-drag:** After outer drag, inner divider must still be draggable to a new position (not frozen).
- [ ] **SplitPane single-level regression:** Window resize — divider must stay at same visual fraction (not reset to `initialSplitFraction`).
- [ ] **AeroDateTimeRangePicker — no auto-close on second date click:** Click first date, click second date — popup must remain open, time rows visible.
- [ ] **AeroDateTimeRangePicker — Apply gate:** Apply button must be disabled until both dates are selected. Enabled after second date click.
- [ ] **AeroDateTimeRangePicker — Cancel does not emit:** Click dates, set times, Cancel — `onRangeSelect` must not have been called.
- [ ] **AeroDateTimeRangePicker — same-day reversed times:** Select same date twice, set `startTime = 15:00`, `endTime = 08:00`, Apply — emitted `(start, end)` has `start.time < end.time`.
- [ ] **AeroDateTimeRangePicker — pending state leak:** Open picker, adjust start time spinner, Cancel. Open again — start time row must show committed value (or 00:00:00), not the cancelled edit.
- [ ] **AeroDateTimeRangePicker — showSeconds in trigger:** `showSeconds = true`, pick range with non-zero seconds, Apply — trigger shows `DD.MM.YYYY HH:MM:SS → DD.MM.YYYY HH:MM:SS`.
- [ ] **AeroDateTimeRangePicker — showSeconds default false:** `showSeconds = false` (default) — trigger shows `HH:MM`, Apply emits `LocalDateTime` with `second == 0`.
- [ ] **AeroDateTimeRangePicker — three-theme visual:** Verify popup renders correctly in AeroBlue, AeroDark, Classic (time rows use same token set as single `AeroDateTimePicker`).
- [ ] **No `transparent = true`:** Grep all three new/modified files — zero results (carry-forward W11-01).

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| PITFALL-A: remember(totalPx) reset discovered during testing | LOW | Replace `remember(totalPx) { mutableStateOf(fractionToPx(...)) }` with `remember { mutableStateOf(initialSplitFraction) }`; derive `dividerPx` inline. One surgical change in `AeroSplitPane.kt`. |
| PITFALL-B: coerceIn crash discovered in nested layout | LOW | Add `.coerceAtLeast(minFirstPx)` to `maxPx` computation in `clampDividerPx` or in the `onDrag` lambda. One-line fix. |
| PITFALL-C: float churn discovered on HiDPI after PITFALL-A partial fix | LOW | Replace any remaining `remember(totalPx)` with `remember(constraints.maxWidth)` (Int key). Covered entirely by PITFALL-A's fraction-based fix if fully applied. |
| PITFALL-D: single-level regression after nested fix | LOW | Verify `dividerPx = dividerFraction * totalPx` is computed on every recomposition (not cached). If divider jumps on window resize, the render derivation is missing. |
| PITFALL-E: auto-close replicated in range picker | MEDIUM | Decouple `nextRangeState` commit from `expanded = false`; add Apply/Cancel buttons. UI structure change but no API change. |
| PITFALL-F: reversed datetime discovered post-ship | LOW | Add `if (startDt > endDt) swap` before emit in Apply onClick. Zero API change. |
| PITFALL-G: state leak discovered in manual testing | LOW | Add `remember(expanded)` keys to `pendingStartTime` and `pendingEndTime`. |
| PITFALL-H: seconds missing in trigger (AeroDateTimePicker fix) | LOW | Replace `value?.let(formatter)` with conditional inline string in composable body. |
| PITFALL-H: seconds missing in range trigger (range picker) | LOW | Same inline conditional pattern from the outset — zero recovery cost if built correctly. |
| PITFALL-I: popup height jump on 768p | LOW | Make time rows unconditional; use `enabled = false` until dates selected. Layout-only change. |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| PITFALL-A: remember(totalPx) nested reset | SplitPane fix phase | 3-pane showcase: drag outer, inner divider stays put |
| PITFALL-B: coerceIn crash | SplitPane fix phase | Squeeze inner pane below 96dp — no exception in log |
| PITFALL-C: float churn | SplitPane fix phase (eliminated by PITFALL-A fix) | HiDPI manual test: hover events must not reset inner divider |
| PITFALL-D: single-level regression | SplitPane fix phase — regression test | Window resize: divider fraction preserved |
| PITFALL-E: auto-close on second date click | AeroDateTimeRangePicker design (first decision) | Second date click — popup remains open |
| PITFALL-F: same-day reversed times | AeroDateTimeRangePicker implementation (Apply onClick) | Unit test: `combineRangeDateTime` with same-date reversed times returns ordered pair |
| PITFALL-G: pending time state leak | AeroDateTimeRangePicker implementation | Cancel + reopen: time spinners show committed or midnight values |
| PITFALL-H: showSeconds trigger bug (DateTimePicker fix) | AeroDateTimePicker fix phase | Trigger shows HH:MM:SS when showSeconds=true |
| PITFALL-H: showSeconds in range trigger (range picker) | AeroDateTimeRangePicker implementation (displayText calculation) | Trigger shows HH:MM:SS for both endpoints when showSeconds=true |
| PITFALL-I: popup height instability | AeroDateTimeRangePicker implementation (layout structure) | Open picker on 768p display — popup does not jump |
| PITFALL-J: kotlinx-datetime api/implementation | POM / publication phase (pre-existing, not v2.0.1) | Consumer Gradle build does not fail without explicit kotlinx-datetime dep |

---

## Sources

- `AeroSplitPane.kt` (read 2026-06-22) — line 105 confirms `remember(totalPx)` float key; lines 110–112 confirm `maxPx = totalPx - minSecondPaneSize.toPx()` without coerceAtLeast guard
- `SplitClamp.kt` (read 2026-06-22) — line 22 confirms `coerceIn(minFirstPx, maxPx)` with no range-validity guard; Kotlin stdlib `Float.coerceIn` throws `IllegalArgumentException` when min > max (documented in stdlib KDoc)
- `AeroDateTimePicker.kt` (read 2026-06-22) — line 76 confirms default formatter hardcodes `%02d:%02d` (HH:MM only); lines 132–133 confirm `remember(expanded)` pending-state pattern; lines 178–188 confirm Apply-gate single-emit pattern
- `AeroDateRangePicker.kt` (read 2026-06-22) — lines 174, 183 confirm `remember(expanded)` for rangeState and leftMonth; line 200 confirms auto-close on second date click (`expanded = false` in `if (commit != null)` block); `nextRangeState` pure function at lines 71–78 orders by date only
- `TimeFields.kt` (read 2026-06-22) — confirms `showSeconds` controls spinner visibility but not the formatter in the parent picker
- `PickerPopupContainer.kt` (read 2026-06-22) — confirms two-background glass pattern; no height cap; safe to add time rows vertically
- `.planning/PROJECT.md` (read 2026-06-22) — v2.0.1 root-cause description confirms SplitPane nested freeze via `remember(totalPx)` re-key and `coerceIn(min>max)` exception; `kotlinx-datetime` implementation/api flag
- Kotlin stdlib KDoc: `Float.coerceIn(minimumValue, maximumValue)` — "Throws IllegalArgumentException if minimumValue is greater than maximumValue"
- Carry-forward pitfalls from v2.0: PITFALL-06 (onRangeSelect exactly once — honoured by PITFALL-E Apply gate), PITFALL-02 (AeroCalendarPositionProvider — reused unchanged), W11-01 (no transparent=true — checklist item above)

---
*Pitfalls research for: aero-compose-ui v2.0.1 SplitPane nested freeze + AeroDateTimeRangePicker + seconds fix*
*Researched: 2026-06-22*
