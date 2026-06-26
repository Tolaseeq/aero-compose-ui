# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.1 — Icon System

**Shipped:** 2026-04-30
**Phases:** 3 (4–6) | **Plans:** 11 | **Sessions:** ~3 (Phase 4, Phase 5, Phase 6 — single-day execution 2026-04-29)

### What Was Built
- 138 typed `AeroIcons` `ImageVector` constants (port of Phosphor Regular, lazy backing-property pattern, `explicitApi()`-clean) — generated via Valkyrie 1.1.1, committed to `library/src/main/kotlin/com/mordred/aero/icons/internal/`
- Migrations across 11 components: `AeroCheckbox`, `AeroDropdown`, `AeroNumberSpinner`, `AeroContextMenu`, `AeroToastHost`, `AeroNotificationBanner`, `AeroAlertKind`, `AeroBannerKind`, `AeroSearchField`, `AeroPasswordField`, `AeroTitleBar` — every text glyph and `Icons.Outlined.*` reference replaced with `Icon(AeroIcons.*)`
- `compose.materialIconsExtended` removed from `library/build.gradle.kts`; compileClasspath shed ~36 MB; tests rewritten to assert `AeroIcons.*` instances
- Showcase `IconsSection.kt`: 138-entry `LazyVerticalGrid` with live `AeroSearchField` filter, glassSurface cells, click-to-copy + toast feedback
- Three-theme visual sign-off (AeroBlue / AeroDark / Classic) — formal milestone gate, all PASS

### What Worked
- **Wave-ordered execution in Phase 5** (Wave 1 internal swaps → Wave 2 Canvas deletions → Wave 3 TitleBar API → Wave 4 test rewrites → Wave 5 dep removal). Every wave landed without broken-build promotions; CLN-01 acted as a clean gate before CLN-02.
- **Spike-then-batch in Phase 4** (P01 with 7 icons + Valkyrie spike + facade authoring + first `compileKotlin` pass) caught two surprises before mass-generation: (1) `--explicit-mode=true` flag form, (2) `defaultWidth=256`→`24.dp` post-generation fix. Spike paid for itself in P02 batch (131 icons) which had no surprises.
- **Verbatim Phosphor naming** (`X` not `Close`, `CaretDown` not `ChevronDown`) — locked at planning time via KDoc naming-table; saved every migration plan from re-debating naming.
- **Generated `.kt` committed to source** (not generated at build time) — every reviewer can see exact `ImageVector` output, no Valkyrie version drift, and `git blame` works.
- **Three-theme visual checkpoint as the milestone sign-off gate** — explicit eye-on-screen verification in AeroBlue/AeroDark/Classic produced the v1.1 approval record without remote-dependent acceptance criteria.

### What Was Inefficient
- **AeroNumberSpinner sub-pixel pitfall surfaced late** — Phase 5 P01 caught it inline (raised button slot to 14dp), but the risk should have been flagged in Phase 4 RESEARCH (Phosphor stroke at 10dp ≈ 0.63dp at 96 DPI is determinable from first principles). Cost: one inline visual checkpoint mid-Wave-1 instead of a clean wave.
- **138 explicit `internal.*` extension property imports per migration file** (no wildcard) — by-design tradeoff (explicit is what `internal` package conventions force) but every migration plan repeats the import block. No refactor planned for v1.x but it's the pattern most likely to be questioned later.
- **v1.0 was never formally archived** — `/gsd:complete-milestone` not run for v1.0, so v1.1 archive (`milestones/v1.1-ROADMAP.md`) doubles as the v1.0 historical record. Future milestones should run `/gsd:complete-milestone` immediately on milestone close, not retroactively.
- **Phase 5 P05 has 6 summaries against 5 plans** — a wave restructuring left an extra summary file. Cosmetic but it tripped the progress-percent calculation (`97%` displayed when work was `100%` complete).

### Patterns Established
- **Spike → batch for code generation** — never batch-generate 100+ files without a 5–10 file spike + first `compileKotlin` pass first.
- **Wave ordering for dep removal** — always have a "test-rewrite gate" wave between API migrations and the actual dependency line-deletion (`materialIconsExtended` removal would have produced unfixable test failures without CLN-01 first).
- **Visual checkpoint as final phase plan** — Phase 6 P03 was a pure visual checkpoint with no code; treating that as a first-class plan (with its own `PLAN.md`/`SUMMARY.md`) preserves the sign-off record cleanly.
- **Locked-decision blocks in PROJECT.md "Key Decisions" with `Outcome` column** — every locked choice now has a `✓ Good` / `⚠ Revisit` mark with rationale; future maintainers don't need to re-litigate.
- **`tools/<vendor>/` for vendored upstream sources with a `.pin` file recording exact SHA** — establishes a reproducibility convention for any future vendored asset (icons, fonts, etc.).

