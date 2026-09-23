---
phase: 21-migration-release-3-1-0
plan: 07
subsystem: build
tags: [gradle, compose-multiplatform, hot-reload, mcp, showcase, jetbrains-runtime]

# Dependency graph
requires:
  - phase: 21-06
    provides: "Toolchain at Gradle 9.7.1 / JDK 21 / Kotlin 2.4.20 / Compose Multiplatform 1.12.0 (CMP >= 1.12.0 is Hot Reload's MCP window-registration floor per MCP-HOWTO.md); tools/verify/check-material3.sh"
provides:
  - "Compose Hot Reload 1.2.0 applied to :showcase only (catalog entry + plugin alias + hotRun forwarding block); :library's build file, POM, and module metadata unchanged"
  - ".mcp.json at repo root wiring the compose-hot-reload MCP server via `cmd /c .\\gradlew.bat --no-daemon --quiet --console=plain hotMcpServer`"
  - "Empirically confirmed facts for downstream plans: hotRun task type FQN (org.jetbrains.compose.reload.gradle.ComposeHotRun, a JavaExec subtype), hotMcpServer stdio carries only JSON-RPC frames, hotRun runs the app on JetBrains Runtime 21.0.9, every Hot Reload listener binds to 127.0.0.1 only"
  - "Recorded, maintainer-accepted finding: Compose Multiplatform 1.12.0 registers auxiliary dev/composeHotReload* Gradle configurations referencing hot-reload artifacts on every subproject unconditionally (including :library), independent of where the Hot Reload plugin is applied; :library's published POM/module.json/compile-runtime/api/test classpaths remain unaffected"
affects: [21-08, 21-09, 21-10, 21-11, 21-12, 21-13, 21-14]

# Tech tracking
tech-stack:
  added: ["org.jetbrains.compose.hot-reload 1.2.0 (showcase-only dev dependency, not published)"]
  patterns:
    - "Sibling tasks.withType<T>() forwarding block per hot-run task type (JavaExec vs ComposeHotRun), both keyed off the same project.findProperty(\"aero.*\") idiom, so aero.scheme/section/page/capture reach both the plain `run` task and any Compose Hot Reload run task without duplicating the parameter-parsing logic in Main.kt"
    - "Isolation proof pattern for a build-file/plugin change: regenerate the publication (POM + module.json), diff byte-for-byte (module.json checksum lines filtered) against a pre-change snapshot, and cross-check with a temporary git checkout -- revert + git apply restore to isolate whether a found difference predates the change under test"

key-files:
  created:
    - .mcp.json
  modified:
    - gradle/libs.versions.toml
    - showcase/build.gradle.kts
    - .planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md

key-decisions:
  - "Maintainer accepted the HRM-01 isolation checkpoint as-is: the plan's literal unscoped `:library:dependencies | grep -ic hot-reload` check (64, not 0) is satisfied in substance, not letter — all 64 hits live in Compose Multiplatform 1.12.0's own auxiliary dev/composeHotReload* configurations (proven intrinsic to the CMP 1.12.0 bump already committed in Plan 06, present with the Hot Reload plugin applied nowhere in the build), never in :library's actual compile/runtime/api/publication surface, which stayed byte-identical"
  - "hotRun's ComposeHotRun task type (confirmed via javap: extends JavaExec through AbstractComposeHotRun) gets its own tasks.withType<> forwarding block rather than being folded into the existing `if (name == \"run\")` JavaExec block, since ComposeHotRun tasks are named hotRun/hotRunAsync/hotDev/hotDevAsync, never `run`"

requirements-completed: [HRM-01, TOOL-12]

# Metrics
duration: ~30min active work across two sessions (pre-checkpoint investigation + post-decision commit), plus a maintainer decision pause on the isolation checkpoint
completed: 2026-09-23
---

# Phase 21 Plan 07: Compose Hot Reload 1.2.0 + MCP server in :showcase only Summary

**Compose Hot Reload 1.2.0 and its MCP server are wired into `:showcase` only via one commit — `.mcp.json` launches `hotMcpServer` through the required `cmd /c .\gradlew.bat` shape, a `ComposeHotRun`-typed forwarding block carries the existing `aero.*` launch parameters into `hotRun`, and `:library`'s published POM/module metadata are proven byte-identical before and after, despite Compose Multiplatform 1.12.0 itself (not this plugin) exposing hot-reload references in `:library`'s unpublished auxiliary Gradle configurations — a finding the maintainer reviewed and explicitly accepted.**

## Performance

