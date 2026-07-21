# Stack Research — v2.1 Glass Refinement

**Domain:** Compose Desktop (Kotlin) UI library — Aero-glass visual ornamentation primitives
**Researched:** 2026-07-21
**Confidence:** HIGH for what is available in 1.7.3 today; MEDIUM for the 1.9.0+ delta (verified via official changelogs, not hands-on tested against this codebase); LOW flagged explicitly where noted

## TL;DR / Recommendation

**Do not upgrade Compose Multiplatform for this milestone.** Everything the eight components need — two-tone fill, proportional top gloss, inner bevel / rim light, outer glow, track groove, colored gradients — is achievable **today, on 1.7.3, with zero new Gradle dependencies**, using plain `Brush` gradients plus manual layered `drawRoundRect` calls inside the existing single-`drawBehind` blocks in `GlassModifiers.kt`. The one genuinely new capability an upgrade would unlock — `Modifier.dropShadow`/`innerShadow` — landed in **Compose Multiplatform 1.9.0** (2025‑09‑16), four minor versions from where this project sits, and is not required: a hand-drawn double-stroke bevel achieves the same "inset rim light" read at lower risk and zero extra composition cost. See "Upgrade Question" below for the full reasoning.

## Recommended Stack — No Additions

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 2.1.21 (unchanged) | language | Already satisfies every downstream requirement below |
| Compose Multiplatform (Desktop) | 1.7.3 (unchanged) | UI/rendering | Sufficient for all v2.1 visual asks — see per-question findings |
| `androidx.compose.ui.graphics.Brush` | ships with `compose.ui` (already `api`) | gradients | `verticalGradient`/`radialGradient`/`sweepGradient` with explicit `colorStops` — stable since Compose 1.0, zero cost beyond interpolation |
| `androidx.compose.ui.graphics.drawscope.drawIntoCanvas` + `Canvas.nativeCanvas` | ships with `compose.ui` | sanctioned Skia escape hatch | Public, non-experimental API; needed only if a true Gaussian blur glow is pursued (optional, see Q1/Q4) |
| `org.jetbrains.skia.*` (transitive via `compose.desktop.common`) | whatever Skiko/Skia build ships inside CMP 1.7.3 (Skia Milestone 126) | `MaskFilter`, `Paint` for optional blur glow | Already on the classpath — **no new Gradle coordinate needed** even for this |

No new `dependencies { }` entries are proposed. Everything below is either already transitively present or is a coding pattern against APIs already in `library/build.gradle.kts`.

---

## Q1 — Shadows / Glow

**What works today in Compose Multiplatform 1.7.3 (Skia Milestone 126):**

