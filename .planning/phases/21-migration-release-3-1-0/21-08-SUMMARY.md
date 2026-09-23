---
phase: 21-migration-release-3-1-0
plan: 08
subsystem: tooling
tags: [compose-hot-reload, mcp, showcase, printwindow, non-interference, windows]

# Dependency graph
requires:
  - phase: 21-07
    provides: "Compose Hot Reload 1.2.0 in :showcase, hotRun forwarding of aero.* launch parameters, .mcp.json"
provides:
  - ".mcp.json runs the module-qualified `:showcase:hotMcpServer` (one MCP server instead of one per module on a shared stdio)"
  - "HRM-02 facts: MCP connected to a hotRun showcase on a chosen theme + section; every Plan 11 trigger unique by text; `./gradlew reload` picks up :library edits without a restart"
  - "HRM-03 fact: capture, tree dump and click left cursor and foreground unchanged in 27/27 measurements (bottom, covered, minimized; 96 DPI)"
  - "21-HRM.md — trigger table and launch-activation fact that Plan 11 builds on"
affects: [21-10, 21-11, 21-12]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Non-interference matrix: probe → operation → probe, with the probe shared between consecutive reps; one changed rep = external activity, redone"
    - "Before relying on MCP: exactly one ComposeHotReloadMcp JVM per client, and several `status` calls in a row answer connected:true"

key-files:
  created:
    - .planning/phases/21-migration-release-3-1-0/21-HRM.md
  modified:
    - .mcp.json

key-decisions:
  - "`.mcp.json` must name the module (`:showcase:hotMcpServer`): CMP 1.12.0 registers `hotMcpServer` on every module, and the bare name starts one MCP JVM per module on one shared stdio. That routes calls randomly and can hang a call for the full 1800 s tool timeout"
  - "No showcase testTag added: `Open dialog`, `Info` and the HRM-03 target `Hover me` are unique by text; AeroFilePicker is never clicked"
  - "The showcase's own start-up took the foreground in 1 of 2 launches, so Plan 11's foreground baseline is taken after the window appears and is sent to the bottom"

requirements-completed: [HRM-02, HRM-03]

# Metrics
duration: 93min wall clock, of which about 60 min were two MCP calls hanging to the 1800 s timeout before the .mcp.json fix, plus one maintainer restart of Claude Code
completed: 2026-09-23
---

# Phase 21 Plan 08: Hot Reload MCP verification Summary

**The Compose Hot Reload MCP server now runs for `:showcase` only. It drives a hotRun showcase on a chosen theme and section, reloads `:library` edits live, and left the maintainer's cursor and active window untouched in 27/27 measured captures, tree dumps and clicks, including with the window covered or minimized.**

## Performance

- **Duration:** 93 min wall clock (about 60 min lost to two hung MCP calls before the fix)
- **Started:** 2026-09-23T18:17:04Z
- **Completed:** 2026-09-23T19:51:00Z
- **Tasks:** 2/2
- **Files modified:** 2 (`.mcp.json`, `21-HRM.md`)

## Accomplishments

- Found and fixed the Plan 07 `.mcp.json` defect on first contact: two MCP servers shared one stdio, so calls went to a random module and some hung. The qualified task was proven with a manual stdio probe before the restart and confirmed in the restarted session.
- HRM-02: `status` connected:true, `list_windows` one window, and the tree plus `AERO_READY` plus a PrintWindow background check prove that AeroDark + Overlays reached hotRun. All Plan 11 triggers are unique by text. Reload finding: **picked up without restarting**.
- HRM-03: a 3 x 3 matrix (bottom / covered / minimized x PrintWindow / `get_semantic_tree` / `click`), 3 reps each, 27/27 clean. Covered and minimized captures are pixel-identical to the bottom capture. With the window minimized, the MCP calls fail at once with a clean error. Listeners are loopback-only. `take_screenshot` was never called.
- Gate: `AERO_TEST_COUNT total=541 skipped=0 expected=541`, `BUILD SUCCESSFUL`.

