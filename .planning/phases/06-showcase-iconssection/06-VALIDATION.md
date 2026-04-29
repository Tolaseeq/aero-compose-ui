---
phase: 6
slug: showcase-iconssection
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-29
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Derived from `06-RESEARCH.md` `## Validation Architecture` and CONTEXT.md three-theme checkpoint.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None for Phase 6 visual deliverables — eyes-on three-theme checkpoint is the spec (locked in CONTEXT.md `### Three-theme visual checkpoint`). Existing JUnit 5 / `kotlin-test` infra in `:library` (`library/build.gradle.kts:21-25`) is unchanged. Showcase has no test config (`showcase/build.gradle.kts` has no `testImplementation`). |
| **Config file** | `library/build.gradle.kts` (existing JUnit 5 setup); no showcase test config |
| **Quick run command** | `./gradlew :showcase:compileKotlin` (compile gate; sub-second after warmup) |
| **Full suite command** | `./gradlew :library:test :showcase:compileKotlin` (lib tests + showcase compile) |
| **Visual gate command** | `./gradlew :showcase:run` (three-theme eyes-on, only available at phase gate) |
| **Estimated runtime** | Compile: ~5–10 s warm. Lib tests: ~10–20 s. Visual run: indefinite (interactive). |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :showcase:compileKotlin`. After the ButtonsSection edit task, also run `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` (must return 0).
- **After every plan wave:** Run all grep verifications + `./gradlew :showcase:compileKotlin` + `./gradlew :library:test`.
- **Before `/gsd:verify-work`:** Full grep suite green; compile green; lib tests green; visual three-theme checkpoint executed and `approved` recorded in `06-VERIFICATION.md`.
- **Max feedback latency:** ~10 seconds for compile, ~20 seconds for lib tests, manual for visual checkpoint.

---

## Per-Task Verification Map

> Filled by gsd-planner using this map; status column updated by gsd-executor.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 6-01-XX | 01 | 1 | SHW-04 | grep | `grep -c "IconEntry(" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must equal 138) | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-04 | grep | `grep -E "GridCells.Adaptive\(80.dp\)\|height\(400.dp\)" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (both patterns hit) | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-04 | grep | `grep -E "Modifier.size\(24.dp\)" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` AND `grep "tint = .*onSurface" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-05 | grep | `grep -E "AeroSearchField\(" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` AND `grep "Search icons" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-05 | grep | `grep "ignoreCase = true" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must hit) | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-05 | grep | `grep "No icons match" showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` (must hit) | created in task | ⬜ pending |
| 6-01-XX | 01 | 1 | SHW-04 | grep | `grep -n "IconsSection" showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` (must hit; line falls between `FoundationSection` block end and `ButtonsSection()` call) | modified in task | ⬜ pending |
| 6-01-XX | 01 | 1 | (compile gate) | automated | `./gradlew :showcase:compileKotlin` exits 0 | ✅ existing | ⬜ pending |
| 6-02-XX | 02 | 1 | SHW-06 | grep | `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` returns 0 hits (currently 3) | modified in task | ⬜ pending |
| 6-02-XX | 02 | 1 | SHW-06 | grep | `grep -E "Icon\(AeroIcons\.(CaretUp\|CaretDown\|X)" showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt` returns 3 hits | modified in task | ⬜ pending |
| 6-02-XX | 02 | 1 | SHW-06 | grep | `grep -A1 "Icon(AeroIcons.CaretUp\|Icon(AeroIcons.CaretDown\|Icon(AeroIcons.X" showcase/src/main/kotlin/com/mordred/showcase/sections/ButtonsSection.kt \| grep -E "tint = colors.onSurface\|Modifier.size\(14.dp\)"` (both patterns hit) | modified in task | ⬜ pending |
| 6-02-XX | 02 | 1 | (compile gate) | automated | `./gradlew :showcase:compileKotlin` exits 0 | ✅ existing | ⬜ pending |
| 6-XX-99 | XX | 2 | SHW-04, SHW-05, SHW-06 | manual | `./gradlew :showcase:run` → three-theme checkpoint (see §Manual-Only Verifications) | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*
*Task IDs are placeholders; gsd-planner will assign concrete `6-{plan}-{task}` IDs when generating PLAN.md files.*

---

## Wave 0 Requirements

**None.** Phase 6 has no automated test infrastructure to install:

- `./gradlew :showcase:compileKotlin` is already configured.
- grep verification needs no tooling.
- The eyes-on three-theme checkpoint is locked into CONTEXT.md as the v1.1 milestone visual sign-off gate; no Wave 0 prep required.

*Existing infrastructure covers all phase requirements.*

---

## Manual-Only Verifications

The three-theme visual checkpoint at `./gradlew :showcase:run` is the formal v1.1 milestone sign-off gate. Folded into Phase 6's closing acceptance task.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| All 138 icons render visibly in AeroBlue with correct contrast | SHW-04 | Visual contrast cannot be grep-verified; locked as eyes-on per CONTEXT.md | `./gradlew :showcase:run` → ThemeSwitcher = AeroBlue → scroll through `IconsSection` → confirm grid shows multiple full pages, no obvious gaps; spot-check `X`, `CaretDown`, `MagnifyingGlass`, `FrameCorners`, `Warning`, `Square` |
| All 138 icons remain visible on AeroDark (no black-on-black, Phosphor stroke holds at 24dp) | SHW-04 | Theme contrast eye-on | Switch to AeroDark → confirm same six representative icons remain readable; specifically inspect `FrameCorners`, `Square`, `Warning` for thin-stroke regression |
| All 138 icons render correctly on Classic theme | SHW-04 | Theme contrast eye-on | Switch to Classic → repeat representative-icon check |
| `AeroNumberSpinner` disabled-state up/down icons remain visible on AeroDark | SHW-04 (carry-forward verification, NOT re-decision) | Phase 5 already approved 12dp/14dp; this is a regression spot-check | On AeroDark, scroll to `InputSection` → `AeroNumberSpinner` → set to disabled state → confirm up/down icons visible |
| Real-time filter behavior + clear-button restore + empty-state message | SHW-05 | Interactive eyes-on; only confirms in running app | Type `caret` → must show only `CaretDown`, `CaretUp`, `CaretLeft`, `CaretRight` (4 entries); clear field via `AeroSearchField`'s built-in clear button → must restore all 138; type `xyzzy` → must show `Text("No icons match 'xyzzy'")` empty state |
| Click-to-copy behavior with toast confirmation | (Click action, supports SHW-04 visual deliverable) | Interactive | Click any cell → toast appears bearing literal `Copied AeroIcons.<Name>` → paste into a text editor → identifier matches |
| `ButtonsSection` real Phosphor caret/X glyphs visible in all three themes | SHW-06 | Visual confirmation that the migration produced visible icons (compile + grep alone don't prove visibility) | Folded into the three-theme pass — confirm `AeroIconButton` row shows three Phosphor icons with no text glyphs in AeroBlue, AeroDark, and Classic |
| Final approval | SHW-04, SHW-05, SHW-06 (and v1.1 milestone visual sign-off) | Per CONTEXT.md, user types `approved` (or describes regression) inline; recorded in `06-VERIFICATION.md` | After all eight checkpoint items pass, user types `approved` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify (compile + grep) or are explicitly Manual-Only
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify (Phase 6 has at most 2 implementation tasks per plan; compile gate covers each)
- [ ] Wave 0 covers all MISSING references — N/A (no Wave 0)
- [ ] No watch-mode flags
- [ ] Feedback latency < ~30s for automated checks
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
