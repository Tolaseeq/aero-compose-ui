# Phase 4: AeroIcons Foundation - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Create the typed icon set — 138 Phosphor Icons Regular `ImageVector` constants accessible via `AeroIcons.*` autocomplete with lazy backing-property initialization, satisfying `explicitApi()`. Phase 4 delivers ONLY the foundation: facade object, per-icon `internal/` builders, vendored SVG sources, KDoc, and the first compile-pass.

**Out of scope for Phase 4 (handled in later phases):**
- Replacing text glyphs in `AeroCheckbox`, `AeroDropdown`, `AeroNumberSpinner`, etc. — Phase 5 (MIG-01..09)
- Migrating `AeroAlertKind` / `AeroBannerKind` off `Icons.Outlined.*` — Phase 5 (MIG-10/11)
- Removing `compose.materialIconsExtended` from `library/build.gradle.kts` — Phase 5 (CLN-02)
- Showcase `IconsSection` grid + search — Phase 6 (SHW-04/05)
- Three-theme visual checkpoint — Phase 6 (formal sign-off gate for entire v1.1 milestone)

</domain>

<decisions>
## Implementation Decisions

### Icon list (count + structure)
- **Final count: 138 unique icons** (not 139). FEATURES.md Part 3 lists `arrow-up-right` at both row #10 and row #139 — the row #139 duplicate is deleted; numbering renumbered.
- **Count propagation is the FIRST commit of 04-01** — fix references in: `.planning/ROADMAP.md` Phase 4 (success criterion 5 says "of the 139", references "139 ImageVector constants"), `.planning/REQUIREMENTS.md` ICN-03 ("139 ImageVector-constants"), `.planning/STATE.md` `[v1.1 locked decisions]` block, `.planning/research/SUMMARY.md`, `.planning/research/FEATURES.md` Part 3 master list, `.planning/research/ARCHITECTURE.md`. Single source-of-truth fix before any code lands.
- **Curated list accepted as-is** — no further adds or drops beyond the duplicate. The 138 unique icons in FEATURES.md Part 3 are the final v1.1 set.
- **Spot-check icon set (7 icons):** `X`, `CaretDown`, `MagnifyingGlass`, `Check`, `Info`, `FrameCorners`, `Square`. Roadmap default (5) plus the two title-bar control icons that PITFALLS / SUMMARY flagged for visual-weight concerns on glassmorphic backgrounds.
- **Facade organization: alphabetical, single block** — all 138 properties sorted A→Z in `AeroIcons.kt`. Matches Material Icons Extended convention. KDoc at the object level covers Phosphor→PascalCase mapping.

### Phosphor SVG vendoring
- **Vendor only the 138 SVGs we use** to `tools/phosphor-svgs/regular/` (~138 files × ~1–2 KB ≈ 200–300 KB total). NOT the full ~1,300-icon Regular set — lean repo wins; the `.pin` file is the recovery path if more icons are needed later.
- **`.pin` file format: single line** — plain text `<40-char-commit-sha>\nhttps://github.com/phosphor-icons/core/tree/<sha>/raw/regular`. Trivial to parse, trivial to verify, trivial to update. Lives at `tools/phosphor-svgs/.pin`.
- **`tools/phosphor-svgs/README.md`** documents: what was pinned, the exact Valkyrie command used, and how to retroactively extract any unvendored icon (`git checkout <pin> -- raw/regular/<name>.svg` against a clone of `phosphor-icons/core`; filename in `raw/regular/` is `<name>.svg` with NO `-regular` suffix — the suffix appears only in `assets/regular/` which is the wrong fill-based directory).
- **Phosphor MIT license file** (`LICENSE` from the source repo) is committed alongside the vendored SVGs at `tools/phosphor-svgs/LICENSE`. Required for MIT compliance even though the JAR redistributes only generated `.kt` files.

