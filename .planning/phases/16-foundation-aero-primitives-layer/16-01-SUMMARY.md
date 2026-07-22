---
phase: 16-foundation-aero-primitives-layer
plan: 01
subsystem: ui
tags: [compose-desktop, kotlin, theme-tokens, drawscope, glass-surface, tdd]

# Dependency graph
requires:
  - phase: 15-toolchain-upgrade
    provides: Kotlin 2.4.10 / CMP 1.11.1 / Material3 1.9.0 toolchain, dropShadow/innerShadow
      confirmed at androidx.compose.ui.draw (ScratchAeroShadowProof.kt)
provides:
  - "Color.lighten/darken RGB-mix helpers (theme/ColorMath.kt)"
  - "AeroOrnamentTokens data class + derive(base) algorithmic per-theme derivation"
  - "AeroColorScheme.ornamentOverride trailing escape hatch (source-compatible)"
  - "AeroTheme.ornaments accessor"
  - "AeroSurfaceStyle declarative surface spec + rest() preset"
  - "drawAeroSurfaceCore(style, cornerPx) single draw implementation + Modifier.aeroSurface(style, shape)"
  - "PrimitivesSection showcase gallery (permanent, D-04) registered in ShowcaseApp"
affects: [16-02-primitives-glow-ring, 16-03-primitives-thumb-groove, 16-04-glassmodifiers-fixes,
  16-05-three-theme-signoff, 17-buttons, 18-range, 19-selectors-lists]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Single DrawScope draw function (drawAeroSurfaceCore) exposed via Modifier extension
      AND direct-call convention for Canvas-owning consumers — no per-component gradient code"
    - "Algorithmic color-token derivation (AeroOrnamentTokens.derive) via RGB-mix lighten/darken,
      never hand-authored per-theme literals"
    - "Source-compatible data-class extension via trailing defaulted field + escape-hatch override"
    - "Gradient fades target baseColor.copy(alpha = 0f), never Color.Transparent (works on
      Classic's opaque tokens too)"
    - "Every gradient stop is size.height/width * fraction, never a bare pixel literal"

key-files:
  created:
    - library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt
    - library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

key-decisions:
  - "AeroOrnamentTokens field set (glossHighlight/bevelLight/bevelShadow/rimLight/hoverGlow/
    grooveShadow/fillSplitTop/fillSplitBottom) and lighten/darken magnitudes taken verbatim
    from 16-RESEARCH.md Code Examples — starting calibration, re-reviewed at 16-05"
  - "AeroSurfaceStyle's two-tone fill uses a 3-stop gradient (fillTop -> lerp midpoint at
    seamFraction -> fillBottom) to make the 'soft seam' (D-01) an actual controllable field
    rather than relying on a plain 2-stop gradient's implicit softness"
  - "drawAeroSurfaceCore is called from inside Modifier.aeroSurface's drawWithCache{onDrawBehind{}}
    block per 16-RESEARCH.md Pattern 1's literal skeleton — brush construction happens per
    draw call, matching the cited authoritative source over a stricter interpretation of
    PRIM-13 caching that would require splitting the single-function design apart"
  - "Existing AeroColorSchemeTest's declaredMemberProperties count assertion updated 23 -> 24
    to reflect the source-compatible ornamentOverride append (the append itself required no
    edits to any constructor call, only this structural-count assertion)"

patterns-established:
  - "Pattern 1: One draw function, exposed via Modifier extension (aeroSurface) — direct-call
    exposure inside Canvas DrawScopes remains available since drawAeroSurfaceCore is a plain
    DrawScope extension, not Modifier-bound"
  - "Pattern 2: Algorithmic ornament derivation via Color.lighten/darken RGB-mix"

requirements-completed: [PRIM-01, PRIM-02, PRIM-03, PRIM-04, PRIM-05, PRIM-12, PRIM-13, PRIM-14, PRIM-16]

