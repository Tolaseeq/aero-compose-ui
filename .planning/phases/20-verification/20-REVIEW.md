---
phase: 20-verification
reviewed: 2026-07-29T00:00:00Z
depth: standard
files_reviewed: 24
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
  - library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt (addendum — 20-06 diff only)
findings:
  critical: 0
  warning: 3
  info: 4
  total: 7
status: issues_found
---

# Phase 20: Code Review Report (D-03 — code review before the SHW-16 sign-off)

**Reviewed:** 2026-07-29
**Depth:** standard
**Status:** issues_found — all eleven never-reviewed files plus Phase 20's own diff covered;
4 findings (0 critical, 2 warning, 2 info), disposition of each recorded in `20-REVIEW-FIX.md`.
An addendum below covers two commits (`1d139a7`, `ff577fc`) that landed after this document
closed, adding 1 warning and 2 info findings (7 total across the whole document; see the
addendum's own status line).

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

**Note (superseded by the addendum below):** at the time this section was written, `AeroTheme`
provided composition locals only and painted no background of its own. Commits `1d139a7`/`ff577fc`,
landing after this document closed, changed that file; see the addendum for the review of that
diff specifically. The finding-free verdict above stands for the file **as it existed at this
review's HEAD** and is not retroactively edited.

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

## Addendum: `1d139a7` / `ff577fc` — AeroTheme Background Establishment (post-close, pre-SHW-16-sign-off)

**Reviewed:** 2026-07-29 (addendum pass, after this document's original close)
**Depth:** standard, focused — exactly two commits, per D-03's requirement that code landing after
this review closed must clear review before the SHW-16 human sign-off
**Commits in scope:**
- `1d139a7` — `fix(20-06): AeroTheme establishes its themed background` (`library/.../theme/AeroTheme.kt`, `showcase/.../ShowcaseApp.kt`)
- `ff577fc` — `test(20-06): gate AeroTheme background establishment` (new file `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt`)

**Status:** issues_found — 0 critical, 1 warning (WR-03), 2 info (IN-03, IN-04). No blocker: the
core defect this pair of commits fixes (VER-05's naive-consumer light-background/dark-label
mismatch) is genuinely closed, the layout-constraint regression the author avoided is genuinely
avoided, and the new gate is genuinely falsifiable. The findings below are about the edges of the
fix, not its center.

### Verified clean (with evidence, not asserted)

**1. `Box` vs. `Surface` — `propagateMinConstraints` claim holds.** Traced Compose Foundation
Layout 1.11.1's actual measure policies rather than trusting the commit message:
`BoxMeasurePolicy` (default `propagateMinConstraints = false`, which `Box` uses and `Surface`'s
internal container does not) computes `contentConstraints = constraints.copy(minWidth = 0, minHeight
= 0)` for any non-`matchParentSize` child — i.e. content gets the incoming `max` back with `min`
zeroed, which is exactly the loose constraint content received pre-fix (no Box/Surface existed
between `MaterialTheme` and `content` at all). The two regression tests the commit message cites
were read directly, not trusted from prose: `AeroListItemLayoutTest.singleLineRowMeasuresExactly36Dp`
asserts `assertHeightIsEqualTo(36.dp)` and
`AeroRangeSliderHoverCancellationTest.hoverGlowDoesNotStickOnAfterEnabledToggleCancelsHoverPointerInput`
asserts pixel-level ring state around a fixed-width slider — both would fail immediately if the
old `Surface` attempt (`propagateMinConstraints = true`, forcing every non-filling child up to full
window height) had shipped instead, and both pass against the current `Box`-based code. No defect
found in this part of the diff.

**2. Double background — genuinely inert, not merely assumed inert.** `ShowcaseApp.kt`'s
`Surface(color = colors.background, modifier = Modifier.fillMaxSize())` has no `tonalElevation`/
`shadowElevation`/`shape`/`border` argument, so all default to `0.dp`/`RectangleShape`/`null`.
Material3's `Surface` only applies a tonal-elevation color overlay when the caller's `color`
parameter equals `MaterialTheme.colorScheme.surface` (its own internal elevation-tint branch) —
here the caller passes an explicit, different `color = colors.background`, so no overlay applies
regardless of elevation value, and elevation is `0.dp` in any case. The new `AeroTheme`-level `Box`
paints the identical `colorScheme.background` via a flat `.background(...)` modifier with no shape/
shadow of its own. Same color, same flatness, same shape (rectangle, unclipped) in both layers —
confirmed via Material3's actual elevation-overlay precondition, not merely "looks the same."
No defect found.

**3. New gate — falsifiability is real, not vacuous.** `AeroThemeBackgroundEstablishmentTest`'s RED
half (`establishBackgroundFalseLeavesTheCornerUnpainted`) drives the real, shipped
`establishBackground = false` code path — no reverted or hand-edited fixture — and asserts the
sampled corner pixel is `assertNotEquals` to `AeroColorScheme.AeroBlue.background`; since
`AeroColorScheme.AeroBlue.background = Color(0xFF0D1B2A)` is a distinctive, fully-opaque dark navy
(confirmed in `AeroColorScheme.kt:53`) vastly unlike any Compose Desktop test-harness default
backdrop, this assertion is a real, non-degenerate check that the sampling methodology can detect
an unpainted background, not a tautology. The GREEN half exercises the real production default
(`establishBackground` omitted) and does an exact `assertEquals` against the same opaque color —
exact-equality is safe here specifically because the background color has `alpha = 0xFF` (no
blending/antialiasing ambiguity at a solid-fill corner pixel). `onRoot()` (not a tagged node) is
the correct scope for this specific defect class, since the defect is about the full window canvas
outside whatever `content` itself draws — a tagged-node capture could never see that. This test
would fail today if a future change silently removed the `Box`/background from `AeroTheme`. No
defect found.

**4. `fillMaxSize()` inside an unbounded parent does not crash.** Risk item 5 asked whether the new
`Box`'s `Modifier.fillMaxSize()` is safe if `AeroTheme {}` is embedded inside a constrained/unbounded
parent (e.g. a vertically-scrolling `Column` item, which measures children with an unbounded max
height). This was **not** taken on faith: `FillNode.measure-3p2s80s` was read directly from the
decompiled `foundation-layout-desktop-1.11.1.jar` bytecode. The width/height override branch is
gated on `Constraints.getHasBoundedWidth-impl`/`getHasBoundedHeight-impl`; when a dimension is
unbounded, that branch is skipped entirely rather than throwing — this Compose version's
`fillMaxSize()` degrades gracefully on unbounded constraints rather than crashing (older Compose
releases used to throw `IllegalStateException` here; this codebase's pinned `composeMultiplatform =
"1.11.1"` does not). So embedding `AeroTheme {}` inside a scrolling/unbounded parent will not crash;
the caveat is narrower than a crash risk (see IN-03 below) and is, in any case, a generic
`fillMaxSize()`-in-a-scroll-container caveat applicable to any Compose layout, not something this
diff introduced.

### WR-03: New public parameter is source-compatible but not verified binary-compatible for a JitPack-published artifact

**File:** `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt:86` (`establishBackground:
Boolean = true`, inserted between `typography` and the trailing `content` lambda)

**Issue:** `library/build.gradle.kts` applies `maven-publish` and the module is distributed via
JitPack by git tag (confirmed in `20-06-PLAN.md`'s own mechanism and `library/build.gradle.kts`'s
`publishing {}` block) — this is a genuinely externally-consumed published artifact, not an
internal-only module. Inserting a new parameter into a public `@Composable` function is
source-compatible by construction (Kotlin resolves the trailing-lambda `content` positionally
against the *last* parameter regardless of how many defaulted parameters precede it, so every
existing `AeroTheme { ... }` / `AeroTheme(colorScheme = ...) { ... }` call site — recompiled against
the new source — keeps compiling with zero edits; verified this is in fact the only call-site shape
in this repository via `grep -rn "AeroTheme(" --include="*.kt"`, one production call site in
`showcase/.../Main.kt` plus ~12 files using the bare `AeroTheme { ... }` form). What was **not**
established, and the commit does not claim to have checked, is binary compatibility: Kotlin compiles
default-parameter functions to a synthetic bridge method carrying a bitmask over the parameter list;
adding a parameter changes that bitmask's shape and the primary method's descriptor. A consumer
holding an **already-compiled** artifact built against a previous published version of this library
(rather than one recompiled from source against the new version) would fail to link against a new
publish of this JAR with a `NoSuchMethodError`-class failure if it called `AeroTheme` positionally/by
name in a way that resolved to the old descriptor. No `@JvmOverloads` is used anywhere in this file
or, per a full-library grep, anywhere else in `library/src/main` — no binary-compatibility validator
(`kotlinx-binary-compatibility-validator` or similar) is configured in either `build.gradle.kts`
either — so this is a systemic gap in the library's public-API-evolution discipline that this commit
inherits and does not introduce new to this file specifically, but this is the first commit in the
milestone to add a *parameter* (rather than a data-class field, as `AeroColorScheme.ornamentOverride`
did, which is a structurally different and lower-risk kind of addition) to an existing public
`@Composable` signature, so it is the first place this gap becomes concretely exercisable.

**Fix:** Either (a) add `@JvmOverloads` to `AeroTheme` so the compiler emits the pre-existing
3-parameter overload as a real, separately-callable JVM method alongside the new 4-parameter one
(restores binary compatibility for old callers, zero source change required elsewhere), or (b)
explicitly accept and document that this library's published-artifact compatibility guarantee is
source-level only (recompile-to-upgrade), consistent with its current practice everywhere else, so
this is not treated as a regression unique to this diff. Either is a reasonable disposition; leaving
it silently undecided is the actual gap. **Severity: warning.**

### IN-03: `establishBackground` default changes rendered output for every existing embedding pattern, with no changelog trail

**File:** `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt:75-80` (KDoc), `86`
(the parameter default itself)

**Issue:** `establishBackground` defaults to `true`, so as of this commit *every* consumer of
`AeroTheme {}` — including a hypothetical existing consumer who was deliberately relying on
`AeroTheme`'s prior no-op-background behavior to compose it inside a foreign surface that already
owns its own background/blur/translucency — gets an opaque `colorScheme.background` painted behind
their content whether they asked for it or not, unless they add `establishBackground = false`. This
is the intended fix for VER-05 and is clearly the right default for the overwhelmingly common case
(a naive consumer with no `Surface` of their own), and it is thoroughly documented in the function's
own KDoc (`@param establishBackground` explicitly names the opt-out and when to use it) — but there
is no `CHANGELOG.md` anywhere in this repository (confirmed: none exists at the repo root) and the
root `build.gradle.kts`'s `version = "2.0.4"` field is deliberately left unbumped this entire
milestone (per `20-06-PLAN.md`'s own `<planner_assumptions>`, since JitPack resolves by git tag, not
this field) — so there is no version-level signal anywhere that a default rendering behavior changed
for `AeroTheme`, only the KDoc a consumer would have to think to go read. This is partially
self-mitigating: JitPack tags are immutable, so any consumer already pinned to an existing tag is
unaffected; the exposure is limited to future consumers who pull a tag cut after this commit. Still,
"the opt-out is discoverable enough to be a real remedy" (risk item 5) is true only for a consumer
who already suspects `AeroTheme` might be painting something and goes looking — it is not surfaced
anywhere a first-time integrator would trip over it before shipping (not in the showcase, not in the
scratch-consumer's own `Main.kt`, not in any top-level README/CHANGELOG this repository does not
have).

**Fix:** No code change required to close this — it is a documentation-and-release-process gap, not
a defect in the diff. If/when this library gains a `CHANGELOG.md` or release notes process, this
change belongs in it as a "behavior change" entry, not just an "addition" entry. **Severity: info.**

### IN-04: New test's KDoc slightly overstates its own precedent

**File:** `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt:25-26`

**Issue:** The class KDoc states the `onRoot().captureToImage()` idiom is "the same
`captureToImage()`/`toPixelMap()` idiom already proven in
`AeroRangeSliderHoverCancellationTest`" — true only for the `captureToImage()`/`toPixelMap()` half.
`AeroRangeSliderHoverCancellationTest` actually calls
`onNodeWithTag("slider").captureToImage()` (a tagged-node capture scoped to the slider's own
bounds), not `onRoot()` (a full-window capture) — a distinction the same KDoc correctly explains two
sentences later ("`onRoot()` is used ... specifically because the defect ... could never" be seen by
a node-scoped capture). The precedent citation is imprecise about which *part* of the prior test it
is reusing; it does not misstate anything that affects the test's own correctness, since the
following sentences self-correct the scoping distinction accurately.

**Fix:** Reword the citation to something like "the same `captureToImage()`/`toPixelMap()` pixel-
sampling idiom already proven in `AeroRangeSliderHoverCancellationTest`, here scoped to `onRoot()`
instead of a tagged node — see below for why." Purely a comment-precision nit; no test behavior is
affected. **Severity: info.**

### Addendum disposition summary

| ID | Severity | Finding | Disposition |
|----|----------|---------|-------------|
| WR-03 | warning | `AeroTheme`'s new `establishBackground` parameter is source- but not verified binary-compatible for the JitPack-published artifact; no `@JvmOverloads`/ABI validator anywhere in the library | recorded, not fixed here — routed to maintainer/backlog as an API-evolution-policy decision, not a local mechanical fix |
| IN-03 | info | `establishBackground = true` default silently changes rendered output for any future consumer wanting a transparent `AeroTheme {}` slot; no changelog exists to record this as a behavior change | recorded — documentation/process gap, not a code defect |
| IN-04 | info | New test's KDoc slightly overstates precedent from `AeroRangeSliderHoverCancellationTest` (tagged-node vs. `onRoot()` capture) | recorded — comment-precision nit only |

---

_Reviewed: 2026-07-29_
_Reviewer: Claude (gsd-executor, inline review per this plan's `planner_assumptions`)_
_Depth: standard_

_Addendum reviewed: 2026-07-29_
_Addendum reviewer: Claude (gsd-code-reviewer, dispatched for commits `1d139a7`/`ff577fc` per D-03)_
_Addendum depth: standard, scope-limited to the two named commits_