### Tooling (Valkyrie CLI)
- **Tool: Valkyrie CLI 1.1.1** (ComposeGears/Valkyrie) with `--output-format backing-property` (lowercase hyphenated; corrected from prior `BackingProperty` per RESEARCH.md verification of CLI source). Locked.
- **Generated files write direct to `library/src/main/kotlin/com/mordred/aero/icons/internal/`** — Valkyrie invocation uses `--package-name com.mordred.aero.icons.internal`, `--output-path library/src/main/kotlin/com/mordred/aero/icons/internal/`, `--iconpack-name AeroIcons`, `--explicit-mode`. No staging directory. (Flag names corrected per RESEARCH.md source-verification.)
- **Invocation: documented one-shot in plan + `tools/phosphor-svgs/README.md`** — NOT wired into Gradle, NOT a shell script. Plan documents the exact command. Re-run is manual when SVGs change. Matches the locked decision: "Gradle build does NOT invoke Valkyrie at build time."
- **Gradle build does NOT depend on Valkyrie** — generated files are committed as ordinary source.

### File layout (locked from research)
```
library/src/main/kotlin/com/mordred/aero/icons/
├── AeroIcons.kt            ← public facade, 138 alphabetized lazy properties, object-level KDoc
└── internal/
    ├── ArrowBendUpLeft.kt  ← internal fun loadArrowBendUpLeft(): ImageVector
    ├── ArrowClockwise.kt
    ├── ArrowCounterClockwise.kt
    ├── … 135 more, alphabetical by Kotlin name
    └── XCircle.kt
```
- One file per icon under `icons/internal/`, named `<KotlinName>.kt`
- Each builder function is `internal fun load<KotlinName>(): ImageVector` (Kotlin `internal` — visible across `:library` module)
- Per-icon files have NO KDoc (the facade carries naming + spec docs); imports are minimal

### Lazy backing-property pattern (locked)
```kotlin
public object AeroIcons {
    public val X: ImageVector get() = _X ?: loadX().also { _X = it }
    private var _X: ImageVector? = null
    // … 137 more properties, alphabetical
}
```
- Lazy mandatory — eager `val` at 138 icons causes measurable startup spike (PITFALL 1)
- Every public constant marked `public` (explicitApi compatible)
- Every `_Name` backing field is `private`

### KDoc (ICN-02)
- **Object-level KDoc only.** No per-property docs. Single block on `public object AeroIcons` covering:
  1. Phosphor source + MIT license + viewBox `256×256` + stroke `16` (corrected from prior research claim of 12 per RESEARCH.md direct SVG inspection)
  2. kebab→PascalCase mapping rule
  3. Naming-convention table with 6–8 examples covering the surprises:
     - `caret-down` → `CaretDown` (NOT `ChevronDown`)
     - `magnifying-glass` → `MagnifyingGlass` (NOT `Search`)
     - `house` → `House` (NOT `Home`)
     - `funnel` → `Funnel` (NOT `Filter`)
     - `gear` → `Gear` (NOT `Settings`)
     - `x` → `X` (NOT `Close`)
     - `eye-slash` → `EyeSlash` (NOT `EyeOff`)
     - `envelope` → `Envelope` (NOT `Mail`)
  4. Recommended size range: 16dp–32dp (typical render)
  5. **Mandatory explicit `tint` warning** — `Icon()`'s default tint is `LocalContentColor`, which `AeroTheme` does NOT set. Every `Icon()` in `:library` must pass `tint = colors.X` explicitly. (PITFALL 5)
  6. Lookup URL: phosphoricons.com

### Plan breakdown
- **04-01 (Spike, ~1 plan):**
  1. First commit: 139→138 reference fix in ROADMAP/REQUIREMENTS/STATE/SUMMARY/FEATURES/ARCHITECTURE
  2. Vendor 7 SVGs to `tools/phosphor-svgs/regular/` (X, CaretDown, MagnifyingGlass, Check, Info, FrameCorners, Square) + `.pin` file + `LICENSE` + `README.md`
  3. Run Valkyrie on the 7 SVGs → 7 files in `icons/internal/`
  4. Author `AeroIcons.kt` facade with 7 alphabetized properties + full object-level KDoc
  5. Verify: `:library:compileKotlin` passes; `grep -rn 'viewportWidth=24f' library/src/main/kotlin/com/mordred/aero/icons/` returns 0; spot-read all 7 `internal/*.kt` to confirm `viewportWidth=256f` + `strokeLineWidth=16f` + `fill=Color.Transparent` + `StrokeCap.Round` + `StrokeJoin.Round` (stroke-width corrected from 12 to 16 per RESEARCH.md SVG inspection)
  6. Commit each natural step atomically
