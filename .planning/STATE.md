---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_plan: 7 of 8 in phase complete (Wave 2 done — 03-02 containers, 03-03 titlebar, 03-04 dialogs, 03-05 popovers, 03-06 toasts, 03-07 navigation)
status: completed
stopped_at: Wave 2 complete — 7/8 plans landed, only 03-08 (showcase wiring) remains
last_updated: "2026-04-28T17:10:00Z"
last_activity: 2026-04-28 — Phase 3 Wave 2 finalized — all atomic+composite components ship; only showcase integration (03-08) and verification remain
progress:
  total_phases: 3
  completed_phases: 2
  total_plans: 18
  completed_plans: 17
  percent: 94
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-27)

**Core value:** Connect one Gradle dependency and get the full Aero-styled component set with three themes, custom window chrome, and a showcase — no manual style work required
**Current focus:** Phase 1 — Foundation

## Current Position

Phase: 3 of 3 (Composite + Navigation) — IN PROGRESS
Current Plan: 7 of 8 in phase complete — Wave 2 (03-02..03-07) finalized. Only 03-08 (showcase wiring) remains.
Status: Phase 3 Wave 2 complete. Plans 03-02 (CNT-01..04), 03-03 (NAV-01 + manual-checkpoint approved), 03-04 (OVL-01/02/08), 03-05 (OVL-03/04/07), 03-06 (OVL-05/06), 03-07 (NAV-02..05) all in HEAD. 03-05 and 03-07 retried after first-attempt watchdog stalls; their commits had already landed when the orchestrator stopped them at SUMMARY-write — orchestrator finalized SUMMARY/metadata directly.
Last activity: 2026-04-28 — Phase 3 Wave 2 finalized; 17/18 plans complete; 03-08 showcase wiring next.

Progress: [█████████▌] 94%

## Known Follow-up

- **AeroDropdown popup regression** — User reported during 03-03 manual checkpoint: dropdown popup is offset right of the trigger field and has trailing empty space below the last item. Root cause located in Plan 03-01's retrofit: `AeroScrollArea` line 31-35 uses `Column.fillMaxSize()` which forces 320dp height when `heightIn(max=320.dp)` is set, regardless of content; combined with always-visible scrollbar overlay the popup looks shifted. Fix is small (touches AeroScrollArea.kt only) — schedule as gap-closure or fold into 03-08 if a checkpoint surfaces.
- **White-border flash on resize** — User accepted as minor visual polish (skip per user instruction).
- **Commit-message interleaving** — Several Wave-2 commits absorbed sibling plans' files due to parallel staging:
  - `7fe9eb0` (subject "feat(03-07): StatusBar+TabBar") includes 03-05's AeroContextMenu.kt + AeroContextMenuItem.kt
  - `9cdf488` (subject "test(03-07): NAV-02") includes 03-03's finalization metadata
  - `8f63ca4` (subject "feat(03-03): resize handles") includes 03-02's docs
  Diff content is correct; only subject lines are partially misattributed. Documented in 03-05/03-07 SUMMARYs; no rebase performed.

## Performance Metrics

**Velocity:**
- Total plans completed: 4
- Average duration: ~7 min
- Total execution time: ~27 min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
| Phase 01-foundation P01 | 6 | 3 tasks | 14 files |
| Phase 01-foundation P02 | 11 | 3 tasks | 5 files |
| Phase 01-foundation P03 | 7 | 2 tasks | 3 files |
| Phase 01-foundation P04 | 3 | 2 tasks | 5 files |
| Phase 02-atomic-components P01 | 8 | 3 tasks | 6 files |
| Phase 02-atomic-components P02 | 7 | 3 tasks | 7 files |
| Phase 02-atomic-components P05 | 9 | 3 tasks | 5 files |
| Phase 02-atomic-components P04 | 9 | 3 tasks | 6 files |
| Phase 02-atomic-components P03 | 10 | 3 tasks | 6 files |
| Phase 02-atomic-components P06 | ~45 | 3 tasks | 2 files |
| Phase 03-composite-navigation P01 | 7m | 4 tasks | 16 files |
| Phase 03-composite-navigation P02 | 11min | 2 tasks | 8 files |
| Phase 03-composite-navigation P06 | 25min | 2 tasks | 9 files |
| Phase 03-composite-navigation P04 | 25min | 3 tasks | 8 files |
| Phase 03-composite-navigation P03 | ~10min | 3 tasks (2 auto + 1 manual checkpoint) | 4 files |

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
- [Phase 03-composite-navigation]: 8-zone resize handlers — Top/Left edges + TL/TR/BL corners mutate BOTH windowState.size AND windowState.position (deltaApplied = oldSize - newSize, then position += deltaApplied) so the anchored opposite edge stays fixed; Bottom/Right + BR corner only mutate size
- [Phase 03-composite-navigation]: windowState.position mutation guarded by `is WindowPosition.Absolute` cast — PlatformDefault positions are not mutable; first drag at top/left silently won't move position until OS assigns absolute, acceptable v1 behavior
- [Phase 03-composite-navigation]: ResizeHandles.kt is a separate file from AeroTitleBar.kt — chrome (visual gradient + buttons + drag) and resize (8-zone overlay + pointerInput) are orthogonal concerns; keeps each file single-responsibility
- [Phase 03-composite-navigation]: Theme propagation gap accepted for Plan 03-03 — AeroTitleBar always renders AeroBlue colors because theme state lives inside ShowcaseApp; Plan 03-08 will lift theme state to Main.kt to fix
- [Phase 03-composite-navigation]: Win11 transparent=false rule now enforced at three source-of-truth points (showcase Main.kt top-level Window via Plan 03-03, AeroDialog DialogWindow via Plan 03-04, plus original Phase 1 doctrine) — future window-creating composables should follow the same grep-checkable literal pattern

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 1: Validate whether Win11 `undecorated+transparent` crash is fixed in CMP 1.10.3 (may have been patched — test in Phase 1)
- Phase 1: Decide Haze vs. gradient-only glass rendering after seeing demo output
- Phase 3: AeroTitleBar + native Aero Snap (HTCAPTION) requires spike — `WindowDraggableArea` does not pass HTCAPTION to OS; this is a documented limitation

## Session Continuity

Last session: 2026-04-28T16:35:00Z
Stopped at: Completed 03-03-PLAN.md (NAV-01 AeroTitleBar + 8-zone AeroResizeHandles + showcase Window(undecorated=true, transparent=false))
Resume file: None
