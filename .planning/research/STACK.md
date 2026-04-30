# Stack Research

**Domain:** Compose Desktop UI Library — v2.0 Stateful + Layout Components
**Researched:** 2026-04-30
**Confidence:** MEDIUM-HIGH (all library versions verified against Maven Central / GitHub releases; Compose API patterns verified against official docs and JetBrains repo)

---

## Verdict: One New Dependency Only

After checking every v2.0 component against what Compose Desktop 1.7.3 / Kotlin 2.1.21 / JDK 17 already provides, the addition is minimal:

**Add: `org.jetbrains.kotlinx:kotlinx-datetime:0.6.2`** (date/time pickers only).

Everything else — table virtualization, column resize, drag gestures, HSV color math, accordion animation, split-pane divider, sidebar state, stepper state — is hand-rolled on top of APIs already available in `compose.foundation`, `compose.animation`, `compose.ui`, and `java.time` (via JDK 17).

Do NOT add `components-splitpane-desktop`. Rationale below.

---

## Recommended Stack

### Core Technologies (unchanged from v1.x)

| Technology | Version | Purpose | Status |
|------------|---------|---------|--------|
| Kotlin | 2.1.21 | Language | Already in `libs.versions.toml` |
| Compose Multiplatform | 1.7.3 | UI framework | Already declared |
| Gradle Kotlin DSL | 8.14.3 | Build | Already in use |
| JDK | 17 | JVM target | Already in `jvmToolchain(17)` |
| kotlinx-coroutines-core | 1.10.2 | Async state | Already in `library/build.gradle.kts` |

### New Dependency: kotlinx-datetime

| Library | Version | Purpose | Needed By |
|---------|---------|---------|-----------|
| `org.jetbrains.kotlinx:kotlinx-datetime` | **0.6.2** | `LocalDate` / `LocalTime` / `LocalDateTime` value types; month arithmetic; `Clock.System.now()` for default values | `AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker` |

**Why 0.6.2, not 0.7.x:**
- 0.7.0 removed `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock` (replaced by `kotlin.time.Instant` / `kotlin.time.Clock` promoted to stdlib in Kotlin 2.1). This is a breaking API change with no consumer demand driving it in this project.
- 0.7.1 added type aliases back for migration convenience, but the `Instant`/`Clock` rename is still live. `dayOfMonth` → `day` and `monthNumber` → `month` renames in 0.7.0 are additional break surface if any caller code builds against the library source.
- 0.6.2 is the last stable release in the 0.6.x train. It is fully compatible with Kotlin 2.1.x (Kotlin stdlib compatibility policy guarantees `.kotlinx` libraries built with older compilers run on newer JVMs/runtimes unchanged). Its JVM artifact delegates directly to `java.time` under the hood.
- The alternative `0.7.1-0.6.x-compat` compat artifact was released precisely to ease migration — using it signals you are in a transitional state. Since this project has no existing kotlinx-datetime code to migrate, starting at 0.6.2 stable is cleaner.

**Confidence:** MEDIUM. The 0.6.x / 0.7.x compatibility status with Kotlin 2.1.21 is inferred from Kotlin's backward-compatibility guarantee and the library changelog; no explicit "requires Kotlin >= X" entry was found in the changelog for either branch. If a compile error appears, the fallback is `0.7.1-0.6.x-compat` which is explicitly documented as the migration bridge.

**Placement in build:**
```kotlin
// library/build.gradle.kts — add inside dependencies { }
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
```

No entry in `libs.versions.toml` is strictly required (the version is pinned inline for a single dep), but adding it is idiomatic:
```toml
# gradle/libs.versions.toml
[versions]
kotlinxDatetime = "0.6.2"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
```

---

## Compose / Foundation APIs Needed (Already Available in Compose 1.7.3)

These are not new dependencies — they are APIs in `compose.foundation`, `compose.ui`, or `compose.animation` that are already on the compile classpath. Listed here so the roadmap phase plans know exactly what to call.

### AeroDataTable

