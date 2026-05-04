---
phase: 07-shared-internal-primitives
plan: 02
subsystem: ui
tags: [drag, hsv-canvas, hue-slider, step-indicator, scratch-section, pitfall-03]

# Dependency graph
requires:
  - phase: 07-shared-internal-primitives (plan 01)
    provides: AeroCalendarGrid + AeroCalendarPositionProvider + AeroColorMath (consumed by Phase7ScratchSection demos)
  - phase: 04-icon-foundation
    provides: AeroIcons.Check (rendered by AeroStepIndicator's Completed state)
  - phase: 01-foundation
    provides: AeroTheme.colors / AeroTheme.typography (read inside all 4 internal composables)
provides:
  - "Modifier.aeroDragSplitter — locked v2.0 drag utility (awaitPointerEventScope manual loop, cursor change, 1D delta) consumed by AeroSplitPane (Phase 10) and AeroDataTable column-resize (Phase 9)"
  - "AeroHsvColorSquare + AeroHueSlider — Canvas-based HSV picker primitives consumed by AeroColorPicker (Phase 8)"
  - "AeroStepIndicator — visual contract consumed by AeroStepperWizard (Phase 10)"
  - "AeroPhase7Scratch (public, deleted Phase 11) — eyes-on confirmation surface for all 6 Phase 7 primitives in :showcase"
affects: [phase-08-pickers, phase-09-data, phase-10-layout, phase-11-showcase]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PITFALL-03 mitigation locked: awaitPointerEventScope + manual loop — detectDragGestures BANNED for in-content Canvas drag"
    - "Cursor change keyed to Orientation (E_RESIZE_CURSOR / N_RESIZE_CURSOR) via remember(orientation)"
    - "change.consume() only on actual drag delta, never on release event (so parents see release)"
    - "Click-to-set UX on HSV/hue (callback fires on first mouse-down, not after slop)"
    - "HSV is degrees [0f, 360f] (Plan-01 ADR carried forward into Color.hsv calls inside primitives)"
    - "Single public aggregator pattern for showcase access to internal symbols (AeroPhase7Scratch in :library, thin wrapper in :showcase)"

key-files:
  created:
    - "library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt"
    - "library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt"
    - "library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt"
    - "showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt"
  modified:
    - "showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt — added Phase7ScratchSection import + call after NavigationSection()"
    - ".gitignore — added .kotlin/ Kotlin 2.x compile cache directory"

key-decisions:
  - "Phase7ScratchSection demo logic lives in :library (AeroPhase7Scratch.kt, public) NOT :showcase — Kotlin's `internal` is module-scoped and the locked Plan-01 ADR keeps every Phase 7 primitive `internal`. Showcase consumes via thin one-line wrapper. Phase 11 cleanup is two file deletions + one ShowcaseApp.kt edit."
  - "Plan typography.body / typography.caption substituted with bodyMedium / label (the actual AeroTypography fields) — Rule 3 deviation, identical to Plan-01"
  - "AeroDragSplitter inner-loop release does NOT call change.consume() — parents (e.g. nested SplitPanes) must see the pointer release event"
  - "Hue slider remember(Unit) for the gradient brush since the Brush.verticalGradient inputs are constants; pointerInput keyed on Unit since the loop reads Box.size each pass"

patterns-established:
  - "Single public aggregator inside :library exposes internal demos to a sibling module without leaking primitives (AeroPhase7Scratch -> Phase7ScratchSection wrapper)"
  - "awaitFirstDown must be explicitly imported from androidx.compose.foundation.gestures (pointerInput scope does not auto-import it)"
  - "AeroIcons icon properties (e.g. AeroIcons.Check) must be imported via the backtick-escaped `internal` package: import com.mordred.aero.icons.`internal`.Check"

requirements-completed: []

# Metrics
duration: ~10m 33s
completed: 2026-05-04
---

# Phase 7 Plan 2: Visuals and Scratch Summary

**Modifier.aeroDragSplitter, AeroHsvColorSquare, AeroHueSlider, AeroStepIndicator landed and wired into the showcase via AeroPhase7Scratch — all four use awaitPointerEventScope (PITFALL-03 mitigation locked), all stay `internal`, and the showcase eyes-on surface exercises every Phase 7 primitive across all three themes.**

## Performance

- **Duration:** ~10m 33s (executor wall clock)
- **Started:** 2026-05-04T16:11:19Z
- **Completed:** 2026-05-04T16:21:52Z
- **Tasks:** 5 (4 implementation + 1 automated sign-off gate)
- **Files created:** 6 (4 library primitives + 1 library scratch aggregator + 1 showcase wrapper)
- **Files modified:** 2 (ShowcaseApp.kt wiring + .gitignore hygiene)

## Accomplishments

- `Modifier.aeroDragSplitter` — locked v2.0 drag pattern (awaitPointerEventScope + manual loop) shipped in `library/.../components/internal/drag/`. Cursor change keyed to Orientation (E_RESIZE / N_RESIZE) via remember; `change.consume()` only fires on actual drag delta; release event left unconsumed so parent containers see it. PITFALL-03 (touchSlop=18dp silent failure) defused at the shared-utility level.
- `AeroHsvColorSquare` — 256x256 Canvas with white→pure-hue horizontal gradient + transparent→black vertical overlay; 8dp double-stroke indicator. First mouse-down fires `onSatValChange` (click-to-set UX) — no slop wait. PITFALL-03-free.
- `AeroHueSlider` — 24x256 vertical Canvas with 7-stop R→Y→G→C→B→M→R gradient; 2dp horizontal indicator at `(hue / 360f) * size.height` in `colors.borderSelected` for cross-theme contrast. Click-to-set + drag-to-update via awaitPointerEventScope.
- `AeroStepIndicator` — horizontal step indicator with 3 visual states (Current = filled `colors.primary`; Completed = primary 0.6 alpha + `AeroIcons.Check` 12dp; Upcoming = outlined 1dp `colors.labelText` for AeroDark contrast) and 2dp connector lines (primary on completed-side, borderDefault on upcoming-side). `onStepClick = null` default keeps it non-interactive for AeroStepperWizard's Phase 10 use.
- `AeroPhase7Scratch` (public, deleted Phase 11) — single library composable hosts 5 demos exercising all 6 Phase 7 primitives: AeroCalendarGrid (with prev/next + selected-date readout), HSV square + hue slider + 64x32 preview Box, horizontal + vertical drag splitter with px counters, 4-step indicator with Prev/Next buttons, and a 1024dp-wide frame with right-edge button + AeroCalendarPositionProvider Popup verifying no clip.
- `Phase7ScratchSection` (showcase) — thin wrapper that delegates to `AeroPhase7Scratch()`. Wired into `ShowcaseApp.kt` after `NavigationSection()` with import added alphabetically.
- Plan-01 test suite stays green (`./gradlew :library:test` exits 0 — 27 tests across 3 classes still passing). Both `:library:compileKotlin` and `:showcase:compileKotlin` exit 0.

## Task Commits

Each task committed atomically. (Plan declared `tdd="true"` on each task but the `<action>` blocks are direct file creation followed by `compileKotlin`; no failing-test cycle was specified, so single-commit-per-task pattern was used.)

1. **Task 1: Modifier.aeroDragSplitter** — `b7b160d` (feat)
2. **Task 2: AeroHsvColorSquare + AeroHueSlider** — `aedbb57` (feat)
3. **Task 3: AeroStepIndicator** — `1a67347` (feat)
4. **Task 4: AeroPhase7Scratch + Phase7ScratchSection wrapper + ShowcaseApp wiring** — `149c6b5` (feat)
5. **Task 5: automated sign-off gate** — verification only; surfaced `.gitignore` hygiene captured as `7ead484` (chore)

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/internal/drag/AeroDragSplitter.kt` — Modifier.aeroDragSplitter (locked drag pattern)
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHsvColorSquare.kt` — 256x256 HSV picker Canvas
- `library/src/main/kotlin/com/mordred/aero/components/pickers/internal/color/AeroHueSlider.kt` — 24x256 hue strip Canvas
- `library/src/main/kotlin/com/mordred/aero/components/layout/internal/stepper/AeroStepIndicator.kt` — horizontal step indicator
- `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` — public scratch aggregator (deleted Phase 11)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt` — thin wrapper that calls AeroPhase7Scratch()
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — import + Phase7ScratchSection() call after NavigationSection()
- `.gitignore` — added `.kotlin/` (Kotlin 2.x incremental-compile cache directory)

## Decisions Made

- **Scratch aggregator lives in :library, not :showcase.** Kotlin's `internal` modifier is gradle-module-scoped, so a sibling module cannot import internal symbols. The plan as written wanted both (a) all Phase 7 primitives `internal` (Plan-01 ADR + 07-CONTEXT.md §carry-forward rules) and (b) the demo aggregator in `:showcase`. To honor both contracts, the demo body lives in `library/.../scratch/AeroPhase7Scratch.kt` (a single `public` composable, deleted Phase 11) and the showcase file at the locked path is a one-liner wrapper. Locked-internal API surface stays intact; Phase 11 cleanup is still small (two file deletions + one ShowcaseApp.kt edit). Alternative considered: cross-module `-Xfriend-paths=` Gradle config — rejected as fragile across CMP version bumps.
- **Plan typography.body / typography.caption substituted with bodyMedium / label** (matches Plan-01 precedent; those fields do not exist on `AeroTypography`).
- **`change.consume()` only on actual drag delta in aeroDragSplitter, never on release.** Per the plan's locked behavior. Lets nested SplitPane parents see pointer-release events.
- **`awaitFirstDown` requires explicit import.** The plan source noted "no extra import needed beyond `pointerInput`" — that's incorrect for `awaitFirstDown`, which lives in `androidx.compose.foundation.gestures`. Corrected at compile time (Rule 3 blocking fix in Task 1).
- **`AeroIcons.Check` import path is `com.mordred.aero.icons.\`internal\`.Check`** (backtick-escaped `internal` package). Plan source listed `import com.mordred.aero.icons.AeroIcons` + `AeroIcons.Check` — the property extension is defined in the `internal` subpackage, so a separate explicit import is required (matches existing precedent in `AeroCheckbox.kt`). Corrected at compile time (Rule 3 blocking fix in Task 3).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] Added `import androidx.compose.foundation.gestures.awaitFirstDown` to AeroDragSplitter.kt**
- **Found during:** Task 1 (first compile)
- **Issue:** Plan source claimed `awaitFirstDown` is provided by `pointerInput` block scope without an explicit import. Compile failed with `Unresolved reference 'awaitFirstDown'`.
- **Fix:** Added the explicit import. Same pattern needed for `AeroHsvColorSquare.kt` (Task 2), preemptively included.
- **Files modified:** `library/.../internal/drag/AeroDragSplitter.kt`, and applied preemptively to `AeroHsvColorSquare.kt` (Task 2)
- **Verification:** `./gradlew :library:compileKotlin` exits 0 after the fix.
- **Committed in:** `b7b160d` (Task 1)

