---
phase: 02-atomic-components
plan: "05"
subsystem: dropdown-components
tags: [dropdown, combobox, popup, keyboard-nav, phase2, atomic]
dependency_graph:
  requires:
    - AeroTheme.colors (cardBackground, glassBorder, buttonHover, primary, borderDefault, borderSelected, onSurface, labelText)
    - AeroTheme.typography (bodyLarge, label, bodyMedium, title)
    - compose.animation (animateColorAsState)
    - androidx.compose.ui.window.Popup + PopupPositionProvider + PopupProperties
  provides:
    - AeroPopupPositionProvider (internal — reusable in Phase 3 for AeroPopover, AeroContextMenu, AeroMenuBar)
    - AeroDropdownItem (internal — shared item renderer for both dropdowns)
    - AeroDropdownPopup (internal — generic popup container)
    - AeroDropdown<T> (public — non-editable select)
    - AeroComboBox (public — free-text + filtered suggestions)
    - DropdownSection (showcase — DRP-01 + DRP-02 demo)
  affects:
    - Phase 3: AeroPopover, AeroContextMenu, AeroMenuBar can reuse AeroPopupPositionProvider
    - showcase/ShowcaseApp.kt: DropdownSection replaces PlaceholderSection for "Dropdown"
tech_stack:
  added:
    - androidx.compose.ui.window.Popup with PopupPositionProvider
    - androidx.compose.ui.window.PopupProperties (focusable, dismissOnBackPress, dismissOnClickOutside)
    - androidx.compose.foundation.text.BasicTextField for AeroComboBox input
  patterns:
    - PopupPositionProvider with window-bound clamping and flip-above logic
    - Internal primitive sharing (AeroDropdownItem, AeroDropdownPopup used by both public components)
    - LaunchedEffect(expanded) for state reset on popup open
    - non-focusable Popup for combobox suggestions (preserves text field focus)
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdownMenu.kt
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroDropdown.kt
    - library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/DropdownSection.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt (Rule 3 fix)
decisions:
  - "AeroComboBox popup uses PopupProperties(focusable=false) to preserve text field keyboard focus; keyboard nav inside popup not supported in v1 (mouse-click selection works)"
  - "AeroPopupPositionProvider is internal (same module) — both AeroDropdown and AeroComboBox can reference it directly; Phase 3 overlay components will reuse it"
  - "Filter in AeroComboBox uses case-insensitive String.contains to match partial input against any substring in option"
metrics:
  duration_minutes: 9
  completed_date: "2026-04-28"
  tasks_completed: 3
  tasks_total: 3
  files_created: 4
  files_modified: 1
---

# Phase 2 Plan 05: Dropdown Components Summary

**One-liner:** Custom Popup-based dropdown and combobox with PopupPositionProvider window-bound clamping, keyboard navigation (Esc/Up/Down/Enter), and non-focusable suggestion popup for free-text combobox.

## What Was Built

### Task 1 — AeroDropdownMenu (internal primitives)

`library/.../dropdown/AeroDropdownMenu.kt` provides three internal declarations:

- **AeroPopupPositionProvider**: Positions popup below anchor, left-aligned. Clamps X so popup stays within window right edge. Flips above anchor if not enough room below (`proposedY + popupContentSize.height > windowSize.height`). Phase 3 overlays (AeroPopover, AeroContextMenu, AeroMenuBar) can reuse this directly.
- **AeroDropdownItem**: Composable item with animated background — selected=primary@30%, highlighted=buttonHover, default=Transparent. 150ms LinearEasing tween. `clickable` closes popup and calls onSelect.
- **AeroDropdownPopup**: Generic popup wrapper; applies cardBackground + glassBorder styling, 4dp corner radius, vertical padding. Accepts `onKeyEvent` lambda so callers handle keyboard nav.

### Task 2 — AeroDropdown (DRP-02)

`library/.../dropdown/AeroDropdown.kt` — public generic `<T>` composable.

