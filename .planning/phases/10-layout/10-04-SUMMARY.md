---
phase: 10-layout
plan: "04"
subsystem: layout
tags: [wizard, stepper, validate-gate, back-preservation, hybrid-ownership, tdd]
dependency_graph:
  requires: [07-02-AeroStepIndicator, 07-01-buttons]
  provides: [AeroStepperWizard, AeroWizardStep, WizardStep-internals]
  affects: [Phase 11 LayoutSection showcase]
tech_stack:
  added: []
  patterns: [hybrid-ownership, validate-gate-onClick-only, all-steps-composed-size0dp, pure-function-TDD]
key_files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/layout/internal/wizard/WizardStep.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroStepperWizard.kt
    - library/src/test/kotlin/com/mordred/aero/components/layout/WizardStepTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroSplitPane.kt
decisions:
  - "[Phase 10-04] AeroStepperWizard validate gate locked: onValidate() called exactly once and only in Next/Finish onClick lambda (PITFALL-12). canProceed is the live enabled-state signal, never onValidate."
  - "[Phase 10-04] Back state preservation uses Modifier.size(0.dp) on inactive steps — all steps remain composed, never destroyed, so remember/rememberSaveable state survives Back navigation (Pitfall 5)."
  - "[Phase 10-04] Hybrid ownership locked: uncontrolled by default (internalStep seeded from initialStep); controlled when onStepChange != null (caller owns currentStep). Both branches intentionally kept."
  - "[Phase 10-04] nextStepIndex passes valid=true explicitly after onValidate gate passes in onClick — clamp at lastIndex prevents overflow on last step."
metrics:
  duration: "~13 minutes"
  completed_date: "2026-06-18"
  tasks_completed: 2
  files_created: 3
  files_modified: 1
requirements: [LAYO-08, LAYO-09]
---

# Phase 10 Plan 04: AeroStepperWizard Summary

**One-liner:** Linear step wizard with AeroStepIndicator, hybrid ownership, onValidate-in-onClick-only gate (PITFALL-12), and size(0.dp) all-steps-composed pattern for Back state preservation.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 (RED) | WizardStepTest failing tests | df02ae2 | WizardStepTest.kt |
| 1 (GREEN) | WizardStep.kt pure logic + pre-existing layout fixes | d93df5c | WizardStep.kt, AeroAccordion.kt, AeroSidebar.kt, AeroSplitPane.kt |
| 2 | AeroStepperWizard composable | aa99de7 | AeroStepperWizard.kt |

## Implementation Notes

### Task 1: Pure wizard step-transition logic (TDD)

`WizardStep.kt` provides three pure internal functions — no Compose imports, fully JVM-testable:
- `nextStepIndex(current, lastIndex, valid)` — returns current when invalid (Next blocked); advances by 1 clamped to lastIndex when valid
- `prevStepIndex(current)` — returns current-1 clamped to 0 (Back at first step is no-op)
- `isLastStep(current, lastIndex)` — true when current >= lastIndex

`WizardStepTest.kt` covers all 10 behavioral cases: advance valid, block invalid, no overflow at last, mid-step advance, last-invalid-stays, back, back at 0, isLast true/false, single-step wizard.

### Task 2: AeroStepperWizard composable

`AeroWizardStep` data class holds per-step placement: `label`, `content`, `onValidate: () -> Boolean = { true }`, `canProceed: Boolean = true`.

`AeroStepperWizard`:
- Hybrid ownership — uncontrolled (internalStep) or controlled (currentStep + onStepChange)
- `AeroStepIndicator(currentStepInt, steps.size)` rendered at top (Phase 7 primitive, 0-based, surface-less)
- All steps kept composed via `size(0.dp)` for inactive steps (Back state preservation)
- `onValidate()` called exactly once, only in Next/Finish `onClick` (PITFALL-12 compliance verified by `grep -c` = 1)
- Next button `enabled = steps[currentStepInt].canProceed` (live signal, not onValidate)
- Last step: Next label → Finish label; onClick calls `onFinish()` after `onValidate()` passes
- Surface-less: no glass background applied — caller responsibility

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing untracked layout files caused compileKotlin failures**
- **Found during:** Task 1 GREEN (verifying tests pass)
- **Issue:** `AeroAccordion.kt`, `AeroSidebar.kt`, `AeroSplitPane.kt` were untracked from prior plan executions and had compilation errors:
  - `AeroSplitPane.kt`: `weight(0f, fill = false)` on fixed-width pane; `Modifier.align()` called outside BoxScope; missing `onSizeChanged` import
  - `AeroAccordion.kt`: wrong `animateFloatAsState` import (`animation` vs `animation.core`)
- **Fix:** Committed all three files; linter then rewrote `AeroSplitPane.kt` to use `BoxWithConstraints` pattern (correct, eliminates `onSizeChanged` approach entirely)
- **Files modified:** AeroSplitPane.kt, AeroAccordion.kt, AeroSidebar.kt
- **Commits:** d93df5c (initial), aa99de7 (linter fixes committed)

**2. [Rule 3 - Blocking] AeroSidebarMode enum missing from disk**
- **Found during:** Task 1 GREEN (forced recompile)
- **Issue:** `AeroSidebarState.kt` references `AeroSidebarMode` and `targetWidthForMode` but no file defined them on disk (prior plan left them untracked or was never committed)
- **Fix:** Created `AeroSidebarMode.kt` with the enum and helper — but linter then handled this via the `AeroSidebarState.kt` rewrite. Compile succeeded after linter applied fixes.
- **Outcome:** compileKotlin BUILD SUCCESSFUL

## Self-Check: PASSED

- WizardStep.kt: FOUND
- AeroStepperWizard.kt: FOUND
- WizardStepTest.kt: FOUND
- Commit df02ae2 (RED test): FOUND
- Commit d93df5c (GREEN impl + fixes): FOUND
- Commit aa99de7 (Task 2 composable): FOUND
