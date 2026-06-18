# Phase 11: Showcase + v2.0 Visual Sign-off — Research

**Researched:** 2026-06-18
**Domain:** Compose Multiplatform showcase wiring + v2.0 visual sign-off (codebase-internal)
**Confidence:** HIGH — all findings come from reading actual source files in this repo

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- Phase boundary: strictly additive; zero changes to any v2.0/v1.x component public API.
- DataSection subject domain: satellite communication sessions (mordred origin project).
- DataSection row count: ~100 rows.
- DataSection columns: 6 columns — Name (text), NORAD ID (number), AOS Date (date), Duration min (number), Elevation° (number), Status (badge). AeroBadge for status column.
- AeroTreeView is REQUIRED in DataSection (drives 16-item checklist PITFALL-05 verification even though SHW-07 does not name it explicitly).
- PickersSection layout: vertical label-rows (RangeRow pattern), AeroColorPickerButton (popup form).
- Value display: raw `.toString()` ISO format — not formatted text. DateRangePicker value reflects committed range only (after onRangeSelect); partial state shows placeholder.
- LayoutSection: AeroSplitPane (h+v) each in a `Box` with fixed height (~240–300dp). AeroSidebar as top-level sibling in a fixed-height `Box { Row { AeroSidebar; demo-content } }`, never inside SplitPane pane.
- AeroStepperWizard: 3–4 steps using real v1.x fields (AeroTextField, AeroCheckbox/AeroRadioGroup), at least one onValidate gate.
- AeroAccordion: two side-by-side Columns (single + multi) in a Row.
- Sign-off gate: 16-item PITFALLS.md checklist × 3 themes = 48 individual checks, recorded in a separate sign-off document (`11-SIGNOFF.md`). Phase 11 is NOT complete until all 48 are PASS.
- Scratch cleanup is in Phase 11 scope: delete `AeroPhase7Scratch.kt` (:library) + `Phase7ScratchSection.kt` (:showcase) + remove call + import from `ShowcaseApp.kt`.
- ShowcaseApp.kt receives exactly 3 new calls: `DataSection()`, `PickersSection()`, `LayoutSection()`.
- RangeSection receives exactly 1 new row (AeroRangeSlider) with no structural changes.
- No new Gradle dependencies, no new AeroColorScheme tokens, no library component changes.

### Claude's Discretion

- Exact column names/values within satellite theme.
- Structure of mock TreeView (e.g. ground-station / orbit-group hierarchy).
- Status-to-AeroBadge color mapping and set of status values.
- Exact heights of demo boxes (SplitPane/Sidebar), paddings, column widths for accordion.
- Wizard step content, summary text, validation messages.
- Sign-off file name (`11-SIGNOFF.md` or section in SUMMARY) and exact table format.
- Plan granularity / wave split.
- Wizard button labels, sidebar tooltip texts.

### Deferred Ideas (OUT OF SCOPE)

- AeroSidebar as global showcase navigation.
- AeroDropdown popup-offset regression fix (v1.0 carry-over).
- Mock-data generator as shared reusable utility.
- Inline-mode ColorPicker demo in section.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| SHW-07 | DataSection: AeroDataTable ~100 rows, 5–6 mixed-type columns (text/number/date/status badge); sort, multi-selection, column resize interactive; PITFALL-04 absent | Confirmed AeroDataTable API; mock-data pattern documented; AeroTreeView inclusion required for checklist item 4 |
| SHW-08 | PickersSection: all 5 pickers + AeroRangeSlider; each shows current value as Text for UAT | All 6 component APIs confirmed from source; value-display = `.toString()` pattern documented |
| SHW-09 | LayoutSection: AeroAccordion (single+multi), AeroSplitPane (h+v), AeroSidebar (mode toggle), AeroStepperWizard (3–4 steps + validation) | All 4 component APIs confirmed; bounded-box requirement for SplitPane/Sidebar documented |
| SHW-10 | Three-theme visual checkpoint with 16-item checklist; grep gates pass | All 16 checklist items reproduced verbatim from PITFALLS.md with per-item verification methods |
</phase_requirements>

---

## Summary

Phase 11 is a pure wiring and sign-off phase. All 12 v2.0 components are already implemented and tested (Phases 7–10). The work is: create three new section files (`DataSection.kt`, `PickersSection.kt`, `LayoutSection.kt`), extend `RangeSection.kt` with one `AeroRangeSlider` row, wire the three sections into `ShowcaseApp.kt` (exactly three new calls), delete the Phase 7 scratch files, and then run a formal 16-item × 3-theme visual sign-off gate.

No new external dependencies are needed. No component code changes are permitted. The challenge is not technical novelty — it is structural correctness (bounded boxes for SplitPane/Sidebar so they work inside the vertically-scrolling ShowcaseApp column), mock-data design that exercises every checklist item, and disciplined sign-off execution across all three themes.

The 16-item checklist from PITFALLS.md is the formal milestone gate. All 16 items must be PASS in each of the three themes before Phase 11 is marked complete. Two items (items 15 and 16) are automated grep gates; the remaining 14 are visual/interactive checks performed eyes-on in the running showcase.

**Primary recommendation:** Follow the build order RangeSection extension → PickersSection → DataSection → LayoutSection → ShowcaseApp wiring → scratch cleanup → sign-off UAT × 3 themes.

---

## Standard Stack

### Core (no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.1.21 | Language | Locked project-wide |
| Compose Multiplatform | 1.7.3 | UI runtime | Locked project-wide |
| JDK | 17 | JVM target | Locked project-wide |
| kotlinx-datetime | 0.6.2 | LocalDate/Time/DateTime for picker values and mock AOS dates | Added in Phase 8; already in :library deps |

