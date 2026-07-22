---
phase: 16-foundation-aero-primitives-layer
plan: 02
subsystem: ui
tags: [compose-desktop, kotlin, theme-tokens, drawscope, glass-surface, tdd]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    plan: 01
    provides: "drawAeroSurfaceCore(style, cornerPx) single draw implementation, AeroSurfaceStyle,
      AeroOrnamentTokens.derive(base), PrimitivesSection showcase gallery"
provides:
  - "Modifier.aeroGlowRing(active, glowColor, cornerRadius) — gated double-stroke hover/focus glow"
  - "internal DrawScope.drawAeroThumb(style, radiusPx) + public Modifier.aeroThumbSurface(style) — raised circular thumb"
  - "internal DrawScope.drawAeroGroove(style, cornerPx) + public Modifier.aeroGroove(style) — recessed track bed"
  - "PrimitivesSection gallery extended to four demo groups (aeroSurface, aeroGlowRing, aeroThumbSurface, groove)"
affects: [16-05-verification-spike-smoke, 17-buttons, 18-range, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "aeroGlowRing/aeroThumbSurface/aeroGroove all take an already-resolved Color/AeroSurfaceStyle
      parameter rather than reading LocalAeroColors themselves — same 'caller resolves theme,
      primitive just draws' convention as aeroSurface"
    - "Both the raised-thumb and recessed-groove primitives are call paths of drawAeroSurfaceCore
      (thumb: cornerPx = radiusPx on a square draw area collapses the rounded-rect into a circle;
      groove: fillTop/fillBottom and bevelLight/bevelShadow swapped + gloss disabled to read as
      carved-in) — zero bespoke gradient/bevel implementations added"
    - "aeroGlowRing's outer stroke is drawn outside its own layout bounds via unclipped
      drawWithCache/onDrawBehind (Compose's draw phase isn't clipped to layout bounds by default)
      — never by padding/growing the modifier's declared size (Pitfall 7)"
    - "aeroGlowRing gates on a plain active: Boolean inside onDrawBehind (draw block contributes
      nothing at all when false) — no rememberInfiniteTransition or other always-on animation"

key-files:
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt

key-decisions:
  - "Both thumb and groove reuse drawAeroSurfaceCore via style.copy() field swaps rather than
    forking a second gradient implementation, per the plan's explicit prohibition — thumb
    collapses the rounded-rect corner radius to the draw area's half-side-length (circle);
    groove swaps fillTop/fillBottom and bevelLight/bevelShadow (raised -> recessed) with gloss
    forced off, plus one extra darkened top-edge inner-shadow cue layered on top"
  - "D-02 (native dropShadow/innerShadow vs. manual gradient) resolved in favor of manual
    gradients for both aeroGlowRing (radial-gradient double-stroke) and the groove's inner-shadow
    cue (vertical-gradient), matching drawAeroSurfaceCore's existing gradient-only approach and
    avoiding a fourth distinct depth-cue technique in one file; aeroThumbSurface/aeroGroove keep
    optional native dropShadow/innerShadow support inherited from AeroSurfaceStyle for callers
    that want it, unused by the gallery demos themselves"
  - "aeroGlowRing/aeroThumbSurface/aeroGroove take already-resolved Color/AeroSurfaceStyle
    parameters (not @Composable, don't read LocalAeroColors internally) — mirrors aeroSurface's
    existing convention rather than introducing a second theme-access pattern in the same file"

patterns-established:
  - "Pattern 3: circle/groove-shape special cases of a single shared draw core via style field
    swaps (fillTop<->fillBottom, bevelLight<->bevelShadow, cornerPx=radiusPx) — extends 16-01's
    Pattern 1 (one draw function, multiple exposure paths) to cover shape variants too, not just
    Modifier-vs-direct-Canvas exposure"

requirements-completed: [PRIM-06, PRIM-07, PRIM-08, PRIM-13, PRIM-14, PRIM-16]

coverage:
  - id: D1
    description: "Modifier.aeroGlowRing renders without exception both active=true and
      active=false across AeroBlue/AeroDark/Classic; gated so inactive contributes zero draw
      calls; no rememberInfiniteTransition anywhere in its function body"
    requirement: "PRIM-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt"
        status: pass
    human_judgment: false
  - id: D2
    description: "drawAeroThumb/Modifier.aeroThumbSurface render a raised circular surface across
      three themes and call drawAeroSurfaceCore (source-level guard, not just a passing render)"
    requirement: "PRIM-07"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt"
        status: pass
    human_judgment: false
  - id: D3
    description: "drawAeroGroove/Modifier.aeroGroove render a recessed bed across three themes,
      drawAeroGroove has a DrawScope receiver callable directly inside Canvas, and both call
      drawAeroSurfaceCore (source-level guard)"
    requirement: "PRIM-08"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt"
        status: pass
    human_judgment: false
  - id: D4
    description: "Glow/thumb/groove geometry built inside drawWithCache; no Color.Transparent in
      any of the three new function bodies (KDoc mentions of the anti-pattern by name excluded
      from the scoped regex, matching the GlassModifiersTest precedent)"
    requirement: "PRIM-13, PRIM-14"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt"
        status: pass
    human_judgment: false
  - id: D5
    description: "PrimitivesSection renders four captioned demo groups (aeroSurface, aeroGlowRing,
      aeroThumbSurface, groove) in the existing DemoBox grid, reachable under the showcase's
      ThemeSwitcher; :showcase:compileKotlin succeeds"
    requirement: "PRIM-16"
    verification:
      - kind: other
        ref: "./gradlew :showcase:compileKotlin (BUILD SUCCESSFUL) + caption-string greps"
        status: pass
    human_judgment: true
    rationale: "Visual three-theme confirmation that the glow ring reads as a hover/focus glow,
      the thumb reads as raised, and the groove reads as recessed (not just 'renders without
      exception') is explicitly deferred to the 16-05 three-theme sign-off checkpoint per this
      plan's own <verify> block and CONTEXT.md D-04 — not a per-plan gate."

duration: 32min
completed: 2026-07-22
status: complete
---

# Phase 16 Plan 02: Draw Primitives Expansion Summary

**Three new call paths of the single `drawAeroSurfaceCore` — a gated double-stroke hover/focus glow ring, a raised-thumb circle, and a recessed track-groove — added to `AeroSurfacePrimitives.kt` and wired into the permanent Primitives showcase gallery alongside the 16-01 surface demo, proven to compile and unit-test green across all three built-in themes with zero bespoke gradient/bevel code.**

## Performance

- **Duration:** ~32 min
- **Tasks:** 2 (both `tdd="true"`, each RED → GREEN cycle)
- **Files modified:** 3 (`AeroSurfacePrimitives.kt`, `AeroSurfacePrimitivesTest.kt`, `PrimitivesSection.kt`)

## Accomplishments

- `Modifier.aeroGlowRing(active, glowColor, cornerRadius)` — inner crisp stroke + outer wider/fainter radial-gradient stroke fading to `glowColor.copy(alpha = 0f)`; gated so `active = false` contributes zero draw calls (no `rememberInfiniteTransition`, no always-on animation); outer stroke renders outside the modifier's own layout bounds via unclipped `drawWithCache`/`onDrawBehind` (Pitfall 7 — never by growing declared size)
- `internal DrawScope.drawAeroThumb(style, radiusPx)` + `public Modifier.aeroThumbSurface(style)` — the circle-shape special case of `drawAeroSurfaceCore`: passing `radiusPx` as `cornerPx` on a square draw area collapses the shared rounded-rect fill/gloss/bevel/rim into a circle; zero second gradient implementation
- `internal DrawScope.drawAeroGroove(style, cornerPx)` + `public Modifier.aeroGroove(style)` — the recessed-shape special case: `style.copy()` swaps `fillTop`/`fillBottom` and `bevelLight`/`bevelShadow` (raised → carved-in) with gloss disabled, plus one extra darkened top-edge inner-shadow cue layered on top; `drawAeroGroove` stays a plain `DrawScope` extension so `AeroRangeSlider`-style Canvas-owning consumers can call it directly, matching `drawAeroSurfaceCore`'s dual Modifier/direct-Canvas convention
- `PrimitivesSection` gallery extended from one to four captioned `DemoBox`es (`aeroSurface`, `aeroGlowRing`, `aeroThumbSurface`, `groove`) in the existing `Row`/`DemoBox` grid
- `AeroSurfacePrimitivesTest.kt` — 10 tests total: render-without-exception smoke passes across `AeroBlue`/`AeroDark`/`Classic` for each new primitive, source-level "reuses `drawAeroSurfaceCore`" guards for both thumb and groove, a `DrawScope`-receiver guard for `drawAeroGroove`, and `rememberInfiniteTransition`/`Color.Transparent` regression guards scoped to the relevant function bodies

## Task Commits

**Task 1** (`tdd="true"`) — `aeroGlowRing` + raised-thumb primitive:

1. **RED:** `c43ca5e` — `test(16-02): add failing test for aeroGlowRing + raised-thumb primitive`
2. **GREEN:** `6647222` — `feat(16-02): implement aeroGlowRing + raised-thumb primitive`

**Task 2** (`tdd="true"`) — recessed track-groove + gallery demos:

3. **RED:** `4967d95` — `test(16-02): add failing test for recessed track-groove primitive`
4. **GREEN:** `3f015ab` — `feat(16-02): implement recessed track-groove + gallery demos`

No REFACTOR commits needed — both GREEN implementations were already minimal and idiomatic; no cleanup pass changed behavior.

**Plan metadata:** committed separately by this SUMMARY step (see final commit below).

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — added `aeroGlowRing`, `drawAeroThumb`/`aeroThumbSurface`, `drawAeroGroove`/`aeroGroove`
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfacePrimitivesTest.kt` — 10 tests covering all three new primitives (render smoke + source-level reuse/regression guards)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt` — added `aeroGlowRing`, `aeroThumbSurface`, and `groove` DemoBoxes to the existing gallery grid

## Decisions Made

- Both the thumb and groove primitives reuse `drawAeroSurfaceCore` via `style.copy()` field swaps rather than a second gradient implementation, per the plan's explicit prohibition. Thumb: `cornerPx = radiusPx` on a square draw area collapses the rounded-rect into a circle. Groove: `fillTop`/`fillBottom` and `bevelLight`/`bevelShadow` swapped (raised → recessed) with `glossAlpha = 0f`, plus one additional darkened top-edge inner-shadow cue layered on top of the reused core's output.
- D-02 (native `dropShadow`/`innerShadow` vs. manual gradient, left to Claude's discretion per primitive) resolved in favor of manual gradients for both `aeroGlowRing`'s double-stroke and the groove's inner-shadow cue — consistent with `drawAeroSurfaceCore`'s existing all-gradient approach, avoiding a fourth distinct depth-cue technique in one file. `aeroThumbSurface`/`aeroGroove` still expose `AeroSurfaceStyle`'s optional native `dropShadow`/`innerShadow` fields for callers that want them (unused by the gallery demos themselves, matching `aeroSurface`'s existing pattern).
- `aeroGlowRing`/`aeroThumbSurface`/`aeroGroove` all take already-resolved `Color`/`AeroSurfaceStyle` parameters rather than reading `LocalAeroColors` internally — mirrors `aeroSurface`'s existing "caller resolves the theme, primitive just draws" convention rather than introducing a second theme-access pattern in the same file.
- Test-design fix mid-task: two source-level regression guards (`Color.Transparent`, `rememberInfiniteTransition`) initially scanned the whole file and false-positived on this plan's own KDoc prose naming those anti-patterns by name (to document what NOT to do). Rescoped both guards to the specific new functions' bodies (matching `GlassModifiersTest`'s existing `functionBody()` extraction helper), which also caught a real instance later — an inline code comment inside `drawAeroGroove`'s body literally said "never `Color.Transparent`", requiring a rewording (not a code change) to keep the guard meaningful.

## Deviations from Plan

None — plan executed exactly as written. The RED-phase stub production code (identity-modifier `aeroGlowRing`/`aeroThumbSurface`/`aeroGroove`, a fresh-`Color`-only `drawAeroThumb`/`drawAeroGroove`) was TDD scaffolding internal to each task's own cycle, not a deviation from the plan's `<action>` steps — same pattern 16-01 established.

## Issues Encountered

None blocking. Two RED-phase test-design false positives (both source-level regex guards matching this file's own KDoc prose rather than actual code) were caught and fixed during the same TDD cycle — see Decisions Made above. `./gradlew :library:compileKotlin :showcase:compileKotlin` succeeded on the first attempt after each GREEN implementation; the full `:library:test` suite (258 tests, up from 247 after 16-01) passed with no regressions.

Note: the IDE's live Kotlin-plugin diagnostics (surfaced automatically after several `Edit`/`Write` calls, same as 16-01/16-03/16-04) again reported spurious `INLINE_FROM_HIGHER_PLATFORM`/`INCOMPATIBLE_CLASS`/`Unresolved reference` errors on both touched and untouched lines. These are IDE tooling/indexing artifacts (bundled Kotlin compiler lags the Gradle-pinned 2.4.10 toolchain) — not real compile errors; the authoritative `./gradlew` runs are clean throughout.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- All three PRIM-06/07/08 primitives now exist as proven call paths of `drawAeroSurfaceCore`, unblocking Phase 18 (Range — `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar` all need the groove; both sliders need the thumb) and Phase 19 (`AeroSwitch` needs both the thumb and the glow ring for hover/focus).
- Per this plan's explicit prohibition, none of the three new primitives is wired into any real target component yet (confirmed: `grep -rn "aeroGlowRing\|aeroThumbSurface\|aeroGroove"` across `library/src/main` and `showcase/src/main` matches only `AeroSurfacePrimitives.kt` itself and the isolated `PrimitivesSection.kt` gallery demos) — Phases 17-19 consume them starting from a clean slate.
- The three-theme visual confirmation that the glow reads as glow (not a flat outline), the thumb reads as raised, and the groove reads as recessed remains explicitly deferred to the 16-05 three-theme sign-off checkpoint, per this plan's own `<verify>` block and CONTEXT.md D-04 — this plan's automated proof (compile + unit tests across three themes, source-level reuse guards) is complete, but no human has yet visually confirmed the four gallery demos in the running showcase.
- `aeroGlowRing`'s stroke widths/outset (1.5dp/4dp/3dp) and the groove's inner-shadow cue fraction/alpha (0.4/0.5) are calibration starting points (16-CONTEXT.md: "glow-ring geometry and thumb/groove geometry are Claude's-discretion") — expect possible refinement at 16-05, consistent with 16-01's `AeroOrnamentTokens` magnitudes carrying the same caveat.

---
*Phase: 16-foundation-aero-primitives-layer*
*Completed: 2026-07-22*

## Self-Check: PASSED

All 3 files modified in this plan confirmed present on disk with the expected new functions
(`aeroGlowRing`, `drawAeroThumb`, `aeroThumbSurface`, `drawAeroGroove`, `aeroGroove` in
`AeroSurfacePrimitives.kt`; four captioned DemoBoxes in `PrimitivesSection.kt`); all 4 task
commits (`c43ca5e` RED, `6647222` GREEN, `4967d95` RED, `3f015ab` GREEN) confirmed present in
`git log`.
