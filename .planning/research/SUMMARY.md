# Project Research Summary

**Project:** aero-compose-ui — v3.1 "Dependency Refresh + Hot Reload MCP"
**Domain:** Forced toolchain/dependency refresh (Kotlin/CMP/Gradle/JDK/coroutines/kotlinx-datetime/JUnit) + first-time Compose Hot Reload MCP server install, on an already-shipped Compose Desktop (JVM) component library published via JitPack
**Researched:** 2026-09-21
**Confidence:** MEDIUM-HIGH overall — every target version confirmed against Maven Central / services.gradle.org; the Hot Reload + MCP interaction is confirmed via primary GitHub source reads at the exact `v1.2.0` tag; several genuine conflicts between researchers remain and are called out explicitly below rather than silently resolved

## Executive Summary

This is not new-product research — it is a forced-upgrade research pass for a mature, ~50-component Compose Desktop UI library that already shipped a toolchain migration once (v3.0, Kotlin 2.4.10 + CMP 1.11.1) and locked a hard-won lesson from it: isolate dependency-upgrade risk from drawing-code risk so a regression is always attributable to one cause. All four researchers agree the target version set (Kotlin 2.4.20, CMP 1.12.0, Gradle 9.7.1, JDK 21, coroutines 1.11.0, kotlinx-datetime 0.8.0 plain, JUnit 6.1.3, Compose Hot Reload 1.2.0) is mutually compatible with no hard blocker, and that the correct execution shape is a small number of coarse-grained, independently-gated steps (build-tool bump -> Kotlin/CMP pair -> supporting libraries -> Hot Reload/MCP wiring -> empirical MCP safety proof -> agent visual sweep -> release), matching the milestone's own "keep it SMALL" framing and the v3.0-proven throwaway-JitPack-tag release discipline.

The single most consequential finding, confirmed independently by FEATURES and ARCHITECTURE researchers reading Compose Hot Reload's actual source at tag `v1.2.0`: the MCP tool set has **two structurally different mechanisms**. `click`/`type_text`/`scroll`/`resize_window`/`get_semantic_tree` are pure in-process semantics-tree invocations — no OS input injection, no cursor movement, no focus theft, and they work even when the showcase window is occluded. `take_screenshot` is a real `java.awt.Robot.createScreenCapture` screen scrape — it does NOT move the cursor or steal focus either, but it DOES capture whatever is actually on top of the screen (garbage if occluded) and it fails entirely if the window is minimized (every window-targeting tool does, because Hot Reload's runtime deliberately unregisters iconified windows). This gives the milestone's core acceptance question — "does MCP debugging get in the way of the maintainer using the computer" — a precise, source-backed, testable answer rather than a hope: the answer is conditionally yes-it's-safe, with occlusion/minimization as the two documented edges to verify empirically, exactly as the milestone's own locked decision demands ("если MCP двигает реальный курсор / крадёт фокус — остановка и вопрос мейнтейнеру").

The second major risk cluster is the kotlinx-datetime 0.6.2->0.8.0 jump inside the library's four date/time pickers, and here the researchers genuinely disagree — not on trivia, but on whether this is a real compile break requiring source changes or a no-op version bump. STACK and PITFALLS both did grep-verified, changelog-cited analysis concluding it IS a break (`Clock` import path change, `dayOfMonth`->`day`, `monthNumber`->`month.number`, four files each). ARCHITECTURE, reading the same changelog, concluded the 0.7.1 typealias reintroduction makes this a zero-source-change, recompile-only bump. Per the "never fill a gap by guessing" rule, this is NOT resolved here — it is flagged as Conflict 1 below, with the cheapest empirical check (compile `:library` against plain 0.8.0) named as the tiebreaker. Three further conflicts (JBR/Java version requirement, Hot Reload task-name/plain-JVM-bundling behavior, and the screenshot-capture mechanism hypothesis vs. source-read) are similarly unresolved and listed for empirical settlement rather than researcher-vote.

## Key Findings

### Recommended Stack

All four researchers independently confirm, via direct Maven Central / services.gradle.org queries, that the maintainer's pre-decided target set is internally consistent: Kotlin 2.4.20 (latest stable) satisfies CMP 1.12.0's `>=2.1.0` floor with wide margin; CMP 1.12.0 (latest stable, released August 2026) bundles Hot Reload 1.2.0; Gradle 9.7.1 is current stable and JDK 21 is inside its supported-JVM range; JUnit 6.1.3 keeps the same Maven coordinates as JUnit 5 and requires only Java 17 / Kotlin 2.2 minimums, both cleared. The one asterisk STACK flags: Gradle 9.7.1 sits one patch above Kotlin 2.4.20's documented "fully compatible" ceiling (9.7.0) — a warning zone per Kotlin's own docs, not a known incompatibility, but not inside the exact tested matrix either.

