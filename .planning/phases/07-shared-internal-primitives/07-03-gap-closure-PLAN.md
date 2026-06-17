---
phase: 07-shared-internal-primitives
plan: 03
type: execute
wave: 1
depends_on: [07-01, 07-02]
gap_closure: true
autonomous: true
requirements: []
files_modified:
  - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
  - library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt

must_haves:
  truths:
    - "AeroCalendarGrid renders a visually symmetric month grid — the month-selector header (prev / month-name / next) is the same width as the 7-column day grid, not stretched to fill its parent (gap-01)."
    - "Existing 27 Phase 7 unit tests (AeroCalendarGridTest 7 + AeroColorMathTest 16 + AeroCalendarPositionProviderTest 4) remain green after the AeroCalendarGrid layout change."
    - "In the scratch demo, the AeroStepIndicator block (indicator + Prev/Next) sits on an Aero glass surface, accurately representing how AeroStepperWizard (Phase 10) will host it — unblocks the 'StepIndicator three-state contrast across all three themes' UAT truth (gap-02)."
    - "In the scratch demo, the AeroCalendarGrid inside the wide-popup body sits on an Aero glass surface, representing how AeroDatePicker (Phase 8) will host it — unblocks the 'CalendarPositionProvider wide-popup visually clean on glass' UAT truth (gap-03)."
    - "The scratch demo uses AeroButton / AeroOutlinedButton (not raw Material Button) for Prev/Next and the Open-calendar trigger, each wrapped so it sits at intrinsic width and does not stretch to full container width (gap-04)."
  artifacts:
    - path: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
      provides: "Month-grid primitive whose header width matches the day-grid width (header no longer fillMaxWidth)"
      contains: "internal fun AeroCalendarGrid"
    - path: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
      provides: "Scratch demo with glass-wrapped StepIndicator + glass-wrapped calendar popup body + AeroButton/AeroOutlinedButton triggers at intrinsic width"
      contains: "AeroPhase7Scratch"
  key_links:
    - from: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
      to: "header Row width == day-grid width"
      via: "outer Column wrapContentWidth + header/day Rows no longer fillMaxWidth"
      pattern: "wrapContentWidth"
    - from: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
      to: "AeroCard glass container"
      via: "import com.mordred.aero.components.containers.AeroCard wrapping StepIndicator block + calendar popup body"
      pattern: "AeroCard"
    - from: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
      to: "AeroButton + AeroOutlinedButton"
      via: "import com.mordred.aero.components.buttons.{AeroButton, AeroOutlinedButton}"
      pattern: "AeroButton|AeroOutlinedButton"
---

<objective>
Close the 4 UAT gaps recorded in `07-VERIFICATION.md` for Phase 7 — and nothing more. One primitive-layout fix (gap-01: AeroCalendarGrid header stretches past the day grid) and three scratch-demo presentation fixes (gap-02: StepIndicator demo lacks glass; gap-03: calendar-popup demo body lacks glass; gap-04: raw Material Buttons that stretch full-width).

Purpose: Unblock the two UAT truths that are currently `blocked_by_gaps` (AeroStepIndicator three-theme contrast; AeroCalendarPositionProvider wide-popup visual confirmation) and fix the one BLOCKING primitive layout bug so the calendar renders symmetrically for every Phase 8 date-picker consumer.

Output: 2 modified files. NO new files, NO new primitives, NO changes to any primitive other than AeroCalendarGrid's header layout. Architecture decision B stays locked: primitives are surface-less — glass is added ONLY in the scratch demo wrapper (gaps 02/03), never inside a primitive.
</objective>

<execution_context>
@C:/Users/1/.claude/get-shit-done/workflows/execute-plan.md
@C:/Users/1/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/phases/07-shared-internal-primitives/07-CONTEXT.md
@.planning/phases/07-shared-internal-primitives/07-VERIFICATION.md
@.planning/phases/07-shared-internal-primitives/07-02-SUMMARY.md

# Files this plan modifies (CURRENT STATE — read before editing)
@library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt
@library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt

