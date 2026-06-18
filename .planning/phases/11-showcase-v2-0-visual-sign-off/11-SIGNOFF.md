---
phase: 11-showcase-v2-0-visual-sign-off
date: 2026-06-18
launch_command: "./gradlew :showcase:run"
gate_status: FAILED
verified_by: human (eyes-on, all three themes)
---

# Phase 11 v2.0 Visual Sign-off

**Phase:** 11-showcase-v2-0-visual-sign-off
**Date:** 2026-06-18
**Launch command:** `./gradlew :showcase:run`
**Milestone gate:** Phase 11 is NOT complete until all 48 cells below are PASS.

> **GATE FAILED on human verification 2026-06-18.** Automated grep gates (items 15-16) pass.
> Eyes-on verification across AeroBlue / AeroDark / Classic surfaced 16 component-level
> defects + several design-change requests. All defects reproduce theme-independently
> (they are component bugs, not theme bugs). Full detail in **Findings** below. These route
> to gap closure — gap plans MAY edit component source (the additive-only constraint applied
> only to the sign-off plan 11-05 itself).

---

## Sign-off Table

| # | Checklist Item | AeroBlue | AeroDark | Classic | Result note |
|---|----------------|----------|----------|---------|-------------|
| 1 | DataTable virtualization (~10-15 rows, scroll renders new) | PASS | PASS | PASS | OK |
| 2 | Selection survives sort (SAT-050 keeps highlight) | PASS | PASS | PASS | OK |
| 3 | Column resize bounds | FAIL | FAIL | FAIL | Right drag unbounded — column leaves screen, table not width-clamped. Left drag over-shrinks "Name" — header label + data unreadable, no usable min width. See **F-RESIZE**. |
| 4 | TreeView lazy callback fires once | PASS | PASS | PASS | OK |
| 5 | DatePicker popup right-aligns at ~1024dp | BLOCKED | BLOCKED | BLOCKED | Could not verify — full-width trigger field obscured interaction. See **F8**. |
| 6 | DateRangePicker partial state shows "—" | PASS | PASS | PASS | OK |
| 7 | ColorPicker HEX round-trip (no drift) | FAIL | FAIL | FAIL | Saturation control not discoverable; layout unclear. See **F12 / F10**. |
| 8 | RangeSlider thumbs both reachable on overlap | FAIL | FAIL | FAIL | Moving one thumb resets the other to default. See **F9**. |
| 9 | Accordion single-mode (A closes when B opens) | PASS | PASS | PASS | Behaviour OK; cosmetic divider issue **F11**. |
| 10 | SplitPane clamp (panes ≥48dp) | PASS | PASS | PASS | Clamp OK; but ghosting on drag **F3** + reduced drag sensitivity **F15**. |
| 11 | Sidebar adjacent reflow on toggle | PASS | PASS | PASS | OK |
| 12 | Wizard validation gate | FAIL | FAIL | FAIL | Back/Next disabled — wizard non-interactive in demo. See **F-WIZARD**. |
| 13 | AeroDark disabled cells visible (grey) | N/A | BLOCKED | N/A | No disabled dates exist in demo — nothing to verify. See **F13**. |
| 14 | Desktop drag responds on first pixel | FAIL | FAIL | FAIL | Clear lag / reduced sensitivity vs cursor. See **F15**. |
| 15 | No `transparent = true` (grep) | PASS | PASS | PASS | Automated — 0 hits, 2026-06-18. |
| 16 | No `AeroScrollArea` in DataTable pkg (grep) | PASS | PASS | PASS | Automated — 0 hits, 2026-06-18. |

---

## Findings (human UAT, 2026-06-18)

Grouped by component. Severity: 🔴 bug · 🟠 UX defect · 🟣 design change / scope decision.

