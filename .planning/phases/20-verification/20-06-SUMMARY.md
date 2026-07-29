---
phase: 20-verification
plan: 06
subsystem: testing
tags: [jitpack, gradle, compose-desktop, external-consumer, verification, theme]

# Dependency graph
requires:
  - phase: 15-toolchain-upgrade
    provides: "15-06-SUMMARY.md — the throwaway pre-release tag + JitPack build-and-verify mechanism this plan reuses (not its scope, which never created or ran a consuming project)"
  - phase: 20-verification
    plan: 05
    provides: "20-REVIEW.md/20-REVIEW-FIX.md — the reviewed HEAD (3de82c8) this plan's first tag was cut from"
provides:
  - "v3.0.0-verify01 — throwaway tag cut at 3de82c8, JitPack-built ok, but proven to ship a real defect (unpainted AeroTheme background) by the human-verify checkpoint"
  - "1d139a7/ff577fc — the library-level fix (AeroTheme establishBackground) and its regression gate, landed in response to that defect"
  - "f07cc48 — review addendum closing WR-03/IN-03/IN-04 before re-tagging, per D-03"
  - "v3.0.0-verify02 — the re-cut, re-reviewed, JitPack-built, human-verified tag proving the published artifact renders correctly for a stranger"
  - "C:/1A_W/aero-scratch-consumer — external Compose Desktop project, outside this repo, resolving the library purely by JitPack coordinate"
