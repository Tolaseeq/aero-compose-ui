---
phase: 5
slug: component-migrations
status: draft
nyquist_compliant: false
wave_0_complete: true
created: 2026-04-29
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter (JUnit 5) via `libs.junit.jupiter` |
| **Config file** | `library/build.gradle.kts` — `tasks.test { useJUnitPlatform() }` |
| **Quick run command** | `./gradlew :library:compileKotlin` (per-task grep + compile) |
| **Full suite command** | `./gradlew :library:test` |
| **Estimated runtime** | ~30 seconds (compile) / ~45 seconds (test) |

---

## Sampling Rate

- **After every task commit:** Run grep acceptance assertions for that task; on any task that mutates Kotlin code, also run `./gradlew :library:compileKotlin`.
- **After every plan wave:** Run `./gradlew :library:test`.
- **Before `/gsd:verify-work`:** Full suite must be green; CLN-02/CLN-03 grep verifications must pass.
- **Max feedback latency:** ~45 seconds.

---

## Per-Task Verification Map

> Filled by gsd-planner during plan creation. Each plan task must list its `<automated>` command (grep, compile, test) tied to a requirement ID.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-XX | 01 | 1 | MIG-01 | grep + compile | `grep -nE 'Text\("(✓\|–)"' library/src/.../AeroCheckbox.kt` → 0; `./gradlew :library:compileKotlin` | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-02 | grep + compile | `grep -n 'Text("▼"' library/src/.../AeroDropdown.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-03 | grep + compile + visual | `grep -nE 'Text\("(▲\|▼)"' AeroNumberSpinner.kt` → 0; `grep -c 'Modifier.size(12.dp)' AeroNumberSpinner.kt` ≥ 2; visual approval AeroDark | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-05 | grep + compile | `grep -n 'Text("▶"' AeroContextMenu.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-06 | grep + compile | `grep -n 'Text("✕"' AeroToastHost.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-07 | grep + compile | `grep -n 'Text("✕"' AeroNotificationBanner.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-10 | grep + compile | `grep -n 'Icons.Outlined' AeroAlertKind.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-XX | 01 | 1 | MIG-11 | grep + compile | `grep -n 'Icons.Outlined' AeroBannerKind.kt` → 0; compile | ✅ | ⬜ pending |
| 5-02-XX | 02 | 2 | MIG-08 | grep + compile | `grep -nE 'SearchIcon\|Canvas\|Text\("x"' AeroSearchField.kt` → 0; compile | ✅ | ⬜ pending |
| 5-02-XX | 02 | 2 | MIG-09 | grep + compile | `grep -nE 'EyeOpenIcon\|EyeClosedIcon\|Canvas' AeroPasswordField.kt` → 0; compile | ✅ | ⬜ pending |
| 5-03-XX | 03 | 3 | MIG-04 | grep + compile | `grep -nE 'glyph.*String\|Text\("(─\|□\|❒\|✕)"' AeroTitleBar.kt` → 0; `grep -nE 'AeroIcons\.(Minus\|Square\|FrameCorners\|X)' AeroTitleBar.kt` ≥ 4; compile | ✅ | ⬜ pending |
| 5-04-XX | 04 | 4 | CLN-01 | test run | `./gradlew :library:test --tests "*AeroAlertKindTest" --tests "*AeroBannerKindTest"` exits 0; `grep -n 'Icons.Outlined' AeroAlertKindTest.kt AeroBannerKindTest.kt` → 0 | ✅ | ⬜ pending |
| 5-05-XX | 05 | 5 | CLN-02 | compile + dep check | `./gradlew :library:compileKotlin` succeeds without `compose.materialIconsExtended`; `./gradlew :library:dependencies --configuration compileClasspath` lacks `materialIcons` lines | ✅ | ⬜ pending |
| 5-05-XX | 05 | 5 | CLN-03 | grep | `grep -rn "androidx.compose.material.icons" library/src/` → 0; ROADMAP success-criteria greps for unicode glyphs all → 0 | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing test infrastructure covers all phase requirements:
- `AeroAlertKindTest.kt` and `AeroBannerKindTest.kt` already exist; CLN-01 is an in-place rewrite, not a new file.
- JUnit 5 platform already wired via `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`).
- No new test framework, fixtures, or stubs required.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AeroNumberSpinner caret legibility (no sub-pixel collapse) | MIG-03 | Renders only at runtime; sub-pixel rasterization can't be asserted by grep or unit test | Run showcase, navigate to Spinner section, observe up/down icons in AeroDark + AeroBlue + Classic themes (including disabled state). User types `approved` or describes regression. If hairline observed, escalate per fallback ladder (12dp → 14dp → 16dp icon size). |
| JAR-size delta documentation | CLN-02 (success criterion 5) | Filesystem measurement, not a code property | Capture `library-*.jar` size on post-Wave-4 tree (baseline); capture again after CLN-02 commits; record pre/post/delta in `05-SUMMARY.md` and `STATE.md` Performance Metrics block. Expected delta: ~6–8 MB. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (none required)
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter (post-planner sign-off)

**Approval:** pending
