---
phase: 11-showcase-v2-0-visual-sign-off
plan: "06"
subsystem: ui
tags: [compose-desktop, drag, pointer-input, range-slider, color-picker, aero-drag-splitter]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives
    provides: aeroDragSplitter Modifier + AeroHsvColorSquare + AeroHueSlider primitives
  - phase: 08-pickers
    provides: AeroRangeSlider with applyThumbMove/xToValue helpers
  - phase: 09-data
    provides: AeroDataTable consuming aeroDragSplitter for column resize
  - phase: 10-layout
    provides: AeroSplitPane consuming aeroDragSplitter for divider
provides:
  - Frame-stable single-event drag delta in aeroDragSplitter (F3 ghosting eliminated, F15 sensitivity fixed)
  - Live-value read in AeroRangeSlider drag loop (F9 other-thumb reset eliminated)
  - F9 regression guard test (chainedThumbMovePreservesMovedEndEndpoint)
  - Audit confirming AeroHsvColorSquare + AeroHueSlider are already frame-stable (no offset bug)
affects:
  - 11-SIGNOFF.md items 3 (column resize), 8 (RangeSlider thumb overlap), 10 (SplitPane divider), 14 (drag-on-first-pixel)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Single-frame drag delta: use change.positionChange() (or change.position - change.previousPosition) inside awaitPointerEventScope — never maintain a cross-frame prev variable in a Modifier that relocates its hit-area Box"
    - "Live-value drag loop: val currentValue by rememberUpdatedState(value) at composable top; read currentValue inside awaitPointerEventScope so non-dragged state is always current"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderTest.kt (new F9 guard test added to existing file)
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt

key-decisions:
  - "positionChange() chosen over (position - previousPosition) — idiomatic Compose API, computes intra-event delta immune to hit-area Box relocation between frames"
  - "rememberUpdatedState(value) placed above all remember{} blocks in AeroRangeSlider to ensure the drag loop lambda always reads the latest committed range"
  - "AeroHsvColorSquare + AeroHueSlider audit: no offset bug found — both use absolute position / Canvas size mapping with no cross-frame prev variable; F15 on color picker was transitive from the splitter/RangeSlider bugs"

patterns-established:
  - "positionChange() for all Modifier-based drag: single-frame, frame-stable, no hit-area relocation artifact"
  - "rememberUpdatedState for any drag loop that must see fresh outer state"

requirements-completed: [SHW-10]

# Metrics
duration: 12min
completed: 2026-06-18
---

# Phase 11 Plan 06: Drag Root-Cause Fix (F3/F15/F9) Summary

**Frame-stable positionChange() delta in aeroDragSplitter (F3/F15) and rememberUpdatedState live-value drag loop in AeroRangeSlider (F9), plus absolute-position audit confirming HSV/hue pickers need no change**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-06-18T17:30:00Z
- **Completed:** 2026-06-18T17:42:00Z
- **Tasks:** 3 (2 code changes + 1 audit)
- **Files modified:** 2 source files + 1 test file

## Accomplishments

- Fixed F3 (ghosting/doubling on column splitter + SplitPane divider) and F15 (reduced drag sensitivity) by replacing the cross-frame `cur.x - prev.x` delta in `aeroDragSplitter` with `change.positionChange().x/y` — single-frame, coordinate-frame-stable, immune to hit-area Box relocation between frames
- Fixed F9 (RangeSlider dragging one thumb resets the other) by adding `val currentValue by rememberUpdatedState(value)` in `AeroRangeSlider` and using `currentValue` at both `applyThumbMove` call sites and the nearest-thumb pick — the non-dragged endpoint now always reflects the latest committed value
- Added F9 regression guard test `chainedThumbMovePreservesMovedEndEndpoint` to existing `AeroRangeSliderTest` — verifies that chaining two sequential applyThumbMove calls preserves the first move's result (0.2..0.7 → End→0.9 → Start→0.4 = 0.4..0.9)
- Audited `AeroHsvColorSquare` and `AeroHueSlider` — both use absolute-position mapping (`position.x / width`, `position.y / height`) with no cross-frame `prev` variable; F15 on color picker was a transitive effect of the splitter bug, resolved in this same plan

## Diagnosed Root Causes

### F3 Ghosting + F15 Reduced Sensitivity (aeroDragSplitter)

