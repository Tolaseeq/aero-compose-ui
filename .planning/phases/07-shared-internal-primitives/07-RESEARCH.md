# Phase 7: Shared Internal Primitives - Research

**Researched:** 2026-04-30
**Domain:** Compose Desktop internal primitives — drag (touchSlop-defused), calendar grid, popup positioning, HSV color math, Canvas-based color selection, step indicator
**Confidence:** HIGH

## User Constraints (from CONTEXT.md)

### Locked Decisions

**Date type for `AeroCalendarGrid`** — `kotlinx-datetime:0.6.2` is added to `:library` in Phase 7 plan-01 (moved forward from Phase 8). First-compile validation is a Phase 7 plan-01 acceptance criterion; fallback `0.7.1-0.6.x-compat` is decided here, not deferred.
- Signature: `internal fun AeroCalendarGrid(displayMonth: LocalDate, selected: LocalDate?, onDateSelected: (LocalDate) -> Unit, onMonthChange: (LocalDate) -> Unit, modifier: Modifier = Modifier, isDisabled: (LocalDate) -> Boolean = { false })`
- Phase 8 pickers pass `LocalDate` straight through — zero boundary conversion.
- Month arithmetic via `kotlinx.datetime.plus(1, DateTimeUnit.MONTH)` / `minus(1, DateTimeUnit.MONTH)`.
- 3-scenario unit test: current month, Dec→Jan boundary, leap-year Feb 2024 vs Feb 2023. Lives in `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt`.

**Internal package layout — hybrid (locked):**
- `library/src/main/kotlin/com/mordred/aero/components/internal/` (NEW cross-cutting bucket):
  - `drag/AeroDragSplitter.kt`
  - `popup/AeroCalendarPositionProvider.kt`
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/` (NEW package):
  - `calendar/AeroCalendarGrid.kt`
  - `color/AeroColorMath.kt`
  - `color/AeroHsvColorSquare.kt`
  - `color/AeroHueSlider.kt`
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/` (NEW package):
  - `stepper/AeroStepIndicator.kt`
- `components/datatable/internal/` is NOT created in Phase 7 — Phase 9 territory.

**`Modifier.aeroDragSplitter` API shape (locked):**
```kotlin
internal fun Modifier.aeroDragSplitter(
    orientation: Orientation, // Horizontal or Vertical
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,
): Modifier
```
- `onDrag` receives `Float` (1D delta along orientation axis), NOT `Offset` — both consumers (SplitPane, DataTable column-resize) operate in 1D.
- The Modifier owns ONLY: `awaitPointerEventScope` + manual loop (PITFALL-03), cursor change on hover, `enabled` gate. Modifier does NOT own: visual line, hit-area thickness, color, shape (consumers wrap manually).
- `detectDragGestures` is BANNED for all Canvas drag in v2.0.

**`AeroColorMath` shape:**
- HSV is single source of truth (PITFALL-15). `Color.hsv(...)` from `compose.ui.graphics` is the forward conversion. Inverse (`Color → HSV`) is hand-rolled.
- API surface — Claude's discretion (top-level functions or `internal object AeroColorMath { fun ... }` namespace are both fine; the round-trip test is the contract):
  - `fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float>` (or `FloatArray(3)`)
  - `fun hexToRgb(hex: String): Triple<Int, Int, Int>?` returns null on parse failure
  - `fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String`
  - All values in `[0f, 1f]` for hue/sat/val/alpha; `[0, 255]` for r/g/b ints — implementer locks the convention in plan-01.
- Mandatory unit test: `hsv(0f, 1f, 1f)` → `rgb` → `hsv` returns hue within `0.001f` tolerance.

**`AeroHsvColorSquare` + `AeroHueSlider` (locked):**
- Canvas-based, drag via `Modifier.pointerInput { awaitPointerEventScope { ... } }`.
- First mouse-down sets the cursor immediately (no slop wait).
- HSV square: 256×256 logical Canvas; horizontal axis = saturation, vertical = value. Background uses `drawRect(brush = Brush.horizontalGradient(...))` then a `Brush.verticalGradient(black overlay)`.
- Hue slider: vertical strip ~20–24dp wide × 256dp tall with 7-stop gradient (red → yellow → green → cyan → blue → magenta → red); 2dp horizontal indicator line.
- Both are `internal` composables; receive `colors: AeroColorScheme = AeroTheme.colors` for indicator strokes.
- No glass styling on these primitives — they live inside `AeroColorPicker`'s glass panel container.

**`AeroStepIndicator` (locked):**
- Horizontal-only.
- Visual states: Current (filled circle in `colors.primary`; step number in `colors.onPrimary`); Completed (`colors.primary` at `0.6f` alpha; `Icon(AeroIcons.Check)` at 12dp); Upcoming (outlined circle, `colors.borderDefault` 1dp stroke; step number in `colors.labelText`).
- Connecting lines: `colors.borderDefault` for upcoming-side, `colors.primary` for completed-side.
- API:
  ```kotlin
  internal fun AeroStepIndicator(
      currentStep: Int,
      totalSteps: Int,
      modifier: Modifier = Modifier,
      onStepClick: ((Int) -> Unit)? = null,
  )
  ```

**`AeroCalendarPositionProvider` (locked, per PITFALL-02 + PITFALL-08):**
1. Default: left-aligned to `anchorBounds.left`, vertically below the anchor.
2. If `anchorBounds.left + popupContentSize.width > windowSize.width`: right-align to `anchorBounds.right`.
3. If popup exceeds window height below the anchor: flip to above (Top side).
4. **Width overflow does NOT trigger Top/Bottom flip** (locked — fixes existing `AeroPopupPositionProvider` bug for wide calendars).
5. First-frame guard: `popupContentSize == IntSize.Zero` → return `IntOffset.Zero` (off-screen at top-left, invisible on frame 1). Do NOT use the `>= windowSize` sentinel from `AeroPopupPositionProvider`.
6. `PopupProperties(focusable = true, dismissOnClickOutside = true)`.

**Sign-off approach (locked):**
- Unit tests (mandatory in CI): `AeroColorMathTest.kt` + `AeroCalendarGridTest.kt`.
- Temporary `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` (deleted in Phase 11) — eyes-on confirmation across all six primitives in one spot.
- Touchslop spike (per STATE.md): Phase 7 plan-01 includes a 1-minute drag test confirming whether issue #343 is fixed in CMP 1.7.3. Ship `awaitPointerEventScope` regardless.

**Carry-forward rules (NOT re-asked):**
- `awaitPointerEventScope` + manual loop is the ONLY drag pattern for Canvas drag on Compose Desktop (PITFALL-03).
- HSV is single source of truth (PITFALL-15).
- `Popup(...)` for all overlays — NEVER `Dialog(undecorated=true, transparent=true)` (W11-01).
- `Aero` prefix everywhere; PascalCase Phosphor verbatim for AeroIcons.
- Three themes (AeroBlue / AeroDark / Classic) supported by every visual primitive.
- `:library` uses `compose.desktop.common`; `explicitApi()` enforced — every primitive must be `internal`.

### Claude's Discretion

- Plan granularity: 2–3 plans recommended (e.g. plan-01: dep bump + spike + ColorMath + CalendarGrid + tests; plan-02: AeroDragSplitter + AeroCalendarPositionProvider + HsvSquare/HueSlider + StepIndicator + scratch section). Single plan also acceptable.
- `AeroColorMath` API surface (top-level functions vs `internal object` vs facade).
- Exact cursor type for `Modifier.aeroDragSplitter` hover (`PointerIcon.Hand` vs explicit AWT `Cursor`).
- HSV square exact pixel dimensions (256×256 vs 240×240 vs configurable).
- `AeroStepIndicator` exact dot diameter, line stroke, and inter-step spacing (within established `AeroBadge`/`AeroChip` Phase 2 visual scale).

### Deferred Ideas (OUT OF SCOPE)

- Vertical orientation for `AeroStepIndicator`.
- Public API for any Phase 7 primitive.
- `AeroEyedropperButton` for `AeroColorPicker`.
- A reusable `Modifier.aeroDragHandle(orientation, hitAreaPx, ...)` that bundles drag + invisible hit-area.
- Compose UI screenshot tests / Paparazzi infra.
- `AeroPopupPositionProvider` rewrite to consolidate with `AeroCalendarPositionProvider`.
- Adopting `kotlinx-datetime:0.7.x` — Phase 7 pins `0.6.2`.
- Fixing v1.0 `AeroDropdown` popup-offset regression.

