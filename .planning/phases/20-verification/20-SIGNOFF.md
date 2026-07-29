---
phase: 20-verification
date: 2026-07-29
launch_command: "./gradlew :showcase:run"
gate_status: PASSED
# ^ PASSED carries a recorded gap: Block D (real-OS DPI, 125%/200%) was WAIVED by the maintainer,
#   not performed. See "Block D" and "Verdict" below before treating this as an unconditional pass.
verified_by: "maintainer (2026-07-29)"
dpi_scales: [125%, 200%]
dpi_theme: AeroBlue
---

# Phase 20 v3.0 Three-Theme Visual Sign-off (SHW-16)

**Phase:** 20-verification
**Date:** 2026-07-29
**Launch command:** `./gradlew :showcase:run`
**Milestone gate:** v3.0 "Glass Refinement" — last gate before ship.

> **`gate_status: PASSED` — but read this before trusting it as unconditional.** The maintainer's
> coherence pass (Blocks A/B/C, 100% DPI) is a real, capture-backed PASS. **Block D (the two
> non-100%-DPI passes, 125% and 200%) was explicitly WAIVED by the maintainer, not performed.**
> They were asked directly whether they had run them and answered **"Принять без DPI-прогонов"**
> ("Accept without the DPI passes"). The gap this leaves is recorded in full under "Block D" below
> and is not softened here or anywhere else in this document.

---

## D-03 Ordering Proof

This sign-off runs LAST, after the mechanical gates and the code review are both green — a verdict
recorded before that ordering holds is void (SHW-16/ordering, D-03; T-20-07-02).

