# Project Research Summary

**Project:** aero-compose-ui -- v1.1 Icon System
**Domain:** Compose Desktop UI component library -- typed icon set introduction (Phosphor Regular)
**Researched:** 2026-04-29
**Confidence:** HIGH

## Executive Summary

v1.1 replaces every text-glyph icon and all compose.materialIconsExtended usage with a unified typed vector icon set derived from Phosphor Icons Regular weight. Phosphor was chosen over Feather because its softer rounded stroke at 256-unit viewBox more closely matches the Win7-toolbar-glyph aesthetic that defines aero-compose-ui's visual identity. Valkyrie CLI 1.1.1 batch-converts selected Phosphor SVGs to Kotlin ImageVector builders, producing one .kt file per icon under com.mordred.aero.icons.internal/, with a public AeroIcons facade object exposing all 139 typed constants via lazy backing properties. No new runtime dependencies are added; compose.materialIconsExtended is removed.

The 139-icon set covers all existing component migration requirements (17 required icons) plus a standard desktop UI vocabulary. Icons are named verbatim after their Phosphor source names in PascalCase -- CaretDown not ChevronDown, MagnifyingGlass not Search, Gear not Settings, House not Home, Funnel not Filter -- ensuring frictionless 1-to-1 lookup against phosphoricons.com. The set lives inside :library (not a separate Gradle module), preserving single-artifact delivery.

The primary risks are mechanical rather than conceptual: lazy initialization must be enforced from the start (eager val at 139 icons causes a measurable startup spike); compose.materialIconsExtended removal requires purging 4 files (2 source + 2 test) before the Gradle line is touched; AeroNumberSpinner's 16x12dp buttons are too small for Icon() at the Phosphor stroke weight -- at 10dp render the stroke is 0.47dp sub-pixel -- and require a Canvas or oversized-button solution; tint must always be passed explicitly because Icon()'s default LocalContentColor is not set by AeroTheme.

---

## Key Findings

### Recommended Stack

The existing stack (Kotlin 2.1.21, CMP 1.7.3, Gradle 8.14.3, JDK 17) requires zero changes for v1.1. The only build-file modification is removal of implementation(compose.materialIconsExtended) from library/build.gradle.kts -- done last, after all migration is verified.

