# Architecture Research

**Domain:** Compose Desktop UI Component Library (Windows Aero visual style)
**Researched:** 2026-04-27
**Confidence:** HIGH (based on official Android docs + direct inspection of mordred source + Jewel reference architecture)

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     CONSUMER APPLICATION                         │
│   AeroTheme { /* all app content here */ }                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ CompositionLocalProvider
┌──────────────────────────▼──────────────────────────────────────┐
│                   THEME LAYER (aero-compose-ui)                  │
│                                                                  │
│  LocalAeroColors  LocalAeroTypography  LocalAeroShapes           │
│  (staticCompositionLocalOf — read-fast, rarely changes)          │
│                                                                  │
│  AeroColorScheme       AeroTypography        AeroShapes          │
│  (data class, @Immutable)  (data class)     (data class)         │
└──────────────────────────┬──────────────────────────────────────┘
                           │ AeroTheme.colors / AeroTheme.typography
┌──────────────────────────▼──────────────────────────────────────┐
│                  MODIFIER LAYER                                  │
│                                                                  │
│  Modifier.glassEffect()   Modifier.glassPanel()                  │
│  Modifier.glassSurface()                                         │
│  (extension functions on Modifier, consume theme colors)         │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  COMPONENT LAYER                                  │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │AeroButton│ │AeroCard  │ │AeroText  │ │AeroTitle │            │
│  │          │ │          │ │Field     │ │Bar       │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
│                                                                  │
│  Each component: stateless core + optional stateful wrapper      │
│  Wraps Material3 primitives, never replaces them                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ depends on
┌──────────────────────────▼──────────────────────────────────────┐
│                  SHOWCASE MODULE (separate Gradle module)         │
│                                                                  │
│  aero-compose-ui-showcase — Compose Desktop app                  │
│  Groups components by category, live theme switching             │
└─────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `AeroColorScheme` | All color tokens for one theme | `@Immutable data class`, ~30 named Color fields |
| `AeroTypography` | TextStyle tokens (body, title, label variants) | `@Immutable data class`, wraps `TextStyle` |
| `AeroShapes` | Corner radius tokens | `@Immutable data class`, wraps `Shape` values |
| `LocalAeroColors` | Implicit theme propagation | `staticCompositionLocalOf { AeroBlue }` |
| `AeroTheme { }` | Theme entry point composable | `CompositionLocalProvider` + `MaterialTheme` bridge |
| `AeroTheme` object | Accessor for current theme values | `@Composable get() = Local*.current` |
| Glass modifiers | Visual surface treatment | `Modifier` extension functions consuming `AeroColorScheme` |
| Components (`AeroButton` etc.) | Individual UI elements | Wrap Material3 primitives, read theme via `AeroTheme.colors` |
| Showcase app | Living documentation, visual testing | Separate Gradle module, `main()` entry point |

## Recommended Project Structure

