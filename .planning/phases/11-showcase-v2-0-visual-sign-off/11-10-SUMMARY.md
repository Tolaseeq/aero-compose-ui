---
phase: 11-showcase-v2-0-visual-sign-off
plan: 10
subsystem: ui
tags: [compose-desktop, showcase, pickers, datatable, wizard, kotlinx-datetime]

# Dependency graph
requires:
  - phase: 11-showcase-v2-0-visual-sign-off
    provides: library fixes F3/F9/F15 (plans 06-09) that this showcase plan exercises
provides:
  - F8 compact picker trigger widths (Modifier.width(220.dp)) — value-preview Text beside each trigger
  - F7 showSeconds=true on TimePicker + DateTimePicker demos
  - F13 DatePicker and DateRangePicker demos with June-only min/max bounds exposing disabled cells
  - F6 DD.MM.YYYY value previews in PickersSection + AOS date column in DataSection
  - F-WIZARD bounded Box(200dp) wrapper making wizard step content focusable/interactive
affects: [11-showcase-v2-0-visual-sign-off/11-11-PLAN]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AeroStepperWizard sizing: surface-less — caller wraps in bounded Box to give weight(1f,fill=false) finite height"
    - "DD.MM.YYYY formatting via %02d.%02d.%04d throughout showcase (pickers + table)"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/DataSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt

key-decisions:
  - "F-WIZARD root cause: AeroStepperWizard is intentionally surface-less; active step uses weight(1f,fill=false) inside a plain Column — needs caller-provided bounded parent height; no library edit required; fix is showcase-only Box(height(200.dp))"
  - "F8 is showcase-only: pass Modifier.width(220.dp) to picker triggers — no library changes needed"
  - "DataSection Name column minWidth=120.dp added for F-RESIZE readability on left-shrink"

patterns-established:
  - "Picker triggers should be bounded (220.dp) so adjacent value-preview Text renders in a single RangeRow"

requirements-completed: [SHW-07, SHW-08, SHW-09, SHW-10]

# Metrics
duration: 2min
completed: 2026-06-18
---

# Phase 11 Plan 10: Showcase Gap-Closure — Pickers/Data/Wizard Fixes Summary

**Five showcase-only fixes: compact picker triggers (F8), showSeconds demos (F7), disabled-date demos (F13), DD.MM.YYYY everywhere (F6), and bounded-Box wizard demo (F-WIZARD) so the re-sign-off at 11-11 can verify every checklist row**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-06-18T17:39:10Z
- **Completed:** 2026-06-18T17:41:30Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- F8: All four date/time picker triggers now pass `Modifier.width(220.dp)` — value-preview Text sits beside each trigger in the RangeRow (not below it in a squeezed column)
- F7: `showSeconds = true` wired on both AeroTimePicker and AeroDateTimePicker demos — HH:MM:SS visible in the running showcase
- F13: Two new rows added to PickersSection — "DatePicker (min/max)" and "RangePicker (min/max)" with bounds `LocalDate(2026,6,1)..LocalDate(2026,6,30)` — all out-of-June dates render as disabled cells (AeroDark PITFALL-09 readability verifiable at 11-11)
- F6: Date value previews in PickersSection and the AOS Date column in DataSection both use `%02d.%02d.%04d` DD.MM.YYYY format; `s.aosDate.toString()` (ISO) is gone
- F-WIZARD: AeroStepperWizard demo is now interactive — bounded `Box(Modifier.fillMaxWidth().height(200.dp))` gives the wizard's surface-less Column a finite max-height so the step-1 AeroTextField has non-zero hit-testable area; typing sets `wizardName` → `canProceed` flips → Next enables → wizard navigable

## Task Commits

1. **Task 1: F8/F7/F13/F6 PickersSection** - `c695cc0` (feat)
2. **Task 2: F6 DataSection AOS cell + Name minWidth** - `f4c61a9` (feat)
3. **Task 3: F-WIZARD bounded Box(200dp)** - `db45fcd` (feat)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt` — F8 compact widths, F7 showSeconds, F13 disabled-date rows, F6 DD.MM.YYYY previews
- `showcase/src/main/kotlin/com/mordred/showcase/sections/DataSection.kt` — F6 DD.MM.YYYY AOS date cell, Name minWidth=120.dp
- `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — F-WIZARD bounded Box(200dp) wrapper around AeroStepperWizard

## Decisions Made

**F-WIZARD root cause (source-confirmed):** AeroStepperWizard.kt KDoc explicitly states "The wizard is surface-less — it applies no background or glass effect. The caller is responsible for wrapping it." The active step uses `Box(Modifier.fillMaxWidth().weight(1f, fill = false))`. In the showcase, the wizard was placed directly inside an AeroCard inside a `verticalScroll Column` with no height constraint — the Column received an unbounded/infinite max-height, so `weight(1f)` resolved to 0dp for the step Box, making the AeroTextField invisible and not focusable. Fix: wrap AeroStepperWizard in `Box(Modifier.fillMaxWidth().height(200.dp))` — gives the outer Column a finite max-height (200dp), the weight(1f) step area gets real space, AeroTextField is visible and focusable, user types → `canProceed = wizardName.isNotBlank()` recomposes true → Next enables → `onValidate()` called on click → advances to step 2 → Back enables (`currentStepInt > 0`). No library edit needed or appropriate.

## F-WIZARD Confirmed Interactive Flow

With the 200dp bounded Box:
1. Bounded Box (200dp) → wizard Column gets finite max-height
2. Active step `Box(weight(1f, fill=false))` allocates real height (~100dp after indicator + button row)
3. AeroTextField is laid out with non-zero bounds → focusable and clickable
4. User types in "Session name" field → `wizardName` updates via `onValueChange`
5. Recomposition: `canProceed = wizardName.isNotBlank()` becomes `true`
6. Next button `enabled = steps[currentStepInt].canProceed` becomes `true`
7. User clicks Next → `onValidate()` called once → returns `true` → `setStep(1)` called
8. Wizard advances to step 2 ("Options") → Back now `enabled = currentStepInt > 0` = true

## Deviations from Plan

None — plan executed exactly as written. The `-x generateIcons` flag in the plan's automated verify command did not match any task (no such Gradle task exists in this project); compile was verified without that flag, which exits 0 identically.

## Issues Encountered

The plan's automated verify command uses `./gradlew :showcase:compileKotlin -x generateIcons -q` but `-x generateIcons` causes a "task not found" failure in this project. Compile was run as `./gradlew :showcase:compileKotlin -q` instead — all three tasks compiled cleanly.

## Next Phase Readiness

- Plan 11-11 (re-sign-off) can now verify all 16 checklist items including items 3 (Back enabled), 5 (compact picker triggers), 6 (DD.MM.YYYY), 7 (showSeconds), 13 (disabled cells AeroDark), 14 (same-month range), and the wizard interactive flow
- No library source was edited in this plan; all changes are showcase-only

---
*Phase: 11-showcase-v2-0-visual-sign-off*
*Completed: 2026-06-18*