### Key Lessons
1. **Compute pixel-stroke risks at planning time, not at execution.** Phosphor stroke = `dp × (16 / 256)` is deterministic; sub-pixel pitfalls can be enumerated in RESEARCH for every component using the icons before any plan is written.
2. **`/gsd:complete-milestone` on milestone close, not retroactively.** The cost of skipping it (informal v1.0) is that the next milestone has to absorb the v1.0 history, leading to misleading archive titles.
3. **Phosphor mixed-fill icons retain `fill=SolidColor(Color.Black)` by design.** `ColorFilter.tint` replaces all colors at render time via `Icon(tint=...)`; do not "clean up" the fills in generated source — that breaks the tint contract.
4. **Wave 4 (test rewrites) MUST precede Wave 5 (dep removal).** Tests asserting `Icons.Outlined.*` instances become uncompilable the moment `materialIconsExtended` is removed; ordering is not optional.
5. **`internal/` package + extension properties is the right shape for typed icon sets at this scale.** The facade `object AeroIcons` stays empty; 138 extension properties live in per-icon files, autoloaded via Kotlin extension resolution. No wildcard imports — every migration file is explicit and grep-discoverable.

### Cost Observations
- Model mix: opus for planning (`gsd:plan-phase`, `gsd:research-phase`), sonnet for execution (per `model_profile: balanced`)
- Sessions: ~3 (one per phase), single-day push 2026-04-29
- Notable: Phase 4 P02 (138 generated icons) was the largest single plan in either milestone (139 files, 35 min) — Valkyrie batch + post-generation `defaultWidth` fix dominated the time, not Compose code

---

## Milestone: v2.0 — Stateful + Layout

**Shipped:** 2026-06-18
**Phases:** 5 (7–11) | **Plans:** 27 | **Sessions:** concentrated execution; git range `2a4eaae`→`4d902cb` (145 commits, 47 `feat`), 152 files changed (+27,406 / −2,285)

### What Was Built
- **Internal foundation (Phase 7):** `AeroCalendarGrid`, `AeroColorMath`, `AeroHsvColorSquare` + `AeroHueSlider`, `Modifier.aeroDragSplitter`, `AeroStepIndicator`, `AeroCalendarPositionProvider` — shared primitives behind 27 JUnit tests, no public API.
- **Pickers (Phase 8):** `AeroRangeSlider`, `AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker`, `AeroColorPicker`; `kotlinx-datetime:0.6.2` added.
- **Data (Phase 9):** `AeroDataTable` (virtualized LazyColumn, 3-position sort, `Set<RowKey>` Ctrl/Shift selection, drag-resize columns) + `AeroTreeView` (once-only lazy `onExpand` via `SnapshotStateMap`).
- **Layout (Phase 10):** `AeroAccordion` (single/multi), `AeroSplitPane` (clamped, 8dp hit-area), `AeroSidebar` (3-mode animated DSL), `AeroStepperWizard` (commit-gate validation, Back-state preserved).
- **Showcase + sign-off (Phase 11):** DataSection / PickersSection / LayoutSection wired in, RangeSection extended; 16-item × 3-theme silent-failure checklist (48 cells) as the formal milestone gate.

### What Worked
- **Enabling-phase-first (Phase 7).** Building every cross-cutting primitive (calendar, color math, drag, step indicator, popup positioner) once, gated by 27 green unit tests *before* any public component, meant Phases 8/9/10 never re-invented drag logic or color math with divergent bugs. The audit confirmed zero local re-implementations — every Phase 7 primitive is imported and called by its intended consumer.
- **Pure-logic-then-Compose.** Stateful behavior was extracted into pure functions and sealed state machines unit-tested without the Compose runtime: `nextRangeState` (range commit gate), `toggleNode` (once-only expand guard), `resolveColumnWidths`, `assembleTime`, `thumbToDrawFirst`. The hardest correctness pitfalls (PITFALL-06 partial-range leak, PITFALL-05 lazy-callback repeat, PITFALL-15 HSV drift) were locked at the pure-function layer.
- **Front-loaded PITFALLS.md catalog.** Risks were enumerated at planning time with explicit resolutions per pitfall (touchSlop, calendar popup width, HSV drift, selection-by-index, LazyColumn virtualization). Phase notes carried the resolution into each plan, so executors never rediscovered a known trap mid-phase.
- **16-item × 3-theme silent-failure checklist as a hard gate.** It earned its keep: the first pass FAILED with 16 real defects in components that compiled and "looked done." A checklist tied to specific failure modes (drag-on-first-pixel, AeroDark disabled-cell contrast, selection-survives-sort) caught what code review could not.
- **Grep-gates for banned APIs.** `transparent = true`, `AeroScrollArea`-inside-table, `detectDragGestures`, `stickyHeader` each had a zero-match grep gate per plan — mechanical, fast, and impossible to forget.
- **Hybrid controlled/uncontrolled state** (`onExpandedChange`/`onSortChange` null = uncontrolled internal state, non-null = controlled pure renderer) reused across `AeroDataTable` and `AeroAccordion` — one pattern, two components.

