---
phase: 20-verification
plan: 01
subsystem: testing
tags: [kotlin-test, source-scan-gate, regression-guard, compose-modifier-chain]

# Dependency graph
requires:
  - phase: 16-foundation-aero-primitives-layer
    provides: "drawAeroSurfaceCore / Modifier.aeroSurface / Modifier.aeroGlowRing (the primitives VER-01/VER-02 protect) and the six shipped *SourceTest.kt precedents (sourceFile()/nonCommentSource idiom)"
  - phase: 19-selectors-lists
    provides: "AeroSegmentedControl.kt's two-unrelated-chains real shape (the concrete VER-02 false-positive case)"
provides:
  - "VER-01 gate: every gradient's explicit end-stop coordinate must derive from size. (proportional), never a pixel literal/named pixel constant"
  - "VER-02 gate: aeroGlowRing must precede aeroSurface in a shared chain; no second .clip( after aeroSurface( in the same chain — chain-aware, does not misfire on AeroSegmentedControl.kt's two unrelated chains"
  - "D-08 in-file fail-then-pass fixture convention (VER-06), proven end-to-end and replicated a second time"
affects: [20-verification remaining plans (20-03 VER-03, 20-04 D-13 contrast regression), any future gate reusing the modifierChains/stripComments/mainSourceFiles idioms]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure detector-over-source-text function, never over File — the in-file fixture proof and the real-source scan exercise the exact same code path (D-08)"
    - "Chain-aware source scanning: modifierChains() segments comment-stripped text into individual Compose modifier-chain blocks via bracket-depth tracking, instead of whole-file substring/order scanning"
    - "fun-declaration exclusion in chain-start detection — prevents a primitive's own definition site (e.g. `fun Modifier.aeroSurface(...)`) from being misread as a consumer call site"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt
    - library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt
  modified: []

key-decisions:
  - "Task 1 (tracer) shape accepted as-is at the tracer feedback checkpoint: START coordinates stay outside the endpoint rule (D-05), a named constant carrying no size. counts as a violation, and the three-layer shape (pure detector -> in-file RED/GREEN fixtures -> real-source scan with non-zero file-count assertion) is the template all later gates in this phase replicate"
  - "VER-02's modifierChains() segmenter excludes any chain-start candidate line containing the `fun ` keyword — without this, AeroSurfacePrimitives.kt's own `fun Modifier.aeroSurface(style: AeroSurfaceStyle, shape: Shape): Modifier = this` declaration would be misread as a chain, and its declaration text `Modifier.aeroSurface(` (containing the literal substring `aeroSurface(`) would falsely pair with that same function's own internal `.clip(shape)` line, producing a false VER-02 violation against the primitive the gate exists to protect. Found and fixed during Task 2 authoring, before the fixture proof or real-source scan were run (Rule 1 - bug, caught pre-verification via source reading, not left for a failing test to surface)."

patterns-established:
  - "Pattern 1: modifierChains(source) — bracket-depth-tracked chain segmentation. A chain starts at a line matching \\bModifier\\b|\\bmodifier\\s*= that does not also contain `fun `; continues through top-level `.`-continuation lines and any line inside a bracket run the chain itself opened; stops at a line that is neither, or that itself starts a NESTED Modifier chain at a deeper depth (e.g. AeroSegmentedControl.kt's `.then(...)` nesting a `Modifier.drawBehind { ... }` block)."

requirements-completed: [VER-01, VER-02, VER-06]

