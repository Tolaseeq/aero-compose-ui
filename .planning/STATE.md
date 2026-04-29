---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Icon System
status: completed
stopped_at: "Completed 05-05-PLAN.md — Phase 5 complete: materialIconsExtended removed, 14 requirements closed"
last_updated: "2026-04-29T10:30:01.825Z"
last_activity: "2026-04-29 — Phase 5 complete: 14 requirements (MIG-01..11, CLN-01..03), materialIconsExtended removed from :library"
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 25
  completed_plans: 26
  percent: 97
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-28)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, and a showcase — no manual style work required
**Current focus:** v1.1 Icon System — Phase 4: AeroIcons Foundation (ready to plan)

## Current Position

Phase: 5 — Component Migrations + Dependency Removal — COMPLETE
Plan: 05 (final wave — dep removal + verification + docs)
Status: complete
Last activity: 2026-04-29 — Phase 5 complete: 14 requirements (MIG-01..11, CLN-01..03), materialIconsExtended removed from :library

Progress: [██████████] 97%  (25/25 total plans; Phase 6 next)

**Phase 6 — Showcase IconsSection — READY TO START**

## Previous Milestone

**v1.0 (completed 2026-04-28):** 3 phases, 53 requirements, 26 plans. Foundation + Atomic Components + Composite/Navigation. All shipped, all visual checkpoints approved.

## v1.1 Phase Summary

| Phase | Goal | Requirements | Status |
|-------|------|--------------|--------|
| 4 — AeroIcons Foundation | 138 Phosphor Regular ImageVector constants, lazy init, KDoc, explicitApi | ICN-01, ICN-02, ICN-03 | Complete |
| 5 — Migrations + Dep Removal | Replace all text glyphs + Material Icons; remove materialIconsExtended | MIG-01..11, CLN-01..03 | Complete |
| 6 — Showcase IconsSection | Grid + live search of all 138 icons; three-theme visual checkpoint | SHW-04, SHW-05, SHW-06 | Not started |

## Known Follow-up (from v1.0)

- **AeroDropdown popup regression** — User reported during 03-03 manual checkpoint: dropdown popup is offset right of the trigger field and has trailing empty space below the last item. Root cause: `AeroScrollArea` line 31-35 uses `Column.fillMaxSize()` which forces 320dp height when `heightIn(max=320.dp)` is set; combined with always-visible scrollbar overlay the popup looks shifted. Fix touches AeroScrollArea.kt only — schedule as gap-closure task.
- **White-border flash on resize** — User accepted as minor visual polish (skip per user instruction).
- **Commit-message interleaving** — Several Wave-2 commits absorbed sibling plans' files due to parallel staging (documented in 03-05/03-07 SUMMARYs; no rebase performed).

## Performance Metrics

**Velocity:**
- Total plans completed (v1.0): 26
- Average duration: ~7–25 min per plan
- Total v1.0 execution time: ~3 days

**By Phase (v1.0):**

| Phase | Plans | Total Time | Avg/Plan |
|-------|-------|------------|----------|
| 01-foundation | 4 | ~27 min | ~7 min |
| 02-atomic-components | 6 | ~1 h | ~10 min |
| 03-composite-navigation | 8 | ~2 h | ~15 min |

**v1.1 metrics:** pending (no plans completed yet)

