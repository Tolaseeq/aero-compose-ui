# Phase 4: AeroIcons Foundation - Research

**Researched:** 2026-04-29
**Domain:** Phosphor Icons SVG vendoring, Valkyrie CLI 1.1.1, Kotlin ImageVector lazy backing-property pattern, explicitApi() compatibility
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Final count: 138 unique icons (not 139). `arrow-up-right` appears at row #10 and row #139 — row #139 is the duplicate; it is deleted.
- Count propagation is the FIRST commit of 04-01 — fix references in: ROADMAP.md, REQUIREMENTS.md, STATE.md, SUMMARY.md, FEATURES.md Part 3, ARCHITECTURE.md.
- Curated list accepted as-is. No further adds or drops beyond the duplicate.
- Spot-check icon set (7 icons): `X`, `CaretDown`, `MagnifyingGlass`, `Check`, `Info`, `FrameCorners`, `Square`.
- Facade organization: alphabetical, single block. All 138 properties sorted A→Z in AeroIcons.kt.
- Vendor only the 138 SVGs we use to `tools/phosphor-svgs/regular/`. NOT the full ~1,300 Regular set.
- `.pin` file format: single line `<40-char-commit-sha>\nhttps://github.com/phosphor-icons/core/tree/<sha>` in plain text at `tools/phosphor-svgs/.pin`.
- `tools/phosphor-svgs/README.md` documents: what was pinned, the exact Valkyrie command used, and how to retroactively extract any unvendored icon.
- Phosphor MIT license file committed at `tools/phosphor-svgs/LICENSE`.
- Tool: Valkyrie CLI 1.1.1 with `--output-format backing-property`. Locked.
- Generated files write direct to `library/src/main/kotlin/com/mordred/aero/icons/internal/`. No staging directory.
- Invocation documented in plan + README. NOT wired into Gradle. NOT a shell script.
- Gradle build does NOT depend on Valkyrie — generated files committed as ordinary source.
- File layout: `icons/AeroIcons.kt` (facade) + `icons/internal/<KotlinName>.kt` (138 files).
- Each builder function: `internal fun load<KotlinName>(): ImageVector`.
- Per-icon files have NO KDoc.
- Lazy backing-property pattern: `public val X: ImageVector get() = _X ?: loadX().also { _X = it }` + `private var _X: ImageVector? = null`.
- Every public constant marked `public` (explicitApi compatible).
- Object-level KDoc only; covers: Phosphor source + MIT license + viewBox + naming rule + 8-example table + recommended size 16dp–32dp + mandatory explicit tint warning + phosphoricons.com URL.
- Plan breakdown: 04-01 = Spike (7-icon spot-check + 139→138 reference fix + facade authoring + first compile-pass); 04-02 = Batch (remaining 131 icons + facade complete).
- No smoke test in Phase 4 — ICN-01/02/03 acceptance is structural/compile-time only.
- No visual sign-off in Phase 4 — Phase 6 owns visual sign-off.
- `AeroBreadcrumb.separator` stays `String` in v1.1 — intentionally NOT migrated.
- Single `:library` module (no separate `:icons` Gradle module).
- Generated files committed to `src/main`, NOT `build/`.
- `compose.material3.Icon()` used directly — no custom `AeroIcon()` wrapper; tint always explicit.

### Claude's Discretion
- Builder function naming style — `loadCaretDown()` is recommended; accept Valkyrie's actual generated convention if it differs.
- Per-icon file imports — minimize as Valkyrie generates; reformat if messy.
- Wave 1 of 04-02 (131-icon batch) — sequencing within the plan; single commit or split for reviewability.
- Exact wording inside the object-level KDoc beyond the structural requirements.
- Whether to commit `tools/phosphor-svgs/regular/` SVGs alphabetically or as-downloaded.
- KDoc tag rendering (Markdown vs Dokka @example block) for the naming-convention table.

### Deferred Ideas (OUT OF SCOPE)
- Valkyrie wrapped in a Gradle task or shell/PS script.
- Adding `git-branch`, `pin`, or `tag` to fill the 139th slot.
- Sweeping the curated list for adds/drops.
- Lazy-init smoke test.
- Reflective property-count test.
- Per-property KDoc.
- Categorical sectioning in facade.
- Phase 4 visual sign-off via temporary preview.
- Vendoring all ~1,300 Regular-weight Phosphor SVGs.
- Filled / Bold / Light / Duotone weight variants.
- Custom user icon registration API.
- Separate `:icons` Gradle module.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| ICN-01 | `public object AeroIcons` in `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt`; each icon is `public val Name: ImageVector` with lazy backing-property pattern; explicitApi-compatible | Lazy pattern confirmed via Valkyrie BackingProperty output + ARCHITECTURE.md pattern; explicitApi() compatibility confirmed via PITFALLS.md §11 |
| ICN-02 | KDoc on `AeroIcons` explains naming (Phosphor kebab-case → PascalCase), 8 examples, size recommendation (16–32dp), mandatory explicit tint on dark themes | KDoc content spec locked in CONTEXT.md; tint discipline documented in PITFALLS.md §5 |
| ICN-03 | 138 ImageVector-constants (corrected from 139) accessible via `AeroIcons.*` autocomplete; Phosphor Regular; viewBox 256×256; stroke 16; rounded caps/joins; defaultWidth=defaultHeight=24.dp | Complete 138-icon list in this document §Final 138-Icon List; Valkyrie handles SVG→Kotlin conversion automatically |
</phase_requirements>

---

## Summary

Phase 4 creates the typed icon set from scratch. No `icons/` package exists in the codebase yet. The primary mechanical work is: (1) vendor 138 Phosphor Regular SVGs to `tools/phosphor-svgs/regular/`, (2) run Valkyrie CLI 1.1.1 to generate 138 `internal/*.kt` builder files, (3) author `AeroIcons.kt` facade with all 138 lazy properties + object-level KDoc, (4) compile-verify with `./gradlew :library:compileKotlin`.

