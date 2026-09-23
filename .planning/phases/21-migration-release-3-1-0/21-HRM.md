# Phase 21 — Hot Reload MCP verification (HRM-02, HRM-03)

Measured by the agent on 2026-09-23 against the showcase launched with
`./gradlew :showcase:hotRun --mainClass=com.mordred.showcase.MainKt -Paero.scheme=AeroDark -Paero.section=Overlays`
(normal, focusable window — no `-Paero.capture`). Display scale during every measurement: **100 %
(96 DPI)** — `GetDpiForWindow` on the showcase window returned 96 for every capture. Window
handles below are numbers only; no window title is recorded (T-21-02). The server's
`take_screenshot` tool was never called.

## HRM-02 — connection, section/theme, triggers, reload

### MCP configuration fix found on first contact

The first session after the Plan 07 hand-off found `.mcp.json` running the unqualified task
`hotMcpServer`. Compose Multiplatform 1.12.0 registers that task on every module, so Gradle
started **two** `ComposeHotReloadMcp` JVMs — one per module (pidFile
`showcase\build\run\main\main.pid` and `library\build\run\main\main.pid`) — reading the same
stdin and writing the same stdout. Effects observed in the Claude Code session: consecutive
`status` calls alternated between `connected:true` and `connected:false`; `list_windows` once
answered "No application is currently connected" while the showcase was running; then
`get_semantic_tree` and `status` each hung with no reply until the 1800 s tool timeout.

Fix (`883a8d3`): `.mcp.json` now runs `:showcase:hotMcpServer`. A manual stdio probe of the
qualified task started exactly one `ComposeHotReloadMcp` JVM (showcase pidFile) and answered
`status` / `list_windows` / `get_semantic_tree` in ~0.1 s each. After the maintainer restarted
Claude Code, each server process for this repository ran the qualified task with its own
stdio. The first few seconds after a hotRun launch can still answer `connected:false` while the
server attaches to the app.

### Connection (after the restart)

| Check | Result |
|---|---|
| `status` before the app was launched | `{"connected":false}`, immediate |
| `status` after launch (3 calls in a row) | `connected:true`, `buildContinuous:false`, `reloadState:"ok"` every time |
| `list_windows` | one window: id `82e9a667-…`, 1200 x 800 at (48, 48) |
| `get_semantic_tree` | single root; section heading `Overlays`, `Open dialog`, `Hover me`, `Info`; none of the Buttons-section texts (`Buttons`, `Save Changes`) |
| JVM running the app | JetBrains Runtime 21.0.9 (`C:\Users\1\.jdks\jbr-21.0.9`), per the hotRun banner and `AERO_READY ... jvm=21.0.9/JetBrains_s.r.o.` |

**Theme and section reach hotRun:** `AERO_READY scheme=AeroDark section=Overlays ...
background=FF0A0A1A`; the PrintWindow capture's background check against `FF0A0A1A` passed
(AeroDark); the semantic tree contains the Overlays section only.

**Launch-activation fact (app start-up, not MCP):** measured with the probe before the launch
and after the window appeared. Launch 1 (session before the restart): the cursor was unchanged
but the foreground window became the showcase's own hwnd. Launch 2 (after the restart): cursor
and foreground both unchanged. So the showcase's own start-up *can* take the foreground
(1 of 2 launches). Plan 11 must take its foreground baseline after the window has appeared and
been sent to the bottom, not before the launch.

### Addressable triggers

No `testTag` was needed — every trigger is unique by its text in the tree. No showcase section
file was changed.

| Use | Component | Tree identification | Section |
|---|---|---|---|
| Plan 11 D-08 | AeroDialog | `role:Button`, text `Open dialog` (unique) | Overlays |
| Plan 11 D-08 | AeroAlertDialog | `role:Button`, text `Info` (unique exact text; the banner text `Info banner with dismiss button.` is a different node) | Overlays |
| HRM-03 click target | AeroOutlinedButton `onClick = {}` inside AeroTooltip | `role:Button`, text `Hover me` (unique; the click opens nothing, the tooltip needs hover which MCP cannot do) | Overlays |
| never clicked | AeroFilePicker | — (opens the native OS file dialog; stays on the VER-09 unconfirmed list, D-08) | — |

`21-UITEST-COVERAGE.md` marks no other component "after-only via MCP (D-08)": all 13
`Popup(`-based components were captured by the UI tests.

### Reload finding — **picked up without restarting the showcase**

