---
phase: 22-native-window-behavior-release-3-2-0
plan: 13
subsystem: windows-native
tags: [ver-14, ver-11, d-01, d-02, d-05, api-01, api-04, btn-01, win-02, locked-test-count, headless-tests, mutation-proof, no-jna-gate]

requires:
  - phase: 22-native-window-behavior-release-3-2-0
    provides: "22-07/22-19 live-region pipeline (HitTestRegionRegistry, classifyHitTest, captions map); 22-09 AeroMaxButtonInteraction bridge; 22-11 bands + D-01 floor + resize gate; 22-12/22-19 public opt-out and rememberAeroWindowChrome"
provides:
  - "51 headless tests in 10 new/extended classes locking the pure native-window logic: HitTestClassificationTest (13), Win32GeometryTest (7), MinimumSizeTest (5), HitTestRegionRegistryTest (3), WndProcSupportTest (4), ResizeHandlesGateTest (4), TitleBarButtonParityTest (9), AeroWindowChromeStateTest (4), PublicApiNoJnaTest (1), AeroTitleBarTest compile gate (+1)"
  - "VER-14 executed: lockedTestTotal 541 -> 577 -> 587 -> 592 across three single commits whose messages name the reason; before every raise an unfiltered run against the old number was recorded failing on the guard's own message"
  - "One recorded failing mutation per test class (10 mutations), each reverted with git diff --quiet -- library/src/main holding afterwards"
  - "D-02 pixel parity locked headlessly: bridged MutableInteractionSource vs real Compose hover/press, pixel-identical in all three themes, with non-vacuous controls"
  - "Public no-JNA rule locked by a reflection gate over the compiled artifact bytes, proven failing on an injected com.sun.jna.Pointer leak"
affects: [22-15, 22-16, 22-17, 22-18]

tech-stack:
  added: []
  patterns:
    - "A locked-count raise is ONE commit holding the new tests together with the new number — a tests-only commit would itself fail the guard; the pre-raise failing run is the guard's liveness proof"
    - "Public-API surface rules are guarded at the bytecode level (Class.forName walk over the compiled classes dir), not by source grep — Kotlin internal compiles to public bytecode, and consumers see bytes"
    - "runComposeUiTest with mainClock.autoAdvance = false needs an explicit advanceTimeBy for recomposition to run at all; one identical advance per compared variant makes pixel parity deterministic"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestClassificationTest.kt
    - library/src/test/kotlin/com/mordred/aero/internal/windows/Win32GeometryTest.kt
    - library/src/test/kotlin/com/mordred/aero/internal/windows/MinimumSizeTest.kt
    - library/src/test/kotlin/com/mordred/aero/internal/windows/HitTestRegionRegistryTest.kt
    - library/src/test/kotlin/com/mordred/aero/internal/windows/WndProcSupportTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/ResizeHandlesGateTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/TitleBarButtonParityTest.kt
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroWindowChromeStateTest.kt
    - library/src/test/kotlin/com/mordred/aero/verification/PublicApiNoJnaTest.kt
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-13-SUMMARY.md
  modified:
    - library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt
    - library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt
    - library/build.gradle.kts
    - .planning/STATE.md
    - .planning/REQUIREMENTS.md
    - .planning/ROADMAP.md

key-decisions:
  - "VER-14 mechanics: three raises (logic tests 577, parity + API shapes 587, custom-title-bar API + no-JNA 592), each a single commit containing the new tests together with the new number and a message naming the reason"
  - "AeroWindowChromeStateImpl private -> internal (no behavior change) — the constructor seam the headless API-04 tests compose against a free-standing HitTestRegionRegistry"
  - "Only VER-14 marked complete: BTN-01's and WIN-02's real-input clauses (native hover frames, system cursor, live drag) stay Plan 15's; API-01/API-04 were already Complete and were re-proven here"

requirements-completed: [VER-14]

duration: ~45min
completed: 2026-09-27
---

# Phase 22 Plan 13: Headless test layer + first locked-count raises — Summary

**51 headless tests lock the native-window logic, D-01/D-02, the API-01/API-04 shapes and the public no-JNA rule — every class proven failing on a recorded mutation — and the locked test count moves 541 → 592 through three commits that each name the reason, with the count guard recorded failing on its own message before every raise (VER-14).**

## Performance

