# Stack Research

**Domain:** Compose Desktop UI library — v2.0.2 AeroPanelGroup (additive layout component)
**Researched:** 2026-06-22
**Confidence:** HIGH (all claims verified against in-repo source files)

---

## Verdict: NO new Gradle dependencies required

`AeroPanelGroup` + `AeroPanelSection` are built entirely from Compose APIs already declared
`api(...)` in `library/build.gradle.kts` and from internal library utilities already shipped
in v2.0.1. Nothing needs to be added to `libs.versions.toml` or either module's
`build.gradle.kts`.

---

## Confirmed Stack (in-repo versions — do not invent newer ones)

| Technology | Version | Source of truth |
|------------|---------|-----------------|
| Kotlin | 2.1.21 | `gradle/libs.versions.toml` `kotlin = "2.1.21"` |
| Compose Multiplatform | 1.7.3 | `gradle/libs.versions.toml` `composeMultiplatform = "1.7.3"` |
| Gradle Kotlin DSL | 8.14.3 | PROJECT.md Constraints section |
| JDK | 17 | `library/build.gradle.kts` `jvmToolchain(17)` |
| kotlinx-datetime | 0.6.2 | `libs.versions.toml` (irrelevant to PanelGroup — date picker only) |
| kotlinx-coroutines-core | 1.10.2 | `library/build.gradle.kts` `implementation(...)` (irrelevant to PanelGroup) |

All Compose artifacts (`compose.desktop.common`, `compose.material3`, `compose.animation`,
`compose.foundation`, `compose.runtime`, `compose.ui`) are already declared `api(...)` —
transitive on the consumer classpath, no duplication required.

---

## Exact Compose APIs Required for AeroPanelGroup

### Animation

| API | Package | Usage in PanelGroup | Precedent in library |
|-----|---------|---------------------|----------------------|
| `animateFloatAsState` | `androidx.compose.animation.core` | Animate each section's target height in px (collapse → `headerHeightPx`; expand → `lastExpandedPx`). Drives the height `Float` passed to `Modifier.height(with(density) { animatedPx.toDp() })`. | `AeroAccordion` caret rotation; `AeroSidebar` uses `animateDpAsState` for width |
| `tween(durationMillis = 200, easing = FastOutSlowInEasing)` | `androidx.compose.animation.core` | Animation spec for collapse/expand — matches `AeroSidebar` (200ms, same easing). Do NOT use `animateContentSize` — that works on natural content height, not controlled px values. | `AeroSidebar` width: `tween(200, FastOutSlowInEasing)`; `AeroAccordion` caret: `tween(160, FastOutSlowInEasing)` |
| `FastOutSlowInEasing` | `androidx.compose.animation.core` | Same easing as all existing animated layout components. | `AeroAccordion`, `AeroSidebar` |
| `graphicsLayer { rotationZ = caretRotation }` | `androidx.compose.ui` (via `Modifier.graphicsLayer`) | Caret chevron 0°→90° on expand — identical to `AeroAccordion`. | `AeroAccordion` `AccordionSectionRow` |

**Critical design constraint (from PROJECT.md spike note):** drag writes `sizePx` directly
without animation; `animateFloatAsState` only runs when `expanded` toggles. The target value
passed to `animateFloatAsState` must be derived from `lastExpandedPx` (expand) or
`headerHeightPx` (collapse), NOT from drag deltas. This keeps the two code paths cleanly
separated and prevents fighting between the animation loop and the drag callback.

### Measurement and density conversion

| API | Package | Usage | Precedent |
|-----|---------|-------|-----------|
| `BoxWithConstraints` | `androidx.compose.foundation.layout` | Read `constraints.maxHeight.toFloat()` once at composition to get `totalPx`. Same pattern as `AeroSplitPane` — not SubcomposeLayout, not per-frame height measurement. | `AeroSplitPane` — `BoxWithConstraints` outer wrapper, `constraints.maxHeight.toFloat()` |
| `LocalDensity.current` | `androidx.compose.ui.platform` | Convert `36.dp` header height to px at composition time via `with(density) { 36.dp.toPx() }`; convert section `sizePx` back to `Dp` for `Modifier.height(...)` every recompose. | `AeroSplitPane` — `with(density) { dividerPx.toDp() }` and `with(density) { minFirstPaneSize.toPx() }` |
| `rememberUpdatedState` | `androidx.compose.runtime` | Wrap `totalPx` so the drag lambda always reads the live value — mandatory pattern per FIXSP-01 fix (v2.0.1) to prevent stale-capture snap-back. | `AeroSplitPane` line 116: `val liveTotalPx by rememberUpdatedState(totalPx)` |

