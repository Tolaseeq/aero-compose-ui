---
phase: 17-buttons
plan: 04
subsystem: ui
tags: [compose-desktop, kotlin, aero-theme, buttons, showcase]

# Dependency graph
requires:
  - phase: 17-buttons
    plan: 01
    provides: "AeroButtonSurface shared surface + filled AeroButton restyled, showcase row wired with 'Save Changes' demo label"
  - phase: 17-buttons
    plan: 03
    provides: "AeroOutlinedButton as a thin wrapper over AeroButtonSurface(outlined = true), Role.Button + Space/Enter keyboard activation proven"
provides:
  - "ButtonsSection.kt demo rows for both AeroButton and AeroOutlinedButton covering enabled, disabled, and a width-constrained long-label truncation example"
  - "Live-state review note (bodySmall) documenting hover/press/focus are exercised via the theme switcher, mouse-over, click-and-hold, and Tab"
affects: [17-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Width-constrained demo instance (Modifier.width(120.dp)) is the mechanism to visibly exercise maxLines=1/TextOverflow.Ellipsis truncation in a showcase row, since AeroButtonSurface's Box has no width constraint of its own and otherwise grows to fit the label"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt

key-decisions:
  - "No forced-state static styling was added for hover/press/focus — the desktop showcase's live AeroBlue/AeroDark/Classic theme switcher plus real mouse/keyboard makes these transient states genuinely reviewable without bypassing resolveButtonStyle's real state-resolution path (must_haves prohibition honored)"
  - "Long-label demo text constrained via modifier = Modifier.width(120.dp) rather than any change to the button's own locked height/contentPadding/corner radius — the truncation cue is exercised purely through the pre-existing maxLines=1/overflow=Ellipsis Text call inside AeroButtonSurface"

requirements-completed: [VBTN-02, VBTN-05]

coverage:
  - id: D1
    description: "ButtonsSection demonstrates both AeroButton and AeroOutlinedButton in every reviewable state (enabled, disabled, long-label truncation statically; hover/press/focus documented as live-reviewed) across all three themes via the existing live theme switcher"
    requirement: "VBTN-02"
    verification:
      - kind: integration
        ref: "./gradlew :showcase:compileKotlin"
        status: pass
      - kind: manual
        ref: "source assertion: AeroButton row renders enabled + disabled + long-label; AeroOutlinedButton row renders enabled + disabled + long-label"
        status: pass
    human_judgment: true
    rationale: "Compile-time proof that the demo rows exist and build; the actual three-theme x five-state visual review is the 17-05 human sign-off gate this plan exists to prepare for, per the phase's validation architecture."
  - id: D2
    description: "AeroOutlinedButton's outlined-variant treatment is demonstrated in the showcase alongside the filled AeroButton, ready for the fixed-delta visual comparison at sign-off"
    requirement: "VBTN-05"
    verification:
      - kind: integration
        ref: "./gradlew :showcase:compileKotlin"
        status: pass
    human_judgment: true
    rationale: "The algebraic outlined-delta guarantee was already proven by unit test in 17-03 (AeroOutlinedButtonStylesTest); this plan's job is only to make the result visible for eyes-on review, not to re-verify the delta."

# Metrics
duration: 8min
completed: 2026-07-23
status: complete
---

# Phase 17 Plan 04: Showcase State-Matrix Demo Rows Summary

**`ButtonsSection.kt`'s AeroButton and AeroOutlinedButton rows now each render enabled, disabled, and a width-constrained long-label example (visibly exercising the existing `maxLines = 1`/`TextOverflow.Ellipsis` truncation), plus a header note documenting that hover/press/focus are reviewed live via the theme switcher — no forced-state fake styling, no layout-lock changes.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-07-23T14:16:23Z (after 17-03 plan-metadata commit 986a779)
- **Completed:** 2026-07-23T14:19:30Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- `ButtonsSection.kt` "AeroButton" row extended from enabled+disabled to enabled ("Save Changes") + disabled + a width-constrained (`Modifier.width(120.dp)`) long-label instance ("Save All Changes and Close This Dialog Window") that visibly truncates via the `AeroButtonSurface`-internal `maxLines = 1`/`TextOverflow.Ellipsis` (shipped in 17-01/17-02, unmodified this plan)
- "AeroOutlinedButton" row extended identically: enabled ("Cancel") + disabled + a long-label instance ("Cancel All Pending Operations Immediately")
- Added a `bodySmall` header note (mirroring the existing `LayoutSection.kt` note-text precedent) stating hover/press/focus are reviewed live via the theme switcher: mouse-over for hover, click-and-hold for press, Tab for focus
- No forced/fake per-state styling added — every row still resolves through the real `resolveButtonStyle` path; no `RoundedCornerShape`/`height`/`contentPadding` override introduced (verified via `grep`, zero matches)

## Task Commits

Each task was committed atomically:

1. **Task 1: State-matrix demo rows for both button variants** - `ca8adcd` (feat)

**Plan metadata:** pending (this commit)

## Files Created/Modified
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` - "AeroButton"/"AeroOutlinedButton" rows extended with disabled + width-constrained long-label examples; added a `bodySmall` live-state review note above both rows

## Decisions Made
- No forced-state static styling (fake "hovered"/"pressed" look) was added — the plan's `must_haves.prohibitions` explicitly forbids bypassing `resolveButtonStyle`'s real state resolution, and the desktop showcase already has a live three-theme switcher plus a real mouse/keyboard, making transient-state review genuinely possible without a hack
- The long-label truncation demo needed an explicit `Modifier.width(120.dp)` on the button instance — `AeroButtonSurface`'s `Box` has no width constraint of its own (it wraps content by default), so without a width cap the long label would simply widen the button instead of truncating; this is a demo-row-only modifier, not a change to the component's locked defaults

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- Same stale-IDE-diagnostic pattern noted in 17-01/02/03: the IDE's language server reported `INLINE_FROM_HIGHER_PLATFORM` (JVM target 11 vs 1.8) errors on the edited lines immediately after the `Edit` call. Confirmed non-issue again — `./gradlew :showcase:compileKotlin` ran clean with no errors, consistent with prior plans' finding that this is a stale IDE plugin/Kotlin-metadata mismatch unrelated to the actual Gradle Kotlin 2.4.10 toolchain.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The showcase now has concrete, honest demo rows for both restyled button variants covering every statically-reviewable state (enabled/disabled/long-label truncation), with hover/press/focus explicitly documented as live-reviewed — 17-05 can proceed directly to the mandatory three-theme × five-state human sign-off with no further showcase wiring needed
- No blockers.

---
*Phase: 17-buttons*
*Completed: 2026-07-23*

## Self-Check: PASSED

- FOUND: showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt (modified, verified via Read)
- FOUND: commit ca8adcd (verified via `git log --oneline --all | grep ca8adcd`)
