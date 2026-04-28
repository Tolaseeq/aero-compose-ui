# Roadmap: aero-compose-ui

## Overview

Three phases deliver a complete Compose Desktop UI component library styled after Windows Aero. Phase 1 establishes the theme system and glass modifiers — everything else depends on it. Phase 2 builds all atomic components (buttons, inputs, selection, sliders, lists) that define reusable interaction patterns. Phase 3 completes the library with composite containers, overlays, and window chrome components that depend on atomic primitives. The showcase application grows with each phase so the library is always demonstrable.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation** - Theme system, glass modifiers, module structure, showcase skeleton (completed 2026-04-27)
- [x] **Phase 2: Atomic Components** - Buttons, inputs, selection controls, sliders, list items, badges (completed 2026-04-28)
- [ ] **Phase 3: Composite + Navigation** - Containers, overlays, dialogs, menus, tabs, window chrome

## Phase Details

### Phase 1: Foundation
**Goal**: The library module exists with a complete theme system, glass modifiers, and explicit public API — and a running showcase skeleton demonstrates theme switching
**Depends on**: Nothing (first phase)
**Requirements**: FOUND-01, FOUND-02, FOUND-03, FOUND-04, FOUND-05, FOUND-06, FOUND-07, FOUND-08, FOUND-09, FOUND-10, SHW-01, SHW-02, SHW-03
**Success Criteria** (what must be TRUE):
  1. A consumer project can add one Gradle dependency, wrap content in `AeroTheme {}`, and all child composables receive AeroBlue, AeroDark, or Classic color tokens automatically
  2. A developer can call `Modifier.glassEffect()`, `Modifier.glassPanel()`, or `Modifier.glassSurface()` and the composable renders the correct gradient + border + shadow in a single `drawBehind` pass (no overdraw)
  3. Attempting to use any non-public type from the `:library` JAR produces a compile error — `explicitApi()` is enforced
  4. The `:showcase` app launches, shows an empty component grid grouped by category, and a theme switcher toggles between AeroBlue / AeroDark / Classic instantly
  5. A developer can supply a custom `AeroColorScheme` to `AeroTheme` and all glass surfaces and text reflect the custom tokens
**Plans**: 4 plans
- [ ] 01-01-PLAN.md — Gradle skeleton: root build, version catalog, :library + :showcase modules with explicitApi
- [ ] 01-02-PLAN.md — AeroColorScheme (23 tokens, 3 presets) + AeroTypography data classes (TDD)
- [ ] 01-03-PLAN.md — AeroTheme composable + LocalAero* CompositionLocals + glass modifiers (single-pass drawBehind)
- [ ] 01-04-PLAN.md — :showcase skeleton: Main, ShowcaseApp, ThemeSwitcher, FoundationSection, PlaceholderSection (visual checkpoint)

### Phase 2: Atomic Components
**Goal**: All self-contained interactive components are implemented and interactive in the showcase — hover, focus, disabled, and pressed states work correctly using theme tokens
**Depends on**: Phase 1
**Requirements**: BTN-01, BTN-02, BTN-03, BTN-04, INP-01, INP-02, INP-03, INP-04, INP-05, INP-06, SEL-01, SEL-02, SEL-03, SEL-04, SEL-05, DRP-01, DRP-02, RNG-01, RNG-02, LST-01, LST-02
**Success Criteria** (what must be TRUE):
  1. Every button variant (AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar) renders enabled/disabled/hover/pressed states with correct Aero glass styling — verifiable in the showcase
  2. Every text input (AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker) accepts keyboard input, shows focus animation, and exposes its value via state — verifiable in the showcase
  3. Every selection control (AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl) toggles state correctly and renders the correct Aero visual for each state
  4. AeroSlider moves a single thumb and emits the correct float value; AeroProgressBar displays both determinate (with %) and indeterminate (animated shimmer) modes
  5. AeroDropdown and AeroComboBox open a themed dropdown, allow item selection, and close — AeroComboBox also accepts free-text input
**Plans**: 6 plans
- [ ] 02-01-PLAN.md — Buttons (BTN-01..04): AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar + ButtonsSection
- [ ] 02-02-PLAN.md — Text inputs (INP-01..06): AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker + InputSection
- [ ] 02-03-PLAN.md — Selection controls (SEL-01..05): AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl + SelectionSection
- [x] 02-04-PLAN.md — Range + Lists (RNG-01..02, LST-01..02): AeroSlider, AeroProgressBar, AeroListItem, AeroBadge + RangeSection + ListSection (completed 2026-04-28)
- [ ] 02-05-PLAN.md — Dropdowns (DRP-01..02): AeroDropdown + AeroComboBox + AeroPopupPositionProvider + DropdownSection
- [ ] 02-06-PLAN.md — Wire all sections into ShowcaseApp + ThemeSwitcher swap to AeroSegmentedControl + visual checkpoint

### Phase 3: Composite + Navigation
**Goal**: All container, overlay, and navigation components are implemented — a complete Compose Desktop window can be built using only aero-compose-ui with no custom chrome or raw Material3 surfaces visible
**Depends on**: Phase 2
**Requirements**: CNT-01, CNT-02, CNT-03, CNT-04, CNT-05, CNT-06, OVL-01, OVL-02, OVL-03, OVL-04, OVL-05, OVL-06, OVL-07, OVL-08, NAV-01, NAV-02, NAV-03, NAV-04, NAV-05
**Success Criteria** (what must be TRUE):
  1. AeroCard, AeroPanel, AeroDivider, AeroGroupBox, AeroScrollArea, and AeroScrollBar render correctly with glass styling; AeroScrollArea scrolls arbitrary content and AeroScrollBar is compatible with Compose `ScrollState`
  2. AeroDialog and AeroAlertDialog appear as modal overlays; AeroToast/AeroSnackbar auto-dismiss after a configurable timeout; AeroNotificationBanner shows info/warning/error/success variants with a close button; AeroDrawer animates in from the side; AeroPopover positions next to its anchor; AeroContextMenu opens on right-click; AeroTooltip appears on hover with a delay
  3. AeroTitleBar renders the Aero gradient, draggable region, and Minimize/Maximize/Close buttons with hover effects — built inside `FrameWindowScope` using `undecorated=true` (without `transparent=true` to avoid the Win11 crash)
  4. AeroMenuBar shows top-level items with dropdown submenus; AeroTabBar switches between tabs; AeroStatusBar displays text sections and colored indicators at the bottom of the window
  5. The showcase is complete: every component from Phases 1-3 appears in the appropriate category section, is interactive (not a placeholder), and all three themes render correctly across every component
**Plans**: TBD

## Progress

**Execution Order:** 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 4/4 | Complete   | 2026-04-27 |
| 2. Atomic Components | 6/6 | In Progress|  |
| 3. Composite + Navigation | 0/TBD | Not started | - |