## Task Commits

1. **Deviation fix: MCP server for :showcase only** - `883a8d3` (fix)
2. **Restart hand-off recorded** - `68cee5f` (docs)
3. **Task 1 + Task 2: HRM-02 / HRM-03 report** - `589d2e0` (docs). The plan's `feat` commit with showcase testTags became a docs-only commit, since no tag was needed.

## Files Created/Modified

- `.mcp.json` - `hotMcpServer` → `:showcase:hotMcpServer`
- `.planning/phases/21-migration-release-3-1-0/21-HRM.md` - HRM-02 connection facts, the MCP fix, trigger table, reload finding; HRM-03 matrix, external-activity note, listeners, one-line fact, DPI

## Decisions Made

See frontmatter `key-decisions`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `.mcp.json` started one MCP server per module on a shared stdio**
- **Found during:** Task 1 (first `status` / `list_windows` calls)
- **Issue:** The unqualified `hotMcpServer` ran in both `:library` and `:showcase`. Two `ComposeHotReloadMcp` JVMs read the same stdin. `status` alternated connected true/false, and `get_semantic_tree` and then `status` hung for 1800 s each.
- **Fix:** `:showcase:hotMcpServer`. A manual stdio probe showed one JVM with the showcase pidFile and answers in ~0.1 s. The maintainer restarted Claude Code so the session picked up the fix (a human action, the second restart in this phase).
- **Files modified:** `.mcp.json`
- **Verification:** after the restart, 3 consecutive `status` calls answered connected:true and every later MCP call answered within about a second
- **Committed in:** `883a8d3`

### Plan adjustments

- **No testTags, so no showcase section file changed:** the plan adds tags only where a trigger is not unique; none needed one.
- **No relaunch between Task 1 and Task 2:** Task 2's launch parameters are identical to Task 1's (AeroDark, Overlays, no capture flag). The Task 1 launch was reused; the reload probe had already been reverted and reloaded back.
- **One external-activity redo:** during bottom / tree rep 2 the maintainer switched between two editor windows and the showcase got minimized. That rep was redone after `ShowWindow(h, 4)` (which itself left cursor and foreground unchanged).

---

**Total deviations:** 1 auto-fixed (Rule 1 bug inherited from Plan 07), 3 plan adjustments with no scope change.
**Impact on plan:** HRM-02 and HRM-03 are met as written. The fix is required for Plans 10–12, which rely on MCP.

## Issues Encountered

- Two MCP calls hung to the 1800 s timeout before the fix (about 60 min lost); the maintainer had to restart Claude Code again.
- `AeroCapture.ps1` sets `Set-StrictMode -Version 2`, which breaks the harness's PowerShell wrapper when dot-sourced directly in the PowerShell tool. The helpers were run through `powershell.exe -File` instead. Tooling only; no code change.

## Next Phase Readiness

- Plan 21-09 (kotlinx-coroutines / kotlinx-datetime / JUnit bumps) has no MCP dependency and can run as a normal executor.
- For Plan 21-11: triggers are in `21-HRM.md`. The showcase can take the foreground on start-up, so take the foreground baseline after the window is at the bottom. AeroDialog / AeroAlertDialog open real OS windows, and whether that steals focus is exactly what Plan 11's stop rule checks.

## Self-Check: PASSED

- `21-HRM.md` exists; contains `HRM-02`, `HRM-03`, `GetForegroundWindow`; the HRM-03 table has 9 rows, each with Reps = 3; no window title text
- `git status --porcelain -- library/` is empty
- commits `883a8d3`, `68cee5f`, `589d2e0` are in history
- `.captures/old-kt2.4.10-cmp1.11.1/` still holds 1152 files

---
*Phase: 21-migration-release-3-1-0*
*Completed: 2026-09-23*
