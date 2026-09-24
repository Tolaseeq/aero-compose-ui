# Phase 21 Plan 10: Post-Upgrade Comparison (VER-07 / VER-08)

Agent's own post-upgrade sweep: every showcase frame and every UI-test state re-captured on the
final toolchain (Kotlin 2.4.20 / Compose Multiplatform 1.12.0 / kotlinx-coroutines 1.11.0 /
kotlinx-datetime 0.8.0 / JUnit 6.1.3 / Gradle 9.7.1 / JDK 21), compared against the pre-upgrade
baseline (`21-BASELINE.md`, Kotlin 2.4.10 / Compose Multiplatform 1.11.1), and reviewed frame by
frame. Nothing is fixed here (D-04) — every difference outside the named `21-noise-regions.json`
regions is localised, run-to-run-tested (D-05) and described below for the single Plan 11 list.

## Showcase (VER-07)

### Method

**After-run.** `.captures\new-kt2.4.20-cmp1.12.0\showcase\runA6` is the after-run: 75 frames (17
sections x 3 themes, every page), 3 captures each, `AERO_SWEEP_DONE frames=75`, toolchain block
confirms kotlin 2.4.20 / composeMultiplatform 1.12.0 / kotlinxCoroutines 1.11.0 / kotlinxDatetime
0.8.0 / junit 6.1.3 / gradle 9.7.1 / jvm 21.0.9 Microsoft. Same directory folder-name convention,
same `Invoke-ShowcaseSweep.ps1` driver and parameters as the baseline. DPI check: every one of
runA6's 75 frames reports 96 DPI (`GetDpiForWindow`), identical to every baseline frame in
`21-BASELINE.md` — frames are pixel-comparable, nothing was rescaled.

**Aborted prior runs (never used, never deleted).** Five earlier `showcase\runA*` directories in
the same folder predate runA6 and are not part of this comparison:
- `runA` — 9 frames; this was the previous executor's background sweep, killed when that executor
  returned before the sweep finished.
- `runA2` (~155 frames), `runA4`, `runA5` — the old capture guard in `Invoke-AeroWindowCapture`
  treated the maintainer's own mouse moves and window switches as a violation and aborted the
  sweep. A foreground sampler running beside runA5/runA6 recorded only the editor, browser and
  Explorer as foreground owners during that window, never the showcase JVM — the guard was wrong,
  not the maintainer's activity.
- `runA3` — system-wide commit-memory exhaustion at 23:48 on 2026-09-23 (`hs_err_pid*.log`,
  "insufficient memory"; the showcase process itself used only ~260 MB).

**Guard fix.** The capture guard was fixed in commit `9834e8c` (`tools/capture/AeroCapture.ps1`):
it now throws only when the captured app's own process takes the foreground; unrelated foreground
changes are recorded as `ExternalActivity` instead of aborting the sweep. `PrintWindow`, the
background-colour sanity check, and pixel output are unchanged, so runA6's frames stay comparable
with the baseline. runA6 recorded `externalActivity: true` on 10 of 75 frames (`AeroBlue/Icons/p0`,
`AeroBlue/Selection/p0`, `AeroBlue/Dropdown/p0`, `AeroBlue/Overlays/p0`, `AeroBlue/Layout/p4`,
`AeroDark/Buttons/p0`, `AeroDark/Input/p0`, `AeroDark/Selection/p1`, `AeroDark/Data/p0`,
`Classic/Selection/p0`) — that is the maintainer working at the keyboard/mouse while the sweep ran
in the background, not a finding; none of those 10 keys appear among the outside-noise categories
below with an unexplained large diff (see Category A/D/C — all tiny, deterministic, present on
every key of their pattern regardless of `externalActivity`).

