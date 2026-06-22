# Phase 12: v2.0.1 — Seconds Fix + SplitPane Fix + AeroDateTimeRangePicker - Context

**Gathered:** 2026-06-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Patch milestone delivering three things and nothing more:
- **Fix A** — `AeroDateTimePicker` shows seconds in the trigger when `showSeconds = true`.
- **Fix B** — nested N-pane `AeroSplitPane` (3+ panes, 2+ splitters via `end`-slot nesting) drags without snap-back or crash.
- **New** — `AeroDateTimeRangePicker`: dual-calendar range + two time rows + Cancel/Apply commit gate, emitting `(LocalDateTime, LocalDateTime)`.

Plus showcase demos for all three on three themes (AeroBlue / AeroDark / Classic) and one doc-hygiene cleanup. **Zero new dependencies. No breaking changes to v2.0 public API.** Discussion below clarifies the new picker's popup UX and the showcase shape — the two bug fixes are mechanically determined (root causes confirmed from source) and carry no user latitude.

</domain>

<decisions>
## Implementation Decisions

### AeroDateTimeRangePicker — popup layout
- **Time rows go BELOW the dual calendar**, as a stacked block of two labeled rows: `Start [ HH:MM ]` then `End [ HH:MM ]`, then the Cancel/Apply row. Layout order: dual calendar → Start row → End row → buttons. Mirrors the existing `AeroDateTimePicker` structure (calendar → time → buttons), extended to two endpoints.
- Rejected: time-under-each-calendar (left/right months are a shared navigation window, NOT start/end — the association would be false and confusing). Rejected: single combined row (gets cramped with `showSeconds = true` × 2, especially on narrow `< 560dp` popups).
- **Labels are explicit text** ("Start" / "End") so the two rows are unambiguous. Exact label string ("Start" vs "Start time") is Claude's discretion — keep terse, English, matching existing picker copy.
- **Narrow mode (`< 560dp`)**: calendars stack vertically (reuse the `AeroDateRangePicker` `maxWidth < 560.dp` branch verbatim); the two time rows remain below the stacked calendars. Consistent placement regardless of width.

### AeroDateTimeRangePicker — time editability gate
- **Both time rows render unconditionally but `enabled = false` until the full date range is selected** (`rangeState is AeroDateRangeState.Selected`). This keeps popup height stable from frame 1 (matters for `AeroCalendarPositionProvider` flip logic on 768p displays — PITFALL-I) and prevents setting a time before a date exists.
- This is the research default (SUMMARY.md / PITFALLS.md PITFALL-I), confirmed by the user.
- Disabled visual treatment is Claude's discretion via the existing `enabled` parameter on `TimeFields` — greyed spinners are acceptable.

### AeroDateTimeRangePicker — trigger field display
- **Single line**, format `DD.MM.YYYY HH:MM → DD.MM.YYYY HH:MM` (or `…HH:MM:SS…` when `showSeconds = true`), separator glyph **`→`** — identical convention to `AeroDateRangePicker` (`AeroDateRangePicker.kt:126`).
- Overflow handling: **standard `AeroTextField` ellipsis** — no custom wrapping, no two-line trigger (other pickers are single-line `AeroTextField`; uniformity wins).
- Rejected: short/collapsed format (e.g. omitting duplicate year) — contradicts DTR-06 which locks the full format; user-supplied `formatter` still overrides everything.
- Partial / incomplete state (no committed range, or only a start picked): trigger shows the empty/placeholder state, same as `AeroDateRangePicker` (`displayText = ""` unless both endpoints committed).

### Narrow-case range support (user-mandated — must all work)
- **Same-month range**: supported. `nextRangeState` already commits same-month ranges (confirmed by Phase 11 fix F14); both endpoints render in the left calendar grid, the right calendar shows the next month. Intermediate-cell highlight works within a single grid.
- **Single-day range (`start date == end date`)**: supported. From `SelectingEnd`, a click on the same date yields `Selected(date, date)` because `clicked >= start` holds at equality — a valid `Selected` state, so Apply enables and the time rows turn editable. (Discoverability note — selecting one day means clicking that day twice — is acceptable, no special UI.)
- **Single-day + reversed times**: handled by the same-day ordering decision below — this is exactly DTR-04.

