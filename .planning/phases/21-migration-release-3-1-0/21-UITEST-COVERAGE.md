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
