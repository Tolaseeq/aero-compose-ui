# Milestones

## v2.0 Stateful + Layout (Shipped: 2026-06-18)

**Phases:** 7–11 (5 phases) | **Plans:** 27 (Phase 7: 3, Phase 8: 6, Phase 9: 3, Phase 10: 4, Phase 11: 11) | **Requirements:** 27/27 (PICK-01..08, DATA-01..06, LAYO-01..09, SHW-07..10)

**Timeline:** 2026-04-30 → 2026-06-18 (~49 days elapsed, concentrated execution)
**Git range:** `2a4eaae` (feat 07-01) → `4d902cb` (HEAD) — 145 commits, 47 `feat`
**Diff:** 152 files changed, +27,406 / −2,285
**Audit:** ✅ PASSED — 27/27 requirements (3-source cross-referenced), 0 integration blockers, E2E showcase compiles + wires end-to-end (`.planning/milestones/v2.0-MILESTONE-AUDIT.md`)

**Delivered:** 12 new stateful + layout components (8 complex stateful, 4 advanced layout) added on top of an internal-primitives foundation, no breaking changes to the v1.x public API. `kotlinx-datetime:0.6.2` is the only new dependency. The showcase gained DataSection, PickersSection, and LayoutSection, and passed the 16-item × 3-theme "looks done but isn't" silent-failure checklist (48 cells) as the formal milestone sign-off gate.

**Key accomplishments:**
1. **Shared internal primitives (Phase 7)** — `AeroCalendarGrid`, `AeroColorMath` (HSV single-source-of-truth, round-trip drift-free), `AeroHsvColorSquare` + `AeroHueSlider`, `Modifier.aeroDragSplitter`, `AeroStepIndicator`, and `AeroCalendarPositionProvider` built and tested (27 JUnit tests) so Phases 8–10 share one drag pattern, one calendar renderer, and one color-math layer with no per-component duplication.
2. **Pickers (Phase 8)** — Six public components: `AeroRangeSlider`, `AeroDatePicker`, `AeroTimePicker`, `AeroDateTimePicker`, `AeroDateRangePicker`, `AeroColorPicker`. PITFALL-03 (touchSlop drag), PITFALL-06 (partial-range leak via sealed state machine), PITFALL-15 (HSV drift), and PITFALL-02/08 (popup clipping/first-frame jump) all defused; `kotlinx-datetime:0.6.2` added.
3. **Data (Phase 9)** — `AeroDataTable` (virtualized LazyColumn, 3-position column sort, `Set<RowKey>` Ctrl/Shift multi-selection surviving sort, drag-resize columns with min-width clamp) and `AeroTreeView` (once-only lazy `onExpand` via `SnapshotStateMap` above LazyColumn). PITFALL-01/04/05 resolved.
4. **Layout (Phase 10)** — `AeroAccordion` (single/multi, lifted state), `AeroSplitPane` (clamped divider, 8dp hit-area), `AeroSidebar` (3-mode `animateDpAsState`, scope DSL, collapsed tooltips), `AeroStepperWizard` (`onValidate` commit-gate on click only, Back-navigation preserves composable state). PITFALL-11/12/13/14 resolved.
5. **Showcase + v2.0 visual sign-off (Phase 11)** — DataSection / PickersSection / LayoutSection wired into ShowcaseApp, RangeSection extended; Phase 7 scratch files deleted cleanly. The 16-item × 3-theme checklist FAILED first pass (16 defects), all closed across gap-plans 11-06…11-11, then re-verified — all 48 cells PASS (SHW-10 gate satisfied).

**Locked v2.0 decisions (carried forward):**
- `detectDragGestures` banned for Canvas-based drag on Compose Desktop — use `awaitPointerEventScope` + manual loop (PITFALL-03, touchSlop=18dp); `Modifier.aeroDragSplitter` is the shared utility
- `AeroScrollArea` banned inside DataTable / TreeView — raw `LazyListState + AeroScrollBar` (PITFALL-01)
- DataTable selection API is `Set<RowKey>` + caller `key: (T) -> Any`, never `Set<Int>` indices (PITFALL-04)
- ColorPicker internal state is HSV float tuple only; RGB and HEX are derived views (PITFALL-15)
- `AeroCalendarPositionProvider` (Phase 7) replaces `AeroDropdownPopup` for all date-picker popups (PITFALL-02)
- All v2.0 overlays use `Popup(...)`, never `Dialog(transparent=true)` (W11-01, grep-gated)
- `kotlinx-datetime:0.6.2` is the only new dependency

**Issues deferred to follow-up (tech debt — all documentation hygiene or advisory, no deferred functional work):**
- SUMMARY frontmatter `requirements-completed` gaps: PICK-03, DATA-05/06, LAYO-03/04/08/09 absent from plan SUMMARYs (all verified SATISFIED in phase VERIFICATIONs)
- Phase 8 verification filed as `08-VERIFICATION-REPORT.md`, not the glob-standard `08-VERIFICATION.md`
- `kotlinx-datetime` declared `implementation` not `api` — picker public signatures expose `kotlinx.datetime.*`; transitive type would leak for a PUBLISHED library; address at publish/POM step
- Cosmetic: duplicate import (`AeroAccordion.kt:21,24`), stale KDoc reference to deleted scratch section (`AeroStepIndicator.kt:45`)
- LAYO-06 API deviation: per-item `onClick:()->Unit` in `AeroSidebarScope.item()` instead of sidebar-level `onItemClick:(ItemKey)->Unit` — intentional, strictly more flexible
- Nyquist: Phases 7, 8, 11 not formally `nyquist_compliant` (substantial JUnit coverage exists — 27 + 65 tests; formal validation contract not completed) — optional `/gsd:validate-phase 7|8|11`
- Carry-over: AeroDropdown popup-offset regression (v1.0) — explicitly OUT of v2.0 scope; future gap-closure or v2.x

---

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