## Phase Requirements

Phase 7 is an enabling phase — it owns zero public requirements. Downstream consumers depend on these primitives:

| Downstream ID | Phase | Description | Phase 7 Primitive Enabling It |
|---------------|-------|-------------|-------------------------------|
| PICK-01 | 8 | `AeroDatePicker` popup calendar | `AeroCalendarGrid` + `AeroCalendarPositionProvider` |
| PICK-02 | 8 | `AeroDateRangePicker` dual-month popup (>560dp) | `AeroCalendarGrid` + `AeroCalendarPositionProvider` (the wide-popup case) |
| PICK-03 | 8 | `AeroTimePicker` (no calendar) | None directly — independent of Phase 7 |
| PICK-04 | 8 | `AeroDateTimePicker` (calendar + time) | `AeroCalendarGrid` + `AeroCalendarPositionProvider` |
| PICK-05 | 8 | `AeroColorPicker` HSV square + hue + RGB + HEX, mutually synced | `AeroColorMath` + `AeroHsvColorSquare` + `AeroHueSlider` |
| PICK-06 | 8 | `AeroColorPicker` swatches | (uses Phase 8 own swatch UI; depends only on `AeroColorMath` for color parsing) |
| PICK-07 | 8 | `AeroColorPicker` alpha | `AeroColorMath` (HEX expanded to `#RRGGBBAA`) |
| PICK-08 | 8 | `AeroRangeSlider` dual-thumb drag | NOT directly served by Phase 7 — RangeSlider has its own drag (Canvas thumbs); however the established `awaitPointerEventScope` template from `AeroDragSplitter` is the reference pattern Phase 8 RangeSlider drag adopts |
| DATA-04 | 9 | `AeroDataTable` column drag-resize | `Modifier.aeroDragSplitter(orientation = Horizontal, ...)` |
| LAYO-03 | 10 | `AeroSplitPane` dual panes with draggable splitter | `Modifier.aeroDragSplitter(orientation = ...)` |
| LAYO-04 | 10 | `AeroSplitPane` 8–12dp invisible hit-area + cursor change | `Modifier.aeroDragSplitter` provides drag + cursor; consumer wraps with hit-area |
| LAYO-08 | 10 | `AeroStepperWizard` step indicator (current/completed/upcoming) | `AeroStepIndicator` |
| LAYO-09 | 10 | `AeroStepperWizard` step content + Back/Next buttons | (uses `AeroStepIndicator` for the indicator row only; rest is Phase 10 territory) |

## Summary

Phase 7 is purely additive enabling work. The library is on a stable Kotlin 2.1.21 + CMP 1.7.3 + JDK 17 foundation; no version bumps. One new dependency lands here (`kotlinx-datetime:0.6.2`, moved forward from Phase 8) so `AeroCalendarGrid` can use real `LocalDate` types in the leap-year unit test. All other primitives are hand-rolled using Compose Desktop primitives already on the classpath (`androidx.compose.foundation.Canvas`, `androidx.compose.ui.input.pointer.awaitPointerEventScope`, `androidx.compose.ui.graphics.Color.hsv`, `androidx.compose.ui.window.PopupPositionProvider`, `androidx.compose.material3.Icon`).

The dominant risk is **silent-behavior bugs** at the primitive level that would propagate downstream undetected:
1. `detectDragGestures` is the wrong API for desktop Canvas drag (touchSlop=18dp swallows every mouse event); the canonical `awaitPointerEventScope` pattern must be established here once and reused everywhere.
2. The existing `AeroPopupPositionProvider` width-clamps to anchor and flips Top/Bottom on width overflow — both wrong for wide calendars; a parallel provider is required, not a refactor of the existing one.
3. `Color.hsv(...)` uses degrees [0..360] for hue with a `requirePrecondition` runtime check — implementers expecting [0..1] will throw at first invocation. This is the single most likely surprise in `AeroColorMath`.
4. Visual-language consistency across three themes (AeroBlue / AeroDark / Classic) for the step indicator and HSV square indicators is set here; getting it wrong creates rework in Phase 10 and Phase 11.

**Primary recommendation:** Build `AeroDragSplitter` and `AeroColorMath` first (highest pitfall-mitigation value, straightforward to test in isolation). Then `AeroCalendarPositionProvider` and `AeroCalendarGrid` (calendar-related, both feed Phase 8 datepickers). Finally `AeroHsvColorSquare`/`AeroHueSlider` and `AeroStepIndicator` (visual primitives, sign-off via scratch section). Three commits map naturally to two plans; planner picks the exact split.

## Standard Stack

### Core (already on classpath — no new deps)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.foundation` | CMP 1.7.3 | `Canvas`, `pointerInput`, `awaitPointerEventScope`, `Modifier` | Existing Phase 1+ dependency; zero churn |
| `androidx.compose.ui` | CMP 1.7.3 | `PopupPositionProvider`, `IntRect`/`IntSize`/`IntOffset`, `Color.hsv`, `Brush`, `pointerHoverIcon`, `PointerIcon` | Existing Phase 1+ dependency |
| `androidx.compose.material3` | CMP 1.7.3 | `Icon` (for `AeroIcons.Check` / `CaretLeft` / `CaretRight` rendering inside `AeroStepIndicator` and `AeroCalendarGrid`) | Established v1.1 rule: `Icon()` direct, no wrapper, explicit tint |
| Kotlin stdlib | 2.1.21 | `kotlin.math` (`floor`, `min`, `max`, `abs`) for HSV/RGB conversion | Already present |
| `java.awt.Cursor` (JDK) | JDK 17 | `Cursor.E_RESIZE_CURSOR` / `N_RESIZE_CURSOR` for `aeroDragSplitter` hover (consistent with existing `AeroResizeHandles`) | JDK shipped; `:library` already uses it in `ResizeHandles.kt` |

### NEW dependency (this phase only)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `org.jetbrains.kotlinx:kotlinx-datetime` | **0.6.2** | `LocalDate`, `LocalTime`, `LocalDateTime`, `Month`, `DateTimeUnit.MONTH`, `plus`/`minus` for calendar arithmetic | Last stable release in 0.6.x line before the 0.7.0 breaking renames. Stable with Kotlin 2.1.21 per Kotlin's backward-compat policy. **Pinned by user decision; do not upgrade in this phase.** |

**Version verification (verified 2026-04-30):** kotlinx-datetime stable releases are 0.6.2 (Feb 2024) and 0.7.0 / 0.7.1 (Jun–Jul 2024); 0.8.0-rc01/02 are pre-release (Apr 2025). 0.6.2 is the last stable release in the 0.6.x line and is what user has locked. The fallback if Kotlin 2.1.21 compatibility fails is documented as `0.7.1-0.6.x-compat` per STACK.md, but no failure is expected; Kotlin's backward-compat policy keeps 0.6.x compiling under 2.1.21.

### Supporting (existing project assets — read-only references)
| Library / File | Purpose | When to Use |
|----------------|---------|-------------|
| `theme/AeroTheme.kt` (`AeroTheme.colors`, `AeroTheme.typography`) | Read color/typography tokens from inside `internal` composables | Every visual primitive |
| `theme/AeroColorScheme.kt` | 23 token reference (`primary`, `onPrimary`, `borderDefault`, `borderSelected`, `labelText`, `surface`, `panelBackground`, `glassBorder`, `buttonHover`) | StepIndicator state colors; HSV square indicator strokes |
| `theme/GlassModifiers.kt` (`Modifier.glassPanel(cornerRadius = 8.dp)`) | Calendar popup container styling (Phase 8 will use; Phase 7 confirms position provider plays nicely) | Read-only in Phase 7 |
| `icons/AeroIcons.kt` + `icons/internal/Check.kt`, `CaretLeft.kt`, `CaretRight.kt` | StepIndicator completed checkmark; CalendarGrid prev/next month buttons | Verified present in `library/src/main/kotlin/com/mordred/aero/icons/internal/` |
| `components/popup/AeroPopupPositionProvider.kt` | Design reference (intentional divergence — new provider does NOT width-lock and does NOT flip Top/Bottom on width overflow) | Pattern reference only |
| `components/popup/AeroDropdownPopup.kt` | Two-layer-background W11-02 shadow workaround (Phase 8 reuses; Phase 7 confirms position provider works alongside it) | Reference; Phase 7 doesn't modify |
| `components/range/AeroSlider.kt` | Visual reference for `AeroHueSlider`. Does NOT use `awaitPointerEventScope` (wraps Material3 Slider directly) — Phase 7's HueSlider establishes the new pattern from scratch | Visual cue only |
| `components/navigation/ResizeHandles.kt` | Existing precedent for `pointerHoverIcon(PointerIcon(Cursor.E_RESIZE_CURSOR))` pattern | Cursor pattern reference for `aeroDragSplitter`. NOTE: ResizeHandles uses `detectDragGestures` (acceptable for window-edge resize from edges; UNACCEPTABLE for in-content Canvas drag). The new `aeroDragSplitter` deliberately replaces this for the in-content drag domain |

