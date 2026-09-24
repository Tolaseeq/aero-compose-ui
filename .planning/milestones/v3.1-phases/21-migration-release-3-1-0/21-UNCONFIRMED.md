# Phase 21: Unconfirmed List (VER-09)

Everything on this list was checked by neither the live window (MCP click + `PrintWindow`) nor the
permanent UI-test suite (`runComposeUiTest` + `captureToImage()`) on the new toolchain (Kotlin
2.4.20 / Compose Multiplatform 1.12.0). None of it is presented as passed, tested or confirmed
anywhere else in Phase 21 — it is carried here instead, for the maintainer to see alongside
`21-DRIFT.md`.

## 1. Hover / focus / drag of the remaining ~40 components (VER-F01)

The permanent UI-test suite (`Base05GlassStateCaptureTest.kt`, `Base05DragCaptureTest.kt`,
`D07MenuPopupCaptureTest.kt`, `D07PickerPopupCaptureTest.kt`) exercises hover/focus/press/drag for
23 components (10 BASE-05 glass/drag components + 13 D-07 popup components, listed in
`21-UITEST-COVERAGE.md`), and the MCP after-only sweep (Plan 11 Task 1) opened `AeroDialog` and
`AeroAlertDialog` without hover. `AeroFilePicker` was never clicked (native OS dialog, see §5).
That leaves the library's other public composable components with **no hover, focus or drag
capture on either toolchain, old or new**:

`grep -rn "public fun .*Aero\w*(" library/src/main/kotlin/com/mordred/aero/components` lists 57
public top-level composable declarations (some components have more than one overload, e.g.
`AeroProgressBar`, `AeroScrollBar`); removing the 22 that ARE covered by the UI-test suite and the
3 D-08 window/native-dialog components leaves 32 distinct component names, plus `AeroTitleBar`
(a `FrameWindowScope` extension — the library's custom window title bar/chrome, visible only in a
real `Window`, never composed by `runComposeUiTest`) = **33 components with zero hover/focus/drag
coverage on any toolchain**:

`AeroAccordion`, `AeroBadge`, `AeroBreadcrumb`, `AeroCard`, `AeroCheckbox`, `AeroColorPicker`,
`AeroDivider`, `AeroDropdownItem`, `AeroDropdownPopup`, `AeroGroupBox`, `AeroIconButton`,
`AeroNotificationBanner`, `AeroNumberSpinner`, `AeroPanel`, `AeroPasswordField`,
`AeroProgressBar`, `AeroRadioButton`, `AeroRadioGroup`, `AeroScrollArea`, `AeroScrollBar`,
`AeroSearchField`, `AeroSidebar`, `AeroStatusBar`, `AeroStepperWizard`, `AeroTabBar`,
`AeroTextArea`, `AeroTextField`, `AeroTitleBar`, `AeroToastHost`, `AeroToolbar`, `AeroTreeView`,
`AeroTriStateCheckbox`, `AeroChip`.

These 33 were seen only as static content inside the showcase's default-state frames (Plan 10's
75-key comparison shows them at rest, unfocused, unhovered — the showcase sweep never interacts
with anything). Their interactive states (hover glow, focus ring, press/drag feedback) were not
captured or reviewed by the agent on either toolchain, so a rendering regression specific to
hover/focus/drag on any of these 33 would not have been caught by this phase's comparison.

(Internal, non-public sub-parts used only inside already-tested pickers —
`AeroHsvColorSquare`, `AeroHueSlider`, `AeroCalendarGrid`, `AeroStepIndicator` — are exercised
indirectly whenever their owning picker/wizard opens in a UI test, but were not independently
hover/focus-tested either; not counted above since they have no public entry point of their own.)

## 2. Popup parts that extend beyond the window frame

