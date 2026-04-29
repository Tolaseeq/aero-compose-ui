---
phase: 4
slug: aeroicons-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-29
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Phase 4 is **structural / compile-time only** — no runtime tests, no UI surface; the Kotlin compiler + grep checks enforce ICN-01/02/03.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None — compile-time + grep only (no `:test` task added) |
| **Config file** | `library/build.gradle.kts` (existing `explicitApi()` block at line ~9) |
| **Quick run command** | `./gradlew :library:compileKotlin` |
| **Full suite command** | `./gradlew :library:compileKotlin` + grep verification block (see Per-Task table) |
| **Estimated runtime** | ~30–60s (cold compile of `:library` after generation) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :library:compileKotlin` (proves explicitApi + lazy pattern + ImageVector typing)
- **After every plan wave:** Run full grep verification block + property count
- **Before `/gsd:verify-work`:** All grep checks return 0 hits where required, 138 where required
- **Max feedback latency:** ~60 seconds per compile pass

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | (meta) | grep | `grep -rn "139" .planning/ROADMAP.md .planning/REQUIREMENTS.md .planning/STATE.md .planning/research/SUMMARY.md .planning/research/FEATURES.md .planning/research/ARCHITECTURE.md` (must return 0 references to 139-icon count after fix; date/year hits OK) | ✅ existing | ⬜ pending |
| 04-01-02 | 01 | 1 | (meta) | grep | `grep -rn "strokeLineWidth=12f\|stroke-width.*12\|stroke 12" .planning/research/ .planning/phases/04-aeroicons-foundation/04-CONTEXT.md .planning/REQUIREMENTS.md` (must return 0; corrected to 16) | ✅ existing | ⬜ pending |
| 04-01-03 | 01 | 1 | ICN-03 | file | `test -f tools/phosphor-svgs/.pin && test -f tools/phosphor-svgs/LICENSE && test -f tools/phosphor-svgs/README.md` | ❌ W0 | ⬜ pending |
| 04-01-04 | 01 | 1 | ICN-03 | grep + count | `ls tools/phosphor-svgs/regular/*.svg \| wc -l` (must equal 7 after spike vendoring) | ❌ W0 | ⬜ pending |
| 04-01-05 | 01 | 1 | ICN-03 | file count | `ls library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt \| wc -l` (must equal 7 after Valkyrie spike) | ❌ W0 | ⬜ pending |
| 04-01-06 | 01 | 1 | ICN-03 | grep | `grep -rn "viewportWidth=24f" library/src/main/kotlin/com/mordred/aero/icons/` (must return 0) | ❌ W0 | ⬜ pending |
| 04-01-07 | 01 | 1 | ICN-03 | grep | `grep -rn "viewportWidth=256f" library/src/main/kotlin/com/mordred/aero/icons/internal/` (must return ≥7 hits — one per spike file) | ❌ W0 | ⬜ pending |
| 04-01-08 | 01 | 1 | ICN-03 | grep | `grep -rn "strokeLineWidth=16f" library/src/main/kotlin/com/mordred/aero/icons/internal/` (must return ≥7 hits) | ❌ W0 | ⬜ pending |
| 04-01-09 | 01 | 1 | ICN-03 | grep | `grep -rn "StrokeCap.Round\|StrokeCap\.Companion\.Round" library/src/main/kotlin/com/mordred/aero/icons/internal/` (must return ≥7 hits) | ❌ W0 | ⬜ pending |
| 04-01-10 | 01 | 1 | ICN-01 | file | `test -f library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` | ❌ W0 | ⬜ pending |
| 04-01-11 | 01 | 1 | ICN-01 | grep | `grep -E "^public object AeroIcons" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must match 1 line) | ❌ W0 | ⬜ pending |
| 04-01-12 | 01 | 1 | ICN-01 | grep | `grep -c "public val .*: ImageVector" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must equal 7 after spike) | ❌ W0 | ⬜ pending |
| 04-01-13 | 01 | 1 | ICN-02 | grep | `grep -B2 "public object AeroIcons" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt \| grep -E "\\*/\|/\\*\\*"` (KDoc block precedes object) | ❌ W0 | ⬜ pending |
| 04-01-14 | 01 | 1 | ICN-02 | grep | KDoc must contain naming convention table — `grep -E "caret-down\|magnifying-glass\|kebab\|PascalCase" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must match ≥4 lines) | ❌ W0 | ⬜ pending |
| 04-01-15 | 01 | 1 | ICN-02 | grep | KDoc tint warning — `grep -E "tint\|LocalContentColor" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must match ≥1 line in KDoc region) | ❌ W0 | ⬜ pending |
| 04-01-16 | 01 | 1 | ICN-01, ICN-02, ICN-03 | compile | `./gradlew :library:compileKotlin` (must exit 0; no `Visibility must be specified` errors) | ❌ W0 | ⬜ pending |
| 04-02-01 | 02 | 1 | ICN-03 | count | `ls tools/phosphor-svgs/regular/*.svg \| wc -l` (must equal 138) | ❌ W0 | ⬜ pending |
| 04-02-02 | 02 | 1 | ICN-03 | count | `ls library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt \| wc -l` (must equal 138) | ❌ W0 | ⬜ pending |
| 04-02-03 | 02 | 1 | ICN-03 | grep | `grep -c "public val .*: ImageVector" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must equal 138) | ❌ W0 | ⬜ pending |
| 04-02-04 | 02 | 1 | ICN-03 | grep | `grep -rn "viewportWidth=24f" library/src/main/kotlin/com/mordred/aero/icons/` (must return 0 across all 138 files) | ❌ W0 | ⬜ pending |
| 04-02-05 | 02 | 1 | ICN-03 | grep | `grep -rn "viewportWidth=256f" library/src/main/kotlin/com/mordred/aero/icons/internal/ \| wc -l` (must equal 138) | ❌ W0 | ⬜ pending |
| 04-02-06 | 02 | 1 | ICN-01 | grep | Lazy pattern check — `grep -E "private var _[A-Z]" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt \| wc -l` (must equal 138, OR Valkyrie's actual cache mechanism is documented if shape differs) | ❌ W0 | ⬜ pending |
| 04-02-07 | 02 | 1 | ICN-01 | grep | No eager-init `val ... = ImageVector.Builder` — `grep -E "public val [A-Z][A-Za-z]*: ImageVector =" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must return 0) | ❌ W0 | ⬜ pending |
| 04-02-08 | 02 | 1 | ICN-01, ICN-02, ICN-03 | compile | `./gradlew :library:compileKotlin` (must exit 0) | ❌ W0 | ⬜ pending |
| 04-02-09 | 02 | 1 | ICN-03 | sample-grep | Spot-read 5 random `internal/*.kt` files: `viewportWidth=256f`, `strokeLineWidth=16f`, `fill=Color.Transparent` or equivalent, `StrokeCap.Round`, `StrokeJoin.Round` all present | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

