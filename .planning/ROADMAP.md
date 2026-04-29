# Roadmap: aero-compose-ui

## Overview

Three phases deliver a complete Compose Desktop UI component library styled after Windows Aero. Phase 1 establishes the theme system and glass modifiers — everything else depends on it. Phase 2 builds all atomic components (buttons, inputs, selection, sliders, lists) that define reusable interaction patterns. Phase 3 completes the library with composite containers, overlays, and window chrome components that depend on atomic primitives. The showcase application grows with each phase so the library is always demonstrable.

Milestone v1.1 (Icon System) appends Phases 4–6. Phases 1–3 are complete and not renumbered.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

### v1.0 (Complete)

- [x] **Phase 1: Foundation** - Theme system, glass modifiers, module structure, showcase skeleton (completed 2026-04-27)
- [x] **Phase 2: Atomic Components** - Buttons, inputs, selection controls, sliders, list items, badges (completed 2026-04-28)
- [x] **Phase 3: Composite + Navigation** - Containers, overlays, dialogs, menus, tabs, window chrome (completed 2026-04-28)

### v1.1 Icon System (Active)

- [x] **Phase 4: AeroIcons Foundation** - Valkyrie CLI conversion of 138 Phosphor Regular SVGs; lazy backing-property pattern; KDoc; explicitApi; spot-check 5 icons before full batch (completed 2026-04-29)
- [x] **Phase 5: Component Migrations + Dependency Removal** - Wave-ordered migrations of 11 components; Canvas composable deletions; TitleBar private restructure; test rewrites; materialIconsExtended Gradle line removal + grep verification (completed 2026-04-29)
- [ ] **Phase 6: Showcase IconsSection** - LazyVerticalGrid of all 138 icons with name labels and live search filter; ButtonsSection glyph demos updated; three-theme visual checkpoint

## Phase Details

### Phase 1: Foundation
**Goal**: The library module exists with a complete theme system, glass modifiers, and explicit public API — and a running showcase skeleton demonstrates theme switching
**Depends on**: Nothing (first phase)
**Requirements**: FOUND-01, FOUND-02, FOUND-03, FOUND-04, FOUND-05, FOUND-06, FOUND-07, FOUND-08, FOUND-09, FOUND-10, SHW-01, SHW-02, SHW-03
**Success Criteria** (what must be TRUE):
  1. A consumer project can add one Gradle dependency, wrap content in `AeroTheme {}`, and all child composables receive AeroBlue, AeroDark, or Classic color tokens automatically
  2. A developer can call `Modifier.glassEffect()`, `Modifier.glassPanel()`, or `Modifier.glassSurface()` and the composable renders the correct gradient + border + shadow in a single `drawBehind` pass (no overdraw)
  3. Attempting to use any non-public type from the `:library` JAR produces a compile error — `explicitApi()` is enforced
  4. The `:showcase` app launches, shows an empty component grid grouped by category, and a theme switcher toggles between AeroBlue / AeroDark / Classic instantly
  5. A developer can supply a custom `AeroColorScheme` to `AeroTheme` and all glass surfaces and text reflect the custom tokens
**Plans**: 4 plans
- [x] 01-01-PLAN.md — Gradle skeleton: root build, version catalog, :library + :showcase modules with explicitApi (completed 2026-04-27)
- [x] 01-02-PLAN.md — AeroColorScheme (23 tokens, 3 presets) + AeroTypography data classes (TDD) (completed 2026-04-27)
- [x] 01-03-PLAN.md — AeroTheme composable + LocalAero* CompositionLocals + glass modifiers (single-pass drawBehind) (completed 2026-04-27)
- [x] 01-04-PLAN.md — :showcase skeleton: Main, ShowcaseApp, ThemeSwitcher, FoundationSection, PlaceholderSection (visual checkpoint) (completed 2026-04-27)