**Tooling:**
- **Valkyrie CLI 1.1.1** (ComposeGears/Valkyrie) -- batch SVG-to-ImageVector; --output-format BackingProperty; run once offline, commit output
- **Phosphor Icons Regular** (github.com/phosphor-icons/core, MIT) -- SVGs at raw/regular/*.svg; viewBox 256x256, stroke-width 12, stroke-linecap round

**Generated builder constants (Phosphor -- override Feather values in STACK.md body):**
- defaultWidth = 24.dp, defaultHeight = 24.dp (render target, unchanged)
- viewportWidth = 256f, viewportHeight = 256f (Feather was 24f)
- strokeLineWidth = 12f (Feather was 2f -- Valkyrie handles this automatically)

### Expected Features

**Must have (table stakes -- v1.1 launch blockers):**
- AeroIcons object with 139 typed ImageVector constants, flat namespace, lazy backing property, explicitApi() compatible
- All 17 required migration icons present before any component touch
- All 10 component text-glyph migrations: AeroCheckbox, AeroDropdown, AeroNumberSpinner, AeroTitleBar, AeroToastHost, AeroNotificationBanner, AeroContextMenu, AeroSearchField, AeroPasswordField, plus AeroAlertKind/AeroBannerKind off Icons.Outlined.*
- compose.materialIconsExtended removed from :library/build.gradle.kts
- IconsSection in showcase -- LazyVerticalGrid of all 139 icons with name labels, AeroSearchField live filter

**Critical naming table (Phosphor deviates from industry conventions):**

| Use | Not |
|-----|-----|
| AeroIcons.CaretDown | ChevronDown |
| AeroIcons.MagnifyingGlass | Search |
| AeroIcons.Gear | Settings |
| AeroIcons.House | Home |
| AeroIcons.Funnel | Filter |
| AeroIcons.EyeSlash | EyeOff |
| AeroIcons.X | Close |
| AeroIcons.Warning | AlertTriangle |
| AeroIcons.XCircle | Error / AlertCircle |
| AeroIcons.Question | HelpCircle / HelpOutline |
| AeroIcons.Envelope | Mail |
| AeroIcons.PaperPlane | Send |

Full 139-icon master list: FEATURES.md section 3 (Required column marks the 17 migration-blocking icons).

**Defer to v1.2+:** Filled/Bold/Duotone variants; separate :icons Gradle module; custom icon registration API; brand/currency/weather/medical icons.
### Architecture Approach

The architecture follows the Material Icons Extended pattern exactly: a public object AeroIcons facade exposes 139 constants as public val Name: ImageVector get() lazy properties backed by private var _Name: ImageVector? = null; per-icon ImageVector.Builder calls live in internal fun functions inside icons/internal/ (one file per icon). compose.material3.Icon() is used at all call sites -- no custom AeroIcon() wrapper. Tint is always passed explicitly; LocalContentColor is never relied upon inside library code.

**Major components:**
1. **icons/AeroIcons.kt** -- public facade, 139 lazy-property constants, KDoc with naming guide and phosphoricons.com lookup instructions
2. **icons/internal/*.kt** -- one file per icon; internal fun loadX(): ImageVector with viewportWidth=256f, viewportHeight=256f, strokeLineWidth=12f
3. **Migrated library components** -- Text(glyph) / Canvas drawing / Icons.Outlined.* replaced with Icon(AeroIcons.Name, tint=colors.X, modifier=Modifier.size(Ndp))
4. **IconsSection.kt (showcase)** -- LazyVerticalGrid(GridCells.Adaptive(80.dp)), bounded Modifier.height(400.dp), AeroSearchField filter

**API decision locked:** AeroBreadcrumb.separator stays as a String parameter in v1.1 -- intentional. Only the internal submenu indicator in AeroContextMenu is migrated to Icon(AeroIcons.CaretRight).

### Critical Pitfalls

1. **Eager ImageVector initialization** -- every constant must use the lazy backing pattern (get() = _X ?: loadX().also { _X = it }). Never eager val. Verify after writing first 5 icons before batch-generating the rest. (PITFALLS.md section 1)

2. **materialIconsExtended removal requires 4-file purge including tests** -- AeroAlertKindTest.kt and AeroBannerKindTest.kt import Icons.Outlined.* in assertions. Grep library/src/ before touching the Gradle line; must return zero results. (PITFALLS.md sections 2, 4)

3. **AeroNumberSpinner buttons too small for Phosphor stroke** -- at 10dp render, stroke = 10*(12/256) = 0.47dp sub-pixel. Raise button to 14dp+ with Modifier.size(12.dp), or Canvas-draw the chevrons. (PITFALLS.md section 8)

4. **Tint discipline -- always explicit** -- Icon()'s default tint is LocalContentColor, which AeroTheme does not set. Every Icon() in :library must pass tint=colors.X explicitly. Verify in all three themes. (PITFALLS.md section 5)

5. **Phosphor naming confusion** -- AeroIcons.Close does not exist; the name is AeroIcons.X. Document in KDoc with full naming table. (PITFALLS.md Phosphor revision point 3)

6. **SVG conversion verification** -- spot-check 5 icons after Valkyrie batch: viewportWidth=256f (not 24f), strokeLineWidth=12f (not 2f), fill=Color.Transparent. Grep for viewportWidth=24f in icons/internal/ -- must return nothing. (PITFALLS.md section 9)

7. **Easy-to-miss clear button glyph** -- AeroSearchField line 121 uses a plain lowercase-x text character, not a Unicode glyph. Add a dedicated grep for this pattern to the migration checklist. (PITFALLS.md section 16)
---

## Implications for Roadmap

v1.0 completed through Phase 3. v1.1 continues numbering from Phase 4.

### Phase 4: AeroIcons Foundation

**Rationale:** Hard build-order gate -- all 139 component migrations and the showcase require AeroIcons.* to compile. Nothing else in v1.1 can proceed until this exists and is verified.

**Delivers:**
- icons/AeroIcons.kt -- 139 lazy properties, KDoc with naming convention and phosphoricons.com lookup
- icons/internal/*.kt -- 139 ImageVector.Builder files generated via Valkyrie CLI from Phosphor Regular SVGs
- Phosphor SVG source pinned and committed to tools/phosphor-svgs/regular/
- Spot-check of 5 representative icons (viewportWidth=256f, strokeLineWidth=12f, fill=Transparent, strokeLineCap=Round) before batch-generating the rest
- explicitApi() first-compile verified on initial 5 icons
- KDoc: naming convention table, recommended size range 16-32dp, tint requirement, phosphoricons.com URL

**Avoids:** eager init (Pitfall 1), SVG conversion artifacts (Pitfall 9), explicitApi() surprises (Pitfall 11), file hygiene (Pitfall 12)

**Research flag:** No research-phase needed. Material Icons pattern and Valkyrie CLI are HIGH-confidence verified.

---

### Phase 5: Component Migrations + Dependency Removal

**Rationale:** Once AeroIcons compiles, all migrations are unblocked. materialIconsExtended removal can only happen after every consumer -- including test files -- is migrated. This phase ends with a passing build and verified JAR size reduction.

**Delivers:**
- 10 text-glyph component migrations (exact file/line locations in FEATURES.md section 5 and ARCHITECTURE.md section 3)
- AeroAlertKind and AeroBannerKind migrated to AeroIcons.*
- AeroAlertKindTest and AeroBannerKindTest rewritten to assert AeroIcons.* by name
- compose.materialIconsExtended removed; ./gradlew :library:dependencies | grep materialIcons returns nothing
- Full glyph grep passes zero hits in library source (including separate lowercase-x clear-button check)
- JAR size before/after documented

**Wave ordering within phase:**
1. Wave 1 (parallel): AeroCheckbox, AeroDropdown, AeroNumberSpinner, AeroToastHost, AeroNotificationBanner, AeroContextMenu, AeroAlertKind, AeroBannerKind
2. Wave 2 (parallel): AeroSearchField, AeroPasswordField (Canvas composables deleted)
3. Wave 3: AeroTitleBar (private TitleBarButton restructured from glyph: String to icon: ImageVector)
4. Wave 4: test file rewrites (AeroAlertKindTest, AeroBannerKindTest)
5. Wave 5: remove materialIconsExtended + verify

**Avoids:** 4-file purge missed (Pitfalls 2+4), AeroNumberSpinner stroke too thin (Pitfall 8), tint omitted (Pitfall 5), ContextMenu submenu glyph missed (Pitfall 14), JAR size not verified (Pitfall 13)

**Research flag:** No research-phase needed. All 14 migration touchpoints inventoried with exact file/line references.
---

### Phase 6: Showcase IconsSection

**Rationale:** Showcase is the visual sign-off checkpoint for the entire v1.1 milestone. Sequenced last so it exercises the final verified icon set and serves as the three-theme visual validation gate.

**Delivers:**
- showcase/sections/IconsSection.kt -- LazyVerticalGrid(GridCells.Adaptive(80.dp)), bounded Modifier.height(400.dp), AeroSearchField live name filter, explicit tint=AeroTheme.colors.onSurface
- ButtonsSection.kt glyph demos updated to Icon(AeroIcons.CaretUp/CaretDown/X)
- IconsSection registered in ShowcaseApp.kt (after FoundationSection)
- Three-theme visual checkpoint: AeroBlue, AeroDark, Classic -- all 139 icons visible with correct tint
- AeroNumberSpinner disabled-state visual checkpoint in AeroDark

**Avoids:** LazyVerticalGrid unbounded height crash (ARCHITECTURE.md section 6), tint invisible in dark themes (Pitfall 5), stroke too faint at small sizes (Pitfall 15)

**Research flag:** No research-phase needed. Standard Compose patterns throughout.

---

### Phase Ordering Rationale

- Phase 4 to 5 to 6 is a hard dependency chain: foundation must compile before migration, migration must complete (including test files) before dependency removal, showcase is the final visual sign-off
- Migrations within Phase 5 are mostly parallel (Wave 1) with four sequential gates: Canvas deletion after glyph replacements; TitleBar isolated for higher complexity; test rewrites before dep removal; dep removal before showcase sign-off
- AeroBreadcrumb.separator is intentionally excluded from migration -- locked decision, not an oversight

### Research Flags

- **Phase 4:** No additional research needed
- **Phase 5:** No additional research needed
- **Phase 6:** No additional research needed

All three phases use well-documented patterns with HIGH-confidence verified specifics.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Versions locked in libs.versions.toml; Valkyrie CLI 1.1.1 confirmed active Feb 2026; Phosphor MIT license confirmed |
| Features | HIGH | 139 icons cross-referenced against official Phosphor React port and Kotlin port; all PascalCase names verified against phosphoricons.com |
| Architecture | HIGH | Direct codebase inspection of all 14 migration targets; exact file/line locations confirmed |
| Pitfalls | HIGH | Sourced from direct code inspection; AeroNumberSpinner size math computed; Phosphor stroke scaling recalculated for 256-unit viewBox |

**Overall confidence:** HIGH

### Gaps to Address

- **AeroNumberSpinner size decision:** Research confirms the sub-pixel stroke problem at 10dp but defers implementation choice (Canvas draw vs button height increase) to Phase 5 execution. Implementer must test visually.
- **Phosphor SVG commit hash:** The exact commit to pin from phosphor-icons/core is resolved at conversion time. Record in tools/phosphor-svgs/.pin or a README alongside the SVGs.
- **frame-corners and square visual weight:** These two title-bar icons must be spot-checked after Valkyrie conversion to confirm acceptable appearance against AeroTitleBar's glassmorphic background.
- **AeroBreadcrumb separator:** Confirmed intentionally left as String -- document in Phase 5 plan so it is not accidentally changed during migration sweep.

---

## Sources

### Primary (HIGH confidence)
- github.com/phosphor-icons/core -- SVG source repository, file naming, viewBox 256x256 spec, stroke-width 12 spec
- github.com/phosphor-icons/react -- confirms Question, SpeakerHigh/Low/X, BatteryFull/Low/Empty names
- github.com/dev778g-me/PhosphorIcon-compose -- official Kotlin/CMP port; all 139 PascalCase names confirmed
- github.com/ComposeGears/Valkyrie (cli-1.1.1, Feb 2026) -- batch conversion tool; BackingProperty output format
- Direct codebase inspection -- all 14 migration target files in :library; AeroAlertKindTest.kt; AeroBannerKindTest.kt; library/build.gradle.kts line 15; ButtonsSection.kt lines 50-52
- Material Icons Extended source -- lazy backing-field pattern reference
- phosphoricons.com -- official icon browser, name verification

### Secondary (MEDIUM confidence)
- iconbolt.com/iconsets/phosphor-regular/* -- cross-reference for arrow-square-out, lock-open
- icon-sets.iconify.design/ph/ -- cross-reference for wifi-high, wifi-slash, frame-corners
- github.com/adamglin0/compose-phosphor-icon -- secondary Kotlin port namespace confirmation
- github.com/rafaeltonholo/svg-to-compose (v2.2.0) -- fallback conversion tool if Valkyrie has issues

---
*Research completed: 2026-04-29*
*Ready for roadmap: yes*
