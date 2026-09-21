# Toolchain Upgrade Research — v2.1 Glass Refinement

**Domain:** Compose Multiplatform (Desktop/JVM) toolchain upgrade for a published Maven/JitPack library
**Researched:** 2026-07-21
**Scope:** Binding decision already made (upgrade IS happening). This document answers WHAT it entails and WHAT breaks — not whether to do it.
**Overall confidence:** HIGH for version numbers/dates (GitHub Releases API, Maven Central metadata.xml, official CMP CHANGELOG.md — all machine-verified this session). MEDIUM for exact `dropShadow`/`innerShadow` constructor shape (cross-checked but not hands-on compiled). MEDIUM for the M3-1.4/1.5-era component-level breaking-change survey (budget-limited; no structural changes found for the specific APIs this repo uses, but a full M3 changelog line-by-line audit was out of scope). Every claim below is tagged.

---

## PART 1 — Target Version (verified via GitHub Releases API + Maven Central)

Verified 2026-07-21 via `api.github.com/repos/JetBrains/compose-multiplatform/releases` (full paginated list) and `repo1.maven.org/maven2/org/jetbrains/compose/compose-gradle-plugin/maven-metadata.xml`. **HIGH confidence — machine-verified ground truth, not memory.**

| Version | Published | Status |
|---|---|---|
| **1.7.3** (current) | 2024‑12‑20 | Baseline |
| 1.8.0 / 1.8.1 / 1.8.2 | 2025‑05‑06 / 05‑20 / 06‑17 | Stable |
| **1.9.0** | **2025‑09‑16** | Stable — `Modifier.dropShadow`/`innerShadow` land here |
| 1.9.1 / 1.9.2 | 2025‑10‑14 / 10‑29 | Stable |
| **1.9.3** | **2025‑11‑06** | **Latest stable 1.9.x line** |
| 1.10.0 / .1 / .2 | 2026‑01‑13 / 02‑10 / 03‑05 | Stable |
| **1.10.3** | **2026‑03‑19** | Latest stable 1.10.x line |
| **1.11.0** | **2026‑05‑13** | Stable |
| **1.11.1** | **2026‑06‑02** | **Latest STABLE overall** as of 2026‑07‑21 |
| 1.12.0-alpha01/02, -beta01/02 | 2026‑05‑19 → 2026‑07‑14 | **Prerelease — NOT stable.** `1.12.0-beta02` (2026‑07‑14) is the newest artifact published to Maven Central, but it is explicitly marked `prerelease: true` by the GitHub Releases API. Do not target it. |

**Answer: latest stable = `1.11.1` (2026‑06‑02). Latest stable 1.9.x (the conservative intermediate target) = `1.9.3` (2025‑11‑06).**

Note on Maven Central's `maven-metadata.xml`: its `<release>` tag literally says `1.12.0-beta02` — this element means "most recently deployed artifact," not "latest stable." The GitHub Releases API's `prerelease` boolean is the authoritative stability signal and was cross-checked against the full version list in `maven-metadata.xml` (which enumerates every published coordinate including alphas/betas) — the two sources agree on which versions exist; only the GitHub API distinguishes stability.

**Recommendation up front (justified fully in Part 5): target `1.9.3`, not `1.11.1`.** It delivers the one capability this milestone needs at a materially smaller compatibility blast radius.

---

## PART 2 — Compatibility Matrix

### Kotlin