**Critical stroke-width correction from prior research:** The existing research documents (`SUMMARY.md`, `STACK.md`, `PITFALLS.md`) state Phosphor Regular stroke-width is `12`. Direct inspection of the actual `phosphor-icons/core` `raw/regular/` SVG files reveals the stroke-width is **16**, not 12. This matters for: the spot-check verification step (grep for `strokeLineWidth=16f` not `12f`), the KDoc accuracy, and the `PITFALLS.md` §1 re-calibration note. Valkyrie translates the SVG `stroke-width="16"` to `strokeLineWidth = 16f` in the generated Kotlin — no manual intervention needed.

**Valkyrie CLI actual flags (verified from source):** The command uses `--input-path`, `--output-path`, `--package-name`, `--iconpack-name`, `--explicit-mode`, and `--output-format backing-property` (lowercase hyphenated, NOT camelCase). It accepts a directory via `--input-path` and processes all SVGs in that directory in one invocation.

**Primary recommendation:** Run Valkyrie once on the 7 spot-check SVGs, verify `compileKotlin` passes, then run Valkyrie on the remaining 131 SVGs. Commit each natural milestone atomically. Never author the ImageVector builder code by hand.

---

## Standard Stack

### Core (no changes to library/build.gradle.kts in Phase 4)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.1.21 | Language | Locked in libs.versions.toml |
| CMP | 1.7.3 | UI framework | Locked in libs.versions.toml |
| `compose.ui` | bundled with CMP 1.7.3 | `ImageVector`, `ImageVector.Builder`, `StrokeCap`, `StrokeJoin` | Already declared in library/build.gradle.kts |
| `compose.material3` | bundled with CMP 1.7.3 | `Icon()` composable at call sites | Already declared; `Icon()` is in material3, not materialIconsExtended |

### External Tool (not a Gradle dependency)

| Tool | Version | Purpose | Invocation |
|------|---------|---------|------------|
| Valkyrie CLI | 1.1.1 | Batch-converts SVGs to Kotlin `ImageVector` builders | One-shot offline; output committed to src/ |

**Phase 4 does NOT touch `library/build.gradle.kts`** — `materialIconsExtended` removal is Phase 5 (CLN-02).

### Installation (Valkyrie CLI)

```bash
# Download cli-1.1.1 from:
# https://github.com/ComposeGears/Valkyrie/releases/tag/cli-1.1.1
# The release ships a ZIP containing the valkyrie binary.
# On Windows (Git Bash / WSL):
chmod +x valkyrie   # if needed
./valkyrie --version
```

---

## Architecture Patterns

### Recommended Project Structure (Phase 4 creates `icons/` from scratch)

```
library/src/main/kotlin/com/mordred/aero/icons/
├── AeroIcons.kt                    ← public facade, 138 alphabetized lazy properties, object-level KDoc
└── internal/
    ├── Archive.kt
    ├── ArrowBendUpLeft.kt
    ├── ArrowClockwise.kt
    ├── ArrowCounterClockwise.kt
    ├── ArrowDown.kt
    ├── ArrowLeft.kt
    ├── ArrowRight.kt
    ├── ArrowSquareOut.kt
    ├── ArrowUp.kt
    ├── ArrowUpRight.kt
    ├── ArrowsDownUp.kt
    ├── ... (138 files total, alphabetical by Kotlin name)
    └── XCircle.kt

tools/phosphor-svgs/
├── .pin                            ← plain text: <sha>\nhttps://github.com/phosphor-icons/core/tree/<sha>
├── LICENSE                         ← Phosphor MIT license text
├── README.md                       ← Valkyrie command + recovery instructions
└── regular/
    ├── archive.svg
    ├── arrow-bend-up-left.svg
    ├── ... (138 SVG files, kebab-case Phosphor names, no -regular suffix)
    └── x-circle.svg
```

### Pattern 1: Lazy Backing-Property (locked)

This is the exact pattern Valkyrie BackingProperty output format generates, and what the facade must use:

```kotlin
// AeroIcons.kt
package com.mordred.aero.icons

import androidx.compose.ui.graphics.vector.ImageVector
import com.mordred.aero.icons.internal.loadX
import com.mordred.aero.icons.internal.loadCaretDown
// ... 136 more imports

/**
 * Typed [ImageVector] constants — Phosphor Icons Regular weight.
 *
 * All constants are initialized lazily on first access. Each icon is cached after
 * first use and reused for the lifetime of the process.
 *
 * **Naming:** Phosphor kebab-case names map 1-to-1 to PascalCase Kotlin identifiers.
 * Look up icons at [phosphoricons.com](https://phosphoricons.com) and use the
 * source name directly — no renaming layer.
 *
 * | Phosphor name   | AeroIcons identifier | NOT               |
 * |-----------------|----------------------|-------------------|
 * | `caret-down`    | `CaretDown`          | ~~ChevronDown~~   |
 * | `magnifying-glass` | `MagnifyingGlass` | ~~Search~~        |
 * | `house`         | `House`              | ~~Home~~          |
 * | `funnel`        | `Funnel`             | ~~Filter~~        |
 * | `gear`          | `Gear`               | ~~Settings~~      |
 * | `x`             | `X`                  | ~~Close~~         |
 * | `eye-slash`     | `EyeSlash`           | ~~EyeOff~~        |
 * | `envelope`      | `Envelope`           | ~~Mail~~          |
 *
 * Source: [Phosphor Icons](https://github.com/phosphor-icons/core) Regular weight, MIT license.
 * ViewBox 256×256, stroke-width 16, stroke-linecap round.
 *
 * **Recommended render size:** 16dp–32dp. Below 14dp the stroke antialiases to <1dp.
 *
 * **Tint is mandatory:** `Icon()`'s default tint is `LocalContentColor`, which `AeroTheme`
 * does NOT set. Always pass `tint = AeroTheme.colors.X` explicitly at every `Icon()` call site
 * inside `:library` and `:showcase`.
 */
public object AeroIcons {

    // A
    public val Archive: ImageVector get() = _Archive ?: loadArchive().also { _Archive = it }
    private var _Archive: ImageVector? = null

    public val ArrowBendUpLeft: ImageVector get() = _ArrowBendUpLeft ?: loadArrowBendUpLeft().also { _ArrowBendUpLeft = it }
    private var _ArrowBendUpLeft: ImageVector? = null

    // ... 136 more, alphabetical ...

    // X
    public val X: ImageVector get() = _X ?: loadX().also { _X = it }
    private var _X: ImageVector? = null

    public val XCircle: ImageVector get() = _XCircle ?: loadXCircle().also { _XCircle = it }
    private var _XCircle: ImageVector? = null
}
```