**No new dependencies required.** Phase 11 is showcase code only — all imports come from `com.mordred.aero.*` (already in :library, available to :showcase).

**Installation:** nothing to install; existing `library/build.gradle.kts` already declares kotlinx-datetime.

---

## Architecture Patterns

### Established Showcase Structure

```
showcase/src/main/kotlin/com/mordred/showcase/
├── ShowcaseApp.kt               # Root wiring — verticalScroll Column, toastState, ThemeSwitcher
└── sections/
    ├── FoundationSection.kt     # Existing v1.0 sections
    ├── ButtonsSection.kt
    ├── InputSection.kt
    ├── SelectionSection.kt
    ├── DropdownSection.kt
    ├── RangeSection.kt          # EXTEND: add AeroRangeSlider row
    ├── ListSection.kt
    ├── ContainersSection.kt
    ├── OverlaysSection.kt
    ├── NavigationSection.kt
    ├── IconsSection.kt
    ├── Phase7ScratchSection.kt  # DELETE in Phase 11
    ├── DataSection.kt           # NEW — SHW-07
    ├── PickersSection.kt        # NEW — SHW-08
    └── LayoutSection.kt         # NEW — SHW-09
```

### Pattern 1: Section File Structure (from `RangeSection.kt`)

Every section is a `@Composable fun XxxSection()` in package `com.mordred.showcase.sections`. The section owns local `remember` state. A private `XxxRow` helper holds the label–content layout (160dp fixed-width label, then content in a horizontal Row).

```kotlin
// Source: showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt
@Composable
fun RangeSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    var sliderValue by remember { mutableStateOf(0.5f) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Range & Progress", color = colors.onBackground, style = typography.title)

        RangeRow(label = "AeroSlider") {
            AeroSlider(value = sliderValue, onValueChange = { sliderValue = it }, modifier = Modifier.width(240.dp))
            Text("= ${"%.2f".format(sliderValue)}", color = colors.labelText, style = typography.bodyMedium)
        }
    }
}

@Composable
private fun RangeRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AeroTheme.colors.labelText, style = AeroTheme.typography.bodyMedium, modifier = Modifier.width(160.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
```

### Pattern 2: ShowcaseApp Wiring (from `ShowcaseApp.kt`)

`ShowcaseApp.kt` is a `verticalScroll` Column inside a `Box`. New sections are added as direct children of the outer Column. `AeroToastHostState` is already available as a parameter for sections that need it (like `IconsSection`). The Phase 7 scratch call to remove is at line 96.

```kotlin
// Source: showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt (excerpt)
Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(48.dp),
    verticalArrangement = Arrangement.spacedBy(32.dp)
) {
    // ... existing sections ...
    Phase7ScratchSection()   // ← DELETE this call (line 96) + its import (line 32)
    // New v2.0 calls go here:
    DataSection()
    PickersSection()
    LayoutSection()
    Spacer(Modifier.height(24.dp))
}
```

### Pattern 3: Bounded-Box for Height-Consuming Components

`ShowcaseApp.kt` uses `verticalScroll` on its root Column, which gives infinite height to children. `AeroDataTable`, `AeroTreeView`, `AeroSplitPane`, and `AeroSidebar` all require a bounded height — they contain their own `LazyColumn` or animate a fixed width. The pattern is:

```kotlin
// Fixed-height wrapper — mandatory for any component that requires bounded constraints
Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
    AeroDataTable(...)   // or AeroSplitPane / AeroSidebar-containing Row
}
```

Omitting this causes an infinite-constraint crash or layout failure (same root cause as PITFALL-01).

### Pattern 4: Value Display for Pickers (from CONTEXT.md decision)

Each picker in PickersSection shows its committed value as a plain `Text` using `.toString()` (ISO format). DateRangePicker shows the range only after both endpoints are committed; before that, shows `""` or a placeholder. This pattern is already used in `RangeSection` for `AeroSlider`.

```kotlin
// PickersSection picker row pattern — follows RangeRow
var dateValue by remember { mutableStateOf<LocalDate?>(null) }
RangeRow(label = "AeroDatePicker") {
    AeroDatePicker(value = dateValue, onValueChange = { dateValue = it })
    Text(
        text = dateValue?.toString() ?: "—",
        color = AeroTheme.colors.labelText,
        style = AeroTheme.typography.bodyMedium
    )
}
```

### Pattern 5: Mock-Data Generator for DataSection

Generate deterministic mock data at the section's `remember` call site. Use a fixed seed or a hard-coded list so the data is stable across recompositions. For 100 satellite sessions:

```kotlin
// DataSection mock-data pattern — deterministic, inline
data class SatSession(
    val id: Int,
    val name: String,
    val noradId: Int,
    val aosDate: LocalDate,
    val durationMin: Int,
    val elevationDeg: Float,
    val status: String   // "Active" | "Scheduled" | "Failed" | "Complete"
)

val sessions: List<SatSession> = remember {
    (1..100).map { i ->
        SatSession(
            id = i,
            name = "SAT-${"%03d".format(i)}",
            noradId = 40000 + i,
            aosDate = LocalDate(2026, 1, 1).plus(i.toLong(), DateTimeUnit.DAY),
            durationMin = 5 + (i % 15),
            elevationDeg = 10f + (i % 80),
            status = listOf("Active","Scheduled","Failed","Complete")[(i % 4)]
        )
    }
}
```