```
aero-compose-ui/                     # root Gradle project
├── settings.gradle.kts              # includes :library and :showcase modules
├── build.gradle.kts                 # root build config (versions, publishing plugins)
│
├── library/                         # :library module — the published artifact
│   ├── build.gradle.kts             # kotlin("jvm") + compose + maven-publish
│   └── src/
│       └── main/kotlin/
│           └── com/mordred/aero/
│               ├── theme/
│               │   ├── AeroColorScheme.kt     # @Immutable data class + 3 presets
│               │   ├── AeroTypography.kt      # @Immutable data class
│               │   ├── AeroShapes.kt          # @Immutable data class
│               │   ├── AeroTheme.kt           # CompositionLocals + AeroTheme{} + accessor
│               │   └── GlassModifiers.kt      # glassEffect / glassPanel / glassSurface
│               │
│               ├── components/
│               │   ├── buttons/
│               │   │   ├── AeroButton.kt
│               │   │   ├── AeroOutlinedButton.kt
│               │   │   └── AeroIconButton.kt
│               │   ├── inputs/
│               │   │   ├── AeroTextField.kt
│               │   │   ├── AeroTextArea.kt
│               │   │   ├── AeroPasswordField.kt
│               │   │   ├── AeroNumberSpinner.kt
│               │   │   ├── AeroSearchField.kt
│               │   │   └── AeroFilePicker.kt
│               │   ├── selection/
│               │   │   ├── AeroCheckbox.kt
│               │   │   ├── AeroRadioButton.kt
│               │   │   ├── AeroSwitch.kt
│               │   │   ├── AeroChip.kt
│               │   │   └── AeroSegmentedControl.kt
│               │   ├── containers/
│               │   │   ├── AeroCard.kt
│               │   │   ├── AeroPanel.kt
│               │   │   ├── AeroAccordion.kt
│               │   │   ├── AeroGroupBox.kt
│               │   │   ├── AeroSplitPane.kt
│               │   │   ├── AeroScrollArea.kt
│               │   │   └── AeroScrollBar.kt
│               │   ├── navigation/
│               │   │   ├── AeroTitleBar.kt
│               │   │   ├── AeroMenuBar.kt
│               │   │   ├── AeroContextMenu.kt
│               │   │   ├── AeroStatusBar.kt
│               │   │   ├── AeroBreadcrumb.kt
│               │   │   ├── AeroSidebar.kt
│               │   │   └── AeroTabBar.kt
│               │   ├── overlays/
│               │   │   ├── AeroDialog.kt
│               │   │   ├── AeroAlertDialog.kt
│               │   │   ├── AeroToast.kt
│               │   │   ├── AeroDrawer.kt
│               │   │   ├── AeroPopover.kt
│               │   │   └── AeroTooltip.kt
│               │   ├── data/
│               │   │   ├── AeroDataTable.kt
│               │   │   ├── AeroTreeView.kt
│               │   │   ├── AeroProgressBar.kt
│               │   │   ├── AeroSlider.kt
│               │   │   ├── AeroRangeSlider.kt
│               │   │   └── AeroListItem.kt
│               │   ├── pickers/
│               │   │   ├── AeroDatePicker.kt
│               │   │   ├── AeroTimePicker.kt
│               │   │   ├── AeroDateTimePicker.kt
│               │   │   ├── AeroDateRangePicker.kt
│               │   │   ├── AeroColorPicker.kt
│               │   │   └── AeroComboBox.kt
│               │   └── misc/
│               │       ├── AeroDivider.kt
│               │       ├── AeroBadge.kt
│               │       ├── AeroTag.kt
│               │       ├── AeroToolbar.kt
│               │       └── AeroStepperWizard.kt
│               │
│               └── AeroComposeUi.kt           # public API surface / re-exports
│
└── showcase/                        # :showcase module — not published
    ├── build.gradle.kts             # compose desktop app, depends on :library
    └── src/
        └── main/kotlin/
            └── com/mordred/showcase/
                ├── Main.kt
                ├── ShowcaseApp.kt
                └── screens/
                    ├── ButtonsScreen.kt
                    ├── InputsScreen.kt
                    ├── ContainersScreen.kt
                    ├── NavigationScreen.kt
                    ├── OverlaysScreen.kt
                    ├── DataScreen.kt
                    └── PickersScreen.kt
```

### Structure Rationale

- **`library/` module:** Compiled as a JVM library (not an application), published as `com.mordred:aero-compose-ui`. Contains zero `main()` function, no showcase pages.
- **`showcase/` module:** A runnable Compose Desktop application (`compose { application { ... } }`). Depends on `:library` as a project dependency, not a Maven artifact — this gives live reload during development.
- **Component grouping by domain** (buttons, inputs, containers…): Mirrors the component list in PROJECT.md and makes PRs and reviews scoped. Each sub-package is ~1 file per component.
- **`theme/` package first:** Everything in `components/` imports from `theme/`. No circular dependencies.
- **`GlassModifiers.kt` inside `theme/`:** Glass modifiers consume color tokens directly, so they belong to the theme layer, not the component layer. Components call `Modifier.glassEffect(...)` — they do not duplicate the gradient logic.

## Architectural Patterns

### Pattern 1: staticCompositionLocalOf for Immutable Theme Tokens

**What:** Theme color/typography/shape tokens are provided via `staticCompositionLocalOf`, not `compositionLocalOf`.

**When to use:** When the value changes only on explicit user action (theme switch), not during normal animation or interaction. The theme itself is effectively static within a session.

