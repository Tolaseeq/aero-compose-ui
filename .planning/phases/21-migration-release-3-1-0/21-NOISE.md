# Phase 21: Run-to-Run Noise (BASE-03)

Named noise regions measured on the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1),
before any dependency bump, per D-05: the noise list is established here and any region added to
it after the upgrade must itself be shown to vary run-to-run on the new toolchain and be called
out on the drift stop list (D-04).

## Method

- Two independent full showcase sweeps (`tools/capture/Invoke-ShowcaseSweep.ps1`), `runA` and
  `runB`, each capturing every `Theme x Section x Page` 3 times 700ms apart (BASE-04's frame set,
  see `21-BASELINE.md`) — 6 captures per frame key total (3 per launch x 2 launches).
- Reference = `runA`'s first capture (`c1`) for that key.
- Every other capture of the same key (`runA` c2/c3, `runB` c1/c2/c3 — 5 comparisons) is diffed
  against the reference with `[AeroPixels]::DiffCells(ref, other, 8)` (8px cells).
- Every cell touched by any of those 5 comparisons is unioned, then merged into rectangles by
  8-connected adjacency (`tools/capture/Compare-AeroCaptures.ps1 -Mode Noise`), and each rectangle
  is padded by 2px (clamped to the frame bounds).
- Command: `Compare-AeroCaptures.ps1 -Mode Noise -RunDirs runA,runB -Kind showcase -OutJson
  21-noise-regions.json`.
- Output: `.planning/phases/21-migration-release-3-1-0/21-noise-regions.json`, `showcase` object,
  one entry per frame key: `{ captures, stable, dimensionMismatch, regions: [{x,y,w,h,px}] }`.

## Result summary

- **75 frame keys** (17 sections x 3 themes, multi-page sections captured on every page) — see
  `21-BASELINE.md` frame index.
- **66 keys stable** (identical across all 6 captures — 0 touched cells).
- **9 keys unstable**, all traced to two known, named, time-dependent sources below. No key's
  noise region covers more than 0.5% of its 1200x800 (960,000px) frame — well under the "more than
  2%" investigate-further threshold; none needed investigation as a non-deterministic layout.
- `dimensionMismatch` is 0 for every key (all captures of a given key were the same 1200x800 @ 96
  DPI).

## Named regions

| Frame key | Regions (x,y,w,h) | px | Named source |
|---|---|---|---|
| AeroBlue/Range/p0 | (230,414,52,20); (310,454,84,20); (230,454,28,20); (318,414,20,20) | 1078; 1436; 216; 116 | `AeroProgressBar (ind)` indeterminate shimmer — `rememberInfiniteTransition(label = "indeterminate")` (`AeroProgressBar.kt:181`), rendered in the "Range & Progress" showcase row |
| AeroDark/Range/p0 | (246,414,36,20); (318,454,76,20); (230,454,36,20); (326,414,12,20); (230,414,12,20) | 1008; 1340; 336; 34; 52 | same — `AeroProgressBar (ind)` indeterminate shimmer |
| Classic/Range/p0 | (230,414,60,20); (302,454,100,20); (230,454,44,20); (318,414,20,20) | 1166; 2080; 274; 125 | same — `AeroProgressBar (ind)` indeterminate shimmer |
| AeroBlue/Layout/p3 | (262,670,28,20) | 312 | Live-ticking recompose-drive counter — `LaunchedEffect` `delay(32L)` (~30fps) increments `recomposeDriveCounter`, rendered in "Drag the dividers while this counter ticks (N). Must stay 3 sections, never 9." (`showcase/.../sections/LayoutSection.kt:403-420`, RCMP-04 regression demo) |
| AeroBlue/Layout/p4 | (262,422,28,20) | 315 | same — recompose-drive counter text (same string, different on-screen y because page 4 is scrolled further) |
| AeroDark/Layout/p3 | (262,670,28,20) | 352 | same — recompose-drive counter text |
| AeroDark/Layout/p4 | (262,422,28,20) | 251 | same — recompose-drive counter text |
| Classic/Layout/p3 | (262,670,28,20) | 305 | same — recompose-drive counter text |
| Classic/Layout/p4 | (262,422,28,20) | 344 | same — recompose-drive counter text |

Every other showcase frame key (66 of 75) is byte-for-byte stable across all 6 captures — 0
touched cells, `stable: true` in `21-noise-regions.json`.

**Range section check (per plan instruction):** the Range section's noise is explicitly the
`AeroProgressBar (ind)` shimmer in all three themes — confirmed above, not "none varied".

## UI-test reference images noise (BASE-05, added in Task 3)

See "UI-test noise" below, appended after the Task 3 opt-in runs.

<!-- UITEST_NOISE_APPENDED -->
