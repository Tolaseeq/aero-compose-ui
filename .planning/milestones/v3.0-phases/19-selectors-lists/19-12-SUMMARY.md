---
phase: 19-selectors-lists
plan: 12
subsystem: ui
tags: [compose-desktop, kotlin, human-verify, sign-off, showcase, gap-closure]

# Dependency graph
requires:
  - phase: 19-09
    provides: "CR-02 focus-visible reducer fix + WR-01 AeroButton/AeroOutlinedButton focus-glow gate"
  - phase: 19-10
    provides: "CR-01 raised-segment fill contrast fix + 12-assertion WCAG guard (later retuned by WR-12)"
  - phase: 19-11
    provides: "WR-03 segment identity keying + WR-04 single hover emitter across AeroSwitch/AeroSegmentedControl/AeroListItem"
provides:
  - "Reachability audit: option-list-change affordance added for block F (WR-03), all other checkpoint blocks already reachable"
  - "Human re-sign-off recorded per block for CR-01, CR-02, WR-01, WR-03, WR-04 — blocks A-F PASSED, block G FAILED"
  - "New gap G5: AeroSegmentedControl's raised fill has drifted into a bespoke colour language unlike AeroButton, with root cause (wrong content-token target) and required fix direction (unify with AeroButton's code path) recorded"
  - "Depth/recess judgement explicitly recorded as human-accepted and out of any follow-up gap's scope"
affects: [19-13 or successor gap-closure plan]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Demo affordance capped to the minimum needed to exercise one named checkpoint block, with a source comment naming the gap ID (WR-03) it exists for — showcase-only, no library code"

key-files:
  created: []
  modified:
    - showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt
    - .planning/phases/19-selectors-lists/19-UAT.md

key-decisions:
  - "Gate NOT approved this round. Blocks A-F (CR-01 legibility, CR-02 focus, WR-01 button focus, WR-03 identity keying, WR-04 hover) all PASSED per the developer's explicit per-block verdict. Block G FAILED with a new gap, G5."
  - "Block B's depth/recess judgement is recorded as ACCEPTED and explicitly excluded from G5's scope — the developer's own words: 'по глубине всё норм вроде, всё понятно, что вдавлено и что нет' (depth-wise it's fine, it's clear what's pushed in and what isn't)."
  - "G5's root cause: the segment label is locked to colors.onSurface (by design, per gap G3's resolution — a state-dependent label colour must not be reintroduced), so CR-01/WR-12's darken had to chase the raised fill down to 0.58f/0.61f to clear the 4.5:1 floor against that token. AeroButton's label is not locked to onSurface and needs no such bespoke darkening, so its rest fill stays light blue while the segment's has become a dark navy plate."
  - "G5's fix direction, per the developer's explicit instruction: unify AeroSegmentedControl's raised fill AND its label content token with AeroButton/AeroButtonSurface's own code path, rather than retuning segment-specific darken constants further. Not attempted here — this plan's scope fence forbids library source changes."
  - "No per-theme breakdown was given by the developer for blocks A-F (only a per-block verdict); recorded exactly that way, without fabricating per-theme claims. Two attached screenshots, both on AeroDark, are the directly-evidenced theme."

requirements-completed: [VSEL-02, VSEL-03, VSEL-04, VLST-03]

coverage:
  - id: D1
    description: "Reachability audit: all six checkpoint blocks (A-F) confirmed reachable in the running showcase before the human was asked to look; block F (option-list identity, WR-03) needed a new demo affordance, added and captioned"
    requirement: "VSEL-02, VSEL-03, VSEL-04, VLST-03"
    verification:
      - kind: automated_ui
        ref: "./gradlew build (commit fc04d17) — green tree; grep -c 'WR-03' SelectionSection.kt >= 1"
        status: pass
    human_judgment: false
  - id: D2
    description: "Blocks A, C, D, E, F confirmed PASSED by the developer's explicit per-block verdict (CR-01 legibility, CR-02 focus, WR-01 button focus, WR-04 hover, WR-03 identity keying)"
    requirement: "VSEL-02, VSEL-04, VLST-03"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md tests 12, 14, 15, 16, 17 (Round 3, Blocks A/C/D/E/F) — developer's verbatim 2026-07-28 response"
        status: pass
    human_judgment: true
    rationale: "Visual legibility and interaction-sequence confirmation on the live showcase; no automated substitute for a rendered/interactive judgement."
  - id: D3
    description: "Block B (depth/recess judgement) recorded: depth/recess ACCEPTED as-is; the button-match question answered YES, which is the substance of block G's failure rather than a separate open item"
    requirement: "VSEL-03"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md test 13 (Round 3, Block B) — developer's verbatim answer"
        status: pass
    human_judgment: true
    rationale: "Subjective depth/value judgement against a reference component; not automatable."
  - id: D4
    description: "Block G FAILED — new gap G5 opened, recording both the symptom (bespoke dark-navy colour language unique to AeroSegmentedControl) and the root cause (label locked to onSurface forced the fill dark to clear contrast, instead of adopting the button's own on-fill content token)"
    requirement: "VSEL-03, VSEL-04"
    verification:
      - kind: manual_procedural
        ref: "19-UAT.md test 18 (Round 3, Block G) and Gap G5 entry — developer's verbatim response, two AeroDark screenshots"
        status: fail
    human_judgment: true
    rationale: "A genuine visual defect reported by the developer against the shipped component; routes to a future gap-closure plan, not fixed here per this plan's scope fence."

