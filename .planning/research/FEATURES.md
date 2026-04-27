# Feature Research

**Domain:** Compose Desktop UI component library (Windows Aero aesthetic)
**Researched:** 2026-04-27
**Confidence:** MEDIUM-HIGH (Jewel/ecosystem HIGH; Aero-specific theming patterns MEDIUM)

---

## Feature Landscape

### Table Stakes (Users Expect These)

Features developers assume exist in any UI library. Missing these means they won't adopt it — no points for having them, automatic rejection for lacking them.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Theme provider + CompositionLocal | Every Compose library wraps a theme; developers call `AeroTheme {}` at root | LOW | `LocalAeroColors`, `LocalAeroShapes`, `LocalAeroTypography` via `staticCompositionLocalOf`. Pattern is identical to Material3. |
| Color token data class | Developers expect to swap colors without touching components | LOW | `AeroColorScheme` data class with named semantic slots (background, surface, onSurface, primary, onPrimary, border, etc.) |
| At least one working dark theme | Developers test in dark mode on day one | LOW | Three themes from mordred (AeroBlue, AeroDark, Classic) are already validated |
| Button (filled + outlined) | Every library has buttons | LOW | `AeroButton`, `AeroOutlinedButton`. Hover/pressed/disabled states required. |
| Text input (single-line) | Every library has text input | MEDIUM | `AeroTextField` with focus ring animation, placeholder, error state |
| Checkbox | Every library has checkbox | LOW | Including tri-state (indeterminate) — Jewel confirmed tri-state is expected |
| RadioButton / RadioGroup | Paired selection control | LOW | Single-value selection pattern |
| Dropdown / Select | Non-editable option picker | MEDIUM | `AeroDropdown`. Popup positioning on desktop is non-trivial |
| Slider | Range input | MEDIUM | Single handle, min/max/step |
| Progress indicator | Async feedback | LOW | Determinate + indeterminate variants |
| Dialog (modal) | Blocking confirmation/error UI | MEDIUM | Focus trap, keyboard Escape to close |
| Tooltip | Desktop hover affordance — no mobile equivalent, desktop users expect it | LOW | Desktop-only API via `TooltipArea` |
| Context menu | Right-click interaction — desktop expectation, not mobile | MEDIUM | Compose Desktop has `ContextMenuArea`; needs Aero styling |
| Scrollable area with styled scrollbar | Custom scrollbars are a visual regression if unstyled | MEDIUM | Compose Desktop `VerticalScrollbar` / `HorizontalScrollbar` need Aero skin |
| Keyboard navigation (Tab / Arrow) | Desktop apps must be keyboard-operable; missing this feels broken | MEDIUM | `focusable()`, `focusRequester`, tab order via `focusProperties` |
| Consistent hover states on all interactive components | Mouse cursor is always present on desktop; hover is a primary affordance | MEDIUM | `hoverable()` + `Modifier.onHover`; every interactive component needs it |
| Showcase / demo application | Developers evaluate libraries visually before adopting | MEDIUM | Separate `aero-compose-ui-showcase` module showing all components live |
| Maven/Gradle dependency setup (one line) | `implementation("com.mordred:aero-compose-ui:x.y.z")` just works | LOW | Local Maven publish to start; clean Gradle module structure |
| Custom window title bar | Desktop-specific; without it the Aero aesthetic breaks immediately | HIGH | `WindowDraggableArea`, undecorated window, min/max/close buttons, OS snap workaround |

---

### Differentiators (Competitive Advantage)

