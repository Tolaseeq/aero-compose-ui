# Stack Research

**Domain:** Compose Desktop UI component library (Windows Aero / glassmorphism visual style)
**Researched:** 2026-04-28 (v1.1 icon system appended; v1.0 section preserved)
**Confidence:** HIGH (primary sources: JetBrains official docs, GitHub releases, kotlinlang.org)

---

## ⚠ SOURCE REVISION (2026-04-28, supersedes Feather references below)

**Icon source changed from Feather to Phosphor Regular.** User feedback: Feather's flat-modern outline doesn't fit Aero aesthetic; Phosphor's softer rounded outline is closer to Win7-toolbar-glyph style. Tooling (Valkyrie CLI 1.1.1) and overall pipeline are unchanged — only the source SVGs and a few constants differ.

**Phosphor-specific facts that override Feather references in the rest of this document:**

| Concern | Feather (old) | **Phosphor Regular (new — use these)** |
|---------|---------------|----------------------------------------|
| Source repo | `github.com/feathericons/feather` v4.29.2 | `github.com/phosphor-icons/core` (raw SVGs at `raw/regular/*.svg`) |
| License | MIT | **MIT** (unchanged) |
| Total icons available | ~286 | **~1300 in Regular weight** |
| viewBox | `0 0 24 24` | **`0 0 256 256`** |
| stroke-width attribute | `2` | **`16`** (in 256-unit space — equivalent visual weight to ~1.5px when rendered at 24dp; matches Aero soft-outline target) |
| stroke-linecap / stroke-linejoin | `round` / `round` | `round` / `round` (unchanged) |
| Maintenance status | Archived (no further releases) | **Active** (multi-weight family, regular branch maintained) |
| Naming idioms | `chevron-down`, `home`, `settings`, `filter`, `mail`, `send` | **`caret-down`, `house`, `gear`, `funnel`, `envelope`, `paper-plane`** — keep Phosphor names verbatim for traceability against phosphoricons.com |

**ImageVector.Builder constants (Phosphor):**
- `defaultWidth = 24.dp`, `defaultHeight = 24.dp` (render target — unchanged from Feather plan)
- `viewportWidth = 256f`, `viewportHeight = 256f` (was `24f`/`24f`)
- `strokeLineWidth = 16f` (in viewport units; was `2f`)

Valkyrie CLI handles the viewBox/stroke math automatically when converting raw Phosphor SVGs — no manual scaling needed.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | **2.1.21** | Language runtime | Locked by libs.versions.toml. K2 compiler. Compatible with CMP 1.7.3. |
| Compose Multiplatform | **1.7.3** | UI framework, Compose Desktop engine | Locked by libs.versions.toml. Bundles Skiko, desktop rendering. |
| Gradle | **8.14.3** | Build system | Kotlin DSL. Locked by gradle-wrapper. |
| JDK | **17** | JVM runtime | Locked by `jvmToolchain(17)` in library/build.gradle.kts. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `compose.material3` | bundled with CMP 1.7.3 | Material Design 3 foundations — theming, state, accessibility | Always — wrap/extend, do not replace. |
| `kotlinx-coroutines-core` | **1.10.2** | Async animation state, debounce, flow | Wherever animation tick or async state change needed. |
| `compose.desktop.common` | bundled with CMP 1.7.3 | Core Compose Desktop primitives (library JAR) | Always in :library — platform-agnostic. |
| `compose.desktop.currentOs` | bundled with CMP 1.7.3 | OS-specific native binaries | Only in :showcase — never in :library. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle Kotlin DSL (`build.gradle.kts`) | Typed build scripts | Mandatory; all IDE refactoring and completion works. |
| `gradle/libs.versions.toml` (Version Catalog) | Centralized dependency versions | Standard practice. Use kebab-case keys. |
| `maven-publish` Gradle plugin | Publish JAR to `~/.m2` | Built into Gradle. `./gradlew publishToMavenLocal` |

---

## v1.1 Addition: AeroIcons Icon System

This section covers only what is new or changed for the v1.1 milestone. Everything above is unchanged.

### Dependency Change in :library

Remove from `library/build.gradle.kts`:
```kotlin
// REMOVE:
implementation(compose.materialIconsExtended)
```

No new runtime dependencies are added. `AeroIcons` is pure generated Kotlin (`ImageVector` builders) — zero extra libraries required.

### SVG Source: Feather Icons