- **04-02 (Batch, ~1 plan):**
  1. Vendor remaining 131 SVGs to `tools/phosphor-svgs/regular/`
  2. Run Valkyrie on all 131 → 131 files in `icons/internal/`
  3. Add 131 alphabetized properties to `AeroIcons.kt` (facade now has all 138)
  4. Verify: `:library:compileKotlin` passes; same grep returns 0 across the whole `icons/` directory; spot-read 5–10 random additional files
  5. Phase 4 done

### Verification gates (Phase 4 done criteria)
- ✓ `./gradlew :library:compileKotlin` passes — proves: explicitApi compatibility, lazy backing-property pattern compiles, all 138 properties + 138 builder fns resolve
- ✓ `grep -rn 'viewportWidth=24f' library/src/main/kotlin/com/mordred/aero/icons/` returns **0 hits** — proves: every icon converted to Phosphor 256-unit viewBox (Feather artifacts purged)
- ✓ Spot-read 5–10 random `icons/internal/*.kt` files confirm `viewportWidth=256f`, `strokeLineWidth=16f`, `fill=Color.Transparent`, `StrokeCap.Round`, `StrokeJoin.Round` are correct in generated code (stroke-width corrected to 16 per RESEARCH.md)
- ✗ **No smoke test in Phase 4** — ICN-01/02/03 acceptance is structural; the compiler enforces all three. Reflective tests would re-assert what the compiler already proved.
- ✗ **No visual sign-off in Phase 4** — Phase 6 IconsSection is the formal three-theme visual checkpoint. Phase 4 is pure infrastructure; nothing the user sees changes.
- ✗ **No temporary preview render in showcase** — keeps Phase 4 invisible to the user; Phase 6 owns visual sign-off.

### Carry-forward from milestone-level locked decisions (NOT re-asked)
- AeroIcons source: Phosphor Regular (rejected Feather)
- Naming: PascalCase verbatim from Phosphor (X, CaretDown, MagnifyingGlass, Gear, House, Funnel, EyeSlash)
- Lazy backing-property pattern mandatory at this scale
- Use `compose.material3.Icon()` directly at all call sites — no custom `AeroIcon()` wrapper; tint always explicit
- Single `:library` module (no separate `:icons` Gradle module)
- Generated files committed to `src/main`, NOT `build/`
- `AeroBreadcrumb.separator` stays `String` in v1.1 — intentionally NOT migrated
- Win11 `transparent=false` rule (irrelevant to Phase 4 but governs all v1.x window/dialog work)
- 23-token `AeroColorScheme` is NOT extended for icons; tint always pulled from existing tokens

### Claude's Discretion
- Builder function naming style — `loadCaretDown()` (research-suggested) is recommended; small variations like `caretDown()` or `_caretDown()` are acceptable if Valkyrie's `BackingProperty` output uses a different convention. Match Valkyrie's actual generated output rather than fighting the tool.
- Per-icon file imports — minimize as Valkyrie generates; reformat if the output is messy
- Wave 1 of 04-02 (the 131-icon batch) — sequencing within the plan; can be a single commit or split for reviewability if the diff is unwieldy
- Exact wording inside the object-level KDoc beyond the structural requirements above
- Whether to commit `tools/phosphor-svgs/regular/` SVGs in alphabetical name order vs as-downloaded (cosmetic)
- KDoc tag rendering (Markdown vs Dokka @example block) for the naming-convention table

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 4 source-of-truth research (already completed for v1.1)
- `.planning/research/SUMMARY.md` — Phase 4 rationale, deliverables, pitfall summary, confidence assessment
- `.planning/research/FEATURES.md` §Part 3 — Master 139-row icon list (resolve duplicate to 138 unique per this CONTEXT)
- `.planning/research/FEATURES.md` §Part 4 — Naming convention decision rationale (kebab→PascalCase, flat namespace, AeroIcons prefix)
- `.planning/research/ARCHITECTURE.md` §1 — Package and file layout (facade + `internal/` per-icon files); §2 — Public API surface (lazy backing-field pattern with worked example)
- `.planning/research/PITFALLS.md` §1 — Eager init pitfall; §5 — Tint discipline; §9 — SVG conversion verification; §11 — explicitApi() surprises; §12 — File hygiene
- `.planning/research/STACK.md` — Tooling versions (Valkyrie CLI 1.1.1, Phosphor source repo, Kotlin 2.1.21, CMP 1.7.3)

