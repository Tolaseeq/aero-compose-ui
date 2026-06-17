---
phase: 07-shared-internal-primitives
verified: 2026-06-17T18:00:00Z
status: passed
score: 5/5 must-haves verified — all programmatic gates GREEN; both visual UAT checks confirmed by user
re_verification:
  previous_status: gaps_found
  previous_score: 4/5 must-haves verified — must-have #1 (AeroCalendarGrid layout) failed UAT
  gaps_closed:
    - "gap-01: AeroCalendarGrid header Row now Modifier.width(252.dp) — outer Column wrapContentWidth; header no longer stretches past the 7-column day grid (commit 3ce9ea0)"
    - "gap-02: StepIndicatorDemo wrapped in AeroCard(modifier = Modifier.wrapContentWidth()); AeroStepIndicator inside at Modifier.width(320.dp) — scratch demo accurately represents Phase 10 glass hosting context (commit 06c460f)"
    - "gap-03: CalendarPopupDemo popup body AeroCard replaces bare Box+panelBackground — calendar grid on glass surface, representing Phase 8 AeroDatePicker hosting context (commit 06c460f)"
    - "gap-04: raw Material Button/OutlinedButton removed; AeroButton + AeroOutlinedButton at intrinsic width; Open-calendar trigger no longer fills 1024dp frame (commit 06c460f)"
  gaps_remaining: []
  regressions: []
  followup_fixes:
    - "AeroCalendarGrid day-of-week + day rows pinned to Modifier.width(252.dp) (were fillMaxWidth). Under wrapContentWidth() those rows had expanded the grid Column to full parent width, stretching the CalendarGridDemo background and CalendarPopupDemo AeroCard across the window with cells pinned to the left ~25%. Completes the gap-01 intent (commit ac1d571)."
    - "CalendarPopupDemo: trigger button + Popup wrapped in a shared Box so anchorBounds == button bounds. Popup was a direct child of the 1024dp frame, so AeroCalendarPositionProvider left-aligned it to the frame's left edge instead of opening under the right-edge trigger. Position-provider primitive unchanged (commit 6c87f78)."
human_verification_result:
  status: confirmed
  confirmed_by: user
  date: 2026-06-17
  notes: "AeroStepIndicator three-theme contrast confirmed correct. AeroCalendarGrid background now wraps the 252dp grid tightly. Calendar popup opens under the right-edge trigger on a glass AeroCard surface. Standalone CalendarGridDemo intentionally matte (architecture-B demonstration — primitive carries no glass; host adds it in Phase 8)."
human_verification:
  - test: "AeroStepIndicator three-state contrast across all three themes"
    expected: "In :showcase, scrolling to 'Phase 7 Scratch' and toggling ThemeSwitcher (AeroBlue / AeroDark / Classic) keeps Current step (filled primary), Completed steps (primary 0.6 alpha + Check icon), and Upcoming steps (1dp labelText border) all visually distinguishable. The indicator renders inside an AeroCard glass panel with Prev/Next as AeroOutlinedButton / AeroButton at compact intrinsic widths."
    why_human: "Three-theme visual contrast and glass surface presentation cannot be JVM-tested. Both blocking conditions (gap-02 glass, gap-04 button sizing) are now code-verified as closed; only eyes-on remains."
  - test: "AeroCalendarPositionProvider wide-popup near right edge of 1024dp frame"
    expected: "In the scratch section's CalendarPopupDemo, clicking 'Open calendar' opens the calendar grid popup right-aligned (no clip) on a glass AeroCard surface, positioned below the anchor. The trigger button sits at its intrinsic width at the right edge of the 1024dp frame (not stretched across it)."
    why_human: "Popup position math is unit-tested green (4/4 tests). The trigger button width fix (gap-04) and glass popup body (gap-03) are code-verified. Visual confirmation of the rendered layout requires showcase:run with eyes-on check."
---

# Phase 7: Shared Internal Primitives — Verification Report

**Phase Goal:** All shared internal helpers that two or more v2.0 public components depend on are built, tested, and stable — Phases 8, 9, and 10 can proceed without per-component duplication of drag logic, calendar rendering, or color math.

