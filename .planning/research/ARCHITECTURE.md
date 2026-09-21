# Architecture Research — v3.1 Dependency Refresh + Hot Reload MCP Integration

**Domain:** Gradle multi-module Kotlin/JVM library (`aero-compose-ui`) — integrating a toolchain/dependency bump with Compose Hot Reload + an MCP server, isolated to `:showcase`
**Researched:** 2026-09-21
**Confidence:** MEDIUM-HIGH overall (file-level claims HIGH, Gradle-9/Hot-Reload-MCP external facts MEDIUM-HIGH, a few flagged UNVERIFIED)

## System Overview

```
repo root (C:\1A_WORK\ui_lib)
├── gradle/libs.versions.toml         ── single source of version truth (catalog)
├── gradle/wrapper/gradle-wrapper.properties
├── settings.gradle.kts               ── plugin/dep repos, :library + :showcase includes
├── build.gradle.kts                  ── allprojects{ group; version } ONLY, no plugins
├── gradle.properties                 ── org.gradle.* JVM/parallel/caching flags
├── jitpack.yml                       ── JitPack CI: JDK install + gradlew invocation
├── .mcp.json                         ── NEW — Claude Code project-scoped MCP config
│
├── library/build.gradle.kts          ── kotlin.jvm + compose.multiplatform + compose.compiler
│                                         + maven-publish. explicitApi(). PUBLISHED artifact.
│                                         NEVER touches Hot Reload.
│
└── showcase/build.gradle.kts         ── kotlin.jvm + compose.multiplatform + compose.compiler
                                          + compose.desktop.application. NOT published.
                                          Hot Reload + MCP live ONLY here.
```

```
┌───────────────────────────────────────────────────────────────────────────┐
│  Claude Code (this machine)                                                 │
│  reads .mcp.json → spawns ONE long-lived stdio child process                │
│  cmd /c gradlew.bat --no-daemon --quiet --console=plain :showcase:hotMcpServer│
└───────────────────────────┬───────────────────────────────────────────────┘
                             │ stdin/stdout = JSON-RPC ONLY (no Gradle chatter allowed)
                             ▼
┌───────────────────────────────────────────────────────────────────────────┐
│  Gradle 9.7.1 process (cold JVM each launch, JDK 21 toolchain)              │
│  :showcase:hotMcpServer  ── waits for app, then connects to the             │
│  hot-reload-supervised JVM running Main.kt on JetBrains Runtime 21          │
│    - reload / take_screenshot / get_semantic_tree / get_logs /              │
│      click / type_text / scroll  tools exposed to the MCP client            │
└───────────────────────────┬───────────────────────────────────────────────┘
                             ▼
                :showcase JVM (JBR 21) — AeroTheme(currentScheme) { ShowcaseApp }
                initialScheme() reads -Daero.scheme, unaffected by which
                Gradle task launched it (hotRun vs hotMcpServer vs run)
```

`:library`'s publish pipeline (`generatePomFileForMavenPublication`, `publishToMavenLocal`, JitPack) is a **completely separate graph** — it never includes `:showcase`'s build script, so nothing added to `showcase/build.gradle.kts` can reach it unless someone also edits `library/build.gradle.kts` or the root `build.gradle.kts`'s `allprojects{}` block.

## Component Responsibilities

| Component | Responsibility | Touched by this milestone |
|-----------|-----------------|---------------------------|
| `gradle/libs.versions.toml` | Single version source (`[versions]`/`[libraries]`/`[plugins]`) consumed via `libs.*` aliases by both modules | MODIFIED (5 version bumps) + NEW entries (`composeHotReload`, optionally `foojayResolver`) |
| `gradle/wrapper/gradle-wrapper.properties` | Pins the Gradle distribution the `./gradlew`/`gradlew.bat` wrapper downloads | MODIFIED (`distributionUrl`) |
| `settings.gradle.kts` | Plugin/dependency repositories, module includes, `FAIL_ON_PROJECT_REPOS` | MODIFIED (foojay resolver plugin; JetBrains Space repo re-evaluated) |
| `build.gradle.kts` (root) | `group`/`version` for all subprojects only — deliberately plugin-free per its own header comment (`build.gradle.kts:1`) | MODIFIED (version bump `3.0.0`→`3.1.0`) |
| `library/build.gradle.kts` | Published artifact: `explicitApi()`, `maven-publish`, `jvmToolchain`, `api(...)` surface | MODIFIED (`jvmToolchain(17)`→`21`); **Hot Reload plugin is never added here** |
| `showcase/build.gradle.kts` | Demo app: `compose.desktop.application`, `-Paero.scheme` forwarding | MODIFIED (`jvmToolchain`, Hot Reload plugin, `ComposeHotRun` forwarding block) |
| `gradle.properties` | `org.gradle.*` global flags | MODIFIED (add `org.gradle.welcome=never`) |
| `jitpack.yml` | JitPack CI JDK provisioning before `./gradlew` runs | MODIFIED (JDK 17 → 21) |
| `.mcp.json` | Claude Code stdio MCP server registration | NEW |
| `README.md` | Consumer-facing toolchain requirements | MODIFIED |