**Trade-offs:** `staticCompositionLocalOf` invalidates the entire `content` lambda when the value changes (full recompose of all children of `AeroTheme {}`). `compositionLocalOf` tracks reads and only recomposes affected composables. For infrequent theme switches this is acceptable; the static variant has faster reads (no tracking overhead) which matters since every component reads colors on every frame.

**Decision for this project:** Use `staticCompositionLocalOf` for colors, typography, shapes — matching the approach in mordred's `LocalMordredColors` and Material3's own internals. If animated theme transitions become a requirement, switch `LocalAeroColors` to `compositionLocalOf` at that point.

```kotlin
// theme/AeroTheme.kt
val LocalAeroColors = staticCompositionLocalOf<AeroColorScheme> {
    AeroThemePresets.AeroBlue
}

val LocalAeroTypography = staticCompositionLocalOf<AeroTypography> {
    AeroTypography()
}

@Composable
fun AeroTheme(
    colorScheme: AeroColorScheme = AeroThemePresets.AeroBlue,
    content: @Composable () -> Unit
) {
    val materialColors = darkColorScheme(
        primary = colorScheme.primary,
        surface = colorScheme.surface,
        background = colorScheme.background,
        // ... bridge to Material3
    )
    CompositionLocalProvider(
        LocalAeroColors provides colorScheme,
        LocalAeroTypography provides AeroTypography(colorScheme)
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}

// Accessor object — clean call site
object AeroTheme {
    val colors: AeroColorScheme
        @Composable get() = LocalAeroColors.current
    val typography: AeroTypography
        @Composable get() = LocalAeroTypography.current
}
```

### Pattern 2: Modifiers for Glass Effects (Not Wrapper Composables)

**What:** Glass surface treatment is implemented as `Modifier` extension functions that receive `AeroColorScheme` (or read from `LocalAeroColors.current` internally).

**When to use:** When the visual effect does not add its own layout, children, or interaction semantics. Glass is paint on top of a layout, not a layout itself.

**Trade-offs:**
- **Modifier approach (recommended):** Caller controls layout structure (`Box`, `Column`, etc.). Effect is composable with other modifiers. No extra composable in the tree.
- **Wrapper composable approach:** `GlassCard { ... }` is intuitive for consumers but forces a specific layout hierarchy and duplicates layout code across `GlassCard`, `GlassPanel`, `GlassSurface`. The modifier approach keeps layout decisions with the caller.

**Concrete implementation:** Three modifiers (`glassEffect`, `glassPanel`, `glassSurface`) read from the color scheme. Components like `AeroCard` wrap a `Box` and apply `Modifier.glassEffect(AeroTheme.colors)` internally. The modifier functions themselves remain internal implementation detail of the component; consumers only see `AeroCard { }`.

```kotlin
// Internal modifier (theme/GlassModifiers.kt)
internal fun Modifier.glassEffect(
    colors: AeroColorScheme,
    cornerRadius: Dp = 8.dp,
    elevation: Dp = 4.dp
): Modifier = this
    .shadow(elevation, RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(listOf(
            colors.glassSurface,
            colors.glassSurface.copy(alpha = colors.glassSurface.alpha * 0.5f)
        )),
        RoundedCornerShape(cornerRadius)
    )
    .border(1.dp, colors.glassBorder, RoundedCornerShape(cornerRadius))

// Public component that uses the modifier
@Composable
fun AeroCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AeroTheme.colors
    Box(
        modifier = modifier.glassEffect(colors, cornerRadius),
        content = content
    )
}
```

**Note on true backdrop blur:** Compose Desktop on JVM does not expose a native `RenderEffect` API equivalent to Android 12's `BlurMaskFilter` backed by GPU. The mordred approach (gradient + transparency layers + border) correctly simulates the Aero aesthetic without requiring actual pixel blurring. If real blur is needed, the `haze` library (chrisbanes/haze) supports Compose Multiplatform including Desktop, but adds a dependency and changes the modifier contract significantly. Recommend starting without it.

### Pattern 3: State Hoisting with Stateless Core + Stateful Wrapper

**What:** Each component is written in two layers — a stateless core that receives all state as parameters, and an optional stateful convenience variant that owns internal state via `remember`.

