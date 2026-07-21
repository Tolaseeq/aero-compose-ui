# Project Research Summary

**Project:** aero-compose-ui v2.0.2 - AeroPanelGroup
**Domain:** Compose Desktop UI library - additive vertical collapsible+resizable layout component
**Researched:** 2026-06-22
**Confidence:** HIGH

---

## Executive Summary

AeroPanelGroup is a single new layout component (plus its AeroPanelSection data class) that fills the gap between AeroAccordion (content-sized, no drag) and AeroSplitPane (2-pane, no collapse): N vertical sections that fill their parent container height, collapse to a fixed header strip on click, and let adjacent expanded sections be resized by dragging a divider between them. The canonical reference is the VS Code Left Sidebar (Explorer / Source Control / Outline / Timeline). The component is additive - no existing components change, no new Gradle dependencies are required.

The implementation strategy is almost entirely a composition of existing in-repo primitives: BoxWithConstraints (same as AeroSplitPane), aeroDragSplitter + clampDividerPx (reused verbatim), animateFloatAsState at 200ms FastOutSlowInEasing (same as AeroSidebar), the AeroAccordion hybrid controlled/uncontrolled API shape, and glassPanel + CaretRight for the Aero header. The only genuinely new code is one pure-JVM distribution file (PanelDistribution.kt - no Compose imports, fully unit-testable) and the two public Compose files (AeroPanelGroup.kt and its showcase addition).

The primary risk - and the mandatory first step - is confirming that animateFloatAsState and direct drag writes to the same sizePx state can coexist without oscillation. The architecture section describes the likely resolution (split intent-state from display-state), but this has not been empirically tested in this codebase. All other implementation decisions are direct ports of shipped patterns with HIGH confidence. This is strictly a single-phase milestone; the build order within the phase leads with that spike.

---

## Open Questions

These questions were raised by the researchers and must be answered before requirements are finalized. Resolve them with the user before writing REQ-IDs.

| # | Question | Where It Bites | Researcher Suggested Default |
|---|----------|----------------|------------------------------|
| OQ-1 | **onLayoutChange firing contract:** Should it fire on every drag delta, on drag-end only, or on both drag-end and toggle? FEATURES.md says after drag-end or toggle; PITFALLS.md says not on every drag frame. These are consistent but must be explicit in the REQ-ID. | API design; caller persist strategy | Fire on drag-end and on each collapse/expand toggle - not on every drag frame |
| OQ-2 | **Min-size granularity:** Is `minSectionSize: Dp` a group-level parameter or per-section? FEATURES.md writes group-level; ARCHITECTURE.md shows per-section in pseudocode. The clamp logic differs. | API shape of AeroPanelSection and clampPanelDividerPx signature | Group-level matches AeroSplitPane simplicity; per-section is more flexible - confirm with user |
| OQ-3 | **Collapsed header height:** STACK.md states `36.dp` with no qualifier; ARCHITECTURE.md treats it as a constant. Fixed in the library or caller-configurable? | Whether a `headerHeight: Dp` parameter is needed | Fixed 36dp matches AeroAccordion; exposing it adds API surface with no stated consumer need |
| OQ-4 | **Section stable ID:** PITFALLS.md (PNL-PITFALL-08) requires `key(section.id)` to prevent state re-key on reorder. FEATURES.md and ARCHITECTURE.md do not include `id` in the data class. A default of `title` breaks with duplicate titles. | Public API of AeroPanelSection | Add `id: String = title` as a defaulted parameter; document that callers with duplicate titles must supply unique IDs |

---

## Key Findings

### Recommended Stack

No Gradle changes required. AeroPanelGroup is built entirely from APIs already on the compile classpath. Confirmed versions: Kotlin 2.1.21, Compose Multiplatform 1.7.3, JDK 17.

**Core APIs used:**

