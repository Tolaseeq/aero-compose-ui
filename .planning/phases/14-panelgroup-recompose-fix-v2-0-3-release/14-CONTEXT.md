# Phase 14: PanelGroup Recompose Fix + v2.0.3 Release - Context

**Gathered:** 2026-06-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Eliminate horizontal-controlled section duplication in `AeroPanelGroup` when a section's content recomposes during an active divider drag, by removing the in-composition write to observed snapshot-state (`expandedState`) that the same `BoxWithConstraints` pass reads. Add a minimal showcase repro, regression-guard the vertical + uncontrolled paths byte-identical, then release v2.0.3 on JitPack.

Single-phase patch milestone (all 8 v1 requirements). Zero breaking changes to the v2.x API, zero new dependencies, Compose stays 1.7.3. New PanelGroup capabilities (drag-to-reorder, nested first-class API, keyboard resize) stay deferred.

</domain>

<decisions>
## Implementation Decisions

### Fix mechanism (LOCKED upstream — RCMP-02/03, not re-litigated)
- Boolean expansion array for size-math (`computeAvailablePx` / `distributePx`) is computed directly from `isExpanded(sec)` each composition, NOT read from the written `expandedState` list ([AeroPanelGroup.kt:346](../../../library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt#L346)).
- `expandedState` sync (still needed for the uncontrolled path / animations) is moved out of the composition body into a `SideEffect` ([AeroPanelGroup.kt:331-335](../../../library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt#L331-L335)).
- The seed block ([AeroPanelGroup.kt:297-320](../../../library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt#L297-L320)) must not mutate read-in-same-composition state.
- **Invariant:** no `BoxWithConstraints` composition pass writes to `expandedState`/`sizePx` that it also reads.

### Showcase repro design (RCMP-04)
- **Recompose driver:** a live counter — a `LaunchedEffect` ticks a `mutableStateOf` (~16–100ms cadence); the content of ONE section reads that value, so the section recomposes on its own during a divider drag with no user action required. This is the most reliable way to surface the ×N duplication.
- **Placement:** `LayoutSection.kt`, a horizontal **controlled** `AeroPanelGroup` (`expandedKeys` + `onExpandedChange`).
- **Permanence:** the repro stays as a **permanent, labeled regression demo** (not throwaway) — signed with a short caption that explains it guards the recompose-during-drag fix.

### Fix verification (RCMP-01)
- **Primary:** manual drag in the showcase repro — eyes-on. Before the fix: N declared sections render as ×N header strips (3→9). After: exactly N. Confirm while the live counter is ticking and the divider is being dragged.
- **Static:** verify the invariant statically (success criterion #2) — grep/inspection confirming no in-composition write to `expandedState`/`sizePx` in the read path.
- **No Compose UI / instrumented test** — the project has no UI-test infra; mirrors the v2.0.1 / Phase 13 patch precedent (visual sign-off + pure-logic JVM tests). The 12 `PanelGroupLogicTest` JVM tests stay GREEN as the logic gate.

### Regression guard — SideEffect safety (REG-01)
- **Explicit invariant captured:** the moved-to-`SideEffect` `expandedState` sync exists ONLY to keep the uncontrolled path / animations consistent — it MUST NOT call `onLayoutChange`. `onLayoutChange` keeps firing on **drag-end + toggle only**, never per frame and never from the `SideEffect`.
- **Verification:** manual — drag the divider with the live counter ticking and confirm `onLayoutChange` does not fire per frame (counter/log or eyes-on), plus a grep confirming `onLayoutChange` is not called from inside the `SideEffect`.
- Vertical path (Phase 13) and uncontrolled mode stay byte-identical: drag-resize, collapse/expand animations (`snap()` during drag, `tween(200ms, FastOutSlowInEasing)` after), window-resize proportion preservation — no visual or behavioral change.

### Release & docs (REL-01/02)
- **Full milestone cycle:** bump `2.0.2`→`2.0.3` in `build.gradle.kts` (version bump precedes the tag, per standing preference) → land the fix → tag `v2.0.3` → push to `Tolaseeq/aero-compose-ui` → confirm JitPack resolves `com.github.Tolaseeq:aero-compose-ui:2.0.3`.
- **Docs that ship with the release:** update `MILESTONES.md`, `RETROSPECTIVE.md`, and add a v2.0.3 row to the PROJECT.md "Key Decisions" table; mark Phase 14 done in ROADMAP.md / STATE.md.
- **Archive:** run `/gsd:complete-milestone` after the tag pushes to archive v2.0.3 — consistent with how v2.0.1 / v2.0.2 were closed.

### Claude's Discretion
- Exact `SideEffect` body shape and where the seed block's expansion derivation lives.
- Counter cadence and the precise caption wording / layout of the repro demo.
- Wording of the new PROJECT.md Key Decisions row and RETROSPECTIVE lessons.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase requirements & scope
- `.planning/REQUIREMENTS.md` — RCMP-01..04, REG-01..02, REL-01..02 with exact acceptance criteria (the source of truth for the fix invariant and regression guarantees).
- `.planning/ROADMAP.md` §"Phase 14" — goal, depends-on (Phase 13.1), 5 success criteria (esp. #2 statically-verifiable invariant, #4 repro, #5 release).
- `.planning/STATE.md` §"Phase 14 Scope" + §"Blockers/Concerns" — target file/line map and the SideEffect/onLayoutChange watch-out.

### Target code
- `library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt` — the internal `AeroPanelGroupImpl(orientation)` core. Edit sites: seed block ~297-320, sync loop ~331-335, `expandedArr` read ~346. The `isExpanded(sec)` helper (~327-329) is the source the size-math should read each composition.
- `library/src/main/kotlin/com/mordred/aero/components/layout/PanelDistribution.kt` — pure size-math (`computeAvailablePx`, `distributePx`); unchanged by the fix but consumes the boolean array.
- `library/src/test/.../PanelGroupLogicTest.kt` — the 12 JVM logic tests that must stay GREEN (REG-02 gate).
- `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt` — where the RCMP-04 repro demo is added.

### Release infra
- `build.gradle.kts:4` — `version = "2.0.2"` → `2.0.3` (REL-01).
- `jitpack.yml` — existing JitPack build config (no change expected; release path validated through v2.0.2).

### Locked patterns to preserve (PROJECT.md "Key Decisions" + STATE.md "v2.0.2 Locked Decisions")
- Pattern 3 (PNL-PITFALL-01): `animateFloatAsState` reads target-only, drag writes `sizePx` directly, `isDragging`→`snap()` during gesture / `tween(200ms, FastOutSlowInEasing)` after.
- Header reservation: `availableForExpanded = totalPx − sectionCount*headerPx − activeDividers*thickness`, one header per section.
- Public-wrapper + internal-core `AeroPanelGroupImpl(orientation)`; only 3 orientation branch points differ.
- `onLayoutChange` fires on drag-end + toggle only.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `isExpanded(sec: AeroPanelSectionConfig): Boolean` ([AeroPanelGroup.kt:327-329](../../../library/src/main/kotlin/com/mordred/aero/components/layout/AeroPanelGroup.kt#L327)) — already the effective hybrid controlled/uncontrolled expansion source; the fix points size-math at this directly instead of the mirrored `expandedState`.
- `expandedState` / `sizePx` / `lastExpandedFractionState` `mutableStateListOf` (~287-289) — seeded once on structure change; the fix keeps `expandedState` for the uncontrolled/animation path but stops the in-composition write.
- `PanelDistribution.kt` pure functions consume a `BooleanArray` — the fix changes only how that array is produced (`isExpanded()` per composition), not the math.

### Established Patterns
- Hybrid controlled/uncontrolled ownership (PNL-08, AeroAccordion convention): `val controlled = onExpandedChange != null`; both branches intentional — the fix must not collapse them.
- Patch-release precedent (v2.0.1, Phase 13): direct edit to existing code, pure-logic JVM tests + manual three-theme/visual sign-off as the gate, no Compose UI tests.
- `SideEffect` (not `LaunchedEffect`) for per-composition state sync is already the locked pattern elsewhere — `AeroSidebar` uses `SideEffect` to sync `animateDpAsState` to `state.widthState` each frame (STATE.md, Phase 10).

### Integration Points
- Fix is fully contained in the internal `AeroPanelGroupImpl` core — vertical wrapper, public DSL, and `PanelDistribution.kt` need no edits.
- Showcase repro integrates into the existing `LayoutSection.kt` PanelGroup demos.
- Release touches `build.gradle.kts`, git tags, and JitPack (infra already exists through v2.0.2).

</code_context>

<specifics>
## Specific Ideas

- Repro should reproduce the bug "by itself" — a ticking counter, not a user-driven toggle, so dragging the divider is the only manual action needed to witness ×N before the fix.
- Keep the repro permanent and captioned so it doubles as a living regression guard, consistent with how the library's showcase demos serve as the visual sign-off surface.

</specifics>

<deferred>
## Deferred Ideas

- AeroPanelGroup drag-to-reorder sections (PNL-REORDER-01) — future feature milestone.
- Nested `AeroPanelGroup` as first-class API (PNL-NEST-01) — future feature milestone.
- Keyboard resize of dividers (PNL-KBD-01) — future feature milestone.
- AeroDropdown popup-offset regression (DROP-FIX-01, v1.0 carry-over) — separate gap-closure, explicitly out of v2.0.3.

</deferred>

---

*Phase: 14-panelgroup-recompose-fix-v2-0-3-release*
*Context gathered: 2026-06-25*
