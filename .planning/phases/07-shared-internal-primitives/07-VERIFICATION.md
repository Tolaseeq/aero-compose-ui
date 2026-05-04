---
phase: 07-shared-internal-primitives
verified: 2026-05-04T17:00:00Z
status: gaps_found
score: 4/5 must-haves verified — must-have #1 (AeroCalendarGrid layout) failed UAT
re_verification: 2026-05-04T18:30:00Z
re_verification_note: "Initial verification passed programmatically with status human_needed. UAT on 2026-05-04 surfaced 2 primitive-level gaps + 2 scratch-demo presentation gaps. Architecture decision (option B) locked: primitives stay surface-less; scratch demo and future public consumers (AeroDatePicker, AeroStepperWizard) own glass surfaces."
gaps:
  - id: gap-01-calendar-grid-layout
    severity: blocking
    must_have: "AeroCalendarGrid renders month grid"
    finding: "Inside AeroCalendarGrid, the month-selector header (prev / month-name / next row) is stretched via fillMaxWidth while the 7×6 day-cell grid retains its intrinsic compact width. Result: header fills the parent constraint, day grid sits left-aligned at ~25% of available width — visual asymmetry breaks the calendar."
    fix_scope: primitive
    target_file: "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt"
    fix_intent: "Header row width must match the day-grid width (do not fillMaxWidth on the header). Either constrain the whole AeroCalendarGrid to wrapContentWidth, or constrain the header to the same width as the grid (computed from cellSize × 7 + gaps). Day grid layout itself is correct — only the header needs to stop stretching past it."
    status: failed
  - id: gap-02-step-indicator-no-glass-wrapping
    severity: ux
    must_have: "AeroStepIndicator renders three states"
    finding: "AeroStepIndicator renders correctly but the scratch-demo wrapper currently shows it on a flat (non-glass) background, breaking aero aesthetic continuity with the rest of the showcase."
    fix_scope: scratch_demo
    target_file: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
    fix_intent: "Wrap the StepIndicator demo block (indicator + Prev/Next row) in AeroSurface (glass variant) so the demo accurately represents how AeroStepperWizard in Phase 10 will host it. Primitive itself stays surface-less per architecture B (consumer owns glass)."
    status: failed
  - id: gap-03-calendar-grid-no-glass-wrapping
    severity: ux
    must_have: "AeroCalendarGrid renders month grid"
    finding: "Scratch-demo CalendarPopupDemo shows the calendar grid without a glass background panel, breaking aero aesthetic continuity. The popup-positioning test passes but visual confirmation is harder to read against the bare background."
    fix_scope: scratch_demo
    target_file: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
    fix_intent: "Inside the Popup body, wrap AeroCalendarGrid in AeroSurface (glass variant) so the demo accurately represents how AeroDatePicker in Phase 8 will host it. Primitive itself stays surface-less per architecture B."
    status: failed
  - id: gap-04-scratch-buttons-not-aero-style
    severity: ux
    must_have: "Phase7ScratchSection wired into showcase"
    finding: "Scratch demo uses raw Material Button(...) calls for Prev/Next (StepIndicator) and Open Calendar — these widgets stretch to fill their parent (the OpenCalendar trigger ends up at the right edge of an 80%-empty wide background) and visually clash with AeroBlue/AeroDark theming, since they don't use the existing AeroButton primitive."
    fix_scope: scratch_demo
    target_file: "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
    fix_intent: "Replace all raw Button(...) calls with AeroButton (or whatever the existing showcase pattern uses for Prev/Next pairs). Wrap each button in a container (Row(arrangement = Center) or Box(contentAlignment = Center)) so the trigger does not stretch to full container width. OpenCalendar trigger must sit at its intrinsic width (no fillMaxWidth)."
    status: failed
