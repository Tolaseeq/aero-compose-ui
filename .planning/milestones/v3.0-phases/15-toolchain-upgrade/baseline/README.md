# Pre-Migration Visual Baseline (TOOL-06)

**Purpose:** Reference screenshots of the showcase captured on the CURRENT (pre-migration)
toolchain, before the Kotlin/Compose Multiplatform version bump in plan 02. Plan 04 captures
the "after" screenshots on the new toolchain and performs the before/after diff. Skia jumps
m126 → m138 → m144 across CMP 1.7.3 → 1.11.1 (two milestone bumps), so subtle toolchain-induced
rendering drift is possible even with zero rendering-code changes — this baseline makes that
diff possible instead of relying on an eyes-on-from-memory pass.

## Toolchain at capture time

| Property | Value |
|----------|-------|
| Kotlin | `2.1.21` |
| Compose Multiplatform | `1.7.3` |
| Gradle | `8.14.3` |
| JDK | `17` |
| Capture date | 2026-07-22 |
| DPI scale | 100% (1.0x) — record actual monitor scale factor if different when capturing |

`./gradlew :showcase:compileKotlin` confirmed green on this toolchain immediately before capture
(BUILD SUCCESSFUL, 2026-07-22).

## Themes to capture

Use the in-app theme switcher (`showcase/.../sections/ThemeSwitcher.kt`, values from
`AeroColorScheme`). Showcase starts on `AeroBlue` (`Main.kt:32`).

1. `AeroBlue`
2. `AeroDark`
3. `Classic`

## Eight target components (must appear in at least one screenshot per theme)

These are the components restyled later this milestone (Phases 16-19) — capture their resting
state specifically, found in the Buttons / Range / Input / List showcase sections:

1. `AeroButton`
2. `AeroOutlinedButton`
3. `AeroSwitch`
4. `AeroSegmentedControl`
5. `AeroSlider`
6. `AeroRangeSlider`
7. `AeroProgressBar`
8. `AeroListItem`

## Capture checklist (for the human-verify step)

- [x] AeroBlue: screenshot(s) covering all 8 components saved as `baseline-AeroBlue-*.png`
- [x] AeroDark: screenshot(s) covering all 8 components saved as `baseline-AeroDark-*.png`
- [x] Classic: screenshot(s) covering all 8 components saved as `baseline-Classic-*.png`
- [x] At least 3 image files total (7 captured)

## Capture record (2026-07-22)

**Method:** windows-mcp desktop automation (CursorTouch/windows-mcp). Showcase launched via
`./gradlew :showcase:run`, maximized to the left monitor, theme changed via the in-app
`ThemeSwitcher` tabs, page scrolled through the component sections. Each frame captured as the
full showcase window region **1920×1080 @ 100% DPI** (`Graphics.CopyFromScreen`, PNG). The
screenshots are 1:1 with screen pixels — no downscaling — so plan 04's "after" pass should
reproduce the same 1920×1080 maximized framing for a clean pixel diff.

**Files and component coverage** (every one of the 8 target components appears in ≥1 shot per theme):

| Theme | File | Sections shown | Target components |
|-------|------|----------------|-------------------|
| AeroBlue | `baseline-AeroBlue-01-top.png` | Foundation, Icons, Buttons (top) | AeroButton, AeroOutlinedButton |
| AeroBlue | `baseline-AeroBlue-02.png` | Icons (end), Buttons (full), Input (start) | AeroButton, AeroOutlinedButton |
| AeroBlue | `baseline-AeroBlue-03.png` | Switch/Chip/Segmented, Dropdown, Range&Progress, Lists | AeroSwitch, AeroSegmentedControl, AeroSlider, AeroProgressBar, AeroRangeSlider, AeroListItem |
| AeroDark | `baseline-AeroDark-01-top.png` | Foundation, Icons, Buttons (top) | AeroButton, AeroOutlinedButton |
| AeroDark | `baseline-AeroDark-02-controls.png` | Switch/Segmented, Range&Progress, Lists | AeroSwitch, AeroSegmentedControl, AeroSlider, AeroProgressBar, AeroRangeSlider, AeroListItem |
| Classic | `baseline-Classic-01-top.png` | Foundation, Icons, Buttons (top) | AeroButton, AeroOutlinedButton |
| Classic | `baseline-Classic-02-controls.png` | Switch/Segmented, Range&Progress, Lists | AeroSwitch, AeroSegmentedControl, AeroSlider, AeroProgressBar, AeroRangeSlider, AeroListItem |

**Note for plan 04:** as documented in STATE.md "Baseline Findings", these eight components
currently use Material colors for their own surfaces, so they render very similarly across the
three themes at rest (theme differences are most visible in the Foundation glass cards and
background tint). The after-diff should therefore treat any per-component pixel change as a
potential Skia drift signal, not dismiss it as a theme artifact.
