# Milestones

## v1.1 Icon System (Shipped: 2026-04-30)

**Phases:** 4–6 (3 phases) | **Plans:** 11 (Phase 4: 2, Phase 5: 6, Phase 6: 3) | **Requirements:** 17/17 (ICN-01..03, MIG-01..11, CLN-01..03, SHW-04..06)

**Timeline:** 2026-04-29 (single-day push, ~10 h: 08:40 → 18:26 +0300)
**Git range:** `19dd352` → `80ef218` (60 commits, 20 `feat`)
**Diff:** 340 files changed, +20,212 / −477 (most insertions are 138 generated `ImageVector` builders in `library/src/main/kotlin/com/mordred/aero/icons/internal/`)

**Delivered:** A typed `AeroIcons` set of 138 Phosphor Regular `ImageVector` constants replaces every text glyph and every `Icons.Outlined.*` usage in the library; `compose.materialIconsExtended` is gone from the Gradle dependency graph; the showcase has a searchable `IconsSection` grid signed off across all three themes.

**Key accomplishments:**
1. **AeroIcons Foundation (Phase 4)** — 138 Phosphor Regular `ImageVector` constants generated via Valkyrie 1.1.1 (`--output-format BackingProperty`), lazy backing-property pattern, `explicitApi()`-clean. Phosphor naming preserved verbatim (`X` not `Close`, `CaretDown` not `ChevronDown`, `MagnifyingGlass` not `Search`, `Gear` not `Settings`, `House` not `Home`, `Funnel` not `Filter`, `EyeSlash` not `EyeOff`).
2. **Component Migrations (Phase 5, Waves 1–3)** — All 11 components migrated: `AeroCheckbox`, `AeroDropdown`, `AeroNumberSpinner`, `AeroContextMenu`, `AeroToastHost`, `AeroNotificationBanner`, `AeroAlertKind`, `AeroBannerKind` (Wave 1); `AeroSearchField` and `AeroPasswordField` Canvas composables deleted in favor of `Icon(AeroIcons.*)` (Wave 2); `AeroTitleBar` `TitleBarButton` restructured to take `ImageVector` (Wave 3).
3. **Dependency Cleanup (Phase 5, Waves 4–5)** — `AeroAlertKindTest` and `AeroBannerKindTest` rewritten to assert `AeroIcons.*` instances (CLN-01); `compose.materialIconsExtended` line removed from `library/build.gradle.kts` (CLN-02); grep gate clean (CLN-03). `compileClasspath` shed `material-icons-extended-desktop-1.7.3.jar` (~36 MB).
4. **Showcase IconsSection (Phase 6, Plan 01)** — `IconsSection.kt`: 138-entry `LazyVerticalGrid` with live `AeroSearchField` filter (case-insensitive substring), glassSurface cells, click-to-copy + toast feedback, "not found" empty state. Wired into `ShowcaseApp.kt` between `FoundationSection` and `ButtonsSection`.
5. **ButtonsSection migration (Phase 6, Plan 02)** — Showcase `AeroIconButton` row demos converted from `Text("▲"/"▼"/"×")` to real `Icon(AeroIcons.{CaretUp,CaretDown,X})` at 14dp; SHW-06 grep gate closed.
6. **Three-theme visual sign-off (Phase 6, Plan 03)** — All AeroBlue / AeroDark / Classic items PASS in formal visual checkpoint; v1.1 milestone signed off 2026-04-29. AeroNumberSpinner Phase 5 visual approval confirmed (12dp/14dp slot, no regression).

**Locked v1.1 decisions (carried forward):**
- AeroIcons source: Phosphor Regular (not Feather) — softer rounded stroke matches Win7-toolbar-glyph aesthetic
- Lazy backing-property pattern mandatory for all 138 constants — eager `val` at this scale causes measurable startup spike
- Phosphor naming preserved verbatim (no Material/Feather aliases)
- `Icon()` from material3 used directly — no custom `AeroIcon()` wrapper; tint always passed explicitly in library code
- Phase 5 wave ordering is mandatory: Waves 1+2 → Wave 3 → Wave 4 → Wave 5 (dep removal cannot precede test rewrites)
- AeroBreadcrumb `separator: String` intentionally NOT migrated to `ImageVector` in v1.1
- Generated `.kt` files committed to `src/main/`, NOT regenerated at build time; Phosphor SVG source committed to `tools/phosphor-svgs/regular/` with `.pin` file recording exact upstream SHA

**Issues resolved during milestone:**
- AeroNumberSpinner sub-pixel pitfall: Phosphor stroke at 10dp ≈ 0.63dp (sub-pixel at 96 DPI) → mitigated by raising button slot to 14dp; visual checkpoint confirmed in AeroDark
- Valkyrie 1.1.1 generated Shape A (extension properties) → required `--explicit-mode=true` flag and post-generation fix (`defaultWidth=256`→`24.dp`, AeroIcons import)
- Phosphor mixed-fill icons (DotsThree, Warning dot, Info dot, Bug eyes, etc.) correctly retain `fill=SolidColor(Color.Black)` — `ColorFilter.tint` replaces all colors at render time via `Icon(tint=...)`

**Issues deferred to follow-up:**
- AeroDropdown popup offset regression (carried over from v1.0 03-03 manual checkpoint; root cause `AeroScrollArea` `Column.fillMaxSize()`) — does not block v1.1 ship
- Win11 `undecorated+transparent` crash retest in CMP 1.10.3 — inherited from v1.0, no v1.1 action

**Technical debt incurred:**
- 138 explicit `internal.*` extension property imports per migration file (no wildcard) — consistent with Phase 5 precedent; no resolution planned for v1.x
- v1.0 was never formally archived through `/gsd:complete-milestone`. Phases 1–3 history is captured in `.planning/milestones/v1.1-ROADMAP.md` (full ROADMAP snapshot at v1.1 ship time) but not in a dedicated `v1.0-ROADMAP.md` archive.

---
