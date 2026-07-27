---
phase: 19-selectors-lists
plan: 06
subsystem: ui
tags: [compose-desktop, kotlin, layout, focus-visible, accessibility, gap-closure]

# Dependency graph
requires:
  - phase: 19-01
    provides: "AeroListItem's clipped Aero selection/hover pill, resolveListItemPillStyle, in-bounds focus stroke geometry"
  - phase: 19-05
    provides: "Internal, Compose-free FocusVisibility/reduce/rememberFocusVisible mechanism and AeroInteractionState.focusVisible field"
provides:
  - "Content-derived AeroListItem row height (36.dp floor, not ceiling) — a primary+secondary row and a wrapped long-label row both grow to contain their text"
  - "Pill and focus Boxes measured against the row's resolved size via matchParentSize(), so both grow with the row instead of collapsing to zero height"
  - "AeroListItem's in-bounds focus stroke gated on focus-visible (state.focusVisible), closing gap G2 for the third and final Phase 19 component"
affects: [19-07, 19-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "matchParentSize() on a Box with no content of its own, sized against the parent Box's OTHER content — the answer to 'fill-the-parent size modifiers silently resolve to zero once the parent's height becomes a minimum instead of a fixed value', now proven on both AeroSwitch's groove (19-02) and AeroListItem's pill/focus Boxes (19-06)"

key-files:
  created:
    - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt
    - library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt

key-decisions:
  - "ROW_MIN_HEIGHT (36.dp, was the fixed height) and ROW_VERTICAL_PADDING (4.dp, new) declared as private vals beside the existing pill constants, each with a KDoc line tying it to gap G1 — matches this file's existing constant-documentation convention exactly."
  - "KDoc mentions of matchParentSize() and heightIn(min = ROW_MIN_HEIGHT) written as `Modifier.matchParentSize` (no trailing parens) in the component's own top-level doc comment, specifically so the acceptance-criteria grep -cF \"matchParentSize()\" count (must equal exactly 2 — the two real call sites) is not inflated by KDoc prose repeating the call verbatim."

requirements-completed: [VLST-01, VLST-03]

coverage:
  - id: D1
    description: "A primary-plus-secondary row and a wrapped-long-label row both grow past 36.dp and contain their text; a single-line row is unchanged at exactly 36.dp; an empty-text row still holds the 36.dp floor; two adjacent selected single-line rows still read as two separate, non-overlapping pills"
    requirement: VLST-01, VLST-03
    verification:
      - kind: unit
        ref: "AeroListItemLayoutTest.kt#singleLineRowMeasuresExactly36Dp, #rowWithPrimaryPlusSecondaryTextGrowsPast36Dp, #emptyPrimaryTextRowStillMeasuresAtLeast36Dp, #wrappedLongPrimaryLabelWithSecondaryTextGrowsPast36Dp, #adjacentSelectedSingleLineRowsEachMeasure36DpAndDoNotOverlap"
        status: pass
    human_judgment: true
    rationale: "The visual result — text inside the pill's bounded edge, two adjacent pills still separate — is confirmed by a human in 19-08 (UAT test 10) across three themes; the measured-height tests here prove the layout mechanism (composed height, no clipping), not the rendered pixels or color contrast."
  - id: D2
    description: "Clicking a clickable row with the mouse and moving the pointer away leaves no focus stroke; Tab still draws the stroke inside the pill's own geometry; a display-only row (onClick == null) is still not a focus stop"
    requirement: VLST-03
    verification:
      - kind: unit
        ref: "AeroListItemSourceTest.kt#aeroListItemGatesTheFocusStrokeOnFocusVisible"
        status: pass
    human_judgment: true
    rationale: "The observable G2 contract on the live AeroListItem (mouse click leaves no stroke, Tab draws one) is confirmed by a human in 19-08 across three themes, per this plan's own <verification> section — the source guard proves the wiring, not the rendered pixels. 19-05's FocusVisibilityTest.kt already unit-tests the shared reducer's behavior; this plan does not re-test the reducer itself."
  - id: D3
    description: "The row-growth contract (heightIn(min = ROW_MIN_HEIGHT), matchParentSize(), no maxLines/TextOverflow) survives future edits"
    requirement: VLST-01
    verification:
      - kind: unit
        ref: "AeroListItemSourceTest.kt#aeroListItemRowGrowthContractSurvives"
        status: pass
    human_judgment: false

# Metrics
duration: 24min
completed: 2026-07-27
status: complete
---

# Phase 19 Plan 06: AeroListItem row growth + focus-visible gate (gaps G1, G2) Summary

**Row grows past its 36.dp floor to contain a two-line or wrapped-long-label row, and the in-bounds focus stroke now gates on the shared focus-visible mechanism instead of the raw focused flag — closing gaps G1 and G2 for AeroListItem (VLST-01, VLST-03)**

## Performance

- **Duration:** 24 min
- **Started:** 2026-07-27T16:18:xx Z (approx, restart after machine reboot interrupted the prior dispatch before any commit)
- **Completed:** 2026-07-27T16:42:xx Z
- **Tasks:** 2
- **Files modified:** 3 (1 new test file, 1 component behavior change, 1 existing test file extended)

## Accomplishments

- Replaced `AeroListItem`'s fixed `.height(36.dp)` with `.heightIn(min = ROW_MIN_HEIGHT)` — the row now grows with its content instead of clipping a primary-plus-secondary row or a wrapped long label.
- Changed the pill `Box` and the focus-stroke `Box` from a fill-the-parent size modifier to `matchParentSize()`, per this plan's `<planner_finding>` — once the row's height became a minimum instead of a fixed value, a fill-the-parent modifier would have silently resolved to zero height inside a `Column`/`LazyColumn`'s unbounded constraint, taking the selection highlight and focus stroke with it.
- Content `Row` switched from `fillMaxSize()` to `fillMaxWidth()` (so it determines the row's height) and gained `ROW_VERTICAL_PADDING` (4.dp) vertical padding, keeping a grown row's text off the pill's rounded edge.
- Added 5 measured-height regression tests in `AeroListItemLayoutTest.kt`, written RED first (2 of 5 failed against the pre-fix fixed-height row) and confirmed GREEN after the fix.
- Changed the focus-stroke `Box`'s guard condition from `state.focused` to `state.focusVisible`, consuming the 19-05 mechanism with no changes to the mechanism itself — a mouse click leaves no residual stroke once the pointer moves away, Tab still draws it, a display-only row (`onClick == null`) is still not a focus stop.
- Added a `nonCommentSource` property and two new source guards to `AeroListItemSourceTest.kt` (8 `@Test` functions total): one for the focus-visible gate (G2), one asserting the row-growth contract survives (`heightIn(min = ROW_MIN_HEIGHT)` + `matchParentSize()` present, `maxLines`/`TextOverflow` absent — G1).
- No file under `library/src/main/kotlin/com/mordred/aero/theme/` was touched (G4 scope fence honored); `AeroSegmentedControl.kt` was not touched (19-07's file).

## Task Commits

Each task was committed atomically:

1. **Task 1: let the row grow with its content so the pill contains its text (G1)** — `08ed471` (feat)
2. **Task 2: gate the row's focus stroke on focus-visible, plus the two source guards (G2)** — `3127f3a` (test)

_TDD note: Task 1 is `tdd="true"`. `AeroListItemLayoutTest.kt` was written and run first against the un-restyled, fixed-height `AeroListItem.kt` — 2 of 5 cases failed as expected (RED), confirming the tests exercise the real defect. Implementation followed, and the full re-run went GREEN (5/5). RED and GREEN landed in a single commit per this plan's task granularity, matching the phase's established single-commit-per-task convention (19-01..19-05)._

## Files Created/Modified

- `library/src/main/kotlin/com/mordred/aero/components/list/AeroListItem.kt` — `.heightIn(min = ROW_MIN_HEIGHT)` row height, `matchParentSize()` pill/focus sizing, `ROW_VERTICAL_PADDING` content padding, `state.focusVisible` focus-stroke gate, updated component KDoc
- `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemLayoutTest.kt` — new, 5 measured-height regression tests
- `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt` — `nonCommentSource` property + 2 new guards (8 `@Test` total)

## Measured Heights (AeroListItemLayoutTest, observed via a temporary diagnostic run, not committed)

| Case | Measured height |
|------|------------------|
| Single-line row (`text = "Inbox"`, no secondary) | **36.0.dp** — unchanged from the shipped single-line geometry |
| Primary + secondary row (`text = "Sent"`, `secondaryText = "Yesterday at 4:32 PM — your message was delivered"`) | **43.0.dp** — grew 7.dp past the old fixed ceiling to contain both lines |
| Wrapped long-label row (long primary label + `secondaryText`, forced to wrap inside a 160.dp-wide container) | **138.0.dp** — grew substantially because the deliberately long label wraps across several lines at that width; the assertion only requires `>= 37.dp`, and the actual value confirms the row scales with however much the content needs, with no ceiling reintroduced |

These three numbers were captured by temporarily adding `println` diagnostics to the already-committed `AeroListItemLayoutTest.kt`, running `./gradlew :library:test --tests "*AeroListItemLayoutTest*" --info`, and then restoring the test file to its exact committed content (`git diff` empty afterward, no extra commit needed) — the committed test file only asserts the `assertHeightIsAtLeast`/`assertHeightIsEqualTo` thresholds; the raw numbers above are recorded here for the human sign-off in 19-08.

## Guard Fail-Then-Pass Proof

Per this project's fail-then-pass convention (VER-06), both new guards in `AeroListItemSourceTest.kt` were proven to FAIL against the pre-fix source before being trusted:

1. Temporarily reverted `AeroListItem.kt`'s focus-stroke condition from `state.focusVisible && enabled && onClick != null` back to `state.focused && enabled && onClick != null`, and its row height modifier from `.heightIn(min = ROW_MIN_HEIGHT)` back to a fixed `.height(36.dp)` (temporarily restoring the `height` import needed for that revert to compile).
2. Ran `./gradlew :library:test --tests "*AeroListItemSourceTest*"`:
   ```
   8 tests completed, 2 failed

   AeroListItemSourceTest > aeroListItemRowGrowthContractSurvives() FAILED
       org.opentest4j.AssertionFailedError: AeroListItem.kt must keep .heightIn(min = ROW_MIN_HEIGHT)
       as its row height modifier — truncation was explicitly rejected because it would mask the
       layout defect rather than fix it (G1)

   AeroListItemSourceTest > aeroListItemGatesTheFocusStrokeOnFocusVisible() FAILED
       org.opentest4j.AssertionFailedError: AeroListItem.kt must gate its in-bounds focus stroke on
       state.focusVisible && enabled && onClick != null so a pointer-acquired focus does not draw
       the stroke, while the row stays focusable and clickable either way (G2)
   ```
   Confirmed FAIL — both new guards correctly reject the pre-fix source; the 6 pre-existing guards stayed green throughout.
3. Restored `AeroListItem.kt`'s focus-stroke condition and row height modifier to their Task 1/Task 2 committed values, and removed the temporarily-restored `height` import.
4. Re-ran `./gradlew :library:test --tests "*AeroListItemSourceTest*"` — exited 0, all 8 tests pass.
5. Ran the full `./gradlew :library:test` suite after restoring — exited 0, no regression anywhere in the library (`AeroButton`/`AeroSlider`/`AeroRangeSlider`/`AeroSwitch`/`AeroSegmentedControl`/`AeroListItem`).

`git diff --stat` for `AeroListItem.kt` after the revert-and-restore cycle showed zero net changes versus the Task 2 commit — the file returned to its committed state exactly, so no extra commit was needed for the proof itself.

## Decisions Made

- **`Modifier.matchParentSize` (no trailing parens) used in KDoc prose instead of the literal call-site text `matchParentSize()`** — Task 1's own acceptance criteria requires `grep -cF "matchParentSize()"` to return exactly 2 (the two real call sites, no more). Writing the two KDoc explanations of the pattern using the exact literal string would have inflated that count to 4 and failed the plan's own gate; rewording to `Modifier.matchParentSize` (matching this codebase's existing KDoc convention of referencing a modifier by name without invoking it) keeps the documentation accurate while keeping the grep gate honest.
- **`private val nonCommentSource` strips KDoc/line/block-comment-opener prefixes from `AeroListItemSourceTest.kt`'s own source-under-test string** — matches this plan's explicit instruction and precedent from the same negative-guard-vs-KDoc-prose problem; discovered mid-implementation that the KDoc explaining `nonCommentSource` itself must avoid a literal `/*` substring (Kotlin's block comments nest, so a literal `/*` inside a `/** ... */` doc comment opens a nested comment that the doc comment's own closing `*/` then closes instead of the outer one, leaving the outer comment unterminated and the rest of the file unparseable) — reworded to describe "a block-comment opener" in prose instead of the literal token.

## Deviations from Plan

**1. [Rule 3 - blocking, mid-task] Nested-block-comment syntax error in `AeroListItemSourceTest.kt`'s own new KDoc.**
- **Found during:** Task 2, immediately after adding the `nonCommentSource` property's KDoc, before the first compile attempt succeeded.
- **Issue:** The KDoc explaining `nonCommentSource` originally read `...starts with `*`, `//` or `/*``. Kotlin's block comments (`/* ... */`, and `/** ... */` is the same construct) nest — encountering a literal `/*` while already inside a block comment opens a second, inner comment. The file's own `*/` that was meant to close the outer KDoc instead closed only that inner nested comment, leaving the outer one open through the rest of the file. The compiler reported `Unresolved reference 'nonCommentSource'`, `Unresolved reference 'sourceFile'`, and `Unclosed comment` at EOF — none of which named the real cause directly.
- **Fix:** Reworded the KDoc to describe "a block-comment opener" in prose instead of writing the literal `/*` token, eliminating the nested-comment trigger with no change to the property's behavior or the guard's intent.
- **Files modified:** `library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt`
- **Commit:** `3127f3a` (folded into Task 2's commit — caught before the task's first successful compile, so no separate fix commit was needed)

No other deviations. Both tasks otherwise followed the plan's `<action>` instructions exactly, and every acceptance-criteria grep/count matched on the first or second attempt.

## Issues Encountered

- The nested-block-comment syntax error above (see Deviations) was the only compile-time surprise; it was diagnosed and fixed within the same task before any commit was made, so it never reached a committed state.
- No other issues — `./gradlew :library:test` and `./gradlew :showcase:compileKotlin` were both green on the first run after Task 2's restore.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- G1 and G2 are both closed for `AeroListItem`. Combined with 19-05 (AeroSwitch, G2) and the upcoming 19-07 (AeroSegmentedControl, G2 + G3), all three components sharing gap G2 will have adopted the same `state.focusVisible` gate by the end of the round.
- `AeroSegmentedControl.kt` was not touched in this plan (explicit scope fence — that is 19-07's file, which runs next).
- No file under `library/src/main/kotlin/com/mordred/aero/theme/` was modified — G4 (ornament token brightness) remains untouched and deferred, per the scope fence.
- The visual result (text inside the pill's bounded edge on the "Sent" row and the long-label row, two adjacent pills still separate, no focus stroke after a mouse click) is NOT yet human-confirmed — that happens in 19-08's re-sign-off (UAT test 10), per this plan's own `<verification>` section.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27*

## Self-Check: PASSED

All created/modified files verified present on disk; both task commits (`08ed471`, `3127f3a`) verified present in git log.