<interfaces>
<!-- CONCRETE composable APIs the executor uses. "AeroSurface (glass variant)" from the gap
     text does NOT exist in this codebase — the real glass container is AeroCard. -->

GLASS CONTAINER (resolves the gaps' "AeroSurface (glass variant)" placeholder):
```kotlin
// com.mordred.aero.components.containers.AeroCard — PUBLIC, callable from :library scratch file.
// Renders Modifier.glassEffect (single drawBehind pass: vertical gradient fill + 1dp glassBorder +
// shadow). This is the card-level glass surface — exactly what AeroDatePicker / AeroStepperWizard
// will host these primitives in.
@Composable
public fun AeroCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp,
    padding: Dp = 16.dp,        // inner content padding (default 16.dp)
    content: @Composable () -> Unit,
)
```

AERO BUTTONS (resolve gap-04 — replace raw Material Button / OutlinedButton):
```kotlin
// com.mordred.aero.components.buttons.AeroButton — text + onClick, NOT a content slot.
@Composable
public fun AeroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 30.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)

// com.mordred.aero.components.buttons.AeroOutlinedButton — same signature shape, height default 28.dp.
@Composable
public fun AeroOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 28.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

GRID WIDTH FACTS (for gap-01):
- Day cells are fixed `Modifier.size(36.dp)` (DayCell + day-of-week header Boxes + blank slots), 7 per row → intrinsic day-grid width = 7 × 36dp = 252dp.
- The header Row currently uses `Modifier.fillMaxWidth()` + `Arrangement.SpaceBetween`, so in a wide parent the prev/label/next spread to the full parent width while day rows stay packed at 252dp left-aligned. THIS is the asymmetry.
- The day-of-week Row and the 6 day Rows also currently use `Modifier.fillMaxWidth()`, but because their children are fixed 36dp with no Arrangement spacing they pack left at 252dp regardless. They render fine today, but `fillMaxWidth()` on them is what lets the header "see" a wide parent.

UNCHANGED primitive contract: AeroCalendarGrid signature, day-of-week labels, DayCell visuals, daysInMonth helper, kotlinx-datetime arithmetic — all stay exactly as-is. Only the width-constraint modifiers on the outer Column / header Row change.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Fix AeroCalendarGrid header stretching past the day grid (gap-01, BLOCKING)</name>
  <files>library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt</files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt (CURRENT STATE — the file being edited; note header Row at the `Arrangement.SpaceBetween` block uses `Modifier.fillMaxWidth()`, and the day-of-week Row + the 6 day Rows also use `Modifier.fillMaxWidth()`; cells are fixed 36.dp)
    - .planning/phases/07-shared-internal-primitives/07-VERIFICATION.md (gap-01 finding + fix_intent: header width must MATCH the day-grid width; day grid layout itself is correct — only the header must stop stretching)
    - library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt (the 7 tests that MUST stay green — confirm none assert on layout width before changing)
  </read_first>
  <behavior>
    - After the change, when AeroCalendarGrid is placed in a parent WIDER than 252dp, the month-selector header (prev / month-year label / next) spans only the 252dp day-grid width — prev sits at the left edge of the grid, next at the right edge of the grid — NOT at the far edges of the wide parent.
    - The 6×7 day grid layout is UNCHANGED — cells stay 36dp, rows stay packed, selection/disabled/today rendering unchanged.
    - The day-of-week header (Mo..Su) stays aligned directly above its day columns.
    - All 7 existing AeroCalendarGridTest tests stay green (the change is layout-only; no logic touched).
  </behavior>
  <action>
    Make the whole grid size to its intrinsic 252dp width and stop the header from seeing the wide parent. Two concrete edits in `AeroCalendarGrid.kt`:

    EDIT 1 — outer Column: change
    ```kotlin
    Column(modifier = modifier.padding(8.dp)) {
    ```
    to
    ```kotlin
    Column(modifier = modifier.wrapContentWidth().padding(8.dp)) {
    ```
    Add the import `import androidx.compose.foundation.layout.wrapContentWidth` (alphabetically among the existing `androidx.compose.foundation.layout.*` imports).

    EDIT 2 — header Row: remove `.fillMaxWidth()` from the header Row modifier so it sizes to the width its content is constrained to (the 252dp grid, because the outer Column now wraps content). Change
    ```kotlin
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
    ```
    to
    ```kotlin
    Row(
        modifier = Modifier.width(252.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
    ```
    This pins the header to exactly the day-grid width (7 × 36dp = 252dp). `Arrangement.SpaceBetween` now spreads prev/label/next across 252dp — prev flush-left of the grid, next flush-right of the grid. Add the import `import androidx.compose.foundation.layout.width` (alphabetically among the existing layout imports).

    Leave the day-of-week Row and the 6 day Rows AS-IS (they may keep `.fillMaxWidth()` — with the outer Column now `wrapContentWidth`, their max width collapses to the 252dp content width, so they render identically and stay column-aligned with the 252dp header). Do NOT change DayCell, daysInMonth, the day-of-week labels, or any kotlinx-datetime logic.

    Compile, then run the existing test suite to confirm no regression:
    `./gradlew :library:compileKotlin :library:test`
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin :library:test</automated>
  </verify>
  <acceptance_criteria>
    - `grep -n "wrapContentWidth" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns the outer Column line (1+ hits).
    - `grep -n "Modifier.width(252.dp).padding(vertical = 4.dp)" library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt` returns the header Row line (1 hit).
    - The header Row no longer contains `fillMaxWidth()` — verify: the line immediately following `horizontalArrangement = Arrangement.SpaceBetween` block's `Row(` opener does NOT use `Modifier.fillMaxWidth()`. (Other `fillMaxWidth()` hits on the day-of-week / day Rows are allowed to remain.)
    - `import androidx.compose.foundation.layout.wrapContentWidth` and `import androidx.compose.foundation.layout.width` are both present.
    - `internal fun AeroCalendarGrid` signature is byte-for-byte unchanged (params, order, defaults).
    - `./gradlew :library:compileKotlin` exits 0.
    - `./gradlew :library:test` exits 0 AND the existing 27 unit tests still pass (AeroCalendarGridTest 7 + AeroColorMathTest 16 + AeroCalendarPositionProviderTest 4 — XML reports show 0 failures, 0 errors). Existing 27 unit tests still green is REQUIRED.
  </acceptance_criteria>
  <done>AeroCalendarGrid's header Row is pinned to the 252dp day-grid width (no longer fillMaxWidth); outer Column wraps content; the day grid is untouched; the library compiles and all 27 existing tests stay green.</done>
</task>

<task type="auto">
  <name>Task 2: Glass-wrap StepIndicator + calendar popup demos and swap raw Buttons to Aero buttons (gaps 02, 03, 04 — scratch demo only)</name>
  <files>library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt</files>
  <read_first>
    - library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt (CURRENT STATE — StepIndicatorDemo uses Material `OutlinedButton`/`Button`; CalendarPopupDemo uses Material `Button` inside a 1024dp Box with `contentAlignment = Alignment.CenterEnd`; both demos render on bare backgrounds)
    - library/src/main/kotlin/com/mordred/aero/components/containers/AeroCard.kt (the glass container — AeroCard wraps glassEffect; default 16dp padding)
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt (AeroButton(text, onClick, ...) — text param, NOT content slot)
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt (AeroOutlinedButton(text, onClick, ...))
    - .planning/phases/07-shared-internal-primitives/07-VERIFICATION.md (gaps 02/03/04 fix_intent — glass wrappers represent how Phase 8/10 public consumers host the primitives; buttons must sit at intrinsic width, not stretch)
  </read_first>
  <behavior>
    - StepIndicatorDemo: the indicator + the Prev/Next button row are wrapped together in an AeroCard (glass surface). Prev is an AeroOutlinedButton; Next is an AeroButton. Buttons sit at intrinsic width inside a Row (no fillMaxWidth on the buttons).
    - CalendarPopupDemo: the Open/Close trigger is an AeroButton at intrinsic width (no fillMaxWidth) — it must NOT stretch across the 1024dp frame; it stays at the right edge via the existing `contentAlignment = Alignment.CenterEnd` of the parent Box. The Popup BODY (the calendar) is wrapped in an AeroCard (glass surface) instead of the current bare `Box(Modifier.background(colors.panelBackground))`.
    - No raw Material `Button(` or `OutlinedButton(` calls remain anywhere in the file.
    - Architecture B preserved: glass is added ONLY in the scratch wrappers — AeroCalendarGrid and AeroStepIndicator themselves stay surface-less (no glass modifier passed into the primitives).
    - The StepIndicator inside the AeroCard must NOT use `Modifier.fillMaxWidth()` that would force it to span an over-wide card; keep it at a readable width (the AeroStepIndicator already lays out at its content width — remove the `fillMaxWidth()` currently wrapping it so the connector lines stay compact and the three states are clearly readable; the AeroCard sizes to content).
  </behavior>
  <action>
    Edit `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`. Four concrete changes:

    CHANGE A — imports. Remove the two Material button imports:
    ```kotlin
    import androidx.compose.material3.Button
    import androidx.compose.material3.OutlinedButton
    ```
    Add (alphabetically among existing `com.mordred.aero.*` imports):
    ```kotlin
    import androidx.compose.foundation.layout.wrapContentWidth
    import com.mordred.aero.components.buttons.AeroButton
    import com.mordred.aero.components.buttons.AeroOutlinedButton
    import com.mordred.aero.components.containers.AeroCard
    ```

    CHANGE B — StepIndicatorDemo. Replace the current body so the indicator + button row sit inside an AeroCard, the indicator is NOT fillMaxWidth, and the buttons are Aero buttons at intrinsic width. Replace from the `Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {` line through its matching closing brace with:
    ```kotlin
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AeroStepIndicator (toggle theme to verify all 3)", color = colors.onBackground, style = typography.bodyMedium)
        AeroCard(modifier = Modifier.wrapContentWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AeroStepIndicator(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    modifier = Modifier.width(320.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AeroOutlinedButton(
                        text = "Prev",
                        onClick = { if (currentStep > 0) currentStep-- },
                        enabled = currentStep > 0,
                    )
                    AeroButton(
                        text = "Next",
                        onClick = { if (currentStep < totalSteps - 1) currentStep++ },
                        enabled = currentStep < totalSteps - 1,
                    )
                    Text(
                        text = "step ${currentStep + 1} / $totalSteps",
                        color = colors.labelText,
                        style = typography.label,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
    ```
    (Use `Modifier.width(320.dp)` for the indicator so the four dots + connectors read compactly inside the card. The previous `Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp))` wrapper around the indicator is removed.)

    CHANGE C — CalendarPopupDemo trigger button. Replace the raw Material trigger:
    ```kotlin
    Button(
        onClick = { open = !open },
        modifier = Modifier.padding(end = 16.dp),
    ) {
        Text(if (open) "Close calendar" else "Open calendar")
    }
    ```
    with:
    ```kotlin
    AeroButton(
        text = if (open) "Close calendar" else "Open calendar",
        onClick = { open = !open },
        modifier = Modifier.padding(end = 16.dp),
    )
    ```
    The parent Box keeps `contentAlignment = Alignment.CenterEnd`, so the AeroButton sits at its intrinsic width at the right edge of the 1024dp frame (no fillMaxWidth — confirmed: AeroButton does not fill width by default).

    CHANGE D — CalendarPopupDemo popup body. Replace the bare popup-body Box:
    ```kotlin
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
    ```
    with an AeroCard glass wrapper:
    ```kotlin
    AeroCard {
        AeroCalendarGrid(
            displayMonth = displayMonth,
            selected = selected,
            onDateSelected = { selected = it; open = false },
            onMonthChange = { displayMonth = it },
        )
    }
    ```

    Do NOT touch CalendarGridDemo, HsvDemo, or DragSplitterDemo. Do NOT add any glass modifier INSIDE AeroCalendarGrid or AeroStepIndicator — glass lives only in these scratch wrappers (architecture B).

    If `background`, `Alignment`, or `padding` imports become unused after the edits, remove them only if the compiler flags them; otherwise leave imports as-is. Compile both modules:
    `./gradlew :library:compileKotlin :showcase:compileKotlin`
  </action>
  <verify>
    <automated>cd c:/1A_WORK/ui_lib && ./gradlew :library:compileKotlin :showcase:compileKotlin</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "import androidx.compose.material3.Button$" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns 0 (Material Button import removed).
    - `grep -c "import androidx.compose.material3.OutlinedButton" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns 0.
    - `grep -E "(^|[^a-zA-Z])(OutlinedButton|Button)\(" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns 0 raw Material button call sites (only `AeroButton(` / `AeroOutlinedButton(` may appear).
    - `grep -c "AeroButton(" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns >= 2 (Next in StepIndicatorDemo + Open-calendar trigger).
    - `grep -c "AeroOutlinedButton(" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns >= 1 (Prev).
    - `grep -c "AeroCard" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns >= 2 (StepIndicator wrap + popup-body wrap), plus the import line.
    - `grep -q "import com.mordred.aero.components.containers.AeroCard" library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` returns 0.
    - The AeroButton trigger in CalendarPopupDemo does NOT use `fillMaxWidth` (grep the CalendarPopupDemo region — no `fillMaxWidth` on the trigger).
    - No glass modifier (`glassEffect`/`glassPanel`/`glassSurface`/`AeroCard`) is passed as an argument INTO `AeroCalendarGrid(` or `AeroStepIndicator(` — primitives stay surface-less (architecture B).
    - `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0.
  </acceptance_criteria>
  <done>StepIndicator demo and calendar-popup body are wrapped in AeroCard glass; Prev/Next and the Open-calendar trigger use AeroOutlinedButton/AeroButton at intrinsic width; zero raw Material Button calls remain; primitives stay surface-less; both modules compile.</done>
</task>

</tasks>

<verification>
- gap-01 (BLOCKING): `AeroCalendarGrid` header Row is `Modifier.width(252.dp)` (not `fillMaxWidth`), outer Column is `wrapContentWidth`; `:library:test` green (27 tests).
- gap-02: StepIndicator demo block wrapped in `AeroCard`.
- gap-03: Calendar popup body wrapped in `AeroCard`.
- gap-04: Zero raw Material `Button(`/`OutlinedButton(` call sites in `AeroPhase7Scratch.kt`; AeroButton/AeroOutlinedButton used; trigger at intrinsic width.
- Architecture B intact: no glass passed into any primitive; AeroCalendarGrid + AeroStepIndicator stay surface-less.
- Both modules compile: `:library:compileKotlin :showcase:compileKotlin` exit 0.
- No other primitive touched; no new files created.
</verification>

<success_criteria>
All 4 gaps from `07-VERIFICATION.md` closed: calendar header matches grid width (symmetric layout), StepIndicator + calendar-popup demos on glass, scratch buttons Aero-styled at intrinsic width. The two `blocked_by_gaps` UAT truths (StepIndicator three-theme contrast; CalendarPositionProvider wide-popup visual) become re-checkable. Existing 27 unit tests stay green. Exactly 2 files modified; 0 files created.
</success_criteria>

<output>
After completion, create `.planning/phases/07-shared-internal-primitives/07-03-SUMMARY.md`.

Then a human re-runs the UAT visual checks via `./gradlew :showcase:run` (scroll to "Phase 7 Scratch") toggling AeroBlue / AeroDark / Classic to confirm: (1) calendar header aligns with the day grid; (2) StepIndicator three states readable on glass in all themes; (3) Open-calendar trigger sits at the right edge at intrinsic width and the wide popup right-aligns on a clean glass panel.
</output>
