---
phase: 07-shared-internal-primitives
plan: 02
type: execute
wave: 2
depends_on: [07-01]
files_modified:
  - library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt
  - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt
  - library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt
  - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
autonomous: true
requirements: []

must_haves:
  truths:
    - "AeroHsvColorSquare and AeroHueSlider internal Canvas composables respond to drag on the first pixel of mouse movement (verified by drag-start smoke test using awaitPointerEventScope loop — PITFALL-03 touchSlop pattern is established and working)"
    - "AeroDragSplitter internal composable fires onDrag on first mouse movement in both horizontal and vertical orientations — the shared awaitPointerEventScope drag utility is confirmed working"
    - "AeroStepIndicator internal composable renders step dots with connecting lines and visually distinguishes current / completed / upcoming states across all three themes"
  artifacts:
    - path: "library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt"
      provides: "Modifier.aeroDragSplitter shared drag utility (awaitPointerEventScope loop, cursor change, 1D delta)"
      contains: "internal fun Modifier.aeroDragSplitter"
    - path: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt"
      provides: "Canvas-based saturation/value picker with first-pixel drag response"
      contains: "internal fun AeroHsvColorSquare"
    - path: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt"
      provides: "Canvas-based vertical hue strip with first-pixel drag response"
      contains: "internal fun AeroHueSlider"
    - path: "library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt"
      provides: "Horizontal step indicator (current/completed/upcoming dots + connector lines)"
      contains: "internal fun AeroStepIndicator"
    - path: "showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt"
      provides: "TEMPORARY (deleted in Phase 11) eyes-on verification of all 6 primitives"
      contains: "Phase7ScratchSection"
    - path: "showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt"
      provides: "Phase7ScratchSection() call wired into the section list (removed Phase 11)"
      contains: "Phase7ScratchSection"
  key_links:
    - from: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt"
      to: "Modifier.pointerInput { awaitPointerEventScope { ... } }"
      via: "PITFALL-03 drag pattern"
      pattern: "awaitPointerEventScope"
    - from: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt"
      to: "Modifier.pointerInput { awaitPointerEventScope { ... } }"
      via: "PITFALL-03 drag pattern"
      pattern: "awaitPointerEventScope"
    - from: "library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt"
      to: "Modifier.pointerInput { awaitPointerEventScope { ... } }"
      via: "PITFALL-03 drag pattern (shared utility consumers will reuse)"
      pattern: "awaitPointerEventScope"
    - from: "showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt"
      to: "AeroCalendarGrid + AeroHsvColorSquare + AeroHueSlider + AeroDragSplitter + AeroStepIndicator + AeroCalendarPositionProvider"
      via: "imports + composable invocations"
      pattern: "AeroCalendarGrid|AeroHsvColorSquare|AeroHueSlider|aeroDragSplitter|AeroStepIndicator"
    - from: "showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt"
      to: "Phase7ScratchSection"
      via: "import + invocation"
      pattern: "Phase7ScratchSection\\(\\)"
---

<objective>
Build the visual + drag primitives of Phase 7 (Modifier.aeroDragSplitter, AeroHsvColorSquare, AeroHueSlider, AeroStepIndicator) and wire all six Phase 7 primitives into a temporary `Phase7ScratchSection` in the showcase for eyes-on confirmation across all three themes.

Purpose: Establishes PITFALL-03 mitigation (`awaitPointerEventScope` + manual loop is THE drag pattern) as a reusable shared utility consumed by SplitPane (Phase 10) and DataTable column-resize (Phase 9). The HSV square + hue slider expose the same pattern at the Canvas level for ColorPicker (Phase 8). The step indicator is the visual contract `AeroStepperWizard` (Phase 10) renders. The scratch section gives the user a 5-minute eyes-on confirmation of all six Phase 7 primitives in one spot.

Output: 4 new internal source files + 1 temporary showcase section + ShowcaseApp.kt wiring. All primitives exercise via the scratch section's drag inputs and theme-switcher; the existing `:library:test` suite (untouched in this plan) remains green.
</objective>

<execution_context>
@C:/Users/1/.claude/get-shit-done/workflows/execute-plan.md
@C:/Users/1/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/07-shared-internal-primitives/07-CONTEXT.md
@.planning/phases/07-shared-internal-primitives/07-RESEARCH.md
@.planning/phases/07-shared-internal-primitives/07-VALIDATION.md
@.planning/phases/07-shared-internal-primitives/07-01-SUMMARY.md

# Existing reference files (read-only)
@library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt
@library/src/main/kotlin/com/mordred/aero/components/list/AeroBadge.kt
@library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt
@library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
@library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
@library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt
@showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
@showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt

# Plan-01 artifacts THIS plan consumes
@library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
@library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt
@library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt

<interfaces>
<!-- Locked API signatures the scratch section will invoke. -->

From Plan-01 (already committed before this plan runs):
```kotlin
// AeroColorMath - hue is degrees [0f, 360f]
internal fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float>
internal fun hexToRgb(hex: String): Triple<Int, Int, Int>?
internal fun rgbToHex(r: Int, g: Int, b: Int, alpha: Int? = null): String

// AeroCalendarGrid composable
@Composable
internal fun AeroCalendarGrid(
    displayMonth: LocalDate,
    selected: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: (LocalDate) -> Boolean = { false },
)

// AeroCalendarPositionProvider
internal class AeroCalendarPositionProvider(private val gap: Int = 4) : PopupPositionProvider
```

API contracts THIS plan creates:

```kotlin
// library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
package com.mordred.aero.components.internal.drag

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier

internal fun Modifier.aeroDragSplitter(
    orientation: Orientation,                  // Horizontal or Vertical
    onDrag: (deltaPx: Float) -> Unit,          // 1D delta along orientation axis
    onDragEnd: () -> Unit = {},
    enabled: Boolean = true,                   // when false, modifier is no-op
): Modifier
```

```kotlin
// library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt
@Composable
internal fun AeroHsvColorSquare(
    hue: Float,                  // [0f, 360f] — locked in Plan-01 ADR
    saturation: Float,           // [0f, 1f]
    value: Float,                // [0f, 1f]
    onSatValChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
// library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt
@Composable
internal fun AeroHueSlider(
    hue: Float,                  // [0f, 360f]
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
)
```

