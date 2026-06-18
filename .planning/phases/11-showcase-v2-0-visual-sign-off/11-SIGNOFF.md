---
phase: 11-showcase-v2-0-visual-sign-off
date: 2026-06-18
launch_command: "./gradlew :showcase:run"
gate_status: IN PROGRESS
---

# Phase 11 v2.0 Visual Sign-off

**Phase:** 11-showcase-v2-0-visual-sign-off
**Date:** 2026-06-18
**Launch command:** `./gradlew :showcase:run`
**Milestone gate:** Phase 11 is NOT complete until all 48 cells below are PASS.

> Run the showcase with `./gradlew :showcase:run`, switch themes using the ThemeSwitcher
> (AeroBlue → AeroDark → Classic), and verify each item eyes-on. Items 15 and 16 are
> automated grep gates already verified — they apply identically to all three themes.

---

## Sign-off Table

| # | Checklist Item | AeroBlue | AeroDark | Classic | How Verified |
|---|----------------|----------|----------|---------|--------------|
| 1 | **DataTable virtualization:** Table shows ~10-15 rows visible (not all 100); scroll renders new rows without materializing all items at once. | PENDING | PENDING | PENDING | Visual: scroll DataSection table; count visible rows above fold; confirm new rows render on scroll. |
| 2 | **Selection after sort:** Select SAT-050, sort "AOS Date" desc — SAT-050 keeps its highlight at new position. (In AeroDark also verify all four row states — normal/hover/selected/selected+hover — are distinguishable.) | PENDING | PENDING | PENDING | Interactive: sort asc → click SAT-050 → sort desc → verify SAT-050 still highlighted. In AeroDark also hover over unselected + hover over selected to confirm 4 states distinct. |
| 3 | **Column resize:** Drag "Name" column splitter right past "NORAD ID" — other columns shrink but none vanish; drag left to 40dp minimum stop. | PENDING | PENDING | PENDING | Interactive: drag Name header splitter right → confirm columns reflow without zero-width. Drag left until it stops (40dp). |
| 4 | **TreeView lazy callback:** Expand a ground-station node, scroll it off screen, scroll back — console shows only ONE "onExpand fired for: X" line for that node. | PENDING | PENDING | PENDING | Interactive: expand node → scroll away → scroll back → check stdout/console for duplicate onExpand lines (0 duplicates = PASS). |
| 5 | **DatePicker popup position:** At ~1024dp window width, open DatePicker in right-half area — calendar right-aligns and does not clip. | PENDING | PENDING | PENDING | Visual: resize window to ~1024dp → open AeroDatePicker → confirm calendar does not clip the right edge. |
| 6 | **DateRangePicker partial state:** Click one date, dismiss — value Text still shows placeholder ("—"), not a committed range. | PENDING | PENDING | PENDING | Interactive: open DateRangePicker → click one date → click outside to dismiss → confirm value Text reads "—" (placeholder). |
| 7 | **ColorPicker round-trip:** Set to #FF0000, drag saturation to 50% then back to 100% — HEX reads #FF0000 (no drift). | PENDING | PENDING | PENDING | Interactive: open AeroColorPickerButton → drag HSV saturation left then back right → confirm HEX Text = #FF0000. |
| 8 | **RangeSlider thumb overlap:** Drag start thumb to meet end thumb — both thumbs remain individually reachable and moveable. | PENDING | PENDING | PENDING | Interactive: drag start thumb right to meet end thumb → release → drag both thumbs individually to confirm neither is stuck. |
| 9 | **Accordion single mode:** Open section A then section B — A closes. | PENDING | PENDING | PENDING | Interactive: in LayoutSection single-mode column, open section 1 → click section 2 → confirm section 1 closes. |
| 10 | **SplitPane clamp:** Drag divider to each edge (h+v demos) — both panes stay >= 48dp. | PENDING | PENDING | PENDING | Interactive: drag SplitPane divider all the way left/right (horizontal) and up/down (vertical) → confirm neither pane collapses to zero. |
| 11 | **Sidebar adjacent reflow:** Click "Toggle mode" — sidebar animates Expanded→Collapsed→Hidden; adjacent content expands to fill reclaimed space. | PENDING | PENDING | PENDING | Interactive: click Toggle mode button three times → verify smooth animation and adjacent content reflowing each step. |
| 12 | **Wizard validation:** Tab through step 1 field without clicking Next — no validation feedback; then click Next with blank field — Next is blocked. | PENDING | PENDING | PENDING | Interactive: Tab through Identifier field → confirm no feedback; then click Next with blank → confirm button is disabled or validation prevents advance. |
| 13 | **AeroDark disabled cells:** In AeroDark, open DatePicker/DateRangePicker to a month with disabled dates — disabled cells are visible (grey via labelText token), not invisible. | N/A | PENDING | N/A | AeroDark only: open DatePicker → navigate to month with disabled (past) dates → confirm disabled cells show as grey, not invisible. |
| 14 | **Desktop drag response:** HSV square, RangeSlider thumbs, and DataTable column splitters all respond on FIRST mouse movement (no ~18px slop delay). | PENDING | PENDING | PENDING | Interactive: drag each of the three drag targets — first pixel should register. Any 18dp delay indicates leftover detectDragGestures touchSlop bug. |
| 15 | **No `transparent = true`:** Grep across all new/touched v2.0 files returns 0 matches. | PASS | PASS | PASS | Automated grep gate (theme-independent). W11-01: `grep -rn "transparent = true"` across pickers/datatable/layout/range library dirs + DataSection.kt/PickersSection.kt/LayoutSection.kt → 0 hits. Verified 2026-06-18. |
| 16 | **No `AeroScrollArea` in DataTable package:** Grep returns 0 matches. | PASS | PASS | PASS | Automated grep gate (theme-independent). W11-02: `grep -rn "AeroScrollArea" library/src/main/kotlin/com/mordred/aero/components/datatable/` → 0 hits. Verified 2026-06-18. |

---

## Notes

- Item 13 is AeroDark-only by definition — AeroBlue and Classic columns marked N/A.
- Items 15 and 16 are automated grep gates run on 2026-06-18; result applies to all three theme columns identically since grep is theme-independent.
- All other items (1-14) require eyes-on verification in the running showcase (`./gradlew :showcase:run`) with ThemeSwitcher set to each theme in turn.
- Any FAIL cell is a component-level bug; Phase 11 is additive-only — do NOT edit component source from this plan. Record FAIL and report for gap-closure decision.

---

## Completion Criteria

Phase 11 milestone gate (SHW-10) is satisfied when:
- [ ] All 48 cells are filled (no PENDING remaining)
- [ ] Every cell is PASS (or N/A for item 13 AeroBlue/Classic columns)