Features that set aero-compose-ui apart. Not expected, but valued. Should align with core value proposition: "connect one dependency, get a complete Aero Windows 7 aesthetic."

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Glass surface modifiers (`glassEffect`, `glassPanel`, `glassSurface`) | The entire Aero identity — gradient layering with alpha simulates frosted glass without native DWM dependency | MEDIUM | Already implemented in mordred's `GlassModifiers.kt`. Gradient-based (no native blur needed). Cloudy lib (Skia BlurEffect on JVM) is an option for true blur if desired. |
| Three pre-built themes (AeroBlue, AeroDark, Classic) | Developer gets a polished palette without any design work | LOW | All three already validated in production (mordred). Port as-is. |
| `AeroTitleBar` with gradient + system button styling | Complete Win7 window chrome replication in Compose | HIGH | Requires undecorated window, `WindowDraggableArea`, custom min/max/close rendering. Note: Aero Snap won't work with undecorated windows on Windows — document this limitation. |
| `AeroMenuBar` + `AeroContextMenu` with themed popups | System menu bar is not themed; these components extend the aesthetic top-to-bottom | HIGH | Compose Desktop `MenuBar` composable handles system integration; Aero styling layered on top |
| `AeroDataTable` with sortable headers | Jewel has `SelectableLazyColumn` but no styled data grid with sorting; this is a gap | HIGH | Sort icons, header glass styling, row hover state |
| `AeroTreeView` | Jewel has `BasicLazyTree` unstyled; a styled Aero tree is a gap | MEDIUM | Expand/collapse animation, connector lines optional |
| `AeroDatePicker` / `AeroDateRangePicker` | No Compose Desktop date picker exists in Jewel or Material3 desktop | HIGH | Full calendar grid, month/year navigation, range selection variant |
| `AeroColorPicker` | Rare in Compose Desktop ecosystem | HIGH | Palette grid, RGB sliders, hex input — useful for design/tool apps |
| `AeroStepperWizard` | No equivalent in Jewel | MEDIUM | Step indicator, animated progress between steps |
| `AeroRangeSlider` | Material3 adds RangeSlider on Android, but Compose Desktop support is less mature | MEDIUM | Two handles, non-overlapping constraint |
| `AeroSplitPane` | Jewel lacks a resizable split pane | MEDIUM | Drag divider, min size constraints |
| Theme customization API | Extend base themes with project-specific accent colors | LOW | `AeroColorScheme.copy(primary = MyColor)` pattern. Zero learning curve for Material3 developers. |
| Per-animation approval workflow | Explicit control over which hover/transition animations ship | LOW (process) | Not a technical feature — a development constraint that prevents scope creep and keeps animations coherent |

---

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems in v1. Document explicitly to prevent scope creep.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| True OS-level blur (DWM Aero Glass) | Authentic Win7 glass effect uses real background blur | Requires JNI/native interop into Windows DWM API, breaks Linux/macOS targets, creates platform-detection complexity, crashes if DWM unavailable | Gradient + alpha simulation (already in mordred) is visually convincing and cross-platform. Document it as "Aero-inspired" not "DWM-backed". `ComposeWindowStyler` has `WindowBackdrop.Aero` if native is ever needed — expose as opt-in, not default. |
| Full i18n / RTL support | Seems responsible to include | Doubles the testing surface, RTL layout logic is complex in Compose, none of the source app (mordred) uses it | Leave to consuming app. Components should not hardcode LTR assumptions, but don't build RTL scaffolding in v1. |
| Android / iOS targets | "Make it multiplatform" | The Aero aesthetic is desktop-only by design; glass effects and window chrome have no mobile counterpart; Android/iOS targets double build complexity | Desktop-only. State this explicitly in README. |
| Web (WASM/JS) target | "Support web too" | Same reasoning as mobile; window chrome literally does not exist on web | Out of scope. |
| Built-in i18n string resources | Tooltips, ARIA labels, dialog button labels in multiple languages | Requires resource system, translation infrastructure, locale detection | Provide English defaults; expose string parameters (e.g., `confirmLabel: String = "OK"`) for consumer override. |
| Annotation-processor based showcase (Showkase-style) | Auto-generate previews | Adds KSP/KAPT dependency, increases compile times, fragile with Compose Desktop | Hand-authored showcase app with explicit categories is faster to ship and easier to control. |
| Versioned design token system (DTCG format) | Enterprise teams want exportable tokens | Over-engineering for v1; token export requires tooling that doesn't exist in Compose ecosystem | `AeroColorScheme` data class is readable by Kotlin devs. Export/import can be v2. |
| Storybook-equivalent interactive docs | Rich documentation site | Requires significant infrastructure; Compose Desktop has no equivalent to React Storybook | Showcase app + KDoc/README is sufficient for v1. |
| Animated page/screen transitions | "It would look cool" | Not a component library responsibility; belongs in navigation layer | Document that transitions should be handled by the consuming app's navigation. `AeroDrawer` and `AeroAccordion` animations are in-component only. |
| Semantic HTML / ARIA accessibility compliance | Libraries often promise full a11y | Compose Desktop does not have a screen reader API comparable to Android's TalkBack or web ARIA on all desktop platforms (Windows accessibility is via UIA, not consistently exposed through Compose) | Use Compose `semantics {}` correctly for what IS supported. Don't promise WCAG compliance. |