### What Was Inefficient
- **All visual verification deferred to Phase 11 → 16-defect batch.** Phases 8/9/10 shipped on compile + unit tests; the first eyes-on pass at sign-off surfaced 16 defects at once, requiring six gap-closure plans (11-06…11-11) and a full re-verification. Many (cell padding, full-cell header sort target, whole-row tree toggle, compact triggers, wizard bounded height) are per-component visual issues that a targeted checkpoint at the end of each component phase would have caught incrementally instead of as an end-of-milestone debug surge.
- **The shared `aeroDragSplitter` delta bug (F3/F15) was found late.** Root cause — accumulated delta is unstable when the hit-area Box relocates between frames — wasn't exposed until the showcase exercised real drag. Fixing it once (`positionChange()` single-frame intra-event delta) corrected all consumers, but a Phase 7 drag *integration* smoke test (not just a drag-start smoke test) would have found it before three components depended on the broken behavior.
- **Documentation hygiene drift across phases.** Seven requirements (PICK-03, DATA-05/06, LAYO-03/04/08/09) were missing from their SUMMARY `requirements-completed` frontmatter; Phase 8 verification was filed as the glob-invisible `08-VERIFICATION-REPORT.md`; Phase 10 VERIFICATION sat at `human_needed`. None were coverage gaps (all verified satisfied), but the audit had to manually cross-reference three sources to prove it.
- **Nyquist validation contract incomplete for Phases 7, 8, 11** despite heavy JUnit coverage (27 + 65 tests) — the formal `nyquist_compliant` flag was never set, leaving the milestone audit at `partial` on that axis.

### Patterns Established
- **Enabling-phase-first** — when 2+ components need the same non-trivial mechanic (drag, calendar, color math), build it as an internal-only phase gated by unit tests before any public work.
- **Pure-function + sealed-state-machine core, Compose as a thin renderer** — make the correctness-critical logic testable without a Compose runtime; the state machine's single commit point is the only callback call site.
- **Silent-failure checklist as the milestone sign-off gate** — a fixed list of "looks done but isn't" failure modes, verified eyes-on per theme, with FAIL blocking sign-off.
- **Zero-match grep-gates per plan** for every banned API/anti-pattern (`transparent=true`, `AeroScrollArea` in tables, `detectDragGestures`, `stickyHeader`).
- **Hybrid controlled/uncontrolled component API** — `onXChange: ((...) -> Unit)? = null`; null drives internal `mutableStateOf`, non-null makes the component a pure controlled renderer.

### Key Lessons
1. **Distribute visual verification per component-phase, not all at the showcase phase.** 16 defects at the end is a batch-debug tax; the same checklist applied to each component phase's own mini-showcase would have amortized the cost and isolated root causes earlier.
2. **Frame-stable drag delta is mandatory when the hit-area can move between frames.** Use `positionChange()` (single-frame intra-event) — accumulated/`positionChanged` deltas drift when the splitter's hit-area Box relocates after a resize (F3/F15 root cause).
3. **`pointerInput` lambdas capture stale state — wrap mutable reads in `rememberUpdatedState`.** The RangeSlider live-value bug (F9) was a stale captured `value` inside the drag loop.
4. **Fill SUMMARY `requirements-completed` frontmatter at plan close.** Seven reqs went unrecorded; the milestone audit paid for it in manual cross-referencing. The frontmatter is the machine-readable coverage source — leaving it stale forces a slower human verification path.
5. **For a published library, `implementation` vs `api` is a real leak, not a convention detail.** Picker public signatures expose `kotlinx.datetime.LocalDate/Time/DateTime`; declared `implementation`, that type leaks transitively at publish. The all-deps-`implementation` repo convention masks it in-repo — must be revisited at the POM/publish step.

