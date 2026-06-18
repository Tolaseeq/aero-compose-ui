# Phase 8: Pickers - Research

**Researched:** 2026-06-18
**Domain:** Compose Desktop pickers — six public components built on Phase 7 internal primitives
**Confidence:** HIGH (stack already on classpath and compiling; primitives inspected from source; critical unknowns resolved)

---

<user_constraints>
## User Constraints (from 08-CONTEXT.md)

### Locked Decisions

- Build order: RangeSlider → DatePicker → TimePicker → DateTimePicker → DateRangePicker → ColorPicker
- Trigger closed-state = read-only AeroTextField + AeroIconButton; single popup per picker
- State API = controlled: `value: T?` + `onValueChange: (T) -> Unit` across all pickers
- Value formatting = caller-supplied formatter; ISO default; i18n is consumer concern
- Empty/placeholder: `value == null`; `placeholder: String`; optional clear via `clearable: Boolean = false`
- Selectable-date constraints: `minDate/maxDate + selectableDates: (LocalDate) -> Boolean` mapped to existing `AeroCalendarGrid(isDisabled=...)`
- AeroDateRangePicker trigger = single read-only field rendering `start → end` + one icon button + one popup
- TimePicker controls = two AeroNumberSpinner instances; 24-hour only (12h/AM-PM is explicitly dropped)
- Seconds: `showSeconds: Boolean = false` adds a third AeroNumberSpinner (0–59)
- minuteStep: `minuteStep: Int = 1`; free via AeroNumberSpinner's existing `step` parameter
- ColorPicker surfaces: `AeroColorPicker` (inline panel) + `AeroColorPickerButton` (thin Popup wrapper around same panel)
- ColorPicker panel layout (top→bottom): HSV square + hue strip row → preview bar → RGB sliders → HEX field → swatch row
- ColorPicker callback type = Compose `Color`; internal truth = HSV(A) float (PITFALL-15)
- Alpha: `enableAlpha: Boolean = false` — adds alpha slider + checkerboard; HEX widens to `#RRGGBBAA`
- Swatches: fixed default ~16-color set (Aero palette); `swatches: List<Color>? = null` to override
- RangeSlider API: `value: ClosedFloatingPointRange<Float>` + `onValueChange` + `valueRange` + `steps`
- RangeSlider tooltip: glass pill above thumb during drag, behind `showTooltip: Boolean = true`
- Thumbs cannot cross; most-recently-moved thumb drawn on top (PITFALL-07, locked)
- All Canvas drag via `awaitPointerEventScope` manual loop — `detectDragGestures` is BANNED
- All popups via `Popup(popupPositionProvider = AeroCalendarPositionProvider(...))` — NOT AeroDropdownPopup
- No new AeroColorScheme tokens; no changes to v1.x public API
- kotlinx-datetime:0.6.2 already on classpath (added Phase 7, verified compiling)
- Popup animation: fade (alpha 0→1) + light scale (~0.96→1), ~120–150ms; subject to per-animation user sign-off
- Day-cell selection and DateRangePicker range hover-preview: soft color transition ~80–100ms; subject to sign-off

### Claude's Discretion

- Exact 16 default swatch colors (Aero-fit palette)
- ColorPicker panel exact dimensions, spacing, and whether RGB/HEX sit in one or two visual rows
- HSV square / hue strip pixel sizes (Phase 7 defaults: 256×256 square; 24dp-wide hue strip)
- Default ISO format strings and exact formatter parameter shape (`(T) -> String` vs format-string)
- Calendar keyboard navigation depth, HEX-input commit timing (Enter vs focus-loss), popup dismiss rules — within `PopupProperties(focusable=true, dismissOnClickOutside=true)` baseline
- Plan granularity / wave split (build order is locked)

### Deferred Ideas (OUT OF SCOPE)

- 12-hour / AM-PM TimePicker mode
- Calendar keyboard-navigation richness (arrow-key grid traversal, type-to-jump)
- Inline (always-visible) date/time pickers — v3+ (PICK-INL-01)
- ColorPicker eyedropper / screen color picking — platform-specific (COLOR-EYE-01)
- Localized / locale-aware default formatting
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PICK-01 | AeroDatePicker — popup month calendar → LocalDate; prev/next nav; visual highlights | AeroCalendarGrid (Phase 7) is ready; AeroCalendarPositionProvider is ready; LocalDate on classpath |
| PICK-02 | AeroDateRangePicker — dual-month popup; start→end selection; range highlight; responsive stacking | Sealed state pattern (PITFALL-06); PITFALL-08 guard already in AeroCalendarPositionProvider; BoxWithConstraints for stack |
| PICK-03 | AeroTimePicker — hour/minute AeroNumberSpinner; 0–23/0–59 range; LocalTime callback; 24h only | AeroNumberSpinner's min/max/step already cover this; LocalTime on classpath |
| PICK-04 | AeroDateTimePicker — calendar + time controls + Apply/Cancel → LocalDateTime | Composition of DatePicker + TimePicker plumbing; Apply button prevents auto-close |
| PICK-05 | AeroColorPicker — HSV square + hue + RGB + HEX; 5 controls mutually synced; single HSV truth | AeroHsvColorSquare, AeroHueSlider, AeroColorMath all Phase 7 ready; PITFALL-15 guard established |
| PICK-06 | AeroColorPicker — ~16 preset swatches; click sets color; before/after preview bar | Default swatch list is Claude's discretion; preview = two color rects |
| PICK-07 | AeroColorPicker — alpha channel; alpha slider + checkerboard; HEX widens to RRGGBBAA | hexToRgba already in AeroColorMath; alpha param in Color.hsv() |
| PICK-08 | AeroRangeSlider — dual thumb; awaitPointerEventScope drag; start≤end enforcement; range fill | Pattern established by AeroDragSplitter and AeroHsvColorSquare; NOT a Material3 RangeSlider wrapper |
</phase_requirements>