`key = { it.id }` — stable across sort (PITFALL-04 compliance).

### Pattern 6: AeroBadge in DataTable Cell (from `ListSection.kt`)

```kotlin
// Source: showcase/.../sections/ListSection.kt (AeroBadge usage reference)
// Status column cell:
AeroTableColumn<SatSession>(
    header = "Status",
    width = AeroColumnWidth.Fixed(100.dp),
    cell = { session ->
        val (bg, fg) = when (session.status) {
            "Active"    -> colors.success to colors.onSuccess
            "Scheduled" -> colors.primary to colors.onPrimary
            "Failed"    -> colors.error to colors.onError
            else        -> colors.borderDefault to colors.onSurface
        }
        AeroBadge(text = session.status, color = bg, contentColor = fg)
    }
)
```

### Anti-Patterns to Avoid

- **AeroDataTable/AeroTreeView inside verticalScroll without a height-clamping Box:** causes infinite constraint crash (PITFALL-01). Wrap in `Box(Modifier.height(Xdp))`.
- **AeroSidebar inside AeroSplitPane pane:** two width systems fight; sidebar width animation and SplitPane divider offset conflict (PITFALL-11). Sidebar must be a top-level sibling in a `Row`.
- **Calling `onValidate` in composable body for wizard step enabled state:** fires on every recomposition (PITFALL-12). Use `AeroWizardStep.canProceed` for live enabled state; `onValidate` only in `onClick`.
- **Using `transparent = true` on any Popup/Dialog:** Win11 crash (W11-01). All v2.0 popups use `Popup(...)`, never `Dialog(undecorated=true, transparent=true)`.
- **Using `AeroScrollArea` to wrap DataTable or TreeView:** they own their own `LazyColumn + AeroScrollBar` (PITFALL-01 rule). Do not add any scroll wrapper on top.
- **Using `detectDragGestures` for any new drag code in showcase:** banned v2.0-wide (PITFALL-03). The showcase adds no new drag code anyway — all drag is in the components themselves.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Scrollable table with virtualization | Custom scrollable Column | `AeroDataTable` (already owns LazyColumn + AeroScrollBar) | PITFALL-01 — infinite constraint |
| Calendar popup positioning | New PopupPositionProvider | `AeroCalendarPositionProvider` (Phase 7) | Already correct for wide calendar widths |
| Color picker UI | New HSV canvas | `AeroColorPickerButton` (Phase 8) | Avoids PITFALL-15 drift; full HSV panel already built |
| Dual-thumb slider | Two Material3 Sliders on one track | `AeroRangeSlider` (Phase 8) | PITFALL-07 and PITFALL-03 already solved |
| Draggable divider | Raw `detectDragGestures` on a Box | `AeroSplitPane` (Phase 10, uses `aeroDragSplitter`) | PITFALL-03 — touchSlop 18dp silently kills desktop drag |
| Mock AOS dates | Raw numeric arithmetic | `kotlinx.datetime.LocalDate.plus(n, DateTimeUnit.DAY)` | Already on classpath; ISO `.toString()` works directly |
| Status color variants | Hardcoded Color literals | `AeroTheme.colors.{success,error,primary,borderDefault}` | Token-based — adapts across all three themes |

---

## Common Pitfalls

### Pitfall 1: DataTable/TreeView without bounded height box

**What goes wrong:** `AeroDataTable` or `AeroTreeView` placed directly in the `verticalScroll` Column of `ShowcaseApp` receives infinite height from the parent scroll. The inner `LazyColumn` will either crash or materialize all rows.
**Why it happens:** `ShowcaseApp.kt` wraps everything in a `verticalScroll` Column — correct for static content, fatal for lazy lists.
**How to avoid:** Wrap every `AeroDataTable` and `AeroTreeView` call in `Box(Modifier.fillMaxWidth().height(300.dp))` (height is a discretionary choice ~240–400dp).
**Warning signs:** Showcase renders all 100 rows simultaneously visible without scrolling the table; or layout exception about infinite constraints.

### Pitfall 2: AeroSidebar inside AeroSplitPane pane (PITFALL-11)

**What goes wrong:** Placing `AeroSidebar` inside the `start` pane of `AeroSplitPane` causes the SplitPane's static divider offset to fight the sidebar's `animateDpAsState` width animation. The sidebar clips or the divider jumps during collapse.
**How to avoid:** The demo box for the Sidebar section must be `Box { Row { AeroSidebar(state); Box(Modifier.weight(1f)) { demoContent() } } }`. The adjacent content box reads `state.currentWidthDp` if it needs to reflow, but in the simplest demo `weight(1f)` handles it automatically.
**Warning signs:** PITFALL-11 sign-off item fails in any theme.

### Pitfall 3: DateRangePicker value Text leaks partial state

**What goes wrong:** If the value Text below `AeroDateRangePicker` is driven by an intermediate variable updated on every date click (not just on `onRangeSelect`), partial state (only start selected) appears as a committed range.
**How to avoid:** The `var rangeStart / rangeEnd` state variables that feed the value Text must be updated ONLY inside the `onRangeSelect` lambda. `AeroDateRangePicker` already guards this internally (sealed `AeroDateRangeState`), but the showcase state must mirror that discipline.
**Warning signs:** Clicking one date causes the value Text to update; sign-off checklist item 6 fails.

### Pitfall 4: AeroStepperWizard requires a glass surface wrapper