## File-Level Change Map (Q1)

| Target | File(s) | New / Modified | What changes |
|---|---|---|---|
| Kotlin 2.4.10→2.4.20 | `gradle/libs.versions.toml:2` (`kotlin = "2.4.10"`) | Modified | Version string only; consumed by `alias(libs.plugins.kotlin.jvm)` and `alias(libs.plugins.compose.compiler)` in both `library/build.gradle.kts` and `showcase/build.gradle.kts` — no other line changes needed there since both already use catalog aliases |
| CMP 1.11.1→1.12.0 | `gradle/libs.versions.toml:3` (`composeMultiplatform = "1.11.1"`) | Modified | Same propagation via `alias(libs.plugins.compose.multiplatform)` |
| coroutines 1.10.2→1.11.0 | `gradle/libs.versions.toml:4` | Modified | `implementation(libs.kotlinx.coroutines.core)` (`library/build.gradle.kts:29`) and `testImplementation(libs.kotlinx.coroutines.test)` (`:33`) pick it up automatically — internal-only, no public API leak (confirmed by the existing `implementation` scoping) |
| kotlinx-datetime 0.6.2→0.8.0 | `gradle/libs.versions.toml:6` | Modified | See dedicated section below — **zero source-line changes expected** in the 4 picker files |
| JUnit 5.10.0→6.1.3 | `gradle/libs.versions.toml:5` | Modified | `junit-jupiter` alias (`library/build.gradle.kts:32`) tracks it; unversioned `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` (`:35`) continues to resolve via Jupiter's own BOM-style dependency constraints — **no line change needed there** |
| Gradle wrapper 8.14.3→9.7.1 | `gradle/wrapper/gradle-wrapper.properties:3` | Modified | `distributionUrl` string |
| JDK 17→21 everywhere | `library/build.gradle.kts:9` (`jvmToolchain(17)`), `showcase/build.gradle.kts:10` (`jvmToolchain(17)`) | Modified | Both become `jvmToolchain(21)` |
| Hot Reload plugin | `gradle/libs.versions.toml` `[versions]`+`[plugins]` (new `composeHotReload` entries) + `showcase/build.gradle.kts` `plugins{}` block | New catalog entry + modified showcase plugins block | See isolation section — **`library/build.gradle.kts` is not touched** |
| foojay/toolchain resolver | `settings.gradle.kts` | New block | `pluginManagement { plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" } }` — see reasoning below; recommended, not strictly forced by any file that currently exists |
| `.mcp.json` | repo root | New file | See shape below |
| `jitpack.yml` | `jitpack.yml:1-6` | Modified | `jdk: [openjdk21]`, `sdk install/use java 21.x.x-tem` (confirm exact SDKMAN identifier at build time — training data does not pin the exact current 21.x-tem patch string) |
| README | `README.md:6` (header badge line), `:80-82` (toolchain requirement), `:129-135` (tech stack list), `:73` (`v3.0.0`→`v3.1.0` dependency snippet) | Modified | Also fixes a **pre-existing staleness bug**: the header line (`README.md:6`) still says `Kotlin 2.1.21 · Compose Multiplatform 1.7.3 · JVM 17` — it was never updated when v3.0 shipped Kotlin 2.4.10/CMP 1.11.1/JDK 17. Worth fixing now regardless of v3.1, since v3.1 touches the same lines anyway |
| Root version bump | `build.gradle.kts:4` | Modified | `version = "3.0.0"` → `"3.1.0"`, per the locked "bump version on milestone" rule — do this **before** tagging, not after |
| `gradle.properties` | `gradle.properties` | Modified | Add `org.gradle.welcome=never` (see stdio-cleanliness section) |
| `Main.kt` | `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` | **Unchanged** | `initialScheme()` (`Main.kt:29`) reads `System.getProperty("aero.scheme")` regardless of which Gradle task launched the JVM — the forwarding logic that needs duplicating lives entirely in `showcase/build.gradle.kts`, not here |

## Isolating Hot Reload to `:showcase` (Q2)

