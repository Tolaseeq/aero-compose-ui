# Stack Research

**Domain:** Compose Desktop UI component library (Windows Aero / glassmorphism visual style)
**Researched:** 2026-04-27
**Confidence:** HIGH (primary sources: JetBrains official docs, GitHub releases, kotlinlang.org)

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | **2.3.21** | Language runtime | Latest stable as of April 2026. Required K2 compiler. Minimum for Compose MP 1.10.3 is 2.1.0; 2.3.21 is fully compatible and is the current stable. |
| Compose Multiplatform | **1.10.3** | UI framework, Compose Desktop engine | Latest stable (March 19, 2025). Bundles Skiko (Skia bindings), desktop rendering. Desktop target has been stable for years. Required for Haze 1.7.x. |
| Gradle | **9.4.1** | Build system | Latest stable (March 2026). Kotlin DSL is the mandatory choice — Groovy is deprecated for new projects. |
| JDK | **17** (min) / **21** (recommended) | JVM runtime | JDK 17 is the minimum for jpackage native distributions. JDK 21 is the current LTS and is fully supported. The PROJECT.md specifies JDK 17; that is the safe floor. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `org.jetbrains.compose.material3:material3` | **1.10.0-alpha05** (bundled with CMP 1.10.3) | Material Design 3 foundations — theming, state, accessibility | Always — wrap/extend, do not replace. Use `MaterialTheme` as the inner layer underneath `AeroTheme`. |
| `kotlinx-coroutines-core` | **1.10.2** | Async animation state, debounce, flow | Wherever animation tick or async state change needed (progress bar, date picker). Compatible with Kotlin 2.3.x. |
| Haze | **1.7.2** | GPU-accelerated background blur for glassmorphism | Required for the glass surface blur effect on Desktop/Skiko. Built on GraphicsLayer API. Supports Desktop JVM via Skiko. Single implementation across all CMP platforms. |
| `org.jetbrains.compose.ui:ui` | (bundled with CMP) | Core Compose UI primitives, `Canvas`, `Modifier`, `DrawScope` | Always — base of all rendering. |
| `org.jetbrains.compose.foundation:foundation` | (bundled with CMP) | `LazyColumn`, gestures, `BasicTextField`, scroll | Always — layout and interaction primitives. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle Kotlin DSL (`build.gradle.kts`) | Typed build scripts | Mandatory for this project; all IDE refactoring and completion works. Never use `.gradle` Groovy files. |
| `gradle/libs.versions.toml` (Version Catalog) | Centralized dependency versions | Standard practice as of Gradle 7.4+. Eliminates version string duplication. Use kebab-case keys. |
| `maven-publish` Gradle plugin | Publish JAR to `~/.m2` | Built into Gradle; no extra dependencies. Run `./gradlew publishToMavenLocal` to produce `com.mordred:aero-compose-ui:VERSION`. |
| Compose Hot Reload | Fast composable preview refresh | Bundled with CMP 1.10.0+. Requires Kotlin 2.1.20+. Use during development; disable in CI. |
| IntelliJ IDEA / Android Studio | IDE | IntelliJ IDEA Ultimate or Community with Kotlin plugin is preferred for Desktop-only work. Android Studio works but has heavier Android tooling. |

---

## Gradle Setup (Prescriptive)

### Project layout

```
aero-compose-ui/               ← root project
├── gradle/
│   └── libs.versions.toml    ← version catalog
├── settings.gradle.kts
├── build.gradle.kts           ← root build (repos, top-level config)
├── aero-ui/                   ← library module
│   └── build.gradle.kts
└── showcase/                  ← demo app module
    └── build.gradle.kts
```

### `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.3.21"
compose-multiplatform = "1.10.3"
coroutines = "1.10.2"
haze = "1.7.2"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
haze = { module = "dev.chrisbanes.haze:haze", version.ref = "haze" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-plugin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
```

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
        mavenLocal()   // for consuming the library in other projects
    }
}

rootProject.name = "aero-compose-ui"
include(":aero-ui", ":showcase")
```

### `aero-ui/build.gradle.kts` (library module)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
    `maven-publish`
}

group = "com.mordred"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.haze)
            }
        }
    }
}

publishing {
    publications {
        // KMP plugin auto-generates publications per target.
        // No manual publication block needed for Maven Local.
    }
}
```

### Publishing to Maven Local

```bash
./gradlew :aero-ui:publishToMavenLocal
```

Artifact lands at: `~/.m2/repository/com/mordred/aero-ui/0.1.0-SNAPSHOT/`

Consuming project adds to `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        // ...
    }
}
```
And declares:
```kotlin
implementation("com.mordred:aero-ui-desktop:0.1.0-SNAPSHOT")
```

---

## Installation

```bash
# No npm — pure Gradle. Initialize from scratch:
gradle init --type kotlin-multiplatform  # optional scaffold

# After configuring build files:
./gradlew build

# Publish library locally:
./gradlew :aero-ui:publishToMavenLocal