**A pre-existing tooling bug was found and fixed before this comparison could run at all.**
`tools/capture/Compare-AeroCaptures.ps1`'s `-Mode Compare` and `-Mode ContactSheet` had never been
exercised before this plan (Plans 01–04 only used `-Mode Noise`/`-Mode SelfTest`). Two independent
PowerShell 5.1 pipeline-unrolling bugs made every `-Mode Compare` run silently collapse 225 real
per-file entries into a single malformed entry (`Get-AeroFrameEntries ... | Where-Object { $_.Capture
-eq 1 }` triggered PowerShell's collection member-enumeration on a comma-protected `List<object>`
instead of iterating its elements — the array-`-eq` comparison passed the *whole list* through as
one item), and separately caused `ConvertTo-Json` to throw for any key whose per-key noise-region
list held exactly one region (`$keyNoise = if (...) { $x } else { @() }` collapses a genuine
one-element array to a bare scalar on assignment). Both are fixed with the standard PowerShell
idioms (`foreach` over the raw function call instead of piping through `Where-Object`; wrapping the
if/else in `@(...)`) in the same file — no library or showcase code was touched. `-Mode SelfTest`
was re-run and still passes after the fix.

**Comparison.** `Compare-AeroCaptures.ps1 -Mode Compare -BeforeDir
.captures\old-kt2.4.10-cmp1.11.1\showcase\runA -AfterDir
.captures\new-kt2.4.20-cmp1.12.0\showcase\runA6 -Kind showcase -NoiseJson 21-noise-regions.json
-DiffDir .captures\diff\showcase -OutJson .captures\diff\compare-showcase.json`. Reference = `c1`
of `runA` (before) vs `c1` of `runA6` (after), per key.

**D-05 run-to-run check.** Every one of the 75 keys carried at least one outside-noise rect (see
Totals), so the full driver was re-run for all 75 keys as a second independent new-toolchain run,
split into four foreground batches to respect the 10-minute-per-command limit (`runB-1`:
ThemeSwitcher/Verification/Foundation/Primitives, 15 keys; `runB-2`: Icons/Buttons/Input/Selection/
Dropdown, 18 keys; `runB-3`: Range/List/Containers/Overlays/Navigation, 18 keys; `runB-4`:
Data/Pickers/Layout, 24 keys — 15+18+18+24=75). All four batches report DPI 96 and the identical
final toolchain block. `Compare-AeroCaptures.ps1 -Mode Noise -RunDirs runA6,runB-1,runB-2,runB-3,
runB-4 -Kind showcase -OutJson .captures\diff\noise-new.json` unions the touched cells across
runA6's own c2/c3 and every runB capture against runA6's c1, per key — the same methodology
`21-NOISE.md` used for the pre-upgrade baseline (runA c1 as reference, runA c2/c3 + runB c1/c2/c3
as the 5 comparisons). A key is `stable` in `noise-new.json` when all 6 new-toolchain captures are
pixel-identical at that key; `varies-run-to-run` otherwise.

**Classification.** For every key, each `Compare-AeroCaptures.ps1 -Mode Compare` diff region was
classified `insideNoise`/`outsideNoise` against `21-noise-regions.json`, then each key with an
outside-noise rect was labelled `stable` (real, reproducible before/after difference — a genuine
finding for Plan 11) or `varies-run-to-run` (the new toolchain itself does not reproduce byte-
identical frames at that key, so the before/after diff reflects run-to-run jitter, not a fixed
before/after change) using `noise-new.json`.

### Totals

75 keys compared, 0 missing (every baseline key exists in runA6), 0 identical, 0 inside-noise-only,
**75 with an outside-noise rect**. This is not 75 unrelated regressions: the diffs fall into 8
patterns (A–H below), 7 of which are either a known animation-driven noise source re-observed at a
slightly different phase, a single tiny deterministic icon-rendering delta repeated at the same
screen location in nearly every frame, or a one-off capture flake independently disproved by the
D-05 re-sweep. Only one pattern (H) is a genuine, unexplained content difference confined to a
single frame.