# Metrics
duration: ~10min (Task 2 continuation; Task 1's fc04d17 audit ran in a prior session)
completed: 2026-07-28
status: complete
---

# Phase 19 Plan 12: Gap-Round Re-Sign-Off (CR-01/CR-02/WR-01/WR-03/WR-04) Summary

**Blocks A-F (legibility, focus, and identity-keying fixes) all PASSED per the developer's explicit verdict, but block G surfaced a new gap (G5): `AeroSegmentedControl`'s raised fill has drifted into a bespoke dark-navy colour language distinct from `AeroButton`, because its label stayed locked to `colors.onSurface` instead of adopting the button's own on-fill content token — the round is NOT closed**

## Performance

- **Duration:** ~10 min for this continuation (Task 2's recording pass); Task 1 (reachability audit, commit `fc04d17`) completed in a prior session.
- **Tasks:** 2/2 complete.
- **Files modified:** 2 (`SelectionSection.kt` from Task 1, `19-UAT.md` from Task 2).

## Accomplishments

- Task 1 (prior session) confirmed the tree green and audited all six checkpoint blocks for reachability: only block F (segment identity under an option-list change, WR-03) needed a new affordance — a "Remove first" / "Restore full list" pair of demo controls added to `SelectionSection.kt`, captioned and commented with `WR-03`. All other blocks were already reachable.
- Task 2 recorded the developer's actual per-block response against that reachable showcase:
  - **Block A (CR-01 legibility):** PASSED.
  - **Block B (CR-01 judgement call):** depth/recess explicitly ACCEPTED ("по глубине всё норм вроде, всё понятно, что вдавлено и что нет" — depth-wise it's fine, clear what's pushed in and what isn't). The "should it match the button more closely" question is answered YES — this is the substance of block G's failure, not a separate open item.
  - **Blocks C, D (CR-02, WR-01 focus-visible on switch/segment/row and both button variants):** PASSED.
  - **Block E (WR-04 hover after emitter change):** PASSED.
  - **Block F (WR-03 segment identity under option-list change):** PASSED.
  - **Block G (anything else):** FAILED — a new, distinct defect, NOT the deferred G4 ornament-brightness condition. Developer's verbatim report (quoted in full in `19-UAT.md`): `AeroSegmentedControl` has acquired a colour that belongs only to it; it must be built in the image and likeness of an ordinary button, one unified style. Two screenshots on AeroDark show the segmented strips reading as dark navy plates directly beside `AeroButton`'s light-blue rest fill.
- Opened gap **G5** in `19-UAT.md` with both the symptom and its root cause: CR-01's original darken (`0.45f`/`0.61f`, plan 19-10) and WR-12's retune (`RAISED_FILL_TOP_DARKEN` pushed to `0.58f`, commit `5cca3a1`) were both chasing contrast against `colors.onSurface` — the label token the segment is deliberately locked to (per G3's resolution, which forbids a state-dependent label colour). `AeroButton`'s label is not locked to that token and reads near-white against its own lighter fill without any bespoke darkening (per 19-10-SUMMARY.md's own key-decisions), so it never needed to go dark to clear contrast. The fix direction, per the developer's explicit instruction, is to unify the segment's raised fill AND its label content token with `AeroButton`/`AeroButtonSurface`'s own code path — not to retune the segment-specific darken constants further. The recessed/selected treatment (Phase 17 pressed-button reuse) is unaffected and explicitly preserved.
- `19-UAT.md` updated: appended Round 3 (tests 12-18, one per block A-G) alongside the two existing intact rounds; G5 gap entry appended after G4 without touching G1-G4; `## Summary` counters updated (11->18 total, 9->15 passed, 2->3 issues); frontmatter `status` moved from `passed` to `gap-open`; a closing Notes bullet records the round's outcome.
- No library source was touched by either task (`git status --porcelain library/src` empty both times).

## Task Commits

Each task was committed atomically:

1. **Task 1: green tree, every state in this round's block list reachable** — `fc04d17` (feat) — completed in a prior session.
2. **Task 2: three-theme re-sign-off — record the developer's response** — `674bdf7` (docs) — records the checkpoint outcome in `19-UAT.md`.

**Plan metadata:** commit to follow this SUMMARY (docs: complete 19-12 plan).

## Files Created/Modified

- `showcase/src/main/kotlin/com/mordred/showcase/sections/SelectionSection.kt` - Task 1: added an "AeroSegmentedControl (identity)" demo strip with "Remove first" / "Restore full list" controls, captioned and commented `WR-03`, for block F. No other blocks needed changes.
- `.planning/phases/19-selectors-lists/19-UAT.md` - Task 2: appended Round 3 (tests 12-18, blocks A-G) with the developer's verbatim response and translation; opened gap G5 with symptom + root cause + fix direction + explicit depth/recess-out-of-scope note; updated `## Summary` counters and frontmatter `status`; added a closing Notes bullet. Rounds 1 and 2 (tests 1-11, gaps G1-G4) are untouched apart from the Summary counters and top-level status field.

## Decisions Made

- **Round NOT approved.** Six of seven blocks passed, but block G's failure is a genuine new defect (G5), not the twice-deferred G4 condition — it is recorded and routed accordingly, not folded into G4 and not silently passed.
- **Depth/recess judgement accepted, explicitly out of G5's scope.** The developer's own words separate the depth question (fine) from the colour-identity question (not fine); G5's `scope_note` records this distinction so a future fix does not also re-touch the recess magnitude the developer just re-confirmed.
- **G5's root cause recorded as a content-token mismatch, not merely "too dark."** The darkening is a symptom of the segment's label being locked to `onSurface` while the button's label uses its own on-fill token; recording this (rather than just "make it lighter") is what makes the fix direction — unify with `AeroButton`'s code path — actionable for whichever plan closes G5.
- **No per-theme claims fabricated.** The developer answered per block, not per theme, and attached AeroDark-only screenshots; blocks A-F are recorded as passed exactly as reported, without inventing a per-theme breakdown the developer did not give and without downgrading the explicit PASS to pending.
- **G5 not fixed in this plan.** Per 19-12's scope fence (no library source changes; a defect routes back to the plan that owns the code), G5 is recorded only. It requires a new gap-closure plan.

## Deviations from Plan

None — plan executed exactly as written for this continuation. Task 1 was already complete and verified before this agent was spawned; Task 2's only action was recording the developer's actual response into `19-UAT.md` and this SUMMARY, with no code changes and no approval inferred beyond what the developer explicitly stated.

## Issues Encountered

None. No `./gradlew` re-run was needed — Task 1 already proved the tree green at commit `fc04d17`, and this plan's Task 2 work is presentation/record-keeping only.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Phase 19's gate is NOT closed.** G5 is open: `AeroSegmentedControl`'s raised fill needs to be rebuilt against `AeroButton`'s own fill + content-token code path rather than further segment-specific darken tuning. This requires a new gap-closure plan (library source change), which this plan explicitly could not perform.
- G4 (ornament-token brightness, Phase 16 territory) remains separately deferred and untouched by this round.
- No defect from this round routes back to 19-09, 19-10 or 19-11's underlying mechanisms as broken — CR-01/CR-02/WR-01/WR-03/WR-04 are all confirmed working as implemented. G5 is a new, distinct visual-identity finding about the value chosen to clear a contrast floor, not a mechanism defect in any of those three plans.

---
*Phase: 19-selectors-lists*
*Completed: 2026-07-28*

## Self-Check: PASSED

`.planning/phases/19-selectors-lists/19-UAT.md` verified present on disk with Round 3 appended (tests 12-18), G5 gap entry present, `## Summary` counters at 18/15/3, frontmatter `status: gap-open`. Commits `fc04d17` (Task 1) and `674bdf7` (Task 2, UAT record) verified present in git log.
