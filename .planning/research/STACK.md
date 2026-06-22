# Stack Research

**Domain:** Compose Desktop UI library — v2.0.1 milestone (2 bug fixes + AeroDateTimeRangePicker)
**Researched:** 2026-06-22
**Confidence:** HIGH

---

## Verdict: Zero new dependencies required

All three deliverables in v2.0.1 are fully implementable with the exact stack currently declared in `library/build.gradle.kts`. No version bumps, no new library coordinates, no build file changes of any kind.

---

## Current Stack (confirmed from source)

| Technology | Version | Source of truth |
|------------|---------|-----------------|
| Kotlin | 2.1.21 | `gradle/libs.versions.toml` |
| Compose Desktop | 1.7.3 | `gradle/libs.versions.toml` |
| Gradle Kotlin DSL | 8.14.3 | project root |
| JDK | 17 | `library/build.gradle.kts` `jvmToolchain(17)` |
| kotlinx-datetime | 0.6.2 | `gradle/libs.versions.toml` |
| kotlinx-coroutines-core | 1.10.2 | `gradle/libs.versions.toml` (internal only) |

---

## Question (a): Does AeroDateTimeRangePicker need any new dependency?

**No. Zero new dependencies.**

`AeroDateTimeRangePicker` is a composition of two already-built components. Every primitive it needs exists in the codebase today:

| Needed primitive | Already exists in |
|------------------|-------------------|
| Dual-calendar layout (`AeroCalendarGrid` × 2) | `AeroDateRangePicker` |
| `BoxWithConstraints` responsive stacking (portrait < 560dp) | `AeroDateRangePicker` |
| `nextRangeState` sealed state machine | `AeroDateRangePicker.kt` (internal) |
| Two-row `TimeFields` (start + end) | `TimeFields.kt` (internal, already accepts `LocalTime`) |
| `combineDateTime(date, time) → LocalDateTime` | `AeroDateTimePicker.kt` (internal) |
| Apply / Cancel commit-gate | `AeroDateTimePicker` |
| `AeroCalendarPositionProvider` popup anchor | `AeroCalendarPositionProvider.kt` |
| `PickerPopupContainer` glass surface | `PickerPopupContainer.kt` |
| `LocalDateTime` output type | `kotlinx-datetime:0.6.2` (already `api`-scoped) |

The new component is a structural merge of `AeroDateRangePicker` (dual calendar + range state machine) and `AeroDateTimePicker` (time rows + Apply gate) with `LocalDateTime` as the emit type instead of `LocalDate`. No new library is needed.

---

## Question (b): LocalDateTime comparison/ordering API in kotlinx-datetime 0.6.2

**`LocalDateTime` implements `Comparable<LocalDateTime>` in 0.6.2. The `<`, `>`, `<=`, `>=` Kotlin operators work directly. No helper function or conversion to `Instant` is needed.**

Verified from the v0.6.2 source (`core/common/src/LocalDateTime.kt`):

```kotlin
public expect class LocalDateTime : Comparable<LocalDateTime>

public override operator fun compareTo(other: LocalDateTime): Int
```

Comparison semantics: returns negative if `this` is earlier civil time, zero if equal, positive if later. Comparison is lexicographic across (year, month, day, hour, minute, second, nanosecond) — correct for ordering a (start, end) pair regardless of whether they fall on the same date or different dates.

**Recommended ordering idiom for AeroDateTimeRangePicker** (mirrors the `LocalDate` pattern already used in `nextRangeState`):

```kotlin
// AeroDateRangePicker.nextRangeState — existing idiom on LocalDate:
val (s, e) = if (clicked >= current.start) current.start to clicked else clicked to current.start

// AeroDateTimeRangePicker — identical idiom on LocalDateTime:
val (s, e) = if (clickedEnd >= pendingStart) pendingStart to clickedEnd else clickedEnd to pendingStart
```

The `>=` operator on `LocalDateTime` routes through `Comparable.compareTo`, so the semantics are identical to the existing `LocalDate` usage. The codebase already demonstrates this works; `AeroDateRangePicker` has been in production use since v2.0.

No `.atStartOfDayIn(TimeZone)` conversion or `Instant` arithmetic is needed. Civil-time `LocalDateTime` comparison is exactly correct for a picker that does not cross time-zone boundaries.

**Note on the `nextRangeState` state machine:** The existing function is `internal` and typed to `LocalDate`. `AeroDateTimeRangePicker` will need its own parallel sealed type (e.g. `AeroDateTimeRangeState`) typed to `LocalDateTime`, or a refactored generic version if sharing is preferred. This is an implementation-level decision for the phase; it is not a stack constraint.

---