**When to use:** Always for components that have interactive state (text content, checked state, expanded state, etc.). Stateless core = testable, composable, controllable by parent. Stateful wrapper = ergonomic for simple use cases.

**Trade-offs:** Two composables per component vs one. The cost is worth it: library consumers who integrate with a ViewModel need the stateless variant; consumers building quick demos use the stateful one.

```kotlin
// Stateless core — parent controls state
@Composable
fun AeroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false
) { /* implementation */ }

// Stateful convenience wrapper — owns state internally
@Composable
fun AeroTextField(
    initialValue: String = "",
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    onValueChange: ((String) -> Unit)? = null
) {
    var value by remember { mutableStateOf(initialValue) }
    AeroTextField(
        value = value,
        onValueChange = { value = it; onValueChange?.invoke(it) },
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled
    )
}
```

### Pattern 4: Slots API for Content-Flexible Components

**What:** Components that contain arbitrary child content expose `content: @Composable () -> Unit` (or typed slot parameters) as the last parameter, following Kotlin trailing lambda convention.

**When to use:** All container components (`AeroCard`, `AeroDialog`, `AeroAccordion`), components with icon slots (`AeroTextField` leading/trailing icons), components with action slots (`AeroDialog` buttons).

**Trade-offs:** More flexible than prop-based (`title: String`), allows icons, badges, or custom layouts in slots. The downside is no type enforcement on what goes in a slot.

```kotlin
@Composable
fun AeroDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit  // trailing slot last
) { /* implementation */ }
```

## Data Flow

### Theme Token Flow

```
AeroThemePresets.AeroBlue (object, compile-time constant)
    ↓ passed as parameter to
AeroTheme(colorScheme = AeroBlue) { ... }
    ↓ bridges to Material3
MaterialTheme(colorScheme = materialColors)
    ↓ also provides custom locals
CompositionLocalProvider(LocalAeroColors provides colorScheme)
    ↓ any composable in subtree reads via
AeroTheme.colors  →  LocalAeroColors.current
    ↓ passed to modifier or used directly
Modifier.glassEffect(colors)  /  colors.primary  /  colors.glassBorder
    ↓ rendered
DrawScope (Canvas)
```

### Theme Switch Flow (in Showcase)

```
User taps "AeroDark" in ShowcaseApp
    ↓ state update
var currentScheme by remember { mutableStateOf(AeroBlue) }
    ↓ triggers recompose of AeroTheme { } with new colorScheme
AeroTheme(colorScheme = AeroDark) { ... }
    ↓ staticCompositionLocalOf → entire content lambda recomposes
All components repaint with new colors
    ↓ single-frame visual update, no animation unless explicitly added
```

### Component State Flow (State Hoisting)

```
ViewModel / Parent Composable   ← holds authoritative state
    ↓ value + onValueChange
AeroTextField (stateless core)  ← renders, fires events up
    ↓ user types
onValueChange("new text")
    ↑ bubbles up
ViewModel updates state
    ↓ recompose
AeroTextField repaints with new value
```

## Build Order (What Must Exist Before What)

This order determines the development sequence for phases:

```
1. Theme foundation
   AeroColorScheme.kt  ──► everything depends on this
   AeroTypography.kt
   AeroShapes.kt
   AeroTheme.kt (CompositionLocals + provider + accessor)
        │
        ▼
2. Glass modifier layer
   GlassModifiers.kt   ──► AeroCard, AeroPanel, AeroButton borders need this
        │
        ▼
3. Primitive / atomic components (no dependencies on other Aero components)
   AeroButton, AeroOutlinedButton, AeroIconButton
   AeroDivider
   AeroBadge, AeroTag
   AeroTextField (basic)
        │
        ▼
4. Showcase skeleton
   ShowcaseApp.kt + theme switcher  ──► validates theme + atoms visually
        │
        ▼
5. Composite components (depend on atoms or each other)
   AeroCard (uses glassEffect modifier)
   AeroPanel (uses glassPanel modifier)
   AeroDialog (uses AeroCard + AeroButton)
   AeroScrollBar → AeroScrollArea
   AeroMenuBar → AeroContextMenu
        │
        ▼
6. Complex / stateful components
   AeroDataTable (sorting state, row selection)
   AeroTreeView (expand/collapse state, recursion)
   AeroDatePicker / AeroTimePicker (calendar logic)
   AeroColorPicker (HSV math, HEX parsing)
        │
        ▼
7. Window chrome
   AeroTitleBar  ──► requires FrameWindowScope, window-specific APIs
                     must be tested in a real Window, not previews
```