| Cat | Keys | diffPx range | Verdict | What it is |
|---|---|---|---|---|
| A | 58 | 4px (0.0004%) | stable | Window-chrome maximize/restore glyph, one fixed screen location `~(1110,6,12,12)`, present in every frame regardless of section/content |
| B | 1 (`AeroDark/List/p1`) | 4px | varies-run-to-run | Same screen location/magnitude as A, but the D-05 re-sweep did not reproduce it byte-identically — see below |
| C | 3 (`*/Layout/p2`) | 17px (0.0018%) | stable | Collapsible-panel header chevron/action-icon glyphs in the `AeroPanelGroup` demo |
| D | 3 (`*/Input/p0`) | 28–30px (0.003%) | stable | `AeroPasswordField`'s show/hide-password eye icon glyph |
| E | 3 (`*/Range/p0`) | 736–1214px (0.08–0.13%) | varies-run-to-run | Already-named `AeroProgressBar (ind)` indeterminate shimmer + `(det, sheen)` row — both wall-clock-animated, sampled at a different phase |
| F | 5 (`*/Layout/p3`,`*/Layout/p4` except Classic/p3) | 84–115px (0.01%) | varies-run-to-run | Already-named recompose-drive-counter noise (ticking digit) plus a few Category-A/C icon glyphs just outside the tightly-fitted baseline noise rect |
| G | 1 (`Classic/Layout/p3`) | 398 566px (41.5%) | varies-run-to-run | One-off scroll-settle flake — see below |
| H | 1 (`Classic/Data/p1`) | 37 941px (3.95%) | varies-run-to-run | Real-cursor hover artifact on an `AeroDataTable` row — see below |

### Per-key table

`Key | diffPx | % | maxΔ | category | run-to-run verdict | highlight (.captures/-relative)`