| CMP version | Kotlin requirement | Source | This project (2.1.21) |
|---|---|---|---|
| 1.9.0 | **≥ 2.1.0, for ALL platforms including JVM** (previously only native/web) [PR #2276] | Official CHANGELOG.md, 1.9.0 "Migration Notes / Desktop" | ✅ Already satisfies — **zero Kotlin bump needed** |
| 1.9.1–1.9.3 | Same floor, unchanged | CHANGELOG.md | ✅ |
| 1.10.0 | Known Issue: bundled Compose Hot Reload plugin requires **Kotlin ≥ 2.1.20** [#5444]; "Kotlin 2.2 required for native and web platforms" [#2357] — JVM-only projects (this one) are NOT gated by the 2.2 requirement | CHANGELOG.md, 1.10.0-beta01 "Known Issues" + 1.10.0 "Breaking Changes/iOS" (scoped to iOS in the actual breaking-changes entry, not JVM) | ✅ 2.1.21 > 2.1.20 |
| 1.10.1–1.10.3 | Same | CHANGELOG.md | ✅ |
| **1.11.0** | **"The project has migrated to Kotlin language version and API version 2.2. The projects depending on Compose Multiplatform need to use the same version or newer"** [#2614] — this is NOT platform-scoped, applies to JVM too. Separately: "Kotlin 2.3 is required for native and web platforms" [#2755] (still not JVM) | CHANGELOG.md, 1.11.0 "Migration Notes / Multiple Platforms" | ❌ **Must bump to ≥ 2.2.0** |
| 1.11.1 | Same as 1.11.0 | CHANGELOG.md | ❌ Must bump |

Kotlin release dates (verified via kotlinlang.org/docs/releases.html, HIGH confidence): 2.1.21 = 2025‑05‑13 (current), 2.2.0 = 2025‑06‑23, 2.2.21 = 2025‑10‑23 (latest 2.2.x), 2.3.0 = 2025‑12‑16, 2.4.0 = 2026‑06‑03, 2.4.10 = 2026‑07‑14 (latest overall).

**If targeting 1.9.3: no Kotlin change.** `kotlin.jvm` plugin stays `2.1.21`; `compose.compiler` plugin (`org.jetbrains.kotlin.plugin.compose`) auto-tracks the same catalog entry via `version.ref = "kotlin"` in `libs.versions.toml` — nothing to touch.
**If targeting 1.11.1: bump `kotlin = "2.1.21"` → at minimum `"2.2.0"`, recommend `"2.2.21"`** (latest 2.2.x stable; going further to 2.3.x/2.4.x is optional, not required by CMP 1.11.x specifically).

### Gradle

Official CMP-specific "minimum Gradle version" documentation was not found as an explicit single number (MEDIUM confidence, derived rather than directly stated). What IS verified: Kotlin 2.2's own compatibility statement is **"fully compatible with Gradle 7.6.3 through 8.14"** (kotlinlang.org, via WebSearch — MEDIUM confidence, single-source). Since `org.jetbrains.compose` builds on top of the Kotlin Gradle Plugin, this is the binding floor. Separately, **AGP 9.0 requires Gradle ≥ 9.1.0** — but this is **not applicable**: this project has no Android target (`settings.gradle.kts` includes only `:library`/`:showcase`, both pure JVM via `org.jetbrains.kotlin.jvm`).

**Gradle 8.14.3 (current) is sufficient for both 1.9.3 and 1.11.1.** No Gradle bump required at either target. MEDIUM confidence (derived from Kotlin's stated ceiling, not a first-party CMP statement) — recommend a real `./gradlew build` as the actual verification, not just trusting this table.

### JDK

Official guidance (WebSearch, corroborated by JetBrains docs summary, MEDIUM-HIGH confidence): Compose Multiplatform for Desktop requires **JDK 11+ to run**, but **JDK 17+ is required for `jpackage`-based native distribution packaging** — which `:showcase/build.gradle.kts` already relies on (`targetFormats(TargetFormat.Exe, TargetFormat.Deb)`).

**`jvmToolchain(17)` (both `library/build.gradle.kts` and `showcase/build.gradle.kts`) remains correct and sufficient at both 1.9.3 and 1.11.1 — no JDK change.** One unrelated macOS-only note found in CMP 1.9.0 CHANGELOG ("Fix codesigning on macOS when developer id contains non-ASCII characters. Note that this requires JDK 21 or later" [#5358]) does not apply — this project is Windows-primary, does no macOS codesigning.

### `kotlinx-coroutines` and `kotlinx-datetime`

No CMP-forced version bump was found for either library in the default (non-opt-in) dependency path.

- `kotlinx-datetime`: CMP 1.9.0's CHANGELOG states `kotlinx-datetime` is updated to `0.7.1` **only if you opt into the beta Material3 artifact** (`org.jetbrains.compose.material3:material3:1.9.0-beta06`) — this project does not and should not opt into that beta artifact (see Material3 section below), so **this forced bump does not apply**. `kotlinx-datetime:0.6.2` — pinned, `api()`-exposed at `library/build.gradle.kts:27`, consumer-visible in picker signatures — should remain compatible unchanged. LOW-MEDIUM confidence (no explicit compatibility statement found either way for the default path); verify empirically by running the full picker test suite after the bump rather than trusting this in isolation.
- `kotlinx-coroutines:1.10.2`: no CMP-driven forcing found anywhere in the reviewed changelog. LOW risk; opportunistic bump only if a compile warning appears.

### Gradle Plugin ID / DSL Accessors — the most consequential finding in this section

Plugin ID `org.jetbrains.compose` is unchanged throughout 1.7.3 → 1.11.1 — no action needed there.

**But the `compose.material3` DSL accessor (`api(compose.material3)` at `library/build.gradle.kts:22`) resolves to a DIFFERENT actual Material3 artifact depending on target version, and this matters a lot for a published library:**

| CMP version | What `compose.material3` alias resolves to | Jetpack M3 basis | Stability |
|---|---|---|---|
| 1.7.3 (current) | `material3:1.7.3`-line artifact | ~1.3.x | Stable |
| 1.9.0 | `org.jetbrains.compose.material3:material3:1.8.2` (deliberately **frozen/decoupled** because Jetpack M3 1.4 wasn't stable yet) [#5360] | 1.3.2 | Stable |
| **1.9.1 → 1.9.3** | **`org.jetbrains.compose.material3:material3:1.9.0`** (alias re-mapped once Jetpack M3 1.4 went stable) [#5441] | **1.4.0** | **Stable** |
| 1.10.0 → 1.10.3 | `org.jetbrains.compose.material3:material3:1.10.0-alpha05` | 1.5.0-alpha08 | **Alpha** |
| 1.11.0 / 1.11.1 | `org.jetbrains.compose.material3:material3:1.11.0-alpha07` | 1.5.0-alpha17 | **Alpha** |

Source: CMP official CHANGELOG.md, sections `1.9.0`, `1.9.1` (explicit migration note quoted verbatim), `1.10.0`/`1.10.3`, `1.11.0` "Components/Libraries" tables. **HIGH confidence — directly quoted from the primary source, cross-checked across five separate release sections.**

Additionally: **"Dependency aliases in Gradle plugin (e.g. `compose.ui`) are now deprecated"** starting **1.10.0-beta01** [#5462] — `compose.material3`/`compose.foundation`/`compose.ui`/etc. still *work* through 1.11.1 (deprecated, not removed) but emit warnings and are explicitly discouraged going forward.

**Consequence: if this project targets 1.10.x or 1.11.x and leaves `api(compose.material3)` as-is, it will transitively ship an ALPHA Material3 build inside a PUBLISHED library's public API surface to every consumer.** This alone is close to disqualifying for those targets unless the alias is replaced with an explicit pinned coordinate (see Part 5).

**If targeting 1.9.3: the alias already resolves to a stable M3 1.4.0 artifact — no code change strictly required, though pinning explicitly (`api("org.jetbrains.compose.material3:material3:1.9.0")`) is still good practice given the alias-deprecation trajectory.**

### `compose.uiTest`

Still gated behind `@OptIn(ExperimentalComposeLibrary::class)` through at least 1.9.x — no graduation-to-stable found anywhere in the reviewed changelog window. At **1.11.0**, the underlying test-runner functions themselves change status: **`runComposeUiTest`, `runSkikoComposeUiTest`, `runDesktopComposeUiTest` are deprecated in favor of unnamed "v2" APIs** [#2919], and the v2 APIs on non-Android targets **switch the default `TestDispatcher` from `UnconfinedTestDispatcher` to `StandardTestDispatcher`** [#2919]. This is a real behavior change for a coroutine-timing-sensitive test (see Part 3/5). **Not present at 1.9.3** — v1 test API and its Unconfined default are unchanged through the entire 1.9.x line.

---

## PART 3 — Breaking Changes Across 1.7.3 → Target (tied to concrete APIs this repo uses)

Method: full-text review of the official `JetBrains/compose-multiplatform` `CHANGELOG.md` (6,875 lines, every stable + prerelease section from 1.7.3 through 1.12.0-beta02), cross-referenced against this repo's actual API usage via `Grep`.

### 1. Material3 version bump

Concrete usage found in this repo: `ButtonDefaults.buttonColors()` (`AeroButton.kt:93`), `ButtonDefaults.outlinedButtonColors()` (`AeroOutlinedButton.kt:94`), `SliderDefaults.colors()` (`AeroSlider.kt:51`). **Zero removals or signature-breaking changes were found for `ButtonDefaults`, `SliderDefaults`, `ButtonColors`, or `SliderColors` anywhere in the reviewed changelog** — these are long-stable, additive-parameter M3 surface areas. Going Jetpack M3 ~1.3.x → 1.4.0 stable (the actual delta at CMP 1.9.1+) is LOW structural risk for this repo. HIGH confidence for "no removal found"; this is a changelog-absence finding, cross-checked by the fact this project's usage is limited to three trivial call sites.

`LocalMinimumInteractiveComponentSize` / `LocalMinimumInteractiveComponentEnforcement`: **grep confirms neither identifier is used anywhere in `library/src/main` today** — this is currently a non-issue for the *existing* codebase. It becomes relevant only if v2.1's planned custom-drawing work (this milestone, not the upgrade phase) newly touches minimum-touch-target enforcement. MEDIUM confidence that `LocalMinimumInteractiveComponentSize` (the post-1.2.0 replacement name) remains the correct API at M3 1.4/1.5 — no deprecation-of-the-replacement was found in this session's changelog window, but a full M3-specific changelog audit was out of scope for the research budget. **Flag: do a 10-minute doc check at the point any v2.1 component implementation actually references this API**, don't trust this document alone for that specific symbol.

### 2. `androidx.compose.foundation` — hover/pointer/interaction/layout primitives

Grepped the full CMP changelog for `hoverable`, `MutableInteractionSource`, `positionChange`, `BoxWithConstraints`, `SubcomposeLayout`, `LazyColumn`/`LazyListState`. **Zero breaking-change or signature-change entries found for any of these across the entire 1.7.3 → 1.11.1 (and beyond, into 1.12.0-beta02) window.** The only hits are unrelated bug fixes (Linux `WindowDraggableArea` drag-UX improvement, pre-1.7.3-era `LazyColumn` scroll fixes already baked into this project's baseline).

**This directly de-risks the project's most load-bearing locked pattern:** `awaitPointerEventScope` + manual pointer loop (PITFALL-03, the `detectDragGestures`-ban due to 18dp desktop touchSlop) and the shared `Modifier.aeroDragSplitter` utility should port unchanged at either 1.9.3 or 1.11.1. HIGH confidence — absence-of-evidence across a full official changelog text search is a strong signal here, given how much this project depends on these exact APIs.

### 3. Indication / Ripple migration

`rememberRipple()` → `ripple()`, `Indication`/`IndicationNodeFactory`, `LocalIndication` — **zero matches anywhere in the CMP CHANGELOG.md 1.7.3 → 1.11.1**, and a repo grep confirms `rememberRipple` is not used anywhere in `library/src/main` today. This migration is understood (training-data knowledge, NOT independently re-verified via a primary source this session — flag MEDIUM confidence for the specific claim "it landed in Compose Foundation 1.6.0") to have already happened well before this project's 1.7.3 baseline. **Net effect for this upgrade: nothing changes here, in either direction.** No action needed.

### 4. `graphicsLayer` / `drawBehind` / `drawWithCache` / `Brush` / `Popup` / `Dialog` / `PopupPositionProvider`

No breaking changes found for `drawBehind`, `drawWithCache`, `Brush`, or custom `PopupPositionProvider` implementations (this project's `AeroCalendarPositionProvider`, `AeroPopupPositionProvider`, `AeroCursorPositionProvider`, `FullWindowPositionProvider`) across the whole window. `graphicsLayer` gained one **additive** feature at 1.11.1 (`LayerOutsets` for `GraphicsLayer`/`Modifier.graphicsLayer`, [#3144]) — opt-in, no impact unless adopted.

**Popup/Dialog DID change in ways directly relevant to this project's heaviest-used pattern (locked rule: "ALL overlays use `Popup(...)`, never `Dialog(transparent=true)`"):**

- **CMP 1.10.0: "Deprecation level of `Popup` overloads without `PopupProperties` parameter changed from `WARNING` to `ERROR`"** [#2495]. This would be a genuine COMPILE-BREAKING change for any `Popup(...)` call that omits `properties = PopupProperties(...)`.
  - **Verified via `Grep` across `library/src/main`: all 14 `Popup(` call sites in this codebase already pass `properties = PopupProperties(...)` explicitly** (`AeroDropdownPopup.kt`, `AeroDropdown.kt`, `AeroComboBox.kt`, `AeroTooltip.kt` ×2, `AeroContextMenu.kt`, `AeroPopover.kt`, `AeroDateTimePicker.kt`, `AeroDrawer.kt`, `AeroColorPickerButton.kt`, `AeroDateRangePicker.kt`, `AeroDatePicker.kt`, `AeroDateTimeRangePicker.kt`, `AeroTimePicker.kt`). **This is a non-issue for THIS codebase at either target — verified, not assumed. Zero action required.** HIGH confidence (direct grep of every call site, not sampled).
- CMP 1.10.0: pre-1.7 `PopupProperties`/`DialogProperties` constructor workaround removed — "may formally affect binary compatibility," but the maintainers state they're "not aware of concrete cases" and this only bites if a *third-party* library binary-links against the removed constructor shape. This project builds `Popup`/`PopupProperties` from source every time (not linking a stale compiled binary), so this is a non-issue. LOW risk.
- CMP 1.11.0: `Dialog` gained a default enter/exit animation (`DialogProperties.animateTransition`, on by default) — this project's locked rule bans `Dialog(transparent=true)` and this repo doesn't appear to use `Dialog`/`DialogWindow` at all in the overlay layer (all overlays are `Popup`-based per the audit above) — N/A.

### 5. Desktop-specific

`WindowDraggableArea`: one Linux-only drag-UX fix at 1.11.0, no API change. `undecorated`/`transparent` window behavior: see Part 5 (crash-class analysis). `androidx.compose.ui.awt` interop: no findings either way. **Skiko/Skia milestone bumps across the path: Skia m126 (1.7.3 baseline) → Skia m138 at CMP 1.10.0 [#2304] → Skia m144 at CMP 1.11.0 [#2779]** — two milestone bumps if going all the way to 1.11.x, zero if staying at 1.9.3 relative to... actually 1.9.0 itself doesn't explicitly list a Skia bump in its own section (the m138 bump is specifically called out at 1.10.0), so 1.9.3 stays on the Skia generation already effectively in place since 1.7.3/early-1.8.x. This project has **zero direct `org.jetbrains.skia.*` type references today** (confirmed by prior `STACK.md` research's own audit of `GlassModifiers.kt`), so the documented breakage precedent (Haze library's `NoSuchMethodError` on a CMP 1.11.0-alpha Skia bump, per `STACK.md`) does not apply to this repo unless/until the v2.1 visual work later adds `nativeCanvas`/`MaskFilter` code — which `STACK.md` already flags as an opt-in escalation, not the default plan.

### 6. Test infrastructure — the sharpest risk in this whole document

`runComposeUiTest`'s test block became `suspend` at **1.9.0** [#2066] — source-compatible (a bare `{ ... }` lambda literal satisfies a `suspend` function-type parameter without any call-site change required), so this is a non-event even at 1.9.3.

At **1.11.0**, `runComposeUiTest`/`runSkikoComposeUiTest`/`runDesktopComposeUiTest` are **deprecated** (not removed — still compiles, still runs) in favor of v2 APIs, and **the v2 APIs default to `StandardTestDispatcher` instead of `UnconfinedTestDispatcher`** on non-Android targets [#2919]. `UnconfinedTestDispatcher` runs queued coroutines eagerly/immediately; `StandardTestDispatcher` queues them and requires explicit pumping (`advanceUntilIdle()`, `runCurrent()`, or the test harness's own idling calls) to execute.

**This project has exactly one Compose UI test that depends on precise coroutine-dispatch interleaving: `AeroPanelGroupRecomposeUiTest`** (added Phase 14, `testImplementation(compose.uiTest)` + `compose.desktop.currentOs`), which drives a programmatic `performMouseInput` drag *interleaved with* a recompose trigger to reproduce the v2.0.4 RCMP bug (header-strip duplication under drag-while-recompose). This is the **sole real regression guard** for that fix — and this project has direct, recorded institutional memory (`feedback_repro_must_exercise_path`) of a *prior* regression guard that gave a false-positive sign-off (v2.0.3/14-02) because its repro didn't actually exercise the buggy code path under test. A default-dispatcher change that alters coroutine timing is exactly the class of change that could silently turn this test into a second false-positive.

**This risk exists only if targeting 1.11.x. It does not exist at 1.9.3** (v1 test API + Unconfined default is unchanged through the entire 1.9.x line — confirmed by the absence of any test-infra migration note in the 1.9.0/1.9.1/1.9.2/1.9.3 changelog sections). This is one of the strongest concrete arguments for the 1.9.3 recommendation in Part 5.

`captureToImage()` — no changes found in the reviewed window.

---

## PART 4 — `Modifier.dropShadow` / `Modifier.innerShadow` Exact Signatures

**Confidence: MEDIUM-HIGH.** Cross-checked against a *version-pinned* third-party API mirror (`composables.com/docs/androidx.compose.ui/ui/1.9.0-rc01/modifiers/dropShadow` — explicitly pinned to the `1.9.0-rc01` build, i.e. the actual release-candidate immediately preceding CMP 1.9.0 GA) and a second, unpinned mirror of the same site that shows an identical shape, suggesting the API has been stable since RC and unchanged through current releases. Landed via JetBrains/compose-multiplatform-core PR **#2183 "Support customizable shadows"**, listed in the official CHANGELOG under **`## Features > ### Multiple Platforms`** (not gated to any single platform) — this is the basis for the desktop-support confirmation below.

```kotlin
// package androidx.compose.ui.graphics.shadow — introduced Compose (Multiplatform) UI 1.9.0

fun Modifier.dropShadow(shape: Shape, shadow: Shadow): Modifier
fun Modifier.dropShadow(shape: Shape, block: DropShadowScope.() -> Unit): Modifier

fun Modifier.innerShadow(shape: Shape, shadow: Shadow): Modifier
fun Modifier.innerShadow(shape: Shape, block: InnerShadowScope.() -> Unit): Modifier

class Shadow {
    // Color-based overload
    constructor(
        radius: Dp,
        color: Color = Color.Black,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    )

    // Brush-based overload (gradient shadows)
    constructor(
        radius: Dp,
        brush: Brush,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    )
}
```

Both `dropShadow` and `innerShadow` take a single unified `Shadow` type (not separate `DropShadow`/`InnerShadow` classes as the milestone brief's framing suggested) — the `Shape`/color/radius/spread/offset/blendMode parameters live on `Shadow` itself, shared by both modifier functions. The lambda-block overloads (`DropShadowScope`/`InnerShadowScope`) exist specifically so animated shadow properties don't force a modifier-chain recomposition.

**Residual uncertainty, stated plainly:** one earlier source (an Android-developer blog post describing an August-2025 preview build, predating the `1.9.0-rc01` snapshot checked above) shows a *different*, factory-style call shape — `DropShadow(15.dp, color = ..., spread = ..., alpha = ...)` as a standalone constructible type distinct from `Shadow`. This is almost certainly a pre-stabilization preview shape that got unified into the single `Shadow` class before RC/GA (the RC-pinned source is more authoritative and self-consistent with the current unversioned docs), but this was **not independently confirmed against a compiled 1.9.x jar or decompiled source this session** — no environment with the actual dependency available. **Do not copy the code below into production without first confirming the signature against the real 1.9.3 jar (IDE autocomplete or decompile) — budget 15–30 minutes for this at the start of the upgrade phase, per this project's own "verify before asserting" discipline.**

Desktop/JVM support: **HIGH confidence** — placement under the changelog's platform-agnostic "Multiple Platforms" heading (the same heading under which this project's other cross-platform APIs like `Brush`, `drawBehind`, and `PopupPositionProvider` all live) is the strongest available signal short of a hands-on compile; no Android-only gating language was found anywhere (contrast with AGSL/`RuntimeShader`, which prior `STACK.md` research confirmed IS explicitly Android-only).

Modifier-ordering rule (consistent across both sources checked): `dropShadow(...)` must precede `.background(...)` in the chain (shadow drawn behind the fill); `innerShadow(...)` must follow `.background(...)` (drawn on top, recessed into the shape). This matches `STACK.md`'s own pre-flagged rule.

**Minimal illustrative example** (final radius/alpha/offset values are a design decision for the implementation phase, not this research — do not treat these numbers as final):

```kotlin
private val AeroButtonShape = RoundedCornerShape(4.dp)

// Aero-style raised control surface: outer glow behind the fill, rim-light bevel on top.
fun Modifier.aeroRaisedSurface(fillBrush: Brush): Modifier = this
    .dropShadow(
        shape = AeroButtonShape,
        shadow = Shadow(
            radius = 6.dp,
            color = Color.Black.copy(alpha = 0.35f),
            offset = DpOffset(0.dp, 2.dp),
        ),
    )
    .background(brush = fillBrush, shape = AeroButtonShape)
    .innerShadow(
        shape = AeroButtonShape,
        shadow = Shadow(
            radius = 2.dp,
            color = Color.White.copy(alpha = 0.45f),
            offset = DpOffset(0.dp, 1.dp), // light from above -> highlight along top edge
        ),
    )

// Inset track groove (e.g. AeroSlider/AeroProgressBar trough): innerShadow only —
// no dropShadow, since a groove should read as recessed, never floating above the surface.
fun Modifier.aeroTrackGroove(trackColor: Color): Modifier = this
    .background(color = trackColor, shape = RoundedCornerShape(50))
    .innerShadow(
        shape = RoundedCornerShape(50),
        shadow = Shadow(
            radius = 3.dp,
            color = Color.Black.copy(alpha = 0.5f),
            spread = 1.dp,
            offset = DpOffset(0.dp, 1.dp),
        ),
    )
```

---

## PART 5 — Risk Register and Migration Plan

### Ranked risks (highest first), tied to concrete files

1. **RESOLVED / verified-safe (was flagged HIGH in the milestone brief, downgraded after direct audit): `Popup(...)` bare-overload ERROR-deprecation at CMP 1.10.0+.** All 14 call sites in this codebase already pass `properties = PopupProperties(...)` explicitly (verified by `Grep`, listed in Part 3 §4). **Zero action required at either target.**
2. **HIGH — `compose.material3` alias resolving to an ALPHA Material3 artifact if the project targets 1.10.x/1.11.x and leaves `api(compose.material3)` unchanged** (`library/build.gradle.kts:22`). Must be replaced with an explicit pinned coordinate before shipping. This is the single strongest "don't jump to latest stable" argument, independent of everything else — see Migration Sequencing below.
3. **MEDIUM-HIGH — `AeroPanelGroupRecomposeUiTest`'s coroutine-timing dependency, only if targeting 1.11.x** (Part 3 §6). The v1→v2 `runComposeUiTest` dispatcher-default change (Unconfined → Standard) is exactly the failure class this project has already been burned by once (the v2.0.3 false-positive sign-off). Non-issue at 1.9.3.
4. **MEDIUM — Kotlin bump 2.1.21 → ≥2.2.0, only if targeting 1.11.x.** Mechanical (single `libs.versions.toml` line, both `kotlin.jvm` and `compose.compiler` plugins version-ref the same catalog entry) but touches every compiled file transitively. Zero if targeting 1.9.3.
5. **LOW-MEDIUM — `kotlinx-datetime:0.6.2`, `api()`-exposed in picker signatures** (`library/build.gradle.kts:27`, consumer-visible). No forced bump found for the default (non-beta-M3) path at either target; verify empirically via the full picker test suite rather than trusting this in isolation.
6. **LOW — Skia milestone churn (m126→m138→m144), only relevant if targeting 1.11.x** and only if future `GlassModifiers.kt` work adds direct `org.jetbrains.skia.*` references (currently zero such references exist, per prior `STACK.md` audit).
7. **LOW — Foundation drag/hover/pointer APIs.** Zero changes found across the entire reviewed window at either target; the `PITFALL-03` pattern and `Modifier.aeroDragSplitter` should port unchanged.

### Win11 `undecorated=true` + `transparent=true` crash class (issue #3757) — status at target

Verified directly via GitHub REST API: issue **#3757 is `state: closed`, `state_reason: completed`, closed 2024‑07‑17** — the original reporter confirmed resolution by upgrading to Compose **1.6.11**/Kotlin 1.9.23, i.e. a version **older than this project's current 1.7.3 baseline**. This project has never set `transparent = true` (locked rule, `undecorated=true` WITHOUT `transparent=true`), so #3757 itself was never actually reachable in this codebase, at any CMP version.

**Newer reports in the same crash class, verified via direct YouTrack API query:** **SKIKO-1072** ("Fatal Crash EXCEPTION_ACCESS_VIOLATION in skiko-windows-x64.dll on AMD GPU (Direct3D)") — `created: 2025-11-13` (approx, from raw epoch), **`resolved: null`** as of last update **~2026-03-30** — i.e. **still open, unresolved, as of a date well after this project's 1.7.3 baseline and squarely within the CMP 1.9.x/1.10.x timeframe**. Explicitly reported against **Skiko 0.9.30** with a **transparent + undecorated window on an AMD GPU**. HIGH confidence this is a live, unresolved issue (direct API query, not a cached search snippet).

**Verdict: this crash class (GPU-driver/Direct3D interaction specifically with `transparent=true`) is not retired by any CMP version, including either upgrade target.** Since this project's locked rule keeps `transparent=false` unconditionally, **the risk is moot for this codebase regardless of which target is chosen — the upgrade neither increases nor decreases it.** Do not let a future milestone's window-chrome work assume this class of crash is "fixed upstream now" just because the CMP version moved.

### JitPack implications

`jitpack.yml` pins `openjdk17` (`sdk install java 17.0.10-tem`). **JDK 17 remains sufficient for both Kotlin 2.1.21 (current) and Kotlin ≥2.2.0 (needed only for the 1.11.x target)** — Kotlin/Compose compilation itself does not require a JDK bump on the JitPack build side at either target. **No `jitpack.yml` change is needed purely for the version bump.**

**Consumer-facing cost — explicit, because this is a PUBLISHED library (`com.github.Tolaseeq:aero-compose-ui` via JitPack):**

- `api(compose.material3)` and `api(libs.kotlinx.datetime)` are `api`-scoped in `library/build.gradle.kts` — every consumer app gets these on its OWN compile classpath transitively. Any Kotlin/Compose/M3 version floor this library adopts becomes a floor every downstream consumer app must also meet to compile against the new artifact.
- **If targeting 1.11.x:** every consumer must be on Kotlin ≥2.2.0 (matching or newer) to compile against the upgraded library artifact — a real, non-optional floor imposed on every downstream app, not just an internal implementation detail. This should be called out explicitly in release notes/README the moment this ships.
- **If targeting 1.9.3:** the consumer-facing Kotlin floor stays at ≥2.1.0 — **unchanged from today**. Materially smaller consumer-facing cost. This is one more concrete point in favor of 1.9.3 over 1.11.1 for this milestone.

### Recommended migration sequencing

**Target `CMP 1.9.3` (latest stable 1.9.x line), not `1.11.1` (latest stable overall), for this milestone.**

Rationale, consolidated:
- Delivers the one capability this milestone actually needs (`Modifier.dropShadow`/`innerShadow`, landed 1.9.0) at the smallest possible compatibility blast radius.
- **Zero** Kotlin bump (2.1.21 already satisfies ≥2.1.0), **zero** Gradle bump, **zero** JDK bump.
- `compose.material3` alias already resolves to a **stable** Material3 1.4.0 artifact at 1.9.1+ — no alpha-M3-in-a-published-library risk.
- Avoids the 1.11.0 test-infra v1→v2 `runComposeUiTest` deprecation + `Unconfined`→`Standard` dispatcher-default change entirely — directly protects `AeroPanelGroupRecomposeUiTest`, this project's only real regression guard for a bug it has already shipped a false-positive fix for once.
- Keeps the consumer-facing Kotlin floor unchanged (≥2.1.0) for every downstream JitPack consumer.
- Explicitly defers the 1.11.x jump (Kotlin 2.2 floor, alpha-M3-alias trap, v2 test-API dispatcher change) to a **later, separately-scoped** upgrade — consistent with this project's own demonstrated discipline of isolating one class of change per phase/milestone (e.g. PNL-HORIZ-01 shipped as a strictly additive follow-on rather than folded into Phase 13; every v2.0.x patch release stayed single-purpose).

**Sequencing: single-shot version bump (1.7.3 → 1.9.3 directly), not stepped through 1.8.x.** No breaking changes relevant to this repo were found in the 1.8.x line (full K2 transition, already the state this project is in); stepping through an intermediate tag buys no extra safety margin here, only extra CI cycles. **What should be "stepped" instead is verification, in this exact gate order:**

1. `./gradlew :library:compileKotlin` — cheapest, fastest-failing check for any source-incompatible removal.
2. `Grep -rn "Popup(" library/src/main` re-confirmation — already verified safe in this research (14/14 sites pass `PopupProperties` explicitly), re-run as a cheap regression check, not a fresh investigation.
3. Replace `api(compose.material3)` with an explicit pinned coordinate: `api("org.jetbrains.compose.material3:material3:1.9.0")` (this is exactly what the alias itself resolves to at 1.9.1+ anyway — making it explicit protects against a future accidental alias re-map surprise and is forward-hardening against the alias-deprecation trajectory).
4. `./gradlew :library:test` — full 232-test suite (12 pure-JVM `PanelGroupLogicTest` + all others) must be 100% green. **The `AeroPanelGroupRecomposeUiTest` result must be manually inspected, not just checked for "passed"** — confirm it still asserts exactly 1 header per section post-drag. This is a precaution at 1.9.3 (the underlying dispatcher is unchanged, so an actual failure here would be a genuine surprise, not an expected risk) — but given this project's own history, "still green" is not sufficient without a human glance at what it's asserting.
5. `./gradlew :showcase:run` smoke-launch across all three themes — confirms window chrome (`undecorated=true`, no `transparent`) and general rendering are unaffected. This is a regression-detection pass, not a new-feature sign-off — lighter weight than the eventual v2.1 visual sign-off.
6. Only after 1–5 are green: build a throwaway scratch composable (mirroring the Phase 7 enabling-phase precedent) to confirm the real `dropShadow`/`innerShadow` signature against the actual 1.9.3 jar before any `GlassModifiers.kt` production code depends on it (resolves Part 4's residual signature uncertainty empirically).

### Should the upgrade be its own phase, preceding all visual work?

**Yes — confirmed, not merely suspected.** Beyond the general "don't mix change classes" precedent already established by this project's Key Decisions log:

- It is categorically different from visual/drawing work (toolchain/dependency vs. rendering code) — mixing them makes it impossible to tell, if something breaks mid-milestone, whether the upgrade or the new Aero visuals caused it.
- It has independently, objectively verifiable exit criteria (compiles, 232 tests green, three-theme showcase launches unchanged) that can be fully satisfied *before* any new visual code exists.
- It directly protects this project's own hard-won lesson (`feedback_repro_must_exercise_path` memory) by giving the recompose-drag test focused scrutiny in isolation, not lost in the noise of simultaneous visual-code review.

**Suggested exit criteria for the upgrade phase:**

- [ ] `gradle/libs.versions.toml`: `composeMultiplatform = "1.9.3"`; `kotlin` unchanged at `2.1.21` — no other version line touched unless a compile/test failure demands it.
- [ ] `library/build.gradle.kts`: `api(compose.material3)` replaced with an explicit pinned `org.jetbrains.compose.material3:material3:1.9.0` coordinate.
- [ ] Full `library` test suite (232 tests) green, **with explicit human confirmation** that `AeroPanelGroupRecomposeUiTest` still asserts exactly 1 header per section post-drag (not merely "did not fail").
- [ ] `showcase` compiles and launches; three-theme (AeroBlue/AeroDark/Classic) smoke pass confirms no visual regression across the ~50 existing components.
- [ ] A throwaway scratch composable proves `Modifier.dropShadow`/`Modifier.innerShadow` compile and render correctly on Windows desktop with the signature documented in Part 4 (or documents the corrected real signature if it differs) — de-risks Part 4's residual uncertainty before `GlassModifiers.kt` production code depends on it.
- [ ] `jitpack.yml` unchanged (JDK 17 confirmed sufficient); verified by a successful JitPack build (throwaway pre-release tag, or deferred to the actual release step).
- [ ] Version bump + a CHANGELOG/README note documenting the consumer-facing Kotlin floor for downstream consumers (unchanged at ≥2.1.0 for the 1.9.3 target — still worth stating explicitly since this is the first time it's been documented anywhere).

---

## Sources

- GitHub Releases API, `api.github.com/repos/JetBrains/compose-multiplatform/releases` — full paginated version/date/prerelease-flag table (Part 1). HIGH confidence, machine-verified this session.
- Maven Central `maven-metadata.xml`, `repo1.maven.org/maven2/org/jetbrains/compose/compose-gradle-plugin/` — full coordinate list, cross-checked against the Releases API (Part 1).
- `JetBrains/compose-multiplatform` official `CHANGELOG.md` (raw, `master` branch, 6,875 lines) — read in full across all stable sections 1.7.3 through 1.11.1 plus prerelease sections through 1.12.0-beta02. Primary source for Parts 2, 3, and the Material3-alias finding. HIGH confidence — direct quotes, not paraphrase-of-paraphrase.
- `kotlinlang.org/docs/releases.html` — Kotlin release date table (Part 2). HIGH confidence.
- `kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html` (WebFetch) — general Kotlin-floor guidance ("≥2.1.0 for your projects, ≥2.2.20 for iOS/web"). MEDIUM confidence (WebFetch summary of a longer page, not the raw page text).
- WebSearch, "Kotlin 2.2 Gradle plugin minimum Gradle version" — Gradle 7.6.3–8.14 compatibility claim (Part 2). MEDIUM confidence, single-source, not independently cross-checked against a second primary source.
- `composables.com/docs/androidx.compose.ui/ui/1.9.0-rc01/modifiers/dropShadow` and the unversioned `.../modifiers/innerShadow`, `.../ui-graphics/classes/Shadow` — version-pinned third-party API mirror, primary basis for Part 4's signatures. MEDIUM-HIGH confidence (version-pinned, internally consistent across two independently-fetched pages, but third-party, not first-party androidx/JetBrains docs — official `developer.android.com`/`kotlinlang.org/api` pages returned 404/JS-render-blocked this session and could not be used directly).
- `android-developers.googleblog.com/2025/08/whats-new-in-jetpack-compose-august-25-release.html` — earlier preview-shape code example (Part 4, noted as likely superseded), confirms modifier-ordering rule (dropShadow before background, innerShadow after).
- GitHub REST API, `api.github.com/repos/JetBrains/compose-multiplatform/issues/3757` — direct query, `state: closed`, `state_reason: completed`, `closed_at: 2024-07-17` (Part 5). HIGH confidence, direct API query.
- YouTrack API, `youtrack.jetbrains.com/api/issues/SKIKO-1072` — direct query, `resolved: null`, created ~2025-11-13, updated ~2026-03-30, Skiko 0.9.30 / AMD GPU / Direct3D / transparent+undecorated window (Part 5). HIGH confidence, direct API query.
- This repository: `Grep` audits of `library/src/main` for `Popup(` (14 call sites, all verified to pass explicit `PopupProperties`), `ButtonDefaults`/`SliderDefaults` usage (3 call sites), `LocalMinimumInteractiveComponent*`/`rememberRipple` (zero matches). All HIGH confidence — direct source inspection, not inference.
- `.planning/research/STACK.md` (prior research, 2026-07-21) — origin of the 1.9.0 dropShadow/innerShadow finding and the now-superseded "don't upgrade" recommendation; its Skia-type-usage audit of `GlassModifiers.kt` (zero direct Skia references) is reused here in Part 3 §5 and Part 5 risk #6.

---
*Toolchain upgrade research for: aero-compose-ui v2.1 Glass Refinement*
*Researched: 2026-07-21*