---

## Summary

Phase 8 is unusually implementation-ready: the six internal primitives (AeroCalendarGrid, AeroCalendarPositionProvider, AeroColorMath, AeroHsvColorSquare, AeroHueSlider, AeroDragSplitter) are fully implemented and have passing unit tests from Phase 7. The only new dependency — `kotlinx-datetime:0.6.2` — is already wired into `libs.versions.toml` and `library/build.gradle.kts`, and the git history confirms it compiled cleanly in Phase 7 (`feat(07-01): implement AeroCalendarGrid composable with kotlinx-datetime`).

The two genuine unknowns at research start were: (1) `kotlinx-datetime:0.6.2` ↔ Kotlin 2.1.21 formal compatibility, and (2) `Color.hsv()` precondition behavior at the Compose API boundary. Both are now resolved. The library's JVM POM declares `kotlin-stdlib:1.9.21` as a compile dependency, but Kotlin's binary-compatibility guarantee means it runs on Kotlin 2.1.21 without recompilation of the library itself — confirmed by the Kotlin 2.1 compatibility guide (only `kotlin-stdlib-common.jar` was removed; the main stdlib continues). Confidence on this point is upgraded to HIGH given that Phase 7 actually compiled successfully.

The discretionary open questions — HEX-input commit timing, popup open/close animation curves, the exact swatch palette — are documented below with the recommended idiomatic patterns so the planner can proceed without further user input.

**Primary recommendation:** Wire the six Phase 7 primitives into the six public picker components, one component per plan task, in the locked build order. No new infrastructure is needed — every primitive, popup provider, and reusable component (AeroNumberSpinner, AeroSlider, AeroTextField) already exists.

---

## Standard Stack

### Core (all already on `:library` classpath — NO new dependencies)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Kotlin | 2.1.21 | Language | LOCKED |
| Compose Multiplatform | 1.7.3 | UI framework | LOCKED |
| kotlinx-datetime | 0.6.2 | `LocalDate`, `LocalTime`, `LocalDateTime` public API types; month arithmetic | LOCKED — already in classpath, confirmed compiling |
| compose.ui.graphics | (CMP 1.7.3) | `Color.hsv(hue, saturation, value, alpha)` for HSV→Color | Already available |
| compose.animation | (CMP 1.7.3) | `AnimatedVisibility`, `fadeIn/Out`, `scaleIn/Out`, `animateColorAsState` | Already available |
| compose.ui.window | (CMP 1.7.3) | `Popup`, `PopupProperties` | Already available |
| JDK 17 | 17 | JVM target (jvmToolchain) | LOCKED |

### Phase 7 Primitives Phase 8 Imports (all `internal`)

| Primitive | File | Consumed By |
|-----------|------|-------------|
| `AeroCalendarGrid` | `components/pickers/internal/calendar/` | DatePicker, DateRangePicker, DateTimePicker |
| `AeroCalendarPositionProvider` | `components/internal/popup/` | All 4 date/time pickers + ColorPickerButton |
| `AeroColorMath` | `components/pickers/internal/color/` | ColorPicker (HSV/RGB/HEX conversions) |
| `AeroHsvColorSquare` | `components/pickers/internal/color/` | ColorPicker HSV square |
| `AeroHueSlider` | `components/pickers/internal/color/` | ColorPicker hue strip |
| `AeroDragSplitter` | `components/internal/drag/` | RangeSlider dual-thumb (pattern reference) |

### Existing v1.x Components Phase 8 Reuses

| Component | Reused By | How |
|-----------|-----------|-----|
| `AeroNumberSpinner(value, onValueChange, min, max, step)` | TimePicker, DateTimePicker | Hours: `min=0, max=23`; Minutes: `min=0, max=59, step=minuteStep`; Seconds: `min=0, max=59` |
| `AeroSlider` | ColorPicker | 3× instances for R/G/B channel sliders (`valueRange=0f..255f`) |
| `AeroTextField` | Date/time pickers (read-only trigger); ColorPicker (HEX input) | `readOnly=true` for triggers; editable for HEX |
| `AeroIconButton` / `AeroIcons.Calendar` / `AeroIcons.Clock` / `AeroIcons.X` | All pickers | Trigger button; clear button |
| `GlassModifiers.glassPanel(cornerRadius = 8.dp)` | All popup containers + ColorPicker panel | Popup surface styling |
| `AeroDropdownPopup` two-layer background technique | All picker popups | `.background(colors.background).background(colors.panelBackground)` stack for W11-02 |

**Installation:** No new Gradle dependencies. `kotlinx-datetime:0.6.2` is already declared in `libs.versions.toml` and `library/build.gradle.kts`.

### kotlinx-datetime:0.6.2 ↔ Kotlin 2.1.21 — VERDICT

**RESOLVED HIGH CONFIDENCE.**

- The `kotlinx-datetime-jvm:0.6.2` POM declares `kotlin-stdlib:1.9.21` as its compile dependency (built with Kotlin 1.9.21).
- The Kotlin 2.1 compatibility guide documents only two binary-incompatible changes: removal of `kotlin-stdlib-common.jar` and a visibility alignment for var/val overrides. Neither affects `kotlinx-datetime`.
- The strongest evidence: Phase 7 committed `feat(07-01): implement AeroCalendarGrid composable with kotlinx-datetime` against Kotlin 2.1.21 and CMP 1.7.3 with zero compile errors. The library is on the classpath and in active use.
- **Fallback is not needed.** If for any reason a future Kotlin upgrade produces an issue, the documented fallback is `0.7.1-0.6.x-compat` (type-alias compat artifact).

---

## Architecture Patterns

### Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
├── components/
│   ├── range/
│   │   ├── AeroSlider.kt              # UNCHANGED
│   │   └── AeroRangeSlider.kt         # NEW — Phase 8 (PICK-08)
│   └── pickers/
│       ├── AeroDatePicker.kt          # NEW — Phase 8 (PICK-01)
│       ├── AeroTimePicker.kt          # NEW — Phase 8 (PICK-03)
│       ├── AeroDateTimePicker.kt      # NEW — Phase 8 (PICK-04)
│       ├── AeroDateRangePicker.kt     # NEW — Phase 8 (PICK-02)
│       ├── AeroColorPicker.kt         # NEW — Phase 8 (PICK-05/06/07) — inline panel
│       ├── AeroColorPickerButton.kt   # NEW — Phase 8 (PICK-05/06/07) — swatch trigger + Popup
│       └── internal/                  # Phase 7 — already exists, Phase 8 imports only
│           ├── calendar/AeroCalendarGrid.kt
│           ├── color/AeroColorMath.kt
│           ├── color/AeroHsvColorSquare.kt
│           └── color/AeroHueSlider.kt
```

`AeroRangeSlider` lives in `components/range/` (alongside AeroSlider — same package, same family). All date/time and color pickers live in `components/pickers/`.

### Pattern 1: Controlled State + Read-Only Trigger

All four date/time pickers follow the same outer pattern:

```kotlin
// Source: 08-CONTEXT.md §Implementation Decisions — identical across all date/time pickers
@Composable
public fun AeroDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDate) -> String = { it.toString() },   // ISO default
    placeholder: String = "Select date",
    clearable: Boolean = false,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = value?.let { formatter(it) } ?: ""

    Box(modifier = modifier) {
        // Trigger row: read-only AeroTextField + AeroIconButton
        Row(verticalAlignment = Alignment.CenterVertically) {
            AeroTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    AeroIconButton(onClick = { if (enabled) expanded = !expanded }) {
                        Icon(AeroIcons.Calendar, contentDescription = "Open calendar")
                    }
                    if (clearable && value != null) {
                        AeroIconButton(onClick = { onValueChange(/* clear = */ null as LocalDate) }) {
                            Icon(AeroIcons.X, contentDescription = "Clear")
                        }
                    }
                }
            )
        }

        // Popup: anchored via AeroCalendarPositionProvider (PITFALL-02 guard)
        if (expanded) {
            Popup(
                popupPositionProvider = remember { AeroCalendarPositionProvider(gap = 4) },
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                // W11-02 two-layer background — no Modifier.shadow (shadow clips on undecorated)
                Box(
                    Modifier
                        .background(colors.background, RoundedCornerShape(8.dp))
                        .background(colors.panelBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .glassPanel(cornerRadius = 8.dp)
                ) {
                    var displayMonth by remember { mutableStateOf(value ?: todayLocalDate()) }
                    AeroCalendarGrid(
                        displayMonth = displayMonth,
                        selected = value,
                        onDateSelected = { date ->
                            onValueChange(date)
                            expanded = false
                        },
                        onMonthChange = { displayMonth = it },
                        isDisabled = { date ->
                            (minDate != null && date < minDate) ||
                            (maxDate != null && date > maxDate) ||
                            !selectableDates(date)
                        },
                    )
                }
            }
        }
    }
}
```

### Pattern 2: AeroRangeSlider — Custom Canvas Dual Thumb (NOT Material3 RangeSlider)

The ARCHITECTURE.md suggested wrapping Material3 `RangeSlider`, but the CONTEXT.md (PICK-08) and PITFALL-07 lock the implementation as custom Canvas. The reason: PITFALL-07 explicitly states "Do NOT try to compose two AeroSlider instances" and PITFALL-03 bans `detectDragGestures` for all Canvas drag — the Material3 `RangeSlider` uses `detectDragGestures` internally, which means it will hit the 18dp touchSlop on Desktop. **Use custom Canvas with `awaitPointerEventScope`.**

```kotlin
// Custom Canvas RangeSlider pattern — follows AeroHsvColorSquare/AeroDragSplitter pattern
@Composable
public fun AeroRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true,
) {
    val colors = AeroTheme.colors
    var lastMovedThumb by remember { mutableStateOf(Thumb.End) }  // for z-order on overlap

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Determine which thumb is closer to the down position
                        val startX = valueToX(value.start, valueRange, size.width.toFloat())
                        val endX   = valueToX(value.endInclusive, valueRange, size.width.toFloat())
                        val thumb = if (abs(down.position.x - startX) <= abs(down.position.x - endX))
                            Thumb.Start else Thumb.End
                        lastMovedThumb = thumb
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val newVal = xToValue(change.position.x, valueRange, size.width.toFloat())
                            val newRange = when (thumb) {
                                Thumb.Start -> (newVal.coerceAtMost(value.endInclusive))..value.endInclusive
                                Thumb.End   -> value.start..(newVal.coerceAtLeast(value.start))
                            }
                            onValueChange(newRange)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        // Inactive track (full width)
        drawTrack(colors.borderDefault, 0f, size.width)
        // Active track (between thumbs)
        val startX = valueToX(value.start, valueRange, size.width)
        val endX   = valueToX(value.endInclusive, valueRange, size.width)
        drawTrack(colors.primary, startX, endX)
        // Draw thumbs — most-recently-moved on top (drawn last)
        val firstThumb  = if (lastMovedThumb == Thumb.Start) Thumb.End else Thumb.Start
        drawThumb(firstThumb, colors, value, valueRange)
        drawThumb(lastMovedThumb, colors, value, valueRange)
    }
}
```

### Pattern 3: DateRangePicker Sealed State (PITFALL-06)

```kotlin
// Source: PITFALL-06 + 08-CONTEXT.md
sealed class AeroDateRangeState {
    object Idle : AeroDateRangeState()
    data class SelectingEnd(val start: LocalDate) : AeroDateRangeState()
    data class Selected(val start: LocalDate, val end: LocalDate) : AeroDateRangeState()
}

// Inside AeroDateRangePicker:
var rangeState by remember { mutableStateOf<AeroDateRangeState>(AeroDateRangeState.Idle) }
var hoveredDate by remember { mutableStateOf<LocalDate?>(null) }