### Cost Observations
- Model mix: opus for planning/research, sonnet for execution (per `model_profile: balanced`); unchanged from v1.1.
- Plans were small and numerous — 27 plans, most 2–6 min execution per the STATE metrics table; Phase 11 gap-closure (11-06 ~12 min, 11-11 ~15 min) dominated wall-clock.
- Notable: the cost concentrated in *verification and gap-closure* (Phase 11: 11 plans for what was scoped as showcase wiring) rather than in component implementation, which went smoothly on the Phase 7 foundation.

---

## Milestone: v2.0.1 — Picker & SplitPane Fixes

**Shipped:** 2026-06-22
**Phases:** 1 (12) | **Plans:** 4 | **Sessions:** single-day push (~2h20m execution, 14:05→16:23 +0300); git range `b684382`→`2c6202c` (25 commits, 5 `feat` / 3 `fix`), 9 code files (+520 / −14)

### What Was Built
- **Fix A — seconds in trigger (FIXDT-01/02):** `internal fun formatAeroDateTime(ldt, showSeconds)` single-source helper + nullable-formatter dispatch in `AeroDateTimePicker`; seconds now render, custom formatters preserved.
- **Fix B — nested SplitPane freeze (FIXSP-01..04):** fraction-based divider state (no `remember(totalPx)` re-key) + `clampDividerPx` `coerceAtLeast` guard against inverted-range throw; TDD RED→GREEN.
- **New `AeroDateTimeRangePicker` (DTR-01..08):** Apply-gate dual-calendar datetime range picker reusing `AeroDateRangeState`/`nextRangeState` verbatim; `orderDateTimeRange` same-day swap; four `remember(expanded)` no-leak blocks.
- **Showcase + sign-off (SHW-11..14):** range-picker live label, `showSeconds` contrast pair, nested 3-pane SplitPane demo; three-theme sign-off PASSED; kotlinx-datetime doc note corrected.

### What Worked
- **Verbatim reuse over re-derivation.** `AeroDateRangeState` + `nextRangeState` were reused unchanged for the new range picker (no generic refactor); `orderDateTimeRange` leaned on `LocalDateTime` `Comparable` directly (no Instant/timezone conversion). The new component inherited `formatAeroDateTime` from Fix A rather than re-implementing trigger formatting — so PITFALL-H could not recur.
- **TDD RED-before-GREEN for the pure clamp helper.** The inverted-range test (`clampInvertedRangeDoesNotThrow`) was committed failing (`38e0de6`) before the guard (`f4de00f`) — separate commits prove the test actually catches the bug, not just documents the fix.
- **A documented prior lesson caught the regression fast.** When the fraction-state rewrite snapped the inner splitter back during sign-off, it was immediately recognized as the v2.0 lesson #3 class (stale captured state in `pointerInput`, same root as the AeroRangeSlider F9 bug) and fixed with the established `rememberUpdatedState` pattern.
- **Single-phase patch milestone shipped on a verification-only gate.** Phase 12's `12-VERIFICATION.md` passed 18/18 (requirements + integration + showcase sign-off); a separate `/gsd:audit-milestone` pass would have been redundant for one phase.

### What Was Inefficient
- **The fraction-state rewrite re-introduced exactly the stale-capture bug v2.0 lesson #3 warned about.** The lesson was already in this retrospective, yet the 12-02 drag loop still captured `dividerFraction` from the lambda. It wasn't caught until the 12-04 three-theme sign-off — one phase later. Applying the `rememberUpdatedState` pattern *while writing* the fraction-state drag loop would have avoided the regression and the extra fix commit (`7f38c0c`) entirely. **Documented lessons need to be applied at write-time, not merely available for diagnosis.**
- **No nested-drag integration check at 12-02 close.** 12-02 verified the pure clamp helper (unit) and single-level behaviour but had no nested-topology smoke check — the exact scenario the milestone existed to fix. The regression therefore surfaced at the visual gate rather than at the fix's own plan boundary (a smaller echo of the v2.0 "defer-all-visual" tax).

### Patterns Established
- **Nullable-formatter dispatch** (`formatter: ((T) -> String)? = null` + body-level `?:` fallback to a `showSeconds`-aware default) — avoids the PITFALL-H capture trap; now the convention for both datetime pickers.
- **Fraction-as-stable-coordinate** for any resizable divider — store the fraction, derive px each recompose; survives `totalPx` changes without reset. Pair it with a live-state read (`rememberUpdatedState`) in the drag loop.
- **Dual Apply-gate picker** — two endpoints sharing the single-commit-point discipline of `AeroDateTimePicker`, extended over the `AeroDateRangeState` machine.