### Pattern 2: Per-Icon Internal Builder File (Valkyrie-generated shape)

Valkyrie CLI with `--output-format backing-property` generates per-icon files as **extension properties** on the `AeroIcons` object, NOT as standalone internal functions. The actual generated shape is:

```kotlin
// icons/internal/X.kt  (Valkyrie BackingProperty output — VERIFIED from source)
package com.mordred.aero.icons.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.mordred.aero.icons.AeroIcons

// NOTE: Valkyrie BackingProperty generates an extension property on AeroIcons,
// not a standalone internal function. The facade's private var caches it.
// The exact generated shape (extension property vs loadX() function) may differ
// from CONTEXT.md's locked pattern. Per Claude's Discretion: match Valkyrie's
// actual output rather than fighting the tool.

val AeroIcons.X: ImageVector
    get() {
        if (_x != null) return _x!!
        _x = ImageVector.Builder(
            name = "X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = SolidColor(Color.Transparent)
            ) {
                moveTo(200f, 56f)
                lineTo(56f, 200f)
                moveTo(56f, 56f)
                lineTo(200f, 200f)
            }
        }.build()
        return _x!!
    }

@Suppress("ObjectPropertyName")
private var _x: ImageVector? = null
```

**IMPORTANT — Two Valid Generated Shapes:** Valkyrie BackingProperty can generate either:
- (A) Extension property on `AeroIcons` (shown above) — `AeroIcons.kt` becomes `object AeroIcons` with no property bodies, all properties live in `internal/*.kt` as extension properties.
- (B) Standalone `internal fun loadX(): ImageVector` called from `AeroIcons.kt` getter — matches CONTEXT.md's locked pattern.

The locked decision in CONTEXT.md says "match Valkyrie's actual output." Run Valkyrie on a single SVG first and inspect the output before writing the facade. The verification step in 04-01 is where this is discovered.

### Valkyrie CLI — Exact Invocation (verified from source code)

```bash
# Step 1: Spot-check 7 SVGs (04-01 spike)
./valkyrie svgxml2imagevector \
  --input-path tools/phosphor-svgs/regular/ \
  --output-path library/src/main/kotlin/com/mordred/aero/icons/internal/ \
  --package-name com.mordred.aero.icons.internal \
  --iconpack-name AeroIcons \
  --output-format backing-property \
  --explicit-mode

# Note: Run from the repo root (C:/1A_WORK/ui_lib/).
# --explicit-mode adds 'public' modifier to satisfy explicitApi().
# --output-format takes "backing-property" (lowercase hyphenated), not "BackingProperty".
# --input-path accepts a directory; processes all SVG/XML files in that directory.
# Valkyrie also generates AeroIcons.kt (the object declaration) in --output-path.
# If AeroIcons.kt is generated in internal/, move it up one level to icons/.
```

### Anti-Patterns to Avoid

- **Eager `val` initialization:** `public val X: ImageVector = buildX()` — forces all 138 icons to allocate at class-load time. Use the lazy getter pattern only.
- **`by lazy {}` delegation:** Works but adds `Lazy<T>` wrapper per property. The explicit null-check backing field pattern has zero overhead. Material Icons uses the explicit pattern; match it.
- **Nested sub-objects:** `AeroIcons.Navigation.CaretDown` — fragments autocomplete, adds no value at 138 icons. Flat namespace only.
- **Custom `AeroIcon()` wrapper:** Locked decision — use `material3.Icon()` directly.
- **viewportWidth=24f in generated output:** Would indicate Valkyrie converted the wrong SVG path. Must be 256f.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| SVG → ImageVector conversion | Manual path transcription | Valkyrie CLI 1.1.1 | 138 icons × 10–80 SVG path commands each = thousands of lines of error-prone work |
| Lazy singleton per-icon | Custom cache map or `by lazy` | Null-backed getter pattern | Zero allocations; identical to Material Icons pattern; already verified to work under explicitApi() |
| SVG fetching | Clone entire phosphor-icons/core (~50MB) | `curl` 138 files individually from `raw.githubusercontent.com` at pinned SHA | Minimal repo footprint; reproducible via `.pin` file |

**Key insight:** Valkyrie exists precisely to solve the SVG→ImageVector conversion problem. The tool is tested against Phosphor and Feather SVGs in production use. Do not transcribe path data by hand.

---

## Common Pitfalls

### Pitfall 1: Stroke-Width Discrepancy — 16, Not 12

**What goes wrong:** All existing research documents state `strokeLineWidth = 12f`. The actual Phosphor Regular SVGs (verified at `raw.githubusercontent.com/phosphor-icons/core/main/raw/regular/x.svg`, `check.svg`, `caret-down.svg`, `info.svg`, `magnifying-glass.svg`, `frame-corners.svg`) use `stroke-width="16"`. Valkyrie will translate this faithfully to `strokeLineWidth = 16f`.

**How to avoid:** Spot-check grep command for verification: `grep -rn "strokeLineWidth=12f" library/src/main/kotlin/com/mordred/aero/icons/` should return 0 hits after Phase 4. The correct check is `strokeLineWidth=16f`.

**Update required:** References to `strokeLineWidth=12f` in CONTEXT.md (verification gates), ROADMAP.md, SUMMARY.md, PITFALLS.md, REQUIREMENTS.md, and ARCHITECTURE.md must be corrected. Do this in the first commit alongside the 139→138 count fix.

### Pitfall 2: Wrong SVG Directory — `raw/` vs `assets/`

