# Phase 21 BASE-05/D-07 UI-Test Coverage

Per-component x theme record of which states are captured by the permanent UI-test suite added in
Plan 02 (`library/src/test/kotlin/com/mordred/aero/capture/`), which per-state assertions are kept
as a regression gate, and how keyboard focus was reached. Captured on the OLD toolchain (Kotlin
2.4.10 / Compose Multiplatform 1.11.1), before the Phase 21 dependency bumps — the baseline every
later plan's post-upgrade sweep compares against (D-04/D-05).

All captures land under `.captures/<Component>/<Theme>/<state>.png` only when `-Paero.captureDir`
is passed (D-03); an ordinary `./gradlew test` run writes nothing.

## Task 2 — Glass components (`Base05GlassStateCaptureTest.kt`)

| Component | Theme | States captured | Assertions kept | Focus method | Notes |
|---|---|---|---|---|---|
| AeroButton | AeroBlue | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroButton | AeroDark | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroButton | Classic | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroOutlinedButton | AeroBlue | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroOutlinedButton | AeroDark | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroOutlinedButton | Classic | default, focus, hover, press | focus, hover, press | tab-traversal | |
| AeroSwitch | AeroBlue | default, focus, hover, press, default-on | focus, hover, press, default-on | tab-traversal | `default-on` reached via a real `performClick()`, not a hand-set boolean |
| AeroSwitch | AeroDark | default, focus, hover, press, default-on | focus, hover, press, default-on | tab-traversal | same as above |
| AeroSwitch | Classic | default, focus, hover, press, default-on | focus, hover, press, default-on | tab-traversal | same as above |
| AeroSegmentedControl | AeroBlue | default, focus, hover, press (segment "B") | focus, hover, press | requestFocus() on segment "B" | outer Row (the `target`-tagged node) is only hoverable, not a focus stop — each segment's own `selectable()` is; Tab traversal did not visibly change the capture, so the requestFocus() fallback on segment "B" was used |
| AeroSegmentedControl | AeroDark | default, focus, hover, press (segment "B") | focus, hover, press | requestFocus() on segment "B" | same as above |
| AeroSegmentedControl | Classic | default, focus, hover, press (segment "B") | focus, hover, press | requestFocus() on segment "B" | same as above |
| AeroSlider | AeroBlue | default, focus, hover, press, drag | focus, hover, press, drag | tab-traversal | thumb screen position computed as `left + thumbRadius + fraction*(width - 2*thumbRadius)` (M3 Slider insets the track by half the thumb's measured width on each side); no explicit-node fallback exists (AeroSlider's own `modifier` lands on the outer Box, not the M3 Slider's internal focusable node) but Tab traversal reached it directly |
| AeroSlider | AeroDark | default, focus, hover, press, drag | focus, hover, press, drag | tab-traversal | same as above |
| AeroSlider | Classic | default, focus, hover, press, drag | focus, hover, press, drag | tab-traversal | same as above |
| AeroRangeSlider | AeroBlue | default, hover, press, drag | hover, press, drag | n/a: no keyboard focus in AeroRangeSlider (pre-existing v3.0 debt) | thumb screen position computed as a plain fraction of the full track width (`valueToX`'s own formula — no thumb-radius inset, unlike AeroSlider's M3-hosted track); hover/press/drag act on the start thumb (value 0.2) |
| AeroRangeSlider | AeroDark | default, hover, press, drag | hover, press, drag | n/a: no keyboard focus in AeroRangeSlider (pre-existing v3.0 debt) | same as above |
| AeroRangeSlider | Classic | default, hover, press, drag | hover, press, drag | n/a: no keyboard focus in AeroRangeSlider (pre-existing v3.0 debt) | same as above |
| AeroListItem | AeroBlue | default, focus, hover, press, default-selected | focus, hover, press, default-selected | tab-traversal | `default-selected` reached by flipping a hoisted `MutableState<Boolean>` (selection in AeroListItem is caller-controlled, not toggled by clicking) |
| AeroListItem | AeroDark | default, focus, hover, press, default-selected | focus, hover, press, default-selected | tab-traversal | same as above |
| AeroListItem | Classic | default, focus, hover, press, default-selected | focus, hover, press, default-selected | tab-traversal | same as above |

All 21 combinations above produced a non-vacuous visual difference for every kept assertion on the
old toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1) — no state needed to be dropped as
"no visual change on old toolchain."

## Task 3 — Drag components (`Base05DragCaptureTest.kt`)