- `BoxWithConstraints` - read `constraints.maxHeight.toFloat()` once as `totalPx`; same pattern as AeroSplitPane; not SubcomposeLayout
- `animateFloatAsState` + `tween(200, FastOutSlowInEasing)` - collapse/expand animation; matches AeroSidebar timing
- `Modifier.aeroDragSplitter(Orientation.Vertical, onDrag)` - drag resize; reused verbatim from AeroDragSplitter.kt; avoids `detectDragGestures` touchSlop bug (PITFALL-03)
- `clampDividerPx` from SplitClamp.kt - reused verbatim; inverted-range guard already in place
- `mutableStateListOf` - per-section `sizePx`, `lastExpandedFraction`, `expanded`
- `rememberUpdatedState(totalPx)` - mandatory; prevents stale-capture snap-back (FIXSP-01 pattern)
- `Modifier.glassPanel(cornerRadius = 0.dp)` + `AeroIcons.CaretRight` - Win7 Aero header

**New internal file (pure JVM, no Compose imports):**

    library/src/main/kotlin/com/mordred/aero/components/layout/internal/panelgroup/PanelDistribution.kt

Contains `distributePx`, `shareTransferOnCollapse`, `shareTransferOnExpand`, `computeAvailablePx`. All unit-testable without a Compose runtime.

### Expected Features

**Must have (table stakes):**

- N sections in a vertical column filling parent height - the defining behavior vs. AeroAccordion
- Collapse section to ~36dp header strip; neighbors absorb freed space proportionally
- Expand collapsed section; size restores from `lastExpandedFraction`; neighbors shrink proportionally
- Drag divider between two adjacent expanded sections; direct px writes, no animation lag
- Divider only between two expanded neighbors; static join between expanded+collapsed pair
- Min-size clamp per divider drag (prevents zero-height sections)
- `collapsible = false` per section (locked decision)
- `resizable = false` per section or group-level (locked decision)
- Win7 Aero header styling: `glassPanel`, CaretRight 0-to-90-degree rotation animation
- Hybrid controlled/uncontrolled expansion API (matches AeroAccordion exactly)
- `onLayoutChange` + `initialSizes` parameters for persist/restore
- Showcase demo in LayoutSection.kt with three-theme sign-off

**Should have (cheap differentiators, include in this milestone):**

- `headerActions: @Composable RowScope.() -> Unit` slot per section (VS Code model; essentially free)
- `leadingIcon: ImageVector?` per section (matches AeroAccordion API; free)
- Pure-logic unit tests in PanelDistributionTest.kt (library quality gate)

**Defer (not in v2.0.2):**

- Horizontal orientation
- Keyboard resize of dividers
- Imperative collapse/expand handle
- `maxSize` per section
- Nested AeroPanelGroup support
- Drag-to-reorder sections

### Architecture Approach

The component follows three existing patterns simultaneously: the `BoxWithConstraints` + fraction-state pattern from AeroSplitPane (sizes stored as weights in an expanded pool, derived to px each recompose - no `remember(totalPx)` key); the `aeroDragSplitter` + `clampDividerPx` drag infrastructure from AeroSplitPane (reused verbatim, `Orientation.Vertical`); and the hybrid controlled/uncontrolled expansion API from AeroAccordion. The distribution math (`availableForExpanded = totalPx - sum-of-collapsedHeaders - sum-of-activeDividers`) is extracted into a pure-JVM PanelDistribution.kt file testable with `kotlin.test` in the same pattern as SplitClampTest.kt and AccordionToggleTest.kt.

**Major components:**

1. `AeroPanelGroup` (layout/AeroPanelGroup.kt) - public composable; owns `BoxWithConstraints`, section state list, render loop, divider placement logic
2. `AeroPanelSection` (data class, same file) - pure data descriptor: title, content lambda, collapsible, resizable, leadingIcon, headerActions
3. `PanelGroupDivider` (private composable, same file) - 8dp hit-area + 1dp Aero line + grip dots; `aeroDragSplitter` applied; rendered only between two adjacent expanded sections
4. `PanelDistribution.kt` (pure JVM, internal/panelgroup/) - `distributePx`, `shareTransferOnCollapse`, `shareTransferOnExpand`, `computeAvailablePx`

**Key patterns:**

- Sizes stored as raw px weights in a `SnapshotStateList<Float>`; rendered heights derived as proportional shares of `availableForExpanded` each recompose - window resize rescales proportionally with no state reset
- `animateFloatAsState` per section targeting rendered height or `headerPx`; drag writes directly to `sizePx` (subject to spike confirmation in Step 1)
- Divider existence derived inline (`isExpanded(i) && isExpanded(i+1)`); no divider state to synchronize
- `rememberUpdatedState(totalPx)` mandatory in drag lambda; `SnapshotStateList` reads inside drag lambda are always live