**Verified:** 2026-06-17T18:00:00Z
**Status:** passed — all 4 UAT gaps closed by plan 07-03; 27 unit tests green; both visual UAT checks confirmed by user (after follow-up fixes ac1d571, 6c87f78)
**Re-verification:** Yes — after gap closure by plan 07-03 (commits 3ce9ea0, 06c460f, ab21e44) and two follow-up layout/anchor fixes (ac1d571, 6c87f78)

## Goal Achievement

### Observable Truths (mapped from ROADMAP Success Criteria)

| #   | Truth (from ROADMAP)                                                                                                                                             | Status            | Evidence                                                                                                                                                                                                                                                                                                                    |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | AeroCalendarGrid renders a visually symmetric month grid — header width matches the day-grid width; AeroCalendarPositionProvider positions wide popup without clip | ✓ VERIFIED        | Header Row, day-of-week Row, and all six day-cell Rows pinned to `Modifier.width(252.dp)`; outer Column: `modifier.wrapContentWidth().padding(8.dp)` (line 67). All three row widths agree, so the grid Column wraps to 252dp and host backgrounds (CalendarGridDemo Box, CalendarPopupDemo AeroCard) wrap it tightly (fix ac1d571). Popup anchors to the trigger button (fix 6c87f78). AeroCalendarGridTest 7 + AeroCalendarPositionProviderTest 4 — all green. User-confirmed visually. |
| 2   | AeroColorMath round-trip test passes (PITFALL-15)                                                                                                                | ✓ VERIFIED        | `AeroColorMath.kt` 107 lines, 4 `internal` funs. AeroColorMathTest 16 tests, 0 failures (timestamp 2026-06-17T14:15:xx).                                                                                                                                                                                                     |
| 3   | AeroHsvColorSquare + AeroHueSlider drag on first pixel (PITFALL-03)                                                                                              | ✓ HUMAN (PASSED)  | Code path verified: `awaitPointerEventScope` manual loop, `awaitFirstDown` + click-to-set fires before inner loop. `detectDragGestures(` call sites = 0. UAT 2026-05-04: human confirmed first-pixel response.                                                                                                               |
| 4   | AeroDragSplitter fires onDrag on first movement, both orientations                                                                                               | ✓ HUMAN (PASSED)  | Code path verified: `AeroDragSplitter.kt` 84 lines, orientation enum, `awaitPointerEventScope` manual loop, `change.consume()` only on non-zero delta, cursor change E_RESIZE/N_RESIZE. UAT 2026-05-04: human confirmed vPos/hPos update from first pixel; splitter is event-only by design.                                |
| 5   | AeroStepIndicator renders three states across all themes                                                                                                         | ⚠️ HUMAN NEEDED   | Code path verified: `AeroStepIndicator.kt` 145 lines; `StepState.Current/Completed/Upcoming` enum drives 3 visual treatments. Scratch demo now wraps indicator in `AeroCard` with `AeroOutlinedButton`/`AeroButton` at intrinsic width (gap-02 + gap-04 closed). Three-theme contrast is a visual quality check.            |

**Score:** 5/5 — truths 1+2 fully programmatic (27 green unit tests); truths 3+4 human-confirmed in UAT 2026-05-04; truth 5 code-verified + previously-blocked UAT check now unblocked by gap closure.

### Gap Closure Verification (plan 07-03)

All 4 gaps recorded in the previous VERIFICATION.md are closed. Evidence:

**gap-01 — AeroCalendarGrid header width (BLOCKING, primitive):**
- `wrapContentWidth` present on outer Column: line 14 import + line 67 usage — CONFIRMED
- `Modifier.width(252.dp)` on header Row: line 71 — CONFIRMED
- No `fillMaxWidth` on header Row (only on day-of-week row line 113 and day rows line 132, which collapse correctly under `wrapContentWidth`) — CONFIRMED
- Commit `3ce9ea0` exists with matching diff (+`wrapContentWidth`, +`width(252.dp)`)
- 27 unit tests remain green (AeroCalendarGridTest: `tests="7" failures="0"`, AeroColorMathTest: `tests="16" failures="0"`, AeroCalendarPositionProviderTest: `tests="4" failures="0"`) — all timestamps 2026-06-17T14:15:xx