human_verification:
  - test: "HSV square + hue slider drag on first pixel (PITFALL-03)"
    expected: "Click anywhere on AeroHsvColorSquare moves the indicator immediately (no 18dp slop wait); dragging slowly across 1-2 px updates h/s/v continuously. Same for AeroHueSlider."
    status: passed_human_check
    why_human: "PITFALL-03 is a UX-feel bug: the code path uses awaitPointerEventScope (verified via grep) but only a real mouse-drag at the OS layer can confirm 'first-pixel response' subjectively. JVM unit tests cannot drive Compose pointerInput."
  - test: "aeroDragSplitter horizontal + vertical drag on first pixel"
    expected: "Dragging the 16dp wide horizontal handle in the scratch section moves the hPos counter on the very first pixel of motion; same for the vertical handle and vPos counter. Cursor changes to E_RESIZE / N_RESIZE on hover."
    status: passed_human_check
    why_human: "Same as above — locked v2.0 drag pattern can only be eyes-on confirmed via :showcase:run + manual mouse drag. UAT 2026-05-04 confirmed: vPos/hPos update from first pixel; user explicitly verified that the splitter modifier is purely event-emitting (visual move is the consumer's job, by design — splitter alone is not supposed to move panes)."
  - test: "AeroStepIndicator three-state contrast across all three themes"
    expected: "In :showcase, toggling ThemeSwitcher (AeroBlue / AeroDark / Classic) keeps Current step (filled primary), Completed steps (primary 0.6 alpha + Check icon), and Upcoming steps (1dp labelText border) all visually distinguishable."
    status: blocked_by_gaps
    why_human: "Three-state contrast cannot be confirmed until the demo wraps the indicator in a glass surface (gap-02) and the Prev/Next buttons stop dominating the layout (gap-04)."
  - test: "AeroCalendarPositionProvider wide-popup near right edge of 1024dp frame"
    expected: "In the scratch section's CalendarPopupDemo, the trigger button sits at the right edge of a 1024dp box; clicking it opens a 320dp+ wide AeroCalendarGrid popup that right-aligns (does not clip) and stays below the anchor."
    status: blocked_by_gaps
    why_human: "Position math is unit-tested green (4/4); but the trigger button's full-width stretching (gap-04) and the popup body's missing glass surface (gap-03) prevent a clean visual assessment. After scratch-demo gaps close, this becomes re-checkable."
---

# Phase 7: Shared Internal Primitives — Verification Report

**Phase Goal:** All shared internal helpers that two or more v2.0 public components depend on are built, tested, and stable — Phases 8, 9, and 10 can proceed without per-component duplication of drag logic, calendar rendering, or color math.

**Verified:** 2026-05-04T17:00:00Z
**Status:** human_needed (all programmatic gates GREEN; Wave 2 visual SC3/SC4/SC5 require manual `:showcase:run` eyes-on as pre-`/gsd:verify-work` step per plan-02 design)
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (mapped from ROADMAP Success Criteria)