**Critical dependency:** `AeroTitleBar` requires `FrameWindowScope` from Compose Desktop. It cannot be used inside a plain `@Preview` or a non-Window composable. Build it last (or in its own sub-phase) because its test environment is more constrained.

**AeroScrollBar before AeroScrollArea:** `AeroScrollArea` composes `AeroScrollBar` internally. Build the bar primitive first.

## Anti-Patterns

### Anti-Pattern 1: Passing Color Scheme as a Parameter to Every Component

**What people do:**
```kotlin
@Composable
fun AeroButton(text: String, onClick: () -> Unit, colors: AeroColorScheme) { ... }
```

**Why it's wrong:** Creates prop drilling. Every composable in a hierarchy must receive and forward the color scheme. Changing the theme requires touching every call site.

**Do this instead:** Read from `AeroTheme.colors` inside the component. `CompositionLocal` exists precisely to eliminate this.

```kotlin
@Composable
fun AeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    // use colors here
}
```

### Anti-Pattern 2: Replacing Material3 Primitives Entirely

**What people do:** Write a from-scratch `Button` using `Canvas` or raw `Modifier.clickable` to avoid Material3 defaults.

**Why it's wrong:** Loses accessibility semantics (role, state descriptions for screen readers), loses ripple/indication behavior, loses disabled state handling. Extremely time-consuming to replicate correctly.

**Do this instead:** Wrap Material3 `Button`, `TextField`, `Checkbox`, etc. Override colors, shape, and borders via the `colors = ButtonDefaults.buttonColors(...)` pattern. This is what mordred already does correctly.

### Anti-Pattern 3: Using a Single Monolithic Color Data Class for Everything

**What people do:** Add every possible token to `AeroColorScheme` (30+ fields). When adding a new component that needs a new color, add it directly to the shared data class.

**Why it's wrong:** The data class becomes an implicit dependency of every component. Adding a field is a breaking API change for any consumer who constructs the class manually. Over time the class becomes hard to reason about.

**Do this instead:** Keep `AeroColorScheme` to semantic roles (primary, surface, background, glassSurface, glassBorder, glassHighlight, titleBarGradientStart/End, buttonHover, error…). Component-specific colors that are derivable (e.g., `primary.copy(alpha = 0.8f)`) stay inside the component. Only promote a color to `AeroColorScheme` if multiple unrelated components need the same named value.

The mordred `MordredColorScheme` already has good scope. Migrate it as-is; add new fields only when a component proves it needs a distinct named token.

### Anti-Pattern 4: Glass Modifiers Internally Reading CompositionLocal

**What people do:**
```kotlin
fun Modifier.glassEffect(): Modifier {
    val colors = LocalAeroColors.current  // ERROR: Modifier is not @Composable
    ...
}
```

**Why it's wrong:** `Modifier` extension functions are not `@Composable`. They cannot call `compositionLocalOf` reads. This is a compile error.

**Do this instead:** Either (a) pass `AeroColorScheme` explicitly as a parameter (the mordred pattern, which is correct), or (b) create a `@Composable` wrapper that reads the local and returns a configured `Modifier`:

```kotlin
// Option A (recommended — matches mordred): explicit parameter
fun Modifier.glassEffect(colors: AeroColorScheme, cornerRadius: Dp = 8.dp): Modifier = ...

// Option B: composable factory
@Composable
fun Modifier.glassEffect(cornerRadius: Dp = 8.dp): Modifier {
    val colors = LocalAeroColors.current
    return this.glassEffect(colors, cornerRadius)
}
```

Option A is already proven in mordred. Option B is acceptable but adds a `@Composable` annotation to modifier chain, which has implications for inline optimization. Start with Option A.

### Anti-Pattern 5: Putting Showcase Screens Inside the Library Module

**What people do:** Keep `ShowcaseApp.kt` and component demo screens in the same source set as the library to keep the project simple.

