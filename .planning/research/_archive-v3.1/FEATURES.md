# Feature Research — Compose Hot Reload MCP Server as an Agent-Driven Visual QA Mechanism

**Domain:** Agent-driven GUI debugging/QA loop for a Compose Desktop (JVM) component library showcase
**Researched:** 2026-09-21
**Confidence:** HIGH for mechanism/lifecycle/tool-list (read directly from JetBrains source at the exact `v1.2.0` tag, commit `3315f8dd8f5cc4db1359ff13b2be178ba19486e3`, repo `JetBrains/compose-hot-reload`); MEDIUM for forward-looking notes (later releases); LOW/marked where inferred or unverifiable.

All source citations below are file paths relative to the repo root, at tag `v1.2.0` (commit `3315f8dd8f5c`), unless stated otherwise. Compose Hot Reload 1.2.0 pairs with Compose Multiplatform 1.12.0 (confirmed: [JetBrains blog, Compose Multiplatform 1.12.0](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/)).

---

## 1. THE DECISIVE FINDING — mechanism, and the maintainer's acceptance question

This is the answer to "does the real OS cursor move, is focus stolen, does it work occluded/minimized?" Read directly from source, not inferred from docs.

**There are two completely different mechanisms in v1.2.0, and they behave oppositely:**

### A. `take_screenshot` — real OS-level screen capture (`java.awt.Robot`)

`hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/screenshotHandler.kt:58-71`:

```kotlin
internal fun captureWindow(window: Window): Try<BufferedImage> {
    return Try {
        val robot = Robot()
        val location = window.locationOnScreen
        val insets = window.insets
        val rect = Rectangle(
            location.x + insets.left, location.y + insets.top,
            window.width - insets.left - insets.right,
            window.height - insets.top - insets.bottom,
        )
        robot.createScreenCapture(rect)
    }
}
```

This is `java.awt.Robot.createScreenCapture` — a literal screen-pixel read at the window's on-screen rectangle. It is **not** off-screen rendering of the `ComposeScene`/`ComposeWindow` contents (no `ImageComposeScene`, no `captureContentToImage()` — those only exist starting `v1.3.0-alpha02`, gated to Compose Multiplatform 1.13+, with automatic Robot fallback below that — see §7 "Known issues"). Our milestone installs 1.2.0/CMP 1.12.0, so **Robot is the only path available.**

Consequences, derived directly from this code (HIGH confidence):
- **Cursor:** `Robot.createScreenCapture` does not move the mouse. No cursor movement for this tool.
- **Focus:** reading screen pixels does not touch AWT/Swing/OS focus. No focus stolen.
- **Occluded window:** Robot reads whatever is actually on top of the screen at that rectangle. If another window (even partially) covers the showcase, the screenshot will contain the occluding window's pixels, not the showcase's Compose content. **`take_screenshot` does NOT work correctly when occluded.**
- **Minimized window:** see the shared finding in §B below — it fails even before Robot gets a chance to run, because the window disappears from the registered-window list entirely.

### B. `click` / `long_click` / `type_text` / `scroll` / `scroll_to_index` — in-process semantics-action invocation, no OS input at all

`hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/uiActionHandler.kt:67-99`:

```kotlin
return when (val action = request.action) {
    is UIAction.Click -> invokeNoArgAction(request, node, SemanticsActions.OnClick, "onClick", windowId)
    is UIAction.LongClick -> invokeNoArgAction(request, node, SemanticsActions.OnLongClick, "onLongClick", windowId)
    is UIAction.SetText -> {
        val setText = node.config.getOrNull(SemanticsActions.SetText) ?: return missingAction(...)
        val lambda = setText.action ?: return missingActionLambda(...)
        val handled = lambda.invoke(AnnotatedString(action.text))
        ...
    }
    is UIAction.ScrollBy -> { /* invokes SemanticsActions.ScrollBy lambda directly */ }
    is UIAction.ScrollToIndex -> { /* invokes SemanticsActions.ScrollToIndex lambda directly */ }
}
```