| API | Source Module | Purpose |
|-----|--------------|---------|
| `LazyColumn` + `rememberLazyListState()` | `compose.foundation` | Virtualized row rendering — only visible rows compose |
| `LazyListState.firstVisibleItemIndex` | `compose.foundation` | Drive vertical scrollbar position |
| `rememberScrollState()` + `Modifier.horizontalScroll()` | `compose.foundation` | Shared horizontal scroll state for header row + data rows |
| `VerticalScrollbar` + `HorizontalScrollbar` + `rememberScrollbarAdapter()` | `compose.desktop.common` | Native desktop scrollbar chrome |
| `Modifier.pointerInput` + `detectDragGestures` | `compose.ui` | Column resize — drag handle on header divider |
| `remember { mutableStateOf(columnWidths) }` | `compose.runtime` | Mutable column width state |
| `SubcomposeLayout` or `Layout` | `compose.ui` | Measure header cells to match data cells (if uniform column-width approach is not used) |

**Critical known issue:** `LazyColumn.stickyHeader` interacts badly with `VerticalScrollbar` in CMP — the scrollbar flickers (issues #3016, #2940 on JetBrains tracker). **Do not use `stickyHeader` for the data table header row.** Instead, render the header as a normal `Row` outside the `LazyColumn`, share the same horizontal `ScrollState`, and overlay the `LazyColumn` below it. This is the correct desktop table pattern.

### AeroTreeView

| API | Source Module | Purpose |
|-----|--------------|---------|
| `LazyColumn` | `compose.foundation` | Virtualized node list |
| `AnimatedVisibility` + `expandVertically` / `shrinkVertically` | `compose.animation` | Expand/collapse transition |
| `animateFloatAsState` | `compose.animation` | Caret rotation (0° → 90°) |
| `Modifier.padding(start = depth * indent)` | `compose.foundation.layout` | Indentation by tree depth |

### AeroDatePicker / AeroTimePicker / AeroDateTimePicker / AeroDateRangePicker

| API | Source | Purpose |
|-----|--------|---------|
| `kotlinx.datetime.LocalDate` | kotlinx-datetime 0.6.2 | Immutable date value type passed to `onDateSelected` |
| `kotlinx.datetime.LocalTime` | kotlinx-datetime 0.6.2 | Immutable time value type |
| `kotlinx.datetime.LocalDateTime` | kotlinx-datetime 0.6.2 | Combined date+time value |
| `kotlinx.datetime.Clock.System.todayIn(TimeZone.currentSystemDefault())` | kotlinx-datetime 0.6.2 | Default "today" for the picker |
| `LocalDate.plus(DateTimeUnit.MONTH, n)` | kotlinx-datetime 0.6.2 | Month navigation in calendar grid |
| `Popup` + `PopupProperties` | `compose.ui.window` | Popup container (same mechanism as AeroDropdown / AeroTooltip already in library) |
| `AnimatedVisibility` | `compose.animation` | Popup open/close fade |
| `LazyVerticalGrid` | `compose.foundation.lazy.grid` | Calendar day grid (7 columns) |

**Why not `java.time.LocalDate` directly:** `java.time` is available on JDK 17 and requires zero additional deps. However the picker's public API surface (`onDateSelected: (LocalDate) -> Unit`) would then expose a JDK platform type (`java.time.LocalDate`). kotlinx-datetime's `LocalDate` is the multiplatform-clean equivalent and it delegates to `java.time` on JVM at zero overhead. Consumers who need `java.time` can call `.toJavaLocalDate()` from the kotlinx-datetime extension. This keeps the library API consistent and Kotlin-idiomatic.

**Why not Material3 DatePicker:** Material3's `DatePicker` is documented to crash on Compose Desktop (Kotlin Slack `#compose-desktop`, confirmed 2025). It relies on internal Android-only APIs. Not viable.

### AeroColorPicker

| API | Source | Purpose |
|-----|--------|---------|
| `Color.hsv(hue, saturation, value, alpha)` | `compose.ui.graphics` (built-in) | Construct `Color` from HSV components |
| `Color.red`, `.green`, `.blue`, `.alpha` | `compose.ui.graphics` (built-in) | Decompose color to RGB for sliders |
| Custom HSV decomposition (hand-rolled ~10 lines) | Hand-rolled | Compose provides `Color.hsv()` (HSV → Color) but NOT the inverse; RGB → HSV must be hand-written using standard math formulas |
| `Canvas` + `drawRect` with `Brush.horizontalGradient` | `compose.foundation` | HSV saturation/value square gradient rendering |
| `Modifier.pointerInput` + `detectDragGestures` | `compose.ui` | Drag crosshair on HSV square |
| `BasicTextField` | `compose.foundation` | HEX input field |

**Color math:** The HSV ↔ RGB round-trip is ~20 lines of pure Kotlin math (no external library needed). The concern in STATE.md about "drift on round-trips" is real: use `Float` throughout and only convert to `Int` (0–255) at the display/output layer, not in intermediate state. No third-party color library is needed.

### AeroRangeSlider

No new APIs — composition over existing `AeroSlider` logic + `Modifier.pointerInput` for dual-thumb gesture disambiguation.

### AeroAccordion

| API | Source | Purpose |
|-----|--------|---------|
| `AnimatedVisibility` + `expandVertically` / `shrinkVertically` | `compose.animation` | Content reveal |
| `animateFloatAsState` | `compose.animation` | Chevron rotation |
| `remember { mutableStateSetOf<Int>() }` | `compose.runtime` | Multi-mode: set of open section indices |

### AeroSplitPane

| API | Source | Purpose |
|-----|--------|---------|
| `BoxWithConstraints` | `compose.foundation.layout` | Measure total available size |
| `Modifier.pointerInput` + `detectDragGestures` | `compose.ui` | Divider drag |
| `remember { mutableStateOf(fraction) }` | `compose.runtime` | Split position (0f–1f fraction) |
| `Modifier.width(...)` / `Modifier.height(...)` derived from fraction | `compose.foundation.layout` | Slot sizing |

**Why not `components-splitpane-desktop`:** The last stable release on Maven Central is **1.5.2** (September 2023). Development builds (1.7.0-dev1703) exist but are not stable releases. There is no `1.7.3` matching the project's CMP version. The component's API is thin (it is ~200 lines of Compose code internally). Hand-rolling `AeroSplitPane` avoids a dev-build dependency, gives full control over the Aero divider styling, and matches the project's existing pattern of owning all visual components. The implementation fits in ~80 lines.

### AeroSidebar

| API | Source | Purpose |
|-----|--------|---------|
| `AnimatedVisibility` + `slideInHorizontally` / `slideOutHorizontally` | `compose.animation` | Expand ↔ hidden transition |
| `animateDpAsState` | `compose.animation` | Smooth width transition expanded ↔ collapsed |
| `Modifier.width(expandedWidth)` / `collapsedWidth` | `compose.foundation.layout` | Fixed width slots (no drag-resize per OUT-OF-SCOPE decision) |
| `AeroTooltip` (existing) | library | Tooltip on collapsed icon buttons |

### AeroStepperWizard

| API | Source | Purpose |
|-----|--------|---------|
| `remember { mutableStateOf(currentStep) }` | `compose.runtime` | Step index |
| `AnimatedContent` with `slideInHorizontally` | `compose.animation` | Step content transition |
| `onValidate: () -> Boolean` | library API surface | Per-step gate; called on "Next" click synchronously (no coroutine needed for v2.0) |

---

## What NOT to Add

| Reject | Why | What to Use Instead |
|--------|-----|---------------------|
| `org.jetbrains.compose.components:components-splitpane-desktop` | Latest stable on Maven Central is 1.5.2 (Sept 2023); no 1.7.3 stable exists; dev builds are not stable; adds an unvetted dep for ~80 lines of code | Hand-roll `AeroSplitPane` with `BoxWithConstraints` + `detectDragGestures` |
| `kotlinx-datetime:0.7.0` or `0.7.1` (stable) | Breaking API changes: `Instant`/`Clock` removed, `dayOfMonth`→`day`, `monthNumber`→`month` renames; introduces more migration surface with no benefit for a new codebase | Use `0.6.2` (last stable in 0.6.x train; compatible with Kotlin 2.1.x) |
| `kotlinx-datetime:0.8.0-rc02` | Release candidate, not stable | Wait for stable or use 0.6.2 |
| `compose.materialIconsExtended` | Explicitly removed in v1.1 (shed ~36 MB classpath). No component in v2.0 requires Material icons — all glyphs use `AeroIcons.*` | `AeroIcons.*` (already in library) |
| Third-party color picker libraries (e.g. `kolor-picker`, `godaddy/compose-color-picker`, `SmartToolFactory/Compose-Color-Picker-Bundle`) | Android-only or bring heavy transitive deps; HSV ↔ RGB math is ~20 lines; visual style must be Aero-glass which no third-party component delivers | Hand-roll HSV square + `Color.hsv()` built-in |
| Third-party table/grid libraries | None exist for Compose Desktop with the Aero visual contract; `LazyColumn` virtualization is sufficient for v2.0 read-only tables | `LazyColumn` + shared `ScrollState` for header |
| `kotlinx.coroutines.flow.MutableStateFlow` for date/time state | Coroutines already on classpath but date picker state is synchronous UI state — no async needed | `remember { mutableStateOf() }` |
| `java.time.LocalDate` as the public API type for pickers | Exposes a JDK platform type; less idiomatic in KMP-adjacent library | `kotlinx.datetime.LocalDate` (delegates to `java.time` on JVM at zero cost) |

---

## Installation

```kotlin
// gradle/libs.versions.toml — add:
[versions]
kotlinxDatetime = "0.6.2"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }

// library/build.gradle.kts — add inside dependencies { }:
implementation(libs.kotlinx.datetime)
```

No changes to `showcase/build.gradle.kts` or root `build.gradle.kts` are needed. The showcase will pick up `kotlinx-datetime` transitively through `:library`.

---

## Version Compatibility

| Package | Version | Compatible With | Notes |
|---------|---------|-----------------|-------|
| `kotlinx-datetime` | 0.6.2 | Kotlin 2.1.21 / JDK 17 | MEDIUM confidence — compatibility inferred from Kotlin backward-compat policy; no explicit incompatibility found; fallback is `0.7.1-0.6.x-compat` if compile errors surface |
| All other v2.0 APIs | — | Already in Compose 1.7.3 | HIGH confidence — `LazyColumn`, `AnimatedVisibility`, `detectDragGestures`, `Color.hsv()`, `Popup`, `BoxWithConstraints` all stable in CMP 1.7.3 |
| `components-splitpane-desktop` | N/A — NOT added | — | Latest stable (1.5.2) predates CMP 1.7.3; no stable 1.7.x exists |

---

## Sources

- `github.com/Kotlin/kotlinx-datetime/releases` — version list; 0.6.2 is last stable 0.6.x; 0.7.0/0.7.1 stable with breaking changes; 0.8.0-rc02 is latest pre-release (MEDIUM confidence)
- `github.com/Kotlin/kotlinx-datetime/blob/master/CHANGELOG.md` — 0.7.0 breaking changes confirmed (`Instant`/`Clock` removal, `dayOfMonth`→`day`, `monthNumber`→`month`) (HIGH confidence)
- `repo1.maven.org/maven2/org/jetbrains/compose/components/components-splitpane-desktop/` — only 1.5.2 and 1.2.0-alpha01-dev609 present as Maven Central stable; dev builds (1.7.0-dev1703) not stable (HIGH confidence)
- `developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Color.Companion` — `Color.hsv(hue, saturation, value, alpha)` exists as built-in in `compose.ui.graphics`; no inverse function (HIGH confidence)
- `github.com/JetBrains/compose-multiplatform/issues/3016` and `#2940` — `stickyHeader` + `VerticalScrollbar` known flicker bug; do not use `stickyHeader` for table header row (HIGH confidence)
- `slack-chats.kotlinlang.org` — Material3 `DatePicker` crashes on Compose Desktop (MEDIUM confidence — community-confirmed, no official JB bug tracker link found)
- `github.com/JetBrains/compose-multiplatform/tree/master/components/SplitPane` — SplitPane component source; publishes as `org.jetbrains.compose.components:components-splitpane-desktop`; group ID confirmed from `build.gradle.kts` (HIGH confidence)

---

*Stack research for: aero-compose-ui v2.0 Stateful + Layout*
*Researched: 2026-04-30*