// On day click:
fun onDayClick(date: LocalDate) {
    rangeState = when (val s = rangeState) {
        is AeroDateRangeState.Idle,
        is AeroDateRangeState.Selected -> AeroDateRangeState.SelectingEnd(date)
        is AeroDateRangeState.SelectingEnd -> {
            val (start, end) = if (date >= s.start) s.start to date else date to s.start
            onRangeSelect(start, end)    // callback fires ONLY here
            AeroDateRangeState.Selected(start, end)
        }
    }
}
// CRITICAL: onRangeSelect is called ONLY when transitioning to Selected — not on first click.
```

Note: PICK-02 explicitly says "range NOT auto-swapped if end < start" — the swap above handles presentation in the popup (which thumb is rendered as start/end), but the swap for internal rendering is fine because the callback emits the correctly-ordered pair. The caller validates business rules.

### Pattern 4: ColorPicker HSV State Machine (PITFALL-15)

```kotlin
// Source: PITFALL-15 + AeroColorMath.kt (Phase 7 source)
// Internal state — NEVER store Color or RGB alongside HSV
var hue        by remember { mutableFloatStateOf(0f) }   // [0f, 360f] degrees
var saturation by remember { mutableFloatStateOf(1f) }   // [0f, 1f]
var value      by remember { mutableFloatStateOf(1f) }   // [0f, 1f]
var alpha      by remember { mutableFloatStateOf(1f) }   // [0f, 1f] — only when enableAlpha=true

// Derived output — NEVER stored as state
val currentColor = Color.hsv(hue, saturation, value, if (enableAlpha) alpha else 1f)

// When RGB slider moves:
fun onRedChange(r: Float) {
    val (h, s, v) = rgbToHsv(r / 255f, currentColor.green, currentColor.blue)
    hue = h; saturation = s; value = v  // RGB→HSV immediately; no RGB state kept
}

// When HEX input commits (on Enter or focus loss — see discretion below):
fun onHexCommit(hex: String) {
    val rgb = hexToRgb(hex) ?: return  // null = parse failure, keep current state
    val (h, s, v) = rgbToHsv(rgb.first / 255f, rgb.second / 255f, rgb.third / 255f)
    hue = h; saturation = s; value = v
    // alpha unchanged unless `#RRGGBBAA` form and enableAlpha=true
}

// Callback to consumer — derived, not stored:
LaunchedEffect(hue, saturation, value, alpha) {
    onValueChange(currentColor)
}
```

### Pattern 5: Popup Open/Close Animation

```kotlin
// AnimatedVisibility wrapping the Popup content — fires on expanded state
// Animation direction: agreed in 08-CONTEXT.md; exact curves need per-animation sign-off
AnimatedVisibility(
    visible = expanded,
    enter = fadeIn(animationSpec = tween(120)) + scaleIn(
        initialScale = 0.96f,
        animationSpec = tween(120),
        transformOrigin = TransformOrigin(0.5f, 0f),  // scale from top center (trigger)
    ),
    exit  = fadeOut(animationSpec = tween(100)) + scaleOut(
        targetScale = 0.96f,
        animationSpec = tween(100),
        transformOrigin = TransformOrigin(0.5f, 0f),
    ),
) {
    // popup content
}
// NOTE: AnimatedVisibility wraps the Popup content box, not the Popup composable itself.
// The Popup composable must still be present in the tree when expanded=false to avoid
// recomposition thrash; use if(expanded) Popup { AnimatedVisibility(visible=true) } pattern.
// Alternatively: keep Popup always in tree with if(!expanded) return@Popup.
```

### Pattern 6: HEX Input Commit Timing (Discretion)

Idiomatic Compose Desktop recommendation: commit on **both** Enter (via `KeyboardOptions(imeAction = ImeAction.Done) + KeyboardActions(onDone = { onHexCommit(textState) })`) and focus-loss (via `Modifier.onFocusChanged { if (!it.isFocused) onHexCommit(textState) }`). This covers the "user types then clicks elsewhere" case without requiring an explicit button. The in-flight text state (partial HEX like `#FF`) must not call `onHexCommit` until length is 6 or 8 — guard with `if (cleaned.length == 6 || cleaned.length == 8)`.

### Anti-Patterns to Avoid

