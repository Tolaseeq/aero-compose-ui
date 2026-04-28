---
phase: 02-atomic-components
plan: "02"
subsystem: input-components
tags: [input, text-field, password, number-spinner, search, file-picker, composable, animation]
dependency_graph:
  requires: [01-01, 01-02, 01-03]
  provides: [INP-01, INP-02, INP-03, INP-04, INP-05, INP-06]
  affects: [02-06-showcase-wiring]
tech_stack:
  added: [compose.animation]
  patterns: [BasicTextField+decorationBox, focus-border-animation-150ms-LinearEasing, glassSurface-button]
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroTextField.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroTextArea.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt
    - library/src/main/kotlin/com/mordred/aero/components/input/AeroFilePicker.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/InputSection.kt
  modified:
    - library/build.gradle.kts
decisions:
  - "compose.animation added to library/build.gradle.kts — animateColorAsState lives in androidx.compose.animation (not .core); missing dep broke all plans using color animation"
  - "AeroPasswordField implemented as inline BasicTextField (not wrapping AeroTextField) to support visualTransformation parameter that AeroTextField lacks"
  - "AeroFilePicker uses inline glassSurface Box for Обзор button — avoids Wave 1 compile-order dependency on AeroOutlinedButton (Plan 02-01)"
  - "AeroSearchField uses wrapper Row approach: leading icon Box + AeroTextField — keeps AeroTextField API unchanged"
  - "AeroTextArea verticalScroll-only (no scrollbar) — AeroScrollArea ships in Phase 3 (CNT-05); KDoc TODO added per plan spec"
metrics:
  duration: 7 min
  completed_date: "2026-04-28"
  tasks_completed: 3
  files_created: 7
  files_modified: 1
---

# Phase 02 Plan 02: Input Components Summary

**One-liner:** Six BasicTextField-based input variants (single-line, textarea, password, number spinner, search, file picker) with 150ms LinearEasing focus-border animation reading all colors from AeroTheme.

## What Was Built

All six Phase 2 text-input requirements implemented in `com.mordred.aero.components.input` package, plus `InputSection` showcase wiring.

### Components Delivered

| Component | Requirement | Key Feature |
|-----------|-------------|-------------|
| AeroTextField | INP-01 | Single-line, animates borderDefault→borderSelected 150ms, trailingIcon slot |
| AeroTextArea | INP-02 | Multi-line, heightIn(min,max), verticalScroll, TODO(Phase 3) KDoc |
| AeroPasswordField | INP-03 | Inline BasicTextField with PasswordVisualTransformation + 🙈/👁 toggle |
| AeroNumberSpinner | INP-04 | BasicTextField + ▲▼ glassSurface buttons, coerceIn(min,max) clamping |
| AeroSearchField | INP-05 | Row with 🔍 icon Box + AeroTextField, × clear button when value.isNotEmpty() |
| AeroFilePicker | INP-06 | Read-only AeroTextField + glassSurface Обзор button → java.awt.FileDialog |
| InputSection | showcase | All 6 components with remembered state, InputRow label-column layout (140dp) |

### Focus-Border Animation Pattern

All input components (AeroTextField, AeroTextArea, AeroPasswordField) share the same inline pattern:

```kotlin
val borderColor by animateColorAsState(
    targetValue = if (!enabled) colors.borderDefault.copy(alpha = 0.4f)
                  else if (focused) colors.borderSelected
                  else colors.borderDefault,
    animationSpec = tween(150, easing = LinearEasing),
    label = "border"
)
val borderWidth by animateFloatAsState(
    targetValue = if (focused && enabled) 2f else 1f,
    animationSpec = tween(150, easing = LinearEasing),
    label = "borderWidth"
)
```

Duplication is intentional (self-contained files; refactor deferred to Phase 3 per plan spec).

## Verification Results

- `./gradlew :library:compileKotlin` — PASSED
- `./gradlew :library:test` — PASSED (no regression)
- `./gradlew :showcase:compileKotlin` — PASSED
- All 6 input files exist in library/src/main/kotlin/com/mordred/aero/components/input/
- InputSection.kt exists in showcase/src/main/kotlin/com/mordred/showcase/sections/
- `grep -rn "Color(0x" library/src/main/kotlin/com/mordred/aero/components/input/` — 0 matches

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing compose.animation dependency**
- **Found during:** Task 1 compilation (animateColorAsState unresolved)
- **Issue:** `library/build.gradle.kts` lacked `implementation(compose.animation)`. The function `animateColorAsState` lives in `androidx.compose.animation` (not `.core`), and the separate animation artifact was missing. This also affected Plan 02-01 files (AeroCheckbox, AeroRadioButton, AeroDropdownMenu) that were already written with the correct import.
- **Fix:** Added `implementation(compose.animation)` to library dependencies (linter auto-applied before manual edit; confirmed in build.gradle.kts)
- **Files modified:** library/build.gradle.kts
- **Commit:** included in 1aa1427

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1 | 1aa1427 | feat(02-02): implement AeroTextField, AeroTextArea, AeroPasswordField |
| Task 2 | 42c7a01 | feat(02-02): implement AeroNumberSpinner, AeroSearchField, AeroFilePicker |
| Task 3 | 234c172 | feat(02-02): add InputSection showcase wiring all six inputs |

## Self-Check: PASSED

Files verified:
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroTextField.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroTextArea.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroPasswordField.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroNumberSpinner.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroSearchField.kt
- FOUND: library/src/main/kotlin/com/mordred/aero/components/input/AeroFilePicker.kt
- FOUND: showcase/src/main/kotlin/com/mordred/showcase/sections/InputSection.kt

Commits verified:
- FOUND: 1aa1427 (AeroTextField + AeroTextArea + AeroPasswordField)
- FOUND: 42c7a01 (AeroNumberSpinner + AeroSearchField + AeroFilePicker)
- FOUND: 234c172 (InputSection)
