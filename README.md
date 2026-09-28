# aero-compose-ui

A Windows 7 **Aero**–styled UI component library for **Compose Multiplatform** (Desktop / JVM).
Glossy gradients, glass surfaces, rounded depth — the classic Aero look, built as idiomatic Compose composables.

> Package: `com.mordred.aero` · Kotlin `2.4.20` · Compose Multiplatform `1.12.0` · JVM 21

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
    implementation("com.github.Tolaseeq:aero-compose-ui:v3.2.0")
}
```

Your consuming module also needs the Compose Multiplatform plugin applied (the library
exposes Compose types in its public API). [See available versions on JitPack →](https://jitpack.io/#Tolaseeq/aero-compose-ui)

**Toolchain requirement.** `v3.2.0` requires **Java 21**, **Compose Multiplatform 1.12**,
**Kotlin 2.4.20** and **kotlinx-datetime 0.8** (the library exposes kotlinx-datetime types in its
public API; `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock` no longer exist — use
`kotlin.time.Instant` / `kotlin.time.Clock`). A project on JDK 17 cannot resolve this artifact
(Gradle reports the `org.gradle.jvm.version` mismatch). `v3.1.0` has the same toolchain floor and
is the last release without native window management. If your project is on Kotlin 2.4.10 +
Compose Multiplatform 1.11.1 + Java 17, stay on **`v3.0.0`**; if it's still on Compose 1.7.3, stay
on **`v2.0.4`** — both lines remain functional and are the last releases before their respective
migrations.

## Windows window behavior

Since `v3.2.0` a window built on `AeroTitleBar` (+ `AeroResizeHandles`) is a native window to
Windows: the library tells the OS where the caption, the caption buttons and the window edges
are, and Windows itself owns dragging, snapping, resizing and maximizing. The library contains
no snapping logic of its own, and no code change is needed in an existing app — every window on
`AeroTitleBar` gets the behavior. On Linux and macOS nothing changes: those windows keep the
Compose-driven drag and resize exactly as before.

### What works

With the default `AeroTitleBar(...)` composition on Windows:

- **Drag snapping** — drag the title bar to the left or right screen edge (snap to half), to a
  corner (quarter) or to the top (maximize); dragging a snapped or maximized window away
  restores its former size.
- **Snap Layouts flyout** — hover the maximize button and Windows 11 shows its layout menu.
- **Maximize-button press parity** — pressed, the maximize button shows the same fill as the
  minimize button (pixel-exact on the standard JDK 21; its hover and click parity hold on
  both runtimes).
- **Win+arrow keys** — Win+← / Win+→ snap the window to a half, Win+↑ maximizes it, Win+↓
  restores and minimizes it.
- **Native resize** — the window resizes from every edge and corner with the system cursors,
  including narrow (~300 px) windows; `AeroResizeHandles` composes nothing on this path.
- **Taskbar-aware maximize** — a maximized window fills its monitor's work area exactly: it
  does not cover a visible taskbar, and leaves the edge an auto-hidden taskbar needs to slide
  out on hover.
- **Minimum size floor** — the window cannot be resized below its minimum (see
  [Minimum window size](#minimum-window-size)).
- **Multiple windows** — several such windows in one app work independently: open, drag, snap
  and close them in any order; closing one leaves the rest unaffected.
- **FancyZones** — Shift-drag places the window into PowerToys FancyZones zones.
- **Alt+F4** — closes the window through the same `onCloseRequest` path as the close button.

This is the verified behavior on Windows 11 24H2. The Win+arrow keys, edge and corner
resizing, and the minimum-size floors were verified on both the standard JDK 21 (the
supported runtime) and the JetBrains Runtime 21; drag snapping and maximize-button press
parity were verified on the standard JDK 21, and the caption double-click on the JetBrains
Runtime 21. Known gaps in this release: the caption double-click did not fire on the standard
JDK in the re-verification run (an unexplained difference between the runtimes), the Alt+Space
system menu opens (JetBrains Runtime 21) but its commands do not yet act on the window, two
snapped windows sharing their common border while resizing remains unverified (the
verification run's own pre-snap step was blocked by an environment issue), the top-left
corner resize read inert in the re-verification run (the other three corners are verified on
both runtimes), choosing a layout in the Snap Layouts flyout and the taskbar's Snap Groups
thumbnail have not been verified, and under the JetBrains Runtime dragging the window by its
caption still does not move it in verification runs (a runtime-specific drift — the same
window's double-click, Win+arrow keys and edge drags do work). Windows 10 and display scaling
other than 100 % are not verified (next section).

### Windows 10 and 11

The Snap Layouts flyout, Snap Groups and rounded window corners are Windows 11 shell features
and do not exist on Windows 10; everything else above is standard Windows window behavior
present on both systems (the library deliberately asks Windows 11 not to round the corners —
Aero windows keep square corners). This release was verified on Windows 11 24H2 only: Windows
10 is untested. All verification also ran at 100 % display scaling on a single monitor, so
other scales and monitor-to-monitor moves are not verified either.

### Interactive content over the title bar

The `leading` slot of `AeroTitleBar` is clickable as-is. Any other element drawn over the
title bar (overlays, counters, custom actions) must be marked so it receives clicks instead
of dragging the window:

```kotlin
// an interactive element overlaid on the title bar row
Box(
    Modifier
        .align(Alignment.TopEnd)
        .padding(top = 5.dp, end = 160.dp)
        .markAeroTitleBarInteractive()  // receives clicks — does not start a window drag
        .clickable { /* ... */ }
) {
    Text("Return queue")
}
```

### Opting out per window

Pass `nativeWindowManagement = false` to get exactly the behavior from before native window
management for that window — Compose drag (`WindowDraggableArea`), Compose resize zones, no
native snapping:

```kotlin
AeroTitleBar(
    title = "My App",
    windowState = state,
    onCloseRequest = ::exitApplication,
    nativeWindowManagement = false
)
```

The same parameter exists on `rememberAeroWindowChrome`. It has no effect on Linux/macOS.

### Minimum window size

A window cannot be resized below its minimum: the value your app sets through the standard
AWT `window.minimumSize`, or — when the app sets none — a floor of 320 × 240 dp (applied in
physical pixels at the window's current DPI on Windows). The library never overwrites an
app-set minimum, and the same rule applies to the Compose-side resize zones on Linux/macOS.

### The JNA dependency

Native window management uses JNA 5.19.1 (`net.java.dev.jna:jna` + `jna-platform`), added as
an internal `implementation` dependency of the library: it arrives transitively at runtime,
stays off your compile classpath, and no JNA type appears in the library's public API. An app
that already depends on JNA 5.19.1 resolves a single version with no conflict. No JetBrains
Runtime is needed — the standard JDK 21 is the supported runtime; on JDK 24+ the JVM may print
a native-access warning when JNA loads (not applicable to the supported JDK 21).

### Custom title bar

Apps that draw their own header instead of `AeroTitleBar` get the same native behavior
through `rememberAeroWindowChrome`: mark the draggable caption band, the maximize button and
the clickable elements, and read the button's hover/pressed state.

```kotlin
val chrome = rememberAeroWindowChrome(windowState)