### Test infrastructure (already wired — zero Gradle work)
| Library | Version | Purpose |
|---------|---------|---------|
| `kotlin-test` | 2.1.21 | `@Test`, `assertEquals`, `assertTrue`, `assertNotNull` |
| JUnit 5 (`junit-jupiter`) | 5.10.0 | Platform; `tasks.test { useJUnitPlatform() }` already in `library/build.gradle.kts` |
| `kotlinx-coroutines-test` | 1.10.2 | `runTest` if any suspend tests appear |
| `kotlin("reflect")` | 2.1.21 | Reflective compile-smoke tests (existing pattern from `AeroDividerTest.kt`) |

### Alternatives Considered & Rejected
| Instead of | Could Use | Why Rejected |
|------------|-----------|--------------|
| Hand-rolled HSV→RGB inverse | Bundled converter (`androidx.core.graphics.ColorUtils`) | Not on `:library` classpath; requires `androidx.core` add. ~20 lines of pure math is cheaper than a new dep. |
| `Color.hsv(...)` factory | Hand-rolled HSV→RGB forward conversion | Already on classpath; passes `requirePrecondition` checks; correct sRGB color space. Use it for forward. |
| `detectDragGestures` for splitter/HSV drag | `awaitPointerEventScope` manual loop | PITFALL-03: touchSlop=18dp on Compose Desktop swallows all mouse-distance dragging. CONFIRMED in `JetBrains/compose-jb` issue #343. Even if the upstream slop is reduced in CMP 1.7.x, the manual loop is the safe path. |
| `compose.foundation.gestures.awaitDragOrCancellation` | Custom `awaitPointerEvent` loop | `awaitDrag*` family also enforces touchSlop internally on CMP Desktop. Manual loop is the only fully reliable path. |
| `LazyVerticalGrid(GridCells.Fixed(7))` for the calendar grid | Plain `Column` of `Row(7 cells)` | The calendar is a fixed 6×7 (or 5×7) layout — lazy grid overhead is unwarranted; fixed layout measures stable on frame 1 (relevant for PITFALL-08 first-frame guard). |
| `Material3 DatePicker` | Hand-rolled `AeroCalendarGrid` | Material3's DatePicker pulls Android-only internals and crashes on Compose Desktop (community-confirmed). Locked exclusion. |
| Composable wrapping (e.g. `AeroDragSplitter(content: @Composable () -> Unit)`) | `Modifier.aeroDragSplitter(...)` | Locked decision: SplitPane and DataTable column-resize have very different visual treatments and hit-area shapes; a content-slot Composable would grow arms or force one consumer to fight the API. Modifier keeps the contract minimal. |
| Top-level public functions for `AeroColorMath` | `internal` top-level / `internal object AeroColorMath` | Both acceptable per locked rules; only the round-trip drift contract is locked. Implementer chooses. |

### Installation (only this one new dep)
**`gradle/libs.versions.toml`:**
```toml
[versions]
kotlinxDatetime = "0.6.2"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
```

**`library/build.gradle.kts`** — add inside the existing `dependencies { ... }` block:
```kotlin
implementation(libs.kotlinx.datetime)
```

## Architecture Patterns

### Recommended Project Structure (Phase 7 deliverables)
```
library/src/main/kotlin/com/mordred/aero/components/
├── internal/                           # NEW (Phase 7) — cross-cutting bucket
│   ├── drag/
│   │   └── AeroDragSplitter.kt          # Modifier extension (NOT a Composable)
│   └── popup/
│       └── AeroCalendarPositionProvider.kt
├── pickers/                            # NEW package directory (Phase 7 creates)
│   └── internal/
│       ├── calendar/
│       │   └── AeroCalendarGrid.kt
│       └── color/
│           ├── AeroColorMath.kt         # pure-function utility (NO Composables)
│           ├── AeroHsvColorSquare.kt    # internal Composable, Canvas + pointerInput
│           └── AeroHueSlider.kt         # internal Composable, Canvas + pointerInput
└── layout/                             # NEW package directory (Phase 7 creates)
    └── internal/
        └── stepper/
            └── AeroStepIndicator.kt

library/src/test/kotlin/com/mordred/aero/components/
├── pickers/internal/
│   ├── calendar/AeroCalendarGridTest.kt
│   └── color/AeroColorMathTest.kt

showcase/src/main/kotlin/com/mordred/showcase/sections/
└── Phase7ScratchSection.kt              # TEMPORARY — deleted Phase 11
```

### Pattern 1: `awaitPointerEventScope` + manual loop for Canvas drag (PITFALL-03 mitigation)

**What:** Replace `detectDragGestures` (and the entire `awaitDrag*` family) with a `pointerInput` block that opens an `awaitPointerEventScope`, awaits the first pointer-down, and processes every subsequent pointer event manually until pressed becomes false. No touchSlop check, no idle delay — drag fires on the first 1px movement.

**When to use:** For `AeroDragSplitter` (Phase 7 main deliverable), `AeroHsvColorSquare` (Phase 7), `AeroHueSlider` (Phase 7), and downstream `AeroRangeSlider` thumb drag (Phase 8) and `AeroDataTable` column resize (Phase 9). It is THE pattern for any Canvas-based drag interaction in v2.0.

**Code skeleton — `Modifier.aeroDragSplitter`:**
```kotlin
// library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
package com.mordred.aero.components.internal.drag

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import java.awt.Cursor

internal fun Modifier.aeroDragSplitter(
    orientation: Orientation,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,
): Modifier = composed {
    val cursor = remember(orientation) {
        when (orientation) {
            Orientation.Horizontal -> PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
            Orientation.Vertical   -> PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))
        }
    }
    if (!enabled) return@composed this
    this
        .pointerHoverIcon(cursor)
        .pointerInput(orientation, enabled) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var prev = down.position
                    // Fire onDrag(0f) is NOT necessary — we wait for movement.
                    // First mouse-move event after down delivers the first delta.
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            onDragEnd()
                            break
                        }
                        val cur = change.position
                        val delta = when (orientation) {
                            Orientation.Horizontal -> cur.x - prev.x
                            Orientation.Vertical   -> cur.y - prev.y
                        }
                        if (delta != 0f) {
                            onDrag(delta)
                            change.consume()
                        }
                        prev = cur
                    }
                }
            }
        }
}
```