### Critical Pitfalls

**Top risks, in priority order:**

1. **PNL-PITFALL-01: Animation vs. drag state conflict** - `animateFloatAsState` and direct drag writes competing for the same `sizePx` cause oscillation or snap-back. Mitigation: split intent-state (written by drag) from display-state (animated, read-only in layout); disable drag while animation is in-flight on either neighbor. Validate in the mandatory spike.

2. **PNL-PITFALL-04: N-section cascading clamp crash** - With 3+ sections, dragging past the point where remaining sections cannot satisfy their combined `minSize` causes `coerceIn(min, max)` to throw `IllegalArgumentException` (same class as PITFALL-B). Mitigation: `clampPanelDividerPx` summing all minSizes above and below the divider; TDD with RED test first.

3. **PITFALL-A carry-forward: `remember(totalPx)` re-key** - Initializing `sizePx` in a `remember(totalPx)` block resets all section sizes on every window resize. Mitigation: `remember { mutableStateListOf(...) }` with no key, always.

4. **FIXSP-01 carry-forward: stale capture in drag lambda** - `aeroDragSplitter` captures `onDrag` once; plain locals in the composable scope are stale after first drag event. Mitigation: read `sizePx[above]` and `sizePx[below]` directly inside the lambda; wrap `totalPx` in `rememberUpdatedState`.

5. **PNL-PITFALL-06: `lastExpandedPx` overflow after window shrink** - Restoring absolute px after the window has shrunk overflows the group. Mitigation: store `lastExpandedFraction` (ratio at collapse time); restore as `lastExpandedFraction * currentAvailableForExpanded`.

6. **PNL-PITFALL-08: Section state re-key on list reorder** - Positional keys in the render loop reset section state when order changes. Mitigation: `key(section.id)` in the render loop; `id: String` on `AeroPanelSection` (see OQ-4).

---

## Implications for Roadmap

This is a **single-phase milestone**. The user confirmed small scope, strictly one phase. All tasks belong to Phase 13. The ordering within the phase is non-negotiable - the spike must come first.

### Phase 13: AeroPanelGroup

**Rationale:** Additive component with no dependence on any not-yet-shipped primitive. All blocking work shipped in prior phases. The only open risk (animation-vs-drag coexistence) is resolved by an upfront spike before any library code is written.

**Delivers:** Fully functional `AeroPanelGroup` + `AeroPanelSection` public API; `PanelDistribution.kt` pure-logic module with unit tests; `PanelGroupDivider` private composable; showcase demo with three-theme sign-off.

**Build order within Phase 13 (non-negotiable):**

| Step | Name | Gate before proceeding |
|------|------|------------------------|
| 1 | Animation-vs-drag spike | Drag is instant; toggle animates 200ms; collapse-then-immediate-drag produces no snap-back or oscillation |
| 2 | Pure logic + TDD (PanelDistribution.kt, PanelGroupLogicTest.kt) | All tests GREEN; covers distributePx, shareTransfer, computeAvailablePx, clampPanelDividerPx, lastExpandedFraction restore |
| 3 | Layout skeleton (no animation, no drag) | Window resize redistributes heights correctly; collapse/expand toggle changes heights correctly |
| 4 | Collapse/expand animation | 200ms FastOutSlowInEasing; concurrent animations on multiple sections do not conflict |
| 5 | Drag resize | Instant; no snap-back after window resize during drag |
| 6 | Controlled expansion path + KDoc | Both branches present; KDoc has do-not-collapse-to-one-branch comment |
| 7 | Aero visual polish | glassPanel header, CaretRight caret, headerActions slot, grip dots on divider; three-theme check |
| 8 | Showcase demo + sign-off | LayoutSection.kt updated; three-theme visual sign-off completed |

**Features addressed:** All P1 table-stakes plus the three cheap differentiators (headerActions, leadingIcon, unit tests).

**Pitfalls to address per step:**

