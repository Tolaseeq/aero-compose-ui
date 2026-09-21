# Pitfalls Research — v3.1 Dependency Refresh + Hot Reload MCP

**Domain:** Forced multi-major dependency/toolchain refresh (Kotlin 2.4.10→2.4.20, Compose Multiplatform 1.11.1→1.12.0, coroutines 1.10.2→1.11.0, kotlinx-datetime 0.6.2→0.8.0 plain, JUnit 5.10.0→6.1.3, Gradle 8.14.3→9.7.1, JDK 17→21) plus first-time Compose Hot Reload 1.2.0 + MCP server install, on an already-shipped Compose Desktop (JVM) UI library published via JitPack (aero-compose-ui).
**Researched:** 2026-09-21
**Confidence:** HIGH where grounded in this repo's actual source (grep-verified, cited file:line) or an official changelog/release-notes page fetched directly; MEDIUM where an official source was fetched but the specific interaction with this project's setup required inference; LOW/UNVERIFIED flagged inline where only general WebSearch discussion (not an official doc) supports the claim — these need a spike/smoke-test before being trusted, not treated as fact.

This catalog assumes the reader knows the project's locked cross-milestone lessons (from `.planning/research/_archive-v3.0/PITFALLS.md` and PROJECT.md Key Decisions): toolchain migration isolated in its own first phase with zero behavior changes; every guard proven failing-on-unfixed-code before being trusted; `undecorated=true` WITHOUT `transparent=true` (Win11 access violation, issue #3757); Material3 pinned explicitly to stable `1.9.0` because the `compose.material3` alias silently resolves to whatever alpha CMP bundles; JitPack throwaway-tag verification before the real release tag. It does not repeat those — it extends them into the specific failure modes of *this* dependency jump and *this* first-time MCP integration.

---

## Critical Pitfalls

### Pitfall 1: `kotlinx.datetime.Clock` import and `.dayOfMonth`/`.monthNumber` property access are confirmed compile breaks under a clean 0.8.0

