---
phase: 19-selectors-lists
plan: 08
subsystem: ui
tags: [compose-desktop, kotlin, human-verify, sign-off, showcase, gap-closure]

# Dependency graph
requires:
  - phase: 19-04
    provides: "Showcase state-matrix rows and the failed first three-theme sign-off (19-UAT.md, gaps G1-G4)"
  - phase: 19-05
    provides: "FocusVisibility reducer + AeroSwitch focus-visible gate (G2)"
  - phase: 19-06
    provides: "AeroListItem row growth (G1) + focus-visible gate (G2)"
  - phase: 19-07
    provides: "AeroSegmentedControl label-token fix + recessed-fill darken (G3) + focus-visible gate (G2)"
provides:
  - "The APPROVED three-theme human sign-off for Phase 19 — gaps G1, G2 and G3 confirmed closed by eye on AeroBlue, AeroDark and Classic"
  - "19-UAT.md with no row left at a stale pending or a stale FAILED — tests 1, 4, 6 and 7 re-recorded against the fixes, tests 8-11 moved off pending"
  - "Confirmation that Classic (never previously reviewed for this phase) shows no defects, corroborating G4's root-cause analysis"
  - "G4's re-report with a new fix direction (try darker values specifically for AeroDark/AeroBlue) and an explicit reviewer decision to keep it deferred and close Phase 19"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Split UAT row for a gap-and-a-half: when a single numbered test bundles two independently-tracked gaps (G3 + G4 on test 6), record each half's resolution explicitly rather than passing or failing the whole row"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt
    - .planning/phases/19-selectors-lists/19-UAT.md

key-decisions:
  - "Gate APPROVED. Gaps G1 (AeroListItem row growth), G2 (focus-visible on all three components) and G3 (AeroSegmentedControl label token + recessed-fill direction) are confirmed closed by eye on all three themes. No defect from this pass routes back to 19-05, 19-06 or 19-07."
  - "RECESSED_FILL_DARKEN = 0.20f (19-07's judgement call) is kept as-is — reviewer's verbatim answer: 'в самый раз' (just right). No follow-up tuning."
  - "G4 (AeroOrnamentTokens hoverGlow/borderSelected blowing out on AeroBlue/AeroDark) stays DEFERRED. Reviewer re-confirmed the same known condition, not worse than commit de44669, and added a direction for the eventual fix (try darker values specifically for AeroDark and AeroBlue, not a global change). When asked directly, the reviewer chose to keep G4 deferred and close Phase 19 rather than reopen scope. G4 routes to a separate Phase 16 foundation session."
  - "The reviewer's adjacent-selected-row question (do the two pinned-selected AeroListItem demo rows respond to clicks?) was answered NO by design — those two rows use a hardcoded selected = true literal and an empty onClick lambda, existing solely as the pill-adjacency backstop (UAT test 3). Live selection switching is demonstrated separately by the Inbox/Sent/Drafts group. Recorded as a resolved question in 19-UAT.md, not a gap — routes nowhere."

requirements-completed: [VSEL-01, VSEL-02, VSEL-03, VSEL-04, VLST-01, VLST-02, VLST-03, VLST-04]

coverage:
  - id: D1
    description: "Gap G1 (AeroListItem row growth / pill bounded edge) confirmed closed by eye on all three themes"
    requirement: "VLST-01, VLST-03"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md test 1 (Block A) — reviewer's 2026-07-28 blanket pass, no defect raised"
        status: pass
    human_judgment: true
    rationale: "Visual/layout confirmation on rendered pixels across three themes; automated tests (19-06's AeroListItemLayoutTest) prove the measured-height mechanism, not the rendered result."
  - id: D2
    description: "Gap G2 (focus-visible: no residual focus cue after a mouse click, Tab still draws one, hover unchanged) confirmed closed on AeroSwitch, AeroSegmentedControl and AeroListItem, all three themes"
    requirement: "VSEL-02, VSEL-04, VLST-03"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md tests 4, 8, 9, 10 (Block B) — reviewer's 2026-07-28 pass"
        status: pass
    human_judgment: true
    rationale: "Interaction-sequence confirmation (click, move pointer, Tab, Space) on the live showcase; unit tests in 19-05/19-06/19-07 prove the reducer and wiring, not the rendered/interactive behaviour."
  - id: D3
    description: "Gap G3 (every segment label one colour and legible; exactly one segment reads visibly pushed in, comparable to a pressed AeroButton; recessed darken magnitude judged 'just right') confirmed closed on all three themes"
    requirement: "VSEL-03, VSEL-04"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md tests 6a, 7 (Block C) — reviewer's 2026-07-28 pass, explicit judgement-call answer recorded"
        status: pass
    human_judgment: true
    rationale: "Depth/colour comparison against a reference component (pressed AeroButton) and a subjective magnitude judgement; neither is automatable."
  - id: D4
    description: "UAT tests 8, 9, 10 (AeroSwitch, AeroSegmentedControl, AeroListItem full state matrices, never previously reported) and test 11 (Classic theme, all three components, never previously reviewed) all moved off pending to an explicit passed result"
    requirement: "VSEL-01, VSEL-02, VSEL-03, VSEL-04, VLST-01, VLST-02, VLST-03, VLST-04"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md tests 8-11 (Blocks D, E, F, G) — reviewer's 2026-07-28 blanket 'остальное passed' report, Classic given an explicit standalone statement"
        status: pass
    human_judgment: true
    rationale: "First-ever human confirmation of VLST-02 (the phase's headline fix: hover brightens an already-selected row) and of the full Classic-theme matrix; no automated substitute exists for a visual/behavioural full-state review."
  - id: D5
    description: "Gap G4 (AeroOrnamentTokens brightness blow-out on AeroBlue/AeroDark) remains deferred; re-confirmed by a second independent eye-pass, not worsened, with a new fix direction and explicit reviewer decision to close Phase 19 with it deferred"
    requirement: "cross-cutting (out of Phase 19 scope)"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md test 6b and the G4 gap entry's re-report_2026-07-28 field"
        status: pass
    human_judgment: true
    rationale: "A scope/deferral decision requiring the reviewer's explicit choice; not a pass/fail correctness check."