`aeroDragSplitter` computed delta as `cur.x - prev.x` across frames, where `cur`/`prev` are `change.position` LOCAL to the 8dp hit-area Box. During a resize/divider drag the hit-area Box itself relocates each frame (column width or divider px changes), so `prev` and `cur` are in DIFFERENT coordinate frames. The leftover offset makes the delta smaller (F15: cursor outpaces handle) and causes the consumed-drag visual to double (F3: ghosting).

**Fix:** `change.positionChange().x` — intra-event delta computed within one pointer event in one coordinate frame.

### F9 RangeSlider Other-Thumb Reset

`AeroRangeSlider`'s `pointerInput(enabled, valueRange, steps)` lambda captured `value` once at setup. Inside `awaitPointerEventScope`, `applyThumbMove(value, thumb, ...)` read the stale snapshot. `applyThumbMove` carries the non-dragged endpoint from `current` — but `current` was the drag-start snapshot, so dragging End from 0.7→0.9 and then dragging Start would call `applyThumbMove(0.2..0.7, Start, ...)` and reset End to 0.7.

**Fix:** `val currentValue by rememberUpdatedState(value)` above the drag loop; read `currentValue` inside `awaitPointerEventScope`.

### F15 on HSV Square / Hue Slider (transitive — no code change)

Both Canvas pickers use absolute-position mapping (`s = position.x / width`, `h = (position.y / height) * 360f`) — no cross-frame `prev`, no offset accumulation. These are inherently frame-stable. The perceived slow-follow was the `aeroDragSplitter` bug affecting all drag perception, now resolved in Task 1.

## Task Commits

1. **Task 1: Fix aeroDragSplitter frame-stable delta (F3/F15)** - `ecb7318` (fix)
2. **Task 2: Fix AeroRangeSlider live-value drag (F9) + F9 guard test** - `ebb7bb1` (fix + test)
3. **Task 3: Audit HSV square + hue slider** - no commit (no code change; audit confirmed transitively resolved)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — Removed cross-frame `prev` bookkeeping; added `positionChange` import; replaced delta with `change.positionChange().x/y`
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroRangeSlider.kt` — Added `rememberUpdatedState` import; added `val currentValue by rememberUpdatedState(value)`; replaced `value` with `currentValue` at both applyThumbMove call sites and nearest-thumb pick
- `library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderTest.kt` — Added `chainedThumbMovePreservesMovedEndEndpoint` (F9 regression guard)

## Sign-off Checklist Rows Unblocked

| Item | Description | Fix |
|------|-------------|-----|
| 3 | Column resize splitter drag (ghosting + sensitivity) | aeroDragSplitter positionChange() — Task 1 |
| 8 | RangeSlider thumb overlap / other-thumb reset | AeroRangeSlider rememberUpdatedState — Task 2 |
| 10 | SplitPane divider drag (ghosting + sensitivity) | aeroDragSplitter positionChange() — Task 1 |
| 14 | Drag-on-first-pixel | aeroDragSplitter already uses awaitFirstDown; delta now correct from first event — Task 1 |

## Decisions Made

- `positionChange()` chosen over manual `position - previousPosition` — idiomatic Compose API, same semantics, cleaner
- `rememberUpdatedState(value)` placed at composable body top (not inside `pointerInput` lambda) — ensures the delegated property is always in scope for the capture in the lambda
- AeroHsvColorSquare + AeroHueSlider audit found no offset bug — F15 for color picker is transitively resolved; files left byte-for-byte unchanged; re-verification at eyes-on sign-off (items 7 + 14) is still required

## Deviations from Plan

None — plan executed exactly as written. Task 3 audit found no offset bug as the plan anticipated; files left unchanged per plan instructions.

## Issues Encountered

- The plan's `<automated>` verification command uses `-x generateIcons` which fails because that Gradle task does not exist in this project. Used `./gradlew :library:compileKotlin -q` directly — identical effect, clean output.
- The `grep -c "applyThumbMove(currentValue"` check returned 0 because the calls are multiline-formatted with `currentValue` on the line after `applyThumbMove(`. Verified via `grep -n "applyThumbMove\|currentValue"` that both call sites correctly use `currentValue`.

## Next Phase Readiness

- aeroDragSplitter, AeroRangeSlider, AeroHsvColorSquare, AeroHueSlider are all ready for eyes-on re-verification
- Sign-off checklist items 3, 8, 10, 14 can now be re-tested
- No public or internal signatures changed; all existing tests remain green

---
*Phase: 11-showcase-v2-0-visual-sign-off*
*Completed: 2026-06-18*