- Step 1: PNL-PITFALL-01 (animation-vs-drag), PNL-PITFALL-08 (remember discipline)
- Step 2: PNL-PITFALL-04 (N-section clamp), PNL-PITFALL-02 (float drift), PNL-PITFALL-05 (share-transfer rounding), PNL-PITFALL-06 (lastExpandedFraction), PNL-PITFALL-07 (divider count off-by-one), PITFALL-A + PITFALL-B carry-forwards
- Step 3: PNL-PITFALL-08 (key(section.id) in render loop), PNL-PITFALL-09 (no drag divider on collapsed boundary), PNL-PITFALL-10 (no animateContentSize), PNL-PITFALL-11 (weight only on last expanded section), PITFALL-03 carry-forward (no detectDragGestures), FIXSP-01 carry-forward (pointerInput(Unit) + rememberUpdatedState)
- Steps 3+: PNL-PITFALL-03 (mid-drag collapse race - dragging flag guard)

### Research Flags

Phase 13 does NOT need `/gsd:research-phase` - all patterns are direct ports of shipped code with verified in-repo precedents.

One spike is needed before requirements are written: Step 1 (animation-vs-drag coexistence). This is an empirical question answered by a throwaway proof-of-concept composable in the showcase, not external documentation.

Before requirements finalization: resolve OQ-1 through OQ-4 with the user. These are API-shape decisions that do not block the spike or the pure-logic step.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All APIs verified against in-repo source files; no new dependencies |
| Features | HIGH | Cross-referenced VS Code, react-resizable-panels v4, JetBrains tool windows, and existing codebase; all decisions locked |
| Architecture | HIGH (one MEDIUM gap) | All patterns are direct ports of shipped code; the MEDIUM item is animation-vs-drag coexistence, architecturally sound but unverified empirically in this codebase |
| Pitfalls | HIGH | Grounded in actual shipped bugs (FIXSP-01), actual source code (SplitClamp.kt, AeroSplitPane.kt), and Kotlin stdlib semantics (`coerceIn` throws on inverted range) |

**Overall confidence:** HIGH

### Gaps to Address

- **Animation-vs-drag coexistence:** Architecture is sound but must be confirmed empirically in the Step 1 spike before Step 3 starts. If the spike fails, the fallback (separate intent-state and display-state; disable drag during animation) is documented in PNL-PITFALL-01 and adds approximately 20 lines of complexity.
- **OQ-1 through OQ-4:** API design questions that need a short conversation with the user before REQ-IDs are written. They do not block the spike or the pure-logic step.

---

## Sources

### Primary (HIGH confidence - in-repo source files, read 2026-06-22)

- `library/.../AeroSplitPane.kt` - BoxWithConstraints pattern, rememberUpdatedState(totalPx), aeroDragSplitter usage, clampDividerPx, weight(1f) second-pane, fraction-state rationale
- `library/.../AeroAccordion.kt` - hybrid controlled/uncontrolled API, animateFloatAsState caret, tween(160ms), animateContentSize (negative precedent for AeroPanelGroup)
- `library/.../AeroSidebar.kt` - animateDpAsState tween(200ms, FastOutSlowInEasing); the 200ms animation spec precedent
- `library/.../AeroDragSplitter.kt` - aeroDragSplitter signature, Orientation.Vertical, awaitPointerEventScope, PITFALL-03 documentation
- `library/.../SplitClamp.kt` - clampDividerPx, coerceAtLeast guard
- `library/.../SplitClampTest.kt` and `AccordionToggleTest.kt` - TDD pattern templates
- `gradle/libs.versions.toml` - confirmed Kotlin 2.1.21, CMP 1.7.3
- `library/build.gradle.kts` - confirmed all api(...) declarations; no new dependencies required
- `.planning/PROJECT.md` - locked decisions, v2.0.2 spec, spike mandate, PITFALL cross-references

### Secondary (HIGH confidence - design references)

- VS Code Left Sidebar UX (collapse to strip, drag between expanded, no drag on collapsed boundary)
- react-resizable-panels v4 (collapsible, minSize, onLayoutChange after drag-end, proportional redistribution)
- JetBrains tool windows (per-window size memory, drag sash between adjacent tool windows)

### Tertiary (MEDIUM confidence - changelog/web)

- react-resizable-panels v4 CHANGELOG - onCollapse/onExpand removed in v4 in favor of onResize; onLayoutChanged fires after resize completes

---

*Research completed: 2026-06-22*
*Ready for roadmap: yes - single phase; resolve OQ-1 through OQ-4 before writing REQ-IDs*
