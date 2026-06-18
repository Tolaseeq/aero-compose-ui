# Phase 10: Layout - Research

**Researched:** 2026-06-18
**Domain:** Compose Multiplatform Desktop — layout composables (Accordion, SplitPane, Sidebar, StepperWizard)
**Confidence:** HIGH — all findings drawn from existing project source files (verified), project research documents (previously verified), and the actual Phase 7 primitives already in the codebase.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**AeroAccordion**
- Sections declared as data-list: `sections: List<AeroAccordionSection>` where each section = `(title: String, leadingIcon: ImageVector? = null, content: @Composable () -> Unit)`
- Ownership = hybrid (uncontrolled default + `initiallyExpanded`, controlled when explicit expansion param + `onExpandedChange` provided)
- `mode = single` controlled param: `expandedIndex: Int?`; `mode = multi` controlled param: `expandedIndices: Set<Int>`
- Header = `title` + optional `leadingIcon` (left) + library-drawn caret (right, `AeroIcons.{CaretRight,CaretDown}`, `rotationZ` 0→90°)
- Animation = `animateContentSize()` ~150–180ms + caret rotate via `animateFloatAsState`; NOT `animateIntAsState(maxHeight)`

**AeroSplitPane**
- Split position = fraction `0f..1f` (`initialSplitFraction: Float = 0.5f`), internally stored as px for clamp math
- Ownership = uncontrolled + optional `onSplitChange: (Float) -> Unit` callback (NOT hybrid — controlled drag sends callback every frame)
- Divider visual = 1dp `borderDefault` line + grip nasechki (3–4 dots/dashes) brighter on hover
- Hit-area invisibly extended to ~8–12dp around the 1dp line (LAYO-04)
- Keyboard nudge = Claude's Discretion (add if clean, else defer v2.x)

**AeroSidebar**
- State = `rememberAeroSidebarState(initialMode)` holding `var mode: AeroSidebarMode` + `val currentWidthDp: State<Dp>` (animated via `animateDpAsState`)
- Structure: optional `header`/`footer` slots + items between via `AeroSidebarScope`
- `AeroSidebarScope` exposes: `item(icon, label, selected, onClick)`, `section(label)`, `divider()`
- Active item visual = left vertical primary accent-bar (~3dp) + glass-gradient fill (bar visible in collapsed too)
- Identification = per-item `selected: Boolean` (no internal selectedKey state in v2.0)

**AeroStepperWizard**
- Steps declared as data-list: `steps: List<AeroWizardStep>` where each step = `(label, content, onValidate, canProceed)`
- Ownership = hybrid (`initialStep` uncontrolled default, controlled when `currentStep` + `onStepChange` provided)
- Buttons = built-in Back/Next/Finish with customizable labels (`backLabel`/`nextLabel`/`finishLabel`)
- `onValidate: () -> Boolean` called ONLY in Next/Finish `onClick` — NEVER in composable body
- `canProceed: Boolean` = caller-driven live enabled-signal for the button
- `onFinish: () -> Unit` called on Finish after successful `onValidate` of last step

### Claude's Discretion
- AeroSplitPane keyboard nudge (add if clean; else defer v2.x)
- Exact placement of `onValidate`/`canProceed` (per-step in `AeroWizardStep` preferred; planner confirms at signature build)
- Exact dp/timing values: grip nasechki color/geometry, sidebar expanded/collapsed widths (~240dp/~48dp), paddings, indent, animation timing curves
- Data-class/enum names (`AeroAccordionSection`, `AeroWizardStep`, `AeroSidebarMode`, `AeroSplitOrientation`)
- Wave split / plan granularity

### Deferred Ideas (OUT OF SCOPE)
- StepperWizard non-linear branching (STEP-BR-01)
- AeroSidebar drag-to-resize width (SIDE-RES-01)
- AeroSplitPane recursive N-pane public API (N-pane via caller nesting only)
- AeroStepIndicator vertical orientation (horizontal-only)
- AeroSidebar built-in selectedKey state (per-item `selected: Boolean` flag only)
- AeroSplitPane keyboard nudge (if not clean)
- AeroAccordion full header-slot (`@Composable (expanded) -> Unit`)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| LAYO-01 | AeroAccordion renders collapsible sections; animated via `animateContentSize` or `expandVertically`; caret indicator | State-lift pattern (PITFALL-13), `animateContentSize` + `animateFloatAsState` confirmed |
| LAYO-02 | AeroAccordion `mode = single \| multi`; single: opening new closes previous; default = multi | Lifted `expandedIndex: Int?` / `expandedIndices: Set<Int>` in parent (PITFALL-13) |
| LAYO-03 | AeroSplitPane two panes + draggable splitter; orientation H/V; minimum sizes prevent collapse | `BoxWithConstraints` measurement + clamp formula (PITFALL-14) + `aeroDragSplitter` |
| LAYO-04 | Splitter extended hit-area ~8–12dp; cursor changes on hover; `awaitPointerEventScope` via `aeroDragSplitter` | `aeroDragSplitter` already uses `pointerHoverIcon` + manual loop; Phase 7 primitive verified |
| LAYO-05 | AeroSidebar persistent; icon+label expanded; icon+tooltip collapsed; hidden; `AeroTooltip` in collapsed | `rememberAeroSidebarState` pattern, `AeroTooltip` wrapper API verified in source |
| LAYO-06 | Sidebar animated width transitions via `animateDpAsState`; active item primary-color highlight; `onItemClick` callback | `animateDpAsState` pattern from STACK.md; sidebar state-object exposes `currentWidthDp: State<Dp>` |
| LAYO-07 | Sidebar items via composable slots: `AeroSidebarScope` with `item(icon, label, selected, onClick)` | Scope-DSL pattern (like `RowScope`), `content: @Composable AeroSidebarScope.() -> Unit` |
| LAYO-08 | AeroStepperWizard horizontal `AeroStepIndicator`; current/completed/upcoming visually distinct | `AeroStepIndicator(currentStep, totalSteps)` internal API verified in source |
| LAYO-09 | StepperWizard shows step content + Back/Next/Finish; per-step `onValidate` gate on Next only; state preserved on Back | PITFALL-12 pattern: `onValidate` in onClick only; `canProceed` for live enabled; state preservation via all-steps-composed pattern |
</phase_requirements>

