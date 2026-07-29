---
phase: 19-selectors-lists
plan: 04
subsystem: ui
tags: [showcase, compose-desktop, kotlin, state-matrix, human-verify, sign-off, failed-gate]

# Dependency graph
requires:
  - phase: 19-01
    provides: "Restyled AeroListItem (clipped selection pill, base-then-transform hover composition, in-bounds focus stroke)"
  - phase: 19-02
    provides: "Restyled AeroSwitch (recessed groove, raised thumb, hover/press/focus states, trailing interactionSource parameter)"
  - phase: 19-03
    provides: "Restyled AeroSegmentedControl (raised/recessed segments, Role.RadioButton semantics, in-bounds cues, trailing interactionSource parameter)"
provides:
  - "Showcase state-matrix rows making every Phase 19 state reachable by a reviewer with a mouse and a Tab key"
  - "The executed (and FAILED) three-theme human sign-off whose findings became 19-UAT.md and gap plans 19-05..19-08"
affects: [19-05, 19-06, 19-07, 19-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Additive state-matrix demo rows reusing the existing private SelRow(label, content) / ListRow(label, content) helpers — presentation-only, zero library source touched (following the Phase 18 RangeSection precedent)"
    - "Unexercised review states recorded as `pending`, never as `passed` — the v2.0.3/v2.0.4 false-positive-sign-off control, applied to this plan's own gate"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
    - showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt

key-decisions:
  - "Task 2's blocking human-verify gate was executed on 2026-07-27 and FAILED. No approval was recorded. Findings were persisted verbatim to 19-UAT.md (commit 2964ad0) rather than being resolved inside this plan, because 19-04's own prohibitions forbid changing any library source file here."
  - "G4 (ornament token derivation blowing out on light-primary themes) was DEFERRED out of Phase 19 by explicit user decision — those tokens are Phase 16 foundation and affect work already accepted in phases 16-18. G1/G2/G3 only were routed into the gap round."
  - "Two scope questions the plan raised were answered by the reviewer and closed with no work following: segmented-control long-label behaviour stays widening (no truncation added), and the list pill geometry stays 2.dp inset / 6.dp corner radius."
  - "This SUMMARY is a close-out of a failed gate, written by the orchestrator after the fact. It records no approval; the sign-off itself is re-run by plan 19-08, which supersedes this plan's Task 2."

requirements-completed: []

coverage:
  - id: D1
    description: "Every state named in the phase contract is reachable in the running showcase — five-plus AeroSwitch call sites (including disabled-and-checked), four-plus AeroSegmentedControl call sites (including a disabled strip, an N=1 strip and a long-label strip), and AeroListItem rows covering two adjacent pinned-selected rows, a disabled row, a display-only onClick-less row, and a long primary label with a secondary line pinned selected"
    requirement: "VSEL-01, VSEL-02, VSEL-03, VSEL-04, VLST-01, VLST-02, VLST-03"
    verification:
      - kind: manual
        ref: "commit de44669 — 105 insertions across SelectionSection.kt and ListSection.kt; re-audited in grep form by 19-08 Task 1"
        status: pass
    human_judgment: false
  - id: D2
    description: "A human three-theme (AeroBlue / AeroDark / Classic) x per-state sign-off passes for AeroSwitch, AeroSegmentedControl and AeroListItem"
    requirement: "VSEL-01, VSEL-02, VSEL-03, VSEL-04, VLST-01, VLST-02, VLST-03"
    verification:
      - kind: manual
        ref: "19-UAT.md — sign-off performed 2026-07-27 at commit de44669, AeroBlue and AeroDark only"
        status: fail
    human_judgment: true
    rationale: "FAILED. 11 tests: 2 passed (both scope decisions), 5 issues, 4 pending. Classic was never exercised. Re-run is owned by plan 19-08."

duration: 10min for task 1 (47b826c 16:36 -> de44669 16:46, UTC+3, per commit timestamps); task 2 gate executed same day, outcome FAILED
completed: 2026-07-27
status: gate-failed
---

# Phase 19 Plan 04: Showcase State Matrix + Three-Theme Sign-Off Summary

**Task 1 landed the showcase state-matrix rows that make every Phase 19 state reachable without editing code (`de44669`). Task 2's blocking three-theme human sign-off then RAN and FAILED — 5 defects and 4 unexercised states, persisted to `19-UAT.md` as gaps G1/G2/G3 (routed to gap plans 19-05..19-07) and G4 (deferred out of the phase by explicit user decision). No approval was recorded. The sign-off is re-run in full by plan 19-08.**

## Performance

- **Duration:** 10 min for Task 1 (commit `47b826c` at 16:36 -> commit `de44669` at 16:46, UTC+3). Task 2's gate was executed the same day against the running showcase.
- **Tasks:** 1/2 completed. Task 2 is a blocking human-verify gate that returned defects rather than an approval.
- **Files modified:** 2 (both showcase section files; zero library source files, per the plan's prohibition).

## Accomplishments

- `SelectionSection.kt` (+52 lines): extended the `AeroSwitch` row to a state matrix — a live switch captioned "Notifications", a pinned-checked switch, a pinned-unchecked switch, the existing `enabled = false` example and a second disabled-and-checked example so the flattened accent groove is visible alongside the flattened neutral one. Extended the `AeroSegmentedControl` row to the live "Day" / "Week" / "Month" strip, a `enabled = false` strip, a single-option (N = 1) strip and a strip carrying a deliberately long middle label for the E2 overflow backstop. The existing private `SelRow(label, content)` helper and the section layout were reused unchanged.
- `ListSection.kt` (+53 lines): extended the `AeroListItem` demo column to the three live click-selectable rows ("Inbox" / "Sent" / "Drafts") for the VLST-02 selected-then-hovered combination, two adjacent pinned-`selected = true` rows for the pill adjacency backstop, an `enabled = false` row, a display-only row with `onClick = null` for the no-focus-stop contract, and a long-primary-label-plus-`secondaryText` row pinned selected for the E3 overflow backstop. The existing private `ListRow(label, content)` helper, the section layout and the `AeroBadge` trailing content were kept.
- Caption `Text` added above each new group so a reviewer working the sign-off checklist can locate each case without reading source.
- Task 2's gate was executed against the running showcase (`./gradlew :showcase:run`) at commit `de44669`. It produced a real defect list, which was written to `.planning/phases/19-selectors-lists/19-UAT.md` (commit `2964ad0`) with per-state results, root causes traced to specific file:line locations, and fix directions.

## Task Commits

1. **Task 1: state-matrix showcase rows for AeroSwitch, AeroSegmentedControl, AeroListItem** - `de44669` (feat)
2. **Task 2: three-theme x per-state human sign-off** — no implementation commit. Gate outcome persisted as `2964ad0` (test: persist checkpoint findings as UAT — 5 issues, 4 gaps).

**Plan metadata:** commit to follow this SUMMARY (docs: close out 19-04 failed gate)

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt` - AeroSwitch and AeroSegmentedControl state-matrix rows added (+52)
- `showcase/src/main/kotlin/com/mordred/showcase/sections/ListSection.kt` - AeroListItem state-matrix rows added (+53)

## Gate Outcome — FAILED

The sign-off ran on 2026-07-27 in **AeroBlue and AeroDark only**. Classic — the theme this plan's own checklist named as the one that matters most, because its tokens are fully opaque — was **not** exercised. Full per-state record: `19-UAT.md`.

**Tally:** 11 tests — 2 passed, 5 issues, 4 pending, 0 skipped, 0 blocked.

| # | Component / state | Result | Gap |
|---|---|---|---|
| 1 | AeroListItem — long-label selected row, pill bounded edge | FAILED — text overflows the pill's bottom edge | G1 |
| 2 | AeroSegmentedControl — long-label segment overflow | PASSED — reviewer decision: keep widening, no truncation | — |
| 3 | AeroListItem — pill geometry (2.dp inset / 6.dp radius) | PASSED — keep 2/6; adjacent pills read as two with a clear gap | — |
| 4 | AeroSwitch — focus cue after mouse click | FAILED — highlight persists after the pointer leaves | G2 |
| 5 | AeroSwitch — overall value/brightness (AeroBlue, AeroDark) | FAILED — reads eye-searingly light | G4 |
| 6 | AeroSegmentedControl — overall value/brightness | FAILED — same glare; VSEL-03 unmet on dark themes | G3, G4 |
| 7 | AeroSegmentedControl — selected-segment label colour | FAILED — near-black AND semi-transparent (background token carries `0xCC` alpha) | G3 |
| 8 | AeroSwitch — rest / toggle / hover / press / disabled matrix | pending — never exercised | — |
| 9 | AeroSegmentedControl — hover / Tab / Space / N=1 / disabled matrix | pending — never exercised | — |
| 10 | AeroListItem — VLST-02 selected+hover, focus stroke, display-only, disabled | pending — never exercised (VLST-02 is the phase's headline fix) | — |
| 11 | Classic theme — full matrix, all three components | pending — never exercised | — |

## Decisions Made

- **Scope question 1 (segmented-control long labels):** answered — leave the widening behaviour as is; no truncation is added. Closed, no work follows.
- **Scope question 2 (list pill geometry):** answered — keep `PILL_VERTICAL_INSET = 2.dp` and `PILL_CORNER_RADIUS = 6.dp` rather than moving to 4/8. The reviewer's screenshot confirmed the two adjacent pinned-selected pills read as two separate pills with a clear gap, which is the case the tighter inset exists to protect. Closed, no work follows.
- **G4 deferred out of Phase 19** by explicit user decision. `AeroOrnamentTokens.kt:43` derives `hoverGlow = base.primary.lighten(0.30f)`, a flat boost that lands near white on AeroBlue (`0xFF4FC3F7`) and AeroDark (`0xFF90CAF9`) but stays restrained on Classic (`0xFF5C8ABF`); `AeroColorScheme.kt` additionally sets `borderSelected = primary` in all three schemes. These are Phase 16 foundation tokens affecting every component including work already accepted in phases 16-18, so changing them inside this gap round would alter accepted appearance without re-verification. Scheduled for a separate foundation session.
- **No approval recorded, and none inferred.** The four unexercised states are carried as `pending`, not as passing. This is the specific control the project's v2.0.3 / v2.0.4 false-positive sign-offs exist to enforce.

## Deviations from Plan

**1. [Rule 3 - Blocked, human gate failed] Task 2 returned defects instead of an approval; the plan was left open rather than completed**

- **Found during:** Task 2, the blocking human-verify checkpoint.
- **Issue:** The gate found 5 defects and left 4 states unexercised. The plan's own prohibitions forbid changing any library source file here ("If the review finds a defect, it routes back to Plan 01, 02 or 03"), so the defects could not be fixed in place.
- **Resolution:** Findings were persisted to `19-UAT.md` (`2964ad0`), then `/gsd-plan-phase 19 --gaps` produced gap-closure plans 19-05 (focus-visible reducer + AeroSwitch focus gate, G2), 19-06 (list row growth + list focus gate, G1/G2), 19-07 (segment label token + recessed fill direction + segment focus gate, G3/G2) and 19-08 (the full re-sign-off) — commits `edd002a` and `6f32487`.
- **Files modified:** none beyond Task 1's two showcase files.
- **Commit:** `2964ad0` (UAT record), `edd002a` + `6f32487` (gap plans).

**2. [Close-out] This SUMMARY was written by the execute-phase orchestrator after the fact, not by the plan's own executor**

- **Found during:** the `/gsd-execute-phase 19` safe-resume gate, which detected production commits for 19-04 with no SUMMARY.md present.
- **Issue:** The executor that ran this plan stopped at the checkpoint and the gap-planning round proceeded directly, so no SUMMARY was ever written. The record was accurate but incomplete — `19-UAT.md` itself notes "no 19-04-SUMMARY.md exists ... This is the correct state for a failed gate."
- **Resolution:** Closed out manually with the user's explicit confirmation, reconstructed entirely from `19-UAT.md`, `19-04-PLAN.md` and the commit record. Nothing here is inferred beyond those sources, and no approval is recorded. Task 2's sign-off is re-run in full by plan 19-08, which explicitly covers both the three closed gaps and the four states that stood pending.
- **Commit:** this SUMMARY's own docs commit.

### Auth Gates

None encountered — this plan involves no network or auth-gated tooling.

## Issues Encountered

- The gate could not be completed in one session: only two of three themes were walked, and 4 of 11 checklist rows were never exercised. This is recorded honestly rather than being closed out optimistically — plan 19-08 exists specifically to finish the matrix, and its acceptance criteria require an explicit per-block, per-theme result with nothing silently omitted.
- Execution environment: subagent dispatch for this phase must use `run_in_background: true`. Synchronous dispatch was aborted twice mid-plan at 22 and 39 minutes; background dispatch completed a 26-minute run.

## Known Stubs

None. The showcase rows are fully wired live components, not placeholders — that is precisely what made the gate's findings real.

## Next Phase Readiness

- The showcase state matrix is in place and is the surface plan 19-08's Task 1 re-audits in grep form before re-opening the human gate.
- G1, G2 and G3 are owned by plans 19-06, 19-05 and 19-07 respectively. G4 is out of Phase 19 and must not be pulled into this round.
- Phase 19 **cannot** be marked complete on this plan's record. The blocking gate is unpassed until 19-08 returns an approval.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-27 (Task 1) — Task 2 gate FAILED, superseded by plan 19-08*

## Self-Check: PASSED

Both modified files found on disk; Task 1's commit (`de44669`) found in git history with the expected 105-line diff across the two showcase sections. No approval is claimed by this SUMMARY — the gate's recorded outcome is FAILED, matching `19-UAT.md`.