coverage:
  - id: D1
    description: "VER-01 gate: gradient end-stop proportionality — flags bare pixel literals and named pixel constants carrying no size., accepts size.-relative stops, empty scan sets, and comment-only pixel literals, and passes cleanly against the entire real library source (18 gradient call sites, 9 files)"
    requirement: "VER-01"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt (8 tests, all pass) via ./gradlew :library:test --tests \"*VER01*\""
        status: pass
    human_judgment: false
  - id: D2
    description: "VER-02 gate: aeroSurface clip-order — chain-aware, flags aeroGlowRing-after-aeroSurface and .clip-after-aeroSurface within a shared chain, does not flag AeroSegmentedControl.kt's two genuinely-unrelated chains, and passes cleanly against the entire real library source"
    requirement: "VER-02"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt (8 tests, all pass) via ./gradlew :library:test --tests \"*VER02*\""
        status: pass
    human_judgment: false
  - id: D3
    description: "VER-06: both gates carry an in-file, re-executed red-on-broken/green-on-clean fixture proof (D-08) — proven twice (once per gate) that a gate carrying zero fixtures cannot happen and that the red verdict is attributable to exactly the property under test"
    requirement: "VER-06"
    verification:
      - kind: unit
        ref: "VER01...Test: 2 RED fixture assertions + 5 GREEN fixture assertions + 1 real-source assertion. VER02...Test: 2 RED fixture assertions + 6 GREEN fixture assertions + 1 real-source assertion. Full suite: ./gradlew :library:test"
        status: pass
    human_judgment: false

duration: 11min (Task 2 continuation session; Task 1 was authored and committed in a prior session before the tracer checkpoint)
completed: 2026-07-28
status: complete
---

# Phase 20 Plan 01: VER-01/VER-02 Source-Scan Gates Summary

**Two new chain-aware `*SourceTest.kt` gates (VER-01 gradient end-stop proportionality, VER-02 aeroSurface clip-order), each with its own in-file red-on-broken/green-on-clean fixture proof (D-08/VER-06), both green against the entire real library source with zero production-code changes.**

## Performance

- **Duration:** 11 min (this continuation session, Task 2 + wrap-up). Task 1 was authored, proven, committed, and human-approved at a tracer checkpoint in a prior session.
- **Completed:** 2026-07-28T16:35:32Z (Task 2 commit)
- **Tasks:** 2/2
- **Files modified:** 2 (both new test files; zero production source files touched)

## Accomplishments

- **VER-01** (`VER01GradientProportionalitySourceTest.kt`): pure `gradientEndStopViolations(source: String)` detector flags any `Brush.verticalGradient`/`horizontalGradient`/`linearGradient` whose explicit `endY`/`endX`/`end` argument doesn't contain `size.` — catching both the historical PRIM-09 bare-pixel-literal defect and its named-constant disguise, while leaving `startY`/`startX`/`start` (D-05 carve-out) and colour-stop fraction lists untouched. 7 fixture tests + 1 real-source scan (18 call sites across 9 files, zero violations).
- **VER-02** (`VER02AeroSurfaceClipOrderSourceTest.kt`): a new `modifierChains(source: String)` segmenter splits comment-stripped source into individual Compose modifier-chain text blocks via bracket-depth tracking, so the two detectors — `glowRingBeforeSurfaceViolations` (D-07a: glow must precede surface) and `clipAfterSurfaceViolations` (D-07b: no second clip after surface) — compare call order only WITHIN a chain, never across unrelated chains. Proven chain-aware against a fixture reproducing `AeroSegmentedControl.kt`'s real two-chain shape (outer `.clip(` chain, unrelated inner `aeroSurface(` chain) and against the real file itself. 7 fixture tests + 1 real-source scan, zero violations.
- Both gates follow the D-08 convention Task 1 established as a tracer: pure function over source text (never `File`) → in-file `const val` RED/GREEN fixtures asserted directly against the detector → real-source scan asserting both zero violations and a non-zero scanned-file count (guards against a silently-broken cwd/path resolution producing a vacuous pass).
- Full library suite green: 418 tests, 0 failures, 0 errors, across 81 test classes (`./gradlew :library:test`). `git status --porcelain library/src/main` is empty — no production source was touched by this plan.

## Task Commits

Each task was committed atomically:

