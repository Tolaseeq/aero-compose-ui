---
phase: 08-pickers
plan: 06
subsystem: ui
tags: [colorpicker, hsv, compose-desktop, popup, kotlin]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives
    provides: AeroHsvColorSquare, AeroHueSlider, AeroColorMath (rgbToHsv/hexToRgb/hexToRgba/rgbToHex), AeroCalendarPositionProvider
provides:
  - AeroColorPicker public inline panel (HSV-as-single-source-of-truth)
  - AeroColorPickerButton public swatch-trigger Popup wrapper
  - safeHsvColor / hsvToHex / hexToHsv pure derivation helpers (internal)
  - DefaultAeroSwatches 16-color Aero palette (internal)
affects: [11-showcase-sign-off]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "HSV(A) floats are the only stored state; Color/RGB/HEX are derived views per emit (PITFALL-15)"
    - "safeHsvColor wrapper coerces hue/sat/value/alpha before every Color.hsv call (NEW-PICK-01)"
    - "Text-field Enter-to-commit via Modifier.onPreviewKeyEvent (AeroTextField has no keyboard params)"
    - "Popup hosted inside the trigger Box to anchor to the swatch (NEW-PICK-05)"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPickerButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorSwatches.kt
    - library/src/test/kotlin/com/mordred/aero/components/pickers/AeroColorPickerTest.kt
  modified: []

key-decisions:
  - "HSV(A) floats are the single source of truth; RGB and HEX recomputed each emit, never stored as state"
  - "HEX field commits on Enter and on focus loss; invalid input is silently ignored (state preserved)"
  - "Enter-to-commit implemented with onPreviewKeyEvent rather than extending the shared public AeroTextField API"

patterns-established:
  - "No-drift HSV state machine: drive sat 1.0 -> 0.5 -> 1.0 restores #FF0000 exactly"
  - "safeHsvColor as the only Color.hsv entry point inside the picker"

requirements-completed: [PICK-05, PICK-06, PICK-07]

# Metrics
duration: 5min
completed: 2026-06-18
---

# Phase 8 Plan 06: AeroColorPicker Summary

**HSV-truth color picker — inline `AeroColorPicker` panel (HSV square + hue strip + R/G/B sliders + HEX field + swatches + optional checkerboard alpha) plus a swatch-trigger `AeroColorPickerButton` Popup, all drift-free from a single HSV(A) source.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-06-18T09:57:15Z
- **Completed:** 2026-06-18T10:02:42Z
- **Tasks:** 3
- **Files modified:** 4 (all created)

## Accomplishments
- Pure HSV<->Color<->HEX derivation layer (`safeHsvColor`, `hsvToHex`, `hexToHsv`) with an 11-test suite incl. the `#FF0000` round-trip drift gate (PITFALL-15)
- `AeroColorPicker` inline panel wiring all five controls synchronized from a single HSV(A) state, with a before/after preview bar, swatch row, and optional alpha slider over an 8dp checkerboard
- `AeroColorPickerButton` swatch trigger whose anchored `Popup` (inside the trigger Box) hosts the same panel — no transparent Dialog (W11-01)
- `DefaultAeroSwatches`: 16 Aero-fit preset colors

## Task Commits

Each task was committed atomically:

1. **Task 1: Swatches + HSV round-trip helpers + tests** - `6c41d78` (feat, TDD: helpers + 11 tests landed together)
2. **Task 2: AeroColorPicker inline panel** - `d1789d0` (feat)
3. **Task 3: AeroColorPickerButton popup wrapper** - `735ea43` (feat)

## Files Created/Modified
- `library/.../pickers/AeroColorPicker.kt` - Pure derivation helpers + public inline panel; HSV(A) is the single source of truth
- `library/.../pickers/AeroColorPickerButton.kt` - Public swatch trigger + anchored Popup hosting the panel
- `library/.../pickers/internal/color/AeroColorSwatches.kt` - `DefaultAeroSwatches` (16 colors)
- `library/src/test/.../pickers/AeroColorPickerTest.kt` - 11 tests: drift gate, parsing, alpha length, swatch count

## Decisions Made
- HSV(A) floats are the only stored state; RGB/HEX derived per emit (PITFALL-15) — verified by the drift-gate test.
- The HEX field commits on Enter and on focus loss; invalid HEX is ignored and the prior state is kept.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] HEX Enter-to-commit without modifying shared AeroTextField**
- **Found during:** Task 2 (AeroColorPicker inline panel)
- **Issue:** The plan specified `KeyboardActions(onDone = ...)` on the HEX `AeroTextField`, but the existing public `AeroTextField` is a fixed `BasicTextField` wrapper that exposes no `keyboardOptions`/`keyboardActions` params. Wiring them would require widening a shared public component's API (borderline architectural).
- **Fix:** Implemented Enter-to-commit with `Modifier.onPreviewKeyEvent` (Enter/NumPadEnter) on the field, keeping the focus-loss commit as specified. No change to `AeroTextField`.
- **Files modified:** library/.../pickers/AeroColorPicker.kt
- **Verification:** `:library:compileKotlin` green; HEX commit logic unchanged from the plan's intent.
- **Committed in:** d1789d0 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Functionally equivalent to the planned HEX commit behavior; avoided scope creep into the shared input component.

## Issues Encountered
None — all three tasks compiled and tested green on first verification.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All Phase 8 picker components (waves 1-4) now include the color picker; Phase 11 showcase can wire a `PickersSection` color demo using `AeroColorPicker` / `AeroColorPickerButton`.
- No blockers. The HSV-truth pattern and `safeHsvColor` guard are locked for any future color UI.

---
*Phase: 08-pickers*
*Completed: 2026-06-18*

## Self-Check: PASSED

- All 4 created files present on disk.
- All 3 task commits present in git history (6c41d78, d1789d0, 735ea43).