### Key Lessons
1. **Apply documented lessons at write-time, not just at diagnosis.** v2.0 lesson #3 (`rememberUpdatedState` for `pointerInput` captures) was on the books; the fraction-state rewrite ignored it and regressed FIXSP-01. The retrospective made the fix fast — but the regression was avoidable. A lesson is only paying off when it shapes the code as it's written.
2. **A fix's own plan should verify the topology the fix targets.** FIXSP existed for *nested* SplitPanes; 12-02 verified pure-clamp + single-level only. Put the targeted repro (nested 3-pane drag) in the fixing plan's verification, not just the showcase phase.
3. **Reuse a working state machine verbatim — don't refactor it generic mid-milestone.** Reusing `nextRangeState`/`AeroDateRangeState` unchanged kept the new range picker's correctness inside an already-proven, already-unit-tested boundary.
4. **For a single-phase patch milestone, a passing phase VERIFICATION can substitute for a full milestone audit.** The audit's value (cross-phase integration, E2E) collapses to the phase verification when there's exactly one phase.

### Cost Observations
- Model mix: opus for planning/research, sonnet for execution (`model_profile: balanced`); unchanged from v1.1/v2.0.
- Fastest milestone by far — 4 plans, ~2h20m wall. Fix A / Fix B / new component each ~4 min execution; the cost concentrated in 12-04 (~40 min: showcase wiring + the sign-off round + the regression fix), again confirming that verification/sign-off, not implementation, dominates wall-clock.

---

## Milestone: v2.0.2 — AeroPanelGroup

**Shipped:** 2026-06-23
**Phases:** 2 (13 + 13.1) | **Plans:** 8 (Phase 13: 5, Phase 13.1: 3) | **Sessions:** ~1-day push (2026-06-22 17:15 → 2026-06-23 11:01 +0300); git range `603ae57`→`2d7de8f` (49 commits, 11 `feat` / 7 `fix`), 4 code files (+1,516)

### What Was Built
- **AeroPanelGroup (Phase 13, PNL-01..18):** N-section layout that fills its parent, collapses any section to a ~36dp header strip with neighbors absorbing freed height, drag-resizes between adjacent expanded sections (VS Code Side Bar). Fraction-based size state surviving window resize, hybrid controlled/uncontrolled expansion (`AeroAccordion` pattern), Win7 Aero header (glassPanel, CaretRight 0°→90°, `leadingIcon`, `headerActions`), `collapsible`/`resizable` per-section flags. `PanelDistribution.kt` (8 pure functions) + 12 GREEN JVM unit tests.
- **Horizontal orientation variant (Phase 13.1, PNL-HORIZ-01):** refactored to a public wrapper + internal `AeroPanelGroupImpl(orientation)` core with an additive `orientation: Orientation = Orientation.Vertical` default param. Horizontal branch: `Row` container, `maxWidth` axis, axis-swapped modifiers, rotated bottom-to-top header strip, 0°/180° chevron, E/W drag. Two showcase demos added append-only. Three-theme sign-off APPROVED on both orientations.

### What Worked
- **Mandatory spike resolved the highest risk before any UI code.** PNL-PITFALL-01 (animate-vs-drag two-writer conflict) was the gating unknown; Plan 13-01 was a throwaway spike whose sole job was to confirm Pattern 3 (animation reads a target-only value, drag writes state directly, `isDragging`→`snap()`). It also surfaced three layout-math rules (per-section header reservation, drag-delta scaling into sizePx units, `coerceAtLeast(0f)` guard) that ported straight into the real component — so the production code was written against already-proven mechanics.
- **Pure-logic-then-Compose held again.** `PanelDistribution.kt` with 12 GREEN tests (no Compose runtime) locked all the distribution/clamp/share-transfer/fraction-restore math before the composable existed — the same discipline that paid off in v2.0/v2.0.1, now with the N-section cascading clamp (PITFALL-B `coerceAtLeast`) verified RED→GREEN.
- **Public-wrapper + internal-core made orientation a near-free addition.** Extracting `AeroPanelGroupImpl(orientation)` first (Plan 13.1-01, a pure refactor that left the vertical body and all 12 tests unchanged/GREEN) turned the horizontal variant into three branch points (axis, container, section modifiers) instead of a parallel component. Zero breaking change, zero vertical regression.
- **The three-theme sign-off gate caught real horizontal-only defects.** GAP-1 (rotated vertical-title width/position) and GAP-2 (last-column float-rounding distribution) were invisible in code and surfaced only at the visual gate — exactly the silent-failure class the gate exists for.