| #   | Truth (from ROADMAP)                                                                                                                                                                | Status                | Evidence                                                                                                                                                                                                                                                                                                                                                                |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | AeroCalendarGrid renders month grid + 3-scenario unit test green; AeroCalendarPositionProvider positions wide popup without clip on 1024dp window                                   | ✓ VERIFIED            | `AeroCalendarGrid.kt` 219 lines, `internal fun` composable + `daysInMonth` helper + Mon/Sun row + 6×7 grid. `AeroCalendarGridTest.kt` runs 7 tests (April=30, leap year Feb 2024=29, non-leap Feb 2023=28, century non-leap 1900=28, century leap 2000=29, Dec→Jan crosses year, compile-smoke) — all green. `AeroCalendarPositionProviderTest.kt` runs 4 tests — all green (incl. wide-popup right-edge re-anchor on 1024dp window). |
| 2   | AeroColorMath round-trip test passes (PITFALL-15)                                                                                                                                   | ✓ VERIFIED            | `AeroColorMath.kt` 107 lines, all 4 utilities (`rgbToHsv`, `hexToRgb`, `hexToRgba`, `rgbToHex`) `internal`. `AeroColorMathTest.pureRedRoundTripPreservesHueWithinTolerance` asserts `abs(hue - 0f) < 0.001f` for `Color.hsv(0,1,1) → rgb → rgbToHsv` round-trip — green. 16 tests total in this class, all green.                                                          |
| 3   | AeroHsvColorSquare + AeroHueSlider drag on first pixel (PITFALL-03)                                                                                                                 | ⚠️ HUMAN NEEDED       | Code path verified: both files use `awaitPointerEventScope` + manual loop (grep confirms 3 hits across drag/HSV/Hue files); `awaitFirstDown` import present; click-to-set fires `onSatValChange`/`onHueChange` BEFORE the inner-loop. `detectDragGestures(` callsite count = 0 in internal directories. Real-mouse first-pixel drag is a UX-feel test that JVM unit tests cannot drive. |
| 4   | AeroDragSplitter fires onDrag on first movement, both orientations                                                                                                                  | ⚠️ HUMAN NEEDED       | Code path verified: `AeroDragSplitter.kt` 84 lines, `internal fun Modifier.aeroDragSplitter`; orientation enum gates 1D delta; `awaitPointerEventScope` + manual loop; cursor change keyed via `remember(orientation)` to E_RESIZE/N_RESIZE; `change.consume()` only on non-zero delta. Same human-test concern as Truth 3.                                            |
| 5   | AeroStepIndicator renders three states across all themes                                                                                                                            | ⚠️ HUMAN NEEDED       | Code path verified: `AeroStepIndicator.kt` 145 lines; explicit `StepState.Current/Completed/Upcoming` enum drives 3 visual treatments (filled primary, primary 0.6 alpha + Check icon, 1dp labelText border); 2dp connector lines colored by side-of-currentStep. Compiles clean. Three-theme contrast (AeroBlue / AeroDark / Classic) is a visual quality check.       |

**Score:** 5/5 must-haves verified — 2 fully programmatic (truths 1+2 backed by 27 green unit tests), 3 deferred to human eyes-on (truths 3, 4, 5 — Wave 2 visual primitives whose code paths are programmatically verified but whose real-mouse / theme-contrast UX cannot be JVM-tested).

### Required Artifacts