---

## Summary

Phase 10 delivers four layout composables that are structural in nature — they arrange other composables rather than presenting data or input controls. All four have their architectural contracts fully locked by prior research (PITFALL-11 through PITFALL-14, CONTEXT.md decisions, STACK.md API table) and depend on Phase 7 primitives (`aeroDragSplitter`, `AeroStepIndicator`) that already exist in the codebase.

The dominant concerns are state architecture (accordion coordination, sidebar state-object, wizard hybrid navigation), animation correctness (animateContentSize vs animateIntAsState, animateDpAsState for sidebar width), and the composition between `AeroSidebar`'s animated width and adjacent layout (PITFALL-11). None of these require new Gradle dependencies.

The implementation risk is MEDIUM overall: AeroAccordion (SMALL — ~100 lines) and AeroSplitPane (SMALL — ~80 lines) are the simplest; AeroSidebar (MEDIUM — state-object + three modes + scope DSL + tooltip + Aero styling) and AeroStepperWizard (MEDIUM — hybrid nav + validate gate + step content management) are more involved.

**Primary recommendation:** Build in the order SplitPane → Accordion → Sidebar → StepperWizard (per CONTEXT.md recommendation). Each component lands in `components/layout/` package. Unit tests cover the three pure-function concerns: accordion toggle logic, split clamp math, wizard step-transition + validate-gate. All Compose behavior is verified visually; no Compose UI testing framework is required.

---

## Standard Stack

### Core (unchanged — no new dependencies)

| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Kotlin | 2.1.21 | Language | Already in project |
| Compose Multiplatform | 1.7.3 | UI framework (desktop) | Already in project |
| JDK | 17 | JVM target | Already in project |
| compose.animation | bundled with CMP 1.7.3 | `animateContentSize`, `animateDpAsState`, `animateFloatAsState`, `AnimatedVisibility` | Already on classpath |
| compose.foundation.layout | bundled with CMP 1.7.3 | `BoxWithConstraints`, layout primitives | Already on classpath |
| compose.ui | bundled with CMP 1.7.3 | `graphicsLayer`, `pointerHoverIcon`, `Modifier.onSizeChanged` | Already on classpath |

### Phase 7 Primitives (existing, ready to consume)

| Artifact | Location | Used By | API |
|----------|----------|---------|-----|
| `Modifier.aeroDragSplitter` | `components/internal/drag/AeroDragSplitter.kt` | AeroSplitPane | `aeroDragSplitter(orientation, onDrag: (deltaPx: Float) -> Unit, onDragEnd, enabled)` |
| `AeroStepIndicator` | `components/layout/internal/stepper/AeroStepIndicator.kt` | AeroStepperWizard | `AeroStepIndicator(currentStep: Int, totalSteps: Int, modifier, onStepClick?)` |

### v1.x Components (existing, ready to consume)

| Component | Location | Used By |
|-----------|----------|---------|
| `AeroTooltip` | `components/overlay/AeroTooltip.kt` | AeroSidebar (collapsed item labels) |
| `AeroButton` / `AeroOutlinedButton` | `components/buttons/` | AeroStepperWizard (Next=filled, Back=outlined) |
| `GlassModifiers` (`glassPanel`, `glassSurface`, `glassEffect`) | `theme/GlassModifiers.kt` | Accordion headers, Sidebar background, Wizard surface |
| `AeroColorScheme` tokens | `theme/AeroColorScheme.kt` | All four components — 23 existing tokens, no new tokens needed |
| `AeroIcons.{CaretRight,CaretDown,Check}` | `icons/AeroIcons.kt` | Accordion caret; StepIndicator already uses Check |

**Installation:** No new dependencies. No `build.gradle.kts` changes needed for Phase 10.

---

## Architecture Patterns

### Recommended Project Structure

```
library/src/main/kotlin/com/mordred/aero/
└── components/
    └── layout/                              # NEW public files go here
        ├── AeroAccordion.kt                 # Public — LAYO-01, LAYO-02
        ├── AeroSplitPane.kt                 # Public — LAYO-03, LAYO-04
        ├── AeroSidebar.kt                   # Public — LAYO-05, LAYO-06, LAYO-07
        ├── AeroStepperWizard.kt             # Public — LAYO-08, LAYO-09
        └── internal/
            └── stepper/
                └── AeroStepIndicator.kt     # EXISTING — Phase 7; do NOT modify
```

The `components/internal/drag/AeroDragSplitter.kt` is already at the internal/drag level (not under layout/), which is correct — it is shared by DataTable column resize and SplitPane.

