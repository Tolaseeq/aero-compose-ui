---
phase: 20-verification
reviewed: 2026-07-29T00:00:00Z
depth: standard
files_reviewed: 23
files_reviewed_list:
  - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt
  - library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
  - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt
  - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroOutlinedButton.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt
  - showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt
  - library/src/test/kotlin/com/mordred/aero/verification/VER01GradientProportionalitySourceTest.kt
  - library/src/test/kotlin/com/mordred/aero/verification/VER02AeroSurfaceClipOrderSourceTest.kt
  - showcase/src/main/kotlin/com/mordred/showcase/sections/VerificationSection.kt
  - library/src/test/kotlin/com/mordred/aero/verification/VER03BaselineSizeSnapshotTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt
  - library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt (20-04 diff only)
  - library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt (20-04 diff only)
  - library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt (20-04 diff only)
  - library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt (20-04 diff only)
findings:
  critical: 0
  warning: 2
  info: 2
  total: 4
status: issues_found
---

# Phase 20: Code Review Report (D-03 — code review before the SHW-16 sign-off)

**Reviewed:** 2026-07-29
**Depth:** standard
**Status:** issues_found — all eleven never-reviewed files plus Phase 20's own diff covered;
4 findings (0 critical, 2 warning, 2 info), disposition of each recorded in `20-REVIEW-FIX.md`.

## Scope Derivation

This milestone (`v3.0`) changed 28 files under `library/src/main`/`showcase/src/main` against tag
`v2.0.4` (`git diff --name-status v2.0.4..HEAD -- library/src/main showcase/src/main`). The plan's
own objective text states "27" — this document uses the actual `git diff` count (28) rather than
silently reconciling to the plan's number; the one-off discrepancy is noted here per this plan's
own instruction to flag it rather than substitute a derived list quietly. It does not change which
files are in scope below — every file the diff reports is accounted for.

| # | File | Status | Disposition |
|---|------|--------|-------------|
| 1 | `library/.../components/buttons/AeroButton.kt` | M | in-scope (Task 2) |
| 2 | `library/.../components/buttons/AeroButtonSurface.kt` | A | already-covered (`19-REVIEW.md`); re-reviewed here **only** for Plan 20-04's diff (Task 2) |
| 3 | `library/.../components/buttons/AeroIconButton.kt` | M | in-scope (Task 2) |
| 4 | `library/.../components/buttons/AeroOutlinedButton.kt` | M | in-scope (Task 2) |
| 5 | `library/.../components/buttons/InteractionStates.kt` | D (deleted) | nothing to review — content moved to #6, covered by `19-REVIEW.md` |
| 6 | `library/.../components/common/InteractionStates.kt` | A | already-covered (`19-REVIEW.md`) |
| 7 | `library/.../components/list/AeroListItem.kt` | M | already-covered (`19-REVIEW.md`) |
| 8 | `library/.../components/range/AeroProgressBar.kt` | M | already-covered (`18-REVIEW.md`) |
| 9 | `library/.../components/range/AeroRangeSlider.kt` | M | already-covered (`18-REVIEW.md`) |
| 10 | `library/.../components/range/AeroSlider.kt` | M | already-covered (`18-REVIEW.md`) |
| 11 | `library/.../components/selection/AeroSegmentedControl.kt` | M | already-covered (`19-REVIEW.md`); re-reviewed here **only** for Plan 20-04's diff (Task 2) |
| 12 | `library/.../components/selection/AeroSwitch.kt` | M | already-covered (`19-REVIEW.md`) |
| 13 | `library/.../theme/AeroColorScheme.kt` | M | **in-scope (Task 1)** |
| 14 | `library/.../theme/AeroOrnamentTokens.kt` | A | **in-scope (Task 1)** |
| 15 | `library/.../theme/AeroSurfacePrimitives.kt` | A | already-covered (`18-REVIEW.md`) |
| 16 | `library/.../theme/AeroSurfaceStyle.kt` | A | already-covered (`18-REVIEW.md`) |
| 17 | `library/.../theme/AeroTheme.kt` | M | **in-scope (Task 1)** |
| 18 | `library/.../theme/ColorMath.kt` | A | **in-scope (Task 1)** |
| 19 | `library/.../theme/GlassModifiers.kt` | M | **in-scope (Task 1)** |
| 20 | `showcase/.../ShowcaseApp.kt` | M | in-scope (Task 2) |
| 21 | `showcase/.../scratch/ScratchAeroShadowProof.kt` | A | **in-scope (Task 1)** |
| 22 | `showcase/.../scratch/ScratchSliderSlotSpike.kt` | A | **in-scope (Task 1)** |
| 23 | `showcase/.../sections/ButtonsSection.kt` | M | in-scope (Task 2) |
| 24 | `showcase/.../sections/ListSection.kt` | M | already-covered (`19-REVIEW.md`) |
| 25 | `showcase/.../sections/PrimitivesSection.kt` | A | in-scope (Task 2) |
| 26 | `showcase/.../sections/RangeSection.kt` | M | already-covered (`18-REVIEW.md`) |
| 27 | `showcase/.../sections/SelectionSection.kt` | M | already-covered (`19-REVIEW.md`) |
| 28 | `showcase/.../sections/VerificationSection.kt` | A | in-scope — Phase 20's own diff, reviewed in Task 2 (created by sibling plan 20-02, never separately reviewed) |