**What goes wrong:** `AeroStepperWizard` is surface-less by design (KDoc: "the wizard is surface-less"). If the LayoutSection renders it without a glass-panel wrapper, it floats on the raw background with no visual container.
**How to avoid:** Wrap the wizard demo in a `Box(Modifier.glassPanel(...))` or `AeroCard`. Consult existing `ContainersSection.kt` for the `glassPanel` Modifier usage pattern.

### Pitfall 5: Scratch files left in after sign-off (W11-01 grep gate contamination)

**What goes wrong:** `AeroPhase7Scratch.kt` is in the `:library` module at `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`. If it is not deleted before the `transparent = true` grep gate runs, the grep may return false positives from scratch demo code.
**How to avoid:** Delete both files (`AeroPhase7Scratch.kt` in :library and `Phase7ScratchSection.kt` in :showcase) and remove the `Phase7ScratchSection()` call + import from `ShowcaseApp.kt` before running grep gates.

### Pitfall 6: AeroAccordion side-by-side layout needs explicit width

**What goes wrong:** Two `Column(weight 1f)` inside a `Row` will each receive half the available width, which is correct. But if the outer Row itself is inside the `verticalScroll` Column without a `fillMaxWidth()`, it may collapse to wrap-content width.
**How to avoid:** Use `Row(Modifier.fillMaxWidth())` for the accordion demo row.

---

## Code Examples

### ShowcaseApp.kt — diff after Phase 11

```kotlin
// Source: showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

// REMOVE these lines:
// import com.mordred.showcase.sections.Phase7ScratchSection   (line 32)
// Phase7ScratchSection()                                       (line 96)

// ADD these imports:
import com.mordred.showcase.sections.DataSection
import com.mordred.showcase.sections.PickersSection
import com.mordred.showcase.sections.LayoutSection

// ADD these calls (after NavigationSection(), before Spacer):
DataSection()
PickersSection()
LayoutSection()
```

### RangeSection.kt — AeroRangeSlider extension

```kotlin
// ADD to RangeSection() body, after existing rows:
var rangeValue by remember { mutableStateOf(0.2f..0.7f) }

RangeRow(label = "AeroRangeSlider") {
    AeroRangeSlider(
        value = rangeValue,
        onValueChange = { rangeValue = it },
        modifier = Modifier.width(240.dp)
    )
    Text(
        text = "${"%.2f".format(rangeValue.start)} → ${"%.2f".format(rangeValue.endInclusive)}",
        color = AeroTheme.colors.labelText,
        style = AeroTheme.typography.bodyMedium
    )
}
```

### AeroDataTable call signature (confirmed from source)

```kotlin
// Source: library/.../datatable/AeroDataTable.kt
AeroDataTable(
    data = sessions,                    // List<T>
    columns = columns,                  // List<AeroTableColumn<T>>
    key = { it.id },                    // (T) -> Any — stable, survives sort (PITFALL-04)
    modifier = Modifier.fillMaxWidth().height(360.dp),
    selectionMode = SelectionMode.Multi,
    selectedKeys = selectedKeys,        // Set<Any>
    onSelectionChange = { selectedKeys = it },
)
```

### AeroTreeView call signature (confirmed from source)

```kotlin
// Source: library/.../datatable/AeroTreeView.kt
AeroTreeView(
    rootNodes = groundStations,
    children = { node -> node.children },
    isExpandable = { node -> node.children.isNotEmpty() },
    key = { node -> node.id },
    nodeContent = { node -> Text(node.name, color = AeroTheme.colors.onSurface) },
    modifier = Modifier.fillMaxWidth().height(200.dp),
    onExpand = { node ->
        // Log here for checklist item 4 verification:
        println("onExpand fired for: ${node.name}")
    },
)
```

### AeroSidebar demo (PITFALL-11 compliant pattern)

```kotlin
// Source: library/.../layout/AeroSidebar.kt KDoc pattern
val sidebarState = rememberAeroSidebarState(AeroSidebarMode.Expanded)
Box(Modifier.fillMaxWidth().height(280.dp)) {
    Row(Modifier.fillMaxSize()) {
        AeroSidebar(state = sidebarState) {
            item(AeroIcons.House, "Home", selected = true) {}
            item(AeroIcons.Gear, "Settings", selected = false) {}
            divider()
            item(AeroIcons.Bell, "Alerts", selected = false) {}
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            // adjacent content — reflows as sidebar animates
        }
    }
}
// Mode toggle button (outside the Box):
AeroButton(onClick = {
    sidebarState.mode = when (sidebarState.mode) {
        AeroSidebarMode.Expanded  -> AeroSidebarMode.Collapsed
        AeroSidebarMode.Collapsed -> AeroSidebarMode.Hidden
        AeroSidebarMode.Hidden    -> AeroSidebarMode.Expanded
    }
}) { Text("Toggle mode: ${sidebarState.mode}") }
```

### AeroStepperWizard call signature (confirmed from source)

```kotlin
// Source: library/.../layout/AeroStepperWizard.kt
AeroStepperWizard(
    steps = listOf(
        AeroWizardStep(
            label = "Identifier",
            content = { /* AeroTextField */ },
            canProceed = nameValue.isNotBlank(),   // live enabled state
            onValidate = { nameValue.isNotBlank() } // commit gate — only fires on Next click
        ),
        AeroWizardStep(label = "Options", content = { /* AeroCheckbox/AeroRadioGroup */ }),
        AeroWizardStep(label = "Summary", content = { /* summary text */ }),
    ),
    onFinish = { /* handle finish */ },
    modifier = Modifier.glassPanel(...)
)
```

### AeroColorPickerButton call signature (confirmed from source)