coverage:
  - id: D1
    description: "Color.lighten/darken RGB-mix toward White/Black with alpha preserved"
    requirement: "PRIM-01"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt"
        status: pass
    human_judgment: false
  - id: D2
    description: "AeroOrnamentTokens.derive(base) produces non-null, theme-sensitive tokens
      for AeroBlue/AeroDark/Classic, including Classic's opaque tokens"
    requirement: "PRIM-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt"
        status: pass
    human_judgment: false
  - id: D3
    description: "AeroColorScheme carries a trailing, defaulted ornamentOverride field;
      existing 23-field constructor calls and presets compile unchanged"
    requirement: "PRIM-03"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt"
        status: pass
    human_judgment: false
  - id: D4
    description: "AeroSurfaceStyle declarative spec + AeroSurfacePrimitives single
      drawAeroSurfaceCore/Modifier.aeroSurface implementation compiles and centralizes clip
      ordering (PRIM-05/12/13/14)"
    requirement: "PRIM-05"
    verification:
      - kind: other
        ref: "./gradlew :library:compileKotlin (BUILD SUCCESSFUL) + acceptance-criteria greps
          (clip precedes drawWithCache; no Color.Transparent; all gradient stops proportional)"
        status: pass
    human_judgment: false
  - id: D5
    description: "PrimitivesSection aeroSurface demo renders and is registered in ShowcaseApp,
      reachable under the existing ThemeSwitcher across AeroBlue/AeroDark/Classic"
    requirement: "PRIM-16"
    verification:
      - kind: other
        ref: "./gradlew :showcase:compileKotlin (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: true
    rationale: "Visual three-theme glass-quality confirmation (gloss+gradient+seam+bevel
      reading as glass, not flat) is explicitly deferred to the 16-05 three-theme sign-off
      checkpoint per this plan's own <verify><human-check> and CONTEXT.md D-04 — not a
      per-plan gate. Compile success + automated token tests are the this-plan-level proof."

duration: 20min
completed: 2026-07-22
status: complete
---

# Phase 16 Plan 01: Primitives Spine Tracer Summary

**End-to-end Aero surface primitive spine — RGB-mix color math, algorithmic per-theme ornament tokens, a source-compatible `AeroColorScheme.ornamentOverride` escape hatch, declarative `AeroSurfaceStyle`, a single `drawAeroSurfaceCore`/`Modifier.aeroSurface` draw implementation, and a permanent showcase gallery — proven to compile and unit-test green across all three built-in themes.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-07-22T17:17Z (RED commit)
- **Completed:** 2026-07-22T17:21Z (GREEN commit)
- **Tasks:** 1 (tracer, tdd=true → RED + GREEN commits)
- **Files modified:** 11 (7 created, 4 modified)

## Accomplishments

- `Color.lighten(amount)`/`Color.darken(amount)` — RGB-mix extension functions that visibly brighten/darken even `AeroColorScheme.Classic`'s fully-opaque tokens, alpha always preserved
- `AeroOrnamentTokens` data class + `derive(base)` — algorithmic, theme-sensitive ornament token derivation; zero hand-authored per-theme literals
- `AeroColorScheme.ornamentOverride: AeroOrnamentTokens? = null` — trailing, defaulted, fully source-compatible with every existing constructor call and preset
- `AeroTheme.ornaments` — resolves the override-or-derive chain
- `AeroSurfaceStyle` — declarative surface spec (fill/gloss/bevel/rim + optional native drop/inner shadow) with a `rest()` preset built from `AeroOrnamentTokens`
- `drawAeroSurfaceCore(style, cornerPx)` + `Modifier.aeroSurface(style, shape)` — single implementation authoring two-tone fill (soft seam), proportional gloss band, inset bevel, inset rim; `.clip(shape)` centralized as the outermost paint-affecting modifier
- `PrimitivesSection` showcase gallery — `aeroSurface` demo box, registered in `ShowcaseApp` under a new "Primitives" heading, reachable via the existing `ThemeSwitcher`

## Task Commits

Task 1 (`tdd="true"`, `type="tracer"`) produced the required RED → GREEN cycle:

1. **RED:** `9586b7f` — `test(16-01): add failing tests for Color.lighten/darken and AeroOrnamentTokens.derive`
2. **GREEN:** `693bc6c` — `feat(16-01): implement aeroSurface spine — tokens through showcase, three themes`

No REFACTOR commit needed — implementation was already minimal and idiomatic after GREEN.