Row(
    Modifier
        .fillMaxWidth()
        .height(40.dp)
        .then(with(chrome) { Modifier.captionArea() }),  // Windows owns drag + snap here
    verticalAlignment = Alignment.CenterVertically
) {
    Text("My App", Modifier.weight(1f).padding(start = 12.dp))
    AeroTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = "Search",
        modifier = Modifier
            .width(140.dp)
            .then(with(chrome) { Modifier.captionExclude() })  // stays clickable/focusable
    )
    AeroIconButton(
        onClick = { /* ... */ },
        modifier = Modifier.then(with(chrome) { Modifier.maximizeButtonArea() }),
        interactionSource = chrome.maximizeInteractionSource
    ) {
        Icon(AeroIcons.Square, contentDescription = "Maximize")
    }
}
```

- `captionArea()` tells Windows the pixels are the caption (`HTCAPTION`) and it owns dragging
  and snapping there; several caption areas are allowed (e.g. left and right of a centered
  search field).
- `captionExclude()` marks clickable content inside a caption area — it keeps its ordinary
  Compose input instead of starting a window drag.
- `maximizeButtonArea()` marks the maximize button (`HTMAXBUTTON`): hovering it shows the Snap
  Layouts flyout, and a click toggles `windowState.placement` between `Maximized` and
  `Floating`. One maximize area per window — the last one laid out wins.
- `chrome.maximizeInteractionSource` (or `chrome.maximizeHovered` / `chrome.maximizePressed`)
  exposes the button's native hover/press state, so its visuals reflect native input, not
  just Compose events.
- `chrome.isNative == false` means the native wiring is not active (non-Windows OS,
  `nativeWindowManagement = false`, or a failed install): keep your own drag handling there —
  e.g. `WindowDraggableArea` — exactly as before.

`AeroTitleBar` itself is built on this API.

## Usage

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

## Theming

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

- **Kotlin** 2.4.20 (JVM toolchain 21)
- **Compose Multiplatform** 1.12.0 (Desktop)
- **Material 3** 1.9.0 (pinned)
- kotlinx-coroutines 1.11.0 · kotlinx-datetime 0.8.0
- JNA 5.19.1 (Windows window management, internal)
- Tests: JUnit 6

## License

_TBD._