**Resulting in-scope count:** 7 files reviewed in this task (foundation tier + the two scratch
files) + 6 files reviewed in Task 2 (buttons/showcase tier) + Phase 20's own diff (the four sibling
plans' `files_modified`, including `VerificationSection.kt` above) = 13 explicitly-named files plus
the sibling-plan diff, all covered across the two tasks. 13 already-covered files are excluded
(named above), and 2 files (`AeroButtonSurface.kt`, `AeroSegmentedControl.kt`) are counted once as
already-covered but reviewed a second time narrowly, in Task 2, for the specific lines Plan 20-04
touched — this is not a re-review of the whole file, only of the new diff.

## Findings — Foundation Tier + Scratch Files (Task 1)

Reviewed against, in order: (1) `PROJECT.md`/`STATE.md` locked positions — `Color.Transparent`
banned as a gradient/fade target (PRIM-14), `drawWithCache` as the perf baseline (PRIM-13), RGB
lighten/darken not alpha manipulation (Classic's tokens are opaque); (2) correctness/resource-
lifecycle bugs; (3) API-surface hygiene; (4) dead code / stale KDoc.

### `theme/GlassModifiers.kt`

Diffed directly against `v2.0.4` (`git diff v2.0.4..HEAD -- .../GlassModifiers.kt`). Every change
matches its own KDoc claim exactly: `glassEffect` gained a real `dropShadow` (PRIM-11, the
previously-dead `elevation` parameter is now wired); `glassPanel`/`glassSurface` moved to
`drawWithCache` (PRIM-13 discipline, brush built once, not per frame); `glassSurface`'s gloss
`endY` is now `size.height * GLASS_SURFACE_GLOSS_FRACTION` (proportional, PRIM-09 fixed) and
`.clip(shape)` now runs first with the border additionally half-width-inset (PRIM-10/12 fixed, no
more clipped-away outer half). No new defect found in the diff itself.

**IN-01: `glassEffect`/`glassPanel` apply no `.clip(shape)` to their own returned modifier chain —
only `glassSurface` does.** `Modifier.background(brush, shape)`/`.border(width, color, shape)`
clip their *own paint* to `shape`, but do not clip a caller's *descendant content* drawn on top —
only an explicit `.clip(shape)` (or a self-clipping layer) does that. `glassEffect`/`glassPanel`
are consumed by ~40 components across the library (`AeroCard`, `AeroPanel`, `AeroToastHost`,
`AeroAccordion`, `AeroSidebar`, `AeroMenuBar`, `AeroStatusBar`, `AeroToolbar`,
`AeroTableHeader`, `AeroColorPicker`, `PickerPopupContainer`, `AeroPanelGroup`,
`AeroNotificationBanner`, and more — confirmed via a full-library grep of both call sites), so a
child that visually reaches the rounded corner (e.g. an edge-to-edge image) would draw square
over a rounded background in any of them. **Pre-existing since before `v2.0.4`** — this milestone's
diff (above) did not introduce or touch this behavior; `glassSurface` alone was the file's
PRIM-10/12 target. **Not fixed here**: `STATE.md`'s own recorded blast-radius warning is explicit —
"fixing `GlassModifiers.kt` re-renders the ~40 out-of-scope components sharing those modifiers" —
so a change to this file's clip behavior is weighed against that blast radius and deferred rather
than applied blind. **Severity: info. Disposition: deferred to a future foundation-tier plan**,
should any of the 40 consumers actually exhibit visible square-corner bleed at a three-theme
sign-off.

### `theme/ColorMath.kt`

`lighten`/`darken` mix RGB channels toward White/Black proportionally to `amount`, preserving
`alpha` — matches the locked "RGB lighten/darken, never alpha-only, because Classic's tokens are
opaque" position exactly (PRIM-01). No defect found.

### `theme/AeroOrnamentTokens.kt`

`derive(base)` produces all eight ornament tokens via `ColorMath` calls against the active scheme,
each magnitude annotated in-line with its Phase 16/17 sign-off history (D-01's ~30-35% gloss
target, the 17-05 rim/glow/fillSplit gap-fixes). The class KDoc itself states these magnitudes are
"re-reviewed at the Phase 16 three-theme sign-off — not a locked spec," and STATE.md already tracks
gap **G4** (AeroOrnamentTokens brightness on AeroBlue/AeroDark, deferred to a separate Phase 16
foundation session, VIS-F01-adjacent) as an open, human-reviewed, already-tracked item. **Not
re-filed as a new finding here** — refiling an already-tracked deferred item under a new ID would
duplicate, not add, signal; see G4 in STATE.md's Decisions log for its own disposition.

### `theme/AeroColorScheme.kt`

Three schemes (`AeroBlue`/`AeroDark`/`Classic`), all fields consistently typed `Color`, trailing
defaulted `ornamentOverride: AeroOrnamentTokens? = null` (PRIM-03 escape hatch) is source-
compatible by construction (every pre-existing positional/named constructor call keeps compiling).
No defect found.

### `theme/AeroTheme.kt`

`LocalAeroColors`/`LocalAeroTypography` composition locals, `AeroTheme{}` root wrapper bridging
Aero tokens into Material3's `darkColorScheme`/`Typography`, and the `AeroTheme` companion object's
`colors`/`typography`/`ornaments` static accessors. `ornaments` correctly resolves
`ornamentOverride` before falling back to `AeroOrnamentTokens.derive(it)` (PRIM-03 honored). No
defect found in this file itself — see `PrimitivesSection.kt` in Task 2 for a call site that
bypasses this accessor.

### `showcase/scratch/ScratchAeroShadowProof.kt` and `ScratchSliderSlotSpike.kt` — ship-or-remove verdict

Both are `internal` `@Composable` functions. Confirmed via a full-showcase grep
(`ScratchAeroShadowProof(` / `ScratchSliderSlotSpike(`) that **neither has any call site anywhere
in `showcase/src/main`** — they are compiled into the showcase module but never invoked by
`ShowcaseApp` or any section. Both carry an explicit KDoc "SCOPE GUARD: ... Do NOT wire this into
any real showcase screen or component," confirming this is intentional, not an oversight of a
wiring step.

**WR-01: Two permanent, unreachable compile-proof artifacts (`ScratchAeroShadowProof`,
`ScratchSliderSlotSpike`) ship inside `showcase/src/main`, not a `test`/`docs` source set —
meaning every showcase distribution permanently compiles and packages two dead composables.**
Both are genuinely valuable as historical evidence (`ScratchAeroShadowProof` is the TOOL-07
`dropShadow`/`innerShadow` package-correction record cited by Phase 16+; `ScratchSliderSlotSpike`
is the PRIM-18 M3-slot-compatibility PASS verdict cited by Phase 18) and their own KDoc explicitly
forbids wiring them live — so this is a deliberate choice, not a bug, but nobody has weighed
whether `showcase/src/main` (packaged into the distributed showcase jar) is the right permanent
home for evidence artifacts versus `library/src/test` or a `docs/` location that compiles but does
not ship. **Severity: warning. Disposition: recorded, not fixed here** — relocating a file across
Gradle source sets is a build-topology change adjacent to Rule 4 (architectural), and both files
are explicitly historical evidence rather than a functional defect; routed to the maintainer/backlog
for a decision at a future foundation-tier plan, not fixed blind in this one.

## Findings — Buttons/Showcase Tier (Task 2)

### `components/buttons/AeroButton.kt` / `AeroOutlinedButton.kt`

Both are thin public wrappers delegating all painting to `AeroButtonSurface` (already covered by
`19-REVIEW.md`) with only their variant's locked defaults (height, content padding, `outlined`
flag). No logic of their own beyond assembling the call. No defect found.

