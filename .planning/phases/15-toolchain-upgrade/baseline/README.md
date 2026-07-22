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

- [ ] AeroBlue: screenshot(s) covering all 8 components saved as `baseline-AeroBlue-*.png`
- [ ] AeroDark: screenshot(s) covering all 8 components saved as `baseline-AeroDark-*.png`
- [ ] Classic: screenshot(s) covering all 8 components saved as `baseline-Classic-*.png`
- [ ] At least 3 image files total (one per theme minimum; more is better for a rigorous diff)
