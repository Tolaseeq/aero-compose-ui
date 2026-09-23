# Phase 21: Migration + Release 3.1.0 - Research

**Researched:** 2026-09-23
**Domain:** Forced toolchain/dependency refresh (Gradle/JDK/Kotlin/CMP/coroutines/kotlinx-datetime/JUnit) + first-time Compose Hot Reload MCP install, on a shipped Compose Desktop (JVM) library published via JitPack
**Confidence:** MEDIUM-HIGH — every touched file read directly at current state; version-compatibility facts inherited from milestone-level research (SUMMARY/STACK/PITFALLS/ARCHITECTURE/FEATURES, dated 2026-09-21) and the maintainer-verified MCP-HOWTO.md (2026-09-23); five milestone-level conflicts remain open and are carried forward, not resolved here — they resolve empirically during execution per the roadmap's own plan

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Carried forward (locked before this discussion — do not re-litigate):**
- Execution order inside the phase is the 7-step order in ROADMAP.md § Phase 21; every version bump is its own commit gated by a full test run before the next.
- Global stop rule: if any upgrade or the JBR setup needs more than a version bump plus mechanical renames forced by the new API, or the MCP moves the real cursor / steals input focus — stop and ask. No workarounds.
- Window capture is `PrintWindow(hwnd, hdc, 2)` only. The MCP server's `take_screenshot` is banned (it is a `java.awt.Robot` screen scrape and has already captured the maintainer's private browser).
- Hover / press / keyboard focus / drag are verified by Compose UI tests only; no system input (Windows messages, `java.awt.Robot`) is ever delivered to a live window.
- Pixel comparison is a tool for the agent's own analysis, never an automatic pass/fail gate.
- `.planning/research/MCP-HOWTO.md` (maintainer-verified) wins over the desk research wherever they disagree; JBR 21 is already installed, no foojay resolver.
- The test-count guard (TOOL-16) must be proven red on a deliberately excluded test class first.