**Plugin application.** Add to `gradle/libs.versions.toml`:
```toml
[versions]
composeHotReload = "1.2.0"

[plugins]
compose-hot-reload = { id = "org.jetbrains.compose.hot-reload", version.ref = "composeHotReload" }
```
Then, in `showcase/build.gradle.kts` **only**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}
```

JetBrains' own docs recommend applying the plugin `apply false` at the root and `alias(...)` in each consuming subproject — that pattern exists to avoid loading the plugin multiple times across *many* KMP subprojects sharing one root. This repo's root `build.gradle.kts` deliberately applies **no plugins at all** (its own comment: "Root build — no plugins applied here. Per-module build files own their plugins.", `build.gradle.kts:1`), and only one of the two subprojects will ever touch Hot Reload. Following the existing repo convention (apply directly in the one module that needs it) is simpler and equally correct here; there is no second subproject that would trigger the multiple-loading problem the root-`apply false` pattern exists to solve. (Recommendation, not an external requirement — flagging so the roadmapper can override if they'd rather match upstream docs literally.)

**Why `:library`'s POM/module metadata cannot be affected — structurally, not just empirically.** Gradle gives each subproject its own isolated `plugins{}`/`dependencies{}` graph; `:library`'s publication metadata (`generatePomFileForMavenPublication`, and Gradle Module Metadata's `.module` file) is derived exclusively from `library/build.gradle.kts` plus the root `allprojects{}` block (group/version only). Since the Hot Reload plugin is added only inside `showcase/build.gradle.kts`, there is no code path by which it can appear in `:library`'s output — the same way `compose.desktop.currentOs` (showcase-only, native-binary dependency) never leaks into `:library`'s `api(compose.desktop.common)` (platform-neutral) surface today.

**How to prove it (in order of cheapness):**
1. `git diff --stat -- library/build.gradle.kts` on the commit that adds Hot Reload — must show **zero** lines changed. This alone is the simplest proof and should be the PR-level gate.
2. Empirical belt-and-suspenders: before adding Hot Reload (right after the library-dependency-bump phase), run
   ```
   cmd /c gradlew.bat --no-daemon --quiet --console=plain :library:generatePomFileForMavenPublication :library:publishToMavenLocal
   ```
   and copy `library/build/publications/maven/pom-default.xml` plus the generated `.module` file (Gradle Module Metadata, under `library/build/libs/`) to a scratch location. After adding Hot Reload + MCP to `:showcase`, re-run the same two tasks and diff both files against the saved copies — expect byte-identical output (the `3.1.0` version string is already fixed by that point in the build order, so there's no legitimate reason for any diff at all).

## Task Wiring (Q3)

**MCP server task name for a plain Kotlin/JVM module.** Compose Hot Reload names its run task `hotRunJvm` for Kotlin Multiplatform projects with a `jvm` target, but **`hotRun` (no suffix) for plain `kotlin("jvm")`/`org.jetbrains.kotlin.jvm` modules** — `:showcase` is the latter (`showcase/build.gradle.kts:4`, `alias(libs.plugins.kotlin.jvm)`, not the multiplatform plugin). By the same naming convention, the MCP task on a plain JVM module should be **`hotMcpServer`** (no `Jvm`/`Desktop` suffix) rather than `hotMcpServerJvm`. **Confidence: MEDIUM** — this is inferred by analogy from confirmed `hotRun`-vs-`hotRunJvm` naming, corroborated by (lower-quality) search summaries stating the same, but not confirmed against a primary source enumerating the plain-JVM MCP task name explicitly. **Verify before wiring `.mcp.json`:** `cmd /c gradlew.bat --no-daemon :showcase:tasks --all` and grep for `hot` — do this as the very first step of the Hot Reload phase, before writing `.mcp.json`.

**Extending `-Paero.scheme` forwarding.** The existing block:
```kotlin
tasks.withType<JavaExec>().configureEach {
    if (name == "run") { ... systemProperty("aero.scheme", it) }
}
```
(`showcase/build.gradle.kts:27-31`) only fires for the plain `compose.desktop.application` `run` task, which **is** a `JavaExec`. Compose Hot Reload's run task is **not** a `JavaExec` subtype — it is its own Gradle task type, `ComposeHotRun` (package believed to be `org.jetbrains.compose.reload.*` — **UNVERIFIED exact FQN**, confirm via IDE autocomplete or `gradlew :showcase:help --task hotRun` once the plugin is applied), which exposes its own `systemProperty(...)`, `jvmArgs`, `args(...)`, `mainClass.set(...)` API. Because it is a separate task type, the existing `tasks.withType<JavaExec>()` block **will not** reach it — it must be extended with a sibling block:
```kotlin
tasks.withType<ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
}
```
Unlike the `JavaExec` case, there's no need for an `if (name == "run")` guard here, since (unlike `JavaExec`, which also matches unrelated tasks like `compileTestJava`'s helper execs) `ComposeHotRun` is Hot-Reload-specific — every task of that type in this module is a "run the showcase" task.

Whether `hotMcpServer` itself also respects this forwarding depends on whether it launches the app via the same `ComposeHotRun` machinery internally or via a separate code path — the README excerpt found during research says the MCP task "does not require a running application: it starts, waits for the app to launch, and then connects automatically," which reads as `hotMcpServer` supervising its own app launch rather than attaching to an already-running `hotRun`. If so, the `ComposeHotRun` forwarding block above should cover it since both would go through the same task type. **This is the milestone's own point 3's fallback already anticipated by the maintainer** ("theme switching should instead be done by MCP clicks") — if `-Paero.scheme` forwarding turns out not to reach the MCP-launched instance in practice, driving the theme switcher via the MCP `click` tool is the documented fallback, not a blocker.

## `.mcp.json` on Windows (Q4)

Claude Code's project-scoped `.mcp.json` schema (`mcpServers.<name>`) has no `cwd` field — a project-root `.mcp.json` runs with the project directory as the working directory automatically, which is exactly where `gradlew.bat` lives, so no path juggling is needed:

```json
{
  "mcpServers": {
    "aero-showcase-hot-reload": {
      "type": "stdio",
      "command": "cmd",
      "args": [
        "/c",
        "gradlew.bat",
        "--no-daemon",
        "--quiet",
        "--console=plain",
        ":showcase:hotMcpServer"
      ]
    }
  }
}
```
(`command: "cmd"` + `args: ["/c", ...]` is Claude Code's documented Windows pattern for `.bat`/`.cmd` launchers — confirmed against the current Claude Code MCP docs.)

**Stdio cleanliness — what's confirmed vs. what to test:**

| Source of noise | Mitigation | Confidence |
|---|---|---|
| ANSI/progress-bar rendering | `--console=plain` | HIGH — documented Gradle flag |
| LIFECYCLE/WARNING logging (deprecation warnings, most task chatter) | `--quiet` | HIGH — standard Gradle log-level semantics |
| First-run "Welcome to Gradle 9.7.1!" banner after the wrapper bump | `org.gradle.welcome=never` in `gradle.properties` | HIGH — a known Gradle GitHub issue (#5213) specifically notes the welcome message **does not respect `--quiet`**, so the explicit property is the only reliable suppression |
| Gradle **wrapper distribution download** progress lines (`Downloading https://services.gradle.org/distributions/gradle-9.7.1-bin.zip...`) | **Pre-warm**: run the upgraded wrapper once, interactively, in a normal terminal, before ever registering `.mcp.json` | HIGH on the risk (Gradle issue #1845 explicitly documents that wrapper-download messages print to stdout even with `--console=plain --quiet` combined) — mitigation is a process recommendation, not a flag |
| JDK 21 toolchain auto-download progress (if JDK 21 isn't already installed on the maintainer's machine) | Same pre-warm step; also confirm a JDK 21 is already discoverable (`java -version` / installed IDE JDK) before relying on auto-download at all | MEDIUM — reasoned by analogy from the wrapper-download case; no primary source found describing toolchain-download stdout behavior specifically |
| Configuration-cache state/report noise | **Don't enable it.** `gradle.properties` currently has no `org.gradle.configuration-cache=true` (confirmed by reading the file) — leave it off for this milestone; it's a whole separate noisy-output category (`org.gradle.unsafe.configuration-cache.quiet=true` is the only documented mitigation, and it's explicitly undocumented/internal) that the milestone doesn't need | HIGH on current state, judgment call on "leave it off" |
| Stray `println`/`System.out` in the build scripts themselves | Grepped: none found in any of the 3 `build.gradle.kts` files read for this research | HIGH (direct grep) |

**`--no-daemon` and toolchain resolution.** `--no-daemon` only changes whether the *Gradle-launcher JVM itself* persists between invocations — toolchain discovery/auto-download is part of Gradle's build-execution logic, not daemon state, so it behaves identically with or without a daemon. The practical implication for this milestone: because `hotMcpServer` is expected to be a **long-lived** process (it's the stdio MCP server Claude Code keeps open for the whole session, not a one-shot CLI call), `--no-daemon` mainly avoids leaving behind an orphaned background Gradle daemon after that long-lived process exits — it does not mean toolchain resolution repeats per MCP tool-call, only once at that single process's startup.