| Component | Theme | States captured | Assertions kept |
|---|---|---|---|
| AeroSplitPane | AeroBlue | default, dragging, dropped | `onSplitChange` reports a fraction != initial 0.5f; dropped differs from default |
| AeroSplitPane | AeroDark | default, dragging, dropped | same as above |
| AeroSplitPane | Classic | default, dragging, dropped | same as above |
| AeroPanelGroup | AeroBlue | default, dragging, dropped | `onLayoutChange` fires at drag-end with sizes no longer all equal; dropped differs from default; each of LeftPane/CenterPane/RightPane header still occurs exactly once (RCMP invariant) |
| AeroPanelGroup | AeroDark | default, dragging, dropped | same as above |
| AeroPanelGroup | Classic | default, dragging, dropped | same as above |
| AeroDataTable | AeroBlue | default, dragging, dropped | Col2's left edge moves right after widening Col1 by drag; dropped differs from default |
| AeroDataTable | AeroDark | default, dragging, dropped | same as above |
| AeroDataTable | Classic | default, dragging, dropped | same as above |

All 9 combinations produced a non-vacuous visual difference and the structural assertions above
held on the old toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1) — no state needed to be
dropped as "no visual change on old toolchain."

Full suite (`./gradlew :library:test --rerun`) is green with these 3 new test classes included:
502 tests across 91 classes (up from 467 before this plan). The exact count is not locked here —
TOOL-16's count guard is a later plan (Plan 05).

## Popup capture method (D-07 Task 1 probe, empirical)

Decided by `D07MenuPopupCaptureTest`'s AeroDropdown probe (`aeroDropdown<Theme>Opened`, 3 tests) on
the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1), against a 480x360dp host:

- **Closed state (no `Popup` composed): M1 — `onRoot().captureToImage()`.** With `AeroDropdown`
  collapsed there is exactly one semantics root (`onAllNodes(isRoot())` finds `1` node,
  `1024x768px`, confirmed by an instrumented run of this probe), so `onRoot()` resolves directly.
- **Opened state (a `Popup` is composed): M1 throws, falls back to M2 —
  `onAllNodes(isRoot())`, last-added root.** The instant `AeroDropdown`'s `Popup` opens, Compose
  Multiplatform Desktop's `SkikoComposeUiTest` reports a SECOND semantics root — both `1024x768px`,
  the same full window bounds — and `onRoot()` throws `AssertionError: Expected exactly '1' node
  but found '2' nodes that satisfy: (isRoot)` rather than resolving to either one. Falling back to
  `onAllNodes(isRoot())[count - 1]` (the most-recently-composed root — the `Popup`'s own owner) and
  calling `captureToImage()` on it succeeds and rasterizes the SAME full-window Skia surface the
  main content paints onto (desktop `Popup`s are layered onto one shared canvas, not a separate
  off-screen bitmap for their own root) — so the resulting image contains the popup pixels too.
- **Evidence:** all 3 `aeroDropdown<Theme>Opened` tests pass; `pixelMapsDiffer(closed, opened)` is
  `true` in every theme, proving the M2-fallback capture actually differs from (and therefore
  contains) the popup content, not a byte-identical re-capture of the closed window.
- **Shared helper:** `captureOpened()` implements exactly this M1-then-M2-fallback sequence, plus
  `assertOpenedDiffers()` (write both images + assert they differ). Both were moved into
  `UiCapture.kt` in Task 3 once `D07PickerPopupCaptureTest` needed them too, and are reused,
  unmodified, by every D-07 test in both files — no per-component capture-method variation was
  needed anywhere.

D-09 check: `grep -cE "Clock|todayLocalDate|now\(" AeroCalendarGrid.kt` = `0` — AeroCalendarGrid has
no today-highlight, confirming a fixed `value` is sufficient for deterministic picker frames
(D-09 confirmed).

## Task 2 — Menu/overlay popup components (`D07MenuPopupCaptureTest.kt`)

All open via real UI-test input only (`performClick`/`performMouseInput`/`performKeyInput` —
never `java.awt.Robot` or OS input). Capture method for every row below is `captureOpened()`
(M1 `onRoot()` for the closed capture, automatic M2 `onAllNodes(isRoot())` fallback for the
opened capture — see "Popup capture method" above). All 24 tests (3 AeroDropdown from Task 1 +
21 here) pass on the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1); every row
produced a genuine `pixelMapsDiffer(closed, opened) == true` result — no component needed the
D-08 fallback.

| Component | Theme | Open input | Capture method | Image assertion kept | D-08 status |
|---|---|---|---|---|---|
| AeroDropdown | AeroBlue / AeroDark / Classic | `performClick()` on trigger | captureOpened() (M1→M2) | yes | not needed — M2 fallback captures the popup |
| AeroComboBox | AeroBlue / AeroDark / Classic | `performClick()` focuses the field (empty text already matches every option per `shouldAutoOpen`) | captureOpened() (M1→M2) | yes | not needed |
| AeroContextMenu | AeroBlue / AeroDark / Classic | `performMouseInput { rightClick(center) }` | captureOpened() (M1→M2) | yes | not needed |
| AeroMenuBar | AeroBlue / AeroDark / Classic | `performClick()` on the "File" top-level label | captureOpened() (M1→M2) | yes | not needed |
| AeroTooltip | AeroBlue / AeroDark / Classic | `performMouseInput { moveTo(center) }` + `mainClock.advanceTimeBy(700)` (past the 600ms show delay) | captureOpened() (M1→M2) | yes | not needed |
| AeroPopover | AeroBlue / AeroDark / Classic | `performClick()` on an anchor-side "Open" button | captureOpened() (M1→M2) | yes | not needed |
| AeroDrawer | AeroBlue / AeroDark / Classic | `performClick()` on "Open drawer" + `mainClock.advanceTimeBy(400)` (past the 220ms slide) | captureOpened() (M1→M2) | yes | not needed |
| AeroColorPickerButton | AeroBlue / AeroDark / Classic | `performClick()` on the 32dp swatch trigger; popup-only "Original" label asserted | captureOpened() (M1→M2) | yes | not needed |