**What goes wrong:** `phosphor-icons/core` has TWO icon directories:
- `raw/regular/<name>.svg` — STROKE-BASED icons. `stroke="currentColor"`, `fill="none"`, `stroke-width="16"`. This is what we want.
- `assets/regular/<name>.svg` — FILL-BASED icons. `fill="currentColor"`, no stroke attributes. Solid paths. Wrong for our stroke aesthetic.

**How to avoid:** Always fetch from `raw/regular/`, never from `assets/regular/`. The raw URL template is:
```
https://raw.githubusercontent.com/phosphor-icons/core/<SHA>/raw/regular/<name>.svg
```
NOT `assets/regular/<name>-regular.svg`.

**Filename format in `raw/regular/`:** Files are named `<name>.svg` with NO `-regular` suffix (e.g., `caret-down.svg`, not `caret-down-regular.svg`). The prior research docs state "raw/regular/<name>-regular.svg" — that suffix is WRONG for the raw/ directory. (The assets/ directory uses the `-regular` suffix pattern but serves fill-based icons.)

### Pitfall 3: Eager Initialization at 138 Icons

**What goes wrong:** Using `public val X: ImageVector = buildX()` (eager assignment) causes all 138 ImageVector objects to be constructed when `AeroIcons` class is first loaded. Each `ImageVector.Builder` chain allocates multiple objects. At 138 icons this is a measurable startup spike.

**How to avoid:** The null-backed lazy getter pattern is mandatory:
```kotlin
public val X: ImageVector get() = _X ?: loadX().also { _X = it }
private var _X: ImageVector? = null
```
Verify the first 7 generated files before the batch run. If Valkyrie generates `by lazy` instead, accept it — both patterns satisfy the requirement.

### Pitfall 4: explicitApi() Violations

**What goes wrong:** Any public declaration without an explicit `public` modifier triggers: `Visibility must be specified in explicit API mode`.

**Exact syntax requirements:**
- `public object AeroIcons` — required
- `public val X: ImageVector get() = ...` — required; the `get()` suffix does not remove the need for `public`
- `private var _X: ImageVector? = null` — `private` is explicit; no violation
- `internal fun loadX(): ImageVector` (if used) — `internal` is explicit; no violation

**How to avoid:** Pass `--explicit-mode` to Valkyrie CLI — it adds `public` modifiers to generated code. Run `./gradlew :library:compileKotlin` after the 7-icon spike before proceeding to batch.

### Pitfall 5: Tint Not Passed Explicitly

**What goes wrong:** `Icon(AeroIcons.X, contentDescription = null)` — no explicit tint. `Icon()` defaults to `LocalContentColor`, which `AeroTheme` does NOT set via `CompositionLocalProvider`. On AeroDark background (`#0A0A1A`), Material's default `LocalContentColor` resolves to an unpredictable color that may be invisible.

