---
phase: 17-buttons
plan: 03
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, buttons, source-scan-guards, keyboard-a11y]

# Dependency graph
requires:
  - phase: 17-buttons
    plan: 01
    provides: "AeroButtonSurface (shared internal surface), glow-before-surface modifier ordering, Modifier.clickable(role = Role.Button, ...) wiring"
  - phase: 17-buttons
    plan: 02
    provides: "resolveButtonStyle(colors, outlined, hovered, pressed, focused, enabled) pure per-state resolver"
provides:
  - "AeroSurfaceStyle.outlinedStyle() — fixed-delta filled-to-outlined transform (fill alpha x0.15, gloss proportionally scaled to 0.15, rim to 0.85), applied on top of resolveButtonStyle's per-state output"
  - "AeroOutlinedButton as a thin public wrapper delegating to AeroButtonSurface(outlined = true) — zero M3 container, zero bespoke painting"
  - "AeroButtonSemanticsTest.kt — runComposeUiTest Role.Button + Space/Enter keyboard-activation convention for future Aero interactive controls"
  - "AeroButtonSurfaceSourceTest.kt — VBTN-03/VBTN-06 whole-file source-scan regression guard convention (no larger-file functionBody() extraction needed for thin single-declaration wrapper files)"
affects: [17-04, 17-05, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Button-specific style deltas (outlinedStyle) live in components/buttons/AeroButtonSurface.kt, not theme/ — only the cross-phase-reusable pressedRecess transform (17-02) needed theme/ placement"
    - "Source-scan regression guard applied to a whole small wrapper file (AeroButton.kt/AeroOutlinedButton.kt), not a functionBody()-extracted slice — appropriate when the file IS a single declaration, unlike AeroSurfacePrimitives.kt's multi-function file"
    - "Compile-proof spike for new Compose test-API surface done via direct javap bytecode inspection of the real dependency jar, not a throwaway scratch composable — equally valid per the project's own TOOL-07 precedent, faster, and leaves no scratch artifact to clean up"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt

key-decisions:
  - "outlinedStyle() implemented verbatim per 17-RESEARCH.md Pattern 5's recommended formula: fillTop/fillBottom alpha x0.15 (absolute multiplier), glossAlpha scaled proportionally (glossAlpha x (0.15/0.32), so an already-zeroed gloss from pressed/disabled states stays zero rather than jumping back up), rimAlpha set to a literal 0.85 (not proportional) — chosen over a ratio-scaled rimAlpha because it is the exact snippet 17-RESEARCH.md verified as the concrete implementable default, not a guess"
  - "outlinedStyle() lives in AeroButtonSurface.kt (components/buttons/), not theme/AeroSurfaceStyle.kt — unlike 17-02's pressedRecess, this transform is button-family-specific with no cross-phase (Phase 19) reuse need, per the plan's own read_first guidance"
  - "Pitfall 6 compile-proof spike done via direct javap bytecode inspection of ui-test-desktop-1.11.1.jar / ui-desktop-1.11.1.jar (mirrors Phase 15's TOOL-07 precedent for dropShadow/innerShadow) rather than writing a separate throwaway scratch composable — all four symbols (performKeyInput, pressKey, Key.Enter/Key.Spacebar, requestFocus) resolved exactly as assumed, all in androidx.compose.ui.test / androidx.compose.ui.input.key, no package-location surprise this time"
  - "Source-scan guards (VBTN-03/VBTN-06) scan the whole AeroButton.kt/AeroOutlinedButton.kt file text rather than extracting a functionBody() slice — both files are single-declaration thin wrappers, so there is no larger file body to narrow away from (unlike AeroSurfacePrimitives.kt's multi-function file, which the functionBody() convention exists for)"

requirements-completed: [VBTN-03, VBTN-04, VBTN-05, VBTN-06]

coverage:
  - id: D1
    description: "AeroOutlinedButton shows the outlined-variant treatment as a fixed delta of the filled style, provably unable to drift since both consume AeroButtonSurface"
    requirement: "VBTN-05"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt#outlinedEqualsOutlinedStyleAppliedToFilledForEveryState (parametrized over 5 states x 2 color schemes)"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt#restOutlinedMatchesTheUiSpecFixedDeltaRatios"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt#pressedAndDisabledZeroedGlossStaysZeroAfterOutlinedTransform"
        status: pass
    human_judgment: true
    rationale: "Value-level tests prove outlined is algebraically the filled style transformed; the actual visual look (a faint glass fill inside a brighter contour) is deferred to the 17-05 three-theme sign-off gate per the phase's validation architecture, same as 17-01/17-02's D1/D2."
  - id: D2
    description: "Role.Button semantics present and Space/Enter keyboard activation both invoke onClick, for both AeroButton and AeroOutlinedButton, proven by automated test not visual review"
    requirement: "VBTN-04"
    verification:
      - kind: integration
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt (5 tests: role+click-action x2 variants, Space, Enter x2 variants)"
        status: pass
    human_judgment: false
  - id: D3
    description: "No unclipped drawWithContent overlay remains in AeroButton.kt/AeroOutlinedButton.kt — the hover-brighten cue is inside aeroSurface()'s own clip"
    requirement: "VBTN-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt#noUnclippedDrawWithContentOverlayInAeroButton / #noUnclippedDrawWithContentOverlayInAeroOutlinedButton"
        status: pass
    human_judgment: false
    rationale: "Guard proven to FAIL against a deliberately-reintroduced drawWithContent overlay before being trusted — see Guard Fail-Then-Pass Proof below (repro-must-exercise-the-path, VER-06)."
  - id: D4
    description: "Both AeroButton.kt and AeroOutlinedButton.kt call the one shared AeroButtonSurface; neither calls aeroSurface(/drawAeroSurfaceCore( directly — no duplicated painter"
    requirement: "VBTN-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt#aeroButtonCallsTheSharedSurfaceNotADuplicatedPainter / #aeroOutlinedButtonCallsTheSharedSurfaceNotADuplicatedPainter / #aeroOutlinedButtonPassesOutlinedTrueToTheSharedSurface"
        status: pass
    human_judgment: false
    rationale: "Guard proven to FAIL against a deliberately-reintroduced direct aeroSurface( call before being trusted — see Guard Fail-Then-Pass Proof below (repro-must-exercise-the-path, VER-06)."

# Metrics
duration: 15min
completed: 2026-07-23
status: complete
---

# Phase 17 Plan 03: Outlined Variant + Structural/Semantic Locks Summary

**`AeroOutlinedButton` becomes a thin wrapper painting through `AeroButtonSurface`'s fixed-delta `outlinedStyle()` transform, `Role.Button` + Space/Enter keyboard activation is proven automatically, and the no-unclipped-overlay (VBTN-03) / single-shared-surface (VBTN-06) guards are locked after being demonstrated to fail on deliberately-broken code first.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-07-23T16:59:14+03:00 (after 17-02 plan-metadata commit `a90a9a8`)
- **Completed:** 2026-07-23T17:14:15+03:00
- **Tasks:** 2
- **Files modified:** 5 (3 created, 2 modified)

## Accomplishments
- `AeroSurfaceStyle.outlinedStyle()` (new internal extension fn in `AeroButtonSurface.kt`) implements 17-UI-SPEC.md's fixed filled→outlined delta verbatim per 17-RESEARCH.md Pattern 5's recommended formula; `resolveButtonStyle` applies it on top of the already-per-state-resolved filled style when `outlined = true`, so outlined provably inherits every state delta (rest/hover/press/focus/disabled) by construction
- `AeroOutlinedButton.kt` gutted to a thin public wrapper mirroring `AeroButton.kt` exactly: every M3-container artifact removed (`OutlinedButton`, `ButtonDefaults`, `LocalMinimumInteractiveComponentSize`, `CompositionLocalProvider`, `BorderStroke`, `drawWithContent`, `graphicsLayer` scale-shrink, the three separate `rememberHoverState`/`rememberPressedState`/`rememberFocusState` calls) — locked public signature and defaults (`height = 28.dp`, `contentPadding` horizontal `10.dp`/vertical `2.dp`) preserved
- `Role.Button` semantics + Space/Enter keyboard activation proven end-to-end via `runComposeUiTest` for both `AeroButton` and `AeroOutlinedButton` (VBTN-04) — a compile-proof spike (direct `javap` bytecode inspection of the real CMP 1.11.1 desktop test jars, mirroring the project's own TOOL-07 precedent) confirmed `performKeyInput`/`pressKey`/`Key.Enter`/`Key.Spacebar`/`requestFocus()` signatures before the real test was written (Pitfall 6)
- VBTN-03 (no unclipped overlay) and VBTN-06 (single shared surface) source-scan guards added and — per this project's own v2.0.3 false-positive-sign-off lesson — each proven to FAIL against deliberately-reintroduced broken code before being trusted (see below)
- Full `:library:test` suite: 296 tests, 0 failures (no regression to the 232+ baseline)

## Task Commits

Each task was committed atomically:

1. **Task 1: Outlined variant as a fixed delta of the filled style** - `514d56d` (feat)
2. **Task 2: Semantics + keyboard test and the VBTN-03/VBTN-06 source-scan guards** - `0c01eab` (test)

**Plan metadata:** pending (this commit)

## Guard Fail-Then-Pass Proof

Per the plan's `<critical_reminder>` and this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path, VER-06), both new source-scan guards were demonstrated to FAIL against deliberately-broken code, via a temporary local edit to `AeroButton.kt` that was fully reverted (confirmed via `git diff` showing zero changes) before the Task 2 commit.

**VBTN-03 (no-unclipped-overlay guard):**
- Temporarily added `import androidx.compose.ui.draw.drawWithContent` (plus an explanatory comment containing the literal text `drawWithContent`) to `AeroButton.kt`.
- Re-ran `AeroButtonSurfaceSourceTest`: **5 tests completed, 1 failed** — `noUnclippedDrawWithContentOverlayInAeroButton()` failed with `AssertionFailedError: AeroButton.kt must not contain a drawWithContent overlay — the hover-brighten cue must be one of aeroSurface()'s own clipped draws (VBTN-03)`. All 4 other guard tests still passed (isolated to the intended assertion).
- Reverted the edit; re-ran the same test class: 5/5 passed.

**VBTN-06 (single-shared-surface guard):**
- Temporarily added a commented-out `// aeroSurface(` token after the real `AeroButtonSurface(...)` call in `AeroButton.kt` (text-only — never actually composed, since the source-scan test only inspects raw file text, not compiled bytecode).
- Re-ran `AeroButtonSurfaceSourceTest`: **5 tests completed, 1 failed** — `aeroButtonCallsTheSharedSurfaceNotADuplicatedPainter()` failed with `AssertionFailedError: AeroButton.kt must not call aeroSurface(/drawAeroSurfaceCore( directly — that would be a second, duplicated painter (VBTN-06)`. All 4 other guard tests still passed.
- Reverted the edit; re-ran the same test class: 5/5 passed.

Both proofs used the exact same guard test file that ships in this commit — no guard code was authored differently between the fail-demonstration and the final commit.

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - Added `outlinedStyle()` extension fn + 4 tuning constants; `resolveButtonStyle` applies it on top of the per-state filled resolution when `outlined = true`
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt` - Rewritten as a thin wrapper delegating to `AeroButtonSurface(..., outlined = true)`, mirroring `AeroButton.kt`
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt` - New JVM value-level parametrized test (5 states x 2 color schemes) proving `outlined == outlinedStyle(filled)`, plus UI-SPEC ratio and zeroed-gloss sanity checks
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` - New `runComposeUiTest` suite (5 tests): Role.Button + click-action for both variants, Space/Enter keyboard activation for both variants after `requestFocus()`
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` - New source-scan regression guard file (5 tests): VBTN-03 no-unclipped-overlay x2 files, VBTN-06 shared-surface x2 files + outlined-param check

## Decisions Made
- `outlinedStyle()` follows 17-RESEARCH.md Pattern 5's exact recommended formula (fill alpha x0.15 absolute multiplier; glossAlpha proportionally scaled by 0.15/0.32 so a zeroed gloss from pressed/disabled stays zero; rimAlpha set to a literal 0.85 rather than proportionally scaled) — the literal snippet is what 17-RESEARCH.md itself frames as the checker-verified concrete implementable default, so it was used verbatim rather than re-derived
- `outlinedStyle()` placed in `components/buttons/AeroButtonSurface.kt`, not `theme/AeroSurfaceStyle.kt` — unlike 17-02's `pressedRecess`, this transform has no Phase 19 cross-package reuse need (Phase 19's `AeroSegmentedControl` reuses `pressedRecess` for its recessed segment, not an outlined-button concept)
- Compile-proof spike for Pitfall 6 done via direct `javap` bytecode inspection of the real `ui-test-desktop-1.11.1.jar`/`ui-desktop-1.11.1.jar` dependency jars (found via `~/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2/bin/javap.exe`, since `javap` was not on `PATH`) rather than a separate throwaway scratch composable — confirmed `performKeyInput`(`ActionsKt`)/`pressKey`(`KeyInjectionScopeKt`)/`Key.Enter`/`Key.Spacebar`(`Key.Companion`)/`requestFocus`(`ActionsKt`) all resolve in `androidx.compose.ui.test`/`androidx.compose.ui.input.key` exactly as RESEARCH.md assumed — no package-location surprise this time (unlike Phase 15's TOOL-07 dropShadow/innerShadow finding)
- `AeroOutlinedButtonStylesTest`'s float-alpha assertions use a `1/255` tolerance (not a tight epsilon) after discovering `Color.copy(alpha = ...)` quantizes through its internal packed representation (e.g. `0.15f` round-trips as `0.14901961f`) — a precision artifact of `Color`'s storage, not a transform-logic bug

## Deviations from Plan

**None — plan executed as written**, with one small auto-fix surfaced during test-writing (Rule 1 — test bug, not implementation bug):

### Auto-fixed Issues

**1. [Rule 1 - Test bug] Float-alpha assertion tolerance too tight for `Color`'s internal quantization**
- **Found during:** Task 1 verification (`AeroOutlinedButtonStylesTest`)
- **Issue:** `restOutlinedMatchesTheUiSpecFixedDeltaRatios` asserted `fillTop.alpha`/`fillBottom.alpha` with a `0.0001f` tolerance; failed with `Expected <0.15> ... actual <0.14901961>` — `Color.copy(alpha = ...)` quantizes alpha through its internal packed representation, not a bug in `outlinedStyle()`'s multiplication itself
- **Fix:** Widened the tolerance to `1f / 255f` (the actual quantization step) for the two fill-alpha assertions only; the `glossAlpha`/`rimAlpha` assertions (plain `Float` fields, not routed through `Color`) kept their tight `0.0001f` tolerance
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt`
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.components.buttons.AeroOutlinedButtonStylesTest"` — 3/3 passed
- **Committed in:** `514d56d` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1, test-only, isolated to one assertion tolerance)
**Impact on plan:** No change to production code, architecture, or the plan's intended design — a test-precision correction only.

## Issues Encountered
- Same stale-IDE-diagnostic pattern as 17-01/17-02 (JVM target 1.8 vs 11 inline-bytecode errors, Kotlin metadata 2.4.0 vs compiler-readable 2.2.0) surfaced after each Edit/Write call. Confirmed non-issue again: every `./gradlew :library:test` invocation (scoped and full-suite) compiled and passed cleanly throughout, including during the two temporary breaking edits used for the guard fail-proof.
- The acceptance criteria's literal grep `grep -E "OutlinedButton|BorderStroke|drawWithContent|graphicsLayer" AeroOutlinedButton.kt` necessarily matches the file's own declaration (`public fun AeroOutlinedButton(`) and KDoc prose mentioning the component's own name — this is an inherent artifact of the grep pattern (no word-boundary/package qualifier), not a real M3 leftover. Verified precisely instead via `grep -n "^import"` (only 7 non-M3 imports remain) and a scoped `material3\.OutlinedButton|BorderStroke|drawWithContent|graphicsLayer` grep (zero matches).
- `javap` was not on `PATH` in this environment; located it under the Gradle-managed JDK toolchain (`~/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2/bin/javap.exe`) rather than assuming a system JDK.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Both `AeroButton` and `AeroOutlinedButton` now paint exclusively through `AeroButtonSurface`, with `resolveButtonStyle` + `outlinedStyle()` as the complete pure-function style-resolution pipeline for all five states x two variants — 17-04/17-05 can proceed directly to the showcase state-matrix + three-theme sign-off without further structural changes
- The VBTN-03/VBTN-06 source-scan guards are now permanent regression protection — any future edit that reintroduces a bespoke overlay or a second painter in either button file will be caught by `AeroButtonSurfaceSourceTest` before it reaches a visual review
- No blockers. Full `:library:test` suite green (296 tests) after this plan's changes.

---
*Phase: 17-buttons*
*Completed: 2026-07-23*

## Self-Check: PASSED

All created/modified files and both task commits (`514d56d`, `0c01eab`) verified present on disk / in git history (see below).