| Key | diffPx | % | maxΔ | Cat | Run-to-run | Highlight |
|---|---|---|---|---|---|---|
| AeroBlue/Buttons/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Buttons/p0.png |
| AeroBlue/Containers/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Containers/p0.png |
| AeroBlue/Data/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Data/p0.png |
| AeroBlue/Data/p1 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Data/p1.png |
| AeroBlue/Dropdown/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Dropdown/p0.png |
| AeroBlue/Foundation/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Foundation/p0.png |
| AeroBlue/Icons/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Icons/p0.png |
| AeroBlue/Input/p0 | 30 | 0.0031 | 60 | D | stable | .captures/diff/showcase/AeroBlue/Input/p0.png |
| AeroBlue/Layout/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Layout/p0.png |
| AeroBlue/Layout/p1 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Layout/p1.png |
| AeroBlue/Layout/p2 | 17 | 0.0018 | 85 | C | stable | .captures/diff/showcase/AeroBlue/Layout/p2.png |
| AeroBlue/Layout/p3 | 96 | 0.01 | 176 | F | varies-run-to-run | .captures/diff/showcase/AeroBlue/Layout/p3.png |
| AeroBlue/Layout/p4 | 99 | 0.0103 | 176 | F | varies-run-to-run | .captures/diff/showcase/AeroBlue/Layout/p4.png |
| AeroBlue/List/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/List/p0.png |
| AeroBlue/List/p1 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/List/p1.png |
| AeroBlue/Navigation/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Navigation/p0.png |
| AeroBlue/Overlays/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Overlays/p0.png |
| AeroBlue/Pickers/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Pickers/p0.png |
| AeroBlue/Primitives/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Primitives/p0.png |
| AeroBlue/Range/p0 | 830 | 0.0865 | 164 | E | varies-run-to-run | .captures/diff/showcase/AeroBlue/Range/p0.png |
| AeroBlue/Selection/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Selection/p0.png |
| AeroBlue/Selection/p1 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Selection/p1.png |
| AeroBlue/ThemeSwitcher/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/ThemeSwitcher/p0.png |
| AeroBlue/Verification/p0 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Verification/p0.png |
| AeroBlue/Verification/p1 | 4 | 0.0004 | 60 | A | stable | .captures/diff/showcase/AeroBlue/Verification/p1.png |
| AeroDark/Buttons/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Buttons/p0.png |
| AeroDark/Containers/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Containers/p0.png |
| AeroDark/Data/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Data/p0.png |
| AeroDark/Data/p1 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Data/p1.png |
| AeroDark/Dropdown/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Dropdown/p0.png |
| AeroDark/Foundation/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Foundation/p0.png |
| AeroDark/Icons/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Icons/p0.png |
| AeroDark/Input/p0 | 30 | 0.0031 | 54 | D | stable | .captures/diff/showcase/AeroDark/Input/p0.png |
| AeroDark/Layout/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Layout/p0.png |
| AeroDark/Layout/p1 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Layout/p1.png |
| AeroDark/Layout/p2 | 17 | 0.0018 | 79 | C | stable | .captures/diff/showcase/AeroDark/Layout/p2.png |
| AeroDark/Layout/p3 | 115 | 0.012 | 160 | F | varies-run-to-run | .captures/diff/showcase/AeroDark/Layout/p3.png |
| AeroDark/Layout/p4 | 97 | 0.0101 | 160 | F | varies-run-to-run | .captures/diff/showcase/AeroDark/Layout/p4.png |
| AeroDark/List/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/List/p0.png |
| AeroDark/List/p1 | 4 | 0.0004 | 54 | B | varies-run-to-run | .captures/diff/showcase/AeroDark/List/p1.png |
| AeroDark/Navigation/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Navigation/p0.png |
| AeroDark/Overlays/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Overlays/p0.png |
| AeroDark/Pickers/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Pickers/p0.png |
| AeroDark/Primitives/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Primitives/p0.png |
| AeroDark/Range/p0 | 736 | 0.0767 | 190 | E | varies-run-to-run | .captures/diff/showcase/AeroDark/Range/p0.png |
| AeroDark/Selection/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Selection/p0.png |
| AeroDark/Selection/p1 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Selection/p1.png |
| AeroDark/ThemeSwitcher/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/ThemeSwitcher/p0.png |
| AeroDark/Verification/p0 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Verification/p0.png |
| AeroDark/Verification/p1 | 4 | 0.0004 | 54 | A | stable | .captures/diff/showcase/AeroDark/Verification/p1.png |
| Classic/Buttons/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Buttons/p0.png |
| Classic/Containers/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Containers/p0.png |
| Classic/Data/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Data/p0.png |
| Classic/Data/p1 | 37941 | 3.9522 | 51 | H | varies-run-to-run | .captures/diff/showcase/Classic/Data/p1.png |
| Classic/Dropdown/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Dropdown/p0.png |
| Classic/Foundation/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Foundation/p0.png |
| Classic/Icons/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Icons/p0.png |
| Classic/Input/p0 | 28 | 0.0029 | 51 | D | stable | .captures/diff/showcase/Classic/Input/p0.png |
| Classic/Layout/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Layout/p0.png |
| Classic/Layout/p1 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Layout/p1.png |
| Classic/Layout/p2 | 17 | 0.0018 | 74 | C | stable | .captures/diff/showcase/Classic/Layout/p2.png |
| Classic/Layout/p3 | 398566 | 41.5173 | 225 | G | varies-run-to-run | .captures/diff/showcase/Classic/Layout/p3.png |
| Classic/Layout/p4 | 84 | 0.0088 | 174 | F | varies-run-to-run | .captures/diff/showcase/Classic/Layout/p4.png |
| Classic/List/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/List/p0.png |
| Classic/List/p1 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/List/p1.png |
| Classic/Navigation/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Navigation/p0.png |
| Classic/Overlays/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Overlays/p0.png |
| Classic/Pickers/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Pickers/p0.png |
| Classic/Primitives/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Primitives/p0.png |
| Classic/Range/p0 | 1214 | 0.1265 | 125 | E | varies-run-to-run | .captures/diff/showcase/Classic/Range/p0.png |
| Classic/Selection/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Selection/p0.png |
| Classic/Selection/p1 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Selection/p1.png |
| Classic/ThemeSwitcher/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/ThemeSwitcher/p0.png |
| Classic/Verification/p0 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Verification/p0.png |
| Classic/Verification/p1 | 4 | 0.0004 | 51 | A | stable | .captures/diff/showcase/Classic/Verification/p1.png |