There is **no `java.awt.Robot`, no synthetic `AWTEvent`/`MouseEvent`/`KeyEvent` dispatch, no `ComposeScene.sendPointerEvent`.** The handler walks the in-memory `SemanticsNode` tree (found via reflection on Compose Desktop's internal `ComposeAccessible.getSemanticsNode()`, see `semanticTreeHandler.kt:87-96`) and calls the accessibility-action lambda (`AccessibilityAction<() -> Boolean>.action.invoke()`) directly — exactly the same code path a screen reader uses, entirely inside the JVM, never touching the OS input queue.

`resize_window` (`windowResizeHandler.kt:16-30`) is the same story: `window.setSize(...)` + `window.validate()` — a direct AWT API call, not an OS-level drag of the window border.

Consequences (HIGH confidence, same reasoning as above): **no cursor movement, no focus stolen**, for any of `click`/`long_click`/`type_text`/`scroll`/`scroll_to_index`/`resize_window`. These act purely on the in-memory semantics tree and are **unaffected by screen occlusion** — they will succeed even if another window fully covers the showcase, because nothing about them touches actual screen pixels or input queues.

### C. Minimized window — fails for every window-targeting tool, not just the screenshot one

`hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/window.kt:76-84`:

```kotlin
val windowListener = object : WindowAdapter() {
    override fun windowIconified(e: WindowEvent?) {
        broadcastGone()   // removes this window from WindowsState entirely
    }
    override fun windowDeiconified(e: WindowEvent?) {
        broadcastActiveState()  // re-registers it
    }
    ...
}
```

`broadcastGone()` removes the window from the shared `WindowsState.windows` map (the same map `list_windows` reads and `resolveWindowId` resolves against, `hot-reload-mcp/.../McpServer.kt:919-927`). **Minimizing the showcase window makes it vanish from `list_windows` and every window-targeting tool** (`take_screenshot`, `get_semantic_tree`, `click`, `long_click`, `type_text`, `scroll`, `scroll_to_index`, `resize_window`) **fails with "no application window is currently available"** (or, if an explicit `window_id` was passed, `"Window '...' not found"`). This is a hard, deliberate design choice in the runtime — not a bug the milestone needs to route around, but a real operational constraint: **the agent cannot inspect a minimized showcase at all**, via any tool. It has to be visible (can be occluded for everything except the screenshot; must be non-minimized for everything).

### Summary table — answers the maintainer's acceptance question directly

| Tool | Real cursor moves? | Focus stolen? | Works occluded? | Works minimized? |
|---|---|---|---|---|
| `take_screenshot` | No (Robot reads pixels, doesn't move mouse) | No | **No** — captures whatever is actually on top of the screen at that rect | **No** — window is unregistered on iconify |
| `get_semantic_tree` | No | No | **Yes** — pure in-memory tree walk | **No** — same unregistration |
| `click` / `long_click` / `type_text` / `scroll` / `scroll_to_index` | No | No | **Yes** — invokes the semantics-action lambda in-process | **No** — same unregistration |
| `resize_window` | No | No | **Yes** — direct `window.setSize()` | **No** — same unregistration |
| `status` / `reload` / `await_reload` / `get_logs` / `get_ui_error` / `list_windows` / `restart` / `reset_ui` | N/A (no window targeting, or window-agnostic) | No | Yes | `list_windows` will simply omit the minimized window; other non-window tools are unaffected |

**Bottom line for the milestone's acceptance test:** the "does not move the cursor / does not steal focus" half of the promise holds unconditionally in v1.2.0 for every tool — there is no OS-level input injection anywhere in this server. The "works occluded / works minimized" half is **conditional**: occlusion only breaks `take_screenshot` (garbled/wrong image); minimization breaks *every* window-targeting tool including the screenshot. This is a precise, testable, source-backed claim — see §8 for a concrete verification protocol.

---

## 2. Full tool list (16 tools, v1.2.0)

Source: `hot-reload-mcp/src/main/kotlin/org/jetbrains/compose/reload/mcp/McpServer.kt:160-389`. Every tool returns a `CallToolResult` — either `TextContent` (JSON string, described per row) or, for `take_screenshot`, `ImageContent` (base64 PNG) plus optional `TextContent` confirming a saved path.

| Tool | Required params | Optional params | Target addressing | Returns |
|---|---|---|---|---|
| `status` | — | `max_error_detail_lines` | — | `{"connected":bool, "buildContinuous":bool, "reloadState":"ok"/"reloading"/"failed", "lastError", "lastErrorDetails"?, "successfulReloads":n, "failedReloads":n, "uiErrorWindows"?:[id]}` |
| `reload` | — | `timeout_seconds` | — | `{"success":true,"reloaded":true/false}` or `{"status":"reloading"}` if still running. For non-`--auto` (explicit reload) runs. |
| `await_reload` | — | `timeout_seconds` | — | Same shape as `reload`, but for `--auto` (continuous build) runs — waits for the autonomous reload instead of triggering one. |
| `list_windows` | — | — | — | JSON array of `{"id","title","x","y","width","height"}` |
| `get_ui_error` | — | `window_id`, `max_error_detail_lines` | `window_id` (defaults to first window) | `{"windowId","hasError":bool,"message"?,"stacktrace"?:[line],"stacktraceTruncated"?:n}` |
| `get_logs` | — | `limit` (default 200, 0=all) | — | Plain text, oldest first, tail of the app's `.chr.log` |
| `take_screenshot` | — | `window_id`, `save_to` | `window_id` | Inline base64 PNG (`ImageContent`) + optional "saved to" text |
| `get_semantic_tree` | — | `window_id` | `window_id` | JSON tree (or array of trees — see §4) |
| `click` | `nodeId` | `window_id` | **`nodeId` (integer semantic node id from `get_semantic_tree`)** | `{"success":true}` or error if node lacks `onClick` |
| `long_click` | `nodeId` | `window_id` | node id | same shape, requires `onLongClick` |
| `type_text` | `nodeId`, `text` | `window_id` | node id | Replaces the field's whole content (`SemanticsActions.SetText`), requires `editableText` present on the node |
| `scroll` | `nodeId` | `deltaX`, `deltaY`, `window_id` | node id (scrollable container) | Logical-pixel delta scroll (`ScrollBy` semantic action) |
| `scroll_to_index` | `nodeId`, `index` | `window_id` | node id | For `LazyColumn`/`LazyRow`-style containers (`ScrollToIndex` semantic action) |
| `resize_window` | `width`, `height` | `window_id` | `window_id` | `window.setSize()` + `validate()` |
| `restart` | — | `timeout_seconds` | — | Relaunches the app process with the same args; `{"success":true,"reconnected":bool}` |
| `reset_ui` | — | — | — | Discards the current composition (drops all `remember`-ed state), like the DevTools "Reset UI" button |

**Click/scroll/type target addressing is exclusively by semantic node id** (an integer assigned by Compose's `SemanticsNode.id`, discovered via `get_semantic_tree`) — **not** by test tag, text match, or window coordinates. There is no coordinate-based click tool at all in v1.2.0. This is a hard dependency (§6).

**No tool exists for:** raw keyboard key-press (Tab, Enter, arrow keys, Escape), pointer hover/move, or drag gestures. See §5.

---

## 3. Lifecycle — does `hotMcpServer` launch the app itself?

**No.** `hotMcpServer` and the application process are two entirely independent things that rendezvous through a **pidFile**.

`hot-reload-mcp/src/main/kotlin/org/jetbrains/compose/reload/mcp/McpMain.kt:55-90`: the MCP server's `main()` takes a pidFile path (via `-Dcompose.reload.pidFile=` or CLI arg), then loops: watch the pidFile directory with a `WatchService` until a `orchestrationPort=` line appears, connect, and on disconnect (app exit) **go back to watching and reconnect automatically** — this is the "waits for startup, detects shutdowns, reconnects" behavior mentioned in the 1.2.0 release notes, confirmed in code.

**The app must be started separately**, by a task that writes that pidFile. That task is `hotRun` (for a plain `org.jetbrains.kotlin.jvm` project, matching `:showcase`'s current plugin setup) — confirmed directly by the plugin's own test, `ComposeHotRunTasksTest.kt: "test - default run task name - jvm"` → asserts task names `{"hotRun", "hotDev"}` for this exact plugin combination. **This is a new task registered by the `org.jetbrains.compose.hot-reload` Gradle plugin — it is a different task from the showcase's existing `run` task** (from `compose.desktop.application`). Concretely:

- `hotRun` wires `pidFile`/`argFile` via `configureJavaExecTaskForHotReload` (`hotRunTasks.kt:85-194`), inherits `mainClass` from the same `compose.desktop.application.mainClass` convention the existing `run` task uses (confirmed by `ComposeHotRunTasksTest.kt: "test - mainClass"`), so `com.mordred.showcase.MainKt` carries over with no extra config.
- The showcase's existing `-Paero.scheme=` forwarding hook only matches `if (name == "run")` (`showcase/build.gradle.kts:27-31`) — **it will not apply to `hotRun` as written.** This is a concrete, small dependency for the milestone's implementation phase: either broaden that predicate (e.g. `name == "run" || name == "hotRun"`) or accept that theme preselection needs a separate path for the hot-reload flow.

**Gradle daemon / `--no-daemon` — three independent processes, three independent daemon decisions:**

1. **`hotMcpServer` itself** is a plain `JavaExec` (`UntrackedTask` — "should always run", no up-to-date caching) that just proxies stdio and watches a file; whatever `--no-daemon` flag was passed to invoke it only affects that one Gradle invocation. Matches the milestone's planned `cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer` — confirmed as the exact pattern JetBrains' own docs recommend (`.mcp.json` example: `{"command":"./gradlew","args":["--no-daemon","--quiet","--console=plain","hotMcpServer"]}`, [kotlinlang.org docs](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html)); Windows needs `cmd /c gradlew.bat` instead of `./gradlew` because the wrapper script isn't directly spawnable on Windows — this matches the milestone's own noted decision.
2. **Running the app itself** (`./gradlew hotRun` or `hotRun --auto`) is its own separate, independently-invoked Gradle build.
3. **Every `reload`** (triggered by the MCP `reload` tool, or by `--auto` continuous mode) spawns a **brand-new Gradle sub-process** via `ProcessBuilder`, from *inside* the running application (not from the `hotMcpServer` process) — `hot-reload-devtools/src/main/kotlin/org/jetbrains/compose/devtools/gradle/GradleRecompiler.kt:73-121`. This sub-process's daemon usage is decided independently (`GradleRecompiler.kt:162-170`):
   ```kotlin
   private val useGradleDaemon = run {
       if (HotReloadEnvironment.buildSystem != Gradle) return@run false
       if (!HotReloadEnvironment.gradleBuildContinuous) return@run true   // explicit reload: daemon USED by default
       when (HotReloadEnvironment.launchMode) {
           LaunchMode.Ide, LaunchMode.Detached -> true
           LaunchMode.GradleBlocking -> false                             // continuous + plain gradlew: no daemon
           null -> false
       }
   }
   ```
   Since `hotRun` unconditionally sets `-Dcompose.reload.launchMode=GradleBlocking` (`hotRunTasks.kt:147`) for any plain `./gradlew hotRun` invocation: **one-shot `reload` calls (no `--auto`) spin up their own background Gradle daemon regardless of how `hotMcpServer` or `hotRun` were themselves launched**; only continuous (`--auto`) mode forces `--no-daemon` for the internal rebuild loop. **Practical consequence for the milestone:** passing `--no-daemon` to the `hotMcpServer` task does *not* prevent Compose Hot Reload from leaving its own Gradle daemon running in the background once the agent calls `reload` a first time (unless the showcase is always run with `--auto`). Worth a line in the milestone's own verification notes — it's a background-process hygiene question, not a cursor/focus one.

**Several instances:** `GradleRecompiler` tracks the previous recompile sub-process's pid in a sibling `*.gradle.pid` file and **waits for it to exit before starting a new one** (`GradleRecompiler.kt:51-68`) — concurrent `reload` calls are serialized, not run in parallel, per app instance. Running two independent showcase processes (e.g. two Gradle projects) would need two separate `.mcp.json` server entries, each with its own `hotMcpServer` task/pidFile — the MCP server is a 1:1 bridge to one pidFile.

**How does the agent stop the app?** There is **no `stop`/`shutdown`/`kill` tool in the 16-tool list.** The only lifecycle-adjacent tool is `restart`, which relaunches the app with the same args (`hot-reload-devtools/.../restart.kt:20-49`: spawns a new `java @argfile` process, then sends itself a `ShutdownRequest`) — it does not leave the app stopped. **Cleanly stopping the app is not exposed via MCP at all** in v1.2.0; it requires killing the process outside the protocol (closing the window, Ctrl+C on the `hotRun` Gradle invocation, or `taskkill`/process-manager on Windows). Flag this for requirements: if "the agent tears down the showcase when done" is part of the milestone's workflow, it needs a process-management step outside the MCP tool surface.

---

## 4. Multi-window / popup behavior

**Confirmed by source comment + JetBrains' own regression test** (`hot-reload-runtime-jvm/.../semanticTreeHandler.kt:40-49` and `tests/.../SemanticTreeIntegrationTest.kt`, test `"test - get semantic tree with overlay"`, regression coverage for `CMP-10282`):

> "A Popup renders in its own owner (a separate semantics root) within the same window, exactly like a Dialog / ModalBottomSheet."

This means `Popup`, `Dialog`, and `ModalBottomSheet` on Compose Desktop are **not** separate OS-level `java.awt.Window`s — they render as additional semantics "owners" painted within the *same* underlying `ComposeWindow`'s Skia surface.

- **`get_semantic_tree`:** picks up every open owner automatically. `findAllRootSemanticsNodes()` walks the AWT accessibility tree of the one target window and collects every distinct root; `joinSemanticForest()` returns a single object when there's one root, or a **JSON array of roots** when several are open (main content + an open Popup/Dialog), each flagged `"isDialog":true` / `"isPopup":true`. This library's heavy `Popup`-based dropdowns/date pickers/tooltips/context menus are correctly captured **without any extra plumbing** — HIGH confidence, directly tested by JetBrains.
- **`take_screenshot`:** since Popup content is drawn onto the *same* window's Skia surface (not a separate window), and the screenshot mechanism is `Robot.createScreenCapture` over that window's on-screen rectangle (§1A), an open Popup/Dialog **will appear in the screenshot as long as it is positioned within that window's bounds.** If a popup positions itself partially or fully outside the parent window's rectangle (a real, known risk for this library — see `PROJECT.md`'s open `AeroDropdown popup-offset regression`, and the general pattern of dropdowns/date-range calendars extending below the window edge), **that overflow portion is clipped from the screenshot**, because `Robot` only captures the owning window's own rect. INFERRED from the confirmed mechanism (not a case JetBrains' own tests directly exercise with an out-of-bounds popup), but a direct, low-risk consequence of §1A + this section's "same window" finding — flag as a concrete visual-QA gap for popups near window edges.
- **`click`/`type_text`/etc. on popup content:** `dispatchUIAction` explicitly searches "across all roots so actions also reach nodes inside a Dialog/ModalBottomSheet/Popup" (`uiActionHandler.kt:58`) — confirmed to work by design, node ids from any open root are valid targets.

---

## 5. Limits relevant to visual QA

- **Screenshot resolution/DPI:** the PNG dimensions equal `window.width/height` in on-screen (AWT-reported) pixels, adjusted for `window.insets` — i.e. whatever physical pixels Windows' display scaling actually produces for that window, with **no explicit DPI-normalization logic** in `screenshotHandler.kt`. This directly inherits the same 125%/200% DPI sensitivity the project already flagged as deferred in v3.0 (SHW-16). One integration test (`SemanticTreeIntegrationTest.kt`) pins `sun.java2d.uiScale=1` specifically *because* node bounds otherwise vary with the host's display scale — corroborates that DPI scaling is a real, JetBrains-acknowledged source of non-determinism for both the semantic tree and (by the same reasoning) screenshots.
- **Hover states:** **there is no hover/pointer-move tool.** The 16-tool list has nothing that simulates `PointerEventType.Enter`/`Move`. Since `click` bypasses real pointer input entirely (§1B), it also does not trigger whatever `Modifier.hoverable`/`InteractionSource` hover state the library's custom Aero primitives track (`rememberAeroInteractionState()`, per `PROJECT.md`'s Phase 17/18 hover/press/focus calibration work). **Hover cannot be exercised or visually verified by the agent at all in v1.2.0.**
- **Keyboard key presses (Tab, Enter, arrows, Escape) vs. `type_text`:** `type_text` only replaces a text field's *entire content* via the `SetText` semantics action (`uiActionHandler.kt:74-81`) — it is not character-by-character key dispatch and cannot press Tab to move focus, Enter to submit, or arrow keys to navigate a list/calendar. **There is no generic "press key" tool.** This directly blocks agent self-verification of the library's own focus-visible mechanism (v3.0's "клавиатура показывает фокус, указатель — нет" — keyboard shows the focus ring, pointer doesn't), which is exactly the kind of thing Tab-navigation would need to exercise.
- **Drag gestures (sliders, split panes, resizable columns):** the only motion primitives exposed are `scroll` (`ScrollBy` semantics action, logical-pixel delta) and `scroll_to_index` — both require the target node to expose those specific semantic actions (typically `LazyColumn`/scrollable containers). **There is no drag/pointer-down-move-up tool.** This library deliberately implements slider/split-pane/panel-group dragging with manual `awaitPointerEventScope` loops (its own documented `PITFALL-03`, precisely *because* `detectDragGestures`'s `touchSlop` breaks Canvas drag on Desktop) — none of that is a semantics `ScrollBy`/`ScrollToIndex` action, so **`AeroSlider`, `AeroRangeSlider`, `AeroSplitPane`, `AeroPanelGroup`'s resize-drag, and `AeroDataTable`'s column-resize cannot be driven by any MCP tool in v1.2.0.**

---

## 6. Table stakes vs. differentiators vs. anti-features for this agent-side visual QA loop

| Capability | Category | What the agent can verify | Complexity | Notes |
|---|---|---|---|---|
| Reload-and-screenshot round trip (`reload`/`await_reload` → `take_screenshot`) | **Table stakes** | Fully — static layout, colors, gradients, text, icons across 3 themes | LOW | Direct replacement for the manual windows-mcp screenshot recipe; no cursor/focus cost (§1) |
| Structural presence/absence checks (`get_semantic_tree`) | **Table stakes** | Fully — role, text, enabled/disabled, selected, focused, testTag, bounds, actions list | LOW | Cheap, in-process, works even when occluded; good for "did this component render at all" gates |
| Click-driven state changes (open a dropdown, toggle a switch, expand a panel) | **Table stakes** | Fully, for anything wired through `onClick`/`onLongClick` | LOW–MEDIUM | Needs testTag/discoverable semantics on the target (see Dependencies below) |
| Popup/Dialog content verification (dropdown lists, date pickers, tooltips, context menus) | **Table stakes** | Fully for semantic tree; **partially** for screenshot (clipped if popup overflows window bounds, §4) | MEDIUM | Library uses `Popup` pervasively — this is the single most load-bearing finding for this project |
| Text field content entry (`type_text`) | **Table stakes** | Fully, but as one-shot "set the whole value", not incremental typing | LOW | Fine for verifying rendered state after entry; cannot test incremental validation-while-typing behavior |
| List/table scroll verification (`AeroDataTable`, `AeroTreeView` virtualization) | **Differentiator** | Fully via `scroll`/`scroll_to_index`, if the container exposes those semantic actions | MEDIUM | Needs a scoped check per component — Compose's default `LazyColumn` semantics usually expose `ScrollToIndex`; custom `AeroScrollArea` wrapping needs verifying it doesn't suppress the action |
| Hover-state visual QA (`AeroButton`/`AeroSwitch`/`AeroSegmentedControl`/`AeroListItem` hover) | **Cannot self-verify — needs the human** | Not at all — no hover tool exists (§5) | N/A | The single biggest gap vs. this library's v3.0 investment in hover states |
| Drag-driven components (`AeroSlider`, `AeroRangeSlider`, `AeroSplitPane`, `AeroPanelGroup` resize, `AeroDataTable` column resize) | **Cannot self-verify — needs the human** | Not at all — no drag tool exists (§5) | N/A | Second biggest gap; covers a meaningful fraction of the ~50-component showcase |
| Keyboard focus-visible / Tab-order QA | **Cannot self-verify — needs the human** | Not at all — no key-press tool, `type_text` isn't keystroke-level (§5) | N/A | Directly relevant: v3.0 built a dedicated focus-visible mechanism this milestone can't exercise |
| Full-frame reliance on `take_screenshot` alone, ignoring occlusion risk | **Anti-feature** | — | — | A verification loop that only screenshots (no `status` check, no occlusion guard) will silently pass garbage frames if another window briefly covers the showcase during an automated pass — see the protocol in §8 |
| Treating `reload` and `await_reload` as interchangeable | **Anti-feature** | — | — | `reload` is for explicit (non-`--auto`) mode and triggers a build itself; `await_reload` is for `--auto` continuous mode and only *waits*. Using the wrong one either double-triggers a build or hangs waiting for one that will never start on its own |
| Building a custom coordinate-click fallback "just in case" | **Anti-feature** | — | — | There is no coordinate-click tool by design (§2) — components without proper semantics (missing `testTag`/`onClick`/role) are a real gap to close in the *library*, not to work around with pixel-coordinate hacks that reintroduce the exact cursor-stealing risk this milestone exists to remove |

---

## 7. Known issues

GitHub Issues for this repo are **closed to new reports and effectively archived** — the pinned notice (issue #488) states: *"we've disabled new issue form on GitHub and imported all most existing issues to JetBrains' YouTrack."* Current bug tracking lives at YouTrack project `CMP`, group "Hot Reload" (`https://youtrack.jetbrains.com/issues?q=project:%20{Compose%20Multiplatform}%20Library%20group:%20{Hot%20Reload}`). YouTrack's issue list is a client-rendered app that couldn't be scraped via WebFetch in this session — **could not enumerate current open bugs; treat this as a gap, not a "no known issues" finding.**

From what could be verified:
- **Screenshot mechanism is evolving away from `Robot`:** confirmed via the `v1.3.0-alpha02` release notes — *"Screenshots are captured natively using `ComposeDesktopEntryPoint.captureContentToImage()` for Compose 1.13+ with an automatic fallback to the AWT `Robot` path for previous Compose versions."* MEDIUM confidence (release notes, not source-read at that tag). **Directly relevant to this milestone:** v1.2.0/CMP 1.12.0 (what this milestone installs) is squarely in the "Robot fallback" bucket — the occlusion/minimization limitations in §1 are not a permanent architectural ceiling, they're specific to the version this milestone pins. A future dependency bump to Hot Reload ≥1.3.0 + CMP ≥1.13.0 could remove the occlusion limitation for screenshots specifically (worth a note in `Next Milestone Goals`).
- **DPI/display-scale sensitivity is JetBrains-acknowledged**, not just this project's own concern: the `PinUiScaleExtension`/`sun.java2d.uiScale=1` workaround exists in JetBrains' own integration test suite specifically to make semantic-tree bounds deterministic across machines (`SemanticTreeIntegrationTest.kt:213-220`). Corroborates that non-100%-DPI runs are a known soft spot, consistent with this project's own deferred SHW-16 125%/200% DPI gap.
- **Old, likely-stale, low-relevance:** issue #70 ("Dev tooling window crashes on trying to open it in Windows 11") — a `NoSuchMethodError` in the separate DevTools overlay window (not the MCP server), reported against a much older Compose snapshot (`1.8.0+dev2030`), left open and labeled "up for grabs" as of the web search. LOW confidence this is still reproducible against 1.2.0/CMP 1.12.0; flagging only because it's Windows + this repo's dev-tooling window, and it incidentally notes a Command Prompt window appearing when running via Gradle on Windows (worth a passing check during setup, not a blocker).
- **MCP server is explicitly "experimental"** per both the repo README and kotlinlang.org docs — API/behavior stability across patch releases is not guaranteed; re-verify tool schemas after any Hot Reload version bump.

---

## 8. Dependencies on existing showcase code, and a concrete testable "does not interfere" protocol

### Dependencies this milestone creates for library/showcase code

1. **Every clickable target the agent needs to drive must already expose the right semantics** — `Role`/`onClick`/`onLongClick`/`editableText`/`ScrollBy`/`ScrollToIndex`, discoverable via `get_semantic_tree`. Compose's built-in `clickable`/`Button`/`TextField`/`LazyColumn` modifiers already wire these by default, so this is likely **already satisfied for most of the ~50 components** without extra work — but it is a real, checkable dependency, not an assumption: any component built on a bare `Modifier.pointerInput`/custom gesture detector (rather than `clickable`/`toggleable`/`selectable`) will **not** expose `onClick` in the semantics tree and will be invisible to the `click` tool. Given this library's documented pattern of manual `awaitPointerEventScope` for drag components (§5), those exact components are both (a) undriveable via `click`/`scroll` and (b) the ones most likely to lack semantics entirely — worth an explicit per-component audit pass before relying on agent-driven clicks for anything beyond simple buttons/switches/segments.
2. **`testTag` is not required for `click`/`type_text` targeting** (targeting is by semantic node `id`, discovered fresh each `get_semantic_tree` call) — but `testTag` **does** show up in the returned JSON (`semanticTreeHandler.kt:162`) when present, making it far easier for the agent to reliably re-identify "the same" node across reloads (node ids are not guaranteed stable across a reload/recomposition). **Recommendation for requirements: adding `Modifier.semantics { testTag = "..." }` to interactive elements in the showcase (not necessarily the library itself) is a low-cost, high-value addition** for a stable agent-driven QA loop, even though it's not a hard MCP protocol requirement.
3. **Popup-overflow risk is now concretely testable, not theoretical** (§4): any showcase section whose `Popup`/dropdown/calendar can extend past the parent window's edge is a screenshot blind spot. Given the window is fixed-size `undecorated` chrome (not resizable-to-fit), this is a realistic scenario for the DataRangePicker/ColorPicker/wide dropdowns already in the showcase.
4. **The showcase's `-Paero.scheme=` forwarding hook needs a one-line change** (`showcase/build.gradle.kts:27-31`, `if (name == "run")`) to also match whatever the `hotRun` task is actually named once the plugin is applied, or theme preselection silently stops working for the hot-reload flow.

### Concrete, testable protocol for "MCP debugging does not interfere with the human using the computer"

Given §1's precise, source-backed claims, the acceptance test can be built directly around them rather than around vague "seems fine" observation:

1. **Cursor-position proof:** record `Cursor.getSystemCursor` / actual mouse coordinates (any OS-level poll, e.g. via `java.awt.MouseInfo.getPointerInfo()` in a tiny probe, or simply the human's own mouse) immediately before and after a full agent pass (`reload` → `take_screenshot` → `get_semantic_tree` → several `click`/`type_text`/`scroll` calls). **Expected: byte-identical position** — the mechanism findings in §1 predict zero cursor movement for any of these calls. A failing result would falsify the mechanism read here and must stop the milestone per its own locked decision ("если MCP двигает реальный курсор... остановка и вопрос мейнтейнеру").
2. **Focus-proof:** have the human keep an unrelated window (e.g. a text editor) focused and actively typing in a loop (or just note focus via `GetForegroundWindow` on Windows) while the agent runs the same pass. **Expected: the foreground window never changes to the showcase.** This directly exercises §1's "no focus stolen" claim for `click`/`type_text` in particular, since those are the tools most likely (if the mechanism read were wrong) to require the target window to have input focus.
3. **Occlusion test, split by tool (this is the one place the milestone's target feature list should be split, given §1):**
   - Cover the showcase window with another window, then call `get_semantic_tree` + `click` + `scroll`: **expect success** (in-process, unaffected by occlusion per §1B).
   - Same occlusion, call `take_screenshot`: **expect the returned image to show the occluding window's content, not the showcase** — this is the *known, source-confirmed limitation*, not a bug to chase. The milestone's proof should capture this as a documented fact ("screenshot requires the window to be actually on top of the screen; other tools don't"), not silently treat a garbled screenshot as a mystery failure.
4. **Minimized test:** minimize the showcase, then call `list_windows`: **expect the window to be absent**; then call any window-targeting tool: **expect a clean "no application window is currently available" error**, not a hang or a stale/wrong result. This validates §1C precisely and sets the correct expectation for the milestone's own workflow (the agent must ensure the window is at least restored, even if it doesn't need to be on top, before running any check beyond `status`/`reload`/`get_logs`).
5. **Record the result as fact, per the milestone's own decision** ("Результат фиксируется как факт, а не как допущение") — the four checks above produce a pass/fail matrix that matches exactly the summary table in §1, so any deviation from that table is itself the actionable finding.

---

## Sources

**Primary (source code, read directly, HIGH confidence):**
- [JetBrains/compose-hot-reload](https://github.com/JetBrains/compose-hot-reload), tag `v1.2.0`, commit `3315f8dd8f5cc4db1359ff13b2be178ba19486e3`:
  - `hot-reload-mcp/src/main/kotlin/org/jetbrains/compose/reload/mcp/McpServer.kt` — full tool list, schemas, handlers
  - `hot-reload-mcp/src/main/kotlin/org/jetbrains/compose/reload/mcp/McpMain.kt` — pidFile-watch/reconnect loop
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/screenshotHandler.kt` — `Robot.createScreenCapture`
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/semanticTreeHandler.kt` — in-process accessibility-tree walk, popup/dialog forest
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/uiActionHandler.kt` — semantics-action invocation (click/longClick/setText/scrollBy/scrollToIndex)
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/window.kt` — window registration, iconify → unregister
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/windowResizeHandler.kt`
  - `hot-reload-runtime-jvm/src/main/kotlin/org/jetbrains/compose/reload/jvm/DevelopmentEntryPoint.kt` — per-window/dialog wiring, instrumentation hook
  - `hot-reload-gradle-plugin/src/main/kotlin/org/jetbrains/compose/reload/gradle/mcpTasks.kt`, `mcpConfiguration.kt`, `hotRunTasks.kt` — task registration, daemon/launch-mode wiring
  - `hot-reload-gradle-plugin/src/test/kotlin/org/jetbrains/compose/reload/gradle/tests/ComposeHotMcpServerTasksTest.kt`, `ComposeHotRunTasksTest.kt` — confirmed task names (`hotMcpServer`, `hotRun`, `hotDev` for plain `kotlin.jvm` projects)
  - `hot-reload-devtools/src/main/kotlin/org/jetbrains/compose/devtools/gradle/GradleRecompiler.kt` — per-reload Gradle sub-process, daemon decision logic
  - `hot-reload-devtools/src/main/kotlin/org/jetbrains/compose/devtools/restart.kt`, `shutdown.kt` — no stop/kill tool, only restart
  - `tests/src/reloadFunctionalTest/kotlin/org/jetbrains/compose/reload/tests/SemanticTreeIntegrationTest.kt`, `TakeScreenshotIntegrationTest.kt`, `MultiWindowScreenshotIntegrationTest.kt` — JetBrains' own regression coverage, confirms popup-forest behavior and Robot-based capture assumptions (`isInteractiveDesktopAvailable()`/`@Headless(false)` gating)

**Secondary (release notes / official docs, MEDIUM confidence):**
- [kotlinlang.org — Compose Hot Reload docs](https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html) — `.mcp.json` config example, task-name resolution notes
- [JetBrains blog — Compose Multiplatform 1.12.0](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/) — version pairing (Hot Reload 1.2.0 ↔ CMP 1.12.0)
- [JetBrains/compose-hot-reload v1.3.0-alpha02 release notes](https://github.com/JetBrains/compose-hot-reload/releases/tag/v1.3.0-alpha02) — future `captureContentToImage()` off-screen path, CMP 1.13+ gate, Robot fallback confirmation for earlier versions
- [JetBrains/compose-hot-reload README, MCP section](https://github.com/JetBrains/compose-hot-reload#mcp-server-for-ai-agents) — tool summary cross-check, confirmed consistent with source read

**Tertiary (WebSearch-only or unverifiable, LOW confidence, flagged inline where used):**
- [InfoWorld — Compose Multiplatform 1.12.0 welcomes coding agents](https://www.infoworld.com/article/4216661/compose-multiplatform-1-12-0-welcomes-coding-agents-with-mcp-server.html)
- [GitHub issue #70](https://github.com/JetBrains/compose-hot-reload/issues/70) — old, likely-stale Windows DevTools crash, low relevance, noted only for the incidental Command Prompt window observation
- [GitHub issue #488](https://github.com/JetBrains/compose-hot-reload/issues/488) — pinned notice: GitHub issue tracking migrated to YouTrack (`project:CMP`, group "Hot Reload")
- YouTrack current open-issue list **could not be enumerated** in this session (client-rendered page, not scrapable via WebFetch) — explicit gap, not a "no issues" finding
