# aero-compose-ui

A Windows 7 **Aero**–styled UI component library for **Compose Multiplatform** (Desktop / JVM).
Glossy gradients, glass surfaces, rounded depth — the classic Aero look, built as idiomatic Compose composables.

> Package: `com.mordred.aero` · Kotlin `2.1.21` · Compose Multiplatform `1.7.3` · JVM 17

---

## Features

- **49 `Aero*` composables** across 13 categories — buttons, inputs, containers, overlays, navigation, data tables, pickers, and more.
- **~140 built-in vector icons** (`AeroIcons`), no external icon dependency.
- **3 ready-made color schemes** — `AeroBlue`, `AeroDark`, `Classic` — plus `copy()` for custom palettes.
- **Themeable** through a single `AeroTheme {}` provider that also bridges values into Material 3.
- **Explicit public API** (`explicitApi()`) — every exported symbol is intentional and documented.
- **Stateful, layout-aware components** — accordions, split panes, stepper wizards, tree views, data tables with sorting/selection.
- Runnable **desktop showcase** demonstrating every component.

## Component categories

| Category    | Examples |
|-------------|----------|
| Buttons     | `AeroButton`, `AeroOutlinedButton`, `AeroIconButton`, `AeroToolbar` |
| Inputs      | `AeroTextField`, `AeroTextArea`, `AeroSearchField`, `AeroPasswordField`, `AeroNumberSpinner`, `AeroFilePicker` |
| Containers  | `AeroCard`, `AeroPanel`, `AeroGroupBox`, `AeroDivider`, `AeroScrollArea`, `AeroScrollBar` |
| Dropdowns   | `AeroDropdown`, `AeroComboBox` |
| Layout      | `AeroAccordion`, `AeroSidebar`, `AeroSplitPane`, `AeroStepperWizard` |
| Navigation  | `AeroTitleBar`, `AeroMenuBar`, `AeroTabBar`, `AeroBreadcrumb`, `AeroStatusBar` |
| Overlays    | `AeroDialog`, `AeroAlertDialog`, `AeroDrawer`, `AeroPopover`, `AeroContextMenu`, `AeroNotificationBanner` |
| Data        | `AeroDataTable`, `AeroTreeView` |
| Pickers     | date / time / value pickers |
| Range       | sliders & range controls |
| Selection   | checkboxes, radios, toggles, switches |
| List        | `AeroListItem`, `AeroBadge` |
| Icons       | `AeroIcons.*` (~140 glyphs) |

## Project structure

```
aero-compose-ui/
├── library/     # the component library (published artifact)
├── showcase/    # desktop demo app showing every component
├── gradle/      # version catalog (libs.versions.toml)
└── settings.gradle.kts
```

## Getting started

### Run the showcase

```bash
./gradlew :showcase:run
```

### Add as a dependency (JitPack)

Add the JitPack repository to your `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then declare the dependency:

```kotlin
dependencies {
    implementation("com.github.Tolaseeq:aero-compose-ui:v2.0.0")
}
```

Your consuming module also needs the Compose Multiplatform plugin applied (the library
exposes Compose types in its public API). [See available versions on JitPack →](https://jitpack.io/#Tolaseeq/aero-compose-ui)

### Usage

Wrap your UI in `AeroTheme` and use the components:

```kotlin
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.AeroColorScheme
import com.mordred.aero.components.buttons.AeroButton

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "My Aero App") {
        AeroTheme(colorScheme = AeroColorScheme.AeroBlue) {
            AeroButton(onClick = { /* ... */ }) {
                // content
            }
        }
    }
}
```

### Theming

`AeroTheme` exposes the active palette and typography at any call site:

```kotlin
val accent = AeroTheme.colors.primary
val titleStyle = AeroTheme.typography.title
```

Switch schemes by passing a different `AeroColorScheme`, or derive your own:

```kotlin
val myScheme = AeroColorScheme.AeroBlue.copy(primary = Color(0xFF1565C0))
AeroTheme(colorScheme = myScheme) { /* ... */ }
```

## Building & testing

```bash
./gradlew build        # compile everything
./gradlew :library:test  # run the library test suite
```

## Tech stack

- **Kotlin** 2.1.21 (JVM toolchain 17)
- **Compose Multiplatform** 1.7.3 (Desktop)
- **Material 3** (bridged under the hood)
- kotlinx-coroutines · kotlinx-datetime
- Tests: JUnit 5

## License

_TBD._
