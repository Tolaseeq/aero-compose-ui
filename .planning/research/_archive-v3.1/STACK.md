# Stack Research: v3.1 Dependency Refresh + Hot Reload MCP

**Domain:** Forced toolchain/dependency refresh + Compose Hot Reload MCP server setup for an existing Compose Desktop (JVM-only, `org.jetbrains.kotlin.jvm` + `org.jetbrains.compose`, NOT the `org.jetbrains.kotlin.multiplatform` plugin) library + showcase app.
**Researched:** 2026-09-21
**Confidence:** MEDIUM-HIGH overall (every version number CONFIRMED against Maven Central `maven-metadata.xml` / `services.gradle.org`; the Hot Reload + plain-JVM interaction is CONFIRMED via the primary GitHub README but contradicts secondary doc-summary tools, flagged below; a small number of items remain genuinely UNVERIFIED and are called out explicitly, per "never fill a gap by guessing")

This file answers only the six questions in the research brief. It does not re-litigate the already-decided target versions (see `.planning/PROJECT.md` "Current Milestone" section) — it verifies them, states integration points, and flags what could not be confirmed.

---

## 1. Is the full target version set mutually compatible?

**Kotlin 2.4.20 × CMP 1.12.0 × Compose compiler plugin × Gradle 9.7.1 × JDK 21 × Hot Reload 1.2.0**

