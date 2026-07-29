---
phase: 20-verification
plan: 08
subsystem: testing
tags: [compose-ui-test, gradle, verification, focus-visible, contrast, density]

# Dependency graph
requires:
  - phase: 20-verification
    plan: 04
    provides: "resolveLabelColor (shared label-contrast mechanism) that VER-08 samples across the selection animation"
  - phase: 20-verification
    plan: 05
    provides: "WR-02 finding (AeroIconButton never converted to focusVisible) and its fix, 20-05's commit 61cab14 — this plan adds the missing guard"
  - phase: 20-verification
    plan: 06
    provides: "no direct dependency on VER-05's artifact; ordering only (wave 5 depends_on 20-04/05/06)"
provides:
  - "VER07ButtonStateMatrixTest — 8 tests: 5-state x 2-button x 3-scheme differentiation matrix, plus 2 D-08 fixture-proof tests"
  - "VER08SegmentLabelFlipTest — 5 tests: manual-clock label-flip sampling across 3 schemes, plus 2 D-08 fixture-proof tests"
  - "AeroIconButtonFocusVisibleWiringTest — 2 tests: both halves of the WR-02 focus-ring rule (mouse silent, keyboard rings)"
  - "VER09FractionalDensityRoundingTest — 2 tests: 1dp-contour survival at Density(1.25f)/Density(2f), plus its own D-08 fixture proof"
  - "17 new permanent Compose UI-test gates (435 -> 452 tests, 0 failures), zero production change"