| Attribute | Value |
|-----------|-------|
| Repository | `https://github.com/feathericons/feather` |
| License | **MIT** |
| Latest version | **v4.29.2** (released 2024-05-01) |
| Icon count | **286 SVG icons** |
| SVG viewBox | `0 0 24 24` |
| Default stroke-width | `2` |
| Stroke linecap | `round` |
| Stroke linejoin | `round` |
| Maintenance status | Archived / no longer actively developed. v4.29.2 is the final release. Lucide is the community fork (1500+ icons, identical design language). For a fixed subset port this is acceptable — the source is stable. |
| NPM distribution | `feather-icons@4.29.2` (611 KB unpacked, includes `/icons/*.svg`) |
| SVG download | `npm pack feather-icons@4.29.2` or clone the tag: `git clone --depth 1 --branch v4.29.2 https://github.com/feathericons/feather.git` |

Acquisition for the project: clone `v4.29.2` tag, copy the `icons/` directory (286 `.svg` files) into a scratch location. Feed the ~120-150 selected SVG files to Valkyrie CLI. Do NOT commit the source SVGs into the repo — only the generated `.kt` files go in.

### SVG-to-ImageVector Tooling: Recommendation

**Use: Valkyrie CLI `cli-1.1.1`**

| Tool | Verdict | Reason |
|------|---------|--------|
| **Valkyrie CLI 1.1.1** | **RECOMMENDED** | Actively maintained (Feb 2026 release), explicit KMP/ImageVector support, generates clean backing-property or lazy-property Kotlin, icon-pack object support, batch-capable, no IDE required. |
| Valkyrie IntelliJ plugin 1.5.0 | Supplementary | Useful for one-off previewing during development. Requires IntelliJ IDEA 2025.3.3+. Not suitable as the primary batch conversion tool. |
| Valkyrie Gradle plugin 0.4.0 | Not recommended for this project | Adds build-time generation complexity. Appropriate if SVGs remain the source of truth and change frequently. For a fixed Feather subset, static generation is simpler. |
| svg-to-compose (DevSrSouza) | Not recommended | Last release 0.11.0 in Sep 2024; marked experimental; lower community traction. |
| rafaeltonholo/svg-to-compose | Alternative | More recent fork (v2.2.0 as of April 2026), KMP support, Gradle plugin + CLI + online playground. Viable if Valkyrie has conversion issues with specific Feather paths. |
| JetBrains "Vector Asset" import | Not applicable | Android Studio only. Compose Desktop has no equivalent import wizard. |
| Hand-coding 120 ImageVectors | Rejected | Feather paths are stroke-based with cubic beziers — manually translating 286 paths is error-prone and takes days. |
| Runtime `vectorResource` from XML | Rejected | Compose Desktop `loadXmlImageVector` works but requires bundling XML files in resources, adds I/O at startup, and loses compile-time type safety. The `AeroIcons.Close` constant pattern requires static Kotlin vals. |

### Valkyrie CLI Usage

**Installation:**
```bash
# Download cli-1.1.1 from GitHub releases:
# https://github.com/ComposeGears/Valkyrie/releases/tag/cli-1.1.1
# Unzip, make executable
chmod +x valkyrie
```

**Batch conversion command:**
```bash
./valkyrie svgxml2imagevector \
  --input-dir ./feather-selected/ \
  --output-dir ./library/src/main/kotlin/com/mordred/aero/icons/ \
  --package-name com.mordred.aero.icons \
  --icon-pack-name AeroIcons \
  --output-format BackingProperty
```

Options of interest:
- `--output-format BackingProperty` — generates the null-check backing property pattern (matches Material Icons generator output; more explicit than `lazy`)
- `--output-format LazyProperty` — generates `by lazy(LazyThreadSafetyMode.NONE)` delegate (slightly cleaner syntax, same runtime behaviour in single-threaded UI)
- `--icon-pack-name` — names the top-level object (`object AeroIcons`)
- `--package-name` — sets the Kotlin package in generated files

Run `./valkyrie svgxml2imagevector -h` for the authoritative options list (version may differ).

**Recommended output format: BackingProperty** — matches the upstream Material Icons generated pattern; IDE-friendly; no Lazy overhead; fits `explicitApi()` requirement cleanly.

### Generated Code Pattern

Valkyrie produces one `.kt` file per icon. Each file follows this pattern:

```kotlin
// File: Close.kt
package com.mordred.aero.icons

import androidx.compose.ui.graphics.vector.ImageVector
// ... path imports

val AeroIcons.Close: ImageVector
    get() {
        if (_close != null) return _close!!
        _close = ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 6f); lineTo(6f, 18f)
                moveTo(6f, 6f); lineTo(18f, 18f)
            }
        }.build()
        return _close!!
    }

@Suppress("ObjectPropertyName")
private var _close: ImageVector? = null
```

The top-level `AeroIcons` object is generated as a separate file:
```kotlin
// File: AeroIcons.kt
package com.mordred.aero.icons

object AeroIcons
```

