---
phase: 19-selectors-lists
plan: 01
subsystem: ui
tags: [compose-desktop, kotlin, aero-primitives, list, selection, hover, focus]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "Modifier.aeroSurface / drawAeroSurfaceCore, AeroSurfaceStyle rest()/neutralRest(), hoverLighten()/flattenDisabled() transforms"
  - phase: 17-buttons
    provides: "resolveButtonStyle precedence-chain model this phase's resolver mirrors"
provides:
  - "resolveListItemPillStyle — nullable, base-then-transform (D-11) resolver for AeroListItem's selection/hover pill"
  - "Clipped Aero selection/hover pill on AeroListItem (VLST-01), replacing the unclipped full-bleed .background(animatedBg)"
  - "In-bounds inset focus stroke on AeroListItem (VLST-03), no outer aeroGlowRing (D-13)"
  - "Proven base-then-transform composition shape (D-11) for Plans 02/03 to copy onto AeroSwitch/AeroSegmentedControl"
affects: [19-02-selectors-lists, 19-03-selectors-lists, 19-04-selectors-lists, 20-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Nullable base-then-transform resolver (selected resolves base FIRST, hover transforms SECOND) — structurally prevents the VLST-02 'selection suppresses hover' bug class, no when-branch color pick"
    - "In-bounds inset drawRoundRect focus stroke instead of aeroGlowRing, for components living inside scrolling/clipping containers"
    - "Fail-then-pass source-scan guard discipline (VER-06): guards proven RED against the shipped file before being trusted"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt

key-decisions:
  - "D-11 base-then-transform composition implemented exactly as specified: selectedStyle resolves from `selected` first, hoverLighten() composes on top second — verified byte-for-byte equal via AeroListItemStylesTest"
  - "D-13: in-bounds inset stroke used for focus instead of aeroGlowRing — list rows live inside scrolling/clipping containers that would slice an outer bloom"
  - "P-01/P-04 applied: indication = null on clickable, and disabled dims content via DISABLED_CONTENT_ALPHA on the Row on top of flattenDisabled on the pill (since AeroListItem draws no surface at rest)"

patterns-established:
  - "Pattern: nullable resolver return type (AeroSurfaceStyle?) — the one place this phase's resolver family diverges from the non-null resolveButtonStyle/resolveSliderThumbStyle shape, because 'no pill at rest' is a real state"

requirements-completed: [VLST-01, VLST-02, VLST-03, VLST-04]

coverage:
  - id: D1
    description: "AeroListItem's selection highlight renders as a clipped, gradient+gloss+rim Aero pill (2.dp vertical inset, 6.dp corner radius) via Modifier.aeroSurface, replacing the unclipped full-bleed .background(animatedBg)"
    requirement: "VLST-01"
    verification:
      - kind: unit
        ref: "AeroListItemStylesTest#restSelectedEqualsHalfAlphaAccentRest"
        status: pass
      - kind: unit
        ref: "AeroListItemSourceTest#aeroListItemUsesAeroSurfaceForItsPill"
        status: pass
    human_judgment: true
    rationale: "Visual gradient/gloss/rim rendering quality is a three-theme sign-off concern (Plan 04), not verifiable from unit tests alone"
  - id: D2
    description: "Hovering an already-selected row composes strictly brighter (hoverLighten on top of the selected base) rather than the selection suppressing hover — the VLST-02 structural fix"
    requirement: "VLST-02"
    verification:
      - kind: unit
        ref: "AeroListItemStylesTest#hoverSelectedComposesOnTopOfSelectedBase"
        status: pass
    human_judgment: false
  - id: D3
    description: "A focused clickable row shows an in-bounds inset stroke at colors.borderSelected within the pill's own geometry; no outer aeroGlowRing; a display-only row (onClick == null) gains no focus stop"
    requirement: "VLST-03"
    verification:
      - kind: unit
        ref: "AeroListItemSourceTest#aeroListItemDoesNotUseAeroGlowRing"
        status: pass
    human_judgment: true
    rationale: "Focus-cue legibility across three themes, including when hover is co-active, requires human visual sign-off (deferred to Plan 04)"
  - id: D4
    description: "AeroListItem keeps Modifier.hoverable(interactionSource) + rememberAeroInteractionState as its sole hover/focus source — the reference wiring Plans 02/03 copy"
    requirement: "VLST-04"
    verification:
      - kind: unit
        ref: "AeroListItemSourceTest#aeroListItemKeepsHoverableAndAvoidsRawPointerTracking"
        status: pass
    human_judgment: false
  - id: D5
    description: "resolveListItemPillStyle returns null for the unselected+unhovered row and disabled short-circuits ahead of everything, hover included"
    verification:
      - kind: unit
        ref: "AeroListItemStylesTest#restUnselectedUnhoveredReturnsNull"
        status: pass
      - kind: unit
        ref: "AeroListItemStylesTest#disabledWinsRegardlessOfSelectedOrHovered"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-07-27
status: complete
---

# Phase 19 Plan 01: AeroListItem Aero Restyle Summary

**AeroListItem's selection/hover highlight is now a clipped Aero pill (gradient+gloss+rim via `resolveListItemPillStyle` + `Modifier.aeroSurface`) with an in-bounds focus stroke, and hovering an already-selected row composes strictly brighter through D-11's base-then-transform resolver — proving the tracer shape Plans 02/03 copy onto `AeroSwitch`/`AeroSegmentedControl`.**

## Performance

- **Duration:** 3 min (14:12:43 → 14:15:29 UTC+3, per commit timestamps)
- **Tasks:** 2/2 completed
- **Files modified:** 3 (1 main source, 2 new test files)

## Accomplishments
- Replaced the unclipped, full-bleed `.background(animatedBg)` selection highlight with a clipped Aero pill (`Modifier.aeroSurface`, 2.dp vertical inset, 0.dp horizontal inset, 6.dp corner radius) — VLST-01, D-12.
- Added the nullable `resolveListItemPillStyle(colors, selected, hovered, enabled): AeroSurfaceStyle?` resolver implementing D-11's base-then-transform composition: `selected` resolves the base FIRST, `hovered` transforms it SECOND — structurally eliminating the "selection suppresses hover" bug class (VLST-02).
- Added an in-bounds inset `drawRoundRect` focus stroke at `colors.borderSelected` (no outer `aeroGlowRing` — D-13, list rows live inside scrolling/clipping containers).
- Replaced `collectIsHoveredAsState()` with `rememberAeroInteractionState(interactionSource)` to also collect `focused`; added `indication = null` on `clickable` (P-01).
- Replaced the row-level uniform `.alpha(0.4f)` disabled fade with `flattenDisabled(colors)` on the pill plus a content-only `DISABLED_CONTENT_ALPHA` dim on the Row (P-04 — `AeroListItem` draws no surface at rest, so an unselected+unhovered disabled row needs its own disabled affordance).
- Removed the PRIM-14 `Color.Transparent` violation (the shipped file's line 63).
- `AeroListItemSourceTest` — 6 source-scan guards, each proven to fail (RED) against the shipped, un-restyled file before being trusted, then proven green (GREEN) after the restyle.
- `AeroListItemStylesTest` — pure JVM value-level tests of the resolver's full state matrix across `AeroBlue` (translucent tokens) and `Classic` (fully-opaque tokens), including the load-bearing VLST-02 byte-for-byte proof.

## Task Commits

Each task was committed atomically (both commits were already present on `master` at agent start — this session verified, documented, and finalized the plan rather than re-executing already-completed work):

1. **Task 1: AeroListItem end-to-end — source guard proven RED first, then resolveListItemPillStyle + clipped pill + in-bounds focus stroke, proven GREEN** - `906e044` (feat, tracer)
2. **Task 2: AeroListItemStylesTest — the VLST-02 structural proof plus the nullable/precision/ordering assertions** - `a8579eb` (test, tdd)

**Plan metadata:** commit to follow this SUMMARY (docs: complete plan)

## Files Created/Modified
- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` - Restyled: `resolveListItemPillStyle` resolver, clipped pill via `aeroSurface`, in-bounds focus stroke, `rememberAeroInteractionState`, `indication = null`, `DISABLED_CONTENT_ALPHA` content dim, `Color.Transparent` removed
- `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt` - 6 source-scan regression guards (aeroSurface call, resolver delegation, no `Color.Transparent`, no `aeroGlowRing`, hoverable-not-pointerInput, `indication = null`)
- `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemStylesTest.kt` - Pure JVM value-level tests of `resolveListItemPillStyle`'s full state matrix across AeroBlue/Classic

## Guard Fail-Then-Pass Proof (VER-06)

Per the commit message on `906e044` (the only record available — this session's re-verification ran the guard suite only against the already-restyled file, since the shipped pre-restyle file no longer exists on disk to re-run against):

Against the shipped, un-restyled `AeroListItem.kt`, 4 of 6 guards failed (RED):
- `aeroListItemUsesAeroSurfaceForItsPill` — RED (no `aeroSurface(` call; used `.background(animatedBg)`)
- `aeroListItemDelegatesToTheResolver` — RED (no `resolveListItemPillStyle(`; inline `when` branch instead)
- `aeroListItemDoesNotUseTheFullyTransparentColorConstant` — RED (line 63 used `Color.Transparent`)
- `aeroListItemSuppressesPlatformIndication` — RED (no `indication = null` on the existing `clickable`)

2 of 6 guards passed even against the shipped file (not regressions, already-correct baseline):
- `aeroListItemDoesNotUseAeroGlowRing` — GREEN (shipped file never called `aeroGlowRing`)
- `aeroListItemKeepsHoverableAndAvoidsRawPointerTracking` — GREEN (shipped file already used `.hoverable(` with no `pointerInput`/`awaitPointerEventScope`)

After the restyle (this session, re-verified): all 6 guards GREEN — `./gradlew :library:test --tests "com.mordred.aero.components.list.AeroListItemSourceTest"` exits 0.

## Decisions Made
- D-11's base-then-transform composition implemented exactly as specified in 19-CONTEXT.md/19-UI-SPEC.md — no deviation, no `when`-branch reintroduction.
- D-13's in-bounds focus stroke chosen over `aeroGlowRing`, matching the plan's mandated geometry (inset by half the stroke width so the whole stroke lies inside the pill's clip).
- P-01/P-04 (indication suppression + content-level disabled dim) applied verbatim as recorded in the plan's "Planner decisions" table.

## Deviations from Plan

None — plan executed exactly as written. Both tasks' commits match the plan's task names, file lists, and acceptance criteria verbatim (verified by direct read of `AeroListItem.kt`, `AeroListItemSourceTest.kt`, `AeroListItemStylesTest.kt` against the plan's `<action>`/`<acceptance_criteria>` blocks).

## Issues Encountered

None. This execution session found both plan tasks already implemented and committed on `master` (commits `906e044`, `a8579eb`) prior to this agent's start — likely from an interrupted prior execution run that completed the code/tests but not the SUMMARY/STATE finalization. This session:
1. Read and cross-checked both commits against the plan's full task specifications (source, tests, acceptance criteria) — no gaps found.
2. Re-ran `./gradlew :library:compileKotlin :library:test --tests "com.mordred.aero.components.list.*"` — exit 0.
3. Re-ran the full `./gradlew :library:test` suite for the no-regression check — exit 0.
4. Produced this SUMMARY.md and completed state/roadmap/requirements bookkeeping.

## Known Stubs

None found. Scanned `AeroListItem.kt` for hardcoded empty values, placeholder text, and unwired data sources — the component is a genuine, complete render-path restyle with no stubbed behavior.

## Next Phase Readiness

- The D-11 base-then-transform resolver shape, the fail-then-pass source-guard discipline, and the in-bounds-focus-stroke pattern are all proven end-to-end and ready for Plan 02 (`AeroSwitch`) and Plan 03 (`AeroSegmentedControl`) to copy.
- `resolveListItemPillStyle`'s nullable-return shape is the one documented divergence from the non-null `resolveButtonStyle`/`resolveSliderThumbStyle` precedent — Plans 02/03 should NOT copy the nullability, only the base-then-transform composition order.
- Visual/legibility verification of the pill and the focus stroke across all three themes is deferred to Plan 04's formal sign-off, per the plan's flagged assumptions for VLST-02/VLST-03 (unclassified edge-probe rows).
- No blockers.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27*

## Self-Check: PASSED

All created/modified files found on disk; all task and metadata commits (`906e044`, `a8579eb`, `f8ca08c`) found in git history.