### Pointer input (drag)

| API | Package | Usage | Notes |
|-----|---------|-------|-------|
| `Modifier.aeroDragSplitter(orientation = Orientation.Vertical, onDrag = { delta -> ... })` | `com.mordred.aero.components.internal.drag` (internal) | The draggable grip between two adjacent expanded sections. `Orientation.Vertical` → delta reports `positionChange().y`, cursor becomes `N_RESIZE_CURSOR`. | Already used in `AeroSplitPane`. |

`aeroDragSplitter` is declared `internal` in
`library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt`.
It is accessible to all `com.mordred.aero.*` packages within `:library`, which is where
`AeroPanelGroup` will live. No visibility change needed.

### State primitives

| API | Package | Usage |
|-----|---------|-------|
| `mutableStateOf` | `androidx.compose.runtime` | Per-section `expanded: Boolean`, `sizePx: Float`, `lastExpandedPx: Float`. |
| `remember { ... }` | `androidx.compose.runtime` | Seed uncontrolled expansion state (mirrors `AeroAccordion` uncontrolled path). |
| `mutableStateListOf` | `androidx.compose.runtime` | Per-section state list inside the group (N sections, each holding `expanded` + `sizePx` + `lastExpandedPx`). Prefer over `List<MutableState<...>>` — snapshot-aware, triggers targeted recomposition on single-section change. |

### Layout

| API | Package | Usage | Notes |
|-----|---------|-------|-------|
| `Column` | `androidx.compose.foundation.layout` | Stack N section composables vertically. | Standard. |
| `Modifier.height(dp)` | `androidx.compose.foundation.layout` | Apply animated/drag-driven height to each section `Box`. Derived: `with(density) { animatedSizePx.toDp() }`. | Same approach as SplitPane first-pane fixed height. |
| `Modifier.weight(1f)` | `androidx.compose.foundation.layout` | Last visible expanded section takes remainder after all others have explicit heights — avoids a floating-point accumulation gap at the bottom. | SplitPane uses explicit px for first pane + `weight(1f)` for second. |
| `Row` | `androidx.compose.foundation.layout` | Header strip layout (chevron + title + optional actions). | Standard. |

### Visual (Aero theme)

| API | Package | Usage |
|-----|---------|-------|
| `Modifier.glassPanel(cornerRadius = 0.dp)` | `com.mordred.aero.theme` | Section header strip background — matches `AeroAccordion` header. |
| `AeroIcons.CaretRight` | `com.mordred.aero.icons.internal` | Chevron glyph in header, rotated 0°→90° via `graphicsLayer`. Vendored and used in `AeroAccordion`. |
| `AeroTheme.colors.borderDefault` | `com.mordred.aero.theme` | 1dp divider line between sections and static header-to-header joint. |
| `AeroTheme.colors.onSurface` | `com.mordred.aero.theme` | Icon and text tint in header. |
| `AeroTheme.colors.buttonHover` | `com.mordred.aero.theme` | Divider grip hover overlay (same as `SplitPaneDivider`). |

---

## Internal Utilities to Reuse (exact paths)

| Utility | Path | Reuse pattern |
|---------|------|---------------|
| `Modifier.aeroDragSplitter` | `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` | Apply to the 8dp hit-area Box between two adjacent expanded sections. Pass `Orientation.Vertical`, `onDrag = { delta -> ... }`, `onDragEnd = {}`. |
| `clampDividerPx` | `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` | Clamp per-section drag: `clampDividerPx(currentPx, delta, minSectionPx, maxPx)`. The `safeMax` guard already handles the case where squeezing pushes `maxPx < minPx` (PITFALL-B guard, already in prod). |
| `AeroAccordion` controlled/uncontrolled hybrid | `library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt` | API shape: `expandedIndices: Set<Int>? = null`, `onExpandedChange: ((Set<Int>) -> Unit)? = null`; uncontrolled internal `remember { mutableStateOf(...) }`. Both branches intentional — do not collapse to one. |
| `AeroIcons.CaretRight` | `library/src/main/kotlin/com/mordred/aero/icons/internal/CaretRight.kt` | Same chevron glyph as Accordion, same 0°→90° rotation pattern. |
| `glassPanel` modifier | `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` | Section header strip background (`cornerRadius = 0.dp` for flush full-width strips). |