Icons are accessed as extension properties on `AeroIcons`: `AeroIcons.Close`, `AeroIcons.ChevronDown`, etc. This is compile-time safe and IDE-autocomplete-friendly.

### File Organization Pattern

**Recommendation: flat package, one file per icon, no sub-categories.**

```
library/src/main/kotlin/com/mordred/aero/icons/
├── AeroIcons.kt           ← object AeroIcons declaration
├── Close.kt
├── ChevronDown.kt
├── ChevronUp.kt
├── ChevronLeft.kt
├── ChevronRight.kt
├── Check.kt
├── AlertCircle.kt
├── Info.kt
├── ...                    ← one file per icon (~120-150 files)
```

Rationale for flat (no `outlined/` sub-package):
- Feather has only one weight/style — no meaningful sub-categorization needed
- Material Icons sub-packages (`outlined/`, `filled/`) exist because they have multiple style variants
- Flat keeps access syntax clean: `AeroIcons.Close` vs `AeroIcons.Outlined.Close`
- All 120-150 icons in one package is well within normal library size

If sub-categories are added later (e.g., a `filled` variant in v1.2), Valkyrie supports nested packs via `--nested-pack-name`.

### Build-Time vs Source-Checked-In Decision

**Decision: Generate once, commit `.kt` files, do not commit source SVGs.**

| Approach | JAR impact | Build performance | Type safety | Recommended |
|----------|-----------|-------------------|-------------|-------------|
| Commit generated `.kt` files (no SVGs in repo) | Neutral — bytecode size equals hand-written code | No build-time overhead | Full compile-time safety | **YES** |
| Commit SVGs + Valkyrie Gradle plugin generation at build time | Same bytecode output | Adds generation step to every clean build (~5-15s for 120 icons) | Full compile-time safety | No — unnecessary complexity for a fixed icon set |
| Commit SVGs + runtime `loadXmlImageVector` | Larger JAR (XML files in resources) + I/O overhead at first access | Fast build | No type safety (string-based) | No — breaks `AeroIcons.X` constant API |

Storing SVGs in a separate scratch/tools directory (not version-controlled) is acceptable. The generated `.kt` files are the source of truth in git.

### ImageVector Caching Behaviour

Compose Multiplatform 1.7.3 does **not** have a global ImageVector cache. Caching is handled entirely by the backing property pattern in the generated code:

- Each icon is initialized on **first access** (lazy)
- The backing `private var _close: ImageVector?` holds the instance
- Subsequent accesses return the same instance — no re-building
- The `ImageVector` object lives for the lifetime of the JVM process (it is a top-level property, not scoped to composition)
- 120 icons × ~2-5 KB each in memory = ~240-600 KB total if all icons are touched — negligible for a Desktop JVM app with 256+ MB heap

No `remember {}` is needed at the call site when using `Icon(AeroIcons.Close, ...)` — the `ImageVector` instance itself is already cached at the module level. `remember` is still appropriate if the `ImageVector` is passed to `painterFor(imageVector)` in a hot path, but for the `Icon()` composable this is handled internally.

`LazyThreadSafetyMode.NONE` is appropriate for Compose Desktop's single UI thread model. If `BackingProperty` mode is chosen (recommended), thread safety is not a concern — the null check is in the getter, and UI composition is always on the main thread.

### No New Gradle Dependencies

The only change to `library/build.gradle.kts` is removal of one line:
```kotlin
// Remove:
implementation(compose.materialIconsExtended)
```

