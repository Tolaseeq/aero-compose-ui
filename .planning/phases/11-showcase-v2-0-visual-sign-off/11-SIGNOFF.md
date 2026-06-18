---
phase: 11-showcase-v2-0-visual-sign-off
date: 2026-06-18
launch_command: "./gradlew :showcase:run"
gate_status: PASSED
verified_by: human (eyes-on, all three themes)
---

# Phase 11 v2.0 Visual Sign-off

**Phase:** 11-showcase-v2-0-visual-sign-off
**Date:** 2026-06-18
**Launch command:** `./gradlew :showcase:run`
**Milestone gate:** Phase 11 complete — all 48 cells PASS.

> **GATE PASSED 2026-06-18** (re-verification after gap-closure plans 11-06 to 11-10).
> One additional defect **F-ACCORDION-HOVER** (accordion header hover/press highlight rendered with sharp corners over the rounded glass surface) was found and fixed during sign-off at commit **94a524f** (added `.clip(RoundedCornerShape(8.dp))` before `.clickable` in `AeroAccordion.kt`). After that fix all items passed.

---

## Re-verification 2026-06-18 (after gap closure plans 11-06 to 11-10)

### Automated Gates (pre-verified)

| Gate | Command | Result | Date |
|------|---------|--------|------|
| W11-01 `transparent = true` | `grep -rn "transparent = true" library/.../pickers/ datatable/ layout/ range/ internal/drag/ showcase/sections/Data\|Pickers\|Layout...` | 0 hits — PASS | 2026-06-18 |
| W11-02 `AeroScrollArea` in datatable | `grep -rn "AeroScrollArea" library/.../datatable/` | 0 hits — PASS | 2026-06-18 |

### Build Gate

| Command | Result | Date |
|---------|--------|------|
| `./gradlew :library:test :showcase:compileKotlin` | BUILD SUCCESSFUL — 0 test failures | 2026-06-18 |

---

## Sign-off Table (Re-verification — FINAL)

> All 48 cells verified eyes-on across all three themes (AeroBlue / AeroDark / Classic).
> Items 15-16: PASS (automated grep gates, re-run 2026-06-18).
> Previously-FAIL rows re-tested against their gap-closure fixes.

| # | Checklist Item | AeroBlue | AeroDark | Classic | Re-test target / Notes |
|---|----------------|----------|----------|---------|------------------------|
| 1 | DataTable virtualization (~10-15 rows, scroll renders new) | PASS | PASS | PASS | Was PASS — re-confirmed still OK |
| 2 | Selection survives sort (SAT-050 keeps highlight; AeroDark four-state distinguishable) | PASS | PASS | PASS | Was PASS — re-confirmed (PITFALL-10 AeroDark) |
| 3 | Column resize bounds — right-bound clamped, left stops at readable min (~120dp), no ghosting, 1:1 drag | PASS | PASS | PASS | Fixed by **F-RESIZE** (11-07) + drag root cause (11-06) |
| 4 | TreeView lazy callback fires once (row-level click expands; no second onExpand on scroll-back) | PASS | PASS | PASS | F5 whole-row toggle (11-07) — once-only guard intact |
| 5 | DatePicker popup right-aligns at ~1024dp (compact trigger, no clip) | PASS | PASS | PASS | Fixed by **F8** compact trigger (11-10) |
| 6 | DateRangePicker partial state shows "—" (same-month range works; dismiss mid-range stays "—") | PASS | PASS | PASS | Fixed by **F14** same-month (11-08); PITFALL-06 partial-state guard |
| 7 | ColorPicker HEX round-trip: hue slider visible, #FF0000 survives sat 100%→50%→100% | PASS | PASS | PASS | Fixed by **F12+F10** (11-09): hue slider discoverable, glass panel |
| 8 | RangeSlider: both thumbs hold independent values; overlap reachable | PASS | PASS | PASS | Fixed by **F9** (11-06): stale-capture root cause resolved |
| 9 | Accordion single mode (A closes when B opens); divider fits rounded bg | PASS | PASS | PASS | Behaviour was OK; cosmetic divider fixed by **F11** (11-09); hover clip fixed by **F-ACCORDION-HOVER** (94a524f) |
| 10 | SplitPane clamp (panes ≥48dp); no ghosting; 1:1 drag | PASS | PASS | PASS | Fixed by **F3+F15** (11-06): positionChange() delta, no ghosting |
| 11 | Sidebar adjacent reflow on toggle | PASS | PASS | PASS | Was PASS — re-confirmed still OK |
| 12 | Wizard: type field→Next enables; Next advances; Back returns with value; Next blocked when blank | PASS | PASS | PASS | Fixed by **F-WIZARD** (11-10): bounded Box(height) wrapper |
| 13 | AeroDark disabled cells visible (grey, not invisible) — DatePicker min/max + RangePicker min/max demos | N/A | PASS | N/A | Fixed by **F13** (11-10): disabled-date demo added; AeroDark readable |
| 14 | Desktop drag responds on first pixel: HSV square, RangeSlider thumbs, DataTable column splitter | PASS | PASS | PASS | Fixed by **F15** (11-06): positionChange() root cause; all three drag sites |
| 15 | No `transparent = true` (grep W11-01) | PASS | PASS | PASS | Automated — 0 hits, re-run 2026-06-18 |
| 16 | No `AeroScrollArea` in DataTable pkg (grep W11-02) | PASS | PASS | PASS | Automated — 0 hits, re-run 2026-06-18 |