**Notes on the skeleton:**
- `composed { ... }` is needed because `remember` and `pointerHoverIcon` are Composable-context features (cursor is computed once per orientation).
- `requireUnconsumed = false` on `awaitFirstDown` lets the splitter intercept mouse-down even if a parent already saw the event — important for nested SplitPanes (Phase 10's N-pane via nesting).
- `change.consume()` after we apply the delta prevents parent gesture detectors from also responding (e.g. a SplitPane inside a scrollable container).
- The outer `while (true)` allows multiple drag sequences in the lifetime of the modifier (one per mouse-down/up cycle).
- `enabled = false` early-returns the original `Modifier` so no pointer input is attached at all (consumer can disable when at min/max bounds).

**For `AeroHsvColorSquare`/`AeroHueSlider`** the same skeleton applies, but instead of `(deltaPx: Float) -> Unit` it's `(position: Offset) -> Unit` — these primitives need absolute positions inside the Canvas, not deltas. Mouse-down position should also fire `onPositionChange` immediately so a click-without-drag still updates the color (canonical color-picker UX).

### Pattern 2: `PopupPositionProvider` with first-frame guard (PITFALL-02 + PITFALL-08)

**What:** Implement the `androidx.compose.ui.window.PopupPositionProvider` interface for popups whose content is wider/taller than the anchor. Position the popup left-aligned by default, right-align on horizontal overflow only, and flip Top/Bottom only on vertical overflow. First-frame guard returns `IntOffset.Zero` when `popupContentSize == IntSize.Zero` to avoid the position-jump flash.

**When to use:** For all 4 date/time pickers in Phase 8 (and any future v2.x component whose popup content is wider than its anchor — e.g. a shortcut palette).

**Code skeleton — `AeroCalendarPositionProvider`:**
```kotlin
// library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt
package com.mordred.aero.components.internal.popup

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

internal class AeroCalendarPositionProvider(
    private val gap: Int = 4,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // First-frame guard (PITFALL-08): unmeasured popup → off-screen.
        if (popupContentSize == IntSize.Zero) return IntOffset.Zero

        // Horizontal: prefer left-aligned to anchor.left; right-align if it would overflow.
        val xLeft = anchorBounds.left
        val xRight = anchorBounds.right - popupContentSize.width
        val x = if (xLeft + popupContentSize.width <= windowSize.width) {
            xLeft
        } else {
            xRight.coerceAtLeast(0)
        }

        // Vertical: prefer below; flip above if below would overflow.
        val yBelow = anchorBounds.bottom + gap
        val yAbove = anchorBounds.top - popupContentSize.height - gap
        val y = if (yBelow + popupContentSize.height <= windowSize.height) {
            yBelow
        } else {
            yAbove.coerceAtLeast(0)
        }

        return IntOffset(x, y)
    }
}
```

**Notes:**
- Width overflow does NOT trigger Top/Bottom flip — only horizontal re-anchoring (`xRight`). This is the locked correction over the existing `AeroPopupPositionProvider` whose `overflows()` indiscriminately flips on any axis.
- The first-frame guard uses `popupContentSize == IntSize.Zero` (struct equality), NOT `>= windowSize`. Wide calendars (~560dp) often pass the `>= windowSize` test on a 1280dp window, missing the unmeasured signal.
- No `clamp` to non-negative beyond `coerceAtLeast(0)` on the fallback — the popup may legitimately overlap the anchor in extreme corner cases (acceptable for a calendar; user can dismiss and re-trigger).

### Pattern 3: HSV-as-single-source-of-truth for color state (PITFALL-15 mitigation)

**What:** Internal state is always `(h, s, v, alpha)` floats. RGB and HEX are derived views — when an RGB slider moves, convert RGB→HSV and store the HSV result. When the user types a HEX value and commits, parse to RGB, convert to HSV, store HSV. Never keep both HSV and RGB in state simultaneously.

**When to use:** Inside `AeroColorPicker` (Phase 8). The math primitives in `AeroColorMath` (Phase 7) are the building blocks.

**Code skeleton — `AeroColorMath` (top-level functions; choose `internal object` if preferred):**
```kotlin
// library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt
package com.mordred.aero.components.pickers.internal.color

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * RGB → HSV conversion.
 * Inputs r/g/b are in [0f, 1f]. Output triple is (h in [0f, 360f], s in [0f, 1f], v in [0f, 1f]).
 *
 * NOTE on hue range: the convention here is DEGREES [0, 360] to match [androidx.compose.ui.graphics.Color.hsv]
 * which requirePreconditions hue in [0, 360]. If implementer prefers normalized [0, 1], divide by 360 before
 * passing to `Color.hsv()` — but be consistent across the whole codebase. CONTEXT.md proposes [0, 1] for "hue/sat/val/alpha";
 * this requires a `* 360f` multiplication at the Color.hsv boundary. Implementer locks the convention in plan-01.
 */
internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val cMax = max(r, max(g, b))
    val cMin = min(r, min(g, b))
    val delta = cMax - cMin

    val h = when {
        delta == 0f       -> 0f
        cMax == r         -> 60f * (((g - b) / delta) % 6f)
        cMax == g         -> 60f * (((b - r) / delta) + 2f)
        else              -> 60f * (((r - g) / delta) + 4f)
    }
    val hPositive = if (h < 0f) h + 360f else h

    val s = if (cMax == 0f) 0f else delta / cMax
    val v = cMax
    return Triple(hPositive, s, v)
}

/**
 * HEX → RGB. Accepts "#RGB", "#RRGGBB", "#RRGGBBAA" with or without leading '#'.
 * Returns Triple(r, g, b) in [0, 255]. Returns null on parse failure (invalid characters or wrong length).
 *
 * NOTE: the alpha component, if present, is silently dropped here — caller asks for r/g/b only.
 * Add `hexToRgba(hex: String): IntArray?` if alpha is needed (Phase 8 ColorPicker enableAlpha=true).
 */
internal fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
    val cleaned = hex.removePrefix("#").trim()
    return when (cleaned.length) {
        3 -> {
            val r = cleaned[0].digitToIntOrNull(16) ?: return null
            val g = cleaned[1].digitToIntOrNull(16) ?: return null
            val b = cleaned[2].digitToIntOrNull(16) ?: return null
            Triple(r * 17, g * 17, b * 17)  // expand "F" → 0xFF
        }
        6, 8 -> {
            val r = cleaned.substring(0, 2).toIntOrNull(16) ?: return null
            val g = cleaned.substring(2, 4).toIntOrNull(16) ?: return null
            val b = cleaned.substring(4, 6).toIntOrNull(16) ?: return null
            Triple(r, g, b)
        }
        else -> null
    }
}

/** RGB ints (0..255) → uppercase HEX without '#'. If alpha != null, format as RRGGBBAA. */
internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String {
    val rgb = "%02X%02X%02X".format(r, g, b)
    return if (alpha == null) rgb else "$rgb${"%02X".format(alpha)}"
}
```

**Round-trip drift contract — must pass:**
```kotlin
// library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt
@Test
fun pureRedRoundTripPreservesHueWithinTolerance() {
    // Pure red: hue=0, sat=1, val=1
    val red = Color.hsv(0f, 1f, 1f)
    val r = red.red
    val g = red.green
    val b = red.blue
    val (hue, _, _) = rgbToHsv(r, g, b)
    assertTrue(abs(hue - 0f) < 0.001f, "hue drift on pure red round-trip; got $hue")
}
```

### Pattern 4: Canvas-based primitive composable with theme-token reads

**What:** Internal Composables that draw their own visuals via `Canvas` (or `Modifier.drawBehind`). They read `AeroTheme.colors` inside the composable body (matches existing pattern in `AeroBadge`, `AeroSlider`). They are `internal` (not `public`) — `explicitApi()` permits this only with the explicit `internal` modifier.

**Code skeleton — `AeroHueSlider`:**
```kotlin
// library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt
package com.mordred.aero.components.pickers.internal.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mordred.aero.theme.AeroTheme

@Composable
internal fun AeroHueSlider(
    hue: Float,             // [0f, 360f] (or [0f, 1f] — locked in plan-01)
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }
    Canvas(
        modifier = modifier
            .size(width = 24.dp, height = 256.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Fire on first down — color picker UX expects click-to-set.
                        onHueChange(yToHue(down.position.y, size.height.toFloat()))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            onHueChange(yToHue(change.position.y, size.height.toFloat()))
                            change.consume()
                        }
                    }
                }
            }
    ) {
        drawRect(brush = gradient)
        // Indicator: 2dp horizontal line at current hue position.
        val y = (hue / 360f) * size.height
        drawLine(
            color = colors.borderSelected,
            start = Offset(0f, y),
            end   = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

private fun yToHue(y: Float, height: Float): Float =
    ((y / height) * 360f).coerceIn(0f, 360f)
```

### Anti-Patterns to Avoid

- **`detectDragGestures` for Canvas drag:** silently fails on Compose Desktop due to `touchSlop=18dp` (PITFALL-03). Use `awaitPointerEventScope` manual loop. Acceptable usage: window-edge resize (large drags from edge bands, e.g. existing `ResizeHandles.kt`).
- **Reusing `AeroDropdownPopup` for date/time pickers:** locks popup width to anchor width; calendar (320dp) on a 240dp trigger field would clip. Use raw `Popup(popupPositionProvider = AeroCalendarPositionProvider(...))`.
- **Storing both HSV and RGB in `AeroColorPicker` state:** round-trip drift after a few user interactions. HSV is single source of truth; RGB is derived view at render time only.
- **Per-section `var expanded by remember { ... }` in accordion children** (NOT Phase 7 territory but the pattern carries forward to all stateful primitives): coordinated multi-element state must live at the parent. Phase 7's `AeroStepIndicator` follows this — `currentStep` and `totalSteps` come from the wizard parent, not from per-step state.
- **Reading `AeroTheme.colors` in default-argument position:** Composable context is required, but default-arg expressions don't execute in Composable scope. Use the `Color.Unspecified` sentinel pattern (see existing `AeroBadge.kt` lines 33–37) or read inside the body.
- **`Modifier.shadow` on popup content with `transparent=false` host window** (W11-02): renders with hard edges. Use `border + glassBorder + two-layer background` pattern (existing `AeroDropdownPopup` lines 122–125).
- **`Dialog(undecorated=true, transparent=true)` for any v2.0 overlay:** Win11 EXCEPTION_ACCESS_VIOLATION (W11-01). Use `Popup(...)` always.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Forward HSV → RGB conversion | Hand-rolled HSV→RGB sector math | `androidx.compose.ui.graphics.Color.hsv(hue, sat, val, alpha)` | Already on classpath; correct sRGB color space; `requirePrecondition` catches range violations. Hand-rolling repeats 30+ lines that exist for free. |
| Calendar arithmetic (month-add, leap-year detection) | Manual `if (month == 12) year++ else month++` | `kotlinx.datetime.LocalDate.plus(1, DateTimeUnit.MONTH)` | Handles leap years, varying month lengths, and date validity. The whole reason `kotlinx-datetime:0.6.2` is the only new dep. |
| Popup positioning logic with first-frame guard | Custom `Layout` with absolute coordinates | `androidx.compose.ui.window.Popup(popupPositionProvider = ...)` | The standard CMP popup machinery handles AWT child window creation, dismissal, focus, and click-outside. Phase 7 only writes the `PopupPositionProvider` implementation. |
| Mouse cursor change on hover | Custom AWT layer or `Modifier.onPointerEvent(Enter/Exit)` | `Modifier.pointerHoverIcon(PointerIcon(java.awt.Cursor(...)))` | Native cursor swap; existing pattern in `ResizeHandles.kt`; works correctly with undecorated windows. |
| LazyColumn item disposal-safe state | Per-item `remember` | `SnapshotStateMap<Key, State>` at parent (see PITFALL-05 prevention) | Per-item `remember` resets when the item scrolls out of viewport. Not Phase 7's deliverable, but the pattern carries forward to TreeView (Phase 9). |
| Day-of-week localization | Custom locale lookup | `kotlinx.datetime.DayOfWeek` + `java.time.format.DateTimeFormatter` (JDK 17 on the classpath) | The user has not requested i18n; defaults are acceptable. If localization is added in v2.x, JDK's existing `DateTimeFormatter` is the path. Phase 7 uses English short labels (Mo/Tu/We/Th/Fr/Sa/Su). |
| Row-virtualized step indicator (for >100 steps) | LazyRow inside `AeroStepIndicator` | Plain `Row` — locked horizontal-only, expected step count 3–10 | Wizards with >10 steps are a UX smell; horizontal step indicator > 8 dots is unreadable. No virtualization needed for in-scope use cases. |

**Key insight:** Phase 7 is "primitives over libraries" — every primitive is hand-rolled because each is too small to be worth a dep, but each builds on Compose Desktop's standard interfaces (`PopupPositionProvider`, `Modifier`, `Canvas`, `pointerInput`). The exception is `kotlinx-datetime` — calendar arithmetic is too error-prone to hand-roll.

## Common Pitfalls

### Pitfall 1: `Color.hsv()` requires hue in [0, 360], NOT [0, 1] (HIGH likelihood, LOW recovery cost)
**What goes wrong:** CONTEXT.md states "All values in `[0f, 1f]` for hue/sat/val/alpha", but `androidx.compose.ui.graphics.Color.hsv(...)` enforces `hue in 0f..360f` via `requirePrecondition`. Calling `Color.hsv(0.5f, 1f, 1f)` (intending mid-spectrum cyan) throws `IllegalArgumentException: HSV (0.5, 1.0, 1.0) must be in range (0..360, 0..1, 0..1)`.
**Why it happens:** Two conventions coexist in color libraries — HSL/HSV in Material Android Core uses [0, 1] for normalized hue; CSS HSL and `Color.hsv` in Compose use [0, 360] degrees. Implementers default to whichever they read most recently.
**How to avoid:** Lock the convention in `AeroColorMath` plan-01:
- **Recommended:** internal `[0f, 360f]` for hue (matches `Color.hsv` directly; no boundary multiplication; single source of truth across all color primitives). Update CONTEXT.md note to reflect.
- **Alternative:** internal `[0f, 1f]` — must multiply by `360f` at every `Color.hsv()` call site; adds boundary-conversion bugs.
**Warning signs:** Unit test `Color.hsv(0.5f, 1f, 1f)` throws at runtime instead of returning a Color value.

### Pitfall 2: `change.consume()` placement in the drag loop (MEDIUM likelihood, MEDIUM recovery cost)
**What goes wrong:** In the `awaitPointerEventScope` loop, calling `change.consume()` BEFORE checking `change.pressed` consumes the up event, so siblings or parents never see "pointer released". This causes downstream gesture detectors (e.g. a parent SplitPane outside an inner SplitPane) to remain in dragging state forever.
**Why it happens:** Copy-paste from PITFALLS.md template that consumes universally.
**How to avoid:** Only consume when `change.pressed && delta != 0f` (we're actively handling drag). Don't consume the up event — let parents see the release.
**Warning signs:** Showcase Phase7ScratchSection: drag the splitter inside a nested SplitPane; release; outer SplitPane stays "stuck" in drag state on next move.

### Pitfall 3: First-frame popup flash on wide calendars (PITFALL-08)
**What goes wrong:** The existing `AeroPopupPositionProvider` uses `popupContentSize >= windowSize` as the unmeasured guard. On a 1280dp window with a 560dp `AeroDateRangePicker` popup, that condition is false on frame 1 (popup is 0×0 then 560×400) — neither hits the guard. Result: popup renders at frame-1 coordinates derived from `popupContentSize=(0,0)` then jumps on frame 2.
**Why it happens:** The `>= windowSize` guard was sized for narrow dropdowns (max 320dp). Wide calendars sneak past it.
**How to avoid:** In `AeroCalendarPositionProvider`, use `popupContentSize == IntSize.Zero` (struct equality). On unmeasured, return `IntOffset.Zero` (top-left, off-screen if popup is wider than window-left = 0; otherwise visible at top-left, which is acceptable for one frame and barely noticeable). Verified visually in Phase 8 plan-01.
**Warning signs:** Visual checkpoint: trigger AeroDateRangePicker; calendar appears at top-left for one frame, then snaps to anchor.

### Pitfall 4: `composed { ... }` recreating cursor on every recomposition (LOW likelihood, LOW perf cost)
**What goes wrong:** If the cursor is computed inline via `PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))` without `remember`, every recomposition allocates a new AWT Cursor. Acceptable in functional terms (`PointerIcon` equality is structural) but adds AWT object churn.
**Why it happens:** `composed { ... }` is meant exactly for `remember`-bearing modifiers; inlining without `remember` defeats the purpose.
**How to avoid:** `remember(orientation) { PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) }` (cached per orientation; key is the orientation parameter).
**Warning signs:** Profiler shows excessive `Cursor` allocations during hover.

### Pitfall 5: `kotlinx-datetime` Month-arithmetic edge cases (low likelihood, fixed by library)
**What goes wrong:** Naive month-add with `if (month == 12) year++ else month++` doesn't handle Feb 29 → Mar 29 vs Mar 30 (when starting from Jan 31, "month + 1" is undefined). Also, `LocalDate(year, 13, 1)` throws `IllegalArgumentException`.
**Why it happens:** Hand-rolled month math is the canonical "looks easy, isn't" trap.
**How to avoid:** Use `displayMonth.plus(1, DateTimeUnit.MONTH)` directly. `kotlinx-datetime` clamps to the last day of the target month for date-clipping cases. The leap-year unit test (Feb 2024 → 29 days; Feb 2023 → 28 days) verifies this.
**Warning signs:** Unit test fails: "Feb 29 + 1 month produced March 29 (expected: clamped to month-end logic)".

### Pitfall 6: AeroDark contrast on `AeroStepIndicator` upcoming dots (PITFALL-09 family)
**What goes wrong:** `colors.borderDefault` in AeroDark is `Color(0x40FFFFFF)` — a faint white at 25% alpha. On the `Color(0xFF0A0A1A)` AeroDark background, this renders as ~0xFF383846 effective tone — visible but low contrast. An "upcoming" dot drawn as a 16dp 1dp-stroked outline circle can read as missing entirely if the user's monitor brightness is low.
**Why it happens:** Token alpha was set for borders (relatively faint by design); tested against text-bearing surfaces, not standalone outline shapes.
**How to avoid:** Two paths — both acceptable:
1. Use `colors.borderDefault.copy(alpha = (colors.borderDefault.alpha * 1.5f).coerceAtMost(1f))` for upcoming-dot strokes only (boost alpha by 50% for this single-purpose use).
2. Use `colors.labelText` (which is `Color(0xFFAAAAAA)` in AeroDark — fully opaque) as the upcoming-dot stroke color. Slightly off-spec but readable.
The scratch section's three-theme switch surfaces this immediately.
**Warning signs:** Visual checkpoint in AeroDark: "upcoming" dots are nearly invisible; user has to squint to find them.

### Pitfall 7: `internal` package directory creation in Kotlin (LOW likelihood, gotcha-ish)
**What goes wrong:** Creating a Kotlin file in a non-existent directory (e.g. `library/src/main/kotlin/com/mordred/aero/components/internal/popup/`) requires the directory to exist on disk; Gradle won't auto-create it. Also: `internal` is a Kotlin visibility modifier, NOT a folder convention — but folder conventions still matter for IDE grouping. Test source mirror is also not auto-created.
**Why it happens:** Phase 7 creates THREE new package directories (`components/internal/`, `components/pickers/internal/`, `components/layout/internal/`). Plan-01 must explicitly create them.
**How to avoid:** Plan-01 acceptance criteria: "All seven new files exist; all parent directories exist; `./gradlew :library:compileKotlin` passes; `./gradlew :library:test` finds and runs both new test files (verified by test-class-found assertion in CI output)."

## Code Examples

Verified patterns from official sources and the existing `:library` codebase.

### `awaitPointerEventScope` minimal drag loop
```kotlin
// Source: PITFALL-03 prevention template (.planning/research/PITFALLS.md lines 74–97)
// Verified pattern; applies to AeroDragSplitter, AeroHsvColorSquare, AeroHueSlider.
Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            // First-pixel response: fire onDown immediately, no slop wait.
            var prev = down.position
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    onDragEnd()
                    break
                }
                val cur = change.position
                if (cur != prev) {
                    onDrag(cur - prev)
                    change.consume()  // only consume when we actually handled the move
                }
                prev = cur
            }
        }
    }
}
```

### `Color.hsv(...)` API surface (verified)
```kotlin
// Source: androidx-main compose/ui/ui-graphics/.../Color.kt (verified 2026-04-30)
public fun hsv(
    hue: Float,                    // REQUIRED: 0f..360f (degrees)
    saturation: Float,             // REQUIRED: 0f..1f
    value: Float,                  // REQUIRED: 0f..1f
    alpha: Float = 1f,             // 0f..1f
    colorSpace: Rgb = ColorSpaces.Srgb,
): Color
// requirePrecondition(hue in 0f..360f && saturation in 0f..1f && value in 0f..1f)
```

### Custom `PopupPositionProvider` shape (verified against existing `AeroPopupPositionProvider`)
```kotlin
// Source: existing library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt
public class AeroPopupPositionProvider(...) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset { ... }
}
```
The new `AeroCalendarPositionProvider` (Phase 7 deliverable) implements the same interface; constructor and contract differ per locked decisions above.

### Existing test pattern for compile-smoke (carry-forward)
```kotlin
// Source: existing library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt
@Test
fun aeroDividerCompileSmoke() {
    val cls = Class.forName("com.mordred.aero.components.containers.AeroDividerKt")
    assertNotNull(cls)
}
```
Phase 7 unit tests follow this only for "module-loadable" smoke tests on the Composable primitives (`AeroCalendarGridKt`, `AeroHsvColorSquareKt`, etc.) where actual rendering would require Compose UI test harness; combined with the CONTEXT-locked behavioral tests (round-trip, calendar 3-scenario), this gives meaningful coverage without UI-test infra.

### Existing visual-token-resolution pattern (reuse for `AeroStepIndicator`, color primitives)
```kotlin
// Source: existing library/src/main/kotlin/com/mordred/aero/components/list/AeroBadge.kt (lines 30–37)
@Composable
public fun AeroBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val resolvedBg = if (color == Color.Unspecified) AeroTheme.colors.primary else color
    val resolvedFg = if (contentColor == Color.Unspecified) AeroTheme.colors.onPrimary else contentColor
    // ...
}
```
Phase 7 internal Composables don't expose color parameters (per CONTEXT decision: "colors pulled from `AeroTheme.colors` inside the composable, not parameterized"), but the inside-body pattern is the same.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `detectDragGestures` for any drag | `awaitPointerEventScope` + manual loop on Compose Desktop | CMP issue #343 (Feb 2021), unresolved as of CMP 1.7.3 | Locked v2.0 rule |
| Single `AeroPopupPositionProvider` for all popup types | `AeroPopupPositionProvider` (anchor-width-locked) + `AeroCalendarPositionProvider` (anchor-width-independent) | Phase 7 (this milestone) | Adds the second provider; existing one stays unchanged |
| Material3 `DatePicker` integration | Hand-rolled `AeroCalendarGrid` + `AeroCalendarPositionProvider` | v2.0 milestone scoping (community-confirmed CMP Desktop crashes in M3 DatePicker) | Locked exclusion in PROJECT.md |
| Per-component custom drag implementations | Single `Modifier.aeroDragSplitter(...)` + `awaitPointerEventScope` template | Phase 7 (this milestone) | Reduces drift between SplitPane / column-resize / RangeSlider / HSV drag |
| `AeroScrollArea` wrapping any list | Raw `LazyListState + AeroScrollBar` for virtualized lists | v2.0 (PITFALL-01 establishment) | Not Phase 7's concern, but the rule is locked |

**Deprecated/outdated (not used in Phase 7):**
- `androidx.compose.foundation.gestures.awaitDragOrCancellation`: also enforces touchSlop on CMP Desktop; no advantage over `detectDragGestures` for this domain.
- `Modifier.draggable(state)`: state-machine drag wrapper; doesn't expose enough control for the cursor + 1D-delta + early-return contract `aeroDragSplitter` needs.
- Material3 `DatePicker` / `DateRangePicker`: Android-only internals.
- `LazyVerticalGrid` for the 7-column calendar grid: overhead unwarranted; fixed `Column { Row { 7 cells } * 6 }` measures stable on frame 1 (helps PITFALL-08).

## Open Questions

1. **`AeroColorMath` hue convention — degrees [0, 360] vs normalized [0, 1]**
   - What we know: `Color.hsv()` requires hue in `[0f..360f]` (verified). CONTEXT.md proposes "All values in `[0f, 1f]` for hue/sat/val/alpha".
   - What's unclear: Which convention better serves the consumer (`AeroColorPicker` Phase 8). The picker's HSV-state-of-truth design implies tight coupling to whatever convention `AeroColorMath` picks.
   - Recommendation: lock degrees `[0f, 360f]` in plan-01. Reasons: matches `Color.hsv()` directly (no `* 360f` multiplications at every call site → fewer bugs); standard CSS/HTML/Photoshop convention; HSV-square `x = saturation` and `y = 1 - value` are independent of hue's range. Update the line in CONTEXT.md "All values in [0f, 1f]" to "Saturation/Value/Alpha in [0f, 1f]; Hue in [0f, 360f]" via plan-01 ADR.

2. **Touchslop spike outcome — does CMP 1.7.3 still need the manual loop?**
   - What we know: Issue #343 (Feb 2021) is the original report; reports of partial fixes in newer CMP versions exist (search results indicate "touch slop was significantly decreased in a subsequent Android Review change") but no canonical "fixed in CMP 1.x.x" announcement on the issue itself.
   - What's unclear: Whether CMP 1.7.3 specifically reduces the touchSlop low enough that mouse drags register on first or second event.
   - Recommendation: run the 1-minute spike in plan-01 (CONTEXT-locked). The spike outcome is documentation only — we ship `awaitPointerEventScope` regardless. If the spike shows `detectDragGestures` works in CMP 1.7.3, document in `.planning/STATE.md` "Pending Todos" as resolved; we still ship the manual loop because it's the single source of truth for v2.0 drag behavior.

3. **`AeroStepIndicator` vertical-drop handling on tight wizards (3 vs 8 steps in same width)**
   - What we know: Locked horizontal-only with no vertical fallback. `AeroStepperWizard` Phase 10 owns the wizard layout.
   - What's unclear: Whether 8 steps in a 600dp showcase column produce readable dot+number+gap, or if dot diameter/font reduces below readable.
   - Recommendation: implementer picks dot diameter (suggest 24dp, matching `AeroBadge` height range) and `Arrangement.SpaceBetween` for inter-dot gaps. The scratch section's "4 steps" demo is in scope; "8 steps" is Phase 10 / 11 territory if reachable.

4. **`Phase7ScratchSection` placement in `ShowcaseApp.kt`**
   - What we know: locked at "end of existing section list (after v1.x sections; before any v2.0 sections that don't exist yet)".
   - What's unclear: Specific imports and the exact insertion line.
   - Recommendation: ScratchSection imports `Phase7ScratchSection` from `com.mordred.showcase.sections.Phase7ScratchSection`; add `Phase7ScratchSection()` after the last existing call (e.g. after `RangeSection()` or `OverlaysSection()` — confirm via reading current `ShowcaseApp.kt` body in plan execution). Plan-01/plan-02 picks the exact location; Phase 11 cleanup deletes the line and the import.

## Validation Architecture

> Note: nyquist_validation is enabled (`workflow.nyquist_validation: true` in `.planning/config.json`).

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (`junit-jupiter` 5.10.0) + `kotlin-test` (Kotlin 2.1.21) |
| Config file | `library/build.gradle.kts:28-30` (`tasks.test { useJUnitPlatform() }`) |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.pickers.internal.color.AeroColorMathTest"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

Phase 7 owns 5 success criteria from ROADMAP §Phase 7 (no public REQ IDs, since this is an enabling phase). Each maps to a verification approach:

| ROADMAP Criterion | Behavior | Test Type | Automated Command | File Exists? |
|------------------|----------|-----------|-------------------|-------------|
| #1: `AeroCalendarGrid` renders + 3-scenario test (current month, boundary, leap year) | Calendar grid produces correct day-cell count for any `LocalDate` | unit (logic-extracting) + smoke (compile-load) | `./gradlew :library:test --tests "*AeroCalendarGridTest*"` | Wave 0 — file does not exist |
| #1 (cont'd): `AeroCalendarPositionProvider` no-clip on 1024dp window | Position provider returns valid offset for wide popup near right edge | unit (PopupPositionProvider contract test) | `./gradlew :library:test --tests "*AeroCalendarPositionProviderTest*"` | Wave 0 — file does not exist |
| #2: `AeroColorMath` round-trip drift `hsv(0,1,1) → rgb → hsv` hue within `0.001f` | Pure red round-trip preserves hue | unit | `./gradlew :library:test --tests "*AeroColorMathTest*"` | Wave 0 — file does not exist |
| #3: `AeroHsvColorSquare` + `AeroHueSlider` first-pixel drag response | First mouse-move fires `onColorChange` (no slop delay) | smoke (compile-load) + manual-only (interactive scratch section) | `./gradlew :library:test --tests "*AeroHsvColorSquareTest*"` (compile smoke) + visual eyes-on via Phase7ScratchSection | Wave 0 — files do not exist |
| #4: `Modifier.aeroDragSplitter` first-pixel response (horizontal + vertical) | First mouse-move after down fires `onDrag(deltaPx)` | smoke (compile-load) + manual-only (interactive scratch section) | compile smoke via `./gradlew :library:compileKotlin` (Modifier extension is internal — no class generation; load-test the file's primary `Kt` class) + visual eyes-on via Phase7ScratchSection | Wave 0 — file does not exist |
| #5: `AeroStepIndicator` renders current/completed/upcoming across 3 themes | Step states visually distinct in AeroBlue / AeroDark / Classic | manual-only (three-theme visual via Phase7ScratchSection) — NO automated test | visual eyes-on via Phase7ScratchSection (theme switch toggles all 3) | Wave 0 — file does not exist |

**Notes on test types:**
- **unit:** real logic/math test with concrete assertions (`AeroColorMathTest`, `AeroCalendarGridTest`, `AeroCalendarPositionProviderTest`).
- **smoke (compile-load):** existing project pattern from `AeroDividerTest.kt` — `Class.forName("com.mordred.aero...XxxKt")` confirms the file compiled and is on the classpath. Used for Composable primitives where rendering needs Compose UI test infra (out of v2.0 scope).
- **manual-only:** for Canvas-render visual + interactive drag-response — covered by `Phase7ScratchSection` in showcase. Justified because Compose UI screenshot/Paparazzi infrastructure is OUT of v2.0 scope per Phase 6 carry-forward decision.

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests "*AeroCalendarGridTest*" --tests "*AeroColorMathTest*"` (the two CONTEXT-locked unit tests; ~5 seconds; runs whenever those modules are modified).
- **Per wave merge:** `./gradlew :library:test` (full :library suite; existing 27 test files + the 2–3 new ones; ~30–60 seconds).
- **Phase gate:** `./gradlew :library:test` green AND `./gradlew :showcase:run` launches AND Phase7ScratchSection exercises all 6 primitives in all 3 themes (eyes-on via `ThemeSwitcher`) AND a touchslop spike result is recorded in plan-01 SUMMARY before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` — covers ROADMAP criterion #1 (3-scenario calendar test). Test directory `pickers/internal/calendar/` does not exist.
- [ ] `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` — covers ROADMAP criterion #2 (round-trip drift test). Test directory `pickers/internal/color/` does not exist.
- [ ] `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` — covers ROADMAP criterion #1 (popup-no-clip on 1024dp window) and PITFALL-08 (first-frame guard). Test directory `internal/popup/` does not exist. **NOT in CONTEXT.md test list — recommend planner add this 4-case test for parity with existing `AeroPopupPositionProviderTest.kt` (1024dp wide popup, narrow popup, top-flip, first-frame guard).**
- [ ] (Optional, planner discretion) Compile-smoke tests for `AeroDragSplitterKt`, `AeroHsvColorSquareKt`, `AeroHueSliderKt`, `AeroStepIndicatorKt`, `AeroCalendarGridKt`. Existing project pattern; ~5 lines each; provides "did this file compile?" coverage. Their absence is acceptable since ALL these files are touched by `:library:compileKotlin` already.
- [ ] Framework install: NONE — JUnit 5 + kotlin-test infra is already present in `library/build.gradle.kts:21-26`; no Gradle work needed for new tests.

## File-Path Proposals (deliverables)

Per CONTEXT-locked hybrid layout. All paths absolute from project root.

| # | Primitive | Source path | Test path |
|---|-----------|-------------|-----------|
| 1 | `Modifier.aeroDragSplitter` | `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` | (smoke-only optional; planner picks) |
| 2 | `AeroCalendarPositionProvider` | `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt` | `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt` (RECOMMENDED — parity with existing `AeroPopupPositionProviderTest.kt`) |
| 3 | `AeroCalendarGrid` | `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` | `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt` |
| 4 | `AeroColorMath` | `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt` | `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt` |
| 5 | `AeroHsvColorSquare` | `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` | (smoke-only optional) |
| 6 | `AeroHueSlider` | `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` | (smoke-only optional) |
| 7 | `AeroStepIndicator` | `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` | (smoke-only optional) |
| 8 | `Phase7ScratchSection` (TEMPORARY — deleted Phase 11) | `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` | n/a (showcase has no tests) |

**Modified files:**
- `gradle/libs.versions.toml` — add `kotlinxDatetime = "0.6.2"` version + library entry
- `library/build.gradle.kts` — add `implementation(libs.kotlinx.datetime)` inside `dependencies { ... }`
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — add `Phase7ScratchSection()` call + import (both removed in Phase 11)

## Build-Order Recommendation

Suggested ordering for plans within Phase 7. Optimizes for: (a) early pitfall mitigation, (b) test-first on independently testable primitives, (c) downstream-readiness for Phase 8.

| Order | Item | Risk | Rationale |
|-------|------|------|-----------|
| 1 | Bump `kotlinx-datetime:0.6.2` + first-compile validation + touchslop spike | LOW–MED | Dep + spike are both "outcome documents" — fastest things to land; everything downstream needs the dep. Touchslop spike result also drives `aeroDragSplitter` confidence. |
| 2 | `AeroColorMath` + `AeroColorMathTest` (round-trip) | LOW | Pure functions, fully unit-testable, defuses PITFALL-15. No upstream dependency. The `Color.hsv()` boundary convention (degrees vs normalized) is locked here for downstream HsvColorSquare/HueSlider. |
| 3 | `Modifier.aeroDragSplitter` + (optional) compile-smoke test | MED | Establishes PITFALL-03 mitigation as the canonical pattern. Built before HsvColorSquare/HueSlider so the loop pattern is reused identically. |
| 4 | `AeroCalendarPositionProvider` + `AeroCalendarPositionProviderTest` | LOW–MED | PopupPositionProvider implementation is fully unit-testable (no Compose UI needed — just `IntRect`/`IntSize` inputs and `IntOffset` output). Defuses PITFALL-02 + PITFALL-08. |
| 5 | `AeroCalendarGrid` + `AeroCalendarGridTest` (3-scenario) | MED | Depends on `kotlinx-datetime` (#1) and reuses `AeroIcons.CaretLeft`/`CaretRight`. Largest primitive (probably ~150 lines). |
| 6 | `AeroHsvColorSquare` + `AeroHueSlider` | MED | Reuse `aeroDragSplitter`-style loop pattern (#3) and `AeroColorMath` (#2). Eyes-on via scratch section. |
| 7 | `AeroStepIndicator` | LOW | Last because lowest risk: pure visual primitive, three-state stencil, no drag, no popup, no calendar arithmetic. Eyes-on via scratch section across all 3 themes. |
| 8 | `Phase7ScratchSection` + `ShowcaseApp.kt` insertion | LOW | Glue-code; depends on #1–#7. |

**Suggested plan split (per CONTEXT.md "Claude's Discretion: 2–3 plans recommended"):**
- **Plan-01 (logic + tests):** dep bump, touchslop spike, `AeroColorMath` + test, `AeroCalendarGrid` + test, `AeroCalendarPositionProvider` + test. Rationale: everything in plan-01 is unit-testable via JUnit; plan-01 is "green CI" before any Canvas code.
- **Plan-02 (visuals + scratch):** `Modifier.aeroDragSplitter`, `AeroHsvColorSquare`, `AeroHueSlider`, `AeroStepIndicator`, `Phase7ScratchSection`, `ShowcaseApp.kt` wiring. Rationale: all visual primitives that exercise via the scratch section.

(Single plan also acceptable per CONTEXT — planner picks based on wave reviewability.)

## API Signatures (concrete, derived from CONTEXT + downstream consumers)

### `Modifier.aeroDragSplitter`
```kotlin
package com.mordred.aero.components.internal.drag

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier

internal fun Modifier.aeroDragSplitter(
    orientation: Orientation,
    onDrag: (deltaPx: Float) -> Unit,
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,
): Modifier
```

### `AeroCalendarPositionProvider`
```kotlin
package com.mordred.aero.components.internal.popup

import androidx.compose.ui.window.PopupPositionProvider

internal class AeroCalendarPositionProvider(
    private val gap: Int = 4,
) : PopupPositionProvider
```

### `AeroCalendarGrid`
```kotlin
package com.mordred.aero.components.pickers.internal.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

@Composable
internal fun AeroCalendarGrid(
    displayMonth: LocalDate,
    selected: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: (LocalDate) -> Boolean = { false },
)
```

### `AeroColorMath` (top-level functions; alternative `internal object` is acceptable)
```kotlin
package com.mordred.aero.components.pickers.internal.color

internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float>
internal fun hexToRgb(hex: String): Triple<Int, Int, Int>?
internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String
// Optional companion (planner discretion): hexToRgba (for enableAlpha=true ColorPicker case)
internal fun hexToRgba(hex: String): IntArray?
```

### `AeroHsvColorSquare`
```kotlin
package com.mordred.aero.components.pickers.internal.color

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AeroHsvColorSquare(
    hue: Float,                  // [0f, 360f] — locked by AeroColorMath plan-01 convention
    saturation: Float,           // [0f, 1f]
    value: Float,                // [0f, 1f]
    onSatValChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
)
```

### `AeroHueSlider`
```kotlin
package com.mordred.aero.components.pickers.internal.color

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AeroHueSlider(
    hue: Float,                  // [0f, 360f]
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
)
```

### `AeroStepIndicator`
```kotlin
package com.mordred.aero.components.layout.internal.stepper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AeroStepIndicator(
    currentStep: Int,            // 0-based; 0 = first step is current
    totalSteps: Int,             // >= 1
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,  // null = non-interactive (default for AeroStepperWizard)
)
```

## Sources

### Primary (HIGH confidence)
- Existing codebase — read-only references during research:
  - `library/src/main/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProvider.kt`
  - `library/src/main/kotlin/com/mordred/aero/components/popup/AeroDropdownPopup.kt`
  - `library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt`
  - `library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt` (cursor pattern; `detectDragGestures` usage example which Phase 7 deliberately replaces in-content)
  - `library/src/main/kotlin/com/mordred/aero/components/list/AeroBadge.kt` (visual scale + Color.Unspecified sentinel pattern)
  - `library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt` (existing `pointerInput` use)
  - `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` (23-token reference for AeroBlue/AeroDark/Classic)
  - `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` (`AeroTheme.colors` access pattern)
  - `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` (`Modifier.glassPanel(cornerRadius)`)
  - `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` + `icons/internal/{Check,CaretLeft,CaretRight}.kt` (verified all 3 icon files present)
  - `library/build.gradle.kts` (test infra wired)
  - `library/src/test/kotlin/com/mordred/aero/components/popup/AeroPopupPositionProviderTest.kt` (test pattern reference)
  - `library/src/test/kotlin/com/mordred/aero/components/containers/AeroDividerTest.kt` (compile-smoke pattern)
- Project planning artifacts (research baseline):
  - `.planning/research/PITFALLS.md` — PITFALL-02, PITFALL-03 (with full code template), PITFALL-08, PITFALL-15, W11-01, W11-02
  - `.planning/research/SUMMARY.md` — architecture approach, primitive listing, complexity ratings
  - `.planning/research/ARCHITECTURE.md` — package layout
  - `.planning/research/STACK.md` — Kotlin 2.1.21 / CMP 1.7.3 / kotlinx-datetime 0.6.2 rationale
  - `.planning/ROADMAP.md` §Phase 7 — 5 success criteria + 4 phase notes
  - `.planning/REQUIREMENTS.md` §Coverage — Phase 7 enabling-phase rationale
  - `.planning/STATE.md` — Pending Todos (touchslop spike, kotlinx-datetime first-compile validation now moved to Phase 7)

### Secondary (MEDIUM confidence — verified against multiple sources)
- `Color.hsv()` API signature and `requirePrecondition` validation — verified directly from androidx-main GitHub source (compose/ui/ui-graphics/src/commonMain/kotlin/androidx/compose/ui/graphics/Color.kt). Function takes hue in `[0f..360f]`, saturation/value in `[0f..1f]`.
- `kotlinx-datetime:0.6.2` stability and version progression — verified from GitHub releases page; 0.6.2 (Feb 2024) is the last stable in the 0.6.x line; 0.7.0/0.7.1 (Jun–Jul 2024) introduced breaking renames; 0.8.0-rc01/02 are pre-release.
- `awaitPointerEventScope` + manual loop pattern — confirmed via `.planning/research/PITFALLS.md` PITFALL-03 (project's own research, grounded in JetBrains/compose-jb #343); cross-referenced with Compose Pointer documentation overview at developer.android.com.

### Tertiary (LOW confidence — flagged for spike validation)
- Status of CMP 1.7.3 touchSlop fix — conflicting reports (some web sources suggest the slop was reduced; no canonical "fixed in CMP X.Y.Z" announcement). The 1-minute spike in plan-01 is the resolution path. Ship `awaitPointerEventScope` regardless.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Kotlin/CMP/JDK versions + `kotlinx-datetime:0.6.2` are verified against existing `gradle/libs.versions.toml` and the CONTEXT-locked decision; `Color.hsv` API verified against androidx-main source.
- Architecture: HIGH — package layout fully specified by CONTEXT.md "Internal package layout — hybrid (locked)"; existing precedent in `icons/internal/` (138 files) confirms the convention works under `explicitApi()`.
- Pitfalls: HIGH — every pitfall mentioned has a verified prevention pattern (either in PITFALLS.md PITFALL-XX, or directly observable in existing `:library` source).
- Validation Architecture: HIGH — test infra is already present (JUnit 5 + kotlin-test wired); existing `AeroPopupPositionProviderTest.kt` and `AeroDividerTest.kt` patterns are reusable.
- Open question on hue convention: MEDIUM — unresolvable without implementer choice; locked in plan-01 ADR.
- Open question on touchSlop fix in CMP 1.7.3: LOW — resolves via 1-minute spike in plan-01.

**Research date:** 2026-04-30
**Valid until:** 2026-05-30 (30 days; Compose Desktop is a stable API surface; the only fast-moving piece is the touchSlop fix status, which the spike resolves immediately).
