---
phase: 20-verification
date: 2026-07-29
launch_command: "./gradlew :showcase:run"
gate_status: PENDING
verified_by: pending
dpi_scales: [125%, 200%]
dpi_theme: AeroBlue
---

# Phase 20 v3.0 Three-Theme Visual Sign-off (SHW-16)

**Phase:** 20-verification
**Date:** 2026-07-29
**Launch command:** `./gradlew :showcase:run`
**Milestone gate:** v3.0 "Glass Refinement" — last gate before ship.

> `gate_status` starts `PENDING` and is set to `PASSED` or `FAILED` only by the developer's own
> stated verdict, transcribed verbatim, after the coherence pass (Task 2) and the DPI passes
> (Task 3) both complete. It is never advanced or inferred by the agent (T-20-07-01).

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
| Full suite (build gate) | `./gradlew :library:test` | PASS — 435 tests, 0 failures | 2026-07-29 |
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

## Sign-off Table

> One verdict cell **per theme, per row**. No cell may be filled by carrying over or deriving
> another theme's verdict (T-20-07-03) — each is answered independently, backed by its own
> capture. A theme with no capture on file is recorded as **FAIL**, never an implicit pass.

### Block A — Cross-Component Coherence (all three themes, all eight components)

| # | Item | AeroBlue | AeroDark | Classic | Capture |
|---|------|----------|----------|---------|---------|
| A1 | All eight read as ONE material family, not eight separate approximations of it | | | | |
| A2 | Fill/gloss/bevel/rim treatment is consistent in KIND across the eight (magnitude may differ per component; language must not) | | | | |
| A3 | Contrast-fixed label on `AeroButton`/`AeroOutlinedButton`/`AeroSegmentedControl` reads legibly, not garish (cross-check `20-04-SUMMARY.md`'s measured ratios) | | | | |
| A4 | `AeroSegmentedControl` mid-selection tween (~150ms): label flips candidate once as fill crosses contrast midpoint — reads acceptably? (open judgment per 20-04, not a defect claim) | | | | |
| A5 | `AeroTheme` now paints its own background (`1d139a7`) — the showcase's own redundant `Surface(colors.background)` on top produces no banding/double-tone at window edges | | | | |
| A6 | `AeroIconButton`, `AeroColorPickerButton`, `AeroRadioButton` — out of restyle scope, but affected if D-12's shared constants moved. Look at them; record what you see | | | | |
| A7 | `AeroIconButton`'s focus ring (WR-02/20-05 fix): appears on keyboard Tab focus only, NOT on a plain mouse click | | | | |
| A8 | No component clipped, no bloom sliced, no seam visible where the design says there should be none | | | | |

### Block B — Per-State Matrix, `AeroButton` and `AeroOutlinedButton` ONLY (D-01, all three themes)

| # | Component | State | AeroBlue | AeroDark | Classic |
|---|-----------|-------|----------|----------|---------|
| B1 | AeroButton | default | | | |
| B2 | AeroButton | hover | | | |
| B3 | AeroButton | press | | | |
| B4 | AeroButton | focus (Tab-acquired only; mouse click must leave no residual ring) | | | |
| B5 | AeroButton | disabled | | | |
| B6 | AeroOutlinedButton | default | | | |
| B7 | AeroOutlinedButton | hover | | | |
| B8 | AeroOutlinedButton | press | | | |
| B9 | AeroOutlinedButton | focus (Tab-acquired only; mouse click must leave no residual ring) | | | |
| B10 | AeroOutlinedButton | disabled | | | |

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
| C2 | AeroDark recessed (selected) segment: label-to-`fillBottom` contrast measures **4.079**, below the 4.5:1 WCAG floor. Proven not fixable by the candidate flip (black's worst-case there is 3.538, strictly worse); retuning `RECESSED_FILL_DARKEN` is forbidden (D-12, 19-08's acceptance of the recess/depth reading). Is the label acceptably legible in practice? (This is a judgment call about acceptability, not a re-measurement — 4.079 vs. 4.5 is below what the eye reliably resolves on its own.) | AeroDark | | AeroBlue/Classic: **N/A** — this exact finding is AeroDark-specific (see `20-04-SUMMARY.md`'s Measured Contrast Table; AeroBlue/Classic recessed segments both clear 4.5:1). |

### Block D — DPI (AeroBlue only, filled by Task 3)

| # | Scale | Theme | Verdict | Capture | Notes |
|---|-------|-------|---------|---------|-------|
| D1 | 125% (exact) | AeroBlue | | | 1dp contour/seam/bevel integrity at fractional scale — zoom on `AeroSwitch`, `AeroSegmentedControl`, `AeroListItem` (thinnest contours). |
| D2 | 200% (exact) | AeroBlue | | | Gloss-band coverage / gradient proportionality / bloom falloff at size. |

---

## Verdict

`gate_status`: **PENDING** — becomes `PASSED` or `FAILED` only once the developer states it, after
Blocks A/B/C (Task 2, 100% DPI) and Block D (Task 3, 125%/200% DPI) all carry verdicts.

`verified_by`: **pending**

If any row FAILs, `gate_status` is set to `FAILED`, the failing rows are listed in
`20-07-SUMMARY.md` as gaps for `/gsd-plan-phase 20 --gaps`, and `.planning/REQUIREMENTS.md`'s
SHW-16 checkbox stays unticked. No row is softened into a pass.

---

*Phase: 20-verification*
*Opened: 2026-07-29*