- **Duration:** ~45min (started 2026-09-27T21:02:48Z)
- **Tasks:** 3/3
- **Files:** 15 tracked (9 test files created, AeroTitleBarTest + AeroWindowChrome.kt + build.gradle.kts modified, plus SUMMARY/STATE/REQUIREMENTS/ROADMAP)
- Final unfiltered run: `AERO_TEST_COUNT total=592 skipped=0 expected=592 expectedSkipped=0 filtered=false` — BUILD SUCCESSFUL, re-verified after the last commit

## Accomplishments

- **Task 1 — native-window logic tests + first raise (`bd40502`):** 36 tests across six classes. `HitTestClassificationTest` locks the classifier on AeroTitleBar's real geometry at 100% and 150% (caption/max/min/close/leading/client codes, edge 10/11/12/15 and corner 13/14/16/17 bands at 2 px from the border, band-beats-caption precedence, no bands maximized or at band 0, EMPTY snapshot, the narrow-window leading+marked layout, and a custom two-caption header with a left-hand maximize box — API-04/D-05 generality). `Win32GeometryTest` locks the work-area clamp of the sizing-border overhang, the per-detected-edge auto-hide inset (incl. `rcWork == rcMonitor`), taskbars on each edge, a negative-origin secondary monitor, and `resizeBandPx = max(system frame, CMP 8 dp)` at 100/125/150/200%. `MinimumSizeTest` locks D-01 (app-set 390×300 at 150% returned unchanged; default 320×240 dp in px at scale; Compose dp path). `HitTestRegionRegistryTest` runs a 200k-publish writer against 4 lock-free readers (never-torn invariants + retained snapshots immutable after return) and two-registry independence. `WndProcSupportTest` locks `dispatchSafely` (value, fallback-exactly-once on RuntimeException, StackOverflowError) and negative 16-bit decode. `ResizeHandlesGateTest` locks the one-resize-path gate.
- **Task 2 — D-02 pixel parity + API-01 compile gate + second raise (`49bf306`):** `TitleBarButtonParityTest` (9 tests) proves the maximize button driven through a bridged `MutableInteractionSource` renders pixel-identically to the same `TitleBarButton` driven by real `performMouseInput` hover/press in AeroBlue/AeroDark/Classic, with `mainClock.autoAdvance = false` and one identical clock advance per variant; each state also asserts it visibly paints (differs from that theme's own rest capture) so a both-sides-identical break cannot pass vacuously. `AeroTitleBarTest` gained `preservedCallShapes` (both pre-phase `AeroTitleBar` call shapes, `AeroResizeHandles`, the `nativeWindowManagement = false` opt-out, `Modifier.markAeroTitleBarInteractive()`) as the API-01 compile gate.
- **Task 3 — API-04 tests + public no-JNA gate + third raise (`1b68a77`):** `AeroWindowChromeStateTest` composes the state class on a free-standing `HitTestRegionRegistry` (`isNative = true`): a custom non-AeroTitleBar 48 dp header (maximize box on the LEFT, two caption areas split by a clickable search field) publishes 2 caption + 1 maximize + 1 interactive rect matching the nodes' `boundsInRoot` within ±1 px and classifies 9/2/1/2; the opted-out state publishes nothing (`HitTestSnapshot.EMPTY`) while `maximizeHovered` still flows from the shared source; removing an element from composition removes its rect; `apiShape` is the compile gate for both `rememberAeroWindowChrome` shapes and the three modifiers. `PublicApiNoJnaTest` walks every compiled main class under `com/mordred/aero` (via the code source of a known main class) and fails listing any public signature outside `internal/windows` mentioning `com.sun.jna` — bytecode level, so Kotlin `internal`'s public bytecode is covered too.

## VER-14: the three raises and their recorded guard failures

Each raise is ONE commit containing the new tests together with the new number; before each raise an unfiltered `./gradlew :library:test --rerun` with the OLD number was run and recorded failing on the guard's own message:

| Raise | Commit | Pre-raise guard failure (recorded verbatim) | New total |
|---|---|---|---|
| 1 | `bd40502` `build(22-13): raise locked test count to 577 — native-window logic unit tests (VER-14)` | `Test count guard: executed total=577 skipped=0, locked total=541 skipped=0` (line `AERO_TEST_COUNT total=577 skipped=0 expected=541 expectedSkipped=0 filtered=false`) | 577 = 541 + 36 |
| 2 | `49bf306` `build(22-13): raise locked test count to 587 — max-button visual parity and API shape gates (VER-14)` | `Test count guard: executed total=587 skipped=0, locked total=577 skipped=0` | 587 = 577 + 10 |
| 3 | `1b68a77` `build(22-13): raise locked test count to 592 — custom-title-bar API and public no-JNA gate (VER-14)` | `Test count guard: executed total=592 skipped=0, locked total=587 skipped=0` | 592 = 587 + 5 |

The `library/build.gradle.kts` header now carries all three reason lines (`577 = …`, `587 = …`, `592 = …`).

## Mutation proofs (feedback_repro_must_exercise_path)

Every new test class was shown to FAIL on a named, temporary mutation of the code it guards, run class-filtered (`--tests`, guard-exempt), then reverted — `git diff --quiet -- library/src/main` held after each revert:

| Test class | Mutation | Failing tests observed |
|---|---|---|
| HitTestClassificationTest | resize-band block moved after the caption check (precedence swap) | `topResizeBandBeatsTheCaption`, `customHeaderAreasAnswerTheirOwnCodes` |
| Win32GeometryTest | auto-hide inset application disabled (`if (false && …)`) | `autoHideBottomInsetsTheBottomEdgeWhenWorkAreaEqualsTheMonitor`, `secondaryMonitorAtANegativeOrigin`, `autoHideInsetsApplyOnlyOnDetectedEdges` |
| MinimumSizeTest | `appMinimumSet` flag ignored in `resolveMinimumTrackSizePx` | `appSetMinimumIsReturnedUnchangedNeverRaisedToTheDefault` |
| WndProcSupportTest | `dispatchSafely` rethrows instead of falling back | `runtimeExceptionYieldsTheFallbackExactlyOnce`, `stackOverflowErrorYieldsTheFallback` |
| ResizeHandlesGateTest | gate inverted (`&& nativeChromeActive`) | `floatingWindowWithNativeChromeStandsDown`, `floatingWindowWithoutNativeChromeComposesTheHandles` |
| HitTestRegionRegistryTest | `publishInteractive` mutates a shared in-place map across snapshots | `concurrentPublishAndReadNeverTearsARect` |
| TitleBarButtonParityTest | bridged branch paints the hover colour at half alpha | all 6 hover/press parity tests (rest control unaffected) |
| AeroWindowChromeStateTest | (a) `captionArea` publishes into the interactive map; (b) inert state publishes anyway (guard removed) | (a) `customHeaderPublishesAndClassifiesItsOwnRegions`, `removingAnElementFromCompositionRemovesItsPublishedRect`; (b) `inertStatePublishesNothingAndStillReportsHoverFromTheSource` |
| PublicApiNoJnaTest | injected `public val apiLeak: com.sun.jna.Pointer? = null` into AeroWindowChrome.kt | `noPublicLibrarySignatureMentionsComSunJna` |
| AeroTitleBarTest | — (compile gate; compilation of the preserved call shapes IS the gate, existing style) | n/a |

## Task Commits

1. **Task 1: Pure logic tests + mutation proofs + first named count raise** - `bd40502` (build)
2. **Task 2: D-02 pixel parity test, API-01 compile gate, second named count raise** - `49bf306` (build)
3. **Task 3: API-04 tests, opt-out inertness, public no-JNA reflection gate, third named count raise** - `1b68a77` (build)

## Files Created/Modified

- `library/src/test/kotlin/com/mordred/aero/internal/windows/*Test.kt` (5 files) — the pure-logic locks
- `library/src/test/kotlin/com/mordred/aero/components/navigation/ResizeHandlesGateTest.kt`, `TitleBarButtonParityTest.kt`, `AeroWindowChromeStateTest.kt` — the gate / parity / API-04 locks
- `library/src/test/kotlin/com/mordred/aero/verification/PublicApiNoJnaTest.kt` — the bytecode-level no-JNA gate
- `library/src/test/kotlin/com/mordred/aero/components/navigation/AeroTitleBarTest.kt` — added the preserved call shapes + compile-gate test
- `library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt` — `AeroWindowChromeStateImpl` private → internal (headless seam, no behavior change)
- `library/build.gradle.kts` — lockedTestTotal 541 → 592 with the three reason lines

## Decisions Made

- VER-14 executed exactly as the plan's mechanics section chose: three raises, each ONE commit with tests + number + named reason (a tests-only commit would itself fail the guard), the pre-raise failing run recorded each time.
- Requirements: only VER-14 newly marked complete. BTN-01 and WIN-02 stay Pending — their real-input clauses (native hover frames over the max button, system cursor, live drag, SNAP-06 shared border) belong to Plan 15's session; the headless halves (pixel parity, classification, gate) are now locked. API-01 and API-04 were already Complete (22-12 / 22-19); this plan added their permanent automated gates (compile shapes, custom-layout generality, no-JNA reflection scan) without re-marking.
- `PublicApiNoJnaTest` anchors on `AeroColorScheme::class.java`'s code source (Kotlin file-facade classes like `AeroTitleBarKt` cannot be named in Kotlin source; the facade classes are still scanned through the directory walk).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `AeroWindowChromeStateImpl` was file-private, blocking the plan's headless composition**
- **Found during:** Task 3
- **Issue:** the plan composes "the internal state class … built on a free-standing HitTestRegionRegistry"; the class Plan 19 delivered is `private` (constructor parameterized for exactly this use, but not constructible from tests)
- **Fix:** `private class AeroWindowChromeStateImpl` → `internal class` (one word + KDoc note; no behavior change, no new public surface — `internal` is module-scoped and excluded from the public API)
- **Files modified:** library/src/main/kotlin/com/mordred/aero/components/navigation/AeroWindowChrome.kt
- **Verification:** 592/0 unfiltered green; `PublicApiNoJnaTest` still passes (the internal class is bytecode-public but outside `internal/windows`, so it is scanned and clean)
- **Committed in:** `1b68a77`

**Total deviations:** 1 auto-fixed (1 Rule 3)
**Impact on plan:** necessary for the plan's own Task 3 acceptance criteria. No scope creep.

## Assumption Drift (advisory)

- Planned: Task 3 speaks of "the internal state class", implying it was already internal. Actual: Plan 19 shipped it `private` while parameterizing the constructor for headless composition. Why the drift matters: the seam existed but was unreachable; surfaced as the Rule 3 deviation above rather than a design change.
- Planned: Task 2's "identical clock advances in both variants" read as a determinism nicety. Actual: with `mainClock.autoAdvance = false` a recomposition only runs once a frame is clocked through — the hover variant initially failed its own non-vacuous control until an explicit `advanceTimeBy` was added (identical in both variants, as the plan requires). No product code involved.

## Issues Encountered

- Two compile/test iterations while authoring the new tests (not product bugs): `AeroIcons.Square` needs the `com.mordred.aero.icons.internal.Square` extension import (lazy backing-property pattern); empty `Box(Modifier.weight(1f))` children measure to zero rects in the uiTest environment, so the custom-header boxes use `fillMaxHeight()` to be measurable — the published-rect-vs-node-bounds assertions then pass strictly.
- A `git checkout --` revert of the injected-JNA mutation also reverted the not-yet-committed `internal class` seam edit; re-applied immediately and the final committed diff is exactly the one-word visibility change plus KDoc note.

## Threat Model Coverage

- **T-22-28 (lockedTestTotal tampering):** mitigated — three named `build(22-13): raise locked test count to <N> — <reason> (VER-14)` commits; the pre-raise guard failure recorded verbatim for each (table above).
- **T-22-29 (false-positive guards):** mitigated — one recorded failing mutation per test class (table above); `git diff --quiet -- library/src/main` verified after every revert and again after each commit.
- **T-22-32 (API leakage):** mitigated — `PublicApiNoJnaTest` scans every compiled main class outside `internal/windows` (methods, constructors, fields, generic types) and was proven failing on an injected `com.sun.jna.Pointer` leak.

## Known Stubs

None — all ten test classes assert real behavior; no placeholder or unwired data.

## Threat Flags

None — no security-relevant surface beyond the plan's threat model was introduced.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 15 (real input): BTN-01's native-hover frames and BTN-02/WIN-02/SNAP's real-gesture clauses remain the open proof obligations; the headless halves are locked (parity test + classifier).
- Plans 16-17 (README/KDoc): the locked suite now guards every behavior they will document.
- The count guard now sits at 592/0 — any later plan adding tests must raise it the same named-commit way (VER-14 pattern).

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-27*

## Self-Check: PASSED

- All 10 created files exist on disk; task commits bd40502 / 49bf306 / 1b68a77 present in git log, and this SUMMARY is committed (docs commit at HEAD); working tree clean apart from the pre-existing never-staged untracked set (.planning/config.json, hs_err/replay logs, .serena/, .vscode/, .planning/HANDOFF.json); no process left running (all Gradle runs were foreground; no showcase JVM was ever launched by this plan).