### AeroDateTimeRangePicker — same-day time ordering (DTR-04)
- **Silent swap at Apply.** When `start date == end date` and `startTime > endTime`, the emitted pair is reordered so `start ≤ end` by full `LocalDateTime` (e.g. user enters 15:00→08:00, component emits `(…08:00, …15:00)`). No error UI, no Apply block — consistent with the silent date-swap already in `nextRangeState`.
- Implement as a pure, unit-testable `internal fun orderDateTimeRange(...)` applied only at the Apply `onClick` (the sole emit site). Uses `LocalDateTime <= LocalDateTime` directly (Comparable in kotlinx-datetime 0.6.2 — no Instant conversion).

### Showcase shape
- **SHW-12 (seconds fix)**: keep the existing `AeroDateTimePicker` demo (`showSeconds = false`) and add a **second instance** with `showSeconds = true` beside it — both modes visible side by side, makes the fix self-evident.
- **SHW-11 (new picker)**: add an `AeroDateTimeRangePicker` row in `PickersSection` next to `AeroDateRangePicker`, with a live label rendering the emitted `(LocalDateTime, LocalDateTime)`.
- **SHW-13 (nested SplitPane)**: `LayoutSection` gets a nested 3-pane demo (2 splitters, inner pane nested in the `end` slot) where the outer/left splitter can be dragged across its range and the inner/right splitter holds position and never freezes. Exact pane proportions/content are Claude's discretion — must make FIXSP-01/02 reproducible by eye.
- All showcase demos verified on AeroBlue / AeroDark / Classic.

### Bug fixes (mechanical — locked by research, no user latitude)
- **Fix A**: replace the hardcoded `"%02d:%02d"` default formatter at `AeroDateTimePicker.kt:76`; introduce shared `internal fun formatAeroDateTime(ldt: LocalDateTime, showSeconds: Boolean): String` (conditional seconds suffix). New component inherits it (prevents re-introducing PITFALL-H). A caller's explicit `formatter` is used verbatim (FIXDT-02).
- **Fix B**: `AeroSplitPane.kt:105` — replace `remember(totalPx)` divider state with fraction-based state (`var dividerFraction by remember { mutableStateOf(...) }`, `dividerPx` derived each recompose). `SplitClamp.kt:22` — guard `clampDividerPx` with `val safeMax = maxPx.coerceAtLeast(minFirstPx)` before `coerceIn`. **Both files change in one commit.** Unit test for inverted-range `clampDividerPx` (`maxPx < minFirstPx`) written BEFORE the fix (FIXSP-04, mandatory).

### Claude's Discretion
- Disabled time-row visual (greyed spinners via `TimeFields(enabled = false)`).
- Label strings ("Start"/"End" vs "Start time"/"End time").
- Nested SplitPane demo proportions/content (SHW-13).
- Exact spacing/padding inside the new popup (follow `AeroDateTimePicker` 8dp rhythm).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope & requirements
- `.planning/PROJECT.md` — v2.0.1 milestone goal, target features, Key Decisions table (incl. stale kotlinx-datetime note to clear, SHW-14)
- `.planning/REQUIREMENTS.md` — FIXDT-01..02, FIXSP-01..04, DTR-01..08, SHW-11..14 with acceptance criteria; Out-of-Scope table (anti-features)
- `.planning/ROADMAP.md` §"Phase 12" — plan-level build order (Fix A → Fix B → New Component) and 8 success criteria

### Research (HIGH confidence, root causes confirmed from source)
- `.planning/research/SUMMARY.md` — executive summary, root causes with file/line, architecture (4 files touched), reused primitives list, build-order rationale
- `.planning/research/PITFALLS.md` — PITFALL-A..I (the verification "Looks-Done-But-Isn't" checklist for the new component lives here)
- `.planning/research/ARCHITECTURE.md` — file-change map and reuse seams
- `.planning/research/FEATURES.md` — AeroDateTimeRangePicker feature table + anti-features
- `.planning/research/STACK.md` — zero-new-deps confirmation; `api(libs.kotlinx.datetime)` already at `library/build.gradle.kts:27`

