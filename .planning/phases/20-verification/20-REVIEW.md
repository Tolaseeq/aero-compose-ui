---
phase: 20-verification
reviewed: 2026-07-29T00:00:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt
  - library/src/main/kotlin/com/mordred/aero/theme/ColorMath.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroOrnamentTokens.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt
  - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt
  - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchAeroShadowProof.kt
  - showcase/src/main/kotlin/com/mordred/showcase/scratch/ScratchSliderSlotSpike.kt
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 20: Code Review Report (D-03 — code review before the SHW-16 sign-off)

**Reviewed:** 2026-07-29
**Depth:** standard
**Status:** issues_found (in progress — Task 1 of 2 review tasks; Task 2 appends the
buttons/showcase tier plus Phase 20's own diff)

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

---

_Reviewed: 2026-07-29 (Task 1 of 2)_
_Reviewer: Claude (gsd-executor, inline review per this plan's `planner_assumptions`)_
_Depth: standard_
