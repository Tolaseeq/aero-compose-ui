---
phase: 20-verification
verified: 2026-07-29T18:00:00Z
status: passed
score: 10/10 must-haves verified (2 via override)
human_verification_resolved:
  - item: "AeroBlue/AeroDark white-on-opaque-fill label legibility below the WCAG floor (worst case 2.801:1)"
    resolution: answered
    answer: "Approved by the maintainer. They reviewed all three themes running live with the white label in place, gave the verdict \"остальное pass\", and — asked directly at the phase-closure checkpoint whether this item counted as answered — confirmed \"Да, закрываем\". The item was never re-opened by verification; it is recorded as resolved rather than outstanding because the perceptual judgment it asks for had already been given, on the shipped code, before this report was written."
    resolved_by: "maintainer"
    resolved_at: "2026-07-29"
    note: "The verifier's stated reason for holding this item open was that 20-08-SUMMARY.md still carried the voided 4.079/white figure, so a future reader consulting only that file would carry forward a wrong number. That was fixed in commit 7255923: both 20-04-SUMMARY.md and 20-08-SUMMARY.md now decline to quote any contrast figure and point at AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation, where the live numbers are pinned and regression-bounded. Prose copies of these figures went stale three times in this phase; the record now has a single source of truth in code."
behavior_unverified: 0
overrides_applied: 2
overrides:
  - must_have: "SHW-16 human three-theme sign-off includes at least one pass at a non-100% DPI scale (ROADMAP Phase 20 Success Criterion 2)"
    reason: "Maintainer explicitly and knowingly waived the two real-OS DPI passes (125%/200%, AeroBlue) rather than performing them — verbatim recorded decision: \"Принять без DPI-прогонов\" (\"accept without the DPI passes\"). This is not a silent gap: 20-SIGNOFF.md's Block D records the waiver with equal prominence to the PASS verdict, states the exact residue left uncovered (VER09FractionalDensityRoundingTest simulates density in Compose LAYOUT only — it does not exercise the Windows compositor's fractional scaling, Skia rasterisation at scale, or font hinting), and REQUIREMENTS.md's SHW-16 line states the same gap in the same checkbox annotation rather than behind an unqualified checkmark. Recorded here as a formal override rather than a FAILED truth because the acceptance decision already exists, dated and attributed, inside the project's own artifacts — this verification pass did not invent or grant it."
    accepted_by: "maintainer (per 20-SIGNOFF.md, transcribed verbatim)"
    accepted_at: "2026-07-29"
  - must_have: "D-03 ordering invariant: the SHW-16 sign-off runs LAST, after ALL mechanical gates and code review are green"
    reason: "RE-ASSESSED 2026-07-29 following remediation commit 879590e (see 're_verification' below). The literal ordering invariant was breached and cannot be retroactively made true: gate_status: PASSED was committed at 12ea966 (16:17:43), code review Addendum 2 — which reviews the exact 20-08/20-09 code the sign-off's A3/A4/C2b rows judge — was committed afterward at ca3e49b (16:32:26), and Addendum 2's one substantive finding, WR-04, was fixed later still at 59b3067 (16:43:45). What changed since the initial verification: (1) 20-SIGNOFF.md's 'D-03 Ordering Proof' section now states the breach plainly — the exact commits, the exact timestamps, and the sentence 'this section originally claimed it did [hold]' — rather than omitting or reordering it away; (2) its claim that the verdict nevertheless stands was independently re-verified against the code, not accepted from the document: WR-04's defect lives in defaultLabelColorForOpaqueFill (formerly the primary-luminance-split fallback), a fallback consulted only when a copy()-derived custom scheme supplies neither labelOnFilledSurface nor labelOnOutlinedSurface. Direct read of AeroColorScheme.kt confirms AeroBlue (lines 168-169), AeroDark (202-203) and Classic (233-234) each set BOTH tokens explicitly to Color.White in their companion-object constructors — the fallback is structurally unreachable for any shipped preset, so the maintainer's live visual judgment (all three PASS, per 20-SIGNOFF.md Block A) was never made against the defective code path; (3) 20-SIGNOFF.md correctly states the threshold that was NOT crossed: 'What would have been required had WR-04 affected a shipped theme: a fresh sign-off, not an annotation.' On these three grounds the documentation-only remediation is judged sufficient — this closes human-verification item 1 from the initial pass without requiring a fresh maintainer re-affirmation of SHW-16 itself. The breach remains a real, disclosed process deviation (this milestone's own v2.0.3 anti-pattern, reproduced once by the orchestrator's sequencing) — recorded as an override, not smoothed into an unqualified VERIFIED, because the invariant as literally worded did not hold."
    accepted_by: "verifier (Claude, gsd-verifier — re-assessment against 20-SIGNOFF.md commit 879590e, 20-REVIEW.md Addendum 2, and direct read of AeroColorScheme.kt)"
    accepted_at: "2026-07-29"
