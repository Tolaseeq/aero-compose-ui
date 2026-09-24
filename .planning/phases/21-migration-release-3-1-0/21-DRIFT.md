# Phase 21: Drift List (D-04, D-05)

Every difference outside the named noise regions (`21-noise-regions.json`) found by Plan 10's
post-upgrade comparison (`21-COMPARE.md`), for the maintainer's single D-04 ruling. Nothing here
was fixed — no file under `library/src` or `showcase/src` was touched to produce this list
(`git status --porcelain -- library/src showcase/src` is empty throughout this plan).

## Verdict

**102 items, in 8 groups by cause.** `compare-showcase.json` reports `keys=75, withOutside=75`
(every showcase key carries at least one outside-noise rect); `compare-uitest.json` reports
`keys=200, identical=173, withOutside=27`. 75 + 27 = 102, matching this list's item count exactly
(grouped items list every one of their keys below — nothing folded away, per D-05).

Every item below was independently viewed and categorised by the agent in `21-COMPARE.md`
(Plan 10, Categories A-H for showcase, the three UI-test sub-patterns) before this list existed;
this file re-groups those same 102 keys by root cause for a single maintainer decision, and adds
the `Ruling` column Plan 12 will fill in from the maintainer's reply.

| Group | Cause | Keys | Run-to-run | Proposed action |
|---|---|---|---|---|
| 1 | Skia/Compose text and icon-glyph anti-aliasing shift (Compose Multiplatform 1.11.1 -> 1.12.0) | 82 | stable | accept as upstream rendering change |
| 2 | Same glyph anti-aliasing shift, one key not reproduced byte-identical on the new toolchain | 1 | varies-run-to-run | add region to noise list |
| 3 | Popup border/shadow-edge anti-aliasing shift (same rendering-pipeline change, applied to `dropShadow`/border strokes) | 6 | stable | accept as upstream rendering change |
| 4 | Single-pixel text-cursor (caret) colour/alpha shift in `AeroComboBox`'s opened search field | 3 | stable | accept as upstream rendering change |
| 5 | `AeroProgressBar` indeterminate/sheen shimmer resampled at a different animation phase (already-named noise source, wider footprint than the baseline rect) | 3 | varies-run-to-run | add region to noise list |
| 6 | Recompose-drive counter text plus a few glyph deltas just outside the baseline noise rect (already-named noise source, wider footprint) | 5 | varies-run-to-run | add region to noise list |
| 7 | One-off scroll-settle capture-timing flake (`SettleMs=1500` insufficient for a tall, 5-page section under load) | 1 | varies-run-to-run | fix in code (capture tooling: raise `SettleMs` for `Classic/Layout`, or verify `scrollPx` reached before capture, in `tools/capture/AeroCapture.ps1`/`Invoke-ShowcaseSweep.ps1` — not library/showcase drawing code) |
| 8 | Real-cursor hover interference during capture (the capture guard detects foreground takeover, not hover-only pointer movement over an unfocused window) | 1 | varies-run-to-run | fix in code (capture tooling: extend the capture guard in `tools/capture/AeroCapture.ps1` to detect/retry on hover-state interference — not library/showcase drawing code) |

Total: 82 + 1 + 6 + 3 + 3 + 5 + 1 + 1 = **102**.

---

## Group 1 — Skia/Compose glyph anti-aliasing shift (82 keys, stable)