### `components/buttons/AeroIconButton.kt`

**WR-02: `AeroIconButton`'s focus ring gates on the raw `focused` flag
(`rememberFocusState`/`collectIsFocusedAsState`), not `focusVisible` — the one button-family
component never migrated to the pointer-acquired-suppression fix WR-01/CR-02/G2 applied
everywhere else in the library.**

**File:** `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroIconButton.kt:63,73-77`

**Issue:** `AeroButtonSurface` (WR-01, closed at 19-09), `AeroSwitch`/`AeroSegmentedControl`/
`AeroListItem` (G2, closed at 19-05/06/07) all gate their focus glow/ring on
`state.focusVisible`/`rememberFocusVisible(...)` — a reducer that suppresses the ring when focus
was pointer-acquired (a mouse click), showing it only for genuine keyboard-Tab focus.
`AeroIconButton` still calls `rememberFocusState(interactionSource)` (`= collectIsFocusedAsState()`,
the pre-fix raw signal) and gates its `2.dp` `borderSelected` border on `focused && enabled`
directly — so clicking an `AeroIconButton` with a mouse draws the focus border immediately, the
exact defect class every other Aero component in the library was fixed to not exhibit. This
component is in the milestone's own diff (`M`, changed this milestone) and was never previously
reviewed, so it is squarely in scope here rather than a pre-existing item to defer.

