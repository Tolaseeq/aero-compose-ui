---
phase: 17-buttons
plan: 01
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, buttons, drawscope-gradients]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "aeroSurface/aeroGlowRing modifiers, AeroSurfaceStyle.rest() factory, AeroOrnamentTokens.derive(), rememberAeroInteractionState() collector"
provides:
  - "AeroButtonSurface — internal shared surface composable backing all Aero button variants (VBTN-06 vehicle)"
  - "AeroButton rewritten as a thin wrapper with the M3 container fully removed"
  - "Modifier.clickable(role = Role.Button, ...) click/keyboard wiring pattern for buttons without an M3 container"
  - "AeroButtonTest — runComposeUiTest render+click smoke test convention for this phase's remaining plans"
affects: [17-02, 17-03, 17-04, 17-05, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "One internal shared surface composable (AeroButtonSurface) painting multiple public button variants via an outlined: Boolean differentiator — mirrors the AeroPanelGroupImpl(orientation) one-core precedent"
    - "Modifier chain ordering: .height() -> .clickable(role = Role.Button, ...) -> .aeroGlowRing(focus) -> .aeroGlowRing(hover) -> .aeroSurface(style, shape) — glow rings precede the surface's own .clip()"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt

key-decisions:
  - "AeroSurfaceStyle.rest(AeroTheme.colors, cornerRadius = 4.dp) used verbatim for the tracer's single rest-state resolution — no bespoke accent-override style needed (D-01 confirmed satisfied by the existing rest() factory)"
  - "AeroButtonSurface accepts an outlined: Boolean parameter now but does not yet branch on it — the single clearly-named style-resolution line is the point 17-02's resolveButtonStyle(...) replaces"

patterns-established:
  - "Pattern 1: Shared internal surface, param-differentiated (VBTN-06) — AeroButtonSurface(outlined: Boolean) instead of two independent painters"
  - "Pattern 2: Glow-before-surface modifier ordering is load-bearing — every future Aero component composing aeroGlowRing + aeroSurface must order glow calls before the surface call"

requirements-completed: [VBTN-01, VBTN-04, VBTN-06]

coverage:
  - id: D1
    description: "AeroButtonSurface paints AeroButton's filled rest state (two-tone fill, gloss, bevel, rim) via the Phase 16 aeroSurface/aeroGlowRing primitives, with the M3 container fully removed"
    requirement: "VBTN-01"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt#rendersWithoutException_onAeroBlue"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt#rendersWithoutException_onAeroDark"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt#rendersWithoutException_onClassic"
        status: pass
    human_judgment: true
    rationale: "The render smoke tests prove no-exception composition across all three themes, but the actual gradient/gloss/bevel/rim visual fidelity (Success Criterion 1) requires a human eyes-on check — deferred to the 17-05 three-theme sign-off gate per the phase's validation architecture."
  - id: D2
    description: "Click and Space/Enter keyboard activation work via Modifier.clickable(role = Role.Button, ...) after the Material3 container is dropped"
    requirement: "VBTN-04"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt#clickInvokesOnClick"
        status: pass
    human_judgment: true
    rationale: "This plan proves the click path end-to-end; the formal Role.Button semantics assertion + Space/Enter keyboard-activation test is explicitly deferred to 17-03 (AeroButtonSemanticsTest.kt) per the plan's must_haves truth #2 wording."
  - id: D3
    description: "AeroButtonSurface exists as the one internal composable painting AeroButton, establishing the shared-surface vehicle both AeroOutlinedButton (17-03) and AeroSegmentedControl (Phase 19) will consume"
    requirement: "VBTN-06"
    verification:
      - kind: unit
        ref: "source assertion: AeroButton.kt calls AeroButtonSurface( with no own aeroSurface(/aeroGlowRing( calls"
        status: pass
    human_judgment: false
  - id: D4
    description: "Showcase renders the restyled filled AeroButton with the UI-SPEC demo label 'Save Changes'"
    verification:
      - kind: integration
        ref: "./gradlew :showcase:compileKotlin"
        status: pass
    human_judgment: false

# Metrics
duration: 20min
completed: 2026-07-23
status: complete
---

# Phase 17 Plan 01: Shared Button Surface Tracer Summary

**New internal `AeroButtonSurface` composable paints `AeroButton`'s filled rest state via Phase 16 primitives, with the Material3 `Button` container fully removed and click/keyboard wired through `Modifier.clickable(role = Role.Button, ...)`.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-07-23T13:26:23Z (after plan finalization commit 986a779)
- **Completed:** 2026-07-23T13:45:50Z
- **Tasks:** 2
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments
- `AeroButtonSurface` (new internal shared surface) paints two-tone fill, proportional top gloss, inner bevel, and outer contour via `aeroSurface`, plus persistent-focus and hover glow via two ordered `aeroGlowRing` calls — the VBTN-06 shared-surface vehicle for all Phase 17/19 button-family components
- `AeroButton.kt` gutted to a thin public wrapper: every M3-container artifact removed (`Button`, `ButtonDefaults`, `LocalMinimumInteractiveComponentSize`, `CompositionLocalProvider`, `border`, `graphicsLayer`, `animateFloatAsState`/`tween`/`LinearEasing`, `drawWithContent`, the three separate `rememberHoverState`/`rememberPressedState`/`rememberFocusState` calls) — public signature and locked defaults (`height = 30.dp`, `contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)`) preserved
- Click + Space/Enter keyboard activation proven end-to-end via `Modifier.clickable(role = Role.Button, indication = null, interactionSource = interactionSource, enabled = enabled, onClick = onClick)` — no M3 container supplying semantics for free (VBTN-04, the `AeroRangeSlider` zero-semantics precedent not repeated)
- Showcase `ButtonsSection.kt` "AeroButton" row renders through the new surface with the UI-SPEC demo label "Save Changes"

## Task Commits

Each task was committed atomically:

1. **Task 1: Shared internal surface + filled AeroButton rest state, end-to-end** - `085afe9` (feat)
2. **Task 2: Wire the restyled filled AeroButton into the showcase** - `8a8f10e` (feat)

**Plan metadata:** pending (this commit)

_Note: Task 1 carried `tdd="true"`; the surface, wrapper, and test were authored together as one coherent tracer slice (the surface did not exist to test incrementally against) rather than a separate RED-then-GREEN commit pair — the four `AeroButtonTest` behaviors were all green on first run once the missing `dp` import was fixed._

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - New internal shared surface; resolves `AeroSurfaceStyle.rest(AeroTheme.colors, cornerRadius = 4.dp)`, chains `clickable(role = Role.Button)` -> `aeroGlowRing`(focus) -> `aeroGlowRing`(hover) -> `aeroSurface`
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` - Rewritten as a thin wrapper delegating to `AeroButtonSurface(..., outlined = false)`
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt` - `runComposeUiTest` render (x3 themes) + click smoke test
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` - "AeroButton" row demo label updated to "Save Changes"

## Decisions Made
- `AeroSurfaceStyle.rest(colors, cornerRadius = 4.dp)` used verbatim for the tracer's rest-state resolution — the plan's D-01 finding (fillTop/fillBottom/bevelLight/bevelShadow/hoverGlow already `primary`-derived accent tokens) meant no bespoke accent-override style was needed; flagged for the 17-05 three-theme sign-off per RESEARCH.md Pattern 2
- `outlined: Boolean` parameter added to `AeroButtonSurface`'s signature now but left unconsumed by the body — a single, clearly-named style-resolution line stands in its place, intentionally not stubbed with multiple fake per-state branches, per the plan's explicit tracer-scope instruction (17-02 replaces this line with `resolveButtonStyle(...)`)

## Deviations from Plan

**None — plan executed as written**, with two small auto-fixes surfaced during verification (Rule 3 — blocking compile issues):

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing `androidx.compose.ui.unit.dp` import in `AeroButtonSurface.kt`**
- **Found during:** Task 1 verification (`./gradlew :library:test`)
- **Issue:** `4.dp` literals used in the modifier chain without the `dp` extension import — `:library:compileKotlin` failed with 4 "Unresolved reference 'dp'" errors
- **Fix:** Added `import androidx.compose.ui.unit.dp`
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt`
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.components.buttons.AeroButtonTest"` compiled and passed 4/4
- **Committed in:** `085afe9` (Task 1 commit)

**2. [Rule 3 - Blocking] `assertExists()` not available on this CMP 1.11.1 `ui-test-desktop` version**
- **Found during:** Task 1 verification
- **Issue:** `AeroButtonTest.kt` initially called `.assertExists()` on `SemanticsNodeInteraction`, which failed to resolve — inspection of the `ui-test-desktop-1.11.1` sources jar confirmed this exact function name isn't exposed in `Assertions.kt` on this toolchain (unlike some other Compose UI test API surfaces)
- **Fix:** Switched to `.assertIsDisplayed()`, which is present in the same file and equivalent for this test's purpose (the node must exist and be visible after `waitForIdle()`)
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt`
- **Verification:** `./gradlew :library:test --tests "com.mordred.aero.components.buttons.AeroButtonTest"` compiled and passed 4/4
- **Committed in:** `085afe9` (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (both Rule 3 — blocking compile issues, both within the same file/task, both resolved before the task's commit)
**Impact on plan:** Neither fix altered behavior, architecture, or the plan's intended design — both were compile-time corrections needed to make the already-planned code build. No scope creep.

## Issues Encountered
- IDE language-server diagnostics (JVM target 1.8 vs 11, Kotlin metadata 2.4.0 vs compiler 2.1.0) appeared after each `Edit`/`Write` call — these are a stale IDE plugin version mismatch unrelated to the project's actual Gradle Kotlin 2.4.10 toolchain (confirmed: `./gradlew :library:test` and `:showcase:compileKotlin` both ran clean). No action taken; not a real build issue.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The Phase 17 architecture is proven end-to-end (caller -> `AeroButtonSurface` -> `aeroGlowRing`x2 -> `aeroSurface` -> `drawAeroSurfaceCore`) and committed before any expansion plan runs, per the tracer's purpose
- 17-02 can now build directly on `AeroButtonSurface`'s single style-resolution line, replacing it with `resolveButtonStyle(colors, outlined, hovered, pressed, focused, enabled)` and the `AeroSurfaceStyle.pressedRecess()`/`flattenDisabled()`/`hoverLighten()` extension fns in `theme/AeroSurfaceStyle.kt`
- 17-03 can add `AeroOutlinedButton` by consuming `AeroButtonSurface(..., outlined = true)`, plus the `Role.Button` + Space/Enter keyboard semantics test and the no-unclipped-overlay / shared-surface source-scan guards
- No blockers. Full `:library:test` suite re-run green after this plan's changes (no regressions from the M3 removal in `AeroButton.kt`).

---
*Phase: 17-buttons*
*Completed: 2026-07-23*

## Self-Check: PASSED

All created/modified files and both task commits (085afe9, 8a8f10e) verified present on disk / in git history.