### What Was Inefficient
- **The rotated-title approach took three tries at sign-off.** `graphicsLayer`-only and `placeRelativeWithLayer` were both attempted and abandoned before `BoxWithConstraints` + `requiredWidth(maxHeight)` + `rotate(-90f)` landed (GAP-1). Rotation-in-a-fixed-strip is a known-thorny Compose problem; a small spike for the horizontal header strip (mirroring the Phase 13 animate-vs-drag spike) would likely have found the working pattern before the sign-off round rather than during it.
- **Horizontal orientation re-entered as an inserted phase right after shipping the milestone-as-one-phase plan.** v2.0.2 was scoped as a single vertical phase with horizontal explicitly deferred (PNL-HORIZ-01, "out of scope"); it was then pulled back in as Phase 13.1 within the same milestone. The core-extraction refactor made this cheap and clean, but it's a scope boundary that moved mid-milestone — worth noting that the "strictly one phase" framing didn't hold.

### Patterns Established
- **Pattern 3 (two-writer coexistence)** — when a value is both animated and dragged, give the animation a read-only target and the drag the source-of-truth state, and switch the animation spec to `snap()` while a gesture is active (`isDragging` flag set on `awaitFirstDown`, cleared in `try/finally`). The locked answer to "animate vs. drag the same value."
- **Public-wrapper + internal-core for orientation symmetry** — `Impl(orientation, …)` holds all shared layout/state/drag; the public default param adds the new axis without breaking callers. Mirrors `AeroSplitPane`; the template for any orientation-symmetric layout.
- **Rotated fixed-width strip** — `BoxWithConstraints` + `requiredWidth(maxHeight)` + `rotate(-90f)` for bottom-to-top text in a fixed-size strip (supersedes `graphicsLayer`-only and `placeRelativeWithLayer`).
- **Per-section header reservation** — reserve one header height per section (not per collapsed section); `availableForExpanded = totalPx − sectionCount*headerPx − activeDividers*thickness`, guarded `.coerceAtLeast(0f)`.

### Key Lessons
1. **Spike the highest-risk interaction before writing the component — and spike each new thorny rendering primitive too.** The animate-vs-drag spike (13-01) paid off cleanly; the *absence* of a parallel spike for the rotated horizontal header strip is exactly why GAP-1 took three tries at the sign-off gate. If a primitive is known-hard (rotation in a fixed strip), give it its own throwaway spike, don't discover the working approach during sign-off.
2. **Extract the shared core first, then branch.** Doing the pure-refactor extraction (`AeroPanelGroupImpl`) as its own plan — with the vertical body and all unit tests provably unchanged — made the orientation variant a 3-branch-point addition with zero regression. Refactor-then-extend beats building a parallel implementation.
3. **"Strictly one phase" is a soft boundary.** A deferred/out-of-scope feature (horizontal orientation) re-entered the same milestone as an inserted phase. That was the right call (the core extraction made it cheap), but milestone scope framing should expect this and not over-commit to "one phase, never more."
4. **Pure-logic TDD keeps paying off for layout math.** The N-section cascading clamp would have crashed (`coerceIn(min>max)`) under realistic squeeze; catching it RED→GREEN in `PanelGroupLogicTest` — without a Compose runtime — is the fourth milestone running where the hardest defect was a pure function, not a rendering bug.

### Cost Observations
- Model mix: opus for planning/research, sonnet for execution (`model_profile: balanced`); unchanged from v1.1/v2.0/v2.0.1.
- 8 plans over ~1 day wall. Cost concentrated in the two sign-off plans (13-05 vertical, 13.1-03 horizontal) where the visual gate + GAP fixes lived — again confirming verification/sign-off, not implementation, dominates wall-clock. The 13-01 spike was cheap insurance that prevented the most expensive possible failure mode (a snap-back bug discovered after the full component was built).

---

## Milestone: v2.0.3 — PanelGroup Recompose Fix

**Shipped:** 2026-06-26
**Phases:** 1 (14) | **Plans:** 3 | **Sessions:** single-day push; 3 plans (fix, repro, release); fix commit `fbba375` → v2.0.3 tag

### What Was Built
- **Core fix (Phase 14-01, RCMP-01..03):** Eliminated all in-composition writes to observed snapshot-state in `AeroPanelGroupImpl`. `expandedArr` (used for size-math throughout the composable) now computed directly from `isExpanded(sec)` each composition. `expandedState` sync deferred to `SideEffect`. Seed block rewritten to not mutate state read in the same composition pass.
- **Showcase repro (Phase 14-02, RCMP-04):** Permanent horizontal controlled `AeroPanelGroup` demo (`rcmpExpandedKeys`) in `LayoutSection.kt` — Live / Static A / Static B sections, Live reads a `LaunchedEffect`-ticked counter (~32 ms). Human-verified: exactly 3 sections under recompose-while-drag after fix.
- **Release (Phase 14-03, REL-01..02):** `build.gradle.kts` version bumped `2.0.2`→`2.0.3`; annotated tag `v2.0.3` pushed to `Tolaseeq/aero-compose-ui`; JitPack build confirmed green.