**What goes wrong:**
This is not a hypothetical risk — it is a **confirmed, grep-verified break** in this exact codebase. The official kotlinx-datetime changelog states verbatim for 0.7.0: *"Remove `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock` in favor of `kotlin.time.Instant`"* and *"Rename `dayOfMonth` to `day`, `monthNumber` to `month`"* (source: [kotlinx-datetime CHANGELOG.md](https://github.com/Kotlin/kotlinx-datetime/blob/master/CHANGELOG.md), HIGH confidence — official changelog, fetched directly). A 0.7.1 compatibility release added `kotlinx.datetime.Instant`/`Clock` back as **type aliases** to `kotlin.time.Instant`/`Clock` for migration convenience, but the milestone's own decision is to go to a **clean 0.8.0 without the `-0.6.x-compat` artifact**, so that convenience alias is not present.

Grep of `library/src/main` confirms exactly four files import `kotlinx.datetime.Clock` and call `Clock.System.now()`:
- `AeroDatePicker.kt:27,168` — `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date`
- `AeroDateTimePicker.kt:33,197` — same pattern
- `AeroDateRangePicker.kt:31,264` — same pattern
- `AeroDateTimeRangePicker.kt:35,334` — same pattern

And four call sites use the now-renamed properties directly:
- `AeroDateTimePicker.kt:220` — `LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute, time.second)`
- `AeroDatePicker.kt:150` — `"%02d.%02d.%04d".format(date.dayOfMonth, date.monthNumber, date.year)`
- `AeroCalendarGrid.kt:71` — `LocalDate(displayMonth.year, displayMonth.monthNumber, 1)`
- `AeroCalendarGrid.kt:150` — `LocalDate(displayMonth.year, displayMonth.monthNumber, dayCounter)`

Per the current kotlinx-datetime 0.8.0 API docs ([`LocalDate.day`](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-local-date/day.html), fetched directly, HIGH confidence): `dayOfMonth` is now `day: Int`, and `monthNumber` is now reached via `.month.number` (`.month` returns the `Month` enum, `.month.number` returns the 1-based `Int` — the old `Int`-returning `monthNumber` property no longer exists under that name). The `LocalDate(year: Int, month: Int, day: Int)` *constructor* overload is unaffected and still accepts a plain `Int` for month — only the *property accessors* on an existing `LocalDate` instance changed name/shape. `Month` itself (used in `AeroCalendarGrid.kt`'s `daysInMonth`/`englishName` via `Month.JANUARY` etc.) is unaffected beyond no longer being a `java.time.Month` type alias — the enum constants (`Month.JANUARY`...`Month.DECEMBER`) still exist under kotlinx-datetime's own type, so that code compiles unchanged.

**Why it happens:**
The project correctly decided to skip the compatibility artifact ("чистая 0.8.0, без `-0.6.x-compat`") for a clean floor, but that decision converts what would otherwise be a deprecation warning into a hard `error: unresolved reference` for every `Clock`/`.dayOfMonth`/`.monthNumber` call site the moment the version bumps — these are exactly the pickers this library's whole `PICK-01..04`/`DTR-01..08` requirement set depends on.

**How to avoid:**
Before bumping the version catalog entry, do the mechanical fixes as their own commit, gated by the existing picker test suite (`AeroDatePickerTest`, `AeroDateTimePickerTest`, `AeroDateRangePickerTest`, `AeroDateTimeRangePickerTest`, `AeroCalendarGridTest` — all already import `kotlinx.datetime.*` and will themselves need the same treatment if they touch `Clock`/`dayOfMonth`/`monthNumber`, verify via the same grep):
1. `import kotlinx.datetime.Clock` → `import kotlin.time.Clock` (4 files) — `Clock.System.now()` call syntax is unchanged.
2. `date.dayOfMonth` → `date.day` (2 files).
3. `date.monthNumber` → `date.month.number` (4 call sites across 3 files).
4. Re-run the full picker + calendar-grid test files immediately after the mechanical rename, before touching anything else in the milestone — this is a pure API-surface fix with zero behavior change, so it should be a green, isolated commit exactly like the v3.0 toolchain-isolation lesson prescribes.

**Warning signs:**
- Build fails with `unresolved reference: Clock` or `unresolved reference: dayOfMonth`/`monthNumber` the moment `kotlinxDatetime` is bumped in `gradle/libs.versions.toml` — this is not a warning-then-later-failure, it fails immediately, which is actually the *good* case (loud, not silent).
- `kotlin.time.Clock`/`kotlin.time.Instant` conflicting by simple-name with any lingering `kotlinx.datetime.Clock`/`Instant` import elsewhere in the same file (unlikely here per the grep, but worth a final IDE-level "no `kotlinx.datetime.Clock` anywhere" grep before closing this phase).

**Phase to address:** First, as an isolated commit inside the dependency-bump phase, gated by the picker/calendar test files — do this *before* the coroutines/JUnit/Gradle/JDK bumps in the same phase, so a failure is attributable to exactly this rename, not entangled with the rest of the refresh.

---

### Pitfall 2: Compose Hot Reload's devtools panel introduces a *second* path to a Material3 alpha, bypassing the existing pin

**What goes wrong:**
This project already has one locked defense against this exact class of bug (PROJECT.md Constraints: *"алиас `compose.material3` на CMP 1.11.x молча резолвится в alpha"*) — both `library/build.gradle.kts:22` and `showcase/build.gradle.kts:16` pin `org.jetbrains.compose.material3:material3:1.9.0` by exact coordinate instead of using the `compose.material3` alias. Compose Multiplatform 1.12.0 still bundles a pre-release Material3: the official JetBrains 1.12.0 changelog states *"Material3 1.12.0-alpha03 library was bundled, based on Jetpack Material3 1.5.0-alpha22"* (source: JetBrains CMP 1.12.0 changelog, fetched directly, HIGH confidence). This milestone adds a **new** dependency edge that the v3.0 defense never had to account for: Compose Hot Reload's `hot-reload-devtools` module (the on-screen reload-status overlay), published as `org.jetbrains.compose.hot-reload:hot-reload-devtools`, itself depends on `org.jetbrains.compose.material3:material3` for its own UI (confirmed via Maven Central artifact listing and the project's `dependencies.toml`, MEDIUM confidence — module existence and the dependency's presence confirmed, exact requested version string not independently verified). Because Gradle's default conflict-resolution strategy picks the numerically-highest requested version across a configuration, and `1.12.0-alpha03` sorts higher than `1.9.0` (12 > 9 in the minor-version position — pre-release suffix does not change Gradle's default comparison the way it might in strict semver tooling), a devtools-pulled `1.12.0-alpha03` request can silently win over the explicit `1.9.0` pin in `:showcase`'s resolved `testRuntimeClasspath`/`runtimeClasspath` **the moment Hot Reload is added**, even though `:library` and `:showcase`'s own `implementation(...)` line still say `1.9.0`.

**Why it happens:**
The existing pin was designed to defeat one source of alpha leakage (the CMP alias itself); it was never tested against a *third-party* dependency that independently requests Material3 via the alias internally, which is exactly what a brand-new devtools/dev-only dependency is.

**How to avoid:**
After adding the Hot Reload plugin/dependency to `:showcase` (never `:library` — the milestone already scopes it there correctly), run `./gradlew :showcase:dependencyInsight --dependency material3 --configuration runtimeClasspath` (and repeat for `testRuntimeClasspath` if uiTest is also affected) and confirm the resolved version is `1.9.0`, not `1.12.0-alpha03` or newer. If it resolves to the alpha, force the pin explicitly:
```kotlin
configurations.all {
    resolutionStrategy.force("org.jetbrains.compose.material3:material3:1.9.0")
}
```
scoped to `:showcase` only (never force it in `:library`'s `api` surface, since that's the published coordinate consumers see). Treat this `dependencyInsight` check as a required step of the Hot Reload setup, not an optional sanity check — it is cheap (one command) and the failure mode it catches (a stale/wrong Material3 alpha silently backing every `:showcase` composable, including the devtools overlay itself) is exactly the "looks fine until a subtle visual/API mismatch appears" class of bug this project has already been burned by once.

**Warning signs:**
- `:showcase` builds and runs fine after adding Hot Reload, but a Material3 API used elsewhere in the showcase (e.g., `Slider`, `TextField` internals `AeroSlider` still wraps) behaves subtly differently or a new deprecation warning appears referencing a `1.12.0-alpha03`-only API.
- `dependencyInsight --dependency material3` shows more than one requested version with the devtools module listed as a requester.

**Phase to address:** Hot Reload/MCP setup phase — run the `dependencyInsight` check as the very next step after adding the Hot Reload plugin/dependency to `:showcase`, before writing any `.mcp.json` wiring.

---

### Pitfall 3: Compose Multiplatform 1.12's `uiTest` default dispatcher change risks silently altering the 467-test suite's timing assumptions

**What goes wrong:**
The official 1.12.0 changelog states: *"The v2 Compose UI Tests APIs were expanded to support non-Android targets using `StandardTestDispatcher` by default. Support for customizing `effectContext` in tests was also implemented"* (source: JetBrains CMP 1.12.0 changelog via GitHub release, fetched directly, MEDIUM confidence — the bullet is confirmed verbatim, but the exact prior-version default for Desktop specifically, and therefore the precise before/after delta this project will experience, was not independently confirmed beyond this one line). `StandardTestDispatcher` requires explicit `advanceUntilIdle()`/test-clock advancement to run queued coroutines, unlike an eager/unconfined dispatcher — a `runComposeUiTest`-based test (this project has these for the RCMP drag-during-recompose guard, per PROJECT.md's v2.0.4/v3.0 entries) that implicitly relied on `LaunchedEffect`/animation coroutines running eagerly between actions could start flaking, hanging, or timing out differently after the bump, purely from the harness's own dispatcher default changing underneath it — a false positive or false negative unrelated to any real product code change.

**Why it happens:**
Test-infrastructure defaults changing inside a minor library bump is exactly the kind of change that doesn't show up in a diff of *this project's* code — it's invisible until the suite is actually run against the new version.

**How to avoid:**
Run the full 467-test suite immediately after the CMP version bump, in isolation from the other dependency bumps in the same phase (mirrors Pitfall 1's isolation advice) — if a `runComposeUiTest`-based test starts failing or hanging in a way unrelated to product logic, suspect the dispatcher default first, and check whether the test needs an explicit `mainClock.advanceTimeUntilIdle()`/`awaitIdle()` call it previously got "for free." Do not treat a newly-flaky `uiTest` as a product regression until this has been ruled out.

**Warning signs:**
- A `runComposeUiTest`-based test (especially ones exercising drag/animation timing, like the RCMP guard) that passed reliably pre-bump starts intermittently timing out or asserting on stale state post-bump, with no corresponding product-code change in the same commit.

**Phase to address:** Dependency-bump phase, verification step — run the full suite right after the CMP bump specifically, not only at the end of the whole phase, so a dispatcher-related flake is attributable to CMP alone.

---

### Pitfall 4: Windows cannot execute `./gradlew` from `.mcp.json` — and the MCP server only works against an *already-running* app, not as a standalone launcher

**What goes wrong:**
Two distinct, both confirmed-in-the-wild problems with the exact setup this milestone specifies:
1. **The POSIX-script problem.** A real-world adopter (`aoreshkov/kmp-ledger`, PR #27) hit this directly: *"MCP clients execute configured commands directly without a shell interpretation. The `.mcp.json` configuration pointed to `./gradlew`, which is a POSIX script lacking a file extension. Windows cannot execute extensionless scripts directly, preventing the `hotMcpServer` endpoint from launching."* (MEDIUM confidence — third-party PR description, not an official doc, but directly corroborates the milestone's own stated decision to launch through `cmd /c gradlew.bat ...` instead of `./gradlew`, meaning that decision is already the correct fix for a real, documented failure mode, not a speculative precaution).
2. **The already-running-app prerequisite.** The same PR's accompanying `CONTRIBUTING.md` update clarifies that the hot-reload MCP server endpoint *"attaches to an already-running app"* — meaning a client must first start the app itself (e.g., `hotRunJvm`/`hotRunAsync`) before the MCP server's `reload`/`take_screenshot`/`get_semantic_tree` tools have anything to attach to; launching only the `hotMcpServer` task with no running showcase process makes the server *look* broken (tools return errors or hang) when it is actually working exactly as designed, just with nothing to attach to yet.

**Why it happens:**
`.mcp.json`'s `command`/`args` are executed directly by the MCP client's process-spawning code, not through a shell — so shebang-less scripts that rely on the OS shell to dispatch to `bash`/`sh` never get that dispatch on Windows. And the MCP server's own architecture (attach-to-running-process, not launch-and-attach) is a design choice the tool's own documentation doesn't foreground as a prerequisite, so it reads as a working server with a failure, not as "step 1 of 2 wasn't done."

**How to avoid:**
Confirm `cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer` (the milestone's own planned command) actually launches on a clean Windows shell before wiring it into `.mcp.json` — this is the correct workaround for problem 1. For problem 2, the setup must document and the agent's workflow must follow the two-step sequence: start the showcase app first (`hotRunJvm`/equivalent, backgrounded), *then* connect the MCP server — never expect `hotMcpServer` alone to produce a usable window to screenshot/click. If the target module has multiple JVM targets, use the fully-qualified task name (e.g., `:showcase:hotMcpServerJvm`) to avoid Gradle's short-name task-matching ambiguity across subprojects. As a documented fallback if `cmd /c gradlew.bat` proves flaky (daemon reuse across restarts, quoting issues), the more portable fix used by the same upstream PR is invoking the wrapper JAR directly: `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain <task>` — byte-identical across OSes, needs only `java` on `PATH`, no `.bat`/`.sh` dispatch at all.

**Warning signs:**
- `.mcp.json` shows the server as connected/started, but every tool call (`take_screenshot`, `get_semantic_tree`, `click`) errors or hangs — check whether the showcase app was actually running first before assuming the MCP server itself is broken.
- The MCP client reports a spawn failure referencing `gradlew` with no `.bat`/`.exe` extension, or "not recognized as an internal or external command."

**Phase to address:** Hot Reload/MCP setup phase — verify the two-step (run app, then connect MCP) sequence explicitly as part of standing the server up, before the MCP-safety-verification phase tries to prove the server doesn't steal focus/cursor.

---

### Pitfall 5: `--no-daemon` cold-start latency vs. MCP client connection timeout, and orphaned JVM processes after disconnect

**What goes wrong:**
The milestone's planned launch command is `cmd /c gradlew.bat --no-daemon --quiet --console=plain <task>` — `--no-daemon` is a deliberate, reasonable choice (avoids a lingering Gradle daemon complicating the "does MCP debugging get in the way of normal computer use" question this milestone is explicitly trying to answer), but it means **every** MCP server start pays full Gradle JVM bootstrap + configuration-phase cost from cold, with no daemon warm-up to amortize it. If the MCP client (the agent's own harness) has a fixed connection/handshake timeout shorter than this cold-start time — which grows with Gradle 9's own JVM 17+ startup cost, this project's larger dependency graph after the refresh, and JBR provisioning/verification on first launch — the very first connection attempt in a session can fail or appear hung even though the server would have come up correctly given more time. Separately, if the MCP client process disconnects or is killed without a graceful shutdown signal reaching the spawned `cmd.exe` → `gradlew.bat` → JVM process tree, `--no-daemon` means there is no daemon-registry bookkeeping to clean the process up — an orphaned `java.exe` (running the showcase app under JBR, kept alive by the Hot Reload server for exactly the "attach to a running app" reason in Pitfall 4) can persist after the MCP client thinks the session ended, consuming memory/CPU and potentially holding the showcase window open or a port bound, until the maintainer notices and kills it manually. (LOW/UNVERIFIED confidence for both — no official doc or reproduced incident found for this exact command chain; this is a reasoned risk from the documented pieces (`--no-daemon` semantics, Windows process-tree signal-forwarding behavior through `cmd /c`), not a confirmed report, and must be spiked rather than assumed.)

**Why it happens:**
`--no-daemon` optimizes for "don't leave a background Gradle process the maintainer didn't ask for," which is the right call given this milestone's own stated goal — but it trades that for cold-start latency and removes the one mechanism (`--stop`/daemon expiry) that would otherwise clean up an abandoned build process automatically.

**How to avoid:**
Time the actual cold start of `cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer` (and the app-launch task) on this machine, on this toolchain, before assuming it fits inside whatever timeout the MCP client enforces — if it's tight, that's a fact to report, not a corner to round off. After a verification session, explicitly check Task Manager / `Get-Process java` for orphaned JVMs tied to the showcase app or the MCP server task after the client disconnects, and if found, document the cleanup step (kill by PID, or a documented `taskkill` step) as part of the session-end routine rather than discovering it days later as "why is my machine slow."

**Warning signs:**
- The MCP client reports a connection timeout or failure specifically on the *first* tool call of a session, but a manual retry immediately after succeeds (classic cold-start-vs-timeout symptom).
- `java.exe`/`javaw.exe` processes matching the showcase or hot-reload-server visible in Task Manager well after the agent's session (and the visible terminal, if any) has ended.

**Phase to address:** MCP-safety-verification phase — this is exactly the phase whose job is proving MCP debugging doesn't get in the way of the maintainer's own computer use; cold-start timing and orphan-process cleanup belong in that same "prove it, don't assume it" pass, alongside cursor/focus/overlapped-window checks.

---

### Pitfall 6: Gradle's `--quiet --console=plain` suppresses Gradle's own logging, not necessarily JBR/app stdout — a real risk for a stdio-transport MCP protocol

**What goes wrong:**
The MCP protocol over stdio depends on stdout carrying *only* protocol frames — any stray line (a progress bar, a warning, a println somewhere in the dependency graph) corrupts the stream from the client's perspective. `--quiet` and `--console=plain` are Gradle-level flags that control *Gradel's own* log verbosity and console rendering (progress bars, ANSI codes) — they do not gate what the JVM process the task eventually launches (JBR running the showcase app, or the MCP server's own runtime) writes to its inherited stdout. If JBR's automatic-provisioning path (mentioned as experimental in Compose Hot Reload's own docs: `compose.reload.jbr.autoProvisioningEnabled`) prints download/verification progress on first run, or if any dependency in the now-larger graph logs a deprecation warning to stdout rather than stderr during the configuration phase, that output can land on the same stdout the MCP client is trying to parse as JSON-RPC frames — before `--quiet` even has a chance to matter, since Gradle's own quiet flag governs *Gradle's* logger, not arbitrary child-process/library `println`s.

**Why it happens:**
`--quiet --console=plain` looks like it should fully sanitize stdout because it's the standard advice for "clean Gradle output," but it's scoped to Gradle's own logging infrastructure, not to every line any dependency or forked JVM might write.

**How to avoid:**
On first setup, capture the raw stdout of `cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer` to a file and inspect it line-by-line for anything that isn't a protocol frame, specifically around first-ever JBR provisioning (which is the likeliest one-time source of extra noise) — if found, either pre-provision JBR once outside the MCP-launched path (so provisioning noise never occurs during a live MCP session) or redirect the offending stream. This should be a one-time check during Hot Reload/MCP setup, not a recurring worry, but it must actually be checked rather than assumed clean because `--quiet --console=plain` was used.

**Warning signs:**
- The MCP client reports malformed/unparseable JSON-RPC frames specifically on the very first connection after a fresh machine setup or a JBR version bump (when provisioning is most likely to run), but not on subsequent connections.

**Phase to address:** Hot Reload/MCP setup phase — verify clean stdout once, on first setup, as part of standing the server up.

---

### Pitfall 7: DPI scaling (125%/200%) was never verified for the *old* toolchain either — Hot Reload/MCP inherits, not introduces, this gap, but the milestone's verification plan can silently paper over it

**What goes wrong:**
This is a carried-over gap, not a new one: v3.0's own closeout explicitly states *"прогоны на 125%/150%/200% DPI, которых требовал SHW-16, сознательно отменены решением мейнтейнера — пробел зафиксирован, а не выдан за пройденный"* (PROJECT.md, v3.0 section). This milestone's plan is to have the agent walk the showcase through MCP at whatever the *default* display scaling is and diff against BEFORE screenshots — if that default is 100% (the only scale ever verified), the "no unexplained visual drift" verdict this milestone produces says nothing about 125%/200%, continuing the same unstated gap rather than closing or re-flagging it. Separately, Compose Hot Reload's devtools overlay and the MCP server's `take_screenshot`/semantic-tree tools have never been run on this project at non-100% scaling at all — the sub-pixel/hairline-stroke class of DPI bug already catalogued in the v3.0 pitfalls research (archived Pitfall 12: fixed-dp strokes going sub-pixel at 125%/150%/200%) is exactly as live today as it was then, and a Hot-Reload-driven verification pass run only at 100% cannot detect it.

**Why it happens:**
"Walk the showcase in three themes" is a well-defined, boundable task; "...at every DPI scale too" multiplies the verification surface and wasn't asked for by this milestone's explicit scope (the milestone's own text lists visual-drift *from the update itself* as in scope, not closing DPI debt).

**How to avoid:**
State explicitly, in the milestone's own verification output, what DPI scale the agent's MCP walkthrough ran at — do not let a scale-unspecified "no drift found" implicitly read as "all scales checked" the way SHW-16's original ask did before it was explicitly waived. If time allows, one quick manual (not MCP-driven, since MCP-at-non-100%-scaling is itself unverified per Pitfall 8 below) spot-check at 150% or 200% costs little and either surfaces a real problem early or lets the existing acknowledged gap stand un-multiplied by new tooling.

**Warning signs:**
- The milestone's final report says "walked all sections, three themes, no drift" without naming a DPI scale — this phrasing is the warning sign itself, since it invites the reader to assume full coverage.

**Phase to address:** Verification phase — name the DPI scale explicitly in the final report; treat as a known, carried-over, out-of-scope gap rather than silently expanding "verified" to include it.

---

### Pitfall 8: MCP screenshot capture path and the project's own established CopyFromScreen (windows-mcp) capture path are not the same pixel pipeline — before/after comparison risks false drift or masked drift

**What goes wrong:**
The project's established BEFORE-screenshot method (documented in the `reference_windows_mcp_showcase_capture` memory) uses Win32 `CopyFromScreen` — a true screen-scrape that includes whatever DWM/compositor/color-management the OS applies to the actual displayed pixels, at the OS's current display scale, potentially including window chrome. Compose Hot Reload's MCP `take_screenshot` tool, by contrast, is very likely an **in-process Skia-surface capture** (rendering the composition to a bitmap from inside the JVM, not scraping the screen) — this is the typical implementation shape for this kind of dev-tool screenshot API, though the exact mechanism was not independently confirmed against Compose Hot Reload's source in this research pass (UNVERIFIED — treat as a hypothesis to confirm, not a fact). If the two capture paths differ in *any* of: DPI/scale factor applied, color-space/gamma handling, whether window chrome/title bar is included, or antialiasing settings at capture time, then a pixel-diff between a `CopyFromScreen` BEFORE image and an MCP `take_screenshot` AFTER image can show differences that have nothing to do with the dependency refresh — a false positive that wastes investigation time, or worse, a false negative that masks real drift if the two paths both smooth over the same class of difference.

**Why it happens:**
The milestone's own plan already anticipates half of this ("Снимки «до» снимаются раньше обновления прежним ручным способом: MCP-сервер появляется только с Compose Multiplatform 1.12.0" — PROJECT.md) — acknowledging the BEFORE/AFTER tooling *must* differ since the AFTER-only tool didn't exist yet. What the plan doesn't yet state is how the comparison step accounts for that known pipeline difference rather than treating both images as directly diffable.

**How to avoid:**
Before trusting any AFTER-vs-BEFORE diff produced this way, run one calibration pass: with the toolchain fully upgraded but **before** any intentional visual change (i.e., immediately after Pitfall 1-3's dependency fixes, on otherwise-unchanged draw code), capture the *same* screen/section with both `CopyFromScreen` (the old method, still available since it doesn't depend on the app being Hot-Reload-launched) and MCP `take_screenshot`, and diff those two AFTER-only images against each other. Any difference found here is entirely capture-pipeline artifact (DPI/gamma/chrome), not product drift — that delta becomes the tolerance band (or the specific known-different regions to visually ignore) applied when later comparing the true BEFORE (`CopyFromScreen`, pre-upgrade) against the true AFTER (MCP `take_screenshot`, post-upgrade). Skipping this calibration step means every drift finding from the real comparison carries unresolved ambiguity about whether it's real.

**Warning signs:**
- A pixel-diff between BEFORE and AFTER shows drift uniformly across *every* component/theme rather than isolated to specific ones — a uniform, global difference is the signature of a capture-pipeline mismatch, not a targeted rendering regression.
- Window-chrome-adjacent regions (title bar, edges) differ even where the milestone made no title-bar-adjacent changes.

**Phase to address:** Verification phase — run the calibration pass (same-toolchain, both capture methods) as the first step of setting up the diff comparison, before generating the real BEFORE/AFTER verdict.

---

### Pitfall 9: A minor-version-numbered but consumer-breaking release needs the break stated where a consumer will actually see it before installing, not only in the README body

**What goes wrong:**
The milestone's own key decision explicitly accepts shipping `3.1.0` (a minor bump under semver-ish convention) while raising the Java floor to 21, the Compose floor to 1.12, and cleanly removing `kotlinx.datetime.Instant`/`Clock` for any consumer that happened to depend on them transitively through this library's public API surface (`kotlinx-datetime` is declared `api(...)` in `library/build.gradle.kts:27`, meaning consumers get it on their own compile classpath transitively, per the existing Key Decision recorded in PROJECT.md — so a consumer's own code referencing `kotlinx.datetime.Instant`/`Clock` via this library's transitive dependency breaks too, not just this library's internals). A consumer who upgrades `3.0.x → 3.1.0` expecting a routine minor bump (per the version number alone) hits a Java-version-mismatch Gradle resolution error (see Pitfall 10) or a compile break in their own code with no warning beforehand, unless the documentation makes the break impossible to miss *before* they add the dependency, not just discoverable after a broken build.

**Why it happens:**
The maintainer's decision to use `3.1.0` for this was explicit and deliberate (PROJECT.md: *"требования прописываются в README, а не выражаются номером версии"*) — this pitfall is not about relitigating that choice, only about what documentation completeness that choice now obligates.

**How to avoid:**
The README's dependency-declaration section (the `implementation("com.github.Tolaseeq:aero-compose-ui:vX.Y.Z")` snippet, currently at `README.md:73` referencing `v3.0.0`) must state the Java 21 / Compose 1.12 / Kotlin 2.4.20 / kotlinx-datetime 0.8 floor **immediately adjacent to** the dependency coordinate for `3.1.0`, not only in prose above/below it — the existing `v3.0.0` toolchain-requirement callout pattern (README.md:80, *"Toolchain requirement. `v3.0.0` is built on..."*) is the right template to repeat and update, not a new format to invent. The JitPack tag description / GitHub release notes for `v3.1.0` should carry the same floor statement, since that's often the page a consumer sees first via JitPack's version browser, before ever opening the README.

**Warning signs:**
- A consumer issue/question along the lines of "upgraded to 3.1.0 and now nothing compiles" that could have been prevented by a floor statement visible at the point of adding the dependency.

**Phase to address:** Release phase — write the README/release-notes floor statement as part of the same commit that bumps the version and tags the release, not as an afterthought.

---

### Pitfall 10: Gradle Module Metadata's `org.gradle.jvm.version` attribute will make the artifact *fail to resolve* for JDK 17 consumers, not merely fail to run — confirm the error is legible, not confusing

**What goes wrong:**
Building `:library` with `jvmToolchain(21)` causes the published Gradle Module Metadata to declare `org.gradle.jvm.version = 21` on the relevant variant. A consumer still on JDK 17 (with a Gradle version that understands and enforces this attribute, i.e. most Gradle 7+) will get a **dependency resolution failure**, not a runtime `UnsupportedClassVersionError` — Gradle refuses to select a variant it knows the consumer's JVM can't run, and (per current Gradle behavior, MEDIUM confidence — general Gradle attribute-matching documentation confirms the mechanism, the exact wording of the error message was not independently reproduced in this research pass) modern Gradle versions produce a specific, actionable message suggesting "upgrade your JVM or downgrade the dependency version" rather than a generic incompatible-attribute dump. Separately and independently, a consumer whose Gradle *doesn't* enforce this attribute (an older Gradle, or a non-Gradle consumer like plain Maven) would instead hit the runtime version at classload time: class file major version 65 (JDK 21) cannot be loaded by a JDK 17 (major version 61) JVM — confirmed via the standard Java class-file-version mapping (major version − 44 = feature release number; 65 → 21), producing `java.lang.UnsupportedClassVersionError` at the point the JAR's classes are actually loaded, not at build/resolve time.

**Why it happens:**
This is the intended, correct behavior of Gradle Module Metadata (fail fast and legibly at resolution time for Gradle consumers) plus the intended, correct behavior of the JVM class-file-version check (fail at load time for anyone the module-metadata check didn't already catch) — it isn't a bug to fix, it's a floor to document clearly (see Pitfall 9) so it's never actually hit by surprise.

**How to avoid:**
Do not assume the Gradle-resolution-time error is self-explanatory enough to skip a README floor statement — verify once, deliberately, what the actual message looks like: build and publish a throwaway JitPack tag of the JDK-21 artifact, then attempt to add it as a dependency from a scratch project pinned to JDK 17/Gradle 8, and read the literal failure text. If it's as clear as expected, that's useful confirmation; if it's confusing (attribute-mismatch jargon with no plain-English "you need JDK 21" line), the README needs to pre-empt that specific confusion by naming the exact error string a JDK-17 consumer will see, so a search for that string finds the README.

**Warning signs:**
- A consumer report describing a `org.gradle.jvm.version`/variant-selection error with no obvious connection to "upgrade your JDK" in their own reading of it — confirms the message needs a README cross-reference.

**Phase to address:** Release phase, as part of the same throwaway-tag JitPack verification the project already does before a real tag (v3.0 precedent: `-alpha01`, `-verify01/02`) — read the actual consumer-side error once during that verification, don't assume it.

---

### Pitfall 11: JUnit major-version bump interacting with `kotlin-test`'s auto-selected test-framework capability and `compose.uiTest`'s JUnit4 pull — verify the 467 count as a hard gate, not a green-build assumption

**What goes wrong:**
This project already uses `org.junit.jupiter:junit-jupiter` (the aggregator artifact, `library/build.gradle.kts:32`), which bundles the engine together with the API — this is the *safer* of the two common JUnit-Gradle setups (the other, more fragile one being separate `junit-jupiter-api`/`junit-jupiter-engine` coordinates that can drift out of version-lockstep and cause exactly the "0 tests run, build green" failure mode). Per the official JUnit 6 migration notes (fetched directly, HIGH confidence): *"To simplify dependency management, all modules of JUnit Platform, Jupiter, and Vintage now use the same version number: 6.0.0"* and *"migrating from JUnit 5.x.y to 6.0.0 should be much easier than migrating from 4.x to 5.x... version 6.0.0 [is] a drop-in replacement in most cases."* This lowers but does not eliminate the risk: the project *also* declares `testImplementation(libs.kotlin.test)` (a plain, framework-agnostic `kotlin-test` artifact) alongside JUnit Jupiter — the Kotlin Gradle plugin auto-selects the concrete `kotlin-test-junit5` implementation via a Gradle "capability" mechanism when it detects a JUnit 5 engine on the test classpath, redirecting `kotlin-test`'s `@Test`/`assertEquals` calls to JUnit 5's runner. This auto-detection heuristic is version-sensitive machinery that has not been exercised against a JUnit 6 classpath by this project before — it is *expected* to keep working per the "drop-in replacement" framing, but "expected to work" is exactly the class of assumption this project's own locked lesson (*"a guard must provably fail on unfixed code"*) exists to distrust. Separately, `compose.uiTest` (`library/build.gradle.kts:40`) is documented to depend on JUnit4-shaped test infrastructure on some platforms (the classic `androidx.compose.ui:ui-test-junit4` lineage) — on a JVM/Desktop target this may or may not actually pull a JUnit4 engine onto the classpath (MEDIUM confidence, not independently confirmed for the Desktop `compose.uiTest`/`compose.desktop.currentOs` combination specifically), but if it does, a JUnit4-vs-JUnit6-Vintage-Engine interaction becomes a second, independent source of "some tests silently skipped rather than failed."

**Why it happens:**
"Build succeeds, exit code 0" and "all 467 tests ran and passed" are different claims, and a silently-reduced test-discovery count (a misconfigured engine simply finding fewer `@Test`-annotated methods) produces the first without the second — this is precisely the "tests silently not discovered, 0 tests run, build green" failure mode the question calls out, and it is a known, real Gradle/JUnit failure class (confirmed via multiple independent WebSearch sources describing this exact symptom in Gradle+JUnit5 setups generally, MEDIUM confidence for the general pattern, not specific to this project).

**How to avoid:**
Do not trust `./gradlew test` exiting 0 as sufficient evidence after the JUnit bump — parse or read the actual test-count summary Gradle prints (`XXX tests completed`) and assert it reads **467**, not merely "0 failed." If the count drops, the build is still green and this is exactly the silent-failure shape to distrust. As a stronger gate, deliberately break one test (rename its `@Test`-annotated method to something that shouldn't match, or introduce a trivial `assertTrue(false)`) after the JUnit bump and confirm the suite *does* fail red — this reuses the project's own "prove the guard fails on unfixed code" discipline applied to the test-runner configuration itself, not just to the product-level guards it's normally applied to.

**Warning signs:**
- Test count in the Gradle summary is anything other than 467 after the bump, even if 0 are reported failed.
- `./gradlew test --tests "SomeKnownTestClass"` reports "no tests found" for a class that's known to contain tests.

**Phase to address:** Dependency-bump phase — assert the exact 467 count (or the new expected count, if any tests were touched by Pitfall 1's mechanical fixes) as a named verification step immediately after the JUnit version bump, before moving on to Gradle/JDK bumps in the same phase.

---

### Pitfall 12: Gradle 9's stricter Kotlin-DSL/configuration-cache defaults are a real category, but this project's actual scripts were checked and are clear — don't spend phase time re-litigating what's already ruled out

**What goes wrong:**
Gradle 9.0's official upgrade guide (fetched directly, HIGH confidence) lists several breaking-change categories: removal of `jcenter()`, `Project#exec()`/`Project#javaexec()`, `GroovySourceSet`/`ScalaSourceSet`, the Convention system, and custom build-layout flags (`-b`, `-c`, `--settings-file`); Kotlin-DSL script-instance labels like `this@Build_gradle` no longer resolving (use `project`/`settings`/`gradle` directly); and stricter JSpecify-based nullability checking for Kotlin-DSL code such that `Property<String?>` (nullable generic) fails to compile. A grep of this project's four `*.gradle.kts` files for `jcenter`, `.exec(`, `javaexec`, `GroovySourceSet`, `this@Build_gradle`, and `this@Settings_gradle` returned **zero matches** — none of these specific removed-API patterns are present in `library/build.gradle.kts`, `showcase/build.gradle.kts`, `build.gradle.kts` (root), or `settings.gradle.kts`. This project also has no custom Gradle plugin code (`buildSrc`/included builds) where the JSpecify-nullability strictness would bite. Configuration-cache: Gradle 9 makes unsupported build-event listeners a hard configuration-cache failure rather than a silent discard — this project's one dynamic-task-configuration usage (`showcase/build.gradle.kts:27-31`, `tasks.withType<JavaExec>().configureEach { ... }` to forward `-Paero.scheme`) is a standard, supported task-configuration pattern, not a build-event listener, so it is not expected to trip this specific change (MEDIUM confidence — the pattern is standard Gradle API, not independently smoke-tested against Gradle 9 in this research pass).

**Why it happens:**
Listed here specifically so the roadmap does not allocate open-ended investigation time to "check for Gradle 9 breaking changes in our scripts" as an unbounded task — the bounded, mechanical part of that check has already been done.

**How to avoid:**
Treat the removed-API and script-label categories as cleared by this grep, not as a phase-blocking research item — the actual work in the Gradle-bump portion of the toolchain phase should be: bump the wrapper (`gradlew wrapper --gradle-version 9.7.1`), run a full build with `--configuration-cache` enabled explicitly at least once to catch anything this static grep can't (dynamic/runtime configuration-cache incompatibilities), and treat any failure there as the first real signal, not a re-scan of the script text for patterns already ruled out.

**Warning signs:**
- A configuration-cache-related build failure that references something *not* in this pitfall's cleared list — that's a genuine new finding, worth its own investigation; a failure referencing `jcenter`/`exec`/`javaexec`/`GroovySourceSet` would be surprising given the grep and worth double-checking the grep was run against the right files.

**Phase to address:** Toolchain phase — the Gradle-bump step should budget time for the `--configuration-cache` smoke run, not for re-scanning scripts this research already cleared.

---

### Pitfall 13: JitPack's per-tag build cache is sticky on failure — this project already learned this once; the same throwaway-tag discipline must be reapplied for `v3.1.0`, including for the JDK-21/Gradle-9 combination specifically

**What goes wrong:**
Per JitPack's own FAQ (fetched directly, HIGH confidence): a failed build at a given tag is served as-is on future requests — *"if your first build wasn't successful you can rebuild it, but only if you sign in and manually remove the old build to trigger a fresh one"* — and public artifacts become immutable after 7 days regardless. This project already internalized this lesson for v3.0 (PROJECT.md: *"Phase 15 деliberately proved JitPack with throwaway tags (`v3.0.0-alpha01`, `-verify01/02`) instead of bumping mid-milestone"*) — the risk for v3.1 is specifically that JitPack's own build environment must independently support Gradle 9 + JDK 21 (a combination this project has never asked JitPack to build before), and if the *first* tag pushed to test this is the real `v3.1.0` tag rather than a throwaway, any JitPack-environment-specific failure (missing JDK 21 availability on JitPack's build image, a `jitpack.yml` misconfiguration, a timeout on a heavier Gradle 9 first-run) permanently taints that exact tag string.

**Why it happens:**
JitPack's own infrastructure (available JDKs, default Gradle version, timeout budgets) is out of this project's control and can differ from what builds locally — the only way to learn whether `jdk: openjdk21` in `jitpack.yml` (updated from the current `openjdk17`) actually works on JitPack's build image is to ask JitPack to build it, and the first ask should never be the real release tag.

**How to avoid:**
Repeat the exact v3.0 pattern: push one or more `v3.1.0-alpha0N`/`-verify0N` tags first, update `jitpack.yml`'s `jdk:` entry (and the `sdk install java`/`sdk use java` lines, currently pinned to `17.0.10-tem`) to a JDK 21 distribution identifier verified to exist on JitPack, and only cut the real `v3.1.0` tag once a throwaway tag has built green end-to-end on JitPack's own infrastructure — not merely locally.

**Warning signs:**
- Any JitPack build attempt against a tag reachable only by deleting and recreating that exact tag name — a strong sign a throwaway tag should have been used instead.

**Phase to address:** Release phase — reuse the v3.0-proven throwaway-tag-first sequence; update `jitpack.yml`'s JDK identifier as part of the same commit that updates the wrapper/toolchain versions, not as a separate late step.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|--------------------|-----------------|------------------|
| Trust "build green, 0 failed" without reading the actual test count after the JUnit bump | Faster to move on | Silent test-discovery regression ships undetected (Pitfall 11) | Never — assert the 467 count explicitly, every time a test-infra dependency changes |
| Skip the `dependencyInsight` check after adding Hot Reload to `:showcase` | Saves one command | Material3 alpha silently backs the showcase, masking or faking visual drift in the AFTER screenshots (Pitfall 2) | Never — this is a one-time, cheap check with a real, previously-hit failure mode behind it |
| Cut the real `v3.1.0` tag first "to save a round-trip" instead of a throwaway JitPack verify tag | One fewer tag/push cycle | A JitPack-environment-specific JDK21/Gradle9 failure permanently taints the real release tag (Pitfall 13) | Never — this project already paid for this lesson once in v3.0 |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|-----------------|-------------------|
| MCP client ↔ `.mcp.json` ↔ `gradlew` on Windows | Point `command` at `./gradlew` (POSIX script, no extension) and assume shell dispatch happens | `cmd /c gradlew.bat ...` (milestone's own plan, confirmed correct against a real-world failure) or the portable `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` fallback |
| Compose Hot Reload MCP server tools | Start `hotMcpServer` alone and expect a screenshot/click target to exist | Start the showcase app first (`hotRunJvm`/equivalent), *then* connect the MCP server — it attaches to a running app, it does not launch one |
| JitPack + `jitpack.yml` JDK pin | Update the wrapper/toolchain locally to JDK 21 and forget `jitpack.yml` still says `openjdk17`/`17.0.10-tem` | Update `jitpack.yml`'s `jdk:` and `sdk install/use java` lines in the same commit as the toolchain bump, verified via a throwaway tag |
| Gradle Module Metadata `org.gradle.jvm.version` | Assume a JDK-17 consumer will get a clear runtime crash they can Google | It's a *resolution-time* failure for Gradle consumers (different symptom, different search terms) — verify the actual message text once and cross-reference it in the README |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|-----------------|
| `--no-daemon` cold Gradle+JVM start on every MCP server connection | First tool call of a session times out or hangs; retry immediately after succeeds | Time the actual cold start once; report the number rather than assuming it fits the client's timeout | Any MCP client with a connection/handshake timeout shorter than this project's cold-start time on this machine |
| Orphaned `java.exe`/JBR processes after MCP client disconnect without graceful shutdown | Idle CPU/memory creep tied to a showcase/hot-reload process with no visible owning terminal | Explicitly check for and document cleanup of orphaned processes as part of ending a verification session | Every session where the MCP client is killed/crashes rather than cleanly disconnecting |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Treating the Hot Reload MCP server as safe-by-default because it's "just for showcase, just for dev" | Any local process that can reach the MCP server's stdio/port can drive a live click/screenshot/reload session against a window on the maintainer's own desktop — scope this is deliberately narrow (`:showcase` only, never shipped in the published artifact per the milestone's own scope), but confirm nothing in `:showcase`'s Hot-Reload wiring is reachable outside a local, maintainer-initiated session | Confirm the MCP server binds/communicates only via the stdio transport the MCP client itself spawns (matching the `.mcp.json` `command`/`args` model), not an open network port reachable by other local processes or, worse, other machines on the network |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|--------------|-------------------|
| MCP debugging session moves the real mouse cursor or steals OS focus while the maintainer is using the computer for something else | Directly contradicts this milestone's own explicit goal ("доказать, что этот способ отладки не мешает мейнтейнеру пользоваться компьютером") — if unverified, this is not a minor rough edge, it's the milestone's core deliverable failing silently | Treat the cursor-movement/focus-stealing check as a hard, binary pass/fail gate with a concrete test (overlapped window, minimized window, maintainer actively typing elsewhere during a session) before calling the MCP integration done — the milestone's own "stop and ask" rule applies directly if this turns out to be true |
| A consumer upgrades to `3.1.0` expecting a routine minor bump and hits a wall of compile/resolution errors with no prior warning | Consumer trust erosion, wasted debugging time on *their* end for a known, documented (but not surfaced to them beforehand) break | Floor statement immediately adjacent to the dependency-declaration snippet in README and in the release notes (Pitfall 9) |

## "Looks Done But Isn't" Checklist

- [ ] **"All dependencies bumped"**: Often missing the mechanical kotlinx-datetime API renames (`Clock` import, `.dayOfMonth`→`.day`, `.monthNumber`→`.month.number`) that a version-catalog bump alone does not perform — verify the build actually *compiles*, not just that the version string changed (Pitfall 1).
- [ ] **"Material3 stays pinned at 1.9.0"**: Often true for `:library` and `:showcase`'s own declared dependency, but not verified against *new* dependencies (Hot Reload devtools) that independently request the alias — verify via `dependencyInsight`, not by re-reading the pin line (Pitfall 2).
- [ ] **"467 tests green"**: Often means "0 reported failed," not "467 actually ran" — read the printed count, and confirm a deliberately-broken test fails red (Pitfall 11).
- [ ] **"MCP server connected"**: Often means the process spawned successfully, not that a running showcase app exists for it to attach to and screenshot — verify a `take_screenshot` call actually returns a real window image before trusting the setup (Pitfall 4).
- [ ] **"No visual drift found"**: Often means "no drift found at 100% DPI, using an unstated/uncalibrated capture pipeline" — name the DPI scale and run the same-toolchain BEFORE/AFTER capture-method calibration pass before trusting a diff (Pitfalls 7, 8).
- [ ] **"JitPack build green"**: Often means the *throwaway* tag built green — confirm the real `v3.1.0` tag itself was never previously pushed and failed (which would permanently taint it) before treating the final release as done (Pitfall 13).

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|-----------------|------------------|
| kotlinx-datetime compile breaks (Pitfall 1) | LOW | Mechanical rename across the ~8 confirmed call sites; re-run picker/calendar tests; no design decision required |
| Material3 alpha leak via Hot Reload devtools (Pitfall 2) | LOW | Add `resolutionStrategy.force(...)` scoped to `:showcase`; re-run `dependencyInsight` to confirm |
| Silent test-count regression (Pitfall 11) | MEDIUM | Bisect which dependency bump (JUnit vs. CMP vs. coroutines) changed the discovered count by reverting one at a time; likely a missing engine/capability wiring, not a design problem |
| A real `v3.1.0` JitPack tag fails and is now tainted (Pitfall 13) | MEDIUM | Per JitPack's own FAQ, sign in and manually delete the failed build within the mutability window, or cut a new patch tag (`v3.1.1`) and treat the failed tag as abandoned — do not attempt to silently "fix and re-push" the same tag |
| MCP debugging found to move the cursor / steal focus (UX Pitfalls) | HIGH | Per the milestone's own locked rule, this is a stop-and-ask-the-maintainer situation, not a workaround-and-proceed one — no silent recovery path is appropriate |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|-------------------|----------------|
| 1. kotlinx-datetime `Clock`/`.dayOfMonth`/`.monthNumber` compile breaks | Dependency-bump phase (isolated first commit) | Build compiles; picker/calendar test files pass unchanged |
| 2. Material3 alpha leak via Hot Reload devtools | Hot Reload/MCP setup phase | `dependencyInsight --dependency material3` resolves to `1.9.0` in `:showcase` |
| 3. `uiTest` `StandardTestDispatcher` default change | Dependency-bump phase (CMP bump step) | Full 467-test suite run immediately after CMP bump, isolated from other bumps |
| 4. Windows `gradlew`/POSIX + MCP attach-to-running-app prerequisite | Hot Reload/MCP setup phase | `cmd /c gradlew.bat ... hotMcpServer` launches cleanly; `take_screenshot` returns a real image only after the app is started first |
| 5. `--no-daemon` cold-start latency / orphaned processes | MCP-safety-verification phase | Cold-start time measured and reported; orphaned-process check after a disconnected session |
| 6. stdout noise corrupting stdio MCP protocol | Hot Reload/MCP setup phase | Raw stdout captured and inspected once on first setup |
| 7. DPI scale unstated in "no drift" verdict | Verification phase | Final report names the DPI scale explicitly |
| 8. BEFORE/AFTER capture-pipeline mismatch | Verification phase | Same-toolchain, both-capture-method calibration pass run before trusting any real diff |
| 9. Minor-version-numbered breaking release under-documented | Release phase | Floor statement present adjacent to the dependency snippet in README and release notes |
| 10. Gradle Module Metadata JDK-17-consumer resolution failure | Release phase (throwaway-tag verification) | Actual consumer-side error text read once from a scratch JDK-17/Gradle-8 project |
| 11. Silent JUnit test-discovery regression | Dependency-bump phase (JUnit bump step) | Exact test count asserted (467, or updated count); deliberately-broken test confirmed to fail red |
| 12. Gradle 9 removed-API/script-label breakage | Toolchain phase | Already cleared by grep (documented here); `--configuration-cache` smoke run as the remaining check |
| 13. JitPack sticky failed-build cache for the JDK21/Gradle9 combination | Release phase | Throwaway `v3.1.0-alpha0N`/`-verify0N` tag built green on JitPack before the real tag is pushed |

## Sources

- [Compose Multiplatform 1.12.0 Released — JetBrains Blog](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/) — HIGH, official
- [Compose Multiplatform CHANGELOG.md (1.12.0 entry)](https://raw.githubusercontent.com/JetBrains/compose-jb/master/CHANGELOG.md) — HIGH, official
- [kotlinx-datetime CHANGELOG.md](https://github.com/Kotlin/kotlinx-datetime/blob/master/CHANGELOG.md) — HIGH, official, fetched verbatim for 0.7.0/0.7.1/0.8.0
- [kotlinx-datetime `LocalDate` API docs](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-local-date/-local-date.html) and [`LocalDate.day`](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-local-date/day.html) — HIGH, official
- [JUnit 6.0.0 Release Notes](https://docs.junit.org/6.0.0/release-notes.html) / [Upgrading to JUnit 6.0](https://github.com/junit-team/junit-framework/wiki/Upgrading-to-JUnit-6.0) — HIGH, official
- [Gradle 9.0.0 Upgrading Guide](https://docs.gradle.org/current/userguide/upgrading_major_version_9.html) / [Gradle 9.0.0 Release Notes](https://docs.gradle.org/9.0.0/release-notes.html) — HIGH, official
- [JitPack Building docs](https://docs.jitpack.io/building/) / [JitPack FAQ](https://docs.jitpack.io/faq/) — HIGH, official
- [Kotlin 2.4.20 Released — JetBrains Blog](https://blog.jetbrains.com/kotlin/2026/09/kotlin-2-4-20-released/) / [What's new in Kotlin 2.4.20](https://kotlinlang.org/docs/whatsnew2420.html) — HIGH, official
- [kotlinx.coroutines 1.11.0 release](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0) — HIGH, official
- [Compose Hot Reload configuration docs](https://jetbrains-compose-hot-reload.mintlify.app/usage/configuration) — MEDIUM, official-adjacent (mirrored docs site)
- [aoreshkov/kmp-ledger PR #27 — "launch the hot-reload MCP server through the wrapper JAR"](https://github.com/aoreshkov/kmp-ledger/pull/27) — MEDIUM, third-party but directly corroborates the milestone's planned Windows workaround
- Java class-file version 65 = JDK 21 mapping — general Java documentation consensus (mkyong.com, javaalmanac.io), MEDIUM
- This project: `C:\1A_WORK\ui_lib\library\build.gradle.kts`, `showcase\build.gradle.kts`, `gradle\libs.versions.toml`, `settings.gradle.kts`, `jitpack.yml`, `README.md`, `gradle\wrapper\gradle-wrapper.properties`, and grep results against `library\src` — HIGH, primary source
- `.planning/research/_archive-v3.0/PITFALLS.md` and `.planning/PROJECT.md` — HIGH, primary source, prior-milestone locked lessons

---
*Pitfalls research for: aero-compose-ui v3.1 Dependency Refresh + Hot Reload MCP*
*Researched: 2026-09-21*