affects: ["20-07 (human three-theme sign-off — inherits these four gates as already-closed mechanical checks; the sign-off's own scope narrows accordingly, but is NOT reduced to a formality)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "mainClock.autoAdvance = false + manual frame stepping to sample an animating value deterministically, introduced to this codebase for the first time by VER08 (no prior test used autoAdvance)"
    - "Every gate in this plan carries its own in-file D-08 fail-then-pass fixture proof, following VER01GradientProportionalitySourceTest's convention — the comparison itself is proven capable of failing before it is trusted to pass"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt
    - library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroIconButtonFocusVisibleWiringTest.kt
    - library/src/test/kotlin/com/mordred/aero/verification/VER09FractionalDensityRoundingTest.kt
  modified: []

key-decisions:
  - "Focus in VER07 is driven by requestFocus() issued BEFORE any hover/press interaction, so pointerAcquired stays false and the captured state matches real keyboard traversal rather than a mouse-then-focus hybrid that no real user path produces."
  - "VER08 asserts the label changes colour AT MOST ONCE across the animation, not exactly-zero or exactly-one — the flip itself is real and expected (resolveLabelColor derives from the animating fill); an OSCILLATING label is the actual defect, since at ~150ms the eye cannot distinguish a flip from a flicker."
  - "VER09 deliberately asserts presence-and-singleness of the 1dp contour (>= 1px, <= scaled width + 1px for AA) rather than absolute pixel counts, because absolute counts legitimately change at 1.25x/2x and asserting them would produce a gate that fails for the wrong reason."
  - "VER09 also re-checks VER-03's pinned 28.dp button height reproduces within 0.05dp after fractional rounding, tying this gate back to the phase's existing snapshot baseline instead of introducing an unrelated new size assumption."

patterns-established:
  - "D-08 fail-then-pass fixture proof is now present in all four of this plan's gates, extending the convention beyond VER-01/02/03 to every gate added in Phase 20's later waves."

requirements-completed: []
# NOTE: this plan's own frontmatter lists requirements: [SHW-16, VER-06]. Neither is marked
# complete here — see "Requirements Note" below. This is a deliberate deviation from the
# default "copy all requirement IDs" instruction, per this plan's own closeout directive.

coverage:
  - id: D1
    description: "Per-state matrix gate: AeroButton and AeroOutlinedButton, 5 states (default/hover/press/focus/disabled) x 3 schemes (AeroBlue/AeroDark/Classic), each state asserted to differ from that component's own default where the design requires a difference; driven by real performMouseInput/requestFocus, never a hand-built InteractionSource"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt#aeroButtonAeroBlueStateMatrix, #aeroButtonAeroDarkStateMatrix, #aeroButtonClassicStateMatrix, #aeroOutlinedButtonAeroBlueStateMatrix, #aeroOutlinedButtonAeroDarkStateMatrix, #aeroOutlinedButtonClassicStateMatrix, #pixelMapsDifferDetectsGenuinelyDifferentCaptures, #pixelMapsDifferReturnsFalseOnIndistinguishableCaptures"
        status: pass
    human_judgment: false
  - id: D2
    description: "Segment label-flip gate: mainClock.autoAdvance = false, label colour sampled frame-by-frame across the selection animation in all three schemes, asserted to change colour AT MOST ONCE with a non-zero sampled-frame count"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt#labelFlipsAtMostOnceAcrossSelectionAnimationInAeroBlue, #labelFlipsAtMostOnceAcrossSelectionAnimationInAeroDark, #labelFlipsAtMostOnceAcrossSelectionAnimationInClassic, #countLabelColorChangesFlagsAnOscillatingSequence, #countLabelColorChangesAcceptsAMonotoneSequence"
        status: pass
    human_judgment: false
  - id: D3
    description: "AeroIconButton focusVisible wiring gate (WR-02): asserts BOTH halves — mouse click leaves no focus ring, keyboard traversal produces one — through the component's own rememberFocusVisible path, not a re-implemented model"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/components/buttons/AeroIconButtonFocusVisibleWiringTest.kt#mouseClickDrawsNoRingButKeyboardTraversalDoes, #mouseClickShapedSequenceIsSuppressedByVisibleButWouldRingOnPlainFocused"
        status: pass
    human_judgment: false
  - id: D4
    description: "Fractional-density rounding gate at Density(1.25f) and Density(2f) vs Density(1f) baseline: 1dp contour asserted present-and-single (not collapsed to zero, not doubled) via a theme-independent pixel-run counter; VER-03's pinned 28.dp height re-checked to reproduce within 0.05dp"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER09FractionalDensityRoundingTest.kt#oneDpBorderContourAndPinnedHeightSurviveFractionalDensity, #borderRunLengthPxDetectsPresenceCollapseAndDoubling"
        status: pass
    human_judgment: false
  - id: D5
    description: "Cross-component aesthetic coherence — whether all eight components read as ONE material family across AeroBlue/AeroDark/Classic — is NOT machine-checkable and was deliberately not attempted by any test in this plan. It remains fully open, owned by 20-07."
    verification: []
    human_judgment: true
    rationale: "A test can assert two components share a gloss constant or a fill formula; it cannot assert the composed result LOOKS like one coherent material family. This judgment is exactly why SHW-16 exists, and this plan does not reduce it to a formality."
  - id: D6
    description: "Whether AeroDark's authorized 4.079 recessed-segment contrast (below the nominal 4.5:1 WCAG target, explicitly authorized in 20-04) is acceptable to a human eye in practice"
    verification: []
    human_judgment: true
    rationale: "The measurement is already gated by AeroButtonContrastRegressionTest (20-04); the open question is precisely the non-measurable perceptual remainder — a number below a threshold is not the same question as 'does this read legibly to a person.'"
  - id: D7
    description: "Real OS DPI behavior (Windows compositor fractional scaling of the final surface, Skia rasterisation at that scale, font hinting) — VER09's LocalDensity substitution only simulates density in Compose LAYOUT and does not exercise any of these three things"
    verification: []
    human_judgment: true
    rationale: "LocalDensity substitution is a layout-math simulation, not a real fractional-DPI render; the gap between the two is recorded in the test's own KDoc per this plan's action item, and only a human running the real app at a real non-100% Windows scale (20-07's two AeroBlue passes) can close it."

duration: n/a (closeout for a completed autonomous plan; no live task execution in this session)
completed: 2026-07-29
status: complete
---

# Phase 20 Plan 08: Compose Test-API Gates for the Mechanical Half of SHW-16 Summary

> **⚠ Every contrast figure in this document is stale — do not quote any of them.** The
> AeroDark recessed-segment exception it cites as **4.079** was voided when plan 20-09 retired the
> per-fill `resolveLabelColor` algorithm the number depended on. Its first replacement (the
> surface-polarity rule's 3.218 / 3.538) was itself voided hours later when the maintainer reviewed
> that rule running, rejected it on appearance, and directed white on both polarities in AeroBlue
> and AeroDark.
>
> This figure has now gone stale three times. **The live numbers live in code, not in prose:**
> `AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`, which pins and
> regression-bounds every accepted case. Read them from there. Narrative context is in
> `20-09-SUMMARY.md`; the sign-off position is row C2b of `20-SIGNOFF.md`.
>
> Also note `VER08SegmentLabelFlipTest`, described below as asserting "the label changes at most
> once across the selection animation", was **strengthened in `34ff18f`** to assert the label colour
> **never** changes — across the animation and across hover, press, focus and disabled. The gates
> themselves stand; only that one assertion got stronger.

**Four new permanent Compose UI-test gates (per-state matrix, segment label-flip, AeroIconButton focus wiring, fractional-density contour rounding) convert the mechanically-checkable slice of the SHW-16 checklist into re-executing tests — 435 -> 452 tests, 0 failures, zero production change — while leaving the genuinely human-only remainder of SHW-16 untouched and unclosed.**

## Performance

- **Tasks:** 4/4 (all four previously executed and committed; this session is a closeout writing SUMMARY/STATE/ROADMAP only, no re-execution)
- **Files created:** 4 (all under `library/src/test/kotlin/`)
- **Files modified:** 0 under `library/src/main` or `showcase/` — confirmed empty diff (`git diff 6474569..HEAD -- library/src/main showcase`)
- **Suite:** 435 -> 452 tests, 0 failures (+17 tests, matching this plan's 8+5+2+2 test-method count exactly)

## Accomplishments

1. **VER07ButtonStateMatrixTest** (`2dc046e`) — 8 tests. Composes `AeroButton` and `AeroOutlinedButton` under AeroBlue, AeroDark and Classic and drives all five states through real input: default (untouched), hover (`performMouseInput { moveTo(center) }`), press (held pointer), focus (`requestFocus()` issued BEFORE any hover/press, so `pointerAcquired` stays false and the captured state matches real keyboard traversal, not a hand-built `InteractionSource`), disabled (`enabled = false`). Each state is asserted to differ from that component's own default in that same scheme — 6 per-scheme-per-component matrix tests plus 2 D-08 fixture-proof tests (`pixelMapsDifferDetectsGenuinelyDifferentCaptures`, `pixelMapsDifferReturnsFalseOnIndistinguishableCaptures`) proving the comparison itself can fail.

2. **VER08SegmentLabelFlipTest** (`a9c1c50`) — 5 tests. Because `resolveLabelColor` derives the segment label from the animating fill, the label is expected to flip once as the fill crosses the contrast midpoint during the selection animation — a real, expected transition, not a defect. `mainClock.autoAdvance = false` (new to this codebase; no prior test used it) drives 15 manual frame steps per scheme, sampling the rendered label pixel nearest either luminance extreme at each step (safe because `resolveLabelColor` returns only pure black or pure white). Asserts the colour changes **AT MOST ONCE** — an OSCILLATING label is the actual defect, since at ~150ms the eye cannot tell a genuine flip from a flicker. Live-verified during authoring: AeroBlue genuinely flips black->white mid-animation while AeroDark and Classic stay constant, proving the sampler reads real animation state rather than passing vacuously. Non-zero sampled-frame-count is also asserted so a clock that never advanced cannot pass silently. 3 per-scheme tests + 2 D-08 fixture-proof tests (`countLabelColorChangesFlagsAnOscillatingSequence`, `countLabelColorChangesAcceptsAMonotoneSequence`).

3. **AeroIconButtonFocusVisibleWiringTest** (`7532b59`) — 2 tests, closing WR-02 (found in 20-05, fixed in commit 61cab14, previously unguarded). Asserts BOTH halves through the component's real `rememberFocusVisible` path on its actual `interactionSource`: a mouse click leaves NO focus ring (`mouseClickDrawsNoRingButKeyboardTraversalDoes`), and keyboard traversal DOES produce one. Asserting only the keyboard half would have passed on the original bug (a ring on every interaction) — both halves are required for the guard to mean anything.

4. **VER09FractionalDensityRoundingTest** (`a4b9505`) — 2 tests. Composes the restyled components at `Density(1f)`, `Density(1.25f)` and `Density(2f)` and measures the 1dp contour via a self-referencing, theme-independent pixel-run counter, asserting it stays present-and-single (>= 1px, <= scaled width + 1px for AA) rather than asserting absolute pixel counts (which legitimately shift at fractional scale and would produce a gate failing for the wrong reason). Also re-checks VER-03's pinned 28.dp button height reproduces within 0.05dp after fractional rounding. `borderRunLengthPxDetectsPresenceCollapseAndDoubling` is the D-08 proof that the counter itself rejects a collapsed or doubled contour. **The test's own KDoc records, in the test file itself and not only in this SUMMARY:** `LocalDensity` substitution simulates density in LAYOUT only — it does not exercise the Windows compositor's fractional scaling of the final surface, Skia rasterisation at that scale, or font hinting.

## The Boundary — What This Plan Does NOT Close

**This is stated with equal prominence to the accomplishments above, and must not be read as a footnote.** This plan does not close SHW-16 and does not reduce it to a formality. Three things remain genuinely human and stay entirely with plan 20-07:

1. **Cross-component aesthetic coherence** — whether all eight restyled components read as ONE material family across AeroBlue, AeroDark and Classic. A test can assert that two components share a gloss constant or a fill-darken formula; it cannot assert that the composed visual result *looks* like one coherent family to a human eye. This is the reason SHW-16 exists at all, and no test in this plan attempts it.

2. **The AeroDark authorized-exception contrast judgment** — whether the recessed segment's authorized 4.079:1 contrast (below the nominal 4.5:1 WCAG target, explicitly authorized in Plan 20-04) reads as legible in practice. The *measurement* is already gated by `AeroButtonContrastRegressionTest`; the open question this plan cannot touch is the perceptual remainder — a number below a threshold is a different question from "does a person reading this actually see it clearly."

3. **Real OS DPI** — `LocalDensity` substitution simulates density in Compose LAYOUT only. It does not exercise the Windows compositor's fractional scaling of the final rendered surface, Skia rasterisation at that scale, or font hinting. VER09's own KDoc records this limit in the test file itself, per this plan's explicit instruction, so nobody reading the gate mistakes it for covering real OS DPI. Only 20-07's two live AeroBlue passes at exactly 125% and exactly 200% on the real running app can answer this.

**A reader of this SUMMARY must not come away thinking the human sign-off in 20-07 is now optional or reduced to rubber-stamping.** All four gates in this plan narrow what 20-07's checklist has to verify by eye and by hand — they do not replace any of it. VER-06 ("every new gate proven to fail on unfixed code before being trusted") is further reinforced by this plan's four D-08 fixture proofs; its existing completed state in `REQUIREMENTS.md` is left unchanged.

## Task Commits

Each task was committed atomically (all four previously executed and committed prior to this closeout):

1. **Task 1: Per-state matrix gate for AeroButton and AeroOutlinedButton across all three schemes** — `2dc046e` (test)
2. **Task 2: Segment label-flip gate over the selection animation** — `a9c1c50` (test)
3. **Task 3: AeroIconButton focusVisible wiring gate (WR-02)** — `7532b59` (test)
4. **Task 4: Fractional-density rounding gate at 125% and 200%** — `a4b9505` (test)

**Plan metadata:** `6474569` (docs: plan Compose test-API gates), committed before task execution began.

**This closeout:** committed immediately after this SUMMARY (see repository history) — `docs(20-08): complete Compose test-API gate plan`.

## Files Created/Modified

- `library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt` — 273 lines, 8 tests: per-state x per-scheme differentiation matrix for both buttons
- `library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt` — 227 lines, 5 tests: manual-clock label-flip sampling across three schemes
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroIconButtonFocusVisibleWiringTest.kt` — 167 lines, 2 tests: both halves of the WR-02 focus-ring rule
- `library/src/test/kotlin/com/mordred/aero/verification/VER09FractionalDensityRoundingTest.kt` — 196 lines, 2 tests: 1dp contour survival at fractional density, KDoc-recorded LocalDensity-simulation limit

No file under `library/src/main` or `showcase/` was touched. Confirmed: `git diff 6474569..HEAD -- library/src/main showcase` is empty.

## Decisions Made

See `key-decisions` in frontmatter. Summarized: focus in VER07 is driven by `requestFocus()` issued before hover/press (matching real keyboard traversal, not a hybrid no user path produces); VER08 asserts "changes at most once" rather than "never changes" because the flip itself is real and correct; VER09 asserts presence-and-singleness of the contour rather than absolute pixel counts, because absolute counts legitimately shift at fractional scale.

## Deviations from Plan

None — plan executed exactly as written across all four tasks. This closeout session performed no new implementation work; it wrote the SUMMARY/STATE/ROADMAP artifacts for work already committed.

## Requirements Note (deliberate deviation from default requirement-marking)

This plan's own frontmatter lists `requirements: [SHW-16, VER-06]`. Per this plan's explicit closeout instructions:

- **SHW-16 is NOT marked complete.** It remains `- [ ]` in `REQUIREMENTS.md`. Plan 20-07's human verdict owns SHW-16's closure and has not yet been given — see "The Boundary" section above for exactly what remains open.
- **VER-06 is already `- [x]` in `REQUIREMENTS.md`** (closed by Plan 20-01/20-03). This plan's four D-08 fixture proofs further reinforce VER-06's "provably fails on unfixed code" standard but do not change its already-complete state — left untouched per instruction.

`gsd-tools query requirements.mark-complete` was deliberately NOT run for this plan, since it would mark both IDs from the frontmatter and there is no partial-mark mode; marking only VER-06 (already marked) would be a no-op, and marking SHW-16 would violate the explicit instruction not to.

## Issues Encountered

None. All four tasks' verify steps (`./gradlew :library:test --tests "*VER07*|*VER08*|*AeroIconButtonFocusVisible*|*VER09*"`) had already passed prior to this closeout session; this session did not re-run the build.

## User Setup Required

None — no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. All four new files are test-only, exercising existing shipped composables through their own public APIs (`requestFocus`, `performMouseInput`, `mainClock`, `LocalDensity`) — no new network, auth, or file-access surface introduced.

## Next Phase Readiness

- The mechanical half of SHW-16's checklist (per-state matrix, label-flip, AeroIconButton focus wiring, fractional-density contour rounding) is now permanently gated and will re-run on every build — 20-07's human pass can rely on these four items being green rather than re-verifying them by eye.
- **SHW-16 remains open.** 20-07 must still run: the cross-component coherence pass over all eight components on all three themes, the AeroDark 4.079-contrast legibility judgment, and the two live non-100%-DPI passes on the real running app (125% and 200%, AeroBlue). None of these three is machine-checkable and none was attempted here.
- Phase 20 now has **8** plans (20-01 through 20-08), not 7 — `ROADMAP.md` and `STATE.md` updated accordingly in this closeout.

---
*Phase: 20-verification*
*Completed: 2026-07-29*

## Self-Check: PASSED

- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroIconButtonFocusVisibleWiringTest.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER09FractionalDensityRoundingTest.kt
- FOUND commit: 2dc046e (test(20-08): gate AeroButton/AeroOutlinedButton state matrix across three schemes)
- FOUND commit: a9c1c50 (test(20-08): gate segment label flip across the selection animation)
- FOUND commit: 7532b59 (test(20-08): gate AeroIconButton focusVisible wiring (WR-02))
- FOUND commit: a4b9505 (test(20-08): gate 1dp contour rounding at fractional density)
- FOUND commit: 6474569 (docs(20-08): plan Compose test-API gates for the mechanical half of SHW-16)