- **DO NOT wrap Material3 `RangeSlider`:** It uses `detectDragGestures` internally → 18dp touchSlop → silent drag failure on Desktop (PITFALL-03). Build custom Canvas.
- **DO NOT reuse `AeroDropdownPopup` for any picker popup:** It locks popup width to trigger width (`widthIn(min=anchorWidth, max=anchorWidth)`) — calendar panels are 252dp; DateRangePicker is ~560dp. Use raw `Popup(popupPositionProvider = AeroCalendarPositionProvider(...))`.
- **DO NOT use `AeroPopupPositionProvider` (the old one):** It flips vertically when width overflows — wrong for wide calendars. Only `AeroCalendarPositionProvider` is correct.
- **DO NOT store Color or RGB as state alongside HSV in ColorPicker:** Round-trip drift (PITFALL-15). HSV is the single source of truth; Color and RGB are derived on every emit.
- **DO NOT call `onRangeSelect` when only the start date has been clicked:** Partial state leaks the unfinished range to the caller (PITFALL-06). Use the sealed state machine.
- **DO NOT use `Dialog(transparent=true)`:** Win11 EXCEPTION_ACCESS_VIOLATION (W11-01). All popups use `Popup(...)`.
- **DO NOT pass hue outside `[0f, 360f]` to `Color.hsv()`:** It calls `requirePrecondition` and throws. The existing `AeroHsvColorSquare` and `AeroHueSlider` both have `.coerceIn(0f, 360f)` guards — maintain these guards in any new code that constructs `Color.hsv(hue, ...)`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Month grid rendering | Custom date grid | `AeroCalendarGrid` (Phase 7) | Leap year, DOW offset, disable predicate, theme tokens — all already correct |
| Popup positioning for wide calendars | Custom PositionProvider | `AeroCalendarPositionProvider` (Phase 7) | PITFALL-08 first-frame guard + PITFALL-02 no-width-lock already implemented |
| HSV ↔ RGB ↔ HEX conversion | Custom math | `AeroColorMath` (`rgbToHsv`, `hexToRgb`, `hexToRgba`, `rgbToHex`) (Phase 7) | Round-trip tested; PITFALL-15 fidelity confirmed by unit tests |
| HSV 2D gradient + drag | Custom Canvas | `AeroHsvColorSquare` (Phase 7) | awaitPointerEventScope drag already correct; coerceIn guard in place |
| Hue strip + drag | Custom Canvas | `AeroHueSlider` (Phase 7) | Same; borderSelected indicator line; 7-stop gradient correct |
| Hour/minute entry with range clamp | Custom number input | `AeroNumberSpinner(min=0, max=23/59, step=minuteStep)` | Keyboard + buttons + scroll wheel + clamp + step already built |
| Date arithmetic (month nav, leap year) | `java.util.Calendar` or manual | `kotlinx.datetime.LocalDate.plus(1, DateTimeUnit.MONTH)` | Immutable, correct at year boundaries, already on classpath |
| RGB slider for ColorPicker | Custom slider | `AeroSlider(valueRange = 0f..255f)` | Aero styling already applied; 3× reuse |
| Popup glass surface | Custom gradient | `Modifier.glassPanel(cornerRadius = 8.dp)` | Existing convention for all popup containers |
| Popup shadow | `Modifier.shadow(elevation = 8.dp)` | Two-layer background technique (`AeroDropdownPopup` pattern) | Shadow clips outside popup AWT window bounds on undecorated Win11 (W11-02) |

**Key insight:** Phase 7 was explicitly designed so that Phase 8 wires, not rebuilds. Every piece of the heavy lifting is already done and tested. The implementation budget for Phase 8 is entirely in the public API surface, state management, and composition — not primitives.

---

## Common Pitfalls

The following are **new pitfalls not yet in PITFALLS.md**, or actionable precision additions to existing entries.

### NEW-PICK-01: `Color.hsv()` throws on hue outside `[0f, 360f]`

**What goes wrong:** `Color.hsv(hue, saturation, value, alpha)` calls `requirePrecondition` internally and throws `IllegalArgumentException` if `hue < 0f` or `hue > 360f`. The error message is: `"HSV ($hue, $saturation, $value) must be in range (0..360, 0..1, 0..1)"`.

**When it happens:** `rgbToHsv` can theoretically return a hue in `[0f, 360f)` (the `hPositive` branch handles negative hues), but floating-point arithmetic near 360.0 can produce `360.0f` exactly after the modulo in the green/blue branches (`60f * (((g - b) / delta) % 6f)`). If the `% 6f` path yields exactly `6.0f` for pure red, the hue computes to `360.0f` — which triggers the precondition.

**Prevention:** Wrap every `Color.hsv()` call with `hue.coerceIn(0f, 360f)`. This is already done in `AeroHsvColorSquare` (line 43: `.coerceIn(0f, 360f)`) and `AeroHueSlider`. Apply the same guard at the `AeroColorPicker` level whenever constructing the output `Color` from internal HSV state.

**Confidence:** HIGH — `requirePrecondition` confirmed in Color.kt source via web search (message text retrieved from androidx source search).

---

### NEW-PICK-02: DateTimePicker popup auto-closes on date selection before time is set

**What goes wrong:** If `AeroDateTimePicker` re-uses the naive `onDateSelected = { date -> onValueChange(...); expanded = false }` pattern from `AeroDatePicker`, the popup closes as soon as the user clicks a day — before they can set the time. The `LocalDateTime` is emitted with whatever the current time is (possibly null/default), not the intended time.

**Why it happens:** Composing `AeroDatePicker` + `AeroTimePicker` and naively forwarding their callbacks would close the picker on the first interaction.

**Prevention:** `AeroDateTimePicker` must hold internal `pendingDate: LocalDate?` and `pendingTime: LocalTime` state. The popup only closes and `onValueChange` only fires when the user clicks the explicit **Apply** button. **Cancel** dismisses without firing. The calendar grid's `onDateSelected` sets `pendingDate` but does NOT close the popup or fire `onValueChange`. (Requirement PICK-04 explicitly requires this: "popup NOT закрывается автоматически при выборе даты.")

---

### NEW-PICK-03: DateRangePicker BoxWithConstraints stacking — threshold must match AeroCalendarGrid width

**What goes wrong:** PICK-02 says stacking occurs at `< ~560dp`. If the threshold is computed as `< 480dp` (two 240dp-wide approximations) but `AeroCalendarGrid` renders at `252.dp + 8.dp padding × 2 + 8.dp gap = ~540dp` minimum, the stacking never triggers and the calendars clip on intermediate window sizes.

**Why it happens:** The actual `AeroCalendarGrid` outer Column has `wrapContentWidth()` wrapping a header of `Modifier.width(252.dp)` with `padding(8.dp)`. This means each calendar takes `252 + 16 = 268dp` minimum. Two calendars side by side + an 8dp gap = 544dp minimum. The `BoxWithConstraints` threshold should be `< 560.dp` (as PICK-02 specifies) to ensure reliable stacking.

**Prevention:** Set `BoxWithConstraints { if (maxWidth < 560.dp) verticalLayout() else horizontalLayout() }`. Do not approximate — derive from the actual `AeroCalendarGrid` rendered width (`252dp header + 16dp padding = 268dp × 2 + gap`).

---

### NEW-PICK-04: AeroNumberSpinner text state desync on parent recomposition