## Gradle 8.14.3 → 9.7.1 Impact on This Build (Q5)

| Construct in this repo | File:line | Gradle 9 impact | Verdict |
|---|---|---|---|
| `tasks.withType<JavaExec>().configureEach { ... }` | `showcase/build.gradle.kts:27` | Not removed/changed in Gradle 9's upgrade guide | Safe as-is; still needed for the plain `run` task |
| `publishing { publications { create<MavenPublication>(...) } }` | `library/build.gradle.kts:48-76` | `maven-publish` DSL unaffected; only a **future Gradle 10** change removes map-style multi-arg dependency notation (`implementation(group=, name=, version=)`) — not used anywhere in this repo (verified: all deps use single-string or catalog-alias notation) | Safe |
| `java { withSourcesJar() }` | `library/build.gradle.kts:13-16` | Unaffected | Safe |
| `kotlin("reflect")` | `library/build.gradle.kts:34` | Unaffected — this is a Gradle Kotlin-DSL helper function, independent of the Gradle-version bump | Safe |
| `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` (unversioned) | `library/build.gradle.kts:35` | Under JUnit 6's **unified versioning** (platform artifacts now share Jupiter's version number), this unversioned declaration continues to resolve correctly via the version constraint that `junit-jupiter`'s own POM brings in — no line change required, it will track `6.1.3` automatically once the catalog's `junit` ref is bumped | Safe, verify by watching which launcher version actually resolves during the test-suite gate |
| `dependencyResolutionManagement { repositoriesMode.set(FAIL_ON_PROJECT_REPOS) }` | `settings.gradle.kts:11` | No documented Gradle 9 change found affecting this mode | Safe |
| Kotlin DSL delegated properties (`by project`, `by registering`) | — | Deprecated in Gradle 9's embedded Kotlin DSL | Not used anywhere in the 3 build files read — no impact |
| `org.gradle.internal.impldep.*` imports | — | Removed — Kotlin DSL scripts now compile against a public API jar only | Not used anywhere in this repo — no impact |
| `maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")` | `settings.gradle.kts:6,15` | **Verified by direct fetch: this URL now returns HTTP 301 → `https://packages.jetbrains.team/maven/p/cmp/dev/`.** It still resolves (redirect followed), so the build won't hard-fail, but it's fragile (an extra network hop through a redirect) and, per JetBrains' own current `compose-multiplatform-desktop-template`, a *stable*-release desktop project only needs `gradlePluginPortal()` + `mavenCentral()` — no dev/Space repo at all. This repo's `settings.gradle.kts` currently carries it in both `pluginManagement` and `dependencyResolutionManagement`, likely a leftover from an early-access era. **Recommendation:** remove both lines as part of this milestone, then run one clean build (`--refresh-dependencies`) to confirm CMP 1.12.0 / Kotlin 2.4.20 / Compose Hot Reload 1.2.0 all resolve from `mavenCentral()`/`google()`/`gradlePluginPortal()` alone before deleting it for good — don't remove blind. | HIGH confidence on the redirect fact (direct fetch); MEDIUM-HIGH on "safe to remove" (based on the official template, not this specific project's dependency graph) |
| JVM toolchain auto-download source | `settings.gradle.kts` (none currently) | Not a Gradle-9-specific break, but **Gradle 9 removed the class that `foojay-resolver-convention` ≤0.5.x depended on** — any pre-9 resolver plugin version would now fail; ≥0.8.0 (or the newer `1.0.0` coordinate) is Gradle-9-compatible | Recommend **adding** `org.gradle.toolchains.foojay-resolver-convention` (≥0.8.0) fresh, rather than assuming it's already handled — it isn't present in `settings.gradle.kts` today, and without it, a machine that doesn't already have a JDK 21 installed will hard-fail toolchain resolution rather than auto-downloading one |
| JitPack CI environment | `jitpack.yml` | JitPack explicitly `sdk use java 21.x.x-tem` before invoking `./gradlew`, so `JAVA_HOME` is already JDK 21 there — Gradle's toolchain auto-*detection* (not auto-*download*) finds it without needing foojay. The foojay addition above is mainly insurance for the maintainer's own Windows machine, not for JitPack | — |

