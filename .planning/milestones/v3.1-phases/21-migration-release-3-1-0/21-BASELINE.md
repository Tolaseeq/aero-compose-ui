# Phase 21: Pre-Upgrade Showcase Baseline (BASE-04)

Reference frames of all 17 showcase sections x AeroBlue / AeroDark / Classic (every page), taken
on the toolchain below by the BASE-01/BASE-02 launch-parameter + `PrintWindow` capture pipeline
(Plan 01), before any Phase 21 dependency bump. Images are NOT committed (D-01) — every path below
is `.captures/`-relative, resolving under `C:\1A_WORK\ui_lib\.captures\` (gitignored).

## Toolchain at capture time

| Property | Value |
|----------|-------|
| Kotlin | `2.4.10` |
| Compose Multiplatform | `1.11.1` |
| Gradle | `8.14.3` |
| JVM (ran the showcase) | `21.0.9` / Microsoft |
| kotlinx-coroutines | `1.10.2` |
| kotlinx-datetime | `0.6.2` |
| JUnit | `5.10.0` |
| Windows | `Microsoft Windows NT 10.0.26100.0` (Windows 11) |
| DPI scale | **100% (96 DPI)** — read from `GetDpiForWindow`, confirmed identical on every one of the 150 captures across both runs |
| Git commit | `096155e7aa2bc9b062e2d0bd78574cd7e7a2b38e` |
| Capture date | 2026-09-23 |

`gradle/libs.versions.toml` / `gradle/wrapper/gradle-wrapper.properties` precondition confirmed
before capture: `kotlin = "2.4.10"`, `composeMultiplatform = "1.11.1"`, Gradle wrapper
`8.14.3-bin.zip`; `git status --porcelain` was clean of any unrelated change at capture time.

## Capture method

`tools/capture/Invoke-ShowcaseSweep.ps1` (Plan 01, BASE-01/BASE-02): for each
`(Theme, Section, Page)` combination, launches `:showcase:run -Paero.scheme=<Theme>
-Paero.section=<Section> -Paero.page=<Page> -Paero.capture=true` (a non-focusable window starting
behind other windows, zero synthetic input), waits for the single `AERO_READY` stdout line, then
captures 3 frames 700ms apart with `PrintWindow(hwnd, hdc, PW_RENDERFULLCONTENT)` via
`tools/capture/AeroCapture.ps1`'s `Invoke-AeroWindowCapture` — never `CopyFromScreen` or the MCP
`take_screenshot` scrape. Every capture is sanity-checked against the app-reported background
colour and proven not to have moved the cursor or changed the foreground window (retried up to 3x
on interference; the sweep aborts with `NON-INTERFERENCE VIOLATION` / `LAUNCH ACTIVATED WINDOW`
otherwise — neither occurred in either run, see `orphansKilled: []` and 0 error-log matches for
both terms in `runA`/`runB`). Each launch's own JVM is terminated by PID before the next one
starts (never two Gradle launches at once); an orphan sweep runs after each full pass.

## Two independent full runs

| Run | Frames | Started | Finished | Toolchain match | Orphans killed | Interference violations |
|---|---|---|---|---|---|---|
| runA | 75 | 2026-09-23T17:04:15+03:00 | 2026-09-23T17:12:48+03:00 | yes | 0 | 0 |
| runB | 75 | 2026-09-23T17:13:09+03:00 | 2026-09-23T17:21:33+03:00 | yes | 0 | 0 |

Both manifests (`.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/manifest.json`,
`.../runB/manifest.json`) report identical `toolchain` blocks (Kotlin 2.4.10, Compose Multiplatform
1.11.1, Gradle 8.14.3, coroutines 1.10.2, datetime 0.6.2, JUnit 5.10.0, JVM 21.0.9/Microsoft),
identical DPI (96) for every frame, and the exact same 75 frame keys (`diff` of the sorted key
lists is empty). `runA` is the noise-measurement reference and the frame index below; `runB` is the
second independent capture used only for the run-to-run noise comparison (`21-NOISE.md`).

## Frame index (17 sections x 3 themes, every page)

All 75 frames are 1200x800px @ 96 DPI. `Pages` lists every page index captured for that
`(Theme, Section)`; `Reference` is page 0's first capture (`c1`) of `runA`. Every page of a
multi-page section has its own file at the same path pattern with `-p<N>-` in place of `-p0-`.

| Theme | Section | Pages | Reference file (.captures/-relative) |
|---|---|---|---|
| AeroBlue | ThemeSwitcher | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/ThemeSwitcher-p0-c1.png` |
| AeroBlue | Verification | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Verification-p0-c1.png` |
| AeroBlue | Foundation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Foundation-p0-c1.png` |
| AeroBlue | Primitives | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Primitives-p0-c1.png` |
| AeroBlue | Icons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Icons-p0-c1.png` |
| AeroBlue | Buttons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Buttons-p0-c1.png` |
| AeroBlue | Input | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Input-p0-c1.png` |
| AeroBlue | Selection | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Selection-p0-c1.png` |
| AeroBlue | Dropdown | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Dropdown-p0-c1.png` |
| AeroBlue | Range | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Range-p0-c1.png` |
| AeroBlue | List | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/List-p0-c1.png` |
| AeroBlue | Containers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Containers-p0-c1.png` |
| AeroBlue | Overlays | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Overlays-p0-c1.png` |
| AeroBlue | Navigation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Navigation-p0-c1.png` |
| AeroBlue | Data | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Data-p0-c1.png` |
| AeroBlue | Pickers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Pickers-p0-c1.png` |
| AeroBlue | Layout | 0,1,2,3,4 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroBlue/Layout-p0-c1.png` |
| AeroDark | ThemeSwitcher | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/ThemeSwitcher-p0-c1.png` |
| AeroDark | Verification | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Verification-p0-c1.png` |
| AeroDark | Foundation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Foundation-p0-c1.png` |
| AeroDark | Primitives | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Primitives-p0-c1.png` |
| AeroDark | Icons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Icons-p0-c1.png` |
| AeroDark | Buttons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Buttons-p0-c1.png` |
| AeroDark | Input | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Input-p0-c1.png` |
| AeroDark | Selection | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Selection-p0-c1.png` |
| AeroDark | Dropdown | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Dropdown-p0-c1.png` |
| AeroDark | Range | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Range-p0-c1.png` |
| AeroDark | List | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/List-p0-c1.png` |
| AeroDark | Containers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Containers-p0-c1.png` |
| AeroDark | Overlays | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Overlays-p0-c1.png` |
| AeroDark | Navigation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Navigation-p0-c1.png` |
| AeroDark | Data | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Data-p0-c1.png` |
| AeroDark | Pickers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Pickers-p0-c1.png` |
| AeroDark | Layout | 0,1,2,3,4 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/AeroDark/Layout-p0-c1.png` |
| Classic | ThemeSwitcher | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/ThemeSwitcher-p0-c1.png` |
| Classic | Verification | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Verification-p0-c1.png` |
| Classic | Foundation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Foundation-p0-c1.png` |
| Classic | Primitives | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Primitives-p0-c1.png` |
| Classic | Icons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Icons-p0-c1.png` |
| Classic | Buttons | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Buttons-p0-c1.png` |
| Classic | Input | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Input-p0-c1.png` |
| Classic | Selection | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Selection-p0-c1.png` |
| Classic | Dropdown | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Dropdown-p0-c1.png` |
| Classic | Range | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Range-p0-c1.png` |
| Classic | List | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/List-p0-c1.png` |
| Classic | Containers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Containers-p0-c1.png` |
| Classic | Overlays | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Overlays-p0-c1.png` |
| Classic | Navigation | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Navigation-p0-c1.png` |
| Classic | Data | 0,1 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Data-p0-c1.png` |
| Classic | Pickers | 0 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Pickers-p0-c1.png` |
| Classic | Layout | 0,1,2,3,4 | `.captures/old-kt2.4.10-cmp1.11.1/showcase/runA/Classic/Layout-p0-c1.png` |

17 section names covered above (per theme): ThemeSwitcher, Verification, Foundation, Primitives,
Icons, Buttons, Input, Selection, Dropdown, Range, List, Containers, Overlays, Navigation, Data,
Pickers, Layout.

## Run-to-run noise

See `21-NOISE.md` (BASE-03): 9 of 75 frame keys carry a small, named, time-dependent noise region
(`AeroProgressBar (ind)` indeterminate shimmer on the Range section; a live 30fps recompose-drive
counter on 2 Layout pages), all under 0.5% of frame area; the remaining 66 keys are pixel-stable
across all 6 captures (3 per run x 2 runs).

## UI-test reference images (BASE-05, D-07)

Two opt-in Compose UI-test runs (`./gradlew :library:test --rerun --tests
"com.mordred.aero.capture.*" -Paero.captureDir=.captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA|runB`)
on the same toolchain as the showcase baseline above, both `BUILD SUCCESSFUL` (full suite: **541
tests across 93 classes**). `UiCapture.write()` only writes into `.captures/` when
`-Paero.captureDir` is passed (D-03) — every capture lands at
`.captures/old-kt2.4.10-cmp1.11.1/ui-tests/<run>/<Component>/<Theme>/<state>.png`.

**D-03 recheck (after this plan's UI-test runs):** `.captures` file count was 1152 both before and
after a plain `./gradlew :library:test --rerun` (no `-Paero.captureDir`) — BUILD SUCCESSFUL, same
541/93 test count, `git status --porcelain` unaffected by that run.

### Test classes

- `Base05CaptureProofTest.kt` — `captureToImage()` proof-of-work (`Base05Proof/` folder; not a
  library component, kept for the mechanism proof).
- `Base05GlassStateCaptureTest.kt` — 10 BASE-05 components x 3 themes, hover/press/keyboard-focus
  (+drag for the two sliders) states.
- `Base05DragCaptureTest.kt` — `AeroSplitPane`/`AeroPanelGroup`/`AeroDataTable` column-resize drag
  states x 3 themes.
- `D07MenuPopupCaptureTest.kt` / `D07PickerPopupCaptureTest.kt` — all 13 `Popup(`-based components
  x 3 themes, closed vs. opened state (D-07), fixed picker values (D-09).

### Component x theme x state index

Full per-state table (which states, which assertions, focus method, D-08 status per component) is
`.planning/phases/21-migration-release-3-1-0/21-UITEST-COVERAGE.md` — not re-transcribed here.
23 library components have a captured folder plus `Base05Proof/` (proof-of-work, not a component):

| Group | Components | Folder count |
|---|---|---|
| BASE-05 glass/drag state components | `AeroButton`, `AeroOutlinedButton`, `AeroSwitch`, `AeroSegmentedControl`, `AeroSlider`, `AeroRangeSlider`, `AeroListItem`, `AeroSplitPane`, `AeroPanelGroup`, `AeroDataTable` | 10 |
| D-07 popup-opened components | `AeroDropdown`, `AeroComboBox`, `AeroContextMenu`, `AeroMenuBar`, `AeroTooltip`, `AeroPopover`, `AeroDrawer`, `AeroColorPickerButton`, `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`, `AeroDateTimeRangePicker`, `AeroTimePicker` | 13 |

Every folder above exists under both `.captures/old-kt2.4.10-cmp1.11.1/ui-tests/runA/<Component>/`
and `.../runB/<Component>/`; each run produced 200 PNGs (24 folders including `Base05Proof/`).

### Popup capture method

`captureOpened()` (`UiCapture.kt`): `onRoot().captureToImage()` for the closed state; the instant a
`Popup` is composed, Compose Multiplatform Desktop reports a second semantics root and `onRoot()`
throws `AssertionError`, caught and handled by a fallback to `onAllNodes(isRoot())`'s last-added
root, which rasterizes the same shared window canvas and reaches the popup pixels too (empirically
proven by `D07MenuPopupCaptureTest`'s `AeroDropdown` probe — see `21-UITEST-COVERAGE.md`).

### D-08 status

**No component needed the D-08 after-only fallback** — all 13 `Popup(`-based components were
captured with a real before/after pair using `captureOpened()`. Three components are outside UI-test
capture entirely and are separately classified (not D-08, D-08 is specifically "cannot capture inside
`runComposeUiTest`" — these three cannot be composed in a unit test at all):

| Component | Reason | Disposition |
|---|---|---|
| `AeroDialog` | Built on a real `Window` — `captureToImage()` cannot reach a separate OS window | after-only via MCP click + `PrintWindow` (a later plan), guarded by a foreground-window check |
| `AeroAlertDialog` | Wraps `AeroDialog` — same `Window`-based reason | same as `AeroDialog` |
| `AeroFilePicker` | Opens the native OS `java.awt.FileDialog`, not this library's own rendering | VER-09 unconfirmed list; never clicked in any test |