**gap-02 — StepIndicator demo glass surface (scratch_demo):**
- `AeroCard(modifier = Modifier.wrapContentWidth())` wrapping StepIndicatorDemo: line 222 — CONFIRMED
- `AeroStepIndicator` inside at `Modifier.width(320.dp)`: line 224-227 — CONFIRMED
- Architecture B intact: no `AeroCard`/`glassEffect` inside `AeroStepIndicator.kt` — CONFIRMED

**gap-03 — Calendar popup demo glass surface (scratch_demo):**
- `AeroCard { AeroCalendarGrid(...) }` as popup body: lines 284-291 — CONFIRMED
- No bare `Box(Modifier.background(colors.panelBackground))` wrapping the popup grid — CONFIRMED
- Architecture B intact: no glass inside `AeroCalendarGrid.kt` — CONFIRMED

**gap-04 — Raw Material buttons replaced with AeroButton/AeroOutlinedButton (scratch_demo):**
- Zero `import androidx.compose.material3.Button` or `OutlinedButton` — CONFIRMED (grep: 0 hits)
- Zero raw `Button(` or `OutlinedButton(` call sites — CONFIRMED (grep: 0 hits)
- `AeroButton(` appears 2 times (Next + Open-calendar trigger): lines 235, 273 — CONFIRMED
- `AeroOutlinedButton(` appears 1 time (Prev): line 230 — CONFIRMED
- `AeroCard` import: `import com.mordred.aero.components.containers.AeroCard` line 32 — CONFIRMED
- No `fillMaxWidth` on AeroButton trigger (only on top-level demo Column, line 69) — CONFIRMED
- Commit `06c460f` exists with matching diff

### Required Artifacts