### Phase 2: Atomic Components
**Goal**: All self-contained interactive components are implemented and interactive in the showcase — hover, focus, disabled, and pressed states work correctly using theme tokens
**Depends on**: Phase 1
**Requirements**: BTN-01, BTN-02, BTN-03, BTN-04, INP-01, INP-02, INP-03, INP-04, INP-05, INP-06, SEL-01, SEL-02, SEL-03, SEL-04, SEL-05, DRP-01, DRP-02, RNG-01, RNG-02, LST-01, LST-02
**Success Criteria** (what must be TRUE):
  1. Every button variant (AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar) renders enabled/disabled/hover/pressed states with correct Aero glass styling — verifiable in the showcase
  2. Every text input (AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker) accepts keyboard input, shows focus animation, and exposes its value via state — verifiable in the showcase
  3. Every selection control (AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl) toggles state correctly and renders the correct Aero visual for each state
  4. AeroSlider moves a single thumb and emits the correct float value; AeroProgressBar displays both determinate (with %) and indeterminate (animated shimmer) modes
  5. AeroDropdown and AeroComboBox open a themed dropdown, allow item selection, and close — AeroComboBox also accepts free-text input
**Plans**: 6 plans
- [x] 02-01-PLAN.md — Buttons (BTN-01..04): AeroButton, AeroOutlinedButton, AeroIconButton, AeroToolbar + ButtonsSection (completed 2026-04-28)
- [x] 02-02-PLAN.md — Text inputs (INP-01..06): AeroTextField, AeroTextArea, AeroPasswordField, AeroNumberSpinner, AeroSearchField, AeroFilePicker + InputSection (completed 2026-04-28)
- [x] 02-03-PLAN.md — Selection controls (SEL-01..05): AeroCheckbox, AeroRadioButton/Group, AeroSwitch, AeroChip, AeroSegmentedControl + SelectionSection (completed 2026-04-28)
- [x] 02-04-PLAN.md — Range + Lists (RNG-01..02, LST-01..02): AeroSlider, AeroProgressBar, AeroListItem, AeroBadge + RangeSection + ListSection (completed 2026-04-28)
- [x] 02-05-PLAN.md — Dropdowns (DRP-01..02): AeroDropdown + AeroComboBox + AeroPopupPositionProvider + DropdownSection (completed 2026-04-28)
- [x] 02-06-PLAN.md — Wire all sections into ShowcaseApp + ThemeSwitcher swap to AeroSegmentedControl + visual checkpoint (completed 2026-04-28)

### Phase 3: Composite + Navigation
**Goal**: All container, overlay, and navigation components are implemented — a complete Compose Desktop window can be built using only aero-compose-ui with no custom chrome or raw Material3 surfaces visible
**Depends on**: Phase 2
**Requirements**: CNT-01, CNT-02, CNT-03, CNT-04, CNT-05, CNT-06, OVL-01, OVL-02, OVL-03, OVL-04, OVL-05, OVL-06, OVL-07, OVL-08, NAV-01, NAV-02, NAV-03, NAV-04, NAV-05
**Success Criteria** (what must be TRUE):
  1. AeroCard, AeroPanel, AeroDivider, AeroGroupBox, AeroScrollArea, and AeroScrollBar render correctly with glass styling; AeroScrollArea scrolls arbitrary content and AeroScrollBar is compatible with Compose `ScrollState`
  2. AeroDialog and AeroAlertDialog appear as modal overlays; AeroToast/AeroSnackbar auto-dismiss after a configurable timeout; AeroNotificationBanner shows info/warning/error/success variants with a close button; AeroDrawer animates in from the side; AeroPopover positions next to its anchor; AeroContextMenu opens on right-click; AeroTooltip appears on hover with a delay
  3. AeroTitleBar renders the Aero gradient, draggable region, and Minimize/Maximize/Close buttons with hover effects — built inside `FrameWindowScope` using `undecorated=true` (without `transparent=true` to avoid the Win11 crash)
  4. AeroMenuBar shows top-level items with dropdown submenus; AeroTabBar switches between tabs; AeroStatusBar displays text sections and colored indicators at the bottom of the window
  5. The showcase is complete: every component from Phases 1-3 appears in the appropriate category section, is interactive (not a placeholder), and all three themes render correctly across every component