### Pattern 1: Accordion — State Lifted to Parent (PITFALL-13)

**What:** `AeroAccordion` holds all expansion state. Individual section composables receive `expanded: Boolean` + `onToggle: () -> Unit` as parameters and hold no state of their own. This is the only pattern that enables single-mode coordination.

**Lifted state shape:**
```kotlin
// mode = single
var expandedIndex: Int? by remember { mutableStateOf(null) }

// mode = multi (mutableStateSetOf preserves Set semantics in Compose)
val expandedIndices: Set<Int> = remember { mutableStateSetOf() }
```

**Toggle logic (lives in AeroAccordion, not in section composable):**
```kotlin
// Single mode toggle
onToggle = { i ->
    expandedIndex = if (expandedIndex == i) null else i
}

// Multi mode toggle
onToggle = { i ->
    if (i in expandedIndices) expandedIndices.remove(i) else expandedIndices.add(i)
}
```

**Hybrid (uncontrolled ↔ controlled) contract:**
- Uncontrolled: component manages `expandedIndex`/`expandedIndices` internally with optional `initiallyExpanded`
- Controlled: caller passes `expandedIndex: Int?` (single) or `expandedIndices: Set<Int>` (multi) + `onExpandedChange` callback; component becomes a pure renderer
- KDoc MUST document the switchover explicitly — executor must not simplify to one branch

**Anti-pattern:** Each `AeroAccordionSection` having `var expanded by remember { mutableStateOf(false) }` — sections cannot coordinate. Never.

### Pattern 2: Accordion — Animation (NOT animateIntAsState)

**What:** Use `animateContentSize()` Modifier on the content area, plus `animateFloatAsState` on the caret rotation.

```kotlin
// Source: PITFALLS.md §Performance Traps; STACK.md §AeroAccordion

// Section content — apply to the container Column/Box holding the content slot
Modifier.animateContentSize(
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
)

// Caret rotation
val rotation by animateFloatAsState(
    targetValue = if (expanded) 90f else 0f,
    animationSpec = tween(durationMillis = 160)
)
Icon(
    imageVector = AeroIcons.CaretRight,
    modifier = Modifier.graphicsLayer { rotationZ = rotation }
)
```

**Why NOT `animateIntAsState(maxHeight)`:** requires measuring actual content height, causes multiple layout passes, visible jank on expand. `animateContentSize()` is Foundation's built-in smooth size transition with no extra measurement.

**Aero-visual header:** Each section header = `Box/Row` with `Modifier.glassPanel(cornerRadius = 8.dp)` (or `glassSurface`) + `borderDefault` bottom divider separating sections.

### Pattern 3: SplitPane — BoxWithConstraints + Clamp (PITFALL-14)

**What:** Use `BoxWithConstraints` once for initial measurement. Store divider position in px internally. Clamp on every drag event.

```kotlin
// Source: PITFALLS.md §Performance Traps, PITFALL-14
BoxWithConstraints(Modifier.fillMaxSize()) {
    val totalPx = if (orientation == Horizontal) constraints.maxWidth.toFloat()
                  else constraints.maxHeight.toFloat()

    // Internal state — px, not Dp (needed for clamp math)
    var dividerPx by remember(totalPx) {
        mutableStateOf(totalPx * initialSplitFraction)
    }

    // Drag handler (passed to aeroDragSplitter)
    val onDrag: (Float) -> Unit = { delta ->
        dividerPx = (dividerPx + delta).coerceIn(
            minimumValue = minFirstPaneSize.toPx(),
            maximumValue = totalPx - minSecondPaneSize.toPx()
        )
        onSplitChange?.invoke(dividerPx / totalPx)
    }
}
```

**Why NOT `SubcomposeLayout` on every drag:** Sub-compose overhead fires on every drag event (60fps = 60 sub-compose cycles). `BoxWithConstraints` runs once; divider is re-laid from the stored px value.

**Orientation mapping in `aeroDragSplitter`:**
- `Orientation.Horizontal` → cursor = `E_RESIZE_CURSOR`, reports `cur.x - prev.x`
- `Orientation.Vertical` → cursor = `N_RESIZE_CURSOR`, reports `cur.y - prev.y`

**Hit-area extension:** Wrap the 1dp visual divider in a `Box` with explicit width/height (e.g., `8.dp` for horizontal) — the visual line is centered. The `aeroDragSplitter` modifier attaches to the outer Box.

**Layout:** Use `Modifier.width()` / `Modifier.height()` derived from `dividerPx` in dp (convert via `LocalDensity`). NOT `weight()` — weight recalculates both slots on every drag recompose.

```kotlin
// Horizontal example
Row {
    Box(Modifier.width((dividerPx / density.density).dp)) { startContent() }
    Divider()  // 1dp visual + 8dp hit-area Box with aeroDragSplitter
    Box(Modifier.weight(1f)) { endContent() }
}
```

### Pattern 4: SplitPane — aeroDragSplitter Usage

The `aeroDragSplitter` Modifier (Phase 7) is the ONLY drag pattern allowed. Never add new `detectDragGestures`, `awaitDragOrCancellation`, or raw `pointerInput` drag for this component.