### Observations (agent review)

**Category A (58 keys) — window-chrome maximize/restore glyph, stable.** Viewed before/after/
highlight for `AeroBlue/Buttons/p0`, `AeroDark/Buttons/p0`, `Classic/Buttons/p0` (one per theme,
representative — the diff is byte-identical in screen location, 12x12 region at approximately
`(1110,6)`, and magnitude per theme across all 58 keys, confirmed programmatically via
`compare-showcase.json`: same `diffPx=4`, same region size, and a `maxChannelDelta` that is
constant per theme — 60 for AeroBlue, 54 for AeroDark, 51 for Classic — regardless of section
content). The highlight rectangle sits exactly on the custom title bar's restore/maximize icon,
present identically in every frame because it is chrome, not section content. This is a tiny but
real, reproducible (`stable` in the D-05 re-sweep) rendering delta in that one glyph's anti-
aliasing, consistent with a Skia/text-rendering change between Compose Multiplatform 1.11.1 and
1.12.0. Cosmetic, sub-pixel, invisible without a diff tool — a genuine but minor finding for Plan
11, not a defect to fix here (D-04).

**Category B (1 key, `AeroDark/List/p1`) — same glyph, not reproduced stable.** Viewed before/
after/highlight; visually identical to the Category A pattern (same location, same 4px count,
maxΔ=54). Programmatically this key alone did not classify `stable` in the D-05 re-sweep
(`noise-new.json`), meaning at least one of the 6 new-toolchain captures at this key was not byte-
identical to the others — most likely a one-frame anti-aliasing jitter at the exact same glyph, not
a distinct phenomenon from Category A. Flagged separately rather than folded into A because the
run-to-run verdict differs, per the plan's "nothing is filtered or dropped" rule (D-05).

**Category C (3 keys, `*/Layout/p2`) — panel-header icon glyphs, stable.** Viewed
`AeroBlue/Layout/p2` before/after/highlight. The highlighted rects sit on the collapsible-panel
chevron/caret icon and a header action-icon (drag-handle dots) in the `AeroPanelGroup` demo. Same
family as Category A — a small, reproducible icon-glyph anti-aliasing delta, just at a different
icon than the window-chrome one. Not viewed individually for `AeroDark/Layout/p2` and
`Classic/Layout/p2`; their `diffPx`/`maxΔ`/region-count triple matches `AeroBlue/Layout/p2`'s
pattern exactly in `compare-showcase.json`, and both are `stable` in the D-05 re-sweep.

**Category D (3 keys, `*/Input/p0`) — password-field eye icon, stable.** Viewed
`AeroBlue/Input/p0` before/after/highlight. The highlighted rect sits exactly on
`AeroPasswordField`'s show/hide-password eye icon. Same icon-glyph-delta family as A/C. Not viewed
individually for `AeroDark/Input/p0` and `Classic/Input/p0`; both `stable` in the D-05 re-sweep
with the matching pattern.