*Acceptance-criterion mappings: every grep above feeds directly into a task `<acceptance_criteria>` block in the PLAN.md files.*

---

## Wave 0 Requirements

Phase 4 has no Wave-0 test-infrastructure setup — there are no test files to author and no test framework to install.

The "Wave 0" entries above mark files that **must exist by phase end**, not test stubs. Specifically:

- [ ] `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — facade; covers ICN-01, ICN-02, ICN-03 (created in 04-01, completed in 04-02)
- [ ] `library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt` — 138 generated files; covers ICN-03 (7 in 04-01 spike, +131 in 04-02 batch)
- [ ] `tools/phosphor-svgs/regular/*.svg` — 138 vendored sources (7 in 04-01, +131 in 04-02)
- [ ] `tools/phosphor-svgs/.pin` + `LICENSE` + `README.md` — provenance + attribution + recovery instructions
- [ ] No new test framework — `./gradlew :library:compileKotlin` already exists
- [ ] No `library/src/test/` or `library/src/androidTest/` additions (locked decision: no smoke test in Phase 4)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Spike-glyph appearance check (7 icons) | ICN-03 | Visual inspection of generated `.kt` paths can detect Valkyrie conversion errors that compile fine but produce wrong glyphs | Spot-read all 7 spike files (`X.kt`, `CaretDown.kt`, `MagnifyingGlass.kt`, `Check.kt`, `Info.kt`, `FrameCorners.kt`, `Square.kt`); confirm path data is non-empty and the file structure looks like a Compose `ImageVector.Builder().materialPath { … }` chain |
| `FrameCorners` / `Square` visual weight on glassmorphic background | ICN-03 (carry-forward from research SUMMARY) | These two title-bar icons need eye-test confirmation that line-weight 16 reads correctly against `AeroTitleBar` glass; pure code grep cannot tell | Deferred to Phase 5 first integration; flag as known check-point in 04-01 plan notes — Phase 4 itself does NOT render anything |

---

## Validation Sign-Off

- [ ] Every task in 04-01-PLAN.md and 04-02-PLAN.md has either an `<automated>` verify command (matching a row above) or a documented Wave 0 dependency
- [ ] Sampling continuity: no 3 consecutive tasks without an automated verify (Phase 4 is grep-heavy, easily satisfied)
- [ ] Wave 0 covers all MISSING references (the 138 icon files + facade are the only "missing" artifacts)
- [ ] No watch-mode flags (none used; one-shot compile + grep)
- [ ] Feedback latency < 60s (compile + grep round-trip is well under)
- [ ] `nyquist_compliant: true` set in frontmatter once all rows green

**Approval:** pending