---

## Feature Dependencies

```
AeroTheme provider
    └──requires──> AeroColorScheme (data class)
                       └──required by──> ALL components

GlassModifiers (glassEffect, glassPanel, glassSurface)
    └──required by──> AeroCard, AeroPanel, AeroButton, AeroTitleBar, AeroDialog

AeroTitleBar
    └──requires──> Undecorated Window (consumer config)
    └──requires──> WindowDraggableArea (Compose Desktop API)

AeroContextMenu
    └──requires──> ContextMenuArea (Compose Desktop API)
    └──enhances──> AeroTextField (right-click copy/paste)
    └──enhances──> AeroListItem (right-click actions)

AeroDialog
    └──requires──> AeroButton (action buttons inside dialog)
    └──enhances──> AeroAlertDialog (specialized variant)

AeroDropdown / AeroComboBox
    └──requires──> Popup positioning (Compose Desktop)
    └──requires──> AeroScrollArea (for long option lists)

AeroDatePicker
    └──requires──> AeroButton (navigation arrows)
    └──requires──> AeroDropdown (month/year select)
    └──enhances──> AeroDateRangePicker (two-picker variant)
    └──enhances──> AeroDateTimePicker (combined with AeroTimePicker)

AeroDataTable
    └──requires──> AeroScrollArea (horizontal + vertical scroll)
    └──requires──> AeroCheckbox (row selection)

AeroTreeView
    └──requires──> AeroScrollArea
    └──requires──> AeroIconButton (expand/collapse trigger)

AeroColorPicker
    └──requires──> AeroSlider (RGB channels)
    └──requires──> AeroTextField (hex input)

AeroStepperWizard
    └──requires──> AeroButton (Next/Back/Finish)

AeroNotificationBanner
    └──enhances──> AeroToast (same styling, different behavior)

AeroSplitPane
    └──requires──> AeroScrollArea (for each pane)

Showcase App
    └──requires──> ALL components above
    └──requires──> AeroTheme (theme switcher)
    └──requires──> AeroTabBar or AeroSidebar (navigation between sections)
```

### Dependency Notes

- **GlassModifiers are a foundation dependency:** They must be stable before any glass-surfaced component (Button, Card, TitleBar, Dialog) is built. Implement and freeze the modifier API in phase 1.
- **AeroScrollArea before complex components:** DataTable, TreeView, ComboBox dropdowns all need a working styled scrollbar. AeroScrollArea/AeroScrollBar must be available early.
- **AeroButton before dialogs and wizards:** Dialogs contain buttons. Build buttons first.
- **AeroDropdown before DatePicker:** The date picker uses dropdowns for month/year. DatePicker is a late-phase component.
- **Undecorated window is a consumer responsibility:** AeroTitleBar does not set the window to undecorated — the consuming app must do this. The component only renders the chrome. Document this clearly.

---

## MVP Definition

### Launch With (v1 — validate the library exists and is usable)