**What goes wrong:** `AeroNumberSpinner` uses `var textState by remember(value) { mutableStateOf(value.toString()) }` (confirmed from source). The `remember(value)` key means the text is reset to `value.toString()` whenever the parent passes a new `value`. In `AeroTimePicker`, if `onValueChange` emits a clamped value that triggers recomposition while the user is mid-typing, the text field's cursor jumps to the end and in-progress input is lost.

**Why it happens:** The spinner's `remember(value)` key resets text on every externally-driven value change. This is intentional for programmatic resets, but in a picker where the user types `"0"` and the spinner is wired to hour state, the parent recomposes and passes `0` back, resetting the text to `"0"` — which is fine. The edge case is typing `-` or an empty string: `AeroNumberSpinner` allows `""` and `"-"` as intermediate states without calling `onValueChange`. If anything else triggers recomposition of the spinner, the `remember(value)` key stays the same (since `value` didn't change) and the intermediate text is preserved. This is actually correct behavior.

**Prevention:** No code change needed — the pattern is correct. But the planner must ensure `AeroTimePicker` does NOT throttle or debounce `onValueChange` at the spinner boundary (which would trigger the desync). Pass spinner callbacks directly to the TimePicker state; do not add intermediate transformations.

---

### NEW-PICK-05: ColorPickerButton popup anchoring — Popup must be inside the trigger Box

**What goes wrong:** If `AeroColorPickerButton` places the `Popup` outside the trigger `Box` (e.g., as a sibling in a `Box` parent rather than a child of the swatch button), the `AeroCalendarPositionProvider` receives `anchorBounds` from the wrong composable — typically the parent layout boundary rather than the trigger swatch. The color picker popup appears at the wrong position.

**Why it happens:** `Popup(popupPositionProvider = ...)` anchors to its **nearest ancestor Layout node** at the point of composition. If the `Popup` call is not a descendant of the trigger button, the anchor is incorrect.

**Prevention:** The `Popup` call must be **inside** the trigger button's `Box` composable scope:
```kotlin
Box(modifier = Modifier.size(32.dp)...) {
    // swatch color box here
    if (expanded) {
        Popup(popupPositionProvider = remember { AeroCalendarPositionProvider() }, ...) {
            AeroColorPicker(...)
        }
    }
}
```
This is the same pattern established by AeroDropdown, AeroComboBox, and AeroTooltip.

---

### Precision Additions to Existing PITFALLS.md Entries (not duplicates — actionable precision)

**PITFALL-07 (RangeSlider thumb overlap) precision:** The `lastMovedThumb` state tracking for z-order must be initialized to `Thumb.End` (not `Thumb.Start`) so the initial render shows start thumb on top when both are at the same position — this matches conventional slider UX where the left thumb is accessible when fully compressed. The minimum separation when `steps > 0` is exactly one step; when `steps = 0` (continuous), enforce a minimum of `(valueRange.endInclusive - valueRange.start) * 0.001f` (0.1% of range) to prevent overlap at floating-point boundary.

**PITFALL-08 (first-frame flash) confirmation:** The `AeroCalendarPositionProvider` already implements the correct `popupContentSize == IntSize.Zero` guard (verified from source: line 46 of `AeroCalendarPositionProvider.kt`). Phase 8 does not need to add this guard — it is already in the Phase 7 primitive. Phase 8 must ensure it does NOT add a second `AnimatedVisibility` that hides the first frame via animation — if visible=false on first frame and the popup is already at the correct position, this creates a visual gap before the fade-in. The correct order: Popup appears → first frame is at `IntOffset.Zero` (off-screen, invisible) → second frame has correct position → AnimatedVisibility fades in.

**PITFALL-09 (AeroDark disabled cells) scope extension:** This applies to AeroDateRangePicker's range-highlight cells in addition to DatePicker disabled cells. In AeroDark, range-highlighted cells (intermediate dates between start and end) should use `colors.primary.copy(alpha = 0.15f)` as background — lighter than the 0.3f selected highlight — rather than `buttonHover` which renders as near-invisible `0x40FFFFFF` on dark backgrounds.

---

## Code Examples

### CalendarGrid Integration (DatePicker)

```kotlin
// Source: AeroCalendarGrid.kt (Phase 7 source — actual parameter signature)
// AeroCalendarGrid(
//     displayMonth: LocalDate,       // any date in the target month; day ignored
//     selected: LocalDate?,
//     onDateSelected: (LocalDate) -> Unit,
//     onMonthChange: (LocalDate) -> Unit,
//     modifier: Modifier = Modifier,
//     isDisabled: (LocalDate) -> Boolean = { false },
// )
var displayMonth by remember { mutableStateOf(value ?: LocalDate(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.year, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.monthNumber, 1)) }
```

Simpler helper for "today in current timezone":
```kotlin
private fun todayLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
```

### Color.hsv() Safe Wrapper

```kotlin
// Always coerce before calling Color.hsv() — requirePrecondition throws on out-of-range
private fun safeHsvColor(hue: Float, sat: Float, v: Float, alpha: Float = 1f): Color =
    Color.hsv(
        hue = hue.coerceIn(0f, 360f),
        saturation = sat.coerceIn(0f, 1f),
        value = v.coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f),
    )
```

### AeroNumberSpinner Wiring for TimePicker

```kotlin
// Source: AeroNumberSpinner.kt (Phase 7 / v1.x source — actual parameter names)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AeroNumberSpinner(
        value = hour,
        onValueChange = { h -> onValueChange(LocalTime(h, minute, if (showSeconds) second else 0)) },
        min = 0,
        max = 23,
        step = 1,
    )
    Text(":", style = AeroTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.CenterVertically))
    AeroNumberSpinner(
        value = minute,
        onValueChange = { m -> onValueChange(LocalTime(hour, m, if (showSeconds) second else 0)) },
        min = 0,
        max = 59,
        step = minuteStep,
    )
    if (showSeconds) {
        Text(":", ...)
        AeroNumberSpinner(
            value = second,
            onValueChange = { s -> onValueChange(LocalTime(hour, minute, s)) },
            min = 0,
            max = 59,
        )
    }
}
```

