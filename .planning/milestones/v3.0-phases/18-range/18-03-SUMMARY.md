---
phase: 18-range
plan: 03
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, progress-bar, drawWithCache, blend-mode, source-scan-guards]

# Dependency graph
requires:
  - phase: 18-range
    provides: "AeroSurfaceStyle.neutralRest(base, cornerRadius) factory, Modifier.aeroGroove/Modifier.aeroSurface Box-owning exposures, drawAeroSurfaceCore (Plan 01)"
provides:
  - "AeroProgressBar (both overloads) fully restyled onto the Aero primitives layer — recessed neutral aeroGroove bed under an accent two-tone-glass aeroSurface fill with a static top gloss ON by default (VRNG-06, D-05)"
  - "showRunningSheen: Boolean = false — new additive optional parameter on the determinate overload; when true, an animated traveling-highlight overlay layers on top of the always-on static gloss via its own drawWithCache, never replacing it (VRNG-07, D-05)"
  - "Indeterminate sweep restyled to an accent aeroSurface segment with a BlendMode.DstIn horizontal edge-fade mask (D-06), fading to baseColor.copy(alpha = 0f) never Color.Transparent (PRIM-14); exact 1500ms LinearEasing/RepeatMode.Restart timing preserved verbatim (VRNG-08 — no ping-pong)"
  - "AeroProgressBarSourceTest.kt — comment-stripping source-scan guard locking the timing/mode/default invariants, proven non-inert via a documented fail-then-pass proof for all three guarded violations"