```kotlin
// Source: AeroDragSplitter.kt (Phase 7) — verified in source
Modifier.aeroDragSplitter(
    orientation = orientation,  // Orientation.Horizontal or Vertical
    onDrag = { delta -> onDrag(delta) },
    onDragEnd = { /* optional: snap, haptic */ },
    enabled = !atMinOrMax  // gate at clamp boundaries if desired
)
```

The Modifier already handles: cursor change, `pointerHoverIcon`, `awaitPointerEventScope` manual loop, `change.consume()` only on actual delta, release event NOT consumed.

### Pattern 5: Sidebar — State Object (rememberAeroSidebarState)

**What:** `rememberAeroSidebarState` is a class holding mode + animated width, following the `DrawerState`/`ScaffoldState` idiom.

```kotlin
// Public API shape
class AeroSidebarState(initialMode: AeroSidebarMode) {
    var mode: AeroSidebarMode by mutableStateOf(initialMode)
    // Derived animated width — driven by mode
    // Exposed for adjacent layout to consume (PITFALL-11)
    val currentWidthDp: State<Dp>  // wired to animateDpAsState internally
}

fun rememberAeroSidebarState(initialMode: AeroSidebarMode = AeroSidebarMode.Expanded): AeroSidebarState
```

**Width animation (animateDpAsState):**
```kotlin
// Source: STACK.md §AeroSidebar
val targetWidth = when (state.mode) {
    AeroSidebarMode.Expanded  -> 240.dp
    AeroSidebarMode.Collapsed -> 48.dp
    AeroSidebarMode.Hidden    -> 0.dp
}
val animatedWidth by animateDpAsState(
    targetValue = targetWidth,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
)
```

**PITFALL-11 prevention:** Expose `currentWidthDp: State<Dp>` from the state object. Caller's adjacent layout reads this to respond to sidebar collapse without placing sidebar inside SplitPane:
```kotlin
// Correct caller pattern (documented in KDoc)
Row {
    AeroSidebar(state = sidebarState) { /* items */ }
    Box(Modifier.weight(1f)) { mainContent() }
}
// Sidebar NOT inside SplitPane pane — top-level Row sibling
```

**NOT an overlay/Popup:** `AeroSidebar` is an in-layout `Box` with animated width. NOT a copy of `AeroDrawer`'s `FullWindowPositionProvider` popup approach.

### Pattern 6: Sidebar — Composable Slot Scope

**What:** Callers pass `content: @Composable AeroSidebarScope.() -> Unit`. `AeroSidebarScope` is a class with composable extension functions.

```kotlin
// Scope API
class AeroSidebarScope(private val state: AeroSidebarState) {
    @Composable
    fun item(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) { ... }

    @Composable
    fun section(label: String) { ... }  // hidden/compact in collapsed mode

    @Composable
    fun divider() { ... }
}
```

**Collapsed tooltip:** In `item()`, when `state.mode == AeroSidebarMode.Collapsed`, wrap the icon in `AeroTooltip(text = label)`:
```kotlin
// AeroTooltip wrapper form (verified in AeroTooltip.kt source)
AeroTooltip(text = label) {
    Icon(imageVector = icon, contentDescription = label, tint = ...)
}
```

**Active visual:** Left vertical accent-bar (~3dp wide, `colors.primary`) + glass-gradient row fill. The bar is always visible, even in collapsed mode.

**Section labels:** In expanded mode, render as small `Text` in `colors.labelText`; in collapsed mode, hide or show as separate tooltip if hover occurs.

### Pattern 7: StepperWizard — Hybrid Navigation + Validate Gate (PITFALL-12)

**What:** Wizard holds `currentStep: Int` state. Hybrid: uncontrolled default, controlled if caller passes `currentStep` + `onStepChange`.

**Step content preservation on Back:** All step content composables remain composed — do NOT `if/when` swap content (that destroys and recreates composable, losing state). Use a layout that always renders all steps but shows only the current one:

```kotlin
// Keep all steps composed — use visibility, not conditional composition
steps.forEachIndexed { index, step ->
    Box(modifier = Modifier.then(if (index == currentStepInt) Modifier else Modifier.size(0.dp))) {
        step.content()  // Always composed — state is preserved on Back
    }
}
```

Alternative: `AnimatedContent` with `ContentTransform` where exiting content is kept alive. The key constraint is: don't use `if (index == currentStep)` composition guard that destroys the composable — that resets all `remember`/`rememberSaveable` state.

**Validate gate — NEVER in composable body:**
```kotlin
// Next button onClick — the ONLY place onValidate is called
AeroButton(
    text = nextLabel,
    onClick = {
        val step = steps[currentStepInt]
        val valid = step.onValidate()  // Called here only, NOT in composable body
        if (valid) {
            if (currentStepInt == steps.lastIndex) onFinish()
            else advanceStep()
        }
    },
    enabled = steps[currentStepInt].canProceed  // Live signal — from caller's Boolean, not onValidate
)
```

**`canProceed` vs `onValidate` distinction:**
- `canProceed: Boolean` = live enabled-state of Next button (caller updates as user fills fields)
- `onValidate: () -> Boolean` = final commit gate checked only on Next click

**AeroStepIndicator usage:**
```kotlin
// Internal horizontal indicator — Phase 7 primitive
AeroStepIndicator(
    currentStep = currentStepInt,  // 0-based
    totalSteps = steps.size,
    modifier = Modifier.fillMaxWidth()
)
```

### Pattern 8: Aero-Visual Requirements (Non-Negotiable)