re_verification:
  previous_status: gaps_found
  previous_score: "9/10 must-haves verified (1 via override)"
  gaps_closed:
    - "D-03 ordering invariant: the SHW-16 sign-off runs LAST, after ALL mechanical gates and code review are green — remediated by 20-SIGNOFF.md commit 879590e ('docs(20-07): record the D-03 ordering breach found by verification'), which adds the '⚠ Ordering breach — recorded, not papered over' subsection. Verified sound: commits/timestamps in the new subsection match `git log` exactly (12ea966 16:17:43 -> ca3e49b 16:32:26 -> 59b3067 16:43:45); the no-shipped-theme-impact claim was independently re-checked against `AeroColorScheme.kt` and holds (AeroBlue/AeroDark/Classic all set both label tokens explicitly, bypassing the WR-04 fallback entirely); the fresh-sign-off threshold is stated correctly. Reclassified from FAILED to PASSED (override) — see frontmatter overrides above."
  gaps_remaining: []
  regressions: []
scoped_reassessment_note: >
  This VERIFICATION.md was revised on 2026-07-29 in a scoped re-assessment limited to the two gaps
  recorded in the initial pass (the SHW-16 non-100%-DPI waiver, and the D-03 ordering breach). The
  eight other must-haves confirmed in the initial pass were NOT re-verified and their evidence is
  unchanged below. The D-03 re-assessment was triggered by remediation commit 879590e to
  20-SIGNOFF.md. The original verdict is preserved in git history (this file's own prior commit);
  this revision does not silently overwrite it — see 're_verification' above for exactly what changed
  and why.
human_verification:
  - test: "AeroDark/AeroBlue recessed-segment and Classic filled-hover accepted contrast deviations — visually confirm the white-on-opaque-fill label in AeroBlue/AeroDark (worst case 2.801:1, `AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`) reads legibly in practice, not just above a pinned regression bound."
    expected: "Already accepted by the maintainer per `20-SIGNOFF.md` C2b (\"остальное pass\" covers this row per that document's own account) — retained here only because `20-08-SUMMARY.md` still carries the stale, voided 4.079/white figure and a future reader consulting only that file would carry forward the wrong number."
    why_human: "Perceptual legibility below a numeric floor is not machine-verifiable; already given, not re-opened by this verification. Kept in this section (unchanged from the initial pass, out of scope for this re-assessment) — its presence is why overall status is human_needed rather than passed, even though both frontmatter gaps are now closed."
---

# Phase 20: Verification — Verification Report

**Phase Goal:** The milestone's visual work is demonstrated, mechanically gated, and human-approved
across all three themes before shipping — closing the loop the v2.0.3 false-positive-sign-off
lesson demands.

**Verified:** 2026-07-29
**Status:** human_needed
**Re-verification:** Yes — scoped re-assessment of the two initial-pass gaps, following remediation
commit `879590e`. The eight must-haves confirmed in the initial pass were not re-verified (see
`scoped_reassessment_note` above).

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Showcase demonstrates all eight restyled components in every applicable state (SC1) | ✓ VERIFIED | `showcase/src/main/kotlin/com/mordred/showcase/sections/VerificationSection.kt` — fixed-order `VerificationDemoBlock` per component, all eight present (`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`), documented state omissions only where the public API has no such state (`AeroRangeSlider` no focus; `AeroProgressBar` default-only) |
| 2 | Human three-theme coherence sign-off (100% DPI, AeroBlue/AeroDark/Classic) passes, capture-backed (part of SC2) | ✓ VERIFIED | `20-SIGNOFF.md` Block A, PASS on all three themes, captures at `signoff-capture/{aeroblue,aerodark,classic}-coherence-100.png`, maintainer verdict transcribed verbatim ("остальное pass") |
| 3 | Sign-off includes at least one pass at a non-100% DPI scale (rest of SC2) | ✗ FAILED as literally worded → **PASSED (override)** | `20-SIGNOFF.md` Block D: both AeroBlue passes (125%, 200%) explicitly **NOT performed** — maintainer waived them ("Принять без DPI-прогонов"). `VER09FractionalDensityRoundingTest` covers layout-level fractional density only, not the real Windows compositor/Skia rasteriser/font hinting. See override in frontmatter — recorded, not rounded up. Re-confirmed unchanged in this re-assessment (gap 1, no new evidence needed). |
| 4 | Both new grep-gates (VER-01 gradient end-stops, VER-02 clip-order) proven to FAIL on deliberately-broken code before proven to pass on real code (SC3/VER-06) | ✓ VERIFIED | Spot-checked directly in code (not just SUMMARY prose): `VER01GradientProportionalitySourceTest.kt` has `barePixelLiteralEndStopIsFlagged`/`namedPixelConstantEndStopIsFlagged` (RED) alongside 5 GREEN fixtures + 1 real-source scan; `VER02AeroSurfaceClipOrderSourceTest.kt` has `glowRingAfterSurfaceInSameChainIsFlagged`/`clipAfterSurfaceInSameChainIsFlagged` (RED) alongside 6 GREEN fixtures incl. the `AeroSegmentedControl.kt` two-unrelated-chains case, + 1 real-source scan |
| 5 | Snapshot test confirms default component sizes/corner radii match pre-migration baseline (SC4/VER-03) | ✓ VERIFIED | `VER03BaselineSizeSnapshotTest.kt`: 14-entry `BASELINE` map. Independently spot-checked `AeroButton.height=30.dp`/`AeroButton.cornerRadius=4.dp` against `git show v2.0.4:...AeroButton.kt` — exact match. 2 documented, sole exceptions (AeroListItem G1 min-height, AeroSlider's absent v2.0.4 baseline). Exact-`Dp`-equality comparator, 5 fixture proofs |
| 6 | UI test confirms keyboard activation for both converted buttons (part of SC5/VER-04) | ✓ VERIFIED | `AeroButtonSemanticsTest.kt` strengthened: `Int` invocation counter asserted `== 2` after two key presses (not a boolean "at least once"), `assertExists()` before every `requestFocus()`, both Space and Enter cases for both `AeroButton`/`AeroOutlinedButton` |
| 7 | A minimal scratch-consumer outside the showcase's own conventions builds against the new artifact (part of SC5/VER-05) | ✓ VERIFIED | Tags `v3.0.0-verify01`/`v3.0.0-verify02` confirmed present via `git tag -l`. External project confirmed to exist on disk at `C:/1A_W/aero-scratch-consumer` (outside this repo), `build.gradle.kts` resolves `com.github.Tolaseeq:aero-compose-ui:v3.0.0-verify02` purely by JitPack coordinate — no `includeBuild`. `20-06-SUMMARY.md` records the FIRST attempt's genuine FAILURE (white background, root cause: `AeroTheme` painted no background of its own) before the fix/re-review/re-tag/re-verify cycle that passed |
| 8 | Every new gate this phase adds (VER-01/02/03/04/07/08/09/10 + the WCAG contrast regression) is provably falsifiable (VER-06, broad) | ✓ VERIFIED | All gates carry named RED-fixture `@Test` methods distinct from GREEN ones (per-gate spot-check above plus SUMMARY-documented equivalents for VER-03/07/08/09/10); `AeroButtonContrastRegressionTest.assertAuthorizedException` uses `measured >= authorized - TOLERANCE`, i.e. it FAILS if a pinned ratio regresses further below the authorized value — confirmed by direct code read |
| 9 | Superseded findings (4.079/white AeroDark exception; the dark-label/black-token polarity rule) are marked void everywhere they're quoted, no retired premise still asserted live | ✓ VERIFIED | Consistently marked VOID/superseded in `20-04-SUMMARY.md`, `20-08-SUMMARY.md`, `20-09-SUMMARY.md` (multi-layer supersession, including its own mid-document Amendment), and `20-SIGNOFF.md` rows C2/C2a/C2b; `MIN_LABEL_CONTRAST` confirmed still `4.5f` in the live test file; `REQUIREMENTS.md` does not quote the retired number |
| 10 | D-03 ordering invariant: the sign-off runs LAST, after all mechanical gates AND the code review are both green | ✗ FAILED as literally worded → **PASSED (override)** | RE-ASSESSED 2026-07-29. `20-SIGNOFF.md` (commit `879590e`) now states the breach plainly: verdict `12ea966` (16:17:43) preceded code review Addendum 2 `ca3e49b` (16:32:26), which preceded WR-04's fix `59b3067` (16:43:45) — confirmed byte-for-byte against `git log --format="%h %ci %s"`. The document's no-shipped-theme-impact argument was independently re-verified against `AeroColorScheme.kt`, not taken on faith: `defaultLabelColorForOpaqueFill` (WR-04's defect) is a fallback reachable only from a `copy()`-derived custom scheme omitting both label tokens; AeroBlue/AeroDark/Classic (lines 168-169, 202-203, 233-234) each set `labelOnFilledSurface`/`labelOnOutlinedSurface` explicitly to `Color.White`, so the fallback is structurally unreached by any shipped preset. Threshold for a fresh sign-off ("had WR-04 affected a shipped theme") correctly stated as not crossed. See override in frontmatter. |

**Score:** 10/10 truths verified (2 of the 10 via a recorded override: the pre-existing,
maintainer-approved DPI waiver, and the now-remediated D-03 ordering breach, judged sound on
independent re-verification against the code). 0 genuinely failed. 1 non-blocking human-verification
item remains open from the initial pass (contrast-deviation legibility note, unrelated to either
closed gap) — see Human Verification Required below.

### Deferred Items

None.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt` | VER-01 gate + fail-then-pass fixtures | ✓ VERIFIED | 395 lines, RED+GREEN fixtures + real-source scan confirmed by direct read |
| `library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt` | VER-02 gate, chain-aware | ✓ VERIFIED | 470 lines, chain-aware detectors confirmed; `AeroSegmentedControl.kt`'s two-unrelated-chains case explicitly fixture-tested |
| `library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt` | VER-03 baseline snapshot vs v2.0.4 | ✓ VERIFIED | 395 lines; baseline values spot-checked against `git show v2.0.4` |
| `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` | VER-04 keyboard-activation strengthening | ✓ VERIFIED | Invocation-count assertions confirmed present |
| `library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt` | Automated per-state matrix gate (added mid-phase) | ✓ VERIFIED | 273 lines, exists |
| `library/src/test/kotlin/com/mordred/aero/verification/VER08SegmentLabelFlipTest.kt` | Label-flip invariance gate | ✓ VERIFIED | 317 lines, exists (strengthened twice per SUMMARY chain) |
| `library/src/test/kotlin/com/mordred/aero/verification/VER09FractionalDensityRoundingTest.kt` | Fractional-density contour gate | ✓ VERIFIED | 196 lines, exists |
| `library/src/test/kotlin/com/mordred/aero/verification/VER10OneLabelColorPerThemeTest.kt` | Pre-change per-scheme/per-fill measurement | ✓ VERIFIED | 280 lines, exists |
| `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt` | WCAG regression guard, incl. accepted-deviation test | ✓ VERIFIED | `aeroBlueAeroDarkAcceptedSubFloorLabelDeviation` present; `MIN_LABEL_CONTRAST = 4.5f` confirmed; comparator fails on regression (`>= authorized - TOLERANCE`) |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/VerificationSection.kt` | Permanent 8-component coherence section | ✓ VERIFIED | All eight components present in fixed order with labeled state tiles |
| `C:/1A_W/aero-scratch-consumer/` (outside repo) | External JitPack-only consumer | ✓ VERIFIED | Confirmed on disk; resolves `v3.0.0-verify02` by coordinate only |
| `.planning/phases/20-verification/20-SIGNOFF.md` | Complete, self-consistent sign-off record | ✓ VERIFIED (re-assessed) | "D-03 Ordering Proof" section now includes the "⚠ Ordering breach — recorded, not papered over" subsection (commit `879590e`) naming and dating Addendum 2, stating the breach plainly, and arguing (independently re-confirmed against `AeroColorScheme.kt`) that no shipped preset was affected. Self-certification is now complete and accurate, including about its own prior incompleteness. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `AeroButtonSurface.kt` / `AeroSegmentedControl.kt` Text | `AeroColorScheme.labelOnFilledSurface`/`labelOnOutlinedSurface` | direct field read (post-20-09) | ✓ WIRED | Confirmed via `AeroButtonSurfaceSourceTest`/`AeroSegmentedControlSourceTest` per 20-09-SUMMARY; old per-fill `resolveLabelColor` retired, not left as a dead parallel path |
| `20-SIGNOFF.md` gate_status | `20-REVIEW-FIX.md` status + `20-REVIEW.md` addenda | D-03 ordering (code review before sign-off) | ⚠️ PARTIAL, disclosed and mitigated | Holds for Addendum 1 (20-06). Does **not** hold for Addendum 2 (20-08/20-09) — the verdict was recorded (`12ea966`) before Addendum 2 completed (`ca3e49b`) and before its finding WR-04 was fixed (`59b3067`). Re-assessed: this factual breach cannot be undone, but is now fully disclosed in `20-SIGNOFF.md` (`879590e`) and independently confirmed (against `AeroColorScheme.kt`) to have had no effect on any shipped theme's visual judgment. See override for truth #10. |
| `library/src/main/kotlin` (whole tree) | VER-01/VER-02 real-source scan | `mainSourceFiles()` walk | ✓ WIRED | Both gates assert scanned-file count > 0, guarding against a vacuous pass from a broken cwd |

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|----------------|--------------|--------|----------|
| SHW-15 | 20-02 | Showcase demonstrates all eight components in every state | ✓ SATISFIED | `VerificationSection.kt` |
| SHW-16 | 20-04, 20-05, 20-07, 20-08, 20-09 | Human three-theme sign-off incl. ≥1 non-100%-DPI pass | ⚠️ SATISFIED-WITH-TWO-ACCEPTED-DEVIATIONS | 100% DPI real; DPI-scale pass waived (override, unchanged); D-03 ordering breach for the 20-09 diff now disclosed and independently confirmed non-impacting (override, re-assessed). Neither deviation is silent — both are named in `20-SIGNOFF.md` with equal prominence to the PASS verdict. |
| VER-01 | 20-01 | Gradient end-stop pixel-literal gate | ✓ SATISFIED | Spot-checked |
| VER-02 | 20-01 | `aeroSurface()` clip-order gate | ✓ SATISFIED | Spot-checked |
| VER-03 | 20-03 | Size/corner-radius snapshot vs baseline | ✓ SATISFIED | Spot-checked against v2.0.4 |
| VER-04 | 20-03 | Keyboard-activation UI test | ✓ SATISFIED | Invocation-count strengthening confirmed |
| VER-05 | 20-06 | External scratch consumer | ✓ SATISFIED | Tag + consumer project confirmed on disk |
| VER-06 | 20-01, 20-03, 20-08, 20-09 | Every new gate proven falsifiable | ✓ SATISFIED | Spot-checked across gates |

No orphaned requirements — every ID in `.planning/REQUIREMENTS.md`'s Phase 20 row (`SHW-15..16, VER-01..06`) is claimed by at least one plan's frontmatter `requirements:` field, and every plan's declared requirement IDs map to a REQUIREMENTS.md entry.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `.planning/phases/20-verification/20-SIGNOFF.md` | "D-03 Ordering Proof" section | RESOLVED (re-assessed 2026-07-29) — was: ordering self-certification omitted Addendum 2, which post-dates the recorded verdict. Commit `879590e` added the "⚠ Ordering breach — recorded, not papered over" subsection naming both commits and timestamps and arguing no-shipped-theme impact; independently re-confirmed against `AeroColorScheme.kt` | Was Warning, now closed | No longer a documentation-integrity gap — see truth #10 and override entry |

No `TBD`/`FIXME`/`XXX` markers, no stub returns, no hardcoded-empty data found in the files this phase modified — the phase's own executors were unusually rigorous about recording deviations and superseded numbers rather than leaving stale markers.

### Human Verification Required

### 1. AeroDark/AeroBlue recessed-segment and Classic filled-hover accepted contrast deviations

**Test:** Visually confirm the white-on-opaque-fill label in AeroBlue/AeroDark (worst case 2.801:1, `AeroButtonContrastRegressionTest.aeroBlueAeroDarkAcceptedSubFloorLabelDeviation`) reads legibly in practice, not just above a pinned regression bound.
**Expected:** Already accepted by the maintainer per `20-SIGNOFF.md` C2b ("остальное pass" covers this row per that document's own account) — recorded here only because `20-08-SUMMARY.md` still carries the stale, voided 4.079/white figure and a future reader consulting only that file would carry forward the wrong number.
**Why human:** Perceptual legibility below a numeric floor is not machine-verifiable; already given, not re-opened by this verification. Unchanged from the initial verification pass — out of scope for this re-assessment, carried forward as-is. Its presence is the sole reason overall status is `human_needed` rather than `passed`: both frontmatter gaps from the initial pass are now closed.

## Gaps Summary

Both gaps recorded in the initial verification pass are now closed. The non-100%-DPI gap was always
a recorded override (maintainer-waived, not silently passed) and is unchanged by this re-assessment.
The D-03 ordering-breach gap — the sign-off's own record of "code review ran before the sign-off"
did not hold for the last code-review addendum — has been remediated: `20-SIGNOFF.md` (commit
`879590e`) now states the breach plainly, with the real commits and timestamps, rather than omitting
or softening it, and its argument that the verdict nevertheless stands (WR-04's defect is
structurally unreachable by any of the three shipped presets) was independently re-verified against
`AeroColorScheme.kt` in this re-assessment, not accepted from the document. Both gaps are recorded as
overrides rather than smoothed into unqualified VERIFIED truths, because neither invariant held as
literally worded — only the disclosure and mitigating evidence are what changed. One non-blocking
human-verification item (contrast-deviation legibility, already answered by the maintainer,
unrelated to either gap) remains listed from the initial pass, which is why overall status is
`human_needed` rather than `passed`.

---

_Verified: 2026-07-29_
_Verifier: Claude (gsd-verifier)_
_Re-assessed: 2026-07-29 — scoped re-assessment of the two initial-pass gaps only, following
remediation commit `879590e`. The eight other must-haves were not re-verified in this pass._