```kotlin
// Source: library/.../pickers/AeroColorPickerButton.kt
var colorValue by remember { mutableStateOf(Color(0xFF4FC3F7)) }
RangeRow(label = "AeroColorPicker") {
    AeroColorPickerButton(
        value = colorValue,
        onValueChange = { colorValue = it },
    )
    Text(
        text = "#${colorValue.red.times(255).toInt().toString(16).padStart(2,'0').uppercase()}" +
               "${colorValue.green.times(255).toInt().toString(16).padStart(2,'0').uppercase()}" +
               "${colorValue.blue.times(255).toInt().toString(16).padStart(2,'0').uppercase()}",
        color = AeroTheme.colors.labelText,
        style = AeroTheme.typography.bodyMedium
    )
}
```

---

## The 16-Item "Looks Done But Isn't" Checklist

**Source:** `.planning/research/PITFALLS.md` §"Looks Done But Isn't" Checklist (lines 455–470)

These 16 items are the formal gate for v2.0 milestone sign-off. Every item must be verified **eyes-on** in each of the three themes (AeroBlue, AeroDark, Classic) except items 15 and 16 which are automated grep gates. Record results in `11-SIGNOFF.md`.

| # | Checklist Item (verbatim) | How to Verify in Showcase | Relevant Section |
|---|--------------------------|---------------------------|-----------------|
| 1 | AeroDataTable virtualization: Verify row count above the fold does NOT equal total row count when data > 50 rows. If all rows are mounted, `LazyColumn` is not actually lazy. | Launch showcase with 100 rows. Table shows ~10–15 rows visible. Scroll down — new rows render; top rows unmount. In debug mode: check recomposition count or log items composing. | DataSection |
| 2 | AeroDataTable selection after sort: Select row with key X, sort descending, verify row with key X is still highlighted at its new position. | Click "AOS Date" header to sort ascending. Click row for SAT-050. Click "AOS Date" again to sort descending. SAT-050 row moves position but keeps blue highlight. | DataSection |
| 3 | AeroDataTable column resize: Drag a column splitter past the available width — verify other columns reflow and no column goes to zero width. | Drag the "Name" column splitter rightward past "NORAD ID". Other columns shrink but never vanish. Also drag leftward past minWidth — column stops at 40dp minimum. | DataSection |
| 4 | AeroTreeView lazy callback: Open a node, scroll it off screen, scroll back — verify `onExpand` is NOT called again. | Expand a ground-station node. Scroll the table/tree area so the node goes off screen. Scroll back. Console log should show only one `"onExpand fired for: X"` line. | DataSection |
| 5 | AeroDatePicker popup position: Place the trigger field at the right edge of a 1024dp window — verify calendar does not clip. | Resize the showcase window to ~1024dp wide. The DatePicker trigger field will be in the right half. Open the calendar — it should right-align to avoid clipping. | PickersSection |
| 6 | AeroDateRangePicker partial state: Click only start date, close without selecting end — verify `onRangeSelect` was NOT called. | Open DateRangePicker popup. Click one date. Click outside to dismiss. The value Text below the picker should still show the placeholder (not a committed range). | PickersSection |
| 7 | AeroColorPicker round-trip: Set to `#FF0000`, drag saturation to 50%, drag back to 100% — verify HEX still reads `#FF0000` (no drift). | Open AeroColorPickerButton. Drag the HSV square saturation axis left, then back to full right. The HEX value Text below should read `#FF0000`. | PickersSection |
| 8 | AeroRangeSlider thumb overlap: Drag start thumb to equal end thumb — verify thumbs do not permanently merge (one becomes unreachable). | In RangeSection, drag the start thumb rightward until both thumbs are adjacent. Release. Drag again — both thumbs should still be individually reachable and moveable. | RangeSection |
| 9 | AeroAccordion single mode: Open section B while section A is open — verify A closes. | In LayoutSection accordion demo, single-mode column: click section 1 header (opens). Click section 2 header — section 1 must close, section 2 opens. | LayoutSection |
| 10 | AeroSplitPane clamp: Drag divider to far edge — verify pane does not collapse to zero. | In LayoutSection SplitPane demo, drag the divider all the way left/right (horizontal) or up/down (vertical). Both panes must maintain at least 48dp. | LayoutSection |
| 11 | AeroSidebar + adjacent layout: Collapse sidebar — verify adjacent content reflows to use reclaimed space. | In LayoutSection Sidebar demo, click "Toggle mode" button. Sidebar animates from Expanded (240dp) to Collapsed (48dp). Adjacent content Box should expand to fill reclaimed space smoothly. | LayoutSection |
| 12 | AeroStepperWizard validation: Tab through fields without clicking Next — verify `onValidate` is NOT called during focus movement. | In LayoutSection wizard demo, Tab through the fields in step 1 without clicking "Next". No validation feedback or blocked-state changes should occur. Then click "Next" with blank field — Next is blocked. | LayoutSection |
| 13 | All pickers: AeroDark disabled cells: Verify disabled date cells are readable (not invisible) in AeroDark theme. | Switch to AeroDark. Open AeroDatePicker, navigate to a month with past disabled dates. Past dates must be visible (grey, not invisible). Same check on AeroDateRangePicker. | PickersSection |
| 14 | All drag components: Desktop drag response: Verify HSV square, RangeSlider thumbs, and DataTable column splitters all respond on first mouse movement (no slop delay). | Drag HSV square in ColorPicker: cursor-position update should be immediate. Drag RangeSlider thumb: responds on first pixel. Drag DataTable column header splitter: responds on first pixel. Any 18px delay = `detectDragGestures` touchSlop bug still present. | DataSection + PickersSection + RangeSection |
| 15 | No `transparent=true`: Grep `transparent = true` in all new v2.0 files — must be zero results. | **Automated grep gate.** See Grep Gates section below. | (all new files) |
| 16 | No `AeroScrollArea` wrapping LazyColumn: Grep for `AeroScrollArea` inside any DataTable or TreeView file — must be zero results. | **Automated grep gate.** See Grep Gates section below. | (datatable package) |