1. **Task 1: VER-01 gradient end-stop gate, end-to-end with its in-file fail-then-pass proof** - `edfd7c7` (test) — tracer, human-approved at checkpoint before Task 2 began
2. **Task 2: VER-02 aeroSurface clip-order gate — chain-aware, with its in-file fail-then-pass proof** - `ed8277b` (test)

**Plan metadata:** commit created immediately after this SUMMARY (see repository history)

## Files Created/Modified

- `library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt` - VER-01 gate: `gradientEndStopViolations`, `stripComments`, `mainSourceFiles`, 8 `@Test` methods
- `library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt` - VER-02 gate: `modifierChains`, `glowRingBeforeSurfaceViolations`, `clipAfterSurfaceViolations`, `isChainStartCandidate`, `stripComments`, `mainSourceFiles`, 8 `@Test` methods

## Decisions Made

- **Tracer checkpoint (Task 1) approved as-is, no changes requested.** The user's approval confirmed three specifics as correct and binding for Task 2: (a) START coordinates stay outside D-05's rule as worded; (b) a named constant carrying no `size.` is a violation, not just a bare numeric literal; (c) the three-layer shape (pure detector → in-file fixtures → real-source scan with non-zero file-count assertion) is the template. Task 2 replicates this shape exactly.
- **`isChainStartCandidate`'s `fun `-keyword exclusion** (see key-decisions in frontmatter) — found and fixed while authoring `modifierChains`, before running any test, by reading `AeroSurfacePrimitives.kt`'s own `Modifier.aeroSurface`/`aeroGlowRing`/`aeroThumbSurface`/`aeroGroove` declarations and recognizing that their declaration lines (`fun Modifier.aeroSurface(...)`) would otherwise self-match as chain starts and falsely pair with their own internal `.clip(shape)`. Verified by the green `realLibrarySourceHasNoClipOrderViolations` result — without the exclusion this test would have failed against `AeroSurfacePrimitives.kt` itself.
- **`AERO_GLOW_RING_TOKEN`/`AERO_SURFACE_TOKEN` chosen deliberately substring-safe**: `aeroGlowRing(` never matches inside `aeroGlowRingRepeated(` (AeroSlider.kt's local intensity-stacking helper) because `Repeated` interposes before the `(`; `aeroSurface(` never matches inside `aeroThumbSurface(` because `Thumb` interposes between `aero` and `Surface(`. Verified directly by reading `AeroSlider.kt`'s thumb slot (which legitimately calls both `aeroGlowRingRepeated` and `aeroThumbSurface` in the same chain) and confirming no false pairing.

## Deviations from Plan

None - plan executed exactly as written. The `isChainStartCandidate` `fun `-exclusion design point is not a deviation from the plan's text (the plan's `<action>` block already anticipates nested-chain handling and does not specify token-matching mechanics at that level of detail); it is an implementation-level correctness requirement discovered by reading the real source before writing the detector, and resolved before any code was run — consistent with Rule 1 (auto-fix bugs) applied proactively during authoring rather than reactively after a failing assertion.

## Issues Encountered

None. Both gates passed on first `./gradlew` run with zero fixture-adjustment iterations required.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- VER-01 and VER-02 both ship in the normal Gradle test suite (D-09), scanning the whole library (D-06), with their fail-then-pass proofs living as re-executed fixtures inside their own test classes (D-08) rather than a one-time SUMMARY transcript.
- The `modifierChains`/`stripComments`/`mainSourceFiles` idioms established here are ready for direct reuse by 20-03's VER-03 baseline-size-snapshot gate and 20-04's D-13 contrast-regression gate — both sibling plans in this phase.
- No blockers. VER-05 (scratch-consumer) and VER-03/SHW-15/SHW-16/VER-04 remain for the phase's other plans.

---
*Phase: 20-verification*
*Completed: 2026-07-28*

## Self-Check: PASSED

- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt
- FOUND: library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt
- FOUND: .planning/phases/20-verification/20-01-SUMMARY.md
- FOUND commit: edfd7c7
- FOUND commit: ed8277b
