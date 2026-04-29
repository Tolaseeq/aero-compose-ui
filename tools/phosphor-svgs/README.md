# tools/phosphor-svgs/

Vendored subset of [Phosphor Icons](https://github.com/phosphor-icons/core) Regular weight,
used by Phase 4 of the v1.1 Icon System milestone to generate the `AeroIcons` typed icon set.

## What is here

| File / Dir | Purpose |
|------------|---------|
| `.pin` | Two-line provenance: line 1 = 40-char Phosphor commit SHA used; line 2 = GitHub tree URL at that SHA |
| `LICENSE` | Verbatim copy of Phosphor's MIT license at the pinned SHA |
| `regular/` | Vendored stroke-based SVGs from `<phosphor-icons/core>/raw/regular/` at the pinned SHA |
| `README.md` | This file |

## Pinned revision

- **SHA**: `2b75f3ad12b420c9504ef05df8d2564a28f8500e` (line 1 of `.pin`)
- **GitHub tree**: <https://github.com/phosphor-icons/core/tree/2b75f3ad12b420c9504ef05df8d2564a28f8500e>
- **License**: MIT (see `LICENSE` for the verbatim text at this revision)

## Why we vendor (rather than fetch at build time)

- **Reproducibility**: The Gradle build does NOT call out to the network. Generated `.kt` files are committed; this directory is the audit trail explaining where they came from.
- **Lean repo**: We vendor only the 138 SVGs we use, not the full ~1300-icon Regular set. Total ~200–300 KB.
- **Recovery path**: The `.pin` file is sufficient to retroactively fetch any unvendored Phosphor icon at the exact same revision (see "Adding a new icon" below).

## Valkyrie CLI 1.1.1 — exact command used to generate the `internal/*.kt` files

Run from the repository root (`C:/1A_WORK/ui_lib/` or equivalent):

```bash
./tools/valkyrie-cli-1.1.1/bin/valkyrie svgxml2imagevector \
  --input-path tools/phosphor-svgs/regular/ \
  --output-path library/src/main/kotlin/com/mordred/aero/icons/internal/ \
  --package-name com.mordred.aero.icons.internal \
  --iconpack-name AeroIcons \
  --output-format backing-property \
  --explicit-mode=true
```

Notes:
- `--output-format` takes `backing-property` (lowercase, hyphenated). NOT `BackingProperty` (camelCase).
- `--explicit-mode=true` (NOT bare `--explicit-mode`) adds `public` modifiers to satisfy `:library`'s `explicitApi()`. Bare flag form causes an error in Valkyrie 1.1.1.
- `--input-path` accepts a directory; Valkyrie processes every `.svg` and `.xml` file inside.
- Valkyrie 1.1.1 does NOT generate `AeroIcons.kt` — the facade must be hand-authored. See "Valkyrie 1.1.1 actual output shape" section below.
- Post-conversion: Valkyrie sets `defaultWidth = 256.dp` from SVG viewBox if no `width` attribute present in SVG. Must be manually corrected to `defaultWidth = 24.dp` after generation.

Valkyrie 1.1.1 download: <https://github.com/ComposeGears/Valkyrie/releases/tag/cli-1.1.1>

## Adding a new icon (recovery / extension procedure)

The vendored set covers Phase 4's 138 icons. To add an icon not in the vendored set:

1. Read the pinned SHA:
   ```bash
   SHA=$(head -1 tools/phosphor-svgs/.pin)
   ```

2. Fetch the SVG at the pinned revision (note: filename in `raw/regular/` is `<name>.svg` — NO `-regular` suffix):
   ```bash
   ICON="some-icon-name"   # kebab-case, e.g. "caret-down"
   curl -fsSL "https://raw.githubusercontent.com/phosphor-icons/core/${SHA}/raw/regular/${ICON}.svg" \
     -o "tools/phosphor-svgs/regular/${ICON}.svg"
   ```

3. Re-run the Valkyrie command above. Valkyrie regenerates only the affected `internal/*.kt` files; the others remain untouched (it's a deterministic SVG→Kotlin transformation).

4. Add the new property to `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` in alphabetical order:
   ```kotlin
   public val SomeIconName: ImageVector get() = _SomeIconName ?: loadSomeIconName().also { _SomeIconName = it }
   private var _SomeIconName: ImageVector? = null
   ```
   (or, if Valkyrie generates extension properties on `AeroIcons` directly, no facade edit is needed — verify by inspecting the generated file shape.)

5. Compile-verify:
   ```bash
   ./gradlew :library:compileKotlin
   ```

## Why `raw/regular/` (NOT `assets/regular/`)

Phosphor's `core` repository has TWO icon directories with the same names:
- `raw/regular/<name>.svg` — STROKE-based (stroke="currentColor", fill="none", stroke-width=16). **This is what we use.**
- `assets/regular/<name>-regular.svg` — FILL-based (fill="currentColor"). Wrong for our soft-outline aesthetic.

Always fetch from `raw/regular/`. The filename in this directory has NO `-regular` suffix.

## Valkyrie 1.1.1 actual output shape (verified 2026-04-29 spike)

Valkyrie 1.1.1 with `--output-format backing-property --iconpack-name AeroIcons` generates:
- **Shape A (extension properties on AeroIcons)** — each `internal/*.kt` file contains a `public val AeroIcons.<Name>: ImageVector` extension property with a backing `private var _<Name>: ImageVector?` field. The lazy caching is implemented inline in the getter body.
- Per-icon files contain: `public val AeroIcons.X: ImageVector get() { if (_X != null) { return _X!! }; _X = ImageVector.Builder(...).apply { ... }.build(); return _X!! }`
- The facade `AeroIcons.kt` was NOT generated by Valkyrie and is hand-authored in Task 5. It only needs to declare `public object AeroIcons` — all property bodies live as extension properties in `internal/*.kt`.

This determines the structure of `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt`:
- Shape A: `AeroIcons.kt` is just `public object AeroIcons`; all properties are extension properties in `internal/*.kt`. No per-property declarations needed in the facade body.

Additional Valkyrie 1.1.1 behaviors discovered during the spike:
- `defaultWidth`/`defaultHeight`: Set to SVG viewBox size (256.dp for Phosphor). Must be corrected to 24.dp post-generation.
- `fill` parameter: Omitted for stroke-only paths (defaults to null in Compose — correct behavior). For icons with genuine filled shapes (e.g., Info dot), Valkyrie correctly emits `fill = SolidColor(Color.Black)`.
- `stroke`: Always `SolidColor(Color.Black)` (Valkyrie uses Color.Black as the literal; tint overrides at the `Icon()` call site).

## Plan 04-02 SVG existence verification (verified 2026-04-29)

The following names from the 138-icon master list were flagged LOW-confidence in
04-RESEARCH.md and HEAD-checked at the pinned SHA before the bulk fetch:

| Name | HTTP code | Decision |
|------|-----------|----------|
| `sort-ascending` | 200 | vendored as-is |
| `sort-descending` | 200 | vendored as-is |
| `spinner` | 200 | vendored as-is |

Final 138-icon target after substitutions: 138.

## License attribution

Phosphor Icons is © Helena Zhang and Tobias Fried, licensed MIT. See `LICENSE` for the
verbatim text. Per MIT, attribution is preserved here and propagated by the `AeroIcons.kt`
KDoc, which references the source repository and license.