**2. [Rule 3 — Blocking] Corrected AeroIcons.Check import to use the `internal` subpackage**
- **Found during:** Task 3 (first compile)
- **Issue:** Plan source listed `import com.mordred.aero.icons.AeroIcons` + `AeroIcons.Check` — but `Check` is an extension property defined in `com.mordred.aero.icons.\`internal\`.Check` (the icon files live in the backtick-escaped `internal` subpackage). Compile failed with `Unresolved reference 'Check'`.
- **Fix:** Replaced `import com.mordred.aero.icons.Check` with `import com.mordred.aero.icons.\`internal\`.Check` (matches the existing `AeroCheckbox.kt` precedent).
- **Files modified:** `library/.../layout/internal/stepper/AeroStepIndicator.kt`
- **Verification:** `./gradlew :library:compileKotlin` exits 0 after the fix.
- **Committed in:** `1a67347` (Task 3)

**3. [Rule 3 — Blocking] Substituted typography.bodyMedium / typography.label for plan's typography.body / typography.caption**
- **Found during:** Task 3 + Task 4
- **Issue:** Plan source listings reference `typography.body` and `typography.caption`, but `AeroTypography` exposes `title`, `bodyLarge`, `bodyMedium`, `bodySmall`, `label` (no `body` or `caption`). Identical issue to Plan-01.
- **Fix:** Used `bodyMedium` for primary body text and `label` for caption-style text. Applied in `AeroStepIndicator.kt` (3 sites) and `AeroPhase7Scratch.kt` (10+ sites).
- **Files modified:** `library/.../layout/internal/stepper/AeroStepIndicator.kt`, `library/.../scratch/AeroPhase7Scratch.kt`
- **Verification:** Both modules compile clean; no warnings.
- **Committed in:** `1a67347` (Task 3) and `149c6b5` (Task 4)

