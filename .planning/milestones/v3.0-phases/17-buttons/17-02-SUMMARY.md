---
phase: 17-buttons
plan: 02
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, buttons, pure-fn-style-resolution]

# Dependency graph
requires:
  - phase: 17-buttons
    plan: 01
    provides: "AeroButtonSurface (shared internal surface), AeroButtonTest render/click smoke suite, glow-before-surface modifier ordering"
provides:
  - "AeroSurfaceStyle.pressedRecess()/flattenDisabled()/hoverLighten() — pure, Compose-free state transforms in theme/AeroSurfaceStyle.kt, importable cross-package by Phase 19's AeroSegmentedControl"
  - "resolveButtonStyle(colors, outlined, hovered, pressed, focused, enabled) — pure per-state resolution point wired into AeroButtonSurface"
  - "AeroSurfaceStyleTransformsTest.kt / AeroButtonStylesTest.kt — JVM value-level test convention for pure style transforms (no runComposeUiTest)"
affects: [17-03, 17-04, 17-05, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure, Compose-free state → AeroSurfaceStyle resolution, unit-tested at the value level (kotlin.test, no Compose runtime) — the RESEARCH.md Validation Architecture pattern"
    - "Cross-phase shared transform placed in theme/ (not components/buttons/) specifically for future cross-package reuse without a reach-around"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfaceStyleTransformsTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt

key-decisions:
  - "Disabled-fill blend done via androidx.compose.ui.graphics.lerp (RGB+alpha interpolation), not Color.lighten/darken, matching the UI-SPEC's literal lerp(fillTop, fillBottom, 0.5f) then lerp(..., borderDefault, 0.4f) wording — both mixes stay correct on Classic's fully-opaque tokens"
  - "focused is kept as a resolveButtonStyle parameter for signature symmetry even though it currently branches nothing — focus is carried entirely by AeroButtonSurface's already-wired persistent aeroGlowRing call (D-04), not a fill delta, exactly as 17-UI-SPEC.md's Focus row specifies"
  - "outlined threaded through resolveButtonStyle but not yet consumed — resolves identically to filled; 17-03 applies the fixed-delta outlined transform (fill α×0.15, gloss 0.32→0.15, rim 0.6→0.85) on top, not a second independent resolver"

requirements-completed: [VBTN-02]

coverage:
  - id: D1
    description: "pressedRecess/flattenDisabled/hoverLighten exist as pure functions in theme/AeroSurfaceStyle.kt, unit-tested at the value level (no runComposeUiTest)"
    requirement: "VBTN-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroSurfaceStyleTransformsTest.kt (5 tests, all green)"
        status: pass
    human_judgment: false
  - id: D2
    description: "resolveButtonStyle resolves all five states (rest/hover/press/focus/disabled) with correct precedence and is wired into AeroButtonSurface"
    requirement: "VBTN-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt (5 tests over AeroBlue + Classic, all green)"
        status: pass
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonTest.kt (17-01 render/click suite, re-run green, no regression)"
        status: pass
    human_judgment: true
    rationale: "Value-level tests prove the correct AeroSurfaceStyle is produced for each state, but the actual pixel-level look of hover-brighten/press-recess/disabled-flatten (Success Criterion 2) requires a human eyes-on check — deferred to the 17-05 three-theme sign-off gate per the phase's validation architecture, same as 17-01's D1."
  - id: D3
    description: "pressedRecess lives in theme/AeroSurfaceStyle.kt (not components/buttons/), importable cross-package for Phase 19's AeroSegmentedControl"
    verification:
      - kind: unit
        ref: "source assertion: grep 'internal fun AeroSurfaceStyle.pressedRecess' only matches library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt"
        status: pass
    human_judgment: false
  - id: D4
    description: "No per-frame Brush/gradient rebuilding or Canvas/drawWithContent block added to AeroButtonSurface.kt — every state is a pure AeroSurfaceStyle data transform"
    verification:
      - kind: unit
        ref: "source assertion: grep -E 'graphicsLayer|animateFloatAsState|Canvas\\(|drawWithContent' AeroButtonSurface.kt returns no matches"
        status: pass
    human_judgment: false

# Metrics
duration: 13min
completed: 2026-07-23
status: complete
---

# Phase 17 Plan 02: Per-State Style Resolver Summary

**`pressedRecess()`/`flattenDisabled()`/`hoverLighten()` land as pure, Compose-free transforms in `theme/AeroSurfaceStyle.kt`, and `resolveButtonStyle(...)` wires all five Aero states (rest/hover/press/focus/disabled) into `AeroButtonSurface` with disabled-wins/press-over-hover precedence.**

## Performance

- **Duration:** ~13 min
- **Started:** 2026-07-23T13:48:04Z (after 17-01 plan-metadata commit `2fa1641`)
- **Completed:** 2026-07-23T13:57:14Z
- **Tasks:** 2
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments
- `theme/AeroSurfaceStyle.kt` gains three `internal` extension fns: `pressedRecess(innerShadow)` (the exact `drawAeroGroove` field-swap idiom — fillTop/fillBottom and bevelLight/bevelShadow swapped, gloss off, innerShadow attached), `flattenDisabled(base)` (collapses two-tone fill/bevel to one tone blended toward `borderDefault`, zeroes gloss, halves rim alpha — D-05), and `hoverLighten()` (brightens both fill stops via `Color.lighten` RGB-mix — D-03). `pressedRecess` deliberately lives in the theme layer (not `components/buttons/`) so Phase 19's `AeroSegmentedControl` can import it cross-package.
- `AeroButtonSurface.kt`'s tracer-plan single `AeroSurfaceStyle.rest(...)` call is replaced by `resolveButtonStyle(colors, outlined, hovered, pressed, focused, enabled)` — a pure fn with UI-SPEC precedence (disabled overrides all; press over hover; focus carries no fill delta, only the already-wired persistent `aeroGlowRing` focus call). A native `Shadow(radius = 2.dp, color = Black@0.35, offset = (0.dp, 1.dp))` constant backs the press inner-shadow.
- Two new JVM (`kotlin.test`, no `runComposeUiTest`) test files prove the value-level correctness of every transform and the resolver's precedence rules across both `AeroBlue` (translucent tokens) and `Classic` (fully-opaque tokens), plus a Classic-safety guard that no transform ever produces a fully-transparent fill channel.
- Full `:library:test` suite re-run green — no regression in 17-01's `AeroButtonTest` render/click smoke suite from wiring the new resolver in.

## Task Commits

Each task was committed atomically:

1. **Task 1: Cross-phase style transforms in theme/AeroSurfaceStyle.kt** - `d9acb5c` (feat)
2. **Task 2: resolveButtonStyle pure fn + wire all five states into the surface** - `c82617d` (feat)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` - Added `pressedRecess(innerShadow)`, `flattenDisabled(base)`, `hoverLighten()` extension fns + four private tuning constants
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfaceStyleTransformsTest.kt` - New JVM value-level test file (5 tests: swap/collapse/brighten/Classic-safety)
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - Replaced tracer's rest-only resolution with `resolveButtonStyle(...)` call; added `resolveButtonStyle` pure fn + `PRESSED_INNER_SHADOW` constant
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` - New JVM value-level test file (5 tests over AeroBlue + Classic: rest-equality, disabled-precedence, pressed-swap, hover-brighten, focus-unchanged)

## Decisions Made
- Disabled-fill blend implemented with `androidx.compose.ui.graphics.lerp` (not `Color.lighten`/`darken`) to match the UI-SPEC's literal two-step lerp wording (`lerp(fillTop, fillBottom, 0.5f)` then `lerp(collapsed, borderDefault, 0.4f)`) — verified this stays correct (never a silent no-op, never a fully-transparent result) on `Classic`'s fully-opaque tokens via the Classic-safety test
- `focused` retained as a `resolveButtonStyle` parameter (not dropped from the signature) even though it currently branches nothing — API symmetry with the UI-SPEC's five named states; the actual focus visual stays exclusively the persistent `aeroGlowRing` call per D-04
- `outlined` stays threaded through but unconsumed exactly as 17-01 left it — 17-03 applies the fixed-delta outlined transform on top of `resolveButtonStyle`'s output rather than this plan inventing a placeholder branch

## Deviations from Plan

None - plan executed exactly as written. Both tasks' `<behavior>` specs, `<action>` instructions, and acceptance criteria were implemented and verified as described; no auto-fixes, no architectural questions, no auth gates.

## Issues Encountered

- Same stale-IDE-diagnostic pattern as 17-01 (JVM target 1.8 vs 11 inline-bytecode errors, Kotlin metadata 2.4.0 vs compiler-readable 2.2.0) surfaced after each Edit/Write call on files importing `androidx.compose.ui.graphics.shadow.Shadow`. Confirmed non-issue: `./gradlew :library:test` (both scoped and full-suite runs) compiled and passed cleanly throughout. No action taken.
- Verified the exact `Shadow` constructor signature (`radius: Dp, color: Color = Color.Black, spread: Dp = 0.dp, offset: DpOffset = DpOffset.Zero, alpha: Float = 1f, blendMode: BlendMode = DefaultBlendMode`) by inspecting the `ui-graphics-desktop-1.11.1-sources.jar` directly rather than assuming from the UI-SPEC's prose — confirmed `Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp))` compiles as written.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 17-03 can add `AeroOutlinedButton` by consuming `AeroButtonSurface(..., outlined = true)` and applying its fixed-delta transform inside (or alongside) `resolveButtonStyle`'s output, plus the `Role.Button` + Space/Enter keyboard semantics test and shared-surface source-scan guards per the phase's remaining scope
- 19-selectors-lists can import `AeroSurfaceStyle.pressedRecess(innerShadow)` directly from `theme/` for `AeroSegmentedControl`'s recessed-selected-segment fill — confirmed importable cross-package by construction (D3 coverage above)
- No blockers. Full `:library:test` suite green after this plan's changes.

---
*Phase: 17-buttons*
*Completed: 2026-07-23*

## Self-Check: PASSED

All created/modified files and both task commits (d9acb5c, c82617d) verified present on disk / in git history (see below).