**Frame storage:**
- **D-01:** No image goes into git. Every image artifact (BASE-04 baseline, BASE-05 reference images, D-07 opened-state images, post-upgrade captures, diff images, D-06 hand-off page) lives in `.captures/` at the repo root (`C:\1A_WORK\ui_lib\.captures\`), gitignored. Outside `build/`, so `gradlew clean` does not wipe it. Consequence accepted: folder absent from other worktrees; deletion loses the baseline permanently.
- **D-02:** What IS committed: the capture helper (BASE-02), the launch-parameter code (BASE-01), all UI-test code (BASE-05 + D-07), and the TEXT of every report (noise-region list, explained-differences list, unconfirmed list, toolchain per run) in the phase directory. Text reports reference frames by `.captures/`-relative path.
- **D-03:** BASE-05/D-07 UI tests are permanent suite members (count toward TOOL-16), so ordinary `gradlew test` must not write into `.captures/` or dirty the working tree. Writing reference/after images happens only on explicit opt-in (Gradle or system property — Claude's discretion).

**Drift found after the upgrade:**
- **D-04:** One stop, with the complete list. Agent finishes the entire post-upgrade sweep first (VER-07 all sections×themes, VER-08 all BASE-05 states, D-07 all opened states), then stops ONCE with every difference outside BASE-03 noise regions: before/after pair, named cause, proposed action. Agent never changes drawing/rendering code on its own to compensate for drift — that is "more than a version bump," a stop-rule violation. Fixes land only after maintainer rules on the list; affected frames re-captured and re-compared.
- **D-05:** Threshold = everything outside named noise regions. Pixel-only differences (e.g. antialiasing shift after a Skia bump) are NOT filtered — they go on the stop list too. Grouping allowed for readability; nothing dropped or silently reclassified. Noise list established on OLD toolchain (BASE-03) before any bump; any region added post-upgrade must be shown to vary run-to-run on the NEW toolchain, and is itself called out on the stop list.
- **D-06:** Hand-off format = local HTML page in `.captures/` (not committed): before/after side by side per section×theme and per UI-test state, differences highlighted, both lists at the bottom. Must work offline (relative image paths). Agent opens with `start "" "C:\1A_WORK\ui_lib\.captures\...\index.html"`. Same format for the D-04 drift stop. List text also committed (D-02). VER-10 still holds: page reaches maintainer only after the agent's own sweep completes.

**Opened popup states:**
- **D-07:** Opened states covered by UI tests with a before-baseline, same mechanism as BASE-05 (`runComposeUiTest` + click/key input → `captureToImage`), in all three themes, captured on the OLD toolchain (Kotlin 2.4.10/CMP 1.11.1) before any bump, compared after. Coverage: every component whose source contains `Popup(` — `AeroDropdown` (via `AeroDropdownPopup`), `AeroComboBox`, `AeroContextMenu`, `AeroMenuBar`, `AeroTooltip`, `AeroPopover`, `AeroDrawer`, `AeroColorPickerButton`, `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`, `AeroDateTimeRangePicker`, `AeroTimePicker`. `AeroDialog`/`AeroAlertDialog` (built on `Dialog`) follow the same rule where the test can capture them. These tests are added BEFORE the TOOL-16 count is locked — part of the locked number.
- **D-08:** Pre-approved per-component fallback: if a UI test cannot capture a component's popup layer (renders as a separate root/window `captureToImage` doesn't reach), for THAT component only the agent switches to after-only inspection — open via MCP `click`, capture with `PrintWindow` — marked "inspected without baseline." Not a stop. The BASE-05 stop still applies if `captureToImage` doesn't work in desktop `runComposeUiTest` at all. `AeroFilePicker` opens the native OS dialog (not this library's rendering) — goes on the unconfirmed list.
- **D-09:** Open-state frames must be deterministic across days: pickers given a fixed `value`. With `value == null` they open on the current month via a private `todayLocalDate()` (`Clock.System.now()` — see exact file:line table below), so before/after frames on different days would differ. `AeroCalendarGrid` has no today-highlight (verified below) — a fixed value is sufficient.

**JitPack verify tag:**
- **D-10:** Agent pushes throwaway verify tags without asking: `v3.1.0-verify01`, then `-verify02`, … after each fix (failed JitPack builds stick to their tag forever, so a name is never reused). Authorization covers ONLY pushing verify tags; moving `master`, setting `3.1.0` in `build.gradle.kts`, and pushing `v3.1.0` stay behind maintainer confirmation at execution time (ROADMAP success criterion 5).
- **D-11:** Verify tags stay on GitHub after `3.1.0` ships — same as v3.0. Nothing deleted.

### Claude's Discretion
- Internal layout and file naming of `.captures/` — must record toolchain (old/new), theme, and section or component/state of each frame.
- BASE-01 mechanism: e.g. `-Paero.section=<Name>` forwarded like the existing `-Paero.scheme` (`showcase/build.gradle.kts` `tasks.withType<JavaExec>` block + `initialScheme()` in `Main.kt`), and a sibling forwarding block for Hot Reload's non-`JavaExec` run task. Whether it renders only the chosen section or scrolls to it is open, as long as the section is fully visible and before/after frames are produced identically. Sections taller than the window may use a taller window or several frames — identically before and after.
- BASE-03 noise handling (measure-and-list vs. a capture mode freezing infinite animations). Known sources: `AeroProgressBar`'s two `rememberInfiniteTransition`s (`runningSheen`, `indeterminate`), the text caret, anything time-dependent.
- TOOL-16 count-guard mechanism.
- Diff/highlight technique on the D-06 page and how differences are grouped by cause (within D-05).
- Showcase `testTag`s added for HRM-02.

### Deferred Ideas (OUT OF SCOPE)
None raised in discussion — stayed within phase scope. New components, public API changes, visual changes beyond fixing D-04 drift, v3.0 debt, an external scratch consumer as a release gate, 125%/200% DPI passes (VER-09 unconfirmed list) are all explicitly out of this phase.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BASE-01 | Showcase launches on chosen theme+section via launch parameter, section fully visible | See §1 (Main.kt/ShowcaseApp.kt current state), §2 (BASE-01 design) |
| BASE-02 | `PrintWindow(hwnd, hdc, 2)` capture helper: occluded, minimized (`ShowWindow(h,4)`), z-order (`SetWindowPos` BOTTOM), pixel sanity check, no cursor/focus | See §2 (capture helper design, PowerShell + C# Add-Type) |
| BASE-03 | Repeat capture of same state gives same frame, or noise regions measured+listed | See §3 (noise sources enumerated with file:line) |
| BASE-04 | Reference frames all sections × 3 themes, old toolchain, outside `build/` | See §2 (BASE-01 mechanism), §9 (`.captures/` layout) |
| BASE-05 | Compose UI tests capture hover/press/focus/drag to images; `captureToImage` proof-of-work first | See §4 (UI-test target enumeration, Popup-root capture risk) |
| TOOL-09 | Gradle 9.7.1, stale JetBrains Space repo removed, `--refresh-dependencies` clean build | See §1 (settings.gradle.kts current state), §6 |
| TOOL-10 | JDK 21 everywhere, class-file 65, `org.gradle.jvm.version=21` in module metadata | See §6, §8 |
| TOOL-11 | Kotlin 2.4.20 + CMP 1.12.0, Gradle-9.7.1-vs-9.7.0-ceiling warning tolerated | See §6 (Conflict 4 carried forward) |
| TOOL-12 | Material3 stays 1.9.0, `dependencyInsight` clean on both modules incl. after Hot Reload | See §6, §7 (Material3 leak via devtools) |
| TOOL-13 | kotlinx-coroutines 1.11.0 | See §6 |
| TOOL-14 | kotlinx-datetime 0.8.0 plain; compile decides picker source changes | See §5 (exact file:line list, more than milestone research found) |
| TOOL-15 | JUnit 6.1.3, jupiter+platform launcher aligned | See §6 |
| TOOL-16 | Exact test count re-measured (not copied from docs), guard proven red first | See §5 (current @Test count measured: 474, not 467) |
| TOOL-17 | Showcase builds and runs on new toolchain | See §6 |
| HRM-01 | Hot Reload 1.2.0 + `.mcp.json`, `:showcase` only, one commit | See §7 |
| HRM-02 | Post-restart MCP connects, sees showcase via `hotRun`; testTags where needed | See §7 (zero existing testTags found) |
| HRM-03 | Agent self-measures cursor/foreground-window unchanged, incl. occluded/minimized | See §7 (FEATURES.md protocol, maintainer-verified) |
| VER-07 | Post-upgrade sections × 3 themes vs. BASE-04, explained or fixed | See §8, §9 |
| VER-08 | Post-upgrade BASE-05 UI tests vs. baseline images | See §8 |
| VER-09 | Everything not confirmed listed separately as "unconfirmed" | See §8 (known unconfirmed categories) |
| VER-10 | Maintainer sees GUI only after agent's own sweep | See §9 (D-06 hand-off page) |
| REL-03 | Throwaway verify tag green on JitPack, JDK 21/Gradle 9.7.1, before real tag | See §8 (v3.0 precedent) |
| REL-04 | README states new consumer floor, fixes stale toolchain line | See §1 (README.md:6 current state) |
| REL-05 | `3.1.0` in `build.gradle.kts` before tag; `v3.1.0` pushed, JitPack `ok`, artifact resolves | See §8 |
</phase_requirements>

## Summary

Every file this phase touches was read at its current state (§1) — the gap between milestone-level desk research and this phase's actual codebase is small but real: the kotlinx-datetime 0.8.0 rename touches **10 files**, not the 4+4 the milestone research counted (§5), and the current test suite has **474 `@Test` annotations**, not the 467 carried in STATE.md from v3.0's closeout (§5) — TOOL-16 must re-measure via `./gradlew test`, never copy either number. No showcase `testTag`s exist anywhere today (grep confirmed zero matches) — HRM-02's "add testTags where ambiguous" is not optional polish, it starts from nothing. The `-Paero.scheme` forwarding pattern BASE-01 must extend is a lazy `tasks.withType<JavaExec>().configureEach { if (name == "run") ... }` block in `showcase/build.gradle.kts:27-31`; Hot Reload's run task is a different, non-`JavaExec` type (`ComposeHotRun`), so this needs a sibling block, not an edit to the existing one.

The Popup-capture question for D-07/D-08 has a documented, source-confirmed answer already in FEATURES.md (not re-derived here): on Compose Desktop, `Popup`/`Dialog`/`ModalBottomSheet` render as additional semantics "owners" **within the same window's Skia surface**, not separate OS windows — `get_semantic_tree`'s `findAllRootSemanticsNodes()` + `joinSemanticForest()` already handles multi-root correctly by design, tested by JetBrains at exactly this version. The open question this phase must answer empirically (not yet confirmed for the *desktop* `runComposeUiTest` harness specifically, only for the live MCP runtime) is whether `onRoot().captureToImage()` in a JVM unit test sees only the primary root or all open owners — the project's own `AeroThemeBackgroundEstablishmentTest.kt` already establishes `onRoot()` captures the full window canvas, but no existing test in this codebase opens a Popup and then calls `captureToImage()`, so this is genuinely untested territory, not a solved problem — see §4.

Five version-compatibility conflicts remain open from milestone-level research (kotlinx-datetime compile-break-vs-noop — now resolved by direct grep below in favor of "compile break, and worse than counted"; JBR Java-21-vs-25; exact Hot Reload task names for plain `kotlin.jvm`; Gradle-9.7.1-vs-Kotlin's-9.7.0-ceiling; screenshot mechanism — already settled, moot since `take_screenshot` is banned). None of these block planning; each has a named, cheap, first-step empirical check that should be literally the first task of its corresponding plan, not deferred discovery.

**Primary recommendation:** Plan this phase as the roadmap's fixed 7 steps, with the kotlinx-datetime mechanical-fix sub-step (step 5) scoped to the corrected 10-file list below, the TOOL-16 count re-measured live (not copied from any document, including this one), and BASE-01's forwarding block built as an explicit second `tasks.withType<ComposeHotRun>()` block from day one (step 4) rather than discovered as a gap after Hot Reload lands.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Toolchain/dependency version bumps | Build config (Gradle) | — | `gradle/libs.versions.toml` is the single propagation point; no application-tier code involved |
| Section/theme launch parameter (BASE-01) | Desktop app entry point (`:showcase` `Main.kt`) | Build config (`showcase/build.gradle.kts` forwarding block) | `initialScheme()`-style pattern reads a JVM system property set by Gradle; app-tier reads, build-tier forwards |
| Window capture helper (BASE-02) | External tooling (PowerShell/WinAPI, outside the Gradle build) | — | Operates on the OS window handle from outside the JVM process; not part of the published library or showcase artifact |
| UI-test hover/press/focus/drag capture (BASE-05, D-07) | Test tier (`library/src/test`, JVM `runComposeUiTest`) | — | In-process Compose semantics + Skia capture; no live window, no OS input |
| Hot Reload + MCP server | Desktop app entry point (`:showcase` only) | Build config (Gradle plugin application) | Structurally isolated by Gradle's per-subproject `plugins{}` graph; `:library`'s publish pipeline has no code path to it |
| Test-count guard (TOOL-16) | Build config (Gradle test task) | — | A Gradle `Test` task listener/verification task, not application code |
| JitPack release (REL-03/05) | External service (CI) | Build config (`jitpack.yml`, root `build.gradle.kts` version) | JitPack is an external build service triggered by git tags; `jitpack.yml` configures its JDK |

## Standard Stack

### Core

| Library | Current Version | Target Version | Purpose | Why Standard |
|---------|---------|---------|---------|--------------|
| Kotlin | `2.4.10` | `2.4.20` [CITED: kotlinlang.org/docs/whatsnew2420.html] | Language/compiler | Latest stable; milestone-decided floor |
| Compose Multiplatform | `1.11.1` | `1.12.0` [CITED: blog.jetbrains.com CMP 1.12.0 announcement] | UI framework | Latest stable desktop-target release; bundles/targets Hot Reload 1.2.0 |
| Gradle | `8.14.3` (wrapper) | `9.7.1` [CITED: services.gradle.org/versions/current] | Build tool | Current stable |
| JDK toolchain | `17` (both modules) | `21` | Compile/runtime target | Milestone-decided floor; JBR 21 already installed on this machine `C:\Users\1\.jdks\jbr-21.0.9` [VERIFIED: MCP-HOWTO.md, checked 2026-09-23] |
| kotlinx-coroutines | `1.10.2` | `1.11.0` | Internal-only (`implementation`) | No public API leak; low risk |
| kotlinx-datetime | `0.6.2` | `0.8.0` plain (no `-0.6.x-compat`) | Picker date math, `api()`-scoped | Milestone-decided clean floor |
| JUnit | `5.10.0` (jupiter) | `6.1.3` | Test runner | Coordinates unchanged from JUnit 5; unified Platform/Jupiter versioning |
| Compose Hot Reload | none | `1.2.0` [CITED: MCP-HOWTO.md, JetBrains README] | `:showcase`-only dev tool | Required for MCP server; ≥1.12.0 CMP is a hard prerequisite (1.11.1 leaves `list_windows` empty per maintainer's own on-machine check) |

**Package-name provenance:** all coordinates above (`org.jetbrains.compose.hot-reload`, `org.jetbrains.kotlinx:kotlinx-datetime`, etc.) were carried from the milestone-level STACK.md, itself sourced against Maven Central `maven-metadata.xml` directly — tag `[CITED]` rather than `[VERIFIED]` per this agent's own provenance rule, since this research session did not re-run `npm view`-equivalent registry queries itself. No new packages are introduced beyond what STACK.md already audited; nothing here is `[ASSUMED]` from an unverified source.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| foojay-resolver-convention for JBR provisioning | Manual JBR install | Not needed — JBR 21 already installed and Gradle-visible on this machine [VERIFIED: MCP-HOWTO.md]; adding foojay anyway is unnecessary complexity the milestone research recommended but the maintainer's own machine check overrides |
| `-0.6.x-compat` kotlinx-datetime variant | Plain `0.8.0` | Compat variant avoids the rename breaks but defeats the "forced refresh" goal — milestone explicitly rejects it |

**Installation:** version bumps happen entirely inside `gradle/libs.versions.toml` (single source of truth, consumed via `libs.*` aliases already in both `library/build.gradle.kts` and `showcase/build.gradle.kts`) — no new `npm install`/`pip install` equivalent; this is a Gradle version-catalog edit.

**Version verification:** every version above was cross-checked against Maven Central / services.gradle.org in the milestone-level STACK.md (dated 2026-09-21, two days before this research) — re-verification at execution time is still recommended since two days have passed; the phase's own step 3 build (`./gradlew build`) is the actual gate, not the docs (per Conflict 4 below).

## Package Legitimacy Audit

No new external packages are introduced by this phase beyond version bumps of already-present, already-audited dependencies (Kotlin, CMP, Gradle, coroutines, kotlinx-datetime, JUnit) plus one new first-party JetBrains plugin (`org.jetbrains.compose.hot-reload`, published by the same JetBrains org that publishes Compose Multiplatform itself, already depended on transitively). None of these are third-party/unknown-provenance packages requiring the slopcheck protocol — all are official JetBrains/Kotlin Foundation artifacts already present in this project's dependency graph or a natural extension of it (Hot Reload is JetBrains' own plugin, same publisher as `org.jetbrains.compose`). Package Legitimacy Audit is **not applicable** to this phase; skipping per the protocol's own scope (only required for external/third-party package installs).

## Current State of Every Touched File (Codebase-Grounded)

### `gradle/libs.versions.toml` (full contents, current)
```toml
[versions]
kotlin = "2.4.10"
composeMultiplatform = "1.11.1"
kotlinxCoroutines = "1.10.2"
junit = "5.10.0"
kotlinxDatetime = "0.6.2"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```
No `composeHotReload` entry exists yet — must be added (`[versions]` + `[plugins]`).

### `gradle/wrapper/gradle-wrapper.properties:3`
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

### `settings.gradle.kts` (full contents, current)
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "aero-compose-ui"
include(":library", ":showcase")
```
The stale JetBrains Space repo (TOOL-09) appears **twice** — once in `pluginManagement`, once in `dependencyResolutionManagement` — both lines need removal. No `foojay-resolver-convention` present; per MCP-HOWTO.md this is **not needed** (JBR 21 already Gradle-visible), overriding the milestone STACK/ARCHITECTURE recommendation to add it.

### `build.gradle.kts` (root, full contents, current)
```kotlin
// Root build — no plugins applied here. Per-module build files own their plugins.
allprojects {
    group = "com.mordred"
    version = "3.0.0"
}
```
REL-05's version bump is a one-line edit: `"3.0.0"` → `"3.1.0"`, must land before the `v3.1.0` tag (locked "bump version on milestone" rule).

### `library/build.gradle.kts` (relevant lines, current)
- Line 9: `jvmToolchain(17)` → `21`
- Line 22: `api("org.jetbrains.compose.material3:material3:1.9.0")` — stays unchanged, verify via `dependencyInsight` post-bump
- Line 27: `api(libs.kotlinx.datetime)` — `api`-scoped, so the 0.8.0 floor propagates transitively to every JitPack consumer (REL-04 must document this)
- Line 29: `implementation(libs.kotlinx.coroutines.core)` — internal only
- Lines 31-41: test dependencies (`kotlin.test`, `junit.jupiter`, `kotlinx.coroutines.test`, `kotlin("reflect")`, unversioned `junit-platform-launcher`, `compose.uiTest`, `compose.desktop.currentOs`)
- **No Hot Reload plugin here — must never be added** (HRM-01's isolation requirement)

### `showcase/build.gradle.kts` (full contents, current)
```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":library"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(libs.kotlinx.datetime)
}

// Forward -Paero.scheme=<name> to the run task as a system property, so a review pass can open a
// specific theme directly (see initialScheme() in Main.kt) instead of clicking the theme switcher.
// withType(...).configureEach is lazy: the Compose Desktop plugin registers `run` after this
// script is evaluated, so tasks.named("run") would fail with "Task with name 'run' not found".
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
}

compose.desktop {
    application {
        mainClass = "com.mordred.showcase.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Deb)
            packageName = "aero-compose-ui-showcase"
            packageVersion = "0.1.0"
        }
    }
}
```
This is the exact block BASE-01's section parameter and HRM-01's Hot Reload plugin both touch:
- `jvmToolchain(17)` → `21`
- Add `alias(libs.plugins.compose.hot.reload)` to `plugins{}` (HRM-01)
- The `tasks.withType<JavaExec>().configureEach { if (name == "run") ... }` block **only fires for the plain `run` task** (a `JavaExec`) — Hot Reload's run task is a different, non-`JavaExec` Gradle task type (`ComposeHotRun`, exact FQN unverified — confirm via `gradlew :showcase:help --task hotRun` once the plugin lands). A **sibling block** is required for both `-Paero.scheme` (existing) and a new `-Paero.section` (BASE-01) to reach the Hot-Reload-launched instance:
  ```kotlin
  tasks.withType<org.jetbrains.compose.reload.ComposeHotRun>().configureEach {
      (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
      (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
  }
  ```
  Confirm the exact FQN before writing this (import resolution or IDE autocomplete once the plugin is applied). If it doesn't reach `hotRun`/`hotMcpServer` in practice, the documented fallback is driving the theme/section switcher via MCP `click` instead (already anticipated in CONTEXT.md's Claude's Discretion).

### `jitpack.yml` (full contents, current)
```yaml
jdk:
  - openjdk17
before_install:
  - sdk install java 17.0.10-tem
  - sdk use java 17.0.10-tem
```
REL-03/TOOL-10: needs `openjdk21` + a current Temurin 21 SDKMAN identifier (re-verify exact patch string at execution time — moving target per STACK.md).

### `README.md:6` (current, stale)
```
> Package: `com.mordred.aero` · Kotlin `2.1.21` · Compose Multiplatform `1.7.3` · JVM 17
```
Already stale since v3.0 shipped Kotlin 2.4.10/CMP 1.11.1/JDK 17 — never updated. REL-04 fixes this line AND adds the new v3.1 floor statement. No `v3.0.0` dependency-snippet line or toolchain-requirement callout block was found at the previously-cited `README.md:73`/`:80-82` line numbers in this current read — **the README's actual current length/structure was only spot-checked (first 40 lines)**; before writing the plan, re-read the full file to find the actual dependency-coordinate snippet and any existing toolchain-requirement callout to update in place, per Pitfall 9's "immediately adjacent" placement rule. [Flag: full README not read in this research pass — plan should do a targeted grep for the `implementation("com.github` snippet before editing.]

### `.gitignore` (full contents, current)
```
.gradle/
.kotlin/
build/
*/build/
.idea/
*.iml
local.properties
.DS_Store
out/
tools/valkyrie-cli-1.1.1/
tools/valkyrie-cli-1.1.1.zip
```
No `.captures/` entry — D-01 requires adding it.

### `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` (full contents, current)
Already shown in full above (§ code_context in CONTEXT.md matches). Key facts confirmed by direct read:
- `initialScheme()` (lines 29-33) reads `System.getProperty("aero.scheme")`, defaults to `AeroColorScheme.AeroBlue` on unrecognized/absent — exactly the pattern BASE-01's section parameter should mirror.
- The `Window` is `undecorated = true, transparent = false` (lines 43-44) — the locked Win11 EXCEPTION_ACCESS_VIOLATION rule, unrelated to this phase but a hard constraint on anything touching the window setup.
- `windowState = rememberWindowState(width = 1200.dp, height = 800.dp)` (line 36) — fixed initial size; BASE-01's "section fully visible" requirement may need this widened/heightened for tall sections, per CONTEXT.md's Claude's Discretion ("taller window or several frames").

### `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` (full contents, current)
One long `verticalScroll` `Column` (lines 72-123) containing, in order: `ThemeSwitcher`, `VerificationSection`, `Foundation` (heading + `FoundationSection`), `Primitives` (heading + `PrimitivesSection`), `IconsSection`, `ButtonsSection`, `InputSection`, `SelectionSection`, `DropdownSection`, `RangeSection`, `ListSection`, `ContainersSection`, `OverlaysSection`, `NavigationSection`, `DataSection`, `PickersSection`, `LayoutSection` — **17 sections**, not the 16 the milestone-level MCP-HOWTO.md's facts-checked section counted (it said "16 sections") — recount confirms 17 distinct section composables (2 of which — Foundation, Primitives — are wrapped in an inline heading `Column` rather than being their own named `*Section` composable with its own heading, which may be why the count differs; treat both readings as consistent, just count by different units). BASE-01's section parameter needs a stable identifier per section — natural choices are the composable function names (`FoundationSection`, `PrimitivesSection`, `IconsSection`, ... `LayoutSection`) since `Foundation`/`Primitives` don't currently have section-level wrapper composables of their own (they're inlined into `ShowcaseApp` directly with just a `Text` heading + the section call) — Claude's Discretion decision needed: either treat "Foundation"/"Primitives" as addressable section names too (requiring a small refactor to extract them into their own named composables, mirroring the other 15), or scope BASE-01's addressable-section list to the 15 composables that are already standalone `*Section()` calls and handle Foundation/Primitives as always-visible (they're near the top of the scroll anyway). This is a real, concrete open point for the plan to resolve, not decided here.

## Architecture Patterns

### System Architecture Diagram

```
[Maintainer / Claude Code session]
        │
        │ registers (session start only)
        ▼
.mcp.json ──spawns──▶ cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer
                              │
                              │ stdio JSON-RPC (must stay clean — no wrapper-download noise)
                              ▼
                    Gradle 9.7.1 process (JDK 21)
                              │
                              │ watches pidFile, connects once app registers
                              ▼
              ┌───────────────────────────────────────┐
              │ :showcase JVM (JBR 21, hotRun-launched) │
              │  Main.kt → initialScheme()/section       │
              │  reads -Daero.scheme / -Daero.section    │──▶ AeroTheme(scheme) { ShowcaseApp }
              │  AeroTitleBar + ShowcaseApp (17 sections,│      one long verticalScroll Column
              │  each Popup-based dropdown/picker renders│
              │  as an extra semantics "owner" in the    │
              │  SAME window, not a separate OS window)  │
              └───────────────────────────────────────┘
                              │
        click/type_text/scroll/get_semantic_tree   take_screenshot (BANNED — Robot scrape)
        (in-process semantics, no OS input,               │
         works occluded, fails minimized)                  ✗ never called by this project
                              │
                              ▼
              External capture path (independent of MCP):
              PowerShell + WinAPI P/Invoke
              FindWindow/EnumWindows(by title) → PrintWindow(hwnd, hdc, 2)
              → sanity-check 2 px vs. app background → save PNG to .captures/
              (works on ANY toolchain — old 1.11.1 baseline AND new 1.12.0 after-captures
               use the SAME pipeline, per MCP-HOWTO.md's key consequence)
                              │
                              ▼
              .captures/ (gitignored, outside build/)
              ── committed instead: capture helper code, launch-param code,
                 UI-test code, and TEXT reports (noise list, explained-diffs,
                 unconfirmed list) in the phase directory, referencing
                 .captures/-relative paths (D-02)
```

### Recommended `.captures/` Structure (Claude's Discretion — proposed)
```
.captures/
├── baseline/                      # old toolchain (Kotlin 2.4.10 / CMP 1.11.1)
│   ├── sections/                  # BASE-04: <section>-<theme>.png
│   ├── ui-tests/                  # BASE-05 reference images (opt-in write, D-03)
│   └── popups/                    # D-07 opened-state baseline images
├── after/                         # new toolchain (Kotlin 2.4.20 / CMP 1.12.0)
│   ├── sections/
│   ├── ui-tests/
│   └── popups/
├── noise/                         # BASE-03 noise-region captures (repeat-capture proof)
└── handoff/                       # D-06 HTML page + its own image copies/symlinked refs
    └── index.html
```
Each filename should encode: toolchain (old/new), theme, section-or-component, state — e.g. `AeroButton-AeroBlue-old-hover.png`. Exact naming is Claude's Discretion per CONTEXT.md, but must satisfy "records toolchain, theme, and section/component/state."

### Pattern: `-Paero.scheme` / `-Paero.section` forwarding (BASE-01 — extend existing)
**What:** Gradle-property → JVM system-property forwarding, read by a private `initialSceneXxx()`-style function in `Main.kt`.
**When to use:** Any launch-time parameter that must reach the showcase JVM identically whether launched via `run`, `hotRun`, or `hotMcpServer`.
**Example (current, verified):**
```kotlin
// showcase/build.gradle.kts:27-31 (existing, unchanged)
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
}
```
```kotlin
// Main.kt:29-33 (existing pattern to mirror for aero.section)
private fun initialScheme(): AeroColorScheme = when (System.getProperty("aero.scheme")) {
    "AeroDark" -> AeroColorScheme.AeroDark
    "Classic" -> AeroColorScheme.Classic
    else -> AeroColorScheme.AeroBlue
}
```
**New sibling block needed for Hot Reload (per ARCHITECTURE.md, unverified exact FQN):**
```kotlin
tasks.withType<org.jetbrains.compose.reload.ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
}
```

### Anti-Patterns to Avoid
- **Assuming the existing `JavaExec` forwarding block reaches `hotRun`/`hotMcpServer`:** it doesn't — `ComposeHotRun` is a different task type. Confirmed reasoning (not yet confirmed exact FQN) in ARCHITECTURE.md.
- **Trusting `./gradlew test` exit code 0 as proof of the locked test count:** must read/assert the literal printed count (Pitfall 11) — this project's own count is provably NOT the 467 carried in STATE.md (see §5).
- **Building a coordinate-click fallback for `click`/`type_text`:** no such tool exists by design in Hot Reload 1.2.0; a missing semantics wiring is a library gap, not something to route around with pixel clicks (which would also violate the no-real-cursor-input decision).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Window screenshot capture | Custom `CopyFromScreen` port or a new Robot-based capture | `PrintWindow(hwnd, hdc, 2)` via PowerShell P/Invoke (`Add-Type` C#), per MCP-HOWTO.md/D-02 | Only `PrintWindow` captures occluded content correctly; `CopyFromScreen`/Robot both scrape the screen region and fail when covered |
| Pixel diffing for the D-06 hand-off page | A new diff library/dependency | Reuse the project's own `pixelMapsDiffer(a: PixelMap, b: PixelMap)` pattern (`VER07ButtonStateMatrixTest.kt:265-273`) for UI-test image comparison, and the v3.0-proven `Bitmap.LockBits` byte-compare (PowerShell) for the PrintWindow-captured PNGs (`reference_windows_mcp_showcase_capture.md`) | Both patterns are already proven in this exact codebase; no new dependency needed |
| Test-count guard | A hand-rolled JUnit extension | A Gradle `Test` task `afterSuite` listener (`tasks.test { afterSuite { desc, result -> ... } }`) asserting `result.testCount` against a recorded value, OR parse the printed HTML/XML test report | Gradle's own `Test` task already exposes this via `TestListener`/`afterSuite` — no need for a custom JUnit runner extension |
| JitPack verify-tag discipline | New process | Reuse the exact v3.0 pattern (`v3.0.0-alpha01`, `-verify01`, `-verify02`, all still on GitHub per D-11) | Already proven working for this exact project/JitPack combination |

**Key insight:** every mechanism this phase needs (capture, diff, test-count assertion, release verification) already has a proven precedent either in this exact codebase (`pixelMapsDiffer`, `-Paero.scheme` forwarding, v3.0's throwaway-tag discipline) or in the maintainer's own first-hand MCP-HOWTO.md — this phase is assembly and extension of existing patterns, not new-pattern invention.

## Common Pitfalls

### Pitfall 1: kotlinx-datetime rename touches MORE files than the milestone research counted
**What goes wrong:** STACK.md/PITFALLS.md counted "4 files" for `Clock` import and "4 call sites across 3 files" for `dayOfMonth`/`monthNumber` inside `library/src/main`. Direct grep in THIS research pass (§5 below) finds the renames also present in **`library/src/test/.../AeroCalendarGridTest.kt:38`** and, critically, in **`showcase/src/main/.../sections/DataSection.kt:82`** and **`showcase/.../sections/PickersSection.kt:64,112,146,164`** — 6 additional call sites across 2 showcase files plus 1 test file, none counted by the milestone research (which only grepped `library/src/main`).
**Why it happens:** the milestone research scoped its grep to `library/src/main` only, since that's the published artifact surface; it never grepped `showcase/src` or `library/src/test` for the same symbols.
**How to avoid:** treat the corrected file list in §5 as the actual scope for TOOL-14's mechanical fix commit — `AeroCalendarGridTest.kt`, `DataSection.kt`, `PickersSection.kt` (×4 call sites) must ALL be fixed in the same commit as the 4 library picker files, or the showcase won't compile even if `:library` does.
**Warning signs:** `:library` compiles clean after the datetime bump but `:showcase:compileKotlin` fails — that's this exact gap.

### Pitfall 2: The showcase has zero `testTag`s today — HRM-02 starts from nothing
**What goes wrong:** grep of all `showcase/src` for `testTag` returns zero matches. HRM-02's "where labels are ambiguous, add testTag" reads as an occasional patch but is actually "add testTags to whichever interactive elements the agent needs to reliably re-target across `reload`-induced node-id changes" — a real, non-trivial per-component task, not a one-line fix.
**How to avoid:** scope this explicitly in the plan — decide which showcase-level interactive elements (theme switcher tabs, section anchors if BASE-01 uses scroll-to-section, any popup triggers the agent will `click` via MCP) need a `testTag`, rather than discovering the gap mid-execution.

### Pitfall 3: BASE-01's addressable section list has a naming ambiguity (Foundation/Primitives)
**What goes wrong:** 15 of `ShowcaseApp.kt`'s 17 top-level items are standalone `*Section()` composable calls; 2 (`Foundation`, `Primitives`) are inline `Column { Text(heading); XxxSection() }` blocks with no section-level wrapper of their own. A section-parameter design that assumes "every section has a matching composable name" needs a decision for these two.
**How to avoid:** the plan should explicitly decide: either (a) extract `FoundationSection`/`PrimitivesSection` wrapper composables (small refactor, technically new code but not new *component* API), or (b) exclude Foundation/Primitives from the addressable list since they're always near the top of the scroll and don't need direct-launch targeting. Document the choice; don't leave it implicit.

### Pitfall 4: TOOL-16's "467" is stale — the actual current count is 474 `@Test` annotations
**What goes wrong:** STATE.md's carried-forward number ("467 tests" at v3.0 closeout) predates whatever test files were added since. A direct count in this research session (`grep -rc "@Test"` across `library/src/test`) totals **474** — 7 more than the archived figure. REQUIREMENTS.md's own text already anticipates this ("467 на закрытии v3.0 плюс тесты BASE-05; число перемеряется, а не берётся из документов") — this pitfall is a reminder that the "467" baseline itself is ALSO already stale before BASE-05 even adds anything.
**How to avoid:** TOOL-16's locked count must come from an actual `./gradlew test` run's printed summary at the START of this phase (step 2 of the roadmap's 7 steps, "lock the literal test count on the old toolchain"), not from grep (grep counts `@Test`-annotated functions, which can diverge from Gradle's actual executed-test count due to parameterized tests, disabled tests, or discovery quirks) and not from any document including this one.
**Warning signs:** any plan task that writes "assert 467" instead of "assert the count measured in step 2" is building on a number already known to be wrong.

### Pitfall 5: `onRoot().captureToImage()` behavior with an open Popup, in the desktop JVM `runComposeUiTest` harness specifically, is unverified in this codebase
**What goes wrong:** FEATURES.md confirms Popups are additional semantics "owners" in the same window for the LIVE MCP runtime (source-read, HIGH confidence) — but that finding is about the Hot Reload runtime's `get_semantic_tree`/`take_screenshot`, not about `androidx.compose.ui.test.captureToImage()` called on `onRoot()` inside a `runComposeUiTest` JVM unit test. No existing test in this codebase (`AeroThemeBackgroundEstablishmentTest.kt`, `VER07ButtonStateMatrixTest.kt`, etc.) opens a Popup and then captures — all existing `captureToImage()` usages capture non-Popup content.
**How to avoid:** BASE-05's own mandated first step — "prove `captureToImage` works in desktop `runComposeUiTest`; if not, stop and ask" — should be read as covering this specific case too: the very first D-07 popup test written should immediately check whether `onRoot().captureToImage()` captures the open popup content, or whether `onAllNodes(isRoot())`/multiple roots need to be enumerated and captured separately (candidate APIs: `onAllNodes(isRoot(), useUnmergedTree = true)` — needs empirical confirmation, this is not stated anywhere in the milestone research or this codebase). If a single `onRoot()` capture only shows the main window content with the popup missing/clipped, that is exactly D-08's pre-approved fallback trigger for that component.
**Warning signs:** a D-07 popup test's captured image visually (or pixel-count-wise) matches the closed-state capture — the popup silently isn't in the image at all.

### Pitfall 6: `AeroCalendarGrid` today-highlight claim (D-09) — verify, don't just trust the CONTEXT.md assertion
**What goes wrong:** D-09 states "AeroCalendarGrid has no today-highlight (checked)" as a settled fact from the discuss-phase session. This research did not independently re-verify that claim by reading `AeroCalendarGrid.kt`'s rendering logic in full — it is carried forward from CONTEXT.md, not re-confirmed here.
**How to avoid:** treat D-09 as MEDIUM confidence until the plan/implementation phase does a direct read of `AeroCalendarGrid.kt` to confirm no `LocalDate.now()`/today-comparison drives a visual highlight; if one exists (e.g., a subtle border on the current day for the grid backing whichever picker is under test), the deterministic-`value` fix alone is insufficient for that specific frame.

## Code Examples

### Exact kotlinx-datetime rename sites (TOOL-14) — corrected, full list
Verified via direct grep in this research session (supersedes the milestone research's narrower `library/src/main`-only count):

**`Clock` import + `Clock.System.now()` (4 files, library only — showcase never imports `Clock` directly):**
```
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt:167-168
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt:196-197
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt:263-264
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt:333-334
```
Each: `private fun todayLocalDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date` — rename `import kotlinx.datetime.Clock` → `import kotlin.time.Clock` (per kotlinx-datetime 0.7.0+ changelog); call syntax unchanged.

**`.dayOfMonth`/`.monthNumber` property access (10 files — library, test, AND showcase):**
```
library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt:71   — displayMonth.monthNumber
library/src/main/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGrid.kt:150  — displayMonth.monthNumber
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt:220                  — date.monthNumber, date.dayOfMonth
library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt:150                      — date.dayOfMonth, date.monthNumber
library/src/test/kotlin/com/mordred/aero/components/pickers/internal/calendar/AeroCalendarGridTest.kt:38 — nextMonth.dayOfMonth
showcase/src/main/kotlin/com/mordred/showcase/sections/DataSection.kt:82                                — s.aosDate.dayOfMonth, s.aosDate.monthNumber
showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt:64                             — it.dayOfMonth, it.monthNumber
showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt:112                            — rangeStart/rangeEnd .dayOfMonth/.monthNumber (2 pairs)
showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt:146                            — boundedDate.dayOfMonth/.monthNumber
showcase/src/main/kotlin/com/mordred/showcase/sections/PickersSection.kt:164                            — bStart/bEnd .dayOfMonth/.monthNumber (2 pairs)
```
Rename: `.dayOfMonth` → `.day`, `.monthNumber` → `.month.number` at every site above. `AeroTimePicker.kt:25` and `showcase/.../IconsSection.kt:65,213` are confirmed FALSE POSITIVES (fully-qualified `com.mordred.aero.icons.internal.Clock` — an icon glyph, unrelated).

### Test-count guard sketch (TOOL-16, Claude's Discretion — proposed mechanism)
```kotlin
// library/build.gradle.kts — sketch, not yet validated against Gradle 9.7.1
tasks.test {
    useJUnitPlatform()
    var executedCount = 0
    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        if (desc.parent == null) { // root suite only
            executedCount = result.testCount.toInt()
            val expected = (project.findProperty("aero.expectedTestCount") as String?)?.toIntOrNull()
            if (expected != null && executedCount != expected) {
                throw GradleException("Test count guard: expected $expected, got $executedCount")
            }
        }
    }))
}
```
Prove red first (TOOL-16's own requirement) by temporarily excluding one test class (`tasks.test { exclude("**/SomeKnownTestClass.class") }`) and confirming the guard throws — then remove the exclusion.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| `CopyFromScreen` (Win32, windows-mcp recipe, v3.0 Phase 15) | `PrintWindow(hwnd, hdc, 2)` (WinAPI, this phase) | This phase, per D-01/D-02 and maintainer's MCP-HOWTO.md | `PrintWindow` captures occluded window content correctly; `CopyFromScreen` (and the MCP server's own `take_screenshot`, which is `Robot.createScreenCapture`) both scrape the screen region and fail/corrupt when covered |
| Manual visual sign-off via windows-mcp (CursorTouch), pixel-coordinate driven | Compose Hot Reload MCP, semantics-tree driven (`click`/`get_semantic_tree` by node id) | This phase, first-time install | No coordinate-click risk of cursor movement; but no hover/drag/key-press tool exists either — those stay UI-test-only per the locked decision |
| kotlinx-datetime 0.6.2 (`dayOfMonth`/`monthNumber` properties) | 0.8.0 (`day`/`month.number`) | This phase | 10 call sites renamed; `Clock`/`Instant` import path also moves to `kotlin.time.*` |

**Deprecated/outdated:**
- README.md:6's toolchain line (Kotlin 2.1.21/CMP 1.7.3/JVM 17) has been stale since v3.0 shipped — two milestone bumps behind reality even before this phase starts.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Exact FQN of `ComposeHotRun` task type is `org.jetbrains.compose.reload.ComposeHotRun` | Architecture Patterns, §1 | Sibling forwarding block won't compile; low-cost fix (confirm via `gradlew :showcase:help --task hotRun` before writing the block) |
| A2 | `hotMcpServer` task name (not `hotMcpServerJvm`) applies for this plain-`kotlin.jvm` module | Standard Stack, inherited from milestone research | `.mcp.json` task name wrong; caught immediately by `gradlew tasks --all \| grep -i hot` per MCP-HOWTO.md's own recommended first step |
| A3 | JBR 21 (already installed) satisfies Hot Reload 1.2.0's runtime requirement without a JBR-25-vs-21 linkage error | Standard Stack | First `hotRun` invocation either links cleanly or fails loudly — self-resolving, no silent failure mode |
| A4 | `onRoot().captureToImage()` in desktop `runComposeUiTest` captures open Popup content (or a documented alternative API does) | Common Pitfalls #5 | If wrong, most of D-07's coverage falls to D-08's pre-approved after-only-inspection fallback — not a stop, but a scope reduction from the BASE-05 mechanism to MCP+PrintWindow for popups |
| A5 | `AeroCalendarGrid` genuinely has no today-highlight (D-09's claim, carried from CONTEXT.md, not independently re-verified in this session) | Common Pitfalls #6 | If a highlight exists, D-09's "fixed value alone suffices" needs an additional fixed-"today" injection mechanism for calendar-grid frames |
| A6 | README.md's actual dependency-coordinate snippet location (previously cited as `:73`, toolchain callout at `:80-82`) — not re-confirmed in this pass (only first 40 lines read) | Current State of Every Touched File, README.md | REL-04's "immediately adjacent" placement could land in the wrong spot if the file has since been restructured; cheap to fix — grep for the actual snippet before editing |

## Open Questions

1. **Does `onRoot().captureToImage()` see Popup content in desktop `runComposeUiTest`?**
   - What we know: the live MCP runtime's `get_semantic_tree` handles multi-root Popup/Dialog correctly by design (FEATURES.md, source-confirmed). No prior test in this codebase exercises `captureToImage()` with an open Popup.
   - What's unclear: whether the JVM unit-test harness's screenshot mechanism (different code path from the live-app MCP server) has the same multi-root awareness.
   - Recommendation: make this literally the first D-07 task — write one popup test, inspect the captured image, and branch to D-08's fallback per-component if it doesn't work. This is exactly what BASE-05's own "prove `captureToImage` works first; if not, stop and ask" clause anticipates, applied to the Popup case specifically.

2. **Is Foundation/Primitives addressable as BASE-01 sections, or excluded?**
   - What we know: 15/17 showcase items are standalone `*Section()` composables; Foundation/Primitives are inlined.
   - What's unclear: whether the plan should do a small structural extraction (2 new wrapper composables) or exclude these 2 from BASE-01's addressable list.
   - Recommendation: exclude by default (lowest-risk, zero new code) unless per-component baseline/after frames specifically need direct-launch access to Foundation/Primitives independent of the top-of-scroll default view.

3. **Exact current README.md structure beyond line 40** — not read in this pass.
   - Recommendation: grep `README.md` for `implementation("com.github` and the existing toolchain-requirement callout before writing REL-04's plan tasks.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JBR 21 | HRM/JDK toolchain | ✓ | `jbr-21.0.9` at `C:\Users\1\.jdks\jbr-21.0.9` | — [VERIFIED: MCP-HOWTO.md, checked on this machine 2026-09-23] |
| foojay-resolver-convention | JDK auto-provisioning | Not needed | — | JBR 21 already Gradle-visible; foojay would be redundant per maintainer's own check |
| `NoDefaultCurrentDirectoryInExePath=1` (Windows env setting) | `.mcp.json` command construction | ✓ (confirmed set) | — | Must use `cmd /c ".\gradlew.bat ..."`, never bare `gradlew.bat` or `./gradlew` |
| PowerShell 5.1 + `Add-Type` (C#) | BASE-02 capture helper | ✓ (project CLAUDE.md: "system PowerShell is 5.1") | 5.1 | — |
| `slopcheck` / package registry checks | Package Legitimacy Audit | N/A — no new third-party packages this phase | — | — |

**Missing dependencies with no fallback:** none identified.
**Missing dependencies with fallback:** none identified — the one "missing" item (foojay) has an explicit not-needed determination, not a gap.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 6.1.3 (jupiter, post-bump) via `kotlin.test` facade; Compose `runComposeUiTest` for UI tests |
| Config file | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`, no separate JUnit XML config file found) |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.pickers.*"` (scoped, for picker-only re-verification after TOOL-14) |
| Full suite command | `./gradlew test` (root — runs `:library:test`; `:showcase` has no test source set found) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BASE-05 | `captureToImage` proof-of-work in desktop `runComposeUiTest` | unit (Compose UI test) | `./gradlew :library:test --tests "*Base05ProofOfWork*"` | ❌ Wave 0 — new file |
| TOOL-14 | Picker/calendar behavior unchanged post-rename | unit | `./gradlew :library:test --tests "com.mordred.aero.components.pickers.*" --tests "*AeroCalendarGridTest*"` | ✅ existing (`AeroDatePickerTest`, `AeroDateTimePickerTest`, `AeroDateRangePickerTest`, `AeroDateTimeRangePickerTest`, `AeroCalendarGridTest` — exact file names to confirm at execution, not independently verified in this pass) |
| TOOL-16 | Exact test count locked and guard proven red | build verification | `./gradlew test` (read printed count) + a temporary `exclude()` run | ❌ Wave 0 — guard task itself is new |
| HRM-01 | `:library` POM/module metadata unchanged after Hot Reload added | build verification | `git diff --stat -- library/build.gradle.kts` (must be empty) + `./gradlew :library:generatePomFileForMavenPublication :library:publishToMavenLocal` diff | ❌ Wave 0 — no existing task for this, per ARCHITECTURE.md's proposed two-tier check |
| TOOL-12 | Material3 stays 1.9.0 after every bump incl. Hot Reload | build verification | `./gradlew :showcase:dependencyInsight --dependency material3 --configuration runtimeClasspath` | ❌ Wave 0 — manual/scripted check, no automated assertion task exists yet |

### Sampling Rate
- **Per task commit:** the full `./gradlew test` run (this project's suite runs in well under 30s historically per v3.0 metrics — confirm at execution) — matches the phase's own "every bump gated by a full test run" locked rule, stricter than the usual "quick run" sampling.
- **Per wave merge:** same — this phase has no smaller unit than "one version bump = one commit = one full-suite gate," per the roadmap's own execution order.
- **Phase gate:** full suite green at the locked, re-measured count (TOOL-16) before `/bm:verify-work`.

### Wave 0 Gaps
- [ ] A BASE-05 "proof of work" test file confirming `captureToImage` works in desktop `runComposeUiTest` — first file to write, per the roadmap's own step-1 requirement.
- [ ] The TOOL-16 test-count guard task itself (see Code Examples sketch above) — does not exist yet.
- [ ] A repeatable `dependencyInsight`-based Material3 check — currently manual; could be scripted as a verification task but no existing automated gate found.
- [ ] The `:library` POM/module-metadata diff check for HRM-01 — no existing task; ARCHITECTURE.md's two-step manual proof (`git diff --stat`, then `generatePomFileForMavenPublication`/`publishToMavenLocal` diff) is the closest existing plan, still manual.

## Security Domain

> `security_enforcement` status not found in `.planning/config.json` in this research pass — treating as enabled per the skill's default.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Desktop UI library, no auth surface |
| V3 Session Management | No | N/A |
| V4 Access Control | No | N/A |
| V5 Input Validation | No | This phase adds no new user-input-handling code — pickers' date logic is unchanged behaviorally (TOOL-14's explicit requirement) |
| V6 Cryptography | No | N/A |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| MCP server reachable by any local process, driving live clicks/reloads against the maintainer's desktop | Elevation of privilege / Tampering | Confirm the MCP server communicates ONLY via the stdio transport `.mcp.json` itself spawns (no open network port) — already flagged in PITFALLS.md's Security Mistakes table; a one-time check during HRM-01 setup |
| Orphaned Gradle/JVM processes surviving a session, silently consuming resources or holding a window open | Denial of Service (local) | Explicit Task Manager / `Get-Process java` check after a verification session, documented cleanup step — PITFALLS.md Pitfall 5 |
| A future consumer's build silently resolving a wrong/malicious Material3 alpha via the Hot Reload devtools dependency edge | Tampering (supply chain, low severity — same-publisher artifact) | `dependencyInsight` check, `resolutionStrategy.force` if needed, scoped to `:showcase` only — PITFALLS.md Pitfall 2 |

This phase introduces no new attack surface beyond the MCP server's local-process-reachable stdio channel (already scoped `:showcase`-only, dev-time-only, never in the published artifact) and the dependency-version bumps themselves (all official JetBrains/Kotlin Foundation coordinates, no new third-party supply-chain exposure).

## Sources

### Primary (HIGH confidence)
- Direct file reads, this session (2026-09-23): `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`, `build.gradle.kts` (root), `library/build.gradle.kts`, `showcase/build.gradle.kts`, `jitpack.yml`, `README.md` (first 40 lines), `.gitignore`, `showcase/src/main/kotlin/com/mordred/showcase/Main.kt`, `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt`, `gradle.properties`
- Direct grep, this session: `dayOfMonth`/`monthNumber` (10 files, corrected from milestone research's 4+4), `kotlinx.datetime.Clock`/`Instant` (4 files, confirmed), `Popup(` (14 files), `todayLocalDate`/`Clock.System.now` (4 files, confirmed matches milestone research), `@Test` count (474), `testTag` in showcase (0 matches)
- `.planning/research/MCP-HOWTO.md` — maintainer-verified, first-hand, wins on conflict per CONTEXT.md/STATE.md

### Secondary (MEDIUM confidence)
- `.planning/research/SUMMARY.md`, `STACK.md`, `PITFALLS.md`, `ARCHITECTURE.md`, `FEATURES.md` — milestone-level desk research, dated 2026-09-21, itself Maven-Central/official-docs-verified for version facts; carried forward without re-verification of external sources in this session (2 days newer)
- `.planning/milestones/v3.0-phases/15-toolchain-upgrade/baseline/README.md`, `.../after/DIFF-REPORT.md` — v3.0 precedent for baseline/diff report format, directly read this session
- Memory: `reference_compose_hot_reload_mcp.md`, `reference_windows_mcp_showcase_capture.md`

### Tertiary (LOW confidence, flagged inline)
- `ComposeHotRun` exact FQN (A1) — inferred by package-naming convention, not confirmed against source in this session
- README.md structure beyond line 40 (A6) — not read in this pass

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all current-state file reads direct; target versions inherited from milestone research which cross-checked Maven Central directly
- Architecture: MEDIUM-HIGH — file-level isolation claims are HIGH (direct reads); `ComposeHotRun` exact task-wiring is MEDIUM (inferred, one open assumption A1)
- Pitfalls: HIGH where grep-verified in this session (datetime rename file list, testTag count, test count); MEDIUM where carried from milestone research without re-verification

**Research date:** 2026-09-23
**Valid until:** short — this is a fast-moving, version-bump-heavy phase; re-verify Maven Central/registry facts if execution starts more than ~7 days after this research date