## Suggested Build Order (Q6)

The v3.0 lesson locked in `.planning/PROJECT.md`'s Key Decisions table is explicit: *"Смешать риск апгрейда зависимостей с риском кода отрисовки — значит потерять возможность отнести регрессию к одной из причин"* (mixing dependency-upgrade risk with drawing-code risk loses the ability to attribute a regression to one cause). v3.1 has no drawing-code risk (out of scope), but it *does* have four risk categories with genuinely different failure signatures, so the same discipline still applies — just at a coarser grain, because the milestone is explicitly meant to be SMALL and none of these four groups share a failure surface with each other:

| Step | What | Depends on | Gate | Why this grain (not finer, not coarser) |
|---|---|---|---|---|
| 0 | **Baseline capture** — manual before-screenshots, 3 themes, every showcase section, on the *current* toolchain (CMP 1.11.1) | Nothing | Screenshots saved | Must happen **before step 2**: the MCP server doesn't exist until CMP 1.12.0 lands, and the manual method is what's available now. Doing this first is not optional — it's the only point at which the "old manual method" baseline is reproducible without checking out an old tag |
| 1 | **Build-tool bump alone**: Gradle wrapper → 9.7.1, JDK → 21 everywhere, foojay resolver, `org.gradle.welcome=never` — **zero Kotlin/Compose/library version change** | Step 0 done | 467 tests green, showcase starts | Isolates "does the build tool still build the *unchanged* v3.0 code" from every other risk. A failure here can only mean Gradle/JDK, nothing else changed |
| 2 | **Kotlin 2.4.20 + Compose Multiplatform 1.12.0** (+ compose-compiler alignment) | Step 1 green | 467 tests green, showcase starts, no compile errors | This exact pairing (like Kotlin 2.4.10+CMP 1.11.1 in v3.0) is the highest-risk single jump — compiler/runtime co-evolution, not library API surface. Isolating it means a regression here is attributable to the compiler/runtime pair, not to a downstream library |
| 3 | **Supporting libraries together**: coroutines 1.11.0, kotlinx-datetime 0.8.0, JUnit 6.1.3 | Step 2 green | 467 tests green (watch specifically the 4 picker-related test files and any JUnit-runner config errors) | These three deliberately are **not** split further: they touch disjoint code (coroutines = internal-only `implementation`, datetime = 4 picker files, JUnit = test infra only), so a test failure already tells you which one broke without needing separate phases — splitting them would add three phase-transition overheads for zero extra attributability, which fights the "keep it SMALL" goal |
| 4 | **Hot Reload + MCP wiring in `:showcase` only** | Step 2 (CMP 1.12.0 is a hard prerequisite — Hot Reload 1.2.0 is bundled with/targets it); independent of step 3 | `git diff --stat -- library/build.gradle.kts` empty; POM/module diff empty; `hotMcpServer`-equivalent task discoverable via `:showcase:tasks --all` | Doing this *after* dependency stabilization means a stdio/task-name/POM-leak problem here is diagnosed against an already-known-good dependency graph, not conflated with "did the datetime bump also break something" |
| 5 | **MCP non-interference proof** — real cursor movement, focus stealing, occluded/minimized-window behavior, with the maintainer at the keyboard | Step 4 | Documented fact, not assumption (per milestone's own explicit decision rule: "если MCP двигает реальный курсор / крадёт фокус — остановка и вопрос мейнтейнеру") | This is empirical by nature — no amount of research substitutes for the milestone's own mandated hands-on test. Research found no documentation confirming or denying whether the MCP `click`/`type_text`/`scroll` tools move the OS cursor — **flagged as the one genuinely open question this document cannot resolve** |
| 6 | **Agent visual sweep** — three themes × all showcase sections via MCP, diffed against step 0's baseline | Step 5 (maintainer needs to be comfortable with MCP running before letting it drive unattended) | No unexplained visual drift | — |
| 7 | **Release** — version → `3.1.0` (`build.gradle.kts:4`), README requirements, tag `v3.1.0`, JitPack green | Steps 1-6 all green | JitPack build succeeds on JDK 21 | — |

**Bundling vs. stepping, argued directly:** bundle steps 1+2+3 into one "just bump everything" commit and a regression could be *any* of five independent version changes plus a build-tool change — exactly the ambiguity the v3.0 retrospective identified as costly. But splitting step 3's three libraries into three separate phases buys nothing, because (unlike Kotlin/CMP or Gradle/JDK) they don't share a failure surface with each other or with steps 1-2 — a failing datetime-picker test can't be confused with a coroutines regression or a JUnit-runner misconfiguration regardless of whether they land in one commit or three. The four-step grain (build-tool / toolchain-pair / library-set / hot-reload-mcp) is the coarsest split that still keeps every category individually attributable, which is the right size for a milestone explicitly scoped as SMALL.

## kotlinx-datetime 0.6.2 → 0.8.0 Inside `:library` (Q7)

Grep results across `library/src/main` and `library/src/test` for `kotlinx.datetime.Instant`, `Clock`, `toInstant`, `TimeZone`, `Clock.System`, `todayIn`:

| File | Usage | Line(s) | Verdict |
|---|---|---|---|
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDatePicker.kt` | `import kotlinx.datetime.Clock`, `import kotlinx.datetime.TimeZone`, `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date` | 27, 29, 168 | Source-compatible, no change expected (see below) |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimePicker.kt` | Same pattern | 33, 37, 197 | Same |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateRangePicker.kt` | Same pattern | 31, 34, 264 | Same |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroDateTimeRangePicker.kt` | Same pattern | 35, 40, 334 | Same |
| `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroTimePicker.kt` | `import com.mordred.aero.icons.\`internal\`.Clock` | 25 | **False positive** — this is the `AeroIcons.Clock` glyph, an unrelated icon import that happens to share the name `Clock`; fully-qualified path proves it's not `kotlinx.datetime.Clock`. No change |
| `showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt` | Same `AeroIcons.Clock` icon reference | 65, 213 | Same false positive, showcase-side |
| `kotlinx.datetime.Instant` | — | — | **Zero matches anywhere in the codebase** — the single highest-risk removal (`Instant` was fully removed in kotlinx-datetime 0.7.0) doesn't apply here at all |
| `todayIn`, `toInstant` | — | — | Zero matches |
| Test files (`library/src/test/**`) | — | — | Zero matches for `Clock`/`Instant`/`TimeZone` (grep confirmed against the full test tree) |

**Why no source change is expected.** kotlinx-datetime 0.7.0 removed `kotlinx.datetime.Instant`/`Clock` in favor of `kotlin.time.Instant`/`kotlin.time.Clock` (stdlib, requires Kotlin ≥2.1.20 — satisfied by the target Kotlin 2.4.20). This *would* have broken all 4 picker files' `Clock.System.now()` calls outright. But **0.7.1 reintroduced `kotlinx.datetime.Instant`/`Clock` as type aliases pointing at the `kotlin.time` equivalents**, restoring source compatibility for exactly this call pattern (`import kotlinx.datetime.Clock; Clock.System.now()` keeps compiling unchanged, it's now just resolving through an alias). `TimeZone` and `LocalDateTime`/`LocalDate` were never moved — only `Instant`/`Clock` — so the `.toLocalDateTime(TimeZone.currentSystemDefault())` chain is untouched regardless.