### DataTable
- **F2** 🟠 Adjacent cell values visually merge with no gap — e.g. NORAD ID `40084` and date `2026-03-26` render as `400842026-03-26`. Columns need explicit horizontal separation / cell padding.
- **F3** 🔴 "Ghosting" / doubling artifact while dragging the column splitter (the column appears duplicated during drag). Same artifact on `AeroSplitPane` divider drag.
- **F4** 🟠 Sort triggers only when clicking exactly on the header **text**, not anywhere in the header cell. The whole header cell should be the click target.
- **F-RESIZE** 🔴 Column resize unbounded on the right (column can be dragged far off-screen; table is not width-clamped) and over-shrinks on the left (the "Name" header label and data become unreadable). Need a usable per-column min width and a table width bound.

### TreeView
- **F5** 🟠 Expand triggers only on the `>` chevron glyph, not anywhere on the node row. The whole row should toggle expand/collapse.

### Pickers (shared)
- **F6** 🟣 Date format must be **strictly DD.MM.YYYY** everywhere a date appears (pickers, table, previews). Currently ISO `yyyy-MM-dd`.
- **F8** 🔴 Every picker trigger `TextField` stretches to full window width, which squeezes the value-preview Text (to the right of the field) into a vertical column. Triggers should be compact / intrinsic width with the preview beside them.

### TimePicker
- **F7** 🟣 Need seconds support (HH:MM:SS). Current `AeroTimePicker` is minutes-only (seconds were descoped in Phase 8). **Decision needed** — re-open scope to add seconds.

### ColorPicker
- **F10** 🟣 Give it the same glass background + rounded-corner Aero styling as the other pickers; stop stretching it full-width. Clarify the left half of the top swatch row (it stays the default colour regardless of selection — purpose unclear / unlabeled).
- **F12** 🔴 No saturation control is discoverable. Visible: HSV square, the old/new colour swatch row, RGB rows, HEX input, presets. The hue/saturation adjustment is not findable. (The square's axes encode S/V, but there is no obvious hue slider — investigate whether the hue slider is missing or mis-rendered.)

### RangeSlider
- **F9** 🔴 Starting to drag one thumb resets the other thumb to its default position. Both thumbs must hold their values independently.

### DatePicker / DateRangePicker
- **F13** 🟠 Demo configures no min/max, so there are no disabled dates to verify item 13. Add a demo with disabled dates.
- **F14** 🟣 `AeroDateRangePicker` cannot select a range within the **same month/year** (e.g. 07.06.2025–17.06.2025) — it forces two adjacent months. Need same-month range selection (both endpoints clickable in one month).

### Accordion
- **F11** 🟠 The inter-section divider appears **longer** than the section background, because the section background has rounded corners. Reduce the divider width/length to fit within the rounded background.

### SplitPane
- (covered by **F3** ghosting + **F15** sensitivity)

### StepperWizard
- **F-WIZARD** 🔴 Back and Next are both disabled — the wizard is non-interactive in the demo; could not advance or interact at all. Investigate the validation gate and field input on step 1.

### Drag (cross-component root cause)
- **F15** 🔴 Reduced drag sensitivity — sliders/handles move slower than the mouse cursor (cursor outpaces the thumb). Affects RangeSlider thumbs, ColorPicker HSV square, and DataTable column splitters. Likely shares a root cause with the **F3** ghosting (drag coordinate mapping / leftover offset).

---

## Notes

- Items 15-16 are automated grep gates (theme-independent), verified 2026-06-18.
- All FAIL findings reproduce across all three themes — they are component-level, not theme-specific.
- Per-theme cell granularity was not separately recorded for the failing items because the defects are theme-independent; the same value is shown across the three theme columns.

## Completion Criteria (NOT yet met)

Phase 11 milestone gate (SHW-10) is satisfied when:
- [ ] All 48 cells are PASS (or N/A for item 13 AeroBlue/Classic)
- [x] Automated gates (15, 16) PASS
- [ ] 16 component defects above resolved and re-verified eyes-on