Per project memory (`aero-aesthetic-priority`): these components must look Win7, not generic-flat.

| Component | Required Aero Treatment |
|-----------|------------------------|
| Accordion header | `glassPanel(cornerRadius = 8.dp)` or `glassSurface` |
| Accordion section border | 1dp `borderDefault` divider between sections |
| SplitPane divider | 1dp `borderDefault` line + grip nasechki (3–4 dots), `buttonHover` tint on hover |
| SplitPane divider hover | `buttonHover` overlay on grip area |
| Sidebar background | `glassPanel` or `glassSurface` for the sidebar container |
| Sidebar active item | Left 3dp `primary` accent-bar + `primary.copy(alpha=0.15f)` gradient fill |
| StepperWizard surface | Caller wraps in glass panel; wizard itself is surface-less (like AeroStepIndicator) |
| All three themes | AeroDark validation required — `labelText` for upcoming steps, `borderDefault` for dividers/connectors |

### Anti-Patterns to Avoid

- **Per-section expanded state:** `var expanded by remember { ... }` inside section composable — sections cannot coordinate. NEVER.
- **SplitPane without clamp:** `dividerPx += delta` with no `coerceIn` — pane collapses to zero. NEVER.
- **Sidebar inside SplitPane pane:** Two independent width systems fight each other. NEVER.
- **AeroDrawer popup pattern for Sidebar:** `AeroSidebar` is NOT a modal overlay. Never copy `FullWindowPositionProvider`. NEVER.
- **`onValidate` in composable body:** `val isValid = step.onValidate()` at top of composable — fires on every recomposition. NEVER.
- **`if (index == currentStep)` to swap step content:** Destroys composable, loses all field state on Back. NEVER for content slots.
- **`detectDragGestures` for SplitPane drag:** Banned. Use `aeroDragSplitter` only.
- **`SubcomposeLayout` for every drag frame:** Performance trap. Use `BoxWithConstraints` once.
- **`animateIntAsState(maxHeight)` for accordion:** Multiple layout passes, jank. Use `animateContentSize()`.
- **`transparent = true` on any Dialog/Window:** Win11 `EXCEPTION_ACCESS_VIOLATION` crash. NEVER.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Divider drag with first-pixel response | New `pointerInput` + `detectDragGestures` | `Modifier.aeroDragSplitter` (Phase 7) | Already handles awaitPointerEventScope loop, cursor change, enabled gate, consume semantics — PITFALL-03 defused |
| Step indicator dots + connectors | Custom Row + Canvas step renderer | `AeroStepIndicator` (Phase 7) | Already handles current/completed/upcoming visual states, connectors, AeroDark contrast |
| Tooltip on icon-only sidebar item | Custom hover handler + Popup | `AeroTooltip(text = label) { Icon(...) }` | Already handles 600ms delay, position, PopupProperties(focusable=false) to avoid stealing hover |
| Back/Next/Finish buttons | Custom styled buttons | `AeroButton` (Next/Finish) + `AeroOutlinedButton` (Back) | Aero style from library — no glass reinvention |
| Sidebar width animation | Custom `LaunchedEffect` + manual interpolation | `animateDpAsState(targetValue, tween(...))` | Foundation API — smooth, cancellable, correct |
| Accordion content size animation | `animateIntAsState(measuredHeight)` | `Modifier.animateContentSize(tween(...))` | Foundation API — no measurement, no jank |
| Glass header for Accordion | Custom `drawBehind` gradient | `Modifier.glassPanel(cornerRadius = 8.dp)` or `glassSurface` | Library-standard glass — consistent across three themes |

**Key insight:** All drag, step-indicator, tooltip, animation, and glass-visual concerns are already solved by existing primitives. Phase 10 is almost entirely about composing them correctly, not building new infrastructure.

---

## Common Pitfalls

### Pitfall 1: Accordion Single-Mode State Not Lifted (PITFALL-13)
**What goes wrong:** `var expanded by remember { ... }` inside each section composable — opening section B does not close section A.
**Why it happens:** Per-section state is the "obvious" instinct.
**How to avoid:** `AeroAccordion` holds `expandedIndex: Int?` or `expandedIndices: Set<Int>`. Sections receive `expanded: Boolean` + `onToggle: () -> Unit`.
**Warning signs:** Two sections open simultaneously in single mode.

### Pitfall 2: SplitPane Divider Without Clamp (PITFALL-14)
**What goes wrong:** `dividerPx += delta` with no `coerceIn` — pane collapses to zero, layout exception from zero-width content.
**Why it happens:** Simple delta accumulation seems sufficient.
**How to avoid:** Always: `dividerPx = (dividerPx + delta).coerceIn(minFirstPaneSize.toPx(), totalPx - minSecondPaneSize.toPx())`
**Warning signs:** Dragging divider to either edge collapses a pane or throws.

### Pitfall 3: Sidebar Inside SplitPane (PITFALL-11)
**What goes wrong:** Layout thrashes — SplitPane divider jumps when sidebar collapses.
**Why it happens:** Two independent width systems operate without coordination.
**How to avoid:** AeroSidebar is a top-level layout sibling. Expose `currentWidthDp: State<Dp>` from state object. KDoc example: `Row { AeroSidebar(...); Box(Modifier.weight(1f)) { ... } }`
**Warning signs:** SplitPane divider jumps during sidebar collapse animation.