| Approach | Available in 1.7.3? | Desktop-backed? | Notes |
|---|---|---|---|
| `Modifier.shadow(elevation, shape, clip, ambientColor, spotColor)` | Yes (since Compose 1.0) | Yes — Skia-native (`SkShadowUtils`-style ambient+spot two-light shadow, not the Android-RenderNode path) | Confidence MEDIUM: the `ambientColor`/`spotColor` "Android 9+ only" caveat in the official docs is an **Android-platform** limitation (pre-P had no color control); Compose Desktop's shadow renderer is a separate Skia implementation and is generally reported to honor both colors. Verify empirically once implemented — do not assume parity with a specific Android API level restriction, it doesn't apply here. |
| `Modifier.dropShadow(shape, DropShadow(radius, color, ...))` / `Modifier.innerShadow(shape, InnerShadow(...))` | **No — lands in Compose Multiplatform 1.9.0** (2025‑09‑16), package `androidx.compose.ui.graphics.shadow` | Yes, ships in `compose.ui` cross-platform (not Android-only) | This is the API that gives arbitrary-shape, colored, spread-controllable shadows and true recessed "inner shadow" — exactly the "inner bevel / inset rim light" ask. **Not available at 1.7.3.** Ordering rule when it does land: `dropShadow()` must precede `.background()`; `innerShadow()` must follow it. |
| Manual layered `drawRoundRect` (no blur filter) | Yes, always | Yes, pure-Compose, zero Skia types | Draw 2–4 progressively larger/lower-alpha rounded rects behind the real shape (or progressively smaller/higher-alpha rects on top for an inset bevel edge). This is a fake "spread" — genuinely how most hand-rolled Compose glow effects are done pre-1.9. **Recommended default for this milestone.** |
| Manual `drawIntoCanvas { nativeCanvas }` + `Paint().asFrameworkPaint().maskFilter = BlurMaskFilter...` | Compose-Desktop's `Paint.asFrameworkPaint()` returns `org.jetbrains.skia.Paint`, which has `maskFilter: MaskFilter?` — Skia's equivalent is `org.jetbrains.skia.MaskFilter.makeBlur(FilterBlurMode, sigma)` (not literally a class named `BlurMaskFilter` on desktop — that's the Android name; the Skia/desktop API is `MaskFilter.makeBlur`) | Yes — true Gaussian blur shadow, single extra `drawRoundRect` call using the Skia-typed `Paint`, can be issued **inside the existing `drawIntoCanvas` block** alongside other draws | Gives a real soft outer glow closer to what `dropShadow` will eventually give, without upgrading. Costs: couples `GlassModifiers.kt` to `org.jetbrains.skia` types (see Q4). |
| `RenderEffect` (`androidx.compose.ui.graphics.RenderEffect` / `graphicsLayer.renderEffect`) | Available since Compose 1.5-ish, desktop-backed via Skia `ImageFilter` | Yes | This is for *layer-level* filters (e.g., blurring an entire composable's rendered output), not for drawing a shadow shape. Overkill and a separate composited layer for what is a simple ornamental stroke/glow — not recommended here (see Q2/perf note). |

**Bottom line for Q1:** `Modifier.shadow` works on desktop today but is coarse (no spread, single elevation-driven blur/offset curve) and — critically — **allocates its own graphics layer**, which is an extra composited pass per component instance. Given the locked "single `drawBehind`, minimize overdraw" performance baseline (iGPU regression precedent), **do not wire `Modifier.shadow` into hot, frequently-recomposing controls** (button hover/press, slider thumb drag). Prefer the manual layered-`drawRoundRect` glow (no blur filter, cheapest, zero Skia types) as the default; reserve the `nativeCanvas` + `MaskFilter.makeBlur` variant only if a design checkpoint judges the flat-layer glow visually insufficient.

---

## Q2 — Blur

| Question | Answer |
|---|---|
| Is `Modifier.blur(radius, edgeTreatment)` supported on Compose Desktop? | Yes (HIGH confidence). `androidx.compose.ui.draw.blur` is a cross-platform `compose.ui` API. On Android it's gated to API 31+ / falls back to a software blur below that; **on Desktop there is no such gate** — Skia supports blur natively regardless of OS version, so it works uniformly on Windows/Linux/macOS. |
| Backing mechanism | `RenderEffect`/`ImageFilter` via Skia, same mechanism third-party libraries (Haze, Cloudy) use for desktop blur — corroborated by multiple independent sources: "iOS, Desktop, and all Web targets in Compose Multiplatform use Skia... All Skia-backed platforms use runtime shader / `ImageFilter.makeBlur` for blur." |
| Achievable "frosted glass" WITHOUT true backdrop sampling | `Modifier.blur` blurs **the composable's own rendered content**, not what's behind it — it cannot sample the window/desktop behind your app (that would require real DWM/compositor backdrop capture, explicitly out of scope per project constraints). So a "frosted" look here means: blur a **synthetic noise/gradient texture you draw yourself** (e.g., blur a subtle two-tone gradient layer), not a live backdrop. This is consistent with the project's existing approach (`glassEffect`/`glassPanel` already simulate glass via alpha + gradient, not real blur) and requires no new capability — just optionally adding a blurred synthetic layer for extra softness. Given the perf baseline, this is **not recommended** as a default (an extra `graphicsLayer`+blur per glass surface is real GPU cost); the existing alpha/gradient simulation is cheaper and already validated across three themes. |

**Recommendation:** Skip `Modifier.blur` entirely for v2.1. It solves a problem ("frosted background") this milestone doesn't have — the eight components need surface *ornamentation* (gloss, bevel, glow), not translucency-over-content. Flag it as available if a future milestone wants softer panel edges.

---

## Q3 — Brushes

| API | Available in 1.7.3? | Cost | Aero use |
|---|---|---|---|
| `Brush.verticalGradient(colors, startY, endY)` / `Brush.verticalGradient(colorStops = arrayOf(0f to c1, 0.4f to c2, ...))` | Yes, stable since Compose 1.0 | Cheap — CPU-side interpolation setup, GPU shader is a trivial built-in Skia gradient shader | Already used in `glassPanel`/`glassSurface`/`glassEffect`. The **proportional endY bug** (`glassSurface` hardcodes `endY = 100f` px instead of `size.height * fraction`) is a direct, in-scope fix using this exact API — no new capability needed, just correct usage (mirrors the `glassPanel` pattern which already does `endY = size.height * 0.55f` correctly). |
| `Brush.radialGradient(colors, center, radius)` | Yes, stable | Cheap | Good for a specular "hotspot" highlight (curved gloss look) on rounded thumbs/buttons — cheaper and simpler than a shader-based specular effect. |
| `Brush.sweepGradient(colors, center)` | Yes, stable | Cheap | Less relevant to Aero chrome; useful if ColorPicker's existing hue ring needs touching (out of scope — that component isn't in the eight). |
| `ShaderBrush(shader: Shader)` | Yes, stable, cross-platform | Cheap once the `Shader` is built | On desktop, `androidx.compose.ui.graphics.Shader` is a `typealias` for `org.jetbrains.skia.Shader` — this is *not* Android-only. |
| AGSL runtime shaders | **Android-only.** AGSL (`RuntimeShader`) is an Android 13+ (API 33+) proprietary shading API and has zero presence on Desktop. Confirmed explicitly: "Android has its own implementation because it uses Android's proprietary RuntimeShader API (AGSL) which is unavailable on other platforms." | — | **Flag: do not reference AGSL syntax/APIs anywhere in this codebase — they will not compile/run for a JVM/Desktop target.** |
| Desktop equivalent of AGSL: SkSL via `org.jetbrains.skia.RuntimeEffect` | Yes, works **today** in 1.7.3 (confirmed via a working 2021-era Compose-Desktop example that predates even Compose 1.0-stable, i.e., this pattern has been stable for the entire life of Compose Desktop) | `RuntimeEffect.makeForShader(sksl)` compiles once (cache with `remember{}`); `.makeShader(uniforms, children, localMatrix, isOpaque)` is cheap per-frame; GPU executes the fragment shader per pixel | This is the real "desktop AGSL." A worked pattern: `ShaderBrush(runtimeEffect.makeShader(...))` used directly with `drawRect(brush = ..., ...)` inside `drawBehind` — no extra composited layer, fits the single-pass constraint. **Not recommended for v2.1** — none of the eight components' asks (two-tone fill, gloss, bevel, glow, groove) require procedural shading; gradients + layered rects cover it. Note for later: JetBrains added an official `.asComposeShader()` wrapper for Skia shaders in **Compose Multiplatform 1.11.0+** — before that (i.e., at 1.7.3), the direct `ShaderBrush(skiaShader)` constructor path shown above is what's used, and it is a real, working, if less "officially paved," pattern. |

**Bottom line for Q3:** Everything Aero ornamentation needs is a `Brush.verticalGradient`/`radialGradient` combination. SkSL/RuntimeEffect is available but is architecturally unnecessary complexity for this milestone — explicitly **out of scope**, revisit only if a future milestone wants an animated sheen/noise texture that gradients genuinely can't express.

---

## Q4 — skiko Interop

| Question | Answer |
|---|---|
| Is `drawIntoCanvas { it.nativeCanvas }` the sanctioned escape hatch? | Yes — it's a public, non-experimental `compose.ui` API (`androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCanvas`), used in JetBrains' own Compose-Desktop example articles. HIGH confidence. |
| What does `org.jetbrains.skia.*` cost, stability-wise? | It is **not** part of Compose's stable public contract — it's the underlying rendering engine, and its Kotlin binding shape moves with each Skia Milestone bump that Compose Multiplatform pulls in transitively (this project's 1.7.3 pulled Skia M116→M126 within the 1.7.x line alone). There is a **documented precedent for this breaking real projects**: a Compose Multiplatform 1.11.0-alpha bump caused a `NoSuchMethodError` in a third-party blur library (Haze) specifically because a Skia API changed shape underneath it. Any code in this library that touches `org.jetbrains.skia.*` types directly inherits that same fragility on the *next* Compose upgrade, whenever it happens. |
| Portability cost (Windows primary / Linux+macOS secondary)? | Low. Skia is the renderer on **all three** desktop OS targets in Compose Multiplatform — `compose.desktop.common` already pulls the Skiko native binaries for every desktop platform transitively. Unlike a hypothetical native DWM/WinAPI blur call, `nativeCanvas`/`MaskFilter` code is not Windows-specific and will run identically on Linux/macOS. |
| No new Gradle dependency needed? | Correct — `org.jetbrains.skiko`/`org.jetbrains.skia` classes are already on the compile classpath transitively via `compose.desktop.common`/`compose.ui`. Using them requires zero new `libs.versions.toml` entries. |

**Recommendation:** If the blur-glow variant from Q1 is pursued, **isolate all `org.jetbrains.skia` type references to one narrow internal helper** in `GlassModifiers.kt` (e.g. `private fun DrawScope.drawBlurredGlow(...)`) rather than letting Skia types leak into the modifier's public surface or spread across multiple call sites. This bounds the blast radius of the next Compose upgrade to a single function, consistent with how this project has always isolated its own risky internals (e.g., `PanelDistribution.kt` kept pure/Compose-free specifically to bound risk). Given the layered-`drawRoundRect` (no-blur) approach from Q1 already achieves a workable glow with **zero** Skia types, treat the `nativeCanvas` path as an opt-in escalation, not a default.

---

## Q5 — Upgrade Question

**Concrete version deltas (verified via GitHub Releases API, dates in this project's actual timeline):**

| Version | Published | Relevant to this milestone |
|---|---|---|
| **1.7.3** (current) | — | Skia Milestone 126. Baseline. |
| 1.8.x | ~early/mid 2025 | Requires Kotlin ≥ 2.1.0 (project's 2.1.21 already satisfies this — no Kotlin bump needed to reach 1.8/1.9). Full K2 compiler transition. |
| **1.9.0** | **2025‑09‑16** | **`Modifier.dropShadow()` / `Modifier.innerShadow()` land** (package `androidx.compose.ui.graphics.shadow`), cross-platform including Desktop. This is the only capability gap identified in Q1–Q4 that a version bump would actually close. |
| 1.9.3 | 2025‑11‑06 | Latest 1.9.x patch. |
| 1.10.x | Dec 2025 – Mar 2026 | No graphics capability identified in this research as relevant to v2.1's asks. |
| 1.11.0 / 1.11.1 | 2026‑05‑13 / 2026‑06‑02 | **Latest stable** as of this research date (2026‑07‑21). Introduces `.asComposeShader()` official Skia-shader wrapper (irrelevant here, see Q3) and `ComposePanel.renderSettings` (irrelevant, this project doesn't embed `ComposePanel`). |
| 1.12.0-beta02 | 2026‑07‑14 | Bleeding edge, not stable. |

**Verdict: NO, do not upgrade for this milestone.**

Reasoning:
1. **No hard blocker exists at 1.7.3.** Every visual device the eight components need (two-tone fill, proportional gloss, rim-light bevel, outer glow, track groove) is buildable today with `Brush` + manual `drawRoundRect` layering inside the existing single-`drawBehind` pattern. `dropShadow`/`innerShadow` would be a *convenience* (less hand-rolled geometry for the inner-bevel effect specifically) — not a capability the milestone cannot ship without. A manual two-stroke bevel (lighter 1px stroke offset toward the light source, darker 1px stroke offset away from it) is a well-established, cheap way to fake an inset rim light without `innerShadow` at all.
2. **The jump is non-trivial even to just reach 1.9.0** (1.7.3 → 1.8.0 → 1.9.0, two minor releases, ~a year of upstream churn as of this research date), and the project's own tests exercise `compose.uiTest`/`runComposeUiTest` under `@OptIn(ExperimentalTestApi::class)` — experimental APIs are exactly the surface most likely to shift shape across minor Compose releases. The existing `AeroPanelGroupRecomposeUiTest` (a deterministic programmatic-drag regression guard, the *only* thing standing between this codebase and a repeat of the v2.0.3 false-positive-signoff failure) would need to be re-verified end-to-end after any Compose bump — real regression-testing surface, unrelated to this milestone's actual goal.
3. **This milestone is explicitly visual-only** ("Функционал и публичный API не меняются" — behaviour and public API are locked). Bundling a cross-cutting dependency bump into a pure-visual milestone contradicts the project's own demonstrated discipline of isolating one class of change per milestone (evidenced by the entire v2.0.x patch-release history, and by the `orientation` param being introduced as a strictly additive change in 13.1 rather than folded into 13).
4. **Regression risk on the locked Win11 workaround is real but orthogonal, and upgrading does not retire it.** The `undecorated=true` + `transparent=true` crash (issue #3757) was reported by its original filer as resolved by upgrading from an unspecified older version to **Compose 1.6.11** back in mid-2024 — i.e., already fixed in a version *older* than this project's current 1.7.3. But this crash class is a recurring, GPU-driver-dependent native fault (`Failed to create DirectX12 device`, `EXCEPTION_ACCESS_VIOLATION` in `skiko-windows-x64.dll`), and near-identical reports continue to surface on newer Compose/Skiko builds against specific AMD/driver combinations (e.g., a still-open Skiko issue, SKIKO-1072, describing the same fault on AMD/Direct3D). **No Compose version number "fixes" this category of bug outright** — it is fundamentally about GPU driver/DirectX interaction with `transparent=true`, not something the version bump changes. Since this milestone never touches window transparency (`undecorated`/`transparent` params are untouched; glass stays simulated in-window via `drawBehind`), the risk is moot for v2.1 specifically — but it means an eventual future upgrade decision should not be justified on the assumption that #3757-class crashes are "fixed upstream now."

**If/when a future milestone wants `dropShadow`/`innerShadow` specifically:** budget it as its own small spike — bump to 1.9.3 (latest 1.9.x patch) rather than jumping straight to 1.11.x, re-run the full test suite (232 tests + the recompose-drag guard) as the primary acceptance gate, and only then adopt the new shadow modifiers. Do not combine that spike with further visual work in the same phase.

---

## Q6 — Testing

| Tool | Desktop support? | Verdict |
|---|---|---|
| **Roborazzi** | **No.** Built on Robolectric, which simulates the **Android** framework on the JVM — there is no "Desktop" target concept for it to render. | Not usable. |
| **Paparazzi** | **No.** Renders via Android's `layoutlib` (the same engine Android Studio's preview uses) — again, Android-only, no Desktop rendering path exists. | Not usable. |
| `SemanticsNodeInteraction.captureToImage()` under `compose.uiTest` / `runComposeUiTest` | **Yes — already in this project's dependency graph** (`testImplementation(compose.uiTest)` + `compose.desktop.currentOs`, added in Phase 14 for the recompose-drag guard). `captureToImage()` is a common (cross-platform) test API and works under `runComposeUiTest` on Desktop, returning an `ImageBitmap` you can convert to a Skia bitmap and `encodeToData(EncodedImageFormat.PNG)` to get PNG bytes. | This is the only viable path for pixel-level verification on Compose Desktop today — no dedicated framework wraps it. |

**What this means concretely for v2.1:**

- There is **no drop-in golden-image / screenshot-regression framework for Compose Desktop** as of this research (2026‑07‑21) — every public option (Roborazzi, Paparazzi, Android Studio's Compose Preview Screenshot Testing) is Android-only, and no clear community-standard equivalent for Desktop was found (flagged **LOW confidence / gap** — this is a "didn't find" result, not a "confirmed absence," so it's worth a 10-minute check before committing to a testing plan, but budget for the answer being "hand-roll it").
- The pragmatic options are: (a) hand-roll a thin `captureToImage()` + PNG-diff test-only helper (a few dozen lines, test-scope only so it doesn't touch the "zero new runtime deps" constraint at all), or (b) skip pixel-diffing and keep using `captureToImage()` purely as a **smoke check** (assert the node renders without throwing, assert dimensions/non-blank pixels) rather than full golden-image comparison.
- **Recommendation for this milestone: don't introduce automated pixel-diff screenshot tests.** Skia-rendered anti-aliasing and font hinting can differ subtly across machines/GPU drivers (the same class of variance implicated in the `#3757` driver-dependent crash reports above), which makes byte-exact or even tolerance-based pixel diffing a real flakiness risk for a CI-less, single-maintainer project. This project's own proven acceptance gate for visual work is the **three-theme human sign-off checklist**, used successfully (with real defects caught) across every prior milestone including the two most recent (v2.0.2, v2.0.4) — reuse that pattern for the eight components rather than introducing a new, unproven, and Skia-fragile automated-visual-testing dependency this milestone doesn't need. Keep `runComposeUiTest`/`captureToImage()` in reserve for **structural** assertions only (e.g., "does the hover state modifier chain apply," "does clip actually bound the highlight to the rounded shape" — testable via semantics/bounds, not pixels), matching the existing `AeroPanelGroupRecomposeUiTest` precedent of asserting counts/structure rather than images.

---

## Integration Points into `GlassModifiers.kt`

Concrete, version-verified fixes/additions available right now at 1.7.3, mapped to the baseline defects STATE.md already identified:

| Defect / Gap (from STATE.md baseline) | Fix using APIs verified above |
|---|---|
| `glassEffect(elevation)` param is dead (`shadow` imported, never applied) | Either wire `Modifier.shadow(elevation, shape, ...)` in **only** low-frequency-recompose contexts (e.g., popups/dialogs, not per-frame hover state), OR replace with a manual layered-`drawRoundRect` glow drawn inside the same `drawBehind` pass — preferred, since it keeps the single-pass perf baseline for the hot-path components (buttons, switch, sliders) that are in scope. |
| `glassSurface`'s gloss gradient hardcodes `endY = 100f` px | Change to `endY = size.height * <fraction>` — same `Brush.verticalGradient` API, already proven correct in `glassPanel` (`0.55f`). Zero new capability needed, this is a straight bug fix using an API already in use two functions above it in the same file. |
| `glassSurface`'s `drawBehind` runs before `.clip(shape)`, border's outer half gets clipped | Reorder: `.clip(shape)` before the `drawBehind` stroke, or draw the stroke `inset` by half its width so the clip doesn't bisect it — pure Compose Modifier ordering fix, no new API. |
| Missing: outer glow / drop shadow | Layered `drawRoundRect` (Q1 default) or `nativeCanvas` + `MaskFilter.makeBlur` (Q1 escalation) — both available today, zero new deps. |
| Missing: bottom reflection (two-tone split) | `Brush.verticalGradient(colorStops = ...)` with an explicit stop at the split point — already-available API, just a new token/usage in `AeroColorScheme`. |
| Missing: inner bevel / inset rim light | Two offset 1px strokes (light toward virtual light source, dark away from it) via `drawRoundRect(style = Stroke(...))` — same primitive already used for the existing 1.dp rim in `glassEffect`/`glassSurface`, just applied twice with an offset. `Modifier.innerShadow` would do this more declaratively but isn't available until 1.9.0 (see Q5) — not needed to ship this. |
| Missing: specular curved gloss | `Brush.radialGradient` positioned off-center — already-available API. |
| Missing: noise texture | Out of scope for v2.1 per this research — would genuinely benefit from a shader (Q3) or a pre-baked texture asset, both disproportionate to "Aero spirit, modern execution" fidelity target; explicitly **do not add**. |
| Missing: horizontal gradient variant | `Brush.horizontalGradient` — same family of API as the vertical ones already in use, zero new capability. |

---

## What NOT to Add (explicit)

| Avoid | Why | Use instead |
|---|---|---|
| `Modifier.dropShadow` / `Modifier.innerShadow` | Not available at 1.7.3 (lands 1.9.0); pulling them in means upgrading Compose Multiplatform, which this research recommends against for this milestone (Q5) | Manual layered `drawRoundRect` / double-stroke bevel |
| AGSL / `RuntimeShader` (Android API) | **Does not exist on Desktop at all** — will not compile against a JVM/Desktop target | SkSL via `org.jetbrains.skia.RuntimeEffect` if ever needed (not needed for v2.1) |
| `Modifier.blur` for a "frosted" look | Blurs the composable's own content, not what's behind it; cannot simulate real backdrop blur (out of scope per project constraints); adds a composited layer + real GPU cost for a look already achieved cheaper by the existing alpha/gradient simulation | Existing `glassSurface`/`glassPanel`/`glassEffect` alpha+gradient pattern |
| Scattering `org.jetbrains.skia.*` types across multiple call sites/public modifier signatures | Documented breakage precedent when Skia's shape shifts under a Compose version bump (Haze's `NoSuchMethodError` on CMP 1.11.0-alpha04); increases future-upgrade blast radius | If used at all (Q1 blur-glow escalation), isolate to one narrow `private`/`internal` helper function |
| A new screenshot-regression testing dependency (Roborazzi/Paparazzi or a hand-picked third-party image-diff library) | Both flagship options are Android-only and unusable; no verified Desktop-native community-standard equivalent was found; pixel-diffing Skia output is a documented source of cross-machine flakiness | `captureToImage()` for structural smoke tests + the proven three-theme human sign-off checklist for actual visual acceptance |
| SkSL/`RuntimeEffect` procedural shaders for any of the eight components | Available (Q3) but unnecessary complexity/coupling for gradient-achievable effects; none of the eight components' Aero gaps require procedural shading | `Brush.verticalGradient`/`radialGradient` layering |
| A full Compose Multiplatform version bump to "latest" (1.11.x) bundled into this milestone | Large, unrelated dependency-upgrade surface mixed into a locked visual-only milestone; real regression-testing cost on the experimental `compose.uiTest` surface and the recompose-drag guard; does not retire the Win11 transparency crash risk class anyway | Stay on 1.7.3 for v2.1; treat a targeted 1.9.x bump as its own future spike if `dropShadow`/`innerShadow` become worth the cost later |

---

## Version Compatibility

| Package A | Compatible With | Notes |
|---|---|---|
| Kotlin 2.1.21 | Compose Multiplatform 1.7.3 (current) and up through at least 1.8.0+ (which requires Kotlin ≥ 2.1.0) | No Kotlin bump would be forced even by a future 1.9.x adoption |
| `org.jetbrains.skia`/`skiko` types used directly | Skia Milestone 126 (bundled with CMP 1.7.3) | Any direct Skia-typed code (Q1/Q4) is implicitly pinned to this Skia milestone's binding shape; a future Compose bump can silently change/rename these APIs (precedent: Haze `NoSuchMethodError` on a CMP 1.11.0-alpha Skia bump) |
| `compose.uiTest` (`@OptIn(ExperimentalTestApi::class)`) | Already used since Phase 14; stable enough for this project's existing 232-test suite on 1.7.3 | Experimental annotation is a real signal — don't assume forward source-compatibility across a Compose version bump without re-running the suite |

## Sources

- Context7 `/jetbrains/compose-multiplatform` — queried for shadow/blur/RenderEffect desktop support, skiko/RuntimeEffect shader interop, `runComposeUiTest`/`captureToImage` desktop usage. HIGH confidence for `GraphicsLayer` (1.7.0), Skia Milestone 126 bump (1.7.3), `CanvasLayersComposeScene` API shape.
- https://kotlinlang.org/docs/multiplatform/whats-new-compose-170.html — official 1.7.x changelog (GraphicsLayer, Skia M126)
- https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html and https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.9.0 — official confirmation `dropShadow()`/`innerShadow()` land in 1.9.0, cross-platform (not Android-only), with modifier-ordering rules
- https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html — Kotlin/CMP version compatibility matrix (CMP 1.8.0+ requires Kotlin ≥ 2.1.0)
- GitHub Releases API (`api.github.com/repos/JetBrains/compose-multiplatform/releases`) — ground-truth version/date table used in Q5 (1.9.0 = 2025‑09‑16, 1.11.1 = 2026‑06‑02 latest stable, 1.12.0-beta02 = 2026‑07‑14 bleeding edge as of this research date)
- https://github.com/JetBrains/compose-multiplatform/issues/3757 + GitHub REST API (`state: closed`, `state_reason: completed`, closed 2024‑07‑17) — original reporter confirmed resolution by upgrading to Compose 1.6.11/Kotlin 1.9.23; MEDIUM confidence this is fully "fixed" as a class of bug, given corroborating still-open/recent reports below
- https://github.com/JetBrains/skiko/issues/327 ("Failed to create DirectX12 device when transparent = true"), YouTrack SKIKO-1072 (AMD/Direct3D `EXCEPTION_ACCESS_VIOLATION`, recent) — corroborates the crash class is GPU-driver-dependent and recurs across Compose versions, not fully retired by any single version bump
- https://www.pushing-pixels.org/2021/09/22/skia-shaders-in-compose-desktop.html and https://www.pushing-pixels.org/2022/04/09/shader-based-render-effects-in-compose-desktop-with-skia.html — worked `ShaderBrush(skiaShader)`/`drawIntoCanvas{nativeCanvas}` patterns, confirmed pre-dating and still applicable to 1.7.3; MEDIUM confidence (community/blog source, not official docs, but pattern is simple and directly demonstrated against public APIs)
- WebSearch: Haze (chrisbanes/haze) and Cloudy (skydoves/Cloudy) library docs/READMEs — corroborate Skia-backed blur on Desktop, AGSL-is-Android-only, `.asComposeShader()` landing in CMP 1.11.0+; MEDIUM confidence (third-party library docs, but consistent across two independent sources)
- WebSearch: Roborazzi/Paparazzi comparison articles — corroborate both are Robolectric/layoutlib (Android-only), no Desktop rendering path found in any source; confidence MEDIUM-HIGH (consistent across multiple independent sources, but no single official "Paparazzi does not support Desktop" statement was directly quoted)
- https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/shadow/package-summary.html — attempted fetch for exact `DropShadow`/`InnerShadow` constructor signatures; page did not yield full parameter list in this session. **Flagged LOW confidence / gap**: exact `DropShadow`/`InnerShadow` constructor parameters (beyond `radius`/`shape`/`color` seen in example code) should be re-verified directly if/when a future milestone actually adopts 1.9.0+.
- https://composables.com/docs/androidx.compose.ui/ui/modifiers/shadow — `Modifier.shadow` parameter list (`elevation`, `shape`, `clip`, `ambientColor`, `spotColor`); confirms the "Android 9+" color caveat is documented as Android-specific, not desktop-specific (used to support the Q1 MEDIUM-confidence claim that desktop shadow coloring works)

---
*Stack research for: aero-compose-ui v2.1 Glass Refinement*
*Researched: 2026-07-21*