```kotlin
// library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt
@Composable
internal fun AeroStepIndicator(
    currentStep: Int,            // 0-based; 0 = first step is current
    totalSteps: Int,             // >= 1
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,  // null = non-interactive
)
```

From existing :library codebase (read-only):
- `com.mordred.aero.theme.AeroTheme.colors: AeroColorScheme` (read inside composable body)
- `colors.{primary, onPrimary, borderDefault, borderSelected, labelText, surface, panelBackground, onSurface, glassBorder, buttonHover}`
- `com.mordred.aero.icons.AeroIcons.{Check, CaretLeft, CaretRight, X}` (verified present in icons/internal/)
- `com.mordred.showcase.sections.ThemeSwitcher` (top of ShowcaseApp.kt — exists already; theme switching for the scratch section comes for free)
</interfaces>

<reference_skeletons>
The CONTEXT and RESEARCH files contain full code skeletons for AeroDragSplitter (07-RESEARCH.md §Pattern 1) and AeroHueSlider (07-RESEARCH.md §Pattern 4). The action sections below embed concrete signatures and key implementation details derived from those skeletons. Executors should follow the skeletons exactly for the drag pattern (PITFALL-03 mitigation), but can apply judgment for visual sizing within the locked decisions.
</reference_skeletons>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create Modifier.aeroDragSplitter (shared drag utility, PITFALL-03 mitigation)</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §Modifier.aeroDragSplitter API shape — locked: Modifier-based, NOT a Composable; `onDrag: (Float) -> Unit` 1D delta; cursor change; `enabled` gate)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Pattern 1 — full code skeleton; §Common Pitfalls §2 — `change.consume()` placement; §Anti-Patterns to Avoid)
    - library/src/main/kotlin/com/mordred/aero/components/navigation/ResizeHandles.kt (existing precedent for `pointerHoverIcon(PointerIcon(java.awt.Cursor(...)))`; NOTE: ResizeHandles uses detectDragGestures — that's the BANNED pattern for in-content drag; we deliberately diverge)
  </read_first>
  <behavior>
    - The Modifier accepts `orientation: Orientation` (Horizontal or Vertical), `onDrag: (deltaPx: Float) -> Unit`, `onDragEnd: () -> Unit = {}`, `enabled: Boolean = true`.
    - On hover (when `enabled`), cursor becomes `E_RESIZE_CURSOR` (Horizontal) or `N_RESIZE_CURSOR` (Vertical).
    - On mouse-down, the modifier opens an `awaitPointerEventScope` and enters a manual event loop.
    - On the FIRST mouse-move event after down (delta != 0), `onDrag(deltaPx)` fires immediately — no slop wait.
    - When `change.pressed` becomes false, `onDragEnd()` fires once and the inner loop exits; the outer loop awaits the next mouse-down.
    - `change.consume()` is called ONLY when we actually applied a drag (`pressed && delta != 0f`) — NOT on the up event (parents must see release).
    - When `enabled = false`, the modifier short-circuits and adds NO pointer input (consumer can disable at min/max bounds).
    - The cursor is `remember`ed keyed by `orientation` so a new AWT Cursor isn't allocated on every recomposition.
  </behavior>
  <action>
    Create directory + file `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` with this exact content:

    ```kotlin
    package com.mordred.aero.components.internal.drag

    import androidx.compose.foundation.gestures.Orientation
    import androidx.compose.runtime.remember
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.composed
    import androidx.compose.ui.input.pointer.PointerIcon
    import androidx.compose.ui.input.pointer.pointerHoverIcon
    import androidx.compose.ui.input.pointer.pointerInput
    import java.awt.Cursor

    /**
     * Modifier extension exposing `awaitPointerEventScope` drag with cursor change.
     *
     * **PITFALL-03 (touchSlop=18dp):** Compose Desktop's `detectDragGestures` and
     * `awaitDragOrCancellation` enforce an 18dp touchSlop before firing the first drag event,
     * which silently breaks Canvas-based mouse drag on Desktop (issue JetBrains/compose-jb #343,
     * unresolved as of CMP 1.7.3). This Modifier uses `awaitPointerEventScope` + manual
     * event loop — `onDrag` fires on the FIRST mouse-move event after pointer-down, not after
     * 18dp of accumulated movement. **`detectDragGestures` is BANNED for all Canvas drag in v2.0.**
     *
     * **Locked design** (per 07-CONTEXT.md):
     * - Modifier-based, NOT a content-slot Composable. Both consumers (AeroSplitPane Phase 10,
     *   AeroDataTable column-resize Phase 9) have very different visual treatments + hit-area
     *   geometry; a content-slot Composable would either grow arms or force one consumer to
     *   fight the API.
     * - `onDrag` receives `Float` (1D delta along the orientation axis), NOT `Offset`. Both
     *   consumers operate in 1D — orientation parameter selects the axis at composition time.
     * - The Modifier owns ONLY: drag loop + cursor + enabled gate. Consumers wrap with their
     *   own visual line, hit-area thickness, color, and shape.
     *
     * **Consume semantics:** `change.consume()` is called only when we actually applied a
     * drag delta (`pressed && delta != 0f`). The release event is NOT consumed — parents
     * (e.g. nested SplitPanes) must see the pointer release.
     *
     * @param orientation drag axis selector. Horizontal → reports `cur.x - prev.x`; Vertical → `cur.y - prev.y`.
     * @param onDrag fires on every mouse move with non-zero delta along the orientation axis.
     * @param onDragEnd fires once when `change.pressed` becomes false at the end of a drag sequence.
     * @param enabled when false, no pointer input is attached (consumer can disable at min/max bounds).
     */
    internal fun Modifier.aeroDragSplitter(
        orientation: Orientation,
        onDrag: (deltaPx: Float) -> Unit,
        onDragEnd: () -> Unit = {},
        enabled: Boolean = true,
    ): Modifier = composed {
        if (!enabled) return@composed this
        val cursor = remember(orientation) {
            when (orientation) {
                Orientation.Horizontal -> PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
                Orientation.Vertical   -> PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))
            }
        }
        this
            .pointerHoverIcon(cursor)
            .pointerInput(orientation, enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var prev = down.position
                        // Inner loop: process every event until pressed=false.
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
                                change.consume()  // only consume when we actually handled the move
                            }
                            prev = cur
                        }
                    }
                }
            }
    }
    ```

    Add the required Compose Foundation pointer import explicitly so the file compiles. Verify the file references `awaitPointerEventScope`, `awaitFirstDown`, and `awaitPointerEvent` — all from `androidx.compose.foundation.gestures` (which `pointerInput` block scope already provides; no extra import needed beyond `pointerInput`).

    Run `./gradlew :library:compileKotlin` to confirm the file compiles cleanly.
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` is true
    - `grep -q "internal fun Modifier.aeroDragSplitter" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "orientation: Orientation" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "onDrag: (deltaPx: Float) -> Unit" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "awaitFirstDown" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "E_RESIZE_CURSOR" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `grep -q "N_RESIZE_CURSOR" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` returns 0
    - `! grep -q "detectDragGestures" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` (banned API absent)
    - `./gradlew :library:compileKotlin` exits 0
  </acceptance_criteria>
  <done>Modifier.aeroDragSplitter exists with locked signature, uses awaitPointerEventScope + manual loop (no detectDragGestures), cursor change keyed to orientation, and `enabled` gate. The library compiles cleanly.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create AeroHsvColorSquare + AeroHueSlider Canvas composables</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt, library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §AeroHsvColorSquare + AeroHueSlider — locked dimensions, pattern, color rules; no glass styling on these primitives)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Pattern 4 — full AeroHueSlider code skeleton; §Common Pitfalls §1 — hue is degrees [0f, 360f])
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt (Plan-01 KDoc — confirms hue convention)
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroSlider.kt (visual reference — DO NOT instantiate AeroSlider inside the hue slider; independent Material3 Slider state would conflict)
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroBadge.kt (Color.Unspecified sentinel pattern; we don't need parameters here, just the precedent for reading AeroTheme.colors inside body)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt (token list — use `colors.borderSelected` for indicators)
  </read_first>
  <behavior>
    **AeroHsvColorSquare:**
    - Renders a 256dp × 256dp Canvas: horizontal axis = saturation `[0f, 1f]`, vertical axis = value `[1f at top, 0f at bottom]`.
    - Background draws `Brush.horizontalGradient(white → Color.hsv(hue, 1f, 1f))` then a `Brush.verticalGradient(Color.Transparent → Color.Black)` overlay.
    - 8dp circle indicator drawn at `(saturation * size.width, (1f - value) * size.height)`, white stroke 2dp, black inner stroke 1dp for contrast on any background.
    - On mouse down, fires `onSatValChange` immediately (click-to-set UX); on every drag move, fires `onSatValChange` with the new s/v.
    - Position-to-sat-val: `s = (x / width).coerceIn(0f, 1f)`, `v = (1f - y / height).coerceIn(0f, 1f)`.
    - Uses `awaitPointerEventScope` + manual loop (PITFALL-03 mitigation — no `detectDragGestures`).

    **AeroHueSlider:**
    - Renders a 24dp × 256dp vertical Canvas with a 7-stop `Brush.verticalGradient` (Red → Yellow → Green → Cyan → Blue → Magenta → Red).
    - 2dp horizontal indicator line at `y = (hue / 360f) * size.height`, color `colors.borderSelected`.
    - On mouse down, fires `onHueChange((y / height) * 360f)`; on every drag move, fires the same.
    - Uses `awaitPointerEventScope` + manual loop.
  </behavior>
  <action>
    Step A — Create file `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt`:

    ```kotlin
    package com.mordred.aero.components.pickers.internal.color

    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.layout.size
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.input.pointer.pointerInput
    import androidx.compose.ui.unit.dp
    import com.mordred.aero.theme.AeroTheme

    /**
     * Internal Canvas composable: 2D saturation/value picker for AeroColorPicker (Phase 8).
     *
     * **Hue convention:** `hue` is in DEGREES `[0f, 360f]` (Plan-01 ADR; matches `Color.hsv(...)`).
     *
     * **PITFALL-03 mitigation:** drag uses `awaitPointerEventScope` + manual loop. `detectDragGestures`
     * is BANNED for in-content Canvas drag. First mouse-down fires `onSatValChange` immediately
     * (click-to-set UX); every subsequent drag move fires `onSatValChange` with the new s/v.
     *
     * **Visual:** 256×256 logical Canvas. Horizontal axis = saturation `[0f, 1f]`, vertical = value
     * (top = 1f, bottom = 0f). Background = `Brush.horizontalGradient(white → Color.hsv(hue, 1f, 1f))`
     * with a `Brush.verticalGradient(transparent → black)` overlay. Indicator = 8dp circle at the
     * (sat, val) position with a white outer stroke and a thin black inner stroke for contrast on
     * any background.
     *
     * **No glass styling here** — AeroColorPicker (Phase 8) wraps this in its glass panel; per-element
     * glass would muddy the saturation gradient.
     */
    @Composable
    internal fun AeroHsvColorSquare(
        hue: Float,
        saturation: Float,
        value: Float,
        onSatValChange: (saturation: Float, value: Float) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val pureHueColor = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)

        Canvas(
            modifier = modifier
                .size(width = 256.dp, height = 256.dp)
                .pointerInput(hue) {
                    awaitPointerEventScope {
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Click-to-set: fire on first mouse-down, no drag wait.
                            val initS = (down.position.x / width).coerceIn(0f, 1f)
                            val initV = (1f - down.position.y / height).coerceIn(0f, 1f)
                            onSatValChange(initS, initV)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val s = (change.position.x / width).coerceIn(0f, 1f)
                                val v = (1f - change.position.y / height).coerceIn(0f, 1f)
                                onSatValChange(s, v)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            // Saturation gradient: white → pure hue.
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor),
                ),
            )
            // Value overlay: transparent → black (top to bottom).
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                ),
            )
            // Indicator: 8dp circle at (s * width, (1 - v) * height); double-stroke for contrast.
            val cx = saturation.coerceIn(0f, 1f) * size.width
            val cy = (1f - value.coerceIn(0f, 1f)) * size.height
            val radius = 8.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Color.Black,
                radius = radius - 2.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
    ```

    Step B — Create file `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt`:

    ```kotlin
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

    /**
     * Internal Canvas composable: vertical hue strip for AeroColorPicker (Phase 8).
     *
     * **Hue convention:** `hue` is DEGREES `[0f, 360f]`. The strip is laid out top→bottom = 0°→360°.
     *
     * **PITFALL-03 mitigation:** drag uses `awaitPointerEventScope` + manual loop. First mouse-down
     * fires `onHueChange` immediately; every drag move fires `onHueChange` with the new hue.
     *
     * **Visual:** 24dp × 256dp Canvas, 7-stop vertical gradient (Red → Yellow → Green → Cyan → Blue
     * → Magenta → Red). 2dp horizontal indicator line at `y = (hue / 360f) * size.height`, drawn in
     * `colors.borderSelected` for contrast across all three themes.
     */
    @Composable
    internal fun AeroHueSlider(
        hue: Float,
        onHueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val colors = AeroTheme.colors
        val gradient = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green,
                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                ),
            )
        }
        Canvas(
            modifier = modifier
                .size(width = 24.dp, height = 256.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        val height = size.height.toFloat()
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Click-to-set: fire on first mouse-down.
                            val initHue = ((down.position.y / height) * 360f).coerceIn(0f, 360f)
                            onHueChange(initHue)
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val h = ((change.position.y / height) * 360f).coerceIn(0f, 360f)
                                onHueChange(h)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            drawRect(brush = gradient)
            // Indicator: 2dp horizontal line at the current hue position.
            val y = (hue.coerceIn(0f, 360f) / 360f) * size.height
            drawLine(
                color = colors.borderSelected,
                start = Offset(0f, y),
                end   = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
    ```

    Step C — Compile both files: `./gradlew :library:compileKotlin`.
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` is true
    - `test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` is true
    - `grep -q "internal fun AeroHsvColorSquare" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` returns 0
    - `grep -q "internal fun AeroHueSlider" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` returns 0
    - `grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` returns 0 (PITFALL-03 pattern)
    - `grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` returns 0
    - `! grep -q "detectDragGestures" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` (banned API absent)
    - `! grep -q "detectDragGestures" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt`
    - `grep -q "Color.hsv(hue.coerceIn" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` returns 0 (uses Color.hsv with degrees range)
    - `grep -q "Brush.verticalGradient" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` returns 0
    - `./gradlew :library:compileKotlin` exits 0
  </acceptance_criteria>
  <done>AeroHsvColorSquare and AeroHueSlider exist with locked signatures, both use awaitPointerEventScope (no detectDragGestures), both fire callbacks on first mouse-down (click-to-set UX), and the library compiles cleanly.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Create AeroStepIndicator composable (current/completed/upcoming visual states)</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §AeroStepIndicator — locked horizontal-only; visual states locked: Current = filled primary; Completed = primary 0.6f alpha + AeroIcons.Check 12dp; Upcoming = outlined borderDefault)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Common Pitfalls §6 — AeroDark contrast on upcoming dots; mitigation: use `colors.labelText` or boost borderDefault alpha)
    - library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt (verify AeroIcons.Check is accessible; icons/internal/Check.kt is present)
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroBadge.kt (visual scale precedent — 24-28dp circles common across v1.x)
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt (verify tokens: primary, onPrimary, borderDefault, labelText)
  </read_first>
  <behavior>
    - Renders `totalSteps` horizontal step dots connected by lines.
    - Step `i` has visual state:
      - **Current** (`i == currentStep`): 24dp filled circle in `colors.primary`; step number `(i+1)` in `colors.onPrimary`.
      - **Completed** (`i < currentStep`): 24dp filled circle in `colors.primary.copy(alpha = 0.6f)`; `Icon(AeroIcons.Check)` 12dp tinted `colors.onPrimary`.
      - **Upcoming** (`i > currentStep`): 24dp outlined circle (1dp stroke, `colors.labelText` for AeroDark contrast — see RESEARCH §Pitfall §6); step number `(i+1)` in `colors.labelText`.
    - Connector lines between dots:
      - Between dots `i` and `i+1`: stroke color is `colors.primary` if `i < currentStep` (completed-side), else `colors.borderDefault` (upcoming-side).
      - Stroke thickness: 2dp.
    - When `onStepClick` is non-null, each dot is `clickable { onStepClick(i) }`. Default null = non-interactive.
    - Reads `AeroTheme.colors` and `AeroTheme.typography` inside the body.
  </behavior>
  <action>
    Create file `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt`:

    ```kotlin
    package com.mordred.aero.components.layout.internal.stepper

    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.weight
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.material3.Icon
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import com.mordred.aero.icons.AeroIcons
    import com.mordred.aero.theme.AeroTheme

    /**
     * Internal horizontal step indicator for AeroStepperWizard (Phase 10).
     *
     * **Locked horizontal-only** (per 07-CONTEXT.md). Vertical orientation deferred to v2.x if a
     * real consumer appears.
     *
     * **Visual states** (per ROADMAP success criterion #5):
     * - **Current** (`i == currentStep`): 24dp filled circle in `colors.primary`; step number
     *   `(i+1)` in `colors.onPrimary`.
     * - **Completed** (`i < currentStep`): 24dp filled circle in `colors.primary.copy(alpha = 0.6f)`;
     *   `Icon(AeroIcons.Check)` at 12dp tinted `colors.onPrimary`.
     * - **Upcoming** (`i > currentStep`): 24dp outlined circle, 1dp stroke in `colors.labelText`
     *   (chosen over `colors.borderDefault` for AeroDark contrast — `borderDefault` at 25% alpha on
     *   the AeroDark background reads as nearly invisible; `labelText` is fully opaque). Step
     *   number `(i+1)` in `colors.labelText`.
     *
     * **Connector lines:** 2dp stroke between dots. Color is `colors.primary` for the completed-side
     * (between dot `i` and `i+1` when `i < currentStep`), else `colors.borderDefault` for the
     * upcoming-side.
     *
     * **AeroDark contrast** is verified via Phase7ScratchSection's three-theme switch.
     *
     * @param currentStep 0-based; 0 = first step is current.
     * @param totalSteps must be >= 1.
     * @param onStepClick null = non-interactive (default for AeroStepperWizard).
     */
    @Composable
    internal fun AeroStepIndicator(
        currentStep: Int,
        totalSteps: Int,
        modifier: Modifier = Modifier,
        onStepClick: ((Int) -> Unit)? = null,
    ) {
        require(totalSteps >= 1) { "AeroStepIndicator requires totalSteps >= 1; got $totalSteps" }
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography

        Row(
            modifier = modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            for (i in 0 until totalSteps) {
                StepDot(
                    index = i,
                    currentStep = currentStep,
                    onClick = if (onStepClick != null) ({ onStepClick(i) }) else null,
                )
                if (i < totalSteps - 1) {
                    // Connector line; color depends on whether dot `i` is on the completed side.
                    val connectorColor = if (i < currentStep) colors.primary else colors.borderDefault
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(connectorColor),
                    )
                }
            }
        }
    }

    @Composable
    private fun StepDot(
        index: Int,
        currentStep: Int,
        onClick: (() -> Unit)?,
    ) {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography

        val state = when {
            index < currentStep  -> StepState.Completed
            index == currentStep -> StepState.Current
            else                 -> StepState.Upcoming
        }

        val baseModifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .let { if (onClick != null) it.clickable { onClick() } else it }

        when (state) {
            StepState.Current -> Box(
                modifier = baseModifier.background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = colors.onPrimary,
                    style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            StepState.Completed -> Box(
                modifier = baseModifier.background(colors.primary.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AeroIcons.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
            StepState.Upcoming -> Box(
                modifier = baseModifier
                    .border(width = 1.dp, color = colors.labelText, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = colors.labelText,
                    style = typography.caption,
                )
            }
        }
        // Avoid "unused" warning when no Spacer is needed between dot and connector.
        @Suppress("UNUSED_EXPRESSION") Spacer
    }

    private enum class StepState { Current, Completed, Upcoming }
    ```

    Run `./gradlew :library:compileKotlin` to confirm the file compiles cleanly.
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `test -f library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` is true
    - `grep -q "internal fun AeroStepIndicator" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "currentStep: Int" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "totalSteps: Int" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "onStepClick: ((Int) -> Unit)?" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "AeroIcons.Check" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "colors.primary" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "colors.onPrimary" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `grep -q "colors.labelText" library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` returns 0
    - `./gradlew :library:compileKotlin` exits 0
  </acceptance_criteria>
  <done>AeroStepIndicator exists with locked signature, renders 3 visual states (Current=primary fill, Completed=primary 0.6 alpha + Check icon, Upcoming=labelText outline), uses connector lines (primary for completed-side, borderDefault for upcoming-side), and the library compiles cleanly.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 4: Create Phase7ScratchSection in showcase + wire into ShowcaseApp.kt</name>
  <files>showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt, showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §Sign-off approach — locked: 4 scratch demos exercising AeroCalendarGrid, drag splitter (H+V), HSV+hue, StepIndicator, calendar position provider near right edge of 1024dp window)
    - .planning/phases/07-shared-internal-primitives/07-RESEARCH.md (§Validation Architecture — manual-only verifications via Phase7ScratchSection; SC3+SC4+SC5)
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt (current section list — Phase7ScratchSection() goes at the end, after NavigationSection())
    - showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt (existing section file pattern — `@Composable fun XxxSection()` with no required params; uses AeroTheme.colors / typography)
  </read_first>
  <behavior>
    - Phase7ScratchSection is a `@Composable fun Phase7ScratchSection()` taking no required parameters.
    - It renders 5 sub-sections in a vertical Column:
      1. **AeroCalendarGrid demo:** stateful month state; clicking prev/next changes month; clicking a day updates `selected` state and shows the selected date as Text below.
      2. **AeroHsvColorSquare + AeroHueSlider demo:** stateful HSV (h, s, v) floats; the two primitives sit side-by-side; below them, a 64×32dp Box previews `Color.hsv(h, s, v)` so drag effects are visible.
      3. **Modifier.aeroDragSplitter demo (horizontal + vertical):** two demo boxes — a horizontal splitter (1dp tall visual line in a 16dp tall hit-area Box) that updates a stateful `Float` X-position; a vertical splitter that updates a stateful `Float` Y-position. Each box shows the current px value as Text.
      4. **AeroStepIndicator demo:** stateful `currentStep` Int; "Prev" and "Next" buttons advance/retreat; renders 4 steps (totalSteps=4) — exercises Current/Completed/Upcoming across the theme switcher.
      5. **AeroCalendarPositionProvider demo:** a Button positioned near the right edge of a 1024dp wide Box (`Modifier.width(1024.dp)`) that toggles a `Popup(popupPositionProvider = AeroCalendarPositionProvider())` containing an AeroCalendarGrid — confirms wide popup right-aligns without clip.
    - ShowcaseApp.kt has `Phase7ScratchSection()` added after `NavigationSection()` and an import added at the top.
  </behavior>
  <action>
    Step A — Create file `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`:

    ```kotlin
    package com.mordred.showcase.sections

    import androidx.compose.foundation.background
    import androidx.compose.foundation.gestures.Orientation
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.material3.Button
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.window.Popup
    import androidx.compose.ui.window.PopupProperties
    import com.mordred.aero.components.internal.drag.aeroDragSplitter
    import com.mordred.aero.components.internal.popup.AeroCalendarPositionProvider
    import com.mordred.aero.components.layout.internal.stepper.AeroStepIndicator
    import com.mordred.aero.components.pickers.internal.calendar.AeroCalendarGrid
    import com.mordred.aero.components.pickers.internal.color.AeroHsvColorSquare
    import com.mordred.aero.components.pickers.internal.color.AeroHueSlider
    import com.mordred.aero.theme.AeroTheme
    import kotlinx.datetime.LocalDate

    /**
     * **TEMPORARY — deleted in Phase 11.**
     *
     * Eyes-on confirmation surface for all 6 Phase 7 internal primitives:
     * - AeroCalendarGrid (logic + rendering)
     * - AeroHsvColorSquare + AeroHueSlider (Canvas drag, PITFALL-03 mitigation)
     * - Modifier.aeroDragSplitter (horizontal + vertical drag)
     * - AeroStepIndicator (current/completed/upcoming across all 3 themes)
     * - AeroCalendarPositionProvider (wide popup near right edge of 1024dp scratch frame)
     *
     * Phase 11 cleanup: delete this file and remove the `Phase7ScratchSection()` call + import
     * from `ShowcaseApp.kt`.
     */
    @Composable
    fun Phase7ScratchSection() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "Phase 7 Scratch (TEMPORARY — deleted Phase 11)",
                color = colors.onBackground,
                style = typography.title.copy(fontWeight = FontWeight.SemiBold),
            )

            // 1. AeroCalendarGrid demo
            CalendarGridDemo()

            // 2. HSV square + hue slider demo
            HsvDemo()

            // 3. aeroDragSplitter demo (horizontal + vertical)
            DragSplitterDemo()

            // 4. Step indicator demo
            StepIndicatorDemo()

            // 5. AeroCalendarPositionProvider — wide popup near right edge of 1024dp frame
            CalendarPopupDemo()
        }
    }

    @Composable
    private fun CalendarGridDemo() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        var displayMonth by remember { mutableStateOf(LocalDate(2026, 4, 1)) }
        var selected by remember { mutableStateOf<LocalDate?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AeroCalendarGrid", color = colors.onBackground, style = typography.body)
            Box(
                modifier = Modifier
                    .background(colors.panelBackground)
                    .padding(8.dp),
            ) {
                AeroCalendarGrid(
                    displayMonth = displayMonth,
                    selected = selected,
                    onDateSelected = { selected = it },
                    onMonthChange = { displayMonth = it },
                )
            }
            Text(
                text = "Selected: ${selected ?: "(none)"}",
                color = colors.labelText,
                style = typography.caption,
            )
        }
    }

    @Composable
    private fun HsvDemo() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        var hue by remember { mutableStateOf(0f) }              // [0f, 360f]
        var saturation by remember { mutableStateOf(1f) }       // [0f, 1f]
        var value by remember { mutableStateOf(1f) }            // [0f, 1f]
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AeroHsvColorSquare + AeroHueSlider", color = colors.onBackground, style = typography.body)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AeroHsvColorSquare(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSatValChange = { s, v -> saturation = s; value = v },
                )
                AeroHueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                )
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 32.dp)
                        .background(Color.hsv(hue.coerceIn(0f, 360f), saturation, value)),
                )
            }
            Text(
                text = "h=${"%.1f".format(hue)} s=${"%.2f".format(saturation)} v=${"%.2f".format(value)}",
                color = colors.labelText,
                style = typography.caption,
            )
        }
    }

    @Composable
    private fun DragSplitterDemo() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        var hPos by remember { mutableStateOf(0f) }
        var vPos by remember { mutableStateOf(0f) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Modifier.aeroDragSplitter", color = colors.onBackground, style = typography.body)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Horizontal splitter demo
                Column {
                    Text("Horizontal", color = colors.labelText, style = typography.caption)
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(16.dp)
                            .background(colors.surface)
                            .aeroDragSplitter(
                                orientation = Orientation.Horizontal,
                                onDrag = { delta -> hPos += delta },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.width(160.dp).height(1.dp).background(colors.borderDefault))
                    }
                    Text(
                        text = "hPos = ${"%.1f".format(hPos)}px",
                        color = colors.labelText,
                        style = typography.caption,
                    )
                }
                // Vertical splitter demo
                Column {
                    Text("Vertical", color = colors.labelText, style = typography.caption)
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(80.dp)
                            .background(colors.surface)
                            .aeroDragSplitter(
                                orientation = Orientation.Vertical,
                                onDrag = { delta -> vPos += delta },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.width(1.dp).height(80.dp).background(colors.borderDefault))
                    }
                    Text(
                        text = "vPos = ${"%.1f".format(vPos)}px",
                        color = colors.labelText,
                        style = typography.caption,
                    )
                }
            }
        }
    }

    @Composable
    private fun StepIndicatorDemo() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        var currentStep by remember { mutableStateOf(0) }
        val totalSteps = 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AeroStepIndicator (toggle theme to verify all 3)", color = colors.onBackground, style = typography.body)
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                AeroStepIndicator(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (currentStep > 0) currentStep-- },
                    enabled = currentStep > 0,
                ) { Text("Prev") }
                Button(
                    onClick = { if (currentStep < totalSteps - 1) currentStep++ },
                    enabled = currentStep < totalSteps - 1,
                ) { Text("Next") }
                Text(
                    text = "step ${currentStep + 1} / $totalSteps",
                    color = colors.labelText,
                    style = typography.caption,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    @Composable
    private fun CalendarPopupDemo() {
        val colors = AeroTheme.colors
        val typography = AeroTheme.typography
        var open by remember { mutableStateOf(false) }
        var displayMonth by remember { mutableStateOf(LocalDate(2026, 4, 1)) }
        var selected by remember { mutableStateOf<LocalDate?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "AeroCalendarPositionProvider — trigger near right edge of 1024dp frame",
                color = colors.onBackground,
                style = typography.body,
            )
            // 1024dp scratch frame; trigger button placed near right edge so popup must right-align.
            Box(
                modifier = Modifier
                    .width(1024.dp)
                    .height(64.dp)
                    .background(colors.surface),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Button(
                    onClick = { open = !open },
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text(if (open) "Close calendar" else "Open calendar")
                }
                if (open) {
                    Popup(
                        popupPositionProvider = AeroCalendarPositionProvider(),
                        onDismissRequest = { open = false },
                        properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(colors.panelBackground)
                                .padding(8.dp),
                        ) {
                            AeroCalendarGrid(
                                displayMonth = displayMonth,
                                selected = selected,
                                onDateSelected = { selected = it; open = false },
                                onMonthChange = { displayMonth = it },
                            )
                        }
                    }
                }
            }
            Text(
                text = "selected (popup): ${selected ?: "(none)"}",
                color = colors.labelText,
                style = typography.caption,
            )
        }
    }
    ```

    Step B — Edit `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt`. Add the import alphabetically among the existing `com.mordred.showcase.sections.*` imports (between `OverlaysSection` and `RangeSection` lines ~31-32):
    ```kotlin
    import com.mordred.showcase.sections.Phase7ScratchSection
    ```

    Then add the call after the existing `NavigationSection()` line (currently line 93). The new line should read:
    ```kotlin
                Phase7ScratchSection()
    ```
    Place it on its own line BETWEEN `NavigationSection()` and the existing `Spacer(Modifier.height(24.dp))` line. Final region of the Column (lines 91-95) should look like:
    ```kotlin
                ContainersSection()
                OverlaysSection(toastState = toastState)
                NavigationSection()

                Phase7ScratchSection()

                Spacer(Modifier.height(24.dp))
    ```

    Step C — Compile both modules to confirm wiring is valid:
    ```bash
    ./gradlew :library:compileKotlin :showcase:compileKotlin
    ```
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin :showcase:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `test -f showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` is true
    - `grep -q "fun Phase7ScratchSection" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "AeroCalendarGrid" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "AeroHsvColorSquare" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "AeroHueSlider" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "aeroDragSplitter" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "AeroStepIndicator" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q "AeroCalendarPositionProvider" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0
    - `grep -q ".width(1024.dp)" showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` returns 0 (1024dp scratch frame for the position-provider demo)
    - `grep -q "import com.mordred.showcase.sections.Phase7ScratchSection" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` returns 0
    - `grep -q "Phase7ScratchSection()" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` returns 0
    - `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0
    - `./gradlew :library:test` exits 0 (no regressions in Plan-01 tests)
  </acceptance_criteria>
  <done>Phase7ScratchSection exists with 5 demos (CalendarGrid, HSV square+slider, drag splitter H+V, step indicator, calendar position-provider on 1024dp frame), is wired into ShowcaseApp.kt after NavigationSection, both library and showcase modules compile cleanly, and Plan-01 tests still pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 5: Run final library test suite + showcase compile gate (automated sign-off)</name>
  <files>(verification only — no files modified by this task)</files>
  <read_first>
    - .planning/phases/07-shared-internal-primitives/07-VALIDATION.md (§Manual-Only Verifications — documents that SC3 + SC4 drag tests and SC5 three-theme visual are manual-only since no Compose UI test framework is wired in Phase 7)
    - .planning/phases/07-shared-internal-primitives/07-CONTEXT.md (§decisions §Sign-off approach — "Keep both plans `autonomous: true` — pure internal infra with unit tests, no UAT required between waves")
  </read_first>
  <behavior>
    - `./gradlew :library:test` exits 0 — Plan-01's 3 test classes (AeroColorMathTest, AeroCalendarPositionProviderTest, AeroCalendarGridTest) plus all pre-existing `:library` tests are still green.
    - `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0 — both modules build with the new files.
    - The grep gates for PITFALL-03 mitigation pass: no `detectDragGestures` in any new in-content drag/Canvas file; `awaitPointerEventScope` present in all 3 drag-bearing primitives.
    - explicitApi() compliance: no `public` symbols leaked from Phase 7 internal files.

    Manual eyes-on verification of SC3, SC4, SC5 (drag responsiveness + 3-theme step-indicator readability) is documented in this plan's `<post_execution_manual_check>` block below; the user runs `./gradlew :showcase:run` after plan completion and verifies eyes-on. This is captured in 07-VALIDATION.md "Manual-Only Verifications" table.
  </behavior>
  <action>
    Step A — Run the full library test suite. From project root:
    ```bash
    ./gradlew :library:test
    ```
    Must exit 0. This confirms Plan-01's tests still pass after Plan-02's source additions (Plan-02 doesn't add tests but adds source files that compile-link with Plan-01's test classpath).

    Step B — Compile both modules:
    ```bash
    ./gradlew :library:compileKotlin :showcase:compileKotlin
    ```
    Must exit 0. Validates that the scratch section's imports of all 6 Phase 7 primitives resolve.

    Step C — PITFALL-03 grep gate (no detectDragGestures in Phase 7 in-content drag files):
    ```bash
    ! grep -r "detectDragGestures" \
        library/src/main/kotlin/com/mordred/aero/components/internal/drag/ \
        library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/
    ```
    Must succeed (the `!` inverts: grep returns 1 = "not found", which becomes exit 0 after `!`). If grep finds `detectDragGestures` in any of those directories, fail and re-edit the offending file to use `awaitPointerEventScope` instead.

    Step D — explicitApi() check on Phase 7 files (must all be `internal`, no public surface added):
    ```bash
    ! grep -E "^public " \
        library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt \
        library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt \
        library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt \
        library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt
    ```
    Must succeed (no `public ` prefix at line start). All Phase 7 declarations are `internal`.

    Step E — W11-01 grep gate (Popup(...) only; Dialog(transparent=true / undecorated=true) BANNED in all new Phase 7 sources, per CONTEXT.md §Carry-forward rules and ROADMAP §Phase 7 phase-note 4):
    ```bash
    ! grep -rE 'transparent\s*=\s*true|undecorated\s*=\s*true' \
        library/src/main/kotlin/com/mordred/aero/components/internal/ \
        library/src/main/kotlin/com/mordred/aero/components/pickers/internal/ \
        library/src/main/kotlin/com/mordred/aero/components/layout/internal/ \
        showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt
    ```
    Must succeed (the `!` inverts: grep returns 1 = "not found" → exit 0 after `!`). If grep finds `transparent=true` or `undecorated=true` in any of those paths, fail and re-edit the offending file to use `Popup(...)` instead — never `Dialog(undecorated=true, transparent=true)` (W11-01, locked since v1.0; tracked in JetBrains/compose-multiplatform #3757 — Win11 access-violation crash).
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:test :library:compileKotlin :showcase:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `./gradlew :library:test` exits 0 (Plan-01 test classes still green: AeroColorMathTest 15 tests, AeroCalendarPositionProviderTest 4 tests, AeroCalendarGridTest 7 tests)
    - `./gradlew :library:compileKotlin` exits 0
    - `./gradlew :showcase:compileKotlin` exits 0
    - `! grep -r "detectDragGestures" library/src/main/kotlin/com/mordred/aero/components/internal/drag/ library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/` exits 0 (PITFALL-03 grep gate passes)
    - `! grep -E "^public " library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` exits 0 (no public API leaked)
    - `! grep -rE 'transparent\s*=\s*true|undecorated\s*=\s*true' library/src/main/kotlin/com/mordred/aero/components/internal/ library/src/main/kotlin/com/mordred/aero/components/pickers/internal/ library/src/main/kotlin/com/mordred/aero/components/layout/internal/ showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` exits 0 (W11-01 grep gate passes — no banned `transparent=true` / `undecorated=true` in any new Phase 7 source)
  </acceptance_criteria>
  <done>The full :library test suite is green, both library and showcase modules compile, the PITFALL-03 grep gate passes, and no public API has leaked from Phase 7 internal files. The plan's automated sign-off is complete; the user performs SC3/SC4/SC5 eyes-on verification per the post_execution_manual_check below as a separate step before /gsd:verify-work.</done>
</task>

</tasks>

<post_execution_manual_check>
After plan-02 tasks complete and the automated sign-off (Task 5) is green, the user runs the showcase and verifies SC3, SC4, SC5 eyes-on per the 07-VALIDATION.md "Manual-Only Verifications" table. This is NOT a blocking task in the autonomous plan — it is a recommended human check before `/gsd:verify-work`.

**Run the showcase:**
```bash
./gradlew :showcase:run
```

Scroll to the bottom — section titled "Phase 7 Scratch (TEMPORARY — deleted Phase 11)" with 5 sub-demos.

**SC3 — HSV / hue first-pixel drag (07-VALIDATION.md):**
- Click anywhere inside the HSV square. The S/V text below updates on the click (no slop wait).
- Press and drag inside the HSV square slowly by 1-2 pixels. The S/V text updates on the FIRST move event (no "dead zone" before the first update).
- Repeat for the hue slider: click changes hue immediately; drag updates on first move.
- The 64x32 preview Box reflects the current Color.hsv(h, s, v).

**SC4 — Modifier.aeroDragSplitter first-pixel drag (horizontal + vertical):**
- Press inside the horizontal splitter Box and drag horizontally by 1-2 pixels. "hPos = X.X px" text updates IMMEDIATELY on the first move. Cursor is E_RESIZE_CURSOR (horizontal arrows) on hover.
- Press inside the vertical splitter Box and drag vertically by 1-2 pixels. "vPos = X.X px" text updates on first move. Cursor is N_RESIZE_CURSOR (vertical arrows) on hover.

**SC5 — AeroStepIndicator across 3 themes:**
- In the AeroStepIndicator demo, click "Next" twice so currentStep = 2 of 4. You should see:
  - Step 1: completed (primary 0.6 alpha + checkmark icon)
  - Step 2: completed
  - Step 3: current (full primary fill + step number "3" in onPrimary)
  - Step 4: upcoming (outlined circle with labelText border + step number "4" in labelText)
  - Connector lines: primary between steps 1-2 and 2-3 (completed-side); borderDefault between 3-4 (upcoming-side).
- Click the ThemeSwitcher to switch to **AeroDark**. Confirm:
  - All 4 dots remain visually distinct.
  - Upcoming dot (step 4) is readable — NOT washed out / invisible (PITFALL-09 / RESEARCH §Pitfall §6).
- Switch to **Classic**. Confirm same — all 4 states distinguishable.
- Switch back to **AeroBlue**.

**AeroCalendarGrid + AeroCalendarPositionProvider eyes-on:**
- In the AeroCalendarGrid demo, click prev/next month arrows — month/year text updates.
- Click a day — "Selected: YYYY-MM-DD" text reflects the click.
- In the "AeroCalendarPositionProvider" demo: click "Open calendar" — popup AeroCalendarGrid appears right-aligned to the button (NOT clipped at the right edge of the 1024dp frame).

If any check fails, capture the specific scenario + theme + observed-vs-expected, and surface as a Phase 7 verification gap for .
</post_execution_manual_check>

<verification>
After all tasks complete:

```bash
# 1. Library compiles and existing tests still pass
./gradlew :library:test
./gradlew :library:compileKotlin

# 2. Showcase compiles
./gradlew :showcase:compileKotlin

# 3. New files exist at locked paths
test -f library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt
test -f library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt
test -f library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt
test -f showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt

# 4. detectDragGestures is BANNED from Phase 7 internal drag/Canvas primitives (PITFALL-03)
! grep -r "detectDragGestures" \
    library/src/main/kotlin/com/mordred/aero/components/internal/drag/ \
    library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/

# 5. awaitPointerEventScope is present in all 3 drag-bearing primitives
grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt
grep -q "awaitPointerEventScope" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt

# 6. Phase7ScratchSection is wired into ShowcaseApp.kt
grep -q "import com.mordred.showcase.sections.Phase7ScratchSection" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
grep -q "Phase7ScratchSection()" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

# 7. No public API leaked from new library files (every fun/class is `internal`)
! grep -E "^public " \
    library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt \
    library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt \
    library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt \
    library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt

# 8. Manual checkpoint approved (Task 5 gate)
```
</verification>

<success_criteria>
- Modifier.aeroDragSplitter exists with locked signature: `(orientation: Orientation, onDrag: (Float) -> Unit, onDragEnd: () -> Unit, enabled: Boolean): Modifier` — uses awaitPointerEventScope manual loop, cursor change keyed by orientation, enabled gate
- AeroHsvColorSquare and AeroHueSlider Canvas composables exist; both fire callbacks on first mouse-down (click-to-set UX) and on first drag move (PITFALL-03 mitigation)
- AeroStepIndicator exists with locked signature; renders 3 visual states (Current=primary fill, Completed=primary 0.6 alpha + AeroIcons.Check, Upcoming=labelText outline) plus connector lines (primary for completed-side, borderDefault for upcoming-side)
- Phase7ScratchSection exists with 5 demos covering all 6 Phase 7 primitives + the AeroCalendarPositionProvider 1024dp wide-popup case
- ShowcaseApp.kt has `Phase7ScratchSection()` call + import added after NavigationSection()
- No `detectDragGestures` usage in any Phase 7 internal drag/Canvas file (verified by grep)
- All new symbols are `internal`; no `public` API surface added
- `./gradlew :library:test` exits 0 (Plan-01 tests still green)
- `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0
- Manual checkpoint (Task 5) approved by user — SC3, SC4, SC5 verified eyes-on across all 3 themes
</success_criteria>

<output>
After completion, create `.planning/phases/07-shared-internal-primitives/07-02-SUMMARY.md` describing:
- File-path manifest for all 4 new library sources + scratch section + ShowcaseApp.kt change
- PITFALL-03 mitigation status: confirmed via 3 awaitPointerEventScope-bearing files (AeroDragSplitter, AeroHsvColorSquare, AeroHueSlider) — all `detectDragGestures`-free
- Manual checkpoint outcome: list any issues raised by the user during Task 5; if all 7 verification steps passed, note "all 5 success criteria green across AeroBlue / AeroDark / Classic"
- Phase 7 readiness for Phase 8 / 9 / 10 consumption:
  - Phase 8 ColorPicker can import AeroColorMath + AeroHsvColorSquare + AeroHueSlider; date pickers can import AeroCalendarGrid + AeroCalendarPositionProvider
  - Phase 9 DataTable column resize can `Modifier.aeroDragSplitter(orientation = Horizontal, ...)`
  - Phase 10 SplitPane can use the same; AeroStepperWizard can import AeroStepIndicator
- Phase 11 cleanup todo: "delete Phase7ScratchSection.kt + remove call/import from ShowcaseApp.kt"
</output>