### What Worked
- **Direct fix scope.** The root cause (in-composition write to observed state inside `BoxWithConstraints`) was well-specified before any plan was written, so plan 14-01 was a pure targeted edit with no spike needed — the earlier `SideEffect` / `isExpanded()` pattern from the research phase mapped directly to the production code.
- **Minimal repro gates the outcome.** The plan 14-02 repro (a controlled group with one `LaunchedEffect`-ticked section) exercises the exact failure mode cheaply and will catch any future regression that re-introduces in-composition writes to `expandedState`.
- **No regression to other paths.** The two-step fix (derive `expandedArr` from `isExpanded()`, defer `expandedState` sync to `SideEffect`) affected only the controlled-mode recompose path; vertical and uncontrolled paths are byte-identical, and all 12 `PanelGroupLogicTest` JVM tests stayed GREEN without any test changes.

### What Was Inefficient
- **The root cause (`SubcomposeLayout` ×N loop on in-composition state write) is a Compose invariant that could have been caught at the `AeroPanelGroupImpl` write time in Phase 13.** The `expandedState` seed block was written during Phase 13.1 composition — the pattern of writing to a `SnapshotStateList` inside `BoxWithConstraints` was not flagged as a PITFALL at the time. A write-during-composition rule ("no `mutableStateList.add/set` inside `BoxWithConstraints` body") added to PITFALLS.md at Phase 13 would have prevented v2.0.3 from being needed.

### Patterns Established
- **`SideEffect` for snapshot-state sync inside `SubcomposeLayout`** — if a composable both reads and writes a `SnapshotStateList`/`SnapshotStateMap` inside `BoxWithConstraints` (or any `SubcomposeLayout`), the write MUST be deferred to a `SideEffect`. Writing during composition creates a ×N recompose loop.
- **`isExpanded()` as structural source of truth** — when the component has a controlled API (`expandedKeys: Set<Any>`), derive all layout-critical booleans (`expandedArr`) from that source of truth each composition. Keep the observed-state mirror (`expandedState`) for animation targets only; update it in a `SideEffect` so it never feeds back into the same composition pass.

### Key Lessons
1. **`BoxWithConstraints`/`SubcomposeLayout` amplifies in-composition state writes into ×N loops.** A state write that is merely "one extra recompose" in a simple composable becomes a recompose-until-stable cascade inside `SubcomposeLayout`, producing visible ×N section duplication. The rule: any `SnapshotStateList.add/set` or `SnapshotStateMap.put` inside `BoxWithConstraints` must be moved to a `SideEffect`, `LaunchedEffect`, or callback. It is never safe during the composition pass inside a sub-compose layout.
2. **Record write-during-composition as a PITFALL at the phase that introduces it.** The pattern was introduced in Phase 13.1 and went undetected until a controlled-mode + recompose-during-drag stress test. A PITFALL note ("no snapshot-state writes inside `BoxWithConstraints` composition body") at Phase 13 write-time would have prevented this milestone.
3. **A `SideEffect` + `isExpanded()` split is the correct answer for controlled-mode Compose layout components.** The controlled API (`expandedKeys`) is the source of truth for structural decisions (which sections are expanded, how to distribute sizes). The observed-state mirror (`expandedState`) exists only to feed animation targets. These two concerns must not be conflated in the same composition pass.