- [ ] AeroTheme provider + AeroColorScheme — without this, nothing else works
- [ ] GlassModifiers (glassEffect, glassPanel, glassSurface) — the core visual identity
- [ ] Three themes (AeroBlue, AeroDark, Classic) — the immediate differentiator
- [ ] AeroButton + AeroOutlinedButton + AeroIconButton — every app needs buttons
- [ ] AeroTextField + AeroPasswordField — every app needs text input
- [ ] AeroCheckbox + AeroRadioButton/Group + AeroSwitch — selection controls
- [ ] AeroDropdown + AeroComboBox — required for most form UIs
- [ ] AeroSlider + AeroProgressBar — range and feedback controls
- [ ] AeroCard + AeroPanel + AeroDivider — layout surfaces
- [ ] AeroScrollArea + AeroScrollBar — required for containers
- [ ] AeroDialog + AeroAlertDialog + AeroTooltip — overlays and feedback
- [ ] AeroToast/Snackbar + AeroNotificationBanner — notifications
- [ ] AeroTitleBar (custom window chrome) — the Aero identity centerpiece
- [ ] AeroMenuBar + AeroContextMenu — native desktop interaction
- [ ] AeroTabBar + AeroStatusBar — structural navigation
- [ ] Showcase app with all above components, theme switcher — required for library evaluation

### Add After Validation (v1.x)

- [ ] AeroDataTable — high complexity, high value; add when core is stable
- [ ] AeroTreeView — medium complexity; add when scroll/hover patterns are proven
- [ ] AeroDatePicker + AeroTimePicker — depends on dropdown being stable
- [ ] AeroDateRangePicker + AeroDateTimePicker — depends on DatePicker
- [ ] AeroColorPicker — depends on slider and text field
- [ ] AeroNumberSpinner + AeroSearchField + AeroFilePicker — input variants
- [ ] AeroChip + AeroSegmentedControl + AeroBadge/Tag — secondary controls
- [ ] AeroAccordion + AeroGroupBox + AeroSplitPane — advanced containers
- [ ] AeroSidebar + AeroBreadcrumb — navigation components
- [ ] AeroDrawer + AeroPopover — advanced overlays
- [ ] AeroStepperWizard + AeroListItem — specialized components

### Future Consideration (v2+)

- [ ] True native DWM blur via ComposeWindowStyler integration (opt-in, Windows-only)
- [ ] Animation preset library (predefined easing curves for common transitions)
- [ ] Design token export tooling
- [ ] Maven Central publication

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| AeroTheme + AeroColorScheme | HIGH | LOW | P1 |
| GlassModifiers | HIGH | LOW | P1 |
| Three default themes | HIGH | LOW | P1 |
| AeroButton variants | HIGH | LOW | P1 |
| AeroTextField | HIGH | MEDIUM | P1 |
| AeroCheckbox/Radio/Switch | HIGH | LOW | P1 |
| AeroDropdown/ComboBox | HIGH | MEDIUM | P1 |
| AeroTitleBar | HIGH | HIGH | P1 |
| AeroDialog/AlertDialog | HIGH | MEDIUM | P1 |
| AeroScrollArea/ScrollBar | HIGH | MEDIUM | P1 |
| AeroTooltip | HIGH | LOW | P1 |
| AeroContextMenu | HIGH | MEDIUM | P1 |
| Showcase App | HIGH | MEDIUM | P1 |
| AeroDataTable | HIGH | HIGH | P2 |
| AeroTreeView | MEDIUM | MEDIUM | P2 |
| AeroDatePicker | MEDIUM | HIGH | P2 |
| AeroSlider/RangeSlider | MEDIUM | MEDIUM | P2 |
| AeroProgressBar | MEDIUM | LOW | P1 |
| AeroCard/Panel | HIGH | LOW | P1 |
| AeroMenuBar | MEDIUM | HIGH | P2 |
| AeroAccordion/SplitPane | MEDIUM | MEDIUM | P2 |
| AeroColorPicker | LOW | HIGH | P3 |
| AeroStepperWizard | LOW | MEDIUM | P3 |
| AeroDrawer | MEDIUM | MEDIUM | P2 |
| AeroSidebar/Breadcrumb | MEDIUM | MEDIUM | P2 |