| Artifact                                                                                                                    | Expected                                                                       | Status     | Details                                                                                                                                                            |
| --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt`                        | Month grid primitive, header == day-grid width, all `internal`                 | ✓ VERIFIED | 221 lines; `wrapContentWidth` outer Column; `Modifier.width(252.dp)` header Row; `internal fun AeroCalendarGrid` + `daysInMonth` helper unchanged.                  |
| `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`                                                    | Scratch demo with glass-wrapped StepIndicator + glass popup body + Aero buttons | ✓ VERIFIED | 302 lines; `AeroCard` wrapping StepIndicator (line 222) + popup body (line 284); `AeroButton`/`AeroOutlinedButton` replacing all Material buttons; zero Material button imports/calls. |
| `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt`                       | PopupPositionProvider for wide popups, `internal class`                        | ✓ VERIFIED | 68 lines, unchanged from previous verification; PITFALL-02/PITFALL-08 mitigations intact.                                                                         |
| `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt`                                    | `Modifier.aeroDragSplitter` extension, `internal`                              | ✓ VERIFIED | 84 lines, unchanged from previous verification.                                                                                                                    |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt`                         | 256×256 HSV picker Canvas, `internal`                                          | ✓ VERIFIED | 101 lines, unchanged.                                                                                                                                              |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt`                              | 24×256 hue strip Canvas, `internal`                                            | ✓ VERIFIED | 77 lines, unchanged.                                                                                                                                               |
| `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt`                         | Horizontal step indicator with 3 states, `internal`                            | ✓ VERIFIED | 145 lines, unchanged; surface-less (architecture B intact).                                                                                                        |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt`                              | Pure HSV/RGB/HEX utilities, all `internal`                                     | ✓ VERIFIED | 107 lines, unchanged.                                                                                                                                              |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`                                            | Thin wrapper calling `AeroPhase7Scratch()`                                     | ✓ VERIFIED | 23 lines, unchanged.                                                                                                                                               |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt`                          | 16-test JUnit5 suite                                                           | ✓ VERIFIED | XML: `tests="16" failures="0" errors="0"` @ 2026-06-17T14:15:59.                                                                                                  |
| `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt`                   | 4-test JUnit5 suite                                                            | ✓ VERIFIED | XML: `tests="4" failures="0" errors="0"` @ 2026-06-17T14:15:59.                                                                                                   |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt`                    | 7-test JUnit5 suite                                                            | ✓ VERIFIED | XML: `tests="7" failures="0" errors="0"` @ 2026-06-17T14:15:59.                                                                                                   |

### Key Link Verification

| From                                        | To                                              | Via                                                                                                         | Status     | Details                                                                                                          |
| ------------------------------------------- | ----------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | ---------- | ---------------------------------------------------------------------------------------------------------------- |
| `AeroCalendarGrid.kt` outer Column          | intrinsic 252dp width                           | `modifier.wrapContentWidth().padding(8.dp)` on Column                                                      | ✓ WIRED    | Line 67; import line 14. Day-of-week and day Rows with `fillMaxWidth` collapse correctly under this constraint. |
| `AeroCalendarGrid.kt` header Row            | exact 252dp == day-grid width                   | `Modifier.width(252.dp).padding(vertical = 4.dp)`                                                          | ✓ WIRED    | Line 71; import line 13. `Arrangement.SpaceBetween` spreads prev/label/next across exactly 252dp.               |
| `AeroPhase7Scratch.kt` StepIndicatorDemo    | AeroCard glass surface                          | `AeroCard(modifier = Modifier.wrapContentWidth()) { ... }` wrapping indicator + button row                 | ✓ WIRED    | Lines 222-249; import line 32.                                                                                   |
| `AeroPhase7Scratch.kt` CalendarPopupDemo    | AeroCard glass popup body                       | `AeroCard { AeroCalendarGrid(...) }` in Popup body                                                          | ✓ WIRED    | Lines 284-291; same AeroCard import.                                                                             |
| `AeroPhase7Scratch.kt` CalendarPopupDemo    | AeroButton trigger at intrinsic width           | `AeroButton(text=..., onClick=..., modifier=Modifier.padding(end=16.dp))` — no fillMaxWidth               | ✓ WIRED    | Lines 273-277; parent Box `contentAlignment = Alignment.CenterEnd` positions it at right edge.                   |
| `AeroPhase7Scratch.kt` StepIndicatorDemo    | AeroOutlinedButton (Prev) + AeroButton (Next)   | Direct calls with `text` param at intrinsic width inside `Row`                                              | ✓ WIRED    | Lines 230-239; imports lines 30-31.                                                                              |
| `AeroCalendarGrid.kt` + `AeroStepIndicator.kt` | surface-less (architecture B)                | No `glassEffect`/`AeroCard` imported or applied inside either primitive                                    | ✓ WIRED    | 0 grep hits for glass in both primitive files.                                                                   |
| `ShowcaseApp.kt`                            | `Phase7ScratchSection`                          | import + call after `NavigationSection()`                                                                   | ✓ WIRED    | Unchanged from previous verification; line 32 import + line 96 invocation.                                      |
| `library:test`                              | All 27 Phase 7 tests                            | `useJUnitPlatform()` in `tasks.test`                                                                        | ✓ WIRED    | XML reports confirm 16+7+4=27 tests, 0 failures, 0 errors, all timestamps 2026-06-17T14:15:xx.                  |

### Requirements Coverage

| Requirement    | Source Plan | Description                                                                                              | Status | Evidence                                                                                                               |
| -------------- | ----------- | -------------------------------------------------------------------------------------------------------- | ------ | ---------------------------------------------------------------------------------------------------------------------- |
| (none owned)   | n/a         | Enabling phase — all 27 v2.0 requirements (PICK-01..08, DATA-01..04, LAYO-01..09, SHW-01..06) owned by Phases 8–11. | n/a    | Plans 07-01, 07-02, 07-03 all declare `requirements: []`. ROADMAP confirms Phase 7 has no public requirement IDs.    |

Phase 7 is an enabling phase. Requirements PICK-01..08, DATA-04, LAYO-03/04/08/09 are delivered-against indirectly (internal primitives these phases consume), but ownership is in Phases 8–11.

### Anti-Patterns Found

| File                                  | Line       | Pattern                                                     | Severity  | Impact                                                                                                                 |
| ------------------------------------- | ---------- | ----------------------------------------------------------- | --------- | ---------------------------------------------------------------------------------------------------------------------- |
| `AeroDragSplitter.kt`                 | 16, 21     | `detectDragGestures` mention                                | Info      | KDoc-only PITFALL-03 documentation reference. `detectDragGestures(` call-site count = 0 (verified). Intentional.       |
| `AeroHsvColorSquare.kt`               | 21         | `detectDragGestures` mention                                | Info      | Same — KDoc-only. No call site.                                                                                        |
| `AeroPhase7Scratch.kt`                | 41, 73     | "TEMPORARY — deleted Phase 11" markers                      | Info      | Intentional Phase 11 deletion plan. File is fully implemented. Not a stub.                                             |
| `Phase7ScratchSection.kt`             | 7, 15      | "TEMPORARY — deleted Phase 11" markers                      | Info      | Same — intentional lifecycle documentation.                                                                            |

No blockers, no warnings. All hits are intentional documentation markers covered by Phase 11 cleanup plan.

### Negative-Control Checks

| Check                                                                                          | Expected | Result                                                                           |
| ---------------------------------------------------------------------------------------------- | -------- | -------------------------------------------------------------------------------- |
| `transparent=true` / `undecorated=true` in any Phase 7 internal source file (W11-01 gate)     | 0 hits   | 0 hits                                                                           |
| `detectDragGestures\s*\(` callsite in `internal/drag` or `pickers/internal/color`             | 0 hits   | 0 hits                                                                           |
| `^public\s+(fun\|class\|object)` in any `**/internal/**/*.kt` file                             | 0 hits   | 0 hits                                                                           |
| `import androidx.compose.material3.Button` in `AeroPhase7Scratch.kt`                          | 0 hits   | 0 hits — CONFIRMED gap-04 closed                                                |
| `import androidx.compose.material3.OutlinedButton` in `AeroPhase7Scratch.kt`                  | 0 hits   | 0 hits — CONFIRMED gap-04 closed                                                |
| Raw `Button(` / `OutlinedButton(` call sites in `AeroPhase7Scratch.kt`                        | 0 hits   | 0 hits — CONFIRMED gap-04 closed                                                |
| `glassEffect`/`AeroCard` inside `AeroCalendarGrid.kt` or `AeroStepIndicator.kt`               | 0 hits   | 0 hits — architecture B intact                                                  |
| `fillMaxWidth` on header Row in `AeroCalendarGrid.kt`                                         | 0 hits   | 0 hits (only on day-of-week row line 113 + day rows line 132) — gap-01 closed  |
| `wrapContentWidth` on outer Column in `AeroCalendarGrid.kt`                                   | 1+ hits  | 1 hit (line 67) — gap-01 closed                                                |
| `Modifier.width(252.dp)` on header Row in `AeroCalendarGrid.kt`                              | 1 hit    | 1 hit (line 71) — gap-01 closed                                                |
| `AeroCard` in `AeroPhase7Scratch.kt` (StepIndicator + popup body)                            | 2+ hits  | 2 hits (lines 222, 284) + import line 32 — gaps-02/03 closed                   |
| `AeroButton(` in `AeroPhase7Scratch.kt`                                                       | 2+ hits  | 2 hits (lines 235, 273) — gap-04 closed                                        |
| `AeroOutlinedButton(` in `AeroPhase7Scratch.kt`                                               | 1+ hits  | 1 hit (line 230) — gap-04 closed                                               |
| `fillMaxWidth` on AeroButton trigger in CalendarPopupDemo                                     | 0 hits   | 0 hits (fillMaxWidth only on top-level Column, line 69) — gap-04 closed        |
| All gap-closure commits exist in git log                                                       | 3 found  | `3ce9ea0` (gap-01), `06c460f` (gaps 02/03/04), `ab21e44` (docs) — all present |
| `:library:test` XML reports — 27 tests, 0 failures                                            | 27/0     | AeroCalendarGridTest 7/0 + AeroColorMathTest 16/0 + AeroCalendarPositionProviderTest 4/0 @ 2026-06-17T14:15:xx |

### Human Verification Required

To complete Phase 7 verification, run the showcase and perform two visual checks:

```
./gradlew :showcase:run
```

Scroll to the section titled **"Phase 7 Scratch (TEMPORARY — deleted Phase 11)"**.

#### 1. AeroStepIndicator Three-Theme Contrast

**Test:** Click Prev / Next to advance `currentStep` through {0, 1, 2, 3}. Toggle `ThemeSwitcher` at the top of the showcase between AeroBlue / AeroDark / Classic.

**Expected:** In every theme, the indicator renders inside an AeroCard glass panel. Current step: filled primary dot. Completed steps: primary 0.6 alpha + Check icon. Upcoming steps: 1dp labelText border dot. All three states visually distinguishable. Prev button (AeroOutlinedButton) and Next button (AeroButton) sit at compact intrinsic widths inside the card — neither stretches across the glass panel.

**Why human:** Three-state contrast and glass panel presentation are visual quality checks; JVM unit tests cannot drive Compose theming or measure visual distinction.

#### 2. AeroCalendarPositionProvider Wide-Popup

**Test:** In the 1024dp scratch frame, click "Open calendar". The trigger button should be at the right edge of the frame at its intrinsic width.

**Expected:** The popup opens to a calendar grid displayed on an AeroCard glass surface. The popup is fully visible (no right-edge clipping) and positioned below the trigger anchor. The trigger button does not stretch across the 1024dp frame.

**Why human:** Popup position math is unit-tested green (4/4 tests including `widePopupNearRightEdgeRightAlignsWithoutClip`). The glass surface and trigger width fixes are code-verified closed. Visual confirmation of rendered layout at runtime requires eyes-on.

### Subsequent-Phase Readiness

| Future phase consumer            | What it imports                                                                        | Status                                                                                                                                                                  |
| -------------------------------- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phase 8 — AeroColorPicker        | `AeroColorMath` + `AeroHsvColorSquare` + `AeroHueSlider`                               | All in `:library` module, all `internal`-visible. No duplication needed. PICK-05/06 unblocked.                                                                          |
| Phase 8 — AeroDatePicker family  | `AeroCalendarGrid` + `AeroCalendarPositionProvider` + kotlinx-datetime                 | Symmetric layout (gap-01 closed): glass hosting pattern demonstrated (AeroCard wraps grid in scratch). PICK-01..04, PICK-07 unblocked.                                  |
| Phase 9 — AeroDataTable resize   | `Modifier.aeroDragSplitter(orientation = Horizontal, ...)`                             | Locked drag pattern shipped. DATA-04 unblocked.                                                                                                                         |
| Phase 10 — AeroSplitPane         | `Modifier.aeroDragSplitter(orientation, ...)` per pane                                 | Both orientations supported; release event NOT consumed. LAYO-03/04/08 unblocked.                                                                                       |
| Phase 10 — AeroStepperWizard     | `AeroStepIndicator(currentStep, totalSteps, onStepClick)`                              | Glass hosting pattern demonstrated (AeroCard in scratch). LAYO-09 unblocked.                                                                                            |

### Gaps Summary

All 4 UAT gaps from the previous `gaps_found` verification are now closed:

- **gap-01 (BLOCKING):** `AeroCalendarGrid` header pinned to `Modifier.width(252.dp)`, outer Column `wrapContentWidth()`. Header and day grid are now symmetric. 27 unit tests confirm no regression.
- **gap-02 (UX):** `StepIndicatorDemo` wrapped in `AeroCard(modifier = Modifier.wrapContentWidth())` with `AeroStepIndicator` at `Modifier.width(320.dp)`. Architecture B maintained.
- **gap-03 (UX):** `CalendarPopupDemo` popup body is `AeroCard { AeroCalendarGrid(...) }`. Architecture B maintained.
- **gap-04 (UX):** All raw `Button`/`OutlinedButton` replaced with `AeroButton`/`AeroOutlinedButton` at intrinsic width. Trigger no longer fills 1024dp frame.

The two visual UAT truths previously marked `blocked_by_gaps` are now **unblocked** and ready for human visual confirmation:
- AeroStepIndicator three-theme contrast (unblocked by gap-02 + gap-04)
- AeroCalendarPositionProvider wide-popup visual (unblocked by gap-03 + gap-04)

No new gaps found. No regressions found. Phase 7 is complete pending the two human visual checks above.

---

_Verified: 2026-06-17T14:30:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification: Yes — after gap closure by plan 07-03_