## Question (c): `api` vs `implementation` for kotlinx-datetime

**Already resolved. `kotlinx-datetime` is declared `api` in the current `library/build.gradle.kts`. No action required.**

```kotlin
// library/build.gradle.kts — current state (confirmed):
api(libs.kotlinx.datetime)
```

The PROJECT.md "Key Decisions" table has a stale tech-debt flag:

> `kotlinx-datetime` declared `implementation`, not `api` — ⚠️ Revisit on publish

That note does not match the actual build file. The dependency is already `api`-scoped. This means:

- Consumers of `com.mordred:aero-compose-ui` who use `AeroDateTimePicker`, `AeroDateRangePicker`, or the new `AeroDateTimeRangePicker` get `kotlinx-datetime` on their compile classpath transitively.
- They do not need to add `kotlinx-datetime` themselves to reference `LocalDate`/`LocalDateTime`/`LocalTime` in their own callbacks and state.
- This is the correct configuration for a published library whose public API surface exposes types from another library.

**Recommendation for v2.0.1:** Clear the stale tech-debt note from PROJECT.md as part of this milestone. No build file changes needed.

---

## Bug fixes — stack impact

**Fix 1: AeroDateTimePicker default formatter ignores `showSeconds` (line 76)**

Pure logic change in `AeroDateTimePicker.kt`. The default `formatter` lambda is:

```kotlin
formatter: (LocalDateTime) -> String = { ldt ->
    "${formatAeroDate(ldt.date)} ${"%02d:%02d".format(ldt.hour, ldt.minute)}"
},
```

It hardcodes `HH:MM` and never appends seconds. The fix makes the default formatter conditional on `showSeconds`. No new API, no new types, no dependency change. The parameter is in scope as a `@Composable` parameter.

**Fix 2: AeroSplitPane nested freeze**

Pure logic change in `AeroSplitPane.kt`. Root causes per PROJECT.md:
1. `remember(totalPx)` re-keys when a parent splitter drag changes the outer pane's total size, resetting the inner divider position.
2. `coerceIn(min, max)` throws `IllegalArgumentException` when the inner pane is squeezed below `minFirst + minSecond` (i.e., `min > max`).

No new API, no new types, no dependency change.

---

## What NOT to Add

| Avoid | Why |
|-------|-----|
| kotlinx-datetime version bump (0.6.2 → 0.7.x / 0.8.x) | No API gap exists. 0.7.x introduced breaking renames (`dayOfMonth→day`, `monthNumber→month`, `Instant`/`Clock` moved). Unnecessary churn risk on a published library mid-milestone. |
| `java.time` types in picker public signatures | Breaks API parity with existing pickers; consumers would need two different date type systems side-by-side. |
| ThreeTenBP or any backport library | JDK 17 baseline + JVM-only target; no Android; `java.time` is native. |
| Separate `:datepickers` Gradle module | Explicitly deferred per PROJECT.md; single `:library` module is the constraint through v2.x. |
| `components-splitpane-desktop` | Not needed; SplitPane is already hand-rolled and the freeze is a logic bug, not a missing library. |

---

## Sources

- `C:\1A_WORK\ui_lib\library\build.gradle.kts` — confirmed `api(libs.kotlinx.datetime)`, confirmed no `implementation(libs.kotlinx.datetime)` (HIGH confidence)
- `C:\1A_WORK\ui_lib\gradle\libs.versions.toml` — confirmed `kotlinxDatetime = "0.6.2"` (HIGH confidence)
- `C:\1A_WORK\ui_lib\.planning\PROJECT.md` — Key Decisions table, Current Milestone scope, Constraints (HIGH confidence)
- `C:\1A_WORK\ui_lib\library\src\main\kotlin\com\mordred\aero\components\pickers\AeroDateTimePicker.kt` — formatter bug confirmed at line 76; `combineDateTime` utility confirmed; public signature confirmed (HIGH confidence)
- `C:\1A_WORK\ui_lib\library\src\main\kotlin\com\mordred\aero\components\pickers\AeroDateRangePicker.kt` — `nextRangeState` `LocalDate >= LocalDate` operator confirmed as existing idiom (HIGH confidence)
- `https://github.com/Kotlin/kotlinx-datetime/blob/v0.6.2/core/common/src/LocalDateTime.kt` — `LocalDateTime : Comparable<LocalDateTime>` and `compareTo` operator confirmed at v0.6.2 source level (HIGH confidence)
- `https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-local-date-time/compare-to.html` — `compareTo` semantics documented (HIGH confidence)

---

*Stack research for: aero-compose-ui v2.0.1 Picker & SplitPane Fixes*
*Researched: 2026-06-22*