`ImageVector`, `ImageVector.Builder`, `PathBuilder`, `SolidColor`, `StrokeCap`, `StrokeJoin` are all in `compose.ui` and `compose.foundation`, which are already declared dependencies. No new artifacts required.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not Alternative |
|----------|-------------|-------------|---------------------|
| UI framework | Compose Multiplatform Desktop | JavaFX / TornadoFX | Dead community, no modern theming primitives, no Skia rendering |
| UI framework | Compose Multiplatform Desktop | Swing directly | No declarative composition, no state-driven rendering, no animation framework |
| Blur / glass | Custom gradient modifiers (shipped v1.0) | Haze library | Already implemented in v1.0 without Haze; adding it would be a migration, not a new feature |
| Build plugin | `kotlin("jvm")` (current) | `kotlin("multiplatform")` | Current project is Desktop-only; `kotlin("jvm")` is correct and simpler for single-platform. KMP migration is optional later. |
| Publishing | `maven-publish` (built-in) | Vanniktech maven-publish plugin | Vanniktech is needed for Maven Central; for local-only, `maven-publish` is simpler |
| Icon source | Feather v4.29.2 | Lucide (Feather fork, 1500+ icons) | Lucide has no official Kotlin/Compose port as a package; would also need SVG→ImageVector conversion. Feather is smaller and the design language is identical. Decision already made. |
| SVG conversion | Valkyrie CLI 1.1.1 | rafaeltonholo/svg-to-compose v2.2.0 | Both are viable; Valkyrie has clearer KMP focus and backing-property output matching Material Icons pattern. |
| File organization | Flat `com.mordred.aero.icons` package | Sub-packages by category | Feather has one weight — no meaningful sub-categorization. Flat is simpler. |
| Build strategy | Commit generated `.kt` | Gradle plugin regeneration at build time | Fixed icon set does not change; build-time generation adds overhead with no benefit. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `compose.materialIconsExtended` | Being removed from :library in v1.1 — replaced by AeroIcons | `AeroIcons.*` extension properties |
| Runtime `loadXmlImageVector` for icon set | Loses type safety, requires XML resource files in JAR, I/O overhead | Pre-generated `ImageVector` Kotlin constants |
| Valkyrie Gradle plugin (for this project) | Fixed icon set doesn't change — build-time generation is waste | Run Valkyrie CLI once offline, commit output |
| Hand-coding ImageVector paths | Feather uses complex cubic bezier strokes — error-prone and slow for 120+ icons | Valkyrie CLI batch conversion |
| Groovy `build.gradle` files | No type safety, deprecated for new KMP/Compose projects | Kotlin DSL `.gradle.kts` throughout |

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| Compose Multiplatform 1.7.3 | Kotlin 2.1.21 | Locked in libs.versions.toml; confirmed compatible. |
| Valkyrie CLI 1.1.1 | Any JVM (runs standalone) | Not a Gradle plugin — no version coupling with project's Kotlin/CMP versions. Generates source-compatible code for any Kotlin 1.8+ target. |
| Generated `ImageVector` code | CMP 1.7.3 / compose.ui bundled | Uses `androidx.compose.ui.graphics.vector.ImageVector` — present in all CMP versions. |
| kotlinx-coroutines 1.10.2 | Kotlin 2.1.21 | Confirmed via libs.versions.toml. |
| JDK 17 | All of the above | Toolchain locked; Valkyrie CLI binary also runs on JDK 17. |

---

## Sources

- [GitHub: ComposeGears/Valkyrie](https://github.com/ComposeGears/Valkyrie) — CLI tool, Gradle plugin, output format documentation — HIGH confidence
- [Valkyrie Releases](https://github.com/ComposeGears/Valkyrie/releases) — cli-1.1.1 (Feb 26, 2025), idea-plugin-1.5.0 (Apr 17, 2025), gradle-plugin-0.4.0 (Feb 26, 2025) — HIGH confidence
- [Valkyrie README (raw)](https://raw.githubusercontent.com/ComposeGears/Valkyrie/main/README.md) — Gradle plugin configuration block, CLI command syntax — HIGH confidence
- [GitHub: feathericons/feather](https://github.com/feathericons/feather) — MIT license, v4.29.2 latest, 24x24 viewBox, stroke-width 2, stroke-linecap round — HIGH confidence
- [npm: feather-icons@4.29.2](https://www.npmjs.com/package/feather-icons) — 286 icons, 611 KB unpacked, last published 2024-05-01 — HIGH confidence
- [GitHub: rafaeltonholo/svg-to-compose](https://github.com/rafaeltonholo/svg-to-compose) — v2.2.0, April 2026, KMP support — MEDIUM confidence (alternative tool)
- [compose-multiplatform Image_And_Icons_Manipulations tutorial](https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/Image_And_Icons_Manipulations/README.md) — `loadXmlImageVector` pattern, `remember {}` caching — HIGH confidence
- [Material Icons source structure](https://android.googlesource.com/platform/frameworks/support/+/4bd91cd472a563d6a788dd8d1fb1dc0e76b9480f/compose/material/material/icons/) — one `.kt` file per icon, `by lazy` backing property pattern, `Icons` object with extension properties — HIGH confidence
- [Kotlin Lazy thread safety modes](https://docs.bswen.com/blog/2026-02-22-kotlin-lazy-thread-safety/) — NONE mode appropriate for single-threaded UI context — MEDIUM confidence
- [GitHub: DevSrSouza/svg-to-compose](https://github.com/DevSrSouza/svg-to-compose) — 0.11.0, Sep 2024, experimental, lower maintenance — MEDIUM confidence

---

*Stack research for: Compose Desktop UI component library (aero-compose-ui) — v1.1 AeroIcons icon system*
*Researched: 2026-04-28*