Probe edit: `AeroNotificationBanner.kt:70` `contentDescription = "Close notification"` →
`"Close notification HRMPROBE"`. `./gradlew reload` → `:library:compileKotlin`, `:library:jar`,
`:showcase:hotReloadMain`, `:showcase:reload`, `BUILD SUCCESSFUL in 49s`. MCP `status` then
reported `successfulReloads:1`, and the tree showed `Close notification HRMPROBE` on the Info
banner's close button (banner node ids changed 62 → 112, confirming ids change after
recomposition). The file was restored with `git checkout --`, `./gradlew reload` ran again
(`BUILD SUCCESSFUL in 24s`), and the tree showed `Close notification` again.
`git status --porcelain -- library/` was empty afterwards; the probe edit was never committed.

## HRM-03 — non-interference

Each repetition: probe (`tools/capture/Test-NonInterference.ps1`: `GetCursorPos` +
`GetForegroundWindow` → `{CursorX, CursorY, ForegroundHwnd}`) → operation → probe. Consecutive repetitions share the
probe between them. Conditions: **bottom** — `SetWindowPos(HWND_BOTTOM, 0x13)`,
`Test-AeroOccluded` = true; **covered** — `Show-AeroOccluder` placed a magenta non-activating
form directly above the showcase, `Test-AeroOccluded` = true; **minimized** —
`ShowWindow(h, 7)` (SW_SHOWMINNOACTIVE), `IsIconic` = true. Click target: `Hover me`, tree
re-read before every click. Showcase hwnd: 50333616.

| Condition | Operation | Reps | Cursor unchanged | Foreground unchanged | Notes |
|---|---|---|---|---|---|
| bottom | PrintWindow capture | 3 | 3/3 | 3/3 | sanity background `FF0A0A1A` passed; the three frames are pixel-identical |
| bottom | `get_semantic_tree` | 3 | 3/3 | 3/3 | rep 2 redone, see "External activity" below |
| bottom | `click` (`Hover me`) | 3 | 3/3 | 3/3 | `{"success": true}` each time |
| covered | PrintWindow capture | 3 | 3/3 | 3/3 | frames pixel-identical to the bottom frame (0 differing px), no magenta 8x8 block |
| covered | `get_semantic_tree` | 3 | 3/3 | 3/3 | full tree returned |
| covered | `click` (`Hover me`) | 3 | 3/3 | 3/3 | `{"success": true}` each time |
| minimized | `get_semantic_tree` | 3 | 3/3 | 3/3 | clean error `No application window is currently available.`, immediate, no hang |
| minimized | `click` (`Hover me`) | 3 | 3/3 | 3/3 | same clean error, immediate |
| minimized | PrintWindow capture | 3 | 3/3 | 3/3 | re-minimized before each rep; `WasIconic:true`, restored with SW_SHOWNOACTIVATE; frames pixel-identical to the bottom frame |

Foreground hwnd values: 7012418 at the start of the matrix, 2689236 from bottom/tree rep 2
onwards. Neither is the showcase (50333616). Cursor: (1238, 451) at the start, (754, 893–894)
after the external change below.

**External activity (one repetition, redone):** during bottom / `get_semantic_tree` rep 2 the
cursor moved from (1238, 451) to (754, 894) and the foreground changed 7012418 → 2689236, and
the showcase window became minimized. Both foreground windows belong to the maintainer's own
editor process, not the showcase, and nothing in the agent's calls touched the minimize
button (only node 43 `Hover me` was clicked). Recorded as the maintainer's own desktop
activity. The immediate retry reached no app (`No application window is currently
available.`, because the window was minimized), so it did not count. The window was restored
with `ShowWindow(h, 4)`, which left cursor and foreground unchanged. After that the rep was
redone cleanly. No cell had a change in more than one repetition, so there is no violation.

**Listeners (T-21-03, this session's launch):** app JVM (JBR 21.0.9, `com.mordred.showcase.MainKt`)
— no listening socket; its Hot Reload devtools JVM (`org.jetbrains.compose.devtools.Main`) —
`127.0.0.1:63452`; Gradle daemon — `127.0.0.1:54296`, `127.0.0.1:54313`; Kotlin compile daemon
— `127.0.0.1:17359`. No `0.0.0.0` / `[::]` listener under any PID from this launch.

**Cleanup:** the app was stopped by the PID that owns the showcase hwnd; its devtools JVM and
the hotRun Gradle client exited on their own. Orphan check afterwards: no process whose command
line contains `com.mordred.showcase.MainKt`, `:showcase:hotRun` or the showcase devtools. The
Gradle and Kotlin daemons were left to idle out. `./gradlew --stop` was not used, because the
live MCP server runs under Gradle too.

**Fact:** Capture, tree dump and click left the cursor and the active window unchanged in
27/27 measurements at 100 % (96 DPI), including with the showcase covered and minimized. With
the window minimized, the tree dump and click fail at once with a clean "no window" error.
`take_screenshot` was not used.