### Source — Fix A
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` §line 76 — default formatter bug; pending-state pattern keyed on `expanded` (lines 132-133); Apply gate (lines 178-188); `combineDateTime` helper (line 205)

### Source — Fix B
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` §line 105 — `remember(totalPx)` re-key (root cause 1)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` §line 22 — `coerceIn(min,max)` throw (root cause 2); target for the pre-fix unit test

### Source — New component (template + reused primitives, read-only)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` — structural template: `AeroDateRangeState` + `nextRangeState` (lines 49-78), dual-calendar layout with `< 560dp` stack branch (lines 245-255), `→` trigger format (line 126), auto-close at line 200 that must NOT be copied (PITFALL-E)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/TimeFields.kt` — `showSeconds`, `minuteStep`, `enabled`, `assembleTime`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` — `rangeStart`/`rangeEnd` highlight params, `isDisabled`, `onMonthChange`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/PickerPopupContainer.kt` — W11-02 glass popup surface
- `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` — popup position provider (PITFALL-02; W11-01 — `Popup` not `Dialog`, never `transparent`)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (all read-only, zero modification)
- `AeroDateRangeState` + `nextRangeState` (`AeroDateRangePicker.kt:49-78`): date-level range state machine, reused verbatim by the new component. Already handles same-month and single-day commits and same-direction date ordering.
- `formatAeroDate` (package-internal in `AeroDatePicker.kt`, F6): `DD.MM.YYYY` formatter used by all date pickers; `formatAeroDateTime` (Fix A) builds on the same convention.
- `combineDateTime(date, time)` (`AeroDateTimePicker.kt:205`): pure date+time merge; reused per endpoint in the new component.
- `TimeFields` + `assembleTime`: the spinner row consumed by `AeroDateTimePicker`; the new component renders two instances.
- `AeroCalendarGrid` with additive `rangeStart`/`rangeEnd` params: dual-calendar range highlight, `primary@0.15f` intermediate cells (AeroDark-readable).
- `PickerPopupContainer` + `AeroCalendarPositionProvider`: the shared W11-02 popup surface and position provider for all date pickers.

### Established Patterns (constraints to honor)
- **Apply-gate (commit-gate) picker**: day clicks and time edits only mutate pending state; the SOLE emit + close site is the Apply `onClick`. Copying `AeroDateRangePicker`'s auto-close-on-second-click (`line 200`) into the new component is PITFALL-E — explicitly forbidden.
- **Pending state keyed on `expanded`**: `remember(expanded) { ... }` so a cancelled/click-outside session never leaks into the next open (PITFALL-G). Applies to range state and both pending times.
- **`awaitPointerEventScope` manual drag loop** for SplitPane (no `detectDragGestures`) — unchanged by Fix B; only the state-keying and clamp guard change.
- **Single `:library` module**; `Popup` not `Dialog`; never `transparent = true` (W11-01).

### Integration Points
- New file: `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt` (same package as `AeroDateRangePicker` → `nextRangeState`, `combineDateTime`, `formatAeroDate` are package-internal-accessible with no extra action; confirm during Phase 3 setup per SUMMARY gaps).
- `AeroDateTimeRangePicker` parameter set (parity with siblings): `startValue`, `endValue`, `onRangeSelect`, `modifier`, `formatter`, `placeholder`, `clearable`, `onClear`, `minDate`, `maxDate`, `selectableDates`, `enabled`, `showSeconds`, `minuteStep`.
- Showcase: `PickersSection` (SHW-11 new row, SHW-12 second seconds instance), `LayoutSection` (SHW-13 nested 3-pane), all on three themes. `:showcase` already has a direct `kotlinx-datetime` dependency (Phase 11).
- Doc hygiene (SHW-14): replace the stale "Revisit on publish — kotlinx-datetime declared implementation" note in PROJECT.md Key Decisions with the factual record that `api(libs.kotlinx.datetime)` is already in place at `library/build.gradle.kts:27`.

</code_context>

<specifics>
## Specific Ideas

- **All narrow range cases are first-class requirements** (user-mandated this session): same-month ranges, single-day ranges, and single-day-with-reversed-times must all work, not just multi-month spans. These are covered by reusing `nextRangeState` verbatim + the silent same-day swap (DTR-04) — but must be explicitly tested, not assumed.
- New picker should feel like `AeroDateTimePicker` scaled to two endpoints: calendar(s) on top, labeled time rows beneath, Cancel/Apply at the bottom — same vertical rhythm and glass surface.

</specifics>

<deferred>
## Deferred Ideas

- **Hover-preview range highlight** between start and a hovered date (DTR-HOVER-01) — needs `AeroCalendarGrid` API extension; deferred to v2.x.
- **Per-endpoint `showSeconds` / `minuteStep` overrides** — global-to-both is the v2.0.1 contract; no confirmed use case.
- **Live inversion error UI** on same-day reversed times — silent swap is sufficient; no error component in scope.
- **Inline (always-visible) picker mode** — popup-only through v2.x.
- **Timezone selection** — `LocalDateTime` only, like all sibling pickers.
- **AeroDropdown popup-offset regression** (v1.0 carry-over) — separate future milestone, explicitly out of v2.0.1.

</deferred>

---

*Phase: 12-v2-0-1-seconds-fix-splitpane-fix-aerodatetimerangepicker*
*Context gathered: 2026-06-22*