- **`.planning/phases/20-verification/20-REVIEW-FIX.md`** frontmatter `status:` field, verbatim:
  **`all_critical_and_warnings_fixed`** — 0 critical, 0 warning findings remain open (2 info-level
  items, WR-01 and IN-01, are recorded as deferred to a future foundation-tier plan; neither blocks
  this sign-off per that document's own "Open Findings Remaining After This Plan" section).
- **`.planning/phases/20-verification/20-REVIEW.md`** addendum (commits `1d139a7`/`ff577fc`,
  reviewed 2026-07-29) — 0 critical, 1 warning (WR-03, ABI/binary-compatibility policy gap, filed
  as a backlog todo, not a sign-off blocker), 2 info (IN-03, IN-04). No blocker.
- Both are dated 2026-07-29, before this document's own date. The ordering holds.

## Automated Gates

| Gate | Command | Result | Date |
|------|---------|--------|------|
| VER-01 (gradient proportionality) | `./gradlew :library:test --tests "*VER01*"` | PASS — 9 tests, 0 failures | 2026-07-29 |
| VER-02 (surface clip order) | `./gradlew :library:test --tests "*VER02*"` | PASS — 8 tests, 0 failures | 2026-07-29 |
| VER-03 (baseline size/corner-radius snapshot) | `./gradlew :library:test --tests "*VER03*"` | PASS — 6 tests, 0 failures | 2026-07-29 |
| VER-04 (button semantics/invocation-count) | `./gradlew :library:test --tests "*AeroButtonSemanticsTest*"` | PASS — 6 tests, 0 failures | 2026-07-29 |
| D-13 contrast (WCAG label-contrast regression) | `./gradlew :library:test --tests "*ContrastRegression*"` | PASS — 7 tests, 0 failures (includes the named, guarded AeroDark recessed-segment exception — see Block C below) | 2026-07-29 |
| VER-05 (external scratch-consumer verdict) | human-verify, `20-06-SUMMARY.md` | PASS — second attempt, tag `v3.0.0-verify02`, all eight components render on correct dark background, nothing clipped | 2026-07-29 |
| VER-07 (button/outlined-button per-state matrix) | `./gradlew :library:test --tests "*VER07*"` | PASS — added 20-08, converts Block B to an automated gate (see Block B below) | 2026-07-29 |
| VER-08 (segment label-flip invariance) | `./gradlew :library:test --tests "*VER08*"` | PASS — added/strengthened 20-08/20-09, converts A4 to an automated gate | 2026-07-29 |
| AeroIconButton focus-visible wiring | `./gradlew :library:test --tests "*AeroIconButtonFocusVisibleWiringTest*"` | PASS — added 20-08, converts A7 to an automated gate | 2026-07-29 |
| VER-09 (fractional-density rounding, `Density(1.25f)`/`Density(2f)`) | `./gradlew :library:test --tests "*VER09*"` | PASS — see "Block D" for what this test does and does NOT cover | 2026-07-29 |
| Full suite (build gate) | `./gradlew :library:test` | PASS — 468 tests, 0 failures | 2026-07-29 |
| Showcase compiles | `./gradlew :showcase:compileKotlin` | PASS — BUILD SUCCESSFUL | 2026-07-29 |

Every gate above is green and dated before this document's own D-03 ordering proof closed — this
table is the auditable evidence that ordering held.

## Scope Note — D-01 Hybrid Sign-off

Per D-01, this sign-off is **hybrid**, not a flat per-state matrix over all eight components:

- **Block A** — a cross-component **coherence** pass over all eight restyled components
  (`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`,
  `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`) on all three themes, done on the showcase's
  permanent **Verification** section (`VerificationSection.kt`, built by 20-02).
- **Block B** — a full per-state matrix (default / hover / press / focus / disabled) for
  **`AeroButton` and `AeroOutlinedButton` ONLY** — the two components that changed after their own
  Phase 17 sign-off (19-09's `focusVisible` move / WR-01, the G5 segmented-control-fill
  unification, and 20-04's contrast fix).
- **`AeroSlider`, `AeroRangeSlider` and `AeroProgressBar` are deliberately NOT re-reviewed
  state-by-state this round.** Nothing has touched them since their own approval at 18-04. Their
  absence from Block B is a recorded decision (D-01), not an omission.
- **Block D** — two DPI passes, both on **AeroBlue only**, at exactly **125%** and exactly
  **200%** (D-02). One theme suffices because the failure mode hunted is geometric (rounding,
  proportionality), not chromatic.

**Post-20-08/20-09 update:** Block B (the full per-state matrix), A4 (the segmented-control
mid-tween label flip) and A7 (the icon-button focus-ring wiring) are **no longer walked live by a
human in this sign-off session.** Plan 20-08 converted each into a permanent, re-executing Compose
UI-test gate — `VER07ButtonStateMatrixTest`, `VER08SegmentLabelFlipTest`, and
`AeroIconButtonFocusVisibleWiringTest` respectively (see Automated Gates table above). This is a
recorded scope reduction, not a silent omission: those three rows below are marked **AUTOMATED**,
not left blank and not claimed as a human PASS.

## Sign-off Table

> One verdict cell **per theme, per row**. No cell may be filled by carrying over or deriving
> another theme's verdict (T-20-07-03) — each is answered independently, backed by its own
> capture. A theme with no capture on file is recorded as **FAIL**, never an implicit pass.
>
> **Transcription note:** the maintainer's actual verdict was given as a single combined statement
> after reviewing all three themes live with per-theme captures already on file — not as three
> separately-worded per-theme sentences. The PASS recorded in each theme's cell below transcribes
> that combined statement faithfully; it is not an invented per-theme elaboration.

### Block A — Cross-Component Coherence (all three themes, all eight components)

| # | Item | AeroBlue | AeroDark | Classic | Capture |
|---|------|----------|----------|---------|---------|
| A1 | All eight read as ONE material family, not eight separate approximations of it | PASS | PASS | PASS | `signoff-capture/aeroblue-coherence-100.png` / `aerodark-coherence-100.png` / `classic-coherence-100.png` |
| A2 | Fill/gloss/bevel/rim treatment is consistent in KIND across the eight (magnitude may differ per component; language must not) | PASS | PASS | PASS | same captures |
| A3 | Contrast-fixed label on `AeroButton`/`AeroOutlinedButton`/`AeroSegmentedControl` reads legibly, not garish (cross-check `20-04-SUMMARY.md`'s measured ratios) | PASS | PASS | PASS | same captures — see note below |
| A4 | `AeroSegmentedControl` mid-selection tween (~150ms): label flips candidate once as fill crosses contrast midpoint — reads acceptably? (open judgment per 20-04, not a defect claim) | AUTOMATED | AUTOMATED | AUTOMATED | covered by `VER08SegmentLabelFlipTest` (20-08/20-09) — not human-walked this session |
| A5 | `AeroTheme` now paints its own background (`1d139a7`) — the showcase's own redundant `Surface(colors.background)` on top produces no banding/double-tone at window edges | PASS | PASS | PASS | same captures |
| A6 | `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` — out of restyle scope, but affected if D-12's shared constants moved. Look at them; record what you see | PASS (see note) | PASS (see note) | PASS (see note) | same captures |
| A7 | `AeroIconButton`'s focus ring (WR-02/20-05 fix): appears on keyboard Tab focus only, NOT on a plain mouse click | AUTOMATED | AUTOMATED | AUTOMATED | covered by `AeroIconButtonFocusVisibleWiringTest` (20-08) — not human-walked this session |
| A8 | No component clipped, no bloom sliced, no seam visible where the design says there should be none | PASS | PASS | PASS | same captures |

**A3 note.** The maintainer did more than accept the shipped contrast fix at this checkpoint — during
plan 20-09 they rejected the original surface-polarity label-colour result on sight (running live)
and directed the white-on-both-polarities decision that ultimately shipped (commits `24e6705` /
`c943d43`). This sign-off's A3 PASS is on top of that already-decided, already-reviewed result, not
a first look at it.

**A6 note — non-blocking finding, recorded, not fixed here.** The maintainer's verdict, verbatim:

> У RadioButton при наведении тень квадратная, а должна быть круглая. Но это неблокирующий баг
> конкретно его, просто фиксируем на будущее

Translation for context (the verdict itself is quoted above, not translated away): on hover,
`AeroRadioButton`'s shadow renders square where it should render round. The maintainer classified
this explicitly as non-blocking and specific to that one component — not a phase blocker, not a
defect in the shared theme/primitives layer that the rest of A6 is checking for. Filed as a pending
todo: `.planning/todos/pending/2026-07-29-aeroradiobutton-hover-shadow-square-not-round.md`. Not
fixed as part of this plan.

**Then, "остальное pass"** ("everything else passes") — the maintainer's own closing words for
Block A/B/C at 100% DPI, given after the AeroRadioButton note above and before being asked about
Block D.

### Block B — Per-State Matrix, `AeroButton` and `AeroOutlinedButton` ONLY (D-01, all three themes)

> **Converted to an automated gate by plan 20-08 (`VER07ButtonStateMatrixTest`).** Not walked live
> by the maintainer in this session — recorded as AUTOMATED, not as a human PASS, per the scope-note
> update above.

| # | Component | State | AeroBlue | AeroDark | Classic |
|---|-----------|-------|----------|----------|---------|
| B1 | AeroButton | default | AUTOMATED | AUTOMATED | AUTOMATED |
| B2 | AeroButton | hover | AUTOMATED | AUTOMATED | AUTOMATED |
| B3 | AeroButton | press | AUTOMATED | AUTOMATED | AUTOMATED |
| B4 | AeroButton | focus (Tab-acquired only; mouse click must leave no residual ring) | AUTOMATED | AUTOMATED | AUTOMATED |
| B5 | AeroButton | disabled | AUTOMATED | AUTOMATED | AUTOMATED |
| B6 | AeroOutlinedButton | default | AUTOMATED | AUTOMATED | AUTOMATED |
| B7 | AeroOutlinedButton | hover | AUTOMATED | AUTOMATED | AUTOMATED |
| B8 | AeroOutlinedButton | press | AUTOMATED | AUTOMATED | AUTOMATED |
| B9 | AeroOutlinedButton | focus (Tab-acquired only; mouse click must leave no residual ring) | AUTOMATED | AUTOMATED | AUTOMATED |
| B10 | AeroOutlinedButton | disabled | AUTOMATED | AUTOMATED | AUTOMATED |

**`AeroSlider`, `AeroRangeSlider` and `AeroProgressBar` are deliberately NOT re-reviewed
state-by-state this round** (D-01) — nothing has touched them since their approval at 18-04's
sign-off. No rows for them appear above; this is a recorded decision, not an omission.

### Block C — Routed Items

One row per `20-REVIEW-FIX.md` finding disposed `routed-to-signoff` (none exist — both open
info-severity items there, WR-01 and IN-01, are disposed "deferred to backlog/future
foundation-tier plan," not routed to this sign-off), plus one row per open finding recorded by
`20-04-SUMMARY.md`:

| # | Item | Theme | Verdict | Notes |
|---|------|-------|---------|-------|
| C1 | 20-REVIEW-FIX.md routed-to-signoff findings | — | **none** | Confirmed: `20-REVIEW-FIX.md`'s "Open Findings Remaining After This Plan" disposes WR-01/IN-01 to backlog, not to this sign-off. |
| C2 | ~~AeroDark recessed segment, label-to-`fillBottom` = 4.079 (white label)~~ — **SUPERSEDED AND VOID.** This finding described the per-fill `resolveLabelColor` algorithm, which plan 20-09 retired entirely. Neither the number nor the affected stop still describes the code. Replaced by C2a below. | — | **void** | Do not judge this row. Kept only so a reader of `20-04-SUMMARY.md` or `20-08-SUMMARY.md` — both of which still quote 4.079 — can see it was withdrawn. |
| C2a | ~~Recessed segment, `fillTop`, **dark** label token: AeroBlue 3.218 / AeroDark 3.538~~ — **ALSO SUPERSEDED AND VOID.** These were the surface-polarity rule's numbers. The maintainer reviewed that rule running on 2026-07-29, rejected it on appearance, and approved white on both polarities in AeroBlue and AeroDark. There is no dark label token in any shipped scheme any more. | — | **void** | Second staleness of this row. Replaced by C2b, which deliberately cites a test rather than transcribing numbers, so it cannot go stale a third time. |
| C2b | **Accepted sub-floor label contrast in AeroBlue and AeroDark.** With white on opaque fills, the label sits below the WCAG 4.5:1 normal-text floor on most filled-button and raised-segment rest/hover/press cases — worst case **2.801** (AeroBlue filled-hover `fillTop`). The maintainer approved this on 2026-07-29 in exchange for visual coherence with Classic, having been shown the cost in writing and having reviewed the white variant running across all three themes. **Live figures are pinned and regression-bounded in `AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`** — read them from there, never from prose. Nothing to re-decide here; this row exists so the sign-off record states plainly that the milestone ships with a known, bounded accessibility deviation. Reconfirmed unchanged at this 20-07 sign-off — the maintainer's "остальное pass" verdict covers this row as-is; no further decision was requested or given on it. | AeroBlue, AeroDark | **accepted** | Note what the decision also BOUGHT: white fully clears AeroBlue's recessed segment (6.525 / 4.602), which the polarity rule could not. Classic is unaffected apart from one marginal case (filled-hover `fillTop` 4.455, missing by 0.045). Known remedy, deliberately not taken now: darken the opaque fills as Classic does — tracked in `.planning/todos/pending/`. |

### Block D — DPI (AeroBlue only, filled by Task 3)

> **NOT VERIFIED BY A HUMAN.** Neither row below is a PASS, a FAIL, or an N/A — both were
> **explicitly waived by the maintainer's own decision.** Asked directly whether they had performed
> the 125%/200% real-OS-DPI passes (which require changing the Windows display scale and
> relaunching the showcase), the maintainer answered **"Принять без DPI-прогонов"** ("Accept
> without the DPI passes"). Do not read the blank capture columns below as an oversight — they are
> the recorded evidence that these two passes did not happen.
>
> **What still covers this ground.** `VER09FractionalDensityRoundingTest` exercises `Density(1.25f)`
> and `Density(2f)` (see the Automated Gates table above) and is green.
>
> **What remains genuinely uncovered as a result.** `VER09`'s `LocalDensity` substitution simulates
> density in **layout only**. It does not exercise the Windows compositor's fractional scaling of
> the final rendered surface, Skia's rasterisation at that scale, or font hinting at that scale. That
> residue — real-OS compositor/rasteriser/hinting behavior at 125% and 200% — ships **unverified**
> in this milestone, by the maintainer's own informed choice.

| # | Scale | Theme | Verdict | Capture | Notes |
|---|-------|-------|---------|---------|-------|
| D1 | 125% (exact) | AeroBlue | **NOT VERIFIED — waived** | none (pass not performed) | Maintainer accepted SHW-16 without running this pass. Layout-level fractional-density coverage only, via `VER09FractionalDensityRoundingTest` (`Density(1.25f)`). Real compositor/rasteriser/hinting behavior at 125% is unverified. |
| D2 | 200% (exact) | AeroBlue | **NOT VERIFIED — waived** | none (pass not performed) | Maintainer accepted SHW-16 without running this pass. Layout-level fractional-density coverage only, via `VER09FractionalDensityRoundingTest` (`Density(2f)`). Real compositor/rasteriser/hinting behavior at 200% is unverified. |

---

## Verdict

`gate_status`: **PASSED** — the maintainer's own stated verdict, transcribed verbatim below. This is
**not** an unconditional pass: it is a pass on Blocks A/B/C at 100% DPI (Block B/A4/A7 covered by
20-08's automated gates rather than walked live), **plus an explicit, informed waiver of Block D**
rather than a passing Block D verdict. Both parts are recorded; neither is softened into the other.

`verified_by`: **maintainer (2026-07-29)**

**The verdict, verbatim (Russian original):**

> У RadioButton при наведении тень квадратная, а должна быть круглая. Но это неблокирующий баг
> конкретно его, просто фиксируем на будущее
>
> остальное pass

**On Block D specifically**, asked directly whether the 125%/200% real-DPI passes had been
performed, the maintainer chose:

> Принять без DPI-прогонов

— i.e. accept SHW-16 without them, with the gap explicitly recorded (see "Block D" above).

**Non-blocking finding filed, not fixed:** `AeroRadioButton`'s hover shadow renders square instead
of round. Tracked at
`.planning/todos/pending/2026-07-29-aeroradiobutton-hover-shadow-square-not-round.md`. The
maintainer was explicit that this does not block the sign-off and is specific to that one
component.

If any row FAILs, `gate_status` is set to `FAILED`, the failing rows are listed in
`20-07-SUMMARY.md` as gaps for `/gsd-plan-phase 20 --gaps`, and `.planning/REQUIREMENTS.md`'s
SHW-16 checkbox stays unticked. No row is softened into a pass. **No row above FAILed** — Block D's
rows are recorded as waived, a distinct outcome from both PASS and FAIL, per the maintainer's own
explicit choice.

---

*Phase: 20-verification*
*Opened: 2026-07-29*
*Closed: 2026-07-29*