---

## Grep Gates

### W11-01: No `transparent = true`

**Purpose:** Confirm no new v2.0 file introduces `transparent = true` on a Window or Dialog — which causes Win11 `EXCEPTION_ACCESS_VIOLATION` crash.

**Run in project root (Bash / Git Bash):**
```bash
grep -rn "transparent = true" \
  library/src/main/kotlin/com/mordred/aero/components/pickers/ \
  library/src/main/kotlin/com/mordred/aero/components/datatable/ \
  library/src/main/kotlin/com/mordred/aero/components/layout/ \
  library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt \
  showcase/src/main/kotlin/com/mordred/showcase/sections/DataSection.kt \
  showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt \
  showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt
```
**Expected result:** 0 matches. Any match is a blocker.

**PowerShell equivalent (Windows):**
```powershell
Get-ChildItem -Recurse -Include "*.kt" `
  "library\src\main\kotlin\com\mordred\aero\components\pickers",
  "library\src\main\kotlin\com\mordred\aero\components\datatable",
  "library\src\main\kotlin\com\mordred\aero\components\layout",
  "showcase\src\main\kotlin\com\mordred\showcase\sections" |
  Select-String "transparent = true"
```

### W11-02: No `AeroScrollArea` inside DataTable or TreeView

**Purpose:** Confirm DataTable and TreeView packages do not wrap their `LazyColumn` in `AeroScrollArea` (would destroy virtualization — PITFALL-01).

**Run in project root (Bash):**
```bash
grep -rn "AeroScrollArea" \
  library/src/main/kotlin/com/mordred/aero/components/datatable/
```
**Expected result:** 0 matches.

**PowerShell equivalent:**
```powershell
Get-ChildItem -Recurse -Include "*.kt" `
  "library\src\main\kotlin\com\mordred\aero\components\datatable" |
  Select-String "AeroScrollArea"
```

---

## Confirmed Public APIs

All APIs confirmed by reading actual source files. No guessing.

### AeroDataTable (confirmed `AeroDataTable.kt`, `AeroTableColumn.kt`, `AeroDataTableTypes.kt`)

```kotlin
@Composable
fun <T> AeroDataTable(
    data: List<T>,
    columns: List<AeroTableColumn<T>>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    selectionMode: SelectionMode = SelectionMode.None,    // None | Single | Multi
    selectedKeys: Set<Any> = emptySet(),
    rowHeight: Dp = 36.dp,
    emptyContent: (@Composable () -> Unit)? = null,
    onSelectionChange: (Set<Any>) -> Unit = {},
    onSortChange: ((columnIndex: Int, direction: SortDirection) -> Unit)? = null,
)

data class AeroTableColumn<T>(
    header: String,
    width: AeroColumnWidth,                    // Fixed(dp) or Weight(float)
    alignment: Alignment.Horizontal = Start,
    sortKey: ((T) -> Comparable<*>)? = null,   // null = non-sortable column
    minWidth: Dp = 40.dp,
    cell: @Composable (T) -> Unit,
)

// Convenience builder:
fun <T> textColumn(header, width, alignment, sortKey, minWidth, text: (T) -> String): AeroTableColumn<T>

enum class SelectionMode { None, Single, Multi }
enum class SortDirection { Asc, Desc, None }
sealed interface AeroColumnWidth {
    data class Fixed(dp: Dp) : AeroColumnWidth
    data class Weight(value: Float) : AeroColumnWidth
}
```

### AeroTreeView (confirmed `AeroTreeView.kt`)

```kotlin
@Composable
fun <T> AeroTreeView(
    rootNodes: List<T>,
    children: (T) -> List<T>,
    isExpandable: (T) -> Boolean,
    key: (T) -> Any,
    nodeContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 36.dp,
    indentPerLevel: Dp = 16.dp,
    emptyContent: (@Composable () -> Unit)? = null,
    onExpand: (T) -> Unit = {},           // fires EXACTLY ONCE per node on first expand (PITFALL-05)
    onNodeClick: (T) -> Unit = {},
)
```

### AeroDatePicker (confirmed `AeroDatePicker.kt`)

```kotlin
@Composable
fun AeroDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (LocalDate) -> String = { it.toString() },
    placeholder: String = "Select date",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
)
```

### AeroTimePicker (confirmed `AeroTimePicker.kt`)

```kotlin
@Composable
fun AeroTimePicker(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,    // fires on every spinner change; no Apply gate
    modifier: Modifier = Modifier,
    formatter: (LocalTime) -> String = { it.toString() },
    placeholder: String = "Select time",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true,
    showSeconds: Boolean = false,
    minuteStep: Int = 1,
)
// 24-hour only — no use12Hour parameter (descoped in Phase 8)
```

### AeroDateTimePicker (confirmed `AeroDateTimePicker.kt`)