### Pitfall 4: onValidate Called in Composable Body (PITFALL-12)
**What goes wrong:** Validation fires on every recomposition (hover, scroll, sibling state change) — potential infinite loop, wrong UX.
**Why it happens:** Trying to derive button enabled-state from `onValidate` directly.
**How to avoid:** `canProceed: Boolean` = live enabled-signal (caller-driven). `onValidate` = gate in onClick only.
**Warning signs:** Form validation error messages flicker on hover over the Next button.

### Pitfall 5: Step Content Destroyed on Back Navigation (PITFALL-12 corollary)
**What goes wrong:** User fills step 2, navigates Back to step 1, returns to step 2 — fields are empty.
**Why it happens:** `if (index == currentStep) { step.content() }` destroys and recreates the composable.
**How to avoid:** Keep all step content composed at all times. Use size(0.dp) or visibility — not conditional composition.
**Warning signs:** Any `remember {}` state in step content resets on Back.

### Pitfall 6: SplitPane SubcomposeLayout on Every Drag (Performance)
**What goes wrong:** 60fps drag with SubcomposeLayout overhead — CPU spike, jank.
**Why it happens:** SubcomposeLayout seems like the natural measurement tool.
**How to avoid:** `BoxWithConstraints` once for total size. Store divider in px state. Re-layout from stored px on each drag.
**Warning signs:** Frame drops visible in profiler during drag.

### Pitfall 7: animateIntAsState(maxHeight) for Accordion (Performance)
**What goes wrong:** Accordion expand is jittery — multiple layout passes per frame.
**Why it happens:** Measuring actual content height requires extra layout passes.
**How to avoid:** `Modifier.animateContentSize(tween(...))` — Foundation handles it without content measurement.
**Warning signs:** Expand animation is noticeably choppy vs other Compose animations.

### Pitfall 8: Wrong Token for AeroDark Steps/Dividers
**What goes wrong:** In AeroDark, step upcoming-circles or accordion dividers become invisible — `borderDefault` is `Color(0x40FFFFFF)` (25% alpha on dark bg ≈ invisible).
**Why it happens:** Using `borderDefault` for outline/stroke in dark theme.
**How to avoid:** `AeroStepIndicator` already uses `labelText` for upcoming state (established in Phase 7). Accordion section dividers: use `borderDefault` (acceptable on panels) but not for text-level elements.
**Warning signs:** Steps in AeroDark look like they have no circles.

---

## Code Examples

Verified patterns from project source files and prior research:

### animateContentSize (Accordion expand/collapse)
```kotlin
// Source: STACK.md §AeroAccordion; PITFALLS.md §Performance Traps
Column(
    modifier = Modifier
        .animateContentSize(animationSpec = tween(durationMillis = 160))
        .clip(RoundedCornerShape(8.dp))
) {
    if (expanded) {
        // Content slot — always composed when visible
        step.content()
    }
}
// NOTE: animateContentSize handles the height animation; no maxHeight measurement needed
```

### animateFloatAsState (Caret rotation)
```kotlin
// Source: STACK.md; ARCHITECTURE.md §Pattern 5
val caretRotation by animateFloatAsState(
    targetValue = if (expanded) 90f else 0f,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "caretRotation"
)
Icon(
    imageVector = AeroIcons.CaretRight,
    contentDescription = null,
    tint = AeroTheme.colors.onSurface,
    modifier = Modifier.graphicsLayer { rotationZ = caretRotation }
)
```

### animateDpAsState (Sidebar width)
```kotlin
// Source: STACK.md §AeroSidebar; CONTEXT.md decisions
val targetWidth: Dp = when (mode) {
    AeroSidebarMode.Expanded  -> 240.dp
    AeroSidebarMode.Collapsed -> 48.dp
    AeroSidebarMode.Hidden    -> 0.dp
}
val animatedWidth by animateDpAsState(
    targetValue = targetWidth,
    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    label = "sidebarWidth"
)
Box(Modifier.width(animatedWidth).fillMaxHeight()) { /* sidebar content */ }
```

### aeroDragSplitter usage (SplitPane)
```kotlin
// Source: AeroDragSplitter.kt (verified in source)
// Divider Box — visual line + extended hit-area
Box(
    modifier = Modifier
        .let {
            if (orientation == Orientation.Horizontal)
                it.width(8.dp).fillMaxHeight()
            else
                it.height(8.dp).fillMaxWidth()
        }
        .aeroDragSplitter(
            orientation = orientation,
            onDrag = { delta ->
                dividerPx = (dividerPx + delta).coerceIn(
                    minimumValue = minFirstPaneSize.toPx(),
                    maximumValue = totalPx - minSecondPaneSize.toPx()
                )
            }
        )
) {
    // Visual center line — 1dp
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .let {
                if (orientation == Orientation.Horizontal)
                    it.width(1.dp).fillMaxHeight()
                else
                    it.height(1.dp).fillMaxWidth()
            }
            .background(AeroTheme.colors.borderDefault)
    )
    // Grip nasechki drawn here (3-4 dots, colors.labelText, centered)
}
```

### AeroTooltip wrapper for collapsed sidebar item
```kotlin
// Source: AeroTooltip.kt (verified in source) — wrapper form
AeroTooltip(text = label) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AeroTheme.colors.primary else AeroTheme.colors.onSurface
        )
    }
}
```