The milestone's own decision to take "чистая 0.8.0 (без `-0.6.x-compat`)" matters here: a separate `0.7.1-0.6.x-compat`-suffixed artifact exists upstream for consumers who need the *old, real, non-aliased* `kotlinx.datetime.Instant`/`Clock` classes for binary compatibility with code compiled against 0.6.x. Taking plain 0.8.0 (not `-compat`) means source compiles fine (typealias covers it) but any **downstream consumer** still holding bytecode compiled against the 0.6.x real classes will fail to link — which is exactly the breaking-for-consumers change `PROJECT.md` already documents as a conscious choice (`.planning/PROJECT.md:76`), not something this research needs to re-flag as a surprise. **Confidence: MEDIUM-HIGH** on "zero library source changes needed" — verify by compiling, since this reasoning rests on the typealias mechanism persisting unchanged from 0.7.1 through 0.8.0, which was not independently confirmed against the 0.8.0 release notes specifically (only against the aggregate 0.6.2→0.8.0 changelog summary). The actual gate is the same one the milestone already mandates: full compile + 467 tests green.

## Anti-Patterns to Avoid

### Applying Hot Reload at the root instead of in `:showcase`
**What people do:** Follow JetBrains' `apply false`-at-root doc pattern literally, adding a `plugins { alias(...) apply false }` block to the currently plugin-free root `build.gradle.kts`.
**Why it's wrong here:** It breaks the repo's own established convention ("Root build — no plugins applied here") for zero benefit in a 2-module tree where only one module ever needs the plugin, and it creates a new precedent that the next contributor might assume means "shared plugins go in root" — which isn't true here.
**Instead:** Apply directly in `showcase/build.gradle.kts`'s existing `plugins{}` block, matching how `compose.desktop.application`-specific things already work.