## Task 3 — Picker popup components (`D07PickerPopupCaptureTest.kt`)

All 5 pickers open via `performClick()` on their `AeroIconButton` trigger, located via
`onNodeWithContentDescription(...)` (the merged-semantics content description of the Icon inside
the clickable `AeroIconButton`). Every test passes a fixed, non-null `value`/`startValue`+`endValue`
(D-09) using ONLY positional `LocalDate`/`LocalDateTime`/`LocalTime` constructors — never named
parameters, never `.dayOfMonth`/`.monthNumber`/`Clock` — so this file compiles unchanged through the
kotlinx-datetime 0.6.2 -> 0.8.0 bump. Capture method is `captureOpened()` (same M1/M2 as Task 1/2).
All 15 tests pass on the OLD toolchain (Kotlin 2.4.10 / Compose Multiplatform 1.11.1); every row
produced a genuine `pixelMapsDiffer(closed, opened) == true` result — no picker needed the D-08
fallback. Full suite (`./gradlew :library:test --rerun`) is green: **541 tests across 93 classes**
(up from 502 across 91 classes before this plan — the 39 new D-07 tests, 24 in Task 1+2 and 15 here,
account for the difference).

| Component | Theme | Fixed value (D-09) | Open input | Popup-only marker asserted | Capture method | D-08 status |
|---|---|---|---|---|---|---|
| AeroDatePicker | AeroBlue / AeroDark / Classic | `LocalDate(2026, 3, 14)` | `onNodeWithContentDescription("Open calendar")` + `performClick()` | month header text "March 2026" | captureOpened() (M1→M2) | not needed |
| AeroDateRangePicker | AeroBlue / AeroDark / Classic | `LocalDate(2026, 3, 10)` .. `LocalDate(2026, 3, 20)` | `onNodeWithContentDescription("Open range calendar")` + `performClick()` | right calendar's month header "April 2026" (left month + 1) | captureOpened() (M1→M2) | not needed |
| AeroDateTimePicker | AeroBlue / AeroDark / Classic | `LocalDateTime(2026, 3, 14, 10, 30)` | `onNodeWithContentDescription("Open date & time picker")` + `performClick()` | "Apply" button | captureOpened() (M1→M2) | not needed |
| AeroDateTimeRangePicker | AeroBlue / AeroDark / Classic | `LocalDateTime(2026, 3, 10, 9, 0)` .. `LocalDateTime(2026, 3, 20, 17, 30)` | `onNodeWithContentDescription("Open date & time range picker")` + `performClick()` | "Apply" button | captureOpened() (M1→M2) | not needed |
| AeroTimePicker | AeroBlue / AeroDark / Classic | `LocalTime(10, 30)` | `onNodeWithContentDescription("Open time picker")` + `performClick()` | minute spinner's editable "30" field (the closed trigger shows only the single combined node "10:30") | captureOpened() (M1→M2) | not needed |

**Visual confirmation (opt-in smoke run, `-Paero.captureDir=.captures/smoke/plan03`):** `AeroMenuBar`
(File menu open, showing Open/Exit), `AeroDatePicker` (March 2026 grid, 14 highlighted), and
`AeroDrawer` (panel slid in over "Open drawer", showing "Drawer body") were opened with the Read
tool and visually confirm the popup content is present in the `opened.png` capture wherever this
table says the state is capturable.

### Not composable in unit tests (D-07/D-08)

| Component | Reason | Disposition |
|---|---|---|
| `AeroDialog` | Built on a real `Window` (`AeroDialog.kt:53`) — `captureToImage()` cannot reach a separate OS window, and composing it in `runComposeUiTest` would open a live window on the maintainer's desktop | after-only via MCP click + `PrintWindow` of the dialog window (a later plan), guarded by a foreground-window check (D-08) |
| `AeroAlertDialog` | Wraps `AeroDialog` — same `Window`-based reason | same as `AeroDialog` (D-08) |
| `AeroFilePicker` | Opens the native OS `java.awt.FileDialog`, not this library's own rendering | VER-09 unconfirmed list (D-08); never clicked in any test |

All 13 `Popup(`-based components (D-07's full list) now have a capture method and D-08 status
recorded above (Task 1 AeroDropdown, Task 2's 7 menu/overlay components, Task 3's 5 pickers); none
needed the D-08 fallback. The 3 `Window`/native-dialog components above are separately classified.