**Core technologies:**
- Kotlin 2.4.20 — latest stable, satisfies CMP 1.12.0's Kotlin floor with margin
- Compose Multiplatform 1.12.0 — latest stable desktop-target release; bundles/targets Hot Reload 1.2.0; Material3 alias still resolves to an alpha (`1.12.0-alpha03`) — the existing explicit `1.9.0` pin must be kept unchanged
- Gradle 9.7.1 — current stable; requires `foojay-resolver-convention` >=1.0.0 in `settings.gradle.kts` for automatic JDK/JBR toolchain provisioning (not present today; a pre-1.0.0 pin is confirmed broken on Gradle 9)
- JDK 21 — raised everywhere (library, showcase, `jitpack.yml`), not just for the showcase run
- kotlinx-coroutines 1.11.0 — internal-only (`implementation`), no public API leak, low risk
- kotlinx-datetime 0.8.0 (plain, no `-0.6.x-compat`) — see Conflict 1, the one item this document cannot settle from research alone
- JUnit 6.1.3 — coordinates unchanged from JUnit 5; unified Platform/Jupiter versioning simplifies the catalog
- Compose Hot Reload 1.2.0 — `:showcase`-only, never in the published `:library` artifact; ships the MCP server tool surface used for the milestone's visual-QA loop

### Expected Features (this milestone's own scope, not a market feature set)

Because this is an internal tooling/infra milestone rather than a product feature milestone, FEATURES.md instead classified the MCP-driven agent-QA capabilities:

**Must have (table stakes):**
- Reload-and-screenshot round trip (`reload`/`await_reload` -> `take_screenshot`) across three themes
- Structural presence/absence checks via `get_semantic_tree` (works even when occluded)
- Click-driven state changes for anything wired through `onClick`/`onLongClick`
- Popup/Dialog/Tooltip content verification — the library's pervasive `Popup`-based pattern is picked up automatically by `get_semantic_tree` as a separate "owner" root, no extra plumbing needed
- Text field one-shot content entry via `type_text`

**Should have / differentiators:**
- List/table scroll verification (`scroll`/`scroll_to_index`) for `AeroDataTable`/`AeroTreeView`, contingent on the container actually exposing those semantic actions

