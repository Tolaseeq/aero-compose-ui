---
phase: 9
slug: data
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-18
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + `kotlin.test` (existing project convention — see AeroRangeSliderTest.kt, AeroColorMathTest.kt) |
| **Config file** | none — standard Gradle `testImplementation` wiring already in `:library` |
| **Quick run command** | `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*"` |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30–60 seconds (pure-logic unit tests; no Compose UI tests) |

Note: the pure-logic seam is deliberate. SortState, SelectionLogic, ColumnWidth, TreeFlatten, NodeState are extracted as plain Kotlin functions so they unit-test without a Compose runtime. Composables are verified via `./gradlew :library:compileKotlin` + grep-gates (the project's established pattern for composable surface — see Phase 8 plans).

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:compileKotlin` (compile gate) + the task's named test if it created one.
- **After every plan wave:** Run `./gradlew :library:test --tests "com.mordred.aero.components.datatable.*"`
- **Before `/gsd:verify-work`:** Full suite (`./gradlew :library:test`) must be green.
- **Max feedback latency:** ~60 seconds.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 09-01-01 | 01 | 1 | DATA-01 (enabler) | compile | `./gradlew :library:compileKotlin` | n/a | ⬜ pending |
| 09-01-02 | 01 | 1 | DATA-02/03/04 (types) | compile | `./gradlew :library:compileKotlin` | n/a | ⬜ pending |
| 09-01-03 | 01 | 1 | DATA-02 | unit | `./gradlew :library:test --tests "*SortStateTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 09-01-04 | 01 | 1 | DATA-03 | unit | `./gradlew :library:test --tests "*SelectionLogicTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 09-01-05 | 01 | 1 | DATA-04 | unit | `./gradlew :library:test --tests "*ColumnWidthTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 09-02-01 | 02 | 2 | DATA-03 | compile + grep | `./gradlew :library:compileKotlin` + PITFALL-10 grep | n/a | ⬜ pending |
| 09-02-02 | 02 | 2 | DATA-02/04 | compile + grep | `./gradlew :library:compileKotlin` + PITFALL-03 grep | n/a | ⬜ pending |
| 09-02-03 | 02 | 2 | DATA-01/02/03/04 | compile + grep | `./gradlew :library:compileKotlin` + PITFALL-01 grep | n/a | ⬜ pending |
| 09-03-01 | 03 | 2 | DATA-05 | unit | `./gradlew :library:test --tests "*TreeFlattenTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 09-03-02 | 03 | 2 | DATA-06 | unit | `./gradlew :library:test --tests "*NodeStateTest"` | ❌ W0 (this task creates it) | ⬜ pending |
| 09-03-03 | 03 | 2 | DATA-05 | compile + grep | `./gradlew :library:compileKotlin` + PITFALL-NEW-01 grep | n/a | ⬜ pending |
| 09-03-04 | 03 | 2 | DATA-05/06 | compile + grep | `./gradlew :library:compileKotlin` + PITFALL-01/05 grep | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

The Wave 0 test files are authored inside the same TDD tasks that produce the logic (RED then GREEN within the task), per the project's Phase 7/8 precedent — each pure-logic task writes its test alongside the function. No separate Wave 0 plan is required because the tested logic and its tests are co-created and have no cross-plan dependency.

---

## Wave 0 Requirements

The following test files are created by their owning TDD tasks (not a separate scaffold plan):

- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/SortStateTest.kt` — DATA-02 sort state machine (asc→desc→none cycle; single column active) — created by 09-01 Task 3
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/SelectionLogicTest.kt` — DATA-03 (Ctrl-toggle, Shift-range from anchor, key stability across re-sorted order) — created by 09-01 Task 4
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/ColumnWidthTest.kt` — DATA-04 (Fixed/Weight resolution, minWidth clamp, no 0dp collapse) + AeroTableColumn type-reachability — created by 09-01 Task 5
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/TreeFlattenTest.kt` — DATA-05 (flatten depth 0/1/2, collapse hides children, empty children) — created by 09-03 Task 1
- [ ] `library/src/test/kotlin/com/mordred/aero/components/datatable/NodeStateTest.kt` — DATA-06 (childrenLoaded guard: fires once on first expand, NOT on collapse/re-expand/scroll-back) — created by 09-03 Task 2

Framework already installed (JUnit 4 + kotlin.test on `:library` test classpath). No install task needed.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Observable fps with 1000 rows (no frame drop) | DATA-01 | True fps measurement needs a running window; not unit-testable | Deferred to Phase 11 DataSection (SHW-07 / SHW-10 16-item checklist): scroll a 1000-row table and confirm no jank. Unit/grep proxy in Phase 9: PITFALL-01 grep (no AeroScrollArea) + `items(key=...)` present + own LazyColumn confirm virtualization is wired. |
| Drag-resize responds on first mouse pixel | DATA-04 | Pointer-event timing needs a live pointer; the touchSlop defense lives in `aeroDragSplitter` (Phase 7, already smoke-tested) | Phase 9 proxy: grep-gate `detectDragGestures` == 0 + `aeroDragSplitter(Orientation.Horizontal)` present. Live eyes-on in Phase 11. |
| Four-state row color readable in AeroDark | DATA-03 / PITFALL-10 | Contrast judgment is visual | Phase 9 proxy: grep `borderSelected.copy(alpha = 0.15f)` present and `primary.copy(0.2f)` == 0. Eyes-on in Phase 11 AeroDark review. |
| Caret rotation + expand animation smoothness | DATA-05 | Animation feel is visual | Eyes-on in Phase 11. Phase 9 proxy: `animateFloatAsState` + `graphicsLayer { rotationZ }` present. |

All non-visual phase logic (sort/selection/column-width/tree-flatten/node-state) has automated unit coverage. The four manual items above are inherently visual/timing and are formally signed off by the Phase 11 16-item "looks done but isn't" checklist; Phase 9 enforces them via grep-gates and compile checks.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (every task has a `./gradlew` command; pure-logic tasks create + run their own tests)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (every task runs at least compileKotlin)
- [x] Wave 0 covers all MISSING references (5 test files mapped to their owning tasks)
- [x] No watch-mode flags (all commands are one-shot `./gradlew`)
- [x] Feedback latency < 60s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-18