**Category E (3 keys, `*/Range/p0`) — known shimmer/sheen animation, varies-run-to-run.** Viewed
`AeroBlue/Range/p0`, `AeroDark/Range/p0`, `Classic/Range/p0` before/after/highlight (all three,
since this is the section the plan explicitly calls out for a per-theme check). The highlighted
rects sit on the `AeroProgressBar (ind)` indeterminate bar and the `AeroProgressBar (det, sheen)`
row directly above it — both wall-clock-animated (the `(ind)` bar via
`rememberInfiniteTransition`; the `(det, sheen)` row is the same shimmer treatment applied to a
determinate bar). This is the exact already-named noise source from `21-NOISE.md`'s Range-section
entry, just sampled at a different point in the animation cycle between the old and new toolchain's
captures (and between runA6's own repeat captures). Correctly classified `varies-run-to-run`; no
new noise source.

**Category F (5 keys, `*/Layout/p3`/`*/Layout/p4` except `Classic/Layout/p3`) — known recompose
counter plus a few Category-A/C icon deltas, varies-run-to-run.** Viewed `AeroBlue/Layout/p3`,
`AeroDark/Layout/p3`, `AeroBlue/Layout/p4`, `AeroDark/Layout/p4`, `Classic/Layout/p4` before/after/
highlight (all five). Each shows the already-named `21-NOISE.md` recompose-drive-counter digit text
plus, in the 8–9-region cases, a handful of small header/action-icon glyphs (same family as
Category C) that sit just outside the tightly-fitted baseline noise rectangle. No new content or
layout difference. Correctly classified `varies-run-to-run` — same known noise source, slightly
wider footprint than the pre-upgrade baseline's single named rect per key.

**Category G (1 key, `Classic/Layout/p3`) — one-off scroll-settle flake, varies-run-to-run.**
Viewed before (`old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p3-c1.png`), after
(`new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p3-c1.png`) and the highlight
(41.5% of the frame, magenta). The "before" frame shows the correct page-3 content (the two
`AeroPanelGroup` demos near the bottom of the page). The "after" frame instead shows content from
roughly page 2's scroll position (`AeroSidebar` + `AeroStepperWizard`), despite the manifest
reporting the identical `scrollPx=2304`/`contentPx=3316` target on both sides — i.e. the scroll
target was correct but the frame was captured before Compose's scroll had visually settled to that
target. **Disproved as a stable toolchain regression:** the D-05 re-sweep's independent capture of
the same key (`runB-4/Classic/Layout-p3-c1.png`) was viewed and shows the correct page-3 content,
matching the baseline. `noise-new.json` confirms `stable: false` for this key — the new toolchain
does not reproduce this frame byte-identically, consistent with a one-off capture-timing flake
rather than a systematic regression. Still worth naming to Plan 11 as a capture-pipeline
robustness note: `SettleMs=1500` occasionally is not enough for a 5-page, 3316px-tall section under
load.

**Category H (1 key, `Classic/Data/p1`) — real-cursor hover artifact, varies-run-to-run.** Viewed
before, after and highlight. The "after" frame shows the `AeroDataTable`'s last visible row
(`SAT-009`) rendered with a hover highlight that is absent in "before." This key was **not** among
runA6's 10 `externalActivity: true` frames, because the capture guard (fixed in `9834e8c`) only
detects the captured window's own process taking the OS foreground — it does not detect the
maintainer's real cursor merely moving over the (non-focused, non-activated) capture window's
screen region, which is still enough for Compose to register a genuine pointer-hover state on
whatever row sits under the cursor at that moment. This is capture interference from real desktop
use during the sweep, not a toolchain rendering change — `noise-new.json` confirms `stable: false`.
Worth naming to Plan 11 as a capture-guard gap (hover-only interference is not currently detected or
retried), separate from the AeroDataTable component itself.

### Frames viewed with the Read tool

Before/after/highlight triples: `AeroBlue/Buttons/p0`, `AeroDark/Buttons/p0`, `Classic/Buttons/p0`
(Category A representative sample), `AeroBlue/Layout/p2` (Category C), `AeroBlue/Input/p0`
(Category D), `AeroBlue/Range/p0`, `AeroDark/Range/p0`, `Classic/Range/p0` (Category E, all three),
`AeroBlue/Layout/p3`, `AeroDark/Layout/p3`, `AeroBlue/Layout/p4`, `AeroDark/Layout/p4`,
`Classic/Layout/p4` (Category F, all five), `Classic/Layout/p3` before/after/highlight plus the
`runB-4` re-capture (Category G), `Classic/Data/p1` before/after/highlight (Category H) — 17 keys,
~52 image reads. The remaining 58 keys' `diffPx`/`maxΔ`/region-geometry were confirmed
programmatically identical to their viewed representative (Category A: 55 keys not individually
opened beyond the 3-theme sample; Category C/D: 2 keys each not individually opened beyond the
1-sample-per-category check) via `compare-showcase.json`, since PowerShell-side pixel counts,
region coordinates and per-theme `maxChannelDelta` are exact, not approximate, and a visual sample
per theme/category was confirmed to show the same glyph-location pattern.
