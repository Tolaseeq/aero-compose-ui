# Phase 20: Verification - Research

**Researched:** 2026-07-28
**Domain:** Kotlin/Compose Multiplatform Desktop — source-scan regression gates, value-level and
`runComposeUiTest` measurement tests, showcase composition, JitPack-based external consumer proof,
human multi-theme visual sign-off.
**Confidence:** HIGH — every finding below is a direct read of this repository's own shipped code,
tests, and git history (tags/commits). No external library research was required; this phase adds
zero new dependencies and zero new frameworks.

## Summary

Phase 20 writes no new component code — it writes **three new gate tests**, **one value-level
contrast-regression test**, **one snapshot/measurement test against a git tag baseline**, **one new
showcase section**, **a small color-resolution function + its wiring into two existing files**, and
**one scratch-consumer project living outside this repository**. Every one of these has a direct,
literal precedent already in the codebase: six `*SourceTest.kt` files show the exact shape for the
three new gates (VER-01/VER-02 need two new files following this shape, D-09); the retired
`AeroSegmentedControlStylesTest.kt` (recoverable via `git show c35f883~1:...`) contains a
ready-to-reuse WCAG `contrastRatio()` function built on Compose's own `Color.luminance()` — exactly
what D-13's new contrast test needs; `AeroPanelGroupRecomposeUiTest.kt` and
`AeroButtonSemanticsTest.kt` are the two `runComposeUiTest` precedents D-11's measurement test and
VER-04's (already-closed) keyboard test rely on; and 15-06's JitPack throwaway-tag pattern is the
exact mechanism D-14 asks to reuse for VER-05, except this time the consumer must be a *new, separate
project* that renders the components, not merely a compile-only re-proof of the library itself
(15-06 never built or ran a consumer — it only confirmed the library artifact itself builds
externally).