affects: [19-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Animated-sweep-via-layout-offset, not per-frame Brush rebuild: both the indeterminate segment and the new running-sheen overlay read their animated float only to compute an outer Modifier.offset(x = ...); every Brush.horizontalGradient is built inside drawWithCache's cached scope (keyed on size/style only), never depending on the animated value directly (PRIM-13) — the pre-existing indeterminate code already did this for its offset; this plan extends the same idiom to the running-sheen overlay and the new edge-fade mask"
    - "BlendMode.DstIn horizontal alpha mask over an already-painted aeroSurface fill — the first place in this codebase a fill's edges are faded by a SEPARATE overlay draw rather than by the primitive's own gradient, since drawAeroSurfaceCore's built-in gradient is vertical-only (16-RESEARCH.md Assumption A4)"
    - "Comment-stripping source-scan guard (regex-strip /* */ and // before content assertions) — needed because this file's own KDoc intentionally documents the banned Color.Transparent/RepeatMode.Reverse tokens by name, which would otherwise self-fail a naive assertFalse(source.contains(...)) guard"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt

key-decisions:
  - "AeroProgressBar has no enabled/disabled state today (verified: no `enabled` identifier anywhere in the pre-restyle source) and the plan's must_haves explicitly forbid new public API surface beyond showRunningSheen — the plan's own action text mentioning a `!enabled` flattenDisabled branch was therefore treated as inapplicable generic guidance, not implemented; UI-SPEC.md's Disabled row for the accent fill/groove explicitly scopes to AeroSlider/AeroRangeSlider only, not AeroProgressBar, confirming this reading"
  - "Running-sheen restart duration set to 1800ms (RUNNING_SHEEN_DURATION_MS), deliberately distinct from the indeterminate's locked 1500ms, so the two animations never read as visually identical when both happen to be on-screen in the same showcase view (Claude's discretion per D-05/VRNG-07)"
  - "Indeterminate edge-fade implemented as a BlendMode.DstIn Brush.horizontalGradient overlay chained after aeroSurface(...) rather than modifying drawAeroSurfaceCore itself — keeps the shared primitive untouched (Phase 16 architecture boundary) and localizes the D-06 horizontal-fade need to the one component that has it"

requirements-completed: [VRNG-06, VRNG-07, VRNG-08]

coverage:
  - id: D1
    description: "AeroProgressBar's determinate mode shows a recessed neutral aeroGroove bed under an accent two-tone-glass aeroSurface fill with a static top gloss ON by default"
    requirement: "VRNG-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt#fillNoLongerUsesFlatBackgroundColorPrimary"
        status: pass
    human_judgment: true
    rationale: "The source-scan guard proves the flat .background(colors.primary) path was removed and aeroGroove(/aeroSurface( are now used, but the actual raised/recessed/glossy visual read on the pill-shaped 8dp bar across AeroBlue/AeroDark/Classic is a visual judgment not exercised by this autonomous plan (no checkpoint task was present in 18-03-PLAN.md) — flagged for the phase-level three-theme showcase sign-off in a later plan/checkpoint per 18-CONTEXT.md's acceptance framing."
  - id: D2
    description: "The periodic running sheen is a separate optional parameter (showRunningSheen: Boolean = false) layered on top of the always-on static gloss, never a replacement"
    requirement: "VRNG-07"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt#showRunningSheenParamDefaultsFalse"
        status: pass
      - kind: unit
        ref: "./gradlew :library:compileKotlin (confirms RunningSheenOverlay compiles as an additive overlay inside the fill Box, gated by the showRunningSheen boolean, never replacing the aeroSurface fill call)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The indeterminate AeroProgressBar sweeps a single accent-glass segment left-to-right over the recessed bed at the exact existing 1500ms LinearEasing RepeatMode.Restart timing, never RepeatMode.Reverse (no ping-pong)"
    requirement: "VRNG-08"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt#indeterminateKeepsThe1500msRestartTimingWithNoPingPong"
        status: pass
    human_judgment: false

# Metrics
duration: 7min
completed: 2026-07-24
status: complete
---

# Phase 18 Plan 03: AeroProgressBar Determinate/Indeterminate Restyle + Running Sheen Summary

**AeroProgressBar (both overloads) restyled onto the Phase 16 Aero primitives — recessed `aeroGroove` bed under an accent `aeroSurface` glass fill, a new default-off `showRunningSheen` traveling-highlight overlay, and a `BlendMode.DstIn` horizontal edge-fade mask on the indeterminate sweep — with the exact 1500ms restart timing preserved and a comment-stripping source-scan guard proven non-inert.**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-07-24T12:40:31+03:00 (previous plan's completion commit)
- **Completed:** 2026-07-24T12:47:32+03:00 (Task 2 commit)
- **Tasks:** 2 (Task 1 restyle + running sheen, Task 2 source-scan guard with fail-then-pass proof)
- **Files modified:** 2 (1 modified, 1 created)

## Accomplishments
- Determinate `AeroProgressBar`'s flat `Box.background(colors.surface, ...)` bed and `Box.background(colors.primary, ...)` fill both replaced: bed → `Modifier.aeroGroove(AeroSurfaceStyle.neutralRest(colors, cornerPx))`, fill → `Modifier.aeroSurface(AeroSurfaceStyle.rest(colors, cornerPx), RoundedCornerShape(cornerPx))`. A single `val cornerPx = height / 2` feeds both the clip Shape and every `AeroSurfaceStyle`'s `cornerRadius` field (Pitfall 5 — no independently-derived radii).
- New `showRunningSheen: Boolean = false` parameter on the determinate overload — when `true`, `RunningSheenOverlay` draws an animated traveling highlight (1800ms restart, distinct from the indeterminate's 1500ms) on top of the fill's always-on static gloss, never replacing it.
- Indeterminate sweep restyled from a flat sliding `Box.background(colors.primary)` to an accent `aeroSurface` segment, with a new `BlendMode.DstIn` `Brush.horizontalGradient` overlay masking the segment's leading/trailing edges to `baseColor.copy(alpha = 0f)` — the segment's own bed underneath is now `Modifier.aeroGroove(neutralRest(...))` instead of a flat `.background(colors.surface)`.
- The indeterminate's `infiniteRepeatable(tween(1500, LinearEasing), RepeatMode.Restart)` timing kept byte-for-byte unchanged — verified unchanged by both direct read and the new source-scan guard.
- Both the indeterminate sweep and the new running-sheen overlay build their `Brush` once inside `drawWithCache`'s cached scope (keyed on size/style only); the animated float value is read solely to compute an outer `Modifier.offset(x = ...)`, so no per-frame Brush rebuild happens on either animated path (PRIM-13/Pitfall 6).
- `AeroProgressBarSourceTest.kt` (4 tests) added — a comment-stripping source-scan guard (strips `//` and `/* */`/`/** */` blocks before content assertions, since this file's own KDoc intentionally names the banned tokens) locking: `RepeatMode.Restart` + `1500` present, `RepeatMode.Reverse` absent, `showRunningSheen: Boolean = false` present, `Color.Transparent` absent from real code, and `.background(colors.primary` fully removed in favor of `aeroGroove(`/`aeroSurface(`.
- Full `:library:test` suite green throughout (including the pre-existing `AeroRangeSliderTest`, `AeroSliderStylesTest`, `AeroSliderSourceTest`, `AeroRangeSliderDragLogicUntouchedTest`), no regression.

## Task Commits

Each task was committed atomically:

1. **Task 1: Restyle AeroProgressBar determinate + indeterminate; add default-off running sheen** - `d5d52ba` (feat)
2. **Task 2: AeroProgressBarSourceTest guard (timing/mode/default invariants) with fail-then-pass proof** - `09afbfc` (test)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/range/AeroProgressBar.kt` - Both overloads restyled onto `aeroGroove`/`aeroSurface`; new `showRunningSheen` param + `RunningSheenOverlay`; indeterminate `BlendMode.DstIn` edge-fade mask; timing/mode unchanged
- `library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt` - New comment-stripping source-scan guard (4 tests) for VRNG-06/07/08 invariants

## Decisions Made
- No `enabled`/disabled handling added to `AeroProgressBar` — the component has never had an `enabled` parameter (confirmed by direct pre-restyle read) and the plan's own `must_haves.prohibitions` forbid any public-signature change beyond `showRunningSheen`; UI-SPEC.md's Disabled row for the accent fill/groove is scoped to `AeroSlider`/`AeroRangeSlider` only, not `AeroProgressBar`. The plan's Task 1 action text mentioning a `!enabled` branch was treated as inapplicable generic guidance rather than an instruction to add new API surface.
- Running-sheen restart duration set to 1800ms, deliberately distinct from the indeterminate's locked 1500ms so the two motions read as visually distinct animations rather than duplicates.
- Indeterminate edge-fade implemented as a `BlendMode.DstIn` overlay chained after `.aeroSurface(...)` (a new technique for this codebase, not a `drawAeroSurfaceCore` change) — keeps the shared Phase 16 primitive untouched and localizes the horizontal-fade need to the one component that requires it, per Assumption A4.

## Deviations from Plan

None - plan executed exactly as written, with the one clarification above (no `enabled` param added, since none existed and adding one was prohibited by the plan's own constraints).

## Issues Encountered

None. IDE-reported diagnostics on first save (`INLINE_FROM_HIGHER_PLATFORM`, `INCOMPATIBLE_CLASS` against `kotlin-stdlib-2.4.10`) were confirmed to be a stale IDE Kotlin-plugin mismatch (IDE bundled Kotlin 2.1.0 vs. the project's actual pinned Kotlin 2.4.10 toolchain, per STATE.md's Phase 15 record) — the real `./gradlew :library:compileKotlin` build was green from the first attempt, confirming these were IDE-only false positives, not real compile errors.

## Guard Fail-Then-Pass Proof

Per this project's own v2.0.3 false-positive-sign-off lesson (repro-must-exercise-the-path, VER-06/Pitfall 8), all three of `AeroProgressBarSourceTest`'s guarded invariants were demonstrated to FAIL against deliberately-reintroduced violations via temporary local edits — each reverted (confirmed via `git diff --stat` showing zero changes) before Task 2's commit.

**Timing/no-ping-pong guard (`indeterminateKeepsThe1500msRestartTimingWithNoPingPong`):**
- Temporarily changed the indeterminate's `repeatMode = RepeatMode.Restart` to `RepeatMode.Reverse`.
- Re-ran `AeroProgressBarSourceTest`: **4 tests completed, 1 failed** — isolated to this test, `AssertionFailedError` at the `RepeatMode.Reverse` absence assertion.
- Reverted; re-ran: 4/4 passed.

**`showRunningSheen` default-false guard (`showRunningSheenParamDefaultsFalse`):**
- Temporarily flipped `showRunningSheen: Boolean = false` to `= true`.
- Re-ran `AeroProgressBarSourceTest`: **4 tests completed, 1 failed** — isolated to this test.
- Reverted; re-ran: 4/4 passed.

**No-`Color.Transparent` guard (`noColorTransparentAntiPatternInRealCode`):**
- Temporarily replaced the indeterminate edge-mask's first gradient stop with `androidx.compose.ui.graphics.Color.Transparent` (real code, not a comment).
- Re-ran `AeroProgressBarSourceTest`: **4 tests completed, 1 failed** — isolated to this test.
- Reverted; re-ran: 4/4 passed.

All three proofs used the exact same guard test file that ships in `09afbfc` — no guard code was authored differently between the fail-demonstration and the final commit. Full `:library:test` suite confirmed green after every revert, and `git diff --stat` confirmed `AeroProgressBar.kt` had zero net changes from its committed state before Task 2's commit.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- All three Range components (`AeroSlider` Plan 01, `AeroRangeSlider` Plan 02, `AeroProgressBar` Plan 03) are now restyled onto the shared Aero primitives layer — Phase 18's component-level work is complete pending Plan 04 (showcase + three-theme visual sign-off, per `18-04-PLAN.md`).
- The `BlendMode.DstIn` horizontal edge-fade technique introduced here (first use in this codebase) is available as a reusable idiom for any future component needing a horizontal-only fade over an otherwise vertical-gradient primitive.
- Human three-theme visual sign-off for `AeroProgressBar`'s determinate/indeterminate/running-sheen read (D1's `human_judgment: true` coverage entry above) is deferred to Plan 04's showcase checkpoint, consistent with this plan's `autonomous: true` frontmatter (no checkpoint task was present in `18-03-PLAN.md` itself).
- No blockers. Full `:library:test` suite green after this plan's changes.

---
*Phase: 18-range*
*Completed: 2026-07-24*

## Self-Check: PASSED

All created/modified files (`AeroProgressBar.kt`, `AeroProgressBarSourceTest.kt`, this SUMMARY) and both task commits (`d5d52ba`, `09afbfc`) verified present on disk / in git history.