# Run showcase app:
./gradlew :showcase:run
```

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not Alternative |
|----------|-------------|-------------|---------------------|
| UI framework | Compose Multiplatform Desktop | JavaFX / TornadoFX | Dead community, no modern theming primitives, no Skia rendering |
| UI framework | Compose Multiplatform Desktop | Swing directly | No declarative composition, no state-driven rendering, no animation framework |
| Blur / glass | Haze 1.7.2 | Custom Skiko shader code | Haze already handles Desktop/Skiko correctly; custom shaders are fragile and require GLSL expertise |
| Blur / glass | Haze 1.7.2 | Cloudy library | Cloudy is newer and less proven on Desktop; Haze has 1.0 stable history and explicit Skiko support |
| Build plugin | `kotlin("multiplatform")` | `kotlin("jvm")` | `kotlin("jvm")` works for single-platform but loses multiplatform publication structure and source set isolation; KMP plugin also allows future iOS/macOS expansion |
| Publishing | `maven-publish` (built-in) | Vanniktech maven-publish plugin | Vanniktech is needed for Maven Central; for local-only, `maven-publish` is simpler with zero config |
| Material | Material3 wrapped | Custom design system from scratch | Rebuilding accessibility, state hoisting, ripple semantics from scratch is months of work; M3 wrapping is the standard library authorship pattern |
| Version management | `libs.versions.toml` | Manual version strings in `build.gradle.kts` | TOML catalog is the Gradle-recommended standard; required for IDE version assistance and refactoring |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Groovy `build.gradle` files | No type safety, poor IDE support, deprecated for new KMP projects | Kotlin DSL `.gradle.kts` throughout |
| `kotlin("jvm")` plugin for the library module | Single-platform JVM plugin — no multiplatform source sets, no KMP publication structure | `kotlin("multiplatform")` with `jvm("desktop")` target |
| `compose.desktop.currentOs` in the library module | `currentOs` bundles the OS-specific Skiko native binary — correct for apps, wrong for libraries (forces consumers to re-bundle) | `compose.desktop.common` — platform-agnostic; the consuming app provides `currentOs` |
| `alpha` / `beta` Material3 directly in `dependencies {}` | CMP 1.10.3 already bundles Material3 via `compose.material3`; manually pinning `androidx.compose.material3` alpha causes version conflicts | Use `compose.material3` from the Compose plugin's dependency accessor |
| `transparent = true` + `undecorated = true` window in the library itself | Known crashes on Windows 11 (issue #3757); window management belongs to the consuming app, not the library | Expose `AeroTitleBar` as a composable; let the consumer create the `Window` with `undecorated = true` |
| Hardcoding `jvmTarget = "21"` | Not all consumers run JDK 21; PROJECT.md states JDK 17 as baseline | Use `jvmTarget = "17"` in the library; consumers can override in their own compile options |

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| Compose Multiplatform 1.10.3 | Kotlin 2.1.0+ (min), 2.3.21 verified | JetBrains states "latest CMP always compatible with latest Kotlin" |
| Haze 1.7.2 | Compose Multiplatform 1.10.0+ | Release notes explicitly updated to CMP 1.10.0 |
| kotlinx-coroutines 1.10.2 | Kotlin 2.3.x | Confirmed via Slack and mvnrepository; companion version for Kotlin 2.2.20+, backcompat to 2.0+ |
| JDK 17 | All of the above | JetBrains minimum for native distributions; safe floor for Windows targets |
| Gradle 9.4.1 | Kotlin 2.3.x, CMP 1.10.x | Gradle 9.x requires toolchain resolver if using `javaToolchains` |

---

## Stack Patterns by Variant

**If you want to publish to Maven Central later (not MVP scope):**
- Replace `maven-publish` with `com.vanniktech.maven.publish` plugin
- Add PGP signing configuration
- Add `sonatype` repository credentials
- This is out of scope per PROJECT.md (local only for now)

**If you need blur on a Window without a background composable behind it:**
- Haze requires a `hazeSource` composable in the hierarchy — it blurs what's behind the haze child in Compose's draw tree
- For a truly OS-level blurred background (like Acrylic on Windows), you need JNA/WinAPI calls — this is advanced and out of scope
- For the MVP, simulate glass with translucent gradient overlays + Haze blurring Compose content

**If the consuming project uses `kotlin("jvm")` instead of KMP:**
- The library published with KMP will include a `desktop` classifier
- The consumer adds: `implementation("com.mordred:aero-ui-desktop:0.1.0-SNAPSHOT")`
- This works correctly; no changes needed to the library itself

---

## Sources

- [Compose Multiplatform Compatibility and Versions](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) — CMP 1.10.3 versions table, Kotlin minimum requirements — HIGH confidence
- [What's new in Compose Multiplatform 1.10.3](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html) — Material3 version, bundled library versions — HIGH confidence
- [GitHub: compose-multiplatform releases](https://github.com/JetBrains/compose-multiplatform/releases) — release dates and version numbers — HIGH confidence
- [Setting up multiplatform library publication](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html) — `publishToMavenLocal` task, publication structure — HIGH confidence
- [GitHub: chrisbanes/haze releases](https://github.com/chrisbanes/haze/releases) — Haze 1.7.2 latest stable, CMP 1.10.0 dependency — HIGH confidence
- [Haze platforms documentation](https://chrisbanes.github.io/haze/latest/platforms/) — Desktop JVM / Skiko support confirmed — HIGH confidence
- [GitHub: kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases) — 1.10.2 latest stable — HIGH confidence
- [Compose Desktop: Top-level windows management](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-desktop-top-level-windows-management.html) — `undecorated`/`transparent` window API — HIGH confidence
- [GitHub issue #3757: Fatal error undecorated+transparent on Windows 11](https://github.com/JetBrains/compose-multiplatform/issues/3757) — known crash warning — MEDIUM confidence (issue thread, not official docs)
- [Kotlin Blog: Compose Multiplatform 1.10.0](https://blog.jetbrains.com/kotlin/2026/01/compose-multiplatform-1-10-0/) — Hot Reload bundled, unified Preview — HIGH confidence
- [Gradle releases](https://gradle.org/releases/) — Gradle 9.4.1 latest stable — HIGH confidence
- [Kotlin 2.3.20 Released](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) — Kotlin 2.3.21 is current stable — HIGH confidence

---

*Stack research for: Compose Desktop UI component library (aero-compose-ui)*
*Researched: 2026-04-27*