# Metrics
duration: 12min (Task 2 continuation; Task 1's 4d92c13 audit ran in a prior session)
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 08: Three-Theme Re-Sign-Off Summary

**Phase 19's blocking human gate is APPROVED: gaps G1 (AeroListItem row growth), G2 (focus-visible on all three components) and G3 (AeroSegmentedControl label token + recessed-fill direction) are confirmed closed by eye across AeroBlue, AeroDark and Classic, closing out 19-UAT.md with no row left pending; G4 (ornament-token brightness) is re-confirmed but stays deferred to a separate Phase 16 foundation session by explicit reviewer decision**

## Performance

- **Duration:** ~12 min for this continuation (Task 2's recording pass); Task 1 (showcase audit, commit `4d92c13`) completed in a prior session.
- **Tasks:** 2/2 complete.
- **Files modified:** 3 (2 showcase section files from Task 1, `19-UAT.md` from Task 2).

## Accomplishments

- Ran the full three-theme (AeroBlue, AeroDark, Classic) x seven-block human review against the showcase at commit `4d92c13`. Result: **APPROVED**.
- **Block A (G1):** confirmed closed — long-label-plus-secondary and "Sent" rows keep their text inside the pill's bounded edge on all three themes; adjacent pinned-selected pills still read as two with a gap; plain single-line rows unchanged.
- **Block B (G2):** confirmed closed on `AeroSwitch`, `AeroSegmentedControl` and `AeroListItem` — mouse click + pointer-away leaves no focus cue; Tab still draws one; Space/Enter still operates the control; hover indication unaffected.
- **Block C (G3):** confirmed closed — every segment label reads one colour, legible on all three themes; exactly one segment reads visibly pushed in, comparable in depth to a pressed `AeroButton`; the strip reads darker than the `AeroChip` reference row. Judgement call on `RECESSED_FILL_DARKEN = 0.20f`: reviewer's verbatim answer "в самый раз" (just right) — kept as-is, no follow-up tuning.
- **Blocks D, E, F, G (UAT tests 8, 9, 10, 11):** all four previously-pending rows moved to passed under the reviewer's blanket "остальное passed" ("everything else passed") report. Test 11 (Classic) carries the reviewer's explicit standalone confirmation: "у classic всё выглядит нормально, проблемы только у первых двух тем" (on Classic everything looks fine; the problems are only on the first two themes) — the first-ever review of the full Classic matrix for this phase, and it independently corroborates G4's root-cause analysis (Classic's darker `primary` is why it is unaffected).
- **G4 (deferred, not reopened):** the reviewer re-exercised AeroDark and AeroBlue and confirmed the same known brightness condition persists — not worse than commit `de44669`. New direction recorded: try darker values specifically for these two themes (not a global formula change). When asked directly, the reviewer chose to keep G4 deferred and close Phase 19. G4 remains routed to a separate Phase 16 foundation session.
- **Reviewer question resolved, not a gap:** whether the two "adjacent selected" `AeroListItem` demo rows are expected to be inert on click — confirmed YES by design (hardcoded `selected = true` literal, empty `onClick` lambda; they exist purely as the pill-adjacency backstop for UAT test 3). Live selection switching is demonstrated separately by the Inbox/Sent/Drafts group.
- `19-UAT.md` updated end-to-end: tests 1, 4, 7 re-recorded PASSED against the fixes; test 6 split explicitly into its resolved G3 half (6a) and its still-deferred G4 half (6b) — the row is not silently passed as a whole; tests 8-11 moved off `[pending]`; the `## Gaps` section updated with `status: resolved` for G1/G2/G3 (each carrying a `resolution:` field naming the closing commit and the confirming UAT test) and G4's `re-report_2026-07-28` field appended; `## Summary` counts corrected to 9 passed / 2 issues / 0 pending; top-level frontmatter `status` moved from `diagnosed` to `passed`.

## Task Commits

Each task was committed atomically:

1. **Task 1: full-suite green, audit the showcase for every state the sign-off checklist names** — `4d92c13` (feat) — completed in a prior session.
2. **Task 2: three-theme re-sign-off — record the reviewer's response** — this SUMMARY's accompanying commit (docs/test) records the checkpoint outcome in `19-UAT.md`.

**Plan metadata:** commit to follow this SUMMARY (docs: complete 19-08 plan).

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt` - Task 1: reachability audit, no changes needed beyond what 19-04 already committed (unchanged in this continuation)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt` - Task 1: added the short-primary-plus-secondary row and an `AeroSegmentedControl`/`AeroChip` depth-reference caption (commit `4d92c13`, prior session)
- `.planning/phases/19-selectors-lists/19-UAT.md` - tests 1, 4, 6, 7 re-recorded against the fixes (test 6 split into resolved G3 half + deferred G4 half); tests 8-11 moved off pending; `## Gaps` G1/G2/G3 marked resolved with resolution detail, G4 re-report appended; reviewer's adjacent-selected-row question recorded as a resolved note; `## Summary` counts and frontmatter `status` updated

## Decisions Made

- **Gate APPROVED** with no defects routed back to any of 19-05, 19-06 or 19-07. All three closed gaps (G1, G2, G3) held up under a full three-theme, seven-block review, including the theme (Classic) and the state (VLST-02, `AeroListItem`'s selected+hover brightening) that had never once been reviewed for this phase.
- **`RECESSED_FILL_DARKEN = 0.20f` kept unchanged** — the one tunable left open by 19-07 is confirmed correct by the reviewer's own words, no further work.
- **G4 stays deferred, with an added fix direction** (try darker AeroDark/AeroBlue values specifically, not a global change) and an **explicit reviewer decision** to close Phase 19 with G4 outstanding rather than reopen scope inside this round. This decision is now recorded twice (2026-07-27 initial deferral, 2026-07-28 reaffirmation) in `19-UAT.md`.
- **Test 6 recorded as a split row, not silently passed** — its G3 half (exactly one segment visibly pushed in) is resolved; its G4 half (overall brightness) is not. Recording both halves explicitly, rather than marking the numbered row PASSED outright, keeps the record honest about what the reviewer actually confirmed versus what remains a known, deferred condition.
- **Test 5 (AeroSwitch brightness, G4) left untouched in its own numbered entry** — this plan's resume scope named tests 1, 4, 6 and 7 for re-recording; test 5's substance (the same G4 condition) is addressed via the G4 gap entry's `re-report_2026-07-28` field rather than duplicating the re-report text across two numbered rows.

## Deviations from Plan

None — plan executed exactly as written for this continuation. Task 1 was already complete and verified by the orchestrator before this agent was spawned; Task 2's only action was recording the human reviewer's actual response into `19-UAT.md` and this SUMMARY, with no code changes (per the plan's own prohibition against touching library source in this plan) and no approval inferred beyond what the reviewer explicitly stated.

## Issues Encountered

None. No `./gradlew` re-run was needed — Task 1 already proved the tree green at commit `4d92c13`, and nothing changed since (this plan is presentation-only; its code work concluded with Task 1).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Phase 19 is COMPLETE.** All eight requirements (VSEL-01..04, VLST-01..04) are covered by this sign-off to the extent the reviewer exercised them; the phase's blocking gate is approved.
- G4 (ornament token brightness on AeroBlue/AeroDark) is the one item carried out of Phase 19, by explicit and twice-confirmed user decision. It requires a separate Phase 16 foundation session — changing `AeroOrnamentTokens.kt`'s `hoverGlow`/`borderSelected` derivation would affect every component in the library, including work already accepted in phases 16-18, and must not be done inside a component-scoped phase.
- No defect from this round routes back to 19-05, 19-06 or 19-07 — all three gap-closure plans' fixes are human-confirmed end to end.
- Phase 20 (Verification) can proceed; it depends on Phases 17, 18 and 19 all being complete, which is now the case.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*

## Self-Check: PASSED

`.planning/phases/19-selectors-lists/19-UAT.md` verified present on disk with the updated content (frontmatter `status: passed`, tests 1/4/6/7 re-recorded, tests 8-11 off pending, Gaps G1/G2/G3 marked resolved, G4 re-report appended). Commit `4d92c13` (Task 1) verified present in git log.