### Removing the JetBrains Space repo blind
**What people do:** See the redirect, assume "dead repo, delete it," delete without testing.
**Why it's wrong:** The redirect target is live and Gradle follows it — removing it is very likely safe but has not been empirically verified against *this* project's exact dependency graph (only against JetBrains' own template).
**Instead:** Remove, then run one `--refresh-dependencies` build before trusting it — this is a one-line-diff, low-cost, high-value check.

### Enabling configuration-cache while chasing MCP stdio cleanliness
**What people do:** See CC's performance benefits and turn it on "while we're in here."
**Why it's wrong:** It's an entirely separate noisy-output category with only an undocumented/internal quiet flag, and it's out of this milestone's scope (dependency bumps + Hot Reload, not build performance).
**Instead:** Leave `gradle.properties` without `org.gradle.configuration-cache=true` for this milestone; revisit separately if desired later.

### Splitting the library-set bump (coroutines/datetime/JUnit) into three phases
**What people do:** Apply the "isolate risk" lesson too literally and give each dependency its own phase.
**Why it's wrong:** These three touch disjoint code paths; a test failure already identifies which one broke regardless of whether they land together. Splitting adds phase-transition overhead the "keep it SMALL" goal explicitly argues against.
**Instead:** Bundle them into one step, gated by the full 467-test suite.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---|---|---|
| JitPack | `jitpack.yml` sets JDK before `./gradlew` runs; publish is triggered by pushing a `vX.Y.Z` git tag | Needs JDK 21 + Gradle 9.7.1 wrapper both landed before tagging `v3.1.0` |
| Gradle Plugin Portal / Maven Central / Google | `settings.gradle.kts` `pluginManagement`/`dependencyResolutionManagement` | Sufficient alone for stable CMP 1.12.0 per JetBrains' own current desktop template — the JetBrains Space/Team repo is likely removable |
| `packages.jetbrains.team` (redirect target of the old Space URL) | Only reached if the old URL is kept | Verified live via direct fetch (301 redirect), 2026-09-21 |
| Claude Code (MCP client) | `.mcp.json` at repo root, stdio transport, `cmd /c gradlew.bat ...` | No `cwd` field needed — project-root `.mcp.json` runs from the project directory automatically |

### Internal Boundaries

| Boundary | Communication | Notes |
|---|---|---|
| `:library` ↔ `:showcase` | `implementation(project(":library"))` (`showcase/build.gradle.kts:14`) | One-directional; `:library` has zero awareness of `:showcase`, which is exactly why Hot Reload isolation is structurally guaranteed, not just conventionally maintained |
| `:library` ↔ root `build.gradle.kts` | `allprojects { group; version }` only | The only root-level thing that touches `:library` at all — confirms the isolation claim above |
| `showcase/build.gradle.kts`'s `run` task ↔ `ComposeHotRun` task(s) | Both read `-Paero.scheme` via separate `tasks.withType<T>()` blocks | Two parallel forwarding blocks, not one shared one, because the task types don't share a common supertype exposing `systemProperty` |

## Sources

- JetBrains/compose-hot-reload README (fetched via raw.githubusercontent.com, 2026-09-21) — plugin id, version-catalog snippet, MCP task/config shape, JBR requirement — MEDIUM-HIGH (fetched directly, but via an intermediate summarization step)
- WebSearch: Compose Hot Reload 1.2.0 MCP server task naming (`hotMcpServer`, `hotMcpServerJvm`, `hotRunJvm` vs `hotRun`) — MEDIUM, cross-referenced across 2 independent searches, not a primary-source enumeration
- Compose Multiplatform 1.12.0 release (blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/, fetched) — MCP server introduction, Hot Reload 1.2.0 bundling — HIGH
- `https://maven.pkg.jetbrains.space/public/p/compose/dev/` — direct fetch, 2026-09-21 — confirmed HTTP 301 → `https://packages.jetbrains.team/maven/p/cmp/dev/` — HIGH
- `JetBrains/compose-multiplatform-desktop-template` `settings.gradle.kts` (raw fetch) — confirms `gradlePluginPortal()` + `mavenCentral()` alone suffice for a stable-release desktop project — HIGH
- Gradle 9 upgrade guide (`docs.gradle.org/9.7.1/userguide/upgrading_version_9.html`, fetched) — breaking changes enumerated, checked against every construct actually present in this repo's build files — HIGH
- `gradle/gradle` GitHub issue #1845 ("combined --console plain --quiet is no more silent") and #5213 ("Welcome message doesn't check quiet flag") — via WebSearch — MEDIUM-HIGH (issue titles/summaries, not full issue threads read)
- `gradle/foojay-toolchains` README + `facebook/react-native` issue #56287 (foojay-resolver-convention 0.5.0 incompatible with Gradle 9.0.0) — via WebSearch — MEDIUM-HIGH
- `Kotlin/kotlinx-datetime` CHANGELOG.md (fetched) — 0.7.0 removal + 0.7.1 typealias reintroduction of `Instant`/`Clock` — HIGH
- JUnit 6 release notes / "Upgrading to JUnit 6.0" wiki — via WebSearch — minimum Java 17, minimum Kotlin 2.2, unified platform/jupiter versioning — MEDIUM-HIGH
- Claude Code MCP docs (`code.claude.com/docs/en/mcp`, fetched via redirect) — `.mcp.json` schema, Windows `cmd /c` pattern, stdout-must-be-clean-JSON-RPC requirement — HIGH
- Direct repository reads: `PROJECT.md`, `gradle/libs.versions.toml`, `build.gradle.kts`, `library/build.gradle.kts`, `showcase/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `jitpack.yml`, `README.md`, `gradle/wrapper/gradle-wrapper.properties`, `Main.kt`, plus grep of `library/src` and `showcase/src` for `kotlinx.datetime.*` symbols — HIGH (primary source, this repo)

---
*Architecture research for: v3.1 Dependency Refresh + Hot Reload MCP integration, `aero-compose-ui`*
*Researched: 2026-09-21*