---

## In-Sign-off Fix: F-ACCORDION-HOVER

**Defect:** Accordion section-header hover/press highlight rendered with sharp corners over the rounded glass surface.
**Fix:** Added `.clip(RoundedCornerShape(8.dp))` before `.clickable` in `AeroAccordion.kt`.
**Commit:** 94a524f
**Impact:** Item 9 (Accordion) now fully PASS across all three themes.

---

## Findings (human UAT, 2026-06-18 — HISTORICAL RECORD)

> These findings are the original FAILED sign-off from 2026-06-18, preserved as historical record.
> All 16 defects were addressed in gap-closure plans 11-06 through 11-10.
> Re-verification above confirms every fix held.

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
- **F7** 🟣 Need seconds support (HH:MM:SS). Current `AeroTimePicker` is minutes-only (seconds were descoped in Phase 8). **DECISION 2026-06-18:** add an optional `showSeconds: Boolean = false` param to `AeroTimePicker` (and propagate to `AeroDateTimePicker`); default `false` to preserve the existing HH:MM API. Showcase demo enables it.

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
- All original FAIL findings reproduced across all three themes — they are component-level, not theme-specific.
- Gap-closure plans 11-06 (drag root cause F3/F9/F15), 11-07 (DataTable/TreeView F2/F4/F5/F-RESIZE), 11-08 (pickers F6/F14), 11-09 (ColorPicker F12/F10 + Accordion F11), 11-10 (showcase F8/F7/F13/F6/F-WIZARD) were executed to resolve all 16 defects.
- Item 13 columns: AeroDark is the only theme requiring eyes-on verification; AeroBlue/Classic marked N/A as there is no disabled-state difference to test there.
- F-ACCORDION-HOVER was found and fixed during sign-off (not in original gap-closure plans); commit 94a524f.

## Completion Criteria

Phase 11 milestone gate (SHW-10) is satisfied:
- [x] All 40 previously-PENDING cells filled PASS (13 items x 3 themes + item 13 AeroDark only; AeroBlue/Classic for item 13 are N/A)
- [x] Automated gates (15, 16) PASS
- [x] Frontmatter `gate_status` updated to PASSED
- [x] 16 component defects above confirmed resolved eyes-on
- [x] In-sign-off defect F-ACCORDION-HOVER found, fixed (94a524f), and re-verified PASS