**Why it's wrong:** Showcase code and its dependencies (preview data, fake view models, hardcoded strings) get included in the published JAR. Consumers pull in dead code. Build times increase.

**Do this instead:** Separate `:library` and `:showcase` Gradle modules from the start. `:showcase` depends on `:library` as a project dependency (`implementation(project(":library"))`).

## Integration Points

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `theme/` → `components/` | One-way: components read `AeroTheme.colors`, modifiers in `GlassModifiers.kt` take `AeroColorScheme` parameter | No reverse dependency |
| `:library` → `:showcase` | Gradle project dependency only; showcase calls library public API | Showcase must not be imported by library |
| `AeroTheme {}` → `MaterialTheme` | AeroTheme wraps MaterialTheme, bridging Aero color tokens to Material3 slots | Material3 components inside AeroTheme get correct colors automatically |
| `AeroTitleBar` → Compose Desktop Window API | `FrameWindowScope.AeroTitleBar(...)` — extension function requiring window context | Cannot be used outside a `Window { }` composable |

### External Dependencies

| Dependency | Usage | Notes |
|------------|-------|-------|
| `androidx.compose.material3` | Base primitives: `Button`, `TextField`, `Checkbox`, etc. | Override styling, never replace entirely |
| `androidx.compose.foundation` | `BorderStroke`, `Brush`, `RoundedCornerShape`, `hoverable` | Core drawing primitives |
| `androidx.compose.ui` | `Modifier`, `CompositionLocal`, `TextStyle`, `Color` | Fundamental Compose APIs |
| `compose.desktop.currentOs` | Desktop window, `WindowDraggableArea`, `FrameWindowScope` | Required for AeroTitleBar |
| Kotlin JDK 17 | Language runtime | Constraint from PROJECT.md |

**Glass blur libraries (optional — not recommended initially):** `chrisbanes/haze` supports Compose Desktop and provides real backdrop blur. However, it changes the modifier contract (requires two cooperating composables: a `haze{}` parent and `hazeChild()` children) and adds a dependency. The mordred gradient simulation achieves the Aero aesthetic without it. Introduce only if consumer feedback demands real blur.

## Scaling Considerations

This is a library, not a user-facing service, so "scaling" means API surface growth and build time:

| Scale | Architecture Adjustments |
|-------|--------------------------|
| Phase 1 (foundation + 5-10 components) | Single `:library` module is fine, flat package structure acceptable |
| Phase 3+ (20-30 components) | Sub-packages per category (buttons/, inputs/, containers/) — already reflected in structure above |
| Phase 5+ (50 components, public release) | Consider splitting `:library` into `:library-core` (theme + glass) and `:library-components` if consumers want theme-only without all components. Not needed earlier. |
| Multiplatform expansion (Android) | `jvm()` target becomes `jvm("desktop")` + `androidTarget()`. `AeroTitleBar` moves to `desktopMain` source set. Premature until confirmed. |

## Sources

- Android Developers — Custom design systems in Compose: https://developer.android.com/develop/ui/compose/designsystems/custom
- Android Developers — Anatomy of a theme in Compose: https://developer.android.com/develop/ui/compose/designsystems/anatomy
- Android Developers — CompositionLocal documentation: https://developer.android.com/develop/ui/compose/compositionlocal
- Jewel (JetBrains) module reference: https://github.com/JetBrains/jewel — int-ui-standalone / jewel-ui / jewel-foundation split
- mordred source (C:/1A_WORK/lastver_131/mordred) — GlassModifiers.kt, MordredTheme.kt, ColorScheme.kt — inspected directly
- chrisbanes/haze (backdrop blur library): https://github.com/chrisbanes/haze
- droidcon — Static vs Dynamic CompositionLocals: https://www.droidcon.com/2025/10/21/jetpack-compose-static-vs-dynamic-compositionlocals-reads-writes-and-trade-offs/
- Stream.io — Designing Effective UI Components in Jetpack Compose: https://getstream.io/blog/designing-effective-compose/
- ProAndroidDev — Building a Design System with Jetpack Compose: https://proandroiddev.com/building-design-system-with-jetpack-compose-1208c250ae75

---
*Architecture research for: Compose Desktop UI Component Library (aero-compose-ui)*
*Researched: 2026-04-27*