**Plans**: 8 plans
- [x] 03-01-PLAN.md — Popup infrastructure refactor + AeroScrollBar (CNT-06) + AeroScrollArea (CNT-05) + LocalScrollbarStyle wiring + Wave-0 stub tests (completed 2026-04-28)
- [x] 03-02-PLAN.md — Containers: AeroCard (CNT-01), AeroPanel (CNT-02), AeroDivider (CNT-03), AeroGroupBox (CNT-04) (completed 2026-04-28)
- [x] 03-03-PLAN.md — AeroTitleBar (NAV-01) + showcase Main.kt undecorated window + resize handles (completed 2026-04-28)
- [x] 03-04-PLAN.md — Modal overlays: AeroDialog (OVL-01), AeroAlertDialog (OVL-02), AeroDrawer (OVL-08) (completed 2026-04-28)
- [x] 03-05-PLAN.md — Anchored popups: AeroTooltip (OVL-03), AeroContextMenu (OVL-04), AeroPopover (OVL-07) (completed 2026-04-28)
- [x] 03-06-PLAN.md — Notifications: AeroToast/AeroToastHost (OVL-05), AeroNotificationBanner (OVL-06) (completed 2026-04-28)
- [x] 03-07-PLAN.md — Remaining navigation: AeroMenuBar (NAV-02), AeroStatusBar (NAV-03), AeroBreadcrumb (NAV-04), AeroTabBar (NAV-05) (completed 2026-04-28)
- [x] 03-08-PLAN.md — ShowcaseApp wiring: 3 new sections + AeroToastHost mount + theme hoisting + final visual checkpoint (completed 2026-04-28)

---

## v1.1 Icon System — Phase Details

### Phase 4: AeroIcons Foundation
**Goal**: The typed icon set exists and compiles — every one of the 138 Phosphor Regular ImageVector constants is accessible via `AeroIcons.*` autocomplete with lazy initialization, and the object satisfies `explicitApi()`
**Depends on**: Phase 3 (v1.0 complete; all migration targets already exist in :library)
**Requirements**: ICN-01, ICN-02, ICN-03
**Success Criteria** (what must be TRUE):
  1. A developer can write `Icon(AeroIcons.X, contentDescription = null, tint = colors.onSurface)` in any :library or :showcase file and the close-X glyph renders as a Phosphor Regular rounded-stroke vector — no text character, no Canvas draw
  2. `Icon(AeroIcons.CaretDown, ...)`, `Icon(AeroIcons.MagnifyingGlass, ...)`, `Icon(AeroIcons.Gear, ...)`, `Icon(AeroIcons.House, ...)`, `Icon(AeroIcons.Funnel, ...)` all compile and render the correct Phosphor glyphs (not their Material/Feather namesakes)
  3. `./gradlew :library:compileKotlin` passes with no `explicitApi()` errors on `AeroIcons.kt` or any `icons/` file; every public constant has an explicit `public` modifier
  4. Accessing `AeroIcons.X` for the first time does not cause a measurable startup spike — every constant uses the null-backed lazy getter pattern (`private var _X: ImageVector? = null; public val X: ImageVector get() = _X ?: loadX().also { _X = it }`), not an eager `val`
  5. Spot-check of 5 representative icons (at minimum: `X`, `CaretDown`, `MagnifyingGlass`, `Check`, `Info`) confirms `viewportWidth = 256f`, `strokeLineWidth = 16f`, `strokeLineCap = StrokeCap.Round`, `fill = Color.Transparent` in the generated Kotlin — `grep -rn "viewportWidth=24f" library/src/main/kotlin/com/mordred/aero/icons/` returns 0 hits