**Cannot self-verify — remains human-eye or automated-UI-test territory (explicitly out of MCP's reach in v1.2.0):**
- Hover-state visual QA (`AeroButton`/`AeroSwitch`/`AeroSegmentedControl`/`AeroListItem`) — no hover/pointer-move tool exists at all
- Every drag-driven component (`AeroSlider`, `AeroRangeSlider`, `AeroSplitPane`, `AeroPanelGroup` resize, `AeroDataTable` column-resize) — no drag primitive exists; only `scroll`/`scroll_to_index`
- Keyboard focus-visible / Tab-order QA — no generic key-press tool; `type_text` replaces whole field content, it is not keystroke-level

**Anti-features to avoid:**
- Screenshot-only verification loops with no occlusion guard (will silently pass garbage frames)
- Treating `reload` and `await_reload` as interchangeable (one triggers a build, the other only waits)
- Building a custom coordinate-click fallback — there is no coordinate-click tool by design; components lacking proper semantics are a library gap to close, not to work around

### Architecture Approach

The change is structurally simple and well-isolated: a single version-catalog (`gradle/libs.versions.toml`) is the propagation point for every bump, consumed via existing `alias(libs.plugins.*)`/`libs.*` references in both `library/build.gradle.kts` and `showcase/build.gradle.kts` — most of the five version bumps require zero other line changes. Hot Reload is added only to `showcase/build.gradle.kts`'s `plugins{}` block; `:library`'s isolation from it is structurally guaranteed (not just conventional) because Gradle gives each subproject its own independent `plugins{}`/`dependencies{}` graph and the root `build.gradle.kts` deliberately applies no plugins at all. `.mcp.json` (new, repo root) launches `cmd /c gradlew.bat --no-daemon --quiet --console=plain <task>` because Windows cannot spawn the extensionless `./gradlew` POSIX script directly from an MCP client's process launcher.

**Major components:**
1. `gradle/libs.versions.toml` — single version source of truth; five bumps + two new plugin entries (Hot Reload, optionally foojay resolver)
2. `settings.gradle.kts` — new `foojay-resolver-convention` block for automatic JBR/JDK-21 toolchain provisioning; existing JetBrains Space repo URL now redirects (301) and is likely removable per JetBrains' own current template
3. `showcase/build.gradle.kts` — sole home of the Hot Reload plugin and the `ComposeHotRun`-typed `-Paero.scheme` forwarding block (a NEW sibling block; the existing `JavaExec`-typed forwarding block does not reach Hot Reload's own task type)
4. `.mcp.json` (new) — Claude Code stdio MCP registration, Windows `cmd /c gradlew.bat` pattern, no `cwd` field needed
5. `library/build.gradle.kts` — receives only the `jvmToolchain(21)` bump and the datetime/coroutines/JUnit catalog bumps; never touches Hot Reload

### Conflicts Between Researchers / Must Be Resolved Empirically

Per the project standing rule (when a fact is missing: stop and ask; never fill a gap by guessing), these five points are NOT resolved by picking a winner — each is stated with both positions, the evidence behind each, and the cheapest empirical check that settles it. Treat these as open items for the roadmap/plan, not as decided facts.

**1. kotlinx-datetime 0.8.0 — is this a compile break or a no-op recompile?**
- Position A (STACK, PITFALLS): real, grep-verified compile break. Both independently grepped `library/src/main` and found 4 files importing `kotlinx.datetime.Clock` (`AeroDatePicker.kt`, `AeroDateTimePicker.kt`, `AeroDateRangePicker.kt`, `AeroDateTimeRangePicker.kt`) and 4 call sites using `.dayOfMonth`/`.monthNumber` across `AeroDateTimePicker.kt`, `AeroDatePicker.kt`, `AeroCalendarGrid.kt` (x2). Both cite the kotlinx-datetime CHANGELOG 0.7.0 entry (Remove `kotlinx.datetime.Instant` and `kotlinx.datetime.Clock`, rename `dayOfMonth` to `day`, `monthNumber` to `month`) and the 0.8.0 API docs confirming `LocalDate.day: Int` replaces `dayOfMonth`, with `monthNumber` reached only via `.month.number`. PITFALLS frames the milestone own choice to skip the `-0.6.x-compat` artifact as precisely what converts a deprecation into a hard `unresolved reference` error.
- Position B (ARCHITECTURE): zero source changes expected. Grepping the same files, ARCHITECTURE found the exact same `Clock.System.now()` call pattern but concluded the 0.7.1 release reintroduced `kotlinx.datetime.Instant`/`Clock` as type aliases pointing at the `kotlin.time` equivalents, restoring source compatibility for `import kotlinx.datetime.Clock; Clock.System.now()` unchanged — it is now just resolving through an alias. ARCHITECTURE does not separately address the `dayOfMonth`/`monthNumber` renames in this framing, which is itself a gap in Position B own analysis (STACK/PITFALLS renames claim is not rebutted, only the `Clock` import claim is).
- What is itself unsettled: whether the CHANGELOG later renames (`dayOfMonth`->`day`, `monthNumber`->`month`) are hard removals (STACK/PITFALLS reading) or soft-deprecated-but-still-present in 0.8.0 (which would make ARCHITECTURE zero-changes claim at least partially right). No researcher fetched and quoted the exact 0.8.0 release notes/migration guide text for this specific rename deprecation status — all four are reasoning from the aggregate CHANGELOG history plus current API-doc pages, not a single authoritative 0.8.0-specific statement.
- Cheapest empirical check: compile `:library` against plain `kotlinx-datetime:0.8.0` (bump only that one catalog entry, nothing else) and read the actual compiler errors, if any. This is a five-minute check that fully resolves the conflict — do it before committing to either the mechanical-fix-as-its-own-first-commit plan (Pitfall 1) or an expect-zero-changes plan.

**2. JetBrains Runtime (JBR) version requirement — Java 21 only, or JBR 25 by default?**
- Position A: kotlinlang.org Compose Hot Reload docs state that the latest JetBrains Runtime supports only Java 21 — adding Compose Hot Reload to a project only compatible with Java 22 or newer results in a linkage error.
- Position B: The Hot Reload 1.2.0 GitHub release notes (primary source, `gh release view v1.2.0`, dated 2026-07-23) state JBR 25 by default, with the `compose.reload.jbr.min.version` property available to configure the minimum supported JBR version.
- STACK own proposed reconciliation is explicitly unverified: that JBR 25 refers to the JBR build own major version (OpenJDK-25-based), while the supports-only-Java-21 warning is about the project own compile/bytecode target — meaning a JBR-25-based runtime could still execute a project whose `jvmToolchain` is 21 without linkage error. This reading is plausible but not confirmed by any single source that reconciles both texts.
- Cheapest empirical check: the first `hotRun`/`hotMcpServer` invocation in Phase work will prove or disprove this directly (a linkage error surfaces immediately, loudly). No pre-work needed beyond running it and reading the output.

**3. Hot Reload plugin bundling and exact task names for a plain `kotlin.jvm` module**
- Position A (STACK, corroborated by ARCHITECTURE citing a Gradle-plugin unit test): the GitHub README states explicitly, in the Run tasks section, that `:hotRun` is for Kotlin/JVM projects (not `:hotRunJvm`, which is for multiplatform) and that in a plain Kotlin/JVM module the task is simply named `hotMcpServer`. ARCHITECTURE additionally cites `ComposeHotRunTasksTest.kt` (test "default run task name - jvm") asserting task names `hotRun`/`hotDev` for exactly this plugin combination — a primary-source unit test, the strongest evidence found by any researcher on this point.
- Position B (STACK own flagged caveat): two independent WebFetch-summarized passes over kotlinlang.org docs page and the official Quickstart page returned the opposite claim — that plain `kotlin.jvm` is not supported and requires conversion to multiplatform. JetBrains own two sample projects (`samples/counter`, `samples/bytecode-analyzer`) both use `kotlin("multiplatform") { jvm() }`, not plain `kotlin("jvm")`, meaning JetBrains own demos never exercise the path the README documents as a first-class case.
- Whether bundling is automatic for `:showcase` is also unconfirmed either way — kotlinlang.org "bundled and enabled by default for all projects that include a desktop target" wording is written for KMP-shaped projects; no source states whether this auto-activation extends to plain-JVM modules. All four researchers converge on the same mitigation regardless of the answer: apply the plugin explicitly in `showcase/build.gradle.kts`.
- Cheapest empirical check: `./gradlew :showcase:tasks --all | grep -i hot` after applying the plugin, before writing `.mcp.json` or assuming any task name. This single command resolves both the bundling question and the exact task-name question at once.

**4. Gradle 9.7.1 vs. Kotlin 2.4.20 documented compatibility ceiling**
Not a disagreement between researchers, but a genuine warning-zone fact worth carrying into the roadmap as-is: Kotlin 2.4.20 own What's-new page states it is fully compatible with Gradle 7.6.3 through 9.7.0, and that later Gradle versions may result in deprecation warnings and some new Gradle features might not work. The decided target, 9.7.1, is one patch above that documented ceiling — extremely low risk (a patch release of the same minor line), but not inside JetBrains own exactly-tested matrix. Treat a real `./gradlew build` as the actual gate here, not the docs.

**5. `take_screenshot` capture mechanism — Robot screen-scrape vs. hypothesized in-process Skia capture**
- FEATURES (stronger evidence — direct source read): read `hot-reload-runtime-jvm/.../screenshotHandler.kt` at commit `3315f8dd8f5c` (tag `v1.2.0`) directly and found `Robot().createScreenCapture(rect)` — a literal `java.awt.Robot` OS-level screen-pixel read of the window on-screen rectangle, not off-screen rendering of the Compose scene. FEATURES additionally cites the v1.3.0-alpha02 release notes confirming this Robot path is explicitly the fallback for previous Compose versions, with the newer `captureContentToImage()` off-screen path only landing at CMP 1.13+ — version 1.2.0/CMP 1.12.0, what this milestone installs, is squarely in the Robot-only bucket, confirmed both by source and by the vendor own forward-looking changelog language.
- PITFALLS (hypothesis, explicitly flagged as unverified): hypothesized the screenshot tool is very likely an in-process Skia-surface capture, used to build a BEFORE/AFTER calibration-pass recommendation (compare the project existing `CopyFromScreen`-based capture method against MCP method on the same, unchanged toolchain, before trusting any real diff) — explicitly labeling this UNVERIFIED and a hypothesis to confirm, not a fact.
- Resolution: FEATURES direct source read at the exact pinned tag is the stronger, decisive evidence — take it as settled that `take_screenshot` is `Robot.createScreenCapture`, a true OS screen-scrape, same general capture class as the project existing `CopyFromScreen` (Win32) method, though not the same API or necessarily the same DPI/color/chrome handling.
- Consequence that changes PITFALLS own recommendation, not just its confidence label: BEFORE (`CopyFromScreen`) and AFTER (`Robot.createScreenCapture`) are therefore BOTH real screen captures, not one screen-capture and one off-screen render — this is a more apples-to-apples comparison than PITFALLS Skia-hypothesis framing implied, but PITFALLS underlying calibration-pass recommendation should still be kept: two different screen-capture APIs can still differ in DPI handling, window-chrome inclusion, or gamma/color-space treatment, so running one same-toolchain calibration pass (both methods, same screen state, before any intentional change) before trusting a real BEFORE/AFTER diff remains the correct precaution — just for a narrower, better-understood reason than originally hypothesized.

### Consequences That Bear Directly on Acceptance Criteria

These are not in dispute between researchers — they are confirmed, source-backed facts (mostly from FEATURES direct read of Hot Reload source at `v1.2.0`) that should be treated as fixed constraints when the roadmap defines the MCP-safety-verification phase and the what-the-agent-can/cannot-self-verify scope:

- `click`/`type_text`/`scroll`/`resize_window` are in-process semantics actions: no real cursor movement, no focus steal, and they work even when the showcase window is occluded by another window.
- `take_screenshot` needs the showcase window actually visible on screen and not minimized; if occluded, the captured image shows the occluding window content, not the showcase; popups/dialogs that overflow the window own bounds are clipped from the screenshot (they render as an additional semantics "owner" painted onto the same window Skia surface, not a separate OS window, so `Robot` window-rectangle capture cannot see the overflow).
- Minimizing the showcase window removes it from `list_windows` and fails every window-targeting tool (including the screenshot tool) — this is a deliberate design choice in Hot Reload runtime, not a bug to route around.
- The 16-tool MCP surface has NO hover/pointer-move tool, NO generic key-press tool (Tab/arrows/Enter/Escape — `type_text` only replaces a field entire content), and NO drag tool. Hover states, focus-visible keyboard rings, and every custom-drag component (`AeroSlider`, `AeroRangeSlider`, `AeroSplitPane`, `AeroPanelGroup` resize, `AeroDataTable` column-resize) cannot be self-verified by the agent through MCP — those remain human-eye or automated-UI-test territory. This directly limits how much of v3.0 hover/focus/drag calibration work this milestone agent-driven sweep can actually check.
- `hotMcpServer` attaches to an already-running app (started separately via `hotRun`); it does not launch the app itself. There is no `stop`/`shutdown`/`kill` tool — only `restart`, which relaunches rather than cleanly stops. Cleanly ending a session requires killing the process outside the MCP protocol.
- `reload` (explicit, non-`--auto` mode) spawns its own background Gradle sub-process regardless of how `hotMcpServer`/`hotRun` were themselves launched — passing `--no-daemon` to `hotMcpServer` does not prevent Hot Reload from leaving its own Gradle daemon running once the agent calls `reload` a first time.
- The existing `-Paero.scheme` theme-forwarding hook is keyed on `tasks.withType<JavaExec>()` matching `name == "run"` — Hot Reload run task is a different, non-`JavaExec` task type (`ComposeHotRun`) and will not be reached by the existing block; a sibling forwarding block is needed, or theme preselection falls back to driving the MCP `click` tool against the theme switcher (a documented, acceptable fallback per the milestone own anticipated point 3).
- Hot Reload devtools overlay (`hot-reload-devtools`, pulled in only by `:showcase` once the plugin is applied) independently depends on Material3 via the same `compose.material3` alias the project already knows resolves to an alpha on this CMP version — a NEW leak vector the existing `1.9.0` pin was never tested against. Check with `./gradlew :showcase:dependencyInsight --dependency material3 --configuration runtimeClasspath` immediately after adding the plugin, before writing `.mcp.json`.
- The Gradle wrapper/toolchain distribution download (and, separately, first-run JDK/JBR auto-provisioning) prints progress lines to stdout that `--quiet --console=plain` do NOT suppress (confirmed via `gradle/gradle` issues #1845 and #5213) — this can corrupt the JSON-RPC stdio stream the MCP protocol depends on. Pre-warm the wrapper and toolchain once, interactively, before ever registering `.mcp.json`.
- Build green, 0 failed is not the same claim as all 467 tests actually ran — a JUnit major-version bump can silently reduce test-discovery count while still exiting 0. Assert the literal test count (467, or the updated count if Pitfall 1 mechanical fixes touch any test files), and confirm a deliberately-broken test still fails red, as a named verification step.
- The README toolchain-requirements line (`README.md:6`) is stale since before v3.0 shipped — it still names Kotlin 2.1.21/CMP 1.7.3/JVM 17, never updated when v3.0 actually landed Kotlin 2.4.10/CMP 1.11.1/JDK 17. This milestone touches the same lines anyway and should fix the staleness while it is there, in addition to stating the new v3.1 floor.

### Critical Pitfalls (top 5 of 13 catalogued; see PITFALLS.md for the full set)

1. kotlinx-datetime `Clock`/`.dayOfMonth`/`.monthNumber` compile breaks (contested — see Conflict 1) — if Position A is correct, this is a confirmed, grep-verified break in 4+4 files; fix as its own isolated commit, gated by the picker/calendar test files, before any other bump in the same phase.
2. Material3 alpha leak via Hot Reload devtools dependency — a NEW leak vector the existing `1.9.0` pin was never tested against; verify with `dependencyInsight` immediately after adding the Hot Reload plugin, force-pin if needed, scoped to `:showcase` only.
3. CMP 1.12 `uiTest` default dispatcher change (`StandardTestDispatcher`) — can silently alter timing assumptions in `runComposeUiTest`-based tests (notably the RCMP drag regression guard); run the full 467-test suite immediately after the CMP bump, isolated from other bumps, and suspect the dispatcher default before suspecting product code if a `uiTest` starts flaking.
4. Windows cannot spawn `./gradlew` from `.mcp.json`, and the MCP server only attaches to an already-running app — two independently-confirmed real-world failure modes (a third-party PR corroborates both); use `cmd /c gradlew.bat ...`, and always start `hotRun` before connecting the MCP server.
5. Silent JUnit test-discovery regression (0 tests reasoned as fewer, build still green) — do not trust exit code 0; read and assert the literal printed test count, and prove the gate would fail on a deliberately-broken test.

## Implications for Roadmap

Given the v3.0-locked lesson (mixing dependency-upgrade risk with drawing-code risk loses the ability to attribute a regression to one cause) and ARCHITECTURE own argued build order, the roadmap should use a coarse, four-to-six-phase structure — fine enough to keep each category individually attributable, coarse enough to respect the milestone explicit keep-it-SMALL framing. All four researchers converge on substantially the same ordering; this is the strongest-consensus part of the research.

### Phase 0 (pre-phase, not a roadmap phase): Baseline capture
Rationale: must happen before the MCP server exists — the MCP server only appears once CMP 1.12.0 lands, and `CopyFromScreen` is the only capture method available on the current toolchain.
Delivers: manual before-screenshots, three themes, every showcase section, on unchanged CMP 1.11.1.
Avoids: Pitfall 8 (BEFORE/AFTER capture-pipeline mismatch) — this is the BEFORE half of the eventual calibration pair.

### Phase 1: Build-tool bump alone (Gradle 9.7.1, JDK 21, foojay resolver)
Rationale: isolates whether the build tool still builds the unchanged v3.0 code from every other risk — a failure here can only mean Gradle/JDK, nothing else changed.
Delivers: wrapper bumped, `jvmToolchain(21)` everywhere, `foojay-resolver-convention` >=1.0.0 added to `settings.gradle.kts`, `org.gradle.welcome=never` added.
Avoids: Pitfall 12 (Gradle 9 removed-API breakage — already grep-cleared, budget only a `--configuration-cache` smoke run, not open-ended re-scanning).
Gate: 467 tests green, showcase starts.

### Phase 2: Kotlin 2.4.20 + Compose Multiplatform 1.12.0
Rationale: the highest-risk single jump — compiler/runtime co-evolution, not library API surface; isolating it means a regression is attributable to the compiler/runtime pair alone. This is also the phase that resolves Conflict 1 empirically (compile `:library` against the bumped Kotlin/CMP pair — if the datetime catalog entry is bumped in the same step, keep the compile-check separate enough to know which change caused any given error; safest is to bump datetime in Phase 3 as planned and let this phase prove Kotlin/CMP alone first).
Delivers: Kotlin/CMP catalog bump only; Material3 pin verified still `1.9.0` (not yet threatened — Hot Reload is not added until Phase 4).
Avoids: Pitfall 3 (`uiTest` dispatcher default change) — run the full suite immediately after this step, isolated.
Gate: 467 tests green, showcase starts, no compile errors.

### Phase 3: Supporting libraries together (coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3)
Rationale: these three touch disjoint code paths (coroutines = internal-only, datetime = 4 picker files, JUnit = test infra only) — a test failure already identifies which one broke regardless of bundling, so splitting further buys nothing against the keep-it-SMALL goal. This is where Conflict 1 gets its final empirical answer and, if Position A holds, the mechanical `Clock`/`day`/`month.number` fixes land as their own sub-commit within this phase, gated by the picker/calendar test files before the coroutines/JUnit portions.
Delivers: all three catalog bumps; kotlinx-datetime migration fixes if needed; test count asserted literally (467 or updated), with a deliberately-broken-test proof that the gate itself would fail red.
Avoids: Pitfall 1 (datetime break) and Pitfall 11 (silent JUnit discovery regression).
Gate: 467 tests green (or documented updated count), literal count asserted, JUnit gate proven to fail on purpose.

### Phase 4: Hot Reload + MCP wiring, `:showcase` only
Rationale: doing this after dependency stabilization means a stdio/task-name/POM-leak problem is diagnosed against an already-known-good dependency graph. Hard prerequisite: CMP 1.12.0 (Phase 2) must be green first.
Delivers: Hot Reload plugin applied in `showcase/build.gradle.kts` only; `git diff --stat -- library/build.gradle.kts` empty (structural isolation proof); `ComposeHotRun`-typed `-Paero.scheme` forwarding block added; `dependencyInsight --dependency material3` confirmed resolving to `1.9.0`; exact task names discovered via `:showcase:tasks --all` (resolves Conflict 3) before `.mcp.json` is written; `.mcp.json` wired with `cmd /c gradlew.bat --no-daemon --quiet --console=plain <task>`; stdout captured once and inspected for non-JSON-RPC noise (wrapper-download/JBR-provisioning pre-warm).
Avoids: Pitfall 2 (Material3 alpha leak via devtools), Pitfall 4 (Windows spawn + attach-to-running-app), Pitfall 6 (stdio noise).

### Phase 5: MCP non-interference proof + agent visual sweep
Rationale: empirical by nature — no amount of research substitutes for the milestone own mandated hands-on test with the maintainer at the keyboard. This is also where Conflict 2 (JBR/Java version) gets its practical answer (first `hotRun` either links cleanly or does not).
Delivers: documented, not assumed, pass/fail results for cursor-position (byte-identical before/after), focus (foreground window never changes), occlusion (split by tool: `get_semantic_tree`/`click`/`scroll` succeed occluded, `take_screenshot` does not), and minimized-window (window absent from `list_windows`, clean error not a hang) — matching FEATURES summary table exactly; then the agent own three-theme walkthrough via MCP, diffed against Phase 0 baseline, with the same-toolchain calibration pass (Pitfall 8 / Conflict 5) run first so any diff found is attributable to real drift and not a capture-pipeline artifact; DPI scale used for the walkthrough named explicitly in the final report (Pitfall 7 — do not let an unstated 100%-only sweep silently read as all-scales-covered, continuing the same gap v3.0 already flagged and waived).
Avoids: Pitfall 5 (cold-start latency / orphaned processes — measure and document), Pitfall 7 (DPI scope creep in the report), Pitfall 8 (capture-pipeline false drift).

### Phase 6: Release
Rationale: everything above must be green before cutting the real tag — mirrors the v3.0-proven throwaway-tag discipline exactly.
Delivers: version bumped to `3.1.0` in root `build.gradle.kts` (before tagging, per the locked bump-on-milestone rule); README toolchain-requirements section updated (both the stale header line and a new floor statement placed immediately adjacent to the dependency-coordinate snippet, not just in prose elsewhere); `jitpack.yml` updated to JDK 21 / current Temurin identifier; one or more throwaway `v3.1.0-alpha0N`/`-verify0N` tags proven green on JitPack actual build infrastructure before the real `v3.1.0` tag is pushed.
Avoids: Pitfall 9 (under-documented breaking minor release), Pitfall 10 (confusing consumer-side resolution error for JDK-17 holdouts — read the actual error text once during throwaway verification), Pitfall 13 (JitPack sticky failed-build cache tainting the real tag).

### Phase Ordering Rationale

- The four-to-six-step grain (build-tool / Kotlin-CMP-pair / library-set / Hot-Reload-MCP / safety-proof-and-sweep / release) is the coarsest split that still keeps every risk category individually attributable — bundling steps 1-3 would make a regression ambiguous among five independent version changes plus a build-tool change, exactly the ambiguity the v3.0 retrospective identified as costly; splitting step 3 further into three phases would add phase-transition overhead for zero extra attributability, since coroutines/datetime/JUnit touch disjoint code paths regardless of whether they land together.
- Hot Reload/MCP wiring is placed after dependency stabilization (not interleaved) specifically so that a stdio/task-name/POM-leak problem is diagnosed against an already-known-good dependency graph, and because it has a hard prerequisite (CMP 1.12.0) that only exists after Phase 2.
- The MCP-safety-proof phase is placed last among the build phases (before release) because it is explicitly empirical and mandated by the maintainer own locked stop-and-ask rule — no phase before it can substitute for it, and no phase after it should be allowed to proceed without it passing.

### Research Flags

Phases likely needing deeper research during planning (`/bm:plan-phase --research-phase <N>`):
- Phase 3 (Supporting libraries): Conflict 1 (kotlinx-datetime) is unresolved between researchers and materially changes this phase task list (mechanical-fix sub-commit vs. no-op bump) — plan-phase should either commission the empirical compile-check as a first planning step or explicitly schedule it as the phase first task.
- Phase 4 (Hot Reload + MCP wiring): Conflicts 2 and 3 (JBR requirement, exact task names / plain-JVM bundling) are unresolved and this phase task list (which task names to reference in `.mcp.json`, whether an explicit `hotRun` prerequisite step is needed) depends on the answers — the cheap empirical checks named above (`:showcase:tasks --all`, first `hotRun` invocation) should be the literal first two tasks of this phase plan, before any other Hot Reload work is scheduled.
- Phase 5 (Safety proof + sweep): this is inherently research-light (it is a hands-on protocol, not a documentation question) but the exact pass/fail protocol from FEATURES should be transcribed into the phase plan verbatim rather than re-derived.

Phases with standard, well-documented patterns (skip research-phase):
- Phase 1 (build-tool bump): Gradle wrapper bump, JDK toolchain change, and foojay-resolver addition are all standard, well-documented Gradle operations with no project-specific ambiguity.
- Phase 2 (Kotlin/CMP bump): a pure version-catalog edit, mirroring the exact pattern the project already executed successfully in v3.0 Phase 15.
- Phase 6 (release): directly reuses the v3.0-proven throwaway-tag-then-real-tag JitPack pattern; no new research needed, only execution.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Every version number confirmed against Maven Central `maven-metadata.xml` / `services.gradle.org`, dated 2026-09-21; the Hot Reload plain-JVM task-naming question is confirmed via a primary README read but contradicted by secondary doc-summary passes (Conflict 3) |
| Features | HIGH for mechanism/lifecycle/tool-list (read directly from JetBrains source at the exact pinned `v1.2.0` tag/commit); MEDIUM for forward-looking notes (later Hot Reload releases); explicit gap on current open-bug tracking (YouTrack migration, not scrapable in this session) |
| Architecture | MEDIUM-HIGH — file-level claims are HIGH (direct repo reads, grep-verified), external Gradle-9/Hot-Reload-MCP facts are MEDIUM-HIGH, a few items explicitly flagged UNVERIFIED (exact `ComposeHotRun` FQN, whether `hotMcpServer` itself respects the theme-forwarding block) |
| Pitfalls | HIGH where grounded in this repo actual source (grep-verified, file:line cited) or an official changelog/release-notes page fetched directly; MEDIUM where inferred from an official source; LOW/flagged inline for a few WebSearch-only items (`--no-daemon` cold-start/orphan-process risk, not independently reproduced) |

Overall confidence: MEDIUM-HIGH — the version-compatibility groundwork is solid and multiply-corroborated, but five concrete conflicts (above) and a handful of explicitly-flagged UNVERIFIED items mean this is not a just-execute research pass; several of the earliest planned tasks in Phases 3-4 are themselves the resolution mechanism for open questions, not merely execution of pre-settled facts.

### Gaps to Address

- Conflict 1 (kotlinx-datetime compile break vs. no-op): resolve by compiling `:library` against plain `kotlinx-datetime:0.8.0` alone, before committing to either task list for Phase 3. Do this first, not as a discovered surprise mid-phase.
- Conflict 2 (JBR Java-21-only vs. JBR-25-default): no source reconciles the two official statements; resolve via the first real `hotRun`/`hotMcpServer` invocation in Phase 4/5 and record the actual outcome as fact.
- Conflict 3 (exact Hot Reload task names / plain-JVM bundling): resolve via `./gradlew :showcase:tasks --all | grep -i hot` as literally the first action of Phase 4, before writing `.mcp.json`.
- Conflict 4 (Gradle 9.7.1 vs. Kotlin 2.4.20 9.7.0 ceiling): low-risk warning zone, not a blocker — treat a real `./gradlew build` in Phase 1 as the actual gate, do not just trust the docs.
- Conflict 5 (screenshot capture mechanism): settled in favor of FEATURES direct source read (`Robot.createScreenCapture`) — but the calibration-pass recommendation from PITFALLS should still be executed in Phase 5, for the narrower reason that two different screen-capture APIs (Win32 `CopyFromScreen` vs. AWT `Robot`) can still differ in DPI/chrome/color handling even though both are real screen scrapes.
- YouTrack current-bug gap (FEATURES section 7): could not be enumerated in this research session (client-rendered page, not scrapable) — if Phase 4/5 hits an unexplained Hot Reload MCP behavior, check YouTrack project CMP group "Hot Reload" manually before assuming it is project-specific.
- `ComposeHotRun` exact FQN (ARCHITECTURE, flagged UNVERIFIED): confirm via IDE autocomplete or `gradlew :showcase:help --task hotRun` once the plugin is applied, before writing the forwarding block import statement.
- DPI scaling (Pitfall 7): carried-over gap from v3.0 (125%/200% passes waived by the maintainer then), not newly introduced — this milestone own verification report must name the DPI scale it actually ran at, so the gap stays explicitly acknowledged rather than silently absorbed into a scale-unspecified no-drift-found verdict.

## Sources

### Primary (HIGH confidence)
- Maven Central `maven-metadata.xml` (direct queries, 2026-09-21 snapshot): `kotlin-gradle-plugin`, `org.jetbrains.compose.gradle.plugin`, `org.jetbrains.compose.material3:material3`, `kotlinx-coroutines-core`, `kotlinx-datetime`, `junit-bom`/`junit-jupiter`/`junit-platform-launcher`, `hot-reload-core`, `foojay-resolver-convention`
- `services.gradle.org/versions/current` and `/versions/all` — Gradle 9.7.1 current stable confirmation
- `gh release view v1.2.0 --repo JetBrains/compose-hot-reload` — primary release notes (JBR 25 default, MCP tool list)
- `https://github.com/JetBrains/compose-hot-reload`, tag `v1.2.0`, commit `3315f8dd8f5cc4db1359ff13b2be178ba19486e3` — full source read of `McpServer.kt`, `screenshotHandler.kt`, `semanticTreeHandler.kt`, `uiActionHandler.kt`, `window.kt`, `windowResizeHandler.kt`, `GradleRecompiler.kt`, `ComposeHotRunTasksTest.kt`, `ComposeHotMcpServerTasksTest.kt`, and JetBrains own integration tests
- `https://raw.githubusercontent.com/JetBrains/compose-jb/master/CHANGELOG.md` — CMP 1.12.0 bundled-versions section
- `https://raw.githubusercontent.com/Kotlin/kotlinx-datetime/master/CHANGELOG.md` — 0.7.0/0.7.1/0.8.0 breaking-change history (central to Conflict 1)
- `https://kotlinlang.org/docs/whatsnew2420.html`, `/docs/multiplatform/compose-compatibility-and-versioning.html`, `/docs/multiplatform/compose-hot-reload.html` — official Kotlin/CMP docs
- `https://docs.gradle.org/current/userguide/compatibility.html` and Gradle 9.0.0 upgrade guide — JDK-Gradle matrix, breaking-change enumeration
- Direct repository reads (this project): `PROJECT.md`, `gradle/libs.versions.toml`, `build.gradle.kts`, `library/build.gradle.kts`, `showcase/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `jitpack.yml`, `README.md`, `gradle/wrapper/gradle-wrapper.properties`, `Main.kt`, plus grep of `library/src`/`showcase/src` for `kotlinx.datetime.*` symbols

### Secondary (MEDIUM confidence)
- `https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/` — CMP 1.12.0 announcement, version pairing
- JetBrains/compose-hot-reload README (fetched via raw.githubusercontent.com) — plugin ID, task naming, `.mcp.json` shape (underlies Conflict 3)
- `docs.jitpack.io` / JitPack FAQ — JDK config, sticky-failed-build behavior
- `api.sdkman.io` live query — current Temurin 21 identifier (moving target, re-verify at implementation time)
- `gradle/gradle` GitHub issues #1845, #5213 — stdout noise not fully suppressed by `--quiet --console=plain`
- `gradle/foojay-toolchains` issue tracker, `facebook/react-native` #56287/#55781 — pre-1.0.0 resolver breakage on Gradle 9
- JUnit 6.0.0 release notes / migration wiki — coordinate/version-unification confirmation

### Tertiary (LOW confidence, flagged inline in source docs)
- `aoreshkov/kmp-ledger` PR #27 — third-party corroboration of the Windows `./gradlew` spawn failure and the attach-to-running-app prerequisite (Pitfall 4); not an official source but directly reproduces a real-world failure matching this milestone exact setup
- `--no-daemon` cold-start latency / orphaned-process risk (Pitfall 5) — reasoned from documented `--no-daemon` semantics and Windows process-tree behavior, not an independently reproduced incident
- InfoWorld coverage of CMP 1.12.0 MCP server announcement — general-press corroboration only
- GitHub issue #70 (old, likely-stale Windows DevTools crash) — low relevance, noted only for an incidental Command Prompt window observation

---
*Research completed: 2026-09-21*
*Ready for roadmap: yes, with five conflicts flagged for empirical resolution during Phases 3-5 rather than pre-decided here*