**How to avoid:** Always pass `tint = AeroTheme.colors.X` explicitly in all `Icon()` calls inside `:library` and `:showcase`. Document this in the object-level KDoc (mandatory per CONTEXT.md). Phase 4 does not call `Icon()` at all (it's pure data), so this is a documentation-only concern in Phase 4.

### Pitfall 6: Valkyrie Generates AeroIcons.kt in `internal/` Subdirectory

**What goes wrong:** Valkyrie's `--output-path library/src/.../icons/internal/` may generate both the per-icon files AND a top-level `AeroIcons.kt` (the object declaration file) in the `internal/` directory.

**How to avoid:** After running Valkyrie, check if it generated an `AeroIcons.kt` in `icons/internal/`. If so, move it to `icons/AeroIcons.kt` (one level up). The facade must live at `com.mordred.aero.icons.AeroIcons`, not `com.mordred.aero.icons.internal.AeroIcons`.

### Pitfall 7: .pin File Recovery Path

**What goes wrong:** The pinned commit SHA is lost or the `.pin` file format is wrong, making future icon additions require guessing which version was used.

**How to avoid:** The `.pin` file has exactly two lines:
```
2b75f3ad12b420c9504ef05df8d2564a28f8500e
https://github.com/phosphor-icons/core/tree/2b75f3ad12b420c9504ef05df8d2564a28f8500e
```
(The SHA above is the current `main` HEAD as of 2026-04-29. Verify before pinning.)

Recovery command in `tools/phosphor-svgs/README.md`:
```bash
# Fetch a specific icon not in the vendored set:
SHA=$(head -1 tools/phosphor-svgs/.pin)
curl -o tools/phosphor-svgs/regular/<name>.svg \
  "https://raw.githubusercontent.com/phosphor-icons/core/${SHA}/raw/regular/<name>.svg"
```

---

## Code Examples

### Fetching 138 SVGs from Phosphor at Pinned SHA

```bash
# Recommended approach: curl each file individually from pinned SHA
SHA="2b75f3ad12b420c9504ef05df8d2564a28f8500e"  # verify current SHA before use
BASE="https://raw.githubusercontent.com/phosphor-icons/core/${SHA}/raw/regular"
DEST="tools/phosphor-svgs/regular"

mkdir -p "$DEST"

# Example for 7 spot-check icons:
for name in x caret-down magnifying-glass check info frame-corners square; do
  curl -o "${DEST}/${name}.svg" "${BASE}/${name}.svg"
done

# Batch for all 138: use the kebab-case names from the master list below.
# Windows Git Bash supports curl natively.
```

Alternative (sparse checkout — heavier but ensures no misses):
```bash
git clone --no-checkout --depth 1 https://github.com/phosphor-icons/core.git /tmp/ph-core
cd /tmp/ph-core
git sparse-checkout set raw/regular
git checkout
# Then copy only the 138 needed files to tools/phosphor-svgs/regular/
```

### Valkyrie Invocation (7-icon spike)

```bash
# From repo root: C:/1A_WORK/ui_lib/
./tools/valkyrie svgxml2imagevector \
  --input-path tools/phosphor-svgs/regular/ \
  --output-path library/src/main/kotlin/com/mordred/aero/icons/internal/ \
  --package-name com.mordred.aero.icons.internal \
  --iconpack-name AeroIcons \
  --output-format backing-property \
  --explicit-mode

# After running: inspect generated files. Key things to confirm:
# 1. viewportWidth = 256f (not 24f)
# 2. strokeLineWidth = 16f (not 12f)
# 3. fill = SolidColor(Color.Transparent) or strokeAlpha present
# 4. strokeLineCap = StrokeCap.Round
# 5. strokeLineJoin = StrokeJoin.Round
# 6. Check if AeroIcons.kt was generated in internal/ — move to icons/ if so
```

### Verification Commands (Phase 4 gates)

```bash
# Gate 1: compile passes
./gradlew :library:compileKotlin

# Gate 2: no Feather/wrong-viewport artifacts
grep -rn "viewportWidth=24f" library/src/main/kotlin/com/mordred/aero/icons/
# Must return 0 hits.

# Gate 3: correct stroke-width present (sample check)
grep -rn "strokeLineWidth=16f" library/src/main/kotlin/com/mordred/aero/icons/internal/
# Must return hits for every generated file.

# Gate 4: explicit public modifiers
grep -rn "val.*ImageVector" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt | grep -v "public"
# Must return 0 hits (every val property has 'public').
```

### AeroIcons.kt Template (full structure for 04-01 spike, 7 icons)

```kotlin
package com.mordred.aero.icons

import androidx.compose.ui.graphics.vector.ImageVector

// Per Claude's Discretion: if Valkyrie uses extension properties (pattern A),
// this file is just: public object AeroIcons
// If Valkyrie uses loadX() internal functions (pattern B), include the getters here.
// Inspect generated output from 7-icon spike before finalizing this file.

/**
 * Typed [ImageVector] constants — Phosphor Icons Regular weight.
 * ... (full KDoc per CONTEXT.md spec)
 */
public object AeroIcons {

    // ─── C ──────────────────────────────────────────────────────
    public val CaretDown: ImageVector get() = _CaretDown ?: loadCaretDown().also { _CaretDown = it }
    private var _CaretDown: ImageVector? = null

    public val Check: ImageVector get() = _Check ?: loadCheck().also { _Check = it }
    private var _Check: ImageVector? = null

    // ─── F ──────────────────────────────────────────────────────
    public val FrameCorners: ImageVector get() = _FrameCorners ?: loadFrameCorners().also { _FrameCorners = it }
    private var _FrameCorners: ImageVector? = null

    // ─── I ──────────────────────────────────────────────────────
    public val Info: ImageVector get() = _Info ?: loadInfo().also { _Info = it }
    private var _Info: ImageVector? = null

    // ─── M ──────────────────────────────────────────────────────
    public val MagnifyingGlass: ImageVector get() = _MagnifyingGlass ?: loadMagnifyingGlass().also { _MagnifyingGlass = it }
    private var _MagnifyingGlass: ImageVector? = null

    // ─── S ──────────────────────────────────────────────────────
    public val Square: ImageVector get() = _Square ?: loadSquare().also { _Square = it }
    private var _Square: ImageVector? = null

    // ─── X ──────────────────────────────────────────────────────
    public val X: ImageVector get() = _X ?: loadX().also { _X = it }
    private var _X: ImageVector? = null
}
```

---

## Final 138-Icon List (deduplicated; row #139 `arrow-up-right` removed)

The duplicate: `arrow-up-right` appears at original rows #10 and #139. Row #139 is deleted. All references elsewhere must be updated from 139→138.

| # | Phosphor kebab-case | Kotlin `AeroIcons.*` | SVG filename | Required |
|---|---------------------|---------------------|--------------|----------|
| 1 | `archive` | `Archive` | archive.svg | |
| 2 | `arrow-bend-up-left` | `ArrowBendUpLeft` | arrow-bend-up-left.svg | |
| 3 | `arrow-clockwise` | `ArrowClockwise` | arrow-clockwise.svg | |
| 4 | `arrow-counter-clockwise` | `ArrowCounterClockwise` | arrow-counter-clockwise.svg | |
| 5 | `arrow-down` | `ArrowDown` | arrow-down.svg | |
| 6 | `arrow-left` | `ArrowLeft` | arrow-left.svg | |
| 7 | `arrow-right` | `ArrowRight` | arrow-right.svg | |
| 8 | `arrow-square-out` | `ArrowSquareOut` | arrow-square-out.svg | |
| 9 | `arrow-up` | `ArrowUp` | arrow-up.svg | |
| 10 | `arrow-up-right` | `ArrowUpRight` | arrow-up-right.svg | |
| 11 | `arrows-down-up` | `ArrowsDownUp` | arrows-down-up.svg | |
| 12 | `battery-empty` | `BatteryEmpty` | battery-empty.svg | |
| 13 | `battery-full` | `BatteryFull` | battery-full.svg | |
| 14 | `battery-low` | `BatteryLow` | battery-low.svg | |
| 15 | `bell` | `Bell` | bell.svg | |
| 16 | `bell-slash` | `BellSlash` | bell-slash.svg | |
| 17 | `bluetooth` | `Bluetooth` | bluetooth.svg | |
| 18 | `bookmark-simple` | `BookmarkSimple` | bookmark-simple.svg | |
| 19 | `broom` | `Broom` | broom.svg | |
| 20 | `bug` | `Bug` | bug.svg | |
| 21 | `calendar` | `Calendar` | calendar.svg | |
| 22 | `calendar-blank` | `CalendarBlank` | calendar-blank.svg | |
| 23 | `camera` | `Camera` | camera.svg | |
| 24 | `caret-down` | `CaretDown` | caret-down.svg | YES |
| 25 | `caret-left` | `CaretLeft` | caret-left.svg | |
| 26 | `caret-right` | `CaretRight` | caret-right.svg | YES |
| 27 | `caret-up` | `CaretUp` | caret-up.svg | YES |
| 28 | `chat-circle` | `ChatCircle` | chat-circle.svg | |
| 29 | `chat-circle-text` | `ChatCircleText` | chat-circle-text.svg | |
| 30 | `check` | `Check` | check.svg | YES |
| 31 | `check-circle` | `CheckCircle` | check-circle.svg | YES |
| 32 | `clipboard` | `Clipboard` | clipboard.svg | |
| 33 | `clock` | `Clock` | clock.svg | |
| 34 | `cloud` | `Cloud` | cloud.svg | |
| 35 | `cloud-arrow-down` | `CloudArrowDown` | cloud-arrow-down.svg | |
| 36 | `cloud-arrow-up` | `CloudArrowUp` | cloud-arrow-up.svg | |
| 37 | `code` | `Code` | code.svg | |
| 38 | `copy` | `Copy` | copy.svg | |
| 39 | `cpu` | `Cpu` | cpu.svg | |
| 40 | `database` | `Database` | database.svg | |
| 41 | `desktop-tower` | `DesktopTower` | desktop-tower.svg | |
| 42 | `dots-three` | `DotsThree` | dots-three.svg | |
| 43 | `dots-three-vertical` | `DotsThreeVertical` | dots-three-vertical.svg | |
| 44 | `download` | `Download` | download.svg | |
| 45 | `envelope` | `Envelope` | envelope.svg | |
| 46 | `eye` | `Eye` | eye.svg | YES |
| 47 | `eye-slash` | `EyeSlash` | eye-slash.svg | YES |
| 48 | `fast-forward` | `FastForward` | fast-forward.svg | |
| 49 | `file` | `File` | file.svg | |
| 50 | `file-text` | `FileText` | file-text.svg | |
| 51 | `files` | `Files` | files.svg | |
| 52 | `flag` | `Flag` | flag.svg | |
| 53 | `floppy-disk` | `FloppyDisk` | floppy-disk.svg | |
| 54 | `folder` | `Folder` | folder.svg | YES |
| 55 | `folder-open` | `FolderOpen` | folder-open.svg | |
| 56 | `folder-plus` | `FolderPlus` | folder-plus.svg | |
| 57 | `frame-corners` | `FrameCorners` | frame-corners.svg | YES (restore) |
| 58 | `funnel` | `Funnel` | funnel.svg | |
| 59 | `gear` | `Gear` | gear.svg | |
| 60 | `gear-six` | `GearSix` | gear-six.svg | |
| 61 | `globe` | `Globe` | globe.svg | |
| 62 | `hard-drive` | `HardDrive` | hard-drive.svg | |
| 63 | `hash` | `Hash` | hash.svg | |
| 64 | `heart` | `Heart` | heart.svg | |
| 65 | `house` | `House` | house.svg | |
| 66 | `image` | `Image` | image.svg | |
| 67 | `info` | `Info` | info.svg | YES |
| 68 | `key` | `Key` | key.svg | |
| 69 | `lightning` | `Lightning` | lightning.svg | |
| 70 | `lightbulb` | `Lightbulb` | lightbulb.svg | |
| 71 | `link` | `Link` | link.svg | |
| 72 | `link-simple` | `LinkSimple` | link-simple.svg | |
| 73 | `list` | `List` | list.svg | |
| 74 | `lock` | `Lock` | lock.svg | |
| 75 | `lock-open` | `LockOpen` | lock-open.svg | |
| 76 | `magnifying-glass` | `MagnifyingGlass` | magnifying-glass.svg | YES |
| 77 | `magnifying-glass-minus` | `MagnifyingGlassMinus` | magnifying-glass-minus.svg | |
| 78 | `magnifying-glass-plus` | `MagnifyingGlassPlus` | magnifying-glass-plus.svg | |
| 79 | `map-pin` | `MapPin` | map-pin.svg | |
| 80 | `microphone` | `Microphone` | microphone.svg | |
| 81 | `microphone-slash` | `MicrophoneSlash` | microphone-slash.svg | |
| 82 | `minus` | `Minus` | minus.svg | YES |
| 83 | `minus-circle` | `MinusCircle` | minus-circle.svg | |
| 84 | `monitor` | `Monitor` | monitor.svg | |
| 85 | `music-note` | `MusicNote` | music-note.svg | |
| 86 | `music-notes` | `MusicNotes` | music-notes.svg | |
| 87 | `paperclip` | `Paperclip` | paperclip.svg | |
| 88 | `paper-plane` | `PaperPlane` | paper-plane.svg | |
| 89 | `pause` | `Pause` | pause.svg | |
| 90 | `pencil-simple` | `PencilSimple` | pencil-simple.svg | |
| 91 | `phone` | `Phone` | phone.svg | |
| 92 | `play` | `Play` | play.svg | |
| 93 | `plus` | `Plus` | plus.svg | |
| 94 | `plus-circle` | `PlusCircle` | plus-circle.svg | |
| 95 | `power` | `Power` | power.svg | |
| 96 | `printer` | `Printer` | printer.svg | |
| 97 | `prohibit` | `Prohibit` | prohibit.svg | |
| 98 | `question` | `Question` | question.svg | YES |
| 99 | `rewind` | `Rewind` | rewind.svg | |
| 100 | `scissors` | `Scissors` | scissors.svg | |
| 101 | `share-network` | `ShareNetwork` | share-network.svg | |
| 102 | `shield` | `Shield` | shield.svg | |
| 103 | `shield-warning` | `ShieldWarning` | shield-warning.svg | |
| 104 | `sign-in` | `SignIn` | sign-in.svg | |
| 105 | `sign-out` | `SignOut` | sign-out.svg | |
| 106 | `skip-back` | `SkipBack` | skip-back.svg | |
| 107 | `skip-forward` | `SkipForward` | skip-forward.svg | |
| 108 | `sliders` | `Sliders` | sliders.svg | |
| 109 | `sliders-horizontal` | `SlidersHorizontal` | sliders-horizontal.svg | |
| 110 | `sort-ascending` | `SortAscending` | sort-ascending.svg | |
| 111 | `sort-descending` | `SortDescending` | sort-descending.svg | |
| 112 | `speaker-high` | `SpeakerHigh` | speaker-high.svg | |
| 113 | `speaker-low` | `SpeakerLow` | speaker-low.svg | |
| 114 | `speaker-x` | `SpeakerX` | speaker-x.svg | |
| 115 | `spinner` | `Spinner` | spinner.svg | |
| 116 | `square` | `Square` | square.svg | YES (maximize) |
| 117 | `star` | `Star` | star.svg | |
| 118 | `stop` | `Stop` | stop.svg | |
| 119 | `terminal-window` | `TerminalWindow` | terminal-window.svg | |
| 120 | `trash` | `Trash` | trash.svg | |
| 121 | `trash-simple` | `TrashSimple` | trash-simple.svg | |
| 122 | `upload` | `Upload` | upload.svg | |
| 123 | `user` | `User` | user.svg | |
| 124 | `user-check` | `UserCheck` | user-check.svg | |
| 125 | `user-circle` | `UserCircle` | user-circle.svg | |
| 126 | `user-minus` | `UserMinus` | user-minus.svg | |
| 127 | `user-plus` | `UserPlus` | user-plus.svg | |
| 128 | `users` | `Users` | users.svg | |
| 129 | `video-camera` | `VideoCamera` | video-camera.svg | |
| 130 | `warning` | `Warning` | warning.svg | YES |
| 131 | `warning-circle` | `WarningCircle` | warning-circle.svg | |
| 132 | `warning-diamond` | `WarningDiamond` | warning-diamond.svg | |
| 133 | `warning-octagon` | `WarningOctagon` | warning-octagon.svg | |
| 134 | `wifi-high` | `WifiHigh` | wifi-high.svg | |
| 135 | `wifi-slash` | `WifiSlash` | wifi-slash.svg | |
| 136 | `wrench` | `Wrench` | wrench.svg | |
| 137 | `x` | `X` | x.svg | YES |
| 138 | `x-circle` | `XCircle` | x-circle.svg | YES |

**Count verification:** 138 unique icons. Row #139 `arrow-up-right` from FEATURES.md is a duplicate of row #10 and is excluded.

**Required icons (17 unique covering all migration targets in Phase 5):**
`X, CaretDown, CaretUp, CaretRight, Check, CheckCircle, Minus, Square, FrameCorners,
MagnifyingGlass, Eye, EyeSlash, Folder, Info, Warning, XCircle, Question`

**Naming edge cases — all confirmed safe:**
- Single letter: `x` → `X` (valid Kotlin identifier)
- Numbers spelled out: `gear-six` → `GearSix` (not `Gear6`)
- Hyphens stripped, each segment capitalized: `arrow-counter-clockwise` → `ArrowCounterClockwise`
- No Kotlin reserved word collisions in any of the 138 names (verified against Kotlin keyword list: `in`, `is`, `as`, `fun`, `val`, `var`, `object`, `class`, `when`, `if`, `else`, `for`, `do`, `try`, etc.)
- `Files` (Kotlin stdlib `kotlin.io.path.Files` exists but is NOT in the namespace; no collision)
- `List` conflicts with `kotlin.collections.List` — use `import` aliasing if needed, but since it's inside `object AeroIcons` as `AeroIcons.List`, there is no collision at the call site

---

## Thread-Safety of Lazy Backing-Property

**Answer:** No synchronization is needed. Compose Desktop renders on a single UI thread. The null-check pattern `_X ?: loadX().also { _X = it }` is called exclusively during composition (main thread). Two threads cannot race to initialize `_X` in normal Compose Desktop usage. `LazyThreadSafetyMode.NONE` is appropriate and is what the Material Icons Extended source uses in practice. The backing field pattern is equally safe.

If `_X` is written from multiple threads (it is not in this application), the worst outcome is double-initialization (two allocations of the same ImageVector), not a crash. The `ImageVector` object is immutable after construction. Either write wins. For our use case, this race cannot occur.

---

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| Text glyphs `Text("✕")` | `Icon(AeroIcons.X, tint = colors.onSurface)` | Cross-OS consistent; scalable; tinted by theme |
| Material Icons Extended (~7-9 MB dep) | Phosphor Regular ImageVector constants (~300 KB) | JAR size reduction in Phase 5 |
| Canvas hand-drawn icons (`SearchIcon()`, `EyeOpenIcon()`) | Valkyrie-generated ImageVector | Eliminates custom drawing code |
| Feather SVG source (archived, 24px viewport) | Phosphor Regular (active, 256px viewport, rounded stroke) | Better Aero aesthetic match |

**Deprecated:**
- `Icons.Outlined.*` imports in `:library` — replaced in Phase 5
- `compose.materialIconsExtended` dep in `library/build.gradle.kts` — removed in Phase 5 (CLN-02)

---

## Open Questions

1. **Valkyrie output shape — extension property vs internal function**
   - What we know: CONTEXT.md locks `internal fun loadX(): ImageVector` called from facade. Valkyrie BackingProperty generates extension properties on the AeroIcons object (all property bodies in `internal/*.kt`, facade is just `object AeroIcons`).
   - What's unclear: whether Valkyrie 1.1.1 with `--iconpack-name AeroIcons` and `--output-path .../icons/internal/` generates extension properties on a type from a different package, or generates standalone internal functions.
   - Recommendation: Run the 7-icon spike first, inspect output. If Valkyrie generates extension properties, accept that pattern (matches CONTEXT.md "match Valkyrie's actual output"). If it generates standalone `internal fun` functions, the CONTEXT.md pattern applies as-is. Document the actual output in `tools/phosphor-svgs/README.md`.

2. **Some SVG names may not exist in `raw/regular/`**
   - What we know: `sort-ascending`, `sort-descending` are in the FEATURES.md list. Phosphor uses `arrows-down-up` for general sort but may not have separate ascending/descending variants in `raw/`.
   - What's unclear: Whether `sort-ascending.svg` and `sort-descending.svg` exist in the `raw/regular/` directory.
   - Recommendation: Verify these two names with `curl -I` before the batch run. If missing, substitute `arrows-down-up.svg` for both and record the mapping in README.

3. **`spinner.svg` availability**
   - What we know: `spinner` is in the list. Phosphor has a spinner icon.
   - What's unclear: Whether the stroke-based `raw/regular/spinner.svg` exists vs only fill-based.
   - Recommendation: Verify with `curl -I` before batch run.

---

## Validation Architecture

> `workflow.nyquist_validation` key is absent from `.planning/config.json` — treat as enabled.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | No automated test framework for Phase 4 — compile-time validation only |
| Config file | None (Phase 4 creates no new test files) |
| Quick run command | `./gradlew :library:compileKotlin` |
| Full suite command | `./gradlew :library:compileKotlin` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ICN-01 | `public object AeroIcons` compiles under `explicitApi()`, lazy pattern correct | compile | `./gradlew :library:compileKotlin` | ❌ Wave 0 (file created in 04-01) |
| ICN-01 | Every public constant has `public` modifier | grep | `grep -rn "val.*ImageVector" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt \| grep -v "public"` (must return 0) | ❌ Wave 0 |
| ICN-02 | KDoc present on object | compile + manual | `./gradlew :library:compileKotlin` (would fail if @Suppress("KDocMissingDocumentation") not applied AND strict kdoc mode); manual: grep `/**` in AeroIcons.kt | ❌ Wave 0 |
| ICN-03 | 138 ImageVector constants exist | grep count | `grep -c "public val.*: ImageVector" library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` (must be 138) | ❌ Wave 0 |
| ICN-03 | All constants use Phosphor 256-unit viewBox | grep | `grep -rn "viewportWidth=24f" library/src/main/kotlin/com/mordred/aero/icons/` (must return 0) | ❌ Wave 0 |
| ICN-03 | Correct stroke-width in generated code | grep | `grep -c "strokeLineWidth=16f" library/src/main/kotlin/com/mordred/aero/icons/internal/` (must equal 138+, one per path per file) | ❌ Wave 0 |

### Sampling Rate

- **Per task commit (04-01 spike):** `./gradlew :library:compileKotlin` + viewportWidth grep
- **Per wave merge (04-02 batch complete):** `./gradlew :library:compileKotlin` + all grep checks + property count check
- **Phase gate:** All grep checks green + compile passes before calling Phase 4 done

### Wave 0 Gaps

- [ ] `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — covers ICN-01, ICN-02, ICN-03 (created in 04-01)
- [ ] `library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt` (138 files) — covers ICN-03 (created via Valkyrie in 04-01 and 04-02)
- [ ] No new test framework installation needed — Phase 4 validation is compile-time + grep only
- [ ] No new test files needed — compiler enforces all three ICN requirements

*(No existing test infrastructure gaps for Phase 4 — existing `./gradlew :library:test` passes through; Phase 4 adds no new test files and does not modify any existing test files.)*

---

## Sources

### Primary (HIGH confidence)
- Direct SVG inspection: `raw.githubusercontent.com/phosphor-icons/core/main/raw/regular/{x,check,caret-down,info,magnifying-glass,frame-corners}.svg` — all confirmed stroke-width 16 (not 12)
- Valkyrie CLI source: `github.com/ComposeGears/Valkyrie/blob/main/tools/cli/src/main/kotlin/io/github/composegears/valkyrie/cli/command/SvgXmlToImageVectorCommand.kt` — verified all flag names: `--input-path`, `--output-path`, `--package-name`, `--iconpack-name`, `--output-format`, `--explicit-mode`
- Valkyrie CLI source: `github.com/ComposeGears/Valkyrie/blob/main/tools/cli/src/main/kotlin/io/github/composegears/valkyrie/cli/command/IconPackCommand.kt` — verified iconpack subcommand flags
- `phosphor-icons/core` API: `api.github.com/repos/phosphor-icons/core/contents/assets/regular` and `api.github.com/repos/phosphor-icons/core/contents/raw/regular` — confirmed two directory trees (stroke-based `raw/`, fill-based `assets/`); confirmed filename format `<name>.svg` (no `-regular` suffix in `raw/` directory)
- `.planning/phases/04-aeroicons-foundation/04-CONTEXT.md` — all locked decisions, file layout, verification gates
- `.planning/research/ARCHITECTURE.md` §2 — lazy backing-property pattern; explicitApi() compatibility
- `.planning/research/PITFALLS.md` §1, §5, §9, §11, §12 — pitfall details
- `library/build.gradle.kts` — confirmed `explicitApi()` at line 9; confirmed `materialIconsExtended` present (Phase 5 removes it)

### Secondary (MEDIUM confidence)
- `.planning/research/FEATURES.md` Part 3 — 139-row master list (authoritative modulo the row #139 duplicate)
- `.planning/research/STACK.md` — Valkyrie 1.1.1, CMP/Kotlin version lock — HIGH confidence on versions, stroke-width 12 claim is now corrected to 16
- `phosphor-icons/core` main HEAD SHA `2b75f3ad12b420c9504ef05df8d2564a28f8500e` (2026-01-06) — current as of research date; verify before pinning

### Tertiary (LOW confidence — needs validation at execution time)
- `sort-ascending.svg` and `sort-descending.svg` existence in `raw/regular/` — not verified; check before batch run
- `spinner.svg` existence in `raw/regular/` — not verified; check before batch run
- Valkyrie exact output shape (extension property vs internal function) — cannot confirm without running the tool; discover during 04-01 spike

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions locked in libs.versions.toml; no new dependencies in Phase 4
- Valkyrie CLI flags: HIGH — verified from source code at `tools/cli/src/.../SvgXmlToImageVectorCommand.kt`
- SVG stroke-width (16): HIGH — direct inspection of 6 SVG files from `raw/regular/`; corrects prior research claim of 12
- SVG directory/filename format: HIGH — verified via GitHub API; `raw/regular/<name>.svg` (no `-regular` suffix)
- Architecture patterns (lazy backing property, explicitApi): HIGH — direct codebase inspection + Material Icons reference
- Pitfalls: HIGH — sourced from prior research codebase inspection + new findings
- 138-icon list: HIGH — derived directly from FEATURES.md master table with single duplicate removed
- Valkyrie output shape: LOW — cannot confirm without running the tool; discover in 04-01 spike

**Research date:** 2026-04-29
**Valid until:** 2026-05-29 (30 days; Phosphor is stable; Valkyrie CLI 1.1.1 is pinned)