| Artifact                                                                                                                | Expected                                                                       | Status     | Details                                                                                                                                                |
| ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMath.kt`                           | Pure HSV/RGB/HEX utilities, all `internal`                                     | ✓ VERIFIED | 107 lines, 4 internal funs, hue convention `[0f, 360f]` ADR in KDoc, no `transparent=true` introduced.                                                  |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt`                     | Month grid composable + `daysInMonth` helper, all `internal`                   | ✓ VERIFIED | 219 lines, `internal fun AeroCalendarGrid` + `internal fun daysInMonth`. Imports kotlinx-datetime LocalDate/Month. Uses `AeroIcons.CaretLeft/CaretRight`. |
| `library/src/main/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProvider.kt`                    | PopupPositionProvider for wide popups, `internal class`                        | ✓ VERIFIED | 68 lines, `internal class` with `gap` parameter, first-frame guard via `popupContentSize == IntSize.Zero` (PITFALL-08), width overflow re-anchors horizontally (PITFALL-02). |
| `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt`                                 | `Modifier.aeroDragSplitter` extension, `internal`                              | ✓ VERIFIED | 84 lines, `internal fun Modifier.aeroDragSplitter` + orientation/onDrag/onDragEnd/enabled, `awaitPointerEventScope` manual loop (PITFALL-03).         |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt`                      | 256×256 HSV picker Canvas, `internal`                                          | ✓ VERIFIED | 101 lines, `internal fun AeroHsvColorSquare`, `awaitPointerEventScope` + click-to-set on first mouse-down, double-stroke 8dp indicator.                |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt`                           | 24×256 hue strip Canvas, `internal`                                            | ✓ VERIFIED | 77 lines, `internal fun AeroHueSlider`, 7-stop R→Y→G→C→B→M→R gradient, indicator at `(hue/360f) * height`.                                              |
| `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt`                      | Horizontal step indicator with 3 states, `internal`                            | ✓ VERIFIED | 145 lines, `internal fun AeroStepIndicator` + `StepState` enum + `StepDot` private composable.                                                          |
| `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`                                                 | Public scratch aggregator (deleted Phase 11)                                   | ✓ VERIFIED | 301 lines, `public fun AeroPhase7Scratch` — exercises all 6 primitives (CalendarGrid, HSV, HueSlider, DragSplitter H+V, StepIndicator 4 steps, CalendarPositionProvider on 1024dp frame). |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`                                        | Thin wrapper that calls `AeroPhase7Scratch()`                                  | ✓ VERIFIED | 23 lines, single-call delegation to library aggregator. Imported from `com.mordred.aero.scratch.AeroPhase7Scratch`.                                    |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/color/AeroColorMathTest.kt`                       | 16-test JUnit5 suite (HSV/RGB/HEX, round-trip)                                 | ✓ VERIFIED | XML report shows `tests="16" failures="0" errors="0"`.                                                                                                  |
| `library/src/test/kotlin/com/mordred/aero/components/internal/popup/AeroCalendarPositionProviderTest.kt`                | 4-test JUnit5 suite (PITFALL-02 / PITFALL-08)                                  | ✓ VERIFIED | XML report shows `tests="4" failures="0" errors="0"`.                                                                                                   |
| `library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt`                 | 7-test JUnit5 suite (leap-year + month boundary + compile-smoke)               | ✓ VERIFIED | XML report shows `tests="7" failures="0" errors="0"`.                                                                                                   |
| `gradle/libs.versions.toml`                                                                                             | kotlinx-datetime 0.6.2 declared                                                | ✓ VERIFIED | Lines 6 + 13 contain `kotlinxDatetime = "0.6.2"` and `kotlinx-datetime` library entry.                                                                  |
| `library/build.gradle.kts`                                                                                              | `implementation(libs.kotlinx.datetime)` added                                  | ✓ VERIFIED | Confirmed in dependencies block; `explicitApi()` also enforced.                                                                                          |
| `.planning/phases/07-shared-internal-primitives/07-SPIKE-touchslop.md`                                                  | Locked-pattern spike note                                                       | ✓ VERIFIED | File exists, 1183 bytes.                                                                                                                                 |
| `.gitignore`                                                                                                            | `.kotlin/` Kotlin 2.x cache directory ignored                                  | ✓ VERIFIED | Line 2: `.kotlin/`.                                                                                                                                      |

### Key Link Verification