| Pair | Verdict | Evidence |
|------|---------|----------|
| Kotlin 2.4.20 exists, is the latest stable | CONFIRMED | Maven Central `kotlin-gradle-plugin/maven-metadata.xml`: `<release>2.4.20</release>`, no newer non-RC version. |
| CMP 1.12.0 exists, is the latest stable | CONFIRMED | Maven Central `org.jetbrains.compose.gradle.plugin/maven-metadata.xml`: `<release>1.12.0</release>`; next is `1.13.0-alpha01`. Released August 2026 per [JetBrains blog](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/). |
| CMP 1.12.0 requires which Kotlin? | CONFIRMED | kotlinlang.org [Compatibility and versions](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html): "use at least Kotlin 2.1.0 for your projects" (2.2.20+ only needed for iOS/web/rapidly-evolving targets — N/A, this project is JVM-only desktop). Kotlin 2.4.20 clears this with wide margin. Also: "The latest Compose Multiplatform is always compatible with the latest version of Kotlin." |
| Compose compiler plugin version | CONFIRMED | `org.jetbrains.kotlin.plugin.compose` ships inside the Kotlin distribution and is always version-pinned to the Kotlin version (`version.ref = "kotlin"` in the existing `libs.versions.toml` — this pattern is unchanged and correct for 2.4.20; no separate version to track). |
| Which Gradle versions does Kotlin Gradle Plugin 2.4.20 officially support — is 9.7.1 inside the range, or only a warning zone? | **CONFIRMED, with a caveat: 9.7.1 is technically outside the "fully compatible" ceiling.** | kotlinlang.org [What's new in 2.4.20](https://kotlinlang.org/docs/whatsnew2420.html): "Kotlin 2.4.20 is fully compatible with Gradle 7.6.3 through **9.7.0**." It adds: "You can also use Gradle versions up to the latest Gradle release... [but] doing so may result in deprecation warnings, and some new Gradle features might not work." Target Gradle is **9.7.1** — one patch release above the documented "fully compatible" ceiling (9.7.0), i.e. it falls in the "extended/warned" zone per Kotlin's own docs, not a hard incompatibility. Kotlin 2.4.20 release notes separately state it adds "support for Gradle 9.7.0" as a KGP feature target, which is consistent (9.7.1 is a patch of 9.7.0, extremely low risk of an actual break, but this is the one place the target set is not 100% inside JetBrains' officially-tested matrix). |
| Does the Compose Gradle plugin 1.12.0 work on Gradle 9.x? | UNCONFIRMED as an explicit documented minimum, but MEDIUM-HIGH confidence it works | No page states an explicit Gradle floor for the CMP Gradle plugin; it is "built on top of the Kotlin Multiplatform Gradle plugin" per kotlinlang.org, so it inherits KGP's Gradle range. Since KGP 2.4.20 is tested through 9.7.0 and CMP 1.12.0 requires Kotlin ≥2.1.0 (i.e. it doesn't pin an older, stricter Gradle ceiling than KGP itself), there is no documented reason 1.12.0 would reject Gradle 9.7.1. This is an inference, not a quoted compatibility statement — treat as needing a real `./gradlew build` smoke test in Phase 1 of this milestone, not as pre-verified fact. |
| Gradle 9.7.1 exists, is current stable | CONFIRMED | `services.gradle.org/versions/current` → `{"version":"9.7.1", "current":true, "released":true}`. 9.8.0 exists only as RC (`9.8.0-rc-2`), not yet stable. |
| Gradle 9.7.1 on JDK 21 (both to run the Gradle daemon and as a toolchain target) | CONFIRMED | [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html): JDK 17–26 required to *run* Gradle 9.x (21 is inside range); JDK 21 has had toolchain-target support since Gradle 8.4. |
| Hot Reload 1.2.0 is the latest stable (1.3.0-alpha02 is not) | CONFIRMED | Maven Central `hot-reload-core` / `org.jetbrains.compose.hot-reload.gradle.plugin` metadata: versions after `1.2.0` are `1.3.0-alpha01`, `1.3.0-alpha02` — both pre-release. `1.2.0` released 2026-07-23 per `gh release view v1.2.0 --repo JetBrains/compose-hot-reload`. |
| Hot Reload 1.2.0 × CMP 1.12.0 | CONFIRMED, and CMP 1.12.0 bundles it | `compose-jb` CHANGELOG.md, "1.12.0" section: "Updated bundled Compose Hot Reload to version 1.2.0." Hot Reload's own prerequisite ("Compose Multiplatform 1.8.2 or higher; CHR 1.2.0-alpha02+ requires 1.10.0+") is satisfied by 1.12.0. |

**Overall verdict for Q1:** The version set is mutually compatible. The one asterisk: Gradle 9.7.1 sits one patch above Kotlin 2.4.20's documented "fully compatible" ceiling (9.7.0) — not a blocker, but not inside the exact tested matrix either. No other pairwise conflict was found in official sources.

---

## 2. Material3 — is there a newer stable than 1.9.0? What does `compose.material3` resolve to on CMP 1.12.0?

| Question | Verdict | Evidence |
|----------|---------|----------|
| Newer stable `org.jetbrains.compose.material3:material3` than 1.9.0? | **CONFIRMED: No.** | Maven Central `material3/maven-metadata.xml` full version list: after `1.9.0` every subsequent entry is alpha — `1.10.0-alpha01..05`, `1.11.0-alpha01..07`, `1.12.0-alpha01..03`, `1.13.0-alpha01`. `<latest>1.13.0-alpha01</latest>` is itself an alpha. The existing pin to `1.9.0` remains correct and must stay unchanged — this validates the maintainer's decision rather than overriding it. |
| What does `compose.material3` (the CMP alias, not the explicit coordinate) resolve to on CMP 1.12.0? | CONFIRMED | `compose-jb` CHANGELOG.md, "1.12.0 — Component Versions": **"Material3: 1.12.0-alpha03 (based on Jetpack 1.5.0-alpha22)"**. This reconfirms the exact failure mode already known from CMP 1.11.x: the `compose.material3` alias silently resolves to an alpha. The explicit `api("org.jetbrains.compose.material3:material3:1.9.0")` override in `library/build.gradle.kts` and the plain-string dependency in `showcase/build.gradle.kts` must both be kept exactly as-is — do not switch either module to the `compose.material3` alias. |
| Other CMP-adjacent Material3 coordinates the pin affects (adaptive, etc.) | N/A — not used | Grep of `library/build.gradle.kts` / `showcase/build.gradle.kts` shows no `material3-adaptive`, `material3-window-size-class`, or similar dependency. Nothing else to pin or check. |

---

## 3. Compose Hot Reload 1.2.0 — coordinates, bundling, plain-JVM support, task names, MCP requirements

**Plugin ID / coordinates — CONFIRMED**
- Gradle plugin ID: `org.jetbrains.compose.hot-reload`
- Version catalog entry (per the project's `[plugins]` convention in `libs.versions.toml`):
  ```toml
  composeHotReload = { id = "org.jetbrains.compose.hot-reload", version = "1.2.0" }
  ```
- Backing artifact group: `org.jetbrains.compose.hot-reload` (e.g. `hot-reload-core`), confirmed present on Maven Central at `1.2.0`.

**Bundled by CMP 1.12.0, but "apply explicitly" is the safe path for this project — nuanced, partially UNCONFIRMED**
- kotlinlang.org: *"Starting with Compose Multiplatform 1.10.0, the Compose Hot Reload plugin is bundled and enabled by default for all projects that include a desktop target."* This wording ("desktop target", "Kotlin Multiplatform quickstart... select the desktop target") is written for `kotlin("multiplatform") { jvm() }`-style projects.
- **UNCONFIRMED for this project's actual shape:** `:showcase` uses the plain `org.jetbrains.kotlin.jvm` plugin (not `kotlin("multiplatform")`), so it has no KMP "target" in the sense the bundling language describes. No source found states explicitly whether the "bundled and enabled by default" auto-activation extends to plain-JVM modules, or whether it only auto-activates for true multiplatform modules and a plain-JVM module needs the plugin applied by hand.
- **Recommendation (mitigates the unknown either way):** apply `alias(libs.plugins.composeHotReload)` explicitly in `showcase/build.gradle.kts`'s `plugins {}` block, pinned to `1.2.0`. This is documented, safe, and identical whether or not the implicit bundling would also have worked — costs one line, removes the ambiguity.

**Works with the plain `org.jetbrains.kotlin.jvm` plugin (not KMP) — CONFIRMED, directly from the primary source**
- The GitHub README (`https://github.com/JetBrains/compose-hot-reload`, read verbatim via `raw.githubusercontent.com`, not summarized) states explicitly, in the "Run tasks" section:
  > `:hotRunJvm`: For multiplatform projects. The async alternative is `:hotRunJvmAsync`.
  > `:hotRun`: **For Kotlin/JVM projects.** The async alternative is `:hotRunAsync`.
- And in the MCP server section: *"In a **plain Kotlin/JVM module** the task is simply named `hotMcpServer`."*
- This directly answers and resolves the milestone's open question. **Flag:** two independent WebFetch-summarized passes over the kotlinlang.org docs page and the official "Quickstart" page returned the opposite claim ("plain `org.jetbrains.kotlin.jvm` is NOT supported / requires conversion to multiplatform"), and the two official *sample projects* in the repo (`samples/counter`, `samples/bytecode-analyzer`) both use `kotlin("multiplatform") { jvm() }`, not plain `kotlin("jvm")` — i.e. JetBrains' own demos don't showcase the plain-JVM path even though the README documents it as a first-class, named case. Treat the README's explicit `:hotRun` / `hotMcpServer` naming as the authoritative, CONFIRMED fact (it is precise, internally consistent with the MCP section, and version-controlled alongside the plugin's actual source); treat the "must be multiplatform" framing elsewhere as either stale, or referring to the top-level "Compose Hot Reload needs a JVM target in your multiplatform project" caveat that is really about *target platform* (JVM vs Native/Wasm), not about the *Gradle plugin structure* (KMP vs. plain-JVM). Recommend a fast empirical check at the start of Phase work: run `./gradlew :showcase:tasks --all | grep -i hot` after applying the plugin, before assuming task names.

**Exact Gradle task names for a Kotlin/JVM module (this project's `:showcase`) — CONFIRMED**
| Task | Purpose |
|------|---------|
| `:showcase:hotRun` (or unqualified `hotRun`) | Launch the app with hot reload |
| `:showcase:hotRunAsync` | Async/non-blocking variant |
| `:showcase:reload` | Trigger a reload of the already-running app |
| `:showcase:hotMcpServer` (or unqualified `hotMcpServer`) | Start the MCP server |

(Multiplatform-only names `hotRunJvm` / `hotRunJvmAsync` / `hotMcpServerJvm` do **not** apply here since `:showcase` is plain Kotlin/JVM — confirmed from the same README table.)

**Main class wiring** — no extra config needed. The README shows the Hot Reload task picks up the main class from either `tasks.withType<ComposeHotRun>().configureEach { mainClass.set(...) }` **or**, if using the Compose Gradle plugin's `application {}` DSL (which `showcase/build.gradle.kts` already uses: `compose.desktop.application { mainClass = "com.mordred.showcase.MainKt" }`), that existing configuration is reused automatically.

**MCP server requirements — CONFIRMED**
- Introduced in Hot Reload `1.2.0-alpha01`, stable/GA in `1.2.0` (per GitHub release notes, `gh release view v1.2.0`).
- Requires CMP ≥ 1.12.0 for the "MCP server for AI agents" feature to be documented/available (kotlinlang.org "What's new in Compose Multiplatform 1.12.0" names this as the headline Desktop feature of 1.12.0). Target CMP 1.12.0 satisfies this exactly.
- `.mcp.json` wiring (matches the milestone's own plan, and is the officially documented pattern):
  ```json
  {
    "mcpServers": {
      "compose-hot-reload": {
        "command": "./gradlew",
        "args": ["--no-daemon", "--quiet", "--console=plain", "hotMcpServer"]
      }
    }
  }
  ```
  On Windows, `./gradlew` frequently fails to spawn directly from an MCP client process launcher (shell-association issue) — the milestone plan's own note to use `cmd /c gradlew.bat --no-daemon --quiet --console=plain hotMcpServer` is the standard workaround; this is a Windows-process-spawning fact, not something JetBrains' docs cover, but it's consistent with common Windows MCP-server wiring practice — LOW confidence as a citation, HIGH confidence as a practical necessity (should be validated empirically in Phase work, which the milestone plan already schedules).
- MCP tool surface (confirmed from `gh release view v1.2.0`): `status`, `reload`, `await_reload`, `restart`, `reset_ui`, `take_screenshot`, `list_windows`, `resize_window`, `get_semantic_tree`, `get_ui_error`, `get_logs`, `click`, `type_text`, `scroll`.

---

## 4. JetBrains Runtime (JBR)

| Question | Verdict | Evidence |
|----------|---------|----------|
| Which JBR version is required? | **CONFLICTING between two official sources — flagged, not fully resolved** | kotlinlang.org compose-hot-reload docs: *"The latest JetBrains Runtime supports only Java 21: if you add Compose Hot Reload to a project that is only compatible with Java 22 or newer, running the project results in a linkage error."* But the **Hot Reload 1.2.0 GitHub release notes** (`gh release view v1.2.0`, primary source, dated 2026-07-23) state: *"☕ JBR 25 by default. Use the `compose.reload.jbr.min.version` property to configure the minimum supported JBR version."* |
| How to reconcile | UNVERIFIED interpretation, flagged as such | Most likely reading: "JBR 25" is JBR's own major version number (an OpenJDK-25-based JBR build); the "supports only Java 21" warning is about the **project's own compile/bytecode target**, not the JBR build number — i.e. a JBR-25-based runtime can still execute and hot-swap a project whose Kotlin `jvmToolchain` / bytecode target is 21 (this project's plan), but breaks if the project's own target is ≥22. Since this milestone sets `jvmToolchain(21)` project-wide, this reading predicts no linkage error. **This interpretation is not confirmed by an explicit JetBrains statement reconciling the two texts — do not treat it as fact until validated empirically** (first `hotRun`/`hotMcpServer` invocation in Phase work will prove or disprove it directly). |
| How does Hot Reload locate/provision JBR? | CONFIRMED, three documented mechanisms | (1) Reuse IntelliJ's bundled JBR via the Kotlin Multiplatform IDE plugin (not applicable — this workflow is Gradle/CLI-driven, no IDE run config involved). (2) `org.gradle.toolchains.foojay-resolver-convention` Gradle settings-plugin for automatic download — **this is the relevant mechanism for a headless/CLI/MCP workflow**. (3) `compose.reload.jbr.autoProvisioningEnabled` Gradle property (README calls this out as an alternative/experimental path). |
| `foojay-resolver-convention` — current version, Gradle-9-compatible? | **CONFIRMED, and this is a real, must-not-skip pitfall.** | Maven Central: latest/only-recent release is `1.0.0` (unchanged since May 2025 per `lastUpdated`). Web search of `gradle/foojay-toolchains` issue tracker confirms: foojay-resolver-convention **0.5.0 is broken on Gradle 9.0.0** (`FoojayToolchainsPlugin` class removed; `JvmVendorSpec.IBM_SEMERU` constant removed) — two real-world reports (`facebook/react-native` issues #56287, #55781). **Versions ≥0.8.0 fix this; 1.0.0 is confirmed compatible** and is also the exact snippet shown in the Hot Reload README itself: `id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"`. **Action: if `settings.gradle.kts` does not already declare this plugin, it must be added at exactly `1.0.0` — any pre-1.0.0 pin (e.g. a stale `0.5.0`/`0.8.0` copied from an older tutorial) will break on Gradle 9.7.1.** The current `settings.gradle.kts` does not declare this plugin at all yet — confirmed by reading the file directly. |
| `compose.reload.jbr.*` properties | CONFIRMED to exist, not fully documented in one place | `compose.reload.jbr.autoProvisioningEnabled` (README) and `compose.reload.jbr.min.version` (v1.2.0 release notes) are both real Gradle properties. No single page enumerates the full property list — treat any property beyond these two as UNVERIFIED until found in an actual source. |
| What must be installed on Windows | CONFIRMED (nothing manual, if the resolver plugin is present) | With `foojay-resolver-convention` 1.0.0 declared, Gradle auto-downloads a matching JBR the same way it auto-downloads any other toolchain — no manual JBR installer is documented as required on Windows specifically. This is consistent with how the project's existing `jvmToolchain(17)` already presumably resolves via Gradle's toolchain machinery today (not JBR-specific, but the same resolution pattern). |

---

## 5. JitPack on JDK 21 / Gradle 9.x

| Question | Verdict | Evidence |
|----------|---------|----------|
| `jitpack.yml`: `jdk: openjdk21` valid? | CONFIRMED | JitPack's own docs (`docs.jitpack.io`, "Building" page): *"For modern JVM and Android projects (such as Gradle 7+, Gradle 8+, Spring Boot 3+, or AGP 8+), you should specify a newer LTS version (e.g. Java 17 or Java 21)."* A real, in-production example (`polarofficial/polar-ble-sdk/blob/master/jitpack.yml`) uses `jdk: - openjdk21` verbatim. |
| SDKMAN identifier for current Temurin 21 | CONFIRMED, exact value | Queried `api.sdkman.io/2/candidates/java/linuxx64/versions/list` live (2026-09-21): the only currently-listed Temurin 21 build is **`21.0.12+1.1-tem`**. This is a moving target (SDKMAN updates as Temurin ships patches) — re-verify at implementation time the same way the existing `jitpack.yml` pins an exact patch (`17.0.10-tem`) rather than a floating `21-tem`. Recommended `jitpack.yml`: `jdk: - openjdk21` plus `before_install: sdk install java 21.0.12+1.1-tem` / `sdk use java 21.0.12+1.1-tem`, mirroring the existing file's exact-pin style. JitPack's own docs separately show a generic `sdk install java 21-open` (OpenJDK, not Temurin) example — either vendor works for the JVM itself; Temurin keeps parity with the project's existing choice. |
| Does JitPack's build image handle Gradle 9.x? | UNCONFIRMED via docs, MEDIUM-confidence inference | JitPack docs: *"if your project isn't using a Gradle wrapper, JitPack will build it with a default version of Gradle... strongly recommended to use the Gradle wrapper."* The project already uses `./gradlew` (confirmed — `gradlew`/wrapper present), so JitPack should simply invoke the project's own wrapper, which will download Gradle 9.7.1 itself at build time (assuming JitPack's build sandbox allows outbound network access to `services.gradle.org`, which it must, since it already builds this project successfully on the wrapper today). No JitPack doc explicitly confirms Gradle 9.x support or denies it — **this needs the project's own existing practice of proving JitPack builds with throwaway tags (as done in Phase 15 of v3.0, per `.planning/PROJECT.md`) rather than trusting docs.** |

---

## 6. JUnit 6.1.3

| Question | Verdict | Evidence |
|----------|---------|----------|
| Coordinates unchanged? | CONFIRMED | `org.junit.jupiter:junit-jupiter`, `org.junit.platform:junit-platform-launcher`, and `org.junit:junit-bom` all still exist under the same groupId/artifactId as JUnit 5 — JUnit 6 did **not** rename the Maven coordinates. Confirmed directly via `maven-metadata.xml` for all three: `junit-jupiter`, `junit-platform-launcher`, and `junit-bom` all list `6.1.3` as their latest version, with fully parallel version histories (JUnit unified Jupiter/Platform/Vintage versioning as of 6.0 — this is why `junit-platform-launcher` and `junit-jupiter` now share the exact same version number, unlike the JUnit 5 era where Platform trailed Jupiter's numbering). |
| `junit-platform-launcher` alignment | CONFIRMED — use `6.1.3` explicitly, or rely on the JUnit BOM | Since Platform and Jupiter versions are now unified at `6.1.3`, the existing pattern (`testRuntimeOnly("org.junit.platform:junit-platform-launcher")` with no explicit version, letting Gradle's built-in JUnit-Platform-launcher auto-resolution apply) continues to work, but **only if `junit.jupiter` is bumped to a version whose corresponding launcher exists** — trivially true at `6.1.3`/`6.1.3`. No BOM is currently used in `library/build.gradle.kts`; none is required to add — the version-catalog single `version.ref = "junit"` pattern already in place (`junit = "5.10.0"` → `junit = "6.1.3"`) is sufficient. |
| Minimum Java | CONFIRMED | Multiple corroborating sources (JUnit user guide release notes, InfoQ, multiple dev blogs covering the 2025-09-30 JUnit 6.0.0 release): **JUnit 6 requires Java 17 minimum** (raised from Java 8). JDK 21 (target) clears this. |
| `kotlin-test` / `kotlin-test-junit5` compatibility with JUnit 6 | MEDIUM confidence, consistent across sources, not from a single canonical page | Search-aggregated finding: *"For Kotlin users, JUnit 6 now requires Kotlin 2.2 or later"* and *"JUnit 6 requires Java 17 and Kotlin 2.2 as the minimum language levels."* Target Kotlin is 2.4.20, well above this floor. The project's existing `kotlin-test` dependency (generic, framework-agnostic assertion library, not `kotlin-test-junit5` specifically) has no JUnit-major-version coupling — it does not wrap JUnit's runner API, so no change is needed there. If a dedicated JUnit5-integration artifact is ever wanted, it is `org.jetbrains.kotlin:kotlin-test-junit5` (not currently used by this project) — out of scope, not needed. |
| Does Compose `uiTest` (which historically pulls JUnit4) coexist with JUnit 6? | **CONFIRMED via the project's own existing codebase, not just docs** | kotlinlang.org's Compose testing docs distinguish two separate artifacts: `compose.uiTest` (the multiplatform-common API, exposing `runComposeUiTest` — a plain Kotlin function, **not** a JUnit4 `TestRule`, and not coupled to any specific JUnit major version) vs. the separate, opt-in `compose.desktop.uiTestJUnit4` artifact (only pulled in if you explicitly want the JUnit4-`TestRule`-style API, which this project does not use). The project's `library/build.gradle.kts` already uses `testImplementation(compose.uiTest)` (not `uiTestJUnit4`) together with `junit-jupiter` and 467 green tests today on JUnit 5.10.0, including `runComposeUiTest`-based tests (the RCMP drag regression guard, per `.planning/PROJECT.md`). Since `compose.uiTest` itself carries no JUnit-runner dependency, this pattern has no structural reason to break under JUnit 6 — it should be a drop-in version bump. Recommend re-running the full suite as direct proof rather than trusting this reasoning alone (the milestone plan already schedules this). |

---

## Integration Points — Exact Files to Change

| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Bump `kotlin = "2.4.20"`, `composeMultiplatform = "1.12.0"`, `kotlinxCoroutines = "1.11.0"`, `junit = "6.1.3"`, `kotlinxDatetime = "0.8.0"`. Add `composeHotReload = "1.2.0"` version + a `[plugins]` entry `compose-hot-reload = { id = "org.jetbrains.compose.hot-reload", version.ref = "composeHotReload" }`. |
| `library/build.gradle.kts` | `kotlin { jvmToolchain(21) }`. Material3 coordinate string stays `"org.jetbrains.compose.material3:material3:1.9.0"` unchanged. No Hot Reload plugin here (library is not run, and must not carry the dev-only Hot Reload plugin into the published JAR). |
| `showcase/build.gradle.kts` | `kotlin { jvmToolchain(21) }`. Add `alias(libs.plugins.compose.hot.reload)` to the `plugins {}` block (see Q3 recommendation — apply explicitly rather than rely on undocumented plain-JVM auto-bundling). Material3 coordinate string stays `1.9.0`. |
| `settings.gradle.kts` | Add, in `pluginManagement { }` or top-level `plugins { }` as appropriate: `id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"` — **required** for automatic JBR provisioning on Gradle 9.7.1 (see Q4; a pre-1.0.0 pin is a confirmed Gradle-9 breakage). |
| `gradle/wrapper/gradle-wrapper.properties` | Bump distribution URL to `gradle-9.7.1-bin.zip` (or `-all.zip` if the wrapper currently uses `-all`; check the existing file — not read in this research pass, verify before editing). |
| `jitpack.yml` | `jdk: - openjdk21`; `before_install: sdk install java 21.0.12+1.1-tem` / `sdk use java 21.0.12+1.1-tem` (re-verify exact SDKMAN identifier at implementation time — it changes as Temurin ships patches). |
| `.mcp.json` (new, repo root) | `{"mcpServers": {"compose-hot-reload": {"command": "cmd", "args": ["/c", "gradlew.bat", "--no-daemon", "--quiet", "--console=plain", "hotMcpServer"]}}}` — the `cmd /c gradlew.bat` wrapping is the Windows-specific spawning fix noted in the milestone plan; validate empirically. |
| `library/src/main/kotlin/.../pickers/*.kt` | **Real source-code migration required, not just a version bump.** kotlinx-datetime 0.7.0 (carried into 0.8.0) renamed `LocalDate.dayOfMonth` → `day` and `monthNumber` → `month`, and removed `kotlinx.datetime.Instant`/`kotlinx.datetime.Clock` in favor of `kotlin.time.Instant`/`Clock`. Grep of the current codebase confirms **4 files** use `dayOfMonth`/`monthNumber` (`AeroDateTimePicker.kt`, `AeroDatePicker.kt`, `AeroCalendarGrid.kt`, and its test) and **4 files** reference `kotlinx.datetime.Instant`/`Clock` (`AeroDateTimeRangePicker.kt`, `AeroDateTimePicker.kt`, `AeroDateRangePicker.kt`, `AeroDatePicker.kt`). These will not compile against plain `kotlinx-datetime:0.8.0` without source changes. (kotlinx-datetime CHANGELOG: 0.7.1 added transitional type aliases for `Instant`/`Clock` to ease migration, but the target here is 0.8.0 *plain*, not `-0.6.x-compat` — whether those aliases persisted into 0.8.0 plain is UNVERIFIED from the changelog text alone; treat the 4 `Instant`/`Clock` files as needing an actual compile check, not just the 4 `dayOfMonth`/`monthNumber` files which are certainly broken.) |

---

## What NOT to Add

| Avoid | Why |
|-------|-----|
| `compose.material3` alias (in place of the explicit `org.jetbrains.compose.material3:material3:1.9.0` coordinate) | Resolves to `1.12.0-alpha03` on CMP 1.12.0 (CONFIRMED, see Q2) — same trap the project already worked around for 1.11.x. |
| `kotlinx-datetime:0.8.0-0.6.x-compat` (or any `-0.6.x-compat` suffixed variant) | Milestone target is explicitly the plain `0.8.0` artifact; the compat variant exists specifically to *avoid* the `Instant`/`Clock`/`dayOfMonth`/`monthNumber` breaking changes, which defeats the purpose of "жёсткое обновление" (forced refresh) the milestone calls for. |
| `foojay-resolver-convention` below `1.0.0` (e.g. a tutorial-copied `0.5.0`) | CONFIRMED broken on Gradle 9.0.0 (`IBM_SEMERU` constant removed, `FoojayToolchainsPlugin` class removed) — real regressions reported against `facebook/react-native`. |
| Hot Reload `1.3.0-alpha02` (or any 1.3.0-alpha) | Not stable; milestone explicitly targets `1.2.0`, the latest stable. |
| `compose.desktop.uiTestJUnit4` | Not needed — the project already uses the JUnit-agnostic `compose.uiTest` (`runComposeUiTest`) and should keep doing so; adding the JUnit4-`TestRule` artifact would reintroduce the exact transitive-JUnit4 coexistence risk the research brief asked about avoiding. |
| A JUnit BOM (`org.junit:junit-bom`) | Not necessary — the project's existing single-`version.ref` catalog pattern already keeps `junit-jupiter` and `junit-platform-launcher` in lockstep now that JUnit unified their versioning at 6.x; adding a BOM is a valid alternative but not a requirement, and is out of the stated scope ("what's needed for the NEW work" only). |
| `kotlin-test-junit5` | Not currently used and not required — the project's `kotlin-test` dependency is JUnit-version-agnostic; do not add a JUnit5-specific integration artifact that isn't already part of the dependency graph. |
| Anything from CMP 1.12.0's other headline features (web `ComposeViewportConfiguration`, `LayerOutsets`/`GraphicsLayer` additions, `PopupProperties.blockPointerInputOutside`) | Out of scope — this milestone is a version bump + Hot Reload MCP setup only, not an adoption of new CMP 1.12.0 APIs. |

---

## Sources

- Maven Central `maven-metadata.xml` (queried directly via `repo1.maven.org`, authoritative, dated 2026-09-21 snapshot): `kotlin-gradle-plugin`, `org.jetbrains.compose.gradle.plugin`, `org.jetbrains.compose.material3:material3`, `org.jetbrains.kotlinx:kotlinx-coroutines-core`, `org.jetbrains.kotlinx:kotlinx-datetime`, `org.junit:junit-bom`, `org.junit.jupiter:junit-jupiter`, `org.junit.platform:junit-platform-launcher`, `org.jetbrains.compose.hot-reload:hot-reload-core` and its gradle-plugin marker, `org.gradle.toolchains.foojay-resolver-convention` gradle-plugin marker (via `plugins.gradle.org/m2`).
- `services.gradle.org/versions/current` and `/versions/all` — Gradle 9.7.1 confirmed current stable; 9.8.0 confirmed RC-only.
- `gh release view v1.2.0 --repo JetBrains/compose-hot-reload` — primary-source release notes (JBR 25 default, MCP server tool list, `compose.reload.jbr.min.version`).
- `https://raw.githubusercontent.com/JetBrains/compose-hot-reload/master/README.md` — read directly (not LLM-summarized), primary source for plain-Kotlin/JVM task naming (`:hotRun`, `:hotMcpServer`) and the `foojay-resolver-convention version "1.0.0"` setup snippet.
- `https://raw.githubusercontent.com/JetBrains/compose-jb/master/CHANGELOG.md` — CMP 1.12.0 section: bundled Hot Reload version, Material3 alias resolution (`1.12.0-alpha03`).
- `https://kotlinlang.org/docs/whatsnew2420.html` — Kotlin 2.4.20 Gradle compatibility range.
- `https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html` — CMP↔Kotlin minimum version statement.
- `https://kotlinlang.org/docs/multiplatform/compose-hot-reload.html` — MCP server section, JBR linkage-error warning (the source of the Q4 conflict with the GitHub release notes).
- `https://docs.gradle.org/current/userguide/compatibility.html` — JDK↔Gradle 9.x matrix.
- `https://docs.jitpack.io` / `https://jitpack.io/docs/BUILDING/` — `jdk:` config values, Gradle-wrapper handling statement.
- `https://github.com/polarofficial/polar-ble-sdk/blob/master/jitpack.yml` — real-world `jdk: - openjdk21` usage example.
- `api.sdkman.io/2/candidates/java/linuxx64/versions/list` — live query, exact current Temurin 21 identifier (`21.0.12+1.1-tem`).
- `https://github.com/gradle/foojay-toolchains/issues/151`, `facebook/react-native#56287`, `#55781` — foojay-resolver-convention pre-1.0.0 breakage on Gradle 9.0.0.
- `https://raw.githubusercontent.com/Kotlin/kotlinx-datetime/master/CHANGELOG.md` — 0.7.0/0.7.1/0.8.0 breaking changes (`Instant`/`Clock` removal, `dayOfMonth`→`day`, `monthNumber`→`month`).
- JUnit 6 minimum-Java findings cross-referenced across `docs.junit.org/6.0.3/release-notes.html`, InfoQ, and multiple independent 2025-10 coverage articles reporting the same Java-17 floor — treated as MEDIUM-HIGH confidence via multi-source agreement rather than a single quoted line.
- Local codebase (`Grep` against `library/src`) — confirmed the 4+4 files requiring real kotlinx-datetime 0.8.0 source migration (`dayOfMonth`/`monthNumber`, `kotlinx.datetime.Instant`/`Clock`), not found in any external doc but essential for the requirements/roadmap to plan real work, not just a `libs.versions.toml` edit.

---

*Stack research for: v3.1 Dependency Refresh + Hot Reload MCP*
*Researched: 2026-09-21*