`fractionToPx` / `pxToFraction` from `SplitClamp.kt` are NOT needed — PanelGroup stores
absolute `sizePx` per section and redistributes on collapse/expand, not bilateral fractions.
The fraction model in SplitPane solved a 2-pane specific problem; N-section px redistribution
differs enough that the helpers offer no simplification and would import a false analogy.

---

## New Internal File to Create

One new pure-logic internal file is expected (no Compose import, fully unit-testable):

```
library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribute.kt
```

Responsibilities:
- Compute `availableForExpanded = totalPx − Σ(headerPx for collapsed sections) − Σ(dividerThicknessPx)`
- Redistribute freed px among expanded neighbours when a section collapses
- Clamp each section to `minSectionPx`
- Normalize expanded sizes when window resizes (totalPx changes)

This follows the `SplitClamp.kt` pattern: plain JVM functions, no Compose dependency, unit-tested independently before integration (per `SplitClampTest.kt` / `AccordionToggleTest.kt` precedent).

---

## What NOT to Do

| Do not use | Why | Use instead |
|------------|-----|-------------|
| `detectDragGestures` | PITFALL-03: 18dp touchSlop silently blocks first-pixel mouse drag on Compose Desktop (JetBrains/compose-jb #343, unresolved as of CMP 1.7.3). | `Modifier.aeroDragSplitter` (wraps `awaitPointerEventScope` manual loop, no touchSlop). |
| `SubcomposeLayout` | Runs subcomposition on every frame, causes extra layout passes and jank. PROJECT.md explicitly calls this out in the PanelGroup spec. | `BoxWithConstraints` → read `constraints.maxHeight.toFloat()` once. |
| Per-frame height measurement (`onGloballyPositioned`, `Layout` with measuring children each drag event) | Same jank class as SubcomposeLayout; px state must drive layout, not layout drive state. | Compute `totalPx` from `BoxWithConstraints.constraints` once; derive heights from stored `sizePx`. |
| `animateContentSize` | Works on natural content height determined by children — incompatible with explicit px-driven heights where the outer composable controls section size. | `animateFloatAsState` on target `sizePx` per section. |
| Capturing `totalPx` as a plain `val` inside the drag lambda | PITFALL-A (same class as FIXSP-01 regression in v2.0.1): stale capture causes snap-back when window resizes or parent drag changes constraints. | `rememberUpdatedState(totalPx)` — mandatory, exact pattern from `AeroSplitPane` line 116. |
| Third-party split/panel libraries | Introduces an external dependency for a component 100% coverable with existing internal primitives; adds classpath weight; may carry incompatible drag semantics or non-Aero visual style. | Internal implementation using `aeroDragSplitter` + `clampDividerPx`. |
| `fractionToPx` / `pxToFraction` from `SplitClamp.kt` | Designed for bilateral 2-pane fraction model; does not simplify N-section absolute px redistribution. | Inline distribution logic or `PanelDistribute.kt`. |

---

## Gradle Change Summary

```kotlin
// library/build.gradle.kts  — NO CHANGES REQUIRED
// gradle/libs.versions.toml — NO CHANGES REQUIRED
// showcase/build.gradle.kts — NO CHANGES REQUIRED
```

All required Compose APIs are already on the compile classpath through the existing
`api(compose.animation)`, `api(compose.foundation)`, `api(compose.runtime)`, `api(compose.ui)`,
and `api(compose.material3)` declarations in `library/build.gradle.kts`.

---

## Sources

- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — verified `aeroDragSplitter` signature, `Orientation` usage, PITFALL-03 documentation (HIGH)
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/splitpane/SplitClamp.kt` — verified `clampDividerPx` signature and safeMax inverted-range guard (HIGH)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt` — verified `BoxWithConstraints` pattern, `rememberUpdatedState(totalPx)`, `LocalDensity` usage, `weight(1f)` second-pane pattern (HIGH)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt` — verified controlled/uncontrolled hybrid pattern, `animateFloatAsState` caret rotation, `tween(160ms, FastOutSlowInEasing)` (HIGH)
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroSidebar.kt` — verified `animateDpAsState` with `tween(200ms, FastOutSlowInEasing)` as the 200ms easing precedent (HIGH)
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — verified `glassPanel` signature (HIGH)
- `gradle/libs.versions.toml` — verified all version strings (HIGH)
- `library/build.gradle.kts` — verified all `api(...)` declarations; confirmed no relevant dependency is missing (HIGH)
- `.planning/PROJECT.md` — verified Constraints, Key Decisions table, Current Milestone spec including spike note and PITFALL cross-references (HIGH)

---

*Stack research for: aero-compose-ui v2.0.2 AeroPanelGroup*
*Researched: 2026-06-22*