**4. [Rule 3 — Blocking] Relocated scratch demo body from :showcase to :library**
- **Found during:** Task 4 (first cross-module compile)
- **Issue:** Plan source designed `Phase7ScratchSection` to live in `:showcase` and directly invoke `AeroCalendarGrid`, `AeroCalendarPositionProvider`, `aeroDragSplitter`, `AeroStepIndicator`, `AeroHsvColorSquare`, `AeroHueSlider` — all `internal` per Plan-01 ADR + 07-CONTEXT.md §carry-forward rules ("explicitApi() enforced on :library — every primitive must be `internal`"). Kotlin's `internal` is gradle-module-scoped, so the showcase compile failed with `Cannot access ... it is internal in file` for every primitive. Additionally, `kotlinx.datetime.LocalDate` was unresolved because the showcase classpath has no `kotlinx-datetime` dependency.
- **Fix:** Created `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt` (single `public` composable, deleted Phase 11) containing the entire 5-demo body. The showcase file at the locked path (`Phase7ScratchSection.kt`) is now a one-liner wrapper that delegates: `@Composable public fun Phase7ScratchSection() { AeroPhase7Scratch() }`. This preserves every locked contract: (a) all 6 primitives stay `internal`; (b) explicitApi() approves the new `public` aggregator; (c) the showcase file path stays at the locked location for plan acceptance criteria; (d) Phase 11 cleanup is still small — two file deletions + one ShowcaseApp.kt edit.
- **Files modified:** new `library/.../scratch/AeroPhase7Scratch.kt`; rewritten `showcase/.../sections/Phase7ScratchSection.kt`
- **Verification:** `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0; `:library:test` exits 0.
- **Alternatives considered:**
  - Cross-module `-Xfriend-paths=` Gradle config — rejected as fragile across CMP version bumps and adds non-trivial build config without precedent in the project.
  - Promoting all 6 primitives to `public` — rejected because it directly violates the locked Plan-01 ADR + ROADMAP §Phase 7 success criterion (zero public symbols).
- **Committed in:** `149c6b5` (Task 4)

**5. [Rule 1 — Bug] Removed dead `Spacer` reference in plan's AeroStepIndicator listing**
- **Found during:** Task 3 (Write step)
- **Issue:** Plan source ended `StepDot` with `@Suppress("UNUSED_EXPRESSION") Spacer` — a bare class-name reference with no invocation. This was apparently intended as a no-op but does not compile (it's an unresolved reference as written, since `Spacer` is a `@Composable fun`, not a class).
- **Fix:** Removed the dead reference and the now-unused `Spacer` import. The function reads cleanly without it.
- **Files modified:** `library/.../layout/internal/stepper/AeroStepIndicator.kt`
- **Verification:** `./gradlew :library:compileKotlin` exits 0; no warnings.
- **Committed in:** `1a67347` (Task 3)

### Note on PITFALL-03 grep gate semantics

The plan's Task 5 grep gate `! grep -r "detectDragGestures" library/.../internal/drag/ library/.../pickers/internal/color/` expects zero matches anywhere. Three matches exist — all in KDoc comments that document the ban itself ("`detectDragGestures` is BANNED for all Canvas drag in v2.0"). The semantic intent of the gate is "no actual `detectDragGestures` callsite." Verified separately: a more precise grep `detectDragGestures\\s*\\(` returns zero matches in those directories. The locked KDoc text is required by 07-CONTEXT.md to document the banned API for future readers; a literal-grep gate cannot distinguish KDoc from callsite without a more precise pattern. Treating this as documentation-only mention; semantic gate passes.

---

**Total deviations:** 5 auto-fixed (4 blocking — all stemming from plan-source typos/oversights; 1 bug cleanup — dead Spacer reference). 1 grep-gate semantic note documented but no source change required.
**Impact on plan:** Zero scope change. All locked contracts preserved (PITFALL-03 mitigation, internal-only primitives, click-to-set UX, cursor change, change.consume() semantics, 1024dp scratch frame, three-theme verification). Phase 11 cleanup remains a small mechanical task.

## Issues Encountered

- The pre-existing `AeroToastHostState` warning (non-public primary constructor exposed via generated `copy()` method) appeared again in compile logs. Out of scope for plan-02 (predates v2.0). Logged in Plan-01 SUMMARY; no new action.

## User Setup Required

None — no external configuration. Manual eyes-on verification of SC3, SC4, SC5 is documented in the plan's `<post_execution_manual_check>` and is a recommended (non-blocking) check before `/gsd:verify-work`. Run via:

```bash
./gradlew :showcase:run
```

Scroll to the section titled "Phase 7 Scratch (TEMPORARY — deleted Phase 11)" and toggle the ThemeSwitcher across AeroBlue / AeroDark / Classic.

## Manual Checkpoint Outcome

Manual SC3/SC4/SC5 checks (HSV/hue first-pixel drag, drag splitter first-pixel drag horizontal+vertical, step indicator three-theme contrast) are user-driven via `:showcase:run` and were NOT performed by the executor (this plan is `autonomous: true`; no UAT checkpoint blocks the plan). Status: **automated gates green; user-driven eyes-on pending as a recommended pre-`/gsd:verify-work` step.**

## Next Phase Readiness

- **Phase 8 ColorPicker** can directly import `AeroHsvColorSquare`, `AeroHueSlider`, and `AeroColorMath` (Plan-01) — all `internal` and accessible inside the same `:library` module.
- **Phase 8 Date/Time pickers** can directly import `AeroCalendarGrid` and `AeroCalendarPositionProvider` (Plan-01).
- **Phase 9 DataTable column-resize** can `Modifier.aeroDragSplitter(orientation = Horizontal, ...)` for the column divider — the shared utility's `enabled` gate naturally maps to "resize enabled at min/max bounds".
- **Phase 10 SplitPane** can use the same Modifier (one consumer, one orientation per pane).
- **Phase 10 AeroStepperWizard** can directly import `AeroStepIndicator` — the locked signature (`currentStep: Int`, `totalSteps: Int`, `onStepClick: ((Int) -> Unit)? = null`) maps 1-to-1 to the wizard's step model.
- **Phase 11 cleanup** is now three steps (one more than Plan-01 anticipated):
  1. Delete `library/src/main/kotlin/com/mordred/aero/scratch/AeroPhase7Scratch.kt`
  2. Delete `showcase/src/main/kotlin/com/mordred/showcase/sections/Phase7ScratchSection.kt`
  3. Remove the `Phase7ScratchSection()` call + import from `ShowcaseApp.kt`

## Self-Check: PASSED

Verification confirmed against acceptance criteria:
- All 6 created files exist at their committed paths (4 library primitives + library scratch aggregator + showcase wrapper).
- All commit hashes resolve via `git log --oneline`: `b7b160d`, `aedbb57`, `1a67347`, `149c6b5`, `7ead484`.
- `./gradlew :library:test` exits 0 (Plan-01's 27 tests still green).
- `./gradlew :library:compileKotlin :showcase:compileKotlin` exits 0.
- No `^public ` symbols in the 4 internal primitive files (`grep -E "^public " ...` returns no matches).
- W11-01 grep gate passes (no `transparent=true` / `undecorated=true` in Phase 7 sources).
- PITFALL-03 grep gate semantically passes (only KDoc references — no callsite usage).
- All 6 Phase 7 primitives are visible to `AeroPhase7Scratch` (same module) and the showcase calls the public wrapper.

---
*Phase: 07-shared-internal-primitives*
*Completed: 2026-05-04*