Every capture method in this phase — `captureToImage()` (UI tests) and `PrintWindow` (showcase,
MCP after-only) — rasterizes only the bounds of the window/root being captured. A popup
(`AeroDropdown`, `AeroComboBox`, `AeroContextMenu`, `AeroMenuBar`, `AeroTooltip`, `AeroPopover`,
any picker's calendar/time grid) that would visually extend past the edge of its host window on a
small screen or near a screen edge was never produced or seen in this phase — every capture used
the same 1200x800 showcase window (or the UI test's fixed-size host) with popups anchored well
inside those bounds. Off-screen clipping/repositioning behavior for an overflowing popup is
unconfirmed.

## 3. Display scale 125% and 200% (VER-F02)

**Every single capture in Phase 21 — showcase (`21-BASELINE.md`, `21-COMPARE.md`), UI tests
(`captureToImage()`, DPI-independent by construction), and the Plan 11 MCP after-only sweep
(`21-UITEST-COVERAGE.md`) — was taken at 100% display scale (96 DPI), confirmed per-frame via
`GetDpiForWindow` in every showcase manifest and named explicitly in `21-HRM.md`** ("Display scale
during every measurement: 100% (96 DPI)"). **125% and 200% display scale were never tested at any
point in this phase, on either toolchain.** Any layout, glass-surface, icon-scaling, or hit-target
issue that only appears at a non-100% Windows display scale is entirely unconfirmed.

## 4. Behaviour not expressed in semantics

MCP's `get_semantic_tree` and the UI tests' `SemanticsNodeInteraction` both read the Compose
semantics tree, not the rendered pixels' full behavioural surface. Not confirmed by either:
- Cursor shape changes (resize cursors on `AeroSplitPane`/`AeroPanelGroup` dividers, hand cursor
  on clickable rows) — semantics doesn't carry cursor icon state; only the resulting drag/click
  outcome was checked, not what the OS cursor looked like during hover.
- Animation easing/timing curves (spring stiffness/damping on hover glow, press lift-scale,
  drawer slide, tooltip fade) — only start/end frames were captured; the motion in between is
  unconfirmed on the new Compose Multiplatform 1.12.0 animation pipeline.
- Keyboard-only interaction with `AeroDialog`/`AeroAlertDialog` (Escape-to-close, focus trap
  inside the modal, Tab order between Cancel/OK) — Plan 11 Task 1 opened both dialogs only via an
  MCP mouse `click`; no keyboard input was sent to either window.
- Accessibility narrator/screen-reader behavior — the semantics tree used for MCP/UI-test
  identification is the same tree a screen reader would consume, but no accessibility tooling
  itself was run against it in this phase.
- Multi-monitor / per-monitor-DPI behavior — every measurement in this phase ran on one 96 DPI
  monitor; behavior when the showcase window is dragged between monitors at different scales is
  unconfirmed.

## 5. `AeroFilePicker`'s native OS dialog

`AeroFilePicker` opens `java.awt.FileDialog`, not this library's own rendering — clicking it would
open a real file-system dialog on the maintainer's desktop. Per D-08, it was never clicked in any
UI test (`D07PickerPopupCaptureTest.kt`'s scope excludes it) and never clicked via MCP (Plan 11
Task 1's action explicitly names it as never-clicked; `21-UITEST-COVERAGE.md`'s D-07/D-08 table
lists it as "never clicked in any test"). Its visual appearance and behavior are entirely outside
this library's control (native OS chrome) and entirely unconfirmed by this phase.

## 6. `AeroRangeSlider` keyboard focus

Not applicable — `AeroRangeSlider` has no keyboard-focus support at all (pre-existing v3.0 debt,
recorded in `21-UITEST-COVERAGE.md`: "n/a: no keyboard focus in AeroRangeSlider"). Its hover,
press and drag states are captured and compared (Group 1 of `21-DRIFT.md`'s antecedent data shows
`AeroRangeSlider` identical in all 3 themes in `21-COMPARE.md`'s per-component table); a future
keyboard-focus implementation would need its own coverage, not something this phase could confirm
or deny.

## 7. Every "inspected without baseline" (after-only) component

`AeroDialog` and `AeroAlertDialog` cannot be captured by `runComposeUiTest` (built on a real
`Window`, not reachable by `captureToImage()`) and have no pre-upgrade baseline image to diff
against. Plan 11 Task 1 opened both via MCP click in all three themes on the new toolchain only
and recorded a agent's visual read of each (`21-UITEST-COVERAGE.md`, "After-only MCP inspection"
section) — this confirms the new-toolchain rendering looks correct in isolation, but is **not** a
before/after comparison and is **not** counted among `21-DRIFT.md`'s 102 items:

| Component | Theme | Capture |
|---|---|---|
| AeroDialog | AeroBlue | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroDialog/AeroBlue/opened.png` |
| AeroDialog | AeroDark | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroDialog/AeroDark/opened.png` |
| AeroDialog | Classic | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroDialog/Classic/opened.png` |
| AeroAlertDialog | AeroBlue | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroAlertDialog/AeroBlue/opened.png` |
| AeroAlertDialog | AeroDark | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroAlertDialog/AeroDark/opened.png` |
| AeroAlertDialog | Classic | `.captures/new-kt2.4.20-cmp1.12.0/mcp-after-only/AeroAlertDialog/Classic/opened.png` |

**Related, unconfirmed finding (not a rendering drift — a harness/interaction fact):** opening the
Classic-theme `AeroDialog` and `AeroAlertDialog` via MCP click took the OS foreground away from the
maintainer's own (idle) window in both Classic openings, while the same action left the
maintainer's foreground untouched in the other 4 (AeroBlue/AeroDark) openings — Windows'
foreground-lock timeout let the background app steal focus once the maintainer had been idle long
enough. This is a fact about MCP-driven dialog interaction on this machine (T-21-09), not something
confirmed to reproduce on every machine/timing, and not a drawing-code difference — it is listed
here, not in `21-DRIFT.md`.

## 8. UI-test states dropped as "no visual change on old toolchain"

**None.** `21-UITEST-COVERAGE.md` states explicitly, for both the glass/selection states (Task 2,
21 combinations) and the drag states (Task 3, 9 combinations): "no state needed to be dropped as
'no visual change on old toolchain.'" Every state defined by the BASE-05/D-07 suites produced a
real, kept assertion. This item is listed to confirm the check was made, not because anything was
found.

## 9. Anything else found during the sweep

- **`AeroPasswordField`'s show/hide-password toggle was never actually clicked in either UI tests
  or the showcase sweep** — the eye-icon glyph anti-aliasing delta in `21-DRIFT.md` Group 1 covers
  the icon's *rest* state only; the toggled (password-visible) state's rendering is unconfirmed on
  the new toolchain.
- **The showcase's own start-up can take the OS foreground** — `21-HRM.md` recorded this
  happening on 1 of 2 launches during HRM-02 measurement, unrelated to any MCP click. Plan 11
  accounted for it by taking the foreground baseline only after the window was sent to the bottom,
  but the underlying start-up-foreground behavior itself (how often, under what conditions) is not
  characterized beyond that one 1-of-2 observation.
- **CMP 1.12.0's `LocalClipboardManager` deprecation** (`IconsSection.kt:325`, noted in
  `21-TOOLCHAIN-LOG.md` Step 3c) — the warning was recorded but no functional clipboard-copy
  behavior was exercised or captured in this phase; clipboard interaction on the new toolchain is
  unconfirmed.
- **Multi-monitor / window-drag-between-monitors behavior** — see §4; not separately re-listed,
  cross-referenced here as a sweep-wide gap rather than a single-component one.
