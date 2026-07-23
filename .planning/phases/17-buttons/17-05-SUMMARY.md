---
phase: 17-buttons
plan: 05
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, buttons, visual-signoff, showcase]

# Dependency graph
requires:
  - phase: 17-buttons
    plan: 04
    provides: "ButtonsSection.kt demo rows for both variants (enabled/disabled/long-label) plus the live three-theme switcher used for the sign-off review"
provides:
  - "Human three-theme (AeroBlue/AeroDark/Classic) x five-state (rest/hover/press/focus/disabled) visual sign-off: APPROVED for both AeroButton and AeroOutlinedButton"
  - "D-01 accent-identity finding reconfirmed at sign-off: AeroSurfaceStyle.rest() resolves fill/bevel/glow to accent-derived colors, filled button reads as a classic Win7 blue default-action button"
  - "Two rounds of operator-driven calibration fixes closing three defects found during the gate itself (rim brightness, filled-fill contrast, disabled-state legibility)"
affects: [18-range, 19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Theme-aware rim alpha: rim opacity is capped via min(native glassBorder alpha, 0.45) rather than a flat constant, so light AeroBlue/AeroDark themes dim to ~0.19-0.31 while Classic (whose native glassBorder alpha is already <=0.45) is left untouched — a single formula that self-adapts per theme instead of per-theme branching"
    - "Button-scoped fill darkening (primary.darken(0.20f/0.36f)) applied only inside AeroButtonSurface's own style resolution, not to the shared AeroOrnamentTokens/gallery — keeps the fix local to the one surface where AeroBlue/AeroDark's very light primary (#4FC3F7/#90CAF9) made white label text unreadable, without touching every other primary-derived surface in the library"
    - "flattenDisabled's terminal blend target changed from a light borderDefault toward base.surface (the theme's own dark/neutral surface color) at blend 0.5 — disabled buttons now recede into the background instead of standing out as a light-gray blob, consistent across all three themes including Classic's opaque tokens"

key-files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
    - library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroSurfaceStyleTransformsTest.kt

key-decisions:
  - "Rim alpha capped at min(native glassBorder alpha, 0.45) rather than a single flat constant across all three themes, so Classic (which relies on a brighter contour for legibility on its own tokens) is not over-dimmed to match AeroBlue/AeroDark"
  - "Filled-button fill darkening is scoped to AeroButtonSurface's own resolution, not pushed into AeroOrnamentTokens.derive() or AeroSurfaceStyle.rest() defaults — avoids darkening every other component (gallery, panels, etc.) that shares the same accent-derived ornament tokens"
  - "flattenDisabled's blend target flipped from a light neutral (borderDefault) to the theme's own base.surface, and blend ratio raised 0.4 -> 0.5, so 'disabled' reads as receding into the background on all three themes rather than as a conspicuous light-gray patch"

requirements-completed: [VBTN-01, VBTN-02, VBTN-05]

coverage:
  - id: D1
    description: "Human three-theme x five-state sign-off passes for AeroButton (filled): rest/hover/press/focus/disabled all confirmed genuine Aero glass across AeroBlue, AeroDark, and Classic"
    requirement: "VBTN-01"
    verification:
      - kind: manual_procedural
        ref: "Operator ran ./gradlew :showcase:run, navigated to Buttons section, cycled AeroBlue/AeroDark/Classic via the live theme switcher, exercised each state (rest/mouse-over/click-hold/Tab/disabled row) per the 17-05-PLAN.md verification matrix"
        status: pass
    human_judgment: true
    rationale: "Geometry-level Aero idioms (two-tone fill, gloss, bevel, glow, recess, flatten) are only verifiable by eye across all three themes — the project's own v2.0.3/v2.0.4 false-positive-sign-off lesson makes this a mandatory human gate, not an automatable check (must_haves prohibition: must NOT auto-approve)."
  - id: D2
    description: "Human three-theme x five-state sign-off passes for AeroOutlinedButton: fixed-delta faint-glass treatment confirmed as a lighter sibling of the filled button, not a visually-drifted second painter, across all three themes and all five states"
    requirement: "VBTN-02"
    verification:
      - kind: manual_procedural
        ref: "Same operator review session as D1, outlined variant reviewed alongside the filled variant in each theme/state cell"
        status: pass
    human_judgment: true
    rationale: "Same rationale as D1 - visual-only verification, mandatory human gate per must_haves prohibitions."
  - id: D3
    description: "AeroOutlinedButton's fixed-delta transform (whisper of fill, brighter rim) confirmed visually distinct-but-non-drifted from the filled button after calibration"
    requirement: "VBTN-05"
    verification:
      - kind: manual_procedural
        ref: "Same operator review session as D1/D2"
        status: pass
    human_judgment: true
    rationale: "Same rationale as D1 - visual-only verification, mandatory human gate per must_haves prohibitions."
  - id: D4
    description: "D-01 accent-identity finding reconfirmed at sign-off: filled button's fill/bevel/glow resolve to accent (primary-derived) colors via AeroSurfaceStyle.rest(), reading as a classic Win7 blue default-action button, visibly distinct from neutral panels"
    verification:
      - kind: manual_procedural
        ref: "Operator confirmed per 17-05-PLAN.md how-to-verify step 5 (accept resolved-by-construction note)"
        status: pass
    human_judgment: true
    rationale: "Literal-wording confirmation of a documented UI-SPEC finding — requires the human reviewer's explicit agreement, not a code assertion."
  - id: D5
    description: "Two rounds of calibration fixes closed three operator-found defects before final approval: (1) rim too bright/white on AeroBlue/AeroDark, (2) filled fill too light (unreadable white text) on AeroBlue/AeroDark, (3) disabled state stood out as light-gray instead of receding"
    verification:
      - kind: unit
        ref: ":library:test full suite (303 tests, 0 failures) after all four calibration commits"
        status: pass
      - kind: integration
        ref: ":showcase:compileKotlin"
        status: pass
    human_judgment: false
---

# Phase 17 Plan 05: Three-Theme x Five-State Human Sign-Off Summary

**Mandatory visual sign-off APPROVED for both `AeroButton` and `AeroOutlinedButton` across AeroBlue/AeroDark/Classic x rest/hover/press/focus/disabled, reached after two rounds of operator-driven calibration fixes to rim brightness, filled-fill contrast, and disabled-state legibility.**

## Performance

- **Duration:** ~39 min (calibration rounds, 2026-07-23T17:45:52Z -> 2026-07-23T18:24:09Z) + review/write-up
- **Started:** 2026-07-23T17:45:52Z (first calibration commit, following initial operator rejection)
- **Completed:** 2026-07-23 (final "approved" verdict)
- **Tasks:** 1 (checkpoint:human-verify)
- **Files modified:** 6 (library source + tests; no plan-level task files — calibration commits are Rule 1 bug-fixes surfaced by the visual gate itself)

## Accomplishments
- Human operator ran the desktop showcase (`./gradlew :showcase:run`), navigated to the Buttons section, and reviewed both `AeroButton` and `AeroOutlinedButton` across all three themes (AeroBlue, AeroDark, Classic) and all five states (rest, hover, press, focus, disabled)
- Verdict: **approved** — the full three-theme x five-state matrix passes for both variants, closing VBTN-01/02/05's visual acceptance criterion
- D-01 finding reconfirmed at sign-off: the filled button's accent identity (blue on AeroBlue, theme-accent on AeroDark/Classic) is present and correctly resolved-by-construction via `AeroSurfaceStyle.rest()` — no bespoke override was ever needed
- Long-label truncation confirmed: both variants truncate single-line at the locked height, no wrap/grow
- Sign-off was reached only after two calibration rounds addressing three defects the operator found during the gate itself (see Deviations below) — the gate did its job: it caught real visual regressions that static/unit verification in 17-01..17-04 could not

## Task Commits

This plan's sole task is the human-verify checkpoint itself (no code task to commit). The calibration work that unblocked approval was committed as four Rule 1 (auto-fix bug) deviations during the gate:

1. `60b0c23` fix(17): soften button rim to library glassBorder + tone filled fill/gloss (round 1)
2. `eff570f` fix(17): make disabled flatten terminal so disabled-outlined dims its rim (round 1)
3. `09aa7ad` fix(17): theme-aware button rim alpha (cap 0.45, keep Classic) + darker filled fill (round 2)
4. `7b2cd58` fix(17): disabled buttons recede toward theme surface instead of light gray (round 2)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` - rim alpha capping, fill darkening, disabled-flatten wiring
- `library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt` - rim token adjustment supporting the theme-aware alpha cap
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfaceStyle.kt` - `flattenDisabled` terminal blend target changed to `base.surface`, blend ratio 0.4 -> 0.5
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonStylesTest.kt` - updated/added coverage for the calibrated rim/fill values
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButtonStylesTest.kt` - updated/added coverage for outlined-variant rim dimming and disabled-terminal behavior
- `library/src/test/kotlin/com/mordred/aero/theme/AeroSurfaceStyleTransformsTest.kt` - coverage for `flattenDisabled`'s new terminal-target behavior

## Decisions Made
- Rim alpha capped via `min(native glassBorder alpha, 0.45)` rather than a flat constant, so Classic (which needs a brighter contour on its own tokens for legibility) is not forced down to AeroBlue/AeroDark's dimmer level
- Filled-fill darkening (`primary.darken(0.20f/0.36f)`) scoped to `AeroButtonSurface`'s own style resolution only — deliberately not pushed into `AeroOrnamentTokens.derive()` or `AeroSurfaceStyle.rest()`'s defaults, so the fix does not darken every other accent-derived surface in the library (gallery, panels, etc.)
- `flattenDisabled`'s blend target changed from a light neutral (`borderDefault`) to the theme's own `base.surface`, with blend ratio raised 0.4 -> 0.5, so disabled buttons recede toward the background rather than standing out as a light-gray patch — consistent across all three themes, including Classic's opaque tokens

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Rim too bright/white on AeroBlue and AeroDark**
- **Found during:** Task 1 (human-verify checkpoint, first review pass)
- **Issue:** The outer contour rim read as glaringly white on the two light-primary themes (AeroBlue, AeroDark), undermining the "genuine Aero glass, not flat" read the checkpoint gates on
- **Fix:** Rim opacity capped at `min(native glassBorder alpha, 0.45)`; AeroBlue/AeroDark dim to ~0.19-0.31, Classic (already <=0.45 natively) unaffected
- **Files modified:** `AeroButtonSurface.kt`, `AeroOrnamentTokens.kt`, `AeroSurfaceStyle.kt`, `AeroOutlinedButtonStylesTest.kt`
- **Verification:** `:library:test` green after fix; re-reviewed visually by operator in round 2
- **Committed in:** `60b0c23`, refined further in `09aa7ad`

**2. [Rule 1 - Bug] Filled AeroButton fill unreadably light on AeroBlue/AeroDark**
- **Found during:** Task 1 (human-verify checkpoint, first review pass)
- **Issue:** AeroBlue/AeroDark's `primary` tokens are very light (`#4FC3F7` / `#90CAF9`); the filled button's fill inherited this directly, making the white label text hard to read (Classic's `#5C8ABF` was already fine)
- **Fix:** Button-scoped fill darkening via `primary.darken(0.20f)`/`primary.darken(0.36f)` inside `AeroButtonSurface`'s own resolution path only — the shared ornament tokens and gallery surfaces are untouched
- **Files modified:** `AeroButtonSurface.kt`, `AeroButtonStylesTest.kt`
- **Verification:** `:library:test` green after fix; re-reviewed visually by operator in round 2
- **Committed in:** `60b0c23`, refined further in `09aa7ad`

**3. [Rule 1 - Bug] Disabled state stood out as a light-gray blob instead of receding**
- **Found during:** Task 1 (human-verify checkpoint, first review pass)
- **Issue:** `flattenDisabled` blended toward a light `borderDefault`, so disabled buttons visually stood out rather than reading as "dead"/inert glass, undercutting the checkpoint's disabled-state acceptance criterion (D-05, "geometry itself must flatten, not merely fade")
- **Fix:** Blend target changed to the theme's own `base.surface`, blend ratio raised 0.4 -> 0.5, making `flattenDisabled` terminal (fully collapsed, not partially) so the disabled-outlined variant's rim dims correctly too
- **Files modified:** `AeroSurfaceStyle.kt`, `AeroSurfaceStyleTransformsTest.kt`, `AeroOutlinedButtonStylesTest.kt`
- **Verification:** `:library:test` green after fix (303 tests / 0 failures final run); `:showcase:compileKotlin` green; structural guards VBTN-03/VBTN-06 (source-scan) intact
- **Committed in:** `eff570f`, refined further in `7b2cd58`

---

**Total deviations:** 3 auto-fixed defects across 4 commits (2 calibration rounds), all Rule 1 (bug fixes surfaced by the visual gate)
**Impact on plan:** All auto-fixes were necessary corrections to genuinely broken visuals the checkpoint exists to catch — exactly the class of defect static/unit tests in 17-01..17-04 cannot see. No scope creep: no new components, states, or public API surface were touched; only rim alpha, filled-fill darkening, and the disabled-flatten blend target within the already-planned VBTN-01/02/05 surfaces.

## Issues Encountered
None beyond the three visual defects documented above as deviations — all three were found, fixed, and re-verified within this checkpoint's own review loop before the operator's final "approved" verdict.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 17 (Buttons)'s visual acceptance criterion is satisfied: both `AeroButton` and `AeroOutlinedButton` pass the mandatory three-theme x five-state human sign-off, closing VBTN-01, VBTN-02, and VBTN-05's human-judgment coverage
- `:library:test` full suite green (303 tests / 0 failures) and `:showcase:compileKotlin` green as of the final calibration commit (`7b2cd58`)
- The theme-aware rim-alpha-cap and button-scoped fill-darkening patterns established here are candidates for reuse in Phase 19 (`AeroSegmentedControl` reuses pressed-button fill per PROJECT.md's Key Decisions) — flag for the Phase 19 planner
- Phase orchestrator (gsd-verifier) still needs to run phase-level verification and mark Phase 17 complete — this plan intentionally does NOT mark the phase complete

---
*Phase: 17-buttons*
*Completed: 2026-07-23*

## Self-Check: PASSED

- FOUND: `.planning/phases/17-buttons/17-05-SUMMARY.md` (verified via file existence check)
- FOUND: commit `60b0c23` (verified via `git log --oneline --all | grep 60b0c23`)
- FOUND: commit `eff570f` (verified via `git log --oneline --all | grep eff570f`)
- FOUND: commit `09aa7ad` (verified via `git log --oneline --all | grep 09aa7ad`)
- FOUND: commit `7b2cd58` (verified via `git log --oneline --all | grep 7b2cd58`)
