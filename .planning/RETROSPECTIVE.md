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

## Cross-Milestone Trends

### Process Evolution

| Milestone | Sessions | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | ~3 days | 3 (1–3) | Established `:library` + `:showcase` split, Aero theme system, glass modifiers, 50 components, three themes, undecorated-window pattern (without `transparent=true` for Win11 safety) |
| v1.1 | ~1 day | 3 (4–6) | Established wave-ordered phase execution, spike-then-batch for code generation, vendored upstream pattern (`tools/phosphor-svgs/`), and visual-checkpoint-as-plan convention |

### Cumulative Quality

| Milestone | Components | Icons | Themes | Showcase Sections | Library JAR |
|-----------|-----------|-------|--------|-------------------|-------------|
| v1.0 | ~50 | 0 (text glyphs + Material Icons) | 3 (AeroBlue, AeroDark, Classic) | 8 (Foundation, Buttons, Inputs, Selection, Range, Lists, Containers, Overlays/Nav) | ≈0.96 MB + ~36 MB classpath via `materialIconsExtended` |
| v1.1 | ~50 (no new components, all migrated) | 138 (`AeroIcons.*`, Phosphor Regular) | 3 (unchanged) | 9 (+ IconsSection) | ≈0.96 MB, classpath shed `materialIconsExtended` |

### Top Lessons (Verified Across Milestones)

1. **Visual checkpoints belong in plans, not afterthoughts.** v1.0 final visual checkpoint (Phase 3 P08) and v1.1 sign-off (Phase 6 P03) both produced clean approval records by being formal plans with their own `SUMMARY.md`. Off-the-cuff "let's just look at it" checkpoints are not auditable later.
2. **Pre-flight checks catch silent disasters.** Phase 1 doctrine "no `transparent=true`" survives every milestone; v1.1 Phase 5 wave-ordering prevented broken-build commits. Both were enforced via plan-level rules, not afterthought QA.
3. **Wrap, don't replace, Material3.** v1.0 chose to keep `Icon()` from material3 directly without an `AeroIcon()` wrapper; v1.1 reused that decision verbatim. Less surface area, more interop, no consumer surprises.