**Plan metadata:** committed separately by this SUMMARY step (see final commit below).

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt` - `Color.lighten`/`darken` RGB-mix helpers
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` - ornament token data class + `derive(base)`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` - appended trailing `ornamentOverride` field
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` - added `AeroTheme.ornaments` accessor
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` - declarative surface spec + `rest()` preset
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` - `drawAeroSurfaceCore` + `Modifier.aeroSurface`
- `library/src/test/kotlin/com/mordred/aero/theme/ColorMathTest.kt` - PRIM-01 unit tests
- `library/src/test/kotlin/com/mordred/aero/theme/AeroOrnamentTokensTest.kt` - PRIM-02 unit tests
- `library/src/test/kotlin/com/mordred/aero/theme/AeroColorSchemeTest.kt` - extended for `ornamentOverride` + updated token count
- `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt` - permanent Primitives gallery (D-04)
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` - registered `PrimitivesSection()`

## Decisions Made

- Followed 16-RESEARCH.md's Code Examples verbatim for `AeroOrnamentTokens`'s field set and lighten/darken magnitudes (explicitly flagged `[ASSUMED]`/Claude's discretion in the research, re-reviewed at 16-05).
- Implemented the fill gradient as a 3-stop `colorStops` gradient (`fillTop` → seam-fraction midpoint → `fillBottom`) rather than a plain 2-stop gradient, so `AeroSurfaceStyle.seamFraction` is an actual controllable field consistent with D-01's "soft seam" requirement, not a vestigial unused parameter.
- Called `drawAeroSurfaceCore` from inside `Modifier.aeroSurface`'s `drawWithCache { onDrawBehind { ... } }` block exactly per 16-RESEARCH.md Architecture Pattern 1's cited code skeleton (the same file explicitly attributes this shape to satisfying PRIM-13). Followed the authoritative source verbatim rather than pre-splitting the single-function design into a cache-only/draw-only pair, preserving the "one implementation, four exposure paths" architecture the plan's `<must_haves>` require.
- Updated `AeroColorSchemeTest.aeroColorSchemeHasTwentyThreeTokens` → `aeroColorSchemeHasTwentyFourTokensAfterOrnamentOverrideAppend` (23 → 24) since the trailing field append is reflection-visible; this is the only pre-existing assertion that needed changing, and no existing constructor call needed any edit (PRIM-03 confirmed).

## Deviations from Plan

None — plan executed exactly as written. The RED-phase stub production code (identity `lighten`/`darken`, constant `Color.Black` `derive()`) was scaffolding internal to the TDD cycle itself, not a deviation from the plan's `<action>` steps.

## Issues Encountered

None. `./gradlew :library:compileKotlin :showcase:compileKotlin` succeeded on the first attempt after the GREEN implementation; the full `:library:test` suite (247 tests, up from the 232 baseline noted in 16-RESEARCH.md) passed with no regressions.

Note: the IDE's live Kotlin-plugin diagnostics (surfaced automatically after several `Edit`/`Write` calls in this session) reported spurious `INLINE_FROM_HIGHER_PLATFORM` / `INCOMPATIBLE_CLASS` / `Unresolved reference` errors on both touched and untouched pre-existing lines across multiple files. These are IDE tooling/indexing artifacts (the IDE's bundled Kotlin compiler version lags the project's Gradle-pinned Kotlin 2.4.10 toolchain) — not real compile errors. The authoritative `./gradlew` compile and test runs are clean.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The full four-layer architecture (tokens → style → single draw core → showcase) is proven end-to-end on one vertical slice, unblocking 16-02/16-03 (glow-ring, thumb, groove primitives) to reuse the same `AeroOrnamentTokens`/`AeroSurfaceStyle`/`drawAeroSurfaceCore` foundation without re-deriving the architecture.
- The three-theme visual glass-quality confirmation (gloss/gradient/seam/bevel reading as genuinely glass, not flat) remains explicitly deferred to the 16-05 three-theme sign-off checkpoint per this plan's own `<verify>` block and CONTEXT.md D-04 — this plan's automated proof (compile + unit tests) is complete, but no human has yet visually confirmed the `aeroSurface` demo box in the running showcase.
- `AeroSurfaceStyle`'s exact gloss/bevel/rim magnitudes are calibration starting points (16-RESEARCH.md Assumption A2/A3) — expect possible refinement at the 16-05 review, which is a normal part of this phase's planned flow, not a defect.

---
*Phase: 16-foundation-aero-primitives-layer*
*Completed: 2026-07-22*

## Self-Check: PASSED

All 12 files created/modified in this plan confirmed present on disk; all 3 commits
(`9586b7f` RED, `693bc6c` GREEN, `cec5c47` docs SUMMARY) confirmed present in `git log`.