### Cost Observations
- Model mix: opus for planning/research, sonnet for execution (`model_profile: balanced`); unchanged from prior milestones.
- 3 plans: fix (~2 min), repro + human-verify (~6 min), release. The fix itself was small; cost concentrated in the human verification gate (JitPack build wait).

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | ~3 days | 3 (1–3) | Established `:library` + `:showcase` split, Aero theme system, glass modifiers, 50 components, three themes, undecorated-window pattern (without `transparent=true` for Win11 safety) |
| v1.1 | ~1 day | 3 (4–6) | Established wave-ordered phase execution, spike-then-batch for code generation, vendored upstream pattern (`tools/phosphor-svgs/`), and visual-checkpoint-as-plan convention |
| v2.0 | concentrated | 5 (7–11) | Established enabling-phase-first for shared primitives, pure-logic-then-Compose (sealed state machines unit-tested without Compose), front-loaded PITFALLS catalog, silent-failure checklist as sign-off gate, grep-gates for banned APIs, hybrid controlled/uncontrolled component API |
| v2.0.1 | ~2h20m | 1 (12) | First patch milestone — single phase, verification-only gate (no separate audit); established nullable-formatter dispatch, fraction-as-stable-coordinate dividers, and verbatim state-machine reuse; confirmed that documented lessons must be applied at write-time (v2.0 lesson #3 regressed before the sign-off caught it) |
| v2.0.2 | ~1 day | 2 (13 + 13.1) | Single additive layout component + inserted orientation variant; established mandatory-spike-before-component for the highest-risk interaction (Pattern 3 two-writer coexistence), public-wrapper + internal-core for orientation symmetry, and refactor-then-extend (core extraction as its own zero-regression plan); confirmed "strictly one phase" is a soft boundary (deferred horizontal re-entered as Phase 13.1) |
| v2.0.3 | single-day | 1 (14) | Smallest patch milestone — 3 plans (fix, repro, release); established `SideEffect`-for-snapshot-state-sync rule for `BoxWithConstraints`/`SubcomposeLayout` context and `isExpanded()`-as-structural-source-of-truth; confirmed that write-during-composition inside `SubcomposeLayout` amplifies to ×N recompose loops |

### Cumulative Quality

| Milestone | Components | Icons | Themes | Showcase Sections | Library JAR |
|-----------|-----------|-------|--------|-------------------|-------------|
| v1.0 | ~50 | 0 (text glyphs + Material Icons) | 3 (AeroBlue, AeroDark, Classic) | 8 (Foundation, Buttons, Inputs, Selection, Range, Lists, Containers, Overlays/Nav) | ≈0.96 MB + ~36 MB classpath via `materialIconsExtended` |
| v1.1 | ~50 (no new components, all migrated) | 138 (`AeroIcons.*`, Phosphor Regular) | 3 (unchanged) | 9 (+ IconsSection) | ≈0.96 MB, classpath shed `materialIconsExtended` |
| v2.0 | ~62 (+12: 6 pickers, 2 data, 4 layout) | 138 (unchanged) | 3 (unchanged) | 12 (+ DataSection, PickersSection, LayoutSection) | +`kotlinx-datetime:0.6.2` (only new dependency) |
| v2.0.1 | ~63 (+1: `AeroDateTimeRangePicker`) | 138 (unchanged) | 3 (unchanged) | 12 (PickersSection + LayoutSection demos extended) | unchanged (zero new dependencies) |
| v2.0.2 | ~64 (+1: `AeroPanelGroup`, vertical + horizontal orientations) | 138 (unchanged) | 3 (unchanged) | 12 (LayoutSection: + vertical + 2 horizontal AeroPanelGroup demos) | unchanged (zero new dependencies) |
| v2.0.3 | ~64 (no new components — bug fix only) | 138 (unchanged) | 3 (unchanged) | 12 (LayoutSection: + RCMP-04 permanent recompose-during-drag repro) | unchanged (zero new dependencies) |

### Top Lessons (Verified Across Milestones)

1. **Visual checkpoints belong in plans, not afterthoughts.** v1.0 final visual checkpoint (Phase 3 P08) and v1.1 sign-off (Phase 6 P03) both produced clean approval records by being formal plans with their own `SUMMARY.md`. Off-the-cuff "let's just look at it" checkpoints are not auditable later. **v2.0 sharpened this:** deferring *all* visual verification to one end-of-milestone phase produced a 16-defect batch — checkpoints should be per-component-phase, not only at the showcase phase.
2. **Pre-flight checks catch silent disasters.** Phase 1 doctrine "no `transparent=true`" survives every milestone; v1.1 Phase 5 wave-ordering prevented broken-build commits; v2.0 generalized this into per-plan zero-match grep-gates for every banned API and a front-loaded PITFALLS catalog. Both were enforced via plan-level rules, not afterthought QA.
3. **Wrap, don't replace, Material3.** v1.0 chose to keep `Icon()` from material3 directly without an `AeroIcon()` wrapper; v1.1 reused that decision verbatim; v2.0 layered new stateful components on top without touching the v1.x API surface (no breaking changes). Less surface area, more interop, no consumer surprises.
4. **Test the correctness-critical logic without the UI runtime.** v1.1 proved generated code via `compileKotlin`; v2.0 extended it — sealed state machines and pure transition functions (range commit, lazy-expand guard, HSV math) unit-tested without Compose caught the hardest pitfalls before any rendering existed.
