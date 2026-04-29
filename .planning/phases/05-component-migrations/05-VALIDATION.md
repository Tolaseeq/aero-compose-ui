---
phase: 5
slug: component-migrations
status: planned
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-29
updated: 2026-04-29
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

> Filled by gsd-planner during plan creation. Each plan task lists its `<automated>` command (grep, compile, test) tied to a requirement ID.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-T1 | 01 | 1 | MIG-01 | grep + compile | `grep -nE 'Text\("(✓\|–)"' library/src/.../AeroCheckbox.kt` → 0; `./gradlew :library:compileKotlin` | ✅ | ⬜ pending |
| 5-01-T2 | 01 | 1 | MIG-02 | grep + compile | `grep -n 'Text("▼"' library/src/.../AeroDropdown.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-T3 | 01 | 1 | MIG-03 | grep + compile | `grep -nE 'Text\("(▲\|▼)"' AeroNumberSpinner.kt` → 0; `grep -c 'Modifier.size(12.dp)' AeroNumberSpinner.kt` ≥ 2; `grep -c 'height = 14.dp' AeroNumberSpinner.kt` ≥ 2; compile | ✅ | ⬜ pending |
| 5-01-T4 | 01 | 1 | MIG-03 | visual checkpoint | User runs `./gradlew :showcase:run`, eyes spinner up/down in AeroBlue + AeroDark + Classic, types `approved` (or escalates 12→14→16dp ladder) | ✅ | ⬜ pending |
| 5-01-T5 | 01 | 1 | MIG-05 | grep + compile | `grep -n 'Text("▶"' AeroContextMenu.kt` → 0; compile | ✅ | ⬜ pending |
| 5-01-T6 | 01 | 1 | MIG-06 | grep + compile | `grep -n 'Text("✕"' AeroToastHost.kt` → 0; `grep -n '"Close toast"' AeroToastHost.kt` ≥ 1; compile | ✅ | ⬜ pending |
| 5-01-T7 | 01 | 1 | MIG-07 | grep + compile | `grep -n 'Text("✕"' AeroNotificationBanner.kt` → 0; `grep -n '"Close notification"' AeroNotificationBanner.kt` ≥ 1; `grep -n 'imageVector = kind.icon' AeroNotificationBanner.kt` ≥ 1 (line 55 unchanged); compile | ✅ | ⬜ pending |
| 5-01-T8 | 01 | 1 | MIG-10 | grep + compile | `grep -n 'Icons.Outlined' AeroAlertKind.kt` → 0; `grep -nE 'AeroIcons\.(Info\|Warning\|XCircle\|Question)' AeroAlertKind.kt` ≥ 4; compile | ✅ | ⬜ pending |
| 5-01-T9 | 01 | 1 | MIG-11 | grep + compile | `grep -n 'Icons.Outlined' AeroBannerKind.kt` → 0; `grep -nE 'AeroIcons\.(Info\|Warning\|XCircle\|CheckCircle)' AeroBannerKind.kt` ≥ 4; compile | ✅ | ⬜ pending |
| 5-02-T1 | 02 | 2 | MIG-08 | grep + compile | `grep -nE 'SearchIcon\|Canvas\|StrokeCap\|drawscope' AeroSearchField.kt` → 0; `grep -n 'Text("x"' AeroSearchField.kt` → 0; `grep -nE 'AeroIcons\.(MagnifyingGlass\|X)' AeroSearchField.kt` ≥ 2; compile | ✅ | ⬜ pending |
| 5-02-T2 | 02 | 2 | MIG-09 | grep + compile | `grep -nE 'EyeOpenIcon\|EyeClosedIcon\|Canvas\|drawPath' AeroPasswordField.kt` → 0; `grep -nE 'AeroIcons\.(Eye\|EyeSlash)' AeroPasswordField.kt` ≥ 2; `grep -nE '"(Show\|Hide) password"' AeroPasswordField.kt` ≥ 2; compile | ✅ | ⬜ pending |
| 5-03-T1 | 03 | 3 | MIG-04 | grep + compile | `grep -nE 'glyph.*String\|Text\("(─\|□\|❒\|✕)"' AeroTitleBar.kt` → 0; `grep -nE 'AeroIcons\.(Minus\|Square\|FrameCorners\|X)' AeroTitleBar.kt` ≥ 4; `grep -n 'icon: ImageVector' AeroTitleBar.kt` ≥ 1; `grep -nE '"(Minimize\|Maximize\|Restore\|Close) window"' AeroTitleBar.kt` ≥ 4; compile | ✅ | ⬜ pending |
| 5-04-T1 | 04 | 4 | CLN-01a | test run | `./gradlew :library:test --tests "*AeroAlertKindTest"` exits 0; `grep -n 'Icons.Outlined' AeroAlertKindTest.kt` → 0; `grep -nE 'assertEquals\(AeroIcons\.(Info\|Warning\|XCircle\|Question)' AeroAlertKindTest.kt` ≥ 4 | ✅ | ⬜ pending |
| 5-04-T2 | 04 | 4 | CLN-01b | test run | `./gradlew :library:test --tests "*AeroBannerKindTest"` exits 0; `grep -n 'Icons.Outlined' AeroBannerKindTest.kt` → 0; `grep -nE 'assertEquals\(AeroIcons\.(Info\|Warning\|XCircle\|CheckCircle)' AeroBannerKindTest.kt` ≥ 4 | ✅ | ⬜ pending |
| 5-04-T3 | 04 | 4 | CLN-01 (gate) | full test run | `./gradlew :library:test` exits 0; `grep -rn "androidx.compose.material.icons" library/src/test/` → 0 | ✅ | ⬜ pending |
| 5-05-T1 | 05 | 5 | CLN-02 (baseline) | jar build + size capture | `./gradlew :library:test` exits 0; `./gradlew :library:jar` exits 0; baseline byte count captured (numeric > 0) | ✅ | ⬜ pending |
| 5-05-T2 | 05 | 5 | CLN-02 | compile + dep check | `grep -n 'compose.materialIconsExtended' library/build.gradle.kts` → 0; `./gradlew :library:compileKotlin` exits 0; `./gradlew :library:dependencies --configuration compileClasspath \| grep -i materialIcons` → 0 lines | ✅ | ⬜ pending |
| 5-05-T3 | 05 | 5 | CLN-03 | grep | `grep -rn "androidx.compose.material.icons" library/src/` → 0; `grep -rn 'Text("▲\|▼\|▶\|✕\|✓\|─\|□\|❒\|–")' library/src/` → 0; `grep -rn 'Text("x"' library/src/` → 0; `grep -rn 'Icons.Outlined' library/src/` → 0; `grep -rn 'SearchIcon\|EyeOpenIcon\|EyeClosedIcon' library/src/main/` → 0; `./gradlew :library:test` exits 0 | ✅ | ⬜ pending |
| 5-05-T4 | 05 | 5 | CLN-02 (delta) | jar build + docs | `./gradlew :library:jar` exits 0; `.planning/phases/05-component-migrations/05-SUMMARY.md` exists with JAR-size delta block; `grep -n 'JAR size' .planning/STATE.md` ≥ 1; commit lands with `docs(05-05):` prefix | ✅ | ⬜ pending |

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
| AeroNumberSpinner caret legibility (no sub-pixel collapse) | MIG-03 | Renders only at runtime; sub-pixel rasterization can't be asserted by grep or unit test | Run showcase, navigate to Spinner section, observe up/down icons in AeroDark + AeroBlue + Classic themes (including disabled state). User types `approved` or describes regression. If hairline observed, escalate per fallback ladder (12dp → 14dp → 16dp icon size). Owned by Plan 05-01 Task 4. |
| JAR-size delta documentation | CLN-02 (success criterion 5) | Filesystem measurement, not a code property | Capture `library-*.jar` size on post-Wave-4 tree (baseline; Plan 05-05 Task 1); capture again after CLN-02 commits (Plan 05-05 Task 4); record pre/post/delta in `05-SUMMARY.md` and `STATE.md` Performance Metrics block. Expected delta: ~6–8 MB. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (Wave 0 covers everything; Task 5-01-T4 is a checkpoint:human-verify, listed in Manual-Only above)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (every task has grep+compile or full test run)
- [x] Wave 0 covers all MISSING references (none required — both test files already exist)
- [x] No watch-mode flags
- [x] Feedback latency < 60s (compile ~30s, full test ~45s)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready for execution