**Cause:** Compose Multiplatform 1.11.1 -> 1.12.0 changed sub-pixel anti-aliasing on text and icon
glyphs. Confirmed in `21-COMPARE.md` Categories A (58 keys — window-chrome maximize/restore glyph,
`~(1110,6,12,12)` in every showcase frame), C (3 keys — `AeroPanelGroup` header chevron/action-icon
glyphs), D (3 keys — `AeroPasswordField`'s show/hide-password eye icon), and the UI-test
"Text-label anti-aliasing delta" sub-pattern (18 keys — `AeroButton`, `AeroOutlinedButton`,
`AeroSegmentedControl`, `AeroDrawer`, `AeroTooltip`, `AeroPopover`, `AeroDateTimePicker`/
`AeroDateTimeRangePicker`'s Apply button, `Base05Proof`). All four sub-groups are the same
rendering-pipeline shift applied to different glyphs; `21-COMPARE.md` itself calls the UI-test
pattern "same family as showcase Category A". `maxChannelDelta` never exceeds 190; `diffPx` is a
few pixels to a few hundred, always confined to glyph edges — cosmetic, sub-pixel, invisible
without a diff tool. All `stable` in the D-05 re-sweep (`noise-new.json`/`noise-new-uitest.json`)
— reproducible, not run-to-run jitter.

**Proposed action:** accept as upstream rendering change. Nothing in this library's drawing code
produced this delta; it is Skia's own text/icon rasterization changing between engine versions.

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroBlue/Buttons/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Buttons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Buttons-p0-c1.png | .captures/diff/showcase/AeroBlue/Buttons/p0.png | |
| AeroBlue/Containers/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Containers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Containers-p0-c1.png | .captures/diff/showcase/AeroBlue/Containers/p0.png | |
| AeroBlue/Data/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Data-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Data-p0-c1.png | .captures/diff/showcase/AeroBlue/Data/p0.png | |
| AeroBlue/Data/p1 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Data-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Data-p1-c1.png | .captures/diff/showcase/AeroBlue/Data/p1.png | |
| AeroBlue/Dropdown/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Dropdown-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Dropdown-p0-c1.png | .captures/diff/showcase/AeroBlue/Dropdown/p0.png | |
| AeroBlue/Foundation/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Foundation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Foundation-p0-c1.png | .captures/diff/showcase/AeroBlue/Foundation/p0.png | |
| AeroBlue/Icons/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Icons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Icons-p0-c1.png | .captures/diff/showcase/AeroBlue/Icons/p0.png | |
| AeroBlue/Input/p0 | 30 | 0.0031 | 60 | stable | (1110,6,12,12); (366,270,28,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Input-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Input-p0-c1.png | .captures/diff/showcase/AeroBlue/Input/p0.png | |
| AeroBlue/Layout/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Layout-p0-c1.png | .captures/diff/showcase/AeroBlue/Layout/p0.png | |
| AeroBlue/Layout/p1 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Layout-p1-c1.png | .captures/diff/showcase/AeroBlue/Layout/p1.png | |
| AeroBlue/Layout/p2 | 17 | 0.0018 | 85 | stable | (1110,6,12,12); (94,302,12,20); (1118,302,20,20); (54,422,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p2-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Layout-p2-c1.png | .captures/diff/showcase/AeroBlue/Layout/p2.png | |
| AeroBlue/List/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/List-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/List-p0-c1.png | .captures/diff/showcase/AeroBlue/List/p0.png | |
| AeroBlue/List/p1 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/List-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/List-p1-c1.png | .captures/diff/showcase/AeroBlue/List/p1.png | |
| AeroBlue/Navigation/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Navigation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Navigation-p0-c1.png | .captures/diff/showcase/AeroBlue/Navigation/p0.png | |
| AeroBlue/Overlays/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Overlays-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Overlays-p0-c1.png | .captures/diff/showcase/AeroBlue/Overlays/p0.png | |
| AeroBlue/Pickers/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Pickers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Pickers-p0-c1.png | .captures/diff/showcase/AeroBlue/Pickers/p0.png | |
| AeroBlue/Primitives/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Primitives-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Primitives-p0-c1.png | .captures/diff/showcase/AeroBlue/Primitives/p0.png | |
| AeroBlue/Selection/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Selection-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Selection-p0-c1.png | .captures/diff/showcase/AeroBlue/Selection/p0.png | |
| AeroBlue/Selection/p1 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Selection-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Selection-p1-c1.png | .captures/diff/showcase/AeroBlue/Selection/p1.png | |
| AeroBlue/ThemeSwitcher/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/ThemeSwitcher-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/ThemeSwitcher-p0-c1.png | .captures/diff/showcase/AeroBlue/ThemeSwitcher/p0.png | |
| AeroBlue/Verification/p0 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Verification-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Verification-p0-c1.png | .captures/diff/showcase/AeroBlue/Verification/p0.png | |
| AeroBlue/Verification/p1 | 4 | 0.0004 | 60 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Verification-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Verification-p1-c1.png | .captures/diff/showcase/AeroBlue/Verification/p1.png | |
| AeroButton/AeroBlue/default | 163 | 0.7837 | 1 | stable | (0,22,66,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroButton/AeroBlue/default.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroButton/AeroBlue/default.png | .captures/diff/ui-tests/AeroButton/AeroBlue/default.png | |
| AeroButton/AeroBlue/focus | 159 | 0.7644 | 1 | stable | (0,22,66,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroButton/AeroBlue/focus.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroButton/AeroBlue/focus.png | .captures/diff/ui-tests/AeroButton/AeroBlue/focus.png | |
| AeroDark/Buttons/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Buttons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Buttons-p0-c1.png | .captures/diff/showcase/AeroDark/Buttons/p0.png | |
| AeroDark/Containers/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Containers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Containers-p0-c1.png | .captures/diff/showcase/AeroDark/Containers/p0.png | |
| AeroDark/Data/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Data-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Data-p0-c1.png | .captures/diff/showcase/AeroDark/Data/p0.png | |
| AeroDark/Data/p1 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Data-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Data-p1-c1.png | .captures/diff/showcase/AeroDark/Data/p1.png | |
| AeroDark/Dropdown/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Dropdown-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Dropdown-p0-c1.png | .captures/diff/showcase/AeroDark/Dropdown/p0.png | |
| AeroDark/Foundation/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Foundation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Foundation-p0-c1.png | .captures/diff/showcase/AeroDark/Foundation/p0.png | |
| AeroDark/Icons/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Icons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Icons-p0-c1.png | .captures/diff/showcase/AeroDark/Icons/p0.png | |
| AeroDark/Input/p0 | 30 | 0.0031 | 54 | stable | (1110,6,12,12); (366,270,28,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Input-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Input-p0-c1.png | .captures/diff/showcase/AeroDark/Input/p0.png | |
| AeroDark/Layout/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Layout-p0-c1.png | .captures/diff/showcase/AeroDark/Layout/p0.png | |
| AeroDark/Layout/p1 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Layout-p1-c1.png | .captures/diff/showcase/AeroDark/Layout/p1.png | |
| AeroDark/Layout/p2 | 17 | 0.0018 | 79 | stable | (1110,6,12,12); (94,302,12,20); (1118,302,20,20); (54,422,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p2-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Layout-p2-c1.png | .captures/diff/showcase/AeroDark/Layout/p2.png | |
| AeroDark/List/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/List-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/List-p0-c1.png | .captures/diff/showcase/AeroDark/List/p0.png | |
| AeroDark/Navigation/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Navigation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Navigation-p0-c1.png | .captures/diff/showcase/AeroDark/Navigation/p0.png | |
| AeroDark/Overlays/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Overlays-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Overlays-p0-c1.png | .captures/diff/showcase/AeroDark/Overlays/p0.png | |
| AeroDark/Pickers/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Pickers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Pickers-p0-c1.png | .captures/diff/showcase/AeroDark/Pickers/p0.png | |
| AeroDark/Primitives/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Primitives-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Primitives-p0-c1.png | .captures/diff/showcase/AeroDark/Primitives/p0.png | |
| AeroDark/Selection/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Selection-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Selection-p0-c1.png | .captures/diff/showcase/AeroDark/Selection/p0.png | |
| AeroDark/Selection/p1 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Selection-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Selection-p1-c1.png | .captures/diff/showcase/AeroDark/Selection/p1.png | |
| AeroDark/ThemeSwitcher/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/ThemeSwitcher-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/ThemeSwitcher-p0-c1.png | .captures/diff/showcase/AeroDark/ThemeSwitcher/p0.png | |
| AeroDark/Verification/p0 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Verification-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Verification-p0-c1.png | .captures/diff/showcase/AeroDark/Verification/p0.png | |
| AeroDark/Verification/p1 | 4 | 0.0004 | 54 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Verification-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Verification-p1-c1.png | .captures/diff/showcase/AeroDark/Verification/p1.png | |
| AeroDateTimePicker/AeroBlue/opened | 227 | 0.0289 | 1 | stable | (86,414,76,28) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroDateTimePicker/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroDateTimePicker/AeroBlue/opened.png | .captures/diff/ui-tests/AeroDateTimePicker/AeroBlue/opened.png | |
| AeroDateTimePicker/Classic/opened | 184 | 0.0234 | 1 | stable | (86,422,68,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroDateTimePicker/Classic/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroDateTimePicker/Classic/opened.png | .captures/diff/ui-tests/AeroDateTimePicker/Classic/opened.png | |
| AeroDateTimeRangePicker/AeroBlue/opened | 2 | 0.0003 | 1 | stable | (94,470,12,12); (142,470,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroDateTimeRangePicker/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroDateTimeRangePicker/AeroBlue/opened.png | .captures/diff/ui-tests/AeroDateTimeRangePicker/AeroBlue/opened.png | |
| AeroDrawer/AeroBlue/closed | 307 | 0.039 | 1 | stable | (0,22,114,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroDrawer/AeroBlue/closed.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroDrawer/AeroBlue/closed.png | .captures/diff/ui-tests/AeroDrawer/AeroBlue/closed.png | |
| AeroOutlinedButton/AeroBlue/default | 4 | 0.0192 | 1 | stable | (0,6,10,20); (46,6,12,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroOutlinedButton/AeroBlue/default.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroOutlinedButton/AeroBlue/default.png | .captures/diff/ui-tests/AeroOutlinedButton/AeroBlue/default.png | |
| AeroOutlinedButton/AeroBlue/focus | 6 | 0.0288 | 1 | stable | (0,6,10,28); (46,6,12,28) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroOutlinedButton/AeroBlue/focus.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroOutlinedButton/AeroBlue/focus.png | .captures/diff/ui-tests/AeroOutlinedButton/AeroBlue/focus.png | |
| AeroOutlinedButton/AeroBlue/hover | 6 | 0.0288 | 1 | stable | (0,6,10,28); (46,6,12,28) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroOutlinedButton/AeroBlue/hover.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroOutlinedButton/AeroBlue/hover.png | .captures/diff/ui-tests/AeroOutlinedButton/AeroBlue/hover.png | |
| AeroPopover/AeroBlue/closed | 169 | 0.0215 | 1 | stable | (14,38,68,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroPopover/AeroBlue/closed.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroPopover/AeroBlue/closed.png | .captures/diff/ui-tests/AeroPopover/AeroBlue/closed.png | |
| AeroPopover/AeroBlue/opened | 165 | 0.021 | 1 | stable | (14,38,68,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroPopover/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroPopover/AeroBlue/opened.png | .captures/diff/ui-tests/AeroPopover/AeroBlue/opened.png | |
| AeroSegmentedControl/AeroBlue/default | 7 | 0.0333 | 1 | stable | (30,14,12,20); (62,14,12,20); (94,14,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroSegmentedControl/AeroBlue/default.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroSegmentedControl/AeroBlue/default.png | .captures/diff/ui-tests/AeroSegmentedControl/AeroBlue/default.png | |
| AeroSegmentedControl/AeroBlue/focus | 7 | 0.0333 | 1 | stable | (30,14,12,20); (62,14,12,20); (94,14,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroSegmentedControl/AeroBlue/focus.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroSegmentedControl/AeroBlue/focus.png | .captures/diff/ui-tests/AeroSegmentedControl/AeroBlue/focus.png | |
| AeroSegmentedControl/AeroBlue/hover | 5 | 0.0238 | 1 | stable | (30,14,12,12); (62,14,12,20); (94,14,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroSegmentedControl/AeroBlue/hover.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroSegmentedControl/AeroBlue/hover.png | .captures/diff/ui-tests/AeroSegmentedControl/AeroBlue/hover.png | |
| AeroSegmentedControl/AeroBlue/press | 3 | 0.0143 | 1 | stable | (62,14,12,20); (94,14,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroSegmentedControl/AeroBlue/press.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroSegmentedControl/AeroBlue/press.png | .captures/diff/ui-tests/AeroSegmentedControl/AeroBlue/press.png | |
| AeroTooltip/AeroBlue/closed | 247 | 0.0314 | 1 | stable | (14,38,92,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroTooltip/AeroBlue/closed.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroTooltip/AeroBlue/closed.png | .captures/diff/ui-tests/AeroTooltip/AeroBlue/closed.png | |
| Base05Proof/AeroBlue/root | 163 | 0.0207 | 1 | stable | (0,22,66,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/Base05Proof/AeroBlue/root.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/Base05Proof/AeroBlue/root.png | .captures/diff/ui-tests/Base05Proof/AeroBlue/root.png | |
| Base05Proof/AeroBlue/tagged | 163 | 1.3583 | 1 | stable | (0,22,66,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/Base05Proof/AeroBlue/tagged.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/Base05Proof/AeroBlue/tagged.png | .captures/diff/ui-tests/Base05Proof/AeroBlue/tagged.png | |
| Classic/Buttons/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Buttons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Buttons-p0-c1.png | .captures/diff/showcase/Classic/Buttons/p0.png | |
| Classic/Containers/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Containers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Containers-p0-c1.png | .captures/diff/showcase/Classic/Containers/p0.png | |
| Classic/Data/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Data-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Data-p0-c1.png | .captures/diff/showcase/Classic/Data/p0.png | |
| Classic/Dropdown/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Dropdown-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Dropdown-p0-c1.png | .captures/diff/showcase/Classic/Dropdown/p0.png | |
| Classic/Foundation/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Foundation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Foundation-p0-c1.png | .captures/diff/showcase/Classic/Foundation/p0.png | |
| Classic/Icons/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Icons-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Icons-p0-c1.png | .captures/diff/showcase/Classic/Icons/p0.png | |
| Classic/Input/p0 | 28 | 0.0029 | 51 | stable | (1110,6,12,12); (366,270,20,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Input-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Input-p0-c1.png | .captures/diff/showcase/Classic/Input/p0.png | |
| Classic/Layout/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p0-c1.png | .captures/diff/showcase/Classic/Layout/p0.png | |
| Classic/Layout/p1 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p1-c1.png | .captures/diff/showcase/Classic/Layout/p1.png | |
| Classic/Layout/p2 | 17 | 0.0018 | 74 | stable | (1110,6,12,12); (94,302,12,20); (1118,302,20,20); (54,422,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p2-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p2-c1.png | .captures/diff/showcase/Classic/Layout/p2.png | |
| Classic/List/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/List-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/List-p0-c1.png | .captures/diff/showcase/Classic/List/p0.png | |
| Classic/List/p1 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/List-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/List-p1-c1.png | .captures/diff/showcase/Classic/List/p1.png | |
| Classic/Navigation/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Navigation-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Navigation-p0-c1.png | .captures/diff/showcase/Classic/Navigation/p0.png | |
| Classic/Overlays/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Overlays-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Overlays-p0-c1.png | .captures/diff/showcase/Classic/Overlays/p0.png | |
| Classic/Pickers/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Pickers-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Pickers-p0-c1.png | .captures/diff/showcase/Classic/Pickers/p0.png | |
| Classic/Primitives/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Primitives-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Primitives-p0-c1.png | .captures/diff/showcase/Classic/Primitives/p0.png | |
| Classic/Selection/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Selection-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Selection-p0-c1.png | .captures/diff/showcase/Classic/Selection/p0.png | |
| Classic/Selection/p1 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Selection-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Selection-p1-c1.png | .captures/diff/showcase/Classic/Selection/p1.png | |
| Classic/ThemeSwitcher/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/ThemeSwitcher-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/ThemeSwitcher-p0-c1.png | .captures/diff/showcase/Classic/ThemeSwitcher/p0.png | |
| Classic/Verification/p0 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Verification-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Verification-p0-c1.png | .captures/diff/showcase/Classic/Verification/p0.png | |
| Classic/Verification/p1 | 4 | 0.0004 | 51 | stable | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Verification-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Verification-p1-c1.png | .captures/diff/showcase/Classic/Verification/p1.png | |

## Group 2 — same glyph shift, one key not reproduced byte-identical (1 key, varies-run-to-run)

**Cause:** `AeroDark/List/p1` shows the exact same window-chrome glyph pattern as Group 1
(`diffPx=4`, `maxΔ=54`, same rect) — but this single key did not classify `stable` in the D-05
re-sweep (`noise-new.json`): at least one of the 6 new-toolchain captures at this key was not
byte-identical to the others. `21-COMPARE.md` (Category B) treats this as the same glyph
phenomenon with an extra one-frame anti-aliasing jitter, not a distinct cause — flagged separately
here only because its run-to-run verdict differs from Group 1's, per D-05 ("nothing is filtered or
dropped").

**Proposed action:** add region to noise list. Since this key's own diff is shown to vary
run-to-run on the new toolchain, `21-noise-regions.json`'s `showcase` entry for `AeroDark/List/p1`
should gain the `(1110,6,12,12)` rect already carried by every other window-chrome key in Group 1,
so future sweeps do not re-flag it.

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroDark/List/p1 | 4 | 0.0004 | 54 | varies-run-to-run | (1110,6,12,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/List-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/List-p1-c1.png | .captures/diff/showcase/AeroDark/List/p1.png | |

## Group 3 — popup border/shadow-edge anti-aliasing shift (6 keys, stable)

**Cause:** Same Skia rendering-pipeline shift as Group 1, applied to `dropShadow`/border strokes
around an opened popup instead of text glyphs — `AeroContextMenu` and `AeroMenuBar`, all three
themes each. `maxChannelDelta` of 1-2; a few hundred touched pixels around the popup outline. All
`stable` in the D-05 re-sweep.

**Proposed action:** accept as upstream rendering change (same family as Group 1, different
stroke type).

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroContextMenu/AeroBlue/opened | 405 | 0.0515 | 2 | stable | (94,46,20,28); (318,46,20,44); (94,110,20,28); (150,118,188,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroContextMenu/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroContextMenu/AeroBlue/opened.png | .captures/diff/ui-tests/AeroContextMenu/AeroBlue/opened.png | |
| AeroContextMenu/AeroDark/opened | 94 | 0.012 | 2 | stable | (94,54,20,20); (318,54,20,12); (94,118,20,20); (318,118,20,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroContextMenu/AeroDark/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroContextMenu/AeroDark/opened.png | .captures/diff/ui-tests/AeroContextMenu/AeroDark/opened.png | |
| AeroContextMenu/Classic/opened | 166 | 0.0211 | 2 | stable | (94,54,28,20); (318,54,20,12); (94,78,28,60); (318,118,20,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroContextMenu/Classic/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroContextMenu/Classic/opened.png | .captures/diff/ui-tests/AeroContextMenu/Classic/opened.png | |
| AeroMenuBar/AeroBlue/opened | 150 | 0.0191 | 2 | stable | (0,22,10,20); (38,22,20,12); (110,22,20,20); (0,86,10,20); (110,86,20,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroMenuBar/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroMenuBar/AeroBlue/opened.png | .captures/diff/ui-tests/AeroMenuBar/AeroBlue/opened.png | |
| AeroMenuBar/AeroDark/opened | 82 | 0.0104 | 1 | stable | (0,22,10,28); (38,22,20,12); (110,22,20,20); (0,86,10,20); (110,86,20,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroMenuBar/AeroDark/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroMenuBar/AeroDark/opened.png | .captures/diff/ui-tests/AeroMenuBar/AeroDark/opened.png | |
| AeroMenuBar/Classic/opened | 164 | 0.0209 | 2 | stable | (0,22,10,20); (110,22,20,20); (0,86,10,20); (30,86,100,20) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroMenuBar/Classic/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroMenuBar/Classic/opened.png | .captures/diff/ui-tests/AeroMenuBar/Classic/opened.png | |

## Group 4 — single-pixel caret colour shift in `AeroComboBox` (3 keys, stable)

**Cause:** Exactly 1 touched pixel per theme at a fixed position inside the opened search field's
text cursor (caret), with a comparatively large `maxChannelDelta` (76-138) because a single-pixel
colour/alpha change registers as a big per-channel delta even though it touches almost nothing.
Confirmed visually via the contact sheet (`21-COMPARE.md`): a barely-visible highlight at the
caret's screen position — consistent with the same rendering-pipeline shift changing a small
colour/alpha value in the caret or its immediate surrounding pixel, not a layout or functional
change. All `stable` in the D-05 re-sweep.

**Proposed action:** accept as upstream rendering change.

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroComboBox/AeroBlue/opened | 1 | 0.0001 | 92 | stable | (22,30,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroComboBox/AeroBlue/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroComboBox/AeroBlue/opened.png | .captures/diff/ui-tests/AeroComboBox/AeroBlue/opened.png | |
| AeroComboBox/AeroDark/opened | 1 | 0.0001 | 138 | stable | (22,30,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroComboBox/AeroDark/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroComboBox/AeroDark/opened.png | .captures/diff/ui-tests/AeroComboBox/AeroDark/opened.png | |
| AeroComboBox/Classic/opened | 1 | 0.0001 | 76 | stable | (22,30,12,12) | .captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/AeroComboBox/Classic/opened.png | .captures/new-kt2.4.20-cmp1.12.0/ui-tests/runA/AeroComboBox/Classic/opened.png | .captures/diff/ui-tests/AeroComboBox/Classic/opened.png | |

## Group 5 — `AeroProgressBar` shimmer/sheen animation, wider than the baseline rect (3 keys, varies-run-to-run)

**Cause:** Already the named noise source in `21-noise-regions.json` for `*/Range/p0`
(`AeroProgressBar (ind)` indeterminate bar via `rememberInfiniteTransition`, and the `(det, sheen)`
row's matching shimmer treatment) — wall-clock-animated, so the before/after diff is the animation
sampled at a different phase, not a toolchain content change. Classified `outsideNoise` here only
because the new-toolchain diff extends beyond the tightly-fitted pre-upgrade baseline rect
(1078-2080 px per key vs. the baseline's narrower rects). `21-COMPARE.md` confirms this is the
exact already-named source, not a new one.

**Proposed action:** add region to noise list — widen `21-noise-regions.json`'s `showcase`
entries for `AeroBlue/Range/p0`, `AeroDark/Range/p0`, `Classic/Range/p0` to cover the rects listed
below (they already vary run-to-run on the new toolchain, satisfying D-05's condition for adding a
noise region).

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroBlue/Range/p0 | 830 | 0.0865 | 164 | varies-run-to-run | (1110,6,12,12); (278,454,44,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Range-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Range-p0-c1.png | .captures/diff/showcase/AeroBlue/Range/p0.png | |
| AeroDark/Range/p0 | 736 | 0.0767 | 190 | varies-run-to-run | (1110,6,12,12); (246,414,52,20); (382,454,36,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Range-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Range-p0-c1.png | .captures/diff/showcase/AeroDark/Range/p0.png | |
| Classic/Range/p0 | 1214 | 0.1265 | 125 | varies-run-to-run | (1110,6,12,12); (230,414,68,20); (302,454,116,20) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Range-p0-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Range-p0-c1.png | .captures/diff/showcase/Classic/Range/p0.png | |

## Group 6 — recompose-drive counter plus nearby glyph deltas, wider than the baseline rect (5 keys, varies-run-to-run)

**Cause:** Already the named noise source for `*/Layout/p3`/`*/Layout/p4` (the live-ticking
`recomposeDriveCounter` text, `LaunchedEffect` `delay(32L)`, part of the RCMP-04 regression demo) —
wall-clock-animated. In the 8-9-region cases the diff also picks up a handful of small
header/action-icon glyphs (same family as Group 1's Category C) that sit just outside the
tightly-fitted baseline noise rectangle. No new content or layout difference; `21-COMPARE.md`
confirms the same already-known source, just a slightly wider footprint on the new toolchain.

**Proposed action:** add region to noise list — widen `21-noise-regions.json`'s `showcase`
entries for these 5 keys to cover the rects listed below (all vary run-to-run on the new
toolchain, satisfying D-05).

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| AeroBlue/Layout/p3 | 96 | 0.01 | 176 | varies-run-to-run | (1110,6,12,12); (438,222,12,12); (62,238,12,12); (798,238,12,12); (334,558,12,12); (342,590,12,12); (886,598,20,20); (54,614,20,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p3-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Layout-p3-c1.png | .captures/diff/showcase/AeroBlue/Layout/p3.png | |
| AeroBlue/Layout/p4 | 99 | 0.0103 | 176 | varies-run-to-run | (1110,6,12,12); (334,310,12,12); (342,350,12,12); (886,358,20,20); (54,366,20,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p4-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroBlue/Layout-p4-c1.png | .captures/diff/showcase/AeroBlue/Layout/p4.png | |
| AeroDark/Layout/p3 | 115 | 0.012 | 160 | varies-run-to-run | (1110,6,12,12); (438,222,12,12); (62,238,12,12); (798,238,12,12); (334,558,12,12); (342,590,12,12); (886,598,20,20); (54,614,20,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p3-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Layout-p3-c1.png | .captures/diff/showcase/AeroDark/Layout/p3.png | |
| AeroDark/Layout/p4 | 97 | 0.0101 | 160 | varies-run-to-run | (1110,6,12,12); (334,310,12,12); (342,350,12,12); (886,358,20,20); (54,366,20,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p4-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/AeroDark/Layout-p4-c1.png | .captures/diff/showcase/AeroDark/Layout/p4.png | |
| Classic/Layout/p4 | 84 | 0.0088 | 174 | varies-run-to-run | (1110,6,12,12); (334,310,12,12); (342,350,12,12); (886,358,20,20); (54,366,20,12) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p4-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p4-c1.png | .captures/diff/showcase/Classic/Layout/p4.png | |

## Group 7 — one-off scroll-settle capture flake (1 key, varies-run-to-run)

**Cause:** `Classic/Layout/p3`'s "after" frame (`runA6`) shows content from roughly page-2's
scroll position (`AeroSidebar` + `AeroStepperWizard`) instead of the correct page-3 content (two
`AeroPanelGroup` demos), despite the capture manifest reporting the identical
`scrollPx=2304`/`contentPx=3316` target on both sides — the scroll target was correct but the
frame was captured before Compose's scroll had visually settled. **Disproved as a stable
toolchain regression:** the independent D-05 re-capture of the same key (`runB-4/Classic/
Layout-p3-c1.png`) shows the correct page-3 content, matching the baseline; `noise-new.json`
confirms `stable: false` for this key. This is a capture-pipeline timing gap
(`SettleMs=1500` occasionally insufficient for a 5-page, 3316px-tall section under load), not a
rendering regression in library or showcase code.

**Proposed action:** fix in code — but in the capture tooling
(`tools/capture/AeroCapture.ps1`/`Invoke-ShowcaseSweep.ps1`'s `SettleMs`/scroll-verification
logic), not `library/src` or `showcase/src`. Raise `SettleMs` for tall, multi-page sections or
verify the captured frame's on-screen content matches the target `scrollPx` before saving.

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| Classic/Layout/p3 | 398566 | 41.5173 | 225 | varies-run-to-run | (1110,6,12,12); (38,30,1124,770); (782,30,52,228) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p3-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Layout-p3-c1.png | .captures/diff/showcase/Classic/Layout/p3.png | |

## Group 8 — real-cursor hover interference during capture (1 key, varies-run-to-run)

**Cause:** `Classic/Data/p1`'s "after" frame shows the `AeroDataTable`'s last visible row
(`SAT-009`) rendered with a hover highlight absent in "before." This key was **not** among
`runA6`'s 10 `externalActivity: true` frames, because the capture guard (fixed in `9834e8c`) only
detects the captured window's own process taking the OS foreground — it does not detect the
maintainer's real cursor merely moving over the (non-focused, non-activated) capture window's
screen region, which is still enough for Compose to register a genuine pointer-hover state on
whatever row sits under the cursor. This is capture interference from real desktop use during the
sweep, not a toolchain rendering change — `noise-new.json` confirms `stable: false`.

**Proposed action:** fix in code — but in the capture tooling
(`tools/capture/AeroCapture.ps1`'s capture guard), not `library/src` or `showcase/src`. Extend the
guard to detect hover-only interference (e.g. sample `GetCursorPos` against the capture window's
screen rect, not just `GetForegroundWindow`) and retry the capture when it fires.

| Key | diffPx | % | maxΔ | Run-to-run | Outside-noise rects (x,y,w,h) | Before | After | Highlight | Ruling |
|---|---|---|---|---|---|---|---|---|---|
| Classic/Data/p1 | 37941 | 3.9522 | 51 | varies-run-to-run | (1110,6,12,12); (46,390,1100,52) | .captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Data-p1-c1.png | .captures/new-kt2.4.20-cmp1.12.0/showcase/runA6/Classic/Data-p1-c1.png | .captures/diff/showcase/Classic/Data/p1.png | |

---

## D-08 after-only inspections (no before/after comparison possible)

`AeroDialog` and `AeroAlertDialog` were opened without a baseline (Plan 11 Task 1,
`21-UITEST-COVERAGE.md`'s "After-only MCP inspection" section) — there is no "before" frame to
diff against, so these are not comparison items and are not counted in the 102 above. They are
carried on the VER-09 unconfirmed list (`21-UNCONFIRMED.md`) instead, together with the
foreground-takeover finding from the two Classic openings (a harness/interaction fact, not a
rendering drift).
