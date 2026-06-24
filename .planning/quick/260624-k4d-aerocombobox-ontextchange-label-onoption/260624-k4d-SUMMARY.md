---
phase: quick-260624-k4d
plan: 01
subsystem: components/dropdown
tags: [aerocombobox, bugfix, tdd, backward-compatible]
requires: []
provides:
  - "ComboBoxLogic.shouldAutoOpen (pure popup-reopen gate)"
  - "ComboBoxLogic.textAfterSelect (clearOnSelect-gated post-select text)"
  - "AeroComboBox clearOnSelect parameter"
affects:
  - "library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt"
tech-stack:
  added: []
  patterns:
    - "Pure decision logic in components/<area>/internal/<feature>/<Name>.kt + matching JVM unit test (AccordionToggle convention)"
    - "justSelected one-shot latch to suppress a self-triggered LaunchedEffect pass"
    - "Additive Boolean = false flag for backward-compatible behavior opt-in"
key-files:
  created:
    - "library/src/main/kotlin/com/mordred/aero/components/dropdown/internal/ComboBoxLogic.kt"
    - "library/src/test/kotlin/com/mordred/aero/components/dropdown/ComboBoxLogicTest.kt"
  modified:
    - "library/src/main/kotlin/com/mordred/aero/components/dropdown/AeroComboBox.kt"
decisions:
  - "clearOnSelect: Boolean = false additive flag (NOT unconditional removal of trailing onTextChange) keeps every existing consumer behaving identically"
  - "justSelected latch fixes the exact-full-match popup reopen independently of clearOnSelect — suppresses exactly one auto-open pass after a click"
metrics:
  duration: "~2m"
  completed: "2026-06-24"
  tasks: 2
  files: 3
---

# Quick Task 260624-k4d: AeroComboBox post-select text + popup-reopen Summary

Fixed two coupled `AeroComboBox` library bugs: a trailing `onTextChange(opt)` stomping consumer-cleared text after `onOptionSelect`, and a `LaunchedEffect` re-opening the popup on the exact full-match left in the field. Solved by extracting pure `textAfterSelect`/`shouldAutoOpen` helpers (TDD), adding a backward-compatible `clearOnSelect` flag, and a `justSelected` one-shot latch — default behavior preserved.

## What Was Built

### Task 1 — Pure ComboBox decision logic + unit tests (TDD)
- `ComboBoxLogic.kt`: two `internal` top-level functions, no Compose imports.
  - `textAfterSelect(opt, clearOnSelect)` → `opt` when false, `""` when true.
  - `shouldAutoOpen(focused, text, options, justSelected)` → `false` if `justSelected`, else `focused && options.any { it.contains(text, ignoreCase = true) }` (byte-for-byte match with the original LaunchedEffect predicate).
- `ComboBoxLogicTest.kt`: 8 cases (kotlin.test), AccordionToggleTest style — RED committed before GREEN.

### Task 2 — Wire AeroComboBox to the helpers
- Added `clearOnSelect: Boolean = false` after `placeholder`, before `width` (KDoc included).
- `var justSelected by remember { mutableStateOf(false) }` latch.
- `LaunchedEffect(focused, text)` now calls `shouldAutoOpen(...)` then resets `justSelected = false`.
- `onClick` order: `justSelected = true` → `expanded = false` → `onOptionSelect(opt)` → `onTextChange(textAfterSelect(opt, clearOnSelect))`. The literal `onTextChange(opt)` is gone.

## Verification

- `./gradlew :library:test --tests "...ComboBoxLogicTest"` — RED (unresolved reference) then GREEN, all 8 pass.
- `./gradlew :library:test :library:compileKotlin :showcase:compileKotlin` — BUILD SUCCESSFUL; existing showcase call site (no `clearOnSelect`) compiles unchanged, proving backward compatibility.
- Grep: `onTextChange(opt)` absent from AeroComboBox.kt; `shouldAutoOpen`, `textAfterSelect`, `clearOnSelect` all present.

## Deviations from Plan

None - plan executed exactly as written.

## Commits

- `fbba99c` test(quick-260624-k4d-01): add failing tests (RED)
- `dc255d3` feat(quick-260624-k4d-01): add pure ComboBox post-select logic (GREEN)
- `d371e72` fix(quick-260624-k4d-01): wire AeroComboBox to post-select helpers

## Self-Check: PASSED