- Trigger Box: 28dp height, 4dp rounded corners, cardBackground fill, 1dp borderDefault / 2dp borderSelected on expanded, ▼ chevron.
- `LaunchedEffect(expanded)` resets `highlightedIndex` to `options.indexOf(selected).coerceAtLeast(0)` on open.
- Keyboard: Esc closes, Up/Down moves index (coerced to valid range), Enter commits selection.
- `enabled=false` → 0.4 alpha, clickable disabled.

### Task 3 — AeroComboBox (DRP-01) + DropdownSection

`library/.../dropdown/AeroComboBox.kt` — free-text + filtered dropdown.

- Uses `BasicTextField` with `MutableInteractionSource` + `collectIsFocusedAsState`.
- Case-insensitive filter: `options.filter { it.contains(text, ignoreCase = true) }`.
- `LaunchedEffect(focused, text)` auto-expands popup when focused + filtered list non-empty.
- Popup uses `PopupProperties(focusable=false)` — text field retains keyboard focus. Mouse-click selection works.
- Escape key on the text field closes popup via `onPreviewKeyEvent`.

`showcase/.../sections/DropdownSection.kt` — demo with a 6-country list showing both components live.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] Fixed LocalContentColor import in AeroIconButton.kt**
- **Found during:** Task 3 — :library:compileKotlin failed with `Unresolved reference 'LocalContentColor'`
- **Issue:** AeroIconButton.kt (created in Plan 02-01) imported `LocalContentColor` from `androidx.compose.ui.platform` which is not available in the compose.desktop.common dependency set; correct import is `androidx.compose.material3.LocalContentColor`
- **Fix:** Changed import to `androidx.compose.material3.LocalContentColor`
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt`
- **Commit:** 85ccee0

### Deferred Issues

**Pre-existing: :library:test — compileTestKotlin fails**
- `AeroColorSchemeTest.kt` and `AeroTypographyTest.kt` fail to compile with "Argument type mismatch: actual type is 'Color', but 'Double' was expected" for `kotlin.test.assertEquals` calls
- Root cause: Kotlin/JVM overload resolution ambiguity between `assertEquals(expected, actual, message)` and numeric overloads — pre-exists from Phase 1, not caused by Plan 02-05
- Impact: `./gradlew :library:test` does not exit 0; compile gates (compileKotlin + showcase) are green
- Action: Deferred — needs a dedicated test-fix pass

## Requirements Satisfied

- **DRP-01**: AeroComboBox supports free-text typing AND filtering popup suggestions
- **DRP-02**: AeroDropdown shows popup with keyboard navigation (Esc/Up/Down/Enter)
- **AeroPopupPositionProvider**: Clamps popup inside window bounds, flips above anchor when below would overflow

## Reusability Note

`AeroPopupPositionProvider` is `internal` to the library module. Phase 3 plans implementing `AeroPopover`, `AeroContextMenu`, and `AeroMenuBar` MUST import it from the same package (`com.mordred.aero.components.dropdown`) — no duplication needed.

## Self-Check: PASSED

Files verified:
- FOUND: library/.../dropdown/AeroDropdownMenu.kt
- FOUND: library/.../dropdown/AeroDropdown.kt
- FOUND: library/.../dropdown/AeroComboBox.kt
- FOUND: showcase/.../sections/DropdownSection.kt
- FOUND: .planning/phases/02-atomic-components/02-05-SUMMARY.md

Commits verified:
- FOUND: 948f8b4 feat(02-05): AeroDropdownMenu — popup container + position provider + item
- FOUND: 53b421f feat(02-05): AeroDropdown — non-editable select with keyboard navigation
- FOUND: 85ccee0 feat(02-05): AeroComboBox + DropdownSection showcase wiring (DRP-01, DRP-02)

Compile gates:
- :library:compileKotlin — PASSED
- :showcase:compileKotlin — PASSED