| From                                  | To                                                                            | Via                                                                                                  | Status     | Details                                                                                                                                                                                       |
| ------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ShowcaseApp.kt`                      | `Phase7ScratchSection`                                                        | `import com.mordred.showcase.sections.Phase7ScratchSection` + call after `NavigationSection()`        | ✓ WIRED    | Line 32 import + line 96 invocation.                                                                                                                                                          |
| `Phase7ScratchSection.kt`             | `AeroPhase7Scratch`                                                           | `import com.mordred.aero.scratch.AeroPhase7Scratch` + call inside `Phase7ScratchSection()`           | ✓ WIRED    | One-line wrapper delegating to library aggregator.                                                                                                                                            |
| `AeroPhase7Scratch.kt`                | All 6 internal primitives                                                     | Direct import (same module, internal-visible)                                                        | ✓ WIRED    | Imports include `aeroDragSplitter`, `AeroCalendarPositionProvider`, `AeroStepIndicator`, `AeroCalendarGrid`, `AeroHsvColorSquare`, `AeroHueSlider`. Each consumed inside its corresponding demo composable. |
| `AeroDragSplitter.kt` drag loop       | `awaitFirstDown` + `awaitPointerEventScope`                                   | Explicit imports + `pointerInput` block                                                              | ✓ WIRED    | Manual event loop processes pressed/non-pressed and dispatches `onDrag(delta)` only when delta ≠ 0; `change.consume()` only after dispatch (parent sees release).                              |
| `AeroHsvColorSquare.kt` Canvas        | `onSatValChange`                                                              | First mouse-down fires immediately + drag loop fires on every move                                    | ✓ WIRED    | Click-to-set UX: `down.position` computed and `onSatValChange(initS, initV)` invoked before the inner-loop starts.                                                                            |
| `AeroHueSlider.kt` Canvas             | `onHueChange`                                                                 | First mouse-down + drag loop                                                                          | ✓ WIRED    | Same click-to-set pattern; hue computed as `(y / height) * 360f`.                                                                                                                              |
| `AeroCalendarGrid.kt` header buttons  | `onMonthChange(displayMonth ± 1.MONTH)`                                       | `clickable { onMonthChange(displayMonth.plus/minus(1, DateTimeUnit.MONTH)) }`                         | ✓ WIRED    | kotlinx-datetime arithmetic used; UI fires callback on click.                                                                                                                                |
| `AeroCalendarGrid.kt` day cells       | `onDateSelected(cellDate)`                                                    | `DayCell` `onClick = { if (!disabled) onDateSelected(cellDate) }`                                    | ✓ WIRED    | Disabled gate present; selected dates are highlighted via `colors.primary` background.                                                                                                       |
| `AeroCalendarPositionProvider`        | First-frame guard returns `IntOffset.Zero`                                    | `if (popupContentSize == IntSize.Zero) return IntOffset.Zero`                                         | ✓ WIRED    | Struct equality used (NOT the broken `>= windowSize` heuristic). Verified by `firstFrameUnmeasuredPopupReturnsIntOffsetZero` test.                                                            |
| `AeroCalendarPositionProvider`        | Width overflow → right-align                                                  | `xRight = anchorBounds.right - popupContentSize.width; coerceAtLeast(0)`                              | ✓ WIRED    | `widePopupNearRightEdgeRightAlignsWithoutClip` test asserts position math; explicit assertion that y stays below (no Top/Bottom flip on width overflow).                                       |
| `AeroStepIndicator`                   | 3 visual states + AeroIcons.Check                                              | `StepState` enum drives `when` branches; `import com.mordred.aero.icons.\`internal\`.Check`           | ✓ WIRED    | `index < currentStep → Completed`, `==  currentStep → Current`, `> currentStep → Upcoming`. Connector colored by `i < currentStep`.                                                            |
| `library:test`                        | All 27 Phase 7 tests                                                          | `useJUnitPlatform()` in `tasks.test`                                                                  | ✓ WIRED    | XML test reports confirm 16 + 7 + 4 = 27 tests across 3 classes, 0 failures.                                                                                                                  |
| `:library:compileKotlin :showcase:compileKotlin` | Both modules compile                                                          | Gradle build                                                                                          | ✓ WIRED    | `BUILD SUCCESSFUL in 1s` (UP-TO-DATE — both modules previously compiled green).                                                                                                                |

### Requirements Coverage

| Requirement                          | Source Plan       | Description                                                                                                              | Status            | Evidence                                                                                                                                                |
| ------------------------------------ | ----------------- | ------------------------------------------------------------------------------------------------------------------------ | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (none owned)                         | n/a               | Enabling phase — all 27 v2.0 requirements (PICK-01..08, DATA-01..04, LAYO-01..09, SHW-01..06) are owned by Phases 8–11. | n/a               | ROADMAP.md confirms Phase 7 has no public requirement IDs; this phase enables the listed requirements without owning them.                              |

**Note:** Per phase brief, Phase 7 is an enabling phase (no public requirements owned). The cross-reference against `REQUIREMENTS.md` for owned IDs is intentionally skipped. Verification focused on the 5 ROADMAP success criteria (must-haves).

### Anti-Patterns Found