### AeroColorPicker Alpha Slider Background (checkerboard)

```kotlin
// Checkerboard backdrop for alpha slider — drawBehind custom pattern
Modifier.drawBehind {
    val tileSize = 8.dp.toPx()
    val tilesX = (size.width / tileSize).toInt() + 1
    val tilesY = (size.height / tileSize).toInt() + 1
    for (row in 0..tilesY) {
        for (col in 0..tilesX) {
            val color = if ((row + col) % 2 == 0) Color.White else Color.LightGray
            drawRect(color, topLeft = Offset(col * tileSize, row * tileSize), size = Size(tileSize, tileSize))
        }
    }
}
```

### DateRangePicker Range Highlight in AeroCalendarGrid

`AeroCalendarGrid` currently takes `selected: LocalDate?` (single date). DateRangePicker needs range highlight. The `isDisabled` predicate cannot provide range color — it only grays cells. Two options:
1. **Extend `AeroCalendarGrid` with optional `rangeStart: LocalDate?` + `rangeEnd: LocalDate?` parameters** (preferred) — the grid renders range-highlighted cells in `colors.primary.copy(alpha = 0.15f)` background. This is an internal API change; no public surface changes.
2. Build a duplicate grid just for range picker (anti-pattern — violates Don't Hand-Roll).

**Use option 1.** Phase 8 plan must include a task that adds `rangeStart/rangeEnd` parameters to `AeroCalendarGrid`. This is additive — default values are `null`, so DatePicker callers are unaffected.

---

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| Material3 `DatePicker` for desktop | Hand-rolled AeroCalendarGrid (Phase 7) | Material3 DatePicker crashes on CMP Desktop (Android-only internals) — hand-rolled is the only viable path |
| Material3 `RangeSlider` wrapper | Custom Canvas + `awaitPointerEventScope` | Material3 RangeSlider uses `detectDragGestures` → 18dp touchSlop → silent failure on Desktop |
| `AeroDropdownPopup` for calendar popups | `Popup(AeroCalendarPositionProvider(...))` | AeroDropdownPopup locks width to trigger width and caps height at 320dp — wrong for calendars |
| `java.time.LocalDate` in public API | `kotlinx.datetime.LocalDate` | kotlinx type is KMP-clean; delegates to java.time at JVM with zero overhead; consumers convert via `.toJavaLocalDate()` |
| `detectDragGestures` for Canvas drag | `awaitPointerEventScope` + manual loop | Desktop touchSlop=18dp makes detectDragGestures silently unusable for pixel-precise Canvas drag |
| Dual RGB+HSV state in ColorPicker | Single HSV float state; Color derived on emit | Eliminates round-trip drift (PITFALL-15) |

---

## Open Questions

1. **`AeroCalendarGrid` extension for range highlight**
   - What we know: current `AeroCalendarGrid` accepts only `selected: LocalDate?` (single date, confirmed from source); it does not support range-background rendering.
   - What's unclear: whether to add `rangeStart/rangeEnd` to the existing composable (cleanest) or build a separate `AeroCalendarRangeGrid` internal composable.
   - Recommendation: Add `rangeStart: LocalDate? = null` + `rangeEnd: LocalDate? = null` parameters to `AeroCalendarGrid`. Default null = backward-compatible with DatePicker. This is a Phase 8, Wave 1, Task A item (must happen before DateRangePicker).

2. **Animation sign-off gate**
   - What we know: animation direction is agreed in 08-CONTEXT.md (fade+scale ~120–150ms popup; soft ~80–100ms highlights); per project convention each animation needs explicit user sign-off before code.
   - Recommendation: planner should schedule a single animation sign-off touchpoint before the visual-polish plan/task, not block each component individually. Batch the four popup animations (DatePicker, TimePicker, DateTimePicker/DateRangePicker, ColorPickerButton) into one sign-off.

3. **ColorPicker preview bar — "before" color source**
   - What we know: PICK-06 specifies "before / after preview bar"; `AeroColorPicker` is controlled (`value: Color`, `onValueChange`).
   - What's unclear: in a controlled component, "before" is the `value` prop at the time the picker opened; "after" is the current internal HSV state. The "before" must be captured via `remember { value }` at popup-open time, not tracked as `value` changes.
   - Recommendation: `var beforeColor by remember { mutableStateOf(value) }` initialized once at composition (or popup-open for `AeroColorPickerButton`). For the inline `AeroColorPicker`, caller controls the before/after semantic.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | kotlin-test + JUnit Jupiter 5.10.0 |
| Config file | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`) |
| Quick run command | `./gradlew :library:test --tests "*.pickers.*" -x :showcase:*` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PICK-08 | RangeSlider: start ≤ end enforced; thumb overlap z-order | unit | `./gradlew :library:test --tests "*.range.AeroRangeSliderTest"` | ❌ Wave 1 |
| PICK-01 | DatePicker: `isDisabled` predicate applied to minDate/maxDate | unit | `./gradlew :library:test --tests "*.pickers.AeroDatePickerTest"` | ❌ Wave 1 |
| PICK-03 | TimePicker: LocalTime emitted with correct h/m/s; minuteStep clamping | unit | `./gradlew :library:test --tests "*.pickers.AeroTimePickerTest"` | ❌ Wave 1 |
| PICK-04 | DateTimePicker: `onValueChange` fires ONLY on Apply; pending state not emitted | unit | `./gradlew :library:test --tests "*.pickers.AeroDateTimePickerTest"` | ❌ Wave 1 |
| PICK-02 | DateRangePicker: `onRangeSelect` called exactly once per complete range; not on first click | unit | `./gradlew :library:test --tests "*.pickers.AeroDateRangePickerTest"` | ❌ Wave 1 |
| PICK-05 | ColorPicker: HSV round-trip preserves hue within tolerance (PITFALL-15 gate) | unit | `./gradlew :library:test --tests "*.color.AeroColorMathTest"` | ✅ already exists |
| PICK-06 | ColorPicker: swatch click sets correct Color value | unit | `./gradlew :library:test --tests "*.pickers.AeroColorPickerTest"` | ❌ Wave 1 |
| PICK-07 | ColorPicker: alpha disabled → emitted Color has alpha=1f | unit | included in AeroColorPickerTest | ❌ Wave 1 |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test --tests "*.pickers.*" --tests "*.range.*" -x :showcase:*`
- **Per wave merge:** `./gradlew :library:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `library/src/test/.../pickers/AeroDatePickerTest.kt` — covers PICK-01 (isDisabled, formatter, minDate/maxDate)
- [ ] `library/src/test/.../pickers/AeroTimePickerTest.kt` — covers PICK-03 (LocalTime emission, minuteStep, showSeconds)
- [ ] `library/src/test/.../pickers/AeroDateTimePickerTest.kt` — covers PICK-04 (Apply-only emission, Cancel no-op)
- [ ] `library/src/test/.../pickers/AeroDateRangePickerTest.kt` — covers PICK-02 (sealed state transitions, single callback per range)
- [ ] `library/src/test/.../pickers/AeroColorPickerTest.kt` — covers PICK-05/06/07 (swatch, alpha, round-trip)
- [ ] `library/src/test/.../range/AeroRangeSliderTest.kt` — covers PICK-08 (start≤end, step constraint)

Note: Composable rendering tests (visual layout, popup behavior) are manual-only for v2.0 per established project pattern. The test classes above test state logic, callback timing, and value emission — all testable without the Compose UI test harness.

---

## Sources

### Primary (HIGH confidence)

- `AeroCalendarGrid.kt` (source read) — actual parameter signature; `kotlinx.datetime.LocalDate.plus(1, DateTimeUnit.MONTH)` usage confirmed; `AeroCalendarGrid(displayMonth, selected, onDateSelected, onMonthChange, modifier, isDisabled)` exact API
- `AeroCalendarPositionProvider.kt` (source read) — `IntSize.Zero` first-frame guard confirmed at line 46; horizontal re-anchor without Top/Bottom flip confirmed
- `AeroColorMath.kt` (source read) — `rgbToHsv` returns `hPositive` always `[0f, 360f)`; `hexToRgb/hexToRgba/rgbToHex` confirmed; PITFALL-15 guard documented in KDoc
- `AeroHsvColorSquare.kt` (source read) — `.coerceIn(0f, 360f)` guard on hue confirmed; `awaitPointerEventScope` drag pattern confirmed; 256×256dp size confirmed
- `AeroHueSlider.kt` (source read) — 24dp × 256dp size; `coerceIn(0f, 360f)` guard; 7-stop gradient; `borderSelected` indicator line
- `AeroDragSplitter.kt` (source read) — `awaitPointerEventScope` + manual loop pattern locked; consume-on-delta-only semantics confirmed
- `AeroSlider.kt` (source read) — `SliderDefaults.colors()` pattern + tooltip glass pill + `MutableInteractionSource.collectIsDraggedAsState()`; single-slider wraps Material3 Slider
- `AeroNumberSpinner.kt` (source read) — `min/max/step` params confirmed; `remember(value)` text state key confirmed; scroll wheel support via `awaitEachGesture + PointerEventType.Scroll`
- `AeroDropdownPopup.kt` (source read) — two-layer background technique (`background(colors.background).background(colors.panelBackground)`) + `Modifier.shadow` usage confirmed (W11-02 reference)
- `library/build.gradle.kts` (source read) — `libs.kotlinx.datetime` in `dependencies { implementation(...) }` confirmed
- `gradle/libs.versions.toml` (source read) — `kotlinxDatetime = "0.6.2"` confirmed
- Git log (bash) — `feat(07-01): implement AeroCalendarGrid composable with kotlinx-datetime` confirms 0.6.2 compiles against Kotlin 2.1.21 with zero errors
- `AeroColorMathTest.kt` (source read) — `pureRedRoundTripPreservesHueWithinTolerance` test confirms PITFALL-15 "no drift" guarantee holds at `Color.hsv` boundary
- `AeroCalendarGridTest.kt` (source read) — month arithmetic, leap year, compile-smoke tests confirmed passing
- `central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime-jvm/0.6.2` — POM declares `kotlin-stdlib:1.9.21` compile dep; Kotlin backward-compat policy confirmed via Kotlin 2.1 compat guide

### Secondary (MEDIUM confidence)

- `developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Color.Companion` + androidx source search — `Color.hsv()` calls `requirePrecondition` with message `"HSV ($hue, $saturation, $value) must be in range (0..360, 0..1, 0..1)"` confirmed via web search of androidx source
- `kotlinlang.org/docs/compatibility-guide-21.html` — Kotlin 2.1 binary compatibility: only stdlib-common.jar removed and var/val visibility changes; kotlinx-datetime unaffected
- Official Compose animation docs (`developer.android.com/develop/ui/compose/animation/composables-modifiers`) — `AnimatedVisibility` + `fadeIn + scaleIn` pattern confirmed as stable API

### Tertiary (LOW confidence — for context only)

- Web search results on `DateRangePicker` sealed state patterns — no official source; pattern derived from PITFALL-06 (project-sourced) which is HIGH confidence

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions confirmed in build files; kotlinx-datetime compile confirmed by git history
- Architecture: HIGH — all primitives read from source; exact API signatures confirmed
- Pitfalls: HIGH (NEW-PICK-01 to NEW-PICK-05) — grounded in source reading; Color.hsv precondition verified via authoritative source search
- Validation: HIGH — test framework and existing test classes confirmed; new test file list is exhaustive

**Research date:** 2026-06-18
**Valid until:** 2026-09-18 (stable library — no fast-moving deps; only risk is a CMP version bump which is not planned)