*Updated after each plan completion*
| Phase 04-aeroicons-foundation P04-02 | 35min | 4 tasks | 139 files |
| Phase 05-component-migrations P01 | 25min | 9 tasks | 8 files |
| Phase 05-component-migrations P02 | 12min | 2 tasks | 2 files |
| Phase 05-component-migrations P03 | 4min | 1 tasks | 1 files |
| Phase 05-component-migrations P04 | 5min | 3 tasks | 2 files |
| Phase 05-component-migrations P05 | 15min | 4 tasks | 2 files |
JAR size: pre-v1.1 = ~0.96 MB (thin lib JAR), post-Phase-5 = ~0.96 MB (delta: -36.02 MB after materialIconsExtended removal from compileClasspath). See .planning/phases/05-component-migrations/05-SUMMARY.md.
| Phase 05-component-migrations P05 | 15 | 4 tasks | 4 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Phase 1: Use `undecorated=true` WITHOUT `transparent=true` — prevents Win11 EXCEPTION_ACCESS_VIOLATION (issue #3757); glass effect simulated via gradient
- Phase 1: Glass effect implemented in single `drawBehind` block to avoid overdraw and iGPU performance collapse
- Phase 1: Library module uses `compose.desktop.common` (not `currentOs`) to keep JAR platform-neutral
- Phase 3: AeroTitleBar placed in Phase 3 because it requires `FrameWindowScope` — only available in real window context
- [Phase 01-foundation]: Gradle 8.14.3 used (not 8.10.2) — cached locally, fully CMP 1.7.3 compatible
- [Phase 01-foundation]: Kotlin 2.1.21 / CMP 1.7.3 confirmed as actual published versions (RESEARCH.md cited non-existent 2.3.21/1.10.3)
- [Phase 01-foundation]: :library uses compose.desktop.common (platform-neutral JAR); :showcase uses compose.desktop.currentOs (native binary)
- [Phase 01-foundation]: AeroColorScheme drops 6 mordred domain tokens: connectionActive, connectionInactive, executionHighlight, logBackground, timeStampBackground, cardContent — application-specific, no meaning in a general UI library
- [Phase 01-foundation]: bodySmall retained on AeroTypography despite UI-SPEC removal — Plan 03 Material3 bridge maps MaterialTheme.typography.bodySmall to it; removing breaks the bridge
- [Phase 01-foundation]: @Immutable runtime reflection test replaced with isData check — Compose @Immutable uses BINARY retention (CLASS level), not visible at runtime; isData is correct runtime proxy
- [Phase 01-foundation]: defaultFactory pivot: ProvidableCompositionLocal.defaultFactory not public in CMP 1.7.3/Kotlin 2.1.21 — AeroThemeTest uses smoke tests per plan fallback
- [Phase 01-foundation]: JAVA_HOME must be JDK 17 (Gradle toolchain) for all Gradle invocations — Java 25 version string breaks Kotlin compiler JavaVersion.parse()
- [Phase 01-foundation]: Showcase omits explicitApi() — showcase build.gradle.kts has no constraint, public modifiers omitted
- [Phase 01-foundation]: No transparent/undecorated on Window — Win11 EXCEPTION_ACCESS_VIOLATION blocker remains in effect for Phase 1
- [Phase 02-atomic-components]: compose.animation added to library/build.gradle.kts — animateColorAsState is in androidx.compose.animation not .core
- [Phase 02-atomic-components]: AeroPasswordField uses inline BasicTextField (not AeroTextField wrapper) to support visualTransformation parameter
- [Phase 02-atomic-components]: AeroFilePicker uses inline glassSurface Box for Обзор button — avoids Wave 1 compile-order dependency on AeroOutlinedButton
- [Phase 02-01-buttons]: indication=null on AeroIconButton.clickable — hover/pressed drawn manually, suppresses M3 ripple double-effect
- [Phase 02-01-buttons]: AeroToolbarDefaults is a top-level object (not companion) — idiomatic Kotlin for composable helpers
- [Phase 02-01-buttons]: hover overlay via drawWithContent inside Button content Box (not outer wrapper) — avoids M3 Button shape clipping issues
- [Phase 02-atomic-components]: AeroComboBox popup uses PopupProperties(focusable=false) to preserve text field keyboard focus; keyboard nav inside popup not supported in v1
- [Phase 02-atomic-components]: AeroPopupPositionProvider is internal (same module) — Phase 3 AeroPopover/AeroContextMenu/AeroMenuBar can reuse it directly without duplication
- [Phase 02-04-range/list]: Color.Unspecified sentinel for AeroBadge defaults — AeroTheme.colors not accessible as Kotlin default arg outside Composable context
- [Phase 02-04-range/list]: AeroSlider tooltip centred above slider (not tracking thumb x) — Phase 3 KDoc enhancement documented
- [Phase 02-atomic-components]: animateColorAsState is in androidx.compose.animation (not .core) in CMP 1.7.3
- [Phase 02-atomic-components]: AeroChip uses primary.copy(0.25f) selected bg and cardBackground.copy(0.3f) unselected — direct MordredChip port
- [Phase 02-atomic-components]: Color.Transparent allowed in AeroSegmentedControl — named constant not hex literal
- [Phase 02-06-showcase]: M3 Button LocalMinimumInteractiveComponentSize defaults to 48dp — must set to Dp.Unspecified so hover overlay Box(matchParentSize) aligns with visual button bounds, not inflated touch target
- [Phase 02-06-showcase]: Dropdown popups need two-layer .background() (opaque base + translucent tint) — single 0xCC alpha is insufficient for readability over glassy parent surfaces
- [Phase 02-06-showcase]: All themes' buttonHover must be alpha-tinted whites (<=0x40 alpha) — fully opaque overlay paints over button text; Classic theme was incorrectly opaque
- [Phase 02-06-showcase]: AeroFilePicker keeps path field fully editable; Browse button is convenience-only to populate the field via native dialog
- [Phase 02-06-showcase]: AeroNumberSpinner scroll wheel via pointerInput PointerEventType.Scroll — works whether or not the field has keyboard focus (Mordred-style spinner pattern)
- [Phase 03-composite-navigation]: AeroPopupSide enum + auto-flip provider serves DRP, OVL-03/04/07 simultaneously — single source of truth for anchored popup placement
- [Phase 03-composite-navigation]: AeroDropdownPopup uses AeroScrollArea wrapping inner Column (preserves vertical padding); AeroTextArea uses AeroScrollBar overlay (BasicTextField needs its own verticalScroll modifier order)
- [Phase 03-composite-navigation]: Stub-test compile-reachability uses Class.forName(...Kt) instead of ::Composable — Kotlin Compose plugin disallows function references to @Composable functions
- [Phase 03-composite-navigation]: Container wrappers (CNT-01..04) are thin glassEffect/glassPanel wrappers — single Box, one padding param, content slot — no internal layout rules
- [Phase 03-composite-navigation]: AeroGroupBox label uses opaque-background Text painted over the top border line — no custom Layout/measure pass; produces Windows-Forms inset-label visual cheaply
- [Phase 03-composite-navigation]: AeroDivider exposes thickness param (default 1.dp) on top of vertical: Boolean — caller constrains length via passed Modifier
- [Phase 03-composite-navigation]: AeroToastHostState uses suspendCancellableCoroutine + onDismissed callback — caller awaits AeroToastResult; stack overflow evicts oldest (FIFO) at MAX_STACK_SIZE=5
- [Phase 03-composite-navigation]: Per-toast LaunchedEffect(data.id) inside key(data.id) — each toast carries its own dismiss timer that survives stack reorders without restarting
- [Phase 03-composite-navigation]: AeroBannerKind icon mapping uses Icons.Outlined (CheckCircle for Success); banner uses 1dp accent border at 0.6 alpha over glassPanel
- [Phase 03-composite-navigation]: AeroDialog enforces Win11 rule (undecorated=true, transparent=false) literally so source can be grep-checked; glass containment lives in inner Box via Modifier.glassSurface, NOT via window transparency
- [Phase 03-composite-navigation]: AeroAlertDialog delegates to AeroDialog (single Win11 rule source-of-truth); kind-keyed buttons mean Question gets OK+Cancel, others get OK only
- [Phase 03-composite-navigation]: AeroAlertKind enum cannot return theme tokens (Color companion sentinels need @Composable scope) — `accentFor` @Composable helper resolves primary/error/warning at composition time
- [Phase 03-composite-navigation]: AeroDrawer uses in-window scrim (Color.Black.copy(alpha=animatedAlpha)) + animated x-offset Box, NOT a separate DialogWindow — slide animation must render inside the parent window
- [Phase 03-composite-navigation]: AeroDrawer closed-but-composed early-return (`if (!open && offsetFraction == 1f) return`) is mandatory — prevents the transparent scrim from eating pointer input over the main UI when "closed"
- [Phase 03-composite-navigation]: compose.materialIconsExtended dep added to :library — Outlined.Error and Outlined.HelpOutline are not in icons-core (transitive via material3); AeroAlertKind needs them
- [Phase 03-composite-navigation]: AeroTitleBar declared as `FrameWindowScope.AeroTitleBar(...)` extension — required so it can call WindowDraggableArea; locks API to "must be invoked inside Window content lambda" which is fine because titlebar has no meaning outside a window
- [Phase 03-composite-navigation]: AeroResizeHandles is `public` (not `internal`) — `:library` and `:showcase` are separate Gradle modules and Kotlin's per-module `internal` blocks cross-module access; revisit visibility tightening when an internal-only navigation seam appears
- [Phase 03-composite-navigation]: 8-zone resize handlers — Top/Left edges + TL/TR/BL corners mutate BOTH windowState.size AND windowState.position; Bottom/Right + BR corner only mutate size
- [Phase 03-composite-navigation]: windowState.position mutation guarded by `is WindowPosition.Absolute` cast — PlatformDefault positions are not mutable; first drag at top/left silently won't move position until OS assigns absolute, acceptable v1 behavior
- [Phase 03-composite-navigation]: ResizeHandles.kt is a separate file from AeroTitleBar.kt — chrome and resize are orthogonal concerns
- [Phase 03-composite-navigation]: Theme propagation gap fixed in Plan 03-08 — theme state lifted to Main.kt
- [Phase 03-composite-navigation]: Win11 transparent=false rule enforced at three source-of-truth points (showcase Main.kt, AeroDialog DialogWindow, Phase 1 doctrine)
- [v1.1 locked decisions]:
  - AeroIcons source: Phosphor Regular (not Feather) — softer rounded stroke matches Win7-toolbar-glyph aesthetic
  - Lazy backing-property pattern mandatory for all 138 constants — eager val at this scale causes measurable startup spike
  - AeroIcons naming follows Phosphor verbatim (PascalCase from kebab): `X` not `Close`, `CaretDown` not `ChevronDown`, `MagnifyingGlass` not `Search`, `Gear` not `Settings`, `House` not `Home`, `Funnel` not `Filter`, `EyeSlash` not `EyeOff`
  - AeroBreadcrumb `separator: String` stays as-is in v1.1 — intentional, not an oversight; the separator is the only text-rendered glyph deliberately excluded from migration
  - Valkyrie CLI 1.1.1 with `--output-format BackingProperty`; generated files committed to src/main (not build/); Phosphor SVGs committed to tools/phosphor-svgs/regular/
  - Icon() from material3 used directly — no custom AeroIcon() wrapper; tint always passed explicitly in library code
  - Phase 5 wave ordering is mandatory: Waves 1+2 (component migrations) → Wave 3 (TitleBar) → Wave 4 (test rewrites CLN-01) → Wave 5 (dep removal CLN-02+CLN-03)
  - AeroNumberSpinner sub-pixel risk: implementer must choose Canvas draw OR button height ≥ 14dp after visual check in AeroDark
- [Phase 04-aeroicons-foundation]: Valkyrie 1.1.1 generates Shape A (extension properties); --explicit-mode=true required (not bare flag); post-generation: fix defaultWidth=256→24dp + add AeroIcons import to internal/*.kt
- [Phase 04-aeroicons-foundation]: Shape A at 138-icon scale — AeroIcons.kt facade empty body; 138 extension properties in internal/*.kt auto-callable as AeroIcons.*
- [Phase 04-aeroicons-foundation]: Phosphor mixed-fill icons (DotsThree, Warning dot, Info dot, Bug eyes, etc.) correctly retain fill=SolidColor(Color.Black); ColorFilter.tint replaces ALL colors including fill at render time via Icon(tint=...)
- [Phase 05-component-migrations]: AeroNumberSpinner carets at 12dp/14dp slot approved at visual checkpoint; no fallback needed
- [Phase 05-component-migrations]: Explicit internal.* extension property imports required alongside AeroIcons facade in all migration files
- [Phase 05-component-migrations]: Error->AeroIcons.XCircle and Question->AeroIcons.Question (Phosphor naming, replaces Icons.Outlined.Error/HelpOutline)
- [Phase 05-component-migrations]: internal.* extension property imports required alongside AeroIcons facade in AeroSearchField + AeroPasswordField (same as 05-01 pattern)
- [Phase 05-component-migrations]: AeroTitleBar TitleBarButton: textColor param removed; tint uniformly colors.onSurface; hover color only on background; AeroIcons.Square=maximize, AeroIcons.FrameCorners=restore
- [Phase 05-component-migrations]: Explicit internal.* imports required in test files using AeroIcons — facade alone does not expose extension properties; same pattern as production enums
- [Phase 05-component-migrations]: Library thin JAR unchanged (0-byte delta) after materialIconsExtended removal — dep was classpath-only, never embedded; material-icons-extended-desktop-1.7.3.jar (~36 MB) eliminated from compileClasspath

### Pending Todos

- Gap-close: AeroDropdown popup offset regression (from v1.0 checkpoint; does not block v1.1)

### Blockers/Concerns

- Phase 1: Validate whether Win11 `undecorated+transparent` crash is fixed in CMP 1.10.3 (may have been patched — test in Phase 1) — inherited from v1.0, no action in v1.1
- AeroNumberSpinner small-size: RESOLVED in Phase 5 — 12dp / 14dp slot approved at visual checkpoint; no blockers remaining for Phase 6

## Session Continuity

Last session: 2026-04-29T10:30:01.817Z
Stopped at: Completed 05-05-PLAN.md — Phase 5 complete: materialIconsExtended removed, 14 requirements closed
Resume file: None
Next action: `/gsd:plan-phase 6` (Showcase IconsSection — SHW-04, SHW-05, SHW-06)
