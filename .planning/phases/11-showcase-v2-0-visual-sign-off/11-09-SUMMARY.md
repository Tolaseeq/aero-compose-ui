---
phase: 11-showcase-v2-0-visual-sign-off
plan: "09"
subsystem: pickers/layout
tags: [color-picker, accordion, hue-slider, gap-closure, F10, F11, F12]
dependency_graph:
  requires: []
  provides: [hue-slider-visible, before-after-labels, intrinsic-width-popup, accordion-divider-inset]
  affects: [sign-off-item-7, sign-off-item-9]
tech_stack:
  added: []
  patterns:
    - "Modifier.width(280.dp) before glassPanel to bound popup width"
    - "AeroHsvColorSquare explicit Modifier.size(220.dp) to honor sibling 24dp hue slider"
    - "padding(horizontal=8.dp) before height(1.dp) for inset divider inside rounded bg"
key_files:
  created: []
  modified:
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPicker.kt
    - library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPickerButton.kt
    - library/src/main/kotlin/com/mordred/aero/components/layout/AeroAccordion.kt
decisions:
  - "AeroHsvColorSquare sized to 220dp (not 256dp) so 24dp hue slider fits inside 280dp bounded panel"
  - "Popup popup wraps AeroColorPicker in Box to prevent Popup measuring children at window width"
  - "Accordion divider inset 8dp horizontal matching glassPanel cornerRadius"
metrics:
  duration: "~3min"
  completed_date: "2026-06-18"
  tasks_completed: 3
  files_modified: 3
---

# Phase 11 Plan 09: ColorPicker Hue Slider + Popup Glass + Accordion Divider Summary

Gap-closure plan fixing three sign-off defects: F12 (hue slider not discoverable), F10 (ColorPicker popup glass/width/labels), F11 (accordion divider overflows rounded bg). Unblocks sign-off item 7 (ColorPicker round-trip) and clears the item 9 cosmetic note.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | F12 surface hue slider + F10 before/after labels + intrinsic width | 3154357 | AeroColorPicker.kt |
| 2 | F10 ColorPicker popup glass/rounded consistency | 770f872 | AeroColorPickerButton.kt |
| 3 | F11 Accordion divider inset 8dp | cab5ff5 | AeroAccordion.kt |

## Root Causes and Fixes

### F12 — Hue slider not discoverable

**Root cause:** `AeroHsvColorSquare` had no explicit size modifier and defaulted to `256x256dp`. Inside a `Row` placed in a Column with no width constraint, the Row was as wide as the available space (potentially window width), distributing space. With `fillMaxWidth` semantics on the parent Column, the `Row` would expand so the 256dp square consumed nearly all horizontal space and pushed the 24dp `AeroHueSlider` Canvas off-screen or clipped it.

**Fix (Task 1):**
- Added `modifier.width(280.dp)` to the outer Column so the entire panel has a bounded width.
- Added explicit `Modifier.size(220.dp)` to `AeroHsvColorSquare` so the Row knows exactly how wide the square is (220 + 8 gap + 24 hue = 252dp, fits within 280dp minus 24dp padding).
- `AeroHueSlider` retains its intrinsic `24x256dp` Canvas — no changes to the slider itself.

### F10 — Before/after swatch purpose unclear + popup stretches full width

**Root cause (labels):** The preview Row was two bare colored Boxes with no text indicating which was the original and which was the live color.

**Root cause (width):** The Column had no width constraint; inside an open Popup the Compose layout engine can measure children at the full available width.

**Fix (Task 1 — labels):** Replaced the bare two-Box Row with two labeled `Column(weight(1f))` containers, each having a `Text("Original"/"Current")` caption above the color `Box`.

**Fix (Task 1 — width):** `Modifier.width(280.dp)` bounds the panel before `glassPanel(cornerRadius = 8.dp)` is applied, preserving glass + rounded corners (W11-02).

**Fix (Task 2 — popup container):** Wrapped `AeroColorPicker(...)` in a plain `Box` inside the `Popup` so the Popup measures to content rather than expanding to match available window width. `PickerPopupContainer` was NOT used (would double the glass surface). `AeroCalendarPositionProvider` + `PopupProperties` preserved (W11-01).

### F11 — Accordion divider longer than rounded section background

**Root cause:** The inter-section divider was `Modifier.fillMaxWidth().height(1.dp)` — it spanned the full column width, visually extending past the section header's `glassPanel(cornerRadius = 8.dp)` rounded edges.

**Fix (Task 3):** Added `.padding(horizontal = 8.dp)` before `.height(1.dp)`. The 8dp inset matches the 8.dp cornerRadius of the `glassPanel`, so the divider aligns with the visual start of the straight edge of the rounded background.

## Sign-off Impact

| Sign-off Row | Defect | Status after this plan |
|---|---|---|
| 7 — ColorPicker round-trip | F12 hue not findable, F10 popup stretched/unlabelled | Unblocked — hue visible, popup bounded glass, swatches labelled |
| 9 — Accordion cosmetic | F11 divider overflows | Cleared — divider inset |

## Deviations from Plan

None — plan executed exactly as written.

## Self-Check: PASSED

- AeroColorPicker.kt: FOUND
- AeroColorPickerButton.kt: FOUND
- AeroAccordion.kt: FOUND
- Commit 3154357 (Task 1): FOUND
- Commit 770f872 (Task 2): FOUND
- Commit cab5ff5 (Task 3): FOUND