- **Duration:** ~30 min of active gated work, split across an investigation session that stopped at a decision checkpoint and a short session that committed after the maintainer's decision
- **Started:** 2026-09-23 (previous plan's last commit `a877a80`)
- **Completed:** 2026-09-23T19:39:43+03:00 (commit `bb8c716`)
- **Tasks:** 2/2 auto tasks completed; Task 3 (checkpoint:human-action, maintainer restarts Claude Code) handed off per this plan's execution instructions — not completed in this session, its acceptance is verified by Plan 21-08 Task 1
- **Files modified/created:** 4 (3 modified, 1 created)

## Accomplishments
- **Task 1 (pre-install):** Confirmed CMP 1.12.0 already registers the full Hot Reload task set (`hotMcpServer`, `hotRun`, `hotDev`, etc.) on both `:library` and `:showcase` before any file in this plan was touched — a task-registration fact, not a classpath fact. `:library`'s `runtimeClasspath` hot-reload count was `0` at this point (the actual pre-install gate). Snapshotted `:library`'s POM and module.json to `build/aero-hrm01-before/`. `MATERIAL3 OK`.
- **Task 2 (install + verify):** Added `composeHotReload = "1.2.0"` and the `compose-hot-reload` plugin entry to the catalog; applied `alias(libs.plugins.compose.hot.reload)` to `showcase/build.gradle.kts` only; added a `tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>()` sibling block forwarding `aero.scheme`/`aero.section`/`aero.page`/`aero.capture` (task type FQN confirmed via `./gradlew :showcase:help --task hotRun` and `javap`, which showed it extends `JavaExec`). Wrote `.mcp.json` with the exact MCP-HOWTO shape.
- Verified stdio cleanliness: one JSON-RPC `initialize` frame piped into `hotMcpServer` (240s time-boxed) produced exactly one stdout line, valid JSON, with an `id:1` response; stderr carried only `JAVA_TOOL_OPTIONS`/Gradle noise.
- Verified the `hotRun` smoke launch: `AERO_READY scheme=AeroDark section=Buttons ... jvm=21.0.9/JetBrains_s.r.o.` — forwarding reaches `hotRun`, and the app runs on JetBrains Runtime 21.0.9 without a foojay resolver. Every listening socket opened by the app/Gradle processes bound to `127.0.0.1` only (T-21-03). Cleanup left zero orphan `java.exe` processes referencing `com.mordred.showcase.MainKt` or the `ui_lib` path.
- Isolation: `:library`'s regenerated POM and module.json (checksum lines filtered) are byte-identical to the pre-install snapshot; `git diff --stat HEAD -- library/` is empty; `:library`'s `compileClasspath`/`runtimeClasspath`/`api`/`apiElements`/`runtimeElements`/`compileOnly`/`runtimeOnly`/`testCompileClasspath`/`testRuntimeClasspath` all show zero hot-reload hits. `MATERIAL3 OK` after the plugin was added. `./gradlew :library:test --rerun` stayed at the locked `541/0` count.
- **Checkpoint raised and resolved:** the plan's own unscoped `./gradlew -q :library:dependencies | grep -ic hot-reload` check returned `64`, not `0`. Traced every hit to its real owning configuration — all confined to Compose Multiplatform 1.12.0's own `dev*`/`composeHotReload*` auxiliary configurations, never to anything that reaches `:library`'s publication. Proved this predates and is unrelated to this task's changes by reverting `gradle/libs.versions.toml`/`showcase/build.gradle.kts` to the Plan 06 commit (Hot Reload plugin applied nowhere in the build) and re-running the same check — still `64`, identical — then restoring the Task 2 edits. Presented this as a decision checkpoint; the maintainer chose **accept as-is**, recording that HRM-01's actual requirement text (`:library`'s build file/POM/module metadata unchanged) is met in full, and the 64 unscoped hits are a CMP-1.12.0-intrinsic characteristic, not a Hot Reload-plugin leak.
- **Task 3 handoff:** ran Task 3's automated verify — `.mcp.json` parses as valid JSON, contains `hotMcpServer`, working tree has only the plan's own committed changes plus the same 6 pre-existing untouched items (`.planning/config.json` modification, `.planning/HANDOFF.json`, `.serena/`, `.vscode/`, two stray log files). Everything else in Task 3 (the maintainer's Claude Code restart) is a human action this session cannot perform or wait on — it is handed to the maintainer per this plan's explicit execution instructions, and its acceptance criteria (the `mcp__compose-hot-reload__*` tools becoming available) is verified by Plan 21-08's Task 1 in the next session.

## Task Commits

Each task was committed atomically:

1. **Task 1 + Task 2: Compose Hot Reload 1.2.0 + MCP server in :showcase only (HRM-01, TOOL-12)** - `bb8c716` (build)

_Task 1 has no separate commit — the plan defines it as a pre-install investigation whose log entry is committed together with Task 2, as written in the plan._
_Task 3 (checkpoint:human-action) is not committed by this session — it is a human action handed to the maintainer; see "Next Phase Readiness"._

## Files Created/Modified
- `gradle/libs.versions.toml` - `composeHotReload = "1.2.0"` version + `compose-hot-reload` plugin entry (`org.jetbrains.compose.hot-reload`)
- `showcase/build.gradle.kts` - `alias(libs.plugins.compose.hot.reload)` in `plugins {}`; new `tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>()` block forwarding the four `aero.*` launch parameters
- `.mcp.json` (new) - `compose-hot-reload` MCP server entry, exact MCP-HOWTO shape (`cmd /c .\gradlew.bat --no-daemon --quiet --console=plain hotMcpServer`)
- `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` - Step 4 pre-install section + Task 2 findings section (task names, task type FQN, stdio check, hotRun smoke with JBR/listener evidence, full isolation checkpoint trace, and the maintainer's recorded accept-as-is decision)

## Decisions Made
See frontmatter `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

None — no Rule 1/2/3 auto-fixes were needed; every command in the plan ran as written.

### Rule 4 — Architectural/verification-gate checkpoint (resolved by maintainer decision)

**1. Unscoped `:library:dependencies` hot-reload check failed (64, not 0)**
- **Found during:** Task 2 (Isolation verification paragraph)
- **Issue:** The plan's isolation check `./gradlew -q :library:dependencies | grep -ic hot-reload` requires `0`; it returned `64`. Per the plan's own text ("Any difference or alpha → STOP and ask"), this halted Task 2 before its commit.
- **Investigation:** Every hit was attributed to its exact owning Gradle configuration (not a text-proximity guess): all 64 are confined to `devCompileClasspath`, `devImplementation`, `devRuntimeClasspath`, `composeHotReloadDevRuntimeClasspath`, `composeHotReloadDevDevRuntimeClasspath`, `composeHotReloadDevTestRuntimeClasspath`, `composeHotReloadMcp`, `composeHotReloadRuntime` — auxiliary "dev tooling" configurations. `:library`'s actual compile/publish surface (`compileClasspath`, `runtimeClasspath`, `api`, `apiElements`, `runtimeElements`, `implementation`, `compileOnly`, `runtimeOnly`, `testCompileClasspath`, `testRuntimeClasspath`) has zero hits. Reverted `gradle/libs.versions.toml`/`showcase/build.gradle.kts` to the Plan 06 commit (Hot Reload plugin applied nowhere in the build), re-ran the identical check — still `64` — proving the exposure is intrinsic to Compose Multiplatform 1.12.0 itself (already committed/gated in Plan 06), not to this task's plugin application. Restored the Task 2 edits afterward.
- **Resolution:** Returned a decision checkpoint with the full evidence rather than guessing or silently proceeding. The maintainer chose **accept as-is**: HRM-01's actual requirement (`:library`'s build file/POM/module metadata unchanged) is satisfied; the auxiliary-configuration exposure is a CMP-1.12.0-intrinsic characteristic of every subproject, not a Hot Reload-plugin leak, and disabling it would require disabling Compose Multiplatform 1.12.0's own bundled dev-tooling behavior — out of bounds under the phase's global stop rule.
- **Files modified:** none beyond the planned Task 2 files; `.planning/phases/21-migration-release-3-1-0/21-TOOLCHAIN-LOG.md` records the full trace and the maintainer's decision.
- **Verification:** POM byte-identical, module.json identical (checksum-filtered), `git diff --stat HEAD -- library/` empty, `runtimeClasspath`-scoped hot-reload count `0`, `MATERIAL3 OK`, `AERO_TEST_COUNT total=541 skipped=0` — all confirmed before and reconfirmed after the checkpoint resolved.
- **Committed in:** `bb8c716`

---

**Total deviations:** 0 auto-fixed. 1 Rule 4 architectural/verification-gate checkpoint, resolved by explicit maintainer decision (accept as-is), fully investigated and documented before the decision was requested.
**Impact on plan:** No scope or design change — the plan's implementation (catalog, forwarding block, `.mcp.json`) is exactly as specified. Only the interpretation of one verification gate's literal wording (unscoped vs. publication-scoped classpath check) was clarified by the maintainer.

## Issues Encountered
None beyond the Rule 4 item above.

## User Setup Required

**Task 3 requires a manual action.** The Compose Hot Reload MCP server (`compose-hot-reload`, defined in `.mcp.json`) only attaches at Claude Code session start. To activate it:
1. Fully close and restart Claude Code in `C:\1A_WORK\ui_lib` (not `/mcp reconnect`, not continuing the old session — a full restart is required).
2. If Claude Code asks whether to allow the `compose-hot-reload` MCP server from `.mcp.json`, allow it.
3. Continue the phase with `/bm:execute-phase 21` (resumes at Plan 21-08).

Plan 21-08's Task 1 verifies the `mcp__compose-hot-reload__*` tools are present; if they are missing, it reports the drive-letter-case project-key issue noted in `reference_windows_mcp_showcase_capture.md` as the likely cause.

## Next Phase Readiness
- Hot Reload + MCP server are installed and proven working end-to-end (stdio, launch-parameter forwarding, JBR linkage, loopback-only listeners) except for the one step no automation can perform: the Claude Code restart itself.
- `:library`'s published artifact is unaffected — Plan 21-08 onward can proceed on the isolation guarantee as accepted by the maintainer.
- `tools/verify/check-material3.sh` remains clean and reusable for any later plan touching dependencies.
- No blockers for Plan 21-08 beyond the pending restart — its Task 1 is exactly the acceptance check for this plan's Task 3.

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*

## Self-Check: PASSED

All 4 created/modified files found on disk (`.mcp.json`, `gradle/libs.versions.toml`, `showcase/build.gradle.kts`, `21-TOOLCHAIN-LOG.md`); commit `bb8c716` found in git history.