### Milestone-level requirements & roadmap
- `.planning/REQUIREMENTS.md` §Icon Set (Foundation) — ICN-01 (`public object AeroIcons` + lazy pattern + explicitApi), ICN-02 (KDoc requirements), ICN-03 (~138 ImageVector constants — count corrected from 139)
- `.planning/ROADMAP.md` §Phase 4: AeroIcons Foundation — 5 success criteria + phase notes (Phosphor SVG pin, Valkyrie 1.1.1, generated files committed, KDoc requirements, 5-icon spot-check predates this CONTEXT's 7-icon set)
- `.planning/PROJECT.md` §Current Milestone — v1.1 Icon System scope and target features; §Key Decisions — `AeroIcons` source = Phosphor Regular (locked), one weight (Regular), typed constants (not name-based lookup), `materialIconsExtended` removed, single `:library` module
- `.planning/STATE.md` §Accumulated Context — `[v1.1 locked decisions]` block carries every milestone-level decision relevant to Phases 4–6

### Existing codebase (Phase 4 creates `icons/`; nothing to read inside it yet)
- `library/build.gradle.kts` — current `implementation(compose.materialIconsExtended)` line (added in Phase 03 for AeroAlertKind/AeroBannerKind); Phase 5 removes; Phase 4 leaves untouched
- `library/src/main/kotlin/com/mordred/aero/components/` — full migration target inventory (Phase 5 work; Phase 4 references for placement context only)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroColorScheme.kt` — 23-token scheme that downstream `Icon(tint = …)` callers will reference (Phase 4 doesn't touch; Phase 5/6 do)
- `library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt` — `LocalAeroColors` accessor; per the locked decision, `LocalContentColor` is NOT bridged here (KDoc warns explicit-tint requirement)

### External / upstream sources
- `https://github.com/phosphor-icons/core` — SVG source repository; raw SVGs at `raw/regular/<name>.svg` (NO `-regular` suffix in `raw/`; the `-regular` suffix is in `assets/regular/` which is fill-based, not stroke-based — wrong dir); viewBox 256×256, stroke-width **16** (corrected from prior research claim of 12 per RESEARCH.md direct SVG inspection), stroke-linecap round
- `https://github.com/phosphor-icons/core/blob/main/LICENSE` — MIT license text (vendor copy at `tools/phosphor-svgs/LICENSE`)
- `https://github.com/ComposeGears/Valkyrie` — CLI tool 1.1.1 used to convert SVG → Kotlin `ImageVector.Builder`; `--output-format BackingProperty` is the lock-in flag
- `https://phosphoricons.com` — official icon browser; canonical lookup URL referenced in `AeroIcons` KDoc
- `https://github.com/dev778g-me/PhosphorIcon-compose` — secondary Kotlin port; cross-reference for any name disambiguation

### Compose / Material reference (for the lazy pattern)
- `androidx.compose.material.icons.Icons` — Material Icons Extended is the architectural reference for the `private var _X: ImageVector? = null` + lazy getter pattern (PITFALLS.md confirms this is what they use)
- `androidx.compose.ui.graphics.vector.ImageVector` and `ImageVector.Builder` — type signatures for all generated builder functions
- `androidx.compose.ui.graphics.Color`, `SolidColor`, `StrokeCap`, `StrokeJoin` — referenced inside generated `internal/*.kt` files

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (NONE specific to Phase 4)
- The `icons/` package does not exist yet. Phase 4 creates it from scratch.
- `AeroTheme.colors.*` (Phase 1) supplies tint values that Phase 5 callers will pass to `Icon(tint = …)` — Phase 4 only references the contract via KDoc.
- `compose.material3.Icon` is already on the `:library` classpath via the existing material3 dependency — no new Gradle dependency needed for Phase 4 (or Phase 5; only `materialIconsExtended` gets removed).

### Established Patterns
- **explicitApi() throughout `:library`** — every public declaration in `:library/src/main/` must carry an explicit `public` modifier. Applies to `object AeroIcons` and every `val Name: ImageVector` property. Does NOT apply to `internal fun loadName()` — `internal` is sufficient.
- **No Composable wrappers around primitives** — `AeroIcons.*` are plain `ImageVector` values, not Composables. Callers wrap in `material3.Icon(…)`. (Locked decision: no `AeroIcon()` wrapper.)
- **Lazy/cached state via `private var _Name`** — well-established Material Icons pattern; no need to invent a new caching scheme.
- **Generated source committed, not built** — same precedent as: zero generated source in `:library` today, but if it ever appears it's checked in (philosophy aligns with PITFALL 12: file hygiene).
- **One-file-per-thing for component-shaped artifacts** — `:library` already has one file per component under `components/<category>/<Component>.kt`. Phase 4 follows the same shape: one file per icon under `icons/internal/<IconName>.kt`.

### Integration Points (Phase 4 doesn't connect to runtime code yet)
- **No component touches Phase 4 output** — Phase 5 is when `AeroCheckbox`, `AeroDropdown`, etc. start importing `com.mordred.aero.icons.AeroIcons`
- **No showcase touches Phase 4 output** — Phase 6 is when `IconsSection` imports `AeroIcons.*`
- **`library/build.gradle.kts` is unchanged in Phase 4** — no new dependency, no removal yet (Phase 5 owns the `materialIconsExtended` removal)
- **No theme/token wiring in Phase 4** — `AeroIcons` is colorless `ImageVector` data; tint comes from callers via `Icon(tint = …)`

### File-creation manifest (Phase 4 produces)
- `library/src/main/kotlin/com/mordred/aero/icons/AeroIcons.kt` — facade object (1 file)
- `library/src/main/kotlin/com/mordred/aero/icons/internal/<IconName>.kt` × 138 (138 files)
- `tools/phosphor-svgs/regular/<icon-name>.svg` × 138 (138 files; mirrors source filename in `raw/regular/`)
- `tools/phosphor-svgs/.pin` (1 file, plain text)
- `tools/phosphor-svgs/LICENSE` (1 file, MIT text)
- `tools/phosphor-svgs/README.md` (1 file, Valkyrie command + recovery instructions)
- Updates to: `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/research/SUMMARY.md`, `.planning/research/FEATURES.md`, `.planning/research/ARCHITECTURE.md` (139→138 references)

### Things Phase 4 must NOT touch
- Any file under `library/src/main/kotlin/com/mordred/aero/components/` — Phase 5 territory
- `library/build.gradle.kts` — Phase 5 territory (CLN-02 removes `materialIconsExtended`)
- Any `AeroAlertKind.kt` / `AeroBannerKind.kt` — Phase 5 territory (MIG-10/11)
- Any showcase file — Phase 6 territory
- `AeroBreadcrumb.kt` `separator: String` parameter — locked OUT of v1.1 migration scope; do NOT change

</code_context>

<specifics>
## Specific Ideas

- **Master list duplicate is real** — `arrow-up-right` appears at row #10 (Navigation/Actions) and row #139 (Actions). This is a research-doc bug. Phase 4 fixes it as the FIRST commit of 04-01 so the implementation has a single canonical count.
- **Spot-check rationale for FrameCorners + Square** — research SUMMARY explicitly flagged "frame-corners and square visual weight: these two title-bar icons must be spot-checked after Valkyrie conversion to confirm acceptable appearance against AeroTitleBar's glassmorphic background." Folding that check into Phase 4 (rather than waiting for Phase 5 visual checkpoint) catches glyph-quality issues before they propagate.
- **Why no smoke test in Phase 4** — every behavior ICN-01/02/03 demands is structural and compile-time-verifiable: object exists (compiler), KDoc present (compiler if `@Suppress("KDocMissingDocumentation")` not applied), 138 properties typed `ImageVector` (compiler), explicitApi compatible (compiler with `-Xexplicit-api=strict`). A unit test re-asserting these would only catch compiler regressions, which is not Phase 4's job.
- **Why no preview render in Phase 4** — temporary visual UI in showcase becomes immediate technical debt: someone has to remember to remove it when Phase 6's IconsSection lands. Cleaner to keep Phase 4 strictly invisible and Phase 6 owns the first visual surface.
- **Valkyrie `BackingProperty` output format** — produces files where the property's lazy-cached pattern is already wired. Per research, this is the closest match to the Material Icons pattern. If Valkyrie's actual output deviates (e.g., uses `delegate` instead of explicit `private var _X`), we accept Valkyrie's style and update KDoc — fighting the tool would create maintenance burden every regen.
- **MIT license vendor copy** — Phosphor's MIT license technically only requires attribution if redistributing source. We vendor the LICENSE file anyway to make the provenance auditable in `tools/phosphor-svgs/LICENSE` — defensive zero-cost compliance.
- **Phosphor revision pin format `<sha>\n<URL>`** — chose plain text over YAML so that `cat .pin | head -1` returns the SHA directly for scripting; the URL on line 2 is human reference. No keys, no parsing fragility.

</specifics>

<deferred>
## Deferred Ideas

- **Valkyrie wrapped in a Gradle task or shell/PS script** — both raised as alternatives during discussion; rejected for Phase 4. Re-vendor cadence is "occasionally, by hand"; building scaffolding for it is over-engineering. Revisit if regen becomes frequent (e.g., adding 5+ icons in a single sprint).
- **Adding `git-branch`, `pin`, or `tag` to fill the 139th slot** — rejected; we accept 138 unique. Backfilling for a count is cosmetic.
- **Sweeping the curated list for adds/drops** — rejected; the list is locked. Genuine gaps surfaced post-launch can be added via a tiny Phase-4-style mini-plan.
- **Lazy-init smoke test (`assertSame(AeroIcons.X, AeroIcons.X)`)** — rejected for Phase 4; the compiler enforces the contract. Could be added in v1.2 if the lazy contract starts drifting.
- **Reflective property-count test (`AeroIcons::class.memberProperties.size == 138`)** — rejected; reflection-based smoke tests are slow and brittle, and re-assert what the compiler already proved.
- **Per-property KDoc** (`/** Phosphor `caret-down`. */` on each `val`) — rejected for v1.1; object-level KDoc is sufficient. Could be added in v1.2 if developers ask for IDE-hover source-name traceability.
- **Categorical sectioning in facade** (`// ── Files ──` separators between groups) — rejected for v1.1; alphabetical single block is more maintainable. Could revisit at 250+ icons.
- **Phase 4 visual sign-off via temporary preview** — rejected; Phase 6 owns visual sign-off. Adding/removing temporary UI is unnecessary churn.
- **Vendoring all ~1,300 Regular-weight Phosphor SVGs** — rejected; lean vendor wins for v1.1. The `.pin` file + GitHub source is the recovery path.
- **`FrameCorners` vs `Square` final mapping** — confirmed: `Square` = maximize, `FrameCorners` = restore-from-maximized. Phase 5 wires the toggle. No re-decision needed.
- **AeroNumberSpinner sub-pixel mitigation** — explicitly Phase 5's call (Canvas vs button-height-≥-14dp). Phase 4 does NOT pre-judge the choice; the icon `AeroIcons.CaretUp` / `CaretDown` is generated identically either way.
- **Filled / Bold / Light / Duotone weight variants** — already deferred to v2 in REQUIREMENTS.md "Out of Scope".
- **Custom user icon registration API (`AeroIcons.register(...)`)** — already declared anti-feature in research FEATURES.md; consumers pass any `ImageVector` to `Icon()` directly.
- **Separate `:icons` Gradle module** — already deferred to v2 in PROJECT.md Out-of-Scope.

</deferred>

---

*Phase: 04-aeroicons-foundation*
*Context gathered: 2026-04-29*