affects: ["20-07 (three-theme sign-off — inherits AeroTheme's establishBackground default and the WR-03 ABI gap as background context)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Throwaway pre-release JitPack tag re-cut mid-plan when a human-verify checkpoint fails, rather than patching the consumer around a library defect — the fix and its gate land in the library, the tag is re-cut on the reviewed fix, per user's own architectural decision at the checkpoint"
    - "AeroTheme establishBackground: Boolean = true — plain Box (propagateMinConstraints = false), not Material3 Surface, to avoid forcing non-filling content up to full window height"

key-files:
  created:
    - C:/1A_W/aero-scratch-consumer/settings.gradle.kts
    - C:/1A_W/aero-scratch-consumer/build.gradle.kts
    - C:/1A_W/aero-scratch-consumer/gradle.properties
    - C:/1A_W/aero-scratch-consumer/src/main/kotlin/Main.kt
    - library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
    - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt

key-decisions:
  - "User decided (orchestrator checkpoint, 2026-07-29) to fix the LIBRARY (AeroTheme itself), not patch around the defect in the scratch consumer alone, after being told this changes public API, affects ~40 components sharing the theme, and requires a new tag plus re-review."
  - "Plain Box + .background(...) chosen over Material3 Surface specifically because Surface's propagateMinConstraints = true forced non-filling content (a bare AeroListItem with heightIn(min = 36.dp)) up to full window height — caught by two real test failures (AeroListItemLayoutTest, AeroRangeSliderHoverCancellationTest) before landing on Box."
  - "establishBackground defaults to true (not an unconditional always-on fill) so ~40 existing call sites (previews, unit tests, the showcase) are fixed with zero signature changes, while still letting a consumer embedding AeroTheme {} inside a foreign, already-painted surface opt out."
  - "WR-03 (ABI/binary-compatibility of the new parameter for the JitPack-published artifact) filed as a pending todo rather than fixed inline — it is a policy decision (add @JvmOverloads, or accept source-only compatibility as the library's standing practice), not a mechanical fix."
  - "IN-03/IN-04 recorded in 20-REVIEW.md's addendum only — no separate todo, per this plan's own instruction; both are documentation-precision gaps, not defects."

patterns-established:
  - "A failed human-verify checkpoint on a published artifact is itself the plan's most valuable output when it exposes a genuine library defect no source-scan or contrast gate could reach — recorded prominently in the SUMMARY body, not smoothed into a deviations footnote."

requirements-completed: [VER-05]

coverage:
  - id: D1
    description: "Throwaway pre-release tag v3.0.0-verify01 cut at 3de82c8, pushed to origin, JitPack build confirmed ok with matching commit SHA"
    requirement: "VER-05"
    verification:
      - kind: other
        ref: "git ls-remote --tags origin v3.0.0-verify01; JitPack build API status: ok"
        status: pass
    human_judgment: false
  - id: D2
    description: "External scratch consumer at C:/1A_W/aero-scratch-consumer resolves com.github.Tolaseeq:aero-compose-ui purely by published JitPack coordinate (no includeBuild, no project substitution), imports nothing from the showcase, builds green with --refresh-dependencies"
    requirement: "VER-05"
    verification:
      - kind: other
        ref: "gradle --refresh-dependencies build (consumer dir) — BUILD SUCCESSFUL; gradle dependencies --configuration runtimeClasspath confirms resolved coordinate"
        status: pass
    human_judgment: false
  - id: D3
    description: "First human-verify attempt on v3.0.0-verify01 — window rendered on a white background with washed-out dark-theme labels; FAILED"
    requirement: "VER-05"
    verification: []
    human_judgment: true
    rationale: "Visual defect only observable by running the app and looking at it; this is the finding itself, not a re-derivable automated check."
  - id: D4
    description: "Root-cause fix: AeroTheme establishes its own themed background (establishBackground: Boolean = true, plain Box) instead of relying on the showcase's own Surface to hide the gap; regression gate added (AeroThemeBackgroundEstablishmentTest, captured-pixel RED/GREEN)"
    requirement: "VER-05"
    verification:
      - kind: unit
        ref: "library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt — 433 -> 435 tests, 0 failures"
        status: pass
    human_judgment: false
  - id: D5
    description: "Re-review (D-03) before re-tagging: 20-REVIEW.md addendum covering 1d139a7/ff577fc — 0 critical, 1 warning (WR-03), 2 info (IN-03, IN-04), no blockers"
    requirement: "VER-05"
    verification:
      - kind: other
        ref: ".planning/phases/20-verification/20-REVIEW.md — Addendum section, status: issues_found, no blocker"
        status: pass
    human_judgment: false
  - id: D6
    description: "Tag v3.0.0-verify02 cut at reviewed HEAD (f07cc48), JitPack build ok with exact SHA match, consumer bumped and rebuilt green against it"
    requirement: "VER-05"
    verification:
      - kind: other
        ref: "JitPack build API: status ok, commit f07cc485a9a51bbef41004910f9b6d95070f499c, isTag: true; consumer ./gradlew --refresh-dependencies build — BUILD SUCCESSFUL"
        status: pass
    human_judgment: false
  - id: D7
    description: "Second human-verify attempt — all eight components render on the correct dark background (AeroButton, AeroOutlinedButton, AeroSwitch, AeroSegmentedControl, AeroSlider, AeroRangeSlider, AeroProgressBar, AeroListItem), window titled exactly 'aero-compose-ui scratch consumer', nothing clipped; PASSED"
    requirement: "VER-05"
    verification: []
    human_judgment: true
    rationale: "Render correctness (background color, clipping, exact window title) is a visual judgment; the human confirmed a captured screen render of the launched app."

duration: n/a (spans a checkpoint fail/fix/retag cycle across multiple sessions)
completed: 2026-07-29
status: complete
---

# Phase 20 Plan 06: External Consumer Verification (VER-05) Summary

**A published-JitPack scratch consumer caught a real defect the showcase had been hiding — `AeroTheme` painted no background of its own — which was fixed in the library (not patched around), re-reviewed, re-tagged as `v3.0.0-verify02`, and confirmed rendering correctly on the second human-verify attempt.**

## Performance

- **Completed:** 2026-07-29
- **Tasks:** 3/3 (Task 1 tag+JitPack, Task 2 scaffold+build, Task 3 human-verify — run twice across a fix/re-review/re-tag cycle)
- **Files created:** 5 (4 external consumer project files outside this repo, 1 new library test)
- **Files modified:** 2 (`AeroTheme.kt`, `ShowcaseApp.kt`)

## Accomplishments

This plan's real yield is the fail-then-fix arc, not a single clean pass. Recorded in order:

1. **Tag `v3.0.0-verify01` cut at `3de82c8`** (the reviewed HEAD after 20-05's code review closed) and pushed to `origin`. JitPack built it successfully (`status: ok`), commit SHA confirmed to match the pushed tag.

2. **External consumer scaffolded at `C:/1A_W/aero-scratch-consumer`** — a sibling directory outside this repository (D-14), resolving `com.github.Tolaseeq:aero-compose-ui` purely by its published JitPack coordinate. No `includeBuild`, no project substitution, no import from the showcase module. First build attempt failed on `FAIL_ON_PROJECT_REPOS` (a stray `repositories{}` block in the consumer's own `build.gradle.kts` conflicting with settings-level repository declarations) — fixed by removing that block (Rule 3, blocking issue). Built green afterward.

3. **Human verification FAILED on the first attempt.** The user ran the consumer and reported the window rendered on a **white background** while every Aero theme is dark, with washed-out labels — a legibility failure, not a crash or missing component.

4. **Root cause — the headline VER-05 finding.** `AeroTheme` provided composition locals (`LocalAeroColors`/`LocalAeroTypography`/`LocalScrollbarStyle`) and wrapped content in `MaterialTheme`, but **painted no background at all**. The showcase had been hiding this the entire milestone by painting its own `Surface(color = colors.background)` in `ShowcaseApp.kt` — satisfying the assumption on the components' behalf. A stranger doing `AeroTheme { Column { ... } }` got Compose Desktop's default light window with AeroBlue's `labelText = 0xFFBDBDBD` and `onBackground = 0xFFE0E0E0` painted on top of it; `background = 0xFF0D1B2A` was never drawn. This is verbatim the hazard the plan's own objective named ahead of time: *"LocalAeroColors assumptions the showcase satisfies on the components' behalf."* No source-scan gate and no contrast test could have caught this — the defect was in behavior that did not exist (a missing paint call), not in a value that could be measured.

5. **User decided (orchestrator checkpoint) to fix the LIBRARY, not the consumer**, after being told the fix changes public API, touches all ~40 components sharing `AeroTheme`, and requires a new tag plus re-review before it can be re-verified.

6. **Fix landed (`1d139a7`, `fix(20-06): AeroTheme establishes its themed background`):** new parameter `establishBackground: Boolean = true`, wrapping `content` in `Box(Modifier.fillMaxSize().background(colorScheme.background))` when true. **Notable mid-fix regression avoided:** the author first tried Material3's `Surface`, whose internal container sets `propagateMinConstraints = true` — this forced non-filling content (e.g. a bare `AeroListItem` with `Modifier.heightIn(min = 36.dp)`) up to full window height. Two real, pre-existing tests caught this before it shipped — `AeroListItemLayoutTest` and `AeroRangeSliderHoverCancellationTest` — leading to a plain `Box` (`propagateMinConstraints = false`) instead, which reproduces exactly the loose constraints content received before this change existed.

7. **Gate added (`ff577fc`, `test(20-06): gate AeroTheme background establishment`):** a composed-node regression test using `onRoot().captureToImage()`, whose RED half drives the real, shipped `establishBackground = false` opt-out path (not a fabricated fixture) to prove the sampling methodology can actually detect an unpainted background. Suite went from 433 to 435 tests, 0 failures.

8. **Re-reviewed per D-03 before re-tagging** (`f07cc48`, `docs(20-06): review addendum for AeroTheme background change`) — no blockers. Three findings added to `20-REVIEW.md`'s addendum:
   - **WR-03**: the new `establishBackground` parameter is source-compatible (verified: Kotlin resolves the trailing-lambda `content` positionally regardless of preceding defaulted parameters, so every existing call site keeps compiling with zero edits) but **not verified binary-compatible** for the JitPack-published artifact — no `@JvmOverloads`, no ABI/binary-compatibility validator anywhere in the library. A consumer holding an already-compiled jar against the old 3-parameter descriptor could hit a `NoSuchMethodError`-class failure against a new publish. Filed as a pending todo (see below).
   - **IN-03**: `establishBackground = true` silently changes rendered output for a hypothetical future consumer who wanted a transparent `AeroTheme {}` slot; the project has no `CHANGELOG.md` to record this as a behavior change. Recorded in `20-REVIEW.md` only — documentation/process gap, no code change required.
   - **IN-04**: the new test's KDoc slightly overstates its precedent citation (claims the same `captureToImage()` idiom as `AeroRangeSliderHoverCancellationTest` without noting the scope difference — `onRoot()` vs. a tagged node — which the same KDoc correctly explains two sentences later). Comment-precision nit only, recorded in `20-REVIEW.md` only.

9. **Tag `v3.0.0-verify02` cut at `f07cc48`** (the re-reviewed HEAD) and pushed. JitPack: `status: "ok"`, `commit: f07cc485a9a51bbef41004910f9b6d95070f499c` — exact match with the tag; `isTag: true`; jar reachable (HTTP 200). Consumer's `build.gradle.kts` bumped `verify01` -> `verify02`; `./gradlew --refresh-dependencies build` -> BUILD SUCCESSFUL; `dependencies --configuration runtimeClasspath` confirms `com.github.Tolaseeq:aero-compose-ui:v3.0.0-verify02` resolved.

10. **Human verification PASSED on the second attempt.** The orchestrator launched the app and captured the window; the user confirmed. All eight components render on the correct dark background: `AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`. Window title exactly `aero-compose-ui scratch consumer`. Nothing clipped by the window edge.

   **Scope limit, recorded honestly:** confirmation was of the RENDER, from a static screen capture. Live interactivity (dragging a slider, toggling the switch) was **not** exercised on this second pass — narrower than the plan's own Task 3 `<how-to-verify>` step 5, which asked for interaction. This narrower scope is recorded here rather than silently treated as equivalent to a full pass.

## Repository State Note

Local `master` is ~218 commits ahead of `origin/master`. Only **tags** were ever pushed to the remote — never a branch. The reviewed code (`f07cc48`) is reachable on the remote solely via the `v3.0.0-verify02` tag, not via `master`. `v3.0.0-verify01` remains on the remote and carries code **without** the background fix — it is superseded and should not be used as a reference for what the published artifact currently renders.

## Task Commits

Each task was committed atomically (across the fail/fix/retag cycle):

1. **Task 1: Cut and publish the throwaway pre-release tag, confirm JitPack builds it** — tag `v3.0.0-verify01` at `3de82c8` (no repository commit; tag-only)
2. **Task 2: Scaffold the external consumer and build against the published artifact** — external project files at `C:/1A_W/aero-scratch-consumer` (outside this repo's git tree — no commit here)
3. **Task 3 (first attempt): Human-verify** — FAILED; findings routed to library fix
   - `1d139a7` (fix) — `AeroTheme` establishes its themed background
   - `ff577fc` (test) — gate AeroTheme background establishment
   - `f07cc48` (docs) — review addendum closing WR-03/IN-03/IN-04
   - Tag `v3.0.0-verify02` at `f07cc48` (no repository commit; tag-only)
4. **Task 3 (second attempt): Human-verify** — PASSED

**Plan metadata:** committed immediately after this SUMMARY (see repository history).

## Files Created/Modified

- `C:/1A_W/aero-scratch-consumer/settings.gradle.kts` — external Gradle settings, JitPack + standard repos, no `includeBuild`/`include`
- `C:/1A_W/aero-scratch-consumer/build.gradle.kts` — resolves `com.github.Tolaseeq:aero-compose-ui` by tag, bumped `verify01` -> `verify02`
- `C:/1A_W/aero-scratch-consumer/gradle.properties`
- `C:/1A_W/aero-scratch-consumer/src/main/kotlin/Main.kt` — minimal window rendering all eight components inside `AeroTheme {}`
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `establishBackground: Boolean = true` parameter, plain `Box` background paint
- `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt` — new regression gate (RED/GREEN via `captureToImage()`)
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` — its own `Surface(color = colors.background)` confirmed genuinely inert alongside the new `AeroTheme`-level paint (no double-paint visual defect)

## Decisions Made

See `key-decisions` in frontmatter. Summarized: the library was fixed (not the consumer alone) per explicit user decision at the orchestrator checkpoint; a plain `Box` was chosen over Material3 `Surface` specifically to avoid the `propagateMinConstraints` regression two real tests caught; `establishBackground` defaults `true` for zero-edit backward compatibility at ~40 existing call sites; WR-03 (ABI risk) is filed as a pending todo rather than fixed inline, since it is a policy decision, not a mechanical one.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed stray `repositories{}` block from consumer's `build.gradle.kts`**
- **Found during:** Task 2 (scaffold and build the external consumer)
- **Issue:** A `repositories{}` block declared inside the consumer's project-level `build.gradle.kts` conflicted with the settings-level `dependencyResolutionManagement` repositories, triggering Gradle's `FAIL_ON_PROJECT_REPOS` mode.
- **Fix:** Removed the project-level `repositories{}` block; all repository resolution now goes through `settings.gradle.kts`'s `dependencyResolutionManagement` block as originally planned.
- **Files modified:** `C:/1A_W/aero-scratch-consumer/build.gradle.kts` (outside this repository)
- **Verification:** `gradle --refresh-dependencies build` exits 0 afterward.

**2. [Rule 4 - Architectural, escalated to user] `AeroTheme` painted no background — library fix required**
- **Found during:** Task 3, first human-verify attempt
- **Issue:** The library's `AeroTheme` composable never painted a background of its own; the showcase's own `Surface` masked this for the entire milestone. This is a structural gap in a public composable, not a local bug — fixing it changes public API and affects every consumer of `AeroTheme`.
- **Escalated per Rule 4** to an orchestrator checkpoint. User decided: fix the library (not just the consumer), accepting the stated cost (new public parameter, re-review, re-tag).
- **Fix:** `establishBackground: Boolean = true` parameter added to `AeroTheme`, painting `colorScheme.background` via a plain `Box` when true; `AeroThemeBackgroundEstablishmentTest` added as a permanent regression gate.
- **Files modified:** `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt`, `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt`
- **Verification:** Full suite green (433 -> 435 tests, 0 failures); re-reviewed (`f07cc48`); re-tagged (`v3.0.0-verify02`); re-verified by the human on the second attempt.
- **Committed in:** `1d139a7`, `ff577fc`, `f07cc48`

---

**Total deviations:** 2 (1 auto-fixed blocking issue, 1 architectural fix escalated to and approved by the user)
**Impact on plan:** The architectural fix is the plan's actual deliverable of value — VER-05 existed precisely to catch defects a naive consumer would hit that the showcase's own conventions hide, and it did exactly that on the first attempt.

## Issues Encountered

The first Task 3 human-verify attempt is not treated as a plan failure to be minimized: it is the mechanism working as designed. See Accomplishments items 3-4 above for the full account. No auto-fix-attempt-limit was approached; the fix and its gate were both a single, correct, non-iterated pass once the root cause was identified.

## User Setup Required

None — no external service configuration required beyond the JitPack tag pushes already covered above (`git push origin <tag>`), which the executor performed directly.

## Known Stubs

None.

## Threat Flags

None beyond the plan's own pre-declared `threat_model` (T-20-06-01..04, T-20-06-SC) — no new security-relevant surface was introduced by the fix (`establishBackground` is a rendering-only parameter with no I/O, network, or auth surface).

## Next Phase Readiness

- VER-05 is complete: the published JitPack artifact, at `v3.0.0-verify02`, has been proven — by a stranger-shaped consumer outside this repository — to render all eight components correctly on their intended dark background.
- `SHW-16` (the human three-theme sign-off) is **not** touched by this plan and remains open for Plan 20-07, which should be aware that `AeroTheme` now always paints a background by default and that `AeroIconButton`'s focus-ring fix (20-05) is worth a quick spot-check alongside its own three-theme pass.
- **WR-03 (ABI/binary-compatibility gap for `establishBackground`)** is filed as a pending todo — a policy decision (add `@JvmOverloads`, or explicitly accept source-only compatibility as this library's standing practice) that 20-07 or a future maintainer session should resolve before the next real tag/release, not before SHW-16 itself.
- Local `master` remains ~218 commits ahead of `origin/master`; only tags have been pushed. If a future plan needs the reviewed code reachable from a branch (not just a tag), that push has not happened yet and is out of this plan's scope.

---
*Phase: 20-verification*
*Completed: 2026-07-29*

## Self-Check: PASSED

- FOUND: .planning/phases/20-verification/20-06-SUMMARY.md
- FOUND: .planning/phases/20-verification/20-REVIEW.md (addendum section present)
- FOUND commit: 1d139a7 (fix — AeroTheme establishes its themed background)
- FOUND commit: ff577fc (test — gate AeroTheme background establishment)
- FOUND commit: f07cc48 (docs — review addendum for AeroTheme background change)
- FOUND tag: v3.0.0-verify01 (superseded)
- FOUND tag: v3.0.0-verify02 (current, human-verified)