| File                              | Line  | Pattern                                                | Severity | Impact                                                                                                                                                  |
| --------------------------------- | ----- | ------------------------------------------------------ | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AeroDragSplitter.kt`             | 16,21 | `detectDragGestures` mention                           | ℹ️ Info  | KDoc-only mention documenting that the API is BANNED for v2.0. No call site. Pattern intentionally kept for future-reader safety.                       |
| `AeroHsvColorSquare.kt`           | 21    | `detectDragGestures` mention                           | ℹ️ Info  | Same — KDoc-only PITFALL-03 documentation reference. No call site (verified via `detectDragGestures\s*\(` precise grep — 0 hits).                        |
| `AeroPhase7Scratch.kt`            | 39, 71 | "TEMPORARY — deleted Phase 11" markers                 | ℹ️ Info  | Intentional Phase 11 deletion plan; scratch aggregator carries explicit lifecycle docs. Not a stub or TODO — file is fully implemented.                  |
| `Phase7ScratchSection.kt`         | 7,15  | "TEMPORARY — deleted in Phase 11" markers              | ℹ️ Info  | Same — intentional lifecycle documentation; thin wrapper is fully implemented.                                                                          |

**No blockers, no warnings.** All anti-pattern hits are intentional documentation markers covered by the Phase 11 cleanup plan (3 mechanical steps documented in plan-02 SUMMARY).

### Negative-control checks

| Check                                                                                          | Expected      | Result        |
| ---------------------------------------------------------------------------------------------- | ------------- | ------------- |
| `transparent\s*=\s*true` or `undecorated\s*=\s*true` in any Phase 7 source file (W11-01 gate)   | 0 hits        | ✓ 0 hits      |
| `detectDragGestures\s*\(` callsite in `internal/drag` or `pickers/internal/color`              | 0 hits        | ✓ 0 hits      |
| `^public\s+(fun\|class\|object)` in any `**/internal/**/*.kt` file                              | 0 hits        | ✓ 0 hits      |
| `internal` keyword on all 7 primitive declarations                                              | 7 hits        | ✓ 11 hits (incl. helpers like `daysInMonth` and 4 ColorMath funs all `internal`) |
| `:library:test` (full suite) exit code                                                          | 0             | ✓ BUILD SUCCESSFUL — UP-TO-DATE / all green   |
| `:library:compileKotlin :showcase:compileKotlin` exit code                                      | 0             | ✓ BUILD SUCCESSFUL                            |
| All 14 commit hashes from plan-01+plan-02 SUMMARYs resolve in `git log`                          | All 14 found  | ✓ All present (`2a4eaae`, `b48ecd5`, `cb18cc6`, `130da39`, `7c421ad`, `719a372`, `35f56c7`, `b7b160d`, `aedbb57`, `1a67347`, `149c6b5`, `7ead484`, `76371e5`, `34a6280`) |

### Subsequent-phase readiness

| Future phase consumer            | What it imports                                                                                  | Status                                                                                                                                                                                                                |
| -------------------------------- | ------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Phase 8 — AeroColorPicker        | `AeroColorMath` + `AeroHsvColorSquare` + `AeroHueSlider`                                         | ✓ All in same `:library` module, all `internal`-visible to other library composables. No duplication needed.                                                                                                          |
| Phase 8 — AeroDatePicker family  | `AeroCalendarGrid` + `AeroCalendarPositionProvider` + kotlinx-datetime LocalDate/Time            | ✓ All on classpath, all `internal`-visible. PITFALL-02 / PITFALL-08 mitigations baked into `AeroCalendarPositionProvider`. PICK-01..04, PICK-07 all unblocked.                                                         |
| Phase 9 — AeroDataTable resize   | `Modifier.aeroDragSplitter(orientation = Horizontal, ...)`                                       | ✓ Locked drag pattern shipped; column-resize can use `enabled` gate at min/max width bounds. DATA-04 unblocked.                                                                                                       |
| Phase 10 — AeroSplitPane         | `Modifier.aeroDragSplitter(orientation, ...)` per pane                                           | ✓ Same modifier, both orientations supported. LAYO-03/04/08 unblocked. Release event NOT consumed → nested SplitPanes work correctly.                                                                                  |
| Phase 10 — AeroStepperWizard     | `AeroStepIndicator(currentStep, totalSteps, onStepClick)`                                        | ✓ Locked signature maps 1-to-1 to wizard step model. LAYO-09 unblocked.                                                                                                                                                |

### Human Verification Required

See `human_verification:` block in frontmatter. To run the visual checkpoint:

```
./gradlew :showcase:run
```

Then scroll to the section titled **"Phase 7 Scratch (TEMPORARY — deleted Phase 11)"** and:

1. **HSV / Hue first-pixel drag** — click and drag inside the 256×256 saturation square; the indicator should follow the mouse from pixel 1. Same for the 24×256 hue strip. The 64×32 color-preview Box on the right should update live.
2. **DragSplitter horizontal + vertical** — drag the 16dp surface-tinted horizontal splitter; `hPos = ...px` should update from the first pixel of motion. Same for the vertical splitter / `vPos`. Cursor should change to E_RESIZE / N_RESIZE on hover.
3. **Step indicator three-theme** — click Prev / Next to advance currentStep through {0, 1, 2, 3}; toggle `ThemeSwitcher` at the top of the showcase between AeroBlue / AeroDark / Classic; in every theme the Current dot, Completed dots (Check icon), and Upcoming dots must be visually distinguishable.
4. **CalendarPositionProvider wide popup** — in the 1024dp scratch frame, click "Open calendar"; the popup must render fully visible (no clipping) and sit BELOW the trigger even though the trigger is at the right edge.

### Gaps Summary

**Re-verification 2026-05-04T18:30:00Z** — UAT surfaced 4 gaps (1 primitive-level layout bug + 3 scratch-demo presentation bugs).

**Architecture decision locked: option B** — primitives stay surface-less; glass is the responsibility of the public consumer (AeroDatePicker / AeroStepperWizard) and, for Phase 7 demo purposes, the scratch wrapper. This preserves clean composition and avoids double-glass when public components are built in Phase 8/10.

| Gap                                              | Severity | Scope         | Target file                                                  |
| ------------------------------------------------ | -------- | ------------- | ------------------------------------------------------------ |
| gap-01 — `AeroCalendarGrid` header stretches past day-grid | blocking | primitive     | `library/.../calendar/AeroCalendarGrid.kt`                   |
| gap-02 — StepIndicator demo lacks glass surface  | ux       | scratch_demo  | `library/.../scratch/AeroPhase7Scratch.kt`                   |
| gap-03 — Calendar popup demo lacks glass surface | ux       | scratch_demo  | `library/.../scratch/AeroPhase7Scratch.kt`                   |
| gap-04 — Raw Button(...) instead of AeroButton + width-stretching | ux | scratch_demo  | `library/.../scratch/AeroPhase7Scratch.kt`                   |

**Passed during UAT (status promoted from human_needed → passed_human_check):**
- HSV square + hue slider first-pixel drag (PITFALL-03)
- AeroDragSplitter first-pixel drag, both orientations (visual no-op of splitter alone is by-design — the modifier is event-only; layout-mover is the consumer)

**Blocked by gaps (cannot be UAT-confirmed until gaps close):**
- AeroStepIndicator three-theme contrast (blocked by gap-02 + gap-04)
- AeroCalendarPositionProvider wide-popup behavior (blocked by gap-03 + gap-04 — position math unit-tested green, only visual confirmation pending)

Gap closure path: `/gsd:plan-phase 7 --gaps` reads this file's `gaps:` block → creates a 7.1 plan(s) with `gap_closure: true` → `/gsd:execute-phase 7.1` runs the fix → re-verification re-runs UAT for the blocked truths.

---

_Verified: 2026-05-04T17:00:00Z_
_Verifier: Claude (gsd-verifier)_