```kotlin
@Composable
fun AeroDateTimePicker(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,   // fires ONLY on Apply click (NEW-PICK-02)
    modifier: Modifier = Modifier,
    formatter: (LocalDateTime) -> String = { it.toString() },
    placeholder: String = "Select date & time",
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

### AeroDateRangePicker (confirmed `AeroDateRangePicker.kt`)

```kotlin
@Composable
fun AeroDateRangePicker(
    startValue: LocalDate?,
    endValue: LocalDate?,
    onRangeSelect: (start: LocalDate, end: LocalDate) -> Unit,   // fires EXACTLY ONCE per committed range (PITFALL-06)
    modifier: Modifier = Modifier,
    formatter: (LocalDate) -> String = { it.toString() },
    placeholder: String = "Select range",
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    selectableDates: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
)
```

### AeroColorPickerButton (confirmed `AeroColorPickerButton.kt`)

```kotlin
@Composable
fun AeroColorPickerButton(
    value: Color,
    onValueChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enableAlpha: Boolean = false,
    swatches: List<Color> = DefaultAeroSwatches,
    enabled: Boolean = true,
)
```

### AeroRangeSlider (confirmed `AeroRangeSlider.kt`)

```kotlin
@Composable
fun AeroRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    showTooltip: Boolean = true,
)
```

### AeroAccordion (confirmed `AeroAccordion.kt`)

```kotlin
@Composable
fun AeroAccordion(
    sections: List<AeroAccordionSection>,
    modifier: Modifier = Modifier,
    mode: AeroAccordionMode = AeroAccordionMode.Multi,   // Single | Multi
    initiallyExpanded: Set<Int> = emptySet(),
    expandedIndex: Int? = null,           // controlled single-mode
    expandedIndices: Set<Int>? = null,    // controlled multi-mode
    onExpandedChange: ((Set<Int>) -> Unit)? = null,   // null = uncontrolled
)

data class AeroAccordionSection(
    title: String,
    leadingIcon: ImageVector? = null,
    content: @Composable () -> Unit,
)
```

### AeroSplitPane (confirmed `AeroSplitPane.kt`)

```kotlin
@Composable
fun AeroSplitPane(
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    orientation: AeroSplitOrientation = AeroSplitOrientation.Horizontal,   // Horizontal | Vertical
    initialSplitFraction: Float = 0.5f,
    minFirstPaneSize: Dp = 48.dp,
    minSecondPaneSize: Dp = 48.dp,
    onSplitChange: ((Float) -> Unit)? = null,
)
```

### AeroSidebar (confirmed `AeroSidebar.kt`, `AeroSidebarState.kt`)

```kotlin
@Composable
fun AeroSidebar(
    state: AeroSidebarState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable AeroSidebarScope.() -> Unit,
)

@Composable
fun rememberAeroSidebarState(initialMode: AeroSidebarMode = Expanded): AeroSidebarState

class AeroSidebarState(initialMode: AeroSidebarMode) {
    var mode: AeroSidebarMode           // change to trigger animation
    val currentWidthDp: State<Dp>       // live animated width — adjacent layout reads this
}

enum class AeroSidebarMode { Expanded, Collapsed, Hidden }
// target widths: Expanded=240dp, Collapsed=48dp, Hidden=0dp (confirmed from targetWidthForMode())

class AeroSidebarScope {
    @Composable fun item(icon, label, selected, onClick)
    @Composable fun section(label)
    @Composable fun divider()
}
```

### AeroStepperWizard (confirmed `AeroStepperWizard.kt`)

```kotlin
@Composable
fun AeroStepperWizard(
    steps: List<AeroWizardStep>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 0,
    currentStep: Int? = null,
    onStepChange: ((Int) -> Unit)? = null,
    backLabel: String = "Back",
    nextLabel: String = "Next",
    finishLabel: String = "Finish",
)

data class AeroWizardStep(
    label: String,
    content: @Composable () -> Unit,
    onValidate: () -> Boolean = { true },   // called ONLY in onClick, never in body (PITFALL-12)
    canProceed: Boolean = true,             // live enabled-state for Next button
)
```

---

## Scratch File Deletion Targets (confirmed from source)

| File | Location | Action |
|------|----------|--------|
| `AeroPhase7Scratch.kt` | `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` | Delete |
| `Phase7ScratchSection.kt` | `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` | Delete |
| `Phase7ScratchSection()` call | `ShowcaseApp.kt` line 96 | Remove |
| `import com.mordred.showcase.sections.Phase7ScratchSection` | `ShowcaseApp.kt` line 32 | Remove |

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (JUnit Jupiter) |
| Config file | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*" -x generateIcons` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

Phase 11 is a wiring + sign-off phase. There are no new stateful components and thus no new unit tests to add. All component logic (selection, sort, range-state, expand-guard, etc.) was tested in Phases 8–10. The phase's verification is visual/interactive (sign-off) and grep-based.

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SHW-07 | DataSection compiles, table renders 100 rows | Compile gate | `./gradlew :showcase:compileKotlin` | ❌ Wave 0 — new file |
| SHW-08 | PickersSection compiles, all 6 pickers wired | Compile gate | `./gradlew :showcase:compileKotlin` | ❌ Wave 0 — new file |
| SHW-09 | LayoutSection compiles, all 4 components wired | Compile gate | `./gradlew :showcase:compileKotlin` | ❌ Wave 0 — new file |
| SHW-10 | 16-item checklist × 3 themes all PASS; grep gates return 0 | Manual visual (14 items) + grep automation (2 items) | grep commands in Grep Gates section | Manual only |

### Sampling Rate

- **Per task commit:** `./gradlew :library:test :showcase:compileKotlin`
- **Per wave merge:** `./gradlew :library:test :showcase:compileKotlin`
- **Phase gate:** Full compile green + 16-item sign-off document complete with all PASS before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/DataSection.kt` — SHW-07
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt` — SHW-08
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — SHW-09
- [ ] `.planning/phases/11-showcase-v2-0-visual-sign-off/11-SIGNOFF.md` — SHW-10 gate artifact