**Fix:** Replace `rememberFocusState` with the shared `rememberFocusVisible` reducer (already
`internal` in `components/common/InteractionStates.kt`, no new symbol needed) and gate the border
on the resolved `focusVisible` boolean instead of the raw `focused` flag. **Severity: warning.
Disposition: fixed in `20-REVIEW-FIX.md`** (mechanical, local, matches the library-wide precedent
exactly — no architectural decision required).

### `showcase/ShowcaseApp.kt`

`VerificationSection()` is registered as the first content section, immediately after
`ThemeSwitcher` and before "Foundation" — matches the 20-02-SUMMARY claim exactly. No defect
found.

### `showcase/sections/ButtonsSection.kt`

Table-style rows for `AeroButton`/`AeroOutlinedButton`/`AeroIconButton`/`AeroToolbar`, each composing
the real component with no forked demo styling and no style resolver called from showcase code. No
defect found.

### `showcase/sections/PrimitivesSection.kt`

**IN-02: Calls `AeroOrnamentTokens.derive(colors)` directly instead of `AeroTheme.ornaments` —
bypasses the `ornamentOverride` escape hatch (PRIM-03) for this one demo section.**

**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/PrimitivesSection.kt:36`

**Issue:** `AeroTheme.ornaments` (in `theme/AeroTheme.kt`, reviewed above) is the documented
accessor: it resolves `colorScheme.ornamentOverride` first and only falls back to
`AeroOrnamentTokens.derive(it)` when no override is set. `PrimitivesSection.kt` instead calls
`AeroOrnamentTokens.derive(colors)` directly, so if a future custom `AeroColorScheme` supplies an
`ornamentOverride`, this one gallery section's `aeroGlowRing` demo tile would silently show the
algorithmically-derived tokens instead of the override — a showcase-only display bug (the real
components elsewhere all read via `AeroTheme.ornaments` or an equivalent resolved path), not a
library defect. **Severity: info. Disposition: fixed in `20-REVIEW-FIX.md`** (one-line swap, local,
obviously safe per this plan's info-finding discretion).

## Findings — Phase 20's Own Diff

Reviewed the four sibling plans' output against the four checks this plan's Task 2 names.

### 20-01 (VER-01/VER-02 gates): gate falsifiability — VERIFIED

Both `VER01GradientProportionalitySourceTest.kt` and `VER02AeroSurfaceClipOrderSourceTest.kt`
follow the D-08 three-layer shape: a pure detector over source text, in-file `const val` fixture
pairs, and a real-source scan. Spot-checked directly (not just trusted from the SUMMARY): each file
carries explicit `@Test` methods asserting a **non-empty** violation list against a `VIOLATING_*`
fixture (the RED half) alongside methods asserting an **empty** list against `CLEAN_*` fixtures
(the GREEN half) — e.g. `VER01...Test`'s `bareNumericPixelLiteralEndYIsFlagged`/
`namedPixelConstantEndYIsFlagged` (RED) vs. `sizeRelativeEndYIsClean`/`noExplicitStopsIsClean` etc.
(GREEN); `VER02...Test`'s `glowRingAfterSurfaceIsFlagged`/`clipAfterSurfaceIsFlagged` (RED) vs. the
clean/two-unrelated-chains fixtures (GREEN). Each detector is genuinely capable of failing — a
detector whose rule could not match a realistic violation would have no RED-half assertion at all;
both files have one for each gate half. Neither class writes to the source tree (both operate on
in-memory `String`/comment-stripped source, confirmed by reading the imports and function
signatures — no `File(...).writeText` or similar anywhere in either file). **No finding.**

### 20-02 (Verification showcase section): composition/style-resolver/nested-scroll — VERIFIED

Read `VerificationSection.kt` in full: it imports and calls the eight real shipped components
(`AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`,
`AeroRangeSlider`, `AeroProgressBar`, `AeroListItem`) with no forked demo styling and no call to any
`resolve*Style`/`resolveLabelColor`-class internal resolver from showcase code. Its own KDoc states
explicitly "This section reuses `ShowcaseApp`'s existing outer scrolling `Column`... introduces no
second, nested scroll region," and the file's `Column`/`Row` composition confirms this (no
`verticalScroll`/`LazyColumn`/`AeroScrollArea` anywhere in the file). **No finding.**

### 20-03 (VER-03 baseline provenance) — VERIFIED (spot-check)

Read the `BASELINE` map (`VER03BaselineSizeSnapshotTest.kt:292-306`, 14 entries) and independently
cross-checked one entry against the actual `v2.0.4` tag rather than trusting the SUMMARY's own
claim: `git show v2.0.4:.../AeroButton.kt` confirms `height: Dp = 30.dp`, matching
`"AeroButton.height" to 30.dp` exactly. No float-tolerance construct appears in the comparison
(`baselineDeviations` compares by exact `Dp` equality, confirmed via the SUMMARY's own quoted
implementation and this file's fixture set, which includes a deliberate ±1.dp-shift RED case that
would be pointless if the real comparison tolerated any drift). **No finding.**

### 20-04 (contrast fix scope) — VERIFIED

Diffed `AeroButtonSurface.kt`/`AeroSegmentedControl.kt` directly between the commits Plan 20-04
introduced (`b25ca73`..`dcc7087`) rather than trusting the SUMMARY's claim alone:
- Both `Text` call sites read `color = resolveLabelColor(style.fillTop, style.fillBottom,
  colors.background)` — the same shared, cross-package-imported function. Confirmed.
- No `.dp`/`RoundedCornerShape`/`PaddingValues`/geometry token changed in either file's diff — the
  entire diff is additive (`resolveLabelColor`, `LABEL_CANDIDATE_DARK/LIGHT`, `labelContrastRatio`)
  plus the two `Text` color-parameter wirings and KDoc prose. Confirmed.
- `RECESSED_FILL_DARKEN` does not appear anywhere in the diff (only KDoc prose referencing it by
  name changed) — confirmed untouched.
- `AeroButtonContrastRegressionTest.kt`'s `contrastRatio` (line 321) is a private, test-local
  function; the file's imports (`grep '^import'`) show no import of `resolveLabelColor` or
  `labelContrastRatio` from production — confirmed independently-derived (D-13), matching the
  SUMMARY's claim.

**No finding.** The one open item from this plan (AeroDark recessed-segment `fillBottom` at
4.0787, below the 4.5 floor) is the already-authorized, tracked exception per this plan's own
`<prior_phase_context>` — **not relitigated here**, consistent with the instruction not to
re-file it as a new review finding.

---

_Reviewed: 2026-07-29_
_Reviewer: Claude (gsd-executor, inline review per this plan's `planner_assumptions`)_
_Depth: standard_
