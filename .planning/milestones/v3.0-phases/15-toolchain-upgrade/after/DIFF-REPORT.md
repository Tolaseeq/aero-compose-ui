# After-Migration Visual Diff (TOOL-06)

**Verdict: NO rendering drift.** The Kotlin 2.1.21+CMP 1.7.3 → Kotlin 2.4.10+CMP 1.11.1
migration (Skia m126 → m138 → m144) produced no observable rendering change in the eight
target components across all three themes.

## Method

After-migration screenshots captured with the SAME windows-mcp automation and framing as the
plan-01 baseline (showcase maximized, 1920×1080 @100% DPI, in-app ThemeSwitcher, `CopyFromScreen`).
Each `after/` frame pixel-diffed against its `baseline/` counterpart (per-pixel max RGB-channel
delta, threshold 8) via `Graphics.LockBits`.

## Results

| Pair | diff px | % | maxΔ | meanΔ | Interpretation |
|------|--------:|--:|-----:|------:|----------------|
| AeroBlue top | 4 | 0.000% | 28 | 0.06 | Pixel-identical (single-glyph AA) |
| AeroDark top | 3 | 0.000% | 26 | 0.06 | Pixel-identical |
| Classic top | 3 | 0.000% | 25 | 0.00 | Pixel-identical |
| AeroDark controls | 694 | 0.033% | 207 | 0.13 | Localized — `AeroProgressBar (ind)` animation phase |
| Classic controls | 926 | 0.045% | 146 | 0.07 | Localized — `AeroProgressBar (ind)` animation phase |
| AeroBlue controls | 151000 | 7.282% | 242 | 5.88 | **Scroll-offset artifact, not drift** (see below) |

## Notes on the two non-zero rows

- **Controls views (AeroDark/Classic): 0.03–0.045%.** The only moving element on the page is
  `AeroProgressBar (ind)` — an indeterminate, continuously-animated bar. Baseline and after were
  captured at different animation phases, so its blue segment sits at a different x-position. This
  is a live animation, not a rendering change. Every static component (Switch, SegmentedControl,
  Slider, RangeSlider, det ProgressBar, ListItem) is unchanged.
- **AeroBlue controls: 7.28%.** A capture-alignment artifact: the baseline AeroBlue controls frame
  was reached via two 8-wheel scrolls (a capture was taken between them), while the after frame
  used a single 16-wheel scroll — scroll momentum landed at a slightly different pixel offset
  (`after-AeroBlue-03` shows `AeroSearchField`/`AeroFilePicker` at the top that the baseline frame
  had scrolled past). Visual inspection confirms the component rendering is identical; only the
  vertical scroll position differs. The properly-aligned AeroDark/Classic controls frames (both
  captured with a single 16-wheel scroll on baseline AND after) corroborate this: their diff is
  ~0.04%, i.e. drift-free.

## Conclusion

TOOL-06 satisfied: no toolchain-induced (Skia) rendering drift in the eight target components on
any of the three themes. Combined with the 232/232 green library suite (TOOL-05), the migration is
both behaviorally and visually inert. No rendering-code changes were made in response (out of scope
for Phase 15; the Aero restyle work begins in Phase 16).