### AeroStepIndicator invocation
```kotlin
// Source: AeroStepIndicator.kt (verified in source)
// 0-based currentStep; surface-less (glass is wizard's responsibility)
AeroStepIndicator(
    currentStep = currentStepInt,
    totalSteps = steps.size,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
)
```

### Wizard Next button onClick — validate gate
```kotlin
// Source: PITFALL-12; CONTEXT.md decisions
AeroButton(
    text = if (isLastStep) finishLabel else nextLabel,
    onClick = {
        val step = steps[currentStepInt]
        if (step.onValidate()) {
            if (isLastStep) onFinish()
            else currentStepInt++
        }
        // onValidate NOT called anywhere else
    },
    enabled = steps[currentStepInt].canProceed
)
```

### Step content — keep all composed (Back state preservation)
```kotlin
// All step content stays composed to preserve remember/rememberSaveable state
steps.forEachIndexed { index, step ->
    Box(
        modifier = if (index == currentStepInt) Modifier.fillMaxWidth()
                   else Modifier.size(0.dp)
    ) {
        step.content()
    }
}
// Alternatively: AnimatedContent — but ensure exit keeps composable alive
```

### Pure-function test pattern (from existing test suite)
```kotlin
// Source: SortStateTest.kt, SelectionLogicTest.kt, ColumnWidthTest.kt — established pattern
// Phase 10 pure functions to test: accordion toggle logic, split clamp, wizard step transition

// Example: accordion toggle (pure, no Compose)
fun accordionToggleSingle(expandedIndex: Int?, clickedIndex: Int): Int? =
    if (expandedIndex == clickedIndex) null else clickedIndex

class AccordionToggleTest {
    @Test fun openNewSectionClosesPrevious() {
        assertEquals(1, accordionToggleSingle(expandedIndex = 0, clickedIndex = 1))
    }
    @Test fun clickOpenSectionClosesIt() {
        assertNull(accordionToggleSingle(expandedIndex = 0, clickedIndex = 0))
    }
}

// Example: SplitPane clamp (pure, no Compose)
fun clampDivider(currentPx: Float, delta: Float, minPx: Float, maxPx: Float): Float =
    (currentPx + delta).coerceIn(minPx, maxPx)

class SplitClampTest {
    @Test fun clampAtMin() = assertEquals(48f, clampDivider(50f, -100f, 48f, 500f))
    @Test fun clampAtMax() = assertEquals(500f, clampDivider(490f, 100f, 48f, 500f))
}
```

---

## State of the Art

| Old Approach | Current Approach | Source | Impact |
|--------------|------------------|--------|--------|
| `animateIntAsState(maxHeight)` for accordion expand | `Modifier.animateContentSize()` | PITFALLS.md §Performance Traps | Eliminates content measurement overhead and layout jank |
| `detectDragGestures` for splitter drag | `awaitPointerEventScope` manual loop via `aeroDragSplitter` | PITFALL-03; STATE.md locked decisions | First-pixel response (touchSlop=18dp eliminated) |
| Per-section `expanded` state in section composable | State lifted to parent `AeroAccordion` | PITFALL-13 | Single-mode coordination becomes possible |
| Sidebar as modal overlay (like AeroDrawer) | Sidebar as in-layout `Box` with `animateDpAsState` width | PITFALL-11; ARCHITECTURE.md §Pattern 6 | Adjacent layout reflows correctly on sidebar collapse |
| Derive button enabled from `onValidate()` in body | Separate `canProceed: Boolean` from `onValidate` gate | PITFALL-12 | Eliminates recomposition-driven validation calls |
| Controlled-drag SplitPane (sends callback every frame) | Uncontrolled + optional `onSplitChange` callback | CONTEXT.md decisions | Caller not required to hold split state for smooth drag |
| `SubcomposeLayout` for SplitPane measurement | `BoxWithConstraints` once at composition | PITFALLS.md §Performance Traps | No sub-compose overhead during drag |

**Deprecated/outdated (do not use):**
- `components-splitpane-desktop` library: last stable Maven Central release is 1.5.2 (Sept 2023), predates CMP 1.7.3; not usable.
- `MaterialTheme.colors.primary` or hardcoded hex values in new components: always use `AeroTheme.colors.*` tokens.
- `Icon()` without explicit `tint`: banned since v1.1.

---

## Open Questions