**Priority key:**
- P1: Must have for v1 launch
- P2: Add in v1.x after core validation
- P3: Future consideration

---

## Competitor Feature Analysis

| Feature | Jewel (JetBrains) | compose-jetbrains-theme (ButterCam) | aero-compose-ui (this) |
|---------|-------------------|-------------------------------------|------------------------|
| Theme system | IntelliJ Int UI + Swing bridge | Light/Dark JetBrains style | Custom 3-theme Aero with color tokens |
| Custom title bar | DecoratedWindow (JBR-required) | JBWindow (JBR or standard Java) | AeroTitleBar (undecorated + WindowDraggableArea) |
| Glass effects | None | None | Core identity via GlassModifiers |
| Button | Yes (DefaultSlimButton, IconButton, SplitButton) | Not documented | AeroButton, AeroOutlinedButton, AeroIconButton |
| TextField / TextArea | Yes | Not documented | AeroTextField, AeroTextArea, AeroPasswordField |
| Checkbox / Radio | Yes | Not documented | AeroCheckbox, AeroRadio, AeroSwitch |
| Combobox | Yes (ListComboBox, EditableComboBox) | Not documented | AeroDropdown, AeroComboBox |
| Tree | BasicLazyTree (unstyled foundation) | Not documented | AeroTreeView (fully styled) |
| Data table | SelectableLazyColumn (no headers/sort) | Not documented | AeroDataTable (with sort, row select) |
| Date picker | None | None | AeroDatePicker (differentiator) |
| Color picker | None | None | AeroColorPicker (differentiator) |
| Banner / notification | Yes (4 variants) | Not documented | AeroNotificationBanner, AeroToast |
| Tooltip | Yes | Not documented | AeroTooltip |
| Context menu | Yes | Not documented | AeroContextMenu |
| Showcase app | Sample apps in repo | Not documented | aero-compose-ui-showcase (separate module) |
| Target aesthetic | IntelliJ IDE look | IntelliJ IDE look | Windows 7 Aero glass |
| Maven publish | Yes (Maven Central) | Not documented | Local Maven (v1), Maven Central (v2+) |
| Accessibility | Good (Jewel invests in it) | Unknown | Compose semantics for what is supported |

---

## Sources

- [Jewel JetBrains Compose Desktop UI framework (moved to intellij-community)](https://github.com/JetBrains/jewel)
- [Jewel at intellij-community](https://github.com/JetBrains/intellij-community/tree/master/platform/jewel)
- [Jewel Release Notes](https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/RELEASE%20NOTES.md)
- [ButterCam compose-jetbrains-theme](https://github.com/ButterCam/compose-jetbrains-theme)
- [ComposeWindowStyler (Aero/Mica/Acrylic window backdrop)](https://github.com/MayakaApps/ComposeWindowStyler)
- [Cloudy — blur/glass library for Compose Multiplatform (Skia on JVM Desktop)](https://github.com/skydoves/Cloudy)
- [Compose Desktop — desktop-only APIs (context menus, tooltips, MenuBar, Tray)](https://kotlinlang.org/docs/multiplatform/compose-desktop-components.html)
- [Custom design systems in Compose — CompositionLocal pattern](https://developer.android.com/develop/ui/compose/designsystems/custom)
- [Building a custom Design System in Compose Multiplatform](https://proandroiddev.com/building-a-custom-design-system-in-compose-multiplatform-6f5f42f06fa0)
- [WindowDraggableArea for undecorated windows](https://www.sasikanth.dev/dragging-undecorated-windows-in-compose-desktop/)
- [Jewel UI Framework overview (DeepWiki)](https://deepwiki.com/JetBrains/intellij-community/6.2-jewel-ui-framework)
- [Compose API Guidelines for component design](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md)

---
*Feature research for: Compose Desktop UI component library (Windows Aero aesthetic)*
*Researched: 2026-04-27*