**Phase notes:**
- Phosphor SVG source committed to `tools/phosphor-svgs/regular/` alongside a `.pin` file recording the exact `phosphor-icons/core` commit hash used for conversion
- Valkyrie CLI 1.1.1 used for batch conversion with `--output-format BackingProperty`; the Gradle build does NOT invoke Valkyrie at build time — generated `.kt` files are committed as ordinary source
- `AeroIcons.kt` KDoc must include: naming convention table (Phosphor kebab → PascalCase, e.g. `caret-down` → `CaretDown` not `ChevronDown`), recommended size range 16dp–32dp, mandatory explicit `tint` note, and phosphoricons.com lookup URL
- The 5-icon spot-check and first `compileKotlin` run happen before batch-converting all remaining 131 icons (138 total per CONTEXT-locked correction; row #139 was a duplicate)
**Plans**: 2 plans
- [x] 04-01-PLAN.md — Spike: doc corrections (139→138, stroke 12→16) + 7-icon vendoring + Valkyrie spike + facade authoring + first compile-pass (completed 2026-04-29)
- [x] 04-02-PLAN.md — Batch: verify questionable SVGs + bulk-fetch remaining 131 + Valkyrie full run + facade extension to 138 + final verification gate (completed 2026-04-29)

### Phase 5: Component Migrations + Dependency Removal
**Goal**: Every text glyph and Material Icons reference is replaced with `AeroIcons.*` across all :library components and tests, and `compose.materialIconsExtended` is removed from the Gradle dependency graph entirely
**Depends on**: Phase 4
**Requirements**: MIG-01, MIG-02, MIG-03, MIG-04, MIG-05, MIG-06, MIG-07, MIG-08, MIG-09, MIG-10, MIG-11, CLN-01, CLN-02, CLN-03
**Success Criteria** (what must be TRUE):
  1. `grep -rn 'Text("▲\|▼\|▶\|✕\|✓\|─\|□\|❒\|–")' library/src/` returns 0 hits; `grep -rn 'Text("x"' library/src/` returns 0 hits — AeroSearchField's lowercase-x clear button is confirmed migrated
  2. `AeroCheckbox` renders `Icon(AeroIcons.Check, ...)` for checked state and `Icon(AeroIcons.Minus, ...)` for indeterminate; `AeroTitleBar` window control buttons render `AeroIcons.Minus` (minimize), `AeroIcons.Square` / `AeroIcons.FrameCorners` (maximize/restore), `AeroIcons.X` (close) — private `TitleBarButton` accepts `ImageVector` not `String`
  3. `AeroSearchField` has no `SearchIcon()` Canvas composable — the private function is deleted; `AeroPasswordField` has no `EyeOpenIcon()` or `EyeClosedIcon()` Canvas composables — both private functions are deleted; `Icon(AeroIcons.Eye)` / `Icon(AeroIcons.EyeSlash)` are used at the call sites
  4. `./gradlew :library:compileKotlin` succeeds with `implementation(compose.materialIconsExtended)` removed from `library/build.gradle.kts`; `./gradlew :library:dependencies --configuration compileClasspath | grep -i materialIcons` returns 0 lines; `grep -rn "androidx.compose.material.icons" library/src/` returns 0 hits (including test sources)
  5. JAR size after removal is documented (expected reduction: ~6–8 MB vs pre-migration JAR); `AeroNumberSpinner` up/down icons are visually confirmed not sub-pixel-thin in all three themes — if Phosphor stroke at the button's render size antialiases to below ~0.5dp, the plan documents the chosen mitigation (Canvas draw or button height increase to 14dp+)

**Phase notes — wave ordering (must be respected):**
- Wave 1 (parallel, internal-only, no API impact): AeroCheckbox (MIG-01), AeroDropdown (MIG-02), AeroNumberSpinner (MIG-03), AeroToastHost (MIG-06), AeroNotificationBanner (MIG-07), AeroContextMenu (MIG-05), AeroAlertKind (MIG-10), AeroBannerKind (MIG-11)
- Wave 2 (Canvas deletions, parallel per file): AeroSearchField (MIG-08), AeroPasswordField (MIG-09)
- Wave 3 (isolated): AeroTitleBar private restructure — `TitleBarButton(glyph: String)` → `TitleBarButton(icon: ImageVector)` (MIG-04)
- Wave 4 (gate for dep removal): AeroAlertKindTest.kt and AeroBannerKindTest.kt rewritten to assert `AeroIcons.*` by name — CLN-01; this MUST complete before Wave 5
- Wave 5 (final): remove `implementation(compose.materialIconsExtended)` from `library/build.gradle.kts` (CLN-02) + grep verification (CLN-03)
- AeroBreadcrumb `separator: String` is intentionally NOT migrated to `ImageVector` in v1.1 — this is a locked decision; do not change the parameter type during any migration sweep
- AeroNumberSpinner sub-pixel risk: at 10dp render, Phosphor stroke = 10×(16/256) ≈ 0.63dp (sub-pixel at 96 DPI); visual checkpoint required in AeroDark disabled state; Canvas draw or button height ≥ 14dp are the accepted mitigations
**Plans**: 5 plans
- [ ] 05-01-PLAN.md — Wave 1: internal-only Text-glyph and Material-icon swaps (MIG-01, MIG-02, MIG-03, MIG-05, MIG-06, MIG-07, MIG-10, MIG-11) + inline AeroNumberSpinner visual checkpoint
- [ ] 05-02-PLAN.md — Wave 2: Canvas composable deletions (MIG-08 AeroSearchField, MIG-09 AeroPasswordField)
- [ ] 05-03-PLAN.md — Wave 3: AeroTitleBar private restructure (MIG-04, atomic signature + 3 call sites)
- [ ] 05-04-PLAN.md — Wave 4: Test rewrites (CLN-01 — AeroAlertKindTest + AeroBannerKindTest); gate for Wave 5
- [ ] 05-05-PLAN.md — Wave 5: Dependency removal (CLN-02 + CLN-03) + JAR-size pre/post measurement

### Phase 6: Showcase IconsSection
**Goal**: The showcase has a scrollable, searchable grid of all 138 AeroIcons that serves as the visual sign-off checkpoint for the entire v1.1 milestone across all three themes
**Depends on**: Phase 5
**Requirements**: SHW-04, SHW-05, SHW-06
**Success Criteria** (what must be TRUE):
  1. `IconsSection` is visible in the running showcase after `FoundationSection`; it displays all 138 icons in a `LazyVerticalGrid(GridCells.Adaptive(80.dp))` with each cell showing the icon at 24dp and its Kotlin identifier name below — all 138 are visible without horizontal scrolling
  2. Typing a query into the `AeroSearchField` at the top of `IconsSection` filters the grid in real-time (case-insensitive substring match on identifier name); typing `"caret"` shows only `CaretDown`, `CaretUp`, `CaretLeft`, `CaretRight`; clearing the field restores all 138 icons; an empty result shows a "not found" message
  3. Switching to AeroDark theme: all 138 icons are visible with correct contrast — no black or invisible icons (explicit `tint = AeroTheme.colors.onSurface` is confirmed at the `Icon()` call site in `IconCell`); `AeroNumberSpinner` disabled-state up/down icons are confirmed visible in AeroDark
  4. Switching to Classic theme: all 138 icons render with correct tint and Phosphor stroke contrast is acceptable at 24dp; stroke-weight check passes eye-on-screen for representative icons (`X`, `CaretDown`, `MagnifyingGlass`, `FrameCorners`, `Warning`)
  5. `ButtonsSection` demo `AeroIconButton` cells use `Icon(AeroIcons.CaretUp, ...)`, `Icon(AeroIcons.CaretDown, ...)`, and `Icon(AeroIcons.X, ...)` — no text glyphs remain in showcase source; `grep -rn 'Text("▲\|▼\|×\|✕")' showcase/src/` returns 0 hits

**Phase notes:**
- `LazyVerticalGrid` requires `Modifier.height(400.dp)` (or `heightIn(max=600.dp)`) — without a bounded height it cannot measure itself inside the parent vertically-scrolling `Column` and will crash at layout
- `IconsSection` is registered in `ShowcaseApp.kt` positioned after `FoundationSection` and before `ButtonsSection` — icons are foundational infrastructure and deserve early prominence
- The three-theme visual checkpoint is the formal sign-off gate for the entire v1.1 milestone; do not mark Phase 6 complete without eye-on-screen verification in all three themes
- Phosphor stroke on AeroDark background requires specific attention: Phosphor Regular at 24dp renders ~1.13dp stroke at 96 DPI — confirm visibility is acceptable, especially for fine-detail icons like `FrameCorners` and `Square`
**Plans**: 3 plans
- [ ] 06-01-PLAN.md — Create IconsSection.kt (138-icon grid + search/filter + click-to-copy) and wire into ShowcaseApp.kt (SHW-04, SHW-05) [Wave 1]
- [ ] 06-02-PLAN.md — Migrate ButtonsSection.kt three text glyphs to Icon(AeroIcons.{CaretUp,CaretDown,X}) (SHW-06) [Wave 1]
- [ ] 06-03-PLAN.md — Three-theme visual checkpoint (AeroBlue + AeroDark + Classic) — v1.1 milestone sign-off gate [Wave 2]

## Progress

**Execution Order:** 1 → 2 → 3 → 4 → 5 → 6

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 4/4 | Complete | 2026-04-27 |
| 2. Atomic Components | 6/6 | Complete | 2026-04-28 |
| 3. Composite + Navigation | 8/8 | Complete | 2026-04-28 |
| 4. AeroIcons Foundation | 2/2 | Complete   | 2026-04-29 |
| 5. Component Migrations + Dependency Removal | 6/5 | Complete   | 2026-04-29 |
| 6. Showcase IconsSection | 0/3 | Not started | - |