---

## State of the Art

| Old Pattern | Current Pattern | When Changed | Impact |
|-------------|-----------------|--------------|--------|
| `AeroScrollArea` wrapping LazyColumn | `LazyColumn` + standalone `AeroScrollBar(lazyListState)` | Phase 9 (PITFALL-01) | DataTable/TreeView now truly virtualized |
| `detectDragGestures` for canvas drag | `awaitPointerEventScope` manual loop | Phase 7 (PITFALL-03) | Drag responds on first mouse pixel on Desktop |
| `Set<Int>` selection indices | `Set<Any>` with `key: (T) -> Any` | Phase 9 (PITFALL-04) | Selection survives sort |
| `AeroDropdownPopup` for date pickers | `AeroCalendarPositionProvider` + raw `Popup()` | Phase 7/8 (PITFALL-02) | Wide calendars position correctly at window edges |
| `Dialog(undecorated=true, transparent=true)` | `Popup(...)` only | Locked v1.0 (W11-01) | No Win11 crash |

---

## Open Questions

1. **AeroColorPickerButton HEX value display**
   - What we know: `AeroColorPickerButton.onValueChange` returns a Compose `Color` (not a HEX string). Converting `Color` to HEX requires channel arithmetic.
   - What's unclear: Whether there is an internal `rgbToHex` utility already exported or only `internal`. `AeroColorMathTest.kt` exists, suggesting `rgbToHex` is in `com.mordred.aero.components.pickers.internal.color`.
   - Recommendation: If `rgbToHex` is `internal`, compute HEX inline in the showcase (`"#%02X%02X%02X".format((c.red*255).roundToInt(), ...)`) rather than trying to import an internal function.

2. **`glassPanel` Modifier import path for wizard wrapper**
   - What we know: `AeroStepperWizard` is surface-less; it needs a caller-supplied glass wrapper.
   - What's unclear: Whether the demo box should use `Modifier.glassPanel(...)` (from `AeroTheme`) or `AeroCard { }`. Both exist per prior section code.
   - Recommendation: Use `AeroCard` — it is a public composable, already used in `ContainersSection.kt`, and provides the correct glass surface without needing to call `Modifier.glassPanel` directly.

---

## Sources

### Primary (HIGH confidence — direct source reads)

- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — exact wiring structure, toastState availability, Phase7ScratchSection call location
- `showcase/src/main/kotlin/com/mordred/showcase/sections/RangeSection.kt` — canonical `RangeRow` pattern
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt` — canonical `AeroBadge` usage
- `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` — file to delete + confirmed library reference path
- `library/.../datatable/AeroDataTable.kt` — full public API + KDoc
- `library/.../datatable/AeroTableColumn.kt` — AeroTableColumn/AeroColumnWidth/textColumn
- `library/.../datatable/AeroDataTableTypes.kt` — SelectionMode/SortDirection enums
- `library/.../datatable/AeroTreeView.kt` — full public API + PITFALL-05 guard confirmation
- `library/.../pickers/AeroDatePicker.kt` — full signature
- `library/.../pickers/AeroTimePicker.kt` — full signature; 24h-only confirmed
- `library/.../pickers/AeroDateTimePicker.kt` — Apply-gate behavior confirmed (NEW-PICK-02)
- `library/.../pickers/AeroDateRangePicker.kt` — sealed state machine + onRangeSelect guard (PITFALL-06)
- `library/.../pickers/AeroColorPickerButton.kt` — swatch-trigger form confirmed
- `library/.../range/AeroRangeSlider.kt` — full signature + ClosedFloatingPointRange API
- `library/.../layout/AeroAccordion.kt` — sections data-list API, hybrid mode, AeroAccordionSection
- `library/.../layout/AeroSplitPane.kt` — full signature including orientation/fraction/min sizes
- `library/.../layout/AeroSidebarState.kt` — AeroSidebarState/rememberAeroSidebarState/AeroSidebarScope
- `library/.../layout/AeroSidebar.kt` — KDoc including PITFALL-11 Row sibling pattern
- `library/.../layout/AeroStepperWizard.kt` — AeroWizardStep data class + PITFALL-12 gate behavior
- `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` — confirmed deletion target path
- `.planning/research/PITFALLS.md` — all 16 checklist items (lines 455–470), PITFALL-01 through W11-02
- `.planning/phases/11-showcase-v2-0-visual-sign-off/11-CONTEXT.md` — all locked decisions
- `.planning/REQUIREMENTS.md` — SHW-07 through SHW-10
- `.planning/STATE.md` — v2.0 locked decisions, accumulated context
- `.planning/config.json` — nyquist_validation: true confirmed
- `library/build.gradle.kts` — JUnit 5 test setup confirmed

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — confirmed from build.gradle.kts (no new deps)
- Architecture patterns: HIGH — confirmed from reading actual ShowcaseApp.kt and section files
- Component public APIs: HIGH — confirmed from reading actual source files, not KDoc summaries
- Checklist items: HIGH — reproduced verbatim from PITFALLS.md source, lines 455–470
- Grep gate commands: HIGH — paths verified against actual file locations found by Glob
- Bounded-box requirement: HIGH — confirmed from ShowcaseApp.kt verticalScroll structure + PITFALL-01

**Research date:** 2026-06-18
**Valid until:** Stable (all findings from this repo's own source; no external ecosystem drift risk)