The two new gates (VER-01, VER-02) are provably safe to write today: a direct grep across all
`Brush.*Gradient` call sites in the library (18 sites, 9 files, matching D-06's cited count exactly)
shows every explicit-stop gradient already uses a `size.*`-relative fraction, and the two clip-order
bypasses D-07 names are absent from every shipped `aeroSurface(`/`aeroGlowRing(` call site examined.
Both gates are expected to be GREEN on day one — their fail-then-pass proof (VER-06/D-08) must
therefore come from an in-file fixture string (a hand-written violating snippet asserted against the
detector function), never from temporarily breaking real source, exactly as D-08 specifies.

**Primary recommendation:** For VER-01/VER-02, write two new `*SourceTest.kt` files under
`library/src/test/kotlin/com/mordred/aero/theme/` (the primitives package, since both gates are
library-wide per D-06, not scoped to `components/buttons/`), extracting each detector as a small
pure function over a `String` (source text) that is unit-tested directly against a hardcoded
violating string AND a hardcoded compliant string (D-08's fixture-based proof), then separately
applied to the real files' text. Reuse the `nonCommentSource` filtering helper from
`AeroSwitchSourceTest.kt` for any check that could be tripped by a KDoc comment merely discussing
the forbidden pattern — this is not hypothetical: `AeroButtonSurface.kt`'s own KDoc contains the
literal substring `.clip()` in prose, and `GlassModifiers.kt` and `AeroSurfacePrimitives.kt` discuss
the historical `endY = 100f` bug in comments. A positional/textual gate that does not strip comments
first risks both false positives (comment text mentioning a banned pattern) and — more dangerously —
false negatives (a KDoc example showing the CORRECT pattern could mask a missing real check).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Gradient-literal / clip-order gates (VER-01/02) | Library (`library/src/test`) | — | Structural invariants over `library/src/main` source text; JVM unit tests, no UI runtime |
| Size/radius snapshot vs. baseline (VER-03) | Library (`library/src/test`) | — | `runComposeUiTest` measurement + value-level constant assertion, same tier as existing guards |
| Keyboard activation (VER-04) | Library (`library/src/test`) | — | Already closed by `AeroButtonSemanticsTest`; no new work |
| Contrast fix mechanism (D-12) | Library (`theme`/`components/buttons`) | `components/selection` (consumer) | Shared function owned by `AeroButtonSurface.kt`, imported by `AeroSegmentedControl.kt` — mirrors the existing `FILLED_FILL_TOP_DARKEN`/`PRESSED_INNER_SHADOW` cross-package pattern |
| Contrast regression test (D-13) | Library (`library/src/test`) | — | Value-level JVM test, no Compose runtime, mirrors the retired `AeroSegmentedControlStylesTest` |
| New "Verification" showcase section (SHW-15/D-04) | Showcase (Compose Desktop app) | — | Pure composition of already-shipped public components; no library changes |
| Scratch consumer (VER-05) | External project (outside this repo) | — | Deliberately outside showcase conventions; consumes the published JitPack artifact the way an outside developer would |
| Human three-theme sign-off (SHW-16) | Showcase (runtime, human-driven) | OS display settings (DPI) | Runtime theme toggle inside the showcase window; DPI scale is a Windows display-setting change, not a code path |

## Package Legitimacy Audit

**Not applicable.** This phase installs zero new external packages. `library/build.gradle.kts` and
`gradle/libs.versions.toml` are unmodified by any Phase 20 task — no `dependencies {}` block change
is in scope. The one external system touched is JitPack (D-14), which is this project's OWN publish
target (already configured, already proven in 15-06), not a new third-party dependency.

The scratch-consumer project (VER-05) is a *separate Gradle project outside this repository* — its
own `build.gradle.kts` will declare exactly one dependency:
`implementation("com.github.Tolaseeq:aero-compose-ui:<throwaway-tag>")` plus the same Compose
Multiplatform Desktop plugin/runtime the library itself requires (`org.jetbrains.compose`,
Kotlin `2.4.10`). Because this coordinate is the project's own published artifact (confirmed
buildable via JitPack in 15-06 `[VERIFIED: this repo's own git history + JitPack API/build-log,
15-06-SUMMARY.md]`), it does not need a `package-legitimacy check` run against an ecosystem registry
— it is not a third-party package discovered via search, it is the deliverable this milestone ships.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `com.github.Tolaseeq:aero-compose-ui` | JitPack (mirrors this repo) | this project | n/a (own artifact) | github.com/Tolaseeq/aero-compose-ui | OK | Approved — own artifact, not third-party |

**Packages removed due to [SLOP] verdict:** none.
**Packages flagged as suspicious [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
                     ┌─────────────────────────────────────────────┐
                     │   library/src/main (Phases 16-19, FROZEN)   │
                     │   AeroButtonSurface.kt / AeroSegmentedControl│
                     │   AeroSurfacePrimitives.kt / GlassModifiers  │
                     └───────────────┬───────────────────────────────┘
                                     │ source text (read, not executed)
                                     ▼
   ┌─────────────────────────────────────────────────────────────────────┐
   │  library/src/test  — VER-01/02/03/04 gates                          │
   │                                                                     │
   │  [source-scan]  VER01GradientProportionalitySourceTest              │
   │       reads *.kt across whole library ──► detector fn ──► assert    │
   │       fixture strings (violating + compliant) proven inline (D-08) │
   │                                                                     │
   │  [source-scan]  VER02AeroSurfaceClipOrderSourceTest                 │
   │       same shape, two detector fns (glow-before-surface,            │
   │       no-second-clip)                                               │
   │                                                                     │
   │  [runComposeUiTest]  VER03BaselineSizeSnapshotTest                  │
   │       composes each of the 8 components ──► measures node bounds    │
   │       ──► compares against literal constants copied from            │
   │       `git show v2.0.4:...` (D-10) ──► radii asserted at value level │
   │                                                                     │
   │  [runComposeUiTest]  AeroButtonSemanticsTest (EXISTING, unchanged)   │
   │       already closes VER-04 — re-run only, not re-authored           │
   └─────────────────────────────────────────────────────────────────────┘
                                     │ green suite
                                     ▼
   ┌─────────────────────────────────────────────────────────────────────┐
   │  library: contrast-fix mechanism (D-12)                              │
   │  AeroButtonSurface.kt: new fn resolveLabelColor(fillTop, fillBottom) │
   │     candidates = {near-black, near-white}; picks by WCAG contrast    │
   │     against the WORSE-contrast fill stop                             │
   │  AeroButtonSurface's Text + AeroSegmentedControl's Text both call it │
   │  ── AeroButtonContrastRegressionTest (D-13, value-level, reuses      │
   │     the retired AeroSegmentedControlStylesTest's contrastRatio() fn) │
   └───────────────────────┬───────────────────────────────────────────────┘
                           │
                           ▼
   ┌─────────────────────────────────────────────────────────────────────┐
   │  showcase/src/main — new VerificationSection() (SHW-15/D-04)         │
   │  ShowcaseApp.kt Column gains one more entry, title "Verification"    │
   │  composes all 8 already-shipped public components at rest,          │
   │  live ThemeSwitcher (already exists) drives the 3-theme toggle        │
   └───────────────────────┬───────────────────────────────────────────────┘
                           │
              ┌────────────┴─────────────┐
              ▼                          ▼
   ┌─────────────────────┐   ┌──────────────────────────────────────┐
   │ Human sign-off       │   │ VER-05: scratch consumer (external)   │
   │ (SHW-16, D-01/D-02)  │   │ new git-tag e.g. v3.0.0-verify01      │
   │ AeroBlue/Dark/Classic│   │ JitPack pulls it into a brand-new,    │
   │ + AeroBlue @125%/200%│   │ separate Gradle project (not in this  │
   │ DPI (Windows display │   │ repo) that launches a Window and      │
   │ scaling, no code)    │   │ renders all 8 components (D-15)       │
   └─────────────────────┘   └──────────────────────────────────────┘
```

### Recommended Project Structure

No new modules or top-level folders. New files land inside existing package boundaries:

```
library/src/test/kotlin/com/mordred/aero/theme/
├── VER01GradientProportionalitySourceTest.kt   (NEW — library-wide scan, D-06)
├── VER02AeroSurfaceClipOrderSourceTest.kt       (NEW — library-wide scan, D-06/D-07)
└── VER03BaselineSizeSnapshotTest.kt             (NEW — runComposeUiTest + value asserts, D-10/D-11)
    (naming: match whichever package the six existing guards' own convention favors — see
     "Don't Hand-Roll" below; a per-component-package location is equally defensible for VER-03
     specifically since it measures 8 different components' composables)

library/src/test/kotlin/com/mordred/aero/components/buttons/
└── AeroButtonContrastRegressionTest.kt          (NEW — value-level, D-13)

library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt   (MODIFIED — D-12 mechanism + Text color=)
library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt (MODIFIED — Text color= import + call)

showcase/src/main/kotlin/com/mordred/showcase/sections/
└── VerificationSection.kt                       (NEW — SHW-15/D-04)
showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt  (MODIFIED — one new Column entry)

<outside this repo>/aero-scratch-consumer/        (NEW, separate Gradle project — VER-05/D-14)
├── settings.gradle.kts
├── build.gradle.kts   (implementation("com.github.Tolaseeq:aero-compose-ui:<tag>"))
└── src/main/kotlin/Main.kt   (launches a Window, renders all 8 components — D-15)
```

### Pattern 1: Source-scan gate with in-file fixture proof (D-08/D-09)

**What:** A detector extracted as a pure `(String) -> Boolean`/assertion function, unit-tested
directly against a hand-written violating string literal AND a compliant string literal inside the
SAME test file, in addition to being run against the real source files.

**When to use:** Any gate whose invariant is structural (a substring or positional relationship in
source text) rather than a runtime behavior — exactly VER-01 and VER-02.

**Example (skeleton, following `AeroButtonSurfaceSourceTest.kt`'s exact shape):**
```kotlin
// Source: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
// (this repo, read directly)
class VER01GradientProportionalitySourceTest {

    // --- D-08: the fixture-based fail-then-pass proof lives HERE, re-run on every build ---
    @Test
    fun detectorFlagsAPixelLiteralEndStop() {
        val violating = """
            Brush.verticalGradient(colors = listOf(a, b), startY = 0f, endY = 100f)
        """.trimIndent()
        assertFalse(
            gradientStopsAreProportional(violating),
            "detector must FAIL (return false / flag a violation) on a bare pixel-literal endY " +
                "stop — this is the exact historical PRIM-09 defect (VER-01)"
        )
    }

    @Test
    fun detectorFlagsANamedPixelConstantWearingADifferentName() {
        val violating = """
            Brush.verticalGradient(colors = listOf(a, b), startY = 0f, endY = GLOSS_END_PX)
        """.trimIndent()
        assertFalse(
            gradientStopsAreProportional(violating),
            "a named constant is the same bug wearing a named constant (D-05) — must still fail"
        )
    }

    @Test
    fun detectorAcceptsASizeRelativeStop() {
        val compliant = """
            Brush.verticalGradient(colors = listOf(a, b), startY = 0f, endY = size.height * 0.32f)
        """.trimIndent()
        assertTrue(gradientStopsAreProportional(compliant))
    }

    @Test
    fun detectorAcceptsAGradientWithNoExplicitStops() {
        // D-05: no explicit stop = legal, already spans full bounds.
        val compliant = "Brush.verticalGradient(colors = listOf(a, b))"
        assertTrue(gradientStopsAreProportional(compliant))
    }

    @Test
    fun realLibrarySourcePassesTheDetector() {
        // Only AFTER the fixture proof above establishes the detector can fail, run it for real.
        val offenders = allMainSourceFiles().filterNot { gradientStopsAreProportional(it.readText()) }
        assertTrue(offenders.isEmpty(), "files with non-proportional gradient stops: $offenders (VER-01)")
    }
}
```

**Why this satisfies VER-06 more strongly than the Phase 17 precedent:** the Phase 17 precedent
(`17-03-SUMMARY.md` "Guard Fail-Then-Pass Proof") temporarily broke real source, ran the suite,
recorded the transcript in a SUMMARY, then reverted — the proof is a historical document, not
re-executed code. D-08's fixture-string convention makes the SAME proof re-run on every single test
invocation, forever — strictly stronger, and the convention this phase must follow for its three new
gates per the explicit decision (costly-to-reverse, D-08).

### Pattern 2: `nonCommentSource` filtering before any textual gate check

**What:** Strip every line whose trimmed form starts with `*`, `//`, or `/*` before running a
`.contains(...)`/regex check, so KDoc prose cannot satisfy OR defeat a structural guard.

**When to use:** Any VER-01/VER-02 detector that scans real source files (not the in-file fixture
strings, which are plain data, not source-with-comments).

**Concrete, load-bearing evidence this is necessary (not theoretical) — confirmed by direct read this
session:**
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` and
  `AeroSurfacePrimitives.kt` both discuss the historical `endY = 100f` / clip-order bugs in KDoc
  prose (though the literal string `100f` no longer appears verbatim — confirmed via
  `grep -rn "100f"` returning only unrelated icon-path coordinates).
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt:42` contains
  the literal substring `.clip()` inside a KDoc sentence ("an earlier `.clip()` (which [aeroSurface]
  applies internally)") — a naive "does `.clip(` appear anywhere after `aeroSurface(` in this file's
  text" check would misfire on this exact file if it didn't first strip comments, because the KDoc
  paragraph sits BEFORE the real `aeroSurface(` call at line 114 in this particular file (so today it
  happens not to trip), but a differently-ordered KDoc paragraph in a future file could trigger a
  false positive without warning.

**Example (verbatim, reusable):**
```kotlin
// Source: library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
// (this repo, read directly — copy this helper into the new VER-01/02 test files)
private val nonCommentSource: String
    get() = aeroSwitchSource
        .lineSequence()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
        }
        .joinToString("\n")
```

### Pattern 3: WCAG contrast ratio via `Color.luminance()` (D-13's exact reusable formula)

**What:** A `contrastRatio(foreground, background)` function using Compose's own
`androidx.compose.ui.graphics.luminance()` extension (WCAG relative luminance, already implemented
by the platform — do not hand-roll the luminance formula).

**When to use:** D-13's value-level `AeroButtonContrastRegressionTest`.

**Example — recovered verbatim from this repo's own git history (retired when gap G5 closed, but
fully recoverable and directly reusable, not hypothetical):**
```kotlin
// Source: git show c35f883~1:library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt
// (this repo's own git history — read directly this session)
import androidx.compose.ui.graphics.luminance

private const val MIN_LABEL_CONTRAST: Float = 4.5f  // WCAG 1.4.3 normal-text floor

/**
 * Standard WCAG 2.x contrast ratio: the lighter of the two relative luminances plus 0.05f,
 * divided by the darker plus 0.05f. Only meaningful between two fully opaque colours — callers
 * assert opacity first.
 */
private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}
```
D-13's new test differs from this retired precedent in one respect: the retired test asserted
against a FIXED label token (`colors.onSurface`); the new test must assert against whatever
`resolveLabelColor(fillTop, fillBottom)` (D-12's new mechanism) actually returns for each
theme/stop combination — i.e. it tests that the ALGORITHM'S OWN chosen candidate clears 4.5:1
against both `fillTop` and `fillBottom`, in all three themes (the UI-SPEC's Color table already
computed the expected black/white winner per theme — reuse those values as the expected picks, not
just the ratio floor).

### Pattern 4: `runComposeUiTest` measurement (D-11)

**What:** Compose nodes measured via `onNodeWithTag`/`onNodeWithText(...).fetchSemanticsNode().size`
(or `.boundsInRoot`) inside `runComposeUiTest { setContent { ... } }`, exactly as
`AeroPanelGroupRecomposeUiTest` and `AeroButtonSemanticsTest` already do in this codebase — no new
test infrastructure, dependency, or `@OptIn` beyond `@OptIn(ExperimentalTestApi::class)` (already in
use in both existing files).

**When to use:** VER-03's size measurements. Corner radii CANNOT be measured this way (a radius
does not participate in layout/bounds) — assert those directly against the internal constant
values (`SEGMENT_CORNER_RADIUS`, the literal `4.dp`/`RoundedCornerShape(4.dp)` call sites, etc.),
per D-11's explicit reasoning.

**Baseline values to assert against — verified this session directly against git tag `v2.0.4`**
(`git show v2.0.4:library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` was
read live; confirms `height: Dp = 30.dp` default and `RoundedCornerShape(4.dp)` unchanged) plus the
UI-SPEC's own pre-verified table for the remaining seven:

| Component | Locked value | Confirmed by |
|-----------|-------------|--------------|
| `AeroButton` | height = 30.dp | `[VERIFIED: git show v2.0.4:...AeroButton.kt]` (read live this session) |
| `AeroButton`/`AeroSegmentedControl` outer corner radius | 4.dp | `[VERIFIED: git show v2.0.4:...]` + `AeroSegmentedControl.kt:204` `SEGMENT_CORNER_RADIUS = 4.dp` (read live this session) |
| `AeroSwitch` track | 36×18.dp, 14.dp thumb, travel 2.dp→20.dp | `[CITED: 20-UI-SPEC.md, itself a direct-source-read table]` |
| `AeroSegmentedControl` height | 28.dp | `[VERIFIED: AeroSegmentedControl.kt:120 .height(28.dp)]` (read live this session) |
| `AeroProgressBar` height | 8.dp | `[VERIFIED: AeroProgressBar.kt:176 height: Dp = 8.dp]` (read live this session) |
| `AeroListItem` row height | `.heightIn(min = 36.dp)`, changed from fixed `.height(36.dp)` | `[CITED: 20-UI-SPEC.md]` — the ONE documented VER-03 exception (19-06/G1), do not reopen |

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| WCAG relative luminance / contrast ratio | A custom sRGB-to-linear luminance formula | `androidx.compose.ui.graphics.Color.luminance()` (already used by the retired `AeroSegmentedControlStylesTest`) | Compose ships the correct WCAG formula; hand-rolling risks a subtly wrong gamma/coefficient set that silently under- or over-reports contrast |
| Detecting "does this component bypass the shared clip order" | A regex over raw file text without comment-stripping | The `nonCommentSource` helper pattern (Pattern 2 above), reused verbatim from `AeroSwitchSourceTest.kt` | KDoc prose in this exact codebase already contains the literal tokens (`.clip()`, `endY`) a naive scanner would misfire on |
| Screenshot/pixel-diff regression testing | Roborazzi / Paparazzi / any pixel-diff harness | Human three-theme sign-off + mechanical source/value gates | Both tools are Android-only and do not run on Compose Desktop — already explicitly rejected in `REQUIREMENTS.md` line 128; do not propose reintroducing this |
| Proving the published artifact works for a stranger | A third Gradle module inside this repo (`:scratch-consumer`) | A genuinely separate project pulling `com.github.Tolaseeq:aero-compose-ui:<tag>` via JitPack | An in-repo module builds together with `:library`'s source and proves nothing about the published, compiled artifact's public API surface (explicitly rejected, D-14) |
| Cutting a release tag to prove JitPack | Bumping `build.gradle.kts`'s real `version` | A throwaway pre-release tag (e.g. `v3.0.0-alpha01` precedent from 15-06) | Keeps the locked bump-on-milestone rule intact; JitPack resolves coordinates from git tags independent of the `version` field |

**Key insight:** Every piece of infrastructure this phase needs (WCAG luminance, `runComposeUiTest`,
source-scan test shape, throwaway-tag JitPack proof) already has a working, shipped implementation
somewhere in this repository's current tree or git history. The work is disciplined reuse, not
invention — deviating from any of these five reuse points should be treated as a red flag requiring
justification.

## Runtime State Inventory

Not applicable — Phase 20 is a verification/consolidation phase touching no rename, refactor, or
migration surface. No public API, package name, identifier, or persisted-state key is renamed or
moved. Confirmed by direct read of `20-CONTEXT.md`'s Phase Boundary ("No component is restyled in
this phase... any change to public API, default sizes, or behavior" is explicitly out of scope).

## Common Pitfalls

### Pitfall 1: Treating VER-01/VER-02 as "expected to find violations"
**What goes wrong:** A planner assumes the gates need to find and fix real bugs, and schedules
remediation tasks for the library's gradient/clip code.
**Why it happens:** Every other phase's gates (VBTN-03, VSEL-02, etc.) were written to catch a
freshly-introduced defect; it's natural to assume these two are the same shape.
**How to avoid:** D-06's own scan evidence (18 gradient sites / 9 files, confirmed independently
this session) and D-07's bypass check (confirmed absent from every `aeroGlowRing`/`aeroSurface`
call site examined this session) both show the codebase is ALREADY compliant — these gates are
expected GREEN on day one. The only real work is authoring the gate + its in-file fixture proof
(D-08), not fixing production code.
**Warning signs:** A plan that includes a "fix the gradient/clip violations found by the new gate"
task — there should be none to fix.

### Pitfall 2: Writing VER-01/VER-02 detectors that scan comments as if they were code
**What goes wrong:** A gate flags (or misses) a violation because it matched/missed a substring
inside a KDoc paragraph rather than real source.
**Why it happens:** The simplest possible implementation is `file.readText().contains(...)`, which
doesn't distinguish code from comments.
**How to avoid:** Reuse `nonCommentSource` (Pattern 2) for any check whose target substring could
plausibly appear in prose — this is concretely true today for both `.clip(` and `endY`/gradient
discussion in this codebase's own KDoc.
**Warning signs:** A gate test whose assertion message references a file known to discuss the
banned pattern in its own documentation comments (`AeroButtonSurface.kt`, `GlassModifiers.kt`,
`AeroSurfacePrimitives.kt`).

### Pitfall 3: Building D-13's contrast test against a fixed expected color instead of the algorithm's own output
**What goes wrong:** The new contrast test hardcodes "label must equal `Color.Black`" for every
theme, then breaks the moment Classic (which the UI-SPEC's own pre-computed table shows needs WHITE)
is exercised.
**Why it happens:** Copy-pasting the retired test's shape without adjusting for the fact that D-12's
mechanism is explicitly per-theme by construction (light Aero themes want dark labels, Classic's
mid-saturation blue wants a light label — 20-UI-SPEC.md Color section).
**How to avoid:** Assert the CONTRAST RATIO floor (`>= 4.5f`) against both fill stops in all three
themes, not a specific color value — and separately assert (per the UI-SPEC's own pre-computed
table) which of the two fixed candidates (near-black/near-white) each theme is expected to resolve
to, so a silent flip in the wrong direction is still caught.
**Warning signs:** A test with only one expected literal color shared across all three
`AeroColorScheme` instances.

### Pitfall 4: Forgetting the fillBottom margin is genuinely tight on AeroBlue
**What goes wrong:** The starting darken values (`FILLED_FILL_TOP_DARKEN = 0.20f`,
`FILLED_FILL_BOTTOM_DARKEN = 0.36f`) are left completely unchanged, and D-13's test then measures
AeroBlue's fillBottom-vs-black contrast just barely under 4.5:1 once the REAL `Color.darken()`
RGB-mix math runs (the UI-SPEC's hand-computed estimate was 4.56 — a 0.06 margin, explicitly
flagged as "tight" and "should be confirmed programmatically, not trusted by hand").
**Why it happens:** The UI-SPEC's numbers are a hand-computed estimate, not a compiler-verified
value; a planner might assume they're already correct enough to skip re-verification.
**How to avoid:** Run D-13's test against the REAL constants before considering the contrast fix
task done. If it fails, the sanctioned fix (per UI-SPEC point 3) is to widen
`FILLED_FILL_BOTTOM_DARKEN` very slightly for that one measurement — never to special-case the
label color per theme beyond the black/white pick, and never to retune
`AeroSegmentedControl`-specific constants separately (D-12 still applies).
**Warning signs:** A plan that treats the starting darken values as immutable inputs rather than as
a starting point subject to the test's own verdict.

### Pitfall 5: Scoping VER-05's scratch consumer as an in-repo module for convenience
**What goes wrong:** Under time pressure, a third `include(":scratch-consumer")` line is added to
`settings.gradle.kts`, defeating the entire point of the requirement.
**Why it happens:** It is dramatically less setup work than standing up a genuinely separate
Gradle project with its own JitPack dependency resolution.
**How to avoid:** D-14 explicitly rejected this ("was rejected precisely because it builds together
with everything and therefore proves nothing about the published artifact"). The scratch consumer
MUST live outside this repository's `settings.gradle.kts` `include()` list and resolve the library
purely via its JitPack Maven coordinate.
**Warning signs:** Any diff touching this repo's own `settings.gradle.kts` as part of a VER-05 task.

### Pitfall 6: Treating 15-06 as already having proven VER-05
**What goes wrong:** A plan marks VER-05 "mostly done" because 15-06 already did a JitPack proof.
**Why it happens:** Surface similarity — both use a throwaway tag and JitPack.
**How to avoid:** 15-06 (`.planning/phases/15-toolchain-upgrade/15-06-SUMMARY.md`, read directly
this session) proved only that the LIBRARY ARTIFACT ITSELF builds cleanly on JitPack's infra — it
never created, built, or ran any consuming project, and never rendered a single component. VER-05
requires a NEW project that depends on the artifact and launches a window rendering all eight
components (D-15) — genuinely new work, reusing only the "throwaway tag" mechanism from 15-06, not
its scope.
**Warning signs:** A plan whose VER-05 task list has zero tasks, on the theory 15-06 already covers it.

## Code Examples

### Six existing `*SourceTest.kt` guards — exact naming/shape convention to match
```
library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt
library/src/test/kotlin/com/mordred/aero/components/list/AeroListItemSourceTest.kt
library/src/test/kotlin/com/mordred/aero/components/range/AeroSliderSourceTest.kt
library/src/test/kotlin/com/mordred/aero/components/range/AeroProgressBarSourceTest.kt
library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlSourceTest.kt
library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt
```
All six share: a `sourceFile(name): File` helper with two candidate paths (cwd varies between
`library/` and repo root when Gradle runs the test task), a class-level KDoc naming the exact
requirement ID each assertion protects, and (per the project's own convention) a KDoc pointer to
the SUMMARY.md where the fail-then-pass proof transcript lives — D-08 changes this last part for
Phase 20's new gates only (in-file fixture instead of a SUMMARY transcript).

### Confirmed gradient call-site inventory (D-06 evidence, re-verified this session)
18 sites across 9 files, matching the CONTEXT.md count exactly:
- `AeroProgressBar.kt` — 2 explicit-stop `horizontalGradient` calls, both `size.width`-relative or alpha-stop-only (no bare pixel literal)
- `AeroSurfacePrimitives.kt` — the core primitive itself, all `size.*`-relative
- `GlassModifiers.kt` — `glassSurface`/`glassPanel`, both `size.height * fraction` (already fixed, PRIM-09)
- `AeroHueSlider.kt` / `AeroHsvColorSquare.kt` (color-picker internals) — gradients with NO explicit stops (legal per D-05's positive rule) or Color-only lists
- `AeroTitleBar.kt` / `AeroDialog.kt` — no explicit stops, `colors = listOf(...)` only
- `AeroDrawer.kt:150` (`size.height * 0.4f`) and `AeroPopover.kt:75` (`size.height * 0.55f`) — the two library-wide, out-of-8-target sites D-06 cites as "already correct"

### Existing `runComposeUiTest` precedents (compile/runtime-proven on this CMP 1.11.1 toolchain)
```kotlin
// Source: library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt
// (this repo, read directly)
@OptIn(ExperimentalTestApi::class)
class AeroButtonSemanticsTest {
    @Test
    fun aeroButtonHasRoleButtonAndClickAction() = runComposeUiTest {
        setContent { AeroTheme { AeroButton(text = "Save Changes", onClick = {}) } }
        waitForIdle()
        onNodeWithText("Save Changes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
    }
}
```
This file, unmodified, already closes VER-04 (D-16) — Phase 20 verifies it is current and green;
no new keyboard test is written.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| Fail-then-pass proof recorded as a one-time SUMMARY.md transcript | Fail-then-pass proof as an in-file fixture (violating string + compliant string), re-asserted every build | This phase (D-08) | Strictly stronger — the proof cannot silently rot; every future CI run re-verifies the gate can still fail |
| Segmented control's own bespoke contrast-chasing darken constants (`0.58f`/`0.61f`) | Unified fill/label mechanism shared with `AeroButton` via `AeroButtonSurface.kt` | 19-12 (gap G5, commit `c35f883`) | The retired test recovered in this research is the direct ancestor of D-13's new test — same formula, different (per-theme-computed) expected values |
| `.contains(...)` textual scans without comment-stripping | `nonCommentSource`-filtered scans for any check whose target string appears in KDoc prose | Introduced in `AeroSwitchSourceTest.kt` (Phase 19) | Load-bearing precedent this phase's two new gates must copy, not reinvent |

**Deprecated/outdated:** none specific to this phase — the CMP 1.11.1 `runComposeUiTest` v1 API is
already deprecated-but-compiling and deliberately kept (TOOL-03/04, locked decision from Phase 15);
Phase 20 does not touch that decision.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The new VER-01/VER-02 test files should live in `library/src/test/kotlin/com/mordred/aero/theme/` (mirroring where the primitives they guard, `AeroSurfacePrimitives.kt`/`GlassModifiers.kt`, live) rather than a new top-level `verification/` test package | Recommended Project Structure | Low — pure file-location preference; either location compiles and runs identically under Gradle's test source set. Planner may relocate freely. |
| A2 | AeroBlue's fillBottom-vs-black contrast will land at ~4.56 (a thin margin) once measured against the real `Color.darken()` implementation, per the UI-SPEC's hand-computed estimate | Common Pitfalls #4 | Medium — if the real measured value differs meaningfully from the hand estimate, the planner's contingency (widen `FILLED_FILL_BOTTOM_DARKEN` slightly) may need a different magnitude, or may not be needed at all. D-13's own test is the authoritative check; this number is not load-bearing for anything except sizing engineering risk. |

**If this table is empty:** N/A — two low/medium-risk assumptions remain, both explicitly flagged
by the UI-SPEC itself as needing programmatic (not hand) confirmation, which is exactly what D-13's
test performs.

## Open Questions (RESOLVED)

> Both questions were resolved at plan time; the deciding record lives in `planner_assumptions` of
> `20-01-PLAN.md` (Q1) and `20-03-PLAN.md` (Q2). Resolutions are recorded inline below.

1. **Exact detector granularity for VER-02's "no component-authored `.clip(` after `aeroSurface(`" rule** — **RESOLVED**
   - What we know: no shipped component currently violates this (confirmed by direct grep of every
     `.clip(`/`aeroSurface(` call site in `components/`); the rule needs to be chain-aware, not
     merely "any `.clip(` textually after any `aeroSurface(` in the whole file" (a file could
     legitimately contain both patterns in unrelated, non-chained code, e.g. `AeroSegmentedControl.kt`
     has both a standalone `.clip(shape)` on the outer `Row` at line 122 AND an `.aeroSurface(...)`
     call on a per-segment `Box` at line 172 — two entirely different modifier chains, not a
     violation, but a naive "textually after" scan would need to be smart enough not to flag this).
   - What's unclear: whether the detector should parse actual modifier-chain boundaries (e.g. split
     on `Modifier`/`.` chains delimited by `Box(...)`/`Row(...)` boundaries) or whether a simpler
     per-declaration-block heuristic suffices given the codebase's actual shape.
   - Recommendation: start with the simplest heuristic that passes today's real code AND the
     fixture-string fail case (D-08) — e.g. scan within each `Modifier`-chain literal block
     (delimited by the enclosing `Box(`/`.modifier =`/similar), not the whole file. The
     `AeroSegmentedControl.kt` outer-Row-clip / per-segment-aeroSurface case above is the concrete
     test case to validate the chosen heuristic against, since it's the one real file containing
     both tokens today without being a violation.
   - **RESOLVED (20-01-PLAN.md):** chain-aware detection — the scan segments modifier chains rather
     than reading the whole file, and the mandatory clean fixture reproduces the real
     `AeroSegmentedControl.kt` shape (outer `Row` with `.clip(`, inner `Box` with `aeroSurface(`)
     so the false-positive case is pinned by a test, not by inspection.

2. **Whether VER-03's runComposeUiTest measurement needs one test class per component or one class covering all eight** — **RESOLVED**
   - What we know: `AeroPanelGroupRecomposeUiTest` and `AeroButtonSemanticsTest` are both
     single-component, single-class. VER-03 needs to measure eight different components' sizes.
   - What's unclear: whether a single `VER03BaselineSizeSnapshotTest` class with eight `@Test`
     methods (one per component) is preferred, or eight small additions distributed across each
     component's own package (mirroring where the six existing `*SourceTest.kt` guards each live
     next to their own component).
   - Recommendation: Claude's discretion per CONTEXT.md — either is structurally sound; a single
     class centralizing all eight measurements against the one `v2.0.4` baseline probably reads
     more coherently as "the VER-03 gate" than eight scattered additions, but this is not
     load-bearing either way.
   - **RESOLVED (20-03-PLAN.md):** one centralized `VER03BaselineSizeSnapshotTest` class. All three
     new gates live under the `com.mordred.aero.verification` package.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | Gradle build/test | ✓ | Temurin 17.0.19 | — |
| Gradle wrapper | All build/test tasks | ✓ | `gradlew`/`gradlew.bat` present at repo root | — |
| Git tag `v2.0.4` | VER-03 baseline (D-10) | ✓ | Confirmed via `git tag` and `git show v2.0.4:...` | — |
| JitPack (external service) | VER-05 (D-14), precedent 15-06 | ✓ (proven working in 15-06) | n/a | — |
| Windows display-scaling change (125%/200%) | SHW-16 non-100% DPI pass (D-02) | Requires human action at sign-off time, not a build dependency | n/a | — the sign-off protocol itself IS the fallback/mechanism |
| A second machine/environment for the scratch consumer | VER-05 (D-15) | Can be the SAME machine, different working directory outside this repo's git tree — no second physical environment required | n/a | — |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** none — everything this phase needs is already present and
previously proven working in this repository's own history.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.0`) via `kotlin.test`, `useJUnitPlatform()` |
| Config file | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`), `gradle/libs.versions.toml` for version pins |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.buttons.*"` (or the specific new class name) |
| Full suite command | `./gradlew :library:test` (78 existing test files as of this research; project reports 232 tests green as of Phase 15) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SHW-15 | Showcase demonstrates all 8 components in every state | manual (human sign-off) | n/a — visual review | New showcase section, ❌ Wave 0 |
| SHW-16 | Human 3-theme + non-100% DPI sign-off | manual-only, human_judgment | n/a — no automated command exists for a visual judgment | n/a (protocol, not code) |
| VER-01 | No pixel-literal gradient stops | unit (source-scan) | `./gradlew :library:test --tests "*VER01*"` | ❌ Wave 0 — new file |
| VER-02 | No `aeroSurface()` clip-order bypass | unit (source-scan) | `./gradlew :library:test --tests "*VER02*"` | ❌ Wave 0 — new file |
| VER-03 | Sizes/radii match `v2.0.4` baseline | unit (`runComposeUiTest` + value asserts) | `./gradlew :library:test --tests "*VER03*"` | ❌ Wave 0 — new file |
| VER-04 | Keyboard activation for both buttons | unit (`runComposeUiTest`) | `./gradlew :library:test --tests "AeroButtonSemanticsTest"` | ✅ already exists, closed (D-16) |
| VER-05 | Scratch consumer builds + renders all 8 | e2e (external project + human-verify) | n/a — separate project's own `./gradlew run` plus visual confirm | ❌ Wave 0 — new external project |
| VER-06 | Each new gate provably fails on unfixed code | unit (fixture assertions inside VER-01/02/03's own test files) | same commands as VER-01/02/03 above | ❌ Wave 0 — folded into the same new files (D-08) |
| D-12/D-13 (folded contrast fix) | Label clears 4.5:1 against both stops, all 3 themes | unit (value-level) | `./gradlew :library:test --tests "*ContrastRegression*"` | ❌ Wave 0 — new file (retired precedent recoverable via `git show c35f883~1:...`) |

### Sampling Rate
- **Per task commit:** the specific new test class's `--tests` filter (fast, seconds)
- **Per wave merge:** `./gradlew :library:test` (full suite, confirms no cross-component regression)
- **Phase gate:** full suite green AND human three-theme (+ DPI) sign-off passed, in that order
  (D-03 — code review and mechanical gates close FIRST, sign-off happens LAST)

### Wave 0 Gaps
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/VER01GradientProportionalitySourceTest.kt` — covers VER-01, VER-06
- [ ] `library/src/test/kotlin/com/mordred/aero/theme/VER02AeroSurfaceClipOrderSourceTest.kt` — covers VER-02, VER-06
- [ ] `library/src/test/kotlin/.../VER03BaselineSizeSnapshotTest.kt` (exact package per Open Question 2) — covers VER-03, VER-06
- [ ] `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonContrastRegressionTest.kt` — covers D-13 (folded todo)
- [ ] `showcase/src/main/kotlin/com/mordred/showcase/sections/VerificationSection.kt` — covers SHW-15
- [ ] New external Gradle project (outside this repo) for VER-05 — no framework gap, just net-new project scaffolding
- [ ] Framework install: none — JUnit 5 / `kotlin.test` / `compose.uiTest` are all already declared in `library/build.gradle.kts`

## Security Domain

Not applicable in the ASVS sense — this phase adds no authentication, session, network-input, or
cryptography surface. The one "external" interaction (JitPack artifact resolution for VER-05) is a
build-time dependency-resolution action against a service already trusted and used by this project
(TOOL-08/15-06 precedent), not a new attack surface introduced by this phase. No `security_enforcement:
false` override was found in `.planning/config.json`; noted here as "not applicable by domain" rather
than skipped by config.

## Sources

### Primary (HIGH confidence — direct reads of this repository's own files/history this session)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroSurfacePrimitives.kt` — full file read
- `library/src/main/kotlin/com/mordred/aero/theme/GlassModifiers.kt` — full file read
- `library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButtonSurface.kt` — full file read
- `library/src/main/kotlin/com/mordred/aero/components/selection/AeroSegmentedControl.kt` — full file read
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — full file read
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSurfaceSourceTest.kt` — full file read
- `library/src/test/kotlin/com/mordred/aero/components/buttons/AeroButtonSemanticsTest.kt` — full file read
- `library/src/test/kotlin/com/mordred/aero/components/selection/AeroSwitchSourceTest.kt` — full file read
- `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt` — full file read
- `git show c35f883~1:library/src/test/kotlin/com/mordred/aero/components/selection/AeroSegmentedControlStylesTest.kt` — retired file recovered from git history, full read
- `git show v2.0.4:library/src/main/kotlin/com/mordred/aero/components/buttons/AeroButton.kt` — baseline tag content, direct read
- `.planning/phases/15-toolchain-upgrade/15-06-SUMMARY.md` — full file read
- `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt`, `Main.kt` — full file reads
- `library/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `jitpack.yml` — full file reads
- Grep across `library/src/main/kotlin` for all `Brush.*Gradient` call sites (18 confirmed, matching D-06) and all `aeroGlowRing(`/`aeroSurface(`/`.clip(` call sites (confirmed no VER-02 bypass exists today)

### Secondary (MEDIUM confidence)
- `.planning/phases/20-verification/20-UI-SPEC.md` — this phase's own approved design contract, itself built from direct source reads by the ui-researcher; the per-theme contrast table (Pattern 3/Pitfall 3/4) is a hand-computed estimate the UI-SPEC itself flags as needing programmatic confirmation

### Tertiary (LOW confidence)
- None — every claim in this document traces to a direct file/git read performed this session, not to training-data recall or unverified web search. This phase required zero external library research.

## Metadata

**Confidence breakdown:**
- Gate mechanics (VER-01/02/06): HIGH — six existing precedent files read directly, zero ambiguity in shape/convention
- VER-03 baseline: HIGH — the exact `v2.0.4` tag content was read live for `AeroButton`; the other seven values are cited from the phase's own already-checker-approved UI-SPEC, itself sourced from direct reads
- Contrast mechanism (D-12/D-13): HIGH for the reusable test infrastructure (retired test recovered verbatim from git history); MEDIUM for the exact final darken-constant magnitude, which the UI-SPEC itself flags as an estimate pending D-13's own programmatic confirmation
- VER-05 scratch consumer: HIGH for the JitPack mechanism (proven in 15-06); the "must render, not just compile" requirement (D-15) is net-new work with no in-repo precedent to point to beyond the general Compose Desktop `Window`/`application {}` pattern already used in `showcase/Main.kt`

**Research date:** 2026-07-28
**Valid until:** No external dependency currency to expire — this phase adds zero new libraries.
Valid indefinitely against the current `master` state; re-verify only if Phases 17-19's shipped code
changes again before Phase 20 executes (unlikely, per REQUIREMENTS.md's "no component restyled"
constraint).