1. **Keyboard nudge for SplitPane divider (Claude's Discretion)**
   - What we know: `onPreviewKeyEvent` on the focusable divider Box; ~4dp per arrow keypress; standard desktop pattern
   - What's unclear: Does `focusable()` Modifier + `onPreviewKeyEvent` cleanly integrate with `aeroDragSplitter`?
   - Recommendation: Attempt in plan; if clean (~10 lines), include. If it requires a new Modifier composition concern, defer to v2.x per CONTEXT.md.

2. **Exact placement of `onValidate`/`canProceed` in `AeroWizardStep` data class**
   - What we know: Both are per-step, locked principle is onValidate in onClick only
   - What's unclear: Whether `onValidate` should be nullable (`onValidate: (() -> Boolean)? = null`) for steps with no server-side validation
   - Recommendation: `onValidate: () -> Boolean = { true }` default (always valid if not overridden); `canProceed: Boolean = true` default. Planner confirms at signature build.

3. **Step content all-composed vs. AnimatedContent approach**
   - What we know: `size(0.dp)` pattern keeps composable alive; `AnimatedContent` with cross-fade or slide also possible
   - What's unclear: Whether `size(0.dp)` trick has accessibility implications on Desktop CMP
   - Recommendation: Use `size(0.dp)` hiding pattern (simpler, no animation framework needed for content swap); animate only the step indicator. If executor wants AnimatedContent for polish, ensure exit content is kept composed.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | `kotlin.test` (JUnit 5 on JVM) — established in project, no new setup |
| Config file | `library/build.gradle.kts` — `testImplementation(kotlin("test"))` already present |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.layout.*"` |
| Full suite command | `./gradlew :library:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| LAYO-01 | Accordion animates (visual only) | manual/visual | n/a | n/a |
| LAYO-02 | Single-mode toggle coordination | unit (pure fn) | `./gradlew :library:test --tests "*.AccordionToggleTest"` | Wave 0 |
| LAYO-02 | Multi-mode toggle independence | unit (pure fn) | `./gradlew :library:test --tests "*.AccordionToggleTest"` | Wave 0 |
| LAYO-03 | SplitPane divider clamp at min/max | unit (pure fn) | `./gradlew :library:test --tests "*.SplitClampTest"` | Wave 0 |
| LAYO-03 | SplitPane fraction ↔ px conversion | unit (pure fn) | `./gradlew :library:test --tests "*.SplitClampTest"` | Wave 0 |
| LAYO-04 | Drag hits first pixel (aeroDragSplitter) | smoke/visual | verified in Phase 7 SC4 — no new test needed | EXISTING (Phase 7 SC4) |
| LAYO-05/06 | Sidebar width animation (visual only) | manual/visual | n/a | n/a |
| LAYO-07 | AeroSidebarScope item registration | unit (structure) | `./gradlew :library:test --tests "*.SidebarScopeTest"` | Wave 0 |
| LAYO-08 | AeroStepIndicator renders (visual) | manual/visual | verified in Phase 7 SC5 | EXISTING (Phase 7) |
| LAYO-09 | Wizard validate gate — Next blocked | unit (pure fn) | `./gradlew :library:test --tests "*.WizardStepTest"` | Wave 0 |
| LAYO-09 | Wizard step advance on valid | unit (pure fn) | `./gradlew :library:test --tests "*.WizardStepTest"` | Wave 0 |
| LAYO-09 | Wizard Back preserves step index | unit (pure fn) | `./gradlew :library:test --tests "*.WizardStepTest"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests "com.mordred.aero.components.layout.*"`
- **Per wave merge:** `./gradlew :library:test`
- **Phase gate:** Full suite green + 4-component visual review across 3 themes before Phase 11

### Wave 0 Gaps

- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/AccordionToggleTest.kt` — covers LAYO-02 (pure `accordionToggleSingle` / `accordionToggleMulti` functions)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/SplitClampTest.kt` — covers LAYO-03 (pure `clampDivider` + fraction conversion)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/WizardStepTest.kt` — covers LAYO-09 (validate gate, step advance, Back navigation logic)
- [ ] `library/src/test/kotlin/com/mordred/aero/components/layout/SidebarScopeTest.kt` — covers LAYO-07 (scope item registration / scope DSL structure — can be a unit test on the scope class without Compose)

---

## Sources

### Primary (HIGH confidence)
- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — actual Phase 7 aeroDragSplitter source, verified API signature and behavior
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` — actual Phase 7 AeroStepIndicator source, verified 0-based currentStep API and visual states
- `library/src/main/kotlin/com/mordred/aero/components/overlay/AeroTooltip.kt` — actual AeroTooltip source, verified wrapper form `AeroTooltip(text) { content }` API
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — confirmed 23 tokens, three preset values (AeroBlue/AeroDark/Classic)
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — confirmed `glassPanel`, `glassSurface`, `glassEffect` Modifier signatures
- `.planning/phases/10-layout/10-CONTEXT.md` — all locked API decisions for all four components
- `.planning/research/PITFALLS.md` lines 262–343, 401–410 — PITFALL-11 through PITFALL-14, performance traps table
- `.planning/research/STACK.md` §AeroAccordion, §AeroSplitPane, §AeroSidebar, §AeroStepperWizard — API table confirmed
- `.planning/research/ARCHITECTURE.md` §Pattern 5, §Pattern 6, §Phase 10 — component responsibilities, file structure

### Secondary (MEDIUM confidence)
- `.planning/ROADMAP.md` §Phase 10 — success criteria and phase notes confirm PITFALL references
- `.planning/STATE.md` §v2.0 Locked Decisions — detectDragGestures banned, additive milestone constraints

### Tertiary (LOW confidence)
- None — all critical claims are backed by HIGH confidence sources (project source files and prior verified research)

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all components and APIs verified in existing project source files
- Architecture: HIGH — PITFALL-11 through PITFALL-14 patterns drawn directly from PITFALLS.md + verified Phase 7 source
- Pitfalls: HIGH — sourced from PITFALLS.md (project's authoritative pitfall register) + actual Phase 7 source code
- Animation APIs: HIGH — `animateContentSize`, `animateDpAsState`, `animateFloatAsState` confirmed in STACK.md; `graphicsLayer { rotationZ }` pattern used in Phase 9 (TreeView caret)
- Test architecture: HIGH — established `kotlin.test` framework confirmed across 40+ existing test files

**Research date:** 2026-06-18
**Valid until:** 2026-07-18 (stable Compose Multiplatform 1.7.3 stack — low churn; primary concern is project-internal patterns which don't expire)
